/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.TypesafeMap;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforgespi.Environment;

public class FMLEnvironment {
    public static final Dist dist = FMLLoader.getDist();
    public static final boolean production = FMLLoader.isProduction() || System.getProperties().containsKey("production");
    public static final boolean secureJarsEnabled = FMLLoader.isSecureJarEnabled();

    static void setupInteropEnvironment(IEnvironment environment) {
        environment.computePropertyIfAbsent(Environment.Keys.DIST.get(), v -> dist);
    }

    public static class Keys {
        public static final Supplier<TypesafeMap.Key<ClassLoader>> LOCATORCLASSLOADER = IEnvironment.buildKey("LOCATORCLASSLOADER", ClassLoader.class);
    }
}
