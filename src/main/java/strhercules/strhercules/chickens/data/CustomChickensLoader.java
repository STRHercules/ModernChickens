package strhercules.chickens.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.registry.ModRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Loads the TOML chicken tables that allow players to define new chickens and
 * override stock chicken values without touching the core mod jar. Legacy JSON
 * is still accepted for existing installations.
 */
public final class CustomChickensLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensCustomData");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LEGACY_CONFIG_FILE = "chickens_custom.json";

    private CustomChickensLoader() {
    }

    /**
     * Loads and appends any custom chicken definitions to the supplied list.
     * The caller should feed in the default registry snapshot so identifiers
     * remain stable and so that custom chickens can reference vanilla parents.
     */
    public static void load(List<ChickensRegistryItem> chickens) {
        Objects.requireNonNull(chickens, "chickens");

        Path configFile = TomlConfigBridge.customConfigPath();
        List<CustomChickenDefinition> definitions = readTomlConfig(configFile);
        Path legacyConfigFile = FMLPaths.CONFIGDIR.get().resolve(LEGACY_CONFIG_FILE);
        CustomConfigFile legacyConfig = Files.exists(legacyConfigFile) ? readConfig(legacyConfigFile) : null;
        if (legacyConfig != null) {
            definitions = new ArrayList<>(definitions);
            definitions.addAll(legacyConfig.chickens());
        }
        LOGGER.info("Loading custom chicken definitions from {}", configFile.toAbsolutePath());

        if (definitions.isEmpty()) {
            return;
        }

        Map<String, ChickensRegistryItem> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<Integer> usedIds = new HashSet<>();
        int nextId = 0;
        for (ChickensRegistryItem chicken : chickens) {
            byName.put(chicken.getEntityName(), chicken);
            usedIds.add(chicken.getId());
            nextId = Math.max(nextId, chicken.getId());
        }

        Map<ChickensRegistryItem, ParentNames> parentsToResolve = new HashMap<>();
        int loaded = 0;
        for (CustomChickenDefinition definition : definitions) {
            if (definition.name() != null && byName.containsKey(definition.name())) {
                // Built-in entries are represented in custom_chickens.toml so
                // their editable fields live in one file. They are applied by
                // ChickensDataLoader; only new names are constructed here.
                continue;
            }
            try {
                Optional<CreatedChicken> created = createChicken(definition, byName, usedIds, nextId + 1);
                if (created.isEmpty()) {
                    continue;
                }

                CreatedChicken data = created.get();
                ChickensRegistryItem chicken = data.chicken();
                chickens.add(chicken);
                byName.put(chicken.getEntityName(), chicken);
                usedIds.add(chicken.getId());
                nextId = Math.max(nextId, chicken.getId());
                parentsToResolve.put(chicken, data.parents());
                loaded++;
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Skipping custom chicken due to invalid data: {}", ex.getMessage());
            }
        }

        // Resolve parent references in a second pass so custom chickens can
        // refer to each other regardless of declaration order.
        for (Map.Entry<ChickensRegistryItem, ParentNames> entry : parentsToResolve.entrySet()) {
            ChickensRegistryItem chicken = entry.getKey();
            ParentNames parentNames = entry.getValue();
            ChickensRegistryItem parent1 = resolveParent(byName, parentNames.parent1());
            ChickensRegistryItem parent2 = resolveParent(byName, parentNames.parent2());
            if (parent1 != null && parent2 != null) {
                chicken.setParentsNew(parent1, parent2);
            } else if (parent1 == null && parent2 == null) {
                chicken.setNoParents();
            } else {
                LOGGER.warn("Custom chicken '{}' has incomplete parents; clearing breeding data", chicken.getEntityName());
                chicken.setNoParents();
            }
        }
        LOGGER.info("Loaded {} custom chickens from {} ({} rejected)", loaded, configFile.toAbsolutePath(),
                definitions.size() - loaded);
    }

    private static Optional<CreatedChicken> createChicken(CustomChickenDefinition definition,
            Map<String, ChickensRegistryItem> existing,
            Set<Integer> usedIds,
            int initialNextId) {
        if (definition.name() == null || definition.name().isEmpty()) {
            LOGGER.warn("Encountered custom chicken with no name; skipping entry");
            return Optional.empty();
        }

        String name = definition.name();
        if (existing.containsKey(name)) {
            LOGGER.warn("Custom chicken name '{}' already exists; skipping duplicate", name);
            return Optional.empty();
        }

        int id = definition.id() != null ? definition.id() : findNextId(usedIds, initialNextId);
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive for chicken '" + name + "'");
        }
        if (usedIds.contains(id)) {
            LOGGER.warn("Custom chicken '{}' requested duplicate id {}; skipping", name, id);
            return Optional.empty();
        }

        ResourceLocation texture = parseResource(definition.texture(), "texture", name);
        if (texture == null) {
            if (Boolean.TRUE.equals(definition.generatedTexture())) {
                ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath(
                        ChickensMod.MOD_ID, "textures/entity/whitechicken.png");
                LOGGER.warn(
                        "Custom chicken '{}' will use generated texture base {} because the configured texture '{}' was invalid",
                        name, fallback, definition.texture());
                texture = fallback;
            } else {
                return Optional.empty();
            }
        }

        ItemStack layItem = parseItemStack(definition.layItem(), name, "lay_item");
        if (layItem.isEmpty()) {
            return Optional.empty();
        }

        int background = parseColour(definition.backgroundColour(), 0xffffff, "background_color", name);
        int foreground = parseColour(definition.foregroundColour(), 0xffff00, "foreground_color", name);

        ChickensRegistryItem chicken = new ChickensRegistryItem(id, name, texture, layItem, background, foreground)
                .markCustom();

        if (definition.itemTexture() != null && !definition.itemTexture().isEmpty()) {
            ResourceLocation itemTexture = parseResource(definition.itemTexture(), "item_texture", name);
            if (itemTexture != null) {
                // Remember the custom item sprite so the client can build an override model during baking.
                chicken.setItemTexture(itemTexture);
            }
        }

        ItemStack dropItem = parseOptionalItemStack(definition.dropItem(), name, "drop_item");
        if (!dropItem.isEmpty()) {
            chicken.setDropItem(dropItem);
        }

        if (definition.spawnType() != null && !definition.spawnType().isEmpty()) {
            SpawnType spawnType = parseSpawnType(definition.spawnType(), name);
            chicken.setSpawnType(spawnType);
        }

        if (definition.layCoefficient() != null) {
            chicken.setLayCoefficient(Math.max(definition.layCoefficient(), 0.0f));
        }

        if (definition.displayName() != null && !definition.displayName().isEmpty()) {
            chicken.setDisplayName(Component.literal(definition.displayName()));
        }

        if (definition.generatedTexture() != null) {
            chicken.setGeneratedTexture(definition.generatedTexture());
        }

        if (definition.enabled() != null) {
            chicken.setEnabled(definition.enabled());
        }
        if (definition.allowDousing() != null) {
            chicken.setDousingAllowed(definition.allowDousing());
        }

        ParentNames parents = new ParentNames(normaliseParent(definition.parents(), 0),
                normaliseParent(definition.parents(), 1));

        return Optional.of(new CreatedChicken(chicken, parents));
    }

    private static String normaliseParent(@Nullable List<String> parents, int index) {
        if (parents == null || parents.size() <= index) {
            return "";
        }
        String parent = parents.get(index);
        return parent != null ? parent.trim() : "";
    }

    private static int findNextId(Set<Integer> usedIds, int startingValue) {
        int nextId = Math.max(startingValue, 1);
        while (usedIds.contains(nextId)) {
            nextId++;
        }
        return nextId;
    }

    private static ItemStack parseOptionalItemStack(@Nullable CustomItemStackDefinition definition, String name, String key) {
        if (definition == null) {
            return ItemStack.EMPTY;
        }
        return parseItemStack(definition, name, key);
    }

    private static ItemStack parseItemStack(@Nullable CustomItemStackDefinition definition, String name, String key) {
        if (definition == null || definition.item() == null || definition.item().isEmpty()) {
            LOGGER.warn("Custom chicken '{}' is missing required field '{}'; skipping", name, key);
            return ItemStack.EMPTY;
        }

        ResourceLocation id = ResourceLocation.tryParse(definition.item());
        if (id == null) {
            LOGGER.warn("Custom chicken '{}' has malformed item identifier '{}'", name, definition.item());
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            LOGGER.warn("Custom chicken '{}' references unknown item '{}'", name, definition.item());
            return ItemStack.EMPTY;
        }

        int count = Math.max(definition.count() != null ? definition.count() : 1, 1);
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        if (count > maxStackSize) {
            LOGGER.warn("Custom chicken '{}' {} count {} exceeds its stack limit {}; using {}", name, key, count,
                    maxStackSize, maxStackSize);
            count = maxStackSize;
        }
        ItemStack stack = new ItemStack(item, count);
        if (stack.is(ModRegistry.LIQUID_EGG.get()) && definition.type() != null) {
            int type = Math.max(definition.type(), 0);
            ChickenItemHelper.setChickenType(stack, type);
        }
        return stack;
    }

    private static int parseColour(@Nullable String raw, int fallback, String field, String chickenName) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        String normalised = raw.trim();
        if (normalised.startsWith("#")) {
            normalised = normalised.substring(1);
        }
        try {
            int value = Integer.parseUnsignedInt(normalised, 16);
            return clampColour(value, fallback, field, chickenName);
        } catch (NumberFormatException ignored) {
            try {
                int value = Integer.parseInt(normalised);
                return clampColour(value, fallback, field, chickenName);
            } catch (NumberFormatException ex) {
                LOGGER.warn("Custom chicken '{}' could not parse '{}' value '{}'; using default", chickenName, field, raw);
                return fallback;
            }
        }
    }

    private static int clampColour(int value, int fallback, String field, String chickenName) {
        if (value < 0 || value > 0xFFFFFF) {
            LOGGER.warn("Custom chicken '{}' provided out-of-range '{}' value {}; using default", chickenName, field, value);
            return fallback;
        }
        return value;
    }

    private static SpawnType parseSpawnType(String raw, String chickenName) {
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return SpawnType.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Custom chicken '{}' has invalid spawn type '{}'; defaulting to NORMAL", chickenName, raw);
            return SpawnType.NORMAL;
        }
    }

    @Nullable
    private static ResourceLocation parseResource(@Nullable String raw, String field, String chickenName) {
        if (raw == null || raw.isEmpty()) {
            LOGGER.warn("Custom chicken '{}' missing required '{}' field", chickenName, field);
            return null;
        }
        ResourceLocation resource = ResourceLocation.tryParse(raw);
        if (resource != null) {
            return resource;
        }

        // Normalise common mistakes (uppercase letters, Windows separators)
        // so that player supplied paths that look like resource pack paths
        // still resolve to legal Minecraft resource locations.
        String sanitised = sanitiseResource(raw);
        if (!sanitised.equals(raw)) {
            ResourceLocation sanitisedResource = ResourceLocation.tryParse(sanitised);
            if (sanitisedResource != null) {
                LOGGER.warn(
                        "Custom chicken '{}' normalised '{}' value '{}' to '{}' to satisfy resource naming rules",
                        chickenName, field, raw, sanitisedResource);
                return sanitisedResource;
            }
        }

        LOGGER.warn("Custom chicken '{}' has malformed '{}' value '{}'", chickenName, field, raw);
        return null;
    }

    private static String sanitiseResource(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return trimmed.toLowerCase(Locale.ROOT);
        }

        String namespace = trimmed.substring(0, separator).toLowerCase(Locale.ROOT);
        String path = trimmed.substring(separator + 1)
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);
        return namespace + ":" + path;
    }

    private static List<CustomChickenDefinition> readTomlConfig(Path configFile) {
        if (!Files.exists(configFile)) {
            return List.of();
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(configFile, TomlFormat.instance()).build()) {
            config.load();
            Object rawChickens = config.getRaw("chickens");
            if (!(rawChickens instanceof UnmodifiableConfig tables)) {
                return List.of();
            }

            List<CustomChickenDefinition> definitions = new ArrayList<>();
            for (UnmodifiableConfig.Entry entry : tables.entrySet()) {
                if (entry.getRawValue() instanceof UnmodifiableConfig table) {
                    definitions.add(definitionFromToml(entry.getKey(), table));
                }
            }
            return List.copyOf(definitions);
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse {}; entries will be ignored", configFile.getFileName(), ex);
            return List.of();
        }
    }

    private static CustomChickenDefinition definitionFromToml(String name, UnmodifiableConfig table) {
        return new CustomChickenDefinition(
                name,
                integer(table, "id"),
                string(table, "texture"),
                itemStack(table, "layItemName", "lay_item"),
                itemStack(table, "dropItemName", "drop_item"),
                string(table, "backgroundColor", "background_color"),
                string(table, "foregroundColor", "foreground_color"),
                parents(table),
                string(table, "spawnType", "spawn_type"),
                decimal(table, "layCoefficient", "lay_coefficient"),
                string(table, "displayName", "display_name"),
                bool(table, "generatedTexture", "generated_texture"),
                bool(table, "enabled"),
                bool(table, "allowDousing", "allow_dousing"),
                string(table, "itemTexture", "item_texture"));
    }

    @Nullable
    private static CustomItemStackDefinition itemStack(UnmodifiableConfig table, String flatKey, String nestedKey) {
        Object nested = value(table, nestedKey);
        if (nested instanceof UnmodifiableConfig nestedConfig) {
            String item = string(nestedConfig, "item");
            if (item != null) {
                return new CustomItemStackDefinition(item, integer(nestedConfig, "count"),
                        integer(nestedConfig, "type"));
            }
        }

        String item = string(table, flatKey);
        return item == null ? null : new CustomItemStackDefinition(item,
                integer(table, flatKey.replace("Name", "Amount")),
                integer(table, flatKey.replace("Name", "Meta")));
    }

    @Nullable
    private static List<String> parents(UnmodifiableConfig table) {
        Object raw = value(table, "parents");
        if (raw instanceof List<?> values) {
            List<String> parents = new ArrayList<>();
            for (Object value : values) {
                if (value != null) {
                    parents.add(String.valueOf(value));
                }
            }
            return List.copyOf(parents);
        }

        String parent1 = string(table, "parent1");
        String parent2 = string(table, "parent2");
        if (parent1 == null && parent2 == null) {
            return null;
        }
        return List.of(parent1 != null ? parent1 : "", parent2 != null ? parent2 : "");
    }

    @Nullable
    private static Object value(UnmodifiableConfig table, String... keys) {
        for (String key : keys) {
            Object value = table.getRaw(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private static String string(UnmodifiableConfig table, String... keys) {
        Object raw = value(table, keys);
        return raw == null ? null : String.valueOf(raw);
    }

    @Nullable
    private static Integer integer(UnmodifiableConfig table, String... keys) {
        Object raw = value(table, keys);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Float decimal(UnmodifiableConfig table, String... keys) {
        Object raw = value(table, keys);
        if (raw instanceof Number number) {
            return number.floatValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Float.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean bool(UnmodifiableConfig table, String... keys) {
        Object raw = value(table, keys);
        if (raw instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return raw == null ? null : Boolean.valueOf(String.valueOf(raw));
    }

    @Nullable
    private static CustomConfigFile readConfig(Path configFile) {
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, CustomConfigFile.class);
        } catch (JsonParseException ex) {
            LOGGER.warn("Failed to parse legacy custom chicken JSON; entries will be ignored", ex);
        } catch (IOException ex) {
            LOGGER.warn("Unable to read legacy custom chicken JSON", ex);
        }
        return null;
    }

    @Nullable
    private static ChickensRegistryItem resolveParent(Map<String, ChickensRegistryItem> byName, String parentName) {
        if (parentName == null || parentName.isEmpty()) {
            return null;
        }
        ChickensRegistryItem parent = byName.get(parentName);
        if (parent == null) {
            LOGGER.warn("Unknown parent '{}' referenced in custom chicken configuration", parentName);
        }
        return parent;
    }

    private record CreatedChicken(ChickensRegistryItem chicken, ParentNames parents) {
    }

    private record ParentNames(String parent1, String parent2) {
        ParentNames {
            Objects.requireNonNull(parent1, "parent1");
            Objects.requireNonNull(parent2, "parent2");
        }
    }

    private record CustomConfigFile(List<CustomChickenDefinition> chickens) {
        CustomConfigFile {
            chickens = chickens != null ? List.copyOf(chickens) : List.of();
        }
    }

    private record CustomChickenDefinition(
            String name,
            @SerializedName("id") @Nullable Integer id,
            String texture,
            @SerializedName("lay_item") CustomItemStackDefinition layItem,
            @SerializedName("drop_item") @Nullable CustomItemStackDefinition dropItem,
            @SerializedName("background_color") @Nullable String backgroundColour,
            @SerializedName("foreground_color") @Nullable String foregroundColour,
            @SerializedName("parents") @Nullable List<String> parents,
            @SerializedName("spawn_type") @Nullable String spawnType,
            @SerializedName("lay_coefficient") @Nullable Float layCoefficient,
            @SerializedName("display_name") @Nullable String displayName,
            @SerializedName("generated_texture") @Nullable Boolean generatedTexture,
            @SerializedName("enabled") @Nullable Boolean enabled,
            @SerializedName("allow_dousing") @Nullable Boolean allowDousing,
            @SerializedName("item_texture") @Nullable String itemTexture) {
    }

    private record CustomItemStackDefinition(
            String item,
            @SerializedName("count") @Nullable Integer count,
            @SerializedName("type") @Nullable Integer type) {
    }
}
