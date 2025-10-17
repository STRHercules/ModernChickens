package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.HenhouseBlockEntity;
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
 * Container menu mirroring the slot layout and energy sync from the 1.10 henhouse.
 * It wires the hay/dirt slots plus the 3x3 output grid and keeps the vanilla
 * quick-move rules so automation behaves exactly like before.
 */
public class HenhouseMenu extends AbstractContainerMenu {
    private final HenhouseBlockEntity henhouse;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public HenhouseMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public HenhouseMenu(int id, Inventory playerInventory, HenhouseBlockEntity henhouse) {
        this(id, playerInventory, henhouse, henhouse.getDataAccess());
    }

    public HenhouseMenu(int id, Inventory playerInventory, HenhouseBlockEntity henhouse, ContainerData data) {
        super(ModMenuTypes.HENHOUSE.get(), id);
        this.henhouse = henhouse;
        this.data = data;
        Level blockLevel = henhouse.getLevel();
        this.access = blockLevel != null ? ContainerLevelAccess.create(blockLevel, henhouse.getBlockPos())
                : ContainerLevelAccess.NULL;

        // Henhouse internals: hay bale fuel, dirt output, and 3x3 storage for eggs.
        this.addSlot(new Slot(henhouse, HenhouseBlockEntity.HAY_SLOT, 25, 19));
        this.addSlot(new Slot(henhouse, HenhouseBlockEntity.DIRT_SLOT, 25, 55));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = HenhouseBlockEntity.FIRST_OUTPUT_SLOT + row * 3 + column;
                this.addSlot(new Slot(henhouse, index, 98 + column * 18, 17 + row * 18));
            }
        }

        // Player inventory mirroring the legacy GUI coordinates.
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        this.addDataSlots(data);
    }

    private static HenhouseBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HenhouseBlockEntity henhouse) {
            return henhouse;
        }
        throw new IllegalStateException("Henhouse not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return henhouse.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < HenhouseBlockEntity.SLOT_COUNT) {
                // Shift-clicking internal slots moves items into the player inventory.
                if (!this.moveItemStackTo(current, HenhouseBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, HenhouseBlockEntity.SLOT_COUNT, false)) {
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

    public int getEnergy() {
        // Exposed for the client screen so it can render the hay bale progress bar.
        return data.get(0);
    }
}
