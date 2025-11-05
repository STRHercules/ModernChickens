package com.modernfluidcows.client.screen;

import com.modernfluidcows.client.screen.util.FluidGuiRenderer;
import com.modernfluidcows.menu.StallMenu;
import com.modernfluidcows.util.FCUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen that renders the stall inventory and tank information.
 */
public class StallScreen extends AbstractContainerScreen<StallMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/gui/gui_stall.png");
    private static final int FLUID_BAR_HEIGHT = 74;
    private static final int FLUID_BAR_WIDTH = 16;
    private static final int FLUID_BAR_OVERLAY_HEIGHT = 76;
    private static final int FLUID_BAR_OVERLAY_WIDTH = 18;
    private static final int FLUID_BAR_X_OFFSET = 7;
    private static final int FLUID_BAR_Y_OFFSET = 5;

    public StallScreen(final StallMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int filled = menu.getScaledFluidHeight(FLUID_BAR_HEIGHT);
        if (filled > 0) {
            menu.getDisplayedFluidStack()
                    .ifPresent(stack -> FluidGuiRenderer.blitFluid(
                            graphics,
                            stack,
                            leftPos + FLUID_BAR_X_OFFSET + 1,
                            topPos + FLUID_BAR_Y_OFFSET + 1,
                            FLUID_BAR_WIDTH,
                            filled));
        }
        graphics.blit(
                TEXTURE,
                leftPos + FLUID_BAR_X_OFFSET,
                topPos + FLUID_BAR_Y_OFFSET,
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

        if (isHovering(
                FLUID_BAR_X_OFFSET, FLUID_BAR_Y_OFFSET, FLUID_BAR_OVERLAY_WIDTH, FLUID_BAR_OVERLAY_HEIGHT, mouseX, mouseY)) {
            String fluidName = menu.getDisplayedFluidStack()
                    .map(stack -> FCUtils.getFluidName(stack.getFluid()))
                    .orElse("???");
            int amount = menu.getTankAmount();
            Component tooltip = Component.translatable(
                    "screen.fluidcows.stall.tank", fluidName, amount, menu.getCapacity());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component cooldown = Component.translatable(
                "screen.fluidcows.stall.cooldown", FCUtils.toTime(menu.getCooldownTicks() / 20, "00:00"));
        graphics.drawString(font, cooldown, 8, 6, 0x404040, false);
    }
}
