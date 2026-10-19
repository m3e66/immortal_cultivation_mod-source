package com.example.immortal_cultivation_mod.system;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** A roll is created once on the server and persisted before any client reveal. */
public final class LifeSimulationDice {
    private LifeSimulationDice() {}

    public static int attribute(int value) { return Mth.clamp(Math.floorDiv(value - 50, 10), -4, 5); }

    public static CompoundTag check(RandomSource random, String purpose, int modifier, int dc,
                                    boolean advantage, boolean disadvantage) {
        CompoundTag roll = new CompoundTag();
        roll.putUUID("Id", UUID.randomUUID());
        roll.putString("Purpose", purpose);
        roll.putInt("Sides", 20);
        int first = random.nextInt(20) + 1;
        int second = advantage != disadvantage ? random.nextInt(20) + 1 : first;
        int selected = advantage == disadvantage ? first : advantage ? Math.max(first, second) : Math.min(first, second);
        roll.putIntArray("Dice", advantage == disadvantage ? new int[]{first} : new int[]{first, second});
        roll.putInt("Selected", selected);
        roll.putInt("Modifier", modifier);
        roll.putInt("DC", dc);
        roll.putInt("Total", selected + modifier);
        roll.putBoolean("Advantage", advantage && !disadvantage);
        roll.putBoolean("Disadvantage", disadvantage && !advantage);
        roll.putInt("Degree", degree(selected, modifier, dc));
        return roll;
    }

    public static int degree(int selected, int modifier, int dc) {
        int margin = selected + modifier - dc;
        int degree = margin >= 5 ? 3 : margin >= 0 ? 2 : margin >= -4 ? 1 : 0;
        if (selected == 20) degree = Math.min(3, degree + 1);
        if (selected == 1) degree = Math.max(0, degree - 1);
        return degree;
    }

    /** Exact outcome distribution; never consumes RNG or creates a committed roll. */
    public static double[] probabilities(int modifier, int dc, boolean advantage, boolean disadvantage) {
        double[] result = new double[4];
        int secondCount = advantage == disadvantage ? 1 : 20;
        for (int first = 1; first <= 20; first++) for (int second = 1; second <= secondCount; second++) {
            int selected = advantage == disadvantage ? first
                    : advantage ? Math.max(first, second) : Math.min(first, second);
            result[degree(selected, modifier, dc)] += 1.0 / (20 * secondCount);
        }
        return result;
    }

    public static CompoundTag damage(RandomSource random, int count, int sides) {
        CompoundTag roll = new CompoundTag();
        roll.putUUID("Id", UUID.randomUUID());
        roll.putString("Purpose", "damage");
        roll.putInt("Sides", sides);
        int[] dice = new int[count];
        int total = 0;
        for (int i = 0; i < count; i++) { dice[i] = random.nextInt(sides) + 1; total += dice[i]; }
        roll.putIntArray("Dice", dice);
        roll.putInt("Total", total);
        roll.putDouble("Factor", 0.75 + 0.50 * (total - count) / Math.max(1.0, count * (sides - 1)));
        return roll;
    }

    public static double hitChance(int modifier, int defense, boolean plausible) {
        if (!plausible) return 0;
        int hits = 0;
        for (int value = 2; value <= 20; value++) if (value == 20 || value + modifier >= defense) hits++;
        return hits / 20.0;
    }

    public static void remember(CompoundTag life, CompoundTag roll) {
        ListTag history = life.getList("DiceHistory", net.minecraft.nbt.Tag.TAG_COMPOUND);
        history.add(roll.copy());
        while (history.size() > 160) history.remove(0);
        life.put("DiceHistory", history);
    }
}
