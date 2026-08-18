package strhercules.chickens.item;

import strhercules.chickens.entity.Rooster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import strhercules.chickens.item.ItemData;

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
    private static final String TAG_ROBOT_ROOSTER = "RobotRooster";
    private static final String TAG_VIRUS_TICKS = "VirusTicks";

    private RoosterItemData() {
    }

    public static void copyFromEntity(ItemStack stack, Rooster rooster) {
        ChickenItemHelper.setRooster(stack, true);
        ChickenItemHelper.setRobotRooster(stack, rooster.isRobotRooster());
        ItemData.update(stack, tag -> {
            CompoundTag root = tag.contains(TAG_ROOT) ? tag.getCompound(TAG_ROOT) : new CompoundTag();
            root.putInt(TAG_SEEDS, rooster.getSeeds());
            root.putBoolean(TAG_ROBOT_ROOSTER, rooster.isRobotRooster());
            root.putInt(TAG_VIRUS_TICKS, rooster.getVirusTicksRemaining());
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
        CompoundTag data = ItemData.read(stack);
        if (!data.contains(TAG_ROOT)) {
            rooster.setRobotRooster(ChickenItemHelper.isRobotRooster(stack));
            return;
        }
        CompoundTag root = data.getCompound(TAG_ROOT);
        rooster.setRobotRooster(root.getBoolean(TAG_ROBOT_ROOSTER) || ChickenItemHelper.isRobotRooster(stack));
        rooster.setVirusTicksRemaining(root.getInt(TAG_VIRUS_TICKS));
        rooster.setSeeds(root.getInt(TAG_SEEDS));
        if (root.contains(TAG_ITEMS)) {
            CompoundTag items = root.getCompound(TAG_ITEMS);
            // Slot 0 is the rooster's single seed slot; fall back to empty if
            // the item data is missing or malformed.
            if (!items.isEmpty() && items.contains(TAG_ITEM_ID)) {
                ResourceLocation id = new ResourceLocation(items.getString(TAG_ITEM_ID));
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    int count = Math.max(1, Math.min(64, items.getInt(TAG_ITEM_COUNT)));
                    rooster.setItem(0, new ItemStack(item, count));
                }
            }
        }
    }
}
