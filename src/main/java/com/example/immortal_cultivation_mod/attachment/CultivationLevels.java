package com.example.immortal_cultivation_mod.attachment;

import java.util.List;
import java.util.Map;

public class CultivationLevels {
    public static final String REALM_MORTAL = "\u51e1\u4eba";
    public static final String REALM_LIANQI = "\u7ec3\u6c14";
    public static final String REALM_ZHUJI = "\u7b51\u57fa";
    public static final String REALM_JINDAN = "\u91d1\u4e39";
    public static final String REALM_YUANYING = "\u5143\u5a74";
    public static final String REALM_HUASHEN = "\u5316\u795e";
    public static final String REALM_LIANXU = "\u70bc\u865a";
    public static final String REALM_HETI = "\u5408\u4f53";
    public static final String REALM_DACHENG = "\u5927\u4e58";
    public static final String REALM_DUJIE = "\u6e21\u52ab";

    public static final String STAGE_EARLY = "\u524d\u671f";
    public static final String STAGE_MID = "\u4e2d\u671f";
    public static final String STAGE_LATE = "\u540e\u671f";

    public static final LevelDef LEVEL_MORTAL = new LevelDef(REALM_MORTAL, "", 20, 100, 20, 10L);

    public record StageDef(String stage, int maxHp, int maxAge, int maxQi, long qiNeeded, float breakthroughFailMultiplier) {}

    public record RealmDef(String realm, List<StageDef> stages) {
        public StageDef getStage(String stage) {
            return stages.stream().filter(s -> s.stage().equals(stage)).findFirst().orElse(null);
        }
    }

    public record LevelDef(String realm, String stage, int maxHp, int maxAge, int maxQi, long qiNeeded) {}

    private static final Map<String, RealmDef> REALMS = Map.of(
            REALM_LIANQI, new RealmDef(REALM_LIANQI, List.of(
                    new StageDef(STAGE_EARLY, 50, 105, 50, 100L, 0.01f),
                    new StageDef(STAGE_MID, 80, 115, 120, 800L, 0.01f),
                    new StageDef(STAGE_LATE, 120, 130, 250, 2500L, 0.01f)
            )),
            REALM_ZHUJI, new RealmDef(REALM_ZHUJI, List.of(
                    new StageDef(STAGE_EARLY, 250, 180, 500, 8000L, 0.01f),
                    new StageDef(STAGE_MID, 400, 230, 900, 25000L, 0.01f),
                    new StageDef(STAGE_LATE, 700, 300, 1500, 80000L, 0.01f)
            )),
            REALM_JINDAN, new RealmDef(REALM_JINDAN, List.of(
                    new StageDef(STAGE_EARLY, 1500, 500, 3000, 200000L, 0.01f),
                    new StageDef(STAGE_MID, 2500, 700, 5000, 600000L, 0.01f),
                    new StageDef(STAGE_LATE, 4000, 1000, 8000, 2000000L, 0.01f)
            )),
            REALM_YUANYING, new RealmDef(REALM_YUANYING, List.of(
                    new StageDef(STAGE_EARLY, 8000, 2000, 15000, 5000000L, 0.01f),
                    new StageDef(STAGE_MID, 13000, 3000, 25000, 15000000L, 0.01f),
                    new StageDef(STAGE_LATE, 22000, 4500, 40000, 50000000L, 0.01f)
            )),
            REALM_HUASHEN, new RealmDef(REALM_HUASHEN, List.of(
                    new StageDef(STAGE_EARLY, 50000, 8000, 100000, 120000000L, 0.01f),
                    new StageDef(STAGE_MID, 80000, 12000, 160000, 400000000L, 0.01f),
                    new StageDef(STAGE_LATE, 130000, 18000, 250000, 1200000000L, 0.01f)
            )),
            REALM_LIANXU, new RealmDef(REALM_LIANXU, List.of(
                    new StageDef(STAGE_EARLY, 300000, 30000, 600000, 3000000000L, 0.01f),
                    new StageDef(STAGE_MID, 500000, 50000, 1000000, 9000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 800000, 80000, 1600000, 30000000000L, 0.01f)
            )),
            REALM_HETI, new RealmDef(REALM_HETI, List.of(
                    new StageDef(STAGE_EARLY, 2000000, 150000, 4000000, 80000000000L, 0.01f),
                    new StageDef(STAGE_MID, 3500000, 250000, 7000000, 250000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 6000000, 400000, 12000000, 800000000000L, 0.01f)
            )),
            REALM_DACHENG, new RealmDef(REALM_DACHENG, List.of(
                    new StageDef(STAGE_EARLY, 15000000, 1000000, 30000000, 2000000000000L, 0.01f),
                    new StageDef(STAGE_MID, 25000000, 1800000, 50000000, 7000000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 40000000, 3000000, 80000000, 25000000000000L, 0.01f)
            )),
            REALM_DUJIE, new RealmDef(REALM_DUJIE, List.of(
                    new StageDef(STAGE_EARLY, 100000000, 10000000, 200000000, 100000000000000L, 0.01f),
                    new StageDef(STAGE_MID, 180000000, 30000000, 350000000, 500000000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 300000000, 100000000, 600000000, 2000000000000000L, 0.01f)
            ))
    );

