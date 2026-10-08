package com.example.immortal_cultivation_mod.combat;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class CombatKnockbackGameTests {
    @GameTest(template = "npc_combat_arena")
    public static void spellsLaunchTargetsAwayFromAttacker(GameTestHelper h) {
        var attacker = h.spawn(EntityType.PIG, new BlockPos(4, 4, 4));
        var target = h.spawn(EntityType.COW, new BlockPos(4, 4, 6));
        attacker.setNoAi(true); target.setNoAi(true);
        target.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(target.hurt(target.damageSources().indirectMagic(attacker, attacker), 4), "Spell hit accepted");
        CombatKnockback.tick(new EntityTickEvent.Pre(target));
        h.assertTrue(target.getDeltaMovement().z > .3, "Magic impact pushes away from the caster");
        h.assertTrue(CombatKnockback.strength(100) > CombatKnockback.strength(4), "Heavy hits launch harder");
        h.assertTrue(CombatKnockback.blockBudget(100) > CombatKnockback.blockBudget(4)
                && CombatKnockback.blockBudget(Float.MAX_VALUE) == 256
                && CombatKnockback.blockBudget(Float.NaN) == 0, "Damage-scaled terrain work is finite and capped");
        attacker.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void launchedPlayerBreaksContactButNotBedrock(GameTestHelper h) {
        var attacker = h.spawn(EntityType.PIG, new BlockPos(4, 5, 3));
        attacker.setNoAi(true);
        var target = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "slam-target")) {
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
        };
        attacker.setPos(h.absoluteVec(new Vec3(4.5, 5, 3.5)));
        target.setPos(h.absoluteVec(new Vec3(4.5, 5, 5.5)));
        target.setOnGround(true);
        target.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            var immunity = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            immunity.setAccessible(true); immunity.setInt(target, 0);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        h.setBlock(new BlockPos(4, 5, 6), Blocks.STONE);
        h.setBlock(new BlockPos(4, 6, 6), Blocks.BEDROCK);
        h.assertTrue(target.hurt(target.damageSources().mobAttack(attacker), 8), "Player impact accepted");
        CombatKnockback.tick(new EntityTickEvent.Pre(target));
        h.assertTrue(h.getBlockState(new BlockPos(4, 5, 6)).isAir(), "Knockback collision removes contact stone");
        h.assertTrue(h.getBlockState(new BlockPos(4, 6, 6)).is(Blocks.BEDROCK), "Unbreakable contact survives");
        attacker.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void movementWithoutAttackCannotBreakTerrain(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "slam-walker"));
        player.setPos(h.absoluteVec(new Vec3(4.5, 5, 5.5)));
        player.setDeltaMovement(0, 0, 2);
        h.setBlock(new BlockPos(4, 5, 6), Blocks.STONE);
        CombatKnockback.tick(new EntityTickEvent.Pre(player));
        h.assertTrue(h.getBlockState(new BlockPos(4, 5, 6)).is(Blocks.STONE), "Sprinting is not an impact");
        h.assertTrue(ExternalQiSize.scale(player) == 1 && ExternalQiSize.clampPercent(Integer.MAX_VALUE) == 200
                && ExternalQiSize.clampPercent(Integer.MIN_VALUE) == 10, "Size starts at normal and clamps network input");
        player.discard(); h.succeed();
    }
}
