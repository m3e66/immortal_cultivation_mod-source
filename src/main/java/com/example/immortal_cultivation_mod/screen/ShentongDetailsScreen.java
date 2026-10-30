package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.ShentongClient;
import com.example.immortal_cultivation_mod.network.DebugShentongPayloads.AbilityState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The same registry-backed fields as the debug editor, presented without editing controls. */
public final class ShentongDetailsScreen extends CultivationTextScreen {
    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
            "immortal_cultivation_mod","textures/gui/shentong_button.png");
    public static final int ICON_SIZE = 64;
    private String selectedId;
    private int refreshTicks;
    private CultivationGuiButton previous, next;

    public ShentongDetailsScreen(Screen parent) {
        super(text("title"),parent);
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
        if (!chosen.id().equals(selectedId)) { selectedId=chosen.id(); resetTextScroll(); }
        return chosen;
    }
    private void change(int step) {
        var selected=selected();
        if (selected==null) return;
        var all=abilities();
        selectedId=all.get(Math.floorMod(all.indexOf(selected)+step,all.size())).id(); resetTextScroll();
    }
    @Override protected void initTextPage() {
        previous=addFooterButton(48,32,Component.literal("‹"),() -> change(-1));
        next=addFooterButton(344,32,Component.literal("›"),() -> change(1));
        addFooterButton(157,110,Component.translatable("gui.back"),this::onClose);
        ShentongClient.request();
    }
    @Override public void tick() {
        super.tick();
        if (++refreshTicks>=20) { refreshTicks=0; ShentongClient.request(); }
    }
    @Override protected List<Paragraph> paragraphs() {
        var chosen=selected();
        var all=abilities();
        previous.active=next.active=all.size()>1;
        previous.visible=next.visible=chosen!=null;
        if(chosen==null) return List.of(paragraph(text(ShentongClient.state()==null ? "loading" : "empty")));
        var result=new ArrayList<Paragraph>();
        result.add(paragraph(Component.translatable(chosen.nameKey()),0xFF5F4A82));
        result.add(paragraph(Component.literal((all.indexOf(chosen)+1)+" / "+all.size()),JADE));
        result.addAll(content(chosen));
        return result;
    }
    private List<Paragraph> content(AbilityState ability) {
        var lines=new ArrayList<Paragraph>();
        append(lines,text("stats"),0xFF9B7C43);
        for (var field : ability.fields()) {
            Component value=field.toggle() ? text(field.value()==0 ? "off" : "on") : Component.literal(Integer.toString(field.value()));
            append(lines,Component.translatable(field.nameKey()).append(": ").append(value),0xFF465A52);
        }
        if (ability.fields().isEmpty()) append(lines,text("no_stats"),0xFF756F63);
        append(lines,Component.empty(),0xFF38352E);
        append(lines,text("description"),0xFF9B7C43);
        String description=ability.nameKey()+".description";
        append(lines,Language.getInstance().has(description) ? Component.translatable(description) : text("no_description"),0xFF38352E);
        return lines;
    }
    private void append(List<Paragraph> lines,Component text,int color) {
        lines.add(paragraph(text,color));
    }
}
