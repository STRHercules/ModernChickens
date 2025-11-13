package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsAmbientOcclusionFix extends DataFix {
   public OptionsAmbientOcclusionFix(Schema var1) {
      super(var1, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("OptionsAmbientOcclusionFix", this.getInputSchema().getType(References.OPTIONS), (var0) -> {
         return var0.update(DSL.remainderFinder(), (var0x) -> {
            return (Dynamic)DataFixUtils.orElse(var0x.get("ao").asString().map((var1) -> {
               return var0x.set("ao", var0x.createString(updateValue(var1)));
            }).result(), var0x);
         });
      });
   }

   private static String updateValue(String var0) {
      byte var2 = -1;
      switch(var0.hashCode()) {
      case 48:
         if (var0.equals("0")) {
            var2 = 0;
         }
         break;
      case 49:
         if (var0.equals("1")) {
            var2 = 1;
         }
         break;
      case 50:
         if (var0.equals("2")) {
            var2 = 2;
         }
      }

      String var10000;
      switch(var2) {
      case 0:
         var10000 = "false";
         break;
      case 1:
      case 2:
         var10000 = "true";
         break;
      default:
         var10000 = var0;
      }

      return var10000;
   }
}
