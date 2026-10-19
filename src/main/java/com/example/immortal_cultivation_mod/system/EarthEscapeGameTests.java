package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_earth_escape")
@PrefixGameTestTemplate(false)
public final class EarthEscapeGameTests {
    private static void allowDamage(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(false);
        // Fixtures do not run ServerPlayer ticks that normally expire spawn protection.
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    @GameTest(template = "fate_test")
    public static void undergroundSpellSurvivesVanillaSuffocationChecks(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            var player = fixture.player;
            allowDamage(player);
            var feet = helper.absolutePos(new BlockPos(3, 2, 3));
            for (var pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 2, 1))) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            player.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            player.addEffect(new MobEffectInstance(ModEffects.EARTH_ESCAPE, 200));
            player.noPhysics = false;
            helper.assertTrue(player.isInWall(), "Fixture embeds the player's head in solid stone");
            float health = player.getHealth();
            for (int tick = 0; tick < 60; tick++) {
                player.invulnerableTime = 0;
                player.noPhysics = false;
                player.baseTick();
            }
            helper.assertTrue(player.getHealth() == health,
                    "Active Earth Escape prevents repeated vanilla suffocation even after noPhysics resets");
            player.removeEffect(ModEffects.EARTH_ESCAPE);
            player.invulnerableTime = 0;
            player.baseTick();
            helper.assertTrue(player.getHealth() < health,
                    "Removing Earth Escape immediately restores normal suffocation damage");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void earthEscapeDoesNotGrantGeneralDamageImmunity(GameTestHelper helper) {
        try (var fixture = new LifeSimulationGameTests.Player(helper)) {
            var player = fixture.player;
            allowDamage(player);
            player.addEffect(new MobEffectInstance(ModEffects.EARTH_ESCAPE, 200));
            float health = player.getHealth();
            player.hurt(player.damageSources().magic(), 2);
            helper.assertTrue(player.getHealth() < health, "Magic still damages a phasing player");
            health = player.getHealth();
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().drown(), 2);
            helper.assertTrue(player.getHealth() < health, "Drowning remains separate from suffocation inside blocks");
            health = player.getHealth();
            player.invulnerableTime = 0;
            var attacker = helper.spawn(EntityType.COW, 1, 2, 1);
            player.hurt(player.damageSources().mobAttack(attacker), 2);
            helper.assertTrue(player.getHealth() < health, "Melee damage is not blocked by Earth Escape");
        }
        helper.succeed();
    }
}
