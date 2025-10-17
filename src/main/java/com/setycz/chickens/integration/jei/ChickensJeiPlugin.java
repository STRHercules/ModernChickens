package com.setycz.chickens.integration.jei;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.integration.jei.category.BreedingCategory;
import com.setycz.chickens.integration.jei.category.DropCategory;
import com.setycz.chickens.integration.jei.category.HenhousingCategory;
import com.setycz.chickens.integration.jei.category.LayingCategory;
import com.setycz.chickens.integration.jei.category.ThrowingCategory;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.item.ColoredEggItem;
import com.setycz.chickens.registry.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers the Chickens JEI plugin so the modern port exposes the same recipe
 * guides as the original mod. All data is sourced from the live registry so
 * configuration tweaks are reflected instantly.
 */
@JeiPlugin
public class ChickensJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new LayingCategory(guiHelper),
                new DropCategory(guiHelper),
                new BreedingCategory(guiHelper),
                new ThrowingCategory(guiHelper),
                new HenhousingCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ChickensJeiRecipeTypes.LAYING, buildLayingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.DROPS, buildDropRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.BREEDING, buildBreedingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.THROWING, buildThrowingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.HENHOUSE, buildHenhouseRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.SPAWN_EGG.get()),
                ChickensJeiRecipeTypes.LAYING, ChickensJeiRecipeTypes.DROPS, ChickensJeiRecipeTypes.BREEDING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.COLORED_EGG.get()), ChickensJeiRecipeTypes.THROWING);
        for (ItemStack itemStack : buildHenhouseCatalysts()) {
            registration.addRecipeCatalyst(itemStack, ChickensJeiRecipeTypes.HENHOUSE);
        }
    }

    private static List<ChickensJeiRecipeTypes.LayingRecipe> buildLayingRecipes() {
        return ChickensRegistry.getItems().stream()
                .filter(ChickensRegistryItem::isEnabled)
                .map(chicken -> new ChickensJeiRecipeTypes.LayingRecipe(
                        ChickensSpawnEggItem.createFor(chicken),
                        chicken.createLayItem(),
                        chicken.getMinLayTime(),
                        chicken.getMaxLayTime()))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.DropRecipe> buildDropRecipes() {
        return ChickensRegistry.getItems().stream()
                .filter(ChickensRegistryItem::isEnabled)
                .map(chicken -> new ChickensJeiRecipeTypes.DropRecipe(
                        ChickensSpawnEggItem.createFor(chicken),
                        chicken.createDropItem()))
                .filter(drop -> !drop.drop().isEmpty())
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.BreedingRecipe> buildBreedingRecipes() {
        return ChickensRegistry.getItems().stream()
                .filter(chicken -> chicken.isEnabled() && chicken.isBreedable())
                .map(chicken -> new ChickensJeiRecipeTypes.BreedingRecipe(
                        ChickensSpawnEggItem.createFor(chicken.getParent1()),
                        ChickensSpawnEggItem.createFor(chicken.getParent2()),
                        ChickensSpawnEggItem.createFor(chicken),
                        Math.round(ChickensRegistry.getChildChance(chicken))))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.ThrowingRecipe> buildThrowingRecipes() {
        return ChickensRegistry.getItems().stream()
                .filter(chicken -> chicken.isEnabled() && chicken.isDye())
                .map(chicken -> new ChickensJeiRecipeTypes.ThrowingRecipe(
                        ColoredEggItem.createFor(chicken),
                        ChickensSpawnEggItem.createFor(chicken)))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.HenhouseRecipe> buildHenhouseRecipes() {
        return List.of(new ChickensJeiRecipeTypes.HenhouseRecipe(
                new ItemStack(Blocks.HAY_BLOCK),
                new ItemStack(Blocks.DIRT)));
    }

    private static List<ItemStack> buildHenhouseCatalysts() {
        List<ItemStack> items = new ArrayList<>();
        ModRegistry.getHenhouseItems().stream()
                .map(deferred -> new ItemStack(deferred.get()))
                .forEach(items::add);
        return items;
    }
}