    public static RealmDef getRealm(String realm) {
        return REALMS.get(realm);
    }

    public static boolean isMortal(String cultivationLevel) {
        return cultivationLevel == null || cultivationLevel.isBlank() || cultivationLevel.equals(REALM_MORTAL);
    }

    public static LevelDef getLevelDef(String cultivationLevel) {
        if (isMortal(cultivationLevel)) {
            return LEVEL_MORTAL;
        }
        for (var entry : REALMS.entrySet()) {
            for (var stage : entry.getValue().stages()) {
                String fullName = entry.getKey() + stage.stage();
                if (fullName.equals(cultivationLevel)) {
                    return new LevelDef(entry.getKey(), stage.stage(), stage.maxHp(), stage.maxAge(), stage.maxQi(), stage.qiNeeded());
                }
            }
        }
        return LEVEL_MORTAL;
    }

    public static String getNextStage(String currentLevel) {
        if (isMortal(currentLevel)) {
            return REALM_LIANQI + STAGE_EARLY;
        }
        List<String> allStages = allStages();
        int idx = allStages.indexOf(currentLevel);
        if (idx >= 0 && idx < allStages.size() - 1) {
            return allStages.get(idx + 1);
        }
        return null;
    }

    public static String getPreviousStage(String currentLevel) {
        if (isMortal(currentLevel)) {
            return null;
        }
        List<String> allStages = allStages();
        int idx = allStages.indexOf(currentLevel);
        if (idx > 0) {
            return allStages.get(idx - 1);
        }
        return idx == 0 ? REALM_MORTAL : null;
    }

    public static int getStageIndex(String currentLevel) {
        if (isMortal(currentLevel)) {
            return 0;
        }

        int idx = allStages().indexOf(currentLevel);
        return idx < 0 ? 0 : idx + 1;
    }

    public static int getRealmIndex(String currentLevel) {
        if (isMortal(currentLevel)) {
            return 0;
        }
        LevelDef levelDef = getLevelDef(currentLevel);
        return switch (levelDef.realm()) {
            case REALM_LIANQI -> 1;
            case REALM_ZHUJI -> 2;
            case REALM_JINDAN -> 3;
            case REALM_YUANYING -> 4;
            case REALM_HUASHEN -> 5;
            case REALM_LIANXU -> 6;
            case REALM_HETI -> 7;
            case REALM_DACHENG -> 8;
            case REALM_DUJIE -> 9;
            default -> 0;
        };
    }

    public static long getTotalQiNeeded(String currentLevel) {
        if (isMortal(currentLevel)) {
            return 10L;
        }
        return getLevelDef(currentLevel).qiNeeded();
    }

    public static float getBreakthroughFailChance(int luck, String currentLevel) {
        return Math.max(0.0f, Math.min(1.0f, 1.0f - (luck / 100.0f)));
    }

    public static boolean needsBreakthroughPill(String currentLevel) {
        if (isMortal(currentLevel)) return false;
        String next = getNextStage(currentLevel);
        if (next == null) return false;
        LevelDef cur = getLevelDef(currentLevel);
        LevelDef nxt = getLevelDef(next);
        return cur.realm().equals(REALM_LIANQI) && nxt.realm().equals(REALM_ZHUJI);
    }

    private static List<String> allStages() {
        return List.of(
                REALM_LIANQI + STAGE_EARLY,
                REALM_LIANQI + STAGE_MID,
                REALM_LIANQI + STAGE_LATE,
                REALM_ZHUJI + STAGE_EARLY,
                REALM_ZHUJI + STAGE_MID,
                REALM_ZHUJI + STAGE_LATE,
                REALM_JINDAN + STAGE_EARLY,
                REALM_JINDAN + STAGE_MID,
                REALM_JINDAN + STAGE_LATE,
                REALM_YUANYING + STAGE_EARLY,
                REALM_YUANYING + STAGE_MID,
                REALM_YUANYING + STAGE_LATE,
                REALM_HUASHEN + STAGE_EARLY,
                REALM_HUASHEN + STAGE_MID,
                REALM_HUASHEN + STAGE_LATE,
                REALM_LIANXU + STAGE_EARLY,
                REALM_LIANXU + STAGE_MID,
                REALM_LIANXU + STAGE_LATE,
                REALM_HETI + STAGE_EARLY,
                REALM_HETI + STAGE_MID,
                REALM_HETI + STAGE_LATE,
                REALM_DACHENG + STAGE_EARLY,
                REALM_DACHENG + STAGE_MID,
                REALM_DACHENG + STAGE_LATE,
                REALM_DUJIE + STAGE_EARLY,
                REALM_DUJIE + STAGE_MID,
                REALM_DUJIE + STAGE_LATE
        );
    }

    public static List<String> allLevels() {
        java.util.ArrayList<String> levels = new java.util.ArrayList<>();
        levels.add(REALM_MORTAL);
        levels.addAll(allStages());
        return levels;
    }
}
