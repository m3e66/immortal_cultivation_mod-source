package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_forging_purity")
@PrefixGameTestTemplate(false)
public final class ForgingPurityGameTests {
    @GameTest(template = "fate_test")
    public static void spiritStoneStacksHaveOnePurityVote(GameTestHelper helper) {
        var stones = List.of(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get(),
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get());
        for (Item stone : stones) {
            for (int count : new int[]{1, 16, 64}) {
                var stack = new ItemStack(stone, count);
                double expected = (200.0 + ForgingSystem.purity(stack)) / 3.0;
                assertNear(helper, ForgingSystem.forgingSuccessChancePercent(
                        List.of(material(1, 100), material(1, 100), stack)), expected,
                        "Stone tier purity counts once for a stack of " + count);
                helper.assertTrue(stack.getCount() == count, "Purity calculation preserves ingredient quantity");
            }
        }
        var low = new ItemStack(stones.getFirst(), 64);
        assertNear(helper, ForgingSystem.forgingSuccessChancePercent(
                List.of(material(2, 100), low)), 230.0 / 3.0,
                "Ordinary materials retain their existing count weighting");
        assertNear(helper, ForgingSystem.forgingSuccessChancePercent(
                List.of(material(1, 100), low, low.copy())), 160.0 / 3.0,
                "Two separate stone stacks contribute two units");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void moldsKeepCorrectPurityAndFullStoneIngredientCount(GameTestHelper helper) {
        var recipe = ForgingSystem.recipeById("dao");
        var stone = new ItemStack(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(), 64);
        var inputs = List.of(material(1, 100), material(1, 100), stone);
        var mold = ForgingSystem.createMold(recipe, inputs, recipe.averageTemp(), "normal_fire");
        var data = mold.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(ForgingSystem.TAG_FORGING);
        helper.assertTrue(data.getInt("purity_weight") == 3, "Saved mold weight counts the stone stack once");
        assertNear(helper, data.getDouble("average_purity"), 230.0 / 3.0,
                "Displayed mold purity agrees with the forge success formula");
        String stoneId = BuiltInRegistries.ITEM.getKey(stone.getItem()).toString();
        int recordedStones = 0;
        for (Tag entry : data.getList("ingredients", Tag.TAG_COMPOUND)) {
            var ingredient = (CompoundTag) entry;
            if (stoneId.equals(ingredient.getString("item"))) recordedStones += ingredient.getInt("count");
        }
        helper.assertTrue(recordedStones == 64 && recipe.spiritStoneCount() == 64,
                "Recipe cost and saved ingredient history still require all 64 stones");
        assertNear(helper, ForgingSystem.forgingSuccessChancePercent(List.of(mold, material(1, 100))),
                330.0 / 4.0, "Later forging inherits the corrected material weight");
        helper.succeed();
    }

    private static ItemStack material(int count, int purity) {
        var stack = new ItemStack(ModBlocks.JINGTIE_MOTHERSTONE.get(), count);
        var tag = new CompoundTag();
        tag.putInt(ForgingSystem.TAG_PURITY, purity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void assertNear(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.000001, message + ": " + actual);
    }
}
