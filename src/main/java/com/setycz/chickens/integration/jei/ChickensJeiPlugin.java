package com.setycz.chickens.integration.jei;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.integration.jei.category.AvianChemicalConverterCategory;
import com.setycz.chickens.integration.jei.category.AvianDousingCategory;
import com.setycz.chickens.integration.jei.category.AvianFluidConverterCategory;
import com.setycz.chickens.integration.jei.category.BreederCategory;
import com.setycz.chickens.integration.jei.category.BreedingCategory;
import com.setycz.chickens.integration.jei.category.CatchingCategory;
import com.setycz.chickens.integration.jei.category.DropCategory;
import com.setycz.chickens.integration.jei.category.HenhousingCategory;
import com.setycz.chickens.integration.jei.category.LayingCategory;
import com.setycz.chickens.integration.jei.category.RoostingCategory;
import com.setycz.chickens.integration.jei.category.ThrowingCategory;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.item.ColoredEggItem;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.ChickenItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickenStats;
import com.setycz.chickens.item.GasEggItem;
import com.setycz.chickens.item.LiquidEggItem;
import com.setycz.chickens.registry.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModRegistry.CHICKEN_ITEM.get(), (stack, context) -> {
            if (!(stack.getItem() instanceof ChickenItem)) {
                return IIngredientSubtypeInterpreter.NONE;
            }
            ChickensRegistryItem chicken = ChickenItemHelper.resolve(stack);
            if (chicken == null) {
                return IIngredientSubtypeInterpreter.NONE;
            }
            ChickenStats stats = ChickenItemHelper.getStats(stack);
            return String.format("%d/%d/%d/%d/%s", chicken.getId(), stats.gain(), stats.growth(), stats.strength(),
                    stats.analysed());
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new LayingCategory(guiHelper),
                new DropCategory(guiHelper),
                new BreedingCategory(guiHelper),
                new BreederCategory(guiHelper),
                new ThrowingCategory(guiHelper),
                new HenhousingCategory(guiHelper),
                new RoostingCategory(guiHelper),
                new CatchingCategory(guiHelper),
                new AvianFluidConverterCategory(guiHelper),
                new AvianChemicalConverterCategory(guiHelper),
                new AvianDousingCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ChickensJeiRecipeTypes.LAYING, buildLayingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.DROPS, buildDropRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.BREEDING, buildBreedingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.THROWING, buildThrowingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.HENHOUSE, buildHenhouseRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.ROOSTING, buildRoostingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.CATCHING, buildCatchingRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.BREEDER, buildBreederRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.AVIAN_FLUID_CONVERTER, buildAvianFluidConverterRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.AVIAN_CHEMICAL_CONVERTER, buildAvianChemicalConverterRecipes());
        registration.addRecipes(ChickensJeiRecipeTypes.AVIAN_DOUSING, buildAvianDousingRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.SPAWN_EGG.get()),
                ChickensJeiRecipeTypes.LAYING, ChickensJeiRecipeTypes.DROPS, ChickensJeiRecipeTypes.BREEDING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.COLORED_EGG.get()), ChickensJeiRecipeTypes.THROWING);
        for (ItemStack itemStack : buildHenhouseCatalysts()) {
            registration.addRecipeCatalyst(itemStack, ChickensJeiRecipeTypes.HENHOUSE);
        }
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.ROOST.get()), ChickensJeiRecipeTypes.ROOSTING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.BREEDER.get()), ChickensJeiRecipeTypes.BREEDER);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.CATCHER.get()), ChickensJeiRecipeTypes.CATCHING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.AVIAN_FLUID_CONVERTER_ITEM.get()),
                ChickensJeiRecipeTypes.AVIAN_FLUID_CONVERTER);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.AVIAN_CHEMICAL_CONVERTER_ITEM.get()),
                ChickensJeiRecipeTypes.AVIAN_CHEMICAL_CONVERTER);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.AVIAN_DOUSING_MACHINE_ITEM.get()),
                ChickensJeiRecipeTypes.AVIAN_DOUSING);
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

    private static List<ChickensJeiRecipeTypes.RoostingRecipe> buildRoostingRecipes() {
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        return ChickensRegistry.getItems().stream()
                .filter(ChickensRegistryItem::isEnabled)
                .map(chicken -> {
                    ItemStack stack = chickenItem.createFor(chicken);
                    stack.setCount(16);
                    ItemStack drop = chicken.createDropItem();
                    return new ChickensJeiRecipeTypes.RoostingRecipe(stack, drop, stack.getCount());
                })
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.CatchingRecipe> buildCatchingRecipes() {
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        ItemStack catcher = new ItemStack(ModRegistry.CATCHER.get());
        return ChickensRegistry.getItems().stream()
                .filter(ChickensRegistryItem::isEnabled)
                .map(chicken -> new ChickensJeiRecipeTypes.CatchingRecipe(
                        catcher.copy(),
                        ChickensSpawnEggItem.createFor(chicken),
                        chickenItem.createFor(chicken)))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.BreederRecipe> buildBreederRecipes() {
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS, 2);
        return ChickensRegistry.getItems().stream()
                .filter(chicken -> chicken.isEnabled() && chicken.isBreedable())
                .map(chicken -> new ChickensJeiRecipeTypes.BreederRecipe(
                        chickenItem.createFor(chicken.getParent1()),
                        chickenItem.createFor(chicken.getParent2()),
                        seeds.copy(),
                        chickenItem.createFor(chicken),
                        Math.round(ChickensRegistry.getChildChance(chicken))))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> buildAvianFluidConverterRecipes() {
        return LiquidEggRegistry.getAll().stream()
                .map(liquid -> {
                    FluidStack fluid = liquid.createFluidStack();
                    if (fluid.isEmpty()) {
                        return null;
                    }
                    return new ChickensJeiRecipeTypes.AvianFluidConverterRecipe(
                            LiquidEggItem.createFor(liquid),
                            fluid);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> buildAvianChemicalConverterRecipes() {
        return Stream.concat(
                ChemicalEggRegistry.getAll().stream()
                        .filter(entry -> entry.getVolume() > 0)
                        .map(entry -> new ChickensJeiRecipeTypes.AvianChemicalConverterRecipe(
                                ChemicalEggItem.createFor(entry), entry)),
                GasEggRegistry.getAll().stream()
                        .filter(entry -> entry.getVolume() > 0)
                        .map(entry -> new ChickensJeiRecipeTypes.AvianChemicalConverterRecipe(
                                GasEggItem.createFor(entry), entry)))
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.AvianDousingRecipe> buildAvianDousingRecipes() {
        ChickensRegistryItem smartChicken = ChickensRegistry.getSmartChicken();
        if (smartChicken == null) {
            return List.of();
        }
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        ItemStack smartEgg = ChickensSpawnEggItem.createFor(smartChicken);
        ItemStack smartChickenStack = chickenItem.createFor(smartChicken);
        return ChickensRegistry.getItems().stream()
                .map(chicken -> createDousingRecipe(chicken, smartEgg, smartChickenStack))
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private static ChickensJeiRecipeTypes.AvianDousingRecipe createDousingRecipe(ChickensRegistryItem chicken,
            ItemStack smartEgg, ItemStack smartChicken) {
        ItemStack layItem = chicken.createLayItem();
        if (layItem.isEmpty() || layItem.getItem() != ModRegistry.CHEMICAL_EGG.get()) {
            return null;
        }
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findById(ChickenItemHelper.getChickenType(layItem));
        if (entry == null || entry.getVolume() <= 0) {
            return null;
        }
        ItemStack reagent = ChemicalEggItem.createFor(entry);
        ItemStack result = ChickensSpawnEggItem.createFor(chicken);
        return new ChickensJeiRecipeTypes.AvianDousingRecipe(
                smartEgg.copy(),
                smartChicken.copy(),
                reagent,
                result,
                entry);
    }

    private static List<ItemStack> buildHenhouseCatalysts() {
        List<ItemStack> items = new ArrayList<>();
        ModRegistry.getHenhouseItems().stream()
                .map(deferred -> new ItemStack(deferred.get()))
                .forEach(items::add);
        return items;
    }
}
