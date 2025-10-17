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
 * Recreates the coloured egg throwing category so players can discover which
 * dyed eggs summon which chicken breeds.
 */
public class ThrowingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.ThrowingRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/throws.png");
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/throws_icon.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public ThrowingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawable(ICON_TEXTURE, 0, 0, 16, 16);
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 13, 10);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.ThrowingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.THROWING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.throws");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.ThrowingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 15)
                .addItemStack(recipe.coloredEgg());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 55, 15)
                .addItemStack(recipe.chicken());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.ThrowingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 32, 21);
    }
}
