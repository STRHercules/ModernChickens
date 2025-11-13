package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final Iterable<Path> inputFolders;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(PackOutput var1, Iterable<Path> var2) {
      super();
      this.output = var1;
      this.inputFolders = var2;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter var1) {
      this.filters.add(var1);
      return this;
   }

   private CompoundTag applyFilters(String var1, CompoundTag var2) {
      CompoundTag var3 = var2;

      SnbtToNbt.Filter var5;
      for(Iterator var4 = this.filters.iterator(); var4.hasNext(); var3 = var5.apply(var1, var3)) {
         var5 = (SnbtToNbt.Filter)var4.next();
      }

      return var3;
   }

   public CompletableFuture<?> run(CachedOutput var1) {
      Path var2 = this.output.getOutputFolder();
      ArrayList var3 = Lists.newArrayList();
      Iterator var4 = this.inputFolders.iterator();

      while(var4.hasNext()) {
         Path var5 = (Path)var4.next();
         var3.add(CompletableFuture.supplyAsync(() -> {
            try {
               Stream var4 = Files.walk(var5);

               CompletableFuture var5x;
               try {
                  var5x = CompletableFuture.allOf((CompletableFuture[])var4.filter((var0) -> {
                     return var0.toString().endsWith(".snbt");
                  }).map((var4x) -> {
                     return CompletableFuture.runAsync(() -> {
                        SnbtToNbt.TaskResult var5x = this.readStructure(var4x, this.getName(var5, var4x));
                        this.storeStructureIfChanged(var1, var5x, var2);
                     }, Util.backgroundExecutor());
                  }).toArray((var0) -> {
                     return new CompletableFuture[var0];
                  }));
               } catch (Throwable var8) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (var4 != null) {
                  var4.close();
               }

               return var5x;
            } catch (Exception var9) {
               throw new RuntimeException("Failed to read structure input directory, aborting", var9);
            }
         }, Util.backgroundExecutor()).thenCompose((var0) -> {
            return var0;
         }));
      }

      return Util.sequenceFailFast(var3);
   }

   public final String getName() {
      return "SNBT -> NBT";
   }

   private String getName(Path var1, Path var2) {
      String var3 = var1.relativize(var2).toString().replaceAll("\\\\", "/");
      return var3.substring(0, var3.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path var1, String var2) {
      try {
         BufferedReader var3 = Files.newBufferedReader(var1);

         SnbtToNbt.TaskResult var10;
         try {
            String var4 = IOUtils.toString(var3);
            CompoundTag var5 = this.applyFilters(var2, NbtUtils.snbtToStructure(var4));
            ByteArrayOutputStream var6 = new ByteArrayOutputStream();
            HashingOutputStream var7 = new HashingOutputStream(Hashing.sha1(), var6);
            NbtIo.writeCompressed(var5, (OutputStream)var7);
            byte[] var8 = var6.toByteArray();
            HashCode var9 = var7.hash();
            var10 = new SnbtToNbt.TaskResult(var2, var8, var9);
         } catch (Throwable var12) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (var3 != null) {
            var3.close();
         }

         return var10;
      } catch (Throwable var13) {
         throw new SnbtToNbt.StructureConversionException(var1, var13);
      }
   }

   private void storeStructureIfChanged(CachedOutput var1, SnbtToNbt.TaskResult var2, Path var3) {
      Path var4 = var3.resolve(var2.name + ".nbt");

      try {
         var1.writeIfNeeded(var4, var2.payload, var2.hash);
      } catch (IOException var6) {
         LOGGER.error("Couldn't write structure {} at {}", new Object[]{var2.name, var4, var6});
      }

   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String var1, CompoundTag var2);
   }

   static record TaskResult(String name, byte[] payload, HashCode hash) {
      final String name;
      final byte[] payload;
      final HashCode hash;

      TaskResult(String param1, byte[] param2, HashCode param3) {
         super();
         this.name = var1;
         this.payload = var2;
         this.hash = var3;
      }

      public String name() {
         return this.name;
      }

      public byte[] payload() {
         return this.payload;
      }

      public HashCode hash() {
         return this.hash;
      }
   }

   private static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path var1, Throwable var2) {
         super(var1.toAbsolutePath().toString(), var2);
      }
   }
}
