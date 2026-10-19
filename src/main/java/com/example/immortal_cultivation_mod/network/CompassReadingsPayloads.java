package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.client.CompassNavigation;
import com.example.immortal_cultivation_mod.world.YinQiField;
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

/** Small live readings; never rescans the full map or loads terrain chunks. */
@EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
public final class CompassReadingsPayloads {
    private CompassReadingsPayloads() {}
    public record Reading(ResourceLocation dimension,int x,int y,int z,int yin,int qi) implements CustomPacketPayload {
        public static final Type<Reading> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","compass_reading"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Reading> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.x);b.writeVarInt(v.y);b.writeVarInt(v.z);b.writeByte(v.yin);b.writeByte(v.qi);
        },b->new Reading(b.readResourceLocation(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readByte(),b.readByte()));
        @Override public Type<Reading> type(){return TYPE;}
    }
    public static void send(ServerPlayer player){
        var pos=player.blockPosition();var level=player.serverLevel();
        PacketDistributor.sendToPlayer(player,new Reading(level.dimension().location(),pos.getX(),pos.getY(),pos.getZ(),YinQiField.sample(level,pos),YinQiField.sampleQi(level,pos)));
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(Reading.TYPE,Reading.CODEC,(packet,ctx)->{
            if(FMLEnvironment.dist.isClient())ctx.enqueueWork(()->CompassNavigation.accept(packet));
        });
    }
}
