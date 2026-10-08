package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.ThoughtControlGuard;
import com.example.immortal_cultivation_mod.network.ThoughtControlPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Observes shared input operations, without relying on any mod ID or effect name. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class ThoughtInputGuard {
    private static ClientLevel world;
    private static int lastAttempt=-1000, sequence, pendingSequence=-1, predicted=-1;
    private static boolean wasActive;
    private static boolean restoringBindings;
    private static final Map<KeyMapping,InputConstants.Key> UNBOUND=new IdentityHashMap<>();
    private static final Map<KeyMapping, Press> PRESSES=new IdentityHashMap<>();
    private static final Set<InputEvent.InteractionKeyMappingTriggered> HANDLED=
            Collections.newSetFromMap(new IdentityHashMap<>());
    // Package access permits the isolated client harness to supply physical key transitions.
    static Predicate<InputConstants.Key> physicalInput=ThoughtInputGuard::readPhysical;
    private static final class Press { boolean down, consumed; }
    public record Movement(float left, float forward, boolean up, boolean down, boolean leftKey,
                           boolean rightKey, boolean jump, boolean shift) {
        public static Movement capture(Input input) {
            return new Movement(input.leftImpulse,input.forwardImpulse,input.up,input.down,input.left,input.right,input.jumping,input.shiftKeyDown);
        }
        public boolean restricted(Input input) {
            return left!=0 && (Math.abs(input.leftImpulse)<Math.abs(left)*.5 || input.leftImpulse*left<0)
                    || forward!=0 && (Math.abs(input.forwardImpulse)<Math.abs(forward)*.5 || input.forwardImpulse*forward<0)
                    || jump&&!input.jumping || shift&&!input.shiftKeyDown;
        }
        public void restore(Input input) {
            input.leftImpulse=left;input.forwardImpulse=forward;input.up=up;input.down=down;
            input.left=leftKey;input.right=rightKey;input.jumping=jump;input.shiftKeyDown=shift;
        }
    }
    private ThoughtInputGuard() {}
    public static boolean active() {
        var mc=Minecraft.getInstance();
        return mc.player!=null && mc.level!=null && mc.getConnection()!=null && mc.player.isAlive()
                && !mc.player.isSpectator() && mc.screen==null && mc.isWindowActive()
                && mc.mouseHandler.isMouseGrabbed();
    }
    private static boolean readPhysical(InputConstants.Key key) {
        long window=Minecraft.getInstance().getWindow().getWindow();
        if(key.getValue()<0)return false;
        return key.getType()==InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS
                : key.getType()==InputConstants.Type.KEYSYM && InputConstants.isKeyDown(window,key.getValue());
    }
    public static int availableThoughts() {
        int actual=ClientData.cultivationData.thoughts();
        return predicted<0 ? actual : Math.min(actual,predicted);
    }
    /** One bounded server charge covers all keys cancelled by the same continuing input ban. */
    public static boolean resist() {
        if(!active() || availableThoughts()<=0)return false;
        var mc=Minecraft.getInstance();
        if(mc.player.tickCount<lastAttempt || mc.player.tickCount-lastAttempt>=ThoughtControlGuard.REFRESH_INTERVAL) {
            lastAttempt=mc.player.tickCount;
            predicted=Math.max(0,availableThoughts()-ThoughtControlGuard.BASE_DAMAGE);
            pendingSequence=++sequence;
            PacketDistributor.sendToServer(new ThoughtControlPayloads.Attempt(sequence));
        }
        return availableThoughts()>0;
    }
    public static void accept(ThoughtControlPayloads.Balance packet) {
        if(packet.sequence()!=pendingSequence)return;
        predicted=Math.min(ClientData.cultivationData.thoughts(),Math.max(0,packet.thoughts()));pendingSequence=-1;
        if(predicted==ClientData.cultivationData.thoughts())predicted=-1;
    }
    public static void onThoughtSync() {
        if(pendingSequence<0)predicted=-1;
    }
    public static boolean keepKeyDown(KeyMapping key) {
        return active() && !toggleEnabled(key) && !key.isUnbound()
                && key.getKeyModifier().isActive(key.getKeyConflictContext())
                && physicalInput.test(key.getKey()) && resist();
    }
    public static boolean guardUnbind(KeyMapping key, InputConstants.Key replacement) {
        if(restoringBindings)return false;
        if(!active() || replacement.getValue()>=0) {UNBOUND.remove(key);return false;}
        if(key.isUnbound())return false;
        if(resist())return true;
        UNBOUND.putIfAbsent(key,key.getKey());
        return false;
    }
    public static boolean finishClick(KeyMapping key, boolean consumed) {
        if(!active())return consumed;
        if(key.isUnbound() || !key.getKeyConflictContext().isActive()
                || !key.getKeyModifier().isActive(key.getKeyConflictContext()))return consumed;
        Press state=PRESSES.computeIfAbsent(key,ignored->new Press());
        boolean down=physicalInput.test(key.getKey());
        if(!down) {state.down=false;state.consumed=false;return consumed;}
        if(!state.down) {state.down=true;state.consumed=false;}
        if(consumed) {state.consumed=true;return true;}
        if(!state.consumed && !toggleEnabled(key) && resist()) {
            state.consumed=true;
            return true;
        }
        return false;
    }
    public static void restoreMovement(Movement before, Input after) {
        var mc=Minecraft.getInstance();
        if(before==null || !active() || ClientData.cultivationData.isMeditating()
                || mc.player.isPassenger())return;
        if(before.restricted(after) && resist())before.restore(after);
    }
    public static Movement keyboardMovement(boolean slow, float factor) {
        if(!active())return null;
        var options=Minecraft.getInstance().options;
        boolean up=held(options.keyUp),down=held(options.keyDown),left=held(options.keyLeft),right=held(options.keyRight);
        float multiplier=slow?factor:1;
        return new Movement(((left?1:0)-(right?1:0))*multiplier,((up?1:0)-(down?1:0))*multiplier,
                up,down,left,right,held(options.keyJump),held(options.keyShift));
    }
    private static boolean held(KeyMapping key) {
        if(toggleEnabled(key))return key.isDown();
        return !key.isUnbound() && physicalInput.test(key.getKey())
                && key.getKeyModifier().isActive(key.getKeyConflictContext());
    }
    private static boolean toggleEnabled(KeyMapping key) {
        return key instanceof ToggleKeyMapping && ((com.example.immortal_cultivation_mod.mixin.ThoughtToggleKeyAccessor)key)
                .immortalCultivation$needsToggle().getAsBoolean();
    }
    private static void restoreBindings() {
        if(UNBOUND.isEmpty())return;
        restoringBindings=true;
        try {
            UNBOUND.forEach((key,original)->{if(key.isUnbound())key.setKey(original);});
            UNBOUND.clear();KeyMapping.resetMapping();
        } finally {restoringBindings=false;}
    }
    /** Consuming an input to perform a replacement action is not a control debuff. */
    public static void handled(InputEvent.InteractionKeyMappingTriggered event) { HANDLED.add(event); }
    public static void finishInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if(HANDLED.remove(event))return;
        if(event.isCanceled() && resist()) {event.setCanceled(false);event.setSwingHand(true);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var mc=Minecraft.getInstance();
        if(world!=mc.level) {
            restoreBindings();
            world=mc.level;PRESSES.clear();HANDLED.clear();lastAttempt=-1000;predicted=-1;pendingSequence=-1;wasActive=false;
        }
        if(availableThoughts()>0)restoreBindings();
        boolean enabled=active();
        for(KeyMapping key:mc.options.keyMappings) {
            Press state=PRESSES.computeIfAbsent(key,ignored->new Press());
            boolean down=enabled&&physicalInput.test(key.getKey());
            if(!down || !state.down)state.consumed=!wasActive;
            state.down=down;
        }
        wasActive=enabled;
        // Marker entries are normally removed at the end of onClickInput; do not retain abandoned events.
        HANDLED.clear();
    }
}
