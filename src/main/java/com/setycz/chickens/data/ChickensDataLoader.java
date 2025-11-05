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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Handles the external configuration that drives chicken definitions.
 * The modern release stores everything in a Forge-style {@code chickens.cfg}
 * file, but still honours the old {@code chickens.properties} if it is found
 * so existing packs upgrade without losing their tweaks.
 */
public final class ChickensDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensData");
    private static final String LEGACY_PROPERTIES_FILE = "chickens.properties";

    private ChickensDataLoader() {
    }

    public static void bootstrap() {
        registerLiquidEggs();
        List<ChickensRegistryItem> defaults = DefaultChickens.create();
        // Allow external JSON definitions to extend the in-memory list before
        // configuration overrides are resolved.
        CustomChickensLoader.load(defaults);
        ChickensConfigValues values = applyConfiguration(defaults);
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
        for (LiquidEggDefinition definition : LiquidEggDefinition.ALL) {
            Optional<Fluid> fluid = BuiltInRegistries.FLUID.getOptional(definition.fluidId());
            if (fluid.isEmpty() || fluid.get() == Fluids.EMPTY) {
                LOGGER.debug("Skipping liquid egg {} because fluid {} is missing", definition.id(), definition.fluidId());
                continue;
            }

            Supplier<Fluid> fluidSupplier = () -> BuiltInRegistries.FLUID.getOptional(definition.fluidId()).orElse(Fluids.EMPTY);
            Supplier<BlockState> blockSupplier = definition.blockId()
                    .flatMap(id -> BuiltInRegistries.BLOCK.getOptional(id))
                    .<Supplier<BlockState>>map(block -> () -> block.defaultBlockState())
                    .orElse(null);

            LiquidEggRegistry.register(new LiquidEggRegistryItem(
                    definition.id(),
                    blockSupplier,
                    definition.eggColor(),
                    fluidSupplier,
                    definition.volume(),
                    definition.hazards()));
        }
    }

    private record LiquidEggDefinition(int id,
                                       ResourceLocation fluidId,
                                       Optional<ResourceLocation> blockId,
                                       int eggColor,
                                       int volume,
                                       EnumSet<LiquidEggRegistryItem.HazardFlag> hazards) {
        private static final List<LiquidEggDefinition> ALL = List.of(
                // Vanilla support retained for water and lava so legacy chickens still work.
                new LiquidEggDefinition(0,
                        id("minecraft", "water"),
                        Optional.of(BuiltInRegistries.BLOCK.getKey(Blocks.WATER)),
                        0x0000ff,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class)),
                new LiquidEggDefinition(1,
                        id("minecraft", "lava"),
                        Optional.of(BuiltInRegistries.BLOCK.getKey(Blocks.LAVA)),
                        0xff0000,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.HOT)),
                new LiquidEggDefinition(2,
                        id("minecraft", "experience"),
                        Optional.empty(),
                        0x3dff1e,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.MAGICAL)),
                // Immersive Engineering fuels.
                new LiquidEggDefinition(3,
                        id("immersiveengineering", "creosote"),
                        Optional.empty(),
                        0x3c2f1f,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.TOXIC)),
                new LiquidEggDefinition(4,
                        id("immersiveengineering", "plantoil"),
                        Optional.empty(),
                        0xc9a866,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class)),
                new LiquidEggDefinition(5,
                        id("immersiveengineering", "ethanol"),
                        Optional.empty(),
                        0xf1db72,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.HOT)),
                new LiquidEggDefinition(6,
                        id("immersiveengineering", "biodiesel"),
                        Optional.empty(),
                        0xf5c244,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.HOT)),
                // BuildCraft energy fluids.
                new LiquidEggDefinition(7,
                        id("buildcraftenergy", "oil"),
                        Optional.empty(),
                        0x1f1b15,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.TOXIC)),
                new LiquidEggDefinition(8,
                        id("buildcraftenergy", "fuel"),
                        Optional.empty(),
                        0xfbe34b,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.HOT, LiquidEggRegistryItem.HazardFlag.TOXIC)),
                // Mekanism chemicals that manifest as fluids.
                new LiquidEggDefinition(9,
                        id("mekanism", "bioethanol"),
                        Optional.empty(),
                        0xffe880,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.HOT)),
                new LiquidEggDefinition(10,
                        id("mekanism", "brine"),
                        Optional.empty(),
                        0xf0f6ff,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.CORROSIVE)),
                new LiquidEggDefinition(11,
                        id("mekanism", "spent_nuclear_waste"),
                        Optional.empty(),
                        0x88b43c,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.RADIOACTIVE, LiquidEggRegistryItem.HazardFlag.TOXIC)),
                new LiquidEggDefinition(12,
                        id("mekanism", "sulfuric_acid"),
                        Optional.empty(),
                        0xf7ff99,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.CORROSIVE)),
                // Industrial Foregoing processing fluids.
                new LiquidEggDefinition(13,
                        id("industrialforegoing", "latex"),
                        Optional.empty(),
                        0xd7d0b2,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.noneOf(LiquidEggRegistryItem.HazardFlag.class)),
                new LiquidEggDefinition(14,
                        id("industrialforegoing", "pink_slime"),
                        Optional.empty(),
                        0xff9ad7,
                        FluidType.BUCKET_VOLUME,
                        EnumSet.of(LiquidEggRegistryItem.HazardFlag.MAGICAL))
        );

        private static ResourceLocation id(String namespace, String path) {
            return ResourceLocation.fromNamespaceAndPath(namespace, path);
        }
    }

    private static ChickensConfigValues applyConfiguration(List<ChickensRegistryItem> chickens) {
        Properties props = loadLegacyProperties();
        LegacyConfigBridge.importIfPresent(props, chickens);

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

        LegacyConfigBridge.export(props, chickens, values);
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
        double roostSpeed = readDouble(props, "general.roostSpeedMultiplier", 1.0D);
        double breederSpeed = readDouble(props, "general.breederSpeedMultiplier", 1.0D);
        boolean disableEggLaying = readBoolean(props, "general.disableVanillaEggLaying", false);
        int collectorRange = readInt(props, "general.collectorScanRange", 4);
        boolean avianFluxEffects = readBoolean(props, "general.avianFluxEffectsEnabled", true);
        double fluxEggMultiplier = readDouble(props, "general.fluxEggCapacityMultiplier", 1.0D);
        if (fluxEggMultiplier < 0.0D) {
            fluxEggMultiplier = 0.0D;
            props.setProperty("general.fluxEggCapacityMultiplier", Double.toString(fluxEggMultiplier));
        }
        int avianCapacity = ensurePositive(props, "general.avianFluxCapacity", readInt(props, "general.avianFluxCapacity", 50_000), 1);
        int avianReceive = ensureNonNegative(props, "general.avianFluxMaxReceive", readInt(props, "general.avianFluxMaxReceive", 4_000));
        int avianExtract = ensureNonNegative(props, "general.avianFluxMaxExtract", readInt(props, "general.avianFluxMaxExtract", 4_000));
        return new ChickensConfigValues(spawnProbability, minBroodSize, maxBroodSize, multiplier, alwaysShowStats,
                roostSpeed, breederSpeed, disableEggLaying, collectorRange, avianFluxEffects,
                Math.max(0.0D, fluxEggMultiplier), avianCapacity, avianReceive, avianExtract);
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

    private static double readDouble(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            props.setProperty(key, Double.toString(defaultValue));
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            props.setProperty(key, Double.toString(defaultValue));
            return defaultValue;
        }
    }

    private static int ensurePositive(Properties props, String key, int value, int minValue) {
        if (value < minValue) {
            int clamped = Math.max(minValue, 1);
            props.setProperty(key, Integer.toString(clamped));
            return clamped;
        }
        return value;
    }

    private static int ensureNonNegative(Properties props, String key, int value) {
        if (value < 0) {
            props.setProperty(key, "0");
            return 0;
        }
        return value;
    }

    private static Properties loadLegacyProperties() {
        Properties props = new Properties();
        Path legacyProps = FMLPaths.CONFIGDIR.get().resolve(LEGACY_PROPERTIES_FILE);
        if (Files.exists(legacyProps)) {
            try (Reader reader = Files.newBufferedReader(legacyProps)) {
                props.load(reader);
                LOGGER.info("Loaded configuration overrides from legacy chickens.properties; future saves only update chickens.cfg");
            } catch (IOException e) {
                LOGGER.warn("Failed to migrate chickens.properties; continuing with defaults", e);
            }
        }
        return props;
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD
                || event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            ModdedChickens.retryPending();
            DynamicMaterialChickens.refresh();
        }
    }

    private record ParentNames(String parent1, String parent2) {
        ParentNames {
            Objects.requireNonNull(parent1);
            Objects.requireNonNull(parent2);
        }
    }
}
