package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NullMarked;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.AbstractRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CraftingRecipeOperation;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public interface MaterialGeneartorMachineTicker extends MachineTicker {
    ItemStack NO_POWER = new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&4Not enough power", "");
    ItemStack NO_SPACE = new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE, "&cNot enough space", "");
    ItemStack PROCESSING = new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, "&aProcessing", "");

    @Override
    default MachineTicker.Type getType() {
        return Type.MATERIAL_GENERATOR;
    }

    @Range(from = -1, to = 53)
    int getStatusSlot();

    @Override
    default boolean preTick(Location location) {
        if (!takeCharge(location)) {
            if (getStatusSlot() == -1) return false;
            BlockMenu inv = StorageCacheUtils.getMenu(location);
            if (inv != null && inv.hasViewer()) {
                inv.replaceExistingItem(getStatusSlot(), NO_POWER);
            }
            return false;
        } else {
            return true;
        }
    }

    default void tick(Location location) {
        if (!canTick(location) || !preTick(location)) return;

        BlockMenu inv = StorageCacheUtils.getMenu(location);
        if (inv == null) return;

        var currentOperation = getCurrentOperation(location);
        if (currentOperation == null) {
            InvIndex index = InvIndex.create(inv);
            if (getOnlyRecipe().matches(index)) {
                currentOperation = new CraftingRecipeOperation(getOnlyRecipe());
                getAdvancedMachineProcessor().startOperation(location, currentOperation);
                if (currentOperation.isFinished()) {
                    finishOperation(currentOperation, inv, location);
                }
            }
            return;
        }

        if (!currentOperation.isFinished()) {
            if (getStatusSlot() != -1 && inv.hasViewer()) {
                inv.replaceExistingItem(getStatusSlot(), PROCESSING);
            }
            currentOperation.addProgress(1);
            return;
        }

        // finish recipe
        finishOperation(currentOperation, inv, location);
    }

    default void finishOperation(CraftingRecipeOperation currentOperation, BlockMenu inv, Location location) {
        if (!currentOperation.getRecipe().pushOutputs(inv)) {
            if (getStatusSlot() != -1 && inv.hasViewer()) {
                inv.replaceExistingItem(getStatusSlot(), NO_SPACE);
            }
        }

        getAdvancedMachineProcessor().endOperation(location);
    }

    default <T extends MachineRecipe & Recipe> T getOnlyRecipe() {
        return getRecipes().getFirst().asMachineRecipe();
    }

    @Override
    default List<ItemStack> getDisplayRecipes() {
        ItemStack speed = new CustomItemStack(
            Material.KNOWLEDGE_BOOK,
            "&a&lSpeed",
            "RSC message" + getOnlyRecipe().getTicks() + "RSC message"
        );
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack gen : getOnlyRecipe().getOutput()) {
            list.add(speed);
            list.add(gen);
        }
        return list;
    }

    @Override
    default int[] getInputSlots() {
        return new int[0];
    }
}
