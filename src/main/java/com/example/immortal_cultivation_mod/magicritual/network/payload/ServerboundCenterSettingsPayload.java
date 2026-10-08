package com.example.immortal_cultivation_mod.magicritual.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCenterSettingsPayload(BlockPos pos, String action, String value) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ServerboundCenterSettingsPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "center_settings"));
   public static final StreamCodec<ByteBuf, ServerboundCenterSettingsPayload> STREAM_CODEC;

   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ServerboundCenterSettingsPayload::pos, ByteBufCodecs.stringUtf8(32), ServerboundCenterSettingsPayload::action, ByteBufCodecs.stringUtf8(64), ServerboundCenterSettingsPayload::value, ServerboundCenterSettingsPayload::new);
   }
}
