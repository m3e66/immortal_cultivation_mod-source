package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.item.HeavyIronBracerItem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.TieguShanItem;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.example.immortal_cultivation_mod.spell.HealingTechniques;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.YufengJue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One-action-at-a-time tactical combat controller shared by cultivator NPC tiers. */
public final class NpcCombatBrain {
    private static final int MIN_DECISION_DELAY = 2;
    private static final int RANDOM_DECISION_DELAY = 3;
    private static final double TARGET_SEARCH_RANGE = 40.0D;
    private static final double HOSTILE_MOB_AGGRO_RADIUS = 10.0D;
    private static final double HOSTILE_MOB_AGGRO_RADIUS_SQUARED =
            HOSTILE_MOB_AGGRO_RADIUS * HOSTILE_MOB_AGGRO_RADIUS;
    private static final double LOCAL_COMBAT_RADIUS = 12.0D;
    private static final float SURVIVAL_HP_RATIO = 0.20F;
    private static final float LOW_QI_RATIO = 0.15F;
    private static final float COVER_HEAL_RATIO = 0.60F;
    private static final float COVER_QI_RATIO = 0.45F;
    private static final int ACTION_MEMORY = 6;
    private static final String COMBAT_STATE_TAG = "ImmortalCultivationNpcCombatState";

    private final PathfinderMob mob;
    private final NpcBattlePlan battle = new NpcBattlePlan();
    private final NpcCombatRhythm rhythm = new NpcCombatRhythm();
    private NpcCombatKnowledge.View knowledge;
    private Vec3 escapeProgressOrigin;
    private long escapeProgressTick;
    private double escapeThreatDistance;
    private long stuckUntil;
    private Vec3 feintEnemyOrigin;
    private Vec3 feintOrigin;
    private long feintUntil;
    private long counterUntil;
    private final Map<UUID, RecentDamage> recentDamageByAttacker = new HashMap<>();
    private final Deque<ActionKind> recentActions = new ArrayDeque<>();
    private final Deque<String> recentSpells = new ArrayDeque<>();
    private PendingCast pendingCast;
    private PendingPill pendingPill;
    private long nextThreatCheck;
    private Vec3 flightDestination;
    private long flightIntentUntil;
    private boolean flightRequiresGround;
    private Vec3 idleLanding;
    private long nextLandingCheck;
    private Vec3 recoveryAnchor;
    private long nextDecisionTick;
    private long actionLockUntil;
    private int comboStep;
    private int strafeSign = 1;
    private long strafeChangeTick;
    private String lastSpell = "";
    private UUID trackedRetreatTarget;
    private UUID coverTarget;
    private long coverHoldUntil;
    private Vec3 lastTargetPosition;
    private ActionKind lastRememberedAction;
    private long lastRememberTick = -1L;

    public NpcCombatBrain(PathfinderMob mob) {
        this.mob = mob;
    }

    public void tick() {
        if (mob.level().isClientSide || !mob.isAlive()) {
            return;
        }
        long now = mob.level().getGameTime();
        NpcCombatResources.tick(mob);
        HeavyIronBracerItem.syncNpcOffhandCompanion(mob);
        if (mob.isNoAi() || NpcProfile.isInteracting(mob)) {
            cancelPendingPill(now);
            cancelPendingCast(now);
            clearFlightIntent();
            mob.removeEffect(ModEffects.NPC_WEAPON_GUARD);
            rhythm.reset();
            return;
        }
        if (NpcCombatReactions.disabled(mob)) {
            cancelPendingPill(now);
            cancelPendingCast(now);
            clearFlightIntent();
            mob.getNavigation().stop();
            mob.removeEffect(ModEffects.NPC_WEAPON_GUARD);
            rhythm.reset();
            return;
        }
        tickRecoveryAnchor();
        if (pendingPill != null) {
            tickPendingPill(now);
            return;
        }
        if (pendingCast != null) {
            tickPendingCast(now);
            return;
        }
        if (mob.hasEffect(ModEffects.QI_GATHERING)) {
            if (qiFullyRecovered() || shouldCancelRecoveryImmediately()) {
                cancelQiRecovery(now);
            }
        }
        boolean survivalEmergency = healthRatio(mob) < SURVIVAL_HP_RATIO;
        boolean canPreemptMovement = survivalEmergency && isMovementAction(lastRememberedAction);
        // Navigation can yield to danger even between broad decisions. Committed attacks,
        // casting, and dodges retain their recovery; a reaction cooldown prevents chaining evasions.
        LivingEntity currentTarget = mob.getTarget();
        if (isMovementAction(lastRememberedAction) && readyTick("Reaction") <= now
                && now >= nextThreatCheck && currentTarget != null && validTarget(currentTarget)) {
            nextThreatCheck = now + 2;
            if (NpcCombatReactions.projectile(mob) != null || currentTarget.swinging || isCasting(currentTarget)) {
                if (reactToThreat(observe(currentTarget), now)) {
                    diagnostic("react_to_threat");
                    nextDecisionTick = now + 2;
                    return;
                }
            }
        }
        if ((now < actionLockUntil && !canPreemptMovement) || now < nextDecisionTick) {
            return;
        }
        if (canPreemptMovement && now < actionLockUntil) {
            // Survival may cancel an in-progress reposition, but the next branch still issues
            // only one replacement action.
            mob.getNavigation().stop();
            actionLockUntil = now;
        }
        nextDecisionTick = now + MIN_DECISION_DELAY + mob.getRandom().nextInt(RANDOM_DECISION_DELAY);
        recentDamageByAttacker.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        // Do not periodically replace the live equipped stack with the profile copy: doing so
        // would reset durability and weapon-stored qi during combat.
        if (mob.tickCount == 1 || mob.getMainHandItem().isEmpty()) {
            NpcProfile.equipCombatWeapon(mob);
        }

        LivingEntity target = selectTarget();
        if (target == null) {
            battle.reset();
            rhythm.reset();
            mob.removeEffect(ModEffects.NPC_WEAPON_GUARD);
            mob.setTarget(null);
            clearFlightIntent();
            clearCoverPlan();
            if (healthRatio(mob) < SURVIVAL_HP_RATIO) {
                tryBestHealing(ActionKind.SURVIVAL);
            }
            return;
        }
        if (mob.getTarget() != target) {
            rhythm.reset();
            mob.setTarget(target);
        }
        NpcCultivation.interrupt(mob, "combat");
        battle.begin(target.getUUID(), now, realmIndex(mob), NpcProfile.personality(mob, archetype()));
        if (!mob.hasLineOfSight(target)) {
            searchLastSeen(target, now);
            return;
        }
        Context context = observe(target);
        updateBattle(context, now);

        if (mob.hasEffect(ModEffects.QI_GATHERING)) {
            if (isThreatenedDuringRecovery(context)) {
                cancelQiRecovery(now);
                moveToSafety(context, true, true, ActionKind.SURVIVAL);
            }
            return;
        }

        chooseIntent(context, now);
    }

    private record Intent(String name, double utility, BooleanSupplier perform) {}

    private void chooseIntent(Context c, long now) {
        if (battle.frenzy()) {
            feintUntil=0; clearCoverPlan();
            if (deathFrenzy(c)) diagnostic("last_stand");
            return;
        }
        List<Intent> choices=new ArrayList<>();
        boolean survival=battle.mode()==NpcBattlePlan.Mode.SURVIVAL;
        choices.add(new Intent("survive",survival?1000:-1,()->immediateSurvival(c)));
        choices.add(new Intent("react_to_threat",readyTick("Reaction")<=now?940:-1,()->reactToThreat(c,now)));
        choices.add(new Intent("interrupt",c.targetDangerousAction()?900:-1,()->interrupt(c)));
        choices.add(new Intent("avoid_desperation",knowledge.frenzy()?850:-1,()->
                tryCast(c.target(),ModSpells.DIELANG_SHIELD,ActionKind.DEFENSE)
                        || moveToSafety(c,true,true,ActionKind.MOVEMENT) || rangedAttack(c)));
        choices.add(new Intent("dodge_preparation",c.targetDangerousAction()?800:-1,()->
                controlOrDefense(c)||moveToSafety(c,true,true,ActionKind.MOVEMENT)));
        choices.add(new Intent("recover_qi",c.selfQiRatio()<LOW_QI_RATIO?650:-1,()->
                useResource(c,NpcCombatResources.Purpose.QI)||lowQiBehavior(c)));
        choices.add(new Intent("cleanse",c.negativeEffects()>0?600:-1,()->
                useResource(c,NpcCombatResources.Purpose.CLEANSE)||tryCast(null,ModSpells.CLEANSE,ActionKind.DEFENSE)));
        choices.add(new Intent("heal",c.selfHealthRatio()<.65F?610:-1,()->
                healingOpening()&&tryBestHealing(ActionKind.DEFENSE)));
        choices.add(new Intent("protect_ally",battle.loyal()&&threatensWoundedAlly(c.target())?560:-1,()->
                interrupt(c)||bestAttack(c)));
        choices.add(new Intent("formation",NpcBattlefield.needsFormationResponse(mob,c.target())?520:-1,()->formationResponse(c)));
        choices.add(new Intent("external_qi",!survival&&readyTick("ExternalQi")<=now?490:-1,
                ()->tryExternalQi(c,false)));
        choices.add(new Intent("counter_pursuit",feintUntil>now?480:-1,()->continueFeint(c,now)));
        choices.add(new Intent("counterattack",!survival&&counterUntil>now?475:-1,()->
                weaponAttack(c,c.targetBlocking())||tryExternalQi(c,true)||approachPredicted(c)));
        choices.add(new Intent("close_gap",!survival&&c.distance()>(c.weaponStyle().isMelee()
                &&!hasUsableRangedSpell(c)?weaponRange(c.weaponStyle())+.75:Math.max(8,weaponRange(c.weaponStyle())+2))?470:-1,
                ()->approachPredicted(c)||tryMovementSpell(c,false,ActionKind.MOVEMENT)));
        choices.add(new Intent("combo_followup",!survival&&rhythm.follows(c.target().getUUID(),now)?460:-1,()->
                weaponAttack(c,c.targetBlocking())||approachPredicted(c)));
        choices.add(new Intent("reset_spacing",!survival&&rhythm.recovering(now)?455:-1,()->
                combatStep(c,false)||reposition(c,ActionKind.REPOSITION)));
        choices.add(new Intent("pressure",battle.pressure()||counterUntil>now?430:-1,()->
                controlOrDefense(c)||bestAttack(c)||maintainDistance(c)));
        choices.add(new Intent("probe",battle.probing()?350:-1,()->probe(c)));
        choices.add(new Intent("distance",220+Math.min(90,Math.abs(c.distance()-c.preferredDistance())*12),()->maintainDistance(c)));
        choices.add(new Intent("defense",200+(shieldUseful(c)?60:0),()->controlOrDefense(c)));
        choices.add(new Intent("deceive",battle.cunning()&&!battle.probing()&&!survival&&readyTick("Feint")<=now?250:-1,()->startFeint(c,now)));
        choices.add(new Intent("attack",310+((battle.momentum()==NpcBattlePlan.Momentum.ADVANTAGE)?45:0),()->bestAttack(c)));
        choices.add(new Intent("reposition",20,()->reposition(c,ActionKind.REPOSITION)));
        // Sample once per candidate: a random comparator would violate ordering and resample scores.
        choices.replaceAll(i->new Intent(i.name(),i.utility()<0?-1:i.utility()+mob.getRandom().nextDouble()*6,i.perform()));
        choices.sort(Comparator.comparingDouble(Intent::utility).reversed());
        for (Intent intent:choices) if (intent.utility()>=0&&intent.perform().getAsBoolean()) {
            diagnostic(intent.name()); return;
        }
        diagnostic("wait_for_opening");
    }

    private boolean reactToThreat(Context context, long now) {
        // Check only visible trajectories and visible melee wind-ups, never an opponent's hidden skill data.
        var projectile = NpcCombatReactions.projectile(mob);
        Vec3 toward = mob.position().subtract(context.target().position()).multiply(1,0,1).normalize();
        boolean melee = context.distance() <= weaponRange(context.weaponStyle())+1
                && context.target().swinging
                && context.target().getLookAngle().multiply(1,0,1).normalize().dot(toward) > .25;
        if (projectile == null && !melee && !context.targetCasting()) return false;
        Vec3 threat = projectile == null ? context.target().position() : projectile.position();
        boolean dodgeFirst = projectile != null || context.targetCasting() || mob.getRandom().nextBoolean();
        if (dodgeFirst && combatStep(context,true,threat)) {
            setReadyTick("Reaction",now+24+mob.getRandom().nextInt(13));
            counterUntil=now+32;
            rhythm.reset();
            return true;
        }
        if (NpcCombatReactions.startGuard(mob)) {
            Vec3 delta=threat.subtract(mob.position());
            mob.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
            mob.setYHeadRot(mob.getYRot());
            mob.getNavigation().stop();clearFlightIntent();holdForAction();
            remember(ActionKind.DEFENSE,"weapon_guard");
            actionLockUntil = Math.max(actionLockUntil, mob.getPersistentData().getLong("NpcGuardRecoveryUntil"));
            setReadyTick("Reaction",now+32);
            counterUntil=now+36;
            rhythm.reset();
            return true;
        }
        if (!dodgeFirst && combatStep(context,true,threat)) {
            setReadyTick("Reaction",now+28);
            counterUntil=now+32;
            rhythm.reset();
            return true;
        }
        return false;
    }

