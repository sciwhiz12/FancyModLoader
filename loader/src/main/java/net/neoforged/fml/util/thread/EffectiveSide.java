/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.util.thread;

import net.neoforged.fml.LogicalSide;

public class EffectiveSide {
    public static LogicalSide get() {
        final ThreadGroup group = Thread.currentThread().getThreadGroup();
        return group instanceof SidedThreadGroup ? ((SidedThreadGroup) group).getSide() : LogicalSide.CLIENT;
    }
}
