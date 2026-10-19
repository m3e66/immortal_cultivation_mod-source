package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.alchemy.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Statistics detail page uses the synchronized server policy, including custom server balance. */
public final class PillToxicityScreen extends FittedMenuScreen {
    private final Screen parent;
    public PillToxicityScreen(Screen parent){super(Component.translatable("toxicity.title"),436,320);this.parent=parent;}
    @Override protected void initMenu() {
        addRenderableWidget(new CultivationGuiButton((width-100)/2,(height-308)/2+276,100,20,Component.translatable("gui.back"),this::onClose));
        addRenderableWidget(new CultivationGuiButton((width-424)/2+280,(height-308)/2+276,120,20,
                Component.translatable("toxicity.config.short"),()->minecraft.setScreen(new PillToxicitySettingsScreen(this))));
    }
    @Override protected void renderMenu(GuiGraphics g,int mouseX,int mouseY,float partial) {
        int x=(width-424)/2,y=(height-308)/2;
        g.fill(0,0,width,height,0xA0101716);CultivationGuiTextures.drawLandscape(g,x,y,424,308);
        g.drawString(font,title,x+212-font.width(title)/2,y+17,0xFF554A61,false);
        if(minecraft.player!=null) {
            var p=minecraft.player;drawDetails(g,PillToxicity.state(p),PillToxicity.policy(p),x,y);
        }
        super.renderMenu(g,mouseX,mouseY,partial);
    }
    public static int drawDetails(GuiGraphics g,net.minecraft.nbt.CompoundTag state,net.minecraft.nbt.CompoundTag rules,int x,int y) {
            var font=net.minecraft.client.Minecraft.getInstance().font;
            int stageIndex=PillToxicity.stage(state.getDouble("Value"),rules);var stage=rules.getCompound("Stage"+stageIndex);
            Component[] lines={
                Component.translatable("toxicity.value",PillData.number(state.getDouble("Value")),PillToxicity.stageName(stageIndex)),
                Component.translatable("toxicity.stats.restore",percent(stage,"Restore")),
                Component.translatable("toxicity.stats.benefit",percent(stage,"Benefit")),
                Component.translatable("toxicity.stats.natural",percent(stage,"Natural")),
                Component.translatable("toxicity.stats.cultivation",percent(stage,"Cultivation")),
                Component.translatable("toxicity.stats.cost",percent(stage,"Cost")),
                Component.translatable("toxicity.stats.damage",stageIndex==4?PillData.number(rules.getDouble("Damage")):"0",rules.getInt("DamageSeconds")),
                Component.translatable("toxicity.stats.recovery",PillData.number(rules.getDouble("Recovery")),PillData.number(rules.getDouble("Meditation")),PillData.number(state.getDouble("RecoveryRate"))),
                Component.translatable("toxicity.stats.detox",PillData.number(rules.getDouble("Detox")),rules.getInt("DetoxSeconds"),PillToxicity.realmName(rules.getInt("DetoxRealm")),Math.max(0,state.getLong("DetoxUntil")-state.getLong("Seconds"))),
                Component.translatable("toxicity.advice")};
            int lineY=y+43;
            for(var line:lines) {for(var wrapped:font.split(line,374)){g.drawString(font,wrapped,x+25,lineY,0xFF554A61,false);lineY+=12;}lineY+=4;}
            return lineY-y;
    }
    private static String percent(net.minecraft.nbt.CompoundTag stage,String key){return PillData.number(stage.getDouble(key)*100);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partial) {}
    @Override public boolean isPauseScreen(){return false;}
}
