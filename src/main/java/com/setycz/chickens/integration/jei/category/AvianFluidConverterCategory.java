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
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * JEI category that previews the Avian Fluid Converter. Displays each liquid
 * egg alongside the resulting fluid volume so automation setups can be planned
 * without placing the block.
 */
public final class AvianFluidConverterCategory implements IRecipeCategory<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public AvianFluidConverterCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(132, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.AVIAN_FLUID_CONVERTER_ITEM.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.AvianFluidConverterRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.AVIAN_FLUID_CONVERTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.avian_fluid_converter");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.AvianFluidConverterRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 18)
                .addItemStack(recipe.egg());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.AvianFluidConverterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        FluidStack fluid = recipe.fluid();
        Component fluidName = fluid.isEmpty()
                ? Component.translatable("tooltip.chickens.avian_fluid_converter.empty")
                : fluid.getHoverName();
        Component amount = Component.translatable("gui.chickens.avian_fluid_converter.amount", fluid.getAmount());
        int textColor = 0xFF7F7F7F;
        graphics.drawString(Minecraft.getInstance().font, fluidName, 4, 4, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, amount, 4, 42, textColor, false);
    }
}
