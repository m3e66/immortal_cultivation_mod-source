package com.example.immortal_cultivation_mod.physics;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class DragonLookGameTests {
    @GameTest(template="fate_test") public static void verticalLookEasesAndReturnsToRest(GameTestHelper h) {
        float pitch=0;
        for(int i=0;i<45;i++) {
            float next=DragonLookPose.advance(pitch,-90);
            h.assertTrue(next<=pitch && pitch-next<=4.001,"Looking up changes smoothly without snapping");pitch=next;
        }
        h.assertTrue(pitch==-40,"Upward look is bounded to protect the neck");
        for(int i=0;i<60;i++) {
            float next=DragonLookPose.advance(pitch,90);
            h.assertTrue(next>=pitch && next-pitch<=4.001,"Reversing from up to down stays smooth");pitch=next;
        }
        h.assertTrue(pitch==50,"Downward look is bounded");
        for(int i=0;i<45;i++)pitch=DragonLookPose.advance(pitch,0);
        h.assertTrue(pitch==0,"The neck returns exactly to its authored rest pose");h.succeed();
    }
    @GameTest(template="fate_test") public static void lookingMovesHeadAndJawHitboxes(GameTestHelper h) {
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());
        dragon.setNoAi(true);dragon.setNoGravity(true);dragon.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4,20,4)));
        dragon.setXRot(0);dragon.tick();double rest=headY(dragon,"head"),jawRest=headY(dragon,"jaw");
        dragon.setXRot(-40);for(int i=0;i<40;i++)dragon.tick();
        h.assertTrue(dragon.getLookPitch(1)<-39 && headY(dragon,"head")>rest+1 && headY(dragon,"jaw")>jawRest+1,
                "Looking up raises the real head and jaw collision parts");
        dragon.setXRot(50);dragon.tick();
        h.assertTrue(dragon.getLookPitch(.5F)<0,"Render interpolation does not snap across a look reversal");
        for(int i=0;i<50;i++)dragon.tick();
        h.assertTrue(dragon.getLookPitch(1)>49 && headY(dragon,"head")<rest-1 && headY(dragon,"jaw")<jawRest-1,
                "Looking down lowers the real head and jaw collision parts");
        dragon.discard();h.succeed();
    }
    private static double headY(CrimsonSkyFlameFloodDragonEntity dragon,String name) {
        for(var part:dragon.getParts())if(part instanceof ModelBonePartEntity<?> bone && bone.boneName().equals(name))
            return bone.getBoundingBox().getCenter().y;
        throw new AssertionError("Missing dragon collision part: "+name);
    }
}
