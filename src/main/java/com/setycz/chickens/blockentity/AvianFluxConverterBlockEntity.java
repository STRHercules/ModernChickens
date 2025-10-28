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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

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
    private static final int CAPACITY = 50_000;
    private static final int MAX_TRANSFER = 4_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int space = capacity - AvianFluxConverterBlockEntity.this.energy;
            if (space <= 0) {
                return 0;
            }
            int accepted = Math.min(MAX_TRANSFER, Math.min(space, maxReceive));
            if (accepted <= 0) {
                return 0;
            }
            if (!simulate) {
                AvianFluxConverterBlockEntity.this.energy += accepted;
                markEnergyDirty();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int available = Math.min(MAX_TRANSFER,
                    Math.min(AvianFluxConverterBlockEntity.this.energy, maxExtract));
            if (available <= 0) {
                return 0;
            }
            if (!simulate) {
                AvianFluxConverterBlockEntity.this.energy -= available;
                markEnergyDirty();
            }
            return available;
        }

        @Override
        public int getEnergyStored() {
            return AvianFluxConverterBlockEntity.this.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return AvianFluxConverterBlockEntity.this.capacity;
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
        boolean drainedEgg = drainFluxEgg();
        if (energy > 0) {
            pushEnergyToNeighbors(level);
        }
        if (drainedEgg) {
            setChanged();
        }
    }

    private void markEnergyDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    // Pulls Redstone Flux out of the inserted egg, returning whether the stack was
    // mutated so the caller can refresh container state when needed.
    private boolean drainFluxEgg() {
        ItemStack stack = items.get(0);
        if (!isFluxEgg(stack)) {
            return false;
        }
        int stored = FluxEggItem.getStoredEnergy(stack);
        if (stored <= 0 || energy >= capacity) {
            return false;
        }
        int transferred = energyStorage.receiveEnergy(stored, false);
        if (transferred <= 0) {
            return false;
        }
        int remaining = stored - transferred;
        FluxEggItem.setStoredEnergy(stack, remaining);
        emitFluxTransferParticles(transferred);
        if (remaining <= 0) {
            // Remove the depleted shell once its Redstone Flux payload is exhausted.
            items.set(0, ItemStack.EMPTY);
        }
        return true;
    }

    // Kicks a subtle burst of red dust into the air whenever the converter absorbs
    // power from a Flux Egg so players spot the transfer without relying on the GUI.
    private void emitFluxTransferParticles(int transferred) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int bursts = Mth.clamp(transferred / 2000 + 1, 1, 5);
        double baseX = worldPosition.getX() + 0.5D;
        double baseY = worldPosition.getY() + 0.6D;
        double baseZ = worldPosition.getZ() + 0.5D;
        for (int i = 0; i < bursts; i++) {
            double offsetX = (serverLevel.getRandom().nextDouble() - 0.5D) * 0.3D;
            double offsetZ = (serverLevel.getRandom().nextDouble() - 0.5D) * 0.3D;
            double yMotion = 0.01D + serverLevel.getRandom().nextDouble() * 0.02D;
            serverLevel.sendParticles(DustParticleOptions.REDSTONE, baseX + offsetX, baseY,
                    baseZ + offsetZ, 1, 0.0D, yMotion, 0.0D, 0.0D);
        }
    }

    // Attempts to hand off power to every adjacent block entity so automation
    // mods can siphon the converter's charge with standard pipes.
    private void pushEnergyToNeighbors(Level level) {
        for (Direction direction : Direction.values()) {
            if (energy <= 0) {
                return;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos,
                    direction.getOpposite());
            if (target == null) {
                continue;
            }
            int available = Math.min(MAX_TRANSFER, energy);
            if (available <= 0) {
                continue;
            }
            int accepted = target.receiveEnergy(available, false);
            if (accepted <= 0) {
                continue;
            }
            // Mirror the transfer into our internal storage so comparator outputs and
            // GUI sync reflect the exported RF total immediately.
            energyStorage.extractEnergy(accepted, false);
        }
    }

    public NonNullList<ItemStack> getItems() {
        return items;
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

    /**
     * Exposes the current RF stored so menus can mirror the exact value over the
     * vanilla {@link net.minecraft.world.inventory.DataSlot} syncing system.
     */
    public int getEnergyStored() {
        return energy;
    }

    /**
     * Reports the converter's configured RF ceiling for GUI widgets and other
     * client-side displays that need to scale values against the full buffer.
     */
    public int getEnergyCapacity() {
        return capacity;
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
        return new AvianFluxConverterMenu(id, playerInventory, this);
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
            // Clamp legacy saves down to the modern 50k ceiling so the GUI and
            // battery math stay in sync with the new balance pass.
            capacity = Math.min(CAPACITY, Math.max(1, tag.getInt("Capacity")));
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
