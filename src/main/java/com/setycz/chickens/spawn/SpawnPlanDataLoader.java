package com.setycz.chickens.spawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.setycz.chickens.SpawnType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads optional spawn plan overrides from datapacks so pack makers can tune weights and brood sizes
 * without editing the code or global configuration file.
 */
public final class SpawnPlanDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensSpawnPlans");
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIRECTORY = "chickens/spawn_plans";
    private static final Map<SpawnType, SpawnPlanOverride> OVERRIDES = new EnumMap<>(SpawnType.class);

    private SpawnPlanDataLoader() {
        super(GSON, DIRECTORY);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SpawnPlanDataLoader());
    }

    public static Optional<SpawnPlanOverride> getOverride(SpawnType spawnType) {
        return Optional.ofNullable(OVERRIDES.get(spawnType));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<SpawnType, SpawnPlanOverride> parsed = new EnumMap<>(SpawnType.class);
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                JsonObject object = entry.getValue().getAsJsonObject();
                SpawnPlanOverride override = parseOverride(object);
                if (parsed.put(override.spawnType(), override) != null) {
                    LOGGER.warn("Spawn plan override for {} was defined multiple times; later files replace earlier ones", override.spawnType());
                }
            } catch (IllegalArgumentException | JsonSyntaxException ex) {
                LOGGER.warn("Ignoring malformed spawn plan {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        OVERRIDES.clear();
        OVERRIDES.putAll(parsed);
        if (!parsed.isEmpty()) {
            LOGGER.info("Loaded {} spawn plan override{}", parsed.size(), parsed.size() == 1 ? "" : "s");
        }
        ChickensSpawnManager.refreshFromRegistry();
    }

    private static SpawnPlanOverride parseOverride(JsonObject object) {
        if (!object.has("spawn_type")) {
            throw new JsonSyntaxException("Missing required field 'spawn_type'");
        }
        SpawnType type = SpawnType.valueOf(object.get("spawn_type").getAsString().trim().toUpperCase());
        Integer weight = object.has("spawn_weight") ? ensurePositive(object, "spawn_weight") : null;
        Double weightMultiplier = object.has("weight_multiplier") ? ensurePositiveDouble(object, "weight_multiplier") : null;
        Integer minBrood = object.has("min_brood_size") ? ensurePositive(object, "min_brood_size") : null;
        Integer maxBrood = object.has("max_brood_size") ? ensurePositive(object, "max_brood_size") : null;
        Double charge = object.has("spawn_charge") ? ensurePositiveDouble(object, "spawn_charge") : null;
        Double energy = object.has("energy_budget") ? ensurePositiveDouble(object, "energy_budget") : null;
        if (weight != null && weightMultiplier != null) {
            throw new JsonSyntaxException("Specify either spawn_weight or weight_multiplier, not both");
        }
        return new SpawnPlanOverride(type, weight, weightMultiplier, minBrood, maxBrood, charge, energy);
    }

    private static Integer ensurePositive(JsonObject object, String key) {
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new JsonSyntaxException("'" + key + "' must be greater than zero");
        }
        return value;
    }

    private static Double ensurePositiveDouble(JsonObject object, String key) {
        double value = object.get(key).getAsDouble();
        if (value <= 0.0D) {
            throw new JsonSyntaxException("'" + key + "' must be greater than zero");
        }
        return value;
    }
}
