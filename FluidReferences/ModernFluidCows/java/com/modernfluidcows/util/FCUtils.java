package com.modernfluidcows.util;

import com.modernfluidcows.config.FCConfig;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/** Utility helpers shared across the FluidCows port. */
public final class FCUtils {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<ResourceLocation, String> FLUID_NAME_CACHE = new ConcurrentHashMap<>();

    private FCUtils() {}

    /**
     * Returns every registered fluid that exposes a bucket. NeoForge exposes this via the registry,
     * so we filter by the presence of a bucket item instead of using reflection like the legacy mod.
     */
    public static Set<Fluid> getBucketFluids() {
        Set<Fluid> fluids = new LinkedHashSet<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (!fluid.defaultFluidState().isSource()) {
                continue;
            }
            if (fluid.getBucket() != null && fluid.getBucket() != Items.AIR) {
                fluids.add(fluid);
            }
        }
        return fluids;
    }

    /** Chooses a random enabled fluid using the configured spawn weights. */
    public static Fluid getRandFluid() {
        if (FCConfig.FLUIDS.isEmpty() || FCConfig.sumWeight <= 0) {
            return null;
        }
        double threshold = RANDOM.nextDouble() * FCConfig.sumWeight;
        int cumulative = 0;
        for (Fluid fluid : FCConfig.FLUIDS) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key == null) {
                continue;
            }
            cumulative += FCConfig.getRate(key);
            if (cumulative >= threshold) {
                return fluid;
            }
        }
        return FCConfig.FLUIDS.getLast();
    }

    /** Resolves a friendly fluid name, caching lookups for repeated tooltips. */
    public static String getFluidName(final Fluid fluid) {
        if (fluid == null) {
            return "ERROR";
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) {
            return "UNKNOWN";
        }
        return FLUID_NAME_CACHE.computeIfAbsent(key, ignored -> new FluidStack(fluid, FluidType.BUCKET_VOLUME)
                .getHoverName()
                .getString());
    }

    /** Convenience helper mirroring the legacy utility for time formatting. */
    public static String toTime(final int secondsToAdd, final String zero) {
        final int secondsPerMinute = 60;
        final int minutesPerHour = 60;
        final int hoursPerDay = 24;
        final int secondsPerHour = secondsPerMinute * minutesPerHour;
        final int secondsPerDay = secondsPerHour * hoursPerDay;

        int wrappedSeconds = ((secondsToAdd % secondsPerDay) + secondsPerDay) % secondsPerDay;
        int hours = wrappedSeconds / secondsPerHour;
        int minutes = (wrappedSeconds / secondsPerMinute) % minutesPerHour;
        int seconds = wrappedSeconds % secondsPerMinute;
        if (hours == 0 && minutes == 0 && seconds == 0) {
            return zero;
        }
        return hours != 0
                ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Recreates the legacy AABB helper used by the feeder and sorter to determine their work area.
     *
     * <p>The original mod offset the block centre outward along its facing direction, then expanded
     * the box horizontally while stretching two blocks upward. This retains that behaviour so range
     * upgrades continue to feel identical.</p>
     */
    public static AABB getWorkArea(final BlockPos pos, final Direction facing, final int size) {
        int clampedSize = Math.max(0, size);
        AABB bounds = new AABB(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        BlockPos offset = pos.relative(facing, clampedSize + 1);
        bounds = bounds.move(
                offset.getX() - pos.getX(), offset.getY() - pos.getY(), offset.getZ() - pos.getZ());
        double minX = bounds.minX - clampedSize;
        double minY = bounds.minY;
        double minZ = bounds.minZ - clampedSize;
        double maxX = bounds.maxX + clampedSize;
        double maxY = bounds.maxY + clampedSize * 2.0D;
        double maxZ = bounds.maxZ + clampedSize;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
