package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.physics.DomainVolume;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class DomainClashGameTests {
    private static final AtomicInteger LAYERS=new AtomicInteger();
    private static int pvpUsers;
    private static boolean savedPvp;
    private static final class Field implements AutoCloseable {
        final FakePlayer first,second;
        Field(GameTestHelper h,String methodA,int masteryA,String methodB,int masteryB) {
            // Asynchronous fields share one test server. Restore its global PvP flag
            // only after the final field closes, never during another live contest.
            if(pvpUsers++==0)savedPvp=h.getLevel().getServer().isPvpAllowed();
            h.getLevel().getServer().setPvpAllowed(true);
            Vec3 p=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,5000+LAYERS.getAndIncrement()*64,3)));
            first=player(h,p,methodA,masteryA);second=player(h,p.add(24,0,0),methodB,masteryB);
            QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(first).opponent==second.getId()&&!QiDomainSystem.volume(first).cuts().isEmpty(),"Fixture establishes a real hostile contact");
        }
        @Override public void close(){QiDomainSystem.stop(first);QiDomainSystem.stop(second);first.discard();second.discard();if(--pvpUsers==0)first.level().getServer().setPvpAllowed(savedPvp);}
    }
    private static FakePlayer player(GameTestHelper h,Vec3 p,String method,int mastery) {
        // NeoForge's automation FakePlayer explicitly forbids PvP. This fixture represents
        // a survival player and restores vanilla's server/team permission for its contests.
        var actor=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"domain-clash")) {
            @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return getServer().isPvpAllowed()&&!isAlliedTo(other);
            }
        };actor.setGameMode(GameType.SURVIVAL);actor.setPos(p);
        ModAttachments.setData(actor,ModAttachments.getData(actor).withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY)
                .withActiveCultivationMethod(method).withMethodProficiencies(Map.of(method,mastery)).withQi(1000000));
        CombatStateManager.setEnabled(actor,true);CombatStateManager.toggleManualCombat(actor);
        actor.getPersistentData().putInt("ImmortalCultivationQiReleasePercent",200);QiDomainSystem.toggle(actor);return actor;
    }
    @GameTest(template="fate_test") public static void ceilingRanksBeforeProficiencyAndAllMethodsCovered(GameTestHelper h) {
        h.assertTrue(DomainClashes.compare(CultivationMethods.REINCARNATION_TRUE_ART,0,CultivationMethods.BASIC_BREATHING,10000)>0,"Method realm ceiling wins before mastery");
        h.assertTrue(DomainClashes.compare(CultivationMethods.POKONG_JUE,501,CultivationMethods.CHANGQING_JUE,500)>0,"Equal ceilings use exact learned proficiency");
        h.assertTrue(DomainClashes.compare(CultivationMethods.POKONG_JUE,500,CultivationMethods.CHANGQING_JUE,500)==0,"Exact tie has no arbitrary winner");
        h.assertTrue(CultivationMethods.allMethods().stream().allMatch(m->DomainClashes.limit(m.id())>=0),"Every domain has its real method ceiling");h.succeed();
    }
    @GameTest(template="fate_test") public static void compressedVolumesNeverOverlapAndTrackVerticalMovement(GameTestHelper h) {
        for(var rival:List.of(new Vec3(28,0,0),new Vec3(16,12,0),new Vec3(0,30,0)))for(int age:new int[]{0,10,25,40,60,79,80,95,160}) {
            var a=new DomainVolume(Vec3.ZERO,List.of(DomainVolume.contact(Vec3.ZERO,rival,age,1,1)));
            var b=new DomainVolume(rival,List.of(DomainVolume.contact(rival,Vec3.ZERO,age,-1,-1)));
            for(int x=-20;x<=40;x+=2)for(int y=-20;y<=40;y+=2)for(int z=-20;z<=20;z+=4) {
                var point=new Vec3(x+.13,y+.17,z+.19);
                h.assertTrue(!(a.contains(point)&&b.contains(point)),"Two pressed domain volumes cannot both own a point");
            }
            for(int i=0;i<100;i++) {
                double theta=i*2.399,y=1-2*(i+.5)/100,r=Math.sqrt(1-y*y);
                Vec3 point=a.surface(new Vec3(Math.cos(theta)*r,y,Math.sin(theta)*r));
                h.assertTrue(Double.isFinite(point.x)&&point.length()<=20.00001,"Elastic boundary remains finite and within its radius");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=100) public static void clashRemainsActivePastOldTwoSecondTimer(GameTestHelper h) {
        var f=new Field(h,CultivationMethods.BASIC_BREATHING,10000,CultivationMethods.REINCARNATION_TRUE_ART,500);
        h.runAfterDelay(55,()->{try {
            h.assertTrue(QiDomainSystem.view(f.first).domain()&&QiDomainSystem.view(f.second).domain(),"Unequal domains continue clashing after 2.75 seconds");
            h.assertTrue(!QiDomainSystem.volume(f.first).cuts().isEmpty()&&!QiDomainSystem.volume(f.second).cuts().isEmpty(),"Both domains remain physically pressed during the extended contest");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test") public static void membraneShuddersStayInsideBothRealDomains(GameTestHelper h) {
        Vec3 rival=new Vec3(24,0,0);
        var first=new DomainVolume(Vec3.ZERO,List.of(DomainVolume.contact(Vec3.ZERO,rival,60,1,1)));
        var second=new DomainVolume(rival,List.of(DomainVolume.contact(rival,Vec3.ZERO,60,-1,-1)));
        var quiet=new DomainVolume(Vec3.ZERO,List.of());
        for(var field:List.of(first,second)) {
            double largestMotion=0;
            for(int i=0;i<100;i++) {
                double y=1-2*(i+.5)/100,angle=i*2.399,r=Math.sqrt(1-y*y);
                Vec3 direction=new Vec3(Math.cos(angle)*r,y,Math.sin(angle)*r);
                h.assertTrue(quiet.surface(direction).equals(quiet.vibratingSurface(direction,60)),"A field without contact never shakes");
                Vec3 previous=field.vibratingSurface(direction,0);
                for(int time=1;time<=120;time++) {
                    Vec3 point=field.vibratingSurface(direction,time);
                    h.assertTrue(Double.isFinite(point.lengthSqr())&&field.contains(point),"Shudder cannot cross a gameplay border or expand attack range");
                    largestMotion=Math.max(largestMotion,point.distanceTo(previous));previous=point;
                }
            }
            h.assertTrue(largestMotion>.12,"Both real membranes visibly move each frame, including the winning side");
        }
        var early=DomainVolume.contact(Vec3.ZERO,rival,90,0,1);
        var later=DomainVolume.contact(Vec3.ZERO,rival,93,0,1);
        h.assertTrue(Math.abs(early.offset()-later.offset())>.05,"Equal contests keep physically recoiling after the initial squeeze");h.succeed();
    }
    @GameTest(template="fate_test") public static void extendedClashAgeSurvivesRealNetworkCodec(GameTestHelper h) {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            for(int age:new int[]{0,40,65,80,119}) {
                var state=new com.example.immortal_cultivation_mod.network.QiAuraPayloads.DomainClash(h.getLevel().dimension().location(),1,UUID.randomUUID(),2,UUID.randomUUID(),0,7,1,age,0);
                com.example.immortal_cultivation_mod.network.QiAuraPayloads.DomainClash.CODEC.encode(buffer,state);
                h.assertTrue(state.equals(com.example.immortal_cultivation_mod.network.QiAuraPayloads.DomainClash.CODEC.decode(buffer)),"Late clash packet round-trips without decoder failure");buffer.clear();
            }
        }finally{buffer.release();}
        for(long age:new long[]{80,119,120,204,600,1000000}) {
            var actual=DomainVolume.contact(Vec3.ZERO,new Vec3(24,0,0),age,0,1);
            var received=DomainVolume.contact(Vec3.ZERO,new Vec3(24,0,0),DomainVolume.clashVisualAge(age),0,1);
            h.assertTrue(Math.abs(actual.offset()-received.offset())<1E-7,"A long tied contest keeps its exact shared recoil phase without packet growth");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=100) public static void lowerMethodCeilingShattersAndCannotImmediatelyReopen(GameTestHelper h) {
        var f=new Field(h,CultivationMethods.BASIC_BREATHING,10000,CultivationMethods.REINCARNATION_TRUE_ART,500);
        h.assertTrue(QiDomainSystem.view(f.first).domain()&&QiDomainSystem.view(f.second).domain(),"Both domains visibly clash before resolution");
        h.runAfterDelay(DomainClashes.CONTEST_TICKS+2,()->{try {
            h.assertTrue(!QiDomainSystem.view(f.first).domain()&&QiDomainSystem.view(f.second).domain(),"Lower-ceiling domain breaks even with higher mastery");
            h.assertTrue(QiDomainSystem.blasting(f.first),"Domain destruction preserves body Qi release");
            f.first.setShiftKeyDown(true);QiDomainSystem.view(f.first).percent=190;QiDomainSystem.adjust(f.first,1);
            h.assertTrue(!QiDomainSystem.view(f.first).domain(),"Three-second recovery blocks scroll reopening");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test",timeoutTicks=100) public static void equalCeilingHigherProficiencyWins(GameTestHelper h) {
        var f=new Field(h,CultivationMethods.POKONG_JUE,501,CultivationMethods.CHANGQING_JUE,500);
        h.runAfterDelay(DomainClashes.CONTEST_TICKS+2,()->{try {
            h.assertTrue(QiDomainSystem.view(f.first).domain()&&!QiDomainSystem.view(f.second).domain(),"Equal realm limit resolves by method proficiency");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test",timeoutTicks=130) public static void exactTiePersistsAndSeparatingCancelsContest(GameTestHelper h) {
        var f=new Field(h,CultivationMethods.POKONG_JUE,500,CultivationMethods.CHANGQING_JUE,500);
        h.runAfterDelay(DomainClashes.CONTEST_TICKS+15,()->{try {
            h.assertTrue(QiDomainSystem.view(f.first).domain()&&QiDomainSystem.view(f.second).domain(),"Exact tie remains locked beyond destruction timer");
            f.second.setPos(f.first.position().add(41,0,0));QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(f.first).opponent==-1&&QiDomainSystem.volume(f.first).cuts().isEmpty(),"Separation clears collision and restores the full shell");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test") public static void effectsCannotCrossPressedBoundaryAndStopClearsOtherSide(GameTestHelper h) {
        try(var f=new Field(h,CultivationMethods.POKONG_JUE,500,CultivationMethods.CHANGQING_JUE,500)) {
            var victim=EntityType.COW.create(h.getLevel());victim.setPos(f.first.position().add(18,0,0));
            h.assertTrue(!MethodDomainEffects.targets(f.first).contains(victim),"Unregistered victim cannot enter target query");
            h.getLevel().addFreshEntity(victim);
            try {
                h.assertTrue(!MethodDomainEffects.targets(f.first).contains(victim),"Enemy past shared border cannot be hit by clipped domain");
                QiDomainSystem.stop(f.second);QiDomainSystem.tickAll();
                h.assertTrue(MethodDomainEffects.targets(f.first).contains(victim),"Stopping rival restores true effect area immediately");
            }finally{victim.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=100) public static void threeWayContestResolvesAllLosersWithoutIterationBias(GameTestHelper h) {
        var f=new Field(h,CultivationMethods.BASIC_BREATHING,10000,CultivationMethods.POKONG_JUE,5000);
        var third=player(h,f.first.position().add(12,0,14),CultivationMethods.REINCARNATION_TRUE_ART,500);
        QiDomainSystem.tickAll();
        h.assertTrue(QiDomainSystem.volume(third).cuts().size()==2,"Third domain presses against both opponents");
        h.runAfterDelay(DomainClashes.CONTEST_TICKS+2,()->{try {
            h.assertTrue(!QiDomainSystem.view(f.first).domain()&&!QiDomainSystem.view(f.second).domain()&&QiDomainSystem.view(third).domain(),"Highest method limit survives the simultaneous three-way contest");
            h.assertTrue(QiDomainSystem.volume(third).cuts().isEmpty(),"No destroyed-domain clipping remains on winner");h.succeed();
        }finally{QiDomainSystem.stop(third);third.discard();f.close();}});
    }
}
