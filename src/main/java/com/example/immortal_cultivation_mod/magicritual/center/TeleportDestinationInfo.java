package com.example.immortal_cultivation_mod.magicritual.center;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TeleportDestinationInfo(String code, String label, int qiCost, long energyCost, boolean available) {
   public static final StreamCodec<ByteBuf, TeleportDestinationInfo> STREAM_CODEC;

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), TeleportDestinationInfo::code, ByteBufCodecs.stringUtf8(192), TeleportDestinationInfo::label, ByteBufCodecs.VAR_INT, TeleportDestinationInfo::qiCost, ByteBufCodecs.VAR_LONG, TeleportDestinationInfo::energyCost, ByteBufCodecs.BOOL, TeleportDestinationInfo::available, TeleportDestinationInfo::new);
   }
}
