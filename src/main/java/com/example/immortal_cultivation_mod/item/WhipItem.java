package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WhipItem extends SwordItem implements GeoItem {
    private static final double NORMAL_RANGE = 8.0D;
    private static final double HEAVY_RANGE = 16.0D;
    private static final int COMBO_WINDOW_TICKS = 40;
    private static final int MAX_COMBO_STACKS = 5;
    private static final String COMBO_STACKS_TAG =
            "ImmortalCultivationWhipComboStacks";
    private static final String COMBO_UNTIL_TAG =
            "ImmortalCultivationWhipComboUntil";
    private static final String ANIMATION_VARIANT_TAG =
            "ImmortalCultivationWhipAnimationVariant";
    private static final String BACK_ANIMATION_VARIANT_TAG =
            "ImmortalCultivationWhipBackAnimationVariant";
    private static final String ANIMATION_RETURN_AT_TAG =
            "ImmortalCultivationWhipAnimationReturnAt";
    private static final int ATTACK_ANIMATION_TICKS = 4;
    private static final int ANIMATION_RETURN_DELAY_TICKS = 4;
    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("animation.whip.idle");
    private static final RawAnimation CRACK_ANIMATION_A =
            createAttackAnimation();
    private static final RawAnimation CRACK_ANIMATION_B =
            createAttackAnimation();
    private static final RawAnimation BACK_ANIMATION_A =
            createBackAnimation();
    private static final RawAnimation BACK_ANIMATION_B =
            createBackAnimation();
    private static final ResourceLocation COMBO_SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "whip_combo_speed");
    private static final ResourceKey<DamageType> WHIP_DAMAGE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, "whip"));
    private static final TagKey<net.minecraft.world.level.block.Block>
            WHIP_BREAKABLE_BLOCKS = TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID,
                            "whip_breakable"));
    private static final Map<UUID, WallImpact> WALL_IMPACTS =
            new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public WhipItem(Properties properties) {
        super(Tiers.IRON, properties
                .stacksTo(1)
                .durability(Tiers.IRON.getUses())
                .attributes(SwordItem.createAttributes(
                        Tiers.IRON, 5.0F, -2.6F)));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static boolean performNormalAttack(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhipItem) || !WeaponBinding.checkUse(stack, player)) {
            return false;
        }
        triggerCrackAnimation(player);

        float qiDamage = QiInfusedWeapon.injectedQiDamage(stack);
        float comboMultiplier = comboDamageMultiplier(player);
        float damage = (float) (
                QiInfusedWeapon.baseAttackDamage(stack) + qiDamage)
                * comboMultiplier;
        if (!WeaponQiSlashEntity.releaseWhip(
                player, damage, qiDamage, NORMAL_RANGE)) {
            return false;
        }

        playCrack(player, false);
        return true;
    }

    public static boolean performHeavyAttack(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhipItem) || !WeaponBinding.checkUse(stack, player)) {
            return false;
        }
        triggerCrackAnimation(player);

        Vec3 direction = horizontalLook(player);
        List<LivingEntity> targets = targetsInHeavyCone(player, direction);
        float qiDamage = QiInfusedWeapon.injectedQiDamage(stack);
        float comboMultiplier = comboDamageMultiplier(player);
        DamageSource source = damageSource(player);
        long gameTime = player.serverLevel().getGameTime();
        boolean hit = false;

        for (LivingEntity target : targets) {
            float damage = adjustedDamage(
                    stack, target,
                    (float) (QiInfusedWeapon.baseAttackDamage(stack) + qiDamage)
                            * 2.0F * comboMultiplier);
            float armorAdjusted = armorPenetratingRawDamage(
                    target, damage, 0.30F);
            if (!CombatStateManager.hurtAsHeavy(
                    player, target, source, armorAdjusted)) {
                continue;
            }
            hit = true;
            applyWeaponEffects(stack, target);
            launchTarget(player, target, direction);
            WALL_IMPACTS.put(
                    target.getUUID(),
                    new WallImpact(
                            player.getUUID(),
                            Math.max(1.0F, damage * 0.5F),
                            gameTime,
                            gameTime + 24L, stack.copy()));
            CombatStateManager.recordHostileAction(player, target);
        }

        breakWeakBlocks(player, direction);
        spawnHeavyVfx(player.serverLevel(), player, direction);
        playCrack(player, true);
        finishAttack(player, stack, qiDamage, hit);
        return true;
    }

    public static void tickCombo(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        boolean holdingWhip = player.getMainHandItem().getItem()
                instanceof WhipItem;
        long comboUntil = player.getPersistentData().getLong(
                COMBO_UNTIL_TAG);
        tickAnimationReturn(player, holdingWhip, gameTime);
        if (!holdingWhip || comboUntil < gameTime) {
            clearCombo(player);
        }
    }

    public static void tickWallImpact(LivingEntity target) {
        WallImpact impact = WALL_IMPACTS.get(target.getUUID());
        if (impact == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        if (!target.isAlive() || gameTime > impact.expiresAt()) {
            WALL_IMPACTS.remove(target.getUUID());
            return;
        }
        if (gameTime <= impact.launchedAt() || !target.horizontalCollision) {
            return;
        }

        ServerPlayer owner = level.getServer().getPlayerList()
                .getPlayer(impact.ownerId());
        WALL_IMPACTS.remove(target.getUUID());
        if (owner == null || !owner.isAlive() || !WeaponBinding.canUse(impact.weapon(), owner)) {
            return;
        }

        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(
                ModEffects.DAZE, 30, 0, false, true, true));
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                30, 10, false, true, true));
        var impactSource = damageSource(owner);
        WeaponBinding.withWeapon(impact.weapon(), impactSource, () -> CombatStateManager.hurtAsHeavy(
                owner, target, impactSource,
                armorPenetratingRawDamage(target, impact.bonusDamage(), 0.30F)));
        spawnQiBurst(
                level,
                owner,
                target.getBoundingBox().getCenter(),
                50,
                0.65D);
        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.75F,
                1.35F);
    }

    private static List<LivingEntity> targetsInHeavyCone(
            ServerPlayer player, Vec3 direction) {
        AABB search = player.getBoundingBox().inflate(
                HEAVY_RANGE, 6.0D, HEAVY_RANGE);
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity target : player.serverLevel()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        candidate -> canHit(player, candidate))) {
            Vec3 offset = target.getBoundingBox().getCenter()
                    .subtract(origin);
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            double distance = horizontal.length();
            if (distance <= 0.01D || distance > HEAVY_RANGE
                    || Math.abs(offset.y) > 6.0D
                    || direction.dot(horizontal.scale(1.0D / distance))
                    < 0.20D
                    || !player.hasLineOfSight(target)) {
                continue;
            }
            targets.add(target);
        }

        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        return targets;
    }

    private static boolean canHit(
            ServerPlayer player, LivingEntity target) {
        return target != player
                && target.isAlive()
                && !player.isAlliedTo(target)
                && player.canAttack(target);
    }

    public static boolean hitFromQiLine(
            ServerPlayer player,
            LivingEntity target,
            float rawDamage,
            float qiDamage,
            boolean firstHit) {
        return hitFromQiLine(player, target, rawDamage, qiDamage, firstHit, player.getMainHandItem());
    }

    public static boolean hitFromQiLine(ServerPlayer player, LivingEntity target,
            float rawDamage, float qiDamage, boolean firstHit, ItemStack launchWeapon) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhipItem)
                || !WeaponBinding.checkUse(launchWeapon, player) || !canHit(player, target)) {
            return false;
        }

        float damage = adjustedDamage(stack, target, rawDamage);
        var source = damageSource(player);
        if (!WeaponBinding.withWeapon(launchWeapon, source, () -> target.hurt(source, damage))) {
            return false;
        }

        applyWeaponEffects(stack, target);
        applyNormalKnockback(player, target);
        CombatStateManager.recordHostileAction(player, target);
        if (firstHit) {
            finishAttack(player, stack, qiDamage, true);
        }
        return true;
    }

    public static void createQiContactPop(ServerPlayer player, Vec3 center) {
        createQiContactPop(player, center, player.getMainHandItem());
    }

    public static void createQiContactPop(ServerPlayer player, Vec3 center, ItemStack launchWeapon) {
        if (!WeaponBinding.canUse(launchWeapon, player)) return;
        ServerLevel level = player.serverLevel();
        double radius = 1.75D;
        AABB area = new AABB(center, center).inflate(radius);
        DamageSource baseSource = damageSource(player);
        DamageSource pulseSource = new DamageSource(
                baseSource.typeHolder(), player, player, center);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> canHit(player, candidate)
                        && candidate.getBoundingBox().getCenter()
                                .distanceToSqr(center)
                        <= radius * radius)) {
            target.invulnerableTime = 0;
            if (!WeaponBinding.withWeapon(launchWeapon, pulseSource, () -> target.hurt(pulseSource, 10.0F))) {
                continue;
            }

            Vec3 push = target.getBoundingBox().getCenter()
                    .subtract(center);
            if (push.lengthSqr() > 1.0E-5D) {
                push = push.normalize().scale(0.35D);
                target.push(push.x, 0.16D, push.z);
                target.hurtMarked = true;
            }
            CombatStateManager.recordHostileAction(player, target);
        }

        spawnQiBurst(level, player, center, 36, 0.55D);
        List<SimpleParticleType> particles =
                MethodQiParticleEffects.particlesForPlayer(player);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            level.sendParticles(
                    particles.get(i % particles.size()),
                    center.x,
                    center.y,
                    center.z,
                    0,
                    Math.cos(angle) * 0.42D,
                    0.03D,
                    Math.sin(angle) * 0.42D,
                    1.0D);
        }
        level.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.65F,
                1.65F);
    }

    private static float adjustedDamage(
            ItemStack stack, LivingEntity target, float damage) {
        damage = ForgingSystem.adjustWeaponDamage(stack, target, damage);
        return WeaponRestrictionSystem.adjustWeaponDamage(
                stack, target, damage);
    }

    private static void applyWeaponEffects(
            ItemStack stack, LivingEntity target) {
        ForgingSystem.applyWeaponHitEffects(stack, target);
        WeaponRestrictionSystem.applyWeaponHitEffects(stack, target);
    }

    private static void applyNormalKnockback(
            ServerPlayer player, LivingEntity target) {
        Vec3 direction = target.position().subtract(player.position())
                .multiply(1.0D, 0.0D, 1.0D)
                .normalize();
        if (direction.lengthSqr() < 0.001D) {
            direction = horizontalLook(player);
        }
        target.push(direction.x * 0.32D, 0.08D, direction.z * 0.32D);
        target.hurtMarked = true;
    }

    private static void launchTarget(
            ServerPlayer player, LivingEntity target, Vec3 fallback) {
        Vec3 direction = target.position().subtract(player.position())
                .multiply(1.0D, 0.0D, 1.0D)
                .normalize();
        if (direction.lengthSqr() < 0.001D) {
            direction = fallback;
        }
        target.setDeltaMovement(
                direction.x * 2.15D,
                Math.max(0.9D, target.getDeltaMovement().y + 0.65D),
                direction.z * 2.15D);
        target.hurtMarked = true;
    }

    private static void breakWeakBlocks(
            ServerPlayer player, Vec3 direction) {
        if (!BlockBreakingSettings.enabled(player,
                BlockBreakingSettings.Category.WEAPONS)) return;
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        int radius = (int) Math.ceil(HEAVY_RANGE);
        BlockPos min = origin.offset(-radius, -2, -radius);
        BlockPos max = origin.offset(radius, 5, radius);
        Vec3 center = player.position().add(0.0D, 0.9D, 0.0D);

        for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
            Vec3 offset = Vec3.atCenterOf(mutable).subtract(center);
            double forward = offset.x * direction.x
                    + offset.z * direction.z;
            if (forward < 0.0D || forward > HEAVY_RANGE) {
                continue;
            }
            double lateralX = offset.x - direction.x * forward;
            double lateralZ = offset.z - direction.z * forward;
            double maxWidth = 1.0D + forward * 0.62D;
            if (lateralX * lateralX + lateralZ * lateralZ
                    > maxWidth * maxWidth
                    || Math.abs(offset.y) > 2.2D + forward * 0.18D) {
                continue;
            }
            BlockState state = level.getBlockState(mutable);
            if (isWeakBlock(state)) {
                BlockPos pos = mutable.immutable();
                BlockBreakingSettings.runWithSource(
                        BlockBreakingSettings.Category.WEAPONS, player,
                        () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.breakBlock(level, pos, false, player));
            }
        }
    }

    private static boolean isWeakBlock(BlockState state) {
        return state.is(WHIP_BREAKABLE_BLOCKS);
    }

    private static float armorPenetratingRawDamage(
            LivingEntity target, float damage, float penetration) {
        float armor = target.getArmorValue();
        if (armor <= 0.0F || penetration <= 0.0F) {
            return damage;
        }
        float toughness = (float) target.getAttributeValue(
                Attributes.ARMOR_TOUGHNESS);
        float desired = damageAfterArmor(
                damage, armor * (1.0F - penetration), toughness);
        float low = damage;
        float high = Math.max(damage * 12.0F, damage + armor * 5.0F);

        for (int i = 0; i < 18; i++) {
            float mid = (low + high) * 0.5F;
            if (damageAfterArmor(mid, armor, toughness) < desired) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static float damageAfterArmor(
            float damage, float armor, float toughness) {
        float effectiveArmor = Mth.clamp(
                armor - damage / (2.0F + toughness / 4.0F),
                armor * 0.2F,
                20.0F);
        return damage * (1.0F - effectiveArmor / 25.0F);
    }

    private static void finishAttack(
            ServerPlayer player,
            ItemStack stack,
            float qiDamage,
            boolean hit) {
        if (!hit) {
            return;
        }
        if (qiDamage > 0.0F) {
            QiInfusedWeapon.consumeInjectedQiDamage(stack, qiDamage);
        }
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        advanceCombo(player);
    }

    private static float comboDamageMultiplier(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        if (player.getPersistentData().getLong(COMBO_UNTIL_TAG) < gameTime) {
            clearCombo(player);
            return 1.0F;
        }
        int stacks = Mth.clamp(
                player.getPersistentData().getInt(COMBO_STACKS_TAG),
                0,
                MAX_COMBO_STACKS);
        return 1.0F + stacks * 0.10F;
    }

    private static void advanceCombo(ServerPlayer player) {
        int stacks = Mth.clamp(
                player.getPersistentData().getInt(COMBO_STACKS_TAG) + 1,
                1,
                MAX_COMBO_STACKS);
        player.getPersistentData().putInt(COMBO_STACKS_TAG, stacks);
        player.getPersistentData().putLong(
                COMBO_UNTIL_TAG,
                player.serverLevel().getGameTime() + COMBO_WINDOW_TICKS);
        applyComboSpeed(player, stacks);
    }

    private static void clearCombo(ServerPlayer player) {
        player.getPersistentData().remove(COMBO_STACKS_TAG);
        player.getPersistentData().remove(COMBO_UNTIL_TAG);
        applyComboSpeed(player, 0);
    }

    private static void applyComboSpeed(
            ServerPlayer player, int stacks) {
        AttributeInstance attackSpeed = player.getAttribute(
                Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        if (attackSpeed.getModifier(COMBO_SPEED_MODIFIER) != null) {
            attackSpeed.removeModifier(COMBO_SPEED_MODIFIER);
        }
        if (stacks > 0) {
            attackSpeed.addTransientModifier(new AttributeModifier(
                    COMBO_SPEED_MODIFIER,
                    Math.min(MAX_COMBO_STACKS, stacks) * 0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static DamageSource damageSource(ServerPlayer player) {
        Holder<DamageType> type = player.serverLevel()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(WHIP_DAMAGE);
        return new DamageSource(type, player);
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 direction = player.getLookAngle()
                .multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private static void spawnHeavyVfx(
            ServerLevel level, ServerPlayer player, Vec3 direction) {
        List<SimpleParticleType> particles =
                MethodQiParticleEffects.particlesForPlayer(player);
        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);
        for (int distance = 1; distance <= (int) HEAVY_RANGE; distance++) {
            double halfWidth = 0.6D + distance * 0.58D;
            int samples = Math.max(3, (int) Math.ceil(halfWidth * 2.0D));
            for (int i = -samples; i <= samples; i++) {
                double side = halfWidth * i / samples;
                Vec3 point = origin
                        .add(direction.scale(distance))
                        .add(right.scale(side));
                SimpleParticleType particle = particles.get(Math.floorMod(
                        distance * 31 + i, particles.size()));
                level.sendParticles(
                        particle,
                        point.x,
                        point.y + Math.sin(side * 0.7D) * 0.3D,
                        point.z,
                        1,
                        0.06D,
                        0.06D,
                        0.06D,
                        0.025D);
            }
        }
        spawnQiBurst(
                level,
                player,
                origin.add(direction.scale(3.0D)),
                42,
                0.58D);
    }

    private static void spawnQiBurst(
            ServerLevel level,
            ServerPlayer player,
            Vec3 center,
            int count,
            double speed) {
        List<SimpleParticleType> particles =
                MethodQiParticleEffects.particlesForPlayer(player);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double vertical = -0.55D
                    + 1.10D * ((i * 7) % count) / Math.max(1, count - 1);
            double horizontal = Math.sqrt(Math.max(
                    0.0D, 1.0D - vertical * vertical));
            Vec3 velocity = new Vec3(
                    Math.cos(angle) * horizontal,
                    vertical,
                    Math.sin(angle) * horizontal).scale(speed);
            level.sendParticles(
                    particles.get(i % particles.size()),
                    center.x,
                    center.y,
                    center.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0D);
        }
    }

    private static void playCrack(
            ServerPlayer player, boolean heavy) {
        ServerLevel level = player.serverLevel();
        level.playSound(
                null,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                heavy ? 1.8F : 1.25F,
                heavy ? 0.55F : 1.35F);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                heavy ? 1.1F : 0.32F,
                heavy ? 1.25F : 1.85F);
    }

    private static void triggerCrackAnimation(ServerPlayer player) {
        boolean alternate = !player.getPersistentData().getBoolean(
                ANIMATION_VARIANT_TAG);
        player.getPersistentData().putBoolean(
                ANIMATION_VARIANT_TAG, alternate);
        player.getPersistentData().putLong(
                ANIMATION_RETURN_AT_TAG,
                player.serverLevel().getGameTime()
                        + ATTACK_ANIMATION_TICKS
                        + ANIMATION_RETURN_DELAY_TICKS);
        triggerItemAnimation(
                player, alternate ? "crack_a" : "crack_b");
    }

    private static RawAnimation createAttackAnimation() {
        return RawAnimation.begin()
                .thenPlayAndHold("animation.whip.attack");
    }

    private static RawAnimation createBackAnimation() {
        return RawAnimation.begin()
                .thenPlay("animation.whip.back");
    }

    private static void tickAnimationReturn(
            ServerPlayer player,
            boolean holdingWhip,
            long gameTime) {
        long returnAt = player.getPersistentData().getLong(
                ANIMATION_RETURN_AT_TAG);
        if (!holdingWhip) {
            player.getPersistentData().remove(ANIMATION_RETURN_AT_TAG);
            return;
        }
        if (returnAt <= 0L || gameTime < returnAt) {
            return;
        }

        player.getPersistentData().remove(ANIMATION_RETURN_AT_TAG);
        boolean alternate = !player.getPersistentData().getBoolean(
                BACK_ANIMATION_VARIANT_TAG);
        player.getPersistentData().putBoolean(
                BACK_ANIMATION_VARIANT_TAG, alternate);
        triggerItemAnimation(
                player, alternate ? "back_a" : "back_b");
    }

    public static void clearAnimationState(ServerPlayer player) {
        player.getPersistentData().remove(ANIMATION_RETURN_AT_TAG);
        player.getPersistentData().remove(ANIMATION_VARIANT_TAG);
        player.getPersistentData().remove(BACK_ANIMATION_VARIANT_TAG);
    }

    private static void triggerItemAnimation(
            ServerPlayer player, String animation) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhipItem item)) {
            return;
        }
        item.triggerAnim(
                player,
                GeoItem.getOrAssignId(stack, player.serverLevel()),
                "main",
                animation);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private WhipItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                    getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new WhipItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> state.setAndContinue(IDLE_ANIMATION))
                .triggerableAnim("crack_a", CRACK_ANIMATION_A)
                .triggerableAnim("crack_b", CRACK_ANIMATION_B)
                .triggerableAnim("back_a", BACK_ANIMATION_A)
                .triggerableAnim("back_b", BACK_ANIMATION_B));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private record WallImpact(
            UUID ownerId,
            float bonusDamage,
            long launchedAt,
            long expiresAt, ItemStack weapon) {
    }
}
