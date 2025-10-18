package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import javax.annotation.Nullable;

/**
 * Simplified roost block that mirrors the legacy interaction rules. Players can
 * insert chicken items, extract them while sneaking, or open the menu for
 * manual automation.
 */
public class RoostBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RoostBlock> CODEC = simpleCodec(RoostBlock::new);

    public RoostBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 5.0F).sound(SoundType.WOOD));
    }

    public RoostBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<RoostBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoostBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.ROOST.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof RoostBlockEntity roost) {
                AbstractChickenContainerBlockEntity.serverTick(lvl, pos, blockState, roost);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RoostBlockEntity roost)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && roost.pullChickenOut(player)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ((IPlayerExtension) serverPlayer).openMenu(roost, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RoostBlockEntity roost) {
                Containers.dropContents(level, pos, roost.getItems());
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
