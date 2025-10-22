package com.setycz.chickens.screen;

import com.setycz.chickens.menu.AvianFluxConverterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen that repurposes the vanilla furnace background while rendering
 * a vertical energy gauge. The tooltip surfaces the precise RF totals so
 * players can monitor charge levels without opening external probes.
 */
public class AvianFluxConverterScreen extends AbstractContainerScreen<AvianFluxConverterMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 14;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 46;

    public AvianFluxConverterScreen(AvianFluxConverterMenu menu, Inventory playerInventory, Component title) {
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
        renderEnergyBar(graphics, x, y);
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
        renderEnergyTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderEnergyBar(GuiGraphics graphics, int originX, int originY) {
        int energy = this.menu.getEnergy();
        int capacity = this.menu.getCapacity();
        int filledHeight = capacity <= 0 ? 0 : Mth.ceil((energy / (float) capacity) * ENERGY_BAR_HEIGHT);
        int x = originX + ENERGY_BAR_X;
        int y = originY + ENERGY_BAR_Y;
        graphics.fill(x, y, x + ENERGY_BAR_WIDTH, y + ENERGY_BAR_HEIGHT, 0xFF202020);
        if (filledHeight > 0) {
            graphics.fill(x + 1, y + ENERGY_BAR_HEIGHT - filledHeight + 1, x + ENERGY_BAR_WIDTH - 1,
                    y + ENERGY_BAR_HEIGHT - 1, 0xFFE53935);
        }
    }

    private void renderEnergyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringEnergy(mouseX, mouseY)) {
            return;
        }
        int energy = this.menu.getEnergy();
        int capacity = this.menu.getCapacity();
        graphics.renderTooltip(this.font, Component.translatable("tooltip.chickens.avian_flux_converter.energy", energy, capacity),
                mouseX, mouseY);
    }

    private boolean isHoveringEnergy(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH
                && mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT;
    }
}
