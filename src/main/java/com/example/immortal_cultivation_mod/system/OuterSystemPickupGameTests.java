package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.OuterSystemItem;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.EnumSet;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_system_pickup")
@PrefixGameTestTemplate(false)
public final class OuterSystemPickupGameTests {
    private OuterSystemPickupGameTests() {
    }

    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;

        Fixture(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "system-pickup"),
                    ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet) {
                }
            };
            BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
            player.setPos(pos.getX(), pos.getY(), pos.getZ());
        }

        ItemEntity drop(ItemStack stack) {
            ItemEntity entity = new ItemEntity(player.level(), player.getX(),
                    player.getY(), player.getZ(), stack);
            entity.setNoPickUpDelay();
            return entity;
        }

        @Override
        public void close() {
            player.discard();
            channel.close();
        }
    }

    private static OuterSystemType expectedRoll(long seed) {
        OuterSystemType[] types = OuterSystemType.values();
        return types[RandomSource.create(seed).nextInt(types.length)];
    }

    @GameTest(template = "fate_test")
    public static void untypedItemsCanRollEverySystemThroughBothActions(GameTestHelper helper) {
        EnumSet<OuterSystemType> picked = EnumSet.noneOf(OuterSystemType.class);
        EnumSet<OuterSystemType> used = EnumSet.noneOf(OuterSystemType.class);
        for (long seed = 0; seed < 32; seed++) {
            for (boolean pickup : new boolean[]{true, false}) {
                try (Fixture fixture = new Fixture(helper)) {
                    fixture.player.getRandom().setSeed(seed);
                    ItemStack stack = new ItemStack(ModItems.SYSTEM.get());
                    if (pickup) {
                        fixture.drop(stack).playerTouch(fixture.player);
                    } else {
                        fixture.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        stack.getItem().use(fixture.player.level(), fixture.player,
                                InteractionHand.MAIN_HAND);
                    }
                    OuterSystemType rolled = OuterSystemData.type(fixture.player);
                    helper.assertTrue(OuterSystemData.equipped(fixture.player).isPresent()
                                    && rolled == expectedRoll(seed)
                                    && OuterSystemData.read(fixture.player)
                                            .getString(OuterSystemData.TYPE).equals(rolled.id()),
                            "Untyped system item rolls once on server and saves chosen type");
                    (pickup ? picked : used).add(rolled);
                }
            }
        }
        helper.assertTrue(picked.equals(EnumSet.allOf(OuterSystemType.class))
                        && used.equals(EnumSet.allOf(OuterSystemType.class)),
                "Both pickup and right-click can grant every available system");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rightClickRollsRandomSystemFromEitherHand(GameTestHelper helper) {
        for (OuterSystemType type : OuterSystemType.values()) {
            for (InteractionHand hand : InteractionHand.values()) {
                try (Fixture fixture = new Fixture(helper)) {
                    ItemStack stack = OuterSystemItem.create(type);
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("used-" + type.id()));
                    ItemStack expected = stack.copy();
                    long seed = 8123L + type.ordinal() * 10L + hand.ordinal();
                    OuterSystemType rolled = expectedRoll(seed);
                    OuterSystemItem.setType(expected, rolled);
                    fixture.player.getRandom().setSeed(seed);
                    fixture.player.setItemInHand(hand, stack);
                    var result = stack.getItem().use(fixture.player.level(), fixture.player, hand);
                    helper.assertTrue(result.getResult().consumesAction()
                                    && fixture.player.getItemInHand(hand).isEmpty(),
                            "Right-click consumes one system from the actual used hand: " + hand);
                    helper.assertTrue(ItemStack.isSameItemSameComponents(
                                    OuterSystemData.equipped(fixture.player).orElseThrow(), expected),
                            "Right-click rolls server-side and preserves other item metadata: " + type);
                    OuterSystemManager.onBodyTypeChanged(fixture.player);
                    helper.assertTrue(OuterSystemData.type(fixture.player) == rolled
                                    && OuterSystemData.equipped(fixture.player).isPresent(),
                            "Right-click binding persists on an ordinary body");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rightClickCannotReplaceExistingSystem(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.drop(OuterSystemItem.create(OuterSystemType.SYSTEM_SHOP))
                    .playerTouch(fixture.player);
            CompoundTag before = OuterSystemData.read(fixture.player);
            OuterSystemType initial = OuterSystemData.type(fixture.player);
            ItemStack spare = OuterSystemItem.create(OuterSystemType.DAILY_SIGN_IN);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, spare);
            var result = spare.getItem().use(fixture.player.level(), fixture.player,
                    InteractionHand.MAIN_HAND);
            helper.assertTrue(!result.getResult().consumesAction() && spare.getCount() == 1
                            && OuterSystemData.type(fixture.player) == initial
                            && before.equals(OuterSystemData.read(fixture.player)),
                    "Using another system preserves current binding and unused item");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void repeatedCreativeUseCannotDuplicateBinding(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.player.getAbilities().instabuild = true;
            ItemStack stack = OuterSystemItem.create(OuterSystemType.DIVINE_GACHA);
            // Even command-created oversized stacks consume exactly one item.
            stack.setCount(2);
            fixture.player.setItemInHand(InteractionHand.OFF_HAND, stack);
            stack.getItem().use(fixture.player.level(), fixture.player, InteractionHand.OFF_HAND);
            OuterSystemType initial = OuterSystemData.type(fixture.player);
            stack.getItem().use(fixture.player.level(), fixture.player, InteractionHand.OFF_HAND);
            helper.assertTrue(stack.getCount() == 1
                            && OuterSystemData.equipped(fixture.player).orElseThrow().getCount() == 1
                            && OuterSystemData.type(fixture.player) == initial,
                    "Repeated right-click cannot consume more items or duplicate an equipped system");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void pickupRollsRandomSystemAndPreservesOtherItemData(GameTestHelper helper) {
        for (OuterSystemType type : OuterSystemType.values()) {
            try (Fixture fixture = new Fixture(helper)) {
                ItemStack stack = OuterSystemItem.create(type);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("picked-" + type.id()));
                long seed = 4827L + type.ordinal();
                OuterSystemType rolled = expectedRoll(seed);
                fixture.player.getRandom().setSeed(seed);
                ItemStack expected = stack.copy();
                OuterSystemItem.setType(expected, rolled);
                ItemEntity item = fixture.drop(stack.copy());
                item.playerTouch(fixture.player);
                ItemStack equipped = OuterSystemData.equipped(fixture.player).orElseThrow();
                helper.assertTrue(ItemStack.isSameItemSameComponents(equipped, expected)
                                && equipped.getCount() == 1,
                        "Pickup rolls a system while preserving other metadata: " + type);
                helper.assertTrue(item.isRemoved()
                                && fixture.player.getInventory().countItem(ModItems.SYSTEM.get()) == 0,
                        "Exactly one picked-up system is consumed without duplication");
                OuterSystemManager.onBodyTypeChanged(fixture.player);
                helper.assertTrue(OuterSystemData.equipped(fixture.player).isPresent()
                                && OuterSystemData.type(fixture.player) == rolled,
                        "Ordinary body does not lose or reroll its picked-up system");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void secondPickupNeverReplacesOrConsumesAnotherSystem(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.drop(OuterSystemItem.create(OuterSystemType.SYSTEM_SHOP))
                    .playerTouch(fixture.player);
            OuterSystemType initial = OuterSystemData.type(fixture.player);
            fixture.drop(OuterSystemItem.create(OuterSystemType.LIFE_SIMULATION))
                    .playerTouch(fixture.player);
            helper.assertTrue(OuterSystemData.type(fixture.player) == initial,
                    "A second pickup in the same tick cannot replace the first system");
            helper.assertTrue(fixture.player.getInventory().countItem(ModItems.SYSTEM.get()) == 1,
                    "Additional systems remain normal inventory items");
            ItemStack original = OuterSystemData.equipped(fixture.player).orElseThrow();
            helper.assertTrue(OuterSystemItem.getType(original) == initial,
                    "Existing slot contains its original system");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void invalidPickupsCannotGrantSystems(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            ItemEntity delayed = fixture.drop(OuterSystemItem.create(OuterSystemType.DIVINE_GACHA));
            delayed.setPickUpDelay(20);
            delayed.playerTouch(fixture.player);
            ItemEntity owned = fixture.drop(OuterSystemItem.create(OuterSystemType.DAILY_SIGN_IN));
            owned.setTarget(UUID.randomUUID());
            owned.playerTouch(fixture.player);
            for (int slot = 0; slot < fixture.player.getInventory().items.size(); slot++) {
                fixture.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
            ItemEntity full = fixture.drop(OuterSystemItem.create(OuterSystemType.KILL_GROWTH));
            full.playerTouch(fixture.player);
            helper.assertTrue(OuterSystemData.equipped(fixture.player).isEmpty()
                            && !OuterSystemData.read(fixture.player).getBoolean(OuterSystemData.PICKUP_BOUND),
                    "Delay, ownership and full inventory cannot grant an uncollected system");
            helper.assertTrue(!delayed.isRemoved() && !owned.isRemoved() && !full.isRemoved()
                            && delayed.getItem().getCount() == 1 && owned.getItem().getCount() == 1
                            && full.getItem().getCount() == 1,
                    "Failed pickup keeps each original item intact");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void pickupBindingSurvivesCloneAndDestroyedSystemNeedsNewPickup(GameTestHelper helper) {
        try (Fixture original = new Fixture(helper); Fixture replacement = new Fixture(helper)) {
            original.drop(OuterSystemItem.create(OuterSystemType.LIFE_SIMULATION))
                    .playerTouch(original.player);
            OuterSystemType initial = OuterSystemData.type(original.player);
            OuterSystemData.copy(original.player, replacement.player);
            OuterSystemManager.onBodyTypeChanged(replacement.player);
            helper.assertTrue(OuterSystemData.equipped(replacement.player).isPresent()
                            && OuterSystemData.type(replacement.player) == initial,
                    "Saved pickup binding restores on respawn without special body");
            CompoundTag state = OuterSystemData.read(replacement.player);
            state.putBoolean(OuterSystemData.DESTROYED, true);
            OuterSystemData.save(replacement.player, state);
            OuterSystemManager.onBodyTypeChanged(replacement.player);
            OuterSystemManager.onBodyTypeChanged(replacement.player);
            helper.assertTrue(OuterSystemData.equipped(replacement.player).isEmpty(),
                    "Destroyed system is not regenerated by binding maintenance");
            replacement.drop(OuterSystemItem.create(OuterSystemType.DIVINE_GACHA))
                    .playerTouch(replacement.player);
            OuterSystemType renewed = OuterSystemData.type(replacement.player);
            OuterSystemManager.onBodyTypeChanged(replacement.player);
            helper.assertTrue(OuterSystemData.type(replacement.player) == renewed
                            && OuterSystemData.equipped(replacement.player).isPresent()
                            && !OuterSystemData.read(replacement.player).getBoolean(OuterSystemData.DESTROYED),
                    "A new valid pickup can bind a replacement system");
        }
        helper.succeed();
    }

    private static void bindWithProgress(ServerPlayer player, OuterSystemType type) {
        OuterSystemData.ensureSlot(player);
        if (!OuterSystemData.equip(player, OuterSystemItem.create(type))) throw new AssertionError("System fixture equip failed");
        CompoundTag state = new CompoundTag();
        state.putBoolean(OuterSystemData.PICKUP_BOUND, true);
        state.putBoolean(OuterSystemData.EVER_BOUND, true);
        state.putString(OuterSystemData.TYPE, type.id());
        state.putLong("Points", 12345);
        CompoundTag quest = new CompoundTag();
        quest.putBoolean("Active", true);
        state.put(OuterSystemData.QUEST, quest);
        OuterSystemData.save(player, state);
    }

    @GameTest(template = "fate_test")
    public static void naturalAgeExhaustionRemovesEverySystemAndItsProgress(GameTestHelper helper) {
        for (OuterSystemType type : OuterSystemType.values()) {
            try (Fixture fixture = new Fixture(helper)) {
                var player = fixture.player;
                bindWithProgress(player, type);
                var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player);
                int age = com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge();
                com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, data.withAgePenalty(age));
                com.example.immortal_cultivation_mod.event.ServerEvents.onPlayerTick(
                        new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
                helper.assertTrue(OuterSystemData.equipped(player).isEmpty() && OuterSystemData.read(player).isEmpty(),
                        "Age exhaustion removes equipped system, binding, currency and quest: " + type);
                OuterSystemManager.onBodyTypeChanged(player);
                helper.assertTrue(OuterSystemData.equipped(player).isEmpty(), "Expired system cannot auto-rebind");
                helper.assertTrue(OuterSystemManager.bindFromItem(player, new ItemStack(ModItems.SYSTEM.get())),
                        "New life can acquire a fresh system item");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void onlyFinalAgeDeathLosesSystemAndCloneCannotRestoreIt(GameTestHelper helper) {
        try (Fixture original = new Fixture(helper); Fixture replacement = new Fixture(helper)) {
            var player = original.player;
            bindWithProgress(player, OuterSystemType.LIFE_SIMULATION);
            CompoundTag before = OuterSystemData.read(player);
            var death = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(player, player.damageSources().magic());
            com.example.immortal_cultivation_mod.event.ServerEvents.onPlayerDeath(death);
            helper.assertTrue(OuterSystemData.equipped(player).isPresent() && OuterSystemData.read(player).equals(before),
                    "Ordinary death with lifespan remaining keeps the system and progress");
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player);
            int age = com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge();
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, data.withAgePenalty(age - 10));
            com.example.immortal_cultivation_mod.event.ServerEvents.onPlayerDeath(death);
            helper.assertTrue(OuterSystemData.equipped(player).isEmpty() && OuterSystemData.read(player).isEmpty(),
                    "Final ten-year death penalty removes system");
            OuterSystemManager.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(replacement.player, player, true));
            OuterSystemManager.onBodyTypeChanged(replacement.player);
            helper.assertTrue(OuterSystemData.equipped(replacement.player).isEmpty() && OuterSystemData.read(replacement.player).isEmpty(),
                    "Respawn copy cannot restore expired system");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void expiredLivingCorpseCannotRegenerateBodySystem(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            bindWithProgress(player, OuterSystemType.DIVINE_GACHA);
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player)
                    .withBodyType(com.example.immortal_cultivation_mod.attachment.BodyTypes.OUTER_DEMON_BODY);
            int age = com.example.immortal_cultivation_mod.attachment.CultivationLevels.getLevelDef(data.cultivationLevel()).maxAge();
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, data.withAgePenalty(age));
            player.getPersistentData().putBoolean(com.example.immortal_cultivation_mod.item.CultivationAccessoryItem.LIVING_CORPSE_TAG, true);
            com.example.immortal_cultivation_mod.event.ServerEvents.onPlayerTick(
                    new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            OuterSystemManager.onBodyTypeChanged(player);
            helper.assertTrue(OuterSystemData.equipped(player).isEmpty()
                            && OuterSystemData.read(player).getBoolean(OuterSystemData.DESTROYED)
                            && !OuterSystemData.read(player).contains(OuterSystemData.TYPE),
                    "Retained Outer Demon body does not grant another system after lifespan exhaustion");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lifespanResetClosesSystemAndRemovesManifest(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            bindWithProgress(player, OuterSystemType.SYSTEM_SHOP);
            OuterSystemManager.openScreen(player);
            helper.assertTrue(OuterSystemManager.isScreenOpen(player), "System screen opened before reset");
            com.example.immortal_cultivation_mod.event.ServerEvents.resetPlayer(player);
            helper.assertTrue(!OuterSystemManager.isScreenOpen(player) && OuterSystemData.equipped(player).isEmpty(),
                    "Reset removes open system and slot item");
            for (var entity : helper.getLevel().getAllEntities()) {
                helper.assertTrue(!(entity instanceof com.example.immortal_cultivation_mod.entity.SystemManifestEntity manifest)
                                || !manifest.ownerId().filter(player.getUUID()::equals).isPresent() || manifest.isRemoved(),
                        "Reset removes this player's system manifest");
            }
        }
        helper.succeed();
    }
    @GameTest(template = "fate_test")
    public static void debugChangesItemBoundSystemsAndPreservesTheirMetadataAndProgress(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper); Fixture restored = new Fixture(helper)) {
            var player = fixture.player;
            bindWithProgress(player, OuterSystemType.SYSTEM_SHOP);
            ItemStack original = OuterSystemData.equipped(player).orElseThrow();
            original.set(DataComponents.CUSTOM_NAME, Component.literal("debug-bound-system"));
            CompoundTag custom = original.get(DataComponents.CUSTOM_DATA).copyTag();
            custom.putString("DebugMetadataTest", "preserve");
            original.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(custom));
            ItemStack spare = OuterSystemItem.create(OuterSystemType.KILL_GROWTH).copyWithCount(3);
            player.setItemInHand(InteractionHand.MAIN_HAND, spare);
            OuterSystemManager.openScreen(player);
            for (OuterSystemType type : OuterSystemType.values()) {
                ItemStack expected = original.copy();
                OuterSystemItem.setType(expected, type);
                OuterSystemManager.setDebugSystem(player, type);
                CompoundTag state = OuterSystemData.read(player);
                helper.assertTrue(OuterSystemData.type(player) == type
                                && OuterSystemManager.debugSystemType(player) == type
                                && state.getString(OuterSystemData.TYPE).equals(type.id()),
                        "Debug selection updates both equipped and stored types on an ordinary body: " + type);
                helper.assertTrue(ItemStack.isSameItemSameComponents(
                                OuterSystemData.equipped(player).orElseThrow(), expected)
                                && state.getLong("Points") == 12345 && state.getBoolean(OuterSystemData.PICKUP_BOUND),
                        "Type changes preserve custom item data, progress and item binding");
                helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3
                                && !OuterSystemManager.isScreenOpen(player),
                        "Changing type neither consumes spare items nor leaves the previous system screen active");
                OuterSystemManager.onBodyTypeChanged(player);
                helper.assertTrue(OuterSystemData.type(player) == type && OuterSystemData.equipped(player).isPresent(),
                        "Binding reconciliation cannot restore the old equipped type");
            }
            CompoundTag saved = new CompoundTag();
            player.saveWithoutId(saved);
            restored.player.load(saved);
            OuterSystemManager.onBodyTypeChanged(restored.player);
            helper.assertTrue(OuterSystemData.type(restored.player) == OuterSystemType.LIFE_SIMULATION
                            && OuterSystemData.read(restored.player).getLong("Points") == 12345,
                    "Selected type and system progress survive player save/load");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void debugPresetStillWaitsForBodyAndChangesBodyGrantedSystems(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            OuterSystemManager.setDebugSystem(player, OuterSystemType.LIFE_SIMULATION);
            helper.assertTrue(OuterSystemData.equipped(player).isEmpty()
                            && OuterSystemManager.debugSystemType(player) == OuterSystemType.LIFE_SIMULATION,
                    "An unbound ordinary player retains a preset without being granted an unsolicited system");
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player,
                    data.withBodyType(com.example.immortal_cultivation_mod.attachment.BodyTypes.OUTER_DEMON_BODY));
            OuterSystemManager.onBodyTypeChanged(player);
            helper.assertTrue(OuterSystemData.equipped(player).isPresent()
                            && OuterSystemData.type(player) == OuterSystemType.LIFE_SIMULATION,
                    "Outer Demon Body still grants the selected debug preset");
            for (OuterSystemType type : OuterSystemType.values()) {
                OuterSystemManager.setDebugSystem(player, type);
                OuterSystemManager.onBodyTypeChanged(player);
                helper.assertTrue(OuterSystemData.type(player) == type
                                && !OuterSystemData.read(player).getBoolean(OuterSystemData.PICKUP_BOUND),
                        "Body-granted system changes type without turning into a permanent item binding");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void debugUpdatesTemporarilyUnequippedItemBinding(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            bindWithProgress(fixture.player, OuterSystemType.DAILY_SIGN_IN);
            fixture.player.getAbilities().instabuild = true;
            OuterSystemData.removeEquipped(fixture.player);
            OuterSystemManager.setDebugSystem(fixture.player, OuterSystemType.DIVINE_GACHA);
            helper.assertTrue(OuterSystemData.equipped(fixture.player).isPresent()
                            && OuterSystemData.type(fixture.player) == OuterSystemType.DIVINE_GACHA
                            && OuterSystemData.read(fixture.player).getLong("Points") == 12345,
                    "Existing item binding accepts a debug type change even while its creative slot is empty");
        }
        helper.succeed();
    }


    @GameTest(template = "fate_test")
    public static void travelRewardsAreGenerousStableAndNeverBread(GameTestHelper helper) throws Exception {
        var stones = com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get();
        var medium = com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get();
        var high = com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get();
        String realm = com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL;
        for (int tier = 1; tier <= 5; tier++) for (long seed = 0; seed < 128; seed++) {
            var rewards = OuterSystemManager.rewards(realm, tier, seed);
            var repeat = OuterSystemManager.rewards(realm, tier, seed);
            helper.assertTrue(rewards.size() >= 3, "Every travel tier has a guaranteed cultivation bundle");
            for (int i = 0; i < rewards.size(); i++) {
                var stack = rewards.get(i);
                helper.assertTrue(!stack.is(Items.BREAD) && stack.getCount() <= stack.getMaxStackSize(), "No bread consolation or oversized stacks");
                helper.assertTrue(ItemStack.matches(stack, repeat.get(i)), "Same saved seed and realm give identical previews/claims");
            }
            var currency = rewards.get(0);
            helper.assertTrue(currency.is(tier < 3 ? stones : tier < 5 ? medium : high), "Longer routes guarantee higher spirit-stone tiers");
            helper.assertTrue(currency.getCount() >= (tier == 1 ? 16 : tier == 2 ? 48 : tier == 4 ? 24 : 8), "Guaranteed currency minimum");
        }
        var method = OuterSystemManager.class.getDeclaredMethod("tierForDistance", int.class);
        method.setAccessible(true);
        int[] distances = {0, 999, 1000, 2499, 2500, 4999, 5000, 7999, 8000, 9999};
        int[] expected = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int i = 0; i < distances.length; i++) helper.assertTrue((int) method.invoke(null, distances[i]) == expected[i], "Reward distance thresholds");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void signInClaimMatchesPreviewAndCannotClaimTwice(GameTestHelper helper) throws Exception {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            bindWithProgress(player, OuterSystemType.DAILY_SIGN_IN);
            var state = OuterSystemData.read(player);
            var quest = state.getCompound(OuterSystemData.QUEST);
            quest.putBoolean("Completed", true);
            quest.putInt("Tier", 5);
            quest.putLong("Seed", 17);
            quest.putString("RewardRealm", com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL);
            OuterSystemData.save(player, state);
            var create = OuterSystemManager.class.getDeclaredMethod("createClientState", ServerPlayer.class, boolean.class);
            create.setAccessible(true);
            String preview = ((CompoundTag) create.invoke(null, player, false)).getString("QuestRewardSummary");
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player,
                    data.withCultivationLevel(com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_DUJIE));
            helper.assertTrue(preview.equals(((CompoundTag) create.invoke(null, player, false)).getString("QuestRewardSummary")), "Realm changes cannot alter promised reward eligibility");
            var expected = OuterSystemManager.rewards(quest.getString("RewardRealm"), 5, 17);
            OuterSystemManager.claimQuest(player);
            for (var reward : expected) helper.assertTrue(player.getInventory().countItem(reward.getItem()) >= reward.getCount(), "Claim delivers advertised rewards");
            int count = player.getInventory().countItem(expected.get(0).getItem());
            OuterSystemManager.claimQuest(player);
            helper.assertTrue(count == player.getInventory().countItem(expected.get(0).getItem()), "Completed claim cannot repeat");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void selfExplosionRequiresThreeTimedDistinctConfirmations(GameTestHelper helper) {
        var sequence = new com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.Sequence();
        helper.assertTrue(sequence.press(100) == 2 && sequence.press(100) == 2, "First warns twice remaining; same-tick repeats never advance");
        helper.assertTrue(sequence.press(102) == 1 && sequence.press(104) == 0, "Only third distinct input permits detonation");
        helper.assertTrue(sequence.press(106) == 2, "Successful confirmation is consumed");
        helper.assertTrue(sequence.press(207) == 2 && sequence.press(209) == 1, "Five-second timeout restarts from first press");
        helper.assertTrue(sequence.press(1) == 2, "Clock reset cannot reuse confirmation");
        helper.assertTrue(com.example.immortal_cultivation_mod.spell.ModSpells.requiresPreparation("zibao"), "Self-explosion must be prepared for R; wheel release cannot cast it");
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            helper.assertTrue(!com.example.immortal_cultivation_mod.combat.CombatStateManager.finishSpellPreparation(player, "zibao", 1), "Unprepared packet cannot confirm");
            com.example.immortal_cultivation_mod.combat.CombatStateManager.beginSpellPreparation(player, "zibao");
            helper.assertTrue(com.example.immortal_cultivation_mod.combat.CombatStateManager.finishSpellPreparation(player, "zibao", 1), "Matching R preparation is consumed");
            helper.assertTrue(!com.example.immortal_cultivation_mod.combat.CombatStateManager.finishSpellPreparation(player, "zibao", 1), "Cannot reuse one press");
            helper.assertTrue(!com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.confirm(player) && player.isAlive(), "First confirmation never harms player");
            com.example.immortal_cultivation_mod.spell.SelfExplosionConfirmation.cancel(player);
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void partiallyFullInventoryDropsOnlyRewardRemainder(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            bindWithProgress(player, OuterSystemType.DAILY_SIGN_IN);
            var state = OuterSystemData.read(player);
            var quest = state.getCompound(OuterSystemData.QUEST);
            quest.putBoolean("Completed", true);quest.putInt("Tier", 1);quest.putLong("Seed", 5);
            String realm = com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_MORTAL;
            quest.putString("RewardRealm", realm);OuterSystemData.save(player, state);
            var currency = OuterSystemManager.rewards(realm, 1, 5).get(0);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(0, currency.copyWithCount(currency.getMaxStackSize() - 1));
            OuterSystemManager.claimQuest(player);
            int dropped = player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2)).stream()
                    .filter(entity -> entity.getItem().is(currency.getItem())).mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(dropped == currency.getCount() - 1, "Only uninserted items drop, never a duplicate full reward stack");
        }
        helper.succeed();
    }
}
