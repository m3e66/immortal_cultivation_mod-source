package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.SpellRibbonGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises the two late-loaded paths reported in the development client/server crashes. */
@GameTestHolder("immortal_cultivation_spell_runtime")
@PrefixGameTestTemplate(false)
public final class SpellRuntimeGameTests {
    private SpellRuntimeGameTests() {
    }

    @GameTest(template = "fate_test", timeoutTicks = 40)
    public static void lingzhiBulletDamagesTargetAndFinishesImpact(GameTestHelper h) {
        var target = EntityType.COW.create(h.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(h.absoluteVec(new Vec3(3, 4, 3)));
        h.getLevel().addFreshEntity(target);
        float initialHealth = target.getHealth();

        var projectile = new LingzhiBulletProjectileEntity(ModEntities.LINGZHI_BULLET_PROJECTILE.get(), h.getLevel());
        projectile.setPos(target.position().add(-2, 0.5, 0));
        projectile.setDeltaMovement(1, 0, 0);
        h.getLevel().addFreshEntity(projectile);
        h.runAfterDelay(8, () -> {
            h.assertTrue(target.getHealth() < initialHealth, "Lingzhi impact must load SpellDamageHelper and damage target");
            h.assertTrue(projectile.isRemoved(), "Impact must finish its ribbon payload and discard projectile");
            h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void allSpellRibbonPathsLoadAndAnimate(GameTestHelper h) {
        SpellRibbonGameTests.allRequestedSpellsHaveDistinctBoundedAnimatedPaths(h);
    }

    @GameTest(template = "fate_test")
    public static void allSpellRibbonDetailsLoadAndStayFinite(GameTestHelper h) {
        SpellRibbonGameTests.detailedSpellSurfacesAndSparksStayFinite(h);
    }
}
