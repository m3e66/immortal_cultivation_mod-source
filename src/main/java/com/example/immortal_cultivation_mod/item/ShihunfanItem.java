package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.ShihunfanEntity;
import com.example.immortal_cultivation_mod.entity.ShihunfanGhostEntity;
import com.example.immortal_cultivation_mod.entity.ShihunfanSoulWaveEntity;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShihunfanItem extends Item implements GeoItem, SoulStorageWeapon {
    private static final String SOUL_AMOUNT = ImmortalCultivationMod.MODID + ".shihunfan_souls";
    private static final String LAST_SOUL_WIND_SLASH = "ImmortalCultivationLastSoulWindSlash";
    public static final String CAPTURED_SOUL_TAG = "ImmortalCultivationShihunfanCapturedSoul";
    private static final int BEAM_QI_COST = 100;
    private static final int HUNDRED_GHOST_QI_COST = 500;
    private static final int HUNDRED_GHOST_COOLDOWN_TICKS = 20 * 10;
    private static final double SOUL_WIND_RANGE = 5.0D;
    private static final double BEAM_RANGE = 32.0D;
    private static final long[] SOUL_CAPACITIES = {
            50L, 100L, 250L, 750L, 2_500L, 10_000L, 50_000L, 500_000L
    };
    private static final DustParticleOptions DARK_DUST =
            new DustParticleOptions(new Vector3f(0.14F, 0.01F, 0.20F), 1.25F);
    private static final DustParticleOptions BLACK_MIST =
            new DustParticleOptions(new Vector3f(0.025F, 0.02F, 0.035F), 1.55F);
    private static final DustParticleOptions SMOKY_GRAY =
            new DustParticleOptions(new Vector3f(0.18F, 0.19F, 0.21F), 1.15F);
    private static final DustParticleOptions GHOST_CYAN =
            new DustParticleOptions(new Vector3f(0.11F, 0.43F, 0.47F), 1.0F);
    private static final DustParticleOptions GHOST_GREEN =
            new DustParticleOptions(new Vector3f(0.40F, 0.68F, 0.47F), 0.9F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShihunfanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ShihunfanItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ShihunfanItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WeaponBinding.checkUse(stack, player)) return InteractionResultHolder.fail(stack);
        WeaponBinding.prepare(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        InteractionResultHolder<ItemStack> result = player.isShiftKeyDown()
                ? deploy(level, serverPlayer, hand, stack)
                : hundredGhostMarch(serverPlayer, stack)
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.fail(stack);
        if (result.getResult().consumesAction()) {
            com.example.immortal_cultivation_mod.network.WeaponUseAnimations.play(serverPlayer,
                    com.example.immortal_cultivation_mod.network.WeaponUseAnimations.Type.BANNER);
        }
        return result;
    }

    public static boolean soulWindSlash(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.SHIHUNFAN.get()) || !player.isShiftKeyDown() || !WeaponBinding.checkUse(stack, player)) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLong(LAST_SOUL_WIND_SLASH) < 4L) {
            return false;
        }
        double souls = getSoulAmount(stack);
        if (souls <= 0.0D) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".shihunfan.no_souls"), true);
            return false;
        }
        player.getPersistentData().putLong(LAST_SOUL_WIND_SLASH, now);

        Vec3 forward = horizontalDirection(player.getLookAngle());
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
        float soulDensity = soulDensity(stack, souls);
        double baseDamage = Math.max(1.0D, QiInfusedWeapon.baseAttackDamage(stack));
        float damage = (float) Math.min(1_000_000_000.0D, baseDamage + souls * 0.05D * 10.0D);
        setSoulAmount(stack, souls * 0.99D);

        ServerLevel level = player.serverLevel();
        renderSoulWindSlash(level, origin, forward, soulDensity);
        AABB area = player.getBoundingBox().inflate(SOUL_WIND_RANGE, 3.0D, SOUL_WIND_RANGE);
        DamageSource damageSource = player.damageSources()
                .indirectMagic(player, player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> isEnemy(player, living))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            double distance = horizontal.length();
            if (distance > SOUL_WIND_RANGE || Math.abs(offset.y) > 2.75D
                    || (distance > 1.0E-4D && horizontal.scale(1.0D / distance).dot(forward) < 0.5D)) {
                continue;
            }
            WeaponBinding.withWeapon(stack, damageSource, () -> target.hurt(damageSource, damage));
            renderSoulImpact(level, target.getBoundingBox().getCenter(), soulDensity, true);
        }
        return true;
    }

    private static boolean hundredGhostMarch(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        double souls = getSoulAmount(stack);
        if (souls <= 0.0D) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".shihunfan.no_souls"), true);
            return false;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (data.qi() < HUNDRED_GHOST_QI_COST) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"), true);
            return false;
        }

        double baseDamage = Math.max(1.0D, QiInfusedWeapon.baseAttackDamage(stack));
        float damage = (float) Math.min(1_000_000_000.0D, baseDamage + souls * 5.0D);
        WeaponBinding.prepare(stack);
        ShihunfanSoulWaveEntity wave = ShihunfanSoulWaveEntity.create(
                player.serverLevel(), player, player.getLookAngle(), damage, soulDensity(stack, souls));
        if (wave != null) wave.setBindingWeapon(stack);
        if (wave == null) {
            return false;
        }
        Vec3 ghostTarget = hundredGhostTarget(player);
        ModAttachments.setData(player, data.withQi(data.qi() - HUNDRED_GHOST_QI_COST));
        ServerEvents.syncPlayerData(player);
        setSoulAmount(stack, souls * 0.95D);
        player.serverLevel().addFreshEntity(wave);
        spawnHundredGhosts(player.serverLevel(), player, ghostTarget);
        player.getCooldowns().addCooldown(stack.getItem(), HUNDRED_GHOST_COOLDOWN_TICKS);
        return true;
    }

    private static Vec3 hundredGhostTarget(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 maximumEnd = start.add(look.scale(32.0D));
        HitResult blockHit = level.clip(new ClipContext(
                start, maximumEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS
                ? maximumEnd
                : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(look.scale(32.0D)).inflate(3.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, player, start, blockedEnd, search,
                candidate -> {
                    LivingEntity living = ModelBonePartEntity.resolveLiving(candidate);
                    return living != null && isEnemy(player, living);
                });
        return entityHit == null
                ? blockedEnd
                : entityHit.getEntity().getBoundingBox().getCenter();
    }

    private static void spawnHundredGhosts(ServerLevel level, ServerPlayer player, Vec3 target) {
        Vec3 forward = horizontalDirection(player.getLookAngle());
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 50; i++) {
            double sideOffset = (level.random.nextDouble() - 0.5D) * 12.0D;
            double forwardOffset = -2.0D + level.random.nextDouble() * 8.0D;
            double heightOffset = -1.0D + level.random.nextDouble() * 6.0D;
            Vec3 start = center.add(side.scale(sideOffset))
                    .add(forward.scale(forwardOffset))
                    .add(0.0D, heightOffset, 0.0D);
            Vec3 scatteredTarget = target.add(
                    (level.random.nextDouble() - 0.5D) * 2.0D,
                    (level.random.nextDouble() - 0.5D) * 2.0D,
                    (level.random.nextDouble() - 0.5D) * 2.0D);
            ShihunfanGhostEntity ghost = ModEntities.SHIHUNFAN_GHOST.get().create(level);
            if (ghost != null) {
                ghost.initialize(start, scatteredTarget);
                level.addFreshEntity(ghost);
            }
        }
    }

    private static InteractionResultHolder<ItemStack> deploy(
            Level level, ServerPlayer player, InteractionHand hand, ItemStack stack) {
        ShihunfanEntity entity = ShihunfanEntity.create(
                level, player, placementPosition(player), stack.copyWithCount(1));
        if (entity == null) {
            return InteractionResultHolder.fail(stack);
        }
        level.addFreshEntity(entity);
        stack.shrink(1);
        player.setItemInHand(hand, stack);
        return InteractionResultHolder.success(stack);
    }

    private static Vec3 placementPosition(ServerPlayer player) {
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            var pos = blockHit.getBlockPos().relative(blockHit.getDirection());
            return Vec3.atBottomCenterOf(pos);
        }
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return player.position().add(horizontal.normalize().scale(2.0D));
    }

    private static boolean fireBeam(ServerPlayer player, ItemStack stack) {
        if (!WeaponBinding.checkUse(stack, player)) return false;
        double souls = getSoulAmount(stack);
        if (souls <= 0.0D) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".shihunfan.no_souls"), true);
            return false;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (data.qi() < BEAM_QI_COST) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"), true);
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.45D));
        Vec3 maximumEnd = start.add(player.getLookAngle().scale(BEAM_RANGE));
        HitResult blockHit = level.clip(new ClipContext(
                start, maximumEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockedEnd = blockHit.getType() == HitResult.Type.MISS ? maximumEnd : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(BEAM_RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, player, start, blockedEnd, search,
                candidate -> {
                    LivingEntity living = ModelBonePartEntity.resolveLiving(candidate);
                    return living != null && living.isAlive() && living != player;
                });
        Vec3 end = entityHit == null ? blockedEnd : entityHit.getLocation();

        ModAttachments.setData(player, data.withQi(data.qi() - BEAM_QI_COST));
        ServerEvents.syncPlayerData(player);
        setSoulAmount(stack, souls * 0.95D);

        renderBeam(level, start, end);
        LivingEntity target = entityHit == null
                ? null : ModelBonePartEntity.resolveLiving(entityHit.getEntity());
        if (target != null) {
            float damage = (float) Math.min(1_000_000_000.0D, souls * 5.0D);
            var source = player.damageSources().indirectMagic(player, player);
            WeaponBinding.withWeapon(stack, source, () -> target.hurt(source, damage));
        }
        return true;
    }

    public static boolean fireNpcBeam(PathfinderMob caster, LivingEntity target, ItemStack stack) {
        if (!(caster instanceof CultivationNpc npc)
                || !target.isAlive()
                || caster.distanceToSqr(target) > BEAM_RANGE * BEAM_RANGE) {
            return false;
        }
        double souls = getSoulAmount(stack);
        if (souls <= 0.0D || NpcProfile.qi(caster, npc.npcArchetype()) < BEAM_QI_COST) {
            return false;
        }

        ServerLevel level = (ServerLevel) caster.level();
        caster.lookAt(target, 30.0F, 30.0F);
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.45D));
        Vec3 targetEnd = target.getEyePosition();
        HitResult blockHit = level.clip(new ClipContext(
                start, targetEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        boolean blocked = blockHit.getType() != HitResult.Type.MISS
                && start.distanceToSqr(blockHit.getLocation()) + 0.25D < start.distanceToSqr(targetEnd);
        if (blocked || !NpcProfile.isCombatEnemy(caster, target)) {
            return false;
        }
        Vec3 end = targetEnd;

        if (!NpcProfile.spendQi(caster, npc.npcArchetype(), BEAM_QI_COST)) {
            return false;
        }
        setSoulAmount(stack, souls * 0.95D);
        renderBeam(level, start, end);
        float damage = (float) Math.min(1_000_000_000.0D, souls * 5.0D);
        var source = caster.damageSources().indirectMagic(caster, caster);
        WeaponBinding.withWeapon(stack, source, () -> target.hurt(source, damage));
        return true;
    }

    private static void renderBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += 0.3D) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(DARK_DUST, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
            if (((int) (distance * 10.0D)) % 9 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                        1, 0.02D, 0.02D, 0.02D, 0.01D);
            }
        }
        level.sendParticles(ParticleTypes.WITCH, end.x, end.y, end.z,
                18, 0.3D, 0.3D, 0.3D, 0.06D);
    }

    private static void renderSoulWindSlash(ServerLevel level, Vec3 origin, Vec3 forward, float density) {
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        int layers = 2 + Math.round(density * 3.0F);
        double outerRadius = 4.25D + density * 0.75D;
        for (int angle = -62; angle <= 62; angle += 4) {
            double radians = Math.toRadians(angle);
            Vec3 radial = forward.scale(Math.cos(radians)).add(side.scale(Math.sin(radians)));
            for (int layer = 0; layer < layers; layer++) {
                double radius = outerRadius - layer * 0.20D;
                double height = (layer - (layers - 1) * 0.5D) * 0.18D;
                Vec3 point = origin.add(radial.scale(radius)).add(0.0D, height, 0.0D);
                level.sendParticles(layer % 2 == 0 ? BLACK_MIST : DARK_DUST,
                        point.x, point.y, point.z, 2, 0.12D, 0.12D, 0.12D, 0.01D);
                level.sendParticles(layer % 3 == 0 ? GHOST_CYAN : GHOST_GREEN,
                        point.x, point.y, point.z, 1, 0.07D, 0.07D, 0.07D, 0.0D);
                if ((angle + layer) % 12 == 0) {
                    level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                            1 + Math.round(density * 2.0F), 0.12D, 0.12D, 0.12D, 0.015D);
                }
            }
        }
        for (double distance = 1.0D; distance <= outerRadius; distance += 0.55D) {
            Vec3 center = origin.add(forward.scale(distance));
            level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z,
                    3 + Math.round(density * 5.0F), distance * 0.32D, 0.28D, distance * 0.32D, 0.035D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                    2 + Math.round(density * 4.0F), distance * 0.24D, 0.24D, distance * 0.24D, 0.025D);
        }
    }

    public static void renderSoulImpact(
            ServerLevel level, Vec3 point, float density, boolean large) {
        double spread = large ? 0.75D : 0.4D;
        int multiplier = large ? 2 : 1;
        level.sendParticles(BLACK_MIST, point.x, point.y, point.z,
                multiplier * (10 + Math.round(density * 14.0F)), spread, spread, spread, 0.06D);
        level.sendParticles(DARK_DUST, point.x, point.y, point.z,
                multiplier * (7 + Math.round(density * 10.0F)), spread, spread, spread, 0.08D);
        level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                multiplier * (3 + Math.round(density * 7.0F)), spread, spread, spread, 0.08D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, point.x, point.y, point.z,
                multiplier * (2 + Math.round(density * 5.0F)), spread, spread, spread, 0.05D);
        level.sendParticles(ParticleTypes.ASH, point.x, point.y, point.z,
                multiplier * 8, spread, spread, spread, 0.04D);
    }

    public static void renderSoulTrail(ServerLevel level, Vec3 start, Vec3 end, float density) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += 0.25D) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(distance % 0.5D < 0.25D ? GHOST_CYAN : GHOST_GREEN,
                    point.x, point.y, point.z, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            if (((int) (distance * 4.0D)) % 3 == 0) {
                level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                        1 + Math.round(density), 0.04D, 0.04D, 0.04D, 0.015D);
            }
        }
    }

    public static void storeCapturedSouls(ServerPlayer player, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        ItemStack banner = findBanner(player);
        if (banner.isEmpty()) {
            return;
        }
        setSoulAmount(banner, getSoulAmount(banner) + amount);
    }

    private static ItemStack findBanner(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.SHIHUNFAN.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModItems.SHIHUNFAN.get())) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SHIHUNFAN.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isEnemy(ServerPlayer player, LivingEntity target) {
        return target.isAlive() && target != player && !player.isAlliedTo(target) && player.canAttack(target);
    }

    private static Vec3 horizontalDirection(Vec3 look) {
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-5D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : horizontal.normalize();
    }

    private static float soulDensity(ItemStack stack, double souls) {
        return (float) Math.min(1.0D, Math.sqrt(souls / Math.max(1.0D, maxSoulCapacity(stack))));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof Player player)
                || player.tickCount % 4 != 0
                || (!player.getMainHandItem().equals(stack) && !player.getOffhandItem().equals(stack))) {
            return;
        }
        double souls = getSoulAmount(stack);
        float density = soulDensity(stack, souls);
        Vec3 point = player.position().add(0.0D, 0.12D, 0.0D);
        serverLevel.sendParticles(DARK_DUST, point.x, point.y, point.z,
                2 + Math.round(density * 7.0F), 0.34D, 0.10D, 0.34D, 0.015D);
        serverLevel.sendParticles(SMOKY_GRAY, point.x, point.y, point.z,
                1 + Math.round(density * 4.0F), 0.38D, 0.12D, 0.38D, 0.012D);
        serverLevel.sendParticles(density > 0.5F ? GHOST_GREEN : GHOST_CYAN,
                point.x, point.y, point.z, 1 + Math.round(density * 3.0F),
                0.30D, 0.10D, 0.30D, 0.01D);
        if (density > 0.15F) {
            serverLevel.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                    1 + Math.round(density * 3.0F), 0.28D, 0.10D, 0.28D, 0.012D);
            serverLevel.sendParticles(ParticleTypes.ASH, point.x, point.y, point.z,
                    1 + Math.round(density * 4.0F), 0.38D, 0.12D, 0.38D, 0.018D);
        }
    }

    public static double getSoulAmount(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.min(maxSoulCapacity(stack), Math.max(0.0D, tag.getDouble(SOUL_AMOUNT)));
    }

    public static void setSoulAmount(ItemStack stack, double amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        amount = Math.min(maxSoulCapacity(stack), Math.max(0.0D, amount));
        if (amount <= 1.0E-6D) {
            tag.remove(SOUL_AMOUNT);
        } else {
            tag.putDouble(SOUL_AMOUNT, amount);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long maxSoulCapacity(ItemStack stack) {
        int tier = Math.max(0, Math.min(
                SOUL_CAPACITIES.length - 1, WeaponRestrictionSystem.tier(stack)));
        return SOUL_CAPACITIES[tier];
    }

    @Override
    public double storedSouls(ItemStack stack) {
        return getSoulAmount(stack);
    }

    @Override
    public void setStoredSouls(ItemStack stack, double amount) {
        setSoulAmount(stack, amount);
    }

    @Override
    public double soulCapacity(ItemStack stack) {
        return maxSoulCapacity(stack);
    }

    public static String formatSoulAmount(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + ".shihunfan.souls",
                formatSoulAmount(getSoulAmount(stack)), maxSoulCapacity(stack)));
    }
}
