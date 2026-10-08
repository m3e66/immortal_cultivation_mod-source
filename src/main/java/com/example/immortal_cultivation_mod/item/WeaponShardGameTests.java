package com.example.immortal_cultivation_mod.item;

import java.util.*;
import com.example.immortal_cultivation_mod.block.*;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class WeaponShardGameTests {
    private static ItemStack weapon(int rank){
        var weapon=new ItemStack(ModItems.DAO.get());
        for(int i=0;i<rank;i++){
            var layers=new int[WeaponRestrictionSystem.RestrictionType.values().length];
            layers[WeaponRestrictionSystem.RestrictionType.SHARPNESS.ordinal()]=36;
            layers[WeaponRestrictionSystem.RestrictionType.STURDY.ordinal()]=36;
            if(!WeaponRestrictionSystem.addRestrictions(weapon,layers)||!WeaponRestrictionSystem.evolve(weapon))throw new AssertionError("Cannot prepare restriction tier");
        }
        return weapon;
    }
    private static List<ItemStack> batch(List<ItemStack> shards){
        var inputs=new ArrayList<>(shards);
        inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(),64));
        inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(),64));
        return inputs;
    }
    @GameTest(template="fate_test")
    public static void spiritWeaponShardDropsExcessLayers(GameTestHelper h){
        var original=weapon(1);int max=original.getMaxDamage();
        int[] extra=new int[WeaponRestrictionSystem.RestrictionType.values().length];
        extra[WeaponRestrictionSystem.RestrictionType.STURDY.ordinal()]=40;
        extra[WeaponRestrictionSystem.RestrictionType.SHARPNESS.ordinal()]=32;
        WeaponRestrictionSystem.addRestrictions(original,extra);
        var shards=WeaponShardSystem.split(original,h.getLevel().registryAccess());
        h.assertTrue(shards.size()==1,"Spirit weapon yields exactly one shard");
        int count=0;for(var type:WeaponRestrictionSystem.RestrictionType.values())count+=WeaponShardSystem.data(shards.get(0)).getCompound("layers").getInt(type.id());
        h.assertTrue(count==72,"Spirit weapon shard retains at most 72 completed layers");
        var restored=WeaponShardSystem.reforge(batch(shards),h.getLevel().registryAccess());
        h.assertTrue(WeaponRestrictionSystem.tierProgress(restored)==0 && WeaponRestrictionSystem.count(restored,WeaponRestrictionSystem.RestrictionType.STURDY)==0 && restored.getMaxDamage()==max,"Unfinished layers and their durability bonus are permanently lost");
        h.assertTrue(WeaponRestrictionSystem.tierProgress(original)==72,"Splitting does not mutate original stack snapshot");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void blankAndMortalWeaponsHaveNoShardStats(GameTestHelper h){
        var blank=new ItemStack(ModItems.WEAPON_SHARD.get());
        h.assertTrue(WeaponShardSystem.data(blank).isEmpty()&&!WeaponRestrictionSystem.hasRestrictions(blank)&&!WeaponShardSystem.populated(blank),"Default shard has no weapon stats");
        h.assertTrue(WeaponShardSystem.split(weapon(0),h.getLevel().registryAccess()).isEmpty(),"Mortal weapons do not create shards");
        for(int tier=1;tier<=7;tier++){
            var shards=WeaponShardSystem.split(weapon(tier),h.getLevel().registryAccess());
            h.assertTrue(shards.size()==tier,"Shard count equals weapon rank");
            int total=0;
            for(var shard:shards){var d=WeaponShardSystem.data(shard);h.assertTrue(d.getInt("layer_capacity")==72,"Each shard stores one 72-layer restriction set");for(var type:WeaponRestrictionSystem.RestrictionType.values())total+=d.getCompound("layers").getInt(type.id());}
            h.assertTrue(total==tier*72,"Completed restriction layers are neither lost nor duplicated");
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void completeSetsRestoreOriginalComponentsAndRejectMixedPieces(GameTestHelper h){
        var original=weapon(2);original.set(DataComponents.CUSTOM_NAME,Component.literal("Lost Star Dao"));
        original.setDamageValue(99);
        int[] partial=new int[WeaponRestrictionSystem.RestrictionType.values().length];partial[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()]=5;
        WeaponRestrictionSystem.addRestrictions(original,partial);
        var shards=WeaponShardSystem.split(original,h.getLevel().registryAccess());
        var encoded=shards.get(0).save(h.getLevel().registryAccess());
        shards.set(0,ItemStack.parse(h.getLevel().registryAccess(),encoded).orElseThrow());
        var inputs=batch(shards);var restored=WeaponShardSystem.reforge(inputs,h.getLevel().registryAccess());
        var expected=original.copy();WeaponRestrictionSystem.retainShardLayers(expected,expected.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound(WeaponRestrictionSystem.TAG_RESTRICTIONS).getCompound("merged"));expected.setDamageValue(0);
        h.assertTrue(WeaponRestrictionSystem.count(restored,WeaponRestrictionSystem.RestrictionType.QI_STORAGE)==0 && WeaponRestrictionSystem.tierProgress(restored)==0,"Excess unfinished restrictions must be lost on reforge");
        h.assertTrue(ItemStack.isSameItemSameComponents(expected,restored),"Reforge repairs and preserves name, attributes, durability, completed restrictions; unfinished layers are lost");
        h.assertTrue(!WeaponShardSystem.canReforge(batch(List.of(shards.get(0),shards.get(0).copy()))),"Repeated fragment cannot complete a set");
        var other=WeaponShardSystem.split(original,h.getLevel().registryAccess());
        h.assertTrue(!WeaponShardSystem.canReforge(batch(List.of(shards.get(0),other.get(1)))),"Separate weapons with identical stats cannot be mixed");
        inputs.get(inputs.size()-1).setCount(63);h.assertTrue(!WeaponShardSystem.canReforge(inputs),"127 spirit stones cannot pay the reforge cost");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualDurabilityBreakDropsOneSetForMobs(GameTestHelper h){
        var mob=h.spawn(EntityType.ZOMBIE,new BlockPos(3,2,3));mob.setNoAi(true);
        var stack=weapon(2);stack.setDamageValue(stack.getMaxDamage()-1);mob.setItemSlot(EquipmentSlot.MAINHAND,stack);
        stack.hurtAndBreak(1,mob,EquipmentSlot.MAINHAND);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(mob.blockPosition()).inflate(2),e->WeaponShardSystem.populated(e.getItem()));
        h.assertTrue(stack.isEmpty()&&drops.size()==2,"Native NPC weapon break creates exactly two shards through shared durability hook");
        drops.forEach(ItemEntity::discard);mob.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void playerAndQiOverflowBreaksProduceExactlyOneSet(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"shard-breaker"));
        var channel=new io.netty.channel.embedded.EmbeddedChannel(player.connection.getConnection());
        var pos=h.absolutePos(new BlockPos(3,2,3));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var normal=weapon(1);normal.setDamageValue(normal.getMaxDamage()-1);player.setItemSlot(EquipmentSlot.MAINHAND,normal);
        normal.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        var area=new AABB(pos).inflate(2);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->WeaponShardSystem.populated(e.getItem()));
        h.assertTrue(normal.isEmpty()&&drops.size()==1,"ServerPlayer overload produces one set, never duplicate hooks");drops.forEach(ItemEntity::discard);
        var overflowing=weapon(2);overflowing.setDamageValue(overflowing.getMaxDamage()-1);
        var root=overflowing.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        root.putDouble("immortal_cultivation_mod.weapon_qi",QiInfusedWeapon.storageLimit(overflowing)+100);
        overflowing.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));player.getInventory().setItem(0,overflowing);
        player.tickCount=0;QiInfusedWeapon.tickOverflow(player);
        drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->WeaponShardSystem.populated(e.getItem()));
        h.assertTrue(overflowing.isEmpty()&&drops.size()==2,"Qi overload break preserves both shards through its direct destruction path");
        drops.forEach(ItemEntity::discard);player.discard();channel.close();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void shardsTransferThroughMoldAndTemperingWithoutDoubleApplying(GameTestHelper h){
        var original=weapon(1);var shard=WeaponShardSystem.split(original,h.getLevel().registryAccess()).get(0);
        var recipe=ForgingSystem.recipeById("dao");var materials=new ArrayList<ItemStack>();
        recipe.required().forEach((item,count)->materials.add(new ItemStack(item,count)));
        materials.add(shard);
        h.assertTrue(ForgingSystem.isIngredientForRecipe(recipe,shard),"Shards are accepted by normal forging recipes");
        var mold=ForgingSystem.createMold(recipe,materials,recipe.averageTemp(),"normal_fire");var result=ForgingSystem.temper(mold);
        h.assertTrue(WeaponRestrictionSystem.tier(result)==1&&WeaponRestrictionSystem.mergedCount(result,WeaponRestrictionSystem.RestrictionType.SHARPNESS)==36&&WeaponRestrictionSystem.mergedCount(result,WeaponRestrictionSystem.RestrictionType.STURDY)==36,"Forged weapon automatically inherits original restriction types");
        double damage=QiInfusedWeapon.baseAttackDamage(result);int durability=result.getMaxDamage();
        WeaponRestrictionSystem.finalizeInheritedLayers(result);
        h.assertTrue(QiInfusedWeapon.baseAttackDamage(result)==damage&&result.getMaxDamage()==durability,"Repeated processing cannot bake bonuses twice");
        h.assertTrue(!WeaponShardSystem.validAdditives(List.of(shard,shard.copy())),"Repeated shard index rejected as additive");
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void furnaceReforgeConsumesEntireSetAndExactly128Stones(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.setBlock(new BlockPos(3,1,3),Blocks.CAMPFIRE);
        h.setBlock(new BlockPos(3,2,3),ModBlocks.CU_TIE_XIAODING.get());
        var ding=(DingBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"shard-forger"));
        var channel=new io.netty.channel.embedded.EmbeddedChannel(player.connection.getConnection());
        // This test advances an entire batch within one world tick. If that tick
        // is a heat-upkeep tick, every simulated tick needs a nearby funded crafter.
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        h.getLevel().addNewPlayer(player);
        ModAttachments.setData(player,ModAttachments.getData(player).withQi(1_000_000));
        var original=weapon(2);var inputs=batch(WeaponShardSystem.split(original,h.getLevel().registryAccess()));
        for(int i=0;i<inputs.size();i++)ding.setItem(DingBlockEntity.MATERIAL_START+i,inputs.get(i));
        ding.startForging(player);h.assertTrue(ding.data(4)==1,"Complete set starts automatic reforging without a recipe slip");
        var saved=ding.saveWithFullMetadata(h.getLevel().registryAccess());
        var loaded=new DingBlockEntity(pos,h.getLevel().getBlockState(pos));loaded.loadWithComponents(saved,h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(loaded);ding=loaded;
        h.assertTrue(ding.data(4)==1,"Active reforge and ingredient snapshots survive a world save");
        for(int i=0;i<ding.data(3)+1&&ding.data(4)==1;i++)DingBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),ding);
        h.assertTrue(ItemStack.isSameItemSameComponents(original,ding.getItem(DingBlockEntity.RESULT_SLOT)),"Furnace restores exact weapon");
        for(int i=0;i<inputs.size();i++)h.assertTrue(ding.getItem(DingBlockEntity.MATERIAL_START+i).isEmpty(),"Every unique shard and both stone stacks consumed");
        player.discard();channel.close();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void interruptedReforgeKeepsAllIngredients(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.setBlock(new BlockPos(3,1,3),Blocks.CAMPFIRE);h.setBlock(new BlockPos(3,2,3),ModBlocks.CU_TIE_XIAODING.get());
        var ding=(DingBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"shard-abort"));
        var channel=new io.netty.channel.embedded.EmbeddedChannel(player.connection.getConnection());
        ModAttachments.setData(player,ModAttachments.getData(player).withQi(1000));
        var inputs=batch(WeaponShardSystem.split(weapon(1),h.getLevel().registryAccess()));
        for(int i=0;i<inputs.size();i++)ding.setItem(DingBlockEntity.MATERIAL_START+i,inputs.get(i).copy());
        ding.startForging(player);DingBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),ding);
        h.setBlock(new BlockPos(3,1,3),Blocks.AIR);DingBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),ding);
        h.assertTrue(ding.data(4)==0&&ding.getItem(DingBlockEntity.RESULT_SLOT).isEmpty(),"Interrupted reforge produces no output");
        for(int i=0;i<inputs.size();i++)h.assertTrue(ItemStack.matches(inputs.get(i),ding.getItem(DingBlockEntity.MATERIAL_START+i)),"Interrupted reforging does not destroy or duplicate fragments or stones");
        player.discard();channel.close();h.succeed();
    }
}
