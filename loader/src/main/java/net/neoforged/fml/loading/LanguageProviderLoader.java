/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import com.mojang.logging.LogUtils;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.util.ServiceLoaderUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.language.IModLanguageProvider;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;

public class LanguageProviderLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<IModLanguageProvider> languageProviders;
    private final Map<String, ModLanguageWrapper> languageProviderMap = new HashMap<>();

    public void forEach(final Consumer<IModLanguageProvider> consumer) {
        languageProviders.forEach(consumer);
    }

    public <T> Stream<T> applyForEach(final Function<IModLanguageProvider, T> function) {
        return languageProviders.stream().map(function);
    }

    private record ModLanguageWrapper(IModLanguageProvider modLanguageProvider, ArtifactVersion version) {}

    LanguageProviderLoader(ILaunchContext launchContext) {
        languageProviders = ServiceLoaderUtil.loadServices(launchContext, IModLanguageProvider.class);
        ImmediateWindowHandler.updateProgress("Loading language providers");
        languageProviders.forEach(lp -> {
            final Path lpPath;
            try {
                lpPath = Paths.get(lp.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Huh?", e);
            }
            Optional<String> version = JarVersionLookupHandler.getVersion(lp.getClass());
            String impl = version.orElse(Files.isDirectory(lpPath) ? FMLLoader.versionInfo().fmlVersion().split("\\.")[0] : null);
            if (impl == null) {
                LOGGER.error(LogMarkers.CORE, "Found unversioned language provider {}", lp.name());
                throw new RuntimeException("Failed to find implementation version for language provider " + lp.name());
            }
            LOGGER.debug(LogMarkers.CORE, "Found language provider {}, version {}", lp.name(), impl);
            ImmediateWindowHandler.updateProgress("Loaded language provider " + lp.name() + " " + impl);
            languageProviderMap.put(lp.name(), new ModLanguageWrapper(lp, new DefaultArtifactVersion(impl)));
        });
    }

    public IModLanguageProvider findLanguage(ModFile mf, String modLoader, VersionRange modLoaderVersion) {
        final String languageFileName = mf.getFileName();
        final ModLanguageWrapper mlw = languageProviderMap.get(modLoader);
        if (mlw == null) {
            LOGGER.error(LogMarkers.LOADING, "Missing language {} version {} wanted by {}", modLoader, modLoaderVersion, languageFileName);
            throw new ModLoadingException(ModLoadingIssue.error("fml.language.missingversion", modLoader, modLoaderVersion, languageFileName, "null").withAffectedModFile(mf));
        }
        if (!VersionSupportMatrix.testVersionSupportMatrix(modLoaderVersion, modLoader, "languageloader", (llid, range) -> range.containsVersion(mlw.version()))) {
            LOGGER.error(LogMarkers.LOADING, "Missing language {} version {} wanted by {}, found {}", modLoader, modLoaderVersion, languageFileName, mlw.version());
            throw new ModLoadingException(ModLoadingIssue.error("fml.language.missingversion", modLoader, modLoaderVersion, languageFileName, mlw.version()).withAffectedModFile(mf));
        }

        return mlw.modLanguageProvider();
    }
}