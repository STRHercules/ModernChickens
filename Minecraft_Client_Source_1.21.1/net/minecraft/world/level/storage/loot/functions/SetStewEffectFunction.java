package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetStewEffectFunction extends LootItemConditionalFunction {
   private static final Codec<List<SetStewEffectFunction.EffectEntry>> EFFECTS_LIST;
   public static final MapCodec<SetStewEffectFunction> CODEC;
   private final List<SetStewEffectFunction.EffectEntry> effects;

   SetStewEffectFunction(List<LootItemCondition> var1, List<SetStewEffectFunction.EffectEntry> var2) {
      super(var1);
      this.effects = var2;
   }

   public LootItemFunctionType<SetStewEffectFunction> getType() {
      return LootItemFunctions.SET_STEW_EFFECT;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return (Set)this.effects.stream().flatMap((var0) -> {
         return var0.duration().getReferencedContextParams().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack run(ItemStack var1, LootContext var2) {
      if (var1.is(Items.SUSPICIOUS_STEW) && !this.effects.isEmpty()) {
         SetStewEffectFunction.EffectEntry var3 = (SetStewEffectFunction.EffectEntry)Util.getRandom(this.effects, var2.getRandom());
         Holder var4 = var3.effect();
         int var5 = var3.duration().getInt(var2);
         if (!((MobEffect)var4.value()).isInstantenous()) {
            var5 *= 20;
         }

         SuspiciousStewEffects.Entry var6 = new SuspiciousStewEffects.Entry(var4, var5);
         var1.update(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY, var6, SuspiciousStewEffects::withEffectAdded);
         return var1;
      } else {
         return var1;
      }
   }

   public static SetStewEffectFunction.Builder stewEffect() {
      return new SetStewEffectFunction.Builder();
   }

   static {
      EFFECTS_LIST = SetStewEffectFunction.EffectEntry.CODEC.listOf().validate((var0) -> {
         ObjectOpenHashSet var1 = new ObjectOpenHashSet();
         Iterator var2 = var0.iterator();

         SetStewEffectFunction.EffectEntry var3;
         do {
            if (!var2.hasNext()) {
               return DataResult.success(var0);
            }

            var3 = (SetStewEffectFunction.EffectEntry)var2.next();
         } while(var1.add(var3.effect()));

         return DataResult.error(() -> {
            return "Encountered duplicate mob effect: '" + String.valueOf(var3.effect()) + "'";
         });
      });
      CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return commonFields(var0).and(EFFECTS_LIST.optionalFieldOf("effects", List.of()).forGetter((var0x) -> {
            return var0x.effects;
         })).apply(var0, SetStewEffectFunction::new);
      });
   }

   private static record EffectEntry(Holder<MobEffect> effect, NumberProvider duration) {
      public static final Codec<SetStewEffectFunction.EffectEntry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(MobEffect.CODEC.fieldOf("type").forGetter(SetStewEffectFunction.EffectEntry::effect), NumberProviders.CODEC.fieldOf("duration").forGetter(SetStewEffectFunction.EffectEntry::duration)).apply(var0, SetStewEffectFunction.EffectEntry::new);
      });

      EffectEntry(Holder<MobEffect> param1, NumberProvider param2) {
         super();
         this.effect = var1;
         this.duration = var2;
      }

      public Holder<MobEffect> effect() {
         return this.effect;
      }

      public NumberProvider duration() {
         return this.duration;
      }
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetStewEffectFunction.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<SetStewEffectFunction.EffectEntry> effects = ImmutableList.builder();

      public Builder() {
         super();
      }

      protected SetStewEffectFunction.Builder getThis() {
         return this;
      }

      public SetStewEffectFunction.Builder withEffect(Holder<MobEffect> var1, NumberProvider var2) {
         this.effects.add(new SetStewEffectFunction.EffectEntry(var1, var2));
         return this;
      }

      public LootItemFunction build() {
         return new SetStewEffectFunction(this.getConditions(), this.effects.build());
      }

      // $FF: synthetic method
      protected LootItemConditionalFunction.Builder getThis() {
         return this.getThis();
      }
   }
}
