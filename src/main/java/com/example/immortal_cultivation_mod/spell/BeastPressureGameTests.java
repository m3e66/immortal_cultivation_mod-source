package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.BeastRealms;
import com.example.immortal_cultivation_mod.entity.BeastSpawnRules;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.RealmBeast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_beast_pressure")
@PrefixGameTestTemplate(false)
public final class BeastPressureGameTests {
    @GameTest(template = "fate_test")
    public static void goldenCoreCannotSuppressNascentSoulFox(GameTestHelper h) {
        var fox = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        fox.setBeastRealm(4);
        for (String stage : List.of(CultivationLevels.STAGE_EARLY,
                CultivationLevels.STAGE_MID, CultivationLevels.STAGE_LATE)) {
            Weiya.applyPressureEffects(CultivationLevels.REALM_JINDAN + stage, fox);
            assertUnaffected(h, fox, "Golden Core " + stage + " cannot suppress Nascent Soul fox");
        }
        Weiya.applyPressureEffects(CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_MID, fox);
        assertUnaffected(h, fox, "Equal-stage pressure cannot suppress the fox either");
        Weiya.applyPressureEffects(CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, fox);
        h.assertTrue(fox.hasEffect(ModEffects.WEIYA_SUPPRESSED)
                        && fox.getEffect(ModEffects.WEIYA_SUPPRESSED).getAmplifier() == 0,
                "One higher minor stage still applies only minor suppression");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void everySpeciesAndRealmVariantRespectsActualRealm(GameTestHelper h) {
        var types = new ArrayList<>(BeastSpawnRules.rules().keySet());
        types.add(ModEntities.VAULT_GUARDIAN.get());
        for (var type : types) {
            for (int realm : BeastRealms.variants(type)) {
                var beast = (LivingEntity) type.create(h.getLevel());
                ((RealmBeast) beast).setBeastRealm(realm);
                int stage = CultivationLevels.getStageIndex(BeastRealms.level(realm));
                for (int sourceStage = 0; sourceStage <= stage; sourceStage++) {
                    Weiya.applyPressureEffects(CultivationLevels.allLevels().get(sourceStage), beast);
                    assertUnaffected(h, beast, "Lower/equal pressure must not suppress " + type + " realm " + realm);
                }
                Weiya.applyPressureEffects(CultivationLevels.allLevels().get(stage + 1), beast);
                h.assertTrue(beast.hasEffect(ModEffects.WEIYA_SUPPRESSED),
                        "Stronger pressure still suppresses " + type + " realm " + realm);
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void savedAndChangedBeastRealmsControlPressure(GameTestHelper h) {
        var fox = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        fox.setBeastRealm(4);
        var saved = new CompoundTag();
        fox.saveWithoutId(saved);
        var restored = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        restored.load(saved);
        String goldenCore = BeastRealms.level(3);
        Weiya.applyPressureEffects(goldenCore, restored);
        assertUnaffected(h, restored, "Reloaded Nascent Soul fox keeps pressure resistance");
        restored.setBeastRealm(2);
        Weiya.applyPressureEffects(goldenCore, restored);
        h.assertTrue(restored.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Foundation variant can be suppressed by Golden Core");
        restored.removeAllEffects();
        restored.setBeastRealm(4);
        Weiya.applyPressureEffects(goldenCore, restored);
        assertUnaffected(h, restored, "Runtime realm change is read immediately without a stale species cache");
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void activePlayerPressureOnlySuppressesLowerRealmBeasts(GameTestHelper h) {
        var caster = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "beast-pressure"));
        caster.setGameMode(GameType.SURVIVAL);
        caster.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5)));
        ModAttachments.setData(caster, ModAttachments.getData(caster)
                .withCultivationLevel(BeastRealms.level(3)).withQi(1000000));
        var beasts = new ArrayList<com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity>();
        for (int realm : List.of(2, 3, 4)) {
            var beast = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
            beast.setBeastRealm(realm);
            beast.setNoAi(true);
            beast.setPos(caster.position().add(realm, 0, 0));
            h.getLevel().addFreshEntity(beast);
            beasts.add(beast);
        }
        try {
            Weiya.toggle(caster);
            h.assertTrue(caster.hasEffect(ModEffects.WEIYA), "Actual player pressure activates");
            caster.tickCount = 0;
            Weiya.tick(caster);
            h.assertTrue(beasts.get(0).hasEffect(ModEffects.WEIYA_SUPPRESSED), "Golden Core still suppresses Foundation fox");
            assertUnaffected(h, beasts.get(1), "Actual active pressure leaves equal-stage Golden Core fox unaffected");
            assertUnaffected(h, beasts.get(2), "Actual active pressure cannot suppress Nascent Soul fox");
        } finally {
            Weiya.clear(caster);
            beasts.forEach(LivingEntity::discard);
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void ambientPressureUsesRealBeastRealms(GameTestHelper h) {
        var fox = ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        fox.setBeastRealm(4);
        fox.setNoAi(true);
        BlockPos source = h.absolutePos(new BlockPos(4, 2, 4));
        fox.setPos(source.getX() + 1.5, source.getY(), source.getZ() + .5);
        h.getLevel().addFreshEntity(fox);
        try {
            Weiya.applyAmbientPressure(h.getLevel(), source, BeastRealms.level(3));
            assertUnaffected(h, fox, "Actual ambient field cannot suppress higher-realm beast");
            Weiya.applyAmbientPressure(h.getLevel(), source, BeastRealms.level(5));
            h.assertTrue(fox.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Actual stronger ambient field still suppresses beast");
        } finally {
            fox.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void xuanjinSealsUseTheirExactMinorStage(GameTestHelper h) {
        var beast = ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());
        for (int seal = 0; seal <= 6; seal++) {
            beast.setSeal(seal);
            beast.removeAllEffects();
            int stage = XuanjinRules.stage(seal).realmStage();
            Weiya.applyPressureEffects(CultivationLevels.allLevels().get(stage), beast);
            assertUnaffected(h, beast, "Xuanjin seal " + seal + " resists equal-stage pressure");
            Weiya.applyPressureEffects(CultivationLevels.allLevels().get(stage + 1), beast);
            h.assertTrue(beast.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Xuanjin still yields to higher-stage pressure");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void ordinaryAnimalsRemainMortalAndCoreKeepsBeastRealm(GameTestHelper h) {
        var cow = EntityType.COW.create(h.getLevel());
        Weiya.applyPressureEffects(BeastRealms.level(1), cow);
        h.assertTrue(cow.hasEffect(ModEffects.WEIYA_SUPPRESSED), "Uncultivated animal remains mortal");
        var dragon = ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setBeastRealm(4);
        var core = new com.example.immortal_cultivation_mod.entity.EscapedYuanyingCoreEntity(h.getLevel(), dragon);
        Weiya.applyPressureEffects(BeastRealms.level(3), core);
        assertUnaffected(h, core, "Escaped beast soul retains its real realm against lower pressure");
        h.succeed();
    }

    private static void assertUnaffected(GameTestHelper h, LivingEntity beast, String message) {
        h.assertTrue(!beast.hasEffect(ModEffects.WEIYA_SUPPRESSED)
                        && !beast.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        && !beast.hasEffect(MobEffects.WEAKNESS)
                        && !beast.hasEffect(ModEffects.DAZE), message);
    }
}
