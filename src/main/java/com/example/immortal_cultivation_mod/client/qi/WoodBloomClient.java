package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.entity.TemporaryTerrainEntity;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Veins, bark, leaf bursts and cracks attach only to accepted temporary wood placements. */
public final class WoodBloomClient {
    private record Face(BlockPos block,Direction normal) { }
    private static final class Bloom {
        final long start;final List<BlockPos> blocks;final List<Face> faces=new ArrayList<>();final Set<BlockPos> dissolved=new HashSet<>();
        Bloom(long now,List<BlockPos> blocks) {
            start=now;this.blocks=blocks;var all=new HashSet<>(blocks);
            for(var block:blocks)for(var normal:Direction.values())if(!all.contains(block.relative(normal))&&faces.size()<96)faces.add(new Face(block,normal));
        }
    }
    private static final List<Bloom> BLOOMS=new ArrayList<>();
    private static ClientLevel world;
    public static long bloomEvents,veinFrames,decayEvents;
    private WoodBloomClient() { }
    public static void clear(){BLOOMS.clear();world=null;}
    public static void accept(QiAuraPayloads.WoodBloom packet) {
        var level=Minecraft.getInstance().level;if(world!=level){clear();world=level;}
        if(world==null||!world.dimension().location().equals(packet.dimension())||packet.blocks().isEmpty()
                ||Vec3.atCenterOf(packet.origin()).distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>128*128
                ||packet.blocks().stream().anyMatch(pos->pos.distManhattan(packet.origin())>24))return;
        if(BLOOMS.size()>=8)BLOOMS.removeFirst();var bloom=new Bloom(world.getGameTime(),packet.blocks());BLOOMS.add(bloom);bloomEvents++;
        var leaf=new QiElementLook(0x68AF45,0xD4DC71,QiElementLook.WOOD,.6F);
        for(int i=0;i<12;i++) {
            var cell=bloom.blocks.get(i*bloom.blocks.size()/12);
            Vec3 point=Vec3.atCenterOf(cell).add(0,.5,0);
            QiEnergyParticles.energy(point,new Vec3(world.random.nextGaussian()*.08,.10,world.random.nextGaussian()*.08),leaf,18);
            QiEnergyParticles.debris(point,new Vec3(world.random.nextGaussian()*.07,.06,world.random.nextGaussian()*.07),Blocks.OAK_LOG.defaultBlockState());
        }
    }
    public static void tick() {
        var level=Minecraft.getInstance().level;if(world!=level){clear();world=level;}
        if(world==null)return;long now=world.getGameTime();BLOOMS.removeIf(bloom->now-bloom.start>44);
        for(var bloom:BLOOMS) {
            int phase=(int)(now-bloom.start-30);if(phase<0)continue;
            int emitted=0;
            for(var block:bloom.blocks)if(TemporaryTerrainEntity.woodWitherTick(block)<=phase&&bloom.dissolved.add(block)) {
                decayEvents++;if(emitted++>=8)continue;
                Vec3 point=Vec3.atCenterOf(block);
                QiEnergyParticles.energy(point,new Vec3(world.random.nextGaussian()*.07,.07,world.random.nextGaussian()*.07),
                        new QiElementLook(0x5BC969,0xCBEC9A,QiElementLook.WOOD,.7F),16);
                QiEnergyParticles.debris(point,new Vec3(world.random.nextGaussian()*.06,.02,world.random.nextGaussian()*.06),Blocks.OAK_LOG.defaultBlockState());
            }
        }
    }
    public static void render(PoseStack pose,MultiBufferSource buffers,Vec3 camera,double time) {
        if(world==null)return;var out=buffers.getBuffer(QiAuraMaterial.GLOW);var matrix=pose.last().pose();
        for(var bloom:BLOOMS) {
            double age=time-bloom.start;if(age<0||age>=40)continue;
            float fade=(float)Math.min(1,(40-age)/10);int shown=0;
            for(var face:bloom.faces) {
                if(++shown>(QiVfxQuality.level()==0?32:96))break;
                if(!world.hasChunkAt(face.block)||!world.getBlockState(face.block).is(Blocks.OAK_LOG))continue;
                Vec3 n=Vec3.atLowerCornerOf(face.normal.getNormal()),center=Vec3.atCenterOf(face.block).add(n.scale(.504));
                if(center.distanceToSqr(camera)>48*48)continue;
                veinFrames++;
                Vec3 u=face.normal.getAxis()==Direction.Axis.Y?new Vec3(1,0,0):new Vec3(0,1,0),v=n.cross(u).normalize();
                Vec3 last=center.subtract(v.scale(.42));
                for(int j=1;j<=5;j++) {
                    double t=j/5.0;Vec3 next=center.add(v.scale(t*.84-.42)).add(u.scale(Math.sin(j*2.1+face.block.hashCode())*.08));
                    QiPressureMesh.ribbon(out,matrix,last,next,camera,.012,age<28?0x79D56A:0x3B3026,fade*(age<28?.28F:.55F));last=next;
                }
                if(age>27)QiPressureMesh.ribbon(out,matrix,center.subtract(u.scale(.35)),center.add(u.scale(.35)),camera,.008,0x413020,fade*.45F);
            }
        }
    }
}
