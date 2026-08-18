package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilder;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilderMap;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import strhercules.chickens.recipe.DousingRecipe;

public interface DousingRecipeSchema {
    RecipeKey<String> RESULT = StringComponent.NON_BLANK.key("result").alt("output");

    RecipeKey<String> INPUT = StringComponent.NON_BLANK.key("input").alt("chicken");

    RecipeComponentBuilder REAGENT_COMPONENT = new RecipeComponentBuilder(3)
            .add(StringComponent.NON_BLANK.key("type"))
            .add(StringComponent.ID.key("id"))
            .add(NumberComponent.intRange(1, Integer.MAX_VALUE).key("amount").optional(1))
            .inputRole();

    RecipeKey<RecipeComponentBuilderMap> REAGENT = REAGENT_COMPONENT.key("reagent");

    RecipeKey<Integer> ENERGY = NumberComponent.intRange(0, Integer.MAX_VALUE)
            .key("energy")
            .optional(DousingRecipe.DEFAULT_ENERGY)
            .alt("energyCost");

    RecipeSchema SCHEMA = new RecipeSchema(RESULT, INPUT, REAGENT, ENERGY);
}
