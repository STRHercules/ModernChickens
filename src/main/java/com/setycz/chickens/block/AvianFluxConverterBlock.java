package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Surface the carried RF so players can confirm their preserved charge without placing the block back down.
        EnergySnapshot snapshot = readEnergySnapshot(stack);
        tooltip.add(Component.translatable("tooltip.chickens.avian_flux_converter.energy",
                Component.literal(formatEnergy(snapshot.energy())).withStyle(ChatFormatting.GOLD),
                Component.literal(formatEnergy(snapshot.capacity())).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));
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

    /**
     * Pulls the preserved block entity data off the item stack so the tooltip can mirror
     * the exact RF buffer that will be restored when the converter is placed again.
     */
    private static EnergySnapshot readEnergySnapshot(ItemStack stack) {
        CompoundTag data = extractBlockEntityTag(stack);
        int capacity = AvianFluxConverterBlockEntity.DEFAULT_CAPACITY;
        if (data != null && data.contains("Capacity")) {
            capacity = Mth.clamp(data.getInt("Capacity"), 1, AvianFluxConverterBlockEntity.DEFAULT_CAPACITY);
        }
        int energy = 0;
        if (data != null && data.contains("Energy")) {
            energy = data.getInt("Energy");
        }
        energy = Mth.clamp(energy, 0, capacity);
        return new EnergySnapshot(energy, capacity);
    }

    @Nullable
    private static CompoundTag extractBlockEntityTag(ItemStack stack) {
        CustomData component = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (component.isEmpty()) {
            return null;
        }

        CompoundTag tag = component.copyTag();
        if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            // saveToItem writes the payload inside BlockEntityTag so unwrap it
            // when present. Older stacks written before the NeoForge data
            // component migration already contain the raw payload so fall back
            // to the root tag when the nested structure is missing.
            return tag.getCompound("BlockEntityTag");
        }
        return tag;
    }

    /**
     * Formats large RF totals with grouping separators to keep the tooltip legible at a glance.
     */
    private static String formatEnergy(int value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    private record EnergySnapshot(int energy, int capacity) {
    }
}
