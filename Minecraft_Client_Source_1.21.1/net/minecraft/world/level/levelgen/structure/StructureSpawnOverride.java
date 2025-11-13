package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;

public record StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType boundingBox, WeightedRandomList<MobSpawnSettings.SpawnerData> spawns) {
   public static final Codec<StructureSpawnOverride> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(StructureSpawnOverride.BoundingBoxType.CODEC.fieldOf("bounding_box").forGetter(StructureSpawnOverride::boundingBox), WeightedRandomList.codec(MobSpawnSettings.SpawnerData.CODEC).fieldOf("spawns").forGetter(StructureSpawnOverride::spawns)).apply(var0, StructureSpawnOverride::new);
   });

   public StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType param1, WeightedRandomList<MobSpawnSettings.SpawnerData> param2) {
      super();
      this.boundingBox = var1;
      this.spawns = var2;
   }

   public StructureSpawnOverride.BoundingBoxType boundingBox() {
      return this.boundingBox;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> spawns() {
      return this.spawns;
   }

   public static enum BoundingBoxType implements StringRepresentable {
      PIECE("piece"),
      STRUCTURE("full");

      public static final Codec<StructureSpawnOverride.BoundingBoxType> CODEC = StringRepresentable.fromEnum(StructureSpawnOverride.BoundingBoxType::values);
      private final String id;

      private BoundingBoxType(final String param3) {
         this.id = var3;
      }

      public String getSerializedName() {
         return this.id;
      }

      // $FF: synthetic method
      private static StructureSpawnOverride.BoundingBoxType[] $values() {
         return new StructureSpawnOverride.BoundingBoxType[]{PIECE, STRUCTURE};
      }
   }
}
