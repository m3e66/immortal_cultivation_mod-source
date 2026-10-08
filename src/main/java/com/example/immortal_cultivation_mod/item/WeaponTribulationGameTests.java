package com.example.immortal_cultivation_mod.item;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class WeaponTribulationGameTests {
    private static ItemStack weapon(int tier) {
        var stack = new ItemStack(ModItems.DAO.get());
        int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
        layers[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()] = 72;
        for (int i = 0; i < tier; i++) { WeaponRestrictionSystem.addRestrictions(stack, layers); WeaponRestrictionSystem.evolve(stack); }
        WeaponRestrictionSystem.addRestrictions(stack, layers); return stack;
    }
    private static final class Player implements AutoCloseable {
        final ServerPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        final GameTestHelper helper;
        private boolean lookupRegistered;
        private ServerPlayer previousLookup;
        Player(GameTestHelper helper) {
            this(helper, UUID.randomUUID());
        }
        Player(GameTestHelper helper, UUID id) {
            this.helper = helper;
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(id, "weapon-trial"),
                    net.minecraft.server.level.ClientInformation.createDefault());
            try {
                var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); field.setAccessible(true); field.setInt(player, 0);
            } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot expire test spawn protection", e); }
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            var position = helper.absolutePos(new BlockPos(3, 2, 3)); player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
        }
        @SuppressWarnings("unchecked")
        private java.util.List<net.minecraft.server.level.ServerPlayer> mutablePlayers() {
            try {
                var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("players"); field.setAccessible(true);
                return (java.util.List<net.minecraft.server.level.ServerPlayer>) field.get(player.server.getPlayerList());
            } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot register test player", e); }
        }
        void online() { mutablePlayers().add(player); }
        @SuppressWarnings("unchecked")
        private Map<UUID, ServerPlayer> playersById() {
            try {
                var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
                field.setAccessible(true);
                return (Map<UUID, ServerPlayer>) field.get(player.server.getPlayerList());
            } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot register test player UUID lookup", e); }
        }
        void onlineWithLookup() {
            online();
            previousLookup = playersById().put(player.getUUID(), player);
            lookupRegistered = true;
            helper.assertTrue(player.server.getPlayerList().getPlayer(player.getUUID()) == player,
                    "Trial reward tests use the real online owner UUID lookup");
        }
        @Override public void close() {
            var data = WeaponTribulationData.get(player.server);
            for (var trial : new ArrayList<>(data.trials.values())) if (trial.owner.equals(player.getUUID())) WeaponTribulation.abort(player.server, trial.id);
            if (lookupRegistered && playersById().get(player.getUUID()) == player) {
                if (previousLookup == null) playersById().remove(player.getUUID());
                else playersById().put(player.getUUID(), previousLookup);
            }
            mutablePlayers().remove(player); player.discard(); channel.close();
        }
    }
    private static WeaponTribulationData.Trial trial(Player p) {
        return WeaponTribulationData.get(p.player.server).trials.values().stream().filter(t -> t.owner.equals(p.player.getUUID())).findFirst().orElseThrow();
    }
    private static ItemEntity entity(Player p, UUID id) {
        for (var level : p.player.server.getAllLevels()) for (var entity : level.getAllEntities())
            if (entity instanceof ItemEntity item && id.equals(WeaponTribulation.token(item.getItem()))) return item;
        throw new AssertionError("Missing trial item");
    }
    @GameTest(template = "fate_test")
    public static void powerUsesActualModifiersAndScalesSequence(GameTestHelper h) {
        var weak = weapon(0); var strong = weak.copy();
        var modifiers = ItemAttributeModifiers.builder();
        for (var entry : strong.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers())
            modifiers.add(entry.attribute(), entry.modifier(), entry.slot());
        modifiers.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "trial_test_damage"), 20,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        strong.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.build());
        double a = WeaponTribulation.power(weak), b = WeaponTribulation.power(strong);
        h.assertTrue(b > a && WeaponTribulation.strikeDamage(b) > WeaponTribulation.strikeDamage(a)
                && WeaponTribulation.duration(b) > WeaponTribulation.duration(a), "Actual attack multipliers increase trial power, damage and duration");
        int max = weak.getMaxDamage(); WeaponTribulation.damageWeapon(weak);
        h.assertTrue(weak.getDamageValue() == (int)Math.ceil(max * .1) && weak.getMaxDamage() == max, "Each weapon strike consumes ten percent of maximum durability");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void forgedThunderResistanceReducesRealHeldStrikesAndPersists(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online();
            p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            p.player.setHealth(1000);
            var recipe = ForgingSystem.recipeById("dao");
            var stack = ForgingSystem.temper(ForgingSystem.createMold(recipe, List.of(), 3500, "thunder_fire"));
            WeaponRestrictionSystem.setTier(stack, 2);
            int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
            layers[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()] = 72;
            h.assertTrue(WeaponRestrictionSystem.addRestrictions(stack, layers), "Prepare a real thunder-forged breakthrough weapon");
            stack = net.minecraft.world.item.ItemStack.parse(h.getLevel().registryAccess(), stack.save(h.getLevel().registryAccess())).orElseThrow();
            p.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            h.assertTrue(WeaponTribulation.start(p.player, stack), "A persisted thunder-forged weapon starts its normal ranked trial");
            var sequence = trial(p);
            float before = p.player.getHealth();
            tickToNextStrike(p, sequence);
            h.assertTrue(Math.abs(before - p.player.getHealth() - sequence.damage * .85F) < .002F
                            && sequence.damage == WeaponTribulation.strikeDamage(sequence.power, 2)
                            && stack.getDamageValue() == 0,
                    "The actual held lightning strike applies forged resistance after rank scaling and spends player health");
            h.assertTrue(WeaponTribulation.flameResistanceMultiplier(new ItemStack(ModItems.DAO.get())) == 1,
                    "Weapons without the forged bonus retain their original lightning damage");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void equalStatWeaponsDoubleHeldLightningDamageAtEachRank(GameTestHelper h) {
        final float initialHealth = 2000;
        double expectedPower = WeaponTribulation.power(equalStatWeapon(0));
        float baselineDamage = WeaponTribulation.strikeDamage(expectedPower);
        int baselineDuration = 0, baselineStrikes = 0;
        float previousDamage = 0;
        for (int rank = 0; rank <= 6; rank++) {
            try (var p = new Player(h)) {
                p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(initialHealth); p.player.setHealth(initialHealth);
                var stack = equalStatWeapon(rank);
                h.assertTrue(WeaponTribulation.power(stack) == expectedPower,
                        "Raw restriction rank does not change this fixture's weapon stats or calculated power");
                p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                h.assertTrue(WeaponTribulation.start(p.player, stack), "Every unfinished rank below maximum can begin its trial");
                var trial = trial(p);
                float expectedDamage = Math.scalb(baselineDamage, rank);
                h.assertTrue(trial.originalTier == rank && trial.power == expectedPower
                                && Math.abs(trial.damage - expectedDamage) < .001
                                && trial.damage > previousDamage,
                        "Starting a trial snapshots the original rank and doubles lightning damage per stage");
                if (rank == 0) {
                    baselineDuration = trial.duration; baselineStrikes = trial.strikes;
                } else {
                    h.assertTrue(trial.duration == baselineDuration && trial.strikes == baselineStrikes,
                            "Equal power keeps sequence duration and strike count identical while rank increases damage");
                }
                tickToNextStrike(p, trial);
                float lostHealth = initialHealth - p.player.getHealth();
                h.assertTrue(p.player.isAlive() && Math.abs(lostHealth - expectedDamage) < .002
                                && stack.getDamageValue() == 0 && trial.hits == 1,
                        "The real held lightning strike doubles player health damage and leaves weapon durability intact");
                previousDamage = trial.damage;
            }
        }
        h.assertTrue(WeaponTribulation.strikeDamage(expectedPower, -5) == baselineDamage
                        && WeaponTribulation.strikeDamage(expectedPower, 100) == previousDamage,
                "Out-of-range original ranks clamp to the first and last breakthrough stages");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void scaledDamageAboveOldCapPersistsAndLegacyMigrationRunsOnce(GameTestHelper h) {
        try (var p = new Player(h)) {
            var stack = equalStatWeapon(6);
            stack.set(DataComponents.MAX_DAMAGE, 50_000_000);
            h.assertTrue(WeaponTribulation.start(p.player, stack), "A powerful last-stage weapon starts its actual trial");
            var original = trial(p); original.age = 123; original.hits = 2; original.groundOnly = true;
            h.assertTrue(original.damage > 64000 && original.damage <= 128000,
                    "Strengthened high-rank lightning can exceed the old ranked 64000-damage ceiling");
            var registries = h.getLevel().registryAccess();
            var saved = WeaponTribulationData.get(p.player.server).save(new net.minecraft.nbt.CompoundTag(), registries);
            h.assertTrue(savedTrial(saved, original.id).getInt("damage_version") == 2,
                    "New trials mark persisted damage as already strengthened and rank-scaled");
            var loaded = WeaponTribulationData.load(saved, registries);
            for (int reload = 0; reload < 3; reload++) {
                var restored = loaded.trials.get(original.id);
                h.assertTrue(restored.damage == original.damage && restored.originalTier == 6
                                && restored.power == original.power && restored.age == 123 && restored.hits == 2
                                && restored.groundOnly && Objects.equals(restored.entity, original.entity)
                                && restored.owner.equals(original.owner) && restored.dimension.equals(original.dimension)
                                && restored.position.equals(original.position) && restored.duration == original.duration
                                && restored.strikes == original.strikes,
                        "Repeated current-version saves preserve large damage and sequence progress without scaling again");
                loaded = WeaponTribulationData.load(loaded.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
            }
            var legacy = saved.copy();
            var legacyTrial = savedTrial(legacy, original.id);
            legacyTrial.remove("damage_version");
            legacyTrial.putFloat("damage", WeaponTribulation.strikeDamage(original.power) / 2);
            loaded = WeaponTribulationData.load(legacy, registries);
            for (int reload = 0; reload < 3; reload++) {
                var migrated = loaded.trials.get(original.id);
                h.assertTrue(migrated.damage == original.damage && migrated.age == 123 && migrated.hits == 2
                                && migrated.groundOnly && Objects.equals(migrated.entity, original.entity)
                                && migrated.duration == original.duration && migrated.strikes == original.strikes,
                        "Legacy saves migrate from original power and rank exactly once while retaining lightning progress");
                var rewritten = loaded.save(new net.minecraft.nbt.CompoundTag(), registries);
                h.assertTrue(savedTrial(rewritten, original.id).getInt("damage_version") == 2,
                        "Migrated saves write the damage version so later reloads cannot multiply damage again");
                loaded = WeaponTribulationData.load(rewritten, registries);
            }
            var excessive = saved.copy();
            savedTrial(excessive, original.id).putFloat("damage", Float.MAX_VALUE);
            h.assertTrue(WeaponTribulationData.load(excessive, registries).trials.get(original.id).damage == 128000
                            && WeaponTribulation.strikeDamage(1.0E12, 6) == 128000,
                    "Runtime calculation and persisted scaled damage both respect the new 128000 ceiling");
            var item = entity(p, original.id);
            WeaponTribulation.abort(p.player.server, original.id); item.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void strengthenedLightningDoublesKnownDamageWithoutExtendingSequence(GameTestHelper h) {
        h.assertTrue(WeaponTribulation.strikeDamage(0) == 4
                        && WeaponTribulation.strikeDamage(100) == 17
                        && WeaponTribulation.strikeDamage(400) == 30
                        && WeaponTribulation.strikeDamage(1.0E12) == 2000,
                "The strengthened stat curve doubles the former 2, 8.5, 15 and 1000 damage values");
        for (int rank = 0; rank <= 6; rank++) {
            h.assertTrue(WeaponTribulation.strikeDamage(100, rank) == Math.scalb(17F, rank),
                    "Each original restriction rank keeps its existing multiplier on the doubled base damage");
        }
        h.assertTrue(WeaponTribulation.strikeDamage(1.0E12, 6) == 128000
                        && WeaponTribulation.strikeCount(100) == 8 && WeaponTribulation.duration(100) == 520,
                "Doubling health damage raises the ranked cap while keeping the same eight-strike, 520-tick sequence");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void versionOneRankedSavesMigrateOnceAndVersionTwoKeepsItsSnapshot(GameTestHelper h) {
        try (var p = new Player(h)) {
            var stack = equalStatWeapon(3);
            h.assertTrue(WeaponTribulation.start(p.player, stack), "Create an actual ground-custody trial for save migration");
            var original = trial(p);
            original.age = 87; original.hits = 1; original.groundOnly = true;
            var registries = h.getLevel().registryAccess();
            var legacy = WeaponTribulationData.get(p.player.server).save(new net.minecraft.nbt.CompoundTag(), registries);
            var saved = savedTrial(legacy, original.id);
            saved.putInt("damage_version", 1);
            saved.putFloat("damage", original.damage / 2);
            var loaded = WeaponTribulationData.load(legacy, registries);
            for (int reload = 0; reload < 3; reload++) {
                var migrated = loaded.trials.get(original.id);
                h.assertTrue(migrated.damage == original.damage && migrated.originalTier == 3
                                && migrated.age == 87 && migrated.hits == 1 && migrated.power == original.power
                                && migrated.duration == original.duration && migrated.strikes == original.strikes
                                && migrated.groundOnly && Objects.equals(migrated.entity, original.entity)
                                && migrated.owner.equals(original.owner) && migrated.dimension.equals(original.dimension)
                                && migrated.position.equals(original.position) && countGroundWeapons(p, original.id) == 1,
                        "Version-one ranked saves migrate once without restarting progress, transferring custody or duplicating the weapon");
                var rewritten = loaded.save(new net.minecraft.nbt.CompoundTag(), registries);
                h.assertTrue(savedTrial(rewritten, original.id).getInt("damage_version") == 2,
                        "A migrated rank-scaled save records the strengthened damage version");
                loaded = WeaponTribulationData.load(rewritten, registries);
            }
            var staleDamage = legacy.copy();
            savedTrial(staleDamage, original.id).putFloat("damage", 7);
            h.assertTrue(WeaponTribulationData.load(staleDamage, registries).trials.get(original.id).damage == original.damage,
                    "Legacy migration calculates from stored weapon power and rank rather than blindly doubling a stale damage field");
            var current = loaded.save(new net.minecraft.nbt.CompoundTag(), registries);
            float snapshot = original.damage * .75F;
            savedTrial(current, original.id).putFloat("damage", snapshot);
            loaded = WeaponTribulationData.load(current, registries);
            for (int reload = 0; reload < 3; reload++) {
                var restored = loaded.trials.get(original.id);
                h.assertTrue(restored.damage == snapshot && restored.age == 87 && restored.hits == 1
                                && restored.groundOnly && Objects.equals(restored.entity, original.entity),
                        "Version-two reloads preserve a valid saved damage snapshot and do not recalculate or scale it again");
                loaded = WeaponTribulationData.load(loaded.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
            }
            var item = entity(p, original.id);
            WeaponTribulation.abort(p.player.server, original.id); item.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void higherRankGroundedLightningStillCostsTenPercentDurability(GameTestHelper h) {
        double expectedPower = WeaponTribulation.power(equalStatWeapon(0));
        for (int rank = 0; rank <= 6; rank++) {
            try (var p = new Player(h)) {
                var stack = equalStatWeapon(rank);
                h.assertTrue(WeaponTribulation.start(p.player, stack), "A ground-targeted trial starts at each original rank");
                var trial = trial(p); var item = entity(p, trial.id);
                int maximum = item.getItem().getMaxDamage();
                int perHit = (int)Math.ceil(maximum * .1);
                h.assertTrue(trial.power == expectedPower, "Ground fixtures have equal weapon power across all ranks");
                for (int hit = 1; hit <= 2; hit++) {
                    tickToNextStrike(p, trial);
                    h.assertTrue(item.isAlive() && item.getItem().getDamageValue() == hit * perHit
                                    && item.getItem().getMaxDamage() == maximum && p.player.getHealth() == 20
                                    && WeaponRestrictionSystem.tier(item.getItem()) == rank,
                            "Higher lightning damage cannot multiply weapon wear or promote a ground weapon early");
                }
                WeaponTribulation.abort(p.player.server, trial.id); item.discard();
            }
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void droppedWeaponSurvivesThenAdvancesOnlyAtEnd(GameTestHelper h) {
        try (var p = new Player(h)) {
            var stack = weapon(0);
            h.assertTrue(WeaponTribulation.start(p.player, stack), "Valid completed cycle starts trial");
            var trial = trial(p); var item = entity(p, trial.id);
            h.assertTrue(WeaponRestrictionSystem.tier(item.getItem()) == 0 && !WeaponRestrictionSystem.evolve(item.getItem()), "Active trial cannot evolve early");
            h.assertTrue(!WeaponTribulation.start(p.player, item.getItem()), "Duplicate start rejected");
            h.assertTrue(!HopperBlockEntity.addItem(new SimpleContainer(5), item), "Hopper cannot hide trial weapon");
            for (int i = 0; i < trial.duration; i++) WeaponTribulation.tick(p.player.server);
            h.assertTrue(!WeaponTribulationData.get(p.player.server).trials.containsKey(trial.id)
                    && WeaponRestrictionSystem.tier(item.getItem()) == 1 && WeaponRestrictionSystem.tierProgress(item.getItem()) == 0,
                    "Surviving full sequence advances one stage and merges restrictions");
            h.assertTrue(item.getItem().getDamageValue() > 0 && !item.isInvulnerable() && !WeaponTribulation.active(item.getItem()), "Completed item retains damage and releases trial protection");
            item.discard();
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void lethalDurabilityStrikeShattersWithoutPromotion(GameTestHelper h) {
        try (var p = new Player(h)) {
            var stack = weapon(1); stack.setDamageValue(stack.getMaxDamage() - 1);
            WeaponTribulation.start(p.player, stack); var trial = trial(p); var item = entity(p, trial.id);
            trial.age = trial.duration - 1;
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(item.isRemoved() && !WeaponTribulationData.get(p.player.server).trials.containsKey(trial.id), "Durability failure destroys weapon and ends trial");
            int shards = 0;
            for (var e : h.getLevel().getAllEntities()) if (e instanceof ItemEntity drop && WeaponShardSystem.populated(drop.getItem())) {
                var data = WeaponShardSystem.data(drop.getItem());
                if (data.getInt("tier") == 1 && drop.distanceToSqr(p.player) < 9) { shards++; drop.discard(); }
            }
            h.assertTrue(shards == 1, "Failed spirit weapon creates one shard at old grade, never advances");
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void heldTrialDamagesHolderAndLeavesWeaponIntact(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.player.setHealth(1000);
            var stack = weapon(0); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p);
            for (int i = 0; i < trial.duration; i++) WeaponTribulation.tick(p.player.server);
            h.assertTrue(p.player.getHealth() < 1000 && p.player.isAlive() && stack.getDamageValue() == 0, "Lightning targets holder rather than weapon durability");
            h.assertTrue(WeaponRestrictionSystem.tier(stack) == 1 && !WeaponTribulation.active(stack), "Holder survives full duration and weapon advances");
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void deathForcesExactlyOneDropWithKeepInventory(GameTestHelper h) {
        boolean previous = h.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, h.getLevel().getServer());
        try (var p = new Player(h)) {
            p.online(); var stack = weapon(1); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p); trial.hits = 2;
            WeaponTribulation.death(new LivingDeathEvent(p.player, h.getLevel().damageSources().lightningBolt()));
            WeaponTribulation.forceDrop(p.player, true);
            h.assertTrue(p.player.getMainHandItem().isEmpty() && trial.groundOnly && trial.hits == 1, "Death removes weapon before keepInventory retains other items and repeats fatal strike on weapon");
            var item = entity(p, trial.id); int damage = item.getItem().getDamageValue(); trial.age = trial.duration - 1;
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(item.getItem().getDamageValue() > damage, "After death lightning damages weapon entity");
            int count = 0;
            for (var e : h.getLevel().getAllEntities()) if (e instanceof ItemEntity drop && trial.id.equals(WeaponTribulation.token(drop.getItem()))) count++;
            h.assertTrue(count == 1, "Repeated death cleanup cannot duplicate weapon");
            WeaponTribulation.abort(p.player.server, trial.id); item.discard();
        } finally { h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(previous, h.getLevel().getServer()); }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void savedProgressAndLogoutCannotResetLightning(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); var stack = weapon(1); p.player.setItemInHand(InteractionHand.OFF_HAND, stack);
            WeaponTribulation.start(p.player, stack); var original = trial(p); original.age = 123; original.hits = 2;
            var data = WeaponTribulationData.get(p.player.server);
            var loaded = WeaponTribulationData.load(data.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            var restored = loaded.trials.get(original.id);
            h.assertTrue(restored.age == 123 && restored.hits == 2 && restored.duration == original.duration && restored.damage == original.damage,
                    "Save/restart preserves elapsed time, strikes, power and damage");
            data.trials.put(restored.id, restored);
            WeaponTribulation.forceDrop(p.player, false);
            h.assertTrue(p.player.getOffhandItem().isEmpty() && restored.groundOnly && restored.age == 123 && restored.hits == 2,
                    "Logout forcibly transfers real weapon to item entity, preserving progress");
            var item = entity(p, restored.id); WeaponTribulation.abort(p.player.server, restored.id); item.discard();
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void disappearingItemFailsWithoutRecreatingWeapon(GameTestHelper h) {
        try (var p = new Player(h)) {
            WeaponTribulation.start(p.player, weapon(0)); var trial = trial(p); entity(p, trial.id).discard();
            for (int i = 0; i < 200; i++) WeaponTribulation.tick(p.player.server);
            h.assertTrue(!WeaponTribulationData.get(p.player.server).trials.containsKey(trial.id) && trial.age == 0,
                    "Missing entity fails without consuming time or awarding advancement");
            int count = 0;
            for (var e : h.getLevel().getAllEntities()) if (e instanceof ItemEntity drop && trial.id.equals(WeaponTribulation.token(drop.getItem())) && !drop.isRemoved()) count++;
            h.assertTrue(count == 0, "Missing item is never recreated or duplicated");
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void activeWeaponCannotBeHiddenInChest(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); var stack = weapon(0); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var id = trial(p).id;
            var container = new SimpleContainer(27); var menu = ChestMenu.threeRows(3, p.player.getInventory(), container);
            p.player.containerMenu = menu; menu.clicked(54, 0, ClickType.QUICK_MOVE, p.player);
            h.assertTrue(container.isEmpty() && p.player.getMainHandItem() == stack, "Container menu cannot transfer active weapon out of player inventory");
            WeaponTribulation.abort(p.player.server, id);
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void nativeDeathKeepsOtherItemsButDropsTrialWeapon(GameTestHelper h) {
        boolean previous = h.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, h.getLevel().getServer());
        try (var p = new Player(h)) {
            p.online(); var stack = weapon(1); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            p.player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
            WeaponTribulation.start(p.player, stack); var trial = trial(p);
            p.player.setHealth(0); p.player.die(h.getLevel().damageSources().lightningBolt());
            h.assertTrue(p.player.getMainHandItem().isEmpty() && p.player.getInventory().getItem(1).getCount() == 3,
                    "Vanilla death with keepInventory retains ordinary items but must remove tribulating weapon");
            var item = entity(p, trial.id);
            h.assertTrue(item.isAlive() && trial.groundOnly, "Real death redirects remaining lightning to dropped item");
            WeaponTribulation.abort(p.player.server, trial.id); item.discard();
        } finally { h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(previous, h.getLevel().getServer()); }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void dimensionalTravelPreservesSequenceAndTarget(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); var stack = weapon(0); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p); trial.age = 123; trial.hits = 2;
            var original = p.player.serverLevel(); var destination = p.player.server.getLevel(net.minecraft.world.level.Level.NETHER);
            h.assertTrue(destination != null, "Nether available for dimension travel test");
            p.player.setServerLevel(destination);
            WeaponTribulation.dimensionChanged(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                    p.player, original.dimension(), destination.dimension()));
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(trial.dimension.equals(destination.dimension().location()) && trial.age == 124 && trial.hits == 2,
                    "Player dimension travel moves target without resetting time or consumed strikes");
            p.player.setServerLevel(original); WeaponTribulation.abort(p.player.server, trial.id);
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void restrictionTableStartsTrialAndTransfersOneWeapon(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online();
            var local = new BlockPos(3, 2, 3); var position = h.absolutePos(local);
            h.setBlock(local, com.example.immortal_cultivation_mod.block.ModBlocks.RESTRICTION_TABLE.get());
            var table = (com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity)h.getLevel().getBlockEntity(position);
            var stack = weapon(0); table.setItem(0, stack); table.evolveWeapon(p.player);
            var trial = trial(p); var item = entity(p, trial.id);
            h.assertTrue(table.getItem(0).isEmpty() && WeaponRestrictionSystem.tier(item.getItem()) == 0,
                    "Evolve button transfers table weapon to trial entity without early advancement");
            p.player.setItemInHand(InteractionHand.MAIN_HAND, item.getItem()); item.discard();
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(trial.age == 1 && trial.entity == null && WeaponTribulation.active(p.player.getMainHandItem()),
                    "Picking up and holding table weapon transfers lightning target without restarting trial");
            WeaponTribulation.abort(p.player.server, trial.id);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void nativePickupKeepsStoredWeaponAndHeldRecoveryContinuesTrial(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.player.setHealth(1000);
            var other = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
            p.player.setItemInHand(InteractionHand.MAIN_HAND, other);
            WeaponTribulation.start(p.player, weapon(0));
            var trial = trial(p); var item = entity(p, trial.id);
            trial.age = 37;
            item.setNoPickUpDelay(); item.playerTouch(p.player);
            var stored = p.player.getInventory().getItem(1);
            h.assertTrue(item.isRemoved() && trial.id.equals(WeaponTribulation.token(stored))
                            && p.player.getMainHandItem() == other,
                    "Native pickup places trial weapon in the free inventory slot without replacing held item");
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(p.player.getInventory().getItem(1) == stored && trial.entity == null
                            && trial.age == 38 && trial.hits == 0 && countGroundWeapons(p, trial.id) == 0,
                    "Server tracking retains the actual picked-up stack and elapsed sequence without spawning a duplicate");
            tickToNextStrike(p, trial);
            h.assertTrue(stored.getDamageValue() == (int)Math.ceil(stored.getMaxDamage() * .1)
                            && p.player.getHealth() == 1000 && p.player.getInventory().getItem(1) == stored,
                    "Lightning hits an unheld inventory weapon for ten percent durability and does not hurt its carrier");
            int age = trial.age, hits = trial.hits, damage = stored.getDamageValue();
            p.player.getInventory().setItem(1, ItemStack.EMPTY);
            p.player.setItemInHand(InteractionHand.MAIN_HAND, stored);
            p.player.invulnerableTime = 0;
            tickToNextStrike(p, trial);
            h.assertTrue(trial.age > age && trial.hits == hits + 1 && p.player.getHealth() < 1000
                            && p.player.getMainHandItem() == stored && stored.getDamageValue() == damage,
                    "Holding the recovered weapon resumes player lightning in the same trial without further weapon wear");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void cursorRearrangementRetainsWeaponAndDamagesOnlyDurability(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.player.setHealth(1000);
            var stack = weapon(0); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p);
            p.player.containerMenu = p.player.inventoryMenu;
            p.player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, p.player);
            var carried = p.player.inventoryMenu.getCarried();
            h.assertTrue(p.player.getMainHandItem().isEmpty() && trial.id.equals(WeaponTribulation.token(carried)),
                    "Native inventory click moves the trial weapon onto the player cursor");
            trial.age = 17;
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(p.player.inventoryMenu.getCarried() == carried && trial.age == 18
                            && trial.entity == null && countGroundWeapons(p, trial.id) == 0,
                    "Tracking retains the cursor stack during inventory rearrangement instead of forcing a ground drop");
            tickToNextStrike(p, trial);
            h.assertTrue(p.player.inventoryMenu.getCarried() == carried
                            && carried.getDamageValue() == (int)Math.ceil(carried.getMaxDamage() * .1)
                            && p.player.getHealth() == 1000,
                    "Cursor weapon remains recoverable and bears durability lightning while it is unheld");
            int age = trial.age, hits = trial.hits;
            p.player.inventoryMenu.clicked(37, 0, ClickType.PICKUP, p.player);
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(p.player.inventoryMenu.getCarried().isEmpty()
                            && trial.id.equals(WeaponTribulation.token(p.player.getInventory().getItem(1)))
                            && trial.age == age + 1 && trial.hits == hits && countGroundWeapons(p, trial.id) == 0,
                    "Putting the cursor weapon back in inventory preserves one stack and the same age and strike count");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void cursorWeaponCannotHideInOwnCraftingGrid(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.player.setHealth(1000);
            var stack = weapon(0); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p);
            p.player.containerMenu = p.player.inventoryMenu;
            p.player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, p.player);
            var carried = p.player.inventoryMenu.getCarried();
            h.assertTrue(trial.id.equals(WeaponTribulation.token(carried)),
                    "Native hotbar pickup puts the active weapon on the cursor");
            p.player.inventoryMenu.clicked(1, 0, ClickType.PICKUP, p.player);
            h.assertTrue(p.player.inventoryMenu.getSlot(1).getItem().isEmpty()
                            && p.player.inventoryMenu.getCarried() == carried
                            && trial.id.equals(WeaponTribulation.token(carried)),
                    "Own crafting grid rejects the active weapon while retaining the actual cursor stack and trial UUID");
            trial.age = 40;
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(trial.age == 41 && trial.entity == null
                            && p.player.inventoryMenu.getCarried() == carried && countGroundWeapons(p, trial.id) == 0,
                    "Rejected crafting deposit cannot hide the weapon from server tracking or force a duplicate drop");
            tickToNextStrike(p, trial);
            h.assertTrue(carried.getDamageValue() == (int)Math.ceil(carried.getMaxDamage() * .1)
                            && p.player.getHealth() == 1000,
                    "The protected cursor still receives normal weapon durability lightning");
            int age = trial.age, hits = trial.hits;
            p.player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, p.player);
            WeaponTribulation.tick(p.player.server);
            h.assertTrue(p.player.inventoryMenu.getCarried().isEmpty()
                            && trial.id.equals(WeaponTribulation.token(p.player.getMainHandItem()))
                            && trial.age == age + 1 && trial.hits == hits,
                    "Returning the weapon from cursor to hotbar remains allowed without resetting its trial");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void deathAndLogoutWeaponsCanBeRecoveredWithoutResetOrDuplication(GameTestHelper h) {
        boolean previous = h.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, h.getLevel().getServer());
        try {
            for (boolean death : new boolean[]{true, false}) {
                try (var original = new Player(h); var recovered = new Player(h, original.player.getUUID())) {
                    original.online();
                    var stack = weapon(1); original.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    original.player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                    WeaponTribulation.start(original.player, stack); var trial = trial(original);
                    trial.age = 123; trial.hits = 2;
                    if (death) {
                        original.player.setHealth(0); original.player.die(h.getLevel().damageSources().lightningBolt());
                    } else {
                        WeaponTribulation.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(original.player));
                    }
                    h.assertTrue(original.player.getMainHandItem().isEmpty() && trial.groundOnly
                                    && original.player.getInventory().getItem(1).getCount() == 3
                                    && countGroundWeapons(original, trial.id) == 1,
                            "Death or logout creates exactly one trial drop and preserves unrelated inventory");
                    int age = trial.age, hits = trial.hits, duration = trial.duration;
                    recovered.online();
                    recovered.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
                    var item = entity(original, trial.id); item.setNoPickUpDelay(); item.playerTouch(recovered.player);
                    var stored = recovered.player.getInventory().getItem(1);
                    h.assertTrue(item.isRemoved() && trial.id.equals(WeaponTribulation.token(stored)),
                            "The living owner can pick up the real weapon after death or logout");
                    WeaponTribulation.tick(recovered.player.server);
                    h.assertTrue(!trial.groundOnly && trial.entity == null && trial.age == age + 1
                                    && trial.hits == hits && trial.duration == duration
                                    && recovered.player.getInventory().getItem(1) == stored
                                    && countGroundWeapons(recovered, trial.id) == 0,
                            "Owner recovery clears ground-only custody while preserving sequence and exactly one weapon");
                }
            }
        } finally {
            h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(previous, h.getLevel().getServer());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void successfulPowerfulTrialCreatesOneHeavenlyOpportunity(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.onlineWithLookup();
            var stack = equalStatWeapon(3);
            stack.set(DataComponents.MAX_DAMAGE, 20000);
            h.assertTrue(WeaponTribulation.start(p.player, stack), "Start a powerful unheld weapon's normal trial");
            var sequence = trial(p); var item = entity(p, sequence.id);
            h.assertTrue(sequence.power >= 300 && !sequence.ownerDied
                            && !heavenlyClaimed(h, sequence.id),
                    "A fresh powerful trial has no death marker and no preexisting rare reward opportunity");
            for (int tick = 0; tick < sequence.duration; tick++) WeaponTribulation.tick(p.player.server);
            h.assertTrue(item.isAlive() && WeaponRestrictionSystem.tier(item.getItem()) == 4
                            && !WeaponTribulationData.get(p.player.server).trials.containsKey(sequence.id),
                    "Surviving every real weapon strike advances this powerful weapon normally");
            h.assertTrue(p.player.isAlive() && heavenlyClaimed(h, sequence.id),
                    "Successful trial with a living online owner reaches the one-time Heavenly Flame rare roll");
            item.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void groundTrialOwnerDeathPersistsAndRespawnCannotReward(GameTestHelper h) {
        try (var original = new Player(h); var recovered = new Player(h, original.player.getUUID())) {
            original.onlineWithLookup();
            var stack = equalStatWeapon(3);
            stack.set(DataComponents.MAX_DAMAGE, 20000);
            h.assertTrue(WeaponTribulation.start(original.player, stack), "Start a powerful weapon already on the ground");
            var sequence = trial(original); var item = entity(original, sequence.id);
            tickToNextStrike(original, sequence);
            h.assertTrue(original.player.getMainHandItem().isEmpty() && item.isAlive()
                            && !sequence.ownerDied && countGroundWeapons(original, sequence.id) == 1,
                    "Ground-custody fixture keeps exactly one real weapon while its owner remains outside");
            int age = sequence.age, hits = sequence.hits;
            original.player.setHealth(0);
            original.player.die(h.getLevel().damageSources().lightningBolt());
            h.assertTrue(sequence.ownerDied && item.isAlive() && sequence.age == age && sequence.hits == hits
                            && countGroundWeapons(original, sequence.id) == 1,
                    "Actual owner death marks an owned ground trial even when no weapon can be forcibly dropped");

            var registries = h.getLevel().registryAccess();
            var data = WeaponTribulationData.get(original.player.server);
            var loaded = WeaponTribulationData.load(data.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
            for (int reload = 0; reload < 3; reload++) {
                var restored = loaded.trials.get(sequence.id);
                h.assertTrue(restored.ownerDied && restored.owner.equals(original.player.getUUID())
                                && restored.age == age && restored.hits == hits && restored.entity.equals(item.getUUID()),
                        "Repeated world saves preserve death disqualification, weapon custody and elapsed trial progress");
                loaded = WeaponTribulationData.load(loaded.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
            }
            sequence = loaded.trials.get(sequence.id);
            data.trials.put(sequence.id, sequence);
            recovered.onlineWithLookup();
            recovered.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            item.setNoPickUpDelay(); item.playerTouch(recovered.player);
            var restoredWeapon = recovered.player.getInventory().getItem(1);
            h.assertTrue(item.isRemoved() && sequence.id.equals(WeaponTribulation.token(restoredWeapon)),
                    "Respawned owner recovers the actual surviving weapon into inventory");
            WeaponTribulation.tick(recovered.player.server);
            h.assertTrue(!sequence.groundOnly && sequence.ownerDied && sequence.age == age + 1 && sequence.hits == hits,
                    "Recovery clears ground custody without clearing persisted death disqualification");
            for (int tick = sequence.age; tick < sequence.duration; tick++) WeaponTribulation.tick(recovered.player.server);
            h.assertTrue(recovered.player.isAlive() && WeaponRestrictionSystem.tier(restoredWeapon) == 4
                            && !WeaponTribulationData.get(recovered.player.server).trials.containsKey(sequence.id),
                    "Recovered weapon may survive its remaining strikes and advance while its respawned owner lives");
            h.assertTrue(!heavenlyClaimed(h, sequence.id),
                    "Owner death followed by save, respawn, pickup and eventual success never grants a Heavenly Flame opportunity");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lethalStoredWeaponStrikeShattersItsActualStackOnce(GameTestHelper h) {
        try (var p = new Player(h)) {
            p.online(); p.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.player.setHealth(1000);
            var stack = weapon(1); p.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            WeaponTribulation.start(p.player, stack); var trial = trial(p);
            p.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            p.player.getInventory().setItem(9, stack);
            stack.setDamageValue(stack.getMaxDamage() - 1);
            tickToNextStrike(p, trial);
            h.assertTrue(p.player.getInventory().getItem(9).isEmpty() && p.player.getHealth() == 1000
                            && !WeaponTribulationData.get(p.player.server).trials.containsKey(trial.id)
                            && countGroundWeapons(p, trial.id) == 0,
                    "Fatal durability strike removes the real stored weapon and fails advancement without hurting carrier");
            WeaponTribulation.tick(p.player.server);
            int shards = 0;
            for (var entity : h.getLevel().getAllEntities()) {
                if (entity instanceof ItemEntity drop && drop.isAlive() && WeaponShardSystem.populated(drop.getItem())
                        && WeaponShardSystem.data(drop.getItem()).getInt("tier") == 1 && drop.distanceToSqr(p.player) < 9) {
                    shards++; drop.discard();
                }
            }
            h.assertTrue(shards == 1, "Stored spirit weapon shatters once at its original tier and cannot duplicate shards");
        }
        h.succeed();
    }

    private static ItemStack equalStatWeapon(int rank) {
        var stack = new ItemStack(ModItems.DAO.get());
        WeaponRestrictionSystem.setTier(stack, rank);
        int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
        layers[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()] = 72;
        if (!WeaponRestrictionSystem.addRestrictions(stack, layers)) throw new AssertionError("Cannot prepare equal-stat trial weapon");
        return stack;
    }

    private static boolean heavenlyClaimed(GameTestHelper helper, UUID trialId) {
        try {
            var type = Class.forName("com.example.immortal_cultivation_mod.flame.FlameAcquisitionData");
            var get = type.getDeclaredMethod("get", net.minecraft.server.level.ServerLevel.class);
            get.setAccessible(true);
            var data = (net.minecraft.world.level.saveddata.SavedData) get.invoke(null, helper.getLevel());
            var saved = data.save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
            for (var claim : saved.getList("Claims", net.minecraft.nbt.Tag.TAG_STRING)) {
                if (claim.getAsString().equals("heavenly:" + trialId)) return true;
            }
            return false;
        } catch (ReflectiveOperationException e) { throw new AssertionError("Cannot inspect saved Heavenly Flame opportunities", e); }
    }

    private static net.minecraft.nbt.CompoundTag savedTrial(net.minecraft.nbt.CompoundTag saved, UUID id) {
        for (var entry : saved.getList("trials", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var trial = (net.minecraft.nbt.CompoundTag)entry;
            if (trial.getUUID("id").equals(id)) return trial;
        }
        throw new AssertionError("Missing serialized trial");
    }

    private static void tickToNextStrike(Player player, WeaponTribulationData.Trial trial) {
        int previousHits = trial.hits;
        for (int ticks = 0; ticks < trial.duration && trial.hits == previousHits; ticks++) {
            WeaponTribulation.tick(player.player.server);
        }
        player.helper.assertTrue(trial.hits == previousHits + 1, "Active trial reaches its next lightning strike");
    }

    private static int countGroundWeapons(Player player, UUID id) {
        int count = 0;
        for (var level : player.player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item && item.isAlive() && id.equals(WeaponTribulation.token(item.getItem()))) count++;
            }
        }
        return count;
    }
}
