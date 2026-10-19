package com.example.immortal_cultivation_mod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record PillToxicityPayload(CompoundTag state,CompoundTag rules) implements CustomPacketPayload {
    public static final Type<PillToxicityPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","pill_toxicity"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PillToxicityPayload> STREAM_CODEC=StreamCodec.of(
        (b,p)->{b.writeNbt(p.state);b.writeNbt(p.rules);},b->new PillToxicityPayload(b.readNbt(),b.readNbt()));
    public PillToxicityPayload {state=state==null?new CompoundTag():state.copy();rules=rules==null?new CompoundTag():rules.copy();}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e) {
            e.registrar("1").playToClient(TYPE,STREAM_CODEC,(p,c)->c.enqueueWork(()->com.example.immortal_cultivation_mod.client.PillToxicityClient.receive(p)));
        }
    }
}
