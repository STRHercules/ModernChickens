package strhercules.chickens.data;

import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.item.ChemicalEggItem;
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
 * Creates the chicken that the Avian Dousing Machine targets for each
 * discovered Mekanism chemical, including radioactive chemicals.
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
        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            attemptRegistration(chickens, byName, false);
        }
    }

    static void refresh() {
        if (ChickensConfigHolder.get().isChemicalChickensEnabled()) {
            attemptRegistration(null, buildRegistryIndex(), true);
        }
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
            Map<String, ChickensRegistryItem> byName, boolean registerImmediately) {
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        int created = 0;
        Set<ResourceLocation> registeredChemicals = new HashSet<>();
        for (ChemicalEggRegistryItem entry : ChemicalEggRegistry.getAll()) {
            if (!registeredChemicals.add(entry.getChemicalId())) {
                continue;
            }

            ItemStack layStack = ChemicalEggItem.createFor(entry);
            if (layStack.isEmpty() || alreadyRepresents(byName.values(), layStack)) {
                continue;
            }

            String entityName = buildEntityName(entry.getChemicalId());
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                continue;
            }

            int primaryColor = entry.getEggColor();
            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    allocateId(entry.getChemicalId(), usedIds),
                    entityName,
                    PLACEHOLDER_TEXTURE,
                    layStack,
                    primaryColor,
                    accentColor(primaryColor));
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
        Map<String, ChickensRegistryItem> index = new HashMap<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            index.put(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
            index.putIfAbsent(chicken.getEntityName().toLowerCase(Locale.ROOT), chicken);
        }
        return index;
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
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)
                    || ItemStack.isSameItemSameComponents(chicken.createDropItem(), layStack)) {
                return true;
            }
        }
        return false;
    }

    private static Component buildDisplayName(ChemicalEggRegistryItem entry) {
        return entry.getDisplayName().copy().append(Component.literal(" Chicken"));
    }

    private static int allocateId(ResourceLocation chemicalId, Set<Integer> usedIds) {
        int candidate = ID_BASE + Math.floorMod(chemicalId.hashCode(), ID_SPAN);
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static String buildEntityName(ResourceLocation chemicalId) {
        String combined = chemicalId.getNamespace() + "_" + chemicalId.getPath();
        String[] parts = combined.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return "chemicalChicken";
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return builder.append("Chicken").toString();
    }

    private static int accentColor(int base) {
        int r = Math.min(0xFF, ((base >> 16) & 0xFF) + 0x30);
        int g = Math.min(0xFF, ((base >> 8) & 0xFF) + 0x30);
        int b = Math.min(0xFF, (base & 0xFF) + 0x30);
        return (r << 16) | (g << 8) | b;
    }
}
