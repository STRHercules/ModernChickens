package net.minecraft.client.gui.navigation;

public record ScreenPosition(int x, int y) {
   public ScreenPosition(int param1, int param2) {
      super();
      this.x = var1;
      this.y = var2;
   }

   public static ScreenPosition of(ScreenAxis var0, int var1, int var2) {
      ScreenPosition var10000;
      switch(var0) {
      case HORIZONTAL:
         var10000 = new ScreenPosition(var1, var2);
         break;
      case VERTICAL:
         var10000 = new ScreenPosition(var2, var1);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ScreenPosition step(ScreenDirection var1) {
      ScreenPosition var10000;
      switch(var1) {
      case DOWN:
         var10000 = new ScreenPosition(this.x, this.y + 1);
         break;
      case UP:
         var10000 = new ScreenPosition(this.x, this.y - 1);
         break;
      case LEFT:
         var10000 = new ScreenPosition(this.x - 1, this.y);
         break;
      case RIGHT:
         var10000 = new ScreenPosition(this.x + 1, this.y);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int getCoordinate(ScreenAxis var1) {
      int var10000;
      switch(var1) {
      case HORIZONTAL:
         var10000 = this.x;
         break;
      case VERTICAL:
         var10000 = this.y;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }
}
