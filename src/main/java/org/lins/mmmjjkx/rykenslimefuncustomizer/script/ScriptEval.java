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
package org.lins.mmmjjkx.rykenslimefuncustomizer.script;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import lombok.AccessLevel;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.PluginStateCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.integrations.NBTAPIIntegration;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.objects.CiConsumer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.objects.CiFunction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Permission;
import java.security.Permissions;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Getter(AccessLevel.PROTECTED)
public abstract class ScriptEval {
    @Getter
    private static final Queue<Runnable> initTasks = new ConcurrentLinkedQueue<>();
    protected final HostAccess UNIVERSAL_HOST_ACCESS = createHostAccess();

    private static HostAccess createHostAccess() {
        HostAccess.Builder builder = HostAccess.newBuilder()
                .allowPublicAccess(true)
                .allowAllImplementations(true)
                .allowAllClassImplementations(true)
                .allowArrayAccess(true)
                .allowListAccess(true)
                .allowBufferAccess(true)
                .allowIterableAccess(true)
                .allowIteratorAccess(true)
                .allowMapAccess(true)
                .allowAccessInheritance(true)
                .targetTypeMapping(Double.class, Float.class, null, Double::floatValue)
                .targetTypeMapping(Integer.class, Float.class, null, Integer::floatValue)
                .targetTypeMapping(Boolean.class, String.class, null, String::valueOf)
                .targetTypeMapping(Integer.class, String.class, null, String::valueOf)
                .targetTypeMapping(Character.class, String.class, null, String::valueOf)
                .targetTypeMapping(Long.class, String.class, null, String::valueOf)
                .targetTypeMapping(Float.class, String.class, null, String::valueOf)
                .targetTypeMapping(Double.class, String.class, null, String::valueOf)
                .targetTypeMapping(Object.class, String.class, null, String::valueOf)
                .denyAccess(System.class)
                .denyAccess(Process.class)
                .denyAccess(Runtime.class)
                .denyAccess(ProcessBuilder.class)
                .denyAccess(ClassLoader.class)
                .denyAccess(Permission.class)
                .denyAccess(Permissions.class);

        denyLuckPerms(builder);
        denyGroupManager(builder);

        return builder.build();
    }

