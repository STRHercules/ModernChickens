package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.block.AvianChemicalConverterBlock;
import strhercules.chickens.block.AvianFluxConverterBlock;
import strhercules.chickens.block.AvianDousingMachineBlock;
import strhercules.chickens.block.AvianFluidConverterBlock;
import strhercules.chickens.block.BreederBlock;
import strhercules.chickens.block.CollectorBlock;
import strhercules.chickens.block.IncubatorBlock;
import strhercules.chickens.block.MechanicalRoostBlock;
import strhercules.chickens.block.MechanicalNestBlock;
import strhercules.chickens.block.HenhouseBlock;
import strhercules.chickens.block.LavaChickenFireBlock;
import strhercules.chickens.block.RoostBlock;
import strhercules.chickens.block.NestBlock;
import strhercules.chickens.item.AnalyzerItem;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.item.ColoredEggItem;
import strhercules.chickens.item.FluxEggItem;
import strhercules.chickens.item.FlyingEggItem;
import strhercules.chickens.item.ChickenItem;
import strhercules.chickens.item.ChickenCatcherItem;
import strhercules.chickens.item.CreativeCatcherItem;
import strhercules.chickens.item.LiquidEggItem;
import strhercules.chickens.item.LavaChickenItem;
import strhercules.chickens.item.ChemicalEggItem;
import strhercules.chickens.item.GasEggItem;
import strhercules.chickens.item.MegaChickenItem;
import strhercules.chickens.item.MegaChickenSkinCrateItem;
import strhercules.chickens.item.MachineConfiguratorItem;
import strhercules.chickens.entity.MegaChickenSkin;
import strhercules.chickens.item.UpgradeItem;
import strhercules.chickens.item.RobotSpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.nbt.CompoundTag;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Collections;
import java.util.List;

