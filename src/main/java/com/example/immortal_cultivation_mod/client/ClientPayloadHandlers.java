package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.client.hud.JindanTribulationOverlay;
import com.example.immortal_cultivation_mod.client.hud.MysteriousRingDialogueOverlay;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.network.RestrictionPayloads;
import com.example.immortal_cultivation_mod.network.DisguiseMaskPayloads;
import com.example.immortal_cultivation_mod.screen.DebugStatEditorScreen;
import com.example.immortal_cultivation_mod.screen.DisguiseMaskSkinScreen;
import com.example.immortal_cultivation_mod.screen.JindanBreakthroughScreen;
import com.example.immortal_cultivation_mod.screen.LearningMatchScreen;
import com.example.immortal_cultivation_mod.screen.LingqiTrajectoryScreen;
import com.example.immortal_cultivation_mod.screen.MethodLearningScreen;
import com.example.immortal_cultivation_mod.screen.NpcInteractionScreen;
import com.example.immortal_cultivation_mod.screen.NpcRelationshipDebugScreen;
import com.example.immortal_cultivation_mod.screen.NpcDataScreen;
import com.example.immortal_cultivation_mod.screen.ScrollLearningScreen;
import com.example.immortal_cultivation_mod.screen.RestrictionPuzzleScreen;
import com.example.immortal_cultivation_mod.screen.SpiritRootSelectionScreen;
import com.example.immortal_cultivation_mod.screen.SteleSpellEditorScreen;
import com.example.immortal_cultivation_mod.screen.YinYangCompassScreen;
import com.example.immortal_cultivation_mod.screen.WantedOrderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandlers {
    public static void handleXuanjinView(com.example.immortal_cultivation_mod.network.XuanjinPayloads.View payload,
                                        net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(()->com.example.immortal_cultivation_mod.screen.XuanjinScreen.accept(payload));
    }


    public static void handleOpenDisguiseMaskPicker(
            DisguiseMaskPayloads.ClientboundOpenSkinPicker payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new DisguiseMaskSkinScreen(payload.mainHand(), payload.selectedSkin())));
    }

    public static void openScrollLearning(String spellId) {
        Minecraft.getInstance().setScreen(new ScrollLearningScreen(spellId));
    }

    public static void openMethodLearning(String methodId) {
        Minecraft.getInstance().setScreen(new MethodLearningScreen(methodId));
    }

    public static void handleOpenDebugStatEditor(
            ModPayloads.ClientboundOpenDebugStatEditorPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new DebugStatEditorScreen(
                        payload.targetId(), payload.targetName(), payload.systemType(), payload.page())));
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
            Minecraft minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            boolean previewingOtherPlayer = minecraft.screen instanceof DebugStatEditorScreen editor
                    && player != null
                    && !editor.targetId().equals(player.getUUID());
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
                    payload.yuqiControlAllMode(),
                    payload.talent()
            );
            ClientData.cultivationData = data;
            ThoughtInputGuard.onThoughtSync();
            ClientData.playerIdentity = payload.playerIdentity();
            ClientData.jindanQuality = payload.jindanQuality();
            if (!previewingOtherPlayer) {
                ClientData.setLivingCorpse(payload.livingCorpse());
                ClientData.setYuqiControlAllMode(payload.yuqiControlAllMode());
                ClientData.reconcileWheelSelection(payload.knownSpells());
                if (player != null) {
                    ClientData.updateMeditationState(player.getUUID(), payload.isMeditating());
                }
            }
        });
    }

    public static void handleMeditationState(ModPayloads.ClientboundMeditationStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.updateMeditationState(payload.playerId(), payload.meditating()));
    }

    public static void handleCastAnimation(ModPayloads.ClientboundCastAnimationPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientData.beginCasting(payload.playerId());
            SwordComboAnimations.playSpellCast(
                    payload.playerId(), payload.spellId());
        });
    }

    public static void handleShieldData(ModPayloads.ClientboundShieldDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.updateShield(payload.amount(), payload.max(),payload.layers()));
    }

    public static void handleCombatState(
            CombatPayloads.ClientboundCombatStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientData.updateCombatState(
                    payload.active(),
                    payload.posture(),
                    payload.maxPosture(),
                    payload.remainingTicks(),
                    payload.broken());
            ClientCombatController.onCombatStateUpdated(payload.active(), payload.broken());
            if (payload.broken()) {
                ModClientEvents.interruptPreparedCharge();
            }
        });
    }

    public static void handleSwordComboAnimation(
            CombatPayloads.ClientboundSwordComboAnimationPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SwordComboAnimations.play(
                payload.playerId(), payload.slashIndex()));
    }

    public static void handleSpellCooldown(ModPayloads.ClientboundSpellCooldownPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.startSpellCooldown(payload.spellId(), payload.ticks()));
    }

    public static void handleInterruptSpellPreparation(
            ModPayloads.ClientboundInterruptSpellPreparationPayload payload,
            IPayloadContext ctx) {
        ctx.enqueueWork(ModClientEvents::interruptPreparedCharge);
    }

    public static void handleRingDialogue(
            ModPayloads.ClientboundRingDialoguePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> MysteriousRingDialogueOverlay.enqueue(
                payload.message(), payload.elder(), payload.durationTicks()));
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
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new JindanBreakthroughScreen(payload.assisted())));
    }

    public static void handleJindanTribulation(
            ModPayloads.ClientboundJindanTribulationPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> JindanTribulationOverlay.handleEvent(
                payload.event(), payload.durationTicks(), payload.intensity()));
    }

    public static void handleOpenLearningGame(
            ModPayloads.ClientboundOpenLearningGamePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean usesTrajectory = "spell".equals(payload.learningType())
                    || "method".equals(payload.learningType());
            Screen screen = usesTrajectory
                    ? new LingqiTrajectoryScreen(
                            payload.learningType(),
                            payload.targetId(),
                            payload.requiredPoints(),
                            payload.difficulty(),
                            payload.talent(),
                            payload.assisted(),
                            payload.playerLevel(),
                            payload.token())
                    : new LearningMatchScreen(
                            payload.learningType(),
                            payload.targetId(),
                            payload.requiredPoints(),
                            payload.difficulty(),
                            payload.talent(),
                            payload.assisted(),
                            payload.token());
            Minecraft.getInstance().setScreen(screen);
        });
    }

    public static void handleOpenSteleSpellEditor(
            ModPayloads.ClientboundOpenSteleSpellEditorPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new SteleSpellEditorScreen(payload.pos(), payload.spellId())));
    }

    public static void handleNpcProfile(ModPayloads.ClientboundNpcProfilePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            NpcClientData data = NpcClientData.from(payload.data());
            if (payload.data().getBoolean("DebugRelationship")) {
                if (mc.screen instanceof NpcRelationshipDebugScreen debugScreen
                        && debugScreen.npcEntityId() == data.entityId()) {
                    debugScreen.updateNpcData(data);
                } else {
                    mc.setScreen(new NpcRelationshipDebugScreen(data));
                }
            } else if (mc.screen instanceof NpcDataScreen npcScreen && npcScreen.npcEntityId() == data.entityId()) {
                npcScreen.updateNpcData(data);
            } else if (data.openMenu()) {
                mc.setScreen(new NpcInteractionScreen(data));
            }
        });
    }

    public static void handleDebugEyeState(ModPayloads.ClientboundDebugEyeStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.setDebugEyeEnabled(payload.enabled()));
    }

    public static void handleJindanFlightControl(
            ModPayloads.ClientboundJindanFlightControlPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientData.setJindanFlightControlActive(payload.active());
            JindanFlightEffects.setControlActive(payload.active());
        });
    }

    public static void handleJindanFlightSpeed(
            ModPayloads.ClientboundJindanFlightSpeedPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> JindanFlightEffects.updateSpeed(
                payload.selectedSpeed(), payload.changedLayers()));
    }

    public static void handleJindanFlightDive(
            ModPayloads.ClientboundJindanFlightDivePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> JindanFlightEffects.triggerDive(
                payload.impact(), payload.intensity()));
    }

    public static void handleDebugHintCount(ModPayloads.ClientboundDebugHintCountPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.setRestrictionHints(payload.hints()));
    }

    public static void handleWantedOrder(ModPayloads.ClientboundOpenWantedOrderPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new WantedOrderScreen(
                payload.skin(), payload.name(), payload.realm(), payload.rewardKey(), payload.rewardCount())));
    }

    public static void handleProtectivePendantActivation(
            ModPayloads.ClientboundProtectivePendantActivationPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().gameRenderer.displayItemActivation(
                new ItemStack(ModItems.PROTECTIVE_JADE_PENDANT.get())));
    }

    public static void handleOpenRestrictionPuzzle(
            RestrictionPayloads.ClientboundOpenPuzzle payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new RestrictionPuzzleScreen(
                payload.tablePos(),
                payload.token(),
                payload.firstLayer(),
                payload.attemptedLayers(),
                payload.difficulty(),
                payload.moveLimit(),
                payload.instabilityInterval(),
                payload.artifact(),
                payload.hintsRemaining(),
                payload.values(),
                payload.targets(),
                payload.flags(),
                payload.links())));
    }

    public static void handleRestrictionPuzzleState(
            RestrictionPayloads.ClientboundPuzzleState payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RestrictionPuzzleScreen screen) {
                screen.applyServerState(payload.token(), payload.values(), payload.movesUsed(),
                        payload.result(), payload.instabilityShifted(),
                        payload.hintsRemaining(), payload.hintUsed());
            }
        });
    }
}
