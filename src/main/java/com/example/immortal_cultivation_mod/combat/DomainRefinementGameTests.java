package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class DomainRefinementGameTests {
    private static final AtomicInteger LAYERS=new AtomicInteger();
    private static final class Roll extends LegacyRandomSource {
        final int result;int calls;
        Roll(int result){super(1);this.result=result;}
        @Override public int nextInt(int bound){calls++;return Math.min(bound-1,result);}
    }
    private static final class Field implements AutoCloseable {
        final FakePlayer owner;final List<LivingEntity> actors=new ArrayList<>();
        Field(GameTestHelper h,MethodDomainStyle style) {
            owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"domain-refinement"));owner.setGameMode(GameType.SURVIVAL);
            owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2200+LAYERS.getAndIncrement()*48,3))));
            ModAttachments.setData(owner,ModAttachments.getData(owner).withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY)
                    .withActiveCultivationMethod(style.method).withMethodProficiencies(Map.of(style.method,500)).withQi(1000000));
            CombatStateManager.setEnabled(owner,true);CombatStateManager.toggleManualCombat(owner);
            owner.getPersistentData().putInt("ImmortalCultivationQiReleasePercent",200);QiDomainSystem.toggle(owner);
        }
        QiDomainSystem.Aura aura(){return QiDomainSystem.view(owner);}
        long pulse(){long now=owner.level().getGameTime();return now+Math.floorMod(owner.getId()-now,20);}
        Mob cow(double x) {
            var cow=EntityType.COW.create(owner.level());cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(owner.position().add(x,0,0));
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);cow.setHealth(100000);owner.level().addFreshEntity(cow);actors.add(cow);return cow;
        }
        @Override public void close(){QiDomainSystem.stop(owner);for(var actor:actors){ColdTideFreeze.clear(actor);TideWaterStacks.clear(actor);actor.discard();}owner.discard();}
    }
    @GameTest(template="fate_test") public static void devourChanceAddsOnePointPerMissAndResetsAfterGuaranteedWarp(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.DEVOUR)) {
            var target=f.cow(3);long now=f.pulse();var roll=new Roll(99);float hp=target.getHealth();
            for(int i=1;i<100;i++) {
                MethodDomainEffects.tick(f.aura(),now+(i-1)*20,roll);
                h.assertTrue(f.aura().domainExposure.get(target.getUUID())==i&&target.getHealth()==hp,"Misses build one percentage point each second without premature damage");
            }
            MethodDomainEffects.tick(f.aura(),now+99*20,roll);
            h.assertTrue(f.aura().domainExposure.get(target.getUUID())==0&&target.getHealth()<hp,"A hundred-percent roll guarantees warp and resets chance to zero");
            hp=target.getHealth();MethodDomainEffects.tick(f.aura(),now+100*20,roll);
            h.assertTrue(f.aura().domainExposure.get(target.getUUID())==1&&target.getHealth()==hp,"The first roll after warp starts again at one percent");
            h.assertTrue(roll.calls==101,"Exactly one roll per target per pulse");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void devourCountersAreIndependentAndClearOnExitOrDomainLoss(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.DEVOUR)) {
            var a=f.cow(3);var b=f.cow(5);long now=f.pulse();
            for(int i=0;i<3;i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Roll(99));
            b.setPos(b.position().add(0,25,0));MethodDomainEffects.tick(f.aura(),now+60,new Roll(99));
            h.assertTrue(!f.aura().domainExposure.containsKey(b.getUUID()),"Leaving removes only that target's accumulated chance");
            b.setPos(f.owner.position().add(5,0,0));MethodDomainEffects.tick(f.aura(),now+80,new Roll(99));
            h.assertTrue(f.aura().domainExposure.get(a.getUUID())==5&&f.aura().domainExposure.get(b.getUUID())==1,"Reentry starts at one while another target keeps its own chance");
            MethodDomainEffects.tick(f.aura(),now+100,new Roll(3));
            h.assertTrue(f.aura().domainExposure.get(a.getUUID())==0&&f.aura().domainExposure.get(b.getUUID())==2,"One target's warp resets only its own counter");
            f.aura().percent=190;QiDomainSystem.tickAll();h.assertTrue(f.aura().domainExposure.isEmpty(),"Dropping domain intensity clears accumulated chances");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void coldCryHealsCasterAndExtinguishesFireOncePerPulse(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.ICE)) {
            f.owner.setHealth(f.owner.getMaxHealth()/2);f.owner.igniteForSeconds(10);float before=f.owner.getHealth();long now=f.pulse();
            MethodDomainEffects.tick(f.aura(),now,new Roll(99));float after=f.owner.getHealth();
            float expected=Math.min(4,f.owner.getMaxHealth()*.002F);
            h.assertTrue(Math.abs(after-before-expected)<.001&&!f.owner.isOnFire(),"Cold Cry heals 0.2 percent of caster max HP capped at four and extinguishes fire, even without targets");
            MethodDomainEffects.tick(f.aura(),now,new Roll(99));h.assertTrue(f.owner.getHealth()==after,"Cannot heal twice on the same pulse");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void waterCountsTravelledPathOnceAcrossCastersAndIgnoresTeleports(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.COLD_TIDE);var other=new Field(h,MethodDomainStyle.COLD_TIDE)) {
            other.owner.setPos(f.owner.position());var target=f.cow(3);long now=f.owner.level().getGameTime();
            TideWaterStacks.expose(f.aura(),target,now);TideWaterStacks.expose(other.aura(),target,now);Vec3 start=target.position();
            for(int i=1;i<=5;i++){target.setPos(start.add(i%2==0?0:.2,0,0));TideWaterStacks.update(target,now+i);}
            h.assertTrue(TideWaterStacks.stacks(target)==1,"One block of back-and-forth travel counts once, rather than net displacement or twice for overlapping casters");
            for(int i=6;i<=16;i++){target.setPos(start.add(i%2==0?1:0,0,0));TideWaterStacks.update(target,now+i);}
            h.assertTrue(TideWaterStacks.stacks(target)==8&&ColdTideFreeze.progress(target)==0&&target.getTicksFrozen()==0,"Water stacks cap at eight without freezing");
            var teleported=f.cow(4);TideWaterStacks.expose(f.aura(),teleported,now);
            teleported.setPos(teleported.position().add(9,0,0));TideWaterStacks.update(teleported,now+1);
            h.assertTrue(TideWaterStacks.stacks(teleported)==0,"A teleport cannot manufacture nine movement stacks");
            teleported.setPos(teleported.position().add(1,0,0));TideWaterStacks.update(teleported,now+2);
            h.assertTrue(TideWaterStacks.stacks(teleported)==1,"Ordinary movement after teleport resumes normal stacking");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=60) public static void realServerTickSamplesMovementAndDampSurvivesSave(GameTestHelper h) {
        var f=new Field(h,MethodDomainStyle.COLD_TIDE);
        try {
            var target=f.cow(3);long now=h.getLevel().getGameTime();TideWaterStacks.expose(f.aura(),target,now);target.setPos(target.position().add(1,0,0));
            h.runAfterDelay(3,()->{try {
                h.assertTrue(TideWaterStacks.stacks(target)==1&&!ColdTideFreeze.isFrozen(target),"Normal server ticking samples actual movement and applies Damp without an ice lock");
                var saved=new CompoundTag();target.saveWithoutId(saved);var copy=EntityType.COW.create(h.getLevel());copy.load(saved);
                h.assertTrue(TideWaterStacks.stacks(copy)==1,"Damp stacks persist through vanilla entity save/load");
                QiDomainSystem.stop(f.owner);target.removeEffect(ModEffects.DAMP);target.addEffect(new MobEffectInstance(ModEffects.DAMP,3,1,false,false));
                h.runAfterDelay(6,()->{try{h.assertTrue(!target.hasEffect(ModEffects.DAMP),"Damp expires normally after leaving the domain");h.succeed();}finally{f.close();}});
            }catch(Throwable error){f.close();throw error;}});
        }catch(Throwable error){f.close();throw error;}
    }
    @GameTest(template="fate_test") public static void waterDoesNotStackForStillOrProtectedTargetsOrInactiveDomains(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.COLD_TIDE)) {
            var target=f.cow(3);long now=f.owner.level().getGameTime();TideWaterStacks.expose(f.aura(),target,now);
            for(int i=1;i<10;i++)TideWaterStacks.update(target,now+i);
            h.assertTrue(TideWaterStacks.stacks(target)==0,"Standing still never accumulates water stacks");
            f.aura().power=0;target.setPos(target.position().add(2,0,0));TideWaterStacks.update(target,now+10);
            h.assertTrue(TideWaterStacks.stacks(target)==0,"Suppressed domains cannot add movement stacks");f.aura().power=1;
            var protectedPlayer=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"protected-water"));protectedPlayer.setGameMode(GameType.CREATIVE);protectedPlayer.setPos(f.owner.position().add(4,0,0));
            TideWaterStacks.expose(f.aura(),protectedPlayer,now);protectedPlayer.setPos(protectedPlayer.position().add(1,0,0));TideWaterStacks.update(protectedPlayer,now+1);
            h.assertTrue(TideWaterStacks.stacks(protectedPlayer)==0,"Creative targets remain protected even at the movement sampler boundary");protectedPlayer.discard();
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void coldTideWaterCanWetSlimesWithoutFreezingThem(GameTestHelper h) {
        try(var f=new Field(h,MethodDomainStyle.COLD_TIDE)) {
            for(var type:List.of(EntityType.SLIME,EntityType.MAGMA_CUBE)) {
                var slime=type.create(h.getLevel());slime.setNoAi(true);slime.setNoGravity(true);slime.setPos(f.owner.position().add(3,0,0));h.getLevel().addFreshEntity(slime);f.actors.add(slime);
                long now=f.owner.level().getGameTime();TideWaterStacks.expose(f.aura(),slime,now);slime.setPos(slime.position().add(1,0,0));TideWaterStacks.update(slime,now+1);
                h.assertTrue(TideWaterStacks.stacks(slime)==1&&!ColdTideFreeze.isFrozen(slime)&&slime.getTicksFrozen()==0,"Water can wet moving slime-family mobs while preserving their ice immunity");
            }
        }h.succeed();
    }
}
