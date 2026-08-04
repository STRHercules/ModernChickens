package strhercules.chickens.menu;

import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.registry.ModMenuTypes;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;

/** Flight and chest equipment slots plus the mega chicken's 54 cargo slots. */
public final class MegaChickenMenu extends AbstractContainerMenu {
    private static final int SADDLE_SLOT = 0;
    private static final int FLYING_EGG_SLOT = 1;
    private static final int RIGHT_CHEST_SLOT = 2;
    private static final int LEFT_CHEST_SLOT = 3;
    private static final int ANIMAL_SLOT_COUNT = 4;
    private static final int CARGO_COLUMNS = 9;
    private static final int CARGO_ROWS_PER_CHEST = 3;
    private static final int CARGO_SLOTS_PER_CHEST = CARGO_COLUMNS * CARGO_ROWS_PER_CHEST;
    private static final int CARGO_START = ANIMAL_SLOT_COUNT;
    private static final int CARGO_END = CARGO_START + CARGO_SLOTS_PER_CHEST * 2;
    private static final int PLAYER_START = CARGO_END;
    private static final int HOTBAR_START = PLAYER_START + 27;

    private final MegaChicken chicken;
    private final Container cargo;
    private final Container chestEquipment;

    public MegaChickenMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveChicken(playerInventory, buffer));
    }

    public MegaChickenMenu(int id, Inventory playerInventory, MegaChicken chicken) {
        super(ModMenuTypes.MEGA_CHICKEN.get(), id);
        this.chicken = chicken;
        this.cargo = chicken != null ? chicken.getInventory() : new SimpleContainer(55);
        this.chestEquipment = chicken != null ? chicken.getChestEquipment() : new SimpleContainer(3);

        if (chicken != null) {
            this.cargo.startOpen(playerInventory.player);
            this.chestEquipment.startOpen(playerInventory.player);
        }

        this.addSlot(new SaddleSlot(this.cargo, 0, chicken));
        this.addSlot(new FlyingEggSlot(this.chestEquipment, MegaChicken.FLYING_EGG_SLOT, 86, 10, chicken));
        this.addSlot(new ChestSlot(this.chestEquipment, MegaChicken.RIGHT_CHEST_SLOT, 86, 40,
                this.cargo, chicken, false));
        this.addSlot(new ChestSlot(this.chestEquipment, MegaChicken.LEFT_CHEST_SLOT, 65, 40,
                this.cargo, chicken, true));

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < CARGO_COLUMNS; column++) {
                this.addSlot(new CargoSlot(this.cargo, 1 + column + row * CARGO_COLUMNS,
                        8 + column * 18, 63 + row * 18, chicken, row < CARGO_ROWS_PER_CHEST));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 232));
        }
    }

    private static MegaChicken resolveChicken(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        if (buffer == null) {
            return null;
        }
        if (playerInventory.player.level().getEntity(buffer.readVarInt()) instanceof MegaChicken chicken) {
            return chicken;
        }
        throw new IllegalStateException("Mega chicken not found for inventory menu");
    }

    @Override
    public boolean stillValid(Player player) {
        return this.chicken == null
                || (!this.chicken.hasInventoryChanged(this.cargo)
                && this.cargo.stillValid(player)
                && this.chestEquipment.stillValid(player)
                && this.chicken.isAlive()
                && (this.chicken.level().isClientSide || this.chicken.isOwnedByPlayer(player))
                && player.canInteractWithEntity(this.chicken, 4.0D));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem() && slot.isActive() && slot.mayPickup(player)) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();

            if (index < PLAYER_START) {
                if (!this.moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(FLYING_EGG_SLOT).mayPlace(stack)
                    && this.moveItemStackTo(stack, FLYING_EGG_SLOT, FLYING_EGG_SLOT + 1, false)) {
                // Flying Eggs go into the dedicated flight slot.
            } else if (this.getSlot(SADDLE_SLOT).mayPlace(stack)
                    && this.moveItemStackTo(stack, SADDLE_SLOT, SADDLE_SLOT + 1, false)) {
                // Saddle goes into the dedicated saddle slot.
            } else if (this.moveItemStackTo(stack, RIGHT_CHEST_SLOT, LEFT_CHEST_SLOT + 1, false)) {
                // Chests go into the first available chest equipment slot.
            } else if (this.moveItemStackTo(stack, CARGO_START, CARGO_END, false)) {
                // Other items use the inherited cargo inventory.
            } else if (index < HOTBAR_START) {
                if (!this.moveItemStackTo(stack, HOTBAR_START, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.chicken != null) {
            this.cargo.stopOpen(player);
            this.chestEquipment.stopOpen(player);
        }
    }

    public MegaChicken getChicken() {
        return this.chicken;
    }

    private static final class SaddleSlot extends Slot {
        private final MegaChicken chicken;

        private SaddleSlot(Container container, int index, MegaChicken chicken) {
            super(container, index, 65, 10);
            this.chicken = chicken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.chicken != null && stack.is(Items.SADDLE) && this.chicken.isSaddleable();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class FlyingEggSlot extends Slot {
        private final MegaChicken chicken;

        private FlyingEggSlot(Container container, int index, int x, int y, MegaChicken chicken) {
            super(container, index, x, y);
            this.chicken = chicken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.chicken != null && stack.is(ModRegistry.FLYING_EGG.get());
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.chicken != null && this.chicken.canRemoveFlyingEgg();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class CargoSlot extends Slot {
        private final MegaChicken chicken;
        private final boolean topRows;

        private CargoSlot(Container container, int index, int x, int y,
                           MegaChicken chicken, boolean topRows) {
            super(container, index, x, y);
            this.chicken = chicken;
            this.topRows = topRows;
        }

        @Override
        public boolean isActive() {
            return this.chicken != null
                    && (this.topRows ? this.chicken.hasLeftChest() : this.chicken.hasRightChest());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.isActive();
        }
    }

    private static final class ChestSlot extends Slot {
        private final Container cargo;
        private final MegaChicken chicken;
        private final boolean topRows;

        private ChestSlot(Container container, int index, int x, int y,
                          Container cargo, MegaChicken chicken, boolean topRows) {
            super(container, index, x, y);
            this.cargo = cargo;
            this.chicken = chicken;
            this.topRows = topRows;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.chicken != null && stack.is(Items.CHEST);
        }

        @Override
        public boolean mayPickup(Player player) {
            int start = 1 + (this.topRows ? 0 : CARGO_SLOTS_PER_CHEST);
            int end = start + CARGO_SLOTS_PER_CHEST;
            for (int index = start; index < end; index++) {
                if (!this.cargo.getItem(index).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
