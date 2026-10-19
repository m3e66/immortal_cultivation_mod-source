package com.example.immortal_cultivation_mod.flame;

import net.minecraft.core.BlockPos;

/** Temporary scorch colors only: no replacement blocks, fluids, loot, or saved-world data. */
public final class FlameAftermathStyle {
    public static final int DURATION = 600;

    public static int color(FlameKind kind, BlockPos pos, long seed) {
        int variant = (int)Math.floorMod(FlameCraterShape.mix(seed ^ region(pos)), 3);
        int[] palette = switch (kind) {
            case CRIMSON -> new int[]{0x62281C, 0xA64021, 0xD7752E};
            case EARTH -> new int[]{0x54372B, 0x91603A, 0xBD7735};
            case WOOD -> new int[]{0x254C38, 0x36765B, 0x779E69};
            case SOLAR -> new int[]{0xA38545, 0xD1AB56, 0xEFE0A4};
            case NETHER -> new int[]{0x281C3F, 0x46305F, 0x6F5487};
            case THUNDER -> new int[]{0x303349, 0x526182, 0x7892BD};
            case DRAGON -> new int[]{0x782727, 0xB64629, 0xD09C44};
            case HEAVENLY -> new int[]{0x655083, 0x9F79BB, 0xD0BADD};
            default -> new int[]{0x302728, 0x51403B, 0x75513B};
        };
        return palette[variant];
    }

    public static float opacity(float age, BlockPos pos, long seed) {
        if (!Float.isFinite(age) || age <= 8 || age >= DURATION) return 0;
        float appear = Math.clamp((age - 8) / 12, 0, 1);
        float fade = Math.clamp((DURATION - age) / 200, 0, 1);
        fade *= fade * (3 - 2 * fade);
        float variation = .38F + (FlameCraterShape.mix(seed ^ region(pos)) >>> 40) * (.26F / 0x1.0p24F);
        return variation * appear * fade;
    }

    /** Neighboring blocks share a color patch instead of looking like unrelated speckles. */
    private static long region(BlockPos pos) {
        int size = FlameAftermathMesh.TILE_SIZE;
        return BlockPos.asLong(Math.floorDiv(pos.getX(), size), Math.floorDiv(pos.getY(), size),
                Math.floorDiv(pos.getZ(), size));
    }

    private FlameAftermathStyle() {}
}
