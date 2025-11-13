package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public record FileIOStat(Duration duration, @Nullable String path, long bytes) {
   public FileIOStat(Duration param1, @Nullable String param2, long param3) {
      super();
      this.duration = var1;
      this.path = var2;
      this.bytes = var3;
   }

   public static FileIOStat.Summary summary(Duration var0, List<FileIOStat> var1) {
      long var2 = var1.stream().mapToLong((var0x) -> {
         return var0x.bytes;
      }).sum();
      return new FileIOStat.Summary(var2, (double)var2 / (double)var0.getSeconds(), (long)var1.size(), (double)var1.size() / (double)var0.getSeconds(), (Duration)var1.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus), ((Map)var1.stream().filter((var0x) -> {
         return var0x.path != null;
      }).collect(Collectors.groupingBy((var0x) -> {
         return var0x.path;
      }, Collectors.summingLong((var0x) -> {
         return var0x.bytes;
      })))).entrySet().stream().sorted(Entry.comparingByValue().reversed()).map((var0x) -> {
         return Pair.of((String)var0x.getKey(), (Long)var0x.getValue());
      }).limit(10L).toList());
   }

   public Duration duration() {
      return this.duration;
   }

   @Nullable
   public String path() {
      return this.path;
   }

   public long bytes() {
      return this.bytes;
   }

   public static record Summary(long totalBytes, double bytesPerSecond, long counts, double countsPerSecond, Duration timeSpentInIO, List<Pair<String, Long>> topTenContributorsByTotalBytes) {
      public Summary(long param1, double param3, long param5, double param7, Duration param9, List<Pair<String, Long>> param10) {
         super();
         this.totalBytes = var1;
         this.bytesPerSecond = var3;
         this.counts = var5;
         this.countsPerSecond = var7;
         this.timeSpentInIO = var9;
         this.topTenContributorsByTotalBytes = var10;
      }

      public long totalBytes() {
         return this.totalBytes;
      }

      public double bytesPerSecond() {
         return this.bytesPerSecond;
      }

      public long counts() {
         return this.counts;
      }

      public double countsPerSecond() {
         return this.countsPerSecond;
      }

      public Duration timeSpentInIO() {
         return this.timeSpentInIO;
      }

      public List<Pair<String, Long>> topTenContributorsByTotalBytes() {
         return this.topTenContributorsByTotalBytes;
      }
   }
}
