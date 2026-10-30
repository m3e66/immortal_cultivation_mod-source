package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.PillToxicityPayload;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.*;

@GameTestHolder("immortal_cultivation_toxicity")
@PrefixGameTestTemplate(false)
public final class PillToxicityGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"toxicity-test"));
        var pos=h.absolutePos(new BlockPos(1,1,1));p.setPos(pos.getX(),pos.getY(),pos.getZ());
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY).withQi(0));
        return p;
    }
    private static ItemStack pill(String id){return new ItemStack(AlchemyContent.pill(id));}
    private static void near(GameTestHelper h,double actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.0001,why+": "+actual+" != "+expected);}
    @GameTest(template="fate_test") public static void consumeExactlyOnceAndRejectFailedUse(GameTestHelper h) {
        var p=player(h);var stack=pill("huiling");double expected=PillToxicity.expected(p,stack);
        h.assertTrue(AlchemyEffects.consume(p,stack),"Valid dose succeeds");near(h,PillToxicity.value(p),expected,"One consumption gain");
        h.assertTrue(!AlchemyEffects.consume(p,stack),"Blocked use rejected");near(h,PillToxicity.value(p),expected,"Rejected use adds nothing");
        h.assertTrue(!AlchemyEffects.consume(p,ItemStack.EMPTY),"Empty use rejected");
        for(int i=0;i<200;i++)AlchemyEffects.tickPlayer(p);
        near(h,PillToxicity.value(p),expected,"Ongoing effects never add toxicity");
        h.assertTrue(ModAttachments.getData(p).qi()==ModAttachments.getData(p).maxQi()/20,"Existing five-percent Qi dose remains intact in clean stage");
        // Cancelled interactions never reach item.use/consume: no consumption callback, no change.
        near(h,PillToxicity.value(p),expected,"No callback adds no gain");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void cancelledInteractionAndLegacyFailureDoNotConsume(GameTestHelper h) {
        var p=player(h);var stack=pill("huichun");p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            p.gameMode.useItem(p,p.level(),stack,InteractionHand.MAIN_HAND);
            near(h,PillToxicity.value(p),0,"Actual cancelled server interaction adds no toxicity");h.assertTrue(stack.getCount()==1,"Cancelled interaction keeps item");
        } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        p.getPersistentData().putInt(com.example.immortal_cultivation_mod.spell.BodyCultivation.BODY_TEMPERING_PILL_USES,10);
        var failed=new ItemStack(ModItems.BODY_TEMPERING_PILL.get());p.setItemInHand(InteractionHand.MAIN_HAND,failed);failed.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
        near(h,PillToxicity.value(p),0,"At-cap legacy use does not add toxicity");h.assertTrue(failed.getCount()==1,"Failed legacy keeps pill");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void repeatTypesExpireSeparately(GameTestHelper h) {
        var p=player(h);var a=pill("huiling");var b=pill("huichun");double first=PillToxicity.expected(p,a),other=PillToxicity.expected(p,b);
        PillToxicity.consumed(p,a);near(h,PillToxicity.expected(p,a),first*1.35,"Same type repeats");near(h,PillToxicity.expected(p,b),other,"Other type independent");
        PillToxicity.state(p).putLong("Seconds",120);near(h,PillToxicity.expected(p,a),first,"Repeat expires at exact window boundary");
        PillToxicity.onlineSecond(p);h.assertTrue(PillToxicity.state(p).getCompound("Repeats").isEmpty(),"Expired records removed");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void potencyQualityRealmAndOldPillsUseCurrentRules(GameTestHelper h) {
        var p=player(h);var plain=pill("huichun");double base=PillToxicity.expected(p,plain);near(h,base,4,"Default ordinary potency");
        var batch=PillData.create(new Batch("huichun",1,Map.of(),Map.of(Effect.HEALTH,new Dose(12,120)),100,0,"superior",false));
        near(h,PillToxicity.expected(p,batch),base,"Twice potency times half quality");
        var rules=PillToxicity.policy(p).copy();rules.getCompound("Pills").getCompound("huichun").putInt("Realm",3);
        near(h,PillToxicity.expected(new CompoundTag(),rules,plain,1),base*1.75*1.75,"Above realm scaling");
        near(h,PillToxicity.expected(new CompoundTag(),rules,plain,4),base*.85,"Below realm scaling");
        rules.putDouble("Base",8);near(h,PillToxicity.expected(new CompoundTag(),rules,plain,3),8,"Previously saved plain stack obeys new policy");
        var old=plain.copy();var root=new CompoundTag();var data=new CompoundTag();var effects=new CompoundTag();var dose=new CompoundTag();dose.putDouble("Amount",12);dose.putInt("Ticks",120);effects.put("HEALTH",dose);data.put("Effects",effects);data.putString("Quality","common");root.put(PillData.KEY,data);
        old.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
        near(h,PillToxicity.expected(new CompoundTag(),rules,old,3),8,"Old saved fixed-dose batch migrates before current toxicity rules");
        h.assertTrue(!plain.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),"Read-time rules do not mutate old pills");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void stagesAndRealGameplayPenalties(GameTestHelper h) {
        var p=player(h);double[] thresholds={0,19.99,20,39.99,40,59.99,60,79.99,80,100};int[] expected={0,0,1,1,2,2,3,3,4,4};
        for(int i=0;i<thresholds.length;i++){PillToxicity.setValue(p,thresholds[i]);h.assertTrue(PillToxicity.stage(p)==expected[i],"Threshold "+thresholds[i]);}
        PillToxicity.setValue(p,60);near(h,AlchemyEffects.cultivationGain(p,1000),850,"Cultivation penalty once");
        var data=ModAttachments.getData(p).withQi(1000);ModAttachments.setData(p,data);
        h.assertTrue(ServerEvents.spendSpellQiOrBlood(p,data,100),"Spell can spend");h.assertTrue(ModAttachments.getData(p).qi()==885,"115 percent spell cost");
        ModAttachments.setData(p,data);h.assertTrue(ServerEvents.spendSustainedSpellQiOrBlood(p,data,100,"ignite_flare"),"Sustained spell can spend");h.assertTrue(ModAttachments.getData(p).qi()==885,"Sustained spell cost applied once");
        int total=0;for(int i=0;i<10;i++)total+=PillToxicity.naturalQi(p,1);h.assertTrue(total==7,"Small natural recovery carries fractions");
        near(h,PillToxicity.multiplier(p,"Restore"),.6,"Explicit total restoration penalty");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void tooltipPredictionMatchesCapAndDetoxEligibility(GameTestHelper h) {
        var p=player(h);var stack=pill("huiling");PillToxicity.setValue(p,99);
        near(h,PillToxicity.expected(p,stack),1,"Tooltip shows actual increase at meter cap");
        PillToxicity.consumed(p,stack);near(h,PillToxicity.value(p),100,"Actual consumption matches preview");
        near(h,PillToxicity.expected(p,stack),0,"Full meter previews zero increase");
        var s=PillToxicity.state(p);var rules=PillToxicity.policy(p);
        h.assertTrue(PillToxicity.detoxReason(s,rules,1)==null&&PillToxicity.canDetox(p,false),"Client readiness uses the server eligibility rule");
        h.assertTrue(PillToxicity.detoxReason(s,rules,0).equals("realm"),"Realm restriction predicted");
        s.putLong("DetoxUntil",100);h.assertTrue(PillToxicity.detoxReason(s,rules,1).equals("cooldown"),"Cooldown restriction predicted");
        s.remove("DetoxUntil");PillToxicity.setValue(p,0);h.assertTrue(PillToxicity.detoxReason(s,rules,1).equals("clean"),"Clean restriction predicted");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void higherFortuneQualityCannotIncreaseConfiguredToxicity(GameTestHelper h) {
        var p=player(h);var stack=pill("huichun");var rules=PillToxicity.policy(p).copy();
        rules.getCompound("Quality").putDouble("common",2);rules.getCompound("Quality").putDouble("fine",1.5);
        double previous=PillToxicity.expected(new CompoundTag(),rules,stack,1);
        for(int quality=1;quality<=3;quality++) {
            var tag=new CompoundTag();tag.putInt("icmPillQuality",quality);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            double current=PillToxicity.expected(new CompoundTag(),rules,stack,1);
            h.assertTrue(current<previous,"Higher quality always reduces toxicity even if custom quality factors exceed one");previous=current;
        }
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void ongoingRestorationDoesNotCompound(GameTestHelper h) {
        var p=player(h);p.setHealth(1);var item=PillData.create(new Batch("huichun",1,Map.of(),Map.of(Effect.HEALTH,new Dose(6,120),Effect.QI,new Dose(20,120)),100,0,"superior",false));
        PillToxicity.setValue(p,40);h.assertTrue(AlchemyEffects.consume(p,item),"Dose succeeds");PillToxicity.setValue(p,40);
        for(int i=0;i<120;i++)AlchemyEffects.tickPlayer(p);
        near(h,p.getHealth(),1+p.getMaxHealth()*.06*.75,"Ongoing HP gets one multiplier per base increment");
        h.assertTrue(ModAttachments.getData(p).qi()==(int)(ModAttachments.getData(p).maxQi()*.20*.75),"Ongoing Qi retains fractions after penalty");
        near(h,PillToxicity.value(p),40,"Doses do not change toxicity");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void nonlethalDamageAllRealms(GameTestHelper h) {
        for(float max:new float[]{20,120,700,4000,22000,130000,800000,6000000,40000000,300000000}) {
            near(h,PillToxicity.nonlethalHealth(max,max,1),max*.99,"One percent scales to realm HP");
            h.assertTrue(PillToxicity.nonlethalHealth(2,max,25)>=1,"Never lethal");
        }
        near(h,PillToxicity.nonlethalHealth(.5F,1000,1),.5,"Never heals an already low player");
        var p=player(h);PillToxicity.setValue(p,90);p.setHealth(2);PillToxicity.state(p).putLong("DamageAt",1);PillToxicity.onlineSecond(p);
        h.assertTrue(p.isAlive()&&p.getHealth()>=1,"Scheduled server damage nonlethal");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void damagePausesDetoxWithoutStoppingMeditation(GameTestHelper h) {
        var p=player(h);PillToxicity.setValue(p,50);PillToxicity.onlineSecond(p);near(h,PillToxicity.value(p),49.99,"Normal online recovery");
        ModAttachments.setData(p,ModAttachments.getData(p).withMeditating(true));PillToxicity.onlineSecond(p);near(h,PillToxicity.value(p),49.94,"Meditation bonus");
        float before=p.getHealth();PillToxicity.damageNonlethal(p,1);
        h.assertTrue(p.getHealth()<before&&ModAttachments.getData(p).isMeditating(),"Toxicity damage reduces health while meditation continues");
        PillToxicity.onlineSecond(p);near(h,PillToxicity.value(p),49.93,"Damage pauses only the extra detox bonus");
        ModAttachments.setData(p,ModAttachments.getData(p).withMeditating(false));PillToxicity.onlineSecond(p);near(h,PillToxicity.value(p),49.92,"Leaving stops bonus");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void damagePauseExpiresWhileClean(GameTestHelper h) {
        var p=player(h);PillToxicity.pauseMeditationDetox(p);for(int i=0;i<6;i++)PillToxicity.onlineSecond(p);
        PillToxicity.setValue(p,10);ModAttachments.setData(p,ModAttachments.getData(p).withMeditating(true));PillToxicity.onlineSecond(p);near(h,PillToxicity.value(p),9.95,"A past damage pause cannot remain frozen while clean");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void detoxRealmCooldownAndActualConsumption(GameTestHelper h) {
        var p=player(h);PillToxicity.setValue(p,80);var stack=pill("qingdu");p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_MORTAL));
        stack.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);h.assertTrue(stack.getCount()==1,"Wrong realm does not consume");near(h,PillToxicity.value(p),80,"Failed detox unchanged");
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY));
        stack.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);h.assertTrue(stack.isEmpty(),"Successful detox consumes one");near(h,PillToxicity.value(p),55,"Removes 25 and adds none");
        var second=pill("qingdu");p.setItemInHand(InteractionHand.MAIN_HAND,second);p.getPersistentData().remove("NextCultivationPill");
        second.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);h.assertTrue(second.getCount()==1,"Cooldown prevents consumption");near(h,PillToxicity.value(p),55,"Cooldown does not detox");
        h.assertTrue(PillToxicity.cooldown(p)==300,"Server cooldown");PillToxicity.state(p).putLong("Seconds",300);h.assertTrue(PillToxicity.canDetox(p,false),"Cooldown expires");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void persistenceCloneDeathAndSyncCodec(GameTestHelper h) {
        var p=player(h);PillToxicity.consumed(p,pill("huiling"));PillToxicity.setValue(p,70);PillToxicity.state(p).putLong("DetoxUntil",300);
        var saved=p.saveWithoutId(new CompoundTag());var loaded=player(h);loaded.load(saved);
        near(h,PillToxicity.value(loaded),70,"Player save/load persists attachment");h.assertTrue(PillToxicity.cooldown(loaded)==300,"Offline time does not tick cooldown");
        h.assertTrue(PillToxicity.state(loaded).getCompound("Repeats").contains("huiling"),"Repeat tracking persists");
        var payload=new PillToxicityPayload(PillToxicity.state(p),PillToxicity.policy(p));
        var other=player(h);near(h,PillToxicity.value(other),0,"Another player's state remains isolated");other.discard();
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {PillToxicityPayload.STREAM_CODEC.encode(buffer,payload);var copy=PillToxicityPayload.STREAM_CODEC.decode(buffer);h.assertTrue(copy.state().equals(payload.state())&&copy.rules().equals(payload.rules()),"Network roundtrip preserves rules, repeats, cooldown and precise value");}finally{buffer.release();}
        var clone=player(h);PillToxicity.clone(new PlayerEvent.Clone(clone,p,false));near(h,PillToxicity.value(clone),70,"Nondeath clone preserves toxicity");
        PillToxicity.clone(new PlayerEvent.Clone(clone,p,true));near(h,PillToxicity.value(clone),0,"Death clears toxicity");h.assertTrue(PillToxicity.state(clone).getCompound("Repeats").isEmpty(),"Death clears repeats");
        p.discard();loaded.discard();clone.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void legacyPillsAndBottleAreCovered(GameTestHelper h) {
        var p=player(h);var bigu=new ItemStack(ModItems.BIGU_PILL.get());p.setItemInHand(InteractionHand.MAIN_HAND,bigu);
        double expected=PillToxicity.expected(p,bigu);bigu.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);near(h,PillToxicity.value(p),expected,"Legacy successful use once");h.assertTrue(bigu.isEmpty(),"Legacy pill consumed");
        var bottle=new ItemStack(AlchemyContent.BOTTLE.get());var contents=pill("huiling");JadeBottleItem.store(bottle,contents);p.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        double before=PillToxicity.value(p),gain=PillToxicity.expected(p,contents);bottle.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);near(h,PillToxicity.value(p),before+gain,"Bottle applies once through central dose path");h.assertTrue(JadeBottleItem.contents(bottle).isEmpty(),"Bottle consumes one");p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void detoxRecipeAndClamp(GameTestHelper h) {
        var f=formula("qingdu");var batch=brew(f,f.required());h.assertTrue(!batch.failed()&&batch.count()==5,"Detox brew valid");
        h.assertTrue(PillData.create(batch).is(AlchemyContent.pill("qingdu")),"Crafted detox retains special behavior");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","qingdu_formula")).isPresent(),"Formula obtainable");
        near(h,PillToxicity.clamp(-10),0,"Lower clamp");near(h,PillToxicity.clamp(500),100,"Upper clamp");near(h,PillToxicity.clamp(Double.NaN),0,"Invalid value sanitized");h.succeed();
    }
}
