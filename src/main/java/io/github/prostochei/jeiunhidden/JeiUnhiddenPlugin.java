/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden;

import com.mojang.logging.LogUtils;
import io.github.prostochei.jeiunhidden.config.FilterConfig;
import io.github.prostochei.jeiunhidden.config.FilterMode;
import io.github.prostochei.jeiunhidden.data.ModItemData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@JeiPlugin
public final class JeiUnhiddenPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(
            JeiUnhidden.MOD_ID,
            "all_items"
    );
    private static final AtomicBoolean RESTORATION_SCHEDULED = new AtomicBoolean();
    private static final AtomicBoolean IS_APPLYING_FILTERS = new AtomicBoolean();
    private static final Map<IIngredientType<?>, Map<String, List<Object>>> INITIAL_INGREDIENTS = new ConcurrentHashMap<>();
    private static final Map<IRecipeType<?>, List<Object>> INITIAL_RECIPES = new ConcurrentHashMap<>();
    private static final List<IRecipeCategory<?>> ALL_CATEGORIES = new ArrayList<>();

    private static IJeiRuntime jeiRuntime;
    private static IIngredientManager listenedManager;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        FilterConfig.load();
        Map<String, ModItemData> modDataMap = ModItemData.collectAll();
        List<ItemStack> allExtraItems = new ArrayList<>();

        for (ModItemData data : modDataMap.values()) {
            allExtraItems.addAll(data.hiddenItems());
        }

        if (!allExtraItems.isEmpty()) {
            registration.addExtraItemStacks(allExtraItems);
            LOGGER.info("Registered {} extra items to JEI across all mods", allExtraItems.size());
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        captureInitialIngredients(runtime.getIngredientManager());
        captureInitialRecipes(runtime.getRecipeManager());
        registerRemovalListener(runtime.getIngredientManager());
        applyFilters(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        listenedManager = null;
        INITIAL_INGREDIENTS.clear();
        INITIAL_RECIPES.clear();
        ALL_CATEGORIES.clear();
        RESTORATION_SCHEDULED.set(false);
    }

    public static void reapplyFilters() {
        IJeiRuntime current = jeiRuntime;
        if (current != null) {
            Minecraft.getInstance().execute(() -> applyFilters(current));
        }
    }

    @SuppressWarnings("unchecked")
    private static void captureInitialIngredients(IIngredientManager ingredientManager) {
        INITIAL_INGREDIENTS.clear();
        for (IIngredientType<?> rawType : ingredientManager.getRegisteredIngredientTypes()) {
            captureInitialForType(ingredientManager, (IIngredientType<Object>) rawType);
        }
    }

    private static <V> void captureInitialForType(IIngredientManager ingredientManager, IIngredientType<V> type) {
        IIngredientHelper<V> helper = ingredientManager.getIngredientHelper(type);
        Collection<V> all = ingredientManager.getAllIngredients(type);
        Map<String, List<Object>> byMod = new HashMap<>();

        for (V ingredient : all) {
            ResourceLocation loc = helper.getResourceLocation(ingredient);
            if (loc != null) {
                byMod.computeIfAbsent(loc.getNamespace(), k -> new ArrayList<>()).add(ingredient);
            }
        }

        INITIAL_INGREDIENTS.put(type, byMod);
    }

    @SuppressWarnings("unchecked")
    private static void captureInitialRecipes(IRecipeManager recipeManager) {
        INITIAL_RECIPES.clear();
        ALL_CATEGORIES.clear();

        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
                .includeHidden()
                .get()
                .toList();
        ALL_CATEGORIES.addAll(categories);

        for (IRecipeCategory<?> category : categories) {
            captureCategoryRecipes(recipeManager, (IRecipeCategory<Object>) category);
        }

        LOGGER.info("Captured {} recipe categories and their initial recipes for JEI Unhidden", ALL_CATEGORIES.size());
    }

    @SuppressWarnings("unchecked")
    private static <T> void captureCategoryRecipes(IRecipeManager recipeManager, IRecipeCategory<T> category) {
        IRecipeType<T> type = category.getRecipeType();
        List<T> recipes = recipeManager.createRecipeLookup(type)
                .includeHidden()
                .get()
                .toList();
        INITIAL_RECIPES.put(type, (List<Object>) recipes);
    }

    @SuppressWarnings("unchecked")
    private static void applyFilters(IJeiRuntime runtime) {
        IIngredientManager ingredientManager = runtime.getIngredientManager();
        IRecipeManager recipeManager = runtime.getRecipeManager();
        Map<String, ModItemData> modDataMap = ModItemData.collectAll();
        Collection<IIngredientType<?>> allTypes = ingredientManager.getRegisteredIngredientTypes();

        Set<String> hiddenMods = new HashSet<>();
        Set<Item> hiddenItemTypes = Collections.newSetFromMap(new IdentityHashMap<>());

        IS_APPLYING_FILTERS.set(true);
        try {
            for (ModItemData data : modDataMap.values()) {
                FilterMode mode = FilterConfig.getMode(data.modId());
                switch (mode) {
                    case SHOW_UNHIDDEN -> {
                        if (!data.allItems().isEmpty()) {
                            ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, data.allItems());
                        }
                        restoreNonItemIngredients(ingredientManager, allTypes, data.modId());
                    }
                    case NORMAL -> {
                        if (!data.normalItems().isEmpty()) {
                            ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, data.normalItems());
                        }
                        if (!data.hiddenItemTypes().isEmpty()) {
                            hiddenItemTypes.addAll(data.hiddenItemTypes());
                            List<ItemStack> hiddenStacksInJei = ingredientManager.getAllItemStacks().stream()
                                    .filter(stack -> data.hiddenItemTypes().contains(stack.getItem()))
                                    .toList();
                            if (!hiddenStacksInJei.isEmpty()) {
                                ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacksInJei);
                            }
                        }
                        restoreNonItemIngredients(ingredientManager, allTypes, data.modId());
                    }
                    case HIDE_ALL -> {
                        hiddenMods.add(data.modId());
                        hiddenItemTypes.addAll(data.allItems().stream().map(ItemStack::getItem).toList());
                        for (IIngredientType<?> rawType : allTypes) {
                            removeModIngredientsForType(ingredientManager, (IIngredientType<Object>) rawType, data.modId());
                        }
                    }
                }
            }

            applyRecipeFilters(recipeManager, hiddenMods, hiddenItemTypes);

            IIngredientFilter filter = runtime.getIngredientFilter();
            filter.setFilterText(filter.getFilterText());

            LOGGER.info("Applied JEI Unhidden mod filters and recipe visibility");
        } finally {
            IS_APPLYING_FILTERS.set(false);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyRecipeFilters(
            IRecipeManager recipeManager,
            Set<String> hiddenMods,
            Set<Item> hiddenItemTypes
    ) {
        for (IRecipeCategory<?> rawCategory : ALL_CATEGORIES) {
            filterCategoryRecipes(recipeManager, (IRecipeCategory<Object>) rawCategory, hiddenMods, hiddenItemTypes);
        }
    }

    private static <T> void filterCategoryRecipes(
            IRecipeManager recipeManager,
            IRecipeCategory<T> category,
            Set<String> hiddenMods,
            Set<Item> hiddenItemTypes
    ) {
        IRecipeType<T> type = category.getRecipeType();
        boolean categoryBelongsToHiddenMod = hiddenMods.contains(type.getUid().getNamespace());

        if (categoryBelongsToHiddenMod) {
            recipeManager.hideRecipeCategory(type);
        } else {
            recipeManager.unhideRecipeCategory(type);
        }

        @SuppressWarnings("unchecked")
        List<T> allRecipes = (List<T>) INITIAL_RECIPES.getOrDefault(type, List.of());
        if (allRecipes.isEmpty()) {
            return;
        }

        List<T> toHide = new ArrayList<>();
        List<T> toUnhide = new ArrayList<>();

        for (T recipe : allRecipes) {
            boolean shouldHide = false;
            ResourceLocation id = category.getRegistryName(recipe);
            if (id != null && hiddenMods.contains(id.getNamespace())) {
                shouldHide = true;
            } else if (recipe instanceof RecipeHolder<?>) {
                try {
                    // Minecraft no longer exposes Recipe#getResultItem; use JEI's output ingredients.
                    shouldHide = recipeManager.getRecipeIngredients(category, recipe)
                            .getIngredients(RecipeIngredientRole.OUTPUT).stream()
                            .flatMap(ingredient -> ingredient.getItemStack().stream())
                            .filter(stack -> !stack.isEmpty())
                            .anyMatch(stack -> hiddenMods.contains(
                                    BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()
                            ) || hiddenItemTypes.contains(stack.getItem()));
                } catch (Exception ignored) {
                }
            }

            if (shouldHide) {
                toHide.add(recipe);
            } else {
                toUnhide.add(recipe);
            }
        }

        if (!toHide.isEmpty()) {
            recipeManager.hideRecipes(type, toHide);
        }
        if (!toUnhide.isEmpty()) {
            recipeManager.unhideRecipes(type, toUnhide);
        }
    }

    @SuppressWarnings("unchecked")
    private static void restoreNonItemIngredients(
            IIngredientManager ingredientManager,
            Collection<IIngredientType<?>> allTypes,
            String modId
    ) {
        for (IIngredientType<?> rawType : allTypes) {
            if (rawType == VanillaTypes.ITEM_STACK) {
                continue;
            }
            IIngredientType<Object> type = (IIngredientType<Object>) rawType;
            Map<String, List<Object>> byMod = INITIAL_INGREDIENTS.get(type);
            if (byMod != null) {
                List<Object> initial = byMod.get(modId);
                if (initial != null && !initial.isEmpty()) {
                    ingredientManager.addIngredientsAtRuntime(type, initial);
                }
            }
        }
    }

    private static <V> void removeModIngredientsForType(
            IIngredientManager ingredientManager,
            IIngredientType<V> ingredientType,
            String modId
    ) {
        IIngredientHelper<V> helper = ingredientManager.getIngredientHelper(ingredientType);
        Collection<V> all = ingredientManager.getAllIngredients(ingredientType);
        List<V> toRemove = new ArrayList<>();

        for (V ingredient : all) {
            ResourceLocation loc = helper.getResourceLocation(ingredient);
            if (loc != null && loc.getNamespace().equals(modId)) {
                toRemove.add(ingredient);
            }
        }

        if (!toRemove.isEmpty()) {
            ingredientManager.removeIngredientsAtRuntime(ingredientType, toRemove);
        }
    }

    private static void registerRemovalListener(IIngredientManager ingredientManager) {
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
                if (!IS_APPLYING_FILTERS.get() && ingredients.stream().anyMatch(ingredient -> ingredient.getItemStack().isPresent())) {
                    scheduleRestoration();
                }
            }
        });
    }

    private static void scheduleRestoration() {
        if (!RESTORATION_SCHEDULED.compareAndSet(false, true)) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            RESTORATION_SCHEDULED.set(false);
            IJeiRuntime currentRuntime = jeiRuntime;
            if (currentRuntime != null && !IS_APPLYING_FILTERS.get()) {
                restoreMissingItems(currentRuntime);
            }
        });
    }

    private static void restoreMissingItems(IJeiRuntime runtime) {
        IIngredientManager ingredientManager = runtime.getIngredientManager();
        Set<Item> currentItems = Collections.newSetFromMap(new IdentityHashMap<>());
        ingredientManager.getAllItemStacks().stream()
                .map(ItemStack::getItem)
                .forEach(currentItems::add);

        Map<String, ModItemData> modDataMap = ModItemData.collectAll();
        List<ItemStack> toRestore = new ArrayList<>();

        for (ModItemData data : modDataMap.values()) {
            FilterMode mode = FilterConfig.getMode(data.modId());
            if (mode == FilterMode.SHOW_UNHIDDEN) {
                for (ItemStack stack : data.allItems()) {
                    if (!currentItems.contains(stack.getItem())) {
                        toRestore.add(stack);
                    }
                }
            }
        }

        if (!toRestore.isEmpty()) {
            IS_APPLYING_FILTERS.set(true);
            try {
                ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toRestore);
                LOGGER.info("Restored {} missing item stacks in the JEI runtime for SHOW_UNHIDDEN mods", toRestore.size());
            } finally {
                IS_APPLYING_FILTERS.set(false);
            }
        }
    }
}
