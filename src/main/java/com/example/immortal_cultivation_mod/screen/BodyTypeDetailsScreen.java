package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.client.ClientData;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only explanation of the player's saved physique. */
public final class BodyTypeDetailsScreen extends CultivationTextScreen {
    public BodyTypeDetailsScreen(Screen parent) {
        super(Component.translatable("screen.immortal_cultivation_mod.body_details"), parent);
    }
    @Override protected List<Paragraph> paragraphs() {
        var data = ClientData.cultivationData;
        if (data == null) return List.of(paragraph(Component.translatable("screen.immortal_cultivation_mod.no_data")));
        return List.of(paragraph(BodyTypes.displayName(data.bodyType()), JADE),
                paragraph(BodyTypes.rarityName(data.bodyType()), GOLD),
                paragraph(BodyTypes.description(data.bodyType())));
    }
}
