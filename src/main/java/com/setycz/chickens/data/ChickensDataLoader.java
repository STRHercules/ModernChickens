package com.setycz.chickens.data;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Handles the legacy configuration file that drives chicken definitions.
 * The original release stored everything in a Forge {@code .cfg}; here we
 * emulate that behaviour with a simple {@link Properties} file so that we
 * can preserve the mod's customisation surface without waiting for the GUI
 * tooling to be ported.
 */
public final class ChickensDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensData");
    private static final String CONFIG_FILE = "chickens.properties";

    private ChickensDataLoader() {
    }

    public static void bootstrap() {
        registerLiquidEggs();
        List<ChickensRegistryItem> defaults = DefaultChickens.create();
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        ChickensConfigValues values = applyConfiguration(configFile, defaults);
        ChickensConfigHolder.set(values);
        defaults.forEach(ChickensRegistry::register);

        LOGGER.info("Loaded {} chickens ({} enabled, {} disabled)",
                defaults.size(),
                ChickensRegistry.getItems().size(),
                ChickensRegistry.getDisabledItems().size());

        // Export the breeding graph during bootstrap so tooling retains the
        // legacy log output without waiting for command invocation.
        BreedingGraphExporter.export(ChickensRegistry.getItems());
    }

    private static void registerLiquidEggs() {
        LiquidEggRegistry.register(new LiquidEggRegistryItem(0, Blocks.WATER, 0x0000ff, Fluids.WATER));
        LiquidEggRegistry.register(new LiquidEggRegistryItem(1, Blocks.LAVA, 0xff0000, Fluids.LAVA));
    }

    private static ChickensConfigValues applyConfiguration(Path configFile, List<ChickensRegistryItem> chickens) {
        Properties props = new Properties();
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                props.load(reader);
            } catch (IOException e) {
                LOGGER.warn("Failed to read chickens configuration; using defaults", e);
            }
        }

        ChickensConfigValues values = readGeneralSettings(props);
        Map<String, ChickensRegistryItem> byName = new HashMap<>();
        for (ChickensRegistryItem chicken : chickens) {
            byName.put(chicken.getEntityName(), chicken);
        }

        Map<ChickensRegistryItem, ParentNames> parentOverrides = new HashMap<>();
        for (ChickensRegistryItem chicken : chickens) {
            String prefix = "chicken." + chicken.getEntityName() + ".";
            boolean enabled = readBoolean(props, prefix + "enabled", chicken.isEnabled());
            chicken.setEnabled(enabled);

            float layCoefficient = readFloat(props, prefix + "layCoefficient", 1.0f);
            chicken.setLayCoefficient(layCoefficient);

            ItemStack defaultEgg = chicken.createLayItem();
            ItemStack layItem = readItemStack(props,
                    prefix + "eggItem",
                    prefix + "eggCount",
                    prefix + "eggType",
                    defaultEgg);
            chicken.setLayItem(layItem);

            ItemStack defaultDrop = chicken.createDropItem();
            ItemStack dropItem = readItemStack(props,
                    prefix + "dropItem",
                    prefix + "dropCount",
                    prefix + "dropType",
                    defaultDrop);
            chicken.setDropItem(dropItem);

            String parent1 = readString(props, prefix + "parent1", chicken.getParent1() != null ? chicken.getParent1().getEntityName() : "");
            String parent2 = readString(props, prefix + "parent2", chicken.getParent2() != null ? chicken.getParent2().getEntityName() : "");
            parentOverrides.put(chicken, new ParentNames(parent1, parent2));

            String spawnTypeName = readString(props, prefix + "spawnType", chicken.getSpawnType().name());
            SpawnType spawnType = parseSpawnType(spawnTypeName, chicken.getSpawnType());
            chicken.setSpawnType(spawnType);
        }

        for (Map.Entry<ChickensRegistryItem, ParentNames> entry : parentOverrides.entrySet()) {
            ChickensRegistryItem chicken = entry.getKey();
            ParentNames parents = entry.getValue();
            ChickensRegistryItem parent1 = resolveParent(byName, parents.parent1());
            ChickensRegistryItem parent2 = resolveParent(byName, parents.parent2());
            if (parent1 != null && parent2 != null) {
                chicken.setParentsNew(parent1, parent2);
            } else {
                chicken.setNoParents();
            }
        }

        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                props.store(writer, "Modern Chickens configuration");
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to write chickens configuration", e);
        }

        return values;
    }

    private static ChickensRegistryItem resolveParent(Map<String, ChickensRegistryItem> byName, String parentName) {
        if (parentName == null || parentName.isEmpty()) {
            return null;
        }
        ChickensRegistryItem parent = byName.get(parentName);
        if (parent == null) {
            LOGGER.warn("Unknown parent '{}' referenced in configuration", parentName);
        }
        return parent;
    }

    private static SpawnType parseSpawnType(String value, SpawnType fallback) {
        try {
            return SpawnType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Invalid spawn type '{}' in configuration, using {}", value, fallback);
            return fallback;
        }
    }

    private static ItemStack readItemStack(Properties props, String itemKey, String countKey, String typeKey, ItemStack fallback) {
        String defaultItemId = getItemId(fallback);
        String itemId = readItemId(props, itemKey, defaultItemId);
        int count = readItemCount(props, countKey, fallback.getCount());
        ItemStack stack = decodeItemStack(itemId, count);
        if (stack.isEmpty()) {
            // Persist defaults so the written configuration mirrors the in-game values.
            props.setProperty(itemKey, defaultItemId);
            props.setProperty(countKey, Integer.toString(fallback.getCount()));
            if (typeKey != null) {
                props.setProperty(typeKey, Integer.toString(readDefaultType(fallback)));
            }
            return fallback.copy();
        }

        if (stack.is(ModRegistry.LIQUID_EGG.get())) {
            int defaultType = readDefaultType(fallback);
            int type = readItemType(props, itemKey, typeKey, defaultType);
            ChickenItemHelper.setChickenType(stack, type);
            props.setProperty(typeKey, Integer.toString(type));
        } else if (typeKey != null) {
            props.remove(typeKey);
        }

        props.setProperty(itemKey, getItemId(stack));
        props.setProperty(countKey, Integer.toString(stack.getCount()));
        return stack;
    }

    private static int readDefaultType(ItemStack fallback) {
        return fallback.is(ModRegistry.LIQUID_EGG.get()) ? ChickenItemHelper.getChickenType(fallback) : 0;
    }

    private static String readItemId(Properties props, String itemKey, String defaultItemId) {
        String legacyKey = itemKey + "Name";
        String itemId = props.getProperty(itemKey);
        if (itemId == null) {
            itemId = props.getProperty(legacyKey);
        }
        if (itemId == null || itemId.isEmpty()) {
            itemId = defaultItemId;
        }
        props.setProperty(itemKey, itemId);
        return itemId;
    }

    private static int readItemCount(Properties props, String countKey, int defaultCount) {
        String legacyKey = countKey.replace("Count", "ItemAmount");
        String value = props.getProperty(countKey);
        if (value == null) {
            value = props.getProperty(legacyKey);
        }
        int count = parseInt(value, defaultCount);
        props.setProperty(countKey, Integer.toString(count));
        return count;
    }

    private static int readItemType(Properties props, String itemKey, String typeKey, int defaultType) {
        // Honour both the modern "type" key and the legacy metadata entry so
        // existing configuration files retain their liquid egg variants.
        String legacyKey = itemKey + "Meta";
        String value = props.getProperty(typeKey);
        if (value == null) {
            value = props.getProperty(legacyKey);
        }
        int type = parseInt(value, defaultType);
        return Math.max(type, 0);
    }

    private static int parseInt(String raw, int defaultValue) {
        if (raw == null || raw.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static ItemStack decodeItemStack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            LOGGER.warn("Malformed item identifier '{}' in configuration", itemId);
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null) {
            LOGGER.warn("Unknown item '{}' referenced in configuration", itemId);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(count, 1));
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.toString() : "minecraft:air";
    }

    private static ChickensConfigValues readGeneralSettings(Properties props) {
        int spawnProbability = readInt(props, "general.spawnProbability", 10);
        int minBroodSize = readInt(props, "general.minBroodSize", 3);
        int maxBroodSize = readInt(props, "general.maxBroodSize", 5);
        float multiplier = readFloat(props, "general.netherSpawnChanceMultiplier", 1.0f);
        boolean alwaysShowStats = readBoolean(props, "general.alwaysShowStats", false);
        return new ChickensConfigValues(spawnProbability, minBroodSize, maxBroodSize, multiplier, alwaysShowStats);
    }

    private static String readString(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            props.setProperty(key, defaultValue);
            return defaultValue;
        }
        return value;
    }

    private static boolean readBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            props.setProperty(key, Boolean.toString(defaultValue));
            return defaultValue;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        props.setProperty(key, Boolean.toString(defaultValue));
        return defaultValue;
    }

    private static int readInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            props.setProperty(key, Integer.toString(defaultValue));
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            props.setProperty(key, Integer.toString(defaultValue));
            return defaultValue;
        }
    }

    private static float readFloat(Properties props, String key, float defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            props.setProperty(key, Float.toString(defaultValue));
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            props.setProperty(key, Float.toString(defaultValue));
            return defaultValue;
        }
    }

    private record ParentNames(String parent1, String parent2) {
        ParentNames {
            Objects.requireNonNull(parent1);
            Objects.requireNonNull(parent2);
        }
    }
}
