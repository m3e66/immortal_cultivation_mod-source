package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientData {
    public static final int MAX_WHEEL_SPELLS = 8;
    public static ModAttachments.CultivationData cultivationData = ModAttachments.CultivationData.createDefault();
    private static final Set<UUID> MEDITATING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, Integer> CASTING_PLAYERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> SPELL_COOLDOWNS = new ConcurrentHashMap<>();
    private static final LinkedHashSet<String> SELECTED_WHEEL_SPELLS = new LinkedHashSet<>();
    private static boolean wheelSelectionLoaded = false;
    private static boolean wheelSelectionFileExists = false;
    private static float shieldAmount = 0.0F;
    private static float shieldMax = 0.0F;
    private static boolean yuqiControlAllMode = false;
    private static boolean yinYangCompassQiMode = false;
    private static boolean clientTogglesLoaded = false;
    private static String preparedSpellId = "";
    private static float preparedChargeScale = 1.0F;
    private static boolean preparedCharging = false;

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

    public static void beginCasting(UUID playerId) {
        CASTING_PLAYERS.put(playerId, 18);
    }

    public static boolean isPlayerCasting(UUID playerId) {
        Integer ticks = CASTING_PLAYERS.get(playerId);
        return ticks != null && ticks > 0;
    }

    public static void tickClientAnimations() {
        CASTING_PLAYERS.replaceAll((id, ticks) -> ticks - 1);
        CASTING_PLAYERS.entrySet().removeIf(entry -> entry.getValue() <= 0);
        SPELL_COOLDOWNS.replaceAll((id, ticks) -> ticks - 1);
        SPELL_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public static void startSpellCooldown(String spellId, int ticks) {
        String normalized = normalizeWheelId(spellId);
        if (!isValidWheelId(normalized) || ticks <= 0) {
            return;
        }
        SPELL_COOLDOWNS.put(normalized, ticks);
    }

    public static int spellCooldownTicks(String spellId) {
        String normalized = normalizeWheelId(spellId);
        return Math.max(0, SPELL_COOLDOWNS.getOrDefault(normalized, 0));
    }

    public static String preparedSpellId() {
        ensureClientTogglesLoaded();
        if (preparedSpellId == null || preparedSpellId.isBlank() || !isValidWheelId(preparedSpellId)) {
            return "";
        }
        return preparedSpellId;
    }

    public static void setPreparedSpellId(String spellId) {
        ensureClientTogglesLoaded();
        String normalized = normalizeWheelId(spellId);
        preparedSpellId = isValidWheelId(normalized) ? normalized : "";
        saveClientToggles();
    }

    public static Component preparedSpellName() {
        String spellId = preparedSpellId();
        if (spellId.isEmpty()) {
            return Component.empty();
        }
        if (spellId.startsWith(ClientSpellCombos.CAST_PREFIX)) {
            ClientSpellCombos.SpellCombo combo = ClientSpellCombos.get(spellId.substring(ClientSpellCombos.CAST_PREFIX.length()));
            return combo == null ? Component.empty() : Component.literal(combo.name());
        }
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        return spell == null ? Component.empty() : spell.name();
    }

    public static void updatePreparedCharge(float chargeScale, boolean charging) {
        preparedChargeScale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        preparedCharging = charging;
    }

    public static void resetPreparedCharge() {
        updatePreparedCharge(1.0F, false);
    }

    public static float preparedChargeScale() {
        return preparedChargeScale;
    }

    public static int preparedChargePercent() {
        return Math.round(preparedChargeScale * 100.0F);
    }

    public static boolean preparedCharging() {
        return preparedCharging;
    }

    public static void updateShield(float amount, float max) {
        shieldAmount = Math.max(0.0F, amount);
        shieldMax = Math.max(0.0F, max);
        if (shieldAmount <= 0.0F || shieldMax <= 0.0F) {
            shieldAmount = 0.0F;
            shieldMax = 0.0F;
        }
    }

    public static float shieldAmount() {
        return shieldAmount;
    }

    public static float shieldMax() {
        return shieldMax;
    }

    public static boolean yuqiControlAllMode() {
        ensureClientTogglesLoaded();
        return yuqiControlAllMode;
    }

    public static boolean toggleYuqiControlAllMode() {
        ensureClientTogglesLoaded();
        yuqiControlAllMode = !yuqiControlAllMode;
        saveClientToggles();
        return yuqiControlAllMode;
    }

    public static void setYuqiControlAllMode(boolean allMode) {
        ensureClientTogglesLoaded();
        yuqiControlAllMode = allMode;
        saveClientToggles();
    }

    public static boolean yinYangCompassQiMode() {
        ensureClientTogglesLoaded();
        return yinYangCompassQiMode;
    }

    public static void setYinYangCompassQiMode(boolean qiMode) {
        ensureClientTogglesLoaded();
        yinYangCompassQiMode = qiMode;
        saveClientToggles();
    }

    public static boolean isSpellVisibleInWheel(String spellId) {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return SELECTED_WHEEL_SPELLS.contains(normalizeWheelId(spellId));
        }
    }

    public static void toggleSpellVisibleInWheel(String spellId) {
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        if (!isValidWheelId(normalized)) {
            return;
        }
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

    public static List<String> selectedWheelSpellIds() {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            return List.copyOf(SELECTED_WHEEL_SPELLS);
        }
    }

    public static int selectedWheelSpellIndex(String spellId) {
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        synchronized (SELECTED_WHEEL_SPELLS) {
            int index = 0;
            for (String selected : SELECTED_WHEEL_SPELLS) {
                if (selected.equals(normalized)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    public static boolean canMoveWheelSpell(String spellId, int offset) {
        int index = selectedWheelSpellIndex(spellId);
        if (index < 0) {
            return false;
        }
        int target = index + offset;
        return target >= 0 && target < selectedWheelSpellCount();
    }

    public static void moveWheelSpell(String spellId, int offset) {
        ensureWheelSelectionLoaded();
        String normalized = normalizeWheelId(spellId);
        if (offset == 0) {
            return;
        }
        synchronized (SELECTED_WHEEL_SPELLS) {
            ArrayList<String> ordered = new ArrayList<>(SELECTED_WHEEL_SPELLS);
            int index = ordered.indexOf(normalized);
            if (index < 0) {
                return;
            }
            int target = Math.max(0, Math.min(ordered.size() - 1, index + offset));
            if (target == index) {
                return;
            }
            ordered.remove(index);
            ordered.add(target, normalized);
            SELECTED_WHEEL_SPELLS.clear();
            SELECTED_WHEEL_SPELLS.addAll(ordered);
            saveWheelSelection();
        }
    }

    public static void reconcileWheelSelection(List<String> knownSpells) {
        ensureWheelSelectionLoaded();
        synchronized (SELECTED_WHEEL_SPELLS) {
            Set<String> known = ConcurrentHashMap.newKeySet();
            for (String spell : knownSpells) {
                known.add(ModSpells.normalizeId(spell));
            }
            for (String spellId : ModSpells.innateSpellIds()) {
                if (ModSpells.isInnateKnown(spellId, cultivationData)) {
                    known.add(spellId);
                }
            }
            SELECTED_WHEEL_SPELLS.removeIf(spell -> !known.contains(spell) && !isValidComboWheelId(spell));
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
                for (String spellId : ModSpells.innateSpellIds()) {
                    if (SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS && known.contains(spellId)) {
                        SELECTED_WHEEL_SPELLS.add(spellId);
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
                        String normalized = normalizeWheelId(line.trim());
                        if (!normalized.isEmpty() && isValidWheelId(normalized) && SELECTED_WHEEL_SPELLS.size() < MAX_WHEEL_SPELLS) {
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

    private static void ensureClientTogglesLoaded() {
        if (clientTogglesLoaded) {
            return;
        }
        Path path = clientTogglesPath();
        if (Files.exists(path)) {
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
                yuqiControlAllMode = Boolean.parseBoolean(properties.getProperty("yuqiControlAllMode", "false"));
                yinYangCompassQiMode = Boolean.parseBoolean(properties.getProperty("yinYangCompassQiMode", "false"));
                String savedPreparedSpell = normalizeWheelId(properties.getProperty("preparedSpellId", ""));
                preparedSpellId = isValidWheelId(savedPreparedSpell) ? savedPreparedSpell : "";
            } catch (IOException ignored) {
            }
        }
        clientTogglesLoaded = true;
    }

    private static void saveClientToggles() {
        Path path = clientTogglesPath();
        Properties properties = new Properties();
        properties.setProperty("yuqiControlAllMode", Boolean.toString(yuqiControlAllMode));
        properties.setProperty("yinYangCompassQiMode", Boolean.toString(yinYangCompassQiMode));
        properties.setProperty("preparedSpellId", preparedSpellId == null ? "" : preparedSpellId);
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Immortal Cultivation client toggles");
            }
        } catch (IOException ignored) {
        }
    }

    private static Path clientTogglesPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(ImmortalCultivationMod.MODID + "_client_toggles.properties");
    }

    private static String normalizeWheelId(String id) {
        if (id != null && id.startsWith(ClientSpellCombos.CAST_PREFIX)) {
            return id.trim();
        }
        return ModSpells.normalizeId(id);
    }

    private static boolean isValidWheelId(String id) {
        return ModSpells.get(id) != null || isValidComboWheelId(id);
    }

    private static boolean isValidComboWheelId(String id) {
        return id != null
                && id.startsWith(ClientSpellCombos.CAST_PREFIX)
                && ClientSpellCombos.get(id.substring(ClientSpellCombos.CAST_PREFIX.length())) != null;
    }
}
