package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.AbstractRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomLinkedMachineRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.LinkedOutput;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public class LinkedRecipeMachineTickerCreator extends RecipeMachineTickerCreator {
    @Override
    public @Nullable List<? extends Recipe> read(File file, ConfigurationSection section, ProjectAddon addon) {
        List<Recipe> result = new ArrayList<>();
        var importFrom = section.getString("recipes_import_from");
        if (importFrom != null) {
            var sf = SlimefunItem.getById(importFrom);
            if (sf == null) {
                Debug.warn(file, section, "InvalidrecipeSource (recipes_import_from): " + importFrom);
            } else {
                result.addAll(readRecipes(sf));
            }
        }
        var rps = readRecipes(file, section, addon);
        if (rps != null) {
            result.addAll(rps);
        }
        return result;
    }

    public @Nullable List<? extends AbstractRecipe> readRecipes(File file, ConfigurationSection section, ProjectAddon addon) {
        var recipes = section.getConfigurationSection("recipes");
        if (recipes == null) return Collections.emptyList();

        int saveAmount = section.getInt("saveAmount", 0);
        if (saveAmount < 0 || saveAmount >= 63) {
            Debug.error(file, section, "Configuration error '' (saveAmount)", 0, 62);
            return null;
        }

        List<CustomLinkedMachineRecipe> list = new ArrayList<>();
        for (String key : recipes.getKeys(false)) {
            ConfigurationSection recipe = recipes.getConfigurationSection(key);
            if (recipe == null) continue;
            int seconds = recipe.getInt("seconds");
            if (seconds < 0) {
                Debug.warn(file, recipe, "MissingConfiguration error '' (seconds)");
                continue;
            }
            ConfigurationSection inputs = recipe.getConfigurationSection("input");
            if (inputs == null) {
                Debug.warn(file, recipe, "Missing 'item' (input)");
                continue;
            }

            ConfigurationSection outputs = recipe.getConfigurationSection("output");
            if (outputs == null) {
                Debug.warn(file, recipe, "Missing 'item' (output)");
                continue;
            }

            List<ItemStack> freeOutput = new ArrayList<>();
            IntList freeChances = new IntArrayList();

            Map<Integer, ItemStack> linkedOutput = new HashMap<>();
            Map<Integer, Integer> linkedChances = new HashMap<>();

            for (String k : outputs.getKeys(false)) {
                ConfigurationSection output = outputs.getConfigurationSection(k);
                if (output == null) break;
                var item = CommonUtils.readItem(file, output, addon);
                if (item != null) {
                    int chance = CommonUtils.clamp(output.getInt("chance", 100), 1, 100,
                        file, output, "' (chance) '");

                    int slot = output.getInt("slot", -1);
                    if (slot == -1) {
                        freeOutput.add(item);
                        freeChances.add(chance);
                    } else {
                        linkedOutput.put(slot, item);
                        linkedChances.put(slot, chance);
                    }
                }
            }

            boolean chooseOne = recipe.getBoolean("chooseOne", false);
            boolean forDisplay = recipe.getBoolean("forDisplay", false);
            boolean hide = recipe.getBoolean("hide", false);
            boolean noConsumeAll = recipe.getBoolean("noConsume", false);

            IntSet noConsume = new IntOpenHashSet();
            Map<Integer, ItemStack> stackMap = new HashMap<>();
            for (String k : inputs.getKeys(false)) {
                ConfigurationSection input = inputs.getConfigurationSection(k);
                if (input == null) continue;

                ItemStack itemStack = CommonUtils.readItem(file, input, addon);
                if (itemStack == null) {
                    continue;
                }

                int slot = input.getInt("slot", -1);
                if (slot == -1) {
                    Debug.warn(file, input, "MissingConfiguration error 'slot' (slot)");
                    continue;
                }

                if (slot < 0 || slot > 53) {
                    Debug.warn(file, input, "'slot' (slot)");
                    continue;
                }

                stackMap.put(slot, itemStack);

                if (input.getBoolean("noConsume", false)) {
                    noConsume.add(slot);
                }
            }
            if (noConsumeAll) {
                noConsume.addAll(stackMap.keySet());
            }

            list.add(new CustomLinkedMachineRecipe(
                seconds,
                stackMap,
                new LinkedOutput(
                    freeOutput.toArray(new ItemStack[0]),
                    InvIndex.mergeItems(freeOutput),
                    linkedOutput,
                    freeChances,
                    linkedChances
                ),
                noConsume,
                chooseOne,
                forDisplay,
                hide,
                saveAmount,
                noConsumeAll));
        }
        return list;
    }

    @Override
    public @Nullable MachineTicker create(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon) {
        var recipes = read(file, section, addon);
        if (recipes == null) return null;
        return new LinkedRecipeMachineTicker() {
            @Override
            public int getEnergyConsumption() {
                return sf.getEnergyConsumption();
            }

            @Override
            public int getCapacity() {
                return sf.getCapacity();
            }

            @Override
            public @Nullable CustomMenu getCustomMenu() {
                return menu;
            }

            @Override
            public AdvancedCustomMachine getMachine() {
                return sf;
            }

            @Override
            public int[] getInputSlots() {
                return sf.getInputSlots();
            }

            @Override
            public int[] getOutputSlots() {
                return sf.getOutputSlots();
            }

            @Override
            public List<? extends Recipe> getRecipes() {
                return recipes;
            }
        };
    }
}
