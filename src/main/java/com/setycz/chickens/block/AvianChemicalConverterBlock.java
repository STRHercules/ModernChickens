package com.setycz.chickens.block;

import com.mojang.serialization.MapCodec;
import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.blockentity.AvianChemicalConverterBlockEntity;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Block wrapper for the chemical converter. Behaviour mirrors the fluid and
 * flux converters so the machine feels familiar while adding Mekanism chemical
 * support.
 */
public class AvianChemicalConverterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<AvianChemicalConverterBlock> CODEC = simpleCodec(AvianChemicalConverterBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AvianChemicalConverterBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIT) ? 7 : 0));
    }

    public AvianChemicalConverterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    public MapCodec<AvianChemicalConverterBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AvianChemicalConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.AVIAN_CHEMICAL_CONVERTER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) AvianChemicalConverterBlockEntity.serverTicker();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AvianChemicalConverterBlockEntity converter)) {
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
            if (blockEntity instanceof AvianChemicalConverterBlockEntity converter) {
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
        builder.add(FACING, LIT);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (MachineBlockHelper.canHarvestWith(tool)) {
            if (!level.isClientSide && !player.isCreative()) {
                ItemStack drop = new ItemStack(this);
                if (blockEntity instanceof AvianChemicalConverterBlockEntity converter) {
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
            if (blockEntity instanceof AvianChemicalConverterBlockEntity converter) {
                converter.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chickens.avian_chemical_converter"));

        var config = ChickensConfigHolder.get();
        int capacity = Math.max(1, config.getAvianChemicalConverterCapacity(8_000));
        int amount = 0;
        Component chemicalName = Component.translatable("tooltip.chickens.avian_chemical_converter.empty");

        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (!data.isEmpty()) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("ChemicalTank", Tag.TAG_COMPOUND)) {
                CompoundTag tankTag = tag.getCompound("ChemicalTank");
                amount = tankTag.contains("Amount") ? tankTag.getInt("Amount") : amount;
                if (tankTag.contains("Capacity")) {
                    capacity = Math.max(1, tankTag.getInt("Capacity"));
                }
                if (tankTag.contains("Chemical")) {
                    ResourceLocation id = ResourceLocation.tryParse(tankTag.getString("Chemical"));
                    ChemicalEggRegistryItem entry = id != null ? ChemicalEggRegistry.findByChemical(id) : null;
                    if (entry == null && id != null) {
                        entry = GasEggRegistry.findByChemical(id);
                    }
                    if (entry != null) {
                        chemicalName = entry.getDisplayName();
                    } else if (id != null) {
                        chemicalName = Component.literal(id.toString());
                    }
                }
            }
        }
        tooltip.add(Component.translatable("tooltip.chickens.avian_chemical_converter.level", chemicalName, amount, capacity));
    }

    @Override
    public Item asItem() {
        return super.asItem();
    }
}
