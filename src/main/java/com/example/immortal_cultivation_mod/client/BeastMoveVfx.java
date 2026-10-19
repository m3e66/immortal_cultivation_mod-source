package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.flame.FlameHeatHaze;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.network.BeastVfxPayloads.Style;
import com.example.immortal_cultivation_mod.physics.CultivationFlightTrail;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Cultivation beast prompt choreography: anatomy, anticipation, release, and physical matter. */
final class BeastMoveVfx {
    private final BeastVfxGeometry g;
    private final Entity entity;
    private final Vec3 center,floor,forward,side,mouth,head;
    private final Style style;
    private final int color,soil,remaining;
    private final double age,time,size,phase;
    private final float fade;
    private final Map<String,List<Vec3>> paths;
    private final List<Vec3> bodyPath;
    private static final Vec3 UP=new Vec3(0,1,0);
    private BeastMoveVfx(BeastVfxGeometry g,Entity entity,Vec3 center,Style style,double age,
                         int remaining,double time,int soil,Map<String,List<Vec3>> paths,List<Vec3> bodyPath){
        this.g=g;this.entity=entity;this.center=center;this.style=style;this.age=age;this.remaining=remaining;
        this.time=time;this.soil=soil;this.paths=paths;this.bodyPath=bodyPath;this.color=BeastAttackVfx.color(style);
        this.floor=entity.getPosition((float)(time-Math.floor(time))).add(0,.035,0);
        this.forward=entity.getLookAngle().normalize();this.side=CultivationFlightTrail.side(forward,null);
        this.size=Mth.clamp(entity.getBbWidth(),.4,5);
        this.phase=Mth.clamp(age/Math.max(1,age+remaining),0,1);
        this.fade=(float) Math.min(1,Math.min((age+1)/3,(remaining+1)/4.0));
        this.head=anchor(center.add(forward.scale(size*.45)),"head");
        this.mouth=anchor(head.add(forward.scale(size*.13)),"jaw","head").add(forward.scale(size*.12));
    }
    static void draw(BeastVfxGeometry g,Entity entity,Vec3 center,Style style,int move,double age,
                     int remaining,double time,int soil,Map<String,List<Vec3>> paths,List<Vec3> bodyPath){
        var v=new BeastMoveVfx(g,entity,center,style,age,remaining,time,soil,paths,bodyPath);
        switch(style){
            case SILK->v.silk(move);case RAT->v.rat(move);case SCORPION->v.scorpion(move);
            case WIND->v.deer(move);case EARTH->v.tortoise(move);case OWL->v.owl(move);
            case LIZARD->v.lizard(move);case SPACE->v.beetle(move);case THUNDER->v.badger(move);
            case FROST->v.bear(move);case BLOOD->v.ape(move);case DRAGON->v.dragon(move);
            case FOX->v.fox(move);case GUARDIAN->v.guardian(move);
        }
    }
    private Vec3 anchor(Vec3 fallback,String... names){return BeastVfxAnchors.point(entity,fallback,names);}
    private Vec3 left(){return anchor(center.add(side.scale(size*.4)),"hand_l","pincer_left","front_leg_left","antler_l_s0");}
    private Vec3 right(){return anchor(center.subtract(side.scale(size*.4)),"hand_r","pincer_right","front_leg_right","antler_r_s0");}
    private Vec3 tail(){return anchor(center.subtract(forward.scale(size)),"stinger","tail_tip","tail_5","tail_0_tip","tail_3");}
    private void silk(int move){
        if(move==1){
            veins(mouth,color,.35,3);
            for(int i=0;i<3;i++)g.curve(mouth,mouth.add(forward.scale(.7)).add(0,.18,0),
                    mouth.add(forward.scale(1.6)).add(side.scale((i-1)*.12)),.007,color,fade);
        }
        web(move==2?.55:.85);
    }
    private void web(double radius){
        radius *= 1.35;
        // Irregular crossed fibers in three planes: woven cloth, never a spherical shield.
        for(int i=0;i<BeastVfxGeometry.detail(14);i++){
            double y=(i%5-2)*size*.13,z=Math.sin(i*3.1)*size*.35;
            Vec3 a=center.add(side.scale(-size*radius*(.6+noise(i)*.3))).add(0,y,z);
            Vec3 b=center.add(side.scale(size*radius*(.6+noise(i+3)*.3))).add(0,-y*.7,-z);
            Vec3 knot=a.lerp(b,.4).add(0,Math.sin(time*.11+i)*.015,0);
            g.curve(a,knot,b,.006,color,fade*.7F);
            if(i%3==0)g.mote(knot,.018,color,fade*.5F);
        }
    }
    private void rat(int move){
        switch(move){
            case -1->{teeth(.08);trail("mouth",color,.006);paws(.2);}
            case 1->{teeth(.1);veins(head,0x315B42,.2,4);g.mist(mouth,.12,0x213E2B,fade*.12F,1);}
            case 2->{shadow(0x203B2E);groundWisps(.35,0x273C31);}
            case 3->{
                Vec3 source=paths.containsKey("scent")?paths.get("scent").getFirst():mouth.add(forward.scale(1.5));
                for(int i=0;i<3;i++){
                    double u=(time*.025+i*.3)%1;
                    Vec3 p=source.lerp(mouth,u).add(0,Math.sin(u*Math.PI)*.2,0);
                    g.curve(p,p.add(0,.05,0),p.lerp(mouth,.15),.003,0xAED5B3,fade*.3F);
                }
                teeth(.045);
            }
            case 100->{paws(.4);shadow(0x344738);veins(head,0x497455,.12,2);}
            default->{}
        }
    }
    private void scorpion(int move){
        switch(move){
            case 1->{
                Vec3 tip=mouth.add(forward.scale(size*.5));
                g.curve(left(),left().add(forward.scale(size*.5)),tip.add(side.scale((1-phase)*.3)),.014,color,fade);
                g.curve(right(),right().add(forward.scale(size*.5)),tip.subtract(side.scale((1-phase)*.3)),.014,color,fade);
                trail("left",color,.015);trail("right",color,.015);
            }
            case 2->{Vec3 tip=tail();g.curve(center,tip.add(0,.15,0),tip,.014,0x8E2835,fade);
                trail("tail",0xA43C44,.013);g.mist(tip,.11,0x4E2630,fade*.17F,.8);}
            case 3->seams(.75,0xA77E51);
            case 4->{
                boolean buried=entity instanceof CultivationBestiaryBeastEntity b&&b.hasVisualFlag(1);
                disturbance(buried?1.2:2.0,!buried);
            }
            default->chips(center,soil,5,.5);
        }
    }
    private void deer(int move){
        switch(move){
            case -1,4->{trail("left",color,.009);trail("right",color,.009);featherFan(left(),3,false);}
            case 1->{hooves();trail("left",color,.008);trail("right",color,.008);paws(.6);}
            case 2->{veins(left(),color,.2,3);veins(right(),color,.2,3);cone(head,5.3,2.1,color,false,false);}
            case 3->{featherFan(left(),3,false);featherFan(right(),2,false);}
            default->{}
        }
    }
    private void tortoise(int move){
        switch(move){
            case 1->{seams(.8,0x756040);paws(.7);g.curve(left(),mouth.add(forward.scale(.3)),right(),.015,color,fade*.6F);}
            case 2->{for(int i=0;i<5;i++){double u=(i+phase)/5;Vec3 p=floor.lerp(mouth,u);g.chip(p,.04+i*.006,soil,fade*.6F,time*.1+i);}
                g.mist(mouth,.18,soil,fade*.15F,1.4);}
            case 3->{seams(.9,color);slabs();}
            case 4->{seams(.95,0x8F744A);g.mist(center,.3,0x3F4138,fade*.05F,2);}
            case 5->{if(age<3)seams(.9,color);else groundCracks(5,0x866F49,9);}
            default->chips(center,soil,4,.3);
        }
    }
    private void owl(int move){
        Vec3 a=anchor(left(),"wing_left_hand"),b=anchor(right(),"wing_right_hand");
        switch(move){
            case 1->{featherFan(a,4,true);featherFan(b,3,true);}
            case 2->{trail("wing_l",color,.012);trail("wing_r",color,.012);stream(center,forward.reverse(),1.8,color,6);embers(g,tail(),.5,time,fade*.6F);}
            case 3->{cone(center,5.3,2.3,color,true,false);g.mist(a,.16,0x665C50,fade*.1F,2);}
            case 4->{
                FlameHeatHaze.observeCharge(FlameKind.CRIMSON,center,.8F,2.5F);
                if(age<6){veins(a,color,.3,4);veins(b,color,.3,4);}
                else {groundWisps(2+phase*4,soil);fireTongues(center,1+phase*3,8);}
            }
            case 5->{hooves();stream(floor,UP,1.8,color,5);featherFan(center,4,true);}
            default->{veins(a,color,.35,3);veins(b,color,.35,3);}
        }
    }
    private void lizard(int move){
        switch(move){
            case -1,1->{teeth(.1);trail("mouth",color,.012);embers(g,mouth,.15,time,fade*.4F);}
            case 2->{
                if(remaining>27){veins(head,color,.25,4);g.mist(mouth,.22,0x55483C,fade*.15F,1);}
                else if(remaining>=7){cone(mouth,6.2,1.55,color,true,false);FlameHeatHaze.observeCharge(FlameKind.CRIMSON,mouth,.3F,1.7F);}
            }
            case 3->{trail("tail",color,.016);paws(.4);}
            case 4->{
                if(remaining>=9){Vec3 c=mouth.add(forward.scale(.25));fireTongues(c,.12+phase*.15,5);
                    stream(head,forward,.45,color,3);g.mote(c,.08,0xE4A366,fade*.6F);}
            }
            default->{}
        }
    }
    private void beetle(int move){
        switch(move){
            case -1,1->{teeth(.07);fracture(mouth,.2,2);}
            case 3->{Vec3 a=anchor(left(),"left_mandible"),b=anchor(right(),"right_mandible");
                fracture(a.lerp(mouth,phase),.45,1);fracture(b.lerp(mouth,phase),.45,3);}
            case 4->{for(int i=0;i<5;i++){Vec3 c=center.add(forward.scale(1.5)).add((noise(i)-.5)*3,2+noise(i+4),0);
                fracture(c,.22,i);g.chip(c.add(0,-phase*.9,0),.07,color,fade*.5F,time*.04+i);}}
            case 5->{fracture(center,.55,5);chips(center,0xB8AFCA,7,.6);shadow(0xA99CBD);}
            case 100->{seams(.7,0x756180);for(int i=0;i<3;i++)if(Math.sin(time*.2+i*5)> .25)fracture(center.add(side.scale((i-1)*.3)),.2,i);}
            default->{}
        }
    }
    private void badger(int move){
        Vec3 horn=anchor(head.add(forward.scale(.15)),"horn");
        switch(move){
            case 1->{trail("left",color,.015);lightning(left(),left().add(0,-.4,0),3);paws(.5);}
            case 2->{lightning(horn,horn.add(forward.scale(.7)),4);if((int)time%3==0)lightning(horn,floor.add(side.scale(.5)),2);paws(.6);}
            case 3->{if(remaining<=13){groundCracks(3.5,color,7);groundWisps(2,soil);}else paws(.2);}
            case 4->{for(int i=0;i<5;i++){double a=i*1.5;Vec3 c=center.add(Math.cos(a)*size,.2,Math.sin(a)*size);
                g.chip(c,.08+noise(i)*.04,soil,fade*.7F,time*.05+i);lightning(c,c.add(0,.2,0),i);}}
            case 5->disturbance(1.3,remaining<13);
            case 6->{seams(.8,color);chips(center,soil,8,.6);}
            case 7,100->{lightning(horn,left(),4);lightning(horn,right(),7);seams(.8,0x927C54);paws(.2);}
            default->{}
        }
    }
    private void bear(int move){
        switch(move){
            case 1->{trail("left",color,.02);g.mist(left(),.22,0xD7E5E6,fade*.15F,1.3);}
            case 2->{if(remaining>18)g.mist(mouth,.28,0xDCE7E7,fade*.16F,1.4);else cone(mouth,6,2.4,color,false,true);}
            case 3->{int count=(int)Math.min(6,age/5+1);for(int i=0;i<count;i++){
                Vec3 c=floor.add(forward.scale(i*.85+noise(i)*.3)).add(side.scale((noise(i+2)-.5)*.35));
                iceSpike(c,.35+noise(i+3)*.8);g.mist(c,.22,0xD0DBDD,fade*.12F,1.4);}}
            case 4->{trail("left",color,.012);trail("right",color,.012);chips(center,0xBCD5DF,5,.5);paws(.45);}
            case 5,101->fog(4,0xD3DBDC,.1F);
            case 6->{for(int i=0;i<7;i++){Vec3 c=center.add(side.scale((i-3)*.22)).add(0,size*.3,0);
                iceSpike(c,.15+noise(i)*.35);g.mist(c,.12,0xDAE8EC,fade*.14F,1.2);}}
            case 7,100->{veins(center,0x83B5CD,size*.3,5);g.mist(mouth,.32,0xCFDDE1,.15F,1.8);paws(.3);}
            default->{}
        }
    }
    private void ape(int move){
        switch(move){
            case 1->{veins(left(),color,.24,4);trail("left",color,.025);g.mist(left(),.13,0x52232C,fade*.1F,1);}
            case 2->{veins(anchor(floor,"foot_l"),color,.3,3);if(remaining<=14)groundCracks(4,color,7);}
            case 3->{veins(left(),color,.2,3);trail("left",color,.015);trail("right",color,.015);}
            case 4->{stream(left(),forward.reverse(),1.2,color,3);stream(right(),forward.reverse(),1.2,color,3);paws(.6);}
            case 5,100->{veins(center,color,size*.4,8);seams(.75,color);fog(size*.6,0x42242B,.055F);}
            default->{}
        }
    }
    private void dragon(int move){
        switch(move){
            case 1->{
                if(remaining>45){veins(head,color,.4,5);g.mist(mouth,.4,0x55443A,fade*.15F,1.4);}
                else if(remaining>=7){cone(mouth,10,2.4,color,true,false);FlameHeatHaze.observeCharge(FlameKind.DRAGON,mouth,.6F,2.2F);}
            }
            case 2->{trail("tail",color,.025);embers(g,tail(),.6,time,fade*.5F);paws(.6);}
            case 3->{seams(.85,0xA37442);for(int i=0;i<8;i++){Vec3 p=center.add(side.scale((i-4)*.4));g.chip(p,.08,color,fade*.6F,time*.05+i);}}
            case 4->{stream(head,forward.reverse(),3,color,7);veins(anchor(head,"horn_left"),color,.3,3);paws(.6);}
            case 5->{
                groundCracks(7,0xA65332,11);
                for(int i=0;i<5;i++){double a=i*2.4;Vec3 p=floor.add(Math.cos(a)*(2+noise(i)*4),0,Math.sin(a)*(2+noise(i)*4));
                    fireTongues(p,.6+noise(i)*.7,3);g.mist(p.add(0,1,0),.4,0x51443A,fade*.08F,1.2);}
                FlameHeatHaze.observeCharge(FlameKind.DRAGON,center,.4F,3);
            }
            default->{}
        }
    }
    private void fox(int move){
        switch(move){
            case -1,11->{shadow(0xD3DAD9);trail("tail",color,.006);trail("left",color,.005);paws(.08);}
            case 1,12->{shadow(0xD6DCDC);fog(size*.6,0xD9DEDD,.06F);veins(head,0x965D62,.16,3);}
            case 2->{for(int i=0;i<5;i++){double a=time*.04+i*1.3;Vec3 c=tail().add(Math.cos(a)*.6,.15+Math.sin(a*2)*.1,Math.sin(a)*.6);
                g.mote(c,.055,0xE5EBE7,fade*.45F);g.curve(c,c.add(0,.16,0),c.add(.04,.22,0),.007,0x9E686C,fade*.45F);}}
            case 3->{fog(2.4,0xDBDFDD,.075F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 c=center.add(Math.cos(a)*2,0,Math.sin(a)*2);foxOutline(c,i);}}
            case 4->fog(5,0xD9DDDC,.085F);
            case 5->{trail("tail",color,.012);g.curve(tail(),tail().add(forward.scale(size)),mouth,.01,0xC7D2D4,fade);}
            case 6,7->{veins(head,0xAF777A,.3,4);g.curve(left(),center.add(0,.5,0),right(),.006,color,fade*.4F);}
            case 8->{for(int i=0;i<3;i++)foxOutline(center.add(forward.scale(i*1.5)),i);fog(2.2,0xD5DBDA,.06F);}
            case 9,10->{veins(head,0xA87076,.24,5);fog(2.2,0xD8DEDA,.065F);for(int i=0;i<5;i++)foxOutline(center.add(side.scale((i-2)*.45)),i);}
            default->{}
        }
    }
    private void guardian(int move){
        switch(move){
            case 1,2->{trail("left",color,.02);paws(.6);}
            case 3->cone(mouth,5,1.3,color,true,false);
            case 4->{groundCracks(5,color,8);chips(floor,soil,6,.8);}
            case 5->chips(center,soil,8,1);
            case 6->{trail("left",color,.015);trail("right",color,.015);}
            case 7->seams(.8,color);
            case 8,9->{veins(center,color,.4,5);g.mist(head,.3,0x615749,.13F,1.3);}
            default->veins(center,color,.2,4);
        }
    }
    private void teeth(double length){for(int i=0;i<3;i++)g.line(mouth.add(side.scale((i-1)*.035)),mouth.add(side.scale((i-1)*.035)).add(0,length,0),.006,color,fade*.6F);}
    private void veins(Vec3 c,int tint,double radius,int count){
        radius *= 1.5; count = BeastVfxGeometry.detail(count);
        for(int i=0;i<count;i++){
        Vec3 a=c.add(side.scale((noise(i)-.5)*radius));g.curve(a,a.add(0,radius*.3,0),a.add(forward.scale(radius*.65)),.004,tint,fade*(float)(.4+.2*Math.sin(time*.12+i)));}}
    private void seams(double scale,int tint){
        Vec3 shell=anchor(center,"body","torso","shell_front","left_shell");
        for(int i=0;i<6;i++){
            double a=i*1.04;Vec3 c=shell.add(side.scale(Math.cos(a)*size*scale*.3)).add(forward.scale(Math.sin(a)*size*.22));
            g.curve(c,c.add(0,.08,0),c.add(side.scale(size*.17)).add(0,-.05,0),.006,tint,fade*.5F);
        }
    }
    private void trail(String slot,int tint,double width){
        List<Vec3> points=paths.get(slot);if(points==null)return;
        for(int i=0;i<Math.min(points.size()-1,5);i++)g.streak(points.get(i),points.get(i+1),width*(1-i*.13),tint,fade*(1-i*.17F),time+i);
    }
    private void shadow(int tint){
        for(int i=1;i<Math.min(bodyPath.size(),5);i++){
            Vec3 c=bodyPath.get(i);g.mist(c,size*.24,tint,fade*(.10F-i*.015F),1.9);
            g.curve(c.add(side.scale(-size*.25)),c.add(0,size*.25,0),c.add(side.scale(size*.25)),.004,tint,fade*(.22F-i*.03F));
        }
    }
    private void cone(Vec3 origin,double length,double spread,int tint,boolean fire,boolean frost){
        length *= 1.25; spread *= 1.6;
        for(int strand=0;strand<BeastVfxGeometry.detail(7);strand++){
            double angle=strand*2.4;Vec3 last=origin;
            for(int j=1;j<=9;j++){
                double u=j/9.0,r=u*spread*(.35+noise(strand)*.65);
                Vec3 p=origin.add(forward.scale(length*u)).add(side.scale(Math.cos(angle+u*2+time*.05)*r))
                        .add(UP.scale(Math.sin(angle+u*3)*r*.45+Math.sin(time*.13+j+strand)*.07));
                g.line(last,p,fire?.025:.011,tint,fade*(float)(1-u)*.65F);
                if(j%3==0){
                    g.mist(p,.14+u*.3,fire?0x655347:frost?0xDCE7E8:0xC2CEC7,fade*(float)(1-u)*.12F,1.5);
                    if(fire)g.mote(p,.025,tint,fade*.4F);
                    else if(frost)g.chip(p,.02,tint,fade*.4F,time*.1+strand);
                    else g.feather(p,forward,.1,tint,fade*.25F,time*.03,false);
                }
                last=p;
            }
        }
    }
    private void stream(Vec3 c,Vec3 direction,double length,int tint,int strands){
        length *= 1.5; strands = BeastVfxGeometry.detail(strands);
        for(int i=0;i<strands;i++){
            Vec3 a=c.add(side.scale((i-(strands-1)*.5)*.09));
            g.curve(a,a.add(direction.scale(length*.5)).add(0,Math.sin(time*.13+i)*.12,0),a.add(direction.scale(length)),.008,tint,fade*.6F);
        }
    }
    private void groundCracks(double radius,int tint,int count){
        radius *= 1.5; count = BeastVfxGeometry.detail(count);
        for(int i=0;i<count;i++){
            double a=i*2.4;Vec3 direction=new Vec3(Math.cos(a),0,Math.sin(a));
            g.crack(floor,direction,radius*(.45+noise(i)*.55),tint,fade*.4F,i+entity.getId());
        }
        groundWisps(radius,soil);
    }
    private void groundWisps(double radius,int tint){
        radius *= 1.4;
        for(int i=0;i<BeastVfxGeometry.detail(7);i++){
            double a=i*2.4,u=(time*.017+noise(i))%1;
            Vec3 c=floor.add(Math.cos(a)*radius*u,.06+u*.14,Math.sin(a)*radius*u);
            g.mist(c,.1+u*.25,tint,fade*(float)Math.sin(u*Math.PI)*.11F,1.8);
        }
    }
    private void disturbance(double radius,boolean erupt){
        groundWisps(radius,soil);
        for(int i=0;i<4;i++){double a=i*2.4;g.crack(floor,new Vec3(Math.cos(a),0,Math.sin(a)),radius*(.4+noise(i)*.3),soil,fade*.35F,i);}
        if(erupt)chips(floor,soil,7,.8);
    }
    private void chips(Vec3 c,int tint,int count,double radius){
        radius *= 1.5; count = BeastVfxGeometry.detail(count);
        for(int i=0;i<count;i++){
        double u=(time*.021+noise(i))%1,a=i*2.4;
        Vec3 p=c.add(Math.cos(a)*radius*u,Math.sin(u*Math.PI)*radius*.7,Math.sin(a)*radius*u);
        g.chip(p,.025+noise(i+3)*.035,tint,fade*(float)Math.sin(u*Math.PI)*.6F,time*.06+i);
    }}
    private void paws(double strength){
        Vec3 a=anchor(floor.add(side.scale(size*.3)),"foot_l","fore_hoof_l","front_leg_left");
        Vec3 b=anchor(floor.subtract(side.scale(size*.3)),"foot_r","fore_hoof_r","front_leg_right");
        for(Vec3 p:List.of(a,b))g.mist(p.add(0,.05,0),.1+strength*.14,soil,fade*(float)(.04+.05*strength),1.6);
    }
    private void hooves(){for(Vec3 c:List.of(anchor(floor,"fore_hoof_l"),anchor(floor,"fore_hoof_r"))){
        for(int i=0;i<3;i++){double a=time*.1+i*2;g.curve(c.add(Math.cos(a)*.13,0,Math.sin(a)*.13),c.add(0,.16,0),c.add(Math.cos(a+1)*.13,.3,Math.sin(a+1)*.13),.005,color,fade*.5F);}
    }}
    private void slabs(){for(int i=0;i<5;i++){
        Vec3 source=floor.add(side.scale((i-2)*.35));Vec3 end=center.add(side.scale((i-2)*size*.12));
        Vec3 p=source.lerp(end,Mth.clamp(age/12,0,1));g.chip(p,.1+noise(i)*.05,soil,fade*.75F,i*.7);
        g.line(p,end,.005,color,fade*.3F);
    }}
    private void featherFan(Vec3 origin,int count,boolean burning){
        count = BeastVfxGeometry.detail(count);
        for(int i=0;i<count;i++){
        Vec3 c=origin.add(side.scale((i-(count-1)*.5)*.16)).add(0,Math.sin(time*.06+i)*.04,0);
        g.feather(c,forward,.3,color,fade*.65F,time*.025+i,burning);
    }}
    private void fireTongues(Vec3 c,double radius,int count){
        radius *= 1.5; count = BeastVfxGeometry.detail(count);
        for(int i=0;i<count;i++){
        double a=i*2.4;Vec3 start=c.add(Math.cos(a)*radius*.5,0,Math.sin(a)*radius*.5);
        g.curve(start,start.add(side.scale(Math.sin(time*.12+i)*radius*.25)).add(0,radius*.8,0),
                start.add(0,radius*(1.1+noise(i)*.4),0),.018,color,fade*.65F);
    }}
    private void fog(double radius,int tint,float opacity){
        radius *= 1.5;
        for(int i=0;i<BeastVfxGeometry.detail(9);i++){
        double a=i*2.4,u=(time*.008+noise(i))%1;Vec3 p=floor.add(Math.cos(a)*radius*u,.1+noise(i+1)*.35,Math.sin(a)*radius*u);
        g.mist(p,.22+noise(i+4)*.2,tint,fade*opacity*(float)Math.sin(u*Math.PI),1.9);
    }}
    private void fracture(Vec3 c,double length,int seed){
        length *= 1.5;
        Vec3 axis=UP.add(side.scale(.3));g.crack(c.subtract(UP.scale(length*.5)),axis,length,0x4C3E61,fade*.5F,seed);
        g.crack(c.subtract(UP.scale(length*.5)).add(side.scale(.014)),axis,length,0xC5BED0,fade*.7F,seed);
    }
    private void lightning(Vec3 a,Vec3 b,int seed){
        Vec3 last=a;for(int i=1;i<=7;i++){
            double u=i/7.0;Vec3 p=a.lerp(b,u).add(side.scale(Math.sin(i*5.8+seed+Math.floor(time/3))*.10*Math.sin(u*Math.PI)));
            g.line(last,p,.008,0xEAD99B,fade*.65F);if(i==3)g.line(p,p.add(side.scale(.12)).add(0,.1,0),.004,0xEAD99B,fade*.4F);last=p;
        }
    }
    private void iceSpike(Vec3 c,double height){
        height *= 1.5;
        Vec3 tip=c.add(0,height,0);g.triangle(c.add(.07,0,0),c.add(0,0,.1),tip,0xA4C4D4,fade*.45F);
        g.triangle(c.add(0,0,.1),c.add(-.08,0,0),tip,0xD1E2E6,fade*.5F);
        g.line(c,tip,.006,color,fade*.4F);
    }
    private void foxOutline(Vec3 c,int seed){
        double sway=Math.sin(time*.04+seed)*.05;
        g.curve(c.add(-.22,0,0),c.add(0,.18+sway,0),c.add(.25,0,0),.004,color,fade*.18F);
        g.curve(c.add(.25,0,0),c.add(.4,.2,0),c.add(.43,.14,0),.003,color,fade*.16F);
        g.curve(c.add(-.22,0,0),c.add(-.4,.4+sway,0),c.add(-.5,.13,0),.004,color,fade*.15F);
        g.line(c.add(.38,.12,0),c.add(.41,.18,0),.003,0xA56D72,fade*.2F);
    }
    private double noise(int i){return Math.abs(Math.sin(i*12.9898+entity.getId()*3.17));}
    static void embers(BeastVfxGeometry g,Vec3 c,double radius,double time,float fade){
        radius *= 1.5;
        for(int i=0;i<BeastVfxGeometry.detail(6);i++){
            double u=(time*.018+i*.137)%1,a=i*2.4;
            Vec3 p=c.add(Math.cos(a)*radius*.5,u*.65,Math.sin(a)*radius*.5);
            g.mote(p,.02,0xD58249,fade*(float)Math.sin(u*Math.PI)*.6F);
            g.mist(p.add(0,.2,0),.1,0x554D44,fade*.035F,1.3);
        }
    }
    static void projectile(BeastVfxGeometry g,Entity entity,Style style,Vec3 head,List<Vec3> points,double time,int soil,boolean body){
        int color=BeastAttackVfx.color(style);Vec3 motion=entity.getDeltaMovement();
        Vec3 direction=motion.lengthSqr()<.001?new Vec3(0,1,0):motion.normalize();
        boolean stone=style==Style.EARTH||style==Style.THUNDER;
        boolean feather=style==Style.WIND||style==Style.OWL;
        for(int i=0;i<Math.min(points.size()-1,7);i++){
            Vec3 p=points.get(i);float fade=(1-i/8F)*.6F;
            if(stone){g.mist(p,.07+i*.012,soil,fade*.08F,1.1);g.chip(p,.015,soil,fade*.3F,time*.2+i);}
            else if(style==Style.SPACE){g.streak(p,points.get(i+1),.006,0xC3B8D0,fade,time+i);g.chip(p,.014,color,fade*.25F,time*.08+i);}
            else {g.streak(p,points.get(i+1),style==Style.SILK?.003:.007,color,fade,time+i);
                if(style==Style.FOX)g.mist(p,.1,0xD9DDDA,fade*.045F,1.5);}
        }
        if(!body)return;
        if(feather)g.feather(head,direction,.36,color,.8F,time*.17,style==Style.OWL);
        else if(stone){g.chip(head,.11,soil,.85F,time*.16);if(style==Style.THUNDER)g.crack(head,direction,.25,0xEAD99B,.6F,entity.getId());}
        else if(style==Style.SPACE)g.chip(head,.09,0xB4A7C8,.65F,time*.12);
        else if(style==Style.DRAGON){g.chip(head,.13,0xB7854D,.8F,time*.2);embers(g,head,.2,time,.8F);}
        else if(style==Style.FOX){g.mote(head,.065,0xE0E4DF,.65F);g.curve(head,head.add(0,.13,0),head.add(.05,.22,0),.006,0xA2666A,.55F);}
        else if(style==Style.SILK){g.line(head.subtract(direction.scale(.12)),head.add(direction.scale(.12)),.004,0xBBE7DC,.7F);}
        else if(style==Style.LIZARD){g.mote(head,.14,0xD28851,.5F);embers(g,head,.28,time,.7F);}
    }
}
