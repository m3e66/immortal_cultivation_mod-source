package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_npc_target_priority")
@PrefixGameTestTemplate(false)
public final class NpcTargetPriorityGameTests {
    @GameTest(template="npc_combat_arena")
    public static void protectedModesOverrideHatredRevengeSparAndNpcEffects(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            prepare(h, npc);
            var player = player(h, npc, true);
            var profile = NpcProfile.cultivationProfile(npc);
            var relation = profile.getCompound("Relationships").getCompound(player.getUUID().toString());
            relation.putInt("Hatred", 100);
            relation.putLong("RetaliateUntil", h.getLevel().getGameTime() + 600);
            npc.setTarget(player);
            npc.setLastHurtByMob(player);
            profile.putUUID("SparPlayer", player.getUUID());
            for (var mode : List.of(GameType.CREATIVE, GameType.SPECTATOR)) {
                player.setGameMode(mode);
                h.assertTrue(!NpcProfile.shouldAttackPlayer(npc, player), "Protected mode overrides all hostility: " + mode);
                h.assertTrue(!NpcProfile.isCombatEnemy(npc, player), "Stale target, revenge and spar cannot bypass protected mode");
                h.assertTrue(!NpcProfile.canNpcProjectileHit(npc, player), "NPC-owned projectiles cannot hit protected players");
                h.assertTrue(NpcProfile.isNonCombatNpcDamage(npc.damageSources().mobAttack(npc), player),
                        "Incoming NPC damage is rejected for protected players");
                h.assertTrue(!new NpcHurtByTargetGoal(npc).canUse(), "Revenge goal cannot target protected players");
            }
            profile.remove("SparPlayer");
            player.setGameMode(GameType.SURVIVAL);
            h.assertTrue(NpcProfile.shouldAttackPlayer(npc, player), "Mode changes preserve old hostility for Survival");
            player.setGameMode(GameType.ADVENTURE);
            h.assertTrue(NpcProfile.shouldAttackPlayer(npc, player), "Adventure combat remains enabled");
            npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void creativeHitsDoNotStartRetaliation(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, npc);
        var player = player(h, npc, false);
        player.setGameMode(GameType.CREATIVE);
        NpcProfile.recordPlayerAttack(npc, player);
        h.assertTrue(npc.getTarget() == null, "Creative hits do not assign a retaliation target");
        h.assertTrue(!NpcProfile.cultivationProfile(npc).hasUUID("RetaliationPlayer"), "Creative hits do not queue retaliation");
        var brain = new NpcCombatBrain(npc);
        brain.recordDamage(npc.damageSources().playerAttack(player), 100);
        npc.setLastHurtByMob(player);
        brain.tick();
        h.assertTrue(npc.getTarget() == null && npc.getLastHurtByMob() == null, "Brain discards protected revenge targets");
        npc.discard();
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void modeSwitchCancelsRealSpellWindupWithoutSpendingQi(GameTestHelper h) {
        var npc = NpcCombatGameTests.npc(h);
        npc.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        var player = player(h, npc, true);
        player.setPos(npc.position().add(5, 0, 0));
        var spells = new ListTag();
        spells.add(StringTag.valueOf(com.example.immortal_cultivation_mod.spell.ModSpells.FIREBALL));
        NpcProfile.cultivationProfile(npc).put("Spells", spells);
        npc.setTarget(player);
        var brain = new NpcCombatBrain(npc);
        int qi = NpcProfile.qi(npc, NpcProfile.LIANQI);
        brain.tick();
        h.assertTrue(npc.npcCastingTicks() > 0, "Survival enemy starts a real spell windup");
        player.setGameMode(GameType.CREATIVE);
        brain.tick();
        h.assertTrue(npc.getTarget() == null && npc.npcCastingTicks() == 0,
                "Switching to Creative immediately cancels locked windup and target");
        h.assertTrue(npc.getNavigation().isDone(), "NPC stops chasing protected target");
        h.assertTrue(NpcProfile.qi(npc, NpcProfile.LIANQI) == qi, "Canceled spell spends no Qi");
        h.assertTrue(!NpcSpellcasting.cast(npc, player, com.example.immortal_cultivation_mod.spell.ModSpells.FIREBALL),
                "Direct NPC spell release also rejects Creative players");
        npc.discard();
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void protectedPlayerCannotPreventBeastDefense(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            prepare(h, npc);
            var player = player(h, npc, true);
            player.setGameMode(GameType.CREATIVE);
            npc.setTarget(player);
            npc.setLastHurtByMob(player);
            var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
            beast.setNoAi(true);
            beast.setPos(npc.position().add(8, 0, 0));
            h.getLevel().addFreshEntity(beast);
            new NpcCombatBrain(npc).tick();
            h.assertTrue(npc.getTarget() == beast, "Dropping Creative player still allows immediate beast defense");
            beast.discard();
            new NpcCombatBrain(npc).tick();
            h.assertTrue(npc.getTarget() == null, "NPC does not resume attacking Creative player after beast is gone");
            npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void unengagedNpcPrioritizesBeastsAndResumesPlayerAfterward(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            prepare(h, npc);
            var player = player(h, npc, true);
            var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
            beast.setNoAi(true);
            beast.setPos(npc.position().add(16, 0, 0));
            h.getLevel().addFreshEntity(beast);
            npc.setLastHurtByMob(player);
            var brain = new NpcCombatBrain(npc);
            brain.recordDamage(npc.damageSources().playerAttack(player), 100000);
            brain.tick();
            h.assertTrue(npc.getTarget() == beast, "Unengaged NPC selects beast before a closer hostile player for " + type);
            npc.tickCount = 1;
            npc.setLastHurtByMob(player);
            h.assertTrue(!new NpcHurtByTargetGoal(npc).canUse(), "Player retaliation cannot steal the beast target");
            NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
            h.assertTrue(npc.getTarget() == beast, "Social control preserves beast combat");
            beast.discard();
            h.assertTrue(!NpcCombatBrain.hasValidCombatTarget(npc), "Removed beast no longer locks player targeting");
            new NpcCombatBrain(npc).tick();
            h.assertTrue(npc.getTarget() == player, "Hostile player becomes target after beast is gone for " + type);
            npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void everyTierKeepsPlayerWhenBeastArrivesOrAttacks(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            prepare(h, npc);
            var player = player(h, npc, true);
            player.setInvulnerable(true);
            npc.setTarget(player);
            var brain = new NpcCombatBrain(npc);
            brain.tick();
            var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
            beast.setNoAi(true);
            beast.setPos(npc.position().add(3, 0, 0));
            h.getLevel().addFreshEntity(beast);
            npc.tickCount = 1;
            npc.setLastHurtByMob(beast);
            brain.recordDamage(npc.damageSources().mobAttack(beast), 100000);
            h.assertTrue(!new NpcHurtByTargetGoal(npc).canUse(), "Beast retaliation cannot steal an existing player target");
            // Fresh decision state forces a full candidate scan, rather than passing on an action lock.
            new NpcCombatBrain(npc).tick();
            NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
            h.assertTrue(npc.getTarget() == player, "Player stays selected despite closer, high-damage beast for " + type);
            player.setGameMode(GameType.CREATIVE);
            new NpcCombatBrain(npc).tick();
            h.assertTrue(npc.getTarget() == beast, "Protected player releases the lock and allows beast defense");
            beast.discard(); npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void everyTierKeepsNpcOpponentUntilItBecomesInvalid(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            for (String reason : List.of("dead", "distant", "allied")) {
                var npc = type.create(h.getLevel());
                prepare(h, npc);
                var opponent = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
                prepare(h, opponent);
                makeOpponents(npc, opponent);
                opponent.setPos(npc.position().add(6, 0, 0));
                opponent.setTarget(npc);
                npc.setTarget(opponent);
                var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
                beast.setNoAi(true);
                beast.setPos(npc.position().add(3, 0, 0));
                h.getLevel().addFreshEntity(beast);
                npc.tickCount = 1;
                npc.setLastHurtByMob(beast);
                h.assertTrue(!new NpcHurtByTargetGoal(npc).canUse(), "Beast revenge cannot replace an ongoing NPC duel");
                new NpcCombatBrain(npc).tick();
                h.assertTrue(npc.getTarget() == opponent, "NPC duel survives beast arrival for " + type);
                if (reason.equals("dead")) opponent.setHealth(0);
                else if (reason.equals("distant")) opponent.setPos(npc.position().add(80, 0, 0));
                else {
                    NpcProfile.cultivationProfile(opponent).putString("Identity", "wandering_cultivator");
                    h.assertTrue(NpcProfile.areCombatAllies(npc, opponent), "Fixture really becomes allied before releasing its target");
                }
                new NpcCombatBrain(npc).tick();
                h.assertTrue(npc.getTarget() == beast, "Invalid opponent releases the target lock: " + reason);
                beast.discard(); opponent.discard(); npc.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void queuedPlayerRetaliationDoesNotReplaceNpcDuel(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, npc);
        var opponent = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, opponent);
        makeOpponents(npc, opponent);
        opponent.setPos(npc.position().add(6, 0, 0));
        npc.setTarget(opponent);
        var player = player(h, npc, true);
        h.getLevel().addNewPlayer(player);
        try {
            npc.tickCount = 10;
            var relation = NpcProfile.cultivationProfile(npc).getCompound("Relationships").getCompound(player.getUUID().toString());
            relation.putInt("Hatred", 100);
            NpcProfile.recordPlayerAttack(npc, player);
            h.assertTrue(npc.getTarget() == opponent, "Immediate player retaliation cannot overwrite the duel");
            NpcProfile.tickCultivation(npc, NpcProfile.LIANQI);
            h.assertTrue(npc.getTarget() == opponent, "Retaliation and hatred scans preserve the active NPC opponent");
            opponent.setHealth(0);
            NpcProfile.tickCultivation(npc, NpcProfile.LIANQI);
            h.assertTrue(npc.getTarget() == player, "Queued retaliation can resume after the duel ends");
        } finally {
            player.discard(); opponent.discard(); npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void finishedDuelChoosesBeastBeforeQueuedPlayerAndNpc(GameTestHelper h) {
        for (var type : List.of(ModEntities.LIANQI_CULTIVATOR.get(), ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            prepare(h, npc);
            var opponent = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
            prepare(h, opponent);
            makeOpponents(npc, opponent);
            opponent.setPos(npc.position().add(6, 0, 0));
            npc.setTarget(opponent);
            var nextNpc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
            prepare(h, nextNpc);
            makeOpponents(npc, nextNpc);
            nextNpc.setNoAi(true);
            nextNpc.setPos(npc.position().add(4, 0, 0));
            nextNpc.setTarget(npc);
            var player = player(h, npc, true);
            var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
            beast.setNoAi(true);
            beast.setPos(npc.position().add(12, 0, 0));
            var secondBeast = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
            secondBeast.setNoAi(true);
            secondBeast.setPos(npc.position().add(0, 0, 16));
            h.getLevel().addFreshEntity(nextNpc);
            h.getLevel().addFreshEntity(beast);
            h.getLevel().addFreshEntity(secondBeast);
            h.getLevel().addNewPlayer(player);
            try {
                NpcProfile.cultivationProfile(npc).putUUID("RetaliationPlayer", player.getUUID());
                NpcProfile.cultivationProfile(npc).getCompound("Relationships")
                        .getCompound(player.getUUID().toString()).putInt("Hatred", 100);
                npc.tickCount = 10;
                NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
                h.assertTrue(npc.getTarget() == opponent, "Beast waits until current duel ends");
                opponent.setHealth(0);
                NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
                h.assertTrue(npc.getTarget() == beast, "Beast precedes queued retaliation, hatred scan and another NPC after death");
                new NpcCombatBrain(npc).tick();
                h.assertTrue(npc.getTarget() == beast, "Combat brain keeps the beast selected by social acquisition");
                beast.setHealth(0);
                NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
                h.assertTrue(npc.getTarget() == secondBeast, "Remaining beast is chosen before returning to players or NPCs");
                secondBeast.setHealth(0);
                NpcProfile.tickCultivation(npc, ((CultivationNpc) npc).npcArchetype());
                h.assertTrue(npc.getTarget() == player, "Player becomes eligible after beast is gone");
            } finally {
                player.discard(); nextNpc.discard(); beast.discard(); secondBeast.discard(); opponent.discard(); npc.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void revengeAndFreshPlayerHitCannotBypassBeastsAfterDeath(GameTestHelper h) {
        for (boolean revengeGoal : List.of(false, true)) {
            var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
            prepare(h, npc);
            var oldPlayer = player(h, npc, true);
            oldPlayer.setHealth(0);
            npc.setTarget(oldPlayer);
            var player = player(h, npc, true);
            var beast = ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
            beast.setNoAi(true);
            beast.setPos(npc.position().add(12, 0, 0));
            h.getLevel().addFreshEntity(beast);
            try {
                npc.tickCount = 10;
                npc.setLastHurtByMob(player);
                if (revengeGoal) {
                    h.assertTrue(!new NpcHurtByTargetGoal(npc).canUse(), "Player revenge goal yields to a beast after previous target dies");
                } else {
                    NpcProfile.recordPlayerAttack(npc, player);
                }
                h.assertTrue(npc.getTarget() == beast, "Fresh hostility cannot lock a new player before checking beasts");
            } finally {
                beast.discard(); npc.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void wildXuanjinOutranksPlayerAfterPreviousTargetDies(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, npc);
        var oldPlayer = player(h, npc, true);
        oldPlayer.setHealth(0);
        npc.setTarget(oldPlayer);
        var player = player(h, npc, true);
        npc.setLastHurtByMob(player);
        var beast = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        beast.setNoAi(true);
        beast.setPos(npc.position().add(16, 0, 0));
        h.getLevel().addFreshEntity(beast);
        try {
            var brain = new NpcCombatBrain(npc);
            brain.recordDamage(npc.damageSources().playerAttack(player), 100000);
            brain.tick();
            h.assertTrue(npc.getTarget() == beast, "Wild Xuanjin is a beast priority even though it does not implement RealmBeast");
            beast.bind(player, 2);
            npc.setTarget(null);
            NpcCombatBrain.acquireCombatTarget(npc, player);
            h.assertTrue(npc.getTarget() == player, "Peaceful bound Xuanjin is not hunted during reacquisition");
        } finally {
            beast.discard(); npc.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void peacefulPlayersDoNotBecomeEnemiesAfterBeastDies(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, npc);
        var player = player(h, npc, false);
        var beast = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        beast.setNoAi(true);
        beast.setPos(npc.position().add(8, 0, 0));
        h.getLevel().addFreshEntity(beast);
        npc.setLastHurtByMob(player);
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget() == beast, "Wild beast is targeted before peaceful player");
        beast.discard();
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget() == null, "Finishing a beast does not make a peaceful player hostile");
        npc.discard();
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void peacefulBoundBeastIsNotHuntedInsteadOfHostilePlayer(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        prepare(h, npc);
        var player = player(h, npc, true);
        var beast = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        beast.bind(player, 2);
        beast.setNoAi(true);
        beast.setPos(npc.position().add(8, 0, 0));
        h.getLevel().addFreshEntity(beast);
        npc.setTarget(player);
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget() == player, "Peaceful bound beast does not enter the hostile candidate set");
        beast.discard(); npc.discard();
        h.succeed();
    }

    private static void makeOpponents(PathfinderMob npc, PathfinderMob opponent) {
        NpcProfile.cultivationProfile(npc).putString("Identity", "wandering_cultivator");
        NpcProfile.cultivationProfile(npc).putString("Name", "A");
        NpcProfile.cultivationProfile(opponent).putString("Identity", "sword_cultivator");
        NpcProfile.cultivationProfile(opponent).putString("Name", "B");
    }

    private static void prepare(GameTestHelper h, PathfinderMob npc) {
        NpcProfile.ensure(npc, ((CultivationNpc) npc).npcArchetype());
        var profile = NpcProfile.cultivationProfile(npc);
        profile.putString("Realm", CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY);
        profile.putString("Personality", "gentle");
        npc.setPos(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        npc.setHealth(npc.getMaxHealth());
    }

    private static FakePlayer player(GameTestHelper h, PathfinderMob npc, boolean hostile) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "target-priority"));
        player.setPos(npc.position().add(2, 0, 0));
        var relation = new CompoundTag();
        relation.putBoolean("Initialized", true);
        relation.putBoolean("PermanentEnemy", hostile);
        NpcProfile.cultivationProfile(npc).getCompound("Relationships").put(player.getUUID().toString(), relation);
        return player;
    }
}
