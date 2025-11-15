package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import javax.annotation.Nullable;
import java.util.List;

/**
 * RF-powered machine that incubates chicken spawn eggs into their itemified
 * counterparts. The block mirrors the other automation-friendly machines by
 * exposing facing/lit properties, preserving inventory data on break, and
 * offering comparator support for automation chains.
 */
public class IncubatorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<IncubatorBlock> CODEC = simpleCodec(IncubatorBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public IncubatorBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIT) ? 9 : 0));
    }

    public IncubatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    public MapCodec<IncubatorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncubatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.INCUBATOR.get()) {
            return null;
        }
        return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof IncubatorBlockEntity incubator) {
                IncubatorBlockEntity.serverTick(lvl, blockPos, blockState, incubator);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IncubatorBlockEntity incubator)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ((IPlayerExtension) serverPlayer).openMenu(incubator, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IncubatorBlockEntity incubator) {
                Containers.dropContents(level, pos, incubator);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (canHarvest(tool)) {
            if (!level.isClientSide && !player.isCreative()) {
                ItemStack drop = new ItemStack(this);
                if (blockEntity instanceof IncubatorBlockEntity incubator) {
                    incubator.saveToItem(drop, level.registryAccess());
                    var customName = incubator.getCustomName();
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

    private static boolean canHarvest(ItemStack tool) {
        return !tool.isEmpty() && tool.is(ItemTags.PICKAXES);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IncubatorBlockEntity incubator) {
                incubator.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chickens.incubator"));
        int capacity = Math.max(1, ChickensConfigHolder.get().getIncubatorEnergyCapacity());
        int energy = 0;
        int cost = Math.max(1, ChickensConfigHolder.get().getIncubatorEnergyCost());
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (!data.isEmpty()) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("Energy")) {
                energy = Math.max(0, tag.getInt("Energy"));
            }
            if (tag.contains("EnergyCost")) {
                cost = Math.max(1, tag.getInt("EnergyCost"));
            }
        }
        tooltip.add(Component.translatable("tooltip.chickens.incubator.energy", energy, capacity));
        tooltip.add(Component.translatable("tooltip.chickens.incubator.cost", cost));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IncubatorBlockEntity incubator) {
            return incubator.getComparatorOutput();
        }
        return 0;
    }
}
