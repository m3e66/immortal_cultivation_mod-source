package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.fate.LuckSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_npc_breakthrough_pill")
@PrefixGameTestTemplate(false)
public final class NpcBreakthroughPillGameTests {
    private static final String LATE = CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE;

    @GameTest(template="fate_test")
    public static void pillPromotesLateLianqiAndPreservesIdentityAndOtherInventory(GameTestHelper h) {
        NpcCultivationGameTests.majorBreakthroughPreservesNpcAndUpgradesStats(h);
    }

    @GameTest(template="fate_test")
    public static void fullProgressWithoutPillWaitsWithoutSpendingQi(GameTestHelper h) {
        var npc = ready(h, 0);
        var profile = NpcProfile.cultivationProfile(npc);
        train(npc, NpcCultivation.CHANNEL_TICKS + 1);
        h.assertTrue(LATE.equals(npc.npcRealm()), "Full progress alone cannot skip the breakthrough pill");
        h.assertTrue(npc.npcCultivationState() == NpcCultivation.MEDITATING, "NPC keeps meditating until it has a pill");
        h.assertTrue(profile.getInt("Qi") == profile.getInt("MaxQi"), "Waiting for a pill spends no breakthrough Qi");
        h.assertTrue(profile.getCompound(NpcCultivation.TRAINING).getInt("Attempts") == 0, "No failed rolls without a pill");
        npc.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void failedAttemptConsumesOnlyOnePill(GameTestHelper h) {
        var npc = ready(h, 2);
        LuckSystem.data(npc).putInt("Luck", 0);
        train(npc, NpcCultivation.CHANNEL_TICKS + 1);
        h.assertTrue(LATE.equals(npc.npcRealm()), "Pill does not bypass success chance");
        h.assertTrue(pills(npc) == 1, "Failed attempt consumes exactly one of two pills");
        h.assertTrue("failed".equals(NpcProfile.cultivationProfile(npc).getCompound(NpcCultivation.TRAINING).getString("LastOutcome")), "Failure is recorded");
        npc.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void savedChannelConsumesOnceAndUpgradesRealm(GameTestHelper h) {
        var npc = ready(h, 2);
        train(npc, 31);
        h.assertTrue(npc.npcCultivationState() == NpcCultivation.BREAKTHROUGH, "Pill permits channeling");
        var save = new CompoundTag(); npc.saveWithoutId(save);
        var loaded = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel()); loaded.load(save);
        loaded.setOnGround(true);
        train(loaded, NpcCultivation.CHANNEL_TICKS);
        h.assertTrue(CultivationLevels.getNextStage(LATE).equals(loaded.npcRealm()), "Saved channel resumes into Zhuji");
        h.assertTrue(pills(loaded) == 1, "Reloaded attempt consumes one pill exactly once");
        npc.discard(); loaded.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void removedPillCancelsChannelWithoutFreePromotion(GameTestHelper h) {
        var npc = ready(h, 1);
        train(npc, 1);
        h.assertTrue(npc.npcCultivationState() == NpcCultivation.BREAKTHROUGH, "Attempt starts while pill is present");
        var inventory = NpcProfile.inventory(npc);
        for (var stack : inventory) if (stack.is(ModItems.BREAKTHROUGH_PILL.get())) stack.setCount(0);
        NpcProfile.writeInventory(npc, NpcProfile.cultivationProfile(npc), inventory);
        train(npc, NpcCultivation.CHANNEL_TICKS);
        h.assertTrue(LATE.equals(npc.npcRealm()), "Removing pill during channel prevents promotion");
        h.assertTrue(NpcProfile.cultivationProfile(npc).getLong("CultivationProgress") == CultivationLevels.getTotalQiNeeded(LATE), "Missing pill preserves progress");
        npc.discard(); h.succeed();
    }

    @GameTest(template="fate_test")
    public static void pillDoesNotSkipCultivationProgress(GameTestHelper h) {
        var npc = ready(h, 1);
        NpcProfile.cultivationProfile(npc).putLong("CultivationProgress", 0);
        train(npc, NpcCultivation.CHANNEL_TICKS + 1);
        h.assertTrue(LATE.equals(npc.npcRealm()) && pills(npc) == 1, "Pill waits until cultivation threshold is reached");
        npc.discard(); h.succeed();
    }

    private static LianqiCultivatorEntity ready(GameTestHelper h, int pills) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        NpcProfile.ensure(npc, npc.npcArchetype());
        var profile = NpcProfile.cultivationProfile(npc);
        profile.putString("Realm", LATE);
        profile.putString("Method", CultivationMethods.BLOOD_DEMON_HUASHEN);
        profile.putLong("CultivationProgress", CultivationLevels.getTotalQiNeeded(LATE));
        int qi = CultivationLevels.getLevelDef(LATE).maxQi();
        profile.putInt("MaxQi", qi); profile.putInt("Qi", qi);
        profile.put(NpcCultivation.TRAINING, new CompoundTag());
        var inventory = NpcProfile.inventory(npc);
        for (var stack : inventory) if (stack.is(ModItems.BREAKTHROUGH_PILL.get())) stack.setCount(0);
        if (pills > 0) inventory.set(0, new ItemStack(ModItems.BREAKTHROUGH_PILL.get(), pills));
        NpcProfile.writeInventory(npc, profile, inventory);
        LuckSystem.data(npc).putInt("Luck", 100);
        npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2, 3.5)));
        npc.setOnGround(true); npc.setHealth(npc.getMaxHealth());
        return npc;
    }

    private static int pills(LianqiCultivatorEntity npc) {
        return NpcProfile.inventory(npc).stream().filter(s -> s.is(ModItems.BREAKTHROUGH_PILL.get())).mapToInt(ItemStack::getCount).sum();
    }

    private static void train(LianqiCultivatorEntity npc, int ticks) {
        for (int i = 0; i < ticks; i++) { npc.tickCount++; NpcCultivation.tick(npc); }
    }
}
