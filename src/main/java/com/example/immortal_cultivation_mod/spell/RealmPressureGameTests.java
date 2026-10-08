package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class RealmPressureGameTests {
    @GameTest(template = "fate_test")
    public static void pressureGrowsWithEveryStageAndCostsTwoPercent(GameTestHelper h) {
        double previous = 0;
        for (String level : CultivationLevels.allLevels()) {
            int stage = CultivationLevels.getStageIndex(level);
            double radius = Weiya.radius(level);
            if (stage == 0) {
                h.assertTrue(radius == 0, "Mortals have no pressure range");
                continue;
            }
            h.assertTrue(radius > previous && radius <= 64, "Every stage increases a bounded pressure range");
            previous = radius;
            var data = ModAttachments.CultivationData.createDefault().withCultivationLevel(level);
            int maxQi = CultivationLevels.getLevelDef(level).maxQi();
            h.assertTrue(Weiya.qiCostPerSecond(data) == Math.max(1, (int)Math.ceil(maxQi * .02)),
                    "Pressure costs two percent of maximum Qi per second");
        }
        h.assertTrue(previous == 64 && Weiya.radiusForStage(Integer.MAX_VALUE) == 64,
                "Highest realm and invalid oversized stages cannot create unbounded fields");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void pressureUsesExactRadiusAndRespectsHigherRealms(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(16, 4, 16));
        Vec3 center = Vec3.atBottomCenterOf(origin);
        var inside = EntityType.ZOMBIE.create(level);
        var outside = EntityType.ZOMBIE.create(level);
        var corner = EntityType.ZOMBIE.create(level);
        var stronger = ModEntities.ZHUJI_CULTIVATOR.get().create(level);
        inside.setPos(center.add(11.99, 0, 0)); outside.setPos(center.add(12.01, 0, 0));
        corner.setPos(center.add(9, 0, 9)); stronger.setPos(center.add(1, 0, 0));
        for (var mob : java.util.List.of(inside, outside, corner, stronger)) {
            mob.setNoAi(true); level.addFreshEntity(mob);
        }
        try {
            Weiya.applyAmbientPressure(level, origin, CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY);
            h.assertTrue(inside.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Weak creature inside range is visibly suppressed");
            h.assertTrue(!outside.hasEffect(ModEffects.WEIYA_SUPPRESSED) && !corner.hasEffect(ModEffects.WEIYA_SUPPRESSED),
                    "Exact radius excludes points outside the sphere, including old AABB corners");
            h.assertTrue(!stronger.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Higher realm resists lower pressure");
        } finally { inside.discard(); outside.discard(); corner.discard(); stronger.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void pressureTogglePaysOnceAndTimedPressureExpires(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "PressureTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(2, 3, 2)));
        String realm = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
        var data = ModAttachments.getData(player).withCultivationLevel(realm).withQi(1000000);
        ModAttachments.setData(player, data);
        int cost = Weiya.qiCostPerSecond(data);
        Weiya.toggle(player);
        h.assertTrue(player.hasEffect(ModEffects.WEIYA)
                        && player.getEffect(ModEffects.WEIYA).getAmplifier() == CultivationLevels.getStageIndex(realm),
                "Synced pressure effect includes stage for matching client radius");
        h.assertTrue(ModAttachments.getData(player).qi() == 1000000 - cost, "Activating pressure pays one new lower cost");
        Weiya.toggle(player);
        h.assertTrue(!player.hasEffect(ModEffects.WEIYA), "Toggling off removes pressure immediately");
        Weiya.activateTimed(player, 1);
        h.runAfterDelay(2, () -> {
            Weiya.tick(player);
            h.assertTrue(!player.hasEffect(ModEffects.WEIYA), "Expired timed pressure clears its effect and client field");
            int qi = ModAttachments.getData(player).qi();
            player.tickCount = 20;
            Weiya.tick(player);
            h.assertTrue(ModAttachments.getData(player).qi() == qi, "Stopped pressure cannot spend again");
            Weiya.clear(player); player.discard(); h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void rainCeilingKeepsWeatherOutsideThePressureDome(GameTestHelper h) {
        Vec3 center = new Vec3(10, 70, -4);
        double radius = Weiya.radiusForStage(1);
        h.assertTrue(Weiya.rainCeiling(center, radius, 10, -4) == 82, "Rain stops at the dome above its caster");
        h.assertTrue(Weiya.rainCeiling(center, radius, 22, -4) == 70, "Rain dome meets the exact horizontal field edge");
        h.assertTrue(Weiya.rainCeiling(center, radius, 22.01, -4) == Double.NEGATIVE_INFINITY,
                "Rain beyond pressure radius is not changed");
        double top = Weiya.rainCeiling(center, radius, 16, -4);
        h.assertTrue(top > 70 && top < 82 && Math.abs(center.distanceToSqr(new Vec3(16, top, -4)) - radius * radius) < 1e-7,
                "Intermediate rain columns follow the spherical dome");
        h.assertTrue(Weiya.rainCeiling(center, 0, 10, -4) == Double.NEGATIVE_INFINITY,
                "Inactive or mortal fields do not suppress rain");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rainIsHeldThenFallsWhenPressureReleases(GameTestHelper h) {
        Vec3 origin = new Vec3(3, 80, 7);
        var step = new com.example.immortal_cultivation_mod.effect.PressureRainMotion.Step(origin, -.5);
        for (int i=0;i<100;i++)
            step = com.example.immortal_cultivation_mod.effect.PressureRainMotion.advance(step.position(), step.velocity(), true);
        h.assertTrue(step.position().equals(origin) && step.velocity()==0,
                "Active pressure keeps rain visible at its exact world position, without downward drift");
        for (int i=0;i<10;i++)
            step = com.example.immortal_cultivation_mod.effect.PressureRainMotion.advance(step.position(), step.velocity(), false);
        h.assertTrue(step.position().y < origin.y-1 && step.velocity()<0,
                "Released rain resumes falling under gravity");
        Vec3 released = step.position();
        step = com.example.immortal_cultivation_mod.effect.PressureRainMotion.advance(released,step.velocity(),true);
        h.assertTrue(step.position().equals(released) && step.velocity()==0,
                "A moving or reactivated field catches an already falling drop");
        h.succeed();
    }
}
