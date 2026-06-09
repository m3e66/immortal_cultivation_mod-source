package com.example.immortal_cultivation_mod.effect;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhotonEffects {
    private static final String FIREBALL_FX = "immortal_cultivation_mod:fireball";
    private static final String LINGBENG_FX = "immortal_cultivation_mod:lingbeng";
    private static final String BEAM_FX = "immortal_cultivation_mod:beam";
    private static final String MEDITATING_FX = "immortal_cultivation_mod:meditating";
    private static final int BEAM_DURATION_TICKS = 20 * 10;
    private static final Map<UUID, BeamRemoval> BEAM_REMOVALS = new ConcurrentHashMap<>();

    private PhotonEffects() {
    }

    public static void fireballProjectile(Entity projectile) {
        if (projectile.level() instanceof ServerLevel serverLevel) {
            playEntityEffect(serverLevel, projectile);
        }
    }

    public static void lingbengExplosion(ServerLevel level, double x, double y, double z) {
        playBlockEffect(level, LINGBENG_FX, x, y, z, 1.6D);
    }

    public static void beamSpell(ServerLevel level, ServerPlayer caster) {
        beamSpell(level, caster, BEAM_DURATION_TICKS);
    }

    public static void beamSpell(ServerLevel level, ServerPlayer caster, int durationTicks) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        List<Entity> entities = level.getEntities(caster, new AABB(caster.blockPosition()).inflate(128.0D), entity -> true);
        entities.add(caster);
        sendEntityEffectToPlayer(caster, BEAM_FX, entities);
        BEAM_REMOVALS.put(caster.getUUID(), new BeamRemoval(level.getGameTime() + durationTicks, entities));
    }

    public static void meditatingStart(ServerLevel level, ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT, "photon fx %s entity @s", MEDITATING_FX);
        runPhotonCommand(level, player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    public static void meditatingStop(ServerPlayer player) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        for (ServerPlayer viewer : player.serverLevel().players()) {
            sendRemoveEntityEffectToPlayer(viewer, MEDITATING_FX, List.of(player));
        }
    }

    public static void tick(ServerPlayer player) {
        BeamRemoval removal = BEAM_REMOVALS.get(player.getUUID());
        if (removal == null || player.level().getGameTime() < removal.removeAtGameTime()) {
            return;
        }

        BEAM_REMOVALS.remove(player.getUUID());
        sendRemoveEntityEffectToPlayer(player, BEAM_FX, removal.entities());
    }

    private static void playBlockEffect(ServerLevel level, String fx, double x, double y, double z, double scale) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s block %s %s %s %s 0 true true false",
                fx, vec(x, y, z), vec(0, 0, 0), vec(0, 0, 0), vec(scale, scale, scale));

        var source = level.getServer().createCommandSourceStack()
                .withPermission(4)
                .withPosition(new net.minecraft.world.phys.Vec3(x, y, z))
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void playEntityEffect(ServerLevel level, Entity entity) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        String command = String.format(Locale.ROOT,
                "photon fx %s entity @e[type=immortal_cultivation_mod:fireball_projectile,limit=1,sort=nearest]",
                FIREBALL_FX);

        var source = entity.createCommandSourceStack()
                .withPermission(4)
                .withPosition(entity.position())
                .withSuppressedOutput();
        runPhotonCommand(level, source, command);
    }

    private static void runPhotonCommand(ServerLevel level, net.minecraft.commands.CommandSourceStack source, String command) {
        if (!ModList.get().isLoaded("photon")) {
            return;
        }

        try {
            level.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException ignored) {
        }
    }

    private static void sendEntityEffectToPlayer(ServerPlayer player, String fx, List<Entity> entities) {
        try {
            Object command = Class.forName("com.lowdragmc.photon.command.EntityEffectCommand").getConstructor().newInstance();
            command.getClass().getMethod("setLocation", ResourceLocation.class).invoke(command, ResourceLocation.parse(fx));
            command.getClass().getMethod("setEntities", List.class).invoke(command, entities);
            command.getClass().getMethod("setAllowMulti", boolean.class).invoke(command, true);
            PacketDistributor.sendToPlayer(player, (CustomPacketPayload) command);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static void sendRemoveEntityEffectToPlayer(ServerPlayer player, String fx, List<Entity> entities) {
        try {
            Object command = Class.forName("com.lowdragmc.photon.command.RemoveEntityEffectCommand").getConstructor().newInstance();
            command.getClass().getMethod("setEntities", List.class).invoke(command, entities);
            command.getClass().getMethod("setForce", boolean.class).invoke(command, true);
            command.getClass().getMethod("setLocation", ResourceLocation.class).invoke(command, ResourceLocation.parse(fx));
            PacketDistributor.sendToPlayer(player, (CustomPacketPayload) command);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static String vec(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", x, y, z);
    }

    private record BeamRemoval(long removeAtGameTime, List<Entity> entities) {
    }
}
