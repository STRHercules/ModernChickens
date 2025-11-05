package com.modernfluidcows.registry;

import com.modernfluidcows.ModernFluidCows;
import com.modernfluidcows.block.AcceleratorBlock;
import com.modernfluidcows.block.FeederBlock;
import com.modernfluidcows.block.StallBlock;
import com.modernfluidcows.block.SorterBlock;
import com.modernfluidcows.blockentity.AcceleratorBlockEntity;
import com.modernfluidcows.blockentity.FeederBlockEntity;
import com.modernfluidcows.blockentity.SorterBlockEntity;
import com.modernfluidcows.blockentity.StallBlockEntity;
import com.modernfluidcows.menu.AcceleratorMenu;
import com.modernfluidcows.menu.FeederMenu;
import com.modernfluidcows.menu.SorterMenu;
import com.modernfluidcows.menu.StallMenu;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.item.CowDisplayerItem;
import com.modernfluidcows.item.CowHalterItem;
import com.modernfluidcows.item.CowRangerItem;
import com.modernfluidcows.item.FluidCowSpawnEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Collection of DeferredRegisters used throughout the mod.
 *
 * <p>The legacy mod registered content during FML pre-initialisation. NeoForge enforces data-driven
 * registries instead, so this helper exposes strongly typed handles that subsequent commits can
 * expand on. Every entry currently uses very small vanilla stand-ins; behaviour will be migrated as
 * the project progresses.</p>
 */
