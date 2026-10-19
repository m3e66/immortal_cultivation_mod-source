package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Method-specific authority: phased probability rolls and bounded continuous effects once per second. */
public final class MethodDomainEffects {
    public static final int DEBUFF_TICKS=200,WIND_SEAMS=14,WIND_CYCLE_TICKS=32;
    public static final int MAX_CONTINUOUS_TARGETS=24,MAX_STORM_ARCS=12;
    public static final double STORM_CHAIN_RADIUS=6;
    public static final float WIND_DAMAGE_MULTIPLIER=.5F;
    private MethodDomainEffects() { }

    static void tick(QiDomainSystem.Aura aura,long now,RandomSource random) {
        if(!aura.domain()||aura.power<=0||QiDomainSystem.mastery(aura.owner)<QiDomainSystem.GREAT_MASTERY)return;
        if(!aura.method.equals(aura.pulseMethod)) {
            aura.pulseMethod=aura.method;aura.domainExposure.clear();aura.lastMethodPulse=Long.MIN_VALUE;
        }
        // An entity-stable phase spreads load and prevents toggles/method swaps buying extra rolls.
        if(Math.floorMod(now-aura.owner.getId(),20)!=0)return;
        if(aura.lastMethodPulse!=Long.MIN_VALUE&&now-aura.lastMethodPulse<20)return;
        aura.lastMethodPulse=now;
        var style=MethodDomainStyle.of(aura.method);
        if(AdvancedMethodDomains.handles(style)) {
            AdvancedMethodDomains.tick(aura,style,now);
            return;
        }
        if(style==MethodDomainStyle.STORM||style==MethodDomainStyle.MOUNTAIN||style==MethodDomainStyle.COLD_TIDE||BloodLilyBloom.blood(style)) {
            continuous(aura,style,now);
            return;
        }
        if(style.kind!=MethodDomainStyle.Kind.REINCARNATION&&style.kind!=MethodDomainStyle.Kind.DEVOUR
                &&style.kind!=MethodDomainStyle.Kind.BLOOD&&style.kind!=MethodDomainStyle.Kind.DRAIN
                &&style.kind!=MethodDomainStyle.Kind.CROWN)return;
        var victims=targets(aura.owner);
        if(style==MethodDomainStyle.DEVOUR) {
            var present=new HashSet<java.util.UUID>();for(var victim:victims)present.add(victim.getUUID());
            aura.domainExposure.keySet().retainAll(present);
        }
        for(var victim:victims) {
            switch(style.kind) {
                case REINCARNATION -> {
                    if(random.nextInt(100)==0) {
                        increment(victim,MobEffects.MOVEMENT_SLOWDOWN);
                        increment(victim,MobEffects.WEAKNESS);
                        effect(aura,victim,QiAuraPayloads.DomainEffect.REBIRTH);
                    }
                }
                case DEVOUR -> {
                    int chance=aura.domainExposure.merge(victim.getUUID(),1,(old,value)->Math.min(100,old+value));
                    if(random.nextInt(100)<chance) {
                        aura.domainExposure.put(victim.getUUID(),0);
                        // This is HP damage based on the caster's maximum Qi, never a Qi payment.
                        float amount=maxQi(aura.owner)*.10F*aura.power;
                        if(amount>0)QiDomainDamage.hurt(aura.owner,victim,amount,false);
                        effect(aura,victim,QiAuraPayloads.DomainEffect.WARP);
                    }
                }
                default -> { }
            }
        }
    }

