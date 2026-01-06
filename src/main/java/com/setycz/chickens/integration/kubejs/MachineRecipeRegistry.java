package com.setycz.chickens.integration.kubejs;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for KubeJS-defined machine recipes so block entities and JEI
 * can consume the same custom data without duplicating parsing logic.
 */
public final class MachineRecipeRegistry {
    public enum DousingType {
        FLUID,
        CHEMICAL
    }

    public record DousingRecipe(int inputChickenId,
                                int outputChickenId,
                                DousingType type,
                                ResourceLocation reagentId,
                                int reagentAmount,
                                int energyCost) {
    }

    public record FluidConverterRecipe(ResourceLocation inputFluidId,
                                       ResourceLocation outputFluidId,
                                       int outputAmount) {
    }

    public record ChemicalConverterRecipe(ResourceLocation inputChemicalId,
                                          ResourceLocation outputChemicalId,
                                          int outputAmount) {
    }

    private static final List<DousingRecipe> DOUSING_RECIPES = new ArrayList<>();
    private static final List<FluidConverterRecipe> FLUID_CONVERTER_RECIPES = new ArrayList<>();
    private static final List<ChemicalConverterRecipe> CHEMICAL_CONVERTER_RECIPES = new ArrayList<>();

    private MachineRecipeRegistry() {
    }

    /**
     * Clears all KubeJS recipes so script reloads do not accumulate duplicates.
     */
    public static void clear() {
        DOUSING_RECIPES.clear();
        FLUID_CONVERTER_RECIPES.clear();
        CHEMICAL_CONVERTER_RECIPES.clear();
    }

    public static void addDousingRecipe(DousingRecipe recipe) {
        DOUSING_RECIPES.add(recipe);
    }

    public static void addFluidConverterRecipe(FluidConverterRecipe recipe) {
        FLUID_CONVERTER_RECIPES.add(recipe);
    }

    public static void addChemicalConverterRecipe(ChemicalConverterRecipe recipe) {
        CHEMICAL_CONVERTER_RECIPES.add(recipe);
    }

    public static List<DousingRecipe> getDousingRecipes() {
        return List.copyOf(DOUSING_RECIPES);
    }

    public static List<FluidConverterRecipe> getFluidConverterRecipes() {
        return List.copyOf(FLUID_CONVERTER_RECIPES);
    }

    public static List<ChemicalConverterRecipe> getChemicalConverterRecipes() {
        return List.copyOf(CHEMICAL_CONVERTER_RECIPES);
    }

    public static boolean hasDousingRecipeForInput(int inputChickenId) {
        for (DousingRecipe recipe : DOUSING_RECIPES) {
            if (recipe.inputChickenId() == inputChickenId) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static DousingRecipe findDousingRecipe(DousingType type, int inputChickenId, ResourceLocation reagentId) {
        for (DousingRecipe recipe : DOUSING_RECIPES) {
            if (recipe.type() != type) {
                continue;
            }
            if (recipe.inputChickenId() != inputChickenId) {
                continue;
            }
            if (recipe.reagentId().equals(reagentId)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static FluidConverterRecipe findFluidConverterRecipe(ResourceLocation inputFluidId) {
        for (FluidConverterRecipe recipe : FLUID_CONVERTER_RECIPES) {
            if (recipe.inputFluidId().equals(inputFluidId)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static ChemicalConverterRecipe findChemicalConverterRecipe(ResourceLocation inputChemicalId) {
        for (ChemicalConverterRecipe recipe : CHEMICAL_CONVERTER_RECIPES) {
            if (recipe.inputChemicalId().equals(inputChemicalId)) {
                return recipe;
            }
        }
        return null;
    }
}
