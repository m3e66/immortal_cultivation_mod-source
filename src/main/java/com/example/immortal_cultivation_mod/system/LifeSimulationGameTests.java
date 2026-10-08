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
@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class LifeSimulationGameTests {
    private static final String LIFE = "LifeSimulation";

    private LifeSimulationGameTests() {
    }

    private static final class Player implements AutoCloseable {
        final ServerPlayer player;
        private final EmbeddedChannel channel;

        Player(GameTestHelper helper) {
            player = new ServerPlayer(
                    helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "life-settlement"),
                    ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(
                    player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet) {
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
            helper.assertTrue(settled.getLong("Score") == 4_110L + realm * 1_100L,
                    "Existing score rewards only the years and achievements already earned");
            helper.assertTrue(settled.getInt("InsightEarned") >= 8
                            && settled.getInt("InsightEarned") <= 160
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
                LifeSimulationSystem.handleAction(fixture.player, "settle");
                helper.assertTrue(read(fixture.player).equals(life),
                        "Settlement ignores nonliving or absent simulation: " + invalidState);
            }
        }
        helper.succeed();
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
