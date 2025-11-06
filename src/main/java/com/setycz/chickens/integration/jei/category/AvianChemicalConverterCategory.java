package com.setycz.chickens.integration.jei.category;

import com.setycz.chickens.ChemicalEggRegistryItem;
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
 * JEI category that previews the Avian Chemical Converter. The layout mirrors the
 * fluid converter category but substitutes Mekanism chemical data so players can
 * review volumes and states even when the Mekanism API is absent at compile time.
 */
public final class AvianChemicalConverterCategory
        implements IRecipeCategory<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public AvianChemicalConverterCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(132, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.AVIAN_CHEMICAL_CONVERTER_ITEM.get()));
    }

    @Override
    public RecipeType<ChickensJeiRecipeTypes.AvianChemicalConverterRecipe> getRecipeType() {
        return ChickensJeiRecipeTypes.AVIAN_CHEMICAL_CONVERTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.chickens.avian_chemical_converter");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ChickensJeiRecipeTypes.AvianChemicalConverterRecipe recipe,
            IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 18)
                .addItemStack(recipe.egg());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.AvianChemicalConverterRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY) {
        ChemicalEggRegistryItem entry = recipe.entry();
        Component name = entry.getDisplayName();
        Component amount = Component.translatable("gui.chickens.avian_chemical_converter.amount", entry.getVolume());
        Component state = entry.isGaseous()
                ? Component.translatable("gui.chickens.avian_chemical_converter.state.gas")
                : Component.translatable("gui.chickens.avian_chemical_converter.state.chemical");
        Component idLine = Component.translatable("gui.chickens.avian_chemical_converter.chemical",
                entry.getChemicalId().toString());
        int textColor = 0xFF7F7F7F;
        graphics.drawString(Minecraft.getInstance().font, name, 4, 4, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, amount, 4, 24, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, state, 4, 34, textColor, false);
        graphics.drawString(Minecraft.getInstance().font, idLine, 4, 44, textColor, false);
    }
}
