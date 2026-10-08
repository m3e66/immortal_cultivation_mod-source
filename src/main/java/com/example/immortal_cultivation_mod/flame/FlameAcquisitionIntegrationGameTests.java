package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercise survival source events with actual registered entities, trees and structure templates. */
@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameAcquisitionIntegrationGameTests {
    // Source claims occur asynchronously; concurrent fixtures must reserve sites before enqueueing.
    private static final java.util.Set<String> RESERVED_SOURCE_KEYS = new java.util.HashSet<>();
    @GameTest(template = "fate_test")
    public static void allDeathSourcesProduceFlamesThatSurvivalPlayersCanCapture(GameTestHelper h) {
        var level = h.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (var biomeKey : List.of(Biomes.BADLANDS, Biomes.NETHER_WASTES)) {
            var biome = biomes.get(biomeKey.location());
            h.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                            .anyMatch(spawn -> spawn.type == ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get()),
                    "The actual modified survival biome can spawn the Crimson source beast: " + biomeKey.location());
        }
        var npc = ModEntities.JINDAN_CULTIVATOR.get().create(level);
        NpcProfile.ensure(npc, npc.npcArchetype());
        var profile = npc.getPersistentData().getCompound("ImmortalCultivationNpc");
        profile.putString("Realm", CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        var roots = new ListTag(); roots.add(StringTag.valueOf(SpiritRoots.METAL));
        profile.put("SpiritRoots", roots);
        var undead = EntityType.ZOMBIE.create(level);
        undead.getPersistentData().putLong(UndeadControl.TAG_GRUDGE, 12_500);
        LivingEntity[] victims = {ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(level), npc,
                undead, ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(level)};
        FlameKind[] kinds = {FlameKind.CRIMSON, FlameKind.SOLAR, FlameKind.NETHER, FlameKind.DRAGON};
        BlockPos pos = h.absolutePos(new BlockPos(4, 3, 4));
        clearAir(level, pos, 3);
        try {
            for (int i = 0; i < victims.length; i++) {
                var victim = victims[i];
                int denominator = FlameAcquisition.deathChance(victim, kinds[i]);
                UUID id = null;
                for (long attempt = 0; attempt < 100_000; attempt++) {
                    UUID candidate = new UUID(pos.asLong() + i, attempt);
                    if (FlameAcquisition.roll(level.getSeed(), "death:" + kinds[i] + ":" + candidate, denominator)) {
                        id = candidate; break;
                    }
                }
                h.assertTrue(id != null, "Fixture finds a real successful death roll for " + kinds[i]);
                victim.setUUID(id); victim.setPos(Vec3.atBottomCenterOf(pos));
                String key = "death:" + kinds[i] + ":" + id;
                // Real death event subscriber, rather than a direct spawn or eligibility-only assertion.
                NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, level.damageSources().generic()));
                captureSource(h, key, kinds[i], pos);
            }
        } finally { for (var victim : victims) victim.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void undergroundEarthSourceUsesNewChunkHookAndCanBeCaptured(GameTestHelper h) {
        earthSource(h, false);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void exposedMountainMagmaSourceUsesActualSurfaceBlock(GameTestHelper h) {
        earthSource(h, true);
    }

    private static void earthSource(GameTestHelper h, boolean mountain) {
        var level = h.getLevel();
        ChunkPos pos = winningChunk(h, "earth", 64, null);
        level.setChunkForced(pos.x, pos.z, true);
        var chunk = level.getChunk(pos.x, pos.z);
        BlockPos support = new BlockPos(pos.getMinBlockX(), mountain ? level.getSeaLevel() + 26
                : level.getMinBuildHeight() + 10, pos.getMinBlockZ());
        if (!mountain) {
            level.setBlockAndUpdate(support.below(), Blocks.STONE.defaultBlockState());
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL)
                level.setBlockAndUpdate(support.relative(direction), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(support, mountain ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.LAVA.defaultBlockState());
        for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(support.above(y), Blocks.AIR.defaultBlockState());
        String key = FlameAcquisition.chunkKey("earth", pos);
        processNewChunk(level, chunk);
        // Force the fixture chunk to become entity-ticking before querying its natural spawn.
        h.runAfterDelay(30, () -> {
            try { captureSource(h, key, FlameKind.EARTH, support.above()); h.succeed(); }
            finally { level.setChunkForced(pos.x, pos.z, false); }
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void lightningAtActualGeneratedMatureSpiritwoodProducesCapturableWood(GameTestHelper h) {
        var level = h.getLevel();
        ChunkPos pos = winningChunk(h, "wood", 12, null);
        level.setChunkForced(pos.x, pos.z, true);
        level.getChunk(pos.x, pos.z);
        BlockPos base = new BlockPos(pos.getMinBlockX() + 8, level.getSeaLevel() + 4, pos.getMinBlockZ() + 8);
        var tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "lingmu"));
        h.assertTrue(tree != null, "Survival spiritwood configured feature is registered");
        boolean mature = false;
        for (int seed = 0; seed < 100 && !mature; seed++) {
            clearAir(level, base, 4);
            level.setBlockAndUpdate(base.below(), Blocks.DIRT.defaultBlockState());
            tree.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), base);
            mature = FlameAcquisition.matureSpiritwood(level, base);
        }
        h.assertTrue(mature, "The real generated tree can satisfy the mature trunk and living crown requirement");
        String key = FlameAcquisition.chunkKey("wood", pos);
        // Let the loaded fixture reach FULL before generating ordinary lightning.
        h.runAfterDelay(10, () -> {
            var bolt = EntityType.LIGHTNING_BOLT.create(level);
            bolt.setVisualOnly(true); bolt.setPos(Vec3.atCenterOf(base.above(7)));
            h.assertTrue(level.addFreshEntity(bolt), "Ordinary lightning enters through the actual entity-join event");
            h.runAfterDelay(30, () -> {
                try { captureSource(h, key, FlameKind.WOOD, base.above(5)); h.succeed(); }
                finally { bolt.discard(); level.setChunkForced(pos.x, pos.z, false); }
            });
        });
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void shippedDeadRuinCanHostSolarFlameWithoutInventedMetalOrFire(GameTestHelper h) {
        structureSource(h, "dead_ruin1", FlameKind.SOLAR);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void suppliedDaoistTempleCanHostSolarFlameWithoutInventedMetalOrFire(GameTestHelper h) {
        structureSource(h, "daoist_temple", FlameKind.SOLAR);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void shippedSmallTombCanHostCapturableNetherFlame(GameTestHelper h) {
        structureSource(h, "tomb1", FlameKind.NETHER);
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void shippedLargeTombCanHostCapturableNetherFlame(GameTestHelper h) {
        structureSource(h, "tomb2", FlameKind.NETHER);
    }

    private static void structureSource(GameTestHelper h, String name, FlameKind kind) {
        var level = h.getLevel();
        var id = ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, name);
        boolean tomb = kind == FlameKind.NETHER;
        String prefix = (tomb ? "tomb:" : "ruin:") + id;
        ChunkPos pos = winningChunk(h, prefix, tomb ? 12 : 16, id);
        level.setChunkForced(pos.x, pos.z, true);
        var chunk = level.getChunk(pos.x, pos.z);
        var manager = level.getStructureManager();
        var template = manager.getOrCreate(id);
        BlockPos origin = new BlockPos(pos.getMinBlockX(), level.getSeaLevel() + 8, pos.getMinBlockZ());
        var element = StructurePoolElement.single(id.toString()).apply(StructureTemplatePool.Projection.RIGID);
        var bounds = element.getBoundingBox(manager, origin, Rotation.NONE);
        var settings = new StructurePlaceSettings().setBoundingBox(new net.minecraft.world.level.levelgen.structure.BoundingBox(
                pos.getMinBlockX(), bounds.minY(), pos.getMinBlockZ(), pos.getMaxBlockX(), bounds.maxY(), pos.getMaxBlockZ()));
        h.assertTrue(template.placeInWorld(level, origin, origin, settings, RandomSource.create(1), 2),
                "Actual packaged survival structure template places: " + name);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var piece = new PoolElementStructurePiece(manager, element, origin, element.getGroundLevelDelta(),
                Rotation.NONE, bounds, JigsawStructure.DEFAULT_LIQUID_SETTINGS);
        var start = new StructureStart(structure, pos, 0, new PiecesContainer(List.of(piece)));
        var oldStarts = new HashMap<>(chunk.getAllStarts());
        var starts = new HashMap<>(oldStarts); starts.put(structure, start); chunk.setAllStarts(starts);
        String key = prefix + ":" + pos.toLong();
        processNewChunk(level, chunk);
        h.runAfterDelay(30, () -> {
            try { captureSource(h, key, kind, origin); h.succeed(); }
            finally { chunk.setAllStarts(oldStarts); level.setChunkForced(pos.x, pos.z, false); }
        });
    }

    private static ChunkPos winningChunk(GameTestHelper h, String kind, int denominator, ResourceLocation structure) {
        var level = h.getLevel();
        int x = (h.absolutePos(BlockPos.ZERO).getX() >> 4) + 256;
        int z = (h.absolutePos(BlockPos.ZERO).getZ() >> 4) + 256;
        for (int attempt = 0; attempt < 8192; attempt++) {
            var pos = new ChunkPos(x + attempt % 128, z + attempt / 128);
            String key = structure == null ? FlameAcquisition.chunkKey(kind, pos) : kind + ":" + pos.toLong();
            if (!RESERVED_SOURCE_KEYS.contains(key) && !FlameAcquisitionData.get(level).contains(key)
                    && FlameAcquisition.roll(level.getSeed(), key, denominator)) {
                RESERVED_SOURCE_KEYS.add(key);
                return pos;
            }
        }
        throw new AssertionError("No unused successful source fixture for " + kind);
    }

    private static void processNewChunk(ServerLevel level, LevelChunk chunk) {
        NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, true));
    }

    private static void clearAir(ServerLevel level, BlockPos base, int radius) {
        for (var pos : BlockPos.betweenClosed(base.offset(-radius, 0, -radius), base.offset(radius, 12, radius)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void captureSource(GameTestHelper h, String key, FlameKind kind, BlockPos near) {
        var flames = h.getLevel().getEntitiesOfClass(SpiritFlameEntity.class, new AABB(near).inflate(48),
                flame -> key.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY)));
        h.assertTrue(flames.size() == 1 && flames.getFirst().kind() == kind,
                "Survival source creates exactly one actual matching essence: " + key
                        + " found=" + flames.size() + " claimed=" + FlameAcquisitionData.get(h.getLevel()).contains(key));
        var flame = flames.getFirst();
        try (var user = new Capturer(h.getLevel())) {
            user.player.setPos(flame.position());
            var bottle = new ItemStack(ModItems.FLAME_VESSEL.get());
            user.player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
            h.assertTrue(FlameVesselItem.capture(user.player, bottle, flame)
                            && FlameVesselItem.flame(bottle) == kind && flame.isRemoved(),
                    "A survival player captures the naturally acquired " + kind + " into a real empty vessel");
        } finally { flame.discard(); }
    }

    private static final class Capturer implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        Capturer(ServerLevel level) {
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "natural-flame-capture"),
                    ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND); channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { }
            };
            player.setGameMode(GameType.SURVIVAL);
        }
        @Override public void close() { player.discard(); channel.close(); }
    }
}
