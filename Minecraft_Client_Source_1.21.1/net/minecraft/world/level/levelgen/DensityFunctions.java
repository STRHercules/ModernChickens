package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;

public final class DensityFunctions {
   private static final Codec<DensityFunction> CODEC;
   protected static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0D;
   static final Codec<Double> NOISE_VALUE_CODEC;
   public static final Codec<DensityFunction> DIRECT_CODEC;

   public static MapCodec<? extends DensityFunction> bootstrap(Registry<MapCodec<? extends DensityFunction>> var0) {
      register(var0, "blend_alpha", DensityFunctions.BlendAlpha.CODEC);
      register(var0, "blend_offset", DensityFunctions.BlendOffset.CODEC);
      register(var0, "beardifier", DensityFunctions.BeardifierMarker.CODEC);
      register(var0, "old_blended_noise", BlendedNoise.CODEC);
      DensityFunctions.Marker.Type[] var1 = DensityFunctions.Marker.Type.values();
      int var2 = var1.length;

      int var3;
      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.Marker.Type var4 = var1[var3];
         register(var0, var4.getSerializedName(), var4.codec);
      }

      register(var0, "noise", DensityFunctions.Noise.CODEC);
      register(var0, "end_islands", DensityFunctions.EndIslandDensityFunction.CODEC);
      register(var0, "weird_scaled_sampler", DensityFunctions.WeirdScaledSampler.CODEC);
      register(var0, "shifted_noise", DensityFunctions.ShiftedNoise.CODEC);
      register(var0, "range_choice", DensityFunctions.RangeChoice.CODEC);
      register(var0, "shift_a", DensityFunctions.ShiftA.CODEC);
      register(var0, "shift_b", DensityFunctions.ShiftB.CODEC);
      register(var0, "shift", DensityFunctions.Shift.CODEC);
      register(var0, "blend_density", DensityFunctions.BlendDensity.CODEC);
      register(var0, "clamp", DensityFunctions.Clamp.CODEC);
      DensityFunctions.Mapped.Type[] var5 = DensityFunctions.Mapped.Type.values();
      var2 = var5.length;

      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.Mapped.Type var7 = var5[var3];
         register(var0, var7.getSerializedName(), var7.codec);
      }

      DensityFunctions.TwoArgumentSimpleFunction.Type[] var6 = DensityFunctions.TwoArgumentSimpleFunction.Type.values();
      var2 = var6.length;

      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.TwoArgumentSimpleFunction.Type var8 = var6[var3];
         register(var0, var8.getSerializedName(), var8.codec);
      }

      register(var0, "spline", DensityFunctions.Spline.CODEC);
      register(var0, "constant", DensityFunctions.Constant.CODEC);
      return register(var0, "y_clamped_gradient", DensityFunctions.YClampedGradient.CODEC);
   }

   private static MapCodec<? extends DensityFunction> register(Registry<MapCodec<? extends DensityFunction>> var0, String var1, KeyDispatchDataCodec<? extends DensityFunction> var2) {
      return (MapCodec)Registry.register(var0, (String)var1, var2.codec());
   }

   static <A, O> KeyDispatchDataCodec<O> singleArgumentCodec(Codec<A> var0, Function<A, O> var1, Function<O, A> var2) {
      return KeyDispatchDataCodec.of(var0.fieldOf("argument").xmap(var1, var2));
   }

   static <O> KeyDispatchDataCodec<O> singleFunctionArgumentCodec(Function<DensityFunction, O> var0, Function<O, DensityFunction> var1) {
      return singleArgumentCodec(DensityFunction.HOLDER_HELPER_CODEC, var0, var1);
   }

   static <O> KeyDispatchDataCodec<O> doubleFunctionArgumentCodec(BiFunction<DensityFunction, DensityFunction, O> var0, Function<O, DensityFunction> var1, Function<O, DensityFunction> var2) {
      return KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var3) -> {
         return var3.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(var1), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(var2)).apply(var3, var0);
      }));
   }

   static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> var0) {
      return KeyDispatchDataCodec.of(var0);
   }

   private DensityFunctions() {
      super();
   }

   public static DensityFunction interpolated(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Interpolated, var0);
   }

   public static DensityFunction flatCache(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.FlatCache, var0);
   }

   public static DensityFunction cache2d(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Cache2D, var0);
   }

   public static DensityFunction cacheOnce(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheOnce, var0);
   }

   public static DensityFunction cacheAllInCell(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheAllInCell, var0);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, @Deprecated double var1, double var3, double var5, double var7) {
      return mapFromUnitTo(new DensityFunctions.Noise(new DensityFunction.NoiseHolder(var0), var1, var3), var5, var7);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3, double var5) {
      return mappedNoise(var0, 1.0D, var1, var3, var5);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3) {
      return mappedNoise(var0, 1.0D, 1.0D, var1, var3);
   }

   public static DensityFunction shiftedNoise2d(DensityFunction var0, DensityFunction var1, double var2, Holder<NormalNoise.NoiseParameters> var4) {
      return new DensityFunctions.ShiftedNoise(var0, zero(), var1, var2, 0.0D, new DensityFunction.NoiseHolder(var4));
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0) {
      return noise(var0, 1.0D, 1.0D);
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3) {
      return new DensityFunctions.Noise(new DensityFunction.NoiseHolder(var0), var1, var3);
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0, double var1) {
      return noise(var0, 1.0D, var1);
   }

   public static DensityFunction rangeChoice(DensityFunction var0, double var1, double var3, DensityFunction var5, DensityFunction var6) {
      return new DensityFunctions.RangeChoice(var0, var1, var3, var5, var6);
   }

   public static DensityFunction shiftA(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.ShiftA(new DensityFunction.NoiseHolder(var0));
   }

   public static DensityFunction shiftB(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.ShiftB(new DensityFunction.NoiseHolder(var0));
   }

   public static DensityFunction shift(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.Shift(new DensityFunction.NoiseHolder(var0));
   }

   public static DensityFunction blendDensity(DensityFunction var0) {
      return new DensityFunctions.BlendDensity(var0);
   }

   public static DensityFunction endIslands(long var0) {
      return new DensityFunctions.EndIslandDensityFunction(var0);
   }

   public static DensityFunction weirdScaledSampler(DensityFunction var0, Holder<NormalNoise.NoiseParameters> var1, DensityFunctions.WeirdScaledSampler.RarityValueMapper var2) {
      return new DensityFunctions.WeirdScaledSampler(var0, new DensityFunction.NoiseHolder(var1), var2);
   }

   public static DensityFunction add(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.ADD, var0, var1);
   }

   public static DensityFunction mul(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MUL, var0, var1);
   }

   public static DensityFunction min(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MIN, var0, var1);
   }

   public static DensityFunction max(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MAX, var0, var1);
   }

   public static DensityFunction spline(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> var0) {
      return new DensityFunctions.Spline(var0);
   }

   public static DensityFunction zero() {
      return DensityFunctions.Constant.ZERO;
   }

   public static DensityFunction constant(double var0) {
      return new DensityFunctions.Constant(var0);
   }

   public static DensityFunction yClampedGradient(int var0, int var1, double var2, double var4) {
      return new DensityFunctions.YClampedGradient(var0, var1, var2, var4);
   }

   public static DensityFunction map(DensityFunction var0, DensityFunctions.Mapped.Type var1) {
      return DensityFunctions.Mapped.create(var1, var0);
   }

   private static DensityFunction mapFromUnitTo(DensityFunction var0, double var1, double var3) {
      double var5 = (var1 + var3) * 0.5D;
      double var7 = (var3 - var1) * 0.5D;
      return add(constant(var5), mul(constant(var7), var0));
   }

   public static DensityFunction blendAlpha() {
      return DensityFunctions.BlendAlpha.INSTANCE;
   }

   public static DensityFunction blendOffset() {
      return DensityFunctions.BlendOffset.INSTANCE;
   }

   public static DensityFunction lerp(DensityFunction var0, DensityFunction var1, DensityFunction var2) {
      if (var1 instanceof DensityFunctions.Constant) {
         DensityFunctions.Constant var5 = (DensityFunctions.Constant)var1;
         return lerp(var0, var5.value, var2);
      } else {
         DensityFunction var3 = cacheOnce(var0);
         DensityFunction var4 = add(mul(var3, constant(-1.0D)), constant(1.0D));
         return add(mul(var1, var4), mul(var2, var3));
      }
   }

   public static DensityFunction lerp(DensityFunction var0, double var1, DensityFunction var3) {
      return add(mul(var0, add(var3, constant(-var1))), constant(var1));
   }

   static {
      CODEC = BuiltInRegistries.DENSITY_FUNCTION_TYPE.byNameCodec().dispatch((var0) -> {
         return var0.codec().codec();
      }, Function.identity());
      NOISE_VALUE_CODEC = Codec.doubleRange(-1000000.0D, 1000000.0D);
      DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC).xmap((var0) -> {
         return (DensityFunction)var0.map(DensityFunctions::constant, Function.identity());
      }, (var0) -> {
         if (var0 instanceof DensityFunctions.Constant) {
            DensityFunctions.Constant var1 = (DensityFunctions.Constant)var0;
            return Either.left(var1.value());
         } else {
            return Either.right(var0);
         }
      });
   }

   protected static enum BlendAlpha implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private BlendAlpha() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 1.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 1.0D);
      }

      public double minValue() {
         return 1.0D;
      }

      public double maxValue() {
         return 1.0D;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      // $FF: synthetic method
      private static DensityFunctions.BlendAlpha[] $values() {
         return new DensityFunctions.BlendAlpha[]{INSTANCE};
      }
   }

   protected static enum BlendOffset implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private BlendOffset() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 0.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 0.0D);
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return 0.0D;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      // $FF: synthetic method
      private static DensityFunctions.BlendOffset[] $values() {
         return new DensityFunctions.BlendOffset[]{INSTANCE};
      }
   }

   protected static enum BeardifierMarker implements DensityFunctions.BeardifierOrMarker {
      INSTANCE;

      private BeardifierMarker() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 0.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 0.0D);
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return 0.0D;
      }

      // $FF: synthetic method
      private static DensityFunctions.BeardifierMarker[] $values() {
         return new DensityFunctions.BeardifierMarker[]{INSTANCE};
      }
   }

   protected static record Marker(DensityFunctions.Marker.Type type, DensityFunction wrapped) implements DensityFunctions.MarkerOrMarked {
      protected Marker(DensityFunctions.Marker.Type param1, DensityFunction param2) {
         super();
         this.type = var1;
         this.wrapped = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.wrapped.compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.wrapped.fillArray(var1, var2);
      }

      public double minValue() {
         return this.wrapped.minValue();
      }

      public double maxValue() {
         return this.wrapped.maxValue();
      }

      public DensityFunctions.Marker.Type type() {
         return this.type;
      }

      public DensityFunction wrapped() {
         return this.wrapped;
      }

      static enum Type implements StringRepresentable {
         Interpolated("interpolated"),
         FlatCache("flat_cache"),
         Cache2D("cache_2d"),
         CacheOnce("cache_once"),
         CacheAllInCell("cache_all_in_cell");

         private final String name;
         final KeyDispatchDataCodec<DensityFunctions.MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec((var1x) -> {
            return new DensityFunctions.Marker(this, var1x);
         }, DensityFunctions.MarkerOrMarked::wrapped);

         private Type(final String param3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.Marker.Type[] $values() {
            return new DensityFunctions.Marker.Type[]{Interpolated, FlatCache, Cache2D, CacheOnce, CacheAllInCell};
         }
      }
   }

   protected static record Noise(DensityFunction.NoiseHolder noise, double xzScale, double yScale) implements DensityFunction {
      public static final MapCodec<DensityFunctions.Noise> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.Noise::noise), Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.Noise::xzScale), Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.Noise::yScale)).apply(var0, DensityFunctions.Noise::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.Noise> CODEC;

      protected Noise(DensityFunction.NoiseHolder param1, @Deprecated double param2, double param4) {
         super();
         this.noise = var1;
         this.xzScale = var2;
         this.yScale = var4;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.noise.getValue((double)var1.blockX() * this.xzScale, (double)var1.blockY() * this.yScale, (double)var1.blockZ() * this.xzScale);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.Noise(var1.visitNoise(this.noise), this.xzScale, this.yScale));
      }

      public double minValue() {
         return -this.maxValue();
      }

      public double maxValue() {
         return this.noise.maxValue();
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction.NoiseHolder noise() {
         return this.noise;
      }

      /** @deprecated */
      @Deprecated
      public double xzScale() {
         return this.xzScale;
      }

      public double yScale() {
         return this.yScale;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static final class EndIslandDensityFunction implements DensityFunction.SimpleFunction {
      public static final KeyDispatchDataCodec<DensityFunctions.EndIslandDensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(new DensityFunctions.EndIslandDensityFunction(0L)));
      private static final float ISLAND_THRESHOLD = -0.9F;
      private final SimplexNoise islandNoise;

      public EndIslandDensityFunction(long var1) {
         super();
         LegacyRandomSource var3 = new LegacyRandomSource(var1);
         var3.consumeCount(17292);
         this.islandNoise = new SimplexNoise(var3);
      }

      private static float getHeightValue(SimplexNoise var0, int var1, int var2) {
         int var3 = var1 / 2;
         int var4 = var2 / 2;
         int var5 = var1 % 2;
         int var6 = var2 % 2;
         float var7 = 100.0F - Mth.sqrt((float)(var1 * var1 + var2 * var2)) * 8.0F;
         var7 = Mth.clamp(var7, -100.0F, 80.0F);

         for(int var8 = -12; var8 <= 12; ++var8) {
            for(int var9 = -12; var9 <= 12; ++var9) {
               long var10 = (long)(var3 + var8);
               long var12 = (long)(var4 + var9);
               if (var10 * var10 + var12 * var12 > 4096L && var0.getValue((double)var10, (double)var12) < -0.8999999761581421D) {
                  float var14 = (Mth.abs((float)var10) * 3439.0F + Mth.abs((float)var12) * 147.0F) % 13.0F + 9.0F;
                  float var15 = (float)(var5 - var8 * 2);
                  float var16 = (float)(var6 - var9 * 2);
                  float var17 = 100.0F - Mth.sqrt(var15 * var15 + var16 * var16) * var14;
                  var17 = Mth.clamp(var17, -100.0F, 80.0F);
                  var7 = Math.max(var7, var17);
               }
            }
         }

         return var7;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return ((double)getHeightValue(this.islandNoise, var1.blockX() / 8, var1.blockZ() / 8) - 8.0D) / 128.0D;
      }

      public double minValue() {
         return -0.84375D;
      }

      public double maxValue() {
         return 0.5625D;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   protected static record WeirdScaledSampler(DensityFunction input, DensityFunction.NoiseHolder noise, DensityFunctions.WeirdScaledSampler.RarityValueMapper rarityValueMapper) implements DensityFunctions.TransformerWithContext {
      private static final MapCodec<DensityFunctions.WeirdScaledSampler> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.WeirdScaledSampler::input), DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.WeirdScaledSampler::noise), DensityFunctions.WeirdScaledSampler.RarityValueMapper.CODEC.fieldOf("rarity_value_mapper").forGetter(DensityFunctions.WeirdScaledSampler::rarityValueMapper)).apply(var0, DensityFunctions.WeirdScaledSampler::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.WeirdScaledSampler> CODEC;

      protected WeirdScaledSampler(DensityFunction param1, DensityFunction.NoiseHolder param2, DensityFunctions.WeirdScaledSampler.RarityValueMapper param3) {
         super();
         this.input = var1;
         this.noise = var2;
         this.rarityValueMapper = var3;
      }

      public double transform(DensityFunction.FunctionContext var1, double var2) {
         double var4 = this.rarityValueMapper.mapper.get(var2);
         return var4 * Math.abs(this.noise.getValue((double)var1.blockX() / var4, (double)var1.blockY() / var4, (double)var1.blockZ() / var4));
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.WeirdScaledSampler(this.input.mapAll(var1), var1.visitNoise(this.noise), this.rarityValueMapper));
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return this.rarityValueMapper.maxRarity * this.noise.maxValue();
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public DensityFunction.NoiseHolder noise() {
         return this.noise;
      }

      public DensityFunctions.WeirdScaledSampler.RarityValueMapper rarityValueMapper() {
         return this.rarityValueMapper;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }

      public static enum RarityValueMapper implements StringRepresentable {
         TYPE1("type_1", NoiseRouterData.QuantizedSpaghettiRarity::getSpaghettiRarity3D, 2.0D),
         TYPE2("type_2", NoiseRouterData.QuantizedSpaghettiRarity::getSphaghettiRarity2D, 3.0D);

         public static final Codec<DensityFunctions.WeirdScaledSampler.RarityValueMapper> CODEC = StringRepresentable.fromEnum(DensityFunctions.WeirdScaledSampler.RarityValueMapper::values);
         private final String name;
         final Double2DoubleFunction mapper;
         final double maxRarity;

         private RarityValueMapper(final String param3, final Double2DoubleFunction param4, final double param5) {
            this.name = var3;
            this.mapper = var4;
            this.maxRarity = var5;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.WeirdScaledSampler.RarityValueMapper[] $values() {
            return new DensityFunctions.WeirdScaledSampler.RarityValueMapper[]{TYPE1, TYPE2};
         }
      }
   }

   protected static record ShiftedNoise(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise) implements DensityFunction {
      private static final MapCodec<DensityFunctions.ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(DensityFunctions.ShiftedNoise::shiftX), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_y").forGetter(DensityFunctions.ShiftedNoise::shiftY), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(DensityFunctions.ShiftedNoise::shiftZ), Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.ShiftedNoise::xzScale), Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.ShiftedNoise::yScale), DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.ShiftedNoise::noise)).apply(var0, DensityFunctions.ShiftedNoise::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.ShiftedNoise> CODEC;

      protected ShiftedNoise(DensityFunction param1, DensityFunction param2, DensityFunction param3, double param4, double param6, DensityFunction.NoiseHolder param8) {
         super();
         this.shiftX = var1;
         this.shiftY = var2;
         this.shiftZ = var3;
         this.xzScale = var4;
         this.yScale = var6;
         this.noise = var8;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         double var2 = (double)var1.blockX() * this.xzScale + this.shiftX.compute(var1);
         double var4 = (double)var1.blockY() * this.yScale + this.shiftY.compute(var1);
         double var6 = (double)var1.blockZ() * this.xzScale + this.shiftZ.compute(var1);
         return this.noise.getValue(var2, var4, var6);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.ShiftedNoise(this.shiftX.mapAll(var1), this.shiftY.mapAll(var1), this.shiftZ.mapAll(var1), this.xzScale, this.yScale, var1.visitNoise(this.noise)));
      }

      public double minValue() {
         return -this.maxValue();
      }

      public double maxValue() {
         return this.noise.maxValue();
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction shiftX() {
         return this.shiftX;
      }

      public DensityFunction shiftY() {
         return this.shiftY;
      }

      public DensityFunction shiftZ() {
         return this.shiftZ;
      }

      public double xzScale() {
         return this.xzScale;
      }

      public double yScale() {
         return this.yScale;
      }

      public DensityFunction.NoiseHolder noise() {
         return this.noise;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   static record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange) implements DensityFunction {
      public static final MapCodec<DensityFunctions.RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.RangeChoice::input), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(DensityFunctions.RangeChoice::minInclusive), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(DensityFunctions.RangeChoice::maxExclusive), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_in_range").forGetter(DensityFunctions.RangeChoice::whenInRange), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_out_of_range").forGetter(DensityFunctions.RangeChoice::whenOutOfRange)).apply(var0, DensityFunctions.RangeChoice::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.RangeChoice> CODEC;

      RangeChoice(DensityFunction param1, double param2, double param4, DensityFunction param6, DensityFunction param7) {
         super();
         this.input = var1;
         this.minInclusive = var2;
         this.maxExclusive = var4;
         this.whenInRange = var6;
         this.whenOutOfRange = var7;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         double var2 = this.input.compute(var1);
         return var2 >= this.minInclusive && var2 < this.maxExclusive ? this.whenInRange.compute(var1) : this.whenOutOfRange.compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input.fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            double var4 = var1[var3];
            if (var4 >= this.minInclusive && var4 < this.maxExclusive) {
               var1[var3] = this.whenInRange.compute(var2.forIndex(var3));
            } else {
               var1[var3] = this.whenOutOfRange.compute(var2.forIndex(var3));
            }
         }

      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.RangeChoice(this.input.mapAll(var1), this.minInclusive, this.maxExclusive, this.whenInRange.mapAll(var1), this.whenOutOfRange.mapAll(var1)));
      }

      public double minValue() {
         return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
      }

      public double maxValue() {
         return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minInclusive() {
         return this.minInclusive;
      }

      public double maxExclusive() {
         return this.maxExclusive;
      }

      public DensityFunction whenInRange() {
         return this.whenInRange;
      }

      public DensityFunction whenOutOfRange() {
         return this.whenOutOfRange;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static record ShiftA(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      static final KeyDispatchDataCodec<DensityFunctions.ShiftA> CODEC;

      protected ShiftA(DensityFunction.NoiseHolder param1) {
         super();
         this.offsetNoise = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockX(), 0.0D, (double)var1.blockZ());
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.ShiftA(var1.visitNoise(this.offsetNoise)));
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction.NoiseHolder offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftA::new, DensityFunctions.ShiftA::offsetNoise);
      }
   }

   protected static record ShiftB(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      static final KeyDispatchDataCodec<DensityFunctions.ShiftB> CODEC;

      protected ShiftB(DensityFunction.NoiseHolder param1) {
         super();
         this.offsetNoise = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockZ(), (double)var1.blockX(), 0.0D);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.ShiftB(var1.visitNoise(this.offsetNoise)));
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction.NoiseHolder offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftB::new, DensityFunctions.ShiftB::offsetNoise);
      }
   }

   protected static record Shift(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      static final KeyDispatchDataCodec<DensityFunctions.Shift> CODEC;

      protected Shift(DensityFunction.NoiseHolder param1) {
         super();
         this.offsetNoise = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockX(), (double)var1.blockY(), (double)var1.blockZ());
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.Shift(var1.visitNoise(this.offsetNoise)));
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction.NoiseHolder offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, DensityFunctions.Shift::new, DensityFunctions.Shift::offsetNoise);
      }
   }

   static record BlendDensity(DensityFunction input) implements DensityFunctions.TransformerWithContext {
      static final KeyDispatchDataCodec<DensityFunctions.BlendDensity> CODEC = DensityFunctions.singleFunctionArgumentCodec(DensityFunctions.BlendDensity::new, DensityFunctions.BlendDensity::input);

      BlendDensity(DensityFunction param1) {
         super();
         this.input = var1;
      }

      public double transform(DensityFunction.FunctionContext var1, double var2) {
         return var1.getBlender().blendDensity(var1, var2);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.BlendDensity(this.input.mapAll(var1)));
      }

      public double minValue() {
         return -1.0D / 0.0;
      }

      public double maxValue() {
         return 1.0D / 0.0;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }
   }

   protected static record Clamp(DensityFunction input, double minValue, double maxValue) implements DensityFunctions.PureTransformer {
      private static final MapCodec<DensityFunctions.Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.DIRECT_CODEC.fieldOf("input").forGetter(DensityFunctions.Clamp::input), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min").forGetter(DensityFunctions.Clamp::minValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max").forGetter(DensityFunctions.Clamp::maxValue)).apply(var0, DensityFunctions.Clamp::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.Clamp> CODEC;

      protected Clamp(DensityFunction param1, double param2, double param4) {
         super();
         this.input = var1;
         this.minValue = var2;
         this.maxValue = var4;
      }

      public double transform(double var1) {
         return Mth.clamp(var1, this.minValue, this.maxValue);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return new DensityFunctions.Clamp(this.input.mapAll(var1), this.minValue, this.maxValue);
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static record Mapped(DensityFunctions.Mapped.Type type, DensityFunction input, double minValue, double maxValue) implements DensityFunctions.PureTransformer {
      protected Mapped(DensityFunctions.Mapped.Type param1, DensityFunction param2, double param3, double param5) {
         super();
         this.type = var1;
         this.input = var2;
         this.minValue = var3;
         this.maxValue = var5;
      }

      public static DensityFunctions.Mapped create(DensityFunctions.Mapped.Type var0, DensityFunction var1) {
         double var2 = var1.minValue();
         double var4 = transform(var0, var2);
         double var6 = transform(var0, var1.maxValue());
         return var0 != DensityFunctions.Mapped.Type.ABS && var0 != DensityFunctions.Mapped.Type.SQUARE ? new DensityFunctions.Mapped(var0, var1, var4, var6) : new DensityFunctions.Mapped(var0, var1, Math.max(0.0D, var2), Math.max(var4, var6));
      }

      private static double transform(DensityFunctions.Mapped.Type var0, double var1) {
         double var10000;
         switch(var0.ordinal()) {
         case 0:
            var10000 = Math.abs(var1);
            break;
         case 1:
            var10000 = var1 * var1;
            break;
         case 2:
            var10000 = var1 * var1 * var1;
            break;
         case 3:
            var10000 = var1 > 0.0D ? var1 : var1 * 0.5D;
            break;
         case 4:
            var10000 = var1 > 0.0D ? var1 : var1 * 0.25D;
            break;
         case 5:
            double var3 = Mth.clamp(var1, -1.0D, 1.0D);
            var10000 = var3 / 2.0D - var3 * var3 * var3 / 24.0D;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public double transform(double var1) {
         return transform(this.type, var1);
      }

      public DensityFunctions.Mapped mapAll(DensityFunction.Visitor var1) {
         return create(this.type, this.input.mapAll(var1));
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.type.codec;
      }

      public DensityFunctions.Mapped.Type type() {
         return this.type;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      // $FF: synthetic method
      public DensityFunction mapAll(final DensityFunction.Visitor param1) {
         return this.mapAll(var1);
      }

      static enum Type implements StringRepresentable {
         ABS("abs"),
         SQUARE("square"),
         CUBE("cube"),
         HALF_NEGATIVE("half_negative"),
         QUARTER_NEGATIVE("quarter_negative"),
         SQUEEZE("squeeze");

         private final String name;
         final KeyDispatchDataCodec<DensityFunctions.Mapped> codec = DensityFunctions.singleFunctionArgumentCodec((var1x) -> {
            return DensityFunctions.Mapped.create(this, var1x);
         }, DensityFunctions.Mapped::input);

         private Type(final String param3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.Mapped.Type[] $values() {
            return new DensityFunctions.Mapped.Type[]{ABS, SQUARE, CUBE, HALF_NEGATIVE, QUARTER_NEGATIVE, SQUEEZE};
         }
      }
   }

   interface TwoArgumentSimpleFunction extends DensityFunction {
      Logger LOGGER = LogUtils.getLogger();

      static DensityFunctions.TwoArgumentSimpleFunction create(DensityFunctions.TwoArgumentSimpleFunction.Type var0, DensityFunction var1, DensityFunction var2) {
         double var3 = var1.minValue();
         double var5 = var2.minValue();
         double var7 = var1.maxValue();
         double var9 = var2.maxValue();
         if (var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN || var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
            boolean var11 = var3 >= var9;
            boolean var12 = var5 >= var7;
            if (var11 || var12) {
               Logger var10000 = LOGGER;
               String var10001 = String.valueOf(var0);
               var10000.warn("Creating a " + var10001 + " function between two non-overlapping inputs: " + String.valueOf(var1) + " and " + String.valueOf(var2));
            }
         }

         double var17;
         switch(var0.ordinal()) {
         case 0:
            var17 = var3 + var5;
            break;
         case 1:
            var17 = var3 > 0.0D && var5 > 0.0D ? var3 * var5 : (var7 < 0.0D && var9 < 0.0D ? var7 * var9 : Math.min(var3 * var9, var7 * var5));
            break;
         case 2:
            var17 = Math.min(var3, var5);
            break;
         case 3:
            var17 = Math.max(var3, var5);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         double var16 = var17;
         switch(var0.ordinal()) {
         case 0:
            var17 = var7 + var9;
            break;
         case 1:
            var17 = var3 > 0.0D && var5 > 0.0D ? var7 * var9 : (var7 < 0.0D && var9 < 0.0D ? var3 * var5 : Math.max(var3 * var5, var7 * var9));
            break;
         case 2:
            var17 = Math.min(var7, var9);
            break;
         case 3:
            var17 = Math.max(var7, var9);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         double var13 = var17;
         if (var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL || var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
            DensityFunctions.Constant var15;
            if (var1 instanceof DensityFunctions.Constant) {
               var15 = (DensityFunctions.Constant)var1;
               return new DensityFunctions.MulOrAdd(var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL, var2, var16, var13, var15.value);
            }

            if (var2 instanceof DensityFunctions.Constant) {
               var15 = (DensityFunctions.Constant)var2;
               return new DensityFunctions.MulOrAdd(var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL, var1, var16, var13, var15.value);
            }
         }

         return new DensityFunctions.Ap2(var0, var1, var2, var16, var13);
      }

      DensityFunctions.TwoArgumentSimpleFunction.Type type();

      DensityFunction argument1();

      DensityFunction argument2();

      default KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.type().codec;
      }

      public static enum Type implements StringRepresentable {
         ADD("add"),
         MUL("mul"),
         MIN("min"),
         MAX("max");

         final KeyDispatchDataCodec<DensityFunctions.TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec((var1x, var2x) -> {
            return DensityFunctions.TwoArgumentSimpleFunction.create(this, var1x, var2x);
         }, DensityFunctions.TwoArgumentSimpleFunction::argument1, DensityFunctions.TwoArgumentSimpleFunction::argument2);
         private final String name;

         private Type(final String param3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.TwoArgumentSimpleFunction.Type[] $values() {
            return new DensityFunctions.TwoArgumentSimpleFunction.Type[]{ADD, MUL, MIN, MAX};
         }
      }
   }

   public static record Spline(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline) implements DensityFunction {
      private static final Codec<CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate>> SPLINE_CODEC;
      private static final MapCodec<DensityFunctions.Spline> DATA_CODEC;
      public static final KeyDispatchDataCodec<DensityFunctions.Spline> CODEC;

      public Spline(CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> param1) {
         super();
         this.spline = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return (double)this.spline.apply(new DensityFunctions.Spline.Point(var1));
      }

      public double minValue() {
         return (double)this.spline.minValue();
      }

      public double maxValue() {
         return (double)this.spline.maxValue();
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.Spline(this.spline.mapAll((var1x) -> {
            return var1x.mapAll(var1);
         })));
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public CubicSpline<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline() {
         return this.spline;
      }

      static {
         SPLINE_CODEC = CubicSpline.codec(DensityFunctions.Spline.Coordinate.CODEC);
         DATA_CODEC = SPLINE_CODEC.fieldOf("spline").xmap(DensityFunctions.Spline::new, DensityFunctions.Spline::spline);
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }

      public static record Point(DensityFunction.FunctionContext context) {
         public Point(DensityFunction.FunctionContext param1) {
            super();
            this.context = var1;
         }

         public DensityFunction.FunctionContext context() {
            return this.context;
         }
      }

      public static record Coordinate(Holder<DensityFunction> function) implements ToFloatFunction<DensityFunctions.Spline.Point> {
         public static final Codec<DensityFunctions.Spline.Coordinate> CODEC;

         public Coordinate(Holder<DensityFunction> param1) {
            super();
            this.function = var1;
         }

         public String toString() {
            Optional var1 = this.function.unwrapKey();
            if (var1.isPresent()) {
               ResourceKey var2 = (ResourceKey)var1.get();
               if (var2 == NoiseRouterData.CONTINENTS) {
                  return "continents";
               }

               if (var2 == NoiseRouterData.EROSION) {
                  return "erosion";
               }

               if (var2 == NoiseRouterData.RIDGES) {
                  return "weirdness";
               }

               if (var2 == NoiseRouterData.RIDGES_FOLDED) {
                  return "ridges";
               }
            }

            return "Coordinate[" + String.valueOf(this.function) + "]";
         }

         public float apply(DensityFunctions.Spline.Point var1) {
            return (float)((DensityFunction)this.function.value()).compute(var1.context());
         }

         public float minValue() {
            return this.function.isBound() ? (float)((DensityFunction)this.function.value()).minValue() : -1.0F / 0.0;
         }

         public float maxValue() {
            return this.function.isBound() ? (float)((DensityFunction)this.function.value()).maxValue() : 1.0F / 0.0;
         }

         public DensityFunctions.Spline.Coordinate mapAll(DensityFunction.Visitor var1) {
            return new DensityFunctions.Spline.Coordinate(new Holder.Direct(((DensityFunction)this.function.value()).mapAll(var1)));
         }

         public Holder<DensityFunction> function() {
            return this.function;
         }

         static {
            CODEC = DensityFunction.CODEC.xmap(DensityFunctions.Spline.Coordinate::new, DensityFunctions.Spline.Coordinate::function);
         }
      }
   }

   static record Constant(double value) implements DensityFunction.SimpleFunction {
      final double value;
      static final KeyDispatchDataCodec<DensityFunctions.Constant> CODEC;
      static final DensityFunctions.Constant ZERO;

      Constant(double param1) {
         super();
         this.value = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.value;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, this.value);
      }

      public double minValue() {
         return this.value;
      }

      public double maxValue() {
         return this.value;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public double value() {
         return this.value;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(DensityFunctions.NOISE_VALUE_CODEC, DensityFunctions.Constant::new, DensityFunctions.Constant::value);
         ZERO = new DensityFunctions.Constant(0.0D);
      }
   }

   static record YClampedGradient(int fromY, int toY, double fromValue, double toValue) implements DensityFunction.SimpleFunction {
      private static final MapCodec<DensityFunctions.YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("from_y").forGetter(DensityFunctions.YClampedGradient::fromY), Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("to_y").forGetter(DensityFunctions.YClampedGradient::toY), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(DensityFunctions.YClampedGradient::fromValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(DensityFunctions.YClampedGradient::toValue)).apply(var0, DensityFunctions.YClampedGradient::new);
      });
      public static final KeyDispatchDataCodec<DensityFunctions.YClampedGradient> CODEC;

      YClampedGradient(int param1, int param2, double param3, double param5) {
         super();
         this.fromY = var1;
         this.toY = var2;
         this.fromValue = var3;
         this.toValue = var5;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return Mth.clampedMap((double)var1.blockY(), (double)this.fromY, (double)this.toY, this.fromValue, this.toValue);
      }

      public double minValue() {
         return Math.min(this.fromValue, this.toValue);
      }

      public double maxValue() {
         return Math.max(this.fromValue, this.toValue);
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public int fromY() {
         return this.fromY;
      }

      public int toY() {
         return this.toY;
      }

      public double fromValue() {
         return this.fromValue;
      }

      public double toValue() {
         return this.toValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   static record Ap2(DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction argument1, DensityFunction argument2, double minValue, double maxValue) implements DensityFunctions.TwoArgumentSimpleFunction {
      Ap2(DensityFunctions.TwoArgumentSimpleFunction.Type param1, DensityFunction param2, DensityFunction param3, double param4, double param6) {
         super();
         this.type = var1;
         this.argument1 = var2;
         this.argument2 = var3;
         this.minValue = var4;
         this.maxValue = var6;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         double var2 = this.argument1.compute(var1);
         double var10000;
         switch(this.type.ordinal()) {
         case 0:
            var10000 = var2 + this.argument2.compute(var1);
            break;
         case 1:
            var10000 = var2 == 0.0D ? 0.0D : var2 * this.argument2.compute(var1);
            break;
         case 2:
            var10000 = var2 < this.argument2.minValue() ? var2 : Math.min(var2, this.argument2.compute(var1));
            break;
         case 3:
            var10000 = var2 > this.argument2.maxValue() ? var2 : Math.max(var2, this.argument2.compute(var1));
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.argument1.fillArray(var1, var2);
         double var3;
         int var5;
         double var6;
         switch(this.type.ordinal()) {
         case 0:
            double[] var9 = new double[var1.length];
            this.argument2.fillArray(var9, var2);

            for(int var10 = 0; var10 < var1.length; ++var10) {
               var1[var10] += var9[var10];
            }

            return;
         case 1:
            for(int var8 = 0; var8 < var1.length; ++var8) {
               double var4 = var1[var8];
               var1[var8] = var4 == 0.0D ? 0.0D : var4 * this.argument2.compute(var2.forIndex(var8));
            }

            return;
         case 2:
            var3 = this.argument2.minValue();

            for(var5 = 0; var5 < var1.length; ++var5) {
               var6 = var1[var5];
               var1[var5] = var6 < var3 ? var6 : Math.min(var6, this.argument2.compute(var2.forIndex(var5)));
            }

            return;
         case 3:
            var3 = this.argument2.maxValue();

            for(var5 = 0; var5 < var1.length; ++var5) {
               var6 = var1[var5];
               var1[var5] = var6 > var3 ? var6 : Math.max(var6, this.argument2.compute(var2.forIndex(var5)));
            }
         }

      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(DensityFunctions.TwoArgumentSimpleFunction.create(this.type, this.argument1.mapAll(var1), this.argument2.mapAll(var1)));
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
         return this.type;
      }

      public DensityFunction argument1() {
         return this.argument1;
      }

      public DensityFunction argument2() {
         return this.argument2;
      }
   }

   private static record MulOrAdd(DensityFunctions.MulOrAdd.Type specificType, DensityFunction input, double minValue, double maxValue, double argument) implements DensityFunctions.PureTransformer, DensityFunctions.TwoArgumentSimpleFunction {
      MulOrAdd(DensityFunctions.MulOrAdd.Type param1, DensityFunction param2, double param3, double param5, double param7) {
         super();
         this.specificType = var1;
         this.input = var2;
         this.minValue = var3;
         this.maxValue = var5;
         this.argument = var7;
      }

      public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
         return this.specificType == DensityFunctions.MulOrAdd.Type.MUL ? DensityFunctions.TwoArgumentSimpleFunction.Type.MUL : DensityFunctions.TwoArgumentSimpleFunction.Type.ADD;
      }

      public DensityFunction argument1() {
         return DensityFunctions.constant(this.argument);
      }

      public DensityFunction argument2() {
         return this.input;
      }

      public double transform(double var1) {
         double var10000;
         switch(this.specificType.ordinal()) {
         case 0:
            var10000 = var1 * this.argument;
            break;
         case 1:
            var10000 = var1 + this.argument;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         DensityFunction var2 = this.input.mapAll(var1);
         double var3 = var2.minValue();
         double var5 = var2.maxValue();
         double var7;
         double var9;
         if (this.specificType == DensityFunctions.MulOrAdd.Type.ADD) {
            var7 = var3 + this.argument;
            var9 = var5 + this.argument;
         } else if (this.argument >= 0.0D) {
            var7 = var3 * this.argument;
            var9 = var5 * this.argument;
         } else {
            var7 = var5 * this.argument;
            var9 = var3 * this.argument;
         }

         return new DensityFunctions.MulOrAdd(this.specificType, var2, var7, var9, this.argument);
      }

      public DensityFunctions.MulOrAdd.Type specificType() {
         return this.specificType;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      public double argument() {
         return this.argument;
      }

      static enum Type {
         MUL,
         ADD;

         private Type() {
         }

         // $FF: synthetic method
         private static DensityFunctions.MulOrAdd.Type[] $values() {
            return new DensityFunctions.MulOrAdd.Type[]{MUL, ADD};
         }
      }
   }

   interface ShiftNoise extends DensityFunction {
      DensityFunction.NoiseHolder offsetNoise();

      default double minValue() {
         return -this.maxValue();
      }

      default double maxValue() {
         return this.offsetNoise().maxValue() * 4.0D;
      }

      default double compute(double var1, double var3, double var5) {
         return this.offsetNoise().getValue(var1 * 0.25D, var3 * 0.25D, var5 * 0.25D) * 4.0D;
      }

      default void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }
   }

   public interface MarkerOrMarked extends DensityFunction {
      DensityFunctions.Marker.Type type();

      DensityFunction wrapped();

      default KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.type().codec;
      }

      default DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.Marker(this.type(), this.wrapped().mapAll(var1)));
      }
   }

   @VisibleForDebug
   public static record HolderHolder(Holder<DensityFunction> function) implements DensityFunction {
      public HolderHolder(Holder<DensityFunction> param1) {
         super();
         this.function = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return ((DensityFunction)this.function.value()).compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         ((DensityFunction)this.function.value()).fillArray(var1, var2);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return var1.apply(new DensityFunctions.HolderHolder(new Holder.Direct(((DensityFunction)this.function.value()).mapAll(var1))));
      }

      public double minValue() {
         return this.function.isBound() ? ((DensityFunction)this.function.value()).minValue() : -1.0D / 0.0;
      }

      public double maxValue() {
         return this.function.isBound() ? ((DensityFunction)this.function.value()).maxValue() : 1.0D / 0.0;
      }

      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
      }

      public Holder<DensityFunction> function() {
         return this.function;
      }
   }

   public interface BeardifierOrMarker extends DensityFunction.SimpleFunction {
      KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(DensityFunctions.BeardifierMarker.INSTANCE));

      default KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   private interface PureTransformer extends DensityFunction {
      DensityFunction input();

      default double compute(DensityFunction.FunctionContext var1) {
         return this.transform(this.input().compute(var1));
      }

      default void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input().fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var1[var3] = this.transform(var1[var3]);
         }

      }

      double transform(double var1);
   }

   private interface TransformerWithContext extends DensityFunction {
      DensityFunction input();

      default double compute(DensityFunction.FunctionContext var1) {
         return this.transform(var1, this.input().compute(var1));
      }

      default void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input().fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var1[var3] = this.transform(var2.forIndex(var3), var1[var3]);
         }

      }

      double transform(DensityFunction.FunctionContext var1, double var2);
   }
}
