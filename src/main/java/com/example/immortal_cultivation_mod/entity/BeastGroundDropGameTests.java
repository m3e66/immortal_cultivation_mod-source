package com.example.immortal_cultivation_mod.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_ground_drops")
@PrefixGameTestTemplate(false)
public final class BeastGroundDropGameTests {
    @GameTest(template = "fate_test")
    public static void actualGroundDropsMatchOneLootRollEvenAfterRepeatedLethalHits(GameTestHelper h) {
        var types = new ArrayList<EntityType<?>>(BeastSpawnRules.rules().keySet());
        types.add(ModEntities.VAULT_GUARDIAN.get());
        int checked = 0;
        for (var type : types) for (int realm : BeastRealms.variants(type)) {
            var mob = (Mob)type.create(h.getLevel());
            mob.setNoAi(true);
            ((RealmBeast)mob).setBeastRealm(realm);
            mob.getRandom().setSeed(0); // Keep the initial lethal strike outside the rat's random dodge window.
            mob.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
            h.getLevel().addFreshEntity(mob);
            var dropArea = mob.getBoundingBox().inflate(2);
            var previousItems = h.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea)
                    .stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
            var dropped = new ArrayList<ItemEntity>();
            var snapshots = new ArrayList<ItemStack>();
            int[] events = {0};
            Consumer<LivingDropsEvent> listener = event -> {
                if (event.getEntity() != mob) return;
                events[0]++;
                dropped.addAll(event.getDrops());
                event.getDrops().forEach(item -> snapshots.add(item.getItem().copy()));
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, listener);
            try {
                mob.hurt(mob.damageSources().genericKill(), Float.MAX_VALUE);
                h.assertTrue(!mob.isAlive(), "Lethal hit kills actual beast: " + type + " realm=" + realm);
                mob.invulnerableTime = 0;
                mob.hurt(mob.damageSources().genericKill(), Float.MAX_VALUE);
                mob.die(mob.damageSources().genericKill());
                h.assertTrue(events[0] == 1, "Death loot rolls once despite repeated lethal hits: " + type);
                h.assertTrue(new HashSet<>(dropped).size() == dropped.size(), "Drop list contains no duplicate item entities");
                for (int i = 0; i < dropped.size(); i++) {
                    var item = dropped.get(i);
                    h.assertTrue(h.getLevel().getEntity(item.getUUID()) == item
                                    && ItemStack.matches(snapshots.get(i), item.getItem()),
                            "Exactly one ground entity holds each final loot stack, with unchanged count");
                }
                var ground = h.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea,
                        item -> !previousItems.contains(item.getUUID()));
                var blood = ground.stream().filter(BloodDropEntity.class::isInstance).map(BloodDropEntity.class::cast).toList();
                var ordinary = ground.stream().filter(item -> !(item instanceof BloodDropEntity)).toList();
                h.assertTrue(blood.size() == 1 && blood.getFirst().getAmount() == Math.max(1L, (long)(mob.getMaxHealth() / 10F)),
                        "Repeated death calls create exactly one blood drop with the original amount: " + type + " realm=" + realm
                                + " bloodDrops=" + blood.size());
                h.assertTrue(ordinary.size() == dropped.size() && new HashSet<>(ordinary).equals(new HashSet<>(dropped)),
                        "No extra original-tier or duplicated loot appears on ground: " + type + " realm=" + realm
                                + " expected=" + snapshots + " ground=" + ground.stream().map(ItemEntity::getItem).toList());
                checked++;
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                dropped.forEach(ItemEntity::discard);
                h.getLevel().getEntitiesOfClass(BloodDropEntity.class, dropArea,
                        item -> !previousItems.contains(item.getUUID())).forEach(BloodDropEntity::discard);
                h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class, mob.getBoundingBox().inflate(48),
                        core -> mob.getUUID().equals(core.originalDragon())).forEach(EscapedYuanyingCoreEntity::discard);
                mob.discard();
            }
        }
        h.assertTrue(checked == 41, "Ground-drop quantities checked for all 39 beast and both guardian variants");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void canceledDeathStillAllowsOneLaterDeathAndItsDrops(GameTestHelper h) {
        var beast = ModEntities.AZURE_SPIRIT_SILKWORM.get().create(h.getLevel());
        beast.setNoAi(true);
        beast.setBeastRealm(1);
        beast.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
        h.getLevel().addFreshEntity(beast);
        int[] attempts = {0};
        int[] loot = {0};
        var drops = new ArrayList<ItemEntity>();
        Consumer<LivingDeathEvent> preventFirst = event -> {
            if (event.getEntity() == beast && ++attempts[0] == 1) {
                event.setCanceled(true);
                beast.setHealth(1);
            }
        };
        Consumer<LivingDropsEvent> observe = event -> {
            if (event.getEntity() == beast) {
                loot[0]++;
                drops.addAll(event.getDrops());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, preventFirst);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, observe);
        try {
            beast.setHealth(0);
            beast.die(beast.damageSources().genericKill());
            h.assertTrue(beast.isAlive() && loot[0] == 0, "Canceled death retains life and produces no loot");
            beast.setHealth(0);
            beast.die(beast.damageSources().genericKill());
            beast.die(beast.damageSources().genericKill());
            h.assertTrue(attempts[0] == 2 && loot[0] == 1, "Canceled attempt does not suppress a later accepted death or duplicate rewards");
        } finally {
            NeoForge.EVENT_BUS.unregister(preventFirst);
            NeoForge.EVENT_BUS.unregister(observe);
            drops.forEach(ItemEntity::discard);
            h.getLevel().getEntitiesOfClass(BloodDropEntity.class, beast.getBoundingBox().inflate(2)).forEach(BloodDropEntity::discard);
            beast.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void escapedCoreRepeatedDeathCannotDuplicateCoreOrBlood(GameTestHelper h) {
        for (int realm : new int[]{3, 4, 5}) {
            var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
            dragon.setBeastRealm(realm);
            dragon.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
            var core = new EscapedYuanyingCoreEntity(h.getLevel(), dragon);
            core.setNoAi(true);
            core.setPos(dragon.position());
            h.getLevel().addFreshEntity(core);
            var area = core.getBoundingBox().inflate(2);
            var previous = h.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID)
                    .collect(java.util.stream.Collectors.toSet());
            try {
                core.hurt(core.damageSources().genericKill(), Float.MAX_VALUE);
                core.die(core.damageSources().genericKill());
                core.die(core.damageSources().genericKill());
                var ground = h.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                        item -> !previous.contains(item.getUUID()));
                h.assertTrue((ground.size() == 7 || ground.size() == 8) && ground.stream().filter(BloodDropEntity.class::isInstance).count() == 1
                                && ground.stream().filter(item -> BeastRealmEvents.isCore(item.getItem()))
                                .mapToInt(item -> item.getItem().getCount()).sum() == 1,
                        "Repeated escaped-spirit deaths produce one dragon loot roll and one blood drop for realm " + realm);
            } finally {
                h.getLevel().getEntitiesOfClass(ItemEntity.class, area, item -> !previous.contains(item.getUUID()))
                        .forEach(ItemEntity::discard);
                core.discard();
                dragon.discard();
            }
        }
        h.succeed();
    }
}
