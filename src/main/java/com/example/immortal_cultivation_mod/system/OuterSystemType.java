package com.example.immortal_cultivation_mod.system;

import java.util.Locale;
import net.minecraft.util.RandomSource;

public enum OuterSystemType {
    KILL_GROWTH("kill_growth"),
    DAILY_SIGN_IN("daily_sign_in"),
    SYSTEM_SHOP("system_shop"),
    DIVINE_GACHA("divine_gacha"),
    LIFE_SIMULATION("life_simulation");

    private static final OuterSystemType[] VALUES = values();
    private final String id;

    OuterSystemType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "system.immortal_cultivation_mod." + id;
    }

    public static OuterSystemType random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public static OuterSystemType byId(String id) {
        if (id != null) {
            for (OuterSystemType type : VALUES) {
                if (type.id.equals(id.toLowerCase(Locale.ROOT))) {
                    return type;
                }
            }
        }
        return KILL_GROWTH;
    }
}
