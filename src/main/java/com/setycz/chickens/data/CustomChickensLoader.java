package com.setycz.chickens.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.registry.ModRegistry;
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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the optional JSON configuration that allows players to define their
 * own chickens without touching the core mod jar. The structure embraces a
 * compact schema so server owners can script new breeds with just a text
 * editor while still validating the fields enough to catch typos early.
 */
public final class CustomChickensLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensCustomData");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "chickens_custom.json";

    private CustomChickensLoader() {
    }

    /**
     * Loads and appends any custom chicken definitions to the supplied list.
     * The caller should feed in the default registry snapshot so identifiers
     * remain stable and so that custom chickens can reference vanilla parents.
     */
    public static void load(List<ChickensRegistryItem> chickens) {
        Objects.requireNonNull(chickens, "chickens");

        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        ensureTemplateExists(configFile);

        if (!Files.exists(configFile)) {
            return;
        }

        CustomConfigFile config = readConfig(configFile);
        if (config == null || config.chickens().isEmpty()) {
            return;
        }

        Map<String, ChickensRegistryItem> byName = new HashMap<>();
        Set<Integer> usedIds = new HashSet<>();
        int nextId = 0;
        for (ChickensRegistryItem chicken : chickens) {
            byName.put(chicken.getEntityName(), chicken);
            usedIds.add(chicken.getId());
            nextId = Math.max(nextId, chicken.getId());
        }

        Map<ChickensRegistryItem, ParentNames> parentsToResolve = new HashMap<>();
        for (CustomChickenDefinition definition : config.chickens()) {
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

        ChickensRegistryItem chicken = new ChickensRegistryItem(id, name, texture, layItem, background, foreground);

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
        if (item == null) {
            LOGGER.warn("Custom chicken '{}' references unknown item '{}'", name, definition.item());
            return ItemStack.EMPTY;
        }

        int count = Math.max(definition.count() != null ? definition.count() : 1, 1);
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

    private static void ensureTemplateExists(Path configFile) {
        if (Files.exists(configFile)) {
            return;
        }
        String template = """
                {
                  "_comment": "Add entries to the chickens array. Copy the sample block from _example to get started.",
                  "chickens": [],
                  "_example": [
                    {
                      "name": "CopperChicken",
                      "texture": "chickens:textures/entity/CopperChicken.png",
                      "item_texture": "chickens:textures/item/chicken/copperchicken.png",
                      "lay_item": {
                        "item": "minecraft:copper_ingot"
                      },
                      "drop_item": {
                        "item": "minecraft:copper_ingot",
                        "count": 2
                      },
                      "background_color": "#b87333",
                      "foreground_color": "#f8cfa9",
                      "parents": ["IronChicken", "WaterChicken"],
                      "spawn_type": "normal",
                      "lay_coefficient": 1.0,
                      "display_name": "Copper Chicken"
                    }
                  ]
                }
                """;
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                writer.write(template);
            }
        } catch (IOException ex) {
            LOGGER.warn("Unable to create custom chicken configuration template", ex);
        }
    }

    @Nullable
    private static CustomConfigFile readConfig(Path configFile) {
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, CustomConfigFile.class);
        } catch (JsonParseException ex) {
            LOGGER.warn("Failed to parse custom chicken configuration; entries will be ignored", ex);
        } catch (IOException ex) {
            LOGGER.warn("Unable to read custom chicken configuration", ex);
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
            @SerializedName("item_texture") @Nullable String itemTexture) {
    }

    private record CustomItemStackDefinition(
            String item,
            @SerializedName("count") @Nullable Integer count,
            @SerializedName("type") @Nullable Integer type) {
    }
}

