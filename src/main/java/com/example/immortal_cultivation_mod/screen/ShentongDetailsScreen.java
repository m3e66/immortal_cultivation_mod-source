package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.ShentongClient;
import com.example.immortal_cultivation_mod.network.DebugShentongPayloads.AbilityState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** The same registry-backed fields as the debug editor, presented without editing controls. */
public final class ShentongDetailsScreen extends FittedMenuScreen {
    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
            "immortal_cultivation_mod","textures/gui/shentong_button.png");
    public static final int ICON_SIZE = 64;
    private static final int WINDOW_W=436, WINDOW_H=330, CONTENT_H=186, LINE_H=13;
    private final Screen parent;
    private String selectedId;
    private int scroll, maxScroll, refreshTicks;
    private CultivationGuiButton previous, next;

    public ShentongDetailsScreen(Screen parent) {
        super(text("title"),WINDOW_W+12,WINDOW_H+12);
        this.parent=parent;
    }
    private static Component text(String key) { return Component.translatable("screen.immortal_cultivation_mod.shentong_"+key); }
    private List<AbilityState> abilities() {
        var state=ShentongClient.state();
        return state==null ? List.of() : state.abilities();
    }
    private AbilityState selected() {
        var all=abilities();
        if (all.isEmpty()) return null;
        var chosen=all.stream().filter(a -> a.id().equals(selectedId)).findFirst().orElse(all.getFirst());
        if (!chosen.id().equals(selectedId)) { selectedId=chosen.id(); scroll=0; }
        return chosen;
    }
    private void change(int step) {
        var selected=selected();
        if (selected==null) return;
        var all=abilities();
        selectedId=all.get(Math.floorMod(all.indexOf(selected)+step,all.size())).id(); scroll=0;
    }
    @Override protected void initMenu() {
        clearWidgets();
        int x=(width-WINDOW_W)/2, y=(height-WINDOW_H)/2;
        previous=addRenderableWidget(new CultivationGuiButton(x+24,y+50,24,22,Component.literal("‹"),() -> change(-1)));
        next=addRenderableWidget(new CultivationGuiButton(x+WINDOW_W-48,y+50,24,22,Component.literal("›"),() -> change(1)));
        addRenderableWidget(new CultivationGuiButton(x+(WINDOW_W-110)/2,y+297,110,20,Component.translatable("gui.back"),this::onClose));
        ShentongClient.request();
    }
    @Override public void tick() {
        super.tick();
        if (++refreshTicks>=20) { refreshTicks=0; ShentongClient.request(); }
    }
    @Override protected void renderMenu(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        int x=(width-WINDOW_W)/2, y=(height-WINDOW_H)/2;
        g.fill(0,0,width,height,0xA0101716);
        CultivationGuiTextures.drawLandscape(g,x,y,WINDOW_W,WINDOW_H);
        g.drawString(font,title,x+WINDOW_W/2-font.width(title)/2,y+17,0xFF38352E,false);
        var selected=selected(); var all=abilities();
        previous.active=next.active=all.size()>1;
        previous.visible=next.visible=selected!=null;
        if (selected==null) {
            var message=text(ShentongClient.state()==null ? "loading" : "empty");
            g.drawString(font,message,x+WINDOW_W/2-font.width(message)/2,y+142,0xFF756F63,false);
        } else {
            g.blit(ICON,x+61,y+46,30,30,0,0,ICON_SIZE,ICON_SIZE,ICON_SIZE,ICON_SIZE);
            Component name=Component.translatable(selected.nameKey());
            float scale=Math.min(1,275F/Math.max(1,font.width(name)));
            g.pose().pushPose(); g.pose().translate(x+101,y+56,0); g.pose().scale(scale,scale,1);
            g.drawString(font,name,0,0,0xFF5F4A82,false); g.pose().popPose();
            String count=(all.indexOf(selected)+1)+" / "+all.size();
            g.drawString(font,count,x+WINDOW_W/2-font.width(count)/2,y+79,0xFF756F63,false);
            g.fill(x+24,y+94,x+WINDOW_W-24,y+95,0xFFAAA18F);
            var lines=content(selected,WINDOW_W-64);
            maxScroll=Math.max(0,lines.size()*LINE_H-CONTENT_H); scroll=Math.clamp(scroll,0,maxScroll);
            g.enableScissor(x+26,y+101,x+WINDOW_W-26,y+101+CONTENT_H);
            for (int i=0; i<lines.size(); i++) {
                int lineY=y+103+i*LINE_H-scroll;
                if (lineY+LINE_H>=y+101 && lineY<y+101+CONTENT_H)
                    g.drawString(font,lines.get(i).text(),x+32,lineY,lines.get(i).color(),false);
            }
            g.disableScissor();
            if (maxScroll>0) {
                int trackX=x+WINDOW_W-27, trackY=y+102;
                int thumb=Math.max(18,CONTENT_H*CONTENT_H/(maxScroll+CONTENT_H));
                int thumbY=trackY+(CONTENT_H-thumb)*scroll/maxScroll;
                g.fill(trackX,trackY,trackX+2,trackY+CONTENT_H,0x3080796A);
                g.fill(trackX,thumbY,trackX+2,thumbY+thumb,0xFF728779);
            }
        }
        super.renderMenu(g,mouseX,mouseY,partialTick);
    }
    private record Line(FormattedCharSequence text,int color) {}
    private List<Line> content(AbilityState ability,int width) {
        var lines=new ArrayList<Line>();
        append(lines,text("stats"),width,0xFF9B7C43);
        for (var field : ability.fields()) {
            Component value=field.toggle() ? text(field.value()==0 ? "off" : "on") : Component.literal(Integer.toString(field.value()));
            append(lines,Component.translatable(field.nameKey()).append(": ").append(value),width,0xFF465A52);
        }
        if (ability.fields().isEmpty()) append(lines,text("no_stats"),width,0xFF756F63);
        append(lines,Component.empty(),width,0xFF38352E);
        append(lines,text("description"),width,0xFF9B7C43);
        String description=ability.nameKey()+".description";
        append(lines,Language.getInstance().has(description) ? Component.translatable(description) : text("no_description"),width,0xFF38352E);
        return lines;
    }
    private void append(List<Line> lines,Component text,int width,int color) {
        var wrapped=font.split(text,width);
        if (wrapped.isEmpty()) lines.add(new Line(FormattedCharSequence.EMPTY,color));
        else for (var line:wrapped) lines.add(new Line(line,color));
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy) {
        int next=Math.clamp(scroll-(int)Math.signum(dy)*LINE_H*3,0,maxScroll);
        if (next==scroll) return super.mouseScrolled(x,y,dx,dy);
        scroll=next; return true;
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
