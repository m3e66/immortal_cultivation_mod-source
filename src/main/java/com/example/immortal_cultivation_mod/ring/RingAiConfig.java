package com.example.immortal_cultivation_mod.ring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.neoforged.fml.loading.FMLPaths;

/** Host-local configuration, deliberately outside synchronized NeoForge SERVER specs. */
public final class RingAiConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Settings cached;
    public enum Protocol { OPENAI, ANTHROPIC, GEMINI }
    public record Settings(boolean enabled, String endpoint, String model, String apiKey, Protocol protocol) {
        public Settings(boolean enabled, String endpoint, String model, String apiKey) {
            this(enabled, endpoint, model, apiKey, Protocol.OPENAI);
        }
        @Override public String toString() { return "RingAiSettings[enabled=" + enabled + ", key=REDACTED]"; }
    }
    private RingAiConfig() { }
    public static Path path() { return FMLPaths.CONFIGDIR.get().resolve("immortal_cultivation_mod-ring-ai.json"); }
    public static synchronized Settings get() {
        if (cached == null) {
            Settings defaults = new Settings(false, "https://api.openai.com/v1/chat/completions", "gpt-4.1-mini", "");
            try {
                cached = Files.exists(path()) ? validate(JSON.fromJson(Files.readString(path()), Settings.class)) : defaults;
            } catch (Exception ignored) { cached = defaults; }
        }
        return cached;
    }
    public static Settings validate(Settings value) {
        if (value == null || value.endpoint() == null || value.model() == null || value.apiKey() == null)
            throw new IllegalArgumentException("Invalid settings");
        URI uri = URI.create(value.endpoint().trim());
        boolean local = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || local && "http".equalsIgnoreCase(uri.getScheme())))
            throw new IllegalArgumentException("Use HTTPS, or localhost HTTP");
        String model = value.model().trim();
        String key = value.apiKey().trim();
        if (model.isEmpty() || model.length() > 128 || key.length() > 512 || key.contains("\n") || key.contains("\r"))
            throw new IllegalArgumentException("Invalid model or key");
        if (uri.getQuery() != null || value.endpoint().length() > 512)
            throw new IllegalArgumentException("Put credentials in the API key field, not the URL");
        return new Settings(value.enabled(), uri.toString(), model, key,
                value.protocol() == null ? Protocol.OPENAI : value.protocol());
    }
    public static synchronized void save(Settings value) throws IOException {
        Settings valid = validate(value);
        Files.createDirectories(path().getParent());
        Path temp = path().resolveSibling(path().getFileName() + ".tmp");
        Files.writeString(temp, JSON.toJson(valid), StandardCharsets.UTF_8);
        Files.move(temp, path(), StandardCopyOption.REPLACE_EXISTING);
        cached = valid;
    }
    public static String key(Settings settings) {
        if (!settings.apiKey().isBlank()) return settings.apiKey();
        String env = System.getenv(switch (settings.protocol()) {
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case GEMINI -> "GEMINI_API_KEY";
            default -> "OPENAI_API_KEY";
        });
        return env == null ? "" : env.trim();
    }
}
