package strhercules.chickens.integration.jei;

import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.GasEggRegistry;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.integration.jei.category.AvianChemicalConverterCategory;
import strhercules.chickens.integration.jei.category.AvianDousingCategory;
import strhercules.chickens.integration.jei.category.AvianFluidConverterCategory;
import strhercules.chickens.integration.jei.category.BreederCategory;
import strhercules.chickens.integration.jei.category.BreedingCategory;
import strhercules.chickens.integration.jei.category.CatchingCategory;
import strhercules.chickens.integration.jei.category.DropCategory;
import strhercules.chickens.integration.jei.category.IncubatorCategory;
import strhercules.chickens.integration.jei.category.HenhousingCategory;
import strhercules.chickens.integration.jei.category.LayingCategory;
import strhercules.chickens.integration.jei.category.RoostingCategory;
import strhercules.chickens.integration.jei.category.TeachingCategory;
import strhercules.chickens.integration.jei.category.ThrowingCategory;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.item.ColoredEggItem;
import strhercules.chickens.item.ChemicalEggItem;
import strhercules.chickens.item.ChickenItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.item.ChickenStats;
import strhercules.chickens.item.GasEggItem;
import strhercules.chickens.item.LiquidEggItem;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
            return String.valueOf(chicken.getId());
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
                new IncubatorCategory(guiHelper),
                new TeachingCategory(guiHelper)
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
        registration.addRecipes(ChickensJeiRecipeTypes.TEACHING, buildTeachingRecipes());
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
        // El libro es el catalizador de la receta de teaching
        registration.addRecipeCatalyst(new ItemStack(Items.BOOK), ChickensJeiRecipeTypes.TEACHING);
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
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        List<ChickensJeiRecipeTypes.DropRecipe> recipes = new ArrayList<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            if (!chicken.isEnabled()) continue;
            ItemStack drop = chicken.createDropItem();
            if (drop.isEmpty()) continue;
            ItemStack chickenStack = chickenItem.createFor(chicken);
            recipes.add(new ChickensJeiRecipeTypes.DropRecipe(chickenStack, drop));
        }
        return recipes;
    }

    private static List<ChickensJeiRecipeTypes.BreedingRecipe> buildBreedingRecipes() {
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        List<ChickensJeiRecipeTypes.BreedingRecipe> recipes = new ArrayList<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            if (!chicken.isEnabled() || !chicken.isBreedable()) {
                continue;
            }
            int chance = Math.round(ChickensRegistry.getChildChance(chicken));
            ItemStack parent1 = chickenItem.createFor(chicken.getParent1());
            ItemStack parent2 = chickenItem.createFor(chicken.getParent2());
            ItemStack child = chickenItem.createFor(chicken);
            recipes.add(new ChickensJeiRecipeTypes.BreedingRecipe(parent1, parent2, child, chance));
        }
        return recipes;
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
        List<ChickensJeiRecipeTypes.RoostingRecipe> recipes = new ArrayList<>();
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            if (!chicken.isEnabled()) continue;
            ItemStack lay = chicken.createLayItem();
            if (lay.isEmpty()) continue;
            ItemStack chickenStack = chickenItem.createFor(chicken);
            chickenStack.setCount(16);
            recipes.add(new ChickensJeiRecipeTypes.RoostingRecipe(chickenStack, lay, 16));
        }
        return recipes;
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

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> chemical = ChickensRegistry.getItems().stream()
                .map(chicken -> createDousingRecipe(chicken, smartEgg, smartChickenStack))
                .filter(Objects::nonNull)
                .toList();

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> liquid = ChickensRegistry.getItems().stream()
                .map(chicken -> createLiquidDousingRecipe(chicken, smartEgg, smartChickenStack))
                .filter(Objects::nonNull)
                .toList();

        List<ChickensJeiRecipeTypes.AvianDousingRecipe> special = buildSpecialDousingRecipes(chickenItem);

        return Stream.of(chemical, liquid, special)
                .flatMap(List::stream)
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

    // Recetas de teaching: libro+gallina vanilla → smart chicken, y right-click con item especial → pollo especial
    private static List<ChickensJeiRecipeTypes.TeachingRecipe> buildTeachingRecipes() {
        ChickensRegistryItem smartChicken = ChickensRegistry.getSmartChicken();
        if (smartChicken == null || !smartChicken.isEnabled()) {
            return List.of();
        }
        ChickenItem chickenItem = (ChickenItem) ModRegistry.CHICKEN_ITEM.get();
        List<ChickensJeiRecipeTypes.TeachingRecipe> list = new ArrayList<>();

        // Receta base: libro + gallina vanilla → smart chicken
        list.add(new ChickensJeiRecipeTypes.TeachingRecipe(
                new ItemStack(Items.BOOK),
                new ItemStack(Items.CHICKEN_SPAWN_EGG),
                chickenItem.createFor(smartChicken)));

        // chickenNosto: tarta de calabaza + gallina vanilla → chickenNosto
        ChickensRegistryItem nostoChicken = ChickensRegistry.getByEntityName("chickenNosto");
        if (nostoChicken != null && nostoChicken.isEnabled()) {
            list.add(new ChickensJeiRecipeTypes.TeachingRecipe(
                    new ItemStack(Items.CAKE),
                    new ItemStack(Items.CHICKEN_SPAWN_EGG),
                    chickenItem.createFor(nostoChicken)));
        }

        // americanChicken: grass_block + gallina vanilla → americanChicken
        ChickensRegistryItem americanChicken = ChickensRegistry.getByEntityName("americanChicken");
        if (americanChicken != null && americanChicken.isEnabled()) {
            list.add(new ChickensJeiRecipeTypes.TeachingRecipe(
                    new ItemStack(Blocks.GRASS_BLOCK),
                    new ItemStack(Items.CHICKEN_SPAWN_EGG),
                    chickenItem.createFor(americanChicken)));
        }

        // dirtChicken: dirt + gallina vanilla → dirtChicken
        ChickensRegistryItem dirtChicken = ChickensRegistry.getByEntityName("dirtChicken");
        if (dirtChicken != null && dirtChicken.isEnabled()) {
            list.add(new ChickensJeiRecipeTypes.TeachingRecipe(
                    new ItemStack(Blocks.DIRT),
                    new ItemStack(Items.CHICKEN_SPAWN_EGG),
                    chickenItem.createFor(dirtChicken)));
        }

        return list;
    }

    private static List<ItemStack> buildHenhouseCatalysts() {
        List<ItemStack> items = new ArrayList<>();
        ModRegistry.getHenhouseItems().stream()
                .map(deferred -> new ItemStack(deferred.get()))
                .forEach(items::add);
        return items;
    }
}
