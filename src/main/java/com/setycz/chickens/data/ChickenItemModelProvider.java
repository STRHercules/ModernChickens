package com.setycz.chickens.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.data.DefaultChickens;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider that generates the base chicken item model with overrides for
 * every known chicken variant. Each override maps a {@code custom_model_data}
 * predicate to the corresponding Roost sprite so the inventory icon reflects
 * the captured chicken type.
 */
public final class ChickenItemModelProvider implements DataProvider {
    private final PackOutput packOutput;

    public ChickenItemModelProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");

        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/egg");
        root.add("textures", textures);

        JsonArray overrides = new JsonArray();
        List<ChickensRegistryItem> chickens = DefaultChickens.create();
        Set<Integer> seenIds = new HashSet<>();
        for (ChickensRegistryItem chicken : chickens) {
            if (!seenIds.add(chicken.getId())) {
                continue;
            }
            String modelName = chicken.getEntityName().toLowerCase(Locale.ROOT);

            JsonObject override = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", chicken.getId());
            override.add("predicate", predicate);
            override.addProperty("model", ChickensMod.MOD_ID + ":item/chicken/" + modelName);
            overrides.add(override);

            JsonObject variant = new JsonObject();
            variant.addProperty("parent", "minecraft:item/generated");
            JsonObject variantTextures = new JsonObject();
            variantTextures.addProperty("layer0", ChickensMod.MOD_ID + ":item/chicken/" + modelName);
            variant.add("textures", variantTextures);

            Path variantPath = this.packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(ChickensMod.MOD_ID).resolve("models/item/chicken/" + modelName + ".json");
            futures.add(DataProvider.saveStable(output, variant, variantPath));
        }
        root.add("overrides", overrides);

        Path path = this.packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(ChickensMod.MOD_ID).resolve("models/item/chicken.json");
        futures.add(DataProvider.saveStable(output, root, path));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Chicken Item Models";
    }
}
