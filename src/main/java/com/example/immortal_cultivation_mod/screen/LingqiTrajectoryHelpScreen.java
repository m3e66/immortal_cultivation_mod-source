package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LingqiTrajectoryHelpScreen extends CultivationTextScreen {
    private static final String PREFIX = "screen." + ImmortalCultivationMod.MODID + ".trajectory.help.";
    private static final List<LingqiTrajectoryConfig.NodeType> NODE_TYPES = List.of(
            LingqiTrajectoryConfig.NodeType.NORMAL,
            LingqiTrajectoryConfig.NodeType.START,
            LingqiTrajectoryConfig.NodeType.CORE,
            LingqiTrajectoryConfig.NodeType.SUB_CORE,
            LingqiTrajectoryConfig.NodeType.BLOCKED,
            LingqiTrajectoryConfig.NodeType.ELEMENT,
            LingqiTrajectoryConfig.NodeType.GATHER,
            LingqiTrajectoryConfig.NodeType.DRAIN,
            LingqiTrajectoryConfig.NodeType.STABILIZE,
            LingqiTrajectoryConfig.NodeType.SPLIT,
            LingqiTrajectoryConfig.NodeType.REVERSE,
            LingqiTrajectoryConfig.NodeType.UNSTABLE,
            LingqiTrajectoryConfig.NodeType.CONVERTER,
            LingqiTrajectoryConfig.NodeType.QI_CHANNEL,
            LingqiTrajectoryConfig.NodeType.TIMED,
            LingqiTrajectoryConfig.NodeType.TIMED_TARGET,
            LingqiTrajectoryConfig.NodeType.ROTATING,
            LingqiTrajectoryConfig.NodeType.LINKED,
            LingqiTrajectoryConfig.NodeType.SWITCH,
            LingqiTrajectoryConfig.NodeType.GATE,
            LingqiTrajectoryConfig.NodeType.FALSE_CORE);

    public LingqiTrajectoryHelpScreen(Screen parent) {
        super(Component.translatable(PREFIX + "title"), parent);
    }
    @Override protected List<Paragraph> paragraphs() {
        var result = new ArrayList<Paragraph>();
        result.add(paragraph(Component.translatable(PREFIX + "intro")));
        entry(result, "difficulty");
        entry(result, "realm_examples");
        for (var type : NODE_TYPES) entry(result, type.name().toLowerCase(Locale.ROOT));
        entry(result, "moving_hazard");
        return result;
    }
    private void entry(List<Paragraph> result, String key) {
        result.add(paragraph(Component.translatable(PREFIX + "node." + key + ".name"), JADE));
        result.add(paragraph(Component.translatable(PREFIX + "node." + key + ".description")));
    }
}