public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChickensMod.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ChickensMod.MOD_ID);

    private ModRegistry() {
    }

    public static void init(IEventBus modBus) {
        ModEffects.init(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ModEntityTypes.init(modBus);
        ModBlockEntities.init(modBus);
        ModMenuTypes.init(modBus);
        ModRecipeTypes.init(modBus);
        ModSpawns.init(modBus);
        ModBiomeModifiers.init(modBus);
        ModCreativeTabs.init(modBus);
    }

    public static final RegistryObject<ChickensSpawnEggItem> SPAWN_EGG = ITEMS.register("spawn_egg",
            () -> new ChickensSpawnEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<ForgeSpawnEggItem> ROOSTER_SPAWN_EGG = ITEMS.register("rooster_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.ROOSTER, 0xD6B26D, 0x9B2D20,
                    new Item.Properties().stacksTo(64)));
    public static final RegistryObject<ForgeSpawnEggItem> MEGA_CHICKEN_SPAWN_EGG = ITEMS.register("mega_chicken_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.MEGA_CHICKEN, 0xFFFFFF, 0xD51F1F,
                    new Item.Properties().stacksTo(64)));
    public static final RegistryObject<RobotSpawnEggItem> ROBOT_CHICKEN_SPAWN_EGG = ITEMS.register("robot_chicken_spawn_egg",
            () -> new RobotSpawnEggItem(ModEntityTypes.CHICKENS_CHICKEN, 0x4A4A4A, 0xC0C0C0,
                    new Item.Properties().stacksTo(64), ModRegistry::robotChickenSpawnData));
    public static final RegistryObject<RobotSpawnEggItem> ROBOT_ROOSTER_SPAWN_EGG = ITEMS.register("robot_rooster_spawn_egg",
            () -> new RobotSpawnEggItem(ModEntityTypes.ROOSTER, 0x4A4A4A, 0xD6A32D,
                    new Item.Properties().stacksTo(64), ModRegistry::robotRoosterSpawnData));
    public static final RegistryObject<ColoredEggItem> COLORED_EGG = ITEMS.register("colored_egg",
            () -> new ColoredEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<LiquidEggItem> LIQUID_EGG = ITEMS.register("liquid_egg",
            () -> new LiquidEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<ChemicalEggItem> CHEMICAL_EGG = ITEMS.register("chemical_egg",
            () -> new ChemicalEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<GasEggItem> GAS_EGG = ITEMS.register("gas_egg",
            () -> new GasEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<FluxEggItem> FLUX_EGG = ITEMS.register("flux_egg",
            () -> new FluxEggItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<FlyingEggItem> FLYING_EGG = ITEMS.register("flying_egg",
            () -> new FlyingEggItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<LavaChickenItem> LAVA_CHICKEN = ITEMS.register("lava_chicken",
            () -> new LavaChickenItem(new Item.Properties()
                    .stacksTo(16)
                    .fireResistant()));
    public static final RegistryObject<UpgradeItem> SPEED_UPGRADE = ITEMS.register("speedupgrade",
            () -> new UpgradeItem(new Item.Properties().stacksTo(64), UpgradeItem.Kind.SPEED));
    public static final RegistryObject<UpgradeItem> STACK_UPGRADE = ITEMS.register("stackupgrade",
            () -> new UpgradeItem(new Item.Properties().stacksTo(64), UpgradeItem.Kind.STACK));
    public static final RegistryObject<UpgradeItem> STORAGE_CAPACITY_UPGRADE = ITEMS.register("storagecapacity",
            () -> new UpgradeItem(new Item.Properties().stacksTo(64), UpgradeItem.Kind.STORAGE));
    public static final RegistryObject<UpgradeItem> RANGE_UPGRADE = ITEMS.register("rangeupgrade",
            () -> new UpgradeItem(new Item.Properties().stacksTo(64), UpgradeItem.Kind.RANGE));
    public static final RegistryObject<UpgradeItem> RF_UPGRADE = ITEMS.register("rfupgrade",
            () -> new UpgradeItem(new Item.Properties().stacksTo(64), UpgradeItem.Kind.RF));
    public static final RegistryObject<MachineConfiguratorItem> CONFIGURATOR = ITEMS.register("configurator",
            () -> new MachineConfiguratorItem(new Item.Properties()));
    public static final RegistryObject<AnalyzerItem> ANALYZER = ITEMS.register("analyzer",
            () -> new AnalyzerItem(new Item.Properties().durability(238)));
    public static final RegistryObject<RoostBlock> ROOST = BLOCKS.register("roost", () -> new RoostBlock());
    public static final RegistryObject<NestBlock> NEST = BLOCKS.register("nest", () -> new NestBlock());
    public static final RegistryObject<BreederBlock> BREEDER = BLOCKS.register("breeder", () -> new BreederBlock());
    public static final RegistryObject<CollectorBlock> COLLECTOR = BLOCKS.register("collector", () -> new CollectorBlock());
    public static final RegistryObject<LavaChickenFireBlock> LAVA_CHICKEN_FIRE = BLOCKS.register("lava_chicken_fire",
            () -> new LavaChickenFireBlock());
    public static final RegistryObject<AvianFluxConverterBlock> AVIAN_FLUX_CONVERTER = BLOCKS.register("avian_flux_converter", () -> new AvianFluxConverterBlock());
    public static final RegistryObject<AvianFluidConverterBlock> AVIAN_FLUID_CONVERTER = BLOCKS.register("avian_fluid_converter", () -> new AvianFluidConverterBlock());
    public static final RegistryObject<AvianChemicalConverterBlock> AVIAN_CHEMICAL_CONVERTER = BLOCKS.register("avian_chemical_converter", () -> new AvianChemicalConverterBlock());
    public static final RegistryObject<AvianDousingMachineBlock> AVIAN_DOUSING_MACHINE = BLOCKS.register("avian_dousing_machine",
            () -> new AvianDousingMachineBlock());
    public static final RegistryObject<IncubatorBlock> INCUBATOR = BLOCKS.register("incubator", () -> new IncubatorBlock());
    public static final RegistryObject<MechanicalRoostBlock> MECHANICAL_ROOST = BLOCKS.register("mechanical_roost",
            () -> new MechanicalRoostBlock());
    public static final RegistryObject<MechanicalNestBlock> MECHANICAL_NEST = BLOCKS.register("mechanical_nest",
            () -> new MechanicalNestBlock());
    public static final RegistryObject<HenhouseBlock> HENHOUSE = registerHenhouse("henhouse", MapColor.COLOR_BROWN);
    public static final RegistryObject<HenhouseBlock> HENHOUSE_SPRUCE = registerHenhouse("henhouse_spruce", MapColor.COLOR_BROWN);
    public static final RegistryObject<HenhouseBlock> HENHOUSE_BIRCH = registerHenhouse("henhouse_birch", MapColor.COLOR_BROWN);
    public static final RegistryObject<HenhouseBlock> HENHOUSE_JUNGLE = registerHenhouse("henhouse_jungle", MapColor.COLOR_BROWN);
    public static final RegistryObject<HenhouseBlock> HENHOUSE_ACACIA = registerHenhouse("henhouse_acacia", MapColor.COLOR_BROWN);
    public static final RegistryObject<HenhouseBlock> HENHOUSE_DARK_OAK = registerHenhouse("henhouse_dark_oak", MapColor.COLOR_BROWN);

    public static final List<RegistryObject<HenhouseBlock>> HENHOUSE_BLOCKS = List.of(
            HENHOUSE, HENHOUSE_SPRUCE, HENHOUSE_BIRCH, HENHOUSE_JUNGLE, HENHOUSE_ACACIA, HENHOUSE_DARK_OAK
    );

    public static final RegistryObject<BlockItem> HENHOUSE_ITEM = registerHenhouseItem("henhouse", HENHOUSE);
    public static final RegistryObject<BlockItem> HENHOUSE_SPRUCE_ITEM = registerHenhouseItem("henhouse_spruce", HENHOUSE_SPRUCE);
    public static final RegistryObject<BlockItem> HENHOUSE_BIRCH_ITEM = registerHenhouseItem("henhouse_birch", HENHOUSE_BIRCH);
    public static final RegistryObject<BlockItem> HENHOUSE_JUNGLE_ITEM = registerHenhouseItem("henhouse_jungle", HENHOUSE_JUNGLE);
    public static final RegistryObject<BlockItem> HENHOUSE_ACACIA_ITEM = registerHenhouseItem("henhouse_acacia", HENHOUSE_ACACIA);
    public static final RegistryObject<BlockItem> HENHOUSE_DARK_OAK_ITEM = registerHenhouseItem("henhouse_dark_oak", HENHOUSE_DARK_OAK);
    public static final RegistryObject<BlockItem> ROOST_ITEM = ITEMS.register("roost", () -> new BlockItem(ROOST.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> NEST_ITEM = ITEMS.register("nest",
            () -> new BlockItem(NEST.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> BREEDER_ITEM = ITEMS.register("breeder",
            () -> new BlockItem(BREEDER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> COLLECTOR_ITEM = ITEMS.register("collector",
            () -> new BlockItem(COLLECTOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> AVIAN_FLUX_CONVERTER_ITEM = ITEMS.register("avian_flux_converter",
            () -> new BlockItem(AVIAN_FLUX_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> AVIAN_FLUID_CONVERTER_ITEM = ITEMS.register("avian_fluid_converter",
            () -> new BlockItem(AVIAN_FLUID_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> AVIAN_CHEMICAL_CONVERTER_ITEM = ITEMS.register("avian_chemical_converter",
            () -> new BlockItem(AVIAN_CHEMICAL_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> AVIAN_DOUSING_MACHINE_ITEM = ITEMS.register("avian_dousing_machine",
            () -> new BlockItem(AVIAN_DOUSING_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> INCUBATOR_ITEM = ITEMS.register("incubator",
            () -> new BlockItem(INCUBATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> MECHANICAL_ROOST_ITEM = ITEMS.register("mechanical_roost",
            () -> new BlockItem(MECHANICAL_ROOST.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> MECHANICAL_NEST_ITEM = ITEMS.register("mechanical_nest",
            () -> new BlockItem(MECHANICAL_NEST.get(), new Item.Properties()));

    private static final List<RegistryObject<BlockItem>> HENHOUSE_ITEMS = List.of(
            HENHOUSE_ITEM, HENHOUSE_SPRUCE_ITEM, HENHOUSE_BIRCH_ITEM,
            HENHOUSE_JUNGLE_ITEM, HENHOUSE_ACACIA_ITEM, HENHOUSE_DARK_OAK_ITEM
    );
    public static final RegistryObject<ChickenItem> CHICKEN_ITEM = ITEMS.register("chicken",
            () -> new ChickenItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<ChickenItem> ROBOT_CHICKEN_ITEM = ITEMS.register("robot_chicken",
            () -> new ChickenItem(robotChickenItemProperties()));
    public static final RegistryObject<ChickenItem> ROBOT_ROOSTER_ITEM = ITEMS.register("robot_rooster",
            () -> new ChickenItem(robotRoosterItemProperties()));
    public static final RegistryObject<ChickenCatcherItem> CATCHER = ITEMS.register("catcher",
            () -> new ChickenCatcherItem(new Item.Properties().stacksTo(1).durability(64)));
    public static final RegistryObject<MegaChickenItem> MEGA_CHICKEN_ITEM = ITEMS.register("mega_chicken",
            () -> new MegaChickenItem(new Item.Properties()));
    public static final RegistryObject<MegaChickenSkinCrateItem> ZOMBIE_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.ZOMBIE);
    public static final RegistryObject<MegaChickenSkinCrateItem> VALENTINES_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.VALENTINES);
    public static final RegistryObject<MegaChickenSkinCrateItem> TOXIC_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.TOXIC);
    public static final RegistryObject<MegaChickenSkinCrateItem> REPTAR_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.REPTAR);
    public static final RegistryObject<MegaChickenSkinCrateItem> RAMBO_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.RAMBO);
    public static final RegistryObject<MegaChickenSkinCrateItem> PINK_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.PINK);
    public static final RegistryObject<MegaChickenSkinCrateItem> FOX_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.FOX);
    public static final RegistryObject<MegaChickenSkinCrateItem> DUCK_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.DUCK);
    public static final RegistryObject<MegaChickenSkinCrateItem> DODO_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.DODO);
    public static final RegistryObject<MegaChickenSkinCrateItem> DEEPDARK_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.DEEPDARK);
    public static final RegistryObject<MegaChickenSkinCrateItem> CREEPER_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.CREEPER);
    public static final RegistryObject<MegaChickenSkinCrateItem> BIGBRAIN_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.BIGBRAIN);
    public static final RegistryObject<MegaChickenSkinCrateItem> AVIATOR_SKIN_CRATE = registerSkinCrate(MegaChickenSkin.AVIATOR);
    public static final RegistryObject<CreativeCatcherItem> CREATIVE_CATCHER = ITEMS.register("creative_catcher",
            () -> new CreativeCatcherItem(new Item.Properties().stacksTo(1)));

    public static List<RegistryObject<BlockItem>> getHenhouseItems() {
        return Collections.unmodifiableList(HENHOUSE_ITEMS);
    }

    public static RegistryObject<MegaChickenSkinCrateItem> skinCrate(MegaChickenSkin skin) {
        return switch (skin) {
            case ZOMBIE -> ZOMBIE_SKIN_CRATE;
            case VALENTINES -> VALENTINES_SKIN_CRATE;
            case TOXIC -> TOXIC_SKIN_CRATE;
            case REPTAR -> REPTAR_SKIN_CRATE;
            case RAMBO -> RAMBO_SKIN_CRATE;
            case PINK -> PINK_SKIN_CRATE;
            case FOX -> FOX_SKIN_CRATE;
            case DUCK -> DUCK_SKIN_CRATE;
            case DODO -> DODO_SKIN_CRATE;
            case DEEPDARK -> DEEPDARK_SKIN_CRATE;
            case CREEPER -> CREEPER_SKIN_CRATE;
            case BIGBRAIN -> BIGBRAIN_SKIN_CRATE;
            case AVIATOR -> AVIATOR_SKIN_CRATE;
        };
    }

    private static RegistryObject<MegaChickenSkinCrateItem> registerSkinCrate(MegaChickenSkin skin) {
        return ITEMS.register(skin.id() + "_skin_crate",
                () -> new MegaChickenSkinCrateItem(new Item.Properties(), skin));
    }

    private static Item.Properties robotChickenItemProperties() {
        return new Item.Properties().stacksTo(16);
    }

    private static Item.Properties robotRoosterItemProperties() {
        return new Item.Properties().stacksTo(16);
    }

    private static CompoundTag robotChickenSpawnData() {
        CompoundTag data = new CompoundTag();
        data.putInt("Type", ChickensRegistry.SMART_CHICKEN_ID);
        data.putBoolean("RobotChicken", true);
        return data;
    }

    private static CompoundTag robotRoosterSpawnData() {
        CompoundTag data = new CompoundTag();
        data.putBoolean("RobotRooster", true);
        return data;
    }

    private static RegistryObject<HenhouseBlock> registerHenhouse(String name, MapColor color) {
        return BLOCKS.register(name, () -> new HenhouseBlock(color));
    }

    private static RegistryObject<BlockItem> registerHenhouseItem(String name, RegistryObject<HenhouseBlock> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
