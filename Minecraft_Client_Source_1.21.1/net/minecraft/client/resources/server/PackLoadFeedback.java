package net.minecraft.client.resources.server;

import java.util.UUID;

public interface PackLoadFeedback {
   void reportUpdate(UUID var1, PackLoadFeedback.Update var2);

   void reportFinalResult(UUID var1, PackLoadFeedback.FinalResult var2);

   public static enum FinalResult {
      DECLINED,
      APPLIED,
      DISCARDED,
      DOWNLOAD_FAILED,
      ACTIVATION_FAILED;

      private FinalResult() {
      }

      // $FF: synthetic method
      private static PackLoadFeedback.FinalResult[] $values() {
         return new PackLoadFeedback.FinalResult[]{DECLINED, APPLIED, DISCARDED, DOWNLOAD_FAILED, ACTIVATION_FAILED};
      }
   }

   public static enum Update {
      ACCEPTED,
      DOWNLOADED;

      private Update() {
      }

      // $FF: synthetic method
      private static PackLoadFeedback.Update[] $values() {
         return new PackLoadFeedback.Update[]{ACCEPTED, DOWNLOADED};
      }
   }
}
