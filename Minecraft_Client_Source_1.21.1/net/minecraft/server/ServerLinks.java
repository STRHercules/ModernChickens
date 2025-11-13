package net.minecraft.server;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public record ServerLinks(List<ServerLinks.Entry> entries) {
   public static final ServerLinks EMPTY = new ServerLinks(List.of());
   public static final StreamCodec<ByteBuf, Either<ServerLinks.KnownLinkType, Component>> TYPE_STREAM_CODEC;
   public static final StreamCodec<ByteBuf, List<ServerLinks.UntrustedEntry>> UNTRUSTED_LINKS_STREAM_CODEC;

   public ServerLinks(List<ServerLinks.Entry> param1) {
      super();
      this.entries = var1;
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public Optional<ServerLinks.Entry> findKnownType(ServerLinks.KnownLinkType var1) {
      return this.entries.stream().filter((var1x) -> {
         return (Boolean)var1x.type.map((var1xx) -> {
            return var1xx == var1;
         }, (var0) -> {
            return false;
         });
      }).findFirst();
   }

   public List<ServerLinks.UntrustedEntry> untrust() {
      return this.entries.stream().map((var0) -> {
         return new ServerLinks.UntrustedEntry(var0.type, var0.link.toString());
      }).toList();
   }

   public List<ServerLinks.Entry> entries() {
      return this.entries;
   }

   static {
      TYPE_STREAM_CODEC = ByteBufCodecs.either(ServerLinks.KnownLinkType.STREAM_CODEC, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC);
      UNTRUSTED_LINKS_STREAM_CODEC = ServerLinks.UntrustedEntry.STREAM_CODEC.apply(ByteBufCodecs.list());
   }

   public static enum KnownLinkType {
      BUG_REPORT(0, "report_bug"),
      COMMUNITY_GUIDELINES(1, "community_guidelines"),
      SUPPORT(2, "support"),
      STATUS(3, "status"),
      FEEDBACK(4, "feedback"),
      COMMUNITY(5, "community"),
      WEBSITE(6, "website"),
      FORUMS(7, "forums"),
      NEWS(8, "news"),
      ANNOUNCEMENTS(9, "announcements");

      private static final IntFunction<ServerLinks.KnownLinkType> BY_ID = ByIdMap.continuous((var0) -> {
         return var0.id;
      }, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      public static final StreamCodec<ByteBuf, ServerLinks.KnownLinkType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, (var0) -> {
         return var0.id;
      });
      private final int id;
      private final String name;

      private KnownLinkType(final int param3, final String param4) {
         this.id = var3;
         this.name = var4;
      }

      private Component displayName() {
         return Component.translatable("known_server_link." + this.name);
      }

      public ServerLinks.Entry create(URI var1) {
         return ServerLinks.Entry.knownType(this, var1);
      }

      // $FF: synthetic method
      private static ServerLinks.KnownLinkType[] $values() {
         return new ServerLinks.KnownLinkType[]{BUG_REPORT, COMMUNITY_GUIDELINES, SUPPORT, STATUS, FEEDBACK, COMMUNITY, WEBSITE, FORUMS, NEWS, ANNOUNCEMENTS};
      }
   }

   public static record UntrustedEntry(Either<ServerLinks.KnownLinkType, Component> type, String link) {
      public static final StreamCodec<ByteBuf, ServerLinks.UntrustedEntry> STREAM_CODEC;

      public UntrustedEntry(Either<ServerLinks.KnownLinkType, Component> param1, String param2) {
         super();
         this.type = var1;
         this.link = var2;
      }

      public Either<ServerLinks.KnownLinkType, Component> type() {
         return this.type;
      }

      public String link() {
         return this.link;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ServerLinks.TYPE_STREAM_CODEC, ServerLinks.UntrustedEntry::type, ByteBufCodecs.STRING_UTF8, ServerLinks.UntrustedEntry::link, ServerLinks.UntrustedEntry::new);
      }
   }

   public static record Entry(Either<ServerLinks.KnownLinkType, Component> type, URI link) {
      final Either<ServerLinks.KnownLinkType, Component> type;
      final URI link;

      public Entry(Either<ServerLinks.KnownLinkType, Component> param1, URI param2) {
         super();
         this.type = var1;
         this.link = var2;
      }

      public static ServerLinks.Entry knownType(ServerLinks.KnownLinkType var0, URI var1) {
         return new ServerLinks.Entry(Either.left(var0), var1);
      }

      public static ServerLinks.Entry custom(Component var0, URI var1) {
         return new ServerLinks.Entry(Either.right(var0), var1);
      }

      public Component displayName() {
         return (Component)this.type.map(ServerLinks.KnownLinkType::displayName, (var0) -> {
            return var0;
         });
      }

      public Either<ServerLinks.KnownLinkType, Component> type() {
         return this.type;
      }

      public URI link() {
         return this.link;
      }
   }
}
