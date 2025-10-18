package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.CollectorBlockEntity;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * 27-slot inventory that mirrors the original collector chest layout.
 */
public class CollectorMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SIZE = CollectorBlockEntity.INVENTORY_SIZE;
    private final CollectorBlockEntity collector;
    private final ContainerLevelAccess access;

    public CollectorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public CollectorMenu(int id, Inventory playerInventory, CollectorBlockEntity collector) {
        super(ModMenuTypes.COLLECTOR.get(), id);
        this.collector = collector;
        Level level = collector.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, collector.getBlockPos()) : ContainerLevelAccess.NULL;

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(collector, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }
    }

    private static CollectorBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CollectorBlockEntity collector) {
            return collector;
        }
        throw new IllegalStateException("Collector not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return collector.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < INVENTORY_SIZE) {
                if (!this.moveItemStackTo(current, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, INVENTORY_SIZE, false)) {
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
}
