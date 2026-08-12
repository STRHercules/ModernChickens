package strhercules.chickens.screen;

import strhercules.chickens.menu.CollectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CollectorScreen extends AbstractContainerScreen<CollectorMenu> {
    private static final ResourceLocation NONE_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/gui/collector_none.png");
    private static final ResourceLocation ONE_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/gui/collector_one.png");
    private static final ResourceLocation TWO_TEXTURE = ResourceLocation.fromNamespaceAndPath("chickens",
            "textures/gui/collector_two.png");

    public CollectorScreen(CollectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 252;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(textureForCapacity(), x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private ResourceLocation textureForCapacity() {
        return switch (this.menu.getCapacityLevel()) {
            case 1 -> ONE_TEXTURE;
            case 2 -> TWO_TEXTURE;
            default -> NONE_TEXTURE;
        };
    }
}
