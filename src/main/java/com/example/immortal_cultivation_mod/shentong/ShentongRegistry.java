package com.example.immortal_cultivation_mod.shentong;

import com.example.immortal_cultivation_mod.curse.AnnihilationShentong;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;

/** Register special abilities here; the debug editor discovers their controls automatically. */
public final class ShentongRegistry {
    public static final int MAX_ABILITIES = 256;
    public static final int MAX_FIELDS = 64;
    // Bound the complete metadata snapshot well below the clientbound payload size limit.
    private static final int MAX_TOTAL_FIELDS = 512;
    private static final LinkedHashMap<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    static { register(AnnihilationShentong.definition()); }
    private ShentongRegistry() {}

    public record Field(String id, String nameKey, int min, int max, boolean toggle,
                        ToIntFunction<ServerPlayer> read, ObjIntConsumer<ServerPlayer> write) {
        public Field {
            validateId(id, 32);
            validateKey(nameKey);
            if (id.equals("add") || id.equals("remove") || min > max
                    || (toggle && (min != 0 || max != 1))) throw new IllegalArgumentException("Invalid shentong field bounds/id: " + id);
            Objects.requireNonNull(read); Objects.requireNonNull(write);
        }
    }

    public record Definition(String id, String nameKey, Predicate<ServerPlayer> owned,
                             Consumer<ServerPlayer> grant, Consumer<ServerPlayer> remove,
                             Consumer<ServerPlayer> sync, List<Field> fields) {
        public Definition {
            validateId(id, 96); validateKey(nameKey);
            Objects.requireNonNull(owned); Objects.requireNonNull(grant);
            Objects.requireNonNull(remove); Objects.requireNonNull(sync);
            fields = List.copyOf(fields);
            if (fields.size() > MAX_FIELDS || fields.stream().map(Field::id).distinct().count() != fields.size())
                throw new IllegalArgumentException("Too many or duplicate shentong fields: " + id);
        }
        public Field field(String id) { return fields.stream().filter(f -> f.id().equals(id)).findFirst().orElse(null); }
    }

    /** Call during common setup for each new special ability, alongside its gameplay registration. */
    public static synchronized void register(Definition definition) {
        Objects.requireNonNull(definition);
        if (DEFINITIONS.containsKey(definition.id())) throw new IllegalArgumentException("Duplicate shentong: " + definition.id());
        if (DEFINITIONS.size() >= MAX_ABILITIES) throw new IllegalStateException("Too many shentong definitions");
        if (DEFINITIONS.values().stream().mapToInt(d -> d.fields().size()).sum() + definition.fields().size() > MAX_TOTAL_FIELDS)
            throw new IllegalStateException("Too many shentong fields in registry");
        DEFINITIONS.put(definition.id(), definition);
    }
    public static synchronized Definition get(String id) { return DEFINITIONS.get(id); }
    public static synchronized List<Definition> all() { return List.copyOf(DEFINITIONS.values()); }
    private static void validateId(String id, int limit) {
        if (id == null || id.isBlank() || id.length() > limit || !id.matches("[a-z0-9_.:/-]+"))
            throw new IllegalArgumentException("Invalid shentong id: " + id);
    }
    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 256) throw new IllegalArgumentException("Invalid shentong translation key");
    }
}
