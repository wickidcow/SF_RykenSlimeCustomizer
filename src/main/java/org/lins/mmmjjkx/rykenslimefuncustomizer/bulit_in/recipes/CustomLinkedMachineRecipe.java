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
package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.AsyncChanceRecipeTask;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.LinkedOutput;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.BlockMenuUtil;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.StackUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NullMarked
@Getter
public class CustomLinkedMachineRecipe extends AbstractRecipe {
    ItemStack LINKED_RECIPE_INPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&aLinked recipe input", "", "&2> &aClick to view");
    ItemStack LINKED_RECIPE_OUTPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&aLinked recipe output", "", "&2> &aClick to view");
    private final Map<Integer, ItemStack> linkedInput;
    private final LinkedOutput linkedOutput;
    private final IntSet noConsumeIndexes;
    private final boolean chooseOneIfHas; // keep field name for LogiTech reflection
    private final boolean forDisplay; // keep field name for LogiTech reflection
    private final boolean hide;
    private final int saveAmount;
    private final boolean noConsume; // keep field name for LogiTech reflection

    public boolean isChooseOne() {
        return chooseOneIfHas;
    }

    public boolean isForDisplayOnly() {
        return forDisplay;
    }

    public boolean isNoConsumeAll() {
        return noConsume;
    }

    @Deprecated
    public boolean isChooseOneIfHas() {
        return chooseOneIfHas;
    }

    @Deprecated
    public boolean isForDisplay() {
        return forDisplay;
    }

    @Deprecated
    public boolean isNoConsume() {
        return noConsume;
    }

    @Override
    public void formatGUI(Player p, ChestMenu inv, int[] inputSlots, int[] outputSlots) {
        // input
        for (var e : linkedInput.entrySet()) {
            var slot = e.getKey();
            var stack = e.getValue();
            if (getNoConsumeIndexes().contains(slot)) {
                stack = Recipe.tagNoConsume(stack);
            }
            ClickableDisplay.display(p, inv, slot, stack);
        }

        // output - choose one
        if (isChooseOne()) {
            List<ItemStack> allStacks = new ArrayList<>();
            IntList allChances = new IntArrayList();
            for (var e : linkedOutput.linkedOutput().entrySet()) {
                int slot = e.getKey();
                allStacks.add(e.getValue());
                allChances.add(linkedOutput.linkedChances().get(slot));
            }
            for (int i = 0; i < linkedOutput.freeOutput().length; i++) {
                allStacks.add(linkedOutput.freeOutput()[i]);
                allChances.add(linkedOutput.freeChance().getInt(i));
            }

            // normalize
            DoubleList weightedChance = new DoubleArrayList();
            int allChance = allChances.intStream().sum();
            for (int i = 0; i < allStacks.size(); i++) {
                int chance = allChances.getInt(i);
                weightedChance.add((double) chance / allChance);
            }

            List<ItemStack> cycles = new ArrayList<>();
            for (int i = 0; i < allStacks.size(); i++) {
                cycles.add(Recipe.tagOutputChance(allStacks.get(i), weightedChance.getDouble(i)));
            }

            AsyncChanceRecipeTask task = new AsyncChanceRecipeTask();
            task.add(outputSlots[0], cycles);
            task.start(inv);
            return;
        }

        // output - normal
        IntList emptyOutputSlots = new IntArrayList();
        for (int slot : outputSlots) {
            emptyOutputSlots.add(slot);
        }

        for (var e : linkedOutput.linkedOutput().entrySet()) {
            Integer slot = e.getKey();
            var stack = e.getValue();
            int chance = linkedOutput.linkedChances().get(slot);
            ClickableDisplay.display(p, inv, slot, Recipe.tagOutputChance(stack, chance));
            emptyOutputSlots.remove(slot); // remove value
        }

        boolean overflowed = false;
        for (int i = 0; i < linkedOutput.freeOutput().length; i++) {
            if (i == emptyOutputSlots.size()) {
                overflowed = true;
            }
            if (overflowed) break;

            var stack = linkedOutput.freeOutput()[i];
            var chance = linkedOutput.freeChance().getInt(i);
            ClickableDisplay.display(p, inv, emptyOutputSlots.getInt(i), Recipe.tagOutputChance(stack, chance));
        }
        if (overflowed) {
            // generate info stack at the last slot
            inv.addItem(emptyOutputSlots.getLast(), Recipe.asOutputInfoStack(linkedOutput.freeOutput(), linkedOutput.freeChance()), ChestMenuUtils.getEmptyClickHandler());
        }
    }

    @Override
    public boolean matches(InvIndex index) {
        return super.matches(index);
    }

    @Override
    public boolean pushOutputs(BlockMenu inv) {
        var slots = inv.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
        return BlockMenuUtil.pushItem(inv, linkedOutput, isChooseOne(), slots).isEmpty();
    }

    @Override
    public ItemStack getDisplayInput(int index) {
        return Recipe.tagItem(LINKED_RECIPE_INPUT, index);
    }

    @Override
    public ItemStack getDisplayOutput(int index) {
        return Recipe.tagItem(LINKED_RECIPE_OUTPUT, index);
    }

    @Override
    public boolean matches(InvIndex index, boolean consumeItems) {
        for (var e : linkedInput.entrySet()) {
            ItemStack stack = index.getItemInSlot(e.getKey());
            if (stack == null
                || stack.getAmount() < e.getValue().getAmount() + saveAmount
                || !StackUtils.itemsMatch(e.getValue(), stack, true, false)) {
                return false;
            }
        }

        if (!BlockMenuUtil.fits(index.getInv(), linkedOutput)) return false;

        if (consumeItems) {
            if (isNoConsumeAll()) return true;
            for (var e : linkedInput.entrySet()) {
                if (getNoConsumeIndexes().contains(e.getKey())) continue;
                ItemStack stack = index.getItemInSlot(e.getKey());
                if (stack != null) {
                    stack.setAmount(stack.getAmount() - e.getValue().getAmount());
                }
            }
        }
        return true;
    }

    public CustomLinkedMachineRecipe(
            int seconds,
            Map<Integer, ItemStack> input,
            LinkedOutput linkedOutput,
            IntSet noConsume,
            boolean chooseOne,
            boolean forDisplayOnly,
            boolean hide,
            int saveAmount,
            boolean noConsumeAll) {
        super(seconds, input.values().toArray(new ItemStack[0]), linkedOutput.toArray());
        this.linkedInput = input;
        this.linkedOutput = linkedOutput;
        this.noConsumeIndexes = noConsume;
        this.chooseOneIfHas = chooseOne;
        this.forDisplay = forDisplayOnly;
        this.hide = hide;
        this.saveAmount = saveAmount;
        this.noConsume = noConsumeAll;
    }

    @Override
    public <T extends MachineRecipe & Recipe> T asMachineRecipe() {
        return (T) this;
    }
}