    private boolean combatStep(Context context, boolean evasive) {
        return combatStep(context,evasive,context.target().position());
    }

    private boolean combatStep(Context context, boolean evasive, Vec3 threat) {
        if (!mob.onGround() && !NpcSpellcasting.canFly(mob)) return false;
        Vec3 radial=mob.position().subtract(threat).multiply(1,0,1).normalize();
        if (radial.lengthSqr()<.01) radial=new Vec3(1,0,0);
        Vec3 side=new Vec3(-radial.z,0,radial.x).scale(strafeSign);
        // Try both sides, then a backstep. Validate the whole route before adding any impulse.
        for (Vec3 direction:List.of(side,side.reverse(),radial)) {
            Vec3 destination=mob.position().add(direction.scale(evasive?1.8:1.2));
            boolean safe=true;
            for(int sample=1;sample<=6;sample++) {
                Vec3 point=mob.position().lerp(destination,sample/6D);
                if (!isSafe(point)||!isClearTravelSpace(point)
                        ||CenterEffectSupport.isForcefieldRouteBlocked(mob,mob.position(),point)) {safe=false;break;}
            }
            if (!safe || !issueMovement(destination,evasive?1.8:1.15,ActionKind.REPOSITION)) continue;
            if (!NpcSpellcasting.canFly(mob)) {
                Vec3 impulse=direction.scale(evasive?.42:.2);
                mob.setDeltaMovement(impulse.x,mob.getDeltaMovement().y,impulse.z);
                if (evasive) com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashStarted(mob,direction,.25F,7);
                mob.hurtMarked=true;
            }
            mob.lookAt(context.target(),60,60);
            strafeSign=-strafeSign;
            actionLockUntil=Math.max(actionLockUntil,mob.level().getGameTime()+(evasive?7:5));
            return true;
        }
        return false;
    }

    private void diagnostic(String intent) {
        var state=mob.getPersistentData().getCompound(COMBAT_STATE_TAG);
        state.putString("Mode",battle.mode().name());state.putString("Phase",battle.phase().name());
        state.putString("Momentum",battle.momentum().name());state.putString("Intent",intent);
        state.putFloat("SurvivalConfidence",battle.confidence());state.putInt("FailedEscapes",battle.escapeFailures());
        state.putString("EnemyKnowledge",knowledge==null?"UNKNOWN":knowledge.confidence().name());
        mob.getPersistentData().put(COMBAT_STATE_TAG,state);
    }

    private void updateBattle(Context c,long now) {
        NpcCombatKnowledge.shareObserved(mob,c.target());
        if(NpcCombatKnowledge.newThreat(mob,c.target()))battle.reassess(now);
        boolean route=c.escapeDestination()!=null;
        if (escapeProgressOrigin!=null&&now-escapeProgressTick>=24) {
            if (mob.position().distanceToSqr(escapeProgressOrigin)<1||c.distance()<escapeThreatDistance-.5) {battle.failedEscape(now);stuckUntil=now+80;}
            else {battle.escaped(now);stuckUntil=0;}
            escapeProgressOrigin=null;
        }
        if (!route) battle.failedEscape(now);
        route&=now>=stuckUntil;
        boolean movement=NpcProfile.learnedSpellIds(mob,archetype()).stream().anyMatch(id-> {
            var p=NpcSpellcasting.profile(id);
            return p!=null&&p.role()==NpcSpellcasting.Role.MOVEMENT&&readyTick("Spell."+id)<=now
                    &&NpcSpellcasting.canCast(mob,c.target(),id)
                    &&resolveMovementDestination(MovementFollowup.ESCAPE,c.target(),id)!=null;
        });
        boolean healing=healthRatio(mob)<.8F&&NpcSpellcasting.healingSpellIds().stream()
                .anyMatch(id->readyTick("Spell."+id)<=now&&NpcSpellcasting.canCast(mob,null,id));
        healing|=NpcCombatResources.available(mob,NpcCombatResources.Purpose.HEALTH,null);
        movement|=NpcCombatResources.available(mob,NpcCombatResources.Purpose.ESCAPE,c.escapeDestination());
        boolean defense=DielangShield.remaining(mob)>0||NpcCombatResources.shield(mob)>0
                || NpcCombatResources.available(mob,NpcCombatResources.Purpose.SHIELD,null);
        battle.update(new NpcBattlePlan.Situation(c.selfHealthRatio(),c.selfQiRatio(),c.negativeEffects(),
                c.nearbyAllies(),Math.max(1,c.nearbyEnemies()),c.targetRealm()-c.selfRealm(),
                knowledge.health(),knowledge.qi(),knowledge.opening(),route,movement,healing,defense,
                !route&&CenterEffectSupport.isDangerousPosition(mob,mob.position()),
                NpcCombatKnowledge.recentDamage(mob,c.target()),NpcProfile.isSparring(mob)),now,knowledge.observations());
    }

    private void searchLastSeen(LivingEntity target,long now) {
        knowledge=NpcCombatKnowledge.view(mob,target);
        if (knowledge.lastPosition()==null||now-knowledge.lastSeen()>100) {
            mob.setTarget(null);mob.setLastHurtByMob(null);recentDamageByAttacker.remove(target.getUUID());
            mob.getNavigation().stop();clearFlightIntent();battle.reset();return;
        }
        if (healthRatio(mob)<SURVIVAL_HP_RATIO||battle.mode()==NpcBattlePlan.Mode.SURVIVAL) {
            escapeProgressOrigin=null;stuckUntil=0;battle.escaped(now);
            if (tryBestHealing(ActionKind.SURVIVAL))return;
            if (qiRatio()<COVER_QI_RATIO&&!hasImmediateRecoveryThreat())tryCast(null,ModSpells.QI_GATHERING,ActionKind.RECOVER_QI);
            return;
        }
        if (mob.position().distanceToSqr(knowledge.lastPosition())>4)
            issueMovement(knowledge.lastPosition(),.9,ActionKind.MOVEMENT);
        diagnostic("search_last_seen");
    }

    private boolean probe(Context c) {
        if (c.distance()<=weaponRange(c.weaponStyle()))return weaponAttack(c,false);
        if (tryExternalQi(c,false))return true;
        String cheap=selectSpell(c,id->NpcSpellcasting.isAttackSpell(id)
                &&NpcSpellcasting.qiCost(mob,id)<=Math.max(1,NpcProfile.maxQi(mob,archetype())*.07),false);
        return !cheap.isBlank()&&tryCast(c.target(),cheap,ActionKind.SPELL_ATTACK);
    }

    private boolean rangedAttack(Context c) {
        String spell=selectSpell(c,NpcSpellcasting::isAttackSpell,false);
        return !spell.isBlank()&&tryCast(c.target(),spell,ActionKind.SPELL_ATTACK)||tryExternalQi(c,false);
    }

    private boolean deathFrenzy(Context c) {
        if (NpcCombatResources.burnEssence(mob)) {finishCastAction(ActionKind.SPELL_ATTACK,"blood_combustion");return true;}
        if (c.distance()<=5&&NpcCombatResources.detonateArtifact(mob,c.target())) {
            finishCastAction(ActionKind.SPELL_ATTACK,"artifact_detonation");return true;
        }
        return bestAttack(c)||tryMovementSpell(c,false,ActionKind.MOVEMENT)
                || approachPredicted(c);
    }

    private boolean useResource(Context c,NpcCombatResources.Purpose purpose) {
        if (!NpcCombatResources.use(mob,purpose,c.escapeDestination()))return false;
        finishCastAction(purpose==NpcCombatResources.Purpose.QI?ActionKind.RECOVER_QI:ActionKind.SURVIVAL,"resource_"+purpose.name());
        return true;
    }

    private boolean startFeint(Context c,long now) {
        if (c.selfHealthRatio()<.5F||c.selfQiRatio()<.35F||c.distance()>12||knowledge.frenzy())return false;
        Vec3 before=mob.position();Vec3 enemy=c.target().position();
        if (!moveToSafety(c,true,true,ActionKind.FEINT))return false;
        feintOrigin=before;feintEnemyOrigin=enemy;feintUntil=now+60;setReadyTick("Feint",now+240);return true;
    }

    private boolean continueFeint(Context c,long now) {
        if (c.target().position().distanceTo(feintOrigin)+1.5<feintEnemyOrigin.distanceTo(feintOrigin)) {
            feintUntil=0;counterUntil=now+40;
            return controlOrDefense(c)||bestAttack(c);
        }
        if (c.targetDangerousAction()||c.selfHealthRatio()<.4F) {feintUntil=0;return false;}
        return moveToSafety(c,true,true,ActionKind.FEINT);
    }

    private boolean threatensWoundedAlly(LivingEntity target) {
        return target instanceof Mob attacker&&attacker.getTarget()!=null
                &&mob.hasLineOfSight(attacker.getTarget())&&NpcProfile.areCombatAllies(mob,attacker.getTarget())
                &&healthRatio(attacker.getTarget())<.4F;
    }

    private boolean formationResponse(Context c) {
        Vec3 refuge=NpcBattlefield.refuge(mob);
        if (refuge!=null&&(battle.mode()==NpcBattlePlan.Mode.SURVIVAL||c.selfHealthRatio()<.6F)
                &&mob.position().distanceToSqr(refuge)>4&&issueMovement(refuge,1.15,ActionKind.MOVEMENT))return true;
        return NpcBattlefield.breakHostileCore(mob,c.target())&&rememberFormationStrike()
                ||rangedAttack(c)||reposition(c,ActionKind.REPOSITION);
    }

    private boolean rememberFormationStrike(){remember(ActionKind.WEAPON_HEAVY,"formation_component");return true;}

    public void recordDamage(DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        NpcCultivation.onHurt(mob);
        LivingEntity sourceActor = source.getEntity() instanceof LivingEntity living ? living : null;
        if (sourceActor == mob || sourceActor != null && NpcProfile.areCombatAllies(mob, sourceActor)) {
            return;
        }
        long now = mob.level().getGameTime();
        cancelPendingPill(now);
        cancelPendingCast(now);
        if (mob.hasEffect(ModEffects.QI_GATHERING)) {
            cancelQiRecovery(now);
        }
        if (sourceActor == null) {
            return;
        }
        recentDamageByAttacker.compute(sourceActor.getUUID(), (ignored, previous) ->
                new RecentDamage((previous == null ? 0.0F : previous.amount()) + amount, now + 200L));
    }

    private LivingEntity selectTarget() {
        Set<LivingEntity> candidates = new LinkedHashSet<>();
        addCandidate(candidates, mob.getTarget());
        addCandidate(candidates, mob.getLastHurtByMob());
        AABB search = mob.getBoundingBox().inflate(TARGET_SEARCH_RANGE);
        for (LivingEntity nearby : mob.level().getEntitiesOfClass(LivingEntity.class, search)) {
            if (isHostileCandidate(nearby)) {
                candidates.add(nearby);
            }
        }
        return candidates.stream()
                .filter(this::validTarget)
                .max(Comparator.comparingDouble(this::targetScore))
                .orElse(null);
    }

    private void addCandidate(Set<LivingEntity> candidates, LivingEntity candidate) {
        if (candidate != null) {
            candidates.add(candidate);
        }
    }

