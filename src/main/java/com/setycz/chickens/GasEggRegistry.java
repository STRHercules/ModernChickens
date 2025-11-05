package com.setycz.chickens;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry entry map for gaseous chemicals. Uses the same data format as {@link ChemicalEggRegistry}.
 */
public final class GasEggRegistry {
    private static final Map<Integer, ChemicalEggRegistryItem> ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, ChemicalEggRegistryItem> BY_CHEMICAL = new HashMap<>();

    private GasEggRegistry() {
    }

    public static void register(ChemicalEggRegistryItem item) {
        ITEMS.put(item.getId(), item);
        BY_CHEMICAL.put(item.getChemicalId(), item);
    }

    public static Collection<ChemicalEggRegistryItem> getAll() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    @Nullable
    public static ChemicalEggRegistryItem findById(int id) {
        return ITEMS.get(id);
    }

    @Nullable
    public static ChemicalEggRegistryItem findByChemical(ResourceLocation id) {
        return BY_CHEMICAL.get(id);
    }

    public static void clear() {
        ITEMS.clear();
        BY_CHEMICAL.clear();
    }
}
