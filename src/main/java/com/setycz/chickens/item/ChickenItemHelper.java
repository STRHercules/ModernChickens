package com.setycz.chickens.item;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import net.minecraft.core.component.DataComponents;
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
}
