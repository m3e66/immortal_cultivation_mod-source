package com.example.immortal_cultivation_mod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Actual depth-tested polyhedral dice, with deterministic cosmetic drop/bounce motion.
 * No randomness or gameplay state is generated here: the winning face is the saved server roll.
 */
public final class LifeDiceRenderer {
    private static final Map<Integer,Die> MODELS=new ConcurrentHashMap<>();
    private LifeDiceRenderer() {}

    public static void drawPool(GuiGraphics g,CompoundTag roll,float center,float floor,float radius,
                                long elapsed,long duration,float availableWidth) {
        drawPool(g,roll,center,floor,radius,elapsed,duration,availableWidth,0xFFF7ECD2);
    }
    public static void drawPool(GuiGraphics g,CompoundTag roll,float center,float floor,float radius,
                                long elapsed,long duration,float availableWidth,int captionColor) {
        int sides=Mth.clamp(roll.getInt("Sides"),4,100);
        int[] values=roll.getIntArray("Dice");
        if(values.length==0)values=new int[]{Math.max(1,roll.getInt("Selected"))};
        int count=Math.min(12,values.length);
        float spacing=Math.min(radius*2.9F,availableWidth/Math.max(1,count));
        float size=Math.min(radius,spacing/2.6F);
        for(int i=0;i<count;i++) {
            float x=center+(i-(count-1)/2F)*spacing;
            long delay=duration==0?0:Math.min(35*i,duration/5);
            float t=duration<=0?1:Mth.clamp((elapsed-delay)/(float)Math.max(1,duration-delay),0,1);
            draw(g,MODELS.computeIfAbsent(sides,LifeDiceRenderer::model),sides,values[i],x,floor,size,t,i,captionColor);
        }
    }

