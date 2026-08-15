/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden.gui;

import io.github.prostochei.jeiunhidden.JeiUnhiddenPlugin;
import io.github.prostochei.jeiunhidden.config.FilterConfig;
import io.github.prostochei.jeiunhidden.config.FilterMode;
import io.github.prostochei.jeiunhidden.data.ModItemData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModFilterScreen extends Screen {
    private final Screen parentScreen;
    private final Map<String, FilterMode> pendingModes;
    private ModFilterList filterList;
    private EditBox searchBox;
    private String lastSearch = "";

    public ModFilterScreen(Screen parentScreen) {
        super(Component.translatable("gui.jei_unhidden.title"));
        this.parentScreen = parentScreen;
        this.pendingModes = new HashMap<>(FilterConfig.getAllModes());
    }

    @Override
    protected void init() {
        super.init();

        List<ModItemData> mods = ModItemData.collectSortedList();

        int listTop = 46;
        int listBottom = this.height - 32;
        int listHeight = listBottom - listTop;

        this.filterList = new ModFilterList(
                this.minecraft,
                this.width,
                listHeight,
                listTop,
                26,
                mods,
                this.pendingModes
        );
        this.addRenderableWidget(this.filterList);

        int searchWidth = Math.min(240, this.width - 40);
        this.searchBox = new EditBox(
                this.font,
                (this.width - searchWidth) / 2,
                22,
                searchWidth,
                18,
                Component.translatable("gui.jei_unhidden.search")
        );
        this.searchBox.setHint(Component.translatable("gui.jei_unhidden.search.hint"));
        this.searchBox.setValue(this.lastSearch);
        this.searchBox.setResponder(text -> {
            this.lastSearch = text;
            if (this.filterList != null) {
                this.filterList.refreshList(text);
            }
        });
        this.addRenderableWidget(this.searchBox);

        int buttonWidth = Math.min(200, this.width - 40);
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.jei_unhidden.save"),
                button -> saveAndClose()
        ).bounds((this.width - buttonWidth) / 2, this.height - 26, buttonWidth, 20).build());

        if (!this.lastSearch.isEmpty()) {
            this.filterList.refreshList(this.lastSearch);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    private void saveAndClose() {
        FilterConfig.setAllModes(this.pendingModes);
        FilterConfig.save();
        JeiUnhiddenPlugin.reapplyFilters();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
