package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in;

import com.balugaq.jeg.utils.GuideUtil;
import com.balugaq.jeg.utils.clickhandler.OnDisplay;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomMenuHolder;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Keys;

import java.util.List;

@Getter
@NullMarked
@SuppressWarnings("deprecation")
public class MachineMenuPreviewGroup extends FlexItemGroup {
    public static final ItemStack TOP_BACKGROUND = new CustomItemStack(Material.PURPLE_STAINED_GLASS_PANE, " ", " ");
    private final Player player;
    private final @Nullable CustomMenu menu;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final MachineRecipe recipe;
    private final SlimefunItem sf;

    public MachineMenuPreviewGroup(Player player, @Nullable CustomMenu menu, int[] inputSlots, int[] outputSlots, MachineRecipe recipe, SlimefunItem sf) {
        super(Keys.newKey("placeholder"), new ItemStack(Material.STONE));
        this.player = player;
        this.menu = menu;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.recipe = recipe;
        this.sf = sf;
    }

    @Override
    public boolean isVisible(Player p, PlayerProfile profile, SlimefunGuideMode layout) {
        return false;
    }

    @Override
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        profile.getGuideHistory().add(this, 0);

        ChestMenu inv = presetMenu(p, profile, mode, menu);

        // add templates;
        // add inputs;
        // add outputs;
        if (recipe instanceof Recipe rp) {
            rp.formatGUI(p, inv, inputSlots, outputSlots);
        }

        // add progress bar;
        int progressSlot;
        ItemStack progressBar;
        if (getMenu() == null || getMenu().getProgressSlot() == -1) {
            progressSlot = CustomMenuHolder.DEFAULT_PROGRESS_SLOT;
            progressBar = CustomMenuHolder.DEFAULT_PROGRESS_BAR;
        } else {
            progressSlot = getMenu().getProgressSlot();
            progressBar = getMenu().getProgressBar();
        }

        inv.addItem(progressSlot, new CustomItemStack(progressBar, CommonUtils.richFormatSeconds(recipe.getTicks() / 2)));
        tryAddBackButton(inv, profile, mode);

        for (int i = 0; i < inv.getSize(); i++) {
            var stack = inv.getItemInSlot(i);
            if (stack == null || stack.getType() == Material.AIR) {
                inv.addMenuClickHandler(i, ChestMenuUtils.getEmptyClickHandler());
            }
        }

        inv.open(p);
    }

    private static ChestMenu presetMenu(Player p, PlayerProfile profile, SlimefunGuideMode mode, @Nullable CustomMenu menu) {
        var inv = new ChestMenu(RykenSlimefunCustomizer.jeg
            ? GuideUtil.getGuideTitle(mode)
            : Slimefun.getLocalization().getMessage(p, "guide.title.main"));

        if (menu != null) {
            menu.apply(inv);
        } else {
            CustomMenuHolder.constructMenu(inv, CustomMenuHolder.DEFAULT_PROGRESS_SLOT, CustomMenuHolder.DEFAULT_PROGRESS_BAR);
        }

        return inv;
    }

    private void tryAddBackButton(ChestMenu inv, PlayerProfile profile, SlimefunGuideMode mode) {
        // add back button
        if (inv.getSize() != 54) {
            // automatically insert a line at the top
            // bBBBMBBBB
            int newSize = inv.getSize() + 9;
            for (int i = newSize - 1; i >= 9; i--) {
                inv.addItem(i, inv.getItemInSlot(i - 9), ChestMenuUtils.getEmptyClickHandler());
            }

            for (int i = 1; i < 9; i++) {
                inv.addItem(i, TOP_BACKGROUND, ChestMenuUtils.getEmptyClickHandler());
            }

            addMachineButton(inv, 4, profile, mode);
            addBackButton(inv, 0, profile, mode);

            return;
        }

        // find a background item and replace it.
        for (int i = 0; i < 54; i++) {
            if (isBackground(inv.getItemInSlot(i))) {
                addBackButton(inv, i, profile, mode);
                return;
            }
        }

        // find a empty slot and replace it
        for (int i = 0; i < 54; i++) {
            ItemStack stack = inv.getItemInSlot(i);
            if (stack == null || stack.getType().isAir()) {
                addBackButton(inv, i, profile, mode);
                return;
            }
        }
    }

    private void addMachineButton(ChestMenu inv, int slot, PlayerProfile profile, SlimefunGuideMode mode) {
        if (RykenSlimefunCustomizer.jeg) {
            OnDisplay.Item.display(player, sf, OnDisplay.Item.DisplayType.Normal, GuideUtil.getLastGuide(player))
                .at(inv, slot, 1);
            return;
        }

        var guide = Slimefun.getRegistry().getSlimefunGuide(mode);
        inv.addItem(slot, sf.getItem(), (p, s, i, a) -> {
            guide.displayItem(profile, sf, true);
            return false;
        });
    }

    private boolean isBackground(@Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.getType().name().endsWith("_STAINED_GLASS_PANE")) return false;

        if (!itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        String name = meta.getDisplayName();
        if (!ChatColor.stripColor(name).isBlank()) return false;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return true;
        for (String s : lore) {
            if (!ChatColor.stripColor(s).isBlank()) return false;
        }

        return true;
    }

    private void addBackButton(ChestMenu inv, int slot, PlayerProfile profile, SlimefunGuideMode mode) {
        if (RykenSlimefunCustomizer.jeg) {
            GuideUtil.addBackButton(inv, List.of(slot), profile, player);
            return;
        }

        var guide = Slimefun.getRegistry().getSlimefunGuide(mode);
        GuideHistory history = profile.getGuideHistory();
        ItemStack backIcon;
        if (history.size() > 1) {
            backIcon = ChestMenuUtils.getBackButton(
                player,
                "",
                ChatColor.GRAY + Slimefun.getLocalization().getMessage(player, "guide.back.guide"));
        } else {
            backIcon = ChestMenuUtils.getBackButton(
                player,
                "",
                "&fLeft click: &7Previous page",
                "&fShift + Left click: &7Main menu"
            );
        }
        inv.addItem(slot, backIcon, (p, s, i, a) -> {
            GuideHistory guideHistory = profile.getGuideHistory();
            if (history.size() == 1 || a.isShiftClicked()) {
                guide.openMainMenu(profile, guideHistory.getMainMenuPage());
                return false;
            }

            history.goBack(guide);
            return false;
        });
    }
}
