package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.compat.jei.AcquisitionCatalog;
import com.example.immortal_cultivation_mod.event.VaultProtectionGameTests;
import com.example.immortal_cultivation_mod.item.BeastSpawnEggItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_legacy_realms")
@PrefixGameTestTemplate(false)
public final class LegacyRealmMigrationGameTests {
    @GameTest(template = "fate_test")
    public static void unifiedGuardianEggSpawnsBothRealmsAndSupportsLegacyRegistryIds(GameTestHelper h) {
        var type = ModEntities.VAULT_GUARDIAN.get();
        var egg = (BeastSpawnEggItem)ModItems.VAULT_GUARDIAN_SPAWN_EGG.get();
        var stack = new ItemStack(egg);
        h.assertTrue(BeastRealms.variants(type).equals(List.of(2, 3)), "Guardian preserves its two existing realms");
        for (int realm : BeastRealms.variants(type)) {
            egg.cycleRealm(stack);
            var guardian = type.spawn(h.getLevel(), stack, null, h.absolutePos(new BlockPos(3, 2, 3)),
                    MobSpawnType.SPAWN_EGG, false, false);
            try {
                h.assertTrue(guardian != null && guardian.beastRealm() == realm
                                && guardian.isJindanVariant() == (realm == 3)
                                && guardian.getMaxHealth() == CultivationLevels.getLevelDef(BeastRealms.level(realm)).maxHp(),
                        "Actual egg placement transfers realm, current HP and guardian abilities");
                h.assertTrue(guardian.getName().getString().startsWith("[") && stack.getHoverName().getString().startsWith("["),
                        "Guardian and selected egg display bracketed realm before species name");
            } finally { if (guardian != null) guardian.discard(); }
            var oldId = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",
                    (realm == 2 ? "zhuji" : "jindan") + "_vault_guardian");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(oldId) == type
                            && BuiltInRegistries.ITEM.get(oldId.withPath(oldId.getPath() + "_spawn_egg")) == egg,
                    "Old guardian IDs resolve without duplicate entities or eggs");
        }
        egg.cycleRealm(stack);
        h.assertTrue(BeastSpawnEggItem.selectedRealm(stack) == -1, "Guardian egg cycles back to random realm");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oldFoxAndGuardianSavesMigrateStatsInjuryRealmAndVaultLinks(GameTestHelper h) {
        for (boolean fox : List.of(false, true)) {
            for (int realm : fox ? List.of(2, 3, 4) : List.of(2, 3)) {
                EntityType<? extends Mob> type = fox ? ModEntities.PHANTOM_VEIL_FOX.get() : ModEntities.VAULT_GUARDIAN.get();
                var original = type.create(h.getLevel());
                double oldHp = fox ? (realm == 2 ? 1200 : realm == 3 ? 4500 : 15000) : (realm == 2 ? 2800 : 6800);
                original.getAttribute(Attributes.MAX_HEALTH).setBaseValue(oldHp);
                original.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(777);
                original.setHealth((float)(oldHp * .37));
                var saved = new CompoundTag();
                original.saveWithoutId(saved);
                saved.remove(BeastRealms.NBT_KEY);
                String path = fox ? (realm == 4 ? "phantom_veil_fox" : (realm == 2 ? "zhuji" : "jindan") + "_phantom_veil_fox")
                        : (realm == 2 ? "zhuji" : "jindan") + "_vault_guardian";
                saved.putString("id", "immortal_cultivation_mod:" + path);
                saved.putBoolean("HomeInitialized", true);
                saved.putInt("GuardHomeX", 44);
                saved.putInt("FurnacePressure", 36);
                saved.putBoolean("SmallSectGuardian", true);
                saved.putString("SmallSectInstance", "legacy-test-sect");
                var restored = (Mob)EntityType.loadEntityRecursive(saved, h.getLevel(), entity -> entity);
                h.assertTrue(restored instanceof RealmBeast beast && beast.beastRealm() == realm,
                        "Legacy entity ID migrates the original realm");
                double expectedHp = CultivationLevels.getLevelDef(BeastRealms.level(realm)).maxHp();
                double expectedAttack = (fox ? 520 : 240) * BeastRealms.powerScale(realm, fox ? 4 : 3);
                h.assertTrue(Math.abs(restored.getMaxHealth() - expectedHp) < .01
                                && Math.abs(restored.getHealth() / expectedHp - .37) < .001
                                && Math.abs(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) - expectedAttack) < .001,
                        "Legacy HP/attack are replaced by current realm values while preserving injury fraction");
                var migrated = new CompoundTag();
                restored.saveWithoutId(migrated);
                for (int repeat = 0; repeat < 3; repeat++) restored.load(migrated);
                h.assertTrue(((RealmBeast)restored).beastRealm() == realm
                                && Math.abs(restored.getMaxHealth() - expectedHp) < .01
                                && Math.abs(restored.getHealth() / expectedHp - .37) < .001,
                        "Repeated save/load never compounds HP or heals injuries");
                if (!fox) {
                    var guardian = (VaultGuardianEntity)restored;
                    var state = new CompoundTag();
                    guardian.saveWithoutId(state);
                    h.assertTrue(guardian.isSmallSectGuardian() && guardian.getFurnacePressure() == 36
                                    && state.getInt("GuardHomeX") == 44 && state.getString("SmallSectInstance").equals("legacy-test-sect"),
                            "Migration preserves furnace pressure, anchored home and sect identity");
                }
                original.discard();
                restored.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void actualMeleeTechniquesAndGuardianProjectilesScaleExactlyOnce(GameTestHelper h) throws Exception {
        var target = EntityType.COW.create(h.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        target.setNoAi(true);
        h.getLevel().addFreshEntity(target);
        var hurtAbility = VaultGuardianEntity.class.getDeclaredMethod("hurtAbilityTarget", LivingEntity.class, float.class, boolean.class);
        hurtAbility.setAccessible(true);
        try {
            for (boolean fox : List.of(false, true)) {
                for (int realm : fox ? List.of(2, 3, 4) : List.of(2, 3)) {
                    Mob mob = fox ? ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel()) : ModEntities.VAULT_GUARDIAN.get().create(h.getLevel());
                    mob.setNoAi(true);
                    ((RealmBeast)mob).setBeastRealm(realm);
                    h.getLevel().addFreshEntity(mob);
                    try {
                        double scale = BeastRealms.powerScale(mob);
                        reset(target);
                        if (fox) mob.doHurtTarget(target);
                        else {
                            mob.doHurtTarget(target);
                            var basic = VaultGuardianEntity.class.getDeclaredMethod("tickBasicStrike",
                                    net.minecraft.server.level.ServerLevel.class, int.class, LivingEntity.class);
                            basic.setAccessible(true);
                            basic.invoke(mob, h.getLevel(), 5, target);
                        }
                        h.assertTrue(Math.abs((10000 - target.getHealth()) - (fox ? 520 : 240) * scale) < .01,
                                "Actual melee damage uses current realm scale exactly once");
                        reset(target);
                        if (fox) target.hurt(mob.damageSources().indirectMagic(mob, mob), ((PhantomVeilFoxEntity)mob).techniqueDamage(900));
                        else hurtAbility.invoke(mob, target, 380F, false);
                        h.assertTrue(Math.abs((10000 - target.getHealth()) - (fox ? 900 : 380) * scale) < .01,
                                "Actual technique damage uses the same realm ratio, without legacy multipliers");
                        if (!fox) {
                            reset(target);
                            var guardian = (VaultGuardianEntity)mob;
                            var spike = new VaultSpiritSpikeEntity(h.getLevel(), guardian, target, guardian.techniqueDamage(80), false);
                            spike.onHitEntity(new EntityHitResult(target));
                            h.assertTrue(Math.abs((10000 - target.getHealth()) - 80 * scale) < .01,
                                    "Guardian spike owner does not cause a second realm damage multiplier");
                        }
                    } finally { mob.discard(); }
                }
            }
        } finally { target.discard(); }
        h.succeed();
    }

    private static void reset(LivingEntity target) {
        target.invulnerableTime = 0;
        target.setHealth(10000);
    }

    @GameTest(template = "fate_test")
    public static void guardianLootAndJeiRemainDiscoverableWithoutDuplicateRealmEntities(GameTestHelper h) {
        var recipes = AcquisitionCatalog.collect();
        for (int realm : List.of(2, 3)) {
            var guardian = ModEntities.VAULT_GUARDIAN.get().create(h.getLevel());
            guardian.setBeastRealm(realm);
            h.getLevel().addFreshEntity(guardian);
            var drops = new java.util.ArrayList<ItemStack>();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDropsEvent> listener = event -> {
                if (event.getEntity() == guardian) event.getDrops().forEach(item -> drops.add(item.getItem().copy()));
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, listener);
            try {
                guardian.setHealth(0);
                guardian.die(guardian.damageSources().genericKill());
                h.assertTrue(drops.stream().anyMatch(stack -> stack.is(ModItems.XUANDUAN_IRON_SHARD.get()))
                                && drops.stream().anyMatch(stack -> stack.is(ModItems.SPIRIT_GUIDING_GEAR.get()))
                                && drops.stream().anyMatch(stack -> stack.is(ModItems.DAMAGED_FORGING_FURNACE_CORE.get())),
                        "Unified guardian actually drops original construct materials for each realm");
            } finally {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
                guardian.discard();
            }
            h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.sourceIcon().is(ModItems.VAULT_GUARDIAN_SPAWN_EGG.get())
                            && BeastSpawnEggItem.selectedRealm(recipe.sourceIcon()) == realm
                            && recipe.result().is(ModItems.DAMAGED_FORGING_FURNACE_CORE.get())),
                    "JEI shows guardian drops using its unified egg with correct realm");
            boolean bonus = recipes.stream().anyMatch(recipe -> recipe.sourceIcon().is(ModItems.VAULT_GUARDIAN_SPAWN_EGG.get())
                    && BeastSpawnEggItem.selectedRealm(recipe.sourceIcon()) == realm
                    && recipe.id().getPath().contains("/entities/weapon_care_boss_bonus/"));
            h.assertTrue(bonus == (realm == 3), "Only Golden Core guardian retains its existing boss bonus loot");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void guardianStillProtectsSectTerrain(GameTestHelper h) { VaultProtectionGameTests.guardianAttackCannotExcavateGuardedSect(h); }

    @GameTest(template = "npc_combat_arena")
    public static void pendingGuardianStillLocksSect(GameTestHelper h) { VaultProtectionGameTests.sectStaysLockedBeforeGuardianLoads(h); }

    @GameTest(template = "npc_combat_arena")
    public static void guardianReloadRepairsSectLock(GameTestHelper h) { VaultProtectionGameTests.livingGuardianRepairsDefeatAndStopsRealQiParticles(h); }

    @GameTest(template = "npc_combat_arena")
    public static void queuedQiStillChecksGuardian(GameTestHelper h) throws Exception { VaultProtectionGameTests.queuedQiRechecksRestoredGuardianLock(h); }

    @GameTest(template = "npc_combat_arena")
    public static void finalGuardianDeathStillUnlocksSect(GameTestHelper h) { VaultProtectionGameTests.lastGuardianDeathUnlocksButPlacedBlocksStayExempt(h); }
}
