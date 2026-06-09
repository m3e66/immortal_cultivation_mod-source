package com.example.immortal_cultivation_mod.attachment;

import java.util.List;
import java.util.Map;

public class CultivationLevels {
    public static final String REALM_MORTAL = "\u51e1\u4eba";
    public static final String REALM_LIANQI = "\u7ec3\u6c14";
    public static final String REALM_ZHUJI = "\u7b51\u57fa";
    public static final String REALM_JINDAN = "\u91d1\u4e39";
    public static final String REALM_YUANYING = "\u5143\u5a74";

    public static final String STAGE_EARLY = "\u524d\u671f";
    public static final String STAGE_MID = "\u4e2d\u671f";
    public static final String STAGE_LATE = "\u540e\u671f";

    public static final LevelDef LEVEL_MORTAL = new LevelDef(REALM_MORTAL, "", 20, 100, 20, 10);

    public record StageDef(String stage, int maxHp, int maxAge, int maxQi, int qiNeeded, float breakthroughFailMultiplier) {}

    public record RealmDef(String realm, List<StageDef> stages) {
        public StageDef getStage(String stage) {
            return stages.stream().filter(s -> s.stage().equals(stage)).findFirst().orElse(null);
        }
    }

    public record LevelDef(String realm, String stage, int maxHp, int maxAge, int maxQi, int qiNeeded) {}

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

    public static int getTotalQiNeeded(String currentLevel) {
        if (isMortal(currentLevel)) {
            return 10;
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
                REALM_YUANYING + STAGE_LATE
        );
    }
}
