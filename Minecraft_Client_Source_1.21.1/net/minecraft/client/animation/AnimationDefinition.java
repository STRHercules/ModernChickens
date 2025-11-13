package net.minecraft.client.animation;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AnimationDefinition(float lengthInSeconds, boolean looping, Map<String, List<AnimationChannel>> boneAnimations) {
   public AnimationDefinition(float param1, boolean param2, Map<String, List<AnimationChannel>> param3) {
      super();
      this.lengthInSeconds = var1;
      this.looping = var2;
      this.boneAnimations = var3;
   }

   public float lengthInSeconds() {
      return this.lengthInSeconds;
   }

   public boolean looping() {
      return this.looping;
   }

   public Map<String, List<AnimationChannel>> boneAnimations() {
      return this.boneAnimations;
   }

   public static class Builder {
      private final float length;
      private final Map<String, List<AnimationChannel>> animationByBone = Maps.newHashMap();
      private boolean looping;

      public static AnimationDefinition.Builder withLength(float var0) {
         return new AnimationDefinition.Builder(var0);
      }

      private Builder(float var1) {
         super();
         this.length = var1;
      }

      public AnimationDefinition.Builder looping() {
         this.looping = true;
         return this;
      }

      public AnimationDefinition.Builder addAnimation(String var1, AnimationChannel var2) {
         ((List)this.animationByBone.computeIfAbsent(var1, (var0) -> {
            return new ArrayList();
         })).add(var2);
         return this;
      }

      public AnimationDefinition build() {
         return new AnimationDefinition(this.length, this.looping, this.animationByBone);
      }
   }
}
