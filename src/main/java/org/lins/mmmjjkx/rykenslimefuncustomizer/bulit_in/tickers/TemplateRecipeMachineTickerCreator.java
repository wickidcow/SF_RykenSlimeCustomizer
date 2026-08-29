package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.AbstractRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomTemplateMachineRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NullMarked
public class TemplateRecipeMachineTickerCreator extends RecipeMachineTickerCreator {
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

    public @Nullable List<? extends Recipe> readRecipes(File file,  ConfigurationSection section, ProjectAddon addon) {
        int templateSlot = section.getInt("templateSlot"); // checked
        boolean moreOutputIfMoreTemplates = section.getBoolean("moreOutputIfMoreTemplates", false);

        List<CustomTemplateMachineRecipe> result = new ArrayList<>();
        var recipes = section.getConfigurationSection("recipes");
        if (recipes == null) return Collections.emptyList();

        for (String key : recipes.getKeys(false)) {
            var item = addon.getSfStack(key);

            if (item == null) {
                Debug.error(file, section, "Unable toitem: " + key);
                continue;
            }

            var innerRecipes = recipes.getConfigurationSection(key);
            if (innerRecipes == null) return Collections.emptyList();

            var r = readRecipes(file, innerRecipes, addon, true);
            if (r != null) {
                for (var recipe : r) {
                    result.add(new CustomTemplateMachineRecipe(templateSlot, item, recipe, moreOutputIfMoreTemplates));
                }
            }
        }
        return result;
    }

    @Override
    public @Nullable MachineTicker create(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon) {
        int templateSlot = section.getInt("templateSlot");
        if (templateSlot < 0 || templateSlot > 53) {
            Debug.error(file, section, "MissingConfiguration error 'slot' (templateSlot)", 0, 53);
            return null;
        }

        var recipes = read(file, section, addon);
        if (recipes == null) return null;
        boolean fasterIfMoreTemplates = section.getBoolean("fasterIfMoreTemplates", false);
        return new TemplateRecipeMachineTicker() {
            @Override
            public int getTemplateSlot() {
                return templateSlot;
            }

            @Override
            public boolean isFasterIfMoreTemplates() {
                return fasterIfMoreTemplates;
            }

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

            @Override
            public AdvancedCustomMachine getMachine() {
                return sf;
            }
        };
    }
}
