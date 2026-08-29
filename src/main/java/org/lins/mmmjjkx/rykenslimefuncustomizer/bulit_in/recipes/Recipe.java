package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes;

import com.balugaq.jeg.utils.GuideUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import it.unimi.dsi.fastutil.ints.IntList;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.MachineMenuPreviewGroup;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InputWrapper;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.wrappers.InvIndex;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NullMarked
public interface Recipe {
    NamespacedKey RECIPE_INDEX_KEY = Keys.newKey("rsc_recipe_index");
    NamespacedKey FAKE_ITEM = Keys.newKey("fake_item");

    static ItemStack tagItem(ItemStack item, int index) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(RECIPE_INDEX_KEY, PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack fakeItem(ItemStack ori) {
        return new CustomItemStack(ori, meta -> {
            meta.getPersistentDataContainer().set(FAKE_ITEM, PersistentDataType.BOOLEAN, true);
        });
    }

    static ItemStack asInputInfoStack(List<InputWrapper> wrappers) {
        List<String> lore = new ArrayList<>();
        for (var wrapper : wrappers) {
            lore.add(CommonUtils.getItemName(wrapper.getStack()) + CMIChatColor.translate("&e x" + wrapper.getAmount()));
            if (wrapper.getNoConsume().getNoConsumeAmountExcludeLinked() != 0) {
                lore.add(CMIChatColor.translate("&e" + wrapper.getNoConsume().getNoConsumeAmountExcludeLinked() + " item(s) are not consumed"));
            }
            if (!wrapper.getNoConsume().getLinkedNoConsume().isEmpty()) {
                lore.add(CMIChatColor.translate("&eItems in slots " + Arrays.toString(wrapper.getNoConsume().getLinkedNoConsume().toIntArray()) + " are not consumed"));
            }
        }

        return new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&eItems: ", lore);
    }

    static ItemStack asOutputInfoStack(ItemStack[] outputs, IntList chances) {
        List<String> lore = new ArrayList<>();
        for (int i = 0; i < outputs.length; i++) {
            var output = outputs[i];
            lore.add(getOutputChanceLore(chances.getInt(i)) + ": " + CommonUtils.getItemName(output) + CMIChatColor.translate("&e x" + output.getAmount()));
        }

        return new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&eItems: ", lore);
    }

    static String getOutputChanceLore(double chance) {
        String cs = String.format("%.1f", chance); // 保留 1 位小数
        return CMIChatColor.translate("&b" + cs + "% &achance to output");
    }

    static String getOutputChanceLore(int chance) {
        return CMIChatColor.translate("&b" + chance + "% &achance to output");
    }

    static ItemStack tagOutputChance(ItemStack item, double chance) {
        chance *= 100;
        if (chance == 100) return item;
        item = item.clone();
        CommonUtils.addLore(item, true, getOutputChanceLore(chance));
        return item;
    }

    static ItemStack tagOutputChance(ItemStack item, int chance) {
        if (chance == 100) return item;
        item = item.clone();
        CommonUtils.addLore(item, true, getOutputChanceLore(chance));
        return item;
    }

    static ItemStack tagNoConsume(ItemStack item) {
        item = item.clone();
        CommonUtils.addLore(item, true, "&dThis item is not consumed");
        return item;
    }

    void formatGUI(Player p, ChestMenu inv, int[] inputSlots, int[] outputSlots);

    default boolean matches(InvIndex index) {
        return matches(index, true);
    }

    default boolean isChooseOne() {
        return false;
    }
    default boolean isForDisplayOnly() {
        return false;
    }
    default boolean isHide() {
        return false;
    }

    boolean matches(InvIndex index, boolean consumeItems);
    int getTicks();
    boolean pushOutputs(BlockMenu inv);
    <T extends MachineRecipe & Recipe> T asMachineRecipe();

    static void openGUI(Player p, @Nullable CustomMenu menu, int[] inputSlots, int[] outputSlots, Recipe recipe, SlimefunItem sf) {
        openGUI(p, menu, inputSlots, outputSlots, (MachineRecipe) recipe.asMachineRecipe(), sf);
    }

    static void openGUI(Player p, @Nullable CustomMenu menu, int[] inputSlots, int[] outputSlots, MachineRecipe mr, SlimefunItem sf) {
        var group = new MachineMenuPreviewGroup(p, menu, inputSlots, outputSlots, mr, sf);
        group.open(p, PlayerProfile.find(p).orElse(null), getGuideMode(p));
    }

    static void openGUI(Player p, AContainer ac, int idx) {
        if (idx >= ac.getMachineRecipes().size()) return;
        openGUI(p, null, ac.getInputSlots(), ac.getOutputSlots(), ac.getMachineRecipes().get(idx), ac);
    }

    static SlimefunGuideMode getGuideMode(Player p) {
        if (RykenSlimefunCustomizer.jeg)
            return GuideUtil.getLastGuideMode(p);
        return SlimefunGuide.getDefaultMode();
    }

    ItemStack getDisplayInput(int index);
    ItemStack getDisplayOutput(int index);
}
