package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface AllOf {
   static <T, A extends T> MapCodec<A> codec(Codec<T> var0, Function<List<T>, A> var1, Function<A, List<T>> var2) {
      return RecordCodecBuilder.mapCodec((var3) -> {
         return var3.group(var0.listOf().fieldOf("effects").forGetter(var2)).apply(var3, var1);
      });
   }

   static AllOf.EntityEffects entityEffects(EnchantmentEntityEffect... var0) {
      return new AllOf.EntityEffects(List.of(var0));
   }

   static AllOf.LocationBasedEffects locationBasedEffects(EnchantmentLocationBasedEffect... var0) {
      return new AllOf.LocationBasedEffects(List.of(var0));
   }

   static AllOf.ValueEffects valueEffects(EnchantmentValueEffect... var0) {
      return new AllOf.ValueEffects(List.of(var0));
   }

   public static record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {
      public static final MapCodec<AllOf.EntityEffects> CODEC;

      public EntityEffects(List<EnchantmentEntityEffect> param1) {
         super();
         this.effects = var1;
      }

      public void apply(ServerLevel var1, int var2, EnchantedItemInUse var3, Entity var4, Vec3 var5) {
         Iterator var6 = this.effects.iterator();

         while(var6.hasNext()) {
            EnchantmentEntityEffect var7 = (EnchantmentEntityEffect)var6.next();
            var7.apply(var1, var2, var3, var4, var5);
         }

      }

      public MapCodec<AllOf.EntityEffects> codec() {
         return CODEC;
      }

      public List<EnchantmentEntityEffect> effects() {
         return this.effects;
      }

      static {
         CODEC = AllOf.codec(EnchantmentEntityEffect.CODEC, AllOf.EntityEffects::new, AllOf.EntityEffects::effects);
      }
   }

   public static record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect {
      public static final MapCodec<AllOf.LocationBasedEffects> CODEC;

      public LocationBasedEffects(List<EnchantmentLocationBasedEffect> param1) {
         super();
         this.effects = var1;
      }

      public void onChangedBlock(ServerLevel var1, int var2, EnchantedItemInUse var3, Entity var4, Vec3 var5, boolean var6) {
         Iterator var7 = this.effects.iterator();

         while(var7.hasNext()) {
            EnchantmentLocationBasedEffect var8 = (EnchantmentLocationBasedEffect)var7.next();
            var8.onChangedBlock(var1, var2, var3, var4, var5, var6);
         }

      }

      public void onDeactivated(EnchantedItemInUse var1, Entity var2, Vec3 var3, int var4) {
         Iterator var5 = this.effects.iterator();

         while(var5.hasNext()) {
            EnchantmentLocationBasedEffect var6 = (EnchantmentLocationBasedEffect)var5.next();
            var6.onDeactivated(var1, var2, var3, var4);
         }

      }

      public MapCodec<AllOf.LocationBasedEffects> codec() {
         return CODEC;
      }

      public List<EnchantmentLocationBasedEffect> effects() {
         return this.effects;
      }

      static {
         CODEC = AllOf.codec(EnchantmentLocationBasedEffect.CODEC, AllOf.LocationBasedEffects::new, AllOf.LocationBasedEffects::effects);
      }
   }

   public static record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {
      public static final MapCodec<AllOf.ValueEffects> CODEC;

      public ValueEffects(List<EnchantmentValueEffect> param1) {
         super();
         this.effects = var1;
      }

      public float process(int var1, RandomSource var2, float var3) {
         EnchantmentValueEffect var5;
         for(Iterator var4 = this.effects.iterator(); var4.hasNext(); var3 = var5.process(var1, var2, var3)) {
            var5 = (EnchantmentValueEffect)var4.next();
         }

         return var3;
      }

      public MapCodec<AllOf.ValueEffects> codec() {
         return CODEC;
      }

      public List<EnchantmentValueEffect> effects() {
         return this.effects;
      }

      static {
         CODEC = AllOf.codec(EnchantmentValueEffect.CODEC, AllOf.ValueEffects::new, AllOf.ValueEffects::effects);
      }
   }
}
