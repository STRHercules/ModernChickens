package net.minecraft.world.damagesource;

import javax.annotation.Nullable;

public record CombatEntry(DamageSource source, float damage, @Nullable FallLocation fallLocation, float fallDistance) {
   public CombatEntry(DamageSource param1, float param2, @Nullable FallLocation param3, float param4) {
      super();
      this.source = var1;
      this.damage = var2;
      this.fallLocation = var3;
      this.fallDistance = var4;
   }

   public DamageSource source() {
      return this.source;
   }

   public float damage() {
      return this.damage;
   }

   @Nullable
   public FallLocation fallLocation() {
      return this.fallLocation;
   }

   public float fallDistance() {
      return this.fallDistance;
   }
}
