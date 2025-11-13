package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.slf4j.Logger;

public interface ListOperation {
   MapCodec<ListOperation> UNLIMITED_CODEC = codec(2147483647);

   static MapCodec<ListOperation> codec(int var0) {
      return ListOperation.Type.CODEC.dispatchMap("mode", ListOperation::mode, (var0x) -> {
         return var0x.mapCodec;
      }).validate((var1) -> {
         if (var1 instanceof ListOperation.ReplaceSection) {
            ListOperation.ReplaceSection var2 = (ListOperation.ReplaceSection)var1;
            if (var2.size().isPresent()) {
               int var3 = (Integer)var2.size().get();
               if (var3 > var0) {
                  return DataResult.error(() -> {
                     return "Size value too large: " + var3 + ", max size is " + var0;
                  });
               }
            }
         }

         return DataResult.success(var1);
      });
   }

   ListOperation.Type mode();

   default <T> List<T> apply(List<T> var1, List<T> var2) {
      return this.apply(var1, var2, 2147483647);
   }

   <T> List<T> apply(List<T> var1, List<T> var2, int var3);

   public static enum Type implements StringRepresentable {
      REPLACE_ALL("replace_all", ListOperation.ReplaceAll.MAP_CODEC),
      REPLACE_SECTION("replace_section", ListOperation.ReplaceSection.MAP_CODEC),
      INSERT("insert", ListOperation.Insert.MAP_CODEC),
      APPEND("append", ListOperation.Append.MAP_CODEC);

      public static final Codec<ListOperation.Type> CODEC = StringRepresentable.fromEnum(ListOperation.Type::values);
      private final String id;
      final MapCodec<? extends ListOperation> mapCodec;

      private Type(final String param3, final MapCodec<? extends ListOperation> param4) {
         this.id = var3;
         this.mapCodec = var4;
      }

      public MapCodec<? extends ListOperation> mapCodec() {
         return this.mapCodec;
      }

      public String getSerializedName() {
         return this.id;
      }

      // $FF: synthetic method
      private static ListOperation.Type[] $values() {
         return new ListOperation.Type[]{REPLACE_ALL, REPLACE_SECTION, INSERT, APPEND};
      }
   }

   public static record ReplaceSection(int offset, Optional<Integer> size) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.ReplaceSection> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.ReplaceSection::offset), ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("size").forGetter(ListOperation.ReplaceSection::size)).apply(var0, ListOperation.ReplaceSection::new);
      });

      public ReplaceSection(int var1) {
         this(var1, Optional.empty());
      }

      public ReplaceSection(int param1, Optional<Integer> param2) {
         super();
         this.offset = var1;
         this.size = var2;
      }

      public ListOperation.Type mode() {
         return ListOperation.Type.REPLACE_SECTION;
      }

      public <T> List<T> apply(List<T> var1, List<T> var2, int var3) {
         int var4 = var1.size();
         if (this.offset > var4) {
            LOGGER.error("Cannot replace when offset is out of bounds");
            return var1;
         } else {
            Builder var5 = ImmutableList.builder();
            var5.addAll(var1.subList(0, this.offset));
            var5.addAll(var2);
            int var6 = this.offset + (Integer)this.size.orElse(var2.size());
            if (var6 < var4) {
               var5.addAll(var1.subList(var6, var4));
            }

            ImmutableList var7 = var5.build();
            if (var7.size() > var3) {
               LOGGER.error("Contents overflow in section replacement");
               return var1;
            } else {
               return var7;
            }
         }
      }

      public int offset() {
         return this.offset;
      }

      public Optional<Integer> size() {
         return this.size;
      }
   }

   public static record StandAlone<T>(List<T> value, ListOperation operation) {
      public StandAlone(List<T> param1, ListOperation param2) {
         super();
         this.value = var1;
         this.operation = var2;
      }

      public static <T> Codec<ListOperation.StandAlone<T>> codec(Codec<T> var0, int var1) {
         return RecordCodecBuilder.create((var2) -> {
            return var2.group(var0.sizeLimitedListOf(var1).fieldOf("values").forGetter((var0x) -> {
               return var0x.value;
            }), ListOperation.codec(var1).forGetter((var0x) -> {
               return var0x.operation;
            })).apply(var2, ListOperation.StandAlone::new);
         });
      }

      public List<T> apply(List<T> var1) {
         return this.operation.apply(var1, this.value);
      }

      public List<T> value() {
         return this.value;
      }

      public ListOperation operation() {
         return this.operation;
      }
   }

   public static class Append implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final ListOperation.Append INSTANCE = new ListOperation.Append();
      public static final MapCodec<ListOperation.Append> MAP_CODEC = MapCodec.unit(() -> {
         return INSTANCE;
      });

      private Append() {
         super();
      }

      public ListOperation.Type mode() {
         return ListOperation.Type.APPEND;
      }

      public <T> List<T> apply(List<T> var1, List<T> var2, int var3) {
         if (var1.size() + var2.size() > var3) {
            LOGGER.error("Contents overflow in section append");
            return var1;
         } else {
            return Stream.concat(var1.stream(), var2.stream()).toList();
         }
      }
   }

   public static record Insert(int offset) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.Insert> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.Insert::offset)).apply(var0, ListOperation.Insert::new);
      });

      public Insert(int param1) {
         super();
         this.offset = var1;
      }

      public ListOperation.Type mode() {
         return ListOperation.Type.INSERT;
      }

      public <T> List<T> apply(List<T> var1, List<T> var2, int var3) {
         int var4 = var1.size();
         if (this.offset > var4) {
            LOGGER.error("Cannot insert when offset is out of bounds");
            return var1;
         } else if (var4 + var2.size() > var3) {
            LOGGER.error("Contents overflow in section insertion");
            return var1;
         } else {
            Builder var5 = ImmutableList.builder();
            var5.addAll(var1.subList(0, this.offset));
            var5.addAll(var2);
            var5.addAll(var1.subList(this.offset, var4));
            return var5.build();
         }
      }

      public int offset() {
         return this.offset;
      }
   }

   public static class ReplaceAll implements ListOperation {
      public static final ListOperation.ReplaceAll INSTANCE = new ListOperation.ReplaceAll();
      public static final MapCodec<ListOperation.ReplaceAll> MAP_CODEC = MapCodec.unit(() -> {
         return INSTANCE;
      });

      private ReplaceAll() {
         super();
      }

      public ListOperation.Type mode() {
         return ListOperation.Type.REPLACE_ALL;
      }

      public <T> List<T> apply(List<T> var1, List<T> var2, int var3) {
         return var2;
      }
   }
}
