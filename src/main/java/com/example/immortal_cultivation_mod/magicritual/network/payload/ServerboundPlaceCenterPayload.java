package com.example.immortal_cultivation_mod.magicritual.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record ServerboundPlaceCenterPayload(int radius) implements CustomPacketPayload {
   public static final Type<ServerboundPlaceCenterPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "place_center"));
   public static final StreamCodec<ByteBuf, ServerboundPlaceCenterPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ServerboundPlaceCenterPayload::radius, ServerboundPlaceCenterPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
