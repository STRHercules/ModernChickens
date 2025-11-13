package net.minecraft.world.item;

public interface TooltipFlag {
   TooltipFlag.Default NORMAL = new TooltipFlag.Default(false, false);
   TooltipFlag.Default ADVANCED = new TooltipFlag.Default(true, false);

   boolean isAdvanced();

   boolean isCreative();

   public static record Default(boolean advanced, boolean creative) implements TooltipFlag {
      public Default(boolean param1, boolean param2) {
         super();
         this.advanced = var1;
         this.creative = var2;
      }

      public boolean isAdvanced() {
         return this.advanced;
      }

      public boolean isCreative() {
         return this.creative;
      }

      public TooltipFlag.Default asCreative() {
         return new TooltipFlag.Default(this.advanced, true);
      }

      public boolean advanced() {
         return this.advanced;
      }

      public boolean creative() {
         return this.creative;
      }
   }
}
