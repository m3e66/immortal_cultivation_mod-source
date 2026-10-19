package com.example.immortal_cultivation_mod.client.qi;

import net.minecraft.world.phys.Vec3;

/** Closed, rising flame volume with broad travelling lobes and an uneven crown. */
public final class QiEnergyShape {
    private QiEnergyShape() { }
    public static Vec3 point(double angle,double t,double time,float surge,Vec3 drag,int art) {
        if(art<0||art>16)return point(angle,t,time,surge,drag);
        if(art==5)return reincarnation(angle,t,time,surge,drag);
        Vec3 p=point(angle,t,time,surge,Vec3.ZERO);
        double wave=Math.sin(t*Math.PI),scale=1;
        switch(art) {
            case 0 -> scale=.92+.035*Math.sin(t*9-time*.06);
            case 1 -> scale=.94+.025*Math.sin(angle*2+t*5-time*.035)*wave;
            case 2,3,4 -> {
                double shoulders=Math.exp(-Math.pow((t-.64)/.22,2))*wave;
                scale=1+shoulders*(.08+.22*Math.pow(.5+.5*Math.cos(angle*5+time*.025),5));
                p=p.add(0,shoulders*wave*.07*Math.pow(.5+.5*Math.cos(angle*5+time*.025),5),0);
            }
            // Match Evergreen's flame volume exactly. Devouring's inward motion
            // belongs in its shader, without twisting mesh rows into stacked discs.
            case 6 -> { }
            case 7 -> scale=.93+.055*Math.sin(angle*3+t*7-time*.12)*wave;
            case 9 -> scale=1+.10*Math.sin(angle*4+t*9-time*.16)*wave;
            case 10 -> scale=1+.09*Math.pow(.5+.5*Math.cos(angle*6+t*13),7)*wave;
            case 11 -> scale=1+.14*Math.pow(.5+.5*Math.cos(t*17),5)*wave;
            case 12 -> scale=.95+.06*Math.sin(angle*3+t*6-time*.04)*wave;
            case 13 -> scale=1.08+.045*Math.sin(angle*4+t*8-time*.12)*wave;
            case 14 -> scale=1+.08*Math.sin(angle*7+t*17-Math.floor(time/3)*2.1)*wave;
            case 15 -> scale=1.12+.12*Math.sin(t*11-time*.025)*wave;
            case 16 -> scale=1+.08*Math.sin(angle*2+t*7-time*.035)*wave;
        }
        // Preserve the original rearward drag regardless of each art's local swirl.
        Vec3 styled=new Vec3(p.x*scale,p.y,p.z*scale);
        return styled.add(-drag.x*t*t,0,-drag.z*t*t);
    }
    /** One smooth flame volume. Opposed currents travel over it instead of twisting mesh rows. */
    private static Vec3 reincarnation(double angle,double t,double time,float surge,Vec3 drag) {
        double phase=(time%240)*Math.PI/120;
        double envelope=Math.sqrt(smooth(0,.075,t))*Math.pow(1-smooth(.68,1,t),.56);
        double lobe=.055*Math.cos(angle*2+phase-t*3)+.035*Math.sin(angle*3-phase+t*4);
        double radius=envelope*(.83+.08*Math.sin(t*Math.PI)+lobe)*(1+surge*.12);
        double turn=angle+.065*Math.sin(phase+t*3),crown=t*t;
        double y=t+.23*Math.pow(t,5)*Math.pow(Math.max(0,1-t),.4)*(.5+.5*Math.cos(angle*2-phase));
        return new Vec3(Math.cos(turn)*radius+.045*crown*Math.sin(phase+t*2)-drag.x*crown,y,
                Math.sin(turn)*radius+.045*crown*Math.cos(phase+t*2)-drag.z*crown);
    }
    public static Vec3 point(double angle,double t,double time,float surge,Vec3 drag) {
        double envelope=Math.sqrt(smooth(0,.08,t))*Math.pow(1-smooth(.72,1,t),.45);
        // Most of the reference is a broad column. Small angular eddies travel
        // upward independently, rather than making horizontal rings around it.
        double lobe=.09*Math.sin(angle*4+t*11-time*.14)+.06*Math.sin(angle*7+t*22-time*.25);
        double body=.86+.08*Math.sin(t*Math.PI);
        double asymmetric=1+.07*Math.sin(angle*3+t*6)+.05*Math.cos(angle*2-t*5+time*.07);
        double radius=envelope*(body+lobe)*asymmetric*(1+surge*.15);
        double twist=angle+.10*Math.sin(t*7-time*.08);
        double crown=t*t;
        double x=Math.cos(twist)*radius+.12*crown*Math.sin(time*.045+t*5);
        double z=Math.sin(twist)*radius+.10*crown*Math.sin(time*.059+t*4+1.7);
        // Travelling shoulders break into several broad tongues near the crown.
        // Their displacement vanishes at the poles, keeping the entire surface closed.
        double tongues=.55+.45*Math.sin(angle*3+time*.08)+.20*Math.sin(angle*5-time*.11);
        double y=t+.75*Math.pow(t,5)*Math.pow(Math.max(0,1-t),.32)*tongues;
        double lag=t*t;
        return new Vec3(x-drag.x*lag,y,z-drag.z*lag);
    }
    private static double smooth(double a,double b,double x){double t=Math.clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
}
