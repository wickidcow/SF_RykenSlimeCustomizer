package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public interface RecipesHolder extends ProcessorHolder, RecipeDisplayItem {
    ItemStack RECIPE_INPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&aMultiple item input", "", "&2> &aClick to view");
    ItemStack RECIPE_OUTPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&aMultiple item output", "", "&2> &aClick to view");

    List<? extends Recipe> getRecipes();

    default boolean isHideAllRecipes() {
        return false;
    }

    default @Nullable Recipe findNextRecipe(InvIndex index, @Nullable Recipe lastRecipe) {
        for (var recipe : getRecipes()) {
            if (recipe != lastRecipe && recipe.matches(index)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    default List<ItemStack> getDisplayRecipes() {
        List<ItemStack> displayRecipes = new ArrayList<>();

        if (isHideAllRecipes()) {
            return displayRecipes;
        }

        int i = 0;
        for (var recipe : getRecipes()) {
            if (recipe.isHide()) continue;
            displayRecipes.add(recipe.getDisplayInput(i));
            displayRecipes.add(recipe.getDisplayOutput(i));
            i++;
        }

        return displayRecipes;
    }

    default String getMachineIdentifier() {
        return getMachine().getId();
    }

    boolean preTick(Location location);

    AdvancedCustomMachine getMachine();
}
