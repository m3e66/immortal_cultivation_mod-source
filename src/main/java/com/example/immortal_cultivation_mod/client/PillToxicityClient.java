package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.client.hud.HudLayout;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.alchemy.*;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.PillToxicityPayload;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Cached edge-only masks. Fixed center exclusion exists in every texture, at every GUI scale. */
@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT)
public final class PillToxicityClient {
    private static final ResourceLocation[] MASKS=new ResourceLocation[4], VEINS=new ResourceLocation[2];
    private static final float[] WEIGHTS=new float[4], TARGETS=new float[4];
    private static PillToxicityVisualState.Warning warningState=new PillToxicityVisualState.Warning();
    private static long lastFrame;
    private static double animationSeconds;
    private static int warningStage;
    private static boolean connected;
    private static net.minecraft.nbt.CompoundTag pendingState,pendingRules;
    private static Component warning=Component.empty();
    static {
        for(int i=0;i<4;i++)MASKS[i]=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","textures/gui/toxicity/mist_"+(i+1)+".png");
        for(int i=0;i<2;i++)VEINS[i]=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","textures/gui/toxicity/veins_"+(i+3)+".png");
    }
    public static void receive(PillToxicityPayload payload) {
        var player=Minecraft.getInstance().player;
        if(player==null){pendingState=payload.state();if(!payload.rules().isEmpty())pendingRules=payload.rules();return;}
        connected=true;
        if(!payload.rules().isEmpty())PillToxicity.clientPolicy=payload.rules();
        else if(pendingRules!=null)PillToxicity.clientPolicy=pendingRules;
        player.setData(ModAttachments.PILL_TOXICITY,payload.state());
        pendingState=pendingRules=null;
        warningStageUpdate(PillToxicity.stage(player));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();
        if(mc.player==null && connected){
            java.util.Arrays.fill(WEIGHTS,0);warningState=new PillToxicityVisualState.Warning();lastFrame=0;
            animationSeconds=0;PillToxicity.clientPolicy=new net.minecraft.nbt.CompoundTag();connected=false;
            pendingState=pendingRules=null;
        } else if(mc.player!=null && pendingState!=null) {
            receive(new PillToxicityPayload(pendingState,pendingRules==null?new net.minecraft.nbt.CompoundTag():pendingRules));
            pendingState=pendingRules=null;
        }
    }
    private static void warningStageUpdate(int stage) {
        if(warningState.accept(stage,animationSeconds)){warningStage=stage;warning=PillToxicity.stageName(stage);}
    }
    public static void render(GuiGraphics g,float partial) {
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        long now=System.nanoTime();double elapsed=lastFrame==0?0:Math.max(0,Math.min(.25,(now-lastFrame)/1_000_000_000.));lastFrame=now;
        animationSeconds+=elapsed;
        PillToxicityVisualState.targets(PillToxicity.value(mc.player),PillToxicity.policy(mc.player),TARGETS);
        PillToxicityVisualState.advance(WEIGHTS,TARGETS,elapsed);
        if(mc.options.hideGui||!ModClientConfig.PILL_TOXICITY_OVERLAY.get())return;
        float opacity=ModClientConfig.PILL_TOXICITY_OPACITY.get().floatValue();if(opacity<=0)return;
        drawEdges(g,WEIGHTS,(float)animationSeconds,opacity);
        drawWarning(g,warning,warningStage,warningState.opacity(animationSeconds)*opacity);
    }
    /** Shared with the isolated client visual check; no resource creation or texture uploads. */
    public static void drawEdges(GuiGraphics g,float[] weights,float seconds,float opacity) {
        if(opacity<=0)return;
        // Finish queued HUD geometry before changing its shader color or blend state.
        g.flush();
        for(int i=0;i<4;i++) if(weights[i]>.0001F) {
            // Two independently drifting mist layers, enlarged to keep every corner covered.
            mistLayer(g,MASKS[i],seconds,i,weights[i]*opacity*.68F,false);
            mistLayer(g,MASKS[i],seconds,i,weights[i]*opacity*.45F,true);
            if(i>=2) {
                float pulse=.91F+.055F*(float)Math.sin(seconds*.57+i)+.035F*(float)Math.sin(seconds*.93+i*1.7);
                layerOpacity(g,weights[i]*opacity*pulse);
                g.blit(VEINS[i-2],0,0,g.guiWidth(),g.guiHeight(),0,0,256,256,256,256);
                g.setColor(1,1,1,1);
                drawWisps(g,seconds,weights[i]*opacity,i==3);
            }
        }
        g.setColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private static void mistLayer(GuiGraphics g,ResourceLocation texture,float seconds,int stage,float alpha,boolean second) {
        int w=g.guiWidth(),h=g.guiHeight();
        float direction=second?-1:1;
        int dx=(int)(Math.sin(seconds*.10*direction+stage*1.3+(second?2:0))*w*.008);
        int dy=(int)(Math.sin(seconds*.13*direction+stage*.7)*h*.009);
        float breathing=.94F+.06F*(float)Math.sin(seconds*.21+stage+(second?1.4:0));
        layerOpacity(g,alpha*breathing);
        g.blit(texture,-w/20+dx,-h/20+dy,w+w/10,h+h/10,0,0,256,256,256,256);
    }
    private static void layerOpacity(GuiGraphics g,float alpha) {
        // setColor can flush a GUI batch, whose render type disables blending on exit.
        // Re-enable it after that flush for every immediate texture draw, including
        // crossfades following buffered wisp geometry.
        g.setColor(1,1,1,Math.clamp(alpha,0,1));
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
    }
    public static void drawWarning(GuiGraphics g,Component message,int stage,float opacity) {
        int alpha=(int)(255*Math.max(0,Math.min(1,opacity)));if(alpha<4)return;
        var font=Minecraft.getInstance().font;
        int color=stage>=3?0xBBA1D4:0xB8CEB0;
        var position=HudLayout.place(g,HudElement.TOXICITY,(g.guiWidth()-font.width(message))/2,12,font.width(message),9);
        g.drawString(font,message,position.x(),position.y(),(alpha<<24)|color,false);
    }
    private static void drawWisps(GuiGraphics g,float seconds,float alpha,boolean severe) {
        // Inward travel stays within the outer 15%; aiming region remains clear.
        int width=g.guiWidth(),height=g.guiHeight();
        for(int i=0;i<(severe?8:4);i++) {
            float phase=(seconds/12+i*.173F)%1;
            if(phase>.65F)continue; // Sparse, occasional wisps followed by a quiet interval.
            phase/=.65F;
            int x=(int)(width*.14*phase),y=(int)(height*(.12+((i*.173)% .76)));
            int a=(int)(alpha*(severe?55:30)*Math.sin(phase*Math.PI));
            int size=Math.max(2,width/180);
            g.fillGradient(i%2==0?x:width-x-size,y,i%2==0?x+size:width-x,y+size*3,(a<<24)|0x9974B2,0x00767798);
        }
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
        var player=e.getEntity();if(player==null)return;
        ItemStack pill=e.getItemStack();if(pill.getItem() instanceof JadeBottleItem)pill=JadeBottleItem.contents(pill);
        if(!(pill.getItem() instanceof AlchemyPillItem)&&!(pill.getItem() instanceof LegacyPillItem))return;
        var rules=PillToxicity.policy(player);if(rules.isEmpty())return;
        var lines=e.getToolTip();
        if(PillToxicity.pillId(pill).equals("qingdu")) {
            lines.add(Component.translatable("toxicity.detox.amount",PillData.number(rules.getDouble("Detox"))).withStyle(ChatFormatting.GREEN));
            if(PillToxicity.cooldown(player)>0)lines.add(Component.translatable("toxicity.detox.cooldown",PillToxicity.cooldown(player)).withStyle(ChatFormatting.RED));
            String reason=PillToxicity.detoxReason(PillToxicity.state(player),rules,PillToxicity.state(player).getInt("PlayerRealm"));
            lines.add(Component.translatable("toxicity.detox.usable."+(reason==null?"ready":reason),
                    PillData.number(Math.min(PillToxicity.value(player),rules.getDouble("Detox"))))
                    .withStyle(reason==null?ChatFormatting.GREEN:ChatFormatting.RED));
        } else lines.add(Component.translatable("toxicity.expected",PillData.number(PillToxicity.expected(PillToxicity.state(player),rules,pill,
                PillToxicity.state(player).getInt("PlayerRealm")))).withStyle(ChatFormatting.DARK_PURPLE));
    }
    private PillToxicityClient() {}
}
