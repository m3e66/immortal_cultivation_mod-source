package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** One compact event drives deterministic local particles, injuries and short camera impulses. */
public final class CombatFeedbackPayloads {
    public static final int PHYSICAL = 1, FIRE = 2, FROST = 4, LIGHTNING = 8, AURA = 16, SPATIAL = 32, LOW_QI = 64;
    public static final int HIGH_DAMAGE = 128;
    public enum Kind { HIT, BLOCK, DASH_START, DASH_STOP, LANDING, SHIELD_BREAK }
    private CombatFeedbackPayloads() {}

    public record Effect(ResourceLocation dimension, Kind kind, int subjectId, Vec3 position,
                         Vec3 direction, float intensity, int injuryFlags, int seed) implements CustomPacketPayload {
        public static final Type<Effect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "combat_feedback"));
        public Effect {
            intensity = Float.isFinite(intensity) ? Mth.clamp(intensity, 0, 1) : 0;
            injuryFlags &= PHYSICAL | FIRE | FROST | LIGHTNING | AURA | SPATIAL | LOW_QI | HIGH_DAMAGE;
        }
        public static final StreamCodec<RegistryFriendlyByteBuf, Effect> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeResourceLocation(value.dimension); buf.writeEnum(value.kind);
                    buf.writeVarInt(value.subjectId); writeVec(buf, value.position);
                    buf.writeFloat((float) value.direction.x); buf.writeFloat((float) value.direction.y);
                    buf.writeFloat((float) value.direction.z); buf.writeFloat(value.intensity);
                    buf.writeByte(value.injuryFlags); buf.writeInt(value.seed);
                }, buf -> new Effect(buf.readResourceLocation(), buf.readEnum(Kind.class), buf.readVarInt(),
                        new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                        new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                        buf.readFloat(), buf.readUnsignedByte(), buf.readInt()));
        private static void writeVec(RegistryFriendlyByteBuf buf, Vec3 value) {
            buf.writeDouble(value.x); buf.writeDouble(value.y); buf.writeDouble(value.z);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Effect.TYPE, Effect.STREAM_CODEC, (effect, context) -> {
                if (FMLEnvironment.dist.isClient()) context.enqueueWork(() -> {
                    com.example.immortal_cultivation_mod.client.CombatImpactVfx.accept(effect);
                    com.example.immortal_cultivation_mod.client.InjuryVfx.accept(effect);
                    com.example.immortal_cultivation_mod.client.CombatCameraVfx.accept(effect);
                });
            });
        }
    }
}