    private static void denyLuckPerms(HostAccess.Builder builder) {
        if (!PluginStateCache.isEnabled("LuckPerms")) {
            return;
        }
        String[] classNames = {
            "net.luckperms.api.LuckPerms",
            "net.luckperms.api.LuckPermsProvider",
            "net.luckperms.api.model.user.User",
            "net.luckperms.api.model.user.UserManager",
            "net.luckperms.api.model.group.Group",
            "net.luckperms.api.model.group.GroupManager",
            "net.luckperms.api.node.Node",
            "net.luckperms.api.node.NodeBuilder",
            "net.luckperms.api.node.NodeEqualityPredicate",
            "net.luckperms.api.node.types.PermissionNode",
            "net.luckperms.api.node.types.PrefixNode",
            "net.luckperms.api.node.types.SuffixNode",
            "net.luckperms.api.node.types.MetaNode",
            "net.luckperms.api.node.types.InheritanceNode",
            "net.luckperms.api.node.types.RegexPermissionNode",
            "net.luckperms.api.node.types.ChatMetaNode",
            "net.luckperms.api.node.types.WeightNode",
            "net.luckperms.api.node.types.DisplayNameNode",
            "net.luckperms.api.track.Track",
        };
        for (String name : classNames) {
            try {
                builder.denyAccess(Class.forName(name));
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private static void denyGroupManager(HostAccess.Builder builder) {
        if (!PluginStateCache.isEnabled("GroupManager")) {
            return;
        }
        String[] classNames = {
            "org.anjocaido.groupmanager.GroupManager",
            "org.anjocaido.groupmanager.data.Group",
            "org.anjocaido.groupmanager.data.User",
            "org.anjocaido.groupmanager.dataholder.WorldDataHolder",
            "org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler",
        };
        for (String name : classNames) {
            try {
                builder.denyAccess(Class.forName(name));
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    @Getter
    protected final File file;
    protected final ProjectAddon addon;
    protected String fileContext;

    public ScriptEval(File file, ProjectAddon addon) {
        this.file = file;
        this.addon = addon;

        contextInit();
    }

    public abstract String key();

    protected void contextInit() {
        try {
            fileContext = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            fileContext = "";
            e.printStackTrace();
        } catch (IOException e) {
            fileContext = "";
            e.printStackTrace();
        }
    }

    private static Server getServer() {
        return Bukkit.getServer();
    }

    protected final void setup() {
        addThing("server", getServer());

        // functions
        addThing("isPluginLoaded", (Function<String, Boolean>)
                s -> Bukkit.getPluginManager().isPluginEnabled(s));

        addThing("getServer", (Supplier<Server>) () -> getServer());
        addThing("getSuperMultiBlockManager", (Supplier<SuperMultiBlockManager>) () -> SuperMultiBlockManager.getInstance());

        addThing("runOpCommand", (BiConsumer<Player, String>) (p, s) -> {
            boolean op = p.isOp();
            p.setOp(true);
            try {
                p.performCommand(parsePlaceholder(p, s));
            } finally {
                p.setOp(op);
            }
        });

        addThing("runConsoleCommand", (Consumer<String>) s -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsePlaceholder(null, s));
        });

        addThing("sendMessage", (BiConsumer<Player, String>)
                (p, s) -> p.sendMessage(CMIChatColor.translate(parsePlaceholder(p, s))));

        // get slimefun item
        addThing("getSfItemById", (Function<String, SlimefunItem>) SlimefunItem::getById);
        addThing("getSfItemByItem", (Function<ItemStack, SlimefunItem>) SlimefunItem::getByItem);

        // SlimefunUtils functions
        addThing("isItemSimilar", (CiFunction<ItemStack, ItemStack, Boolean, Boolean>) SlimefunUtils::isItemSimilar);
        addThing("isRadioactiveItem", (Function<ItemStack, Boolean>) SlimefunUtils::isRadioactive);
        addThing("isSoulbound", (Function<ItemStack, Boolean>) SlimefunUtils::isSoulbound);
        addThing("canPlayerUseItem", (CiFunction<Player, ItemStack, Boolean, Boolean>) SlimefunUtils::canPlayerUseItem);

        // ChatInput functions
        addThing("getChatInput", (BiConsumer<Player, Consumer<String>>)
                (p, s) -> ChatInput.waitForPlayer(RykenSlimefunCustomizer.INSTANCE, p, s));
        addThing("getChatInputWithCheck", (CiConsumer<Player, Predicate<String>, Consumer<String>>)
                (p, b, s) -> ChatInput.waitForPlayer(RykenSlimefunCustomizer.INSTANCE, p, b, s));

        // randint function
        addThing("randintA", (Function<Integer, Integer>) i -> new Random().nextInt(i));
        addThing("randintB", (BiFunction<Integer, Boolean, Integer>) (i, b) -> new Random().nextInt(b ? (i + 1) : i));
        addThing("randintC", (BiFunction<Integer, Integer, Integer>) (start, end) -> {
            IntStream is = IntStream.range(start, end);
            Random random = new Random();
            int[] arr = is.toArray();
            return arr[random.nextInt(arr.length)];
        });
        addThing("randintD", (CiFunction<Integer, Integer, Boolean, Integer>) (start, end, rangeClosed) -> {
            IntStream stream = rangeClosed ? IntStream.rangeClosed(start, end) : IntStream.range(start, end);
            Random random = new Random();
            int[] arr = stream.toArray();
            return arr[random.nextInt(arr.length)];
        });

        // StorageCacheUtils functions
        // removal
        addThing("setData", (CiConsumer<Location, String, String>) StorageCacheUtils::setData);
        addThing("getData", (BiFunction<Location, String, String>)
                (a, b) -> StorageCacheUtils.getData(a, b));
        addThing("getBlockMenu", (Function<Location, BlockMenu>) a -> StorageCacheUtils.getMenu(a));
        addThing("getBlockData", (Function<Location, SlimefunBlockData>)
                a -> StorageCacheUtils.getBlock(a));
        addThing("isSlimefunBlock", (Function<Location, Boolean>) StorageCacheUtils::hasBlock);
        addThing("isBlock", (BiFunction<Location, String, Boolean>) StorageCacheUtils::isBlock);
        addThing("getSfItemByBlock", (Function<Location, SlimefunItem>) StorageCacheUtils::getSlimefunItem);

        // task
        addThing("runLater", (BiFunction<Function<Object[], ?>, Integer, BukkitTask>) (r, l) -> {
            AtomicReference<BukkitTask> task = new AtomicReference<>();
            Bukkit.getScheduler()
                    .runTaskLater(
                            RykenSlimefunCustomizer.INSTANCE,
                            t -> {
                                r.apply(new Object[] {t});
                                task.set(t);
                            },
                            l);
            return task.get();
        });
        addThing("runRepeating", (CiFunction<Function<Object[], ?>, Integer, Integer, BukkitTask>) (r, l, t) -> {
            AtomicReference<BukkitTask> task = new AtomicReference<>();
            Bukkit.getScheduler()
                    .runTaskTimer(
                            RykenSlimefunCustomizer.INSTANCE,
                            ta -> {
                                r.apply(new Object[] {ta});
                                task.set(ta);
                            },
                            l,
                            t);
            return task.get();
        });
        /*
        addThing("runAsync", (Function<Function<Object[], ?>, BukkitTask>) r -> {
            AtomicReference<BukkitTask> task = new AtomicReference<>();
            Bukkit.getScheduler().runTaskAsynchronously(RykenSlimefunCustomizer.INSTANCE, t -> {
                r.apply(new Object[] {t});
                task.set(t);
            });
            return task.get();
        });
        addThing("runLaterAsync", (BiFunction<Function<Object[], ?>, Integer, BukkitTask>) (r, l) -> {
            AtomicReference<BukkitTask> task = new AtomicReference<>();
            Bukkit.getScheduler()
                    .runTaskLaterAsynchronously(
                            RykenSlimefunCustomizer.INSTANCE,
                            t -> {
                                r.apply(null);
                                task.set(t);
                            },
                            l);
            return task.get();
        });
        addThing("runRepeatingAsync", (CiFunction<Function<Object[], ?>, Integer, Integer, BukkitTask>) (r, l, t) -> {
            AtomicReference<BukkitTask> task = new AtomicReference<>();
            Bukkit.getScheduler()
                    .runTaskTimerAsynchronously(
                            RykenSlimefunCustomizer.INSTANCE,
                            ta -> {
                                r.apply(null);
                                task.set(ta);
                            },
                            l,
                            t);
            return task.get();
        });

         */

        addThing("getAddonConfig", (Supplier<YamlConfiguration>) () -> {
            if (addon.getConfig() == null) {
                throw new RuntimeException("The addon does not have a config file!");
            }

            return addon.getConfig().config();
        });

        if (PluginStateCache.isEnabled("NBTAPI")) {
            addThing("NBTAPI", NBTAPIIntegration.instance);
        }
    }

    private String parsePlaceholder(@Nullable Player p, String text) {
        if (p != null) {
            text = text.replaceAll("%player%", p.getName());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(p, text);
        }

        return text;
    }

    public abstract void addThing(String name, Object value);

    public final void doInit() {
        initTasks.add(() -> {
            if (fileContext == null || fileContext.isBlank()) {
                contextInit();
            }

            evalFunction("init");
        });
    }

    @CanIgnoreReturnValue
    @Nullable public abstract Value evalFunction(String functionName, Object... args);

    public abstract void close();
}
