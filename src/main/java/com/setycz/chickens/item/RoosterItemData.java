package com.setycz.chickens.item;

import com.setycz.chickens.entity.Rooster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Helper for serialising rooster-specific data into an item stack so captured
 * roosters can retain their stored seeds and feed level across conversions.
 */
public final class RoosterItemData {
    private static final String TAG_ROOT = "Rooster";
    private static final String TAG_SEEDS = "Seeds";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_ITEM_ID = "Item";
    private static final String TAG_ITEM_COUNT = "Count";

    private RoosterItemData() {
    }

    public static void copyFromEntity(ItemStack stack, Rooster rooster) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.contains(TAG_ROOT) ? tag.getCompound(TAG_ROOT) : new CompoundTag();
            root.putInt(TAG_SEEDS, rooster.getSeeds());
            CompoundTag items = new CompoundTag();
            // Persist the single seed slot using a lightweight representation
            // (item id + count) so the stack does not depend on HolderLookup
            // providers that are only available on worlds.
            ItemStack seedStack = rooster.getItem(0);
            if (!seedStack.isEmpty()) {
                Item item = seedStack.getItem();
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                items.putString(TAG_ITEM_ID, id.toString());
                items.putInt(TAG_ITEM_COUNT, seedStack.getCount());
            }
            root.put(TAG_ITEMS, items);
            tag.put(TAG_ROOT, root);
        });
    }

    public static void applyToEntity(ItemStack stack, Rooster rooster) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.contains(TAG_ROOT)) {
            return;
        }
        CompoundTag root = data.copyTag().getCompound(TAG_ROOT);
        rooster.setSeeds(root.getInt(TAG_SEEDS));
        if (root.contains(TAG_ITEMS)) {
            CompoundTag items = root.getCompound(TAG_ITEMS);
            // Slot 0 is the rooster's single seed slot; fall back to empty if
            // the item data is missing or malformed.
            if (!items.isEmpty() && items.contains(TAG_ITEM_ID)) {
                ResourceLocation id = ResourceLocation.parse(items.getString(TAG_ITEM_ID));
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    int count = Math.max(1, Math.min(64, items.getInt(TAG_ITEM_COUNT)));
                    rooster.setItem(0, new ItemStack(item, count));
                }
            }
        }
    }
}
