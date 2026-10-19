package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Target-owned, saved cold: leaving or replacing a caster never resets partial ice. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class ColdTideFreeze {
    public static final String TAG="ImmortalCultivationColdTide";
    public static final int HOLD_TICKS=5*60*20;
    private static final Map<LivingEntity,State> ACTIVE=new WeakHashMap<>();
    private static final class State {
        float progress;double seconds;long updated,contactUntil,lastPulse=Long.MIN_VALUE,until;
        int poseTick;
        Vec3 anchor;
    }
    private ColdTideFreeze() { }
    /** Twenty HP takes about nine seconds; a hundred HP takes twenty. Wounds do not alter this. */
    public static double freezeSeconds(float maximumHp) {
        return Math.max(5,2*Math.sqrt(Float.isFinite(maximumHp)?Math.max(1,maximumHp):1));
    }
    public static boolean immune(LivingEntity target){return target instanceof Slime;}
    public static float progress(LivingEntity target){if(target.level().isClientSide)return 0;var s=ACTIVE.get(target);return s==null?0:s.progress;}
    public static boolean isFrozen(LivingEntity target){return isFrozen(target,target.level().getGameTime());}
    static boolean isFrozen(LivingEntity target,long now){if(target.level().isClientSide)return false;var s=ACTIVE.get(target);return !immune(target)&&target.isAlive()&&s!=null&&s.until>now;}
    public static void expose(LivingEntity owner,LivingEntity target,long now) {
        if(target.level().isClientSide||immune(target)||!target.isAlive())return;
        update(target,now);
        var s=ACTIVE.computeIfAbsent(target,ignored->{var value=new State();value.updated=now;return value;});
        s.seconds=freezeSeconds(target.getMaxHealth());s.contactUntil=now+21;
        if(s.until>now)s.until=now+HOLD_TICKS;
        else if(s.lastPulse==Long.MIN_VALUE||now-s.lastPulse>=20) {
            s.lastPulse=now;s.progress=Math.min(1,s.progress+(float)(1/s.seconds));
            if(s.progress>=.99999F) {
                if(ThoughtControlGuard.resistForDuration(target,"cold_tide_freeze",owner,HOLD_TICKS))s.progress=.99F;
                else {s.progress=1;s.until=now+HOLD_TICKS;s.anchor=target.position();s.poseTick=target.tickCount;target.stopRiding();hold(target,s);QiDomainSystem.stop(target);}
            }
        }
        target.setTicksFrozen(Math.max(target.getTicksFrozen(),Math.max(1,Math.round(s.progress*140))));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,Math.min(3,(int)(s.progress*4)),false,false));
        persist(target,s);send(target,s,now);
    }
    /** Absolute world time also advances cold while the actor's own tick is stopped. */
    public static void update(LivingEntity target,long now) {
        var s=ACTIVE.get(target);if(s==null)return;
        if(!target.isAlive()||immune(target)){clear(target);return;}
        if(s.until>0) {
            if(now>=s.until){clear(target);return;}
        }else {
            long elapsed=Math.max(0,now-Math.max(s.updated,s.contactUntil));
            // Off-domain melting is at least a minute per full coat, and four times slower than buildup.
            s.progress=Math.max(0,s.progress-(float)(elapsed/(20*Math.max(60,s.seconds*4))));
            if(s.progress<=0){clear(target);return;}
        }
        s.updated=now;
    }
    public static void clear(LivingEntity target) {
        if(target.level().isClientSide)return;
        if(ACTIVE.remove(target)==null&&!target.getPersistentData().contains(TAG))return;
        target.getPersistentData().remove(TAG);send(target,null,target.level().getGameTime());
    }
    private static void hold(LivingEntity target,State s) {
        // The level increments age before its cancellable tick hook. Hold that clock too,
        // otherwise animated beast models and age-based abilities continue advancing.
        target.tickCount=s.poseTick;
        if(s.anchor!=null&&target.position().distanceToSqr(s.anchor)>.000001) {
            if(target instanceof ServerPlayer p)p.connection.teleport(s.anchor.x,s.anchor.y,s.anchor.z,p.getYRot(),p.getXRot());
            else target.setPos(s.anchor);
        }
        target.setDeltaMovement(Vec3.ZERO);target.fallDistance=0;target.stopUsingItem();target.setSprinting(false);
        if(target instanceof Mob mob)mob.getNavigation().stop();
        target.hurtMarked=true;
    }
    private static void persist(LivingEntity target,State s) {
        var tag=new CompoundTag();tag.putFloat("Progress",s.progress);tag.putDouble("Seconds",s.seconds);
        tag.putLong("Updated",s.updated);tag.putLong("ContactUntil",s.contactUntil);tag.putLong("LastPulse",s.lastPulse);tag.putLong("Until",s.until);
        if(s.anchor!=null){tag.putDouble("X",s.anchor.x);tag.putDouble("Y",s.anchor.y);tag.putDouble("Z",s.anchor.z);}
        target.getPersistentData().put(TAG,tag);
    }
    /** Called after NBT load as well as in regression checks simulating chunk reload. */
    public static void restore(LivingEntity target) {
        if(target.level().isClientSide||!target.getPersistentData().contains(TAG)||immune(target))return;
        var tag=target.getPersistentData().getCompound(TAG);var s=new State();s.progress=tag.getFloat("Progress");
        if(!Float.isFinite(s.progress)||s.progress<=0||s.progress>1){target.getPersistentData().remove(TAG);return;}
        s.seconds=freezeSeconds(target.getMaxHealth());s.updated=tag.getLong("Updated");s.contactUntil=tag.getLong("ContactUntil");
        s.lastPulse=tag.getLong("LastPulse");s.until=tag.getLong("Until");s.anchor=new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"));
        s.poseTick=target.tickCount;ACTIVE.put(target,s);update(target,target.level().getGameTime());
    }
    private static QiAuraPayloads.ColdTideStatus packet(LivingEntity target,State s,long now) {
        return new QiAuraPayloads.ColdTideStatus(target.level().dimension().location(),target.getId(),target.getUUID(),
                s==null?0:Math.clamp((int)Math.ceil(s.progress*255),1,s.until>now?255:254),s==null?0:(int)Math.clamp(s.until-now,0,HOLD_TICKS));
    }
    private static void send(LivingEntity target,State s,long now){PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,packet(target,s,now));}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var entry:new ArrayList<>(ACTIVE.entrySet())) {
            var target=entry.getKey();if(target==null)continue;
            if(target.isRemoved()){ACTIVE.remove(target);continue;}
            long now=target.level().getGameTime();update(target,now);var s=ACTIVE.get(target);
            // Also undo motion from other actors' knockback, pushes and world-tick callbacks.
            if(s!=null&&s.until>now)hold(target,s);
            if(s!=null&&Math.floorMod(now-target.getId(),20)==0) {
                if(s.until<=now)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,25,Math.min(3,(int)(s.progress*4)),false,false));
                persist(target,s);send(target,s,now);
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void actorTick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof LivingEntity target)||target.level().isClientSide||!isFrozen(target))return;
        // Retain passive lifecycle/effect timers and ordinary damage immunity windows.
        // Never run the actor's tick: beast attacks, navigation and custom abilities live there.
        target.baseTick();
        if(target instanceof ServerPlayer&&target.invulnerableTime>0)target.invulnerableTime--;
        var state=ACTIVE.get(target);if(state!=null&&target.isAlive())hold(target,state);
        event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingIncomingDamageEvent event) {
        if(event.getSource().getEntity() instanceof LivingEntity owner&&isFrozen(owner))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event){if(isFrozen(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Start event){if(isFrozen(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(event.getEntity() instanceof LivingEntity target)restore(target);}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof LivingEntity target){var s=ACTIVE.remove(target);if(s!=null)persist(target,s);}}
    @SubscribeEvent public static void death(LivingDeathEvent event){clear(event.getEntity());}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getTarget() instanceof LivingEntity target) {
            var s=ACTIVE.get(target);if(s!=null)PacketDistributor.sendToPlayer(player,packet(target,s,target.level().getGameTime()));
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){ACTIVE.clear();}
}
