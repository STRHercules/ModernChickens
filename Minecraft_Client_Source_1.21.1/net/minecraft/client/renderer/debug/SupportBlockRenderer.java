package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = 4.9E-324D;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft var1) {
      super();
      this.minecraft = var1;
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7) {
      double var9 = (double)Util.getNanos();
      if (var9 - this.lastUpdateTime > 1.0E8D) {
         this.lastUpdateTime = var9;
         Entity var11 = this.minecraft.gameRenderer.getMainCamera().getEntity();
         this.surroundEntities = ImmutableList.copyOf(var11.level().getEntities(var11, var11.getBoundingBox().inflate(16.0D)));
      }

      LocalPlayer var14 = this.minecraft.player;
      if (var14 != null && var14.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(var1, var2, var3, var5, var7, var14, () -> {
            return 0.0D;
         }, 1.0F, 0.0F, 0.0F);
      }

      Iterator var12 = this.surroundEntities.iterator();

      while(var12.hasNext()) {
         Entity var13 = (Entity)var12.next();
         if (var13 != var14) {
            this.drawHighlights(var1, var2, var3, var5, var7, var13, () -> {
               return this.getBias(var13);
            }, 0.0F, 1.0F, 0.0F);
         }
      }

   }

   private void drawHighlights(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7, Entity var9, DoubleSupplier var10, float var11, float var12, float var13) {
      var9.mainSupportingBlockPos.ifPresent((var14) -> {
         double var15 = var10.getAsDouble();
         BlockPos var17 = var9.getOnPos();
         this.highlightPosition(var17, var1, var3, var5, var7, var2, 0.02D + var15, var11, var12, var13);
         BlockPos var18 = var9.getOnPosLegacy();
         if (!var18.equals(var17)) {
            this.highlightPosition(var18, var1, var3, var5, var7, var2, 0.04D + var15, 0.0F, 1.0F, 1.0F);
         }

      });
   }

   private double getBias(Entity var1) {
      return 0.02D * (double)(String.valueOf((double)var1.getId() + 0.132453657D).hashCode() % 1000) / 1000.0D;
   }

   private void highlightPosition(BlockPos var1, PoseStack var2, double var3, double var5, double var7, MultiBufferSource var9, double var10, float var12, float var13, float var14) {
      double var15 = (double)var1.getX() - var3 - 2.0D * var10;
      double var17 = (double)var1.getY() - var5 - 2.0D * var10;
      double var19 = (double)var1.getZ() - var7 - 2.0D * var10;
      double var21 = var15 + 1.0D + 4.0D * var10;
      double var23 = var17 + 1.0D + 4.0D * var10;
      double var25 = var19 + 1.0D + 4.0D * var10;
      LevelRenderer.renderLineBox(var2, var9.getBuffer(RenderType.lines()), var15, var17, var19, var21, var23, var25, var12, var13, var14, 0.4F);
      LevelRenderer.renderVoxelShape(var2, var9.getBuffer(RenderType.lines()), this.minecraft.level.getBlockState(var1).getCollisionShape(this.minecraft.level, var1, CollisionContext.empty()).move((double)var1.getX(), (double)var1.getY(), (double)var1.getZ()), -var3, -var5, -var7, var12, var13, var14, 1.0F, false);
   }
}
