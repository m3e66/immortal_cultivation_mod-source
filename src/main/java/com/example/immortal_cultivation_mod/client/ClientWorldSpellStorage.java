package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

final class ClientWorldSpellStorage {
    private ClientWorldSpellStorage() {
    }

    static boolean ready() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.level != null && minecraft.getConnection() != null;
    }

    static String storageKey() {
        Minecraft minecraft = Minecraft.getInstance();
        String worldIdentity;
        if (minecraft.getSingleplayerServer() != null) {
            worldIdentity = "local:" + minecraft.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize();
        } else if (minecraft.getCurrentServer() != null) {
            worldIdentity = "server:" + minecraft.getCurrentServer().ip;
        } else if (minecraft.getConnection() != null) {
            worldIdentity = "remote:" + minecraft.getConnection().getConnection().getRemoteAddress();
        } else {
            worldIdentity = "disconnected";
        }
        String playerIdentity = minecraft.player == null
                ? "unknown"
                : minecraft.player.getUUID().toString();
        return UUID.nameUUIDFromBytes((worldIdentity + "|" + playerIdentity)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(ImmortalCultivationMod.MODID)
                .resolve("world_spell_lists")
                .resolve(storageKey());
    }
}
