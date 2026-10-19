package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Read-only self inspection. No target UUID or edit action is accepted from the player. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ShentongPayloads {
    private ShentongPayloads() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,path); }
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("shentong_details_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC = StreamCodec.unit(new Request());
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Details(DebugShentongPayloads.State state) implements CustomPacketPayload {
        public static final Type<Details> TYPE = new Type<>(id("shentong_details"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Details> CODEC = StreamCodec.of(
                (b,p) -> DebugShentongPayloads.State.CODEC.encode(b,p.state),
                b -> new Details(DebugShentongPayloads.State.CODEC.decode(b)));
        @Override public Type<Details> type() { return TYPE; }
    }
    public record Activated(UUID casterId,String abilityId) implements CustomPacketPayload {
        public Activated {
            if(casterId==null||abilityId==null||abilityId.length()>96||!abilityId.matches("[a-z0-9_.:/-]+"))throw new IllegalArgumentException("Invalid shentong activation");
        }
        public static final Type<Activated> TYPE=new Type<>(id("shentong_activated"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Activated> CODEC=StreamCodec.of(
                (b,p)->{b.writeUUID(p.casterId);b.writeUtf(p.abilityId,96);},b->new Activated(b.readUUID(),b.readUtf(96)));
        @Override public Type<Activated> type(){return TYPE;}
    }
    public static DebugShentongPayloads.State snapshot(ServerPlayer player) {
        var all = DebugShentongPayloads.snapshot(player);
        return new DebugShentongPayloads.State(player.getUUID(),all.abilities().stream()
                .filter(DebugShentongPayloads.AbilityState::owned).toList());
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(Activated.TYPE,Activated.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->
                    com.example.immortal_cultivation_mod.client.hud.ShentongCalligraphyOverlay.activate(p.casterId,p.abilityId));
        });
        registrar.playToServer(Request.TYPE,Request.CODEC,(p,c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player,new Details(snapshot(player)));
        }));
        registrar.playToClient(Details.TYPE,Details.CODEC,(p,c) -> {
            if (FMLEnvironment.dist.isClient()) c.enqueueWork(() ->
                    com.example.immortal_cultivation_mod.client.ShentongClient.accept(p.state));
        });
    }
}
