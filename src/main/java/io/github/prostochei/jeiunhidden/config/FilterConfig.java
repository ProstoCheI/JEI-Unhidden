/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Copyright (c) 2026 ProstoCheI
 */
package io.github.prostochei.jeiunhidden.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FilterConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("jei_unhidden.json");
    private static final Type MAP_TYPE = new TypeToken<Map<String, FilterMode>>() {}.getType();

    private static final Map<String, FilterMode> MOD_FILTERS = new ConcurrentHashMap<>();

    private FilterConfig() {
    }

    public static FilterMode getMode(String modId) {
        return MOD_FILTERS.getOrDefault(modId, FilterMode.SHOW_UNHIDDEN);
    }

    public static void setMode(String modId, FilterMode mode) {
        if (mode == FilterMode.SHOW_UNHIDDEN) {
            MOD_FILTERS.remove(modId);
        } else {
            MOD_FILTERS.put(modId, mode);
        }
    }

    public static Map<String, FilterMode> getAllModes() {
        return new HashMap<>(MOD_FILTERS);
    }

    public static void setAllModes(Map<String, FilterMode> modes) {
        MOD_FILTERS.clear();
        for (Map.Entry<String, FilterMode> entry : modes.entrySet()) {
            if (entry.getValue() != FilterMode.SHOW_UNHIDDEN) {
                MOD_FILTERS.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, FilterMode> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                MOD_FILTERS.clear();
                MOD_FILTERS.putAll(loaded);
            }
            LOGGER.info("Loaded {} mod filter configurations for JEI Unhidden", MOD_FILTERS.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load JEI Unhidden config file: {}", CONFIG_PATH, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(MOD_FILTERS, writer);
            }
            LOGGER.info("Saved JEI Unhidden mod filters config to {}", CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to save JEI Unhidden config file: {}", CONFIG_PATH, e);
        }
    }
}
