package com.example.immortal_cultivation_mod.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Traceable talisman scripts built from one traditional vertical grammar:
 * head, authority, body, core, command and delivery foot.  Recipes share that
 * grammar while replacing each section with marks that describe their own source,
 * element, function and delivery method.  The law seal is deliberately not part of
 * these brush paths; it authenticates a successfully completed talisman afterward.
 */
public final class TalismanDrawingPatterns {
    private static final int MAX_TRACE_POINTS = 1_024;
    private static final int FRAME_PADDING = 18;
    private static final int[][] OUTER_FRAME_SOURCE_CONTOUR = {
            {26, 63}, {27, 62}, {27, 60}, {28, 59}, {28, 47},
            {29, 46}, {29, 44}, {28, 43}, {28, 42}, {29, 41},
            {30, 40}, {28, 39}, {27, 38}, {27, 37}, {28, 36},
            {29, 35}, {29, 34}, {30, 33}, {30, 27}, {29, 25},
            {28, 24}, {29, 23}, {31, 22}, {32, 21}, {32, 20},
            {33, 19}, {32, 18}, {31, 17}, {30, 16}, {44, 16},
            {43, 17}, {42, 18}, {41, 19}, {42, 20}, {42, 21},
            {43, 22}, {45, 23}, {46, 24}, {45, 25}, {44, 27},
            {44, 33}, {45, 34}, {45, 35}, {46, 36}, {47, 37},
            {47, 38}, {46, 39}, {44, 40}, {45, 41}, {46, 42},
            {46, 43}, {45, 44}, {45, 46}, {46, 47}, {46, 59},
            {47, 60}, {47, 62}, {48, 63}
    };
    private static final List<Point> OUTER_FRAME_POINTS = buildOuterFramePoints();

    private TalismanDrawingPatterns() {
    }

