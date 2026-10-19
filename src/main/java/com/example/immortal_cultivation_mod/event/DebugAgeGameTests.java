package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.combat.QiDomainSystem;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.item.CultivationAccessoryItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@GameTestHolder("immortal_cultivation_debug_age")
@PrefixGameTestTemplate(false)
public final class DebugAgeGameTests {
    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final List<ModPayloads.ClientboundSyncPlayerDataPayload> syncs=new ArrayList<>();
        Fixture(GameTestHelper h) {
            player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"age-test"),ClientInformation.createDefault());
            var connection=new Connection(PacketFlow.SERVERBOUND);channel=new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server,connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
                @Override public void send(Packet<?> packet) {
                    if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof ModPayloads.ClientboundSyncPlayerDataPayload sync)syncs.add(sync);
                }
            };
            player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5,2,3.5)));player.setOnGround(true);player.tickCount=1;
            // Register test connections so the actual UUID-based editor resolver and selectors run unchanged.
            players(player).add(player);playersById(player).put(player.getUUID(),player);
            seedOldLife(player);
        }
        int command(String command)throws Exception {
            return player.server.getCommands().getDispatcher().execute(command,player.createCommandSourceStack().withPermission(4).withSuppressedOutput());
        }
        @Override public void close() {
            ModPayloads.clearPendingLearning(player);QiDomainSystem.stop(player);CombatStateManager.clear(player);
            ServerEvents.stopMeditation(player);
            players(player).remove(player);playersById(player).remove(player.getUUID());
            player.serverLevel().getEntitiesOfClass(CultivatorCorpseEntity.class,player.getBoundingBox().inflate(8),
                    corpse->corpse.getPlayerUuid().filter(player.getUUID()::equals).isPresent()).forEach(CultivatorCorpseEntity::discard);
            player.discard();channel.close();
        }
    }
    @SuppressWarnings("unchecked") private static List<ServerPlayer> players(ServerPlayer p) {
        try{var field=PlayerList.class.getDeclaredField("players");field.setAccessible(true);return (List<ServerPlayer>)field.get(p.server.getPlayerList());}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @SuppressWarnings("unchecked") private static Map<UUID,ServerPlayer> playersById(ServerPlayer p) {
        try{var field=PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);return (Map<UUID,ServerPlayer>)field.get(p.server.getPlayerList());}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void seedOldLife(ServerPlayer player) {
        var data=ModAttachments.CultivationData.createDefault()
                .withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY)
                .withActiveCultivationMethod(CultivationMethods.CHANGQING_JUE)
                .withMethodProficiencies(Map.of(CultivationMethods.CHANGQING_JUE,700))
                .withSpiritRoots(List.of(SpiritRoots.WOOD),"heaven")
                .withKnownSpells(List.of(ModSpells.FIREBALL)).withCultivationProgress(1234).withQi(100000).withAddedSkillPoints(42)
                .debugAdjustStat("max_hp",120);
        int limit=CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge();
        ModAttachments.setData(player,data.withAgePenalty(limit-10));ServerEvents.syncPlayerData(player);
    }
    private static int remaining(ServerPlayer p) {
        var data=ModAttachments.getData(p);return CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge()-data.agePenalty();
    }
    private static void assertNewLife(GameTestHelper h,Fixture f) {
        var data=ModAttachments.getData(f.player);
        h.assertTrue(CultivationLevels.isMortal(data.cultivationLevel())&&data.agePenalty()==0&&remaining(f.player)>0,"New life is mortal with a fresh lifespan");
        h.assertTrue(data.cultivationProgress()==0&&data.knownSpells().isEmpty()&&data.methodProficiencies().isEmpty()
                &&CultivationMethods.NONE.equals(data.activeCultivationMethod())&&data.skillPoints()==0&&data.maxHpBonus()==0,"Previous cultivation, learned spells/methods and debug growth are reset");
        h.assertTrue(f.player.isAlive()&&f.player.getHealth()==f.player.getMaxHealth()&&!data.isMeditating()
                &&f.player.getPose()==Pose.STANDING,"Player starts alive, healthy and standing");
    }
    private static void editor(Fixture editor,Fixture target,int delta) throws Exception {
        var payload=new ModPayloads.ServerboundDebugAdjustStatPayload(target.player.getUUID(),"age",delta);
        IPayloadContext context=(IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class[]{IPayloadContext.class},
                (proxy,method,args)->switch(method.getName()) {
                    case "player" -> editor.player;
                    case "enqueueWork" -> {((Runnable)args[0]).run();yield CompletableFuture.completedFuture(null);}
                    default -> throw new AssertionError("Unexpected context call: "+method.getName());
                });
        var handler=Class.forName(ModPayloads.class.getName()+"$ModPayloadsHandler");
        var method=handler.getDeclaredMethod("handleDebugAdjustStatOnServer",payload.getClass(),IPayloadContext.class);
        method.setAccessible(true);method.invoke(null,payload,context);
    }

    @GameTest(template="fate_test") public static void commandSetZeroImmediatelyStartsANewLife(GameTestHelper h)throws Exception {
        try(var f=new Fixture(h)) {
            f.player.setHealth(3);
            h.assertTrue(f.command("cultivationdebug set @s age 0")==1,"Actual command counts the restarted target");
            assertNewLife(h,f);
            h.assertTrue(f.player.serverLevel().getEntitiesOfClass(CultivatorCorpseEntity.class,f.player.getBoundingBox().inflate(4),
                    corpse->corpse.getPlayerUuid().filter(f.player.getUUID()::equals).isPresent()).size()==1,"Exactly one previous-life corpse is created");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void commandAdjustCrossingZeroAndExtremeNegativeValuesRestart(GameTestHelper h)throws Exception {
        try(var f=new Fixture(h)) {
            h.assertTrue(f.command("cultivationdebug adjust @s age -10")==1,"Adjust reaches exact zero");assertNewLife(h,f);
            seedOldLife(f.player);f.command("cultivationdebug adjust @s age -2147483648");assertNewLife(h,f);
            seedOldLife(f.player);f.command("cultivationdebug set @s age -1");assertNewLife(h,f);
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void positiveAgeEditsAndHugeIncreasesDoNotResetCultivation(GameTestHelper h)throws Exception {
        try(var f=new Fixture(h)) {
            f.command("cultivationdebug set @s age 1");h.assertTrue(remaining(f.player)==1,"Positive lifespan of one remains editable");
            h.assertTrue(!CultivationLevels.isMortal(ModAttachments.getData(f.player).cultivationLevel()),"One year does not start a new life");
            f.command("cultivationdebug set @s age 18");f.command("cultivationdebug adjust @s age -7");
            h.assertTrue(remaining(f.player)==11&&ModAttachments.getData(f.player).knownSpells().contains(ModSpells.FIREBALL),"Positive edits preserve learned progress");
            f.command("cultivationdebug adjust @s age 2147483647");
            h.assertTrue(ModAttachments.getData(f.player).agePenalty()==0&&!CultivationLevels.isMortal(ModAttachments.getData(f.player).cultivationLevel()),"Huge age increases clamp safely without integer overflow or rebirth");
            h.assertTrue(f.command("cultivationdebug adjust @s age 0")==0,"Zero delta is a no-op");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void commandSelectorsRestartOnlyChosenPlayers(GameTestHelper h)throws Exception {
        try(var editor=new Fixture(h);var first=new Fixture(h);var second=new Fixture(h)) {
            String tag="age_"+UUID.randomUUID().toString().replace("-","");first.player.addTag(tag);second.player.addTag(tag);
            h.assertTrue(editor.command("cultivationdebug set @a[tag="+tag+"] age 0")==2,"Multi-target command reports both new lives");
            assertNewLife(h,first);assertNewLife(h,second);
            h.assertTrue(!CultivationLevels.isMortal(ModAttachments.getData(editor.player).cultivationLevel()),"Unselected editor keeps its current life");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void editorPacketRestartsSelectedTargetAndSynchronizesBothPlayers(GameTestHelper h)throws Exception {
        try(var editor=new Fixture(h);var target=new Fixture(h)) {
            editor.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            editor.syncs.clear();target.syncs.clear();editor(editor,target,-10);assertNewLife(h,target);
            h.assertTrue(!CultivationLevels.isMortal(ModAttachments.getData(editor.player).cultivationLevel()),"Remote edit does not reset the editor");
            h.assertTrue(editor.syncs.stream().anyMatch(sync->CultivationLevels.isMortal(sync.cultivationLevel()))
                    &&target.syncs.stream().anyMatch(sync->CultivationLevels.isMortal(sync.cultivationLevel())),"Fresh state reaches both target client and remote editor");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void editorStillRequiresItsItemAndPositiveEditsRemainOrdinary(GameTestHelper h)throws Exception {
        try(var editor=new Fixture(h);var target=new Fixture(h)) {
            var before=ModAttachments.getData(target.player);editor(editor,target,-10);
            h.assertTrue(ModAttachments.getData(target.player).equals(before),"Packet without editor item cannot end another player's life");
            editor.player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));editor(editor,target,-9);
            h.assertTrue(remaining(target.player)==1&&!CultivationLevels.isMortal(ModAttachments.getData(target.player).cultivationLevel()),"Offhand editor can reduce lifespan to one without resetting");
            editor(editor,target,-10);assertNewLife(h,target);
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void newLifeStopsOldQiAndMeditationAndResetsTheDailyAgeClock(GameTestHelper h)throws Exception {
        try(var f=new Fixture(h)) {
            CombatStateManager.setEnabled(f.player,true);CombatStateManager.toggleManualCombat(f.player);QiDomainSystem.toggle(f.player);
            h.assertTrue(QiDomainSystem.blasting(f.player),"Old life has an active Qi aura");
            var learner=ModAttachments.getData(f.player).withKnownSpells(List.of());
            ModAttachments.setData(f.player,learner);
            var spell=ModSpells.all().stream().filter(candidate->!ModSpells.isInnateKnown(candidate.id(),learner)
                    &&ModSpells.meetsRequirement(learner.cultivationLevel(),candidate)).findFirst().orElseThrow();
            h.assertTrue(ModPayloads.beginDunwuSteleLearning(f.player,spell.id()),"Old life has an unfinished learning session");
            var learningField=Class.forName(ModPayloads.class.getName()+"$ModPayloadsHandler").getDeclaredField("PENDING_LEARNING");
            learningField.setAccessible(true);
            Map<?,?> learning=(Map<?,?>)learningField.get(null);
            h.assertTrue(learning.containsKey(f.player.getUUID()),"Old learning session is registered");
            ModAttachments.setData(f.player,ModAttachments.getData(f.player).withMeditating(true));f.player.setPose(Pose.SITTING);
            f.player.getPersistentData().putLong("ImmortalCultivationLastAgeDay",-10000);
            f.command("cultivationdebug set @s age 0");assertNewLife(h,f);
            h.assertTrue(!QiDomainSystem.blasting(f.player)&&!CombatStateManager.isInCombat(f.player),"Old combat and aura end immediately");
            h.assertTrue(!learning.containsKey(f.player.getUUID()),"Old-life learning cannot complete after the reset");
            h.assertTrue(f.player.getPersistentData().getLong("ImmortalCultivationLastAgeDay")==f.player.server.overworld().getDayTime()/24000L,"New life does not inherit unprocessed old-life aging");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void explicitDebugNewLifeDoesNotRemainALivingCorpse(GameTestHelper h)throws Exception {
        try(var f=new Fixture(h)) {
            f.player.getPersistentData().putBoolean(CultivationAccessoryItem.LIVING_CORPSE_TAG,true);
            f.command("cultivationdebug set @s age 0");assertNewLife(h,f);
            h.assertTrue(!CultivationAccessoryItem.isLivingCorpse(f.player),"Explicit zero lifespan starts a living new life even for an old living corpse");
        }h.succeed();
    }
}
