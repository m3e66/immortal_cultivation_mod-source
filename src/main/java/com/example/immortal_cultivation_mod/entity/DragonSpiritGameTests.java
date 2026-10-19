package com.example.immortal_cultivation_mod.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_realms")
@PrefixGameTestTemplate(false)
public final class DragonSpiritGameTests {
    @GameTest(template = "fate_test")
    public static void capturedSpiritCanProduceOneDragonFlame(GameTestHelper h) {
        com.example.immortal_cultivation_mod.flame.FlameAcquisitionIntegrationGameTests.dragonSpiritCaptureProducesOneNaturalFlame(h);
    }

    private static EscapedYuanyingCoreEntity release(GameTestHelper h) {
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true);
        dragon.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
        h.getLevel().addFreshEntity(dragon);
        dragon.setHealth(0);
        dragon.die(dragon.damageSources().genericKill());
        var spirits = h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class,
                dragon.getBoundingBox().inflate(48), core -> dragon.getUUID().equals(core.originalDragon()));
        h.assertTrue(spirits.size() == 1, "Actual dragon death releases one spirit");
        dragon.discard();
        return spirits.getFirst();
    }

    private static CompoundTag expirySave(EscapedYuanyingCoreEntity spirit) {
        var tag = spirit.saveWithoutId(new CompoundTag());
        tag.putInt("RevivalTicks", 1);
        return tag;
    }

    private static EscapedYuanyingCoreEntity reload(GameTestHelper h, CompoundTag tag, boolean duplicate) {
        var spirit = ModEntities.ESCAPED_YUANYING_CORE.get().create(h.getLevel());
        spirit.load(tag);
        if (duplicate) spirit.setUUID(UUID.randomUUID());
        h.getLevel().addFreshEntity(spirit);
        return spirit;
    }

    private static long bodies(GameTestHelper h, AABB area, UUID original) {
        return h.getLevel().getEntitiesOfClass(CrimsonSkyFlameFloodDragonEntity.class, area,
                dragon -> original.equals(dragon.getUUID()) && dragon.isAlive()).size();
    }

    private static void cleanup(GameTestHelper h, AABB area, UUID original) {
        h.getLevel().getEntitiesOfClass(CrimsonSkyFlameFloodDragonEntity.class, area,
                dragon -> original.equals(dragon.getUUID())).forEach(Entity::discard);
        h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class, area,
                core -> original.equals(core.originalDragon())).forEach(Entity::discard);
        h.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(Entity::discard);
    }

    @GameTest(template = "fate_test")
    public static void duplicateAndStaleSavedSpiritsCannotCreateTwoDragons(GameTestHelper h) {
        var spirit = release(h);
        UUID original = spirit.originalDragon();
        var area = spirit.getBoundingBox().inflate(48);
        var saved = expirySave(spirit);
        try {
            spirit.discard();
            var duplicate = reload(h, saved, true);
            duplicate.tick();
            h.assertTrue(duplicate.isRemoved() && bodies(h, area, original) == 0,
                    "Duplicate spirit cannot take ownership even while the canonical spirit is unloaded");
            var restored = reload(h, saved, false);
            restored.tick();
            restored.tick();
            h.assertTrue(restored.isRemoved() && bodies(h, area, original) == 1,
                    "Canonical spirit revives one body even if its final tick repeats");
            var stale = reload(h, saved, false);
            stale.tick();
            h.assertTrue(stale.isRemoved() && bodies(h, area, original) == 1,
                    "Pre-revival save copy cannot revive another dragon");

            var body = (CrimsonSkyFlameFloodDragonEntity) h.getLevel().getEntity(original);
            body.setHealth(0);
            body.die(body.damageSources().genericKill());
            body.discard();
            stale = reload(h, saved, false);
            stale.tick();
            h.assertTrue(stale.isRemoved() && bodies(h, area, original) == 0,
                    "Old spirit cannot take over a later escape of the same dragon");
            var current = h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class, area,
                    core -> original.equals(core.originalDragon()));
            h.assertTrue(current.size() == 1 && !current.getFirst().getUUID().equals(spirit.getUUID()),
                    "Later death retains exactly its new canonical spirit");
        } finally { cleanup(h, area, original); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void duplicateSpiritCannotGrantLootAndOldCopiesStayResolved(GameTestHelper h) {
        var spirit = release(h);
        UUID original = spirit.originalDragon();
        var area = spirit.getBoundingBox().inflate(48);
        var saved = expirySave(spirit);
        int[] rolls = {0};
        Consumer<LivingDropsEvent> observe = event -> {
            if (event.getEntity() instanceof EscapedYuanyingCoreEntity core
                    && original.equals(core.originalDragon())) rolls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, observe);
        try {
            var duplicate = reload(h, saved, true);
            duplicate.setHealth(0);
            duplicate.die(duplicate.damageSources().genericKill());
            h.assertTrue(duplicate.isRemoved() && rolls[0] == 0 && spirit.isAlive(),
                    "Killing duplicate spirit releases no loot and leaves the real spirit intact");
            spirit.setHealth(0);
            spirit.die(spirit.damageSources().genericKill());
            spirit.die(spirit.damageSources().genericKill());
            spirit.discard();
            var stale = reload(h, saved, false);
            stale.setHealth(0);
            stale.die(stale.damageSources().genericKill());
            h.assertTrue(stale.isRemoved() && rolls[0] == 1,
                    "Spirit loot resolves once even after replaying a pre-death save");
        } finally {
            NeoForge.EVENT_BUS.unregister(observe);
            cleanup(h, area, original);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void existingLiveBodyPreventsSpiritFromCreatingAnother(GameTestHelper h) {
        var spirit = release(h);
        UUID original = spirit.originalDragon();
        var area = spirit.getBoundingBox().inflate(48);
        var saved = expirySave(spirit);
        spirit.discard();
        try {
            var body = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
            body.setUUID(original);
            body.setNoAi(true);
            body.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
            h.getLevel().addFreshEntity(body);
            var restored = reload(h, saved, false);
            restored.tick();
            h.assertTrue(restored.isRemoved() && bodies(h, area, original) == 1 && body.isAlive(),
                    "Recovered original body survives and its stale spirit creates no duplicate");
        } finally { cleanup(h, area, original); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void canceledRevivalRetriesWithoutDuplicatingOrLosingSpirit(GameTestHelper h) {
        var spirit = release(h);
        UUID original = spirit.originalDragon();
        var area = spirit.getBoundingBox().inflate(48);
        var saved = expirySave(spirit);
        spirit.discard();
        int[] attempts = {0};
        Consumer<EntityJoinLevelEvent> cancelFirst = event -> {
            if (event.getEntity() instanceof CrimsonSkyFlameFloodDragonEntity
                    && original.equals(event.getEntity().getUUID()) && ++attempts[0] == 1) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancelFirst);
        try {
            var restored = reload(h, saved, false);
            restored.tick();
            h.assertTrue(!restored.isRemoved() && bodies(h, area, original) == 0,
                    "Canceled body spawn keeps spirit available");
            for (int tick = 0; tick < 20; tick++) restored.tick();
            h.assertTrue(restored.isRemoved() && attempts[0] == 2 && bodies(h, area, original) == 1,
                    "One retry creates exactly one body");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancelFirst);
            cleanup(h, area, original);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void escapeOwnershipSurvivesWorldSaveAndReusesOneRecord(GameTestHelper h) {
        var data = DragonSpiritData.load(new CompoundTag(), h.getLevel().registryAccess());
        UUID dragon = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        data.begin(dragon, first);
        data = DragonSpiritData.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(data.owns(dragon, first) && !data.owns(dragon, second), "World save retains canonical spirit identity");
        data.finish(dragon, first);
        data = DragonSpiritData.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(!data.owns(dragon, first), "Resolved escape stays resolved after world reload");
        data.begin(dragon, second);
        h.assertTrue(data.owns(dragon, second) && !data.owns(dragon, first)
                        && data.save(new CompoundTag(), h.getLevel().registryAccess()).getList("Escapes", 10).size() == 1,
                "Next escape replaces the record without accepting old copies or growing a history");
        h.succeed();
    }
}
