package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers, boolean showInTooltip) {
   public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of(), true);
   private static final Codec<ItemAttributeModifiers> FULL_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ItemAttributeModifiers.Entry.CODEC.listOf().fieldOf("modifiers").forGetter(ItemAttributeModifiers::modifiers), Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(ItemAttributeModifiers::showInTooltip)).apply(var0, ItemAttributeModifiers::new);
   });
   public static final Codec<ItemAttributeModifiers> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC;
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT;

   public ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> param1, boolean param2) {
      super();
      this.modifiers = var1;
      this.showInTooltip = var2;
   }

   public ItemAttributeModifiers withTooltip(boolean var1) {
      return new ItemAttributeModifiers(this.modifiers, var1);
   }

   public static ItemAttributeModifiers.Builder builder() {
      return new ItemAttributeModifiers.Builder();
   }

   public ItemAttributeModifiers withModifierAdded(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3) {
      com.google.common.collect.ImmutableList.Builder var4 = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);
      Iterator var5 = this.modifiers.iterator();

      while(var5.hasNext()) {
         ItemAttributeModifiers.Entry var6 = (ItemAttributeModifiers.Entry)var5.next();
         if (!var6.matches(var1, var2.id())) {
            var4.add(var6);
         }
      }

      var4.add(new ItemAttributeModifiers.Entry(var1, var2, var3));
      return new ItemAttributeModifiers(var4.build(), this.showInTooltip);
   }

   public void forEach(EquipmentSlotGroup var1, BiConsumer<Holder<Attribute>, AttributeModifier> var2) {
      Iterator var3 = this.modifiers.iterator();

      while(var3.hasNext()) {
         ItemAttributeModifiers.Entry var4 = (ItemAttributeModifiers.Entry)var3.next();
         if (var4.slot.equals(var1)) {
            var2.accept(var4.attribute, var4.modifier);
         }
      }

   }

   public void forEach(EquipmentSlot var1, BiConsumer<Holder<Attribute>, AttributeModifier> var2) {
      Iterator var3 = this.modifiers.iterator();

      while(var3.hasNext()) {
         ItemAttributeModifiers.Entry var4 = (ItemAttributeModifiers.Entry)var3.next();
         if (var4.slot.test(var1)) {
            var2.accept(var4.attribute, var4.modifier);
         }
      }

   }

   public double compute(double var1, EquipmentSlot var3) {
      double var4 = var1;
      Iterator var6 = this.modifiers.iterator();

      while(var6.hasNext()) {
         ItemAttributeModifiers.Entry var7 = (ItemAttributeModifiers.Entry)var6.next();
         if (var7.slot.test(var3)) {
            double var8 = var7.modifier.amount();
            double var10001;
            switch(var7.modifier.operation()) {
            case ADD_VALUE:
               var10001 = var8;
               break;
            case ADD_MULTIPLIED_BASE:
               var10001 = var8 * var1;
               break;
            case ADD_MULTIPLIED_TOTAL:
               var10001 = var8 * var4;
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
            }

            var4 += var10001;
         }
      }

      return var4;
   }

   public List<ItemAttributeModifiers.Entry> modifiers() {
      return this.modifiers;
   }

   public boolean showInTooltip() {
      return this.showInTooltip;
   }

   static {
      CODEC = Codec.withAlternative(FULL_CODEC, ItemAttributeModifiers.Entry.CODEC.listOf(), (var0) -> {
         return new ItemAttributeModifiers(var0, true);
      });
      STREAM_CODEC = StreamCodec.composite(ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ByteBufCodecs.BOOL, ItemAttributeModifiers::showInTooltip, ItemAttributeModifiers::new);
      ATTRIBUTE_MODIFIER_FORMAT = (DecimalFormat)Util.make(new DecimalFormat("#.##"), (var0) -> {
         var0.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      });
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      Builder() {
         super();
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3) {
         this.entries.add(new ItemAttributeModifiers.Entry(var1, var2, var3));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build(), true);
      }
   }

   public static record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
      final Holder<Attribute> attribute;
      final AttributeModifier modifier;
      final EquipmentSlotGroup slot;
      public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute), AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier), EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot)).apply(var0, ItemAttributeModifiers.Entry::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC;

      public Entry(Holder<Attribute> param1, AttributeModifier param2, EquipmentSlotGroup param3) {
         super();
         this.attribute = var1;
         this.modifier = var2;
         this.slot = var3;
      }

      public boolean matches(Holder<Attribute> var1, ResourceLocation var2) {
         return var1.equals(this.attribute) && this.modifier.is(var2);
      }

      public Holder<Attribute> attribute() {
         return this.attribute;
      }

      public AttributeModifier modifier() {
         return this.modifier;
      }

      public EquipmentSlotGroup slot() {
         return this.slot;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(Attribute.STREAM_CODEC, ItemAttributeModifiers.Entry::attribute, AttributeModifier.STREAM_CODEC, ItemAttributeModifiers.Entry::modifier, EquipmentSlotGroup.STREAM_CODEC, ItemAttributeModifiers.Entry::slot, ItemAttributeModifiers.Entry::new);
      }
   }
}
