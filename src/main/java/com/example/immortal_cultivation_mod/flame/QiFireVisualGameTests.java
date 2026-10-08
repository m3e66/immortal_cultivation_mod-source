package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class QiFireVisualGameTests {
    @GameTest(template = "fate_test")
    public static void animatedQiFireKeepsItsBurnAndDoesNotAcquireSpecialFireExpiry(GameTestHelper h) {
        BlockPos local = new BlockPos(3, 2, 3);
        BlockPos pos = h.absolutePos(local);
        h.setBlock(local, ModBlocks.LING_FIRE.get());
        var state = h.getLevel().getBlockState(pos);
        h.assertTrue(state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED,
                "Qi fire uses the animated flame renderer instead of vanilla cross planes");
        h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof SpiritFlameBlockEntity,
                "Placing Qi fire creates a registered shared render carrier");
        var flame = (SpiritFlameBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(flame.isQiFire() && flame.kind() == FlameKind.MORTAL,
                "Renderer distinguishes blue Qi fire from the eight special flame kinds");
        h.assertTrue(state.getCollisionShape(h.getLevel(), pos).isEmpty(), "Qi fire remains non-solid");
        var target = EntityType.COW.create(h.getLevel());
        state.entityInside(h.getLevel(), pos, target);
        h.assertTrue(target.isOnFire() && target.hasEffect(ModEffects.QI_FIRE_BURN),
                "The visual upgrade keeps the original Qi-burning contact effect");
        h.assertTrue(state.getTicker(h.getLevel(), ModBlockEntities.SPIRIT_FLAME.get()) == null,
                "Qi fire does not inherit the special flame's three-minute expiry ticker");
        var saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.putLong("ExpiresAt", 0);
        var restored = BlockEntity.loadStatic(pos, state, saved, h.getLevel().registryAccess());
        h.assertTrue(restored instanceof SpiritFlameBlockEntity, "Render carrier reloads with its registered block type");
        h.getLevel().setBlockEntity(restored);
        SpiritFlameBlockEntity.serverTick(h.getLevel(), pos, state, (SpiritFlameBlockEntity) restored);
        h.assertTrue(h.getLevel().getBlockState(pos).is(ModBlocks.LING_FIRE.get()),
                "Even a stale special-fire deadline cannot delete Qi fire");
        h.setBlock(local, Blocks.AIR);
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oldQiFireBlocksGainRenderCarriersOnChunkMigration(GameTestHelper h) {
        BlockPos local = new BlockPos(3, 2, 3);
        BlockPos pos = h.absolutePos(local);
        h.setBlock(local, ModBlocks.LING_FIRE.get());
        var chunk = h.getLevel().getChunkAt(pos);
        h.getLevel().removeBlockEntity(pos);
        h.assertTrue(!chunk.getBlockEntities().containsKey(pos), "Fixture represents an old Qi fire with no block entity");
        h.assertTrue(SpiritFlameBlockEntity.migrateLoadedChunk(h.getLevel(), chunk), "Migration accepts the loaded chunk");
        var repaired = chunk.getBlockEntities().get(pos);
        h.assertTrue(repaired instanceof SpiritFlameBlockEntity flame && flame.isQiFire() && flame.expiresAt() == -1,
                "Old placed Qi fires receive their blue animated carrier without an expiry deadline");
        SpiritFlameBlockEntity.migrateLoadedChunk(h.getLevel(), chunk);
        h.assertTrue(chunk.getBlockEntities().get(pos) == repaired, "Repeated migration preserves the carrier");
        h.assertTrue(ModBlocks.ZHEN_FIRE.get().defaultBlockState().getRenderShape() == RenderShape.MODEL,
                "Legacy True Fire keeps its existing renderer");
        h.setBlock(local, Blocks.AIR);
        h.succeed();
    }
}
