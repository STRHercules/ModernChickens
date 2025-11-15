package com.setycz.chickens.menu;

import com.setycz.chickens.entity.Rooster;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Simple container menu for the rooster. It mirrors the legacy Hatchery GUI by
 * exposing a single seed slot alongside the player inventory.
 */
public class RoosterMenu extends AbstractContainerMenu {
    private static final int SEED_SLOT_INDEX = 0;

    private final Rooster rooster;
    private final Container inventory;
    private int clientSeeds;

    public RoosterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, (Rooster) null);
    }

    public RoosterMenu(int id, Inventory playerInventory, Rooster rooster) {
        super(ModMenuTypes.ROOSTER.get(), id);
        this.rooster = rooster;
        this.inventory = rooster != null ? rooster : new SimpleContainer(1);

        // Rooster seed slot
        this.addSlot(new SeedSlot(inventory, SEED_SLOT_INDEX, 25, 36));

        // Player inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 7 + column * 18, 83 + row * 18));
            }
        }
        // Hotbar
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 7 + hotbar * 18, 141));
        }

        // Sync the rooster's internal seed charge to the client so the GUI can
        // render a progress bar without directly querying entity data.
        this.addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return rooster != null ? rooster.getSeeds() : clientSeeds;
            }

            @Override
            public void set(int value) {
                clientSeeds = value;
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return rooster == null || rooster.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();

            if (index == SEED_SLOT_INDEX) {
                if (!this.moveItemStackTo(current, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, SEED_SLOT_INDEX, SEED_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, current);
        }
        return original;
    }

    public Rooster getRooster() {
        return rooster;
    }

    /**
     * Returns the scaled seed charge for GUI bars. The rooster currently caps
     * seed storage at 20 points, mirroring the legacy Hatchery behaviour.
     */
    public int getScaledSeeds(int scale) {
        if (clientSeeds <= 0) {
            return 0;
        }
        return clientSeeds * scale / 20;
    }

    private static class SeedSlot extends Slot {
        public SeedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Only accept items that chickens recognise as food (seeds, etc.).
            return !stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.CHICKEN_FOOD);
        }
    }
}

