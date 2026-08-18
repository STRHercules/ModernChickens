package strhercules.chickens.item;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.entity.ChickensChicken;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.function.Consumer;

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
    private static final String TAG_CUSTOM_MODEL_DATA = "CustomModelData";
    // Reserved custom model id used for rooster stacks so the chicken item
    // model can swap to the dedicated rooster sprite.
    public static final int ROOSTER_MODEL_ID = 900000;
    public static final int ROBOT_ROOSTER_MODEL_ID = 900001;
    public static final int ROBOT_CHICKEN_MODEL_ID = 900002;

    private ChickenItemHelper() {
    }

    private static void update(ItemStack stack, Consumer<CompoundTag> mutator) {
        mutator.accept(stack.getOrCreateTag());
    }

    private static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
    }

    private static boolean isRobotChickenItem(ItemStack stack) {
        return stack.is(ModRegistry.ROBOT_CHICKEN_ITEM.get())
                || stack.is(ModRegistry.ROBOT_ROOSTER_ITEM.get());
    }

    public static void setChickenType(ItemStack stack, int type) {
        update(stack, tag -> {
            tag.putInt(TAG_CHICKEN_TYPE, type);
            tag.putInt(TAG_CUSTOM_MODEL_DATA, type);
        });
    }

    public static int getChickenType(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data.contains(TAG_CHICKEN_TYPE)) {
            int type = data.getInt(TAG_CHICKEN_TYPE);
            if (!data.contains(TAG_CUSTOM_MODEL_DATA) || data.getInt(TAG_CUSTOM_MODEL_DATA) != type) {
                // Ensure the item displays with the correct baked model, even if an older stack
                // or command-generated item forgot to sync the model data tag.
                stack.getOrCreateTag().putInt(TAG_CUSTOM_MODEL_DATA, type);
            }
            return type;
        }
        if (isRobotChickenItem(stack)) {
            return ChickensRegistry.SMART_CHICKEN_ID;
        }
        return 0;
    }

    /**
     * Marks the provided stack as representing a rooster rather than a standard
     * ChickensChicken. Rooster stacks use a dedicated custom model id so the
     * item renderer can swap to textures/item/rooster.png.
     */
    public static void setRooster(ItemStack stack, boolean rooster) {
        update(stack, tag -> {
            if (rooster) {
                tag.putBoolean(TAG_ROOSTER, true);
            } else {
                tag.remove(TAG_ROOSTER);
            }
        });
    }

    public static boolean isRooster(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data.contains(TAG_ROOSTER)) {
            return data.getBoolean(TAG_ROOSTER);
        }
        return stack.is(ModRegistry.ROBOT_ROOSTER_ITEM.get());
    }

    public static void setRobotChicken(ItemStack stack, boolean robotChicken) {
        update(stack, tag -> {
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
        CompoundTag data = read(stack);
        if (data.contains(TAG_ROBOT_CHICKEN)) {
            return data.getBoolean(TAG_ROBOT_CHICKEN);
        }
        return isRobotChickenItem(stack);
    }

    public static void setRobotRooster(ItemStack stack, boolean robotRooster) {
        update(stack, tag -> {
            if (robotRooster) {
                tag.putBoolean(TAG_ROOSTER, true);
                tag.putBoolean(TAG_ROBOT_ROOSTER, true);
            } else {
                tag.remove(TAG_ROBOT_ROOSTER);
            }
        });
    }

    public static boolean isRobotRooster(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data.contains(TAG_ROBOT_ROOSTER)) {
            return data.getBoolean(TAG_ROBOT_ROOSTER);
        }
        return stack.is(ModRegistry.ROBOT_ROOSTER_ITEM.get());
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
        update(stack, tag -> tag.put(TAG_STATS, stats.toTag()));
    }

    public static ChickenStats getStats(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data.contains(TAG_STATS)) {
            return ChickenStats.fromTag(data.getCompound(TAG_STATS));
        }
        return ChickenStats.DEFAULT;
    }

    public static void copyFromEntity(ItemStack stack, ChickensChicken chicken) {
        setChickenType(stack, chicken.getChickenType());
        setStats(stack, new ChickenStats(chicken.getGrowth(), chicken.getGain(), chicken.getStrength(),
                chicken.getStatsAnalyzed()));
        update(stack, tag -> {
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
        CompoundTag custom = read(stack);
        tag.putInt(TAG_ROBOT_UPGRADES_GIVEN, custom.getInt(TAG_ROBOT_UPGRADES_GIVEN));
        tag.putInt(TAG_ROBOT_UPGRADES_REQUIRED, custom.getInt(TAG_ROBOT_UPGRADES_REQUIRED));
        chicken.readAdditionalSaveData(tag);
        chicken.setStatsAnalyzed(stats.analysed());
    }

    public static boolean isChicken(ItemStack stack) {
        return stack.getItem() instanceof ChickenItem;
    }
}
