package strhercules.chickens.data;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Placeholder for dynamic material chicken registration.
 * Auto-registration has been removed; chickens are defined via static data only.
 */
final class DynamicMaterialChickens {

    private DynamicMaterialChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        // Auto-registration disabled. No dynamic chickens will be created.
    }

    static void refresh() {
        // Auto-registration disabled. Nothing to refresh.
    }
}