package strhercules.chickens.integration.kubejs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Fluent KubeJS modifier for a chicken that already exists in the roster. */
public final class KubeChickenModifier {
    private final String chickenName;

    @Nullable
    private ItemStack layItem;
    @Nullable
    private ItemStack dropItem;
    @Nullable
    private String displayName;
    @Nullable
    private String parent1;
    @Nullable
    private String parent2;
    private boolean parentsSet;
    @Nullable
    private Integer tier;
    @Nullable
    private SpawnType spawnType;
    @Nullable
    private Integer primaryColor;
    @Nullable
    private Integer secondaryColor;
    @Nullable
    private Float layCoefficient;
    @Nullable
    private Boolean generatedTexture;
    @Nullable
    private String itemTexture;
    @Nullable
    private Boolean enabled;
    @Nullable
    private Boolean naturalSpawn;
    @Nullable
    private Boolean dousingAllowed;
    @Nullable
    private Integer liquidDousingCost;
    @Nullable
    private Integer spawnWeight;
    private boolean spawnWeightSet;

    KubeChickenModifier(String chickenName) {
        this.chickenName = chickenName;
    }

    public KubeChickenModifier layItem(Object value) {
        layItem = parseItem(value, -1);
        return this;
    }

    public KubeChickenModifier layItem(Object value, int count) {
        layItem = parseItem(value, count);
        return this;
    }

    public KubeChickenModifier dropItem(Object value) {
        dropItem = parseItem(value, -1);
        return this;
    }

    public KubeChickenModifier dropItem(Object value, int count) {
        dropItem = parseItem(value, count);
        return this;
    }

    public KubeChickenModifier displayName(String value) {
        displayName = value;
        return this;
    }

    public KubeChickenModifier parents(String first, String second) {
        parent1 = normaliseName(first);
        parent2 = normaliseName(second);
        parentsSet = true;
        return this;
    }

    public KubeChickenModifier parent1(String value) {
        parent1 = normaliseName(value);
        parentsSet = true;
        return this;
    }

    public KubeChickenModifier parent2(String value) {
        parent2 = normaliseName(value);
        parentsSet = true;
        return this;
    }

    public KubeChickenModifier clearParents() {
        parent1 = null;
        parent2 = null;
        parentsSet = true;
        return this;
    }

    public KubeChickenModifier tier(int value) {
        tier = Math.max(1, value);
        return this;
    }

