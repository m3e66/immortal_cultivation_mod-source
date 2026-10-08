package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.ThoughtControlGuard;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client-only input cancellation is reported; the server owns the cost and acknowledges the remaining bar. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID, bus=EventBusSubscriber.Bus.MOD)
public final class ThoughtControlPayloads {
    public record Attempt(int sequence) implements CustomPacketPayload {
        public static final Type<Attempt> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"input_control_attempt"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Attempt> CODEC = StreamCodec.of(
                (b,p)->b.writeVarInt(p.sequence), b->new Attempt(b.readVarInt()));
        @Override public Type<Attempt> type(){return TYPE;}
    }
    public record Balance(int sequence, int thoughts) implements CustomPacketPayload {
        public static final Type<Balance> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"input_control_balance"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Balance> CODEC = StreamCodec.of(
                (b,p)->{b.writeVarInt(p.sequence);b.writeVarInt(p.thoughts);}, b->new Balance(b.readVarInt(),b.readVarInt()));
        @Override public Type<Balance> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("1");
        registrar.playToServer(Attempt.TYPE,Attempt.CODEC,(payload,context)->context.enqueueWork(()->{
            if (!(context.player() instanceof ServerPlayer player) || payload.sequence < 0) return;
            if (player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu)
                ThoughtControlGuard.resist(player,"generic_input",null,ThoughtControlGuard.BASE_DAMAGE);
            PacketDistributor.sendToPlayer(player,new Balance(payload.sequence,ModAttachments.getData(player).thoughts()));
        }));
        registrar.playToClient(Balance.TYPE,Balance.CODEC,(payload,context)->{
            if(FMLEnvironment.dist==Dist.CLIENT)context.enqueueWork(()->
                    com.example.immortal_cultivation_mod.client.ThoughtInputGuard.accept(payload));
        });
    }
}
