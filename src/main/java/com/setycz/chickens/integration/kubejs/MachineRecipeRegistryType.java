package com.setycz.chickens.integration.kubejs;

import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * KubeJS entry point for registering machine recipes with the Modern Chickens mod.
 */
public final class MachineRecipeRegistryType {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensMachineRecipes");

    private MachineRecipeRegistryType() {
    }

    /**
     * Wrapper exposed to JavaScript as ChickensMachineRecipes.register(event => { ... }).
     */
    @RemapPrefixForJS("chickensMachineRecipes$")
    public static class MachineRecipesWrapper {
        public static void chickensMachineRecipes$register(Consumer<MachineRecipeEventJS> callback) {
            LOGGER.info("ChickensMachineRecipes.register called from KubeJS script");

            // Reset per-script recipe state so reloads don't accumulate duplicates.
            MachineRecipeRegistry.clear();
            MachineRecipeEventJS event = new MachineRecipeEventJS();
            try {
                callback.accept(event);
                LOGGER.info("Machine recipe registration callback executed successfully");
            } catch (Exception e) {
                LOGGER.error("Error executing machine recipe registration callback", e);
                throw new RuntimeException("Failed to register machine recipes", e);
            }
        }
    }

    /**
     * Event handler for JavaScript machine recipe definitions.
     */
    public static class MachineRecipeEventJS {
        public void dousingFluid(String inputChicken, String outputChicken, String fluidId, int fluidAmount, int energyCost) {
            ChickensRegistryItem input = resolveChicken(inputChicken);
            ChickensRegistryItem output = resolveChicken(outputChicken);
            if (input == null || output == null) {
                LOGGER.warn("Skipping dousing fluid recipe; input or output chicken missing ({}, {})", inputChicken, outputChicken);
                return;
            }
            ResourceLocation reagent = parseId(fluidId, "fluid");
            MachineRecipeRegistry.addDousingRecipe(new MachineRecipeRegistry.DousingRecipe(
                    input.getId(),
                    output.getId(),
                    MachineRecipeRegistry.DousingType.FLUID,
                    reagent,
                    Math.max(1, fluidAmount),
                    Math.max(0, energyCost)
            ));
        }

        public void dousingChemical(String inputChicken, String outputChicken, String chemicalId, int chemicalAmount, int energyCost) {
            ChickensRegistryItem input = resolveChicken(inputChicken);
            ChickensRegistryItem output = resolveChicken(outputChicken);
            if (input == null || output == null) {
                LOGGER.warn("Skipping dousing chemical recipe; input or output chicken missing ({}, {})", inputChicken, outputChicken);
                return;
            }
            ResourceLocation reagent = parseId(chemicalId, "chemical");
            MachineRecipeRegistry.addDousingRecipe(new MachineRecipeRegistry.DousingRecipe(
                    input.getId(),
                    output.getId(),
                    MachineRecipeRegistry.DousingType.CHEMICAL,
                    reagent,
                    Math.max(1, chemicalAmount),
                    Math.max(0, energyCost)
            ));
        }

        public void fluidConverter(String inputFluidId, String outputFluidId, int outputAmount) {
            ResourceLocation input = parseId(inputFluidId, "input fluid");
            ResourceLocation output = parseId(outputFluidId, "output fluid");
            MachineRecipeRegistry.addFluidConverterRecipe(new MachineRecipeRegistry.FluidConverterRecipe(
                    input,
                    output,
                    Math.max(1, outputAmount)
            ));
        }

        public void chemicalConverter(String inputChemicalId, String outputChemicalId, int outputAmount) {
            ResourceLocation input = parseId(inputChemicalId, "input chemical");
            ResourceLocation output = parseId(outputChemicalId, "output chemical");
            MachineRecipeRegistry.addChemicalConverterRecipe(new MachineRecipeRegistry.ChemicalConverterRecipe(
                    input,
                    output,
                    Math.max(1, outputAmount)
            ));
        }
    }

    private static ResourceLocation parseId(String raw, String label) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Machine recipe " + label + " id cannot be empty");
        }
        if (trimmed.contains(":")) {
            return ResourceLocation.parse(trimmed);
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", trimmed);
    }

    @Nullable
    private static ChickensRegistryItem resolveChicken(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String name = trimmed.contains(":") ? ResourceLocation.parse(trimmed).getPath() : trimmed;
        ChickensRegistryItem byName = ChickensRegistry.getByEntityName(name);
        if (byName != null) {
            return byName;
        }
        try {
            int id = Integer.parseInt(trimmed);
            return ChickensRegistry.getByType(id);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
