package com.setycz.chickens.blockentity;

import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.menu.CollectorMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Block entity that periodically scans nearby roost-style containers and pulls
 * drops into its own inventory. The logic mirrors the legacy collector while
 * reusing the shared container base for inventory persistence.
 */
public class CollectorBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int INVENTORY_SIZE = 27;
    private static final int MAX_SCAN_RANGE = 16;

    public CollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTOR.get(), pos, state, INVENTORY_SIZE, 0);
    }

    @Override
    protected void runServerTick(Level level) {
        super.runServerTick(level);
        int range = clampRange(ChickensConfigHolder.get().getCollectorScanRange());
        gatherItems(level, range);
    }

    @Override
    protected void spawnChickenItem(RandomSource random) {
        // No-op: the collector never generates drops on its own.
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
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory, ContainerData dataAccess) {
        return new CollectorMenu(id, playerInventory, this);
    }

    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        return null;
    }

    private void gatherItems(Level level, int range) {
        if (range <= 0) {
            return;
        }
        // Sweep the entire configured cube (default 9x9x9) each tick to mirror the original mod reach.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int xOffset = -range; xOffset <= range; xOffset++) {
            for (int yOffset = -range; yOffset <= range; yOffset++) {
                for (int zOffset = -range; zOffset <= range; zOffset++) {
                    if (xOffset == 0 && yOffset == 0 && zOffset == 0) {
                        continue;
                    }
                    cursor.setWithOffset(worldPosition, xOffset, yOffset, zOffset);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity instanceof AbstractChickenContainerBlockEntity other && other != this) {
                        if (drainContainer(other)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean drainContainer(AbstractChickenContainerBlockEntity other) {
        int start = other.getOutputSlotIndex();
        int size = other.getContainerSize();
        for (int slot = start; slot < size; slot++) {
            ItemStack stack = other.getItem(slot);
            // Drain the full stack before advancing so large drop buffers empty quickly.
            while (!stack.isEmpty()) {
                ItemStack remaining = pushIntoOutput(stack);
                int transferred = stack.getCount() - remaining.getCount();
                if (transferred <= 0) {
                    return true;
                }
                other.removeItem(slot, transferred);
                if (isOutputInventoryFull()) {
                    return true;
                }
                stack = other.getItem(slot);
            }
        }
        return false;
    }

    @Override
    public void storeTooltipData(CompoundTag tag) {
        super.storeTooltipData(tag);
        int filled = 0;
        for (int slot = getOutputSlotIndex(); slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                filled++;
            }
        }
        tag.putInt("FilledSlots", filled);
        tag.putInt("TotalSlots", getContainerSize());
    }

    @Override
    public void appendTooltip(List<Component> tooltip, CompoundTag data) {
        tooltip.add(Component.translatable("tooltip.chickens.collector.slots", data.getInt("FilledSlots"),
                data.getInt("TotalSlots")));
        super.appendTooltip(tooltip, data);
    }

    private static int clampRange(int configuredRange) {
        return Mth.clamp(configuredRange, 0, MAX_SCAN_RANGE);
    }

    private boolean isOutputInventoryFull() {
        int start = getOutputSlotIndex();
        int size = getContainerSize();
        for (int slot = start; slot < size; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}
