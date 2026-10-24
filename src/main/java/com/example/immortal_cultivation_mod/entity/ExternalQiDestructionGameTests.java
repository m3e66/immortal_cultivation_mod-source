package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ExternalQiDestructionGameTests {
    @GameTest(template = "npc_combat_arena")
    public static void legacySixSectionsBecomeParticlesWithoutRevivingConsumedParts(GameTestHelper helper) {
        var slash = sectionedTestSlash(helper, true, 60);
        var saved = new net.minecraft.nbt.CompoundTag();
        slash.saveWithoutId(saved);
        saved.putIntArray("SectionBlocks", new int[]{10,10,10,10,10,10});
        saved.putLong("ActiveSections", 63L & ~(1L << 2));
        saved.putLong("ActiveSectionsHigh", 0);
        slash.load(saved);
        slash.saveWithoutId(saved);
        var budgets = saved.getIntArray("SectionBlocks");
        var mask = savedMask(saved);
        helper.assertTrue(budgets.length == 36 && java.util.Arrays.stream(budgets).sum() == 60,
                "Old six-piece saves split into 36 particles without adding block capacity");
        for (int particle = 0; particle < 36; particle++) {
            helper.assertTrue(mask.active(particle) == (particle < 12 || particle >= 18),
                    "Every child of the consumed legacy section remains consumed");
        }
        slash.discard(); helper.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void externalQiCutsTerrainAndModWorkstations(GameTestHelper helper) {
        var slash = sectionedTestSlash(helper, true, 1600);
        var targets = new net.minecraft.world.level.block.Block[]{Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
                com.example.immortal_cultivation_mod.block.ModBlocks.LINGMU_LOG.get(),
                com.example.immortal_cultivation_mod.block.ModBlocks.CU_TIE_XIAODING.get()};
        for (int i = 0; i < targets.length; i++) helper.setBlock(new BlockPos(12, 6, 11 + i), targets[i]);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(12, 6, 14))) != null,
                "Ding fixture includes its block entity");
        for (int i = 0; i < 5; i++) slash.tick();
        slash.discard();
        helper.runAfterDelay(3, () -> {
            for (int i = 0; i < targets.length - 1; i++) {
                helper.assertTrue(helper.getBlockState(new BlockPos(12, 6, 11 + i)).isAir(),
                        "External Qi cuts through " + targets[i]);
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 6, 14)).isAir()
                            && helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(12, 6, 14))) == null,
                    "External Qi destroys the workstation without leaving a stale block entity");
            helper.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena")
    public static void legacyLaunchGraceCannotDisableCollision(GameTestHelper helper) {
        var slash = sectionedTestSlash(helper, true, 1600);
        var tag = new net.minecraft.nbt.CompoundTag();
        slash.saveWithoutId(tag);
        tag.putInt("LaunchPassThroughTicks", 10);
        slash.load(tag);
        helper.assertTrue(!slash.forcefieldPassThrough(), "Legacy saves cannot bypass forcefields");
        helper.setBlock(new BlockPos(12, 6, 11), Blocks.STONE);
        slash.tick();
        slash.discard();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 6, 11)).isAir(),
                    "The first tick interacts with nearby terrain");
            helper.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena")
    public static void offCenterConsumptionPreservesNearbyPieces(GameTestHelper helper) {
        var slash = sectionedTestSlash(helper, true, 36);
        for (int z = 11; z <= 16; z++) helper.setBlock(new BlockPos(7, 6, z), Blocks.STONE);
        helper.setBlock(new BlockPos(10, 6, 16), Blocks.STONE);
        helper.setBlock(new BlockPos(14, 6, 16), Blocks.STONE);
        for (int tick = 0; tick < 3; tick++) slash.tick();
        var saved = new net.minecraft.nbt.CompoundTag();
        slash.saveWithoutId(saved);
        var mask = savedMask(saved);
        helper.assertTrue(!pieceAtX(mask, 7) && pieceAtX(mask, 10) && pieceAtX(mask, 12) && pieceAtX(mask, 14),
                "Off-center material consumes only local pieces, not a whole left/middle/right third");
        int[] budgets = saved.getIntArray("SectionBlocks");
        helper.assertTrue(budgets.length > 3 && java.util.Arrays.stream(budgets).sum() == saved.getInt("RemainingBlocks"),
                "Local budgets preserve total remaining terrain capacity");
        slash.load(saved);
        for (int tick = 0; tick < 2; tick++) slash.tick();
        slash.discard();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(10, 6, 16)).isAir()
                            && helper.getBlockState(new BlockPos(14, 6, 16)).isAir(),
                    "Unconsumed nearby pieces keep cutting after save/load");
            helper.assertTrue(helper.getBlockState(new BlockPos(7, 6, 16)).is(Blocks.STONE),
                    "Consumed off-center pieces cannot cut farther");
            int holes = 0;
            for (int z = 11; z <= 16; z++) if (helper.getBlockState(new BlockPos(7, 6, z)).isAir()) holes++;
            helper.assertTrue(holes > 0 && holes <= 4, "Only particles overlapping the column spend their one-block capacity");
            helper.succeed();
        });
    }

    private static com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.BladeMask savedMask(net.minecraft.nbt.CompoundTag tag) {
        return new com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.BladeMask(
                tag.getLong("ActiveSections"), tag.getLong("ActiveSectionsHigh"));
    }

    private static boolean pieceAtX(com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.BladeMask mask, int x) {
        return com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.sectionActive(mask, 4,
                (x - 12.0) / com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.bladeReach(4));
    }

    @GameTest(template = "npc_combat_arena")
    public static void multipleOffCenterGapsKeepOtherPiecesActive(GameTestHelper helper) {
        for (boolean destructive : new boolean[]{false, true}) {
            var slash = sectionedTestSlash(helper, destructive, 1600);
            for (int x : new int[]{7, 18}) for (int y = 5; y <= 8; y++) {
                helper.setBlock(new BlockPos(x, y, 11), Blocks.BEDROCK);
            }
            var cows = new java.util.ArrayList<net.minecraft.world.entity.animal.Cow>();
            int[] positions = {7, 10, 12, 14, 18};
            for (int x : positions) {
                var cow = helper.spawn(EntityType.COW, new BlockPos(x, 6, 15));
                cow.setNoAi(true); cow.setNoGravity(true); cows.add(cow);
            }
            float health = cows.getFirst().getHealth();
            for (int tick = 0; tick < 5; tick++) slash.tick();
            var tag = new net.minecraft.nbt.CompoundTag();
            slash.saveWithoutId(tag);
            var mask = savedMask(tag);
            helper.assertTrue(!pieceAtX(mask, 7) && !pieceAtX(mask, 18)
                            && pieceAtX(mask, 10) && pieceAtX(mask, 12) && pieceAtX(mask, 14),
                    "Two arbitrary collisions carve separate gaps while neighboring pieces survive");
            for (int i = 0; i < positions.length; i++) {
                boolean blocked = positions[i] == 7 || positions[i] == 18;
                helper.assertTrue(blocked ? cows.get(i).getHealth() == health : cows.get(i).getHealth() == health - 3,
                        "Only surviving pieces deal one hit past the obstacles at x=" + positions[i]);
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(7, 6, 11)).is(Blocks.BEDROCK)
                            && helper.getBlockState(new BlockPos(18, 6, 11)).is(Blocks.BEDROCK), "Protected terrain stays intact");
            slash.discard();
            cows.forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    private static WeaponQiSlashEntity sectionedTestSlash(GameTestHelper helper, boolean blockBreaking, int budget) {
        var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 6, 8));
        owner.setNoAi(true);
        owner.setNoGravity(true);
        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        var target = EntityType.COW.create(helper.getLevel());
        target.setPos(owner.position().add(0, 0, 12));
        helper.assertTrue(WeaponQiSlashEntity.releaseNpc(owner, target, 12, 3, 4, "any", 2, false), "Test slash releases");
        var slash = helper.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class,
                owner.getBoundingBox().inflate(3)).getFirst();
        var tag = new net.minecraft.nbt.CompoundTag();
        slash.saveWithoutId(tag);
        tag.remove("RestrictedTarget");
        tag.putDouble("DirectionX", 0); tag.putDouble("DirectionY", 0); tag.putDouble("DirectionZ", 1);
        tag.putDouble("Speed", 1.4); tag.putDouble("Radius", 4);
        tag.putInt("RemainingBlocks", budget);
        tag.putBoolean("BlockBreaking", blockBreaking);
        slash.load(tag);
        slash.setPos(helper.absoluteVec(new Vec3(12.5, 6.5, 10.5)));
        return slash;
    }

    @GameTest(template = "npc_combat_arena")
    public static void terrainCutHasLongPointedEnds(GameTestHelper helper) {
        var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 6, 8));
        owner.setNoAi(true);
        owner.setNoGravity(true);
        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        var target = EntityType.COW.create(helper.getLevel());
        target.setPos(owner.position().add(0, 0, 12));
        helper.assertTrue(WeaponQiSlashEntity.releaseNpc(owner, target, 8, 100, 4, "any", 2, false),
                "Test blade releases");
        var slash = helper.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class,
                owner.getBoundingBox().inflate(3)).getFirst();
        var tag = new net.minecraft.nbt.CompoundTag();
        slash.saveWithoutId(tag);
        tag.putDouble("DirectionX", 0);
        tag.putDouble("DirectionY", 0);
        tag.putDouble("DirectionZ", 1);
        tag.putDouble("Speed", 1.4);
        tag.putDouble("Radius", 4);
        tag.putBoolean("BlockBreaking", true);
        slash.load(tag);
        slash.setPos(helper.absoluteVec(new Vec3(12.5, 6.5, 10.5)));
        for (int x = 4; x <= 20; x++) for (int y = 3; y <= 10; y++) {
            helper.setBlock(new BlockPos(x, y, 11), Blocks.STONE);
        }
        slash.tick();
        slash.discard();
        helper.runAfterDelay(3, () -> {
            for (int y = 5; y <= 7; y++) {
                helper.assertTrue(helper.getBlockState(new BlockPos(12, y, 11)).isAir(),
                        "Blade keeps its thick central cut");
            }
            for (int x : new int[]{6, 18}) {
                helper.assertTrue(helper.getBlockState(new BlockPos(x, 6, 11)).isAir(),
                        "Point reaches beyond the original four-block radius");
                for (int y : new int[]{5, 7}) {
                    helper.assertTrue(helper.getBlockState(new BlockPos(x, y, 11)).is(Blocks.STONE),
                            "Tip narrows to one block instead of a rectangular end");
                }
            }
            for (int x : new int[]{5, 19}) {
                helper.assertTrue(helper.getBlockState(new BlockPos(x, 6, 11)).is(Blocks.STONE),
                        "Cut stops at the pointed reach");
            }
            owner.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "npc_combat_arena")
    public static void strongerSlashCutsWiderAndBreaksMoreTerrain(GameTestHelper helper) {
        var level = helper.getLevel();
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(level);
        npc.setNoAi(true);
        npc.setPos(helper.absoluteVec(new Vec3(12.5, 2, 8.5)));
        npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        level.addFreshEntity(npc);
        var target = EntityType.ZOMBIE.create(level);
        target.setPos(npc.position().add(0, 0, 12));
        npc.setTarget(target);
        fillWall(helper);
        fire(helper, npc, target, 1);
        helper.runAfterDelay(3, () -> {
            int weak = holes(helper);
            helper.assertTrue(weak > 0 && weak <= WeaponQiSlashEntity.damageScaledBlockBudget(1),
                    "Weak slash cuts terrain without exceeding its damage budget");
            fillWall(helper);
            fire(helper, npc, target, 100);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(holes(helper) > weak, "Higher-damage slash removes more actual blocks from the same wall");
                helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(8, 3, 10))).isAir(),
                        "Strong slash cuts outside the weak slash's width");
                helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(10, 3, 11))).isAir(),
                        "Shared destruction breaks containers in the blade path");
                helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(12, 4, 10))).isAir(),
                        "Thick slash cuts blocks above its old single-block plane");
                helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(12, 7, 10))).is(Blocks.STONE),
                        "Terrain outside the thick blade stays intact");
                npc.discard();
                helper.succeed();
            });
        });
    }

    private static void fire(GameTestHelper helper, LianqiCultivatorEntity npc,
                             net.minecraft.world.entity.LivingEntity target, float damage) {
        helper.assertTrue(WeaponQiSlashEntity.releaseNpc(npc, target, 8, damage, 2, "any", 2, false),
                "Real NPC external-Qi slash releases");
        var slashes = helper.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, npc.getBoundingBox().inflate(3));
        helper.assertTrue(slashes.size() == 1, "One test slash");
        var slash = slashes.getFirst();
        for (int tick = 0; tick < 5 && !slash.isRemoved(); tick++) slash.tick();
        slash.discard();
    }

    private static void fillWall(GameTestHelper helper) {
        for (int x = 3; x <= 22; x++) for (int z = 10; z <= 14; z++) {
            helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(10, 3, 11), Blocks.CHEST);
        helper.setBlock(new BlockPos(12, 4, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(12, 7, 10), Blocks.STONE);
    }

    private static int holes(GameTestHelper helper) {
        int count = 0;
        for (int x = 3; x <= 22; x++) for (int z = 10; z <= 14; z++) {
            if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 3, z))).isAir()) count++;
        }
        return count;
    }

    @GameTest(template = "fate_test")
    public static void normalWhipCannotBreakTerrainWithToggleEnabled(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "qi-terrain-toggle"));
        player.setPos(helper.absoluteVec(new Vec3(2, 2, 2)));
        for (boolean enabled : new boolean[]{false, true}) {
            QiInfusedWeapon.setOutwardQiBlockBreaking(player, enabled);
            helper.assertTrue(WeaponQiSlashEntity.releaseWhip(player, 100, 50, 8), "Whip Qi releases");
            var slashes = helper.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, player.getBoundingBox().inflate(3));
            helper.assertTrue(slashes.size() == 1, "One whip slash");
            var slash = slashes.getFirst();
            var tag = new net.minecraft.nbt.CompoundTag();
            slash.saveWithoutId(tag);
            helper.assertTrue(!tag.getBoolean("BlockBreaking"), "Normal whip preserves terrain even with destruction enabled");
            helper.assertTrue(tag.getInt("RemainingBlocks") == 1600, "Whip uses released damage for block capacity");
            helper.assertTrue(tag.getInt("LaunchPassThroughTicks") == 0, "Player whip Qi collides immediately");
            slash.discard();
        }
        helper.succeed();
    }
}