    private boolean isHostileCandidate(LivingEntity candidate) {
        if (candidate == mob || !candidate.isAlive() || NpcProfile.areCombatAllies(mob, candidate)) {
            return false;
        }
        if (!mob.hasLineOfSight(candidate)) return candidate==mob.getTarget()
                &&NpcCombatKnowledge.view(mob,candidate).lastPosition()!=null
                &&mob.level().getGameTime()-NpcCombatKnowledge.view(mob,candidate).lastSeen()<100;
        if (candidate instanceof EscapedYuanyingCoreEntity core&&NpcCombatKnowledge.witnessedCore(mob,core))return true;
        if (candidate instanceof Mob attacker&&attacker.getTarget()!=null
                &&mob.hasLineOfSight(attacker.getTarget())&&NpcProfile.areCombatAllies(mob,attacker.getTarget()))return true;
        // Every cultivator tier uses this shared brain. Hostile vanilla and
        // modded mobs that enter the ten-block defense radius are acquired
        // even when they have not attacked the cultivator yet.
        if (candidate instanceof Enemy
                && mob.distanceToSqr(candidate) <= HOSTILE_MOB_AGGRO_RADIUS_SQUARED) {
            return true;
        }
        if (!NpcProfile.isSparring(mob)
                && isHostileBeast(candidate)
                && mob.hasLineOfSight(candidate)) {
            return true;
        }
        if (candidate == mob.getTarget() || candidate == mob.getLastHurtByMob()
                || recentDamageByAttacker.containsKey(candidate.getUUID())) {
            return true;
        }
        if (candidate instanceof Mob other
                && (other.getTarget() == mob || other.getLastHurtByMob() == mob)) {
            return true;
        }
        LivingEntity current = mob.getTarget();
        if (current != null && current != candidate && NpcProfile.areCombatAllies(current, candidate)) {
            return true;
        }
        return candidate instanceof ServerPlayer player && NpcProfile.shouldAttackPlayer(mob, player);
    }

    private static boolean isHostileBeast(LivingEntity candidate) {
        return candidate instanceof CrimsonScaleFlameLizardEntity
                || candidate instanceof CrimsonSkyFlameFloodDragonEntity
                || candidate instanceof BloodApeDemonGeneralEntity
                || candidate instanceof XuanjinZhengEntity beast && beast.isFree();
    }

    private boolean validTarget(LivingEntity target) {
        return target != mob && target.isAlive() && target.level() == mob.level()
                && !NpcProfile.areCombatAllies(mob, target)
                && mob.distanceToSqr(target) <= TARGET_SEARCH_RANGE * TARGET_SEARCH_RANGE
                && (!(target instanceof ServerPlayer player)
                || NpcProfile.isSparPair(mob, player)
                || NpcProfile.shouldAttackPlayer(mob, player));
    }

    private double targetScore(LivingEntity target) {
        double distance = Math.sqrt(mob.distanceToSqr(target));
        double score = 180.0D - distance * 3.0D;
        if (target instanceof Enemy
                && distance <= HOSTILE_MOB_AGGRO_RADIUS) {
            // Nearby hostile mobs take priority over distant social enemies so
            // the cultivator actually defends its immediate surroundings.
            score += 1000.0D;
        }
        RecentDamage recentDamage = recentDamageByAttacker.get(target.getUUID());
        score += (recentDamage == null ? 0.0F : recentDamage.amount()) * 5.0D;
        var known=NpcCombatKnowledge.view(mob,target);
        score += (1.0D - known.health()) * 70.0D;
        score += known.realmDelta() * 10.0D;
        if (mob.hasLineOfSight(target)&&(isCasting(target) || isRecovering(target))) score += 85.0D;
        if (threatensWoundedAlly(target)) score+=battle.loyal()?200:70;
        if (isRangedCombatant(target)) score += 30.0D;
        if (target instanceof Mob other && other.getTarget() == mob) score += 45.0D;
        if (canTargetReachQuick(target)) score += 20.0D;
        return score;
    }

