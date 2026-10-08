package com.example.immortal_cultivation_mod.block;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.Map;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;

@GameTestHolder("immortal_cultivation_ding_models")
@PrefixGameTestTemplate(false)
public final class DingModelGameTests {
    @GameTest(template = "fate_test")
    public static void heatedDingsResumeBurningAfterRepeatedLidCycles(GameTestHelper h) throws Exception {
        var pos = new BlockPos(3, 2, 3);
        var viewer = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ding-heat-loop"));
        for (var block : new Block[]{ModBlocks.QINGTONG_YINHUO_DING.get(), ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
            h.setBlock(pos, block);
            h.setBlock(pos.below(), Blocks.LAVA);
            var ding = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            try (var reader = new InputStreamReader(DingModelGameTests.class.getResourceAsStream(
                    "/assets/immortal_cultivation_mod/animations/block/" + ding.modelName() + ".animation.json"),
                    StandardCharsets.UTF_8)) {
                var clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
                var close = clips.getAsJsonObject("close");
                // Exercise GeckoLib's actual loop decision against the packaged hold-on-last-frame clip.
                var closeClip = new Animation("close", close.get("animation_length").getAsDouble() * 20,
                        Animation.LoopType.fromJson(close.get("loop")), null, null);
                var controller = new AnimationController<>(ding, "test", 0, state -> PlayState.CONTINUE);
                h.assertTrue(ding.updateDingAnimation().getAnimationStages().getLast().animationName().equals("ignite"),
                        "Heat animates before the first lid opening");
                for (int cycle = 0; cycle < 5; cycle++) {
                    ding.setLidOpen(true);
                    var opened = ding.updateDingAnimation().getAnimationStages();
                    h.assertTrue(opened.getLast().animationName().equals("open")
                                    && opened.getLast().loopType() == Animation.LoopType.HOLD_ON_LAST_FRAME,
                            "Lid opens and stays raised while its viewer remains");
                    ding.setLidOpen(false);
                    var closing = ding.updateDingAnimation();
                    var stages = closing.getAnimationStages();
                    h.assertTrue(stages.size() == 2 && stages.getFirst().animationName().equals("close")
                                    && !stages.getFirst().loopType().shouldPlayAgain(ding, controller, closeClip),
                            "Closing finishes instead of holding forever and blocking the next animation");
                    var burning = stages.getLast();
                    var igniteClip = new Animation("ignite", 40,
                            Animation.LoopType.fromJson(clips.getAsJsonObject("ignite").get("loop")), null, null);
                    h.assertTrue(burning.animationName().equals("ignite")
                                    && burning.loopType().shouldPlayAgain(ding, controller, igniteClip),
                            "Burning runs continuously after closing on both heated ding models");
                    h.assertTrue(ding.updateDingAnimation() == closing,
                            "Repeated render frames do not restart closing or burning");
                }
                ding.setLidOpen(true);
                ding.updateDingAnimation();
                h.setBlock(pos.below(), Blocks.AIR);
                ding.setLidOpen(false);
                h.assertTrue(ding.updateDingAnimation().getAnimationStages().getLast().animationName().equals("idle"),
                        "Removing heat while open returns to idle after closing");
                h.setBlock(pos.below(), Blocks.LAVA);
                h.assertTrue(ding.updateDingAnimation().getAnimationStages().getLast().animationName().equals("ignite"),
                        "Restoring heat restarts burning without reopening the menu");
            }
        }
        h.setBlock(pos, ModBlocks.CU_TIE_XIAODING.get());
        var iron = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        iron.startOpen(viewer);
        h.assertTrue(iron.updateDingAnimation().getAnimationStages().getFirst().animationName().equals("ignite"),
                "The lidless iron ding keeps burning without requesting nonexistent lid clips");
        iron.stopOpen(viewer);
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sanyangCapacitySyncAndSaveRetainTwentySlots(GameTestHelper h) {
        Block[] blocks = {ModBlocks.CU_TIE_XIAODING.get(), ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get()};
        var pos = new BlockPos(3, 2, 3);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "sanyang-capacity"));
        for (int i = 0; i < blocks.length; i++) {
            h.setBlock(pos, blocks[i]);
            var ding = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
            int capacity = 10 + i * 5;
            var menu = new DingMenu(i + 1, player.getInventory(), ding);
            h.assertTrue(ding.materialSlotCount() == capacity && menu.materialSlotCount() == capacity,
                    "Server and menu agree on each ding's capacity");
            ding.setItem(DingBlockEntity.YUJIAN_SLOT, new ItemStack(ModItems.CHI_JIN_SANYANG_DING_YUJIAN.get()));
            var ingredient = new ItemStack(ModBlocks.CHITONG_SUI.get());
            int last = DingBlockEntity.MATERIAL_START + capacity - 1;
            h.assertTrue(menu.getSlot(last).isActive() && menu.getSlot(last).mayPlace(ingredient)
                            && ding.canPlaceItem(last, ingredient), "Final material slot accepts recipe ingredients");
            h.assertTrue(!menu.getSlot(last + 1).isActive() && !menu.getSlot(last + 1).mayPlace(ingredient)
                            && !ding.canPlaceItem(last + 1, ingredient), "Slot above capacity stays closed");
            ding.setItem(last, ingredient);
            var copy = new DingBlockEntity(ding.getBlockPos(), ding.getBlockState());
            copy.loadWithComponents(ding.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(copy.getItem(last).is(ingredient.getItem()), "Final slot survives save and reload");
            menu.removed(player);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sanyangUpgradeFitsBronzeAndGoldenCoreRecipesAllowTwentySlots(GameTestHelper h) {
        var recipe = ForgingSystem.recipe(new ItemStack(ModItems.CHI_JIN_SANYANG_DING_YUJIAN.get()));
        h.assertTrue(recipe != null && recipe.directOutput()
                        && recipe.moldItem() == ModBlocks.CHI_JIN_SANYANG_DING.get()
                        && recipe.ingredientSlotCount() == DingBlockEntity.LARGE_FORGE_SLOTS,
                "Upgrade uses the existing slip system and fits the bronze ding");
        h.assertTrue(recipe.requiredFireType().equals("zhen_fire") && recipe.averageTemp() == 6500,
                "Golden Core upgrade requires true fire at 6500 degrees");
        for (int slots = 15; slots <= 20; slots++) {
            var largeRecipe = new ForgingSystem.ForgingRecipe("test_golden_core_" + slots,
                    ModBlocks.CHI_JIN_SANYANG_DING.get(), 6500,
                    Map.of(ModItems.JINDAN_YAODAN.get(), slots - 1), true, 64, "zhen_fire");
            h.assertTrue(largeRecipe.ingredientSlotCount() == slots, "All Golden Core recipe sizes from 15 to 20 are valid");
        }
        boolean rejected = false;
        try {
            new ForgingSystem.ForgingRecipe("test_oversized", ModBlocks.CHI_JIN_SANYANG_DING.get(), 6500,
                    Map.of(ModItems.JINDAN_YAODAN.get(), 20), true, 64, "zhen_fire");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "Recipes beyond 20 slots are rejected");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void latestDingsKeepTheirOwnSizeAtEveryFacingAndScale(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        double ironHeight = 0;
        for (var block : new Block[]{ModBlocks.CU_TIE_XIAODING.get(), ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
            var base = block.defaultBlockState();
            h.assertTrue(base.getRenderShape() == RenderShape.INVISIBLE,
                    "Only the animated block-entity renderer draws the ding");
            var north = base.getShape(h.getLevel(), pos).bounds();
            for (var facing : Direction.Plane.HORIZONTAL) {
                var oriented = base.setValue(DingBlock.FACING, facing);
                var bounds = oriented.getShape(h.getLevel(), pos).bounds();
                h.assertTrue(Math.abs(bounds.getYsize() - north.getYsize()) < 1e-6,
                        "Facing must preserve model height");
                for (int scale = 1; scale <= GeoRockBlock.MAX_SCALE; scale++) {
                    var scaled = oriented.setValue(GeoRockBlock.SCALE, scale).getShape(h.getLevel(), pos).bounds();
                    h.assertTrue(Math.abs(scaled.getXsize() - bounds.getXsize() * scale) < 1e-6
                                    && Math.abs(scaled.getYsize() - bounds.getYsize() * scale) < 1e-6
                                    && Math.abs(scaled.getZsize() - bounds.getZsize() * scale) < 1e-6,
                            "Collision dimensions follow the rendered scale");
                }
            }
            if (block == ModBlocks.CU_TIE_XIAODING.get()) ironHeight = north.maxY;
            else if (block == ModBlocks.QINGTONG_YINHUO_DING.get()) h.assertTrue(ironHeight < north.maxY && ironHeight <= 1.5 && north.maxY <= 2.0,
                    "Compact iron remake remains smaller than the bronze ding");
            else h.assertTrue(north.maxY <= 2.25 && north.minY >= 0,
                    "Three-Sun Ding fits its render bounds and stands above the ground");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void copperAndSanyangGainCollisionDetailAboveDoubleScale(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        for (var block : new Block[]{ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
            for (var facing : Direction.Plane.HORIZONTAL) {
                for (int scale = 1; scale <= GeoRockBlock.MAX_SCALE; scale++) {
                    var state = block.defaultBlockState().setValue(DingBlock.FACING, facing)
                            .setValue(DingBlock.SCALE, scale);
                    var outline = state.getShape(h.getLevel(), pos);
                    h.assertTrue(scale <= 2 ? outline.toAabbs().size() == 1 : outline.toAabbs().size() > 1,
                            "Small dings have one hull; enlarged dings expose native contours");
                    h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(outline,
                                    state.getCollisionShape(h.getLevel(), pos), net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),
                            "Selection and physical collision agree at every facing and scale");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void manualLidPersistsAfterViewersLeaveAndSyncsDisplayCopies(GameTestHelper h) {
        var pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.QINGTONG_YINHUO_DING.get());
        var ding = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        var first = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ding-first"));
        var second = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ding-second"));
        ding.setItem(DingBlockEntity.MATERIAL_START, new ItemStack(Items.IRON_INGOT, 3));
        ding.setLidOpen(true);
        ding.startOpen(first);
        ding.startOpen(second);
        ding.stopOpen(first);
        var tag = ding.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(ding.isLidOpen() && tag.getBoolean("MeshLidOpen"),
                "Another viewer keeps the lid open and the visual state is synchronized");
        h.assertTrue(!tag.contains("Items") && !tag.contains("items"),
                "Animation packets do not expose the real inventory");
        var client = new DingBlockEntity(ding.getBlockPos(), ding.getBlockState());
        client.handleUpdateTag(tag, h.getLevel().registryAccess());
        h.assertTrue(client.getDisplayItem(DingBlockEntity.MATERIAL_START).is(Items.IRON_INGOT)
                        && client.getDisplayItem(DingBlockEntity.MATERIAL_START).getCount() == 1
                        && client.isEmpty(), "Display copies show the ingredient without creating client inventory");
        ding.stopOpen(second);
        h.assertTrue(ding.isLidOpen(), "Exiting the last menu leaves the physical lid open");
        var loaded = new DingBlockEntity(ding.getBlockPos(), ding.getBlockState());
        loaded.loadWithComponents(ding.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(loaded.isLidOpen() && loaded.getDisplayItem(DingBlockEntity.MATERIAL_START).is(Items.IRON_INGOT),
                "The manual lid and visible contents survive chunk reload");
        ding.setLidOpen(false);
        h.assertTrue(!ding.isLidOpen() && !ding.getUpdateTag(h.getLevel().registryAccess()).getBoolean("MeshLidOpen"),
                "Only explicitly closing the lid changes its synchronized state");
        h.assertTrue(ding.getItem(DingBlockEntity.MATERIAL_START).getCount() == 3,
                "Opening and closing preserve forge ingredients");
        ding.removeItem(DingBlockEntity.MATERIAL_START, 3);
        client.handleUpdateTag(ding.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(client.getDisplayItem(DingBlockEntity.MATERIAL_START).isEmpty(),
                "Removing the last ingredient clears its world display");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void trackingUpdatesRestoreForgingAndExpireCompletion(GameTestHelper h) {
        var copy = new DingBlockEntity(h.absolutePos(new BlockPos(3, 2, 3)),
                ModBlocks.CU_TIE_XIAODING.get().defaultBlockState());
        copy.setLevel(h.getLevel());
        copy.setItem(DingBlockEntity.MATERIAL_START, new ItemStack(Items.GOLD_INGOT, 2));
        var update = new CompoundTag();
        update.putBoolean("Forging", true);
        update.putBoolean("MeshLidOpen", true);
        copy.handleUpdateTag(update, h.getLevel().registryAccess());
        h.assertTrue(copy.isForging() && copy.isLidOpen() && !copy.isFinishingAnimation(),
                "A newly tracking client restores current forging state");
        update.putBoolean("Forging", false);
        update.putLong("FinishAnimationUntil", h.getLevel().getGameTime() + 18);
        copy.handleUpdateTag(update, h.getLevel().registryAccess());
        h.assertTrue(!copy.isForging() && copy.isFinishingAnimation()
                        && copy.getItem(DingBlockEntity.MATERIAL_START).getCount() == 2,
                "Completion state reaches clients without clearing inventory");
        h.runAfterDelay(20, () -> {
            h.assertTrue(!copy.isFinishingAnimation(), "Completion expires without another network packet");
            h.succeed();
        });
    }
}
