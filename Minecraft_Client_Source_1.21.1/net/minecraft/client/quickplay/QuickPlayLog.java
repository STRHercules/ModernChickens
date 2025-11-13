package net.minecraft.client.quickplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

public class QuickPlayLog {
   private static final QuickPlayLog INACTIVE = new QuickPlayLog("") {
      public void log(Minecraft var1) {
      }

      public void setWorldData(QuickPlayLog.Type var1, String var2, String var3) {
      }
   };
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = (new GsonBuilder()).create();
   private final Path path;
   @Nullable
   private QuickPlayLog.QuickPlayWorld worldData;

   QuickPlayLog(String var1) {
      super();
      this.path = Minecraft.getInstance().gameDirectory.toPath().resolve(var1);
   }

   public static QuickPlayLog of(@Nullable String var0) {
      return var0 == null ? INACTIVE : new QuickPlayLog(var0);
   }

   public void setWorldData(QuickPlayLog.Type var1, String var2, String var3) {
      this.worldData = new QuickPlayLog.QuickPlayWorld(var1, var2, var3);
   }

   public void log(Minecraft var1) {
      if (var1.gameMode != null && this.worldData != null) {
         Util.ioPool().execute(() -> {
            try {
               Files.deleteIfExists(this.path);
            } catch (IOException var3) {
               LOGGER.error("Failed to delete quickplay log file {}", this.path, var3);
            }

            QuickPlayLog.QuickPlayEntry var2 = new QuickPlayLog.QuickPlayEntry(this.worldData, Instant.now(), var1.gameMode.getPlayerMode());
            DataResult var10000 = Codec.list(QuickPlayLog.QuickPlayEntry.CODEC).encodeStart(JsonOps.INSTANCE, List.of(var2));
            Logger var10002 = LOGGER;
            Objects.requireNonNull(var10002);
            var10000.resultOrPartial(Util.prefix("Quick Play: ", var10002::error)).ifPresent((var1x) -> {
               try {
                  Files.createDirectories(this.path.getParent());
                  Files.writeString(this.path, GSON.toJson(var1x));
               } catch (IOException var3) {
                  LOGGER.error("Failed to write to quickplay log file {}", this.path, var3);
               }

            });
         });
      } else {
         LOGGER.error("Failed to log session for quickplay. Missing world data or gamemode");
      }
   }

   private static record QuickPlayWorld(QuickPlayLog.Type type, String id, String name) {
      public static final MapCodec<QuickPlayLog.QuickPlayWorld> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(QuickPlayLog.Type.CODEC.fieldOf("type").forGetter(QuickPlayLog.QuickPlayWorld::type), ExtraCodecs.ESCAPED_STRING.fieldOf("id").forGetter(QuickPlayLog.QuickPlayWorld::id), Codec.STRING.fieldOf("name").forGetter(QuickPlayLog.QuickPlayWorld::name)).apply(var0, QuickPlayLog.QuickPlayWorld::new);
      });

      QuickPlayWorld(QuickPlayLog.Type param1, String param2, String param3) {
         super();
         this.type = var1;
         this.id = var2;
         this.name = var3;
      }

      public QuickPlayLog.Type type() {
         return this.type;
      }

      public String id() {
         return this.id;
      }

      public String name() {
         return this.name;
      }
   }

   public static enum Type implements StringRepresentable {
      SINGLEPLAYER("singleplayer"),
      MULTIPLAYER("multiplayer"),
      REALMS("realms");

      static final Codec<QuickPlayLog.Type> CODEC = StringRepresentable.fromEnum(QuickPlayLog.Type::values);
      private final String name;

      private Type(final String param3) {
         this.name = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static QuickPlayLog.Type[] $values() {
         return new QuickPlayLog.Type[]{SINGLEPLAYER, MULTIPLAYER, REALMS};
      }
   }

   static record QuickPlayEntry(QuickPlayLog.QuickPlayWorld quickPlayWorld, Instant lastPlayedTime, GameType gamemode) {
      public static final Codec<QuickPlayLog.QuickPlayEntry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(QuickPlayLog.QuickPlayWorld.MAP_CODEC.forGetter(QuickPlayLog.QuickPlayEntry::quickPlayWorld), ExtraCodecs.INSTANT_ISO8601.fieldOf("lastPlayedTime").forGetter(QuickPlayLog.QuickPlayEntry::lastPlayedTime), GameType.CODEC.fieldOf("gamemode").forGetter(QuickPlayLog.QuickPlayEntry::gamemode)).apply(var0, QuickPlayLog.QuickPlayEntry::new);
      });

      QuickPlayEntry(QuickPlayLog.QuickPlayWorld param1, Instant param2, GameType param3) {
         super();
         this.quickPlayWorld = var1;
         this.lastPlayedTime = var2;
         this.gamemode = var3;
      }

      public QuickPlayLog.QuickPlayWorld quickPlayWorld() {
         return this.quickPlayWorld;
      }

      public Instant lastPlayedTime() {
         return this.lastPlayedTime;
      }

      public GameType gamemode() {
         return this.gamemode;
      }
   }
}
