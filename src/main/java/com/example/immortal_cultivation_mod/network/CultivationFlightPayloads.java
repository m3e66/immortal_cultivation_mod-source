package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.CultivationFlightTrailRenderer;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** A small server-authorized descriptor; each client builds the moving wake locally. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID, bus=EventBusSubscriber.Bus.MOD)
public final class CultivationFlightPayloads {
    private CultivationFlightPayloads() {}
    public record Trail(ResourceLocation dimension, int entityId, UUID uuid, int color, float strength) implements CustomPacketPayload {
        public static final Type<Trail> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "cultivation_flight_trail"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Trail> CODEC = StreamCodec.of((buf, value) -> {
            buf.writeResourceLocation(value.dimension); buf.writeVarInt(value.entityId); buf.writeUUID(value.uuid);
            buf.writeInt(value.color); buf.writeFloat(value.strength);
        }, buf -> new Trail(buf.readResourceLocation(), buf.readVarInt(), buf.readUUID(), buf.readInt(), buf.readFloat()));
        @Override public Type<Trail> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Trail.TYPE, Trail.CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) context.enqueueWork(() -> CultivationFlightTrailRenderer.accept(payload));
        });
    }
}
