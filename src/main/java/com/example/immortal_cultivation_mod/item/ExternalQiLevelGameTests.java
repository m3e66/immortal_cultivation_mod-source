package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.entity.NpcCombatGameTests;
import com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.combat.ExternalQiSize;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_external_qi")
@PrefixGameTestTemplate(false)
public final class ExternalQiLevelGameTests {
    @GameTest(template = "fate_test")
    public static void scrollingRequiresShiftAndRejectsDuplicateTick(GameTestHelper h) {
        var player=player(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));player.setShiftKeyDown(false);
        ExternalQiSize.adjust(player,1);h.assertTrue(ExternalQiSize.scale(player)==1,"Normal scrolling cannot resize external Qi");
        player.setShiftKeyDown(true);ExternalQiSize.adjust(player,1);h.assertTrue(ExternalQiSize.scale(player)==1.1,"Shift-scroll changes size by ten percent");
        ExternalQiSize.adjust(player,1);h.assertTrue(ExternalQiSize.scale(player)==1.1,"Duplicate same-tick scroll cannot apply twice");
        player.discard();h.succeed();
    }
    private static FakePlayer player(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "qi-level"));
        player.setPos(h.absoluteVec(new Vec3(4, 3, 3)));
        QiInfusedWeapon.setOutwardQiRelease(player, true);
        QiInfusedWeapon.setOutwardQiAutoFit(player, false);
        CombatStateManager.enterCombat(player);
        return player;
    }

    private static CompoundTag shot(GameTestHelper h, FakePlayer player) {
        var shots = h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class, player.getBoundingBox().inflate(3));
        h.assertTrue(shots.size() == 1, "Exactly one real projectile releases");
        var tag = new CompoundTag();
        shots.getFirst().saveWithoutId(tag);
        shots.getFirst().discard();
        return tag;
    }

    private static CompoundTag release(GameTestHelper h, FakePlayer player, ItemStack weapon, double qi) {
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        QiInfusedWeapon.setQi(weapon, qi);
        h.assertTrue(WeaponQiSlashEntity.release(player, 2, false), "Real player release succeeds");
        var tag = shot(h, player);
        h.assertTrue(Math.abs(QiInfusedWeapon.getQi(weapon) - qi * .9) < .001,
                "Weapon-level sizing preserves ten-percent injected Qi payment");
        return tag;
    }

    @GameTest(template = "fate_test")
    public static void actualReleaseSeparatesWidthFromDamageAndTracksWeaponSwap(GameTestHelper h) {
        var player = player(h);
        var weapon = new ItemStack(Items.IRON_SWORD);
        var low = release(h, player, weapon, 100);
        var high = release(h, player, weapon, 100000);
        h.assertTrue(high.getDouble("Radius") == low.getDouble("Radius")
                        && high.getFloat("Damage") > low.getFloat("Damage")
                        && high.getInt("RemainingBlocks") > low.getInt("RemainingBlocks"),
                "More Qi changes damage and terrain capacity, not weapon-level width");
        double previous = low.getDouble("Radius");
        for (int tier = 1; tier <= WeaponRestrictionSystem.MAX_TIER; tier++) {
            WeaponRestrictionSystem.setTier(weapon, tier);
            var promoted = release(h, player, weapon, 100);
            h.assertTrue(promoted.getDouble("Radius") > previous, "Each weapon promotion enlarges actual projectile");
            previous = promoted.getDouble("Radius");
        }
        var swapped = release(h, player, new ItemStack(Items.IRON_SWORD), 100);
        h.assertTrue(swapped.getDouble("Radius") == low.getDouble("Radius"),
                "Switching to a low-level weapon immediately restores its smaller width");
        player.getPersistentData().putInt("ExternalQiSizePercent", 50);
        var adjusted = release(h, player, weapon, 100);
        h.assertTrue(adjusted.getDouble("Radius") == previous * .5, "Saved scroll preference scales current weapon level");
        player.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void intrinsicHigherWeaponsAndPromotionsSetStartingWidth(GameTestHelper h) {
        var player = player(h);
        double mortal = release(h, player, new ItemStack(Items.IRON_SWORD), 100).getDouble("Radius");
        var foundation = new ItemStack(BuiltInRegistries.ITEM.get(HigherWeaponItem.id(HigherWeaponSpec.BLACK_IRON_JIAN.id)));
        var golden = new ItemStack(BuiltInRegistries.ITEM.get(HigherWeaponItem.id(HigherWeaponSpec.SEVEN_STAR_SWORD.id)));
        double foundationRadius = release(h, player, foundation, 100).getDouble("Radius");
        double goldenRadius = release(h, player, golden, 100).getDouble("Radius");
        h.assertTrue(mortal < foundationRadius && foundationRadius < goldenRadius,
                "Intrinsic Foundation and Golden Core gear retain distinct starting widths without restrictions");
        WeaponRestrictionSystem.setTier(golden, WeaponRestrictionSystem.MAX_TIER);
        var promoted = release(h, player, golden, 100);
        h.assertTrue(promoted.getDouble("Radius") > goldenRadius, "High-tier promotion overrides intrinsic starting level");
        h.assertTrue(WeaponRestrictionSystem.tier(foundation) == 0, "Sizing does not rewrite existing weapon tier");
        h.assertTrue(Double.isFinite(ExternalQiSize.radius(golden, Double.NaN, false, false, false))
                        && ExternalQiSize.radius(golden, Double.POSITIVE_INFINITY, false, false, false)
                            == promoted.getDouble("Radius"), "Invalid size factors fall back to a finite default");
        player.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void actualWhipWidthTracksWeaponLevelRatherThanDamage(GameTestHelper h) {
        var player = player(h);
        var whip = new ItemStack(ModItems.WHIP.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, whip);
        h.assertTrue(WeaponQiSlashEntity.releaseWhip(player, 10, 5, 8), "Low-damage whip releases");
        var low = shot(h, player);
        h.assertTrue(WeaponQiSlashEntity.releaseWhip(player, 1000, 500, 8), "High-damage whip releases");
        var high = shot(h, player);
        h.assertTrue(high.getDouble("Radius") == low.getDouble("Radius"), "Whip damage does not enlarge its width");
        WeaponRestrictionSystem.setTier(whip, 4);
        h.assertTrue(WeaponQiSlashEntity.releaseWhip(player, 10, 5, 8), "Promoted whip releases");
        var promoted = shot(h, player);
        h.assertTrue(promoted.getDouble("Radius") > low.getDouble("Radius")
                        && promoted.getFloat("Damage") == low.getFloat("Damage")
                        && promoted.getDouble("MaxDistance") == 8 && !promoted.getBoolean("BlockBreaking"),
                "Whip promotion changes width while retaining damage, range, and no terrain destruction");
        player.discard(); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void widthCapsAndGeometry(GameTestHelper h) {
        WeaponQiGameTests.externalQiWidthGrowsWithWeaponLevelAndStaysBounded(h);
    }
    @GameTest(template = "fate_test")
    public static void sharedInjectionAndOverflow(GameTestHelper h) {
        WeaponQiGameTests.oldAndNewWeaponsShareInjectionAutoChannelAndOverflow(h);
    }
    @GameTest(template = "fate_test")
    public static void unchangedQiConversion(GameTestHelper h) {
        WeaponQiGameTests.newWeaponsShowQiAndUseHundredToFiveConversion(h);
    }
    @GameTest(template = "fate_test")
    public static void immediateCollision(GameTestHelper h) {
        WeaponQiGameTests.playerExternalQiCollidesImmediately(h);
    }
    @GameTest(template = "fate_test")
    public static void heavyAttackTerrainGate(GameTestHelper h) {
        WeaponQiGameTests.externalQiBreaksTerrainOnlyForEnabledHeavyAttacks(h);
    }
    @GameTest(template = "npc_combat_arena")
    public static void npcWidthAndCollision(GameTestHelper h) {
        NpcCombatGameTests.realNpcSlashUsesWeaponLevelCollisionRadius(h);
    }
    @GameTest(template = "npc_combat_arena")
    public static void enclosureFitting(GameTestHelper h) {
        ExternalQiSpaceFitGameTests.spaceFitDetectsEnclosuresButNotOutdoorGround(h);
    }
    @GameTest(template = "npc_combat_arena")
    public static void fittingPreservesCostDamageAndPersistence(GameTestHelper h) {
        ExternalQiSpaceFitGameTests.fittedReleaseKeepsDamageCostAndSavedSize(h);
    }
}
