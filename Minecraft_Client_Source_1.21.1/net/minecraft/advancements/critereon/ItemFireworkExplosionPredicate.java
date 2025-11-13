package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;

public record ItemFireworkExplosionPredicate(ItemFireworkExplosionPredicate.FireworkPredicate predicate) implements SingleComponentItemPredicate<FireworkExplosion> {
   public static final Codec<ItemFireworkExplosionPredicate> CODEC;

   public ItemFireworkExplosionPredicate(ItemFireworkExplosionPredicate.FireworkPredicate param1) {
      super();
      this.predicate = var1;
   }

   public DataComponentType<FireworkExplosion> componentType() {
      return DataComponents.FIREWORK_EXPLOSION;
   }

   public boolean matches(ItemStack var1, FireworkExplosion var2) {
      return this.predicate.test(var2);
   }

   public ItemFireworkExplosionPredicate.FireworkPredicate predicate() {
      return this.predicate;
   }

   static {
      CODEC = ItemFireworkExplosionPredicate.FireworkPredicate.CODEC.xmap(ItemFireworkExplosionPredicate::new, ItemFireworkExplosionPredicate::predicate);
   }

   public static record FireworkPredicate(Optional<FireworkExplosion.Shape> shape, Optional<Boolean> twinkle, Optional<Boolean> trail) implements Predicate<FireworkExplosion> {
      public static final Codec<ItemFireworkExplosionPredicate.FireworkPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(FireworkExplosion.Shape.CODEC.optionalFieldOf("shape").forGetter(ItemFireworkExplosionPredicate.FireworkPredicate::shape), Codec.BOOL.optionalFieldOf("has_twinkle").forGetter(ItemFireworkExplosionPredicate.FireworkPredicate::twinkle), Codec.BOOL.optionalFieldOf("has_trail").forGetter(ItemFireworkExplosionPredicate.FireworkPredicate::trail)).apply(var0, ItemFireworkExplosionPredicate.FireworkPredicate::new);
      });

      public FireworkPredicate(Optional<FireworkExplosion.Shape> param1, Optional<Boolean> param2, Optional<Boolean> param3) {
         super();
         this.shape = var1;
         this.twinkle = var2;
         this.trail = var3;
      }

      public boolean test(FireworkExplosion var1) {
         if (this.shape.isPresent() && this.shape.get() != var1.shape()) {
            return false;
         } else if (this.twinkle.isPresent() && (Boolean)this.twinkle.get() != var1.hasTwinkle()) {
            return false;
         } else {
            return !this.trail.isPresent() || (Boolean)this.trail.get() == var1.hasTrail();
         }
      }

      public Optional<FireworkExplosion.Shape> shape() {
         return this.shape;
      }

      public Optional<Boolean> twinkle() {
         return this.twinkle;
      }

      public Optional<Boolean> trail() {
         return this.trail;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((FireworkExplosion)var1);
      }
   }
}
