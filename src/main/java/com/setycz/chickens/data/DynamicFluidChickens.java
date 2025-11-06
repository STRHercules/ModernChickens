package com.setycz.chickens.data;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.item.LiquidEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Dynamically creates chickens for every registered liquid egg so mod packs
 * automatically gain coverage for fluid resources introduced by other mods.
 * The chickens inherit the placeholder texture pipeline used by the dynamic
 * material generator, ensuring they integrate cleanly with the existing
 * rendering systems.
 */
final class DynamicFluidChickens {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensDynamicFluid");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/entity/unknownchicken.png");
    private static final int ID_BASE = 3_000_000;
    private static final int ID_SPAN = 1_000_000;

    private DynamicFluidChickens() {
    }

    static void register(List<ChickensRegistryItem> chickens, Map<String, ChickensRegistryItem> byName) {
        attemptRegistration(chickens, byName, false);
    }

    static void refresh() {
        Map<String, ChickensRegistryItem> index = buildRegistryIndex();
        attemptRegistration(null, index, true);
    }

    private static void attemptRegistration(@Nullable List<ChickensRegistryItem> collector,
                                            Map<String, ChickensRegistryItem> byName,
                                            boolean registerImmediately) {
        Set<Integer> usedIds = collectUsedIds(collector, byName);
        int created = 0;
        Set<String> registeredKeys = new HashSet<>();
        for (LiquidEggRegistryItem entry : LiquidEggRegistry.getAll()) {
            Fluid fluid = entry.getFluid();
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            if (fluidId == null) {
                continue;
            }
            String uniqueKey = fluidId.toString();
            if (!registeredKeys.add(uniqueKey)) {
                continue;
            }

            ItemStack layStack = LiquidEggItem.createFor(entry);
            if (layStack.isEmpty() || alreadyRepresents(byName.values(), layStack)) {
                continue;
            }

            String entityName = buildEntityName(fluidId);
            String nameKey = entityName.toLowerCase(Locale.ROOT);
            if (byName.containsKey(nameKey)) {
                continue;
            }

            int primaryColor = sanitizeColor(entry.getEggColor());
            int accentColor = accentColor(primaryColor);

            ChickensRegistryItem chicken = new ChickensRegistryItem(
                    allocateId(fluidId, usedIds),
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
            LOGGER.info("Registered {} dynamic fluid chickens", created);
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

    private static boolean alreadyRepresents(Collection<ChickensRegistryItem> chickens, ItemStack layStack) {
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

    private static Component buildDisplayName(LiquidEggRegistryItem entry) {
        Component fluidName = entry.getDisplayName();
        return fluidName.copy().append(Component.literal(" Chicken"));
    }

    private static int allocateId(ResourceLocation fluidId, Set<Integer> usedIds) {
        int seed = Math.floorMod(fluidId.toString().hashCode(), ID_SPAN);
        int candidate = ID_BASE + seed;
        while (usedIds.contains(candidate)) {
            candidate++;
        }
        usedIds.add(candidate);
        return candidate;
    }

    private static String buildEntityName(ResourceLocation fluidId) {
        String combined = fluidId.getNamespace() + "_" + fluidId.getPath();
        String[] parts = combined.split("[^a-z0-9]+");
        if (parts.length == 0) {
            return "fluidChicken";
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
        builder.append("Chicken");
        return builder.toString();
    }

    private static int sanitizeColor(int color) {
        int sanitized = color & 0x00FFFFFF;
        return sanitized != 0 ? sanitized : 0xFFFFFF;
    }

    private static int accentColor(int base) {
        int r = Math.min(0xFF, ((base >> 16) & 0xFF) + 0x30);
        int g = Math.min(0xFF, ((base >> 8) & 0xFF) + 0x30);
        int b = Math.min(0xFF, (base & 0xFF) + 0x30);
        return (r << 16) | (g << 8) | b;
    }
}
