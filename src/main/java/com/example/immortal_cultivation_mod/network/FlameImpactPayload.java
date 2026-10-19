package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.example.immortal_cultivation_mod.flame.FlameShotEntity;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** One bounded impact event starts local VFX before the crater's block update packets arrive. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public record FlameImpactPayload(ResourceLocation dimension, UUID impactId, String flame, Vec3 center,
        float radius, float charge, long seed, int terrainState, boolean terrainDamaged) implements CustomPacketPayload {
    public static final float MAX_RADIUS = 60;
    public static final double OBSERVER_RADIUS = 192;
    public static final Type<FlameImpactPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "flame_impact"));

    public FlameImpactPayload {
        Objects.requireNonNull(dimension, "Impact dimension");
        Objects.requireNonNull(impactId, "Impact identity");
        Objects.requireNonNull(flame, "Impact flame");
        if (!finiteCenter(center) || !Float.isFinite(radius) || !Float.isFinite(charge))
            throw new IllegalArgumentException("Non-finite flame impact geometry");
        if (!FlameKind.fromId(flame).id().equals(flame))
            throw new IllegalArgumentException("Unknown flame impact kind");
        if (terrainState < 0 || terrainState >= Block.BLOCK_STATE_REGISTRY.size())
            throw new IllegalArgumentException("Unknown flame impact terrain state");
        radius = Math.clamp(radius, 1, MAX_RADIUS);
        charge = Math.clamp(charge, 0, 1);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, FlameImpactPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeUUID(payload.impactId);
                buffer.writeUtf(payload.flame, 32);
                buffer.writeVec3(payload.center);
                buffer.writeFloat(payload.radius);
                buffer.writeFloat(payload.charge);
                buffer.writeLong(payload.seed);
                buffer.writeVarInt(payload.terrainState);
                buffer.writeBoolean(payload.terrainDamaged);
            }, buffer -> new FlameImpactPayload(buffer.readResourceLocation(), buffer.readUUID(),
                    buffer.readUtf(32), buffer.readVec3(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readLong(), buffer.readVarInt(), buffer.readBoolean()));

    @Override public Type<FlameImpactPayload> type() { return TYPE; }

    /** Server-only factory is also usable by GameTests without loading any client renderer. */
    public static Optional<FlameImpactPayload> prepare(FlameShotEntity shot, HitResult hit, Vec3 center) {
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK
                || !(shot.level() instanceof ServerLevel level) || !(shot.getOwner() instanceof ServerPlayer owner)
                || shot.isRemoved() || !Float.isFinite(shot.charge()) || shot.charge() <= 0 || !finiteCenter(center)
                || !owner.isAlive() || owner.isRemoved() || owner.isSpectator() || owner.level() != level
                || !level.hasChunkAt(block.getBlockPos()) || level.isOutsideBuildHeight(block.getBlockPos()))
            return Optional.empty();
        boolean terrainDamaged = owner.getAbilities().mayBuild
                && BlockBreakingSettings.enabled(owner, BlockBreakingSettings.Category.FLAME_VESSELS);
        float radius = terrainDamaged ? shot.destructionRadius() : shot.blastRadius();
        if (!Float.isFinite(radius) || radius <= 0) return Optional.empty();
        UUID identity = shot.getUUID();
        long seed = com.example.immortal_cultivation_mod.flame.FlameCraterShape.seed(identity);
        return Optional.of(new FlameImpactPayload(level.dimension().location(), identity, shot.kind().id(), center,
                radius, shot.charge(), seed, Block.getId(level.getBlockState(block.getBlockPos())), terrainDamaged));
    }

    public static void send(ServerLevel level, FlameImpactPayload payload) {
        if (!level.dimension().location().equals(payload.dimension())) return;
        PacketDistributor.sendToPlayersNear(level, null, payload.center.x, payload.center.y, payload.center.z,
                OBSERVER_RADIUS, payload);
    }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) context.enqueueWork(() -> ClientHandler.accept(payload));
        });
    }

    private static boolean finiteCenter(Vec3 center) {
        return center != null && Double.isFinite(center.x) && Double.isFinite(center.y) && Double.isFinite(center.z)
                && Math.abs(center.x) <= 30_000_000 && Math.abs(center.y) <= 30_000_000 && Math.abs(center.z) <= 30_000_000;
    }

    private static final class ClientHandler {
        private static void accept(FlameImpactPayload payload) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.dimension().location().equals(payload.dimension()))
                com.example.immortal_cultivation_mod.client.flame.FlameImpactRenderer.accept(payload);
        }
    }
}
