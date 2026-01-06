package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import com.setycz.chickens.integration.jei.ChickensJeiRecipeTypes;
import com.setycz.chickens.integration.jei.MekanismJeiChemicalHelper;
import com.setycz.chickens.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
 * JEI category visualising the Avian Dousing Machine's chemical infusion path so players
 * can confirm which reagent produces each chemical chicken spawn egg.
 */
public final class AvianDousingCategory implements IRecipeCategory<ChickensJeiRecipeTypes.AvianDousingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public AvianDousingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(162, 74);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.AVIAN_DOUSING_MACHINE_ITEM.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.AvianDousingRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.AVIAN_DOUSING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.avian_dousing_machine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.AvianDousingRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder reagentSlot = builder.addSlot(RecipeIngredientRole.INPUT, 20, 52);
        MekanismJeiChemicalHelper.JeiChemicalStack chemical = recipe.chemical();
        if (chemical != null) {
            reagentSlot.addIngredient(chemical.type(), chemical.stack());
        } else {
            reagentSlot.addItemStack(recipe.reagent());
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 58, 52)
                .addItemStack(recipe.inputEgg())
                .addItemStack(recipe.inputChicken());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 52)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.AvianDousingRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY) {
        Component input = Component.translatable("gui.chickens.avian_dousing_machine.input");
        // Null guard: datapacks or fallback item recipes may omit a chemical entry; show a placeholder instead of crashing.
        Component chemicalName = recipe.entry() != null
                ? recipe.entry().getDisplayName()
                : recipe.reagent().getHoverName();
        String chemicalId = recipe.entry() != null
                ? recipe.entry().getChemicalId().toString()
                : "missing";
        Component chemical = Component.translatable("gui.chickens.avian_dousing_machine.chemical",
                chemicalName, chemicalId);
        Component volume = Component.translatable("gui.chickens.avian_dousing_machine.volume", recipe.fluidCost());
        Component energy = Component.translatable("gui.chickens.avian_dousing_machine.energy", recipe.energyCost());
        int textColor = 0xFF7F7F7F;
        graphics.drawString(Minecraft.getInstance().font, input, 4, 4, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, chemical, 4, 16, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, volume, 4, 28, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, energy, 4, 40, textColor, false);
    }
}
