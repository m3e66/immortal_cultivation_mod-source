package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.flame.FlameVesselItem;
import com.example.immortal_cultivation_mod.item.HigherWeaponItem;
import com.example.immortal_cultivation_mod.item.HigherWeaponSpec;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import org.joml.Matrix4f;

/** One shared, transparent-center charge dial for combat, spells and held items. */
public final class ChargeReticleOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation HUD_ID=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","charge_reticle");
    public static final ChargeReticleOverlay INSTANCE=new ChargeReticleOverlay();
    public static final int WIDTH=136,HEIGHT=160,CENTER_X=68,CENTER_Y=64;
    private static final double TAU=Math.PI*2;
    public record Charge(String id,HudElement layout,float progress,int percent,Component label,int color) {}
    private ChargeReticleOverlay() {}

    /** Reads existing gameplay counters; presentation never starts or completes an action. */
    public static List<Charge> active(Minecraft mc,float partial){
        var result=new ArrayList<Charge>(4);
        if(mc.player==null||!mc.player.isAlive()||mc.player.isSpectator())return result;
        float heavy=ClientData.heavyAttackChargeProgress(),swords=ClientData.swordCaseChargeProgress();
        if(heavy>0)add(result,"heavy",HudElement.HEAVY_ATTACK,heavy,Math.round(heavy*100),Component.empty(),0xFFAF65);
        if(ClientData.preparedCharging()){
            float progress=ClientData.preparedChargeScale()-1;
            add(result,"spell",HudElement.SPELL_CHARGE,progress,Math.round(progress*100),Component.empty(),0x78DFF1);
        }
        if(swords>0)add(result,"swords",HudElement.SWORD_CHARGE,swords,Math.round(swords*100),Component.empty(),0xF4D77D);
        if(mc.player.isUsingItem()){
            var stack=mc.player.getUseItem();float held=mc.player.getTicksUsingItem()+partial;
            float progress=-1;int color=0xA6E6CD;String id="item";
            if(stack.getItem() instanceof HigherWeaponItem weapon&&weapon.spec==HigherWeaponSpec.FLAME_PATTERN_DAO){progress=held/30;id="weapon";color=0xFF9666;}
            else if(stack.getItem() instanceof FlameVesselItem){
                var kind=FlameVesselItem.flame(stack);
                int qi=ClientData.cultivationData==null?0:ClientData.cultivationData.qi();
                if(kind!=null&&FlameVesselItem.canAffordCharge(kind,(int)held,qi,mc.player.isCreative())){
                    progress=(held-4)/(FlameVesselItem.FULL_CHARGE_TICKS-4);id="flame";color=kind.color();
                }
            }else if(stack.getItem() instanceof UtilityTalismanItem&&stack.getUseDuration(mc.player)>0){
                progress=held/UtilityTalismanItem.LIGHT_ANCHOR_HOLD_TICKS;id="talisman";color=0xF5ECB5;
            }else if(stack.getUseAnimation()==UseAnim.BOW){
                float draw=held/20;progress=(draw*draw+draw*2)/3;id="bow";
            }else if(stack.getUseAnimation()==UseAnim.CROSSBOW){
                if(!CrossbowItem.isCharged(stack)){progress=held/Math.max(1,CrossbowItem.getChargeDuration(stack,mc.player));id="crossbow";color=0xA9CFFF;}
            }else if(stack.getUseAnimation()==UseAnim.SPEAR){progress=held/TridentItem.THROW_THRESHOLD_TIME;id="spear";color=0xA7E3F9;}
            if(progress>=0||id.equals("flame")){
                progress=Mth.clamp(progress,0,1);
                add(result,id,HudElement.HEAVY_ATTACK,progress,Math.round(progress*100),Component.empty(),color);
            }
        }
        // A held-item action owns the shared dial. Otherwise show the prepared spell,
        // sword case, then melee charge; concurrent counters never produce extra meters.
        if(result.isEmpty())return List.of();
        Charge selected=result.getFirst();
        for(var charge:result)if(priority(charge)>priority(selected))selected=charge;
        return List.of(selected);
    }
    private static int priority(Charge charge){
        return switch(charge.id){case "heavy"->0;case "swords"->1;case "spell"->2;default->3;};
    }
    private static void add(List<Charge> result,String id,HudElement layout,float progress,int percent,Component label,int color){
        if(Float.isFinite(progress))result.add(new Charge(id,layout,Mth.clamp(progress,0,1),percent,label,color&0xFFFFFF));
    }
    @Override public void render(GuiGraphics g,DeltaTracker delta){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui)return;
        var charges=active(mc,delta.getGameTimeDeltaPartialTick(false));if(charges.isEmpty())return;
        var charge=charges.getFirst();
        // All sources share one HUD anchor, including any user layout adjustment.
        var box=HudLayout.place(g,HudElement.HEAVY_ATTACK,g.guiWidth()/2-CENTER_X,g.guiHeight()/2-CENTER_Y,WIDTH,HEIGHT);
        int cx=box.x()+CENTER_X,cy=box.y()+CENTER_Y;
        double time=mc.level.getGameTime()+delta.getGameTimeDeltaPartialTick(false);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var vertices=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        draw(vertices,g.pose().last().pose(),cx,cy,19,charge,time);
        BufferUploader.drawWithShader(vertices.buildOrThrow());RenderSystem.enableCull();RenderSystem.disableBlend();
        var label=Component.literal(charge.percent+"%");
        float scale=.8F;
        int textY=cy+28;
        if(textY+8>g.guiHeight()-46)textY=cy-36;
        g.pose().pushPose();g.pose().translate(cx,textY,0);g.pose().scale(scale,scale,1);
        g.drawString(mc.font,label,-mc.font.width(label)/2,0,0xFF000000|charge.color,false);g.pose().popPose();
    }
    private static void draw(BufferBuilder out,Matrix4f m,float cx,float cy,float radius,Charge charge,double time){
        float p=charge.progress;boolean ready=p>=.999F;
        float pulse=ready?.82F+.18F*(float)Math.sin(time*.32):1;
        arc(out,m,cx,cy,radius,4.8F,0,TAU,0x06141D,.8F);
        arc(out,m,cx,cy,radius,1.4F,0,TAU,charge.color,.18F);
        // Quarter brackets and a thin inner rim frame an unobstructed aiming center.
        for(int i=0;i<4;i++){
            double angle=i*Math.PI/2;
            arc(out,m,cx,cy,radius+4,.65F,angle+.13,angle+.43,charge.color,.4F);
            arc(out,m,cx,cy,radius-4,.55F,angle+.09,angle+.27,0xD5E9E8,.3F);
        }
        for(int tick=0;tick<16;tick++){
            double a=tick*TAU/16;
            line(out,m,cx+(float)Math.sin(a)*(radius+2),cy-(float)Math.cos(a)*(radius+2),
                    cx+(float)Math.sin(a)*(radius+(tick%4==0?5:3.4F)),cy-(float)Math.cos(a)*(radius+(tick%4==0?5:3.4F)),.65F,charge.color,tick%4==0?.7F:.3F);
        }
        if(p>0){
            arc(out,m,cx,cy,radius,5.2F,0,TAU*p,charge.color,.11F*pulse);
            arc(out,m,cx,cy,radius,2.15F,0,TAU*p,charge.color,.95F*pulse);
            arc(out,m,cx,cy,radius-.55F,.55F,0,TAU*p,0xF1FFFF,.6F*pulse);
            double angle=TAU*p;float hx=cx+(float)Math.sin(angle)*radius,hy=cy-(float)Math.cos(angle)*radius;
            diamond(out,m,hx,hy,3.2F,charge.color,.18F);diamond(out,m,hx,hy,1.35F,0xF1FFFF,.95F);
        }
        if(ready)for(int i=0;i<4;i++){
            double a=i*Math.PI/2;float r=radius+5.5F+.5F*(float)Math.sin(time*.32);
            diamond(out,m,cx+(float)Math.sin(a)*r,cy-(float)Math.cos(a)*r,1.55F,0xFFF1C0,pulse);
        }
    }
    private static void arc(BufferBuilder b,Matrix4f m,float cx,float cy,float r,float width,double from,double to,int color,float alpha){
        int steps=Math.max(1,(int)Math.ceil((to-from)/TAU*128));
        for(int i=0;i<steps;i++){
            double a=from+(to-from)*i/steps,c=from+(to-from)*(i+1)/steps;
            float x0=cx+(float)Math.sin(a)*(r-width/2),y0=cy-(float)Math.cos(a)*(r-width/2);
            float x1=cx+(float)Math.sin(a)*(r+width/2),y1=cy-(float)Math.cos(a)*(r+width/2);
            float x2=cx+(float)Math.sin(c)*(r+width/2),y2=cy-(float)Math.cos(c)*(r+width/2);
            float x3=cx+(float)Math.sin(c)*(r-width/2),y3=cy-(float)Math.cos(c)*(r-width/2);
            quad(b,m,x0,y0,x1,y1,x2,y2,x3,y3,color,alpha);
        }
    }
    private static void line(BufferBuilder b,Matrix4f m,float x,float y,float x2,float y2,float w,int color,float alpha){
        double length=Math.max(.0001,Math.hypot(x2-x,y2-y));float sx=(float)(-(y2-y)/length*w/2),sy=(float)((x2-x)/length*w/2);
        quad(b,m,x-sx,y-sy,x+sx,y+sy,x2+sx,y2+sy,x2-sx,y2-sy,color,alpha);
    }
    private static void diamond(BufferBuilder b,Matrix4f m,float x,float y,float r,int color,float alpha){quad(b,m,x,y-r,x+r,y,x,y+r,x-r,y,color,alpha);}
    private static void quad(BufferBuilder b,Matrix4f m,float x,float y,float x2,float y2,float x3,float y3,float x4,float y4,int color,float alpha){
        vertex(b,m,x,y,color,alpha);vertex(b,m,x2,y2,color,alpha);vertex(b,m,x3,y3,color,alpha);
        vertex(b,m,x,y,color,alpha);vertex(b,m,x3,y3,color,alpha);vertex(b,m,x4,y4,color,alpha);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,float x,float y,int color,float alpha){b.addVertex(m,x,y,0).setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);}
}
