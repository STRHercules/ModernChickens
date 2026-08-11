package strhercules.chickens.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

/** Shared tank/interior rendering for the transparent avian machines. */
abstract class AvianMachineBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    protected static final float SURFACE_EPSILON = 1.0F / 256.0F;
    protected static final float TANK_MIN = SURFACE_EPSILON;
    protected static final float TANK_MAX = 1.0F - SURFACE_EPSILON;
    protected static final float TANK_BOTTOM = SURFACE_EPSILON;
    protected static final float TANK_TOP = 1.0F - SURFACE_EPSILON;
    private static final float INTERIOR_MIN = 0.0F;
    private static final float INTERIOR_MAX = 1.0F;
    private static final float INTERIOR_SIDE_MIN = SURFACE_EPSILON * 0.5F;
    private static final float INTERIOR_SIDE_MAX = 1.0F - SURFACE_EPSILON * 0.5F;
    private static final float INTERIOR_BOTTOM = SURFACE_EPSILON * 0.5F;
    private static final float INTERIOR_TOP = 1.0F - SURFACE_EPSILON * 0.5F;
    private static final float INTERIOR_BACK = INTERIOR_TOP;
    private static final float DIVIDER_EPSILON = 1.0F / 256.0F;
    private static final int CONTENT_ALPHA = 210;
    private static final int INTERIOR_COLOR = 0xFFFFFFFF;
    private static final ResourceLocation INTERIOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chickens", "block/machines/fluid_pump_interior");

    protected AvianMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public final void render(T machine, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        if (!state.hasProperty(BlockStateProperties.LIT)
                || !state.getValue(BlockStateProperties.LIT)) {
            return;
        }

        Level level = machine.getLevel();
        if (level == null) {
            return;
        }

        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(getModelRotation(
                state.getValue(HorizontalDirectionalBlock.FACING))));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        TextureAtlasSprite interior = atlas.getSprite(INTERIOR_TEXTURE);
        VertexConsumer wallConsumer = buffer.getBuffer(RenderType.solid());
        renderBackWall(wallConsumer, poseStack.last(), interior, packedLight, packedOverlay);
        renderHorizontalWall(wallConsumer, poseStack.last(), interior, INTERIOR_BOTTOM, packedLight, packedOverlay);
        renderHorizontalWall(wallConsumer, poseStack.last(), interior, INTERIOR_TOP, packedLight, packedOverlay);
        renderSideWall(wallConsumer, poseStack.last(), interior, INTERIOR_SIDE_MIN, packedLight, packedOverlay);
        renderSideWall(wallConsumer, poseStack.last(), interior, INTERIOR_SIDE_MAX, packedLight, packedOverlay);
        if (hasDivider()) {
            renderDivider(wallConsumer, poseStack.last(), interior, packedLight, packedOverlay);
        }

        VertexConsumer tankConsumer = buffer.getBuffer(RenderType.translucent());
        renderStoredContents(machine, level, atlas, tankConsumer, poseStack.last(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    protected boolean hasDivider() {
        return false;
    }

    protected abstract void renderStoredContents(T machine, Level level, TextureAtlas atlas,
            VertexConsumer consumer, PoseStack.Pose pose, int packedLight, int packedOverlay);

    protected static void renderTankContents(TextureAtlas atlas, VertexConsumer consumer, PoseStack.Pose pose,
            @Nullable TankContents contents, float x0, float x1, int packedLight, int packedOverlay) {
        if (contents == null || contents.amount() <= 0 || contents.capacity() <= 0) {
            return;
        }
        float fill = Mth.clamp(contents.amount() / (float) contents.capacity(), 0.0F, 1.0F);
        float top = Mth.lerp(fill, TANK_BOTTOM, TANK_TOP);
        TextureAtlasSprite sprite = atlas.getSprite(contents.texture());
        renderTank(consumer, pose, sprite, x0, TANK_BOTTOM, TANK_MIN,
                x1, top, TANK_MAX, contents.tint(), packedLight, packedOverlay);
    }

    @Nullable
    protected static TankContents resolveFluidContents(FluidStack fluid, int amount, int capacity,
            Level level, BlockEntity machine) {
        if (fluid.isEmpty() || amount <= 0) {
            return null;
        }
        FluidState state = fluid.getFluid().defaultFluidState();
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extensions.getStillTexture(state, level, machine.getBlockPos());
        if (texture == null) {
            texture = extensions.getFlowingTexture(state, level, machine.getBlockPos());
        }
        if (texture == null) {
            return null;
        }
        int tint = extensions.getTintColor(state, level, machine.getBlockPos());
        return new TankContents(texture, tint, amount, capacity);
    }

    private static float getModelRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void renderBackWall(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            int packedLight, int packedOverlay) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);
        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F,
                INTERIOR_MIN, INTERIOR_MIN, INTERIOR_BACK, u0, v1,
                INTERIOR_MIN, INTERIOR_MAX, INTERIOR_BACK, u0, v0,
                INTERIOR_MAX, INTERIOR_MAX, INTERIOR_BACK, u1, v0,
                INTERIOR_MAX, INTERIOR_MIN, INTERIOR_BACK, u1, v1);
        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F,
                INTERIOR_MIN, INTERIOR_MIN, INTERIOR_BACK, u1, v1,
                INTERIOR_MAX, INTERIOR_MIN, INTERIOR_BACK, u0, v1,
                INTERIOR_MAX, INTERIOR_MAX, INTERIOR_BACK, u0, v0,
                INTERIOR_MIN, INTERIOR_MAX, INTERIOR_BACK, u1, v0);
    }

    private static void renderHorizontalWall(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float y, int packedLight, int packedOverlay) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);

        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F,
                INTERIOR_MIN, y, INTERIOR_MIN, u0, v1,
                INTERIOR_MIN, y, INTERIOR_MAX, u0, v0,
                INTERIOR_MAX, y, INTERIOR_MAX, u1, v0,
                INTERIOR_MAX, y, INTERIOR_MIN, u1, v1);
        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 0.0F, -1.0F, 0.0F,
                INTERIOR_MIN, y, INTERIOR_MIN, u0, v0,
                INTERIOR_MAX, y, INTERIOR_MIN, u1, v0,
                INTERIOR_MAX, y, INTERIOR_MAX, u1, v1,
                INTERIOR_MIN, y, INTERIOR_MAX, u0, v1);
    }

    private static void renderSideWall(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x, int packedLight, int packedOverlay) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);

        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, -1.0F, 0.0F, 0.0F,
                x, INTERIOR_MIN, INTERIOR_BACK, u0, v1,
                x, INTERIOR_MAX, INTERIOR_BACK, u0, v0,
                x, INTERIOR_MAX, INTERIOR_MIN, u1, v0,
                x, INTERIOR_MIN, INTERIOR_MIN, u1, v1);
        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 1.0F, 0.0F, 0.0F,
                x, INTERIOR_MIN, INTERIOR_MIN, u0, v1,
                x, INTERIOR_MAX, INTERIOR_MIN, u0, v0,
                x, INTERIOR_MAX, INTERIOR_BACK, u1, v0,
                x, INTERIOR_MIN, INTERIOR_BACK, u1, v1);
    }

    private static void renderDivider(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            int packedLight, int packedOverlay) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);
        float west = 0.5F - DIVIDER_EPSILON;
        float east = 0.5F + DIVIDER_EPSILON;

        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, -1.0F, 0.0F, 0.0F,
                west, INTERIOR_MIN, INTERIOR_BACK, u0, v1,
                west, INTERIOR_MAX, INTERIOR_BACK, u0, v0,
                west, INTERIOR_MAX, INTERIOR_MIN, u1, v0,
                west, INTERIOR_MIN, INTERIOR_MIN, u1, v1);
        quad(consumer, pose, INTERIOR_COLOR, packedLight, packedOverlay, 1.0F, 0.0F, 0.0F,
                east, INTERIOR_MIN, INTERIOR_MIN, u0, v1,
                east, INTERIOR_MAX, INTERIOR_MIN, u0, v0,
                east, INTERIOR_MAX, INTERIOR_BACK, u1, v0,
                east, INTERIOR_MIN, INTERIOR_BACK, u1, v1);
    }

    private static void renderTank(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x0, float y0, float z0, float x1, float y1, float z1, int tint, int packedLight,
            int packedOverlay) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);
        int color = (CONTENT_ALPHA << 24) | (tint & 0xFFFFFF);

        quad(consumer, pose, color, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F,
                x0, y0, z0, u0, v1, x0, y1, z0, u0, v0, x1, y1, z0, u1, v0, x1, y0, z0, u1, v1);
        quad(consumer, pose, color, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F,
                x0, y0, z1, u1, v1, x1, y0, z1, u0, v1, x1, y1, z1, u0, v0, x0, y1, z1, u1, v0);
        quad(consumer, pose, color, packedLight, packedOverlay, -1.0F, 0.0F, 0.0F,
                x0, y0, z1, u0, v1, x0, y1, z1, u0, v0, x0, y1, z0, u1, v0, x0, y0, z0, u1, v1);
        quad(consumer, pose, color, packedLight, packedOverlay, 1.0F, 0.0F, 0.0F,
                x1, y0, z0, u0, v1, x1, y1, z0, u0, v0, x1, y1, z1, u1, v0, x1, y0, z1, u1, v1);
        quad(consumer, pose, color, packedLight, packedOverlay, 0.0F, -1.0F, 0.0F,
                x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1);
        quad(consumer, pose, color, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F,
                x0, y1, z0, u0, v1, x0, y1, z1, u0, v0, x1, y1, z1, u1, v0, x1, y1, z0, u1, v1);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int color, int packedLight,
            int packedOverlay, float normalX, float normalY, float normalZ,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3) {
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ,
                x0, y0, z0, u0, v0);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ,
                x1, y1, z1, u1, v1);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ,
                x2, y2, z2, u2, v2);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ,
                x3, y3, z3, u3, v3);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int color, int packedLight,
            int packedOverlay, float normalX, float normalY, float normalZ,
            float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    protected record TankContents(ResourceLocation texture, int tint, int amount, int capacity) {
    }
}
