package com.setycz.chickens.integration.jei;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.integration.jei.category.AvianChemicalConverterCategory;
import com.setycz.chickens.integration.jei.category.AvianDousingCategory;
import com.setycz.chickens.integration.jei.category.AvianFluidConverterCategory;
import com.setycz.chickens.integration.jei.category.BreederCategory;
import com.setycz.chickens.integration.jei.category.BreedingCategory;
import com.setycz.chickens.integration.jei.category.CatchingCategory;
import com.setycz.chickens.integration.jei.category.DropCategory;
import com.setycz.chickens.integration.jei.category.IncubatorCategory;
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
import com.setycz.chickens.integration.kubejs.MachineRecipeRegistry;
import com.setycz.chickens.registry.ModRegistry;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
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
                new AvianDousingCategory(guiHelper),
                new IncubatorCategory(guiHelper)
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
        registration.addRecipes(ChickensJeiRecipeTypes.INCUBATOR, buildIncubatorRecipes());
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
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.INCUBATOR_ITEM.get()),
                ChickensJeiRecipeTypes.INCUBATOR);
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

    private static List<ChickensJeiRecipeTypes.IncubatorRecipe> buildIncubatorRecipes() {
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        int energyCost = Math.max(1, ChickensConfigHolder.get().getIncubatorEnergyCost());
        return ChickensRegistry.getItems().stream()
                .filter(ChickensRegistryItem::isEnabled)
                .map(chicken -> new ChickensJeiRecipeTypes.IncubatorRecipe(
                        ChickensSpawnEggItem.createFor(chicken),
                        chickenItem.createFor(chicken),
                        energyCost))
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
        List<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> recipes = new ArrayList<>();
        LiquidEggRegistry.getAll().stream()
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
                .forEach(recipes::add);
        // Append KubeJS-defined conversions so JEI mirrors custom machine behaviour.
        recipes.addAll(buildCustomAvianFluidConverterRecipes());
        return recipes;
    }

    private static List<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> buildCustomAvianFluidConverterRecipes() {
        List<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> recipes = new ArrayList<>();
        for (MachineRecipeRegistry.FluidConverterRecipe recipe : MachineRecipeRegistry.getFluidConverterRecipes()) {
            LiquidEggRegistryItem input = LiquidEggRegistry.findByFluid(recipe.inputFluidId());
            if (input == null) {
                continue;
            }
            FluidStack fluid = new FluidStack(BuiltInRegistries.FLUID.get(recipe.outputFluidId()), recipe.outputAmount());
            if (fluid.isEmpty()) {
                continue;
            }
            recipes.add(new ChickensJeiRecipeTypes.AvianFluidConverterRecipe(
                    LiquidEggItem.createFor(input),
                    fluid));
        }
        return recipes;
    }

    private static List<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> buildAvianChemicalConverterRecipes() {
        List<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> recipes = new ArrayList<>();
        Stream.concat(
                ChemicalEggRegistry.getAll().stream()
                        .filter(entry -> entry.getVolume() > 0)
                        .map(entry -> new ChickensJeiRecipeTypes.AvianChemicalConverterRecipe(
                                ChemicalEggItem.createFor(entry), entry)),
                GasEggRegistry.getAll().stream()
                        .filter(entry -> entry.getVolume() > 0)
                        .map(entry -> new ChickensJeiRecipeTypes.AvianChemicalConverterRecipe(
                                GasEggItem.createFor(entry), entry)))
                .forEach(recipes::add);
        // Append KubeJS-defined chemical conversions so JEI mirrors custom machine behaviour.
        recipes.addAll(buildCustomAvianChemicalConverterRecipes());
        return recipes;
    }

    private static List<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> buildCustomAvianChemicalConverterRecipes() {
        List<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> recipes = new ArrayList<>();
        for (MachineRecipeRegistry.ChemicalConverterRecipe recipe : MachineRecipeRegistry.getChemicalConverterRecipes()) {
            ChemicalEggRegistryItem input = ChemicalEggRegistry.findByChemical(recipe.inputChemicalId());
            if (input == null) {
                input = GasEggRegistry.findByChemical(recipe.inputChemicalId());
            }
            if (input == null) {
                continue;
            }
            ChemicalEggRegistryItem output = ChemicalEggRegistry.findByChemical(recipe.outputChemicalId());
            if (output == null) {
                output = GasEggRegistry.findByChemical(recipe.outputChemicalId());
            }
            if (output == null) {
                continue;
            }
            ItemStack egg = input.isGaseous()
                    ? GasEggItem.createFor(input)
                    : ChemicalEggItem.createFor(input);
            // Clone the entry so JEI reflects custom per-recipe output volumes.
            ChemicalEggRegistryItem displayEntry = new ChemicalEggRegistryItem(
                    output.getId(),
                    output.getChemicalId(),
                    output.getTexture(),
                    output.getDisplayName(),
                    output.getEggColor(),
                    recipe.outputAmount(),
                    output.getHazards(),
                    output.isGaseous());
            recipes.add(new ChickensJeiRecipeTypes.AvianChemicalConverterRecipe(egg, displayEntry));
        }
        return recipes;
    }

    private static List<ChickensJeiRecipeTypes.AvianDousingRecipe> buildAvianDousingRecipes() {
        ChickensRegistryItem smartChicken = ChickensRegistry.getSmartChicken();
        if (smartChicken == null) {
            return List.of();
        }
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        ItemStack smartEgg = ChickensSpawnEggItem.createFor(smartChicken);
        ItemStack smartChickenStack = chickenItem.createFor(smartChicken);

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> chemical = ChickensRegistry.getItems().stream()
                .map(chicken -> createDousingRecipe(chicken, smartEgg, smartChickenStack))
                .filter(Objects::nonNull)
                .toList();

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> liquid = ChickensRegistry.getItems().stream()
                .map(chicken -> createLiquidDousingRecipe(chicken, smartEgg, smartChickenStack))
                .filter(Objects::nonNull)
                .toList();

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> special = buildSpecialDousingRecipes(chickenItem);

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> custom = buildCustomAvianDousingRecipes(chickenItem);

        return Stream.of(chemical, liquid, special, custom)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<ChickensJeiRecipeTypes.AvianDousingRecipe> buildCustomAvianDousingRecipes(ChickenItem chickenItem) {
        List<ChickensJeiRecipeTypes.AvianDousingRecipe> recipes = new ArrayList<>();
        for (MachineRecipeRegistry.DousingRecipe recipe : MachineRecipeRegistry.getDousingRecipes()) {
            ChickensRegistryItem inputChicken = ChickensRegistry.getByType(recipe.inputChickenId());
            ChickensRegistryItem outputChicken = ChickensRegistry.getByType(recipe.outputChickenId());
            if (inputChicken == null || outputChicken == null) {
                continue;
            }
            ItemStack inputEgg = ChickensSpawnEggItem.createFor(inputChicken);
            ItemStack inputChickenStack = chickenItem.createFor(inputChicken);
            ItemStack result = ChickensSpawnEggItem.createFor(outputChicken);

            if (recipe.type() == MachineRecipeRegistry.DousingType.CHEMICAL) {
                ChemicalEggRegistryItem entry = ChemicalEggRegistry.findByChemical(recipe.reagentId());
                if (entry == null) {
                    entry = GasEggRegistry.findByChemical(recipe.reagentId());
                }
                if (entry == null) {
                    continue;
                }
                ItemStack reagent = entry.isGaseous()
                        ? GasEggItem.createFor(entry)
                        : ChemicalEggItem.createFor(entry);
                MekanismJeiChemicalHelper.JeiChemicalStack chemical = MekanismJeiChemicalHelper.createStack(
                        entry,
                        recipe.reagentAmount());
                recipes.add(new ChickensJeiRecipeTypes.AvianDousingRecipe(
                        inputEgg.copy(),
                        inputChickenStack.copy(),
                        reagent,
                        result,
                        entry,
                        chemical,
                        null,
                        recipe.reagentAmount(),
                        recipe.energyCost()));
            } else if (recipe.type() == MachineRecipeRegistry.DousingType.FLUID) {
                LiquidEggRegistryItem liquid = LiquidEggRegistry.findByFluid(recipe.reagentId());
                if (liquid == null) {
                    continue;
                }
                FluidStack fluid = new FluidStack(BuiltInRegistries.FLUID.get(recipe.reagentId()), recipe.reagentAmount());
                if (fluid.isEmpty()) {
                    continue;
                }
                ItemStack reagent = LiquidEggItem.createFor(liquid);
                recipes.add(new ChickensJeiRecipeTypes.AvianDousingRecipe(
                        inputEgg.copy(),
                        inputChickenStack.copy(),
                        reagent,
                        result,
                        null,
                        null,
                        fluid,
                        recipe.reagentAmount(),
                        recipe.energyCost()));
            }
        }
        return recipes;
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
        MekanismJeiChemicalHelper.JeiChemicalStack chemical = MekanismJeiChemicalHelper.createStack(
                entry,
                AvianDousingMachineBlockEntity.CHEMICAL_COST);
        return new ChickensJeiRecipeTypes.AvianDousingRecipe(
                smartEgg.copy(),
                smartChicken.copy(),
                reagent,
                result,
                entry,
                chemical,
                null,
                AvianDousingMachineBlockEntity.CHEMICAL_COST,
                AvianDousingMachineBlockEntity.CHEMICAL_ENERGY_COST);
    }

    @Nullable
    private static ChickensJeiRecipeTypes.AvianDousingRecipe createLiquidDousingRecipe(ChickensRegistryItem chicken,
            ItemStack smartEgg, ItemStack smartChicken) {
        ItemStack layItem = chicken.createLayItem();
        if (layItem.isEmpty() || !(layItem.getItem() instanceof LiquidEggItem)) {
            return null;
        }
        int liquidId = ChickenItemHelper.getChickenType(layItem);
        LiquidEggRegistryItem entry = LiquidEggRegistry.findById(liquidId);
        if (entry == null) {
            return null;
        }
        int liquidCost = chicken.getLiquidDousingCost();
        FluidStack fluid = new FluidStack(entry.getFluid(), liquidCost);
        if (fluid.isEmpty()) {
            return null;
        }
        // Use the liquid egg as the displayed reagent so JEI "uses" on the egg shows the dousing recipe.
        ItemStack reagent = LiquidEggItem.createFor(entry);
        ItemStack result = ChickensSpawnEggItem.createFor(chicken);
        return new ChickensJeiRecipeTypes.AvianDousingRecipe(
                smartEgg.copy(),
                smartChicken.copy(),
                reagent,
                result,
                null,
                null,
                fluid,
                liquidCost,
                AvianDousingMachineBlockEntity.LIQUID_ENERGY_COST);
    }

    private static List<ChickensJeiRecipeTypes.AvianDousingRecipe> buildSpecialDousingRecipes(ChickenItem chickenItem) {
        List<ChickensJeiRecipeTypes.AvianDousingRecipe> list = new ArrayList<>();
        ChickensRegistryItem obsidian = ChickensRegistry.getByEntityName("obsidianChicken");
        ChickensRegistryItem dragon = ChickensRegistry.getByEntityName("dragonChicken");
        if (obsidian != null && dragon != null) {
            list.add(createSpecialDousingRecipe(obsidian, dragon, chickenItem,
                    new ItemStack(Items.DRAGON_BREATH, AvianDousingMachineBlockEntity.SPECIAL_LIQUID_CAPACITY / AvianDousingMachineBlockEntity.SPECIAL_PER_ITEM)));
        }
        ChickensRegistryItem soulSand = ChickensRegistry.getByEntityName("SoulSandChicken");
        ChickensRegistryItem wither = ChickensRegistry.getByEntityName("witherChicken");
        if (soulSand != null && wither != null) {
            list.add(createSpecialDousingRecipe(soulSand, wither, chickenItem,
                    new ItemStack(Items.NETHER_STAR, AvianDousingMachineBlockEntity.SPECIAL_LIQUID_CAPACITY / AvianDousingMachineBlockEntity.SPECIAL_PER_ITEM)));
        }
        return list;
    }

    private static ChickensJeiRecipeTypes.AvianDousingRecipe createSpecialDousingRecipe(ChickensRegistryItem base,
                                                                                        ChickensRegistryItem target,
                                                                                        ChickenItem chickenItem,
                                                                                        ItemStack reagent) {
        return new ChickensJeiRecipeTypes.AvianDousingRecipe(
                ChickensSpawnEggItem.createFor(base),
                chickenItem.createFor(base),
                reagent,
                ChickensSpawnEggItem.createFor(target),
                null,
                null,
                null,
                AvianDousingMachineBlockEntity.SPECIAL_LIQUID_CAPACITY,
                AvianDousingMachineBlockEntity.SPECIAL_ENERGY_COST);
    }

    private static List<ItemStack> buildHenhouseCatalysts() {
        List<ItemStack> items = new ArrayList<>();
        ModRegistry.getHenhouseItems().stream()
                .map(deferred -> new ItemStack(deferred.get()))
                .forEach(items::add);
        return items;
    }
}
