package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.IceFxAnchorEntity;
import com.example.immortal_cultivation_mod.network.SpellVfxPayloads;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class NativeSpellVfxGameTests {
    @GameTest(template = "fate_test")
    public static void qiParticlesMapOneToOneToVisibleFragments(GameTestHelper helper) {
        double radius = 4;
        var active = ExternalQiVfxShape.allSections(radius).without(18);
        double end = -1;
        for (int particle = 0; particle < ExternalQiVfxShape.sectionCount(radius); particle++) {
            double start = ExternalQiVfxShape.particleStart(radius, particle);
            double next = ExternalQiVfxShape.particleEnd(radius, particle);
            helper.assertTrue(Math.abs(start - end) < 1e-9 && next > start,
                    "Particle collision and VFX spans cover the blade without gaps or overlaps");
            helper.assertTrue(ExternalQiVfxShape.section(radius, (start + next) * .5) == particle,
                    "Each visible strip maps to exactly its own invisible particle");
            helper.assertTrue(ExternalQiVfxShape.sectionActive(active, radius, (start + next) * .5) == (particle != 18),
                    "Consuming one particle hides only its own VFX strip");
            end = next;
        }
        helper.assertTrue(Math.abs(end - 1) < 1e-9, "Particle coverage reaches the opposite pointed tip");
        var dimension = helper.getLevel().dimension().location();
        var shape = com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.QI_LEVEL;
        var first = new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect(
                dimension, shape, Vec3.ZERO, new Vec3(0,0,1), 4, 1, 0xffffff, ExternalQiVfxShape.allSections(4), 42);
        var update = new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect(
                dimension, shape, new Vec3(0,0,1), new Vec3(0,0,2), 4, 2, 0xffffff, active, 42);
        var other = new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect(
                dimension, shape, Vec3.ZERO, new Vec3(0,0,1), 4, 1, 0xffffff, active, 43);
        helper.assertTrue(update.replaces(first) && !update.replaces(other),
                "Consumed-fragment updates replace only the matching projectile's old VFX");
        var stopped = new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect(
                dimension, shape, Vec3.ZERO, new Vec3(0,0,1), 4, 3, 0xffffff, new ExternalQiVfxShape.BladeMask(0,0), 42);
        helper.assertTrue(stopped.replaces(update) && stopped.emptyBlade(),
                "Fully consumed particles clear the prior visual immediately");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void externalQiGeometryAndPacketStayAtConfirmedHitPath(GameTestHelper helper) {
        helper.assertTrue(ExternalQiVfxShape.sectionCount(4) == 36
                        && ExternalQiVfxShape.sectionCount(32) == 36,
                "All slash sizes use 36 independent invisible particles");
        var pieces = ExternalQiVfxShape.allSections(32).without(2);
        helper.assertTrue(!pieces.active(2) && pieces.active(0) && pieces.active(35)
                        && !pieces.active(36), "Particle mask consumes only the selected piece");
        double previousExtension = 0;
        for (double radius : new double[]{.2, 2, 8, 32}) {
            double extension = ExternalQiVfxShape.tipExtension(radius);
            helper.assertTrue(extension > previousExtension, "Thicker blades get longer tips");
            previousExtension = extension;
            helper.assertTrue(ExternalQiVfxShape.bladeTaper(radius, 0) == 1
                            && ExternalQiVfxShape.bladeTaper(radius, 1) == 0
                            && ExternalQiVfxShape.bladeTaper(radius, -1) == 0,
                    "Shared blade silhouette keeps a full middle and sharp endpoints");
            double previousTaper = 1;
            for (int i = 1; i <= 100; i++) {
                double t = i / 100D;
                double taper = ExternalQiVfxShape.bladeTaper(radius, t);
                helper.assertTrue(taper <= previousTaper && taper >= 0
                                && taper == ExternalQiVfxShape.bladeTaper(radius, -t),
                        "Both tips narrow continuously without flaring back out");
                previousTaper = taper;
            }
        }
        for (Vec3 forward : new Vec3[]{new Vec3(0,0,1),new Vec3(0,1,0),new Vec3(0,-1,0),new Vec3(1,.7,-.3).normalize()}) {
            for (int stage=0;stage<3;stage++) {
                Vec3 axis=ExternalQiVfxShape.axis(forward,stage);
                helper.assertTrue(Math.abs(axis.length()-1)<1e-6 && Math.abs(axis.dot(forward))<1e-6,
                        "Vertical and diagonal attack axes remain finite and perpendicular");
                for (double radius : new double[]{.2,2,32}) {
                    Vec3 center=new Vec3(123,65,-48);
                    helper.assertTrue(ExternalQiVfxShape.crescent(center,forward,axis,radius,0).equals(center),
                            "Blade center stays on the server projectile position");
                    for(int i=0;i<=32;i++) {
                        Vec3 offset=ExternalQiVfxShape.crescent(center,forward,axis,radius,-1+i/16D).subtract(center);
                        helper.assertTrue(Math.abs(offset.dot(axis))<=ExternalQiVfxShape.bladeReach(radius)+1e-6 && offset.dot(forward)>=-.900001
                                        && offset.dot(forward)<=1e-6,
                                "Crescent stays inside the extended blade reach and swept collision depth");
                    }
                }
            }
        }
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),helper.getLevel().registryAccess());
        try {
            for(var shape:com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.Visual.values()) {
                for (long mask : new long[]{0, 1, 2, 4, 5, -1L, Long.MIN_VALUE, 0x5555555555555555L}) {
                var expected=new com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect(
                        helper.getLevel().dimension().location(),shape,new Vec3(1,2,3),new Vec3(2,3,4),32,1234,0xe45b70,new ExternalQiVfxShape.BladeMask(mask, ~mask), 123);
                com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect.CODEC.encode(buffer,expected);
                helper.assertTrue(expected.equals(com.example.immortal_cultivation_mod.network.HigherWeaponPayloads.SkillEffect.CODEC.decode(buffer)),
                        "Both old and new VFX packets preserve world positions, radius, timing and method color");
                buffer.clear();
                }
            }
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 35)
    public static void externalQiStillDamagesOnceAndStopsAtWalls(GameTestHelper helper) {
        var owner=helper.spawn(EntityType.ZOMBIE,new BlockPos(3,3,2));owner.setNoAi(true);owner.setNoGravity(true);
        var target=helper.spawn(EntityType.COW,new BlockPos(3,3,7));target.setNoAi(true);target.setNoGravity(true);
        var blockedOwner=helper.spawn(EntityType.ZOMBIE,new BlockPos(8,3,2));blockedOwner.setNoAi(true);blockedOwner.setNoGravity(true);
        var blockedTarget=helper.spawn(EntityType.COW,new BlockPos(8,3,7));blockedTarget.setNoAi(true);blockedTarget.setNoGravity(true);
        float initialHealth=target.getHealth();
        float blockedInitialHealth=blockedTarget.getHealth();
        for(int x=7;x<=9;x++)for(int y=3;y<=6;y++)helper.setBlock(new BlockPos(x,y,5),Blocks.STONE);
        // Exercise the non-destructive mode explicitly now that NPC slashes can cut terrain.
        var griefing = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean previousGriefing = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.releaseNpc(owner,target,12,3,.8,"water",0,false),
                    "Normal slash launches with native VFX");
            helper.assertTrue(com.example.immortal_cultivation_mod.entity.WeaponQiSlashEntity.releaseNpc(blockedOwner,blockedTarget,12,3,.8,"fire",1,false),
                    "Wall-bound slash launches with native VFX");
        } finally {
            griefing.set(previousGriefing, helper.getLevel().getServer());
        }
        helper.runAfterDelay(18,()->{
            helper.assertTrue(Math.abs(target.getHealth()-(initialHealth-3))<.001,"Visual upgrade retains exactly one three-damage hit");
            helper.assertTrue(blockedTarget.getHealth()==blockedInitialHealth,"Native VFX cannot cause damage through terrain");
            helper.assertTrue(helper.getBlockState(new BlockPos(8,4,5)).is(Blocks.STONE),"Non-destructive slash still leaves walls intact");
            helper.succeed();
        });
    }

    @GameTest(template = "fate_test")
    public static void authoredEmitterScaleAndTimingSurviveConversion(GameTestHelper helper) throws Exception {
        try (var reader = new InputStreamReader(SpellEmitterPresets.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/vfx/spell_emitters.json"), StandardCharsets.UTF_8)) {
            var profiles = SpellEmitterPresets.read(reader);
            helper.assertTrue(profiles.keySet().equals(Set.of("fireball", "flying_sword", "ball", "beam",
                    "lingbeng", "meditating", "puddle", "rage3", "water1", "water2", "water3", "ice1", "ice2")),
                    "All thirteen previously used effects are present");
            helper.assertTrue(profiles.values().stream().mapToInt(java.util.List::size).sum() == 14,
                    "The light projectile retains both its particle emitter and beam");
            var random = RandomSource.create(9814);
            var meditation = profiles.get("meditating").getFirst();
            int emitted = 0;
            for (int tick = 0; tick < 100; tick++) emitted += meditation.emissionCount(tick, random);
            helper.assertTrue(emitted == 50 && meditation.lifetime[0] == 50,
                    "Fractional emission is per tick, and particle lifetime is fifty ticks");
            var puddle = profiles.get("puddle").getFirst();
            helper.assertTrue(puddle.emissionCount(0, random) == 80 && puddle.scale[0] == 2
                    && puddle.offset().y == -0.5D, "Puddle retains its density, scale and ground offset");
            for (int stack = 1; stack <= 3; stack++) {
                var water = profiles.get("water" + stack).getFirst();
                helper.assertTrue(water.scale[0] == 2.5F + stack * 0.5F && water.maxParticles == 500
                        && water.lifetime[0] == 1000 && water.builtinCircle && water.rate == 10,
                        "Shield layers retain their original sphere sizes and particle budget");
            }
            var beam = profiles.get("beam").getFirst();
            Vec3 end = SpellEmitterPresets.rotate(SpellEmitterPresets.multiply(
                    new Vec3(beam.end[0], beam.end[1], beam.end[2]), beam.scale), beam.rotation());
            helper.assertTrue(end.distanceTo(new Vec3(0, 30, 0)) < 0.0001D
                            && Math.abs(beam.width - 0.2F) < 0.00001F && beam.offset().y == -10,
                    "Fortune beam is thirty blocks tall with its original narrow width and eye-relative offset");
            var light = profiles.get("ball").get(1);
            helper.assertTrue(light.beam() && light.width == 1.5F && light.end[2] == -3,
                    "Light projectile retains the broad white beam instead of the replacement orb");
            var sword = profiles.get("flying_sword").getFirst();
            helper.assertTrue(sword.maxParticles == 1 && sword.speed[0] == 0
                            && Math.abs(sword.trail.lifetime * sword.lifetime[0] - 30) < 0.0001F,
                    "Sword trail lasts thirty ticks and follows a single stationary local particle");
            var start = SpellEmitterPresets.gradient(sword.trail.color, 0, 0);
            var endColor = SpellEmitterPresets.gradient(sword.trail.color, 1, 0);
            helper.assertTrue(Math.abs(start.x - 67 / 255.0F) < 0.0001F
                            && endColor.x == 1 && endColor.y == 1 && endColor.z == 1,
                    "Authored blue-to-white trail gradient is preserved");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void nativeEmitterShapesMatchAuthoredVolumes(GameTestHelper helper) throws Exception {
        try (var reader = new InputStreamReader(SpellEmitterPresets.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/vfx/spell_emitters.json"), StandardCharsets.UTF_8)) {
            var profiles = SpellEmitterPresets.read(reader);
            var random = RandomSource.create(532714);
            double sphereRadiusSquared = 0;
            for (int i = 0; i < 2048; i++) {
                var sphere = profiles.get("water1").getFirst().spawn(random);
                sphereRadiusSquared += sphere.position().lengthSqr();
                helper.assertTrue(sphere.position().length() <= 0.500001D
                                && Math.abs(sphere.velocity().length() - 0.00005D) < 0.0000001D,
                        "Water fills the sphere volume at the original very slow speed");
                var disk = profiles.get("puddle").getFirst().spawn(random);
                helper.assertTrue(disk.position().y == 0 && disk.position().length() <= 0.500001D
                                && disk.velocity().y == 0, "Puddle remains flat and expands horizontally");
                var fire = profiles.get("fireball").getFirst().spawn(random);
                helper.assertTrue(fire.position().y == 0 && fire.position().length() <= 0.100001D
                                && Math.abs(fire.velocity().length() - 0.5D) < 0.000001D
                                && fire.velocity().normalize().y >= Math.cos(Math.toRadians(10)) - 0.000001D,
                        "Fireball particles stay inside the authored ten-degree emission cone");
            }
            helper.assertTrue(Math.abs(sphereRadiusSquared / 2048 - 0.15D) < 0.01D,
                    "Sphere particles are volume-uniform, rather than concentrated on a shell or at the center");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void visualPacketsPreserveAnchorsAndRemoval(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            for (var kind : SpellVfxPayloads.Kind.values()) {
                for (int duration : new int[]{0, 30}) {
                    var expected = new SpellVfxPayloads.Visual(helper.getLevel().dimension().location(),
                            kind, kind == SpellVfxPayloads.Kind.WATER_SHIELD ? 42 : -1,
                            new Vec3(-12.5D, 72.0D, 4.5D), 3, duration);
                    SpellVfxPayloads.Visual.CODEC.encode(buffer, expected);
                    helper.assertTrue(expected.equals(SpellVfxPayloads.Visual.CODEC.decode(buffer)),
                            "Start/stop packets retain their exact dimension, anchor, stack and lifetime");
                    helper.assertTrue(!buffer.isReadable(), "Visual packet consumes exactly its own bytes");
                    buffer.clear();
                }
            }
            var beam = new SpellVfxPayloads.BeamMarkers(helper.getLevel().dimension().location(),
                    new int[]{1, 128, 65000}, 320);
            SpellVfxPayloads.BeamMarkers.CODEC.encode(buffer, beam);
            var decoded = SpellVfxPayloads.BeamMarkers.CODEC.decode(buffer);
            helper.assertTrue(Arrays.equals(beam.entityIds(), decoded.entityIds())
                            && beam.dimension().equals(decoded.dimension()) && decoded.duration() == 320,
                    "Private beam targets and root-adjusted duration survive the network");
            helper.assertTrue(!buffer.isReadable(), "Beam packet consumes exactly its own bytes");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 60)
    public static void nativeVisualsPreserveShieldAndCleanUpIce(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos block = helper.absolutePos(new BlockPos(3, 2, 3));
        Vec3 pos = Vec3.atCenterOf(block);
        var pig = EntityType.PIG.create(level);
        pig.setPos(pos);
        pig.setNoAi(true);
        level.addFreshEntity(pig);
        try {
            DielangShield.activateNext(pig);
            helper.assertTrue(DielangShield.remaining(pig) == 12.0F, "First stack retains 12 shield");
            DielangShield.activateNext(pig);
            helper.assertTrue(DielangShield.remaining(pig) == 36.0F, "Second stack retains 36 shield");
            DielangShield.activateNext(pig);
            helper.assertTrue(DielangShield.remaining(pig) == 108.0F, "Third stack retains 108 shield");
            helper.assertTrue(DielangShield.absorb(pig, 120.0F) == 12.0F && !DielangShield.hasShield(pig),
                    "Breaking the final shield stops it and passes only excess damage");
            level.setBlockAndUpdate(block.below(), Blocks.STONE.defaultBlockState());
            float health = pig.getHealth();
            NativeSpellEffects.lingbengExplosion(level, pos, 4.0F);
            NativeSpellEffects.puddle(level, pos.x, block.getY(), pos.z);
            NativeSpellEffects.removePuddle(level, block);
            NativeSpellEffects.deadSkeletonPressure(level, block);
            helper.assertTrue(pig.getHealth() == health && level.getBlockState(block.below()).is(Blocks.STONE),
                    "VFX do not add damage or place/delete gameplay blocks");
        } finally {
            DielangShield.clear(pig);
            pig.discard();
        }
        NativeSpellEffects.iceImpact(level, pos.x, pos.y, pos.z);
        var anchors = level.getEntitiesOfClass(IceFxAnchorEntity.class, new AABB(block).inflate(1.0D));
        helper.assertTrue(anchors.size() == 1, "Ice effect has one native tracked anchor");
        var anchor = anchors.getFirst();
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(anchor.isRemoved(), "Ice effect disappears after its 30-tick lifetime");
            helper.succeed();
        });
    }
}
