package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.NpcSpellcasting;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.network.CombatPayloads;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FuchenItem extends SwordItem implements GeoItem {
    private static final String BINDING_TAG = "ImmortalCultivationFuchenBinding";
    private static final int SPIN_COUNT = 5;
    private static final int SPIN_INTERVAL_TICKS = 4;
    private static final float SPIN_DAMAGE_PERCENT = 150.0F / 7.0F;
    private static final double SPIN_RADIUS = 3.0D;
    private static final int BIND_DURATION_TICKS = 60;
    private static final int BIND_COOLDOWN_TICKS = 200;
    private static final float BIND_DAMAGE_PERCENT_PER_SECOND = 120.0F / 7.0F;
    private static final double BIND_REACH = 5.0D;

    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("animation.fuchen.idle");
    private static final RawAnimation ATTACK_ANIMATION_A =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen.attack");
    private static final RawAnimation ATTACK_ANIMATION_B =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen.attack")
                    .thenWait(0);
    private static final RawAnimation SPIN_ANIMATION_A =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen.spin");
    private static final RawAnimation SPIN_ANIMATION_B =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen.spin")
                    .thenWait(0);
    private static final RawAnimation WRAP_ANIMATION_A =
            RawAnimation.begin()
                    .thenPlayAndHold("animation.fuchen.wrap");
    private static final RawAnimation WRAP_ANIMATION_B =
            RawAnimation.begin()
                    .thenPlayAndHold("animation.fuchen.wrap");
    private static final RawAnimation BINDING_STATE_ANIMATION =
            RawAnimation.begin()
                    .thenPlayAndHold("animation.fuchen.wrap");
    private static final RawAnimation RELEASE_ANIMATION =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen.release");

    private static final Map<UUID, SpinState> SPIN_ATTACKS = new HashMap<>();
    private static final Map<UUID, BindState> BOUND_TARGETS = new HashMap<>();
    private static final Map<UUID, Boolean> ATTACK_TRIGGER_TOGGLES = new HashMap<>();
    private static final Map<UUID, Boolean> SPIN_TRIGGER_TOGGLES = new HashMap<>();
    private static final Map<UUID, Boolean> WRAP_TRIGGER_TOGGLES = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FuchenItem(Properties properties) {
        super(Tiers.IRON, properties.stacksTo(1)
                .durability(Tiers.IRON.getUses())
                .attributes(SwordItem.createAttributes(Tiers.IRON, 4.0F, -2.4F)));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WeaponBinding.checkUse(stack, player)) return InteractionResultHolder.fail(stack);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        LivingEntity target = findBindTarget(player);
        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = interactLivingEntity(stack, player, target, hand);
        return result.consumesAction()
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND
                || target == player
                || player.isAlliedTo(target)
                || distanceToBounds(player, target) > BIND_REACH) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || physicalStrength(target) >= physicalStrength(player)) {
            return InteractionResult.CONSUME;
        }

        WeaponSkillAttack attack = WeaponSkillAttack.capture(stack);
        if (ForcefieldCenterBehavior.interceptAttackPath(
                serverPlayer,
                serverPlayer.getEyePosition(),
                target.getBoundingBox().getCenter(),
                (float) Math.max(1.0D,
                        serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE)))) {
            attack.consumeInjectedQi();
            serverPlayer.swing(hand, true);
            serverPlayer.getCooldowns().addCooldown(this, BIND_COOLDOWN_TICKS);
            return InteractionResult.CONSUME;
        }

        attack.consumeInjectedQi();
        long gameTime = serverPlayer.serverLevel().getGameTime();
        finishBindingsOwnedBy(serverPlayer, true);
        BindState previousState = BOUND_TARGETS.get(target.getUUID());
        if (previousState != null) {
            finishBinding(target, previousState);
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.DINGSHEN,
                BIND_DURATION_TICKS,
                0,
                false,
                true,
                true));
        FuchenBindingPlumeEntity plume = FuchenBindingPlumeEntity.create(
                serverPlayer.serverLevel(),
                serverPlayer,
                target,
                BIND_DURATION_TICKS + 5);
        UUID plumeId = null;
        if (plume != null) {
            serverPlayer.serverLevel().addFreshEntity(plume);
            plumeId = plume.getUUID();
        }
        long animatableId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        setBinding(stack, true);
        BOUND_TARGETS.put(target.getUUID(), new BindState(
                serverPlayer.getUUID(),
                plumeId,
                animatableId,
                stack,
                gameTime + 20L,
                gameTime + BIND_DURATION_TICKS,
                3,
                attack));
        boolean alternate = WRAP_TRIGGER_TOGGLES.merge(
                serverPlayer.getUUID(), true, (previous, ignored) -> !previous);
        triggerItemAnimation(
                serverPlayer,
                animatableId,
                alternate ? "wrap_a" : "wrap_b");
        broadcastBindingPose(serverPlayer, true);
        serverPlayer.swing(hand, true);
        serverPlayer.getCooldowns().addCooldown(this, BIND_COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)) {
            return;
        }
        long animatableId = GeoItem.getOrAssignId(stack, serverLevel);
        if (isBinding(stack)
                && !hasActiveBinding(player.getUUID(), animatableId)) {
            setBinding(stack, false);
        }
    }

    public static void playNormalAttackAnimation(ServerPlayer player) {
        boolean alternate = ATTACK_TRIGGER_TOGGLES.merge(
                player.getUUID(), true, (previous, ignored) -> !previous);
        triggerItemAnimation(player, alternate ? "attack_b" : "attack_a");
    }

    public static boolean startHeavyAttack(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)
                || !(attacker.getMainHandItem().getItem() instanceof FuchenItem)
                || !WeaponBinding.checkUse(attacker.getMainHandItem(), attacker)) {
            return false;
        }
        long gameTime = serverLevel.getGameTime();
        ItemStack weapon = attacker.getMainHandItem();
        WeaponSkillAttack attack = WeaponSkillAttack.capture(weapon);
        attack.consumeInjectedQi();
        SPIN_ATTACKS.put(attacker.getUUID(), new SpinState(
                gameTime,
                0,
                weapon,
                attack));
        if (attacker instanceof ServerPlayer player) {
            boolean alternate = SPIN_TRIGGER_TOGGLES.merge(
                    player.getUUID(), true, (previous, ignored) -> !previous);
            triggerItemAnimation(player, alternate ? "spin_b" : "spin_a");
        }
        return true;
    }

    public static int heavyAttackDurationTicks() {
        return (SPIN_COUNT - 1) * SPIN_INTERVAL_TICKS + 1;
    }

    public static void tick(LivingEntity living) {
        if (living.level().isClientSide) {
            return;
        }
        tickBoundTarget(living);
        tickSpinAttack(living);
    }

    public static void clear(LivingEntity living) {
        SPIN_ATTACKS.remove(living.getUUID());
        if (living instanceof ServerPlayer player) {
            finishBindingsOwnedBy(player, false);
        }
        ATTACK_TRIGGER_TOGGLES.remove(living.getUUID());
        SPIN_TRIGGER_TOGGLES.remove(living.getUUID());
        WRAP_TRIGGER_TOGGLES.remove(living.getUUID());
    }

    private static void tickSpinAttack(LivingEntity attacker) {
        SpinState state = SPIN_ATTACKS.get(attacker.getUUID());
        if (state == null) {
            return;
        }
        if (!attacker.isAlive()
                || attacker.getMainHandItem() != state.weapon
                || !(attacker.getMainHandItem().getItem() instanceof FuchenItem)
                || !(attacker.level() instanceof ServerLevel serverLevel)) {
            SPIN_ATTACKS.remove(attacker.getUUID());
            return;
        }

        long gameTime = serverLevel.getGameTime();
        if (gameTime < state.nextPulseTick) {
            return;
        }
        strikeNearbyEnemies(attacker, state);
        emitSpinParticles(attacker);
        state.completedSpins++;
        if (state.completedSpins >= SPIN_COUNT) {
            SPIN_ATTACKS.remove(attacker.getUUID());
        } else {
            state.nextPulseTick = gameTime + SPIN_INTERVAL_TICKS;
        }
    }

    private static boolean strikeNearbyEnemies(
            LivingEntity attacker,
            SpinState state) {
        ServerLevel level = (ServerLevel) attacker.level();
        AABB area = attacker.getBoundingBox().inflate(SPIN_RADIUS);
        Vec3 center = attacker.position().add(0.0D, attacker.getBbHeight() * 0.5D, 0.0D);
        double radiusSqr = SPIN_RADIUS * SPIN_RADIUS;
        boolean damaged = false;
        float pulseDamage = state.attack.damage(SPIN_DAMAGE_PERCENT);
        DamageSource pulseSource = attacker.damageSources()
                .indirectMagic(attacker, attacker);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != attacker
                        && target.isAlive()
                        && !target.isSpectator()
                        && canSpinHit(attacker, target))) {
            Vec3 targetCenter = target.position().add(
                    0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (targetCenter.distanceToSqr(center) > radiusSqr
                    || !attacker.hasLineOfSight(target)) {
                continue;
            }
            target.invulnerableTime = 0;
            boolean landed = state.attack.withWeapon(attacker, pulseSource, () -> CombatStateManager.hurtAsHeavy(
                    attacker, target, pulseSource, pulseDamage));
            damaged |= landed;
            if (landed && attacker instanceof PathfinderMob npc) {
                NpcSpellcasting.releaseLingbeng(npc, target);
            }
        }
        return damaged;
    }

    private static boolean canSpinHit(LivingEntity attacker, LivingEntity target) {
        return attacker instanceof Mob
                ? NpcProfile.isCombatEnemy(attacker, target)
                : !NpcProfile.areCombatAllies(attacker, target);
    }

    private static void emitSpinParticles(LivingEntity attacker) {
        ServerLevel level = (ServerLevel) attacker.level();
        var particleTypes = MethodQiParticleEffects.particlesForOwner(attacker, "");
        int particlesPerType = Math.max(1, 36 / particleTypes.size());
        Vec3 forward = attacker.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = Vec3.directionFromRotation(0.0F, attacker.getYRot())
                    .multiply(1.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 center = attacker.position()
                .add(0.0D, attacker.getBbHeight() * 0.58D, 0.0D)
                .add(forward.scale(1.55D));
        double phase = level.getGameTime() * 0.42D;
        double radius = 1.45D;

        for (SimpleParticleType particleType : particleTypes) {
            for (int i = 0; i < particlesPerType; i++) {
                double angle = phase
                        + Math.PI * 2.0D * i / particlesPerType;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 radial = right.scale(cos * radius)
                        .add(0.0D, sin * radius, 0.0D);
                Vec3 velocity = radial.normalize().scale(0.045D)
                        .add(forward.scale(0.025D));
                Vec3 position = center.add(radial);
                level.sendParticles(
                        particleType,
                        position.x,
                        position.y,
                        position.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        1.0D);
            }
        }
    }

    /** Releases an incoming binding when an emergency escape artifact activates. */
    public static void releaseBoundTarget(LivingEntity target) {
        BindState state = BOUND_TARGETS.get(target.getUUID());
        if (state != null && !target.level().isClientSide) finishBinding(target, state);
    }

    private static void tickBoundTarget(LivingEntity target) {
        BindState state = BOUND_TARGETS.get(target.getUUID());
        if (state == null) {
            return;
        }
        long gameTime = target.level().getGameTime();
        if (!target.isAlive()
                || gameTime > state.expiresAt
                || state.hitsRemaining <= 0) {
            finishBinding(target, state);
            return;
        }
        if (gameTime < state.nextDamageTick) {
            return;
        }

        ServerLevel level = (ServerLevel) target.level();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(state.ownerId);
        if (owner == null) {
            finishBinding(target, state);
            return;
        }
        var source = owner.damageSources().indirectMagic(owner, owner);
        state.attack.withWeapon(owner, source, () -> target.hurt(source,
                state.attack.damage(BIND_DAMAGE_PERCENT_PER_SECOND)));
        state.hitsRemaining--;
        state.nextDamageTick += 20L;
        if (state.hitsRemaining <= 0) {
            finishBinding(target, state);
        }
    }

    private static void finishBinding(LivingEntity target, BindState state) {
        if (!BOUND_TARGETS.remove(target.getUUID(), state)) {
            return;
        }
        ServerLevel level = (ServerLevel) target.level();
        discardBindingPlume(level, state.plumeId);
        setBinding(state.boundStack, false);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(state.ownerId);
        if (owner != null && !hasActiveBinding(state.ownerId)) {
            triggerItemAnimation(owner, state.animatableId, "release");
            broadcastBindingPose(owner, false);
        }
    }

    private static void finishBindingsOwnedBy(
            ServerPlayer owner,
            boolean playRelease) {
        ArrayList<BindState> removedStates = new ArrayList<>();
        BOUND_TARGETS.entrySet().removeIf(entry -> {
            BindState state = entry.getValue();
            if (!state.ownerId.equals(owner.getUUID())) {
                return false;
            }
            removedStates.add(state);
            return true;
        });
        if (removedStates.isEmpty()) {
            return;
        }
        for (BindState state : removedStates) {
            discardBindingPlume(owner.serverLevel(), state.plumeId);
            setBinding(state.boundStack, false);
        }
        if (playRelease) {
            BindState latest = removedStates.get(removedStates.size() - 1);
            triggerItemAnimation(owner, latest.animatableId, "release");
        }
        broadcastBindingPose(owner, false);
    }

    private static boolean hasActiveBinding(UUID ownerId) {
        return BOUND_TARGETS.values().stream()
                .anyMatch(state -> state.ownerId.equals(ownerId));
    }

    private static boolean hasActiveBinding(UUID ownerId, long animatableId) {
        return BOUND_TARGETS.values().stream()
                .anyMatch(state -> state.ownerId.equals(ownerId)
                        && state.animatableId == animatableId);
    }

    private static boolean isBinding(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof FuchenItem
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBoolean(BINDING_TAG);
    }

    private static void setBinding(ItemStack stack, boolean binding) {
        if (stack == null || !(stack.getItem() instanceof FuchenItem)) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (binding) {
            tag.putBoolean(BINDING_TAG, true);
        } else {
            tag.remove(BINDING_TAG);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void discardBindingPlume(ServerLevel level, UUID plumeId) {
        if (plumeId == null) {
            return;
        }
        Entity visual = level.getEntity(plumeId);
        if (visual instanceof FuchenBindingPlumeEntity) {
            visual.discard();
        }
    }

    private static double physicalStrength(LivingEntity entity) {
        AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        double strength = attackDamage == null ? 0.0D : attackDamage.getValue();
        if (entity instanceof Player player) {
            strength += ModAttachments.getData(player).physicalAttack();
        }
        return strength;
    }

    private static double distanceToBounds(Player player, LivingEntity target) {
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        AABB bounds = target.getBoundingBox();
        double x = Math.max(bounds.minX, Math.min(origin.x, bounds.maxX));
        double y = Math.max(bounds.minY, Math.min(origin.y, bounds.maxY));
        double z = Math.max(bounds.minZ, Math.min(origin.z, bounds.maxZ));
        return origin.distanceTo(new Vec3(x, y, z));
    }

    private static LivingEntity findBindTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 maximumEnd = start.add(look.scale(BIND_REACH));
        HitResult blockHit = player.level().clip(new ClipContext(
                start,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS
                ? maximumEnd
                : blockHit.getLocation();
        AABB search = player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                search,
                candidate -> {
                    LivingEntity living = ModelBonePartEntity.resolveLiving(candidate);
                    return living != null
                            && living != player
                            && living.isAlive()
                            && candidate.isPickable()
                            && !living.isSpectator();
                });
        return hit == null ? null : ModelBonePartEntity.resolveLiving(hit.getEntity());
    }

    private static void triggerItemAnimation(ServerPlayer player, String animation) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof FuchenItem)) {
            return;
        }
        triggerItemAnimation(
                player,
                GeoItem.getOrAssignId(stack, player.serverLevel()),
                animation);
    }

    private static void triggerItemAnimation(
            ServerPlayer player,
            long animatableId,
            String animation) {
        if (!(ModItems.FUCHEN.get() instanceof FuchenItem item)) {
            return;
        }
        item.triggerAnim(
                player,
                animatableId,
                "main",
                animation);
    }

    private static void broadcastBindingPose(ServerPlayer player, boolean binding) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new CombatPayloads.ClientboundSwordComboAnimationPayload(
                        player.getUUID(),
                        binding
                                ? CombatPayloads.FUCHEN_BIND_ANIMATION_START
                                : CombatPayloads.FUCHEN_BIND_ANIMATION_STOP));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private FuchenItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new FuchenItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> {
                    ItemStack stack = state.getData(DataTickets.ITEMSTACK);
                    return state.setAndContinue(isBinding(stack)
                            ? BINDING_STATE_ANIMATION
                            : IDLE_ANIMATION);
                })
                .triggerableAnim("attack_a", ATTACK_ANIMATION_A)
                .triggerableAnim("attack_b", ATTACK_ANIMATION_B)
                .triggerableAnim("spin_a", SPIN_ANIMATION_A)
                .triggerableAnim("spin_b", SPIN_ANIMATION_B)
                .triggerableAnim("wrap_a", WRAP_ANIMATION_A)
                .triggerableAnim("wrap_b", WRAP_ANIMATION_B)
                .triggerableAnim("release", RELEASE_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static final class SpinState {
        private long nextPulseTick;
        private int completedSpins;
        private final ItemStack weapon;
        private final WeaponSkillAttack attack;

        private SpinState(
                long nextPulseTick,
                int completedSpins,
                ItemStack weapon,
                WeaponSkillAttack attack) {
            this.nextPulseTick = nextPulseTick;
            this.completedSpins = completedSpins;
            this.weapon = weapon;
            this.attack = attack;
        }
    }

    private static final class BindState {
        private final UUID ownerId;
        private final UUID plumeId;
        private final long animatableId;
        private final ItemStack boundStack;
        private long nextDamageTick;
        private final long expiresAt;
        private int hitsRemaining;
        private final WeaponSkillAttack attack;

        private BindState(
                UUID ownerId,
                UUID plumeId,
                long animatableId,
                ItemStack boundStack,
                long nextDamageTick,
                long expiresAt,
                int hitsRemaining,
                WeaponSkillAttack attack) {
            this.ownerId = ownerId;
            this.plumeId = plumeId;
            this.animatableId = animatableId;
            this.boundStack = boundStack;
            this.nextDamageTick = nextDamageTick;
            this.expiresAt = expiresAt;
            this.hitsRemaining = hitsRemaining;
            this.attack = attack;
        }
    }
}
