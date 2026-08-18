package strhercules.chickens.screen;

import strhercules.chickens.menu.MechanicalRoostMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Client screen for the four-row RF-powered roost. */
public class MechanicalRoostScreen extends SideConfigurableScreen<MechanicalRoostMenu> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("chickens",
            "textures/gui/mechanical_roost.png");
    private static final int[] ROW_Y = { 20, 40, 60, 80 };
    private static final int PROGRESS_X = 48;
    private static final int PROGRESS_WIDTH = 25;
    private static final int PROGRESS_HEIGHT = 16;
    private static final int PROGRESS_TEXTURE_X = 176;
    private static final int PROGRESS_TEXTURE_Y = 0;
    private static final int ENERGY_BAR_X = 155;
    private static final int ENERGY_BAR_Y = 28;
    private static final int ENERGY_BAR_WIDTH = 13;
    private static final int ENERGY_BAR_HEIGHT = 58;
    private static final int ENERGY_TEXTURE_X = 208;
    private static final int ENERGY_TEXTURE_Y = 0;

    public MechanicalRoostScreen(MechanicalRoostMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        renderProgress(graphics, x, y);
        renderEnergyBar(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderSideConfig(graphics, mouseX, mouseY);
        renderEnergyTooltip(graphics, mouseX, mouseY);
        renderProgressTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderProgress(GuiGraphics graphics, int originX, int originY) {
        for (int row = 0; row < ROW_Y.length; row++) {
            int progress = this.menu.getProgress(row);
            if (progress <= 0) {
                continue;
            }
            int filled = Math.min(PROGRESS_WIDTH, 1 + progress * PROGRESS_WIDTH / 1000);
            int rowY = ROW_Y[row];
            graphics.blit(GUI_TEXTURE, originX + PROGRESS_X, originY + rowY,
                    PROGRESS_TEXTURE_X, PROGRESS_TEXTURE_Y, filled, PROGRESS_HEIGHT, 256, 256);
        }
    }

    private void renderEnergyBar(GuiGraphics graphics, int originX, int originY) {
        int energy = this.menu.getEnergy();
        int capacity = Math.max(this.menu.getCapacity(), 1);
        int offset = ENERGY_BAR_HEIGHT - Math.min(ENERGY_BAR_HEIGHT, energy * ENERGY_BAR_HEIGHT / capacity);
        if (offset < ENERGY_BAR_HEIGHT) {
            graphics.blit(GUI_TEXTURE, originX + ENERGY_BAR_X, originY + ENERGY_BAR_Y + offset,
                    ENERGY_TEXTURE_X, ENERGY_TEXTURE_Y + offset, ENERGY_BAR_WIDTH,
                    ENERGY_BAR_HEIGHT - offset, 256, 256);
        }
    }

    private void renderEnergyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHoveringEnergy(mouseX, mouseY)) {
            return;
        }
        graphics.renderTooltip(this.font,
                Component.translatable("tooltip.chickens.mechanical_roost.energy",
                        this.menu.getEnergy(), this.menu.getCapacity(), this.menu.getEnergyCostPerOperation()),
                mouseX, mouseY);
    }

    private void renderProgressTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int row = getHoveredProgressRow(mouseX, mouseY);
        if (row < 0) {
            return;
        }
        int progress = this.menu.getProgress(row);
        if (progress <= 0) {
            return;
        }
        int percent = Math.min(100, progress / 10);
        graphics.renderTooltip(this.font,
                Component.translatable("tooltip.chickens.mechanical_roost.progress", percent), mouseX, mouseY);
    }

    private boolean isHoveringEnergy(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2 + ENERGY_BAR_X;
        int y = (this.height - this.imageHeight) / 2 + ENERGY_BAR_Y;
        return mouseX >= x && mouseX <= x + ENERGY_BAR_WIDTH
                && mouseY >= y && mouseY <= y + ENERGY_BAR_HEIGHT;
    }

    private int getHoveredProgressRow(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2 + PROGRESS_X;
        int y = (this.height - this.imageHeight) / 2;
        for (int row = 0; row < ROW_Y.length; row++) {
            int rowY = ROW_Y[row];
            if (mouseX >= x && mouseX <= x + PROGRESS_WIDTH
                    && mouseY >= y + rowY && mouseY <= y + rowY + PROGRESS_HEIGHT) {
                return row;
            }
        }
        return -1;
    }
}
