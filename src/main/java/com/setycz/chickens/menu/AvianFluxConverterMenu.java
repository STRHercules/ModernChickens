package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.AvianFluxConverterBlockEntity;
import com.setycz.chickens.item.FluxEggItem;
import com.setycz.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
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
    private int clientEnergy;
    private int clientCapacity;

    public AvianFluxConverterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public AvianFluxConverterMenu(int id, Inventory playerInventory, AvianFluxConverterBlockEntity converter) {
        super(ModMenuTypes.AVIAN_FLUX_CONVERTER.get(), id);
        this.converter = converter;
        Level level = converter.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, converter.getBlockPos()) : ContainerLevelAccess.NULL;
        this.clientEnergy = converter != null ? converter.getEnergyStored() : 0;
        this.clientCapacity = converter != null ? converter.getEnergyCapacity() : 0;

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

        // Split the 32-bit energy and capacity values across vanilla DataSlots so they
        // synchronise to the client without truncation.
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerEnergy() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergy = (clientEnergy & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerEnergy() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergy = (clientEnergy & 0x0000FFFF) | ((value & 0xFFFF) << 16);
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
        // Server reads straight from the block entity while the client consumes the
        // values hydrated through the DataSlot callbacks above.
        return isServerSide() ? getServerEnergy() : clientEnergy;
    }

    public int getCapacity() {
        return isServerSide() ? getServerCapacity() : clientCapacity;
    }

    private boolean isServerSide() {
        return converter != null && converter.getLevel() != null && !converter.getLevel().isClientSide;
    }

    private int getServerEnergy() {
        return converter != null ? converter.getEnergyStored() : 0;
    }

    private int getServerCapacity() {
        return converter != null ? converter.getEnergyCapacity() : 0;
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
