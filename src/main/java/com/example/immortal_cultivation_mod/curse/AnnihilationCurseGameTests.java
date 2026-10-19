package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_curse")
@PrefixGameTestTemplate(false)
public final class AnnihilationCurseGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"curse-test"));p.setGameMode(GameType.SURVIVAL);
        p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,80,3))));p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);
        ModAttachments.setData(p,ModAttachments.getData(p).withLuck(0).withAddedMaxHpBonus(80));
        ServerEvents.syncPlayerData(p);p.setHealth(p.getMaxHealth());return p;
    }
    private static LivingEntity cow(GameTestHelper h) {
        var p=EntityType.COW.create(h.getLevel());p.setNoGravity(true);p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,90,4))));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);return p;
    }
    private static void near(GameTestHelper h,float actual,float expected,String message){h.assertTrue(Math.abs(actual-expected)<.0001F,message+": "+actual+" / "+expected);}
    private static void ticks(LivingEntity actor,int count){for(int i=0;i<count;i++){actor.tickCount++;AnnihilationCurse.tick(actor);}}
    @GameTest(template="fate_test") public static void realDamageCannotSkipFivePercentRescueAndRestoredHpIsBlack(GameTestHelper h) {
        // FakePlayer deliberately rejects hurt(); this regression needs the actual ServerPlayer path.
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new GameProfile(UUID.randomUUID(),"curse-damage"),net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,connection,p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,80,3))));
        try {var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
        ModAttachments.setData(p,ModAttachments.getData(p).withLuck(0).withAddedMaxHpBonus(80));ServerEvents.syncPlayerData(p);p.setHealth(100);
        AnnihilationCurse.acquire(p);p.hurt(p.damageSources().generic(),120);
        near(h,p.getHealth(),100,"One large real hit triggers rescue instead of skipping the threshold");
        near(h,AnnihilationCurse.blackHp(p),95,"Only rescued health is black");
        h.assertTrue(AnnihilationCurse.healingLocked(p)&&AnnihilationCurse.cooldown(p)==36000,"Thirty-minute online cooldown starts once");
        p.invulnerableTime=0;p.hurt(p.damageSources().generic(),30);
        near(h,AnnihilationCurse.blackHp(p),95-(100-p.getHealth()),"Actual damage immediately spends black HP without waiting for a snapshot tick");
        h.assertTrue(ModAttachments.getData(p).knownSpells().contains(AnnihilationCurse.ABILITY),"Bottle ability appears in learned innate abilities");p.discard();channel.close();h.succeed();
    }
    @GameTest(template="fate_test") public static void exactThresholdRescueAlsoHandlesNonDamageHealthLoss(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.acquire(p);p.setHealth(5);AnnihilationCurse.tick(p);
        near(h,p.getHealth(),100,"At exactly five percent HP, passive rescue fires");
        p.setHealth(1);AnnihilationCurse.tick(p);near(h,p.getHealth(),1,"Same life never gets a second rescue");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void healingAndDirectHealthAssignmentAreBlockedForHostAndTarget(GameTestHelper h) {
        var host=player(h);AnnihilationCurse.acquire(host);AnnihilationCurse.rescue(host);host.setHealth(30);
        host.heal(50);host.setHealth(80);near(h,host.getHealth(),30,"Host cannot heal through either API");
        var target=cow(h);target.setHealth(40);AnnihilationCurse.markByAttack(target);target.heal(60);target.setHealth(100);
        near(h,target.getHealth(),40,"Infected target cannot heal through either API");
        host.discard();target.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void ownerDeathClearsOwnRunesAndHealingLockButNotOwnershipOrCooldown(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.acquire(p);AnnihilationCurse.rescue(p);ticks(p,160);AnnihilationCurse.onlineTick(p);
        int cooldown=AnnihilationCurse.cooldown(p);var clone=player(h);
        AnnihilationCurse.clonePlayer(new PlayerEvent.Clone(clone,p,true));
        h.assertTrue(AnnihilationCurse.owned(clone)&&AnnihilationCurse.cooldown(clone)==cooldown,"Ownership and exact remaining online cooldown survive death");
        near(h,AnnihilationCurse.coverage(clone),0,"Host's own runes clear on death");
        clone.setHealth(20);clone.heal(15);near(h,clone.getHealth(),35,"New life can heal again");p.discard();clone.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void cooldownIsSerializedRemainingOnlineTicksNotWorldOrOfflineTime(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.acquire(p);AnnihilationCurse.rescue(p);
        int before=AnnihilationCurse.cooldown(p);for(int i=0;i<1000;i++)AnnihilationCurse.tick(p);
        h.assertTrue(AnnihilationCurse.cooldown(p)==before,"Entity ticks and world aging cannot spend the online-only timer");
        for(int i=0;i<600;i++)AnnihilationCurse.onlineTick(p);
        var restored=player(h);restored.getPersistentData().put(AnnihilationCurse.TAG,AnnihilationCurse.state(p).copy());
        h.assertTrue(AnnihilationCurse.cooldown(restored)==before-600,"Saved remaining cooldown resumes without offline elapsed time");
        p.discard();restored.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void targetRespawnRetainsRunesButDeathEndsItsHealingPenalty(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.markByAttack(p);AnnihilationCurse.state(p).putFloat("Coverage",.43F);
        var clone=player(h);AnnihilationCurse.clonePlayer(new PlayerEvent.Clone(clone,p,true));
        near(h,AnnihilationCurse.coverage(clone),.43F,"Target coverage persists across its own death");
        h.assertTrue(!AnnihilationCurse.healingLocked(clone),"Until-death healing penalty clears for either player role");
        p.discard();clone.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void anotherBottleCannotRefreshAnExistingRescueCooldown(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.acquire(p);AnnihilationCurse.rescue(p);AnnihilationCurse.onlineTick(p);
        int before=AnnihilationCurse.cooldown(p);AnnihilationCurse.acquire(p);
        h.assertTrue(before==AnnihilationCurse.cooldown(p),"Repeated acquisition never resets or bypasses cooldown");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void bloomingNeverDamagesTheHostAndDamageConsumesBlackHpFirst(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.acquire(p);AnnihilationCurse.rescue(p);ticks(p,160);
        near(h,AnnihilationCurse.coverage(p),1,"Chest bloom reaches full body");near(h,p.getHealth(),100,"Bloom never hurts its host");
        p.setHealth(70);AnnihilationCurse.tick(p);near(h,AnnihilationCurse.blackHp(p),65,"Damage spends restored black HP before original normal HP");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void acceptedAttacksIncreaseGrowthSpeedByPointOnePercentagePoint(GameTestHelper h) {
        var p=cow(h);AnnihilationCurse.markByAttack(p);float first=AnnihilationCurse.state(p).getFloat("Growth");
        for(int i=0;i<10;i++)AnnihilationCurse.markByAttack(p);
        near(h,AnnihilationCurse.state(p).getFloat("Growth"),first+.01F,"Ten accepted hits add one percentage point per second");
        ticks(p,20);h.assertTrue(AnnihilationCurse.coverage(p)>.01F&&AnnihilationCurse.healingLocked(p),"Runes grow and seal healing");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void repeatedContactCannotMultiplyCoverageAndEachGenerationHalves(GameTestHelper h) {
        var source=cow(h);var target=cow(h);var third=cow(h);
        AnnihilationCurse.state(source).putFloat("Coverage",1);
        for(int i=0;i<100;i++)AnnihilationCurse.transfer(source,target);ticks(target,1200);
        near(h,AnnihilationCurse.coverage(target),.5F,"First contact generation caps at half even after repeated touch");
        for(int i=0;i<100;i++)AnnihilationCurse.transfer(target,third);ticks(third,1200);
        near(h,AnnihilationCurse.coverage(third),.25F,"Second generation caps at a quarter");
        h.assertTrue(!AnnihilationCurse.owned(target)&&!AnnihilationCurse.owned(third),"Contact never copies the rescue ability");
        source.discard();target.discard();third.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void meditationRemovesExactlyOnePointPerMinuteAndSuspendsGrowth(GameTestHelper h) {
        var p=player(h);AnnihilationCurse.markByAttack(p);AnnihilationCurse.state(p).putFloat("Coverage",.50F);
        ModAttachments.setData(p,ModAttachments.getData(p).withMeditating(true));ticks(p,1199);
        near(h,AnnihilationCurse.coverage(p),.50F,"No premature cleansing or growth during meditation");ticks(p,1);
        near(h,AnnihilationCurse.coverage(p),.49F,"One online meditation minute removes one percentage point");
        h.assertTrue(AnnihilationCurse.blocksMeditationExperience(p),"Partial runes still block experience");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void realMeditationTickGrantsNeitherCultivationNorMethodExperience(GameTestHelper h) {
        var p=player(h);ModAttachments.setData(p,ModAttachments.getData(p)
                .withCultivationLevel(CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY)
                .withActiveCultivationMethod(CultivationMethods.BASIC_BREATHING).withQi(1000).withMeditating(true));
        AnnihilationCurse.markByAttack(p);var before=ModAttachments.getData(p);p.tickCount=20;p.setOnGround(true);
        ServerEvents.onPlayerTick(new PlayerTickEvent.Post(p));var after=ModAttachments.getData(p);
        h.assertTrue(after.cultivationProgress()==before.cultivationProgress()
                &&after.methodProficiencies().equals(before.methodProficiencies()),"Actual meditation path grants no progress or proficiency while cursed");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void targetRunesSaveWithoutTimerExpiryAndSurviveCasterDeath(GameTestHelper h) {
        var target=cow(h);AnnihilationCurse.markByAttack(target);AnnihilationCurse.state(target).putFloat("Coverage",.72F);
        AnnihilationCurse.state(target).putFloat("Ceiling",.72F);CompoundTag saved=target.saveWithoutId(new CompoundTag());
        var restored=cow(h);restored.load(saved);ticks(restored,200);
        near(h,AnnihilationCurse.coverage(restored),.72F,"No expiry, caster ownership or unload reset controls saved target runes");
        h.assertTrue(AnnihilationCurse.healingLocked(restored),"Healing seal is saved on the target");target.discard();restored.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void curseConsumesNpcQiAndCorrodesHpWithoutNeedingCasterNearby(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());NpcProfile.ensure(npc,npc.npcArchetype());
        var s=npc.getPersistentData().getCompound("ImmortalCultivationNpc");s.putInt("Qi",1000);
        AnnihilationCurse.markByAttack(npc);AnnihilationCurse.state(npc).putFloat("Coverage",1);
        float hp=npc.getHealth();int qi=NpcProfile.qi(npc,npc.npcArchetype());npc.tickCount=20;AnnihilationCurse.tick(npc);
        h.assertTrue(npc.getHealth()<hp&&NpcProfile.qi(npc,npc.npcArchetype())<qi,"Saved target independently loses actual HP and NPC Qi");npc.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void creativeAndSpectatorAreNotInfectedAndInvalidSnapshotsAreRejected(GameTestHelper h) {
        var source=cow(h);AnnihilationCurse.state(source).putFloat("Coverage",1);var p=player(h);
        for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
            p.setGameMode(mode);AnnihilationCurse.transfer(source,p);AnnihilationCurse.markByAttack(p);
            near(h,AnnihilationCurse.coverage(p),0,"Protected game mode is unaffected");
        }
        boolean rejected=false;try{new CursePayloads.Status(h.getLevel().dimension().location(),1,UUID.randomUUID(),Float.NaN,0,false,false,0);}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Malformed non-finite network progress cannot enter client state");source.discard();p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void bottleDropsNothingEvenWithLootContexts(GameTestHelper h) {
        var state=ModBlocks.SEALED_CRADLE_BOTTLE.get().defaultBlockState();var pos=h.absolutePos(new BlockPos(2,2,2));
        h.assertTrue(Block.getDrops(state,h.getLevel(),pos,null,null,ItemStack.EMPTY).isEmpty(),"Broken bottle never yields an item");
        h.succeed();
    }
}
