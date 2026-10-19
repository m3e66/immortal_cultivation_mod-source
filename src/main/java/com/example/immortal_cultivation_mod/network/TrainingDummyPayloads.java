package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.training.TrainingDummyControls;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class TrainingDummyPayloads {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,path);}
    public record Request(UUID dummyId) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(id("training_dummy_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->b.writeUUID(p.dummyId),b->new Request(b.readUUID()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Edit(UUID dummyId,String action,String key,String value) implements CustomPacketPayload {
        public static final Type<Edit> TYPE=new Type<>(id("training_dummy_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.of((b,p)->{
            b.writeUUID(p.dummyId);b.writeUtf(p.action,24);b.writeUtf(p.key,96);b.writeUtf(p.value,256);
        },b->new Edit(b.readUUID(),b.readUtf(24),b.readUtf(96),b.readUtf(256)));
        @Override public Type<Edit> type(){return TYPE;}
    }
    public record State(UUID dummyId,CompoundTag data,boolean open) implements CustomPacketPayload {
        public State {data=data.copy();}
        public static final Type<State> TYPE=new Type<>(id("training_dummy_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{
            b.writeUUID(p.dummyId);b.writeNbt(p.data);b.writeBoolean(p.open);
        },b->{var uuid=b.readUUID();var tag=b.readNbt(NbtAccounter.create(65536));return new State(uuid,tag instanceof CompoundTag compound?compound:new CompoundTag(),b.readBoolean());});
        @Override public Type<State> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("1");
        r.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player){var dummy=TrainingDummyControls.resolve(player,p.dummyId);if(dummy!=null)TrainingDummyControls.respond(player,dummy,false);}}));
        r.playToServer(Edit.TYPE,Edit.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player&&!TrainingDummyControls.edit(player,p.dummyId,p.action,p.key,p.value)){
            var dummy=TrainingDummyControls.resolve(player,p.dummyId);if(dummy!=null){var state=TrainingDummyControls.snapshot(dummy);state.putString("Notice","action_blocked");net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new State(p.dummyId,state,false));}
        }}));
        r.playToClient(State.TYPE,State.CODEC,(p,c)->{if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.screen.TrainingDummyScreen.accept(p));});
    }
}
