package com.example.immortal_cultivation_mod.client.model;

/** A continuous heavy cloak draped around the shoulders, open at the front. */
public final class FlowingCape {
    private FlowingCape() {}
    private static float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}

    public record Point(float x,float y,float z) {}

    public static Point sample(float x,float y,float amount,float turn,float time) {
        float t=clamp((23-y)/22,0,1),u=clamp(x/7,-1,1);
        amount=clamp(amount,0,1);
        float loose=t*t*(3-2*t);
        float angle=u*2.88F;
        float rear=(float)((Math.cos(angle)+1)*.5);
        float fold=(float)Math.sin(u*Math.PI*3+t*1.8-time*.13F);
        float shoulderDrop=clamp((23-y)/3,0,1);
        float shoulderFlare=shoulderDrop*shoulderDrop*(3-2*shoulderDrop);
        float upper=clamp((23-y)/8,0,1);
        float shoulderWidth=1.2F*(1-upper*upper*(3-2*upper));
        float width=8.3F+1.9F*t+1.4F*shoulderFlare+shoulderWidth;
        float pleat=.18F*loose*(float)Math.cos(u*Math.PI*6);
        // Broad, slow folds travel through one connected sheet; the shoulder seam stays fixed.
        float sway=(float)Math.sin(time*.18F-t*1.7F)*2.0F*amount*loose;
        float side=(float)Math.sin(angle)*(width+pleat)+turn*7*loose+sway;
        float height=23-22*t*(1-.60F*amount*rear)
                +fold*1.85F*amount*loose;
        float depth=(float)Math.cos(angle)*(4.4F+.9F*t+pleat)
                +amount*t*(1.98F+14.02F*rear);
        return new Point(side,height,depth);
    }

    /** Frame-rate independent response; no arm collisions or panel springs. */
    public static final class Motion {
        private double previous;
        private float amount,turn;
        public Motion(double time){previous=time;}
        public void advance(double time,float speed,float yawDelta) {
            double dt=Math.max(0,Math.min(.1,(time-previous)/20));previous=time;
            float blend=(float)(1-Math.exp(-dt*5));
            amount+=(clamp(speed/.24F,0,1)-amount)*blend;
            turn+=(clamp(yawDelta*.012F,-.3F,.3F)-turn)*blend;
        }
        public float amount(){return amount;}
        public float turn(){return turn;}
    }
}
