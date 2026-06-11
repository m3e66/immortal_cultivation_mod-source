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
                    new StageDef(STAGE_EARLY, 50, 105, 50, 10, 0.01f),
                    new StageDef(STAGE_MID, 80, 110, 100, 500, 0.01f),
                    new StageDef(STAGE_LATE, 100, 115, 150, 1000, 0.01f)
            )),
            REALM_ZHUJI, new RealmDef(REALM_ZHUJI, List.of(
                    new StageDef(STAGE_EARLY, 150, 130, 200, 2000, 0.01f),
                    new StageDef(STAGE_MID, 180, 150, 300, 5000, 0.01f),
                    new StageDef(STAGE_LATE, 200, 170, 500, 10000, 0.01f)
            )),
            REALM_JINDAN, new RealmDef(REALM_JINDAN, List.of(
                    new StageDef(STAGE_EARLY, 500, 200, 300, 20000, 0.01f),
                    new StageDef(STAGE_MID, 600, 250, 400, 30000, 0.01f),
                    new StageDef(STAGE_LATE, 800, 300, 500, 50000, 0.01f)
            )),
            REALM_YUANYING, new RealmDef(REALM_YUANYING, List.of(
                    new StageDef(STAGE_EARLY, 1000, 400, 700, 100000, 0.01f),
                    new StageDef(STAGE_MID, 1200, 450, 800, 200000, 0.01f),
                    new StageDef(STAGE_LATE, 1500, 500, 900, 300000, 0.01f)
            )),
            REALM_HUASHEN, new RealmDef(REALM_HUASHEN, List.of(
                    new StageDef(STAGE_EARLY, 5000, 1200, 5000, 1000000L, 0.01f),
                    new StageDef(STAGE_MID, 7000, 1600, 7000, 2000000L, 0.01f),
                    new StageDef(STAGE_LATE, 10000, 2200, 10000, 4000000L, 0.01f)
            )),
            REALM_LIANXU, new RealmDef(REALM_LIANXU, List.of(
                    new StageDef(STAGE_EARLY, 15000, 3000, 20000, 8000000L, 0.01f),
                    new StageDef(STAGE_MID, 22000, 4500, 30000, 16000000L, 0.01f),
                    new StageDef(STAGE_LATE, 32000, 6000, 45000, 32000000L, 0.01f)
            )),
            REALM_HETI, new RealmDef(REALM_HETI, List.of(
                    new StageDef(STAGE_EARLY, 50000, 8000, 80000, 60000000L, 0.01f),
                    new StageDef(STAGE_MID, 70000, 12000, 120000, 120000000L, 0.01f),
                    new StageDef(STAGE_LATE, 100000, 18000, 180000, 240000000L, 0.01f)
            )),
            REALM_DACHENG, new RealmDef(REALM_DACHENG, List.of(
                    new StageDef(STAGE_EARLY, 150000, 30000, 300000, 500000000L, 0.01f),
                    new StageDef(STAGE_MID, 220000, 50000, 500000, 1000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 320000, 80000, 800000, 2000000000L, 0.01f)
            )),
            REALM_DUJIE, new RealmDef(REALM_DUJIE, List.of(
                    new StageDef(STAGE_EARLY, 500000, 100000, 1200000, 5000000000L, 0.01f),
                    new StageDef(STAGE_MID, 800000, 200000, 2000000, 10000000000L, 0.01f),
                    new StageDef(STAGE_LATE, 1200000, 500000, 3500000, 20000000000L, 0.01f)
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

    public static int getStageIndex(String currentLevel) {
        if (isMortal(currentLevel)) {
            return 0;
        }

        int idx = allStages().indexOf(currentLevel);
        return idx < 0 ? 0 : idx + 1;
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
