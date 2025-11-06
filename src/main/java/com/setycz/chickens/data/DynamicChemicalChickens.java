package com.setycz.chickens.data;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.item.ChemicalEggItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dynamically creates chickens for every registered chemical egg. Mirrors the
 * fluid implementation so Mekanism chemicals are immediately accessible as part
 * of the chicken ecosystem.
 */
final class DynamicChemicalChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamicChemical");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    private static final int ID_BASE = 4_500_000;
    private static final int ID_SPAN = 500_000;

    private DynamicChemicalChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        if (!ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            return;
        }
        attemptRegistration(chickens, byName, false);
    }

    static void refresh() {
        if (!ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            return;
        }
        Map<String, ChickensRegistryItem> index = buildRegistryIndex();
        attemptRegistration(null, index, true);
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
                                            Map<String, ChickensRegistryItem> byName,
                                            boolean registerImmediately) {
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        int created = 0;
        Set<String> registeredKeys = new HashSet<>();
        for (ChemicalEggRegistryItem entry : ChemicalEggRegistry.getAll()) {
            String uniqueKey = entry.getChemicalId().toString();
            if (!registeredKeys.add(uniqueKey)) {
                continue;
            }

            ItemStack layStack = ChemicalEggItem.createFor(entry);
            if (layStack.isEmpty() || alreadyRepresents(byName.values(), layStack)) {
                continue;
            }

            String entityName = buildEntityName(entry.getChemicalId(), byName);
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                continue;
            }

            int primaryColor = entry.getEggColor();
            int accentColor = accentColor(primaryColor);

            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    allocateId(entry.getChemicalId(), usedIds),
                    entityName,
                    PLACEHOLDER_TEXTURE,
                    layStack,
                    primaryColor,
                    accentColor);
            chicken.setGeneratedTexture(true);
            chicken.setSpawnType(SpawnType.NONE);
            chicken.setDisplayName(buildDisplayName(entry));
            chicken.setNoParents();

            byName.put(nameKey, chicken);
            if (collector != null) {
                collector.add(chicken);
            }
            if (registerImmediately) {
                ChickensRegistry.register(chicken);
            }
            created++;
        }

        if (created > 0) {
            LOGGER.info("Registered {} dynamic chemical chickens", created);
        }
    }

    private static Map<String, ChickensRegistryItem> buildRegistryIndex() {
        Map<String, ChickensRegistryItem> map = new HashMap<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            map.put(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
            map.putIfAbsent(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        return map;
    }

    private static Set<Integer> collectUsedIds(@Nullable List<ChickensRegistryItem> collector,
                                               Map<String, ChickensRegistryItem> byName) {
        Set<Integer> ids = new HashSet<>();
        if (collector != null) {
            for (ChickensRegistryItem chicken : collector) {
                ids.add(chicken.getId());
            }
        } else {
            for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
                ids.add(chicken.getId());
            }
            for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
                ids.add(chicken.getId());
            }
        }
        for (ChickensRegistryItem chicken : byName.values()) {
            ids.add(chicken.getId());
        }
        return ids;
    }

    private static boolean alreadyRepresents(Iterable<ChickensRegistryItem> chickens, ItemStack layStack) {
        for (ChickensRegistryItem chicken : chickens) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(chicken.createDropItem(), layStack)) {
                return true;
            }
        }
        return false;
    }

    private static Component buildDisplayName(ChemicalEggRegistryItem entry) {
        return entry.getDisplayName().copy().append(Component.literal(" Chemical Chicken"));
    }

    private static int allocateId(ResourceLocation chemicalId, Set<Integer> usedIds) {
        int seed = Math.floorMod(chemicalId.hashCode(), ID_SPAN);
        int candidate = ID_BASE + seed;
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static String buildEntityName(ResourceLocation chemicalId,
                                          Map<String, ChickensRegistryItem> byName) {
        String baseName = buildBaseName(chemicalId);
        return resolveUniqueName(baseName, "Chemical", byName);
    }

    private static String buildBaseName(ResourceLocation chemicalId) {
        String combined = chemicalId.getNamespace() + "_" + chemicalId.getPath();
        String[] parts = combined.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return "chemical";
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private static String resolveUniqueName(String baseName,
                                            String variantSuffix,
                                            Map<String, ChickensRegistryItem> byName) {
        String candidate = baseName + "Chicken";
        if (isNameAvailable(candidate, byName)) {
            return candidate;
        }

        // Mekanism commonly exposes both liquid and chemical variants; prefer a semantic suffix before numbering.
        String variant = baseName + variantSuffix + "Chicken";
        if (isNameAvailable(variant, byName)) {
            return variant;
        }

        int counter = 2;
        while (true) {
            String numbered = baseName + variantSuffix + counter + "Chicken";
            if (isNameAvailable(numbered, byName)) {
                return numbered;
            }
            counter++;
        }
    }

    private static boolean isNameAvailable(String candidate, Map<String, ChickensRegistryItem> byName) {
        return !byName.containsKey(candidate.toLowerCase(Locale.ROOT));
    }

    private static int accentColor(int base) {
        int r = Math.min(0xFF, ((base >> 16) & 0xFF) + 0x30);
        int g = Math.min(0xFF, ((base >> 8) & 0xFF) + 0x30);
        int b = Math.min(0xFF, (base & 0xFF) + 0x30);
        return (r << 16) | (g << 8) | b;
    }
}
