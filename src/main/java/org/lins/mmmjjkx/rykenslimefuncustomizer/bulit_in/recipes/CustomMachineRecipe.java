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

import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.AsyncChanceRecipeTask;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.ItemWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.BlockMenuUtil;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.StackUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.RecipesHolder.RECIPE_INPUT;
import static org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.RecipesHolder.RECIPE_OUTPUT;

@NullMarked
@Getter
public class CustomMachineRecipe extends AbstractRecipe {
    private final List<InputWrapper> inputs;
    private final List<ItemWrapper> outputs;
    private final IntList chances; // keep field name for LogiTech reflection
    private final boolean chooseOneIfHas; // keep field name for LogiTech reflection
    private final boolean forDisplay; // keep field name for LogiTech reflection
    private final boolean hide;
    private final boolean noConsumeAll;

    // Compatibility field retained for LogiTech reflection.
    @Deprecated
    @ApiStatus.Obsolete
    @ApiStatus.Internal
    private final IntList noConsume;

    private static IntList buildNoConsumeIndexes(List<InputWrapper> inputs) {
        int size = inputs.stream()
            .mapToInt(wrapper -> wrapper.getNoConsume().getLinkedNoConsume().size()
                + (wrapper.getNoConsume().getNoConsumeAmountExcludeLinked() + wrapper.getMaxStackSize() - 1)
                / wrapper.getMaxStackSize())
            .sum();
        IntList indexes = new IntArrayList();
        for (int i = 1; i <= size; i++) {
            indexes.add(i);
        }
        return indexes;
    }

    public boolean isChooseOne() {
        return chooseOneIfHas;
    }

    public boolean isForDisplayOnly() {
        return forDisplay;
    }

    public boolean isNoConsumeAll() {
        return noConsumeAll;
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
        return noConsumeAll;
    }

    public CustomMachineRecipe(
            int seconds,
            List<InputWrapper> input,
            ItemStack[] output,
            IntList chances,
            boolean chooseOne,
            boolean forDisplayOnly,
            boolean hide,
            boolean noConsumeAll) {
        this(input, output, seconds * 2, chances, chooseOne, forDisplayOnly, hide, noConsumeAll);
    }

    public CustomMachineRecipe(
        List<InputWrapper> input,
        ItemStack[] output,
        int ticks,
        IntList chances,
        boolean chooseOne,
        boolean forDisplayOnly,
        boolean hide,
        boolean noConsumeAll) {
        super(input.stream().flatMap(InputWrapper::asArrayStream).toList().toArray(new ItemStack[0]), output, ticks);

        this.inputs = input;
        this.outputs = InvIndex.mergeItems(output);
        this.chances = chances;
        this.chooseOneIfHas = chooseOne;
        this.forDisplay = forDisplayOnly;
        this.hide = hide;
        this.noConsumeAll = noConsumeAll;
        this.noConsume = buildNoConsumeIndexes(input);
    }

    public static CustomMachineRecipe from(MachineRecipe mr) {
        if (mr instanceof CustomMachineRecipe cmr) return cmr;
        return new CustomMachineRecipe(
            CommonUtils.readInputs(mr.getInput(), false),
            mr.getOutput(),
            mr.getTicks(),
            CustomMachineRecipe.fullChance(mr.getOutput().length),
            false,
            false,
            false,
            false
        );
    }

