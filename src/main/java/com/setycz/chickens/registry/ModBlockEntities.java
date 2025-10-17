package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
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
