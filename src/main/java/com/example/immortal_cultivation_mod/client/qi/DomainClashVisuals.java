package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.client.QiAuraRenderer;
import com.example.immortal_cultivation_mod.client.QiDomainDistortion;
import com.example.immortal_cultivation_mod.combat.MethodDomainStyle;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.physics.DomainVolume;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** World-space elastic membranes and authoritative clash/shatter presentation. */
public final class DomainClashVisuals {
    private record Key(UUID first,UUID second) { }
    private record Contact(QiAuraPayloads.DomainClash state,long received) { }
    private record Collapse(Vec3 center,int style,long started) { }
    private static final Map<Key,Contact> CONTACTS=new HashMap<>();
    private static final List<Collapse> COLLAPSES=new ArrayList<>();
    private static ClientLevel world;
    private static long lastRumble=Long.MIN_VALUE,lastParticleTick=Long.MIN_VALUE;
    private static int particleContacts;
    public static long shellFrames,compressedFrames,contactFrames,shatterEvents,shatterFrames,shakingFrames,pressureBurstFrames;
    private DomainClashVisuals() { }
    private static void use() {var level=Minecraft.getInstance().level;if(level!=world){clear();world=level;}}
    public static void clear(){CONTACTS.clear();COLLAPSES.clear();world=null;lastRumble=Long.MIN_VALUE;lastParticleTick=Long.MIN_VALUE;particleContacts=0;}
    public static void accept(QiAuraPayloads.DomainClash packet) {
        use();if(world==null||!world.dimension().location().equals(packet.dimension()))return;
        var key=new Key(packet.firstUuid(),packet.secondUuid());
        if(packet.kind()==2){CONTACTS.remove(key);return;}
        var first=world.getEntity(packet.firstId());var second=world.getEntity(packet.secondId());
        if(!(first instanceof LivingEntity)||!(second instanceof LivingEntity)
                ||!first.getUUID().equals(packet.firstUuid())||!second.getUUID().equals(packet.secondUuid()))return;
        long now=world.getGameTime();
        if(packet.kind()==1) {
            CONTACTS.remove(key);if(packet.winner()==0)return;
            var loser=packet.winner()==1?second:first;
            int style=packet.winner()==1?packet.secondStyle():packet.firstStyle();
            if(COLLAPSES.size()>=16)COLLAPSES.removeFirst();
            COLLAPSES.add(new Collapse(loser.position(),style,now));shatterEvents++;
            var look=QiElementLook.of(MethodDomainStyle.values()[style]);
            for(int i=0;i<40;i++) {
                Vec3 direction=fibonacci(i,40);
                QiEnergyParticles.energy(loser.position().add(direction.scale(18)),direction.scale(.35),look,22);
            }
            sound(loser.position(),true);return;
        }
        if(CONTACTS.size()>=2016&&!CONTACTS.containsKey(key))return;
        if(!CONTACTS.containsKey(key))sound(first.position().lerp(second.position(),.5),false);
        CONTACTS.put(key,new Contact(packet,now));
        // Packet cadence, rather than render rate, bounds sparks even with many overlapping fields.
        if(lastParticleTick!=now){lastParticleTick=now;particleContacts=0;}
        if(particleContacts++<8)contactSparks((LivingEntity)first,(LivingEntity)second,packet,now);
    }
    private static void contactSparks(LivingEntity first,LivingEntity second,QiAuraPayloads.DomainClash state,long now) {
        Vec3 a=first.position(),b=second.position();
        var cut=DomainVolume.contact(a,b,state.age(),state.winner()==0?0:state.winner()==1?1:-1,1);
        Vec3 normal=cut.normal(),center=a.add(normal.scale(cut.offset()));
        if(center.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>80*80)return;
        Vec3 u=normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=normal.cross(u).normalize();
        double radius=Math.sqrt(Math.max(0,400-cut.offset()*cut.offset()));
        var firstLook=QiElementLook.of(MethodDomainStyle.values()[state.firstStyle()]);
        var secondLook=QiElementLook.of(MethodDomainStyle.values()[state.secondStyle()]);
        int count=QiVfxQuality.level()==0?4:10;
        for(int i=0;i<count;i++) {
            double angle=i*2.399963+now*.31,r=radius*Math.sqrt((i+.5)/count)*.88;
            Vec3 radial=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            QiEnergyParticles.energy(center.add(radial.scale(r)),radial.scale(.16).add(normal.scale(i%2==0?.30:-.30)),i%2==0?firstLook:secondLook,12);
        }
    }
    private static void sound(Vec3 point,boolean shattered) {
        long now=world.getGameTime();if(!shattered&&lastRumble!=Long.MIN_VALUE&&now-lastRumble<20)return;
        lastRumble=now;
        world.playLocalSound(point.x,point.y,point.z,shattered?SoundEvents.GLASS_BREAK:SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,shattered?1.3F:.65F,shattered?.55F:1.35F,false);
        if(shattered)world.playLocalSound(point.x,point.y,point.z,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.8F,.7F,false);
    }
    public static DomainVolume volume(LivingEntity owner,float partial) {
        use();var cuts=new ArrayList<DomainVolume.Cut>();
        if(world==null)return new DomainVolume(owner.getPosition(partial),cuts);
        long now=world.getGameTime();
        for(var contact:CONTACTS.values()) {
            var state=contact.state;if(now-contact.received>18)continue;
            boolean first=state.firstUuid().equals(owner.getUUID());
            if(!first&&!state.secondUuid().equals(owner.getUUID()))continue;
            var rival=world.getEntity(first?state.secondId():state.firstId());
            if(!(rival instanceof LivingEntity)||!rival.getUUID().equals(first?state.secondUuid():state.firstUuid()))continue;
            int sign=state.winner()==0?0:state.winner()==(first?1:2)?1:-1;
            cuts.add(DomainVolume.contact(owner.getPosition(partial),rival.getPosition(partial),
                    state.age()+now-contact.received+partial,sign,first?1:-1));
        }
        return new DomainVolume(owner.getPosition(partial),cuts);
    }
    public static void domain(VertexConsumer out,Matrix4f matrix,LivingEntity owner,Vec3 camera,float partial,
                              double time,int color,int accent,float fade) {
        var volume=volume(owner,partial);shellFrames++;if(!volume.cuts().isEmpty()){compressedFrames++;shakingFrames++;}
        int rows=QiVfxQuality.level()==0?10:18,columns=QiVfxQuality.level()==0?32:64;
        Vec3[][] points=new Vec3[rows+1][columns+1];
        for(int row=0;row<=rows;row++)for(int column=0;column<=columns;column++) {
            double latitude=-Math.PI/2+Math.PI*row/rows,longitude=column*Math.PI*2/columns;
            points[row][column]=volume.vibratingSurface(new Vec3(Math.cos(latitude)*Math.cos(longitude),Math.sin(latitude),Math.cos(latitude)*Math.sin(longitude)),time);
        }
        for(int row=0;row<rows;row++)for(int column=0;column<columns;column++) {
            Vec3[] quad={points[row][column],points[row+1][column],points[row+1][column+1],points[row][column+1]};
            for(Vec3 point:quad) {
                Vec3 normal=point.subtract(volume.center()).normalize();boolean cap=false;
                for(var cut:volume.cuts())if(point.subtract(volume.center()).dot(cut.normal())>cut.offset()-.75){normal=cut.normal();cap=true;break;}
                double rim=Math.pow(1-Math.abs(normal.dot(camera.subtract(point).normalize())),3);
                float alpha=(float)(.018+rim*.17+(cap?.055:0))*fade;
                vertex(out,matrix,point,camera,mix(color,accent,rim*.65),alpha);
            }
        }
        // A clear, continuous floor boundary remains readable from inside the field.
        for(int i=0;i<128;i++) {
            double a=i*Math.PI*2/128,b=(i+1)*Math.PI*2/128;
            var p=volume.vibratingSurface(new Vec3(Math.cos(a),0,Math.sin(a)),time).add(0,.12,0);
            var q=volume.vibratingSurface(new Vec3(Math.cos(b),0,Math.sin(b)),time).add(0,.12,0);
            QiPressureMesh.ribbon(out,matrix,p,q,camera,.065,color,.72F*fade);
            QiPressureMesh.ribbon(out,matrix,p,q,camera,.022,accent,.85F*fade);
        }
        // Sparse travelling arcs describe curvature without turning the dome into a wire cage.
        for(int j=0;j<6;j++)for(int i=0;i<28;i++) {
            double a=j*Math.PI/3+time*.002,latitude=-.12+i*Math.PI/32,next=latitude+Math.PI/32;
            float alpha=(float)(.16+.13*Math.sin(i*.18+time*.045+j))*fade;
            QiPressureMesh.ribbon(out,matrix,volume.vibratingSurface(new Vec3(Math.cos(latitude)*Math.cos(a),Math.sin(latitude),Math.cos(latitude)*Math.sin(a)),time),
                    volume.vibratingSurface(new Vec3(Math.cos(next)*Math.cos(a),Math.sin(next),Math.cos(next)*Math.sin(a)),time),camera,.022,accent,alpha);
        }
    }
    public static void render(VertexConsumer out,Matrix4f matrix,Vec3 camera,double time,float partial) {
        use();if(world==null)return;long now=world.getGameTime();
        CONTACTS.values().removeIf(c->now-c.received>18);
        COLLAPSES.removeIf(c->now-c.started>26);
        int count=0;
        var ordered=new ArrayList<>(CONTACTS.values());
        ordered.sort(Comparator.comparingDouble(c->{var e=world.getEntity(c.state.firstId());return e==null?Double.MAX_VALUE:e.distanceToSqr(camera);}));
        for(var contact:ordered) {
            if(++count>8)break;var state=contact.state;
            var first=world.getEntity(state.firstId());var second=world.getEntity(state.secondId());
            if(first==null||second==null||!first.getUUID().equals(state.firstUuid())||!second.getUUID().equals(state.secondUuid()))continue;
            Vec3 a=first.getPosition(partial),b=second.getPosition(partial);
            if(a.distanceToSqr(camera)>140*140)continue;
            double age=state.age()+now-contact.received+partial;
            var cut=DomainVolume.contact(a,b,age,state.winner()==0?0:state.winner()==1?1:-1,1);
            Vec3 normal=cut.normal(),center=a.add(normal.scale(cut.offset()));
            Vec3 u=normal.cross(Math.abs(normal.y)<.8?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=normal.cross(u).normalize();
            double radius=Math.max(.7,Math.sqrt(Math.max(0,400-cut.offset()*cut.offset())));
            int color=QiElementLook.of(MethodDomainStyle.values()[state.firstStyle()]).color(),other=QiElementLook.of(MethodDomainStyle.values()[state.secondStyle()]).color();
            float surge=(float)Math.pow(Math.max(0,Math.sin(time*.43)),6);
            float pressure=.60F+(float)Math.min(1,age/DomainVolume.CLASH_DURATION_TICKS)*.30F+surge*.15F;
            boolean low=QiVfxQuality.level()==0;
            // Both method colors tear across the same world-space contact disk.
            for(int j=0;j<(low?6:12);j++)for(int i=0;i<14;i++) {
                double t=i/14.0,s=(i+1)/14.0,angle=j*Math.PI/6+time*.025;
                Vec3 p=seam(center,u,v,normal,angle,t,radius,time,j),q=seam(center,u,v,normal,angle,s,radius,time,j);
                QiPressureMesh.ribbon(out,matrix,p,q,camera,j%3==0?.095:.045,j%2==0?color:other,pressure);
                if(i==8)QiPressureMesh.ribbon(out,matrix,p,p.add(u.scale(Math.sin(time+j)*1.8)).add(v.scale(Math.cos(time*.3+j)*2)),camera,.027,0xF3FFFF,.8F);
            }
            for(int j=0;j<3;j++) {
                double phase=(time*.055+j/3.0)%1;
                QiPressureMesh.wave(out,matrix,center,normal,camera,radius*(.15+phase*.85),(float)((1-phase)*.65),j%2==0?color:other);
            }
            // Branching contact sparks and thick, counter-rotating stress arcs remain
            // readable from across the battlefield instead of becoming a single thin line.
            for(int j=0;j<(low?20:40);j++) {
                double angle=j*2.399963+time*.045,r=radius*Math.sqrt((j+.5)/40);
                Vec3 point=center.add(u.scale(Math.cos(angle)*r)).add(v.scale(Math.sin(angle)*r));
                double flash=.3+.7*Math.pow(Math.max(0,Math.sin(time*.65+j*1.9)),3);
                Vec3 tail=point.add(normal.scale(Math.sin(time*.23+j)*1.7)).add(u.scale(Math.sin(j)*.7));
                QiPressureMesh.ribbon(out,matrix,point,tail,camera,.045+flash*.035,j%2==0?color:other,(float)flash);
                if(j%4==0)QiPressureMesh.ribbon(out,matrix,point,tail,camera,.018,0xF2FFFF,(float)flash);
            }
            for(int j=0;j<4;j++)for(int i=0;i<20;i++) {
                double angle=time*(j%2==0?.045:-.045)+j*Math.PI/2+i*.055,next=angle+.055,r=radius*(.52+j*.13);
                Vec3 p=center.add(u.scale(Math.cos(angle)*r)).add(v.scale(Math.sin(angle)*r));
                Vec3 q=center.add(u.scale(Math.cos(next)*r)).add(v.scale(Math.sin(next)*r));
                QiPressureMesh.ribbon(out,matrix,p,q,camera,.07,j%2==0?color:other,.8F);
            }
            // Each side discharges back into its own membrane, with travelling coils and recoil waves.
            for(int side:new int[]{-1,1}) {
                int tint=side<0?color:other;
                int arms=low?2:4;
                for(int j=0;j<arms;j++)for(int i=0;i<10;i++) {
                    double angle=j*Math.PI*2/arms+side*time*.025,r=radius*(.27+j*.13);
                    Vec3 root=center.add(u.scale(Math.cos(angle)*r)).add(v.scale(Math.sin(angle)*r));
                    double t=i/10.,next=(i+1)/10.,length=3+radius*.25;
                    Vec3 p=recoil(root,u,v,normal,side,t,length,time,j),q=recoil(root,u,v,normal,side,next,length,time,j);
                    QiPressureMesh.ribbon(out,matrix,p,q,camera,.095,tint,pressure*.70F);
                    QiPressureMesh.ribbon(out,matrix,p,q,camera,.026,0xEFFFFF,pressure*.65F);
                }
                double phase=(time%16)/16.;
                QiPressureMesh.wave(out,matrix,center.add(normal.scale(side*phase*4)),normal,camera,
                        radius*(.15+phase*.80),(float)((1-phase)*.48),tint);
            }
            pressureBurstFrames++;
            if(count==1&&now%36==0)sound(center,false);
            QiDomainDistortion.observeClash(center,1.8F+surge*.65F,(float)Math.min(26,4+radius*1.4),normal);
            contactFrames++;QiAuraRenderer.clashFrames++;
        }
        for(var collapse:COLLAPSES) {
            double age=time-collapse.started;float fade=(float)Math.max(0,1-age/26);
            int color=MethodDomainStyle.values()[collapse.style].color;
            for(int i=0;i<56;i++) {
                Vec3 direction=fibonacci(i,56);double radius=20+age*.16;
                Vec3 p=collapse.center.add(direction.scale(radius));
                Vec3 tangent=direction.cross(new Vec3(0,1,0)).normalize();
                QiPressureMesh.ribbon(out,matrix,p.add(tangent.scale(-.4*fade)),p.add(tangent.scale(.6*fade)).add(direction.scale(age*.045)),camera,.10*fade,color,.95F*fade);
            }
            for(int i=0;i<3;i++)QiPressureMesh.wave(out,matrix,collapse.center.add(0,.15+i*.1,0),new Vec3(0,1,0),camera,16+age*.6,.65F*fade,color);
            QiDomainDistortion.observeClash(collapse.center.add(0,3,0),1.5F*fade,24,new Vec3(0,1,0));shatterFrames++;
        }
    }
    private static Vec3 recoil(Vec3 root,Vec3 u,Vec3 v,Vec3 normal,int side,double t,double length,double time,int seed) {
        double envelope=Math.sin(t*Math.PI),turn=t*11-time*.85+seed*1.7;
        return root.add(normal.scale(side*t*length)).add(u.scale(Math.sin(turn)*envelope*.65))
                .add(v.scale(Math.cos(turn)*envelope*.65));
    }
    private static Vec3 seam(Vec3 center,Vec3 u,Vec3 v,Vec3 normal,double angle,double t,double radius,double time,int seed) {
        double r=(t*2-1)*radius,zig=Math.sin(t*37+Math.floor(time*.45)*2.7+seed)*(.18+Math.sin(t*Math.PI)*.5);
        return center.add(u.scale(Math.cos(angle)*r+zig)).add(v.scale(Math.sin(angle)*r+zig*.65))
                .add(normal.scale(Math.sin(t*19-time*.28+seed)*.22));
    }
    private static Vec3 fibonacci(int index,int count) {
        double y=1-2*(index+.5)/count,r=Math.sqrt(1-y*y),a=index*2.399963;
        return new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);
    }
    private static int mix(int first,int second,double amount) {
        int r=(int)((first>>16&255)*(1-amount)+(second>>16&255)*amount);
        int g=(int)((first>>8&255)*(1-amount)+(second>>8&255)*amount);
        int b=(int)((first&255)*(1-amount)+(second&255)*amount);
        return r<<16|g<<8|b;
    }
    private static void vertex(VertexConsumer out,Matrix4f m,Vec3 point,Vec3 camera,int color,float alpha) {
        Vec3 p=point.subtract(camera);
        out.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha*QiVfxQuality.opacity());
    }
}
