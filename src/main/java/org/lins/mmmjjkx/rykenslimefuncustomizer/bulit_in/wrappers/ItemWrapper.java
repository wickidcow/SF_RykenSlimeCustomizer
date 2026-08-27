package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@Data
public class ItemWrapper implements Cloneable {
    private final ItemStack stack;
    private int amount;

    public ItemWrapper(ItemStack stack) {
        this(stack, stack.getAmount());
    }

    public ItemWrapper(ItemStack stack, int amount) {
        // Do not use ItemStack#asOne here. That calls clone() polymorphically,
        // and SlimefunItemStack#clone recreates the Slimefun stack and requires
        // non-null ItemMeta. AIR/placeholder-style inputs can legitimately have
        // no ItemMeta, which made malformed or optional recipes crash loading.
        this.stack = new ItemStack(stack);
        this.stack.setAmount(1);
        this.amount = amount;
    }

    public Material getType() {
        return stack.getType();
    }

    public int getMaxStackSize() {
        return stack.getMaxStackSize();
    }

    public void addAmount(int amount) {
        this.amount += amount;
    }

    public static ItemWrapper create(ItemStack stack) {
        return new ItemWrapper(stack);
    }

    public int countStack(int amount) {
        return (amount + stack.getMaxStackSize() - 1) / stack.getMaxStackSize();
    }

    public int countStack() {
        return countStack(amount);
    }

    public List<ItemStack> toStacks() {
        return toStacks(amount);
    }

    public List<ItemStack> toStacks(int amt) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < countStack(amt) - 1; i++) {
            ItemStack copy = new ItemStack(stack);
            copy.setAmount(stack.getMaxStackSize());
            list.add(copy);
        }
        int left = amt - (stack.getMaxStackSize() * Math.max(0, countStack(amt) - 1));
        if (left > 0) {
            ItemStack copy = new ItemStack(stack);
            copy.setAmount(left);
            list.add(copy);
        }
        return list;
    }

    public ItemStack asOneStack() {
        ItemStack copy = new ItemStack(stack);
        copy.setAmount(Math.min(stack.getMaxStackSize(), getAmount()));
        return copy;
    }

    @Override
    public ItemWrapper clone() {
        return new ItemWrapper(new ItemStack(stack), amount);
    }
}
