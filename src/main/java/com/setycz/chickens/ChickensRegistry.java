package com.setycz.chickens;

import net.minecraft.core.Holder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Central registry that keeps track of every chicken descriptor. Mirrors
 * the responsibilities of the legacy implementation but upgrades the
 * biome logic to make use of modern tag helpers.
 */
public final class ChickensRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensRegistry");
    private static final Map<Integer, ChickensRegistryItem> ITEMS = new HashMap<>();
    public static final int SMART_CHICKEN_ID = 50;
    private static final Random RAND = new Random();
    /**
     * Fallback lists that mimic the 1.12 spawn tiers so a broken configuration
     * never leaves the natural spawn pool empty.
     */
    private static final Map<SpawnType, List<FallbackChicken>> STARTER_SPAWNERS = Map.of(
            SpawnType.NORMAL, List.of(
                    fallback("FlintChicken", SpawnType.NORMAL),
                    fallback("LogChicken", SpawnType.NORMAL),
                    fallback("GunpowderChicken", SpawnType.NORMAL)),
            SpawnType.SNOW, List.of(
                    fallback("SnowballChicken", SpawnType.SNOW)),
            SpawnType.HELL, List.of(
                    fallback("QuartzChicken", SpawnType.HELL),
                    fallback("LavaChicken", SpawnType.HELL)));

    private ChickensRegistry() {
    }

    public static void register(ChickensRegistryItem entity) {
        validate(entity);
        ITEMS.put(entity.getId(), entity);
    }

    private static void validate(ChickensRegistryItem entity) {
        for (ChickensRegistryItem item : ITEMS.values()) {
            if (entity.getId() == item.getId()) {
                throw new IllegalStateException("Duplicate chicken id " + entity.getId());
            }
            if (entity.getEntityName().equalsIgnoreCase(item.getEntityName())) {
                throw new IllegalStateException("Duplicate chicken name " + entity.getEntityName());
            }
        }
    }

    public static ChickensRegistryItem getByType(int type) {
        return ITEMS.get(type);
    }

    public static Collection<ChickensRegistryItem> getItems() {
        List<ChickensRegistryItem> result = new ArrayList<>();
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (chicken.isEnabled()) {
                result.add(chicken);
            }
        }
        return result;
    }

    public static Collection<ChickensRegistryItem> getDisabledItems() {
        List<ChickensRegistryItem> result = new ArrayList<>();
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (!chicken.isEnabled()) {
                result.add(chicken);
            }
        }
        return result;
    }

    private static List<ChickensRegistryItem> getChildren(ChickensRegistryItem parent1, ChickensRegistryItem parent2) {
        List<ChickensRegistryItem> result = new ArrayList<>();
        if (parent1.isEnabled()) {
            result.add(parent1);
        }
        if (parent2.isEnabled()) {
            result.add(parent2);
        }
        for (ChickensRegistryItem item : ITEMS.values()) {
            if (item.isEnabled() && item.isChildOf(parent1, parent2)) {
                result.add(item);
            }
        }
        return result;
    }

    @Nullable
    public static ChickensRegistryItem findDyeChicken(net.minecraft.world.item.crafting.Ingredient colour) {
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (chicken.isDye(colour)) {
                return chicken;
            }
        }
        return null;
    }

    @Nullable
    public static ChickensRegistryItem findDyeChicken(DyeColor colour) {
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (chicken.isDye(colour)) {
                return chicken;
            }
        }
        return null;
    }

    public static List<ChickensRegistryItem> getPossibleChickensToSpawn(SpawnType spawnType) {
        List<ChickensRegistryItem> result = new ArrayList<>();
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (chicken.canSpawn() && chicken.getSpawnType() == spawnType && chicken.isEnabled()) {
                result.add(chicken);
            }
        }
        mergeStarterChickens(spawnType, result);
        return result;
    }

    public static SpawnType getSpawnType(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(Tags.Biomes.IS_NETHER)) {
            return SpawnType.HELL;
        }
        if (biomeHolder.is(Tags.Biomes.IS_SNOWY)) {
            return SpawnType.SNOW;
        }
        return SpawnType.NORMAL;
    }

    public static float getChildChance(ChickensRegistryItem child) {
        if (child.getTier() <= 1) {
            return 0;
        }
        List<ChickensRegistryItem> possibleChildren = getChildren(child.getParent1(), child.getParent2());
        int maxChance = getMaxChance(possibleChildren);
        int maxDiceValue = getMaxDiceValue(possibleChildren, maxChance);
        return ((maxChance - child.getTier()) * 100.0f) / maxDiceValue;
    }

    @Nullable
    public static ChickensRegistryItem getRandomChild(ChickensRegistryItem parent1, ChickensRegistryItem parent2) {
        List<ChickensRegistryItem> possibleChildren = getChildren(parent1, parent2);
        if (possibleChildren.isEmpty()) {
            return null;
        }
        int maxChance = getMaxChance(possibleChildren);
        int maxDiceValue = getMaxDiceValue(possibleChildren, maxChance);
        int diceValue = RAND.nextInt(maxDiceValue);
        return getChickenToBeBorn(possibleChildren, maxChance, diceValue);
    }

    @Nullable
    private static ChickensRegistryItem getChickenToBeBorn(List<ChickensRegistryItem> possibleChildren, int maxChance, int diceValue) {
        int currentValue = 0;
        for (ChickensRegistryItem child : possibleChildren) {
            currentValue += maxChance - child.getTier();
            if (diceValue < currentValue) {
                return child;
            }
        }
        return null;
    }

    private static int getMaxDiceValue(List<ChickensRegistryItem> possibleChildren, int maxChance) {
        int maxDiceValue = 0;
        for (ChickensRegistryItem child : possibleChildren) {
            maxDiceValue += maxChance - child.getTier();
        }
        return maxDiceValue;
    }

    private static int getMaxChance(List<ChickensRegistryItem> possibleChildren) {
        int maxChance = 0;
        for (ChickensRegistryItem child : possibleChildren) {
            maxChance = Math.max(maxChance, child.getTier());
        }
        return maxChance + 1;
    }

    public static boolean isAnyIn(SpawnType spawnType) {
        if (ITEMS.values().stream().anyMatch(chicken ->
                chicken.canSpawn() && chicken.isEnabled() && chicken.getSpawnType() == spawnType)) {
            return true;
        }
        return !resolveFallbackChickens(spawnType).isEmpty();
    }

    @Nullable
    public static ChickensRegistryItem getSmartChicken() {
        return ITEMS.get(SMART_CHICKEN_ID);
    }

    @Nullable
    public static ChickensRegistryItem findByName(String entityName) {
        for (ChickensRegistryItem chicken : ITEMS.values()) {
            if (chicken.getEntityName().equalsIgnoreCase(entityName)) {
                return chicken;
            }
        }
        return null;
    }

    private static void mergeStarterChickens(SpawnType spawnType, List<ChickensRegistryItem> target) {
        List<ChickensRegistryItem> resolved = resolveFallbackChickens(spawnType);
        if (resolved.isEmpty()) {
            return;
        }
        List<String> addedNames = new ArrayList<>();
        boolean alreadyContainedAny = false;
        for (ChickensRegistryItem fallback : resolved) {
            boolean contained = target.contains(fallback);
            if (!contained) {
                target.add(fallback);
                addedNames.add(fallback.getEntityName());
            } else {
                alreadyContainedAny = true;
            }
        }
        if (!addedNames.isEmpty()) {
            LOGGER.warn("Added starter chickens {} to the {} spawn pool because configuration removed them", addedNames, spawnType);
        } else if (!resolved.isEmpty() && !alreadyContainedAny) {
            LOGGER.warn("Starter chickens for {} are enabled but already present in the spawn pool", spawnType);
        }
    }

    private static List<ChickensRegistryItem> resolveFallbackChickens(SpawnType spawnType) {
        List<FallbackChicken> definitions = STARTER_SPAWNERS.get(spawnType);
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        List<ChickensRegistryItem> resolved = new ArrayList<>();
        for (FallbackChicken definition : definitions) {
            ChickensRegistryItem fallback = findByName(definition.name());
            if (fallback == null || !fallback.isEnabled()) {
                continue;
            }
            if (fallback.getSpawnType() != definition.spawnType()) {
                fallback.setSpawnType(definition.spawnType());
            }
            resolved.add(fallback);
        }
        return resolved;
    }

    private static FallbackChicken fallback(String name, SpawnType spawnType) {
        return new FallbackChicken(name, spawnType);
    }

    private record FallbackChicken(String name, SpawnType spawnType) {
    }
}
