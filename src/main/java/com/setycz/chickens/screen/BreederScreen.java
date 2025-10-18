package com.setycz.chickens.screen;

import com.setycz.chickens.menu.BreederMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the breeder container. Reuses the shulker box texture as a
 * temporary backdrop until bespoke art is ported.
 */
public class BreederScreen extends AbstractContainerScreen<BreederMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/shulker_box.png");

    public BreederScreen(BreederMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
        int progress = this.menu.getProgress();
        String text = Component.translatable("container.chickens.breeder_progress", progress / 10).getString();
        graphics.drawString(this.font, text, 8, 20, 0x404040, false);
        if (!this.menu.getBreeder().hasRequiredSeeds()) {
            graphics.drawString(this.font, Component.translatable("container.chickens.breeder.no_seeds"), 8, 32, 0xAA0000,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
