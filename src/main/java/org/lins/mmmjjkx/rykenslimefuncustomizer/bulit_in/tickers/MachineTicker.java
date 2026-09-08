package org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.tickers;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lins.mmmjjkx.rykenslimefuncustomizer.addon.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CacheAccess;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.CustomMenuHolder;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.DataCache;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.Recipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.bulit_in.recipes.RecipesHolder;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.AdvancedCustomMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.menu.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.customs.super_multiblock.SuperMultiBlockManager;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.Debug;

import java.io.File;

@NullMarked
public interface MachineTicker extends DataCache, RecipesHolder, CustomMenuHolder, EnergyNetComponent {
    CacheAccess<Recipe> lastRecipeAccessor = () -> Recipe.class;

    Type getType();

    @Override
    AdvancedCustomMachine getMachine();

    @Override
    default EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    boolean preTick(Location location);

    void tick(Location location);

    default void init() {
        boolean workbench = getType() == Type.WORKBENCH;
        var menu = getCustomMenu();
        if (menu == null) {
            Debug.warn("No custom menu found for " + this.getMachine().getId() + "; using the default menu");
            this.createPreset(
                this.getMachine(),
                this.getMachine().getItemName(),
                preset -> {
                    if (workbench) {
                        CustomMenuHolder.constructMenu(preset);
                    } else {
                        CustomMenuHolder.constructMenu(preset, getProgressSlot(), getProgressBar());
                    }
                },
                this::onNewInstance
            );
            return;
        }

        createPreset(
            this.getMachine(),
            menu.getTitle() == null || menu.getTitle().isBlank() ? getMachine().getItemName() : menu.getTitle(),
            menu::apply,
            this::onNewInstance
        );
        if (!workbench && menu.getProgressBar() != null) {
            getAdvancedMachineProcessor().setProgressBar(menu.getProgressBar());
        }
    }

    default void onNewInstance(BlockMenu menu, Block b) {
        getMachine().onNewInstance(menu, b);
    }

    int getEnergyConsumption();

    default boolean takeCharge(Location l) {
        if (isChargeable()) {
            int charge = getCharge(l);

            if (charge < getEnergyConsumption()) {
                return false;
            }

            setCharge(l, charge - getEnergyConsumption());
        }
        return true;
    }

    default boolean canTick(Location location) {
        return SuperMultiBlockManager.canTick(location);
    }

    @Nullable static MachineTicker create(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon, @Nullable Type type) {
        if (type == null) {
            var tpc = section.getString("ticker_type");
            if (tpc == null) {
                type = Type.RECIPE;
            } else {
                var typeOptional = CommonUtils.getEnum(Type.class, tpc);
                if (typeOptional.isEmpty()) {
                    Debug.warn(file, section, "Unknown machine ticker_type '" + tpc + "'; using RECIPE");
                    type = Type.RECIPE;
                } else {
                    type = typeOptional.get();
                }
            }
        }
        return type.createTicker(file, sf, section, menu, addon);
    }

    default void createGUI(Player p, int index) {
        if (index >= getRecipes().size()) return;
        var recipe = getRecipes().get(index);
        Recipe.openGUI(p, getCustomMenu(), getInputSlots(), getOutputSlots(), recipe, this.getMachine());
    }

    @NullMarked
    @Getter
    enum Type {
        RECIPE(new RecipeMachineTickerCreator()),
        LINKED_RECIPE(new LinkedRecipeMachineTickerCreator()),
        TEMPLATE_RECIPE(new TemplateRecipeMachineTickerCreator()),
        MATERIAL_GENERATOR(new MaterialGeneratorMachineTickerCreator()),
        WORKBENCH(new WorkbenchMachineTickerCreator());

        private final TickerCreator tickerCreator;

        public @Nullable MachineTicker createTicker(File file, AdvancedCustomMachine sf, ConfigurationSection section, @Nullable CustomMenu menu, ProjectAddon addon) {
            return tickerCreator.create(file, sf, section, menu, addon);
        }

        Type(TickerCreator tickerCreator) {
            this.tickerCreator = tickerCreator;
        }
    }
}
