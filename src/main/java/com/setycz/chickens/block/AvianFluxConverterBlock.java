package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Horizontal machine that accepts Flux Eggs and converts their stored RF into a
 * persistent buffer. The block mirrors the legacy roost machines by exposing a
 * menu, dropping its inventory, and respecting comparator output updates.
 */
public class AvianFluxConverterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<AvianFluxConverterBlock> CODEC = simpleCodec(AvianFluxConverterBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AvianFluxConverterBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    public AvianFluxConverterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<AvianFluxConverterBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AvianFluxConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.AVIAN_FLUX_CONVERTER.get()) {
            return null;
        }
        return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
                AvianFluxConverterBlockEntity.serverTick(lvl, blockPos, blockState, converter);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AvianFluxConverterBlockEntity converter)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ((IPlayerExtension) serverPlayer).openMenu(converter, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
                Containers.dropContents(level, pos, converter.getItems());
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (MachineBlockHelper.canHarvestWith(tool)) {
            // Preserve the converter's stored RF by copying the block entity data into the
            // dropped item so the machine resumes with the same buffer when replaced.
            if (!level.isClientSide && !player.isCreative()) {
                ItemStack drop = new ItemStack(this);
                if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
                    converter.saveToItem(drop, level.registryAccess());
                    var customName = converter.getCustomName();
                    if (customName != null) {
                        drop.set(DataComponents.CUSTOM_NAME, customName);
                    }
                }
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
                converter.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
            return converter.getComparatorOutput();
        }
        return super.getAnalogOutputSignal(state, level, pos);
    }
}
