/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adds the default stack of every registered item to JEI, including items that
 * are intentionally omitted from creative-mode tabs by their owning mod.
 */
@JeiPlugin
public final class JeiUnhiddenPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(
            JeiUnhidden.MOD_ID,
            "all_items"
    );
    private final AtomicBoolean restorationScheduled = new AtomicBoolean();
    private IJeiRuntime jeiRuntime;
    private IIngredientManager listenedManager;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> allItems = collectRegisteredItems();
        registration.addExtraItemStacks(allItems);
        LOGGER.info("Offered {} registered item stacks to JEI", allItems.size());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        registerRemovalListener(jeiRuntime.getIngredientManager());
        restoreMissingItems(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        restorationScheduled.set(false);
    }

    private void registerRemovalListener(IIngredientManager ingredientManager) {
        if (listenedManager == ingredientManager) {
            return;
        }

        listenedManager = ingredientManager;
        ingredientManager.registerIngredientListener(new IIngredientManager.IIngredientListener() {
            @Override
            public <V> void onIngredientsAdded(
                    IIngredientHelper<V> ingredientHelper,
                    Collection<ITypedIngredient<V>> ingredients
            ) {
            }

            @Override
            public <V> void onIngredientsRemoved(
                    IIngredientHelper<V> ingredientHelper,
                    Collection<ITypedIngredient<V>> ingredients
            ) {
                if (ingredients.stream().anyMatch(ingredient -> ingredient.getItemStack().isPresent())) {
                    scheduleRestoration();
                }
            }
        });
    }

    private void scheduleRestoration() {
        if (!restorationScheduled.compareAndSet(false, true)) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            restorationScheduled.set(false);
            IJeiRuntime currentRuntime = jeiRuntime;
            if (currentRuntime != null) {
                restoreMissingItems(currentRuntime);
            }
        });
    }

    private static void restoreMissingItems(IJeiRuntime jeiRuntime) {
        Set<Item> knownItems = Collections.newSetFromMap(new IdentityHashMap<>());
        jeiRuntime.getIngredientManager().getAllItemStacks().stream()
                .map(ItemStack::getItem)
                .forEach(knownItems::add);

        List<ItemStack> missingItems = collectRegisteredItems().stream()
                .filter(stack -> !knownItems.contains(stack.getItem()))
                .toList();

        if (!missingItems.isEmpty()) {
            jeiRuntime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, missingItems);
            LOGGER.info("Restored {} missing item stacks in the JEI runtime", missingItems.size());
        }
    }

    private static List<ItemStack> collectRegisteredItems() {
        return BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }
}
