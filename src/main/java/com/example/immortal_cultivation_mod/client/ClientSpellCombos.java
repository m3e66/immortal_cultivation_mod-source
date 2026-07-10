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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ClientSpellCombos {
    public static final String CAST_PREFIX = "combo:";

    private static final LinkedHashMap<String, SpellCombo> COMBOS = new LinkedHashMap<>();
    private static boolean loaded = false;

    private ClientSpellCombos() {
    }

    public static List<SpellCombo> combos() {
        ensureLoaded();
        synchronized (COMBOS) {
            return List.copyOf(COMBOS.values());
        }
    }

    public static SpellCombo get(String id) {
        ensureLoaded();
        synchronized (COMBOS) {
            return COMBOS.get(id);
        }
    }

    public static void save(String id, String name, List<String> spellIds, CastMode castMode) {
        ensureLoaded();
        String cleanId = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        String cleanName = name == null || name.isBlank() ? "Unnamed Combo" : name.trim();
        List<String> cleanSpells = spellIds.stream()
                .map(ModSpells::normalizeId)
                .filter(spell -> ModSpells.get(spell) != null && ModSpells.isComboEligible(spell))
                .toList();
        if (cleanSpells.isEmpty()) {
            return;
        }
        synchronized (COMBOS) {
            COMBOS.put(cleanId, new SpellCombo(cleanId, cleanName, cleanSpells,
                    castMode == null ? CastMode.SEQUENTIAL : castMode));
            saveAll();
        }
    }

    public static void remove(String id) {
        ensureLoaded();
        synchronized (COMBOS) {
            COMBOS.remove(id);
            saveAll();
        }
    }

    public static List<ModSpells.SpellDef> knownSpells(ModAttachments.CultivationData data) {
        if (data == null) {
            return List.of();
        }
        ArrayList<String> spellIds = new ArrayList<>(data.knownSpells() == null ? List.of() : data.knownSpells());
        for (String spellId : ModSpells.innateSpellIds()) {
            if (ModSpells.isInnateKnown(spellId, data)) {
                spellIds.add(spellId);
            }
        }
        return spellIds.stream()
                .map(ModSpells::normalizeId)
                .map(ModSpells::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public static List<ModSpells.SpellDef> comboEligibleSpells(ModAttachments.CultivationData data) {
        return knownSpells(data).stream()
                .filter(spell -> ModSpells.isComboEligible(spell.id()))
                .toList();
    }

    public static String topElements(SpellCombo combo) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String spellId : combo.spellIds()) {
            var spell = ModSpells.get(spellId);
            if (spell != null && spell.element() != null && !spell.element().isBlank()) {
                counts.merge(spell.element(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .reduce((a, b) -> a + " " + b)
                .orElse("-");
    }

    public static Component castModeDescription(SpellCombo combo) {
        String modeKey = combo.castMode() == CastMode.SEQUENTIAL
                ? "combo_mode_sequential"
                : "combo_mode_simultaneous";
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_description",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + modeKey));
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (COMBOS) {
            if (loaded) {
                return;
            }
            Path path = comboPath();
            if (Files.exists(path)) {
                try {
                    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                        SpellCombo combo = parseLine(line);
                        if (combo != null) {
                            COMBOS.put(combo.id(), combo);
                        }
                    }
                } catch (IOException ignored) {
                    COMBOS.clear();
                }
            }
            loaded = true;
        }
    }

    private static SpellCombo parseLine(String line) {
        String[] parts = line.split("\t", 4);
        if (parts.length < 3 || parts[0].isBlank()) {
            return null;
        }
        String name;
        try {
            name = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        List<String> spells = new ArrayList<>();
        for (String spell : parts[2].split(",")) {
            String normalized = ModSpells.normalizeId(spell);
            if (ModSpells.get(normalized) != null && ModSpells.isComboEligible(normalized)) {
                spells.add(normalized);
            }
        }
        // Old combo files did not have a mode and cast every spell immediately.
        CastMode castMode = parts.length == 4 ? CastMode.fromSerialized(parts[3]) : CastMode.SIMULTANEOUS;
        return spells.isEmpty() ? null : new SpellCombo(parts[0], name, List.copyOf(spells), castMode);
    }

    private static void saveAll() {
        Path path = comboPath();
        List<String> lines = new ArrayList<>();
        for (SpellCombo combo : COMBOS.values()) {
            String encodedName = Base64.getEncoder().encodeToString(combo.name().getBytes(StandardCharsets.UTF_8));
            lines.add(combo.id() + "\t" + encodedName + "\t" + String.join(",", combo.spellIds())
                    + "\t" + combo.castMode().serializedName());
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path comboPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(ImmortalCultivationMod.MODID + "_spell_combos.txt");
    }

    public enum CastMode {
        SEQUENTIAL("sequential"),
        SIMULTANEOUS("simultaneous");

        private final String serializedName;

        CastMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static CastMode fromSerialized(String serializedName) {
            for (CastMode mode : values()) {
                if (mode.serializedName.equalsIgnoreCase(serializedName)) {
                    return mode;
                }
            }
            return SIMULTANEOUS;
        }
    }

    public record SpellCombo(String id, String name, List<String> spellIds, CastMode castMode) {
        public String castId() {
            return CAST_PREFIX + id;
        }
    }
}
