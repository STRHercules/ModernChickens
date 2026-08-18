package strhercules.chickens.screen;

import strhercules.chickens.blockentity.MachineSideConfig;
import strhercules.chickens.menu.SideConfigMenu;
import strhercules.chickens.network.SideConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared Mekanism-style expandable I/O tab for the mod's machine screens. */
public abstract class SideConfigurableScreen<M extends AbstractContainerMenu & SideConfigMenu>
        extends AbstractContainerScreen<M> {
    private MachineSideConfig.Channel selectedChannel = MachineSideConfig.Channel.ITEMS;
    private boolean sideConfigOpen;

    protected SideConfigurableScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    protected final void renderSideConfig(GuiGraphics graphics, int mouseX, int mouseY) {
        SideConfigPanel.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                this.width, this.menu, this.sideConfigOpen, this.selectedChannel, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (button == 0 && SideConfigPanel.isTabHovered(this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                this.width, x, y)) {
            this.sideConfigOpen = !this.sideConfigOpen;
            return true;
        }
        if (button == 0 && this.sideConfigOpen) {
            int channel = SideConfigPanel.channelAt(this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                    this.width, x, y);
            if (channel >= 0) {
                this.selectedChannel = MachineSideConfig.Channel.values()[channel];
                return true;
            }
            Direction direction = SideConfigPanel.directionAt(this.leftPos, this.topPos, this.imageWidth,
                    this.imageHeight, this.width, x, y);
            if (direction != null) {
                SideConfigPayload.send(this.menu.getSideConfigPos(), direction, this.selectedChannel);
                return true;
            }
            if (SideConfigPanel.isResetHovered(this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                    this.width, x, y)) {
                SideConfigPayload.sendReset(this.menu.getSideConfigPos());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static final class SideConfigPanel {
        private static final int PANEL_WIDTH = 156;
        private static final int PANEL_HEIGHT = 135;
        private static final int TAB_WIDTH = 26;
        private static final int TAB_HEIGHT = 18;
        private static final int BUTTON_SIZE = 22;
        private static final int[] DIRECTION_X = { 67, 67, 67, 44, 44, 90 };
        private static final int[] DIRECTION_Y = { 92, 46, 69, 92, 69, 69 };
        private static final Direction[] DIRECTIONS = {
                Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };
        private static final String[] CHANNEL_LABELS = { "Items", "Fluids", "Chemicals", "Energy" };

        private static final ResourceLocation BASE_TEXTURE = texture("base");
        private static final ResourceLocation SHADOW_TEXTURE = texture("shadow");
        private static final ResourceLocation INNER_SCREEN_TEXTURE = texture("inner_screen");
        private static final ResourceLocation CONFIGURATION_TEXTURE = texture("configuration");
        private static final ResourceLocation BUTTON_TEXTURE = texture("button");
        private static final ResourceLocation HOLDER_LEFT_TEXTURE = texture("holder_left");
        private static final ResourceLocation HOLDER_RIGHT_TEXTURE = texture("holder_right");
        private static final ResourceLocation CLEAR_SIDES_TEXTURE = texture("button/clear_sides");
        private static final ResourceLocation AUTO_EJECT_TEXTURE = texture("button/auto_eject");
        private static final ResourceLocation[] CHANNEL_TEXTURES = {
                texture("items"), texture("fluids"), texture("chemicals"), texture("energy")
        };

        private static ResourceLocation texture(String name) {
            return new ResourceLocation("chickens", "textures/gui/io/" + name + ".png");
        }

        private static void render(GuiGraphics graphics, Font font, int leftPos, int topPos, int imageWidth,
                int imageHeight, int screenWidth, SideConfigMenu menu, boolean open,
                MachineSideConfig.Channel channel, int mouseX, int mouseY) {
            int tabX = tabX(leftPos, imageWidth, screenWidth);
            int tabY = topPos + 6;
            boolean tabHovered = isHovered(tabX, tabY, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY);

            if (!open) {
                drawTab(graphics, tabX, tabY, leftPos, tabHovered, false, CONFIGURATION_TEXTURE);
                if (tabHovered) {
                    graphics.renderTooltip(font, Component.literal("Configure machine I/O"), mouseX, mouseY);
                }
                return;
            }

            int panelX = panelX(leftPos, imageWidth, screenWidth);
            int panelY = topPos + 4;
            drawNineSlice(graphics, SHADOW_TEXTURE, panelX - 3, panelY - 3, PANEL_WIDTH + 6, PANEL_HEIGHT + 6,
                    4, 4, 0, 0, 256, 256, 256, 256, 0xBFFFFFFF);
            drawNineSlice(graphics, BASE_TEXTURE, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                    4, 4, 0, 0, 256, 256, 256, 256, 0xFFFFFFFF);

            graphics.drawString(font, Component.literal(channelLabel(channel) + " Config"), panelX + 5, panelY + 5,
                    0x404040, false);
            drawNineSlice(graphics, INNER_SCREEN_TEXTURE, panelX + 38, panelY + 25, 80, 12,
                    4, 4, 0, 0, 256, 256, 256, 256, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.literal(channelLabel(channel)), panelX + 78, panelY + 27,
                    0x00FF88);

            boolean tabsOnLeft = panelX < leftPos;
            int channelTabX = channelTabX(panelX, leftPos, screenWidth);
            for (int i = 0; i < CHANNEL_TEXTURES.length; i++) {
                int channelTabY = panelY + 2 + 28 * i;
                boolean hovered = isHovered(channelTabX, channelTabY, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY);
                int tabColor = i == channel.ordinal() ? 0xFFD0D0D0 : 0xFFB0B0B0;
                if (hovered) {
                    tabColor = 0xFFE0E0E0;
                }
                drawHolderTab(graphics, channelTabX, channelTabY, tabsOnLeft, tabColor, CHANNEL_TEXTURES[i]);
            }

            MachineSideConfig config = menu.getSideConfigurable().sideConfig();
            for (int i = 0; i < DIRECTIONS.length; i++) {
                Direction direction = DIRECTIONS[i];
                int x = panelX + DIRECTION_X[i];
                int y = panelY + DIRECTION_Y[i];
                MachineSideConfig.Mode mode = config.get(direction, channel);
                boolean hovered = isHovered(x, y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);
                drawButton(graphics, x, y, BUTTON_SIZE, BUTTON_SIZE, hovered, modeColor(mode), true);
            }

            int autoX = panelX + 136;
            int autoY = panelY + 6;
            drawButton(graphics, autoX, autoY, 14, 14, false, 0xFF858585, false);
            graphics.blit(AUTO_EJECT_TEXTURE, autoX, autoY, 0, 0, 14, 14, 14, 14);

            int resetX = panelX + 136;
            int resetY = panelY + 95;
            boolean resetHovered = isHovered(resetX, resetY, 14, 14, mouseX, mouseY);
            drawButton(graphics, resetX, resetY, 14, 14, resetHovered, 0xFF858585, true);
            graphics.blit(CLEAR_SIDES_TEXTURE, resetX, resetY, 0, 0, 14, 14, 14, 14);

            // Keep the close/configuration tab above the panel when the screen is narrow.
            drawTab(graphics, tabX, tabY, leftPos, tabHovered, true, CONFIGURATION_TEXTURE);

            Component tooltip = tooltipAt(panelX, panelY, leftPos, screenWidth, config, channel, mouseX, mouseY);
            if (tooltip != null) {
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        private static Component tooltipAt(int panelX, int panelY, int leftPos, int screenWidth,
                MachineSideConfig config, MachineSideConfig.Channel channel, int mouseX, int mouseY) {
            int tabX = channelTabX(panelX, leftPos, screenWidth);
            for (int i = 0; i < CHANNEL_TEXTURES.length; i++) {
                if (isHovered(tabX, panelY + 2 + 28 * i, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY)) {
                    return Component.literal(channelLabel(MachineSideConfig.Channel.values()[i])
                            + " side configuration");
                }
            }
            for (int i = 0; i < DIRECTIONS.length; i++) {
                int x = panelX + DIRECTION_X[i];
                int y = panelY + DIRECTION_Y[i];
                if (isHovered(x, y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
                    Direction direction = DIRECTIONS[i];
                    return Component.literal(capitalize(direction.getName()) + ": "
                            + modeText(config.get(direction, channel)));
                }
            }
            int resetX = panelX + 136;
            int resetY = panelY + 95;
            return isHovered(resetX, resetY, 14, 14, mouseX, mouseY)
                    ? Component.literal("Reset every side to Input/Output") : null;
        }

        private static int channelAt(int leftPos, int topPos, int imageWidth, int imageHeight, int screenWidth,
                int mouseX, int mouseY) {
            int panelX = panelX(leftPos, imageWidth, screenWidth);
            int panelY = topPos + 4;
            int tabX = channelTabX(panelX, leftPos, screenWidth);
            for (int i = 0; i < CHANNEL_TEXTURES.length; i++) {
                if (isHovered(tabX, panelY + 2 + 28 * i, TAB_WIDTH, TAB_HEIGHT, mouseX, mouseY)) {
                    return i;
                }
            }
            return -1;
        }

        private static Direction directionAt(int leftPos, int topPos, int imageWidth, int imageHeight,
                int screenWidth, int mouseX, int mouseY) {
            int panelX = panelX(leftPos, imageWidth, screenWidth);
            int panelY = topPos + 4;
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if (isHovered(panelX + DIRECTION_X[i], panelY + DIRECTION_Y[i], BUTTON_SIZE, BUTTON_SIZE, mouseX,
                        mouseY)) {
                    return DIRECTIONS[i];
                }
            }
            return null;
        }

        private static boolean isResetHovered(int leftPos, int topPos, int imageWidth, int imageHeight,
                int screenWidth, int mouseX, int mouseY) {
            int panelX = panelX(leftPos, imageWidth, screenWidth);
            int panelY = topPos + 4;
            return isHovered(panelX + 136, panelY + 95, 14, 14, mouseX, mouseY);
        }

        private static boolean isTabHovered(int leftPos, int topPos, int imageWidth, int imageHeight,
                int screenWidth, int mouseX, int mouseY) {
            return isHovered(tabX(leftPos, imageWidth, screenWidth), topPos + 6, TAB_WIDTH, TAB_HEIGHT, mouseX,
                    mouseY);
        }

        private static int panelX(int leftPos, int imageWidth, int screenWidth) {
            int preferred = leftPos >= PANEL_WIDTH + 4 ? leftPos - PANEL_WIDTH - 4 : leftPos + imageWidth + 4;
            return Math.max(2, Math.min(preferred, screenWidth - PANEL_WIDTH - 2));
        }

        private static int channelTabX(int panelX, int leftPos, int screenWidth) {
            int preferred = panelX < leftPos ? panelX - TAB_WIDTH : panelX + PANEL_WIDTH;
            return Math.max(2, Math.min(preferred, screenWidth - TAB_WIDTH - 2));
        }

        private static int tabX(int leftPos, int imageWidth, int screenWidth) {
            int preferred = leftPos >= PANEL_WIDTH + 4 ? leftPos - TAB_WIDTH : leftPos + imageWidth;
            return Math.max(2, Math.min(preferred, screenWidth - TAB_WIDTH - 2));
        }

        private static void drawTab(GuiGraphics graphics, int x, int y, int leftPos, boolean hovered, boolean open,
                ResourceLocation icon) {
            drawHolderTab(graphics, x, y, x < leftPos, hovered || open ? 0xFFD0D0D0 : 0xFFB0B0B0, icon);
        }

        private static void drawHolderTab(GuiGraphics graphics, int x, int y, boolean left, int color,
                ResourceLocation icon) {
            ResourceLocation holder = left ? HOLDER_LEFT_TEXTURE : HOLDER_RIGHT_TEXTURE;
            drawTintedNineSlice(graphics, holder, x, y, TAB_WIDTH, TAB_HEIGHT, 4, 4,
                    0, 0, 26, 9, 26, 9, color);
            graphics.blit(icon, x + 4, y, 0, 0, 18, 18, 18, 18);
        }

        private static void drawButton(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered,
                int color, boolean active) {
            int state = !active ? 0 : hovered ? 2 : 1;
            drawTintedNineSlice(graphics, BUTTON_TEXTURE, x, y, width, height, 20, 4,
                    0, state * 20, 200, 20, 200, 60, color);
        }

        private static void drawTintedNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                int width, int height, int sliceWidth, int sliceHeight, int uOffset, int vOffset, int sourceWidth,
                int sourceHeight, int textureWidth, int textureHeight, int color) {
            graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
            blitNineSlice(graphics, texture, x, y, width, height, sliceWidth, sliceHeight, uOffset, vOffset,
                    sourceWidth, sourceHeight, textureWidth, textureHeight);
            graphics.setColor(1, 1, 1, 1);
        }

        private static void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width,
                int height, int sliceWidth, int sliceHeight, int uOffset, int vOffset, int sourceWidth,
                int sourceHeight, int textureWidth, int textureHeight, int color) {
            drawTintedNineSlice(graphics, texture, x, y, width, height, sliceWidth, sliceHeight, uOffset, vOffset,
                    sourceWidth, sourceHeight, textureWidth, textureHeight, color);
        }

        private static void blitNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width,
                int height, int sliceWidth, int sliceHeight, int uOffset, int vOffset, int sourceWidth,
                int sourceHeight, int textureWidth, int textureHeight) {
            int left = Math.min(sliceWidth, width / 2);
            int right = Math.min(sliceWidth, width - left);
            int top = Math.min(sliceHeight, height / 2);
            int bottom = Math.min(sliceHeight, height - top);
            int centerWidth = width - left - right;
            int centerHeight = height - top - bottom;
            int sourceCenterWidth = sourceWidth - sliceWidth * 2;
            int sourceCenterHeight = sourceHeight - sliceHeight * 2;

            blitPart(graphics, texture, x, y, left, top, uOffset, vOffset, left, top, textureWidth, textureHeight);
            blitPart(graphics, texture, x + left, y, centerWidth, top, uOffset + sliceWidth, vOffset,
                    sourceCenterWidth, sliceHeight, textureWidth, textureHeight);
            blitPart(graphics, texture, x + width - right, y, right, top, uOffset + sourceWidth - sliceWidth,
                    vOffset, right, top, textureWidth, textureHeight);
            blitPart(graphics, texture, x, y + top, left, centerHeight, uOffset, vOffset + sliceHeight, sliceWidth,
                    sourceCenterHeight, textureWidth, textureHeight);
            blitPart(graphics, texture, x + left, y + top, centerWidth, centerHeight, uOffset + sliceWidth,
                    vOffset + sliceHeight, sourceCenterWidth, sourceCenterHeight, textureWidth, textureHeight);
            blitPart(graphics, texture, x + width - right, y + top, right, centerHeight,
                    uOffset + sourceWidth - sliceWidth, vOffset + sliceHeight, right, sourceCenterHeight,
                    textureWidth, textureHeight);
            blitPart(graphics, texture, x, y + height - bottom, left, bottom, uOffset, vOffset + sourceHeight - sliceHeight,
                    left, bottom, textureWidth, textureHeight);
            blitPart(graphics, texture, x + left, y + height - bottom, centerWidth, bottom,
                    uOffset + sliceWidth, vOffset + sourceHeight - sliceHeight, sourceCenterWidth, bottom,
                    textureWidth, textureHeight);
            blitPart(graphics, texture, x + width - right, y + height - bottom, right, bottom,
                    uOffset + sourceWidth - sliceWidth, vOffset + sourceHeight - sliceHeight, right, bottom,
                    textureWidth, textureHeight);
        }

        private static void blitPart(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width,
                int height, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
            if (width > 0 && height > 0 && sourceWidth > 0 && sourceHeight > 0) {
                graphics.blit(texture, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight,
                        textureWidth, textureHeight);
            }
        }

        private static boolean isHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        private static int modeColor(MachineSideConfig.Mode mode) {
            return switch (mode) {
                case NONE -> 0xFF777777;
                case INPUT -> 0xFF6D87BD;
                case OUTPUT -> 0xFFBC805F;
                case BOTH -> 0xFF6EAA7F;
            };
        }

        private static String modeText(MachineSideConfig.Mode mode) {
            return switch (mode) {
                case NONE -> "Disabled";
                case INPUT -> "Input";
                case OUTPUT -> "Output";
                case BOTH -> "Input/Output";
            };
        }

        private static String channelLabel(MachineSideConfig.Channel channel) {
            return CHANNEL_LABELS[channel.ordinal()];
        }

        private static String capitalize(String value) {
            return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
    }
}
