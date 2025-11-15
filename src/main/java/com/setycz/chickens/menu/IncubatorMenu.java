package com.setycz.chickens.menu;

import com.setycz.chickens.blockentity.IncubatorBlockEntity;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.registry.ModMenuTypes;
import com.setycz.chickens.registry.ModRegistry;
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

import java.util.Objects;

/**
 * Container menu for the Incubator. Synchronises the block entity's energy,
 * progress, and per-egg RF cost back to the client so the GUI can render live
 * gauges while exposing standard furnace-style slots for automation.
 */
public class IncubatorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = IncubatorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;
    private static final int PLAYER_SLOT_COUNT = PLAYER_INVENTORY_ROWS * PLAYER_COLUMNS;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + PLAYER_SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + PLAYER_COLUMNS;

    private final IncubatorBlockEntity incubator;
    private final ContainerLevelAccess access;
    private int clientEnergy;
    private int clientCapacity;
    private int clientProgress;
    private int clientEnergyCost;

    public IncubatorMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public IncubatorMenu(int id, Inventory playerInventory, IncubatorBlockEntity incubator) {
        super(ModMenuTypes.INCUBATOR.get(), id);
        this.incubator = incubator;
        Level level = incubator.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, incubator.getBlockPos()) : ContainerLevelAccess.NULL;
        this.clientEnergy = incubator.getEnergyStored();
        this.clientCapacity = incubator.getEnergyCapacity();
        this.clientProgress = incubator.getProgress();
        this.clientEnergyCost = incubator.getEnergyCost();

        this.addSlot(new SpawnEggSlot(incubator, 0, 46, 35));
        this.addSlot(new OutputSlot(incubator, 1, 100, 35));

        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < PLAYER_COLUMNS; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * PLAYER_COLUMNS + PLAYER_COLUMNS,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < PLAYER_COLUMNS; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }

        // Energy stored
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

        // Energy capacity
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

        // Progress
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerProgress() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientProgress = (clientProgress & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerProgress() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientProgress = (clientProgress & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });

        // Energy cost per egg
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getServerEnergyCost() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergyCost = (clientEnergyCost & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (getServerEnergyCost() >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                clientEnergyCost = (clientEnergyCost & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });
    }

    private static IncubatorBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IncubatorBlockEntity incubator) {
            return incubator;
        }
        throw new IllegalStateException("Incubator not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.INCUBATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(current, MACHINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (SpawnEggSlot.isSpawnEgg(current)) {
                if (!this.moveItemStackTo(current, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_END) {
                if (!this.moveItemStackTo(current, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, PLAYER_START, PLAYER_END, false)) {
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
        return isServerSide() ? getServerEnergy() : clientEnergy;
    }

    public int getCapacity() {
        return isServerSide() ? getServerCapacity() : clientCapacity;
    }

    public int getProgress() {
        return isServerSide() ? getServerProgress() : clientProgress;
    }

    public int getMaxProgress() {
        return incubator.getMaxProgress();
    }

    public int getEnergyCost() {
        return isServerSide() ? getServerEnergyCost() : clientEnergyCost;
    }

    private boolean isServerSide() {
        return incubator.getLevel() != null && !incubator.getLevel().isClientSide;
    }

    private int getServerEnergy() {
        return incubator != null ? incubator.getEnergyStored() : 0;
    }

    private int getServerCapacity() {
        return incubator != null ? incubator.getEnergyCapacity() : 0;
    }

    private int getServerProgress() {
        return incubator != null ? incubator.getProgress() : 0;
    }

    private int getServerEnergyCost() {
        return incubator != null ? incubator.getEnergyCost() : 0;
    }

    private static class SpawnEggSlot extends Slot {
        public SpawnEggSlot(IncubatorBlockEntity incubator, int index, int x, int y) {
            super(incubator, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isSpawnEgg(stack);
        }

        static boolean isSpawnEgg(ItemStack stack) {
            return stack.getItem() instanceof ChickensSpawnEggItem;
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(IncubatorBlockEntity incubator, int index, int x, int y) {
            super(incubator, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
