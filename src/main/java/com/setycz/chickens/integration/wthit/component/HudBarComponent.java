package com.setycz.chickens.integration.wthit.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import mcp.mobius.waila.api.ITooltipComponent;
import net.minecraft.client.DeltaTracker;

/**
 * Small custom bar renderer that mirrors Mekanism's HUD elements. The bar renders a textured fill,
 * a dark frame, and scrolls long labels so they remain legible just like the Mek HUD.
 */
public final class HudBarComponent implements ITooltipComponent {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 13;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xFF1B1B1B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final Component text;
    private final TextureAtlasSprite sprite;
    private final int tint;
    private final float ratio;

    public HudBarComponent(Component text, TextureAtlasSprite sprite, int tint, float ratio) {
        this.text = text;
        this.sprite = sprite;
        this.tint = tint;
        this.ratio = Mth.clamp(ratio, 0.0F, 1.0F);
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, DeltaTracker delta) {
        // Border
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER_COLOR);
        // Background
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, BACKGROUND_COLOR);
        // Textured fill
        if (sprite != null && ratio > 0.0F) {
            int fillWidth = (int) ((WIDTH - 2) * ratio);
            RenderSystem.enableBlend();
            float red = ((tint >> 16) & 0xFF) / 255.0F;
            float green = ((tint >> 8) & 0xFF) / 255.0F;
            float blue = (tint & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, 1.0F);
            int drawn = 0;
            while (drawn < fillWidth) {
                int segment = Math.min(sprite.contents().width(), fillWidth - drawn);
                graphics.blit(x + 1 + drawn, y + 1, 0, segment, HEIGHT - 2, sprite);
                drawn += segment;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        renderScrollingText(graphics, x, y);
    }

    private void renderScrollingText(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int areaWidth = WIDTH - 6;
        int baseX = x + 3;
        int textY = y + (HEIGHT - font.lineHeight) / 2;
        int textWidth = font.width(text);
        if (textWidth <= areaWidth) {
            graphics.drawString(font, text, baseX + (areaWidth - textWidth) / 2, textY, TEXT_COLOR, false);
            return;
        }
        // Scroll the text back and forth similar to Mekanism's HUD
        long elapsed = Util.getMillis();
        int travel = textWidth - areaWidth;
        int period = travel * 2 + areaWidth;
        int offset = (int) (elapsed / 20 % period);
        if (offset > travel + areaWidth) {
            offset = period - offset;
        } else if (offset > travel) {
            offset = travel;
        }
        int drawX = baseX - offset;
        graphics.enableScissor(baseX, y + 1, baseX + areaWidth, y + HEIGHT - 1);
        graphics.drawString(font, text, drawX, textY, TEXT_COLOR, false);
        graphics.disableScissor();
    }
}
