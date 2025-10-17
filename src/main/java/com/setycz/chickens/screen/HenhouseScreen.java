package com.setycz.chickens.screen;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.blockentity.HenhouseBlockEntity;
import com.setycz.chickens.menu.HenhouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the henhouse menu. It reuses the legacy texture layout
 * and progress bar math so players see the same UI they expect from the original
 * mod while interacting with the modern container.
 */
public class HenhouseScreen extends AbstractContainerScreen<HenhouseMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
            "textures/gui/henhouse.png");

    public HenhouseScreen(HenhouseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int energy = this.menu.getEnergy();
        final int barHeight = 57;
        int offset = barHeight - Math.min(barHeight,
                energy * barHeight / Math.max(HenhouseBlockEntity.HAY_BALE_ENERGY, 1));
        if (offset < barHeight) {
            graphics.blit(GUI_TEXTURE, x + 75, y + 14 + offset, 195, offset, 12, barHeight - offset);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
