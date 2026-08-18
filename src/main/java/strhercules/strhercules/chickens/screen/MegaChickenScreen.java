package strhercules.chickens.screen;

import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.menu.MegaChickenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla horse-style GUI for the mega chicken's saddle, chests, and cargo. */
public final class MegaChickenScreen extends AbstractContainerScreen<MegaChickenMenu> {
    private static final ResourceLocation FULL_CHEST_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/megachicken.png");
    private static final ResourceLocation ONE_CHEST_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/megachicken_onechest.png");
    private static final ResourceLocation NO_CHEST_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/megachicken_nochest.png");
    private static final ResourceLocation SADDLE_OUTLINE_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/saddle_outline.png");
    private static final ResourceLocation CHEST_OUTLINE_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/chest_outline.png");
    private static final ResourceLocation FLYING_EGG_OUTLINE_TEXTURE = new ResourceLocation(
            "chickens", "textures/gui/flyingegg_outline.png");

    private final MegaChicken chicken;
    private float xMouse;
    private float yMouse;

    public MegaChickenScreen(MegaChickenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.chicken = menu.getChicken();
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        boolean rightChest = this.chicken.hasRightChest();
        boolean leftChest = this.chicken.hasLeftChest();
        ResourceLocation texture = rightChest && leftChest
                ? FULL_CHEST_TEXTURE
                : rightChest
                ? ONE_CHEST_TEXTURE
                : NO_CHEST_TEXTURE;
        graphics.blit(texture, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        if (leftChest && !rightChest) {
            graphics.blit(ONE_CHEST_TEXTURE, x, y + 62, 0.0F, 116.0F,
                    this.imageWidth, 54, 256, 256);
        }
        if (!this.chicken.isSaddled()) {
            graphics.blit(SADDLE_OUTLINE_TEXTURE, x + 65, y + 10, 16, 16,
                    0.0F, 0.0F, 16, 16, 16, 16);
        }
        if (!this.chicken.hasFlyingEgg()) {
            graphics.blit(FLYING_EGG_OUTLINE_TEXTURE, x + 86, y + 10, 16, 16,
                    0.0F, 0.0F, 32, 32, 32, 32);
        }
        if (!rightChest) {
            graphics.blit(CHEST_OUTLINE_TEXTURE, x + 86, y + 40, 16, 16,
                    0.0F, 0.0F, 32, 32, 32, 32);
        }
        if (!leftChest) {
            graphics.blit(CHEST_OUTLINE_TEXTURE, x + 65, y + 40, 16, 16,
                    0.0F, 0.0F, 32, 32, 32, 32);
        }
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 34, y + 59, 17,
                (float) (x + 34) - this.xMouse, (float) (y + 9) - this.yMouse, this.chicken);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.xMouse = mouseX;
        this.yMouse = mouseY;
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
