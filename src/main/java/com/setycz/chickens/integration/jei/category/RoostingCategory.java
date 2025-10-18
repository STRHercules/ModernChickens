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
 * JEI category that showcases the Roost block production cycle. Although we do
 * not yet have the bespoke animated arrow from the original mod, the layout
 * keeps the same slot ordering so veteran players immediately recognise it.
 */
public class RoostingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.RoostingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public RoostingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(122, 36);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.ROOST.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.RoostingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.ROOSTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.roosting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.RoostingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                .addItemStack(recipe.chickenStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 10)
                .addItemStack(recipe.dropStack());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.RoostingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        Component stackSize = Component.translatable("gui.chickens.roosting.stack", recipe.stackSize());
        graphics.drawString(Minecraft.getInstance().font, stackSize, 4, 26, 0xFF7F7F7F, false);
    }
}
