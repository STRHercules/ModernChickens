package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.RoostBlockEntity;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Menu wiring for the roost. It mirrors the slot layout of the 1.12 GUI while
 * syncing the drop progress to the client.
 */
public class RoostMenu extends AbstractContainerMenu {
    private final RoostBlockEntity roost;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public RoostMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public RoostMenu(int id, Inventory playerInventory, RoostBlockEntity roost) {
        this(id, playerInventory, roost, roost.getDataAccess());
    }

    public RoostMenu(int id, Inventory playerInventory, RoostBlockEntity roost, ContainerData data) {
        super(ModMenuTypes.ROOST.get(), id);
        this.roost = roost;
        this.data = data;
        Level level = roost.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, roost.getBlockPos())
                : ContainerLevelAccess.NULL;

        this.addSlot(new ChickenSlot(roost, RoostBlockEntity.CHICKEN_SLOT, 26, 20));
        for (int i = 0; i < RoostBlockEntity.INVENTORY_SIZE - 1; i++) {
            this.addSlot(new OutputSlot(roost, i + 1, 80 + i * 18, 20));
        }

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 109));
        }

        this.addDataSlots(data);
    }

    private static RoostBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RoostBlockEntity roost) {
            return roost;
        }
        throw new IllegalStateException("Roost not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return roost.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < RoostBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(current, RoostBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, RoostBlockEntity.INVENTORY_SIZE, false)) {
                return ItemStack.EMPTY;
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

    public ContainerLevelAccess getAccess() {
        return access;
    }

    public int getProgress() {
        return data.get(0);
    }

    private static class ChickenSlot extends Slot {
        public ChickenSlot(RoostBlockEntity roost, int index, int x, int y) {
            super(roost, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ChickenItemHelper.isChicken(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 16;
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(RoostBlockEntity roost, int index, int x, int y) {
            super(roost, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