    private static void draw(GuiGraphics g,Die die,int sides,int value,float x,float floor,float size,float t,int index,int captionColor) {
        int result=Mth.clamp(value,1,sides);
        Face winning=die.faces.get(result-1);
        float remaining=1-t;
        // A high gravity drop, three distinct shrinking rebounds, then a short settling roll.
        float drop=t<.28F?96*(1-(t/.28F)*(t/.28F)):
                t<.52F?(float)Math.sin((t-.28F)/.24F*Math.PI)*54:
                t<.72F?(float)Math.sin((t-.52F)/.20F*Math.PI)*26:
                t<.86F?(float)Math.sin((t-.72F)/.14F*Math.PI)*10:0;
        float slide=remaining*remaining*((index%2==0?-1:1)*48+(float)Math.sin(t*9+index*1.7)*32);
        Quaternionf rest=new Quaternionf().rotationXYZ(.32F,-.42F,-.08F)
                .mul(new Quaternionf().setFromNormalized(winning.basis()).conjugate());
        Quaternionf rotation=new Quaternionf(rest).rotateXYZ(remaining*remaining*26,
                remaining*remaining*(34+index*.7F),remaining*remaining*22);
        g.flush();
        // Floor shadow changes with die height rather than following the airborne die.
        g.pose().pushPose();g.pose().translate(x+slide,floor+size*.65F,0);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();
        var shadow=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix=g.pose().last().pose();
        float shadowRadius=size*(1-.45F*drop/96),opacity=.28F*(1-.65F*drop/96);
        for(int i=0;i<32;i++) {
            double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
            vertex(shadow,matrix,0,0,0,((int)(255*opacity)<<24)|0x071016);
            vertex(shadow,matrix,(float)Math.cos(a)*shadowRadius,(float)Math.sin(a)*shadowRadius*.3F,0,0x00071016);
            vertex(shadow,matrix,(float)Math.cos(b)*shadowRadius,(float)Math.sin(b)*shadowRadius*.3F,0,0x00071016);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);BufferUploader.drawWithShader(shadow.buildOrThrow());
        g.pose().popPose();
        g.pose().pushPose();g.pose().translate(x+slide,floor-drop,150);g.pose().scale(size,size,size);g.pose().mulPose(rotation);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);RenderSystem.disableCull();
        var mesh=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        matrix=g.pose().last().pose();
        int material=sides==4?0x5597BA:sides==6?0xAD82C5:sides==8?0xD8A15B:sides==20?0x639F8C:0x789AAD;
        Vector3f light=new Vector3f(-.4F,-.6F,1).normalize();
        for(Face face:die.faces) {
            float shade=.48F+.52F*Math.max(0,new Vector3f(face.normal).rotate(rotation).dot(light));
            int color=shade(material,shade),edge=shade(material,shade*.45F);
            for(int i=0;i<face.vertices.length;i++) {
                Vector3f a=face.vertices[i],b=face.vertices[(i+1)%face.vertices.length];
                triangle(mesh,matrix,face.center,a,b,edge);
                // Lift the inset surface off its border plane to avoid depth fighting.
                triangle(mesh,matrix,new Vector3f(face.center).fma(.003F,face.normal),
                        new Vector3f(a).lerp(face.center,.065F).fma(.003F,face.normal),
                        new Vector3f(b).lerp(face.center,.065F).fma(.003F,face.normal),color);
            }
        }
        BufferUploader.drawWithShader(mesh.buildOrThrow());
        var font=Minecraft.getInstance().font;
        for(int i=0;i<die.faces.size();i++) {
            Face face=die.faces.get(i);
            if(new Vector3f(face.normal).rotate(rotation).z<.08F)continue;
            g.pose().pushPose();
            Vector3f position=new Vector3f(face.center).fma(.012F,face.normal);
            g.pose().translate(position.x,position.y,position.z);
            g.pose().mulPose(new Matrix4f(face.basis()));
            if(sides==6) pips(g,i+1,Math.min(.32F,face.width*.28F));
            else {
                String number=Integer.toString(i+1);
                float scale=Math.min(.12F,face.width*.58F/Math.max(font.width(number),font.lineHeight));
                g.pose().scale(scale,scale,scale);
                // GuiGraphics.flush disables depth testing; face labels need normal 3D occlusion.
                font.drawInBatch(number,-font.width(number)/2F,-font.lineHeight/2F,0xFFFFF7DF,false,
                        g.pose().last().pose(),g.bufferSource(),Font.DisplayMode.NORMAL,0,15728880);
                g.bufferSource().endBatch();RenderSystem.enableDepthTest();RenderSystem.depthMask(true);
            }
            g.pose().popPose();
        }
        g.pose().popPose();g.flush();RenderSystem.enableCull();RenderSystem.disableDepthTest();
        // Large pools and custom d100 remain legible even when individual face labels are tiny.
        if(t>=.97F)g.drawString(font,Integer.toString(result),(int)x-font.width(Integer.toString(result))/2,
                (int)(floor+size+5),captionColor,false);
    }

    private static void pips(GuiGraphics g,int value,float spacing) {
        int[][] positions=switch(value) {
            case 1->new int[][]{{0,0}};
            case 2->new int[][]{{-1,-1},{1,1}};
            case 3->new int[][]{{-1,-1},{0,0},{1,1}};
            case 4->new int[][]{{-1,-1},{1,-1},{-1,1},{1,1}};
            case 5->new int[][]{{-1,-1},{1,-1},{0,0},{-1,1},{1,1}};
            default->new int[][]{{-1,-1},{1,-1},{-1,0},{1,0},{-1,1},{1,1}};
        };
        g.flush();var mesh=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix=g.pose().last().pose();
        for(int[] p:positions)for(int i=0;i<12;i++) {
            float x=p[0]*spacing,y=p[1]*spacing,r=.065F;
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
            vertex(mesh,matrix,x,y,.002F,0xFFFFF5DD);
            vertex(mesh,matrix,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,.002F,0xFFFFF5DD);
            vertex(mesh,matrix,x+(float)Math.cos(b)*r,y+(float)Math.sin(b)*r,.002F,0xFFFFF5DD);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);BufferUploader.drawWithShader(mesh.buildOrThrow());
    }

    private static Die model(int sides) {
        if(sides==4)return hull(List.of(new Vector3f(1,1,1),new Vector3f(1,-1,-1),new Vector3f(-1,1,-1),new Vector3f(-1,-1,1)));
        if(sides==6) {
            List<Vector3f> vertices=new ArrayList<>();
            for(int x:new int[]{-1,1})for(int y:new int[]{-1,1})for(int z:new int[]{-1,1})vertices.add(new Vector3f(x,y,z));
            return hull(vertices);
        }
        if(sides==8)return hull(List.of(new Vector3f(1,0,0),new Vector3f(-1,0,0),new Vector3f(0,1,0),new Vector3f(0,-1,0),new Vector3f(0,0,1),new Vector3f(0,0,-1)));
        if(sides==20||sides==12) {
            float golden=(1+(float)Math.sqrt(5))/2;
            List<Vector3f> vertices=new ArrayList<>();
            for(int a:new int[]{-1,1})for(int b:new int[]{-1,1}) {
                vertices.add(new Vector3f(0,a,b*golden));vertices.add(new Vector3f(a,b*golden,0));vertices.add(new Vector3f(b*golden,0,a));
            }
            Die icosa=hull(vertices);
            return sides==20?icosa:hull(icosa.faces.stream().map(f->new Vector3f(f.center)).toList());
        }
        // Dual pentagonal antiprism gives the ten kite-shaped faces of a real d10.
        if(sides==10) {
            List<Vector3f> ring=new ArrayList<>();
            for(int i=0;i<10;i++){double a=i*Math.PI/5;ring.add(new Vector3f((float)Math.cos(a),i%2==0?.38F:-.38F,(float)Math.sin(a)));}
            Die anti=hull(ring);
            return hull(anti.faces.stream().map(f->{Vector3f point=new Vector3f(f.normal).div(f.normal.dot(f.center));point.y*=.52F;return point;}).toList());
        }
        List<Face> faces=new ArrayList<>();int count=sides%2==0?sides/2:sides-1;
        Vector3f top=new Vector3f(0,-1,0),bottom=new Vector3f(0,1,0);
        Vector3f[] ring=new Vector3f[count];
        for(int i=0;i<count;i++){double a=i*Math.PI*2/count;ring[i]=new Vector3f((float)Math.cos(a),0,(float)Math.sin(a));}
        for(int i=0;i<count;i++){faces.add(new Face(new Vector3f[]{top,ring[i],ring[(i+1)%count]}));if(sides%2==0)faces.add(new Face(new Vector3f[]{bottom,ring[(i+1)%count],ring[i]}));}
        if(sides%2!=0) {
            Vector3f[] base=new Vector3f[count];
            for(int i=0;i<count;i++)base[i]=ring[count-1-i];
            faces.add(new Face(base));
        }
        return new Die(faces);
    }

    /** Merge coplanar hull triangles into polygonal faces, cached once for each dice size. */
    private static Die hull(List<Vector3f> input) {
        List<Vector3f> points=input.stream().map(Vector3f::new).toList();
        float radius=points.stream().map(Vector3f::length).max(Float::compare).orElse(1F);
        points.forEach(v->v.div(radius));List<Face> faces=new ArrayList<>();List<String> seen=new ArrayList<>();
        for(int i=0;i<points.size();i++)for(int j=i+1;j<points.size();j++)for(int k=j+1;k<points.size();k++) {
            Vector3f a=points.get(i),normal=new Vector3f(points.get(j)).sub(a).cross(new Vector3f(points.get(k)).sub(a));
            if(normal.lengthSquared()<.000001F)continue;normal.normalize();float plane=normal.dot(a);boolean positive=false,negative=false;
            for(Vector3f p:points){float distance=normal.dot(p)-plane;positive|=distance>.0001F;negative|=distance<-.0001F;}
            if(positive&&negative)continue;if(plane<0){normal.negate();plane=-plane;}
            List<Integer> indices=new ArrayList<>();Vector3f center=new Vector3f();
            for(int n=0;n<points.size();n++)if(Math.abs(normal.dot(points.get(n))-plane)<.0001F){indices.add(n);center.add(points.get(n));}
            String key=indices.toString();if(seen.contains(key))continue;seen.add(key);center.div(indices.size());
            Vector3f u=new Vector3f(points.get(indices.getFirst())).sub(center).normalize(),v=new Vector3f(normal).cross(u);
            indices.sort(java.util.Comparator.comparingDouble(n->{Vector3f relative=new Vector3f(points.get(n)).sub(center);return Math.atan2(relative.dot(v),relative.dot(u));}));
            faces.add(new Face(indices.stream().map(points::get).toArray(Vector3f[]::new)));
        }
        return new Die(faces);
    }
    private record Die(List<Face> faces) {}
    private static final class Face {
        final Vector3f[] vertices;final Vector3f center=new Vector3f(),normal,u,v;final float width;
        Face(Vector3f[] points) {
            vertices=points;for(Vector3f p:points)center.add(p);center.div(points.length);
            normal=new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0])).normalize();
            if(normal.dot(center)<0)normal.negate();
            u=new Vector3f(points[1]).sub(points[0]).normalize();v=new Vector3f(normal).cross(u);
            float smallest=Float.MAX_VALUE;for(int i=0;i<points.length;i++)smallest=Math.min(smallest,points[i].distance(points[(i+1)%points.length]));width=smallest;
        }
        Matrix3f basis(){return new Matrix3f(u.x,u.y,u.z,v.x,v.y,v.z,normal.x,normal.y,normal.z);}
    }
    private static int shade(int color,float value){return 0xFF000000|((int)((color>>16&255)*value)<<16)|((int)((color>>8&255)*value)<<8)|(int)((color&255)*value);}
    private static void vertex(BufferBuilder b,Matrix4f matrix,float x,float y,float z,int color){b.addVertex(matrix,x,y,z).setColor(color);}
    private static void triangle(BufferBuilder b,Matrix4f matrix,Vector3f a,Vector3f c,Vector3f d,int color){vertex(b,matrix,a.x,a.y,a.z,color);vertex(b,matrix,c.x,c.y,c.z,color);vertex(b,matrix,d.x,d.y,d.z,color);}
}
