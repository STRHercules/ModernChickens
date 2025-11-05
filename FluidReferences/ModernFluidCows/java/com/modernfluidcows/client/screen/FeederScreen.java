package com.modernfluidcows.client.screen;

import com.modernfluidcows.menu.FeederMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Client screen that mirrors the feeder's legacy slot layout. */
public class FeederScreen extends AbstractContainerScreen<FeederMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/gui/gui_feeder.png");

    public FeederScreen(final FeederMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 133;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component range = Component.translatable("screen.fluidcows.feeder.range", menu.getRange());
        graphics.drawString(font, range, 8, 6, 0x404040, false);
    }
}
