package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameAftermathGameTests {
    @GameTest(template = "fate_test")
    public static void everyFlameTransformsOnlyItsSurvivingCraterFloor(GameTestHelper h) {
        var level = h.getLevel();
        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "flame-aftermath"));
        owner.getAbilities().mayBuild = true;
        int index = 0;
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            var center = h.absolutePos(new BlockPos(index++ * 12, 20, 0));
            var floor = center.below(4);
            owner.setPos(Vec3.atCenterOf(center));
            for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(floor.above(y), Blocks.STONE.defaultBlockState());
            var protectedFloor = floor.east();
            level.setBlockAndUpdate(protectedFloor, Blocks.BEDROCK.defaultBlockState());
            var shot = ModEntities.FLAME_SHOT.get().create(level);
            shot.setOwner(owner);
            shot.setKind(kind);
            FlameTerrainDestruction.destroy(level, owner, shot, Vec3.atCenterOf(center), 3);
            h.assertTrue(level.getBlockState(floor).equals(FlameTerrainDestruction.aftermath(kind, floor)),
                    "Surviving crater floor receives its flame palette: " + kind);
            h.assertTrue(level.getBlockState(center).isAir(), "Aftermath leaves crater interior empty");
            h.assertTrue(level.getBlockState(protectedFloor).is(Blocks.BEDROCK), "Unbreakable lining is preserved");
        }
        h.succeed();
    }
}
