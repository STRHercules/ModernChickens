package com.setycz.chickens.integration.jei;

import com.setycz.chickens.ChickensMod;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

/**
 * Centralises the custom JEI recipe types so every category and the plugin
 * reference the same identifiers. Each nested record mirrors one of the
 * virtual recipe layouts shipped with the legacy Forge release.
 */
public final class ChickensJeiRecipeTypes {
    public static final RecipeType<LayingRecipe> LAYING = RecipeType.create(ChickensMod.MOD_ID, "laying", LayingRecipe.class);
    public static final RecipeType<DropRecipe> DROPS = RecipeType.create(ChickensMod.MOD_ID, "drops", DropRecipe.class);
    public static final RecipeType<BreedingRecipe> BREEDING = RecipeType.create(ChickensMod.MOD_ID, "breeding", BreedingRecipe.class);
    public static final RecipeType<ThrowingRecipe> THROWING = RecipeType.create(ChickensMod.MOD_ID, "throwing", ThrowingRecipe.class);
    public static final RecipeType<HenhouseRecipe> HENHOUSE = RecipeType.create(ChickensMod.MOD_ID, "henhouse", HenhouseRecipe.class);

    private ChickensJeiRecipeTypes() {
    }

    public record LayingRecipe(ItemStack chicken, ItemStack egg, int minLayTime, int maxLayTime) {
    }

    public record DropRecipe(ItemStack chicken, ItemStack drop) {
    }

    public record BreedingRecipe(ItemStack parent1, ItemStack parent2, ItemStack child, int chancePercent) {
    }

    public record ThrowingRecipe(ItemStack coloredEgg, ItemStack chicken) {
    }

    public record HenhouseRecipe(ItemStack hayBale, ItemStack dirt) {
    }
}
