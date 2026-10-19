package com.example.immortal_cultivation_mod.client;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Remembers chords until a fresh clean press, so queued clicks cannot leak after F3 release. */
public final class ModHotkeyGuard {
    private static final Set<Integer> PRESSED = new HashSet<>();
    private static final Set<Integer> BLOCKED = new HashSet<>();
    private static final Set<Integer> SHIFT_BLOCKED = new HashSet<>();
    private ModHotkeyGuard() {}

    public static void onKey(int key, int action, int modifiers) {
        if (action == GLFW.GLFW_RELEASE) {
            PRESSED.remove(key);
            return;
        }
        PRESSED.add(key);
        // Adding a modifier while a spell key is held must not create a new rising edge later.
        if (chordDown()) BLOCKED.addAll(PRESSED);
        if (shiftDown() || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) SHIFT_BLOCKED.addAll(PRESSED);
        if (action == GLFW.GLFW_PRESS) {
            if (chordDown() || (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) != 0) {
                BLOCKED.add(key);
            } else {
                if (BLOCKED.contains(key) || SHIFT_BLOCKED.contains(key)) {
                    var minecraft = net.minecraft.client.Minecraft.getInstance();
                    if (minecraft.options != null) for (var mapping : minecraft.options.keyMappings) {
                        if (ModKeyMappings.KEY_CATEGORY.equals(mapping.getCategory())
                                && mapping.getKey().getValue() == key) mapping.consumeClick();
                    }
                }
                BLOCKED.remove(key);
                if (!shiftDown() && (modifiers & GLFW.GLFW_MOD_SHIFT) == 0) SHIFT_BLOCKED.remove(key);
            }
        }
    }

    private static boolean chordDown() {
        return PRESSED.contains(GLFW.GLFW_KEY_F3) || physicalDebugKeyDown()
                || PRESSED.contains(GLFW.GLFW_KEY_LEFT_CONTROL) || PRESSED.contains(GLFW.GLFW_KEY_RIGHT_CONTROL)
                || PRESSED.contains(GLFW.GLFW_KEY_LEFT_ALT) || PRESSED.contains(GLFW.GLFW_KEY_RIGHT_ALT)
                || PRESSED.contains(GLFW.GLFW_KEY_LEFT_SUPER) || PRESSED.contains(GLFW.GLFW_KEY_RIGHT_SUPER);
    }

    private static boolean physicalDebugKeyDown() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null
                && com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_F3);
    }

    public static boolean allow(KeyMapping mapping) {
        return !ModKeyMappings.KEY_CATEGORY.equals(mapping.getCategory())
                || !chordDown() && !BLOCKED.contains(mapping.getKey().getValue())
                && (allowsShift(mapping) || !shiftDown() && !SHIFT_BLOCKED.contains(mapping.getKey().getValue()));
    }

    private static boolean shiftDown() {
        return PRESSED.contains(GLFW.GLFW_KEY_LEFT_SHIFT) || PRESSED.contains(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean allowsShift(KeyMapping mapping) {
        String name = mapping.getName();
        return name.endsWith(".cast_prepared_spell") || name.endsWith(".open_spell_wheel")
                || name.endsWith(".spiritual_sense") || name.endsWith(".debug_eye");
    }

    public static void reset() {
        PRESSED.clear();
        BLOCKED.clear();
        SHIFT_BLOCKED.clear();
    }
}
