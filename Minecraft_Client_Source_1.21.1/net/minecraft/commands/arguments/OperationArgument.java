package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.ScoreAccess;

public class OperationArgument implements ArgumentType<OperationArgument.Operation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
   private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(Component.translatable("arguments.operation.invalid"));
   private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

   public OperationArgument() {
      super();
   }

   public static OperationArgument operation() {
      return new OperationArgument();
   }

   public static OperationArgument.Operation getOperation(CommandContext<CommandSourceStack> var0, String var1) {
      return (OperationArgument.Operation)var0.getArgument(var1, OperationArgument.Operation.class);
   }

   public OperationArgument.Operation parse(StringReader var1) throws CommandSyntaxException {
      if (!var1.canRead()) {
         throw ERROR_INVALID_OPERATION.createWithContext(var1);
      } else {
         int var2 = var1.getCursor();

         while(var1.canRead() && var1.peek() != ' ') {
            var1.skip();
         }

         return getOperation(var1.getString().substring(var2, var1.getCursor()));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
      return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, var2);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static OperationArgument.Operation getOperation(String var0) throws CommandSyntaxException {
      return (OperationArgument.Operation)(var0.equals("><") ? (var0x, var1) -> {
         int var2 = var0x.get();
         var0x.set(var1.get());
         var1.set(var2);
      } : getSimpleOperation(var0));
   }

   private static OperationArgument.SimpleOperation getSimpleOperation(String var0) throws CommandSyntaxException {
      byte var2 = -1;
      switch(var0.hashCode()) {
      case 60:
         if (var0.equals("<")) {
            var2 = 6;
         }
         break;
      case 61:
         if (var0.equals("=")) {
            var2 = 0;
         }
         break;
      case 62:
         if (var0.equals(">")) {
            var2 = 7;
         }
         break;
      case 1208:
         if (var0.equals("%=")) {
            var2 = 5;
         }
         break;
      case 1363:
         if (var0.equals("*=")) {
            var2 = 3;
         }
         break;
      case 1394:
         if (var0.equals("+=")) {
            var2 = 1;
         }
         break;
      case 1456:
         if (var0.equals("-=")) {
            var2 = 2;
         }
         break;
      case 1518:
         if (var0.equals("/=")) {
            var2 = 4;
         }
      }

      OperationArgument.SimpleOperation var10000;
      switch(var2) {
      case 0:
         var10000 = (var0x, var1) -> {
            return var1;
         };
         break;
      case 1:
         var10000 = Integer::sum;
         break;
      case 2:
         var10000 = (var0x, var1) -> {
            return var0x - var1;
         };
         break;
      case 3:
         var10000 = (var0x, var1) -> {
            return var0x * var1;
         };
         break;
      case 4:
         var10000 = (var0x, var1) -> {
            if (var1 == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.floorDiv(var0x, var1);
            }
         };
         break;
      case 5:
         var10000 = (var0x, var1) -> {
            if (var1 == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.positiveModulo(var0x, var1);
            }
         };
         break;
      case 6:
         var10000 = Math::min;
         break;
      case 7:
         var10000 = Math::max;
         break;
      default:
         throw ERROR_INVALID_OPERATION.create();
      }

      return var10000;
   }

   // $FF: synthetic method
   public Object parse(final StringReader param1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   @FunctionalInterface
   public interface Operation {
      void apply(ScoreAccess var1, ScoreAccess var2) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface SimpleOperation extends OperationArgument.Operation {
      int apply(int var1, int var2) throws CommandSyntaxException;

      default void apply(ScoreAccess var1, ScoreAccess var2) throws CommandSyntaxException {
         var1.set(this.apply(var1.get(), var2.get()));
      }
   }
}
