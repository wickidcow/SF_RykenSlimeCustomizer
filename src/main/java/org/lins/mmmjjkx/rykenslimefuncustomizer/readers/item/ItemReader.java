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
package org.lins.mmmjjkx.rykenslimefuncustomizer.readers.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.PiglinBarterDrop;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.RainbowTickHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import lombok.SneakyThrows;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.WitherProofBlockImpl;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.CustomItem;
import org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.colors.CMIChatColor;
import org.lins.mmmjjkx.rykenslimefuncustomizer.readers.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.script.JavaScriptEval;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ClassUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Constants;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemReader extends YamlReader<SlimefunItem> {
    @Override
    public String getFileName() {
        return Constants.ITEMS_FILE;
    }

    public ItemReader(File file, ProjectAddon addon) {
        super(file, addon);
    }

    private CustomItem resolveRadiation(CustomItem instance, BaseResult base, ConfigurationSection section, Object[] constructorArgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String radio = section.getString("radiation");
        boolean addRadiationLore = section.getBoolean("add_radiation_lore", true);
        Optional<Radioactivity> radioactivity = CommonUtils.getEnum(Radioactivity.class, radio);
        if (radioactivity.isEmpty()) {
            Debug.warn(file, section, "RSC message" + radio + " skipped");
            return instance;
        }

        if (addRadiationLore) {
            CommonUtils.addLore(base.sfis(), true, LoreBuilder.radioactive(radioactivity.get()));
        }

        Class<? extends CustomItem> clazz = ClassUtils.generateClass(
            instance.getClass(),
            "Radiation",
            "Item",
            new Class[] {Radioactive.class},
            builder -> builder.method(ElementMatchers.isDeclaredBy(Radioactive.class))
                .intercept(FixedValue.value(radioactivity.get())));

        return (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
    }

    @SneakyThrows
    @Override
    public SlimefunItem readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        var base = getBase(section, s);
        if (base == null) return null;

        CustomItem instance = new CustomItem(base);

        // Handlers must be attached only after all dynamic subclasses have been created.
        // Attribute resolution below may replace the current CustomItem instance with a
        // new ByteBuddy-generated subclass. Attaching handlers before that point silently
        // drops them on the replacement instance (notably script handlers + energy_capacity).
        JavaScriptEval eval = getScriptOrNull(section, section.getString("script"));
        if (eval != null) {
            eval.doInit();
        }
        RainbowTickHandler rainbowHandler = null;

        Object[] constructorArgs = instance.constructorArgs();

        if (section.contains("rainbow")) {
            String materialType = section.getString("rainbow", "");
            if (!base.sfis().getType().isBlock()) {
                Debug.warn(file, section, "blockUnable to (rainbow) skipped");
            } else {
                if (materialType.equalsIgnoreCase("CUSTOM")) {
                    List<String> materials = section.getStringList("rainbow_materials");
                    if (materials.isEmpty()) {
                        Debug.warn(file, section, " (rainbow_materials) skipped");
                    } else {
                        List<Material> colorMaterials = new ArrayList<>();

                        for (String materialS : materials) {
                            Optional<Material> material = CommonUtils.getMaterial(materialS);
                            if (material.isEmpty()) {
                                Debug.warn(file, section, " (rainbow_materials): " + materialS + " skipped");
                                continue;
                            }
                            colorMaterials.add(material.get());
                        }

                        rainbowHandler = new RainbowTickHandler(colorMaterials);
                    }
                } else {
                    Optional<ColoredMaterial> cm = CommonUtils.getEnum(ColoredMaterial.class, materialType);
                    if (cm.isEmpty()) {
                        Debug.error(file, section, "Unable to (rainbow): " + materialType);
                        return null;
                    }

                    rainbowHandler = new RainbowTickHandler(cm.get());
                }
            }
        }

        if (section.getBoolean("placeable", false)) {
            Class<? extends CustomItem> clazz = ClassUtils.generateClass(
                instance.getClass(),
                "NotPlaceable",
                "Item",
                new Class[] {NotPlaceable.class},
                null
            );

            instance = (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
        }

        if (section.getBoolean("anti_wither", false)) {
            if (!base.sfis().getType().isBlock()) {
                Debug.warn(file, section, "blockUnable to skipped");
            } else {
                Class<? extends CustomItem> clazz = ClassUtils.generateClass(
                    instance.getClass(),
                    "WitherProof",
                    "Item",
                    new Class[]{WitherProofBlockImpl.class},
                    null
                );

                instance = (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
            }
        }

        if (section.getBoolean("soulbound", false)) {
            Class<? extends CustomItem> clazz = ClassUtils.generateClass(
                instance.getClass(),
                "Soulbound",
                "Item",
                new Class[] {Soulbound.class},
                null
            );

            instance = (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
        }

        if (section.contains("piglin_trade_chance")) {
            int chance = CommonUtils.clamp(section.getInt("chance", 100), 1, 100, file, section, "' (piglin_trade_chance) '");

            Class<? extends CustomItem> clazz = ClassUtils.generateClass(
                instance.getClass(),
                "PiglinBarterDrop",
                "Item",
                new Class[] {PiglinBarterDrop.class},
                builder -> builder.method(ElementMatchers.isDeclaredBy(PiglinBarterDrop.class))
                        .intercept(FixedValue.value(chance)));

            instance = (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
        }

        if (section.contains("energy_capacity")) {
            instance = resolveEnergyCapacity(section, instance, base, constructorArgs);
        }

        if (section.contains("radiation")) {
            instance = resolveRadiation(instance, base, section, constructorArgs);
        }

        // Attach deferred handlers to the final instance so generated attributes cannot
        // discard them. This also preserves rainbow ticking when combined with attributes.
        if (eval != null) {
            instance.addItemHandler((ItemUseHandler) e -> {
                eval.evalFunction("onUse", e, this);
                e.cancel();
            });

            instance.addItemHandler((WeaponUseHandler) (e, p, it) -> {
                eval.evalFunction("onWeaponHit", e, p, it);
            });
            instance.addItemHandler((ToolUseHandler) (e, it, i, drops) -> eval.evalFunction("onToolUse", e, it, i, drops));
        }

        if (rainbowHandler != null) {
            instance.addItemHandler(rainbowHandler);
        }

        boolean hidden = section.getBoolean("hidden", false);
        if (hidden) instance.setHidden(true);

        instance.setUseableInWorkbench(section.getBoolean("vanilla", false));

        if (section.contains("drop_from")) {
            resolveDropFrom(file, section, base.sfis(), addon);
        }

        instance.register(RykenSlimefunCustomizer.INSTANCE);

        return instance;
    }

    private CustomItem resolveEnergyCapacity(ConfigurationSection section, CustomItem instance, BaseResult base, Object[] constructorArgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        double energyCapacity = section.getDouble("energy_capacity");
        if (energyCapacity < 1) {
            Debug.warn(file, section, "Source (energy_capacity) skipped", 1.0d, Float.MAX_VALUE);
            return instance;
        }

        CommonUtils.addLore(base.sfis(), true, CMIChatColor.translate("&8⇨ &e⚡ &70 / " + energyCapacity + " J"));

        Class<? extends CustomItem> clazz = ClassUtils.generateClass(
            instance.getClass(),
            "Rechargeable",
            "Item",
            new Class[] {Rechargeable.class},
            builder -> builder.method(ElementMatchers.isDeclaredBy(Rechargeable.class).and(ElementMatchers.named("getMaxItemCharge")))
                .intercept(FixedValue.value((float) energyCapacity)));

        instance = (CustomItem) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs);
        return instance;
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        return anyPreloadItems(s);
    }
}
