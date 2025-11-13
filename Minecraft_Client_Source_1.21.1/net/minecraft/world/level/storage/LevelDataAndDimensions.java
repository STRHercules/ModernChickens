package net.minecraft.world.level.storage;

import net.minecraft.world.level.levelgen.WorldDimensions;

public record LevelDataAndDimensions(WorldData worldData, WorldDimensions.Complete dimensions) {
   public LevelDataAndDimensions(WorldData param1, WorldDimensions.Complete param2) {
      super();
      this.worldData = var1;
      this.dimensions = var2;
   }

   public WorldData worldData() {
      return this.worldData;
   }

   public WorldDimensions.Complete dimensions() {
      return this.dimensions;
   }
}
