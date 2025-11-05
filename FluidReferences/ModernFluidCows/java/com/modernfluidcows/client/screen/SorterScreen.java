package com.modernfluidcows.client.screen;

import com.modernfluidcows.menu.SorterMenu;
import com.modernfluidcows.util.FCUtils;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

/** Client screen that exposes the sorter configuration UI. */
public class SorterScreen extends AbstractContainerScreen<SorterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/gui/gui_sorter.png");

    public SorterScreen(final SorterMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 138;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.fluidcows.sorter.add"), button -> sendButton(SorterMenu.BUTTON_ADD_FLUID))
                .bounds(leftPos + 8, topPos + 24, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fluidcows.sorter.toggle"), button -> sendButton(SorterMenu.BUTTON_TOGGLE_BLACKLIST))
                .bounds(leftPos + 98, topPos + 24, 70, 20)
                .build());
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderFilterList(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        maybeRenderRemoveTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font,
                Component.translatable("screen.fluidcows.sorter.mode", modeComponent()),
                8,
                6,
                0x404040,
                false);
        graphics.drawString(font,
                Component.translatable("screen.fluidcows.sorter.range", menu.getRange()),
                8,
                16,
                0x404040,
                false);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            int startX = leftPos + 45;
            int startY = topPos + 6;
            List<ResourceLocation> filters = menu.getFilters();
            for (int index = 0; index < filters.size(); index++) {
                int x = startX;
                int y = startY + index * 10;
                if (mouseX >= x && mouseX <= x + 8 && mouseY >= y && mouseY <= y + 8) {
                    sendButton(SorterMenu.BUTTON_REMOVE_BASE + index);
                    return true;
                }
            }
        }
        return handled;
    }

    private void renderFilterList(final GuiGraphics graphics) {
        List<ResourceLocation> filters = menu.getFilters();
        int textX = leftPos + 52;
        int iconX = leftPos + 45;
        int y = topPos + 6;
        for (int index = 0; index < filters.size(); index++) {
            ResourceLocation key = filters.get(index);
            Component fluidName = describeFluid(key);
            graphics.blit(TEXTURE, iconX + 1, y, 176, 0, 5, 5, 256, 256);
            graphics.drawString(font, fluidName, textX, y, 0x404040, false);
            y += 10;
        }
    }

    private void maybeRenderRemoveTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        int startX = leftPos + 45;
        int startY = topPos + 6;
        List<ResourceLocation> filters = menu.getFilters();
        for (int index = 0; index < filters.size(); index++) {
            int x = startX;
            int y = startY + index * 10;
            if (mouseX >= x && mouseX <= x + 8 && mouseY >= y && mouseY <= y + 8) {
                graphics.renderTooltip(font,
                        Component.translatable("screen.fluidcows.sorter.remove", describeFluid(filters.get(index))),
                        mouseX,
                        mouseY);
                break;
            }
        }
    }

    private void sendButton(final int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component modeComponent() {
        return menu.isBlacklist()
                ? Component.translatable("screen.fluidcows.sorter.blacklist")
                : Component.translatable("screen.fluidcows.sorter.whitelist");
    }

    private Component describeFluid(final ResourceLocation key) {
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(null);
        if (fluid == null) {
            return Component.literal(key.toString());
        }
        return Component.literal(FCUtils.getFluidName(fluid));
    }
}
