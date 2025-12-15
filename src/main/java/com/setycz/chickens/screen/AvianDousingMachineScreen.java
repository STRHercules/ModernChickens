package com.setycz.chickens.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity;
import com.setycz.chickens.menu.AvianDousingMachineMenu;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity.InfusionMode;
import com.setycz.chickens.blockentity.AvianDousingMachineBlockEntity.SpecialInfusion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.util.Mth;

/**
 * Client GUI for the Avian Dousing Machine. Renders the combined energy,
 * chemical, and liquid gauges plus a progress indicator showing when the next
 * Smart Chicken infusion will complete.
 */
public class AvianDousingMachineScreen extends AbstractContainerScreen<AvianDousingMachineMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/douser.png");

    private static final int CHEM_BAR_X = 9;
    private static final int CHEM_BAR_Y = 14;
    private static final int LIQUID_BAR_X = 26;
    private static final int LIQUID_BAR_Y = 14;
    private static final int ENERGY_BAR_X = 148;
    private static final int ENERGY_BAR_Y = 13;
    private static final int PROGRESS_X = 82;
    private static final int PROGRESS_Y = 36;
    private static final int BAR_WIDTH = 12;
    private static final int BAR_HEIGHT = 57;
    private static final int PROGRESS_WIDTH = 18;
    private static final int PROGRESS_HEIGHT = 13;
    private static final int BAR_TEXTURE_X = 195;
    private static final int BAR_TEXTURE_Y = 0;
    private static final int ENERGY_TEXTURE_X = 208;
    private static final int ENERGY_TEXTURE_Y = 0;
    private static final int TEXTURE_SIZE = 256;

    public AvianDousingMachineScreen(AvianDousingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        renderChemicalBar(graphics, x, y);
        renderLiquidBar(graphics, x, y);
        renderEnergyBar(graphics, x, y);
        renderProgressArrow(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltips(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderChemicalBar(GuiGraphics graphics, int originX, int originY) {
        int amount = menu.getChemicalAmount();
        int capacity = Math.max(menu.getChemicalCapacity(), 1);
        if (amount <= 0) {
            return;
        }
        int filled = Math.min(BAR_HEIGHT, amount * BAR_HEIGHT / capacity);
        if (filled <= 0) {
            return;
        }
        int offset = BAR_HEIGHT - filled;
        int color = 0xFFFFFF;
        var entry = menu.getStoredChemical();
        if (entry != null) {
            color = entry.getEggColor();
        }
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        graphics.blit(GUI_TEXTURE,
                originX + CHEM_BAR_X,
                originY + CHEM_BAR_Y + offset,
                BAR_TEXTURE_X,
                BAR_TEXTURE_Y + offset,
                BAR_WIDTH,
                filled,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderLiquidBar(GuiGraphics graphics, int originX, int originY) {
        int amount = menu.getFluidAmount();
        int capacity = Math.max(menu.getFluidCapacity(), 1);
        if (amount <= 0) {
            return;
        }
        int filled = Math.min(BAR_HEIGHT, amount * BAR_HEIGHT / capacity);
        if (filled <= 0) {
            return;
        }
        int offset = BAR_HEIGHT - filled;
        FluidStack stack = menu.getFluid();
        int color = colorForSpecial(menu);
        if (color == -1) {
            color = 0x3FA7FF;
            if (!stack.isEmpty()) {
                color = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
            }
        }
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        graphics.blit(GUI_TEXTURE,
                originX + LIQUID_BAR_X,
                originY + LIQUID_BAR_Y + offset,
                BAR_TEXTURE_X,
                BAR_TEXTURE_Y + offset,
                BAR_WIDTH,
                filled,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderEnergyBar(GuiGraphics graphics, int originX, int originY) {
        int energy = getDisplayedEnergy();
        int capacity = Math.max(getDisplayedCapacity(), 1);
        if (energy <= 0) {
            return;
        }
        int filled = energy > 0
                ? Math.max(1, Math.min(BAR_HEIGHT, Mth.ceil(energy * BAR_HEIGHT / (float) capacity)))
                : 0;
        if (filled <= 0) {
            return;
        }
        int offset = BAR_HEIGHT - filled;
        RenderSystem.setShaderColor(0.95F, 0.75F, 0.20F, 1.0F);
        graphics.blit(GUI_TEXTURE,
                originX + ENERGY_BAR_X,
                originY + ENERGY_BAR_Y + offset,
                ENERGY_TEXTURE_X,
                ENERGY_TEXTURE_Y + offset,
                BAR_WIDTH,
                filled,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderProgressArrow(GuiGraphics graphics, int originX, int originY) {
        int progress = menu.getProgress();
        int max = Math.max(menu.getMaxProgress(), 1);
        if (progress <= 0) {
            return;
        }
        int width = Math.min(PROGRESS_WIDTH, progress * PROGRESS_WIDTH / max);
        if (width <= 0) {
            return;
        }
        graphics.blit(GUI_TEXTURE,
                originX + PROGRESS_X,
                originY + PROGRESS_Y,
                176,
                0,
                width,
                PROGRESS_HEIGHT,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHoveringChemical(mouseX, mouseY)) {
            int amount = menu.getChemicalAmount();
            int capacity = Math.max(menu.getChemicalCapacity(), 1);
            Component chemicalName = menu.getStoredChemical() != null
                    ? menu.getStoredChemical().getDisplayName()
                    : Component.translatable("tooltip.chickens.avian_dousing_machine.empty");
            Component tooltip = Component.translatable("tooltip.chickens.avian_dousing_machine.chemical",
                    chemicalName, amount, capacity, AvianDousingMachineBlockEntity.CHEMICAL_COST,
                    AvianDousingMachineBlockEntity.CHEMICAL_ENERGY_COST);
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
        if (isHoveringLiquid(mouseX, mouseY)) {
            int amount = menu.getFluidAmount();
            int capacity = Math.max(menu.getFluidCapacity(), 1);
            FluidStack stack = menu.getFluid();
            Component fluidName = stack.isEmpty()
                    ? Component.translatable("tooltip.chickens.avian_dousing_machine.empty")
                    : stack.getHoverName();
            if (menu.getSpecialInfusion() != SpecialInfusion.NONE && menu.getSpecialAmount() > 0) {
                fluidName = Component.literal(menu.getSpecialInfusion().getDisplayName());
            }
            int useCost = menu.getSpecialInfusion() != SpecialInfusion.NONE
                    ? AvianDousingMachineBlockEntity.SPECIAL_LIQUID_CAPACITY
                    : menu.getLiquidCost();
            int energyCost = menu.getSpecialInfusion() != SpecialInfusion.NONE
                    ? AvianDousingMachineBlockEntity.SPECIAL_ENERGY_COST
                    : AvianDousingMachineBlockEntity.LIQUID_ENERGY_COST;
            Component tooltip = Component.translatable("tooltip.chickens.avian_dousing_machine.liquid",
                    fluidName, amount, capacity, useCost, energyCost);
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
        if (isHoveringEnergy(mouseX, mouseY)) {
            int energy = getDisplayedEnergy();
            int capacity = Math.max(getDisplayedCapacity(), 1);
            Component tooltip = Component.translatable("tooltip.chickens.avian_dousing_machine.energy", energy, capacity);
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
        if (isHoveringProgress(mouseX, mouseY)) {
            int progress = menu.getProgress();
            int max = Math.max(menu.getMaxProgress(), 1);
            Component tooltip = Component.translatable("tooltip.chickens.avian_dousing_machine.progress", progress * 100 / max);
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private boolean isHoveringChemical(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + CHEM_BAR_X && mouseX <= x + CHEM_BAR_X + BAR_WIDTH
                && mouseY >= y + CHEM_BAR_Y && mouseY <= y + CHEM_BAR_Y + BAR_HEIGHT;
    }

    private boolean isHoveringLiquid(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + LIQUID_BAR_X && mouseX <= x + LIQUID_BAR_X + BAR_WIDTH
                && mouseY >= y + LIQUID_BAR_Y && mouseY <= y + LIQUID_BAR_Y + BAR_HEIGHT;
    }

    private boolean isHoveringEnergy(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + BAR_WIDTH
                && mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + BAR_HEIGHT;
    }

    private boolean isHoveringProgress(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + PROGRESS_X && mouseX <= x + PROGRESS_X + PROGRESS_WIDTH
                && mouseY >= y + PROGRESS_Y && mouseY <= y + PROGRESS_Y + PROGRESS_HEIGHT;
    }

    private int colorForSpecial(AvianDousingMachineMenu menu) {
        SpecialInfusion infusion = menu.getSpecialInfusion();
        if (infusion == SpecialInfusion.NONE || menu.getSpecialAmount() <= 0) {
            return -1;
        }
        return switch (infusion) {
            case DRAGON_BREATH -> 0xA05CFF;
            case NETHER_STAR -> 0xE0E0E0;
            default -> -1;
        };
    }

    private int getDisplayedEnergy() {
        return menu.getEnergyStored();
    }

    private int getDisplayedCapacity() {
        return menu.getEnergyCapacity();
    }
}
