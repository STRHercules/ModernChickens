package com.setycz.chickens.screen;

import com.setycz.chickens.blockentity.BreederBlockEntity;
import com.setycz.chickens.menu.BreederMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the breeder container. Restores the legacy Roost layout by
 * drawing the ported texture and heart progress bar from the original mod.
 */
public class BreederScreen extends AbstractContainerScreen<BreederMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/gui/breeder.png");
    private static final int PROGRESS_X = 84;
    private static final int PROGRESS_Y = 22;
    private static final int PROGRESS_HEIGHT = 12;
    private static final int PROGRESS_WIDTH = 25;
    private static final int PROGRESS_OVERLAY_U = 176;

    public BreederScreen(BreederMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int width = getProgressWidth();
        if (width > 0) {
            graphics.blit(GUI_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_OVERLAY_U, 0, width, PROGRESS_HEIGHT, 256,
                    256);
        }
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
        renderProgressTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderProgressTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringProgress(mouseX, mouseY)) {
            return;
        }
        BreederBlockEntity breeder = this.menu.getBreeder();
        int progress = this.menu.getProgress();
        int percent = Math.min(100, progress / 10);
        if (breeder.getItem(BreederBlockEntity.SEED_SLOT).isEmpty()) {
            graphics.renderTooltip(this.font, Component.translatable("container.chickens.breeder.no_seeds"), mouseX, mouseY);
        } else {
            graphics.renderTooltip(this.font, Component.translatable("tooltip.chickens.container.progress", percent), mouseX,
                    mouseY);
        }
    }

    private boolean isHoveringProgress(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        return mouseX > x + PROGRESS_X && mouseX < x + PROGRESS_X + PROGRESS_WIDTH
                && mouseY > y + PROGRESS_Y && mouseY < y + PROGRESS_Y + PROGRESS_HEIGHT;
    }

    private int getProgressWidth() {
        int progress = this.menu.getProgress();
        if (progress <= 0) {
            return 0;
        }
        int width = 1 + (progress * PROGRESS_WIDTH) / 1000;
        return Math.min(width, PROGRESS_WIDTH);
    }
}
