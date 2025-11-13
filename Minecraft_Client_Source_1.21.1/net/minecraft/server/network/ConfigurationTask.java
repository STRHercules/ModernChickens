package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;

public interface ConfigurationTask {
   void start(Consumer<Packet<?>> var1);

   ConfigurationTask.Type type();

   public static record Type(String id) {
      public Type(String param1) {
         super();
         this.id = var1;
      }

      public String toString() {
         return this.id;
      }

      public String id() {
         return this.id;
      }
   }
}
