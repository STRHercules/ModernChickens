package com.modernfluidcows.blockentity;

import com.modernfluidcows.block.SorterBlock;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.menu.SorterMenu;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.modernfluidcows.util.FCUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity that evaluates whitelist/blacklist filters and teleports matching baby fluid cows.
 */
public class SorterBlockEntity extends BlockEntity implements Clearable, MenuProvider {
    private static final String TAG_FILTER = "Filter";
    private static final String TAG_BLACKLIST = "Blacklist";

    public static final int SLOT_RANGER = 0;
    private static final int SLOT_COUNT = 1;

    private final SimpleContainer inventory =
            new SimpleContainer(SLOT_COUNT) {
                @Override
                public boolean canPlaceItem(final int slot, final ItemStack stack) {
                    return slot == SLOT_RANGER && stack.is(FluidCowsRegistries.RANGER.get());
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setChangedAndSync();
                }
            };

    private final Set<ResourceLocation> filter = new LinkedHashSet<>(5);
    private boolean blacklist;

    public SorterBlockEntity(final BlockPos pos, final BlockState state) {
        super(FluidCowsRegistries.SORTER_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Exposes the configured filter list for menus and client rendering. */
    public List<ResourceLocation> getFilters() {
        return List.copyOf(filter);
    }

    /** Returns {@code true} when the sorter excludes configured fluids instead of matching them. */
    public boolean isBlacklist() {
        return blacklist;
    }

    /** Computes the sorter range based on the ranger upgrade stack count. */
    public int getRange() {
        int upgrades = inventory.getItem(SLOT_RANGER).getCount();
        if (upgrades <= 0) {
            return 1;
        }
        return Math.min(upgrades * 2, 14);
    }

    /** Returns {@code true} when the internal upgrade slot can accept another ranger item. */
    public boolean canAcceptRangerUpgrade() {
        return inventory.getItem(SLOT_RANGER).getCount() < getRangerMaxUpgrades();
    }

    /** Adds a single ranger upgrade to the slot and returns the updated stack size. */
    public int addRangerUpgrade() {
        ItemStack slot = inventory.getItem(SLOT_RANGER);
        if (slot.isEmpty()) {
            inventory.setItem(SLOT_RANGER, new ItemStack(FluidCowsRegistries.RANGER.get()));
        } else {
            slot.grow(1);
        }
        setChangedAndSync();
        return inventory.getItem(SLOT_RANGER).getCount();
    }

    /** Exposes the current number of ranger upgrades for chat feedback. */
    public int getRangerUpgradeCount() {
        return inventory.getItem(SLOT_RANGER).getCount();
    }

    /** Returns the ranger stack cap enforced by the item definition. */
    public int getRangerMaxUpgrades() {
        return FluidCowsRegistries.RANGER.get().getDefaultInstance().getMaxStackSize();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fluidcows.sorter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            final int id, final Inventory playerInventory, final Player player) {
        Level level = getLevel();
        return new SorterMenu(
                id,
                playerInventory,
                this,
                inventory,
                level != null ? ContainerLevelAccess.create(level, worldPosition) : ContainerLevelAccess.NULL);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final SorterBlockEntity sorter) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        sorter.moveMatchingCows(serverLevel);
    }

    public static void clientTick(
            final Level level, final BlockPos pos, final BlockState state, final SorterBlockEntity sorter) {
        // Client copies receive filter updates via block entity data packets.
    }

    private void moveMatchingCows(final ServerLevel level) {
        AABB bounds = FCUtils.getWorkArea(worldPosition, getFacing().getOpposite(), getRange());
        List<FluidCow> cows = level.getEntitiesOfClass(FluidCow.class, bounds, FluidCow::isBaby);
        if (cows.isEmpty()) {
            return;
        }

        Direction facing = getFacing();
        BlockPos targetPos = worldPosition.relative(facing);
        Vec3 target = Vec3.atCenterOf(targetPos);

        for (FluidCow cow : cows) {
            if (!matchesFilter(cow)) {
                continue;
            }
            cow.teleportTo(target.x, target.y, target.z);
        }
    }

    private boolean matchesFilter(final FluidCow cow) {
        Fluid fluid = cow.getFluid();
        if (fluid == null) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) {
            return false;
        }
        boolean contains = filter.contains(key);
        return blacklist ? !contains : contains;
    }

    /** Attempts to append the supplied fluid id to the filter list. */
    public boolean addFilter(final ResourceLocation key) {
        if (filter.size() >= 5 || filter.contains(key)) {
            return false;
        }
        if (!BuiltInRegistries.FLUID.containsKey(key)) {
            return false;
        }
        boolean added = filter.add(key);
        if (added) {
            setChangedAndSync();
        }
        return added;
    }

    /** Removes the filter entry at the supplied index. */
    public boolean removeFilter(final int index) {
        if (index < 0 || index >= filter.size()) {
            return false;
        }
        List<ResourceLocation> entries = new ArrayList<>(filter);
        ResourceLocation removed = entries.remove(index);
        if (removed == null) {
            return false;
        }
        filter.clear();
        filter.addAll(entries);
        setChangedAndSync();
        return true;
    }

    /** Toggles the blacklist state and synchronises the change to clients. */
    public void toggleMode() {
        blacklist = !blacklist;
        setChangedAndSync();
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(SorterBlock.FACING)) {
            return state.getValue(SorterBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
        blacklist = tag.getBoolean(TAG_BLACKLIST);
        filter.clear();
        ListTag list = tag.getList(TAG_FILTER, Tag.TAG_STRING);
        for (int i = 0; i < list.size() && filter.size() < 5; i++) {
            Optional.ofNullable(ResourceLocation.tryParse(list.getString(i)))
                    .filter(BuiltInRegistries.FLUID::containsKey)
                    .ifPresent(filter::add);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean(TAG_BLACKLIST, blacklist);
        ListTag list = new ListTag();
        for (ResourceLocation key : filter) {
            list.add(StringTag.valueOf(key.toString()));
        }
        tag.put(TAG_FILTER, list);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        filter.clear();
        blacklist = false;
    }

    private void setChangedAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
