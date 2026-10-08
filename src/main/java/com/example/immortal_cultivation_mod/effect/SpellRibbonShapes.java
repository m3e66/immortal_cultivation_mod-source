package com.example.immortal_cultivation_mod.effect;

import java.util.List;
import java.util.function.DoubleFunction;
import net.minecraft.world.phys.Vec3;

/** World-independent authored paths. Sizes are blocks; time is ticks. */
public final class SpellRibbonShapes {
    public static final List<String> SPELLS = List.of("jinmang_chuansha", "jinfeng_zhi", "wanren_chuanxin",
            "pokong_jianhen", "sanjian_zhuiming", "dingshen", "tianyun_fencheng", "chiyan_longjuan",
            "wulei_hongding", "yinlei_jue", "wulei_zhengfa", "muling_shengxi", "xue_guiyuan_shu",
            "shengxi_baofa", "duanliu_kongdun", "ignite_flare", "yanbao_dan", "leishan", "jinhuo_xinliao",
            "tianguang_guansha", "yihen_ci", "shixu_chiliu", "tianfeng_zhui", "suigui_xingyun",
            "yuangang_hujie", "sifang_jie", "yaoguang_shan", "wind_blade", "lingzhi_bullet", "light_beam_attack");
    public interface Sink {
        void segment(Vec3 a, Vec3 b, double width, float alpha);
        default void accent(Vec3 a,Vec3 b,double width,float alpha,int tint){segment(a,b,width,alpha);}
        default void patch(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int tint,float alpha){}
        default void mote(Vec3 center,double radius,int tint,float alpha){}
    }
    private SpellRibbonShapes() {}
    public static int color(String id) {
        return switch (id) {
            case "meditating" -> 0x8ACDFF;
            case "chiyan_longjuan" -> 0xFF573F;
            case "ignite_flare" -> 0x329FFF;
            case "fire_burst" -> 0xFF8B32;
            case "jinhuo_xinliao" -> 0xFF4C28;
            case "tianyun_fencheng", "yanbao_dan" -> 0xFFAD70;
            case "wulei_hongding", "yinlei_jue", "wulei_zhengfa", "leishan", "yihen_ci", "shixu_chiliu" -> 0xD898FF;
            case "wind_blade", "tianfeng_zhui", "duanliu_kongdun", "sifang_jie" -> 0x91E9FF;
            case "muling_shengxi", "shengxi_baofa", "lingzhi_bullet" -> 0x8AFF77;
            case "xue_guiyuan_shu" -> 0xFF7292;
            case "jinmang_chuansha", "jinfeng_zhi", "wanren_chuanxin", "dingshen", "suigui_xingyun" -> 0xFFE19A;
            default -> 0xEFFFF0;
        };
    }
    public static void emit(String id, double age, Sink out, int detail) {
        if(id.equals("meditating")){MeditationVfxShapes.emit(age,out,detail);return;}
        double t=age*.085;
        int n=Math.max(12,detail);
        switch(id) {
            case "jinmang_chuansha" -> {
                lance(out,t,3,4.6,.48,.08,n);
                curve(out,n,.065,u->v(Math.sin(u*7+t)*.16,Math.cos(u*7+t)*.16,1-u*5));
            }
            case "jinfeng_zhi" -> {
                lance(out,-t,2,3.3,.10,.028,n);
                ring(out,n,.075,.40,0,t,2);
            }
            case "wanren_chuanxin" -> {
                for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.5;
                    curve(out,n,.085,u->v(Math.cos(a)*(.2+u*.8),Math.sin(a)*(.2+u*.8),.8-u*3.7));}
                lance(out,t,1,3,.08,.09,n);
            }
            case "pokong_jianhen" -> {
                for(int i=0;i<2;i++){double edge=(i==0?-.30:.30);
                    curve(out,n,.018,u->{double a=(u-.5)*4;return v(Math.sin(a)*1.9,Math.cos(a)*.45+edge,Math.cos(a)*1.1+Math.sin(t)*.08);});}
            }
            case "sanjian_zhuiming" -> {
                for(int i=0;i<2;i++){double a=t+i*Math.PI;
                    curve(out,n,.055,u->v(Math.cos(a+u*4)*.38*(1-u),Math.sin(a+u*4)*.38*(1-u),1.1-u*3.8));}
                ring(out,n,.045,.72,0,-t,2);
            }
            case "dingshen" -> {
                for(int i=0;i<4;i++)ring(out,n,.065,.85-i*.1,.3+i*.45,t+i,0);
                for(int i=0;i<4;i++){double a=i*Math.PI/2;curve(out,n,.06,u->v(Math.cos(a)*.8,2.3*u,Math.sin(a)*.8));}
            }
            case "chiyan_longjuan" -> {
                for(int i=0;i<7;i++){double a=i*Math.PI*2/7;
                    curve(out,n*2,.13,u->{double r=.55+u*2.7,an=a+t*2.2-u*9;return v(Math.cos(an)*r,u*7.8,Math.sin(an)*r);});}
                for(int i=0;i<3;i++){double a=t+i*2.1;curve(out,n,.08,u->v(Math.cos(a+u*2)*u*4,.1+Math.sin(u*Math.PI)*.5,Math.sin(a+u*2)*u*4));}
            }
            case "wulei_hongding" -> {
                for(int i=0;i<5;i++){double a=i*Math.PI*2/5;double since=age-(16+i*11);
                    if(since>=0&&since<5)bolt(out,n,t+i,v(Math.cos(a)*2,0,Math.sin(a)*2),9,.13*(1-since/6));}
                ring(out,n,.055,2.5,.15,-t,0);
            }
            case "yinlei_jue" -> { bolt(out,n,t,Vec3.ZERO,6.5,.13); ring(out,n,.065,1.3,.1,t,0); }
            case "wulei_zhengfa" -> {
                double phase=age%22, strike=Math.max(0,Math.min(1,(phase-3)/2))*Math.max(0,Math.min(1,(20-phase)/5));
                for(int i=0;i<5;i++){
                    double x=i==0?0:((i==1||i==4)?-1.8:1.8),z=i==0?0:(i<=2?-1.8:1.8);
                    if(strike>0)bolt(out,n,t*3+i,v(x,0,z),i==0?10:8,.15*strike);
                    final double px=x,pz=z;
                    curve(out,n,.055,u->v(px*u,.16,pz*u));
                }
                // A stable octagonal seal holds the synchronized five-strike formation.
                for(int i=0;i<8;i++){double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
                    curve(out,n/2,.075,u->v((Math.cos(a)*(1-u)+Math.cos(b)*u)*3.3,.1,(Math.sin(a)*(1-u)+Math.sin(b)*u)*3.3));}
            }
            case "muling_shengxi" -> {
                for(int i=0;i<4;i++){double a=i*Math.PI/2;curve(out,n,.07,u->v(Math.cos(a+t+u*5)*(.8-u*.4),u*2.8,Math.sin(a+t+u*5)*(.8-u*.4)));}
            }
            case "xue_guiyuan_shu" -> {
                for(int i=0;i<5;i++){double a=i*Math.PI*2/5;curve(out,n,.065,u->{double r=(1-u)*1.7+.08;return v(Math.cos(a+u*4-t)*r,.15+u*1.3,Math.sin(a+u*4-t)*r);});}
            }
            case "shengxi_baofa" -> {
                for(int i=0;i<8;i++){double a=i*Math.PI/4;curve(out,n,.10,u->{double r=Math.sin(u*Math.PI)*(1.7+.4*Math.sin(t));return v(Math.cos(a)*r,u*3,Math.sin(a)*r);});}
                ring(out,n,.10,1+(age%24)/12,.1,t,0);
            }
            case "duanliu_kongdun" -> {
                for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.15;curve(out,n,.10,u->{double r=Math.sin(u*Math.PI)*10;return v(Math.cos(a)*r,Math.cos(u*Math.PI)*10,Math.sin(a)*r);});}
                ring(out,n*2,.12,10,0,t,0);
            }
            case "ignite_flare", "fire_burst" -> {
                double force=.55+Math.min(age/8,1)*1.7;
                for(int i=0;i<12;i++){
                    final int flame=i;double a=i*Math.PI/6,seed=i*.79;
                    curve(out,n,.12,u->{double spread=Math.sin(u*Math.PI)*force*(.4+(flame%3)*.22);
                        double curl=a+Math.sin(u*5+t+seed)*.45;
                        return v(Math.cos(curl)*spread,(u*3.1+Math.sin(u*Math.PI)*.4)*(.7+(flame%4)*.13),Math.sin(curl)*spread);});
                }
                // Fast lateral tongues break the silhouette away from a ring or vortex.
                for(int i=0;i<5;i++){double a=i*Math.PI*.4+t*.2;
                    curve(out,n,.08,u->v(Math.cos(a+u*.8)*u*force*1.5,Math.sin(u*Math.PI)*.65,Math.sin(a+u*.8)*u*force*1.5));}
            }
            case "yanbao_dan" -> {
                for(int i=0;i<3;i++)ring(out,n,.10,.7,0,t+i*2.1,i);
                lance(out,t,3,2.4,.6,.065,n);
            }
            case "leishan" -> {
                for(int i=0;i<3;i++){double x=(i-1)*.35;curve(out,n,.10,u->v(x+Math.sin(u*24+t)*.16,Math.sin(u*Math.PI)*.5,1-u*4));}
            }
            case "jinhuo_xinliao" -> {
                for(int i=0;i<9;i++){
                    final int flame=i;double a=i*Math.PI*2/9,seed=i*.7;
                    curve(out,n,.065,u->{double r=.18+Math.sin(u*Math.PI*.82)*(.45+(flame%3)*.23);
                        double lean=a+Math.sin(t*1.5+u*5+seed)*.25;
                        return v(Math.cos(lean)*r,1.0+u*(1.25+(flame%4)*.18),Math.sin(lean)*r);});
                }
                for(int i=0;i<5;i++){double a=i*Math.PI*.4;
                    curve(out,n,.035,u->v(Math.cos(a)*u*.8,1.2+Math.sin(u*8+t)*.1,Math.sin(a)*u*.8));}
            }
            case "tianguang_guansha" -> {
                if(age<20){
                    ring(out,n,.085,2.8,9,t,0);
                    ring(out,n,.07,1.6,.12,-t,0);
                    curve(out,n,.035,u->v(Math.sin(t)*.10,u*9,Math.cos(t)*.10));
                    break;
                }
                for(int i=0;i<5;i++){double a=i*Math.PI*2/5;curve(out,n,.14,u->v(Math.cos(a+t+u*3)*.55,u*24,Math.sin(a+t+u*3)*.55));}
                for(int i=0;i<4;i++)ring(out,n,.10,1.1,(i*6+age*.3)%24,-t,0);
            }
            case "yihen_ci" -> {
                lance(out,t,2,4,.32,.075,n);
                for(int i=0;i<2;i++)ring(out,n,.045,.5+i*.2,0,t+i,2);
            }
            case "shixu_chiliu" -> { for(int i=0;i<3;i++)ring(out,n,.075,1.5+i*.25,1,-t*.25+i,i); }
            case "tianfeng_zhui" -> {
                if(age>=44)break;
                if(age<20){
                    double fall=Math.max(0,(age-12)/8),height=8*(1-fall*fall),radius=2.8-fall*.9;
                    for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.6;
                        curve(out,n,.065,u->{double r=Math.sin(u*Math.PI)*radius;return v(Math.cos(a)*r,height+Math.cos(u*Math.PI)*.65,Math.sin(a)*r);});}
                    ring(out,n,.045,4.7,.08,-t*.4,0);
                    if(age>12)for(int i=0;i<8;i++){double a=i*Math.PI/4;
                        curve(out,n,.045,u->v(Math.cos(a)*radius*(.65+u*.3),height+u*3,Math.sin(a)*radius*(.65+u*.3)));}
                }else{
                    double progress=(age-20)/24,radius=.6+progress*6.5;
                    for(int i=0;i<8;i++){double a=i*Math.PI/4;
                        curve(out,n,.065*(1-progress)+.008,u->{double r=radius*(.35+u*.65),angle=a+u*.8;return v(Math.cos(angle)*r,.12+Math.sin(u*Math.PI)*.7*(1-progress),Math.sin(angle)*r);});}
                    ring(out,n*2,.06*(1-progress)+.008,radius,.12,t*.1,0);
                }
            }
            case "tianyun_fencheng", "suigui_xingyun" -> {
                double s=id.equals("suigui_xingyun")?1.4:2;
                for(int i=0;i<3;i++)ring(out,n,.13,s,0,t+i,i);
                lance(out,t,6,7,s,.115,n);
            }
            case "yuangang_hujie" -> {
                for(int i=0;i<5;i++)ring(out,n,.065,1,0,t*.2+i, i%3);
                // Unit sphere: renderer scales to the server's actual protection radius.
            }
            case "sifang_jie" -> {
                for(int i=0;i<4;i++){double a=i*Math.PI/2;
                    curve(out,n,.095,u->v(Math.cos(a)*5.5+Math.sin(a)*(u-.5)*11,.2,Math.sin(a)*5.5-Math.cos(a)*(u-.5)*11));
                    curve(out,n,.10,u->v(Math.cos(a)*5.5+Math.sin(a)*Math.sin(u*Math.PI)*2,4*u+Math.sin(t+u*4)*.2,Math.sin(a)*5.5-Math.cos(a)*Math.sin(u*Math.PI)*2));}
            }
            case "yaoguang_shan" -> {
                for(int i=0;i<8;i++){double a=i*Math.PI/4+t;curve(out,n,.11,u->v(Math.cos(a)*u*3,Math.sin(a)*u*3,Math.sin(u*Math.PI)*.3));}
                ring(out,n,.085,1.3+Math.sin(t)*.3,0,-t,2);
            }
            case "wind_blade" -> {
                for(int i=0;i<4;i++){double r=1.6-i*.22;curve(out,n,.09-i*.012,u->{double a=(u-.5)*Math.PI*1.3;return v(Math.sin(a)*r,Math.sin(u*Math.PI*2+t)*.13,Math.cos(a)*r);});}
            }
            case "lingzhi_bullet" -> { lance(out,t,2,1.4,.13,.03,n);ring(out,n,.025,.16,0,t,1); }
            case "light_beam_attack" -> {
                double radius=.48+Math.sin(t*1.6)*.015;
                for(int i=0;i<3;i++){double tilt=i*Math.PI/3+t*.3;
                    curve(out,n*2,.016,u->{double a=u*Math.PI*2;return v(Math.cos(a)*radius,Math.sin(a)*Math.cos(tilt)*radius,Math.sin(a)*Math.sin(tilt)*radius);});}
            }
            default -> { }
        }
        SpellRibbonDetails.emit(id,age,out);
    }
    private static void lance(Sink s,double t,int strands,double length,double radius,double width,int n){
        for(int i=0;i<strands;i++){double a=i*Math.PI*2/strands;
            curve(s,n,width,u->{double r=Math.sin(u*Math.PI)*radius;return v(Math.cos(a+t+u*6)*r,Math.sin(a+t+u*6)*r,1-u*length);});}
    }
    private static void bolt(Sink s,int n,double t,Vec3 base,double height,double width){
        curve(s,n,width,u->base.add(Math.sin(u*39+Math.floor(t*4))*.26*Math.sin(u*Math.PI),u*height,Math.cos(u*31+t)*.23*Math.sin(u*Math.PI)));
    }
    private static void ring(Sink s,int n,double width,double radius,double height,double phase,int axis){
        curve(s,n,width,u->{double a=phase+u*Math.PI*1.82,x=Math.cos(a)*radius,z=Math.sin(a)*radius;
            return axis==0?v(x,height,z):axis==1?v(x,z+height,0):v(0,x+height,z);});
    }
    private static void curve(Sink s,int n,double width,DoubleFunction<Vec3> f){
        Vec3 prev=f.apply(0);
        for(int i=1;i<=n;i++){double u=(double)i/n;Vec3 next=f.apply(u);
            s.segment(prev,next,width*(.5+.5*Math.sin(u*Math.PI)),(float)(.25+.75*Math.sin(u*Math.PI)));prev=next;}
    }
    private static Vec3 v(double x,double y,double z){return new Vec3(x,y,z);}
}
