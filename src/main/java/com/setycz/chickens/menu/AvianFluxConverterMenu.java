package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.item.FluxEggItem;
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
 * Menu for the Avian Flux Converter. The layout mirrors the furnace-style GUI
 * with a single input slot followed by the player inventory, while syncing the
 * machine's energy buffer back to the screen for tooltip rendering.
 */
public class AvianFluxConverterMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SIZE = AvianFluxConverterBlockEntity.SLOT_COUNT;

    private final AvianFluxConverterBlockEntity converter;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AvianFluxConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianFluxConverterMenu(int id, Inventory playerInventory, AvianFluxConverterBlockEntity converter) {
        this(id, playerInventory, converter, converter.getDataAccess());
    }

    public AvianFluxConverterMenu(int id, Inventory playerInventory, AvianFluxConverterBlockEntity converter, ContainerData data) {
        super(ModMenuTypes.AVIAN_FLUX_CONVERTER.get(), id);
        this.converter = converter;
        this.data = data;
        Level level = converter.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, converter.getBlockPos()) : ContainerLevelAccess.NULL;

        this.addSlot(new FluxEggSlot(converter, 0, 80, 35));

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

    private static AvianFluxConverterBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianFluxConverterBlockEntity converter) {
            return converter;
        }
        throw new IllegalStateException("Avian Flux Converter not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return converter.stillValid(player);
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

    public int getEnergy() {
        return data.get(0);
    }

    public int getCapacity() {
        return data.get(1);
    }

    private static class FluxEggSlot extends Slot {
        public FluxEggSlot(AvianFluxConverterBlockEntity converter, int index, int x, int y) {
            super(converter, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof FluxEggItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
