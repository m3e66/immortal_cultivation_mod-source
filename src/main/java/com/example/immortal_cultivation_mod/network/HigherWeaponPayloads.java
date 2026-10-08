package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.item.HigherWeaponSpec;
import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape;
import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.BladeMask;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
public final class HigherWeaponPayloads {
    public static final int THUNDER_RITUAL_ANIMATION = -10;
    public static final int STAR_RITUAL_ANIMATION = -11;
    /** Shapes use server-confirmed impact points and movement segments, never a guessed client projectile. */
    public enum Visual {
        THRUST, CLEAVE, SWEEP, SILK, HOOK, WIND, GUST, THUNDER_SEAL, FLAME, FLAME_CHARGED, STAR,
        QI_RISING, QI_FALLING, QI_LEVEL, QI_CONE, QI_IMPACT
    }

    public record SkillEffect(ResourceLocation dimension, Visual visual, Vec3 from, Vec3 to,
                              float width, long start, int color, BladeMask bladeMask, int projectileId) implements CustomPacketPayload {
        public SkillEffect(ResourceLocation dimension, Visual visual, Vec3 from, Vec3 to,
                           float width, long start, int color) {
            this(dimension, visual, from, to, width, start, color, ExternalQiVfxShape.ALL_SECTIONS);
        }
        public SkillEffect(ResourceLocation dimension, Visual visual, Vec3 from, Vec3 to,
                           float width, long start, int color, BladeMask bladeMask) {
            this(dimension, visual, from, to, width, start, color, bladeMask, -1);
        }
        public boolean replaces(SkillEffect previous) {
            return projectileId >= 0 && projectileId == previous.projectileId
                    && dimension.equals(previous.dimension);
        }
        public boolean emptyBlade() {
            return (visual == Visual.QI_RISING || visual == Visual.QI_FALLING || visual == Visual.QI_LEVEL)
                    && bladeMask.empty();
        }
        public static final Type<SkillEffect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                "immortal_cultivation_mod", "higher_weapon_skill_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillEffect> CODEC = StreamCodec.of((b, p) -> {
            b.writeResourceLocation(p.dimension); b.writeEnum(p.visual); b.writeVec3(p.from);
            b.writeVec3(p.to); b.writeFloat(p.width); b.writeLong(p.start); b.writeInt(p.color); b.writeLong(p.bladeMask.low()); b.writeLong(p.bladeMask.high()); b.writeVarInt(p.projectileId);
        }, b -> new SkillEffect(b.readResourceLocation(), b.readEnum(Visual.class), b.readVec3(),
                b.readVec3(), b.readFloat(), b.readLong(), b.readInt(), new BladeMask(b.readLong(), b.readLong()), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void send(ServerPlayer owner, Visual visual, Vec3 from, Vec3 to, double width) {
        send(owner.serverLevel(), visual, from, to, width, 0xc0e9f2);
    }

    public static double clampWidth(Visual visual, double width) {
        double maximum = switch (visual) {
            case QI_RISING, QI_FALLING, QI_LEVEL, QI_CONE ->
                    com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.MAX_RADIUS;
            default -> 32;
        };
        return Math.clamp(width, .05, maximum);
    }

    public static void send(net.minecraft.server.level.ServerLevel level, Visual visual,
                            Vec3 from, Vec3 to, double width, int color) {
        send(level, visual, from, to, width, color, ExternalQiVfxShape.ALL_SECTIONS);
    }

    public static void send(net.minecraft.server.level.ServerLevel level, Visual visual,
                            Vec3 from, Vec3 to, double width, int color, BladeMask bladeMask) {
        send(level, visual, from, to, width, color, bladeMask, -1);
    }

    public static void send(net.minecraft.server.level.ServerLevel level, Visual visual,
                            Vec3 from, Vec3 to, double width, int color, BladeMask bladeMask, int projectileId) {
        var effect = new SkillEffect(level.dimension().location(), visual, from, to,
                (float) clampWidth(visual, width), level.getGameTime(), color & 0xffffff, bladeMask, projectileId);
        for (var player : level.players()) {
            if (player.distanceToSqr(from) < 96 * 96 || player.distanceToSqr(to) < 96 * 96) {
                PacketDistributor.sendToPlayer(player, effect);
            }
        }
    }

    /** Dedicated Player Animator skill clips are shared by weapons of the same family. */
    public static void animate(ServerPlayer player, HigherWeaponSpec spec, boolean charged) {
        WeaponUseAnimations.play(player, WeaponUseAnimations.type(spec));
    }

    public record Effect(ResourceLocation dimension, Vec3 center, long start, boolean starfall) implements CustomPacketPayload {
        public static final Type<Effect> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","higher_weapon_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Effect> CODEC=StreamCodec.of((b,p)->{
            b.writeResourceLocation(p.dimension);b.writeVec3(p.center);b.writeLong(p.start);b.writeBoolean(p.starfall);
        },b->new Effect(b.readResourceLocation(),b.readVec3(),b.readLong(),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void send(ServerPlayer owner,Vec3 center,boolean stars) {
        var effect=new Effect(owner.level().dimension().location(),center,owner.level().getGameTime(),stars);
        for(var player:owner.serverLevel().players())if(player.distanceToSqr(center)<96*96)PacketDistributor.sendToPlayer(player,effect);
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("6").playToClient(Effect.TYPE,Effect.CODEC,(p,c)->c.enqueueWork(()->
                com.example.immortal_cultivation_mod.client.HigherWeaponEffects.accept(p)));
        event.registrar("6").playToClient(SkillEffect.TYPE,SkillEffect.CODEC,(p,c)->c.enqueueWork(()->
                com.example.immortal_cultivation_mod.client.HigherWeaponEffects.accept(p)));
    }
}
