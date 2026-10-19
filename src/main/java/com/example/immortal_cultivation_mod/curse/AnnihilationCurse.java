package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.MethodDomainEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.util.Comparator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Target-owned, saved runes. No caster lifetime or chunk-local timer owns an infection. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class AnnihilationCurse {
    public static final String TAG="ImmortalCultivationAnnihilationCurse";
    public static final String ABILITY="yanmie_weijin_zhiren";
    public static final int RESCUE_COOLDOWN=30*60*20,CLEANSE_TICKS=60*20,BLOOM_TICKS=160;
    public static final float ATTACK_GROWTH=.02F,ATTACK_ACCELERATION=.001F;
    private static final ThreadLocal<Boolean> RESTORING=ThreadLocal.withInitial(()->false);
    private static long contactTick=Long.MIN_VALUE;
    private static int contacts;
    private AnnihilationCurse() { }

    private static CompoundTag read(LivingEntity entity) {
        return entity.getPersistentData().contains(TAG)?entity.getPersistentData().getCompound(TAG):null;
    }
    public static CompoundTag state(LivingEntity entity) {
        var root=entity.getPersistentData();
        if(!root.contains(TAG))root.put(TAG,new CompoundTag());
        return root.getCompound(TAG);
    }
    public static float coverage(LivingEntity entity){var s=read(entity);return s==null?0:finite(s.getFloat("Coverage"),0,1);}
    public static boolean owned(LivingEntity entity){var s=read(entity);return s!=null&&s.getBoolean("Owned");}
    public static boolean healingLocked(LivingEntity entity){var s=read(entity);return s!=null&&s.getBoolean("NoHealing");}
    /** Training cleanse removes infection and healing locks, preserving innate ownership and cooldown. */
    public static void clearDebuffs(LivingEntity entity) {
        if(entity.level().isClientSide)return;
        var saved=read(entity);if(saved==null)return;
        if(saved.getBoolean("Owned")) {
            var clean=new CompoundTag();clean.putBoolean("Owned",true);clean.putInt("Cooldown",cooldown(entity));
            entity.getPersistentData().put(TAG,clean);
        }else entity.getPersistentData().remove(TAG);
        sync(entity);
    }
    public static boolean blockHealthIncrease(LivingEntity entity,float next) {
        return !entity.level().isClientSide&&!RESTORING.get()&&healingLocked(entity)&&next>entity.getHealth();
    }
    public static boolean blocksMeditationExperience(Player player){return coverage(player)>0;}
    public static int cooldown(LivingEntity entity){var s=read(entity);return s==null?0:Math.clamp(s.getInt("Cooldown"),0,RESCUE_COOLDOWN);}
    private static float finite(float value,float min,float max){return Float.isFinite(value)?Math.clamp(value,min,max):min;}
    public static float blackHp(LivingEntity entity){var s=read(entity);return s==null?0:finite(s.getFloat("BlackHp"),0,Math.max(0,entity.getHealth()));}
    public static boolean protectedTarget(LivingEntity entity) {
        return !entity.isAlive()||entity.isRemoved()||entity.isSpectator()||entity instanceof Player p&&p.isCreative();
    }

    public static void acquire(ServerPlayer player) {
        var s=state(player);if(s.getBoolean("Owned"))return;
        s.putBoolean("Owned",true);
        var data=ModAttachments.getData(player);
        if(!data.knownSpells().contains(ABILITY))ModAttachments.setData(player,data.withKnownSpells(
                java.util.stream.Stream.concat(data.knownSpells().stream(),java.util.stream.Stream.of(ABILITY)).toList()));
        ServerEvents.syncPlayerData(player);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(player,"annihilation_acquired");
        player.sendSystemMessage(Component.translatable("curse.annihilation.acquired"));sync(player);
    }
    /** Called before final HP loss, so a single large hit cannot skip the five-percent threshold. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void rescueDamage(LivingDamageEvent.Pre event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getNewDamage()>0
                &&!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                &&player.getHealth()-event.getNewDamage()<=player.getMaxHealth()*.05F&&canRescue(player)) {
            event.setNewDamage(0);rescue(player);
        }
    }
    public static boolean canRescue(ServerPlayer player) {
        return !protectedTarget(player)&&owned(player)&&!healingLocked(player)&&cooldown(player)==0;
    }
    public static boolean rescue(ServerPlayer player) {
        if(!canRescue(player))return false;
        var s=state(player);float maximum=player.getMaxHealth();
        s.putInt("Cooldown",RESCUE_COOLDOWN);s.putBoolean("NoHealing",true);
        s.putFloat("BlackHp",maximum*.95F);s.putInt("Bloom",BLOOM_TICKS);
        s.putFloat("Coverage",Math.max(.01F,coverage(player)));s.putFloat("Ceiling",1);
        s.putInt("Cleanse",0);
        RESTORING.set(true);
        try{player.setHealth(maximum);}finally{RESTORING.remove();}
        s.putFloat("LastHp",player.getHealth());sync(player);
        com.example.immortal_cultivation_mod.shentong.ShentongActivations.announce(player,ABILITY);
        player.sendSystemMessage(Component.translatable("curse.annihilation.rescued"));return true;
    }
    public static void showStatus(ServerPlayer player) {
        if(!owned(player))return;
        int seconds=(cooldown(player)+19)/20;
        player.sendSystemMessage(Component.translatable("curse.annihilation.status",seconds/60,seconds%60,
                Math.round(coverage(player)*100),Component.translatable(healingLocked(player)?"curse.annihilation.locked":"curse.annihilation.dormant")));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void heal(LivingHealEvent event) {
        if(!event.getEntity().level().isClientSide&&healingLocked(event.getEntity())&&!RESTORING.get())event.setCanceled(true);
    }
    @SubscribeEvent public static void attack(LivingDamageEvent.Post event) {
        var damaged=event.getEntity();var saved=read(damaged);
        if(!damaged.level().isClientSide&&saved!=null&&saved.getFloat("BlackHp")>0&&event.getNewDamage()>0) {
            accountBlackHp(damaged,saved);sync(damaged);
        }
        if(event.getNewDamage()<=0||!(event.getSource().getEntity() instanceof ServerPlayer caster)
                ||!owned(caster)||coverage(caster)<=0||state(caster).getInt("Bloom")>0)return;
        var target=event.getEntity();if(target==caster||protectedTarget(target)||caster.isAlliedTo(target))return;
        markByAttack(target);
    }
    public static void markByAttack(LivingEntity target) {
        if(target.level().isClientSide||protectedTarget(target))return;
        if(coverage(target)==0&&target instanceof ServerPlayer player)
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(player,"annihilation_infected");
        var s=state(target);s.putBoolean("NoHealing",true);s.putFloat("Ceiling",1);
        s.putFloat("Growth",Math.min(.10F,Math.max(ATTACK_GROWTH,s.getFloat("Growth"))+ATTACK_ACCELERATION));
        s.putFloat("Coverage",Math.max(.01F,coverage(target)));s.putFloat("LastHp",target.getHealth());sync(target);
    }
    /** Contact raises a ceiling, never adds half again on each touch or grants the innate rescue. */
    public static void transfer(LivingEntity source,LivingEntity target) {
        if(source==target||source.level()!=target.level()||target.level().isClientSide||protectedTarget(target))return;
        float ceiling=coverage(source)*.5F;if(ceiling<.01F||ceiling<=coverage(target))return;
        if(coverage(target)==0&&target instanceof ServerPlayer player)
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(player,"annihilation_infected");
        var s=state(target);s.putFloat("Ceiling",Math.max(s.getFloat("Ceiling"),ceiling));
        s.putFloat("Growth",Math.max(.01F,s.getFloat("Growth")));
        s.putFloat("Coverage",Math.min(ceiling,Math.max(.005F,coverage(target))));
        s.putBoolean("NoHealing",true);s.putFloat("LastHp",target.getHealth());sync(target);
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof LivingEntity entity&&!entity.level().isClientSide)tick(entity);
    }
    public static void tick(LivingEntity entity) {
        var s=read(entity);if(s==null)return;
        if(protectedTarget(entity))return;
        if(entity instanceof ServerPlayer player&&player.getHealth()<=player.getMaxHealth()*.05F&&canRescue(player))rescue(player);
        accountBlackHp(entity,s);
        float progress=coverage(entity);
        if(progress>0) {
            boolean meditating=entity instanceof Player p&&ModAttachments.getData(p).isMeditating();
            if(meditating) {
                int elapsed=s.getInt("Cleanse")+1;
                if(elapsed>=CLEANSE_TICKS){s.putFloat("Coverage",Math.max(0,progress-.01F));elapsed=0;}
                s.putInt("Cleanse",elapsed);
                if(coverage(entity)<=.00001F){s.putFloat("Coverage",0);s.putFloat("Ceiling",0);s.putFloat("Growth",0);s.putInt("Bloom",0);sync(entity);}
            } else if(s.getInt("Bloom")>0&&owned(entity)) {
                int remaining=s.getInt("Bloom")-1;s.putInt("Bloom",remaining);
                s.putFloat("Coverage",Math.max(progress,1-remaining/(float)BLOOM_TICKS));
            } else if(entity.tickCount%20==0) {
                s.putFloat("Coverage",Math.min(finite(s.getFloat("Ceiling"),0,1),progress+finite(s.getFloat("Growth"),0,.1F)));
            }
            if(entity.tickCount%20==0&&coverage(entity)>0) {
                if(!owned(entity))corrode(entity,coverage(entity));
                contacts(entity);
            }
        }
        if(entity.tickCount%20==0&&(progress>0||owned(entity)||healingLocked(entity)))sync(entity);
    }
    private static void accountBlackHp(LivingEntity entity,CompoundTag s) {
        float hp=entity.getHealth(),previous=s.contains("LastHp")?s.getFloat("LastHp"):hp;
        s.putFloat("BlackHp",Math.min(hp,Math.max(0,s.getFloat("BlackHp")-Math.max(0,previous-hp))));s.putFloat("LastHp",hp);
    }
    private static void corrode(LivingEntity entity,float coverage) {
        int amount=Math.max(1,(int)Math.ceil(MethodDomainEffects.maxQi(entity)*.01*coverage));
        if(entity instanceof ServerPlayer player) {
            var data=ModAttachments.getData(player);ModAttachments.setData(player,data.withQi(Math.max(0,data.qi()-amount)));ServerEvents.syncPlayerData(player);
        } else if(entity instanceof PathfinderMob mob&&entity instanceof CultivationNpc npc) {
            int available=NpcProfile.qi(mob,npc.npcArchetype());NpcProfile.spendQi(mob,npc.npcArchetype(),Math.min(available,amount));
        }
        entity.hurt(entity.damageSources().magic(),Math.max(.01F,entity.getMaxHealth()*.01F*coverage));
    }
    /** Server player list includes death screens; freezing the entity does not freeze this clock. */
    public static void onlineTick(ServerPlayer player) {
        var s=read(player);if(s!=null&&s.getInt("Cooldown")>0)s.putInt("Cooldown",s.getInt("Cooldown")-1);
    }
    private static void contacts(LivingEntity source) {
        long now=source.level().getGameTime();if(contactTick!=now){contactTick=now;contacts=0;}
        if(++contacts>128||coverage(source)<.02F)return;
        source.level().getEntitiesOfClass(LivingEntity.class,source.getBoundingBox().inflate(.08),
                        target->target!=source&&!protectedTarget(target)&&coverage(target)<coverage(source)*.5F)
                .stream().sorted(Comparator.comparingDouble(source::distanceToSqr)).limit(8).forEach(target->transfer(source,target));
    }
    public static void sync(LivingEntity entity) {
        if(!entity.level().isClientSide)PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,CursePayloads.status(entity));
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getTarget() instanceof LivingEntity target&&read(target)!=null)
            PacketDistributor.sendToPlayer(player,CursePayloads.status(target));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player&&read(player)!=null)sync(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        var previous=read(event.getOriginal());if(previous==null)return;
        var copy=previous.copy();
        if(event.isWasDeath()) {
            copy.putBoolean("NoHealing",false);copy.putFloat("BlackHp",0);
            if(previous.getBoolean("Owned")) {
                copy.putFloat("Coverage",0);copy.putFloat("Ceiling",0);copy.putFloat("Growth",0);
                copy.putInt("Bloom",0);copy.putInt("Cleanse",0);
            }
        }
        event.getEntity().getPersistentData().put(TAG,copy);
    }
}
