package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.companion.XuanjinCombat;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Bounded personal observations. Never reads an enemy profile, realm, inventory or Qi attachment. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class NpcCombatKnowledge {
    private static final String TAG="CombatKnowledge";
    private static final int TTL=20*60*5,MAX_ENEMIES=12;
    public enum Confidence { UNKNOWN, SUSPECTED, OBSERVED, CONFIRMED }
    public record View(int observations,int realmDelta,float health,float qi,boolean opening,
                       boolean ranged,boolean mobile,boolean healing,boolean frenzy,int dodgeSide,
                       Vec3 lastPosition,long lastSeen) {
        public Confidence confidence(){return observations==0?Confidence.UNKNOWN:observations<3?Confidence.SUSPECTED:
                observations<7?Confidence.OBSERVED:Confidence.CONFIRMED;}
    }
    private NpcCombatKnowledge(){}
    private static CompoundTag memories(PathfinderMob npc) {
        var profile=NpcProfile.cultivationProfile(npc);
        if(!profile.contains(TAG))profile.put(TAG,new CompoundTag());
        return profile.getCompound(TAG);
    }
    private static CompoundTag entry(PathfinderMob npc,LivingEntity enemy,boolean create) {
        var all=memories(npc);long now=npc.level().getGameTime();
        for(String key:java.util.List.copyOf(all.getAllKeys()))
            if(now-all.getCompound(key).getLong("LastSeen")>TTL || all.getCompound(key).getLong("LastSeen")>now)all.remove(key);
        String key=enemy.getUUID().toString();
        if(!all.contains(key)&&create) {
            if(all.size()>=MAX_ENEMIES)all.getAllKeys().stream().min(Comparator.comparingLong(k->all.getCompound(k).getLong("LastSeen"))).ifPresent(all::remove);
            CompoundTag memory=new CompoundTag();memory.putLong("FirstSeen",now);memory.putLong("LastSeen",now);
            memory.putFloat("Health",1);memory.putFloat("QiEstimate",-1);all.put(key,memory);
        }
        return all.getCompound(key);
    }
    public static View view(PathfinderMob npc,LivingEntity enemy) {
        var e=entry(npc,enemy,false);long now=npc.level().getGameTime();
        return new View(e.getInt("Observations"),e.getInt("RealmDelta"),e.contains("Health")?e.getFloat("Health"):1,
                e.contains("QiEstimate")?e.getFloat("QiEstimate"):-1,e.getLong("OpeningUntil")>now,
                e.getBoolean("Ranged"),e.getInt("MobilityUses")>0,e.getInt("HealingUses")>0,
                e.getLong("FrenzySeenUntil")>now,e.getInt("DodgeRight")-e.getInt("DodgeLeft"),
                e.contains("X")?new Vec3(e.getDouble("X"),e.getDouble("Y"),e.getDouble("Z")):null,e.getLong("LastSeen"));
    }
    private static void evidence(CompoundTag e,String kind) {
        e.putInt("Observations",Math.min(100,e.getInt("Observations")+1));
        e.putInt(kind,Math.min(100,e.getInt(kind)+1));
    }
    static boolean newThreat(PathfinderMob npc,LivingEntity enemy) {
        return entry(npc,enemy,false).getLong("NewThreatUntil")>npc.level().getGameTime();
    }
    static void shareObserved(PathfinderMob recipient,LivingEntity enemy) {
        var own=entry(recipient,enemy,true);long now=recipient.level().getGameTime();
        if(own.getLong("ShareAfter")>now)return;
        own.putLong("ShareAfter",now+60);
        for(var ally:recipient.level().getEntitiesOfClass(PathfinderMob.class,recipient.getBoundingBox().inflate(12),
                a->a!=recipient&&a instanceof CultivationNpc&&a.isAlive()&&recipient.hasLineOfSight(a)
                        &&NpcProfile.areCombatAllies(recipient,a))) {
            var source=entry(ally,enemy,false);
            if(!source.contains("SampleTick")||now-source.getLong("SampleTick")>40)continue;
            if(source.getBoolean("Ranged"))own.putBoolean("Ranged",true);
            // Reports increase suspicion, never copy private inventory, exact resources, positions or certainty.
            if(source.getInt("RealmDelta")>own.getInt("ReportedRealmDelta")) {
                own.putInt("ReportedRealmDelta",source.getInt("RealmDelta"));
                own.putInt("RealmDelta",Math.max(own.getInt("RealmDelta"),source.getInt("RealmDelta")/2));
            }
        }
    }
    public static View observe(PathfinderMob npc,LivingEntity enemy,boolean casting,boolean recovering,boolean blocking) {
        if(!npc.hasLineOfSight(enemy))return view(npc,enemy);
        var e=entry(npc,enemy,true);long now=npc.level().getGameTime();
        if(e.contains("SampleTick") && now-e.getLong("SampleTick")<4)return view(npc,enemy);
        long previous=e.getLong("LastSeen");boolean hadPosition=e.contains("X");
        float hp=Mth.clamp((float)Math.ceil(enemy.getHealth()/Math.max(1,enemy.getMaxHealth())*5)/5F,.05F,1);
        if(e.contains("SampleTick")&&hp>e.getFloat("Health")+.1F){evidence(e,"HealingUses");e.putBoolean("HealingBelowThird",e.getFloat("Health")<=.4F);}
        if(casting&&!e.getBoolean("WasCasting")) {
            evidence(e,"Casts");float estimate=e.getFloat("QiEstimate");
            e.putFloat("QiEstimate",estimate<0?.85F:Math.max(.08F,estimate-.08F));
            e.putLong("CastStarted",now);
        }
        if(recovering&&!e.getBoolean("WasRecovering")) {
            evidence(e,"Recoveries");
            if(enemy.hasEffect(ModEffects.QI_GATHERING))e.putFloat("QiEstimate",.15F);
            else evidence(e,"HealingUses");
            e.putLong("OpeningUntil",now+40);
        } else if(enemy.hasEffect(ModEffects.QI_GATHERING))e.putFloat("QiEstimate",Math.min(.8F,e.getFloat("QiEstimate")+.02F));
        boolean shield=blocking||DielangShield.remaining(enemy)>0||NpcCombatResources.shield(enemy)>0;
        if(shield&&!e.getBoolean("WasShielded"))evidence(e,"Defenses");
        if(!shield&&e.getBoolean("WasShielded")){evidence(e,"BrokenDefenses");e.putLong("OpeningUntil",now+45);}
        String weapon=BuiltInRegistries.ITEM.getKey(enemy.getMainHandItem().getItem()).toString();
        if(e.contains("Weapon")&&!e.getString("Weapon").equals(weapon)) {
            evidence(e,"EquipmentChanges");
            if(enemy.getMainHandItem().isEmpty())e.putLong("OpeningUntil",now+60);
            else e.putLong("NewThreatUntil",now+60);
        }
        if(hadPosition&&now-previous<=12) {
            var old=new Vec3(e.getDouble("X"),e.getDouble("Y"),e.getDouble("Z"));
            var displacement=enemy.position().subtract(old);
            if(displacement.lengthSqr()>6.25) {
                long last=e.getLong("MobilityAt");if(last>0)e.putLong("MobilityInterval",now-last);
                evidence(e,"MobilityUses");e.putLong("MobilityAt",now);
                e.putLong("OpeningUntil",now+Mth.clamp(e.getLong("MobilityInterval")/2,20,60));
            }
            if(displacement.horizontalDistanceSqr()>.08) {
                var aim=old.subtract(npc.position()).normalize();double side=aim.x*displacement.z-aim.z*displacement.x;
                if(Math.abs(side)>.15)e.putInt(side>0?"DodgeRight":"DodgeLeft",Math.min(20,e.getInt(side>0?"DodgeRight":"DodgeLeft")+1));
            }
        }
        for(var effect:enemy.getActiveEffects())if(effect.isVisible()) {
            if(effect.getEffect()==MobEffects.MOVEMENT_SPEED||effect.getEffect()==MobEffects.LEVITATION)e.putBoolean("MovementEffect",true);
            if(effect.getEffect()==ModEffects.DINGSHEN||effect.getEffect()==ModEffects.FROZEN||effect.getEffect()==ModEffects.SPIRIT_SEAL)e.putLong("OpeningUntil",now+20);
        }
        // Desperation is revealed by visible blood combustion/overload, never by reading the target brain.
        if(enemy.getPersistentData().getLong("NpcVisibleFrenzyUntil")>now)e.putLong("FrenzySeenUntil",now+60);
        e.putString("Weapon",weapon);e.putFloat("Health",hp);e.putBoolean("WasCasting",casting);
        e.putBoolean("WasRecovering",recovering);e.putBoolean("WasShielded",shield);
        e.putDouble("X",enemy.getX());e.putDouble("Y",enemy.getY());e.putDouble("Z",enemy.getZ());
        e.putLong("LastSeen",now);e.putLong("SampleTick",now);
        ListTag pending=e.getList("Attempts",10);
        for(int i=pending.size()-1;i>=0;i--) {
            var attempt=pending.getCompound(i);
            if(attempt.getLong("Until")<=now) {
                if(now-previous<12){String key="Miss."+attempt.getString("Spell");e.putInt(key,Math.min(6,e.getInt(key)+1));}
                pending.remove(i);
            }
        }
        return view(npc,enemy);
    }
    public static void attempted(PathfinderMob npc,LivingEntity enemy,String spell) {
        if(enemy==null||!npc.hasLineOfSight(enemy))return;
        var e=entry(npc,enemy,true);ListTag attempts=e.getList("Attempts",10);
        if(e.contains("Hit."+spell)&&e.getLong("Hit."+spell)==npc.level().getGameTime())return;
        if(attempts.size()>=8)attempts.remove(0);
        var a=new CompoundTag();a.putString("Spell",spell);a.putLong("Until",npc.level().getGameTime()+80);attempts.add(a);e.put("Attempts",attempts);
    }
    public static double failurePenalty(PathfinderMob npc,LivingEntity enemy,String spell,String element) {
        var e=entry(npc,enemy,false);
        return e.getInt("Miss."+spell)*18+e.getInt("Resisted."+normalizeElement(element))*12;
    }
    private static String normalizeElement(String element) {
        return switch(element) {
            case "金"->"metal";case "木"->"wood";case "水"->"water";case "火"->"fire";case "土"->"earth";
            case "风"->"wind";case "雷"->"lightning";case "冰"->"ice";case "光"->"light";case "暗"->"dark";
            case "毒"->"poison";case "魂"->"soul";default->element;
        };
    }
    @SubscribeEvent public static void launched(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        if(event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                &&projectile.getOwner() instanceof PathfinderMob npc&&npc instanceof CultivationNpc) {
            String spell=npc.getPersistentData().getString("NpcReleasingSpell");
            if(!spell.isBlank())projectile.getPersistentData().putString("NpcAttemptSpell",spell);
        }
    }
    public static boolean wasRevealed(PathfinderMob npc,String spell) {
        var arts=NpcProfile.cultivationProfile(npc).getCompound("RevealedCombatArts");
        return arts.contains(spell)&&arts.getLong(spell)>npc.level().getGameTime()-1200;
    }
    public static void reveal(PathfinderMob npc,String spell) {
        var profile=NpcProfile.cultivationProfile(npc);if(!profile.contains("RevealedCombatArts"))profile.put("RevealedCombatArts",new CompoundTag());
        profile.getCompound("RevealedCombatArts").putLong(spell,npc.level().getGameTime());
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if(!(event.getEntity().level() instanceof ServerLevel level)||!(event.getSource().getEntity() instanceof LivingEntity attacker))return;
        LivingEntity victim=event.getEntity();String element=XuanjinCombat.element(event.getSource());
        for(var observer:level.getEntitiesOfClass(PathfinderMob.class,victim.getBoundingBox().inflate(32),
                n->n instanceof CultivationNpc&&!NpcProfile.FANREN.equals(((CultivationNpc)n).npcArchetype()))) {
            if(!observer.hasLineOfSight(victim)||!observer.hasLineOfSight(attacker))continue;
            var e=entry(observer,observer==attacker?victim:attacker,true);
            if(observer!=attacker){evidence(e,"Attacks");e.putInt("Element."+element,Math.min(100,e.getInt("Element."+element)+1));}
            if(observer!=attacker&&event.getSource().getDirectEntity()!=attacker)e.putBoolean("Ranged",true);
            if(observer==victim) {
                float fraction=event.getNewDamage()/Math.max(1,observer.getMaxHealth());
                int estimate=fraction>.35F?5:fraction>.15F?3:fraction>.07F?1:0;
                e.putInt("RealmDelta",Math.max(e.getInt("RealmDelta"),estimate));
                e.putFloat("RecentDamage",recentDamage(observer,attacker)+fraction);e.putLong("DamageAt",level.getGameTime());
            }
            if(observer==attacker) {
                var target=entry(observer,victim,true);evidence(target,"Exchanges");
                ListTag attempts=target.getList("Attempts",10);
                String spell=event.getSource().getDirectEntity()==attacker?attacker.getPersistentData().getString("NpcReleasingSpell"):
                        event.getSource().getDirectEntity()==null?"":event.getSource().getDirectEntity().getPersistentData().getString("NpcAttemptSpell");
                if(!spell.isBlank()) {
                    target.putLong("Hit."+spell,level.getGameTime());
                    for(int i=0;i<attempts.size();i++)if(attempts.getCompound(i).getString("Spell").equals(spell)){attempts.remove(i);break;}
                    target.putInt("Miss."+spell,Math.max(0,target.getInt("Miss."+spell)-1));
                }
                if(event.getBlockedDamage()>event.getNewDamage() || event.getNewDamage()<=0)
                    target.putInt("Resisted."+element,Math.min(5,target.getInt("Resisted."+element)+1));
                else target.putInt("Resisted."+element,Math.max(0,target.getInt("Resisted."+element)-1));
            }
        }
    }
    public static float recentDamage(PathfinderMob npc,LivingEntity enemy) {
        var e=entry(npc,enemy,false);long age=npc.level().getGameTime()-e.getLong("DamageAt");
        return age<60?Math.min(1,e.getFloat("RecentDamage"))*(1-age/60F):0;
    }

    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void witnessedDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CrimsonSkyFlameFloodDragonEntity dragon)
                ||!(dragon.level() instanceof ServerLevel level))return;
        for (var npc:level.getEntitiesOfClass(PathfinderMob.class,dragon.getBoundingBox().inflate(40),
                e->e instanceof CultivationNpc&&e.getTarget()==dragon&&e.hasLineOfSight(dragon))) {
            var p=NpcProfile.cultivationProfile(npc);p.putUUID("WitnessedDragonDeath",dragon.getUUID());
            p.putLong("ConfirmKillUntil",level.getGameTime()+220);
        }
    }
    static boolean witnessedCore(PathfinderMob npc,EscapedYuanyingCoreEntity core) {
        var p=NpcProfile.cultivationProfile(npc);
        return p.hasUUID("WitnessedDragonDeath")&&p.getLong("ConfirmKillUntil")>npc.level().getGameTime()
                &&p.getUUID("WitnessedDragonDeath").equals(core.originalDragon())&&npc.hasLineOfSight(core);
    }
}
