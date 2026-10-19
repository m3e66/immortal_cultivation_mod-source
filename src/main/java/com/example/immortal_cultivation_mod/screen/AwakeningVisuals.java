package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Native animated geometry based on awakening_ten_elements_physique_v2.mp4. */
final class AwakeningVisuals {
    static final float ROOT_REVEAL = 9.6F, BODY_REVEAL = 9.2F;
    private static final int GOLD = 0xD8BD72, JADE = 0x69CAB4;
    private static final List<String> ELEMENTS = List.of(SpiritRoots.METAL, SpiritRoots.WOOD,
            SpiritRoots.WATER, SpiritRoots.FIRE, SpiritRoots.EARTH, SpiritRoots.ICE,
            SpiritRoots.THUNDER, SpiritRoots.WIND, SpiritRoots.YANG, SpiritRoots.YIN);
    private static final int[] COLORS = {0xEDCF7F,0x75DAB0,0x66C8EE,0xEF9A77,0xD7BC7A,
            0xABEDFF,0xB1A3F7,0x83D8C4,0xF5E7A1,0xAE83E4};
    private static final float[] ANGLES = {-126,-54,18,90,162,-18,-90,54,126,-162};

    private static final float[][][] CHANNELS = AwakeningBodyTrace.CHANNELS;

    private AwakeningVisuals() {}

    static float scale(int width, int height) { return Math.min(width / 1280F, height / 720F); }
    static int screenX(int width, int height, float x) { return Math.round(width / 2F + (x - 640) * scale(width, height)); }
    static int screenY(int width, int height, float y) { return Math.round(height / 2F + (y - 360) * scale(width, height)); }

    static void roots(GuiGraphics g,int width,int height,float time,List<String> roots,String grade) {
        begin(g,width,height);
        Mesh m=new Mesh(g);
        float settle=smooth((time-8.7F)/.9F);
        rings(m,time,1);
        m.glow(640,360,130,alpha(JADE,.72F));
        m.glow(640,360,38,alpha(JADE,.8F));
        m.ring(640,360,28,0,Math.PI*2,80,.8F,alpha(JADE,.9F));
        for(int i=0;i<10;i++) {
            double angle=Math.toRadians(ANGLES[i]);
            float x=640+(float)Math.cos(angle)*200,y=360+(float)Math.sin(angle)*200;
            float scan=smooth((time-.5F-i*.78F)/.3F);
            boolean owned=roots.contains(ELEMENTS.get(i));
            float energy=scan*(1-settle+(owned?settle:0));
            float pulse=(float)Math.exp(-Math.pow((time-.8F-i*.78F)*2.2,2));
            int c=COLORS[i];
            m.line(640,360,x,y,.7F,alpha(c,.06F+.12F*energy));
            m.glow(x,y,68+30*pulse,alpha(c,.05F+energy*.65F+pulse*.4F));
            m.glow(x,y,26,alpha(c,energy*.45F));
            m.ring(x,y,22,0,Math.PI*2,64,2.2F,alpha(c,.13F+.85F*energy));
            if(pulse>.02F) m.ring(x,y,28+pulse*22,time*.4,Math.PI*2,64,1,alpha(c,pulse*.55F));
            for(int ray=0;ray<8;ray++) {
                double a=ray*Math.PI/4+time*(i%2==0?.12:-.12);
                float r=28+5*(float)Math.sin(time+i+ray);
                float xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;
                if(i==1||i==7) m.ring(xx,yy,3,a,Math.PI*2,16,.6F,alpha(c,.6F*energy));
                else if(i==4) m.diamond(xx,yy,3,alpha(c,.65F*energy));
                else if(i==6) {
                    m.line(xx,yy,xx+(float)Math.cos(a+.4)*9,yy+(float)Math.sin(a+.4)*9,1,alpha(c,energy*.8F));
                    m.line(xx+(float)Math.cos(a+.4)*9,yy+(float)Math.sin(a+.4)*9,xx+(float)Math.cos(a)*15,yy+(float)Math.sin(a)*15,1,alpha(c,energy*.8F));
                } else m.line(xx,yy,x+(float)Math.cos(a)*(r+9),y+(float)Math.sin(a)*(r+9),1,alpha(c,energy*.6F));
            }
        }
        m.finish();
        for(int i=0;i<10;i++) {
            double angle=Math.toRadians(ANGLES[i]);
            float energy=smooth((time-.5F-i*.78F)/.3F)*(1-settle+(roots.contains(ELEMENTS.get(i))?settle:0));
            // Element glyphs live inside their seals, not in surrounding caption panels.
            text(g,Component.literal(ELEMENTS.get(i)),640+(float)Math.cos(angle)*200,
                    348+(float)Math.sin(angle)*200,2.5F,alpha(COLORS[i],.15F+.85F*energy));
        }
        if(settle>.8F) {
            boolean absent=roots.isEmpty()||roots.contains(SpiritRoots.NONE);
            text(g,absent?SpiritRoots.elementDisplayName(SpiritRoots.NONE):SpiritRoots.elementsDisplayName(roots),640,348,1.5F,0xFFECE6C8);
            if(!absent) text(g,SpiritRoots.gradeDisplayName(grade),640,374,1,0xFFC9B273);
        }
        end(g);
    }

