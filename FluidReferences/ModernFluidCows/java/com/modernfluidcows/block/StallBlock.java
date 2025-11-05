package com.modernfluidcows.block;

import com.modernfluidcows.blockentity.StallBlockEntity;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Cow Stall block that stores a captured fluid cow and exposes its tank to players.
 */
public class StallBlock extends Block implements EntityBlock {
    public static final MapCodec<StallBlock> CODEC = simpleCodec(StallBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_COW = BooleanProperty.create("cow");

    private static final VoxelShape OUTLINE = Shapes.box(0.065D, 0.0D, 0.06D, 0.935D, 0.96D, 0.94D);

    public StallBlock(final Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_COW, false));
    }

    @Override
    public MapCodec<StallBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    public VoxelShape getShape(
            final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return OUTLINE;
    }

    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HAS_COW, false);
    }

    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_COW);
    }

    public InteractionResult use(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof StallBlockEntity stall)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (stall.giveOutput(player)) {
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(stall, pos);
        }
        return InteractionResult.CONSUME;
    }

    public void onRemove(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState newState,
            final boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof StallBlockEntity stall) {
                stall.spawnCow(level);
                for (int slot = 0; slot < stall.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = stall.getInventory().getItem(slot);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(
                                level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                    }
                }
                stall.clearContent();
            }
            // Clear any cached fluid handlers so pipes stop targeting the removed stall.
            level.invalidateCapabilities(pos);
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new StallBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (type != FluidCowsRegistries.STALL_BLOCK_ENTITY.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, blockState, be) -> StallBlockEntity.clientTick(lvl, pos, blockState, (StallBlockEntity) be)
                : (lvl, pos, blockState, be) -> StallBlockEntity.serverTick(lvl, pos, blockState, (StallBlockEntity) be);
    }
}
