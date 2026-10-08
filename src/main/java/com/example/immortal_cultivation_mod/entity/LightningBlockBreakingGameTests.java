package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings.Category;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class LightningBlockBreakingGameTests {
    @GameTest(template = "npc_combat_arena")
    public static void disabledSpellLightningPreservesCopperAndRodsButStillStrikesEntities(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "safe-lightning-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1, 5, 1)));
        BlockPos copper = h.absolutePos(new BlockPos(7, 3, 7));
        BlockPos rod = copper.above();
        var target = EntityType.COW.create(h.getLevel());
        h.assertTrue(target != null, "Lightning test target can be created");
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(Vec3.atBottomCenterOf(rod).add(.75, 0, .75));
        h.getLevel().addFreshEntity(target);
        var search = new AABB(Vec3.atCenterOf(rod), Vec3.atCenterOf(rod)).inflate(4);
        try {
            for (Category disabled : new Category[]{Category.SPELLS, Category.ALL}) {
                h.getLevel().setBlock(copper, Blocks.OXIDIZED_COPPER.defaultBlockState(), Block.UPDATE_CLIENTS);
                h.getLevel().setBlock(rod, Blocks.LIGHTNING_ROD.defaultBlockState(), Block.UPDATE_CLIENTS);
                target.setHealth(200);
                target.invulnerableTime = 0;
                target.setRemainingFireTicks(-1);
                BlockBreakingSettings.setDisabledMask(player, disabled.bit());
                var shot = new LightningProjectileEntity(h.getLevel(), player, false);
                shot.onHit(new BlockHitResult(Vec3.atCenterOf(rod).add(0, .5, 0), Direction.UP, rod, false));
                var bolts = h.getLevel().getEntitiesOfClass(LightningBolt.class, search);
                h.assertTrue(bolts.size() == 1, "Small lightning spell creates its normal single bolt");
                for (var bolt : bolts) bolt.tick();
                h.assertTrue(target.getHealth() == 195 && target.isOnFire(),
                        "Terrain preference preserves vanilla lightning damage and ignition");
                h.assertTrue(h.getLevel().getBlockState(copper).is(Blocks.OXIDIZED_COPPER)
                                && !h.getLevel().getBlockState(rod).getValue(LightningRodBlock.POWERED),
                        "Disabled lightning cannot clean copper or activate lightning rods");
                for (var bolt : bolts) {
                    for (int tick = 0; tick < 80 && !bolt.isRemoved(); tick++) bolt.tick();
                    h.assertTrue(bolt.isRemoved() && bolt.getBlocksSetOnFire() == 0,
                            "All subsequent lightning flashes remain non-destructive and expire normally");
                }
            }
        } finally {
            target.discard();
            player.discard();
            h.getLevel().getEntitiesOfClass(LightningBolt.class, search).forEach(LightningBolt::discard);
            h.getLevel().setBlock(copper, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.getLevel().setBlock(rod, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        h.succeed();
    }
}
