package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.XiujianItem;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientCombatController {
    private static boolean charging;
    private static int chargeTicks;
    private static int targetEntityId = -1;
    private static long targetBlockPos = Long.MIN_VALUE;
    private static boolean blocking;
    private static boolean registered;

    private ClientCombatController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(ClientCombatController::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(ClientCombatController::onScreenInit);
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.screen != null) {
            return;
        }
        if (CombatStateManager.isCombatWeapon(
                minecraft.player.getMainHandItem())
                && ownsAttackInput(minecraft)
                && isBlockInputDown(minecraft)) {
            event.setCanceled(true);
            ThoughtInputGuard.handled(event);
            event.setSwingHand(false);
            updateBlocking(true);
            return;
        }
        if (!event.isAttack()) {
            return;
        }
        if (!CombatStateManager.isCombatWeapon(
                minecraft.player.getMainHandItem())
                && !XiujianItem.canFire(minecraft.player)) {
            return;
        }
        if (!ownsAttackInput(minecraft)) {
            if (ClientData.combatActive()
                    && ClientData.outwardQiRelease()
                    && QiInfusedWeapon.hasQi(minecraft.player.getMainHandItem())) {
                PacketDistributor.sendToServer(
                        new CombatPayloads.ServerboundWeaponQiSlashPayload());
            }
            return;
        }
        event.setCanceled(true);
        ThoughtInputGuard.handled(event);
        event.setSwingHand(false);
        if (!charging) {
            charging = true;
            chargeTicks = 0;
            updateTarget(minecraft);
            ClientData.updateHeavyAttackCharge(0, CombatStateManager.HEAVY_CHARGE_TICKS);
            PacketDistributor.sendToServer(new CombatPayloads.ServerboundCombatAttackPayload(
                    CombatPayloads.ATTACK_BEGIN, targetEntityId, targetBlockPos));
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        updateDisconnectButtons(event.getScreen());
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.screen instanceof PauseScreen pauseScreen) {
            updateDisconnectButtons(pauseScreen);
        }
        boolean shouldBlock = minecraft.player != null
                && minecraft.getConnection() != null
                && minecraft.player.isAlive()
                && minecraft.screen == null
                && CombatStateManager.isCombatWeapon(
                        minecraft.player.getMainHandItem())
                && ownsAttackInput(minecraft)
                && isBlockInputDown(minecraft);
        updateBlocking(shouldBlock);
        if (blocking) {
            if (charging) {
                interruptHeavyAttack();
            }
            return;
        }
        if (!charging) {
            return;
        }
        if (minecraft.player == null
                || minecraft.getConnection() == null
                || !minecraft.player.isAlive()) {
            interruptHeavyAttack();
            return;
        }

        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (!attackDown) {
            updateTarget(minecraft);
            PacketDistributor.sendToServer(new CombatPayloads.ServerboundCombatAttackPayload(
                    chargeTicks >= CombatStateManager.HEAVY_CHARGE_TICKS
                            ? CombatPayloads.ATTACK_HEAVY
                            : CombatPayloads.ATTACK_NORMAL,
                    targetEntityId,
                    targetBlockPos));
            resetCharge();
            return;
        }

        updateTarget(minecraft);
        chargeTicks = Math.min(CombatStateManager.HEAVY_CHARGE_TICKS, chargeTicks + 1);
        ClientData.updateHeavyAttackCharge(
                chargeTicks, CombatStateManager.HEAVY_CHARGE_TICKS);
    }

    public static boolean ownsAttackInput(Minecraft minecraft) {
        return minecraft.player != null
                && ClientData.combatActive()
                && ClientData.combatStateEnabled()
                && (CombatStateManager.isCombatWeapon(
                        minecraft.player.getMainHandItem())
                || XiujianItem.canFire(minecraft.player));
    }

    public static void onCombatStateUpdated(boolean active, boolean broken) {
        if (!active || broken) {
            updateBlocking(false);
            interruptHeavyAttack();
        }
    }

    public static void interruptHeavyAttack() {
        if (charging && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new CombatPayloads.ServerboundCombatAttackPayload(
                    CombatPayloads.ATTACK_CANCEL, -1, Long.MIN_VALUE));
        }
        resetCharge();
    }

    private static void resetCharge() {
        charging = false;
        chargeTicks = 0;
        targetEntityId = -1;
        targetBlockPos = Long.MIN_VALUE;
        ClientData.resetHeavyAttackCharge();
    }

    private static boolean isBlockInputDown(Minecraft minecraft) {
        return minecraft.options.keyAttack.isDown()
                && minecraft.options.keyUse.isDown();
    }

    private static void updateBlocking(boolean nextBlocking) {
        if (blocking == nextBlocking) {
            return;
        }
        if (nextBlocking && charging) {
            interruptHeavyAttack();
        }
        blocking = nextBlocking;
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(
                    new CombatPayloads.ServerboundSetBlockingPayload(blocking));
        }
    }

    private static void updateTarget(Minecraft minecraft) {
        targetEntityId = minecraft.hitResult instanceof EntityHitResult entityHit
                ? entityHit.getEntity().getId()
                : -1;
        targetBlockPos = minecraft.hitResult instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos().asLong()
                : Long.MIN_VALUE;
    }

    private static void updateDisconnectButtons(net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof PauseScreen)) {
            return;
        }
        String disconnect = Component.translatable("menu.disconnect").getString();
        String returnToMenu = Component.translatable("menu.returnToMenu").getString();
        boolean allowDisconnect = !ClientData.combatActive()
                || !ClientData.combatStateEnabled();
        for (var child : screen.children()) {
            if (child instanceof AbstractButton button) {
                String label = button.getMessage().getString();
                if (disconnect.equals(label) || returnToMenu.equals(label)) {
                    button.active = allowDisconnect;
                }
            }
        }
    }
}
