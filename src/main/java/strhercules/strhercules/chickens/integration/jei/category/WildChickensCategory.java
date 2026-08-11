package strhercules.chickens.integration.jei.category;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.SpawnType;
import strhercules.chickens.integration.jei.ChickensJeiRecipeTypes;
import strhercules.chickens.registry.ModRegistry;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Lists only chicken breeds that can be selected by the natural-spawn tables. */
public final class WildChickensCategory implements IRecipeCategory<ChickensJeiRecipeTypes.WildChickenRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public WildChickensCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(180, 42);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.SPAWN_EGG.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.WildChickenRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.WILD_CHICKENS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.wild_chickens");
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
    public void setRecipe(IRecipeLayoutBuilder builder,
            ChickensJeiRecipeTypes.WildChickenRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 13)
                .addItemStack(recipe.chicken());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.WildChickenRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY) {
        Component location = Component.translatable("gui.chickens.wild_chickens.location");
        Component biomes = Component.translatable("gui.chickens.wild_chickens.biome."
                + recipe.spawnType().name().toLowerCase(Locale.ROOT));
        graphics.drawString(Minecraft.getInstance().font, location, 32, 6, 0xFF7F7F7F, false);
        graphics.drawString(Minecraft.getInstance().font, biomes, 32, 19, 0xFF404040, false);
    }
}
