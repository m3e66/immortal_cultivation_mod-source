package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_terrain_protection")
@PrefixGameTestTemplate(false)
public final class CombatBlockProtectionGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "terrain-protection"));
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new Vec3(4, 6, 4)));
        return p;
    }
    private static Block[] breakableBlocks() {
        return new Block[] { Blocks.CHEST, Blocks.BARREL, Blocks.SHULKER_BOX, Blocks.HOPPER,
                Blocks.FURNACE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.CRAFTING_TABLE,
                ModBlocks.CU_TIE_XIAODING.get(), ModBlocks.MEDICINE_CABINET_SMALL.get(),
                ModBlocks.MEDICINE_CABINET_PART.get(),
                com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_ORE.get() };
    }
    @GameTest(template="npc_combat_arena")
    public static void allCombatCategoriesBreakOresContainersAndWorkstations(GameTestHelper h) {
        var p = player(h);
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 6, 8));
        try {
            for (Block block : breakableBlocks()) {
                for (var category : BlockBreakingSettings.Category.values()) {
                    level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
                    h.assertTrue(SpellDestruction.canBreakBlock(level, pos, p)
                            && SpellDestruction.canExternalQiBreakBlock(level, pos, p), "Projectile checks allow " + block);
                    h.assertTrue(BlockBreakingSettings.withSource(category, p,
                            () -> SpellDestruction.eraseImpactBlockNow(level, pos, p)), "Impact breaks " + block + " for " + category);
                    h.assertTrue(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null, "No block or stale block entity remains");
                }
            }
            level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertTrue(!SpellDestruction.eraseImpactBlockNow(level, pos, p), "Bedrock remains unbreakable");
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            BlockBreakingSettings.setDisabledMask(p, BlockBreakingSettings.Category.ALL.bit());
            h.assertTrue(!SpellDestruction.eraseImpactBlockNow(level, pos, p), "Disable setting still prevents container destruction");
            BlockBreakingSettings.setDisabledMask(p, 0);
            p.setGameMode(GameType.CREATIVE);
            for (Block block : new Block[] {Blocks.CHEST, Blocks.CAMPFIRE,
                    com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_ORE.get()}) {
                level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
                h.assertTrue(p.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "Manual mining remains available for " + block);
            }
        } finally { p.discard(); }
        h.succeed();
    }
    @GameTest(template="npc_combat_arena", timeoutTicks=100)
    public static void deferredAreaDestroysStoneContainersAndOre(GameTestHelper h) {
        var p = player(h);
        var level = h.getLevel();
        var start = h.absolutePos(new BlockPos(7, 6, 7));
        Block[] blocks = breakableBlocks();
        for (int i=0; i<blocks.length; i++) level.setBlock(start.offset(i, 0, 0), blocks[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        var stone = start.offset(blocks.length, 0, 0);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        SpellDestruction.area(level, p, new AABB(Vec3.atLowerCornerOf(start), Vec3.atLowerCornerOf(stone.offset(1, 1, 1))),
                p.position(), 100, Float.MAX_VALUE, pos -> true, null);
        h.runAfterDelay(8, () -> {
            try {
                for (int i=0; i<blocks.length; i++) h.assertTrue(level.getBlockState(start.offset(i, 0, 0)).isAir(), "Deferred job destroys " + blocks[i]);
                h.assertTrue(level.getBlockState(stone).isAir(), "Ordinary terrain still breaks");
                h.succeed();
            } finally { p.discard(); }
        });
    }
    @GameTest(template="npc_combat_arena")
    public static void externalQiQueueRegression(GameTestHelper h) throws Exception {
        SpellDestructionGameTests.externalQiQueueKeepsItsBlockPolicy(h);
    }
    @GameTest(template="npc_combat_arena")
    public static void realExternalQiProjectileDestroysWorkstation(GameTestHelper h) {
        com.example.immortal_cultivation_mod.entity.ExternalQiDestructionGameTests.externalQiCutsTerrainAndModWorkstations(h);
    }
}
