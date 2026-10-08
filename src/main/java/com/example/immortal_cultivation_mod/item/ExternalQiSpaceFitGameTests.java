package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.ExternalQiSpaceFit;
import com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity;
import com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ExternalQiSpaceFitGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "space-fit"));
        player.setPos(helper.absoluteVec(new Vec3(12.5, 3, 12.5)));
        return player;
    }

    private static void clearRoom(GameTestHelper helper) {
        for (int x = 5; x <= 20; x++) for (int z = 5; z <= 20; z++) {
            helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
            for (int y = 3; y <= 11; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    private static void walls(GameTestHelper helper, boolean present) {
        for (int x : new int[]{11, 13}) for (int z = 8; z <= 17; z++) for (int y = 3; y <= 7; y++) {
            helper.setBlock(new BlockPos(x, y, z), present ? Blocks.STONE : Blocks.AIR);
        }
    }

    @GameTest(template = "npc_combat_arena")
    public static void spaceFitDetectsEnclosuresButNotOutdoorGround(GameTestHelper helper) {
        clearRoom(helper);
        var player = player(helper);
        Vec3 forward = new Vec3(0, 0, 1);
        helper.assertTrue(QiInfusedWeapon.outwardQiAutoFitEnabled(player), "Space fitting defaults on");
        helper.assertTrue(ExternalQiSpaceFit.fit(player, forward, 2, 4, false) == 4,
                "Ordinary outdoor ground does not shrink external Qi");
        walls(helper, true);
        for (int stage = 0; stage < 3; stage++) {
            double fitted = ExternalQiSpaceFit.fit(player, forward, stage, 4, false);
            Vec3 axis = ExternalQiVfxShape.axis(forward, stage);
            Vec3 perpendicular = forward.cross(axis).normalize();
            double horizontal = ExternalQiVfxShape.bladeReach(fitted) * Math.abs(axis.x)
                    + ExternalQiVfxShape.halfThickness(fitted) * Math.abs(perpendicular.x);
            helper.assertTrue(fitted < 1 && horizontal <= .42001,
                    "Every blade orientation, including its tips and thickness, fits the one-block corridor");
        }
        QiInfusedWeapon.setOutwardQiAutoFit(player, false);
        helper.assertTrue(ExternalQiSpaceFit.fit(player, forward, 2, 4, false) == 4, "Disabled setting keeps full size");
        var copy = player(helper);
        QiInfusedWeapon.copyPersistentData(player, copy);
        helper.assertTrue(!QiInfusedWeapon.outwardQiAutoFitEnabled(copy), "Disabled preference survives player cloning");
        copy.discard();
        QiInfusedWeapon.setOutwardQiAutoFit(player, true);
        walls(helper, false);
        for (int x = 9; x <= 15; x++) for (int z = 9; z <= 15; z++) helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
        double roofFit = ExternalQiSpaceFit.fit(player, forward, 0, 4, false);
        helper.assertTrue(roofFit < 4, "A low ceiling also limits diagonal slashes");
        double coneFit = ExternalQiSpaceFit.fit(player, forward, 2, 4, true);
        helper.assertTrue(coneFit <= .73, "Cone radius fits below the ceiling too");
        player.discard(); helper.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void fittedReleaseKeepsDamageCostAndSavedSize(GameTestHelper helper) {
        clearRoom(helper); walls(helper, true);
        var player = player(helper);
        player.setYRot(0); player.setXRot(0);
        var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        QiInfusedWeapon.setOutwardQiRelease(player, true);
        com.example.immortal_cultivation_mod.combat.CombatStateManager.enterCombat(player);
        CompoundTag full = null;
        for (boolean enabled : new boolean[]{false, true}) {
            QiInfusedWeapon.setOutwardQiAutoFit(player, enabled); QiInfusedWeapon.setQi(sword, 1000);
            helper.assertTrue(WeaponQiSlashEntity.release(player, 2, false), "Real player release succeeds");
            var shot = helper.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, player.getBoundingBox().inflate(3)).getFirst();
            var tag = new CompoundTag(); shot.saveWithoutId(tag);
            if (!enabled) full = tag;
            else {
                helper.assertTrue(tag.getDouble("Radius") < full.getDouble("Radius") && tag.getDouble("Radius") < .2,
                        "Real release applies a radius small enough for the corridor");
                helper.assertTrue(tag.getFloat("Damage") == full.getFloat("Damage")
                                && tag.getInt("RemainingBlocks") == full.getInt("RemainingBlocks")
                                && tag.getDouble("MaxDistance") == full.getDouble("MaxDistance")
                                && tag.getInt("LaunchPassThroughTicks") == 0,
                        "Fitting preserves damage, destruction capacity, range, and launch pass-through");
                shot.load(tag);
                var saved = new CompoundTag(); shot.saveWithoutId(saved);
                helper.assertTrue(saved.getDouble("Radius") == tag.getDouble("Radius"), "Tiny fitted radius survives reload");
            }
            helper.assertTrue(QiInfusedWeapon.getQi(sword) == 900, "Fitting keeps the existing Qi payment");
            shot.discard();
        }
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            for (boolean enabled : new boolean[]{false, true}) {
                var expected = new CombatPayloads.ServerboundSetOutwardQiPayload(true, false, enabled);
                CombatPayloads.ServerboundSetOutwardQiPayload.STREAM_CODEC.encode(buffer, expected);
                helper.assertTrue(expected.equals(CombatPayloads.ServerboundSetOutwardQiPayload.STREAM_CODEC.decode(buffer)),
                        "Setting packet retains all three independent toggles");
                buffer.clear();
            }
        } finally { buffer.release(); }
        player.discard(); helper.succeed();
    }
}
