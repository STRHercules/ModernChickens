package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.block.AvianChemicalConverterBlock;
import strhercules.chickens.block.AvianFluxConverterBlock;
import strhercules.chickens.block.AvianDousingMachineBlock;
import strhercules.chickens.block.AvianFluidConverterBlock;
import strhercules.chickens.block.BreederBlock;
import strhercules.chickens.block.CollectorBlock;
import strhercules.chickens.block.IncubatorBlock;
import strhercules.chickens.block.HenhouseBlock;
import strhercules.chickens.block.RoostBlock;
import strhercules.chickens.block.NestBlock;
import strhercules.chickens.item.AnalyzerItem;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.item.ColoredEggItem;
import strhercules.chickens.item.FluxEggItem;
import strhercules.chickens.item.ChickenItem;
import strhercules.chickens.item.ChickenCatcherItem;
import strhercules.chickens.item.CreativeCatcherItem;
import strhercules.chickens.item.LiquidEggItem;
import strhercules.chickens.item.ChemicalEggItem;
import strhercules.chickens.item.GasEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.neoforged.bus.api.IEventBus;

import java.util.Collections;
import java.util.List;

public final class ModRegistry {
    public static final Items ITEMS = DeferredRegister.createItems(ChickensMod.MOD_ID);
    public static final Blocks BLOCKS = DeferredRegister.createBlocks(ChickensMod.MOD_ID);

