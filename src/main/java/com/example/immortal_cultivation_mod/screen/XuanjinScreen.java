package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.companion.XuanjinRules;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import java.util.UUID;

/** Companion control panel. No chat commands are sent by this screen. */
public final class XuanjinScreen extends Screen {
    private static final int GOLD=0xFF785D30,EDGE=0xFF9A8E75,TEXT=0xFF293A32,MUTED=0xFF596052;
    private static final List<String> BOND=List.of("manifest","recall","feed","possess");
    private static final List<String> TECHNIQUES=List.of("eyes","claw","reach","voice","devour","tails","vortex","swallow","return","refine","dash");
    private final UUID session;
    private CompoundTag state;
    private Component feedback=Component.empty();
    private int left,top,panelW,panelH,sideW,contentX,contentW,tab,page,ticks,receivedTick;
    private boolean pending,confirming;
    private int sceneX,sceneW,sceneY,sceneH;
    private float yaw=215,pitch=15;
    private boolean dragging,manualRotation;
    private final ItemStack lantern=new ItemStack(ModItems.XUANJIN_SEAL.get());

    public XuanjinScreen(XuanjinPayloads.View view) {
        super(tr("title"));session=view.session();state=view.state();feedback=view.feedback();
        tab=Math.clamp(state.getInt("GuiTab"),0,2);page=tab==1?Math.clamp(state.getInt("GuiPage"),0,1):0;
        confirming=state.getLong("FreeConfirmUntil")>state.getLong("Now");
    }
    private static Component tr(String key,Object... args){return Component.translatable("xuanjin.gui."+key,args);}
    private static Component actionName(String id){return Component.translatable("xuanjin.action."+id);}
    public static void accept(XuanjinPayloads.View view) {
        Minecraft mc=Minecraft.getInstance();
        if(view.open() && !view.close() && !(mc.screen instanceof XuanjinScreen current && current.session.equals(view.session()))) {
            mc.setScreen(new XuanjinScreen(view));return;
        }
        if(!(mc.screen instanceof XuanjinScreen screen) || !screen.session.equals(view.session()))return;
        if(view.close()) {
            mc.setScreen(null);
            if(mc.player!=null && !view.feedback().getString().isEmpty())mc.player.displayClientMessage(view.feedback(),true);
            return;
        }
        boolean old=screen.confirming;screen.state=view.state();screen.feedback=view.feedback();
        screen.receivedTick=screen.ticks;screen.pending=false;
        screen.confirming=screen.state.getLong("FreeConfirmUntil")>screen.now();
        if(old!=screen.confirming)screen.init();
    }
    private long now(){return state.getLong("Now")+ticks-receivedTick;}
    private int seal(){return Math.clamp(state.getInt("Seal"),0,6);}
    @Override protected void init() {
        clearWidgets();panelW=Math.min(500,width-16);panelH=Math.min(320,height-16);
        left=(width-panelW)/2;top=(height-panelH)/2;sideW=Math.clamp(panelW/4,80,120);
        contentX=left+16;contentW=panelW-32;
        sceneX=contentX+sideW+6;sceneW=contentW-2*sideW-12;
        sceneY=top+72;sceneH=panelH-164;
        addRenderableWidget(new PanelButton(left+panelW-28,top+10,18,18,Component.literal("×"),null,this::onClose));
        if(confirming) {
            contentX=left+18;contentW=panelW-36;
            addRenderableWidget(new PanelButton(contentX,top+panelH-83,(contentW-6)/2,24,tr("cancel"),null,()->send("cancel_free")));
            addRenderableWidget(new PanelButton(contentX+(contentW+6)/2,top+panelH-83,(contentW-6)/2,24,tr("release_confirm"),"free_confirm",()->send("free_confirm")));
            return;
        }
        String[] tabs={"bond","techniques","seals"};int tw=(contentW-8)/3;
        for(int i=0;i<3;i++) {
            final int selected=i;
            var button=new PanelButton(contentX+i*(tw+4),top+42,tw,20,tr(tabs[i]),null,()->{tab=selected;page=0;rememberView();init();});
            button.selected=tab==i;addRenderableWidget(button);
        }
        if(tab<2) {
            List<String> list=tab==0?BOND:TECHNIQUES;int rowH=Math.min(40,(panelH-144)/3);
            for(int i=page*6;i<Math.min(list.size(),page*6+6);i++) {
                String id=list.get(i);int cell=i-page*6;
                var button=new PanelButton(cell%2==0?contentX:contentX+contentW-sideW,top+74+(cell/2)*rowH,
                        sideW,rowH-2,actionName(id),id,()->send(tab==1?"select_"+id:id));
                addRenderableWidget(button);
            }
            if(list.size()>6) {
                addRenderableWidget(new PanelButton(sceneX,top+panelH-85,sceneW,17,
                        tr("page",page+1,(list.size()+5)/6),null,()->{page=(page+1)%((list.size()+5)/6);rememberView();init();}));
            }
        } else {
            addRenderableWidget(new PanelButton(contentX,top+74,sideW,26,actionName("unlock"),"unlock",()->send("unlock")));
            addRenderableWidget(new PanelButton(contentX+contentW-sideW,top+74,sideW,26,actionName("free"),"free",()->send("free")));
        }
    }
    private void send(String id){if(pending || minecraft.getConnection()==null)return;pending=true;PacketDistributor.sendToServer(new XuanjinPayloads.Action(session,id));}
    private void rememberView(){if(minecraft.getConnection()!=null)PacketDistributor.sendToServer(new XuanjinPayloads.Action(session,"view_"+tab+"_"+page));}
    @Override public void tick() {
        ticks++;
        if(ticks%20==0 && minecraft.getConnection()!=null)PacketDistributor.sendToServer(new XuanjinPayloads.Action(session,"refresh"));
        if(confirming && state.getLong("FreeConfirmUntil")<=now()){confirming=false;init();}
    }
    @Override public void removed(){
        if(minecraft.getConnection()!=null)PacketDistributor.sendToServer(new XuanjinPayloads.Action(session,"close"));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partial) {
        // The panel draws its own backdrop; the default blur would blur its contents.
    }
    private int required(String id) {
        return switch(id){case "eyes"->1;case "claw","reach"->2;case "voice"->4;case "devour"->5;
            case "possess","tails","vortex","swallow","return","refine","dash","free","free_confirm"->6;default->0;};
    }
    private int cost(String id){return switch(id){case "possess"->1000;case "vortex"->2500;case "dash"->200;
        case "claw"->seal()==6?800:Math.max(20,XuanjinRules.stage(seal()).energy()/5);default->0;};}
    private Component details(String id) {
        Component help=switch(id) {
            case "claw" -> tr("help.claw",(int)XuanjinRules.CLAW_RANGE);
            case "voice" -> tr("help.voice",(int)XuanjinRules.ROAR_RANGE);
            default -> tr("help."+id);
        };
        var text=Component.empty().append(actionName(id)).append("\n\n").append(help);
        if(TECHNIQUES.contains(id)) {
            String cooldown=XuanjinRules.cooldownSeconds(id);
            text.append("\n\n").append(tr("spell_stats",required(id),cost(id),cooldown));
            if(id.equals("claw"))text.append("\n").append(tr("claw_damage",(int)(XuanjinRules.stage(seal()).attack()*2),(int)(XuanjinRules.stage(seal()).attack()*3)));
            text.append("\n").append(tr("select_hint"));
        }
        return text;
    }
    private Component unavailable(String id) {
        if(pending)return tr("waiting");
        if(seal()<required(id))return tr("seal_required",required(id));
        long cooldown=state.getLong("Cooldown."+id)-now();if(cooldown>0)return tr("cooldown",(cooldown+19)/20);
        if(state.getInt("Energy")<cost(id))return tr("cost",cost(id));
        boolean active=state.getBoolean("Active") || state.getLong("ManifestUntil")>now() || state.getLong("PossessUntil")>now();
        if(id.equals("unlock")) {
            if(seal()==6)return tr("all_seals");
            if(active)return Component.translatable("xuanjin.recall_first");
            if(state.getInt("Realm")<XuanjinRules.stage(seal()+1).realmStage())return tr("realm_required");
            if(state.getInt("Trust")<XuanjinRules.unlockTrust(seal()+1))return tr("trust_required",XuanjinRules.unlockTrust(seal()+1));
            if(state.getInt("Energy")<XuanjinRules.unlockEnergy(seal()+1))return tr("cost",XuanjinRules.unlockEnergy(seal()+1));
        }
        if(id.equals("manifest") || id.equals("possess")) {
            if(active)return tr("already_active");
            if(state.getLong("RecoverUntil")>now())return tr("recovering",(state.getLong("RecoverUntil")-now()+19)/20);
        }
        if(id.equals("feed") && state.getInt("Qi")<Math.max(20,XuanjinRules.stage(seal()).energy()/10))return tr("qi_required");
        if(id.equals("recall") && !active)return tr("resting");
        if(List.of("devour","tails","vortex","swallow","return","refine").contains(id) && !active)return tr("manifest_first");
        if(id.equals("dash") && state.getLong("PossessUntil")<=now())return tr("possess_first");
        if(List.of("swallow","return","refine").contains(id) && !state.getBoolean("VortexReady"))return tr("vortex_first");
        if(id.equals("free_confirm") && state.getLong("FreeConfirmUntil")<=now())return tr("expired");
        return null;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        // Match the established cultivation menus: dim the world and draw their landscape texture.
        g.fill(0,0,width,height,0xA0101716);
        g.setColor(1,1,1,1);
        CultivationGuiTextures.drawLandscape(g,left,top,panelW,panelH);
        centeredFit(g,title,left+panelW/2,top+17,(int)(panelW*.34F),GOLD);
        if(!confirming)centeredFit(g,Component.translatable("xuanjin.stage."+seal()),left+panelW/2,top+64,contentW,MUTED);
        if(confirming) {
            CultivationGuiTextures.drawInnerBox(g,contentX-6,top+44,contentW+12,panelH-101);
            fit(g,tr("release_title"),contentX,top+51,contentW,0xFF893A29);
            wrap(g,tr("release_warning"),contentX,top+73,contentW,Math.max(3,(panelH-164)/11),TEXT);
            fit(g,tr("confirm_seconds",Math.max(0,(state.getLong("FreeConfirmUntil")-now()+19)/20)),contentX,top+panelH-101,contentW,GOLD);
        } else {
            renderLantern(g,partial);
            if(tab==2) {
                int next=Math.min(6,seal()+1),right=contentX+contentW-sideW;
                CultivationGuiTextures.drawInnerBox(g,contentX,top+105,sideW,Math.max(49,panelH-175));
                CultivationGuiTextures.drawInnerBox(g,right,top+105,sideW,Math.max(49,panelH-175));
                fit(g,seal()==6?tr("all_seals"):tr("next_seal",Component.translatable("xuanjin.stage."+next)),contentX+5,top+112,sideW-10,GOLD);
                fit(g,Component.translatable("xuanjin.realm."+next),contentX+5,top+125,sideW-10,MUTED);
                fit(g,tr("trust_required",XuanjinRules.unlockTrust(next)),contentX+5,top+138,sideW-10,MUTED);
                fit(g,tr("cost",XuanjinRules.unlockEnergy(next)),right+5,top+112,sideW-10,MUTED);
                wrap(g,tr("release_short"),right+5,top+125,sideW-10,2,0xFF893A29);
                int cell=(sceneW-12)/7;
                for(int i=0;i<7;i++) {
                    int x=sceneX+i*(cell+2);
                    g.fill(x,top+panelH-85,x+cell,top+panelH-69,i<=seal()?0xFF557C70:0xFFC8C5AE);
                    if(i==seal())g.renderOutline(x,top+panelH-85,cell,16,GOLD);
                    g.drawCenteredString(font,Integer.toString(i),x+cell/2,top+panelH-81,i<=seal()?0xFFF8F3DF:TEXT);
                }
            } else if(tab==0) {
                String status=state.getLong("PossessUntil")>now()?"possessing":state.getLong("ManifestUntil")>now()?"manifested":"resting";
                centeredFit(g,tr(status),left+panelW/2,top+panelH-81,sceneW,MUTED);
                if(panelH>=280) {
                    wrap(g,tr("bond_hint"),contentX+3,top+165,sideW-6,4,MUTED);
                    wrap(g,tr("rotate_hint"),contentX+contentW-sideW+3,top+165,sideW-6,4,MUTED);
                }
            }
            int bars=top+panelH-61,w=(contentW-16)/2;var stage=XuanjinRules.stage(seal());
            bar(g,"health",state.getFloat("Health"),stage.health(),contentX,w,bars,0xFFAB6250);
            bar(g,"energy",state.getInt("Energy"),stage.energy(),contentX+contentW-w,w,bars,0xFF448B7B);
            bar(g,"trust",state.getInt("Trust"),100,contentX,w,bars+19,0xFF9E9450);
            bar(g,"freedom",state.getInt("Freedom"),100,contentX+contentW-w,w,bars+19,0xFFB47A49);
        }
        int footerY=top+panelH-24;
        Component message=feedback.getString().isEmpty()?(tab==1?tr("select_hint"):tr("rotate_hint")):feedback;
        wrap(g,message,contentX+3,footerY,contentW-6,2,MUTED);
        super.render(g,mouseX,mouseY,partial);
        for(var child:children())if(child instanceof PanelButton button && button.actionId!=null
                && (BOND.contains(button.actionId) || TECHNIQUES.contains(button.actionId)) && button.isHoveredOrFocused()) {
            renderDetails(g,details(button.actionId),mouseX,mouseY);break;
        }
        if(mouseX>=contentX && mouseX<contentX+contentW && mouseY>=footerY && mouseY<top+panelH)
            g.renderTooltip(font,font.split(message,Math.min(280,width-24)),mouseX,mouseY);
    }
    private void renderDetails(GuiGraphics g,Component text,int mouseX,int mouseY) {
        int boxW=Math.min(310,width-24);var lines=font.split(text,boxW-12);int boxH=lines.size()*10+12;
        float scale=Math.min(1F,(height-16F)/boxH);
        int x=Math.clamp(mouseX+10,8,Math.max(8,width-(int)Math.ceil(boxW*scale)-8));
        int y=Math.clamp(mouseY+10,8,Math.max(8,height-(int)Math.ceil(boxH*scale)-8));
        g.pose().pushPose();g.pose().translate(x,y,600);g.pose().scale(scale,scale,1);
        g.fill(0,0,boxW,boxH,0xF0182023);g.renderOutline(0,0,boxW,boxH,0xFF9A8E75);
        for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),6,6+i*10,0xFFF2EAD5,false);
        g.pose().popPose();
    }
    private void renderLantern(GuiGraphics g,float partial) {
        g.enableScissor(sceneX,sceneY,sceneX+sceneW,sceneY+sceneH);
        g.flush();RenderSystem.enableDepthTest();Lighting.setupFor3DItems();
        var pose=g.pose();pose.pushPose();
        try {
            pose.translate(sceneX+sceneW/2F,sceneY+sceneH/2F,180);
            float scale=Math.min(sceneW*.9F,sceneH*.9F);
            pose.scale(scale,-scale,scale);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw+(manualRotation?0:(ticks+partial)*.4F)));
            minecraft.getItemRenderer().renderStatic(lantern,ItemDisplayContext.NONE,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,g.bufferSource(),minecraft.level,0);
            g.flush();
        } finally {
            pose.popPose();RenderSystem.disableDepthTest();Lighting.setupForFlatItems();g.disableScissor();
        }
    }
    private boolean overLantern(double x,double y) {
        return !confirming && x>=sceneX && x<sceneX+sceneW && y>=sceneY && y<sceneY+sceneH;
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(overLantern(x,y)) {
            if(button==0) {
                if(!manualRotation)yaw+=ticks*.4F;
                manualRotation=true;dragging=true;return true;
            }
            if(button==1){yaw=215;pitch=15;manualRotation=false;return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(dragging && button==0){yaw+=(float)dx*.8F;pitch=Math.clamp(pitch+(float)dy*.6F,-35,55);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0 && dragging){dragging=false;return true;}
        return super.mouseReleased(x,y,button);
    }
    private void centeredFit(GuiGraphics g,Component text,int x,int y,int w,int color) {
        fit(g,text,x-Math.min(w,font.width(text))/2,y,w,color);
    }
    private void bar(GuiGraphics g,String name,float value,float max,int x,int w,int y,int color) {
        Component label=tr(name,Math.max(0,(int)value),(int)max);
        if(name.equals("energy"))label=Component.empty().append(label).append(" ").append(tr("regen_rate",XuanjinRules.regeneration(seal())));
        fit(g,label,x,y,w,TEXT);
        g.fill(x,y+11,x+w,y+14,0xFFB9B79F);g.fill(x,y+11,x+(int)(w*Math.clamp(value/max,0F,1F)),y+14,color);
    }
    private void wrap(GuiGraphics g,Component text,int x,int y,int w,int lines,int color) {
        var split=font.split(text,w);for(int i=0;i<Math.min(lines,split.size());i++)g.drawString(font,split.get(i),x,y+i*11,color,false);
    }
    private void fit(GuiGraphics g,Component text,int x,int y,int w,int color) {
        float scale=Math.min(1F,w/(float)Math.max(1,font.width(text)));g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private final class PanelButton extends AbstractButton {
        private final String actionId;private final Runnable callback;
        private boolean selected;
        PanelButton(int x,int y,int w,int h,Component name,String actionId,Runnable callback){super(x,y,w,h,name);this.actionId=actionId;this.callback=callback;}
        private boolean technique(){return tab==1 && actionId!=null && TECHNIQUES.contains(actionId);}
        @Override public void onPress(){if(actionId==null || (technique()? !pending && seal()>=required(actionId):unavailable(actionId)==null))callback.run();}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            Component reason=actionId==null?null:unavailable(actionId);boolean enabled=reason==null;
            if(technique()){selected=actionId.equals(state.getString("SelectedTechnique"));enabled=!pending && seal()>=required(actionId);}
            boolean danger=actionId!=null && actionId.startsWith("free");
            CultivationGuiTextures.drawButton(g,getX(),getY(),width,height,enabled,isHoveredOrFocused() || selected);
            g.renderOutline(getX(),getY(),width,height,danger?0xFF9D5148:isHoveredOrFocused() || selected?0xFFE6BA73:EDGE);
            if(selected)g.fill(getX()+3,getY()+height-3,getX()+width-3,getY()+height-2,0xFFE6BA73);
            int ty=getY()+(actionId==null?(height-8)/2:4);
            fit(g,getMessage(),getX()+5,ty,width-10,enabled?(danger?0xFFFFC1A7:0xFFF8F3DF):0xFFC5C2B3);
            if(actionId!=null) {
                Component detail=selected && technique()?tr("selected"):reason!=null?reason:technique()?tr("select"):cost(actionId)>0?tr("cost",cost(actionId)):tr("ready");
                fit(g,detail,getX()+5,getY()+height-11,width-10,enabled?0xFFACDBC3:0xFFC5C2B3);
            }
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
    }
}
