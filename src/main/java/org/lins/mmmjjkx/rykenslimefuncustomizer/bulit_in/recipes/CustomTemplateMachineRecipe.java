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
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.ItemWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.BlockMenuUtil;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.StackUtils;

import java.util.ArrayList;
import java.util.List;

import static org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.RecipesHolder.RECIPE_INPUT;

@NullMarked
@Getter
public class CustomTemplateMachineRecipe extends CustomMachineRecipe {
    private final int templateSlot;
    private final ItemStack templateStack;
    private final boolean moreOutputIfMoreTemplates;
    public CustomTemplateMachineRecipe(
        int templateSlot,
        ItemStack templateStack,
        CustomMachineRecipe recipe,
        boolean moreOutputIfMoreTemplates
    ) {
        this(recipe.getTicks() / 2, recipe.getInputs(), recipe.getOutput(), recipe.getChances(), recipe.isChooseOne(), recipe.isForDisplayOnly(), recipe.isHide(), recipe.isNoConsumeAll(), templateSlot, templateStack, moreOutputIfMoreTemplates);
    }
    public CustomTemplateMachineRecipe(
            int seconds,
            List<InputWrapper> input,
            ItemStack[] output,
            IntList chances,
            boolean chooseOne,
            boolean forDisplayOnly,
            boolean hide,
            boolean noConsumeAll,
            int templateSlot,
            ItemStack templateStack,
            boolean moreOutputIfMoreTemplates) {
        super(seconds, input, output, chances, chooseOne, forDisplayOnly, hide, noConsumeAll);
        this.templateSlot = templateSlot;
        this.templateStack = templateStack;
        this.moreOutputIfMoreTemplates = moreOutputIfMoreTemplates;
    }

    @Override
    public void formatGUI(Player p, ChestMenu inv, int[] inputSlots, int[] outputSlots) {
        super.formatGUI(p, inv, inputSlots, outputSlots);
        ClickableDisplay.display(p, inv, templateSlot, templateStack);
    }

    @Override
    public List<ItemStack> getMatchChanceResult(boolean chooseOne) {
        List<ItemStack> itemStacks = new ArrayList<>();

        for (int i = 0; i < getOutput().length; i++) {
            if (matchChance(getChances().getInt(i))) {
                var output = getOutput()[i].clone();
                if (moreOutputIfMoreTemplates) {
                    output.setAmount(output.getAmount() * templateStack.getAmount());
                }
                itemStacks.add(output);
            }
        }

        return itemStacks;
    }

    @Override
    public boolean matches(InvIndex index, boolean consumeItems) {
        if (this.isForDisplayOnly()) return false;
        if (!StackUtils.itemsMatch(index.getItemInSlot(templateSlot), templateStack)) return false;
        return super.matches(index, consumeItems);
    }

    @Override
    public boolean pushOutputs(BlockMenu inv) {
        BlockMenuUtil.pushItem(inv, getMatchChanceResult(isChooseOne()), inv.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW));
        return true;
    }

    public ItemStack getDisplayTemplate() {
        ItemStack templateItem = templateStack.clone();
        CommonUtils.addLore(templateItem, true, "&b&l&o*Template item is not consumed*");
        return templateItem;
    }

    @Override
    public ItemStack getDisplayInput(int index) {
        if (getInputs().isEmpty()) {
            return getDisplayTemplate();
        } else {
            return Recipe.tagItem(RECIPE_INPUT, index);
        }
    }
}
