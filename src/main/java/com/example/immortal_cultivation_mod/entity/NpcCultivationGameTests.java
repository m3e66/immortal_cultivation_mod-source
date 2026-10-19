package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.fate.LuckSystem;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class NpcCultivationGameTests {
    private static final String EARLY = CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY;
    private static final String LATE = CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE;

    @GameTest(template="fate_test")
    public static void cultivatorTiersAcquireAndDamageWildXuanjin(GameTestHelper h) {
        for(var type:java.util.List.of(ModEntities.LIANQI_CULTIVATOR.get(),ModEntities.ZHUJI_CULTIVATOR.get(),ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc=type.create(h.getLevel());prepare(h,npc,EARLY,0);train(npc,1);
            var beast=ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());beast.setNoAi(true);
            beast.setPos(npc.getX()+8,npc.getY(),npc.getZ());h.getLevel().addFreshEntity(beast);
            new NpcCombatBrain(npc).tick();
            h.assertTrue(npc.getTarget()==beast,"Every cultivator tier proactively targets a visible wild Xuanjin");
            h.assertTrue(((CultivationNpc)npc).npcCultivationState()==0,"Hostile beast interrupts meditation");
            h.assertTrue(NpcProfile.canNpcProjectileHit(npc,beast),"NPC projectiles recognize the beast as a combat enemy");
            float before=beast.getHealth();
            h.assertTrue(beast.hurt(npc.damageSources().mobAttack(npc),100) && beast.getHealth()<before,
                    "Real NPC damage is not discarded by the non-combatant filter");
            beast.discard();npc.discard();
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void peacefulBoundXuanjinIsNotHuntedButAggressionIsAnswered(GameTestHelper h) {
        var npc=lianqi(h,EARLY,0);
        var owner=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"lantern-owner"));
        var beast=ModEntities.XUANJIN_ZHENG.get().create(h.getLevel());beast.bind(owner,2);beast.setNoAi(true);
        beast.setPos(npc.getX()+8,npc.getY(),npc.getZ());h.getLevel().addFreshEntity(beast);
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget()==null,"Peaceful bound companions are not treated as wild beasts");
        beast.setTarget(npc);new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget()==beast,"Cultivators defend themselves against an attacking bound beast too");
        beast.discard();npc.discard();h.succeed();
    }

    private static void methods(CompoundTag profile, String... ids) {
        ListTag methods = new ListTag();
        for (String id : ids) methods.add(StringTag.valueOf(id));
        profile.put("Methods", methods);
        profile.putString("Method", ids[0]);
    }

    private static void prepare(GameTestHelper helper, PathfinderMob mob, String realm, long progress) {
        CultivationNpc npc = (CultivationNpc) mob;
        NpcProfile.ensure(mob, npc.npcArchetype());
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        profile.putString("Realm", realm);
        profile.putString("Personality", "gentle");
        profile.putLong("CultivationProgress", progress);
        int maxQi = CultivationLevels.getLevelDef(realm).maxQi();
        profile.putInt("MaxQi", maxQi);
        profile.putInt("Qi", maxQi);
        methods(profile, CultivationMethods.BLOOD_DEMON_HUASHEN);
        profile.put(NpcCultivation.TRAINING, new CompoundTag());
        LuckSystem.data(mob).putInt("Luck", 100);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        mob.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        mob.setOnGround(true);
        NpcProfile.ensure(mob, npc.npcArchetype());
        mob.setHealth(mob.getMaxHealth());
    }

    private static LianqiCultivatorEntity lianqi(GameTestHelper helper, String realm, long progress) {
        var mob = ModEntities.LIANQI_CULTIVATOR.get().create(helper.getLevel());
        prepare(helper, mob, realm, progress);
        return mob;
    }

    private static void train(PathfinderMob mob, int ticks) {
        for (int i = 0; i < ticks; i++) {
            mob.tickCount++;
            NpcCultivation.tick(mob);
        }
    }

    @GameTest(template = "fate_test", timeoutTicks = 300)
    public static void idleAiBreaksThroughWithoutPlayerInput(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        var mob = lianqi(helper, EARLY, CultivationLevels.getTotalQiNeeded(EARLY));
        helper.getLevel().addFreshEntity(mob);
        helper.runAfterDelay(230, () -> {
            helper.assertTrue(CultivationLevels.getNextStage(EARLY).equals(mob.npcRealm()),
                    "Normal entity/AI ticks autonomously complete a breakthrough");
            helper.assertTrue(mob.npcCultivationState() == 0, "NPC returns to normal activity after success");
            helper.assertTrue(NpcProfile.cultivationProfile(mob).getCompound(NpcCultivation.TRAINING).getInt("Successes") == 1,
                    "One channel promotes exactly one stage");
            mob.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void trainingAccumulatesFractionalProgressAndHonorsCommands(GameTestHelper helper) {
        var mob = lianqi(helper, EARLY, 0);
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        train(mob, 200);
        long progress = profile.getLong("CultivationProgress");
        helper.assertTrue(progress > 0 && progress < 10, "Sub-unit gains accumulate without rounding every second up to one");
        helper.assertTrue(mob.npcCultivationState() == NpcCultivation.MEDITATING, "Safe NPC visibly meditates");
        var before = mob.getPersistentData().copy();
        mob.getRandom().setSeed(1927);
        var random = net.minecraft.util.RandomSource.create(1927);
        NpcCultivation.debugLines(mob);
        helper.assertTrue(before.equals(mob.getPersistentData()), "NPC diagnostics preserve saved state");
        helper.assertTrue(mob.getRandom().nextDouble() == random.nextDouble(), "NPC diagnostics do not consume RNG");
        profile.putUUID("FollowingPlayer", UUID.randomUUID());
        train(mob, 100);
        helper.assertTrue("following".equals(NpcCultivation.blocker(mob)) && mob.npcCultivationState() == 0,
                "Following overrides meditation");
        helper.assertTrue(profile.getLong("CultivationProgress") == progress, "No cultivation progress while following");
        profile.remove("FollowingPlayer");
        mob.setNoAi(true);
        train(mob, 300);
        helper.assertTrue(profile.getLong("CultivationProgress") == progress, "NoAI freezes training");
        mob.setNoAi(false);
        mob.setOnGround(false);
        helper.assertTrue("unsafe_ground".equals(NpcCultivation.blocker(mob)), "Airborne NPC cannot meditate");
        var mortal = ModEntities.FANREN_NPC.get().create(helper.getLevel());
        NpcProfile.ensure(mortal, mortal.npcArchetype());
        long mortalProgress = mortal.npcCultivationProgress();
        train(mortal, 100);
        helper.assertTrue(mortal.npcCultivationProgress() == mortalProgress, "Ordinary mortals do not gain a free cultivation path");
        mob.discard(); mortal.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void majorBreakthroughPreservesNpcAndUpgradesStats(GameTestHelper helper) {
        var mob = lianqi(helper, LATE, CultivationLevels.getTotalQiNeeded(LATE) + 50);
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        // Wanted NPCs intentionally rewrite their bounty order after a realm change.
        // Keep this strict inventory-preservation check on an ordinary cultivator.
        profile.putString("Identity", "wandering_cultivator");
        NpcProfile.ensure(mob, mob.npcArchetype());
        CompoundTag inventoryBefore = new CompoundTag();
        inventoryBefore.put("Inventory", profile.get("Inventory").copy());
        var withPill = NpcProfile.inventory(mob);
        NpcProfile.insertInventory(withPill, new net.minecraft.world.item.ItemStack(
                com.example.immortal_cultivation_mod.item.ModItems.BREAKTHROUGH_PILL.get()));
        NpcProfile.writeInventory(mob, profile, withPill);
        profile.getCompound("Relationships").putString("test-bond", "retained");
        String name = profile.getString("Name");
        int age = profile.getInt("Age");
        UUID uuid = mob.getUUID();
        train(mob, 1);
        helper.assertTrue(mob.npcCultivationState() == NpcCultivation.BREAKTHROUGH, "Full progress begins the channel");
        helper.assertTrue(profile.getInt("Qi") < profile.getInt("MaxQi"), "Attempt spends real Qi");
        train(mob, NpcCultivation.CHANNEL_TICKS);
        String next = CultivationLevels.getNextStage(LATE);
        var level = CultivationLevels.getLevelDef(next);
        helper.assertTrue(next.equals(mob.npcRealm()), "Qi Refining advances into Foundation Establishment");
        helper.assertTrue(profile.getLong("CultivationProgress") == 50, "Only the current threshold is consumed");
        helper.assertTrue(mob.getMaxHealth() == level.maxHp() && mob.getHealth() == mob.getMaxHealth(), "New realm health applies immediately");
        helper.assertTrue(profile.getInt("MaxQi") == level.maxQi() && profile.getInt("Qi") == level.maxQi(), "New realm Qi applies immediately");
        helper.assertTrue(mob.getAttributeValue(Attributes.ARMOR) == level.armor(), "New realm armor applies immediately");
        helper.assertTrue(mob.getAttributeValue(Attributes.ATTACK_DAMAGE) > 4, "Melee strength grows");
        helper.assertTrue(profile.getInt("Age") == age + level.maxAge() - CultivationLevels.getLevelDef(LATE).maxAge(), "Breakthrough extends remaining lifespan");
        helper.assertTrue(uuid.equals(mob.getUUID()) && name.equals(profile.getString("Name")), "NPC identity survives promotion");
        helper.assertTrue(inventoryBefore.get("Inventory").equals(profile.get("Inventory")), "Promotion preserves other inventory and consumes exactly one breakthrough pill");
        helper.assertTrue("retained".equals(profile.getCompound("Relationships").getString("test-bond")), "Promotion preserves relationships");
        mob.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void damageInterruptsAndLuckControlsFailure(GameTestHelper helper) {
        long goal = CultivationLevels.getTotalQiNeeded(EARLY);
        var mob = lianqi(helper, EARLY, goal);
        train(mob, 1);
        mob.hurt(mob.damageSources().generic(), 1);
        helper.assertTrue(mob.npcCultivationState() == 0, "Damage immediately interrupts the synced breakthrough pose");
        train(mob, 220);
        helper.assertTrue(EARLY.equals(mob.npcRealm()), "Interrupted attempt cannot promote later");
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        helper.assertTrue(profile.getLong("CultivationProgress") == goal, "Interruption retains earned progress");
        var doomed = lianqi(helper, EARLY, goal);
        LuckSystem.data(doomed).putInt("Luck", 0);
        train(doomed, NpcCultivation.CHANNEL_TICKS + 1);
        CompoundTag failed = NpcProfile.cultivationProfile(doomed);
        helper.assertTrue(EARLY.equals(doomed.npcRealm()), "Zero Luck fails the actual breakthrough roll");
        helper.assertTrue(failed.getLong("CultivationProgress") == goal * 9 / 10, "Failure loses 10% of the threshold");
        helper.assertTrue(failed.getCompound(NpcCultivation.TRAINING).getInt("RestTicks") == 2400, "Failed attempts wait before retrying");
        helper.assertTrue(failed.getCompound(NpcCultivation.TRAINING).getInt("Attempts") == 1
                && "failed".equals(failed.getCompound(NpcCultivation.TRAINING).getString("LastOutcome")), "Diagnostics record the actual failed attempt");
        mob.discard(); doomed.discard(); helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void methodLimitsAndSavedChannelsAreRespected(GameTestHelper helper) {
        String cap = CultivationMethods.get(CultivationMethods.CLEAR_HEART).limitLevel();
        var mob = lianqi(helper, cap, CultivationLevels.getTotalQiNeeded(cap));
        CompoundTag profile = NpcProfile.cultivationProfile(mob);
        methods(profile, CultivationMethods.CLEAR_HEART);
        train(mob, 220);
        helper.assertTrue(cap.equals(mob.npcRealm()) && "method_limit".equals(NpcCultivation.blocker(mob)),
                "A capped method cannot bypass its realm limit");
        methods(profile, CultivationMethods.CLEAR_HEART, CultivationMethods.BLOOD_DEMON_JINDAN);
        mob.tickCount = 240;
        NpcProfile.tickCultivation(mob, mob.npcArchetype());
        helper.assertTrue(CultivationMethods.BLOOD_DEMON_JINDAN.equals(profile.getString("Method")),
                "An available method that allows progress is preferred over a capped stronger method");
        train(mob, 51);
        int remaining = profile.getCompound(NpcCultivation.TRAINING).getInt("Ticks");
        helper.assertTrue(mob.npcCultivationState() == NpcCultivation.BREAKTHROUGH && remaining > 0, "New method unlocks an attempt");
        CompoundTag saved = new CompoundTag();
        NpcProfile.save(mob, saved);
        var loaded = ModEntities.LIANQI_CULTIVATOR.get().create(helper.getLevel());
        NpcProfile.load(loaded, saved);
        NpcProfile.ensure(loaded, loaded.npcArchetype());
        loaded.setOnGround(true);
        LuckSystem.data(loaded).putInt("Luck", 100);
        var loadedTraining = NpcProfile.cultivationProfile(loaded).getCompound(NpcCultivation.TRAINING);
        helper.assertTrue(loadedTraining.getInt("Ticks") == remaining && cap.equals(loaded.npcRealm()), "Loading does not grant offline progress or finish a channel");
        train(loaded, remaining);
        helper.assertTrue(CultivationLevels.getNextStage(cap).equals(loaded.npcRealm()), "Saved channel completes only after its remaining loaded ticks");
        CompoundTag after = new CompoundTag();
        NpcProfile.save(loaded, after);
        NpcProfile.load(mob, after);
        helper.assertTrue(mob.npcRealm().equals(loaded.npcRealm()), "Completed realm promotion is persisted");
        var high = ModEntities.JINDAN_CULTIVATOR.get().create(helper.getLevel());
        String jindanLate = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE;
        prepare(helper, high, jindanLate, CultivationLevels.getTotalQiNeeded(jindanLate));
        train(high, NpcCultivation.CHANNEL_TICKS + 1);
        helper.assertTrue(CultivationLevels.getNextStage(jindanLate).equals(high.npcRealm()), "Jindan NPCs can reach Nascent Soul using the same entity");
        mob.discard(); loaded.discard(); high.discard(); helper.succeed();
    }
}
