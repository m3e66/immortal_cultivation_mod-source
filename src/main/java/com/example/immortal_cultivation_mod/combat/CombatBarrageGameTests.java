package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class CombatBarrageGameTests {
    private static final class Fighter extends FakePlayer {
        final List<Long> attacks = new ArrayList<>();
        Fighter(GameTestHelper h) {
            super(h.getLevel(), new GameProfile(UUID.randomUUID(), "barrage-test"));
            setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3, 15, 3)));
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            CombatStateManager.toggleManualCombat(this);
        }
        @Override public void attack(Entity target) {
            attacks.add(serverLevel().getGameTime());
            super.attack(target);
        }
    }

    @GameTest(template = "fate_test", timeoutTicks = 90)
    public static void doubleClickSchedulesExactlyTenRealAttacksInThreeSeconds(GameTestHelper h) {
        var p = new Fighter(h);
        var target = EntityType.PIG.create(h.getLevel());
        target.setPos(p.position().add(0, 0, 2));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        h.getLevel().addFreshEntity(target);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, .4, 0));
        CombatStateManager.performNormalAttack(p, target.getId());
        h.assertTrue(p.attacks.size() == 1 && !CombatBarrage.active(p), "Single click remains an ordinary attack");
        CombatStateManager.performNormalAttack(p, target.getId());
        h.assertTrue(CombatBarrage.active(p), "Second normal click begins the barrage");
        p.attacks.clear();
        long started = h.getLevel().getGameTime();
        h.onEachTick(() -> {
            // Keep this timing fixture aimed and in reach despite ordinary melee knockback.
            target.setPos(p.position().add(0, 0, 2));
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            CombatStateManager.tick(p);
            if (h.getLevel().getGameTime() - started < 60) {
                CombatStateManager.performNormalAttack(p, target.getId());
            }
        });
        h.runAfterDelay(62, () -> {
            h.assertTrue(p.attacks.size() == 10 && !CombatBarrage.active(p), "Spam cannot add hits or extend a barrage: hits=" + p.attacks + ", active=" + CombatBarrage.active(p));
            for (int i = 0; i < 10; i++) {
                h.assertTrue(p.attacks.get(i) - started == (i + 1) * 6L,
                        "Hits occur every six ticks, with tenth hit at three seconds");
            }
            h.assertTrue(target.getHealth() < 1000, "Scheduled attacks use real weapon damage");
            CombatStateManager.clear(p);
            target.discard();
            h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void blockingLogoutAndSameTypeWeaponReplacementCancelBarrage(GameTestHelper h) {
        var p = new Fighter(h);
        CombatStateManager.performNormalAttack(p, -1);
        CombatStateManager.performNormalAttack(p, -1);
        h.assertTrue(CombatBarrage.active(p), "Valid combat clicks start barrage");
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        CombatStateManager.tick(p);
        h.assertTrue(!CombatBarrage.active(p), "Replacing weapon with same item type still cancels barrage");
        CombatStateManager.performNormalAttack(p, -1);
        CombatStateManager.performNormalAttack(p, -1);
        CombatStateManager.setBlocking(p, true);
        h.assertTrue(!CombatBarrage.active(p), "Guard input cancels immediately");
        CombatStateManager.setBlocking(p, false);
        CombatStateManager.performNormalAttack(p, -1);
        CombatStateManager.performNormalAttack(p, -1);
        CombatStateManager.onLogout(p);
        h.assertTrue(!CombatBarrage.active(p), "Logout removes queued attacks");
        CombatStateManager.clear(p);
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 30)
    public static void separatedClicksAndDisabledCombatCannotTriggerBarrage(GameTestHelper h) {
        var p = new Fighter(h);
        CombatStateManager.performNormalAttack(p, -1);
        h.runAfterDelay(8, () -> {
            CombatStateManager.performNormalAttack(p, -1);
            h.assertTrue(!CombatBarrage.active(p), "Clicks outside double-click window remain ordinary attacks");
            CombatStateManager.setEnabled(p, false);
            CombatStateManager.performNormalAttack(p, -1);
            CombatStateManager.performNormalAttack(p, -1);
            h.assertTrue(!CombatBarrage.active(p), "Disabled combat cannot queue attacks");
            CombatStateManager.clear(p);
            h.succeed();
        });
    }
}
