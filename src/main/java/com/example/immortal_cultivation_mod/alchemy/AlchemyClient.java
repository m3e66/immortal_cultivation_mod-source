package com.example.immortal_cultivation_mod.alchemy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.item.FateAlchemyFormulaItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid="immortal_cultivation_mod",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class AlchemyClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){
        e.register(AlchemyContent.MENU.get(),AlchemyScreen::new);
        e.register(AlchemyContent.BOTTLE_MENU.get(),BottleScreen::new);
    }
    private static void slot(GuiGraphics g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,0xFF665841);
        g.fill(x,y,x+16,y+16,0xFFDBD2BB);
    }
    public static final class AlchemyScreen extends AbstractContainerScreen<AlchemyMenu>{
        public AlchemyScreen(AlchemyMenu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=246;inventoryLabelY=153;}
        @Override protected void init(){
            super.init();
            addRenderableWidget(Button.builder(Component.translatable("alchemy.brew"),
                b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);})
                .bounds(leftPos+105,topPos+30,62,20).build());
        }
        @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
            g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFF1E8D2);
            for(var s:menu.slots)slot(g,leftPos+s.x,topPos+s.y);
        }
        @Override protected void renderLabels(GuiGraphics g,int mx,int my){
            g.drawString(font,title,8,8,0x332819,false);
            var stats = ClientData.cultivationData;
            var skill = Component.translatable("screen.immortal_cultivation_mod.ding.alchemy_proficiency",
                    CultivationMethods.methodProficiencyLevel(stats, FateAlchemyFormulaItem.PROFICIENCY),
                    stats == null ? 0 : stats.methodProficiency(FateAlchemyFormulaItem.PROFICIENCY));
            g.pose().pushPose();
            g.pose().translate(8, 20, 0);
            float skillScale = Math.min(1, 160F / Math.max(1, font.width(skill)));
            g.pose().scale(skillScale, skillScale, 1);
            g.drawString(font, skill, 0, 0, 0x665841, false);
            g.pose().popPose();
            var formula=AlchemyChemistry.formula(menu.formulaId);
            int y=106;
            for(var entry:formula.required().entrySet()){
                g.drawString(font,Component.translatable("alchemy.plant."+entry.getKey()).append(" ×"+entry.getValue()),8,y,0x332819,false);y+=11;
            }
            if(AlchemyChemistry.matches(formula,menu.inputs())){
                var batch=AlchemyChemistry.brew(formula,menu.inputs());
                g.drawString(font,Component.translatable(batch.failed()?"alchemy.preview_ash":"alchemy.preview_stable",PillData.number(batch.stability())),
                        8,94,batch.failed()?0xAA2222:0x225533,false);
            }
            g.drawString(font,Component.translatable("alchemy.cost"),8,141,0x665841,false);
            g.drawString(font,playerInventoryTitle,8,153,0x665841,false);
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
    }
    public static final class BottleScreen extends AbstractContainerScreen<JadeBottleMenu>{
        public BottleScreen(JadeBottleMenu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=166;inventoryLabelY=73;}
        @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
            g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFDCEBDD);
            for(var s:menu.slots)slot(g,leftPos+s.x,topPos+s.y);
        }
        @Override protected void renderLabels(GuiGraphics g,int mx,int my){
            super.renderLabels(g,mx,my);
            g.drawString(font,Component.translatable("alchemy.capacity",menu.getSlot(0).getItem().getCount(),32),45,59,0x254830,false);
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
    }
}

