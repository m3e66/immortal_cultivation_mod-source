package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameCombatGameTests {
    private static final FlameKind[] KINDS = {FlameKind.CRIMSON, FlameKind.EARTH, FlameKind.WOOD,
            FlameKind.SOLAR, FlameKind.NETHER, FlameKind.THUNDER, FlameKind.DRAGON, FlameKind.HEAVENLY};
    private static final float[] SHOT_DAMAGE = {37.5F, 105, 225, 375, 600, 1200, 1950, 3300};
    private static final float[] CONTACT_DAMAGE = {12.5F, 35, 75, 125, 200, 400, 650, 1100};
    private static final String CONTACT_STAMP = "icmSpiritFlameContactTick";

    private static final class User implements AutoCloseable {
        final ServerPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;

        User(GameTestHelper h) {
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "flame-" + UUID.randomUUID().toString().substring(0, 8)),
                    net.minecraft.server.level.ClientInformation.createDefault());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 1))));
        }

        @Override public void close() { player.discard(); channel.close(); }
    }

    private static void clear(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 2, 1), new BlockPos(6, 5, 6)))
            h.setBlock(pos, Blocks.AIR);
    }

    private static Cow target(GameTestHelper h) {
        var cow = h.spawn(EntityType.COW, new BlockPos(5, 2, 5));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        cow.setHealth(200);
        return cow;
    }

    private static FlameShotEntity shot(GameTestHelper h, ServerPlayer owner, FlameKind kind) {
        var shot = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), h.getLevel());
        shot.setOwner(owner);
        shot.setKind(kind);
        shot.setPos(owner.position().add(0, 1, 0));
        h.getLevel().addFreshEntity(shot);
        return shot;
    }

    private static void contact(GameTestHelper h, FlameKind kind, BlockPos pos, LivingEntity entity) {
        ((SpiritFlameBlock) kind.block()).entityInside(kind.block().defaultBlockState(), h.getLevel(), pos, entity);
    }

    private static SpiritFlameBlockEntity place(GameTestHelper h, BlockPos pos, FlameKind kind) {
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, kind.block().defaultBlockState());
        var entity = h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity instanceof SpiritFlameBlockEntity, "Every placed special flame has its lifetime block entity");
        return (SpiritFlameBlockEntity) entity;
    }

    private static SpiritFlameBlockEntity reload(GameTestHelper h, BlockPos pos, CompoundTag saved) {
        var loaded = new SpiritFlameBlockEntity(pos, h.getLevel().getBlockState(pos));
        loaded.loadWithComponents(saved, h.getLevel().registryAccess());
        loaded.setLevel(h.getLevel());
        h.getLevel().setBlockEntity(loaded);
        return loaded;
    }

    private static void tick(GameTestHelper h, SpiritFlameBlockEntity flame) {
        SpiritFlameBlockEntity.serverTick(h.getLevel(), flame.getBlockPos(), flame.getBlockState(), flame);
    }

    private static void noPlacedFire(GameTestHelper h, LivingEntity target, String reason) {
        h.assertTrue(h.getLevel().getBlockState(target.blockPosition()).isAir(), reason);
    }

    @GameTest(template = "fate_test")
    public static void chargedBlastHitsAreaOnceAndProtectsAlliesAndPets(GameTestHelper h) {
        clear(h);
        var direct = target(h);
        var near = target(h);
        near.setPos(direct.position().add(-1.5, 0, 0));
        var far = target(h);
        far.setPos(direct.position().add(-4, 0, -4));
        var ally = target(h);
        ally.setPos(direct.position().add(0, 0, -1.5));
        var pet = h.spawn(EntityType.WOLF, new BlockPos(4, 2, 4));
        pet.setNoAi(true);
        var scoreboard = h.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("blast_" + UUID.randomUUID().toString().substring(0, 8));
        try (var owner = new User(h)) {
            scoreboard.addPlayerToTeam(owner.player.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
            pet.setTame(true, true);
            pet.setOwnerUUID(owner.player.getUUID());
            float petHealth = pet.getHealth();
            var projectile = shot(h, owner.player, FlameKind.CRIMSON);
            projectile.setCharge(1);
            h.assertTrue(!projectile.canDamageTarget(owner.player), "Caster cannot be hit by own blast");
            projectile.onHit(new EntityHitResult(direct));
            h.assertTrue(Math.abs(direct.getHealth() - 87.5F) < .001,
                    "Direct target takes exactly one full charged hit");
            h.assertTrue(Math.abs(near.getHealth() - direct.getHealth()) < .001F
                            && SpiritFlameBurnEffect.kind(near) == FlameKind.CRIMSON,
                    "Nearby target takes the same full area damage and matching lingering burn");
            h.assertTrue(far.getHealth() == 200 && !far.isOnFire(), "Outside blast radius remains safe");
            h.assertTrue(ally.getHealth() == 200 && !ally.isOnFire() && ally.getDeltaMovement().lengthSqr() == 0
                            && pet.getHealth() == petHealth && !pet.isOnFire() && pet.getDeltaMovement().lengthSqr() == 0,
                    "Allies and owned pets receive neither damage, burning nor knockback");
            float previous = near.getHealth();
            projectile.onHit(new EntityHitResult(near));
            h.assertTrue(near.getHealth() == previous, "Repeated collision cannot trigger another explosion");
            noPlacedFire(h, direct, "Entity explosions never place ground fire");
        } finally {
            scoreboard.removePlayerTeam(team);
            direct.discard(); near.discard(); far.discard(); ally.discard(); pet.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test", batch = "flame_full_radius", timeoutTicks = 200)
    public static void chargedDamageCoversEntireRealmSphereAndExactBoundaryAtEveryCharge(GameTestHelper h) {
        // Isolate the largest real damage radius from other GameTest creatures and terrain.
        var block = h.absolutePos(new BlockPos(3, 2, 3)).atY(200);
        var hitPosition = Vec3.atCenterOf(block);
        var center = hitPosition.add(0, .05, 0);
        // A newly generated FULL chunk is not immediately accessible to entity queries. Hold the
        // complete test area until ticket visibility updates, then release only this fixture's tickets.
        var forced = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        var fixtureChunks = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for (int x = (block.getX() - 61) >> 4; x <= (block.getX() + 61) >> 4; x++) {
            for (int z = (block.getZ() - 61) >> 4; z <= (block.getZ() + 61) >> 4; z++) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                fixtureChunks.add(chunk);
                if (h.getLevel().setChunkForced(x, z, true)) forced.add(chunk);
            }
        }
        h.startSequence().thenWaitUntil(() -> {
            for (var chunk : fixtureChunks)
                h.assertTrue(h.getLevel().areEntitiesLoaded(chunk.toLong())
                                && h.getLevel().isNaturalSpawningAllowed(chunk),
                        "Distant test chunks must be visible to the entity index before combat starts");
        }).thenExecute(() -> {
            try {
                checkEntireRealmSphere(h, block, hitPosition, center);
            } finally {
                forced.forEach(chunk -> h.getLevel().setChunkForced(chunk.x, chunk.z, false));
            }
        }).thenSucceed();
    }

    private static void checkEntireRealmSphere(GameTestHelper h, BlockPos block, Vec3 hitPosition, Vec3 center) {
        try (var owner = new User(h)) {
            owner.player.getAbilities().mayBuild = false;
            for (var kind : FlameKind.values()) {
                for (float charge : new float[]{.1F, .5F, 1}) {
                    var projectile = shot(h, owner.player, kind);
                    projectile.setCharge(charge);
                    float radius = projectile.destructionRadius();
                    h.assertTrue(projectile.blastRadius() == radius,
                            "Damage and terrain use one charge-scaled realm radius");
                    Vec3[] positions = {
                            center.add(radius * .25, 0, 0),
                            center.add(radius * .8, 0, 0),
                            center.add(radius, 0, 0),
                            center.add(0, radius, 0),
                            center.add(radius + .1, 0, 0),
                            center.add(radius * .8, 0, radius * .8)
                    };
                    var creatures = new java.util.ArrayList<Cow>();
                    try {
                        for (var position : positions) {
                            var creature = target(h);
                            creature.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20000);
                            creature.setHealth(20000);
                            creature.setPos(position);
                            creatures.add(creature);
                        }
                        projectile.onHit(new BlockHitResult(hitPosition, Direction.UP, block, false));
                        float expected = kind.shotDamage() * projectile.powerMultiplier();
                        for (int i = 0; i < 4; i++) {
                            var creature = creatures.get(i);
                            h.assertTrue(Math.abs(20000 - creature.getHealth() - expected) < .01F
                                            && (!kind.special() || SpiritFlameBurnEffect.kind(creature) == kind),
                                    "Every creature inside or exactly on the spherical radius gets full damage and burn: "
                                            + kind + " / " + charge + " / " + i
                                            + " health=" + creature.getHealth() + " expectedDamage=" + expected
                                            + " burn=" + SpiritFlameBurnEffect.kind(creature)
                                            + " distance=" + Math.sqrt(creature.distanceToSqr(center))
                                            + " indexed=" + h.getLevel().getEntities(projectile,
                                                    new net.minecraft.world.phys.AABB(center, center).inflate(radius + 1)).contains(creature));
                        }
                        for (int i = 4; i < creatures.size(); i++)
                            h.assertTrue(creatures.get(i).getHealth() == 20000 && !creatures.get(i).isOnFire(),
                                    "Outside-radius and outside-sphere cube corners remain unharmed");
                    } finally {
                        projectile.discard();
                        creatures.forEach(Cow::discard);
                    }
                }
            }
        }
    }

    @GameTest(template = "fate_test")
    public static void flameVesselTerrainChoiceKeepsCombatAndUsesIndependentCategory(GameTestHelper h) {
        clear(h);
        var enemy = target(h);
        BlockPos terrain = enemy.blockPosition().below();
        BlockPos support = h.absolutePos(new BlockPos(3, 1, 3));
        h.getLevel().setBlockAndUpdate(terrain, Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        try (var owner = new User(h)) {
            BlockBreakingSettings.setDisabledMask(owner.player,
                    BlockBreakingSettings.Category.FLAME_VESSELS.bit());
            var charged = shot(h, owner.player, FlameKind.CRIMSON);
            charged.setCharge(1);
            charged.onHit(new EntityHitResult(enemy));
            h.assertTrue(Math.abs(enemy.getHealth() - 87.5F) < .001
                            && SpiritFlameBurnEffect.kind(enemy) == FlameKind.CRIMSON,
                    "Disabling Flame Vessel terrain keeps the charged hit and matching burn");
            h.assertTrue(h.getLevel().getBlockState(terrain).is(Blocks.DIRT),
                    "The real charged projectile preserves terrain when its category is disabled");
            var tapped = shot(h, owner.player, FlameKind.CRIMSON);
            tapped.onHit(new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0),
                    Direction.UP, support, false));
            h.assertTrue(tapped.isRemoved() && h.getLevel().getBlockState(support.above()).isAir(),
                    "Disabled Flame Vessel terrain also prevents projectile fire placement");

            BlockBreakingSettings.setDisabledMask(owner.player, BlockBreakingSettings.Category.SPELLS.bit());
            var enabled = shot(h, owner.player, FlameKind.CRIMSON);
            enabled.setCharge(1);
            enabled.onHit(new BlockHitResult(Vec3.atCenterOf(terrain).add(0, .5, 0),
                    Direction.UP, terrain, false));
            h.assertTrue(h.getLevel().getBlockState(terrain).isAir(),
                    "Flame Vessel craters stay enabled independently from ordinary spell terrain damage");
        } finally {
            enemy.discard();
            h.getLevel().setBlockAndUpdate(terrain, Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargedBlastCraterHonorsBlockProtectionAndPreservesContainers(GameTestHelper h) {
        clear(h);
        BlockPos center = h.absolutePos(new BlockPos(4, 2, 4));
        BlockPos protectedPos = center.east();
        BlockPos chest = center.north();
        BlockPos bedrock = center.south();
        h.getLevel().setBlockAndUpdate(center, Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(protectedPos, Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        h.getLevel().setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
        int[] denied = {0};
        Consumer<BlockEvent.BreakEvent> protect = event -> {
            if (event.getPos().equals(protectedPos)) { denied[0]++; event.setCanceled(true); }
        };
        NeoForge.EVENT_BUS.addListener(protect);
        var owner = new User(h);
        var projectile = shot(h, owner.player, FlameKind.CRIMSON);
        projectile.setCharge(1);
        projectile.onHit(new BlockHitResult(Vec3.atCenterOf(center).add(0, .5, 0), Direction.UP, center, false));
        try {
            h.assertTrue(h.getLevel().getBlockState(center).isAir(), "Charged block impact destroys terrain at impact");
            h.assertTrue(h.getLevel().getBlockState(protectedPos).is(Blocks.DIRT) && denied[0] == 1,
                    "Canceled block protection remains intact and is evaluated only once per block");
            h.assertTrue(h.getLevel().getBlockState(chest).is(Blocks.CHEST)
                            && h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK),
                    "Container contents and unbreakable terrain survive the crater");
        } finally {
            owner.close();
            NeoForge.EVENT_BUS.unregister(protect);
            for (var pos : new BlockPos[] {center, protectedPos, chest, bedrock})
                h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void realmTerrainRadiiReachFullFootprintWithoutOldTraversalCap(GameTestHelper h) {
        int[] expected = {3, 5, 5, 15, 15, 15, 60, 60, 60};
        int index = 0;
        for (FlameKind kind : FlameKind.values()) {
            h.assertTrue(kind.destructionRadius() == expected[index++], "Each flame maps to its realm's terrain radius");
        }
        for (int radius : new int[] {3, 5, 15, 60}) {
            var columns = new FlameTerrainDestruction.Columns(radius);
            var seen = new java.util.HashSet<BlockPos>();
            while (columns.hasNext()) {
                BlockPos pos = columns.next(BlockPos.ZERO);
                h.assertTrue(Math.abs(pos.getX()) <= radius && Math.abs(pos.getZ()) <= radius && seen.add(pos),
                        "Traversal is bounded and visits every column only once");
            }
            h.assertTrue(seen.size() == (radius * 2 + 1) * (radius * 2 + 1)
                            && seen.contains(new BlockPos(radius, 0, 0)) && seen.contains(new BlockPos(-radius, 0, 0)),
                    "Full terrain footprint reaches both edges, including 60 blocks");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test", batch = "flame_instant_terrain")
    public static void nascentFlameInstantlyClearsFullCraterBeyondTickBudgetAndHonorsFiltering(GameTestHelper h) {
        var owner = new User(h);
        // The 19,683-block fixture exceeds the ordinary 16,384-per-tick terrain budget.
        BlockPos center = h.absolutePos(new BlockPos(3, 2, 3)).atY(h.getLevel().getMaxBuildHeight() - 40);
        var min = center.offset(-13, -13, -13);
        var max = center.offset(13, 13, 13);
        for (var pos : BlockPos.betweenClosed(min, max))
            h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2 | 16 | 32);
        BlockPos far = center.east(40), outside = center.east(61), protectedPos = center.south(40);
        for (var pos : new BlockPos[] {center, far, outside, protectedPos})
            h.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        owner.player.setPos(Vec3.atCenterOf(center).add(0, 1, 0));
        var projectile = shot(h, owner.player, FlameKind.HEAVENLY);
        projectile.setCharge(1);
        Consumer<ExplosionEvent.Detonate> protect = event -> {
            if (event.getExplosion().getDirectSourceEntity() == projectile)
                event.getAffectedBlocks().removeIf(protectedPos::equals);
        };
        NeoForge.EVENT_BUS.addListener(protect);
        try {
                projectile.onHit(new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
                for (var pos : BlockPos.betweenClosed(min, max))
                    h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Entire dense crater is gone before impact returns");
                h.assertTrue(h.getLevel().getBlockState(center).isAir() && h.getLevel().getBlockState(far).isAir(),
                        "Distant terrain is destroyed immediately in the same impact");
                h.assertTrue(h.getLevel().getBlockState(outside).is(Blocks.DIRT), "Outside the 60-block radius remains intact");
                h.assertTrue(h.getLevel().getBlockState(protectedPos).is(Blocks.DIRT),
                        "Explosion-event block filtering still protects distant terrain");
        } finally {
                NeoForge.EVENT_BUS.unregister(protect);
                owner.close();
                for (var pos : new BlockPos[] {center, far, outside, protectedPos})
                    h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                for (var pos : BlockPos.betweenClosed(min, max))
                    h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargedCraterInstantlyErasesWaterLavaFlowAndWaterloggedBlocks(GameTestHelper h) {
        clear(h);
        BlockPos center = h.absolutePos(new BlockPos(4, 2, 4));
        // Keep water and lava nonadjacent: lava reacts in onPlace even when neighbor updates are suppressed.
        BlockPos[] positions = {center, center.east(2), center.north(), center.south(2), center.west().north()};
        var states = new net.minecraft.world.level.block.state.BlockState[] {
                Blocks.WATER.defaultBlockState(), Blocks.LAVA.defaultBlockState(),
                Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 5),
                Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 4),
                Blocks.OAK_SLAB.defaultBlockState().setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true)};
        BlockPos protectedWater = center.west();
        for (int i = 0; i < positions.length; i++) h.getLevel().setBlock(positions[i], states[i], 2 | 16 | 32);
        h.getLevel().setBlock(protectedWater, Blocks.WATER.defaultBlockState(), 2 | 16 | 32);
        for (int i = 0; i < positions.length; i++)
            h.assertTrue(h.getLevel().getBlockState(positions[i]) == states[i], "Liquid fixture retains its intended state: " + states[i]);
        Consumer<BlockEvent.BreakEvent> protection = event -> {
            if (event.getPos().equals(protectedWater)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(protection);
        try (var owner = new User(h)) {
            var projectile = shot(h, owner.player, FlameKind.CRIMSON);
            projectile.setCharge(1);
            projectile.onHit(new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
            for (var pos : positions)
                h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getFluidState(pos).isEmpty(),
                        "Sources, flowing liquids and waterlogged terrain are erased immediately: " + h.getLevel().getBlockState(pos));
            h.assertTrue(h.getLevel().getBlockState(protectedWater).is(Blocks.WATER),
                    "Liquid clearing still honors block protection");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
            for (var pos : positions) h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
            h.getLevel().setBlock(protectedWater, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void canceledChargedExplosionAndDamageCauseNoBurnOrTerrainBypass(GameTestHelper h) {
        clear(h);
        var target = target(h);
        BlockPos dirt = target.blockPosition().below();
        h.getLevel().setBlockAndUpdate(dirt, Blocks.DIRT.defaultBlockState());
        try (var owner = new User(h)) {
            var projectile = shot(h, owner.player, FlameKind.CRIMSON);
            projectile.setCharge(1);
            Consumer<ExplosionEvent.Start> cancel = event -> {
                if (event.getExplosion().getDirectSourceEntity() == projectile) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { projectile.onHit(new EntityHitResult(target)); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire()
                            && h.getLevel().getBlockState(dirt).is(Blocks.DIRT),
                    "Canceled explosion suppresses both damage and terrain destruction");
            var blocked = shot(h, owner.player, FlameKind.CRIMSON);
            blocked.setCharge(1);
            Consumer<LivingIncomingDamageEvent> protect = event -> {
                if (event.getEntity() == target) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protect);
            try { blocked.onHit(new EntityHitResult(target)); }
            finally { NeoForge.EVENT_BUS.unregister(protect); }
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire()
                            && !target.hasEffect(ModEffects.SPIRIT_FLAME_BURN) && target.getDeltaMovement().lengthSqr() == 0,
                    "Denied AoE damage cannot sneak through burning or knockback");
        } finally { target.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargedBlastCoversCreaturesBehindTerrainAndRespectsExplosionEntityFiltering(GameTestHelper h) {
        clear(h);
        var direct = target(h);
        direct.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 4))));
        var covered = target(h);
        covered.setPos(direct.position().add(2, 0, 0));
        var filtered = target(h);
        filtered.setPos(direct.position().add(0, 0, -1));
        for (var pos : BlockPos.betweenClosed(new BlockPos(4, 1, 2), new BlockPos(4, 5, 6)))
            h.setBlock(pos, Blocks.OBSIDIAN);
        try (var owner = new User(h)) {
            var projectile = shot(h, owner.player, FlameKind.CRIMSON);
            projectile.setCharge(1);
            Consumer<ExplosionEvent.Detonate> protect = event -> {
                if (event.getExplosion().getDirectSourceEntity() == projectile)
                    event.getAffectedEntities().remove(filtered);
            };
            NeoForge.EVENT_BUS.addListener(protect);
            try { projectile.onHit(new EntityHitResult(direct)); }
            finally { NeoForge.EVENT_BUS.unregister(protect); }
            h.assertTrue(direct.getHealth() < 200, "Unprotected direct target still takes impact damage");
            h.assertTrue(Math.abs(covered.getHealth() - direct.getHealth()) < .001F && covered.isOnFire(),
                    "Every valid creature within the blast sphere receives full damage, including behind terrain");
            h.assertTrue(filtered.getHealth() == 200 && !filtered.isOnFire()
                            && filtered.getDeltaMovement().lengthSqr() == 0,
                    "Explosion protection can remove targets without bypass through separate direct damage");
        } finally { direct.discard(); covered.discard(); filtered.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void chargedFireballPersistsSizeAndDealsTripleDamageWithoutPlacingFire(GameTestHelper h) {
        clear(h);
        try (var owner = new User(h)) {
            var projectile = shot(h, owner.player, FlameKind.CRIMSON);
            float normalWidth = projectile.getBbWidth();
            projectile.setCharge(1);
            h.assertTrue(Math.abs(projectile.getBbWidth() - normalWidth * 3) < .001F,
                    "Full charge expands the projectile bounds threefold");
            var saved = projectile.saveWithoutId(new CompoundTag());
            var loaded = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), h.getLevel());
            loaded.load(saved);
            h.assertTrue(loaded.charge() == 1 && loaded.kind() == FlameKind.CRIMSON
                            && Math.abs(loaded.getBbWidth() - normalWidth * 3) < .001F,
                    "Charge, kind and size survive projectile save/load");
            saved.remove("FlameCharge");
            loaded.load(saved);
            h.assertTrue(loaded.charge() == 0 && Math.abs(loaded.getBbWidth() - normalWidth) < .001F,
                    "Legacy projectiles load as ordinary flares");
            for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -4}) {
                loaded.setCharge(invalid);
                h.assertTrue(loaded.charge() == 0, "Invalid charge cannot create oversized or NaN geometry");
            }
            loaded.setCharge(12);
            h.assertTrue(loaded.charge() == 1, "Excess saved charge is capped");
            var target = target(h);
            try {
                projectile.onHit(new EntityHitResult(target));
                h.assertTrue(Math.abs(target.getHealth() - (200 - 37.5F * 3)) < .001F
                                && target.isOnFire() && projectile.isRemoved(),
                        "Fully charged fireball deals triple direct damage and disappears");
                noPlacedFire(h, target, "Charged entity hits never place ground fire");
            } finally { target.discard(); projectile.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void allEightFlamesUseRealmDamageForRealEntityHitsAndContact(GameTestHelper h) {
        clear(h);
        String[] realms = {CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE};
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        try (var owner = new User(h)) {
            for (int i = 0; i < KINDS.length; i++) {
                FlameKind kind = KINDS[i];
                h.assertTrue(kind.damageCultivationLevel().equals(realms[i])
                                && Math.abs(kind.shotDamage() - SHOT_DAMAGE[i]) < .001F
                                && Math.abs(kind.contactDamagePerSecond() - CONTACT_DAMAGE[i]) < .001F,
                        "Flame damage matches its explicit cultivation tier: " + kind);
                var target = target(h);
                float[] incoming = {-1};
                boolean[] fireball = {false};
                Consumer<LivingIncomingDamageEvent> record = event -> {
                    if (event.getEntity() != target) return;
                    incoming[0] = event.getAmount();
                    fireball[0] = event.getSource().is(DamageTypes.FIREBALL);
                    // Keep the fixture alive while inspecting the real incoming amount for every tier.
                    event.setAmount(1);
                };
                NeoForge.EVENT_BUS.addListener(record);
                try {
                    var projectile = shot(h, owner.player, kind);
                    projectile.onHit(new EntityHitResult(target));
                    h.assertTrue(Math.abs(incoming[0] - SHOT_DAMAGE[i]) < .001F && fireball[0]
                                    && target.getHealth() == 199 && target.isOnFire() && projectile.isRemoved(),
                            "Actual entity hit submits realm-scaled fire damage, ignites and discards: " + kind);
                    noPlacedFire(h, target, "Entity hits never place a forge-fire block");
                    target.clearFire();
                    target.removeEffect(ModEffects.SPIRIT_FLAME_BURN);
                    target.getPersistentData().remove(CONTACT_STAMP);
                    target.invulnerableTime = 0;
                    incoming[0] = -1;
                    contact(h, kind, pos, target);
                    h.assertTrue(Math.abs(incoming[0] - CONTACT_DAMAGE[i]) < .001F && !fireball[0]
                                    && target.getHealth() == 198 && target.isOnFire()
                                    && target.hasEffect(ModEffects.QI_FIRE_BURN),
                            "Actual block contact submits the tier's per-second damage and accepted burn: " + kind);
                } finally {
                    NeoForge.EVENT_BUS.unregister(record);
                    target.discard();
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void entityHitCancellationResistanceAndImmunityCannotIgniteOrPlaceFire(GameTestHelper h) {
        clear(h);
        var target = target(h);
        var blaze = h.spawn(EntityType.BLAZE, new BlockPos(5, 2, 4));
        blaze.setNoAi(true);
        blaze.setNoGravity(true);
        try (var owner = new User(h)) {
            Consumer<LivingIncomingDamageEvent> protection = event -> {
                if (event.getEntity() == target) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protection);
            var blocked = shot(h, owner.player, FlameKind.HEAVENLY);
            try { blocked.onHit(new EntityHitResult(target)); }
            finally { NeoForge.EVENT_BUS.unregister(protection); }
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && blocked.isRemoved(),
                    "Canceled incoming damage keeps health, prevents ignition and discards the projectile");
            noPlacedFire(h, target, "Canceled damage cannot create a forge fire at the target");

            target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
            var resisted = shot(h, owner.player, FlameKind.HEAVENLY);
            resisted.onHit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && resisted.isRemoved(),
                    "Actual fire-resistance potion rejects flame-shot damage and ignition");
            noPlacedFire(h, target, "Resisted entity hits cannot create forge fire");

            float blazeHealth = blaze.getHealth();
            var immune = shot(h, owner.player, FlameKind.HEAVENLY);
            immune.onHit(new EntityHitResult(blaze));
            h.assertTrue(blaze.fireImmune() && blaze.getHealth() == blazeHealth
                            && !blaze.isOnFire() && immune.isRemoved(),
                    "Fire-immune entities take no flare damage or ignition");
            noPlacedFire(h, blaze, "Fire-immune entity hits cannot create forge fire");
        } finally { target.discard(); blaze.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void terrainHitsPlaceOnlyTheirMatchingFlameAndDiscardEvenWhenProtected(GameTestHelper h) {
        clear(h);
        BlockPos support = h.absolutePos(new BlockPos(3, 1, 3));
        BlockPos pos = support.above();
        h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false);
        try (var owner = new User(h)) {
            for (FlameKind kind : KINDS) {
                var projectile = shot(h, owner.player, kind);
                projectile.onHit(hit);
                h.assertTrue(projectile.isRemoved() && h.getLevel().getBlockState(pos).is(kind.block())
                                && h.getLevel().getBlockEntity(pos) instanceof SpiritFlameBlockEntity,
                        "Actual terrain hit places its matching timed flame and discards: " + kind);
                h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            Consumer<BlockEvent.EntityPlaceEvent> protection = event -> {
                if (event.getEntity() == owner.player && event.getPos().equals(pos)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protection);
            var blocked = shot(h, owner.player, FlameKind.DRAGON);
            try { blocked.onHit(hit); }
            finally { NeoForge.EVENT_BUS.unregister(protection); }
            h.assertTrue(blocked.isRemoved() && h.getLevel().getBlockState(pos).isAir()
                            && h.getLevel().getBlockEntity(pos) == null,
                    "Protected terrain hit discards and rolls back both flame and lifetime entity");
            owner.player.getAbilities().mayBuild = false;
            var unbuildable = shot(h, owner.player, FlameKind.CRIMSON);
            unbuildable.onHit(hit);
            h.assertTrue(unbuildable.isRemoved() && h.getLevel().getBlockState(pos).isAir(),
                    "A terrain hit without build permission discards without placing fire");
        } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void shotsRespectAlliesPetsPvPAndInvalidOwners(GameTestHelper h) {
        clear(h);
        var target = target(h);
        var wolf = h.spawn(EntityType.WOLF, new BlockPos(5, 2, 4));
        wolf.setNoAi(true);
        wolf.setNoGravity(true);
        var scoreboard = h.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("flame_" + UUID.randomUUID().toString().substring(0, 8));
        boolean originalPvp = h.getLevel().getServer().isPvpAllowed();
        try (var owner = new User(h); var other = new User(h)) {
            scoreboard.addPlayerToTeam(owner.player.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(target.getScoreboardName(), team);
            var allied = shot(h, owner.player, FlameKind.HEAVENLY);
            h.assertTrue(!allied.canHitEntity(target), "Teammates are excluded from projectile targeting");
            allied.onHit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && allied.isRemoved(),
                    "A forced stale teammate collision is rechecked and cannot hurt or ignite");
            noPlacedFire(h, target, "Ally collisions cannot place fire");
            scoreboard.removePlayerFromTeam(target.getScoreboardName(), team);

            wolf.setTame(true, true);
            wolf.setOwnerUUID(owner.player.getUUID());
            var pet = shot(h, owner.player, FlameKind.HEAVENLY);
            float petHealth = wolf.getHealth();
            h.assertTrue(!pet.canHitEntity(wolf), "Owner's tamed animals cannot be targeted");
            pet.onHit(new EntityHitResult(wolf));
            h.assertTrue(wolf.getHealth() == petHealth && !wolf.isOnFire() && pet.isRemoved(),
                    "Owned pet remains protected when a collision is dispatched directly");
            noPlacedFire(h, wolf, "Owned pet collisions cannot place fire");

            owner.player.server.setPvpAllowed(false);
            var pvp = shot(h, owner.player, FlameKind.HEAVENLY);
            float playerHealth = other.player.getHealth();
            h.assertTrue(!pvp.canHitEntity(other.player), "Disabled server PvP rejects player targets");
            pvp.onHit(new EntityHitResult(other.player));
            h.assertTrue(other.player.getHealth() == playerHealth && !other.player.isOnFire() && pvp.isRemoved(),
                    "PvP protection is rechecked at entity impact");
            owner.player.server.setPvpAllowed(true);
            other.player.setGameMode(GameType.CREATIVE);
            var creative = shot(h, owner.player, FlameKind.CRIMSON);
            h.assertTrue(!creative.canHitEntity(other.player), "Creative players are excluded from flare targeting");
            creative.onHit(new EntityHitResult(other.player));
            other.player.setGameMode(GameType.SPECTATOR);
            var spectator = shot(h, owner.player, FlameKind.CRIMSON);
            h.assertTrue(!spectator.canHitEntity(other.player), "Spectator players are excluded from flare targeting");
            spectator.onHit(new EntityHitResult(other.player));

            owner.player.setGameMode(GameType.SPECTATOR);
            var invalid = shot(h, owner.player, FlameKind.HEAVENLY);
            invalid.onHit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && invalid.isRemoved(),
                    "An owner becoming a spectator in flight invalidates the impact");
            owner.player.setGameMode(GameType.SURVIVAL);
            owner.player.setHealth(0);
            var dead = shot(h, owner.player, FlameKind.HEAVENLY);
            dead.onHit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && dead.isRemoved(),
                    "A dead owner cannot inflict flare damage after launching");
            noPlacedFire(h, target, "Invalid owner impacts cannot place fire");
        } finally {
            h.getLevel().getServer().setPvpAllowed(originalPvp);
            scoreboard.removePlayerTeam(team);
            target.discard(); wolf.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void everyPlacedFlameSavesThirtySecondDeadlineAndExpiresAfterUnloadedTime(GameTestHelper h) {
        clear(h);
        h.assertTrue(SpiritFlameBlockEntity.LIFETIME_TICKS == 600, "Thirty seconds means exactly 600 game ticks");
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        try {
            for (FlameKind kind : KINDS) {
                var flame = place(h, pos, kind);
                h.assertTrue(flame.expiresAt() == -1, "New flame awaits its first authoritative server tick");
                long now = h.getLevel().getGameTime();
                tick(h, flame);
                long deadline = now + 600;
                h.assertTrue(flame.expiresAt() == deadline, "Every exposed flame receives a thirty-second lifetime: " + kind);
                var saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
                h.assertTrue(saved.getLong("ExpiresAt") == deadline, "Absolute lifetime deadline is serialized");
                var loaded = reload(h, pos, saved);
                tick(h, loaded);
                h.assertTrue(loaded.kind() == kind && loaded.expiresAt() == deadline
                                && h.getLevel().getBlockState(pos).is(kind.block()),
                        "Reload preserves kind and deadline without granting another lifetime");

                // A saved absolute deadline models time elapsed while its chunk was unloaded.
                long elapsedDeadline = Math.max(0, now - 80);
                saved.putLong("ExpiresAt", elapsedDeadline);
                saved.putLong("IgnitedAt", elapsedDeadline - 600);
                var overdue = reload(h, pos, saved);
                h.assertTrue(overdue.expiresAt() == elapsedDeadline, "Reload retains an already elapsed deadline");
                tick(h, overdue);
                h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockEntity(pos) == null,
                        "An overdue reloaded flame disappears on its first server tick: " + kind);

                var boundary = place(h, pos, kind);
                var atDeadline = boundary.saveWithFullMetadata(h.getLevel().registryAccess());
                atDeadline.putLong("ExpiresAt", h.getLevel().getGameTime());
                atDeadline.putLong("IgnitedAt", h.getLevel().getGameTime() - 600);
                boundary = reload(h, pos, atDeadline);
                tick(h, boundary);
                h.assertTrue(h.getLevel().getBlockState(pos).isAir(),
                        "Expiration includes the exact saved deadline tick: " + kind);
            }
        } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 40)
    public static void realServerTickerRemovesSavedFlameAtItsDeadline(GameTestHelper h) {
        clear(h);
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        var flame = place(h, pos, FlameKind.HEAVENLY);
        tick(h, flame);
        var saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.putLong("ExpiresAt", h.getLevel().getGameTime() + 5);
        saved.putLong("IgnitedAt", h.getLevel().getGameTime() + 5 - 600);
        reload(h, pos, saved);
        h.runAfterDelay(1, () -> h.assertTrue(h.getLevel().getBlockState(pos).is(FlameKind.HEAVENLY.block()),
                "Saved flame stays until its deadline while the real server ticker runs"));
        h.runAfterDelay(7, () -> {
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockEntity(pos) == null,
                    "The installed block ticker removes both block and render carrier after its deadline");
            h.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void bothForgesExtendEveryFlameWithoutResettingItsAge(GameTestHelper h) {
        clear(h);
        h.assertTrue(SpiritFlameBlockEntity.FORGE_LIFETIME_TICKS == 3600,
                "A flame beneath a forge lasts three minutes total");
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        var forges = new net.minecraft.world.level.block.Block[] {
            com.example.immortal_cultivation_mod.block.ModBlocks.CU_TIE_XIAODING.get(),
            com.example.immortal_cultivation_mod.block.ModBlocks.QINGTONG_YINHUO_DING.get()
        };
        try {
            for (var forge : forges) for (FlameKind kind : KINDS) {
                h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                var flame = place(h, pos, kind);
                tick(h, flame);
                long ignition = h.getLevel().getGameTime();
                h.assertTrue(flame.expiresAt() == ignition + 600, "Uncovered flame lasts thirty seconds");
                h.getLevel().setBlockAndUpdate(pos.above(), forge.defaultBlockState());
                tick(h, flame);
                h.assertTrue(flame.expiresAt() == ignition + 3600,
                        "Both forge types extend all eight flame kinds to three minutes");
                var saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
                flame = reload(h, pos, saved);
                tick(h, flame);
                h.assertTrue(flame.expiresAt() == ignition + 3600 && saved.getLong("IgnitedAt") == ignition,
                        "Reloading under a forge retains the original ignition time");
                h.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                tick(h, flame);
                h.assertTrue(flame.expiresAt() == ignition + 600,
                        "Ordinary covering blocks do not grant forge lifetime");
                h.getLevel().setBlockAndUpdate(pos.above(), forge.defaultBlockState());
                tick(h, flame);
                h.assertTrue(flame.expiresAt() == ignition + 3600,
                        "Replacing the forge does not restart the timer");

                saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
                saved.putLong("IgnitedAt", h.getLevel().getGameTime() - 1200);
                flame = reload(h, pos, saved);
                tick(h, flame);
                h.assertTrue(h.getLevel().getBlockState(pos).is(kind.block()),
                        "A one-minute-old flame remains alive beneath a forge");
                h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                tick(h, flame);
                h.assertTrue(h.getLevel().getBlockState(pos).isAir(),
                        "Removing the forge expires fire already older than thirty seconds");

                flame = place(h, pos, kind);
                h.getLevel().setBlockAndUpdate(pos.above(), forge.defaultBlockState());
                saved = flame.saveWithFullMetadata(h.getLevel().registryAccess());
                saved.putLong("IgnitedAt", h.getLevel().getGameTime() - 3600);
                flame = reload(h, pos, saved);
                tick(h, flame);
                h.assertTrue(h.getLevel().getBlockState(pos).isAir(),
                        "A forge cannot keep fire alive beyond the exact three-minute deadline");
            }
        } finally {
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyThreeMinuteSavesPreserveElapsedAgeUnderNewLifetimeRules(GameTestHelper h) {
        clear(h);
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        long now = h.getLevel().getGameTime();
        var flame = place(h, pos, FlameKind.CRIMSON);
        var legacy = flame.saveWithFullMetadata(h.getLevel().registryAccess());
        legacy.remove("IgnitedAt");
        legacy.putLong("ExpiresAt", now + 3500); // Previously lit five seconds ago.
        flame = reload(h, pos, legacy);
        tick(h, flame);
        h.assertTrue(flame.expiresAt() == now + 500,
                "Old uncovered fire keeps its elapsed five seconds instead of restarting");
        h.getLevel().setBlockAndUpdate(pos.above(),
                com.example.immortal_cultivation_mod.block.ModBlocks.CU_TIE_XIAODING.get().defaultBlockState());
        tick(h, flame);
        h.assertTrue(flame.expiresAt() == now + 3500,
                "A forge restores the original three-minute deadline, not three new minutes");
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyLoadedChunksRecreateMissingFlameCarriersWithoutResettingSavedDeadlines(GameTestHelper h) {
        clear(h);
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        var chunk = h.getLevel().getChunkAt(pos);
        try {
            for (FlameKind kind : KINDS) {
                var existing = place(h, pos, kind);
                tick(h, existing);
                long deadline = existing.expiresAt();
                h.assertTrue(SpiritFlameBlockEntity.migrateLoadedChunk(h.getLevel(), chunk)
                                && chunk.getBlockEntities().get(pos) == existing && existing.expiresAt() == deadline,
                        "Loaded-chunk migration preserves an existing saved deadline: " + kind);

                // Model an old section whose cross fire had no block entity before the renderer update.
                h.getLevel().removeBlockEntity(pos);
                h.assertTrue(!chunk.getBlockEntities().containsKey(pos) && h.getLevel().getBlockState(pos).is(kind.block()),
                        "Legacy fixture retains the real flame block while its render/lifetime carrier is missing");
                h.assertTrue(SpiritFlameBlockEntity.migrateLoadedChunk(h.getLevel(), chunk),
                        "Migration accepts the actual already-loaded full chunk");
                var migrated = chunk.getBlockEntities().get(pos);
                h.assertTrue(migrated instanceof SpiritFlameBlockEntity flame && flame != existing
                                && flame.kind() == kind && flame.expiresAt() == -1,
                        "Palette migration recreates the matching missing flame carrier: " + kind);
                var flame = (SpiritFlameBlockEntity) migrated;
                tick(h, flame);
                long initialized = flame.expiresAt();
                SpiritFlameBlockEntity.migrateLoadedChunk(h.getLevel(), chunk);
                h.assertTrue(initialized == h.getLevel().getGameTime() + 600
                                && chunk.getBlockEntities().get(pos) == flame && flame.expiresAt() == initialized,
                        "Migrated legacy fire gets one full lifetime and repeated migration never extends it");
            }
        } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void legacyFlameDeadlineInitializesOnceAndStaleBlockEntitiesCannotDeleteReplacements(GameTestHelper h) {
        clear(h);
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        try {
            var original = place(h, pos, FlameKind.CRIMSON);
            var legacySave = original.saveWithFullMetadata(h.getLevel().registryAccess());
            legacySave.remove("ExpiresAt");
            var legacy = reload(h, pos, legacySave);
            tick(h, legacy);
            long deadline = legacy.expiresAt();
            tick(h, legacy);
            h.assertTrue(deadline == h.getLevel().getGameTime() + 600 && legacy.expiresAt() == deadline,
                    "Legacy saves acquire one deadline and repeated ticks never reset it");

            var stale = new SpiritFlameBlockEntity(pos, FlameKind.CRIMSON.block().defaultBlockState());
            var expired = legacy.saveWithFullMetadata(h.getLevel().registryAccess());
            expired.putLong("ExpiresAt", h.getLevel().getGameTime());
            stale.loadWithComponents(expired, h.getLevel().registryAccess());
            stale.setLevel(h.getLevel());
            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            tick(h, stale);
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE),
                    "An expired unattached block entity cannot delete a solid replacement");
            var replacement = place(h, pos, FlameKind.HEAVENLY);
            tick(h, stale);
            h.assertTrue(h.getLevel().getBlockState(pos).is(FlameKind.HEAVENLY.block())
                            && h.getLevel().getBlockEntity(pos) == replacement && replacement.expiresAt() == -1,
                    "A stale carrier cannot initialize or expire a different newly placed flame");
        } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 50)
    public static void contactDamageIsGatedAcrossFlameBlocksUntilTwentyTicksElapse(GameTestHelper h) {
        clear(h);
        var target = target(h);
        BlockPos crimson = h.absolutePos(new BlockPos(3, 2, 3));
        BlockPos earth = crimson.east();
        place(h, crimson, FlameKind.CRIMSON);
        place(h, earth, FlameKind.EARTH);
        contact(h, FlameKind.CRIMSON, crimson, target);
        float afterFirst = target.getHealth();
        h.assertTrue(afterFirst == 187.5F && target.isOnFire() && target.hasEffect(ModEffects.QI_FIRE_BURN),
                "First contact deals the exact tier damage and applies accepted ignition/status");
        target.clearFire(); target.removeEffect(ModEffects.QI_FIRE_BURN); target.invulnerableTime = 0;
        contact(h, FlameKind.EARTH, earth, target);
        h.assertTrue(target.getHealth() == afterFirst && !target.isOnFire() && !target.hasEffect(ModEffects.QI_FIRE_BURN),
                "Contact with a second kind cannot stack damage even after vanilla invulnerability is cleared");
        h.runAfterDelay(19, () -> {
            target.invulnerableTime = 0;
            contact(h, FlameKind.EARTH, earth, target);
            h.assertTrue(target.getHealth() == afterFirst && !target.isOnFire(),
                    "Contact remains gated before twenty game ticks");
        });
        h.runAfterDelay(20, () -> {
            try {
                target.invulnerableTime = 0;
                contact(h, FlameKind.EARTH, earth, target);
                h.assertTrue(target.getHealth() == afterFirst - 35 && target.isOnFire()
                                && target.hasEffect(ModEffects.QI_FIRE_BURN),
                        "The next kind may deal its exact damage at twenty ticks");
                h.succeed();
            } finally {
                target.discard();
                h.getLevel().setBlockAndUpdate(crimson, Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(earth, Blocks.AIR.defaultBlockState());
            }
        });
    }

    @GameTest(template = "fate_test")
    public static void protectedContactDoesNotIgniteApplyStatusOrBypassInvulnerability(GameTestHelper h) {
        clear(h);
        var target = target(h);
        var blaze = h.spawn(EntityType.BLAZE, new BlockPos(5, 2, 4));
        blaze.setNoAi(true); blaze.setNoGravity(true);
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        try (var user = new User(h)) {
            Consumer<LivingIncomingDamageEvent> protection = event -> {
                if (event.getEntity() == target) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protection);
            try { contact(h, FlameKind.CRIMSON, pos, target); }
            finally { NeoForge.EVENT_BUS.unregister(protection); }
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && !target.hasEffect(ModEffects.QI_FIRE_BURN),
                    "Canceled block damage cannot ignite or apply Qi-fire burn");
            contact(h, FlameKind.HEAVENLY, pos.east(), target);
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire(),
                    "Canceled attempts share the contact gate across other flame blocks");

            target.getPersistentData().remove(CONTACT_STAMP);
            target.setInvulnerable(true);
            contact(h, FlameKind.HEAVENLY, pos, target);
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire()
                            && !target.getPersistentData().contains(CONTACT_STAMP),
                    "Invulnerable entities are rejected before a contact attempt");
            target.setInvulnerable(false);
            target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
            contact(h, FlameKind.HEAVENLY, pos, target);
            h.assertTrue(target.getHealth() == 200 && !target.isOnFire() && !target.hasEffect(ModEffects.QI_FIRE_BURN),
                    "Fire resistance rejects contact damage and all accepted-hit effects");

            float beforeBlaze = blaze.getHealth();
            contact(h, FlameKind.HEAVENLY, pos, blaze);
            h.assertTrue(blaze.getHealth() == beforeBlaze && !blaze.isOnFire()
                            && !blaze.hasEffect(ModEffects.QI_FIRE_BURN),
                    "Fire-immune mobs receive neither contact damage nor Qi-fire status");
            float playerHealth = user.player.getHealth();
            user.player.setGameMode(GameType.CREATIVE);
            contact(h, FlameKind.HEAVENLY, pos, user.player);
            user.player.setGameMode(GameType.SPECTATOR);
            contact(h, FlameKind.HEAVENLY, pos, user.player);
            h.assertTrue(user.player.getHealth() == playerHealth && !user.player.isOnFire()
                            && !user.player.hasEffect(ModEffects.QI_FIRE_BURN),
                    "Creative and spectator players are excluded from placed-flame contact damage");
        } finally { target.discard(); blaze.discard(); }
        h.succeed();
    }
    @GameTest(template = "fate_test", timeoutTicks = 65)
    public static void leavingEveryFlameKeepsItsTierDamageWithoutVanillaStacking(GameTestHelper h) {
        clear(h);
        var victims = new java.util.ArrayList<Cow>();
        var amounts = new java.util.HashMap<UUID, java.util.List<Float>>();
        Consumer<LivingIncomingDamageEvent> record = event -> {
            var hits = amounts.get(event.getEntity().getUUID());
            if (hits != null && event.getSource().is(DamageTypes.IN_FIRE)) {
                hits.add(event.getAmount());
                event.setAmount(1); // Survive even Nascent Soul fire while recording its unmodified damage.
            }
        };
        NeoForge.EVENT_BUS.addListener(record);
        for (FlameKind kind : KINDS) {
            var victim = target(h); victims.add(victim);
            amounts.put(victim.getUUID(), new java.util.ArrayList<>());
            contact(h, kind, h.absolutePos(new BlockPos(3, 2, 3)), victim);
            h.assertTrue(SpiritFlameBurnEffect.kind(victim) == kind, "Burn retains its source kind for client sync: " + kind);
        }
        h.runAfterDelay(41, () -> {
            try {
                for (int i = 0; i < KINDS.length; i++) {
                    var victim = victims.get(i); var hits = amounts.get(victim.getUUID());
                    h.assertTrue(hits.size() == 3 && hits.stream().allMatch(value -> value == KINDS[victims.indexOf(victim)].contactDamagePerSecond()),
                            "Contact and both off-block burn ticks keep identical tier damage: " + KINDS[i] + " " + hits);
                    h.assertTrue(victim.getHealth() == 197, "No extra vanilla burning damage: " + KINDS[i] + " " + victim.getHealth());
                    contact(h, KINDS[i], victim.blockPosition(), victim);
                    h.assertTrue(victim.getHealth() == 197, "Contact cannot double the ongoing burn tick");
                }
                h.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(record);
                victims.forEach(Cow::discard);
            }
        });
    }

    @GameTest(template = "fate_test")
    public static void flameBurnSaveReloadRetainsKindDamageAndExtinguishes(GameTestHelper h) {
        clear(h);
        var victim = target(h);
        var loaded = EntityType.COW.create(h.getLevel());
        try {
            contact(h, FlameKind.EARTH, victim.blockPosition(), victim);
            loaded.load(victim.saveWithoutId(new CompoundTag()));
            h.assertTrue(SpiritFlameBurnEffect.kind(loaded) == FlameKind.EARTH && loaded.isOnFire(),
                    "Saved burn retains synced type and fire duration");
            loaded.invulnerableTime = 0;
            loaded.getPersistentData().putLong(CONTACT_STAMP, h.getLevel().getGameTime() - 20);
            float before = loaded.getHealth();
            loaded.getEffect(ModEffects.SPIRIT_FLAME_BURN).tick(loaded, () -> {});
            h.assertTrue(loaded.getHealth() == before - 35, "Reloaded off-block burn keeps Earth fire damage");
            loaded.invulnerableTime = 0;
            float after = loaded.getHealth();
            loaded.hurt(loaded.damageSources().onFire(), 1);
            h.assertTrue(loaded.getHealth() == after, "Vanilla burn cannot add damage on top");
            loaded.clearFire();
            loaded.getEffect(ModEffects.SPIRIT_FLAME_BURN).tick(loaded, () -> {});
            h.assertTrue(!loaded.hasEffect(ModEffects.SPIRIT_FLAME_BURN) && loaded.getHealth() == after,
                    "Extinguishing immediately ends tier damage and source color");
            h.succeed();
        } finally { victim.discard(); if (loaded != null) loaded.discard(); }
    }

    @GameTest(template = "fate_test")
    public static void lingeringFlameRespectsProtectionAndFireResistance(GameTestHelper h) {
        clear(h);
        var victim = target(h);
        try {
            contact(h, FlameKind.CRIMSON, victim.blockPosition(), victim);
            float before = victim.getHealth(); victim.invulnerableTime = 0;
            victim.getPersistentData().putLong(CONTACT_STAMP, h.getLevel().getGameTime() - 20);
            Consumer<LivingIncomingDamageEvent> cancel = event -> {
                if (event.getEntity() == victim) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { victim.getEffect(ModEffects.SPIRIT_FLAME_BURN).tick(victim, () -> {}); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(victim.getHealth() == before, "Protection cancels continuing burn damage");
            victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
            victim.getPersistentData().putLong(CONTACT_STAMP, h.getLevel().getGameTime() - 20);
            victim.getEffect(ModEffects.SPIRIT_FLAME_BURN).tick(victim, () -> {});
            h.assertTrue(!victim.hasEffect(ModEffects.SPIRIT_FLAME_BURN) && victim.getHealth() == before,
                    "Fire resistance ends the damaging burn");
            h.succeed();
        } finally { victim.discard(); }
    }

    @GameTest(template = "fate_test")
    public static void chargedProjectileAcceleratesAndRetainsAgeAfterReload(GameTestHelper h) {
        try (var owner = new User(h)) {
            var shot=new FlameShotEntity(ModEntities.FLAME_SHOT.get(),h.getLevel());
            shot.setOwner(owner.player);shot.setKind(FlameKind.THUNDER);shot.setCharge(1);
            shot.setPos(h.absoluteVec(new Vec3(3,25,3)));shot.setDeltaMovement(.5,0,0);
            double speed=0;
            for(int tick=0;tick<15;tick++) {
                // ServerLevel normally advances Entity.tickCount around the entity tick.
                shot.tickCount=tick;
                shot.tick();
                double next=shot.getDeltaMovement().length();
                h.assertTrue(!shot.isRemoved() && next>=speed && next<=1.81,
                        "Launch accelerates smoothly without exceeding maximum speed");
                speed=next;
            }
            h.assertTrue(speed>1.7,"Full speed reached after launch acceleration");
            var reloaded=new FlameShotEntity(ModEntities.FLAME_SHOT.get(),h.getLevel());
            reloaded.load(shot.saveWithoutId(new CompoundTag()));
            h.assertTrue(reloaded.tickCount==shot.tickCount && reloaded.charge()==1 && reloaded.kind()==FlameKind.THUNDER,
                    "Reload preserves launch age, charge and elemental identity rather than restarting acceleration");
            shot.discard();reloaded.discard();
        }
        h.succeed();
    }

}
