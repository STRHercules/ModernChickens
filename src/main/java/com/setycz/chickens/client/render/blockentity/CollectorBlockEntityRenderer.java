package com.setycz.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.setycz.chickens.blockentity.CollectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a handful of the collector's stored items as a lazy orbit above the
 * crate. The original Roost mod baked different item meshes into the block
 * model; rendering real item stacks here recreates that presentation while
 * keeping the logic data driven.
 */
public class CollectorBlockEntityRenderer implements BlockEntityRenderer<CollectorBlockEntity> {
    private static final int MAX_DISPLAY_ITEMS = 4;
    private static final float ORBIT_RADIUS = 0.25F;
    private final ItemRenderer itemRenderer;

    public CollectorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // Pull the shared item renderer from the context so we reuse the same
        // batching and model cache as vanilla containers.
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CollectorBlockEntity collector, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        List<ItemStack> displayStacks = collectDisplayStacks(collector);
        if (displayStacks.isEmpty()) {
            return;
        }

        float time = getAnimationTime(partialTicks);
        int total = displayStacks.size();
        for (int index = 0; index < total; index++) {
            ItemStack stack = displayStacks.get(index);
            poseStack.pushPose();

            float angle = time + (360.0F / total) * index;
            double radians = Math.toRadians(angle);
            double x = ORBIT_RADIUS * Mth.cos((float) radians);
            double z = ORBIT_RADIUS * Mth.sin((float) radians);
            double bob = Mth.sin((time + index * 20.0F) * 0.05F) * 0.05F;

            // Anchor items slightly above the crate so the orbit reads clearly in-game.
            poseStack.translate(0.5D + x, 0.7D + bob, 0.5D + z);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.scale(0.35F, 0.35F, 0.35F);

            Level level = collector.getLevel();
            if (level == null) {
                level = Minecraft.getInstance().level;
            }
            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffer,
                    level, 0);
            poseStack.popPose();
        }
    }

    private static List<ItemStack> collectDisplayStacks(CollectorBlockEntity collector) {
        List<ItemStack> result = new ArrayList<>(MAX_DISPLAY_ITEMS);
        for (ItemStack stack : collector.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean duplicate = result.stream().anyMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack));
            if (duplicate) {
                continue;
            }
            // Render a single item so the orbit stays readable regardless of the real stack size.
            ItemStack copy = stack.copy();
            copy.setCount(1);
            result.add(copy);
            if (result.size() >= MAX_DISPLAY_ITEMS) {
                break;
            }
        }
        return result;
    }

    private static float getAnimationTime(float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return partialTicks;
        }
        // Convert world time into degrees so each item has a smooth circular path.
        return (minecraft.level.getGameTime() + partialTicks) * 6.0F;
    }
}

