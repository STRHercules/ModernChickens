package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.blockentity.AbstractChickenContainerBlockEntity;
import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import javax.annotation.Nullable;

/**
 * Modernised breeder block that mirrors the 1.12 behaviour. Chickens can be
 * inserted directly, sneaking removes the most recent parent, and seeds may be
 * supplied without opening the menu.
 */
public class BreederBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BreederBlock> CODEC = simpleCodec(BreederBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty BREEDING = BooleanProperty.create("breeding");
    public static final BooleanProperty HAS_SEEDS = BooleanProperty.create("has_seeds");

    public BreederBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 5.0F).sound(SoundType.WOOD));
    }

    public BreederBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(BREEDING, Boolean.FALSE)
                .setValue(HAS_SEEDS, Boolean.FALSE));
    }

    @Override
    public MapCodec<BreederBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.BREEDER.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof BreederBlockEntity breeder) {
                AbstractChickenContainerBlockEntity.serverTick(lvl, pos, blockState, breeder);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BreederBlockEntity breeder)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && breeder.extractChicken(player)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ((IPlayerExtension) serverPlayer).openMenu(breeder, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BreederBlockEntity breeder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.isEmpty() && (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS))) {
            if (!level.isClientSide && breeder.addSeeds(stack)) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BreederBlockEntity breeder) {
                Containers.dropContents(level, pos, breeder.getItems());
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
        builder.add(FACING, BREEDING, HAS_SEEDS);
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
}
