package strhercules.chickens.integration.jei.category;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.integration.jei.ChickensJeiRecipeTypes;
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

public class TeachingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.TeachingRecipe> {
    // Reutilizamos la misma textura de fondo que LayingCategory
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/laying.png");
    // El icono es el propio item del libro; no necesitamos una textura separada.
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID, "textures/gui/laying_icon.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public TeachingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawable(ICON_TEXTURE, 0, 0, 16, 16);
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 13, 10);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.TeachingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.TEACHING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.teaching");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.TeachingRecipe recipe, IFocusGroup focuses) {
        // Slot 1: el ítem trigger (libro, tarta, grass_block, etc.)
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 19)
                .addItemStack(recipe.book());
        // Slot 2: la gallina vanilla (spawn egg)
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 19)
                .addItemStack(recipe.vanillaChicken());
        // Slot OUTPUT: el ChickenItem resultante (imagen del pollo, NO el huevo)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 57, 19)
                .addItemStack(recipe.smartChicken());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.TeachingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        arrow.draw(graphics, 38, 21);
        Component hint = Component.translatable("gui.chickens.teaching.hint");
        graphics.drawString(Minecraft.getInstance().font, hint, 5, 7, 0xFF7F7F7F, false);
    }
}