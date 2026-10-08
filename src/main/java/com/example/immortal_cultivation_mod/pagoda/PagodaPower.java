package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import com.example.immortal_cultivation_mod.externalpower.ExternalPowerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public final class PagodaPower {
    public enum Band { TRAPPED, SLOW, SERIOUS, FORCED }
    public static Band band(double power, double containment) {
        double ratio=Math.max(0,power)/Math.max(1,containment);
        return ratio<.6?Band.TRAPPED:ratio<1?Band.SLOW:ratio<1.5?Band.SERIOUS:Band.FORCED;
    }
    public static double breakPower(double realm,double health,double attack,double armor,double qiFraction,double progress) {
        return Math.max(0,realm)+Math.max(0,health)*.35+Math.max(0,attack)*4+Math.max(0,armor)*2
                +Math.clamp(qiFraction,0,1)*Math.max(0,realm)*.30+Math.max(0,progress);
    }
    public static double containment(double realm,double injectedDamage,double health,double qiFraction) {
        return Math.max(1,realm*1.25+Math.max(0,injectedDamage)*8+Math.max(0,health)*.20
                +Math.clamp(qiFraction,0,1)*realm*.25);
    }
    public static double realm(LivingEntity entity) {
        int r=com.example.immortal_cultivation_mod.externalpower.ExternalPowerTargets.realm(entity);
        return r<=0?0:50*Math.pow(4,Math.min(12,r-1));
    }
    public static double qiFraction(LivingEntity e) {
        if(e instanceof Player p) {
            var d=ModAttachments.getData(p);
            return Math.clamp((double)d.qi()/Math.max(1,(long)CultivationLevels.getLevelDef(d.cultivationLevel()).maxQi()+d.maxQiBonus()),0,1);
        }
        return e instanceof CultivationNpc n?Math.clamp((double)n.npcQi()/Math.max(1,n.npcMaxQi()),0,1):0;
    }
    public static boolean boss(LivingEntity e) {
        return e instanceof EnderDragon || e instanceof WitherBoss
                || e.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("c","bosses")));
    }
    public static double power(LivingEntity e,double progress) {
        double result=breakPower(realm(e),e.getMaxHealth(),PagodaSystem.attack(e),e.getArmorValue(),qiFraction(e),progress);
        String namespace=BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace();
        if(realm(e)==0 && !namespace.equals("minecraft") && !namespace.equals("immortal_cultivation_mod")) {
            var external=ExternalPowerConfig.read();
            if(external.enabled()){
                var override=com.example.immortal_cultivation_mod.externalpower.ExternalPowerOverrides.get(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
                int tier=Math.clamp(override==null?external.minimum():override.powerTier(),0,external.maximum());
                if(tier>0)result+=50*Math.pow(4,Math.min(12,tier-1));
            }
            result*=PagodaConfig.EXTERNAL_SCALE.get();
        }
        return Math.min(1E12,result*(boss(e)?PagodaConfig.BOSS_MULTIPLIER.get():1));
    }
    private PagodaPower() {}
}
