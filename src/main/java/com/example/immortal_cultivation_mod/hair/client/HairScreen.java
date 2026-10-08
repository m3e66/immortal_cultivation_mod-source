package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.*;
import com.example.immortal_cultivation_mod.screen.CultivationGuiButton;
import com.example.immortal_cultivation_mod.screen.CultivationGuiTextures;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Draft-only editor: previews are local; Apply is checked by the open server menu. */
public final class HairScreen extends AbstractContainerScreen<HairMenu> {
    private static final int INK=0xFF293A32,MUTED=0xFF596052,EDGE=0xFF9A8E75,GOLD=0xFF997337;
    private static final int[] PRESETS={0x161820,0x2A1812,0x573824,0x96704C,0xDBC18A,0xF1EEE4,0x8C9096,0xAF452D,0x6F2028,0x152431};
    private static final String[] PRESET_NAMES={"black","dark_brown","brown","light_brown","blonde","white","gray","red","dark_red","blue_black"};
    private int style,color,helmet,tab,page,hovered=-1,face,zoom=16,selected=-1,selectedX=-1,selectedY=-1,request;
    private float hue,saturation,value,yaw=-18,pitch=6,previewZoom=1;
    private double uiScale=1;
    private boolean syncing,dragging,overlay,eyedropper=true,saveColor;
    private SkinPixels skin;
    private Component skinStatus=tr("skin.prompt");
    private String username="";
    private EditBox hex,red,green,blue,name;
    private ColorSlider hueSlider,saturationSlider,valueSlider;
    private final List<EditBox> fields=new ArrayList<>();
    public HairScreen(HairMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=620;imageHeight=348;
        var equipped=HairData.equipped(inventory.player,"hair");
        style=Math.max(0,HairData.style(equipped));color=HairData.color(equipped);helmet=HairData.helmet(equipped);
        setHsv();
    }
    private void setHsv(){float[] hsv=java.awt.Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,null);hue=hsv[0];saturation=hsv[1];value=hsv[2];}
    @Override protected void init() {
        uiScale=Math.min(1,Math.min(minecraft.getWindow().getGuiScaledWidth()/640.0,minecraft.getWindow().getGuiScaledHeight()/370.0));
        width=(int)(minecraft.getWindow().getGuiScaledWidth()/uiScale);height=(int)(minecraft.getWindow().getGuiScaledHeight()/uiScale);
        super.init();widgets();
    }
    public void selectTab(int tab){this.tab=tab;widgets();}
    public void selectStyle(int style){this.style=style;}
    public void setDraftColor(int rgb){setColor(rgb,false,null);}
    public int draftColor(){return color;}
    public void loadOwnSkin(){
        request++;
        try{skin=SkinReference.own();skinStatus=tr("skin.own");selected=selectedX=selectedY=-1;}
        catch(Exception e){skinStatus=SkinReference.errorMessage(e);}
        widgets();
    }
    private static Component tr(String key,Object... args){return Component.translatable("hair.ui."+key,args);}
    private CultivationGuiButton button(String key,int x,int y,int w,Runnable action){
        Component label=key.equals("+")||key.equals("-")?Component.literal(key):tr(key);
        return addRenderableWidget(new CultivationGuiButton(leftPos+x,topPos+y,w,20,label,action));
    }
    private void widgets() {
        clearWidgets();fields.clear();hex=red=green=blue=name=null;hueSlider=saturationSlider=valueSlider=null;
        String[] tabs={"styles","color","skin"};
        for(int i=0;i<tabs.length;i++){final int index=i;var b=button(tabs[i],200+i*132,42,129,()->selectTab(index));b.active=i!=tab;}
        button("-",126,66,24,()->previewZoom=Math.max(.6F,previewZoom-.1F));
        button("+",153,66,24,()->previewZoom=Math.min(1.4F,previewZoom+.1F));
        button("front",14,286,51,()->{yaw=0;pitch=0;});button("side",69,286,51,()->{yaw=90;pitch=0;});
        button("back",124,286,51,()->{yaw=180;pitch=0;});
        button("remove",14,318,105,()->style=-1);
        button("cancel",500,318,106,this::onClose);
        button("apply",386,318,106,()->PacketDistributor.sendToServer(new HairPayload(menu.containerId,style,color,helmet,saveColor)));
        String[] modes={"helmet.full","helmet.compact","helmet.hide"};
        button(modes[helmet],tab==2?402:200,286,tab==2?193:292,()->{helmet=(helmet+1)%3;widgets();});
        if(tab==0){button("previous",202,257,88,()->{page=0;widgets();}).active=page>0;
            button("next",506,257,88,()->{page=1;widgets();}).active=page<1;}
        if(tab==1)colorWidgets();
        if(tab==2)skinWidgets();
    }
    private EditBox field(int x,int y,int w,String initial,int limit) {
        var f=new EditBox(font,leftPos+x+4,topPos+y+4,w-8,10,Component.empty());
        f.setBordered(false);f.setTextColor(INK);f.setTextColorUneditable(MUTED);f.setTextShadow(false);
        f.setMaxLength(limit);f.setValue(initial);fields.add(f);addRenderableWidget(f);return f;
    }
    private void colorWidgets() {
        hueSlider=addRenderableWidget(new ColorSlider(0,200,132,234,hue));
        saturationSlider=addRenderableWidget(new ColorSlider(1,200,156,234,saturation));
        valueSlider=addRenderableWidget(new ColorSlider(2,200,180,234,value));
        hex=field(451,132,143,String.format("#%06X",color),7);hex.setResponder(s->{if(!syncing&&s.matches("#?[0-9a-fA-F]{6}"))setColor(Integer.parseInt(s.replace("#",""),16),false,hex);});
        red=field(451,180,43,Integer.toString(color>>16&255),3);
        green=field(501,180,43,Integer.toString(color>>8&255),3);
        blue=field(551,180,43,Integer.toString(color&255),3);
        for(var f:List.of(red,green,blue))f.setResponder(s->{
            if(syncing)return;
            try{int r=Integer.parseInt(red.getValue()),g=Integer.parseInt(green.getValue()),b=Integer.parseInt(blue.getValue());
                if(r>=0&&r<=255&&g>=0&&g<=255&&b>=0&&b<=255)setColor(r<<16|g<<8|b,false,f);
            }catch(NumberFormatException ignored){}
        });
        button(saveColor?"save_color.yes":"save_color.no",451,245,143,()->{saveColor=!saveColor;widgets();});
    }
    private void setColor(int rgb,boolean fromHsv,EditBox source) {
        color=rgb&0xFFFFFF;if(!fromHsv)setHsv();syncing=true;
        if(hex!=null&&hex!=source)hex.setValue(String.format("#%06X",color));
        if(red!=null&&red!=source)red.setValue(Integer.toString(color>>16&255));
        if(green!=null&&green!=source)green.setValue(Integer.toString(color>>8&255));
        if(blue!=null&&blue!=source)blue.setValue(Integer.toString(color&255));
        if(hueSlider!=null){hueSlider.sync(hue);saturationSlider.sync(saturation);valueSlider.sync(value);}
        syncing=false;
    }
    private final class ColorSlider extends AbstractSliderButton {
        private final int channel;
        ColorSlider(int channel,int x,int y,int w,double value){super(leftPos+x,topPos+y,w,18,Component.empty(),value);this.channel=channel;updateMessage();}
        void sync(double v){value=v;updateMessage();}
        @Override protected void updateMessage(){setMessage(tr(new String[]{"hue","saturation","brightness"}[channel],Math.round(value*(channel==0?360:100))));}
        @Override protected void applyValue(){if(channel==0)hue=(float)value;else if(channel==1)saturation=(float)value;else HairScreen.this.value=(float)value;setColor(Mth.hsvToRgb(hue,saturation,HairScreen.this.value),true,null);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){
            CultivationGuiTextures.drawButton(g,getX(),getY(),width,height,active,isHoveredOrFocused());
            int thumb=getX()+4+(int)Math.round(value*(width-16));
            CultivationGuiTextures.drawInnerBox(g,thumb,getY()+2,8,height-4);
            g.renderOutline(thumb,getY()+2,8,height-4,isHoveredOrFocused()?GOLD:EDGE);
            CultivationGuiTextures.drawButtonText(g,getMessage(),getX(),getY(),width,height,active);
        }
    }
    private void skinWidgets() {
        name=field(200,88,204,username,16);name.setResponder(s->username=s);
        button("search",411,87,83,this::search);button("own_skin",501,87,94,this::loadOwnSkin);
        String[] faces={"front","back","left","right","top"};
        for(int i=0;i<5;i++){int f=i;button(faces[i],200+i*49,114,47,()->{face=f;selectedX=selectedY=-1;});}
        button(overlay?"layer.outer":"layer.base",451,114,144,()->{overlay=!overlay;selectedX=selectedY=-1;widgets();});
        button("zoom_out",402,152,91,()->zoom=Math.max(8,zoom-2));button("zoom_in",501,152,94,()->zoom=Math.min(20,zoom+2));
        button(eyedropper?"eyedropper.on":"eyedropper.off",402,178,193,()->{eyedropper=!eyedropper;widgets();});
        button("use_color",402,245,193,()->{if(selected>=0)setColor(selected,false,null);}).active=selected>=0;
    }
    private void search() {
        int active=++request;String searchedName=username.trim();skinStatus=tr("skin.searching");
        SkinReference.search(searchedName).whenComplete((pixels,error)->minecraft.execute(()->{
            if(minecraft.screen!=this||active!=request)return;
            if(error!=null)skinStatus=SkinReference.errorMessage(error);
            else {skin=pixels;skinStatus=Component.literal(searchedName);selected=-1;selectedX=selectedY=-1;}
            widgets();
        }));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        int x=(int)(mx/uiScale),y=(int)(my/uiScale);hovered=tab==0?cardAt(x,y):-1;
        g.pose().pushPose();g.pose().scale((float)uiScale,(float)uiScale,1);super.render(g,x,y,partial);
        if(tab==1){int swatch=(x-leftPos-202)/25;if(y>=topPos+94&&y<topPos+114&&x>=leftPos+202&&swatch>=0&&swatch<10)g.renderTooltip(font,tr("preset."+PRESET_NAMES[swatch]),x,y);}
        g.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){renderBg(g,partial,x,y);}
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,leftPos+x,topPos+y,color,false);}
    private void text(GuiGraphics g,Component s,int x,int y,int color){g.drawString(font,s,leftPos+x,topPos+y,color,false);}
    private void wrapped(GuiGraphics g,Component text,int x,int y,int width,int color){
        for(var line:font.split(text,width)){g.drawString(font,line,x,y,color,false);y+=font.lineHeight;}
    }
    private void outline(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+1,c);g.fill(x,y+h-1,x+w,y+h,c);g.fill(x,y,x+1,y+h,c);g.fill(x+w-1,y,x+w,y+h,c);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(0,0,width,height,0xA0101716);
        CultivationGuiTextures.drawLandscape(g,leftPos,topPos,imageWidth,imageHeight);
        CultivationGuiTextures.drawInnerBox(g,leftPos+10,topPos+39,177,271);
        CultivationGuiTextures.drawInnerBox(g,leftPos+194,topPos+68,413,tab==2?242:212);
        var title=Component.translatable("hair.table");
        g.drawString(font,title,leftPos+(imageWidth-font.width(title))/2,topPos+14,INK,false);
        text(g,tr("preview_hint"),15,47,MUTED);
        g.fill(leftPos+16,topPos+68,leftPos+28,topPos+80,0xFF000000|color);text(g,String.format("#%06X",color),34,71,INK);
        preview(g,partial);
        int shown=hovered>=0?hovered:style;
        Component name=shown<0?tr("no_style"):Component.translatable(HairStyle.values()[shown].translation());
        wrapped(g,name,leftPos+16,topPos+261,159,INK);
        if(tab==0)styles(g,mx,my);
        if(tab==1)colors(g);
        if(tab==2)pixels(g);
        for(var field:fields)CultivationGuiTextures.drawInnerBox(g,field.getX()-4,field.getY()-4,field.getWidth()+8,18);
    }
    private void preview(GuiGraphics g,float partial) {
        g.flush();g.enableScissor(leftPos+12,topPos+90,leftPos+185,topPos+256);
        var pose=g.pose();pose.pushPose();pose.translate(leftPos+97,topPos+180,160);
        float scale=96*previewZoom;pose.scale(scale,scale,-scale);pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupForEntityInInventory();
        HairGeometry.head(pose,g.bufferSource(),SkinReference.ownTexture(),15728880);
        HairGeometry.render(pose,g.bufferSource(),15728880,hovered>=0?hovered:style,color,-1,helmet,false,
                minecraft.player.tickCount+partial,.08F,pitch);
        g.flush();pose.popPose();g.disableScissor();Lighting.setupFor3DItems();
    }
    private void styles(GuiGraphics g,int mx,int my) {
        for(int k=0;k<10;k++){
            int index=page*10+k,x=leftPos+201+k%5*79,y=topPos+80+k/5*85;
            CultivationGuiTextures.drawInnerBox(g,x,y,76,81);
            if(style==index)g.fill(x+2,y+2,x+74,y+79,0x44D5A63A);
            else if(hovered==index)g.fill(x+2,y+2,x+74,y+79,0x224D998B);
            outline(g,x,y,76,81,style==index?GOLD:hovered==index?0xFF4D998B:EDGE);
            g.pose().pushPose();g.pose().translate(x+22,y+5,0);g.pose().scale(2,2,1);
            g.renderItem(HairData.create(index,color,helmet),0,0);g.pose().popPose();
            wrapped(g,Component.translatable(HairStyle.values()[index].translation()),x+5,y+42,67,INK);
        }
        text(g,tr("page",page+1,2),355,263,INK);
    }
    private void swatches(GuiGraphics g,int[] colors,int y) {
        for(int i=0;i<colors.length;i++){int x=leftPos+202+i*25;g.fill(x,topPos+y,x+20,topPos+y+18,0xFF000000|colors[i]);outline(g,x,topPos+y,20,18,colors[i]==color?GOLD:EDGE);}
    }
    private void colors(GuiGraphics g) {
        text(g,tr("presets"),201,78,MUTED);swatches(g,PRESETS,94);
        text(g,tr("hex"),451,120,MUTED);text(g,tr("red_channel"),451,166,MUTED);text(g,tr("green_channel"),501,166,MUTED);text(g,tr("blue_channel"),551,166,MUTED);
        text(g,tr("recent"),201,208,MUTED);swatches(g,menu.colors(false),220);
        text(g,tr("saved"),201,244,MUTED);swatches(g,menu.colors(true),257);
    }
    private void pixels(GuiGraphics g) {
        text(g,tr("username"),201,75,MUTED);
        if(skin!=null){
            int x=leftPos+210,y=topPos+141;
            for(int py=0;py<8;py++)for(int px=0;px<8;px++){
                int sample=skin.head(face,overlay,px,py);
                int c=(sample>>>24)==0?((px+py)%2==0?0xFF657277:0xFF435058):sample;
                g.fill(x+px*zoom,y+py*zoom,x+(px+1)*zoom,y+(py+1)*zoom,c);
            }
            if(selectedX>=0)outline(g,x+selectedX*zoom,y+selectedY*zoom,zoom,zoom,GOLD);
            outline(g,x-1,y-1,zoom*8+2,zoom*8+2,EDGE);
        }else wrapped(g,skinStatus,leftPos+210,topPos+154,170,MUTED);
        if(selected>=0){text(g,tr("selected_hex",String.format("#%06X",selected)),402,207,INK);text(g,tr("selected_rgb",selected>>16&255,selected>>8&255,selected&255),402,224,MUTED);}
        else text(g,tr("pick_hint"),402,215,MUTED);
        text(g,font.plainSubstrByWidth(skinStatus.getString(),193),402,271,MUTED);
    }
    private int cardAt(double x,double y) {
        double rx=x-leftPos-201,ry=y-topPos-80;if(rx<0||rx>=395||ry<0||ry>=170)return -1;
        int col=(int)rx/79,row=(int)ry/85;if(rx%79>=76||ry%85>=81)return -1;return page*10+row*5+col;
    }
    private boolean inPreview(double x,double y){return x>=leftPos+12&&x<leftPos+185&&y>=topPos+90&&y<topPos+256;}
    @Override public boolean mouseClicked(double rawX,double rawY,int button) {
        double x=rawX/uiScale,y=rawY/uiScale;
        if(button==0&&inPreview(x,y)){dragging=true;return true;}
        if(button==0&&tab==0){int index=cardAt(x,y);if(index>=0){style=index;return true;}}
        if(button==0&&tab==1){
            int index=(int)(x-leftPos-202)/25;
            if(x>=leftPos+202&&index>=0){
                int[] palette=y>=topPos+94&&y<topPos+114?PRESETS:y>=topPos+220&&y<topPos+238?menu.colors(false):y>=topPos+257&&y<topPos+275?menu.colors(true):new int[0];
                if(index<palette.length && (x-leftPos-202)%25<20){setColor(palette[index],false,null);return true;}
            }
        }
        if(button==0&&tab==2&&skin!=null&&eyedropper){
            int px=(int)Math.floor((x-leftPos-210)/zoom),py=(int)Math.floor((y-topPos-141)/zoom);
            if(px>=0&&px<8&&py>=0&&py<8){int sample=skin.head(face,overlay,px,py);selectedX=px;selectedY=py;
                selected=(sample>>>24)==0?-1:sample&0xFFFFFF;if(selected>=0)setColor(selected,false,null);widgets();return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(dragging){yaw+=dx/uiScale*.8;pitch=Mth.clamp(pitch+(float)(dy/uiScale*.6),-55,55);return true;}
        return super.mouseDragged(x/uiScale,y/uiScale,button,dx/uiScale,dy/uiScale);
    }
    @Override public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(inPreview(x/uiScale,y/uiScale)){previewZoom=Mth.clamp(previewZoom+(float)vertical*.08F,.6F,1.4F);return true;}
        return super.mouseScrolled(x/uiScale,y/uiScale,horizontal,vertical);
    }
    private EditBox focusedField(){return getFocused() instanceof EditBox f&&fields.contains(f)&&f.isFocused()?f:null;}
    public boolean isEditingText(){return focusedField()!=null;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        var field=focusedField();
        if(field==null||key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)return super.keyPressed(key,scan,modifiers);
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_TAB){
            int next=Math.floorMod(fields.indexOf(field)+(hasShiftDown()?-1:1),fields.size());
            field.setFocused(false);setFocused(fields.get(next));fields.get(next).setFocused(true);return true;
        }
        if(field==name&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)){search();return true;}
        field.keyPressed(key,scan,modifiers);
        // Printable keys arrive separately in charTyped. Do not let the container
        // interpret E, number keys, or rebound inventory keys while editing text.
        return true;
    }
    @Override public boolean charTyped(char character,int modifiers){var field=focusedField();if(field==null)return super.charTyped(character,modifiers);field.charTyped(character,modifiers);return true;}
    @Override public boolean keyReleased(int key,int scan,int modifiers){return focusedField()!=null||super.keyReleased(key,scan,modifiers);}
    @Override public void removed(){request++;super.removed();}
}