    /** One bounded, server-authoritative pulse per second; no attacks or mote collisions required. */
    private static void continuous(QiDomainSystem.Aura aura,MethodDomainStyle style,long now) {
        var nearby=new ArrayList<>(targets(aura.owner));
        nearby.sort(Comparator.comparingDouble((LivingEntity e)->e.distanceToSqr(aura.owner)).thenComparingInt(LivingEntity::getId));
        if(nearby.size()>MAX_CONTINUOUS_TARGETS)nearby.subList(MAX_CONTINUOUS_TARGETS,nearby.size()).clear();
        var present=new HashSet<java.util.UUID>();
        for(var victim:nearby)present.add(victim.getUUID());
        aura.domainExposure.keySet().retainAll(present);
        if(style!=MethodDomainStyle.COLD_TIDE)
            for(var victim:nearby)aura.domainExposure.merge(victim.getUUID(),1,(old,value)->Math.min(8,old+1));
        if(style==MethodDomainStyle.STORM) {
            storm(aura,nearby,now);
            return;
        }
        for(var victim:nearby) {
            if(BloodLilyBloom.blood(style)) {
                BloodLilyBloom.expose(aura,victim,now);
            }else if(style==MethodDomainStyle.MOUNTAIN) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0,false,false));
                if(!victim.onGround()) {
                    Vec3 motion=victim.getDeltaMovement();
                    victim.setDeltaMovement(motion.x,Math.min(motion.y,Math.max(-.25,motion.y-.035)),motion.z);
                    victim.hurtMarked=true;
                }
                int crowd=(int)nearby.stream().filter(other->other.distanceToSqr(victim)<=16).count();
                effect(aura,aura.owner,victim,QiAuraPayloads.DomainEffect.MOUNTAIN,Math.min(8,crowd));
            }else {
                TideWaterStacks.expose(aura,victim,now);
                int stacks=TideWaterStacks.stacks(victim);aura.domainExposure.put(victim.getUUID(),stacks);
                if(stacks>0)effect(aura,aura.owner,victim,QiAuraPayloads.DomainEffect.TIDE_WATER,stacks);
            }
        }
        if(style==MethodDomainStyle.COLD_TIDE)restoreTideVitality(aura);
    }
    static void restoreTideVitality(QiDomainSystem.Aura aura) {
        float before=aura.owner.getHealth();
        aura.owner.heal(Math.min(4,aura.owner.getMaxHealth()*.002F)*aura.power);
        aura.owner.clearFire();
        if(aura.owner.getHealth()>before)effect(aura,aura.owner,aura.owner,QiAuraPayloads.DomainEffect.HEAL,1);
    }

    private static void storm(QiDomainSystem.Aura aura,List<LivingEntity> targets,long now) {
        int arcs=0;
        var edges=new HashSet<Long>();
        for(var source:targets) {
            boolean charged=aura.domainExposure.get(source.getUUID())>=3&&Math.floorMod(now/20+source.getId(),3)==0;
            int branches=charged?2:1,emitted=0;
            var others=targets.stream().filter(other->other!=source&&source.distanceToSqr(other)<=STORM_CHAIN_RADIUS*STORM_CHAIN_RADIUS
                    &&source.hasLineOfSight(other)).sorted(Comparator.comparingDouble((LivingEntity other)->source.distanceToSqr(other))).toList();
            for(var target:others) {
                if(arcs>=MAX_STORM_ARCS)break;
                long key=((long)Math.min(source.getId(),target.getId())<<32)|(Math.max(source.getId(),target.getId())&0xFFFFFFFFL);
                if(!edges.add(key))continue;
                // Shared contact cooldown prevents a crowded electrical network multiplying damage per victim.
                QiDomainDamage.hurt(aura.owner,target,QiDomainDamage.amount(aura.owner,aura.power)*.35F,false);
                QiDomainDamage.hurt(aura.owner,source,QiDomainDamage.amount(aura.owner,aura.power)*.35F,false);
                effect(aura,source,target,QiAuraPayloads.DomainEffect.ARC,charged?2:1);
                arcs++;if(++emitted>=branches)break;
            }
            if(charged&&others.isEmpty()&&arcs<MAX_STORM_ARCS) {
                QiDomainDamage.hurt(aura.owner,source,QiDomainDamage.amount(aura.owner,aura.power)*.35F,false);
                effect(aura,aura.owner,source,QiAuraPayloads.DomainEffect.ARC,2);arcs++;
            }
        }
    }

    private static void increment(LivingEntity victim,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        var old=victim.getEffect(effect);
        int amplifier=old==null?0:Math.min(255,old.getAmplifier()+1);
        // Vanilla retains a pre-existing longer effect underneath this ten-second higher level.
        victim.addEffect(new MobEffectInstance(effect,DEBUFF_TICKS,amplifier,false,false));
    }

    static List<LivingEntity> targets(LivingEntity owner) {
        var volume=QiDomainSystem.volume(owner);
        return owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(QiDomainSystem.DOMAIN_RADIUS),
                e->e.level()==owner.level()&&QiDomainSystem.canDomainAffect(owner,e)
                        &&volume.intersects(e.getBoundingBox()));
    }

    static void mark(QiDomainSystem.Aura aura,LivingEntity victim) {
        BloodLilyBloom.expose(aura,victim,victim.level().getGameTime());
    }

    public static int maxQi(LivingEntity victim) {
        if(victim instanceof ServerPlayer p)return ModAttachments.getData(p).maxQi();
        if(victim instanceof PathfinderMob mob&&victim instanceof CultivationNpc npc)return NpcProfile.maxQi(mob,npc.npcArchetype());
        if(victim instanceof RealmBeast beast)return CultivationLevels.getLevelDef(beast.beastCultivationLevel()).maxQi();
        if(victim instanceof XuanjinZhengEntity zheng)return CultivationLevels.getLevelDef(
                CultivationLevels.allLevels().get(com.example.immortal_cultivation_mod.companion.XuanjinRules.stage(zheng.seal()).realmStage())).maxQi();
        if(victim instanceof EscapedYuanyingCoreEntity core)return CultivationLevels.getLevelDef(BeastRealms.level(core.coreRealm())).maxQi();
        return CultivationLevels.LEVEL_MORTAL.maxQi();
    }

    /** Moving server collision paths; clients show cutting flashes only after accepted contact. */
    public static Vec3 windStart(Vec3 origin,int index,double tick) {
        int parent=index%10;double clock=tick+parent*13,cycle=Math.floor(clock/WIND_CYCLE_TICKS),age=clock-cycle*WIND_CYCLE_TICKS;
        double angle=windHash(parent*19+cycle*47)*Math.PI*2;
        double lane=(windHash(parent*31+cycle*11)-.5)*22;
        double extent=Math.sqrt(17*17-lane*lane),travel=(age/WIND_CYCLE_TICKS*2-1)*extent;
        double curve=Math.sin(age/WIND_CYCLE_TICKS*Math.PI)*(windHash(parent+cycle*23)-.5)*3;
        Vec3 start=origin.add(Math.cos(angle)*travel-Math.sin(angle)*(lane+curve),
                .55+windHash(parent*7+cycle)*1.5+Math.sin(tick*.035+parent)*.18,
                Math.sin(angle)*travel+Math.cos(angle)*(lane+curve));
        if(index>=10)start=start.add(Math.cos(angle+.8)*1.1,.18,Math.sin(angle+.8)*1.1);
        return start;
    }
    public static Vec3 windEnd(Vec3 origin,int index,double tick) {
        int parent=index%10;double cycle=Math.floor((tick+parent*13)/WIND_CYCLE_TICKS);
        double angle=windHash(parent*19+cycle*47)*Math.PI*2+(index>=10?.65:0);
        double length=index>=10?1.8:3.0+windHash(parent+cycle*3)*2;
        return windStart(origin,index,tick).add(-Math.sin(angle)*length,
                Math.sin(parent*1.7+cycle*2.3)*1.25,Math.cos(angle)*length);
    }
    public static float windVisibility(int index,double tick) {
        double clock=tick+(index%10)*13,age=clock-Math.floor(clock/WIND_CYCLE_TICKS)*WIND_CYCLE_TICKS;
        if(index>=10)return (float)Math.max(0,Math.sin(Math.clamp((age-8)/16,0,1)*Math.PI));
        return (float)Math.min(1,Math.min(age/3,(WIND_CYCLE_TICKS-age)/4));
    }
    /** A respawned cut starts a new sweep; never join opposite sides of the field. */
    public static boolean sameWindSweep(int index,double before,double now) {
        double offset=(index%10)*13;
        return Math.floor((before+offset)/WIND_CYCLE_TICKS)==Math.floor((now+offset)/WIND_CYCLE_TICKS);
    }
    private static double windHash(double x){double n=Math.sin(x*127.1+311.7)*43758.5453;return n-Math.floor(n);}
    static void windContacts(QiDomainSystem.Aura aura,long now) {
        var origin=aura.owner.position();
        for(var victim:targets(aura.owner))for(int i=0;i<WIND_SEAMS;i++) {
            if(windVisibility(i,now)<=.05F)continue;
            Vec3 start=windStart(origin,i,now),end=windEnd(origin,i,now);
            Vec3 before=windStart(origin,i,now-2),beforeEnd=windEnd(origin,i,now-2);
            AABB box=victim.getBoundingBox().inflate(.32);
            boolean contact=touches(box,start,end);
            // A seam wrapping at the domain boundary cannot sweep across the whole arena.
            if(!contact&&windVisibility(i,now-2)>.05F&&sameWindSweep(i,now-2,now))
                contact=touches(box,before,beforeEnd)||touches(box,before.lerp(start,.5),beforeEnd.lerp(end,.5))
                        ||touches(box,before,start)||touches(box,beforeEnd,end);
            if(contact) {
                if(QiDomainDamage.hurt(aura.owner,victim,QiDomainDamage.amount(aura.owner,aura.power)*WIND_DAMAGE_MULTIPLIER,false)) {
                    victim.push(0,.25,0);victim.hurtMarked=true;burst(aura,victim.getBoundingBox().getCenter());
                    effect(aura,victim,QiAuraPayloads.DomainEffect.WIND_LIFT);
                }
                break;
            }
        }
    }
    private static boolean touches(AABB box,Vec3 a,Vec3 b) {return box.contains(a)||box.contains(b)||box.clip(a,b).isPresent();}
    private static void burst(QiDomainSystem.Aura aura,Vec3 p) {
        var level=(ServerLevel)aura.owner.level();
        PacketDistributor.sendToPlayersNear(level,null,p.x,p.y,p.z,64,
                new QiAuraPayloads.Burst(level.dimension().location(),p.x,p.y,p.z,MethodDomainStyle.of(aura.method).ordinal()));
    }
    private static void effect(QiDomainSystem.Aura aura,LivingEntity victim,int kind) {
        effect(aura,aura.owner,victim,kind,1);
    }
    static void effect(QiDomainSystem.Aura aura,LivingEntity source,LivingEntity victim,int kind,int strength) {
        var owner=aura.owner;var level=(ServerLevel)owner.level();
        PacketDistributor.sendToPlayersNear(level,null,owner.getX(),owner.getY(),owner.getZ(),96,
                new QiAuraPayloads.DomainEffect(level.dimension().location(),source.getId(),source.getUUID(),
                        victim.getId(),victim.getUUID(),MethodDomainStyle.of(aura.method).ordinal(),kind,strength));
    }
}
