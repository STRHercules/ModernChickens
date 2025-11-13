package net.minecraft.world.level.levelgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
   public static final float GLOBAL_OFFSET = -0.50375F;
   private static final float ORE_THICKNESS = 0.08F;
   private static final double VEININESS_FREQUENCY = 1.5D;
   private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
   private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;
   private static final double CHEESE_NOISE_TARGET = -0.703125D;
   public static final int ISLAND_CHUNK_DISTANCE = 64;
   public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
   private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
   private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
   private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
   private static final ResourceKey<DensityFunction> Y = createKey("y");
   private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
   private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = createKey("overworld/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = createKey("nether/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = createKey("end/base_3d_noise");
   public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
   public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
   public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
   public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
   public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
   public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
   public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
   public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
   public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
   public static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
   private static final ResourceKey<DensityFunction> OFFSET_LARGE = createKey("overworld_large_biomes/offset");
   private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = createKey("overworld_large_biomes/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
   private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = createKey("overworld_amplified/offset");
   private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = createKey("overworld_amplified/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = createKey("overworld_amplified/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = createKey("overworld_amplified/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = createKey("overworld_amplified/sloped_cheese");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
   private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
   private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
   private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
   private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

   public NoiseRouterData() {
      super();
   }

   private static ResourceKey<DensityFunction> createKey(String var0) {
      return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(var0));
   }

   public static Holder<? extends DensityFunction> bootstrap(BootstrapContext<DensityFunction> var0) {
      HolderGetter var1 = var0.lookup(Registries.NOISE);
      HolderGetter var2 = var0.lookup(Registries.DENSITY_FUNCTION);
      var0.register(ZERO, DensityFunctions.zero());
      int var3 = DimensionType.MIN_Y * 2;
      int var4 = DimensionType.MAX_Y * 2;
      var0.register(Y, DensityFunctions.yClampedGradient(var3, var4, (double)var3, (double)var4));
      DensityFunction var5 = registerAndWrap(var0, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(var1.getOrThrow(Noises.SHIFT)))));
      DensityFunction var6 = registerAndWrap(var0, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(var1.getOrThrow(Noises.SHIFT)))));
      var0.register(BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25D, 0.125D, 80.0D, 160.0D, 8.0D));
      var0.register(BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25D, 0.375D, 80.0D, 60.0D, 8.0D));
      var0.register(BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25D, 0.25D, 80.0D, 160.0D, 4.0D));
      Holder.Reference var7 = var0.register(CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var5, var6, 0.25D, var1.getOrThrow(Noises.CONTINENTALNESS))));
      Holder.Reference var8 = var0.register(EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var5, var6, 0.25D, var1.getOrThrow(Noises.EROSION))));
      DensityFunction var9 = registerAndWrap(var0, RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var5, var6, 0.25D, var1.getOrThrow(Noises.RIDGE))));
      var0.register(RIDGES_FOLDED, peaksAndValleys(var9));
      DensityFunction var10 = DensityFunctions.noise(var1.getOrThrow(Noises.JAGGED), 1500.0D, 0.0D);
      registerTerrainNoises(var0, var2, var10, var7, var8, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
      Holder.Reference var11 = var0.register(CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var5, var6, 0.25D, var1.getOrThrow(Noises.CONTINENTALNESS_LARGE))));
      Holder.Reference var12 = var0.register(EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var5, var6, 0.25D, var1.getOrThrow(Noises.EROSION_LARGE))));
      registerTerrainNoises(var0, var2, var10, var11, var12, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false);
      registerTerrainNoises(var0, var2, var10, var7, var8, OFFSET_AMPLIFIED, FACTOR_AMPLIFIED, JAGGEDNESS_AMPLIFIED, DEPTH_AMPLIFIED, SLOPED_CHEESE_AMPLIFIED, true);
      var0.register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(var2, BASE_3D_NOISE_END)));
      var0.register(SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction(var1));
      var0.register(SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(var1.getOrThrow(Noises.SPAGHETTI_2D_THICKNESS), 2.0D, 1.0D, -0.6D, -1.3D)));
      var0.register(SPAGHETTI_2D, spaghetti2D(var2, var1));
      var0.register(ENTRANCES, entrances(var2, var1));
      var0.register(NOODLE, noodle(var2, var1));
      return var0.register(PILLARS, pillars(var1));
   }

   private static void registerTerrainNoises(BootstrapContext<DensityFunction> var0, HolderGetter<DensityFunction> var1, DensityFunction var2, Holder<DensityFunction> var3, Holder<DensityFunction> var4, ResourceKey<DensityFunction> var5, ResourceKey<DensityFunction> var6, ResourceKey<DensityFunction> var7, ResourceKey<DensityFunction> var8, ResourceKey<DensityFunction> var9, boolean var10) {
      DensityFunctions.Spline.Coordinate var11 = new DensityFunctions.Spline.Coordinate(var3);
      DensityFunctions.Spline.Coordinate var12 = new DensityFunctions.Spline.Coordinate(var4);
      DensityFunctions.Spline.Coordinate var13 = new DensityFunctions.Spline.Coordinate(var1.getOrThrow(RIDGES));
      DensityFunctions.Spline.Coordinate var14 = new DensityFunctions.Spline.Coordinate(var1.getOrThrow(RIDGES_FOLDED));
      DensityFunction var15 = registerAndWrap(var0, var5, splineWithBlending(DensityFunctions.add(DensityFunctions.constant(-0.5037500262260437D), DensityFunctions.spline(TerrainProvider.overworldOffset(var11, var12, var14, var10))), DensityFunctions.blendOffset()));
      DensityFunction var16 = registerAndWrap(var0, var6, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(var11, var12, var13, var14, var10)), BLENDING_FACTOR));
      DensityFunction var17 = registerAndWrap(var0, var8, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), var15));
      DensityFunction var18 = registerAndWrap(var0, var7, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(var11, var12, var13, var14, var10)), BLENDING_JAGGEDNESS));
      DensityFunction var19 = DensityFunctions.mul(var18, var2.halfNegative());
      DensityFunction var20 = noiseGradientDensity(var16, DensityFunctions.add(var17, var19));
      var0.register(var9, DensityFunctions.add(var20, getFunction(var1, BASE_3D_NOISE_OVERWORLD)));
   }

   private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> var0, ResourceKey<DensityFunction> var1, DensityFunction var2) {
      return new DensityFunctions.HolderHolder(var0.register(var1, var2));
   }

   private static DensityFunction getFunction(HolderGetter<DensityFunction> var0, ResourceKey<DensityFunction> var1) {
      return new DensityFunctions.HolderHolder(var0.getOrThrow(var1));
   }

   private static DensityFunction peaksAndValleys(DensityFunction var0) {
      return DensityFunctions.mul(DensityFunctions.add(DensityFunctions.add(var0.abs(), DensityFunctions.constant(-0.6666666666666666D)).abs(), DensityFunctions.constant(-0.3333333333333333D)), DensityFunctions.constant(-3.0D));
   }

   public static float peaksAndValleys(float var0) {
      return -(Math.abs(Math.abs(var0) - 0.6666667F) - 0.33333334F) * 3.0F;
   }

   private static DensityFunction spaghettiRoughnessFunction(HolderGetter<NormalNoise.NoiseParameters> var0) {
      DensityFunction var1 = DensityFunctions.noise(var0.getOrThrow(Noises.SPAGHETTI_ROUGHNESS));
      DensityFunction var2 = DensityFunctions.mappedNoise(var0.getOrThrow(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0D, -0.1D);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(var2, DensityFunctions.add(var1.abs(), DensityFunctions.constant(-0.4D))));
   }

   private static DensityFunction entrances(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      DensityFunction var2 = DensityFunctions.cacheOnce(DensityFunctions.noise(var1.getOrThrow(Noises.SPAGHETTI_3D_RARITY), 2.0D, 1.0D));
      DensityFunction var3 = DensityFunctions.mappedNoise(var1.getOrThrow(Noises.SPAGHETTI_3D_THICKNESS), -0.065D, -0.088D);
      DensityFunction var4 = DensityFunctions.weirdScaledSampler(var2, var1.getOrThrow(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction var5 = DensityFunctions.weirdScaledSampler(var2, var1.getOrThrow(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction var6 = DensityFunctions.add(DensityFunctions.max(var4, var5), var3).clamp(-1.0D, 1.0D);
      DensityFunction var7 = getFunction(var0, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction var8 = DensityFunctions.noise(var1.getOrThrow(Noises.CAVE_ENTRANCE), 0.75D, 0.5D);
      DensityFunction var9 = DensityFunctions.add(DensityFunctions.add(var8, DensityFunctions.constant(0.37D)), DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
      return DensityFunctions.cacheOnce(DensityFunctions.min(var9, DensityFunctions.add(var7, var6)));
   }

   private static DensityFunction noodle(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      DensityFunction var2 = getFunction(var0, Y);
      boolean var3 = true;
      boolean var4 = true;
      boolean var5 = true;
      DensityFunction var6 = yLimitedInterpolatable(var2, DensityFunctions.noise(var1.getOrThrow(Noises.NOODLE), 1.0D, 1.0D), -60, 320, -1);
      DensityFunction var7 = yLimitedInterpolatable(var2, DensityFunctions.mappedNoise(var1.getOrThrow(Noises.NOODLE_THICKNESS), 1.0D, 1.0D, -0.05D, -0.1D), -60, 320, 0);
      double var8 = 2.6666666666666665D;
      DensityFunction var10 = yLimitedInterpolatable(var2, DensityFunctions.noise(var1.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction var11 = yLimitedInterpolatable(var2, DensityFunctions.noise(var1.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction var12 = DensityFunctions.mul(DensityFunctions.constant(1.5D), DensityFunctions.max(var10.abs(), var11.abs()));
      return DensityFunctions.rangeChoice(var6, -1000000.0D, 0.0D, DensityFunctions.constant(64.0D), DensityFunctions.add(var7, var12));
   }

   private static DensityFunction pillars(HolderGetter<NormalNoise.NoiseParameters> var0) {
      double var1 = 25.0D;
      double var3 = 0.3D;
      DensityFunction var5 = DensityFunctions.noise(var0.getOrThrow(Noises.PILLAR), 25.0D, 0.3D);
      DensityFunction var6 = DensityFunctions.mappedNoise(var0.getOrThrow(Noises.PILLAR_RARENESS), 0.0D, -2.0D);
      DensityFunction var7 = DensityFunctions.mappedNoise(var0.getOrThrow(Noises.PILLAR_THICKNESS), 0.0D, 1.1D);
      DensityFunction var8 = DensityFunctions.add(DensityFunctions.mul(var5, DensityFunctions.constant(2.0D)), var6);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(var8, var7.cube()));
   }

   private static DensityFunction spaghetti2D(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      DensityFunction var2 = DensityFunctions.noise(var1.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0D, 1.0D);
      DensityFunction var3 = DensityFunctions.weirdScaledSampler(var2, var1.getOrThrow(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
      DensityFunction var4 = DensityFunctions.mappedNoise(var1.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0D, (double)Math.floorDiv(-64, 8), 8.0D);
      DensityFunction var5 = getFunction(var0, SPAGHETTI_2D_THICKNESS_MODULATOR);
      DensityFunction var6 = DensityFunctions.add(var4, DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
      DensityFunction var7 = DensityFunctions.add(var6, var5).cube();
      double var8 = 0.083D;
      DensityFunction var10 = DensityFunctions.add(var3, DensityFunctions.mul(DensityFunctions.constant(0.083D), var5));
      return DensityFunctions.max(var10, var7).clamp(-1.0D, 1.0D);
   }

   private static DensityFunction underground(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1, DensityFunction var2) {
      DensityFunction var3 = getFunction(var0, SPAGHETTI_2D);
      DensityFunction var4 = getFunction(var0, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction var5 = DensityFunctions.noise(var1.getOrThrow(Noises.CAVE_LAYER), 8.0D);
      DensityFunction var6 = DensityFunctions.mul(DensityFunctions.constant(4.0D), var5.square());
      DensityFunction var7 = DensityFunctions.noise(var1.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666D);
      DensityFunction var8 = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27D), var7).clamp(-1.0D, 1.0D), DensityFunctions.add(DensityFunctions.constant(1.5D), DensityFunctions.mul(DensityFunctions.constant(-0.64D), var2)).clamp(0.0D, 0.5D));
      DensityFunction var9 = DensityFunctions.add(var6, var8);
      DensityFunction var10 = DensityFunctions.min(DensityFunctions.min(var9, getFunction(var0, ENTRANCES)), DensityFunctions.add(var3, var4));
      DensityFunction var11 = getFunction(var0, PILLARS);
      DensityFunction var12 = DensityFunctions.rangeChoice(var11, -1000000.0D, 0.03D, DensityFunctions.constant(-1000000.0D), var11);
      return DensityFunctions.max(var10, var12);
   }

   private static DensityFunction postProcess(DensityFunction var0) {
      DensityFunction var1 = DensityFunctions.blendDensity(var0);
      return DensityFunctions.mul(DensityFunctions.interpolated(var1), DensityFunctions.constant(0.64D)).squeeze();
   }

   protected static NoiseRouter overworld(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1, boolean var2, boolean var3) {
      DensityFunction var4 = DensityFunctions.noise(var1.getOrThrow(Noises.AQUIFER_BARRIER), 0.5D);
      DensityFunction var5 = DensityFunctions.noise(var1.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67D);
      DensityFunction var6 = DensityFunctions.noise(var1.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143D);
      DensityFunction var7 = DensityFunctions.noise(var1.getOrThrow(Noises.AQUIFER_LAVA));
      DensityFunction var8 = getFunction(var0, SHIFT_X);
      DensityFunction var9 = getFunction(var0, SHIFT_Z);
      DensityFunction var10 = DensityFunctions.shiftedNoise2d(var8, var9, 0.25D, var1.getOrThrow(var2 ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
      DensityFunction var11 = DensityFunctions.shiftedNoise2d(var8, var9, 0.25D, var1.getOrThrow(var2 ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
      DensityFunction var12 = getFunction(var0, var2 ? FACTOR_LARGE : (var3 ? FACTOR_AMPLIFIED : FACTOR));
      DensityFunction var13 = getFunction(var0, var2 ? DEPTH_LARGE : (var3 ? DEPTH_AMPLIFIED : DEPTH));
      DensityFunction var14 = noiseGradientDensity(DensityFunctions.cache2d(var12), var13);
      DensityFunction var15 = getFunction(var0, var2 ? SLOPED_CHEESE_LARGE : (var3 ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE));
      DensityFunction var16 = DensityFunctions.min(var15, DensityFunctions.mul(DensityFunctions.constant(5.0D), getFunction(var0, ENTRANCES)));
      DensityFunction var17 = DensityFunctions.rangeChoice(var15, -1000000.0D, 1.5625D, var16, underground(var0, var1, var15));
      DensityFunction var18 = DensityFunctions.min(postProcess(slideOverworld(var3, var17)), getFunction(var0, NOODLE));
      DensityFunction var19 = getFunction(var0, Y);
      int var20 = Stream.of(OreVeinifier.VeinType.values()).mapToInt((var0x) -> {
         return var0x.minY;
      }).min().orElse(-DimensionType.MIN_Y * 2);
      int var21 = Stream.of(OreVeinifier.VeinType.values()).mapToInt((var0x) -> {
         return var0x.maxY;
      }).max().orElse(-DimensionType.MIN_Y * 2);
      DensityFunction var22 = yLimitedInterpolatable(var19, DensityFunctions.noise(var1.getOrThrow(Noises.ORE_VEININESS), 1.5D, 1.5D), var20, var21, 0);
      float var23 = 4.0F;
      DensityFunction var24 = yLimitedInterpolatable(var19, DensityFunctions.noise(var1.getOrThrow(Noises.ORE_VEIN_A), 4.0D, 4.0D), var20, var21, 0).abs();
      DensityFunction var25 = yLimitedInterpolatable(var19, DensityFunctions.noise(var1.getOrThrow(Noises.ORE_VEIN_B), 4.0D, 4.0D), var20, var21, 0).abs();
      DensityFunction var26 = DensityFunctions.add(DensityFunctions.constant(-0.07999999821186066D), DensityFunctions.max(var24, var25));
      DensityFunction var27 = DensityFunctions.noise(var1.getOrThrow(Noises.ORE_GAP));
      return new NoiseRouter(var4, var5, var6, var7, var10, var11, getFunction(var0, var2 ? CONTINENTS_LARGE : CONTINENTS), getFunction(var0, var2 ? EROSION_LARGE : EROSION), var13, getFunction(var0, RIDGES), slideOverworld(var3, DensityFunctions.add(var14, DensityFunctions.constant(-0.703125D)).clamp(-64.0D, 64.0D)), var18, var22, var26, var27);
   }

   private static NoiseRouter noNewCaves(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1, DensityFunction var2) {
      DensityFunction var3 = getFunction(var0, SHIFT_X);
      DensityFunction var4 = getFunction(var0, SHIFT_Z);
      DensityFunction var5 = DensityFunctions.shiftedNoise2d(var3, var4, 0.25D, var1.getOrThrow(Noises.TEMPERATURE));
      DensityFunction var6 = DensityFunctions.shiftedNoise2d(var3, var4, 0.25D, var1.getOrThrow(Noises.VEGETATION));
      DensityFunction var7 = postProcess(var2);
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), var5, var6, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), var7, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   private static DensityFunction slideOverworld(boolean var0, DensityFunction var1) {
      return slide(var1, -64, 384, var0 ? 16 : 80, var0 ? 0 : 64, -0.078125D, 0, 24, var0 ? 0.4D : 0.1171875D);
   }

   private static DensityFunction slideNetherLike(HolderGetter<DensityFunction> var0, int var1, int var2) {
      return slide(getFunction(var0, BASE_3D_NOISE_NETHER), var1, var2, 24, 0, 0.9375D, -8, 24, 2.5D);
   }

   private static DensityFunction slideEndLike(DensityFunction var0, int var1, int var2) {
      return slide(var0, var1, var2, 72, -184, -23.4375D, 4, 32, -0.234375D);
   }

   protected static NoiseRouter nether(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      return noNewCaves(var0, var1, slideNetherLike(var0, 0, 128));
   }

   protected static NoiseRouter caves(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      return noNewCaves(var0, var1, slideNetherLike(var0, -64, 192));
   }

   protected static NoiseRouter floatingIslands(HolderGetter<DensityFunction> var0, HolderGetter<NormalNoise.NoiseParameters> var1) {
      return noNewCaves(var0, var1, slideEndLike(getFunction(var0, BASE_3D_NOISE_END), 0, 256));
   }

   private static DensityFunction slideEnd(DensityFunction var0) {
      return slideEndLike(var0, 0, 128);
   }

   protected static NoiseRouter end(HolderGetter<DensityFunction> var0) {
      DensityFunction var1 = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
      DensityFunction var2 = postProcess(slideEnd(getFunction(var0, SLOPED_CHEESE_END)));
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), var1, DensityFunctions.zero(), DensityFunctions.zero(), slideEnd(DensityFunctions.add(var1, DensityFunctions.constant(-0.703125D))), var2, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   protected static NoiseRouter none() {
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   private static DensityFunction splineWithBlending(DensityFunction var0, DensityFunction var1) {
      DensityFunction var2 = DensityFunctions.lerp(DensityFunctions.blendAlpha(), var1, var0);
      return DensityFunctions.flatCache(DensityFunctions.cache2d(var2));
   }

   private static DensityFunction noiseGradientDensity(DensityFunction var0, DensityFunction var1) {
      DensityFunction var2 = DensityFunctions.mul(var1, var0);
      return DensityFunctions.mul(DensityFunctions.constant(4.0D), var2.quarterNegative());
   }

   private static DensityFunction yLimitedInterpolatable(DensityFunction var0, DensityFunction var1, int var2, int var3, int var4) {
      return DensityFunctions.interpolated(DensityFunctions.rangeChoice(var0, (double)var2, (double)(var3 + 1), var1, DensityFunctions.constant((double)var4)));
   }

   private static DensityFunction slide(DensityFunction var0, int var1, int var2, int var3, int var4, double var5, int var7, int var8, double var9) {
      DensityFunction var12 = DensityFunctions.yClampedGradient(var1 + var2 - var3, var1 + var2 - var4, 1.0D, 0.0D);
      DensityFunction var11 = DensityFunctions.lerp(var12, var5, var0);
      DensityFunction var13 = DensityFunctions.yClampedGradient(var1 + var7, var1 + var8, 0.0D, 1.0D);
      var11 = DensityFunctions.lerp(var13, var9, var11);
      return var11;
   }

   protected static final class QuantizedSpaghettiRarity {
      protected QuantizedSpaghettiRarity() {
         super();
      }

      protected static double getSphaghettiRarity2D(double var0) {
         if (var0 < -0.75D) {
            return 0.5D;
         } else if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.5D) {
            return 1.0D;
         } else {
            return var0 < 0.75D ? 2.0D : 3.0D;
         }
      }

      protected static double getSpaghettiRarity3D(double var0) {
         if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.0D) {
            return 1.0D;
         } else {
            return var0 < 0.5D ? 1.5D : 2.0D;
         }
      }
   }
}
