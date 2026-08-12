package strhercules.chickens.blockentity;

import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.block.MechanicalRoostBlock;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.menu.MechanicalRoostMenu;
import strhercules.chickens.registry.ModBlockEntities;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RF-powered roost with four chicken inputs and four output rows per input.
 * Completed output that does not fit in a row's visible slots is retained in
 * that row's persisted internal buffer.
 */
public class MechanicalRoostBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int CHICKEN_SLOT_COUNT = 4;
    public static final int OUTPUT_SLOT_COUNT = 16;
    public static final int INVENTORY_SIZE = CHICKEN_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    public static final int SPEED_UPGRADE_SLOT = 0;
    public static final int STACK_UPGRADE_SLOT = 1;
    public static final int UPGRADE_SLOT_COUNT = 2;

    private static final int MAX_CHICKENS = 16;
    private static final int MAX_MECHANICAL_STACK_UPGRADES = 3;
    private static final int MAX_OUTPUT_COUNT_PER_CYCLE = 64;
    private static final int DEFAULT_ENERGY_CAPACITY = 1_000_000;
    private static final int DEFAULT_ENERGY_MAX_RECEIVE = 4_000;
    private static final double DEFAULT_SPEED_MULTIPLIER = 2.0D;

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage();
    private final ChickenContainerEntry[] rowChickenData = new ChickenContainerEntry[CHICKEN_SLOT_COUNT];
    private final int[] rowTimeUntilNextDrop = new int[CHICKEN_SLOT_COUNT];
    private final int[] rowTimeElapsed = new int[CHICKEN_SLOT_COUNT];
    private final int[] rowProgress = new int[CHICKEN_SLOT_COUNT];
    private final int[] rowRawLayTime = new int[CHICKEN_SLOT_COUNT];
    private final double[] rowAppliedSpeedMultiplier = new double[CHICKEN_SLOT_COUNT];
    private final List<List<ItemStack>> rowPendingOutputs = new ArrayList<>(CHICKEN_SLOT_COUNT);
    private final ContainerData rowData = new ContainerData() {
        @Override
        public int get(int index) {
            return index >= 0 && index < CHICKEN_SLOT_COUNT ? rowProgress[index] : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < CHICKEN_SLOT_COUNT) {
                rowProgress[index] = Mth.clamp(value, 0, 1000);
            }
        }

        @Override
        public int getCount() {
            return CHICKEN_SLOT_COUNT;
        }
    };
    private int capacity = DEFAULT_ENERGY_CAPACITY;
    private int maxReceive = DEFAULT_ENERGY_MAX_RECEIVE;
    private boolean cachedActiveState;
    private boolean rowTimersLoaded;

    public MechanicalRoostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MECHANICAL_ROOST.get(), pos, state,
                INVENTORY_SIZE, CHICKEN_SLOT_COUNT, UPGRADE_SLOT_COUNT);
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            rowPendingOutputs.add(new ArrayList<>());
        }
        syncWithConfig(true);
    }

    @Override
    protected void runServerTick(Level level) {
        if (level.isClientSide) {
            return;
        }
        syncWithConfig(false);
        pullEnergyFromNeighbors(level);
        refreshRowData(level);
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            flushRowPendingOutput(row);
            tickRow(level, row);
        }
        rowTimersLoaded = false;
        updateActiveState(level, getProgress() > 0 && getEnergyStored() > 0);
    }

    @Override
    protected boolean spawnChickenItem(RandomSource random) {
        // The base hook represents one shared cycle; Mechanical Roost uses the
        // row-specific tick loop below instead.
        return false;
    }

    private void refreshRowData(Level level) {
        boolean changed = false;
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            ChickenContainerEntry entry = createChickenData(row, getItem(row));
            if (Objects.equals(rowChickenData[row], entry)) {
                continue;
            }
            rowChickenData[row] = entry;
            if (entry == null) {
                clearRowTimer(row);
            } else if (!rowTimersLoaded || rowTimeUntilNextDrop[row] <= 0) {
                resetRowTimer(level, row);
            } else {
                if (rowRawLayTime[row] <= 0) {
                    rowRawLayTime[row] = rowTimeUntilNextDrop[row];
                    rowAppliedSpeedMultiplier[row] = rowSpeedMultiplier(entry);
                }
                rescaleRowTimerIfNeeded(row, entry);
                updateRowProgress(row);
            }
            changed = true;
        }
        if (changed) {
            setChanged();
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private void tickRow(Level level, int row) {
        ChickenContainerEntry entry = rowChickenData[row];
        if (entry == null) {
            clearRowTimer(row);
            return;
        }
        if (rowTimeUntilNextDrop[row] <= 0) {
            resetRowTimer(level, row);
        }
        rescaleRowTimerIfNeeded(row, entry);
        if (energyStorage.getEnergyStored() <= 0) {
            updateRowProgress(row);
            return;
        }
        int increment = entry.getAddedTime(getItem(row));
        if (increment > 0) {
            rowTimeElapsed[row] = Math.min(Integer.MAX_VALUE,
                    rowTimeElapsed[row] + increment);
        }
        if (rowTimeElapsed[row] >= rowTimeUntilNextDrop[row]
                && spawnRowItem(row, entry, level.random)) {
            resetRowTimer(level, row);
        }
        updateRowProgress(row);
    }

    private boolean spawnRowItem(int row, ChickenContainerEntry entry, RandomSource random) {
        ItemStack output = entry.createLay(random, ChickensConfigHolder.get().isScalingDropsEnabled());
        if (output.isEmpty()) {
            return false;
        }
        int energyCost = (int) Math.min(Integer.MAX_VALUE,
                (long) getEnergyCostPerEgg() * output.getCount());
        if (!energyStorage.consumeEnergy(energyCost)) {
            return false;
        }
        queueRowOutput(row, output);
        return true;
    }

    private void queueRowOutput(int row, ItemStack output) {
        ItemStack remaining = insertIntoRowOutputs(row, output);
        if (remaining.isEmpty()) {
            return;
        }
        List<ItemStack> pending = rowPendingOutputs.get(row);
        if (!pending.isEmpty()) {
            ItemStack last = pending.get(pending.size() - 1);
            if (ItemStack.isSameItemSameComponents(last, remaining)) {
                last.grow(remaining.getCount());
                setChanged();
                return;
            }
        }
        pending.add(remaining);
        setChanged();
    }

    private ItemStack insertIntoRowOutputs(int row, ItemStack output) {
        ItemStack remaining = output.copy();
        int start = getOutputSlotIndex() + row * 4;
        for (int slot = start; slot < start + 4 && !remaining.isEmpty(); slot++) {
            ItemStack existing = getItem(slot);
            int maxStackSize = getMaxStackSizeForSlot(slot, remaining);
            if (existing.isEmpty()) {
                int amount = Math.min(maxStackSize, remaining.getCount());
                setItem(slot, remaining.copyWithCount(amount));
                remaining.shrink(amount);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int amount = Math.min(Math.max(getMaxStackSizeForSlot(slot, existing) - existing.getCount(), 0),
                        remaining.getCount());
                existing.grow(amount);
                remaining.shrink(amount);
                setChanged();
            }
        }
        return remaining;
    }

    private void flushRowPendingOutput(int row) {
        List<ItemStack> pending = rowPendingOutputs.get(row);
        for (int index = 0; index < pending.size();) {
            ItemStack remaining = insertIntoRowOutputs(row, pending.get(index));
            if (remaining.isEmpty()) {
                pending.remove(index);
                setChanged();
                continue;
            }
            pending.set(index, remaining);
            break;
        }
    }

    private void resetRowTimer(Level level, int row) {
        ChickenContainerEntry entry = rowChickenData[row];
        if (entry == null) {
            clearRowTimer(row);
            return;
        }
        int rawTarget = entry.getLayTime(level.random);
        rowRawLayTime[row] = rawTarget;
        rowAppliedSpeedMultiplier[row] = rowSpeedMultiplier(entry);
        rowTimeUntilNextDrop[row] = scaleLayTime(rawTarget, rowAppliedSpeedMultiplier[row]);
        rowTimeElapsed[row] = 0;
        rowProgress[row] = 0;
        setChanged();
    }

    private void clearRowTimer(int row) {
        rowTimeUntilNextDrop[row] = 0;
        rowTimeElapsed[row] = 0;
        rowProgress[row] = 0;
        rowRawLayTime[row] = 0;
        rowAppliedSpeedMultiplier[row] = 0.0D;
    }

    private void rescaleRowTimerIfNeeded(int row, ChickenContainerEntry entry) {
        double newMultiplier = rowSpeedMultiplier(entry);
        if (rowRawLayTime[row] <= 0) {
            rowRawLayTime[row] = Math.max(rowTimeUntilNextDrop[row], 1);
        }
        if (rowAppliedSpeedMultiplier[row] <= 0.0D) {
            rowAppliedSpeedMultiplier[row] = newMultiplier;
            rescaleRowTimer(row, newMultiplier);
            return;
        }
        if (Math.abs(rowAppliedSpeedMultiplier[row] - newMultiplier) > 0.000001D) {
            rowAppliedSpeedMultiplier[row] = newMultiplier;
            rescaleRowTimer(row, newMultiplier);
        }
    }

    private void rescaleRowTimer(int row, double multiplier) {
        int oldTarget = Math.max(rowTimeUntilNextDrop[row], 1);
        int newTarget = scaleLayTime(rowRawLayTime[row], multiplier);
        rowTimeUntilNextDrop[row] = newTarget;
        rowTimeElapsed[row] = (int) Math.min(newTarget,
                (long) rowTimeElapsed[row] * newTarget / oldTarget);
        updateRowProgress(row);
        setChanged();
    }

    private static int scaleLayTime(int rawTarget, double multiplier) {
        return Math.max(1, (int) (Math.max(rawTarget, 1) / Math.max(multiplier, 0.0001D)));
    }

    private void updateRowProgress(int row) {
        int newProgress = rowTimeUntilNextDrop[row] <= 0 ? 0
                : Math.min(1000, (int) ((long) rowTimeElapsed[row] * 1000L / rowTimeUntilNextDrop[row]));
        if (newProgress != rowProgress[row]) {
            rowProgress[row] = newProgress;
            setChanged();
        }
    }

    @Override
    protected int requiredSeedsForDrop() {
        return 0;
    }

    @Override
    protected double speedMultiplier() {
        double multiplier = ChickensConfigHolder.get().getRoostSpeedMultiplier()
                * DEFAULT_SPEED_MULTIPLIER
                * (1.0D + 0.2D * getUpgradeCount(SPEED_UPGRADE_SLOT));
        double chickenCoefficient = Double.POSITIVE_INFINITY;
        for (int slot = 0; slot < CHICKEN_SLOT_COUNT; slot++) {
            ChickenContainerEntry entry = rowChickenData[slot];
            if (entry != null) {
                chickenCoefficient = Math.min(chickenCoefficient, entry.chicken().getLayCoefficient());
            }
        }
        if (Double.isFinite(chickenCoefficient)) {
            multiplier *= Math.max(chickenCoefficient, 0.0D);
        }

        int auraRange = ChickensConfigHolder.get().getRoosterAuraRange();
        double auraMultiplier = ChickensConfigHolder.get().getRoosterAuraMultiplier();
        if (level != null && auraRange > 0 && auraMultiplier > 1.0D) {
            int roosters = countActiveRoostersInNests(level, worldPosition, auraRange);
            multiplier *= Math.max(1.0D + roosters * (auraMultiplier - 1.0D), 0.0D);
        }
        return multiplier;
    }

    private double rowSpeedMultiplier(ChickenContainerEntry entry) {
        double multiplier = ChickensConfigHolder.get().getRoostSpeedMultiplier()
                * DEFAULT_SPEED_MULTIPLIER
                * (1.0D + 0.2D * getUpgradeCount(SPEED_UPGRADE_SLOT))
                * Math.max(entry.chicken().getLayCoefficient(), 0.0D);
        int auraRange = ChickensConfigHolder.get().getRoosterAuraRange();
        double auraMultiplier = ChickensConfigHolder.get().getRoosterAuraMultiplier();
        if (level != null && auraRange > 0 && auraMultiplier > 1.0D) {
            int roosters = countActiveRoostersInNests(level, worldPosition, auraRange);
            multiplier *= Math.max(1.0D + roosters * (auraMultiplier - 1.0D), 0.0D);
        }
        return multiplier;
    }

    private static int countActiveRoostersInNests(Level level, BlockPos origin, int range) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int total = 0;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity instanceof NestBlockEntity nest && nest.hasActiveAura()) {
                        total += Math.max(0, nest.getRoosterCount());
                    }
                }
            }
        }
        return total;
    }

    @Override
    protected int getChickenSlotCount() {
        return CHICKEN_SLOT_COUNT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.chickens.mechanical_roost");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory, net.minecraft.world.inventory.ContainerData dataAccess) {
        return new MechanicalRoostMenu(id, playerInventory, this, getDataAccess());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MechanicalRoostMenu(id, playerInventory, this);
    }

    @Override
    public ContainerData getDataAccess() {
        return rowData;
    }

    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        if (slot < 0 || slot >= CHICKEN_SLOT_COUNT || !ChickenItemHelper.isChicken(stack)) {
            return null;
        }
        ChickensRegistryItem description = ChickenItemHelper.resolve(stack);
        if (description == null) {
            return null;
        }
        return new ChickenContainerEntry(description, ChickenItemHelper.getStats(stack));
    }

    @Override
    public RenderData getRenderData(int slot) {
        if (slot < 0 || slot >= CHICKEN_SLOT_COUNT) {
            return null;
        }
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return null;
        }
        ChickenContainerEntry current = createChickenData(slot, stack);
        if (current == null) {
            return null;
        }
        if (!Objects.equals(rowChickenData[slot], current)) {
            rowChickenData[slot] = current;
        }
        return new RenderData(current.chicken(), current.stats(), stack.getCount());
    }

    @Override
    public boolean hasRequiredChickens() {
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            if (createChickenData(row, getItem(row)) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRequiredSeeds() {
        return true;
    }

    @Override
    public int getProgress() {
        int maximum = 0;
        for (int progressValue : rowProgress) {
            maximum = Math.max(maximum, progressValue);
        }
        return maximum;
    }

    public int getProgress(int row) {
        return row >= 0 && row < CHICKEN_SLOT_COUNT ? rowProgress[row] : 0;
    }

    @Override
    public double getProgressFraction() {
        return getProgress() / 1000.0D;
    }

    @Override
    public int getTotalLayTimeTicks() {
        int row = getStatusRow();
        return row < 0 ? 0 : rowTimeUntilNextDrop[row];
    }

    @Override
    public int getRemainingLayTimeTicks() {
        int row = getStatusRow();
        return row < 0 ? 0 : Math.max(rowTimeUntilNextDrop[row] - rowTimeElapsed[row], 0);
    }

    @Override
    public int getProgressIncrementPerTick() {
        int row = getStatusRow();
        if (row < 0 || getEnergyStored() <= 0) {
            return 0;
        }
        return Math.max(rowChickenData[row].getAddedTime(getItem(row)), 0);
    }

    private int getStatusRow() {
        int selected = -1;
        int shortest = Integer.MAX_VALUE;
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            if (rowChickenData[row] == null || rowTimeUntilNextDrop[row] <= 0) {
                continue;
            }
            int remaining = Math.max(rowTimeUntilNextDrop[row] - rowTimeElapsed[row], 0);
            if (remaining < shortest) {
                shortest = remaining;
                selected = row;
            }
        }
        return selected;
    }

    @Override
    protected boolean hasStackUpgrade() {
        return getUpgradeCount(STACK_UPGRADE_SLOT) > 0;
    }

    @Override
    protected int getStackUpgradeCount() {
        return getUpgradeCount(STACK_UPGRADE_SLOT);
    }

    @Override
    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return slot == SPEED_UPGRADE_SLOT && stack.is(ModRegistry.SPEED_UPGRADE.get())
                || slot == STACK_UPGRADE_SLOT && stack.is(ModRegistry.STACK_UPGRADE.get());
    }

    @Override
    public int getUpgradeMaxStackSize(int slot) {
        return slot == SPEED_UPGRADE_SLOT ? 5
                : slot == STACK_UPGRADE_SLOT ? MAX_MECHANICAL_STACK_UPGRADES : 1;
    }

    @Override
    protected int getMaxStackSizeForSlotWithStackUpgrades(int slot, ItemStack stack, int stackUpgradeCount) {
        if (slot >= 0 && slot < CHICKEN_SLOT_COUNT) {
            int upgrades = Math.min(stackUpgradeCount, MAX_MECHANICAL_STACK_UPGRADES);
            int max = Math.min(MAX_CHICKENS, stack.getMaxStackSize()) + MAX_CHICKENS * upgrades;
            return Math.min(MAX_VIRTUAL_STACK_SIZE, max);
        }
        int upgrades = Math.min(stackUpgradeCount, MAX_MECHANICAL_STACK_UPGRADES);
        int max = Math.min(stack.getMaxStackSize(), getMaxStackSize()) + MAX_CHICKENS * upgrades;
        return Math.min(MAX_VIRTUAL_STACK_SIZE, max);
    }

    @Override
    public boolean canRemoveUpgrade(int slot, int count) {
        return slot != STACK_UPGRADE_SLOT || canRemoveStackUpgrade(count);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack previous = index >= 0 && index < CHICKEN_SLOT_COUNT ? getItem(index).copy() : ItemStack.EMPTY;
        super.setItem(index, stack);
        if (index >= 0 && index < CHICKEN_SLOT_COUNT
                && !ItemStack.isSameItemSameComponents(previous, getItem(index))) {
            rowChickenData[index] = null;
            clearRowTimer(index);
        }
    }

    public int getEnergyCostPerEgg() {
        return Math.max(1, ChickensConfigHolder.get().getIncubatorEnergyCost());
    }

    public boolean pullChickensOut(Player player) {
        boolean removed = false;
        for (int slot = CHICKEN_SLOT_COUNT - 1; slot >= 0; slot--) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            setItem(slot, ItemStack.EMPTY);
            int maxExternalStackSize = getLegalExternalStackSize(stack);
            ItemStack remaining = stack.copy();
            while (!remaining.isEmpty()) {
                ItemStack toGive = remaining.split(maxExternalStackSize);
                if (!player.addItem(toGive) && !toGive.isEmpty()) {
                    player.drop(toGive, false);
                }
            }
            removed = true;
        }
        return removed;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return capacity;
    }

    @Override
    public boolean isEmpty() {
        if (!super.isEmpty()) {
            return false;
        }
        for (List<ItemStack> pending : rowPendingOutputs) {
            if (!pending.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        for (List<ItemStack> pending : rowPendingOutputs) {
            pending.clear();
        }
    }

    @Override
    public void dropBufferedOutput() {
        super.dropBufferedOutput();
        if (level == null || level.isClientSide) {
            return;
        }
        for (List<ItemStack> pending : rowPendingOutputs) {
            for (ItemStack stack : pending) {
                dropLegalStack(stack);
            }
            pending.clear();
        }
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
    }

    public int getComparatorOutput() {
        return capacity <= 0 ? 0 : Math.round(15.0F * getEnergyStored() / (float) capacity);
    }

    private void pullEnergyFromNeighbors(Level level) {
        if (energyStorage.getEnergyStored() >= capacity || maxReceive <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (energyStorage.getEnergyStored() >= capacity) {
                return;
            }
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            int space = Math.min(maxReceive, capacity - energyStorage.getEnergyStored());
            int available = neighbor.extractEnergy(space, true);
            if (available <= 0) {
                continue;
            }
            int accepted = energyStorage.receiveEnergy(available, true);
            if (accepted <= 0) {
                continue;
            }
            int drained = neighbor.extractEnergy(accepted, false);
            if (drained > 0) {
                energyStorage.receiveEnergy(drained, false);
            }
        }
    }

    private void syncWithConfig(boolean overwriteCapacity) {
        var config = ChickensConfigHolder.get();
        int configuredCapacity = Math.max(1, config.getIncubatorEnergyCapacity());
        int configuredReceive = Math.max(1, config.getIncubatorEnergyMaxReceive());
        int minimumCapacity = (int) Math.min(Integer.MAX_VALUE,
                Math.max((long) DEFAULT_ENERGY_CAPACITY,
                        (long) Math.max(1, config.getIncubatorEnergyCost()) * MAX_OUTPUT_COUNT_PER_CYCLE));
        configuredCapacity = Math.max(configuredCapacity, minimumCapacity);
        if (overwriteCapacity) {
            capacity = configuredCapacity;
        } else {
            capacity = Math.max(minimumCapacity, Math.min(capacity, configuredCapacity));
        }
        maxReceive = configuredReceive;
        energyStorage.setLimits(capacity, maxReceive);
    }

    private void updateActiveState(Level level, boolean active) {
        if (cachedActiveState == active) {
            return;
        }
        cachedActiveState = active;
        BlockState state = getBlockState();
        if (!state.hasProperty(MechanicalRoostBlock.LIT)) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(MechanicalRoostBlock.LIT, active), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Energy", getEnergyStored());
        tag.putIntArray("RowTimeUntilNextDrop", rowTimeUntilNextDrop);
        tag.putIntArray("RowTimeElapsed", rowTimeElapsed);
        tag.putIntArray("RowProgress", rowProgress);
        tag.putIntArray("RowRawLayTime", rowRawLayTime);
        ListTag pendingRows = new ListTag();
        for (List<ItemStack> pending : rowPendingOutputs) {
            CompoundTag rowTag = new CompoundTag();
            ListTag stacks = new ListTag();
            for (ItemStack stack : pending) {
                if (!stack.isEmpty()) {
                    stacks.add(saveVirtualStack(stack, provider));
                }
            }
            rowTag.put("Items", stacks);
            pendingRows.add(rowTag);
        }
        tag.put("RowPendingOutputs", pendingRows);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        syncWithConfig(false);
        energyStorage.setEnergy(Mth.clamp(tag.getInt("Energy"), 0, capacity));
        loadRowValues(tag.getIntArray("RowTimeUntilNextDrop"), rowTimeUntilNextDrop, Integer.MAX_VALUE);
        loadRowValues(tag.getIntArray("RowTimeElapsed"), rowTimeElapsed, Integer.MAX_VALUE);
        loadRowValues(tag.getIntArray("RowProgress"), rowProgress, 1000);
        loadRowValues(tag.getIntArray("RowRawLayTime"), rowRawLayTime, Integer.MAX_VALUE);
        for (List<ItemStack> pending : rowPendingOutputs) {
            pending.clear();
        }
        if (tag.contains("RowPendingOutputs", Tag.TAG_LIST)) {
            ListTag pendingRows = tag.getList("RowPendingOutputs", Tag.TAG_COMPOUND);
            int rowCount = Math.min(CHICKEN_SLOT_COUNT, pendingRows.size());
            for (int row = 0; row < rowCount; row++) {
                CompoundTag rowTag = pendingRows.getCompound(row);
                if (!rowTag.contains("Items", Tag.TAG_LIST)) {
                    continue;
                }
                ListTag stacks = rowTag.getList("Items", Tag.TAG_COMPOUND);
                for (int index = 0; index < stacks.size(); index++) {
                    ItemStack stack = loadVirtualStack(stacks.getCompound(index), provider);
                    if (!stack.isEmpty()) {
                        rowPendingOutputs.get(row).add(stack);
                    }
                }
            }
        }
        for (int row = 0; row < CHICKEN_SLOT_COUNT; row++) {
            rowChickenData[row] = null;
        }
        rowTimersLoaded = true;
    }

    private static void loadRowValues(int[] values, int[] target, int max) {
        for (int row = 0; row < target.length; row++) {
            int value = row < values.length ? values[row] : 0;
            target[row] = Mth.clamp(value, 0, max);
        }
    }

    private final class MachineEnergyStorage extends EnergyStorage {
        MachineEnergyStorage() {
            super(DEFAULT_ENERGY_CAPACITY, DEFAULT_ENERGY_MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int previousMaxReceive = this.maxReceive;
            this.maxReceive = MechanicalRoostBlockEntity.this.maxReceive;
            int received = super.receiveEnergy(amount, simulate);
            this.maxReceive = previousMaxReceive;
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
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

        boolean consumeEnergy(int amount) {
            if (amount <= 0) {
                return true;
            }
            if (this.energy < amount) {
                return false;
            }
            this.energy -= amount;
            setChanged();
            return true;
        }
    }
}
