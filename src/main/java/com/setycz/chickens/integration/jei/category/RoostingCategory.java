package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.integration.jei.ChickensJeiRecipeTypes;
import com.setycz.chickens.registry.ModRegistry;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category that mirrors the original Roost production page: a slim row
 * with an animated arrow between the coop's chicken and its drop.
 */
public class RoostingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.RoostingRecipe> {
    private static final ResourceLocation JEI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/jei.png");
    private static final int BG_WIDTH = 140;
    private static final int BG_HEIGHT = 54;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable rowPanel;
    private final IDrawableAnimated arrow;

    public RoostingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.ROOST.get()));
        this.rowPanel = guiHelper.createDrawable(JEI_TEXTURE, 0, 18, 72, 18);
        IDrawableStatic arrowTexture = guiHelper.createDrawable(JEI_TEXTURE, 72, 18, 26, 16);
        this.arrow = guiHelper.createAnimatedDrawable(arrowTexture, 200, IDrawableAnimated.StartDirection.LEFT, false);
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
        int panelX = (BG_WIDTH - 72) / 2;
        int panelY = 18;

        builder.addSlot(RecipeIngredientRole.INPUT, panelX + 1, panelY + 1)
                .addItemStack(recipe.chickenStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, panelX + 55, panelY + 1)
                .addItemStack(recipe.dropStack());
        // Keep the stack size associated for JEI uses without rendering extra text.
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStack(recipe.dropStack());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.RoostingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        int panelX = (BG_WIDTH - rowPanel.getWidth()) / 2;
        int panelY = 18;
        rowPanel.draw(graphics, panelX, panelY);
        arrow.draw(graphics, panelX + 23, panelY + 1);
    }
}
