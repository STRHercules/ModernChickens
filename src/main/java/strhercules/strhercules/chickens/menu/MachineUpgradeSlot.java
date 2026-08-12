package strhercules.chickens.menu;

import strhercules.chickens.blockentity.AbstractChickenContainerBlockEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MachineUpgradeSlot extends Slot {
    private final AbstractChickenContainerBlockEntity machine;
    private final int upgradeSlot;

    public MachineUpgradeSlot(AbstractChickenContainerBlockEntity machine, int upgradeSlot, int x, int y) {
        super(machine, machine.getUpgradeSlotIndex(upgradeSlot), x, y);
        this.machine = machine;
        this.upgradeSlot = upgradeSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return machine.canPlaceUpgrade(upgradeSlot, stack);
    }

    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        return machine.canRemoveUpgrade(upgradeSlot, getItem().getCount());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return machine.getUpgradeMaxStackSize(upgradeSlot);
    }
}
