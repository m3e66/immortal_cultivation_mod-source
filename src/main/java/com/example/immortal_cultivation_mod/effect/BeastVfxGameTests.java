package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity;
import com.example.immortal_cultivation_mod.network.BeastVfxPayloads;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_vfx")
@PrefixGameTestTemplate(false)
public final class BeastVfxGameTests {
    private static CultivationBestiaryBeastEntity rat(GameTestHelper h) {
        var rat=ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get().create(h.getLevel());
        h.assertTrue(rat!=null,"Rat creates on dedicated server without loading client VFX classes");
        rat.setNoAi(true);return rat;
    }
    private static Pig pig(GameTestHelper h) {
        var pig=EntityType.PIG.create(h.getLevel());pig.setNoAi(true);return pig;
    }
    @GameTest(template="fate_test")
    public static void rejectedHitsEmitNoBeastImpact(GameTestHelper h) {
        var rat=rat(h);var pig=pig(h);pig.setInvulnerable(true);
        boolean accepted=pig.hurt(pig.damageSources().mobAttack(rat),2);
        h.assertTrue(!accepted,"Invulnerable target rejects attack");
        h.assertTrue(BeastVfxPayloads.emittedCount(rat)==0,"Rejected damage produces no beast impact or residue");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void successfulHitEmitsOneConfirmedImpact(GameTestHelper h) {
        var rat=rat(h);var pig=pig(h);float before=pig.getHealth();
        h.assertTrue(pig.hurt(pig.damageSources().mobAttack(rat),2),"Real mob damage accepted");
        h.assertTrue(pig.getHealth()<before,"Damage applied");
        h.assertTrue(BeastVfxPayloads.emittedCount(rat)==1,"Exactly one event, no despawn-derived duplicate");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void crowdedAttacksKeepDamageWhenVfxBudgetIsFull(GameTestHelper h) {
        var rat=rat(h);
        for(int i=0;i<8;i++){
            var pig=pig(h);float before=pig.getHealth();
            h.assertTrue(pig.hurt(pig.damageSources().mobAttack(rat),2),"Victim "+i+" receives damage");
            h.assertTrue(pig.getHealth()<before,"VFX cap cannot suppress victim "+i+" damage");
        }
        h.assertTrue(BeastVfxPayloads.emittedCount(rat)==4,"At most four impact packets per source per tick");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void cancelledProjectileImpactCannotCreateResidue(GameTestHelper h) {
        var rat=rat(h);
        var shot=ModEntities.CULTIVATION_BEAST_PROJECTILE.get().create(h.getLevel());
        shot.configure(rat,com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity.Kind.WIND_FEATHER,2);
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(1,1,1),
                net.minecraft.core.Direction.UP,new net.minecraft.core.BlockPos(1,1,1),false);
        var event=new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(shot,hit);
        event.setCanceled(true);
        BeastVfxPayloads.Events.block(event);
        h.assertTrue(BeastVfxPayloads.emittedCount(rat)==0,"Cancelled/forcefield-intercepted projectiles leave no impact residue");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualBlockContactCreatesOneImpact(GameTestHelper h) {
        var rat=rat(h);
        var shot=ModEntities.CULTIVATION_BEAST_PROJECTILE.get().create(h.getLevel());
        shot.configure(rat,com.example.immortal_cultivation_mod.entity.CultivationBeastProjectileEntity.Kind.WIND_FEATHER,2);
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(1,1,1),
                net.minecraft.core.Direction.UP,new net.minecraft.core.BlockPos(1,1,1),false);
        BeastVfxPayloads.Events.block(new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(shot,hit));
        h.assertTrue(BeastVfxPayloads.emittedCount(rat)==1,"Real block contact produces one impact");
        h.succeed();
    }
}
