package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity;
import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class VaultProtectionGameTests {
    @GameTest(template = "npc_combat_arena")
    public static void heavyLandingCannotExcavateGuardedSect(GameTestHelper h) {
        try (var sect = new Sect(h, Blocks.DIRT.defaultBlockState())) {
            var guardian = sect.guardian();
            var feet = Vec3.atBottomCenterOf(sect.block.above());
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.landingCrater(guardian, feet, .95F);
            h.assertTrue(h.getLevel().getBlockState(sect.block).is(Blocks.DIRT),
                    "New landing destruction keeps live guardian terrain protection");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void sectStaysLockedBeforeGuardianLoads(GameTestHelper h) {
        try (var sect = new Sect(h)) {
            h.assertTrue(SmallSectProtection.isLocked(h.getLevel(), sect.block),
                    "Generated sect stays protected before guardian spawning/registration is ready");
            h.assertTrue(!SpellDestruction.breakExternalQiBlock(h.getLevel(), sect.block, false, null)
                            && !SpellDestruction.breakBlock(h.getLevel(), sect.block, false, null)
                            && !h.getLevel().removeBlock(sect.block, false),
                    "External Qi, ordinary spells and direct removal all respect pending guardian lock");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void livingGuardianRepairsDefeatAndStopsRealQiParticles(GameTestHelper h) {
        try (var sect = new Sect(h)) {
            var guardian = sect.guardian();
            var saved = new CompoundTag();
            guardian.saveWithoutId(saved);
            SmallSectProtection.guardianDefeated(h.getLevel(), guardian);
            guardian.discard();
            h.assertTrue(!SmallSectProtection.isLocked(h.getLevel(), sect.block), "Fixture has stale defeat state");
            var restored = ModEntities.JINDAN_VAULT_GUARDIAN.get().create(h.getLevel());
            restored.load(saved);
            h.getLevel().addFreshEntity(restored);
            sect.guardians.add(restored);
            h.assertTrue(SmallSectProtection.isLocked(h.getLevel(), sect.block),
                    "Living guardian reload repairs stale defeat before its first AI tick");
            fireAtSect(h);
            h.assertTrue(h.getLevel().getBlockState(sect.block).is(Blocks.OBSIDIAN)
                            && h.getLevel().getBlockState(sect.block.south()).is(ModBlocks.CU_TIE_XIAODING.get()),
                    "Real external-Qi particles cannot destroy protected obsidian or mod machinery");
            h.assertTrue(!SpellDestruction.breakExternalQiBlock(h.getLevel(), sect.block, false, null),
                    "Protected terrain is rejected instead of queued");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void queuedQiRechecksRestoredGuardianLock(GameTestHelper h) throws Exception {
        var sect = new Sect(h);
        try {
            var guardian = sect.guardian();
            SmallSectProtection.guardianDefeated(h.getLevel(), guardian);
            guardian.discard();
            SpellDestruction.hasBudget(h.getLevel());
            var probes = SpellDestruction.class.getDeclaredField("probes");
            probes.setAccessible(true);
            int previous = probes.getInt(null);
            try {
                probes.setInt(null, SpellDestruction.MAX_PROBES);
                h.assertTrue(SpellDestruction.breakExternalQiBlock(h.getLevel(), sect.block, false, null),
                        "Unlocked terrain enters deferred destruction queue");
            } finally {
                probes.setInt(null, previous);
            }
            sect.guardian();
            h.runAfterDelay(3, () -> {
                try (sect) {
                    h.assertTrue(h.getLevel().getBlockState(sect.block).is(Blocks.OBSIDIAN),
                            "Queued external Qi rechecks restored guardian protection before removal");
                }
                h.succeed();
            });
        } catch (Throwable failure) {
            sect.close();
            throw failure;
        }
    }

    @GameTest(template = "npc_combat_arena")
    public static void lastGuardianDeathUnlocksButPlacedBlocksStayExempt(GameTestHelper h) {
        try (var sect = new Sect(h)) {
            var first = sect.guardian();
            var second = sect.guardian();
            SmallSectProtection.recordPlacedBlock(h.getLevel(), sect.block.south());
            h.assertTrue(!SmallSectProtection.isLocked(h.getLevel(), sect.block.south())
                            && SmallSectProtection.isLocked(h.getLevel(), sect.block),
                    "Player-added blocks remain exempt while original structure stays protected");
            first.setHealth(0);
            first.die(h.getLevel().damageSources().genericKill());
            h.assertTrue(SmallSectProtection.isLocked(h.getLevel(), sect.block),
                    "Duplicate guardian death cannot unlock a sect whose guardian is still alive");
            second.setHealth(0);
            second.die(h.getLevel().damageSources().genericKill());
            h.assertTrue(!SmallSectProtection.isLocked(h.getLevel(), sect.block)
                            && SpellDestruction.breakExternalQiBlock(h.getLevel(), sect.block, false, null),
                    "Defeating the last guardian unlocks ordinary terrain destruction");
        }
        h.succeed();
    }

    private static void fireAtSect(GameTestHelper h) {
        var owner = h.spawn(EntityType.ZOMBIE, new BlockPos(12, 6, 8));
        owner.setNoAi(true);
        owner.setNoGravity(true);
        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        var target = EntityType.COW.create(h.getLevel());
        target.setPos(owner.position().add(0, 0, 12));
        h.assertTrue(WeaponQiSlashEntity.releaseNpc(owner, target, 12, 3, 4, "any", 2, false), "Slash releases");
        var slash = h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class,
                owner.getBoundingBox().inflate(3)).getFirst();
        try {
            var tag = new CompoundTag();
            slash.saveWithoutId(tag);
            tag.remove("RestrictedTarget");
            tag.putDouble("DirectionX", 0); tag.putDouble("DirectionY", 0); tag.putDouble("DirectionZ", 1);
            tag.putDouble("Speed", 1.4); tag.putDouble("Radius", 4);
            tag.putInt("RemainingBlocks", 1600); tag.putBoolean("BlockBreaking", true);
            slash.load(tag);
            slash.setPos(h.absoluteVec(new Vec3(12.5, 6.5, 10.5)));
            for (int i = 0; i < 5 && !slash.isRemoved(); i++) slash.tick();
        } finally {
            slash.discard();
            owner.discard();
        }
    }

    /** Installs real structure metadata, without generating a full village in each test. */
    private static final class Sect implements AutoCloseable {
        final ServerLevel level;
        final BlockPos block;
        final String key;
        final List<VaultGuardianEntity> guardians = new ArrayList<>();
        final List<Runnable> restore = new ArrayList<>();

        Sect(GameTestHelper h) {
            this(h, Blocks.OBSIDIAN.defaultBlockState());
        }

        Sect(GameTestHelper h, net.minecraft.world.level.block.state.BlockState terrain) {
            level = h.getLevel();
            block = h.absolutePos(new BlockPos(12, 6, 11));
            level.setBlockAndUpdate(block, terrain);
            level.setBlockAndUpdate(block.south(), ModBlocks.CU_TIE_XIAODING.get().defaultBlockState());
            Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(
                    ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "small_sect1"));
            var piece = new BuriedTreasurePieces.BuriedTreasurePiece(block.offset(-2, -1, -1));
            piece.getBoundingBox().encapsulate(block.offset(2, 2, 3));
            var root = level.getChunkAt(block);
            var start = new StructureStart(structure, root.getPos(), 0, new PiecesContainer(List.of(piece)));
            var bounds = start.getBoundingBox();
            key = level.dimension().location() + "|" + bounds.minX() + "," + bounds.minY() + "," + bounds.minZ()
                    + "," + bounds.maxX() + "," + bounds.maxY() + "," + bounds.maxZ();
            var oldStarts = new HashMap<>(root.getAllStarts());
            restore.add(() -> root.setAllStarts(oldStarts));
            root.setStartForStructure(structure, start);
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
                for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                    LevelChunk chunk = level.getChunk(x, z);
                    Map<Structure, LongSet> oldRefs = new HashMap<>();
                    chunk.getAllReferences().forEach((s, refs) -> oldRefs.put(s, new LongOpenHashSet(refs)));
                    restore.add(() -> chunk.setAllReferences(oldRefs));
                    chunk.addReferenceForStructure(structure, root.getPos().toLong());
                }
            }
        }

        VaultGuardianEntity guardian() {
            var guardian = ModEntities.JINDAN_VAULT_GUARDIAN.get().create(level);
            guardian.configureAsSmallSectGuardian(key);
            guardian.setNoAi(true);
            guardian.setNoGravity(true);
            guardian.setPos(Vec3.atCenterOf(block.offset(1, 1, 1)));
            level.addFreshEntity(guardian);
            guardians.add(guardian);
            return guardian;
        }

        @Override
        public void close() {
            guardians.forEach(VaultGuardianEntity::discard);
            restore.forEach(Runnable::run);
        }
    }
}
