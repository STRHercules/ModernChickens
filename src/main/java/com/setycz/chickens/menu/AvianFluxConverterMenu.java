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
    private final ContainerData sourceData;
    private final ContainerData syncedData;

    public AvianFluxConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianFluxConverterMenu(int id, Inventory playerInventory, AvianFluxConverterBlockEntity converter) {
        this(id, playerInventory, converter, converter.getDataAccess());
    }

    public AvianFluxConverterMenu(int id, Inventory playerInventory, AvianFluxConverterBlockEntity converter, ContainerData data) {
        super(ModMenuTypes.AVIAN_FLUX_CONVERTER.get(), id);
        this.converter = converter;
        this.sourceData = data;
        this.syncedData = createClientMirror(data.getCount());
        Level level = converter.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, converter.getBlockPos()) : ContainerLevelAccess.NULL;

        // Align the slot with the dedicated socket in fluxconverter.png (49,31 to 73,55).
        this.addSlot(new FluxEggSlot(converter, 0, 52, 34));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        if (playerInventory.player != null && !playerInventory.player.level().isClientSide) {
            // Prime the mirrored data slots on the server so the initial GUI frame already
            // reflects the converter's stored RF before vanilla begins diffing the values.
            refreshSyncedData();
        }

        this.addDataSlots(syncedData);
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
        // Combine the low/high shorts mirrored by the block entity so the GUI
        // reads the full 32-bit RF total rather than the truncated container data.
        return combineData(0, 1);
    }

    public int getCapacity() {
        return combineData(2, 3);
    }

    private int combineData(int lowIndex, int highIndex) {
        int low = syncedData.get(lowIndex) & 0xFFFF;
        int high = syncedData.get(highIndex) & 0xFFFF;
        return (high << 16) | low;
    }

    @Override
    public void broadcastChanges() {
        if (converter != null && converter.getLevel() != null && !converter.getLevel().isClientSide) {
            // Copy the latest energy/capacity shorts from the block entity into the
            // mirrored ContainerData before vanilla compares values for syncing.
            refreshSyncedData();
        }
        super.broadcastChanges();
    }

    private void refreshSyncedData() {
        int count = Math.min(sourceData.getCount(), syncedData.getCount());
        for (int index = 0; index < count; index++) {
            int value = sourceData.get(index) & 0xFFFF;
            if (syncedData.get(index) != value) {
                syncedData.set(index, value);
            }
        }
    }

    private static ContainerData createClientMirror(int size) {
        return new ContainerData() {
            private final int[] values = new int[size];

            @Override
            public int get(int index) {
                return index >= 0 && index < values.length ? values[index] : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < values.length) {
                    values[index] = value & 0xFFFF;
                }
            }

            @Override
            public int getCount() {
                return values.length;
            }
        };
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
