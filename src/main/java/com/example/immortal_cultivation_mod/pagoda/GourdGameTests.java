package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.item.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class GourdGameTests {
    @GameTest(template="fate_test",timeoutTicks=180)
    public static void eachGourdOwnsItsDimensionAndSpawnsWithoutDescent(GameTestHelper h){
        var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"gourd-worlds"));
        var first=new ItemStack(ModItems.JIN_HULU.get());var second=new ItemStack(ModItems.JIN_HULU.get());
        var firstId=GourdSystem.ensure(owner,first);var secondId=GourdSystem.ensure(owner,second);
        var pos=h.absolutePos(new BlockPos(4,3,4));
        var a=PagodaSystem.summon(owner,first,pos,true,firstId);
        var b=PagodaSystem.summon(owner,second,pos,true,secondId);
        h.assertTrue(a!=null&&b!=null&&a.id.equals(firstId)&&b.id.equals(secondId),"Each gourd item UUID owns its temporary dimension");
        var firstWorld=PagodaWorlds.getOrCreate(owner.server,a);var secondWorld=PagodaWorlds.getOrCreate(owner.server,b);
        h.assertTrue(firstWorld!=secondWorld&&!firstWorld.dimension().equals(secondWorld.dimension()),"Two gourds never share interior state");
        var physical=h.getLevel().getEntity(a.entity);
        h.assertTrue(physical!=null&&Math.abs(physical.getY()-pos.getY()-2)<.001,"Gourd appears directly at final hover height");
        h.assertTrue(PagodaSystem.summon(owner,first.copy(),pos,true,firstId)==null,"Item copies cannot overwrite a live prison");
        a.closing=true;
        h.assertTrue(PagodaSystem.summon(owner,first,pos,true,firstId)==null,"Retiring world remains reserved until survivors are safely released");
        b.closing=true;owner.discard();
        h.startSequence().thenWaitUntil(()->h.assertTrue(physical.isRemoved(),"Gourd disappears immediately when closed"))
            .thenSucceed();
    }
    @GameTest(template="fate_test")
    public static void gourdForgeAndPunchFamily(GameTestHelper h){
        var stack=new ItemStack(ModItems.JIN_HULU.get());var recipe=HigherWeaponRecipes.recipe("jin_hulu");
        h.assertTrue(recipe!=null&&HigherWeaponRecipes.item("jin_hulu")==stack.getItem(),"Forge result is golden gourd");
        h.assertTrue(QiInfusedWeapon.isWeapon(stack)&&QiInfusedWeapon.isFistWeapon(stack)&&!QiInfusedWeapon.isSwordOrDao(stack),"Gourd normal attacks use fist family");
        h.assertTrue(stack.getItem().getUseAnimation(stack)==UseAnim.DRINK,"Right-click always uses drinking");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void waterCannotBeForgedOrDuplicatedThroughDisplayTags(GameTestHelper h){
        var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"gourd-water"));
        var stack=new ItemStack(ModItems.JIN_HULU.get());var id=GourdSystem.ensure(owner,stack);var data=PagodaData.get(owner.server);
        var fake=stack.get(DataComponents.CUSTOM_DATA).copyTag();fake.putInt("immortal_cultivation_mod.gourd_water",100);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(fake));
        h.assertTrue(!GourdSystem.drink(owner,stack),"Client/display water does not create drinks");
        GourdSystem.changeWater(data,id,30);var copy=stack.copy();GourdSystem.ensure(owner,copy);
        h.assertTrue(GourdSystem.drink(owner,copy)&&GourdSystem.drink(owner,stack),"Both copies draw from shared authoritative UUID water");
        h.assertTrue(GourdSystem.water(data,id)==10,"Copying items does not double stored water");
        var second=new ItemStack(ModItems.JIN_HULU.get());var secondId=GourdSystem.ensure(owner,second);
        h.assertTrue(!id.equals(secondId)&&GourdSystem.water(data,secondId)==0,"Different gourds have independent meters");
        GourdSystem.changeWater(data,id,1000);h.assertTrue(GourdSystem.water(data,id)==100,"Water capped at 100");
        GourdSystem.changeWater(data,id,-1000);h.assertTrue(GourdSystem.water(data,id)==0,"Water cannot go negative");owner.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void drinkBuffStacksAndExpiresWithoutPermanentStatChanges(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"gourd-buff"));
        double base=player.getAttributeValue(Attributes.MAX_HEALTH);double baseSpeed=player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for(int i=0;i<5;i++)GourdSystem.addBuff(player);
        h.assertTrue(GourdSystem.stacks(player)==3,"Five drinks still cap at three stacks");
        h.assertTrue(Math.abs(player.getMaxHealth()-base*Math.pow(1.05,3))<.001,"Buff multiplies health by 1.05 per stack");
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-baseSpeed*Math.pow(1.05,3))<.0001,"Movement uses same stack multiplier");
        h.assertTrue(Math.abs(com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(player,100,null)-100*Math.pow(1.05,3))<.001,"Spell damage receives the drink bonus too");
        h.assertTrue(com.example.immortal_cultivation_mod.spell.ThoughtDamageHelper.total(player,100)==116,"Mental damage receives the same multiplier");
        h.assertTrue(player.getPersistentData().getLong("immortal_cultivation_mod.gourd_buff_until")==player.server.overworld().getGameTime()+600,"Drink refreshes exactly 30 seconds");
        player.getPersistentData().putLong("immortal_cultivation_mod.gourd_buff_until",0);GourdSystem.updateBuff(player);
        h.assertTrue(GourdSystem.stacks(player)==0&&Math.abs(player.getMaxHealth()-base)<.001&&Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-baseSpeed)<.0001,"Expiry removes all temporary modifiers");player.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void gourdStateAndWaterSurviveSave(GameTestHelper h){
        var data=new PagodaData();UUID container=UUID.randomUUID();
        var tower=new PagodaData.Tower(UUID.randomUUID(),UUID.randomUUID(),h.getLevel().dimension().location(),new BlockPos(3,65,3),1200);
        tower.gourd=true;tower.container=container;data.towers.put(tower.id,tower);GourdSystem.changeWater(data,container,70);
        var restored=PagodaData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.towers.get(tower.id).gourd&&container.equals(restored.towers.get(tower.id).container)&&GourdSystem.water(restored,container)==70,"Prison kind and linked per-item water persist together");
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"gourd-damage"));
        float before=GourdSystem.damagePerSecond(player);GourdSystem.addBuff(player);
        h.assertTrue(GourdSystem.damagePerSecond(player)==before,"Damage uses base health, unaffected by drinking buff");player.discard();h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=180)
    public static void gourdPrisonDeathRaisesRealWaterAndRetainsReleaseSafety(GameTestHelper h){
        var server=h.getLevel().getServer();var data=PagodaData.get(server);UUID container=UUID.randomUUID();
        var tower=new PagodaData.Tower(UUID.randomUUID(),UUID.randomUUID(),h.getLevel().dimension().location(),h.absolutePos(new BlockPos(4,3,4)),1200);
        tower.gourd=true;tower.container=container;tower.ownerPower=10000;data.towers.put(tower.id,tower);
        var inside=PagodaWorlds.getOrCreate(server,tower);
        h.assertTrue(PagodaWorlds.gourdRadius(71)>PagodaWorlds.gourdRadius(80)&&PagodaWorlds.gourdRadius(86)>PagodaWorlds.gourdRadius(80),"Gourd interior has two bulbs and narrow waist");
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPersistenceRequired();UUID prisoner=zombie.getUUID();
        h.startSequence().thenWaitUntil(()->h.assertTrue(inside.isPositionEntityTicking(new BlockPos(3,65,3))&&inside.areEntitiesLoaded(0L),"Interior loads before transfer"))
            .thenExecute(()->{
                h.assertTrue(PagodaSystem.capture(server,tower,zombie),"Actual entity transfers into gourd");
                var moved=(net.minecraft.world.entity.LivingEntity)inside.getEntity(prisoner);
                h.assertTrue(moved!=null&&Math.abs(GourdSystem.damagePerSecond(moved)-moved.getMaxHealth()*.05)<.001,"Captive damage is five percent base health");
                moved.hurt(moved.damageSources().genericKill(),Float.MAX_VALUE);
                h.assertTrue(GourdSystem.water(data,container)==10&&!tower.prisoners.containsKey(prisoner),"One captive death adds ten water exactly once");
                PagodaWorlds.updateWater(inside,tower,10);
                h.assertTrue(inside.getBlockState(new BlockPos(1,67,1)).is(Blocks.WATER),"Meter raises actual interior water blocks");
                GourdSystem.changeWater(data,container,-10);PagodaWorlds.updateWater(inside,tower,0);
                h.assertTrue(inside.getBlockState(new BlockPos(1,67,1)).isAir(),"Draining meter lowers actual water");
                tower.closing=true;
            }).thenSucceed();
    }
}
