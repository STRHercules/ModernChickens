package com.setycz.chickens.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Shared utility for Chickens machine blocks. The modern port still exposes a
 * handful of wooden contraptions (roost, breeder, collector, henhouse) that
 * should behave consistently when broken.
 */
final class MachineBlockHelper {
    private MachineBlockHelper() {
    }

    /**
     * Returns {@code true} when the provided tool can safely harvest wooden
     * machinery. An empty stack or any non-axe tool should fail so the block
     * simply breaks without returning to the player.
     */
    static boolean canHarvestWith(ItemStack tool) {
        return !tool.isEmpty() && tool.is(ItemTags.AXES);
    }

    /**
     * Spawns the machine block item, copying its custom name when present.
     * Inventory contents are handled elsewhere by the block entities, so only
     * the block itself is dropped here.
     */
    static void dropMachine(Level level, BlockPos pos, Block block, @Nullable BlockEntity blockEntity, Player player) {
        if (level.isClientSide || player.isCreative()) {
            return;
        }
        ItemStack drop = new ItemStack(block);
        if (drop.isEmpty()) {
            return;
        }
        if (blockEntity instanceof Nameable nameable) {
            var customName = nameable.getCustomName();
            if (customName != null) {
                drop.set(DataComponents.CUSTOM_NAME, customName);
            }
        }
        Block.popResource(level, pos, drop);
    }
}
