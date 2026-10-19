package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.entity.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class NpcMasteryGameTests {
    @GameTest(template="fate_test") public static void higherNaturalMasteryIsProgressivelyRarer(GameTestHelper h) {
        int[] tiers=new int[5];var random=new Random(20841);
        for(int i=0;i<50000;i++) {
            UUID id=new UUID(random.nextLong(),random.nextLong());int value=NpcProfile.naturalMethodProficiency(id,CultivationMethods.POKONG_JUE);
            h.assertTrue(value>=0&&value<=10000&&value==NpcProfile.naturalMethodProficiency(id,CultivationMethods.POKONG_JUE),"Aptitude is bounded and stable per person/method");
            tiers[value<100?0:value<500?1:value<1000?2:value<2000?3:4]++;
        }
        h.assertTrue(tiers[0]>tiers[1]&&tiers[1]>tiers[2]&&tiers[2]>tiers[3]&&tiers[3]>tiers[4],"Higher mastery tiers are progressively rarer: "+Arrays.toString(tiers));
        int domains=tiers[2]+tiers[3]+tiers[4];h.assertTrue(domains>7000&&domains<8000&&tiers[4]>150&&tiers[4]<350,"About 15 percent reach domain mastery, while highest tier stays near half a percent: "+Arrays.toString(tiers));h.succeed();
    }
    @GameTest(template="fate_test") public static void legacyDefaultsRandomizeOnceAndTrainedOrCustomValuesPersist(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var loaded=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            NpcProfile.ensure(npc,npc.npcArchetype());var p=npc.getPersistentData().getCompound("ImmortalCultivationNpc");String method=p.getString("Method");
            int expected=NpcProfile.naturalMethodProficiency(npc.getUUID(),method);h.assertTrue(QiDomainSystem.mastery(npc)==expected,"Fresh live profile uses its weighted per-method aptitude");
            p.putInt("DomainMasteryVersion",1);p.getCompound("MethodProficiencies").putInt(method,500);NpcProfile.ensure(npc,npc.npcArchetype());
            h.assertTrue(QiDomainSystem.mastery(npc)==expected&&p.getInt("DomainMasteryVersion")==2,"Old forced 500 migrates once");
            p.getCompound("MethodProficiencies").putInt(method,501);p.putInt("DomainMasteryVersion",1);NpcProfile.ensure(npc,npc.npcArchetype());h.assertTrue(QiDomainSystem.mastery(npc)==501,"Earned progression is preserved");
            p.getCompound("MethodProficiencies").putInt(method,500);for(int i=0;i<20;i++)NpcProfile.ensure(npc,npc.npcArchetype());h.assertTrue(QiDomainSystem.mastery(npc)==500,"A subsequent explicit per-method 500 is never rerolled");
            var saved=new net.minecraft.nbt.CompoundTag();npc.saveWithoutId(saved);loaded.load(saved);h.assertTrue(QiDomainSystem.mastery(loaded)==500,"Assigned mastery persists through a real save/load");
            p.putInt("DomainMasteryVersion",1);p.putInt("MethodProficiency",777);p.getCompound("MethodProficiencies").putInt(method,500);NpcProfile.ensure(npc,npc.npcArchetype());h.assertTrue(QiDomainSystem.mastery(npc)==777,"Explicit legacy editor override is preserved");
        }finally{npc.discard();loaded.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void onlyMasteredNaturalNpcsOpenDomainsAndChangingToLowMasteryClosesThem(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var victim=EntityType.COW.create(h.getLevel());
        try {
            npc.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,3000,3))));victim.setPos(npc.position().add(3,0,0));npc.setNoAi(true);npc.setNoGravity(true);NpcProfile.ensure(npc,npc.npcArchetype());
            var p=npc.getPersistentData().getCompound("ImmortalCultivationNpc");String method=p.getString("Method");p.getCompound("MethodProficiencies").putInt(method,123);npc.setTarget(victim);npc.tickCount=21;
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.blasting(npc)&&!QiDomainSystem.view(npc).domain()&&QiDomainSystem.percent(npc)==100,"Low-mastery Golden Core can blast body Qi without a domain");
            QiDomainSystem.stop(npc);npc.getPersistentData().remove("ImmortalCultivationNpcQiBlastReadyAt");p.getCompound("MethodProficiencies").putInt(method,750);
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));h.assertTrue(QiDomainSystem.view(npc).domain(),"Mastered NPC opens its actual domain");
            p.getCompound("MethodProficiencies").putInt(method,499);QiDomainSystem.tickAll();h.assertTrue(QiDomainSystem.blasting(npc)&&!QiDomainSystem.view(npc).domain(),"Losing active-method mastery removes the domain while preserving body Qi");
        }finally{QiDomainSystem.stop(npc);npc.discard();victim.discard();}h.succeed();
    }
}
