package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.shentong.ShentongActivations;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;

/** Short transparent brush bursts; all geometry and timing are client-only and bounded. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ShentongCalligraphyOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation HUD_ID=id("shentong_calligraphy");
    public static final ShentongCalligraphyOverlay INSTANCE=new ShentongCalligraphyOverlay();
    public static final int DURATION_TICKS=24;
    private static final ResourceLocation ATLAS=id("textures/gui/shentong/calligraphy.png");
    private static final Map<String,String> NAMES=new HashMap<>();
    private static final Map<Integer,Integer> GLYPHS=new HashMap<>();
    private static int columns=16,rows=14;
    private static ShaderInstance shader;
    private static Burst burst;
    private record Burst(UUID caster,String id,String title,int[] glyphs,ClientLevel world,long start,int seed) {}
    public static long activations,renderedFrames,glyphDraws;
    public static String lastTitle="";
    public static UUID lastCaster;
    public static float lastAge,lastWidthFraction,lastOpacity;
    private ShentongCalligraphyOverlay() {}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,path);}

    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("shentong_calligraphy"),DefaultVertexFormat.POSITION_TEX_COLOR),s->shader=s);
    }
    @SubscribeEvent public static void resources(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)ShentongCalligraphyOverlay::reload);
    }
    private static void reload(ResourceManager resources) {
        burst=null;NAMES.clear();GLYPHS.clear();
        try(var reader=new InputStreamReader(resources.getResourceOrThrow(id("shentong_calligraphy.json")).open(),StandardCharsets.UTF_8)) {
            var root=JsonParser.parseReader(reader).getAsJsonObject();columns=root.get("columns").getAsInt();rows=root.get("rows").getAsInt();
            int[] chars=root.get("chars").getAsString().codePoints().toArray();for(int i=0;i<chars.length;i++)GLYPHS.put(chars[i],i);
            root.getAsJsonObject("names").entrySet().forEach(e->NAMES.put(e.getKey(),e.getValue().getAsString()));
        }catch(IOException|RuntimeException error){com.mojang.logging.LogUtils.getLogger().error("Cannot load shentong calligraphy",error);}
    }
    public static boolean ready(){return shader!=null&&!GLYPHS.isEmpty();}
    public static boolean active(){return burst!=null&&burst.world==Minecraft.getInstance().level&&Minecraft.getInstance().level!=null
            &&Minecraft.getInstance().level.getGameTime()-burst.start<DURATION_TICKS;}
    public static boolean supports(String id){
        var spell=ModSpells.get(ModSpells.normalizeId(id));String title=NAMES.get(spell==null?id:spell.translationKey());
        return title!=null&&title.codePoints().allMatch(GLYPHS::containsKey);
    }
    public static void activate(UUID casterId,String abilityId) {
        var mc=Minecraft.getInstance();abilityId=ModSpells.normalizeId(abilityId);
        if(mc.player==null||mc.level==null||!ShentongActivations.isAbility(abilityId)||!supports(abilityId))return;
        long now=mc.level.getGameTime();if(burst!=null&&burst.world==mc.level&&burst.caster.equals(casterId)&&burst.id.equals(abilityId)&&now-burst.start<3)return;
        var spell=ModSpells.get(abilityId);String title=NAMES.get(spell==null?abilityId:spell.translationKey());
        burst=new Burst(casterId,abilityId,title,title.codePoints().map(GLYPHS::get).toArray(),mc.level,now,abilityId.hashCode()^casterId.hashCode()^(int)now);
        lastTitle=title;lastCaster=casterId;activations++;
    }
    @Override public void render(GuiGraphics g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();if(burst==null)return;
        if(mc.level!=burst.world||mc.player==null){burst=null;return;}
        float age=(float)(mc.level.getGameTime()-burst.start)+delta.getGameTimeDeltaPartialTick(false);
        if(age>=DURATION_TICKS){burst=null;return;}
        if(mc.options.hideGui||!ready())return;
        int count=burst.glyphs.length;if(count==0)return;
        int perRow=Math.min(8,count),lines=(count+perRow-1)/perRow;
        float size=Math.min(g.guiWidth()*.91F/(perRow*.86F+.14F),g.guiHeight()*.42F/lines);
        float width=size*(perRow*.86F+.14F),cx=g.guiWidth()*.5F,cy=g.guiHeight()*.43F;
        float opacity=1-smooth((age-13)/11),impact=(float)Math.exp(-age*.48);
        lastAge=age;lastOpacity=opacity;lastWidthFraction=width/g.guiWidth();renderedFrames++;
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        drawSplashes(g,cx,cy,width,size,age,opacity,burst.seed);
        RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,ATLAS);
        float stagger=Math.min(.85F,7F/Math.max(1,count-1));
        for(int i=0;i<count;i++) {
            float local=age-i*stagger;if(local<=0)continue;
            float reveal=Math.min(1,local/5),slam=1-smooth(local/3);
            int row=i/perRow,col=i%perRow,rowCount=Math.min(perRow,count-row*perRow);
            float x=cx+(col-(rowCount-1)*.5F)*size*.86F;
            float y=cy+(row-(lines-1)*.5F)*size*.94F;
            float tilt=-4+(hash(burst.seed+i*97)-.5F)*5;
            g.pose().pushPose();g.pose().translate(x-22*slam,y-16*slam+(hash(i+47)-.5F)*size*.055F,300);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tilt+slam*-7));
            float scale=1+slam*.22F+impact*.025F;g.pose().scale(scale,scale,1);
            int glyph=burst.glyphs[i];float u=(glyph%columns)/(float)columns,v=(glyph/columns)/(float)rows,du=1F/columns,dv=1F/rows;
            shader.safeGetUniform("GlyphRect").set(u,v,du,dv);
            shader.safeGetUniform("Brush").set(reveal);
            shader.safeGetUniform("Fade").set(opacity);
            var vertices=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            quad(vertices,g.pose().last().pose(),-size/2,-size/2,size,u,v,du,dv);
            BufferUploader.drawWithShader(vertices.buildOrThrow());g.pose().popPose();glyphDraws++;
        }
        RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);
    }
    private static void quad(BufferBuilder b,Matrix4f m,float x,float y,float size,float u,float v,float du,float dv){
        b.addVertex(m,x,y,0).setUv(u,v).setColor(255,255,255,255);
        b.addVertex(m,x,y+size,0).setUv(u,v+dv).setColor(255,255,255,255);
        b.addVertex(m,x+size,y+size,0).setUv(u+du,v+dv).setColor(255,255,255,255);
        b.addVertex(m,x+size,y,0).setUv(u+du,v).setColor(255,255,255,255);
    }
    private static void drawSplashes(GuiGraphics g,float cx,float cy,float width,float size,float age,float fade,int seed) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);var m=g.pose().last().pose();
        float burst=smooth(age/6),alpha=fade*(1-smooth((age-8)/14));
        for(int i=0;i<64;i++) {
            float delay=hash(seed+i*89)*6,elapsed=age-delay;if(elapsed<0)continue;
            double a=(hash(seed+i*139)-.5)*Math.PI*2;
            float travel=(1-(float)Math.exp(-elapsed*.13))*(size*.30F+hash(i+19)*size*.70F);
            float x=cx+(hash(seed+i*31)-.5F)*width*.9F+(float)Math.cos(a)*travel;
            float y=cy+(hash(seed+i*47)-.5F)*size*.35F+(float)Math.sin(a)*travel*.7F+elapsed*elapsed*.035F;
            float radius=(1.2F+hash(seed+i*73)*5.0F)*Math.min(1,elapsed/1.5F);
            drop(b,m,x,y,radius,radius*(.5F+hash(i+7)*.8F),a,0x100D0B,alpha*.92F);
        }
        // Two torn, tapering brush tails suggest the speed of the strike without a panel.
        for(int i=0;i<18;i++) {
            float x=cx-width*.52F+width*hash(seed+i*113),y=cy+size*(hash(seed+i*59)-.5F)*.62F;
            float length=size*(.25F+hash(i+137)*.8F)*burst,thickness=.4F+hash(i+71)*2.5F;
            triangle(b,m,x-length,y+thickness,x+length*.24F,y-thickness,x+length,y-length*.08F,0x16100D,alpha*.45F);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }
    private static void drop(BufferBuilder b,Matrix4f m,float x,float y,float rx,float ry,double angle,int color,float alpha){
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9,c=(i+1)*Math.PI*2/9;
            float ax=(float)(Math.cos(a)*rx),ay=(float)(Math.sin(a)*ry),bx=(float)(Math.cos(c)*rx),by=(float)(Math.sin(c)*ry);
            triangle(b,m,x,y,x+(float)(ax*Math.cos(angle)-ay*Math.sin(angle)),y+(float)(ax*Math.sin(angle)+ay*Math.cos(angle)),
                    x+(float)(bx*Math.cos(angle)-by*Math.sin(angle)),y+(float)(bx*Math.sin(angle)+by*Math.cos(angle)),color,alpha);}
    }
    private static void triangle(BufferBuilder b,Matrix4f m,float ax,float ay,float bx,float by,float cx,float cy,int rgb,float alpha){
        int a=Math.clamp(Math.round(alpha*255),0,255),r=rgb>>16&255,g=rgb>>8&255,blue=rgb&255;
        b.addVertex(m,ax,ay,290).setColor(r,g,blue,a);b.addVertex(m,bx,by,290).setColor(r,g,blue,a);b.addVertex(m,cx,cy,290).setColor(r,g,blue,a);
    }
    private static float hash(int n){n=(n^(n>>>16))*0x7feb352d;n=(n^(n>>>15))*0x846ca68b;return ((n^(n>>>16))&0xFFFFFF)/16777216F;}
    private static float smooth(float x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
}