    public static Pattern forRecipe(String recipeId) {
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId);
        if (recipe == null) return new Pattern(List.of(), Anatomy.EMPTY);
        return createPattern(recipe.patternId(), TalismanRecipes.indexOf(recipeId),
                recipe.strengthTier());
    }

    private static Pattern createPattern(String patternId, int ignoredIndex, int ignoredStrengthTier) {
        Anatomy anatomy = anatomy(patternId);
        HeadStyle head = headStyle(patternId);
        CoreStyle core = coreStyle(patternId);
        FootStyle foot = footStyle(patternId);
        List<Stroke> strokes = new ArrayList<>();
        addHead(strokes, head);
        addAuthority(strokes, head);
        addBodyPattern(strokes, patternId);
        addCore(strokes, core);
        addCommand(strokes);
        addFoot(strokes, foot);
        addOuterFrame(strokes);
        return new Pattern(organizeInternalStrokes(joinAnatomySections(strokes)), anatomy);
    }

    private static Anatomy anatomy(String patternId) {
        return switch (patternId) {
            case "fuchen" -> new Anatomy("清", "水", "净尘", "净", "敕令", "散");
            case "juguang" -> new Anatomy("灵", "光", "聚光", "明", "敕令", "引");
            case "shouyin" -> new Anatomy("灵", "静", "敛声", "寂", "敕令", "护");
            case "juhuo" -> new Anatomy("火", "炎", "聚火", "炎", "敕令", "发");
            case "fengqie" -> new Anatomy("灵", "风", "风切", "刃", "敕令", "发");
            case "fenhuo_xing" -> new Anatomy("火", "炎", "分火", "星", "敕令", "发");
            case "bojia_hu" -> new Anatomy("镇", "金", "薄甲", "护", "敕令", "护");
            case "fusi" -> new Anatomy("镇", "木", "缚丝", "困", "敕令", "封");
            case "xingqi" -> new Anatomy("灵", "风", "行气", "速", "敕令", "护");
            case "chuansheng" -> new Anatomy("灵", "音", "传声", "信", "敕令", "引");
            case "zhenshi_gu" -> new Anatomy("镇", "阴", "镇尸", "镇", "敕令", "镇");
            case "bimu" -> new Anatomy("镇", "封", "闭目", "盲", "敕令", "封");
            case "xuhe" -> new Anatomy("灵", "生", "续合", "修", "敕令", "护");
            case "dingshen_anhun" -> new Anatomy("清", "魂", "安魂", "定", "敕令", "护");
            case "zhenyu" -> new Anatomy("镇", "地", "镇域", "静", "敕令", "镇");
            case "juya_baofeng" -> new Anatomy("灵", "风", "聚压", "爆", "敕令", "散");
            case "huoyu" -> new Anatomy("火", "炎", "火狱", "焚", "敕令", "散");
            case "wulei_yin" -> new Anatomy("雷", "霆", "五雷", "雷", "敕令", "发");
            case "liuyin_zhuiji" -> new Anatomy("灵", "印", "追迹", "标", "敕令", "引");
            case "dunkong" -> new Anatomy("灵", "空", "遁空", "遁", "敕令", "引");
            case "huabing" -> new Anatomy("灵", "兵", "化兵", "刃", "敕令", "护");
            case "chongguang_hushen" -> new Anatomy("镇", "光", "重光", "护", "敕令", "护");
            case "suomai_fengling" -> new Anatomy("镇", "封", "锁脉", "禁", "敕令", "封");
            case "xunxi_pozhang" -> new Anatomy("灵", "山", "破障", "开", "敕令", "散");
            case "xuanyan_fenjie" -> new Anatomy("火", "玄", "焚界", "焚", "敕令", "散");
            case "wuyue_zhene" -> new Anatomy("镇", "岳", "五岳", "镇", "敕令", "护");
            case "qianli_chuanshen" -> new Anatomy("灵", "音", "千里", "讯", "敕令", "引");
            case "gu_jinkong" -> new Anatomy("镇", "地", "禁空", "压", "敕令", "镇");
            default -> Anatomy.EMPTY;
        };
    }

    private static HeadStyle headStyle(String patternId) {
        return switch (patternId) {
            case "fuchen", "dingshen_anhun" -> HeadStyle.CLEAN;
            case "juhuo", "fenhuo_xing", "huoyu", "xuanyan_fenjie" -> HeadStyle.FIRE;
            case "wulei_yin" -> HeadStyle.THUNDER;
            case "bojia_hu", "fusi", "zhenshi_gu", "bimu", "zhenyu",
                    "chongguang_hushen", "suomai_fengling", "wuyue_zhene",
                    "gu_jinkong" -> HeadStyle.SUPPRESS;
            default -> HeadStyle.SPIRIT;
        };
    }

    private static CoreStyle coreStyle(String patternId) {
        return switch (patternId) {
            case "fuchen" -> CoreStyle.PURIFY;
            case "juguang" -> CoreStyle.LIGHT;
            case "shouyin" -> CoreStyle.SILENCE;
            case "juhuo", "fenhuo_xing", "huoyu", "xuanyan_fenjie" -> CoreStyle.FIRE;
            case "fengqie", "huabing" -> CoreStyle.BLADE;
            case "bojia_hu", "chongguang_hushen" -> CoreStyle.GUARD;
            case "fusi", "suomai_fengling", "bimu" -> CoreStyle.BIND;
            case "xingqi", "dunkong" -> CoreStyle.MOTION;
            case "chuansheng", "qianli_chuanshen" -> CoreStyle.MESSAGE;
            case "zhenshi_gu", "zhenyu", "gu_jinkong" -> CoreStyle.SUPPRESS;
            case "xuhe" -> CoreStyle.REPAIR;
            case "dingshen_anhun" -> CoreStyle.SOUL;
            case "juya_baofeng" -> CoreStyle.BURST;
            case "wulei_yin" -> CoreStyle.THUNDER;
            case "liuyin_zhuiji" -> CoreStyle.TRACK;
            case "xunxi_pozhang" -> CoreStyle.BREAK;
            case "wuyue_zhene" -> CoreStyle.MOUNTAIN;
            default -> CoreStyle.SPIRIT;
        };
    }

    private static FootStyle footStyle(String patternId) {
        return switch (patternId) {
            case "juhuo", "fengqie", "fenhuo_xing", "wulei_yin" -> FootStyle.LAUNCH;
            case "shouyin", "bojia_hu", "xingqi", "xuhe", "dingshen_anhun",
                    "chongguang_hushen", "wuyue_zhene", "huabing" -> FootStyle.ATTACH;
            case "fusi", "bimu", "suomai_fengling" -> FootStyle.SEAL;
            case "fuchen", "juya_baofeng", "huoyu", "xunxi_pozhang",
                    "xuanyan_fenjie" -> FootStyle.SCATTER;
            case "juguang", "chuansheng", "liuyin_zhuiji",
                    "qianli_chuanshen", "dunkong" -> FootStyle.GUIDE;
            default -> FootStyle.STAY;
        };
    }

    private static void addHead(List<Stroke> strokes, HeadStyle style) {
        switch (style) {
            case CLEAN -> {
                strokes.add(stroke(Section.HEAD, 350, 52, 330, 76, 354, 104));
                strokes.add(stroke(Section.HEAD, 500, 42, 478, 72, 503, 105));
                strokes.add(stroke(Section.HEAD, 650, 52, 630, 78, 654, 104));
                strokes.add(stroke(Section.HEAD, 410, 125, 500, 88, 590, 125));
            }
            case FIRE -> {
                strokes.add(stroke(Section.HEAD, 345, 110, 320, 78, 354, 46, 370, 80, 345, 110));
                strokes.add(stroke(Section.HEAD, 500, 112, 468, 72, 506, 36, 526, 78, 500, 112));
                strokes.add(stroke(Section.HEAD, 655, 110, 630, 78, 664, 48, 677, 82, 655, 110));
                strokes.add(stroke(Section.HEAD, 400, 130, 500, 98, 600, 130));
            }
            case THUNDER -> {
                strokes.add(stroke(Section.HEAD, 330, 45, 382, 78, 344, 108, 410, 125));
                strokes.add(stroke(Section.HEAD, 445, 35, 505, 74, 466, 112, 525, 130));
                strokes.add(stroke(Section.HEAD, 570, 43, 628, 78, 590, 108, 668, 124));
                strokes.add(stroke(Section.HEAD, 392, 144, 500, 108, 608, 144));
            }
            case SUPPRESS -> {
                strokes.add(stroke(Section.HEAD, 346, 46, 346, 104, 378, 124));
                strokes.add(stroke(Section.HEAD, 500, 35, 500, 112, 532, 132));
                strokes.add(stroke(Section.HEAD, 654, 46, 654, 104, 622, 124));
                strokes.add(stroke(Section.HEAD, 322, 142, 500, 105, 678, 142));
            }
            case SPIRIT -> {
                strokes.add(stroke(Section.HEAD, 352, 48, 326, 72, 350, 101, 374, 77, 352, 48));
                strokes.add(stroke(Section.HEAD, 500, 38, 472, 68, 500, 103, 528, 68, 500, 38));
                strokes.add(stroke(Section.HEAD, 648, 48, 622, 74, 648, 102, 674, 76, 648, 48));
                strokes.add(stroke(Section.HEAD, 402, 132, 452, 102, 500, 138, 550, 102, 600, 132));
            }
        }
    }

    private static void addAuthority(List<Stroke> strokes, HeadStyle style) {
        switch (style) {
            case FIRE -> {
                strokes.add(stroke(Section.AUTHORITY, 500, 145, 558, 182, 575, 231,
                        542, 270, 500, 281, 458, 268, 426, 228, 444, 182, 500, 145));
                strokes.add(stroke(Section.AUTHORITY, 500, 170, 475, 215, 500, 254, 526, 215, 500, 170));
            }
            case THUNDER -> {
                strokes.add(stroke(Section.AUTHORITY, 500, 145, 570, 181, 548, 222,
                        593, 247, 520, 282, 478, 246, 420, 268, 444, 216, 405, 188, 500, 145));
                strokes.add(stroke(Section.AUTHORITY, 470, 176, 530, 211, 484, 236, 540, 266));
            }
            case SUPPRESS -> {
                strokes.add(stroke(Section.AUTHORITY, 408, 176, 500, 146, 592, 176,
                        574, 271, 426, 271, 408, 176));
                strokes.add(stroke(Section.AUTHORITY, 438, 204, 562, 204));
                strokes.add(stroke(Section.AUTHORITY, 500, 163, 500, 282));
            }
            case CLEAN -> {
                strokes.add(stroke(Section.AUTHORITY, 500, 146, 561, 177, 579, 220,
                        553, 260, 500, 280, 447, 260, 421, 220, 439, 177, 500, 146));
                strokes.add(stroke(Section.AUTHORITY, 448, 225, 480, 207, 512, 229, 550, 208));
            }
            case SPIRIT -> {
                strokes.add(stroke(Section.AUTHORITY, 500, 145, 558, 174, 579, 218,
                        562, 258, 518, 280, 469, 270, 430, 238, 426, 195,
                        457, 160, 500, 145));
                strokes.add(stroke(Section.AUTHORITY, 471, 186, 526, 174, 547, 215,
                        520, 244, 480, 235, 471, 205, 494, 192));
            }
        }
        strokes.add(stroke(Section.AUTHORITY, 500, 278, 500, 305));
    }

    private static void addBodyPattern(List<Stroke> strokes, String patternId) {
        List<Stroke> authored = new ArrayList<>();
        addSignature(authored, patternId);
        for (Stroke source : authored) {
            List<Point> points = new ArrayList<>(source.points().size());
            for (Point point : source.points()) {
                // The signature is already authored for the available width.  Shrinking it
                // here and again in narrowInternalStrokes made the talisman body look
                // unnaturally compressed compared with the head, core, and foot.
                int x = point.x;
                int y = (int) Math.round(300.0D + (point.y - 285.0D) * 0.66D);
                points.add(new Point(clamp(x, 245, 755), clamp(y, 300, 580)));
            }
            strokes.add(new Stroke(Section.BODY, List.copyOf(points)));
        }
    }

    private static void addCore(List<Stroke> strokes, CoreStyle style) {
        switch (style) {
            case PURIFY -> {
                strokes.add(stroke(Section.CORE, 500, 564, 560, 596, 548, 654, 500, 685,
                        452, 654, 440, 596, 500, 564));
                strokes.add(stroke(Section.CORE, 458, 634, 487, 612, 516, 637, 544, 612));
            }
            case LIGHT -> {
                strokes.add(stroke(Section.CORE, 500, 568, 544, 584, 568, 625, 550, 666,
                        500, 684, 452, 664, 433, 624, 456, 583, 500, 568));
                strokes.add(stroke(Section.CORE, 500, 548, 500, 704));
                strokes.add(stroke(Section.CORE, 420, 626, 580, 626));
            }
            case SILENCE -> {
                strokes.add(stroke(Section.CORE, 442, 584, 558, 584, 558, 675, 442, 675, 442, 584));
                strokes.add(stroke(Section.CORE, 456, 646, 500, 610, 544, 646));
                strokes.add(stroke(Section.CORE, 452, 692, 548, 692));
            }
            case FIRE -> {
                strokes.add(stroke(Section.CORE, 500, 558, 463, 604, 488, 631, 454, 669,
                        500, 696, 546, 669, 512, 631, 538, 604, 500, 558));
                strokes.add(stroke(Section.CORE, 500, 590, 478, 627, 500, 663, 522, 627, 500, 590));
            }
            case BLADE -> {
                strokes.add(stroke(Section.CORE, 450, 582, 530, 632, 474, 687));
                strokes.add(stroke(Section.CORE, 550, 582, 470, 632, 526, 687));
                strokes.add(stroke(Section.CORE, 500, 558, 500, 704));
            }
            case GUARD -> {
                strokes.add(stroke(Section.CORE, 500, 562, 570, 588, 556, 656, 500, 700,
                        444, 656, 430, 588, 500, 562));
                strokes.add(stroke(Section.CORE, 500, 590, 540, 608, 531, 646,
                        500, 672, 469, 646, 460, 608, 500, 590));
            }
            case BIND -> {
                strokes.add(stroke(Section.CORE, 444, 580, 556, 690));
                strokes.add(stroke(Section.CORE, 556, 580, 444, 690));
                strokes.add(stroke(Section.CORE, 432, 610, 568, 610));
                strokes.add(stroke(Section.CORE, 432, 662, 568, 662));
            }
            case MOTION -> {
                strokes.add(stroke(Section.CORE, 448, 584, 535, 584, 480, 626,
                        552, 626, 468, 684, 548, 684));
                strokes.add(stroke(Section.CORE, 420, 646, 465, 620));
            }
            case MESSAGE -> {
                strokes.add(stroke(Section.CORE, 458, 594, 500, 574, 542, 594));
                strokes.add(stroke(Section.CORE, 430, 626, 500, 594, 570, 626));
                strokes.add(stroke(Section.CORE, 405, 667, 500, 626, 595, 667));
            }
            case SUPPRESS -> {
                strokes.add(stroke(Section.CORE, 432, 580, 568, 580, 558, 690, 442, 690, 432, 580));
                strokes.add(stroke(Section.CORE, 465, 606, 535, 606));
                strokes.add(stroke(Section.CORE, 500, 580, 500, 692));
                strokes.add(stroke(Section.CORE, 460, 654, 540, 654));
            }
            case REPAIR -> {
                strokes.add(stroke(Section.CORE, 470, 574, 450, 610, 480, 638, 452, 674, 485, 700));
                strokes.add(stroke(Section.CORE, 530, 574, 550, 610, 520, 638, 548, 674, 515, 700));
                strokes.add(stroke(Section.CORE, 462, 604, 536, 617, 465, 646, 538, 660, 468, 684));
            }
            case SOUL -> {
                strokes.add(stroke(Section.CORE, 500, 570, 548, 590, 560, 630, 532, 659,
                        500, 652, 476, 625, 489, 599, 520, 599));
                strokes.add(stroke(Section.CORE, 442, 588, 420, 640, 450, 690, 490, 706));
                strokes.add(stroke(Section.CORE, 558, 588, 580, 640, 550, 690, 510, 706));
            }
            case BURST -> {
                strokes.add(stroke(Section.CORE, 500, 558, 520, 608, 574, 610, 532, 643,
                        548, 696, 500, 665, 452, 696, 468, 643, 426, 610,
                        480, 608, 500, 558));
                strokes.add(stroke(Section.CORE, 500, 592, 500, 674));
            }
            case THUNDER -> {
                strokes.add(stroke(Section.CORE, 462, 565, 516, 606, 480, 638, 535, 692));
                strokes.add(stroke(Section.CORE, 430, 596, 480, 620, 450, 651, 500, 683));
                strokes.add(stroke(Section.CORE, 540, 582, 570, 613, 535, 638, 570, 670));
            }
            case TRACK -> {
                strokes.add(stroke(Section.CORE, 420, 632, 460, 594, 500, 580, 540, 594,
                        580, 632, 540, 670, 500, 684, 460, 670, 420, 632));
                strokes.add(stroke(Section.CORE, 500, 604, 526, 632, 500, 660, 474, 632, 500, 604));
            }
            case BREAK -> {
                strokes.add(stroke(Section.CORE, 442, 574, 490, 610, 462, 642, 512, 690));
                strokes.add(stroke(Section.CORE, 558, 574, 510, 610, 538, 642, 488, 690));
                strokes.add(stroke(Section.CORE, 420, 666, 468, 642));
                strokes.add(stroke(Section.CORE, 580, 666, 532, 642));
            }
            case MOUNTAIN -> {
                strokes.add(stroke(Section.CORE, 410, 674, 455, 610, 492, 648,
                        530, 576, 590, 674));
                strokes.add(stroke(Section.CORE, 430, 686, 570, 686));
                strokes.add(stroke(Section.CORE, 466, 702, 500, 678, 536, 702));
            }
            case SPIRIT -> {
                strokes.add(stroke(Section.CORE, 500, 568, 548, 590, 560, 630,
                        530, 670, 482, 681, 440, 653, 438, 610, 470, 582, 520, 587));
                strokes.add(stroke(Section.CORE, 468, 624, 500, 602, 532, 626, 500, 652, 468, 624));
            }
        }
    }

    private static void addCommand(List<Stroke> strokes) {
        strokes.add(stroke(Section.COMMAND, 500, 688, 476, 718, 520, 744,
                480, 770, 522, 798, 500, 824));
        strokes.add(stroke(Section.COMMAND, 425, 714, 458, 694, 446, 731,
                466, 752, 446, 781, 470, 803));
        strokes.add(stroke(Section.COMMAND, 575, 714, 542, 694, 554, 731,
                534, 752, 554, 781, 530, 803));
    }

    private static void addFoot(List<Stroke> strokes, FootStyle style) {
        switch (style) {
            case LAUNCH -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 898, 430, 870, 500, 938, 570, 870, 500, 898));
                strokes.add(stroke(Section.FOOT, 380, 930, 450, 902));
                strokes.add(stroke(Section.FOOT, 620, 930, 550, 902));
            }
            case ATTACH -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 870, 438, 904,
                        500, 950, 562, 904, 500, 870));
                strokes.add(stroke(Section.FOOT, 420, 928, 500, 888, 580, 928));
            }
            case SEAL -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 858));
                strokes.add(stroke(Section.FOOT, 410, 858, 590, 858, 576, 950, 424, 950, 410, 858));
                strokes.add(stroke(Section.FOOT, 440, 890, 500, 930, 560, 890));
                strokes.add(stroke(Section.FOOT, 560, 890, 500, 872, 440, 890));
            }
            case SCATTER -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 876));
                strokes.add(stroke(Section.FOOT, 500, 876, 430, 918, 350, 926));
                strokes.add(stroke(Section.FOOT, 500, 876, 570, 918, 650, 926));
                strokes.add(stroke(Section.FOOT, 500, 876, 500, 958));
            }
            case GUIDE -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 864, 548, 888,
                        520, 916, 558, 946));
                strokes.add(stroke(Section.FOOT, 500, 864, 452, 888, 480, 916, 442, 946));
                strokes.add(stroke(Section.FOOT, 402, 948, 442, 946, 466, 920));
                strokes.add(stroke(Section.FOOT, 598, 948, 558, 946, 534, 920));
            }
            case STAY -> {
                strokes.add(stroke(Section.FOOT, 500, 816, 500, 870));
                strokes.add(stroke(Section.FOOT, 400, 874, 600, 874));
                strokes.add(stroke(Section.FOOT, 430, 874, 450, 946, 550, 946, 570, 874));
                strokes.add(stroke(Section.FOOT, 450, 916, 500, 888, 550, 916));
            }
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void addSignature(List<Stroke> strokes, String patternId) {
        switch (patternId) {
            case "fuchen" -> {
                strokes.add(stroke(475, 315, 405, 350, 345, 405, 295, 475, 210, 535));
                strokes.add(stroke(525, 330, 600, 370, 655, 435, 710, 500, 795, 550));
                strokes.add(stroke(455, 565, 385, 620, 300, 650, 235, 715));
            }
            case "juguang" -> {
                strokes.add(stroke(500, 300, 565, 320, 610, 375, 620, 435,
                        585, 490, 520, 515, 455, 500, 400, 455, 390, 395,
                        425, 340, 500, 300));
                strokes.add(stroke(420, 405, 580, 405));
                strokes.add(stroke(500, 330, 500, 485));
            }
            case "shouyin" -> {
                strokes.add(stroke(385, 310, 325, 365, 340, 500, 400, 570, 465, 535));
                strokes.add(stroke(615, 315, 680, 375, 660, 500, 600, 570, 530, 535));
                strokes.add(stroke(405, 390, 465, 430, 535, 430, 595, 390));
            }
            case "juhuo" -> {
                strokes.add(stroke(505, 285, 445, 365, 485, 420, 430, 500,
                        475, 585, 545, 530, 575, 450, 545, 380, 505, 285));
                strokes.add(stroke(420, 360, 385, 420, 405, 475));
                strokes.add(stroke(590, 350, 625, 410, 600, 475));
            }
            case "fengqie" -> {
                strokes.add(stroke(270, 420, 350, 345, 460, 335, 570, 385, 700, 350));
                strokes.add(stroke(300, 535, 405, 455, 535, 455, 670, 520, 765, 455));
                strokes.add(stroke(395, 620, 500, 555, 615, 600, 700, 570));
            }
            case "fenhuo_xing" -> {
                strokes.add(stroke(500, 300, 535, 380, 620, 385, 550, 440,
                        575, 525, 500, 475, 425, 525, 450, 440, 380, 385,
                        465, 380, 500, 300));
                strokes.add(stroke(330, 340, 370, 385));
                strokes.add(stroke(650, 325, 625, 375));
                strokes.add(stroke(675, 520, 625, 545));
            }
            case "bojia_hu" -> {
                strokes.add(stroke(500, 295, 640, 350, 620, 500, 550, 590,
                        500, 630, 445, 585, 375, 500, 360, 350, 500, 295));
                strokes.add(stroke(415, 410, 500, 370, 585, 415));
                strokes.add(stroke(430, 485, 500, 535, 570, 480));
            }
            case "fusi" -> {
                strokes.add(stroke(310, 350, 400, 300, 455, 370, 405, 445,
                        350, 510, 430, 585, 500, 520));
                strokes.add(stroke(690, 350, 600, 300, 545, 370, 595, 445,
                        650, 510, 570, 585, 500, 520));
            }
            case "xingqi" -> {
                strokes.add(stroke(390, 325, 345, 410, 400, 480, 335, 560, 265, 585));
                strokes.add(stroke(575, 310, 625, 390, 575, 465, 650, 535, 735, 545));
                strokes.add(stroke(430, 610, 510, 550, 590, 600, 670, 570));
            }
            case "chuansheng" -> {
                strokes.add(stroke(470, 350, 400, 320, 335, 355, 305, 420, 325, 490));
                strokes.add(stroke(530, 345, 605, 315, 675, 360, 700, 430, 675, 505));
                strokes.add(stroke(405, 535, 500, 490, 595, 540, 500, 600, 405, 535));
            }
            case "zhenshi_gu" -> {
                strokes.add(stroke(350, 325, 650, 325, 640, 590, 360, 590, 350, 325));
                strokes.add(stroke(390, 385, 610, 535));
                strokes.add(stroke(610, 385, 390, 535));
                strokes.add(stroke(430, 455, 570, 455));
            }
            case "bimu" -> {
                strokes.add(stroke(315, 430, 390, 365, 500, 345, 610, 375, 685, 440));
                strokes.add(stroke(315, 430, 400, 495, 500, 515, 605, 490, 685, 440));
                strokes.add(stroke(500, 345, 475, 430, 510, 515));
            }
            case "xuhe" -> {
                strokes.add(stroke(475, 300, 430, 375, 480, 430, 445, 500, 490, 585));
                strokes.add(stroke(525, 300, 565, 370, 520, 435, 555, 500, 510, 585));
                strokes.add(stroke(410, 410, 475, 425, 535, 400, 595, 420));
                strokes.add(stroke(420, 520, 480, 500, 530, 525, 585, 505));
            }
            case "dingshen_anhun" -> {
                strokes.add(stroke(500, 300, 575, 335, 600, 405, 565, 465,
                        500, 485, 445, 455, 430, 400, 465, 360, 520, 365));
                strokes.add(stroke(355, 335, 310, 435, 350, 545, 430, 610));
                strokes.add(stroke(645, 335, 690, 435, 650, 545, 570, 610));
            }
            case "zhenyu" -> {
                strokes.add(stroke(500, 285, 655, 385, 620, 555, 500, 630,
                        375, 555, 345, 385, 500, 285));
                strokes.add(stroke(410, 390, 500, 345, 590, 400, 565, 505,
                        500, 550, 430, 500, 410, 390));
                strokes.add(stroke(360, 465, 640, 465));
            }
            case "juya_baofeng" -> {
                strokes.add(stroke(270, 390, 350, 350, 420, 385, 450, 455, 500, 480));
                strokes.add(stroke(730, 390, 650, 350, 580, 385, 550, 455, 500, 480));
                strokes.add(stroke(300, 570, 390, 525, 500, 550, 610, 525, 700, 570));
            }
            case "huoyu" -> {
                strokes.add(stroke(325, 335, 290, 425, 320, 570, 395, 630));
                strokes.add(stroke(675, 335, 710, 425, 680, 570, 605, 630));
                strokes.add(stroke(500, 295, 435, 385, 475, 455, 430, 535,
                        500, 610, 570, 535, 525, 455, 565, 385, 500, 295));
            }
            case "wulei_yin" -> {
                strokes.add(stroke(350, 300, 410, 375, 365, 435, 430, 515));
                strokes.add(stroke(455, 285, 500, 365, 465, 440, 515, 540));
                strokes.add(stroke(555, 295, 600, 370, 565, 440, 625, 520));
                strokes.add(stroke(300, 575, 410, 540, 500, 590, 595, 540, 705, 580));
            }
            case "liuyin_zhuiji" -> {
                strokes.add(stroke(500, 290, 565, 370, 545, 465, 500, 550,
                        455, 465, 435, 370, 500, 290));
                strokes.add(stroke(335, 420, 500, 355, 665, 420, 500, 500, 335, 420));
                strokes.add(stroke(500, 260, 500, 610));
            }
            case "dunkong" -> {
                strokes.add(stroke(285, 430, 365, 350, 470, 330, 555, 380,
                        620, 460, 560, 535, 455, 555, 360, 515, 300, 445,
                        360, 390, 455, 405, 535, 475, 500, 545, 430, 585));
                strokes.add(stroke(330, 605, 440, 560, 555, 600, 680, 545));
            }
            case "huabing" -> {
                strokes.add(stroke(500, 285, 535, 350, 520, 430, 590, 500,
                        530, 555, 500, 630, 470, 555, 410, 500, 480, 430,
                        465, 350, 500, 285));
                strokes.add(stroke(310, 545, 420, 470, 500, 515, 585, 460,
                        700, 535));
                strokes.add(stroke(390, 330, 610, 585));
            }
            case "chongguang_hushen" -> {
                strokes.add(stroke(500, 285, 660, 350, 635, 535, 500, 640,
                        365, 535, 340, 350, 500, 285));
                strokes.add(stroke(500, 345, 595, 390, 575, 505, 500, 570,
                        425, 505, 405, 390, 500, 345));
                strokes.add(stroke(445, 440, 500, 405, 555, 445, 500, 500, 445, 440));
            }
            case "suomai_fengling" -> {
                strokes.add(stroke(330, 350, 400, 315, 455, 360, 425, 420,
                        365, 440, 335, 500, 390, 545, 460, 520));
                strokes.add(stroke(670, 350, 600, 315, 545, 360, 575, 420,
                        635, 440, 665, 500, 610, 545, 540, 520));
                strokes.add(stroke(390, 585, 500, 535, 610, 585));
            }
            case "xunxi_pozhang" -> {
                strokes.add(stroke(325, 330, 380, 400, 350, 470, 410, 545, 380, 615));
                strokes.add(stroke(675, 320, 595, 390, 635, 445, 555, 500,
                        590, 560, 500, 620));
                strokes.add(stroke(285, 520, 430, 475, 545, 430, 710, 350));
            }
            case "xuanyan_fenjie" -> {
                strokes.add(stroke(500, 285, 430, 350, 470, 415, 385, 485,
                        435, 585, 500, 530, 565, 585, 615, 485, 530, 415,
                        570, 350, 500, 285));
                strokes.add(stroke(275, 365, 350, 420, 300, 510, 395, 610));
                strokes.add(stroke(725, 365, 650, 420, 700, 510, 605, 610));
                strokes.add(stroke(350, 630, 430, 580, 500, 625, 570, 580, 650, 630));
            }
            case "wuyue_zhene" -> {
                strokes.add(stroke(255, 525, 330, 420, 395, 490, 500, 315,
                        590, 485, 665, 405, 750, 525));
                strokes.add(stroke(285, 560, 715, 560));
                strokes.add(stroke(350, 610, 500, 575, 650, 610));
            }
            case "qianli_chuanshen" -> {
                strokes.add(stroke(300, 340, 390, 300, 455, 355, 430, 430,
                        500, 475, 570, 430, 545, 355, 610, 300, 700, 340));
                strokes.add(stroke(285, 500, 380, 455, 500, 520, 620, 455, 715, 500));
                strokes.add(stroke(350, 610, 440, 545, 520, 600, 615, 545, 690, 590));
            }
            case "gu_jinkong" -> {
                strokes.add(stroke(315, 310, 685, 310, 665, 625, 335, 625, 315, 310));
                strokes.add(stroke(390, 370, 500, 445, 610, 370));
                strokes.add(stroke(370, 510, 500, 455, 630, 510));
                strokes.add(stroke(430, 565, 500, 520, 570, 565));
            }
            default -> {
                strokes.add(stroke(370, 340, 500, 285, 635, 350, 585, 480,
                        500, 570, 420, 485, 370, 340));
                strokes.add(stroke(410, 430, 500, 375, 590, 430, 500, 515, 410, 430));
            }
        }
    }

    /**
     * Gives every semantic part its own top-to-bottom band.  Fitting sections
     * independently prevents the dense body glyph from colliding with the head,
     * core, command, or delivery foot and makes every recipe share one readable
     * talisman grammar.
     */
    private static List<Stroke> organizeInternalStrokes(List<Stroke> joined) {
        List<Stroke> organized = new ArrayList<>(joined.size());
        for (Stroke stroke : joined) {
            if (stroke.section() == Section.FRAME) {
                // The supplied temp.png contour remains exact and unscaled.
                organized.add(stroke);
                continue;
            }
            SectionLayout layout = layout(stroke.section());
            List<Point> fitted = fitInside(stroke.points(), layout);
            fitted = orientTopToBottom(fitted);
            organized.add(new Stroke(stroke.section(), keepInsideOuterFrame(fitted)));
        }
        return List.copyOf(organized);
    }

    private static SectionLayout layout(Section section) {
        return switch (section) {
            case HEAD -> new SectionLayout(430, 570, 145, 230);
            case AUTHORITY -> new SectionLayout(390, 610, 245, 345);
            case BODY -> new SectionLayout(385, 615, 360, 565);
            case CORE -> new SectionLayout(400, 600, 580, 690);
            case COMMAND -> new SectionLayout(420, 580, 705, 800);
            case FOOT -> new SectionLayout(360, 640, 815, 925);
            case FRAME -> new SectionLayout(285, 715, 115, 960);
        };
    }

    private static List<Point> fitInside(List<Point> points, SectionLayout layout) {
        if (points.isEmpty()) return List.of();
        int minimumX = points.stream().mapToInt(Point::x).min().orElse(500);
        int maximumX = points.stream().mapToInt(Point::x).max().orElse(500);
        int minimumY = points.stream().mapToInt(Point::y).min().orElse(500);
        int maximumY = points.stream().mapToInt(Point::y).max().orElse(500);
        double sourceWidth = Math.max(1.0D, maximumX - minimumX);
        double sourceHeight = Math.max(1.0D, maximumY - minimumY);
        double scale = Math.min(layout.width() / sourceWidth,
                layout.height() / sourceHeight);
        double sourceCenterX = (minimumX + maximumX) * 0.5D;
        double sourceCenterY = (minimumY + maximumY) * 0.5D;
        double targetCenterX = (layout.minimumX() + layout.maximumX()) * 0.5D;
        double targetCenterY = (layout.minimumY() + layout.maximumY()) * 0.5D;

        List<Point> fitted = new ArrayList<>(points.size());
        for (Point point : points) {
            fitted.add(new Point(
                    (int) Math.round(targetCenterX + (point.x() - sourceCenterX) * scale),
                    (int) Math.round(targetCenterY + (point.y() - sourceCenterY) * scale)));
        }
        return List.copyOf(fitted);
    }

    private static List<Point> orientTopToBottom(List<Point> points) {
        if (points.size() < 2 || points.getFirst().y() <= points.getLast().y()) {
            return points;
        }
        List<Point> reversed = new ArrayList<>(points.size());
        for (int index = points.size() - 1; index >= 0; index--) {
            reversed.add(points.get(index));
        }
        return List.copyOf(reversed);
    }

    /**
     * Samples each segment before clamping it against the actual irregular frame.
     * This also contains curved connector strokes between authored fragments, not
     * only their endpoints.
     */
    private static List<Point> keepInsideOuterFrame(List<Point> points) {
        if (points.isEmpty()) return List.of();
        List<Point> contained = new ArrayList<>();
        for (int segment = 0; segment + 1 < points.size(); segment++) {
            Point first = points.get(segment);
            Point second = points.get(segment + 1);
            int samples = Math.max(1, (int) Math.ceil(distance(first, second) / 10.0D));
            for (int sample = 0; sample < samples; sample++) {
                double progress = sample / (double) samples;
                appendDistinct(contained, containPoint(new Point(
                        (int) Math.round(first.x() + (second.x() - first.x()) * progress),
                        (int) Math.round(first.y() + (second.y() - first.y()) * progress))));
            }
        }
        appendDistinct(contained, containPoint(points.getLast()));
        return List.copyOf(contained);
    }

    private static Point containPoint(Point point) {
        int y = clamp(point.y(), 133, 942);
        FrameBounds bounds = frameBoundsAt(y);
        int left = bounds.minimumX() + FRAME_PADDING;
        int right = bounds.maximumX() - FRAME_PADDING;
        return new Point(clamp(point.x(), left, right), y);
    }

    private static FrameBounds frameBoundsAt(int y) {
        int minimumX = Integer.MAX_VALUE;
        int maximumX = Integer.MIN_VALUE;
        for (int index = 0; index + 1 < OUTER_FRAME_POINTS.size(); index++) {
            Point first = OUTER_FRAME_POINTS.get(index);
            Point second = OUTER_FRAME_POINTS.get(index + 1);
            if (y < Math.min(first.y(), second.y())
                    || y > Math.max(first.y(), second.y())) continue;
            if (first.y() == second.y()) {
                minimumX = Math.min(minimumX, Math.min(first.x(), second.x()));
                maximumX = Math.max(maximumX, Math.max(first.x(), second.x()));
                continue;
            }
            double progress = (y - first.y()) / (double) (second.y() - first.y());
            int x = (int) Math.round(first.x() + (second.x() - first.x()) * progress);
            minimumX = Math.min(minimumX, x);
            maximumX = Math.max(maximumX, x);
        }
        if (minimumX == Integer.MAX_VALUE || maximumX == Integer.MIN_VALUE) {
            return new FrameBounds(285, 715);
        }
        return new FrameBounds(minimumX, maximumX);
    }

    /** Exact one-stroke outer contour traced from the supplied temp.png pixels. */
    private static void addOuterFrame(List<Stroke> strokes) {
        strokes.add(new Stroke(Section.FRAME, OUTER_FRAME_POINTS));
    }

    private static List<Point> buildOuterFramePoints() {
        List<Point> contour = new ArrayList<>(OUTER_FRAME_SOURCE_CONTOUR.length);
        for (int[] source : OUTER_FRAME_SOURCE_CONTOUR) {
            contour.add(framePoint(source[0], source[1]));
        }
        return List.copyOf(contour);
    }

    private static Point framePoint(int sourceX, int sourceY) {
        int x = 285 + Math.round((sourceX - 26) * (430.0F / 22.0F));
        int y = 115 + Math.round((sourceY - 16) * (845.0F / 47.0F));
        return new Point(x, y);
    }

    /**
     * A talisman part is one brush action.  The authored fragments above describe
     * the internal turns of each glyph; this joins those fragments by their nearest
     * endpoints into one continuous guide stroke for each semantic section.
     */
    private static List<Stroke> joinAnatomySections(List<Stroke> fragments) {
        List<Stroke> joined = new ArrayList<>();
        for (Section section : Section.values()) {
            List<Stroke> parts = fragments.stream()
                    .filter(stroke -> stroke.section() == section)
                    .toList();
            if (parts.isEmpty()) continue;
            joined.add(new Stroke(section, joinNearest(parts)));
        }
        return List.copyOf(joined);
    }

    private static List<Point> joinNearest(List<Stroke> parts) {
        boolean[] used = new boolean[parts.size()];
        List<Point> result = new ArrayList<>(parts.getFirst().points());
        used[0] = true;

        for (int completed = 1; completed < parts.size(); completed++) {
            Point current = result.getLast();
            int bestIndex = -1;
            boolean reverse = false;
            double bestDistance = Double.MAX_VALUE;
            for (int index = 0; index < parts.size(); index++) {
                if (used[index] || parts.get(index).points().isEmpty()) continue;
                List<Point> candidate = parts.get(index).points();
                double toStart = distance(current, candidate.getFirst());
                if (toStart < bestDistance) {
                    bestDistance = toStart;
                    bestIndex = index;
                    reverse = false;
                }
                double toEnd = distance(current, candidate.getLast());
                if (toEnd < bestDistance) {
                    bestDistance = toEnd;
                    bestIndex = index;
                    reverse = true;
                }
            }
            if (bestIndex < 0) break;
            used[bestIndex] = true;
            List<Point> candidate = parts.get(bestIndex).points();
            Point destination = reverse ? candidate.getLast() : candidate.getFirst();
            addBrushBridge(result, current, destination);
            if (reverse) {
                for (int point = candidate.size() - 1; point >= 0; point--) {
                    appendDistinct(result, candidate.get(point));
                }
            } else {
                for (Point point : candidate) appendDistinct(result, point);
            }
        }
        return List.copyOf(result);
    }

    private static void addBrushBridge(List<Point> points, Point from, Point to) {
        double length = distance(from, to);
        if (length < 2.0D) return;
        double bend = Math.min(14.0D, length * 0.08D);
        int midpointX = (int) Math.round((from.x() + to.x()) * 0.5D
                - (to.y() - from.y()) / length * bend);
        int midpointY = (int) Math.round((from.y() + to.y()) * 0.5D
                + (to.x() - from.x()) / length * bend);
        appendDistinct(points, new Point(midpointX, midpointY));
    }

    private static void appendDistinct(List<Point> points, Point point) {
        if (points.isEmpty() || !points.getLast().equals(point)) points.add(point);
    }

    private static Stroke stroke(int... coordinates) {
        return stroke(Section.BODY, coordinates);
    }

    private static Stroke stroke(Section section, int... coordinates) {
        List<Point> points = new ArrayList<>(coordinates.length / 2);
        for (int i = 0; i + 1 < coordinates.length; i += 2) {
            points.add(new Point(coordinates[i], coordinates[i + 1]));
        }
        return new Stroke(section, List.copyOf(points));
    }

    public static int score(String recipeId, List<Integer> encodedTrace) {
        Pattern pattern = forRecipe(recipeId);
        List<List<Point>> actual = decode(encodedTrace);
        if (actual.isEmpty()) return 0;
        TalismanRecipes.Def recipe = TalismanRecipes.byId(recipeId);
        double tolerance = tracingTolerance(recipe == null ? 3 : recipe.strengthTier());
        double total = 0.0D;
        int compared = Math.max(pattern.strokes.size(), actual.size());
        for (int i = 0; i < compared; i++) {
            if (i >= pattern.strokes.size() || i >= actual.size()) continue;
            total += strokeScore(pattern.strokes.get(i), actual.get(i), tolerance);
        }
        double countRatio = Math.min(pattern.strokes.size(), actual.size())
                / (double) Math.max(pattern.strokes.size(), actual.size());
        return clamp((int) Math.round(total / compared * countRatio), 0, 100);
    }

    private static double strokeScore(Stroke expected, List<Point> actual,
                                      double tolerance) {
        if (actual.size() < 2 || expected.points.size() < 2) return 0.0D;
        List<Point> ideal = dense(expected.points, 12);
        double coverage = proximity(ideal, actual, tolerance);
        double precision = proximity(actual, ideal, tolerance);
        double start = distance(actual.getFirst(), expected.points.getFirst());
        double end = distance(actual.getLast(), expected.points.getLast());
        double endpointScore = Math.max(0.0D,
                1.0D - (start + end) / (tolerance * 2.5D));
        double order = orderScore(actual, ideal);
        return 100.0D * (coverage * 0.38D + precision * 0.32D
                + endpointScore * 0.18D + order * 0.12D);
    }

    private static double proximity(List<Point> source, List<Point> target,
                                    double tolerance) {
        double total = 0.0D;
        for (Point point : source) {
            double closest = Double.MAX_VALUE;
            for (Point other : target) {
                closest = Math.min(closest, distance(point, other));
            }
            total += Math.max(0.0D, 1.0D - closest / tolerance);
        }
        return total / Math.max(1, source.size());
    }

    private static double tracingTolerance(int strengthTier) {
        return switch (clamp(strengthTier, 1, 6)) {
            case 1 -> 170.0D;
            case 2 -> 150.0D;
            case 3 -> 135.0D;
            case 4 -> 120.0D;
            case 5 -> 105.0D;
            default -> 90.0D;
        };
    }

    private static double orderScore(List<Point> actual, List<Point> ideal) {
        int previous = -1;
        int backwards = 0;
        for (Point point : actual) {
            int nearest = 0;
            double distance = Double.MAX_VALUE;
            for (int i = 0; i < ideal.size(); i++) {
                double candidate = distance(point, ideal.get(i));
                if (candidate < distance) {
                    distance = candidate;
                    nearest = i;
                }
            }
            if (previous >= 0 && nearest + 2 < previous) backwards++;
            previous = Math.max(previous, nearest);
        }
        return Math.max(0.0D, 1.0D - backwards / (double) Math.max(1, actual.size() / 3));
    }

    private static List<Point> dense(List<Point> points, int samplesPerSegment) {
        List<Point> dense = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);
            for (int sample = 0; sample < samplesPerSegment; sample++) {
                double t = sample / (double) samplesPerSegment;
                dense.add(new Point(
                        (int) Math.round(a.x + (b.x - a.x) * t),
                        (int) Math.round(a.y + (b.y - a.y) * t)));
            }
        }
        dense.add(points.getLast());
        return dense;
    }

    private static List<List<Point>> decode(List<Integer> encoded) {
        List<List<Point>> strokes = new ArrayList<>();
        List<Point> current = new ArrayList<>();
        int points = 0;
        for (int i = 0; i + 1 < encoded.size() && points < MAX_TRACE_POINTS; i += 2) {
            int x = encoded.get(i);
            int y = encoded.get(i + 1);
            if (x < 0 || y < 0) {
                if (!current.isEmpty()) {
                    strokes.add(List.copyOf(current));
                    current.clear();
                }
                continue;
            }
            current.add(new Point(clamp(x, 0, 1_000), clamp(y, 0, 1_000)));
            points++;
        }
        if (!current.isEmpty()) strokes.add(List.copyOf(current));
        return List.copyOf(strokes);
    }

    private static double distance(Point first, Point second) {
        return Math.hypot(first.x - second.x, first.y - second.y);
    }

    public enum Section {
        HEAD,
        AUTHORITY,
        BODY,
        CORE,
        COMMAND,
        FOOT,
        FRAME
    }

    public record Anatomy(String headGlyph, String authorityGlyph, String bodyGlyphs,
                          String coreGlyph, String commandGlyphs, String footGlyph) {
        private static final Anatomy EMPTY = new Anatomy("", "", "", "", "", "");
    }

    public record Pattern(List<Stroke> strokes, Anatomy anatomy) {
    }

    public record Stroke(Section section, List<Point> points) {
        public Stroke(List<Point> points) {
            this(Section.BODY, points);
        }
    }

    public record Point(int x, int y) {
    }

    private record SectionLayout(int minimumX, int maximumX,
                                 int minimumY, int maximumY) {
        private int width() {
            return maximumX - minimumX;
        }

        private int height() {
            return maximumY - minimumY;
        }
    }

    private record FrameBounds(int minimumX, int maximumX) {
    }

    private enum HeadStyle {
        CLEAN,
        SPIRIT,
        FIRE,
        THUNDER,
        SUPPRESS
    }

    private enum CoreStyle {
        PURIFY,
        LIGHT,
        SILENCE,
        FIRE,
        BLADE,
        GUARD,
        BIND,
        MOTION,
        MESSAGE,
        SUPPRESS,
        REPAIR,
        SOUL,
        BURST,
        THUNDER,
        TRACK,
        BREAK,
        MOUNTAIN,
        SPIRIT
    }

    private enum FootStyle {
        STAY,
        LAUNCH,
        ATTACH,
        SEAL,
        SCATTER,
        GUIDE
    }
}
