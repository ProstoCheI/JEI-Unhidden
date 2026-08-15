/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden;

import io.github.prostochei.jeiunhidden.config.FilterConfig;
import io.github.prostochei.jeiunhidden.gui.ModFilterScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = JeiUnhidden.MOD_ID, dist = Dist.CLIENT)
public final class JeiUnhidden {
    public static final String MOD_ID = "jei_unhidden";

    public JeiUnhidden(ModContainer modContainer) {
        FilterConfig.load();
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parentScreen) -> new ModFilterScreen(parentScreen)
        );
    }
}
