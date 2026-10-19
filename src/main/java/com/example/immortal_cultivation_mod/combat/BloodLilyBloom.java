package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Saved, target-owned flowers. Hits accelerate growth; overlapping domains never stack base pulses. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class BloodLilyBloom {
    public static final String TAG="ImmortalCultivationBloodLily";
    private static final Map<LivingEntity,State> ACTIVE=new WeakHashMap<>();
    private static final class State {
        float progress;double seconds;long updated,contactUntil,lastGrowth=Long.MIN_VALUE,lastDrain=Long.MIN_VALUE;
        double appliedMultiplier=1;
        final LinkedHashMap<UUID,Attacks> attackers=new LinkedHashMap<>();
    }
    private static final class Attacks {int count;long lastHit=Long.MIN_VALUE;}
    private BloodLilyBloom() { }
    public static double bloomSeconds(float maximumHp){return ColdTideFreeze.freezeSeconds(maximumHp);}
    public static float progress(LivingEntity target){var s=ACTIVE.get(target);return s==null?0:s.progress;}
    public static double growthMultiplier(LivingEntity owner,LivingEntity target) {
        var s=ACTIVE.get(target);var attacks=s==null?null:s.attackers.get(owner.getUUID());
        return 1.0+.1*(attacks==null?0:attacks.count);
    }
    static boolean blood(MethodDomainStyle style){return style.kind==MethodDomainStyle.Kind.BLOOD||style.kind==MethodDomainStyle.Kind.DRAIN||style.kind==MethodDomainStyle.Kind.CROWN;}
    private static boolean eligible(QiDomainSystem.Aura aura,LivingEntity target) {
        if(aura==null)return false;
        var owner=aura.owner;
        return !target.level().isClientSide&&aura.domain()&&aura.power>0&&blood(MethodDomainStyle.of(aura.method))
                &&QiDomainSystem.mastery(owner)>=QiDomainSystem.GREAT_MASTERY&&owner.isAlive()
                &&owner.level()==target.level()&&QiDomainSystem.contains(owner,target)
                &&QiDomainSystem.canDomainAffect(owner,target);
    }
    /** Confirmed damaging attacks only. Domain drain cannot feed its own speed bonus. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(LivingDamageEvent.Post event) {
        var source=event.getSource();var target=event.getEntity();
        if(!Float.isFinite(event.getNewDamage())||event.getNewDamage()<=0||QiDomainDamage.isContactDamage(source)
                ||!(source.getEntity() instanceof LivingEntity owner)||source.getDirectEntity()==null)return;
        var aura=QiDomainSystem.view(owner);if(!eligible(aura,target))return;
        long now=target.level().getGameTime();update(target,now);
        var s=ACTIVE.computeIfAbsent(target,ignored->{var value=new State();value.updated=now;return value;});
        s.seconds=bloomSeconds(target.getMaxHealth());s.contactUntil=now+21;
        var attacks=s.attackers.get(owner.getUUID());
        if(attacks==null) {
            if(s.attackers.size()>=32)s.attackers.remove(s.attackers.keySet().iterator().next());
            attacks=new Attacks();s.attackers.put(owner.getUUID(),attacks);
        }
        // One target cannot count the same attack's secondary damage repeatedly in a single tick.
        if(attacks.lastHit==now)return;
        attacks.lastHit=now;if(attacks.count<Integer.MAX_VALUE)attacks.count++;
        persist(target,s);
    }
    public static void expose(QiDomainSystem.Aura aura,LivingEntity target,long now) {
        if(!eligible(aura,target))return;var owner=aura.owner;
        update(target,now);
        var s=ACTIVE.computeIfAbsent(target,ignored->{var value=new State();value.updated=now;return value;});
        s.seconds=bloomSeconds(target.getMaxHealth());s.contactUntil=now+21;
        boolean changed=false;
        double multiplier=growthMultiplier(owner,target),growth=0;
        if(s.lastGrowth==Long.MIN_VALUE||now-s.lastGrowth>=20) {
            s.lastGrowth=now;s.appliedMultiplier=multiplier;growth=multiplier;
        }else if(multiplier>s.appliedMultiplier) {
            // A stronger caster may improve this second's growth, but never adds another base pulse.
            growth=multiplier-s.appliedMultiplier;s.appliedMultiplier=multiplier;
        }
        if(growth>0) {
            s.progress=Math.min(1,s.progress+(float)(growth/s.seconds));
            if(s.progress>=.99999F)s.progress=1;changed=true;
        }
        // A partial flower, an out-of-range former caster, or a second contact in this second cannot leech.
        if(s.progress==1&&(s.lastDrain==Long.MIN_VALUE||now-s.lastDrain>=20)) {
            s.lastDrain=now;
            if(!target.isInvulnerableTo(owner.damageSources().indirectMagic(owner,owner))) {
                float before=target.getHealth();
                QiDomainDamage.hurt(owner,target,QiDomainDamage.amount(owner,aura.power),false);
                float taken=Math.max(0,before-target.getHealth());
                if(taken>0)owner.heal(taken);
                int qi=QiDomainSystem.drain(target,owner,Math.max(1,(int)(QiDomainDamage.amount(owner,aura.power)*2)));
                if(taken>0||qi>0)MethodDomainEffects.effect(aura,owner,target,QiAuraPayloads.DomainEffect.DRAIN,1);
            }
        }
        if(target.isAlive()){persist(target,s);if(changed)send(target,s);}else clear(target);
    }
    public static void update(LivingEntity target,long now) {
        var s=ACTIVE.get(target);if(s==null)return;
        if(!target.isAlive()){clear(target);return;}
        long elapsed=Math.max(0,now-Math.max(s.updated,s.contactUntil));
        s.progress=Math.max(0,s.progress-(float)(elapsed/(20*Math.max(60,s.seconds*4))));s.updated=now;
        if(s.progress<=0&&now>s.contactUntil)clear(target);
    }
    public static void clear(LivingEntity target) {
        if(target.level().isClientSide)return;
        if(ACTIVE.remove(target)==null&&!target.getPersistentData().contains(TAG))return;
        target.getPersistentData().remove(TAG);send(target,null);
    }
    private static void persist(LivingEntity target,State s) {
        var tag=new CompoundTag();tag.putFloat("Progress",s.progress);tag.putDouble("Seconds",s.seconds);
        tag.putLong("Updated",s.updated);tag.putLong("ContactUntil",s.contactUntil);
        tag.putLong("LastGrowth",s.lastGrowth);tag.putLong("LastDrain",s.lastDrain);tag.putDouble("GrowthMultiplier",s.appliedMultiplier);
        var attackers=new ListTag();
        for(var entry:s.attackers.entrySet()) {
            var attack=new CompoundTag();attack.putUUID("Caster",entry.getKey());attack.putInt("Hits",entry.getValue().count);
            attack.putLong("LastHit",entry.getValue().lastHit);attackers.add(attack);
        }
        tag.put("Attackers",attackers);target.getPersistentData().put(TAG,tag);
    }
    public static void restore(LivingEntity target) {
        if(target.level().isClientSide||!target.getPersistentData().contains(TAG))return;
        var tag=target.getPersistentData().getCompound(TAG);float progress=tag.getFloat("Progress");
        if(!Float.isFinite(progress)||progress<0||progress>1){target.getPersistentData().remove(TAG);return;}
        var s=new State();s.progress=progress;s.seconds=bloomSeconds(target.getMaxHealth());
        s.updated=tag.getLong("Updated");s.contactUntil=tag.getLong("ContactUntil");
        s.lastGrowth=tag.getLong("LastGrowth");s.lastDrain=tag.getLong("LastDrain");
        double applied=tag.getDouble("GrowthMultiplier");s.appliedMultiplier=Double.isFinite(applied)?Math.clamp(applied,1,1.0+.1*Integer.MAX_VALUE):1;
        var attackers=tag.getList("Attackers",10);
        for(int i=0;i<Math.min(32,attackers.size());i++) {
            var attack=attackers.getCompound(i);if(!attack.hasUUID("Caster")||attack.getInt("Hits")<=0)continue;
            var value=new Attacks();value.count=attack.getInt("Hits");value.lastHit=attack.getLong("LastHit");s.attackers.put(attack.getUUID("Caster"),value);
        }
        if(progress==0&&s.attackers.isEmpty()){target.getPersistentData().remove(TAG);return;}
        ACTIVE.put(target,s);update(target,target.level().getGameTime());
    }
    private static QiAuraPayloads.BloodLilyStatus packet(LivingEntity target,State s) {
        return new QiAuraPayloads.BloodLilyStatus(target.level().dimension().location(),target.getId(),target.getUUID(),
                s==null||s.progress<=0?0:s.progress>=1?255:Math.clamp(Math.round(s.progress*255),1,254));
    }
    private static void send(LivingEntity target,State s){PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,packet(target,s));}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var entry:new ArrayList<>(ACTIVE.entrySet())) {
            var target=entry.getKey();if(target==null)continue;
            if(target.isRemoved()){ACTIVE.remove(target);continue;}
            long now=target.level().getGameTime();update(target,now);var s=ACTIVE.get(target);
            if(s!=null&&Math.floorMod(now-target.getId(),20)==0){persist(target,s);send(target,s);}
        }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(event.getEntity() instanceof LivingEntity target)restore(target);}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if(!event.getLevel().isClientSide&&event.getEntity() instanceof LivingEntity target){var s=ACTIVE.remove(target);if(s!=null)persist(target,s);}
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){clear(event.getEntity());}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getTarget() instanceof LivingEntity target) {
            var s=ACTIVE.get(target);if(s!=null)PacketDistributor.sendToPlayer(player,packet(target,s));
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){ACTIVE.clear();}
}
