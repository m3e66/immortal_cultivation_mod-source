package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.phys.Vec3;

/** Calm, seated qi gathering: low petals, inward streams and a slow breathing pulse. */
public final class MeditationVfxShapes {
    private static final int BLUE=0x8ACDFF, PALE=0xD8F6FF;
    private MeditationVfxShapes() {}
    public static void emit(double age,SpellRibbonShapes.Sink sink,int detail) {
        int n=Math.max(12,Math.min(28,detail));
        double time=age/20,breath=.5-.5*Math.cos(age*Math.PI/60);
        for(int petal=0;petal<8;petal++){
            double angle=petal*Math.PI/4+time*.025;
            Vec3 previous=petalPoint(0,angle,breath);
            for(int i=1;i<=n;i++){
                double u=(double)i/n;Vec3 next=petalPoint(u,angle,breath);
                sink.accent(previous,next,.018,(float)(.22+.12*breath),BLUE);previous=next;
            }
        }
        for(int stream=0;stream<4;stream++){
            double phase=stream*Math.PI/2+time*.12,head=(age/90+stream*.25)%1;
            Vec3 previous=gatherPoint(Math.max(0,head-.24),phase);
            for(int i=1;i<=n;i++){
                double u=(double)i/n,progress=Math.max(0,head-.24)+Math.min(head,.24)*u;
                Vec3 next=gatherPoint(progress,phase);
                float alpha=(float)(Math.sin(u*Math.PI)*Math.sin(head*Math.PI)*.55);
                sink.accent(previous,next,.028,alpha,BLUE);previous=next;
            }
            sink.mote(gatherPoint(head,phase),.028,PALE,(float)Math.sin(head*Math.PI)*.5F);
        }
        for(int i=0;i<20;i++){
            double progress=(age/110+i/20.0)%1,angle=i*2.39996;
            sink.mote(gatherPoint(progress,angle),.013+(i%3)*.004,BLUE,(float)Math.sin(progress*Math.PI)*.45F);
        }
        sink.mote(new Vec3(0,.63,.06),.08,PALE,(float)(.12+.12*breath));
    }
    private static Vec3 petalPoint(double u,double angle,double breath){
        double radius=Math.sin(u*Math.PI)*(1.22+breath*.12),a=angle+Math.sin(u*Math.PI*2)*.27;
        return new Vec3(Math.cos(a)*radius,.055+Math.sin(u*Math.PI)*.10,Math.sin(a)*radius);
    }
    private static Vec3 gatherPoint(double u,double angle){
        double radius=.14+2.3*(1-u)*(1-u),a=angle+u*2.7;
        return new Vec3(Math.cos(a)*radius,.20+u*.43+Math.sin(u*Math.PI)*.45,Math.sin(a)*radius);
    }
}
