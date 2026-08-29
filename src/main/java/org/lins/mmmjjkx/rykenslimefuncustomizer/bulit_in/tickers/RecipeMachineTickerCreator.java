package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.AbstractRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomMachineRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.RecipeReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NullMarked
public class RecipeMachineTickerCreator implements TickerCreator {
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
        var rps = readRecipes(file, section.getConfigurationSection("recipes"), addon, false);
        if (rps != null) {
            result.addAll(rps);
        }
        return result;
    }

    public @Nullable List<CustomMachineRecipe> readRecipes(File file, @Nullable ConfigurationSection recipes, ProjectAddon addon, boolean canInputEmpty) {
        if (recipes == null) return Collections.emptyList();
        List<CustomMachineRecipe> list = new ArrayList<>();
        for (String key : recipes.getKeys(false)) {
            ConfigurationSection recipe = recipes.getConfigurationSection(key);
            if (recipe == null) continue;
            int seconds = recipe.getInt("seconds", -1);
            if (seconds < 0) {
                Debug.error(file, recipe, "MissingConfiguration error 'recipe' (seconds)");
                continue;
            }
            ConfigurationSection inputs = recipe.getConfigurationSection("input");
            List<InputWrapper> input = CommonUtils.readInputs(file, inputs, addon, recipe.getBoolean("noConsume", false));
            boolean configuredInputs = inputs != null && !inputs.getKeys(false).isEmpty();
            if ((configuredInputs && input.isEmpty()) || (!canInputEmpty && input.isEmpty())) {
                Debug.error(file, recipe, configuredInputs
                    ? "Skipping recipe because one or more configured inputs could not be resolved."
                    : "Missing recipe input (input).");
                continue;
            }
            ConfigurationSection outputs = recipe.getConfigurationSection("output");
            if (outputs == null) {
                Debug.error(file, recipe, "Missing 'item' (output)");
                continue;
            }

            List<ItemStack> output = new ArrayList<>();
            IntList chances = new IntArrayList();
            boolean invalidOutput = false;
            for (String k : outputs.getKeys(false)) {
                ConfigurationSection outputCfg = outputs.getConfigurationSection(k);
                if (outputCfg == null) {
                    invalidOutput = true;
                    break;
                }
                var item = CommonUtils.readItem(file, outputCfg, addon);
                if (item == null) {
                    Debug.error(file, outputCfg, "Skipping recipe because an output item could not be resolved.");
                    invalidOutput = true;
                    break;
                }

                int chance = CommonUtils.clamp(outputCfg.getInt("chance", 100), 1, 100,
                    file, outputCfg, "' (chance) '");

                output.add(item);
                chances.add(chance);
            }

            if (invalidOutput || output.isEmpty()) {
                continue;
            }

            RecipeReader.addToList(list, recipe, seconds, input, chances, output.toArray(new ItemStack[0]));
        }
        return list;
    }

    @Override
    public @Nullable MachineTicker create(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon) {
        var recipes = read(file, section, addon);
        if (recipes == null) return null;
        if (recipes.isEmpty()) {
            Debug.warn("machine " + sf.getId() + " recipe!");
        }
        return new RecipeMachineTicker() {
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
