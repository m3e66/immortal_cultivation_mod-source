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
    public static void debugEyeScalingTakesPriorityOverLidsWithAnyHeldItem(GameTestHelper h) {
        var pos = new BlockPos(3, 2, 3);
        var absolute = h.absolutePos(pos);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ding-scale-priority")) {
            @Override public boolean hasPermissions(int level) { return level <= 2; }
        };
        player.setShiftKeyDown(true);
        player.getPersistentData().putBoolean("ImmortalCultivationDebugEyeEnabled", true);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        for (var block : new Block[]{ModBlocks.QINGTONG_YINHUO_DING.get(), ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
            var dingBlock = (DingBlock) block;
            for (boolean lidOpen : new boolean[]{false, true}) {
                h.setBlock(pos, block.defaultBlockState().setValue(DingBlock.SCALE, 1));
                var ding = (DingBlockEntity) h.getLevel().getBlockEntity(absolute);
                ding.setLidOpen(lidOpen);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                dingBlock.useWithoutItem(h.getLevel().getBlockState(absolute), h.getLevel(), absolute, player, hit);
                h.assertTrue(h.getLevel().getBlockState(absolute).getValue(DingBlock.SCALE) == 2
                                && ding.isLidOpen() == lidOpen,
                        "Empty-handed Debug Eye scale change takes priority over either lid state");
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
                for (int expected : new int[]{3, 4, 5, 1}) {
                    dingBlock.useItemOn(player.getMainHandItem(), h.getLevel().getBlockState(absolute),
                            h.getLevel(), absolute, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    h.assertTrue(h.getLevel().getBlockState(absolute).getValue(DingBlock.SCALE) == expected
                                    && ding.isLidOpen() == lidOpen,
                            "Holding ordinary materials still scales through the full cycle without toggling the lid");
                }
                player.getPersistentData().putBoolean("ImmortalCultivationDebugEyeEnabled", false);
                dingBlock.useItemOn(player.getMainHandItem(), h.getLevel().getBlockState(absolute),
                        h.getLevel(), absolute, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                h.assertTrue(h.getLevel().getBlockState(absolute).getValue(DingBlock.SCALE) == 1
                                && ding.isLidOpen() != lidOpen,
                        "With Debug Eye disabled, sneak-right-click toggles only the lid");
                player.getPersistentData().putBoolean("ImmortalCultivationDebugEyeEnabled", true);
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void sealedFloorsSupportTheModelAtEveryFacingAndScale(GameTestHelper h) throws Exception {
        var position = new BlockPos(3, 2, 3);
        for (var block : new Block[]{ModBlocks.CU_TIE_XIAODING.get(), ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
            h.setBlock(position, block);
            var ding = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(position));
            try (var reader = new InputStreamReader(DingModelGameTests.class.getResourceAsStream(
                    "/assets/immortal_cultivation_mod/geo/block/" + ding.modelName() + ".geo.json"),
                    StandardCharsets.UTF_8)) {
                var geometry = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                var bones = geometry.getAsJsonArray("bones");
                for (var boneValue : bones) {
                    var bone = boneValue.getAsJsonObject();
                    if (!bone.get("name").getAsString().equals("body")) continue;
                    var cubes = bone.getAsJsonArray("cubes");
                    for (var facing : Direction.Plane.HORIZONTAL) for (int scale = 1; scale <= 5; scale++) {
                        var state = block.defaultBlockState().setValue(DingBlock.FACING, facing)
                                .setValue(DingBlock.SCALE, scale);
                        var boxes = state.getCollisionShape(h.getLevel(), h.absolutePos(position)).toAabbs();
                        for (var cubeValue : cubes) {
                            var cube = cubeValue.getAsJsonObject();
                            var origin = cube.getAsJsonArray("origin");
                            var size = cube.getAsJsonArray("size");
                            for (double fraction : new double[]{.01, .5, .99}) {
                                double x = (origin.get(0).getAsDouble() + size.get(0).getAsDouble() * fraction) / 16;
                                double z = (origin.get(2).getAsDouble() + size.get(2).getAsDouble() / 2) / 16;
                                double rotatedX = switch (facing) { case EAST -> -z; case SOUTH -> -x; case WEST -> z; default -> x; };
                                double rotatedZ = switch (facing) { case EAST -> x; case SOUTH -> -z; case WEST -> -x; default -> z; };
                                var point = new net.minecraft.world.phys.Vec3(.5 + rotatedX * scale,
                                        (origin.get(1).getAsDouble() + size.get(1).getAsDouble() - .01) / 16 * scale,
                                        .5 + rotatedZ * scale);
                                h.assertTrue(boxes.stream().anyMatch(box -> box.contains(point)),
                                        "Sealed floor has physical support at " + ding.modelName() + " / " + facing + " / " + scale);
                            }
                        }
                    }
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void materialsSpreadInFourDirectionsWithoutGrowingPastTheBasin(GameTestHelper h) {
        var model = new net.minecraft.world.phys.AABB(-.25, -.125, -.25, .25, .125, .25);
        for (String name : new String[]{"cu_tie_xiaoding", "qingtong_yinhuo_ding", "chi_jin_sanyang_ding"})
        for (int scale = 1; scale <= 5; scale++) {
            var layout = DingPileLayout.forModel(name, scale);
            var repeated = DingPileLayout.forModel(name, scale);
            var placed = new java.util.ArrayList<net.minecraft.world.phys.AABB>();
            for (int index = 0; index < 100; index++) {
                var offset = layout.place(model, index * 7919L);
                h.assertTrue(offset.equals(repeated.place(model, index * 7919L)),
                        "Pile positions do not jitter when rebuilt with unchanged contents");
                var bounds = model.move(offset);
                h.assertTrue(Math.abs(bounds.getXsize() - .5) < .000001
                                && Math.abs(bounds.getYsize() - .25) < .000001,
                        "Material retains native size even in an enlarged ding");
                h.assertTrue(layout.volume().contains(bounds), "Normal material remains completely inside the basin");
                placed.add(bounds);
            }
            h.assertTrue(placed.get(0).getCenter().x > 0 && placed.get(1).getCenter().z > 0
                            && placed.get(2).getCenter().x < 0 && placed.get(3).getCenter().z < 0,
                    "Materials spread toward all four sides instead of only upward");
            double floor = layout.volume().bounds().minY;
            double height = layout.volume().bounds().getYsize();
            h.assertTrue(layout.bounds().maxY <= floor + height * .22 + model.getYsize() + 1e-6,
                    "Large inventories cannot build a tall tower");
        }
        var wide = new net.minecraft.world.phys.AABB(-1, 0, -1, 1, .5, 1);
        var narrowDing = new DingPileLayout(.42, .63, .65, 1.36);
        var first = model.move(narrowDing.place(model, 0));
        var second = model.move(narrowDing.place(model, 1));
        h.assertTrue(first.intersects(second), "A crowded basin allows model overlap instead of tall stacks");
        var widePlaced = wide.move(narrowDing.place(wide, 42));
        h.assertTrue(widePlaced.getXsize() == 2 && widePlaced.minY == .65,
                "Oversized material keeps native dimensions and rests on the floor before visual cropping");
        h.assertTrue(narrowDing.bounds().maxY <= 1.36 && narrowDing.bounds().getXsize() <= 1.26,
                "Visible bounds exclude oversized protrusions");
        h.assertTrue(DingPileLayout.visibleCopies(0) == 0 && DingPileLayout.visibleCopies(3) == 3
                        && DingPileLayout.visibleCopies(64) == 5,
                "Stack quantity adds visible pieces with a bounded draw cost");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oversizedMaterialFacesAreCroppedInsideEveryDing(GameTestHelper h) {
        for (String name : new String[]{"cu_tie_xiaoding", "qingtong_yinhuo_ding", "chi_jin_sanyang_ding"})
        for (int scale = 1; scale <= 5; scale++) {
            var volume = DingPileLayout.forModel(name, scale).volume();
            double middle = volume.bounds().getCenter().y;
            var face = java.util.List.of(new net.minecraft.world.phys.Vec3(-10, middle, -10),
                    new net.minecraft.world.phys.Vec3(10, middle, -10),
                    new net.minecraft.world.phys.Vec3(10, middle, 10),
                    new net.minecraft.world.phys.Vec3(-10, middle, 10));
            var clipped = volume.clip(face, point -> point, net.minecraft.world.phys.Vec3::lerp);
            h.assertTrue(clipped.size() >= 3, "Oversized item faces retain their visible interior");
            for (var vertex : clipped) h.assertTrue(volume.contains(vertex), "No item vertex sticks through a wall");
            var vertical = java.util.List.of(new net.minecraft.world.phys.Vec3(-.1, -10, 0),
                    new net.minecraft.world.phys.Vec3(.1, -10, 0),
                    new net.minecraft.world.phys.Vec3(.1, 20, 0),
                    new net.minecraft.world.phys.Vec3(-.1, 20, 0));
            var cropped = volume.clip(vertical, point -> point, net.minecraft.world.phys.Vec3::lerp);
            h.assertTrue(cropped.size() >= 3, "Tall models retain the part inside the bowl");
            for (var vertex : cropped) h.assertTrue(volume.contains(vertex), "Nothing appears above the ceiling or below the floor");
            var above = face.stream().map(point -> point.add(0, 20, 0)).toList();
            h.assertTrue(volume.clip(above, point -> point, net.minecraft.world.phys.Vec3::lerp).isEmpty(),
                    "Fully outside faces are discarded");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void basinCroppingPreservesTextureCoordinatesAndNarrowingWalls(GameTestHelper h) {
        record TexturedPoint(net.minecraft.world.phys.Vec3 position, double u) {
            TexturedPoint lerp(TexturedPoint other, double t) {
                return new TexturedPoint(position.lerp(other.position, t), u + (other.u - u) * t);
            }
        }
        var volume = new DingBasinVolume(1, .5, 0, 1);
        var face = java.util.List.of(new TexturedPoint(new net.minecraft.world.phys.Vec3(-2, .5, 0), 0),
                new TexturedPoint(new net.minecraft.world.phys.Vec3(2, .5, 0), 1),
                new TexturedPoint(new net.minecraft.world.phys.Vec3(2, .7, 0), 1),
                new TexturedPoint(new net.minecraft.world.phys.Vec3(-2, .7, 0), 0));
        var cropped = volume.clip(face, TexturedPoint::position, TexturedPoint::lerp);
        h.assertTrue(cropped.size() >= 3, "A narrowing basin still keeps interior faces");
        for (var vertex : cropped) {
            h.assertTrue(volume.contains(vertex.position()), "Narrowing wall clips correctly");
            h.assertTrue(Math.abs(vertex.u() - (vertex.position().x + 2) / 4) < 1e-6,
                    "Clipped edges interpolate original UVs rather than stretching the item texture");
        }
        var inside = java.util.List.of(new net.minecraft.world.phys.Vec3(0, .1, 0),
                new net.minecraft.world.phys.Vec3(.1, .1, 0), new net.minecraft.world.phys.Vec3(0, .2, 0));
        h.assertTrue(volume.clip(inside, point -> point, net.minecraft.world.phys.Vec3::lerp) == inside,
                "Models already inside remain unchanged");
        h.succeed();
    }

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
                        && client.getDisplayItem(DingBlockEntity.MATERIAL_START).getCount() == 3
                        && client.isEmpty(), "Display copies show the ingredient without creating client inventory");
        ding.stopOpen(second);
        h.assertTrue(ding.isLidOpen(), "Exiting the last menu leaves the physical lid open");
        var loaded = new DingBlockEntity(ding.getBlockPos(), ding.getBlockState());
        loaded.loadWithComponents(ding.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(loaded.isLidOpen() && loaded.getDisplayItem(DingBlockEntity.MATERIAL_START).is(Items.IRON_INGOT),
                "The manual lid and visible contents survive chunk reload");
        ding.removeItem(DingBlockEntity.MATERIAL_START, 1);
        client.handleUpdateTag(ding.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(client.getDisplayItem(DingBlockEntity.MATERIAL_START).getCount() == 2,
                "Stack quantity changes synchronize the visible pile without adding client inventory");
        ding.setLidOpen(false);
        h.assertTrue(!ding.isLidOpen() && !ding.getUpdateTag(h.getLevel().registryAccess()).getBoolean("MeshLidOpen"),
                "Only explicitly closing the lid changes its synchronized state");
        h.assertTrue(ding.getItem(DingBlockEntity.MATERIAL_START).getCount() == 2,
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
