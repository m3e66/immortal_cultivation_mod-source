package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_corpse")
@PrefixGameTestTemplate(false)
public final class CorpseHitboxGameTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<=8;x++)for(int z=0;z<=8;z++) {
            h.setBlock(new net.minecraft.core.BlockPos(x,3,z),net.minecraft.world.level.block.Blocks.STONE);
            for(int y=4;y<=12;y++)h.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);
        }
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void unsupportedCorpseFallsDespiteStaleGroundFlag(GameTestHelper h) {
        floor(h);
        var corpse=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        corpse.setPos(h.absoluteVec(new Vec3(4.5,8,4.5)));corpse.setOnGround(true);
        corpse.setHealth(1);
        h.getLevel().addFreshEntity(corpse);
        h.runAfterDelay(30,()->{try {
            close(h,corpse.getY(),h.absolutePos(new net.minecraft.core.BlockPos(4,4,4)).getY(),"Corpse lands on real floor");
            h.assertTrue(corpse.onGround() && !corpse.isNoGravity(),"Landed corpse has gravity and actual support");
            h.assertTrue(corpse.isAlive() && corpse.getHealth() == 1, "Even a damaged corpse survives landing without losing HP");
            close(h,corpse.getDeltaMovement().y,0,"Landing clears downward velocity");h.succeed();
        }finally{corpse.discard();}});
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void removingSupportMakesSealedCorpseFall(GameTestHelper h) {
        floor(h);
        var support=new net.minecraft.core.BlockPos(4,7,4);
        h.setBlock(support,net.minecraft.world.level.block.Blocks.STONE);
        var corpse=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        corpse.setRaised(true);corpse.setSealed(true);
        corpse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);corpse.setHealth(1000);
        corpse.setPos(h.absoluteVec(new Vec3(4.5,8,4.5)));corpse.setOnGround(true);h.getLevel().addFreshEntity(corpse);
        h.runAfterDelay(3,()->h.setBlock(support,net.minecraft.world.level.block.Blocks.AIR));
        h.runAfterDelay(35,()->{try {
            close(h,corpse.getY(),h.absolutePos(new net.minecraft.core.BlockPos(4,4,4)).getY(),"Sealed no-AI corpse falls when support breaks");
            h.assertTrue(corpse.isSealed()&&corpse.isRaised()&&corpse.isNoAi(),"Falling preserves seal and standing pose");h.succeed();
        }finally{corpse.discard();}});
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void reloadedFloatingCorpseResumesGravity(GameTestHelper h) {
        floor(h);
        var source=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        source.setPos(h.absoluteVec(new Vec3(4.5,8,4.5)));source.setOnGround(true);source.setNoGravity(true);
        source.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);source.setHealth(1000);
        var restored=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        restored.load(source.saveWithoutId(new CompoundTag()));source.discard();h.getLevel().addFreshEntity(restored);
        h.runAfterDelay(30,()->{try {
            close(h,restored.getY(),h.absolutePos(new net.minecraft.core.BlockPos(4,4,4)).getY(),"Loaded floating body settles");
            h.assertTrue(!restored.isNoGravity()&&!restored.isRaised(),"Old save flags cannot suspend a prone body");h.succeed();
        }finally{restored.discard();}});
    }
    @GameTest(template="fate_test")
    public static void fallDamageImmunityCoversEveryCorpsePose(GameTestHelper h) {
        for (int pose = 0; pose < 3; pose++) {
            var corpse = ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
            try {
                corpse.setRaised(pose > 0);
                corpse.setSealed(pose == 2);
                float health = corpse.getHealth();
                h.assertTrue(!corpse.causeFallDamage(100, 1, corpse.damageSources().fall()), "Long fall never damages corpse pose " + pose);
                close(h, corpse.getHealth(), health, "Fall preserves corpse health");
                h.assertTrue(corpse.hurt(corpse.damageSources().generic(), 1), "Corpse remains vulnerable to ordinary damage");
                h.assertTrue(corpse.getHealth() < health, "Fall immunity does not grant general invulnerability");
            } finally { corpse.discard(); }
        }
        h.succeed();
    }

    private static FakePlayer player(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"corpse-hitbox"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,4,2))));
        return player;
    }
    private static void close(GameTestHelper h,double actual,double expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<.001,message+": "+actual+" expected "+expected);
    }

    @GameTest(template="fate_test")
    public static void proneBoxFollowsBodyYawScaleAndMovementWithoutStandingTargetArea(GameTestHelper h) {
        var source=player(h); var corpse=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        try {
            source.setYRot(0); corpse.copyFrom(source);
            var box=corpse.getBoundingBox();
            close(h,box.getXsize(),1,"Body width"); close(h,box.getZsize(),2,"Body length"); close(h,box.getYsize(),.5,"Body thickness");
            close(h,corpse.getBbHeight(),.5,"Dimension metadata agrees with flat box");
            close(h,corpse.getEyeHeight(),.25,"Eye marker lies inside flat body");
            h.assertTrue(box.clip(corpse.position().add(-2,.25,0),corpse.position().add(2,.25,0)).isPresent(),"Body remains targetable");
            h.assertTrue(box.clip(corpse.position().add(-2,1.5,0),corpse.position().add(2,1.5,0)).isEmpty(),"Air above body is no longer a target");
            source.setYRot(90); corpse.copyFrom(source); box=corpse.getBoundingBox();
            close(h,box.getXsize(),2,"East/west length rotates with corpse"); close(h,box.getZsize(),1,"East/west width");
            source.setYRot(45); corpse.copyFrom(source); box=corpse.getBoundingBox();
            close(h,box.getXsize(),3/Math.sqrt(2),"Diagonal envelope covers rotated body");
            var before=corpse.position(); corpse.noPhysics=true;
            corpse.move(MoverType.SELF,new Vec3(0,.2,0));
            close(h,corpse.getX(),before.x,"Flat box does not drift horizontally during gravity");
            close(h,corpse.getZ(),before.z,"Flat box stays centered during movement");
            source.getAttribute(Attributes.SCALE).setBaseValue(2); corpse.copyFrom(source);
            close(h,corpse.getBoundingBox().getYsize(),1,"Inherited scale doubles thickness");
            close(h,corpse.getBoundingBox().getXsize(),6/Math.sqrt(2),"Inherited scale doubles footprint");
        } finally { source.discard(); corpse.discard(); }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void actualSealingTalismanRaisesBodyAndChangesHitboxImmediately(GameTestHelper h) {
        var player=player(h); var corpse=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        try {
            corpse.copyFrom(player);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ZHENSHI_TALISMAN.get(),2));
            h.assertTrue(UndeadControl.interactEntity(player,corpse,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"Real talisman interaction succeeds");
            h.assertTrue(corpse.isRaised()&&corpse.isSealed()&&corpse.isNoAi(),"Sealed corpse stands motionless");
            close(h,corpse.getBoundingBox().getYsize(),1.8,"Sealing immediately restores standing hitbox");
            close(h,corpse.getBoundingBox().getXsize(),.6,"Standing box has normal width");
            h.assertTrue(player.getMainHandItem().getCount()==1&&corpse.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ZHENSHI_SEAL_HELMET.get()),"Existing talisman consumption and seal helmet preserved");
            corpse.setSealed(false);
            close(h,corpse.getBoundingBox().getYsize(),1.8,"Unsealed raised corpse stays upright");
            corpse.setRaised(false);
            close(h,corpse.getBoundingBox().getYsize(),.5,"Laying down restores flat box");
        } finally { player.discard(); corpse.discard(); }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void dimensionsSurviveSaveLoadAndSyncedPoseChanges(GameTestHelper h) {
        var source=player(h); var corpse=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        var restored=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        var replica=ModEntities.CULTIVATOR_CORPSE.get().create(h.getLevel());
        try {
            source.setYRot(90); corpse.copyFrom(source);
            restored.load(corpse.saveWithoutId(new CompoundTag()));
            close(h,restored.getBoundingBox().getYsize(),.5,"Saved prone corpse loads flat");
            close(h,restored.getBoundingBox().getXsize(),2,"Saved orientation restores footprint");
            corpse.getEntityData().packDirty(); corpse.setRaised(true); corpse.setSealed(true);
            replica.getEntityData().assignValues(corpse.getEntityData().packDirty());
            close(h,replica.getBoundingBox().getYsize(),1.8,"Receiving synced raised flag refreshes box without setter");
            restored.load(corpse.saveWithoutId(new CompoundTag()));
            h.assertTrue(restored.isSealed()&&restored.isRaised(),"Saved sealed pose restored");
            close(h,restored.getBoundingBox().getYsize(),1.8,"Saved sealed corpse loads upright");
            corpse.setSealed(false); corpse.setRaised(false);
            replica.getEntityData().assignValues(corpse.getEntityData().packDirty());
            close(h,replica.getBoundingBox().getYsize(),.5,"Receiving synced lying flag restores flat box");
        } finally { source.discard(); corpse.discard(); restored.discard(); replica.discard(); }
        h.succeed();
    }
}