    public static IntList fullChance(int size) {
        IntList list = new IntArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(100);
        }
        return list;
    }

    public static List<CustomMachineRecipe> from(List<MachineRecipe> mrs) {
        return mrs.stream().map(CustomMachineRecipe::from).toList();
    }

    public List<ItemStack> getMatchChanceResult(boolean chooseOne) {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < getOutput().length; i++) {
            if (matchChance(chances.getInt(i))) {
                stacks.add(getOutput()[i].clone());
            }
        }

        if (stacks.isEmpty()) return stacks;

        if (chooseOne) {
            int index = new Random().nextInt(stacks.size());
            return List.of(stacks.get(index));
        }

        return stacks;
    }

    public static boolean matchChance(int chance) {
        if (chance >= 100) return true;
        if (chance < 1) return false;

        int result = new SecureRandom().nextInt(100);
        return result < chance;
    }

    @Override
    public void formatGUI(Player p, ChestMenu inv, int[] inputSlots, int[] outputSlots) {
        boolean overflowed = false;
        int i = 0;
        // input
        for (var wrapper : getInputs()) {
            for (ItemStack itemStack : wrapper.toStacks()) {
                if (i == inputSlots.length) {
                    overflowed = true;
                }
                if (overflowed) break;
                ClickableDisplay.display(p, inv, inputSlots[i++], Recipe.fakeItem(itemStack));
            }
            if (overflowed) break;
        }
        if (overflowed) {
            inv.addItem(inputSlots[inputSlots.length - 1], Recipe.asInputInfoStack(getInputs()), ChestMenuUtils.getEmptyClickHandler());
        }

        if (isChooseOne()) {
            DoubleList weightedChance = new DoubleArrayList();
            int allChance = getChances().intStream().sum();
            for (i = 0; i < getOutput().length; i++) {
                int chance = getChances().getInt(i);
                weightedChance.add((double) chance / allChance);
            }

            List<ItemStack> cycles = new ArrayList<>();
            for (i = 0; i < getOutput().length; i++) {
                cycles.add(Recipe.tagOutputChance(getOutput()[i], weightedChance.getDouble(i)));
            }

            AsyncChanceRecipeTask task = new AsyncChanceRecipeTask();
            task.add(outputSlots[0], cycles);
            task.start(inv);
            return;
        }

        overflowed = false;
        for (i = 0; i < getOutput().length; i++) {
            if (i == outputSlots.length) {
                overflowed = true;
            }
            if (overflowed) break;

            ItemStack output = getOutput()[i];
            int chance = getChances().getInt(i);
            ClickableDisplay.display(p, inv, outputSlots[i], Recipe.tagOutputChance(output, chance));
        }
        if (overflowed) {
            inv.addItem(outputSlots[outputSlots.length - 1], Recipe.asOutputInfoStack(getOutput(), getChances()), ChestMenuUtils.getEmptyClickHandler());
        }
    }

    @Override
    public boolean matches(InvIndex index, boolean consumeItems) {
        if (this.isForDisplayOnly()) return false;

        for (var input : inputs) {
            if (!index.matches(input)) {
                return false;
            }
        }

        int slotsOutputTakes = outputs.stream().map(ItemWrapper::countStack).mapToInt(i -> i).sum();
        if (index.getEmptyOutputSlots() < slotsOutputTakes) {
            if (!BlockMenuUtil.fits(index.getInv(), outputs, index.getInv().getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW))) {
                return false;
            }
        }

        if (consumeItems) {
            if (isNoConsumeAll()) return true;
            var inv = index.getInv();
            for (var wrapper : inputs) {
                for (var slotWrapper : index.getInputs()) {
                    if (!StackUtils.itemsMatch(slotWrapper.getStack(), wrapper.getStack())) continue;
                    int left = wrapper.getConsumeAmount();
                    if (left <= 0) continue;
                    for (var entry : slotWrapper.getAmounts().int2IntEntrySet()) {
                        int slot = entry.getIntKey();
                        if (wrapper.getNoConsume().getLinkedNoConsume().contains(slot)) continue;
                        int curr = entry.getIntValue();
                        if (curr <= left) {
                            left -= curr;
                            inv.consumeItem(slot, curr);
                        } else {
                            inv.consumeItem(slot, left);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return true;
    }

    public List<ItemWrapper> cloneOutputs() {
        List<ItemWrapper> result = new ArrayList<>();
        for (var output : outputs) {
            result.add(output.clone());
        }
        return result;
    }

    @Override
    public boolean pushOutputs(BlockMenu inv) {
        BlockMenuUtil.pushItem(inv, getMatchChanceResult(isChooseOne()), inv.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW));
        return true;
    }

    @Override
    public <T extends MachineRecipe & Recipe> T asMachineRecipe() {
        return (T) this;
    }

    @Override
    public ItemStack getDisplayInput(int index) {
        if (inputs.size() == 1 && inputs.getFirst().countStack() == 1) return inputs.getFirst().asOneStack();
        return Recipe.tagItem(RECIPE_INPUT, index);
    }

    @Override
    public ItemStack getDisplayOutput(int index) {
        if (outputs.size() == 1) {
            ItemStack out = outputs.getFirst().asOneStack().clone();
            CommonUtils.addLore(out, true, CommonUtils.richFormatSeconds(getTicks() / 2));
            return out;
        }
        return Recipe.tagItem(RECIPE_OUTPUT, index);
    }
}
