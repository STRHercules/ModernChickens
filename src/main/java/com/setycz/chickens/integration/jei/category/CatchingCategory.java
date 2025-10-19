package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.integration.jei.ChickensJeiRecipeTypes;
import com.setycz.chickens.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category that documents the catcher tool workflow. The layout mirrors the
 * original Roost page: a spawn egg and catcher tool combine to produce a caged
 * chicken item.
 */
public class CatchingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.CatchingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public CatchingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(122, 36);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.CATCHER.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.CatchingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.CATCHING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.catching");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.CatchingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                .addItemStack(recipe.target());
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 10)
                .addItemStack(recipe.catcher());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 10)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.CatchingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        Component info = Component.translatable("gui.chickens.catching.info");
        graphics.drawString(Minecraft.getInstance().font, info, 4, 26, 0xFF7F7F7F, false);
    }
}
