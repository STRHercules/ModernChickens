package strhercules.chickens.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.InteractionHand;
import strhercules.chickens.blockentity.AvianFluidConverterBlockEntity;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import strhercules.chickens.item.ItemData;
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
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Machine that converts liquid eggs into a buffered fluid store. Behaviour
 * mirrors the Avian Flux Converter so automation mods can treat both blocks the
 * same: single slot, comparator support, and a lit state while liquids move.
 */
public class AvianFluidConverterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AvianFluidConverterBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIT) ? 7 : 0));
    }

    public AvianFluidConverterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AvianFluidConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.AVIAN_FLUID_CONVERTER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) AvianFluidConverterBlockEntity.serverTicker();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AvianFluidConverterBlockEntity converter)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, converter, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
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
                if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
                    converter.saveToItem(drop);
                    var customName = converter.getCustomName();
                    if (customName != null) {
                        drop.setHoverName(customName);
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
        if (stack.hasCustomHoverName()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
                converter.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chickens.avian_fluid_converter"));

        var config = ChickensConfigHolder.get();
        int capacity = Math.max(1, config.getAvianFluidConverterCapacity(FluidType.BUCKET_VOLUME * 8));
        int amount = 0;
        Component fluidName = Component.translatable("tooltip.chickens.avian_fluid_converter.empty");

        CompoundTag data = ItemData.readBlockEntity(stack);
        if (!data.isEmpty()) {
            CompoundTag tag = data;
            if (tag.contains("Tank", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag tankTag = tag.getCompound("Tank");
                amount = tankTag.contains("Amount") ? tankTag.getInt("Amount") : amount;
                if (tankTag.contains("Capacity")) {
                    capacity = Math.max(1, tankTag.getInt("Capacity"));
                }
                fluidName = resolveFluidName(tankTag, amount);
            }
        }

        tooltip.add(Component.translatable("tooltip.chickens.avian_fluid_converter.level", fluidName, amount, capacity));
    }

    private Component resolveFluidName(CompoundTag tankTag, int amount) {
        String fluidId = "";
        if (tankTag.contains("FluidName")) {
            fluidId = tankTag.getString("FluidName");
        } else if (tankTag.contains("Fluid")) {
            if (tankTag.contains("Fluid", net.minecraft.nbt.Tag.TAG_STRING)) {
                fluidId = tankTag.getString("Fluid");
            } else if (tankTag.contains("Fluid", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag fluidTag = tankTag.getCompound("Fluid");
                if (fluidTag.contains("Name")) {
                    fluidId = fluidTag.getString("Name");
                } else if (fluidTag.contains("id")) {
                    fluidId = fluidTag.getString("id");
                }
            }
        }
        if (fluidId.isEmpty() || amount <= 0) {
            return Component.translatable("tooltip.chickens.avian_fluid_converter.empty");
        }
        ResourceLocation id = ResourceLocation.tryParse(fluidId);
        if (id == null) {
            return Component.literal(fluidId);
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Component.literal(id.toString());
        }
        return new FluidStack(fluid, amount).getDisplayName();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
            return converter.getComparatorOutput();
        }
        return super.getAnalogOutputSignal(state, level, pos);
    }
}
