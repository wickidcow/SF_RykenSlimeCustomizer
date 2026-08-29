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
package org.lins.mmmjjkx.rykenslimefuncustomizer.commands;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.ProjectAddonManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.PluginStateCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.SaveditemsGroup;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlock;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainCommand implements TabExecutor {
    @Override
    public boolean onCommand(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.reload")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                RykenSlimefunCustomizer.reload();
                sendMessage(sender, "&aReload successful!");
                return true;
            } else if (args[0].equalsIgnoreCase("list")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.list")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                List<ProjectAddon> addons = RykenSlimefunCustomizer.addonManager.getAllAddons();
                List<String> nameWithId = addons.stream()
                        .map(a -> a.getAddonName() + "(id: " + a.getAddonId() + ")")
                        .toList();
                StringBuilder component = new StringBuilder("&aLoaded addons: ");
                for (String nwi : nameWithId) {
                    component.append("&a").append(nwi);
                    if (nameWithId.indexOf(nwi) != (nameWithId.size() - 1)) {
                        component.append("&6, ");
                    }
                }
                sender.sendMessage(component.toString());
                return true;
            } else if (args[0].equalsIgnoreCase("reloadPlugin")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.reloadPlugin")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                RykenSlimefunCustomizer.INSTANCE.reloadConfig();
                if (RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("saveExample")) {
                    RykenSlimefunCustomizer.saveExample();
                }
                sendMessage(sender, "&aConfiguration reloaded successfully!");
                return true;
            } else if (args[0].equalsIgnoreCase("resaveitems")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.resaveitems")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "&4Only players can run this command!");
                    return false;
                }

                if (!PluginStateCache.isEnabled("JustEnoughGuide")) {
                    sendMessage(sender, "&4JustEnoughGuide");
                    return false;
                }

                sendMessage(player, "&c: Hasitem, , , /rsc resaveitems start");
                sendMessage(player, "&c, , item");
                sendMessage(player, "&c, /Version, item");
                sendMessage(player, "&c, /rsc resaveitems end item");
                sendMessage(player, "&c, Save failed, plugins/RykenSlimefunCustomizerHas");
            } else if (args[0].equalsIgnoreCase("clearScriptCache")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.clearscriptcache")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                RykenSlimefunCustomizer.clearScriptCache();
                sendMessage(sender, "&ascriptsuccessfully!");
                return true;
            } else if (args[0].equalsIgnoreCase("cleardisplayprojectiles")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.cleardisplayprojectiles")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                RykenSlimefunCustomizer.clearDisplayProjectiles();
                sendMessage(sender, "&amultiblocksuccessfully!");
                return true;
            } else if (args[0].equalsIgnoreCase("buildSuperMultiBlock")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.buildSuperMultiBlock")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "&4Only players can run this command!");
                    return false;
                }

                Block b = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
                if (b == null || b.getType().isAir()) {
                    sendMessage(player, "&4multiblock");
                    return false;
                }

                SuperMultiBlock smb = SuperMultiBlockManager.getInstance().getSuperMultiBlock(b.getLocation());
                if (smb == null) {
                    smb = SuperMultiBlockManager.getCoreStorage().get(b.getLocation());
                    if (smb == null) {
                        sendMessage(player, "&4multiblock");
                        return false;
                    }
                }

                smb.buildMultiBlock(player);
            } else {
                sendMessage(sender, "&4Unknown subcommand!");
                return false;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("enable")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.enable")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                File file = new File(ProjectAddonManager.ADDONS_DIRECTORY, args[1]);

                if (!file.exists() || !file.isDirectory()) {
                    sendMessage(sender, "&4Folder not found!");
                    return false;
                }

                if (RykenSlimefunCustomizer.addonManager.isLoaded(file)) {
                    sendMessage(sender, "&4addon!");
                    return false;
                }

                if (RykenSlimefunCustomizer.addonManager.loadAddon(file)) {
                    sendMessage(sender, "&aAddon loaded successfully!");
                } else {
                    sendMessage(sender, "&cAddon failed to load!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("disable")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.disable")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String id = args[1];
                ProjectAddon addon = RykenSlimefunCustomizer.addonManager.get(id);
                if (addon == null) {
                    sendMessage(sender, "&4Addon not found!");
                    return false;
                }

                RykenSlimefunCustomizer.addonManager.unloadAddon(addon);

                sendMessage(sender, "&aAddon unloaded successfully!");
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.info")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String id = args[1];
                ProjectAddon addon = RykenSlimefunCustomizer.addonManager.get(id);
                if (addon == null) {
                    sendMessage(sender, "&4Addon not found!");
                    return false;
                }

                String authors = addon.getAuthors().toString();
                String authorsRemoveBrackets = authors.substring(1, authors.length() - 1);

                StringBuilder builder = new StringBuilder()
                        .append("Name: &a")
                        .append(addon.getAddonName())
                        .append("\n&f")
                        .append("ID: &a")
                        .append(addon.getAddonId())
                        .append("\n&f")
                        .append("Authors: &a")
                        .append(authorsRemoveBrackets)
                        .append("\n&f")
                        .append("Version: &a")
                        .append(addon.getAddonVersion())
                        .append("\n&f")
                        .append("Dependencies: &a")
                        .append(addon.getDepends())
                        .append("\n&f")
                        .append("Plugin dependencies: &a")
                        .append(addon.getPluginDepends())
                        .append("\n&f")
                        .append("Description: &a")
                        .append(addon.getDescription());

                if (addon.getGitHubRepo() != null && !addon.getGitHubRepo().isBlank()) {
                    builder.append("\n&f").append("GithubRepository: &e").append(addon.getGitHubRepo());
                }

                sendMessage(sender, builder.toString());
                return true;
            } else if (args[0].equalsIgnoreCase("menupreview")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.menupreview")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String menuPresetId = args[1];
                BlockMenuPreset bmp = Slimefun.getRegistry().getMenuPresets().get(menuPresetId);
                if (bmp == null) {
                    sendMessage(sender, "&4Menu not found!");
                    return false;
                }
                if (sender instanceof Player p) {
                    bmp.open(p);
                    return true;
                } else {
                    sendMessage(sender, "&4This command cannot be used from the console!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.reload")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String prjId = args[1];
                ProjectAddon addon = RykenSlimefunCustomizer.addonManager.get(prjId);
                if (addon == null) {
                    sendMessage(sender, "&4Addon not found!");
                    return false;
                }

                if (RykenSlimefunCustomizer.addonManager.reloadAddon(addon)) {
                    sendMessage(sender, "&aReload successful!");
                } else {
                    sendMessage(sender, "&cReload failed!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("resaveitems")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.resaveitems")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "&4Only players can run this command!");
                    return false;
                }

                if (!PluginStateCache.isEnabled("JustEnoughGuide")) {
                    sendMessage(sender, "&4JustEnoughGuide");
                    return false;
                }

                if (player.getLocation().toBlockLocation().getBlockY()
                        == player.getWorld().getMinHeight()) {
                    sendMessage(sender, "&4Y, ");
                    return false;
                }

                if (!player.isOnGround()) {
                    sendMessage(sender, "RSC: ");
                    return false;
                }

                if (args[1].equalsIgnoreCase("start")) {
                    List<ItemStack> itemStacks = SaveditemsGroup.instance.getObjects().stream()
                            .map(x -> (ItemStack) x)
                            .toList();

                    int cnt = 0;
                    for (int i = 0; i < itemStacks.size(); i++) {
                        Location chestLocation = player.getLocation().clone().add((int) (i / 27), -1, 0);
                        Block block = chestLocation.getBlock();
                        if (block.getType() != Material.CHEST) {
                            block.setType(Material.CHEST);
                        }
                        BlockState blockState = block.getState();
                        if (blockState instanceof InventoryHolder holder) {
                            holder.getInventory().setItem(i % 27, itemStacks.get(i));
                            cnt++;
                        }
                    }

                    sendMessage(player, "&aSaved successfully!" + cnt + "item, ");
                } else if (args[1].equalsIgnoreCase("end")) {
                    Bukkit.getScheduler().runTaskLater(RykenSlimefunCustomizer.INSTANCE, () -> {
                        int i = 0;
                        int cnt = 0;
                        int offsetY = -1;
                        while (true) {
                            Location chestLocation =
                                    player.getLocation().clone().add(i++, offsetY, 0);
                            Block block = chestLocation.getBlock();
                            if (block.getType() != Material.CHEST) {
                                if (offsetY == -1) {
                                    offsetY = 0;
                                    i = 0;
                                } else {
                                    sendMessage(player, "&aSaved successfully!" + cnt + "RSC: ");
                                    break;
                                }
                            }

                            BlockState blockState = block.getState();
                            if (!(blockState instanceof InventoryHolder holder)) continue;
                            for (int j = 0; j < 27; j++) {
                                ItemStack itemStack = holder.getInventory().getItem(j);
                                if (itemStack != null) {
                                    ItemStack clone = itemStack.clone();
                                    String source = clone.getItemMeta().getPersistentDataContainer()
                                        .get(SaveditemsGroup.SOURCE_KEY, PersistentDataType.STRING);
                                    if (source == null) continue;
                                    clone.editMeta(meta -> {
                                        meta.getPersistentDataContainer().remove(SaveditemsGroup.SOURCE_KEY);
                                    });

                                    try {
                                        // resave clone
                                        String prjId = source.split(";")[0];
                                        String filePath = source.split(";")[1];

                                        ProjectAddon addon =
                                                RykenSlimefunCustomizer.addonManager.get(prjId);

                                        CommonUtils.saveItem(itemStack, filePath, addon);
                                        sendMessage(player, "RSC: " + source);
                                        cnt++;
                                    } catch (Exception e) {
                                        Debug.error("RSC: " + source + "itemfailed", e);
                                    }
                                }
                            }
                        }
                    },
                    1L);
                } else {
                    sendMessage(sender, "&4Enter a valid argument! (start/end)");
                }
            } else {
                sendMessage(sender, "&4Unknown subcommand!");
                return false;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("saveitem")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.saveitem")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String prjId = args[1];
                String itemId = args[2];
                ProjectAddon addon = RykenSlimefunCustomizer.addonManager.get(prjId);
                if (addon == null) {
                    sendMessage(sender, "&4Addon not found!");
                    return false;
                }
                if (sender instanceof Player p) {
                    ItemStack itemStack = p.getInventory().getItemInMainHand();
                    if (itemStack.getType() == Material.AIR) {
                        sendMessage(sender, "&4You cannot save air!");
                        return false;
                    }
                    CommonUtils.saveItem(itemStack, itemId, addon);
                    sendMessage(sender, "&aSaved successfully!");
                    return true;
                } else {
                    sendMessage(sender, "&4This command cannot be used from the console!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("getsaveditem")) {
                if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.getsaveditem")) {
                    sendMessage(sender, "&4You do not have permission to do that!");
                    return false;
                }

                String prjId = args[1];
                String itemId = args[2];
                ProjectAddon addon = RykenSlimefunCustomizer.addonManager.get(prjId);
                if (addon == null) {
                    sendMessage(sender, "&4Addon not found!");
                    return false;
                }

                File file = new File(
                        RykenSlimefunCustomizer.addonManager.getAddonFolder(prjId), "saveditems/" + itemId + ".yml");
                if (!file.exists() || file.length() == 0) {
                    sendMessage(sender, "&4The selected item file is empty!");
                    return false;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ItemStack item = config.getItemStack("item");
                if (item == null) {
                    sendMessage(sender, "&4Unable to read this item file!");
                    return false;
                }

                if (sender instanceof Player p) {
                    ItemStack itemStack = p.getInventory().getItemInMainHand();
                    if (itemStack.getType() == Material.AIR) {
                        p.getInventory().setItemInMainHand(item);
                        sendMessage(sender, "&aThe item was placed in your hand!");
                        return true;
                    }
                    p.getInventory().addItem(item);
                    sendMessage(sender, "&aThe item was added to your inventory!");
                    return true;
                } else {
                    sendMessage(sender, "&4This command cannot be used from the console!");
                    return false;
                }
            } else {
                sendMessage(sender, "&4Unknown subcommand!");
                return false;
            }
        } else {
            sendMessage(sender, "&4Unknown subcommand!");
            return false;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        List<String> raw = onTabCompleteRaw(args);
        return StringUtil.copyPartialMatches(args[args.length - 1], raw, new ArrayList<>());
    }

    public @NonNull List<String> onTabCompleteRaw(@NonNull String[] args) {
        if (args.length == 1) {
            return List.of(
                    "list",
                    "reload",
                    "reloadPlugin",
                    "list",
                    "enable",
                    "disable",
                    "saveitem",
                    "menupreview",
                    "getsaveditem",
                    "resaveitems",
                    "clearScriptCache",
                    "buildSuperMultiBlock");
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "enable" ->
                    Arrays.stream(Objects.requireNonNull(ProjectAddonManager.ADDONS_DIRECTORY.listFiles()))
                            .map(File::getName)
                            .toList();
                case "disable", "saveitem", "getsaveditem" ->
                    RykenSlimefunCustomizer.addonManager.getAllAddons().stream()
                            .map(ProjectAddon::getAddonId)
                            .toList();
                case "menupreview" ->
                    Slimefun.getRegistry().getMenuPresets().keySet().stream().toList();
                default -> new ArrayList<>();
            };
        }
        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("rsc.command") || !sender.hasPermission("rsc.command.help")) {
            sendMessage(sender, "&4You do not have permission to do that!");
            return;
        }
        sendMessage(sender, """
 &aRykenSlimefunCustomizer
 &e/rsc &7- Show this help menu
 &e/rsc reload &7- Reload RSC addons
 &e/rsc reloadPlugin &7- Reload plugin configuration
 &e/rsc list &7- List loaded RSC addons
 &e/rsc enable <addonName> &7- Enable an addon
 &e/rsc disable <addonID> &7- Disable an addon
 &e/rsc saveitem <addonID> <ID> &7- Save the held item
 &e/rsc menupreview <ID> &7- Preview a machine menu
 &e/rsc getsaveditem <addonID> <ID> &7- Get a saved item
 &e/rsc resaveitems <start|end> &7- Resave stored items after a version change
 &e/rsc clearScriptCache &7- Clear cached scripts
 &e/rsc cleardisplayprojectiles &7- Clear multiblock display entities""");
    }

    public static void sendMessage(CommandSender sender, String s) {
        sender.sendMessage(CommonUtils.decorate(s));
    }
}
