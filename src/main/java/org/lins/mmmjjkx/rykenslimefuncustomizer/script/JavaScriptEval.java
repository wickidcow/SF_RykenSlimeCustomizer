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
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSRealm;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.BlockMenuUtil;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JavaScriptEval extends ScriptEval {
    private static final Map<File, JavaScriptEval> SCRIPT_CACHE = new ConcurrentHashMap<>();

    private final Context jsEngine = Context.newBuilder("js")
            .hostClassLoader(RykenSlimefunCustomizer.class.getClassLoader())
            .allowAllAccess(true)
            .allowHostAccess(UNIVERSAL_HOST_ACCESS)
            .allowNativeAccess(false)
            .allowExperimentalOptions(true)
            .allowPolyglotAccess(PolyglotAccess.ALL)
            .allowCreateProcess(true)
            .allowValueSharing(true)
            .allowIO(IOAccess.ALL)
            .allowHostClassLookup(s -> !s.startsWith("net.luckperms")
                    && !s.startsWith("me.lucko")
                    && !s.startsWith("org.anjocaido.groupmanager"))
            .allowHostClassLoading(true)
            .engine(Engine.newBuilder("js").allowExperimentalOptions(true).build())
            .currentWorkingDirectory(getAddon().getScriptsFolder().toPath().toAbsolutePath())
            .build();

    private JavaScriptEval(@NonNull File js, ProjectAddon addon) {
        super(js, addon);

        advancedSetup();
        setup();
        contextInit();
        addon.getScriptEvals().add(this);
    }

    @Nullable
    public static JavaScriptEval create(@NonNull File js, ProjectAddon addon) {
        try {
            File scriptsRoot = addon.getScriptsFolder().getCanonicalFile();
            File canonicalScript = js.getCanonicalFile();
            Path rootPath = scriptsRoot.toPath();
            Path scriptPath = canonicalScript.toPath();

            // Script names can originate in addon configuration. Resolve symlinks/.. and
            // reject anything that escapes this addon's scripts directory.
            if (!scriptPath.startsWith(rootPath)) {
                Debug.warn("Rejected script path outside addon scripts directory: " + js.getPath());
                return null;
            }

            if (!canonicalScript.isFile()) {
                return null;
            }

            JavaScriptEval cached = SCRIPT_CACHE.get(canonicalScript);
            if (cached != null) {
                // ProjectAddon.unregister() clears its live scriptEvals list. Treat any
                // cached context no longer owned by the current addon lifecycle as stale.
                if (addon.getScriptEvals().contains(cached)) {
                    return cached;
                }
                SCRIPT_CACHE.remove(canonicalScript, cached);
                cached.closeEngine();
            }

            JavaScriptEval created = new JavaScriptEval(canonicalScript, addon);
            JavaScriptEval existing = SCRIPT_CACHE.putIfAbsent(canonicalScript, created);
            if (existing != null) {
                addon.getScriptEvals().remove(created);
                created.closeEngine();
                if (addon.getScriptEvals().contains(existing)) {
                    return existing;
                }
                SCRIPT_CACHE.remove(canonicalScript, existing);
                existing.closeEngine();
                return create(canonicalScript, addon);
            }
            return created;
        } catch (Throwable e) {
            Debug.error("Unable to load script " + js.getAbsolutePath(), e);
            return null;
        }
    }

    public static void clearAddonCache(ProjectAddon addon) {
        try {
            Path root = addon.getScriptsFolder().getCanonicalFile().toPath();
            SCRIPT_CACHE.entrySet().removeIf(entry -> {
                try {
                    if (!entry.getKey().getCanonicalFile().toPath().startsWith(root)) {
                        return false;
                    }
                    entry.getValue().clearScriptCache();
                    entry.getValue().closeEngine();
                    return true;
                } catch (IOException ignored) {
                    return false;
                }
            });
        } catch (IOException ignored) {
        }
    }

    private synchronized void advancedSetup() {
        JSRealm realm = JavaScriptLanguage.getJSRealm(jsEngine);
        TruffleLanguage.Env env = realm.getEnv();
        addThing("SlimefunItems", env.asHostSymbol(SlimefunItems.class));
        addThing("SlimefunItem", env.asHostSymbol(SlimefunItem.class));
        addThing("StorageCacheUtils", env.asHostSymbol(StorageCacheUtils.class));
        addThing("SlimefunUtils", env.asHostSymbol(SlimefunUtils.class));
        addThing("BlockMenu", env.asHostSymbol(BlockMenu.class));
        addThing("BlockMenuUtil", env.asHostSymbol(BlockMenuUtil.class));
        addThing("PlayerProfile", env.asHostSymbol(PlayerProfile.class));
        addThing("Slimefun", env.asHostSymbol(Slimefun.class));
    }

    @Override
    public synchronized void addThing(String name, Object value) {
        jsEngine.getBindings("js").putMember(name, value);
    }

    @Override
    public String key() {
        return "js";
    }

    private final Map<String, Value> functionCache = new ConcurrentHashMap<>();
    private final Set<String> failedFunctions = ConcurrentHashMap.newKeySet();

    @Nullable
    @CanIgnoreReturnValue
    @Override
    public synchronized Value evalFunction(String funName, Object... args) {
        if (failedFunctions.contains(funName)) {
            return null;
        }

        if (RykenSlimefunCustomizer.addonManager.isLockingMainThread()) {
            Debug.warn("=================================================");
            Debug.warn("Addon loading is locking the main thread while a script is executing.");
            Debug.warn("Review this addon's scripts if loading stalls.");
            Debug.warn("=================================================");
        }

        Value function = functionCache.get(funName);

        if (function == null) {
            Value bindings = jsEngine.getBindings("js");

            if (!bindings.hasMember(funName)) {
                Debug.debug(() -> "Addon " + addon.getAddonId() + " script " + getFile().getName()
                    + " does not define function " + funName);
                failedFunctions.add(funName);
                return null;
            }

            Value member = bindings.getMember(funName);
            if (!member.canExecute()) {
                Debug.debug(() -> "Addon " + addon.getAddonId() + " script " + getFile().getName()
                    + " member " + funName + " is not executable");
                failedFunctions.add(funName);
                return null;
            }

            function = member;
            functionCache.put(funName, function);
        }

        try {
            Value result = function.execute(args);
            Debug.debug("Executed " + getAddon().getAddonName() + " script " + getFile().getName()
                + " function " + funName);
            return result;
        } catch (IllegalStateException e) {
            if (e.getMessage() == null || !e.getMessage().contains("Multi threaded access")) {
                handleExecutionError(e, funName);
            }
        } catch (Throwable e) {
            handleExecutionError(e, funName);
        }
        return null;
    }

    @Override
    public void close() {
        // Cached contexts remain active during normal addon operation and are closed when
        // the addon is unloaded/reloaded through clearAddonCache(ProjectAddon), or lazily
        // invalidated on the next create call after ProjectAddon.unregister().
    }

    private void closeEngine() {
        try {
            jsEngine.close(true);
        } catch (Throwable ignored) {
        }
    }

    private void handleExecutionError(Throwable e, String funName) {
        functionCache.remove(funName);

        Debug.debug("Script execution failed for " + funName);
        if (!RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("debug")) {
            failedFunctions.add(funName);
        }

        Debug.error("RSC: " + getAddon().getAddonName() + " script " + getFile().getName()
            + " failed while executing " + funName, e);
    }

    protected final synchronized void contextInit() {
        super.contextInit();
        if (jsEngine != null) {
            try {
                clearScriptCache();
                jsEngine.eval(Source.newBuilder("js", getFileContext(), "JavaScript").build());
            } catch (IOException e) {
                Debug.error("RSC: " + getAddon().getAddonName() + " script " + getFile().getName()
                    + " could not be initialized", e);
            }
        }
    }

    public void clearScriptCache() {
        failedFunctions.clear();
        functionCache.clear();
    }
}
