package strhercules.chickens.integration.jei.category;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.integration.jei.ChickensJeiRecipeTypes;
import strhercules.chickens.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class LavaChickenCategory implements IRecipeCategory<ChickensJeiRecipeTypes.LavaChickenRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/gui/lava_chicken.png");

    private final IDrawable background;
    private final IDrawable icon;

    public LavaChickenCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 90, 77);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.LAVA_CHICKEN.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.LavaChickenRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.LAVA_CHICKEN;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("item.chickens.lava_chicken");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
            ChickensJeiRecipeTypes.LavaChickenRecipe recipe, IFocusGroup focuses) {
        // JEI renders the 16x16 ingredient one pixel inside its 18x18 slot.
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 4)
                .addItemStack(recipe.lavaBucket());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 37, 50)
                .addItemStack(recipe.lavaChicken());
    }
}