    static void body(GuiGraphics g,int width,int height,float time,List<String> roots,String body) {
        begin(g,width,height);Mesh m=new Mesh(g);
        int color=bodyColor(body);
        float reveal=smooth((time-7.5F)/1.7F),outline=smooth(time/1.4F);
        rings(m,time,.58F);
        m.glow(640,365,210,alpha(color,.06F+.12F*reveal));
        silhouette(m,640,360,1,alpha(GOLD,.18F+.63F*outline));
        for(int i=0;i<5;i++) {
            float y=218+i*65;
            m.ring(640,y,150, time*.25+i*.7,Math.PI*1.6,90,.9F,alpha(GOLD,.10F+.30F*smooth((time-i*.6F)/2)) ,.26F);
        }
        for(int i=0;i<CHANNELS.length;i++) {
            float progress=smooth((time-1.1F-i*.37F)/2.5F);
            float[][] path=CHANNELS[i];
            int rootIndex = roots.isEmpty() ? -1 : ELEMENTS.indexOf(roots.get(i % roots.size()));
            int channelColor = rootIndex < 0 ? color : COLORS[rootIndex];
            for(int segment=0;segment<path.length-1;segment++) {
                float local=Mth.clamp(progress*(path.length-1)-segment,0,1);
                if(local<=0)continue;
                float x=Mth.lerp(local,path[segment][0],path[segment+1][0]);
                float y=Mth.lerp(local,path[segment][1],path[segment+1][1]);
                m.line(path[segment][0],path[segment][1],x,y,6,alpha(channelColor,.09F));
                m.line(path[segment][0],path[segment][1],x,y,1.8F,alpha(channelColor,.65F+.25F*reveal));
            }
            // Dense curves need one moving light, not a radial mesh at every sample.
            if(progress>0) {
                float position=progress*(path.length-1);
                int segment=Math.min((int)position,path.length-2);
                float local=position-segment;
                float x=Mth.lerp(local,path[segment][0],path[segment+1][0]);
                float y=Mth.lerp(local,path[segment][1],path[segment+1][1]);
                m.glow(x,y,10,alpha(channelColor,.40F));
                if(progress<1) m.disc(x,y,2,0xFFF3EAC1);
            }
        }
        float core=smooth((time-.5F)/2);
        m.glow(AwakeningBodyTrace.CORE_X,AwakeningBodyTrace.CORE_Y,90,alpha(color,.85F*core));
        m.glow(AwakeningBodyTrace.CORE_X,AwakeningBodyTrace.CORE_Y,34,alpha(0xFFE4A0,.95F*core));
        m.glow(AwakeningBodyTrace.CORE_X,AwakeningBodyTrace.CORE_Y,12,alpha(0xFFF8DF,core));
        m.ring(AwakeningBodyTrace.CORE_X,AwakeningBodyTrace.CORE_Y,14,0,Math.PI*2,80,1.5F,alpha(0xEAE2B4,core));
        m.ring(AwakeningBodyTrace.CORE_X,AwakeningBodyTrace.CORE_Y,7,0,Math.PI*2,48,1,alpha(color,core));
        for(int i=0;i<18;i++) {
            float a=i*(float)Math.PI*2/18+time*.2F,r=20+reveal*8;
            m.line(AwakeningBodyTrace.CORE_X+(float)Math.cos(a)*r,AwakeningBodyTrace.CORE_Y+(float)Math.sin(a)*r,
                    AwakeningBodyTrace.CORE_X+(float)Math.cos(a)*(r+5),AwakeningBodyTrace.CORE_Y+(float)Math.sin(a)*(r+5),1,alpha(GOLD,core*.45F));
        }
        m.finish();
        if(reveal>.85F) text(g,BodyTypes.displayName(body),640,414,1.6F,0xFFF4E5BD);
        end(g);
    }

