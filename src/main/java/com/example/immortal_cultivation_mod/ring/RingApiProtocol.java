package com.example.immortal_cultivation_mod.ring;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Explicit provider formats; keys never appear in URLs, prompts, or response diagnostics. */
public final class RingApiProtocol {
    private RingApiProtocol() { }

    public static HttpRequest request(RingAiConfig.Settings settings, String key, String profile,
                                      ListTag history, String question) {
        var valid = RingAiConfig.validate(settings);
        String endpoint = valid.endpoint();
        if (valid.protocol() == RingAiConfig.Protocol.GEMINI) {
            // The Gemini endpoint field is the API root, so changing Model updates the request URL.
            if (!valid.model().matches("[A-Za-z0-9._-]+")) throw new IllegalArgumentException("Invalid Gemini model");
            endpoint = endpoint.replaceAll("/+$", "") + "/models/" + valid.model() + ":generateContent";
        }
        var request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(35))
                .header("Content-Type", "application/json");
        switch (valid.protocol()) {
            case OPENAI -> request.header("Authorization", "Bearer " + key);
            case ANTHROPIC -> request.header("x-api-key", key).header("anthropic-version", "2023-06-01");
            case GEMINI -> request.header("x-goog-api-key", key);
        }
        return request.POST(HttpRequest.BodyPublishers.ofString(body(valid, profile, history, question))).build();
    }

    public static String body(RingAiConfig.Settings settings, String profile, ListTag history, String question) {
        JsonObject body = new JsonObject();
        JsonArray messages = new JsonArray();
        boolean gemini = settings.protocol() == RingAiConfig.Protocol.GEMINI;
        if (gemini) {
            body.add("systemInstruction", content(null, profile, true));
            JsonObject generation = new JsonObject(); generation.addProperty("maxOutputTokens", 768);
            body.add("generationConfig", generation);
        } else {
            body.addProperty("model", settings.model());
            if (settings.protocol() == RingAiConfig.Protocol.ANTHROPIC) {
                body.addProperty("max_tokens", 512); body.addProperty("system", profile);
            } else {
                // max_tokens works with the broad Chat Completions-compatible provider format.
                body.addProperty("max_tokens", 512);
                if ("api.openai.com".equalsIgnoreCase(URI.create(settings.endpoint()).getHost())) body.addProperty("store", false);
                messages.add(content("system", profile, false));
            }
        }
        for (Tag tag : history) if (tag instanceof CompoundTag turn) {
            messages.add(content("user", turn.getString("Question"), gemini));
            messages.add(content(gemini ? "model" : "assistant", turn.getString("Answer"), gemini));
        }
        messages.add(content("user", question, gemini));
        body.add(gemini ? "contents" : "messages", messages);
        return body.toString();
    }

    private static JsonObject content(String role, String text, boolean gemini) {
        JsonObject message = new JsonObject();
        if (role != null) message.addProperty("role", role);
        if (gemini) {
            JsonObject part = new JsonObject(); part.addProperty("text", text);
            JsonArray parts = new JsonArray(); parts.add(part); message.add("parts", parts);
        } else message.addProperty("content", text);
        return message;
    }

    public static String parse(RingAiConfig.Protocol protocol, String raw) {
        if (raw == null || raw.length() > 65536) throw new IllegalArgumentException("Invalid response size");
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        StringBuilder text = new StringBuilder();
        switch (protocol) {
            case OPENAI -> text.append(root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString());
            case ANTHROPIC -> {
                for (var block : root.getAsJsonArray("content")) {
                    var b = block.getAsJsonObject();
                    if ("text".equals(b.get("type").getAsString())) text.append(b.get("text").getAsString());
                }
            }
            case GEMINI -> {
                for (var part : root.getAsJsonArray("candidates").get(0).getAsJsonObject()
                        .getAsJsonObject("content").getAsJsonArray("parts")) {
                    var p = part.getAsJsonObject();
                    if (p.has("text") && !(p.has("thought") && p.get("thought").getAsBoolean()))
                        text.append(p.get("text").getAsString());
                }
            }
        }
        String answer = text.toString().replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").strip();
        if (answer.isBlank()) throw new IllegalArgumentException("Empty response");
        int end = Math.min(1600, answer.length());
        if (end < answer.length() && Character.isHighSurrogate(answer.charAt(end - 1))) end--;
        return answer.substring(0, end);
    }
}
