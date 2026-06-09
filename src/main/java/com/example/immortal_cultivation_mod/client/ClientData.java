package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientData {
    public static final int MAX_WHEEL_SPELLS = 8;
    public static ModAttachments.CultivationData cultivationData = ModAttachments.CultivationData.createDefault();
    private static final Set<UUID> MEDITATING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final LinkedHashSet<String> SELECTED_WHEEL_SPELLS = new LinkedHashSet<>();
    private static boolean wheelSelectionLoaded = false;
    private static boolean wheelSelectionFileExists = false;

    public static void updateMeditationState(UUID playerId, boolean meditating) {
        if (meditating) {
            MEDITATING_PLAYERS.add(playerId);
        } else {
            MEDITATING_PLAYERS.remove(playerId);
        }
    }

    public static boolean isPlayerMeditating(UUID playerId) {
        return MEDITATING_PLAYERS.contains(playerId);
    }

    public static boolean isSpellVisibleInWheel(String spellId) {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return SELECTED_WHEEL_SPELLS.contains(ModSpells.normalizeId(spellId));
        }
    }

    public static void toggleSpellVisibleInWheel(String spellId) {
        ensureWheelSelectionLoaded();
        String normalized = ModSpells.normalizeId(spellId);
        synchronized (SELECTED_WHEEL_SPELLS) {
            if (!SELECTED_WHEEL_SPELLS.remove(normalized) && SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS) {
                SELECTED_WHEEL_SPELLS.add(normalized);
            }
            saveWheelSelection();
        }
    }

    public static int selectedWheelSpellCount() {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return SELECTED_WHEEL_SPELLS.size();
        }
    }

    public static void reconcileWheelSelection(List<String> knownSpells) {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            Set<String> known = ConcurrentHashMap.newKeySet();
            for (String spell : knownSpells) {
                known.add(ModSpells.normalizeId(spell));
            }
            SELECTED_WHEEL_SPELLS.removeIf(spell -> !known.contains(spell));
            if (!wheelSelectionFileExists && SELECTED_WHEEL_SPELLS.isEmpty()) {
                for (String spell : knownSpells) {
                    if (SELECTED_WHEEL_SPELLS.size() >= MAX_WHEEL_SPELLS) {
                        break;
                    }
                    String normalized = ModSpells.normalizeId(spell);
                    if (ModSpells.get(normalized) != null) {
                        SELECTED_WHEEL_SPELLS.add(normalized);
                    }
                }
            }
        }
    }

    private static void ensureWheelSelectionLoaded() {
        if (wheelSelectionLoaded) {
            return;
        }
        synchronized (SELECTED_WHEEL_SPELLS) {
            if (wheelSelectionLoaded) {
                return;
            }
            Path path = wheelSelectionPath();
            wheelSelectionFileExists = Files.exists(path);
            if (wheelSelectionFileExists) {
                try {
                    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                        String normalized = ModSpells.normalizeId(line.trim());
                        if (!normalized.isEmpty() && ModSpells.get(normalized) != null && SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS) {
                            SELECTED_WHEEL_SPELLS.add(normalized);
                        }
                    }
                } catch (IOException ignored) {
                    SELECTED_WHEEL_SPELLS.clear();
                }
            }
            wheelSelectionLoaded = true;
        }
    }

    private static void saveWheelSelection() {
        Path path = wheelSelectionPath();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, SELECTED_WHEEL_SPELLS, StandardCharsets.UTF_8);
            wheelSelectionFileExists = true;
        } catch (IOException ignored) {
        }
    }

    private static Path wheelSelectionPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(ImmortalCultivationMod.MODID + "_spell_wheel.txt");
    }
}
