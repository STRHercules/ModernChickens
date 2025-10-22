package com.setycz.chickens.item;

import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

/**
 * Flux-infused egg that stores a configurable Redstone Flux charge per stack.
 * Chickens imprint their current stats onto the stack so downstream blocks can
 * extract the energy and display contextual tooltips/bars without recomputing
 * the capacity every tick.
 */
public class FluxEggItem extends Item {
    private static final String TAG_ENERGY = "FluxEnergy";
    private static final String TAG_CAPACITY = "FluxCapacity";

    public static final int BASE_CAPACITY = 1_000;
    public static final int STAT_BONUS = 100;

    public FluxEggItem(Properties properties) {
        super(properties);
    }

    /**
     * Factory helper mirroring the legacy egg creators. The returned stack
     * starts charged to the provided capacity so automated farms can drop the
     * item straight into energy consumers.
     */
    public static ItemStack create(int capacity) {
        ItemStack stack = new ItemStack(ModRegistry.FLUX_EGG.get());
        setEnergy(stack, capacity, capacity);
        return stack;
    }

    /**
     * Calculates the maximum flux payload a chicken with the supplied stats can
     * imprint on a freshly laid egg. Each point above the baseline contributes
     * an extra {@value #STAT_BONUS} RF on top of the {@value #BASE_CAPACITY}
     * starter charge.
     */
    public static int calculateCapacity(ChickenStats stats) {
        int growthBonus = Math.max(0, stats.growth() - 1);
        int gainBonus = Math.max(0, stats.gain() - 1);
        int strengthBonus = Math.max(0, stats.strength() - 1);
        return BASE_CAPACITY + STAT_BONUS * (growthBonus + gainBonus + strengthBonus);
    }

    /**
     * Imprints the egg with the chicken's stats, charging it to the
     * corresponding capacity so downstream machines can drain the stored RF.
     */
    public static void imprintStats(ItemStack stack, ChickenStats stats) {
        int capacity = calculateCapacity(stats);
        setEnergy(stack, capacity, capacity);
    }

    /**
     * Writes both the stored energy and capacity into the custom data component
     * so stacks retain their charge even after being partially drained or moved
     * between inventories.
     */
    public static void setEnergy(ItemStack stack, int stored, int capacity) {
        int safeCapacity = Math.max(BASE_CAPACITY, capacity);
        int clampedEnergy = Mth.clamp(stored, 0, safeCapacity);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_CAPACITY, safeCapacity);
            tag.putInt(TAG_ENERGY, clampedEnergy);
        });
    }

    /**
     * Convenience setter that reuses the current capacity stored on the stack.
     */
    public static void setStoredEnergy(ItemStack stack, int stored) {
        int capacity = getCapacity(stack);
        setEnergy(stack, stored, capacity);
    }

    public static int getStoredEnergy(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(TAG_ENERGY)) {
            CompoundTag tag = data.copyTag();
            return Mth.clamp(tag.getInt(TAG_ENERGY), 0, getCapacity(stack));
        }
        return BASE_CAPACITY;
    }

    public static int getCapacity(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(TAG_CAPACITY)) {
            CompoundTag tag = data.copyTag();
            int capacity = tag.getInt(TAG_CAPACITY);
            return Math.max(BASE_CAPACITY, capacity);
        }
        return BASE_CAPACITY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int stored = getStoredEnergy(stack);
        int capacity = getCapacity(stack);
        tooltip.add(Component.translatable("item.chickens.flux_egg.tooltip", stored, capacity).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = getCapacity(stack);
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(13.0F * getStoredEnergy(stack) / (float) capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Lean on a bright red hue so the charge bar reads as stored RF at a glance.
        return 0xFF3C3C;
    }
}
