package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class HigherWeaponGameTests {
    private static net.minecraft.world.entity.animal.Cow durableTarget(GameTestHelper helper,BlockPos pos) {
        var target=helper.spawn(EntityType.COW,pos);target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        return target;
    }
    @GameTest(template="fate_test")
    public static void skillScalesForgedDamageAndInjectedQiWithoutChargingInvalidCasts(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.BLACK_IRON_JIAN);
        var stack=player.getMainHandItem();var item=(HigherWeaponItem)stack.getItem();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,stack.get(DataComponents.ATTRIBUTE_MODIFIERS).withModifierAdded(
                Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        HigherWeaponItem.id("skill_test_tempering"),12,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND));
        QiInfusedWeapon.setQi(stack,100);
        player.setXRot(-80);
        HigherWeaponCombat.cast(player,stack,item,false);
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==100&&ModAttachments.getData(player).qi()==1000,
                "A thrust without a valid target consumes neither stored nor personal Qi");
        var target=durableTarget(helper,new BlockPos(4,3,6));player.setXRot(8);
        HigherWeaponCombat.cast(player,stack,item,false);
        helper.assertTrue(Math.abs(target.getHealth()-956.5F)<.01,
                "Thrust deals 150% of forged damage (24) plus injected Qi (5), not fixed 18 damage");
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==90,"Stored Qi pays exactly once for a valid cast");
        HigherWeaponCombat.cast(player,stack,item,false);
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==90,"Cooldown rejection must not consume another Qi charge");
        helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void areaSkillChargesInjectedQiOnlyOnce(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.MOON_CRESCENT_HALBERD);
        var stack=player.getMainHandItem();QiInfusedWeapon.setQi(stack,100);
        var first=durableTarget(helper,new BlockPos(3,3,5));
        var second=durableTarget(helper,new BlockPos(5,3,5));
        HigherWeaponCombat.cast(player,stack,(HigherWeaponItem)stack.getItem(),false);
        float expected=1000-(19+5)*22F/19F;
        helper.assertTrue(Math.abs(first.getHealth()-expected)<.01&&Math.abs(second.getHealth()-expected)<.01,
                "Every enemy receives the same captured weapon percentage and Qi bonus");
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==90,"Two targets must not double the stored Qi payment");
        helper.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=45)
    public static void travellingSkillRetainsInfusionAfterChangingWeapons(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SEVEN_STAR_SWORD);
        var stack=player.getMainHandItem();QiInfusedWeapon.setQi(stack,1000);
        var tag=HigherWeaponItem.data(stack);tag.putInt("Stars",3);HigherWeaponItem.save(stack,tag);
        var target=durableTarget(helper,new BlockPos(4,3,7));player.setXRot(8);
        HigherWeaponCombat.cast(player,stack,(HigherWeaponItem)stack.getItem(),false);
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==900,"Travelling attack reserves its Qi charge at launch");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_SWORD));
        helper.runAfterDelay(15,()->{
            helper.assertTrue(Math.abs(target.getHealth()-880)<.01,
                    "Delayed hit retains 150% of the original 30 weapon damage plus 50 damage from 1000 injected Qi");
            helper.assertTrue(QiInfusedWeapon.getQi(stack)==900,"Delayed impact does not consume Qi a second time");
            helper.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void capturedDamageAndWeaponEffectsCannotChangeMidCast(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.BLACK_IRON_JIAN);
        var stack=player.getMainHandItem();QiInfusedWeapon.setQi(stack,100);
        var attack=WeaponSkillAttack.capture(stack);
        attack.consumeInjectedQi();attack.consumeInjectedQi();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        QiInfusedWeapon.setQi(stack,1000);
        helper.assertTrue(Math.abs(attack.damage(150)-25.5F)<.001,"An in-flight cast keeps original weapon power");
        var source=player.damageSources().playerAttack(player);
        var other=player(helper,HigherWeaponSpec.SOUL_HOOK);
        attack.withWeapon(player,source,()->{
            helper.assertTrue(QiInfusedWeapon.baseAttackDamage(WeaponSkillAttack.sourceWeapon(player,source))==12,
                    "Hit effects use the cast weapon snapshot rather than a replacement item");
            helper.assertTrue(WeaponSkillAttack.sourceWeapon(other,source)==other.getMainHandItem(),
                    "Nested damage belonging to another attacker cannot inherit the cast weapon");
            helper.assertTrue(WeaponSkillAttack.sourceWeapon(player,player.damageSources().playerAttack(player))==stack,
                    "A separate damage source from a callback cannot inherit the cast weapon either");
            return true;
        });
        helper.assertTrue(WeaponSkillAttack.sourceWeapon(player,source)==stack,"Scoped weapon context restores after damage");
        helper.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=40)
    public static void npcFuchenSpinAcceptsLivingOwnerAndPaysOnce(GameTestHelper helper) {
        var npc=helper.spawn(EntityType.ZOMBIE,new BlockPos(4,3,3));npc.setNoAi(true);npc.setNoGravity(true);
        var stack=new ItemStack(ModItems.FUCHEN.get());
        QiInfusedWeapon.setQi(stack,70);
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,stack);
        helper.assertTrue(FuchenItem.startHeavyAttack(npc),"Non-player Fuchen owner can start its spin");
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==63,"NPC spin reserves injected Qi before any hit callbacks");
        FuchenItem.tick(npc);
        helper.runAfterDelay(22,()->{
            helper.assertTrue(QiInfusedWeapon.getQi(stack)==63,"Later spin pulses do not consume the stored Qi again");
            FuchenItem.clear(npc);
            helper.succeed();
        });
    }
    @GameTest(template="fate_test",timeoutTicks=50)
    public static void silkOwnPulsePreservesBindAndExpiryClearsEndpoint(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SPIRIT_SILK_WHIP);
        var stack=player.getMainHandItem();QiInfusedWeapon.setQi(stack,110);
        var target=durableTarget(helper,new BlockPos(4,3,6));player.setXRot(8);
        HigherWeaponCombat.cast(player,stack,(HigherWeaponItem)stack.getItem(),false);
        helper.runAfterDelay(22,()->{
            helper.assertTrue(Math.abs(target.getHealth()-996.7F)<.02,
                    "Silk binding pulse uses 20% of its captured weapon and injected Qi");
            helper.assertTrue(target.getAttributeValue(Attributes.MOVEMENT_SPEED)==0,
                    "Binding's own pulse cannot release its own control");
            helper.assertTrue(QiInfusedWeapon.getQi(stack)==99,"Binding damage must not spend a second Qi charge");
        });
        helper.runAfterDelay(35,()->{
            helper.assertTrue(!HigherWeaponItem.data(stack).contains("SilkBindingTarget")
                            &&target.getAttributeValue(Attributes.MOVEMENT_SPEED)>0,
                    "Expiry removes both visual endpoint data and movement control");
            helper.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void forgeCapacityAndLegacyOverflow(GameTestHelper helper) {
        var small = new com.example.immortal_cultivation_mod.block.DingBlockEntity(BlockPos.ZERO,
                com.example.immortal_cultivation_mod.block.ModBlocks.CU_TIE_XIAODING.get().defaultBlockState());
        var bronze = new com.example.immortal_cultivation_mod.block.DingBlockEntity(BlockPos.ZERO,
                com.example.immortal_cultivation_mod.block.ModBlocks.QINGTONG_YINHUO_DING.get().defaultBlockState());
        helper.assertTrue(small.materialSlotCount() == 10 && bronze.materialSlotCount() == 15,
                "Crude iron has ten slots; bronze has fifteen");
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof ForgingYujianItem)) continue;
            var recipe = ForgingSystem.recipe(new ItemStack(item));
            helper.assertTrue(recipe != null && recipe.ingredientSlotCount() <= bronze.materialSlotCount(),
                    "Every forge slip remains usable after the capacity change: " + item);
        }
        var player = player(helper, HigherWeaponSpec.BLACK_IRON_JIAN);
        int legacySlot = com.example.immortal_cultivation_mod.block.DingBlockEntity.MATERIAL_START + 20;
        bronze.setItem(legacySlot, new ItemStack(ModItems.ZHUJI_YAODAN.get()));
        var menu = new com.example.immortal_cultivation_mod.screen.DingMenu(0, player.getInventory(), bronze);
        helper.assertTrue(menu.getSlot(legacySlot).isActive()
                        && !menu.getSlot(legacySlot).mayPlace(new ItemStack(ModItems.ZHUJI_YAODAN.get())),
                "Legacy overflow is visible for extraction, never for new input");
        helper.assertTrue(!menu.quickMoveStack(player, legacySlot).isEmpty()
                        && bronze.getItem(legacySlot).isEmpty()
                        && !menu.getSlot(legacySlot).isActive(),
                "Old stored materials can be recovered without adding usable forge slots");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper,HigherWeaponSpec spec) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"weapon-test"));
        player.setGameMode(GameType.SURVIVAL);
        var pos=helper.absolutePos(new BlockPos(4,3,3));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setYRot(0);player.setXRot(0);
        ModAttachments.setData(player,ModAttachments.getData(player)
                .withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY).withQi(1000));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HigherWeaponRecipes.item(spec.id)));
        return player;
    }
    @GameTest(template="fate_test")
    public static void everyAdvancedSlipForgesAndTempersTheCorrectWeapon(GameTestHelper helper) {
        for(var spec:HigherWeaponSpec.values()) {
            var item=HigherWeaponRecipes.item(spec.id);
            helper.assertTrue(item instanceof HigherWeaponItem,"Registered weapon: "+spec.id);
            var stack=new ItemStack(item);
            helper.assertTrue(stack.getMaxDamage()==spec.durability,"Tier durability must survive SwordItem defaults: "+spec.id);
            var recipe=ForgingSystem.recipe(new ItemStack(HigherWeaponRecipes.item(spec.id+"_yujian")));
            helper.assertTrue(recipe!=null&&recipe.id().equals(spec.id),"Every slip resolves its own recipe");
            int minimumSlots = spec.realm == 3 ? 15 : 10;
            helper.assertTrue(recipe.ingredientSlotCount() >= minimumSlots
                    && recipe.ingredientSlotCount() <= 15
                    && recipe.averageTemp() > 3500 && recipe.requiredFireType().isEmpty(),
                    "Recipe fits its realm budget and the bronze forge: " + spec.id);
            var realmCore = spec.realm == 3 ? ModItems.JINDAN_YAODAN.get() : ModItems.ZHUJI_YAODAN.get();
            helper.assertTrue(recipe.required().containsKey(realmCore), "Weapon uses its own realm's core");
            if (spec.realm == 2) {
                helper.assertTrue(!recipe.required().containsKey(ModItems.JINDAN_YAODAN.get())
                        && !recipe.required().containsKey(ModItems.FLAME_RADIANCE_BONE.get())
                        && !recipe.required().containsKey(ModItems.IRON_SCALE_PLATE.get()),
                        "Foundation weapons must not use mismatched realm beast parts");
            }
            var materials=new ArrayList<ItemStack>();
            for(var ingredient:recipe.required().entrySet()) {
                helper.assertTrue(ingredient.getKey()!=Items.AIR,"No missing recipe registry ID: "+spec.id);
                materials.add(new ItemStack(ingredient.getKey(),ingredient.getValue()));
            }
            var mold=ForgingSystem.createMold(recipe,materials,recipe.averageTemp(),"zhen_fire");
            var tempered=ForgingSystem.temper(mold);
            helper.assertTrue(tempered.is(item)&&tempered.getMaxDamage()>=spec.durability,"Tempering preserves weapon type and base durability");
            helper.assertTrue(ForgingSystem.isForgedWeapon(tempered),"Forged material passives remain enabled");
        }
        helper.succeed();
    }
    @GameTest(template="fate_test")
    public static void legacyFanRestoresQiAndPreservesCustomModifiers(GameTestHelper helper) {
        var stack=new ItemStack(ModItems.WIND_FEATHER_FAN.get());
        var custom=HigherWeaponItem.id("test_forged_speed");
        var builder=net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        builder.add(Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID,7,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.MOVEMENT_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                custom,.1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,builder.build());
        QiInfusedWeapon.setQi(stack,800);
        var tag=HigherWeaponItem.data(stack);tag.putBoolean("WindFanOpen",false);tag.putDouble("WindFanReservedQi",200);tag.putInt("StoredQi",400);HigherWeaponItem.save(stack,tag);
        WindFanQi.migrateLegacyStack(stack);
        WindFanQi.migrateLegacyStack(stack);
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==1000,"Migration restores all reserved Qi exactly once");
        helper.assertTrue(QiInfusedWeapon.baseAttackDamage(stack)==11,"Legacy closed fan gets permanent usable stats");
        helper.assertTrue(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.modifier().id().equals(custom)),"Migration retains forged attributes");
        helper.assertTrue(HigherWeaponItem.data(stack).getInt("StoredQi")==400,"Migration retains unrelated saved data");
        helper.assertTrue(!HigherWeaponItem.data(stack).contains("WindFanOpen")&&!HigherWeaponItem.data(stack).contains("WindFanReservedQi"),"Obsolete folding state removed");
        helper.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=50)
    public static void realMeleeHitsChargeSevenStarAndRightClickReleasesIt(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SEVEN_STAR_SWORD);
        var stack=player.getMainHandItem();var item=(HigherWeaponItem)stack.getItem();
        var target=durableTarget(helper,new BlockPos(4,3,6));
        // Include the cultivation pre-hit and injected-Qi damage paths, not manually seeded star NBT.
        ModAttachments.setData(player,ModAttachments.getData(player).withAddedPhysicalAttack(5));
        QiInfusedWeapon.setQi(stack,270);
        var fragile=helper.spawn(EntityType.COW,new BlockPos(3,3,5));fragile.setHealth(4);
        player.attack(fragile);
        helper.assertTrue(SevenStarPattern.charges(stack)==1,"Cultivation bonus killing before vanilla melee still grants one star");
        for(int hit=2;hit<=8;hit++) {
            target.invulnerableTime=0;
            player.attack(target);
            helper.assertTrue(SevenStarPattern.charges(stack)==Math.min(hit,7),"Each real melee attack gains exactly one star, capped at seven: "+hit);
        }
        target.invulnerableTime=0;
        float health=target.getHealth();
        player.setXRot(8);
        item.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(SevenStarPattern.charges(stack)==4,"Right-click spends three genuinely earned stars");
        helper.assertTrue(ModAttachments.getData(player).qi()==920,"Normal skill spends 80 player Qi");
        helper.runAfterDelay(18,()->{
            helper.assertTrue(target.getHealth()<health,"Skill launched through item use actually damages its target");
            helper.assertTrue(SevenStarPattern.charges(stack)==4,"Skill damage cannot recharge itself");
            helper.succeed();
        });
    }
    @GameTest(template="fate_test")
    public static void skillCostsCooldownAndRealmAreAuthoritative(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SOUL_HOOK);
        var stack=player.getMainHandItem();var item=(HigherWeaponItem)stack.getItem();
        ModAttachments.setData(player,ModAttachments.getData(player).withCultivationLevel(CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY));
        helper.assertTrue(!item.spend(player,stack,35,100)&&ModAttachments.getData(player).qi()==1000,"Wrong realm cannot spend or cast");
        ModAttachments.setData(player,ModAttachments.getData(player).withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY).withQi(34));
        helper.assertTrue(!item.spend(player,stack,35,100)&&stack.getDamageValue()==0,"Insufficient Qi does not consume durability");
        ModAttachments.setData(player,ModAttachments.getData(player).withQi(100));
        helper.assertTrue(item.spend(player,stack,35,100),"Valid cast succeeds");
        helper.assertTrue(ModAttachments.getData(player).qi()==65&&stack.getDamageValue()==1,"Cast pays the exact Qi and durability");
        helper.assertTrue(!item.spend(player,stack,35,100)&&ModAttachments.getData(player).qi()==65,"Cooldown prevents duplicate payment");
        helper.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=35)
    public static void whipBindsNormalTargetsAndDamageReleasesThem(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SPIRIT_SILK_WHIP);
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,6));cow.setNoAi(true);cow.setNoGravity(true);
        player.setXRot(8);
        double before=cow.getAttributeValue(Attributes.MOVEMENT_SPEED);
        HigherWeaponCombat.cast(player,player.getMainHandItem(),(HigherWeaponItem)player.getMainHandItem().getItem(),false);
        helper.assertTrue(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)==0,"Whip actually immobilizes a normal target");
        helper.assertTrue(HigherWeaponItem.data(player.getMainHandItem()).getInt("SilkBindingTarget")==cow.getId(),
                "Silk whip keeps a synchronized binding target for its physical tether");
        cow.hurt(player.damageSources().generic(),1);
        helper.assertTrue(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)==before,"Damage immediately removes the bind modifier");
        helper.assertTrue(!HigherWeaponItem.data(player.getMainHandItem()).contains("SilkBindingTarget"),
                "Breaking the bind also releases its visual tether");
        helper.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=60)
    public static void windBladeStopsAtWalls(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.WIND_FEATHER_FAN);
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,3,8));cow.setNoAi(true);cow.setNoGravity(true);
        for(int x=3;x<=5;x++)for(int y=3;y<=6;y++)helper.setBlock(new BlockPos(x,y,5),Blocks.STONE);
        float before=cow.getHealth();
        HigherWeaponCombat.cast(player,player.getMainHandItem(),(HigherWeaponItem)player.getMainHandItem().getItem(),false);
        helper.runAfterDelay(15,()->{
            helper.assertTrue(cow.getHealth()==before,"Wind cannot damage through a wall");
            helper.assertTrue(helper.getBlockState(new BlockPos(4,4,5)).is(Blocks.STONE),"Wind does not destroy terrain");
            helper.succeed();
        });
    }
    @GameTest(template="fate_test",timeoutTicks=90)
    public static void sevenStarsRequireEnergyAndFinishTheirDelayedAttack(GameTestHelper helper) {
        var player=player(helper,HigherWeaponSpec.SEVEN_STAR_SWORD);
        var stack=player.getMainHandItem();var item=(HigherWeaponItem)stack.getItem();
        var target=helper.spawn(EntityType.COW,new BlockPos(4,3,6));target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        helper.assertTrue(!HigherWeaponCombat.boss(target),"High health alone must not give ordinary mobs boss immunity");
        player.setXRot(8);player.setShiftKeyDown(true);
        HigherWeaponCombat.cast(player,stack,item,false);
        helper.assertTrue(ModAttachments.getData(player).qi()==1000,"Ultimate without seven stars spends nothing");
        QiInfusedWeapon.setQi(stack,300);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var tag=HigherWeaponItem.data(stack);tag.putInt("Stars",7);HigherWeaponItem.save(stack,tag);
        HigherWeaponCombat.cast(player,stack,item,false);
        helper.assertTrue(QiInfusedWeapon.getQi(stack)==270,"Ultimate spends stored Qi once before its delayed hits");
        helper.assertTrue(HigherWeaponItem.data(stack).getInt("Stars")==0&&ModAttachments.getData(player).qi()==750,"Ultimate consumes seven stars and 250 Qi exactly once");
        helper.assertTrue(target.getHealth()==1000,"Descending swords give a visible lead-in before the first impact");
        helper.runAfterDelay(60,()->{
            helper.assertTrue(Math.abs(target.getHealth()-686.5F)<.02,"Seven delayed impacts and final strike retain the captured damage with 300 Qi giving +15 damage");
            helper.assertTrue(QiInfusedWeapon.getQi(stack)==270,"Seven impacts plus finisher still pay injected Qi only once");
            helper.assertTrue(player.getCooldowns().isOnCooldown(item),"Ultimate retains its twenty-second cooldown");
            helper.succeed();
        });
    }
}
