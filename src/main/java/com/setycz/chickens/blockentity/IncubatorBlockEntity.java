package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.block.IncubatorBlock;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.menu.IncubatorMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import com.setycz.chickens.registry.ModRegistry;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Block entity backing the Incubator machine. Tracks a two-slot inventory, a
 * small RF buffer, and incubation progress so automation mods can interact
 * with the machine using vanilla container and NeoForge energy capabilities.
 */
public class IncubatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 2;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int[] TOP_SLOTS = new int[] { INPUT_SLOT };
    private static final int[] SIDE_SLOTS = new int[] { INPUT_SLOT };
    private static final int[] BOTTOM_SLOTS = new int[] { OUTPUT_SLOT };
    private static final int DEFAULT_ENERGY_CAPACITY = 100_000;
    private static final int DEFAULT_ENERGY_MAX_RECEIVE = 4_000;
    public static final int MAX_PROGRESS = 200;
    private static final int DEFAULT_ENERGY_COST = 10_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int capacity = DEFAULT_ENERGY_CAPACITY;
    private int maxReceive = DEFAULT_ENERGY_MAX_RECEIVE;
    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage();
    private int progress;
    private int energyReserved;
    private int cachedEnergyCost = DEFAULT_ENERGY_COST;
    private boolean cachedActiveState;
    @Nullable
    private Component customName;

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR.get(), pos, state);
        syncWithConfig(true);
        refreshEnergyCost();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity incubator) {
        incubator.tickServer(level);
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }
        refreshEnergyCost();
        boolean changed = false;
        boolean pulledEnergy = pullEnergyFromNeighbors(level);
        if (!canOperate()) {
            changed |= resetProgress();
            updateActiveState(level, false);
            if (pulledEnergy || changed) {
                setChanged();
            }
            return;
        }

        boolean advanced = false;
        int energyCost = Math.max(1, cachedEnergyCost);
        if (energyReserved < energyCost) {
            int perTick = Math.max(1, energyCost / MAX_PROGRESS);
            int needed = Math.min(perTick, energyCost - energyReserved);
            if (energyStorage.getEnergyStored() >= needed) {
                energyStorage.extractEnergy(needed, false);
                energyReserved += needed;
                advanced = true;
            }
        } else {
            advanced = true;
        }

        if (advanced) {
            progress = Math.min(MAX_PROGRESS, progress + 1);
            changed = true;
            if (progress >= MAX_PROGRESS && energyReserved >= energyCost) {
                if (craftOutput()) {
                    progress = 0;
                    energyReserved = 0;
                } else {
                    refundReservedEnergy();
                    progress = 0;
                }
                changed = true;
            }
        }

        updateActiveState(level, progress > 0);
        if (changed || pulledEnergy) {
            setChanged();
        }
    }

    private boolean resetProgress() {
        boolean updated = false;
        if (progress > 0) {
            progress = Math.max(progress - 2, 0);
            updated = true;
        }
        if (energyReserved > 0) {
            refundReservedEnergy();
            updated = true;
        }
        return updated;
    }

    private void refundReservedEnergy() {
        if (energyReserved <= 0) {
            return;
        }
        energyStorage.receiveEnergy(energyReserved, false);
        energyReserved = 0;
    }

    private boolean craftOutput() {
        ItemStack input = items.get(INPUT_SLOT);
        if (!isEgg(input)) {
            return false;
        }
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(input);
        if (chicken == null) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        ItemStack result = ModRegistry.CHICKEN_ITEM.get().createFor(chicken);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else if (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        } else {
            return false;
        }
        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        return true;
    }

    private boolean canOperate() {
        ItemStack input = items.get(INPUT_SLOT);
        if (!isEgg(input)) {
            return false;
        }
        ChickensRegistryItem chicken = ChickenItemHelper.resolve(input);
        if (chicken == null) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        ItemStack expected = ModRegistry.CHICKEN_ITEM.get().createFor(chicken);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, expected)) {
            return false;
        }
        return output.getCount() < output.getMaxStackSize();
    }

    private static boolean isEgg(ItemStack stack) {
        return stack.getItem() instanceof ChickensSpawnEggItem;
    }

    private void refreshEnergyCost() {
        int configured = Math.max(1, ChickensConfigHolder.get().getIncubatorEnergyCost());
        if (configured != cachedEnergyCost) {
            cachedEnergyCost = configured;
            if (energyReserved > cachedEnergyCost) {
                refundReservedEnergy();
            }
        }
    }

    private void updateActiveState(Level level, boolean active) {
        if (cachedActiveState == active) {
            return;
        }
        cachedActiveState = active;
        BlockState state = getBlockState();
        if (!state.hasProperty(IncubatorBlock.LIT)) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(IncubatorBlock.LIT, active), Block.UPDATE_CLIENTS);
    }

    private void markEnergyDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private boolean pullEnergyFromNeighbors(Level level) {
        if (energyStorage.getEnergyStored() >= capacity || maxReceive <= 0) {
            return false;
        }
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (energyStorage.getEnergyStored() >= capacity) {
                break;
            }
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            int space = Math.min(maxReceive, capacity - energyStorage.getEnergyStored());
            if (space <= 0) {
                break;
            }
            int available = neighbor.extractEnergy(space, true);
            if (available <= 0) {
                continue;
            }
            int accepted = energyStorage.receiveEnergy(available, true);
            if (accepted <= 0) {
                continue;
            }
            int drained = neighbor.extractEnergy(accepted, false);
            if (drained <= 0) {
                continue;
            }
            energyStorage.receiveEnergy(drained, false);
            changed = true;
        }
        return changed;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return capacity;
    }

    public int getProgress() {
        return progress;
    }

    public int getEnergyCost() {
        return cachedEnergyCost;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public int getComparatorOutput() {
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(15.0F * energyStorage.getEnergyStored() / (float) capacity);
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
        if (stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            updateActiveState(level, progress > 0);
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
        return index == INPUT_SLOT && isEgg(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == OUTPUT_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        if (side == Direction.UP) {
            return TOP_SLOTS;
        }
        return SIDE_SLOTS;
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
        return 64;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("container.chickens.incubator");
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
        return new IncubatorMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("ReservedEnergy", energyReserved);
        tag.putInt("Progress", progress);
        tag.putInt("EnergyCost", cachedEnergyCost);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result().ifPresent(component -> tag.put("CustomName", component));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        int storedEnergy = tag.getInt("Energy");
        int storedReserved = tag.getInt("ReservedEnergy");
        progress = Math.min(tag.getInt("Progress"), MAX_PROGRESS);
        if (tag.contains("EnergyCost")) {
            cachedEnergyCost = Math.max(1, tag.getInt("EnergyCost"));
        } else {
            cachedEnergyCost = DEFAULT_ENERGY_COST;
        }
        syncWithConfig(false);
        energyStorage.setEnergy(Mth.clamp(storedEnergy, 0, capacity));
        energyReserved = Math.min(storedReserved, capacity);
        if (tag.contains("CustomName", Tag.TAG_COMPOUND)) {
            ComponentSerialization.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE),
                    tag.getCompound("CustomName")).result().ifPresent(name -> customName = name);
        } else {
            customName = null;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshEnergyCost();
        syncWithConfig(false);
    }

    private void syncWithConfig(boolean overwriteCapacity) {
        var config = ChickensConfigHolder.get();
        int configuredCapacity = Math.max(1, config.getIncubatorEnergyCapacity());
        int configuredReceive = Math.max(1, config.getIncubatorEnergyMaxReceive());
        if (overwriteCapacity) {
            capacity = configuredCapacity;
        } else {
            capacity = Math.max(1, Math.min(capacity, configuredCapacity));
        }
        maxReceive = configuredReceive;
        energyStorage.setLimits(capacity, maxReceive);
        if (energyReserved > capacity) {
            energyReserved = capacity;
        }
    }

    private final class MachineEnergyStorage extends EnergyStorage {
        MachineEnergyStorage() {
            super(DEFAULT_ENERGY_CAPACITY, DEFAULT_ENERGY_MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) {
                markEnergyDirty();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate);
            if (extracted > 0 && !simulate) {
                markEnergyDirty();
            }
            return extracted;
        }

        void setEnergy(int energy) {
            this.energy = Mth.clamp(energy, 0, getMaxEnergyStored());
        }

        void setLimits(int capacity, int maxReceive) {
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            if (this.energy > capacity) {
                this.energy = capacity;
            }
        }
    }
}
