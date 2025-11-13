package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();
   private static final String WRAPPER_MARKER = "";

   protected NbtOps() {
      super();
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public <U> U convertTo(DynamicOps<U> var1, Tag var2) {
      Object var10000;
      switch(var2.getId()) {
      case 0:
         var10000 = (Object)var1.empty();
         break;
      case 1:
         var10000 = (Object)var1.createByte(((NumericTag)var2).getAsByte());
         break;
      case 2:
         var10000 = (Object)var1.createShort(((NumericTag)var2).getAsShort());
         break;
      case 3:
         var10000 = (Object)var1.createInt(((NumericTag)var2).getAsInt());
         break;
      case 4:
         var10000 = (Object)var1.createLong(((NumericTag)var2).getAsLong());
         break;
      case 5:
         var10000 = (Object)var1.createFloat(((NumericTag)var2).getAsFloat());
         break;
      case 6:
         var10000 = (Object)var1.createDouble(((NumericTag)var2).getAsDouble());
         break;
      case 7:
         var10000 = (Object)var1.createByteList(ByteBuffer.wrap(((ByteArrayTag)var2).getAsByteArray()));
         break;
      case 8:
         var10000 = (Object)var1.createString(var2.getAsString());
         break;
      case 9:
         var10000 = (Object)this.convertList(var1, var2);
         break;
      case 10:
         var10000 = (Object)this.convertMap(var1, var2);
         break;
      case 11:
         var10000 = (Object)var1.createIntList(Arrays.stream(((IntArrayTag)var2).getAsIntArray()));
         break;
      case 12:
         var10000 = (Object)var1.createLongList(Arrays.stream(((LongArrayTag)var2).getAsLongArray()));
         break;
      default:
         throw new IllegalStateException("Unknown tag type: " + String.valueOf(var2));
      }

      return var10000;
   }

   public DataResult<Number> getNumberValue(Tag var1) {
      if (var1 instanceof NumericTag) {
         NumericTag var2 = (NumericTag)var1;
         return DataResult.success(var2.getAsNumber());
      } else {
         return DataResult.error(() -> {
            return "Not a number";
         });
      }
   }

   public Tag createNumeric(Number var1) {
      return DoubleTag.valueOf(var1.doubleValue());
   }

   public Tag createByte(byte var1) {
      return ByteTag.valueOf(var1);
   }

   public Tag createShort(short var1) {
      return ShortTag.valueOf(var1);
   }

   public Tag createInt(int var1) {
      return IntTag.valueOf(var1);
   }

   public Tag createLong(long var1) {
      return LongTag.valueOf(var1);
   }

   public Tag createFloat(float var1) {
      return FloatTag.valueOf(var1);
   }

   public Tag createDouble(double var1) {
      return DoubleTag.valueOf(var1);
   }

   public Tag createBoolean(boolean var1) {
      return ByteTag.valueOf(var1);
   }

   public DataResult<String> getStringValue(Tag var1) {
      if (var1 instanceof StringTag) {
         StringTag var2 = (StringTag)var1;
         return DataResult.success(var2.getAsString());
      } else {
         return DataResult.error(() -> {
            return "Not a string";
         });
      }
   }

   public Tag createString(String var1) {
      return StringTag.valueOf(var1);
   }

   public DataResult<Tag> mergeToList(Tag var1, Tag var2) {
      return (DataResult)createCollector(var1).map((var1x) -> {
         return DataResult.success(var1x.accept(var2).result());
      }).orElseGet(() -> {
         return DataResult.error(() -> {
            return "mergeToList called with not a list: " + String.valueOf(var1);
         }, var1);
      });
   }

   public DataResult<Tag> mergeToList(Tag var1, List<Tag> var2) {
      return (DataResult)createCollector(var1).map((var1x) -> {
         return DataResult.success(var1x.acceptAll((Iterable)var2).result());
      }).orElseGet(() -> {
         return DataResult.error(() -> {
            return "mergeToList called with not a list: " + String.valueOf(var1);
         }, var1);
      });
   }

   public DataResult<Tag> mergeToMap(Tag var1, Tag var2, Tag var3) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else if (!(var2 instanceof StringTag)) {
         return DataResult.error(() -> {
            return "key is not a string: " + String.valueOf(var2);
         }, var1);
      } else {
         CompoundTag var10000;
         if (var1 instanceof CompoundTag) {
            CompoundTag var5 = (CompoundTag)var1;
            var10000 = var5.shallowCopy();
         } else {
            var10000 = new CompoundTag();
         }

         CompoundTag var4 = var10000;
         var4.put(var2.getAsString(), var3);
         return DataResult.success(var4);
      }
   }

   public DataResult<Tag> mergeToMap(Tag var1, MapLike<Tag> var2) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else {
         CompoundTag var10000;
         if (var1 instanceof CompoundTag) {
            CompoundTag var4 = (CompoundTag)var1;
            var10000 = var4.shallowCopy();
         } else {
            var10000 = new CompoundTag();
         }

         CompoundTag var3 = var10000;
         ArrayList var5 = new ArrayList();
         var2.entries().forEach((var2x) -> {
            Tag var3x = (Tag)var2x.getFirst();
            if (!(var3x instanceof StringTag)) {
               var5.add(var3x);
            } else {
               var3.put(var3x.getAsString(), (Tag)var2x.getSecond());
            }
         });
         return !var5.isEmpty() ? DataResult.error(() -> {
            return "some keys are not strings: " + String.valueOf(var5);
         }, var3) : DataResult.success(var3);
      }
   }

   public DataResult<Tag> mergeToMap(Tag var1, Map<Tag, Tag> var2) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else {
         CompoundTag var10000;
         if (var1 instanceof CompoundTag) {
            CompoundTag var4 = (CompoundTag)var1;
            var10000 = var4.shallowCopy();
         } else {
            var10000 = new CompoundTag();
         }

         CompoundTag var3 = var10000;
         ArrayList var8 = new ArrayList();
         Iterator var5 = var2.entrySet().iterator();

         while(var5.hasNext()) {
            Entry var6 = (Entry)var5.next();
            Tag var7 = (Tag)var6.getKey();
            if (var7 instanceof StringTag) {
               var3.put(var7.getAsString(), (Tag)var6.getValue());
            } else {
               var8.add(var7);
            }
         }

         if (!var8.isEmpty()) {
            return DataResult.error(() -> {
               return "some keys are not strings: " + String.valueOf(var8);
            }, var3);
         } else {
            return DataResult.success(var3);
         }
      }
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag var1) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success(var2.entrySet().stream().map((var1x) -> {
            return Pair.of(this.createString((String)var1x.getKey()), (Tag)var1x.getValue());
         }));
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag var1) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success((var2x) -> {
            Iterator var3 = var2.entrySet().iterator();

            while(var3.hasNext()) {
               Entry var4 = (Entry)var3.next();
               var2x.accept(this.createString((String)var4.getKey()), (Tag)var4.getValue());
            }

         });
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<MapLike<Tag>> getMap(Tag var1) {
      if (var1 instanceof CompoundTag) {
         final CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success(new MapLike<Tag>() {
            @Nullable
            public Tag get(Tag var1) {
               return var2.get(var1.getAsString());
            }

            @Nullable
            public Tag get(String var1) {
               return var2.get(var1);
            }

            public Stream<Pair<Tag, Tag>> entries() {
               return var2.entrySet().stream().map((var1) -> {
                  return Pair.of(NbtOps.this.createString((String)var1.getKey()), (Tag)var1.getValue());
               });
            }

            public String toString() {
               return "MapLike[" + String.valueOf(var2) + "]";
            }

            // $FF: synthetic method
            @Nullable
            public Object get(final String param1) {
               return this.get(var1);
            }

            // $FF: synthetic method
            @Nullable
            public Object get(final Object param1) {
               return this.get((Tag)var1);
            }
         });
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> var1) {
      CompoundTag var2 = new CompoundTag();
      var1.forEach((var1x) -> {
         var2.put(((Tag)var1x.getFirst()).getAsString(), (Tag)var1x.getSecond());
      });
      return var2;
   }

   private static Tag tryUnwrap(CompoundTag var0) {
      if (var0.size() == 1) {
         Tag var1 = var0.get("");
         if (var1 != null) {
            return var1;
         }
      }

      return var0;
   }

   public DataResult<Stream<Tag>> getStream(Tag var1) {
      if (var1 instanceof ListTag) {
         ListTag var3 = (ListTag)var1;
         return var3.getElementType() == 10 ? DataResult.success(var3.stream().map((var0) -> {
            return tryUnwrap((CompoundTag)var0);
         })) : DataResult.success(var3.stream());
      } else if (var1 instanceof CollectionTag) {
         CollectionTag var2 = (CollectionTag)var1;
         return DataResult.success(var2.stream().map((var0) -> {
            return var0;
         }));
      } else {
         return DataResult.error(() -> {
            return "Not a list";
         });
      }
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag var1) {
      if (var1 instanceof ListTag) {
         ListTag var3 = (ListTag)var1;
         if (var3.getElementType() == 10) {
            return DataResult.success((var1x) -> {
               Iterator var2 = var3.iterator();

               while(var2.hasNext()) {
                  Tag var3x = (Tag)var2.next();
                  var1x.accept(tryUnwrap((CompoundTag)var3x));
               }

            });
         } else {
            Objects.requireNonNull(var3);
            return DataResult.success(var3::forEach);
         }
      } else if (var1 instanceof CollectionTag) {
         CollectionTag var2 = (CollectionTag)var1;
         Objects.requireNonNull(var2);
         return DataResult.success(var2::forEach);
      } else {
         return DataResult.error(() -> {
            return "Not a list: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag var1) {
      if (var1 instanceof ByteArrayTag) {
         ByteArrayTag var2 = (ByteArrayTag)var1;
         return DataResult.success(ByteBuffer.wrap(var2.getAsByteArray()));
      } else {
         return super.getByteBuffer(var1);
      }
   }

   public Tag createByteList(ByteBuffer var1) {
      ByteBuffer var2 = var1.duplicate().clear();
      byte[] var3 = new byte[var1.capacity()];
      var2.get(0, var3, 0, var3.length);
      return new ByteArrayTag(var3);
   }

   public DataResult<IntStream> getIntStream(Tag var1) {
      if (var1 instanceof IntArrayTag) {
         IntArrayTag var2 = (IntArrayTag)var1;
         return DataResult.success(Arrays.stream(var2.getAsIntArray()));
      } else {
         return super.getIntStream(var1);
      }
   }

   public Tag createIntList(IntStream var1) {
      return new IntArrayTag(var1.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag var1) {
      if (var1 instanceof LongArrayTag) {
         LongArrayTag var2 = (LongArrayTag)var1;
         return DataResult.success(Arrays.stream(var2.getAsLongArray()));
      } else {
         return super.getLongStream(var1);
      }
   }

   public Tag createLongList(LongStream var1) {
      return new LongArrayTag(var1.toArray());
   }

   public Tag createList(Stream<Tag> var1) {
      return NbtOps.InitialListCollector.INSTANCE.acceptAll(var1).result();
   }

   public Tag remove(Tag var1, String var2) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var3 = (CompoundTag)var1;
         CompoundTag var4 = var3.shallowCopy();
         var4.remove(var2);
         return var4;
      } else {
         return var1;
      }
   }

   public String toString() {
      return "NBT";
   }

   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder(this);
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag var0) {
      if (var0 instanceof EndTag) {
         return Optional.of(NbtOps.InitialListCollector.INSTANCE);
      } else {
         if (var0 instanceof CollectionTag) {
            CollectionTag var1 = (CollectionTag)var0;
            if (var1.isEmpty()) {
               return Optional.of(NbtOps.InitialListCollector.INSTANCE);
            }

            if (var1 instanceof ListTag) {
               ListTag var5 = (ListTag)var1;
               Optional var10000;
               switch(var5.getElementType()) {
               case 0:
                  var10000 = Optional.of(NbtOps.InitialListCollector.INSTANCE);
                  break;
               case 10:
                  var10000 = Optional.of(new NbtOps.HeterogenousListCollector(var5));
                  break;
               default:
                  var10000 = Optional.of(new NbtOps.HomogenousListCollector(var5));
               }

               return var10000;
            }

            if (var1 instanceof ByteArrayTag) {
               ByteArrayTag var4 = (ByteArrayTag)var1;
               return Optional.of(new NbtOps.ByteListCollector(var4.getAsByteArray()));
            }

            if (var1 instanceof IntArrayTag) {
               IntArrayTag var3 = (IntArrayTag)var1;
               return Optional.of(new NbtOps.IntListCollector(var3.getAsIntArray()));
            }

            if (var1 instanceof LongArrayTag) {
               LongArrayTag var2 = (LongArrayTag)var1;
               return Optional.of(new NbtOps.LongListCollector(var2.getAsLongArray()));
            }
         }

         return Optional.empty();
      }
   }

   // $FF: synthetic method
   public Object remove(final Object param1, final String param2) {
      return this.remove((Tag)var1, var2);
   }

   // $FF: synthetic method
   public Object createLongList(final LongStream param1) {
      return this.createLongList(var1);
   }

   // $FF: synthetic method
   public DataResult getLongStream(final Object param1) {
      return this.getLongStream((Tag)var1);
   }

   // $FF: synthetic method
   public Object createIntList(final IntStream param1) {
      return this.createIntList(var1);
   }

   // $FF: synthetic method
   public DataResult getIntStream(final Object param1) {
      return this.getIntStream((Tag)var1);
   }

   // $FF: synthetic method
   public Object createByteList(final ByteBuffer param1) {
      return this.createByteList(var1);
   }

   // $FF: synthetic method
   public DataResult getByteBuffer(final Object param1) {
      return this.getByteBuffer((Tag)var1);
   }

   // $FF: synthetic method
   public Object createList(final Stream param1) {
      return this.createList(var1);
   }

   // $FF: synthetic method
   public DataResult getList(final Object param1) {
      return this.getList((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getStream(final Object param1) {
      return this.getStream((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getMap(final Object param1) {
      return this.getMap((Tag)var1);
   }

   // $FF: synthetic method
   public Object createMap(final Stream param1) {
      return this.createMap(var1);
   }

   // $FF: synthetic method
   public DataResult getMapEntries(final Object param1) {
      return this.getMapEntries((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getMapValues(final Object param1) {
      return this.getMapValues((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final MapLike param2) {
      return this.mergeToMap((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final Map param2) {
      return this.mergeToMap((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final Object param2, final Object param3) {
      return this.mergeToMap((Tag)var1, (Tag)var2, (Tag)var3);
   }

   // $FF: synthetic method
   public DataResult mergeToList(final Object param1, final List param2) {
      return this.mergeToList((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToList(final Object param1, final Object param2) {
      return this.mergeToList((Tag)var1, (Tag)var2);
   }

   // $FF: synthetic method
   public Object createString(final String param1) {
      return this.createString(var1);
   }

   // $FF: synthetic method
   public DataResult getStringValue(final Object param1) {
      return this.getStringValue((Tag)var1);
   }

   // $FF: synthetic method
   public Object createBoolean(final boolean param1) {
      return this.createBoolean(var1);
   }

   // $FF: synthetic method
   public Object createDouble(final double param1) {
      return this.createDouble(var1);
   }

   // $FF: synthetic method
   public Object createFloat(final float param1) {
      return this.createFloat(var1);
   }

   // $FF: synthetic method
   public Object createLong(final long param1) {
      return this.createLong(var1);
   }

   // $FF: synthetic method
   public Object createInt(final int param1) {
      return this.createInt(var1);
   }

   // $FF: synthetic method
   public Object createShort(final short param1) {
      return this.createShort(var1);
   }

   // $FF: synthetic method
   public Object createByte(final byte param1) {
      return this.createByte(var1);
   }

   // $FF: synthetic method
   public Object createNumeric(final Number param1) {
      return this.createNumeric(var1);
   }

   // $FF: synthetic method
   public DataResult getNumberValue(final Object param1) {
      return this.getNumberValue((Tag)var1);
   }

   // $FF: synthetic method
   public Object convertTo(final DynamicOps param1, final Object param2) {
      return this.convertTo(var1, (Tag)var2);
   }

   // $FF: synthetic method
   public Object empty() {
      return this.empty();
   }

   private static class InitialListCollector implements NbtOps.ListCollector {
      public static final NbtOps.InitialListCollector INSTANCE = new NbtOps.InitialListCollector();

      private InitialListCollector() {
         super();
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof CompoundTag) {
            CompoundTag var5 = (CompoundTag)var1;
            return (new NbtOps.HeterogenousListCollector()).accept(var5);
         } else if (var1 instanceof ByteTag) {
            ByteTag var4 = (ByteTag)var1;
            return new NbtOps.ByteListCollector(var4.getAsByte());
         } else if (var1 instanceof IntTag) {
            IntTag var3 = (IntTag)var1;
            return new NbtOps.IntListCollector(var3.getAsInt());
         } else if (var1 instanceof LongTag) {
            LongTag var2 = (LongTag)var1;
            return new NbtOps.LongListCollector(var2.getAsLong());
         } else {
            return new NbtOps.HomogenousListCollector(var1);
         }
      }

      public Tag result() {
         return new ListTag();
      }
   }

   private interface ListCollector {
      NbtOps.ListCollector accept(Tag var1);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> var1) {
         NbtOps.ListCollector var2 = this;

         Tag var4;
         for(Iterator var3 = var1.iterator(); var3.hasNext(); var2 = var2.accept(var4)) {
            var4 = (Tag)var3.next();
         }

         return var2;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> var1) {
         Objects.requireNonNull(var1);
         return this.acceptAll(var1::iterator);
      }

      Tag result();
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder(final NbtOps param1) {
         super(var1);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String var1, Tag var2, CompoundTag var3) {
         var3.put(var1, var2);
         return var3;
      }

      protected DataResult<Tag> build(CompoundTag var1, Tag var2) {
         if (var2 != null && var2 != EndTag.INSTANCE) {
            if (!(var2 instanceof CompoundTag)) {
               return DataResult.error(() -> {
                  return "mergeToMap called with not a map: " + String.valueOf(var2);
               }, var2);
            } else {
               CompoundTag var3 = (CompoundTag)var2;
               CompoundTag var4 = var3.shallowCopy();
               Iterator var5 = var1.entrySet().iterator();

               while(var5.hasNext()) {
                  Entry var6 = (Entry)var5.next();
                  var4.put((String)var6.getKey(), (Tag)var6.getValue());
               }

               return DataResult.success(var4);
            }
         } else {
            return DataResult.success(var1);
         }
      }

      // $FF: synthetic method
      protected Object append(final String param1, final Object param2, final Object param3) {
         return this.append(var1, (Tag)var2, (CompoundTag)var3);
      }

      // $FF: synthetic method
      protected DataResult build(final Object param1, final Object param2) {
         return this.build((CompoundTag)var1, (Tag)var2);
      }

      // $FF: synthetic method
      protected Object initBuilder() {
         return this.initBuilder();
      }
   }

   private static class HeterogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      public HeterogenousListCollector() {
         super();
      }

      public HeterogenousListCollector(Collection<Tag> var1) {
         super();
         this.result.addAll(var1);
      }

      public HeterogenousListCollector(IntArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(wrapElement(IntTag.valueOf(var1x)));
         });
      }

      public HeterogenousListCollector(ByteArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(wrapElement(ByteTag.valueOf(var1x)));
         });
      }

      public HeterogenousListCollector(LongArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(wrapElement(LongTag.valueOf(var1x)));
         });
      }

      private static boolean isWrapper(CompoundTag var0) {
         return var0.size() == 1 && var0.contains("");
      }

      private static Tag wrapIfNeeded(Tag var0) {
         if (var0 instanceof CompoundTag) {
            CompoundTag var1 = (CompoundTag)var0;
            if (!isWrapper(var1)) {
               return var1;
            }
         }

         return wrapElement(var0);
      }

      private static CompoundTag wrapElement(Tag var0) {
         CompoundTag var1 = new CompoundTag();
         var1.put("", var0);
         return var1;
      }

      public NbtOps.ListCollector accept(Tag var1) {
         this.result.add(wrapIfNeeded(var1));
         return this;
      }

      public Tag result() {
         return this.result;
      }
   }

   private static class HomogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      HomogenousListCollector(Tag var1) {
         super();
         this.result.add(var1);
      }

      HomogenousListCollector(ListTag var1) {
         super();
         this.result.addAll(var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1.getId() != this.result.getElementType()) {
            return (new NbtOps.HeterogenousListCollector()).acceptAll(this.result).accept(var1);
         } else {
            this.result.add(var1);
            return this;
         }
      }

      public Tag result() {
         return this.result;
      }
   }

   private static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte var1) {
         super();
         this.values.add(var1);
      }

      public ByteListCollector(byte[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof ByteTag) {
            ByteTag var2 = (ByteTag)var1;
            this.values.add(var2.getAsByte());
            return this;
         } else {
            return (new NbtOps.HeterogenousListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int var1) {
         super();
         this.values.add(var1);
      }

      public IntListCollector(int[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof IntTag) {
            IntTag var2 = (IntTag)var1;
            this.values.add(var2.getAsInt());
            return this;
         } else {
            return (new NbtOps.HeterogenousListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long var1) {
         super();
         this.values.add(var1);
      }

      public LongListCollector(long[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof LongTag) {
            LongTag var2 = (LongTag)var1;
            this.values.add(var2.getAsLong());
            return this;
         } else {
            return (new NbtOps.HeterogenousListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }
}
