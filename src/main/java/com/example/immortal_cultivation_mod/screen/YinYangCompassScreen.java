package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.CompassNavigation;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Overview, readable numeric loupe and world-coordinate navigation share one transform. */
public class YinYangCompassScreen extends Screen {
    private static final String PREFIX="screen.immortal_cultivation_mod.compass.";
    private static final int WIDTH=460,HEIGHT=350,ZOOM_SIZE=9,CELL=16;
    private final int centerX,centerZ,step,size;
    private final List<Integer> values,qiValues;
    private Mode mode;
    private int openTicks,focusX,focusZ;
    private Button clearButton;

    public YinYangCompassScreen(int centerX,int centerZ,int step,int size,List<Integer> values,List<Integer> qiValues){
        super(Component.translatable("screen."+ImmortalCultivationMod.MODID+".yinyang_compass"));
        this.centerX=centerX;this.centerZ=centerZ;this.step=Mth.clamp(step,1,128);this.size=Mth.clamp(size,1,65);
        this.values=values==null?List.of():List.copyOf(values);this.qiValues=qiValues==null?List.of():List.copyOf(qiValues);
        mode=ClientData.yinYangCompassQiMode()?Mode.QI:Mode.YIN_YANG;focusX=focusZ=this.size/2;
    }
    private MenuViewport viewport(){return MenuViewport.fit(width,height,WIDTH,HEIGHT,1);}
    private int ox(){return (viewport().logicalWidth()-WIDTH)/2;}
    private int oy(){return (viewport().logicalHeight()-HEIGHT)/2;}
    private int spacing(){return Math.max(2,Math.min(6,198/size));}
    private int zoomSize(){return Math.min(ZOOM_SIZE,size);}
    private int zoomX(){return Mth.clamp(focusX-zoomSize()/2,0,size-zoomSize());}
    private int zoomZ(){return Mth.clamp(focusZ-zoomSize()/2,0,size-zoomSize());}
    @Override protected void init(){
        addRenderableWidget(Button.builder(modeLabel(),button->{
            mode=mode==Mode.QI?Mode.YIN_YANG:Mode.QI;ClientData.setYinYangCompassQiMode(mode==Mode.QI);button.setMessage(modeLabel());
        }).bounds(ox()+WIDTH-122,oy()+12,106,18).build());
        clearButton=addRenderableWidget(Button.builder(Component.translatable(PREFIX+"clear"),button->CompassNavigation.clear())
                .bounds(ox()+WIDTH-98,oy()+322,82,18).build());
    }
    @Override public void tick(){super.tick();openTicks++;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partial){}
    private void text(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x,y,color,false);}
    private void fitted(GuiGraphics g,Component text,int x,int y,int color,int maxWidth){
        float scale=Math.min(1,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);text(g,text,0,0,color);g.pose().popPose();
    }
    private void centered(GuiGraphics g,Component text,int x,int y,int color){text(g,text,x-font.width(text)/2,y,color);}
    private int[] overviewAt(double x,double y){
        int gx=(int)Math.floor((x-ox()-26)/spacing()),gz=(int)Math.floor((y-oy()-76)/spacing());
        return gx>=0&&gz>=0&&gx<size&&gz<size?new int[]{gx,gz}:null;
    }
    private int[] zoomAt(double x,double y){
        int gx=(int)Math.floor((x-ox()-252)/CELL),gz=(int)Math.floor((y-oy()-86)/CELL);
        return gx>=0&&gz>=0&&gx<zoomSize()&&gz<zoomSize()?new int[]{zoomX()+gx,zoomZ()+gz}:null;
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        var v=viewport();double x=v.toMenu(mouseX),y=v.toMenu(mouseY);
        if(super.mouseClicked(x,y,button))return true;
        var point=overviewAt(x,y);if(point==null)point=zoomAt(x,y);
        if(point!=null){
            if(button==0){focusX=point[0];focusZ=point[1];CompassNavigation.pin(worldX(point[0]),worldZ(point[1]),valueAt(point[0],point[1]),mode==Mode.QI);return true;}
            if(button==1){CompassNavigation.clear();return true;}
        }
        return false;
    }
    @Override public boolean mouseReleased(double x,double y,int button){var v=viewport();return super.mouseReleased(v.toMenu(x),v.toMenu(y),button);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        var view=viewport();int mx=(int)view.toMenu(mouseX),my=(int)view.toMenu(mouseY),x=ox(),y=oy(),s=spacing();
        var hovered=overviewAt(mx,my);if(hovered!=null){focusX=hovered[0];focusZ=hovered[1];}
        var zoomed=zoomAt(mx,my);var detail=zoomed!=null?zoomed:new int[]{focusX,focusZ};
        g.pose().pushPose();g.pose().scale((float)view.scale(),(float)view.scale(),1);
        g.fill(x,y,x+WIDTH,y+HEIGHT,0xED101820);g.fill(x,y,x+WIDTH,y+1,0xFF638B9D);
        text(g,title,x+16,y+16,0xFFF3F1DF);
        text(g,Component.translatable(PREFIX+"help"),x+16,y+41,0xFFBAC8CE);
        centered(g,Component.translatable(PREFIX+"overview"),x+26+size*s/2,y+61,0xFFE2E9ED);
        text(g,Component.translatable(PREFIX+"zoom",step),x+252,y+66,0xFFE2E9ED);
        int left=x+26,top=y+76;
        for(int gz=0;gz<size;gz++)for(int gx=0;gx<size;gx++){
            double distance=Math.hypot(gx-size/2,gz-size/2);
            float reveal=Mth.clamp(((openTicks+partial)*1.35F-(float)distance*1.65F)/6F,0,1);
            g.fill(left+gx*s,top+gz*s,left+gx*s+s-1,top+gz*s+s-1,reveal<.5?0xFF202B31:colorFor(valueAt(gx,gz)));
        }
        int fx=left+focusX*s,fz=top+focusZ*s;
        g.renderOutline(fx-1,fz-1,s+1,s+1,0xFFFFFFFF);
        var target=CompassNavigation.target();
        if(target!=null){
            double tx=(target.x()-centerX)/(double)step+size/2,tz=(target.z()-centerZ)/(double)step+size/2;
            if(tx>=0&&tz>=0&&tx<size&&tz<size)cross(g,left+(int)Math.round(tx*s)+s/2,top+(int)Math.round(tz*s)+s/2,0xFFFFD774);
        }
        if(minecraft!=null&&minecraft.player!=null){
            double px=(minecraft.player.getX()-centerX)/(double)step+size/2,pz=(minecraft.player.getZ()-centerZ)/(double)step+size/2;
            if(px>=0&&pz>=0&&px<size&&pz<size)cross(g,left+(int)Math.round(px*s)+s/2,top+(int)Math.round(pz*s)+s/2,0xFF3EF0FF);
        }
        int zx=zoomX(),zz=zoomZ();
        for(int gz=0;gz<zoomSize();gz++)for(int gx=0;gx<zoomSize();gx++){
            int px=x+252+gx*CELL,py=y+86+gz*CELL,value=valueAt(zx+gx,zz+gz),color=colorFor(value);
            g.fill(px,py,px+CELL-1,py+CELL-1,color);
            String number=Integer.toString(value);g.pose().pushPose();g.pose().translate(px+(CELL-1)/2F,py+4,0);g.pose().scale(.8F,.8F,1);
            int luminance=((color>>16&255)*299+(color>>8&255)*587+(color&255)*114)/1000;
            g.drawString(font,number,-font.width(number)/2,0,luminance>155?0xFF111820:0xFFFFFFFF,false);g.pose().popPose();
            if(zx+gx==detail[0]&&zz+gz==detail[1])g.renderOutline(px-1,py-1,CELL+1,CELL+1,0xFFFFD774);
        }
        text(g,Component.translatable("screen.immortal_cultivation_mod."+(mode==Mode.QI?"spirit_qi_here":"yin_qi_here"),valueAt(detail[0],detail[1])),x+252,y+242,0xFFF6E8B6);
        fitted(g,Component.translatable(PREFIX+"sample",worldX(detail[0]),worldZ(detail[1])),x+252,y+256,0xFFD2DFE5,192);
        var reading=CompassNavigation.reading();
        Component current=CompassNavigation.position();
        if(reading!=null)current=current.copy().append("   ").append(Component.translatable(PREFIX+"reading",reading.qi(),reading.yin()));
        fitted(g,current,x+16,y+285,0xFFE2EEF3,WIDTH-32);
        fitted(g,CompassNavigation.targetLabel(),x+16,y+303,0xFFFFD774,WIDTH-32);
        fitted(g,CompassNavigation.directions(),x+16,y+323,0xFFFFD774,WIDTH-130);
        clearButton.active=target!=null;
        super.render(g,mx,my,partial);g.pose().popPose();
    }
    private void cross(GuiGraphics g,int x,int y,int color){
        g.fill(x-4,y-1,x+5,y+2,0xFF081016);g.fill(x-1,y-4,x+2,y+5,0xFF081016);
        g.fill(x-3,y,x+4,y+1,color);g.fill(x,y-3,x+1,y+4,color);
    }
    private int worldX(int cell){return centerX+(cell-size/2)*step;}
    private int worldZ(int cell){return centerZ+(cell-size/2)*step;}
    private int valueAt(int x,int z){
        int index=z*size+x;var active=mode==Mode.QI?qiValues:values;
        return index<0||index>=active.size()?0:Mth.clamp(active.get(index),mode==Mode.QI?0:-15,15);
    }

    private int colorFor(int value) {
        if (mode == Mode.QI) {
            int clamped = Mth.clamp(value, 0, 15);
            if (clamped == 0) {
                return 0xFFFFFFFF;
            }
            float t = 0.30F + 0.70F * clamped / 15.0F;
            int red = (int) (255.0F * (1.0F - t));
            int green = (int) (255.0F * (1.0F - t * 0.45F));
            return argb(255, red, green, 255);
        }
        int clamped = Mth.clamp(value, -15, 15);
        if (clamped == 0) {
            return 0xFFFFFFFF;
        }
        if (clamped > 0) {
            float t = 0.35F + 0.65F * clamped / 15.0F;
            int channel = (int) (255.0F * (1.0F - t));
            return argb(255, 255, channel, channel);
        }
        float t = 0.35F + 0.65F * -clamped / 15.0F;
        int red = (int) (255.0F - 35.0F * t);
        int green = (int) (255.0F - 70.0F * t);
        int blue = (int) (255.0F * (1.0F - t));
        return argb(255, red, green, blue);
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
    }

    private Component modeLabel() {
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + (mode == Mode.QI ? "mode_spirit_qi" : "mode_yin_yang_qi"));
    }

    private enum Mode {
        YIN_YANG,
        QI
    }
}
