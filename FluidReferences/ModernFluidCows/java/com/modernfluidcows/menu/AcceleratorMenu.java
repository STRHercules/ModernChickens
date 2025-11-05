package com.modernfluidcows.menu;

import com.modernfluidcows.blockentity.AcceleratorBlockEntity;
import com.modernfluidcows.registry.FluidCowsRegistries;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Menu wiring for the accelerator block entity.
 */
public class AcceleratorMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 3;
    private static final int SLOT_WHEAT = 0;
    private static final int SLOT_INPUT = 1;
    private static final int SLOT_OUTPUT = 2;

    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final AcceleratorBlockEntity accelerator;

    public AcceleratorMenu(
            final int id,
            final Inventory playerInventory,
            final AcceleratorBlockEntity accelerator,
            final Container container,
            final ContainerLevelAccess access,
            final ContainerData data) {
        super(FluidCowsRegistries.ACCELERATOR_MENU.get(), id);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.access = access;
        this.data = data;
        this.accelerator = accelerator;

        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, SLOT_WHEAT, 81, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return container.canPlaceItem(SLOT_WHEAT, stack);
            }
        });

        addSlot(new Slot(container, SLOT_INPUT, 33, 6) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return container.canPlaceItem(SLOT_INPUT, stack);
            }
        });

        addSlot(new Slot(container, SLOT_OUTPUT, 33, 64) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return false;
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 142));
        }

        addDataSlots(data);
    }

    public static AcceleratorMenu createFallback(final int id, final Inventory inventory) {
        return new AcceleratorMenu(
                id,
                inventory,
                null,
                new SimpleContainer(SLOT_COUNT),
                ContainerLevelAccess.NULL,
                new SimpleContainerData(2));
    }

    @Override
    public boolean stillValid(final Player player) {
        return stillValid(access, player, FluidCowsRegistries.ACCELERATOR_BLOCK.get());
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
            } else {
                if (container.canPlaceItem(SLOT_WHEAT, stack)) {
                    if (!moveItemStackTo(stack, SLOT_WHEAT, SLOT_WHEAT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (container.canPlaceItem(SLOT_INPUT, stack)) {
                    if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < SLOT_COUNT + 27) {
                    if (!moveItemStackTo(stack, SLOT_COUNT + 27, slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, SLOT_COUNT, SLOT_COUNT + 27, false)) {
                    return ItemStack.EMPTY;
                }
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

    public int getTankAmount() {
        return data.get(0);
    }

    public int getSubstance() {
        return data.get(1);
    }

    public int getScaledTankHeight(final int pixels) {
        int amount = getTankAmount();
        if (amount <= 0) {
            return 0;
        }
        return Mth.clamp((int) Math.ceil((double) amount * pixels / AcceleratorBlockEntity.TANK_CAPACITY), 0, pixels);
    }

    public Optional<FluidStack> getDisplayedFluidStack() {
        return Optional.ofNullable(accelerator)
                .map(AcceleratorBlockEntity::getTank)
                .map(FluidTank::getFluid)
                .filter(stack -> !stack.isEmpty())
                .map(FluidStack::copy);
    }

    public int getMaxSubstance() {
        return AcceleratorBlockEntity.MAX_SUBSTANCE;
    }
}
