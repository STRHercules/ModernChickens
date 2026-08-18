package strhercules.chickens.integration.jei.category;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.integration.jei.ChickensJeiRecipeTypes;
import strhercules.chickens.registry.ModRegistry;
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
import net.minecraft.world.item.ItemStack;

public final class RobotRoosterCategory implements IRecipeCategory<ChickensJeiRecipeTypes.RobotRoosterRecipe> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ChickensMod.MOD_ID, "textures/gui/breeding.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public RobotRoosterCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.ROOSTER_SPAWN_EGG.get()));
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 7, 7);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200,
                IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.RobotRoosterRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.ROBOT_ROOSTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.robot_rooster");
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
            ChickensJeiRecipeTypes.RobotRoosterRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 15)
                .addItemStack(recipe.robotChicken());
        builder.addSlot(RecipeIngredientRole.INPUT, 53, 15)
                .addItemStack(recipe.rooster());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 33, 30)
                .addItemStack(recipe.robotRooster());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.RobotRoosterRecipe recipe,
            IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 37, 5);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("gui.chickens.robot_rooster.overworld_only"),
                4, 6, 0xFF7F7F7F, false);
    }
}
