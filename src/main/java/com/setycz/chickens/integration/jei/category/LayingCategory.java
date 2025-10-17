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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Shows the lay time and egg output for each chicken. The layout mirrors the
 * classic JEI category so long-time players immediately recognise the
 * presentation.
 */
public class LayingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.LayingRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/laying.png");
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/laying_icon.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public LayingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawable(ICON_TEXTURE, 0, 0, 16, 16);
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 13, 10);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.LayingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.LAYING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.laying");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.LayingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 13, 15)
                .addItemStack(recipe.chicken());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 57, 15)
                .addItemStack(recipe.egg());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.LayingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 40, 21);

        int minMinutes = recipe.minLayTime() / (20 * 60);
        int maxMinutes = recipe.maxLayTime() / (20 * 60);
        Component message = Component.translatable("gui.laying.time", minMinutes, maxMinutes);
        graphics.drawString(Minecraft.getInstance().font, message, 24, 7, 0xFF7F7F7F, false);
    }
}
