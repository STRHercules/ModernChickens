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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Data-pack/KubeJS recipe for an Avian Dousing Machine infusion.
 *
 * The machine resolves the named chickens at runtime because Modern Chickens
 * creates many chicken entries dynamically from config and optional mods.
 */
public final class DousingRecipe implements Recipe<SingleRecipeInput> {
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

    public static final MapCodec<DousingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("input").forGetter(DousingRecipe::inputChickenName),
            Codec.STRING.fieldOf("result").forGetter(DousingRecipe::resultChickenName),
            Reagent.CODEC.fieldOf("reagent").forGetter(DousingRecipe::reagent),
            Codec.INT.optionalFieldOf("energy", 10_000).forGetter(DousingRecipe::energyCost)
    ).apply(instance, DousingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, DousingRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeUtf(recipe.inputChickenName);
                buffer.writeUtf(recipe.resultChickenName);
                buffer.writeEnum(recipe.reagent.type());
                ResourceLocation.STREAM_CODEC.encode(buffer, recipe.reagent.id());
                buffer.writeVarInt(recipe.reagent.amount());
                buffer.writeVarInt(recipe.energyCost);
            },
            buffer -> new DousingRecipe(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    new Reagent(buffer.readEnum(ReagentType.class), ResourceLocation.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt()),
                    buffer.readVarInt()));

    private final String inputChickenName;
    private final String resultChickenName;
    private final Reagent reagent;
    private final int energyCost;

    public DousingRecipe(String inputChickenName, String resultChickenName, Reagent reagent, int energyCost) {
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

    @Nullable
    public ChickensRegistryItem resultChicken() {
        return ChickensRegistry.getByEntityName(resultChickenName);
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.size() == 1 && matchesChicken(input.getItem(0));
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ChickensRegistryItem result = resultChicken();
        return result == null ? ItemStack.EMPTY : ChickensSpawnEggItem.createFor(result);
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

    public static final class Serializer implements RecipeSerializer<DousingRecipe> {
        @Override
        public MapCodec<DousingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DousingRecipe> streamCodec() {
            return STREAM_CODEC;
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
