package com.setycz.chickens.item;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.entity.ChickensChicken;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/**
 * Utility methods shared by multiple chicken-themed items. The original mod
 * relied on item metadata to encode the chicken id; modern Minecraft exposes
 * persistent item data through NBT, so the helper centralises that logic.
 */
public final class ChickenItemHelper {
    public static final String TAG_CHICKEN_TYPE = "ChickenType";
    private static final String TAG_STATS = "ChickenStats";

    private ChickenItemHelper() {
    }

    public static void setChickenType(ItemStack stack, int type) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_CHICKEN_TYPE, type));
    }

    public static int getChickenType(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(TAG_CHICKEN_TYPE)) {
            return data.copyTag().getInt(TAG_CHICKEN_TYPE);
        }
        return 0;
    }

    @Nullable
    public static ChickensRegistryItem resolve(ItemStack stack) {
        return ChickensRegistry.getByType(getChickenType(stack));
    }

    public static void setStats(ItemStack stack, ChickenStats stats) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_STATS, stats.toTag()));
    }

    public static ChickenStats getStats(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(TAG_STATS)) {
            CompoundTag tag = data.copyTag().getCompound(TAG_STATS);
            return ChickenStats.fromTag(tag);
        }
        return ChickenStats.DEFAULT;
    }

    public static void copyFromEntity(ItemStack stack, ChickensChicken chicken) {
        setChickenType(stack, chicken.getChickenType());
        setStats(stack, new ChickenStats(chicken.getGrowth(), chicken.getGain(), chicken.getStrength(),
                chicken.getStatsAnalyzed()));
    }

    public static void applyToEntity(ItemStack stack, ChickensChicken chicken) {
        chicken.setChickenType(getChickenType(stack));
        ChickenStats stats = getStats(stack);
        CompoundTag tag = stats.toTag();
        tag.putInt("Type", getChickenType(stack));
        chicken.readAdditionalSaveData(tag);
        chicken.setStatsAnalyzed(stats.analysed());
    }

    public static boolean isChicken(ItemStack stack) {
        return stack.getItem() instanceof ChickenItem;
    }
}
