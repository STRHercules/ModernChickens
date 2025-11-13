package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public record MapDecorations(Map<String, MapDecorations.Entry> decorations) {
   public static final MapDecorations EMPTY = new MapDecorations(Map.of());
   public static final Codec<MapDecorations> CODEC;

   public MapDecorations(Map<String, MapDecorations.Entry> param1) {
      super();
      this.decorations = var1;
   }

   public MapDecorations withDecoration(String var1, MapDecorations.Entry var2) {
      return new MapDecorations(Util.copyAndPut(this.decorations, var1, var2));
   }

   public Map<String, MapDecorations.Entry> decorations() {
      return this.decorations;
   }

   static {
      CODEC = Codec.unboundedMap(Codec.STRING, MapDecorations.Entry.CODEC).xmap(MapDecorations::new, MapDecorations::decorations);
   }

   public static record Entry(Holder<MapDecorationType> type, double x, double z, float rotation) {
      public static final Codec<MapDecorations.Entry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(MapDecorationType.CODEC.fieldOf("type").forGetter(MapDecorations.Entry::type), Codec.DOUBLE.fieldOf("x").forGetter(MapDecorations.Entry::x), Codec.DOUBLE.fieldOf("z").forGetter(MapDecorations.Entry::z), Codec.FLOAT.fieldOf("rotation").forGetter(MapDecorations.Entry::rotation)).apply(var0, MapDecorations.Entry::new);
      });

      public Entry(Holder<MapDecorationType> param1, double param2, double param4, float param6) {
         super();
         this.type = var1;
         this.x = var2;
         this.z = var4;
         this.rotation = var6;
      }

      public Holder<MapDecorationType> type() {
         return this.type;
      }

      public double x() {
         return this.x;
      }

      public double z() {
         return this.z;
      }

      public float rotation() {
         return this.rotation;
      }
   }
}
