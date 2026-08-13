package strhercules.chickens.blockentity;

import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.blockentity.MechanicalRoostBlockEntity;
import strhercules.chickens.blockentity.RoostBlockEntity;
import strhercules.chickens.menu.CollectorMenu;
import strhercules.chickens.registry.ModBlockEntities;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Block entity that pulls drops from nearby chicken containers into unlocked
 * storage rows. Capacity upgrades add three 13-slot rows at a time.
 */
public class CollectorBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int BASE_STORAGE_SLOTS = 26;
    public static final int SLOTS_PER_CAPACITY_UPGRADE = 39;
    public static final int MAX_STORAGE_SLOTS = 104;
    public static final int INVENTORY_SIZE = BASE_STORAGE_SLOTS;

    public static final int STACK_UPGRADE_SLOT = 0;
    public static final int CAPACITY_UPGRADE_SLOT = 1;
    public static final int SPEED_UPGRADE_SLOT = 2;
    public static final int RANGE_UPGRADE_SLOT = 3;
    public static final int UPGRADE_SLOT_COUNT = 4;

    private static final int MAX_RANGE = 16 + 20 * 4;
    private double scanWork;
    private int syncedCapacityLevel = -1;

    public CollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTOR.get(), pos, state, MAX_STORAGE_SLOTS, 0, UPGRADE_SLOT_COUNT);
    }

    @Override
    protected int getActiveStorageSize() {
        return getStorageSlotCount();
    }

    public int getCapacityUpgradeCount() {
        return getUpgradeCount(CAPACITY_UPGRADE_SLOT);
    }

    public int getCapacityLevel() {
        return Math.min(2, getCapacityUpgradeCount());
    }

    public int getStorageSlotCount() {
        return BASE_STORAGE_SLOTS + getCapacityLevel() * SLOTS_PER_CAPACITY_UPGRADE;
    }

    public int getStorageRows() {
        return 2 + getCapacityLevel() * 3;
    }

    @Override
    protected int getContainerDataCount() {
        return 2;
    }

    @Override
    protected int getContainerDataValue(int index) {
        if (index != 1) {
            return 0;
        }
        int installed = getCapacityLevel();
        return level != null && level.isClientSide && syncedCapacityLevel >= 0
                ? syncedCapacityLevel : installed;
    }

    @Override
    protected void setContainerDataValue(int index, int value) {
        if (index == 1) {
            syncedCapacityLevel = Mth.clamp(value, 0, 2);
        }
    }

    @Override
    protected void runServerTick(Level level) {
        super.runServerTick(level);
        scanWork += 1.0D + 0.2D * getUpgradeCount(SPEED_UPGRADE_SLOT);
        int operations = (int) scanWork;
        if (operations <= 0) {
            return;
        }
        scanWork -= operations;
        gatherItems(level, getScanRange(), operations);
    }

    @Override
    protected boolean spawnChickenItem(RandomSource random) {
        return false;
    }

    @Override
    protected int requiredSeedsForDrop() {
        return 0;
    }

    @Override
    protected double speedMultiplier() {
        return 1.0D;
    }

    @Override
    protected int getChickenSlotCount() {
        return 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("menu.chickens.collector");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory,
            net.minecraft.world.inventory.ContainerData dataAccess) {
        return new CollectorMenu(id, playerInventory, this, dataAccess);
    }

    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        return null;
    }

    @Override
    protected boolean hasStackUpgrade() {
        return getUpgradeCount(STACK_UPGRADE_SLOT) > 0;
    }

    @Override
    protected int getStackUpgradeCount() {
        return getUpgradeCount(STACK_UPGRADE_SLOT);
    }

    @Override
    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return slot == STACK_UPGRADE_SLOT && stack.is(ModRegistry.STACK_UPGRADE.get())
                || slot == CAPACITY_UPGRADE_SLOT && stack.is(ModRegistry.STORAGE_CAPACITY_UPGRADE.get())
                || slot == SPEED_UPGRADE_SLOT && stack.is(ModRegistry.SPEED_UPGRADE.get())
                || slot == RANGE_UPGRADE_SLOT && stack.is(ModRegistry.RANGE_UPGRADE.get());
    }

    public boolean isUpgradeItem(ItemStack stack) {
        return stack.is(ModRegistry.STACK_UPGRADE.get())
                || stack.is(ModRegistry.STORAGE_CAPACITY_UPGRADE.get())
                || stack.is(ModRegistry.SPEED_UPGRADE.get())
                || stack.is(ModRegistry.RANGE_UPGRADE.get());
    }

    @Override
    public boolean canRemoveUpgrade(int slot, int count) {
        if (slot == STACK_UPGRADE_SLOT) {
            return canRemoveStackUpgrade(count);
        }
        if (slot != CAPACITY_UPGRADE_SLOT) {
            return true;
        }
        int installed = getUpgradeCount(CAPACITY_UPGRADE_SLOT);
        int remaining = Math.max(0, installed - Math.max(count, 0));
        int activeAfter = BASE_STORAGE_SLOTS + Math.min(2, remaining) * SLOTS_PER_CAPACITY_UPGRADE;
        for (int storageSlot = activeAfter; storageSlot < getStorageSlotCount(); storageSlot++) {
            if (!getItem(storageSlot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getUpgradeMaxStackSize(int slot) {
        return switch (slot) {
            case STACK_UPGRADE_SLOT -> MAX_STACK_UPGRADE_COUNT;
            case CAPACITY_UPGRADE_SLOT -> 2;
            case SPEED_UPGRADE_SLOT -> 5;
            case RANGE_UPGRADE_SLOT -> 4;
            default -> 1;
        };
    }

    private void gatherItems(Level level, int range, int maxOperations) {
        if (range <= 0 || maxOperations <= 0) {
            return;
        }

        int minX = worldPosition.getX() - range;
        int maxX = worldPosition.getX() + range;
        int minZ = worldPosition.getZ() - range;
        int maxZ = worldPosition.getZ() + range;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                BlockPos chunkProbe = new BlockPos(chunkX << 4, worldPosition.getY(), chunkZ << 4);
                if (!level.hasChunkAt(chunkProbe)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof RoostBlockEntity)
                            && !(blockEntity instanceof MechanicalRoostBlockEntity)) {
                        continue;
                    }
                    AbstractChickenContainerBlockEntity other = (AbstractChickenContainerBlockEntity) blockEntity;
                    BlockPos otherPos = other.getBlockPos();
                    if (Math.abs(otherPos.getX() - worldPosition.getX()) > range
                            || Math.abs(otherPos.getY() - worldPosition.getY()) > range
                            || Math.abs(otherPos.getZ() - worldPosition.getZ()) > range) {
                        continue;
                    }
                    if (drainContainer(other) && --maxOperations <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private boolean drainContainer(AbstractChickenContainerBlockEntity other) {
        int start = other.getOutputSlotIndex();
        int end = start + other.getOutputSlotCount();
        for (int slot = start; slot < end; slot++) {
            ItemStack stack = other.getItem(slot);
            while (!stack.isEmpty()) {
                ItemStack remaining = pushIntoOutput(stack);
                int transferred = stack.getCount() - remaining.getCount();
                if (transferred <= 0) {
                    return true;
                }
                other.removeItemForMachine(slot, transferred);
                if (isOutputInventoryFull()) {
                    return true;
                }
                stack = other.getItem(slot);
            }
        }
        return false;
    }

    @Override
    public void storeTooltipData(net.minecraft.nbt.CompoundTag tag) {
        super.storeTooltipData(tag);
        int filled = 0;
        for (int slot = getOutputSlotIndex(); slot < getOutputSlotIndex() + getOutputSlotCount(); slot++) {
            if (!getItem(slot).isEmpty()) {
                filled++;
            }
        }
        tag.putInt("FilledSlots", filled);
        tag.putInt("TotalSlots", getStorageSlotCount());
        tag.putInt("Range", getScanRange());
    }

    @Override
    public void appendTooltip(List<Component> tooltip, net.minecraft.nbt.CompoundTag data) {
        tooltip.add(Component.translatable("tooltip.chickens.collector.slots", data.getInt("FilledSlots"),
                data.getInt("TotalSlots")));
        tooltip.add(Component.translatable("tooltip.chickens.collector.range", data.getInt("Range")));
        super.appendTooltip(tooltip, data);
    }

    public int getScanRange() {
        return Mth.clamp(ChickensConfigHolder.get().getCollectorScanRange()
                + 20 * getUpgradeCount(RANGE_UPGRADE_SLOT), 0, MAX_RANGE);
    }

    private boolean isOutputInventoryFull() {
        int start = getOutputSlotIndex();
        int end = start + getOutputSlotCount();
        for (int slot = start; slot < end; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || stack.getCount() < getMaxStackSizeForSlot(slot, stack)) {
                return false;
            }
        }
        return true;
    }
}
