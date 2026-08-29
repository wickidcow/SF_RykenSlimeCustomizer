package org.lins.mmmjjkx.rykenslimefuncustomizer.customs.groups;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.CommandSafe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.ObjectType;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public interface BaseRSCItemGroup {
    EnumMap<ObjectType, List<Pair<SlimefunItem, ItemGroup>>> blocked = new EnumMap<>(ObjectType.class);
    default ItemGroup getSelf() {
        return (ItemGroup) this;
    }

    void register(SlimefunAddon plugin);

    void addContent(SlimefunItem sf);

    void addContent(String action);

    void addContent(ItemGroup itemGroup);

    default void addContent(BaseRSCItemGroup base) {
        addContent(base.getSelf());
    }

    static BaseRSCItemGroup create(NamespacedKey key, ItemStack item, int tier, ProjectAddon addon, GroupType type, Visible visible, boolean forceHidden, boolean hasParent) {
        if (RykenSlimefunCustomizer.jeg) {
            return new RSCItemGroupJEG(key, item, tier, addon, type, visible, forceHidden, hasParent, 1);
        } else {
            return new RSCItemGroupLegacy(key, item, tier, addon, type, visible, forceHidden, hasParent, 1);
        }
    }

    NamespacedKey getKey();

    default void readAction(String action, SlimefunGuideMode mode, Player p, int slot, ItemStack clickedItem, ClickAction clickAction) {
        if (action.split(" ").length != 2) {
            Debug.warn("RSC: " + getKey().getKey() + "item group: " + action);
            return;
        }

        String type = action.split(" ")[0];
        String content = action.split(" ")[1];
        switch (type) {
            case "link" -> {
                p.sendMessage(CMIChatColor.translate("&eClick here to open the link: "));
                TextComponent link = new TextComponent(content);
                link.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(CMIChatColor.translate("&e" +content)));
                link.setHoverEvent(hoverEvent);

                ClickEvent spigotClickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, content);
                link.setClickEvent(spigotClickEvent);

                p.sendMessage(link);
            }
            case "console" -> {
                if (CommandSafe.isBadCommand(content)) {
                    Debug.danger("RSC: " + getKey().getKey() + "item group,addon!!!");
                    return;
                }
                content = action.replace(type + " ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content.replaceAll("%player%", p.getName()));
            }
            case "open_itemgroup" -> {
                if (content.split(":").length < 2) {
                    Debug.warn(
                        "RSC: " + getKey().getKey() + "item groupitem group NamespacedKey: " + content);
                    return;
                }
                String namespace = content.split(":")[0];
                String key = content.split(":")[1];
                int page = 1;
                if (content.split(":").length > 2) {
                    try {
                        page = Integer.parseInt(content.split(":")[2]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                Optional<PlayerProfile> Oprofile = PlayerProfile.find(p);
                if (Oprofile.isEmpty()) {
                    Debug.warn(
                        "RSC: " + getKey().getKey() + "item groupUnable to PlayerProfile: " + p.getName());
                    return;
                }
                PlayerProfile profile = Oprofile.get();
                for (ItemGroup group : Slimefun.getRegistry().getAllItemGroups()) {
                    if (group.getKey().getNamespace().equals(namespace)
                        && group.getKey().getKey().equals(key)) {
                        SlimefunGuideImplementation implementation =
                            Slimefun.getRegistry().getSlimefunGuide(mode);
                        implementation.openItemGroup(profile, group, page);
                    }
                }
            }
            case "display_slimefunitem" -> {
                Optional<PlayerProfile> Oprofile = PlayerProfile.find(p);
                if (Oprofile.isEmpty()) {
                    Debug.warn(
                        "RSC: " + getKey().getKey() + "item groupUnable to PlayerProfile: " + p.getName());
                    return;
                }
                SlimefunItem item = SlimefunItem.getById(content);
                if (item == null) {
                    Debug.warn(
                        "RSC: " + getKey().getKey() + "item group SlimefunItem ID: " + content);
                    return;
                }
                PlayerProfile profile = Oprofile.get();
                SlimefunGuideImplementation implementation =
                    Slimefun.getRegistry().getSlimefunGuide(mode);
                implementation.displayItem(profile, item, true);
            }
            case "script" -> {
                JavaScriptEval eval = null;
                File file = new File(getProjectAddon().getScriptsFolder(), content + ".js");
                if (!file.exists()) {
                    Debug.warn(
                        "RSC: " + getKey().getKey() + "item groupscript: " + "script " + file.getName());
                } else {
                    eval = JavaScriptEval.create(file, getProjectAddon());
                }

                if (eval != null) {
                    eval.evalFunction("onButtonGroupClick", p, slot, clickedItem, clickAction, mode);
                }
            }
            default -> Debug.warn("RSC: " + getKey().getKey() + "item group: " + action);
        }
    }

    ProjectAddon getProjectAddon();

    static void addItemToGroup(ItemGroup itemGroup, SlimefunItem sf) {
        blocked.computeIfAbsent(ObjectType.fromSlimefunItem(sf), k -> new CopyOnWriteArrayList<>());
        blocked.get(ObjectType.fromSlimefunItem(sf)).add(new Pair<>(sf, itemGroup));
    }

    static void addItemsToGroups() {
        blocked.values().forEach(lst -> {
            for (var pair : lst) {
                addItemToGroup0(pair.getSecondValue(), pair.getFirstValue());
            }
        });
        blocked.clear();
    }

    static void addItemToGroup0(ItemGroup itemGroup, SlimefunItem sf) {
        if (itemGroup instanceof BaseRSCItemGroup group) {
            Debug.debug(() -> "item " + sf + " item group " + group.getKey());
            group.addContent(sf);
            return;
        }
        if (itemGroup instanceof FlexItemGroup) {
            Debug.error("Unable toitem "+ sf + "RSC: " + itemGroup.getKey() + " FlexItemGroup!");
            return;
        }
        Debug.debug(() -> "item " + sf + " item group " + itemGroup.getKey());
        itemGroup.add(sf);
    }

    default boolean isContentVisibleInGroup(Object content, Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        switch (content) {
            case RSCItemGroupJEG itemGroup -> { return itemGroup.isVisibleInNested(p, profile, mode); }
            case RSCItemGroupLegacy itemGroup -> { return itemGroup.isVisibleInNested(p, profile, mode); }
            case SlimefunItem sf -> { return !sf.isDisabledIn(p.getWorld()); }
            case String ignored -> { return true; }
            default -> {
                Debug.error("item group " + getKey().getKey() + "RSC: " + content);
                return false;
            }
        }
    };
}
