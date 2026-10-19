package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.config.ModClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local rendering preferences only; none of these settings affect combat strength. */
public final class BlastQiSettingsScreen extends FittedMenuScreen {
    private final Screen parent;
    public BlastQiSettingsScreen(Screen parent){super(Component.translatable("qi.vfx.settings"),436,320);this.parent=parent;}
    @Override protected void initMenu(){
        int x=(width-424)/2,y=(height-308)/2;
        addRenderableWidget(new CultivationGuiButton(x+32,y+63,360,24,qualityLabel(),()->{
            ModClientConfig.BLAST_QI_QUALITY.set((ModClientConfig.BLAST_QI_QUALITY.get()+1)%3);
            ModClientConfig.SPEC.save();rebuildWidgets();
        }));
        addRenderableWidget(new PreferenceSlider(x+32,y+101,false));
        addRenderableWidget(new PreferenceSlider(x+32,y+139,true));
        addRenderableWidget(new CultivationGuiButton((width-100)/2,y+276,100,20,Component.translatable("gui.back"),this::onClose));
    }
    private Component qualityLabel(){
        return Component.translatable("qi.vfx.quality.value",Component.translatable("qi.vfx.quality."+ModClientConfig.BLAST_QI_QUALITY.get()));
    }
    @Override protected void renderMenu(GuiGraphics g,int mx,int my,float partial){
        int x=(width-424)/2,y=(height-308)/2;
        g.fill(0,0,width,height,0xA0101716);CultivationGuiTextures.drawLandscape(g,x,y,424,308);
        g.drawString(font,title,x+212-font.width(title)/2,y+17,0xFF554A61,false);
        int textY=y+181;
        for(var line:font.split(Component.translatable("qi.vfx.description"),360)){g.drawString(font,line,x+32,textY,0xFF554A61,false);textY+=12;}
        super.renderMenu(g,mx,my,partial);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private static final class PreferenceSlider extends AbstractSliderButton{
        private final boolean opacity;
        PreferenceSlider(int x,int y,boolean opacity){
            super(x,y,360,24,Component.empty(),opacity?ModClientConfig.BLAST_QI_OPACITY.get():ModClientConfig.BLAST_QI_PARTICLE_BUDGET.get()/96D);
            this.opacity=opacity;updateMessage();
        }
        @Override protected void updateMessage(){setMessage(Component.translatable(opacity?"qi.vfx.opacity.value":"qi.vfx.particles.value",Math.round(value*(opacity?100:96))));}
        @Override protected void applyValue(){
            if(opacity)ModClientConfig.BLAST_QI_OPACITY.set(value);else ModClientConfig.BLAST_QI_PARTICLE_BUDGET.set((int)Math.round(value*96));
            ModClientConfig.SPEC.save();
        }
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){
            CultivationGuiTextures.drawButton(g,getX(),getY(),width,height,active,isHoveredOrFocused());
            var font=net.minecraft.client.Minecraft.getInstance().font;
            g.drawString(font,getMessage(),getX()+(width-font.width(getMessage()))/2,getY()+7,0xFFF8F3DF,false);
            g.fill(getX()+8,getY()+height-5,getX()+8+(int)((width-16)*value),getY()+height-3,0xFFB9ABD0);
        }
    }
}
