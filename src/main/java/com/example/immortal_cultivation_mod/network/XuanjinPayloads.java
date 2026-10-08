package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.companion.XuanjinInterface;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Clients send only an action and screen nonce; companion state stays authoritative. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class XuanjinPayloads {
    private XuanjinPayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,id));
    }
    public record View(UUID session, CompoundTag state, Component feedback, boolean open, boolean close) implements CustomPacketPayload {
        public static final Type<View> TYPE=XuanjinPayloads.type("xuanjin_view");
        public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.of((b,p)->{
            b.writeUUID(p.session);b.writeNbt(p.state);ComponentSerialization.STREAM_CODEC.encode(b,p.feedback);
            b.writeBoolean(p.open);b.writeBoolean(p.close);
        },b->new View(b.readUUID(),java.util.Objects.requireNonNullElseGet(b.readNbt(),CompoundTag::new),
                ComponentSerialization.STREAM_CODEC.decode(b),b.readBoolean(),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Action(UUID session,String action) implements CustomPacketPayload {
        public static final Type<Action> TYPE=XuanjinPayloads.type("xuanjin_action");
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeUtf(p.action,24);},b->new Action(b.readUUID(),b.readUtf(24)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("1");
        r.playToClient(View.TYPE,View.CODEC,(p,c)->com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.handleXuanjinView(p,c));
        r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player() instanceof ServerPlayer player) XuanjinInterface.action(player,p.session,p.action);
        }));
        r.playToServer(Cast.TYPE,Cast.CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player() instanceof ServerPlayer player)XuanjinInterface.castSelected(player);
        }));
        r.playToClient(Effect.TYPE,Effect.CODEC,(p,c)->c.enqueueWork(()->com.example.immortal_cultivation_mod.client.XuanjinEffects.accept(p)));
    }
    public enum EffectKind { CLAW, BITE, ROAR, TAILS, DEVOUR, VORTEX, RETURN, REFINE, DASH, SIGHT, MANIFEST }
    public record Effect(ResourceLocation dimension,EffectKind kind,int actorId,net.minecraft.world.phys.Vec3 origin,
                         net.minecraft.world.phys.Vec3 target,int duration) implements CustomPacketPayload {
        public static final Type<Effect> TYPE=XuanjinPayloads.type("xuanjin_effect");
        public static final StreamCodec<RegistryFriendlyByteBuf,Effect> CODEC=StreamCodec.of((b,p)->{
            b.writeResourceLocation(p.dimension);b.writeEnum(p.kind);b.writeVarInt(p.actorId);
            b.writeVec3(p.origin);b.writeVec3(p.target);b.writeVarInt(p.duration);
        },b->new Effect(b.readResourceLocation(),b.readEnum(EffectKind.class),b.readVarInt(),b.readVec3(),b.readVec3(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Cast() implements CustomPacketPayload {
        public static final Type<Cast> TYPE=XuanjinPayloads.type("xuanjin_cast");
        public static final StreamCodec<RegistryFriendlyByteBuf,Cast> CODEC=StreamCodec.unit(new Cast());
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
