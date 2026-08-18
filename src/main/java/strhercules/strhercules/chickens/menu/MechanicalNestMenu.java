package strhercules.chickens.menu;

import strhercules.chickens.blockentity.MechanicalNestBlockEntity;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

public final class MechanicalNestMenu extends AbstractContainerMenu implements SideConfigMenu {
    private static final int MACHINE_SLOTS = MechanicalNestBlockEntity.INVENTORY_SIZE;
    private final MechanicalNestBlockEntity nest;
    private final ContainerLevelAccess access;
    private int clientEnergy;
    private int clientCapacity;
    private int clientCost;

    public MechanicalNestMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer));
    }

    public MechanicalNestMenu(int id, Inventory inventory, MechanicalNestBlockEntity nest) {
        super(ModMenuTypes.MECHANICAL_NEST.get(), id);
        this.nest = nest;
        this.access = nest.getLevel() == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(nest.getLevel(), nest.getBlockPos());
        clientEnergy = nest.getEnergyStored();
        clientCapacity = nest.getEnergyCapacity();
        clientCost = nest.getEnergyCost();

        this.addSlot(new RoosterSlot(nest, 0, 80, 26));
        this.addSlot(new UpgradeSlot(nest, MechanicalNestBlockEntity.SPEED_UPGRADE_SLOT, 136, 6));
        this.addSlot(new UpgradeSlot(nest, MechanicalNestBlockEntity.RF_UPGRADE_SLOT, 136, 26));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 66 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            addSlot(new Slot(inventory, hotbar, 8 + hotbar * 18, 124));
        }
        addEnergySlots();
    }

    private void addEnergySlots() {
        addDataSlot(new DataSlot() {
            @Override public int get() { return serverEnergy() & 0xFFFF; }
            @Override public void set(int value) { clientEnergy = (clientEnergy & 0xFFFF0000) | (value & 0xFFFF); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return (serverEnergy() >>> 16) & 0xFFFF; }
            @Override public void set(int value) { clientEnergy = (clientEnergy & 0xFFFF) | ((value & 0xFFFF) << 16); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return serverCapacity() & 0xFFFF; }
            @Override public void set(int value) { clientCapacity = (clientCapacity & 0xFFFF0000) | (value & 0xFFFF); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return (serverCapacity() >>> 16) & 0xFFFF; }
            @Override public void set(int value) { clientCapacity = (clientCapacity & 0xFFFF) | ((value & 0xFFFF) << 16); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return serverCost() & 0xFFFF; }
            @Override public void set(int value) { clientCost = (clientCost & 0xFFFF0000) | (value & 0xFFFF); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return (serverCost() >>> 16) & 0xFFFF; }
            @Override public void set(int value) { clientCost = (clientCost & 0xFFFF) | ((value & 0xFFFF) << 16); }
        });
    }

    private static MechanicalNestBlockEntity resolve(Inventory inventory, FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof MechanicalNestBlockEntity nest) {
            return nest;
        }
        throw new IllegalStateException("Mechanical Nest not found at " + pos);
    }

    private boolean serverSide() {
        return nest.getLevel() != null && !nest.getLevel().isClientSide;
    }

    private int serverEnergy() { return nest.getEnergyStored(); }
    private int serverCapacity() { return nest.getEnergyCapacity(); }
    private int serverCost() { return nest.getEnergyCost(); }

    public int getEnergy() { return serverSide() ? serverEnergy() : clientEnergy; }
    public int getCapacity() { return serverSide() ? serverCapacity() : clientCapacity; }
    public int getEnergyCost() { return serverSide() ? serverCost() : clientCost; }
    public ContainerLevelAccess getAccess() { return access; }

    @Override
    public MechanicalNestBlockEntity getSideConfigurable() { return nest; }

    @Override
    public boolean stillValid(Player player) {
        return nest.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ChickenItemHelper.isRobotRooster(current)) {
                if (!moveItemStackTo(current, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (current.is(strhercules.chickens.registry.ModRegistry.SPEED_UPGRADE.get())) {
                if (!moveItemStackTo(current, 1, 2, false)) return ItemStack.EMPTY;
            } else if (current.is(strhercules.chickens.registry.ModRegistry.RF_UPGRADE.get())) {
                if (!moveItemStackTo(current, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, current);
        }
        return original;
    }

    private static final class RoosterSlot extends Slot {
        private final MechanicalNestBlockEntity nest;

        private RoosterSlot(MechanicalNestBlockEntity nest, int index, int x, int y) {
            super(nest, index, x, y);
            this.nest = nest;
        }
        @Override public boolean mayPlace(ItemStack stack) { return nest.canInsertRooster(stack); }
        @Override public int getMaxStackSize(ItemStack stack) { return 1; }
    }

    private static final class UpgradeSlot extends Slot {
        private final MechanicalNestBlockEntity nest;
        private final int upgrade;
        private UpgradeSlot(MechanicalNestBlockEntity nest, int upgrade, int x, int y) {
            super(nest, upgrade, x, y);
            this.nest = nest;
            this.upgrade = upgrade;
        }
        @Override public boolean mayPlace(ItemStack stack) { return nest.canPlaceUpgrade(upgrade, stack); }
        @Override public int getMaxStackSize(ItemStack stack) { return nest.getUpgradeMaxStackSize(upgrade); }
    }
}
