package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.FuchenItem;
import com.example.immortal_cultivation_mod.item.HeavyIronBracerItem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.TieguShanItem;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.DielangShield;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static final int MIN_DECISION_DELAY = 4;
    private static final int RANDOM_DECISION_DELAY = 4;
    private static final double TARGET_SEARCH_RANGE = 40.0D;
    private static final double LOCAL_COMBAT_RADIUS = 12.0D;
    private static final float SURVIVAL_HP_RATIO = 0.20F;
    private static final float LOW_QI_RATIO = 0.15F;
    private static final float COVER_HEAL_RATIO = 0.60F;
    private static final float COVER_QI_RATIO = 0.45F;
    private static final int ACTION_MEMORY = 6;
    private static final String COMBAT_STATE_TAG = "ImmortalCultivationNpcCombatState";

    private final PathfinderMob mob;
    private final Map<UUID, RecentDamage> recentDamageByAttacker = new HashMap<>();
    private final Deque<ActionKind> recentActions = new ArrayDeque<>();
    private PendingCast pendingCast;
    private Vec3 flightDestination;
    private long flightIntentUntil;
    private boolean flightRequiresGround;
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
        HeavyIronBracerItem.syncNpcOffhandCompanion(mob);
        if (mob.isNoAi() || NpcProfile.isInteracting(mob)) {
            cancelPendingCast(now);
            clearFlightIntent();
            return;
        }
        tickRecoveryAnchor();
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
            clearFlightIntent();
            clearCoverPlan();
            if (healthRatio(mob) < SURVIVAL_HP_RATIO) {
                tryCast(null, ModSpells.REGENERATION, ActionKind.SURVIVAL);
            }
            return;
        }
        if (mob.getTarget() != target) {
            mob.setTarget(target);
        }
        Context context = observe(target);

        if (mob.hasEffect(ModEffects.QI_GATHERING)) {
            if (isThreatenedDuringRecovery(context)) {
                cancelQiRecovery(now);
                moveToSafety(context, true, true, ActionKind.SURVIVAL);
            }
            return;
        }

        // 1. Immediate survival.
        if (context.selfHealthRatio() < SURVIVAL_HP_RATIO) {
            immediateSurvival(context);
            return;
        }

        // 2. Interrupt dangerous enemy actions.
        if (context.targetDangerousAction() && interrupt(context)) {
            return;
        }

        // 3. Recover qi only when genuinely safe.
        if (context.selfQiRatio() < LOW_QI_RATIO && lowQiBehavior(context)) {
            return;
        }

        // Harmful effects are worth cleansing once survival/interrupt pressure is handled.
        if (context.negativeEffects() > 0
                && tryCast(null, ModSpells.CLEANSE, ActionKind.DEFENSE)) {
            return;
        }

        // 4. Maintain weapon-appropriate distance.
        if (maintainDistance(context)) {
            return;
        }

        // 5. Control/defense.
        if (controlOrDefense(context)) {
            return;
        }

        // 6. Best useful attack.
        if (bestAttack(context)) {
            return;
        }

        // 7. Reposition while all useful attacks cool down.
        reposition(context, ActionKind.REPOSITION);
    }

    public void recordDamage(DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        LivingEntity sourceActor = source.getEntity() instanceof LivingEntity living ? living : null;
        if (sourceActor == mob || sourceActor != null && NpcProfile.areCombatAllies(mob, sourceActor)) {
            return;
        }
        long now = mob.level().getGameTime();
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
                || candidate instanceof BloodApeDemonGeneralEntity;
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
        RecentDamage recentDamage = recentDamageByAttacker.get(target.getUUID());
        score += (recentDamage == null ? 0.0F : recentDamage.amount()) * 5.0D;
        score += (1.0D - healthRatio(target)) * 70.0D;
        score += realmIndex(target) * 10.0D;
        if (isCasting(target) || isRecovering(target)) score += 85.0D;
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
        int targetRealm = realmIndex(target);
        double preferred = preferredDistance(weaponStyle);
        if (targetRealm > selfRealm) {
            preferred += 1.5D;
        } else if (targetRealm < selfRealm) {
            preferred = Math.max(1.4D, preferred - 1.0D);
        }
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(LOCAL_COMBAT_RADIUS),
                living -> living != mob && living.isAlive());
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
                healthRatio(target),
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
        if (NpcProfile.tryUseRoyalPendant(mob)) {
            remember(ActionKind.SURVIVAL, "");
            return true;
        }
        if (context.negativeEffects() > 0
                && tryCast(null, ModSpells.CLEANSE, ActionKind.SURVIVAL)) {
            return true;
        }
        if (tryCast(null, ModSpells.REGENERATION, ActionKind.SURVIVAL)) {
            return true;
        }
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
        // Fight only when escape is impossible, or a single quick hit can finish the target.
        if (context.escapeDestination() == null || context.targetHealthRatio() <= 0.10F) {
            return finishAttempt(context);
        }
        return false;
    }

    private boolean interrupt(Context context) {
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
        if (safeToRecover && NpcSpellcasting.hasFlightEffect(mob) && !hasStableRecoveryFooting()) {
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
        if (context.nearbyEnemies() >= 3 || context.targetBlocking()) {
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
        // Occasional feint, but never while the target is nearly dead or actively recovering.
        float feintChance = context.weaponStyle() == WeaponStyle.UNARMED ? 0.16F : 0.07F;
        if (context.targetHealthRatio() > 0.15F && !context.targetRecovering()
                && mob.getRandom().nextFloat() < feintChance
                && reposition(context, ActionKind.FEINT)) {
            return true;
        }
        if (NpcProfile.tryUseIdentityCombatAbility(mob)) {
            finishCastAction(ActionKind.SPELL_ATTACK, "identity_ability");
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
        if (weaponReady && (!recent(ActionKind.WEAPON_LIGHT) && !recent(ActionKind.WEAPON_HEAVY)
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
        if (bannerUseful && readyTick("SoulBanner") <= now
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
                .max(Comparator.comparingDouble(id -> spellScore(context, id, preferFast)))
                .orElse("");
    }

    private double spellScore(Context context, String spellId, boolean preferFast) {
        NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(spellId);
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (profile == null || spell == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double score = 50.0D;
        if (profile.fast() && preferFast) score += 35.0D;
        if (profile.area()) {
            score += areaAlliesAtRisk(context, spellId) > 0
                    ? -200.0D : areaTargetCount(context, spellId) >= 2 ? 35.0D : -45.0D;
        }
        if (profile.role() == NpcSpellcasting.Role.CONTROL && context.targetBlocking()) score += 30.0D;
        if (profile.role() == NpcSpellcasting.Role.CONTROL && context.targetCasting()) score += 45.0D;
        if (spellId.equals(lastSpell)) score -= 65.0D;
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        double costRatio = NpcSpellcasting.qiCost(mob, spellId) / (double) maxQi;
        score -= costRatio * (context.targetRealm() > context.selfRealm() ? 95.0D : 55.0D);
        if (context.selfQiRatio() < 0.35F && costRatio > 0.05D) score -= 60.0D;
        if (context.targetHealthRatio() < 0.20F && profile.fast()) score += 20.0D;
        return score + mob.getRandom().nextDouble() * 8.0D;
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
        if (pendingCast != null || readyTick("Spell." + normalized) > now
                || !NpcSpellcasting.canCast(mob, target, normalized)) {
            return false;
        }
        int cost = NpcSpellcasting.qiCost(mob, normalized);
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        if (action != ActionKind.SURVIVAL
                && qiRatio() < LOW_QI_RATIO && cost > maxQi * 0.03D
                && !ModSpells.REGENERATION.equals(normalized)
                && !ModSpells.CLEANSE.equals(normalized)) {
            return false;
        }
        int windup = NpcSpellcasting.windupTicks(normalized) + mob.getRandom().nextInt(3);
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
        if (!context.lineOfSight() || context.distance() > weaponRange(context.weaponStyle())) {
            return false;
        }
        mob.lookAt(context.target(), 55.0F, 55.0F);
        boolean heavy = forceHeavy
                || context.weaponStyle() == WeaponStyle.SABER && comboStep % 3 != 0
                || context.weaponStyle() == WeaponStyle.STAFF && context.nearbyEnemies() > 1
                || comboStep % 4 == 3;
        ItemStack weapon = mob.getMainHandItem();
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
        if (tier == null || context.selfQiRatio() < LOW_QI_RATIO) {
            return false;
        }
        ItemStack weapon = mob.getMainHandItem();
        double weaponRange = weaponRange(context.weaponStyle());
        double totalRange = weaponRange + tier.additionalRange();
        boolean purposeful = interrupt
                || context.targetRetreating()
                || context.targetFlying()
                || context.targetHealthRatio() <= 0.15F
                || context.distance() > weaponRange - 0.25D && context.distance() <= totalRange;
        if (!purposeful || context.distance() > totalRange + 0.75D) {
            return false;
        }
        int maxQi = Math.max(1, NpcProfile.maxQi(mob, archetype()));
        int cost = Math.max(1, (int) Math.ceil(maxQi * tier.costRatio()));
        if (NpcProfile.qi(mob, archetype()) < cost) {
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
        boolean released = WeaponQiSlashEntity.releaseNpc(
                mob,
                context.target(),
                totalRange,
                damage,
                radius,
                element,
                slashStage,
                heavyCone);
        if (!released) {
            NpcProfile.restoreQi(mob, archetype(), cost);
            return false;
        }
        comboStep = (comboStep + 1) % 12;
        setReadyTick("ExternalQi", now + 30L + mob.getRandom().nextInt(21));
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
                && tryCast(null, ModSpells.REGENERATION, ActionKind.DEFENSE)) {
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
                NpcSpellcasting.hasFlightEffect(mob) ? ModSpells.YUFENG_JUE : "");
        if (predicted == null) {
            return false;
        }
        double speed = (NpcSpellcasting.hasMovementEffect(mob) ? 1.4D : 1.0D)
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
        boolean willFly = NpcSpellcasting.hasFlightEffect(mob)
                || ModSpells.YUFENG_JUE.equals(ModSpells.normalizeId(movementSpellId));
        return willFly
                ? canFlyTo(destination) ? destination : null
                : isSafe(destination) && canWalkTo(destination) ? destination : null;
    }

    private boolean issueMovement(Vec3 destination, double speed, ActionKind action) {
        if (destination == null || !isSafe(destination) || !canTravelTo(destination)) {
            return false;
        }
        boolean issued;
        if (NpcSpellcasting.hasFlightEffect(mob)) {
            mob.getNavigation().stop();
            flightDestination = destination;
            flightRequiresGround = action == ActionKind.RECOVER_QI;
            double distance = Math.sqrt(mob.position().distanceToSqr(destination));
            long travelTicks = (long) Math.ceil(distance / Math.max(0.1D, speed));
            flightIntentUntil = mob.level().getGameTime() + Math.max(30L, Math.min(120L, travelTicks * 3L));
            issued = true;
        } else {
            clearFlightIntent();
            issued = mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
        if (issued && action != null) {
            remember(action, "");
        }
        return issued;
    }

    public void tickFlightMotion(double speed) {
        if (!NpcSpellcasting.hasFlightEffect(mob)) {
            clearFlightIntent();
            return;
        }
        if (!YufengJue.hasFlightSupport(mob)) {
            mob.removeEffect(ModEffects.YUFENG_JUE);
            clearFlightIntent();
            mob.setNoGravity(false);
            return;
        }
        long now = mob.level().getGameTime();
        if (mob.isNoAi() || NpcProfile.isInteracting(mob)
                || mob.hasEffect(ModEffects.QI_GATHERING)
                || pendingCast != null) {
            holdForAction();
            return;
        }
        boolean reachedDestination = flightRequiresGround
                ? hasStableRecoveryFooting()
                : flightDestination != null
                && mob.position().distanceToSqr(flightDestination) <= 1.25D * 1.25D;
        if (flightRequiresGround && now >= flightIntentUntil && !reachedDestination) {
            mob.removeEffect(ModEffects.YUFENG_JUE);
            mob.setNoGravity(false);
            clearFlightIntent();
            return;
        }
        if (flightDestination == null || now >= flightIntentUntil || reachedDestination) {
            clearFlightIntent();
            NpcSpellcasting.tickYufengFlight(mob, null, speed);
            return;
        }
        NpcSpellcasting.tickYufengFlight(mob, flightDestination, speed);
    }

    public void clearFlightIntent() {
        flightDestination = null;
        flightIntentUntil = 0L;
        flightRequiresGround = false;
    }

    private void holdForAction() {
        Vec3 movement = mob.getDeltaMovement();
        if (NpcSpellcasting.hasFlightEffect(mob)) {
            mob.setDeltaMovement(Vec3.ZERO);
        } else {
            mob.setDeltaMovement(movement.x * 0.2D, movement.y, movement.z * 0.2D);
        }
        mob.hurtMarked = true;
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
            double score = retreat ? targetDistance : -Math.abs(targetDistance - 7.0D);
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
        return NpcSpellcasting.hasFlightEffect(mob)
                ? canFlyTo(destination)
                : canWalkTo(destination);
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
                    || !isClearTravelSpace(point) || !hasSafeSupportBelow(point, 20)) {
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
        return NpcSpellcasting.hasFlightEffect(mob) && hasSafeSupportBelow(position, 20);
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
                || CombatStateManager.isPreparingSpell(player)
                || ModPayloads.ModPayloadsHandler.isQueuedComboCasting(player));
    }

    private boolean isRecovering(LivingEntity entity) {
        return entity.hasEffect(ModEffects.QI_GATHERING)
                || entity.hasEffect(MobEffects.REGENERATION);
    }

    private boolean isBlocking(LivingEntity entity) {
        return entity instanceof ServerPlayer player && CombatStateManager.isBlocking(player)
                || entity.isBlocking();
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
        return target instanceof CultivationNpc npc && NpcProfile.learnedSpellIds(
                        (PathfinderMob) target, npc.npcArchetype()).stream()
                .anyMatch(id -> {
                    NpcSpellcasting.SpellProfile profile = NpcSpellcasting.profile(id);
                    return profile != null && NpcSpellcasting.isAttackSpell(id) && profile.maxRange() >= 8.0D;
                })
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
        if (cast == null) {
            return;
        }
        LivingEntity target = resolvePendingTarget(cast.targetId());
        if (cast.targetId() != null && target == null
                || target != null && !validTarget(target)
                || mob.hasEffect(ModEffects.DAZE)
                || mob.hasEffect(ModEffects.DINGSHEN)
                || mob.hasEffect(ModEffects.FROZEN)
                || mob.hasEffect(ModEffects.FROZEN_QI)
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
        if (!NpcSpellcasting.cast(mob, target, cast.spellId())) {
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
        mob.setDeltaMovement(0.0D, NpcSpellcasting.hasFlightEffect(mob) ? 0.0D : movement.y, 0.0D);
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
            case WEAPON_LIGHT -> 8;
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
