package com.setycz.chickens;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Straightforward port of the legacy registry that keeps track of the
 * different liquid egg variants. The actual item implementation has not
 * been modernised yet, but the registry is required so that chicken data
 * and future fluid logic can resolve identifiers.
 */
public final class LiquidEggRegistry {
    private static final Map<Integer, LiquidEggRegistryItem> ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, LiquidEggRegistryItem> BY_FLUID = new HashMap<>();

    private LiquidEggRegistry() {
    }

    public static void register(LiquidEggRegistryItem liquidEgg) {
        ITEMS.put(liquidEgg.getId(), liquidEgg);
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(liquidEgg.getFluid());
        if (fluidId != null) {
            BY_FLUID.put(fluidId, liquidEgg);
        }
    }

    public static Collection<LiquidEggRegistryItem> getAll() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    public static LiquidEggRegistryItem findById(int id) {
        return ITEMS.get(id);
    }

    @Nullable
    public static LiquidEggRegistryItem findByFluid(ResourceLocation fluidId) {
        return BY_FLUID.get(fluidId);
    }
}
