package strhercules.chickens.registry;

import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.GasEggRegistry;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChemicalEggItem;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.item.ColoredEggItem;
import strhercules.chickens.item.GasEggItem;
import strhercules.chickens.item.LiquidEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            ChickensMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chickens.main"))
                    .icon(() -> new ItemStack(ModRegistry.CATCHER.get()))
                    .withSearchBar()
                    .displayItems((parameters, output) -> {
                        // Herramientas
                        output.accept(ModRegistry.ANALYZER.get());
                        output.accept(ModRegistry.CATCHER.get());
                         output.accept(ModRegistry.CREATIVE_CATCHER.get());

                        // Pollos (chicken item, todos los tipos)
                        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                            output.accept(ModRegistry.CHICKEN_ITEM.get().createFor(chicken));
                        }

                        // Máquinas y bloques funcionales
                        output.accept(ModRegistry.ROOST_ITEM.get());
                        output.accept(ModRegistry.NEST_ITEM.get());
                        output.accept(ModRegistry.BREEDER_ITEM.get());
                        output.accept(ModRegistry.COLLECTOR_ITEM.get());
                        output.accept(ModRegistry.AVIAN_FLUX_CONVERTER_ITEM.get());
                        output.accept(ModRegistry.AVIAN_FLUID_CONVERTER_ITEM.get());
                        output.accept(ModRegistry.AVIAN_CHEMICAL_CONVERTER_ITEM.get());
                        output.accept(ModRegistry.AVIAN_DOUSING_MACHINE_ITEM.get());
                        output.accept(ModRegistry.INCUBATOR_ITEM.get());

                        // Gallineros (todas las variantes de madera)
                        for (DeferredItem<BlockItem> item : ModRegistry.getHenhouseItems()) {
                            output.accept(item.get());
                        }

                        // Huevos de spawn y huevos de color
                        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                            output.accept(ChickensSpawnEggItem.createFor(chicken));
                            if (chicken.isDye()) {
                                output.accept(ColoredEggItem.createFor(chicken));
                            }
                        }

                        // Huevo de flux
                        output.accept(ModRegistry.FLUX_EGG.get());

                        // Huevos de fluido (si está habilitado en config)
                        if (ChickensConfigHolder.get().isFluidChickensEnabled()) {
                            for (LiquidEggRegistryItem liquid : LiquidEggRegistry.getAll()) {
                                output.accept(LiquidEggItem.createFor(liquid));
                            }
                        }

                        // Huevos químicos (si está habilitado en config)
                        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
                            for (ChemicalEggRegistryItem chemical : ChemicalEggRegistry.getAll()) {
                                output.accept(ChemicalEggItem.createFor(chemical));
                            }
                        }

                        // Huevos de gas (si está habilitado en config)
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