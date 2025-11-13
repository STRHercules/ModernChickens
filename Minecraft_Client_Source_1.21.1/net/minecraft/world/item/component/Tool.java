package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record Tool(List<Tool.Rule> rules, float defaultMiningSpeed, int damagePerBlock) {
   public static final Codec<Tool> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Tool.Rule.CODEC.listOf().fieldOf("rules").forGetter(Tool::rules), Codec.FLOAT.optionalFieldOf("default_mining_speed", 1.0F).forGetter(Tool::defaultMiningSpeed), ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage_per_block", 1).forGetter(Tool::damagePerBlock)).apply(var0, Tool::new);
   });
   public static final StreamCodec<RegistryFriendlyByteBuf, Tool> STREAM_CODEC;

   public Tool(List<Tool.Rule> param1, float param2, int param3) {
      super();
      this.rules = var1;
      this.defaultMiningSpeed = var2;
      this.damagePerBlock = var3;
   }

   public float getMiningSpeed(BlockState var1) {
      Iterator var2 = this.rules.iterator();

      Tool.Rule var3;
      do {
         if (!var2.hasNext()) {
            return this.defaultMiningSpeed;
         }

         var3 = (Tool.Rule)var2.next();
      } while(!var3.speed.isPresent() || !var1.is(var3.blocks));

      return (Float)var3.speed.get();
   }

   public boolean isCorrectForDrops(BlockState var1) {
      Iterator var2 = this.rules.iterator();

      Tool.Rule var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (Tool.Rule)var2.next();
      } while(!var3.correctForDrops.isPresent() || !var1.is(var3.blocks));

      return (Boolean)var3.correctForDrops.get();
   }

   public List<Tool.Rule> rules() {
      return this.rules;
   }

   public float defaultMiningSpeed() {
      return this.defaultMiningSpeed;
   }

   public int damagePerBlock() {
      return this.damagePerBlock;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(Tool.Rule.STREAM_CODEC.apply(ByteBufCodecs.list()), Tool::rules, ByteBufCodecs.FLOAT, Tool::defaultMiningSpeed, ByteBufCodecs.VAR_INT, Tool::damagePerBlock, Tool::new);
   }

   public static record Rule(HolderSet<Block> blocks, Optional<Float> speed, Optional<Boolean> correctForDrops) {
      final HolderSet<Block> blocks;
      final Optional<Float> speed;
      final Optional<Boolean> correctForDrops;
      public static final Codec<Tool.Rule> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(Tool.Rule::blocks), ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("speed").forGetter(Tool.Rule::speed), Codec.BOOL.optionalFieldOf("correct_for_drops").forGetter(Tool.Rule::correctForDrops)).apply(var0, Tool.Rule::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, Tool.Rule> STREAM_CODEC;

      public Rule(HolderSet<Block> param1, Optional<Float> param2, Optional<Boolean> param3) {
         super();
         this.blocks = var1;
         this.speed = var2;
         this.correctForDrops = var3;
      }

      public static Tool.Rule minesAndDrops(List<Block> var0, float var1) {
         return forBlocks(var0, Optional.of(var1), Optional.of(true));
      }

      public static Tool.Rule minesAndDrops(TagKey<Block> var0, float var1) {
         return forTag(var0, Optional.of(var1), Optional.of(true));
      }

      public static Tool.Rule deniesDrops(TagKey<Block> var0) {
         return forTag(var0, Optional.empty(), Optional.of(false));
      }

      public static Tool.Rule overrideSpeed(TagKey<Block> var0, float var1) {
         return forTag(var0, Optional.of(var1), Optional.empty());
      }

      public static Tool.Rule overrideSpeed(List<Block> var0, float var1) {
         return forBlocks(var0, Optional.of(var1), Optional.empty());
      }

      private static Tool.Rule forTag(TagKey<Block> var0, Optional<Float> var1, Optional<Boolean> var2) {
         return new Tool.Rule(BuiltInRegistries.BLOCK.getOrCreateTag(var0), var1, var2);
      }

      private static Tool.Rule forBlocks(List<Block> var0, Optional<Float> var1, Optional<Boolean> var2) {
         return new Tool.Rule(HolderSet.direct((List)var0.stream().map(Block::builtInRegistryHolder).collect(Collectors.toList())), var1, var2);
      }

      public HolderSet<Block> blocks() {
         return this.blocks;
      }

      public Optional<Float> speed() {
         return this.speed;
      }

      public Optional<Boolean> correctForDrops() {
         return this.correctForDrops;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.holderSet(Registries.BLOCK), Tool.Rule::blocks, ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional), Tool.Rule::speed, ByteBufCodecs.BOOL.apply(ByteBufCodecs::optional), Tool.Rule::correctForDrops, Tool.Rule::new);
      }
   }
}
