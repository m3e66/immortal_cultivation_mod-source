package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.entity.TemporaryTerrainEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real executions granted by 玄岳碎锋诀. */
public final class XuanyueTechniques {
    private static final Set<String> SPELLS = Set.of(
            ModSpells.XUANYUE_BI,
            ModSpells.JINYAN_SHENGFENG,
            ModSpells.SHANSUO_ZHENYU,
            ModSpells.WANFENG_YUEZANG);

    private XuanyueTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(ServerPlayer player, String spellId) {
        if (!isTechnique(spellId)) return false;
        if (!CultivationMethods.isXuanyueSuifengJue(
                ModAttachments.getData(player).activeCultivationMethod())) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".xuanyue_wrong_method"), true);
            return false;
        }
        return true;
    }

    public static boolean cast(
            ServerPlayer player, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        if (!canCast(player, spellId)) return false;
        return castActor(player, null, spellId, chargeScale, projectileHook);
    }

    public static boolean cast(PathfinderMob caster, LivingEntity target, String spellId) {
        return XuanyueSuifengJue.hasMethod(caster)
                && castActor(caster, target, spellId, SpellCharge.NORMAL, entity -> { });
    }

    private static boolean castActor(
            LivingEntity caster, LivingEntity target, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized) || !(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 look = target == null
                ? caster.getLookAngle().normalize()
                : target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize();
        float speed = ModSpells.JINYAN_SHENGFENG.equals(normalized) ? 2.6F : 1.75F;
        SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                level, caster, normalized, chargeScale);
        Vec3 start = caster.getEyePosition().add(look.scale(0.72D));
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(look.x, look.y, look.z, speed, 0.0F);
        projectileHook.accept(projectile);
        castingVfx(level, caster, normalized);
        level.playSound(null, caster.blockPosition(), SoundEvents.STONE_PLACE,
                SoundSource.PLAYERS, 0.9F, ModSpells.JINYAN_SHENGFENG.equals(normalized)
                        ? 1.65F : 0.82F);
        return level.addFreshEntity(projectile);
    }

    public static void activateTriggeredSpell(
            ServerLevel level, LivingEntity caster, String spellId,
            Vec3 impact, LivingEntity hitTarget, float chargeScale) {
        String normalized = ModSpells.normalizeId(spellId);
        Vec3 center = hitTarget == null ? impact : hitTarget.position();
        switch (normalized) {
            case ModSpells.XUANYUE_BI -> raiseWall(level, caster, impact);
            case ModSpells.JINYAN_SHENGFENG -> growMetalBlades(
                    level, caster, center, chargeScale);
            case ModSpells.SHANSUO_ZHENYU -> createGravityDomain(
                    level, caster, center, chargeScale);
            case ModSpells.WANFENG_YUEZANG -> createBurial(
                    level, caster, center, chargeScale);
            default -> {
            }
        }
    }

    public static void tickTriggerProjectile(SpellTriggerProjectileEntity projectile) {
        if (!isTechnique(projectile.spellId())
                || !(projectile.level() instanceof ServerLevel level)) return;
        Vec3 direction = projectile.getDeltaMovement().lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : projectile.getDeltaMovement().normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
        else side = side.normalize();
        double phase = projectile.tickCount * 0.55D;
        for (int i = 0; i < 4; i++) {
            double angle = phase + i * Math.PI * 0.5D;
            Vec3 point = projectile.position().add(side.scale(Math.cos(angle) * 0.22D))
                    .add(0.0D, Math.sin(angle) * 0.22D, 0.0D);
            level.sendParticles(i % 2 == 0 ? ModParticles.METHOD_QI_METAL.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        float base = switch (ModSpells.normalizeId(spellId)) {
            case ModSpells.XUANYUE_BI -> 200.0F;
            case ModSpells.JINYAN_SHENGFENG -> 360.0F;
            case ModSpells.SHANSUO_ZHENYU -> 180.0F;
            case ModSpells.WANFENG_YUEZANG -> 820.0F;
            default -> 0.0F;
        };
        return SpellCharge.damage(base, chargeScale);
    }

    public static float qiCostMultiplier(LivingEntity caster) {
        return XuanyueSuifengJue.isGroundConnected(caster) ? 0.90F : 1.0F;
    }

    private static void raiseWall(ServerLevel level, LivingEntity caster, Vec3 impact) {
        int realmPower = realmPower(caster);
        int width = 5 + realmPower * 2;
        int height = 4 + realmPower;
        Vec3 forward = caster.getLookAngle();
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        BlockPos ground = findSurface(level, BlockPos.containing(impact).above(3), 10);
        Map<BlockPos, BlockState> placements = new LinkedHashMap<>();
        for (int lateral = -width / 2; lateral <= width / 2; lateral++) {
            BlockPos columnGround = findSurface(level, BlockPos.containing(
                    Vec3.atCenterOf(ground).add(side.scale(lateral))).above(2), 7);
            for (int y = 1; y <= height; y++) {
                BlockPos pos = columnGround.above(y);
                placements.put(pos, wallState(realmPower, lateral, y));
            }
        }
        if (realmPower >= 1) {
            int wingLength = realmPower + 2;
            for (int sign : new int[]{-1, 1}) {
                Vec3 edge = Vec3.atCenterOf(ground).add(side.scale(sign * width / 2.0D));
                for (int depth = 1; depth <= wingLength; depth++) {
                    BlockPos wingGround = findSurface(level, BlockPos.containing(
                            edge.subtract(horizontal.scale(depth))).above(2), 7);
                    for (int y = 1; y <= Math.max(3, height - 1); y++) {
                        placements.put(wingGround.above(y), wallState(realmPower, depth, y));
                    }
                }
            }
        }
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.TECHNIQUES, caster,
                () -> TemporaryTerrainEntity.place(level, placements, 300));
        MethodTechniqueAreaEntity wallVfx = MethodTechniqueAreaEntity.create(
                level, caster, MethodTechniqueAreaEntity.Kind.XUANYUE_WALL,
                Vec3.atCenterOf(ground.above()), horizontal,
                SpellCharge.NORMAL, realmPower);
        if (wallVfx != null) level.addFreshEntity(wallVfx);
        Vec3 pathStart = new Vec3(caster.getX(), ground.getY() + 1.08D, caster.getZ());
        Vec3 pathEnd = Vec3.atCenterOf(ground).add(0.0D, 0.58D, 0.0D);
        Vec3 path = pathEnd.subtract(pathStart);
        int pathSamples = Math.max(8, (int) Math.ceil(path.length() * 2.2D));
        for (int sample = 0; sample <= pathSamples; sample++) {
            Vec3 point = pathStart.add(path.scale(sample / (double) pathSamples));
            level.sendParticles(sample % 4 == 0
                            ? ModParticles.METHOD_QI_METAL.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    point.x, point.y, point.z, 1,
                    0.0D, 0.015D, 0.0D, 0.0D);
        }
        SpellImpactParticles.shockRing(level, Vec3.atCenterOf(ground),
                ModParticles.METHOD_QI_EARTH.get(), 0.7D, 46, 0.22D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState()),
                ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D,
                70, width * 0.42D, height * 0.42D, 0.45D, 0.12D);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                ground.getX() + 0.5D, ground.getY() + height * 0.55D, ground.getZ() + 0.5D,
                35, width * 0.42D, height * 0.40D, 0.38D, 0.035D);
    }

    private static BlockState wallState(int realmPower, int x, int y) {
        if ((Math.abs(x) + y) % 7 == 0) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        return switch (realmPower) {
            case 0 -> Blocks.STONE_BRICKS.defaultBlockState();
            case 1 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case 2 -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            default -> Blocks.OBSIDIAN.defaultBlockState();
        };
    }

    private static void growMetalBlades(
            ServerLevel level, LivingEntity caster, Vec3 center, float chargeScale) {
        Map<BlockPos, BlockState> placements = new LinkedHashMap<>();
        BlockPos origin = BlockPos.containing(center);
        int limit = 28 + realmPower(caster) * 8;
        for (BlockPos stone : BlockPos.betweenClosed(
                origin.offset(-5, -4, -5), origin.offset(5, 4, 5))) {
            if (placements.size() >= limit || !isStone(level.getBlockState(stone))) continue;
            BlockPos blade = stone.above();
            if (TemporaryTerrainEntity.canReplace(level, blade)) {
                placements.put(blade.immutable(), Blocks.POINTED_DRIPSTONE.defaultBlockState());
            }
        }
        if (placements.isEmpty()) {
            BlockPos ground = findSurface(level, origin.above(3), 10);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if ((x + z) % 2 == 0) {
                        placements.put(ground.offset(x, 1, z),
                                Blocks.POINTED_DRIPSTONE.defaultBlockState());
                    }
                }
            }
        }
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.TECHNIQUES, caster,
                () -> TemporaryTerrainEntity.place(level, placements, 100));
        MethodTechniqueAreaEntity area = MethodTechniqueAreaEntity.create(
                level, caster, MethodTechniqueAreaEntity.Kind.JINYAN_SPIKES,
                center, caster.getLookAngle(), chargeScale, realmPower(caster));
        if (area != null) level.addFreshEntity(area);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                center.x, center.y + 0.7D, center.z,
                55, 3.4D, 1.0D, 3.4D, 0.15D);
    }

    private static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.GILDED_BLACKSTONE);
    }

    private static void createGravityDomain(
            ServerLevel level, LivingEntity caster, Vec3 center, float chargeScale) {
        BlockPos ground = findSurface(level, BlockPos.containing(center).above(3), 10);
        int realmPower = realmPower(caster);
        MethodTechniqueAreaEntity area = MethodTechniqueAreaEntity.create(
                level, caster, MethodTechniqueAreaEntity.Kind.SHANSUO_DOMAIN,
                Vec3.atCenterOf(ground.above()), caster.getLookAngle(), chargeScale, realmPower);
        if (area != null) level.addFreshEntity(area);
        Map<BlockPos, BlockState> ridges = new LinkedHashMap<>();
        double radius = 6.0D + Math.max(0, realmPower - 1) * 1.5D;
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0D * i / 28.0D;
            BlockPos surface = findSurface(level, BlockPos.containing(
                    Vec3.atCenterOf(ground).add(Math.cos(angle) * radius, 2.0D,
                            Math.sin(angle) * radius)), 7);
            ridges.put(surface.above(), i % 4 == 0
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        }
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.TECHNIQUES, caster,
                () -> TemporaryTerrainEntity.place(level, ridges, 160));
        SpellImpactParticles.shockRing(level, Vec3.atCenterOf(ground),
                ModParticles.METHOD_QI_EARTH.get(), 1.0D, 54, 0.30D);
    }

    private static void createBurial(
            ServerLevel level, LivingEntity caster, Vec3 center, float chargeScale) {
        BlockPos ground = findSurface(level, BlockPos.containing(center).above(4), 12);
        MethodTechniqueAreaEntity area = MethodTechniqueAreaEntity.create(
                level, caster, MethodTechniqueAreaEntity.Kind.WANFENG_BURIAL,
                Vec3.atCenterOf(ground.above()), caster.getLookAngle(), chargeScale,
                realmPower(caster));
        if (area != null) level.addFreshEntity(area);
    }

    private static int realmPower(LivingEntity caster) {
        if (XuanyueSuifengJue.isAtLeast(caster,
                com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_HUASHEN
                        + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY)) return 3;
        if (XuanyueSuifengJue.isAtLeast(caster,
                com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_YUANYING
                        + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY)) return 2;
        if (XuanyueSuifengJue.isAtLeast(caster,
                com.example.immortal_cultivation_mod.attachment.CultivationLevels.REALM_JINDAN
                        + com.example.immortal_cultivation_mod.attachment.CultivationLevels.STAGE_EARLY)) return 1;
        return 0;
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos origin, int search) {
        for (int offset = search; offset >= -search; offset--) {
            BlockPos ground = origin.offset(0, offset, 0);
            BlockState state = level.getBlockState(ground);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && state.isFaceSturdy(level, ground, Direction.UP)
                    && TemporaryTerrainEntity.canReplace(level, ground.above())) {
                return ground;
            }
        }
        return origin.below();
    }

    private static void castingVfx(
            ServerLevel level, LivingEntity caster, String spellId) {
        Vec3 center = caster.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                center.x, center.y, center.z,
                14, 0.45D, 0.70D, 0.45D, 0.04D);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                center.x, center.y, center.z,
                ModSpells.WANFENG_YUEZANG.equals(spellId) ? 28 : 14,
                0.52D, 0.75D, 0.52D, 0.055D);
    }
}
