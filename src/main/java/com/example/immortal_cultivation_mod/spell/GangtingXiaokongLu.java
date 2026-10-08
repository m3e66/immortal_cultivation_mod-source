package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative runtime for 罡霆啸空录 and its 风雷势 resource. */
public final class GangtingXiaokongLu {
    private static final String CONTINUOUS_MOVEMENT =
            "immortal_cultivation_mod:gangting_continuous_movement";
    private static final String LAST_STAGGER_DRAIN =
            "immortal_cultivation_mod:gangting_last_stagger_drain";
    private static final String LAST_COMBO_HIT =
            "immortal_cultivation_mod:gangting_last_combo_hit";
    private static final String COMBO_HITS =
            "immortal_cultivation_mod:gangting_combo_hits";
    private static final String LAST_LIGHTNING_CHAIN =
            "immortal_cultivation_mod:gangting_last_lightning_chain";
    private static final ResourceLocation MOVEMENT_MODIFIER = id("gangting_movement_speed");
    private static final ResourceLocation KNOCKBACK_MODIFIER = id("gangting_knockback_resistance");

    private GangtingXiaokongLu() {
    }

    public static void tick(ServerPlayer player) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) {
            clearModifiers(player);
            player.getPersistentData().remove(CONTINUOUS_MOVEMENT);
            if (player.tickCount % 20 == 0
                    && CultivationMethods.fengleiMomentum(data) > 0.0F) {
                ModAttachments.setData(player, CultivationMethods.withFengleiMomentum(
                        data, CultivationMethods.fengleiMomentum(data) - 1.0F));
            }
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean fastFlight = player.getAbilities().flying || player.isFallFlying();
        boolean moving = horizontalSpeed >= 0.055D
                || player.isSprinting()
                || fastFlight && velocity.lengthSqr() >= 0.0064D;
        int continuous = moving
                ? Math.min(20 * 60, player.getPersistentData().getInt(CONTINUOUS_MOVEMENT) + 1)
                : 0;
        player.getPersistentData().putInt(CONTINUOUS_MOVEMENT, continuous);

        if (player.tickCount % 5 == 0) {
            float momentum = CultivationMethods.fengleiMomentum(data);
            float change;
            if (moving) {
                change = 0.30F;
                if (player.isSprinting()) change += 0.14F;
                if (fastFlight && velocity.length() >= 0.30D) change += 0.24F;
                if (CombatStateManager.isInCombat(player)) change += 0.10F;
            } else {
                change = CombatStateManager.isInCombat(player) ? -0.60F : -0.90F;
            }
            if (!CombatStateManager.isInCombat(player) && continuous < 60) {
                change -= 0.12F;
            }
            float next = Mth.clamp(momentum + change, 0.0F, 100.0F);
            if (Math.abs(next - momentum) >= 0.005F) {
                ModAttachments.setData(player,
                        CultivationMethods.withFengleiMomentum(data, next));
                data = ModAttachments.getData(player);
            }
        }

        float momentum = CultivationMethods.fengleiMomentum(data);
        applyAttributes(player, momentum, moving && continuous >= 60);
        if (continuous >= 60) {
            improveAirMovement(player, momentum, horizontalSpeed);
            emitPassiveVfx(player, momentum, velocity);
        }
        if ((player.hasEffect(ModEffects.DAZE)
                || CombatStateManager.isPostureBroken(player))
                && player.serverLevel().getGameTime()
                >= player.getPersistentData().getLong(LAST_STAGGER_DRAIN) + 20L) {
            player.getPersistentData().putLong(
                    LAST_STAGGER_DRAIN, player.serverLevel().getGameTime());
            consumeMomentum(player, 15.0F);
        }
    }

    private static void improveAirMovement(
            ServerPlayer player, float momentum, double horizontalSpeed) {
        if (player.onGround() || player.isInWaterOrBubble() || horizontalSpeed < 0.06D) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-5D) {
            return;
        }
        horizontalLook = horizontalLook.normalize();
        double acceleration = 0.0035D + momentum * 0.000045D;
        Vec3 next = player.getDeltaMovement().add(
                horizontalLook.x * acceleration, look.y * acceleration * 0.28D,
                horizontalLook.z * acceleration);
        double cap = 0.55D + momentum * 0.010D;
        if (next.length() > cap) {
            next = next.normalize().scale(cap);
        }
        player.setDeltaMovement(next);
        player.hasImpulse = true;
    }

    private static void emitPassiveVfx(
            ServerPlayer player, float momentum, Vec3 velocity) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 3 == 0) {
            double phase = player.tickCount * 0.34D;
            for (int side = 0; side < 2; side++) {
                double angle = phase + side * Math.PI;
                level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                        player.getX() + Math.cos(angle) * 0.28D,
                        player.getY() + 0.12D + side * 0.18D,
                        player.getZ() + Math.sin(angle) * 0.28D,
                        1, 0.02D, 0.02D, 0.02D, 0.018D);
            }
        }
        if (momentum >= 50.0F && player.tickCount % 5 == 0) {
            level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                    player.getX(), player.getY() + player.getBbHeight() * 0.72D,
                    player.getZ(), momentum >= 100.0F ? 5 : 2,
                    0.38D, 0.28D, 0.38D, 0.035D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + player.getBbHeight() * 0.70D,
                    player.getZ(), momentum >= 75.0F ? 3 : 1,
                    0.32D, 0.24D, 0.32D, 0.05D);
        }
        if (player.onGround() && player.tickCount % 10 == 0) {
            emitFootRing(level, player.position().add(0.0D, 0.06D, 0.0D),
                    momentum >= 100.0F ? 1.25D : 0.82D);
        }
        if (momentum >= 75.0F && velocity.lengthSqr() > 0.02D
                && player.tickCount % 2 == 0) {
            Vec3 direction = velocity.normalize();
            for (int image = 1; image <= (momentum >= 100.0F ? 4 : 2); image++) {
                Vec3 base = player.position().subtract(direction.scale(image * 0.48D));
                for (int row = 0; row < 4; row++) {
                    level.sendParticles(row % 2 == 0
                                    ? ModParticles.METHOD_QI_WIND.get()
                                    : ModParticles.METHOD_QI_THUNDER.get(),
                            base.x, base.y + 0.22D + row * player.getBbHeight() / 4.5D,
                            base.z, 1, 0.18D, 0.04D, 0.18D, 0.0D);
                }
            }
        }
    }

    private static void emitFootRing(ServerLevel level, Vec3 center, double radius) {
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0D * i / 18.0D;
            level.sendParticles(i % 5 == 0 ? ParticleTypes.ELECTRIC_SPARK
                            : ModParticles.METHOD_QI_WIND.get(),
                    center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.025D);
        }
    }

    public static float adjustIncomingDamage(
            LivingEntity target, DamageSource source, float amount) {
        if (!(target instanceof ServerPlayer player) || amount <= 0.0F) {
            return amount;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) {
            return amount;
        }
        float momentum = CultivationMethods.fengleiMomentum(data);
        Vec3 movement = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(
                movement.x * movement.x + movement.z * movement.z);
        if (source.getDirectEntity() instanceof Projectile
                && momentum >= 50.0F && horizontalSpeed >= 0.12D) {
            float chance = Mth.lerp((momentum - 50.0F) / 50.0F, 0.05F, 0.18F);
            if (player.getRandom().nextFloat() < chance) {
                Vec3 side = new Vec3(-movement.z, 0.0D, movement.x);
                if (side.lengthSqr() > 1.0E-5D) {
                    side = side.normalize().scale(player.getRandom().nextBoolean() ? 0.18D : -0.18D);
                    player.push(side.x, 0.035D, side.z);
                }
                addMomentum(player, 6.0F);
                player.serverLevel().sendParticles(ModParticles.METHOD_QI_WIND.get(),
                        player.getX(), player.getY() + player.getBbHeight() * 0.55D,
                        player.getZ(), 18, 0.45D, 0.55D, 0.45D, 0.10D);
                player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + player.getBbHeight() * 0.55D,
                        player.getZ(), 8, 0.38D, 0.48D, 0.38D, 0.10D);
                player.serverLevel().playSound(null, player.blockPosition(),
                        SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS,
                        0.55F, 1.65F);
                return 0.0F;
            }
        }
        if (amount >= player.getMaxHealth() * 0.12F) {
            long now = player.serverLevel().getGameTime();
            if (now >= player.getPersistentData().getLong(LAST_STAGGER_DRAIN) + 10L) {
                player.getPersistentData().putLong(LAST_STAGGER_DRAIN, now);
                consumeMomentum(player, 10.0F);
            }
        }
        return amount;
    }

    public static void onDamageApplied(
            ServerPlayer player, LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        long previous = player.getPersistentData().getLong(LAST_COMBO_HIT);
        int hits = now - previous <= 35L
                ? Math.min(8, player.getPersistentData().getInt(COMBO_HITS) + 1) : 1;
        player.getPersistentData().putLong(LAST_COMBO_HIT, now);
        player.getPersistentData().putInt(COMBO_HITS, hits);
        addMomentum(player, 1.0F + hits * 0.35F);
        if (CultivationMethods.fengleiMomentum(ModAttachments.getData(player)) >= 25.0F) {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-5D) {
                double strength = windForceMultiplier(player) - 1.0D;
                target.push(away.normalize().x * strength * 0.18D, 0.025D,
                        away.normalize().z * strength * 0.18D);
            }
        }
    }

    public static void onTechniqueCast(ServerPlayer player, String spellId) {
        ModSpells.SpellDef spell = ModSpells.get(spellId);
        if (spell == null || !CultivationMethods.isGangtingXiaokongLu(
                ModAttachments.getData(player).activeCultivationMethod())) {
            return;
        }
        boolean windOrThunder = java.util.Arrays.stream(spell.element().split(","))
                .map(String::trim)
                .anyMatch(element -> com.example.immortal_cultivation_mod.attachment.SpiritRoots.WIND.equals(element)
                        || com.example.immortal_cultivation_mod.attachment.SpiritRoots.THUNDER.equals(element));
        if (!windOrThunder) return;
        boolean moving = player.isSprinting()
                || player.getAbilities().flying
                || player.getDeltaMovement().horizontalDistanceSqr() >= 0.01D;
        if (moving) {
            addMomentum(player, 5.0F);
        }
    }

    public static void maybeChainLightning(
            LivingEntity owner, LivingEntity struck, float sourceDamage) {
        if (!(owner instanceof ServerPlayer player)
                || sourceDamage <= 0.0F || !struck.isAlive()) {
            return;
        }
        float momentum = momentum(player);
        if (momentum < 50.0F) return;
        long now = player.serverLevel().getGameTime();
        if (player.getPersistentData().getLong(LAST_LIGHTNING_CHAIN) == now) {
            return;
        }
        float chance = momentum >= 100.0F ? 0.55F
                : momentum >= 75.0F ? 0.35F : 0.20F;
        if (player.getRandom().nextFloat() >= chance) return;
        LivingEntity chained = player.serverLevel().getEntitiesOfClass(
                        LivingEntity.class, struck.getBoundingBox().inflate(5.0D),
                        target -> target != player && target != struck && target.isAlive()
                                && NpcProfile.isCombatEnemy(player, target))
                .stream().min(java.util.Comparator.comparingDouble(struck::distanceToSqr))
                .orElse(null);
        if (chained == null || ForcefieldCenterBehavior.interceptAttackPath(
                player, struck.getBoundingBox().getCenter(),
                chained.getBoundingBox().getCenter(), sourceDamage * 0.45F)) {
            return;
        }
        player.getPersistentData().putLong(LAST_LIGHTNING_CHAIN, now);
        chained.hurt(player.damageSources().indirectMagic(player, player),
                sourceDamage * 0.45F);
        ElementalSpellVfx.line(player.serverLevel(),
                struck.getBoundingBox().getCenter(),
                chained.getBoundingBox().getCenter(),
                ParticleTypes.ELECTRIC_SPARK, 0.18D);
        ElementalSpellVfx.line(player.serverLevel(),
                struck.getBoundingBox().getCenter(),
                chained.getBoundingBox().getCenter(),
                ModParticles.METHOD_QI_THUNDER.get(), 0.30D);
    }

    public static void addMomentum(ServerPlayer player, float amount) {
        if (amount <= 0.0F) return;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) return;
        float next = Mth.clamp(CultivationMethods.fengleiMomentum(data) + amount,
                0.0F, 100.0F);
        ModAttachments.setData(player,
                CultivationMethods.withFengleiMomentum(data, next));
    }

    public static void consumeMomentum(ServerPlayer player, float amount) {
        if (amount <= 0.0F) return;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        float next = Mth.clamp(CultivationMethods.fengleiMomentum(data) - amount,
                0.0F, 100.0F);
        ModAttachments.setData(player,
                CultivationMethods.withFengleiMomentum(data, next));
    }

    public static float momentum(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return 0.0F;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        return CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())
                ? CultivationMethods.fengleiMomentum(data) : 0.0F;
    }

    public static float thunderPowerMultiplier(LivingEntity entity) {
        float momentum = momentum(entity);
        if (momentum >= 100.0F) return 1.25F;
        return momentum >= 75.0F ? 1.15F : 1.0F;
    }

    public static float windRadiusMultiplier(LivingEntity entity) {
        return momentum(entity) >= 100.0F ? 1.20F : 1.0F;
    }

    public static float windForceMultiplier(LivingEntity entity) {
        float momentum = momentum(entity);
        if (momentum >= 100.0F) return 1.35F;
        if (momentum >= 75.0F) return 1.22F;
        return momentum >= 25.0F ? 1.10F : 1.0F;
    }

    private static void applyAttributes(
            ServerPlayer player, float momentum, boolean passiveActive) {
        double speed = momentum >= 100.0F ? 0.40D
                : momentum >= 75.0F ? 0.30D
                : momentum >= 50.0F ? 0.20D
                : momentum >= 25.0F ? 0.10D : 0.0D;
        updateModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER,
                speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER,
                passiveActive && player.getDeltaMovement().horizontalDistanceSqr() >= 0.0144D
                        ? 0.20D : 0.0D,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void clearModifiers(ServerPlayer player) {
        updateModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER,
                0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER,
                0.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void updateModifier(
            LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0001D) return;
        if (existing != null) instance.removeModifier(id);
        if (Math.abs(amount) > 0.0001D) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path);
    }
}
