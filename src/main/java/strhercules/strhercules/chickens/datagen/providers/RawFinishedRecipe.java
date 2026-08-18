package strhercules.chickens.datagen.providers;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;

import java.util.Map;

public record RawFinishedRecipe(ResourceLocation id, JsonObject data, RecipeSerializer<?> serializer)
        implements FinishedRecipe {

    @Override
    public void serializeRecipeData(JsonObject json) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getType() {
        return serializer;
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return null;
    }
}
