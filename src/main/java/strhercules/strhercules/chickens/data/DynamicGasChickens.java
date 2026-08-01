package strhercules.chickens.data;

import strhercules.chickens.ChickensRegistryItem;

import java.util.List;
import java.util.Map;

/**
 * Auto-registration of gas chickens has been removed.
 * Gas chickens are now defined exclusively via static data.
 */
final class DynamicGasChickens {
    private DynamicGasChickens() {}

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {}

    static void refresh() {}
}