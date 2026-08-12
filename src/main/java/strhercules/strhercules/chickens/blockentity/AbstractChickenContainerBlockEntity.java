package strhercules.chickens.blockentity;

import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.integration.mekanism.MekanismRadiationCompat;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.item.ChickenStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Base implementation that mirrors the 1.12 roost tile entity logic. The
 * container tracks chicken stacks, internal timers and output slots while
 * remaining agnostic about the concrete drop behaviour.
 */
public abstract class AbstractChickenContainerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    /** Minecraft 1.21.1's normal ItemStack codec accepts counts only up to 99. */
    protected static final int MAX_SERIALIZED_STACK_SIZE = 99;
    /** Machine slots may hold this many items through the virtual-count format. */
    protected static final int MAX_VIRTUAL_STACK_SIZE = 256;
    protected static final int MAX_STACK_UPGRADE_COUNT = 4;

    protected static final class ChickenContainerEntry {
        private final ChickensRegistryItem chicken;
        private final ChickenStats stats;

        public ChickenContainerEntry(ChickensRegistryItem chicken, ChickenStats stats) {
            this.chicken = Objects.requireNonNull(chicken, "chicken");
            this.stats = Objects.requireNonNull(stats, "stats");
        }

        public ChickensRegistryItem chicken() {
            return chicken;
        }

        public ChickenStats stats() {
            return stats;
        }

        public ItemStack createDrop(RandomSource random) {
            return chicken.createDropItem();
        }

        public ItemStack createLay(RandomSource random, boolean scalingDrops) {
            ItemStack stack = chicken.createLayItem();
            if (scalingDrops) {
                stats.scaleOutput(stack);
            }
            return stack;
        }


        public int getAddedTime(ItemStack stack) {
            int raw = Math.max(0, stack.getCount()) * Math.max(stats.growth(), 1);
            return Math.min(raw, 2048);
        }

        public int getLayTime(RandomSource random) {
            int min = Math.max(chicken.getMinLayTime(), 1);
            int max = Math.max(chicken.getMaxLayTime(), min);
            if (max <= min) {
                return min;
            }
            return min + random.nextInt(max - min);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ChickenContainerEntry other)) {
                return false;
            }
            return chicken == other.chicken && stats.equals(other.stats);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chicken, stats);
        }
    }

    /**
     * Lightweight DTO exposed to client renderers so they can animate the
     * chickens sitting inside a container without needing to understand the
     * internal inventory layout.
     */
    public record RenderData(ChickensRegistryItem chicken, ChickenStats stats, int count) {
    }

    private final NonNullList<ItemStack> items;
    private final NonNullList<ItemStack> upgradeItems;
    private final ChickenContainerEntry[] chickenData;
    private final ContainerData dataAccess;
    private boolean needsChickenUpdate = true;
    private boolean skipNextTimerReset = false;
    private int timeUntilNextDrop = 0;
    private int timeElapsed = 0;
    private int progress = 0;
    private boolean fullOfChickens = false;
    private boolean fullOfSeeds = false;
    private ItemStack pendingOutput = ItemStack.EMPTY;

    protected AbstractChickenContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int inventorySize, int chickenSlotCount, int upgradeSlotCount) {
        super(type, pos, state);
        this.items = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
        this.upgradeItems = NonNullList.withSize(upgradeSlotCount, ItemStack.EMPTY);
        this.chickenData = new ChickenContainerEntry[chickenSlotCount];
        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? progress : getContainerDataValue(index);
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    progress = value;
                } else {
                    setContainerDataValue(index, value);
                }
            }

            @Override
            public int getCount() {
                return getContainerDataCount();
            }
        };
    }

    protected int getContainerDataCount() {
        return 1;
    }

    protected int getContainerDataValue(int index) {
        return 0;
    }

    protected void setContainerDataValue(int index, int value) {
    }

    /**
     * Returns the active machine-storage size. Collector overrides this to
     * expose only the rows unlocked by its capacity upgrades.
     */
    protected int getActiveStorageSize() {
        return items.size();
    }

    protected boolean hasStackUpgrade() {
        return false;
    }

    protected int getStackUpgradeCount() {
        return hasStackUpgrade() ? 1 : 0;
    }

    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return false;
    }

    public boolean canRemoveUpgrade(int slot) {
        return canRemoveUpgrade(slot, 1);
    }

    public boolean canRemoveUpgrade(int slot, int count) {
        return true;
    }

    public int getUpgradeMaxStackSize(int slot) {
        return 1;
    }

    public final int getUpgradeSlotIndex(int slot) {
        return items.size() + slot;
    }

    public final int getUpgradeCount(int slot) {
        if (slot < 0 || slot >= upgradeItems.size()) {
            return 0;
        }
        return upgradeItems.get(slot).getCount();
    }

    public final int getUpgradeSlotCount() {
        return upgradeItems.size();
    }

    public final int getOutputSlotCount() {
        return Math.max(0, getActiveStorageSize() - getOutputSlotIndex());
    }

    public static <T extends AbstractChickenContainerBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state,
            T container) {
        container.runServerTick(level);
        MekanismRadiationCompat.tickMachineWarning(level, pos, container);
    }

    protected void runServerTick(Level level) {
        if (level.isClientSide) {
            return;
        }
        flushPendingOutput();
        updateChickenInfoIfNeeded(level);
        updateTimerIfNeeded(level);
        spawnChickenItemIfNeeded(level);
        updateProgress();
        skipNextTimerReset = false;
    }

    private void updateChickenInfoIfNeeded(Level level) {
        if (!needsChickenUpdate) {
            return;
        }
        boolean wasFullOfChickens = fullOfChickens;
        boolean wasFullOfSeeds = fullOfSeeds;
        fullOfChickens = isFullOfChickens();
        fullOfSeeds = isFullOfSeeds();
        // Always push a block update when chicken inventory data changes so the client
        // receives the refreshed stack counts without needing an extra GUI sync.
        notifyBlockUpdate(level);
        if (wasFullOfChickens != fullOfChickens || wasFullOfSeeds != fullOfSeeds) {
            onFullnessChanged(level, fullOfChickens, fullOfSeeds);
        }
        needsChickenUpdate = false;
    }

    private void updateTimerIfNeeded(Level level) {
        if (fullOfChickens && fullOfSeeds && pendingOutput.isEmpty() && !outputIsFull()) {
            timeElapsed += getTimeElapsed();
            setChanged();
        }
    }

    private void spawnChickenItemIfNeeded(Level level) {
        if (fullOfChickens && fullOfSeeds && timeElapsed >= timeUntilNextDrop) {
            if (timeUntilNextDrop > 0) {
                if (!pendingOutput.isEmpty()) {
                    return;
                }
                if (!spawnChickenItem(level.random)) {
                    return;
                }
                consumeSeeds();
            }
            resetTimer(level);
            timeElapsed = 0;
        }
    }

    private void updateProgress() {
        int newProgress = timeUntilNextDrop == 0 ? 0 : Math.min(1000, timeElapsed * 1000 / Math.max(timeUntilNextDrop, 1));
        if (newProgress != progress) {
            progress = newProgress;
            setChanged();
        }
    }

    private int getTimeElapsed() {
        int result = Integer.MAX_VALUE;
        for (int slot = 0; slot < chickenData.length; slot++) {
            ChickenContainerEntry entry = chickenData[slot];
            if (entry == null) {
                return 0;
            }
            result = Math.min(result, entry.getAddedTime(getItem(slot)));
        }
        return result == Integer.MAX_VALUE ? 0 : result;
    }

    private void consumeSeeds() {
        int seedRequirement = requiredSeedsForDrop();
        if (seedRequirement <= 0) {
            return;
        }
        int seedSlot = getSeedSlotIndex();
        if (seedSlot >= 0) {
            removeItem(seedSlot, seedRequirement);
        }
    }

    private void resetTimer(Level level) {
        timeElapsed = 0;
        timeUntilNextDrop = 0;
        int fixedTarget = getTargetCycleTicks();
        if (fixedTarget > 0) {
            // Use the tier-aware fixed duration instead of the generic formula.
            timeUntilNextDrop = fixedTarget;
        } else {
            RandomSource random = level.random;
            for (ChickenContainerEntry entry : chickenData) {
                if (entry != null) {
                    timeUntilNextDrop = Math.max(timeUntilNextDrop, entry.getLayTime(random));
                }
            }
            double multiplier = Math.max(speedMultiplier(), 0.0001D);
            timeUntilNextDrop = (int) (timeUntilNextDrop / multiplier);
        }
        setChanged();
    }

    /**
     * spawns the item that should be spawned during @method runTick
     * @param random
     */
    protected abstract boolean spawnChickenItem(RandomSource random);

    protected abstract int requiredSeedsForDrop();

    protected abstract double speedMultiplier();

    /**
     * Optional override to set a fixed cycle duration in ticks.
     * When this returns a value > 0 it takes precedence over the
     * speedMultiplier + getLayTime calculation in resetTimer.
     * Return 0 to use the default behaviour.
     */
    protected int getTargetCycleTicks() {
        return 0;
    }

    protected abstract int getChickenSlotCount();

    protected abstract Component getDefaultName();

    protected abstract AbstractContainerMenu createMenu(int id, Inventory playerInventory, ContainerData dataAccess);

    @Nullable
    protected abstract ChickenContainerEntry createChickenData(int slot, ItemStack stack);

    protected void markChickenDataDirty() {
        needsChickenUpdate = true;
    }

    @Nullable
    protected ChickenContainerEntry getChickenEntry(int slot) {
        if (slot < 0 || slot >= chickenData.length) {
            return null;
        }
        return chickenData[slot];
    }

    /**
     * Extracts the data required for visualising a chicken in the given slot.
     * Renderers rely on this rather than touching the raw container stacks so
     * server logic stays encapsulated inside the block entity.
     */
    @Nullable
    public RenderData getRenderData(int slot) {

        if (slot < 0 || slot >= getContainerSize()) {
            return null;
        }
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return null;
        }
        ChickenContainerEntry entry = getChickenEntry(slot);
        if (entry == null) {
            entry = createChickenData(slot, stack);
            if (entry == null) {
                return null;
            }
            if (slot < chickenData.length) {
                Level level = getLevel();
                if (level != null && level.isClientSide) {
                    // Cache the generated entry client-side so renderers keep working before the next server sync.
                    chickenData[slot] = entry;
                }
            }
        }
        return new RenderData(entry.chicken(), entry.stats(), stack.getCount());
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getProgress() {
        return progress;
    }

    public double getProgressFraction() {
        return progress / 1000.0D;
    }

    /**
     * Exposes the configured lay timer so integrations can forecast when the
     * next operation will complete. The raw value represents the total ticks
     * required for the current batch once all modifiers have been applied on
     * the server.
     */
    public int getTotalLayTimeTicks() {
        return Math.max(timeUntilNextDrop, 0);
    }

    /**
     * Returns the number of ticks remaining before the current production
     * cycle finishes. When the container is idle the counter resolves to zero
     * so callers can short-circuit any ETA display logic.
     */
    public int getRemainingLayTimeTicks() {
        return Math.max(timeUntilNextDrop - timeElapsed, 0);
    }

    /**
     * Reports how many progress units elapse per server tick while the
     * container is actively working. This lets external integrations translate
     * the internal counters into real-time durations even when multiple
     * chickens accelerate production.
     */
    public int getProgressIncrementPerTick() {
        if (fullOfChickens && fullOfSeeds && !outputIsFull()) {
            return Math.max(getTimeElapsed(), 0);
        }
        return 0;
    }

    private boolean isFullOfChickens() {
        for (int slot = 0; slot < chickenData.length; slot++) {
            updateChickenInfoForSlot(slot);
            if (chickenData[slot] == null) {
                return false;
            }
        }
        return chickenData.length > 0;
    }

    private void updateChickenInfoForSlot(int slot) {
        ChickenContainerEntry oldEntry = chickenData[slot];
        ItemStack stack = getItem(slot);
        ChickenContainerEntry newEntry = createChickenData(slot, stack);
        boolean changed = !Objects.equals(oldEntry, newEntry);
        if (changed) {
            chickenData[slot] = newEntry;
            if (!skipNextTimerReset) {
                Level level = getLevel();
                if (level != null && !level.isClientSide) {
                    resetTimer(level);
                }
            }
            setChanged();
        }
    }

    private boolean isFullOfSeeds() {
        int required = requiredSeedsForDrop();
        if (required <= 0) {
            return true;
        }
        int seedSlot = getSeedSlotIndex();
        if (seedSlot < 0) {
            return false;
        }
        ItemStack stack = getItem(seedSlot);
        return stack.getCount() >= required;
    }

    protected boolean outputIsFull() {
        int start = getOutputSlotIndex();
        for (int slot = start; slot < getActiveStorageSize(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getCount() < getMaxStackSizeForSlot(slot, stack)) {
                return false;
            }
        }
        return true;
    }

    protected ItemStack pushIntoOutput(ItemStack stack) {
        ItemStack remaining = stack.copy();
        int start = getOutputSlotIndex();
        for (int slot = start; slot < getActiveStorageSize() && !remaining.isEmpty(); slot++) {
            remaining = insertStack(remaining, slot);
        }
        if (remaining.isEmpty()) {
            markChickenDataDirty();
        }
        return remaining;
    }

    /** Stores a generated stack in the visible slots or in one persisted overflow slot. */
    protected final boolean queueOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack remaining = pushIntoOutput(stack);
        if (remaining.isEmpty()) {
            return true;
        }
        if (pendingOutput.isEmpty()) {
            pendingOutput = remaining;
            setChanged();
            return true;
        }
        return false;
    }

    private void flushPendingOutput() {
        if (pendingOutput.isEmpty()) {
            return;
        }
        pendingOutput = pushIntoOutput(pendingOutput);
        if (pendingOutput.isEmpty()) {
            setChanged();
        }
    }

    /** Drops the persisted overflow stack when the container itself is removed. */
    public void dropBufferedOutput() {
        if (pendingOutput.isEmpty() || level == null || level.isClientSide) {
            return;
        }
        dropLegalStack(pendingOutput);
        pendingOutput = ItemStack.EMPTY;
    }

    private ItemStack insertStack(ItemStack stack, int slot) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = items.get(slot);
        int maxStackSize = getMaxStackSizeForSlot(slot, stack);
        if (existing.isEmpty()) {
            ItemStack toInsert = stack.split(maxStackSize);
            if (stack.isEmpty()) {
                items.set(slot, toInsert);
                setChanged();
                return ItemStack.EMPTY;
            }
            items.set(slot, toInsert);
            setChanged();
            return stack;
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int canMove = Math.min(maxStackSize - existing.getCount(), stack.getCount());
        if (canMove <= 0) {
            return stack;
        }
        existing.grow(canMove);
        stack.shrink(canMove);
        setChanged();
        return stack;
    }

    protected int getMaxStackSizeForSlotWithStackUpgrades(int slot, ItemStack stack, int stackUpgradeCount) {
        int maxStackSize = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        for (int upgrade = 0; upgrade < stackUpgradeCount; upgrade++) {
            maxStackSize = Math.min(MAX_VIRTUAL_STACK_SIZE, maxStackSize * 2);
        }
        return maxStackSize;
    }

    public int getMaxStackSizeForSlot(int slot, ItemStack stack) {
        return getMaxStackSizeForSlotWithStackUpgrades(slot, stack, getStackUpgradeCount());
    }

    protected final boolean canRemoveStackUpgrade(int count) {
        int installed = getStackUpgradeCount();
        int remaining = Math.max(0, installed - Math.max(count, 0));
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()
                    && stack.getCount() > getMaxStackSizeForSlotWithStackUpgrades(slot, stack, remaining)) {
                return false;
            }
        }
        return true;
    }

    protected int getSeedSlotIndex() {
        return requiredSeedsForDrop() > 0 ? getChickenSlotCount() : -1;
    }

    public int getOutputSlotIndex() {
        return getChickenSlotCount() + (requiredSeedsForDrop() > 0 ? 1 : 0);
    }

    @Override
    public int getContainerSize() {
        return items.size() + upgradeItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        for (ItemStack stack : upgradeItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < items.size()) {
            return items.get(index);
        }
        return upgradeItems.get(index - items.size());
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= items.size() && !canRemoveUpgrade(index - items.size(), count)) {
            return ItemStack.EMPTY;
        }
        if (index < items.size()) {
            count = Math.min(count, getLegalExternalStackSize(getItem(index)));
        }
        return removeItemInternal(index, count);
    }

    public final ItemStack removeItemForMachine(int index, int count) {
        return removeItemInternal(index, count);
    }

    private ItemStack removeItemInternal(int index, int count) {
        if (index < getOutputSlotIndex()) {
            markChickenDataDirty();
        }
        ItemStack result = index < items.size()
                ? ContainerHelper.removeItem(items, index, count)
                : ContainerHelper.removeItem(upgradeItems, index - items.size(), count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= items.size()) {
            int upgradeSlot = index - items.size();
            if (upgradeSlot < 0 || upgradeSlot >= upgradeItems.size()
                    || !canRemoveUpgrade(upgradeSlot, upgradeItems.get(upgradeSlot).getCount())) {
                return ItemStack.EMPTY;
            }
        }
        if (index < getOutputSlotIndex()) {
            markChickenDataDirty();
        }
        ItemStack result;
        if (index < items.size()) {
            int count = Math.min(getItem(index).getCount(), getLegalExternalStackSize(getItem(index)));
            result = ContainerHelper.removeItem(items, index, count);
        } else {
            result = ContainerHelper.takeItem(upgradeItems, index - items.size());
        }
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= items.size()) {
            int upgradeSlot = index - items.size();
            if (upgradeSlot < 0 || upgradeSlot >= upgradeItems.size()) {
                return;
            }
            // Client menus must mirror the authoritative stack received from
            // the server even if their local block-entity state is stale.
            if (level != null && level.isClientSide) {
                upgradeItems.set(upgradeSlot, stack.copy());
                return;
            }
            ItemStack oldUpgrade = upgradeItems.get(upgradeSlot);
            int removedCount = Math.max(0, oldUpgrade.getCount() - (canPlaceUpgrade(upgradeSlot, stack)
                    ? Math.min(stack.getCount(), getUpgradeMaxStackSize(upgradeSlot)) : 0));
            if (removedCount > 0 && !canRemoveUpgrade(upgradeSlot, removedCount)) {
                return;
            }
            if (!stack.isEmpty() && !canPlaceUpgrade(upgradeSlot, stack)) {
                return;
            }
            if (stack.getCount() > getUpgradeMaxStackSize(upgradeSlot)) {
                stack.setCount(getUpgradeMaxStackSize(upgradeSlot));
            }
            upgradeItems.set(upgradeSlot, stack);
            setChanged();
            return;
        }
        if (level != null && level.isClientSide) {
            // Menu packets carry the authoritative virtual count. Do not clamp
            // it against a client upgrade state that may be one packet behind.
            items.set(index, stack.copy());
            return;
        }
        items.set(index, stack);
        int maxStackSize = getMaxStackSizeForSlot(index, stack);
        if (stack.getCount() > maxStackSize) {
            stack.setCount(maxStackSize);
        }
        if (index < getOutputSlotIndex()) {
            markChickenDataDirty();
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
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        if (index >= items.size()) {
            return canPlaceUpgrade(index - items.size(), stack);
        }
        if (index >= getActiveStorageSize()) {
            return false;
        }
        if (index < getChickenSlotCount()) {
            return ChickenItemHelper.isChicken(stack);
        }
        if (index == getSeedSlotIndex()) {
            return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                    || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index >= getOutputSlotIndex() && index < getActiveStorageSize();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < items.size(); index++) {
            items.set(index, ItemStack.EMPTY);
        }
        for (int index = 0; index < upgradeItems.size(); index++) {
            upgradeItems.set(index, ItemStack.EMPTY);
        }
        pendingOutput = ItemStack.EMPTY;
        setChanged();
        markChickenDataDirty();
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return createMenu(id, playerInventory, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveItems(tag, registries);
        saveUpgrades(tag, registries);
        if (!pendingOutput.isEmpty()) {
            tag.put("PendingOutput", saveVirtualStack(pendingOutput, registries));
        }
        tag.putInt("TimeUntilNextDrop", timeUntilNextDrop);
        tag.putInt("TimeElapsed", timeElapsed);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        loadUpgrades(tag, registries);
        loadVirtualItemCounts(tag);
        clampLoadedItems();
        pendingOutput = tag.contains("PendingOutput", net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? loadVirtualStack(tag.getCompound("PendingOutput"), registries)
                : ItemStack.EMPTY;
        timeUntilNextDrop = tag.getInt("TimeUntilNextDrop");
        timeElapsed = tag.getInt("TimeElapsed");
        progress = tag.getInt("Progress");
        skipNextTimerReset = true;
        markChickenDataDirty();
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Broadcast the full block entity tag whenever the server marks the chicken data
        // dirty so the renderer can immediately reflect newly inserted stacks.
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    private void notifyBlockUpdate(Level level) {
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> result = NonNullList.create();
        for (ItemStack stack : items) {
            addLegalStacks(stack, result);
        }
        for (ItemStack stack : upgradeItems) {
            addLegalStacks(stack, result);
        }
        return result;
    }

    private static void addLegalStacks(ItemStack stack, NonNullList<ItemStack> result) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        int max = serializedStackSize(stack);
        while (!remaining.isEmpty()) {
            result.add(remaining.split(max));
        }
    }

    private static int serializedStackSize(ItemStack stack) {
        return Math.max(1, Math.min(MAX_SERIALIZED_STACK_SIZE, stack.getMaxStackSize()));
    }

    public static int getLegalExternalStackSize(ItemStack stack) {
        return stack.isEmpty() ? 0 : serializedStackSize(stack);
    }

    private void saveItems(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        NonNullList<ItemStack> serialized = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        ListTag virtualCounts = new ListTag();
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            int serializedCount = Math.min(count, serializedStackSize(stack));
            serialized.set(index, stack.copyWithCount(serializedCount));
            if (count > serializedCount) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", index);
                entry.putInt("Count", count);
                virtualCounts.add(entry);
            }
        }
        ContainerHelper.saveAllItems(tag, serialized, registries);
        tag.put("VirtualItemCounts", virtualCounts);
    }

    private void loadVirtualItemCounts(CompoundTag tag) {
        if (!tag.contains("VirtualItemCounts", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("VirtualItemCounts", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getInt("Slot");
            int count = entry.getInt("Count");
            if (slot < 0 || slot >= items.size() || count <= 0 || items.get(slot).isEmpty()) {
                continue;
            }
            items.get(slot).setCount(count);
        }
    }

    private void clampLoadedItems() {
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (!stack.isEmpty()) {
                // Client block-entity packets can arrive before the menu's upgrade
                // slot packets. Preserve the server's virtual count until those
                // authoritative slot updates arrive instead of clamping it to a
                // stale pre-upgrade capacity.
                int maxStackSize = level != null && level.isClientSide
                        ? MAX_VIRTUAL_STACK_SIZE : getMaxStackSizeForSlot(index, stack);
                stack.setCount(Math.min(stack.getCount(), maxStackSize));
            }
        }
    }

    private static CompoundTag saveVirtualStack(ItemStack stack,
            net.minecraft.core.HolderLookup.Provider registries) {
        int count = stack.getCount();
        CompoundTag tag = (CompoundTag) stack.copyWithCount(Math.min(count, serializedStackSize(stack)))
                .save(registries);
        if (count > serializedStackSize(stack)) {
            tag.putInt("VirtualCount", count);
        }
        return tag;
    }

    private static ItemStack loadVirtualStack(CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag stackTag = tag.copy();
        stackTag.remove("VirtualCount");
        ItemStack stack = ItemStack.parse(registries, stackTag).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty() && tag.contains("VirtualCount", Tag.TAG_INT)) {
            stack.setCount(tag.getInt("VirtualCount"));
        }
        return stack;
    }

    private void dropLegalStack(ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        int max = serializedStackSize(stack);
        while (!remaining.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D, remaining.split(max));
        }
    }

    private void saveUpgrades(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int index = 0; index < upgradeItems.size(); index++) {
            ItemStack stack = upgradeItems.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = (CompoundTag) stack.save(registries);
            entry.putByte("Slot", (byte) index);
            list.add(entry);
        }
        tag.put("Upgrades", list);
    }

    private void loadUpgrades(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (!tag.contains("Upgrades", Tag.TAG_LIST)) {
            // Some block-entity packets are partial. Keep the client menu's
            // already-synchronised upgrade stacks when no upgrade payload was
            // included instead of making its slots and layout disappear.
            return;
        }
        ListTag list = tag.getList("Upgrades", Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            for (int index = 0; index < upgradeItems.size(); index++) {
                upgradeItems.set(index, ItemStack.EMPTY);
            }
            return;
        }
        NonNullList<ItemStack> loaded = NonNullList.withSize(upgradeItems.size(), ItemStack.EMPTY);
        int validEntries = 0;
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte("Slot");
            if (slot < 0 || slot >= upgradeItems.size()) {
                continue;
            }
            CompoundTag stackTag = entry.copy();
            stackTag.remove("Slot");
            ItemStack stack = ItemStack.parse(registries, stackTag).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty() && canPlaceUpgrade(slot, stack)) {
                stack.setCount(Math.min(stack.getCount(), getUpgradeMaxStackSize(slot)));
                loaded.set(slot, stack);
                validEntries++;
            }
        }
        if (validEntries == 0) {
            return;
        }
        for (int index = 0; index < upgradeItems.size(); index++) {
            upgradeItems.set(index, loaded.get(index));
        }
    }

    /**
     * Hook that specialised containers can override to update block states or
     * trigger particles whenever the chicken or seed state flips. The base
     * implementation intentionally does nothing.
     */
    protected void onFullnessChanged(Level level, boolean hasRequiredChickens, boolean hasRequiredSeeds) {
    }

    /**
     * Exposes whether every chicken slot is currently populated so blocks can
     * reflect the filled animation in their block states.
     */
    public boolean hasRequiredChickens() {
        return fullOfChickens;
    }

    /**
     * Returns true when enough seeds are present to trigger the next drop.
     */
    public boolean hasRequiredSeeds() {
        return fullOfSeeds;
    }

    /**
     * Serialises the state required to build an overlay tooltip. Jade/Waila reads
     * this data on the client after {@link #appendTooltip(List, CompoundTag)} has
     * converted it into human readable text.
     */
    public void storeTooltipData(CompoundTag tag) {
        tag.putFloat("Progress", (float) getProgressFraction());
        tag.putBoolean("HasSeeds", hasRequiredSeeds());
        tag.putBoolean("HasChickens", hasRequiredChickens());
        tag.putInt("RequiredSeeds", Math.max(requiredSeedsForDrop(), 0));
    }

    /**
     * Populates the client-side tooltip with generic container information.
     */
    public void appendTooltip(List<Component> tooltip, CompoundTag data) {
        if (data.contains("HasChickens") && !data.getBoolean("HasChickens")) {
            tooltip.add(Component.translatable("tooltip.chickens.container.empty"));
            return;
        }
        if (data.contains("Progress")) {
            int percent = Math.round(data.getFloat("Progress") * 100.0F);
            tooltip.add(Component.translatable("tooltip.chickens.container.progress", percent));
        }
        int requiredSeeds = data.getInt("RequiredSeeds");
        if (requiredSeeds > 0 && (!data.contains("HasSeeds") || !data.getBoolean("HasSeeds"))) {
            tooltip.add(Component.translatable("tooltip.chickens.container.no_seeds", requiredSeeds));
        }
    }
}
