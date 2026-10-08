package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Block damage invoked by explicit beast moves, never by ordinary movement. */
public final class BeastMoveBlockBreaker {
    private BeastMoveBlockBreaker() {
    }

    /** Only beast realms are amplified; cultivators using the helper keep their existing limits. */
    public static int destructiveRealm(Mob beast) {
        if (beast instanceof CrimsonSkyFlameFloodDragonEntity || beast instanceof BloodApeDemonGeneralEntity) return 4;
        if (beast instanceof PhantomVeilFoxEntity fox)
            return com.example.immortal_cultivation_mod.attachment.CultivationLevels.getRealmIndex(fox.cultivationRealm());
        if (beast instanceof XuanjinZhengEntity zheng) {
            int stage = com.example.immortal_cultivation_mod.companion.XuanjinRules.stage(zheng.seal()).realmStage();
            return stage >= 10 ? 4 : stage >= 7 ? 3 : 2;
        }
        if (beast instanceof CrimsonScaleFlameLizardEntity
                || beast instanceof CultivationBestiaryBeastEntity bestiary && bestiary.kind() == BestiaryBeastKind.EMBER_FEATHER_FIRE_OWL) return 3;
        return 0;
    }

    public static int blockLimit(Mob beast, int base) {
        int realm = destructiveRealm(beast);
        return realm >= 3 ? (int)Math.min(realm >= 4 ? 16384L : 8192L,
                Math.max(0L, (long)base) * (realm >= 4 ? 96 : 64)) : base;
    }

    public static int breakSphere(ServerLevel level, Mob beast, Vec3 center,
                                  double horizontalRadius, double verticalRadius,
                                  int maximumBlocks, float maximumHardness) {
        if (horizontalRadius <= 0.0D || verticalRadius <= 0.0D || maximumBlocks <= 0 || maximumHardness < 0) {
            return 0;
        }
        int realm = destructiveRealm(beast);
        final double radius = realm >= 3 ? Math.min(20, horizontalRadius * (realm >= 4 ? 3 : 2.5)) : horizontalRadius;
        final double height = realm >= 3 ? Math.min(10, verticalRadius * (realm >= 4 ? 3 : 2)) : verticalRadius;
        int limit = blockLimit(beast, maximumBlocks);
        if (realm >= 3) {
            // Budget the whole crater, so dense rock does not leave a random half-excavated volume.
            limit = Math.min(realm >= 4 ? 16384 : 8192,
                    Math.max(limit, (int)Math.ceil(4.2 * radius * radius * height)));
        }
        long seed = level.random.nextLong();
        AABB bounds = new AABB(center, center).inflate(
                radius, height, radius);
        return breakBlocks(level, beast, bounds, realm >= 3 ? beast.getBoundingBox().getCenter() : center, limit,
                realm >= 3 ? Math.max(maximumHardness, realm >= 4 ? 50 : 12) : maximumHardness, pos -> {
                    return com.example.immortal_cultivation_mod.spell.SpellDestruction.fractured(
                            pos, center, radius, height, seed);
                });
    }

    public static int breakBox(ServerLevel level, Mob beast, AABB bounds,
                               Vec3 attackOrigin, int maximumBlocks,
                               float maximumHardness) {
        if (destructiveRealm(beast) >= 3) {
            // Heavy sweeps excavate a continuous uneven crater instead of a few scattered surface blocks.
            return breakSphere(level, beast, bounds.getCenter(),
                    Math.max(bounds.getXsize(), bounds.getZsize()) * .5,
                    Math.max(2, bounds.getYsize() * .5), maximumBlocks, maximumHardness);
        }
        return breakBlocks(level, beast, bounds, attackOrigin, maximumBlocks,
                maximumHardness, pos -> true);
    }

    /** One bounded job for the whole breath path, avoiding many overlapping block-volume scans. */
    public static int breakBreath(ServerLevel level, Mob beast, Vec3 origin, Vec3 direction, double range) {
        if (range <= 0 || direction.lengthSqr() < 1e-6) return 0;
        Vec3 forward = direction.normalize();
        Vec3 end = origin.add(forward.scale(range));
        return breakBlocks(level, beast, new AABB(origin, end).inflate(3.5), origin, 8192, 50, pos -> {
            Vec3 offset = Vec3.atCenterOf(pos).subtract(origin);
            double along = offset.dot(forward);
            if (along < 0 || along > range) return false;
            double radius = 2.2 + along / range + .25 * Math.sin(along * .9);
            return offset.subtract(forward.scale(along)).lengthSqr() <= radius * radius;
        });
    }

    private static int breakBlocks(ServerLevel level, Mob beast, AABB bounds,
                                   Vec3 attackOrigin, int maximumBlocks,
                                   float maximumHardness,
                                   Predicate<BlockPos> shapeTest) {
        if (maximumBlocks <= 0 || maximumHardness < 0 || !beast.isAlive()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return 0;
        return BlockBreakingSettings.withSource(
                BlockBreakingSettings.Category.COMPANIONS, beast,
                () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.area(level, beast, bounds,
                attackOrigin, maximumBlocks, maximumHardness,
                pos -> shapeTest.test(pos) && level.hasChunkAt(pos)
                        && (level.getBlockState(pos).getFluidState().isEmpty()
                        || level.getBlockState(pos).getFluidState().is(net.minecraft.tags.FluidTags.WATER)), null));
    }

}
