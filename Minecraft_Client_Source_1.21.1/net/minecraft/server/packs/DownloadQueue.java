package net.minecraft.server.packs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class DownloadQueue implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_KEPT_PACKS = 20;
   private final Path cacheDir;
   private final JsonEventLog<DownloadQueue.LogEntry> eventLog;
   private final ProcessorMailbox<Runnable> tasks = ProcessorMailbox.create(Util.nonCriticalIoPool(), "download-queue");

   public DownloadQueue(Path var1) throws IOException {
      super();
      this.cacheDir = var1;
      FileUtil.createDirectoriesSafe(var1);
      this.eventLog = JsonEventLog.open(DownloadQueue.LogEntry.CODEC, var1.resolve("log.json"));
      DownloadCacheCleaner.vacuumCacheDir(var1, 20);
   }

   private DownloadQueue.BatchResult runDownload(DownloadQueue.BatchConfig var1, Map<UUID, DownloadQueue.DownloadRequest> var2) {
      DownloadQueue.BatchResult var3 = new DownloadQueue.BatchResult();
      var2.forEach((var3x, var4) -> {
         Path var5 = this.cacheDir.resolve(var3x.toString());
         Path var6 = null;

         try {
            var6 = HttpUtil.downloadFile(var5, var4.url, var1.headers, var1.hashFunction, var4.hash, var1.maxSize, var1.proxy, var1.listener);
            var3.downloaded.put(var3x, var6);
         } catch (Exception var9) {
            LOGGER.error("Failed to download {}", var4.url, var9);
            var3.failed.add(var3x);
         }

         try {
            this.eventLog.write(new DownloadQueue.LogEntry(var3x, var4.url.toString(), Instant.now(), Optional.ofNullable(var4.hash).map(HashCode::toString), var6 != null ? this.getFileInfo(var6) : Either.left("download_failed")));
         } catch (Exception var8) {
            LOGGER.error("Failed to log download of {}", var4.url, var8);
         }

      });
      return var3;
   }

   private Either<String, DownloadQueue.FileInfoEntry> getFileInfo(Path var1) {
      try {
         long var2 = Files.size(var1);
         Path var4 = this.cacheDir.relativize(var1);
         return Either.right(new DownloadQueue.FileInfoEntry(var4.toString(), var2));
      } catch (IOException var5) {
         LOGGER.error("Failed to get file size of {}", var1, var5);
         return Either.left("no_access");
      }
   }

   public CompletableFuture<DownloadQueue.BatchResult> downloadBatch(DownloadQueue.BatchConfig var1, Map<UUID, DownloadQueue.DownloadRequest> var2) {
      Supplier var10000 = () -> {
         return this.runDownload(var1, var2);
      };
      ProcessorMailbox var10001 = this.tasks;
      Objects.requireNonNull(var10001);
      return CompletableFuture.supplyAsync(var10000, var10001::tell);
   }

   public void close() throws IOException {
      this.tasks.close();
      this.eventLog.close();
   }

   static record LogEntry(UUID id, String url, Instant time, Optional<String> hash, Either<String, DownloadQueue.FileInfoEntry> errorOrFileInfo) {
      public static final Codec<DownloadQueue.LogEntry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DownloadQueue.LogEntry::id), Codec.STRING.fieldOf("url").forGetter(DownloadQueue.LogEntry::url), ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(DownloadQueue.LogEntry::time), Codec.STRING.optionalFieldOf("hash").forGetter(DownloadQueue.LogEntry::hash), Codec.mapEither(Codec.STRING.fieldOf("error"), DownloadQueue.FileInfoEntry.CODEC.fieldOf("file")).forGetter(DownloadQueue.LogEntry::errorOrFileInfo)).apply(var0, DownloadQueue.LogEntry::new);
      });

      LogEntry(UUID param1, String param2, Instant param3, Optional<String> param4, Either<String, DownloadQueue.FileInfoEntry> param5) {
         super();
         this.id = var1;
         this.url = var2;
         this.time = var3;
         this.hash = var4;
         this.errorOrFileInfo = var5;
      }

      public UUID id() {
         return this.id;
      }

      public String url() {
         return this.url;
      }

      public Instant time() {
         return this.time;
      }

      public Optional<String> hash() {
         return this.hash;
      }

      public Either<String, DownloadQueue.FileInfoEntry> errorOrFileInfo() {
         return this.errorOrFileInfo;
      }
   }

   public static record BatchResult(Map<UUID, Path> downloaded, Set<UUID> failed) {
      final Map<UUID, Path> downloaded;
      final Set<UUID> failed;

      public BatchResult() {
         this(new HashMap(), new HashSet());
      }

      public BatchResult(Map<UUID, Path> param1, Set<UUID> param2) {
         super();
         this.downloaded = var1;
         this.failed = var2;
      }

      public Map<UUID, Path> downloaded() {
         return this.downloaded;
      }

      public Set<UUID> failed() {
         return this.failed;
      }
   }

   public static record BatchConfig(HashFunction hashFunction, int maxSize, Map<String, String> headers, Proxy proxy, HttpUtil.DownloadProgressListener listener) {
      final HashFunction hashFunction;
      final int maxSize;
      final Map<String, String> headers;
      final Proxy proxy;
      final HttpUtil.DownloadProgressListener listener;

      public BatchConfig(HashFunction param1, int param2, Map<String, String> param3, Proxy param4, HttpUtil.DownloadProgressListener param5) {
         super();
         this.hashFunction = var1;
         this.maxSize = var2;
         this.headers = var3;
         this.proxy = var4;
         this.listener = var5;
      }

      public HashFunction hashFunction() {
         return this.hashFunction;
      }

      public int maxSize() {
         return this.maxSize;
      }

      public Map<String, String> headers() {
         return this.headers;
      }

      public Proxy proxy() {
         return this.proxy;
      }

      public HttpUtil.DownloadProgressListener listener() {
         return this.listener;
      }
   }

   private static record FileInfoEntry(String name, long size) {
      public static final Codec<DownloadQueue.FileInfoEntry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.STRING.fieldOf("name").forGetter(DownloadQueue.FileInfoEntry::name), Codec.LONG.fieldOf("size").forGetter(DownloadQueue.FileInfoEntry::size)).apply(var0, DownloadQueue.FileInfoEntry::new);
      });

      FileInfoEntry(String param1, long param2) {
         super();
         this.name = var1;
         this.size = var2;
      }

      public String name() {
         return this.name;
      }

      public long size() {
         return this.size;
      }
   }

   public static record DownloadRequest(URL url, @Nullable HashCode hash) {
      final URL url;
      @Nullable
      final HashCode hash;

      public DownloadRequest(URL param1, @Nullable HashCode param2) {
         super();
         this.url = var1;
         this.hash = var2;
      }

      public URL url() {
         return this.url;
      }

      @Nullable
      public HashCode hash() {
         return this.hash;
      }
   }
}
