package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import javax.crypto.Cipher;

public class CipherEncoder extends MessageToByteEncoder<ByteBuf> {
   private final CipherBase cipher;

   public CipherEncoder(Cipher var1) {
      super();
      this.cipher = new CipherBase(var1);
   }

   protected void encode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception {
      this.cipher.encipher(var2, var3);
   }

   // $FF: synthetic method
   protected void encode(final ChannelHandlerContext param1, final Object param2, final ByteBuf param3) throws Exception {
      this.encode(var1, (ByteBuf)var2, var3);
   }
}
