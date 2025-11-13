package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundClientCommandPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundClientCommandPacket> STREAM_CODEC = Packet.codec(ServerboundClientCommandPacket::write, ServerboundClientCommandPacket::new);
   private final ServerboundClientCommandPacket.Action action;

   public ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action var1) {
      super();
      this.action = var1;
   }

   private ServerboundClientCommandPacket(FriendlyByteBuf var1) {
      super();
      this.action = (ServerboundClientCommandPacket.Action)var1.readEnum(ServerboundClientCommandPacket.Action.class);
   }

   private void write(FriendlyByteBuf var1) {
      var1.writeEnum(this.action);
   }

   public PacketType<ServerboundClientCommandPacket> type() {
      return GamePacketTypes.SERVERBOUND_CLIENT_COMMAND;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleClientCommand(this);
   }

   public ServerboundClientCommandPacket.Action getAction() {
      return this.action;
   }

   public static enum Action {
      PERFORM_RESPAWN,
      REQUEST_STATS;

      private Action() {
      }

      // $FF: synthetic method
      private static ServerboundClientCommandPacket.Action[] $values() {
         return new ServerboundClientCommandPacket.Action[]{PERFORM_RESPAWN, REQUEST_STATS};
      }
   }
}
