package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.compat.jei.AcquisitionCatalog;
import com.example.immortal_cultivation_mod.item.BeastSpawnEggItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_realms")
@PrefixGameTestTemplate(false)
public final class BeastDropGameTests {
    private static final List<String> REALMS = List.of("mortal", "lianqi", "zhuji", "jindan", "yuanying",
            "huashen", "lianxu", "heti", "dacheng", "dujie");

    private static Item expected(int realm, String suffix) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", REALMS.get(realm) + suffix));
    }

    private record Death(List<ItemStack> drops, List<EscapedYuanyingCoreEntity> escaped) { }

    private static void assertDragonLoot(GameTestHelper h, List<ItemStack> drops, int realm) {
        assertCount(h, drops, ModItems.CHI_LIN.get(), 6, 14);
        assertCount(h, drops, ModItems.CHIYAN_BONE.get(), 2, 5);
        assertCount(h, drops, ModItems.HUO_XI_BLOOD.get(), 2, 4);
        assertCount(h, drops, ModItems.YANJIAO_HORN.get(), 1, 1);
        assertCount(h, drops, expected(realm, "_beast_meat"), 3, 6);
        assertCount(h, drops, expected(realm, "_yaodan"), 1, 1);
        var bonuses = drops.stream().filter(stack -> stack.is(ModItems.HIGH_SWORD_GRINDSTONE.get())
                || stack.is(ModItems.HIGH_SABER_GRINDSTONE.get()) || stack.is(ModItems.HIGH_YANGJIAN_STONE.get())).toList();
        h.assertTrue(bonuses.size() <= 1 && bonuses.stream().allMatch(stack -> stack.getCount() == 1)
                        && drops.size() == 6 + bonuses.size(),
                "One full dragon loot roll, including its optional weapon-care bonus");
    }

    private static void assertCount(GameTestHelper h, List<ItemStack> drops, Item item, int min, int max) {
        var matches = drops.stream().filter(stack -> stack.is(item)).toList();
        h.assertTrue(matches.size() == 1 && matches.getFirst().getCount() >= min && matches.getFirst().getCount() <= max,
                "Exactly one stack in the original quantity range: " + item);
    }

    /** Observe the registered handlers during an actual vanilla death, rather than calling the conversion helper. */
    private static Death kill(GameTestHelper h, Mob beast) {
        var entities = new ArrayList<ItemEntity>();
        boolean[] observed = {false};
        Consumer<LivingDropsEvent> record = event -> {
            if (event.getEntity() == beast) {
                observed[0] = true;
                entities.addAll(event.getDrops());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, record);
        try {
            beast.setNoAi(true);
            beast.setPos(h.absoluteVec(new Vec3(3, 3, 3)));
            if (h.getLevel().getEntity(beast.getUUID()) != beast) h.getLevel().addFreshEntity(beast);
            beast.setHealth(0);
            beast.die(h.getLevel().damageSources().genericKill());
            h.assertTrue(observed[0], "Real death invokes the living-drops pipeline");
            var escaped = h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class,
                    beast.getBoundingBox().inflate(48), core -> beast.getUUID().equals(core.originalDragon()));
            return new Death(entities.stream().map(drop -> drop.getItem().copy()).toList(), escaped);
        } finally {
            NeoForge.EVENT_BUS.unregister(record);
            entities.forEach(ItemEntity::discard);
            h.getLevel().getEntitiesOfClass(com.example.immortal_cultivation_mod.flame.SpiritFlameEntity.class,
                    beast.getBoundingBox().inflate(48), flame -> flame.getPersistentData().getString("icmFlameSource")
                            .endsWith(beast.getUUID().toString())).forEach(net.minecraft.world.entity.Entity::discard);
            beast.discard();
        }
    }

    @GameTest(template = "fate_test")
    public static void everyActualBeastDeathDropsItsRealmMeatAndNeverWrongCore(GameTestHelper h) {
        int checked = 0;
        for (var type : BeastSpawnRules.rules().keySet()) for (int realm : BeastRealms.variants(type)) {
            var beast = (Mob)type.create(h.getLevel());
            ((RealmBeast)beast).setBeastRealm(realm);
            var death = kill(h, beast);
            try {
                var meat = death.drops().stream().filter(BeastRealmEvents::isMeat).toList();
                var cores = death.drops().stream().filter(BeastRealmEvents::isCore).toList();
                boolean dragon = type == ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get();
                h.assertTrue(dragon ? death.drops().isEmpty()
                                : !meat.isEmpty() && meat.stream().allMatch(drop -> drop.is(expected(realm, "_beast_meat"))),
                        "Actual meat drop matches variant: " + type + " realm=" + realm);
                h.assertTrue(cores.stream().mapToInt(ItemStack::getCount).sum() <= 1
                                && (realm == 0 ? cores.isEmpty() : cores.stream().allMatch(drop -> drop.is(expected(realm, "_yaodan")))),
                        "Any actual core drop matches variant and retains one-core cap: " + type + " realm=" + realm);
                if (type == ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get()) {
                    h.assertTrue(cores.isEmpty() && death.escaped().size() == 1,
                            "Every dragon variant releases exactly one spirit and no body loot");
                }
                checked++;
            } finally { death.escaped().forEach(net.minecraft.world.entity.Entity::discard); }
        }
        h.assertTrue(checked == 39, "All thirteen species and all three realm variants checked through real deaths");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void escapedDragonCoreDeathAndBothCaptureItemsKeepOriginalRealm(GameTestHelper h) {
        for (int realm : new int[]{3, 4, 5}) for (int method = 0; method < 3; method++) {
            var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
            dragon.setBeastRealm(realm);
            var death = kill(h, dragon);
            h.assertTrue(death.escaped().size() == 1, "Higher dragon releases its real escaped core");
            var core = death.escaped().getFirst();
            try {
                h.assertTrue(core.coreRealm() == realm, "Escaped core inherits actual dragon realm");
                if (method == 0) {
                    var coreDeath = kill(h, core);
                    var cores = coreDeath.drops().stream().filter(BeastRealmEvents::isCore).toList();
                    h.assertTrue(cores.size() == 1 && cores.getFirst().is(expected(realm, "_yaodan"))
                                    && cores.getFirst().getCount() == 1,
                            "Killing escaped spirit drops full dragon loot and exactly one realm-matched core");
                    assertDragonLoot(h, coreDeath.drops(), realm);
                    h.assertTrue(coreDeath.drops().stream().filter(BeastRealmEvents::isMeat).allMatch(stack->stack.is(expected(realm,"_beast_meat"))),"Spirit meat matches original realm");
                } else {
                    var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "core-capture"));
                    var area = core.getBoundingBox().inflate(3);
                    var previous = h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
                    try {
                        var vessel = new ItemStack(method == 1 ? Items.GLASS_BOTTLE
                                : com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get());
                        player.setItemInHand(InteractionHand.MAIN_HAND, vessel);
                        h.assertTrue(core.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(), "Capture succeeds");
                        int count = 0;
                        for (var stack : player.getInventory().items) if (stack.is(expected(realm, "_yaodan"))) count += stack.getCount();
                        h.assertTrue(count == 1 && core.isRemoved(), "Either capture item yields exactly one matching core");
                        var loot = h.getLevel().getEntitiesOfClass(ItemEntity.class,area,item->!previous.contains(item.getUUID()));
                        var completeLoot = new ArrayList<>(loot.stream().map(ItemEntity::getItem).toList());
                        completeLoot.add(new ItemStack(expected(realm, "_yaodan")));
                        assertDragonLoot(h, completeLoot, realm);
                        h.assertTrue(loot.stream().filter(item->BeastRealmEvents.isMeat(item.getItem())).allMatch(item->item.getItem().is(expected(realm,"_beast_meat"))),"Captured spirit's meat follows original realm");
                        h.assertTrue(!core.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"Removed spirit cannot be harvested twice");
                    } finally {
                        h.getLevel().getEntitiesOfClass(ItemEntity.class,area,item->!previous.contains(item.getUUID())).forEach(ItemEntity::discard);
                        h.getLevel().getEntitiesOfClass(com.example.immortal_cultivation_mod.flame.SpiritFlameEntity.class,area.inflate(8),flame->flame.getPersistentData().getString("icmFlameSource").endsWith(core.getUUID().toString())).forEach(net.minecraft.world.entity.Entity::discard);
                        player.discard();
                    }
                }
            } finally { core.discard(); }
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void dragonSpiritOwnsRareFlameInsteadOfBody(GameTestHelper h) {
        com.example.immortal_cultivation_mod.flame.FlameAcquisitionGameTests.flameDeathsUseActualRealmRootsAndUndeadRank(h);
    }

    @GameTest(template="fate_test")
    public static void actualDragonSpiritFlameCanBeCaptured(GameTestHelper h) {
        com.example.immortal_cultivation_mod.flame.FlameAcquisitionIntegrationGameTests.allDeathSourcesProduceFlamesThatSurvivalPlayersCanCapture(h);
    }

    @GameTest(template="fate_test")
    public static void savedSpiritRevivalDoesNotReleaseLootBeforeFinalDefeat(GameTestHelper h) {
        for(int realm:new int[]{3,4,5}) {
            var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());dragon.setBeastRealm(realm);
            var death=kill(h,dragon);var spirit=death.escaped().getFirst();
            var saved=spirit.saveWithoutId(new net.minecraft.nbt.CompoundTag());saved.putInt("RevivalTicks",1);spirit.discard();
            var loaded=ModEntities.ESCAPED_YUANYING_CORE.get().create(h.getLevel());loaded.load(saved);loaded.setPos(h.absoluteVec(new Vec3(3,3,3)));h.getLevel().addFreshEntity(loaded);
            var before=h.getLevel().getEntitiesOfClass(CrimsonSkyFlameFloodDragonEntity.class,loaded.getBoundingBox().inflate(48)).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            loaded.tick();
            var revived=h.getLevel().getEntitiesOfClass(CrimsonSkyFlameFloodDragonEntity.class,loaded.getBoundingBox().inflate(48),mob->!before.contains(mob.getUUID()));
            h.assertTrue(revived.size()==1 && revived.getFirst().beastRealm()==realm && revived.getFirst().getUUID().equals(dragon.getUUID()),"Saved escaped spirit retains realm and revives one body with the original identity");
            var second=kill(h,revived.getFirst());h.assertTrue(second.drops().isEmpty()&&second.escaped().size()==1,"Revived body still cannot grant dragon loot");
            var finalDeath=kill(h,second.escaped().getFirst());
            assertDragonLoot(h, finalDeath.drops(), realm);
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void canceledDragonDeathAndCanceledCaptureKeepRewardsAvailable(GameTestHelper h) {
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());dragon.setPos(h.absoluteVec(new Vec3(3,3,3)));h.getLevel().addFreshEntity(dragon);
        Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> cancel=e->{if(e.getEntity()==dragon){e.setCanceled(true);dragon.setHealth(1);}};
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,cancel);
        try {
            dragon.setHealth(0);dragon.die(h.getLevel().damageSources().genericKill());
            h.assertTrue(h.getLevel().getEntitiesOfClass(EscapedYuanyingCoreEntity.class,dragon.getBoundingBox().inflate(48),core->dragon.getUUID().equals(core.originalDragon())).isEmpty(),"Canceled body death releases no spirit");
        } finally {NeoForge.EVENT_BUS.unregister(cancel);}
        var death=kill(h,dragon);h.assertTrue(death.escaped().size()==1,"Accepted death after cancellation still releases its spirit");var spirit=death.escaped().getFirst();
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"capture-cancel"));var bottle=new ItemStack(Items.GLASS_BOTTLE);player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        Consumer<LivingDropsEvent> cancelLoot=e->{if(e.getEntity()==spirit)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,cancelLoot);
        try {
            h.assertTrue(!spirit.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction() && bottle.getCount()==1 && !spirit.isRemoved(),"Canceled harvest consumes nothing and preserves spirit");
        } finally {NeoForge.EVENT_BUS.unregister(cancelLoot);}
        var defeated=kill(h,spirit);assertDragonLoot(h,defeated.drops(),dragon.beastRealm());player.discard();h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void duplicateCoreDropsNormalizeAndCapAtOneThroughRegisteredEvents(GameTestHelper h) {
        for (int realm : BeastRealms.variants(ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get())) {
            var beast = ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get().create(h.getLevel());
            beast.setBeastRealm(realm);
            var drops = new ArrayList<ItemEntity>();
            for (var item : List.of(ModItems.LIANQI_YAODAN.get(), ModItems.YUANYING_YAODAN.get(), ModItems.DUJIE_YAODAN.get()))
                drops.add(new ItemEntity(h.getLevel(), 0, 0, 0, new ItemStack(item, 3)));
            drops.add(new ItemEntity(h.getLevel(), 0, 0, 0, new ItemStack(ModItems.CHI_LIN.get(), 7)));
            try {
                NeoForge.EVENT_BUS.post(new LivingDropsEvent(beast, h.getLevel().damageSources().genericKill(), drops, false));
                var cores = drops.stream().filter(drop -> BeastRealmEvents.isCore(drop.getItem())).toList();
                h.assertTrue(cores.size() == 1 && cores.getFirst().getItem().is(expected(realm, "_yaodan"))
                                && cores.getFirst().getItem().getCount() == 1,
                        "Registered realm conversion and existing core cap compose correctly");
                h.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(ModItems.CHI_LIN.get())
                                && drop.getItem().getCount() == 7), "Core cap does not discard species materials");
            } finally { drops.forEach(ItemEntity::discard); beast.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void jeiListsMatchingMeatAndCoreForEveryVariantWithCorrectDragonRoute(GameTestHelper h) {
        var recipes = AcquisitionCatalog.collect();
        for (var type : BeastSpawnRules.rules().keySet()) for (int realm : BeastRealms.variants(type)) {
            var source = recipes.stream().filter(recipe -> recipe.sourceIcon().is(SpawnEggItem.byId(type))
                    && BeastSpawnEggItem.selectedRealm(recipe.sourceIcon()) == realm).toList();
            h.assertTrue(source.stream().anyMatch(recipe -> recipe.result().is(expected(realm, "_beast_meat"))),
                    "JEI shows correct realm meat: " + type + " realm=" + realm);
            var cores = source.stream().filter(recipe -> BeastRealmEvents.isCore(recipe.result())).toList();
            h.assertTrue(realm == 0 ? cores.isEmpty() : !cores.isEmpty()
                            && cores.stream().allMatch(recipe -> recipe.result().is(expected(realm, "_yaodan"))),
                    "JEI shows matching spiritual cores and no mortal cores");
            if (type == ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get()) {
                h.assertTrue(source.stream().allMatch(recipe -> recipe.id().getPath().contains("entities/escaped_yuanying_core/")),
                        "JEI routes every dragon reward through the escaped spirit for all variants");
            }
        }
        h.succeed();
    }
}
