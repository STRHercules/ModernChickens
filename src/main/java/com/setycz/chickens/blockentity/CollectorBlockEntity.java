package com.setycz.chickens.blockentity;

import com.setycz.chickens.menu.CollectorMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
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
    private static final int SCAN_RANGE = 4;
    private int searchOffset = 0;

    public CollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTOR.get(), pos, state, INVENTORY_SIZE, 0);
    }

    @Override
    protected void runServerTick(Level level) {
        super.runServerTick(level);
        updateSearchOffset();
        gatherItems(level);
    }

    @Override
    protected void spawnChickenDrop(RandomSource random) {
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

    private void updateSearchOffset() {
        searchOffset = (searchOffset + 1) % 27;
    }

    private void gatherItems(Level level) {
        int y = searchOffset / 9;
        int zOffset = (searchOffset % 9) - 4;
        for (int xOffset = -SCAN_RANGE; xOffset <= SCAN_RANGE; xOffset++) {
            BlockPos target = worldPosition.offset(xOffset, y, zOffset);
            if (target.equals(worldPosition) || !level.hasChunkAt(target)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(target);
            if (blockEntity instanceof AbstractChickenContainerBlockEntity other && other != this) {
                if (pullFromContainer(other)) {
                    return;
                }
            }
        }
    }

    private boolean pullFromContainer(AbstractChickenContainerBlockEntity other) {
        int start = other.getOutputSlotIndex();
        int size = other.getContainerSize();
        for (int slot = start; slot < size; slot++) {
            ItemStack stack = other.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack single = stack.copy();
            single.setCount(1);
            ItemStack remaining = pushIntoOutput(single);
            if (remaining.isEmpty()) {
                other.removeItem(slot, 1);
                return true;
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
}
