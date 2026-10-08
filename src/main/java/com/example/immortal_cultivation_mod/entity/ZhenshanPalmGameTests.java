package com.example.immortal_cultivation_mod.entity;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ZhenshanPalmGameTests {
    @GameTest(template = "npc_combat_arena")
    public static void palmFrontTracksEveryFlightDirection(GameTestHelper h) {
        for (Vec3 direction : new Vec3[]{new Vec3(1,0,0), new Vec3(-1,0,0),
                new Vec3(0,0,1), new Vec3(0,0,-1), new Vec3(0,1,0), new Vec3(0,-1,0),
                new Vec3(1,2,-3), new Vec3(-2,-3,1), new Vec3(.001,1,.002)}) {
            Vec3 forward = direction.normalize();
            var rotation = ZhenshanPalmShape.rotation(direction);
            var front = rotation.transform(new Vector3f(0,0,-1));
            h.assertTrue(new Vec3(front.x,front.y,front.z).distanceTo(forward) < .00001,
                    "Palm face follows shot, including vertical and diagonal shots: " + direction);
            var right = rotation.transform(new Vector3f(1,0,0));
            Vec3 collisionRight = ZhenshanPalmShape.right(forward);
            h.assertTrue(new Vec3(right.x,right.y,right.z).distanceTo(collisionRight) < .00001,
                    "Rendered silhouette and collision frame agree");
        }
        h.succeed();
    }

    @GameTest(template = "npc_combat_arena")
    public static void reflectedPalmRetainsFacingAfterReload(GameTestHelper h) {
        var palm = new ZhenshanPalmEntity(ModEntities.ZHENSHAN_PALM.get(), h.getLevel());
        Vec3 reflected = new Vec3(-.6,.7,-.2);
        palm.forcefieldBounce(reflected);
        var tag = new CompoundTag();
        palm.saveWithoutId(tag);
        var loaded = new ZhenshanPalmEntity(ModEntities.ZHENSHAN_PALM.get(), h.getLevel());
        loaded.load(tag);
        h.assertTrue(loaded.forward().distanceTo(reflected.normalize()) < .00001,
                "Synced reflected direction survives save/reload");
        var front = ZhenshanPalmShape.rotation(loaded.forward()).transform(new Vector3f(0,0,-1));
        h.assertTrue(new Vec3(front.x,front.y,front.z).distanceTo(reflected.normalize()) < .00001,
                "Reloaded palm faces its new direction, not original yaw");
        palm.discard(); loaded.discard(); h.succeed();
    }
}
