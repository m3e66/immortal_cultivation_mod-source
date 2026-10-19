package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.BeastRealms;
import com.example.immortal_cultivation_mod.entity.BeastDazeControl;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_cold_tide")
@PrefixGameTestTemplate(false)
public final class ColdTideFreezeGameTests {
    private static LivingEntity cow(GameTestHelper h,float hp) {
        var target=EntityType.COW.create(h.getLevel());target.setNoAi(true);target.setNoGravity(true);
        target.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,180,2))));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);target.setHealth(hp);h.getLevel().addFreshEntity(target);return target;
    }
    private static void cleanup(LivingEntity... actors){for(var e:actors){ColdTideFreeze.clear(e);QiDomainSystem.stop(e);e.discard();}}
    private static long freeze(LivingEntity owner,LivingEntity victim,long now) {
        for(int i=0;i<10000&&!ColdTideFreeze.isFrozen(victim,now);i++,now+=20)ColdTideFreeze.expose(owner,victim,now);
        return now-20;
    }
    @GameTest(template="fate_test") public static void woundedAndHealthyTargetsWithSameMaxHpFreezeAtSameSpeed(GameTestHelper h) {
        var owner=cow(h,20);var healthy=cow(h,100);var wounded=cow(h,100);var tough=cow(h,400);
        try {
            wounded.setHealth(1);long now=h.getLevel().getGameTime();
            for(int i=0;i<5;i++)for(var target:List.of(healthy,wounded,tough))ColdTideFreeze.expose(owner,target,now+i*20);
            h.assertTrue(ColdTideFreeze.progress(healthy)==ColdTideFreeze.progress(wounded),"Wounds never change buildup for equal maximum HP");
            h.assertTrue(Math.abs(ColdTideFreeze.progress(healthy)-2*ColdTideFreeze.progress(tough))<.0001,"Four times maximum HP requires twice the buildup time");
        }finally{cleanup(owner,healthy,wounded,tough);}h.succeed();
    }
    @GameTest(template="fate_test") public static void partialIceSlowlyThawsAndReentryResumesRemainder(GameTestHelper h) {
        var owner=cow(h,20);var target=cow(h,100);
        try {
            long now=h.getLevel().getGameTime();for(int i=0;i<6;i++)ColdTideFreeze.expose(owner,target,now+i*20);
            float before=ColdTideFreeze.progress(target);long outside=now+100+21+200;
            QiDomainSystem.stop(owner);ColdTideFreeze.update(target,outside);float left=ColdTideFreeze.progress(target);
            h.assertTrue(left>0&&left<before&&before-left<.15,"Ten seconds outside slowly melts partial ice without clearing it");
            ColdTideFreeze.expose(owner,target,outside);
            h.assertTrue(ColdTideFreeze.progress(target)>left&&ColdTideFreeze.progress(target)>.15,"Reentry adds to surviving ice rather than starting over");
        }finally{cleanup(owner,target);}h.succeed();
    }
    @GameTest(template="fate_test") public static void fullFreezeHoldsForFiveMinutesAfterLastDomainContact(GameTestHelper h) {
        var owner=cow(h,20);var target=cow(h,20);
        try {
            long full=freeze(owner,target,h.getLevel().getGameTime());
            h.assertTrue(ColdTideFreeze.progress(target)==1,"Full ice reached through real buildup");
            ColdTideFreeze.expose(owner,target,full+20);long last=full+20;QiDomainSystem.stop(owner);
            ColdTideFreeze.update(target,last+ColdTideFreeze.HOLD_TICKS-1);
            h.assertTrue(ColdTideFreeze.isFrozen(target,last+ColdTideFreeze.HOLD_TICKS-1),"Frozen for all 5999 ticks after final contact, even after caster stops");
            ColdTideFreeze.update(target,last+ColdTideFreeze.HOLD_TICKS);
            h.assertTrue(!ColdTideFreeze.isFrozen(target,last+ColdTideFreeze.HOLD_TICKS)&&ColdTideFreeze.progress(target)==0,"Exact five-minute expiry releases the actor and ice");
        }finally{cleanup(owner,target);}h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=60) public static void fullFreezeStopsActualBeastTickAndDamage(GameTestHelper h) {
        var owner=cow(h,20);var victim=cow(h,1000);var beast=ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
        beast.setNoGravity(true);beast.setPos(owner.position().add(3,0,0));h.getLevel().addFreshEntity(beast);
        try {
            freeze(owner,beast,h.getLevel().getGameTime());beast.setDeltaMovement(1,1,1);int tick=beast.tickCount;Vec3 place=beast.position();
            var pre=new EntityTickEvent.Pre(beast);NeoForge.EVENT_BUS.post(pre);
            h.assertTrue(pre.isCanceled()&&beast.getDeltaMovement().equals(Vec3.ZERO),"Whole beast tick is canceled, including custom attack timers and motion");
            float hp=victim.getHealth();beast.doHurtTarget(victim);
            h.assertTrue(victim.getHealth()==hp,"Even directly attempted beast damage is blocked after full freeze");
            h.runAfterDelay(6,()->{try {
                h.assertTrue(beast.tickCount==tick&&beast.position().distanceTo(place)<.0001,"Actual level ticking cannot advance or move a fully frozen beast: ticks="+tick+"/"+beast.tickCount+", distance="+beast.position().distanceTo(place));h.succeed();
            }finally{cleanup(owner,victim,beast);}});
        }catch(Throwable e){cleanup(owner,victim,beast);throw e;}
    }
    @GameTest(template="fate_test") public static void slimesAndMagmaCubesNeverBuildIce(GameTestHelper h) {
        var owner=cow(h,20);
        try {
            for(var type:List.of(EntityType.SLIME,EntityType.MAGMA_CUBE)) {
                var slime=type.create(h.getLevel());h.getLevel().addFreshEntity(slime);
                try {for(int i=0;i<100;i++)ColdTideFreeze.expose(owner,slime,h.getLevel().getGameTime()+i*20);
                    h.assertTrue(ColdTideFreeze.progress(slime)==0&&!ColdTideFreeze.isFrozen(slime)&&slime.getTicksFrozen()==0&&slime.getActiveEffects().isEmpty(),"Slime-family immunity includes frost, slow, full lock and magma cubes");
                }finally{cleanup(slime);}
            }
        }finally{cleanup(owner);}h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=70) public static void allFourteenBeastTypesStayLockedDuringRealWorldTicksAndDirectAttacks(GameTestHelper h) {
        var owner=cow(h,20);var victim=cow(h,100000);var control=cow(h,20);
        var beasts=new ArrayList<Mob>();var places=new ArrayList<Vec3>();var clocks=new ArrayList<Integer>();
        owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,900,3))));victim.setPos(owner.position().add(4,0,0));control.setPos(owner.position().add(0,5,0));
        Runnable close=()->{for(var beast:beasts)cleanup(beast);cleanup(owner,victim,control);};
        try {
            for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                if(BeastRealms.baseRealm(type)<0)continue;
                var beast=(Mob)type.create(h.getLevel());beast.setNoAi(false);beast.setNoGravity(true);beast.setPos(owner.position().add(0,beasts.size()*40,0));
                h.getLevel().addFreshEntity(beast);beast.setTarget(victim);beasts.add(beast);
                freeze(owner,beast,h.getLevel().getGameTime());
                h.assertTrue(ColdTideFreeze.isFrozen(beast)&&BeastDazeControl.blocksMoves(beast),"Fully frozen beast also blocks reactive move entry points: "+type);
                places.add(beast.position());clocks.add(beast.tickCount);beast.setDeltaMovement(.5,.5,.5);
                float hp=victim.getHealth();beast.doHurtTarget(victim);
                h.assertTrue(victim.getHealth()==hp,"Direct beast melee cannot damage while frozen: "+type);
            }
            h.assertTrue(beasts.size()==14,"Regression covers all fourteen registered realm-beast types");int awake=control.tickCount;
            h.runAfterDelay(25,()->{try {
                h.assertTrue(control.tickCount>awake,"Control actor really ticked in this chunk; this is not an unloaded-chunk false pass");
                for(int i=0;i<beasts.size();i++) {
                    var beast=beasts.get(i);
                    h.assertTrue(!beast.isNoAi()&&ColdTideFreeze.isFrozen(beast)&&beast.tickCount==clocks.get(i)
                            &&beast.position().distanceToSqr(places.get(i))<.000001&&beast.getDeltaMovement().lengthSqr()==0,
                            "Normal level ticks cannot run beast AI, abilities or motion: "+beast.getType());
                }h.succeed();
            }finally{close.run();}});
        }catch(Throwable error){close.run();throw error;}
    }
    @GameTest(template="fate_test",timeoutTicks=60) public static void frozenMobsStillTakeDamageAndStatusTimersExpire(GameTestHelper h) {
        var owner=cow(h,20);var target=cow(h,100);
        try {
            freeze(owner,target,h.getLevel().getGameTime());
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,3));
            target.hurt(target.damageSources().generic(),2);float after=target.getHealth();
            h.runAfterDelay(12,()->{try {
                h.assertTrue(!target.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Ordinary status durations still expire while AI and attacks are frozen");
                target.hurt(target.damageSources().generic(),2);
                h.assertTrue(target.getHealth()<after&&ColdTideFreeze.isFrozen(target),"Frozen target remains damageable after its normal immunity window instead of becoming invulnerable for five minutes");h.succeed();
            }finally{cleanup(owner,target);}});
        }catch(Throwable e){cleanup(owner,target);throw e;}
    }
    @GameTest(template="fate_test") public static void frozenGuardianCannotReactivateOrStartCountermovesWhenHit(GameTestHelper h) {
        var owner=cow(h,20);var guardian=ModEntities.VAULT_GUARDIAN.get().create(h.getLevel());
        try {
            guardian.setNoGravity(true);guardian.setPos(owner.position().add(3,0,0));h.getLevel().addFreshEntity(guardian);
            freeze(owner,guardian,h.getLevel().getGameTime());
            h.assertTrue(!guardian.isActive(),"Dormant guardian starts inactive");
            guardian.activateAgainst(owner);guardian.hurt(owner.damageSources().mobAttack(owner),10);
            h.assertTrue(!guardian.isActive()&&guardian.getActionState()==0,"Hitting or explicitly activating a frozen guardian cannot start its activation or counterattack");
        }finally{cleanup(owner,guardian);}h.succeed();
    }
    @GameTest(template="fate_test") public static void savedPartialAndFullIceSurviveEntityReload(GameTestHelper h) {
        var owner=cow(h,20);var target=cow(h,100);var copy=cow(h,100);
        try {
            long now=h.getLevel().getGameTime();for(int i=0;i<4;i++)ColdTideFreeze.expose(owner,target,now+i*20);
            copy.getPersistentData().put(ColdTideFreeze.TAG,target.getPersistentData().getCompound(ColdTideFreeze.TAG).copy());ColdTideFreeze.restore(copy);
            h.assertTrue(ColdTideFreeze.progress(copy)==ColdTideFreeze.progress(target),"NBT reload preserves partial ice instead of resetting caster-owned exposure");
            freeze(owner,target,now+100);copy.getPersistentData().put(ColdTideFreeze.TAG,target.getPersistentData().getCompound(ColdTideFreeze.TAG).copy());ColdTideFreeze.restore(copy);
            h.assertTrue(ColdTideFreeze.isFrozen(copy)&&ColdTideFreeze.progress(copy)==1,"Full lock and its world-time expiry survive reload");
        }finally{cleanup(owner,target,copy);}h.succeed();
    }
    @GameTest(template="fate_test") public static void overlappingCastersCannotDoubleFreezeWithinOneSecond(GameTestHelper h) {
        var owner=cow(h,20);var other=cow(h,20);var target=cow(h,100);
        try {
            long now=h.getLevel().getGameTime();ColdTideFreeze.expose(owner,target,now);float once=ColdTideFreeze.progress(target);
            ColdTideFreeze.expose(other,target,now+1);h.assertTrue(ColdTideFreeze.progress(target)==once,"One target receives at most one buildup pulse each second across casters");
            ColdTideFreeze.expose(other,target,now+20);h.assertTrue(ColdTideFreeze.progress(target)>once,"Another caster can continue existing ice on the next pulse");
        }finally{cleanup(owner,other,target);}h.succeed();
    }
    @GameTest(template="fate_test") public static void coldStatusPacketRoundTripsAndRejectsUnboundedTimers(GameTestHelper h) {
        var b=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var packet=new QiAuraPayloads.ColdTideStatus(h.getLevel().dimension().location(),123,UUID.randomUUID(),255,6000);
            QiAuraPayloads.ColdTideStatus.CODEC.encode(b,packet);h.assertTrue(b.readableBytes()<128&&packet.equals(QiAuraPayloads.ColdTideStatus.CODEC.decode(b)),"Fixed-size saved cold snapshot round-trips max hold and full coverage");
            boolean rejected=false;try{new QiAuraPayloads.ColdTideStatus(h.getLevel().dimension().location(),1,UUID.randomUUID(),256,6001);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Invalid coverage and duration cannot poison the client state");
        }finally{b.release();}h.succeed();
    }
}
