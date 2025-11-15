package com.setycz.chickens.screen;

import com.setycz.chickens.menu.RoosterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the rooster inventory. It reuses a Chickens-themed GUI
 * background while exposing the seed slot and player inventory.
 */
public class RoosterScreen extends AbstractContainerScreen<RoosterMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/gui/rooster.png");

    // Seed bar geometry mirrors the legacy Hatchery rooster GUI: a 13px wide
    // column rising from the bottom of the gauge with a total height of 58px.
    private static final int SEED_BAR_X = 75;
    private static final int SEED_BAR_Y = 72;
    private static final int SEED_BAR_WIDTH = 13;
    private static final int SEED_BAR_HEIGHT = 58;
    private static final int SEED_BAR_U = 195;

    public RoosterScreen(RoosterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 174;
        this.imageHeight = 164;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int seeds = this.menu.getScaledSeeds(SEED_BAR_HEIGHT);
        if (seeds > 0) {
            // Draw from the bottom up so the bar fills vertically while
            // reusing the original texture coordinates from Hatchery.
            graphics.blit(GUI_TEXTURE,
                    x + SEED_BAR_X, y + SEED_BAR_Y - seeds,
                    SEED_BAR_U, SEED_BAR_HEIGHT - seeds,
                    SEED_BAR_WIDTH, seeds,
                    256, 256);
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
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

