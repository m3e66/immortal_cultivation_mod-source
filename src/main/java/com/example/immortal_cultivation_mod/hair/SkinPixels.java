package com.example.immortal_cultivation_mod.hair;

/** Exact ARGB samples in the standard 64-pixel Minecraft skin layout. */
public record SkinPixels(int width,int height,int[] argb) {
    public SkinPixels {
        if(width!=64 || height!=32 && height!=64 || argb.length!=width*height)throw new IllegalArgumentException("Unsupported skin dimensions");
        argb=argb.clone();
    }
    public int head(int face,boolean overlay,int x,int y) {
        if(face<0||face>4||x<0||x>7||y<0||y>7)throw new IllegalArgumentException("Invalid head pixel");
        int[] u={8,24,16,0,8},v={8,8,8,8,0};
        return argb[(v[face]+y)*width+u[face]+(overlay?32:0)+x];
    }
    public static int fromAbgr(int abgr){return abgr&0xFF00FF00 | (abgr&255)<<16 | (abgr>>>16&255);}
}
