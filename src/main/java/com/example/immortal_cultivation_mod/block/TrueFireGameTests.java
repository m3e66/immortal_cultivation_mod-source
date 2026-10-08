package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class TrueFireGameTests {
    @GameTest(template = "fate_test")
    public static void removedTinderHasNoItemOrCraftingRecipe(GameTestHelper h) {
        var id = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "zhen_fire_tinder");
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(id),
                "True Fire Tinder is no longer a registered item");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isEmpty(),
                "The removed tinder has no crafting recipe");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void legacyTrueFirePersistsWithoutSpreading(GameTestHelper h) {
        BlockPos support = h.absolutePos(new BlockPos(3, 2, 3));
        BlockPos firePos = support.above();
        h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(firePos, ModBlocks.ZHEN_FIRE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(firePos.east(), Blocks.OAK_PLANKS.defaultBlockState());
        h.getLevel().setBlockAndUpdate(firePos.east().above(), Blocks.AIR.defaultBlockState());
        var state = h.getLevel().getBlockState(firePos);
        h.assertTrue(state.canSurvive(h.getLevel(), firePos) && !state.isRandomlyTicking(),
                "Previously placed True Fire remains supported and has no random fire ticks");
        h.runAfterDelay(80, () -> {
            h.assertTrue(h.getLevel().getBlockState(firePos).is(ModBlocks.ZHEN_FIRE.get()),
                    "Legacy True Fire persists after world ticks without requiring its removed item");
            h.assertTrue(h.getLevel().getBlockState(firePos.east()).is(Blocks.OAK_PLANKS)
                            && h.getLevel().getBlockState(firePos.east().above()).isAir(),
                    "Legacy True Fire does not burn or ignite nearby wood");
            h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void legacyTrueFireRequiresSolidSupport(GameTestHelper h) {
        BlockPos support = h.absolutePos(new BlockPos(3, 2, 3));
        BlockPos firePos = support.above();
        var state = ModBlocks.ZHEN_FIRE.get().defaultBlockState();
        h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(firePos, state);
        h.assertTrue(h.getLevel().getBlockState(firePos).is(ModBlocks.ZHEN_FIRE.get()),
                "Existing True Fire survives above a solid support block");
        h.getLevel().setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(firePos).isAir() && !state.canSurvive(h.getLevel(), firePos),
                "Removing its support clears legacy True Fire instead of leaving a floating flame");
        h.succeed();
    }
}
