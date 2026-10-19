package com.example.immortal_cultivation_mod.client.qi;

/** Three continuous visual phases, measured in ticks. Does not delay the player's controls. */
public final class QiEruption {
    public static final int GATHER=2, ERUPT=10, READY=20;
    private QiEruption() { }
    public static float amplitude(double age) {
        if(age<0)return 1;
        if(age<GATHER)return (float)(1+1.2*smooth(age/GATHER));
        if(age<ERUPT)return 2.2F;
        if(age<READY)return (float)(2.2-1.2*smooth((age-ERUPT)/(READY-ERUPT)));
        return 1;
    }
    public static float opacity(double age) { return age<0?1:(float)Math.min(1,.2+age/8); }
    /** Short crown around the body; the opening burst expands mostly outward, not skyward. */
    public static float bodyHeight(float standingHeight,int percent,double age) {
        float release=1+(Math.clamp(percent,100,200)-100)*.0005F;
        float burst=Math.clamp((amplitude(age)-1)/1.2F,0,1);
        return Math.max(1.2F,standingHeight*1.02F)*release*(1+burst*.5F);
    }
    public static double smooth(double t) {t=Math.clamp(t,0,1);return t*t*(3-2*t);}
}
