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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JavaScriptEval extends ScriptEval {
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
            return new JavaScriptEval(js, addon);
        } catch (Throwable e) {
            Debug.error("Unable toscript " + js.getAbsolutePath(), e);
            return null;
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

    @Nullable @CanIgnoreReturnValue
    @Override
    public synchronized Value evalFunction(String funName, Object... args) {
        if (failedFunctions.contains(funName)) {
            return null;
        }

        if (RykenSlimefunCustomizer.addonManager.isLockingMainThread()) {
            Debug.warn("=================================================");
            Debug.warn("addonLoading, addon, script!");
            Debug.warn(", Unable to, addon!");
            Debug.warn("=================================================");
        }

        Value function = functionCache.get(funName);

        if (function == null) {
            Value bindings = jsEngine.getBindings("js");

            if (!bindings.hasMember(funName)) {
                Debug.debug(() -> "addon" + addon.getAddonId() + "script" + getFile().getName() + "RSC message" + "RSC message" + funName);
                failedFunctions.add(funName);
                return null;
            }

            Value member = bindings.getMember(funName);
            if (!member.canExecute()) {
                Debug.debug(() -> "addon" + addon.getAddonId() + "script" + getFile().getName() + "RSC message" + "RSC message" + funName + "RSC message");
                failedFunctions.add(funName);
                return null;
            }

            function = member;
            functionCache.put(funName, function);
        }

        try {
            Value result = function.execute(args);
            Debug.debug(
                    "RSC message" + getAddon().getAddonName() + "script" + getFile().getName() + "RSC message" + funName);
            return result;
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("Multi threaded access")) {
                handleExecutionError(e, funName);
            }
        } catch (Throwable e) {
            handleExecutionError(e, funName);
        }
        return null;
    }

    @Override
    public void close() {
        // don't close jsEngine, since we just reload the plugin, not the js engine.
    }

    private void handleExecutionError(Throwable e, String funName) {
        functionCache.remove(funName);

        Debug.debug(" debug , scriptfailed");
        if (!RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("debug")) {
            failedFunctions.add(funName);
        }

        Debug.error(
                "RSC message" + getAddon().getAddonName() + "script" + getFile().getName() + "RSC message", e);
    }

    protected final synchronized void contextInit() {
        super.contextInit();
        if (jsEngine != null) {
            try {
                clearScriptCache();

                jsEngine.eval(
                        Source.newBuilder("js", getFileContext(), "JavaScript").build());
            } catch (IOException e) {
                Debug.error(
                        "RSC message" + getAddon().getAddonName() + "script" + getFile().getName() + "RSC message", e);
            }
        }
    }

    public void clearScriptCache() {
        failedFunctions.clear();
        functionCache.clear();
    }
}
