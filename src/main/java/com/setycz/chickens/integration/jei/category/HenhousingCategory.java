package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.integration.jei.ChickensJeiRecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Recreates the henhouse processing category that demonstrates how hay bales
 * are converted into dirt within the block entity.
 */
public class HenhousingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.HenhouseRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/henhouse.png");
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/henhousing_icon.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public HenhousingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 18, 12, 72, 62);
        this.icon = guiHelper.createDrawable(ICON_TEXTURE, 0, 0, 16, 16);
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 195, 0, 12, 57);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.TOP, true);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.HenhouseRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.HENHOUSE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.henhousing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.HenhouseRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 5)
                .addItemStack(recipe.hayBale());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 6, 42)
                .addItemStack(recipe.dirt());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.HenhouseRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 57, 2);
    }
}
