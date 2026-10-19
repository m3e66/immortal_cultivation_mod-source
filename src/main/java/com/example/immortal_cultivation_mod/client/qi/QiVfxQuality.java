package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.config.ModClientConfig;
import net.minecraft.client.Minecraft;

/** Visual-only budgets. These never change server combat or domain behavior. */
public final class QiVfxQuality {
    private static int forms,coatings;
    private QiVfxQuality() { }
    public static int level() { return ModClientConfig.BLAST_QI_QUALITY.get(); }
    public static int actors() { return switch(level()){case 0->4;case 1->8;default->12;}; }
    public static int particleBudget() {
        int limit=ModClientConfig.BLAST_QI_PARTICLE_BUDGET.get();
        return Math.min(limit,switch(Minecraft.getInstance().options.particles().get()){case ALL->64;case DECREASED->24;case MINIMAL->8;});
    }
    public static void beginFrame() { forms=0;coatings=0; }
    public static boolean takeCoating(int quads) { coatings+=quads;return coatings<=(level()==0?4000:level()==1?8000:16000); }
    public static boolean takeForm() { return forms++<(level()==0?32:level()==1?64:96); }
    public static float opacity() { return ModClientConfig.BLAST_QI_OPACITY.get().floatValue(); }
}
