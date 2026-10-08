package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client snapshot and short-lived HUD messages for 引道. */
public final class DaoGuidanceClient {
    private static final Map<String, DaoGuidancePayloads.ChapterState> CHAPTERS =
            new LinkedHashMap<>();
    private static String trackedChapter = "";
    private static String caveTargetDimension = "";
    private static int caveTargetX;
    private static int caveTargetZ;
    private static String noticeKind = "";
    private static String noticeId = "";
    private static String noticeNextId = "";
    private static int noticeTicks;

    private DaoGuidanceClient() {
    }

    public static void accept(
            DaoGuidancePayloads.ClientboundDaoGuidanceSyncPayload payload) {
        trackedChapter = payload.trackedChapter();
        caveTargetDimension = payload.caveTargetDimension();
        caveTargetX = payload.caveTargetX();
        caveTargetZ = payload.caveTargetZ();
        CHAPTERS.clear();
        for (DaoGuidancePayloads.ChapterState chapter : payload.chapters()) {
            CHAPTERS.put(chapter.id(), chapter);
        }
    }

    public static void notice(
            DaoGuidancePayloads.ClientboundDaoGuidanceNoticePayload payload) {
        noticeKind = payload.kind();
        noticeId = payload.id();
        noticeNextId = payload.nextId();
        noticeTicks = "complete".equals(noticeKind) ? 120 : 100;
    }

    public static void tick() {
        if (noticeTicks > 0) noticeTicks--;
    }

    public static String trackedChapter() {
        return trackedChapter;
    }

    public static boolean hasCaveTarget() {
        return !caveTargetDimension.isBlank();
    }

    public static String caveTargetDimension() {
        return caveTargetDimension;
    }

    public static int caveTargetX() {
        return caveTargetX;
    }

    public static int caveTargetZ() {
        return caveTargetZ;
    }

    public static int caveTargetDistance(double playerX, double playerZ) {
        return (int) Math.round(Math.hypot(
                caveTargetX - playerX, caveTargetZ - playerZ));
    }

    public static String caveTargetDirection(double playerX, double playerZ) {
        double angle = Math.atan2(
                caveTargetZ - playerZ, caveTargetX - playerX);
        int index = Math.floorMod(
                (int) Math.floor(angle / (Math.PI / 4.0D) + 0.5D), 8);
        return switch (index) {
            case 0 -> "E";
            case 1 -> "SE";
            case 2 -> "S";
            case 3 -> "SW";
            case 4 -> "W";
            case 5 -> "NW";
            case 6 -> "N";
            default -> "NE";
        };
    }

    public static DaoGuidancePayloads.ChapterState state(String chapterId) {
        return CHAPTERS.getOrDefault(chapterId,
                new DaoGuidancePayloads.ChapterState(
                        chapterId, false, false, 0, 0));
    }

    public static boolean discovered(String chapterId) {
        return state(chapterId).discovered();
    }

    public static boolean complete(String chapterId) {
        return state(chapterId).complete();
    }

    public static boolean isCurrentAction(String action) {
        DaoGuidanceContent.Chapter chapter =
                DaoGuidanceContent.chapter(trackedChapter);
        if (chapter == null) return false;
        DaoGuidancePayloads.ChapterState state = state(trackedChapter);
        if (!state.discovered() || state.complete()
                || state.objectiveIndex() >= chapter.objectives().size()) {
            return false;
        }
        return chapter.objectives().get(state.objectiveIndex())
                .action().equals(action);
    }

    public static int noticeTicks() {
        return noticeTicks;
    }

    public static String noticeKind() {
        return noticeKind;
    }

    public static String noticeId() {
        return noticeId;
    }

    public static String noticeNextId() {
        return noticeNextId;
    }
}
