package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.alchemy.AlchemyEffects;
import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.mixin.BodyEffectAccessor;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_yin_yang")
@PrefixGameTestTemplate(false)
public final class BodyTypeGameTests {
    private static FakePlayer player(GameTestHelper h, String body) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "body-test")) {
            @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
            @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) { return true; }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 2, 1)));
        try {
            var field = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true); field.setInt(player, 0);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        ModAttachments.setData(player, ModAttachments.getData(player)
                .withCultivationLevel(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY)
                .withThoughts(0).withQi(0).debugAdjustStat("max_hp", 750).withBodyType(body));
        player.setHealth(player.getMaxHealth());
        return player;
    }
    private static LivingEntity target(GameTestHelper h) {
        var target = h.spawn(EntityType.PIG, new BlockPos(1, 2, 2));
        target.setNoAi(true); target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        return target;
    }
    private static void body(FakePlayer player, String body) {
        ModAttachments.setData(player, ModAttachments.getData(player).withBodyType(body));
    }
    private static float hit(LivingEntity target, DamageSource source, float amount) {
        target.invulnerableTime = 0; float health = target.getHealth(); target.hurt(source, amount); return health - target.getHealth();
    }
    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < .003, message + ": " + actual + " expected " + expected);
    }

    @GameTest(template = "fate_test")
    public static void exactWeightedPoolsAndEverySaveRoundTrip(GameTestHelper h) {
        var counts = new HashMap<String, Integer>();
        for (int roll = 0; roll < 15000; roll++) counts.merge(BodyTypes.bodyForRoll(roll), 1, Integer::sum);
        h.assertTrue(counts.size() == 21 && counts.get(BodyTypes.MORTAL_BODY) == 13500, "90% mortal and all 21 bodies reachable");
        for (var definition : BodyTypes.DEFINITIONS) {
            int expected = switch (definition.rarity()) { case MORTAL -> 13500; case ADVANCED -> 150; case RARE -> 30; case LEGENDARY -> 10; };
            h.assertTrue(counts.get(definition.id()) == expected, "Each higher section has a lower individual probability: " + definition.id());
            var data = ModAttachments.CultivationData.createDefault().withBodyType(definition.id()).withQi(37).withCultivationProgress(81234567890L);
            var saved = ModAttachments.CultivationData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            h.assertTrue(ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().equals(data), "Body and progress survive saved-world reload");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void statsQiCapacityAndDebugSwitchRemoveOldBonuses(GameTestHelper h) {
        var p = player(h, BodyTypes.EVERGREEN);
        near(h, p.getMaxHealth(), 1150, "Evergreen maximum HP");
        body(p, BodyTypes.ANCIENT_SACRED); near(h, p.getMaxHealth(), 1250, "Sacred maximum HP");
        p.getAttribute(Attributes.ARMOR).setBaseValue(100);
        BodyTypeEffects.refresh(p); near(h, p.getAttributeValue(Attributes.ARMOR), 120.75, "Sacred armor includes equipment and realm armor");
        body(p, BodyTypes.PROFOUND_EARTH); near(h, p.getAttributeValue(Attributes.ARMOR), 126, "Earth armor includes equipment and realm armor");
        body(p, BodyTypes.WINDCHASER);
        near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), .12, "Windchaser movement speed");
        near(h, p.getAttributeValue(Attributes.ATTACK_SPEED), 4.6, "Windchaser attack speed");
        body(p, BodyTypes.GENG_METAL); p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        BodyTypeEffects.refresh(p); near(h, p.getAttributeValue(Attributes.ATTACK_SPEED), 4.4, "Sword attack speed only while holding sword");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY); BodyTypeEffects.refresh(p);
        near(h, p.getAttributeValue(Attributes.ATTACK_SPEED), 4, "Sword speed removed after unequipping");
        body(p, BodyTypes.VAST_SEA); h.assertTrue(ModAttachments.getData(p).maxQi() == 600, "Sea maximum Qi +20%");
        h.assertTrue(ModAttachments.getData(p).debugSetStat("max_qi", 1200,
                CultivationLevels.getLevelDef(ModAttachments.getData(p).cultivationLevel())).maxQi() == 1200,
                "Debug maximum Qi input includes body bonus exactly once");
        body(p, BodyTypes.NINE_ORIFICE); h.assertTrue(ModAttachments.getData(p).maxQi() == 550, "Nine-Orifice maximum Qi +10%");
        body(p, BodyTypes.MORTAL_BODY); near(h, p.getMaxHealth(), 1000, "Changing body removes old HP modifier");
        near(h, p.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Changing body removes old movement modifier");
        p.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void spellCostDiscountAppliesOnceAndDoesNotDiscountForging(GameTestHelper h) {
        var p = player(h, BodyTypes.INNATE_DAO);
        ModAttachments.setData(p, ModAttachments.getData(p).withQi(500));
        CenterEffectSupport.beginSpellCast(p, ModSpells.WEIYA);
        try { h.assertTrue(ServerEvents.spendSpellQiOrBlood(p, ModAttachments.getData(p), 100), "Neutral spell paid"); }
        finally { CenterEffectSupport.endSpellCast(); }
        h.assertTrue(ModAttachments.getData(p).qi() == 410, "Neutral spell discounted once, not twice");
        ServerEvents.spendQiOrBlood(p, ModAttachments.getData(p), 100);
        h.assertTrue(ModAttachments.getData(p).qi() == 310, "Non-spell actions pay unchanged cost");
        near(h, AlchemyEffects.cultivationGain(p, 100), 115, "Dao cultivation gain");
        near(h, AlchemyEffects.recoveryAmount(p, AlchemyChemistry.Effect.CULTIVATION, 1), 92,
                "Dao bonus also applies to cultivation gained from pills");
        body(p, BodyTypes.VAST_SEA);
        near(h, CenterEffectSupport.adjustSpellQiCost(p, 100, ModSpells.DIELANG_SHIELD), 85, "Water spells cost 15% less Qi");
        near(h, CenterEffectSupport.adjustSpellQiCost(p, 100, ModSpells.FIREBALL), 100, "Sea does not discount fire spells");
        body(p, BodyTypes.NINE_ORIFICE); near(h, BodyTypeEffects.cooldown(p, 100), 90, "Nine-Orifice spell cooldown");
        near(h, BodyTypeEffects.qiRegeneration(p, 100), 125, "Nine-Orifice Qi regeneration");
        body(p, BodyTypes.ANCIENT_SACRED); near(h, BodyTypeEffects.qiRegeneration(p, 100), 85, "Sacred Qi regeneration penalty");
        p.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void elementalBonusesAndBurningTargetBonusApplyToActualHits(GameTestHelper h) {
        var p = player(h, BodyTypes.CRIMSON_FLAME); var target = target(h);
        DamageSource fire = new DamageSource(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FIREBALL), p, p);
        near(h, hit(target, fire, 20), 25, "Crimson fire bonus");
        target.igniteForSeconds(10); near(h, hit(target, fire, 20), 27.5, "Crimson additional burning-target bonus");
        target.clearFire(); body(p, BodyTypes.VAST_SEA);
        near(h, hit(target, BodyTypeEffects.spellSource(target.damageSources().indirectMagic(p, p), ModSpells.DIELANG_SHIELD), 20), 24, "Delayed water damage retains element");
        body(p, BodyTypes.PROFOUND_ICE);
        near(h, hit(target, BodyTypeEffects.spellSource(target.damageSources().indirectMagic(p, p), ModSpells.SHUANGTIAN_QI), 20), 25, "Ice damage bonus");
        body(p, BodyTypes.ANCIENT_SACRED);
        near(h, hit(target, target.damageSources().playerAttack(p), 20), 24, "Sacred physical damage bonus");
        body(p, BodyTypes.SUPREME_YIN);
        near(h, hit(target, BodyTypeEffects.spellSource(target.damageSources().indirectMagic(p, p), ModSpells.ABSORB_CULTIVATION), 20), 26, "Yin damage bonus");
        body(p, BodyTypes.SOLAR_DIVINE); near(h, hit(target, fire, 20), 26, "Solar fire bonus");
        body(p, BodyTypes.INNATE_DAO); near(h, hit(target, fire, 20), 21, "Dao all-element bonus");
        p.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void ownedBurningKeepsFireBonusesUntilExtinguished(GameTestHelper h) {
        var p = player(h, BodyTypes.CRIMSON_FLAME); h.getLevel().addNewPlayer(p); var target = target(h);
        DamageSource fire = new DamageSource(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FIREBALL), p, p);
        hit(target, fire, 4); target.igniteForSeconds(10);
        near(h, hit(target, target.damageSources().onFire(), 4), 5.5, "Ongoing burn keeps fire and burning-target bonuses");
        target.clearFire(); target.igniteForSeconds(10);
        near(h, hit(target, target.damageSources().onFire(), 4), 4, "Later unrelated fire does not inherit previous caster");
        target.clearFire(); body(p, BodyTypes.SOLAR_DIVINE); hit(target, fire, 4); target.igniteForSeconds(10);
        near(h, hit(target, target.damageSources().onFire(), 4), 5.2, "Solar body also boosts ongoing fire damage");
        p.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void fiveAcceptedIceHitsFreezeAndFiveThunderAttacksProcOnce(GameTestHelper h) {
        var p = player(h, BodyTypes.PROFOUND_ICE); var target = target(h);
        for (int i = 0; i < 4; i++) hit(target, target.damageSources().playerAttack(p), 4);
        h.assertTrue(!target.hasEffect(ModEffects.FROZEN), "Four Frost stacks do not freeze");
        hit(target, target.damageSources().playerAttack(p), 4);
        h.assertTrue(target.hasEffect(ModEffects.FROZEN) && target.getEffect(ModEffects.FROZEN).getDuration() == 40, "Fifth accepted hit freezes for 2s");
        body(p, BodyTypes.VIOLET_THUNDER); target.removeEffect(ModEffects.FROZEN);
        var type = h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
        for (int i = 0; i < 5; i++) {
            var bolt = EntityType.LIGHTNING_BOLT.create(h.getLevel());
            near(h, hit(target, new DamageSource(type, bolt, p), 8), i == 4 ? 15 : 10, "Fifth lightning attack adds half actual hit damage");
        }
        p.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void meleeLifestealSwordPiercingAndShieldPressure(GameTestHelper h) {
        var p = player(h, BodyTypes.BLOOD_FIEND); var target = target(h); p.setHealth(500);
        near(h, hit(target, target.damageSources().playerAttack(p), 20), 24, "Blood Fiend melee bonus");
        near(h, p.getHealth(), 501.2, "Blood Fiend heals 5% of actual melee damage");
        body(p, BodyTypes.GENG_METAL); p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        near(h, hit(target, target.damageSources().playerAttack(p), 20), 24, "Geng sword damage bonus");
        near(h, BodyTypeEffects.armorBypass(target.damageSources().playerAttack(p)), .1, "Geng sword ignores 10% armor");
        target.getAttribute(Attributes.ARMOR).setBaseValue(100);
        near(h, hit(target, target.damageSources().playerAttack(p), 20), 6.72, "Sword armor bypass modifies actual armor reduction");
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        body(p, BodyTypes.SPIRIT_DEVOURING);
        var defender = player(h, BodyTypes.MORTAL_BODY); DielangShield.activateNext(defender);
        near(h, DielangShield.remaining(defender), 12, "Initial Qi barrier");
        near(h, hit(defender, defender.damageSources().playerAttack(p), 5), 0, "Qi barrier absorbs entire hit");
        near(h, DielangShield.remaining(defender), 6, "Devouring body deals 20% extra barrier damage");
        DielangShield.clear(defender); defender.discard(); p.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void poisonImmunityResistanceDurationAndDamageCredit(GameTestHelper h) {
        var p = player(h, BodyTypes.MYRIAD_POISON);
        h.getLevel().addNewPlayer(p);
        h.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.POISON, 100)) && !p.addEffect(new MobEffectInstance(ModEffects.SHIDU, 100)), "Both poison types are rejected");
        p.forceAddEffect(new MobEffectInstance(MobEffects.POISON, 100), null);
        h.assertTrue(!p.hasEffect(MobEffects.POISON), "Forced poison also rejected");
        var target = target(h); target.addEffect(new MobEffectInstance(ModEffects.SHIDU, 100), p);
        near(h, hit(target, BodyTypeEffects.poisonSource(target, target.damageSources().magic()), 4), 5, "Poison damage keeps owner and gains 25%");
        body(p, BodyTypes.IMMACULATE); p.getRandom().setSeed(1729);
        var effect = new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, true, false, false);
        ((BodyEffectAccessor)(Object)effect).immortalCultivation$hidden(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
        int resisted = 0;
        for (int i = 0; i < 1000; i++) {
            p.removeEffect(MobEffects.WEAKNESS);
            if (!p.addEffect(effect)) resisted++;
            else {
                var applied = p.getEffect(MobEffects.WEAKNESS);
                h.assertTrue(applied.getDuration() == 150 && applied.getAmplifier() == 2 && applied.isAmbient() && !applied.isVisible() && !applied.showIcon(), "Debuff shortened while flags remain intact");
                h.assertTrue(((BodyEffectAccessor)(Object)applied).immortalCultivation$hidden().getDuration() == 300, "Hidden lower-amplifier debuff also shortened");
            }
        }
        h.assertTrue(resisted > 240 && resisted < 360 && effect.getDuration() == 200
                && ((BodyEffectAccessor)(Object)effect).immortalCultivation$hidden().getDuration() == 400, "30% negative-effect resistance, without modifying shared input");
        p.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void naturalAndPillHealingKeepSeparateBonuses(GameTestHelper h) {
        var p = player(h, BodyTypes.EVERGREEN); p.setHealth(500); p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(6);
        for (int i = 0; i < 10; i++) p.getFoodData().tick(p);
        near(h, p.getHealth(), 501.375, "Natural food regeneration includes 25% regeneration and 10% healing");
        body(p, BodyTypes.IMMACULATE); p.setHealth(500); p.heal(10); near(h, p.getHealth(), 511.5, "Immaculate receives 15% additional healing");
        body(p, BodyTypes.BLOOD_FIEND); p.setHealth(500);
        var formula = AlchemyChemistry.formula("huichun");
        var pill = PillData.create(AlchemyChemistry.brew(formula, formula.required()));
        AlchemyEffects.consume(p, pill);
        for (int i = 0; i < 120; i++) AlchemyEffects.tickPlayer(p);
        near(h, p.getHealth(), 548, "Pill restores 6% HP with Blood Fiend's 20% pill-healing penalty");
        p.heal(10); near(h, p.getHealth(), 558, "Non-pill healing has no pill penalty");
        p.discard(); h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 160)
    public static void goldenEmergencyDefenseExpiresAndCannotImmediatelyRetrigger(GameTestHelper h) {
        var p = player(h, BodyTypes.GOLDEN_BODY); var enemy = target(h); p.setHealth(240);
        near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 17, "Golden baseline damage reduction");
        near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 11.9, "Below 20% HP grants additional 30% reduction");
        h.runAtTickTime(105, () -> {
            near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 17, "Emergency defense expires after 5s");
            near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 17, "90s cooldown prevents immediate retrigger");
            p.discard(); enemy.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void standingStillAndDodgeBonusesExpireOnMovementAndTime(GameTestHelper h) {
        var p = player(h, BodyTypes.PROFOUND_EARTH); var enemy = target(h); p.setHealth(500); p.setDeltaMovement(Vec3.ZERO); p.setOnGround(true);
        for (int i = 1; i <= 62; i++) h.runAtTickTime(i, () -> BodyTypeEffects.tick(new PlayerTickEvent.Post(p)));
        h.runAtTickTime(63, () -> {
            near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 18, "Standing for 3s adds 10% damage reduction");
            p.setPos(p.position().add(.1, 0, 0)); BodyTypeEffects.tick(new PlayerTickEvent.Post(p));
            near(h, hit(p, p.damageSources().indirectMagic(enemy, enemy), 20), 20, "Movement removes standing defense");
            body(p, BodyTypes.WINDCHASER); CombatFeedbackEvents.dashStarted(p, new Vec3(0, 0, 1), .5F, 10);
            near(h, hit(enemy, enemy.damageSources().playerAttack(p), 20), 22, "Dodge adds 10% damage");
        });
        h.runAtTickTime(65, () -> { body(p, BodyTypes.MORTAL_BODY); near(h, hit(enemy, enemy.damageSources().playerAttack(p), 20), 20, "Body switch clears dodge bonus"); p.discard(); enemy.discard(); h.succeed(); });
    }

    @GameTest(template = "fate_test")
    public static void killsRestoreQiAndSolarBurningIsFullyBlocked(GameTestHelper h) {
        var p = player(h, BodyTypes.SPIRIT_DEVOURING); var enemy = target(h);
        hit(enemy, enemy.damageSources().playerAttack(p), 2000);
        h.assertTrue(ModAttachments.getData(p).qi() == 25, "Devouring kill restores 5% of 500 maximum Qi");
        body(p, BodyTypes.MYRIAD_POISON); ModAttachments.setData(p, ModAttachments.getData(p).withQi(0));
        var poisoned = target(h); poisoned.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        hit(poisoned, poisoned.damageSources().playerAttack(p), 2000);
        h.assertTrue(ModAttachments.getData(p).qi() == 5, "Poisoned kill restores 1% maximum Qi");
        body(p, BodyTypes.SOLAR_DIVINE); near(h, hit(p, p.damageSources().inFire(), 100), 0, "Solar fire immunity");
        h.assertTrue(!p.addEffect(new MobEffectInstance(ModEffects.SPIRIT_FLAME_BURN, 100)), "Special flame burn rejected");
        p.igniteForSeconds(10); BodyTypeEffects.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(!p.isOnFire(), "Solar burning extinguished");
        p.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void dayNightAndCoveredSunlightUseCorrectRegeneration(GameTestHelper h) {
        var p = player(h, BodyTypes.SUPREME_YIN);
        long previousTime = h.getLevel().getDayTime();
        try {
            p.setPos(p.getX(), 319, p.getZ());
            h.getLevel().setDayTime(6000); h.getLevel().updateSkyBrightness();
            h.assertTrue(BodyTypeEffects.sunlight(p), "Clear sky grants sunlight at midday");
            near(h, BodyTypeEffects.qiRegeneration(p, 100), 85, "Yin Qi regeneration reduced in sunlight");
            p.setPos(h.absoluteVec(new Vec3(1, 2, 1))); h.getLevel().setBlockAndUpdate(p.blockPosition().above(3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            h.assertTrue(!BodyTypeEffects.sunlight(p), "Roof blocks sunlight");
            near(h, BodyTypeEffects.qiRegeneration(p, 100), 100, "Covered daytime avoids sunlight penalty");
            h.getLevel().setDayTime(18000); h.getLevel().updateSkyBrightness();
            near(h, BodyTypeEffects.qiRegeneration(p, 100), 120, "Yin Qi regeneration increased at night");
            body(p, BodyTypes.SOLAR_DIVINE); p.setPos(p.getX(), 319, p.getZ());
            near(h, BodyTypeEffects.naturalHealing(p, 10), 10, "Solar regeneration has no bonus at night");
            h.getLevel().setDayTime(6000); h.getLevel().updateSkyBrightness();
            near(h, BodyTypeEffects.naturalHealing(p, 10), 12, "Solar regeneration increased in sunlight");
            p.setHealth(500); p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(6);
            for (int i = 0; i < 10; i++) p.getFoodData().tick(p);
            near(h, p.getHealth(), 501.2, "Solar sunlight bonus reaches actual natural healing");
        } finally { h.getLevel().setDayTime(previousTime); h.getLevel().updateSkyBrightness(); p.discard(); }
        h.succeed();
    }
}
