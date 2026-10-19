package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.HutiQi;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_huti_qi")
@PrefixGameTestTemplate(false)
public final class HutiQiGameTests {
    @GameTest(template="fate_test")
    public static void recastingDismissesWithoutQiAndCannotRefillShield(GameTestHelper h) {
        try (var f=new LifeSimulationGameTests.Player(h)) {
            var p=f.player;
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(100));
            h.assertTrue(HutiQi.cast(p)&&HutiQi.isActive(p),"First cast enables shield");
            h.assertTrue(p.hasEffect(ModEffects.HUTI_QI)&&PlayerShieldManager.totalAmount(p)==20,"Active marker and shield reach client together");
            HutiQi.absorb(p,p.damageSources().generic(),8);
            h.assertTrue(PlayerShieldManager.totalAmount(p)==12,"Damage consumes original shield capacity");
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
            h.assertTrue(HutiQi.cast(p)&&!HutiQi.isActive(p),"Second cast dismisses even at zero Qi instead of refilling");
            h.assertTrue(!p.hasEffect(ModEffects.HUTI_QI)&&PlayerShieldManager.totalAmount(p)==0,"Dismissal clears marker and shield");
            h.assertTrue(!HutiQi.cast(p)&&!HutiQi.isActive(p),"Empty Qi cannot activate a fresh shield");
            HutiQi.clear(p);
        } h.succeed();
    }

    @GameTest(template="fate_test", timeoutTicks=40)
    public static void brokenShieldAutomaticallyRecastsAndCanStillBeDisabled(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var p=f.player;
        ModAttachments.setData(p,ModAttachments.getData(p).withQi(100));HutiQi.cast(p);
        h.assertTrue(HutiQi.absorb(p,p.damageSources().magic(),8)==8&&PlayerShieldManager.totalAmount(p)==20,"Magic still bypasses physical shield");
        h.assertTrue(HutiQi.absorb(p,p.damageSources().generic(),25)==5,"Overflow from breaking hit still damages the player");
        h.assertTrue(HutiQi.isActive(p)&&p.hasEffect(ModEffects.HUTI_QI),"Breaking the shield preserves toggle and its client marker");
        HutiQi.tick(p);
        h.assertTrue(PlayerShieldManager.totalAmount(p)==0&&ModAttachments.getData(p).qi()==90,"No same-tick refill or extra charge");
        h.assertTrue(HutiQi.absorb(p,p.damageSources().generic(),3)==3,"Other hits before recast cannot use a free shield");
        h.runAfterDelay(2,()->{
            try {
                HutiQi.tick(p);
                h.assertTrue(PlayerShieldManager.totalAmount(p)==20&&ModAttachments.getData(p).qi()==80,"Broken shield automatically recasts for 10 Qi");
                HutiQi.tick(p);HutiQi.tick(p);
                h.assertTrue(ModAttachments.getData(p).qi()==80,"Repeated tick cannot charge for another shield");
                HutiQi.absorb(p,p.damageSources().generic(),8);HutiQi.tick(p);
                h.assertTrue(PlayerShieldManager.totalAmount(p)==12,"Automatic recast never refills a merely damaged shield");
                HutiQi.absorb(p,p.damageSources().generic(),12);
                h.assertTrue(HutiQi.cast(p)&&!HutiQi.isActive(p),"Manual cast disables even while a recast is pending");
                HutiQi.tick(p);
                h.assertTrue(PlayerShieldManager.totalAmount(p)==0&&ModAttachments.getData(p).qi()==80,"Disabled toggle cannot rebuild or spend Qi");
                h.succeed();
            } finally {HutiQi.clear(p);f.close();}
        });
    }

    @GameTest(template="fate_test", timeoutTicks=60)
    public static void automaticRecastWaitsForQiWithoutDisablingTheToggle(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var p=f.player;
        ModAttachments.setData(p,ModAttachments.getData(p).withQi(100));HutiQi.cast(p);
        HutiQi.absorb(p,p.damageSources().generic(),20);
        ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
        h.runAfterDelay(2,()->{
            try {
                HutiQi.tick(p);
                h.assertTrue(HutiQi.isActive(p)&&p.hasEffect(ModEffects.HUTI_QI)&&PlayerShieldManager.totalAmount(p)==0,
                        "Insufficient Qi pauses protection without disabling automatic shielding");
                h.assertTrue(ModAttachments.getData(p).qi()==0,"Failed recast cannot spend or create Qi");
                ModAttachments.setData(p,ModAttachments.getData(p).withQi(10));
            }catch(Throwable error){HutiQi.clear(p);f.close();throw error;}
        });
        h.runAfterDelay(23,()->{
            try {
                HutiQi.tick(p);
                h.assertTrue(HutiQi.isActive(p)&&PlayerShieldManager.totalAmount(p)==20&&ModAttachments.getData(p).qi()==0,
                        "Restored Qi pays for a new shield without another manual cast");
                h.succeed();
            }finally{HutiQi.clear(p);f.close();}
        });
    }

    @GameTest(template="fate_test", timeoutTicks=80)
    public static void upkeepChargesOncePerSecondAndPausesWhenEmpty(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var p=f.player;
        ModAttachments.setData(p,ModAttachments.getData(p).withQi(100));HutiQi.cast(p);
        int initial=ModAttachments.getData(p).qi();
        HutiQi.tick(p);HutiQi.tick(p);
        h.assertTrue(ModAttachments.getData(p).qi()==initial,"No upkeep is charged before first second");
        h.runAfterDelay(21,()->{
            try {
                HutiQi.tick(p);int remaining=ModAttachments.getData(p).qi();
                h.assertTrue(remaining==initial-1,"One second costs one Qi");
                HutiQi.tick(p);
                h.assertTrue(ModAttachments.getData(p).qi()==remaining,"Repeated tick cannot charge twice");
                ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
            }catch(Throwable error){HutiQi.clear(p);f.close();throw error;}
        });
        h.runAfterDelay(42,()->{
            try {
                HutiQi.tick(p);
                h.assertTrue(HutiQi.isActive(p)&&p.hasEffect(ModEffects.HUTI_QI)&&PlayerShieldManager.totalAmount(p)==0,"Unpaid upkeep removes protection but leaves automatic shielding enabled");
                h.assertTrue(HutiQi.cast(p)&&!HutiQi.isActive(p),"Paused toggle can be turned off for free at zero Qi");
                h.succeed();
            }finally{HutiQi.clear(p);f.close();}
        });
    }

    @GameTest(template="fate_test")
    public static void deathClearsPendingAutomaticRecast(GameTestHelper h) {
        try (var f=new LifeSimulationGameTests.Player(h)) {
            var p=f.player;
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(100));HutiQi.cast(p);
            HutiQi.absorb(p,p.damageSources().generic(),20);
            p.setHealth(0);HutiQi.tick(p);
            h.assertTrue(!HutiQi.isActive(p)&&!p.hasEffect(ModEffects.HUTI_QI),"Death clears pending automatic recast and marker");
            p.setHealth(20);HutiQi.tick(p);
            h.assertTrue(PlayerShieldManager.totalAmount(p)==0,"Old toggle cannot recreate protection after death");
            HutiQi.clear(p);
        }
        h.succeed();
    }
}
