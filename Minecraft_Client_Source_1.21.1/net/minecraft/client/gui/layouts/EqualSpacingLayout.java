package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;

public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children;
   private final LayoutSettings defaultChildLayoutSettings;

   public EqualSpacingLayout(int var1, int var2, EqualSpacingLayout.Orientation var3) {
      this(0, 0, var1, var2, var3);
   }

   public EqualSpacingLayout(int var1, int var2, int var3, int var4, EqualSpacingLayout.Orientation var5) {
      super(var1, var2, var3, var4);
      this.children = new ArrayList();
      this.defaultChildLayoutSettings = LayoutSettings.defaults();
      this.orientation = var5;
   }

   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int var1 = 0;
         int var2 = this.orientation.getSecondaryLength((LayoutElement)this);

         EqualSpacingLayout.ChildContainer var4;
         for(Iterator var3 = this.children.iterator(); var3.hasNext(); var2 = Math.max(var2, this.orientation.getSecondaryLength(var4))) {
            var4 = (EqualSpacingLayout.ChildContainer)var3.next();
            var1 += this.orientation.getPrimaryLength(var4);
         }

         int var10 = this.orientation.getPrimaryLength((LayoutElement)this) - var1;
         int var11 = this.orientation.getPrimaryPosition(this);
         Iterator var5 = this.children.iterator();
         EqualSpacingLayout.ChildContainer var6 = (EqualSpacingLayout.ChildContainer)var5.next();
         this.orientation.setPrimaryPosition(var6, var11);
         var11 += this.orientation.getPrimaryLength(var6);
         EqualSpacingLayout.ChildContainer var8;
         if (this.children.size() >= 2) {
            for(Divisor var7 = new Divisor(var10, this.children.size() - 1); var7.hasNext(); var11 += this.orientation.getPrimaryLength(var8)) {
               var11 += var7.nextInt();
               var8 = (EqualSpacingLayout.ChildContainer)var5.next();
               this.orientation.setPrimaryPosition(var8, var11);
            }
         }

         int var12 = this.orientation.getSecondaryPosition(this);
         Iterator var13 = this.children.iterator();

         while(var13.hasNext()) {
            EqualSpacingLayout.ChildContainer var9 = (EqualSpacingLayout.ChildContainer)var13.next();
            this.orientation.setSecondaryPosition(var9, var12, var2);
         }

         switch(this.orientation.ordinal()) {
         case 0:
            this.height = var2;
            break;
         case 1:
            this.width = var2;
         }

      }
   }

   public void visitChildren(Consumer<LayoutElement> var1) {
      this.children.forEach((var1x) -> {
         var1.accept(var1x.child);
      });
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T var1) {
      return this.addChild(var1, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T var1, LayoutSettings var2) {
      this.children.add(new EqualSpacingLayout.ChildContainer(var1, var2));
      return var1;
   }

   public <T extends LayoutElement> T addChild(T var1, Consumer<LayoutSettings> var2) {
      return this.addChild(var1, (LayoutSettings)Util.make(this.newChildLayoutSettings(), var2));
   }

   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      private Orientation() {
      }

      int getPrimaryLength(LayoutElement var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getWidth();
            break;
         case 1:
            var10000 = var1.getHeight();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getPrimaryLength(EqualSpacingLayout.ChildContainer var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getWidth();
            break;
         case 1:
            var10000 = var1.getHeight();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryLength(LayoutElement var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getHeight();
            break;
         case 1:
            var10000 = var1.getWidth();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryLength(EqualSpacingLayout.ChildContainer var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getHeight();
            break;
         case 1:
            var10000 = var1.getWidth();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      void setPrimaryPosition(EqualSpacingLayout.ChildContainer var1, int var2) {
         switch(this.ordinal()) {
         case 0:
            var1.setX(var2, var1.getWidth());
            break;
         case 1:
            var1.setY(var2, var1.getHeight());
         }

      }

      void setSecondaryPosition(EqualSpacingLayout.ChildContainer var1, int var2, int var3) {
         switch(this.ordinal()) {
         case 0:
            var1.setY(var2, var3);
            break;
         case 1:
            var1.setX(var2, var3);
         }

      }

      int getPrimaryPosition(LayoutElement var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getX();
            break;
         case 1:
            var10000 = var1.getY();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryPosition(LayoutElement var1) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = var1.getY();
            break;
         case 1:
            var10000 = var1.getX();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      // $FF: synthetic method
      private static EqualSpacingLayout.Orientation[] $values() {
         return new EqualSpacingLayout.Orientation[]{HORIZONTAL, VERTICAL};
      }
   }

   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement var1, LayoutSettings var2) {
         super(var1, var2);
      }
   }
}
