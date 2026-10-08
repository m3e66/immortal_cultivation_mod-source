package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.SpiritSealSystem;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class ThoughtControlGameTests {
    private static FakePlayer player(GameTestHelper h, int thoughts) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "thought-test"));
        player.setGameMode(GameType.SURVIVAL);
        ModAttachments.setData(player, ModAttachments.getData(player).withThoughts(thoughts));
        return player;
    }
    private static int thoughts(FakePlayer player) { return ModAttachments.getData(player).thoughts(); }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void sharedControlPressureMustEmptyThoughtsBeforeLockingActions(GameTestHelper h) {
        var p = player(h, 2);
        for (var effect : java.util.List.of(ModEffects.DAZE, ModEffects.DINGSHEN, ModEffects.FROZEN)) {
            h.assertTrue(!p.addEffect(new MobEffectInstance(effect, 100)), "A resisted control effect is not installed");
            h.assertTrue(!p.hasEffect(effect) && !CombatStateManager.isSpellcastingBlocked(p), "Positive Thoughts keep actions usable");
        }
        h.assertTrue(thoughts(p) == 1, "Different controls in one interval share a single Thought charge");
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 0, "Sustained pressure spends the next Thought after twenty ticks");
            h.assertTrue(p.addEffect(new MobEffectInstance(ModEffects.FROZEN_QI, 100)), "Incoming control applies after Thoughts reach zero");
            h.assertTrue(p.hasEffect(ModEffects.FROZEN_QI) && CombatStateManager.isSpellcastingBlocked(p),
                    "Zero Thoughts permits the original spell lock");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void vanillaImmobilizationIsGuardedButOrdinaryDebuffsAreNot(GameTestHelper h) {
        var p = player(h, 100);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        h.assertTrue(thoughts(p) == 100 && p.hasEffect(MobEffects.POISON), "Damage and ordinary slowing effects are unaffected");
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        h.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 10)), "Vanilla immobilizing modifiers are intercepted");
        h.assertTrue(thoughts(p) == 99 && p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0,
                "The rejected freeze cannot install its zero-speed modifier");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void auraRefreshesChargeAtMostOncePerSecond(GameTestHelper h) {
        var p = player(h, 100);
        for (int i = 0; i < 100; i++) p.addEffect(new MobEffectInstance(ModEffects.DAZE, 40));
        h.assertTrue(thoughts(p) == 99, "One hundred aura refreshes consume a single Thought");
        for (int i = 0; i < 100; i++) { p.hasEffect(ModEffects.DAZE); p.getEffect(ModEffects.DAZE); }
        h.assertTrue(thoughts(p) == 99, "Effect reads never charge Thoughts");
        h.runAfterDelay(21, () -> {
            ThoughtControlGuard.tick(p);
            p.addEffect(new MobEffectInstance(ModEffects.DAZE, 40));
            h.assertTrue(thoughts(p) == 98 && !p.hasEffect(ModEffects.DAZE), "Tick and aura refresh share the next single-Thought interval");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void forcedEffectsAndThoughtRecoveryRespectTheSameGate(GameTestHelper h) {
        var p = player(h, 100);
        p.forceAddEffect(new MobEffectInstance(ModEffects.DINGSHEN, 100), null);
        h.assertTrue(thoughts(p) == 99 && !p.hasEffect(ModEffects.DINGSHEN), "Forced effects cannot bypass the fixed one-Thought guard");
        ModAttachments.setData(p, ModAttachments.getData(p).withThoughts(0));
        p.forceAddEffect(new MobEffectInstance(ModEffects.DINGSHEN, 100), null);
        h.assertTrue(p.hasEffect(ModEffects.DINGSHEN), "At zero the original effect really exists");
        ServerEvents.adjustThoughts(p, 1);
        h.assertTrue(thoughts(p) == 1 && !p.hasEffect(ModEffects.DINGSHEN)
                && !p.getActiveEffectsMap().containsKey(ModEffects.DINGSHEN), "Recovery releases existing effects and keeps the recovered Thought");
        h.succeed();
    }

    /** Simulates an external mod effect with no entry in our explicit effect tag. */
    private static final class ExternalStunEffect extends MobEffect {
        ExternalStunEffect() { super(MobEffectCategory.HARMFUL, 0x555555); }
    }
    @GameTest(template = "fate_test")
    public static void unlistedExternalStunUsesTheGenericEffectPath(GameTestHelper h) {
        var p = player(h, 100);
        var external = Holder.<MobEffect>direct(new ExternalStunEffect());
        h.assertTrue(!external.is(ThoughtControlGuard.CONTROL_EFFECTS), "Fixture is not explicitly listed by this mod");
        h.assertTrue(!p.addEffect(new MobEffectInstance(external, 100)), "An unlisted foreign stun is identified by its effect class");
        h.assertTrue(thoughts(p) == 99 && !p.hasEffect(external), "External control drains one Thought without blocking input");
        h.assertTrue(ThoughtControlGuard.matchesControlName("deep_paralysis")
                && ThoughtControlGuard.matchesControlName("magic_silenced")
                && !ThoughtControlGuard.matchesControlName("frost_resistance"), "External names recognize control, not resistance buffs");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void naturalImmunityDoesNotLoseThoughts(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "immune-test")) {
            @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
        };
        ModAttachments.setData(p, ModAttachments.getData(p).withThoughts(100));
        h.assertTrue(!p.addEffect(new MobEffectInstance(ModEffects.DAZE, 100)) && thoughts(p) == 100,
                "An attack already rejected by natural immunity cannot spend the shield");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void nativePostureAndRepelCannotSkipThoughts(GameTestHelper h) {
        var p = player(h, 100);
        CombatStateManager.enterCombat(p);
        CombatStateManager.damagePosture(p, 100000);
        h.assertTrue(thoughts(p) == 99 && !CombatStateManager.isPostureBroken(p), "Posture exhaustion spends one Thought before movement can lock");
        UndeadControl.stunFromRepel(p, 20);
        h.assertTrue(thoughts(p) == 99 && !p.getPersistentData().contains(UndeadControl.TAG_REPEL_STUN_UNTIL),
                "Repel stun shares the same interval without an extra charge or installed timer");
        ModAttachments.setData(p, ModAttachments.getData(p).withThoughts(0));
        CombatStateManager.damagePosture(p, 100000);
        h.assertTrue(CombatStateManager.isPostureBroken(p), "A real posture lock works when Thoughts are empty");
        ServerEvents.adjustThoughts(p, 1);
        h.assertTrue(!CombatStateManager.isPostureBroken(p), "Recovery immediately lifts the native movement lock");
        CombatStateManager.clear(p); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void spiritSealDoesNotInstallItsSideEffectsWhenResisted(GameTestHelper h) {
        var p = player(h, 100);
        SpiritSealSystem.apply(p, p);
        h.assertTrue(thoughts(p) == 99 && !p.hasEffect(ModEffects.SPIRIT_SEAL)
                && !p.getPersistentData().getBoolean(ImmortalCultivationMod.MODID + ":spirit_seal_active"),
                "Resisting a seal also prevents its independent activation/interrupt path");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void mixedSourcesAmplifiersAndAttackStatsShareOneFixedCharge(GameTestHelper h) {
        var p = player(h, 100);
        var first = player(h, 100); var second = player(h, 100);
        ModAttachments.setData(first, ModAttachments.getData(first).debugAdjustStat("mental", 10_000));
        ModAttachments.setData(second, ModAttachments.getData(second).debugAdjustStat("mental", 5_000));
        h.assertTrue(com.example.immortal_cultivation_mod.spell.ThoughtDamageHelper.bonus(first) == 10_000,
                "Fixture attacker really has a large Thought Damage bonus");
        h.assertTrue(!p.addEffect(new MobEffectInstance(ModEffects.DAZE, 60, 9), first),
                "Strong amplified control is resisted");
        for (var effect : java.util.List.of(ModEffects.DINGSHEN, ModEffects.FROZEN, ModEffects.FROZEN_QI)) {
            h.assertTrue(!p.addEffect(new MobEffectInstance(effect, 60, 10), second), "Other attackers and controls remain resisted");
        }
        p.forceAddEffect(new MobEffectInstance(Holder.direct(new ExternalStunEffect()), 60, 20), second);
        for (int i = 0; i < 12; i++) {
            h.assertTrue(ThoughtControlGuard.resist(p, "test/input/" + i, i % 2 == 0 ? first : second, Integer.MAX_VALUE),
                    "Different compatibility and input sources use the same protection budget");
        }
        CombatStateManager.enterCombat(p); CombatStateManager.damagePosture(p, 100000);
        UndeadControl.stunFromRepel(p, 60);
        h.assertTrue(thoughts(p) == 99 && !CombatStateManager.isSpellcastingBlocked(p)
                        && !CombatStateManager.isPostureBroken(p),
                "Amplifiers, attack bonuses, native locks and many source IDs cannot multiply the single Thought charge");
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(ThoughtControlGuard.resistForDuration(p, "test/new_attacker", second, 30),
                    "Another sustained source still shares the current interval");
            p.forceAddEffect(new MobEffectInstance(ModEffects.DAZE, 30, 30), first);
            for (int i = 0; i < 100; i++) { p.hasEffect(ModEffects.DAZE); p.getEffect(ModEffects.DAZE); }
            h.assertTrue(thoughts(p) == 98, "Tick, new attackers, forced refresh and effect reads consume only one additional Thought");
            CombatStateManager.clear(p); p.discard(); first.discard(); second.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 80)
    public static void oneRejectedEffectDrainsEachSecondUntilItsOriginalExpiry(GameTestHelper h) {
        var p = player(h, 100);
        h.assertTrue(!p.addEffect(new MobEffectInstance(ModEffects.DINGSHEN, 45)) && thoughts(p) == 99,
                "A single rejected effect immediately consumes one Thought without being installed");
        h.runAfterDelay(19, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 99, "Sustained control cannot drain again before twenty ticks");
        });
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 98 && !p.hasEffect(ModEffects.DINGSHEN), "The rejected effect drains once at its first full second without refresh");
        });
        h.runAfterDelay(39, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 98, "Intermediate ticks cannot add extra charges");
        });
        h.runAfterDelay(40, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 97, "The still-active original duration drains once at the next second");
        });
        h.runAfterDelay(45, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 97, "Pressure stops at the rejected effect's original expiry");
        });
        h.runAfterDelay(60, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 97 && !p.getActiveEffectsMap().containsKey(ModEffects.DINGSHEN),
                    "Expired hidden pressure neither drains later nor creates a stale control lock");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 80)
    public static void recoveryLiftsLockImmediatelyAndRetainsItsRemainingPressureDuration(GameTestHelper h) {
        var p = player(h, 0);
        p.forceAddEffect(new MobEffectInstance(ModEffects.DINGSHEN, 45), null);
        h.assertTrue(p.hasEffect(ModEffects.DINGSHEN), "The existing control can apply while Thoughts are empty");
        ServerEvents.adjustThoughts(p, 3);
        h.assertTrue(thoughts(p) == 3 && !p.hasEffect(ModEffects.DINGSHEN),
                "Recovery removes the installed control without spending the recovered Thoughts immediately");
        h.runAfterDelay(19, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 3, "Recovered control pressure waits for its next full interval");
        });
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 2, "Remaining control duration resumes fixed one-Thought pressure after twenty ticks");
        });
        h.runAfterDelay(40, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && !CombatStateManager.isSpellcastingBlocked(p),
                    "Recovered Thoughts preserve usable actions while the next interval drains one");
        });
        h.runAfterDelay(60, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && !p.getActiveEffectsMap().containsKey(ModEffects.DINGSHEN),
                    "Removed control pressure expires normally instead of consuming the final recovered Thought forever");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void exhaustionDazeDoesNotBecomeNewControlPressureAfterRecovery(GameTestHelper h) {
        var p = player(h, 0);
        ServerEvents.adjustThoughts(p, 0);
        h.assertTrue(p.hasEffect(ModEffects.DAZE), "Thought exhaustion still installs its normal daze");
        ServerEvents.adjustThoughts(p, 1);
        h.assertTrue(thoughts(p) == 1 && !p.hasEffect(ModEffects.DAZE),
                "Recovery immediately lifts exhaustion daze and preserves the recovered Thought");
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && !CombatStateManager.isSpellcastingBlocked(p),
                    "Own exhaustion cannot masquerade as external pressure and spend the recovered Thought again");
        });
        h.runAfterDelay(40, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && !p.getActiveEffectsMap().containsKey(ModEffects.DAZE),
                    "Exhaustion alone cannot create a repeating zero-Thought daze loop");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 80)
    public static void nativeControlAppliedAtZeroKeepsOnlyItsRemainingTimedPressure(GameTestHelper h) {
        var p = player(h, 0);
        UndeadControl.stunFromRepel(p, 45);
        h.assertTrue(thoughts(p) == 0
                        && p.getPersistentData().getLong(UndeadControl.TAG_REPEL_STUN_UNTIL) > p.level().getGameTime(),
                "Timed native restraint applies normally at zero Thoughts");
        ServerEvents.adjustThoughts(p, 3);
        h.assertTrue(thoughts(p) == 3 && ThoughtControlGuard.hasProtection(p)
                        && !CombatStateManager.isSpellcastingBlocked(p),
                "Recovery restores usable controls without immediately spending recovered Thoughts");
        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 2, "Native restraint remembered at zero drains one after recovery's full interval");
        });
        h.runAfterDelay(40, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && ThoughtControlGuard.hasProtection(p),
                    "Remaining native pressure drains once per second without reinstalling a lock");
        });
        h.runAfterDelay(45, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1, "Native pressure stops at its original expiry");
        });
        h.runAfterDelay(60, () -> {
            ThoughtControlGuard.tick(p);
            h.assertTrue(thoughts(p) == 1 && !CombatStateManager.isSpellcastingBlocked(p),
                    "Expired native restraint cannot keep charging recovered Thoughts");
            p.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void shortExternalDazeCannotInheritExhaustionDurationButLongDazeStillDrains(GameTestHelper h) {
        var alreadyEmpty = player(h, 0);
        ThoughtControlGuard.applyExhaustion(alreadyEmpty);
        alreadyEmpty.addEffect(new MobEffectInstance(ModEffects.DAZE, 1));
        ServerEvents.adjustThoughts(alreadyEmpty, 1);

        var exhaustedByHit = player(h, 1);
        exhaustedByHit.addEffect(new MobEffectInstance(ModEffects.DAZE, 1));
        h.assertTrue(thoughts(exhaustedByHit) == 0 && exhaustedByHit.hasEffect(ModEffects.DAZE),
                "A short external daze exhausting the last Thought triggers normal nested exhaustion daze");
        ServerEvents.adjustThoughts(exhaustedByHit, 1);

        var longerExternal = player(h, 0);
        ThoughtControlGuard.applyExhaustion(longerExternal);
        longerExternal.addEffect(new MobEffectInstance(ModEffects.DAZE, 100));
        h.assertTrue(longerExternal.getEffect(ModEffects.DAZE).getDuration() == 100,
                "Long external daze retains its actual duration over the shorter exhaustion effect");
        ServerEvents.adjustThoughts(longerExternal, 3);
        h.assertTrue(thoughts(alreadyEmpty) == 1 && thoughts(exhaustedByHit) == 1
                        && thoughts(longerExternal) == 3
                        && !alreadyEmpty.hasEffect(ModEffects.DAZE)
                        && !exhaustedByHit.hasEffect(ModEffects.DAZE)
                        && !longerExternal.hasEffect(ModEffects.DAZE),
                "Recovery lifts all daze variants immediately without a fresh Thought charge");

        h.runAfterDelay(20, () -> {
            ThoughtControlGuard.tick(alreadyEmpty); ThoughtControlGuard.tick(exhaustedByHit);
            ThoughtControlGuard.tick(longerExternal);
            h.assertTrue(thoughts(alreadyEmpty) == 1 && thoughts(exhaustedByHit) == 1,
                    "One-tick incoming daze cannot acquire the exhaustion effect's forty-tick pressure");
            h.assertTrue(thoughts(longerExternal) == 2,
                    "Exhaustion provenance cannot suppress a genuinely longer external daze's pressure");
        });
        h.runAfterDelay(40, () -> {
            ThoughtControlGuard.tick(alreadyEmpty); ThoughtControlGuard.tick(exhaustedByHit);
            ThoughtControlGuard.tick(longerExternal);
            h.assertTrue(thoughts(alreadyEmpty) == 1 && thoughts(exhaustedByHit) == 1
                            && thoughts(longerExternal) == 1,
                    "Short daze stays expired while real longer external pressure retains the fixed interval");
            alreadyEmpty.discard(); exhaustedByHit.discard(); longerExternal.discard(); h.succeed();
        });
    }
}
