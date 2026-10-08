package com.example.immortal_cultivation_mod.magicritual.network.payload;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCenterAccessPayload(BlockPos pos, List<String> blacklistedPlayers, List<String> controllerPlayers) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ClientboundCenterAccessPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "center_access_sync"));
   private static final StreamCodec<ByteBuf, List<String>> BLACKLIST_CODEC = ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(64));
   private static final StreamCodec<ByteBuf, List<String>> CONTROLLER_CODEC = ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(16));
   public static final StreamCodec<ByteBuf, ClientboundCenterAccessPayload> STREAM_CODEC;

   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ClientboundCenterAccessPayload::pos, BLACKLIST_CODEC, ClientboundCenterAccessPayload::blacklistedPlayers, CONTROLLER_CODEC, ClientboundCenterAccessPayload::controllerPlayers, ClientboundCenterAccessPayload::new);
   }
}
