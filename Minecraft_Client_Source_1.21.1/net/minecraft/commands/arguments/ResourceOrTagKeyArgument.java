package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ResourceOrTagKeyArgument<T> implements ArgumentType<ResourceOrTagKeyArgument.Result<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
   final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceOrTagKeyArgument(ResourceKey<? extends Registry<T>> var1) {
      super();
      this.registryKey = var1;
   }

   public static <T> ResourceOrTagKeyArgument<T> resourceOrTagKey(ResourceKey<? extends Registry<T>> var0) {
      return new ResourceOrTagKeyArgument(var0);
   }

   public static <T> ResourceOrTagKeyArgument.Result<T> getResourceOrTagKey(CommandContext<CommandSourceStack> var0, String var1, ResourceKey<Registry<T>> var2, DynamicCommandExceptionType var3) throws CommandSyntaxException {
      ResourceOrTagKeyArgument.Result var4 = (ResourceOrTagKeyArgument.Result)var0.getArgument(var1, ResourceOrTagKeyArgument.Result.class);
      Optional var5 = var4.cast(var2);
      return (ResourceOrTagKeyArgument.Result)var5.orElseThrow(() -> {
         return var3.create(var4);
      });
   }

   public ResourceOrTagKeyArgument.Result<T> parse(StringReader var1) throws CommandSyntaxException {
      if (var1.canRead() && var1.peek() == '#') {
         int var5 = var1.getCursor();

         try {
            var1.skip();
            ResourceLocation var3 = ResourceLocation.read(var1);
            return new ResourceOrTagKeyArgument.TagResult(TagKey.create(this.registryKey, var3));
         } catch (CommandSyntaxException var4) {
            var1.setCursor(var5);
            throw var4;
         }
      } else {
         ResourceLocation var2 = ResourceLocation.read(var1);
         return new ResourceOrTagKeyArgument.ResourceResult(ResourceKey.create(this.registryKey, var2));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
      Object var4 = var1.getSource();
      if (var4 instanceof SharedSuggestionProvider) {
         SharedSuggestionProvider var3 = (SharedSuggestionProvider)var4;
         return var3.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL, var2, var1);
      } else {
         return var2.buildFuture();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   // $FF: synthetic method
   public Object parse(final StringReader param1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   public interface Result<T> extends Predicate<Holder<T>> {
      Either<ResourceKey<T>, TagKey<T>> unwrap();

      <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> var1);

      String asPrintable();
   }

   static record TagResult<T>(TagKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      TagResult(TagKey<T> param1) {
         super();
         this.key = var1;
      }

      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.right(this.key);
      }

      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> var1) {
         return this.key.cast(var1).map(ResourceOrTagKeyArgument.TagResult::new);
      }

      public boolean test(Holder<T> var1) {
         return var1.is(this.key);
      }

      public String asPrintable() {
         return "#" + String.valueOf(this.key.location());
      }

      public TagKey<T> key() {
         return this.key;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((Holder)var1);
      }
   }

   private static record ResourceResult<T>(ResourceKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      ResourceResult(ResourceKey<T> param1) {
         super();
         this.key = var1;
      }

      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.left(this.key);
      }

      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> var1) {
         return this.key.cast(var1).map(ResourceOrTagKeyArgument.ResourceResult::new);
      }

      public boolean test(Holder<T> var1) {
         return var1.is(this.key);
      }

      public String asPrintable() {
         return this.key.location().toString();
      }

      public ResourceKey<T> key() {
         return this.key;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((Holder)var1);
      }
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ResourceOrTagKeyArgument.Info<T>.Template> {
      public Info() {
         super();
      }

      public void serializeToNetwork(ResourceOrTagKeyArgument.Info<T>.Template var1, FriendlyByteBuf var2) {
         var2.writeResourceKey(var1.registryKey);
      }

      public ResourceOrTagKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf var1) {
         return new ResourceOrTagKeyArgument.Info.Template(var1.readRegistryKey());
      }

      public void serializeToJson(ResourceOrTagKeyArgument.Info<T>.Template var1, JsonObject var2) {
         var2.addProperty("registry", var1.registryKey.location().toString());
      }

      public ResourceOrTagKeyArgument.Info<T>.Template unpack(ResourceOrTagKeyArgument<T> var1) {
         return new ResourceOrTagKeyArgument.Info.Template(var1.registryKey);
      }

      // $FF: synthetic method
      public ArgumentTypeInfo.Template deserializeFromNetwork(final FriendlyByteBuf param1) {
         return this.deserializeFromNetwork(var1);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagKeyArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(final ResourceKey<? extends Registry<T>> param2) {
            super();
            this.registryKey = var2;
         }

         public ResourceOrTagKeyArgument<T> instantiate(CommandBuildContext var1) {
            return new ResourceOrTagKeyArgument(this.registryKey);
         }

         public ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ?> type() {
            return Info.this;
         }

         // $FF: synthetic method
         public ArgumentType instantiate(final CommandBuildContext param1) {
            return this.instantiate(var1);
         }
      }
   }
}