    static void gender(GuiGraphics g,int width,int height,String selected) {
        begin(g,width,height);
        Mesh m=new Mesh(g);
        float time=(net.minecraft.Util.getMillis()%100_000L)/1000F;
        float pulse=.9F+.1F*(float)Math.sin(time*2.4F);
        for(int i=0;i<2;i++) {
            boolean chosen=(i==0?"male":"female").equals(selected);
            int c=i==0?0x54C7FF:0xFF77DC;
            float x=i==0?520:760, y=340;
            m.glow(x,y,96,alpha(c,(chosen?.48F:.20F)*pulse));
            m.glow(x,y,32,alpha(c,(chosen?.55F:.27F)*pulse));
            m.ring(x,y,44,0,Math.PI*2,96,chosen?2.5F:1.4F,alpha(c,chosen?1:.7F));
            int glyph=alpha(c,chosen?1:.9F);
            // The full symbol bounds are symmetric about the circle's centre.
            if(i==0) {
                m.ring(x-8,y+8,12,0,Math.PI*2,48,2.2F,glyph);
                m.line(x+.5F,y-.5F,x+20,y-20,2.2F,glyph);
                m.line(x+20,y-20,x+6,y-20,2.2F,glyph);
                m.line(x+20,y-20,x+20,y-6,2.2F,glyph);
            } else {
                m.ring(x,y-10,12,0,Math.PI*2,48,2.2F,glyph);
                m.line(x,y+2,x,y+22,2.2F,glyph);
                m.line(x-12,y+12,x+12,y+12,2.2F,glyph);
            }
        }
        m.finish();end(g);
    }

    private static void rings(Mesh m,float time,float opacity) {
        float intro=smooth(time/.8F)*opacity;
        for(int i=0;i<3;i++) {
            float r=214-i*39;
            double angle=time*(i%2==0?.065:-.085)*(i+1);
            int color=alpha(i%2==0?GOLD:JADE,(.40F-i*.05F)*intro);
            m.ring(640,360,r,angle,Math.PI*2,140,.95F,color);
            for(int j=0;j<9-i;j++) {
                double a=angle+j*Math.PI*2/(9-i),b=angle+(j+1)*Math.PI*2/(9-i);
                m.line(640+(float)Math.cos(a)*r,360+(float)Math.sin(a)*r,
                        640+(float)Math.cos(b)*r,360+(float)Math.sin(b)*r,.75F,color);
            }
            for(int j=0;j<36;j++) {
                double a=angle+j*Math.PI*2/36;
                m.line(640+(float)Math.cos(a)*(r+4),360+(float)Math.sin(a)*(r+4),
                        640+(float)Math.cos(a)*(r+(j%3==0?11:7)),360+(float)Math.sin(a)*(r+(j%3==0?11:7)),.8F,color);
            }
        }
        for(int i=0;i<180;i++) {
            double angle=hash(i*7+17)*Math.PI*2+time*(i%2==0?.04:-.03);
            float r=70+hash(i*7+19)*175;
            float pulse=.5F+.5F*(float)Math.sin(time*.9+i);
            m.diamond(640+(float)Math.cos(angle)*r,360+(float)Math.sin(angle)*r,1.1F,
                    alpha(COLORS[i%10],intro*(.15F+.25F*pulse)));
        }
    }

    private static void silhouette(Mesh m,float cx,float cy,float s,int outline) {
        for(float[][] path:AwakeningBodyTrace.OUTLINES) {
            for(int i=1;i<path.length;i++) {
                float ax=cx+(path[i-1][0]-640)*s,ay=cy+(path[i-1][1]-360)*s;
                float bx=cx+(path[i][0]-640)*s,by=cy+(path[i][1]-360)*s;
                m.line(ax,ay,bx,by,3.5F*s,(outline&0xFFFFFF)|0x17000000);
                m.line(ax,ay,bx,by,1.3F*s,outline);
            }
        }
    }

