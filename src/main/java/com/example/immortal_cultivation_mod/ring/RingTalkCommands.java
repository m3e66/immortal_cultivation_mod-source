package com.example.immortal_cultivation_mod.ring;

import com.example.immortal_cultivation_mod.item.MysteriousRingItem;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Session-only private chat routing. Commands remain normal Minecraft commands. */
@EventBusSubscriber(modid = "immortal_cultivation_mod")
public final class RingTalkCommands {
    static final class Session {
        private final UUID elder;
        final ArrayDeque<String> messages = new ArrayDeque<>();
        Session(UUID elder) { this.elder = elder; }
        UUID elder() { return elder; }
    }
    private static final Map<MinecraftServer, Map<UUID, Session>> SESSIONS = new ConcurrentHashMap<>();
    private RingTalkCommands() { }

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ring_talk")
                .then(Commands.literal("start").executes(c -> start(c.getSource().getPlayerOrException())))
                .then(Commands.literal("end").executes(c -> end(c.getSource().getPlayerOrException()))));
    }

    public static int start(ServerPlayer player) {
        var ring = MysteriousRingItem.findConversationRing(player, null);
        if (ring.isEmpty()) { tell(player, "sleeping"); return 0; }
        var config = RingAiConfig.get();
        if (!config.enabled()) { tell(player, "disabled"); return 0; }
        if (RingAiConfig.key(config).isBlank()) { tell(player, "missing_key"); return 0; }
        begin(player, MysteriousRingItem.elderId(player, ring));
        tell(player, "session_start");
        return 1;
    }

    // Separate lifecycle from provider configuration so routing can be tested without real credentials.
    static Session begin(ServerPlayer player, UUID elder) {
        Session existing = session(player);
        if (existing != null && existing.elder().equals(elder)) return existing;
        clear(player);
        Session session = new Session(elder);
        SESSIONS.computeIfAbsent(player.server, ignored -> new ConcurrentHashMap<>()).put(player.getUUID(), session);
        return session;
    }

    static Session session(ServerPlayer player) {
        var sessions = SESSIONS.get(player.server);
        return sessions == null ? null : sessions.get(player.getUUID());
    }

    static boolean active(ServerPlayer player, Session expected) { return expected == session(player); }

    public static int end(ServerPlayer player) {
        boolean existed = clear(player);
        tell(player, existed ? "session_end" : "session_inactive");
        return existed ? 1 : 0;
    }

    private static boolean clear(ServerPlayer player) {
        var sessions = SESSIONS.get(player.server);
        Session removed = sessions == null ? null : sessions.remove(player.getUUID());
        if (removed == null) return false;
        removed.messages.clear();
        RingAiService.cancel(player, removed.elder());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Session session = session(player);
        if (session == null) return;
        // Cancel before every validation/error path, so even unavailable AI cannot leak private chat.
        event.setCanceled(true);
        String text = event.getRawText();
        player.server.execute(() -> {
            if (!active(player, session)) return;
            if (!enqueue(session, text)) { tell(player, "queue_full"); return; }
            player.sendSystemMessage(Component.translatable("ring.ai.you", text));
            dispatch(player, session);
        });
    }

    static boolean enqueue(Session session, String text) {
        if (session.messages.size() >= 8) return false;
        session.messages.addLast(text);
        return true;
    }

    private static void dispatch(ServerPlayer player, Session session) {
        if (active(player, session) && !session.messages.isEmpty() && RingAiService.ready(player, session.elder()))
            RingAiService.ask(player, session.elder(), session.messages.removeFirst(), () -> active(player, session));
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var sessions = SESSIONS.get(event.getServer());
        if (sessions == null) return;
        sessions.forEach((id, session) -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) dispatch(player, session);
        });
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.remove(event.getServer()); }

    private static void tell(ServerPlayer player, String key) { player.sendSystemMessage(Component.translatable("ring.ai." + key)); }
}
