package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickenStats;
import com.setycz.chickens.menu.RoostMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Server-side logic for the roost block. The block entity manages a single
 * chicken stack plus four output slots that accumulate drops over time.
 */
public class RoostBlockEntity extends AbstractChickenContainerBlockEntity {
    public static final int INVENTORY_SIZE = 5;
    public static final int CHICKEN_SLOT = 0;
    private static final int MAX_CHICKENS = 16;

    public RoostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROOST.get(), pos, state, INVENTORY_SIZE, 1);
    }

    @Override
    protected void spawnChickenDrop(RandomSource random) {
        ChickenContainerEntry entry = getChickenEntry(CHICKEN_SLOT);
        if (entry == null) {
            return;
        }
        ItemStack drop = entry.createDrop(random);
        ItemStack remaining = pushIntoOutput(drop);
        if (!remaining.isEmpty() && level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remaining);
        }
    }

    @Override
    protected int requiredSeedsForDrop() {
        return 0;
    }

    @Override
    protected double speedMultiplier() {
        return ChickensConfigHolder.get().getRoostSpeedMultiplier();
    }

    @Override
    protected int getChickenSlotCount() {
        return 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("menu.chickens.roost");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory, ContainerData dataAccess) {
        return new RoostMenu(id, playerInventory, this, dataAccess);
    }

    @Override
    protected ChickenContainerEntry createChickenData(int slot, ItemStack stack) {
        if (slot != CHICKEN_SLOT || !ChickenItemHelper.isChicken(stack)) {
            return null;
        }
        ChickensRegistryItem description = ChickenItemHelper.resolve(stack);
        if (description == null) {
            return null;
        }
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        return new ChickenContainerEntry(description, stats);
    }

    @Override
    protected int getMaxStackSizeForSlot(int slot, ItemStack stack) {
        if (slot == CHICKEN_SLOT) {
            return Math.min(MAX_CHICKENS, stack.getMaxStackSize());
        }
        return super.getMaxStackSizeForSlot(slot, stack);
    }

    public boolean putChicken(ItemStack newStack) {
        if (!ChickenItemHelper.isChicken(newStack) || level == null) {
            return false;
        }
        ItemStack current = getItem(CHICKEN_SLOT);
        if (current.isEmpty()) {
            int toMove = Math.min(MAX_CHICKENS, newStack.getCount());
            if (toMove <= 0) {
                return false;
            }
            setItem(CHICKEN_SLOT, newStack.split(toMove));
            playAddSound();
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, newStack)) {
            return false;
        }
        int space = MAX_CHICKENS - current.getCount();
        if (space <= 0) {
            return false;
        }
        int toMove = Math.min(space, newStack.getCount());
        if (toMove <= 0) {
            return false;
        }
        current.grow(toMove);
        newStack.shrink(toMove);
        setChanged();
        playAddSound();
        return true;
    }

    public boolean pullChickenOut(Player player) {
        ItemStack stack = getItem(CHICKEN_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack toGive = stack.copy();
        setItem(CHICKEN_SLOT, ItemStack.EMPTY);
        if (!player.addItem(toGive)) {
            player.drop(toGive, false);
        }
        playRemoveSound();
        return true;
    }

    private void playAddSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void playRemoveSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void storeTooltipData(CompoundTag tag) {
        super.storeTooltipData(tag);
        ItemStack stack = getItem(CHICKEN_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        tag.putInt("ChickenId", ChickenItemHelper.getChickenType(stack));
        tag.putInt("ChickenCount", stack.getCount());
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        tag.putInt("Gain", stats.gain());
    }

    @Override
    public void appendTooltip(List<Component> tooltip, CompoundTag data) {
        if (data.contains("ChickenId")) {
            ChickensRegistryItem chicken = ChickensRegistry.getByType(data.getInt("ChickenId"));
            if (chicken != null) {
                int chickens = data.getInt("ChickenCount");
                int gain = data.getInt("Gain");
                int dropCount = gain >= 10 ? 3 : gain >= 5 ? 2 : 1;
                ItemStack drop = chicken.createDropItem();
                drop.setCount(dropCount);
                tooltip.add(Component.translatable("tooltip.chickens.roost.summary", chicken.getDisplayName(), chickens,
                        drop.getHoverName(), dropCount));
            }
        }
        super.appendTooltip(tooltip, data);
    }
}
