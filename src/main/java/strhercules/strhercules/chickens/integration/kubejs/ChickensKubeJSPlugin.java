package strhercules.chickens.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import strhercules.chickens.registry.ModRecipeTypes;

public class ChickensKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        ChickensKubeEvents.GROUP.register();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(ModRecipeTypes.AVIAN_DOUSING.getId(), DousingRecipeSchema.SCHEMA);
    }
}
