package com.example.immortal_cultivation_mod.companion;

import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import static com.example.immortal_cultivation_mod.network.XuanjinPayloads.EffectKind;

/** One description per cast; nearby clients animate the ash and ember trails locally. */
public final class XuanjinVisuals {
    public static void emit(LivingEntity actor,EffectKind kind,Vec3 target,int ticks) {
        if(!(actor.level() instanceof ServerLevel level))return;
        Vec3 origin=kind==EffectKind.ROAR?actor.getEyePosition():actor.position().add(0,actor.getBbHeight()*.65,0);
        var packet=new XuanjinPayloads.Effect(level.dimension().location(),kind,actor.getId(),origin,target,ticks);
        for(var player:level.players())if(player.distanceToSqr(actor)<96*96
                && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer))PacketDistributor.sendToPlayer(player,packet);
    }
    public static void emit(LivingEntity actor,EffectKind kind,int ticks) {
        emit(actor,kind,actor.getEyePosition().add(actor.getLookAngle().scale(kind==EffectKind.ROAR?XuanjinRules.ROAR_RANGE:3)),ticks);
    }
}
