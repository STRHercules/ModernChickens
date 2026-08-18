package strhercules.chickens.screen;

import strhercules.chickens.menu.MechanicalNestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MechanicalNestScreen extends SideConfigurableScreen<MechanicalNestMenu> {
    private static final ResourceLocation GUI = new ResourceLocation("chickens",
            "textures/gui/mechanical_nest.png");
    private static final int BAR_X = 157;
    private static final int BAR_Y = 6;
    private static final int BAR_WIDTH = 11;
    private static final int BAR_HEIGHT = 55;

    public MechanicalNestScreen(MechanicalNestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 146;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(GUI, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        // The existing texture has a third upgrade outline from the removed
        // Range Upgrade slot; keep the player-facing layout free of a dead slot.
        graphics.fill(x + 134, y + 45, x + 154, y + 65, 0xFFC6C6C6);
        int capacity = Math.max(1, menu.getCapacity());
        int filled = Math.min(BAR_HEIGHT, menu.getEnergy() * BAR_HEIGHT / capacity);
        if (filled > 0) {
            int offset = BAR_HEIGHT - filled;
            graphics.blit(GUI, x + BAR_X, y + BAR_Y + offset, 178, offset,
                    BAR_WIDTH, filled, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderSideConfig(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + BAR_X && mouseX <= leftPos + BAR_X + BAR_WIDTH
                && mouseY >= topPos + BAR_Y && mouseY <= topPos + BAR_Y + BAR_HEIGHT) {
            graphics.renderTooltip(font, Component.translatable("tooltip.chickens.mechanical_nest.energy",
                    menu.getEnergy(), menu.getCapacity(), menu.getEnergyCost()), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
