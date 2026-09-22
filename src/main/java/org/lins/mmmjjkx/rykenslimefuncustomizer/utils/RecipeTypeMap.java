/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjjkx) and balugaq
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.lins.mmmjjkx.rykenslimefuncustomizer.utils;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipeTypeMap {
    private static final Map<String, RecipeType> recipeTypes;

    static {
        recipeTypes = new HashMap<>();

        RecipeTypeExpandIntegration.registerRecipeTypes();
    }

    public static void removeRecipeTypes(String... keys) {
        for (String key : keys) {
            recipeTypes.remove(key);
        }
    }

    public static void pushRecipeType(RecipeType type) {
        recipeTypes.put(type.getKey().getKey().toUpperCase(Locale.ROOT), type);
    }

    public static void pushRecipeType(List<RecipeType> types) {
        types.forEach(RecipeTypeMap::pushRecipeType);
    }

    public static void clearRecipeTypes() {
        recipeTypes.clear();
    }

    @Nullable public static RecipeType getRecipeType(String s) {
        return recipeTypes.get(s);
    }

    public enum RecipeTypeExpandIntegration {
        INFINITY_EXPANSION(
            "InfinityExpansion",
            "io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench",
            "TYPE",
            true
        ),
        SLIME_TINKER(
            "SlimeTinker",
            "io.github.sefiraat.slimetinker.items.workstations.workbench.Workbench",
            "TYPE",
            true
        );

        private final String pluginName;
        private final String clazz;
        private final String fieldName;
        private final boolean isStatic;

        RecipeTypeExpandIntegration(String pluginName, String clazz, String fieldName, boolean isStatic) {
            this.pluginName = pluginName;
            this.clazz = clazz;
            this.fieldName = fieldName;
            this.isStatic = isStatic;
        }

        public RecipeType get() {
            try {
                Class<?> theClazz = Class.forName(clazz);
                Field field = theClazz.getDeclaredField(fieldName);
                return (RecipeType) field.get(null);
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                return null;
            }
        }

        static void registerRecipeTypes() {
            for (RecipeTypeExpandIntegration integration : values()) {
                Plugin detected = Bukkit.getPluginManager().getPlugin(integration.pluginName);
                if (detected == null) {
                    continue;
                }

                String className = integration.clazz;
                String fieldName = integration.fieldName;
                try {
                    Class<?> clazz = Class.forName(className);
                    if (integration.isStatic) {
                        var field = clazz.getField(fieldName);
                        field.setAccessible(true);
                        RecipeTypeMap.pushRecipeType((RecipeType) field.get(null));
                    }
                } catch (ClassNotFoundException e) {
                    if (integration == INFINITY_EXPANSION && isModernInfinityExpansion2(detected)) {
                        Debug.debug("Skipping legacy InfinityWorkbench recipe-type bridge for InfinityExpansion2.");
                        continue;
                    }
                    Debug.warn(
                        "Plugin " + integration.pluginName + " is installed but its recipe type class is missing: " + className
                    );
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    Debug.warn("Failed to get external recipe type from " + className + "#" + fieldName + ": " + e.getMessage());
                }
            }
        }

        private static boolean isModernInfinityExpansion2(Plugin detected) {
            if (detected.getClass().getName().startsWith("net.guizhanss.infinityexpansion2.")) {
                return true;
            }

            Plugin ie2 = Bukkit.getPluginManager().getPlugin("InfinityExpansion2");
            return ie2 != null && ie2.isEnabled();
        }
    }
}
