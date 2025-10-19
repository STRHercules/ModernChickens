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
 * Visualises the automated breeder machine. The layout mirrors the block GUI:
 * two parent chickens sit on the left and right, seeds feed the machine, and the
 * resulting offspring appears in the centre slot.
 */
public class BreederCategory implements IRecipeCategory<ChickensJeiRecipeTypes.BreederRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public BreederCategory(IGuiHelper guiHelper) {
        // A minimal blank canvas keeps the dependency footprint small while we wait
        // for bespoke art assets to be ported from the legacy mod.
        this.background = guiHelper.createBlankDrawable(122, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.BREEDER.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.BreederRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.BREEDER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.breeder");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.BreederRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 18)
                .addItemStack(recipe.parent1());
        builder.addSlot(RecipeIngredientRole.INPUT, 96, 18)
                .addItemStack(recipe.parent2());
        builder.addSlot(RecipeIngredientRole.INPUT, 53, 2)
                .addItemStack(recipe.seeds());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 53, 30)
                .addItemStack(recipe.child());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.BreederRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        Component chance = Component.translatable("gui.chickens.breeder.chance", recipe.chancePercent());
        graphics.drawString(Minecraft.getInstance().font, chance, 6, 40, 0xFF7F7F7F, false);
        Component seeds = Component.translatable("gui.chickens.breeder.seeds", recipe.seeds().getCount());
        graphics.drawString(Minecraft.getInstance().font, seeds, 6, 6, 0xFF7F7F7F, false);
    }
}
