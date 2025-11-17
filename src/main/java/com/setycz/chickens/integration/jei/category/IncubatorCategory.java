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
 * Simple JEI category that showcases the Incubator's spawn-egg-to-chicken conversion and RF cost.
 */
public final class IncubatorCategory implements IRecipeCategory<ChickensJeiRecipeTypes.IncubatorRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public IncubatorCategory(IGuiHelper guiHelper) {
        // Give the description room so text never overflows the JEI panel.
        this.background = guiHelper.createBlankDrawable(148, 70);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.INCUBATOR_ITEM.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.IncubatorRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.INCUBATOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.incubator");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.IncubatorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 26)
                .addItemStack(recipe.spawnEgg());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 26)
                .addItemStack(recipe.chicken());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.IncubatorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        Component energy = Component.translatable("tooltip.chickens.incubator.cost", recipe.energyCost());
        Component hint = Component.translatable("tooltip.chickens.incubator");
        int color = 0xFF7F7F7F;
        graphics.drawString(Minecraft.getInstance().font, energy, 8, 6, color, false);
        drawWrapped(graphics, hint, 8, 48, 132, color);
    }

    private static void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight;
        }
    }
}
