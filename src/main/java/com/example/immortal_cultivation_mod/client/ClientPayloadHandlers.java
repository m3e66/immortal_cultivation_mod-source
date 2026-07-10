package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.screen.DebugStatEditorScreen;
import com.example.immortal_cultivation_mod.screen.JindanBreakthroughScreen;
import com.example.immortal_cultivation_mod.screen.MethodLearningScreen;
import com.example.immortal_cultivation_mod.screen.ScrollLearningScreen;
import com.example.immortal_cultivation_mod.screen.SpiritRootSelectionScreen;
import com.example.immortal_cultivation_mod.screen.YinYangCompassScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandlers {
    public static void openScrollLearning(String spellId) {
        Minecraft.getInstance().setScreen(new ScrollLearningScreen(spellId));
    }

    public static void openMethodLearning(String methodId) {
        Minecraft.getInstance().setScreen(new MethodLearningScreen(methodId));
    }

    public static void openDebugStatEditor() {
        Minecraft.getInstance().setScreen(new DebugStatEditorScreen());
    }

    public static void openSpiritRootSelectionOrMessage(Player player, Component alreadyCultivatingMessage) {
        var data = ClientData.cultivationData;
        if (data == null || com.example.immortal_cultivation_mod.attachment.CultivationLevels.isMortal(data.cultivationLevel())) {
            Minecraft.getInstance().setScreen(new SpiritRootSelectionScreen());
        } else {
            player.displayClientMessage(alreadyCultivatingMessage, true);
        }
    }

    public static void handleSyncPlayerData(ModPayloads.ClientboundSyncPlayerDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var data = new ModAttachments.CultivationData(
                    payload.qi(),
                    payload.cultivationLevel(),
                    payload.luck(),
                    payload.moral(),
                    payload.bodyType(),
                    payload.soul(),
                    payload.thoughts(),
                    payload.spiritRoots(),
                    payload.spiritRootGrade(),
                    payload.agePenalty(),
                    payload.cultivationProgress(),
                    payload.activeCultivationMethod(),
                    payload.blood(),
                    payload.knownSpells(),
                    payload.isMeditating(),
                    payload.skillPoints(),
                    payload.maxHpBonus(),
                    payload.maxQiBonus(),
                    payload.maxEnergyBonus(),
                    payload.physicalAttack(),
                    payload.magicAttack(),
                    payload.mentalAttack(),
                    payload.spellProficiencies(),
                    payload.methodProficiencies(),
                    payload.yuqiControlAllMode()
            );
            ClientData.cultivationData = data;
            ClientData.setYuqiControlAllMode(payload.yuqiControlAllMode());
            ClientData.reconcileWheelSelection(payload.knownSpells());
            var player = Minecraft.getInstance().player;
            if (player != null) {
                ClientData.updateMeditationState(player.getUUID(), payload.isMeditating());
            }
        });
    }

    public static void handleMeditationState(ModPayloads.ClientboundMeditationStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.updateMeditationState(payload.playerId(), payload.meditating()));
    }

    public static void handleCastAnimation(ModPayloads.ClientboundCastAnimationPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.beginCasting(payload.playerId()));
    }

    public static void handleShieldData(ModPayloads.ClientboundShieldDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.updateShield(payload.amount(), payload.max()));
    }

    public static void handleSpellCooldown(ModPayloads.ClientboundSpellCooldownPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.startSpellCooldown(payload.spellId(), payload.ticks()));
    }

    public static void handleYinYangCompass(ModPayloads.ClientboundYinYangCompassPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new YinYangCompassScreen(
                payload.centerX(),
                payload.centerZ(),
                payload.step(),
                payload.size(),
                payload.values(),
                payload.qiValues()
        )));
    }

    public static void handleOpenJindanChallenge(ModPayloads.ClientboundOpenJindanChallengePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new JindanBreakthroughScreen()));
    }
}
