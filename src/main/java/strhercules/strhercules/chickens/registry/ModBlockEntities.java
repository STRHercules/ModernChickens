package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.blockentity.AvianChemicalConverterBlockEntity;
import strhercules.chickens.blockentity.AvianFluxConverterBlockEntity;
import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import strhercules.chickens.blockentity.AvianFluidConverterBlockEntity;
import strhercules.chickens.blockentity.BreederBlockEntity;
import strhercules.chickens.blockentity.CollectorBlockEntity;
import strhercules.chickens.blockentity.IncubatorBlockEntity;
import strhercules.chickens.blockentity.HenhouseBlockEntity;
import strhercules.chickens.blockentity.RoostBlockEntity;
import strhercules.chickens.blockentity.NestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Houses all block entity registrations for Modern Chickens. Keeping the logic
 * here avoids cluttering {@link ModRegistry} with type builders.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChickensMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HenhouseBlockEntity>> HENHOUSE = BLOCK_ENTITIES
            .register("henhouse", () -> BlockEntityType.Builder
                    .of(HenhouseBlockEntity::new, henhouseBlocks())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RoostBlockEntity>> ROOST = BLOCK_ENTITIES
            .register("roost", () -> BlockEntityType.Builder
                    .of(RoostBlockEntity::new, ModRegistry.ROOST.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestBlockEntity>> NEST = BLOCK_ENTITIES
            .register("nest", () -> BlockEntityType.Builder
                    .of(NestBlockEntity::new, ModRegistry.NEST.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreederBlockEntity>> BREEDER = BLOCK_ENTITIES
            .register("breeder", () -> BlockEntityType.Builder
                    .of(BreederBlockEntity::new, ModRegistry.BREEDER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CollectorBlockEntity>> COLLECTOR = BLOCK_ENTITIES
            .register("collector", () -> BlockEntityType.Builder
                    .of(CollectorBlockEntity::new, ModRegistry.COLLECTOR.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AvianFluxConverterBlockEntity>> AVIAN_FLUX_CONVERTER = BLOCK_ENTITIES
            .register("avian_flux_converter", () -> BlockEntityType.Builder
                    .of(AvianFluxConverterBlockEntity::new, ModRegistry.AVIAN_FLUX_CONVERTER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AvianFluidConverterBlockEntity>> AVIAN_FLUID_CONVERTER = BLOCK_ENTITIES
            .register("avian_fluid_converter", () -> BlockEntityType.Builder
                    .of(AvianFluidConverterBlockEntity::new, ModRegistry.AVIAN_FLUID_CONVERTER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AvianChemicalConverterBlockEntity>> AVIAN_CHEMICAL_CONVERTER = BLOCK_ENTITIES
            .register("avian_chemical_converter", () -> BlockEntityType.Builder
                    .of(AvianChemicalConverterBlockEntity::new, ModRegistry.AVIAN_CHEMICAL_CONVERTER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AvianDousingMachineBlockEntity>> AVIAN_DOUSING_MACHINE = BLOCK_ENTITIES
            .register("avian_dousing_machine", () -> BlockEntityType.Builder
                    .of(AvianDousingMachineBlockEntity::new, ModRegistry.AVIAN_DOUSING_MACHINE.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IncubatorBlockEntity>> INCUBATOR = BLOCK_ENTITIES
            .register("incubator", () -> BlockEntityType.Builder
                    .of(IncubatorBlockEntity::new, ModRegistry.INCUBATOR.get())
                    .build(null));

    private ModBlockEntities() {
    }

    public static void init(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    private static Block[] henhouseBlocks() {
        // Expand the type to recognise every wood variant instead of only the oak block.
        return ModRegistry.HENHOUSE_BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new);
    }
}
