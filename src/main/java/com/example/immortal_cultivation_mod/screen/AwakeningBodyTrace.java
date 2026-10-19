package com.example.immortal_cultivation_mod.screen;

import java.util.ArrayList;
import java.util.List;

/** Smooth vector tracing of Leo's front-facing body reference (1086 x 1448).
 * Coordinates stay in reference space until sampled once at class initialization.
 * The paper background is deliberately absent; the dantian is animated separately.
 */
final class AwakeningBodyTrace {
    private static final float SCALE = .31F;
    static final float CORE_X = x(540), CORE_Y = y(591);
    static final float[][][] OUTLINES = outlines();
    static final float[][][] CHANNELS = channels();

    private AwakeningBodyTrace() {}

    private static float x(float x) { return 640 + (x - 540) * SCALE; }
    private static float y(float y) { return 360 + (y - 711) * SCALE; }

    private static float[][][] outlines() {
        List<float[][]> paths = new ArrayList<>();
        // Left exterior: scalp, ear, neck, shoulder, elbow, open hand, leg, toes, crotch.
        Path edge = new Path(540,17)
                .c(493,16,471,47,478,96).c(464,93,467,123,477,137)
                .c(480,142,485,140,487,138).c(494,164,500,177,499,198)
                .c(498,217,495,225,482,231).c(460,242,446,248,432,252)
                .c(392,254,370,269,359,304).c(351,327,350,343,354,359)
                .c(357,378,340,411,338,442).c(337,464,336,485,326,504)
                .c(308,534,301,550,298,579).c(293,617,287,650,279,681)
                .c(257,693,249,703,239,727).c(236,736,225,749,225,758)
                .c(233,763,244,750,254,736).c(250,754,248,775,246,798)
                .c(243,815,247,816,254,811).c(251,827,259,825,266,814)
                .c(264,827,274,824,280,809).c(279,821,286,818,292,801)
                .c(307,775,318,740,318,716).c(321,704,317,694,317,690)
                .c(327,658,347,628,362,596).c(377,566,382,541,382,515)
                .c(383,488,393,468,401,440).c(408,421,413,404,415,386)
                .c(416,433,432,471,437,513).c(442,542,429,562,431,590)
                .c(428,615,421,650,412,682).c(397,723,397,755,402,805)
                .c(405,850,420,902,419,947).c(419,976,412,996,401,1025)
                .c(389,1065,400,1120,407,1160).c(415,1202,419,1231,417,1254)
                .c(410,1274,415,1280,416,1291).c(411,1321,396,1343,382,1364)
                .c(375,1374,376,1383,386,1388).c(381,1397,394,1399,399,1391)
                .c(393,1402,406,1405,413,1395).c(411,1405,425,1409,431,1399)
                .c(431,1408,449,1407,454,1397).c(464,1380,467,1360,464,1341)
                .c(460,1324,463,1304,465,1291).c(467,1280,460,1270,461,1252)
                .c(462,1214,478,1164,485,1116).c(491,1078,488,1062,484,1036)
                .c(481,1016,488,996,494,977).c(503,950,505,920,508,894)
                .c(514,846,528,800,531,728).c(535,729,537,729,540,729);
        addPair(paths, edge);
        // Face follows the same jaw and closed-eye features as the supplied reference.
        addPair(paths, new Path(478,97).c(483,129,486,164,513,186).c(524,194,533,196,540,195));
        addPair(paths, new Path(475,102).c(472,111,478,129,483,134));
        addPair(paths, new Path(497,94).c(504,89,517,89,527,96));
        addPair(paths, new Path(500,111).c(509,115,517,115,525,111));
        paths.add(new Path(534,132).c(530,139,529,144,536,144).sample(false));
        paths.add(new Path(542,144).c(546,146,548,143,550,142).sample(false));
        paths.add(new Path(524,160).c(534,157,545,158,555,160).sample(false));
        paths.add(new Path(535,168).c(539,169,543,169,547,168).sample(false));
        addPair(paths, new Path(460,258).c(489,263,510,260,525,269));
        addPair(paths, new Path(401,340).c(410,349,413,373,427,385));
        addPair(paths, new Path(450,393).c(464,395,477,395,490,391));
        paths.add(new Path(540,432).c(538,456,539,483,540,508).sample(false));
        addPair(paths, new Path(347,484).c(344,489,345,497,348,502));
        addPair(paths, new Path(399,449).c(392,468,385,486,374,500));
        addPair(paths, new Path(445,607).c(450,619,458,633,466,640));
        addPair(paths, new Path(515,707).c(522,719,529,725,540,728));
        addPair(paths, new Path(427,958).c(424,975,428,991,432,1004));
        addPair(paths, new Path(472,960).c(474,975,466,993,458,1005));
        // Palm creases, separate finger seams, and toe seams are part of the trace.
        addPair(paths, new Path(268,730).c(279,727,286,720,291,709));
        addPair(paths, new Path(269,758).c(265,776,260,799,254,811));
        addPair(paths, new Path(282,758).c(278,778,272,804,266,814));
        addPair(paths, new Path(296,763).c(292,780,286,802,280,809));
        addPair(paths, new Path(393,1367).c(387,1375,384,1383,386,1388));
        addPair(paths, new Path(407,1370).c(401,1380,397,1389,399,1391));
        addPair(paths, new Path(423,1373).c(417,1383,412,1391,413,1395));
        addPair(paths, new Path(436,1378).c(431,1386,428,1394,431,1399));
        return paths.toArray(float[][][]::new);
    }

