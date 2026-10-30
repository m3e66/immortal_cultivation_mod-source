package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.alchemy.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Statistics detail page uses the synchronized server policy, including custom server balance. */
public final class PillToxicityScreen extends CultivationTextScreen {
    public PillToxicityScreen(Screen parent) { super(Component.translatable("toxicity.title"), parent); }
    @Override protected void initTextPage() {
        addFooterButton(90, 100, Component.translatable("gui.back"), this::onClose);
        addFooterButton(208, 126, Component.translatable("toxicity.config.short"),
                () -> minecraft.setScreen(new PillToxicitySettingsScreen(this)));
    }
    @Override protected java.util.List<Paragraph> paragraphs() {
        if (minecraft.player == null) return java.util.List.of(paragraph(Component.translatable("screen.immortal_cultivation_mod.no_data")));
        return java.util.Arrays.stream(detailLines(PillToxicity.state(minecraft.player), PillToxicity.policy(minecraft.player)))
                .map(CultivationTextScreen::paragraph).toList();
    }
    public static Component[] detailLines(net.minecraft.nbt.CompoundTag state, net.minecraft.nbt.CompoundTag rules) {
            int stageIndex=PillToxicity.stage(state.getDouble("Value"),rules);var stage=rules.getCompound("Stage"+stageIndex);
            return new Component[]{
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
    }
    /** Compatibility preview uses the same padded content box as the live page. */
    public static int drawDetails(GuiGraphics g, net.minecraft.nbt.CompoundTag state, net.minecraft.nbt.CompoundTag rules, int x, int y) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int lineY = y + TEXT_Y;
        g.enableScissor(x + TEXT_X, lineY, x + TEXT_X + TEXT_W, lineY + TEXT_H);
        for (var line : detailLines(state, rules)) {
            for (var wrapped : font.split(line, TEXT_W)) {
                g.drawString(font, wrapped, x + TEXT_X, lineY, INK, false);
                lineY += 12;
            }
            lineY += 4;
        }
        g.disableScissor();
        return Math.min(TEXT_Y + TEXT_H, lineY - y);
    }
    private static String percent(net.minecraft.nbt.CompoundTag stage,String key){return PillData.number(stage.getDouble(key)*100);}
}
