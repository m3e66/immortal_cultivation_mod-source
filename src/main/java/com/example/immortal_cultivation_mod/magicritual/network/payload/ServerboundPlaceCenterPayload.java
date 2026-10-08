package com.example.immortal_cultivation_mod.magicritual.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundPlaceCenterPayload(int radius) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ServerboundPlaceCenterPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "place_center"));
   public static final StreamCodec<ByteBuf, ServerboundPlaceCenterPayload> STREAM_CODEC;

   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ServerboundPlaceCenterPayload::radius, ServerboundPlaceCenterPayload::new);
   }
}
