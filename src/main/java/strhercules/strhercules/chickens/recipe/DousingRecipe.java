package strhercules.chickens.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.item.ChickenItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.registry.ModRecipeTypes;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Locale;


public final class DousingRecipe implements Recipe<Container> {
    /** Redstone Flux consumed per cycle when a recipe does not specify it. */
    public static final int DEFAULT_ENERGY = 10_000;

    public enum ReagentType {
        ITEM,
        FLUID,
        CHEMICAL;

        private static final Codec<ReagentType> CODEC = Codec.STRING.comapFlatMap(value -> {
            try {
                return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return DataResult.error(() -> "Unknown dousing reagent type: " + value);
            }
        }, type -> type.name().toLowerCase(Locale.ROOT));
    }

    public record Reagent(ReagentType type, ResourceLocation id, int amount) {
        private static final MapCodec<Reagent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ReagentType.CODEC.fieldOf("type").forGetter(Reagent::type),
                ResourceLocation.CODEC.fieldOf("id").forGetter(Reagent::id),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Reagent::amount)
        ).apply(instance, Reagent::new));

        public Reagent {
            amount = Math.max(1, amount);
        }
    }

    private static final ResourceLocation EMPTY_ID = new ResourceLocation("chickens", "dousing");

    public static final MapCodec<DousingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("input").forGetter(DousingRecipe::inputChickenName),
            Codec.STRING.fieldOf("result").forGetter(DousingRecipe::resultChickenName),
            Reagent.CODEC.fieldOf("reagent").forGetter(DousingRecipe::reagent),
            Codec.INT.optionalFieldOf("energy", DEFAULT_ENERGY).forGetter(DousingRecipe::energyCost)
    ).apply(instance, (input, result, reagent, energy) ->
            new DousingRecipe(EMPTY_ID, input, result, reagent, energy)));



    private final ResourceLocation id;
    private final String inputChickenName;
    private final String resultChickenName;
    private final Reagent reagent;
    private final int energyCost;

    public DousingRecipe(ResourceLocation id, String inputChickenName, String resultChickenName, Reagent reagent,
            int energyCost) {
        this.id = id;
        this.inputChickenName = inputChickenName;
        this.resultChickenName = resultChickenName;
        this.reagent = reagent;
        this.energyCost = Math.max(0, energyCost);
    }

    public String inputChickenName() {
        return inputChickenName;
    }

    public String resultChickenName() {
        return resultChickenName;
    }

    public Reagent reagent() {
        return reagent;
    }

    public ReagentType reagentType() {
        return reagent.type();
    }

    public ResourceLocation reagentId() {
        return reagent.id();
    }

    public int reagentAmount() {
        return reagent.amount();
    }

    public int energyCost() {
        return energyCost;
    }

    public boolean matchesChicken(ItemStack stack) {
        ChickensRegistryItem chicken = resolveChicken(stack);
        return chicken != null && chicken.getEntityName().equalsIgnoreCase(inputChickenName);
    }

    public boolean matchesInput(ItemStack stack) {
        if (matchesChicken(stack)) {
            return true;
        }
        Item item = inputItem();
        return item != null && stack.is(item);
    }

    @Nullable
    public ChickensRegistryItem inputChicken() {
        return ChickensRegistry.getByEntityName(inputChickenName);
    }

    @Nullable
    public ChickensRegistryItem resultChicken() {
        return ChickensRegistry.getByEntityName(resultChickenName);
    }

    @Nullable
    public Item inputItem() {
        return resolveItem(inputChickenName);
    }

    @Nullable
    public Item resultItem() {
        return resolveItem(resultChickenName);
    }

    public ItemStack resultStack() {
        ChickensRegistryItem chicken = resultChicken();
        if (chicken != null) {
            return ChickensSpawnEggItem.createFor(chicken);
        }
        Item item = resultItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public ItemStack inputDisplayStack() {
        ChickensRegistryItem chicken = inputChicken();
        if (chicken != null) {
            return ChickensSpawnEggItem.createFor(chicken);
        }
        Item item = inputItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Nullable
    private static Item resolveItem(String name) {
        if (ChickensRegistry.getByEntityName(name) != null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? null : item;
    }

    @Nullable
    public ItemStack reagentItem() {
        if (reagent.type() != ReagentType.ITEM) {
            return null;
        }
        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(reagent.id()));
        return stack.is(Items.AIR) ? null : stack;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return input.getContainerSize() >= 1 && matchesInput(input.getItem(0));
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registryAccess) {
        return resultStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return resultStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        ItemStack item = reagentItem();
        NonNullList<Ingredient> ingredients = NonNullList.create();
        if (item != null) {
            ingredients.add(Ingredient.of(item.getItem()));
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.AVIAN_DOUSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.AVIAN_DOUSING.get();
    }

    public DousingRecipe withId(ResourceLocation newId) {
        return new DousingRecipe(newId, inputChickenName, resultChickenName, reagent, energyCost);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public static final class Serializer implements RecipeSerializer<DousingRecipe> {
        @Override
        public DousingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            DousingRecipe parsed = CODEC.codec().parse(JsonOps.INSTANCE, json)
                    .getOrThrow(false, message -> {
                        throw new com.google.gson.JsonParseException("Invalid dousing recipe " + recipeId + ": " + message);
                    });
            return parsed.withId(recipeId);
        }

        @Override
        public DousingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String input = buffer.readUtf();
            String result = buffer.readUtf();
            Reagent reagent = new Reagent(buffer.readEnum(ReagentType.class), buffer.readResourceLocation(),
                    buffer.readVarInt());
            return new DousingRecipe(recipeId, input, result, reagent, buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, DousingRecipe recipe) {
            buffer.writeUtf(recipe.inputChickenName);
            buffer.writeUtf(recipe.resultChickenName);
            buffer.writeEnum(recipe.reagent.type());
            buffer.writeResourceLocation(recipe.reagent.id());
            buffer.writeVarInt(recipe.reagent.amount());
            buffer.writeVarInt(recipe.energyCost);
        }
    }

    @Nullable
    private static ChickensRegistryItem resolveChicken(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ChickenItem
                || stack.getItem() instanceof ChickensSpawnEggItem)) {
            return null;
        }
        return ChickensRegistry.getByType(ChickenItemHelper.getChickenType(stack));
    }
}
