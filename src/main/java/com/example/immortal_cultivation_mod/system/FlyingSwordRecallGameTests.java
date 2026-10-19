package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.entity.FlyingSwordRecovery;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_sword_flight")
@PrefixGameTestTemplate(false)
public final class FlyingSwordRecallGameTests {
    private static FlyingSwordEntity release(GameTestHelper h,LifeSimulationGameTests.Player f,ItemStack stack){
        var p=f.player;var pos=h.absolutePos(new BlockPos(4,15,4));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setYRot(0);p.setXRot(0);
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY).withQi(1_000_000));
        h.assertTrue(FlyingSwordEntity.releaseControlledFromCase(p,List.of(stack))==1,"Actual owned stack deployed");
        return nearby(f).getFirst();
    }
    private static List<FlyingSwordEntity> nearby(LifeSimulationGameTests.Player f){return f.player.serverLevel().getEntitiesOfClass(FlyingSwordEntity.class,f.player.getBoundingBox().inflate(8),s->!s.isRemoved()&&f.player.getUUID().equals(s.getOwnerUuid()));}
    private static ItemStack exact(){var stack=new ItemStack(ModItems.FLYING_SWORD.get());stack.setDamageValue(37);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Forged recovery blade"));var tag=new CompoundTag();tag.putInt("ForgeRank",7);tag.putString("Engraving","preserve components");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    private static void cleanup(LifeSimulationGameTests.Player f){
        for(var level:f.player.server.getAllLevels())for(var sword:level.getAllEntities())if(sword instanceof FlyingSwordEntity s&&f.player.getUUID().equals(s.getOwnerUuid())){FlyingSwordRecovery.complete(s);s.discard();}
    }
    private static void wall(LifeSimulationGameTests.Player f){for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)f.player.serverLevel().setBlock(f.player.blockPosition().offset(x,y,6),Blocks.STONE.defaultBlockState(),3);}
    @GameTest(template="fate_test")
    public static void launchedSwordReturnsAfterTwoSecondsAndToggleStopsIt(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);wall(f);
            h.assertTrue(FlyingSwordRecovery.autoRecallEnabled(f.player),"Auto-recall defaults on");
            h.assertTrue(FlyingSwordEntity.shootControlled(f.player,false),"Real launch action");
            for(int i=0;i<43&&!sword.isControlled();i++)sword.tick();
            h.assertTrue(sword.isControlled()&&sword.distanceToSqr(f.player)<64,"Launched or embedded sword returns to formation");
            h.assertTrue(ItemStack.isSameItemSameComponents(expected,sword.pouchRecallStack()),"No forged components or durability lost");
            FlyingSwordRecovery.setAutoRecall(f.player,false);FlyingSwordEntity.shootControlled(f.player,false);
            for(int i=0;i<65;i++)sword.tick();
            h.assertTrue(!sword.isControlled()&&!sword.isRemoved(),"Disabled recall leaves actual embedded sword");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void highSpeedMovementCannotDropOrbitingSwordEvenWithRecallOff(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var sword=release(h,f,exact());FlyingSwordRecovery.setAutoRecall(f.player,false);
            f.player.setPos(f.player.position().add(100,20,100));sword.tick();
            h.assertTrue(sword.isControlled()&&sword.distanceToSqr(f.player)<64,"Teleport catches up instead of thirty-block drop");
            for(int i=0;i<10;i++){f.player.setPos(f.player.position().add(5,0,0));sword.tick();}
            h.assertTrue(sword.isControlled()&&sword.distanceToSqr(f.player)<100,"Fast flight cannot outrun orbit indefinitely");
            var start=h.absolutePos(new BlockPos(4,15,4));f.player.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);FlyingSwordRecovery.tickOwner(f.player);
            h.assertTrue(nearby(f).size()==1&&nearby(f).getFirst().isControlled(),"Owner tick recovers an orbital sword whose old chunk stopped ticking");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void unloadedSwordRecoversOnceAndLateChunkCopyIsRejected(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);var saved=sword.saveWithoutId(new CompoundTag());
            int health=sword.getSwordHealth();sword.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);FlyingSwordRecovery.tickOwner(f.player);
            var recovered=nearby(f);h.assertTrue(recovered.size()==1&&recovered.getFirst().isControlled(),"Unloaded orbit recovers next to owner");
            h.assertTrue(recovered.getFirst().getSwordHealth()==health&&ItemStack.isSameItemSameComponents(expected,recovered.getFirst().pouchRecallStack()),"Recovery preserves entity health and stack components");
            var late=new FlyingSwordEntity(ModEntities.FLYING_SWORD.get(),h.getLevel());late.load(saved);h.getLevel().addFreshEntity(late);late.tick();
            h.assertTrue(late.isRemoved(),"Stale saved UUID cannot revive a second sword");
            FlyingSwordRecovery.tickOwner(f.player);h.assertTrue(nearby(f).size()==1,"Repeated recovery is idempotent");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void riddenSwordCrossingHiddenChunkKeepsOriginalEntity(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);var start=f.player.position();
            h.assertTrue(FlyingSwordEntity.toggleRiding(f.player)&&sword.isSwordFlight(),"Actual sword-flight selection");
            FlyingSwordRecovery.setAutoRecall(f.player,false);
            f.player.setPos(start.add(512,0,512));sword.tick();
            h.assertTrue(!sword.isRemoved()&&h.getLevel().getEntity(sword.getUUID())==null,"Original blade still exists while destination chunk is hidden from entity lookup");
            h.assertTrue(FlyingSwordEntity.flightSword(f.player)==sword,"Hidden chunk cannot deselect the player's flight blade");
            FlyingSwordRecovery.tickOwner(f.player);
            var records=FlyingSwordRecovery.get(f.player.server).save(new CompoundTag(),f.player.registryAccess()).getList("Swords",Tag.TAG_COMPOUND);
            h.assertTrue(records.stream().map(t->(CompoundTag)t).filter(e->e.getUUID("Owner").equals(f.player.getUUID())).allMatch(e->e.getUUID("Entity").equals(sword.getUUID())),"Chunk visibility change must not create a replacement sword UUID");
            f.player.setPos(start);FlyingSwordRecovery.tickOwner(f.player);sword.tick();
            h.assertTrue(!sword.isRemoved()&&sword.isSwordFlight()&&nearby(f).size()==1&&nearby(f).getFirst()==sword,"Returning from hidden chunk retains one original ridden sword");
            h.assertTrue(f.player.getInventory().isEmpty()&&ItemStack.isSameItemSameComponents(expected,sword.pouchRecallStack()),"Chunk crossing cannot return an extra inventory copy");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void unloadedRiddenSwordRestoresFlightAndRejectsLateCopyBeforeSpawn(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);
            h.assertTrue(FlyingSwordEntity.toggleRiding(f.player),"Select actual flight blade");
            var saved=sword.saveWithoutId(new CompoundTag());
            h.assertTrue(saved.getBoolean("SwordFlight"),"Chunk save preserves selected flight mode");
            sword.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);FlyingSwordRecovery.tickOwner(f.player);
            var recovered=nearby(f);h.assertTrue(recovered.size()==1&&recovered.getFirst().isSwordFlight(),"Unloaded ridden blade returns as flight blade, not extra orbiting sword");
            var late=new FlyingSwordEntity(ModEntities.FLYING_SWORD.get(),h.getLevel());late.load(saved);
            h.assertTrue(!h.getLevel().addFreshEntity(late)&&late.isRemoved(),"Late chunk copy rejected before entity tracking or client spawn");
            FlyingSwordRecovery.tickOwner(f.player);
            h.assertTrue(nearby(f).size()==1&&ItemStack.isSameItemSameComponents(expected,recovered.getFirst().pouchRecallStack()),"Recovery retains exactly one original item");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void fullInventoryKeepsExactSwordInSavedCustodyUntilSpaceExists(GameTestHelper h)throws Exception{
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);for(int i=0;i<36;i++)f.player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            int dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,f.player.getBoundingBox().inflate(10)).size();
            FlyingSwordRecovery.park(sword);FlyingSwordRecovery.tickOwner(f.player);
            h.assertTrue(FlyingSwordRecovery.pendingCount(f.player)==1,"Full inventory retains pending sword");
            var saved=FlyingSwordRecovery.get(f.player.server).save(new CompoundTag(),f.player.registryAccess());
            var entry=saved.getList("Swords",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(e->e.getUUID("Owner").equals(f.player.getUUID())).findFirst().orElseThrow();
            h.assertTrue(ItemStack.isSameItemSameComponents(expected,ItemStack.parseOptional(f.player.registryAccess(),entry.getCompound("Stack"))),"World save contains exact item");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,f.player.getBoundingBox().inflate(10)).size()==dropped,"No despawning or burnable ground item fallback");
            f.player.getInventory().setItem(0,ItemStack.EMPTY);FlyingSwordRecovery.tickOwner(f.player);FlyingSwordRecovery.tickOwner(f.player);
            h.assertTrue(FlyingSwordRecovery.pendingCount(f.player)==0&&ItemStack.isSameItemSameComponents(expected,f.player.getInventory().getItem(0))&&f.player.getInventory().getItem(0).getCount()==1,"One exact stack returns when space opens");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void dimensionChangeKeepsOwnershipAndLeavesNoOldCopy(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,f,expected);var original=f.player.serverLevel();
            var old=sword.saveWithoutId(new CompoundTag());var target=f.player.server.getLevel(Level.NETHER);h.assertTrue(target!=null,"Destination available");
            FlyingSwordRecovery.setAutoRecall(f.player,false);f.player.setServerLevel(target);FlyingSwordRecovery.tickOwner(f.player);
            h.assertTrue(sword.isRemoved()&&ItemStack.isSameItemSameComponents(expected,f.player.getInventory().getItem(0)),"Dimension change safely returns exact stack even with recall disabled");
            f.player.setServerLevel(original);var late=new FlyingSwordEntity(ModEntities.FLYING_SWORD.get(),original);late.load(old);original.addFreshEntity(late);late.tick();
            h.assertTrue(late.isRemoved()&&f.player.getInventory().getItem(0).getCount()==1,"Returning to old dimension cannot duplicate recovered sword");cleanup(f);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void logoutDeathAndForeignOwnerCannotLoseOrStealCustody(GameTestHelper h){
        try(var owner=new LifeSimulationGameTests.Player(h);var foreign=new LifeSimulationGameTests.Player(h)){
            var expected=exact();var sword=release(h,owner,expected);
            FlyingSwordRecovery.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(owner.player));
            h.assertTrue(sword.isRemoved()&&FlyingSwordRecovery.pendingCount(owner.player)==1,"Logout parks physical sword");
            FlyingSwordRecovery.tickOwner(foreign.player);h.assertTrue(FlyingSwordRecovery.pendingCount(owner.player)==1,"Another player cannot claim it");
            owner.player.setHealth(0);FlyingSwordRecovery.tickOwner(owner.player);h.assertTrue(FlyingSwordRecovery.pendingCount(owner.player)==1,"Dead player cannot consume pending stack");
            owner.player.setHealth(20);FlyingSwordRecovery.tickOwner(owner.player);
            h.assertTrue(ItemStack.isSameItemSameComponents(expected,owner.player.getInventory().getItem(0)),"Sword survives logout and returns to living owner");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void manualCaseCollectionRetiresClaimAndSettingSurvivesClone(GameTestHelper h){
        try(var f=new LifeSimulationGameTests.Player(h);var clone=new LifeSimulationGameTests.Player(h)){
            var sword=release(h,f,exact());var collected=FlyingSwordEntity.collectControlledForCase(f.player);
            h.assertTrue(collected.size()==1&&sword.isRemoved(),"Actual case collection consumes deployed entity");
            FlyingSwordRecovery.tickOwner(f.player);h.assertTrue(nearby(f).isEmpty(),"Manually returned sword cannot recover twice");
            FlyingSwordRecovery.setAutoRecall(f.player,false);FlyingSwordRecovery.clone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone.player,f.player,true));
            h.assertTrue(!FlyingSwordRecovery.autoRecallEnabled(clone.player),"Per-player choice survives death clone");
        }h.succeed();
    }
}
