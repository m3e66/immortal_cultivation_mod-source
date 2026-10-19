package com.example.immortal_cultivation_mod.system;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Shared roll inputs and exact, read-only previews for the simulated person. */
public final class LifeStoryChecks {
    private LifeStoryChecks() {}

    public record Odds(double success, double clean, double cost, double failure,
                       double rollSuccess, boolean extended) {}
    private record Progress(int successes, int failures, int strain, boolean futurePrepared) {}

    public static int modifier(CompoundTag life, CompoundTag check) {
        return LifeSimulationDice.attribute(life.getInt(check.getString("attribute")))
                + Mth.clamp(life.getInt(check.getString("proficiency")) / 30, 0, 4);
    }

    public static boolean advantage(CompoundTag life) {
        return life.getCompound("Story").getBoolean("Prepared") || life.getBoolean("FuturePrepared");
    }

    public static boolean disadvantage(CompoundTag life) {
        return life.getInt("TrainingStrain") >= 70;
    }

    public static Odds odds(CompoundTag life, CompoundTag choice) {
        var check = choice.getCompound("check");
        int modifier = modifier(life, check), target = check.getInt("dc");
        double[] chance = LifeSimulationDice.probabilities(modifier, target, advantage(life), disadvantage(life));
        double rollSuccess = chance[1] + chance[2] + chance[3];
        boolean extended = choice.getBoolean("extended");
        var story = life.getCompound("Story");
        double success = extended ? challenge(modifier, target, story.getBoolean("Prepared"),
                new Progress(Mth.clamp(story.getInt("Successes"), 0, 3), Mth.clamp(story.getInt("Failures"), 0, 2),
                        Mth.clamp(life.getInt("TrainingStrain"), 0, 100), life.getBoolean("FuturePrepared")), new HashMap<>()) : rollSuccess;
        return new Odds(success, chance[2] + chance[3], chance[1], chance[0], rollSuccess, extended);
    }

    private static double challenge(int modifier, int target, boolean prepared,
                                    Progress progress, Map<Progress, Double> memo) {
        if (progress.successes() >= 3) return 1;
        if (progress.failures() >= 2) return 0;
        Double cached = memo.get(progress);
        if (cached != null) return cached;
        double[] chance = LifeSimulationDice.probabilities(modifier, target,
                prepared || progress.futurePrepared(), progress.strain() >= 70);
        double result = chance[0] * challenge(modifier, target, prepared,
                new Progress(progress.successes(), progress.failures() + 1, progress.strain(), false), memo)
                + chance[1] * challenge(modifier, target, prepared,
                new Progress(progress.successes() + 1, progress.failures(), Math.min(100, progress.strain() + 6), false), memo)
                + (chance[2] + chance[3]) * challenge(modifier, target, prepared,
                new Progress(progress.successes() + 1, progress.failures(), progress.strain(), false), memo);
        memo.put(progress, result);
        return result;
    }
}
