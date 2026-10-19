package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/** Raw, realm-independent training and achievement-based settlement defaults. */
public final class LifeSimulationBalance {
    private LifeSimulationBalance() {}

    public static boolean supported(CompoundTag life, int realm) {
        var method = CultivationMethods.get(life.getString("ActiveMethod"));
        return method != null && Math.max(CultivationLevels.getStageIndex(method.limitLevel()),
                life.getBoolean("MethodContinuation") ? life.getInt("MethodContinuationLimit") : 0) >= realm;
    }

    public static long gain(CompoundTag life, double activity) {
        if (life.getInt("Realm") <= 0 || !supported(life, life.getInt("Realm"))) return 0;
        var method = CultivationMethods.get(life.getString("ActiveMethod"));
        double tier = switch (method.tier()) {
            case "heaven" -> LifeSimulationTuning.get("heaven_method");
            case "earth" -> LifeSimulationTuning.get("earth_method");
            default -> 1.0;
        };
        // A method's rank supplies raw power. The current progress requirement never enters this formula.
        int limit = Math.max(CultivationLevels.getStageIndex(method.limitLevel()), life.getBoolean("MethodContinuation") ? life.getInt("MethodContinuationLimit") : 0);
        int rank = Math.max(1, (limit + 2) / 3);
        double aptitude = 0.5 + Mth.clamp(life.getInt("SpiritualRootPotential"), 0, 100) / 100.0;
        double insight = 0.75 + Mth.clamp(life.getInt("Comprehension"), 0, 100) / 200.0;
        double environment = "none".equals(life.getString("Sect")) ? 1.0 : 1.15;
        double strain = Math.max(0.25, 1.0 - life.getInt("TrainingStrain") / 130.0);
        return Math.max(0, (long) (LifeSimulationTuning.get("raw_training") * Math.pow(LifeSimulationTuning.get("method_growth"), rank - 1) * tier * aptitude * insight * environment * strain * activity));
    }

    public static void train(CompoundTag life, double activity) {
        int realm = Mth.clamp(life.getInt("Realm"), 0, CultivationLevels.allLevels().size() - 1);
        long required = Math.max(1, CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(realm)).qiNeeded());
        life.putLong("CultivationProgress", Math.min(required, life.getLong("CultivationProgress") + gain(life, activity)));
    }

    /** Encounter insight is five percent of this simulated realm's full progress bar. */
    public static void rewardEncounter(CompoundTag life) {
        int realm = life.getInt("Realm");
        if (realm <= 0 || realm >= CultivationLevels.allLevels().size()
                || !life.getBoolean("Alive") || life.getInt("Health") <= 0) return;
        long required = Math.max(1, CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(realm)).qiNeeded());
        long progress = Mth.clamp(life.getLong("CultivationProgress"), 0L, required);
        long gain = Math.max(1L, required / 20L);
        life.putLong("CultivationProgress", progress + Math.min(gain, required - progress));
        LifeSimulationSystem.addLog(life, "life.balance.encounter_insight");
    }

    public static long score(CompoundTag life) {
        int endings = (int) life.getCompound("StoryJournal").getAllKeys().stream()
                .filter(id -> life.getCompound("StoryJournal").getCompound(id).getBoolean("Rewarded")).count();
        int dangers = life.getInt("DangersSurvived");
        int allies = (int) life.getList("Relationships", Tag.TAG_COMPOUND).stream()
                .filter(tag -> tag instanceof CompoundTag p && p.getInt("Trust") >= 60).count();
        long realm = life.getInt("HighestRealm") * (long)LifeSimulationTuning.get("realm_score");
        long stories = endings * (long)LifeSimulationTuning.get("story_score");
        long legacy = Math.min(20, allies) * 350L + Math.min(100, dangers) * (long)LifeSimulationTuning.get("danger_score");
        long reputation = Math.max(0, life.getInt("Reputation")) * 20L;
        life.putLong("ScoreRealm", realm);
        life.putLong("ScoreStories", stories);
        life.putLong("ScoreLegacy", legacy);
        life.putLong("ScoreReputation", reputation);
        // Wealth is normalized low-grade stone value; sleeping and elapsed years grant no score.
        return realm + stories + legacy + reputation;
    }

    public static void normalize(CompoundTag life) {
        life.putInt("AnnualActionPoints", Mth.clamp(life.getInt("AnnualActionPoints"), 0, 3));
        life.putInt("TrainingStrain", Mth.clamp(life.getInt("TrainingStrain"), 0, 100));
        life.putInt("MaximumLifespan", Mth.clamp(life.getInt("MaximumLifespan"), 1, 100_000_000));
        for (var tag : life.getList("Relationships", Tag.TAG_COMPOUND)) {
            CompoundTag person = (CompoundTag) tag;
            if (!person.hasUUID("Id")) person.putUUID("Id", java.util.UUID.randomUUID());
            if (!person.contains("Trust")) person.putInt("Trust", Math.max(0, person.getInt("Affinity")));
            if (!person.contains("BirthYear")) person.putInt("BirthYear", life.getInt("Age") - person.getInt("Age"));
            if (!person.contains("Kind")) person.putString("Kind", "human");
            if (!person.contains("Friend")) person.putBoolean("Friend", List.of("friend", "dao_companion").contains(person.getString("Relation")));
            if (LifeSimulationSystem.isParent(person)) person.putBoolean("Friend", false);
            person.putBoolean("Available", person.getBoolean("Alive")
                    && (!person.contains("Observed") || person.getBoolean("Observed"))
                    && !person.getBoolean("Captive"));
            if (person.getInt("Age") >= 16 && "child".equals(person.getString("Occupation"))) person.putString("Occupation", "laborer");
        }
        if (life.getBoolean("RewardClaimed")) life.putString("ClaimTransaction", "delivered");
        CompoundTag battle = life.getCompound("Battle");
        if (!battle.isEmpty() && (!battle.hasUUID("Id") || battle.getList("Actors", Tag.TAG_COMPOUND).isEmpty())) {
            life.remove("Battle");
            if (life.contains("Story")) LifeSimulationStories.openNode(life, "withdraw");
        }
        CompoundTag story = life.getCompound("Story");
        if (!story.isEmpty() && (!story.hasUUID("Instance") || story.getCompound("Definition").isEmpty())) {
            life.remove("Story"); life.remove("CurrentEvent");
        }
        life.putInt("StateVersion", 2);
    }
}
