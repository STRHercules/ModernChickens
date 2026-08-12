package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import strhercules.chickens.recipe.DousingRecipe;

import java.util.List;

public interface DousingRecipeSchema {
   RecipeKey<String> RESULT = StringComponent.STRING
            .outputKey("result")
            .functionNames("result", "output");

    RecipeKey<String> INPUT = StringComponent.STRING
            .inputKey("input")
            .functionNames("input", "chicken");

    RecipeKey<List<CustomObjectRecipeComponent.Value>> REAGENT = new CustomObjectRecipeComponent(List.of(
            new CustomObjectRecipeComponent.Key("type", StringComponent.STRING.instance()),
            new CustomObjectRecipeComponent.Key("id", StringComponent.ID.instance()),
            new CustomObjectRecipeComponent.Key("amount", NumberComponent.POSITIVE_INT.instance(), true)
    )).inputKey("reagent");

    RecipeKey<Integer> ENERGY = NumberComponent.NON_NEGATIVE_INT
            .otherKey("energy")
            .optional(DousingRecipe.DEFAULT_ENERGY)
            .functionNames("energy", "energyCost");

    RecipeSchema SCHEMA = new RecipeSchema(RESULT, INPUT, REAGENT, ENERGY)
            .uniqueId(RESULT);
}
