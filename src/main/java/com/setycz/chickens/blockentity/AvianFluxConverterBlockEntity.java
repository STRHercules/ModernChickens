package com.setycz.chickens.blockentity;

import com.setycz.chickens.item.FluxEggItem;
import com.setycz.chickens.menu.AvianFluxConverterMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;

/**
 * Single-slot machine that siphons RF from Flux Eggs into an internal battery.
 * The block entity exposes sided inventory access for automation mods and
 * synchronises its energy buffer to the menu so the GUI can render live
 * progress bars without repeatedly probing the storage backend.
 */
public class AvianFluxConverterBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 1;
    private static final int[] ACCESSIBLE_SLOTS = new int[] { 0 };
    private static final int CAPACITY = 500_000;
    private static final int MAX_TRANSFER = 4_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            // Split the 32-bit energy/capacity pair across four shorts so vanilla's
            // container sync (which writes values as unsigned shorts) can ship the
            // full Redstone Flux totals to clients without truncating high bits.
            return switch (index) {
            case 0 -> energy & 0xFFFF;
            case 1 -> (energy >>> 16) & 0xFFFF;
            case 2 -> capacity & 0xFFFF;
            case 3 -> (capacity >>> 16) & 0xFFFF;
            default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            int masked = value & 0xFFFF;
            switch (index) {
            case 0 -> energy = clampEnergy((energy & 0xFFFF0000) | masked);
            case 1 -> energy = clampEnergy((energy & 0x0000FFFF) | (masked << 16));
            case 2 -> {
                capacity = Math.max(1, (capacity & 0xFFFF0000) | masked);
                energy = Math.min(energy, capacity);
            }
            case 3 -> {
                capacity = Math.max(1, (capacity & 0x0000FFFF) | (masked << 16));
                energy = Math.min(energy, capacity);
            }
            default -> {
            }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int space = capacity - energy;
            if (space <= 0) {
                return 0;
            }
            int accepted = Math.min(MAX_TRANSFER, Math.min(space, maxReceive));
            if (accepted <= 0) {
                return 0;
            }
            if (!simulate) {
                energy += accepted;
                markEnergyDirty();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int available = Math.min(MAX_TRANSFER, Math.min(energy, maxExtract));
            if (available <= 0) {
                return 0;
            }
            if (!simulate) {
                energy -= available;
                markEnergyDirty();
            }
            return available;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }
    };

    private int energy = 0;
    private int capacity = CAPACITY;
    @Nullable
    private Component customName;

    public AvianFluxConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AVIAN_FLUX_CONVERTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AvianFluxConverterBlockEntity converter) {
        converter.tickServer(level);
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }
        ItemStack stack = items.get(0);
        if (!isFluxEgg(stack)) {
            return;
        }
        int stored = FluxEggItem.getStoredEnergy(stack);
        if (stored <= 0 || energy >= capacity) {
            return;
        }
        int transferred = energyStorage.receiveEnergy(stored, false);
        if (transferred <= 0) {
            return;
        }
        int remaining = stored - transferred;
        FluxEggItem.setStoredEnergy(stack, remaining);
        if (remaining <= 0) {
            // Remove the depleted shell once its Redstone Flux payload is exhausted.
            items.set(0, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void markEnergyDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private int clampEnergy(int value) {
        return Mth.clamp(value, 0, capacity);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getComparatorOutput() {
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(15.0F * energy / (float) capacity);
    }

    public EnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
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
        return isFluxEgg(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
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
        return customName != null ? customName : Component.translatable("menu.chickens.avian_flux_converter");
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
        return new AvianFluxConverterMenu(id, playerInventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Energy", energy);
        tag.putInt("Capacity", capacity);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE),
                    customName).result().ifPresent(component -> tag.put("CustomName", component));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        if (tag.contains("Capacity")) {
            capacity = Math.max(1, tag.getInt("Capacity"));
        } else {
            capacity = CAPACITY;
        }
        energy = Mth.clamp(tag.getInt("Energy"), 0, capacity);
        if (tag.contains("CustomName", Tag.TAG_COMPOUND)) {
            ComponentSerialization.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE),
                    tag.getCompound("CustomName")).result().ifPresent(component -> customName = component);
        } else {
            customName = null;
        }
    }

    private static boolean isFluxEgg(ItemStack stack) {
        return stack.getItem() instanceof FluxEggItem;
    }
}
