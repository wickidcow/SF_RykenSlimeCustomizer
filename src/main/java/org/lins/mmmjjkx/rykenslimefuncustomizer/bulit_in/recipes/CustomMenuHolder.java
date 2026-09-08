package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@NullMarked
public interface CustomMenuHolder extends InventoryBlock {
    int DEFAULT_PROGRESS_SLOT = 22;
    ItemStack DEFAULT_PROGRESS_BAR = new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE);
    int[] DEFAULT_BORDER = {0, 1, 2, 3, 4, 5, 6, 7, 8, 13, 31, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    int[] DEFAULT_BORDER_IN = {9, 10, 11, 12, 18, 21, 27, 28, 29, 30};
    int[] DEFAULT_BORDER_OUT = {14, 15, 16, 17, 23, 26, 32, 33, 34, 35};

    @Nullable CustomMenu getCustomMenu();

    static void constructMenu(ChestMenu menu, int progressSlot, @Nullable ItemStack progressBar) {
        constructMenu(menu);

        if (progressBar != null) {
            menu.addItem(progressSlot, progressBar, ChestMenuUtils.getEmptyClickHandler());
        }
    }

    static void constructMenu(ChestMenu menu) {
        for (int i : DEFAULT_BORDER) {
            menu.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : DEFAULT_BORDER_IN) {
            menu.addItem(i, ChestMenuUtils.getInputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : DEFAULT_BORDER_OUT) {
            menu.addItem(i, ChestMenuUtils.getOutputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }
    }

    default ItemStack getProgressBar() {
        if (getCustomMenu() == null || getCustomMenu().getProgressBar() == null) {
            return DEFAULT_PROGRESS_BAR;
        }
        return getCustomMenu().getProgressBar();
    }

    default int getProgressSlot() {
        if (getCustomMenu() == null || getCustomMenu().getProgressSlot() == -1) {
            return DEFAULT_PROGRESS_SLOT;
        }
        return getCustomMenu().getProgressSlot();
    }

    @Override
    @Deprecated
    default void createPreset(SlimefunItem item, String title, Consumer<BlockMenuPreset> setup) {
        createPreset(item, title, setup, null);
    }

    default void createPreset(SlimefunItem item, String title, @Nullable Consumer<BlockMenuPreset> setup, @Nullable BiConsumer<BlockMenu, Block> onNewInstance) {
        new BlockMenuPreset(item.getId(), title) {

            @Override
            public void init() {
                if (setup != null) setup.accept(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                if (onNewInstance != null) onNewInstance.accept(menu, b);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.isOp() || p.hasPermission("slimefun.inventory.bypass")
                    || (item.canUse(p, false) && Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK));
            }
        };
    }
}
