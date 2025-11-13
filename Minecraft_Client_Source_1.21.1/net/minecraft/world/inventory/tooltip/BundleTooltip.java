package net.minecraft.world.inventory.tooltip;

import net.minecraft.world.item.component.BundleContents;

public record BundleTooltip(BundleContents contents) implements TooltipComponent {
   public BundleTooltip(BundleContents param1) {
      super();
      this.contents = var1;
   }

   public BundleContents contents() {
      return this.contents;
   }
}
