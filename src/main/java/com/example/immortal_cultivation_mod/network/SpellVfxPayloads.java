package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.NativeSpellVfx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Small visual descriptions; clients animate them using the mod's existing assets. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SpellVfxPayloads {
    public enum Kind { WATER_SHIELD, PUDDLE, PRESSURE, LINGBENG }
    private SpellVfxPayloads() {}

    public record Visual(ResourceLocation dimension, Kind kind, int entityId,
                         Vec3 position, int strength, int duration) implements CustomPacketPayload {
        public static final Type<Visual> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "spell_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Visual> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeResourceLocation(value.dimension);
                    buf.writeEnum(value.kind);
                    buf.writeVarInt(value.entityId);
                    buf.writeVec3(value.position);
                    buf.writeVarInt(value.strength);
                    buf.writeVarInt(value.duration);
                }, buf -> new Visual(buf.readResourceLocation(), buf.readEnum(Kind.class),
                        buf.readVarInt(), buf.readVec3(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<Visual> type() { return TYPE; }
    }

    /** The beam spell reveals only the cast-time targets and only to its caster. */
    public record BeamMarkers(ResourceLocation dimension, int[] entityIds, int duration)
            implements CustomPacketPayload {
        public static final int MAX_TARGETS = 4096;
        public static final Type<BeamMarkers> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "beam_markers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BeamMarkers> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeResourceLocation(value.dimension);
                    buf.writeVarIntArray(value.entityIds);
                    buf.writeVarInt(value.duration);
                }, buf -> new BeamMarkers(buf.readResourceLocation(),
                        buf.readVarIntArray(MAX_TARGETS), buf.readVarInt()));
        @Override public Type<BeamMarkers> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Visual.TYPE, Visual.CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() -> NativeSpellVfx.accept(payload));
            }
        });
        registrar.playToClient(BeamMarkers.TYPE, BeamMarkers.CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() -> NativeSpellVfx.accept(payload));
            }
        });
    }
}
