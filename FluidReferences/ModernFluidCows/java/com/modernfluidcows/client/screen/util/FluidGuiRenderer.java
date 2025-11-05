package com.modernfluidcows.client.screen.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Shared helpers for drawing tinted fluid stacks inside GUI tanks.
 */
public final class FluidGuiRenderer {
    private FluidGuiRenderer() {}

    /**
     * Renders the supplied fluid stack into a rectangular region, filling upwards from the bottom.
     *
     * <p>This mirrors the legacy GUI behaviour: the sprite is tiled across the bar, tinted using
     * the fluid's colour, and rendered with blending enabled so translucent fluids remain see
     * through. Callers are expected to draw their tank frame afterwards.</p>
     */
    public static void blitFluid(
            final GuiGraphics graphics, final FluidStack stack, final int x, final int y, final int width, final int height) {
        if (stack == null || stack.isEmpty() || width <= 0 || height <= 0) {
            return;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(extensions.getStillTexture(stack));
        if (sprite == null) {
            return;
        }

        int tint = extensions.getTintColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        int spriteWidth = sprite.contents().width();
        int spriteHeight = sprite.contents().height();

        for (int xOffset = 0; xOffset < width; xOffset += spriteWidth) {
            int drawnWidth = Math.min(spriteWidth, width - xOffset);
            for (int yOffset = 0; yOffset < height; yOffset += spriteHeight) {
                int drawnHeight = Math.min(spriteHeight, height - yOffset);
                int drawX = x + xOffset;
                int drawY = y + height - yOffset - drawnHeight;

                graphics.blit(drawX, drawY, 0, drawnWidth, drawnHeight, sprite, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