    private static int bodyColor(String body) {
        return switch(BodyTypes.sanitize(body)) {
            case BodyTypes.CRIMSON_FLAME,BodyTypes.BLOOD_FIEND -> 0xECA18D;
            case BodyTypes.PROFOUND_ICE,BodyTypes.VAST_SEA -> 0x8DD9EB;
            case BodyTypes.VIOLET_THUNDER,BodyTypes.VOID_BODY,BodyTypes.OUTER_DEMON_BODY,BodyTypes.SUPREME_YIN -> 0xB8A2E4;
            case BodyTypes.GOLDEN_BODY,BodyTypes.GENG_METAL,BodyTypes.ANCIENT_SACRED,BodyTypes.SOLAR_DIVINE -> 0xEDD69B;
            default -> JADE;
        };
    }
    private static void begin(GuiGraphics g,int width,int height) {
        // Every phase owns the full viewport, including letterbox space on wide/tall windows.
        g.fill(0,0,width,height,0xFF000000);
        float s=scale(width,height);g.pose().pushPose();g.pose().translate(width/2F,height/2F,0);g.pose().scale(s,s,1);g.pose().translate(-640,-360,0);
    }
    private static void end(GuiGraphics g) {g.pose().popPose();}
    private static float hash(int input) {int x=input*0x45D9F3B;x=(x^(x>>>16))*0x45D9F3B;return ((x^(x>>>16))&0xFFFFFF)/(float)0x1000000;}
    private static float smooth(float value) {float t=Mth.clamp(value,0,1);return t*t*(3-2*t);}
    private static int alpha(int color,float opacity) {return (Mth.clamp((int)(opacity*255),0,255)<<24)|(color&0xFFFFFF);}
    private static void text(GuiGraphics g,Component label,float x,float y,float scale,int color) {
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        var font=Minecraft.getInstance().font;g.drawString(font,label,-font.width(label)/2,0,color,false);g.pose().popPose();
    }

