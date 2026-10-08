package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.phys.Vec3;
import com.example.immortal_cultivation_mod.effect.SpellRibbonShapes.Sink;

/** Blades, seals, leaves, rifts, clocks and translucent shields from the user's spell specifications. */
public final class SpellRibbonDetails {
    private static final int GOLD=0xFFE19A,SILVER=0xEFFFF0,FIRE=0xFF703C,GREEN=0x8AFF77,CYAN=0x91E9FF,VIOLET=0xD898FF;
    private SpellRibbonDetails(){}
    public static void emit(String id,double age,Sink s){
        double t=age*.075;
        switch(id){
            case "jinmang_chuansha" -> {
                for(int i=0;i<3;i++)ring(s,new Vec3(0,0,-i*.8),.28+i*.12,t+i,2,GOLD,.035);
                sparks(s,t,14,.7,2.5,GOLD);
            }
            case "jinfeng_zhi" -> {
                blade(s,new Vec3(0,0,.15),.07,.85,GOLD);
                for(int i=0;i<5;i++){double a=i*1.256+t;Vec3 p=new Vec3(Math.cos(a)*.2,Math.sin(a)*.2,-(age*.12+i*.4)%2);
                    s.accent(p,p.add(0,0,-.35),.018,.7F,GOLD);}
            }
            case "wanren_chuanxin" -> {
                for(int i=0;i<28;i++){
                    double a=i*2.399963,progress=(age*.035+i/28.0)%1,r=2.5*(1-progress)+.08;
                    Vec3 p=new Vec3(Math.cos(a)*r,Math.sin(a)*r,1-progress*1.6);
                    blade(s,p,.045,.32,GOLD);
                    s.accent(p,p.add(Math.cos(a)*.35,Math.sin(a)*.35,-.18),.025,(float)(.3+progress*.7),GOLD);
                }
            }
            case "pokong_jianhen" -> {
                for(int i=0;i<24;i++){
                    double a=(i/24.0-.5)*4,b=((i+1)/24.0-.5)*4;
                    Vec3 p=scar(a,t),q=scar(b,t);Vec3 d=new Vec3(0,.27*Math.sin((i+.5)*Math.PI/24),0);
                    s.patch(p.add(d),q.add(d),q.subtract(d),p.subtract(d),0x05020F,.94F);
                    s.accent(p.add(d),q.add(d),.024,.85F,SILVER);
                    s.accent(p.subtract(d),q.subtract(d),.018,.55F,VIOLET);
                }
            }
            case "sanjian_zhuiming" -> {blade(s,new Vec3(0,0,.1),.12,1.3,SILVER);ring(s,Vec3.ZERO,.65,-t,2,GOLD,.025);}
            case "dingshen" -> {
                for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.15;Vec3 p=new Vec3(Math.cos(a)*.8,.45+(i%3)*.5,Math.sin(a)*.8);rune(s,p,.16,GOLD);}
                for(int i=0;i<3;i++)ring(s,new Vec3(0,.35+i*.65,0),.78,t+i,0,GOLD,.04);
            }
            case "tianyun_fencheng" -> {sparks(s,t,32,2,5,FIRE);shards(s,t,10,1.5,0x492B20);}
            case "chiyan_longjuan" -> {
                for(int i=0;i<30;i++){double u=(i/30.0+age*.011)%1,a=i*2.4+t*2,r=.5+u*3;
                    s.mote(new Vec3(Math.cos(a)*r,u*8,Math.sin(a)*r),.025+.035*u,FIRE,.8F);}
                // Uneven semi-transparent flame sheets make the column shimmer as it turns.
                for(int i=0;i<8;i++){double a=i*Math.PI/4+t;Vec3 p=new Vec3(Math.cos(a)*1.4,2,Math.sin(a)*1.4),q=new Vec3(Math.cos(a+.65)*2.8,6.5,Math.sin(a+.65)*2.8);
                    s.patch(p,p.add(.25,0,.1),q.add(.2,0,.1),q,0xFF341B,.07F);}
            }
            case "wulei_hongding", "yinlei_jue" -> {
                int count=id.equals("yinlei_jue")?1:5;
                for(int i=0;i<count;i++){double a=i*Math.PI*2/count,r=count==1?0:2.8;Vec3 base=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r);
                    if(id.equals("wulei_hongding")&&(age<16+i*11||age>=21+i*11))continue;
                    for(int k=0;k<3;k++){Vec3 p=base.add(Math.sin(k+t)*.2,2+k*1.5,0);Vec3 q=p.add(Math.sin(i+k+1)*1.1,-.8,Math.cos(i+k)*.5);
                        s.accent(p,p.lerp(q,.5).add(.15,.1,0),.035,.8F,VIOLET);s.accent(p.lerp(q,.5).add(.15,.1,0),q,.022,.65F,SILVER);}
                    rune(s,base.add(0,.15,0),.3,GOLD);
                }
                if(id.equals("yinlei_jue"))ring(s,new Vec3(0,2.6,0),1.3,t,0,VIOLET,.075);
            }
            case "wulei_zhengfa" -> {
                ring(s,new Vec3(0,.12,0),3.05,0,0,GOLD,.045);
                ring(s,new Vec3(0,.13,0),2.45,0,0,VIOLET,.035);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;Vec3 radial=new Vec3(Math.cos(a),0,Math.sin(a)),side=new Vec3(-Math.sin(a),0,Math.cos(a));
                    for(int bar=0;bar<3;bar++){
                        Vec3 c=radial.scale(2.65+bar*.15).add(0,.16,0);
                        if((i&(1<<bar))==0)s.accent(c.subtract(side.scale(.22)),c.add(side.scale(.22)),.024,.8F,GOLD);
                        else {s.accent(c.subtract(side.scale(.22)),c.subtract(side.scale(.07)),.024,.8F,GOLD);s.accent(c.add(side.scale(.07)),c.add(side.scale(.22)),.024,.8F,GOLD);}
                    }
                }
                for(int i=0;i<5;i++){
                    double x=i==0?0:((i==1||i==4)?-1.8:1.8),z=i==0?0:(i<=2?-1.8:1.8);Vec3 base=new Vec3(x,0,z);
                    rune(s,base.add(0,.7,0),.22,GOLD);
                    double phase=age%22;if(phase>3&&phase<20)for(int j=0;j<3;j++){
                        Vec3 p=base.add(0,2+j*1.5,0),q=p.add(Math.sin(i+j+t)*1.25,-.65,Math.cos(i+j+t)*.75);
                        Vec3 mid=p.lerp(q,.5).add(.2,.15,0);
                        s.accent(p,mid,.055,.9F,VIOLET);s.accent(mid,q,.032,.65F,SILVER);
                    }
                }
            }
            case "muling_shengxi" -> {
                for(int i=0;i<12;i++){double u=(i/12.0+age*.009)%1,a=t+i*2.4;Vec3 p=new Vec3(Math.cos(a)*(.7+u*.3),u*2.8,Math.sin(a)*(.7+u*.3));
                    s.patch(p.add(-.13,0,0),p.add(0,.24,.05),p.add(.13,0,0),p.add(0,-.05,0),0x50CE68,.7F);
                    s.accent(p,p.add(0,.20,.05),.015,.8F,GREEN);}
                sparks(s,t,14,1.3,1,GREEN);
            }
            case "xue_guiyuan_shu" -> {s.mote(new Vec3(0,1.25,0),.18,0xFF4264,.8F);ring(s,new Vec3(0,1.2,0),.45,-t,1,0xFF4264,.045);}
            case "shengxi_baofa" -> {
                for(int i=0;i<3;i++)ring(s,new Vec3(0,.15+i*.22,0),.7+((age+i*8)%30)*.14,t,0,i%2==0?GREEN:GOLD,.095);
                sparks(s,t,24,2.5,1.6,GOLD);
            }
            case "duanliu_kongdun" -> {
                shell(s,10,0x677AAF,.035F,8,4);
                for(int i=0;i<7;i++){double a=i*.9+t*.1;Vec3 p=new Vec3(Math.cos(a)*9,2.7,Math.sin(a)*9);
                    s.accent(p,p.add(Math.sin(a),1,-Math.cos(a)),.095,.75F,SILVER);}
                ring(s,new Vec3(0,.2,0),9.9,-t*.25,0,CYAN,.10);
            }
            case "ignite_flare", "fire_burst" -> {
                boolean blueFire=id.equals("ignite_flare");
                double force=.6+Math.min(age/8,1)*1.6;
                for(int i=0;i<14;i++){
                    double a=i*Math.PI/7+t*.12,lean=Math.sin(t*1.8+i)*.35;
                    Vec3 p=new Vec3(Math.cos(a)*.12,.12,Math.sin(a)*.12);
                    Vec3 left=new Vec3(Math.cos(a-.3)*force*.65,1.0,Math.sin(a-.3)*force*.65);
                    Vec3 tip=new Vec3(Math.cos(a+lean)*force*.5,2.1+(i%4)*.25,Math.sin(a+lean)*force*.5);
                    Vec3 right=new Vec3(Math.cos(a+.3)*force*.65,1.0,Math.sin(a+.3)*force*.65);
                    s.patch(p,left,tip,right,i%2==0?(blueFire?0x185CFF:0xFF5418):(blueFire?0x32C5FF:0xFFAA32),.30F);
                    s.accent(p,tip,.035,.45F,blueFire?0xA8E8FF:GOLD);
                }
                sparks(s,t*1.7,34,force*1.2,3.6,blueFire?0x4DAAFF:FIRE);
                s.mote(new Vec3(0,.35,0),.23,blueFire?0xD8F6FF:0xFFE8A8,.8F);
            }
            case "yanbao_dan" -> {shell(s,.42,0xFE5420,.20F,8,4);s.mote(Vec3.ZERO,.19,0xFFF3C4,1);sparks(s,t,8,.7,.8,FIRE);}
            case "leishan" -> {
                Vec3 head=new Vec3(0,.65,0),hips=new Vec3(0,-.3,0);
                ring(s,head,.18,t,2,CYAN,.035);s.accent(head.add(0,-.2,0),hips,.07,.55F,CYAN);
                for(int side:new int[]{-1,1}){s.accent(new Vec3(0,.3,0),new Vec3(side*.45,-.1,-.2),.04,.5F,CYAN);s.accent(hips,new Vec3(side*.24,-1,-.2),.05,.45F,CYAN);}
            }
            case "jinhuo_xinliao" -> {
                // A visibly dark chest core, with red fissures and smoke-edged tongues.
                Vec3 core=new Vec3(0,1.15,0);double pulse=.28+Math.sin(t*2.5)*.025;
                for(int i=0;i<12;i++){
                    double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
                    Vec3 p=core.add(Math.cos(a)*pulse,Math.sin(a)*pulse,-.20),q=core.add(Math.cos(b)*pulse,Math.sin(b)*pulse,-.20);
                    s.patch(core.add(0,0,-.25),p,q,core.add(0,0,-.25),0x160909,.94F);
                    if(i%2==0)s.accent(p,q,.018,.65F,0xFF4520);
                }
                for(int i=0;i<9;i++){
                    double a=i*Math.PI*2/9,lean=Math.sin(t+i)*.4;
                    Vec3 base=core.add(Math.cos(a)*.22,0,Math.sin(a)*.22);
                    Vec3 edge=core.add(Math.cos(a-.3)*.8,.5,Math.sin(a-.3)*.8);
                    Vec3 tip=core.add(Math.cos(a+lean)*.7,1.3+(i%3)*.2,Math.sin(a+lean)*.7);
                    Vec3 other=core.add(Math.cos(a+.3)*.8,.5,Math.sin(a+.3)*.8);
                    s.patch(base,edge,tip,other,0x29101A,.72F);s.accent(edge,tip,.027,.62F,0xFF632B);
                }
                for(int i=0;i<22;i++){
                    double u=(i/22.0+t*.12)%1,a=i*2.4+t*.2;Vec3 p=core.add(Math.cos(a)*u*1.4,u*2,Math.sin(a)*u*1.4);
                    if(i%3==0)s.mote(p,.025,0xFF6B27,(float)(1-u));
                    else s.patch(p,p.add(.06,.02,0),p.add(.08,.07,.02),p.add(.01,.05,.02),0x504443,(float)((1-u)*.6));
                }
            }
            case "tianguang_guansha" -> {if(age>=20){s.accent(Vec3.ZERO,new Vec3(0,16,0),.23,1,SILVER);sparks(s,t,20,1.4,8,GOLD);}}
            case "yihen_ci" -> {blade(s,new Vec3(0,0,.2),.16,1.5,VIOLET);for(int i=0;i<3;i++)ring(s,new Vec3(0,0,-i*.7),.3+i*.2,-t+i,2,VIOLET,.025);}
            case "shixu_chiliu" -> {
                for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 p=new Vec3(Math.cos(a)*1.6,1+Math.sin(a)*1.6,0);s.accent(p,p.scale(.88).add(0,.12,0),.032,.8F,GOLD);}
                Vec3 c=new Vec3(0,1,0);s.accent(c,c.add(Math.cos(t*.12)*1.1,Math.sin(t*.12)*1.1,0),.05,.8F,SILVER);
                s.accent(c,c.add(Math.cos(t*.5)*.75,Math.sin(t*.5)*.75,0),.035,.65F,VIOLET);
                sparks(s,t*.08,16,1.8,1.5,VIOLET);
            }
            case "tianfeng_zhui" -> {
                if(age>=44)break;
                if(age<20){
                    double fall=Math.max(0,(age-12)/8),height=8*(1-fall*fall),radius=2.8-fall*.9;
                    for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
                        s.patch(new Vec3(0,height-.45,0),new Vec3(Math.cos(a)*radius,height,Math.sin(a)*radius),new Vec3(0,height+.65,0),new Vec3(Math.cos(b)*radius,height,Math.sin(b)*radius),CYAN,.10F);}
                }else{
                    double progress=(age-20)/24;
                    for(int i=0;i<24;i++){double a=i*2.4,r=.8+progress*(3+i%4);
                        Vec3 p=new Vec3(Math.cos(a)*r,.15+(1-progress)*(i%3)*.18,Math.sin(a)*r);
                        s.accent(p,p.add(Math.cos(a)*.65,.05,Math.sin(a)*.65),.025,(float)(1-progress)*.6F,SILVER);}
                }
            }
            case "light_beam_attack" -> {
                // A closed, shaded sphere gives the light a round volume from every angle.
                double radius=.46+Math.sin(age*.136)*.015;
                for(int row=0;row<10;row++)for(int i=0;i<20;i++){
                    double a=i*Math.PI/10,b=(i+1)*Math.PI/10,p=row*Math.PI/10,q=(row+1)*Math.PI/10;
                    Vec3 normal=sphere((a+b)/2,(p+q)/2,1);
                    double shade=.55+.45*Math.max(0,normal.dot(new Vec3(-.4,.7,.5).normalize()));
                    int color=((int)(255*shade)<<16)|((int)(238*shade)<<8)|(int)(160*shade);
                    s.patch(sphere(a,p,radius),sphere(b,p,radius),sphere(b,q,radius),sphere(a,q,radius),color,.9F);
                }
                s.mote(Vec3.ZERO,.15,GOLD,.4F);
            }
            case "suigui_xingyun" -> {shards(s,t,18,2.2,GOLD);sparks(s,t,28,2.5,4,SILVER);}
            case "yuangang_hujie" -> {shell(s,1,0xC2EDC9,.05F,12,5);for(int i=0;i<3;i++)ring(s,new Vec3(0,.1*i,0),1-i*.05,t+i,0,SILVER,.025);}
            case "sifang_jie" -> {
                for(int i=0;i<4;i++){
                    double a=i*Math.PI/2;Vec3 p=new Vec3(Math.cos(a)*5.5-Math.sin(a)*5.5,0,Math.sin(a)*5.5+Math.cos(a)*5.5);
                    Vec3 q=new Vec3(Math.cos(a)*5.5+Math.sin(a)*5.5,0,Math.sin(a)*5.5-Math.cos(a)*5.5);
                    s.patch(p,q,q.add(0,4,0),p.add(0,4,0),0x7ADEF4,.075F);
                    s.accent(p,p.add(0,4,0),.10,.9F,CYAN);s.accent(p.add(0,4,0),q.add(0,4,0),.07,.75F,CYAN);
                    double y=(age*.06)%4;s.accent(p.add(0,y,0),q.add(0,y,0),.045,.5F,CYAN);
                }
            }
            case "yaoguang_shan" -> {s.mote(Vec3.ZERO,.55,SILVER,1);sparks(s,t,32,3,1.6,GOLD);}
            case "wind_blade" -> {
                for(int i=0;i<20;i++){double a=(i/20.0-.5)*4,b=((i+1)/20.0-.5)*4;
                    Vec3 p=new Vec3(Math.sin(a)*1.6,0,Math.cos(a)*1.6),q=new Vec3(Math.sin(b)*1.6,0,Math.cos(b)*1.6);
                    s.patch(p,q,q.scale(.82),p.scale(.82),0x9BE7DE,.14F);}
                sparks(s,t,9,1,.5,CYAN);
            }
            case "lingzhi_bullet" -> {s.mote(Vec3.ZERO,.16,GREEN,1);ring(s,Vec3.ZERO,.18,t,2,SILVER,.025);}
            default -> {}
        }
    }
    private static Vec3 scar(double a,double t){return new Vec3(Math.sin(a)*1.9,Math.cos(a)*.45,Math.cos(a)*1.1+Math.sin(t)*.08);}
    private static void blade(Sink s,Vec3 p,double width,double length,int color){
        Vec3 a=p.add(0,0,length*.5),b=p.add(width,0,-length*.25),c=p.add(0,0,-length*.5),d=p.add(-width,0,-length*.25);
        s.patch(a,b,c,d,color,.55F);s.accent(a,c,.035,.9F,color);
    }
    private static void rune(Sink s,Vec3 p,double size,int color){
        Vec3 top=p.add(0,size*1.7,0),bottom=p.add(0,-size,0);s.accent(top,bottom,.022,.8F,color);
        for(int j=0;j<3;j++){double y=j*size*.6;s.accent(p.add(-size,y,0),p.add(size,y,0),.02,.75F,color);}
        s.accent(top,p.add(size*.65,size,0),.02,.7F,color);
    }
    private static void ring(Sink s,Vec3 center,double r,double t,int axis,int color,double width){
        for(int i=0;i<32;i++){double a=t+i*Math.PI/16,b=t+(i+1)*Math.PI/16;
            Vec3 p=circle(a,r,axis),q=circle(b,r,axis);s.accent(center.add(p),center.add(q),width,.65F,color);}
    }
    private static Vec3 circle(double a,double r,int axis){return axis==0?new Vec3(Math.cos(a)*r,0,Math.sin(a)*r):axis==1?new Vec3(0,Math.cos(a)*r,Math.sin(a)*r):new Vec3(Math.cos(a)*r,Math.sin(a)*r,0);}
    private static void sparks(Sink s,double t,int count,double radius,double height,int color){
        for(int i=0;i<count;i++){double a=i*2.399963+t*.6,u=(i/(double)count+t*.13)%1;
            s.mote(new Vec3(Math.cos(a)*radius*u,(u-.15)*height,Math.sin(a)*radius*u),.025+(i%3)*.012,color,(float)(1-u));}
    }
    private static void shards(Sink s,double t,int count,double radius,int color){
        for(int i=0;i<count;i++){double a=t+i*2.4,r=radius*(.5+(i%4)*.16);Vec3 p=new Vec3(Math.cos(a)*r,Math.sin(a)*r,-.4-(i%5)*.6);
            blade(s,p,.08+(i%3)*.045,.35+(i%4)*.15,color);s.accent(p,p.add(0,.15,-.75),.035,.5F,SILVER);}
    }
    private static void shell(Sink s,double radius,int color,float alpha,int slices,int rows){
        for(int row=0;row<rows;row++)for(int i=0;i<slices;i++){
            double a=i*Math.PI*2/slices,b=(i+1)*Math.PI*2/slices,p=row*Math.PI/rows,q=(row+1)*Math.PI/rows;
            s.patch(sphere(a,p,radius),sphere(b,p,radius),sphere(b,q,radius),sphere(a,q,radius),color,alpha);
        }
    }
    private static Vec3 sphere(double a,double b,double r){return new Vec3(Math.cos(a)*Math.sin(b)*r,Math.cos(b)*r,Math.sin(a)*Math.sin(b)*r);}
}
