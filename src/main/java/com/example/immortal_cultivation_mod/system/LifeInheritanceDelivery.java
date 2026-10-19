package com.example.immortal_cultivation_mod.system;

import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Inventory, attachments and inheritance receipt commit in one atomic player snapshot. */
@EventBusSubscriber(modid = "immortal_cultivation_mod")
public final class LifeInheritanceDelivery {
    static final String REVISION = "LifeInheritanceSaveRevision";

    private LifeInheritanceDelivery() {}

    static Path playerFile(ServerPlayer player) {
        return player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
    }

    static boolean checkpoint(ServerPlayer player) {
        long previous = player.getPersistentData().getLong(REVISION);
        player.getPersistentData().putLong(REVISION, previous == Long.MAX_VALUE ? 1 : previous + 1);
        CompoundTag snapshot = player.saveWithoutId(new CompoundTag());
        Path target = playerFile(player), temporary = null;
        boolean committed = false;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), player.getStringUUID() + "-inheritance-", ".dat");
            NbtIo.writeCompressed(snapshot, temporary);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            if (!NbtIo.readCompressed(temporary, NbtAccounter.unlimitedHeap()).equals(snapshot)) {
                throw new java.io.IOException("Inheritance snapshot verification failed");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                net.minecraft.Util.safeReplaceFile(target, temporary, target.resolveSibling(player.getStringUUID() + ".dat_old"));
                if (!NbtIo.readCompressed(target, NbtAccounter.unlimitedHeap()).equals(snapshot)) {
                    throw new java.io.IOException("Inheritance replacement verification failed");
                }
            }
            committed = true;
        } catch (Exception failure) {
            // A completed replacement can survive an exception from a later file operation.
            try { committed = Files.isRegularFile(target) && NbtIo.readCompressed(target, NbtAccounter.unlimitedHeap()).equals(snapshot); }
            catch (Exception ignored) {}
            if (!committed) {
                player.getPersistentData().putLong(REVISION, previous);
                com.mojang.logging.LogUtils.getLogger().error("Could not checkpoint life inheritance for {}", player.getUUID(), failure);
            }
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) {}
        }
        if (committed) {
            // IntegratedPlayerList also caches the owner's NBT for level.dat. Keep that
            // cache current; multiplayer and test fixtures use the same canonical UUID file.
            try {
                player.server.getPlayerList().saveAll();
                // Vanilla may replace the same UUID file while refreshing its owner cache.
                // Flush that replacement too before the caller exposes the reward.
                try (FileChannel channel = FileChannel.open(target, StandardOpenOption.WRITE)) { channel.force(true); }
            }
            catch (Exception failure) { com.mojang.logging.LogUtils.getLogger().error("Could not refresh player save cache after inheritance", failure); }
        }
        return committed;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            Path file = playerFile(player);
            if (!Files.isRegularFile(file)) return;
            CompoundTag durable = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            CompoundTag persisted = durable.getCompound("NeoForgeData");
            // Singleplayer may load an older owner snapshot from level.dat after a crash.
            // Prefer its newer canonical UUID snapshot when it contains our committed revision.
            if (persisted.getLong(REVISION) > player.getPersistentData().getLong(REVISION)
                    && durable.hasUUID("UUID") && durable.getUUID("UUID").equals(player.getUUID())) {
                player.load(durable);
            }
        } catch (Exception failure) {
            com.mojang.logging.LogUtils.getLogger().error("Could not recover life inheritance for {}", player.getUUID(), failure);
        }
    }
}
