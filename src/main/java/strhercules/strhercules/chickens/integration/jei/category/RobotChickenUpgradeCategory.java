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

public final class RobotChickenUpgradeCategory
        implements IRecipeCategory<ChickensJeiRecipeTypes.RobotChickenUpgradeRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChickensMod.MOD_ID, "textures/gui/laying.png");
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public RobotChickenUpgradeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 82, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.SPEED_UPGRADE.get()));
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(TEXTURE, 82, 0, 13, 10);
        this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.RobotChickenUpgradeRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.ROBOT_CHICKEN_UPGRADE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.robot_chicken");
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
            ChickensJeiRecipeTypes.RobotChickenUpgradeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 19)
                .addItemStacks(recipe.upgrades());
        builder.addSlot(RecipeIngredientRole.INPUT, 24, 19)
                .addItemStack(recipe.smartChicken());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 57, 19)
                .addItemStack(recipe.robotChicken());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.RobotChickenUpgradeRecipe recipe,
            IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 38, 21);
        Component requirement = Component.translatable("gui.chickens.robot_chicken.requirement",
                recipe.minimumUpgrades(), recipe.maximumUpgrades());
        graphics.drawString(Minecraft.getInstance().font, requirement, 2, 5, 0xFF7F7F7F, false);
    }
}
