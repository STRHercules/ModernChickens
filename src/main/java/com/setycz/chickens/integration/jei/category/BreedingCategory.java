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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Visualises chicken breeding pairs, including the legacy chance text so
 * players can judge how likely a specific offspring is.
 */
public class BreedingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.BreedingRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/breeding.png");
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/breeding_icon.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public BreedingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawable(ICON_TEXTURE, 0, 0, 16, 16);
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 7, 7);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.BreedingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.BREEDING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.breeding");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.BreedingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 15)
                .addItemStack(recipe.parent1());
        builder.addSlot(RecipeIngredientRole.INPUT, 53, 15)
                .addItemStack(recipe.parent2());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 33, 30)
                .addItemStack(recipe.child());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.BreedingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 37, 5);
        Component message = Component.translatable("gui.breeding.time", recipe.chancePercent());
        graphics.drawString(Minecraft.getInstance().font, message, 32, 25, 0xFF7F7F7F, false);
    }
}
