package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.*;

@GameTestHolder("immortal_cultivation_alchemy")
@PrefixGameTestTemplate(false)
public final class AlchemyGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"alchemy-test"));
        var pos=h.absolutePos(new BlockPos(1,1,1));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY).withQi(500).withThoughts(0).withCultivationProgress(0));
        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(p);
        return p;
    }
    private static ItemStack batch(String id) {var f=formula(id);return PillData.create(brew(f,f.required()));}
    @GameTest(template="fate_test") public static void extraHerbsCannotIntroduceNewPillAbilities(GameTestHelper h) {
        var healing=formula("huichun");var herbs=new HashMap<>(healing.required());
        herbs.merge("ninglu_zhi",1,Integer::sum);
        var boosted=brew(healing,herbs);
        h.assertTrue(!boosted.failed() && boosted.effects().get(Effect.HEALTH).amount()>healing.effects().get(Effect.HEALTH).amount(),
                "Mixed healing/Qi herb strengthens healing");
        h.assertTrue(!boosted.effects().containsKey(Effect.QI) && !boosted.effects().containsKey(Effect.DEFENSE),
                "Unrelated medicinal effects are ignored rather than introduced");
        herbs=new HashMap<>(healing.required());herbs.put("lingquan_leaf",1);
        var failed=brew(healing,herbs);
        h.assertTrue(failed.failed() && PillData.create(failed).is(com.example.immortal_cultivation_mod.item.ModItems.ASHES.get()),
                "Qi-only herb in healing pill destroys the whole batch");
        herbs=new HashMap<>(healing.required());herbs.put("ningling_flower",1);
        h.assertTrue(brew(healing,herbs).failed(),"Qi multiplier cannot modify a pill without Qi recovery");
        herbs=new HashMap<>(healing.required());herbs.put("catalyst_blossom",1);
        var longer=brew(healing,herbs);
        h.assertTrue(!longer.failed() && longer.effects().get(Effect.HEALTH).ticks()>120 && !longer.effects().containsKey(Effect.DURATION),
                "Duration herb modifies existing healing instead of creating a separate ability");
        var qi=formula("huiling");herbs=new HashMap<>(qi.required());herbs.merge("qingling_grass",1,Integer::sum);
        h.assertTrue(!brew(qi,herbs).failed() && !brew(qi,herbs).effects().containsKey(Effect.CULTIVATION),
                "Mixed Qi/cultivation herb cannot introduce cultivation to Qi-only pill");
        var recipe=com.example.immortal_cultivation_mod.item.ForgingSystem.recipe(new ItemStack(AlchemyContent.FORMULAS.get("huichun").get()));
        var inputs=new ArrayList<ItemStack>();recipe.required().forEach((item,count)->{for(int i=0;i<count;i++)inputs.add(new ItemStack(item));});
        inputs.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","lingquan_leaf"))));
        inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),64));
        var output=com.example.immortal_cultivation_mod.item.ForgingSystem.createMold(recipe,inputs,1200,"normal_fire");
        h.assertTrue(output.is(com.example.immortal_cultivation_mod.item.ModItems.ASHES.get()) && output.getCount()==5,
                "Real forge path returns full ash yield for incompatible additives");
        h.succeed();
    }
    @GameTest(template="fate_test") public static void percentageRecoveryUsesActualMaximumsAndCarriesFractions(GameTestHelper h) {
        var p=player(h);p.setHealth(4);float maximum=p.getMaxHealth();
        var custom=PillData.create(new Batch("huichun",1,Map.of(),Map.of(Effect.HEALTH,new Dose(1,120)),100,0,"superior",false));
        AlchemyEffects.consume(p,custom);for(int i=0;i<120;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(Math.abs(p.getHealth()-(4+maximum*.01))<.001,"One percent restores a fraction of actual cultivation HP: "+p.getHealth()+", maximum="+maximum);
        p.getPersistentData().remove("NextCultivationPill");ModAttachments.setData(p,ModAttachments.getData(p).withQi(0).withThoughts(0));
        custom=PillData.create(new Batch("qingling",1,Map.of(),Map.of(Effect.QI,new Dose(1,200),Effect.SENSE,new Dose(1,200)),100,0,"superior",false));
        AlchemyEffects.consume(p,custom);for(int i=0;i<200;i++)AlchemyEffects.tickPlayer(p);
        int maxQi=CultivationLevels.getLevelDef(ModAttachments.getData(p).cultivationLevel()).maxQi()+ModAttachments.getData(p).maxQiBonus();
        h.assertTrue(ModAttachments.getData(p).qi()==maxQi/100 && ModAttachments.getData(p).thoughts()==ModAttachments.getData(p).maxThoughts()/100,
                "Small percentage recovery accumulates across ticks instead of losing each fraction");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void savedFixedDosesMigrateToPercentageTooltips(GameTestHelper h) {
        var stack=new ItemStack(AlchemyContent.pill("huichun"));
        var dose=new CompoundTag();dose.putDouble("Amount",12);dose.putInt("Ticks",120);
        var effects=new CompoundTag();effects.put("HEALTH",dose);
        var data=new CompoundTag();data.put("Effects",effects);var root=new CompoundTag();root.put(PillData.KEY,data);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
        h.assertTrue(PillData.effects(stack).get(Effect.HEALTH).amount()==6,"Old twelve-HP batch migrates through percentage conversion to current six-percent recovery");
        h.assertTrue(PillData.effectLine(Effect.HEALTH,PillData.effects(stack).get(Effect.HEALTH)).getString().contains("6%"),"Tooltip uses actual percentage unit");
        h.assertTrue(formula("mingmu").effects().get(Effect.NIGHT_VISION).amount()==100,"Existing guaranteed utility effect stays a hundred-percent activation");
        h.succeed();
    }
    @GameTest(template="fate_test") public static void savedPillBalancePreservesBonusesAndSurvivesReload(GameTestHelper h) {
        var stack=batch("huiling");
        var root=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        var data=root.getCompound(PillData.KEY);data.putInt("DoseVersion",2);
        var dose=data.getCompound("Effects").getCompound("QI");
        dose.putDouble("Amount",62.5);dose.putInt("Ticks",300);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
        var saved=stack.save(h.getLevel().registryAccess());
        var restored=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)saved);
        for(int i=0;i<3;i++) {
            var current=PillData.effects(restored).get(Effect.QI);
            h.assertTrue(current.amount()==6.25&&current.ticks()==300,"Saved potency bonus and extended duration survive balance migration");
            h.assertTrue(restored.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().equals(root),"Repeated tooltip reads never compound migration or mutate saved metadata");
        }
        var p=player(h);ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
        AlchemyEffects.consume(p,restored);
        for(int i=0;i<300;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(ModAttachments.getData(p).qi()==(int)Math.floor((CultivationLevels.getLevelDef(ModAttachments.getData(p).cultivationLevel()).maxQi()+ModAttachments.getData(p).maxQiBonus())*.0625),"Saved pill consumption uses rebalanced recovery");
        h.assertTrue(PillData.effects(batch("huiling")).get(Effect.QI).amount()==5,"New batches are not migrated again");
        h.assertTrue(PillData.effects(batch("pozhang")).get(Effect.NEXT_MELEE).amount()==25,"Unchanged pills retain their power");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void baseRecipesRetainExactEffects(GameTestHelper h) {
        for(var f:FORMULAS){
            var b=brew(f,f.required());
            h.assertTrue(!b.failed(),f.id()+" base recipe must succeed");
            h.assertTrue(b.effects().equals(f.effects()),f.id()+" exact base effects: "+b.effects());
            h.assertTrue(b.count()==(f.id().equals("juling")?6:5),"Batch yield");
        }
        var herbs=new HashMap<>(formula("huiling").required());herbs.put("ningling_flower",1);
        h.assertTrue(brew(formula("huiling"),herbs).effects().get(Effect.QI).amount()==6.25,"25 percent multiplier strengthens 5 percent Qi recovery to 6.25 percent");
        h.assertTrue(!matches(formula("qingling"),Map.of("qingling_grass",2)),"Missing ingredients rejected");
        h.assertTrue(!matches(formula("qingling"),Map.of("qingling_grass",64,"ninglu_zhi",1)),"Over-limit inputs rejected");
        h.succeed();
    }
    @GameTest(template="fate_test") public static void conflictsDestroyEntireBatchAndHarmonyRescuesIt(GameTestHelper h) {
        var original=formula("chiyan");
        var f=new Formula("chemistry_test",original.required(),Map.of(Effect.FIRE_DAMAGE,new Dose(15,1200),Effect.COLD_RESIST,new Dose(20,1200),
                Effect.FIRE_RESIST,new Dose(25,1200),Effect.CLEAR_MIND,new Dose(100,0),Effect.QI,new Dose(10,200),Effect.HEALTH,new Dose(20,200)),5,5,0);
        var herbs=new HashMap<>(f.required());
        herbs.put("chiyan_flower",8);herbs.put("xuanbing_lotus",8);
        var failed=brew(f,herbs);
        h.assertTrue(failed.failed(),"Strong water/fire conflict fails");
        var ash=PillData.create(failed);h.assertTrue(ash.is(com.example.immortal_cultivation_mod.item.ModItems.ASHES.get())&&ash.getCount()==5,"Whole batch becomes five ash");
        herbs.put("harmonizing_herb",3);
        var stable=brew(f,herbs);h.assertTrue(!stable.failed()&&stable.conflict()<failed.conflict(),"Harmony rescues conflict");
        var mild=new HashMap<>(f.required());mild.put("ninglu_zhi",1);
        h.assertTrue(!brew(f,mild).failed(),"Small opposing addition does not automatically fail");h.succeed();
    }
    @GameTest(template="fate_test") public static void batchDataStacksAndSurvivesSaving(GameTestHelper h) {
        var a=batch("qingling");var b=batch("qingling");
        h.assertTrue(ItemStack.isSameItemSameComponents(a,b),"Identical batches stack");
        var reg=h.getLevel().registryAccess();var restored=ItemStack.parseOptional(reg,(CompoundTag)a.save(reg));
        h.assertTrue(ItemStack.isSameItemSameComponents(a,restored)&&restored.getCount()==5,"Pill data survives save/load");
        var herbs=new HashMap<>(formula("qingling").required());herbs.put("ningling_flower",1);
        var improved=PillData.create(brew(formula("qingling"),herbs));
        h.assertTrue(a.getItem()==improved.getItem()&&!ItemStack.isSameItemSameComponents(a,improved),"Modified stats use same item but separate stack");
        for(String id:List.of("low_qi_recovery_pill","middle_qi_recovery_pill","high_qi_recovery_pill"))
            h.assertTrue(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",id))==AlchemyContent.pill("huiling"),"Legacy item alias");
        h.succeed();
    }
    @GameTest(template="fate_test") public static void bottleCapacityMatchingAndSaveLoad(GameTestHelper h) {
        var p=player(h);var bottle=new ItemStack(AlchemyContent.BOTTLE.get());p.getInventory().setItem(0,bottle);
        var m=new JadeBottleMenu(1,p.getInventory(),0);var pills=batch("qingling");
        p.getInventory().setItem(9,pills.copyWithCount(16));p.getInventory().setItem(10,pills.copyWithCount(16));
        var remainder=pills.copyWithCount(8);p.getInventory().setItem(11,remainder);
        m.quickMoveStack(p,1);
        m.quickMoveStack(p,2);m.quickMoveStack(p,3);
        h.assertTrue(JadeBottleItem.contents(bottle).getCount()==32&&remainder.getCount()==8,"Legal 16-pill stacks fill 32 capacity without losing overflow");
        h.assertTrue(!m.getSlot(0).mayPlace(batch("huichun")),"Different pill rejected");
        var herbs=new HashMap<>(formula("qingling").required());herbs.put("ningling_flower",1);
        h.assertTrue(!m.getSlot(0).mayPlace(PillData.create(brew(formula("qingling"),herbs))),"Different stats rejected");
        m.clicked(0,0,ClickType.SWAP,p);h.assertTrue(p.getInventory().getItem(0)==bottle,"Held bottle cannot swap into itself");
        var reg=h.getLevel().registryAccess();var saved=ItemStack.parseOptional(reg,(CompoundTag)bottle.save(reg));
        h.assertTrue(JadeBottleItem.contents(saved).getCount()==32,"Bottle contents persist");
        p.getInventory().setItem(0,saved);m=new JadeBottleMenu(2,p.getInventory(),0);
        h.assertTrue(JadeBottleItem.contents(saved).getCount()==32&&m.getSlot(0).getItem().getCount()==32,"Opening saved full bottle preserves all 32 pills");
        m.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(JadeBottleItem.contents(saved).isEmpty(),"Extracting saved bottle empties its contents");
        h.assertTrue(p.getInventory().items.stream().filter(s->s.is(AlchemyContent.pill("qingling"))).mapToInt(ItemStack::getCount).sum()==40,"No pill duplication or loss");
        h.assertTrue(p.getInventory().items.stream().allMatch(s->s.isEmpty()||s.getCount()<=s.getMaxStackSize()),"Shift extraction creates only legal inventory stacks");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void bottlePickupAndHotbarRespectLoosePillStackLimit(GameTestHelper h) {
        var p=player(h);var bottle=new ItemStack(AlchemyContent.BOTTLE.get());
        JadeBottleItem.store(bottle,batch("qingling").copyWithCount(32));p.getInventory().setItem(0,bottle);
        var m=new JadeBottleMenu(1,p.getInventory(),0);
        m.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().getCount()==16&&JadeBottleItem.contents(bottle).getCount()==16,"Normal pickup takes 16 and leaves 16 bottled");
        m.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().isEmpty()&&JadeBottleItem.contents(bottle).getCount()==32,"Returning cursor pills restores full bottle without loss");
        m.clicked(0,1,ClickType.SWAP,p);
        h.assertTrue(p.getInventory().getItem(1).getCount()==16&&JadeBottleItem.contents(bottle).getCount()==16,"Empty hotbar receives 16 and leaves 16 bottled");
        m.quickMoveStack(p,29);
        h.assertTrue(JadeBottleItem.contents(bottle).getCount()==32&&p.getInventory().getItem(1).isEmpty(),"Hotbar pills can refill bottle");
        p.getInventory().setItem(1,batch("qingling").copyWithCount(16));
        m.clicked(0,1,ClickType.SWAP,p);
        h.assertTrue(JadeBottleItem.contents(bottle).getCount()==32&&p.getInventory().getItem(1).getCount()==16,"Full bottle cannot swap an oversized stack into occupied hotbar");
        m.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getInventory().items.stream().filter(s->s.is(AlchemyContent.pill("qingling"))).mapToInt(ItemStack::getCount).sum()==48,"Blocked swap and later extraction conserve every pill");
        h.assertTrue(p.getInventory().items.stream().allMatch(s->s.isEmpty()||s.getCount()<=s.getMaxStackSize()),"All extracted pills remain within normal stack limit");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void bottleThrowRespectsLoosePillStackLimit(GameTestHelper h) {
        var p=player(h);var bottle=new ItemStack(AlchemyContent.BOTTLE.get());
        JadeBottleItem.store(bottle,batch("qingling").copyWithCount(32));p.getInventory().setItem(0,bottle);
        var m=new JadeBottleMenu(1,p.getInventory(),0);m.clicked(0,1,ClickType.THROW,p);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4),e->e.getItem().is(AlchemyContent.pill("qingling")));
        h.assertTrue(JadeBottleItem.contents(bottle).getCount()==16&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==16,"Throwing full bottle drops 16 and retains 16");
        h.assertTrue(drops.stream().allMatch(e->e.getItem().getCount()<=e.getItem().getMaxStackSize()),"World drops obey pill stack limit");
        drops.forEach(ItemEntity::discard);p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void bottleUseConsumesOnlyOneAndRetainsBottle(GameTestHelper h) {
        var p=player(h);var bottle=new ItemStack(AlchemyContent.BOTTLE.get());var pills=batch("qingling");pills.setCount(24);
        JadeBottleItem.store(bottle,pills);p.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        bottle.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(bottle.getCount()==1&&JadeBottleItem.contents(bottle).getCount()==23,"24 becomes 23 without consuming bottle");
        bottle.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(JadeBottleItem.contents(bottle).getCount()==23,"Cooldown prevents duplicate use");
        p.getPersistentData().remove("NextCultivationPill");pills.setCount(1);JadeBottleItem.store(bottle,pills);
        bottle.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(JadeBottleItem.contents(bottle).isEmpty()&&bottle.getCount()==1,"Last pill leaves empty bottle");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void timedRecoveryAndBuffExpiry(GameTestHelper h) {
        var p=player(h);ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
        h.assertTrue(AlchemyEffects.consume(p,batch("qingling")),"Consume Qingling");
        long need=CultivationLevels.getTotalQiNeeded(ModAttachments.getData(p).cultivationLevel());
        int maxQi=CultivationLevels.getLevelDef(ModAttachments.getData(p).cultivationLevel()).maxQi()+ModAttachments.getData(p).maxQiBonus();
        h.assertTrue(ModAttachments.getData(p).cultivationProgress()==(long)(need*.001),"Instant cultivation is 0.1 percent of stage requirement");
        for(int i=0;i<100;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(ModAttachments.getData(p).qi()==(int)Math.floor(maxQi*.015),"1.5 percent of max Qi at halfway");
        for(int i=0;i<100;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(ModAttachments.getData(p).qi()==(int)Math.floor(maxQi*.03),"Three percent of max Qi over ten seconds");
        p.getPersistentData().remove("NextCultivationPill");AlchemyEffects.consume(p,batch("yangshen"));
        for(int i=0;i<200;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(ModAttachments.getData(p).thoughts()==(int)(ModAttachments.getData(p).maxThoughts()*.05),"Five percent of mental capacity restored");
        p.getPersistentData().remove("NextCultivationPill");p.setHealth(4);float maxHp=p.getMaxHealth();AlchemyEffects.consume(p,batch("huichun"));
        for(int i=0;i<120;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(Math.abs(p.getHealth()-Math.min(p.getMaxHealth(),4+maxHp*.06))<.002,"Six percent of maximum HP over six seconds: "+p.getHealth());
        p.getPersistentData().remove("NextCultivationPill");AlchemyEffects.consume(p,batch("juling"));
        h.assertTrue(AlchemyEffects.cultivationGain(p,100)==110,"Ten-percent cultivation bonus active");
        for(int i=0;i<2400;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(AlchemyEffects.cultivationGain(p,100)==100,"Cultivation bonus expires");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void formulasUseForgeSlipsAndKeepPillChemistry(GameTestHelper h) {
        for(var formula:FORMULAS) {
            var slip=new ItemStack(AlchemyContent.FORMULAS.get(formula.id()).get());
            h.assertTrue(slip.getItem() instanceof com.example.immortal_cultivation_mod.item.ForgingYujianItem,"Formula is a real forge slip");
            var recipe=com.example.immortal_cultivation_mod.item.ForgingSystem.recipe(slip);
            h.assertTrue(recipe!=null&&recipe.directOutput()&&recipe.ingredientSlotCount()<=10,"Recipe fits old crude iron ding");
            var inputs=new ArrayList<ItemStack>();
            recipe.required().forEach((item,count)->{for(int i=0;i<count;i++)inputs.add(new ItemStack(item));});
            h.assertTrue(!com.example.immortal_cultivation_mod.item.ForgingSystem.hasRequiredMaterials(recipe,inputs),"Old spirit-stone requirement applies");
            inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),64));
            h.assertTrue(com.example.immortal_cultivation_mod.item.ForgingSystem.hasRequiredMaterials(recipe,inputs),"Base herbs and spirit stones accepted");
            var output=com.example.immortal_cultivation_mod.item.ForgingSystem.createMold(recipe,inputs,1200,"normal_fire");
            h.assertTrue(output.is(AlchemyContent.pill(formula.id()))&&output.getCount()==formula.yield(),"Forge produces correct pill batch");
            h.assertTrue(PillData.effects(output).equals(brew(formula,formula.required()).effects()),"Forge retains medicinal effects");
            h.assertTrue(com.example.immortal_cultivation_mod.item.HigherWeaponItem.data(output).getCompound(com.example.immortal_cultivation_mod.item.ForgingSystem.TAG_FORGING).contains("average_temp"),"Old forge quality and temperature metadata retained");
        }
        var p=player(h);var menu=new AlchemyMenu(1,p.getInventory(),p.blockPosition(),"qingling");
        h.assertTrue(!menu.clickMenuButton(p,0),"Old instant furnace menu cannot bypass forging");
        var bottle=com.example.immortal_cultivation_mod.item.ForgingSystem.recipe(new ItemStack(AlchemyContent.BOTTLE_SLIP.get()));
        h.assertTrue(bottle!=null&&com.example.immortal_cultivation_mod.item.ForgingSystem.resultItem(bottle)==AlchemyContent.BOTTLE.get(),"Bottle uses old forging system too");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void newProtectionAndPotionEffects(GameTestHelper h) {
        var p=player(h);
        h.assertTrue(AlchemyEffects.consume(p,batch("mingmu"))
                &&p.getEffect(MobEffects.NIGHT_VISION).getDuration()==3600,"Night Vision lasts three minutes");
        p.getPersistentData().remove("NextCultivationPill");
        h.assertTrue(AlchemyEffects.consume(p,batch("bishui"))
                &&p.getEffect(MobEffects.WATER_BREATHING).getDuration()==3600,"Water Breathing lasts three minutes");
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,0));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,0));
        p.getPersistentData().remove("NextCultivationPill");AlchemyEffects.consume(p,batch("dingshen"));
        h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!p.hasEffect(MobEffects.WEAKNESS),"Steady Mind removes both debuffs");
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0));
        h.assertTrue(!p.hasEffect(MobEffects.WEAKNESS),"Steady Mind prevents weakness");
        p.getPersistentData().remove("NextCultivationPill");AlchemyEffects.consume(p,batch("bixie"));
        p.addEffect(new MobEffectInstance(MobEffects.WITHER,100,0));
        h.assertTrue(!p.hasEffect(MobEffects.WITHER),"Wither Ward prevents wither");
        var bone=player(h);AlchemyEffects.consume(bone,batch("tiegu"));
        h.assertTrue(bone.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==900
                &&bone.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==900
                &&bone.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==2
                &&bone.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==1,"Iron Bone grants Resistance III and Slowness II for 45 seconds");
        p.discard();bone.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void emergencyRegenTriggersOnce(GameTestHelper h) {
        var p=player(h);AlchemyEffects.consume(p,batch("xumai"));
        float maximum=p.getMaxHealth();
        p.setHealth(maximum*.3F);AlchemyEffects.tickPlayer(p);
        h.assertTrue(!AlchemyEffects.hasActive(p,Effect.HEALTH),"Thirty percent health no longer triggers recovery");
        p.setHealth(maximum*.2F);AlchemyEffects.tickPlayer(p);
        h.assertTrue(!AlchemyEffects.hasActive(p,Effect.HEALTH),"Exactly twenty percent does not trigger");
        p.setHealth(maximum*.19F);float before=p.getHealth();AlchemyEffects.tickPlayer(p);
        h.assertTrue(AlchemyEffects.hasActive(p,Effect.HEALTH),"Below twenty percent starts recovery");
        for(int i=1;i<900;i++)AlchemyEffects.tickPlayer(p);
        h.assertTrue(Math.abs(p.getHealth()-(before+maximum*.2F))<.02F
                &&!AlchemyEffects.hasActive(p,Effect.HEALTH),"Recovery totals twenty percent maximum health over exactly 45 seconds");
        p.setHealth(maximum*.1F);AlchemyEffects.tickPlayer(p);
        h.assertTrue(!AlchemyEffects.hasActive(p,Effect.HEALTH),"Regen does not trigger twice");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void qiSpellDiscountIsOneUseAndRequiresPayment(GameTestHelper h) {
        var p=player(h);AlchemyEffects.consume(p,batch("ningqi"));
        h.assertTrue(AlchemyEffects.spellDiscount(p)==.1,"Next spell has 10 percent discount");
        h.assertTrue(!com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(p,ModAttachments.getData(p),1000)
                &&AlchemyEffects.spellDiscount(p)==.1,"Failed cast preserves charge");
        h.assertTrue(com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(p,ModAttachments.getData(p),100)
                &&ModAttachments.getData(p).qi()==410,"Next paid spell costs 90 Qi");
        h.assertTrue(AlchemyEffects.spellDiscount(p)==0,"Charge consumed once");
        h.assertTrue(com.example.immortal_cultivation_mod.event.ServerEvents.spendSpellQiOrBlood(p,ModAttachments.getData(p),100)
                &&ModAttachments.getData(p).qi()==310,"Later spell costs full Qi");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void barrierBreakingAffectsOneMeleeHit(GameTestHelper h) {
        var p=player(h);var target=h.spawnWithNoFreeWill(EntityType.COW,new BlockPos(2,1,2));
        AlchemyEffects.consume(p,batch("pozhang"));
        h.assertTrue(AlchemyEffects.hasActive(p,Effect.NEXT_MELEE),"Melee bonus armed");
        target.hurt(p.damageSources().playerAttack(p),4);
        h.assertTrue(!AlchemyEffects.hasActive(p,Effect.NEXT_MELEE),"Bonus consumed by melee hit");
        h.assertTrue(Math.abs(target.getHealth()-5)<.01,"Four-damage hit becomes five damage: "+target.getHealth());
        p.discard();target.discard();h.succeed();
    }

    @GameTest(template="fate_test") public static void legacyPillsUseForgeRecipesAndRetiredItemsMigrate(GameTestHelper h) {
        for(var entry:AlchemyContent.LEGACY_FORMULAS.entrySet()) {
            var recipe=com.example.immortal_cultivation_mod.item.ForgingSystem.recipe(new ItemStack(entry.getValue().get()));
            h.assertTrue(recipe!=null && recipe.directOutput() && recipe.ingredientSlotCount()<=10,"Legacy pill recipe fits ding: "+entry.getKey());
            var inputs=new ArrayList<ItemStack>();
            recipe.required().forEach((item,count)->{for(int i=0;i<count;i++)inputs.add(new ItemStack(item));});
            h.assertTrue(!com.example.immortal_cultivation_mod.item.ForgingSystem.hasRequiredMaterials(recipe,inputs),"Spirit stones still required");
            inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get(),64));
            h.assertTrue(com.example.immortal_cultivation_mod.item.ForgingSystem.hasRequiredMaterials(recipe,inputs),"Legacy recipe accepts complete ingredients");
            var output=com.example.immortal_cultivation_mod.item.ForgingSystem.createMold(recipe,inputs,1200,"normal_fire");
            var expected=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",entry.getKey()+"_pill"));
            h.assertTrue(output.is(expected) && output.getCount()==1,"Existing pill item and behavior preserved");
            var recipeId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",entry.getKey()+"_formula");
            h.assertTrue(h.getLevel().getRecipeManager().byKey(recipeId).isPresent(),"Forge slip obtainable by crafting");
        }
        for(String old:java.util.List.of("small_level_up","big_level_up")) {
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",old));
            h.assertTrue(item==com.example.immortal_cultivation_mod.item.ModItems.ASHES.get(),"Removed level-up item becomes ash");
        }
        for(String old:java.util.List.of("low_qi_alchemy_formula","middle_qi_alchemy_formula","high_qi_alchemy_formula")) {
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",old));
            h.assertTrue(item==AlchemyContent.FORMULAS.get("huiling").get(),"Old recipe maps to current Qi formula");
        }
        h.succeed();
    }
}
