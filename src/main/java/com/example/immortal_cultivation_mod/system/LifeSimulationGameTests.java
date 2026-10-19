package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.item.OuterSystemItem;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** Exercises the same public actions and persistent state used by the simulation UI. */
@GameTestHolder("immortal_cultivation_life_simulation")
@PrefixGameTestTemplate(false)
public final class LifeSimulationGameTests {
    private static final String LIFE = "LifeSimulation";

    private LifeSimulationGameTests() {
    }

    static final class Player implements AutoCloseable {
        final ServerPlayer player;
        private final EmbeddedChannel channel;
        final java.util.List<CompoundTag> screenResponses = new java.util.ArrayList<>();

        Player(GameTestHelper helper) {
            this(helper, UUID.randomUUID());
        }

        Player(GameTestHelper helper, UUID identity) {
            player = new ServerPlayer(
                    helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(identity, "life-settlement"),
                    ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(
                    player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet) {
                    if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                            && custom.payload() instanceof com.example.immortal_cultivation_mod.network.OuterSystemPayloads.ClientboundOpen response) {
                        screenResponses.add(response.state().copy());
                    }
                }
            };
            BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
            player.setPos(position.getX(), position.getY(), position.getZ());
            OuterSystemData.ensureSlot(player);
        }

        void equip(GameTestHelper helper, OuterSystemType type) {
            unequip(helper);
            helper.assertTrue(OuterSystemData.equip(player, OuterSystemItem.create(type)),
                    "Test system is equipped through its real Curios slot");
            CompoundTag binding = OuterSystemData.read(player);
            binding.putBoolean(OuterSystemData.PICKUP_BOUND, true);
            binding.putBoolean(OuterSystemData.EVER_BOUND, true);
            binding.putBoolean(OuterSystemData.BOOT_SHOWN, true);
            binding.putString(OuterSystemData.TYPE, type.id());
            OuterSystemData.save(player, binding);
            helper.assertTrue(OuterSystemItem.getType(CuriosApi.getCuriosInventory(player)
                            .orElseThrow().getStacksHandler(OuterSystemItem.SLOT).orElseThrow()
                            .getStacks().getStackInSlot(0)) == type,
                    "Equipped system determines the active system type");
        }

