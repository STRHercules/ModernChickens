package strhercules.chickens.datagen.providers;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import strhercules.chickens.registry.ModRecipeTypes;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ChickensRecipeProvider extends RecipeProvider {

    private static final String MODID = "chickens";

    public ChickensRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:mechanical_nest"))
                .pattern("IFI")
                .pattern("FRF")
                .pattern("IFI")
                .define('I', Items.IRON_INGOT)
                .define('F', item("chickens:flux_egg"))
                .define('R', item("chickens:nest"))
                .unlockedBy("has_nest", has(item("chickens:nest")))
                .save(output, id("mechanical_nest"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("chickens:rfupgrade"))
                .pattern("NEN")
                .pattern("RDR")
                .pattern("NEN")
                .define('N', Items.NETHERITE_INGOT)
                .define('E', Items.ENDER_EYE)
                .define('R', Items.REDSTONE_BLOCK)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(output, id("rfupgrade"));

        dousingRecipe(output, "avian_dousing_dragon", "obsidianChicken", "dragonChicken",
                "minecraft:dragon_breath", 10);
        dousingRecipe(output, "avian_dousing_wither", "soulSandChicken", "witherChicken",
                "minecraft:nether_star", 10);
    }

    private static void henhouse(Consumer<FinishedRecipe> output, String recipeName, Item planks) {
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

    private static void dousingRecipe(Consumer<FinishedRecipe> output, String recipeName, String inputChicken,
            String resultChicken, String reagentId, int amount) {
        JsonObject reagent = new JsonObject();
        reagent.addProperty("type", "item");
        reagent.addProperty("id", reagentId);
        reagent.addProperty("amount", amount);

        JsonObject json = new JsonObject();
        json.addProperty("input", inputChicken);
        json.addProperty("result", resultChicken);
        json.add("reagent", reagent);
        json.addProperty("energy", 10_000);

        output.accept(new RawFinishedRecipe(id(recipeName), json,
                ModRecipeTypes.AVIAN_DOUSING_SERIALIZER.get()));
    }

    private static void coloredEgg(Consumer<FinishedRecipe> output, String recipeName, Item dye, int chickenType) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ChickenType", chickenType);
        tag.putInt("CustomModelData", chickenType);

        JsonObject result = new JsonObject();
        result.addProperty("item", BuiltInRegistries.ITEM.getKey(item("chickens:colored_egg")).toString());
        result.addProperty("count", 1);
        result.addProperty("nbt", tag.toString());

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(Items.EGG));
        ingredients.add(ingredient(dye));

        JsonObject json = new JsonObject();
        json.add("ingredients", ingredients);
        json.add("result", result);

        output.accept(new RawFinishedRecipe(id(recipeName), json, RecipeSerializer.SHAPELESS_RECIPE));
    }

    private static JsonObject ingredient(Item item) {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(item).toString());
        return json;
    }

    private static Item item(String id) {
        ResourceLocation location = new ResourceLocation(id);
        Item result = BuiltInRegistries.ITEM.get(location);
        if (result == Items.AIR && !location.equals(new ResourceLocation("minecraft", "air"))) {
            throw new IllegalStateException("Item not registered at datagen time: " + id);
        }
        return result;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
