package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.spell.SolarTechniques;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Ashes, winter, physical suppression and solar pillars share the existing once-per-second pulse. */
public final class AdvancedMethodDomains {
    private AdvancedMethodDomains() { }
    static boolean handles(MethodDomainStyle style) {
        return style==MethodDomainStyle.RENEWAL_FIRE||style==MethodDomainStyle.ICE||style==MethodDomainStyle.BONE
                ||style==MethodDomainStyle.NETHER||style==MethodDomainStyle.SOLAR;
    }
    static void tick(QiDomainSystem.Aura aura,MethodDomainStyle style,long now) {
        if(style==MethodDomainStyle.NETHER)return;
        var targets=new ArrayList<>(MethodDomainEffects.targets(aura.owner));
        if(style==MethodDomainStyle.ICE)targets.removeIf(ColdTideFreeze::immune);
        targets.sort(Comparator.comparingDouble((LivingEntity e)->e.distanceToSqr(aura.owner)).thenComparingInt(LivingEntity::getId));
        if(targets.size()>MethodDomainEffects.MAX_CONTINUOUS_TARGETS)targets.subList(MethodDomainEffects.MAX_CONTINUOUS_TARGETS,targets.size()).clear();
        var present=new HashSet<UUID>();for(var target:targets)present.add(target.getUUID());
        aura.domainExposure.keySet().retainAll(present);
        if(style==MethodDomainStyle.RENEWAL_FIRE||style==MethodDomainStyle.SOLAR)
            aura.owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,false,false));
        for(var target:targets) {
            switch(style) {
                case RENEWAL_FIRE -> {
                    if(Math.abs(target.getY()-aura.owner.getY())>3&&!target.isOnFire())continue;
                    MethodDomainEffects.effect(aura,aura.owner,target,QiAuraPayloads.DomainEffect.FIRE_PLUME,1);
                    float before=target.getHealth();
                    if(QiDomainDamage.hurt(aura.owner,target,QiDomainDamage.amount(aura.owner,aura.power)*.75F,false)) {
                        target.igniteForSeconds(3);
                        float dealt=Math.max(0,before-target.getHealth());
                        float own=aura.owner.getHealth();aura.owner.heal(dealt*.05F);
                        if(aura.owner.getHealth()>own)
                            MethodDomainEffects.effect(aura,aura.owner,target,QiAuraPayloads.DomainEffect.VITALITY,1);
                    }
                }
                case ICE -> {
                    // Coating, partial thaw and full action lock must share one authoritative state.
                    ColdTideFreeze.expose(aura.owner,target,now);
                    float progress=ColdTideFreeze.progress(target);
                    int stage=Math.clamp((int)Math.ceil(progress*8),1,8);aura.domainExposure.put(target.getUUID(),stage);
                    int coverage=progress>=1?255:Math.clamp(Math.round(progress*255),1,254);
                    MethodDomainEffects.effect(aura,aura.owner,target,QiAuraPayloads.DomainEffect.WINTER,coverage);
                }
                case BONE -> {
                    if(bodyStrength(target)>=bodyStrength(aura.owner))continue;
                    // Physical suppression must not be erased by beast abilities that clear ordinary Slowness.
                    target.addEffect(new MobEffectInstance(ModEffects.BONE_BELL,40,0,false,false));
                    MethodDomainEffects.effect(aura,aura.owner,target,QiAuraPayloads.DomainEffect.BONE,1);
                }
                case SOLAR -> {
                    float sun=sunEnergy(aura.owner);
                    if(sun<=0)continue;
                    float amount=solarDamage(sun,aura.owner.getMaxHealth())*aura.power;
                    QiDomainDamage.hurt(aura.owner,target,amount,false);
                    target.igniteForSeconds(2);
                    MethodDomainEffects.effect(aura,aura.owner,target,QiAuraPayloads.DomainEffect.SOLAR,Math.max(1,Math.round(sun)));
                }
                default -> { }
            }
        }
        if(style==MethodDomainStyle.ICE)MethodDomainEffects.restoreTideVitality(aura);
    }
    /** Percentage value: 50 means 5% of the caster's maximum HP, with no added flat magic damage. */
    public static float solarDamage(float sunPercent,float casterMaxHp) {
        if(!Float.isFinite(sunPercent)||!Float.isFinite(casterMaxHp))return 0;
        return Math.clamp(sunPercent,0,100)*.001F*Math.max(0,casterMaxHp);
    }
    public static float sunEnergy(LivingEntity owner) {
        return SolarTechniques.solarPowerFor(owner);
    }
    /** Innate attack, trained physical attack and body durability (one point per twenty maximum HP).
     * Body tempering trains maximum HP, so an attack-only comparison ignored that training entirely.
     * Current wounds and weapon attack modifiers never change the comparison. */
    public static double bodyStrength(LivingEntity entity) {
        var attack=entity.getAttribute(Attributes.ATTACK_DAMAGE);
        double base=attack==null?0:Math.max(0,attack.getBaseValue());
        if(entity instanceof ServerPlayer player)base+=ModAttachments.getData(player).physicalAttack();
        return base+Math.max(0,entity.getMaxHealth())/20.0;
    }
}
