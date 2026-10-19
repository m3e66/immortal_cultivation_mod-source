package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.curse.AnnihilationCurse;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.TrainingDummyEntity;
import com.example.immortal_cultivation_mod.item.DummyControlStickItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.training.TrainingDummyControls;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_dummy_debuff")
@PrefixGameTestTemplate(false)
public final class TrainingDummyDebuffGameTests {
    private static final class Field implements AutoCloseable {
        final FakePlayer player;
        final TrainingDummyEntity dummy;
        Field(GameTestHelper h) {
            player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"dummy-cleanser"));
            player.setGameMode(GameType.SURVIVAL);
            dummy=spawn(h,new Vec3(2,5000,2));dummy.controller=player.getUUID();
            player.setPos(dummy.position().add(0,0,-3));
            var stick=new ItemStack(ModItems.DUMMY_CONTROL_STICK.get());DummyControlStickItem.bind(stick,dummy);
            player.setItemInHand(InteractionHand.MAIN_HAND,stick);
        }
        boolean clear(){return TrainingDummyControls.edit(player,dummy.getUUID(),"clear_debuffs","","");}
        @Override public void close(){dummy.discard();player.discard();}
    }
    private static TrainingDummyEntity spawn(GameTestHelper h,Vec3 point) {
        var dummy=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());dummy.setPos(h.absoluteVec(point));
        dummy.ensureProfile();h.getLevel().addFreshEntity(dummy);return dummy;
    }
    private static CompoundTag saved(TrainingDummyEntity dummy) {
        var tag=new CompoundTag();dummy.addAdditionalSaveData(tag);return tag;
    }
    @GameTest(template="fate_test")
    public static void clearsDebuffsButKeepsBuffsVitalsAndPracticeConfiguration(GameTestHelper h) {
        try(var f=new Field(h)) {
            var state=saved(f.dummy);var cooldowns=new CompoundTag();
            cooldowns.putLong(ModSpells.FIREBALL,h.getLevel().getGameTime()+200);state.put("DummyCooldowns",cooldowns);
            f.dummy.readAdditionalSaveData(state);
            f.dummy.unlimitedQi=false;f.dummy.mobile=true;f.dummy.setHealth(123);f.dummy.profile().putInt("Qi",789);
            f.dummy.setTarget(f.player);f.dummy.setDomainRequested(true);
            QiDomainSystem.npcTick(new EntityTickEvent.Post(f.dummy));
            var before=f.dummy.profile().copy();var oldCooldowns=saved(f.dummy).getCompound("DummyCooldowns").copy();
            double speed=f.dummy.getAttributeValue(Attributes.MOVEMENT_SPEED);
            f.dummy.addEffect(new MobEffectInstance(MobEffects.POISON,200,1));
            f.dummy.addEffect(new MobEffectInstance(ModEffects.WEIYA_SUPPRESSED,200));
            f.dummy.addEffect(new MobEffectInstance(ModEffects.BONE_BELL,200));
            f.dummy.addEffect(new MobEffectInstance(ModEffects.DAMP,200,5));
            f.dummy.addEffect(new MobEffectInstance(MobEffects.GLOWING,200));
            f.dummy.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200));
            f.dummy.addEffect(new MobEffectInstance(ModEffects.HUTI_QI,200));
            f.dummy.addEffect(new MobEffectInstance(ModEffects.NPC_WEAPON_GUARD,200));
            f.dummy.igniteForSeconds(10);f.dummy.setTicksFrozen(120);
            h.assertTrue(f.clear(),"Owner's bound controller accepts cleanse action");
            h.assertTrue(!f.dummy.hasEffect(MobEffects.POISON)&&!f.dummy.hasEffect(ModEffects.WEIYA_SUPPRESSED)
                    &&!f.dummy.hasEffect(ModEffects.BONE_BELL)&&!f.dummy.hasEffect(ModEffects.DAMP)
                    &&!f.dummy.hasEffect(MobEffects.GLOWING),"Vanilla and domain debuffs, including neutral water stacks and solar reveal, removed");
            h.assertTrue(f.dummy.hasEffect(MobEffects.REGENERATION)&&f.dummy.hasEffect(ModEffects.HUTI_QI)
                    &&f.dummy.hasEffect(ModEffects.NPC_WEAPON_GUARD),"Beneficial and neutral guard effects remain");
            h.assertTrue(f.dummy.getHealth()==123&&f.dummy.profile().getInt("Qi")==789
                    &&f.dummy.profile().equals(before)&&saved(f.dummy).getCompound("DummyCooldowns").equals(oldCooldowns),"Cleanse does not heal, refill Qi, change knowledge or reset cooldowns");
            h.assertTrue(f.dummy.mobile&&!f.dummy.unlimitedQi&&f.dummy.immortal&&f.dummy.getTarget()==f.player
                    &&f.dummy.domainRequested&&QiDomainSystem.blasting(f.dummy),"Movement, target and own blast/domain settings remain");
            h.assertTrue(!f.dummy.isOnFire()&&f.dummy.getTicksFrozen()==0
                    &&Math.abs(f.dummy.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed)<.00001,"Burn, frost and negative attribute modifiers cleared");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void clearsHiddenDomainStateAndCasterExposureWithoutStoppingCaster(GameTestHelper h) {
        try(var f=new Field(h)) {
            var caster=spawn(h,new Vec3(6,5000,2));
            try {
                var p=caster.profile();String method="blood_demon_jindan";
                p.putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE);
                p.putString("Method",method);p.getCompound("MethodProficiencies").putInt(method,500);
                caster.setTarget(f.dummy);
                caster.setDomainRequested(true);QiDomainSystem.npcTick(new EntityTickEvent.Post(caster));
                var aura=QiDomainSystem.view(caster);h.assertTrue(aura!=null&&aura.domain(),"Real mastered domain active");
                long now=h.getLevel().getGameTime();
                BloodLilyBloom.expose(aura,f.dummy,now);
                h.assertTrue(BloodLilyBloom.progress(f.dummy)>0,"Live hostile blood domain applies blood-lily growth");
                TideWaterStacks.expose(aura,f.dummy,now);
                f.dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);f.dummy.setHealth(20);
                for(int i=0;i<12;i++)ColdTideFreeze.expose(caster,f.dummy,now+i*20);
                UUID other=UUID.randomUUID();aura.domainExposure.put(f.dummy.getUUID(),8);aura.domainExposure.put(other,4);
                h.assertTrue(ColdTideFreeze.isFrozen(f.dummy),"Full domain freeze is live before cleanse: progress="+ColdTideFreeze.progress(f.dummy));
                h.assertTrue(f.clear(),"Hidden domain debuffs can be cleansed even while dummy tick is frozen");
                h.assertTrue(!ColdTideFreeze.isFrozen(f.dummy)&&ColdTideFreeze.progress(f.dummy)==0&&BloodLilyBloom.progress(f.dummy)==0
                        &&!f.dummy.getPersistentData().contains(ColdTideFreeze.TAG)&&!f.dummy.getPersistentData().contains(BloodLilyBloom.TAG),"Saved and in-memory ice/blood marks cleared together");
                h.assertTrue(!aura.domainExposure.containsKey(f.dummy.getUUID())&&aura.domainExposure.get(other)==4
                        &&QiDomainSystem.view(caster)==aura&&aura.domain(),"Only this dummy's buildup removed; caster and other victims unchanged");
                p.putString("Method","hanlan_ningyuan_jing");p.getCompound("MethodProficiencies").putInt("hanlan_ningyuan_jing",500);
                f.dummy.setPos(f.dummy.position().add(2,0,0));TideWaterStacks.update(f.dummy,now+241);
                h.assertTrue(TideWaterStacks.stacks(f.dummy)==0,"Old water-domain movement tracking cannot restore stacks after cleanse");
                var loaded=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());
                try {loaded.readAdditionalSaveData(saved(f.dummy));ColdTideFreeze.restore(loaded);BloodLilyBloom.restore(loaded);
                    h.assertTrue(ColdTideFreeze.progress(loaded)==0&&BloodLilyBloom.progress(loaded)==0,"Cleansed hidden marks stay absent after save/reload");
                }finally {loaded.discard();}
            }finally {caster.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void clearsShentongInfectionAndHealingLockWhileKeepingInnateOwnership(GameTestHelper h) {
        try(var f=new Field(h)) {
            f.dummy.setHealth(100);AnnihilationCurse.markByAttack(f.dummy);
            var curse=AnnihilationCurse.state(f.dummy);curse.putBoolean("Owned",true);curse.putInt("Cooldown",333);
            curse.putFloat("Coverage",.8F);curse.putFloat("BlackHp",95);curse.putInt("Bloom",160);
            f.dummy.heal(10);h.assertTrue(f.dummy.getHealth()==100,"Curse blocks healing before cleanse");
            h.assertTrue(f.clear(),"Shentong cleanse accepted");
            h.assertTrue(AnnihilationCurse.coverage(f.dummy)==0&&!AnnihilationCurse.healingLocked(f.dummy)
                    &&AnnihilationCurse.blackHp(f.dummy)==0,"Rune coverage, black HP and healing lock cleared");
            h.assertTrue(AnnihilationCurse.owned(f.dummy)&&AnnihilationCurse.cooldown(f.dummy)==333,"Learned innate ability and its cooldown retained");
            f.dummy.heal(10);h.assertTrue(f.dummy.getHealth()==110,"Normal healing works after cleanse");
            f.dummy.getPersistentData().remove(AnnihilationCurse.TAG);AnnihilationCurse.markByAttack(f.dummy);f.clear();
            h.assertTrue(!f.dummy.getPersistentData().contains(AnnihilationCurse.TAG),"Infection-only curse leaves no saved data behind");
            h.assertTrue(f.clear(),"Repeated cleanse is safe");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void strangersUnboundAndOutOfRangeControllersCannotCleanse(GameTestHelper h) {
        try(var f=new Field(h)) {
            f.dummy.addEffect(new MobEffectInstance(MobEffects.POISON,200));AnnihilationCurse.markByAttack(f.dummy);
            var stranger=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"dummy-stranger"));
            try {
                stranger.setPos(f.player.position());stranger.setItemInHand(InteractionHand.MAIN_HAND,f.player.getMainHandItem().copy());
                h.assertTrue(!TrainingDummyControls.edit(stranger,f.dummy.getUUID(),"clear_debuffs","",""),"Copied bound controller does not bypass ownership");
            }finally {stranger.discard();}
            f.player.setPos(f.dummy.position().add(97,0,0));h.assertTrue(!f.clear(),"Out of range cleanse rejected");
            f.player.setPos(f.dummy.position());f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(!f.clear()&&f.dummy.hasEffect(MobEffects.POISON)&&AnnihilationCurse.healingLocked(f.dummy),"Rejected actions leave all effects intact");
        }h.succeed();
    }
}
