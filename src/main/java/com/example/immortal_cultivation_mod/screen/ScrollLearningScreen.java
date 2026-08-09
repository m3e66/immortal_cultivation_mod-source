package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ScrollLearningScreen extends TechniqueDescriptionScreen {
    private final boolean alreadyLearned;

    public ScrollLearningScreen(String spellId) {
        super(Kind.SPELL, spellId, null);
        this.alreadyLearned = ClientData.cultivationData.knownSpells().contains(spellId);
    }

    @Override
    @Nullable
    protected Component primaryActionLabel() {
        return alreadyLearned
                ? null
                : Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learn_button");
    }

    @Override
    protected void performPrimaryAction() {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundLearnSpellPayload(techniqueId));
        onClose();
    }

    @Override
    @Nullable
    protected Component statusText() {
        return alreadyLearned
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_already_learned")
                : null;
    }
}
