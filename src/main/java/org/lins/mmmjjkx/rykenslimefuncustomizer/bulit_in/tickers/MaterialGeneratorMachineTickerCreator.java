package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.matl114.logitech.utils.UtilClass.RecipeClass.MGeneratorRecipe;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.AbstractRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomMachineRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.MGeneratorRecipeRSC;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public class MaterialGeneratorMachineTickerCreator implements TickerCreator {
    @Override
    public @Nullable List<? extends Recipe> read(File file, ConfigurationSection section, ProjectAddon addon) {
        ConfigurationSection outputItems = section.getConfigurationSection("outputs");
        List<ItemStack> outputs = new ArrayList<>();
        IntList chances = new IntArrayList();
        boolean chooseOne = section.getBoolean("chooseOne", false);

        if (outputItems != null) {
            for (String key : outputItems.getKeys(false)) {
                ConfigurationSection outputCfg = outputItems.getConfigurationSection(key);
                if (outputCfg == null) break;
                var item = CommonUtils.readItem(file, outputCfg, addon);
                if (item == null) {
                    Debug.error(file, outputCfg, "itemConfiguration error (outputs)");
                    continue;
                }

                int chance = CommonUtils.clamp(outputCfg.getInt("chance", 100), 1, 100,
                    file, outputCfg, "' (chance) '");

                outputs.add(item);
                chances.add(chance);
            }
        }

        ConfigurationSection outputItem = section.getConfigurationSection("outputItem");
        if (outputItem != null) {
            var item = CommonUtils.readItem(file, outputItem, addon);
            if (item == null) {
                Debug.error(file, outputItem, "itemConfiguration error (outputItem)");
            } else {
                int chance = CommonUtils.clamp(outputItem.getInt("chance", 100), 1, 100,
                    file, outputItem, "' (chance) '");

                outputs.add(item);
                chances.add(chance);
            }
        }

        int tickRate = section.getInt("tickRate");
        if (tickRate < 1) {
            Debug.error(file, section, "Configuration error 'recipe' (tickRate)", 1, Integer.MAX_VALUE);
            return null;
        }
        return List.of(createMaterialGeneratorRecipe(
            outputs.stream().toList().toArray(new ItemStack[0]),
            tickRate,
            chances,
            chooseOne
        ));
    }

    public static <T extends MachineRecipe & Recipe> T createMaterialGeneratorRecipe(ItemStack[] outputs, int tickRate, IntList chances, boolean chooseOne) {
        if (RykenSlimefunCustomizer.logitech) {
            return new MGeneratorRecipeRSC(tickRate, outputs, chances, chooseOne).asMachineRecipe();
        }
        return new CustomMachineRecipe(List.of(), outputs, tickRate, chances, chooseOne, false, false, false).asMachineRecipe();
    }

    @Override
    public @Nullable MachineTicker create(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon) {
        var recipes = read(file, section, addon);
        if (recipes == null) return null;
        int status = section.getInt("status", -1);
        if (status < -1) {
            Debug.error(file, section, "MissingConfiguration error '' (status)", -1, 53);
            return null;
        }

        return new MaterialGeneartorMachineTicker() {
            @Override
            public @Range(from = -1, to = 53) int getStatusSlot() {
                return status;
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
            public AdvancedCustomMachine getMachine() {
                return sf;
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
