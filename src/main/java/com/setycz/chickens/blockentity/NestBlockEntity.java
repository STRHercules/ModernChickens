package com.setycz.chickens.blockentity;

import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.menu.NestMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Block entity backing the rooster nest. It stores a stack of rooster items
 * alongside a seed buffer that is slowly consumed to power the rooster aura.
 * The aura itself is evaluated by nearby roosts via
 * {@link #getActiveRoosterCount()} and {@link #hasActiveAura()}.
 */
public class NestBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int ROOSTER_SLOT = 0;
    public static final int SEED_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;
    private static final int[] ACCESSIBLE_SLOTS = new int[] { ROOSTER_SLOT, SEED_SLOT };

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    /** Remaining ticks of aura powered by the currently consumed seed. */
    private int seedTicksRemaining = 0;

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEST.get(), pos, state);
    }

    /**
     * Server tick hook wired from {@link com.setycz.chickens.block.NestBlock}.
     * The nest only performs work on the logical server: it burns seeds into
     * aura fuel while roosters are present.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NestBlockEntity nest) {
        if (level.isClientSide) {
            return;
        }
        nest.tickServer(level);
    }

    private void tickServer(Level level) {
        boolean wasActive = hasActiveAura();
        updateSeedFuel();
        if (wasActive != hasActiveAura()) {
            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    private void updateSeedFuel() {
        int roosters = getRoosterCount();
        if (roosters <= 0) {
            // Pause consumption when no roosters are present so players can pre-load
            // seeds without wasting fuel.
            return;
        }
        if (seedTicksRemaining > 0) {
            seedTicksRemaining--;
            return;
        }
        ItemStack seeds = items.get(SEED_SLOT);
        if (seeds.isEmpty() || !isSeed(seeds)) {
            return;
        }
        int duration = ChickensConfigHolder.get().getNestSeedDurationTicks();
        if (duration <= 0) {
            return;
        }
        // Consume a single seed and recharge the internal aura timer.
        seeds.shrink(1);
        if (seeds.isEmpty()) {
            items.set(SEED_SLOT, ItemStack.EMPTY);
        }
        seedTicksRemaining = duration;
        setChanged();
        notifyBlockUpdate();
    }

    /**
     * Returns how many roosters currently contribute to aura strength from this
     * nest. Only rooster stacks in {@link #ROOSTER_SLOT} are counted.
     */
    public int getRoosterCount() {
        ItemStack stack = items.get(ROOSTER_SLOT);
        if (stack.isEmpty() || !ChickenItemHelper.isRooster(stack)) {
            return 0;
        }
        int max = ChickensConfigHolder.get().getNestMaxRoosters();
        max = Math.max(1, Math.min(16, max));
        return Math.min(stack.getCount(), max);
    }

    /**
     * Indicates whether this nest is currently powering a rooster aura. The
     * aura requires at least one rooster and non-zero seed fuel.
     */
    public boolean hasActiveAura() {
        return getRoosterCount() > 0 && seedTicksRemaining > 0
                && ChickensConfigHolder.get().getNestSeedDurationTicks() > 0;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    /**
     * Inserts rooster items into the nest's dedicated rooster slot. Only stacks
     * marked as roosters via {@link ChickenItemHelper#isRooster(ItemStack)}
     * are accepted; other chicken items must be placed in Roosts or Breeders.
     */
    public boolean putRoosters(ItemStack newStack) {
        if (newStack.isEmpty() || !ChickenItemHelper.isRooster(newStack)) {
            return false;
        }
        ItemStack current = items.get(ROOSTER_SLOT);
        int max = ChickensConfigHolder.get().getNestMaxRoosters();
        max = Math.max(1, Math.min(16, max));
        if (current.isEmpty()) {
            int toMove = Math.min(max, newStack.getCount());
            if (toMove <= 0) {
                return false;
            }
            ItemStack moved = newStack.split(toMove);
            items.set(ROOSTER_SLOT, moved);
            setChanged();
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, newStack)) {
            return false;
        }
        int space = max - current.getCount();
        if (space <= 0) {
            return false;
        }
        int toMove = Math.min(space, newStack.getCount());
        if (toMove <= 0) {
            return false;
        }
        current.grow(toMove);
        newStack.shrink(toMove);
        setChanged();
        return true;
    }

    /**
     * Attempts to move the stored rooster stack into the player's inventory,
     * mirroring the Roost behaviour for quick block cleanup.
     */
    public boolean pullRoostersOut(Player player) {
        ItemStack stack = items.get(ROOSTER_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack toGive = stack.copy();
        items.set(ROOSTER_SLOT, ItemStack.EMPTY);
        if (!player.addItem(toGive)) {
            player.drop(toGive, false);
        }
        setChanged();
        notifyBlockUpdate();
        return true;
    }

    private static boolean isSeed(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS);
    }

    // ---------------------------------------------------------------------
    // Container implementation
    // ---------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
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
        if (index == ROOSTER_SLOT) {
            // Clamp rooster stacks against the configurable nest maximum.
            int max = ChickensConfigHolder.get().getNestMaxRoosters();
            max = Math.max(1, Math.min(16, max));
            if (stack.getCount() > max) {
                stack.setCount(max);
            }
        } else if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        notifyBlockUpdate();
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
        if (index == ROOSTER_SLOT) {
            return ChickenItemHelper.isRooster(stack);
        }
        if (index == SEED_SLOT) {
            return isSeed(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == ROOSTER_SLOT || index == SEED_SLOT;
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
        notifyBlockUpdate();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // ---------------------------------------------------------------------
    // Menu provider
    // ---------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.chickens.nest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new NestMenu(id, playerInventory, this);
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("SeedTicks", seedTicksRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        seedTicksRemaining = tag.getInt("SeedTicks");
    }

    // ---------------------------------------------------------------------
    // Networking – keep client-side renderer in sync with inventory changes
    // so the rooster becomes visible as soon as it is inserted.
    // ---------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider provider) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, provider);
        }
    }

    private void notifyBlockUpdate() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }
}
