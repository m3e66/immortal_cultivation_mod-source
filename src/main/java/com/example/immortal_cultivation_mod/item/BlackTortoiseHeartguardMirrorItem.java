package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.PlayerShieldManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Foundation Establishment-Golden Core emergency protection artifact.
 *
 * <p>The shield state is server-authoritative. Standard particles are used for
 * the shell lattice so the effect remains visible with shader packs.</p>
 */
public final class BlackTortoiseHeartguardMirrorItem extends Item implements ICurioItem {
    private static final float PASSIVE_DAMAGE_MULTIPLIER = 0.92F;
    private static final float TRIGGER_HEALTH_FRACTION = 0.20F;
    private static final float FULL_CAPACITY_FRACTION = 0.30F;
    private static final float EMPTY_QI_STRENGTH = 0.35F;
    private static final int ACTIVE_TICKS = 4 * 20;
    private static final int COOLDOWN_TICKS = 45 * 20;
    private static final double KNOCKBACK_RESISTANCE = 0.80D;
    private static final String SHIELD_SOURCE = "black_tortoise_heartguard_mirror";
    private static final ResourceLocation KNOCKBACK_MODIFIER = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "black_tortoise_heartguard_mirror_knockback");

    private static final DustParticleOptions JADE = new DustParticleOptions(
            new Vector3f(0.16F, 0.72F, 0.42F), 0.92F);
    private static final DustParticleOptions DEEP_JADE = new DustParticleOptions(
            new Vector3f(0.035F, 0.30F, 0.16F), 1.10F);
    private static final DustParticleOptions GREEN_GOLD = new DustParticleOptions(
            new Vector3f(0.76F, 0.78F, 0.25F), 0.82F);
    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    public BlackTortoiseHeartguardMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "waist_accessory".equals(slotContext.identifier())
                || "neck_accessory".equals(slotContext.identifier());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".black_tortoise_heartguard_mirror.defense")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Applies the passive reduction and absorbs the same hit when an emergency shield triggers. */
    public static float protectFromIncomingDamage(
            ServerPlayer player, DamageSource source, float incomingDamage) {
        if (incomingDamage <= 0.0F) {
            return incomingDamage;
        }
        if (!isEquipped(player)) {
            clear(player, false);
            return incomingDamage;
        }

        long now = player.serverLevel().getGameTime();
        ShieldState state = SHIELDS.get(player.getUUID());
        if (state != null && now >= state.expiresAt) {
            clear(player, true);
            state = null;
        }

        float reducedDamage = incomingDamage * PASSIVE_DAMAGE_MULTIPLIER;
        if (state == null
                && incomingDamage > player.getMaxHealth() * TRIGGER_HEALTH_FRACTION
                && !player.getCooldowns().isOnCooldown(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get())) {
            state = activate(player, now);
        }
        float damageAfterProtection = reducedDamage;
        if (state != null) {
            float pressureMultiplier = shieldPressureMultiplier(player, source);
            float absorbed = Math.min(reducedDamage, state.remaining / pressureMultiplier);
            if (absorbed > 0.0F) {
                state.remaining = Math.max(0.0F,
                        state.remaining - absorbed * pressureMultiplier);
                emitBlockedHit(player, source, state);
                if (state.remaining <= 0.001F) {
                    clear(player, true);
                } else {
                    PlayerShieldManager.set(
                            player, SHIELD_SOURCE, state.remaining, state.maximum);
                }
                damageAfterProtection = Math.max(0.0F, reducedDamage - absorbed);
            }
        }
        damageEquippedMirror(player, incomingDamage);
        return damageAfterProtection;
    }

    public static void tick(ServerPlayer player) {
        ShieldState state = SHIELDS.get(player.getUUID());
        if (state == null) {
            removeKnockbackModifier(player);
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (!player.isAlive() || !isEquipped(player) || now >= state.expiresAt) {
            clear(player, now >= state.expiresAt && player.isAlive());
            return;
        }
        applyKnockbackModifier(player);
        if (now % 4L == 0L) {
            emitShellLattice(player, state);
        }
    }

    public static void clear(ServerPlayer player) {
        clear(player, false);
    }

    public static boolean isActive(ServerPlayer player) {
        ShieldState state = SHIELDS.get(player.getUUID());
        return state != null && player.serverLevel().getGameTime() < state.expiresAt;
    }

    private static ShieldState activate(ServerPlayer player, long now) {
        var data = ModAttachments.getData(player);
        CultivationLevels.LevelDef level = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = Math.max(0, level.maxQi() + data.maxQiBonus());
        int fullQiCost = Math.max(25, Mth.ceil(maxQi * 0.08F));
        int availableQi = Math.max(0, data.qi());
        int spentQi = player.getAbilities().instabuild
                ? fullQiCost : Math.min(fullQiCost, availableQi);
        float paidFraction = fullQiCost <= 0 ? 1.0F : spentQi / (float) fullQiCost;
        float strength = Mth.lerp(paidFraction, EMPTY_QI_STRENGTH, 1.0F);
        float capacity = Math.max(1.0F,
                player.getMaxHealth() * FULL_CAPACITY_FRACTION * strength);

        if (!player.getAbilities().instabuild && spentQi > 0) {
            ModAttachments.setData(player, data.withQi(availableQi - spentQi));
        }
        ShieldState state = new ShieldState(capacity, capacity, now + ACTIVE_TICKS);
        SHIELDS.put(player.getUUID(), state);
        player.getCooldowns().addCooldown(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get(), COOLDOWN_TICKS);
        applyKnockbackModifier(player);
        PlayerShieldManager.set(player, SHIELD_SOURCE, capacity, capacity);
        emitActivation(player);
        player.displayClientMessage(Component.translatable(
                paidFraction >= 0.999F
                        ? "message." + ImmortalCultivationMod.MODID
                                + ".black_tortoise_heartguard_mirror.activated"
                        : "message." + ImmortalCultivationMod.MODID
                                + ".black_tortoise_heartguard_mirror.weak",
                Mth.floor(capacity), Mth.floor(strength * 100.0F)), true);
        ServerEvents.syncPlayerData(player);
        return state;
    }

    private static float shieldPressureMultiplier(ServerPlayer player, DamageSource source) {
        float multiplier = 1.0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            multiplier *= 0.68F;
        }
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            Vec3 horizontalLook = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            Vec3 toSource = sourcePosition.subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
            if (horizontalLook.lengthSqr() > 1.0E-5D && toSource.lengthSqr() > 1.0E-5D
                    && horizontalLook.normalize().dot(toSource.normalize()) > 0.15D) {
                multiplier *= 0.76F;
            }
        }
        return Math.max(0.50F, multiplier);
    }

    private static boolean isEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(
                        ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get()))
                .filter(result -> "waist_accessory".equals(result.slotContext().identifier())
                        || "neck_accessory".equals(result.slotContext().identifier()))
                .isPresent();
    }

    private static void damageEquippedMirror(ServerPlayer player, float incomingDamage) {
        int durabilityDamage = (int) Math.max(1.0F, incomingDamage / 4.0F);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findFirstCurio(ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get())
                        .ifPresent(result -> {
                            ItemStack damaged = result.stack().copy();
                            damaged.hurtAndBreak(
                                    durabilityDamage, player, EquipmentSlot.CHEST);
                            handler.setEquippedCurio(
                                    result.slotContext().identifier(),
                                    result.slotContext().index(),
                                    damaged);
                            if (damaged.isEmpty()) {
                                clear(player, true);
                            }
                        }));
    }

    private static void applyKnockbackModifier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null || attribute.getModifier(KNOCKBACK_MODIFIER) != null) {
            return;
        }
        attribute.addTransientModifier(new AttributeModifier(
                KNOCKBACK_MODIFIER, KNOCKBACK_RESISTANCE,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeKnockbackModifier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null && attribute.getModifier(KNOCKBACK_MODIFIER) != null) {
            attribute.removeModifier(KNOCKBACK_MODIFIER);
        }
    }

    private static void clear(ServerPlayer player, boolean shatter) {
        ShieldState removed = SHIELDS.remove(player.getUUID());
        removeKnockbackModifier(player);
        if (removed != null) {
            PlayerShieldManager.clear(player, SHIELD_SOURCE);
        }
        if (removed != null && shatter && player.isAlive()) {
            emitShatter(player);
        }
    }

    private static void emitActivation(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(JADE, center.x, center.y, center.z,
                34, 0.58D, 0.82D, 0.58D, 0.04D);
        emitSpectralTortoise(player);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 0.72F, 0.72F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.95F, 0.72F);
    }

    private static void emitShellLattice(ServerPlayer player, ShieldState state) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.50D, 0.0D);
        float integrity = Mth.clamp(state.remaining / state.maximum, 0.0F, 1.0F);
        int intactSegments = Math.max(1, Mth.ceil(18.0F * integrity));
        long phase = level.getGameTime() / 4L;
        for (int segment = 0; segment < intactSegments; segment++) {
            double theta = (Math.PI * 2.0D * segment / 18.0D) + phase * 0.035D;
            double y = ((segment % 3) - 1) * player.getBbHeight() * 0.32D;
            double radius = 0.82D + 0.13D * Math.cos(segment * Math.PI / 3.0D);
            Vec3 point = center.add(Math.cos(theta) * radius, y, Math.sin(theta) * radius);
            sendPoint(level, segment % 4 == 0 ? GREEN_GOLD : JADE, point);
        }
        for (int column = 0; column < 6; column++) {
            double theta = Math.PI * 2.0D * column / 6.0D + phase * 0.025D;
            for (int row = 0; row < 3; row++) {
                double y = (row - 1) * player.getBbHeight() * 0.28D;
                double radius = 0.70D + 0.14D * Math.cos(row * Math.PI / 2.0D);
                sendPoint(level, DEEP_JADE,
                        center.add(Math.cos(theta) * radius, y, Math.sin(theta) * radius));
            }
        }
    }

    private static void emitSpectralTortoise(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 forward = horizontalForward(player);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 origin = player.position().subtract(forward.scale(1.15D))
                .add(0.0D, player.getBbHeight() * 0.82D, 0.0D);
        for (int i = 0; i < 30; i++) {
            double angle = Math.PI * 2.0D * i / 30.0D;
            Vec3 point = origin.add(right.scale(Math.cos(angle) * 1.05D))
                    .add(0.0D, Math.sin(angle) * 0.66D, 0.0D);
            sendPoint(level, i % 5 == 0 ? GREEN_GOLD : JADE, point);
        }
        drawLine(level, JADE, origin.add(right.scale(-0.62D)).add(0.0D, -0.48D, 0.0D),
                origin.add(right.scale(-1.00D)).add(0.0D, -0.82D, 0.0D), 5);
        drawLine(level, JADE, origin.add(right.scale(0.62D)).add(0.0D, -0.48D, 0.0D),
                origin.add(right.scale(1.00D)).add(0.0D, -0.82D, 0.0D), 5);
        drawLine(level, GREEN_GOLD, origin.add(0.0D, 0.58D, 0.0D),
                origin.add(0.0D, 1.02D, 0.0D), 5);
        drawLine(level, DEEP_JADE, origin.add(right.scale(-0.72D)),
                origin.add(right.scale(0.72D)), 7);
    }

    private static void emitBlockedHit(ServerPlayer player, DamageSource source, ShieldState state) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        Vec3 direction = source.getSourcePosition() == null
                ? horizontalForward(player)
                : source.getSourcePosition().subtract(center).normalize();
        if (direction.lengthSqr() < 1.0E-5D) {
            direction = horizontalForward(player);
        }
        Vec3 impact = center.add(direction.scale(0.82D));
        level.sendParticles(GREEN_GOLD, impact.x, impact.y, impact.z,
                8, 0.16D, 0.16D, 0.16D, 0.025D);
        float integrity = Mth.clamp(state.remaining / state.maximum, 0.0F, 1.0F);
        int cracks = 2 + Mth.floor((1.0F - integrity) * 5.0F);
        for (int i = 0; i < cracks; i++) {
            double angle = Math.PI * 2.0D * i / cracks + level.random.nextDouble() * 0.35D;
            Vec3 end = impact.add(Math.cos(angle) * 0.28D,
                    Math.sin(angle) * 0.36D, Math.sin(angle * 1.7D) * 0.18D);
            drawLine(level, i % 2 == 0 ? GREEN_GOLD : JADE, impact, end, 4);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.45F, 0.78F + integrity * 0.45F);
    }

    private static void emitShatter(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        level.sendParticles(JADE, center.x, center.y, center.z,
                46, 0.72D, 0.92D, 0.72D, 0.18D);
        level.sendParticles(GREEN_GOLD, center.x, center.y, center.z,
                30, 0.64D, 0.78D, 0.64D, 0.22D);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                12, 0.52D, 0.62D, 0.52D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.68F);
    }

    private static Vec3 horizontalForward(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        return forward.lengthSqr() < 1.0E-5D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
    }

    private static void drawLine(ServerLevel level, ParticleOptions particle,
                                 Vec3 start, Vec3 end, int points) {
        for (int i = 0; i <= points; i++) {
            sendPoint(level, particle, start.lerp(end, i / (double) points));
        }
    }

    private static void sendPoint(ServerLevel level, ParticleOptions particle, Vec3 point) {
        level.sendParticles(particle, point.x, point.y, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static final class ShieldState {
        private float remaining;
        private final float maximum;
        private final long expiresAt;

        private ShieldState(float remaining, float maximum, long expiresAt) {
            this.remaining = remaining;
            this.maximum = maximum;
            this.expiresAt = expiresAt;
        }
    }
}
