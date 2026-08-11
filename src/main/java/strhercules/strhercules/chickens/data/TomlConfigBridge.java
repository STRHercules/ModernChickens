package strhercules.chickens.data;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Reads the two player-owned TOML files while keeping the runtime adapter stable. */
public final class TomlConfigBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensTomlConfig");
    private static final String RESOURCE = "/defaultconfigs/chickens.toml";
    private static final String CONFIG_FILE = "chickens.toml";
    private static final String CUSTOM_RESOURCE = "/defaultconfigs/custom_chickens.toml";
    private static final String CUSTOM_CONFIG_FILE = "custom_chickens.toml";
    private static final String LEGACY_FILE = "chickens.cfg";

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "alwaysShowStats", "disableEggLaying", "disableVanillaEggLaying", "avianFluxEffectsEnabled",
            "avianFluidConverterEffectsEnabled", "avianChemicalConverterEffectsEnabled",
            "liquidEggHazardsEnabled", "scalingDrops", "enableFluidChickens",
            "autoRegisterFluidChickens", "allowAllLiquidChemicalDousing",
            "enableChemicalChickens", "enableGasChickens", "enabled", "allowNaturalSpawn",
            "allowDousing", "generatedTexture");
    private static final Set<String> INTEGER_KEYS = Set.of(
            "spawnProbability", "minBroodSize", "maxBroodSize", "roosterAuraRange",
            "nestMaxRoosters", "nestSeedDurationTicks", "collectorScanRange", "avianFluxCapacity",
            "avianFluxMaxReceive", "avianFluxMaxExtract", "avianFluidConverterCapacity",
            "avianFluidConverterTransferRate", "avianChemicalConverterCapacity",
            "avianChemicalConverterTransferRate", "incubatorEnergyCost", "incubatorCapacity",
            "incubatorMaxReceive", "roostDropCount", "layItemAmount", "layItemMeta",
            "dropItemAmount", "dropItemMeta", "liquidDousingCost", "id");
    private static final Set<String> DECIMAL_KEYS = Set.of(
            "netherSpawnChanceMultiplier", "overworldSpawnChance", "netherSpawnChance",
            "endSpawnChance", "roostSpeed", "breederSpeed", "roosterAuraMultiplier",
            "fluxEggCapacityMultiplier", "layCoefficient");

    private TomlConfigBridge() {
    }

    public static void load(Properties props) {
        Path configPath = configPath();
        Path customPath = customConfigPath();
        boolean customCreated = ensureResource(customPath, CUSTOM_RESOURCE);

        Properties loaded = new Properties();
        if (Files.exists(configPath)) {
            try {
                readToml(loaded, configPath, true);
            } catch (Exception ex) {
                LOGGER.error("Failed to read {}; using safe defaults", configPath.getFileName(), ex);
            }
        } else {
            createDefaultConfig(configPath, loaded, props);
        }

        Properties oldChickenValues = chickenOnly(loaded);
        if (!oldChickenValues.isEmpty()) {
            boolean moved = !customCreated;
            if (customCreated) {
                try {
                    writeToml(customPath, oldChickenValues);
                    moved = true;
                    LOGGER.info("Moved per-chicken values from {} to {}", CONFIG_FILE, CUSTOM_CONFIG_FILE);
                } catch (Exception ex) {
                    LOGGER.error("Failed to migrate per-chicken values to {}", CUSTOM_CONFIG_FILE, ex);
                }
            }
            if (moved) {
                removeChickenSection(configPath);
                removeChickenProperties(loaded);
            }
        }

        props.clear();
        props.putAll(loaded);
        if (Files.exists(customPath)) {
            try {
                readToml(props, customPath, false);
            } catch (Exception ex) {
                LOGGER.error("Failed to read {}; per-chicken values may be unavailable",
                        customPath.getFileName(), ex);
            }
        }
    }

    private static void createDefaultConfig(Path configPath, Properties loaded, Properties existing) {
        Path temporary = null;
        try {
            Files.createDirectories(configPath.getParent());
            temporary = Files.createTempFile(configPath.getParent(), "chickens-", ".toml");
            try (InputStream input = TomlConfigBridge.class.getResourceAsStream(RESOURCE)) {
                if (input == null) {
                    throw new IOException("default TOML resource is missing");
                }
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            readToml(loaded, temporary, true);
            loaded.putAll(existing);
            if (Files.exists(legacyConfigPath())) {
                LegacyConfigBridge.importIfPresent(loaded, List.of());
                LOGGER.info("Migrating legacy {} to {}", LEGACY_FILE, CONFIG_FILE);
            }
            writeToml(temporary, generalOnly(loaded));
            moveIntoPlace(temporary, configPath);
            temporary = null;
        } catch (Exception ex) {
            LOGGER.error("Failed to create {}; using legacy/default values", configPath.getFileName(), ex);
            loaded.clear();
            loaded.putAll(existing);
            try {
                LegacyConfigBridge.importIfPresent(loaded, List.of());
            } catch (Exception legacyEx) {
                LOGGER.warn("Failed to read legacy {}", LEGACY_FILE, legacyEx);
            }
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ex) {
                    LOGGER.debug("Failed to remove temporary TOML file {}", temporary, ex);
                }
            }
        }
    }

    static boolean ensureResource(Path target, String resource) {
        if (Files.exists(target)) {
            return false;
        }
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "chickens-", ".toml");
            try (InputStream input = TomlConfigBridge.class.getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IOException("default TOML resource is missing: " + resource);
                }
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temporary, target);
            temporary = null;
            return true;
        } catch (Exception ex) {
            LOGGER.error("Failed to create {}", target.getFileName(), ex);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ex) {
                    LOGGER.debug("Failed to remove temporary TOML file {}", temporary, ex);
                }
            }
        }
    }

    private static void readToml(Properties props, Path path, boolean includeGeneral) throws IOException {
        try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance()).build()) {
            config.load();
            if (includeGeneral) {
                copySection(props, config.getRaw("general"), "general", false);
            }
            Object chickens = config.getRaw("chickens");
            if (chickens instanceof UnmodifiableConfig chickenTables) {
                for (UnmodifiableConfig.Entry entry : chickenTables.entrySet()) {
                    copySection(props, entry.getRawValue(), "chicken." + entry.getKey(), true);
                }
            }
        }
    }

    private static void copySection(Properties props, Object rawSection, String prefix, boolean chicken) {
        if (!(rawSection instanceof UnmodifiableConfig section)) {
            return;
        }
        for (UnmodifiableConfig.Entry entry : section.entrySet()) {
            Object value = entry.getRawValue();
            if (value instanceof UnmodifiableConfig) {
                continue;
            }
            String key = chicken ? internalChickenKey(entry.getKey()) : internalGeneralKey(entry.getKey());
            props.setProperty(prefix + "." + key, String.valueOf(value));
        }
    }

    private static void writeToml(Path path, Properties props) throws IOException {
        try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance()).build()) {
            config.load();
            for (String property : props.stringPropertyNames()) {
                String[] parts = property.split("\\.", 3);
                if (parts.length != 2 && parts.length != 3) {
                    continue;
                }
                String key;
                List<String> tomlPath;
                if ("general".equals(parts[0]) && parts.length == 2) {
                    key = externalGeneralKey(parts[1]);
                    tomlPath = List.of("general", key);
                } else if ("chicken".equals(parts[0]) && parts.length == 3) {
                    key = externalChickenKey(parts[2]);
                    tomlPath = List.of("chickens", parts[1], key);
                } else {
                    continue;
                }
                config.set(tomlPath, tomlValue(key, props.getProperty(property)));
            }
            config.save();
        }
    }

    private static Object tomlValue(String key, String raw) {
        if (BOOLEAN_KEYS.contains(key)) {
            return Boolean.parseBoolean(raw.trim());
        }
        if (INTEGER_KEYS.contains(key)) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        if (DECIMAL_KEYS.contains(key)) {
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        return raw;
    }

    private static Properties generalOnly(Properties source) {
        Properties result = new Properties();
        for (String property : source.stringPropertyNames()) {
            if (property.startsWith("general.")) {
                result.setProperty(property, source.getProperty(property));
            }
        }
        return result;
    }

    private static Properties chickenOnly(Properties source) {
        Properties result = new Properties();
        for (String property : source.stringPropertyNames()) {
            if (property.startsWith("chicken.")) {
                result.setProperty(property, source.getProperty(property));
            }
        }
        return result;
    }

    private static void removeChickenProperties(Properties props) {
        props.stringPropertyNames().stream()
                .filter(property -> property.startsWith("chicken."))
                .toList()
                .forEach(props::remove);
    }

    private static void removeChickenSection(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance()).build()) {
            config.load();
            if (config.getRaw("chickens") != null) {
                config.remove("chickens");
                config.save();
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove migrated [chickens] tables from {}", path.getFileName(), ex);
        }
    }

    private static String internalGeneralKey(String key) {
        return switch (key) {
            case "roostSpeed" -> "roostSpeedMultiplier";
            case "breederSpeed" -> "breederSpeedMultiplier";
            case "disableEggLaying" -> "disableVanillaEggLaying";
            default -> key;
        };
    }

    private static String externalGeneralKey(String key) {
        return switch (key) {
            case "roostSpeedMultiplier" -> "roostSpeed";
            case "breederSpeedMultiplier" -> "breederSpeed";
            case "disableVanillaEggLaying" -> "disableEggLaying";
            default -> key;
        };
    }

    private static String internalChickenKey(String key) {
        return switch (key) {
            case "layItemName" -> "eggItem";
            case "layItemAmount" -> "eggCount";
            case "layItemMeta" -> "eggType";
            case "dropItemName" -> "dropItem";
            case "dropItemAmount" -> "dropCount";
            case "dropItemMeta" -> "dropType";
            default -> key;
        };
    }

    private static String externalChickenKey(String key) {
        return switch (key) {
            case "eggItem" -> "layItemName";
            case "eggCount" -> "layItemAmount";
            case "eggType" -> "layItemMeta";
            case "dropItem" -> "dropItemName";
            case "dropCount" -> "dropItemAmount";
            case "dropType" -> "dropItemMeta";
            default -> key;
        };
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
    }

    public static Path customConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CUSTOM_CONFIG_FILE);
    }

    static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    private static Path legacyConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(LEGACY_FILE);
    }
}