    private Context observe(LivingEntity target) {
        double distance = Math.sqrt(mob.distanceToSqr(target));
        boolean lineOfSight = mob.hasLineOfSight(target);
        WeaponStyle weaponStyle = weaponStyle(mob.getMainHandItem());
        int selfRealm = realmIndex(mob);
        knowledge = NpcCombatKnowledge.observe(mob,target,isCasting(target),isRecovering(target),isBlocking(target));
        int targetRealm = selfRealm+knowledge.realmDelta();
        double preferred = preferredDistance(weaponStyle);
        if (knowledge.frenzy()&&battle.experience()>.2F) preferred=Math.max(preferred,12);
        if (knowledge.mobile()&&weaponStyle==WeaponStyle.SPEAR) preferred+=.5;
        if (targetRealm > selfRealm) {
            preferred += 1.5D;
        } else if (targetRealm < selfRealm) {
            preferred = Math.max(1.4D, preferred - 1.0D);
        }
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(LOCAL_COMBAT_RADIUS),
                living -> living != mob && living.isAlive()&&mob.hasLineOfSight(living));
        int allies = 0;
        int alliesNearTarget = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(9.0D),
                living -> living != mob && living.isAlive()
                        && NpcProfile.areCombatAllies(mob, living)).size();
        int enemies = 0;
        int closeEnemies = 0;
        for (LivingEntity living : nearby) {
            if (NpcProfile.areCombatAllies(mob, living)) {
                allies++;
            } else if (isHostileCandidate(living) || living == target) {
                enemies++;
                if (living.distanceToSqr(mob) <= 4.5D * 4.5D) {
                    closeEnemies++;
                }
            }
        }
        int clusteredEnemies = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(5.0D),
                living -> living.isAlive()
                        && (living == target || isHostileCandidate(living))).size();
        Vec3 escape = findSafeDestination(target, true, true);
        boolean casting = isCasting(target);
        boolean recovering = isRecovering(target);
        boolean heavyPreparing = target instanceof ServerPlayer player
                && CombatStateManager.isPreparingHeavyAttack(player);
        boolean targetDangerous = casting || recovering || heavyPreparing;
        return new Context(
                target,
                distance,
                lineOfSight,
                healthRatio(mob),
                qiRatio(),
                knowledge.health(),
                selfRealm,
                targetRealm,
                weaponStyle,
                preferred,
                isBlocking(target),
                casting,
                isFlying(target),
                isRetreating(target),
                recovering,
                targetDangerous,
                allies,
                enemies,
                closeEnemies,
                clusteredEnemies,
                alliesNearTarget,
                harmfulEffectCount(mob),
                escape,
                canTargetReach(target),
                isRangedCombatant(target));
    }

    private boolean immediateSurvival(Context context) {
        if (context.selfHealthRatio()<SURVIVAL_HP_RATIO&&NpcProfile.tryUseRoyalPendant(mob)) {
            remember(ActionKind.SURVIVAL, "");
            return true;
        }
        if (context.negativeEffects() > 0
                && tryCast(null, ModSpells.CLEANSE, ActionKind.SURVIVAL)) {
            return true;
        }
        if (tryBestHealing(ActionKind.SURVIVAL)) {
            return true;
        }
        if (context.selfQiRatio()<LOW_QI_RATIO&&(useResource(context,NpcCombatResources.Purpose.QI)
                ||context.distance()>=16&&lowQiBehavior(context)))return true;
        if (context.selfHealthRatio()<.25F&&useResource(context,NpcCombatResources.Purpose.ESCAPE))return true;
        if (shieldUseful(context)&&useResource(context,NpcCombatResources.Purpose.SHIELD))return true;
        if (shieldUseful(context)
                && tryCast(context.target(), ModSpells.DIELANG_SHIELD, ActionKind.SURVIVAL)) {
            return true;
        }
        if (tryMovementSpell(context, true, ActionKind.SURVIVAL)) {
            return true;
        }
        if (moveToSafety(context, true, true, ActionKind.SURVIVAL)) {
            return true;
        }
        if (useResource(context,NpcCombatResources.Purpose.ESCAPE))return true;
        // Fight only when escape is impossible, or a single quick hit can finish the target.
        if (context.escapeDestination() == null || context.targetHealthRatio() <= 0.10F) {
            return finishAttempt(context);
        }
        return false;
    }

    private boolean tryBestHealing(ActionKind action) {
        if (!healingOpening()) return false;
        float health = healthRatio(mob);
        if (health < .8F && healingOpening() && startHealingPill(action)) return true;
        if (health < 0.45F
                && tryCast(null, ModSpells.SHENGXI_BAOFA, action)) {
            return true;
        }
        boolean bleeding = mob.hasEffect(
                com.example.immortal_cultivation_mod.magicritual.effect.ModEffects.BLEEDING);
        if ((bleeding || health < 0.65F)
                && tryCast(null, ModSpells.XUE_GUIYUAN_SHU, action)) {
            return true;
        }
        if (tryCast(null, ModSpells.MULING_SHENGXI, action)) {
            return true;
        }
        return tryCast(null, ModSpells.REGENERATION, action);
    }

    private boolean healingOpening() {
        if (NpcCombatReactions.projectile(mob) != null) return false;
        for (LivingEntity enemy : mob.level().getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(8), living -> living != mob && validTarget(living)
                        && isHostileCandidate(living))) {
            if (!mob.hasLineOfSight(enemy) || isControlled(enemy) || isDistracted(enemy)) continue;
            double reach = Math.max(3, enemy.getBbWidth() + 1.5);
            Vec3 closing = mob.position().subtract(enemy.position()).normalize();
            double approach = Math.max(0, enemy.getDeltaMovement().dot(closing));
            if (enemy.distanceTo(mob) <= reach + approach * 12 || isCasting(enemy)) return false;
        }
        return true;
    }

    private boolean startHealingPill(ActionKind action) {
        if (pendingPill != null || pendingCast != null || mob.hurtTime > 0) return false;
        ItemStack pill = NpcCombatResources.healingPill(mob);
        if (pill.isEmpty()) return false;
        long now = mob.level().getGameTime();
        LivingEntity enemy = mob.getTarget();
        pendingPill = new PendingPill(pill, enemy == null ? null : enemy.getUUID(), now + 12);
        mob.removeEffect(ModEffects.NPC_WEAPON_GUARD);
        mob.getNavigation().stop();clearFlightIntent();holdForAction();
        mob.swing(InteractionHand.MAIN_HAND, true);
        remember(action, "healing_pill");
        actionLockUntil = Math.max(actionLockUntil, now + 18);
        return true;
    }

    private void tickPendingPill(long now) {
        LivingEntity enemy = mob.getTarget();
        UUID targetId = enemy == null ? null : enemy.getUUID();
        if (!java.util.Objects.equals(pendingPill.targetId(), targetId)
                || mob.hasEffect(ModEffects.SPIRIT_SEAL) || !healingOpening()) {
            cancelPendingPill(now);
            return;
        }
        mob.getNavigation().stop();holdForAction();
        if (now < pendingPill.releaseTick()) return;
        ItemStack pill = pendingPill.stack();
        pendingPill = null;
        if (!NpcCombatResources.finishHealingPill(mob, pill)) {
            actionLockUntil = now + 2;
        }
        nextDecisionTick = now + 2;
    }

    private void cancelPendingPill(long now) {
        if (pendingPill == null) return;
        pendingPill = null;
        actionLockUntil = now + 4;
        nextDecisionTick = now + 4;
    }

    private boolean interrupt(Context context) {
        if (tryAttackTalisman(context, ActionKind.INTERRUPT)) {
            return true;
        }
        // Only Dingshen actually stops the registered preparation state. Blindness and
        // inverted movement remain useful control, but are not fake "interrupts".
        Predicate<String> fastControl = ModSpells.DINGSHEN::equals;
        String control = selectSpell(context, fastControl, true);
        if (!control.isBlank() && tryCast(context.target(), control, ActionKind.INTERRUPT)) {
            return true;
        }
        if (tryExternalQi(context, true)) {
            return true;
        }
        if (context.distance() <= weaponRange(context.weaponStyle()) && context.lineOfSight()
                && weaponAttack(context, context.targetBlocking())) {
            return true;
        }
        String fastAttack = selectSpell(context, id -> {
            NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(id);
            return profile != null && profile.role() == NpcSpellcasting.Role.ATTACK && profile.fast();
        }, true);
        return !fastAttack.isBlank() && tryCast(context.target(), fastAttack, ActionKind.INTERRUPT);
    }

    private boolean lowQiBehavior(Context context) {
        boolean recoveryOpening = context.distance() >= 16.0D
                || !context.lineOfSight()
                || isControlled(context.target())
                || isDistracted(context.target());
        boolean safeToRecover = recoveryOpening
                && !hasImmediateRecoveryThreat();
        if (safeToRecover && hasStableRecoveryFooting()
                && tryCast(null, ModSpells.QI_GATHERING, ActionKind.RECOVER_QI)) {
            mob.getNavigation().stop();
            return true;
        }
        if (safeToRecover && NpcSpellcasting.canFly(mob) && !hasStableRecoveryFooting()) {
            Vec3 landing = findRecoveryLanding();
            if (landing != null && issueMovement(landing, 0.85D, ActionKind.RECOVER_QI)) {
                return true;
            }
        }
        if (moveToSafety(context, true, true, ActionKind.RECOVER_QI)) {
            return true;
        }
        return reposition(context, ActionKind.RECOVER_QI);
    }

    private boolean maintainDistance(Context context) {
        double preferred = context.preferredDistance();
        if (continueCoverPlan(context)) {
            return true;
        }
        if (context.closeEnemies() >= 3
                && context.closeEnemies() > context.nearbyAllies() + 1
                && moveToSafety(context, true, true, ActionKind.MOVEMENT)) {
            return true;
        }
        if (context.targetRealm() > context.selfRealm()
                && context.targetRanged()
                && context.lineOfSight()
                && moveToSafety(context, true, true, ActionKind.MOVEMENT, true)) {
            return true;
        }
        if (context.weaponStyle() == WeaponStyle.SPEAR
                && lastActionWasWeaponAttack()
                && moveToSafety(context, true, false, ActionKind.MOVEMENT)) {
            return true;
        }
        if (context.targetFlying() && context.weaponStyle().isMelee()
                && context.distance() > weaponRange(context.weaponStyle())) {
            if (NpcSpellcasting.hasCreativeFlight(mob) && approachPredicted(context)) return true;
            if (tryMovementSpell(context, false, ActionKind.MOVEMENT)) {
                return true;
            }
            String ranged = selectSpell(context, id -> {
                var profile = NpcSpellcasting.profile(id);
                return profile != null && (profile.role() == NpcSpellcasting.Role.ATTACK
                        || profile.role() == NpcSpellcasting.Role.CONTROL);
            }, false);
            if (!ranged.isBlank() && tryCast(context.target(), ranged, ActionKind.SPELL_ATTACK)) {
                return true;
            }
            if (tryExternalQi(context, false)) {
                return true;
            }
            return reposition(context, ActionKind.MOVEMENT);
        }
        if (context.distance() < Math.max(1.4D, preferred - 1.0D)
                && (context.weaponStyle() != WeaponStyle.FAN || hasUsableRangedSpell(context))) {
            return moveToSafety(context, true, false, ActionKind.MOVEMENT);
        }
        double weaponReach = weaponRange(context.weaponStyle());
        boolean usableRangedSpell = hasUsableRangedSpell(context);
        if (context.targetRetreating() && context.distance() > weaponReach) {
            if (context.distance() > weaponReach + 2.0D
                    && tryMovementSpell(context, false, ActionKind.MOVEMENT)) {
                return true;
            }
            if (tryExternalQi(context, false)) {
                return true;
            }
            if (!usableRangedSpell) {
                return approachPredicted(context);
            }
        }
        if (context.distance() > weaponReach + 0.25D && !usableRangedSpell) {
            if (tryExternalQi(context, false)) {
                return true;
            }
            if (context.distance() > weaponReach + 3.0D
                    && tryMovementSpell(context, false, ActionKind.MOVEMENT)) {
                return true;
            }
            return approachPredicted(context);
        }
        return false;
    }

    private boolean controlOrDefense(Context context) {
        boolean danger = context.nearbyEnemies() > context.nearbyAllies() + 1
                || context.targetRealm() > context.selfRealm()
                || context.targetBlocking()
                || context.targetCasting();
        if (danger && DielangShield.remaining(mob) < 12.0F
                && tryCast(context.target(), ModSpells.DIELANG_SHIELD, ActionKind.DEFENSE)) {
            return true;
        }
        if (context.nearbyEnemies() >= 3 || context.targetBlocking()
                ||battle.pressure()&&(knowledge!=null&&knowledge.opening()||context.targetRetreating())) {
            String control = selectSpell(context, id -> {
                var profile = NpcSpellcasting.profile(id);
                return profile != null && profile.role() == NpcSpellcasting.Role.CONTROL
                        && (!profile.area() || areaTargetCount(context, id) >= 2
                        && areaAlliesAtRisk(context, id) == 0);
            }, false);
            if (!control.isBlank() && tryCast(context.target(), control, ActionKind.CONTROL)) {
                return true;
            }
        }
        return false;
    }

    private boolean shieldUseful(Context context) {
        return context.nearbyEnemies() > context.nearbyAllies() + 1
                || context.targetRealm() > context.selfRealm()
                || context.targetBlocking()
                || context.targetCasting()
                || context.canTargetReach()
                && context.distance() <= context.preferredDistance() + 3.0D
                || context.lineOfSight() && context.targetRanged();
    }

    private boolean bestAttack(Context context) {
        boolean reveal=!battle.probing()||battle.mode()!=NpcBattlePlan.Mode.NORMAL||context.targetDangerousAction();
        if (reveal&&NpcProfile.tryUseIdentityCombatAbility(mob)) {
            finishCastAction(ActionKind.SPELL_ATTACK, "identity_ability");
            return true;
        }
        if (reveal&&tryAttackTalisman(context, ActionKind.SPELL_ATTACK)) {
            return true;
        }

        boolean weaponReady = context.lineOfSight()
                && context.distance() <= weaponRange(context.weaponStyle());
        String spell = selectSpell(context, id -> {
            var profile = NpcSpellcasting.profile(id);
            if (profile == null || profile.role() != NpcSpellcasting.Role.ATTACK) {
                return false;
            }
            if (profile.area() && areaTargetCount(context, id) < 2
                    && (context.targetHealthRatio() < 0.45F
                    || context.targetRealm() < context.selfRealm())) {
                return false;
            }
            if (profile.area() && areaAlliesAtRisk(context, id) > 0) {
                return false;
            }
            return true;
        }, false);

        // Alternate weapon and spell openings instead of repeating one move forever.
        if (!battle.frenzy()&&weaponReady && (!recent(ActionKind.WEAPON_LIGHT) && !recent(ActionKind.WEAPON_HEAVY)
                || spell.isBlank() || comboStep % 2 == 0)) {
            if (weaponAttack(context, context.targetBlocking())) {
                return true;
            }
        }
        long now = mob.level().getGameTime();
        boolean bannerUseful = context.lineOfSight()
                && (context.targetDangerousAction()
                || context.targetFlying()
                || context.targetRetreating()
                || !weaponReady
                || context.targetRealm() >= context.selfRealm());
        if (reveal&&bannerUseful && readyTick("SoulBanner") <= now
                && NpcProfile.tryUseSoulBanner(mob, context.target())) {
            setReadyTick("SoulBanner", now + 80L + mob.getRandom().nextInt(41));
            finishCastAction(ActionKind.SPELL_ATTACK, "soul_banner");
            return true;
        }
        if (!spell.isBlank() && tryCast(context.target(), spell, ActionKind.SPELL_ATTACK)) {
            return true;
        }
        if (tryExternalQi(context, false)) {
            return true;
        }
        return weaponReady && weaponAttack(context, context.targetBlocking());
    }

    private boolean tryAttackTalisman(Context context, ActionKind action) {
        long now = mob.level().getGameTime();
        if (!context.lineOfSight()
                || !NpcProfile.isTalismanCultivator(mob)&&battle.mode()==NpcBattlePlan.Mode.NORMAL
                || readyTick("AttackTalisman") > now) {
            return false;
        }
        mob.lookAt(context.target(), 45.0F, 45.0F);
        if (!NpcProfile.tryUseAttackTalisman(mob, context.target(),battle.mode()!=NpcBattlePlan.Mode.NORMAL)) {
            return false;
        }
        setReadyTick("AttackTalisman",
                now + 30L + mob.getRandom().nextInt(21));
        beginCastPose(10);
        finishCastAction(action, "attack_talisman");
        return true;
    }

    private boolean finishAttempt(Context context) {
        if (context.distance() <= weaponRange(context.weaponStyle()) && context.lineOfSight()) {
            return weaponAttack(context, true);
        }
        if (tryExternalQi(context, false)) {
            return true;
        }
        String spell = selectSpell(context, id -> {
            var profile = NpcSpellcasting.profile(id);
            return profile != null && profile.role() == NpcSpellcasting.Role.ATTACK && profile.fast();
        }, true);
        return !spell.isBlank() && tryCast(context.target(), spell, ActionKind.SPELL_ATTACK);
    }

    private record ScoredSpell(String id,double utility) {}
    private String selectSpell(Context context, Predicate<String> filter, boolean preferFast) {
        long now = mob.level().getGameTime();
        List<String> learned = NpcProfile.learnedSpellIds(mob, archetype());
        return learned.stream()
                .map(ModSpells::normalizeId)
                .filter(filter)
                .filter(id -> {
                    NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(id);
                    return profile == null || !profile.area() || areaAlliesAtRisk(context, id) == 0;
                })
                .filter(id -> readyTick("Spell." + id) <= now)
                .filter(id -> NpcSpellcasting.canCast(mob, context.target(), id))
                .filter(id -> !battle.probing()||battle.mode()!=NpcBattlePlan.Mode.NORMAL
                        ||context.targetDangerousAction()||NpcCombatKnowledge.wasRevealed(mob,id)
                        ||NpcSpellcasting.qiCost(mob,id)<=NpcProfile.maxQi(mob,archetype())*.10)
                .filter(id -> battle.canSpend(NpcSpellcasting.qiCost(mob,id),NpcProfile.qi(mob,archetype()),
                        NpcProfile.maxQi(mob,archetype()),context.targetDangerousAction()))
                .map(id->new ScoredSpell(id,spellScore(context,id,preferFast)+mob.getRandom().nextDouble()*8))
                .filter(scored->scored.utility()>0)
                .max(Comparator.comparingDouble(ScoredSpell::utility))
                .map(ScoredSpell::id)
                .orElse("");
    }

    private double spellScore(Context context, String spellId, boolean preferFast) {
        NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(spellId);
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (profile == null || spell == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double score = 90.0D;
        score += tacticalSpellScore(context.target(), spellId, context.distance(),
                context.targetCasting() || context.target().swinging);
        if (profile.fast() && preferFast) score += 35.0D;
        if (profile.area()) {
            score += areaAlliesAtRisk(context, spellId) > 0
                    ? -200.0D : areaTargetCount(context, spellId) >= 2 ? 35.0D : -45.0D;
        }
        if (profile.role() == NpcSpellcasting.Role.CONTROL && context.targetBlocking()) score += 30.0D;
        if (profile.role() == NpcSpellcasting.Role.CONTROL && context.targetCasting()) score += 45.0D;
        if (spellId.equals(lastSpell)) score -= 65.0D;
        score-=recentSpells.stream().filter(spellId::equals).count()*14*battle.experience();
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        double costRatio = NpcSpellcasting.qiCost(mob, spellId) / (double) maxQi;
        score += battle.frenzy()?costRatio*180:-costRatio*(context.targetRealm()>context.selfRealm()?95:55);
        score -= battle.revealPenalty(costRatio,NpcCombatKnowledge.wasRevealed(mob,spellId),context.targetDangerousAction());
        score -= NpcCombatKnowledge.failurePenalty(mob,context.target(),spellId,spell.element())*(.4+battle.experience());
        if (knowledge!=null&&knowledge.opening())score+=profile.role()==NpcSpellcasting.Role.CONTROL?50:30;
        if (knowledge!=null&&Math.abs(knowledge.dodgeSide())>=3&&profile.area())score+=35;
        if (context.selfQiRatio() < 0.35F && costRatio > 0.05D) score -= 60.0D;
        if (context.targetHealthRatio() < 0.20F && profile.fast()) score += 20.0D;
        return score;
    }

    /** Only visible defenses and the existing spell's tactical range/preparation are considered. */
    static double tacticalSpellScore(LivingEntity target, String spellId, double distance, boolean threatened) {
        var profile = NpcSpellcasting.profile(spellId);
        var spell = ModSpells.get(spellId);
        if (profile == null || spell == null) return Double.NEGATIVE_INFINITY;
        double score = -Math.min(60, Math.max(0, distance - profile.maxRange()) * 4);
        if (threatened && distance < 5) score -= NpcSpellcasting.windupTicks(spellId) * 4;
        if (("火".equals(spell.element()) || "fire".equals(spell.element()))
                && (target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE))) score -= 120;
        return score;
    }

    private boolean tryCast(LivingEntity target, String spellId, ActionKind action) {
        return tryCast(target, spellId, action, MovementFollowup.NONE);
    }

    private boolean tryCast(
            LivingEntity target,
            String spellId,
            ActionKind action,
            MovementFollowup movementFollowup) {
        String normalized = ModSpells.normalizeId(spellId);
        long now = mob.level().getGameTime();
        if (pendingCast != null || pendingPill != null || readyTick("Spell." + normalized) > now
                || !NpcSpellcasting.canCast(mob, target, normalized)) {
            return false;
        }
        int cost = NpcSpellcasting.qiCost(mob, normalized);
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        boolean emergency=action==ActionKind.SURVIVAL||action==ActionKind.INTERRUPT||action==ActionKind.DEFENSE;
        if (!battle.canSpend(cost,NpcProfile.qi(mob,archetype()),maxQi,emergency))return false;
        if (!battle.frenzy()&&action != ActionKind.SURVIVAL
                && qiRatio() < LOW_QI_RATIO && cost > maxQi * 0.03D
                && !ModSpells.REGENERATION.equals(normalized)
                && !HealingTechniques.isTechnique(normalized)
                && !ModSpells.CLEANSE.equals(normalized)) {
            return false;
        }
        int windup = NpcSpellcasting.windupTicks(normalized)
                + mob.getRandom().nextInt(3);
        if (mob.hasEffect(ModEffects.SPIRIT_SEAL)) {
            windup = (int) Math.ceil(windup / 0.75D);
        }
        long releaseTick = now + windup;
        pendingCast = new PendingCast(
                normalized,
                target == null ? null : target.getUUID(),
                action,
                releaseTick,
                movementFollowup);
        mob.getNavigation().stop();
        clearFlightIntent();
        if (target != null) {
            mob.lookAt(target, 55.0F, 55.0F);
        }
        // Entity tick decrements synced cast ticks later in this same server tick.
        beginCastPose(windup + 1);
        remember(action, normalized);
        actionLockUntil = Math.max(actionLockUntil, releaseTick + 5L);
        return true;
    }

    private boolean tryMovementSpell(Context context, boolean escaping, ActionKind action) {
        MovementFollowup followup = escaping ? MovementFollowup.ESCAPE : MovementFollowup.APPROACH;
        List<String> learned = new ArrayList<>(NpcProfile.learnedSpellIds(mob, archetype()));
        learned.sort((left, right) -> {
            if (context.targetFlying()) {
                if (ModSpells.YUFENG_JUE.equals(left)) return -1;
                if (ModSpells.YUFENG_JUE.equals(right)) return 1;
            }
            return Integer.compare(NpcSpellcasting.qiCost(mob, left), NpcSpellcasting.qiCost(mob, right));
        });
        for (String spellId : learned) {
            var profile = NpcSpellcasting.profile(spellId);
            if (profile == null || profile.role() != NpcSpellcasting.Role.MOVEMENT) {
                continue;
            }
            if (resolveMovementDestination(followup, context.target(), spellId) != null
                    && tryCast(context.target(), spellId, action, followup)) {
                return true;
            }
        }
        return false;
    }

    private boolean weaponAttack(Context context, boolean forceHeavy) {
        long now=mob.level().getGameTime();
        if (!context.lineOfSight() || context.distance() > weaponRange(context.weaponStyle())
                || rhythm.recovering(now)) {
            return false;
        }
        mob.lookAt(context.target(), 55.0F, 55.0F);
        boolean heavy = rhythm.finisher(context.target().getUUID(),now)
                || battle.frenzy()||!battle.probing()&&forceHeavy
                || context.weaponStyle() == WeaponStyle.SABER && comboStep % 3 != 0
                || context.weaponStyle() == WeaponStyle.STAFF && context.nearbyEnemies() > 1
                || comboStep % 4 == 3;
        ItemStack weapon = mob.getMainHandItem();
        mob.removeEffect(ModEffects.NPC_WEAPON_GUARD);
        if (weapon.getItem() instanceof TieguShanItem) {
            boolean shouldOpen = !heavy;
            if (TieguShanItem.isClosed(weapon) == shouldOpen) {
                TieguShanItem.setOpen(weapon, shouldOpen);
                mob.swing(InteractionHand.MAIN_HAND, true);
                remember(ActionKind.WEAPON_FORM, shouldOpen ? "fan_open" : "fan_closed");
                actionLockUntil = Math.max(actionLockUntil, mob.level().getGameTime() + 3L);
                return true;
            }
        }
        if (heavy && context.weaponStyle() == WeaponStyle.STAFF
                && weapon.getItem() instanceof FuchenItem
                && readyTick("FuchenSpin") <= mob.level().getGameTime()
                && FuchenItem.startHeavyAttack(mob)) {
            mob.getNavigation().stop();
            clearFlightIntent();
            mob.swing(InteractionHand.MAIN_HAND, true);
            comboStep = (comboStep + 1) % 12;
            setReadyTick("FuchenSpin", mob.level().getGameTime() + 70L + mob.getRandom().nextInt(31));
            actionLockUntil = Math.max(actionLockUntil,
                    mob.level().getGameTime() + FuchenItem.heavyAttackDurationTicks());
            remember(ActionKind.WEAPON_HEAVY, "fuchen_spin");
            return true;
        }
        boolean hit = HeavyIronBracerItem.isRealStack(weapon)
                ? HeavyIronBracerItem.punchNpc(mob, context.target(), heavy)
                : heavy
                ? CombatStateManager.performNpcHeavyAttack(mob, context.target())
                : mob.doHurtTarget(context.target());
        rhythm.strike(context.target().getUUID(),now,hit);
        if (!HeavyIronBracerItem.isRealStack(weapon)) {
            mob.swing(InteractionHand.MAIN_HAND, true);
        }
        comboStep = (comboStep + 1) % 12;
        if (hit && context.weaponStyle() == WeaponStyle.STAFF) {
            Vec3 away = context.target().position().subtract(mob.position());
            context.target().knockback(0.8D, -away.x, -away.z);
        }
        remember(heavy ? ActionKind.WEAPON_HEAVY : ActionKind.WEAPON_LIGHT, "");
        return true;
    }

    private boolean tryExternalQi(Context context, boolean interrupt) {
        long now = mob.level().getGameTime();
        if (now < readyTick("ExternalQi") || context.weaponStyle() == WeaponStyle.UNARMED
                || mob.getMainHandItem().isEmpty() || !context.lineOfSight()) {
            return false;
        }
        ExternalQiTier tier = externalQiTier();
        if (tier == null || !battle.frenzy()&&context.selfQiRatio() < LOW_QI_RATIO) {
            return false;
        }
        if (!interrupt&&!battle.frenzy()&&!battle.pressure()
                &&NpcCombatKnowledge.failurePenalty(mob,context.target(),"external_qi","")>40)return false;
        ItemStack weapon = mob.getMainHandItem();
        double weaponRange = weaponRange(context.weaponStyle());
        double totalRange = weaponRange + tier.additionalRange();
        boolean purposeful = interrupt
                || context.targetRetreating()
                || context.targetFlying()
                || context.targetHealthRatio() <= 0.15F
                || context.distance() >= 2 && context.selfQiRatio() >= .35F
                && !rhythm.follows(context.target().getUUID(),now)
                || context.distance() > weaponRange - 0.25D && context.distance() <= totalRange;
        if (!purposeful || context.distance() > totalRange + 0.75D) {
            return false;
        }
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        int cost = Math.max(1, (int) Math.ceil(maxQi * tier.costRatio()));
        if (!battle.canSpend(cost,NpcProfile.qi(mob, archetype()),maxQi,interrupt)) {
            return false;
        }
        // Changing fan form is its own action so attributes refresh before release, but
        // only defer a slash that is currently purposeful, in range, and affordable.
        if (weapon.getItem() instanceof TieguShanItem && TieguShanItem.isClosed(weapon)) {
            TieguShanItem.setOpen(weapon, true);
            mob.swing(InteractionHand.MAIN_HAND, true);
            remember(ActionKind.WEAPON_FORM, "fan_open");
            actionLockUntil = Math.max(actionLockUntil, now + 3L);
            return true;
        }
        if (!NpcProfile.spendQi(mob, archetype(), cost)) {
            return false;
        }
        float damage = (float) Math.max(
                1.0D,
                mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        double radius = switch (context.weaponStyle()) {
            case SPEAR -> 0.60D;
            case SABER, STAFF -> 1.25D;
            case FAN -> 1.45D;
            case FIST -> 1.80D;
            default -> 0.95D;
        };
        String element = NpcProfile.spiritRootIds(mob, archetype()).stream().findFirst().orElse("");
        boolean heavyCone = context.weaponStyle() == WeaponStyle.SABER
                || context.weaponStyle() == WeaponStyle.SPEAR
                || context.weaponStyle() == WeaponStyle.FAN
                || context.weaponStyle() == WeaponStyle.STAFF
                || context.weaponStyle() == WeaponStyle.FIST
                || context.targetBlocking();
        int slashStage = comboStep;
        mob.getPersistentData().putString("NpcReleasingSpell","external_qi");
        boolean released;
        try { released = WeaponQiSlashEntity.releaseNpc(
                mob,
                context.target(),
                totalRange,
                damage,
                radius,
                element,
                slashStage,
                heavyCone); }
        finally {mob.getPersistentData().remove("NpcReleasingSpell");}
        if (!released) {
            NpcProfile.restoreQi(mob, archetype(), cost);
            return false;
        }
        comboStep = (comboStep + 1) % 12;
        setReadyTick("ExternalQi", now + 16L + mob.getRandom().nextInt(9));
        NpcCombatKnowledge.attempted(mob,context.target(),"external_qi");
        mob.swing(InteractionHand.MAIN_HAND, true);
        remember(ActionKind.EXTERNAL_QI, "");
        return true;
    }

    private boolean moveToSafety(
            Context context,
            boolean retreat,
            boolean seekCover,
            ActionKind action) {
        return moveToSafety(context, retreat, seekCover, action, false);
    }

    private boolean moveToSafety(
            Context context,
            boolean retreat,
            boolean seekCover,
            ActionKind action,
            boolean beginCoverHold) {
        Vec3 destination = context.escapeDestination();
        if (!retreat || destination == null || seekCover && !hasCover(context.target(), destination)) {
            destination = findSafeDestination(context.target(), retreat, seekCover);
        }
        if (destination == null || beginCoverHold && !hasCover(context.target(), destination)) {
            return false;
        }
        double speed = (NpcSpellcasting.hasMovementEffect(mob) ? 1.4D : 1.05D)
                * NpcProfile.chaseSpeedMultiplier(mob);
        boolean issued = issueMovement(destination, speed, action);
        if (issued&&battle.mode()==NpcBattlePlan.Mode.SURVIVAL&&escapeProgressOrigin==null) {
            escapeProgressOrigin=mob.position();escapeProgressTick=mob.level().getGameTime();escapeThreatDistance=context.distance();
        }
        if (issued && beginCoverHold) {
            coverTarget = context.target().getUUID();
            coverHoldUntil = mob.level().getGameTime() + 32L + mob.getRandom().nextInt(35);
        }
        return issued;
    }

    private boolean continueCoverPlan(Context context) {
        long now = mob.level().getGameTime();
        if (coverTarget == null) {
            return false;
        }
        if (!coverTarget.equals(context.target().getUUID())
                || now >= coverHoldUntil
                || context.targetRealm() <= context.selfRealm()
                || !context.targetRanged()) {
            clearCoverPlan();
            return false;
        }
        if (context.lineOfSight()) {
            // Let the already-issued retreat finish. If it cannot, choose a fresh cover
            // route below instead of oscillating into an approach spell.
            if (mob.getNavigation().isInProgress() || flightDestination != null) {
                actionLockUntil = Math.max(actionLockUntil, now + 4L);
                return true;
            }
            clearCoverPlan();
            return false;
        }
        if (context.distance() <= 5.0D || mob.hurtTime > 0) {
            clearCoverPlan();
            return moveToSafety(context, true, true, ActionKind.MOVEMENT, true);
        }
        if (context.selfHealthRatio() < COVER_HEAL_RATIO
                && tryBestHealing(ActionKind.DEFENSE)) {
            return true;
        }
        if (context.selfQiRatio() < COVER_QI_RATIO
                && hasStableRecoveryFooting()
                && !hasImmediateRecoveryThreat()
                && tryCast(null, ModSpells.QI_GATHERING, ActionKind.RECOVER_QI)) {
            return true;
        }
        if (shieldUseful(context) && DielangShield.remaining(mob) < 12.0F
                && tryCast(context.target(), ModSpells.DIELANG_SHIELD, ActionKind.DEFENSE)) {
            return true;
        }
        mob.getNavigation().stop();
        clearFlightIntent();
        holdForAction();
        mob.lookAt(context.target(), 35.0F, 35.0F);
        remember(ActionKind.COVER_HOLD, "");
        actionLockUntil = Math.max(actionLockUntil, now + 6L + mob.getRandom().nextInt(7));
        return true;
    }

    private void clearCoverPlan() {
        coverTarget = null;
        coverHoldUntil = 0L;
    }

    private boolean approachPredicted(Context context) {
        Vec3 predicted = resolveMovementDestination(
                MovementFollowup.APPROACH,
                context.target(),
                NpcSpellcasting.canFly(mob) ? ModSpells.YUFENG_JUE : "");
        if (predicted == null) {
            return false;
        }
        double speed = (NpcSpellcasting.hasMovementEffect(mob) ? 1.6D : 1.3D)
                * NpcProfile.chaseSpeedMultiplier(mob);
        return issueMovement(predicted, speed, ActionKind.MOVEMENT);
    }

    private Vec3 resolveMovementDestination(
            MovementFollowup followup,
            LivingEntity target,
            String movementSpellId) {
        if (followup == MovementFollowup.NONE) {
            return null;
        }
        if (target == null || !validTarget(target)) {
            return null;
        }
        Vec3 destination;
        if (followup == MovementFollowup.ESCAPE) {
            destination = findSafeDestination(target, true, false);
        } else {
            double distance = Math.sqrt(mob.distanceToSqr(target));
            double lead = Math.min(1.5D, distance * 0.04D);
            Vec3 predicted = target.position().add(target.getDeltaMovement().scale(lead));
            Vec3 towardMob = mob.position().subtract(predicted).multiply(1.0D, 0.0D, 1.0D);
            if (towardMob.lengthSqr() < 0.01D) {
                towardMob = mob.getLookAngle().reverse().multiply(1.0D, 0.0D, 1.0D);
            }
            double stoppingRange = Math.max(1.35D, weaponRange(weaponStyle(mob.getMainHandItem())) - 0.45D);
            destination = predicted.add(towardMob.normalize().scale(stoppingRange));
        }
        if (destination == null) {
            return null;
        }
        boolean willFly = NpcSpellcasting.canFly(mob)
                || ModSpells.YUFENG_JUE.equals(ModSpells.normalizeId(movementSpellId));
        if (willFly && canFlyTo(destination)) return destination;
        return isSafe(destination) && canWalkTo(destination) ? destination : null;
    }

    private boolean issueMovement(Vec3 destination, double speed, ActionKind action) {
        if (destination == null || !isSafe(destination) || !canTravelTo(destination)) {
            return false;
        }
        boolean issued;
        if (NpcSpellcasting.canFly(mob) && canFlyTo(destination)) {
            mob.getNavigation().stop();
            flightDestination = destination;
            flightRequiresGround = action == ActionKind.RECOVER_QI;
            double distance = Math.sqrt(mob.position().distanceToSqr(destination));
            long travelTicks = (long) Math.ceil(distance / Math.max(0.1D, speed));
            flightIntentUntil = mob.level().getGameTime() + Math.max(30L, Math.min(120L, travelTicks * 3L));
            issued = true;
        } else {
            clearFlightIntent();
            if (NpcSpellcasting.hasCreativeFlight(mob)) mob.setNoGravity(false);
            issued = mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
        if (issued && action != null) {
            remember(action, "");
        }
        return issued;
    }

    public void tickFlightMotion(double speed) {
        if (mob.level().isClientSide || !mob.isAlive()) return;
        if (!NpcSpellcasting.canFly(mob)) {
            clearFlightIntent();
            mob.setNoGravity(false);
            mob.removeEffect(ModEffects.YUFENG_JUE);
            idleLanding = null;
            return;
        }
        boolean fighting = mob.getTarget() != null && mob.getTarget().isAlive();
        // After no-gravity flight touches down, vanilla can clear onGround for one tick
        // while vertical velocity is zero. Contact must still end flight and its upkeep.
        AABB bounds = mob.getBoundingBox();
        AABB belowFeet = new AABB(bounds.minX + .001, bounds.minY - .04, bounds.minZ + .001,
                bounds.maxX - .001, bounds.minY, bounds.maxZ - .001);
        boolean touchingGround = mob.onGround() || mob.getDeltaMovement().y <= 0
                && !mob.level().noBlockCollision(mob, belowFeet);
        if (mob.isPassenger() || mob.isInWaterOrBubble() || mob.isInLava()
                || touchingGround && (flightDestination == null || !fighting)) {
            clearFlightIntent();
            mob.setNoGravity(false);
            idleLanding = null;
            return;
        }
        if (!NpcSpellcasting.hasCreativeFlight(mob) && !YufengJue.hasFlightSupport(mob)) {
            mob.removeEffect(ModEffects.YUFENG_JUE);
            clearFlightIntent();
            mob.setNoGravity(false);
            return;
        }
        if (!NpcFlight.payUpkeep(mob)) {
            clearFlightIntent();
            mob.setNoGravity(false);
            mob.removeEffect(ModEffects.YUFENG_JUE);
            return;
        }
        mob.setNoGravity(true);
        mob.resetFallDistance();
        long now = mob.level().getGameTime();
        if (!fighting && !mob.onGround()) {
            clearFlightIntent();
            if (tickIdleLanding(now)) return;
        } else {
            idleLanding = null;
            nextLandingCheck = 0;
        }
        if (mob.isNoAi() || NpcProfile.isInteracting(mob)
                || mob.hasEffect(ModEffects.QI_GATHERING)
                || pendingCast != null) {
            holdForAction();
            return;
        }
        boolean reachedDestination = flightRequiresGround
                ? hasStableRecoveryFooting()
                : flightDestination != null
                && mob.position().distanceToSqr(flightDestination) <= .3D * .3D
                && mob.getDeltaMovement().lengthSqr() <= .09D * .09D;
        if (flightRequiresGround && now >= flightIntentUntil && !reachedDestination) {
            mob.removeEffect(ModEffects.YUFENG_JUE);
            mob.setNoGravity(false);
            clearFlightIntent();
            return;
        }
        if (flightDestination == null || now >= flightIntentUntil || reachedDestination) {
            clearFlightIntent();
            if (NpcSpellcasting.hasCreativeFlight(mob)) NpcSpellcasting.tickCreativeFlight(mob, null);
            else NpcSpellcasting.tickYufengFlight(mob, null, speed);
            return;
        }
        if (NpcSpellcasting.hasCreativeFlight(mob)) NpcSpellcasting.tickCreativeFlight(mob, flightDestination);
        else NpcSpellcasting.tickYufengFlight(mob, flightDestination, speed);
    }

    private boolean tickIdleLanding(long now) {
        // Scan only the loaded column underneath; roofs and partial blocks count as ground.
        if (now >= nextLandingCheck || idleLanding != null
                && mob.position().subtract(idleLanding).horizontalDistanceSqr() > .25) {
            nextLandingCheck = now + 10;
            idleLanding = null;
            if (mob.level().hasChunkAt(mob.blockPosition())) {
                var hit = mob.level().clip(new ClipContext(mob.position().add(0, .01, 0),
                        new Vec3(mob.getX(), mob.level().getMinBuildHeight(), mob.getZ()),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    var state = mob.level().getBlockState(hit.getBlockPos());
                    Vec3 landing = hit.getLocation();
                    if (!isDangerous(state) && state.getFluidState().isEmpty()
                            && mob.level().noCollision(mob, mob.getBoundingBox().move(landing.subtract(mob.position())))
                            && !CenterEffectSupport.isDangerousPosition(mob, landing)
                            && !CenterEffectSupport.isForcefieldRouteBlocked(mob, mob.position(), landing)) {
                        idleLanding = landing;
                    }
                }
            }
        }
        if (idleLanding == null) return false;
        mob.getNavigation().stop();
        Vec3 movement = mob.getDeltaMovement();
        // Gentle 2.4-block/second descent; a small downward probe lets vanilla collision
        // mark the landing instead of hovering a fraction of a block above the surface.
        double descent = -Math.min(.12, Math.max(.035, (mob.getY() - idleLanding.y) * .25));
        Vec3 horizontal = NpcFlight.approachVelocity(movement.multiply(1, 0, 1), Vec3.ZERO, .18);
        double nextY = movement.y + Mth.clamp(descent - movement.y, -.025, .025);
        mob.setDeltaMovement(horizontal.x, nextY, horizontal.z);
        return true;
    }

    public void clearFlightIntent() {
        flightDestination = null;
        flightIntentUntil = 0L;
        flightRequiresGround = false;
    }

    private void holdForAction() {
        Vec3 movement = mob.getDeltaMovement();
        if (NpcSpellcasting.canFly(mob)) {
            NpcFlight.brake(mob);
        } else {
            mob.setDeltaMovement(movement.x * 0.2D, movement.y, movement.z * 0.2D);
            mob.hurtMarked = true;
        }
    }

    private boolean reposition(Context context, ActionKind action) {
        long now = mob.level().getGameTime();
        if (now >= strafeChangeTick) {
            if (mob.getRandom().nextBoolean()) {
                strafeSign *= -1;
            }
            strafeChangeTick = now + 24L + mob.getRandom().nextInt(38);
        }
        Vec3 radial = mob.position().subtract(context.target().position()).multiply(1.0D, 0.0D, 1.0D);
        if (radial.lengthSqr() < 0.01D) {
            radial = new Vec3(1.0D, 0.0D, 0.0D);
        }
        radial = radial.normalize();
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).scale(strafeSign);
        double radialCorrection = context.distance() < context.preferredDistance()
                ? 2.5D : context.distance() > context.preferredDistance() + 2.0D ? -1.5D : 0.0D;
        Vec3 destination = mob.position().add(tangent.scale(3.5D)).add(radial.scale(radialCorrection));
        if (!isSafe(destination) || !canTravelTo(destination)) {
            return moveToSafety(context, context.distance() < context.preferredDistance(), false, action);
        }
        return issueMovement(destination, 1.0D, action);
    }

    private Vec3 findSafeDestination(LivingEntity target, boolean retreat, boolean seekCover) {
        Vec3 away = mob.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(mob.getRandom().nextDouble() - 0.5D, 0.0D,
                    mob.getRandom().nextDouble() - 0.5D);
        }
        away = away.normalize();
        List<LivingEntity> threats = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(LOCAL_COMBAT_RADIUS + 4.0D),
                living -> living != mob && living.isAlive()
                        && !NpcProfile.areCombatAllies(mob, living)
                        && (living == target || isHostileCandidate(living)));
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < 8; index++) {
            double angle = (index - 3.5D) * Math.PI / 7.0D;
            Vec3 direction = rotateHorizontal(away, angle);
            if (!retreat) {
                direction = rotateHorizontal(away, Math.PI * 0.5D + angle * 0.35D);
            }
            double distance = 6.0D + index % 3 * 2.0D;
            Vec3 candidate = mob.position().add(direction.scale(distance));
            if (!isSafe(candidate) || !canTravelTo(candidate)) {
                continue;
            }
            double targetDistance = candidate.distanceTo(target.position());
            double score = (retreat ? targetDistance : -Math.abs(targetDistance - 7.0D))
                    +NpcBattlefield.positionUtility(mob,candidate);
            if (retreat && !threats.isEmpty()) {
                double nearestThreat = threats.stream()
                        .mapToDouble(threat -> candidate.distanceTo(threat.position()))
                        .min()
                        .orElse(targetDistance);
                long crowdingThreats = threats.stream()
                        .filter(threat -> candidate.distanceToSqr(threat.position()) < 5.0D * 5.0D)
                        .count();
                score += nearestThreat * 1.35D - crowdingThreats * 30.0D;
            }
            boolean cover = hasCover(target, candidate);
            if (cover) score += seekCover ? 18.0D : 4.0D;
            if (seekCover && !cover) score -= 12.0D;
            score -= Math.abs(candidate.y - mob.getY()) * 2.0D;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private boolean canTravelTo(Vec3 destination) {
        return NpcSpellcasting.canFly(mob) && canFlyTo(destination)
                || canWalkTo(destination);
    }

    private boolean canWalkTo(Vec3 destination) {
        var path = mob.getNavigation().createPath(BlockPos.containing(destination), 0);
        if (path == null || !path.canReach()) {
            return false;
        }
        Vec3 previous = mob.position();
        boolean leavingExistingDanger = CenterEffectSupport.isDangerousPosition(mob, previous);
        for (int index = 0; index < path.getNodeCount(); index++) {
            Vec3 node = path.getEntityPosAtNode(mob, index);
            if (CenterEffectSupport.isForcefieldRouteBlocked(mob, previous, node)) {
                return false;
            }
            boolean nodeDangerous = CenterEffectSupport.isDangerousPosition(mob, node);
            if (!leavingExistingDanger && nodeDangerous) {
                return false;
            }
            if (leavingExistingDanger && !nodeDangerous) {
                leavingExistingDanger = false;
            }
            previous = node;
        }
        return !CenterEffectSupport.isForcefieldRouteBlocked(mob, previous, destination)
                && !CenterEffectSupport.isDangerousPosition(mob, destination);
    }

    private boolean canFlyTo(Vec3 destination) {
        Vec3 delta = destination.subtract(mob.position());
        int samples = Math.max(1, (int) Math.ceil(delta.length() / 1.5D));
        Vec3 previous = mob.position();
        boolean leavingExistingDanger = CenterEffectSupport.isDangerousPosition(mob, previous);
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 point = mob.position().add(delta.scale(sample / (double) samples));
            if (CenterEffectSupport.isForcefieldRouteBlocked(mob, previous, point)
                    || !isClearTravelSpace(point)
                    || !NpcSpellcasting.hasCreativeFlight(mob) && !hasSafeSupportBelow(point, 20)) {
                return false;
            }
            boolean pointDangerous = CenterEffectSupport.isDangerousPosition(mob, point);
            if (!leavingExistingDanger && pointDangerous) {
                return false;
            }
            if (leavingExistingDanger && !pointDangerous) {
                leavingExistingDanger = false;
            }
            previous = point;
        }
        return true;
    }

    private boolean isClearTravelSpace(Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        BlockState feetState = mob.level().getBlockState(feet);
        BlockState headState = mob.level().getBlockState(feet.above());
        return !isDangerous(feetState)
                && !feetState.getFluidState().is(FluidTags.LAVA)
                && feetState.getCollisionShape(mob.level(), feet).isEmpty()
                && headState.getCollisionShape(mob.level(), feet.above()).isEmpty();
    }

    private boolean hasSafeSupportBelow(Vec3 position, int maxDepth) {
        BlockPos feet = BlockPos.containing(position);
        for (int depth = 1; depth <= maxDepth; depth++) {
            BlockPos below = feet.below(depth);
            if (below.getY() < mob.level().getMinBuildHeight()) {
                return false;
            }
            BlockState state = mob.level().getBlockState(below);
            if (!state.getCollisionShape(mob.level(), below).isEmpty()) {
                return !isDangerous(state) && !state.getFluidState().is(FluidTags.LAVA);
            }
        }
        return false;
    }

    private boolean hasStableRecoveryFooting() {
        BlockPos feet = mob.blockPosition();
        BlockPos support = feet.below();
        BlockState state = mob.level().getBlockState(support);
        return mob.onGround()
                && !state.getCollisionShape(mob.level(), support).isEmpty()
                && !state.getFluidState().is(FluidTags.LAVA)
                && !isDangerous(state)
                && isSafe(mob.position());
    }

    private Vec3 findRecoveryLanding() {
        BlockPos origin = mob.blockPosition();
        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int radius = 0; radius <= 6; radius += 2) {
            for (int x = -radius; x <= radius; x += Math.max(1, radius)) {
                for (int z = -radius; z <= radius; z += Math.max(1, radius)) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    for (int depth = 1; depth <= 20; depth++) {
                        BlockPos support = origin.offset(x, -depth, z);
                        BlockState supportState = mob.level().getBlockState(support);
                        if (supportState.getCollisionShape(mob.level(), support).isEmpty()) {
                            continue;
                        }
                        BlockPos feet = support.above();
                        Vec3 candidate = Vec3.atBottomCenterOf(feet);
                        if (!isDangerous(supportState)
                                && !supportState.getFluidState().is(FluidTags.LAVA)
                                && isSafe(candidate)) {
                            double score = mob.position().distanceToSqr(candidate);
                            if (score < bestScore) {
                                bestScore = score;
                                best = candidate;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    private boolean isSafe(Vec3 position) {
        if (CenterEffectSupport.isDangerousPosition(mob, position)) {
            return false;
        }
        BlockPos feet = BlockPos.containing(position);
        BlockState feetState = mob.level().getBlockState(feet);
        if (isDangerous(feetState) || feetState.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        if (!feetState.getCollisionShape(mob.level(), feet).isEmpty()
                || !mob.level().getBlockState(feet.above()).getCollisionShape(mob.level(), feet.above()).isEmpty()) {
            return false;
        }
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = feet.below(depth);
            BlockState state = mob.level().getBlockState(below);
            if (state.getFluidState().is(FluidTags.LAVA) || isDangerous(state)) {
                return false;
            }
            if (!state.getCollisionShape(mob.level(), below).isEmpty()) {
                return depth <= 2;
            }
        }
        return NpcSpellcasting.hasCreativeFlight(mob)
                || NpcSpellcasting.canFly(mob) && hasSafeSupportBelow(position, 20);
    }

    private boolean isDangerous(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private boolean hasCover(LivingEntity target, Vec3 destination) {
        HitResult hit = mob.level().clip(new ClipContext(
                target.getEyePosition(),
                destination.add(0.0D, mob.getEyeHeight(), 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private boolean hasUsableRangedSpell(Context context) {
        return !selectSpell(context, id -> {
            var profile = NpcSpellcasting.profile(id);
            return profile != null && (profile.role() == NpcSpellcasting.Role.ATTACK
                    || profile.role() == NpcSpellcasting.Role.CONTROL);
        }, false).isBlank();
    }

    private boolean isThreatenedDuringRecovery(Context context) {
        return mob.hurtTime > 0 || hasImmediateRecoveryThreat();
    }

    private boolean isCasting(LivingEntity entity) {
        if (entity instanceof CultivationNpc npc && npc.npcCastingTicks() > 0) {
            return true;
        }
        return entity.isUsingItem()
                || entity instanceof ServerPlayer player
                && (CombatStateManager.isPreparingHeavyAttack(player)
                || CombatStateManager.isPreparingSpell(player));
    }

    private boolean isRecovering(LivingEntity entity) {
        return entity.hasEffect(ModEffects.QI_GATHERING)
                || entity.hasEffect(MobEffects.REGENERATION)
                || entity.hasEffect(ModEffects.MULING_SHENGXI)
                || entity.hasEffect(ModEffects.XUE_GUIYUAN_RECOVERY);
    }

    private boolean isBlocking(LivingEntity entity) {
        return entity instanceof ServerPlayer player && CombatStateManager.isBlocking(player)
                || entity.isBlocking() || entity.hasEffect(ModEffects.NPC_WEAPON_GUARD);
    }

    private boolean isFlying(LivingEntity entity) {
        return entity.hasEffect(ModEffects.YUFENG_JUE)
                || !entity.onGround() && entity.getY() > mob.getY() + 1.5D;
    }

    private boolean isRetreating(LivingEntity entity) {
        if (!entity.getUUID().equals(trackedRetreatTarget)) {
            trackedRetreatTarget = entity.getUUID();
            lastTargetPosition = null;
        }
        Vec3 away = entity.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 velocity = entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D || velocity.lengthSqr() < 0.0025D) {
            return false;
        }
        boolean retreating = away.normalize().dot(velocity.normalize()) > 0.45D;
        if (lastTargetPosition != null) {
            retreating |= entity.position().distanceToSqr(mob.position())
                    > lastTargetPosition.distanceToSqr(mob.position()) + 0.5D;
        }
        lastTargetPosition = entity.position();
        return retreating;
    }

    private boolean isControlled(LivingEntity entity) {
        return entity.hasEffect(ModEffects.DAZE)
                || entity.hasEffect(ModEffects.DINGSHEN)
                || entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                || entity.hasEffect(MobEffects.WEAKNESS);
    }

    private boolean isDistracted(LivingEntity entity) {
        return entity instanceof Mob other && other.getTarget() != null && other.getTarget() != mob;
    }

    private boolean canTargetReach(LivingEntity target) {
        double distance = Math.sqrt(mob.distanceToSqr(target));
        if (distance <= 3.25D && target.hasLineOfSight(mob)) {
            return true;
        }
        if (isRangedCombatant(target) && target.hasLineOfSight(mob) && distance <= 40.0D) {
            return true;
        }
        if (target instanceof PathfinderMob pathfinder) {
            var path = pathfinder.getNavigation().createPath(mob.blockPosition(), 0);
            return path != null && path.canReach();
        }
        return target.hasLineOfSight(mob) && distance <= 4.5D;
    }

    private boolean canTargetReachQuick(LivingEntity target) {
        double distance = Math.sqrt(mob.distanceToSqr(target));
        if (isRangedCombatant(target) && target.hasLineOfSight(mob) && distance <= 40.0D) {
            return true;
        }
        return target.hasLineOfSight(mob) && distance <= 5.0D
                || target instanceof Mob other && other.getTarget() == mob && distance <= 10.0D;
    }

    private boolean isRangedCombatant(LivingEntity target) {
        ItemStack weapon = target.getMainHandItem();
        String path = weapon.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath();
        return NpcCombatKnowledge.view(mob,target).ranged()
                || path.contains("bow") || path.contains("crossbow")
                || path.contains("trident") || path.contains("fan")
                || isCasting(target);
    }

    private boolean shouldCancelRecoveryImmediately() {
        if (!hasStableRecoveryFooting()) {
            return true;
        }
        if (mob.hurtTime > 0) {
            return true;
        }
        return hasImmediateRecoveryThreat();
    }

    private boolean hasImmediateRecoveryThreat() {
        return mob.level().getEntitiesOfClass(
                        LivingEntity.class,
                        mob.getBoundingBox().inflate(24.0D),
                        living -> living != mob && living.isAlive()
                                && validTarget(living)
                                && isHostileCandidate(living))
                .stream()
                .anyMatch(threat -> {
                    double distance = Math.sqrt(mob.distanceToSqr(threat));
                    if (distance < 15.0D) {
                        return mob.hasLineOfSight(threat)
                                && !isControlled(threat)
                                && !isDistracted(threat);
                    }
                    return mob.hasLineOfSight(threat)
                            && !isControlled(threat)
                            && !isDistracted(threat)
                            && (isCasting(threat) || canTargetReachQuick(threat));
                });
    }

    private int harmfulEffectCount(LivingEntity entity) {
        return (int) entity.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .count();
    }

    private void tickPendingCast(long now) {
        PendingCast cast = pendingCast;
        if (cast == null) return;
        var profile = NpcSpellcasting.profile(cast.spellId());
        if (profile != null && profile.role() == NpcSpellcasting.Role.HEAL && !healingOpening()) {
            cancelPendingCast(now);
            return;
        }
        LivingEntity target = resolvePendingTarget(cast.targetId());
        if (cast.targetId() != null && target == null
                || target != null && !validTarget(target)
                || mob.hasEffect(ModEffects.DAZE)
                || mob.hasEffect(ModEffects.DINGSHEN)
                || mob.hasEffect(ModEffects.FROZEN)
                || mob.hasEffect(ModEffects.FROZEN_QI)
                || mob.hasEffect(ModEffects.SPIRIT_SEAL)
                && mob instanceof CultivationNpc npc
                && npc.npcCastingTicks() <= 0
                || ModSpells.QI_GATHERING.equals(cast.spellId()) && shouldCancelRecoveryImmediately()
                || !NpcSpellcasting.canCast(mob, target, cast.spellId())) {
            cancelPendingCast(now);
            return;
        }
        mob.getNavigation().stop();
        holdForAction();
        if (target != null) {
            mob.lookAt(target, 55.0F, 55.0F);
        }
        if (now < cast.releaseTick()) {
            return;
        }

        if (target != null && !pendingCastStillUseful(cast, target)) {
            cancelPendingCast(now);
            return;
        }

        Vec3 movementDestination = resolveMovementDestination(
                cast.movementFollowup(), target, cast.spellId());
        if (cast.movementFollowup() != MovementFollowup.NONE && movementDestination == null) {
            cancelPendingCast(now);
            return;
        }

        pendingCast = null;
        endCastPose();
        MobEffectInstance previousSpeed = mob.getEffect(MobEffects.MOVEMENT_SPEED);
        MobEffectInstance speedSnapshot = previousSpeed == null
                ? null : new MobEffectInstance(previousSpeed);
        boolean noGravitySnapshot = mob.isNoGravity();
        mob.getPersistentData().putString("NpcReleasingSpell",cast.spellId());
        boolean castSucceeded;
        try { castSucceeded=NpcSpellcasting.cast(mob,target,cast.spellId()); }
        finally { mob.getPersistentData().remove("NpcReleasingSpell"); }
        if (!castSucceeded) {
            actionLockUntil = now + 2L;
            nextDecisionTick = now + 2L;
            return;
        }
        if (ModSpells.QI_GATHERING.equals(cast.spellId())) {
            recoveryAnchor = mob.position();
            mob.getNavigation().stop();
            clearFlightIntent();
            mob.setDeltaMovement(Vec3.ZERO);
            mob.hurtMarked = true;
        } else if (movementDestination != null) {
            double speed = 1.4D * NpcProfile.chaseSpeedMultiplier(mob);
            if (!issueMovement(movementDestination, speed, null)) {
                rollbackMovementCast(cast.spellId(), now, speedSnapshot, noGravitySnapshot);
                return;
            }
        }
        setReadyTick("Spell." + cast.spellId(),
                now + Math.max(1, ModSpells.cooldownTicks(cast.spellId())));
        NpcCombatKnowledge.reveal(mob,cast.spellId());
        recentSpells.addLast(cast.spellId());while(recentSpells.size()>ACTION_MEMORY)recentSpells.removeFirst();
        if (NpcSpellcasting.isAttackSpell(cast.spellId()))NpcCombatKnowledge.attempted(mob,target,cast.spellId());
        actionLockUntil = Math.max(actionLockUntil, now + 5L);
        nextDecisionTick = Math.max(nextDecisionTick, now + 2L);
    }

    private boolean pendingCastStillUseful(PendingCast cast, LivingEntity target) {
        if (ModSpells.DIELANG_SHIELD.equals(cast.spellId())) {
            Context current = observe(target);
            return DielangShield.remaining(mob) < 12.0F && shieldUseful(current);
        }
        if (cast.action() == ActionKind.INTERRUPT
                && !isCasting(target)
                && !isRecovering(target)
                && !(target instanceof ServerPlayer player
                && CombatStateManager.isPreparingHeavyAttack(player))) {
            return false;
        }
        NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(cast.spellId());
        if (profile == null || !profile.area()) {
            return true;
        }
        Context current = observe(target);
        return areaAlliesAtRisk(current, cast.spellId()) == 0
                && (cast.action() == ActionKind.INTERRUPT
                || areaTargetCount(current, cast.spellId()) >= 2
                || current.targetHealthRatio() >= 0.45F
                && current.targetRealm() >= current.selfRealm());
    }

    private int areaTargetCount(Context context, String spellId) {
        return ModSpells.LIUGUANG_JIANYING.equals(ModSpells.normalizeId(spellId))
                ? combatantsAround(mob.position(), 10.0D, false) : context.clusteredEnemies();
    }

    private int areaAlliesAtRisk(Context context, String spellId) {
        return ModSpells.LIUGUANG_JIANYING.equals(ModSpells.normalizeId(spellId))
                ? combatantsAround(mob.position(), 10.0D, true) : context.alliesNearTarget();
    }

    private int combatantsAround(Vec3 center, double radius, boolean allies) {
        AABB bounds = new AABB(center, center).inflate(radius);
        double radiusSqr = radius * radius;
        return mob.level().getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                living -> living != mob && living.isAlive()
                        && living.position().distanceToSqr(center) <= radiusSqr
                        && (allies
                        ? NpcProfile.areCombatAllies(mob, living)
                        : NpcProfile.isCombatEnemy(mob, living))).size();
    }

    private void rollbackMovementCast(
            String spellId,
            long now,
            MobEffectInstance speedSnapshot,
            boolean noGravitySnapshot) {
        if (ModSpells.WIND_STEP.equals(spellId)) {
            mob.removeEffect(ModEffects.WIND_STEP);
            mob.removeEffect(MobEffects.MOVEMENT_SPEED);
            if (speedSnapshot != null) {
                mob.addEffect(speedSnapshot);
            }
        } else if (ModSpells.YUFENG_JUE.equals(spellId)) {
            mob.removeEffect(ModEffects.YUFENG_JUE);
            clearFlightIntent();
            mob.setNoGravity(noGravitySnapshot);
        }
        NpcProfile.restoreQi(mob, archetype(), NpcSpellcasting.qiCost(mob, spellId));
        actionLockUntil = now + 2L;
        nextDecisionTick = now + 2L;
    }

    private LivingEntity resolvePendingTarget(UUID targetId) {
        if (targetId == null) {
            return null;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(targetId) instanceof LivingEntity target)
                || !target.isAlive()
                || target.level() != mob.level()) {
            return null;
        }
        return target;
    }

    private void cancelPendingCast(long now) {
        if (pendingCast == null) {
            return;
        }
        pendingCast = null;
        endCastPose();
        actionLockUntil = now;
        nextDecisionTick = now + 2L;
    }

    private void tickRecoveryAnchor() {
        if (!mob.hasEffect(ModEffects.QI_GATHERING)) {
            recoveryAnchor = null;
            return;
        }
        if (recoveryAnchor == null) {
            recoveryAnchor = mob.position();
        } else if (mob.distanceToSqr(recoveryAnchor) > 0.01D) {
            cancelQiRecovery(mob.level().getGameTime());
            return;
        }
        mob.getNavigation().stop();
        clearFlightIntent();
        Vec3 movement = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, NpcSpellcasting.canFly(mob) ? 0.0D : movement.y, 0.0D);
        mob.hurtMarked = true;
    }

    private void cancelQiRecovery(long now) {
        mob.removeEffect(ModEffects.QI_GATHERING);
        recoveryAnchor = null;
        mob.getNavigation().stop();
        clearFlightIntent();
        actionLockUntil = now;
        nextDecisionTick = now;
    }

    private void beginCastPose(int ticks) {
        if (mob instanceof CultivationNpc npc) {
            npc.beginNpcCast(ticks);
        }
    }

    private void endCastPose() {
        if (mob instanceof CultivationNpc npc) {
            npc.endNpcCast();
        }
    }

    private void finishCastAction(ActionKind action, String actionId) {
        NpcCombatKnowledge.reveal(mob,actionId);
        remember(action, actionId);
        actionLockUntil = Math.max(actionLockUntil, mob.level().getGameTime() + 18L);
    }

    private long readyTick(String key) {
        return mob.getPersistentData().getCompound(COMBAT_STATE_TAG).getLong(key);
    }

    private void setReadyTick(String key, long readyTick) {
        CompoundTag state = mob.getPersistentData().getCompound(COMBAT_STATE_TAG);
        state.putLong(key, readyTick);
        mob.getPersistentData().put(COMBAT_STATE_TAG, state);
    }

    private static boolean isMovementAction(ActionKind action) {
        return action == ActionKind.MOVEMENT
                || action == ActionKind.REPOSITION
                || action == ActionKind.FEINT;
    }

    private void remember(ActionKind action, String spellId) {
        long now = mob.level().getGameTime();
        if (lastRememberTick != now || lastRememberedAction != action) {
            recentActions.addLast(action);
            while (recentActions.size() > ACTION_MEMORY) {
                recentActions.removeFirst();
            }
            lastRememberedAction = action;
            lastRememberTick = now;
        }
        actionLockUntil = Math.max(actionLockUntil, now + actionDuration(action));
        if (!spellId.isBlank()) {
            lastSpell = spellId;
        }
    }

    private boolean lastActionWasWeaponAttack() {
        return lastRememberedAction == ActionKind.WEAPON_LIGHT
                || lastRememberedAction == ActionKind.WEAPON_HEAVY;
    }

    private static int actionDuration(ActionKind action) {
        return switch (action) {
            case WEAPON_LIGHT -> 10;
            case WEAPON_HEAVY -> 18;
            case WEAPON_FORM -> 3;
            case SPELL_ATTACK, INTERRUPT, CONTROL, DEFENSE, SURVIVAL, EXTERNAL_QI -> 12;
            case RECOVER_QI -> 8;
            case COVER_HOLD -> 6;
            case MOVEMENT, REPOSITION, FEINT -> 4;
        };
    }

    private boolean recent(ActionKind action) {
        int count = 0;
        for (ActionKind remembered : recentActions) {
            if (remembered == action) count++;
        }
        return count >= 2;
    }

    private float qiRatio() {
        return NpcProfile.qi(mob, archetype())
                / (float) Math.max(1, NpcProfile.maxQi(mob, archetype()));
    }

    private boolean qiFullyRecovered() {
        return NpcProfile.qi(mob, archetype()) >= NpcProfile.maxQi(mob, archetype());
    }

    private String archetype() {
        return mob instanceof CultivationNpc npc ? npc.npcArchetype() : NpcProfile.FANREN;
    }

    private ExternalQiTier externalQiTier() {
        String realm = CultivationLevels.getLevelDef(NpcProfile.realm(mob, archetype())).realm();
        return switch (realm) {
            case CultivationLevels.REALM_ZHUJI -> new ExternalQiTier(2.0D, 0.05D);
            case CultivationLevels.REALM_JINDAN -> new ExternalQiTier(4.0D, 0.04D);
            case CultivationLevels.REALM_YUANYING,
                 CultivationLevels.REALM_HUASHEN,
                 CultivationLevels.REALM_LIANXU,
                 CultivationLevels.REALM_HETI,
                 CultivationLevels.REALM_DACHENG,
                 CultivationLevels.REALM_DUJIE -> new ExternalQiTier(6.0D, 0.03D);
            default -> null;
        };
    }

    private static int realmIndex(LivingEntity entity) {
        return CultivationLevels.getRealmIndex(CenterEffectSupport.realmName(entity));
    }

    private static float healthRatio(LivingEntity entity) {
        return entity.getMaxHealth() <= 0.0F ? 0.0F : entity.getHealth() / entity.getMaxHealth();
    }

    private static WeaponStyle weaponStyle(ItemStack stack) {
        if (stack.isEmpty()) return WeaponStyle.UNARMED;
        if (HeavyIronBracerItem.isRealStack(stack)) return WeaponStyle.FIST;
        if (QiInfusedWeapon.isSpear(stack)) return WeaponStyle.SPEAR;
        if (stack.getItem() instanceof TieguShanItem) return WeaponStyle.FAN;
        if (stack.getItem() instanceof FuchenItem) return WeaponStyle.STAFF;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("dao") || path.contains("saber") || path.contains("blade")) return WeaponStyle.SABER;
        if (path.contains("staff") || path.contains("rod") || path.contains("fuchen")) return WeaponStyle.STAFF;
        if (QiInfusedWeapon.isSwordOrDao(stack) || path.contains("sword") || path.contains("jian")) {
            return WeaponStyle.SWORD;
        }
        return WeaponStyle.OTHER;
    }

    private static double preferredDistance(WeaponStyle style) {
        return switch (style) {
            case UNARMED -> 1.8D;
            case FIST -> 2.2D;
            case SWORD -> 2.7D;
            case SABER -> 2.4D;
            case SPEAR -> 4.2D;
            case FAN -> 9.0D;
            case STAFF -> 3.5D;
            case OTHER -> 3.0D;
        };
    }

    private static double weaponRange(WeaponStyle style) {
        return switch (style) {
            case UNARMED -> 2.5D;
            case FIST -> 2.8D;
            case SWORD, SABER -> 3.25D;
            case SPEAR -> 4.75D;
            case FAN -> 3.0D;
            case STAFF -> 3.9D;
            case OTHER -> 3.0D;
        };
    }

    private static Vec3 rotateHorizontal(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D,
                vector.x * sin + vector.z * cos);
    }

    private enum ActionKind {
        SURVIVAL,
        INTERRUPT,
        RECOVER_QI,
        MOVEMENT,
        DEFENSE,
        CONTROL,
        SPELL_ATTACK,
        WEAPON_LIGHT,
        WEAPON_HEAVY,
        WEAPON_FORM,
        EXTERNAL_QI,
        COVER_HOLD,
        REPOSITION,
        FEINT
    }

    private enum MovementFollowup {
        NONE,
        ESCAPE,
        APPROACH
    }

    private enum WeaponStyle {
        SWORD,
        SABER,
        SPEAR,
        FAN,
        STAFF,
        FIST,
        UNARMED,
        OTHER;

        private boolean isMelee() {
            return this != FAN;
        }
    }

    private record ExternalQiTier(double additionalRange, double costRatio) {
    }

    private record RecentDamage(float amount, long expiresAt) {
    }

    private record PendingPill(ItemStack stack, UUID targetId, long releaseTick) {}

    private record PendingCast(
            String spellId,
            UUID targetId,
            ActionKind action,
            long releaseTick,
            MovementFollowup movementFollowup) {
    }

    private record Context(
            LivingEntity target,
            double distance,
            boolean lineOfSight,
            float selfHealthRatio,
            float selfQiRatio,
            float targetHealthRatio,
            int selfRealm,
            int targetRealm,
            WeaponStyle weaponStyle,
            double preferredDistance,
            boolean targetBlocking,
            boolean targetCasting,
            boolean targetFlying,
            boolean targetRetreating,
            boolean targetRecovering,
            boolean targetDangerousAction,
            int nearbyAllies,
            int nearbyEnemies,
            int closeEnemies,
            int clusteredEnemies,
            int alliesNearTarget,
            int negativeEffects,
            Vec3 escapeDestination,
            boolean canTargetReach,
            boolean targetRanged) {
    }
}
