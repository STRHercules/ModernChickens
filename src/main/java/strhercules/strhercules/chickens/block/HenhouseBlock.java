package strhercules.chickens.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.InteractionHand;
import strhercules.chickens.blockentity.HenhouseBlockEntity;
import strhercules.chickens.integration.mekanism.MekanismRadiationCompat;
import strhercules.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Modernised henhouse block that keeps the legacy facing and tooltip behaviour
 * while delegating the heavy lifting to {@link HenhouseBlockEntity}. The block
 * itself mostly handles user interaction and inventory drops when broken.
 */
public class HenhouseBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public HenhouseBlock() {
        this(MapColor.COLOR_BROWN);
    }

    /**
     * Allows registry code to pick a different map colour so each wood variant
     * better matches its plank tone while reusing the same behaviour.
     */
    public HenhouseBlock(MapColor mapColor) {
        this(Properties.of().mapColor(mapColor).strength(2.0F, 2.5F).sound(SoundType.WOOD));
    }

    private HenhouseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        // Reuse the legacy tooltip text so players are reminded about the
        // automation radius and hay bale mechanic without checking a wiki.
        tooltip.add(Component.translatable("tooltip.chickens.henhouse"));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The original block used a baked model, so the modern version keeps
        // the standard MODEL render type.
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Each block hosts a dedicated henhouse block entity that stores the
        // hay, dirt, and egg inventory plus the internal energy counter.
        return new HenhouseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.HENHOUSE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof HenhouseBlockEntity henhouse) {
                MekanismRadiationCompat.tickMachineWarning(lvl, pos, henhouse);
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        // Match the classic behaviour by facing the player when placed.
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasCustomHoverName()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HenhouseBlockEntity henhouse) {
                // Preserve custom item names just like the legacy TileEntity did.
                henhouse.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HenhouseBlockEntity henhouse && player instanceof ServerPlayer serverPlayer) {
            // NetworkHooks bridges menu opening and syncs the block position to the client.
            NetworkHooks.openScreen(serverPlayer, henhouse, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HenhouseBlockEntity henhouse) {
                // Drop every stored stack when the block is broken to mirror the
                // legacy InventoryHelper behaviour.
                Containers.dropContents(level, pos, henhouse.getItems());
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (MachineBlockHelper.canHarvestWith(tool)) {
            MachineBlockHelper.dropMachine(level, pos, this, blockEntity, player);
        }
    }
}
