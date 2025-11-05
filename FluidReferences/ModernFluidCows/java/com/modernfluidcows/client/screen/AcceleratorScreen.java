package com.modernfluidcows.client.screen;

import com.modernfluidcows.blockentity.AcceleratorBlockEntity;
import com.modernfluidcows.client.screen.util.FluidGuiRenderer;
import com.modernfluidcows.menu.AcceleratorMenu;
import com.modernfluidcows.util.FCUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen mirroring the accelerator GUI.
 */
public class AcceleratorScreen extends AbstractContainerScreen<AcceleratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/gui/gui_accelerator.png");
    private static final int FLUID_BAR_HEIGHT = 74;
    private static final int FLUID_BAR_WIDTH = 16;
    private static final int FLUID_BAR_OVERLAY_HEIGHT = 76;
    private static final int FLUID_BAR_OVERLAY_WIDTH = 18;
    private static final int FLUID_BAR_X = 7;
    private static final int FLUID_BAR_Y = 5;

    public AcceleratorScreen(final AcceleratorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int filled = menu.getScaledTankHeight(FLUID_BAR_HEIGHT);
        if (filled > 0) {
            menu.getDisplayedFluidStack()
                    .ifPresent(stack -> FluidGuiRenderer.blitFluid(
                            graphics,
                            stack,
                            leftPos + FLUID_BAR_X + 1,
                            topPos + FLUID_BAR_Y + 1,
                            FLUID_BAR_WIDTH,
                            filled));
        }
        graphics.blit(
                TEXTURE,
                leftPos + FLUID_BAR_X,
                topPos + FLUID_BAR_Y,
                176,
                0,
                FLUID_BAR_OVERLAY_WIDTH,
                FLUID_BAR_OVERLAY_HEIGHT,
                256,
                256);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(FLUID_BAR_X, FLUID_BAR_Y, FLUID_BAR_OVERLAY_WIDTH, FLUID_BAR_OVERLAY_HEIGHT, mouseX, mouseY)) {
            String fluidName = menu.getDisplayedFluidStack()
                    .map(stack -> FCUtils.getFluidName(stack.getFluid()))
                    .orElse("???");
            Component tooltip = Component.translatable(
                    "screen.fluidcows.accelerator.tank", fluidName, menu.getTankAmount(), AcceleratorBlockEntity.TANK_CAPACITY);
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (isHovering(145, 47, 16, 16, mouseX, mouseY)) {
            Component tooltip = Component.translatable(
                    "screen.fluidcows.accelerator.substance", menu.getSubstance(), menu.getMaxSubstance());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        String text = menu.getSubstance() + " / " + menu.getMaxSubstance();
        graphics.drawString(font, text, imageWidth - 8 - font.width(text), 12, 0x404040, false);
    }
}