    private ModRegistry() {
    }

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ModEntityTypes.init(modBus);
        ModBlockEntities.init(modBus);
        ModMenuTypes.init(modBus);
        ModSpawns.init(modBus);
        ModBiomeModifiers.init(modBus);
        ModCapabilities.init(modBus);
        ModCreativeTabs.init(modBus);
    }

    public static final DeferredItem<ChickensSpawnEggItem> SPAWN_EGG = ITEMS.register("spawn_egg",
            () -> new ChickensSpawnEggItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<ColoredEggItem> COLORED_EGG = ITEMS.register("colored_egg",
            () -> new ColoredEggItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<LiquidEggItem> LIQUID_EGG = ITEMS.register("liquid_egg",
            () -> new LiquidEggItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ChemicalEggItem> CHEMICAL_EGG = ITEMS.register("chemical_egg",
            () -> new ChemicalEggItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<GasEggItem> GAS_EGG = ITEMS.register("gas_egg",
            () -> new GasEggItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<FluxEggItem> FLUX_EGG = ITEMS.register("flux_egg",
            () -> new FluxEggItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AnalyzerItem> ANALYZER = ITEMS.register("analyzer",
            () -> new AnalyzerItem(new Item.Properties().durability(238)));
    public static final DeferredBlock<RoostBlock> ROOST = BLOCKS.register("roost", () -> new RoostBlock());
    public static final DeferredBlock<NestBlock> NEST = BLOCKS.register("nest", () -> new NestBlock());
    public static final DeferredBlock<BreederBlock> BREEDER = BLOCKS.register("breeder", () -> new BreederBlock());
    public static final DeferredBlock<CollectorBlock> COLLECTOR = BLOCKS.register("collector", () -> new CollectorBlock());
    public static final DeferredBlock<AvianFluxConverterBlock> AVIAN_FLUX_CONVERTER = BLOCKS.register("avian_flux_converter", () -> new AvianFluxConverterBlock());
    public static final DeferredBlock<AvianFluidConverterBlock> AVIAN_FLUID_CONVERTER = BLOCKS.register("avian_fluid_converter", () -> new AvianFluidConverterBlock());
    public static final DeferredBlock<AvianChemicalConverterBlock> AVIAN_CHEMICAL_CONVERTER = BLOCKS.register("avian_chemical_converter", () -> new AvianChemicalConverterBlock());
    public static final DeferredBlock<AvianDousingMachineBlock> AVIAN_DOUSING_MACHINE = BLOCKS.register("avian_dousing_machine",
            () -> new AvianDousingMachineBlock());
    public static final DeferredBlock<IncubatorBlock> INCUBATOR = BLOCKS.register("incubator", () -> new IncubatorBlock());
    public static final DeferredBlock<HenhouseBlock> HENHOUSE = registerHenhouse("henhouse", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HenhouseBlock> HENHOUSE_SPRUCE = registerHenhouse("henhouse_spruce", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HenhouseBlock> HENHOUSE_BIRCH = registerHenhouse("henhouse_birch", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HenhouseBlock> HENHOUSE_JUNGLE = registerHenhouse("henhouse_jungle", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HenhouseBlock> HENHOUSE_ACACIA = registerHenhouse("henhouse_acacia", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HenhouseBlock> HENHOUSE_DARK_OAK = registerHenhouse("henhouse_dark_oak", MapColor.COLOR_BROWN);

    public static final List<DeferredBlock<HenhouseBlock>> HENHOUSE_BLOCKS = List.of(
            HENHOUSE, HENHOUSE_SPRUCE, HENHOUSE_BIRCH, HENHOUSE_JUNGLE, HENHOUSE_ACACIA, HENHOUSE_DARK_OAK
    );

    public static final DeferredItem<BlockItem> HENHOUSE_ITEM = registerHenhouseItem("henhouse", HENHOUSE);
    public static final DeferredItem<BlockItem> HENHOUSE_SPRUCE_ITEM = registerHenhouseItem("henhouse_spruce", HENHOUSE_SPRUCE);
    public static final DeferredItem<BlockItem> HENHOUSE_BIRCH_ITEM = registerHenhouseItem("henhouse_birch", HENHOUSE_BIRCH);
    public static final DeferredItem<BlockItem> HENHOUSE_JUNGLE_ITEM = registerHenhouseItem("henhouse_jungle", HENHOUSE_JUNGLE);
    public static final DeferredItem<BlockItem> HENHOUSE_ACACIA_ITEM = registerHenhouseItem("henhouse_acacia", HENHOUSE_ACACIA);
    public static final DeferredItem<BlockItem> HENHOUSE_DARK_OAK_ITEM = registerHenhouseItem("henhouse_dark_oak", HENHOUSE_DARK_OAK);
    public static final DeferredItem<BlockItem> ROOST_ITEM = ITEMS.register("roost", () -> new BlockItem(ROOST.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NEST_ITEM = ITEMS.register("nest",
            () -> new BlockItem(NEST.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BREEDER_ITEM = ITEMS.register("breeder",
            () -> new BlockItem(BREEDER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> COLLECTOR_ITEM = ITEMS.register("collector",
            () -> new BlockItem(COLLECTOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AVIAN_FLUX_CONVERTER_ITEM = ITEMS.register("avian_flux_converter",
            () -> new BlockItem(AVIAN_FLUX_CONVERTER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AVIAN_FLUID_CONVERTER_ITEM = ITEMS.register("avian_fluid_converter",
            () -> new BlockItem(AVIAN_FLUID_CONVERTER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AVIAN_CHEMICAL_CONVERTER_ITEM = ITEMS.register("avian_chemical_converter",
            () -> new BlockItem(AVIAN_CHEMICAL_CONVERTER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AVIAN_DOUSING_MACHINE_ITEM = ITEMS.register("avian_dousing_machine",
            () -> new BlockItem(AVIAN_DOUSING_MACHINE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> INCUBATOR_ITEM = ITEMS.register("incubator",
            () -> new BlockItem(INCUBATOR.get(), new Item.Properties()));

    private static final List<DeferredItem<BlockItem>> HENHOUSE_ITEMS = List.of(
            HENHOUSE_ITEM, HENHOUSE_SPRUCE_ITEM, HENHOUSE_BIRCH_ITEM,
            HENHOUSE_JUNGLE_ITEM, HENHOUSE_ACACIA_ITEM, HENHOUSE_DARK_OAK_ITEM
    );
    public static final DeferredItem<ChickenItem> CHICKEN_ITEM = ITEMS.register("chicken",
            () -> new ChickenItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ChickenCatcherItem> CATCHER = ITEMS.register("catcher",
            () -> new ChickenCatcherItem(new Item.Properties().stacksTo(1).durability(64)));
    public static final DeferredItem<CreativeCatcherItem> CREATIVE_CATCHER = ITEMS.register("creative_catcher",
            () -> new CreativeCatcherItem(new Item.Properties().stacksTo(1).component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    public static List<DeferredItem<BlockItem>> getHenhouseItems() {
        return Collections.unmodifiableList(HENHOUSE_ITEMS);
    }

    private static DeferredBlock<HenhouseBlock> registerHenhouse(String name, MapColor color) {
        return BLOCKS.register(name, () -> new HenhouseBlock(color));
    }

    private static DeferredItem<BlockItem> registerHenhouseItem(String name, DeferredBlock<HenhouseBlock> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}