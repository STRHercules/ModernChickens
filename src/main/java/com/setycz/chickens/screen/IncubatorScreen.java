package com.setycz.chickens.screen;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.menu.IncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Client-side screen for the Incubator. Renders the bespoke incubator.png layout
 * while overlaying a progress arrow and RF bar synced from the container data.
 */
public class IncubatorScreen extends AbstractContainerScreen<IncubatorMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/incubator.png");
    private static final int PROGRESS_X = 68;
    private static final int PROGRESS_Y = 35;
    private static final int PROGRESS_WIDTH = 25;
    private static final int PROGRESS_HEIGHT = 14;
    private static final int PROGRESS_TEXTURE_X = 176;
    private static final int PROGRESS_TEXTURE_Y = 0;
    private static final int ENERGY_BAR_X = 153;
    private static final int ENERGY_BAR_Y = 14;
    private static final int ENERGY_BAR_WIDTH = 13;
    private static final int ENERGY_BAR_HEIGHT = 57;
    private static final int ENERGY_TEXTURE_X = 202;
    private static final int ENERGY_TEXTURE_Y = 0;

    public IncubatorScreen(IncubatorMenu menu, Inventory playerInventory, Component title) {
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
        renderProgressArrow(graphics, x, y);
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
        renderProgressTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderProgressArrow(GuiGraphics graphics, int originX, int originY) {
        int maxProgress = Math.max(1, this.menu.getMaxProgress());
        int filled = Math.min(PROGRESS_WIDTH, this.menu.getProgress() * PROGRESS_WIDTH / maxProgress);
        if (filled <= 0) {
            return;
        }
        graphics.blit(GUI_TEXTURE,
                originX + PROGRESS_X,
                originY + PROGRESS_Y,
                PROGRESS_TEXTURE_X,
                PROGRESS_TEXTURE_Y,
                filled,
                PROGRESS_HEIGHT,
                256,
                256);
    }

    private void renderEnergyBar(GuiGraphics graphics, int originX, int originY) {
        int energy = this.menu.getEnergy();
        int capacity = Math.max(this.menu.getCapacity(), 1);
        int offset = ENERGY_BAR_HEIGHT - Math.min(ENERGY_BAR_HEIGHT, energy * ENERGY_BAR_HEIGHT / capacity);
        if (offset >= ENERGY_BAR_HEIGHT) {
            return;
        }
        graphics.blit(GUI_TEXTURE,
                originX + ENERGY_BAR_X,
                originY + ENERGY_BAR_Y + offset,
                ENERGY_TEXTURE_X,
                ENERGY_TEXTURE_Y + offset,
                ENERGY_BAR_WIDTH,
                ENERGY_BAR_HEIGHT - offset,
                256,
                256);
    }

    private void renderEnergyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringEnergy(mouseX, mouseY)) {
            return;
        }
        int energy = this.menu.getEnergy();
        int capacity = Math.max(this.menu.getCapacity(), 1);
        graphics.renderTooltip(this.font,
                Component.translatable("tooltip.chickens.incubator.energy", energy, capacity),
                mouseX, mouseY);
    }

    private void renderProgressTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringProgress(mouseX, mouseY)) {
            return;
        }
        int max = Math.max(1, this.menu.getMaxProgress());
        int percent = Math.min(100, this.menu.getProgress() * 100 / max);
        Component progress = Component.translatable("tooltip.chickens.incubator.progress", percent);
        Component cost = Component.translatable("tooltip.chickens.incubator.cost", this.menu.getEnergyCost());
        graphics.renderTooltip(this.font,
                List.of(progress.getVisualOrderText(), cost.getVisualOrderText()),
                mouseX, mouseY);
    }

    private boolean isHoveringEnergy(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2 + ENERGY_BAR_X;
        int y = (this.height - this.imageHeight) / 2 + ENERGY_BAR_Y;
        return mouseX >= x && mouseX <= x + ENERGY_BAR_WIDTH
                && mouseY >= y && mouseY <= y + ENERGY_BAR_HEIGHT;
    }

    private boolean isHoveringProgress(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2 + PROGRESS_X;
        int y = (this.height - this.imageHeight) / 2 + PROGRESS_Y;
        return mouseX >= x && mouseX <= x + PROGRESS_WIDTH
                && mouseY >= y && mouseY <= y + PROGRESS_HEIGHT;
    }
}
