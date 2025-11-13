package net.minecraft.client.resources;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkin(ResourceLocation texture, @Nullable String textureUrl, @Nullable ResourceLocation capeTexture, @Nullable ResourceLocation elytraTexture, PlayerSkin.Model model, boolean secure) {
   public PlayerSkin(ResourceLocation param1, @Nullable String param2, @Nullable ResourceLocation param3, @Nullable ResourceLocation param4, PlayerSkin.Model param5, boolean param6) {
      super();
      this.texture = var1;
      this.textureUrl = var2;
      this.capeTexture = var3;
      this.elytraTexture = var4;
      this.model = var5;
      this.secure = var6;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   @Nullable
   public String textureUrl() {
      return this.textureUrl;
   }

   @Nullable
   public ResourceLocation capeTexture() {
      return this.capeTexture;
   }

   @Nullable
   public ResourceLocation elytraTexture() {
      return this.elytraTexture;
   }

   public PlayerSkin.Model model() {
      return this.model;
   }

   public boolean secure() {
      return this.secure;
   }

   public static enum Model {
      SLIM("slim"),
      WIDE("default");

      private final String id;

      private Model(final String param3) {
         this.id = var3;
      }

      public static PlayerSkin.Model byName(@Nullable String var0) {
         if (var0 == null) {
            return WIDE;
         } else {
            byte var2 = -1;
            switch(var0.hashCode()) {
            case 3533117:
               if (var0.equals("slim")) {
                  var2 = 0;
               }
            default:
               PlayerSkin.Model var10000;
               switch(var2) {
               case 0:
                  var10000 = SLIM;
                  break;
               default:
                  var10000 = WIDE;
               }

               return var10000;
            }
         }
      }

      public String id() {
         return this.id;
      }

      // $FF: synthetic method
      private static PlayerSkin.Model[] $values() {
         return new PlayerSkin.Model[]{SLIM, WIDE};
      }
   }
}
