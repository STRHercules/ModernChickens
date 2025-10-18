package com.setycz.chickens.item;

import net.minecraft.nbt.CompoundTag;

/**
 * Immutable snapshot of a chicken's growth, gain, and strength values. The
 * roost-style containers and chicken item both serialize these values so the
 * captured bird retains its breeding potential when moved around the world.
 */
public record ChickenStats(int growth, int gain, int strength, boolean analysed) {
    private static final String TAG_GROWTH = "Growth";
    private static final String TAG_GAIN = "Gain";
    private static final String TAG_STRENGTH = "Strength";
    private static final String TAG_ANALYSED = "Analyzed";

    public static final ChickenStats DEFAULT = new ChickenStats(1, 1, 1, false);

    public ChickenStats {
        // Clamp the stored stats to the vanilla Chickens range (1-10) so any
        // malformed data coming from configs or older saves cannot break the
        // modern containers.
        growth = clamp(growth);
        gain = clamp(gain);
        strength = clamp(strength);
    }

    private static int clamp(int value) {
        if (value < 1) {
            return 1;
        }
        if (value > 10) {
            return 10;
        }
        return value;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_GROWTH, growth);
        tag.putInt(TAG_GAIN, gain);
        tag.putInt(TAG_STRENGTH, strength);
        tag.putBoolean(TAG_ANALYSED, analysed);
        return tag;
    }

    public static ChickenStats fromTag(CompoundTag tag) {
        if (tag == null) {
            return DEFAULT;
        }
        int growth = tag.getInt(TAG_GROWTH);
        int gain = tag.getInt(TAG_GAIN);
        int strength = tag.getInt(TAG_STRENGTH);
        boolean analysed = tag.getBoolean(TAG_ANALYSED);
        return new ChickenStats(growth, gain, strength, analysed);
    }
}
