package com.example.immortal_cultivation_mod.network;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Visual-only forge activity; does not enable cultivation or grant meditation bonuses. */
public record ForgingVisualPayload(UUID player, ResourceLocation dimension,
        ResourceLocation forgeDimension, BlockPos forge, boolean active) implements CustomPacketPayload {
    public static final Type<ForgingVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "forging_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ForgingVisualPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeResourceLocation(p.dimension);b.writeResourceLocation(p.forgeDimension);b.writeBlockPos(p.forge);b.writeBoolean(p.active);},
            b->new ForgingVisualPayload(b.readUUID(),b.readResourceLocation(),b.readResourceLocation(),b.readBlockPos(),b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

    @EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->{
                if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)
                    context.enqueueWork(()->com.example.immortal_cultivation_mod.client.ForgingVisualState.receive(payload));
            });
        }
    }
}
