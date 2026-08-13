package strhercules.chickens.menu;

import strhercules.chickens.blockentity.AbstractChickenContainerBlockEntity;
import strhercules.chickens.blockentity.MechanicalRoostBlockEntity;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/** Container layout for the four-row Mechanical Roost GUI. */
public class MechanicalRoostMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;
    private final MechanicalRoostBlockEntity roost;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlotCount;
    private int clientEnergy;
    private int clientCapacity;

    public MechanicalRoostMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public MechanicalRoostMenu(int id, Inventory playerInventory, MechanicalRoostBlockEntity roost) {
        this(id, playerInventory, roost, roost.getDataAccess());
    }

    public MechanicalRoostMenu(int id, Inventory playerInventory, MechanicalRoostBlockEntity roost,
            ContainerData data) {
        super(ModMenuTypes.MECHANICAL_ROOST.get(), id);
        this.roost = roost;
        this.data = data;
        this.machineSlotCount = roost.getContainerSize();
        this.clientEnergy = roost.getEnergyStored();
        this.clientCapacity = roost.getEnergyCapacity();
        Level level = roost.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, roost.getBlockPos())
                : ContainerLevelAccess.NULL;

        for (int row = 0; row < MechanicalRoostBlockEntity.CHICKEN_SLOT_COUNT; row++) {
            int chickenSlot = row;
            int rowY = 20 + row * 20;
            this.addSlot(new ChickenSlot(roost, chickenSlot, 26, rowY));
            for (int column = 0; column < 4; column++) {
                int outputSlot = MechanicalRoostBlockEntity.CHICKEN_SLOT_COUNT + row * 4 + column;
                this.addSlot(new OutputSlot(roost, outputSlot, 80 + column * 18, rowY));
            }
        }
        this.addSlot(new MachineUpgradeSlot(roost, MechanicalRoostBlockEntity.SPEED_UPGRADE_SLOT, 128, 101));
        this.addSlot(new MachineUpgradeSlot(roost, MechanicalRoostBlockEntity.RF_UPGRADE_SLOT, 149, 101));

        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < PLAYER_COLUMNS; column++) {
                this.addSlot(new Slot(playerInventory, column + row * PLAYER_COLUMNS + PLAYER_COLUMNS,
                        8 + column * 18, 120 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < PLAYER_COLUMNS; hotbar++) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 180));
        }

        this.addDataSlots(data);
        addEnergyDataSlots();
    }

    private void addEnergyDataSlots() {
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

    private static MechanicalRoostBlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MechanicalRoostBlockEntity roost) {
            return roost;
        }
        throw new IllegalStateException("Mechanical Roost not found at " + pos);
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
            if (index < machineSlotCount) {
                if (!this.moveItemStackTo(current, machineSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, machineSlotCount, false)) {
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
        int progress = 0;
        for (int row = 0; row < MechanicalRoostBlockEntity.CHICKEN_SLOT_COUNT; row++) {
            progress = Math.max(progress, getProgress(row));
        }
        return progress;
    }

    public int getProgress(int row) {
        return row >= 0 && row < MechanicalRoostBlockEntity.CHICKEN_SLOT_COUNT ? data.get(row) : 0;
    }

    public int getEnergy() {
        return isServerSide() ? getServerEnergy() : clientEnergy;
    }

    public int getCapacity() {
        return isServerSide() ? getServerCapacity() : clientCapacity;
    }

    public int getEnergyCostPerOperation() {
        return roost.getEnergyCostPerOperation();
    }

    private boolean isServerSide() {
        return roost.getLevel() != null && !roost.getLevel().isClientSide;
    }

    private int getServerEnergy() {
        return roost.getEnergyStored();
    }

    private int getServerCapacity() {
        return roost.getEnergyCapacity();
    }

    private static final class ChickenSlot extends Slot {
        private final MechanicalRoostBlockEntity roost;

        private ChickenSlot(MechanicalRoostBlockEntity roost, int index, int x, int y) {
            super(roost, index, x, y);
            this.roost = roost;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ChickenItemHelper.isChicken(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return roost.getMaxStackSizeForSlot(getContainerSlot(), stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(Math.min(amount,
                    AbstractChickenContainerBlockEntity.getLegalExternalStackSize(getItem())));
        }
    }

    private static final class OutputSlot extends Slot {
        private final MechanicalRoostBlockEntity roost;

        private OutputSlot(MechanicalRoostBlockEntity roost, int index, int x, int y) {
            super(roost, index, x, y);
            this.roost = roost;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return roost.getMaxStackSizeForSlot(getContainerSlot(), stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(Math.min(amount,
                    AbstractChickenContainerBlockEntity.getLegalExternalStackSize(getItem())));
        }
    }
}
