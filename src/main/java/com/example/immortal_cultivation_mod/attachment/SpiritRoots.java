package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpiritRoots {
    public static final String METAL = "\u91d1";
    public static final String WOOD = "\u6728";
    public static final String WATER = "\u6c34";
    public static final String FIRE = "\u706b";
    public static final String EARTH = "\u571f";
    public static final String THUNDER = "\u96f7";
    public static final String ICE = "\u51b0";
    public static final String WIND = "\u98ce";
    public static final String LIGHT = "\u5149";
    public static final String DARK = "\u6697";
    public static final String NONE = "\u65e0\u7075\u6839";
    public static final String PSEUDO = "\u4f2a\u7075\u6839";

    public static final String GRADE_HEAVEN = "\u5929";
    public static final String GRADE_EARTH = "\u5730";
    public static final String GRADE_MYSTIC = "\u7384";
    public static final String GRADE_YELLOW = "\u9ec4";

    private static final List<String> NORMAL_ELEMENTS = List.of(METAL, WOOD, WATER, FIRE, EARTH);
    private static final List<String> ALL_ELEMENTS = List.of(METAL, WOOD, WATER, FIRE, EARTH, THUNDER, ICE, WIND, LIGHT, DARK);
    private static final List<String> GRADES = List.of(GRADE_HEAVEN, GRADE_EARTH, GRADE_MYSTIC, GRADE_YELLOW);

    private SpiritRoots() {
    }

    public static RootData random(RandomSource random) {
        int special = random.nextInt(100);
        if (special < 3) {
            return new RootData(List.of(NONE), GRADE_YELLOW);
        }
        if (special < 8) {
            return new RootData(List.of(PSEUDO), GRADE_YELLOW);
        }

        int roll = random.nextInt(100);
        int count = roll < 10 ? 1 : roll < 35 ? 2 : roll < 65 ? 3 : roll < 90 ? 4 : 5;
        List<String> pool = new ArrayList<>(ALL_ELEMENTS);
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        List<String> roots = new ArrayList<>();
        for (String element : pool) {
            if (roots.size() >= count) {
                break;
            }
            if (count == 5 && !NORMAL_ELEMENTS.contains(element)) {
                continue;
            }
            roots.add(element);
        }

        int gradeRoll = random.nextInt(100);
        String grade = gradeRoll < 8 ? GRADE_HEAVEN : gradeRoll < 28 ? GRADE_EARTH : gradeRoll < 65 ? GRADE_MYSTIC : GRADE_YELLOW;
        return new RootData(roots, grade);
    }

    public static boolean needsRandomRoots(ModAttachments.CultivationData data) {
        return data.spiritRoots() == null || data.spiritRoots().isEmpty() || data.spiritRootGrade() == null || data.spiritRootGrade().isBlank();
    }

    public static String format(List<String> roots, String grade) {
        if (roots == null || roots.isEmpty()) {
            return NONE + " / " + GRADE_YELLOW;
        }
        return String.join("", roots) + " / " + grade;
    }

    public static boolean hasMatchingRoot(ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
        if (data == null || spell == null || data.spiritRoots() == null) {
            return false;
        }
        String element = spell.element();
        return !NONE.equals(element) && data.spiritRoots().contains(element);
    }

    public static float damageMultiplier(ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
        return hasMatchingRoot(data, spell) ? 2.0F : 1.0F;
    }

    public static int effectDuration(ModAttachments.CultivationData data, ModSpells.SpellDef spell, int duration) {
        return hasMatchingRoot(data, spell) ? duration * 2 : duration;
    }

    public static int cultivationProgressGain(ModAttachments.CultivationData data, int baseGain) {
        if (data.spiritRoots() == null || data.spiritRoots().isEmpty() || data.spiritRoots().contains(NONE) || data.spiritRoots().contains(PSEUDO)) {
            return 1;
        }

        double rootFactor = switch (data.spiritRoots().size()) {
            case 1 -> 1.5D;
            case 2 -> 1.1D;
            case 3 -> 0.75D;
            case 4 -> 0.55D;
            default -> 0.3D;
        };
        double gradeFactor = switch (data.spiritRootGrade()) {
            case GRADE_HEAVEN -> 1.35D;
            case GRADE_EARTH -> 1.1D;
            case GRADE_MYSTIC -> 0.9D;
            default -> 0.7D;
        };
        return Math.max(1, (int)Math.round(baseGain * rootFactor * gradeFactor));
    }

    public static List<String> nextRootSet(List<String> current, int direction) {
        List<List<String>> options = List.of(
                List.of(NONE),
                List.of(PSEUDO),
                List.of(METAL),
                List.of(WOOD),
                List.of(WATER),
                List.of(FIRE),
                List.of(EARTH),
                List.of(THUNDER),
                List.of(ICE),
                List.of(WIND),
                List.of(LIGHT),
                List.of(DARK),
                List.of(METAL, WOOD),
                List.of(WATER, FIRE),
                List.of(WOOD, EARTH, WATER),
                List.of(METAL, WOOD, WATER, FIRE),
                List.of(METAL, WOOD, WATER, FIRE, EARTH)
        );
        int index = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(current)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            index = 0;
        }
        return options.get(Math.floorMod(index + (direction < 0 ? -1 : 1), options.size()));
    }

    public static String nextGrade(String current, int direction) {
        int index = GRADES.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        return GRADES.get(Math.floorMod(index + (direction < 0 ? -1 : 1), GRADES.size()));
    }

    public record RootData(List<String> roots, String grade) {
    }
}
