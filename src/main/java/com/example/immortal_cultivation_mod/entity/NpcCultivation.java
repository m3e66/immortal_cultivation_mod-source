package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics;
import com.example.immortal_cultivation_mod.fate.LuckSystem;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;

/** Loaded NPCs alternate quiet meditation with normal activity, then attempt real breakthroughs. */
public final class NpcCultivation {
    static final String TRAINING = "AutonomousCultivation";
    static final int MEDITATING = 1, BREAKTHROUGH = 2;
    static final int SESSION_TICKS = 1200, CHANNEL_TICKS = 200;

    private NpcCultivation() {}

    /** Read-only: also used by the AI goal and Debug Eye. Empty means it is safe to cultivate. */
    public static String blocker(PathfinderMob mob) {
        if (!(mob instanceof CultivationNpc npc) || NpcProfile.FANREN.equals(npc.npcArchetype())) return "mortal";
        if (!mob.isAlive()) return "dead";
        if (mob.isNoAi()) return "ai_disabled";
        if (mob.getTarget() != null && mob.getTarget().isAlive() || NpcProfile.isSparring(mob)) return "combat";
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        if (profile.hasUUID("InteractingPlayer")) return "interacting";
        if (profile.hasUUID("FollowingPlayer")) return "following";
        if (profile.getBoolean("StayHere") && mob.distanceToSqr(profile.getDouble("StayX"),
                profile.getDouble("StayY"), profile.getDouble("StayZ")) > 2.25) return "returning";
        if (mob.hurtTime > 0 || profile.getCompound(TRAINING).getInt("QuietTicks") > 0) return "recent_damage";
        if (!mob.onGround() || mob.isInWaterOrBubble() || mob.isInLava() || mob.isOnFire()
                || mob.isPassenger() || NpcSpellcasting.hasFlightEffect(mob)) return "unsafe_ground";
        if (npc.npcCastingTicks() > 0 || mob.hasEffect(ModEffects.QI_GATHERING)) return "casting";
        if (mob.hasEffect(ModEffects.DAZE) || mob.hasEffect(ModEffects.DINGSHEN)
                || mob.hasEffect(ModEffects.FROZEN) || mob.hasEffect(ModEffects.SPIRIT_SEAL)) return "controlled";
        String realm = profile.getString("Realm");
        if (CultivationLevels.isMortal(realm)) return "mortal";
        if (CultivationLevels.getNextStage(realm) == null) return "highest_realm";
        if (!CultivationMethods.canGainProgress(profile.getString("Method"), realm)) return "method_limit";
        return "";
    }

    public static void tick(PathfinderMob mob) {
        if (mob.level().isClientSide || !(mob instanceof CultivationNpc npc)
                || NpcProfile.FANREN.equals(npc.npcArchetype())) return;
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        if (!profile.contains(TRAINING)) {
            CompoundTag initial = new CompoundTag();
            initial.putInt("RestTicks", 200 + Math.floorMod(mob.getUUID().hashCode(), 401));
            profile.put(TRAINING, initial);
        }
        CompoundTag training = profile.getCompound(TRAINING);
        if (training.getInt("QuietTicks") > 0) training.putInt("QuietTicks", training.getInt("QuietTicks") - 1);
        if (training.getInt("RestTicks") > 0) training.putInt("RestTicks", training.getInt("RestTicks") - 1);
        String blocked = blocker(mob);
        if (!blocked.isEmpty()) {
            interrupt(mob, blocked);
            return;
        }
        int state = training.getInt("State");
        npc.setNpcCultivationState(state);
        if (state == 0) {
            if (training.getInt("RestTicks") > 0) return;
            setState(npc, training, MEDITATING, SESSION_TICKS);
            state = MEDITATING;
        }
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
        String realm = profile.getString("Realm");
        long goal = CultivationLevels.getTotalQiNeeded(realm);
        if (state == BREAKTHROUGH) {
            if (!realm.equals(training.getString("AttemptRealm")) || profile.getLong("CultivationProgress") < goal) {
                interrupt(mob, "realm_changed");
                return;
            }
            int remaining = training.getInt("Ticks") - 1;
            training.putInt("Ticks", remaining);
            if (remaining <= 0) finish(mob, npc, profile, training, realm, goal);
            return;
        }
        if (mob.tickCount % 20 == 0) {
            double gain = NpcProfile.autonomousProgressPerSecond(profile);
            double accumulated = gain + training.getDouble("Fraction");
            long whole = (long) accumulated;
            long progress = Math.max(0, profile.getLong("CultivationProgress"));
            // Saturate at the threshold, including very large late-realm progress values.
            if (progress < goal) profile.putLong("CultivationProgress", progress + Math.min(whole, goal - progress));
            training.putDouble("Fraction", accumulated - whole);
            training.putDouble("ProgressPerSecond", gain);
            int maxQi = profile.getInt("MaxQi");
            profile.putInt("Qi", (int) Math.min(maxQi, (long) profile.getInt("Qi") + Math.max(1, maxQi / 100)));
            mob.heal(mob.getMaxHealth() * .005F);
        }
        if (profile.getLong("CultivationProgress") >= goal
                && profile.getInt("Qi") >= profile.getInt("MaxQi") * .9
                && mob.getHealth() >= mob.getMaxHealth() * .8F) {
            training.putString("AttemptRealm", realm);
            profile.putInt("Qi", Math.max(0, profile.getInt("Qi") - Math.max(1, profile.getInt("MaxQi") / 5)));
            setState(npc, training, BREAKTHROUGH, CHANNEL_TICKS);
            return;
        }
        int remaining = training.getInt("Ticks") - 1;
        training.putInt("Ticks", remaining);
        if (remaining <= 0) {
            setState(npc, training, 0, 0);
            training.putInt("RestTicks", 400 + Math.floorMod(mob.getUUID().hashCode(), 401));
        }
    }

