/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum FilterMode {
    SHOW_UNHIDDEN("gui.jei_unhidden.mode.show_unhidden", ChatFormatting.GREEN),
    NORMAL("gui.jei_unhidden.mode.normal", ChatFormatting.GOLD),
    HIDE_ALL("gui.jei_unhidden.mode.hide_all", ChatFormatting.RED);

    private final String translationKey;
    private final ChatFormatting color;

    FilterMode(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }

    public FilterMode next() {
        return switch (this) {
            case SHOW_UNHIDDEN -> NORMAL;
            case NORMAL -> HIDE_ALL;
            case HIDE_ALL -> SHOW_UNHIDDEN;
        };
    }
}
