package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.system.LifeStoryChecks;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Explains authored story checks using the simulated person's current attributes. */
public final class LifeStoryCheckText {
    private LifeStoryCheckText() {}

    public static Component label(CompoundTag life, CompoundTag choice, Component label) {
        if (choice.contains("check", Tag.TAG_COMPOUND)) {
            var odds = LifeStoryChecks.odds(life, choice);
            return Component.translatable(odds.extended() ? "life.story.challenge_chance" : "life.story.choice_chance",
                    percent(odds.success())).append("\n").append(label);
        }
        if (!choice.contains("text_key")) return label;
        return Component.translatable(choice.contains("battle", Tag.TAG_COMPOUND)
                ? "life.story.choice_battle" : "life.story.choice_no_roll").append("\n").append(label);
    }

    private static String percent(double probability) {
        double value = Math.round(Math.max(0, Math.min(1, probability)) * 1000) / 10.0;
        if (value == 100 && probability < 1 - 1e-12) return ">99.9%";
        if (value == 0 && probability > 1e-12) return "<0.1%";
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f%%", value)
                : String.format(Locale.ROOT, "%.1f%%", value);
    }

    public static Component tooltip(CompoundTag life, CompoundTag choice, Component label) {
        if (!choice.contains("check", Tag.TAG_COMPOUND)) return label;
        CompoundTag check = choice.getCompound("check");
        int modifier = LifeStoryChecks.modifier(life, check);
        var odds = LifeStoryChecks.odds(life, choice);
        Component purpose = Component.translatable("life.battle.purpose." + check.getString("purpose"));
        Component result = label.copy().append("\n\n").append(Component.translatable("life.story.check_help",
                purpose, check.getInt("dc"), String.format(Locale.ROOT, "%+d", modifier)))
                .append("\n").append(Component.translatable("life.story.check_odds",
                        percent(odds.clean()), percent(odds.cost()), percent(odds.failure())));
        if (odds.extended()) result = result.copy().append("\n").append(Component.translatable(
                "life.story.challenge_odds", percent(odds.success()), percent(odds.rollSuccess())));
        return result;
    }
}
