package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.recipe.DousingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ChickensMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ChickensMod.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DousingRecipe>> AVIAN_DOUSING =
            RECIPE_TYPES.register("avian_dousing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ChickensMod.MOD_ID + ":avian_dousing";
                }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DousingRecipe>> AVIAN_DOUSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("avian_dousing", DousingRecipe.Serializer::new);

    private ModRecipeTypes() {
    }

    public static void init(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
