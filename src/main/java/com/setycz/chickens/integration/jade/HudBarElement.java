package com.setycz.chickens.integration.jade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/**
 * Minimal Jade element that mirrors the custom WTHIT bar renderer so the Jade
 * overlay presents the same fluid/chemical/energy bars.
 */
final class HudBarElement extends Element {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 13;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xFF1B1B1B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final Component label;
    private final TextureAtlasSprite sprite;
    private final int tint;
    private final float ratio;

    HudBarElement(Component label, TextureAtlasSprite sprite, int tint, float ratio) {
        this.label = label;
        this.sprite = sprite;
        this.tint = tint;
        this.ratio = Math.max(0.0F, Math.min(1.0F, ratio));
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
        int intX = (int) x;
        int intY = (int) y;
        graphics.fill(intX, intY, intX + WIDTH, intY + HEIGHT, BORDER_COLOR);
        graphics.fill(intX + 1, intY + 1, intX + WIDTH - 1, intY + HEIGHT - 1, BACKGROUND_COLOR);

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
                graphics.blit(intX + 1 + drawn, intY + 1, 0, segment, HEIGHT - 2, sprite);
                drawn += segment;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        renderScrollingText(graphics, intX, intY);
    }

    private void renderScrollingText(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        int areaWidth = WIDTH - 6;
        int baseX = x + 3;
        int textY = y + (HEIGHT - font.lineHeight) / 2;
        int textWidth = font.width(label);
        if (textWidth <= areaWidth) {
            graphics.drawString(font, label, baseX + (areaWidth - textWidth) / 2, textY, TEXT_COLOR, false);
            return;
        }
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
        graphics.drawString(font, label, drawX, textY, TEXT_COLOR, false);
        graphics.disableScissor();
    }
}
