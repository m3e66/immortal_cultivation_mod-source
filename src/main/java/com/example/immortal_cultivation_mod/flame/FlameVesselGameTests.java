package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameVesselGameTests {
    private static final class ChargingPlayer extends net.minecraft.server.level.ServerPlayer {
        ChargingPlayer(GameTestHelper h) {
            super(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "flame-vessel-test"),
                    net.minecraft.server.level.ClientInformation.createDefault());
        }

        void releaseAfter(int ticks) {
            // Drive the real release lifecycle with a deterministic server-held duration.
            useItemRemaining = getUseItem().getUseDuration(this) - ticks;
            releaseUsingItem();
        }
    }

    private static final class User implements AutoCloseable {
        final ChargingPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        User(GameTestHelper h) {
            // The shared floor-only template leaves prior batches' above-floor blocks intact.
            // Build an explicit air corridor so capture tests exercise their own obstruction.
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 2, 0), new BlockPos(6, 6, 6)))
                h.setBlock(pos, Blocks.AIR);
            player = new ChargingPlayer(h);
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 1))));
        }
        @Override public void close() { player.discard(); channel.close(); }
    }

    private static InteractionResult useHeldSeedOn(User user, BlockPos support) {
        return user.player.getMainHandItem().useOn(new UseOnContext(user.player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0),
                        Direction.UP, support, false)));
    }

    @GameTest(template = "fate_test")
    public static void everyFlameSeedSpawnsCapturableEssenceAndSurvivesVesselSave(GameTestHelper h) {
        Item[] seeds = {ModItems.CRIMSON_FLAME_SEED.get(), ModItems.EARTH_FLAME_SEED.get(),
                ModItems.WOOD_FLAME_HEART.get(), ModItems.SOLAR_FLAME_SEED.get(),
                ModItems.NETHER_FLAME_SEED.get(), ModItems.THUNDER_FLAME_SEED.get(),
                ModItems.DRAGON_FLAME_SEED.get(), ModItems.HEAVENLY_FLAME_SEED.get()};
        FlameKind[] kinds = {FlameKind.CRIMSON, FlameKind.EARTH, FlameKind.WOOD,
                FlameKind.SOLAR, FlameKind.NETHER, FlameKind.THUNDER, FlameKind.DRAGON, FlameKind.HEAVENLY};
        int[] expectedModelIndices = {1, 2, 3, 4, 5, 6, 7, 8};
        h.assertTrue(FlameKind.values().length == 9,
                "The stable catalog contains mortal fire and all eight captured flame kinds");
        var spawned = new ArrayList<SpiritFlameEntity>();
        try (var user = new User(h)) {
            BlockPos target = h.absolutePos(new BlockPos(3, 2, 3));
            BlockPos support = target.below();
            h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            var area = new AABB(target).inflate(.4);
            for (int i = 0; i < seeds.length; i++) {
                h.assertTrue(h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area).isEmpty(),
                        "Seed fixture has no existing flame before " + kinds[i]);
                var seed = new ItemStack(seeds[i], 2);
                user.player.setItemInHand(InteractionHand.MAIN_HAND, seed);
                var result = useHeldSeedOn(user, support);
                var flames = h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area);
                spawned.addAll(flames);
                h.assertTrue(result.consumesAction() && seed.getCount() == 1 && flames.size() == 1,
                        "Actual survival seed use spawns one essence and consumes one " + kinds[i]);
                var flame = flames.getFirst();
                UUID identity = flame.getUUID();
                h.assertTrue(flame.kind() == kinds[i], "Registered seed awakens its matching flame kind");
                var vessel = new ItemStack(ModItems.FLAME_VESSEL.get());
                user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                h.assertTrue(FlameVesselItem.capture(user.player, vessel, flame) && flame.isRemoved(),
                        "Seed essence can be captured into a held vessel");
                h.assertTrue(FlameVesselItem.flame(vessel) == kinds[i]
                                && identity.equals(FlameVesselItem.identity(vessel))
                                && FlameVesselItem.flame(vessel).modelIndex() == expectedModelIndices[i],
                        "Capture transfers kind, original UUID, and explicit filled model index");
                var saved = (CompoundTag) vessel.save(h.getLevel().registryAccess());
                var loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), saved);
                h.assertTrue(FlameVesselItem.flame(loaded) == kinds[i]
                                && identity.equals(FlameVesselItem.identity(loaded))
                                && FlameVesselItem.flame(loaded).modelIndex() == expectedModelIndices[i],
                        "Serialized vessel keeps seed kind, UUID, and explicit model index");
            }
        } finally { spawned.forEach(SpiritFlameEntity::discard); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void flameSeedsKeepCreativeStacksAndRejectOccupiedOrProtectedTargets(GameTestHelper h) {
        BlockPos target = h.absolutePos(new BlockPos(3, 2, 3));
        var area = new AABB(target).inflate(.4);
        try (var user = new User(h)) {
            BlockPos support = target.below();
            h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            var seed = new ItemStack(ModItems.THUNDER_FLAME_SEED.get(), 2);
            user.player.setItemInHand(InteractionHand.MAIN_HAND, seed);
            user.player.setGameMode(GameType.CREATIVE);
            var result = useHeldSeedOn(user, support);
            var flames = h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area);
            h.assertTrue(result.consumesAction() && seed.getCount() == 2 && flames.size() == 1
                            && flames.getFirst().kind() == FlameKind.THUNDER,
                    "Creative seed use spawns matching essence without consuming the seed");
            var flame = flames.getFirst();
            h.assertTrue(useHeldSeedOn(user, support) == InteractionResult.FAIL && seed.getCount() == 2
                            && flame.isAlive() && h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area).size() == 1,
                    "An existing essence prevents duplicate placement and preserves stack and source");
            flame.discard();
            user.player.setGameMode(GameType.SURVIVAL);
            h.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
            h.assertTrue(useHeldSeedOn(user, support) == InteractionResult.FAIL && seed.getCount() == 2
                            && h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area).isEmpty(),
                    "Solid targets reject seed placement without consumption or an essence");
            h.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            user.player.setGameMode(GameType.ADVENTURE);
            h.assertTrue(!user.player.getAbilities().mayBuild
                            && !useHeldSeedOn(user, support).consumesAction() && seed.getCount() == 2
                            && h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area).isEmpty(),
                    "Adventure build protection preserves seed stack and entity count");
        } finally {
            h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, area).forEach(SpiritFlameEntity::discard);
            h.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 40)
    public static void everyFlameTransfersIdentityWithoutDuplicationAndSurvivesSave(GameTestHelper h) {
        User user = new User(h);
        var vessels = new ArrayList<ItemStack>();
        var identities = new ArrayList<UUID>();
        try {
            int i = 0;
            for (FlameKind kind : FlameKind.values()) {
                if (!kind.special()) continue;
                var vessel = new ItemStack(ModItems.FLAME_VESSEL.get());
                user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                Vec3 pos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 3))).add(i++ * .15, .1, 0);
                var flame = SpiritFlameEntity.spawn(h.getLevel(), pos, kind);
                h.assertTrue(flame != null && !flame.hurt(h.getLevel().damageSources().magic(), 100000), "Special flame cannot be killed by damage");
                UUID id = flame.getUUID();
                h.assertTrue(FlameVesselItem.capture(user.player, vessel, flame), "Empty held vessel captures " + kind);
                h.assertTrue(flame.isRemoved() && FlameVesselItem.flame(vessel) == kind
                        && id.equals(FlameVesselItem.identity(vessel)), "Capture atomically transfers kind and original UUID");
                ItemStack loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)vessel.save(h.getLevel().registryAccess()));
                h.assertTrue(FlameVesselItem.flame(loaded) == kind && id.equals(FlameVesselItem.identity(loaded)), "Vessel survives item serialization");
                vessels.add(loaded); identities.add(id);
            }
        } catch (Throwable failure) { user.close(); throw failure; }
        h.runAfterDelay(2, () -> {
            try (user) {
                for (int i = 0; i < vessels.size(); i++) {
                    var vessel = vessels.get(i); FlameKind kind = FlameVesselItem.flame(vessel);
                    user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                    Vec3 pos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 4))).add(i * .15, .1, 0);
                    h.assertTrue(FlameVesselItem.release(user.player, vessel, pos) && FlameVesselItem.flame(vessel) == null,
                            "Release empties vessel only after entity spawn succeeds");
                    var entity = h.getLevel().getEntity(identities.get(i));
                    h.assertTrue(entity instanceof SpiritFlameEntity flame && flame.kind() == kind, "Released essence keeps original UUID and kind");
                    var saved = entity.saveWithoutId(new CompoundTag());
                    var loaded = new SpiritFlameEntity(ModEntities.SPIRIT_FLAME.get(), h.getLevel()); loaded.load(saved);
                    h.assertTrue(loaded.kind() == kind && loaded.getUUID().equals(entity.getUUID()), "Entity state survives server-save serialization");
                    h.assertTrue(!FlameVesselItem.release(user.player, vessel, pos), "Repeated release cannot duplicate essence");
                    entity.discard();
                }
                h.succeed();
            }
        });
    }

    @GameTest(template = "fate_test")
    public static void captureRejectsFullUnheldRemoteBlockedAndSpectatorVessels(GameTestHelper h) {
        try (var user = new User(h)) {
            var vessel = new ItemStack(ModItems.FLAME_VESSEL.get());
            Vec3 pos = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 3))).add(0, .1, 0);
            var flame = SpiritFlameEntity.spawn(h.getLevel(), pos, FlameKind.CRIMSON);
            h.assertTrue(!FlameVesselItem.capture(user.player, vessel, flame), "Unheld vessel cannot capture from inventory or forged packets");
            user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
            FlameVesselItem.fill(vessel, FlameKind.SOLAR, UUID.randomUUID());
            h.assertTrue(!FlameVesselItem.capture(user.player, vessel, flame) && flame.isAlive(), "Full vessel cannot overwrite or destroy essence");
            FlameVesselItem.fill(vessel, null, null);
            user.player.setGameMode(GameType.SPECTATOR);
            h.assertTrue(!FlameVesselItem.capture(user.player, vessel, flame), "Spectators cannot capture");
            user.player.setGameMode(GameType.SURVIVAL);
            flame.setPos(pos.add(0, 0, 10));
            h.assertTrue(!FlameVesselItem.capture(user.player, vessel, flame), "Remote capture rejected");
            flame.setPos(pos);
            BlockPos wall = h.absolutePos(new BlockPos(3, 3, 2));
            h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(wall.below(), Blocks.STONE.defaultBlockState());
            h.assertTrue(!FlameVesselItem.capture(user.player, vessel, flame), "Flames cannot be captured through solid walls");
            h.assertTrue(FlameVesselItem.flame(vessel) == null && flame.isAlive(), "Rejected captures preserve source and empty vessel");
            flame.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void shotsSpendQiOnceKeepEssenceAndRespectCooldown(GameTestHelper h) {
        try (var user = new User(h)) {
            FlameKind[] kinds = {FlameKind.CRIMSON, FlameKind.EARTH, FlameKind.WOOD, FlameKind.SOLAR,
                    FlameKind.NETHER, FlameKind.THUNDER, FlameKind.DRAGON, FlameKind.HEAVENLY};
            int[] costs = {50, 150, 300, 500, 800, 1500, 2500, 4000};
            java.util.function.Supplier<List<FlameShotEntity>> ownShots = () -> h.getLevel()
                    .getEntitiesOfClass(FlameShotEntity.class, user.player.getBoundingBox().inflate(8))
                    .stream().filter(shot -> shot.getOwner() == user.player).toList();
            try {
                for (int i = 0; i < kinds.length; i++) {
                    FlameKind kind = kinds[i];
                    int cost = costs[i];
                    h.assertTrue(FlameVesselItem.shotCost(kind) == cost,
                            "Shot cost is ten percent of its flame grade's base maximum Qi: " + kind);
                    var vessel = new ItemStack(ModItems.FLAME_VESSEL.get());
                    UUID id = UUID.randomUUID();
                    FlameVesselItem.fill(vessel, kind, id);
                    var savedData = vessel.get(DataComponents.CUSTOM_DATA).copyTag();
                    user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                    user.player.setGameMode(GameType.SURVIVAL);
                    user.player.getCooldowns().removeCooldown(vessel.getItem());

                    ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(cost - 1));
                    h.assertTrue(!FlameVesselItem.shoot(user.player, vessel)
                                    && ModAttachments.getData(user.player).qi() == cost - 1 && ownShots.get().isEmpty()
                                    && !user.player.getCooldowns().isOnCooldown(vessel.getItem())
                                    && savedData.equals(vessel.get(DataComponents.CUSTOM_DATA).copyTag()),
                            "One Qi below the cost preserves Qi, vessel data and cooldown without spawning: " + kind);

                    ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(cost));
                    h.assertTrue(FlameVesselItem.shoot(user.player, vessel),
                            "The exact Qi budget fires a real projectile: " + kind);
                    var shots = ownShots.get();
                    h.assertTrue(ModAttachments.getData(user.player).qi() == 0 && shots.size() == 1
                                    && shots.getFirst().kind() == kind
                                    && user.player.getCooldowns().isOnCooldown(vessel.getItem())
                                    && FlameVesselItem.flame(vessel) == kind && id.equals(FlameVesselItem.identity(vessel))
                                    && savedData.equals(vessel.get(DataComponents.CUSTOM_DATA).copyTag()),
                            "One successful shot spends the exact cost once and keeps the original essence: " + kind);

                    // Refill enough Qi so only the existing cooldown can reject this second shot.
                    ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(cost));
                    h.assertTrue(!FlameVesselItem.shoot(user.player, vessel)
                                    && ModAttachments.getData(user.player).qi() == cost && ownShots.get().size() == 1
                                    && savedData.equals(vessel.get(DataComponents.CUSTOM_DATA).copyTag()),
                            "Cooldown blocks a second affordable shot without charging Qi or spawning: " + kind);

                    user.player.getCooldowns().removeCooldown(vessel.getItem());
                    user.player.setGameMode(GameType.CREATIVE);
                    ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(0));
                    h.assertTrue(FlameVesselItem.shoot(user.player, vessel)
                                    && ModAttachments.getData(user.player).qi() == 0 && ownShots.get().size() == 2
                                    && ownShots.get().stream().allMatch(shot -> shot.kind() == kind)
                                    && savedData.equals(vessel.get(DataComponents.CUSTOM_DATA).copyTag()),
                            "Creative mode fires the matching flame without Qi or essence consumption: " + kind);
                    h.assertTrue(!FlameVesselItem.shoot(user.player, vessel) && ownShots.get().size() == 2
                                    && ModAttachments.getData(user.player).qi() == 0,
                            "Creative shots also respect the shared cooldown: " + kind);
                    ownShots.get().forEach(Entity::discard);
                }
            } finally {
                ownShots.get().forEach(Entity::discard);
                user.player.getCooldowns().removeCooldown(ModItems.FLAME_VESSEL.get());
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void vesselChargesUntilReleaseAndCapsPowerForEveryFlame(GameTestHelper h) {
        try (var user = new User(h)) {
            var item = (FlameVesselItem) ModItems.FLAME_VESSEL.get();
            for (FlameKind kind : FlameKind.values()) {
                if (!kind.special()) continue;
                var vessel = new ItemStack(item);
                UUID id = UUID.randomUUID();
                FlameVesselItem.fill(vessel, kind, id);
                user.player.setItemInHand(InteractionHand.OFF_HAND, vessel);
                for (int ticks : new int[] {0, 4, 47, 90, 100}) {
                    int expectedMultiplier = ticks <= 4 ? 1 : ticks == 47 ? 2 : 3;
                    int cost = FlameVesselItem.shotCost(kind) * expectedMultiplier;
                    user.player.getCooldowns().removeCooldown(item);
                    ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(cost));
                    h.assertTrue(item.use(h.getLevel(), user.player, InteractionHand.OFF_HAND).getResult().consumesAction()
                                    && user.player.isUsingItem() && ModAttachments.getData(user.player).qi() == cost,
                            "Starting offhand charge retains Qi and starts item use: " + kind);
                    h.assertTrue(h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                                    user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player).isEmpty(),
                            "Holding use does not fire early");
                    user.player.releaseAfter(ticks);
                    var shots = h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                            user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player);
                    try {
                        h.assertTrue(shots.size() == 1 && shots.getFirst().kind() == kind
                                        && shots.getFirst().powerMultiplier() == expectedMultiplier
                                        && ModAttachments.getData(user.player).qi() == 0 && !user.player.isUsingItem()
                                        && user.player.getCooldowns().isOnCooldown(item)
                                        && id.equals(FlameVesselItem.identity(vessel)),
                                "Release fires exactly one scaled shot, charges once and retains essence: " + kind + "/" + ticks);
                        user.player.releaseUsingItem();
                        h.assertTrue(h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                                        user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player).size() == 1,
                                "Repeated release cannot duplicate a shot");
                    } finally { shots.forEach(Entity::discard); }
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargedVesselRechecksQiHeldStackAndLivingStateAtRelease(GameTestHelper h) {
        try (var user = new User(h)) {
            var item = (FlameVesselItem) ModItems.FLAME_VESSEL.get();
            for (int scenario = 0; scenario < 6; scenario++) {
                var vessel = new ItemStack(item);
                FlameVesselItem.fill(vessel, FlameKind.CRIMSON, UUID.randomUUID());
                user.player.setGameMode(GameType.SURVIVAL);
                user.player.setHealth(user.player.getMaxHealth());
                user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                user.player.getCooldowns().removeCooldown(item);
                ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(150));
                item.use(h.getLevel(), user.player, InteractionHand.MAIN_HAND);
                switch (scenario) {
                    case 0 -> ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(149));
                    case 1 -> user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel.copy());
                    case 2 -> FlameVesselItem.fill(vessel, null, null);
                    case 3 -> user.player.setGameMode(GameType.SPECTATOR);
                    case 4 -> user.player.setHealth(0);
                    case 5 -> user.player.stopUsingItem();
                }
                int before = ModAttachments.getData(user.player).qi();
                if (user.player.isUsingItem()) user.player.releaseAfter(90);
                else item.releaseUsing(vessel, h.getLevel(), user.player, 71910);
                h.assertTrue(ModAttachments.getData(user.player).qi() == before
                                && !user.player.getCooldowns().isOnCooldown(item)
                                && h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                                    user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player).isEmpty(),
                        "Invalid or unaffordable release spends nothing and creates no projectile: " + scenario);
            }
            user.player.setHealth(user.player.getMaxHealth());
            user.player.setGameMode(GameType.CREATIVE);
            var vessel = new ItemStack(item);
            FlameVesselItem.fill(vessel, FlameKind.CRIMSON, UUID.randomUUID());
            user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
            ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(0));
            item.use(h.getLevel(), user.player, InteractionHand.MAIN_HAND);
            user.player.releaseAfter(90);
            var shots = h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                    user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player);
            try {
                h.assertTrue(shots.size() == 1 && shots.getFirst().charge() == 1
                                && ModAttachments.getData(user.player).qi() == 0,
                        "Creative charging keeps the Qi exemption");
            } finally { shots.forEach(Entity::discard); }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void flarePlacesMatchingFireAndProtectionCanCancel(GameTestHelper h) {
        try (var user = new User(h)) {
            var shot = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), h.getLevel());
            shot.setOwner(user.player); shot.setKind(FlameKind.WOOD);
            BlockPos support = h.absolutePos(new BlockPos(3, 2, 4));
            h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            BlockPos pos = support.above();
            java.util.function.Consumer<BlockEvent.EntityPlaceEvent> protection = event -> {
                if (event.getEntity() == user.player && event.getPos().equals(pos)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protection);
            try {
                h.assertTrue(!shot.placeFire(pos, Direction.UP) && h.getLevel().getBlockState(pos).isAir(),
                        "Cancelled placement rolls back projectile fire");
            } finally { NeoForge.EVENT_BUS.unregister(protection); }
            h.assertTrue(shot.placeFire(pos, Direction.UP) && h.getLevel().getBlockState(pos).is(FlameKind.WOOD.block()),
                    "Flare creates vessel's actual forge fire");
            h.assertTrue(!shot.placeFire(pos, Direction.UP), "Flare cannot overwrite occupied blocks");
            user.player.getAbilities().mayBuild = false;
            BlockPos other = pos.east(); h.getLevel().setBlockAndUpdate(other.below(), Blocks.STONE.defaultBlockState());
            h.assertTrue(!shot.placeFire(other, Direction.UP), "Adventure/build protection prevents fire");
            var tag = shot.saveWithoutId(new CompoundTag());
            var loaded = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), h.getLevel()); loaded.load(tag);
            h.assertTrue(loaded.kind() == FlameKind.WOOD, "Projectile saves its flame kind");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void survivalVesselRecipeAndInvalidDataStaySafe(GameTestHelper h) {
        var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY,
                new ItemStack(Items.GLASS), ItemStack.EMPTY, new ItemStack(Items.GLASS),
                ItemStack.EMPTY, new ItemStack(Items.GLASS), ItemStack.EMPTY));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        ItemStack vessel = recipe.value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(recipe.id().equals(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "flame_vessel"))
                && vessel.is(ModItems.FLAME_VESSEL.get()) && vessel.getMaxStackSize() == 1 && FlameVesselItem.flame(vessel) == null,
                "Survival recipe creates one empty nonstacking vessel");
        var tag = new CompoundTag(); var unknown = new CompoundTag(); unknown.putString("kind", "missing_fire");
        tag.put(FlameVesselItem.DATA, unknown); tag.putString("unrelated", "preserved");
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        h.assertTrue(FlameVesselItem.flame(vessel) == null, "Unknown flame IDs cannot activate arbitrary fire");
        FlameVesselItem.fill(vessel, FlameKind.CRIMSON, UUID.randomUUID()); FlameVesselItem.fill(vessel, null, null);
        h.assertTrue(vessel.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated").equals("preserved"), "Release preserves unrelated item metadata");
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void fullChargeAutoLaunchesOnceAfterCoreExposure(GameTestHelper h) {
        try (var user = new User(h)) {
            var item = (FlameVesselItem)ModItems.FLAME_VESSEL.get();
            var vessel = new ItemStack(item);
            FlameVesselItem.fill(vessel, FlameKind.CRIMSON, UUID.randomUUID());
            user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
            ModAttachments.setData(user.player,ModAttachments.getData(user.player).withQi(300));
            item.use(h.getLevel(), user.player, InteractionHand.MAIN_HAND);
            item.onUseTick(h.getLevel(),user.player,vessel,72000-89);
            h.assertTrue(user.player.isUsingItem() && h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                    user.player.getBoundingBox().inflate(8), shot -> shot.getOwner()==user.player).isEmpty(),
                    "Final core exposure happens before launch");
            item.onUseTick(h.getLevel(),user.player,vessel,72000-90);
            item.onUseTick(h.getLevel(),user.player,vessel,72000-91);
            user.player.releaseUsingItem();
            var shots=h.getLevel().getEntitiesOfClass(FlameShotEntity.class,user.player.getBoundingBox().inflate(8),shot -> shot.getOwner()==user.player);
            try {
                h.assertTrue(shots.size()==1 && shots.getFirst().charge()==1 && !user.player.isUsingItem()
                        && ModAttachments.getData(user.player).qi()==150,
                        "Full 4.5-second charge fires and pays once; subsequent use/release cannot duplicate it");
                h.assertTrue(shots.getFirst().getDeltaMovement().length()<.6,
                        "Charged projectile launches slowly before acceleration");
            } finally { shots.forEach(Entity::discard); }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargeEffectsWaitSixTicksAndRequireCurrentShotQi(GameTestHelper h) {
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            for (int held = 0; held < 6; held++)
                h.assertTrue(!FlameVesselItem.chargeEffectsReady(kind, held, Integer.MAX_VALUE, true),
                        "Even creative charges show no effects before 0.3 seconds");
            for (int held : new int[] {6, 47, 89, 90}) {
                int cost = FlameVesselItem.chargedShotCost(kind, FlameVesselItem.chargeFraction(held));
                h.assertTrue(FlameVesselItem.chargeEffectsReady(kind, held, cost, false)
                                && !FlameVesselItem.chargeEffectsReady(kind, held, cost - 1, false)
                                && FlameVesselItem.chargeEffectsReady(kind, held, 0, true),
                        "VFX use current charge cost for every flame; creative keeps its Qi exemption");
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void unaffordableChargeStopsBeforeEffectsAndCannotFireOnRelease(GameTestHelper h) {
        try (var user = new User(h)) {
            var item = (FlameVesselItem) ModItems.FLAME_VESSEL.get();
            var vessel = new ItemStack(item);
            FlameVesselItem.fill(vessel, FlameKind.CRIMSON, UUID.randomUUID());
            user.player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
            for (int held : new int[] {6, 47, 90}) {
                ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(150));
                item.use(h.getLevel(), user.player, InteractionHand.MAIN_HAND);
                int qi = FlameVesselItem.chargedShotCost(FlameKind.CRIMSON,
                        FlameVesselItem.chargeFraction(held)) - 1;
                ModAttachments.setData(user.player, ModAttachments.getData(user.player).withQi(qi));
                item.onUseTick(h.getLevel(), user.player, vessel, 72000 - held);
                h.assertTrue(!user.player.isUsingItem(), "Server stops unaffordable charge immediately");
                item.releaseUsing(vessel, h.getLevel(), user.player, 72000 - held);
                h.assertTrue(ModAttachments.getData(user.player).qi() == qi
                                && !user.player.getCooldowns().isOnCooldown(item)
                                && h.getLevel().getEntitiesOfClass(FlameShotEntity.class,
                                    user.player.getBoundingBox().inflate(8), shot -> shot.getOwner() == user.player).isEmpty(),
                        "Aborted charge spends no Qi, starts no cooldown, and cannot fire on late release");
            }
        }
        h.succeed();
    }

}
