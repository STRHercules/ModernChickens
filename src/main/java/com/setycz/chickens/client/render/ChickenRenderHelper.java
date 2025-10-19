package com.setycz.chickens.client.render;

import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenStats;
import com.setycz.chickens.registry.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Small utility used by block-entity renderers to obtain pre-configured chicken
 * entities. Rendering reuses a cached instance per chicken type which mirrors
 * the original Roost baked-model behaviour without allocating a new entity
 * every frame.
 */
public final class ChickenRenderHelper {
    private static final String TAG_TYPE = "Type";
    private static final Map<Integer, ChickensChicken> CACHE = new HashMap<>();

    private ChickenRenderHelper() {
    }

    /**
     * Returns a client-side chicken entity for the given registry id. The
     * entity is reinitialised with the supplied stats to keep renderers in sync
     * with container contents.
     */
    @Nullable
    public static ChickensChicken getChicken(int type, ChickenStats stats) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return null;
        }
        ChickensChicken chicken = CACHE.compute(type, (key, existing) -> {
            if (existing == null || existing.level() != level) {
                ChickensChicken created = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
                if (created == null) {
                    return null;
                }
                return created;
            }
            return existing;
        });
        if (chicken == null) {
            return null;
        }
        applyStats(chicken, type, stats);
        chicken.tickCount = (int) level.getGameTime();
        return chicken;
    }

    private static void applyStats(ChickensChicken chicken, int type, ChickenStats stats) {
        CompoundTag tag = stats.toTag();
        tag.putInt(TAG_TYPE, type);
        chicken.readAdditionalSaveData(tag);
        chicken.setChickenType(type);
        chicken.setAge(0);
    }

    /**
     * Resets the orientation of a chicken so renderers start from a neutral pose.
     */
    public static void resetPose(ChickensChicken chicken) {
        chicken.setYRot(0.0F);
        chicken.setXRot(0.0F);
        chicken.setYBodyRot(0.0F);
        chicken.yBodyRotO = 0.0F;
        chicken.setYHeadRot(0.0F);
        chicken.yHeadRotO = 0.0F;
        chicken.tickCount = 0;
    }
}
