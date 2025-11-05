package com.modernfluidcows.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Lightweight JSON configuration helper that emulates the legacy FluidCows loader.
 *
 * <p>The original mod used a handwritten JSON parser to populate defaults and persist
 * configuration edits. This modernised variant retains the same behaviour but relies on the
 * {@link java.nio.file} API and try-with-resources so corrupted streams are handled safely.</p>
 */
public final class JsonConfig {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private JsonObject root = new JsonObject();
    private boolean dirty;

    public JsonConfig(final Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /**
     * Loads the configuration JSON, creating an empty object when the file is missing or empty.
     */
    public void load() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                Files.createFile(file);
                root = new JsonObject();
                dirty = true;
                return;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement parsed = gson.fromJson(reader, JsonElement.class);
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load FluidCows configuration from " + file + '.', ex);
        }
    }

    /** Saves the configuration back to disk when any defaults were inserted. */
    public void save() {
        if (!dirty) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(root, writer);
            dirty = false;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save FluidCows configuration to " + file + '.', ex);
        }
    }

    private JsonObject category(final String name) {
        JsonObject existing = root.getAsJsonObject(name);
        if (existing == null) {
            existing = new JsonObject();
            root.add(name, existing);
            dirty = true;
        }
        return existing;
    }

    public boolean getOrDefault(final String category, final String key, final boolean fallback) {
        JsonObject object = category(category);
        if (object.has(key)) {
            return object.get(key).getAsBoolean();
        }
        object.addProperty(key, fallback);
        dirty = true;
        return fallback;
    }

    public String getOrDefault(final String category, final String key, final String fallback) {
        JsonObject object = category(category);
        if (object.has(key)) {
            return object.get(key).getAsString();
        }
        object.addProperty(key, fallback);
        dirty = true;
        return fallback;
    }

    public String[] getOrDefault(final String category, final String key, final String[] fallback) {
        JsonObject object = category(category);
        if (object.has(key)) {
            JsonArray jsonArray = object.getAsJsonArray(key);
            String[] values = new String[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                values[i] = jsonArray.get(i).getAsString();
            }
            return values;
        }

        JsonArray jsonArray = new JsonArray();
        for (String value : fallback) {
            jsonArray.add(value);
        }
        object.add(key, jsonArray);
        dirty = true;
        return fallback;
    }

    public int getOrDefault(final String category, final String key, final int fallback) {
        JsonObject object = category(category);
        if (object.has(key)) {
            return object.get(key).getAsInt();
        }
        object.addProperty(key, fallback);
        dirty = true;
        return fallback;
    }

    public int[] getOrDefault(final String category, final String key, final int[] fallback) {
        JsonObject object = category(category);
        if (object.has(key)) {
            JsonArray jsonArray = object.getAsJsonArray(key);
            int[] values = new int[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                values[i] = jsonArray.get(i).getAsInt();
            }
            return values;
        }

        JsonArray jsonArray = new JsonArray();
        for (int value : fallback) {
            jsonArray.add(value);
        }
        object.add(key, jsonArray);
        dirty = true;
        return fallback;
    }
}
