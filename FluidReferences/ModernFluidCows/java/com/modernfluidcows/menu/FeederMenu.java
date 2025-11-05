package com.modernfluidcows.menu;

import com.modernfluidcows.blockentity.FeederBlockEntity;
import com.modernfluidcows.registry.FluidCowsRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Menu wiring for the feeder block entity. */
public class FeederMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 2;
    private static final int SLOT_WHEAT = 0;
    private static final int SLOT_RANGER = 1;

    private final Container container;
    private final ContainerLevelAccess access;
    @Nullable private final FeederBlockEntity feeder;

    public FeederMenu(
            final int id,
            final Inventory playerInventory,
            @Nullable final FeederBlockEntity feeder,
            final Container container,
            final ContainerLevelAccess access) {
        super(FluidCowsRegistries.FEEDER_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        this.container = container;
        this.access = access;
        this.feeder = feeder;

        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, SLOT_WHEAT, 80, 21) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return container.canPlaceItem(SLOT_WHEAT, stack);
            }
        });

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

    public static FeederMenu createFallback(final int id, final Inventory inventory) {
        return new FeederMenu(
                id, inventory, null, new SimpleContainer(SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    @Override
    public boolean stillValid(final Player player) {
        return stillValid(access, player, FluidCowsRegistries.FEEDER_BLOCK.get());
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
            } else if (container.canPlaceItem(SLOT_WHEAT, stack)) {
                if (!moveItemStackTo(stack, SLOT_WHEAT, SLOT_WHEAT + 1, false)) {
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

    /** Exposes the configured range for the screen tooltip. */
    public int getRange() {
        if (feeder != null) {
            return feeder.getRange();
        }
        return FeederBlockEntity.calculateRange(container.getItem(SLOT_RANGER).getCount());
    }
}
