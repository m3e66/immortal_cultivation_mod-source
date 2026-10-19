package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.BeastSpawnEggItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.PhantomVeilFurItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_realms")
@PrefixGameTestTemplate(false)
public final class BeastRealmGameTests {
    @GameTest(template = "fate_test")
    public static void everySpeciesHasThreeSavedVariantsWithScaledHealthAndOneEgg(GameTestHelper h) {
        h.assertTrue(BeastSpawnRules.rules().size() == 13, "One entity registry entry for each wild beast species");
        for (var type : BeastSpawnRules.rules().keySet()) {
            var realms = BeastRealms.variants(type);
            h.assertTrue(realms.size() == 3 && SpawnEggItem.byId(type) instanceof BeastSpawnEggItem,
                    "Species has three realms and one shared egg: " + type);
            double previousHealth = 0;
            for (int realm : realms) {
                Mob beast = (Mob)type.create(h.getLevel());
                ((RealmBeast)beast).setBeastRealm(realm);
                h.assertTrue(beast.getMaxHealth() > previousHealth, "Higher variants have greater health: " + type);
                previousHealth = beast.getMaxHealth();
                h.assertTrue(beast.getName().getString().startsWith("["), "Realm is a bracket prefix before species name");
                beast.setHealth(beast.getMaxHealth() * .37F);
                var saved = new CompoundTag();
                beast.saveWithoutId(saved);
                Mob restored = (Mob)type.create(h.getLevel());
                restored.load(saved);
                restored.load(saved);
                h.assertTrue(((RealmBeast)restored).beastRealm() == realm
                                && Math.abs(restored.getMaxHealth() - beast.getMaxHealth()) < .01
                                && Math.abs(restored.getHealth() - beast.getHealth()) < .01,
                        "Repeated load preserves realm, injury and attributes without multiplying modifiers");
            }
        }
        h.assertTrue(BeastRealms.variants(ModEntities.XUANJIN_ZHENG.get()).isEmpty(), "Xuanjin remains excluded");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void unifiedEggActuallySpawnsSelectedRealmAndCyclesBackToRandom(GameTestHelper h) {
        var type = ModEntities.PHANTOM_VEIL_FOX.get();
        var egg = (BeastSpawnEggItem)ModItems.PHANTOM_VEIL_FOX_SPAWN_EGG.get();
        var stack = new ItemStack(egg);
        for (int realm : BeastRealms.variants(type)) {
            egg.cycleRealm(stack);
            h.assertTrue(BeastSpawnEggItem.selectedRealm(stack) == realm && egg.getType(stack) == type,
                    "One egg cycles only the species' supported realms");
            var beast = type.spawn(h.getLevel(), stack, null, h.absolutePos(new BlockPos(3, 2, 3)),
                    MobSpawnType.SPAWN_EGG, false, false);
            try {
                h.assertTrue(beast != null && beast.beastRealm() == realm && beast.getHealth() > 0
                                && beast.realmTier().ordinal() == realm - 2,
                        "Vanilla egg spawn transfers realm data, attributes and fox techniques");
            } finally { if (beast != null) beast.discard(); }
        }
        egg.cycleRealm(stack);
        h.assertTrue(BeastSpawnEggItem.selectedRealm(stack) == -1, "Egg cycles back to random without duplicate egg IDs");
        for (int i = 0; i < 2; i++) {
            String legacy = (i == 0 ? "zhuji" : "jindan") + "_phantom_veil_fox";
            var id = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", legacy);
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(id) == type
                            && BuiltInRegistries.ITEM.get(id.withPath(legacy + "_spawn_egg")) == egg,
                    "Old entity and egg IDs resolve to the unified species");
            var old = new CompoundTag();
            type.create(h.getLevel()).saveWithoutId(old);
            old.remove(BeastRealms.NBT_KEY);
            old.putString("id", id.toString());
            var migrated = EntityType.loadEntityRecursive(old, h.getLevel(), entity -> entity);
            h.assertTrue(migrated instanceof PhantomVeilFoxEntity fox && fox.beastRealm() == i + 2,
                    "Loading old fox IDs preserves their original realm");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void dropsAndDamageFollowActualRealmAndSpacingUsesVariantRatherThanSpecies(GameTestHelper h) {
        var type = ModEntities.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get();
        var rat = type.create(h.getLevel());
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        rat.setNoAi(true);
        rat.setBeastRealm(2);
        rat.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        h.getLevel().addFreshEntity(rat);
        try {
            h.assertTrue(!BeastSpawnRules.hasRealmSeparation(2, h.getLevel(), pos)
                            && BeastSpawnRules.hasRealmSeparation(1, h.getLevel(), pos),
                    "Foundation rat blocks Foundation beasts, not Qi Refining beasts");
            h.assertTrue(BeastRealmEvents.realmDrop(new ItemStack(ModItems.LIANQI_YAODAN.get()), 2)
                            .is(ModItems.ZHUJI_YAODAN.get())
                            && BeastRealmEvents.realmDrop(new ItemStack(ModItems.YUANYING_YAODAN.get()), 5)
                            .is(ModItems.HUASHEN_YAODAN.get()),
                    "Core tiers match the actual variant, including new higher realms");
            h.assertTrue(BeastRealmEvents.realmDrop(new ItemStack(ModItems.LIANQI_YAODAN.get()), 0).isEmpty(),
                    "Mortal variants have no spiritual beast core");
            var fur = BeastRealmEvents.realmDrop(new ItemStack(ModItems.PHANTOM_VEIL_FUR.get()), 2);
            h.assertTrue(PhantomVeilFurItem.realmIndex(fur) == 2 && fur.getHoverName().getString().startsWith("["),
                    "Fox fur preserves its forging realm and bracketed name");
            var target = EntityType.COW.create(h.getLevel());
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2_000);
            target.setHealth(2_000);
            target.setNoAi(true);
            h.getLevel().addFreshEntity(target);
            try {
                rat.setBeastRealm(1);
                target.hurt(rat.damageSources().mobAttack(rat), 10);
                float baseLoss = 2_000 - target.getHealth();
                target.invulnerableTime = 0;
                target.setHealth(2_000);
                rat.setBeastRealm(2);
                target.hurt(rat.damageSources().mobAttack(rat), 10);
                h.assertTrue(baseLoss > 0 && Math.abs((2_000 - target.getHealth()) / baseLoss
                                - BeastRealms.powerScale(rat)) < .001,
                        "Actual server damage, including fixed-damage techniques, scales with realm");
            } finally { target.discard(); }
        } finally { rat.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void jeiShowsRealmSpecificDropsForUnifiedSpecies(GameTestHelper h) {
        var recipes = com.example.immortal_cultivation_mod.compat.jei.AcquisitionCatalog.collect();
        for (int realm : BeastRealms.variants(ModEntities.PHANTOM_VEIL_FOX.get())) {
            var core = BeastRealmEvents.realmDrop(new ItemStack(ModItems.YUANYING_YAODAN.get()), realm);
            h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.result().is(core.getItem())
                            && recipe.sourceIcon().is(ModItems.PHANTOM_VEIL_FOX_SPAWN_EGG.get())
                            && BeastSpawnEggItem.selectedRealm(recipe.sourceIcon()) == realm),
                    "JEI core acquisition is discoverable for every unified fox variant");
        }
        h.assertTrue(recipes.stream().map(recipe -> recipe.id()).distinct().count() == recipes.size(),
                "Realm-expanded JEI entries keep unique identities");
        h.succeed();
    }
}
