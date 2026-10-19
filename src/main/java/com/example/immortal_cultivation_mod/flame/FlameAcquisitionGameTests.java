package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameAcquisitionGameTests {
    @GameTest(template = "fate_test")
    public static void flameDeathsUseActualRealmRootsAndUndeadRank(GameTestHelper h) {
        var npc = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        var lizard = ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(h.getLevel());
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        var spirit = new com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity(h.getLevel(),dragon);
        try {
            NpcProfile.ensure(npc, npc.npcArchetype());
            var profile = npc.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Realm", CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
            var roots = new ListTag(); roots.add(StringTag.valueOf(SpiritRoots.FIRE));
            profile.put("SpiritRoots", roots);
            h.assertTrue(FlameAcquisition.deathKind(npc) == FlameKind.SOLAR,
                    "A real Golden Core fire cultivator is a Solar Flame source");
            profile.putString("Realm", CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE);
            h.assertTrue(FlameAcquisition.deathKind(npc) == null,
                    "A lower-realm fire cultivator is not a Solar Flame source");
            profile.putString("Realm", CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
            roots = new ListTag(); roots.add(StringTag.valueOf(SpiritRoots.WATER)); profile.put("SpiritRoots", roots);
            h.assertTrue(FlameAcquisition.deathKind(npc) == null,
                    "Realm alone cannot make a water cultivator drop Solar Flame");
            h.assertTrue(FlameAcquisition.deathKind(zombie) == null, "An ordinary zombie is not a Nether source");
            zombie.getPersistentData().putLong(UndeadControl.TAG_GRUDGE, 12_500);
            h.assertTrue(FlameAcquisition.deathKind(zombie) == FlameKind.NETHER
                            && FlameAcquisition.deathChance(zombie, FlameKind.NETHER) == 10,
                    "Actual flying-undead grudge rank gets the better Nether chance");
            h.assertTrue(FlameAcquisition.deathKind(lizard) == FlameKind.CRIMSON,
                    "The actual Crimson Scale Flame Lizard is a Crimson source");
            h.assertTrue(FlameAcquisition.deathKind(dragon) == null
                            && FlameAcquisition.deathKind(spirit) == FlameKind.DRAGON
                            && FlameAcquisition.deathChance(spirit, FlameKind.DRAGON) == 64,
                    "Only the escaped dragon spirit has the existing one-in-64 Dragon Flame opportunity");
        } finally { npc.discard(); zombie.discard(); lizard.discard(); dragon.discard(); spirit.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void naturalSourceClaimsPersistAndDoNotShareKinds(GameTestHelper h) {
        var data = new FlameAcquisitionData();
        String wood = FlameAcquisition.chunkKey("wood", new ChunkPos(12, -7));
        String thunder = "thunder:" + new BlockPos(192, 64, -112).asLong();
        h.assertTrue(data.claim(wood) && !data.claim(wood) && data.claim(thunder),
                "Natural chunk sources and individual Thunder sites have independent one-time claims");
        var loaded = FlameAcquisitionData.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(loaded.contains(wood) && loaded.contains(thunder) && !loaded.claim(wood),
                "Restarting and revisiting cannot reroll a claimed natural source");
        h.assertTrue(!FlameAcquisition.isSolarRuin(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "tomb1"))
                        && !FlameAcquisition.isSolarRuin(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "tomb2"))
                        && FlameAcquisition.isSolarRuin(ResourceLocation.withDefaultNamespace("ruined_portal_mountain"))
                        && !FlameAcquisition.isSolarRuin(ResourceLocation.withDefaultNamespace("village_plains")),
                "Only solar ruins are eligible; neither tomb can spawn a structure flame");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lizardDeathHookRespectsCancellationAndCannotDuplicate(GameTestHelper h) {
        var level = h.getLevel();
        var lizard = ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(level);
        String source = null;
        for (long i = 0; i < 2000; i++) {
            UUID id = new UUID(h.absolutePos(BlockPos.ZERO).asLong(), i);
            String candidate = "death:CRIMSON:" + id;
            if (FlameAcquisition.roll(level.getSeed(), candidate, 20)) {
                lizard.setUUID(id); source = candidate; break;
            }
        }
        h.assertTrue(source != null, "Fixture finds a deterministic successful rare death roll");
        lizard.setNoAi(true);
        lizard.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 3, 3))));
        level.addFreshEntity(lizard);
        String key = source;
        var area = new AABB(lizard.position(), lizard.position()).inflate(10);
        try {
            var cancelled = new LivingDeathEvent(lizard, level.damageSources().generic());
            cancelled.setCanceled(true);
            FlameAcquisition.onDeath(cancelled);
            h.assertTrue(!lizard.getPersistentData().getBoolean("icmFlameDeathChecked"),
                    "A prevented death does not spend or generate a flame opportunity");
            // Dispatch through the real NeoForge bus, then repeat the same source callback.
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(lizard, level.damageSources().generic()));
            h.assertTrue(level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                            flame -> key.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).size() == 1,
                    "Actual death subscriber spawns one persistent, capturable flame");
            FlameAcquisition.onDeath(new LivingDeathEvent(lizard, level.damageSources().generic()));
            h.assertTrue(level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                            flame -> key.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).size() == 1,
                    "Duplicate death callbacks cannot duplicate flames");
        } finally {
            level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> key.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).forEach(SpiritFlameEntity::discard);
            lizard.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void lightningHooksRequireSpiritwoodOrRealTrialHeat(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(5, 2, 5));
        var area = new AABB(Vec3.atCenterOf(base), Vec3.atCenterOf(base)).inflate(16);
        String woodKey = FlameAcquisition.chunkKey("wood", new ChunkPos(base));
        String thunderKey = "thunder:" + base.offset(3, 0, 0).asLong();
        var data = FlameAcquisitionData.get(level);
        try {
            for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.above(y), ModBlocks.LINGMU_LOG.get().defaultBlockState());
            h.assertTrue(!FlameAcquisition.matureSpiritwood(level, base),
                    "A bare placed log pillar is not ancient spiritwood");
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0)
                level.setBlockAndUpdate(base.offset(x, 5, z), ModBlocks.LINGMU_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
            h.assertTrue(FlameAcquisition.matureSpiritwood(level, base),
                    "A mature six-block spiritwood trunk with its living crown is eligible");
            BlockPos open = FlameAcquisition.findOpenAirNear(level, base.above(5));
            h.assertTrue(open != null && level.getBlockState(open).isAir() && level.getBlockState(open.above()).isAir(),
                    "Wood placement leaves the solid trunk and leaf crown in open, accessible air");
            var placedWood = SpiritFlameEntity.spawn(level, Vec3.atBottomCenterOf(open).add(0, .2, 0), FlameKind.WOOD);
            h.assertTrue(placedWood != null && placedWood.isPickable()
                            && level.getBlockState(placedWood.blockPosition()).isAir(),
                    "The actual Wood essence at that placement is pickable and unobstructed");
            placedWood.discard();
            var bolt = EntityType.LIGHTNING_BOLT.create(level);
            bolt.setVisualOnly(true); bolt.setPos(Vec3.atCenterOf(base.above(6)));
            FlameAcquisition.onLightningJoined(new EntityJoinLevelEvent(bolt, level, false));
            FlameAcquisition.onLevelTick(new LevelTickEvent.Post(() -> true, level));
            h.assertTrue(data.contains(woodKey) && !data.contains(thunderKey),
                    "Ordinary lightning processes one Wood opportunity and never generates Thunder");
            FlameAcquisition.onWeaponLightning(level, Vec3.atCenterOf(base.offset(3, 0, 0)));
            h.assertTrue(!data.contains(thunderKey), "A trial strike on dry ground cannot generate Thunder Flame");
            BlockPos lava = base.offset(3, 0, 0);
            level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
            FlameAcquisition.onWeaponLightning(level, Vec3.atCenterOf(lava));
            h.assertTrue(data.contains(thunderKey), "A genuine weapon strike intersecting lava creates and claims the Thunder site");
            int before = level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> thunderKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).size();
            h.assertTrue(before == 1, "An eligible weapon lightning strike always creates the actual Thunder essence");
            FlameAcquisition.onWeaponLightning(level, Vec3.atCenterOf(lava));
            h.assertTrue(level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> thunderKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).size() == before,
                    "Repeated trial hits cannot farm another Thunder Flame from the same chunk");
            level.setBlockAndUpdate(lava, Blocks.AIR.defaultBlockState());
            bolt.discard();
        } finally {
            level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> woodKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))
                            || thunderKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).forEach(SpiritFlameEntity::discard);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void heavenlyAcquisitionRequiresLivingPowerfulSurvivorAndCannotReroll(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos strike = h.absolutePos(new BlockPos(3, 2, 3));
        for (BlockPos pos : BlockPos.betweenClosed(strike.offset(-2, 0, -2), strike.offset(2, 4, 2)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var data = FlameAcquisitionData.get(level);
        var area = new AABB(strike).inflate(8);
        try (var owner = new Survivor(h)) {
            UUID eligible = null;
            UUID unsuccessful = null;
            for (long i = 0; i < 100000 && (eligible == null || unsuccessful == null); i++) {
                UUID trial = new UUID(strike.asLong(), i);
                String key = "heavenly:" + trial;
                if (data.contains(key)) continue;
                if (FlameAcquisition.roll(level.getSeed(), key, 512)) eligible = trial;
                else unsuccessful = trial;
            }
            h.assertTrue(eligible != null && unsuccessful != null,
                    "Fixture finds both real one-in-512 Heavenly source outcomes");
            String successKey = "heavenly:" + eligible;
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 2, 500);
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 3, 299);
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 3, Double.NaN);
            h.assertTrue(!data.contains(successKey),
                    "Low rank, insufficient power and invalid power do not create or spend a Heavenly opportunity");
            owner.player.setHealth(0);
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 3, 500);
            h.assertTrue(!data.contains(successKey),
                    "A dead owner cannot receive Heavenly fire from a completed weapon trial");
            owner.player.setHealth(owner.player.getMaxHealth());
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 3, 500);
            var flames = level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> successKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY)));
            h.assertTrue(data.contains(successKey) && flames.size() == 1 && flames.getFirst().kind() == FlameKind.HEAVENLY,
                    "An eligible living survivor's successful rare roll creates one capturable Heavenly essence");
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), eligible, 3, 500);
            h.assertTrue(level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                            flame -> successKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).size() == 1,
                    "Repeated callbacks cannot duplicate a successful trial's Heavenly essence");
            String failureKey = "heavenly:" + unsuccessful;
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), unsuccessful, 3, 500);
            FlameAcquisition.onPowerfulTribulationSurvived(owner.player, Vec3.atCenterOf(strike), unsuccessful, 3, 500);
            h.assertTrue(data.contains(failureKey) && level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                            flame -> failureKey.equals(flame.getPersistentData().getString(FlameAcquisition.SOURCE_KEY))).isEmpty(),
                    "A failed rare roll is consumed once and cannot reroll after revisiting the trial");
        } finally {
            level.getEntitiesOfClass(SpiritFlameEntity.class, area,
                    flame -> flame.kind() == FlameKind.HEAVENLY).forEach(SpiritFlameEntity::discard);
        }
        h.succeed();
    }

    private static final class Survivor implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;

        Survivor(GameTestHelper h) {
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "heavenly-survivor"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { }
            };
            player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 1))));
        }

        @Override public void close() { player.discard(); channel.close(); }
    }
}
