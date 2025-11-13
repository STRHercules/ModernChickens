package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentTypeInfo<StringArgumentType, StringArgumentSerializer.Template> {
   public StringArgumentSerializer() {
      super();
   }

   public void serializeToNetwork(StringArgumentSerializer.Template var1, FriendlyByteBuf var2) {
      var2.writeEnum(var1.type);
   }

   public StringArgumentSerializer.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      StringType var2 = (StringType)var1.readEnum(StringType.class);
      return new StringArgumentSerializer.Template(var2);
   }

   public void serializeToJson(StringArgumentSerializer.Template var1, JsonObject var2) {
      String var10002;
      switch(var1.type) {
      case SINGLE_WORD:
         var10002 = "word";
         break;
      case QUOTABLE_PHRASE:
         var10002 = "phrase";
         break;
      case GREEDY_PHRASE:
         var10002 = "greedy";
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      var2.addProperty("type", var10002);
   }

   public StringArgumentSerializer.Template unpack(StringArgumentType var1) {
      return new StringArgumentSerializer.Template(var1.getType());
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template deserializeFromNetwork(final FriendlyByteBuf param1) {
      return this.deserializeFromNetwork(var1);
   }

   public final class Template implements ArgumentTypeInfo.Template<StringArgumentType> {
      final StringType type;

      public Template(final StringType param2) {
         super();
         this.type = var2;
      }

      public StringArgumentType instantiate(CommandBuildContext var1) {
         StringArgumentType var10000;
         switch(this.type) {
         case SINGLE_WORD:
            var10000 = StringArgumentType.word();
            break;
         case QUOTABLE_PHRASE:
            var10000 = StringArgumentType.string();
            break;
         case GREEDY_PHRASE:
            var10000 = StringArgumentType.greedyString();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public ArgumentTypeInfo<StringArgumentType, ?> type() {
         return StringArgumentSerializer.this;
      }

      // $FF: synthetic method
      public ArgumentType instantiate(final CommandBuildContext param1) {
         return this.instantiate(var1);
      }
   }
}
