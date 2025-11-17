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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/**
 * Visualises the automated breeder machine. The layout mirrors the block GUI:
 * two parent chickens sit on the left and right, seeds feed the machine, and the
 * resulting offspring appears in the centre slot.
 */
public class BreederCategory implements IRecipeCategory<ChickensJeiRecipeTypes.BreederRecipe> {
    private static final ResourceLocation JEI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/jei.png");
    private static final int BG_WIDTH = 150;
    private static final int BG_HEIGHT = 72;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable rowPanel;
    private final IDrawableAnimated hearts;

    public BreederCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.BREEDER.get()));
        this.rowPanel = guiHelper.createDrawable(JEI_TEXTURE, 0, 0, 90, 18);
        IDrawableStatic heartsTexture = guiHelper.createDrawable(JEI_TEXTURE, 90, 0, 26, 12);
        this.hearts = guiHelper.createAnimatedDrawable(heartsTexture, 200, IDrawableAnimated.StartDirection.LEFT, false);
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
        int panelX = (BG_WIDTH - 90) / 2;
        int panelY = 24;

        builder.addSlot(RecipeIngredientRole.INPUT, panelX + 1, panelY + 1)
                .addItemStack(recipe.parent1());
        builder.addSlot(RecipeIngredientRole.INPUT, panelX + 19, panelY + 1)
                .addItemStack(recipe.parent2());
        builder.addSlot(RecipeIngredientRole.OUTPUT, panelX + 73, panelY + 1)
                .addItemStack(recipe.child());
        // Keep seeds associated for JEI uses without rendering them.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addItemStack(recipe.seeds());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.BreederRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        int panelX = (BG_WIDTH - rowPanel.getWidth()) / 2;
        int panelY = 24;
        rowPanel.draw(graphics, panelX, panelY);
        hearts.draw(graphics, panelX + 41, panelY + 3);
    }

}
