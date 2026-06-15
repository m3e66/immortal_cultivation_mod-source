package com.example.immortal_cultivation_mod.attachment;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class BodyTypes {
    public static final String MORTAL_BODY = "\u51e1\u4f53";
    public static final String METAL_BODY = "\u91d1\u7075\u4f53";
    public static final String WOOD_BODY = "\u6728\u7075\u4f53";
    public static final String WATER_BODY = "\u6c34\u7075\u4f53";
    public static final String FIRE_BODY = "\u706b\u7075\u4f53";
    public static final String EARTH_BODY = "\u571f\u7075\u4f53";
    public static final String THUNDER_BODY = "\u96f7\u7075\u4f53";
    public static final String WIND_BODY = "\u98ce\u7075\u4f53";
    public static final String ICE_BODY = "\u51b0\u7075\u4f53";
    public static final String LIGHT_BODY = "\u5149\u7075\u4f53";
    public static final String DARK_BODY = "\u6697\u7075\u4f53";

    public static final List<String> VALUES = List.of(
            MORTAL_BODY,
            METAL_BODY,
            WOOD_BODY,
            WATER_BODY,
            FIRE_BODY,
            EARTH_BODY,
            THUNDER_BODY,
            WIND_BODY,
            ICE_BODY,
            LIGHT_BODY,
            DARK_BODY
    );

    private BodyTypes() {
    }

    public static String random(RandomSource random) {
        return VALUES.get(random.nextInt(VALUES.size()));
    }

    public static String sanitize(String bodyType) {
        return VALUES.contains(bodyType) ? bodyType : MORTAL_BODY;
    }

    public static String next(String current, int direction) {
        int index = VALUES.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        return VALUES.get(Math.floorMod(index + (direction < 0 ? -1 : 1), VALUES.size()));
    }
}
