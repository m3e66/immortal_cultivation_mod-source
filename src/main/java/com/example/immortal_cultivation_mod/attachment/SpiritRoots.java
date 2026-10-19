package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
    public static final String YANG = "\u9633";
    public static final String YIN = "\u9634";
    public static final String NONE = "\u65e0\u7075\u6839";

    public static final String GRADE_HEAVEN = "\u5929";
    public static final String GRADE_EARTH = "\u5730";
    public static final String GRADE_MYSTIC = "\u7384";
    public static final String GRADE_YELLOW = "\u9ec4";

    public static final List<String> NORMAL_ELEMENTS = List.of(METAL, WOOD, WATER, FIRE, EARTH);
    public static final List<String> ALL_ELEMENTS = List.of(METAL, WOOD, WATER, FIRE, EARTH, THUNDER, WIND, ICE, YANG, YIN);
    public static final List<String> GRADES = List.of(GRADE_HEAVEN, GRADE_EARTH, GRADE_MYSTIC, GRADE_YELLOW);

    private SpiritRoots() {
    }

    /** Canonicalizes saved pre-Yin/Yang element names without changing other affinities. */
    public static String normalizeElement(String element) {
        if (element == null) return "";
        return switch (element.trim()) {
            case "光", "light", "LIGHT", "yang", "YANG" -> YANG;
            case "暗", "dark", "DARK", "yin", "YIN" -> YIN;
            default -> element.trim();
        };
    }

    public static String normalizeElementSpec(String elementSpec) {
        if (elementSpec == null) return "";
        return java.util.Arrays.stream(elementSpec.split(","))
                .map(SpiritRoots::normalizeElement).collect(java.util.stream.Collectors.joining(","));
    }

    public static List<String> migrateRootList(List<String> roots) {
        if (roots == null || roots.isEmpty()) return List.of();
        // Preserve no-root and other saved entries; only merge duplicate renamed elements.
        for (int i = 0; i < roots.size(); i++) {
            String root = roots.get(i);
            if (!java.util.Objects.equals(root, normalizeElement(root)) || roots.indexOf(root) < i)
                return roots.stream().map(SpiritRoots::normalizeElement).distinct().toList();
        }
        return List.copyOf(roots);
    }

    public static RootData random(RandomSource random) {
        int special = random.nextInt(100);
        if (special < 3) {
            return new RootData(List.of(NONE), GRADE_YELLOW);
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
        roots = migrateRootList(roots);
        if (roots == null || roots.isEmpty()) {
            return NONE + "/" + GRADE_YELLOW;
        }
        return rootTypeName(roots) + "/" + String.join("", roots) + "/" + sanitizeGrade(grade);
    }

    /** Display components stay translatable; saved root identifiers remain unchanged. */
    public static Component displayName(List<String> roots, String grade) {
        roots = migrateRootList(roots);
        boolean rootless = roots.isEmpty() || roots.contains(NONE);
        return Component.translatable("cultivation.immortal_cultivation_mod.roots.format",
                Component.translatable("cultivation.immortal_cultivation_mod.roots." + (rootless ? 0 : roots.size())),
                rootless ? elementDisplayName(NONE) : elementsDisplayName(roots), gradeDisplayName(grade));
    }

    public static Component gradeDisplayName(String grade) {
        String suffix = switch (sanitizeGrade(grade)) {
            case GRADE_HEAVEN -> "heaven";
            case GRADE_EARTH -> "earth";
            case GRADE_MYSTIC -> "mystic";
            default -> "yellow";
        };
        return Component.translatable("cultivation.immortal_cultivation_mod.root_grade." + suffix);
    }

    /** Chart distance reflects the existing grade; absent elements are exactly at the center. */
    public static float elementStrength(List<String> roots, String grade, String element) {
        if (!matchesAny(roots, element)) return 0;
        return switch (sanitizeGrade(grade)) {
            case GRADE_HEAVEN -> 1F;
            case GRADE_EARTH -> .75F;
            case GRADE_MYSTIC -> .5F;
            default -> .25F;
        };
    }

    public static Component elementDisplayName(String element) {
        String normalized = normalizeElement(element);
        String suffix = switch (normalized) {
            case METAL -> "metal";
            case WOOD -> "wood";
            case WATER -> "water";
            case FIRE -> "fire";
            case EARTH -> "earth";
            case THUNDER -> "thunder";
            case ICE -> "ice";
            case WIND -> "wind";
            case YANG -> "yang";
            case YIN -> "yin";
            case NONE, "none", "any", "" -> "none";
            default -> null;
        };
        return suffix == null ? Component.literal(normalized)
                : Component.translatable("screen.immortal_cultivation_mod.trajectory.element." + suffix);
    }

    public static Component elementsDisplayName(List<String> elements) {
        MutableComponent result = Component.empty();
        for (String element : elements) {
            if (!result.getSiblings().isEmpty()) result.append(" / ");
            result.append(elementDisplayName(element));
        }
        return result;
    }

    public static Component elementSpecDisplayName(String elements) {
        if (elements == null || elements.isBlank()) return elementDisplayName(NONE);
        return elementsDisplayName(java.util.Arrays.asList(elements.split(",")));
    }

    public static String rootTypeName(List<String> roots) {
        int count = roots == null ? 0 : roots.size();
        return switch (count) {
            case 1 -> "\u5355\u7075\u6839";
            case 2 -> "\u53cc\u7075\u6839";
            case 3 -> "\u4e09\u7075\u6839";
            case 4 -> "\u56db\u7075\u6839";
            case 5 -> "\u4e94\u7075\u6839";
            default -> NONE;
        };
    }

    public static String sanitizeGrade(String grade) {
        return GRADES.contains(grade) ? grade : GRADE_YELLOW;
    }

    public static List<String> sanitizeRootList(List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        List<String> clean = new ArrayList<>();
        for (String root : roots) {
            root = normalizeElement(root);
            if (ALL_ELEMENTS.contains(root) && !clean.contains(root) && clean.size() < 5) {
                clean.add(root);
            }
        }
        return List.copyOf(clean);
    }

    public static boolean hasMatchingRoot(ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
        if (data == null || spell == null || data.spiritRoots() == null) {
            return false;
        }
        return matchesAny(data.spiritRoots(), spell.element());
    }

    public static boolean matchesAny(List<String> available, String elementSpec) {
        if (available == null || available.isEmpty() || elementSpec == null || elementSpec.isBlank()) {
            return false;
        }
        for (String element : elementSpec.split(",")) {
            String clean = normalizeElement(element);
            if (!clean.isEmpty() && !NONE.equals(clean)
                    && available.stream().map(SpiritRoots::normalizeElement).anyMatch(clean::equals)) {
                return true;
            }
        }
        return false;
    }

    public static boolean elementMatches(String elementSpec, String element) {
        if (elementSpec == null || element == null || element.isBlank()) {
            return false;
        }
        for (String candidate : elementSpec.split(",")) {
            if (normalizeElement(element).equals(normalizeElement(candidate))) {
                return true;
            }
        }
        return false;
    }

    public static String primaryElement(String elementSpec) {
        if (elementSpec == null) {
            return "";
        }
        for (String candidate : elementSpec.split(",")) {
            String clean = normalizeElement(candidate);
            if (ALL_ELEMENTS.contains(clean)) {
                return clean;
            }
        }
        return elementSpec.trim();
    }

    public static float damageMultiplier(ModAttachments.CultivationData data, ModSpells.SpellDef spell) {
        return hasMatchingRoot(data, spell) ? 2.0F : 1.0F;
    }

    public static int effectDuration(ModAttachments.CultivationData data, ModSpells.SpellDef spell, int duration) {
        return hasMatchingRoot(data, spell) ? duration * 2 : duration;
    }

    public static int cultivationProgressGain(ModAttachments.CultivationData data, int baseGain) {
        if (data.spiritRoots() == null || data.spiritRoots().isEmpty() || data.spiritRoots().contains(NONE)) {
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
        current = migrateRootList(current);
        List<List<String>> options = List.of(
                List.of(NONE),
                List.of(METAL),
                List.of(WOOD),
                List.of(WATER),
                List.of(FIRE),
                List.of(EARTH),
                List.of(THUNDER),
                List.of(ICE),
                List.of(WIND),
                List.of(YANG),
                List.of(YIN),
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