        void unequip(GameTestHelper helper) {
            OuterSystemData.removeEquipped(player);
            // Curios item searches cache results for one world tick. Inspect the real slot here.
            var stacks = CuriosApi.getCuriosInventory(player).orElseThrow()
                    .getStacksHandler(OuterSystemItem.SLOT).orElseThrow().getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                helper.assertTrue(stacks.getStackInSlot(slot).isEmpty(),
                        "Fixture actually removed the system before configuring its equipment");
            }
        }

        CompoundTag start(GameTestHelper helper) {
            equip(helper, OuterSystemType.LIFE_SIMULATION);
            LifeSimulationSystem.setAttempts(player, 3);
            LifeSimulationSystem.handleAction(player, "start");
            CompoundTag life = read(player);
            helper.assertTrue(life.getBoolean("Active") && life.getBoolean("Alive")
                            && life.hasUUID("SimulationId") && life.getInt("Attempts") == 2,
                    "A normal start creates a generated living simulation and consumes one attempt");
            return life;
        }

        @Override
        public void close() {
            OuterSystemManager.closeScreen(player);
            player.discard();
            channel.close();
        }
    }

    @GameTest(template = "fate_test")
    public static void highRealmLifeSettlesAtCurrentAgeAndStats(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            int realm = CultivationLevels.allLevels().size() - 1;
            int lifespan = CultivationLevels.getLevelDef(
                    CultivationLevels.allLevels().get(realm)).maxAge();
            helper.assertTrue(lifespan > 100, "High cultivation realm has a long lifespan");
            life.putInt("Age", 37);
            life.putInt("Realm", realm);
            life.putInt("HighestRealm", realm);
            life.putInt("MaximumLifespan", lifespan);
            life.putInt("Health", 91);
            life.putInt("Mentality", 84);
            life.putLong("Wealth", 12_000L);
            life.putInt("Reputation", 40);
            life.putInt("Morality", 25);
            life.putLong("CultivationProgress", 1_234L);
            life.putInt("AnnualActionPoints", 2);
            life.putInt("Insight", 40);
            write(fixture.player, life);

            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag settled = read(fixture.player);
            helper.assertTrue(settled.getBoolean("Finished") && !settled.getBoolean("Alive")
                            && settled.getBoolean("Active") && !settled.getBoolean("RewardClaimed"),
                    "Manual settlement ends the living simulation and opens inheritance selection");
            for (String key : new String[]{"Age", "Realm", "HighestRealm", "MaximumLifespan",
                    "Health", "Mentality", "Wealth", "Reputation", "Morality",
                    "CultivationProgress", "AnnualActionPoints", "Attempts", "SimulationId"}) {
                helper.assertTrue(life.get(key).equals(settled.get(key)),
                        "Settlement preserves current simulation value: " + key);
            }
            helper.assertTrue(settled.getLong("ScoreRealm") == realm * 1_800L && settled.getLong("ScoreReputation") == 800L,
                    "Score rewards attained realm and reputation");
            CompoundTag idle = settled.copy(); idle.putInt("Age", 10_000); idle.putLong("Wealth", 99_000_000L);
            helper.assertTrue(LifeSimulationBalance.score(idle) == settled.getLong("Score"),
                    "Elapsed years and idle wealth cannot inflate settlement score");
            helper.assertTrue(settled.getInt("InsightEarned") >= 8
                            && settled.getInt("InsightEarned") <= 400
                            && settled.getInt("Insight") == 40 + settled.getInt("InsightEarned"),
                    "Existing bounded insight reward is granted once at settlement");
            helper.assertTrue(settled.getList("InheritanceRewards", Tag.TAG_COMPOUND).size() == 3
                            && !settled.getList("EndingCollection", Tag.TAG_STRING).isEmpty(),
                    "Existing inheritance options and ending collection are generated immediately");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void pendingChoicesAndTravelCannotBlockSettlementOrConsumeRescue(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Age", 22);
            life.putInt("Health", 2);
            life.putInt("Mentality", 7);
            ListTag upgrades = new ListTag();
            upgrades.add(StringTag.valueOf("fate_not_end"));
            life.put("Upgrades", upgrades);
            ListTag flags = life.getList("Flags", Tag.TAG_STRING).copy();
            flags.add(StringTag.valueOf("root_tested"));
            life.put("Flags", flags);
            life.put("CurrentEvent", pendingEvent());
            CompoundTag map = life.getCompound(LifeSimulationWorldMap.ROOT);
            CompoundTag journey = new CompoundTag();
            journey.putString("Origin", map.getString(LifeSimulationWorldMap.CURRENT_LOCATION));
            journey.putString("Destination", "futian_academy");
            journey.putInt("RemainingSegments", 2);
            map.put(LifeSimulationWorldMap.ACTIVE_JOURNEY, journey);
            map.putBoolean(LifeSimulationWorldMap.PENDING_ARRIVAL, true);
            life.put(LifeSimulationWorldMap.ROOT, map);
            write(fixture.player, life);

            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag settled = read(fixture.player);
            helper.assertTrue(settled.getBoolean("Finished") && !settled.getBoolean("Alive"),
                    "Unresolved event and travel choices cannot prevent manual settlement");
            helper.assertTrue(!settled.contains("CurrentEvent")
                            && !LifeSimulationWorldMap.hasActiveJourney(settled)
                            && !LifeSimulationWorldMap.hasPendingArrival(settled),
                    "Settlement clears pending event, journey and arrival state");
            helper.assertTrue(settled.getList("Flags", Tag.TAG_STRING).equals(flags)
                            && settled.getInt("Health") == 2 && settled.getInt("Mentality") == 7,
                    "Fate Not End cannot resurrect a voluntarily settled life or consume its rescue flag");
            helper.assertTrue(settled.getInt("Age") == 22 && settled.getInt("Attempts") == 2
                            && settled.getList("InheritanceRewards", Tag.TAG_COMPOUND).size() == 3,
                    "Blocked-state settlement keeps current age and attempts and produces inheritance");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void settledLifeRejectsStaleActionsAndInheritanceSurvivesSaveOnce(GameTestHelper helper) {
        try (Player fixture = new Player(helper); Player restored = new Player(helper)) {
            fixture.start(helper);
            ownSimulatedStones(fixture.player, 18);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag settled = read(fixture.player);
            sendStaleActions(fixture.player);
            helper.assertTrue(read(fixture.player).equals(settled),
                    "Repeated settlement and stale event, map and year packets leave settled data unchanged");

            Item stone = com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get();
            int before = inventoryCount(fixture.player, stone);
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            int received = inventoryCount(fixture.player, stone);
            helper.assertTrue(received == before + 18, "Normal material inheritance is delivered once");
            CompoundTag claimed = read(fixture.player);
            helper.assertTrue(claimed.getBoolean("RewardClaimed") && !claimed.getBoolean("Active")
                            && claimed.getInt("Attempts") == 2,
                    "Inheritance completes the life without refunding or consuming attempts");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            sendStaleActions(fixture.player);
            helper.assertTrue(inventoryCount(fixture.player, stone) == received
                            && read(fixture.player).equals(claimed),
                    "Duplicate inheritance and stale actions cannot grant rewards or mutate final data");

            CompoundTag saved = new CompoundTag();
            fixture.player.saveWithoutId(saved);
            restored.player.load(saved);
            helper.assertTrue(OuterSystemData.equipped(restored.player).isPresent()
                            && OuterSystemData.type(restored.player) == OuterSystemType.LIFE_SIMULATION,
                    "Player save/load retains the equipped simulation system");
            helper.assertTrue(read(restored.player).equals(claimed)
                            && inventoryCount(restored.player, stone) == received,
                    "Player save/load preserves settled state, claimed flag and material reward");
            LifeSimulationSystem.handleAction(restored.player, "inherit:materials");
            sendStaleActions(restored.player);
            helper.assertTrue(read(restored.player).equals(claimed)
                            && inventoryCount(restored.player, stone) == received,
                    "Reloaded claimed life cannot settle or award inheritance again");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void settlementRequiresEquippedSystemAndCurrentLivingLife(GameTestHelper helper) {
        try (Player fixture = new Player(helper);
                Player unequipped = new Player(helper);
                Player otherSystem = new Player(helper)) {
            CompoundTag generated = fixture.start(helper);
            // Separate controls match normal equipment snapshots and avoid same-tick Curios search caches.
            write(unequipped.player, generated);
            LifeSimulationSystem.handleAction(unequipped.player, "settle");
            helper.assertTrue(read(unequipped.player).equals(generated),
                    "Saved simulation data alone cannot settle without an equipped system");
            otherSystem.equip(helper, OuterSystemType.DAILY_SIGN_IN);
            write(otherSystem.player, generated);
            LifeSimulationSystem.handleAction(otherSystem.player, "settle");
            helper.assertTrue(read(otherSystem.player).equals(generated),
                    "Another equipped system cannot settle a saved simulation");
            for (String invalidState : new String[]{"inactive", "dead", "finished", "ungenerated", "missing"}) {
                CompoundTag life = generated.copy();
                switch (invalidState) {
                    case "inactive" -> life.putBoolean("Active", false);
                    case "dead" -> life.putBoolean("Alive", false);
                    case "finished" -> life.putBoolean("Finished", true);
                    case "ungenerated" -> life.remove("Name");
                    case "missing" -> life = new CompoundTag();
                    default -> throw new IllegalStateException(invalidState);
                }
                write(fixture.player, life);
                if (invalidState.equals("finished")) {
                    LifeSimulationSystem.writeClientState(fixture.player, new CompoundTag());
                    life = read(fixture.player);
                }
                LifeSimulationSystem.handleAction(fixture.player, "settle");
                helper.assertTrue(read(fixture.player).equals(life),
                        "Settlement ignores nonliving or absent simulation: " + invalidState);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void memoriesAndActualInheritanceSurviveNewLivesAndSave(GameTestHelper helper) {
        try (Player fixture = new Player(helper); Player restored = new Player(helper)) {
            fixture.start(helper);
            java.util.List<java.util.UUID> ids = new java.util.ArrayList<>();
            for (int n = 0; n < 7; n++) {
                CompoundTag life = read(fixture.player);
                life.putInt("Age", n + 20);
                life.put("Inventory", new ListTag());
                LifeSimulationSystem.addSimulationItem(life, "immortal_cultivation_mod:spirit_stone", 18);
                write(fixture.player, life);
                ids.add(life.getUUID("SimulationId"));
                LifeSimulationSystem.handleAction(fixture.player, "settle");
                CompoundTag client = new CompoundTag();
                LifeSimulationSystem.writeClientState(fixture.player, client);
                CompoundTag preview = client.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND)
                        .getCompound(2).getCompound("Preview").copy();
                LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
                helper.assertTrue(read(fixture.player).getCompound("InheritanceReceipt").equals(preview),
                        "Claim records exactly the inheritance that was previewed");
                LifeSimulationSystem.setAttempts(fixture.player, 3);
                LifeSimulationSystem.handleAction(fixture.player, "start");
            }
            ListTag memories = read(fixture.player).getList("Memories", Tag.TAG_COMPOUND);
            helper.assertTrue(memories.size() == 5, "Archive is bounded to the last five past lives");
            for (int i = 0; i < 5; i++) {
                CompoundTag memory = memories.getCompound(i);
                helper.assertTrue(memory.getUUID("SimulationId").equals(ids.get(i + 2))
                                && memory.getInt("Age") == i + 22
                                && !memory.getList("History", Tag.TAG_COMPOUND).isEmpty()
                                && memory.getCompound("InheritanceReceipt").getInt("Count") == 18,
                        "Archive retains each life identity, age, memories and actual inheritance");
                helper.assertTrue(!memory.contains("Memories"), "Archives do not recursively embed previous archives");
            }
            CompoundTag saved = new CompoundTag();
            fixture.player.saveWithoutId(saved);
            restored.player.load(saved);
            helper.assertTrue(read(restored.player).getList("Memories", Tag.TAG_COMPOUND).equals(memories),
                    "Past-life memories and inheritance survive player save/load");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void clientReceivesMemoriesAndOnlySimulatedOwnedItems(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            ListTag history = new ListTag();
            for (int i = 0; i < 200; i++) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Age", i);
                entry.putString("Key", "life.immortal_cultivation_mod.log.manual_settlement");
                history.add(entry);
            }
            life.put("History", history);
            life.put("Inventory", new ListTag());
            LifeSimulationSystem.addSimulationItem(life, "minecraft:bread", 3);
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag client = new CompoundTag();
            LifeSimulationSystem.writeClientState(fixture.player, client);
            helper.assertTrue(client.getCompound(LIFE).getList("History", Tag.TAG_COMPOUND).size() >= 200,
                    "Client retains earlier memories");
            CompoundTag reward = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(2);
            for (String level : CultivationLevels.allLevels()) {
                var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
                com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, data.withCultivationLevel(level));
                CompoundTag preview = LifeSimulationSystem.inheritancePreview(fixture.player, reward);
                helper.assertTrue(preview.getString("Item").equals("minecraft:bread") && preview.getInt("Count") == 3,
                        "Host realm never upgrades owned bread into invented spirit stones: " + level);
            }
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            helper.assertTrue(inventoryCount(fixture.player, net.minecraft.world.item.Items.BREAD) == 3,
                    "Claim transfers exactly the simulated owned quantity");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void progressPreviewCrossesStageCapsWithoutInventingFallback(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Realm", 3); life.putInt("HighestRealm", 3); life.putString("SpiritualRoot", "single");
            life.putLong("CultivationProgress", 500); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag reward = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(0);
            reward.putInt("Percent", 8);
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player)
                    .withCultivationLevel(CultivationLevels.allLevels().get(4));
            long required = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, data.withCultivationProgress(required - 1));
            helper.assertTrue(LifeSimulationSystem.inheritancePreview(fixture.player, reward).getLong("Gain") == 72,
                    "All 72 points survive a nearly-full recipient stage");
            data = data.withCultivationLevel(CultivationLevels.allLevels().getLast());
            required = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, data.withCultivationProgress(required));
            helper.assertTrue(LifeSimulationSystem.inheritancePreview(fixture.player, reward).getString("Kind").equals("none"),
                    "Only the absolute final-stage cap blocks inheritance, without fabricating fallback items");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            helper.assertTrue(!read(fixture.player).getBoolean("RewardClaimed") && !read(fixture.player).contains("PendingInheritance"),
                    "An unavailable progress choice cannot lock settlement or issue invented rewards");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void spellInheritanceRequiresActuallyLearnedSpellAndKeepsProficiency(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            fixture.start(helper);
            String spellId = com.example.immortal_cultivation_mod.spell.ModSpells.FIREBALL;
            var initial = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, initial.withKnownSpells(java.util.List.of()));
            for (String kind : new String[]{"spell", "proficiency", "none"}) {
                CompoundTag life = read(fixture.player);
                life.putInt("Realm", 1); life.putInt("HighestRealm", 1); life.putString("SpiritualRoot", "single");
                LifeSimulationSystem.applyEffect(life, "spell:" + spellId); write(fixture.player, life);
                LifeSimulationSystem.handleAction(fixture.player, "settle");
                CompoundTag reward = new CompoundTag(); reward.putString("Id", "spell");
                reward.putString("Spell", kind.equals("none") ? "missing_test_spell" : spellId);
                CompoundTag settled = read(fixture.player);
                ListTag choices = new ListTag(); choices.add(reward); settled.put("InheritanceRewards", choices); write(fixture.player, settled);
                CompoundTag preview = LifeSimulationSystem.inheritancePreview(fixture.player, reward);
                helper.assertTrue(preview.getString("Kind").equals(kind), "Preview identifies learned spell, known proficiency, or unavailable spell");
                var before = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
                LifeSimulationSystem.handleAction(fixture.player, "inherit:spell");
                var after = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
                if (kind.equals("none")) {
                    helper.assertTrue(!read(fixture.player).getBoolean("RewardClaimed") && after.equals(before),
                            "Invalid spell grants neither an unlearned spell nor fallback materials");
                    LifeSimulationSystem.handleAction(fixture.player, "inherit:none");
                } else {
                    helper.assertTrue(read(fixture.player).getCompound("InheritanceReceipt").equals(preview), "Receipt matches preview");
                    if (kind.equals("spell")) helper.assertTrue(after.knownSpells().contains(spellId), "Simulated learned spell is inherited even by a mortal host");
                    else helper.assertTrue(after.spellProficiency(spellId) - before.spellProficiency(spellId) == preview.getInt("Gain"), "Already-known spell grants displayed proficiency");
                }
                LifeSimulationSystem.setAttempts(fixture.player, 3); LifeSimulationSystem.handleAction(fixture.player, "start");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void trainingAndRestShareThreeAnnualActions(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Age", 22); life.putInt("Realm", 4); life.putInt("Health", 60);
            life.putString("ActiveMethod", com.example.immortal_cultivation_mod.attachment.CultivationMethods.BASIC_BREATHING);
            life.putLong("CultivationProgress", 0); life.putInt("AnnualActionPoints", 3); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "activity:seclusion");
            CompoundTag trained = read(fixture.player);
            helper.assertTrue(trained.getInt("AnnualActionPoints") == 1 && trained.getBoolean("SeclusionThisYear"), "Seclusion consumes two shared annual actions and marks its use");
            LifeSimulationSystem.handleAction(fixture.player, "activity:seclusion");
            helper.assertTrue(read(fixture.player).equals(trained), "Repeated seclusion cannot gain cultivation or consume another action");
            LifeSimulationSystem.handleAction(fixture.player, "activity:rest");
            CompoundTag rested = read(fixture.player);
            helper.assertTrue(rested.getInt("AnnualActionPoints") == 0 && rested.getInt("Health") == 64, "Rest uses the final action with bounded recovery");
            LifeSimulationSystem.handleAction(fixture.player, "activity:breathing");
            LifeSimulationSystem.handleAction(fixture.player, "activity:consolidate");
            helper.assertTrue(read(fixture.player).equals(rested), "Breathing and consolidation cannot bypass the annual budget");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void parentsCannotBeBefriendedButOrdinaryPeopleCan(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("AnnualActionPoints", 3);
            ListTag people = life.getList("Relationships", Tag.TAG_COMPOUND);
            for (int index = 0; index < 2; index++) {
                CompoundTag parent = people.getCompound(index);
                parent.putString("Relation", index == 0 ? "father" : "mother");
                parent.putBoolean("Alive", true); parent.putBoolean("Observed", true); parent.putBoolean("Captive", false);
                parent.putInt("Affinity", 90); parent.putInt("Trust", 90); parent.putBoolean("Friend", true);
            }
            LifeSimulationBalance.normalize(life);
            for (int index = 0; index < 2; index++) {
                CompoundTag parent = people.getCompound(index);
                helper.assertTrue(!parent.getBoolean("Friend") && parent.getInt("Trust") == 90,
                        "Legacy parent friendship is cleared without changing trust or family role");
            }
            write(fixture.player, life);
            LifeSimulationSystem.writeClientState(fixture.player, new CompoundTag());
            life = read(fixture.player);
            people = life.getList("Relationships", Tag.TAG_COMPOUND);
            LifeSimulationSystem.handleAction(fixture.player, "relationship:0:friend");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:" + people.getCompound(1).getUUID("Id") + ":friend");
            helper.assertTrue(read(fixture.player).equals(life), "Both numeric and UUID parent-friend actions are rejected without consuming actions or wealth");
            CompoundTag person = people.getCompound(0).copy();
            person.putUUID("Id", UUID.randomUUID()); person.putString("Relation", "stranger"); person.putBoolean("Friend", false);
            int index = people.size(); people.add(person); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "relationship:" + index + ":friend");
            helper.assertTrue(read(fixture.player).getList("Relationships", Tag.TAG_COMPOUND).getCompound(index).getBoolean("Friend"),
                    "Ordinary people can still become friends");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void deadPeopleRejectEveryRelationshipAction(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("AnnualActionPoints", 3);
            CompoundTag dead = life.getList("Relationships", Tag.TAG_COMPOUND).getCompound(0);
            dead.putString("Relation", "friend");
            dead.putBoolean("Alive", false); dead.putBoolean("Available", false); dead.putInt("Affinity", 90); dead.putInt("Trust", 90); dead.putBoolean("Friend", true);
            write(fixture.player, life);
            LifeSimulationSystem.writeClientState(fixture.player, new CompoundTag());
            life = read(fixture.player);
            for (String action : new String[]{"talk", "gift", "help", "friend", "breakup", "protector", "nemesis"}) LifeSimulationSystem.handleAction(fixture.player, "relationship:0:" + action);
            helper.assertTrue(read(fixture.player).equals(life), "A memorial cannot be befriended, unfriended, gifted, challenged or recruited; no action or wealth is consumed");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void routineEditsAcknowledgeEveryRequestInAnOpenSession(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            fixture.start(helper);
            OuterSystemManager.openScreen(fixture.player);
            helper.assertTrue(OuterSystemManager.isScreenOpen(fixture.player), "Opening the life screen subscribes it to server state");
            long revision = OuterSystemData.read(fixture.player).getLong("LifeActionRevision");
            helper.assertTrue(fixture.screenResponses.getLast().getBoolean("OpenRequested"),
                    "Only an explicit Open response requests a new client interface");
            OuterSystemManager.handleLifeAction(fixture.player, "routine:slot:0:study:any");
            helper.assertTrue(read(fixture.player).getCompound("Routine").getCompound("0").getString("Action").equals("study")
                            && OuterSystemData.read(fixture.player).getLong("LifeActionRevision") == revision + 1,
                    "First editor click saves and acknowledges its chosen action");
            helper.assertTrue(fixture.screenResponses.getLast().getLong("LifeActionRevision") == revision + 1
                            && !fixture.screenResponses.getLast().getBoolean("OpenRequested"),
                    "The actual action response carries its acknowledgment without reopening a closed client interface");
            OuterSystemManager.refreshScreen(fixture.player);
            helper.assertTrue(fixture.screenResponses.getLast().getLong("LifeActionRevision") == revision + 1,
                    "Periodic refreshes preserve the acknowledgment instead of pretending to complete another request");
            OuterSystemManager.handleLifeAction(fixture.player, "routine:slot:0:work:health");
            CompoundTag saved = read(fixture.player);
            helper.assertTrue(saved.getCompound("Routine").getCompound("0").getString("Action").equals("work")
                            && saved.getCompound("Routine").getCompound("0").getString("Condition").equals("health")
                            && OuterSystemData.read(fixture.player).getLong("LifeActionRevision") == revision + 2,
                    "A second click remains usable and receives its own acknowledgment");
            OuterSystemManager.handleLifeAction(fixture.player, "routine:slot:0:missing_action:any");
            helper.assertTrue(read(fixture.player).equals(saved)
                            && OuterSystemData.read(fixture.player).getLong("LifeActionRevision") == revision + 3,
                    "Invalid choices are rejected without leaving the client waiting indefinitely");
            OuterSystemManager.closeScreen(fixture.player);
            OuterSystemManager.handleLifeAction(fixture.player, "routine:slot:0:study:any");
            helper.assertTrue(!OuterSystemManager.isScreenOpen(fixture.player) && read(fixture.player).equals(saved)
                            && OuterSystemData.read(fixture.player).getLong("LifeActionRevision") == revision + 3,
                    "An action arriving after explicit Close cannot reopen or mutate the closed interface");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void oldLifeGetsOnePersistentAnnualRoutineToken(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.remove("SimulationId");
            write(fixture.player, life);
            CompoundTag first = new CompoundTag();
            CompoundTag second = new CompoundTag();
            LifeSimulationSystem.writeClientState(fixture.player, first);
            LifeSimulationSystem.writeClientState(fixture.player, second);
            helper.assertTrue(first.getCompound("LifeSimulation").hasUUID("SimulationId")
                            && first.getCompound("LifeSimulation").getUUID("SimulationId")
                                    .equals(second.getCompound("LifeSimulation").getUUID("SimulationId"))
                            && first.getCompound("LifeSimulation").getUUID("SimulationId")
                                    .equals(read(fixture.player).getUUID("SimulationId")),
                    "An existing save receives a stable saved token usable by the annual editor");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void cultivationInheritanceUsesSimulatedTotalAndIgnoresHostBodyBonus(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Realm", 4); life.putInt("HighestRealm", 4); life.putString("SpiritualRoot", "single");
            life.putLong("CultivationProgress", 8000); write(fixture.player, life);
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player)
                    .withCultivationLevel(CultivationLevels.allLevels().get(13)).withCultivationProgress(0)
                    .withBodyType(com.example.immortal_cultivation_mod.attachment.BodyTypes.INNATE_DAO);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, data);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag settled = read(fixture.player);
            CompoundTag reward = settled.getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(reward.getLong("SourceCultivation") == 3_400, "Simulated total includes 100 + 800 + 2500 completed stages, excluding unfinished current progress");
            reward.putInt("Percent", 25); write(fixture.player, settled);
            CompoundTag preview = LifeSimulationSystem.inheritancePreview(fixture.player, reward);
            helper.assertTrue(preview.getLong("Gain") == 850, "Exactly 25% of simulated cultivation, independent of host realm and body bonus");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            helper.assertTrue(com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player).cultivationProgress() == 850
                            && read(fixture.player).getCompound("InheritanceReceipt").equals(preview), "Actual delivery matches preview");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void inheritanceCarriesExactUserExampleAcrossRealmsOnce(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Realm", 6); life.putInt("HighestRealm", 6); life.putString("SpiritualRoot", "single");
            life.putLong("CultivationProgress", 17_173);
            CompoundTag journal = new CompoundTag();
            for (int i = 0; i < 8; i++) journal.putString("story_" + i, "completed");
            life.put("StoryJournal", journal); write(fixture.player, life);
            var initial = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player)
                    .withCultivationLevel(CultivationLevels.allLevels().get(1)).withCultivationProgress(0);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, initial);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag reward = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(0);
            CompoundTag preview = LifeSimulationSystem.inheritancePreview(fixture.player, reward);
            helper.assertTrue(reward.getInt("Percent") == 13 && reward.getLong("SourceCultivation") == 36_400
                            && preview.getLong("Gain") == 4_732,
                    "13% of completed simulated stages is exactly 4732, excluding unfinished 17173");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            var result = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
            helper.assertTrue(result.cultivationLevel().equals(CultivationLevels.allLevels().get(4))
                            && result.cultivationProgress() == 1_332
                            && result.skillPoints() == initial.skillPoints() + 30,
                    "Inheritance crosses all three capped stages and delivers Zhuji early 1332/8000");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            helper.assertTrue(com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player).equals(result),
                    "Repeated claims never repeat cultivation or breakthrough skill points");
            var exact = LifeSimulationInheritance.carry(CultivationLevels.allLevels().get(1), 0, 3_400);
            helper.assertTrue(exact.level().equals(CultivationLevels.allLevels().get(4)) && exact.progress() == 0,
                    "An exact boundary advances immediately, leaving zero progress in the next stage");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void inheritanceSelectorsUseOnlySimulatedInventoryAndSpells(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Realm", 4); life.putInt("HighestRealm", 4); life.putString("SpiritualRoot", "single");
            life.putString("ActiveMethod", com.example.immortal_cultivation_mod.attachment.CultivationMethods.BASIC_BREATHING);
            ListTag spells = new ListTag();
            var eligible = com.example.immortal_cultivation_mod.spell.ModSpells.all().stream()
                    .filter(spell -> LifeSimulationInheritance.canLearnSpell(life, spell.id())).limit(2).toList();
            helper.assertTrue(eligible.size() == 2, "Fixture has two eligible learned spells");
            eligible.forEach(spell -> spells.add(StringTag.valueOf(spell.id()))); life.put("DiscoveredSpells", spells);
            ListTag inventory = new ListTag();
            for (String id : new String[]{"minecraft:bread", "minecraft:apple"}) {
                CompoundTag item = new CompoundTag(); item.putString("Id", id); item.putInt("Count", 3); inventory.add(item);
            }
            life.put("Inventory", inventory); write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            var before = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND);
            String originalSpell = before.getCompound(1).getString("Spell"), originalItem = before.getCompound(2).getString("Item");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:spell:next");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:materials:next");
            var changed = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND);
            helper.assertTrue(!changed.getCompound(1).getString("Spell").equals(originalSpell)
                            && !changed.getCompound(2).getString("Item").equals(originalItem),
                    "Both source-backed inheritance categories can be switched independently");
            CompoundTag snapshot = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, snapshot);
            helper.assertTrue(snapshot.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(1).getInt("ChoiceCount") == 2,
                    "Client receives authoritative option counts and previews");
            var clientOptions = snapshot.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(2).getList("Choices", Tag.TAG_COMPOUND);
            helper.assertTrue(clientOptions.size() == 2
                            && clientOptions.getCompound(0).getString("Item").equals("minecraft:bread")
                            && clientOptions.getCompound(1).getString("Item").equals("minecraft:apple")
                            && clientOptions.getCompound(1).getCompound("Preview").getInt("Count") == 3,
                    "Picker receives every owned item and its exact delivery preview");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:spell:previous");
            helper.assertTrue(read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(1).getString("Spell").equals(originalSpell),
                    "Previous reverses next without rerolling rewards");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:spell:1");
            helper.assertTrue(read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(1).getString("Spell").equals(eligible.get(1).id()),
                    "Clicking a spell chooses that exact entry without cycling");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:materials:1");
            CompoundTag selected = read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(2).copy();
            helper.assertTrue(selected.getString("Item").equals("minecraft:apple"), "Clicking an item chooses that exact entry");
            for (String invalid : new String[]{"-1", "2", "2147483648", "minecraft:diamond"})
                LifeSimulationSystem.handleAction(fixture.player, "inherit_select:materials:" + invalid);
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:materials:minecraft:diamond");
            helper.assertTrue(read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(2).equals(selected),
                    "Forged selectors cannot invent unowned rewards");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            helper.assertTrue(read(fixture.player).getCompound("InheritanceReceipt").getString("Item").equals(selected.getString("Item")),
                    "Claim delivers the switched item, not the initial random item");
            LifeSimulationSystem.handleAction(fixture.player, "inherit_select:spell:next");
            helper.assertTrue(read(fixture.player).getList("InheritanceRewards", Tag.TAG_COMPOUND).getCompound(1).getString("Spell").equals(eligible.get(1).id()),
                    "Claimed lives cannot switch or claim another category");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void simulationAttemptRefillRequiresFourGameDays(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            fixture.start(helper); LifeSimulationSystem.setAttempts(fixture.player, 0);
            long day = fixture.player.server.overworld().getDayTime() / 24_000L;
            CompoundTag life = read(fixture.player); life.putLong("LastAttemptDay", day - 3); write(fixture.player, life);
            helper.assertTrue(LifeSimulationSystem.attempts(fixture.player) == 0, "Three game days do not refill an attempt");
            CompoundTag snapshot = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, snapshot);
            helper.assertTrue(snapshot.getCompound(LIFE).getLong("DaysUntilAttempt") == 1, "Countdown matches the four-day rule");
            life = read(fixture.player); life.putLong("LastAttemptDay", day - 4); write(fixture.player, life);
            helper.assertTrue(LifeSimulationSystem.attempts(fixture.player) == 1
                            && LifeSimulationSystem.attempts(fixture.player) == 1, "Fourth day grants exactly one attempt, once");
            life = read(fixture.player); life.putLong("LastAttemptDay", day - 100); write(fixture.player, life);
            helper.assertTrue(LifeSimulationSystem.attempts(fixture.player) == 3, "Long absences still cap stored attempts at three");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void majorBreakthroughCannotBypassTribulationAfterWithdrawal(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Age", 40); life.putInt("Realm", 6); life.putInt("HighestRealm", 6);
            life.putInt("Health", 100); life.putInt("AnnualActionPoints", 3); life.putInt("CultivationStability", 100);
            life.putInt("Comprehension", 100); life.putInt("Luck", 100);
            life.putString("ActiveMethod", com.example.immortal_cultivation_mod.attachment.CultivationMethods.BASIC_BREATHING);
            life.putBoolean("MethodContinuation", true); life.putInt("MethodContinuationLimit", 27);
            life.putLong("CultivationProgress", CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(6)).qiNeeded());
            life.putInt("LastMajorCrisis", 40); // An unrelated recent danger must not permit an ordinary roll.
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "activity:consolidate");
            LifeSimulationSystem.handleAction(fixture.player, "choice:attempt");
            life = read(fixture.player);
            helper.assertTrue(life.getInt("Realm") == 6 && life.getCompound("Story").getString("Id").equals("life_danger_tribulation"),
                    "Even a recent crisis cannot replace a required major trial with a percentage breakthrough");
            LifeSimulationStories.complete(life, "withdraw");
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "activity:consolidate");
            LifeSimulationSystem.handleAction(fixture.player, "choice:attempt");
            life = read(fixture.player);
            helper.assertTrue(life.getInt("Realm") == 6 && !life.contains("Story"),
                    "Withdrawing cannot yield a normal breakthrough or unlimited new trials within the same year");
            life.putInt("Age", 41); life.putInt("AnnualActionPoints", 3);
            write(fixture.player, life);
            LifeSimulationSystem.handleAction(fixture.player, "activity:consolidate");
            LifeSimulationSystem.handleAction(fixture.player, "choice:attempt");
            life = read(fixture.player);
            helper.assertTrue(life.getInt("Realm") == 6 && life.getCompound("Story").getString("Id").equals("life_danger_tribulation"),
                    "A recovered retry in a later year starts the same mandatory trial despite the earlier journal record");
            LifeSimulationStories.complete(life, "a");
            helper.assertTrue(life.getInt("Realm") == 7,
                    "The successful retry applies its breakthrough once instead of being blocked by a prior withdrawal record");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void fullInventoryReservesOneChoiceAndRestoresAtomicDelivery(GameTestHelper helper) throws Exception {
        try (Player fixture = new Player(helper); Player restarted = new Player(helper, fixture.player.getUUID())) {
            fixture.start(helper);
            ownSimulatedStones(fixture.player, 18);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            Item stone = com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get();
            for (int slot = 0; slot < fixture.player.getInventory().items.size(); slot++) {
                fixture.player.getInventory().items.set(slot, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 64));
            }
            fixture.player.getInventory().items.set(1, new net.minecraft.world.item.ItemStack(stone, 60));
            int entities = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    fixture.player.getBoundingBox().inflate(3)).size();
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            CompoundTag reserved = read(fixture.player);
            CompoundTag reservation = reserved.getCompound("PendingInheritance");
            helper.assertTrue(!reserved.getBoolean("RewardClaimed") && reserved.getString("ClaimTransaction").equals("pending")
                            && reservation.getString("RewardId").equals("materials") && reservation.getCompound("Receipt").getInt("Count") == 18
                            && inventoryCount(fixture.player, stone) == 60,
                    "Four remaining stack slots cannot partially deliver an eighteen-item inheritance; chosen reward stays pending");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            fixture.player.getBoundingBox().inflate(3)).size() == entities,
                    "A full backpack never puts inheritance into independently saved ground entities");
            CompoundTag pendingDisk = net.minecraft.nbt.NbtIo.readCompressed(LifeInheritanceDelivery.playerFile(fixture.player), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            helper.assertTrue(pendingDisk.getCompound("NeoForgeData").getCompound(OuterSystemData.ROOT_TAG)
                            .getCompound(LIFE).getCompound("PendingInheritance").equals(reservation),
                    "The reserved choice is present in actual durable player NBT before delivery");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress");
            helper.assertTrue(read(fixture.player).getCompound("PendingInheritance").equals(reservation)
                            && inventoryCount(fixture.player, stone) == 60,
                    "Rapid clicks on a different reward cannot replace the frozen pending choice or grant partial items");
            fixture.player.getInventory().items.set(0, net.minecraft.world.item.ItemStack.EMPTY);
            LifeSimulationSystem.handleAction(fixture.player, "inherit:spell");
            CompoundTag claimed = read(fixture.player);
            helper.assertTrue(claimed.getBoolean("RewardClaimed") && !claimed.contains("PendingInheritance")
                            && claimed.getUUID("ClaimId").equals(reserved.getUUID("ClaimId"))
                            && claimed.getCompound("InheritanceReceipt").equals(reservation.getCompound("Receipt"))
                            && inventoryCount(fixture.player, stone) == 78
                            && fixture.player.getInventory().items.get(1).getCount() == 64
                            && fixture.player.getInventory().items.get(0).getCount() == 14,
                    "Retry fills an existing stack and an empty slot exactly once with the reserved eighteen materials");
            CompoundTag deliveredDisk = net.minecraft.nbt.NbtIo.readCompressed(LifeInheritanceDelivery.playerFile(fixture.player), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            helper.assertTrue(deliveredDisk.getCompound("NeoForgeData").getCompound(OuterSystemData.ROOT_TAG).getCompound(LIFE).getBoolean("RewardClaimed")
                            && deliveredDisk.getList("Inventory", Tag.TAG_COMPOUND).equals(fixture.player.saveWithoutId(new CompoundTag()).getList("Inventory", Tag.TAG_COMPOUND)),
                    "Delivered inventory and claimed receipt are committed together in the same actual player snapshot");
            // Reproduce an integrated server loading a stale owner tag from level.dat.
            restarted.player.load(pendingDisk);
            helper.assertTrue(restarted.player.getUUID().equals(fixture.player.getUUID()),
                    "A reconnect uses the original account UUID, so canonical player-data recovery targets that same account");
            LifeInheritanceDelivery.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(restarted.player));
            helper.assertTrue(read(restarted.player).getBoolean("RewardClaimed") && inventoryCount(restarted.player, stone) == 78,
                    "Restart recovers the newer atomic UUID snapshot instead of reopening a stale pending owner snapshot");
            LifeSimulationSystem.handleAction(restarted.player, "inherit:materials");
            helper.assertTrue(inventoryCount(restarted.player, stone) == 78
                            && read(restarted.player).getCompound("InheritanceReceipt").equals(reservation.getCompound("Receipt")),
                    "Reconnect and repeated claim cannot deliver the committed reward a second time");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void failedInheritanceCheckpointCannotGrantOrLockAChoice(GameTestHelper helper) throws Exception {
        try (Player fixture = new Player(helper)) {
            fixture.start(helper);
            ownSimulatedStones(fixture.player, 18);
            LifeSimulationSystem.handleAction(fixture.player, "settle");
            CompoundTag before = read(fixture.player);
            Item stone = com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get();
            java.nio.file.Path target = LifeInheritanceDelivery.playerFile(fixture.player);
            helper.assertTrue(target.toAbsolutePath().normalize().startsWith(fixture.player.server.getWorldPath(
                            net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toAbsolutePath().normalize()),
                    "Failure fixture stays within this isolated game-test player-data directory");
            java.nio.file.Files.createDirectories(target);
            try {
                LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
                helper.assertTrue(read(fixture.player).equals(before) && inventoryCount(fixture.player, stone) == 0,
                        "A failed reservation checkpoint grants nothing and restores the original unlocked choices");
            } finally {
                java.nio.file.Files.delete(target);
            }
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            helper.assertTrue(read(fixture.player).getBoolean("RewardClaimed") && inventoryCount(fixture.player, stone) == 18,
                    "The same inheritance succeeds once storage becomes writable again");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void unseenAndCaptivePeopleStayPrivateAndUuidTargetsSurviveFiltering(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("AnnualActionPoints", 3); life.remove("CurrentEvent");
            ListTag people = life.getList("Relationships", Tag.TAG_COMPOUND);
            CompoundTag known = people.getCompound(0).copy();
            if (!known.hasUUID("Id")) known.putUUID("Id", UUID.randomUUID());
            known.putBoolean("Alive", true); known.putBoolean("Observed", true); known.putInt("Affinity", 50);
            CompoundTag hidden = known.copy(); hidden.putUUID("Id", UUID.randomUUID()); hidden.putBoolean("Observed", false); hidden.putString("Name", "Unseen test captor");
            CompoundTag captive = known.copy(); captive.putUUID("Id", UUID.randomUUID()); captive.putBoolean("Captive", true);
            people.clear(); people.add(hidden); people.add(captive); people.add(known); life.put("Relationships", people);
            LifeSimulationBalance.normalize(life);
            helper.assertTrue(!hidden.getBoolean("Available") && !captive.getBoolean("Available") && known.getBoolean("Available"),
                    "Normalization never turns unseen or captive story actors into available social contacts");
            ListTag memories = new ListTag(); memories.add(life.copy()); life.put("Memories", memories); write(fixture.player, life);
            CompoundTag client = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, client);
            ListTag visible = client.getCompound("LifeSimulation").getList("Relationships", Tag.TAG_COMPOUND);
            helper.assertTrue(visible.size() == 2 && visible.getCompound(0).getUUID("Id").equals(captive.getUUID("Id"))
                            && visible.getCompound(1).getUUID("Id").equals(known.getUUID("Id")) && visible.getCompound(1).getInt("ServerIndex") == 2
                            && client.getCompound("LifeSimulation").getList("Memories", Tag.TAG_COMPOUND).getCompound(0)
                                    .getList("Relationships", Tag.TAG_COMPOUND).size() == 2,
                    "Live and remembered client snapshots omit unseen names while preserving each known person's server identity");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:" + hidden.getUUID("Id") + ":friend");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:1:gift");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:malformed-uuid:help");
            helper.assertTrue(read(fixture.player).equals(life), "Hidden, captive and malformed targets cannot consume social actions or wealth");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:" + known.getUUID("Id") + ":talk");
            CompoundTag after = read(fixture.player);
            helper.assertTrue(after.getInt("AnnualActionPoints") == 2
                            && after.getList("Relationships", Tag.TAG_COMPOUND).getCompound(2).getInt("Affinity") == 54
                            && after.getList("Relationships", Tag.TAG_COMPOUND).getCompound(0).getInt("Affinity") == 50,
                    "UUID-based action reaches the intended known NPC even when client filtering compacts its row index");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacySocialIdentityIsPersistedBeforeItsFirstClientClick(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.remove("CurrentEvent"); life.putInt("AnnualActionPoints", 3);
            CompoundTag person = life.getList("Relationships", Tag.TAG_COMPOUND).getCompound(0);
            person.remove("Id"); person.putBoolean("Alive", true); person.putInt("Affinity", 40);
            write(fixture.player, life);
            CompoundTag first = new CompoundTag(), next = new CompoundTag();
            LifeSimulationSystem.writeClientState(fixture.player, first);
            LifeSimulationSystem.writeClientState(fixture.player, next);
            UUID identity = first.getCompound("LifeSimulation").getList("Relationships", Tag.TAG_COMPOUND).getCompound(0).getUUID("Id");
            helper.assertTrue(identity.equals(next.getCompound("LifeSimulation").getList("Relationships", Tag.TAG_COMPOUND).getCompound(0).getUUID("Id"))
                            && identity.equals(read(fixture.player).getList("Relationships", Tag.TAG_COMPOUND).getCompound(0).getUUID("Id")),
                    "The first client snapshot persists its migrated NPC UUID instead of generating a different identity per refresh");
            LifeSimulationSystem.handleAction(fixture.player, "relationship:" + identity + ":talk");
            helper.assertTrue(read(fixture.player).getInt("AnnualActionPoints") == 2
                            && read(fixture.player).getList("Relationships", Tag.TAG_COMPOUND).getCompound(0).getInt("Affinity") == 44,
                    "The first real UUID click on a legacy NPC reaches that same saved identity");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void routinePacketCannotAdvanceASecondYear(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putInt("Age",22);life.putInt("Realm",0);life.putInt("Health",100);life.putInt("Mentality",100);
            life.remove("CurrentEvent");life.remove("Story");life.remove("Battle");write(fixture.player,life);
            String action="routine:run:"+life.getUUID("SimulationId")+":"+life.getInt("Age");
            LifeSimulationSystem.handleAction(fixture.player,action);
            var after=read(fixture.player);
            helper.assertTrue(after.getInt("Age")==23,"One planned routine advances one year");
            // Keep the second run otherwise eligible, so the saved year token is what rejects it.
            after.remove("CurrentEvent");after.remove("Story");after.remove("Battle");write(fixture.player,after);
            LifeSimulationSystem.handleAction(fixture.player,action);
            helper.assertTrue(read(fixture.player).equals(after),"Replayed annual token cannot execute next year's actions");
            LifeSimulationSystem.handleAction(fixture.player,"routine:run:"+java.util.UUID.randomUUID()+":23");
            helper.assertTrue(read(fixture.player).equals(after),"Routine from another simulated life cannot execute");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void routineStopsImmediatelyAtAStoryDecision(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper); life.putInt("Realm", 4); life.putInt("Age", 22); write(fixture.player, life);
            var definition = LifeSimulationStories.definitions(helper.getLevel().getServer().getResourceManager()).stream().filter(d -> d.getString("id").equals("life_event_rain_sword")).findFirst().orElseThrow();
            LifeSimulationStories.start(life, definition); write(fixture.player, life);
            // Open the actual client snapshot once, applying legacy method/identity
            // migrations before testing that a blocked routine has no gameplay effect.
            LifeSimulationSystem.writeClientState(fixture.player, new CompoundTag());
            life = read(fixture.player);
            LifeSimulationSystem.handleAction(fixture.player, "routine:run");
            helper.assertTrue(read(fixture.player).equals(life), "Autoplay cannot select story choices, spend actions or advance age while a decision is pending");
        }
        helper.succeed();
    }

    @GameTest(template="fate_test")
    public static void routineRunsFamilyExerciseAndNoConditionThroughRealActions(GameTestHelper helper){
        try(Player fixture=new Player(helper)){
            CompoundTag life=fixture.start(helper);life.putInt("Age",22);life.putInt("Realm",0);life.putInt("Health",80);life.putInt("Mentality",80);life.putInt("AnnualActionPoints",3);life.remove("CurrentEvent");life.remove("Story");life.remove("Battle");write(fixture.player,life);
            LifeSimulationSystem.handleAction(fixture.player,"routine:slot:0:family:none");LifeSimulationSystem.handleAction(fixture.player,"routine:slot:1:exercise:none");LifeSimulationSystem.handleAction(fixture.player,"routine:slot:2:study:any");
            life=read(fixture.player);helper.assertTrue(life.getCompound("Routine").getCompound("0").getString("Condition").equals("none"),"No-condition choice is saved through actual server action");
            String token="routine:run:"+life.getUUID("SimulationId")+":"+life.getInt("Age");LifeSimulationSystem.handleAction(fixture.player,token);var result=read(fixture.player);
            helper.assertTrue(result.getInt("Age")==23,"Direct run token executes one complete planned year");
            for(String action:java.util.List.of("family","exercise","study"))helper.assertTrue(result.getList("History",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).anyMatch(log->log.getString("Key").equals("life.immortal_cultivation_mod.activity."+action)),"Newly available planner action really executes: "+action);
            result.remove("CurrentEvent");result.remove("Story");result.remove("Battle");write(fixture.player,result);LifeSimulationSystem.handleAction(fixture.player,token);helper.assertTrue(read(fixture.player).equals(result),"Direct run button's stale year token cannot run another year");
        }helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rootlessMortalCannotLearnOrInheritSpellsOrHostCultivation(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putString("SpiritualRoot", "none"); life.putInt("Realm", 0); life.putInt("HighestRealm", 0);
            life.putLong("CultivationProgress", 999_999); life.put("DiscoveredSpells", new ListTag()); life.put("Inventory", new ListTag());
            LifeSimulationSystem.applyEffect(life, "spell:fireball");
            helper.assertTrue(life.getList("DiscoveredSpells", Tag.TAG_STRING).isEmpty(), "Story spell reward cannot teach a rootless mortal");
            var data = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player).withCultivationLevel(CultivationLevels.allLevels().get(13)).withCultivationProgress(0);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(fixture.player, data);
            write(fixture.player, life); LifeSimulationSystem.handleAction(fixture.player, "settle");
            var before = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
            var inventoryBefore = fixture.player.saveWithoutId(new CompoundTag()).getList("Inventory", Tag.TAG_COMPOUND).copy();
            CompoundTag client = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, client);
            var rewards = client.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND);
            helper.assertTrue(rewards.getCompound(0).getInt("Percent") == 2 && rewards.getCompound(0).getLong("SourceCultivation") == 0,
                    "Mortal's 2% is zero, never 2400000 from the host's realm");
            for (var tag : rewards) helper.assertTrue(((CompoundTag)tag).getCompound("Preview").getString("Kind").equals("none"), "Empty life offers no invented inheritance");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:progress"); LifeSimulationSystem.handleAction(fixture.player, "inherit:spell"); LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            var after = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(fixture.player);
            helper.assertTrue(!read(fixture.player).getBoolean("RewardClaimed") && after.cultivationProgress() == 0
                            && after.knownSpells().equals(before.knownSpells()) && fixture.player.saveWithoutId(new CompoundTag()).getList("Inventory", Tag.TAG_COMPOUND).equals(inventoryBefore), "Forged unavailable clicks grant nothing");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:none");
            helper.assertTrue(read(fixture.player).getBoolean("RewardClaimed") && read(fixture.player).getCompound("InheritanceReceipt").getString("Kind").equals("none"), "Empty inheritance can finish without locking the next life");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyPendingInventedRewardsAreRepairedBeforeDelivery(GameTestHelper helper) {
        try (Player fixture = new Player(helper)) {
            CompoundTag life = fixture.start(helper);
            life.putString("SpiritualRoot", "none"); life.putInt("Realm", 0); life.putInt("HighestRealm", 0); life.put("Inventory", new ListTag());
            ListTag spells = new ListTag(); spells.add(StringTag.valueOf("fireball")); life.put("DiscoveredSpells", spells);
            life.putBoolean("Finished", true); life.putBoolean("Alive", false); life.remove("InheritanceSourceVersion");
            CompoundTag selected = new CompoundTag(); selected.putString("Id", "materials"); selected.putInt("Bundle", 1);
            ListTag rewards = new ListTag(); rewards.add(selected); life.put("InheritanceRewards", rewards);
            CompoundTag receipt = new CompoundTag(); receipt.putString("Kind", "materials"); receipt.putString("Item", "immortal_cultivation_mod:high_spirit_stone"); receipt.putInt("Count", 4);
            CompoundTag pending = new CompoundTag(); pending.putString("RewardId", "materials"); pending.put("Selected", selected); pending.put("Receipt", receipt);
            life.put("PendingInheritance", pending); life.putString("ClaimTransaction", "pending"); life.putUUID("ClaimId", UUID.randomUUID()); write(fixture.player, life);
            CompoundTag client = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, client);
            var repaired = read(fixture.player);
            helper.assertTrue(!repaired.contains("PendingInheritance") && repaired.getList("DiscoveredSpells", Tag.TAG_STRING).isEmpty()
                            && repaired.getList("InheritanceRewards", Tag.TAG_COMPOUND).size() == 3, "Existing pending fake receipt is safely reopened as corrected choices");
            CompoundTag second = new CompoundTag(); LifeSimulationSystem.writeClientState(fixture.player, second);
            helper.assertTrue(repaired.equals(read(fixture.player)) && client.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND)
                            .equals(second.getCompound(LIFE).getList("InheritanceRewards", Tag.TAG_COMPOUND)), "Corrected choices persist without rerolling per preview");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:materials");
            helper.assertTrue(!read(fixture.player).getBoolean("RewardClaimed") && inventoryCount(fixture.player, com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get()) == 0,
                    "Old pending invented item cannot be claimed");
            LifeSimulationSystem.handleAction(fixture.player, "inherit:none");
            CompoundTag claimed = read(fixture.player); claimed.remove("InheritanceSourceVersion"); write(fixture.player, claimed);
            LifeSimulationSystem.writeClientState(fixture.player, new CompoundTag());
            helper.assertTrue(read(fixture.player).getBoolean("RewardClaimed") && read(fixture.player).getCompound("InheritanceReceipt").getString("Kind").equals("none"), "Claimed receipts remain final during legacy migration");
        }
        helper.succeed();
    }

    private static void ownSimulatedStones(ServerPlayer player, int count) {
        CompoundTag life = read(player); life.put("Inventory", new ListTag());
        LifeSimulationSystem.addSimulationItem(life, "immortal_cultivation_mod:spirit_stone", count); write(player, life);
    }

    private static CompoundTag read(ServerPlayer player) {
        return OuterSystemData.read(player).getCompound(LIFE).copy();
    }

    private static void write(ServerPlayer player, CompoundTag life) {
        CompoundTag outer = OuterSystemData.read(player);
        outer.put(LIFE, life.copy());
        OuterSystemData.save(player, outer);
    }

    private static CompoundTag pendingEvent() {
        CompoundTag choice = new CompoundTag();
        choice.putString("Id", "stale_choice");
        ListTag effects = new ListTag();
        effects.add(StringTag.valueOf("wealth:1000"));
        choice.put("Effects", effects);
        ListTag choices = new ListTag();
        choices.add(choice);
        CompoundTag event = new CompoundTag();
        event.putString("Id", "test_pending_event");
        event.put("Choices", choices);
        return event;
    }

    private static void sendStaleActions(ServerPlayer player) {
        for (String action : new String[]{"settle", "advance", "advance_5",
                "choice:stale_choice", "activity:work", "relationship:0:help",
                "map:select:futian_academy", "map:travel:foot", "map:arrival:enter",
                "map:quest", "map:local_activity", "map:buy_mount",
                "map:register_teleport", "map:companion_next"}) {
            LifeSimulationSystem.handleAction(player, action);
        }
    }

    private static int inventoryCount(ServerPlayer player, Item item) {
        return player.getInventory().items.stream()
                .filter(stack -> stack.is(item))
                .mapToInt(net.minecraft.world.item.ItemStack::getCount)
                .sum();
    }
}
