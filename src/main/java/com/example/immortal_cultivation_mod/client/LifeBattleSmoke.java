package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Tall, billowing impact smoke confined to the simulated battlefield. */
public final class LifeBattleSmoke {
    public static final long DURATION_MILLIS = 2200;
    private LifeBattleSmoke() {}

    public static boolean heavy(CompoundTag frame,CompoundTag target) {
        if(!frame.getString("Effect").equals("attack"))return false;
        double raw=0;
        for(var tag:frame.getList("Hits",Tag.TAG_COMPOUND)) {
            var hit=(CompoundTag)tag;if(hit.getString("Target").equals(target.getString("Id")))raw+=hit.getDouble("Raw");
        }
        return raw>0&&(raw>=target.getDouble("MaxHp")*.18
                ||raw>=Math.max(40,target.getDouble("Power")*1.5));
    }

    public static void draw(GuiGraphics g,float x,float floor,float progress,int seed) {
        if(progress<=.38F||progress>=1)return;
        float t=(progress-.38F)/.62F;
        float fade=Math.min(1,t*8)*(1-t*.75F);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.disableDepthTest();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var mesh=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix=g.pose().last().pose();
        // Three rising columns, overlapping soft puffs and a spreading ground cloud.
        for(int i=0;i<36;i++) {
            float level=i/35F,variation=hash(seed+i*17);
            float rise=level*(100+130*t);
            float centerX=x+(i%3-1)*(9+level*10)+(float)Math.sin(level*9+t*4+variation*5)*8;
            float centerY=floor-rise-4;
            float radius=(13+level*26)*(.8F+t*.6F);
            int rgb=i%3==0?0x8B9690:i%3==1?0x687973:0x9AA39A;
            puff(mesh,matrix,centerX,centerY,radius,radius*.8F,rgb,fade*(.60F-level*.14F));
        }
        for(int i=0;i<9;i++)puff(mesh,matrix,x+(i-4)*(4+18*t),floor-5,15+10*t,7+3*t,0x98A59A,fade*.3F);
        BufferUploader.drawWithShader(mesh.buildOrThrow());RenderSystem.enableCull();
    }
    private static void puff(BufferBuilder mesh,Matrix4f matrix,float x,float y,float rx,float ry,int rgb,float opacity) {
        for(int ring=0;ring<3;ring++)for(int i=0;i<24;i++) {
            float inner=ring/3F,outer=(ring+1)/3F;
            double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            int c0=((int)(255*opacity*(1-inner)*(1-inner))<<24)|rgb;
            int c1=((int)(255*opacity*(1-outer)*(1-outer))<<24)|rgb;
            vertex(mesh,matrix,x+(float)Math.cos(a)*rx*inner,y+(float)Math.sin(a)*ry*inner,c0);
            vertex(mesh,matrix,x+(float)Math.cos(a)*rx*outer,y+(float)Math.sin(a)*ry*outer,c1);
            vertex(mesh,matrix,x+(float)Math.cos(b)*rx*outer,y+(float)Math.sin(b)*ry*outer,c1);
            vertex(mesh,matrix,x+(float)Math.cos(a)*rx*inner,y+(float)Math.sin(a)*ry*inner,c0);
            vertex(mesh,matrix,x+(float)Math.cos(b)*rx*outer,y+(float)Math.sin(b)*ry*outer,c1);
            vertex(mesh,matrix,x+(float)Math.cos(b)*rx*inner,y+(float)Math.sin(b)*ry*inner,c0);
        }
    }
    private static float hash(int value){return Math.floorMod(value*1103515245+12345,65536)/65536F;}
    private static void vertex(BufferBuilder mesh,Matrix4f matrix,float x,float y,int color){mesh.addVertex(matrix,x,y,0).setColor(color);}
}
