package strhercules.chickens.datagen.providers;

import strhercules.chickens.recipe.DousingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.concurrent.CompletableFuture;

public class ChickensRecipeProvider extends RecipeProvider {

    private static final String MODID = "chickens";

    public ChickensRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput rawOutput) {
        RecipeOutput output = new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement) {
                rawOutput.accept(id, recipe, null);
            }

            @Override
            public net.minecraft.advancements.Advancement.Builder advancement() {
                return rawOutput.advancement();
            }

            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
                rawOutput.accept(id, recipe, null, conditions);
            }
        };

        coloredEgg(output, "white_chicken", Items.WHITE_DYE, 0);
        coloredEgg(output, "yellow_chicken", Items.YELLOW_DYE, 4);
        coloredEgg(output, "blue_chicken", Items.BLUE_DYE, 11);
        coloredEgg(output, "green_chicken", Items.GREEN_DYE, 13);
        coloredEgg(output, "red_chicken", Items.RED_DYE, 14);
        coloredEgg(output, "black_chicken", Items.BLACK_DYE, 15);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("chickens:flying_egg"))
                .requires(Items.EGG)
                .requires(Items.ELYTRA)
                .unlockedBy("has_elytra", has(Items.ELYTRA))
                .save(output, id("flying_egg"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("chickens:analyzer"))
                .requires(Items.COMPASS)
                .requires(Items.EGG)
                .unlockedBy("has_compass", has(Items.COMPASS))
                .save(output, id("analyzer"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:catcher"))
                .pattern("E")
                .pattern("S")
                .pattern("F")
                .define('E', Items.EGG)
                .define('S', Items.STICK)
                .define('F', Items.FEATHER)
                .unlockedBy("has_egg", has(Items.EGG))
                .save(output, id("catcher"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:roost"))
                .pattern("PPP")
                .pattern("PHP")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('H', Items.HAY_BLOCK)
                .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                .save(output, id("roost"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:breeder"))
                .pattern("PPP")
                .pattern("PSP")
                .pattern("HHH")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.WHEAT_SEEDS)
                .define('H', Items.HAY_BLOCK)
                .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                .save(output, id("breeder"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:nest"))
                .pattern("SPS")
                .pattern("RHR")
                .pattern("PPP")
                .define('S', Items.WHEAT_SEEDS)
                .define('P', ItemTags.PLANKS)
                .define('R', item("chickens:roost"))
                .define('H', Items.HAY_BLOCK)
                .unlockedBy("has_roost", has(item("chickens:roost")))
                .save(output, id("nest"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:collector"))
                .pattern("PRP")
                .pattern("PHP")
                .pattern("PCP")
                .define('P', ItemTags.PLANKS)
                .define('R', item("chickens:roost"))
                .define('H', Items.HOPPER)
                .define('C', Items.CHEST)
                .unlockedBy("has_roost", has(item("chickens:roost")))
                .save(output, id("collector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:incubator"))
                .pattern("IFI")
                .pattern("GBG")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('F', item("chickens:flux_egg"))
                .define('G', Items.GLASS)
                .define('B', item("chickens:breeder"))
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy("has_breeder", has(item("chickens:breeder")))
                .save(output, id("incubator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:mechanical_roost"))
                .pattern("IFI")
                .pattern("FRF")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('F', item("chickens:flux_egg"))
                .define('R', item("chickens:roost"))
                .unlockedBy("has_roost", has(item("chickens:roost")))
                .save(output, id("mechanical_roost"));

        henhouse(output, "henhouse", Items.OAK_PLANKS);
        henhouse(output, "henhouse_acacia", Items.ACACIA_PLANKS);
        henhouse(output, "henhouse_birch", Items.BIRCH_PLANKS);
        henhouse(output, "henhouse_dark_oak", Items.DARK_OAK_PLANKS);
        henhouse(output, "henhouse_jungle", Items.JUNGLE_PLANKS);
        henhouse(output, "henhouse_spruce", Items.SPRUCE_PLANKS);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:avian_fluid_converter"))
                .pattern("GWG")
                .pattern("BCB")
                .pattern("ILI")
                .define('G', Items.GLASS)
                .define('W', Items.WATER_BUCKET)
                .define('B', Items.BUCKET)
                .define('C', item("chickens:collector"))
                .define('I', Items.IRON_BLOCK)
                .define('L', Items.LAVA_BUCKET)
                .unlockedBy("has_collector", has(item("chickens:collector")))
                .save(output, id("avian_fluid_converter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:avian_chemical_converter"))
                .pattern("ITI")
                .pattern("BCB")
                .pattern("ITI")
                .define('I', Items.IRON_BLOCK)
                .define('T', item("mekanism:basic_chemical_tank"))
                .define('B', Items.BUCKET)
                .define('C', item("chickens:collector"))
                .unlockedBy("has_collector", has(item("chickens:collector")))
                .save(output, id("avian_chemical_converter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:avian_flux_converter"))
                .pattern("RGR")
                .pattern("GCG")
                .pattern("RIR")
                .define('R', Items.REDSTONE)
                .define('G', Items.GLOWSTONE_DUST)
                .define('C', item("chickens:collector"))
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_collector", has(item("chickens:collector")))
                .save(output, id("avian_flux_converter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:avian_dousing_machine"))
                .pattern("FBF")
                .pattern("RCR")
                .pattern("IGI")
                .define('F', item("chickens:avian_fluid_converter"))
                .define('B', Items.BUCKET)
                .define('R', Items.REDSTONE_BLOCK)
                .define('C', item("chickens:collector"))
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_collector", has(item("chickens:collector")))
                .save(output, id("avian_dousing_machine"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:speedupgrade"))
                .pattern("NEN")
                .pattern("EDE")
                .pattern("NEN")
                .define('N', Items.NETHERITE_INGOT)
                .define('E', Items.ENDER_EYE)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(output, id("speedupgrade"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:stackupgrade"))
                .pattern("NSN")
                .pattern("SDS")
                .pattern("NSN")
                .define('N', Items.NETHERITE_INGOT)
                .define('S', Items.SHULKER_SHELL)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output, id("stackupgrade"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:storagecapacity"))
                .pattern("NEN")
                .pattern("ECE")
                .pattern("NEN")
                .define('N', Items.NETHERITE_INGOT)
                .define('E', Items.ENDER_PEARL)
                .define('C', Items.CHEST)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(output, id("storagecapacity"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:rangeupgrade"))
                .pattern("ENE")
                .pattern("NBN")
                .pattern("ENE")
                .define('E', Items.ENDER_EYE)
                .define('N', Items.NETHERITE_INGOT)
                .define('B', Items.BEACON)
                .unlockedBy("has_beacon", has(Items.BEACON))
                .save(output, id("rangeupgrade"));

        dousingRecipe(output, "avian_dousing_dragon", "obsidianChicken", "dragonChicken",
                "minecraft:dragon_breath", 10);
        dousingRecipe(output, "avian_dousing_wither", "soulSandChicken", "witherChicken",
                "minecraft:nether_star", 10);
    }

    private static void henhouse(RecipeOutput output, String recipeName, Item planks) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:henhouse"))
                .pattern("WWW")
                .pattern("WHW")
                .pattern("YYY")
                .define('W', planks)
                .define('H', Items.HOPPER)
                .define('Y', Items.HAY_BLOCK)
                .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                .save(output, id(recipeName));
    }

    private static void dousingRecipe(RecipeOutput output, String recipeName, String inputChicken,
            String resultChicken, String reagentId, int amount) {
        output.accept(id(recipeName), new DousingRecipe(
                inputChicken,
                resultChicken,
                new DousingRecipe.Reagent(DousingRecipe.ReagentType.ITEM, ResourceLocation.parse(reagentId), amount),
                10_000), null);
    }

    private static void coloredEgg(RecipeOutput output, String recipeName, Item dye, int chickenType) {
        ItemStack result = new ItemStack(item("chickens:colored_egg"));

        CompoundTag tag = new CompoundTag();
        tag.putInt("ChickenType", chickenType);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(chickenType));

        ShapelessRecipe recipe = new ShapelessRecipe(
                "",
                CraftingBookCategory.MISC,
                result,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.EGG), Ingredient.of(dye))
        );

        output.accept(id(recipeName), recipe, null);
    }

    private static Item item(String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        Item result = BuiltInRegistries.ITEM.get(location);
        if (result == Items.AIR && !location.equals(ResourceLocation.withDefaultNamespace("air"))) {
            throw new IllegalStateException("Item not registered at datagen time: " + id);
        }
        return result;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
