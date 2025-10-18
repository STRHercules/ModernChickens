package com.setycz.chickens.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.client.render.ChickenRenderHelper;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickenStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Custom item renderer that mirrors the original Roost inventory icons by
 * drawing a tiny Chickens entity directly in the item slot. Using a
 * {@link BlockEntityWithoutLevelRenderer} keeps the rendering self contained
 * while letting NeoForge reuse the baked entity model and animations.
 */
public class ChickenItemRenderer extends BlockEntityWithoutLevelRenderer {
    public ChickenItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ChickensRegistryItem description = ChickenItemHelper.resolve(stack);
        if (description == null) {
            return;
        }
        ChickenStats stats = ChickenItemHelper.getStats(stack);
        ChickensChicken chicken = ChickenRenderHelper.getChicken(description.getId(), stats);
        if (chicken == null) {
            return;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.15D, 0.5D);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        float rotation = context == ItemDisplayContext.GUI ? 150.0F : 0.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        dispatcher.render(chicken, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