public final class FluidCowsRegistries {
    private FluidCowsRegistries() {}

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ModernFluidCows.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernFluidCows.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernFluidCows.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModernFluidCows.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModernFluidCows.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModernFluidCows.MOD_ID);

    /** Stall block capable of storing a fluid cow and bottling its fluid output. */
    public static final DeferredHolder<Block, StallBlock> STALL_BLOCK =
            BLOCKS.register(
                    "stall",
                    () ->
                            new StallBlock(
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion()));

    /** Accelerator block that consumes wheat and water to accelerate calves. */
    public static final DeferredHolder<Block, AcceleratorBlock> ACCELERATOR_BLOCK =
            BLOCKS.register(
                    "accelerator",
                    () ->
                            new AcceleratorBlock(
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 6.0F)
                                            .requiresCorrectToolForDrops()));

    /** Feeding trough that breeds nearby fluid cows. */
    public static final DeferredHolder<Block, FeederBlock> FEEDER_BLOCK =
            BLOCKS.register(
                    "feeder",
                    () ->
                            new FeederBlock(
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 6.0F)
                                            .requiresCorrectToolForDrops()));

    /** Sorting controller that teleports baby fluid cows based on configured filters. */
    public static final DeferredHolder<Block, SorterBlock> SORTER_BLOCK =
            BLOCKS.register(
                    "sorter",
                    () ->
                            new SorterBlock(
                                    BlockBehaviour.Properties.of()
                                            .strength(2.0F, 6.0F)
                                            .requiresCorrectToolForDrops()));

    /** BlockItem variant for the stall block. */
    public static final DeferredHolder<Item, BlockItem> STALL_ITEM =
            ITEMS.register("stall", () -> new BlockItem(STALL_BLOCK.get(), new Item.Properties()));

    /** BlockItem variant for the accelerator block. */
    public static final DeferredHolder<Item, BlockItem> ACCELERATOR_ITEM =
            ITEMS.register(
                    "accelerator",
                    () -> new BlockItem(ACCELERATOR_BLOCK.get(), new Item.Properties()));

    /** BlockItem variant for the feeder block. */
    public static final DeferredHolder<Item, BlockItem> FEEDER_ITEM =
            ITEMS.register(
                    "feeder",
                    () -> new BlockItem(FEEDER_BLOCK.get(), new Item.Properties()));

    /** BlockItem variant for the sorter block. */
    public static final DeferredHolder<Item, BlockItem> SORTER_ITEM =
            ITEMS.register(
                    "sorter",
                    () -> new BlockItem(SORTER_BLOCK.get(), new Item.Properties()));

    /** Cow halter used to capture and redeploy fluid cows. */
    public static final DeferredHolder<Item, Item> COW_HALTER =
            ITEMS.register("cow_halter", () -> new CowHalterItem(new Item.Properties()));

    /** Item used in the original mod to display captured fluids. */
    public static final DeferredHolder<Item, Item> COW_DISPLAYER =
            ITEMS.register("cow_displayer", () -> new CowDisplayerItem(new Item.Properties()));

    /** Miscellaneous ranger utility item. */
    public static final DeferredHolder<Item, Item> RANGER =
            ITEMS.register("ranger", () -> new CowRangerItem(new Item.Properties()));

    /** Block entity type backing the cow stall. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StallBlockEntity>> STALL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "stall",
                    () -> BlockEntityType.Builder.of(StallBlockEntity::new, STALL_BLOCK.get()).build(null));

    /** Block entity type backing the accelerator. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorBlockEntity>>
            ACCELERATOR_BLOCK_ENTITY =
                    BLOCK_ENTITY_TYPES.register(
                            "accelerator",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    AcceleratorBlockEntity::new, ACCELERATOR_BLOCK.get())
                                            .build(null));

    /** Block entity type backing the feeder. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeederBlockEntity>>
            FEEDER_BLOCK_ENTITY =
                    BLOCK_ENTITY_TYPES.register(
                            "feeder",
                            () -> BlockEntityType.Builder.of(FeederBlockEntity::new, FEEDER_BLOCK.get()).build(null));

    /** Block entity type backing the sorter. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SorterBlockEntity>>
            SORTER_BLOCK_ENTITY =
                    BLOCK_ENTITY_TYPES.register(
                            "sorter",
                            () -> BlockEntityType.Builder.of(SorterBlockEntity::new, SORTER_BLOCK.get()).build(null));

    /** Menu exposed when players open the stall UI. */
    public static final DeferredHolder<MenuType<?>, MenuType<StallMenu>> STALL_MENU =
            MENUS.register(
                    "stall",
                    () ->
                            IMenuTypeExtension.create(
                                    (windowId, inventory, buffer) -> {
                                        BlockPos pos = buffer.readBlockPos();
                                        Level level = inventory.player.level();
                                        if (level != null) {
                                            BlockEntity entity = level.getBlockEntity(pos);
                                            if (entity instanceof StallBlockEntity stall) {
                                                return new StallMenu(windowId, inventory, stall, stall.getDataAccess());
                                            }
                                        }
                                        return StallMenu.createFallback(windowId, inventory);
                                    }));

    /** Menu exposed when players open the accelerator UI. */
    public static final DeferredHolder<MenuType<?>, MenuType<AcceleratorMenu>> ACCELERATOR_MENU =
            MENUS.register(
                    "accelerator",
                    () ->
                            IMenuTypeExtension.create(
                                    (windowId, inventory, buffer) -> {
                                        BlockPos pos = buffer.readBlockPos();
                                        Level level = inventory.player.level();
                                        if (level != null) {
                                            BlockEntity entity = level.getBlockEntity(pos);
                                            if (entity instanceof AcceleratorBlockEntity accelerator) {
                                                return new AcceleratorMenu(
                                                        windowId,
                                                        inventory,
                                                        accelerator,
                                                        accelerator.getInventory(),
                                                        ContainerLevelAccess.create(level, pos),
                                                        accelerator.getDataAccess());
                                            }
                                        }
                                        return AcceleratorMenu.createFallback(windowId, inventory);
                                    }));

    /** Menu exposed when players open the feeder UI. */
    public static final DeferredHolder<MenuType<?>, MenuType<FeederMenu>> FEEDER_MENU =
            MENUS.register(
                    "feeder",
                    () ->
                            IMenuTypeExtension.create(
                                    (windowId, inventory, buffer) -> {
                                        BlockPos pos = buffer.readBlockPos();
                                        Level level = inventory.player.level();
                                        if (level != null) {
                                            BlockEntity entity = level.getBlockEntity(pos);
                                            if (entity instanceof FeederBlockEntity feeder) {
                                                return new FeederMenu(
                                                        windowId,
                                                        inventory,
                                                        feeder,
                                                        feeder.getInventory(),
                                                        ContainerLevelAccess.create(level, pos));
                                            }
                                        }
                                        return FeederMenu.createFallback(windowId, inventory);
                                    }));

    /** Menu exposed when players open the sorter UI. */
    public static final DeferredHolder<MenuType<?>, MenuType<SorterMenu>> SORTER_MENU =
            MENUS.register(
                    "sorter",
                    () ->
                            IMenuTypeExtension.create(
                                    (windowId, inventory, buffer) -> {
                                        BlockPos pos = buffer.readBlockPos();
                                        Level level = inventory.player.level();
                                        if (level != null) {
                                            BlockEntity entity = level.getBlockEntity(pos);
                                            if (entity instanceof SorterBlockEntity sorter) {
                                                return new SorterMenu(
                                                        windowId,
                                                        inventory,
                                                        sorter,
                                                        sorter.getInventory(),
                                                        ContainerLevelAccess.create(level, pos));
                                            }
                                        }
                                        return SorterMenu.createFallback(windowId, inventory);
                                    }));

    /** Entity type representing the fluid cow itself. */
    public static final DeferredHolder<EntityType<?>, EntityType<FluidCow>> FLUID_COW =
            ENTITY_TYPES.register(
                    "fluid_cow",
                    () ->
                            EntityType.Builder.of(FluidCow::new, MobCategory.CREATURE)
                                    .sized(0.9F, 1.4F)
                                    .build(ModernFluidCows.MOD_ID + ":fluid_cow"));

    /** Spawn egg that spawns the configured fluid cow entity for creative testing. */
    public static final DeferredHolder<Item, Item> FLUID_COW_SPAWN_EGG =
            ITEMS.register(
                    "fluid_cow_spawn_egg",
                    () ->
                            new FluidCowSpawnEggItem(
                                    FLUID_COW.get(),
                                    0xA06540, // Brown shell to match the vanilla cow aesthetic.
                                    0x4AA3E2, // Blue accent that reflects the mod's fluid theme.
                                    new Item.Properties()));

    /** Dedicated creative tab so the port keeps parity with the 1.12 feature set. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENT_TAB =
            TABS.register(
                    "fluidcows",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.fluidcows"))
                                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                                    .icon(() -> new ItemStack(STALL_ITEM.get()))
                                    .displayItems(
                                            (parameters, output) -> {
                                                // Display every registered item so that development builds remain testable.
                                                output.accept(STALL_ITEM.get());
                                                output.accept(ACCELERATOR_ITEM.get());
                                                output.accept(FEEDER_ITEM.get());
                                                output.accept(SORTER_ITEM.get());
                                                output.accept(COW_HALTER.get());
                                                ((CowDisplayerItem) COW_DISPLAYER.get())
                                                        .addCreativeStacks(output);
                                                output.accept(FLUID_COW_SPAWN_EGG.get());
                                                output.accept(RANGER.get());
                                            })
                                    .build());

    /** Registers all DeferredRegisters on the given mod event bus. */
    public static void bootstrap(final IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
    }

    /** Wires entity attributes once NeoForge fires the creation event. */
    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(FLUID_COW.get(), FluidCow.createAttributes().build());
    }

    /**
     * Registers the fluid cow spawn placement so natural spawns follow vanilla cattle rules while still
     * honouring the entity's config-backed blacklists.
     */
    public static void registerSpawnPlacements(final RegisterSpawnPlacementsEvent event) {
        event.register(
                FLUID_COW.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Registers capability providers so automation can interact with machine tanks and inventories. */
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                STALL_BLOCK_ENTITY.get(),
                (stall, side) -> stall.getFluidHandler());
    }
}
