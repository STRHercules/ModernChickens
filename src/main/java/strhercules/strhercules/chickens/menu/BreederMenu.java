package strhercules.chickens.menu;

import strhercules.chickens.blockentity.AbstractChickenContainerBlockEntity;
import strhercules.chickens.blockentity.BreederBlockEntity;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Menu wiring for the breeder. Two parent slots, a seed slot, and three outputs
 * mirror the legacy GUI layout while syncing progress back to the client.
 */
public class BreederMenu extends AbstractContainerMenu implements SideConfigMenu {
    private static final int INVENTORY_SIZE = BreederBlockEntity.INVENTORY_SIZE;
    private final BreederBlockEntity breeder;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlotCount;

    public BreederMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, resolveBlockEntity(playerInventory, buffer));
    }

    public BreederMenu(int id, Inventory playerInventory, BreederBlockEntity breeder) {
        this(id, playerInventory, breeder, breeder.getDataAccess());
    }

    public BreederMenu(int id, Inventory playerInventory, BreederBlockEntity breeder, ContainerData data) {
        super(ModMenuTypes.BREEDER.get(), id);
        this.breeder = breeder;
        this.data = data;
        this.machineSlotCount = breeder.getContainerSize();
        Level level = breeder.getLevel();
        this.access = level != null ? ContainerLevelAccess.create(level, breeder.getBlockPos()) : ContainerLevelAccess.NULL;

        this.addSlot(new ChickenSlot(breeder, BreederBlockEntity.LEFT_CHICKEN_SLOT, 44, 20));
        this.addSlot(new ChickenSlot(breeder, BreederBlockEntity.RIGHT_CHICKEN_SLOT, 62, 20));
        this.addSlot(new SeedSlot(breeder, BreederBlockEntity.SEED_SLOT, 8, 20));
        for (int i = 0; i < 3; i++) {
            this.addSlot(new OutputSlot(breeder, i + 3, 116 + i * 18, 20));
        }
        this.addSlot(new MachineUpgradeSlot(breeder, BreederBlockEntity.SPEED_UPGRADE_SLOT, 128, 41));
        this.addSlot(new MachineUpgradeSlot(breeder, BreederBlockEntity.STACK_UPGRADE_SLOT, 149, 41));

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 62 + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 120));
        }

        this.addDataSlots(data);
    }

    private static BreederBlockEntity resolveBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        Objects.requireNonNull(inventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = inventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BreederBlockEntity breeder) {
            return breeder;
        }
        throw new IllegalStateException("Breeder not found at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return breeder.stillValid(player);
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

    @Override
    public BreederBlockEntity getSideConfigurable() {
        return breeder;
    }

    public BreederBlockEntity getBreeder() {
        return breeder;
    }

    public int getProgress() {
        return data.get(0);
    }

    private static class ChickenSlot extends Slot {
        public ChickenSlot(BreederBlockEntity breeder, int index, int x, int y) {
            super(breeder, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ChickenItemHelper.isChicken(stack) && !ChickenItemHelper.isRooster(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private static class SeedSlot extends Slot {
        private final BreederBlockEntity breeder;

        public SeedSlot(BreederBlockEntity breeder, int index, int x, int y) {
            super(breeder, index, x, y);
            this.breeder = breeder;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS)
                    || stack.is(Items.PUMPKIN_SEEDS);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return breeder.getMaxStackSizeForSlot(getContainerSlot(), stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(Math.min(amount,
                    AbstractChickenContainerBlockEntity.getLegalExternalStackSize(getItem())));
        }
    }

    private static class OutputSlot extends Slot {
        private final BreederBlockEntity breeder;

        public OutputSlot(BreederBlockEntity breeder, int index, int x, int y) {
            super(breeder, index, x, y);
            this.breeder = breeder;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return breeder.getMaxStackSizeForSlot(getContainerSlot(), stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(Math.min(amount,
                    AbstractChickenContainerBlockEntity.getLegalExternalStackSize(getItem())));
        }
    }
}
