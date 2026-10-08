package com.example.immortal_cultivation_mod.hair;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record HairPayload(int menu, int style, int color, int helmet, boolean saveColor) implements CustomPacketPayload {
    public static final Type<HairPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","hair_apply"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HairPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.style);b.writeInt(p.color);b.writeVarInt(p.helmet);b.writeBoolean(p.saveColor);},
            b->new HairPayload(b.readVarInt(),b.readVarInt(),b.readInt(),b.readVarInt(),b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e) {
            e.registrar("1").playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->{
                var player=c.player();
                if(player.containerMenu instanceof HairMenu menu && menu.containerId==p.menu
                        && menu.apply(player,p.style,p.color,p.helmet,p.saveColor)) {
                    if (player instanceof net.minecraft.server.level.ServerPlayer server) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "hair_saved"); }
                    player.closeContainer();
                }
            }));
        }
    }
}
