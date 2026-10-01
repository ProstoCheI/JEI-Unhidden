/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden.gui;

import io.github.prostochei.jeiunhidden.config.FilterMode;
import io.github.prostochei.jeiunhidden.data.ModItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModFilterList extends ContainerObjectSelectionList<ModFilterList.Entry> {
    private final List<ModItemData> allMods;
    private final Map<String, FilterMode> pendingModes;

    public ModFilterList(
            Minecraft minecraft,
            int width,
            int height,
            int y,
            int itemHeight,
            List<ModItemData> mods,
            Map<String, FilterMode> pendingModes
    ) {
        super(minecraft, width, height, y, itemHeight);
        this.allMods = mods;
        this.pendingModes = pendingModes;
        refreshList("");
    }

    public void refreshList(String filter) {
        this.clearEntries();
        String query = filter.trim().toLowerCase(Locale.ROOT);

        for (ModItemData mod : allMods) {
            if (query.isEmpty()
                    || mod.displayName().toLowerCase(Locale.ROOT).contains(query)
                    || mod.modId().toLowerCase(Locale.ROOT).contains(query)) {
                this.addEntry(new Entry(this.minecraft, mod, pendingModes));
            }
        }
        this.setScrollAmount(this.scrollAmount());
    }

    @Override
    public int getRowWidth() {
        return Math.min(420, this.width - 20);
    }

    @Override
    protected int scrollBarX() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }

    public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final Minecraft minecraft;
        private final ModItemData modData;
        private final Button toggleButton;

        public Entry(Minecraft minecraft, ModItemData modData, Map<String, FilterMode> pendingModes) {
            this.minecraft = minecraft;
            this.modData = modData;

            FilterMode initialMode = pendingModes.getOrDefault(modData.modId(), FilterMode.SHOW_UNHIDDEN);
            this.toggleButton = Button.builder(initialMode.getDisplayName(), button -> {
                FilterMode current = pendingModes.getOrDefault(modData.modId(), FilterMode.SHOW_UNHIDDEN);
                FilterMode next = current.next();
                pendingModes.put(modData.modId(), next);
                button.setMessage(next.getDisplayName());
            }).bounds(0, 0, 130, 20).build();
        }

        @Override
        public void render(
                GuiGraphics guiGraphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovering,
                float partialTick
        ) {
            Font font = minecraft.font;

            Component nameComponent = Component.literal(modData.displayName()).withStyle(ChatFormatting.WHITE);
            guiGraphics.drawString(font, nameComponent, left + 4, top + 2, 0xFFFFFFFF);

            Component infoComponent = Component.literal(modData.modId() + " • ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable("gui.jei_unhidden.items_count", modData.allItems().size())
                            .withStyle(ChatFormatting.GRAY));
            guiGraphics.drawString(font, infoComponent, left + 4, top + 13, 0xFF888888);

            toggleButton.setX(left + width - toggleButton.getWidth() - 4);
            toggleButton.setY(top + 2);
            toggleButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggleButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(toggleButton);
        }
    }
}
