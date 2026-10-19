package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_npc_collision")
@PrefixGameTestTemplate(false)
public final class NpcCollisionRegressionGameTests {
    private static ListTag tags(String... values) {
        ListTag tags = new ListTag();
        for (String value : values) tags.add(StringTag.valueOf(value));
        return tags;
    }

    private static LianqiCultivatorEntity npc(GameTestHelper h, String name, String identity) {
        var npc = NpcCombatGameTests.npc(h);
        var profile = NpcProfile.cultivationProfile(npc);
        profile.putString("Name", name); profile.putString("Identity", identity);
        return npc;
    }

    @GameTest(template = "npc_combat_arena")
    public static void repeatedWindBladeChecksDoNotRebuildNpcProfiles(GameTestHelper h) {
        var owner = npc(h, "甲一", "wandering_cultivator");
        var enemy = npc(h, "乙二", "evil_cultivator");
        owner.setTarget(enemy);
        var profile = NpcProfile.cultivationProfile(owner);
        profile.putString("Method", CultivationMethods.BASIC_BREATHING);
        profile.put("Methods", tags(CultivationMethods.BASIC_BREATHING));
        profile.put("Spells", tags(ModSpells.FIREBALL, ModSpells.QINGTENG_FU));
        // These pending edits make any full ensure observable. Combat reads must
        // not clamp fields, repair spell lists or touch stock for every candidate.
        profile.putInt("Thoughts", ModAttachments.MAX_THOUGHTS + 17);
        CompoundTag before = profile.copy(), enemyBefore = NpcProfile.cultivationProfile(enemy).copy();
        var blade = new WindBladeProjectileEntity(h.getLevel(), owner);
        long start = System.nanoTime();
        for (int i = 0; i < 32_000; i++) h.assertTrue(blade.canHitEntity(enemy), "Wind blade must still hit its actual enemy");
        long millis = (System.nanoTime() - start) / 1_000_000;
        h.assertTrue(profile.equals(before) && NpcProfile.cultivationProfile(enemy).equals(enemyBefore),
                "Repeated collision predicates must not rebuild either NPC profile");
        NpcProfile.ensure(owner, NpcProfile.LIANQI);
        h.assertTrue(profile.getInt("Thoughts") == ModAttachments.MAX_THOUGHTS
                        && !profile.getList("Spells", 8).contains(StringTag.valueOf(ModSpells.QINGTENG_FU)),
                "Normal NPC setup still performs pending repairs outside collision checks");
        com.mojang.logging.LogUtils.getLogger().info("NPC collision regression: 32000 real wind-blade predicates in {} ms without profile setup", millis);
        blade.discard(); owner.discard(); enemy.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void currentBondsAndTargetsRemainImmediate(GameTestHelper h) {
        var owner = npc(h, "甲一", "wandering_cultivator");
        var enemy = npc(h, "乙二", "evil_cultivator");
        var bystander = npc(h, "丙三", "sword_cultivator");
        owner.setTarget(enemy);
        var blade = new WindBladeProjectileEntity(h.getLevel(), owner);
        h.assertTrue(blade.canHitEntity(enemy) && !blade.canHitEntity(bystander), "Only combat enemies are hit");
        UUID leader = UUID.randomUUID();
        NpcProfile.cultivationProfile(owner).putUUID("SectLeader", leader);
        NpcProfile.cultivationProfile(enemy).putUUID("SectLeader", leader);
        h.assertTrue(!blade.canHitEntity(enemy), "New alliance immediately prevents friendly fire");
        NpcProfile.cultivationProfile(enemy).remove("SectLeader");
        h.assertTrue(blade.canHitEntity(enemy), "Removing alliance immediately restores hostile targeting");
        NpcProfile.cultivationProfile(bystander).putString("Identity", "evil_cultivator");
        h.assertTrue(blade.canHitEntity(bystander), "Current enemy's allies remain valid combat targets");
        NpcProfile.cultivationProfile(bystander).putString("Identity", "wandering_cultivator");
        h.assertTrue(!blade.canHitEntity(bystander), "New own ally remains protected without waiting for a cache refresh");
        blade.discard(); owner.discard(); enemy.discard(); bystander.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void coldProfilesStillInitializeAndMigrate(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        var target = EntityType.ZOMBIE.create(h.getLevel());
        NpcProfile.areCombatAllies(npc, target);
        var profile = NpcProfile.cultivationProfile(npc);
        h.assertTrue(profile.getBoolean("Initialized") && profile.getInt("ProfileVersion") > 0,
                "A fresh NPC initializes on its first combat lookup");
        int current = profile.getInt("ProfileVersion");
        profile.putInt("ProfileVersion", 0);
        NpcProfile.areCombatAllies(npc, target);
        h.assertTrue(profile.getInt("ProfileVersion") == current, "Legacy NPC still migrates before combat bond reads");
        var dummy = ModEntities.TRAINING_DUMMY.get().create(h.getLevel());
        dummy.ensureProfile();
        var before = NpcProfile.cultivationProfile(dummy).copy();
        NpcProfile.areCombatAllies(dummy, npc);
        h.assertTrue(NpcProfile.cultivationProfile(dummy).equals(before), "Dummy profile keeps its separate setup lifecycle");
        dummy.discard(); npc.discard(); target.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void vanillaCollisionRejectionPrecedesNpcSetup(GameTestHelper h) {
        var owner = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        var blade = new WindBladeProjectileEntity(h.getLevel(), owner);
        var marker = new ArmorStand(EntityType.ARMOR_STAND, h.getLevel());
        var saved = marker.saveWithoutId(new CompoundTag()); saved.putBoolean("Marker", true); marker.load(saved);
        h.assertTrue(!blade.canHitEntity(marker), "Marker armor stand cannot be hit");
        h.assertTrue(!NpcProfile.cultivationProfile(owner).getBoolean("Initialized"),
                "Rejected vanilla candidates must not initialize NPC profiles");
        blade.discard(); marker.discard(); owner.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void orphanedWindBladesStillFailClosed(GameTestHelper h) {
        var owner = npc(h, "甲一", "wandering_cultivator");
        var enemy = npc(h, "乙二", "evil_cultivator"); owner.setTarget(enemy);
        var blade = new WindBladeProjectileEntity(h.getLevel(), owner);
        h.assertTrue(blade.canHitEntity(enemy), "Living NPC owner can attack enemy");
        owner.discard();
        h.assertTrue(!blade.canHitEntity(enemy), "Removed NPC owner cannot cause collateral hits");
        blade.discard(); enemy.discard(); h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void protectedModesStillOverrideHostility(GameTestHelper h) {
        NpcTargetPriorityGameTests.protectedModesOverrideHatredRevengeSparAndNpcEffects(h);
    }

    @GameTest(template = "npc_combat_arena")
    public static void ordinarySpellRepairStillRuns(GameTestHelper h) {
        NpcSpellPoolGameTests.savedNpcDropsOrphanArtsButKeepsOrdinaryLearnedSpells(h);
    }

    @GameTest(template = "npc_combat_arena")
    public static void knownMethodsStillGrantTheirArts(GameTestHelper h) {
        NpcSpellPoolGameTests.actualKnownMethodsKeepTheirLegitimateTechniques(h);
    }

    @GameTest(template = "npc_combat_arena", timeoutTicks = 30)
    public static void missedWindBladeStillExpires(GameTestHelper h) {
        com.example.immortal_cultivation_mod.effect.SpellRibbonGameTests.missedSpellProjectileExpiresThroughTickEvent(h);
    }
}
