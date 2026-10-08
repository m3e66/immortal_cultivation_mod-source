package com.example.immortal_cultivation_mod.config;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings.Category;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class BlockBreakingGameTests {
    private static final String SETTINGS_TAG = "ImmortalCultivationBlockBreakingDisabled";

    private static final class User implements AutoCloseable {
        final FakePlayer player;

        User(GameTestHelper h) {
            player = new FakePlayer(h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "block-setting-test"));
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(h.absoluteVec(new Vec3(2, 6, 2)));
        }

        @Override public void close() { player.discard(); }
    }

    private static BlockPos stone(GameTestHelper h, int x, int z) {
        BlockPos pos = h.absolutePos(new BlockPos(x, 6, z));
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        return pos;
    }

    private static boolean impact(GameTestHelper h, ServerPlayer player, BlockPos pos, Category category) {
        return BlockBreakingSettings.withSource(category, player,
                () -> SpellDestruction.eraseImpactBlockNow(h.getLevel(), pos, player));
    }

    /** Exercise the real deferred-point path without timing-dependent budget exhaustion. */
    private static <T> T exhaustedBudget(GameTestHelper h, Supplier<T> action) throws Exception {
        SpellDestruction.hasBudget(h.getLevel());
        Field probes = SpellDestruction.class.getDeclaredField("probes");
        probes.setAccessible(true);
        int previous = probes.getInt(null);
        try {
            probes.setInt(null, SpellDestruction.MAX_PROBES);
            return action.get();
        } finally {
            probes.setInt(null, previous);
        }
    }

    @GameTest(template = "npc_combat_arena")
    public static void categoriesIndependentlyGateActualTerrainRemoval(GameTestHelper h) {
        try (var user = new User(h)) {
            int x = 3;
            for (Category category : Category.values()) {
                if (category == Category.ALL) continue;
                BlockBreakingSettings.setDisabledMask(user.player, category.bit());
                BlockPos pos = stone(h, x++, 8);
                h.assertTrue(!impact(h, user.player, pos, category)
                                && h.getLevel().getBlockState(pos).is(Blocks.STONE),
                        "Disabled " + category.id() + " cannot remove actual terrain");
                Category other = category == Category.SPELLS ? Category.WEAPONS : Category.SPELLS;
                h.assertTrue(impact(h, user.player, pos, other)
                                && h.getLevel().getBlockState(pos).isAir(),
                        "Disabling " + category.id() + " leaves " + other.id() + " terrain effects available");
            }
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void masterDisableBlocksEveryCategoryWithoutErasingChoices(GameTestHelper h) {
        try (var user = new User(h)) {
            BlockBreakingSettings.setDisabledMask(user.player, Category.ALL.bit() | Category.WEAPONS.bit());
            BlockPos pos = stone(h, 4, 8);
            for (Category category : Category.values()) {
                h.assertTrue(!impact(h, user.player, pos, category),
                        "Master disable blocks " + category.id());
            }
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "Master disable preserves terrain");
            BlockBreakingSettings.setDisabledMask(user.player,
                    BlockBreakingSettings.disabledMask(user.player) & ~Category.ALL.bit());
            h.assertTrue(!impact(h, user.player, pos, Category.WEAPONS),
                    "Turning master back on retains the independently disabled weapon choice");
            h.assertTrue(impact(h, user.player, pos, Category.SPELLS),
                    "Turning master back on restores categories that were not individually disabled");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void defaultSpellsAndExplicitSourcesKeepExistingBlockProtections(GameTestHelper h) {
        try (var user = new User(h)) {
            BlockBreakingSettings.setDisabledMask(user.player, Category.SPELLS.bit());
            BlockPos pos = stone(h, 4, 8);
            h.assertTrue(!SpellDestruction.breakNow(h.getLevel(), pos, false, user.player),
                    "An unscoped shared destruction call defaults to spells");
            h.assertTrue(impact(h, user.player, pos, Category.SPECIAL_ITEMS),
                    "Explicit item attribution is not incorrectly treated as a disabled spell");

            BlockPos bedrock = stone(h, 5, 8);
            BlockPos chest = stone(h, 6, 8);
            h.getLevel().setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertTrue(!impact(h, user.player, bedrock, Category.SPECIAL_ITEMS)
                            && !impact(h, user.player, chest, Category.SPECIAL_ITEMS),
                    "Enabling an effect cannot bypass unbreakable blocks or inventory-block protection");
            h.assertTrue(h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK)
                            && h.getLevel().getBlockState(chest).is(Blocks.CHEST),
                    "Protected blocks remain intact");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 80)
    public static void queuedPointsRecheckCategoryAndRetainScopedActor(GameTestHelper h) throws Exception {
        var user = new User(h);
        try {
            BlockPos blocked = stone(h, 4, 8);
            BlockPos allowed = stone(h, 5, 8);
            exhaustedBudget(h, () -> {
                h.assertTrue(BlockBreakingSettings.withSource(Category.WEAPONS, user.player,
                                () -> SpellDestruction.breakBlock(h.getLevel(), blocked, false, null)),
                        "Scoped weapon point is accepted while its original category is enabled");
                h.assertTrue(BlockBreakingSettings.withSource(Category.SPELLS, user.player,
                                () -> SpellDestruction.breakBlock(h.getLevel(), allowed, false, null)),
                        "A separate spell point is accepted");
                return null;
            });
            h.assertTrue(h.getLevel().getBlockState(blocked).is(Blocks.STONE)
                            && h.getLevel().getBlockState(allowed).is(Blocks.STONE),
                    "Accepted deferred points have not mutated terrain yet");
            BlockBreakingSettings.setDisabledMask(user.player, Category.WEAPONS.bit());
            h.runAfterDelay(3, () -> {
                try (user) {
                    h.assertTrue(h.getLevel().getBlockState(blocked).is(Blocks.STONE),
                            "Deferred point rechecks the original scoped player's current weapon choice");
                    h.assertTrue(h.getLevel().getBlockState(allowed).isAir(),
                            "Disabling weapons does not cancel an independently attributed spell point");
                }
                h.succeed();
            });
        } catch (Throwable failure) {
            user.close();
            throw failure;
        }
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 80)
    public static void queuedAreasRecheckMasterAndDiscardCanceledWork(GameTestHelper h) {
        var user = new User(h);
        try {
            BlockPos first = stone(h, 4, 8);
            BlockPos second = stone(h, 5, 8);
            int[] completed = {0};
            int[] removed = {-1};
            int accepted = BlockBreakingSettings.withSource(Category.COMBAT_IMPACTS, user.player,
                    () -> SpellDestruction.area(h.getLevel(), null,
                            new AABB(Vec3.atLowerCornerOf(first), Vec3.atLowerCornerOf(second.offset(1, 1, 1))),
                            user.player.position(), 2, Float.MAX_VALUE, pos -> true, null,
                            count -> { completed[0]++; removed[0] = count; }));
            h.assertTrue(accepted == 2, "Enabled combat impact queues its actual terrain footprint");
            BlockBreakingSettings.setDisabledMask(user.player, Category.ALL.bit());
            h.runAfterDelay(3, () -> {
                try {
                    h.assertTrue(completed[0] == 1 && removed[0] == 0,
                            "Master disable cancels queued impact with a zero-removal completion");
                    h.assertTrue(h.getLevel().getBlockState(first).is(Blocks.STONE)
                                    && h.getLevel().getBlockState(second).is(Blocks.STONE),
                            "Master choice is checked before the queued impact mutates blocks");
                    BlockBreakingSettings.setDisabledMask(user.player, 0);
                    h.runAfterDelay(2, () -> {
                        try (user) {
                            h.assertTrue(completed[0] == 1 && h.getLevel().getBlockState(first).is(Blocks.STONE)
                                            && h.getLevel().getBlockState(second).is(Blocks.STONE),
                                    "Reenabling terrain effects cannot resurrect previously canceled work");
                        }
                        h.succeed();
                    });
                } catch (Throwable failure) {
                    user.close();
                    throw failure;
                }
            });
        } catch (Throwable failure) {
            user.close();
            throw failure;
        }
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 80)
    public static void queuedImpactRechecksSettingsBeforeEachMutation(GameTestHelper h) {
        var user = new User(h);
        try {
            BlockPos first = stone(h, 4, 8);
            for (int i = 1; i < 4; i++) stone(h, 4 + i, 8);
            int[] completed = {0};
            int[] removed = {-1};
            int[] callbacks = {0};
            int accepted = BlockBreakingSettings.withSource(Category.TECHNIQUES, user.player,
                    () -> SpellDestruction.area(h.getLevel(), null,
                            new AABB(Vec3.atLowerCornerOf(first), Vec3.atLowerCornerOf(first.offset(4, 1, 1))),
                            user.player.position(), 4, Float.MAX_VALUE, pos -> true,
                            pos -> {
                                callbacks[0]++;
                                BlockBreakingSettings.setDisabledMask(user.player, Category.TECHNIQUES.bit());
                            }, count -> { completed[0]++; removed[0] = count; }));
            h.assertTrue(accepted == 4, "Technique impact queues four stone blocks");
            h.runAfterDelay(3, () -> {
                try (user) {
                    int intact = 0;
                    for (int i = 0; i < 4; i++) {
                        if (h.getLevel().getBlockState(first.offset(i, 0, 0)).is(Blocks.STONE)) intact++;
                    }
                    h.assertTrue(completed[0] == 1 && removed[0] == 1 && callbacks[0] == 1 && intact == 3,
                            "A settings change during an impact stops every later block mutation");
                }
                h.succeed();
            });
        } catch (Throwable failure) {
            user.close();
            throw failure;
        }
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 80)
    public static void legacyExternalQiChoiceStillBlocksTerrainUntilExplicitlyEnabled(GameTestHelper h) {
        var user = new User(h);
        try {
            QiInfusedWeapon.setOutwardQiBlockBreaking(user.player, false);
            h.assertTrue(BlockBreakingSettings.disabledMask(user.player) == Category.EXTERNAL_QI.bit(),
                    "An existing legacy outward-Qi=false choice migrates without disabling unrelated effects");
            BlockPos qiBlock = stone(h, 4, 8);
            BlockPos spellBlock = stone(h, 5, 8);
            h.assertTrue(!SpellDestruction.breakExternalQiBlock(h.getLevel(), qiBlock, false, user.player)
                            && h.getLevel().getBlockState(qiBlock).is(Blocks.STONE),
                    "Legacy false still protects actual blocks from external Qi");
            h.assertTrue(impact(h, user.player, spellBlock, Category.SPELLS),
                    "Legacy external-Qi choice does not suppress ordinary spells");
            BlockBreakingSettings.setDisabledMask(user.player, 0);
            h.assertTrue(QiInfusedWeapon.outwardQiBlockBreakingEnabled(user.player)
                            && SpellDestruction.breakExternalQiBlock(h.getLevel(), qiBlock, false, user.player),
                    "Explicitly enabling the new choice updates the legacy gate and accepts terrain work");
            h.runAfterDelay(3, () -> {
                try (user) {
                    h.assertTrue(h.getLevel().getBlockState(qiBlock).isAir(),
                            "Reenabled external Qi removes the actual previously protected block");
                }
                h.succeed();
            });
        } catch (Throwable failure) {
            user.close();
            throw failure;
        }
    }

    @GameTest(template = "npc_combat_arena")
    public static void projectileAttributionUsesOwnerChoicesWithoutCompanionMisclassification(GameTestHelper h) {
        try (var user = new User(h)) {
            var projectile = new Snowball(h.getLevel(), user.player);
            try {
                BlockBreakingSettings.setDisabledMask(user.player, Category.WEAPONS.bit());
                h.assertTrue(!BlockBreakingSettings.enabled(projectile, Category.WEAPONS)
                                && BlockBreakingSettings.enabled(projectile, Category.SPELLS),
                        "Player projectile uses the owner's independent source-category choices");
                BlockBreakingSettings.setDisabledMask(user.player, Category.COMPANIONS.bit());
                BlockPos pos = stone(h, 4, 8);
                h.assertTrue(BlockBreakingSettings.withSource(Category.WEAPONS, projectile,
                                () -> SpellDestruction.eraseImpactBlockNow(h.getLevel(), pos, user.player)),
                        "Disabling companions does not suppress an owned weapon projectile");
            } finally {
                projectile.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void ownedCompanionProjectilesUseBothCompanionAndAttackChoices(GameTestHelper h) {
        try (var user = new User(h)) {
            h.getLevel().addNewPlayer(user.player);
            var companion = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 6, 2));
            companion.setNoAi(true);
            companion.setNoGravity(true);
            companion.getPersistentData().putUUID(UndeadControl.TAG_OWNER, user.player.getUUID());
            var projectile = new Snowball(h.getLevel(), companion);
            try {
                h.assertTrue(BlockBreakingSettings.playerOwner(projectile) == user.player,
                        "Projectile attribution resolves a real controlled creature's player UUID through the world");
                BlockPos pos = stone(h, 4, 8);
                BlockBreakingSettings.setDisabledMask(user.player, Category.COMPANIONS.bit());
                h.assertTrue(!BlockBreakingSettings.withSource(Category.WEAPONS, projectile,
                                () -> SpellDestruction.eraseImpactBlockNow(h.getLevel(), pos, user.player)),
                        "Companion-off blocks the companion's projectile even when weapons remain enabled");
                BlockBreakingSettings.setDisabledMask(user.player, Category.WEAPONS.bit());
                h.assertTrue(!BlockBreakingSettings.withSource(Category.WEAPONS, projectile,
                                () -> SpellDestruction.eraseImpactBlockNow(h.getLevel(), pos, user.player)),
                        "Weapon-off blocks the companion's weapon projectile even when companions remain enabled");
                h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE),
                        "Both independent choices preserve real terrain");
                BlockBreakingSettings.setDisabledMask(user.player, 0);
                h.assertTrue(BlockBreakingSettings.withSource(Category.WEAPONS, projectile,
                                () -> SpellDestruction.eraseImpactBlockNow(h.getLevel(), pos, user.player))
                                && h.getLevel().getBlockState(pos).isAir(),
                        "Enabling both choices restores actual companion projectile terrain effects");
            } finally {
                projectile.discard();
                companion.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void masterTerrainEffectDisableDoesNotBlockOrdinaryManualMining(GameTestHelper h) {
        try (var user = new User(h)) {
            BlockBreakingSettings.setDisabledMask(user.player, Category.ALL.bit());
            BlockPos pos = stone(h, 4, 8);
            h.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertTrue(user.player.gameMode.destroyBlock(pos) && h.getLevel().getBlockState(pos).isAir(),
                    "Disabling mod terrain effects leaves ordinary survival manual mining available");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void saveLoadAndClonePreserveIndependentAndLegacyChoices(GameTestHelper h) {
        try (var original = new User(h); var restored = new User(h); var clone = new User(h);
             var legacy = new User(h); var legacyClone = new User(h)) {
            int mask = Category.WEAPONS.bit() | Category.FLAME_VESSELS.bit() | Category.EXTERNAL_QI.bit();
            BlockBreakingSettings.setDisabledMask(original.player, mask);
            CompoundTag saved = new CompoundTag();
            original.player.saveWithoutId(saved);
            restored.player.load(saved);
            h.assertTrue(BlockBreakingSettings.disabledMask(restored.player) == mask
                            && !BlockBreakingSettings.enabled(restored.player, Category.WEAPONS)
                            && BlockBreakingSettings.enabled(restored.player, Category.SPELLS),
                    "Real player NBT serialization preserves independent terrain choices");

            QiInfusedWeapon.copyPersistentData(original.player, clone.player);
            BlockBreakingSettings.copy(original.player, clone.player);
            h.assertTrue(BlockBreakingSettings.disabledMask(clone.player) == mask
                            && !QiInfusedWeapon.outwardQiBlockBreakingEnabled(clone.player),
                    "Respawn clone hooks retain new settings and the synchronized legacy gate");
            QiInfusedWeapon.setOutwardQiBlockBreaking(legacy.player, false);
            QiInfusedWeapon.copyPersistentData(legacy.player, legacyClone.player);
            BlockBreakingSettings.copy(legacy.player, legacyClone.player);
            h.assertTrue(BlockBreakingSettings.disabledMask(legacyClone.player) == Category.EXTERNAL_QI.bit(),
                    "A legacy-only choice survives cloning before the new settings have ever been edited");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void invalidMaskBitsCannotDisableUnrelatedChoicesOrSurviveCloning(GameTestHelper h) {
        try (var user = new User(h); var clone = new User(h)) {
            int unexpected = Integer.MIN_VALUE | (1 << 25) | (1 << 9);
            BlockBreakingSettings.setDisabledMask(user.player, unexpected | Category.WEAPONS.bit());
            h.assertTrue(BlockBreakingSettings.disabledMask(user.player) == Category.WEAPONS.bit()
                            && user.player.getPersistentData().getInt(SETTINGS_TAG) == Category.WEAPONS.bit(),
                    "Untrusted high mask bits are removed before persistent storage");
            h.assertTrue(BlockBreakingSettings.enabled(user.player, Category.SPELLS)
                            && BlockBreakingSettings.enabled(user.player, Category.EXTERNAL_QI),
                    "Unknown bits cannot suppress independent spell or Qi choices");
            user.player.getPersistentData().putInt(SETTINGS_TAG, unexpected | Category.FLAME_VESSELS.bit());
            BlockBreakingSettings.copy(user.player, clone.player);
            h.assertTrue(clone.player.getPersistentData().getInt(SETTINGS_TAG) == Category.FLAME_VESSELS.bit(),
                    "Cloning sanitizes settings from malformed saved NBT too");
            BlockBreakingSettings.setDisabledMask(user.player, -1);
            h.assertTrue(BlockBreakingSettings.disabledMask(user.player) == BlockBreakingSettings.validMask(),
                    "An all-bits payload retains only registered settings");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void nestedAndThrowingSourceScopesCannotLeakTerrainAttribution(GameTestHelper h) {
        try (var user = new User(h)) {
            BlockBreakingSettings.setDisabledMask(user.player, Category.SPELLS.bit());
            BlockPos first = stone(h, 4, 8);
            BlockPos second = stone(h, 5, 8);
            BlockBreakingSettings.runWithSource(Category.WEAPONS, user.player, () -> {
                try {
                    BlockBreakingSettings.runWithSource(Category.SPELLS, user.player, () -> {
                        h.assertTrue(!SpellDestruction.eraseImpactBlockNow(h.getLevel(), first, user.player),
                                "Nested spell scope uses its own disabled choice");
                        throw new IllegalStateException("test source scope");
                    });
                } catch (IllegalStateException expected) {
                    h.assertTrue("test source scope".equals(expected.getMessage()), "Only the test exception is expected");
                }
                h.assertTrue(SpellDestruction.eraseImpactBlockNow(h.getLevel(), first, user.player),
                        "A throwing nested source restores the enclosing weapon category");
            });
            h.assertTrue(!SpellDestruction.eraseImpactBlockNow(h.getLevel(), second, user.player)
                            && h.getLevel().getBlockState(second).is(Blocks.STONE),
                    "After the outer scope ends, unscoped work again honors disabled spells");
        }
        h.succeed();
    }
}
