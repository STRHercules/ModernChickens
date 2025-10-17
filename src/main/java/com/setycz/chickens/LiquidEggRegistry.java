package com.setycz.chickens;

import java.util.Collection;
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

    private LiquidEggRegistry() {
    }

    public static void register(LiquidEggRegistryItem liquidEgg) {
        ITEMS.put(liquidEgg.getId(), liquidEgg);
    }

    public static Collection<LiquidEggRegistryItem> getAll() {
        return ITEMS.values();
    }

    public static LiquidEggRegistryItem findById(int id) {
        return ITEMS.get(id);
    }
}
