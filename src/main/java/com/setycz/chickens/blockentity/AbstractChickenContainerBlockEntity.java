package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickenStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
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
            ItemStack drop = chicken.createDropItem();
            int gain = stats.gain();
            drop.setCount(gain >= 10 ? 3 : gain >= 5 ? 2 : 1);
            return drop;
        }

        public ItemStack createLay(RandomSource random) {
            ItemStack lay = chicken.createLayItem();
            int gain = stats.gain();
            lay.setCount(gain >= 10 ? 3 : gain >= 5 ? 2 : 1);
            return lay;
        }


        public int getAddedTime(ItemStack stack) {
            return Math.max(0, stack.getCount()) * Math.max(stats.growth(), 1);
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
    private final ChickenContainerEntry[] chickenData;
    private final ContainerData dataAccess;
    private boolean needsChickenUpdate = true;
    private boolean skipNextTimerReset = false;
    private int timeUntilNextDrop = 0;
    private int timeElapsed = 0;
    private int progress = 0;
    private boolean fullOfChickens = false;
    private boolean fullOfSeeds = false;

    protected AbstractChickenContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int inventorySize, int chickenSlotCount) {
        super(type, pos, state);
        this.items = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
        this.chickenData = new ChickenContainerEntry[chickenSlotCount];
        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? progress : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    progress = value;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    public static <T extends AbstractChickenContainerBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state,
            T container) {
        container.runServerTick(level);
    }

    protected void runServerTick(Level level) {
        if (level.isClientSide) {
            return;
        }
        updateChickenInfoIfNeeded(level);
        updateTimerIfNeeded(level);
//        spawnChickenDropIfNeeded(level);
        spawnChickenLayIfNeeded(level);
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
        if (fullOfChickens && fullOfSeeds && !outputIsFull()) {
            timeElapsed += getTimeElapsed();
            setChanged();
        }
    }

    private void spawnChickenDropIfNeeded(Level level) {
        if (fullOfChickens && fullOfSeeds && timeElapsed >= timeUntilNextDrop) {
            if (timeUntilNextDrop > 0) {
                consumeSeeds();
                spawnChickenDrop(level.random);
            }
            resetTimer(level);
        }
    }

    private void spawnChickenLayIfNeeded(Level level) {
        if (fullOfChickens && fullOfSeeds && timeElapsed >= timeUntilNextDrop) {
            if (timeUntilNextDrop > 0) {
                consumeSeeds();
                spawnChickenLay(level.random);
            }
            resetTimer(level);
        }
    }

    private void updateProgress() {
        progress = timeUntilNextDrop == 0 ? 0 : Math.min(1000, timeElapsed * 1000 / Math.max(timeUntilNextDrop, 1));
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
        RandomSource random = level.random;
        for (ChickenContainerEntry entry : chickenData) {
            if (entry != null) {
                timeUntilNextDrop = Math.max(timeUntilNextDrop, entry.getLayTime(random));
            }
        }
//        chic
        double multiplier = Math.max(speedMultiplier(), 0.0001D);
        timeUntilNextDrop = (int) (timeUntilNextDrop / multiplier);
        setChanged();
    }

    protected abstract void spawnChickenDrop(RandomSource random);
    protected abstract void spawnChickenLay(RandomSource random);

    protected abstract int requiredSeedsForDrop();

    protected abstract double speedMultiplier();

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

        if (slot < 0 || slot >= items.size()) {
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

    private boolean outputIsFull() {
        int start = getOutputSlotIndex();
        for (int slot = start; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    protected ItemStack pushIntoOutput(ItemStack stack) {
        ItemStack remaining = stack.copy();
        int start = getOutputSlotIndex();
        for (int slot = start; slot < items.size() && !remaining.isEmpty(); slot++) {
            remaining = insertStack(remaining, slot);
        }
        if (remaining.isEmpty()) {
            markChickenDataDirty();
        }
        return remaining;
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

    protected int getMaxStackSizeForSlot(int slot, ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), getMaxStackSize());
    }

    protected int getSeedSlotIndex() {
        return requiredSeedsForDrop() > 0 ? getChickenSlotCount() : -1;
    }

    protected int getOutputSlotIndex() {
        return getChickenSlotCount() + (requiredSeedsForDrop() > 0 ? 1 : 0);
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
        if (index < getOutputSlotIndex()) {
            markChickenDataDirty();
        }
        ItemStack result = ContainerHelper.removeItem(items, index, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < getOutputSlotIndex()) {
            markChickenDataDirty();
        }
        ItemStack result = ContainerHelper.takeItem(items, index);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
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
        return index >= getOutputSlotIndex();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[items.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public void clearContent() {
        items.clear();
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
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("TimeUntilNextDrop", timeUntilNextDrop);
        tag.putInt("TimeElapsed", timeElapsed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        timeUntilNextDrop = tag.getInt("TimeUntilNextDrop");
        timeElapsed = tag.getInt("TimeElapsed");
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
        return items;
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
