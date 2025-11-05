package com.modernfluidcows.menu;

import com.modernfluidcows.blockentity.SorterBlockEntity;
import com.modernfluidcows.entity.FluidCow;
import com.modernfluidcows.item.CowDisplayerItem;
import com.modernfluidcows.registry.FluidCowsRegistries;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/** Menu wiring for the sorter block entity. */
public class SorterMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 1;
    private static final int SLOT_RANGER = 0;

    public static final int BUTTON_ADD_FLUID = 0;
    public static final int BUTTON_TOGGLE_BLACKLIST = 1;
    public static final int BUTTON_REMOVE_BASE = 10;

    private final Container container;
    private final ContainerLevelAccess access;
    @Nullable private final SorterBlockEntity sorter;

    public SorterMenu(
            final int id,
            final Inventory playerInventory,
            @Nullable final SorterBlockEntity sorter,
            final Container container,
            final ContainerLevelAccess access) {
        super(FluidCowsRegistries.SORTER_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        this.container = container;
        this.access = access;
        this.sorter = sorter;

        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, SLOT_RANGER, 152, 33) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return container.canPlaceItem(SLOT_RANGER, stack);
            }
        });

        // Player inventory rows
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 58 + row * 18));
            }
        }

        // Hotbar slots
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 116));
        }
    }

    public static SorterMenu createFallback(final int id, final Inventory inventory) {
        return new SorterMenu(id, inventory, null, new SimpleContainer(SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    @Override
    public boolean stillValid(final Player player) {
        return stillValid(access, player, FluidCowsRegistries.SORTER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();

            if (index < SLOT_COUNT) {
                if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (container.canPlaceItem(SLOT_RANGER, stack)) {
                if (!moveItemStackTo(stack, SLOT_RANGER, SLOT_RANGER + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < SLOT_COUNT + 27) {
                if (!moveItemStackTo(stack, SLOT_COUNT + 27, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, SLOT_COUNT, SLOT_COUNT + 27, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (sorter != null) {
            if (id == BUTTON_ADD_FLUID) {
                return tryAddFluidFromCarried(player);
            }
            if (id == BUTTON_TOGGLE_BLACKLIST) {
                sorter.toggleMode();
                return true;
            }
            if (id >= BUTTON_REMOVE_BASE) {
                int index = id - BUTTON_REMOVE_BASE;
                return sorter.removeFilter(index);
            }
        }
        return false;
    }

    private boolean tryAddFluidFromCarried(final Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return false;
        }

        ResourceLocation key = resolveFluidKey(carried);
        if (key == null) {
            return false;
        }
        return sorter.addFilter(key);
    }

    @Nullable
    private static ResourceLocation resolveFluidKey(final ItemStack stack) {
        Optional<FluidStack> contained = FluidUtil.getFluidContained(stack);
        if (contained.isPresent() && !contained.get().isEmpty()) {
            return contained.get().getFluid().builtInRegistryHolder().key().location();
        }

        if (stack.getItem() instanceof CowDisplayerItem) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                if (tag.contains("fluid")) {
                    return ResourceLocation.tryParse(tag.getString("fluid"));
                }
            }
            return null;
        }

        if (stack.getItem() == FluidCowsRegistries.COW_HALTER.get()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                if (tag.contains(FluidCow.HALTER_TAG_FLUID)) {
                    return ResourceLocation.tryParse(tag.getString(FluidCow.HALTER_TAG_FLUID));
                }
            }
        }

        return null;
    }

    /** Exposes the configured filter list for UI rendering. */
    public List<ResourceLocation> getFilters() {
        return sorter != null ? sorter.getFilters() : List.of();
    }

    /** Returns whether the sorter currently excludes configured fluids. */
    public boolean isBlacklist() {
        return sorter != null && sorter.isBlacklist();
    }

    /** Mirrors the block entity range for tooltip display. */
    public int getRange() {
        return sorter != null ? sorter.getRange() : 1;
    }
}
