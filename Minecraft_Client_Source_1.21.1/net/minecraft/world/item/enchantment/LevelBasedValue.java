package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;

public interface LevelBasedValue {
   Codec<LevelBasedValue> DISPATCH_CODEC = BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.byNameCodec().dispatch(LevelBasedValue::codec, (var0) -> {
      return var0;
   });
   Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValue.Constant.CODEC, DISPATCH_CODEC).xmap((var0) -> {
      return (LevelBasedValue)var0.map((var0x) -> {
         return var0x;
      }, (var0x) -> {
         return var0x;
      });
   }, (var0) -> {
      Either var10000;
      if (var0 instanceof LevelBasedValue.Constant) {
         LevelBasedValue.Constant var1 = (LevelBasedValue.Constant)var0;
         var10000 = Either.left(var1);
      } else {
         var10000 = Either.right(var0);
      }

      return var10000;
   });

   static MapCodec<? extends LevelBasedValue> bootstrap(Registry<MapCodec<? extends LevelBasedValue>> var0) {
      Registry.register(var0, (String)"clamped", LevelBasedValue.Clamped.CODEC);
      Registry.register(var0, (String)"fraction", LevelBasedValue.Fraction.CODEC);
      Registry.register(var0, (String)"levels_squared", LevelBasedValue.LevelsSquared.CODEC);
      Registry.register(var0, (String)"linear", LevelBasedValue.Linear.CODEC);
      return (MapCodec)Registry.register(var0, (String)"lookup", LevelBasedValue.Lookup.CODEC);
   }

   static LevelBasedValue.Constant constant(float var0) {
      return new LevelBasedValue.Constant(var0);
   }

   static LevelBasedValue.Linear perLevel(float var0, float var1) {
      return new LevelBasedValue.Linear(var0, var1);
   }

   static LevelBasedValue.Linear perLevel(float var0) {
      return perLevel(var0, var0);
   }

   static LevelBasedValue.Lookup lookup(List<Float> var0, LevelBasedValue var1) {
      return new LevelBasedValue.Lookup(var0, var1);
   }

   float calculate(int var1);

   MapCodec<? extends LevelBasedValue> codec();

   public static record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Clamped> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(LevelBasedValue.CODEC.fieldOf("value").forGetter(LevelBasedValue.Clamped::value), Codec.FLOAT.fieldOf("min").forGetter(LevelBasedValue.Clamped::min), Codec.FLOAT.fieldOf("max").forGetter(LevelBasedValue.Clamped::max)).apply(var0, LevelBasedValue.Clamped::new);
      }).validate((var0) -> {
         return var0.max <= var0.min ? DataResult.error(() -> {
            return "Max must be larger than min, min: " + var0.min + ", max: " + var0.max;
         }) : DataResult.success(var0);
      });

      public Clamped(LevelBasedValue param1, float param2, float param3) {
         super();
         this.value = var1;
         this.min = var2;
         this.max = var3;
      }

      public float calculate(int var1) {
         return Mth.clamp(this.value.calculate(var1), this.min, this.max);
      }

      public MapCodec<LevelBasedValue.Clamped> codec() {
         return CODEC;
      }

      public LevelBasedValue value() {
         return this.value;
      }

      public float min() {
         return this.min;
      }

      public float max() {
         return this.max;
      }
   }

   public static record Fraction(LevelBasedValue numerator, LevelBasedValue denominator) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Fraction> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(LevelBasedValue.CODEC.fieldOf("numerator").forGetter(LevelBasedValue.Fraction::numerator), LevelBasedValue.CODEC.fieldOf("denominator").forGetter(LevelBasedValue.Fraction::denominator)).apply(var0, LevelBasedValue.Fraction::new);
      });

      public Fraction(LevelBasedValue param1, LevelBasedValue param2) {
         super();
         this.numerator = var1;
         this.denominator = var2;
      }

      public float calculate(int var1) {
         float var2 = this.denominator.calculate(var1);
         return var2 == 0.0F ? 0.0F : this.numerator.calculate(var1) / var2;
      }

      public MapCodec<LevelBasedValue.Fraction> codec() {
         return CODEC;
      }

      public LevelBasedValue numerator() {
         return this.numerator;
      }

      public LevelBasedValue denominator() {
         return this.denominator;
      }
   }

   public static record LevelsSquared(float added) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.LevelsSquared> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.FLOAT.fieldOf("added").forGetter(LevelBasedValue.LevelsSquared::added)).apply(var0, LevelBasedValue.LevelsSquared::new);
      });

      public LevelsSquared(float param1) {
         super();
         this.added = var1;
      }

      public float calculate(int var1) {
         return (float)Mth.square(var1) + this.added;
      }

      public MapCodec<LevelBasedValue.LevelsSquared> codec() {
         return CODEC;
      }

      public float added() {
         return this.added;
      }
   }

   public static record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Linear> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.FLOAT.fieldOf("base").forGetter(LevelBasedValue.Linear::base), Codec.FLOAT.fieldOf("per_level_above_first").forGetter(LevelBasedValue.Linear::perLevelAboveFirst)).apply(var0, LevelBasedValue.Linear::new);
      });

      public Linear(float param1, float param2) {
         super();
         this.base = var1;
         this.perLevelAboveFirst = var2;
      }

      public float calculate(int var1) {
         return this.base + this.perLevelAboveFirst * (float)(var1 - 1);
      }

      public MapCodec<LevelBasedValue.Linear> codec() {
         return CODEC;
      }

      public float base() {
         return this.base;
      }

      public float perLevelAboveFirst() {
         return this.perLevelAboveFirst;
      }
   }

   public static record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Lookup> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.FLOAT.listOf().fieldOf("values").forGetter(LevelBasedValue.Lookup::values), LevelBasedValue.CODEC.fieldOf("fallback").forGetter(LevelBasedValue.Lookup::fallback)).apply(var0, LevelBasedValue.Lookup::new);
      });

      public Lookup(List<Float> param1, LevelBasedValue param2) {
         super();
         this.values = var1;
         this.fallback = var2;
      }

      public float calculate(int var1) {
         return var1 <= this.values.size() ? (Float)this.values.get(var1 - 1) : this.fallback.calculate(var1);
      }

      public MapCodec<LevelBasedValue.Lookup> codec() {
         return CODEC;
      }

      public List<Float> values() {
         return this.values;
      }

      public LevelBasedValue fallback() {
         return this.fallback;
      }
   }

   public static record Constant(float value) implements LevelBasedValue {
      public static final Codec<LevelBasedValue.Constant> CODEC;
      public static final MapCodec<LevelBasedValue.Constant> TYPED_CODEC;

      public Constant(float param1) {
         super();
         this.value = var1;
      }

      public float calculate(int var1) {
         return this.value;
      }

      public MapCodec<LevelBasedValue.Constant> codec() {
         return TYPED_CODEC;
      }

      public float value() {
         return this.value;
      }

      static {
         CODEC = Codec.FLOAT.xmap(LevelBasedValue.Constant::new, LevelBasedValue.Constant::value);
         TYPED_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
            return var0.group(Codec.FLOAT.fieldOf("value").forGetter(LevelBasedValue.Constant::value)).apply(var0, LevelBasedValue.Constant::new);
         });
      }
   }
}
