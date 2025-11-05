package com.modernfluidcows.blockentity;

import com.modernfluidcows.block.FeederBlock;
import com.modernfluidcows.config.FCConfig;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.menu.FeederMenu;
import com.modernfluidcows.registry.FluidCowsRegistries;
import com.modernfluidcows.util.FCUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity that consumes wheat to breed nearby {@link FluidCow}s.
 */
public class FeederBlockEntity extends BlockEntity implements Clearable, MenuProvider {
    private static final String TAG_ITEMS = "Items";

    public static final int SLOT_WHEAT = 0;
    public static final int SLOT_RANGER = 1;
    private static final int SLOT_COUNT = 2;

    private final SimpleContainer inventory =
            new SimpleContainer(SLOT_COUNT) {
                @Override
                public boolean canPlaceItem(final int slot, final ItemStack stack) {
                    if (slot == SLOT_WHEAT) {
                        return stack.is(Items.WHEAT);
                    }
                    if (slot == SLOT_RANGER) {
                        return stack.is(FluidCowsRegistries.RANGER.get());
                    }
                    return false;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setChangedAndSync();
                }
            };

    public FeederBlockEntity(final BlockPos pos, final BlockState state) {
        super(FluidCowsRegistries.FEEDER_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Returns the configured breeding range based on the ranger upgrade stack. */
    public int getRange() {
        return calculateRange(inventory.getItem(SLOT_RANGER).getCount());
    }

    /** Returns {@code true} when another ranger upgrade item can be inserted. */
    public boolean canAcceptRangerUpgrade() {
        return inventory.getItem(SLOT_RANGER).getCount() < getRangerMaxUpgrades();
    }

    /** Adds a single ranger upgrade item to the dedicated slot and returns the new stack size. */
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

    /** Exposes the current ranger upgrade stack size for tooltips and chat feedback. */
    public int getRangerUpgradeCount() {
        return inventory.getItem(SLOT_RANGER).getCount();
    }

    /** Returns the maximum number of ranger upgrades supported by the slot. */
    public int getRangerMaxUpgrades() {
        return FluidCowsRegistries.RANGER.get().getDefaultInstance().getMaxStackSize();
    }

    /** Mirrors the legacy range calculation so ranger upgrades expand the work area. */
    public static int calculateRange(final int upgradeCount) {
        if (upgradeCount <= 0) {
            return 1;
        }
        return Math.min(upgradeCount * 2, 14);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fluidcows.feeder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            final int id, final Inventory playerInventory, final Player player) {
        Level level = getLevel();
        return new FeederMenu(
                id,
                playerInventory,
                this,
                inventory,
                level != null
                        ? ContainerLevelAccess.create(level, worldPosition)
                        : ContainerLevelAccess.NULL);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final FeederBlockEntity feeder) {
        feeder.breedNearbyCows(level);
    }

    public static void clientTick(
            final Level level, final BlockPos pos, final BlockState state, final FeederBlockEntity feeder) {
        // Inventory state is synchronised through vanilla container updates; nothing to do client-side.
    }

    private void breedNearbyCows(final Level level) {
        ItemStack wheat = inventory.getItem(SLOT_WHEAT);
        if (wheat.getCount() < 2) {
            return;
        }

        AABB bounds = FCUtils.getWorkArea(worldPosition, getFacing(), getRange());
        List<FluidCow> cows = level.getEntitiesOfClass(FluidCow.class, bounds, this::canFeed);
        if (cows.size() < 2) {
            return;
        }

        boolean consumed = false;
        for (int i = 0; i < cows.size() - 1 && inventory.getItem(SLOT_WHEAT).getCount() >= 2; i++) {
            FluidCow first = cows.get(i);
            for (int j = i + 1; j < cows.size() && inventory.getItem(SLOT_WHEAT).getCount() >= 2; j++) {
                FluidCow second = cows.get(j);
                if (!canMate(first, second)) {
                    continue;
                }
                inventory.removeItem(SLOT_WHEAT, 2);
                first.setInLove(null);
                second.setInLove(null);
                consumed = true;
            }
        }

        if (consumed) {
            setChangedAndSync();
        }
    }

    private boolean canFeed(final FluidCow cow) {
        if (cow.isBaby() || cow.getAge() != 0 || cow.isInLove()) {
            return false;
        }
        Fluid fluid = cow.getFluid();
        if (fluid == null) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        return key != null && !FCConfig.feederBlackList.contains(key.toString());
    }

    private boolean canMate(final FluidCow first, final FluidCow second) {
        Fluid left = first.getFluid();
        Fluid right = second.getFluid();
        if (left == null || right == null) {
            return false;
        }
        return FCConfig.canMateWith(left, right) && !first.isInLove() && !second.isInLove();
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(FeederBlock.FACING)) {
            return state.getValue(FeederBlock.FACING);
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
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, provider);
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
