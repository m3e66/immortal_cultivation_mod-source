package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.client.SpellRibbonRenderer;
import com.example.immortal_cultivation_mod.effect.SpellRibbonShapes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
public record SpellRibbonPayload(ResourceLocation dimension,String spell,Vec3 position,Vec3 direction,
                                 int entityId,int duration,float radius) implements CustomPacketPayload {
    public static final Type<SpellRibbonPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","spell_ribbon"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpellRibbonPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeResourceLocation(p.dimension);b.writeUtf(p.spell,64);b.writeVec3(p.position);b.writeVec3(p.direction);
        b.writeInt(p.entityId);b.writeVarInt(p.duration);b.writeFloat(p.radius);
    },b->new SpellRibbonPayload(b.readResourceLocation(),b.readUtf(64),b.readVec3(),b.readVec3(),b.readInt(),b.readVarInt(),b.readFloat()));
    @Override public Type<SpellRibbonPayload> type(){return TYPE;}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(TYPE,CODEC,(p,context)->{
            if(FMLEnvironment.dist==Dist.CLIENT)context.enqueueWork(()->SpellRibbonRenderer.accept(p));
        });
    }
    public static void send(ServerLevel level,String spell,Vec3 pos,Vec3 direction,int entity,int ticks,float radius){
        if(!SpellRibbonShapes.SPELLS.contains(spell) && !"fire_burst".equals(spell))return;
        PacketDistributor.sendToPlayersNear(level,null,pos.x,pos.y,pos.z,112,
                new SpellRibbonPayload(level.dimension().location(),spell,pos,direction,entity,ticks,radius));
    }
    public static void cast(LivingEntity caster,String spell,Vec3 before){
        if(!(caster.level() instanceof ServerLevel level))return;
        if(spell.equals("leishan")){
            send(level,spell,before.add(0,1,0),caster.position().subtract(before),-1,18,1);
            send(level,spell,caster.position().add(0,1,0),caster.getLookAngle(),-1,18,1);
        }else if(spell.equals("yinlei_jue")||spell.equals("wulei_zhengfa")){
            send(level,spell,caster.position(),new Vec3(0,0,1),caster.getId(),14,.65F);
        }else if(spell.equals("ignite_flare")){
            send(level,spell,caster.getEyePosition().add(caster.getLookAngle().scale(.8)).add(0,-.3,0),new Vec3(0,0,1),-1,12,.25F);
        }else if(spell.equals("shengxi_baofa")){
            send(level,spell,caster.position(),caster.getLookAngle(),caster.getId(),30,1);
        }
    }
}
