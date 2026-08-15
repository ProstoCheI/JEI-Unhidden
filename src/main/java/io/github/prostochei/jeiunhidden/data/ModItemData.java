/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ModItemData(
        String modId,
        String displayName,
        List<ItemStack> allItems,
        List<ItemStack> normalItems,
        List<ItemStack> hiddenItems,
        Set<Item> hiddenItemTypes
) {
    public static Map<String, ModItemData> collectAll() {
        Map<String, List<ItemStack>> normalByMod = new LinkedHashMap<>();
        Set<Item> creativeItemTypes = Collections.newSetFromMap(new IdentityHashMap<>());

        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (!stack.isEmpty()) {
                        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                        normalByMod.computeIfAbsent(namespace, k -> new ArrayList<>()).add(stack);
                        creativeItemTypes.add(stack.getItem());
                    }
                }
            }
        }

        Map<String, List<ItemStack>> allByMod = new LinkedHashMap<>();
        Map<String, List<ItemStack>> hiddenByMod = new LinkedHashMap<>();
        Map<String, Set<Item>> hiddenTypesByMod = new LinkedHashMap<>();

        for (Map.Entry<String, List<ItemStack>> entry : normalByMod.entrySet()) {
            allByMod.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }

        for (Item item : BuiltInRegistries.ITEM) {
            String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            if (!creativeItemTypes.contains(item)) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    allByMod.computeIfAbsent(namespace, k -> new ArrayList<>()).add(stack);
                    hiddenByMod.computeIfAbsent(namespace, k -> new ArrayList<>()).add(stack);
                    hiddenTypesByMod.computeIfAbsent(namespace, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(item);
                }
            }
        }

        Map<String, ModItemData> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<ItemStack>> entry : allByMod.entrySet()) {
            String modId = entry.getKey();
            List<ItemStack> all = entry.getValue();
            List<ItemStack> normal = normalByMod.getOrDefault(modId, List.of());
            List<ItemStack> hidden = hiddenByMod.getOrDefault(modId, List.of());
            Set<Item> hiddenTypes = hiddenTypesByMod.getOrDefault(modId, Set.of());
            String displayName = resolveDisplayName(modId);

            result.put(modId, new ModItemData(modId, displayName, all, normal, hidden, hiddenTypes));
        }

        return result;
    }

    public static List<ModItemData> collectSortedList() {
        List<ModItemData> list = new ArrayList<>(collectAll().values());
        list.sort(Comparator.comparing(data -> data.displayName().toLowerCase(Locale.ROOT)));
        return list;
    }

    private static String resolveDisplayName(String modId) {
        if ("minecraft".equals(modId)) {
            return "Minecraft";
        }
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElseGet(() -> formatFallbackName(modId));
    }

    private static String formatFallbackName(String modId) {
        if (modId.isEmpty()) {
            return modId;
        }
        return Character.toUpperCase(modId.charAt(0)) + modId.substring(1);
    }
}
