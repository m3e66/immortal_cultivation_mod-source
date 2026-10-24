package com.example.immortal_cultivation_mod.combat;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.spell.CultivatorSpellExecution;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.HealingTechniques;

@GameTestHolder("immortal_combat_flow")
@PrefixGameTestTemplate(false)
public final class CombatFlowGameTests {
    @GameTest(template="fate_test",timeoutTicks=160) public static void npcSavesOpeningQiForSpells(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var target=EntityType.COW.create(h.getLevel());
        npc.setPos(h.absoluteVec(new Vec3(3,40,3)));target.setPos(npc.position().add(1,0,0));
        NpcProfile.ensure(npc,npc.npcArchetype());npc.tickCount=20;npc.setTarget(target);
        var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");
        var spells=new net.minecraft.nbt.ListTag();spells.add(net.minecraft.nbt.StringTag.valueOf(ModSpells.FIREBALL));profile.put("Spells",spells);
        profile.putInt("Qi",profile.getInt("MaxQi"));
        int initial=NpcProfile.qi(npc,npc.npcArchetype());
        QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
        h.assertTrue(!QiDomainSystem.blasting(npc),"Acquiring a target must not start Blast Qi");
        h.assertTrue(NpcProfile.qi(npc,npc.npcArchetype())==initial,"Opening consumes no aura Qi");
        h.runAfterDelay(99,()->{
            QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"Opening delay lasts five seconds");
        });
        h.runAfterDelay(101,()->{
            try {
                QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
                h.assertTrue(QiDomainSystem.blasting(npc),"Surplus Qi permits a later combat burst");
                int reserve=QiDomainSystem.npcSpellReserve(npc,npc);
                profile.putInt("Qi",reserve);QiDomainSystem.view(npc).lastUpkeep=Long.MIN_VALUE;
                QiDomainSystem.tickAll();
                h.assertTrue(!QiDomainSystem.blasting(npc),"Casting down to the reserve stops upkeep");
                h.assertTrue(NpcProfile.qi(npc,npc.npcArchetype())==reserve,"Aura leaves spell reserve untouched");
                h.assertTrue(NpcProfile.spendQi(npc,npc.npcArchetype(),ModSpells.get(ModSpells.FIREBALL).qiCost()),
                        "Spells may still spend the protected reserve");
                npc.setTarget(null);QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
                h.assertTrue(!npc.getPersistentData().contains(QiDomainSystem.NPC_COMBAT_READY_AT),"Leaving combat clears opening timer");
                h.succeed();
            } finally {QiDomainSystem.stop(npc);npc.discard();target.discard();}
        });
    }

    @GameTest(template="fate_test") public static void npcBudgetProtectsCostlyKnownSpells(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var target=EntityType.COW.create(h.getLevel());
        try {
            NpcProfile.ensure(npc,npc.npcArchetype());npc.tickCount=20;npc.setTarget(target);
            var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");
            var spells=new net.minecraft.nbt.ListTag();spells.add(net.minecraft.nbt.StringTag.valueOf(ModSpells.TIANYUN_FENCHENG));profile.put("Spells",spells);
            int max=NpcProfile.maxQi(npc,npc.npcArchetype());
            int reserve=QiDomainSystem.npcSpellReserve(npc,npc);
            h.assertTrue(reserve>=Math.ceil(max*.4) && reserve>=NpcSpellcasting.qiCost(npc,ModSpells.TIANYUN_FENCHENG),
                    "Reserve covers both baseline and one costly known spell");
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);
            profile.putInt("Qi",reserve);QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"Reserve alone cannot fund Blast Qi");
            h.assertTrue(NpcProfile.qi(npc,npc.npcArchetype())==reserve,"Rejected burst costs nothing");
        } finally {QiDomainSystem.stop(npc);npc.discard();target.discard();}
        h.succeed();
    }
    @GameTest(template="fate_test") public static void spellProjectilesCatchWiderContacts(GameTestHelper h) {
        var target=h.spawn(EntityType.COW,new BlockPos(4,4,4));target.setNoAi(true);
        var caster=player(h);
        var spell=new LingzhiBulletProjectileEntity(h.getLevel(),caster);
        Vec3 start=target.position().add(-3,1,.80);
        Vec3 end=start.add(6,0,0);
        spell.setPos(start);
        var bounds=new net.minecraft.world.phys.AABB(start,end).inflate(1);
        h.assertTrue(spell.getBbWidth()>=.8F,"Spell dimensions are enlarged");
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                h.getLevel(),spell,start,end,bounds,e -> e==target,.3F);
        h.assertTrue(hit!=null && hit.getEntity()==target,"Expanded spell contact catches a grazing target");
        var arrow=new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW,h.getLevel());
        h.assertTrue(net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                h.getLevel(),arrow,start,end,bounds,e -> e==target,.3F)==null,
                "Vanilla arrow contact radius stays unchanged");
        // The collision ray must still end at the wall, before a target farther behind it.
        start=target.position().add(-3,1,0);spell.setPos(start);spell.setDeltaMovement(6,0,0);
        h.setBlock(new BlockPos(2,5,4),Blocks.STONE);
        h.assertTrue(net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(
                spell,e -> e==target).getType()==net.minecraft.world.phys.HitResult.Type.BLOCK,
                "A wall blocks the enlarged projectile");
        spell.discard();arrow.discard();target.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void healingTiersShareSelfHealing(GameTestHelper h) {
        var spells = new String[]{ModSpells.REGENERATION, ModSpells.MULING_SHENGXI,
                ModSpells.XUE_GUIYUAN_SHU, ModSpells.SHENGXI_BAOFA};
        var actors = new net.minecraft.world.entity.animal.Cow[4];
        var stages = new int[]{2,5,6,6};
        var scales = new float[]{1,1.10F,1.11F,1.11F};
        h.assertTrue(Math.abs(HealingTechniques.realmHealingScale(3)-1.01F)<.0001F,
                "One small stage adds 0.01");
        var target = h.spawn(EntityType.COW, new BlockPos(8,4,8));
        target.setNoAi(true); target.setHealth(5);
        for (int i=0;i<4;i++) {
            var actor = h.spawn(EntityType.COW,new BlockPos(2+i,4,4));
            actors[i]=actor;actor.setNoAi(true);
            actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);actor.setHealth(100);
            boolean cast = i==0
                    ? CultivatorSpellExecution.applyRegeneration(actor)
                    : HealingTechniques.cast(actor,target,spells[i], ignored -> {
                        h.fail("Self healing must not spawn a projectile");
                    },false);
            h.assertTrue(cast,"Wounded caster can heal");
            var effect=actor.getEffect(ModEffects.SPRING_RECOVERY);
            h.assertTrue(effect!=null && effect.getAmplifier()==stages[i] && effect.getDuration()==20,
                    "Each tier shares the one-second healing effect");
            h.assertTrue(!CultivatorSpellExecution.canApplyRegeneration(actor),"Active heal cannot stack");
        }
        h.runAfterDelay(2, () -> {
            float base=actors[0].getHealth()-100;
            h.assertTrue(base>0,"Base spell actually heals");
            for(int i=1;i<4;i++) h.assertTrue(Math.abs(actors[i].getHealth()-100-base*scales[i])<.01,
                    "Healing scales by spell realm, with equal realms healing equally");
            h.assertTrue(target.getHealth()==5,"Supplied target receives no healing");
            for(var actor:actors)actor.discard();target.discard();h.succeed();
        });
    }
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"combat-test"));
        p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new Vec3(4.5,4,4.5)));p.setOnGround(true);
        return p;
    }
    @GameTest(template="fate_test") public static void confirmedComboControlsActualMotion(GameTestHelper h) {
        var p=player(h);var t=h.spawn(EntityType.COW,new BlockPos(4,4,6));t.setNoAi(true);t.setOnGround(true);
        t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);t.setHealth(100);
        t.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(t.hurt(t.damageSources().playerAttack(p),2),"First contact deals damage");
        CombatKnockback.tick(new EntityTickEvent.Pre(t));
        h.assertTrue(Math.abs(t.getDeltaMovement().horizontalDistance()-.25)<.001,"Opening hit uses .25, without vanilla stacking");
        t.invulnerableTime=0;t.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(t.hurt(t.damageSources().playerAttack(p),2),"Second contact deals damage");
        CombatKnockback.tick(new EntityTickEvent.Pre(t));
        h.assertTrue(Math.abs(t.getDeltaMovement().horizontalDistance()-.01)<.001,"Combo keeps target close");
        t.invulnerableTime=0;t.setDeltaMovement(Vec3.ZERO);
        CombatStateManager.hurtAsHeavy(p,t,t.damageSources().playerAttack(p),2);
        CombatKnockback.tick(new EntityTickEvent.Pre(t));
        h.assertTrue(Math.abs(t.getDeltaMovement().horizontalDistance()-1.5)<.001,"Heavy separates targets");
        t.discard();CombatExchange.reset(p);h.succeed();
    }
    @GameTest(template="fate_test") public static void blockAndTargetChangeResetCombo(GameTestHelper h) {
        var p=player(h);var a=h.spawn(EntityType.COW,new BlockPos(4,4,6));var b=h.spawn(EntityType.COW,new BlockPos(6,4,6));
        h.assertTrue(CombatExchange.confirm(p,a,false)==.25&&CombatExchange.confirm(p,a,false)==.01,"Same-target chain");
        h.assertTrue(CombatExchange.confirm(p,b,false)==.25,"Changing target resets");
        CombatFeedbackEvents.blocked(b,b.damageSources().playerAttack(p),4);
        h.assertTrue(CombatExchange.confirm(p,b,false)==.25,"Guard breaks chain");
        a.discard();b.discard();CombatExchange.reset(p);h.succeed();
    }
    private static void floor(GameTestHelper h) {
        for(int x=0;x<10;x++)for(int z=0;z<10;z++) {
            h.setBlock(new BlockPos(x,3,z),Blocks.STONE);
            for(int y=4;y<7;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
    }
    @GameTest(template="fate_test") public static void footworkHasCollisionCooldownAndStunChecks(GameTestHelper h) {
        floor(h);var actor=h.spawn(EntityType.ZOMBIE,new BlockPos(4,4,4));actor.setNoAi(true);actor.setOnGround(true);
        actor.addEffect(new MobEffectInstance(ModEffects.DAZE,100));
        h.assertTrue(!CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Stun blocks step");actor.removeEffect(ModEffects.DAZE);
        h.setBlock(new BlockPos(4,4,5),Blocks.STONE);
        h.assertTrue(!CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Wall blocks route");h.setBlock(new BlockPos(4,4,5),Blocks.AIR);
        h.assertTrue(CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Clear route steps");
        h.assertTrue(!CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Repeated packets cannot bypass cooldown");
        actor.getPersistentData().putLong("CombatStepReady",0);actor.getPersistentData().putInt("CombatStepChain",3);
        h.assertTrue(!CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Three-step exhaustion");
        actor.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void footworkCannotStepAcrossPit(GameTestHelper h) {
        floor(h);var actor=h.spawn(EntityType.ZOMBIE,new BlockPos(4,4,4));actor.setNoAi(true);actor.setOnGround(true);
        h.setBlock(new BlockPos(4,3,5),Blocks.AIR);
        h.assertTrue(!CombatFootwork.step(actor,new Vec3(0,0,1),.66),"Unsafe ground rejects movement");actor.discard();h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=60) public static void sharedProjectileActuallyTravelsAndHits(GameTestHelper h) {
        var caster=h.spawn(EntityType.ZOMBIE,new BlockPos(2,5,4));caster.setNoAi(true);caster.setNoGravity(true);
        var target=h.spawn(EntityType.COW,new BlockPos(7,5,4));target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        h.assertTrue(CultivatorSpellExecution.lingzhiBullet(caster,target,1),"Shared actor path launches custom projectile");
        var list=h.getLevel().getEntitiesOfClass(LingzhiBulletProjectileEntity.class,caster.getBoundingBox().inflate(3));
        h.assertTrue(list.size()==1&&list.getFirst().getOwner()==caster,"One authoritative projectile with correct owner");
        h.assertTrue(target.getHealth()==100,"No instant damage before arrival");
        h.runAfterDelay(12,()-> {h.assertTrue(target.getHealth()<100,"Collision applies actual damage");
            h.assertTrue(list.getFirst().isRemoved(),"Contact cleans projectile");caster.discard();target.discard();h.succeed();});
    }

    @GameTest(template="fate_test",timeoutTicks=50) public static void playerStrikeHasCancelableContactWindow(GameTestHelper h) {
        floor(h);var p=player(h);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                com.example.immortal_cultivation_mod.item.ModItems.DAO.toStack());
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);
        CombatStateManager.enterCombat(p);
        var target=h.spawn(EntityType.COW,new BlockPos(4,4,6));target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        CombatStateManager.performNormalAttack(p,target.getId());
        h.assertTrue(target.getHealth()==100,"Animation precedes damage");
        h.runAfterDelay(3,()-> {
            CombatStateManager.tick(p);
            h.assertTrue(target.getHealth()<100,"Contact window delivers actual damage");
        });
        h.runAfterDelay(12,()-> {
            target.discard();
            var fresh=h.spawn(EntityType.COW,new BlockPos(4,4,6));fresh.setNoAi(true);
            float health=fresh.getHealth();
            CombatStateManager.performNormalAttack(p,fresh.getId());
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(p,
                    com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(p).withThoughts(0));
            p.addEffect(new MobEffectInstance(ModEffects.DAZE,20));
            h.assertTrue(CombatStateManager.isSpellcastingBlocked(p),"Daze blocks attacker");
            CombatStateManager.tick(p);
            h.runAfterDelay(3,()-> {
                CombatStateManager.tick(p);
                h.assertTrue(fresh.getLastHurtByMob()!=p && fresh.getHealth()>=health,"Stun cancels pending attack: before="+health+", after="+fresh.getHealth()+", attacker="+fresh.getLastHurtByMob());
                CombatStateManager.onLogout(p);fresh.discard();h.succeed();
            });
        });
    }

    @GameTest(template="npc_combat_arena") public static void npcProjectileReleaseSpendsOnce(GameTestHelper h) {
        NpcCombatGameTests.successfulReleaseSpendsOnceAndTagsActualProjectile(h);
    }
    @GameTest(template="npc_combat_arena") public static void npcStunCancelsSpellWindup(GameTestHelper h) {
        NpcCombatGameTests.canceledWindupDoesNotSpendQiOrRevealSpell(h);
    }
}
