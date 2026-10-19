package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Fixed-size, validated snapshots; never serializes an entity's inventory or complete NBT. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class CursePayloads {
    private CursePayloads() { }
    public record Status(ResourceLocation dimension,int entityId,UUID uuid,float coverage,float blackHp,
                         boolean noHealing,boolean owned,int cooldown) implements CustomPacketPayload {
        public Status {
            if(!Float.isFinite(coverage)||coverage<0||coverage>1||!Float.isFinite(blackHp)||blackHp<0
                    ||cooldown<0||cooldown>AnnihilationCurse.RESCUE_COOLDOWN)throw new IllegalArgumentException("Invalid curse state");
        }
        public static final Type<Status> TYPE=new Type<>(id("annihilation_curse"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Status> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);
            b.writeFloat(v.coverage);b.writeFloat(v.blackHp);b.writeBoolean(v.noHealing);b.writeBoolean(v.owned);b.writeVarInt(v.cooldown);
        },b->new Status(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readVarInt()));
        @Override public Type<Status> type(){return TYPE;}
    }
    public record Outbreak(ResourceLocation dimension,double x,double y,double z,int targetId,UUID targetUuid,int duration) implements CustomPacketPayload {
        public static final Type<Outbreak> TYPE=new Type<>(id("curse_outbreak"));
        public Outbreak {if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||duration<20||duration>CurseOutbreaks.MAX_TRAVEL_TICKS)throw new IllegalArgumentException("Invalid curse outbreak");}
        public static final StreamCodec<RegistryFriendlyByteBuf,Outbreak> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);
            b.writeVarInt(v.targetId);b.writeUUID(v.targetUuid);b.writeVarInt(v.duration);
        },b->new Outbreak(b.readResourceLocation(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readUUID(),b.readVarInt()));
        @Override public Type<Outbreak> type(){return TYPE;}
    }
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,name);}
    public static Status status(LivingEntity target) {
        return new Status(target.level().dimension().location(),target.getId(),target.getUUID(),AnnihilationCurse.coverage(target),
                AnnihilationCurse.blackHp(target),AnnihilationCurse.healingLocked(target),AnnihilationCurse.owned(target),AnnihilationCurse.cooldown(target));
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("2");
        r.playToClient(Status.TYPE,Status.CODEC,(p,c)->{if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.curse.CurseClient.accept(p));});
        r.playToClient(Outbreak.TYPE,Outbreak.CODEC,(p,c)->{if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.curse.CurseClient.outbreak(p));});
    }
}
