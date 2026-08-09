package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MethodLearningScreen extends TechniqueDescriptionScreen {
    private final boolean active;

    public MethodLearningScreen(String methodId) {
        super(Kind.METHOD, methodId, null);
        this.active = methodId.equals(ClientData.cultivationData.activeCultivationMethod());
    }

    @Override
    @Nullable
    protected Component primaryActionLabel() {
        return CultivationMethods.get(techniqueId) != null && !active
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".activate_method_button")
                : null;
    }

    @Override
    protected void performPrimaryAction() {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundActivateMethodPayload(techniqueId));
        onClose();
    }

    @Override
    @Nullable
    protected Component statusText() {
        return active
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_active")
                : null;
    }
}
