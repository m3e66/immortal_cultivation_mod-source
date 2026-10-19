package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads;
import com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.Kind;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class CombatFeedbackGameTests {
    @GameTest(template = "fate_test")
    public static void spellElementsSurviveGenericDamageTypesWithoutLeaking(GameTestHelper h) {
        var target = EntityType.PIG.create(h.getLevel());
        var caster = EntityType.ZOMBIE.create(h.getLevel());
        target.setPos(h.absoluteVec(new Vec3(2, 2, 2))); caster.setPos(target.position().add(0, 0, -2));
        var lightning = new com.example.immortal_cultivation_mod.entity.LightningProjectileEntity(h.getLevel(), caster, false);
        var ice = new com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity(h.getLevel(), caster,
                com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity.Kind.BINGZHUI_YU);
        var flame = new com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity(h.getLevel(), caster,
                com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity.Kind.YANBAO_DAN);
        h.assertTrue(CombatFeedbackEvents.injuryFlags(target, target.damageSources().thrown(lightning, caster))
                        == CombatFeedbackPayloads.LIGHTNING,
                "Lightning thrown damage creates electrical injuries, not physical blood");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(target, target.damageSources().thrown(ice, caster))
                        == CombatFeedbackPayloads.FROST,
                "Ice projectile metadata applies frost before its status effect is added");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(target, target.damageSources().thrown(flame, caster))
                        == CombatFeedbackPayloads.FIRE,
                "Fire projectile metadata applies burn without misleading blood particles");
        var magic = target.damageSources().indirectMagic(caster, caster);
        CombatFeedbackEvents.hurtWithFlags(target, magic, 2, CombatFeedbackPayloads.LIGHTNING);
        h.assertTrue((CombatFeedbackEvents.lastInjuryFlags(target) & CombatFeedbackPayloads.LIGHTNING) != 0,
                "Caster-only lightning damage reaches the confirmed-hit payload with its element");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(target, magic) == CombatFeedbackPayloads.AURA,
                "Temporary damage classification cannot leak to later generic magic attacks");
        target.setInvulnerable(true);
        CombatFeedbackEvents.hurtWithFlags(target, magic, 2, CombatFeedbackPayloads.FIRE);
        h.assertTrue(CombatFeedbackEvents.emittedCount(target) == 1
                        && CombatFeedbackEvents.injuryFlags(target, magic) == CombatFeedbackPayloads.AURA,
                "Rejected typed spell damage emits nothing and still clears its temporary flags");
        target.discard(); caster.discard(); lightning.discard(); ice.discard(); flame.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void weakQiFeedbackUsesServerPlayerAndNpcState(GameTestHelper h) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "WeakQiVfx"));
        var realm = com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_ZHUJI
                + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY;
        var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player).withCultivationLevel(realm);
        int maximum = com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(realm).maxQi();
        int threshold = (int)Math.ceil(maximum * .15);
        com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, data.withQi(threshold));
        var source = player.damageSources().magic();
        h.assertTrue((CombatFeedbackEvents.injuryFlags(player, source) & CombatFeedbackPayloads.LOW_QI) == 0,
                "Fifteen percent Qi is not weakened");
        com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, data.withQi(threshold - 1));
        h.assertTrue((CombatFeedbackEvents.injuryFlags(player, source) & CombatFeedbackPayloads.LOW_QI) != 0,
                "Remote players below fifteen percent carry authoritative weak-Qi feedback");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(player, player.damageSources().fall()) == 0,
                "Low Qi never turns environmental damage into weapon impacts");
        var npc = com.example.immortal_cultivation_mod.entity.ModEntities.ZHUJI_CULTIVATOR.get().create(h.getLevel());
        int npcMaximum = npc.npcMaxQi();
        com.example.immortal_cultivation_mod.entity.NpcProfile.drainQi(npc, npc.npcArchetype(), npcMaximum);
        h.assertTrue((CombatFeedbackEvents.injuryFlags(npc, source) & CombatFeedbackPayloads.LOW_QI) != 0,
                "Depleted NPCs carry the same weak-Qi flag");
        com.example.immortal_cultivation_mod.entity.NpcProfile.restoreQi(npc, npc.npcArchetype(), npcMaximum);
        h.assertTrue((CombatFeedbackEvents.injuryFlags(npc, source) & CombatFeedbackPayloads.LOW_QI) == 0,
                "Restored NPC Qi stops weak feedback");
        var packet = new CombatFeedbackPayloads.Effect(h.getLevel().dimension().location(), Kind.HIT,
                player.getId(), Vec3.ZERO, new Vec3(0,0,1), .5F, CombatFeedbackPayloads.LOW_QI | 128, 5);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            CombatFeedbackPayloads.Effect.STREAM_CODEC.encode(buffer, packet);
            var decoded = CombatFeedbackPayloads.Effect.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.injuryFlags() == CombatFeedbackPayloads.LOW_QI,
                    "Weak-Qi bit survives packet round trip while unknown bits are discarded");
        } finally { buffer.release(); player.discard(); npc.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void severityAndInjuryFlagsFollowRealDamage(GameTestHelper h) {
        h.assertTrue(CombatFeedbackProfile.hitIntensity(0, 20) == 0
                && CombatFeedbackProfile.hitIntensity(Float.NaN, 20) == 0, "No effect severity for invalid or zero damage");
        h.assertTrue(CombatFeedbackProfile.hitIntensity(4, 20) < .45F
                && CombatFeedbackProfile.heavy(CombatFeedbackProfile.hitIntensity(50, 10000))
                && CombatFeedbackProfile.massive(CombatFeedbackProfile.hitIntensity(400, 10000)),
                "Normal, heavy and massive actual damage produce distinct intensity bands even on cultivation health");
        float previous = 0;
        for (float damage : new float[]{1, 4, 15, 50, 100, 400, 10000, Float.MAX_VALUE}) {
            float value = CombatFeedbackProfile.hitIntensity(damage, 10000);
            h.assertTrue(value >= previous && value <= 1, "Severity is monotonic and bounded"); previous = value;
        }
        var pig = EntityType.PIG.create(h.getLevel());
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        h.assertTrue(CombatFeedbackEvents.injuryFlags(pig, pig.damageSources().mobAttack(zombie)) == CombatFeedbackPayloads.PHYSICAL,
                "Physical damage has blood flag");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(pig, pig.damageSources().onFire()) == CombatFeedbackPayloads.FIRE,
                "Burn damage has fire flag, not blood");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(pig, pig.damageSources().freeze()) == CombatFeedbackPayloads.FROST,
                "Freeze damage has frost flag");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(pig, pig.damageSources().lightningBolt()) == CombatFeedbackPayloads.LIGHTNING,
                "Lightning damage has electrical flag");
        h.assertTrue(CombatFeedbackEvents.injuryFlags(pig, pig.damageSources().fall()) == 0,
                "Falling uses ground contact, not weapon hit flash");
        pig.discard(); zombie.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void confirmedHitsAndBlocksDoNotInventDuplicateImpacts(GameTestHelper h) {
        var pig = EntityType.PIG.create(h.getLevel());
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        pig.setPos(h.absoluteVec(new Vec3(2, 2, 2))); zombie.setPos(pig.position().add(0, 0, -2));
        var source = pig.damageSources().mobAttack(zombie);
        pig.hurt(source, 0);
        pig.setInvulnerable(true); pig.hurt(source, 4); pig.setInvulnerable(false);
        h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 0, "Zero damage and rejected attacks produce no hit VFX");
        pig.hurt(source, 4);
        h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "Actual applied hit emits feedback");
        pig.invulnerableTime = 0; pig.hurt(source, 2);
        h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "Multiple hits on same entity in one tick stay bounded");
        var block = new LivingShieldBlockEvent(pig, new DamageContainer(source, 3), false);
        CombatFeedbackEvents.shieldBlock(block);
        block.setBlocked(true); block.setCanceled(true); CombatFeedbackEvents.shieldBlock(block);
        h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "Failed and canceled block events never flash");
        block.setCanceled(false); CombatFeedbackEvents.shieldBlock(block);
        CombatFeedbackEvents.shieldBlock(block);
        h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 2, "Successful block emits once independently of hit");
        h.assertTrue(CombatFeedbackEvents.incomingDirection(pig, source).z > .5,
                "Impact direction travels away from attacker, through target");
        pig.discard(); zombie.discard(); h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 40)
    public static void landingAndDashRequireRealTransitions(GameTestHelper h) {
        var pig = EntityType.PIG.create(h.getLevel());
        Vec3 base = h.absoluteVec(new Vec3(2, 2, 2));
        pig.setPos(base.add(0, 8, 0)); pig.setOnGround(true); pig.setNoGravity(true);
        CombatFeedbackEvents.tickMovement(pig);
        for (int i = 1; i <= 8; i++) {
            int step = i;
            h.runAfterDelay(i, () -> {
                pig.setPos(base.add(0, 8 - step, 0)); pig.setOnGround(step == 8);
                pig.setDeltaMovement(0, step == 8 ? 0 : -1, 0);
                CombatFeedbackEvents.tickMovement(pig);
            });
        }
        h.runAfterDelay(9, () -> {
            h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "One actual landing emits one ground event");
            CombatFeedbackEvents.tickMovement(pig);
            h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "Standing still never repeats landing");
            pig.setPos(base.add(30, 20, 0)); pig.setOnGround(false);
        });
        h.runAfterDelay(10, () -> CombatFeedbackEvents.tickMovement(pig));
        h.runAfterDelay(11, () -> {
            pig.setPos(base); pig.setOnGround(true); CombatFeedbackEvents.tickMovement(pig);
            h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 1, "Teleport correction cannot create a landing crater");
            CombatFeedbackEvents.dashTravel(pig, base, base.add(3, 0, 0), .5F);
            h.assertTrue(CombatFeedbackEvents.emittedCount(pig) == 3, "Explicit dash produces both start and stop contacts");
            pig.discard(); h.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 70)
    public static void fallingPlayerKeepsItsLandingGround(GameTestHelper h) {
        MovementTerrainGameTests.checkFall(h, new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "landing-test")));
    }
}
