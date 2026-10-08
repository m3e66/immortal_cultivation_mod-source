package com.example.immortal_cultivation_mod.companion;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.Comparator;
import java.util.List;
import static com.example.immortal_cultivation_mod.network.XuanjinPayloads.EffectKind.*;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class XuanjinCombat {
    private static final ResourceLocation AURA = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"xuanjin_dread");
    private static final ResourceLocation FULL = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"xuanjin_full");
    private static final ThreadLocal<Float> PIERCE = ThreadLocal.withInitial(() -> 0F);
    private static final ThreadLocal<LivingEntity> STRIKER = new ThreadLocal<>();
    private XuanjinCombat() {}
    public static float penetration(DamageSource source) {
        if (source == null) return 0;
        if (source.getEntity() != null && source.getEntity() == STRIKER.get()) return PIERCE.get();
        if (source.getEntity() instanceof ServerPlayer p && XuanjinCompanion.possessing(p)
                && source.is(DamageTypes.PLAYER_ATTACK)) return Math.max(.2F, PIERCE.get());
        return 0;
    }
    public static int realm(LivingEntity e) {
        if (e instanceof Player p) return CultivationLevels.getRealmIndex(ModAttachments.getData(p).cultivationLevel());
        if (e instanceof CultivationNpc npc) return CultivationLevels.getRealmIndex(npc.npcRealm());
        if (e instanceof XuanjinZhengEntity beast) return beast.seal() >= 6 ? 5 : Math.max(1, (XuanjinRules.stage(beast.seal()).realmStage()+2)/3);
        // Realm-scaled non-NPC beasts still receive the appropriate suppression tier.
        float hp = e.getMaxHealth();
        return hp >= 300000 ? 6 : hp >= 50000 ? 5 : hp >= 8000 ? 4 : hp >= 1500 ? 3 : hp >= 250 ? 2 : 1;
    }
    public static boolean spirit(LivingEntity e) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return e.getType().is(EntityTypeTags.UNDEAD) || id.contains("ghost") || id.contains("soul") || id.contains("shihun");
    }
    public static boolean enemy(LivingEntity actor, LivingEntity target) {
        if (target == actor || !target.isAlive() || target.isSpectator() || target instanceof Player p && p.isCreative()
                || target instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        if (actor instanceof XuanjinZhengEntity beast && beast.isFree()) return true;
        LivingEntity owner = actor instanceof XuanjinZhengEntity beast ? beast.owner() : actor;
        if (owner == null || target == owner || owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        if (target instanceof XuanjinZhengEntity beast && owner.getUUID().equals(beast.ownerId())) return false;
        return target == owner.getLastHurtByMob() || target == owner.getLastHurtMob()
                || target instanceof Enemy || target instanceof Mob mob && mob.getTarget() == owner;
    }
    public static CompoundTag state(LivingEntity e) {
        return e instanceof XuanjinZhengEntity beast ? beast.companionState() : e instanceof Player p ? XuanjinCompanion.state(p) : new CompoundTag();
    }
    public static List<LivingEntity> enemies(LivingEntity actor, double radius) {
        return actor.level().getEntitiesOfClass(LivingEntity.class,actor.getBoundingBox().inflate(radius),e -> enemy(actor,e) && actor.distanceToSqr(e) <= radius*radius)
                .stream().sorted(Comparator.comparingDouble(actor::distanceToSqr)).toList();
    }
    private static float power(CompoundTag s) { return s.getBoolean("Full") ? 1.2F : 1F; }
    public static boolean hit(LivingEntity actor, LivingEntity target, float damage, float pierce) {
        float old = PIERCE.get(); LivingEntity oldStriker = STRIKER.get(); PIERCE.set(pierce); STRIKER.set(actor);
        try {
            float formationDamage = pierce >= .4F ? XuanjinRules.stage(state(actor).getInt("Seal")).attack() * 3 * power(state(actor)) : damage;
            if (com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior.interceptAttackPath(
                    actor,actor.getBoundingBox().getCenter(),target.getBoundingBox().getCenter(),formationDamage)) return false;
            target.invulnerableTime = 0;
            boolean hit = target.hurt(actor instanceof Player p ? actor.damageSources().playerAttack(p) : actor.damageSources().mobAttack(actor),damage);
            if (hit && actor instanceof XuanjinZhengEntity beast && actor.level() instanceof ServerLevel level
                    && com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.destructiveRealm(beast) >= 3
                    && level.getGameTime() >= beast.getPersistentData().getLong("BeastTerrainImpactReady")) {
                beast.getPersistentData().putLong("BeastTerrainImpactReady", level.getGameTime() + 10);
                com.example.immortal_cultivation_mod.entity.BeastMoveBlockBreaker.breakSphere(
                        level, beast, target.position(), pierce >= .4F ? 3 : 2, 1.8, 48, 12);
            }
            return hit;
        } finally { PIERCE.set(old); if (oldStriker == null) STRIKER.remove(); else STRIKER.set(oldStriker); }
    }
    public static void basic(XuanjinZhengEntity beast, LivingEntity target) {
        var s = beast.companionState(); int combo = s.getInt("Combo") + 1; s.putInt("Combo", combo);
        float damage = XuanjinRules.stage(beast.seal()).attack() * (combo % 3 == 0 ? 1.5F : 1) * power(s);
        if (beast.seal() >= 3 && s.getInt("Charged") > 0) { int charge = Math.min(s.getInt("Energy"),s.getInt("Charged")); damage += charge; XuanjinCompanion.spend(s,charge); s.putInt("Charged",0); }
        beast.action(combo % 3 == 0 ? "bite" : combo%3==1?"claw":"claw_right", 20);
        XuanjinVisuals.emit(beast,combo%3==0?BITE:CLAW,target.getBoundingBox().getCenter(),14);
        if (hit(beast,target,damage,.2F) && combo % 3 == 0) shove(beast,target,3);
    }
    private static void shove(LivingEntity actor, LivingEntity target, double strength) {
        Vec3 away = target.position().subtract(actor.position()).multiply(1,0,1).normalize();
        target.setDeltaMovement(away.scale(strength * .45).add(0,.45,0)); target.hurtMarked = true;
    }
    private static void animate(LivingEntity actor, String clip, int ticks) { if (actor instanceof XuanjinZhengEntity beast) beast.action(clip,ticks); }
    public static boolean command(LivingEntity actor, CompoundTag s, String command, ServerPlayer owner) {
        long now = actor.level().getGameTime();
        if (!XuanjinCompanion.ready(s,command,now)) return false;
        int seal = s.getInt("Seal");
        switch (command) {
            case "claw" -> {
                var targets = enemies(actor,XuanjinRules.CLAW_RANGE).stream().filter(actor::hasLineOfSight).toList(); if (targets.isEmpty()) return false;
                int cost = seal == 6 ? 800 : Math.max(20,XuanjinRules.stage(seal).energy()/5);
                if (!XuanjinCompanion.spend(s,cost)) return false;
                LivingEntity target = targets.getFirst();
                boolean defended = target.isBlocking() || target instanceof ServerPlayer p && PlayerShieldManager.totalAmount(p) > 0
                        || com.example.immortal_cultivation_mod.spell.DielangShield.hasShield(target)
                        || target instanceof OwnableEntity || BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath().matches(".*(formation|guardian|summon|clone|shadow).*" );
                float damage = XuanjinRules.stage(seal).attack() * (defended ? 3 : 2) * power(s);
                if (target instanceof Player p && p.isBlocking()) p.disableShield();
                if (hit(actor,target,damage,.4F)) {
                    shove(actor,target,3);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,3));
                }
                animate(actor,"claw",20); XuanjinVisuals.emit(actor,CLAW,target.getBoundingBox().getCenter(),16); XuanjinCompanion.cooldown(s,command,now); return true;
            }
            case "voice" -> {
                Vec3 origin=actor.getEyePosition(),forward=actor.getLookAngle().normalize();
                var bounds=new net.minecraft.world.phys.AABB(origin,origin.add(forward.scale(XuanjinRules.ROAR_RANGE)))
                        .inflate(XuanjinRules.roarRadius(XuanjinRules.ROAR_RANGE));
                for (var target : actor.level().getEntitiesOfClass(LivingEntity.class,bounds,e->enemy(actor,e)
                        && XuanjinRules.inRoarCone(origin,forward,e.getBoundingBox().getCenter(),e.getBbWidth()*.5))) {
                    if (realm(target) >= 6) continue;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,1));
                    if (target instanceof Mob mob && realm(target) < 5) { mob.setTarget(null); Vec3 away=target.position().subtract(actor.position()).normalize().scale(8); mob.getNavigation().moveTo(target.getX()+away.x,target.getY(),target.getZ()+away.z,1.3); }
                }
                animate(actor,"roar",30); XuanjinVisuals.emit(actor,ROAR,XuanjinRules.ROAR_EFFECT_TICKS); XuanjinCompanion.cooldown(s,command,now); return true;
            }
            case "tails" -> {
                var targets = enemies(actor,8); if (targets.isEmpty()) return false;
                // Aggregate repeated strikes so vanilla invulnerability frames do not drop four hits.
                int count = Math.min(5,targets.size()); int[] strikes = new int[count];
                for (int i=0;i<5;i++) strikes[i%count]++;
                for (int i=0;i<count;i++) hit(actor,targets.get(i),2000*strikes[i]*power(s),0);
                animate(actor,"tail_strike",26); XuanjinVisuals.emit(actor,TAILS,targets.getFirst().getBoundingBox().getCenter(),24); XuanjinCompanion.cooldown(s,command,now); return true;
            }
            case "devour" -> {
                if (actor.getHealth() >= actor.getMaxHealth()*.9F) return false;
                // Loose XP and spiritual projectiles are finite sources; allies are never drained.
                var orbs = actor.level().getEntitiesOfClass(ExperienceOrb.class,actor.getBoundingBox().inflate(10),e -> actor.distanceToSqr(e)<=100);
                if (orbs.isEmpty() && !s.getBoolean("LooseEnergy")) return false;
                if (!orbs.isEmpty()) orbs.getFirst().discard(); s.putBoolean("LooseEnergy",false);
                actor.heal(Math.min(5000,XuanjinRules.stage(seal).health()*.25F)); XuanjinCompanion.energy(s,1000);
                animate(actor,"devour",32); XuanjinVisuals.emit(actor,DEVOUR,30); XuanjinCompanion.cooldown(s,command,now); return true;
            }
            case "vortex" -> {
                if (s.getLong("VortexUntil") > now || s.getFloat("Absorbed") > 0 || !XuanjinCompanion.spend(s,2500)) return false;
                s.putFloat("Absorbed",0); s.putLong("VortexUntil",now+100); s.putBoolean("VortexReady",false);
                XuanjinCompanion.cooldown(s,command,now); animate(actor,"devour",100); XuanjinVisuals.emit(actor,VORTEX,100); return true;
            }
            case "swallow", "return", "refine" -> {
                if (s.getLong("VortexUntil") > now || !s.getBoolean("VortexReady")) return false;
                float absorbed = s.getFloat("Absorbed");
                if (command.equals("swallow")) {actor.heal(Math.min(16000,absorbed*.4F));XuanjinVisuals.emit(actor,DEVOUR,24);animate(actor,"devour",24);}
                else if (command.equals("return")) {
                    var targets = enemies(actor,32); if (targets.isEmpty()) return false;
                    Vec3 center = targets.getFirst().position();
                    for (var target : actor.level().getEntitiesOfClass(LivingEntity.class,targets.getFirst().getBoundingBox().inflate(8),e -> enemy(actor,e) && e.position().distanceToSqr(center)<=64)) hit(actor,target,Math.min(30000,absorbed*.75F),0);
                    XuanjinVisuals.emit(actor,RETURN,targets.getFirst().getBoundingBox().getCenter(),24); animate(actor,"roar",30);
                } else {
                    if (owner == null) return false;
                    var data = ModAttachments.getData(owner); int cap = CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi()+data.maxQiBonus();
                    ModAttachments.setData(owner,data.withQi(Math.min(cap,data.qi()+Math.min(8000,(int)(absorbed*.2F))))); ServerEvents.syncPlayerData(owner);
                    XuanjinVisuals.emit(actor,REFINE,24);animate(actor,"devour",24);
                }
                s.putFloat("Absorbed",0); s.putBoolean("VortexReady",false); return true;
            }
            case "dash" -> {
                if (!(actor instanceof ServerPlayer p) || !XuanjinCompanion.possessing(p) || !XuanjinCompanion.spend(s,200)) return false;
                Vec3 direction=p.getLookAngle().normalize(); p.setDeltaMovement(direction.scale(1.8)); p.hurtMarked=true;
                com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashStarted(p,direction,.7F,18);
                XuanjinCompanion.cooldown(s,command,now); XuanjinVisuals.emit(p,DASH,p.getEyePosition().add(direction.scale(6)),12); return true;
            }
            default -> { return false; }
        }
    }
    public static void finishVortex(CompoundTag s) { s.putLong("VortexUntil",0); s.putBoolean("VortexReady",true); }
    public static void clearFullBonuses(LivingEntity actor) {
        for (var a : List.of(Attributes.ARMOR,Attributes.MOVEMENT_SPEED)) {
            var value = actor.getAttribute(a); if (value != null) value.removeModifier(FULL);
        }
    }
    public static float absorb(CompoundTag s, float damage, float fraction) {
        float amount = Math.min(Math.max(0,40000-s.getFloat("Absorbed")),Math.max(0,damage*fraction));
        s.putFloat("Absorbed",s.getFloat("Absorbed")+amount); return amount;
    }
    public static void tick(LivingEntity actor, CompoundTag s) {
        long now=actor.level().getGameTime(); int seal=s.getInt("Seal");
        boolean full=seal==6 && XuanjinRules.fullEnergy(s.getBoolean("Full"),s.getInt("Energy")); s.putBoolean("Full",full);
        for (var attribute : List.of(Attributes.ARMOR,Attributes.MOVEMENT_SPEED)) {
            var value=actor.getAttribute(attribute); if (value==null) continue;
            if (full && !value.hasModifier(FULL)) value.addTransientModifier(new AttributeModifier(FULL,attribute==Attributes.ARMOR?100:.15,
                    attribute==Attributes.ARMOR?AttributeModifier.Operation.ADD_VALUE:AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (!full) value.removeModifier(FULL);
        }
        if (seal>=4 && now%10==0) for (var target:enemies(actor,16)) {
            if (realm(target)>=6 || (seal<6 && realm(target)>=realm(actor))) continue;
            var tag=target.getPersistentData(); float amount=XuanjinRules.auraDamage(realm(target),spirit(target));
            tag.putFloat("XuanjinDread",Math.max(tag.getLong("XuanjinDreadUntil")>now?tag.getFloat("XuanjinDread"):0,amount));
            tag.putLong("XuanjinDreadUntil",now+15);
            var speed=target.getAttribute(Attributes.MOVEMENT_SPEED);
            if(speed!=null) { speed.removeModifier(AURA); speed.addTransientModifier(new AttributeModifier(AURA,-XuanjinRules.auraSpeed(realm(target)),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
        }
        if (s.getLong("VortexUntil")>0) {
            if (s.getLong("VortexUntil")<=now) { finishVortex(s); if(actor instanceof XuanjinZhengEntity b && b.owner()!=null) XuanjinCompanion.tell(b.owner(),"vortex_ready"); }
            else {
                // Intercept only a measured fraction of each projectile's actual damage on impact.
                // Projectile marks also follow it when it exits the vortex radius.
                for(var projectile:actor.level().getEntitiesOfClass(Projectile.class,actor.getBoundingBox().inflate(10),e -> actor.distanceToSqr(e)<=100)) {
                    if (!(projectile.getOwner() instanceof LivingEntity caster) || !enemy(actor,caster)) continue;
                    if(projectile instanceof AbstractArrow) continue; // Physical arrows remain physical attacks.
                    projectile.getPersistentData().putUUID("XuanjinVortex",actor.getUUID());
                    projectile.getPersistentData().putLong("XuanjinVortexUntil",s.getLong("VortexUntil"));
                }
            }
        }
        if (s.getBoolean("VortexReady") && s.getFloat("Absorbed") > 0 && now % 4 == 0 && actor.level() instanceof ServerLevel server) {
            Vec3 center=actor.getEyePosition().add(actor.getLookAngle().scale(1.5));
            double radius=.25+.65*Math.min(1,s.getFloat("Absorbed")/40000);
            for (int i=0;i<12;i++) {
                double angle=i*Math.PI/6+now*.08;
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,center.x+Math.cos(angle)*radius,center.y+Math.sin(angle)*radius,center.z,1,0,0,0,0);
            }
        }
    }
    public static String element(DamageSource source) {
        if(source.is(DamageTypes.FALL)) return "fall";
        if(source.is(DamageTypes.LIGHTNING_BOLT)) return "lightning";
        if(source.is(DamageTypeTags.IS_FIRE)) return "fire";
        String id=source.getMsgId()+" "+(source.getDirectEntity()==null?"":BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType()).getPath());
        id += " " + com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport.currentSpellElement(source.getEntity());
        if (source.getDirectEntity() instanceof Projectile projectile)
            id += " " + projectile.getPersistentData().getString("XuanjinSpellElement");
        if(id.matches(".*(lightning|thunder|lei|雷).*")) return "lightning";
        if(id.matches(".*(poison|毒).*")) return "poison";
        if(id.matches(".*(soul|ghost|魂).*")) return "soul";
        if(id.matches(".*(frost|ice|bing|冰).*")) return "ice";
        if(id.matches(".*(water|shui|水).*")) return "water";
        if(id.matches(".*(dark|shadow|暗).*")) return "dark";
        if(id.matches(".*(light_beam|光).*")) return "light";
        if(id.matches(".*(wood|木).*")) return "wood";
        if(id.matches(".*(wind|feng|风).*")) return "wind";
        if(id.matches(".*(earth|rock|土).*")) return "earth";
        if(id.matches(".*(metal|金).*")) return "metal";
        if(id.matches(".*(fire|flame|火).*")) return "fire";
        return "physical";
    }
    private static String absorbKind(DamageSource source) {
        if(element(source).equals("lightning")) return "lightning";
        if(source.getDirectEntity() instanceof Projectile && !(source.getDirectEntity() instanceof AbstractArrow)) return "projectile";
        if(source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return "aoe";
        return "physical";
    }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void incoming(LivingIncomingDamageEvent event) {
        if(!(event.getEntity().level() instanceof ServerLevel level)) return;
        var source=event.getSource(); var target=event.getEntity(); long now=level.getGameTime();
        float amount=event.getAmount();
        if(source.getEntity() instanceof LivingEntity attacker) {
            var dread=attacker.getPersistentData(); if(dread.getLong("XuanjinDreadUntil")>now) amount*=1-dread.getFloat("XuanjinDread");
            if(attacker instanceof ServerPlayer p && XuanjinCompanion.possessing(p) && source.is(DamageTypes.PLAYER_ATTACK) && PIERCE.get()==0) {
                var s=state(p);
                if(s.getLong("PossessionAttackAt")>now) { event.setCanceled(true); return; }
                s.putLong("PossessionAttackAt",now+36); int combo=s.getInt("Combo")+1; s.putInt("Combo",combo);
                amount=Math.max(amount,(combo%3==0?6750:4500)*power(s)); if(combo%3==0) shove(p,target,3);
            }
        }
        LivingEntity vortex=null;
        if(source.getDirectEntity() instanceof Projectile projectile && projectile.getPersistentData().hasUUID("XuanjinVortex")
                && projectile.getPersistentData().getLong("XuanjinVortexUntil")>now) {
            if(level.getEntity(projectile.getPersistentData().getUUID("XuanjinVortex")) instanceof LivingEntity e) vortex=e;
        }
        if(vortex==null && !absorbKind(source).equals("physical")) {
            for(var beast:level.getEntitiesOfClass(XuanjinZhengEntity.class,target.getBoundingBox().inflate(10),e -> e.distanceToSqr(target)<=100 && e.companionState().getLong("VortexUntil")>now)) {
                if(source.getEntity()==null || source.getEntity() instanceof LivingEntity caster && enemy(beast,caster)) { vortex=beast; break; }
            }
            if(vortex==null) for(var player:level.players()) if(XuanjinCompanion.possessing(player) && state(player).getLong("VortexUntil")>now && player.distanceToSqr(target)<=100
                    && (source.getEntity()==null || source.getEntity() instanceof LivingEntity caster && enemy(player,caster))) { vortex=player; break; }
        }
        if(vortex!=null && vortex.isAlive() && !(vortex instanceof Player p && !XuanjinCompanion.possessing(p)) && state(vortex).getLong("VortexUntil")>now) {
            int enemyRealm=source.getEntity() instanceof LivingEntity attacker?realm(attacker):0;
            amount-=absorb(state(vortex),amount,XuanjinRules.absorbFraction(absorbKind(source),enemyRealm));
            state(vortex).putBoolean("LooseEnergy",true);
        }
        boolean beast=target instanceof XuanjinZhengEntity || target instanceof ServerPlayer p && XuanjinCompanion.possessing(p);
        if(beast) {
            var s=state(target); String element=element(source);
            float factor=element.equals("lightning") && s.getInt("Seal")<3?1:XuanjinRules.resistance(element);
            float prevented=amount*(1-factor); amount*=factor;
            if(element.equals("lightning") && s.getInt("Seal")>=3) { int gained=XuanjinRules.lightningEnergy(prevented); XuanjinCompanion.energy(s,gained); s.putInt("Charged",Math.min(2000,s.getInt("Charged")+gained)); }
            if(s.getBoolean("Full") && (!element.equals("physical") || !absorbKind(source).equals("physical"))) amount*=.8F;
        }
        event.setAmount(Math.max(0,amount));
    }
    @SubscribeEvent
    public static void launched(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Projectile projectile)
                || projectile instanceof AbstractArrow || projectile.getPersistentData().contains("XuanjinSpellElement")) return;
        if (projectile.getOwner() instanceof Player owner) {
            String element = com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport.currentSpellElement(owner);
            if (element.isEmpty() && owner.level().getGameTime()-owner.getPersistentData().getLong("MagicCenterLastSpellTime") <= 1)
                element = owner.getPersistentData().getString("MagicCenterLastSpellElement");
            projectile.getPersistentData().putString("XuanjinSpellElement",element);
        }
    }
    @SubscribeEvent
    public static void effects(MobEffectEvent.Applicable event) {
        if((event.getEntity() instanceof XuanjinZhengEntity || event.getEntity() instanceof Player p && XuanjinCompanion.possessing(p))
                && event.getEffectInstance().getEffect().is(MobEffects.POISON)) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    @SubscribeEvent
    public static void cleanup(EntityTickEvent.Post event) {
        if(!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide) return;
        var tag=e.getPersistentData(); long now=e.level().getGameTime();
        if(tag.contains("XuanjinDreadUntil") && tag.getLong("XuanjinDreadUntil")<=now) {
            tag.remove("XuanjinDreadUntil"); tag.remove("XuanjinDread"); var speed=e.getAttribute(Attributes.MOVEMENT_SPEED); if(speed!=null) speed.removeModifier(AURA);
        }
        if(e instanceof Player p && !XuanjinCompanion.possessing(p)) for(var a:List.of(Attributes.ARMOR,Attributes.MOVEMENT_SPEED)) { var v=e.getAttribute(a); if(v!=null) v.removeModifier(FULL); }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void kill(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide) return;
        if(event.getSource().getEntity() instanceof XuanjinZhengEntity beast) {
            var s=beast.companionState(); XuanjinCompanion.energy(s,Math.max(5,Math.min(500,(int)event.getEntity().getMaxHealth()/10)));
            s.putBoolean("LooseEnergy",true);
            if(!beast.isFree() && beast.owner()!=null && event.getEntity() instanceof Enemy) s.putInt("Trust",Math.min(100,s.getInt("Trust")+1));
        }
    }
}
