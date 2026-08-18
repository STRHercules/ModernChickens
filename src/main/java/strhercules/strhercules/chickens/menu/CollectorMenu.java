package strhercules.chickens.menu;

import strhercules.chickens.blockentity.AbstractChickenContainerBlockEntity;
import strhercules.chickens.blockentity.CollectorBlockEntity;
import strhercules.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/** Collector menu with 13-column storage and four upgrade slots. */
public class CollectorMenu extends AbstractContainerMenu implements SideConfigMenu {
    private static final int STORAGE_SLOT_COUNT = CollectorBlockEntity.MAX_STORAGE_SLOTS;
    private final CollectorBlockEntity collector;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlotCount;
    private int lastCapacityLevel = -1;

    public CollectorMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public CollectorMenu(int id, Inventory playerInventory, CollectorBlockEntity collector) {
        this(id, playerInventory, collector, collector.getDataAccess());
    }

    public CollectorMenu(int id, Inventory playerInventory, CollectorBlockEntity collector, ContainerData data) {
        super(ModMenuTypes.COLLECTOR.get(), id);
        this.collector = collector;
        this.data = data;
        // Keep the menu shape stable while a capacity upgrade is inserted. Locked
        // slots become active immediately, just like Mega Chicken cargo slots.
        this.machineSlotCount = STORAGE_SLOT_COUNT + collector.getUpgradeSlotCount();
        Level level = collector.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, collector.getBlockPos()) : ContainerLevelAccess.NULL;

        for (int row = 0; row < 8; ++row) {
            for (int column = 0; column < 13; ++column) {
                int slot = column + row * 13;
                this.addSlot(new StorageSlot(this, collector, slot, 0, 0));
            }
        }
        this.addSlot(new MachineUpgradeSlot(collector, CollectorBlockEntity.STACK_UPGRADE_SLOT, 163, 6));
        this.addSlot(new MachineUpgradeSlot(collector, CollectorBlockEntity.CAPACITY_UPGRADE_SLOT, 184, 6));
        this.addSlot(new MachineUpgradeSlot(collector, CollectorBlockEntity.SPEED_UPGRADE_SLOT, 205, 6));
        this.addSlot(new MachineUpgradeSlot(collector, CollectorBlockEntity.RANGE_UPGRADE_SLOT, 226, 6));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 46 + column * 18, 174 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 46 + hotbar * 18, 232));
        }
        this.addDataSlots(data);
        updateStorageLayout();
    }

    private static CollectorBlockEntity resolveBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
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
    public void setData(int id, int value) {
        int previousCapacityLevel = getCapacityLevel();
        super.setData(id, value);
        if (id == 1 && previousCapacityLevel != getCapacityLevel()) {
            updateStorageLayout();
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        updateStorageLayout();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            if (index < machineSlotCount && !slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
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

    @Override
    public CollectorBlockEntity getSideConfigurable() {
        return collector;
    }

    public int getCapacityLevel() {
        return Math.max(0, Math.min(2, data.getCount() > 1 ? data.get(1) : 0));
    }

    private int getStorageRows() {
        return 2 + getCapacityLevel() * 3;
    }

    private int getActiveStorageSlotCount() {
        return CollectorBlockEntity.BASE_STORAGE_SLOTS
                + getCapacityLevel() * CollectorBlockEntity.SLOTS_PER_CAPACITY_UPGRADE;
    }

    private void updateStorageLayout() {
        int capacityLevel = getCapacityLevel();
        if (capacityLevel == lastCapacityLevel) {
            return;
        }
        int activeRows = getStorageRows();
        int activeRowOffset = 8 - activeRows;
        for (int row = 0; row < 8; ++row) {
            // Keep logical storage order aligned with the visible grid. The
            // first unlocked slot is always the top-left slot, while inactive
            // rows remain above the active area and outside the texture.
            int visualRow = row < activeRows ? activeRowOffset + row : row - activeRows;
            for (int column = 0; column < 13; ++column) {
                int slotIndex = column + row * 13;
                StorageSlot storageSlot = new StorageSlot(this, collector, slotIndex,
                        10 + column * 18, 27 + visualRow * 18);
                // AbstractContainerMenu.addSlot normally assigns this field.
                // These slots are rebuilt in-place when capacity changes, so
                // preserve the menu index used by vanilla drag operations.
                storageSlot.index = slotIndex;
                this.slots.set(slotIndex, storageSlot);
            }
        }
        lastCapacityLevel = capacityLevel;
    }

    private static final class StorageSlot extends Slot {
        private final CollectorMenu menu;
        private final CollectorBlockEntity collector;

        private StorageSlot(CollectorMenu menu, CollectorBlockEntity collector, int index, int x, int y) {
            super(collector, index, x, y);
            this.menu = menu;
            this.collector = collector;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return collector.getMaxStackSizeForSlot(getContainerSlot(), stack);
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < menu.getActiveStorageSlotCount();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && !collector.isUpgradeItem(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack stack = getItem();
            return super.remove(Math.min(amount,
                    AbstractChickenContainerBlockEntity.getLegalExternalStackSize(stack)));
        }
    }
}