    public KubeChickenModifier spawnType(String value) {
        if (value == null || value.isBlank()) {
            spawnType = SpawnType.NONE;
            return this;
        }
        try {
            spawnType = SpawnType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' has an unknown spawnType '{}'; ignoring",
                    chickenName, value);
        }
        return this;
    }

    public KubeChickenModifier primaryColor(Object value) {
        primaryColor = parseColour(value, 0xFFFFFF);
        return this;
    }

    public KubeChickenModifier secondaryColor(Object value) {
        secondaryColor = parseColour(value, 0xFFFF00);
        return this;
    }

    public KubeChickenModifier layCoefficient(double value) {
        layCoefficient = (float) Math.max(0.0D, value);
        return this;
    }

    public KubeChickenModifier generatedTexture(boolean value) {
        generatedTexture = value;
        return this;
    }

    public KubeChickenModifier itemTexture(String value) {
        itemTexture = value;
        return this;
    }

    public KubeChickenModifier enabled(boolean value) {
        enabled = value;
        return this;
    }

    public KubeChickenModifier allowNaturalSpawn(boolean value) {
        naturalSpawn = value;
        return this;
    }

    public KubeChickenModifier allowDousing(boolean value) {
        dousingAllowed = value;
        return this;
    }

    public KubeChickenModifier liquidDousingCost(int value) {
        liquidDousingCost = Math.max(1, value);
        return this;
    }

    public KubeChickenModifier spawnWeight(int value) {
        spawnWeight = Math.max(0, value);
        spawnWeightSet = true;
        return this;
    }

    public KubeChickenModifier clearSpawnWeight() {
        spawnWeight = null;
        spawnWeightSet = true;
        return this;
    }

    void apply(ChickensRegistryItem chicken, Map<String, ChickensRegistryItem> byName) {
        if (layItem != null) {
            chicken.setLayItem(layItem);
        }
        if (dropItem != null) {
            chicken.setDropItem(dropItem);
        }
        if (displayName != null) {
            chicken.setDisplayName(Component.literal(displayName));
        }
        if (parentsSet) {
            ChickensRegistryItem resolvedParent1 = resolveParent(parent1, byName);
            ChickensRegistryItem resolvedParent2 = resolveParent(parent2, byName);
            if (resolvedParent1 != null && resolvedParent2 != null
                    && resolvedParent1 != chicken && resolvedParent2 != chicken
                    && !containsParent(resolvedParent1, chicken)
                    && !containsParent(resolvedParent2, chicken)) {
                chicken.setParentsNew(resolvedParent1, resolvedParent2);
            } else if (parent1 == null && parent2 == null) {
                chicken.setNoParents();
            } else {
                ChickenRegistryKubeEvent.LOGGER.warn(
                        "Chicken modifier '{}' has unusable parents '{}', '{}'; clearing its breeding data",
                        chickenName, parent1, parent2);
                chicken.setNoParents();
            }
        }
        if (tier != null) {
            chicken.setTier(tier);
        }
        if (spawnType != null) {
            chicken.setSpawnType(spawnType);
        }
        if (primaryColor != null) {
            chicken.setBgColor(primaryColor);
        }
        if (secondaryColor != null) {
            chicken.setFgColor(secondaryColor);
        }
        if (layCoefficient != null) {
            chicken.setLayCoefficient(layCoefficient);
        }
        if (generatedTexture != null) {
            chicken.setGeneratedTexture(generatedTexture);
        }
        if (itemTexture != null && !itemTexture.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(itemTexture);
            if (parsed != null) {
                chicken.setItemTexture(parsed);
            } else {
                ChickenRegistryKubeEvent.LOGGER.warn(
                        "Chicken modifier '{}' has a malformed itemTexture '{}'; ignoring", chickenName, itemTexture);
            }
        }
        if (enabled != null) {
            chicken.setEnabled(enabled);
        }
        if (naturalSpawn != null) {
            chicken.setNaturalSpawnOverride(naturalSpawn);
        }
        if (dousingAllowed != null) {
            chicken.setDousingAllowed(dousingAllowed);
        }
        if (liquidDousingCost != null) {
            chicken.setLiquidDousingCost(liquidDousingCost);
        }
        if (spawnWeightSet) {
            if (spawnWeight == null) {
                chicken.clearSpawnWeightOverride();
            } else {
                chicken.setSpawnWeight(spawnWeight);
            }
        }
    }

    @Nullable
    private ChickensRegistryItem resolveParent(@Nullable String name, Map<String, ChickensRegistryItem> byName) {
        if (name == null) {
            return null;
        }
        ChickensRegistryItem parent = byName.get(name);
        if (parent == null) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' references unknown parent '{}'",
                    chickenName, name);
        }
        return parent;
    }

    private static boolean containsParent(@Nullable ChickensRegistryItem current, ChickensRegistryItem target) {
        return containsParent(current, target, new HashSet<>());
    }

    private static boolean containsParent(@Nullable ChickensRegistryItem current,
            ChickensRegistryItem target, Set<ChickensRegistryItem> visited) {
        if (current == null || !visited.add(current)) {
            return false;
        }
        return current == target
                || containsParent(current.getParent1(), target, visited)
                || containsParent(current.getParent2(), target, visited);
    }

    @Nullable
    private ItemStack parseItem(Object value, int count) {
        if (value instanceof ItemStack stack) {
            ItemStack copy = stack.copy();
            int requestedCount = count > 0 ? count : copy.getCount();
            copy.setCount(Math.max(1, Math.min(requestedCount, copy.getMaxStackSize())));
            return copy;
        }
        if (value instanceof Item item) {
            return withCount(new ItemStack(item), count > 0 ? count : 1);
        }
        if (value == null) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' received a null item", chickenName);
            return null;
        }

        String raw = value.toString().trim();
        int parsedCount = count > 0 ? count : 1;
        int separator = raw.indexOf('x');
        if (separator > 0) {
            try {
                parsedCount = Integer.parseInt(raw.substring(0, separator).trim());
                raw = raw.substring(separator + 1).trim();
            } catch (NumberFormatException ignored) {
                // Treat the full value as an item id below.
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' could not resolve item '{}'; ignoring",
                    chickenName, value);
            return null;
        }
        return withCount(new ItemStack(item), parsedCount);
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        stack.setCount(Math.max(1, Math.min(count, stack.getMaxStackSize())));
        return stack;
    }

    @Nullable
    private static String normaliseName(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int separator = trimmed.indexOf(':');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }

    private int parseColour(Object value, int fallback) {
        if (value instanceof Number number) {
            return clampColour(number.intValue(), fallback, value);
        }
        if (value != null) {
            String raw = value.toString().trim();
            if (raw.startsWith("#")) {
                raw = raw.substring(1);
            } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
                raw = raw.substring(2);
            }
            try {
                return clampColour(Integer.parseUnsignedInt(raw, 16), fallback, value);
            } catch (NumberFormatException ignored) {
                // Fall through to the warning below.
            }
        }
        ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' could not parse colour '{}'; using default",
                chickenName, value);
        return fallback;
    }

    private int clampColour(int value, int fallback, Object raw) {
        if (value < 0 || value > 0xFFFFFF) {
            ChickenRegistryKubeEvent.LOGGER.warn("Chicken modifier '{}' has an out-of-range colour '{}'; using default",
                    chickenName, raw);
            return fallback;
        }
        return value;
    }
}
