package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ItemAttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, ItemAttributeModifiersPredicate.EntryPredicate>> modifiers) implements SingleComponentItemPredicate<ItemAttributeModifiers> {
   public static final Codec<ItemAttributeModifiersPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CollectionPredicate.codec(ItemAttributeModifiersPredicate.EntryPredicate.CODEC).optionalFieldOf("modifiers").forGetter(ItemAttributeModifiersPredicate::modifiers)).apply(var0, ItemAttributeModifiersPredicate::new);
   });

   public ItemAttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, ItemAttributeModifiersPredicate.EntryPredicate>> param1) {
      super();
      this.modifiers = var1;
   }

   public DataComponentType<ItemAttributeModifiers> componentType() {
      return DataComponents.ATTRIBUTE_MODIFIERS;
   }

   public boolean matches(ItemStack var1, ItemAttributeModifiers var2) {
      return !this.modifiers.isPresent() || ((CollectionPredicate)this.modifiers.get()).test((Iterable)var2.modifiers());
   }

   public Optional<CollectionPredicate<ItemAttributeModifiers.Entry, ItemAttributeModifiersPredicate.EntryPredicate>> modifiers() {
      return this.modifiers;
   }

   public static record EntryPredicate(Optional<HolderSet<Attribute>> attribute, Optional<ResourceLocation> id, MinMaxBounds.Doubles amount, Optional<AttributeModifier.Operation> operation, Optional<EquipmentSlotGroup> slot) implements Predicate<ItemAttributeModifiers.Entry> {
      public static final Codec<ItemAttributeModifiersPredicate.EntryPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(RegistryCodecs.homogeneousList(Registries.ATTRIBUTE).optionalFieldOf("attribute").forGetter(ItemAttributeModifiersPredicate.EntryPredicate::attribute), ResourceLocation.CODEC.optionalFieldOf("id").forGetter(ItemAttributeModifiersPredicate.EntryPredicate::id), MinMaxBounds.Doubles.CODEC.optionalFieldOf("amount", MinMaxBounds.Doubles.ANY).forGetter(ItemAttributeModifiersPredicate.EntryPredicate::amount), AttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(ItemAttributeModifiersPredicate.EntryPredicate::operation), EquipmentSlotGroup.CODEC.optionalFieldOf("slot").forGetter(ItemAttributeModifiersPredicate.EntryPredicate::slot)).apply(var0, ItemAttributeModifiersPredicate.EntryPredicate::new);
      });

      public EntryPredicate(Optional<HolderSet<Attribute>> param1, Optional<ResourceLocation> param2, MinMaxBounds.Doubles param3, Optional<AttributeModifier.Operation> param4, Optional<EquipmentSlotGroup> param5) {
         super();
         this.attribute = var1;
         this.id = var2;
         this.amount = var3;
         this.operation = var4;
         this.slot = var5;
      }

      public boolean test(ItemAttributeModifiers.Entry var1) {
         if (this.attribute.isPresent() && !((HolderSet)this.attribute.get()).contains(var1.attribute())) {
            return false;
         } else if (this.id.isPresent() && !((ResourceLocation)this.id.get()).equals(var1.modifier().id())) {
            return false;
         } else if (!this.amount.matches(var1.modifier().amount())) {
            return false;
         } else if (this.operation.isPresent() && this.operation.get() != var1.modifier().operation()) {
            return false;
         } else {
            return !this.slot.isPresent() || this.slot.get() == var1.slot();
         }
      }

      public Optional<HolderSet<Attribute>> attribute() {
         return this.attribute;
      }

      public Optional<ResourceLocation> id() {
         return this.id;
      }

      public MinMaxBounds.Doubles amount() {
         return this.amount;
      }

      public Optional<AttributeModifier.Operation> operation() {
         return this.operation;
      }

      public Optional<EquipmentSlotGroup> slot() {
         return this.slot;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((ItemAttributeModifiers.Entry)var1);
      }
   }
}
