package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import java.util.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real distance travelled in active water domains. Overlapping casters never count a step twice. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class TideWaterStacks {
    public static final int MAX_STACKS=8,DURATION=200;
    private static final int MAX_TRACKED=QiDomainSystem.MAX_ACTORS*MethodDomainEffects.MAX_CONTINUOUS_TARGETS;
    private static final class Motion {
        final Map<LivingEntity,Long> sources=new WeakHashMap<>();
        Vec3 last;double distance,rippleDistance;long tick,lastRipple=Long.MIN_VALUE,lastRefresh=Long.MIN_VALUE;
        Motion(LivingEntity target,long now){last=target.position();tick=now;}
    }
    private static final Map<LivingEntity,Motion> ACTIVE=new WeakHashMap<>();
    private TideWaterStacks() { }
    public static int stacks(LivingEntity target) {
        var wet=target.getEffect(ModEffects.DAMP);return wet==null?0:Math.min(MAX_STACKS,wet.getAmplifier()+1);
    }
    static void expose(QiDomainSystem.Aura aura,LivingEntity target,long now) {
        if(target.level().isClientSide||!target.isAlive())return;
        var motion=ACTIVE.get(target);
        if(motion==null){if(ACTIVE.size()>=MAX_TRACKED)return;motion=new Motion(target,now);ACTIVE.put(target,motion);}
        if(motion.sources.size()<8||motion.sources.containsKey(aura.owner))motion.sources.put(aura.owner,now+21);
    }
    private static boolean valid(LivingEntity source,LivingEntity target) {
        var aura=QiDomainSystem.view(source);
        return source.isAlive()&&!source.isRemoved()&&source.level()==target.level()&&aura!=null&&aura.domain()&&aura.power>0
                &&QiDomainSystem.mastery(source)>=QiDomainSystem.GREAT_MASTERY
                &&MethodDomainStyle.of(QiDomainSystem.method(source))==MethodDomainStyle.COLD_TIDE
                &&source.distanceToSqr(target)<=QiDomainSystem.DOMAIN_RADIUS*QiDomainSystem.DOMAIN_RADIUS
                &&QiDomainSystem.canDomainAffect(source,target);
    }
    static void update(LivingEntity target,long now) {
        var motion=ACTIVE.get(target);if(motion==null||now<=motion.tick)return;
        if(!target.isAlive()||target.isRemoved()){ACTIVE.remove(target);return;}
        motion.sources.entrySet().removeIf(e->e.getValue()<now||!valid(e.getKey(),target));
        if(motion.sources.isEmpty()){ACTIVE.remove(target);return;}
        var source=motion.sources.keySet().iterator().next();var aura=QiDomainSystem.view(source);
        double step=target.position().distanceTo(motion.last);motion.last=target.position();motion.tick=now;
        // Teleports and chunk corrections do not manufacture movement stacks.
        if(step>8){motion.distance=motion.rippleDistance=0;return;}
        if(step<.0001)return;
        motion.distance+=step;motion.rippleDistance+=step;int before=stacks(target),level=before;
        int gain=(int)Math.floor(motion.distance+1e-7);
        if(gain>0) {
            motion.distance=Math.max(0,motion.distance-gain);level=Math.min(MAX_STACKS,before+gain);
            if(level!=before||motion.lastRefresh==Long.MIN_VALUE||now-motion.lastRefresh>=20) {
                var old=target.getEffect(ModEffects.DAMP);int amplifier=Math.max(level-1,old==null?0:old.getAmplifier());
                target.addEffect(new MobEffectInstance(ModEffects.DAMP,DURATION,amplifier,false,true,false),source);
                target.clearFire();motion.lastRefresh=now;
                MethodDomainEffects.effect(aura,source,target,QiAuraPayloads.DomainEffect.TIDE_WATER,level);
            }
        }
        if(motion.rippleDistance>=.75&&(motion.lastRipple==Long.MIN_VALUE||now-motion.lastRipple>=4)) {
            motion.rippleDistance=0;motion.lastRipple=now;
            MethodDomainEffects.effect(aura,source,target,QiAuraPayloads.DomainEffect.TIDE_RIPPLE,Math.max(1,level));
        }
    }
    public static void clear(LivingEntity target){ACTIVE.remove(target);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var target:new ArrayList<>(ACTIVE.keySet()))if(target!=null)update(target,target.level().getGameTime());
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){if(event.getEntity() instanceof LivingEntity target)ACTIVE.remove(target);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){ACTIVE.clear();}
}
