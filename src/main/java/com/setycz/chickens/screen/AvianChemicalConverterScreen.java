package com.setycz.chickens.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.menu.AvianChemicalConverterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the chemical converter. Reuses the flux converter GUI and
 * tints the tank fill based on the stored chemical egg colour.
 */
public class AvianChemicalConverterScreen extends AbstractContainerScreen<AvianChemicalConverterMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/fluxconverter.png");
    private static final int TANK_X = 103;
    private static final int TANK_Y = 14;
    private static final int TANK_WIDTH = 13;
    private static final int TANK_HEIGHT = 58;
    private static final int TANK_TEXTURE_X = 195;
    private static final int TANK_TEXTURE_Y = 0;

    public AvianChemicalConverterScreen(AvianChemicalConverterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        renderTank(graphics, x, y);
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
        renderTankTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderTank(GuiGraphics graphics, int originX, int originY) {
        int amount = this.menu.getChemicalAmount();
        int capacity = Math.max(this.menu.getCapacity(), 1);
        if (amount <= 0) {
            return;
        }
        int filled = Math.min(TANK_HEIGHT, amount * TANK_HEIGHT / capacity);
        if (filled <= 0) {
            return;
        }
        int offset = TANK_HEIGHT - filled;
        int color = 0xFFFFFF;
        ChemicalEggRegistryItem entry = menu.getStoredEntry();
        if (entry != null) {
            color = entry.getEggColor();
        }
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        graphics.blit(GUI_TEXTURE, originX + TANK_X, originY + TANK_Y + offset, TANK_TEXTURE_X,
                TANK_TEXTURE_Y + offset, TANK_WIDTH, filled, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderTankTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringTank(mouseX, mouseY)) {
            return;
        }
        ChemicalEggRegistryItem entry = menu.getStoredEntry();
        Component name = entry != null ? entry.getDisplayName()
                : Component.translatable("tooltip.chickens.avian_chemical_converter.empty");
        int amount = this.menu.getChemicalAmount();
        int capacity = Math.max(this.menu.getCapacity(), 1);
        Component tooltip = amount <= 0
                ? Component.translatable("tooltip.chickens.avian_chemical_converter.empty")
                : Component.translatable("tooltip.chickens.avian_chemical_converter.level", name, amount, capacity);
        graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private boolean isHoveringTank(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + TANK_X && mouseX <= x + TANK_X + TANK_WIDTH
                && mouseY >= y + TANK_Y && mouseY <= y + TANK_Y + TANK_HEIGHT;
    }
}
