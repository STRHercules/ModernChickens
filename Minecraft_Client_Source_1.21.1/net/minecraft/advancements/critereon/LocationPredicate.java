package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(Optional<LocationPredicate.PositionPredicate> position, Optional<HolderSet<Biome>> biomes, Optional<HolderSet<Structure>> structures, Optional<ResourceKey<Level>> dimension, Optional<Boolean> smokey, Optional<LightPredicate> light, Optional<BlockPredicate> block, Optional<FluidPredicate> fluid, Optional<Boolean> canSeeSky) {
   public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(LocationPredicate.PositionPredicate.CODEC.optionalFieldOf("position").forGetter(LocationPredicate::position), RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(LocationPredicate::biomes), RegistryCodecs.homogeneousList(Registries.STRUCTURE).optionalFieldOf("structures").forGetter(LocationPredicate::structures), ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(LocationPredicate::dimension), Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationPredicate::smokey), LightPredicate.CODEC.optionalFieldOf("light").forGetter(LocationPredicate::light), BlockPredicate.CODEC.optionalFieldOf("block").forGetter(LocationPredicate::block), FluidPredicate.CODEC.optionalFieldOf("fluid").forGetter(LocationPredicate::fluid), Codec.BOOL.optionalFieldOf("can_see_sky").forGetter(LocationPredicate::canSeeSky)).apply(var0, LocationPredicate::new);
   });

   public LocationPredicate(Optional<LocationPredicate.PositionPredicate> param1, Optional<HolderSet<Biome>> param2, Optional<HolderSet<Structure>> param3, Optional<ResourceKey<Level>> param4, Optional<Boolean> param5, Optional<LightPredicate> param6, Optional<BlockPredicate> param7, Optional<FluidPredicate> param8, Optional<Boolean> param9) {
      super();
      this.position = var1;
      this.biomes = var2;
      this.structures = var3;
      this.dimension = var4;
      this.smokey = var5;
      this.light = var6;
      this.block = var7;
      this.fluid = var8;
      this.canSeeSky = var9;
   }

   public boolean matches(ServerLevel var1, double var2, double var4, double var6) {
      if (this.position.isPresent() && !((LocationPredicate.PositionPredicate)this.position.get()).matches(var2, var4, var6)) {
         return false;
      } else if (this.dimension.isPresent() && this.dimension.get() != var1.dimension()) {
         return false;
      } else {
         BlockPos var8 = BlockPos.containing(var2, var4, var6);
         boolean var9 = var1.isLoaded(var8);
         if (this.biomes.isPresent() && (!var9 || !((HolderSet)this.biomes.get()).contains(var1.getBiome(var8)))) {
            return false;
         } else if (this.structures.isPresent() && (!var9 || !var1.structureManager().getStructureWithPieceAt(var8, (HolderSet)this.structures.get()).isValid())) {
            return false;
         } else if (!this.smokey.isPresent() || var9 && (Boolean)this.smokey.get() == CampfireBlock.isSmokeyPos(var1, var8)) {
            if (this.light.isPresent() && !((LightPredicate)this.light.get()).matches(var1, var8)) {
               return false;
            } else if (this.block.isPresent() && !((BlockPredicate)this.block.get()).matches(var1, var8)) {
               return false;
            } else if (this.fluid.isPresent() && !((FluidPredicate)this.fluid.get()).matches(var1, var8)) {
               return false;
            } else {
               return !this.canSeeSky.isPresent() || (Boolean)this.canSeeSky.get() == var1.canSeeSky(var8);
            }
         } else {
            return false;
         }
      }
   }

   public Optional<LocationPredicate.PositionPredicate> position() {
      return this.position;
   }

   public Optional<HolderSet<Biome>> biomes() {
      return this.biomes;
   }

   public Optional<HolderSet<Structure>> structures() {
      return this.structures;
   }

   public Optional<ResourceKey<Level>> dimension() {
      return this.dimension;
   }

   public Optional<Boolean> smokey() {
      return this.smokey;
   }

   public Optional<LightPredicate> light() {
      return this.light;
   }

   public Optional<BlockPredicate> block() {
      return this.block;
   }

   public Optional<FluidPredicate> fluid() {
      return this.fluid;
   }

   public Optional<Boolean> canSeeSky() {
      return this.canSeeSky;
   }

   private static record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
      public static final Codec<LocationPredicate.PositionPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::x), MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::y), MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::z)).apply(var0, LocationPredicate.PositionPredicate::new);
      });

      private PositionPredicate(MinMaxBounds.Doubles param1, MinMaxBounds.Doubles param2, MinMaxBounds.Doubles param3) {
         super();
         this.x = var1;
         this.y = var2;
         this.z = var3;
      }

      static Optional<LocationPredicate.PositionPredicate> of(MinMaxBounds.Doubles var0, MinMaxBounds.Doubles var1, MinMaxBounds.Doubles var2) {
         return var0.isAny() && var1.isAny() && var2.isAny() ? Optional.empty() : Optional.of(new LocationPredicate.PositionPredicate(var0, var1, var2));
      }

      public boolean matches(double var1, double var3, double var5) {
         return this.x.matches(var1) && this.y.matches(var3) && this.z.matches(var5);
      }

      public MinMaxBounds.Doubles x() {
         return this.x;
      }

      public MinMaxBounds.Doubles y() {
         return this.y;
      }

      public MinMaxBounds.Doubles z() {
         return this.z;
      }
   }

   public static class Builder {
      private MinMaxBounds.Doubles x;
      private MinMaxBounds.Doubles y;
      private MinMaxBounds.Doubles z;
      private Optional<HolderSet<Biome>> biomes;
      private Optional<HolderSet<Structure>> structures;
      private Optional<ResourceKey<Level>> dimension;
      private Optional<Boolean> smokey;
      private Optional<LightPredicate> light;
      private Optional<BlockPredicate> block;
      private Optional<FluidPredicate> fluid;
      private Optional<Boolean> canSeeSky;

      public Builder() {
         super();
         this.x = MinMaxBounds.Doubles.ANY;
         this.y = MinMaxBounds.Doubles.ANY;
         this.z = MinMaxBounds.Doubles.ANY;
         this.biomes = Optional.empty();
         this.structures = Optional.empty();
         this.dimension = Optional.empty();
         this.smokey = Optional.empty();
         this.light = Optional.empty();
         this.block = Optional.empty();
         this.fluid = Optional.empty();
         this.canSeeSky = Optional.empty();
      }

      public static LocationPredicate.Builder location() {
         return new LocationPredicate.Builder();
      }

      public static LocationPredicate.Builder inBiome(Holder<Biome> var0) {
         return location().setBiomes(HolderSet.direct(var0));
      }

      public static LocationPredicate.Builder inDimension(ResourceKey<Level> var0) {
         return location().setDimension(var0);
      }

      public static LocationPredicate.Builder inStructure(Holder<Structure> var0) {
         return location().setStructures(HolderSet.direct(var0));
      }

      public static LocationPredicate.Builder atYLocation(MinMaxBounds.Doubles var0) {
         return location().setY(var0);
      }

      public LocationPredicate.Builder setX(MinMaxBounds.Doubles var1) {
         this.x = var1;
         return this;
      }

      public LocationPredicate.Builder setY(MinMaxBounds.Doubles var1) {
         this.y = var1;
         return this;
      }

      public LocationPredicate.Builder setZ(MinMaxBounds.Doubles var1) {
         this.z = var1;
         return this;
      }

      public LocationPredicate.Builder setBiomes(HolderSet<Biome> var1) {
         this.biomes = Optional.of(var1);
         return this;
      }

      public LocationPredicate.Builder setStructures(HolderSet<Structure> var1) {
         this.structures = Optional.of(var1);
         return this;
      }

      public LocationPredicate.Builder setDimension(ResourceKey<Level> var1) {
         this.dimension = Optional.of(var1);
         return this;
      }

      public LocationPredicate.Builder setLight(LightPredicate.Builder var1) {
         this.light = Optional.of(var1.build());
         return this;
      }

      public LocationPredicate.Builder setBlock(BlockPredicate.Builder var1) {
         this.block = Optional.of(var1.build());
         return this;
      }

      public LocationPredicate.Builder setFluid(FluidPredicate.Builder var1) {
         this.fluid = Optional.of(var1.build());
         return this;
      }

      public LocationPredicate.Builder setSmokey(boolean var1) {
         this.smokey = Optional.of(var1);
         return this;
      }

      public LocationPredicate.Builder setCanSeeSky(boolean var1) {
         this.canSeeSky = Optional.of(var1);
         return this;
      }

      public LocationPredicate build() {
         Optional var1 = LocationPredicate.PositionPredicate.of(this.x, this.y, this.z);
         return new LocationPredicate(var1, this.biomes, this.structures, this.dimension, this.smokey, this.light, this.block, this.fluid, this.canSeeSky);
      }
   }
}
