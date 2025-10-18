package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.blockentity.CollectorBlockEntity;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreederBlockEntity>> BREEDER = BLOCK_ENTITIES
            .register("breeder", () -> BlockEntityType.Builder
                    .of(BreederBlockEntity::new, ModRegistry.BREEDER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CollectorBlockEntity>> COLLECTOR = BLOCK_ENTITIES
            .register("collector", () -> BlockEntityType.Builder
                    .of(CollectorBlockEntity::new, ModRegistry.COLLECTOR.get())
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
