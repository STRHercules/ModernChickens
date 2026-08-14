package strhercules.chickens.data;

import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.config.ChickensConfigValues;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Bridges the legacy Forge {@code chickens.cfg} format so existing installations
 * can keep using the configuration layout they are familiar with. The bridge
 * imports key values into the properties-driven system without rewriting the
 * player-owned file.
 */
public final class LegacyConfigBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensLegacyConfig");

    private LegacyConfigBridge() {
    }

    public static void importIfPresent(Properties props, List<ChickensRegistryItem> chickens) {
        Path legacyPath = legacyConfigPath();
        if (!Files.exists(legacyPath)) {
            return;
        }
        Map<String, ChickensRegistryItem> byName = mapByName(chickens);
        try (BufferedReader reader = Files.newBufferedReader(legacyPath, StandardCharsets.UTF_8)) {
            String currentSection = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.endsWith("{")) {
                    currentSection = line.substring(0, line.length() - 1).trim();
                    continue;
                }
                if (line.equals("}")) {
                    currentSection = null;
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                int colon = key.indexOf(':');
                if (colon >= 0) {
                    key = key.substring(colon + 1);
                }
                if (currentSection == null) {
                    continue;
                }
                if ("general".equalsIgnoreCase(currentSection)) {
                    applyGeneralValue(props, key, value);
                } else {
                    ChickensRegistryItem chicken = byName.get(currentSection);
                    applyChickenValue(props, chicken != null ? chicken.getEntityName() : currentSection, key, value);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to read legacy chickens.cfg", ex);
        }
    }

    /**
     * Constructs a {@link ChickensConfigValues} from the given properties and
     * immediately publishes it to {@link ChickensConfigHolder}.
     */
    public static void applyToHolder(Properties props) {
        ChickensConfigValues current = ChickensConfigHolder.get();
        ChickensConfigValues built = new ChickensConfigValues(
                getInt(props, "general.spawnProbability",           current.getSpawnProbability()),
                getInt(props, "general.minBroodSize",               current.getMinBroodSize()),
                getInt(props, "general.maxBroodSize",               current.getMaxBroodSize()),
                getFloat(props, "general.netherSpawnChanceMultiplier", current.getNetherSpawnChanceMultiplier()),
                getFloat(props, "general.overworldSpawnChance",     current.getOverworldSpawnChance()),
                getFloat(props, "general.netherSpawnChance",        current.getNetherSpawnChance()),
                getFloat(props, "general.endSpawnChance",           current.getEndSpawnChance()),
                getBool(props, "general.alwaysShowStats",           current.isAlwaysShowStats()),
                getDouble(props, "general.roostSpeedMultiplier",    current.getRoostSpeedMultiplier()),
                getDouble(props, "general.breederSpeedMultiplier",  current.getBreederSpeedMultiplier()),
                getDouble(props, "general.roosterAuraMultiplier",   current.getRoosterAuraMultiplier()),
                getInt(props, "general.roosterAuraRange",           current.getRoosterAuraRange()),
                getInt(props, "general.nestMaxRoosters",            current.getNestMaxRoosters()),
                getInt(props, "general.nestSeedDurationTicks",      current.getNestSeedDurationTicks()),
                getInt(props, "general.mechanicalNestBaseEnergyPerTick", current.getMechanicalNestBaseEnergyPerTick()),
                getInt(props, "general.mechanicalNestEnergyPerRoostPerTick", current.getMechanicalNestEnergyPerRoostPerTick()),
                getDouble(props, "general.mechanicalNestEnergyCostSpeedIncrease", current.getMechanicalNestEnergyCostSpeedIncrease()),
                getInt(props, "general.mechanicalNestRange",        current.getMechanicalNestRange()),
                getInt(props, "general.mechanicalRoostTier1EnergyCost", current.getMechanicalRoostFullSlotEnergyCost(1)),
                getInt(props, "general.mechanicalRoostTier10EnergyCost", current.getMechanicalRoostFullSlotEnergyCost(10)),
                getDouble(props, "general.mechanicalRoostEnergyCostSpeedIncrease", current.getMechanicalRoostEnergyCostSpeedIncrease()),
                getBool(props, "general.disableVanillaEggLaying",   current.isVanillaEggLayingDisabled()),
                getInt(props, "general.collectorScanRange",         current.getCollectorScanRange()),
                getBool(props, "general.avianFluxEffectsEnabled",   current.isAvianFluxEffectsEnabled()),
                getDouble(props, "general.fluxEggCapacityMultiplier", current.getFluxEggCapacityMultiplier()),
                getInt(props, "general.avianFluxCapacity",          current.getAvianFluxCapacity()),
                getInt(props, "general.avianFluxMaxReceive",        current.getAvianFluxMaxReceive()),
                getInt(props, "general.avianFluxMaxExtract",        current.getAvianFluxMaxExtract()),
                getInt(props, "general.avianFluidConverterCapacity", current.getAvianFluidConverterCapacity(8_000)),
                getInt(props, "general.avianFluidConverterTransferRate", current.getAvianFluidConverterTransfer(2_000)),
                getBool(props, "general.avianFluidConverterEffectsEnabled", current.isAvianFluidConverterEffectsEnabled()),
                getInt(props, "general.avianChemicalConverterCapacity", current.getAvianChemicalConverterCapacity(8_000)),
                getInt(props, "general.avianChemicalConverterTransferRate", current.getAvianChemicalConverterTransfer(2_000)),
                getBool(props, "general.avianChemicalConverterEffectsEnabled", current.isAvianChemicalConverterEffectsEnabled()),
                getBool(props, "general.liquidEggHazardsEnabled",   current.isLiquidEggHazardsEnabled()),
                getBool(props, "general.allowAllLiquidChemicalDousing", current.isAllLiquidChemicalDousingEnabled()),
                getBool(props, "general.enableFluidChickens",       current.isFluidChickensEnabled()),
                getBool(props, "general.autoRegisterFluidChickens", current.isAutomaticFluidChickensEnabled()),
                getBool(props, "general.enableChemicalChickens",    current.isChemicalChickensEnabled()),
                getBool(props, "general.enableGasChickens",         current.isGasChickensEnabled()),
                getInt(props, "general.incubatorEnergyCost",        current.getIncubatorEnergyCost()),
                getInt(props, "general.incubatorCapacity",          current.getIncubatorEnergyCapacity()),
                getInt(props, "general.incubatorMaxReceive",        current.getIncubatorEnergyMaxReceive()),
                current.getDropCount(),
                getBool(props, "general.scalingDrops", current.isScalingDropsEnabled())
        );
        ChickensConfigHolder.set(built);
    }

        private static float getFloat(Properties props, String key, float fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try { return Float.parseFloat(value.trim()); } catch (NumberFormatException ex) { return fallback; }
    }

    private static double getDouble(Properties props, String key, double fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try { return Double.parseDouble(value.trim()); } catch (NumberFormatException ex) { return fallback; }
    }

    private static boolean getBool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        return Boolean.parseBoolean(value.trim());
    }

    private static void applyGeneralValue(Properties props, String key, String value) {
        switch (key) {
            case "spawnProbability" -> props.setProperty("general.spawnProbability", value);
            case "minBroodSize" -> props.setProperty("general.minBroodSize", value);
            case "maxBroodSize" -> props.setProperty("general.maxBroodSize", value);
            case "netherSpawnChanceMultiplier" -> props.setProperty("general.netherSpawnChanceMultiplier", value);
            case "overworldSpawnChance" -> props.setProperty("general.overworldSpawnChance", value);
            case "netherSpawnChance" -> props.setProperty("general.netherSpawnChance", value);
            case "endSpawnChance" -> props.setProperty("general.endSpawnChance", value);
            case "alwaysShowStats" -> props.setProperty("general.alwaysShowStats", value);
            case "roostSpeed" -> props.setProperty("general.roostSpeedMultiplier", value);
            case "breederSpeed" -> props.setProperty("general.breederSpeedMultiplier", value);
            case "roosterAuraMultiplier" -> props.setProperty("general.roosterAuraMultiplier", value);
            case "roosterAuraRange" -> props.setProperty("general.roosterAuraRange", value);
            case "nestMaxRoosters" -> props.setProperty("general.nestMaxRoosters", value);
            case "nestSeedDurationTicks" -> props.setProperty("general.nestSeedDurationTicks", value);
            case "mechanicalNestBaseEnergyPerTick" -> props.setProperty("general.mechanicalNestBaseEnergyPerTick", value);
            case "mechanicalNestEnergyPerRoostPerTick" -> props.setProperty("general.mechanicalNestEnergyPerRoostPerTick", value);
            case "mechanicalNestEnergyCostSpeedIncrease" -> props.setProperty("general.mechanicalNestEnergyCostSpeedIncrease", value);
            case "mechanicalNestRange" -> props.setProperty("general.mechanicalNestRange", value);
            case "mechanicalRoostTier1EnergyCost" -> props.setProperty("general.mechanicalRoostTier1EnergyCost", value);
            case "mechanicalRoostTier10EnergyCost" -> props.setProperty("general.mechanicalRoostTier10EnergyCost", value);
            case "mechanicalRoostEnergyCostSpeedIncrease" -> props.setProperty("general.mechanicalRoostEnergyCostSpeedIncrease", value);
            case "disableEggLaying" -> props.setProperty("general.disableVanillaEggLaying", value);
            case "collectorScanRange" -> props.setProperty("general.collectorScanRange", value);
            case "avianFluxEffectsEnabled" -> props.setProperty("general.avianFluxEffectsEnabled", value);
            case "fluxEggCapacityMultiplier" -> props.setProperty("general.fluxEggCapacityMultiplier", value);
            case "avianFluxCapacity" -> props.setProperty("general.avianFluxCapacity", value);
            case "avianFluxMaxReceive" -> props.setProperty("general.avianFluxMaxReceive", value);
            case "avianFluxMaxExtract" -> props.setProperty("general.avianFluxMaxExtract", value);
            case "avianFluidConverterCapacity" -> props.setProperty("general.avianFluidConverterCapacity", value);
            case "avianFluidConverterTransferRate" -> props.setProperty("general.avianFluidConverterTransferRate", value);
            case "avianFluidConverterEffectsEnabled" -> props.setProperty("general.avianFluidConverterEffectsEnabled", value);
            case "avianChemicalConverterCapacity" -> props.setProperty("general.avianChemicalConverterCapacity", value);
            case "avianChemicalConverterTransferRate" -> props.setProperty("general.avianChemicalConverterTransferRate", value);
            case "avianChemicalConverterEffectsEnabled" -> props.setProperty("general.avianChemicalConverterEffectsEnabled", value);
            case "liquidEggHazardsEnabled" -> props.setProperty("general.liquidEggHazardsEnabled", value);
            case "incubatorEnergyCost" -> props.setProperty("general.incubatorEnergyCost", value);
            case "incubatorCapacity" -> props.setProperty("general.incubatorCapacity", value);
            case "incubatorMaxReceive" -> props.setProperty("general.incubatorMaxReceive", value);
            case "scalingDrops" -> props.setProperty("general.scalingDrops", value);
            case "enableFluidChickens" -> props.setProperty("general.enableFluidChickens", value);
            case "autoRegisterFluidChickens" -> props.setProperty("general.autoRegisterFluidChickens", value);
            case "enableChemicalChickens" -> props.setProperty("general.enableChemicalChickens", value);
            case "enableGasChickens" -> props.setProperty("general.enableGasChickens", value);
            default -> {
            }
        }
    }

    private static void applyChickenValue(Properties props, String entityName, String key, String value) {
        String prefix = prefixFor(entityName);
        switch (key) {
            case "enabled" -> props.setProperty(prefix + "enabled", value);
            case "layCoefficient" -> props.setProperty(prefix + "layCoefficient", value);
            case "spawnType" -> props.setProperty(prefix + "spawnType", value);
            case "allowNaturalSpawn" -> props.setProperty(prefix + "allowNaturalSpawn", value);
            case "parent1" -> props.setProperty(prefix + "parent1", value);
            case "parent2" -> props.setProperty(prefix + "parent2", value);
            case "layItemName" -> props.setProperty(prefix + "eggItem", value);
            case "layItemAmount" -> props.setProperty(prefix + "eggCount", value);
            case "layItemMeta" -> props.setProperty(prefix + "eggType", value);
            case "dropItemName" -> props.setProperty(prefix + "dropItem", value);
            case "dropItemAmount" -> props.setProperty(prefix + "dropCount", value);
            case "dropItemMeta" -> props.setProperty(prefix + "dropType", value);
            case "liquidDousingCost" -> props.setProperty(prefix + "liquidDousingCost", value);
            default -> {
            }
        }
    }

    private static void writeItemEntry(BufferedWriter writer, Properties props, String prefix, String kind, ItemStack stack) throws IOException {
        String itemKey = prefix + kind + "Item";
        String countKey = prefix + kind + "Count";
        String metaKey = prefix + kind + "Type";

        // Always derive the item id from the stack passed in — never from props —
        // so code-defined changes (e.g. GoldChicken lay=gold_ingot) are always written.
        String itemId = getItemId(stack);
        String count = Integer.toString(stack.getCount());
        String type = getString(props, metaKey, "0");

        String legacyPrefix = "egg".equals(kind) ? "lay" : "drop";
        writer.write(String.format(Locale.ROOT, "    S:%sItemName=%s%n", legacyPrefix, itemId));
        writer.write(String.format(Locale.ROOT, "    I:%sItemAmount=%s%n", legacyPrefix, count));
        writer.write(String.format(Locale.ROOT, "    I:%sItemMeta=%s%n", legacyPrefix, type));
    }

    private static Map<String, ChickensRegistryItem> mapByName(List<ChickensRegistryItem> chickens) {
        Map<String, ChickensRegistryItem> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ChickensRegistryItem chicken : chickens) {
            result.put(chicken.getEntityName(), chicken);
        }
        return result;
    }

    private static String prefixFor(String entityName) {
        return "chicken." + entityName + ".";
    }

    private static String getString(Properties props, String key, String fallback) {
        return props.getProperty(key, fallback);
    }

    private static String getBoolean(Properties props, String key, boolean fallback) {
        return props.getProperty(key, Boolean.toString(fallback));
    }

    private static int getInt(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static Path legacyConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("chickens.cfg");
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : "minecraft:air";
    }
}
