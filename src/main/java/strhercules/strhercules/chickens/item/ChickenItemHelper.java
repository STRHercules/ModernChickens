package strhercules.chickens.item;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.entity.ChickensChicken;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nullable;

/**
 * Utility methods shared by multiple chicken-themed items. The original mod
 * relied on item metadata to encode the chicken id; modern Minecraft exposes
 * persistent item data through NBT, so the helper centralises that logic.
 */
public final class ChickenItemHelper {
    public static final String TAG_CHICKEN_TYPE = "ChickenType";
    public static final String TAG_ROBOT_CHICKEN = "RobotChicken";
    private static final String TAG_ROOSTER = "IsRooster";
    private static final String TAG_ROBOT_ROOSTER = "IsRobotRooster";
    private static final String TAG_ROBOT_UPGRADES_GIVEN = "RobotUpgradesGiven";
    private static final String TAG_ROBOT_UPGRADES_REQUIRED = "RobotUpgradesRequired";
    private static final String TAG_STATS = "ChickenStats";
    // Reserved custom model id used for rooster stacks so the chicken item
    // model can swap to the dedicated rooster sprite.
    public static final int ROOSTER_MODEL_ID = 900000;
    public static final int ROBOT_ROOSTER_MODEL_ID = 900001;
    public static final int ROBOT_CHICKEN_MODEL_ID = 900002;

    private ChickenItemHelper() {
    }

    public static void setChickenType(ItemStack stack, int type) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_CHICKEN_TYPE, type));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type));
    }

    public static int getChickenType(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(TAG_CHICKEN_TYPE)) {
            int type = data.copyTag().getInt(TAG_CHICKEN_TYPE);
            CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (modelData == null || modelData.value() != type) {
                // Ensure the item displays with the correct baked model, even if an older stack
                // or command-generated item forgot to sync the model data component.
                stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type));
            }
            return type;
        }
        return 0;
    }

    /**
     * Marks the provided stack as representing a rooster rather than a standard
     * ChickensChicken. Rooster stacks use a dedicated custom model id so the
     * item renderer can swap to textures/item/rooster.png.
     */
    public static void setRooster(ItemStack stack, boolean rooster) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (rooster) {
                tag.putBoolean(TAG_ROOSTER, true);
            } else {
                tag.remove(TAG_ROOSTER);
            }
        });
    }

    public static boolean isRooster(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(TAG_ROOSTER) && data.copyTag().getBoolean(TAG_ROOSTER);
    }

    public static void setRobotChicken(ItemStack stack, boolean robotChicken) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (robotChicken) {
                tag.putBoolean(TAG_ROBOT_CHICKEN, true);
            } else {
                tag.remove(TAG_ROBOT_CHICKEN);
                tag.remove(TAG_ROBOT_UPGRADES_GIVEN);
                tag.remove(TAG_ROBOT_UPGRADES_REQUIRED);
            }
        });
    }

    public static boolean isRobotChicken(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(TAG_ROBOT_CHICKEN) && data.copyTag().getBoolean(TAG_ROBOT_CHICKEN);
    }

    public static void setRobotRooster(ItemStack stack, boolean robotRooster) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (robotRooster) {
                tag.putBoolean(TAG_ROOSTER, true);
                tag.putBoolean(TAG_ROBOT_ROOSTER, true);
            } else {
                tag.remove(TAG_ROBOT_ROOSTER);
            }
        });
    }

    public static boolean isRobotRooster(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(TAG_ROBOT_ROOSTER) && data.copyTag().getBoolean(TAG_ROBOT_ROOSTER);
    }

    @Nullable
    public static ChickensRegistryItem resolve(ItemStack stack) {
        if (isRooster(stack)) {
            // Rooster stacks do not map to a ChickensRegistryItem; callers that
            // need rooster data should consult RoosterItemData instead.
            return null;
        }
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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (chicken.isRobotChicken()) {
                tag.putBoolean(TAG_ROBOT_CHICKEN, true);
            } else {
                tag.remove(TAG_ROBOT_CHICKEN);
            }
            tag.putInt(TAG_ROBOT_UPGRADES_GIVEN, chicken.getRobotUpgradesGiven());
            tag.putInt(TAG_ROBOT_UPGRADES_REQUIRED, chicken.getRobotUpgradesRequired());
        });
    }

    public static void applyToEntity(ItemStack stack, ChickensChicken chicken) {
        chicken.setChickenType(getChickenType(stack));
        ChickenStats stats = getStats(stack);
        CompoundTag tag = stats.toTag();
        tag.putInt("Type", getChickenType(stack));
        tag.putBoolean(TAG_ROBOT_CHICKEN, isRobotChicken(stack));
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag custom = data.copyTag();
        tag.putInt(TAG_ROBOT_UPGRADES_GIVEN, custom.getInt(TAG_ROBOT_UPGRADES_GIVEN));
        tag.putInt(TAG_ROBOT_UPGRADES_REQUIRED, custom.getInt(TAG_ROBOT_UPGRADES_REQUIRED));
        chicken.readAdditionalSaveData(tag);
        chicken.setStatsAnalyzed(stats.analysed());
    }

    public static boolean isChicken(ItemStack stack) {
        return stack.getItem() instanceof ChickenItem;
    }
}