    private static void finish(PathfinderMob mob, CultivationNpc npc, CompoundTag profile,
                               CompoundTag training, String realm, long goal) {
        int luck = LuckSystem.luck(mob);
        double chance = 1.0 - CultivationLevels.getBreakthroughFailChance(luck, realm);
        double draw = mob.getRandom().nextDouble();
        training.putInt("Attempts", training.getInt("Attempts") + 1);
        training.putInt("LastLuck", luck);
        training.putDouble("LastChance", chance);
        training.putDouble("LastRoll", draw);
        training.putLong("LastAttemptTick", mob.level().getGameTime());
        boolean success = draw < chance && NpcProfile.completeAutonomousBreakthrough(mob, realm);
        training.putString("LastOutcome", success ? "success" : "failed");
        if (success) {
            training.putInt("Successes", training.getInt("Successes") + 1);
            training.putDouble("Fraction", 0);
            LuckSystem.change(mob, 3);
            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1, mob.getZ(), 48, .6, .8, .6, .06);
                level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, .8F, .8F);
                Component message = Component.translatable("npc.immortal_cultivation_mod.cultivation.breakthrough",
                        mob.getDisplayName(), CultivationLevels.displayName(profile.getString("Realm")));
                for (var player : level.players()) if (player.distanceToSqr(mob) <= 24 * 24) player.displayClientMessage(message, false);
            }
        } else {
            profile.putLong("CultivationProgress", Math.max(0, profile.getLong("CultivationProgress") - Math.max(1, goal / 10)));
            if (mob.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + .8, mob.getZ(), 12, .3, .4, .3, .015);
        }
        setState(npc, training, 0, 0);
        training.putInt("RestTicks", success ? 1200 : 2400);
    }

    public static void onHurt(PathfinderMob mob) {
        if (mob.level().isClientSide) return;
        interrupt(mob, "recent_damage");
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        if (!profile.contains(TRAINING)) profile.put(TRAINING, new CompoundTag());
        profile.getCompound(TRAINING).putInt("QuietTicks", 200);
    }

    public static void interrupt(PathfinderMob mob, String reason) {
        if (mob.level().isClientSide || !(mob instanceof CultivationNpc npc)) return;
        CompoundTag training = NpcProfile.cultivationProfile(mob).getCompound(TRAINING);
        int state = training.getInt("State");
        if (state != 0) {
            training.putString("LastOutcome", reason);
            training.putInt("RestTicks", Math.max(training.getInt("RestTicks"), state == BREAKTHROUGH ? 1200 : 200));
        }
        setState(npc, training, 0, 0);
    }

    private static void setState(CultivationNpc npc, CompoundTag training, int state, int ticks) {
        training.putInt("State", state);
        training.putInt("Ticks", ticks);
        npc.setNpcCultivationState(state);
    }

    /** Exposes the saved state and real RNG result without ticking, rolling, or awarding progress. */
    public static List<Component> debugLines(PathfinderMob mob) {
        CompoundTag profile = NpcProfile.cultivationProfile(mob), training = profile.getCompound(TRAINING);
        String blocked = blocker(mob);
        String status = !blocked.isEmpty() ? blocked : switch (training.getInt("State")) {
            case MEDITATING -> "meditating";
            case BREAKTHROUGH -> "attempting";
            default -> "resting";
        };
        return List.of(
                DebugEyeDiagnostics.text("npc_cultivation", CultivationLevels.displayName(profile.getString("Realm")),
                        Component.translatable("npc.immortal_cultivation_mod.cultivation." + status)),
                DebugEyeDiagnostics.text("npc_progress", profile.getLong("CultivationProgress"),
                        CultivationLevels.getTotalQiNeeded(profile.getString("Realm")),
                        DebugEyeDiagnostics.number(NpcProfile.autonomousProgressPerSecond(profile)), profile.getString("Method")),
                DebugEyeDiagnostics.text("npc_resources", profile.getInt("Qi"), profile.getInt("MaxQi"), profile.getInt("Age")),
                DebugEyeDiagnostics.text("npc_timers", training.getInt("Ticks"), training.getInt("RestTicks"), training.getInt("QuietTicks")),
                DebugEyeDiagnostics.text("npc_attempts", training.getInt("Attempts"), training.getInt("Successes"),
                        training.getString("LastOutcome"), training.getInt("LastLuck"),
                        DebugEyeDiagnostics.percent(training.getDouble("LastChance")), DebugEyeDiagnostics.percent(training.getDouble("LastRoll"))));
    }
}