    static final class IconButton extends Button {
        private final boolean refresh;
        private final int baseColor;
        private final java.util.function.IntSupplier remaining;
        IconButton(int x,int y,int size,Component narration,OnPress action,boolean refresh,java.util.function.IntSupplier remaining) {
            this(x,y,size,narration,action,refresh,remaining,GOLD);
        }
        IconButton(int x,int y,int size,Component narration,OnPress action,boolean refresh,java.util.function.IntSupplier remaining,int baseColor) {
            super(x,y,size,size,narration,action,DEFAULT_NARRATION);this.refresh=refresh;this.remaining=remaining;this.baseColor=baseColor;
        }
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
            Mesh m=new Mesh(g);float cx=getX()+width/2F,cy=getY()+height/2F,r=width/2F-2;
            int color=alpha(baseColor,active?(isHoveredOrFocused()?1:.7F):.25F);
            m.glow(cx,cy,r+12,alpha(baseColor,active?(isHoveredOrFocused()?.5F:.28F):.035F));
            m.ring(cx,cy,r,0,Math.PI*2,64,1,color);
            float s=width/48F;
            if(refresh) {
                m.ring(cx,cy,10*s,-2.6,4.8,48,1.5F*s,color);
                m.triangle(cx-7*s,cy-9*s,cx-12*s,cy-6*s,cx-5*s,cy-3*s,color,color,color);
                for(int i=0;i<3;i++) m.disc(cx+(i-1)*7*s,cy+r+7*s,1.5F*s,alpha(GOLD,i<remaining.getAsInt()?.8F:.15F));
            } else {
                m.line(cx-10*s,cy,cx+10*s,cy,1.5F*s,color);
                m.line(cx+10*s,cy,cx+3*s,cy-7*s,1.5F*s,color);m.line(cx+10*s,cy,cx+3*s,cy+7*s,1.5F*s,color);
            }
            m.finish();
        }
    }

    private static final class Mesh {
        final BufferBuilder b;final Matrix4f matrix;
        Mesh(GuiGraphics g) {g.flush();RenderSystem.enableBlend();RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionColorShader);b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);matrix=g.pose().last().pose();}
        void vertex(float x,float y,int color) {b.addVertex(matrix,x,y,0).setColor(color);}
        void triangle(float ax,float ay,float bx,float by,float cx,float cy,int a,int d,int e) {vertex(ax,ay,a);vertex(bx,by,d);vertex(cx,cy,e);}
        void line(float x1,float y1,float x2,float y2,float w,int color) {
            float opacity=(color>>>24)/255F;
            if(opacity<=0)return;
            // Wide, soft colored light surrounds a narrow hot core. Additive blending makes
            // overlapping light brighten instead of painting opaque bands over the artwork.
            stroke(x1,y1,x2,y2,w+11,alpha(color,opacity*.075F));
            stroke(x1,y1,x2,y2,w+4,alpha(color,opacity*.24F));
            stroke(x1,y1,x2,y2,w,color);
            int hot=0;
            for(int shift=16;shift>=0;shift-=8)hot|=(int)Mth.lerp(.68F,(color>>>shift)&255,255)<<shift;
            stroke(x1,y1,x2,y2,Math.max(.35F,w*.36F),alpha(hot,opacity*.85F));
        }
        private void stroke(float x1,float y1,float x2,float y2,float w,int color) {
            float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<.001)return;
            float ox=-dy/len*(w/2+.6F),oy=dx/len*(w/2+.6F);
            int edge=color&0xFFFFFF;
            // Vertex-alpha edges remain smooth when the canvas is scaled below a pixel.
            triangle(x1+ox,y1+oy,x2+ox,y2+oy,x2,y2,edge,edge,color);
            triangle(x1+ox,y1+oy,x2,y2,x1,y1,edge,color,color);
            triangle(x1,y1,x2,y2,x2-ox,y2-oy,color,color,edge);
            triangle(x1,y1,x2-ox,y2-oy,x1-ox,y1-oy,color,edge,edge);
        }
        void diamond(float x,float y,float r,int color) {triangle(x-r,y,x,y-r,x+r,y,color,color,color);triangle(x-r,y,x+r,y,x,y+r,color,color,color);}
        void ring(float x,float y,float r,double start,double sweep,int segments,float w,int color) {ring(x,y,r,start,sweep,segments,w,color,1);}
        void ring(float x,float y,float r,double start,double sweep,int segments,float w,int color,float aspect) {
            for(int i=0;i<segments;i++) {double a=start+sweep*i/segments,c=start+sweep*(i+1)/segments;line(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r*aspect,x+(float)Math.cos(c)*r,y+(float)Math.sin(c)*r*aspect,w,color);}
        }
        void glow(float x,float y,float r,int color) {
            // Smooth radial vertex-alpha falloff rather than opaque circles or square particles.
            for(int ring=0;ring<10;ring++) for(int i=0;i<48;i++) {
                float r0=r*ring/10,r1=r*(ring+1)/10;
                double a=i*Math.PI*2/48,c=(i+1)*Math.PI*2/48;
                float a0=(1-ring/10F)*(1-ring/10F),a1=(1-(ring+1)/10F)*(1-(ring+1)/10F);
                int c0=alpha(color,((color>>>24)/255F)*a0),c1=alpha(color,((color>>>24)/255F)*a1);
                triangle(x+(float)Math.cos(a)*r0,y+(float)Math.sin(a)*r0,x+(float)Math.cos(a)*r1,y+(float)Math.sin(a)*r1,x+(float)Math.cos(c)*r1,y+(float)Math.sin(c)*r1,c0,c1,c1);
                triangle(x+(float)Math.cos(a)*r0,y+(float)Math.sin(a)*r0,x+(float)Math.cos(c)*r1,y+(float)Math.sin(c)*r1,x+(float)Math.cos(c)*r0,y+(float)Math.sin(c)*r0,c0,c1,c0);
            }
        }
        void disc(float x,float y,float r,int color) {for(int i=0;i<32;i++) {double a=i*Math.PI*2/32,c=(i+1)*Math.PI*2/32;triangle(x,y,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,x+(float)Math.cos(c)*r,y+(float)Math.sin(c)*r,color,color,color);}}
        void finish() {BufferUploader.drawWithShader(b.buildOrThrow());RenderSystem.defaultBlendFunc();RenderSystem.enableCull();RenderSystem.disableBlend();}
    }
}