    private static void addPair(List<float[][]> paths, Path path) {
        paths.add(path.sample(false));
        paths.add(path.sample(true));
    }

    private static float[][][] channels() {
        List<float[][]> paths = new ArrayList<>();
        paths.add(new Path(540,195).c(538,298,542,436,540,591).sample(false));
        addPair(paths, new Path(519,239).c(463,310,465,473,540,591));
        addPair(paths, new Path(514,258).c(432,255,394,348,374,459)
                .c(356,560,310,652,282,747));
        addPair(paths, new Path(430,367).c(455,398,501,410,528,450).c(546,486,500,548,540,591));
        addPair(paths, new Path(540,591).c(487,654,456,714,455,822)
                .c(455,944,420,1021,441,1117).c(451,1190,433,1314,419,1365));
        paths.add(new Path(540,591).c(482,541,458,444,468,347).c(474,301,508,267,540,237).sample(false));
        paths.add(new Path(540,591).c(598,541,622,444,612,347).c(606,301,572,267,540,237).sample(false));
        paths.add(new Path(540,591).c(493,615,501,679,540,707).c(579,679,587,615,540,591).sample(false));
        return paths.toArray(float[][][]::new);
    }

    private static final class Path {
        private final List<float[]> points = new ArrayList<>();
        private float lastX, lastY;
        Path(float x, float y) { lastX = x; lastY = y; points.add(new float[]{x,y}); }
        Path c(float ax, float ay, float bx, float by, float x, float y) {
            float startX = lastX, startY = lastY;
            // Sample more densely on long contours; all sampling happens once, never per frame.
            float length = distance(startX,startY,ax,ay) + distance(ax,ay,bx,by) + distance(bx,by,x,y);
            int steps = Math.max(4, (int)Math.ceil(length / 8));
            for (int i = 1; i <= steps; i++) {
                float t = i / (float)steps, u = 1-t;
                points.add(new float[]{u*u*u*startX+3*u*u*t*ax+3*u*t*t*bx+t*t*t*x,
                        u*u*u*startY+3*u*u*t*ay+3*u*t*t*by+t*t*t*y});
            }
            lastX = x; lastY = y; return this;
        }
        float[][] sample(boolean mirrored) {
            float[][] result = new float[points.size()][2];
            for (int i = 0; i < result.length; i++) {
                float[] point = points.get(i);
                result[i][0] = x(mirrored ? 1080-point[0] : point[0]);
                result[i][1] = y(point[1]);
            }
            return result;
        }
        private static float distance(float ax, float ay, float bx, float by) {
            return (float)Math.hypot(bx-ax,by-ay);
        }
    }
}
