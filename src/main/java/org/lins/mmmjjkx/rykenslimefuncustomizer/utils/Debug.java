package org.lins.mmmjjkx.rykenslimefuncustomizer.utils;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.ProjectAddonManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;

import java.io.File;
import java.util.concurrent.Callable;

public class Debug {
    private static final String ITEM_CANDIDATE_WARNING = "Unable toitem: ";

    public static void error(File file, ConfigurationSection section, String msg) {
        printFileError(file);
        error("RSC: " + section.getCurrentPath() + "RSC: " + msg);
    }

    public static void error(File file, ConfigurationSection section, String msg, Throwable e) {
        printFileError(file);
        error("RSC: " + section.getCurrentPath() + "RSC: " + msg);
        e.printStackTrace();
    }

    public static void error(File file, ConfigurationSection section, String msg, Number start, Number end) {
        printFileError(file);
        error("RSC: " + section.getCurrentPath() + "RSC: " + msg + "RSC: " + start + ", " + end + "]");
    }

    public static void warn(File file, ConfigurationSection section, String msg) {
        // Item pipe/fallback parsing can legitimately miss the first candidate before a
        // later candidate succeeds. CommonUtils emits one contextual warning if all
        // candidates ultimately fail, so keep these per-candidate misses at debug level
        // instead of producing duplicate/misleading startup warnings.
        if (msg.startsWith(ITEM_CANDIDATE_WARNING)) {
            debug(file, () -> "RSC: " + section.getCurrentPath() + "RSC: " + msg);
            return;
        }

        printFileWarning(file);
        warn("RSC: " + section.getCurrentPath() + "RSC: " + msg);
    }

    public static void warn(File file, ConfigurationSection section, String msg, Throwable e) {
        printFileWarning(file);
        warn("RSC: " + section.getCurrentPath() + "RSC: " + msg);
        e.printStackTrace();
    }

    public static void warn(File file, ConfigurationSection section, String msg, Number start, Number end) {
        printFileWarning(file);
        warn("RSC: " + section.getCurrentPath() + "RSC: " + msg + "RSC: " + start + ", " + end + "]");
    }

    public static void debug(File file, Callable<String> msg) {
        debug(() -> "RSC: " + file.getAbsolutePath());
        debug(msg);
    }

    public static String shortenPath(File file) {
        return ProjectAddonManager.ADDONS_DIRECTORY.toPath().relativize(file.toPath()).toString();
    }

    public static void printFileWarning(File file) {
        warn("RSC: " + shortenPath(file));
    }

    public static void printFileError(File file) {
        error("RSC: " + shortenPath(file));
    }

    public static void warn(String message) {
        getLogger().warn(CommonUtils.decorate("&e" + message));
    }

    public static void warn(String message, Throwable e) {
        getLogger().warn(CommonUtils.decorate("&e" + message), e);
    }

    public static ComponentLogger getLogger() {
        return RykenSlimefunCustomizer.INSTANCE.getComponentLogger();
    }

    public static void error(String message) {
        getLogger().error(CommonUtils.decorate("&c" + message));
    }

    public static void error(String message, Throwable e) {
        getLogger().error(CommonUtils.decorate("&c" + message), e);
    }

    public static void debug(String message) {
        debug(() -> message);
    }

    public static void debug(Callable<String> callable) {
        try {
            if (RykenSlimefunCustomizer.INSTANCE.getConfig().getBoolean("debug")) {
                String message = callable.call();
                getLogger().info(CommonUtils.decorate("&6DEBUG | " + message));
            }
        } catch (Exception e) {
            error("Unable to debug log", e);
        }
    }

    public static void danger(String message) {
        getLogger().error(CommonUtils.decorate("&4DANGER | " + message));
    }

    public static void info(String message) {
        getLogger().info(CommonUtils.decorate("&a" + message));
    }
}
