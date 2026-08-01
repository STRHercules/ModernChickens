package strhercules.chickens.integration.jei.category;

import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import strhercules.chickens.integration.jei.ChickensJeiRecipeTypes;
import strhercules.chickens.integration.jei.MekanismJeiChemicalHelper;
import strhercules.chickens.registry.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
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
        } else if (recipe.fluid() != null && !recipe.fluid().isEmpty()) {
            reagentSlot.addIngredient(NeoForgeTypes.FLUID_STACK, recipe.fluid());
        } else {
            reagentSlot.addItemStack(recipe.reagent());
        }

        // INPUT: the base egg/chicken to be dosed
        builder.addSlot(RecipeIngredientRole.INPUT, 58, 52)
                .addItemStack(recipe.inputEgg())
                .addItemStack(recipe.inputChicken());

        // OUTPUT: the resulting spawn egg.
        // This was already present but now all three reagent paths correctly link
        // their ingredient type so JEI can filter both from input and output focus.
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 52)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(ChickensJeiRecipeTypes.AvianDousingRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY) {
        Component input = Component.translatable("gui.chickens.avian_dousing_machine.input");
        int textColor = 0xFF7F7F7F;
        graphics.drawString(Minecraft.getInstance().font, input, 4, 4, textColor, false);

        // Fluid recipes show amount + energy; chemical recipes show chemical id + volume + energy;
        // item reagent recipes (dragon breath etc.) show energy only.
        if (recipe.fluid() != null && !recipe.fluid().isEmpty()) {
            Component volume = Component.translatable("gui.chickens.avian_dousing_machine.volume",
                    recipe.fluidCost());
            Component energy = Component.translatable("gui.chickens.avian_dousing_machine.energy",
                    recipe.energyCost());
            graphics.drawString(Minecraft.getInstance().font, volume, 4, 16, textColor, false);
            graphics.drawString(Minecraft.getInstance().font, energy, 4, 28, textColor, false);
        } else if (recipe.entry() != null) {
            Component chemicalName = recipe.entry().getDisplayName();
            String chemicalId = recipe.entry().getChemicalId().toString();
            Component chemical = Component.translatable("gui.chickens.avian_dousing_machine.chemical",
                    chemicalName, chemicalId);
            Component volume = Component.translatable("gui.chickens.avian_dousing_machine.volume",
                    AvianDousingMachineBlockEntity.CHEMICAL_COST);
            Component energy = Component.translatable("gui.chickens.avian_dousing_machine.energy",
                    AvianDousingMachineBlockEntity.CHEMICAL_ENERGY_COST);
            graphics.drawString(Minecraft.getInstance().font, chemical, 4, 16, textColor, false);
            graphics.drawString(Minecraft.getInstance().font, volume, 4, 28, textColor, false);
            graphics.drawString(Minecraft.getInstance().font, energy, 4, 40, textColor, false);
        } else {
            // Special item reagent (dragon breath, nether star)
            Component energy = Component.translatable("gui.chickens.avian_dousing_machine.energy",
                    recipe.energyCost());
            graphics.drawString(Minecraft.getInstance().font, energy, 4, 16, textColor, false);
        }
    }
}