package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.integration.jei.ChickensJeiRecipeTypes;
import com.setycz.chickens.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category that documents the catcher tool workflow. The layout mirrors the
 * original Roost page: a spawn egg and catcher tool combine to produce a caged
 * chicken item.
 */
public class CatchingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.CatchingRecipe> {
    private static final ResourceLocation JEI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/jei.png");
    private static final int BG_WIDTH = 140;
    private static final int BG_HEIGHT = 74;
    private static final int TEXT_COLOR = 0xFF7F7F7F;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable rowPanel;
    private final IDrawable catcherAnim;

    public CatchingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.rowPanel = guiHelper.createDrawable(JEI_TEXTURE, 0, 36, 72, 18);
        ITickTimer timer = guiHelper.createTickTimer(18, 18, false);
        this.catcherAnim = new AnimatedSlice(timer);
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
        int panelX = (BG_WIDTH - 72) / 2;
        int panelY = 18;

        builder.addSlot(RecipeIngredientRole.INPUT, panelX + 1, panelY + 1)
                .addItemStack(recipe.target());
        builder.addSlot(RecipeIngredientRole.OUTPUT, panelX + 55, panelY + 1)
                .addItemStack(recipe.result());
        // Keep the catcher as an input for JEI lookups, but render it only via animation.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addItemStack(recipe.catcher());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.CatchingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        Component info = Component.translatable("gui.chickens.catching.info");
        int panelX = (BG_WIDTH - rowPanel.getWidth()) / 2;
        int panelY = 18;

        rowPanel.draw(graphics, panelX, panelY);
        catcherAnim.draw(graphics, panelX + 24, panelY + 2);
        drawWrapped(graphics, info, 8, 48, BG_WIDTH - 16, TEXT_COLOR);
    }

    private static void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight;
        }
    }

    /**
     * Mimics the original Roost catcher animation by sliding a 14px-tall window
     * down a 24x252 strip over 18 ticks.
     */
    private record AnimatedSlice(ITickTimer timer) implements IDrawable {
        private static final int FRAME_HEIGHT = 14;
        private static final int FRAME_WIDTH = 24;

        @Override
        public int getWidth() {
            return FRAME_WIDTH;
        }

        @Override
        public int getHeight() {
            return FRAME_HEIGHT;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y) {
            int frame = timer.getValue();
            int v = frame * FRAME_HEIGHT;
            graphics.blit(JEI_TEXTURE, x, y, 232, v, FRAME_WIDTH, FRAME_HEIGHT, 256, 256);
        }
    }
}
