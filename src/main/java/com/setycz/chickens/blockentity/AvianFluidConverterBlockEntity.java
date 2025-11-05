package com.setycz.chickens.blockentity;

import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.block.AvianFluidConverterBlock;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.LiquidEggItem;
import com.setycz.chickens.menu.AvianFluidConverterMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

/**
 * Fluid-driven counterpart to the Avian Flux Converter. The machine cracks
 * liquid eggs, fills an internal {@link FluidTank}, and pushes contents into
 * adjacent handlers each tick so automation can hook directly into the stored
 * fluids.
 */
public class AvianFluidConverterBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 1;
    private static final int[] ACCESSIBLE_SLOTS = new int[] { 0 };
    private static final int DEFAULT_TANK_CAPACITY = FluidType.BUCKET_VOLUME * 8;
    private static final int DEFAULT_TRANSFER_RATE = FluidType.BUCKET_VOLUME * 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidTank tank = new FluidTank(DEFAULT_TANK_CAPACITY, stack -> {
        if (stack.isEmpty()) {
            return false;
        }
        FluidStack current = getFluid();
        return current.isEmpty() || current.isFluidEqual(stack);
    }) {
        @Override
        protected void onContentsChanged() {
            markFluidDirty();
        }
    };

    private int transferRate = DEFAULT_TRANSFER_RATE;
    private boolean cachedActiveState = false;
    @Nullable
    private Component customName;

    public AvianFluidConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AVIAN_FLUID_CONVERTER.get(), pos, state);
        if (state.hasProperty(AvianFluidConverterBlock.LIT)) {
            cachedActiveState = state.getValue(AvianFluidConverterBlock.LIT);
        }
        syncWithConfig(true);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker() {
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof AvianFluidConverterBlockEntity converter) {
                converter.tickServer(level);
            }
        };
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }
        boolean drained = drainLiquidEgg();
        boolean exported = false;
        int before = tank.getFluidAmount();
        if (!tank.isEmpty()) {
            pushFluidToNeighbors(level);
            exported = tank.getFluidAmount() != before;
        }
        boolean shouldGlow = shouldBlockGlow(drained || exported);
        updateActiveState(level, shouldGlow);
        if (drained || exported) {
            setChanged();
        }
    }

    private boolean drainLiquidEgg() {
        ItemStack stack = items.get(0);
        if (!isLiquidEgg(stack)) {
            return false;
        }
        LiquidEggRegistryItem entry = LiquidEggRegistry.findById(ChickenItemHelper.getChickenType(stack));
        if (entry == null) {
            items.set(0, ItemStack.EMPTY);
            return true;
        }
        FluidStack payload = entry.createFluidStack();
        if (payload.isEmpty()) {
            items.set(0, ItemStack.EMPTY);
            return true;
        }
        if (!tank.getFluid().isEmpty() && !tank.getFluid().isFluidEqual(payload)) {
            return false;
        }
        int accepted = tank.fill(payload, IFluidHandler.FluidAction.SIMULATE);
        if (accepted < payload.getAmount()) {
            return false;
        }
        tank.fill(payload, IFluidHandler.FluidAction.EXECUTE);
        items.set(0, ItemStack.EMPTY);
        return true;
    }

    private void pushFluidToNeighbors(Level level) {
        for (Direction direction : Direction.values()) {
            if (tank.isEmpty()) {
                return;
            }
            FluidStack toDrain = tank.drain(Math.min(transferRate, tank.getFluidAmount()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (toDrain.isEmpty()) {
                return;
            }
            var target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(direction),
                    direction.getOpposite());
            if (target == null) {
                continue;
            }
            int accepted = target.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private void markFluidDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    public int getFluidAmount() {
        return tank.getFluidAmount();
    }

    public int getTankCapacity() {
        return tank.getCapacity();
    }

    public int getComparatorOutput() {
        int capacity = Math.max(1, getTankCapacity());
        return Math.round(15.0F * getFluidAmount() / (float) capacity);
    }

    public FluidTank getFluidTank(@Nullable Direction direction) {
        return tank;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, index, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = ContainerHelper.takeItem(items, index);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return isLiquidEgg(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("menu.chickens.avian_fluid_converter");
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(Component name) {
        customName = name;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AvianFluidConverterMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        CompoundTag tankTag = tank.writeToNBT(provider, new CompoundTag());
        tankTag.putInt("Capacity", tank.getCapacity());
        tag.put("Tank", tankTag);
        tag.putInt("TransferRate", transferRate);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result().ifPresent(component -> tag.put("CustomName", component));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            CompoundTag tankTag = tag.getCompound("Tank");
            tank.readFromNBT(provider, tankTag);
            if (tankTag.contains("Capacity")) {
                int storedCapacity = Math.max(FluidType.BUCKET_VOLUME, tankTag.getInt("Capacity"));
                tank.setCapacity(storedCapacity);
            }
        } else {
            tank.setFluid(FluidStack.EMPTY);
        }
        transferRate = Math.max(0, tag.getInt("TransferRate"));
        if (tag.contains("CustomName", Tag.TAG_COMPOUND)) {
            ComponentSerialization.CODEC
                    .parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("CustomName"))
                    .result().ifPresent(component -> customName = component);
        } else {
            customName = null;
        }
        syncWithConfig(false);
        cachedActiveState = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncWithConfig(false);
        if (level != null && !level.isClientSide) {
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    private void syncWithConfig(boolean overwriteCapacity) {
        ChickensConfigValues config = ChickensConfigHolder.get();
        int configuredCapacity = Math.max(FluidType.BUCKET_VOLUME, config.getAvianFluidConverterCapacity(DEFAULT_TANK_CAPACITY));
        int configuredTransfer = Math.max(FluidType.BUCKET_VOLUME, config.getAvianFluidConverterTransfer(DEFAULT_TRANSFER_RATE));
        transferRate = configuredTransfer;
        int clampedCapacity = overwriteCapacity
                ? configuredCapacity
                : Mth.clamp(tank.getCapacity(), FluidType.BUCKET_VOLUME, configuredCapacity);
        if (tank.getCapacity() != clampedCapacity) {
            tank.setCapacity(clampedCapacity);
            if (tank.getFluidAmount() > clampedCapacity) {
                tank.drain(tank.getFluidAmount() - clampedCapacity, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private boolean shouldBlockGlow(boolean drainedThisTick) {
        ItemStack stack = items.get(0);
        boolean hasEggReserves = isLiquidEgg(stack) && tank.getFluidAmount() < tank.getCapacity();
        if (!ChickensConfigHolder.get().isAvianFluidConverterEffectsEnabled()) {
            return false;
        }
        return drainedThisTick || hasEggReserves || !tank.isEmpty();
    }

    private void updateActiveState(Level level, boolean active) {
        if (cachedActiveState == active) {
            return;
        }
        cachedActiveState = active;
        BlockState state = getBlockState();
        if (!state.hasProperty(AvianFluidConverterBlock.LIT)) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(AvianFluidConverterBlock.LIT, active), Block.UPDATE_CLIENTS);
    }

    private static boolean isLiquidEgg(ItemStack stack) {
        return stack.getItem() instanceof LiquidEggItem;
    }
}
