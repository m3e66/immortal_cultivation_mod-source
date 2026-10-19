package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_luck_rescue")
@PrefixGameTestTemplate(false)
public final class LuckRescueMatchupGameTests {
    private static FakePlayer player(GameTestHelper helper, int luck) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "luck-matchup"));
        var position = helper.absolutePos(new BlockPos(8, 2, 8));
        player.setPos(position.getX()+.5,position.getY(),position.getZ()+.5);
        ModAttachments.setData(player, ModAttachments.getData(player).withLuck(luck));
        return player;
    }

    @GameTest(template="fate_test")
    public static void playerMeleeKillsHighLuckMobsWithoutRescue(GameTestHelper h) {
        var attacker=player(h,0);
        for(var type:java.util.List.of(EntityType.COW,ModEntities.ZHUJI_CULTIVATOR.get())) {
            var victim=h.spawn(type,new BlockPos(8,2,10));victim.setNoAi(true);
            LuckSystem.change(victim,100-LuckSystem.luck(victim));
            var before=LuckSystem.data(victim).copy();var position=victim.position();victim.setHealth(0);
            var event=new LivingDeathEvent(victim,h.getLevel().damageSources().playerAttack(attacker));
            LuckSystem.miracle(event);
            h.assertTrue(!event.isCanceled()&&victim.getHealth()==0,"High-Luck mobs and cultivation NPCs cannot escape player melee through Luck");
            h.assertTrue(before.equals(LuckSystem.data(victim))&&position.equals(victim.position()),"No Luck spent, rescue effects or teleport on excluded matchup");
            victim.discard();
        }
        var cow=h.spawn(EntityType.COW,new BlockPos(9,2,10));cow.setNoAi(true);
        LuckSystem.change(cow,100-LuckSystem.luck(cow));
        cow.hurt(h.getLevel().damageSources().playerAttack(attacker),100000);
        h.assertTrue(cow.getHealth()<=0&&!cow.isAlive()&&LuckSystem.luck(cow)==100,"Actual lethal-damage event pipeline lets the mob die without charging Luck");
        cow.discard();attacker.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void playerOwnedProjectilesAndMagicDoNotRescueMobs(GameTestHelper h) {
        var attacker=player(h,0);var victim=h.spawn(EntityType.ZOMBIE,new BlockPos(8,2,10));victim.setNoAi(true);
        LuckSystem.change(victim,100-LuckSystem.luck(victim));
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(attacker);
        for(var source:java.util.List.of(h.getLevel().damageSources().arrow(arrow,attacker),
                h.getLevel().damageSources().indirectMagic(arrow,attacker))) {
            victim.setHealth(0);var before=LuckSystem.data(victim).copy();
            var event=new LivingDeathEvent(victim,source);LuckSystem.miracle(event);
            h.assertTrue(!event.isCanceled()&&victim.getHealth()==0&&before.equals(LuckSystem.data(victim)),
                    "Player attribution disables mob rescue for arrows and indirect spell damage too");
        }
        victim.discard();arrow.discard();attacker.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void pvpMeleeStillRescuesAndChargesLuckOnce(GameTestHelper h) {
        var attacker=player(h,0);var victim=player(h,100);victim.setHealth(0);
        var event=new LivingDeathEvent(victim,h.getLevel().damageSources().playerAttack(attacker));
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled()&&victim.isAlive()&&LuckSystem.luck(victim)>=85&&LuckSystem.luck(victim)<=90,
                "Registered PvP handler still restores life and spends the normal 10–15 Luck");
        int after=LuckSystem.luck(victim);LuckSystem.miracle(event);
        h.assertTrue(LuckSystem.luck(victim)==after,"Canceled PvP rescue cannot spend Luck again");
        attacker.discard();victim.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void pvpProjectilesStillRescue(GameTestHelper h) {
        var attacker=player(h,0);var victim=player(h,100);victim.setHealth(0);
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(attacker);
        var event=new LivingDeathEvent(victim,h.getLevel().damageSources().arrow(arrow,attacker));
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled()&&victim.isAlive()&&LuckSystem.luck(victim)<100,"Player victims retain Luck rescue against player-owned projectiles");
        attacker.discard();victim.discard();arrow.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void pvpEqualLuckCannotRescue(GameTestHelper h) {
        var attacker=player(h,100);var victim=player(h,100);victim.setHealth(0);
        var event=new LivingDeathEvent(victim,h.getLevel().damageSources().playerAttack(attacker));LuckSystem.miracle(event);
        h.assertTrue(!event.isCanceled()&&victim.getHealth()==0&&LuckSystem.luck(victim)==100,"PvP still requires a strict Luck advantage");
        attacker.discard();victim.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void mobAttacksOnPlayersAndEnvironmentalRulesAreUnchanged(GameTestHelper h) {
        FateGameTests.ordinaryMobProjectilesRescueButEnvironmentalDeathsDoNot(h);
    }
    @GameTest(template="fate_test")
    public static void mobAgainstMobRescueRulesAreUnchanged(GameTestHelper h) {
        FateGameTests.rescueRequiresOnlyStrictLuckAdvantage(h);
    }
    @GameTest(template="fate_test")
    public static void equipmentStillRunsBeforeLuck(GameTestHelper h) {
        FateDeathProtectionGameTests.bothDollsAreConsumedBeforePendantAndLuck(h);
    }
}
