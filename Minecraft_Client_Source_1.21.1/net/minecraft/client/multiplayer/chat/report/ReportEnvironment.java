package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;

public record ReportEnvironment(String clientVersion, @Nullable ReportEnvironment.Server server) {
   public ReportEnvironment(String param1, @Nullable ReportEnvironment.Server param2) {
      super();
      this.clientVersion = var1;
      this.server = var2;
   }

   public static ReportEnvironment local() {
      return create((ReportEnvironment.Server)null);
   }

   public static ReportEnvironment thirdParty(String var0) {
      return create(new ReportEnvironment.Server.ThirdParty(var0));
   }

   public static ReportEnvironment realm(RealmsServer var0) {
      return create(new ReportEnvironment.Server.Realm(var0));
   }

   public static ReportEnvironment create(@Nullable ReportEnvironment.Server var0) {
      return new ReportEnvironment(getClientVersion(), var0);
   }

   public ClientInfo clientInfo() {
      return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
   }

   @Nullable
   public ThirdPartyServerInfo thirdPartyServerInfo() {
      ReportEnvironment.Server var2 = this.server;
      if (var2 instanceof ReportEnvironment.Server.ThirdParty) {
         ReportEnvironment.Server.ThirdParty var1 = (ReportEnvironment.Server.ThirdParty)var2;
         return new ThirdPartyServerInfo(var1.ip);
      } else {
         return null;
      }
   }

   @Nullable
   public RealmInfo realmInfo() {
      ReportEnvironment.Server var2 = this.server;
      if (var2 instanceof ReportEnvironment.Server.Realm) {
         ReportEnvironment.Server.Realm var1 = (ReportEnvironment.Server.Realm)var2;
         return new RealmInfo(String.valueOf(var1.realmId()), var1.slotId());
      } else {
         return null;
      }
   }

   private static String getClientVersion() {
      StringBuilder var0 = new StringBuilder();
      var0.append("1.21.1");
      if (Minecraft.checkModStatus().shouldReportAsModified()) {
         var0.append(" (modded)");
      }

      return var0.toString();
   }

   public String clientVersion() {
      return this.clientVersion;
   }

   @Nullable
   public ReportEnvironment.Server server() {
      return this.server;
   }

   public interface Server {
      public static record Realm(long realmId, int slotId) implements ReportEnvironment.Server {
         public Realm(RealmsServer var1) {
            this(var1.id, var1.activeSlot);
         }

         public Realm(long param1, int param3) {
            super();
            this.realmId = var1;
            this.slotId = var3;
         }

         public long realmId() {
            return this.realmId;
         }

         public int slotId() {
            return this.slotId;
         }
      }

      public static record ThirdParty(String ip) implements ReportEnvironment.Server {
         final String ip;

         public ThirdParty(String param1) {
            super();
            this.ip = var1;
         }

         public String ip() {
            return this.ip;
         }
      }
   }
}
