package com.setycz.chickens.menu;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.blockentity.AvianChemicalConverterBlockEntity;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.GasEggItem;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

import java.util.Objects;

/**
 * Menu wiring for the chemical converter. Mirrors the fluid converter menu so
 * the GUI can reuse the same slot layout while synchronising the buffered
 * chemical information back to the client.
 */
public class AvianChemicalConverterMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SIZE = AvianChemicalConverterBlockEntity.SLOT_COUNT;

    private final AvianChemicalConverterBlockEntity converter;
    private final ContainerLevelAccess access;
    private int clientAmount;
    private int clientCapacity;
    private int clientEntryId = -1;
    private boolean clientGaseous;

    public AvianChemicalConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianChemicalConverterMenu(int id, Inventory playerInventory, AvianChemicalConverterBlockEntity converter) {
        super(ModMenuTypes.AVIAN_CHEMICAL_CONVERTER.get(), id);
        this.converter = converter;
        Level level = converter.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, converter.getBlockPos()) : ContainerLevelAccess.NULL;
        this.clientAmount = converter.getChemicalAmount();
        this.clientCapacity = converter.getTankCapacity();
        this.clientEntryId = converter.getStoredEntryId();
        this.clientGaseous = converter.isStoredGaseous();

        this.addSlot(new ChemicalEggSlot(converter, 0, 52, 34));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerAmount() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientAmount = (clientAmount & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerAmount() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientAmount = (clientAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerCapacity() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientCapacity = (clientCapacity & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerCapacity() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientCapacity = (clientCapacity & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerEntryId();
            }

            @Override
            public void set(int value) {
                clientEntryId = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return converter.isStoredGaseous() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                clientGaseous = value != 0;
            }
        });
    }

    private static AvianChemicalConverterBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AvianChemicalConverterBlockEntity converter) {
            return converter;
        }
        throw new IllegalStateException("Avian Chemical Converter not found at " + pos);
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

    public int getChemicalAmount() {
        return isServerSide() ? converter.getChemicalAmount() : clientAmount;
    }

    public int getCapacity() {
        return isServerSide() ? converter.getTankCapacity() : clientCapacity;
    }

    @Nullable
    public ChemicalEggRegistryItem getStoredEntry() {
        if (isServerSide()) {
            return converter.getStoredEntry();
        }
        if (clientEntryId < 0) {
            return null;
        }
        ChemicalEggRegistryItem entry = clientGaseous ? GasEggRegistry.findById(clientEntryId)
                : ChemicalEggRegistry.findById(clientEntryId);
        if (entry == null) {
            entry = ChemicalEggRegistry.findById(clientEntryId);
        }
        if (entry == null && clientGaseous) {
            entry = GasEggRegistry.findById(clientEntryId);
        }
        return entry;
    }

    private boolean isServerSide() {
        return converter != null && converter.getLevel() != null && !converter.getLevel().isClientSide;
    }

    private int getServerAmount() {
        return converter != null ? converter.getChemicalAmount() : 0;
    }

    private int getServerCapacity() {
        return converter != null ? converter.getTankCapacity() : 0;
    }

    private int getServerEntryId() {
        return converter != null ? converter.getStoredEntryId() : -1;
    }

    private static class ChemicalEggSlot extends Slot {
        private final AvianChemicalConverterBlockEntity converter;

        private ChemicalEggSlot(AvianChemicalConverterBlockEntity converter, int index, int x, int y) {
            super(converter, index, x, y);
            this.converter = converter;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ChemicalEggItem || stack.getItem() instanceof GasEggItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            converter.setChanged();
        }
    }
}
