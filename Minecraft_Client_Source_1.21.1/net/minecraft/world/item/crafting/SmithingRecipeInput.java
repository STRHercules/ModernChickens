package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public record SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition) implements RecipeInput {
   public SmithingRecipeInput(ItemStack param1, ItemStack param2, ItemStack param3) {
      super();
      this.template = var1;
      this.base = var2;
      this.addition = var3;
   }

   public ItemStack getItem(int var1) {
      ItemStack var10000;
      switch(var1) {
      case 0:
         var10000 = this.template;
         break;
      case 1:
         var10000 = this.base;
         break;
      case 2:
         var10000 = this.addition;
         break;
      default:
         throw new IllegalArgumentException("Recipe does not contain slot " + var1);
      }

      return var10000;
   }

   public int size() {
      return 3;
   }

   public boolean isEmpty() {
      return this.template.isEmpty() && this.base.isEmpty() && this.addition.isEmpty();
   }

   public ItemStack template() {
      return this.template;
   }

   public ItemStack base() {
      return this.base;
   }

   public ItemStack addition() {
      return this.addition;
   }
}
