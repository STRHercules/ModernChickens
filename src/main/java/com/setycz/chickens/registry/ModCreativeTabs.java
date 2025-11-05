package com.setycz.chickens.registry;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.item.ColoredEggItem;
import com.setycz.chickens.item.GasEggItem;
import com.setycz.chickens.item.LiquidEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hosts the modern creative mode tab so all ported items remain grouped together
 * just like the legacy release. The display callback inspects the runtime
 * chicken/liquid registries so configuration tweaks immediately reflect in the
 * inventory menu without a restart.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            ChickensMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chickens.main"))
                    // Spawn eggs are the mod's iconography, so reuse the dynamic egg for the tab icon.
                    .icon(() -> new ItemStack(ModRegistry.SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        // Utility items first to mirror the original ordering.
                        output.accept(ModRegistry.ANALYZER.get());
                        output.accept(ModRegistry.CATCHER.get());
                        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                            output.accept(ModRegistry.CHICKEN_ITEM.get().createFor(chicken));
                        }
                        output.accept(ModRegistry.ROOST_ITEM.get());
                        output.accept(ModRegistry.BREEDER_ITEM.get());
                        output.accept(ModRegistry.COLLECTOR_ITEM.get());
                        output.accept(ModRegistry.AVIAN_FLUX_CONVERTER_ITEM.get());
                        output.accept(ModRegistry.AVIAN_FLUID_CONVERTER_ITEM.get());
                        for (DeferredItem<BlockItem> item : ModRegistry.getHenhouseItems()) {
                            output.accept(item.get());
                        }
                        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                            output.accept(ChickensSpawnEggItem.createFor(chicken));
                            if (chicken.isDye()) {
                                output.accept(ColoredEggItem.createFor(chicken));
                            }
                        }
                        output.accept(ModRegistry.FLUX_EGG.get());
                        if (ChickensConfigHolder.get().isFluidChickensEnabled()) {
                            for (LiquidEggRegistryItem liquid : LiquidEggRegistry.getAll()) {
                                output.accept(LiquidEggItem.createFor(liquid));
                            }
                        }
                        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
                            for (ChemicalEggRegistryItem chemical : ChemicalEggRegistry.getAll()) {
                                output.accept(ChemicalEggItem.createFor(chemical));
                            }
                        }
                        if (ChickensConfigHolder.get().isGasChickensEnabled()) {
                            for (ChemicalEggRegistryItem gas : GasEggRegistry.getAll()) {
                                output.accept(GasEggItem.createFor(gas));
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void init(IEventBus modBus) {
        TABS.register(modBus);
    }
}
