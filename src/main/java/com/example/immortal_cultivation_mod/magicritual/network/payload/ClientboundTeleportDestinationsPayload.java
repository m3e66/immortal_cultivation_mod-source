package com.example.immortal_cultivation_mod.magicritual.network.payload;

import com.example.immortal_cultivation_mod.magicritual.center.TeleportDestinationInfo;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundTeleportDestinationsPayload(BlockPos pos, String localCode, String selectedCode, List<TeleportDestinationInfo> destinations) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ClientboundTeleportDestinationsPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "teleport_destinations_sync"));
   private static final StreamCodec<ByteBuf, List<TeleportDestinationInfo>> DESTINATIONS_CODEC;
   public static final StreamCodec<ByteBuf, ClientboundTeleportDestinationsPayload> STREAM_CODEC;

   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   static {
      DESTINATIONS_CODEC = TeleportDestinationInfo.STREAM_CODEC.apply(ByteBufCodecs.list(32));
      STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ClientboundTeleportDestinationsPayload::pos, ByteBufCodecs.stringUtf8(64), ClientboundTeleportDestinationsPayload::localCode, ByteBufCodecs.stringUtf8(64), ClientboundTeleportDestinationsPayload::selectedCode, DESTINATIONS_CODEC, ClientboundTeleportDestinationsPayload::destinations, ClientboundTeleportDestinationsPayload::new);
   }
}
