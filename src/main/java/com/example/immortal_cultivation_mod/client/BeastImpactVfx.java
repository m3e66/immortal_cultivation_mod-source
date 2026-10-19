package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.flame.FlameHeatHaze;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.network.BeastVfxPayloads.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Deterministic, anchored impacts and short residue. Sample ground once; no terrain edits or broad scans. */
final class BeastImpactVfx {
    private final Impact data;
    private final ClientLevel world;
    private final long born;
    private final int lifetime,tint,soil;
    private final Vec3 ground,direction,side;
    private final boolean onSurface;
    BeastImpactVfx(Impact data,ClientLevel world){
        this.data=data;this.world=world;this.born=world.getGameTime();
        tint=BeastAttackVfx.color(data.style());
        lifetime=switch(data.style()){
            case LIZARD->data.projectile()?80:24;
            case DRAGON->60;case OWL,FROST->48;case EARTH,BLOOD,THUNDER->36;
            case SILK,FOX,SPACE->28;default->18;
        };
        direction=data.direction().lengthSqr()<.001?new Vec3(0,1,0):data.direction().normalize();
        side=com.example.immortal_cultivation_mod.physics.CultivationFlightTrail.side(direction,null);
        Vec3 floor=data.point();boolean found=false;
        if(world.hasChunkAt(BlockPos.containing(floor))&&world.hasChunkAt(BlockPos.containing(floor.add(0,-4,0)))){
            var hit=world.clip(groundClip(floor));
            if(hit.getType()==HitResult.Type.BLOCK){floor=hit.getLocation().add(0,.015,0);found=true;}
        }
        ground=floor;onSurface=found&&floor.distanceToSqr(data.point())<4;
        BlockPos pos=BlockPos.containing(ground.add(0,-.03,0));int sampled=0x6E6659;
        if(found&&world.hasChunkAt(pos)){
            var state=world.getBlockState(pos);if(state.getMapColor(world,pos).col!=0)sampled=state.getMapColor(world,pos).col;
            // A few actual ground-texture fragments accompany custom angular debris.
            if(data.style()!=Style.FOX&&data.style()!=Style.SILK&&data.style()!=Style.SPACE&&data.style()!=Style.RAT&&!state.isAir()){
                int count=Minecraft.getInstance().options.particles().get()==ParticleStatus.MINIMAL?2:8;
                for(int i=0;i<count;i++){
                    double a=i*2.4;world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),ground.x,ground.y+.06,ground.z,
                            Math.cos(a)*.07,.09+noise(i)*.05,Math.sin(a)*.07);
                }
            }
        }
        soil=sampled;
    }
    static ClipContext groundClip(Vec3 point) {
        // Visual ground sampling has no entity, including after its source despawns.
        return new ClipContext(point.add(0,.2,0), point.add(0,-4,0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
    }
    Vec3 point(){return data.point();}
    boolean expired(long tick){return tick-born>=lifetime;}
    void draw(BeastVfxGeometry g,double time){
        double age=time-born;if(age<0||age>=lifetime)return;
        float fade=(float)Math.pow(1-age/lifetime,1.4);
        Vec3 point=data.point();
        if(data.phase()==Phase.DISSOLVE){
            double u=Math.min(1,age/18);
            int tint=data.style()==Style.RAT?0x263C31:data.style()==Style.SPACE?0xABA2B9:0xC8D5CD;
            for(int i=0;i<BeastVfxGeometry.detail(6);i++){
                Vec3 c=point.add(side.scale((i-(BeastVfxGeometry.detail(6)-1)*.5)*.13)).add(0,u*.16,0);
                if(data.style()==Style.SILK)g.curve(c,c.add(0,.1-u*.2,0),c.add(side.scale(.12+u*.25)),.003,tint,fade*.45F);
                else if(data.style()==Style.SPACE)g.chip(c,.03,tint,fade*.35F,age*.06+i);
                else {g.mist(c,.13,tint,fade*.07F,1.7);g.curve(c,c.add(0,.2,0),c.add(side.scale(.18)),.004,tint,fade*.18F);}
            }
            return;
        }
        if(data.phase()==Phase.DEFLECT){
            g.crack(point,new Vec3(.2,1,.1),.65,0x473851,fade, data.seed());
            g.crack(point.add(side.scale(.012)),new Vec3(.2,1,.1),.65,0xCDC5D8,fade,data.seed());
            shards(g,point,age,.45,0xB8ACC8,fade,5);return;
        }
        if(data.phase()==Phase.ARMOR){
            if(data.style()==Style.SILK){
                for(int i=0;i<BeastVfxGeometry.detail(4);i++){
                    Vec3 a=point.add(side.scale((i-(BeastVfxGeometry.detail(4)-1)*.5)*.12));
                    g.curve(a,a.add(0,.08*Math.sin(age*.5),0),a.add(direction.scale(.3)),.006,0xCAF7ED,fade*.65F);
                }
                return;
            }
            shards(g,point,age,.5,data.style()==Style.SCORPION?0xA17B57:soil,fade,6);
            g.crack(point,side,.4,tint,fade*.4F,data.seed());return;
        }
        switch(data.style()){
            case RAT->{
                if(data.move()==1){
                    Entity rat=world.getEntity(data.sourceId());
                    Vec3 mouth=rat==null?point.subtract(direction.scale(.8)):
                            BeastVfxAnchors.point(rat,rat.getEyePosition(),"jaw","head");
                    for(int i=0;i<BeastVfxGeometry.detail(4);i++){
                        double u=Mth.clamp(age/12+i*.11,0,1);
                        Vec3 p=point.lerp(mouth,u).add(0,Math.sin(u*Math.PI)*.16,0);
                        g.curve(p,p.add(side.scale(Math.sin(age*.2+i)*.12)),p.lerp(mouth,.28),.005,0x709E7B,fade*.7F);
                    }
                }else flash(g,point,age,tint,fade,.14);
            }
            case SILK->{for(int i=0;i<BeastVfxGeometry.detail(5);i++)g.curve(point.add(side.scale((i-(BeastVfxGeometry.detail(5)-1)*.5)*.13)),point.add(0,.15,0),point.add(0,-.08,0),.004,tint,fade*.6F);}
            case WIND->{for(int i=0;i<BeastVfxGeometry.detail(4);i++){
                Vec3 p=point.add(direction.scale(age*.035)).add(side.scale((i-(BeastVfxGeometry.detail(4)-1)*.5)*.1));
                g.feather(p,direction,.12,tint,fade*.45F,age*.03+i,false);
                g.curve(p,p.add(direction.scale(.25)).add(0,.08,0),p.add(direction.scale(.5)),.005,tint,fade*.4F);
            }}
            case SCORPION->{
                flash(g,point,age,0x9F4342,fade,.2);shards(g,point,age,.45,0x80634D,fade,4);
                if(data.move()==2)for(int i=0;i<BeastVfxGeometry.detail(4);i++){
                    Vec3 p=point.add(side.scale(Math.sin(age*.15+i)*.15)).add(0,age*.003,0);
                    g.mist(p,.08,0x592A37,fade*.10F,1.3);g.chip(p,.013,0x783640,fade*.45F,age*.1+i);
                }
            }
            case EARTH,BLOOD,THUNDER,FROST->{
                double radius=data.phase()==Phase.LANDING?2.5:data.move()==2||data.move()==5?1.7:.7;
                physical(g,age,radius,fade);
                if(data.style()==Style.THUNDER)for(int i=0;i<3;i++){
                    double a=i*2.4;g.crack(onSurface?ground:point,new Vec3(Math.cos(a),.02,Math.sin(a)),.7,tint,fade,data.seed()+i);
                }
                if(data.style()==Style.FROST){shards(g,point,age,radius*.7,0xBDDAE4,fade,6);frost(g,age,radius,fade);}
                if(data.style()==Style.BLOOD)flash(g,point,age,0x8D3541,fade,.24);
            }
            case SPACE->{
                Vec3 start=point.add(side.scale(.12*(1-Math.min(1,age/6))));
                g.crack(start,new Vec3(.2,1,.2),.3,0xC4B8D2,fade,data.seed());
                shards(g,point,Math.max(0,age-3),.6,0xB3A8C5,fade,7);
            }
            case FOX->{
                for(int i=0;i<BeastVfxGeometry.detail(5);i++){
                    double u=Math.min(1,age/18),a=i*2.4;
                    Vec3 p=point.add(Math.cos(a)*u*.5,u*.12,Math.sin(a)*u*.5);
                    g.mist(p,.1+u*.08,0xDDE2DF,fade*.075F,1.4);
                    g.mote(p,.018,0xA56B72,fade*.4F);
                }
            }
            case LIZARD,OWL,DRAGON,GUARDIAN->{
                boolean strong=data.projectile()&&(data.style()==Style.LIZARD||data.style()==Style.DRAGON);
                if(age<5)flash(g,point,age,0xDDA96A,fade,strong?.4:.18);
                double radius=strong?2.3:data.phase()==Phase.LANDING?2:1;
                physical(g,age,radius,fade*.8F);
                flame(g,age,radius,fade,strong);
            }
        }
    }
    private void flash(BeastVfxGeometry g,Vec3 point,double age,int color,float fade,double radius){
        if(age>7)return;
        for(int i=0;i<3;i++){
            Vec3 d=side.scale(Math.cos(i*2.1)).add(new Vec3(0,1,0).scale(Math.sin(i*2.1)));
            g.line(point.subtract(d.scale(radius*.3)),point.add(d.scale(radius*(.4+age*.15))),.012,color,fade*.6F);
        }
    }
    private void physical(BeastVfxGeometry g,double age,double radius,float fade){
        radius *= 1.5;
        shards(g,onSurface?ground:data.point(),age,radius,soil,fade,7);
        if(!onSurface)return;
        double expand=Math.min(1,age/12);
        for(int i=0;i<BeastVfxGeometry.detail(7);i++){
            double a=i*2.4,r=radius*(.35+noise(i)*.65);
            Vec3 axis=new Vec3(Math.cos(a),0,Math.sin(a));
            g.crack(ground,axis,r*expand,BeastVfxGeometry.shade(soil,.75),fade*.55F,data.seed()+i);
            Vec3 dust=ground.add(axis.scale(r*expand)).add(0,.08+age*.004,0);
            g.mist(dust,.12+age*.006,soil,fade*.13F,1.7);
        }
    }
    private void shards(BeastVfxGeometry g,Vec3 center,double age,double radius,int color,float fade,int count){
        count = BeastVfxGeometry.detail(count);
        double u=Math.min(1,age/24);
        for(int i=0;i<count;i++){
            double a=i*2.4,v=.3+noise(i)*.7;
            Vec3 p=center.add(Math.cos(a)*radius*u*v,Math.max(0,.05+Math.sin(u*Math.PI)*radius*(.3+noise(i+1)*.5)),Math.sin(a)*radius*u*v);
            g.chip(p,.025+noise(i+2)*.045,color,fade*(float)(1-u)*.7F,age*.08+i);
        }
    }
    private void frost(BeastVfxGeometry g,double age,double radius,float fade){
        radius *= 1.5;
        for(int i=0;i<BeastVfxGeometry.detail(5);i++){
            double a=i*2.4;Vec3 p=ground.add(Math.cos(a)*radius*.6,.08+age*.003,Math.sin(a)*radius*.6);
            g.mist(p,.15+noise(i)*.15,0xD8E3E5,fade*.12F,1.8);
            if(onSurface)g.crack(ground,new Vec3(Math.cos(a),0,Math.sin(a)),radius,0xB4CED7,fade*.25F,data.seed()+i);
        }
    }
    private void flame(BeastVfxGeometry g,double age,double radius,float fade,boolean strong){
        radius *= 1.5;
        Vec3 origin=onSurface?ground:data.point();
        // Broad ground-hugging tongues give way to dense smoke and embers.
        if(age<20)for(int i=0;i<BeastVfxGeometry.detail(6);i++){
            double a=i*2.4,u=Math.min(1,age/10),r=radius*u*(.4+noise(i)*.6);
            Vec3 p=origin.add(Math.cos(a)*r,.04,Math.sin(a)*r);
            g.curve(p,p.add(side.scale(Math.sin(age*.18+i)*.15)).add(0,.35+noise(i)*.3,0),
                    p.add(0,.5+noise(i)*.4,0),.018,0xD37A43,fade*.65F);
        }
        for(int i=0;i<BeastVfxGeometry.detail(5);i++){
            double a=i*2.4;Vec3 p=origin.add(Math.cos(a)*radius*.5,.15+age*.008,Math.sin(a)*radius*.5);
            g.mist(p,.12+age*.006,0x51483F,fade*.09F,1.4);
        }
        BeastMoveVfx.embers(g,origin,radius,age,fade*.65F);
        if(onSurface&&strong)for(int i=0;i<BeastVfxGeometry.detail(4);i++){
            double a=i*2.4;g.crack(ground,new Vec3(Math.cos(a),0,Math.sin(a)),radius*(.4+noise(i)*.6),0x7B5039,fade*.3F,data.seed()+i);
        }
        if(age<24)FlameHeatHaze.observeCharge(data.style()==Style.DRAGON?FlameKind.DRAGON:FlameKind.CRIMSON,origin,strong?.5F:.2F,(float)Math.min(3,radius));
    }
    private double noise(int i){return Math.abs(Math.sin(data.seed()*.001+i*12.9898));}
}
