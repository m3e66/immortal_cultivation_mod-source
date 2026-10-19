package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class MethodDomainEffectsGameTests {
    private static final class Rolls extends LegacyRandomSource {
        int calls,bound;final boolean success;
        Rolls(boolean success){super(1);this.success=success;}
        @Override public int nextInt(int bound){calls++;this.bound=bound;return success?0:bound-1;}
    }
    private static long pulseTime(Fixture fixture) {
        long now=fixture.owner.level().getGameTime();return now+Math.floorMod(fixture.owner.getId()-now,20);
    }
    @GameTest(template="fate_test") public static void allSeventeenNpcMethodsApplyTheirRealDomainBehavior(GameTestHelper h) {
        h.runAfterDelay(2-Math.floorMod(h.getLevel().getGameTime(),2),()->{
        for(var style:MethodDomainStyle.values()) {
            var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            var first=EntityType.COW.create(h.getLevel());var second=EntityType.COW.create(h.getLevel());
            var qiVictim=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            Vec3 origin=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,170,3)));
            try {
                caster.setNoAi(true);caster.setNoGravity(true);caster.setPos(origin);NpcProfile.ensure(caster,caster.npcArchetype());
                var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
                profile.putString("Method",style.method);profile.getCompound("MethodProficiencies").putInt(style.method,500);profile.putInt("Qi",100000);profile.putFloat("SolarPower",50);
                NpcProfile.ensure(caster,caster.npcArchetype());
                h.assertTrue(QiDomainSystem.mastery(caster)>=500,"NPC fixture has chosen Great mastery for each art: "+style);
                for(var cow:List.of(first,second)) {
                    cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(style==MethodDomainStyle.BONE?20:100000);
                    cow.setHealth(100000);cow.setPos(origin.add(cow==first?3:4.8,0,0));h.getLevel().addFreshEntity(cow);
                }
                second.setLastHurtByMob(caster);
                h.getLevel().addFreshEntity(caster);caster.setTarget(first);caster.tickCount=21;
                QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
                var aura=QiDomainSystem.view(caster);h.assertTrue(aura!=null&&aura.domain()&&aura.method.equals(style.method),"NPC opens correct 200-percent domain: "+style);
                caster.setHealth(caster.getMaxHealth()/2);float ownHp=caster.getHealth();
                first.setDeltaMovement(.1,.1,0);
                long now=h.getLevel().getGameTime(),pulse=now+Math.floorMod(caster.getId()-now,20);
                MethodDomainEffects.tick(aura,pulse,new Rolls(true));
                if(style==MethodDomainStyle.WIND) {
                    int seam=0;while(MethodDomainEffects.windVisibility(seam,now)<.1F)seam++;
                    Vec3 point=MethodDomainEffects.windStart(origin,seam,now).lerp(MethodDomainEffects.windEnd(origin,seam,now),.5);
                    first.setPos(point.add(0,-first.getBbHeight()/2,0));MethodDomainEffects.windContacts(aura,now);
                } else if(style==MethodDomainStyle.EVERGREEN||style.kind==MethodDomainStyle.Kind.BLOOD
                        ||style.kind==MethodDomainStyle.Kind.DRAIN||style.kind==MethodDomainStyle.Kind.CROWN) {
                    Vec3 point=QiDomainSystem.motePosition(caster,0,now);
                    // Real server update performs contact, damage, marking and block placement.
                    first.setPos(point.add(0,-first.getBbHeight()/2,0));
                    QiDomainSystem.tickAll();
                }
                switch(style) {
                    case BREATH,CLEAR,NETHER -> h.assertTrue(first.getHealth()==100000&&first.getActiveEffects().isEmpty(),"Visual-only NPC art adds no passive: "+style);
                    case BLOOD_THORN,BLOOD_DRAIN,BLOOD_CROWN -> {
                        h.assertTrue(BloodLilyBloom.progress(first)>0,"Actual NPC blood lilies grow automatically: "+style);
                        qiVictim.setNoAi(true);qiVictim.setPos(origin.add(4,0,3));NpcProfile.ensure(qiVictim,qiVictim.npcArchetype());
                        qiVictim.getPersistentData().getCompound("ImmortalCultivationNpc").putInt("Qi",10000);
                        caster.setCustomName(net.minecraft.network.chat.Component.literal("施术测试"));qiVictim.setCustomName(net.minecraft.network.chat.Component.literal("受术测试"));
                        profile.putString("Name","甲施术者");profile.putString("Identity","wandering_cultivator");
                        qiVictim.getPersistentData().getCompound("ImmortalCultivationNpc").putString("Name","乙对手");
                        qiVictim.getPersistentData().getCompound("ImmortalCultivationNpc").putString("Identity","demonic_cultivator");
                        h.getLevel().addFreshEntity(qiVictim);profile.putInt("Qi",1000);qiVictim.setLastHurtByMob(caster);fullBloom(aura,qiVictim);
                        h.assertTrue(NpcProfile.qi(qiVictim,qiVictim.npcArchetype())<10000&&NpcProfile.qi(caster,caster.npcArchetype())>1000,"NPC consumes actual victim Qi and receives return: "+style+" progress="+BloodLilyBloom.progress(qiVictim)+" victimQi="+NpcProfile.qi(qiVictim,qiVictim.npcArchetype())+" casterQi="+NpcProfile.qi(caster,caster.npcArchetype())+" power="+aura.power+" mastery="+QiDomainSystem.mastery(caster)+" affect="+QiDomainSystem.canDomainAffect(caster,qiVictim)+" method="+aura.method);
                    }
                    case REINCARNATION -> h.assertTrue(first.hasEffect(MobEffects.WEAKNESS)&&first.getEffect(MobEffects.WEAKNESS).getDuration()==200,"NPC Reincarnation adds ten-second levels");
                    case DEVOUR -> h.assertTrue(first.getHealth()<100000,"NPC Devouring deals real damage");
                    case WIND -> h.assertTrue(first.getHealth()<100000&&first.getDeltaMovement().y>.2,"NPC moving seam cuts and lifts");
                    case EVERGREEN -> h.assertTrue(!h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(30)).isEmpty(),"NPC pollen builds actual collidable wood");
                    case RENEWAL_FIRE -> h.assertTrue(first.getHealth()<100000&&first.isOnFire()&&caster.getHealth()>ownHp,"NPC fire burns and returns actual vitality");
                    case ICE -> h.assertTrue(first.getTicksFrozen()>0&&first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"NPC winter grows model-coating exposure");
                    case BONE -> h.assertTrue(first.hasEffect(ModEffects.BONE_BELL)&&first.getAttributeValue(Attributes.MOVEMENT_SPEED)<first.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),"NPC stronger body applies actual movement suppression to weaker victim");
                    case SOLAR -> h.assertTrue(Math.abs(100000-first.getHealth()-caster.getMaxHealth()*.05F)<.01,"NPC solar uses saved 50-percent Sun Energy and caster maximum HP");
                    case STORM -> h.assertTrue(first.getHealth()<100000&&second.getHealth()<100000,"NPC storm automatically chains real damage between actual enemies");
                    case MOUNTAIN -> h.assertTrue(first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&first.getDeltaMovement().y<.1,"NPC mountain slows and presses down");
                    case COLD_TIDE -> {
                        first.setPos(first.position().add(1,0,0));TideWaterStacks.update(first,pulse+1);
                        h.assertTrue(TideWaterStacks.stacks(first)==1&&first.getTicksFrozen()==0&&caster.getHealth()>ownHp,"NPC abyss adds movement-based water and heals without freezing");
                    }
                }
            } finally {
                for(var terrain:h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(30))) {
                    terrain.placedStates().keySet().forEach(pos->h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));terrain.discard();
                }
                QiDomainSystem.stop(caster);caster.discard();first.discard();second.discard();qiVictim.discard();
            }
        }h.succeed();});
    }

    @GameTest(template="fate_test",timeoutTicks=220) public static void npcWinterCanFullyCoatTwentyMaxHpDuringItsBlast(GameTestHelper h) {
        var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var target=EntityType.COW.create(h.getLevel());
        Runnable cleanup=()->{QiDomainSystem.stop(caster);ColdTideFreeze.clear(target);caster.discard();target.discard();};
        try {
            caster.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,220,3))));caster.setNoAi(true);caster.setNoGravity(true);
            NpcProfile.ensure(caster,caster.npcArchetype());var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Method",MethodDomainStyle.ICE.method);profile.getCompound("MethodProficiencies").putInt(MethodDomainStyle.ICE.method,500);profile.putInt("Qi",100000);
            target.setNoAi(true);target.setNoGravity(true);target.setPos(caster.position().add(3,0,0));target.setInvulnerable(true);
            h.getLevel().addFreshEntity(caster);h.getLevel().addFreshEntity(target);caster.setTarget(target);caster.tickCount=21;
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
            h.runAfterDelay(189,()->{try {
                var aura=QiDomainSystem.view(caster);
                h.assertTrue(aura!=null&&aura.blasting&&aura.domainExposure.getOrDefault(target.getUUID(),0)==8,"Natural NPC blast fully coats a twenty-max-HP target on the shared HP curve: aura="+(aura==null?"null":aura.blasting+"/"+aura.method+"/"+aura.domainExposure.getOrDefault(target.getUUID(),0)+"/"+aura.power)+", alive="+caster.isAlive()+", target="+(caster.getTarget()==target)+", Qi="+NpcProfile.qi(caster,caster.npcArchetype())+", distance="+caster.distanceTo(target)+", mastery="+QiDomainSystem.mastery(caster));
                h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==3,"NPC winter reaches actual Slowness IV");
                h.assertTrue(ColdTideFreeze.isFrozen(target),"NPC winter also triggers the full action lock, not just the ice coating");
                h.succeed();
            }finally{cleanup.run();}});
        }catch(Throwable e){cleanup.run();throw e;}
    }

    @GameTest(template="fate_test") public static void npcMasteryMigrationPersistsAndPreservesExplicitValues(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var copy=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            NpcProfile.ensure(npc,npc.npcArchetype());var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");String method=profile.getString("Method");
            profile.remove("DomainMasteryVersion");var uses=new net.minecraft.nbt.CompoundTag();uses.putInt(method,7);profile.put("MethodProficiencies",uses);
            NpcProfile.ensure(npc,npc.npcArchetype());h.assertTrue(QiDomainSystem.mastery(npc)==7,"Existing earned use counters are never promoted to automatic domain mastery");
            profile.getCompound("MethodProficiencies").putInt(method,1234);var saved=new net.minecraft.nbt.CompoundTag();npc.saveWithoutId(saved);copy.load(saved);
            h.assertTrue(QiDomainSystem.mastery(copy)==1234,"Trained mastery persists through save/reload");
            profile.getCompound("MethodProficiencies").putInt(method,499);NpcProfile.ensure(npc,npc.npcArchetype());
            h.assertTrue(QiDomainSystem.mastery(npc)==499,"Explicit lower per-method value after migration is respected");
            profile.remove("DomainMasteryVersion");profile.remove("MethodProficiencies");profile.putInt("MethodProficiency",123);NpcProfile.ensure(npc,npc.npcArchetype());
            h.assertTrue(QiDomainSystem.mastery(npc)==123,"Explicit legacy proficiency override survives migration");
        }finally{npc.discard();copy.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void npcDomainsProtectNeutralBystandersAndKeepActiveArtDuringCombat(GameTestHelper h) {
        var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var enemy=EntityType.COW.create(h.getLevel());var neutral=EntityType.COW.create(h.getLevel());
        try {
            NpcProfile.ensure(caster,caster.npcArchetype());var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Method",MethodDomainStyle.ICE.method);caster.setTarget(enemy);caster.tickCount=40;
            NpcProfile.tickCultivation(caster,caster.npcArchetype());
            h.assertTrue(profile.getString("Method").equals(MethodDomainStyle.ICE.method),"Ongoing fight cannot silently replace its active art and reset winter buildup");
            h.assertTrue(QiDomainSystem.canDomainAffect(caster,enemy)&&!QiDomainSystem.canDomainAffect(caster,neutral),"NPC effects protect neutral bystanders using actual combat-enemy rules");
            neutral.setLastHurtByMob(caster);h.assertTrue(QiDomainSystem.canDomainAffect(caster,neutral),"Another real combatant can receive chain arcs and passive effects");
        }finally{QiDomainSystem.stop(caster);caster.discard();enemy.discard();neutral.discard();}h.succeed();
    }
    private static final class Fixture implements AutoCloseable {
        final FakePlayer owner;final List<LivingEntity> spawned=new ArrayList<>();
        Fixture(GameTestHelper h,MethodDomainStyle style) {
            owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"method-domain"));
            owner.setGameMode(GameType.SURVIVAL);
            owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,64,3))));
            if(style==MethodDomainStyle.ICE)owner.setPos(owner.position().add(0,256,0));
            ModAttachments.setData(owner,ModAttachments.getData(owner)
                    .withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY)
                    .withActiveCultivationMethod(style.method).withMethodProficiencies(Map.of(style.method,500)).withQi(1_000_000));
            CombatStateManager.setEnabled(owner,true);CombatStateManager.toggleManualCombat(owner);
            owner.getPersistentData().putInt("ImmortalCultivationQiReleasePercent",200);QiDomainSystem.toggle(owner);
        }
        QiDomainSystem.Aura aura(){return QiDomainSystem.view(owner);}
        net.minecraft.world.entity.animal.Cow cow(Vec3 offset) {
            var cow=EntityType.COW.create(owner.level());cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(owner.position().add(offset));
            owner.level().getChunkAt(cow.blockPosition());owner.level().addFreshEntity(cow);spawned.add(cow);return cow;
        }
        @Override public void close(){QiDomainSystem.stop(owner);spawned.forEach(e->{BloodLilyBloom.clear(e);ColdTideFreeze.clear(e);TideWaterStacks.clear(e);e.discard();});owner.discard();}
    }
    @GameTest(template="fate_test") public static void reincarnationRollsEveryEntityOncePerSecondAndStacks(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.REINCARNATION)) {
            var first=f.cow(new Vec3(2,0,0));var second=f.cow(new Vec3(0,0,19));
            var corner=f.cow(new Vec3(19,0,19));var random=new Rolls(true);long now=pulseTime(f);
            MethodDomainEffects.tick(f.aura(),now,random);
            h.assertTrue(random.calls==2&&random.bound==100,"Exactly one 1-in-100 roll for each living entity inside the sphere");
            for(var target:List.of(first,second)) {
                h.assertTrue(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==0
                        &&target.getEffect(MobEffects.WEAKNESS).getAmplifier()==0,"First proc gives both level I debuffs");
                h.assertTrue(target.getEffect(MobEffects.WEAKNESS).getDuration()==200,"Debuff lasts ten seconds");
            }
            MethodDomainEffects.tick(f.aura(),now,random);MethodDomainEffects.tick(f.aura(),now+19,random);
            h.assertTrue(random.calls==2,"No repeated rolls within one second");
            MethodDomainEffects.tick(f.aura(),now+20,random);
            h.assertTrue(random.calls==4&&first.getEffect(MobEffects.WEAKNESS).getAmplifier()==1
                    &&first.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==1,"Second proc adds one level to both effects");
            // A new aura or method change cannot reroll during the nineteen intervening ticks.
            f.aura().lastMethodPulse=Long.MIN_VALUE;
            for(int i=21;i<40;i++)MethodDomainEffects.tick(f.aura(),now+i,random);
            h.assertTrue(random.calls==4,"Fresh aura state cannot bypass the once-per-second owner phase");
            h.assertTrue(!corner.hasEffect(MobEffects.WEAKNESS)&&!f.owner.hasEffect(MobEffects.WEAKNESS),"Outside sphere and caster stay unaffected");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void reincarnationAddedLevelLastsExactlyTenSecondsOverExistingLongDebuffs(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.REINCARNATION)) {
            var target=f.cow(new Vec3(2,0,0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600,2));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,600,1));
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(true));
            h.assertTrue(target.getEffect(MobEffects.WEAKNESS).getAmplifier()==3&&target.getEffect(MobEffects.WEAKNESS).getDuration()==200,
                    "One added Weakness level lasts ten seconds, not the previous effect's thirty seconds");
            h.assertTrue(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==2&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==200,
                    "One added Slowness level also lasts exactly ten seconds");
            h.assertTrue(target.getHealth()==100,"Reincarnation never deals direct damage");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void allThreeBloodRealmsUseTheSameDrainRule(GameTestHelper h) {
        int expected=-1;
        for(var style:List.of(MethodDomainStyle.BLOOD_THORN,MethodDomainStyle.BLOOD_DRAIN,MethodDomainStyle.BLOOD_CROWN))try(var f=new Fixture(h,style)) {
            var target=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());target.setNoAi(true);target.setNoGravity(true);
            target.setPos(f.owner.position().add(3,0,0));NpcProfile.ensure(target,target.npcArchetype());
            var profile=target.getPersistentData().getCompound("ImmortalCultivationNpc");profile.putInt("Qi",1000);profile.putInt("MaxQi",1000);
            h.getLevel().addFreshEntity(target);f.spawned.add(target);fullBloom(f.aura(),target);
            int drained=1000-NpcProfile.qi(target,target.npcArchetype());if(expected<0)expected=drained;
            h.assertTrue(drained>0&&drained==expected,"Same caster strength drains identical Qi for each Blood Demon variant");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void windSeamsChangeDirectionBetweenLifetimesAndSplitWithinTheirVisibleWindow(GameTestHelper h) {
        Vec3 origin=new Vec3(0,100,0);
        for(int i=0;i<10;i++) {
            double tick=16-i*13;
            Vec3 start=MethodDomainEffects.windStart(origin,i,tick),end=MethodDomainEffects.windEnd(origin,i,tick);
            Vec3 next=MethodDomainEffects.windEnd(origin,i,tick+MethodDomainEffects.WIND_CYCLE_TICKS)
                    .subtract(MethodDomainEffects.windStart(origin,i,tick+MethodDomainEffects.WIND_CYCLE_TICKS));
            h.assertTrue(start.distanceTo(origin)<24&&end.distanceTo(origin)<24,"Seam remains near twenty-block domain");
            h.assertTrue(end.subtract(start).normalize().distanceTo(next.normalize())>.01,"Each new seam lifetime gets another deterministic direction");
        }
        h.assertTrue(MethodDomainEffects.windVisibility(10,4)==0&&MethodDomainEffects.windVisibility(10,16)>.5
                &&MethodDomainEffects.windVisibility(10,28)<.01,"Split current appears only during middle of parent lifetime");
        h.assertTrue(MethodDomainEffects.WIND_CYCLE_TICKS==32
                &&MethodDomainEffects.windStart(origin,0,4).distanceTo(MethodDomainEffects.windStart(origin,0,28))>20,
                "Wind repeatedly slices across the field within 1.6 seconds");
        for(int i=0;i<MethodDomainEffects.WIND_SEAMS;i++) {
            double wrap=MethodDomainEffects.WIND_CYCLE_TICKS-(i%10)*13;
            h.assertTrue(!MethodDomainEffects.sameWindSweep(i,wrap-1,wrap+1)
                    &&MethodDomainEffects.sameWindSweep(i,wrap+2,wrap+4),
                    "Fast swept collision and afterimages must never bridge a newly respawned blade");
        }
        h.succeed();
    }
    @GameTest(template="fate_test") public static void failedRollsProduceNeitherDebuffsNorDamage(GameTestHelper h) {
        for(var style:List.of(MethodDomainStyle.REINCARNATION,MethodDomainStyle.DEVOUR))try(var f=new Fixture(h,style)) {
            var target=f.cow(new Vec3(2,0,0));var random=new Rolls(false);
            MethodDomainEffects.tick(f.aura(),pulseTime(f),random);
            h.assertTrue(random.calls==1&&random.bound==100,"Correct independent one-percent probability denominator");
            h.assertTrue(target.getHealth()==100&&!target.hasEffect(MobEffects.WEAKNESS),"Failed probability roll has no gameplay effect");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void devouringDealsTenPercentOfCasterMaxQiNotVictimOrCurrentQi(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.DEVOUR)) {
            var target=f.cow(new Vec3(3,0,0));var random=new Rolls(true);long now=pulseTime(f);
            float expected=ModAttachments.getData(f.owner).maxQi()*.10F;
            float fullHealth=Math.max(10000,expected*2);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(fullHealth);target.setHealth(fullHealth);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withQi(1));
            int qi=ModAttachments.getData(f.owner).qi();
            MethodDomainEffects.tick(f.aura(),now,random);
            h.assertTrue(random.bound==100&&random.calls==1,"Devouring rolls 1% per target per second");
            h.assertTrue(Math.abs((fullHealth-target.getHealth())-expected)<.01,"Mortal victim takes ten percent of caster maximum Qi, independent of victim or caster remaining Qi");
            h.assertTrue(ModAttachments.getData(f.owner).qi()==qi,"Devouring proc is damage, not caster Qi spending");
            MethodDomainEffects.tick(f.aura(),now,random);
            h.assertTrue(random.calls==1&&Math.abs((fullHealth-target.getHealth())-expected)<.01,"Repeated same-tick calls cannot reroll or damage again");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void devouringNpcUsesItsOwnMaximumQi(GameTestHelper h) {
        var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var target=EntityType.COW.create(h.getLevel());
        try {
            caster.setNoAi(true);caster.setNoGravity(true);caster.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,64,3))));
            NpcProfile.ensure(caster,caster.npcArchetype());
            var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Method",MethodDomainStyle.DEVOUR.method);profile.putInt("MethodProficiency",500);
            profile.putInt("Qi",NpcProfile.maxQi(caster,caster.npcArchetype()));
            target.setNoAi(true);target.setNoGravity(true);target.setPos(caster.position().add(3,0,0));
            float expected=NpcProfile.maxQi(caster,caster.npcArchetype())*.10F,fullHealth=Math.max(10000,expected*2);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(fullHealth);target.setHealth(fullHealth);
            h.getLevel().addFreshEntity(caster);h.getLevel().addFreshEntity(target);
            caster.setTarget(target);caster.tickCount=20;
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
            var aura=QiDomainSystem.view(caster);
            h.assertTrue(aura!=null&&aura.domain()&&aura.method.equals(MethodDomainStyle.DEVOUR.method),"NPC opens its real Devouring domain");
            profile.putInt("Qi",1);long now=h.getLevel().getGameTime();
            MethodDomainEffects.tick(aura,now+Math.floorMod(caster.getId()-now,20),new Rolls(true));
            h.assertTrue(Math.abs((fullHealth-target.getHealth())-expected)<.01,"NPC damage uses caster's own maximum Qi, not its remaining Qi or victim's Qi");
            h.assertTrue(NpcProfile.qi(caster,caster.npcArchetype())==1,"NPC proc does not spend additional Qi");
        }finally{QiDomainSystem.stop(caster);caster.discard();target.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void domainRollsHonorMasterySuppressionAndProtectedEntities(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.REINCARNATION)) {
            var target=f.cow(new Vec3(2,0,0));var random=new Rolls(true);long now=pulseTime(f);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(f.aura().method,499)));
            MethodDomainEffects.tick(f.aura(),now,random);h.assertTrue(random.calls==0,"Below Great mastery no rolls");
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(f.aura().method,500)));
            f.aura().power=0;MethodDomainEffects.tick(f.aura(),now,random);h.assertTrue(random.calls==0,"Suppressed domain no rolls");
            f.aura().power=1;f.aura().percent=190;MethodDomainEffects.tick(f.aura(),now,random);h.assertTrue(random.calls==0,"Below 200% no domain rolls");
            f.aura().percent=200;
            var pet=EntityType.WOLF.create(h.getLevel());pet.setOwnerUUID(f.owner.getUUID());pet.setNoAi(true);pet.setNoGravity(true);
            pet.setPos(f.owner.position().add(3,0,0));h.getLevel().addFreshEntity(pet);f.spawned.add(pet);
            for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
                var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"protected"));player.setGameMode(mode);
                player.setPos(f.owner.position().add(4,0,0));h.getLevel().addNewPlayer(player);f.spawned.add(player);
            }
            MethodDomainEffects.tick(f.aura(),now,random);
            h.assertTrue(random.calls==1&&target.hasEffect(MobEffects.WEAKNESS)&&!pet.hasEffect(MobEffects.WEAKNESS),"Caster, own pet and protected modes receive no rolls");
        }h.succeed();
    }
    private static long fullBloom(QiDomainSystem.Aura aura,LivingEntity target) {
        long now=target.level().getGameTime();
        int pulses=(int)Math.ceil(BloodLilyBloom.bloomSeconds(target.getMaxHealth()));
        long end=now;
        // A natural phased pulse or a just-applied NPC health modifier can shift the first growth step.
        for(int i=0;i<pulses+2&&BloodLilyBloom.progress(target)<1;i++) {
            end=now+i*20;BloodLilyBloom.expose(aura,target,end);
        }
        return end;
    }
    @GameTest(template="fate_test") public static void bloodLilyLandedHitsAddPointOneToGrowthSpeed(GameTestHelper h) {
        var f=new Fixture(h,MethodDomainStyle.BLOOD_THORN);
        try {
            // This fixture lives across ticks: keep its active domain away from parallel arenas.
            f.owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,300,3))));
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            var target=f.cow(new Vec3(3,0,0));var control=f.cow(new Vec3(5,0,0));
            for(var actor:List.of(target,control)){actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);actor.setHealth(10000);}
            target.hurt(f.owner.damageSources().playerAttack(f.owner),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"First actual landed hit adds precisely 0.1x");
            target.invulnerableTime=0;target.hurt(f.owner.damageSources().playerAttack(f.owner),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"Same-tick secondary damage cannot count twice");
            h.runAfterDelay(2,()->{try {
                target.invulnerableTime=0;target.hurt(f.owner.damageSources().playerAttack(f.owner),1);
                h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.2)<.000001
                        &&BloodLilyBloom.growthMultiplier(f.owner,control)==1,"Next landed hit reaches 1.2x only on its own target");
                long now=h.getLevel().getGameTime();
                BloodLilyBloom.expose(f.aura(),target,now);BloodLilyBloom.expose(f.aura(),control,now);
                double fast=BloodLilyBloom.progress(target),slow=BloodLilyBloom.progress(control);
                h.assertTrue(Math.abs(fast/slow-1.2)<.0001,"Actual coverage grows 20 percent faster after two hits: fast="+fast+", base="+slow
                        +", multiplier="+BloodLilyBloom.growthMultiplier(f.owner,target)+", power="+f.aura().power+", distance="+f.owner.distanceTo(target));h.succeed();
            }finally{f.close();}});
        }catch(Throwable error){f.close();throw error;}
    }
    @GameTest(template="fate_test") public static void bloodLilyBlockedHitsAndPassiveDrainNeverAddSpeed(GameTestHelper h) {
        for(var style:List.of(MethodDomainStyle.BLOOD_THORN,MethodDomainStyle.BLOOD_DRAIN,MethodDomainStyle.BLOOD_CROWN))try(var f=new Fixture(h,style)) {
            var target=f.cow(new Vec3(3,0,0));target.setInvulnerable(true);
            target.hurt(f.owner.damageSources().indirectMagic(f.owner,f.owner),1);
            h.assertTrue(BloodLilyBloom.growthMultiplier(f.owner,target)==1,"Blocked attacks grant no speed stack");
            target.setInvulnerable(false);QiDomainDamage.hurt(f.owner,target,1,false);
            h.assertTrue(BloodLilyBloom.growthMultiplier(f.owner,target)==1,"Passive lily/domain damage cannot accelerate itself");
            target.invulnerableTime=0;target.hurt(f.owner.damageSources().indirectMagic(f.owner,f.owner),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"Actual spell hit accelerates every Blood Demon tier");
            f.aura().percent=190;target.invulnerableTime=0;target.hurt(f.owner.damageSources().indirectMagic(f.owner,f.owner),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"No new stacks outside active domain intensity");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void bloodLilyHitCountsAreCasterSpecificSavedAndClearWithWilt(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BLOOD_DRAIN);var other=new Fixture(h,MethodDomainStyle.BLOOD_CROWN)) {
            other.owner.setPos(f.owner.position());var target=f.cow(new Vec3(3,0,0));
            target.hurt(f.owner.damageSources().indirectMagic(f.owner,f.owner),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001
                    &&BloodLilyBloom.growthMultiplier(other.owner,target)==1,"One caster's hit cannot grant another caster stacks");
            BloodLilyBloom.restore(target);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"Pending hit count survives reload before initial coverage");
            long now=h.getLevel().getGameTime();BloodLilyBloom.expose(other.aura(),target,now);
            BloodLilyBloom.expose(f.aura(),target,now);
            h.assertTrue(Math.abs(BloodLilyBloom.progress(target)-1.1/BloodLilyBloom.bloomSeconds(target.getMaxHealth()))<.000001,
                    "Overlapping domains use best multiplier, never sum two base growth pulses");
            BloodLilyBloom.restore(target);h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(f.owner,target)-1.1)<.000001,"Growing flowers retain saved hit count");
            BloodLilyBloom.update(target,now+100000);
            h.assertTrue(BloodLilyBloom.progress(target)==0&&BloodLilyBloom.growthMultiplier(f.owner,target)==1,"Fully wilted flowers reset hit bonus");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void npcBloodLilyLandedAttackAlsoAddsPointOne(GameTestHelper h) {
        var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var target=EntityType.COW.create(h.getLevel());
        try {
            caster.setNoAi(true);caster.setNoGravity(true);caster.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,220,3))));
            NpcProfile.ensure(caster,caster.npcArchetype());var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Method",MethodDomainStyle.BLOOD_THORN.method);profile.putInt("MethodProficiency",500);
            profile.getCompound("MethodProficiencies").putInt(MethodDomainStyle.BLOOD_THORN.method,500);profile.putInt("Qi",100000);
            caster.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            target.setNoAi(true);target.setNoGravity(true);target.setPos(caster.position().add(3,0,0));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);
            h.getLevel().addFreshEntity(caster);h.getLevel().addFreshEntity(target);caster.setTarget(target);caster.tickCount=21;
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
            target.hurt(caster.damageSources().mobAttack(caster),1);
            h.assertTrue(Math.abs(BloodLilyBloom.growthMultiplier(caster,target)-1.1)<.000001,"Actual NPC attack adds the same 0.1x bonus");
        }finally{QiDomainSystem.stop(caster);BloodLilyBloom.clear(target);caster.discard();target.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void bloodLiliesDrainOnlyAfterFullCoverage(GameTestHelper h) {
        for(var style:List.of(MethodDomainStyle.BLOOD_THORN,MethodDomainStyle.BLOOD_DRAIN,MethodDomainStyle.BLOOD_CROWN))try(var f=new Fixture(h,style)) {
            var victim=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());victim.setNoAi(true);victim.setNoGravity(true);
            victim.setPos(f.owner.position().add(4,0,0));NpcProfile.ensure(victim,victim.npcArchetype());
            var profile=victim.getPersistentData().getCompound("ImmortalCultivationNpc");profile.putInt("Qi",1000);profile.putInt("MaxQi",1000);
            h.getLevel().addFreshEntity(victim);f.spawned.add(victim);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withQi(1));f.owner.setHealth(1);
            float health=victim.getHealth();long now=h.getLevel().getGameTime();
            BloodLilyBloom.expose(f.aura(),victim,now);
            h.assertTrue(BloodLilyBloom.progress(victim)>0&&BloodLilyBloom.progress(victim)<1&&victim.getHealth()==health
                    &&NpcProfile.qi(victim,victim.npcArchetype())==1000&&f.owner.getHealth()==1,"Partial flowers never drain HP or Qi: "+style);
            long full=fullBloom(f.aura(),victim);int after=NpcProfile.qi(victim,victim.npcArchetype());
            h.assertTrue(BloodLilyBloom.progress(victim)==1&&after<1000&&victim.getHealth()<health&&f.owner.getHealth()>1
                    &&ModAttachments.getData(f.owner).qi()==1+1000-after,"Full flower returns actual HP and all removed Qi");
            BloodLilyBloom.expose(f.aura(),victim,full);
            h.assertTrue(NpcProfile.qi(victim,victim.npcArchetype())==after,"Repeated contact cannot duplicate drain");
            victim.setPos(f.owner.position().add(0,25,0));BloodLilyBloom.expose(f.aura(),victim,full+20);
            h.assertTrue(NpcProfile.qi(victim,victim.npcArchetype())==after,"Leaving domain immediately stops transfer");
            h.assertTrue(!ColdTideFreeze.isFrozen(victim)&&victim.getActiveEffects().isEmpty(),"Blood lilies never immobilize or add ice effects");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void bloodLilyProgressUsesMaximumHpPersistsAndFadesSlowly(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BLOOD_THORN)) {
            var a=f.cow(new Vec3(2,0,0));var b=f.cow(new Vec3(4,0,0));b.setHealth(5);
            long now=h.getLevel().getGameTime();BloodLilyBloom.expose(f.aura(),a,now);BloodLilyBloom.expose(f.aura(),b,now);
            float progress=BloodLilyBloom.progress(a);
            h.assertTrue(progress==BloodLilyBloom.progress(b),"Wounds never accelerate flowers");
            BloodLilyBloom.restore(a);h.assertTrue(BloodLilyBloom.progress(a)==progress,"Saved target state survives reload");
            BloodLilyBloom.update(a,now+61);float faded=BloodLilyBloom.progress(a);
            h.assertTrue(faded>0&&faded<progress,"Flowers slowly recede outside exposure");
            BloodLilyBloom.expose(f.aura(),a,now+80);
            h.assertTrue(BloodLilyBloom.progress(a)>progress,"Re-entry resumes from remaining coverage");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void bloodLiliesRespectMasteryReleaseRangeAndProtectedPlayers(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BLOOD_DRAIN)) {
            var cow=f.cow(new Vec3(3,0,0));long now=h.getLevel().getGameTime();
            f.aura().percent=190;BloodLilyBloom.expose(f.aura(),cow,now);h.assertTrue(BloodLilyBloom.progress(cow)==0,"No sub-domain lily growth");
            f.aura().percent=200;ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(f.aura().method,499)));
            BloodLilyBloom.expose(f.aura(),cow,now);h.assertTrue(BloodLilyBloom.progress(cow)==0,"Unmastered arts cannot grow flowers");
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(f.aura().method,500)));
            cow.setPos(f.owner.position().add(0,25,0));BloodLilyBloom.expose(f.aura(),cow,now);h.assertTrue(BloodLilyBloom.progress(cow)==0,"Sphere radius enforced");
            for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
                var protectedPlayer=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"lily-protected"));
                protectedPlayer.setGameMode(mode);protectedPlayer.setPos(f.owner.position());f.spawned.add(protectedPlayer);
                BloodLilyBloom.expose(f.aura(),protectedPlayer,now);h.assertTrue(BloodLilyBloom.progress(protectedPlayer)==0,"Protected mode never covered");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void bloodLiliesShareGrowthAndNeverHealFromBlockedDamage(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BLOOD_CROWN);var other=new Fixture(h,MethodDomainStyle.BLOOD_THORN)) {
            other.owner.setPos(f.owner.position());var target=f.cow(new Vec3(3,0,0));long now=h.getLevel().getGameTime();
            BloodLilyBloom.expose(f.aura(),target,now);float progress=BloodLilyBloom.progress(target);
            BloodLilyBloom.expose(other.aura(),target,now+1);h.assertTrue(BloodLilyBloom.progress(target)==progress,"Two domains cannot multiply buildup");
            target.setInvulnerable(true);f.owner.setHealth(1);fullBloom(f.aura(),target);
            h.assertTrue(f.owner.getHealth()==1&&target.getHealth()==100,"Immune target produces no stolen HP");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void windSeamsCutOnlyOnContactAndLiftVictim(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.WIND)) {
            long now=240;Vec3 start=MethodDomainEffects.windStart(f.owner.position(),0,now),end=MethodDomainEffects.windEnd(f.owner.position(),0,now);
            var target=f.cow(start.lerp(end,.5).subtract(f.owner.position()));var far=f.cow(new Vec3(0,0,30));
            float expected=QiDomainDamage.amount(f.owner,f.aura().power)*.5F;
            target.setDeltaMovement(Vec3.ZERO);MethodDomainEffects.windContacts(f.aura(),now);
            h.assertTrue(target.getHealth()<100&&target.getDeltaMovement().y>0,"Real seam contact cuts and applies upward pressure");
            h.assertTrue(Math.abs(100-target.getHealth()-expected)<.01F,"Player wind contact deals half the previous damage: "+(100-target.getHealth())+" expected "+expected);
            h.assertTrue(far.getHealth()==100,"Distant entities outside domain never receive wind damage");
            float hp=target.getHealth();MethodDomainEffects.windContacts(f.aura(),now);
            h.assertTrue(target.getHealth()==hp,"Overlapping seams share per-owner damage cooldown");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void maximumQiUsesSavedBeastRealmAndNpcResources(GameTestHelper h) {
        var beast=ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
        beast.setBeastRealm(4);
        h.assertTrue(MethodDomainEffects.maxQi(beast)==CultivationLevels.getLevelDef(beast.beastCultivationLevel()).maxQi(),"Devouring uses beast's saved realm");
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());NpcProfile.ensure(npc,npc.npcArchetype());
        h.assertTrue(MethodDomainEffects.maxQi(npc)==NpcProfile.maxQi(npc,npc.npcArchetype()),"NPC maximum Qi comes from its own profile");
        beast.discard();npc.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void thunderChainsAutonomouslyWithoutAttacksAndRespectsDistance(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.STORM)) {
            var first=f.cow(new Vec3(2,0,0));var second=f.cow(new Vec3(5,0,0));
            var isolated=f.cow(new Vec3(-15,0,0));var outside=f.cow(new Vec3(21,0,0));
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(first.getHealth()<100&&second.getHealth()<100,"Close enemies arc to each other without caster attacking");
            h.assertTrue(isolated.getHealth()==100&&outside.getHealth()==100,"No long-distance chain and no outside-domain damage");
            h.assertTrue(f.aura().domainExposure.size()==3&&f.aura().attackMode==0,"In-domain enemies charge passively, including isolated targets");
            float hp=second.getHealth();MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(second.getHealth()==hp,"Repeated same-second calls cannot multiply discharge damage");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void chargedIsolatedEnemyDischargesAfterExposure(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.STORM)) {
            var target=f.cow(new Vec3(10,0,0));long now=pulseTime(f);
            for(int i=0;i<6;i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Rolls(false));
            h.assertTrue(target.getHealth()<100&&f.aura().domainExposure.get(target.getUUID())>=3,
                    "Charged isolated enemy receives periodic cloud discharge without an attack");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void mountainPressureSlowsAndPressesDownWithoutMoteDamage(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.MOUNTAIN)) {
            var target=f.cow(new Vec3(7,0,0));target.setDeltaMovement(.3,.1,.2);
            var outside=f.cow(new Vec3(19,0,19));MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==0,
                    "Pressure subtly slows targets throughout the domain");
            h.assertTrue(target.getDeltaMovement().y<.1&&target.getHealth()==100,"Downward pressure changes movement without generic contact damage");
            h.assertTrue(!outside.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!f.owner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Sphere boundary and caster protected");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void abyssStacksWaterFromMovementAndGentlyHealsOncePerSecond(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.COLD_TIDE)) {
            var target=f.cow(new Vec3(5,0,0));f.owner.setHealth(f.owner.getMaxHealth()/2);
            float before=f.owner.getHealth();long now=pulseTime(f);
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(target.getTicksFrozen()==0&&!target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&TideWaterStacks.stacks(target)==0,"Stationary water-domain target gets neither ice nor unearned water stacks");
            float healed=f.owner.getHealth();h.assertTrue(healed>before&&healed-before<=4.001,"Existing gentle water healing remains capped");
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));h.assertTrue(f.owner.getHealth()==healed,"Cannot heal twice in one pulse");
            for(int i=1;i<=8;i++){target.setPos(target.position().add(i%2==0?-1:1,0,0));TideWaterStacks.update(target,now+i);}
            h.assertTrue(TideWaterStacks.stacks(target)==8&&ColdTideFreeze.progress(target)==0&&target.getTicksFrozen()==0,"Eight actual blocks of movement give eight Damp stacks and no ice lock");
            target.setTicksFrozen(220);MethodDomainEffects.tick(f.aura(),now+20,new Rolls(false));
            h.assertTrue(target.getTicksFrozen()==220,"Water domain preserves unrelated freezing from other spells");
            target.setPos(target.position().add(0,25,0));MethodDomainEffects.tick(f.aura(),now+40,new Rolls(false));
            TideWaterStacks.update(target,now+41);
            h.assertTrue(!f.aura().domainExposure.containsKey(target.getUUID())&&TideWaterStacks.stacks(target)==8,"Leaving stops exposure while existing Damp keeps its ordinary duration");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void newDomainsRequireMasteryAndProtectPetsCreativeAndSpectator(GameTestHelper h) {
        for(var style:List.of(MethodDomainStyle.STORM,MethodDomainStyle.MOUNTAIN,MethodDomainStyle.COLD_TIDE))try(var f=new Fixture(h,style)) {
            var target=f.cow(new Vec3(3,0,0));long now=pulseTime(f);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(style.method,499)));
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));h.assertTrue(f.aura().domainExposure.isEmpty(),"Mastery gate: "+style);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withMethodProficiencies(Map.of(style.method,500)));
            f.aura().power=0;MethodDomainEffects.tick(f.aura(),now,new Rolls(false));h.assertTrue(f.aura().domainExposure.isEmpty(),"Suppression gate: "+style);
            f.aura().power=1;f.aura().percent=190;MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.isEmpty(),"Intensity gate: "+style);f.aura().percent=200;
            var pet=EntityType.WOLF.create(h.getLevel());pet.setOwnerUUID(f.owner.getUUID());pet.setNoAi(true);pet.setNoGravity(true);
            pet.setPos(f.owner.position().add(4,0,0));h.getLevel().addFreshEntity(pet);f.spawned.add(pet);
            for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
                var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"protected"));player.setGameMode(mode);
                player.setPos(f.owner.position().add(5,0,0));h.getLevel().addNewPlayer(player);f.spawned.add(player);
            }
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.size()==1&&f.aura().domainExposure.containsKey(target.getUUID()),"Only eligible enemy receives exposure: "+style);
            h.assertTrue(!pet.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&pet.getTicksFrozen()==0,"Pet unaffected: "+style);
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void continuousDomainTargetsAreCappedAndMethodChangeClearsExposure(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.COLD_TIDE)) {
            for(int i=0;i<32;i++)f.cow(new Vec3(2+i%8,0,i/8));
            long now=pulseTime(f);MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.size()==MethodDomainEffects.MAX_CONTINUOUS_TARGETS,"Crowd processing has fixed target bound");
            f.aura().method=MethodDomainStyle.BREATH.method;
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withActiveCultivationMethod(MethodDomainStyle.BREATH.method)
                    .withMethodProficiencies(Map.of(MethodDomainStyle.BREATH.method,500)));
            MethodDomainEffects.tick(f.aura(),now+20,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.isEmpty(),"Changing method cannot retain old charged/frost targets");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void npcMountainAndAbyssUseSameAutonomousDomainLogic(GameTestHelper h) {
        for(var style:List.of(MethodDomainStyle.STORM,MethodDomainStyle.MOUNTAIN,MethodDomainStyle.COLD_TIDE)) {
            var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var target=EntityType.COW.create(h.getLevel());
            try {
                caster.setNoAi(true);caster.setNoGravity(true);caster.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,64,3))));
                NpcProfile.ensure(caster,caster.npcArchetype());var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
                profile.putString("Method",style.method);profile.putInt("MethodProficiency",500);profile.putInt("Qi",100000);
                target.setNoAi(true);target.setNoGravity(true);target.setPos(caster.position().add(3,0,0));
                h.getLevel().addFreshEntity(caster);h.getLevel().addFreshEntity(target);caster.setTarget(target);caster.tickCount=20;
                QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
                var aura=QiDomainSystem.view(caster);h.assertTrue(aura!=null&&aura.domain(),"NPC activates domain: "+style);
                long now=h.getLevel().getGameTime();now+=Math.floorMod(caster.getId()-now,20);
                MethodDomainEffects.tick(aura,now,new Rolls(false));
                h.assertTrue(aura.domainExposure.containsKey(target.getUUID()),"NPC applies continuous method effect: "+style);
                if(style==MethodDomainStyle.MOUNTAIN)h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"NPC pressure slows victim: "+style);
                if(style==MethodDomainStyle.COLD_TIDE) {
                    target.setPos(target.position().add(1,0,0));TideWaterStacks.update(target,now+1);
                    h.assertTrue(TideWaterStacks.stacks(target)>0&&ColdTideFreeze.progress(target)==0,"NPC abyss stacks actual Damp from movement without ice");
                }
                QiDomainSystem.stop(caster);h.assertTrue(QiDomainSystem.view(caster)==null,"Stopping releases all per-domain state");
            }finally{QiDomainSystem.stop(caster);caster.discard();target.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void reducingReleaseDiscardsOldChargeAndFrostExposure(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.COLD_TIDE)) {
            f.cow(new Vec3(3,0,0));MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(!f.aura().domainExposure.isEmpty(),"Active domain records exposure");
            f.aura().percent=190;QiDomainSystem.tickAll();
            h.assertTrue(f.aura().domainExposure.isEmpty(),"Reducing below domain intensity drops old exposure immediately");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void solarPillarsUseCasterMaxHpForAllFivePercentagesRegardlessOfTargetHp(GameTestHelper h) {
        for(float percent:new float[]{10,25,50,75,100})try(var f=new Fixture(h,MethodDomainStyle.SOLAR)) {
            var target=f.cow(new Vec3(5,0,0));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
            var tougher=f.cow(new Vec3(7,0,0));tougher.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);tougher.setHealth(10000);
            f.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);f.owner.setHealth(15);
            ModAttachments.setData(f.owner,CultivationMethods.withSolarPower(ModAttachments.getData(f.owner),percent));
            float casterMaxHp=f.owner.getMaxHealth();
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            float expected=percent*.001F*casterMaxHp;
            h.assertTrue(Math.abs(AdvancedMethodDomains.solarDamage(percent,2000)-percent*2)<.001,"All five percentage examples use caster HP");
            h.assertTrue(Math.abs(1000-target.getHealth()-expected)<.01&&Math.abs(10000-tougher.getHealth()-expected)<.01,
                    "Sun Energy "+percent+"% uses caster max HP="+casterMaxHp+", expected="+expected+", actual="+(1000-target.getHealth())+"/"+(10000-tougher.getHealth())+", max after="+f.owner.getMaxHealth());
            h.assertTrue(f.owner.getHealth()==15,"Wounded caster's current HP does not change pillar damage");
            h.assertTrue(target.isOnFire()&&tougher.isOnFire(),"Solar eruption ignites both targets");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void zeroSunProducesNoDamageAndSuppressionScalesExactFormula(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.SOLAR)) {
            var target=f.cow(new Vec3(4,0,0));long now=pulseTime(f);
            f.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            ModAttachments.setData(f.owner,CultivationMethods.withSolarPower(ModAttachments.getData(f.owner),0));
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(target.getHealth()==100&&!target.isOnFire(),"Zero Sun Energy does not emit damaging pillars");
            ModAttachments.setData(f.owner,CultivationMethods.withSolarPower(ModAttachments.getData(f.owner),50));
            float casterMaxHp=f.owner.getMaxHealth();
            f.aura().power=.42F;MethodDomainEffects.tick(f.aura(),now+20,new Rolls(false));
            h.assertTrue(Math.abs(100-target.getHealth()-casterMaxHp*.05F*.42F)<.01,
                    "Suppression scales five percent of caster max HP="+casterMaxHp+", actual damage="+(100-target.getHealth()));
            h.assertTrue(AdvancedMethodDomains.solarDamage(Float.NaN,100)==0&&AdvancedMethodDomains.solarDamage(200,100)==10,
                    "Invalid percentages rejected and resource is bounded");
            h.assertTrue(AdvancedMethodDomains.solarDamage(50,Float.POSITIVE_INFINITY)==0&&AdvancedMethodDomains.solarDamage(-10,100)==0
                            &&AdvancedMethodDomains.solarDamage(50,-100)==0,"Invalid caster HP and negative values cannot create damage");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void winterBuildsFromFeetToFullCoverageAndProgressiveSlowing(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.ICE)) {
            var target=f.cow(new Vec3(17,0,0));long now=pulseTime(f);
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.get(target.getUUID())==1&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==0,
                    "Thin initial frost starts with mild slowing throughout domain");
            for(int i=1;i<20;i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Rolls(false));
            h.assertTrue(f.aura().domainExposure.get(target.getUUID())==8&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==3,
                    "Hundred-maximum-HP target reaches full coverage after twenty seconds, matching lilies");
            h.assertTrue(target.getHealth()==100&&target.getTicksFrozen()==140,"Winter builds capped freezing instead of generic contact damage");
            h.assertTrue(ColdTideFreeze.isFrozen(target),"Full visible winter coating is backed by the actual five-minute action lock");
            target.setPos(f.owner.position().add(0,25,0));MethodDomainEffects.tick(f.aura(),now+400,new Rolls(false));
            h.assertTrue(!f.aura().domainExposure.containsKey(target.getUUID())&&ColdTideFreeze.isFrozen(target),"Leaving removes domain tracking but preserves the full action lock");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void winterUsesTheSameMaximumHpCurveAsLilyBaseGrowth(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.ICE)) {
            var healthy=f.cow(new Vec3(3,0,0));var wounded=f.cow(new Vec3(5,0,0));var tough=f.cow(new Vec3(7,0,0));
            wounded.setHealth(1);tough.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);tough.setHealth(400);
            long now=pulseTime(f);for(int i=0;i<5;i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Rolls(false));
            float normal=ColdTideFreeze.progress(healthy),hurt=ColdTideFreeze.progress(wounded),strong=ColdTideFreeze.progress(tough);
            h.assertTrue(Math.abs(normal-5/BloodLilyBloom.bloomSeconds(100))<.000001,"Winter uses exactly the lily maximum-HP base formula");
            h.assertTrue(normal==hurt&&Math.abs(normal-2*strong)<.000001,"Wounds do not change ice growth; four times maximum HP takes twice as long");
            h.assertTrue(ColdTideFreeze.freezeSeconds(100)==BloodLilyBloom.bloomSeconds(100)
                    &&ColdTideFreeze.freezeSeconds(400)==BloodLilyBloom.bloomSeconds(400),"Cold-Tide already shares that same base curve");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void winterAttacksDoNotAddLilyHitAcceleration(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.ICE)) {
            var attacked=f.cow(new Vec3(3,0,0));var control=f.cow(new Vec3(5,0,0));long now=pulseTime(f);
            MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            attacked.hurt(f.owner.damageSources().indirectMagic(f.owner,f.owner),1);
            MethodDomainEffects.tick(f.aura(),now+20,new Rolls(false));
            h.assertTrue(attacked.getHealth()<100&&ColdTideFreeze.progress(attacked)==ColdTideFreeze.progress(control),
                    "An actual landed hit never changes maximum-HP-based ice growth");
            h.assertTrue(BloodLilyBloom.growthMultiplier(f.owner,attacked)==1,"Ice does not inherit lily attack stacks");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void winterReleaseGatePreservesTargetOwnedPartialIce(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.ICE)) {
            var target=f.cow(new Vec3(3,0,0));long now=pulseTime(f);MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            float before=ColdTideFreeze.progress(target);h.assertTrue(before>0,"Mastered ice domain records precise target-owned coverage");
            f.aura().percent=190;QiDomainSystem.tickAll();
            h.assertTrue(f.aura().domainExposure.isEmpty()&&ColdTideFreeze.progress(target)==before,"Dropping below domain intensity ends exposure without erasing partial ice");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=70) public static void winterFullCoverageStopsAnAwakeBeastAndItsPendingMove(GameTestHelper h) {
        var f=new Fixture(h,MethodDomainStyle.ICE);
        var beast=ModEntities.FROSTSPINE_CAVE_BEAR.get().create(h.getLevel());
        try {
            f.owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,650,3))));
            var victim=f.cow(new Vec3(12,0,0));victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);victim.setHealth(10000);
            beast.setPos(f.owner.position().add(8,0,0));beast.setNoGravity(true);beast.setNoAi(false);h.getLevel().addFreshEntity(beast);f.spawned.add(beast);beast.setTarget(victim);
            var begin=beast.getClass().getDeclaredMethod("beginAction",byte.class,int.class,LivingEntity.class);begin.setAccessible(true);
            begin.invoke(beast,com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity.ACTION_ICE_SPINE_LINE,48,victim);
            long now=pulseTime(f);for(int i=0;i<1000&&!ColdTideFreeze.isFrozen(beast);i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Rolls(false));
            h.assertTrue(ColdTideFreeze.isFrozen(beast)&&ColdTideFreeze.progress(beast)==1,"Actual winter domain starts the full beast lock");
            h.assertTrue(!beast.isNoAi()&&beast.getActionTicks()==48,"AI remains enabled and a real beast move was pending before freezing");
            QiDomainSystem.stop(f.owner);int clock=beast.tickCount;Vec3 place=beast.position();float hp=victim.getHealth();
            beast.setDeltaMovement(.5,.5,.5);beast.doHurtTarget(victim);
            h.runAfterDelay(25,()->{try {
                h.assertTrue(ColdTideFreeze.isFrozen(beast)&&beast.tickCount==clock&&beast.position().distanceToSqr(place)<.000001,"Normal world ticks cannot advance or move the awake frozen beast after leaving the domain");
                h.assertTrue(beast.getActionTicks()==48&&victim.getHealth()==hp,"Pending spine attack never advances or deals damage");
                h.assertTrue(com.example.immortal_cultivation_mod.entity.BeastDazeControl.blocksMoves(beast),"Reactive beast move entry points also reject full freeze");h.succeed();
            }finally{f.close();}});
        }catch(Throwable error){f.close();throw new RuntimeException(error);}
    }
    @GameTest(template="fate_test") public static void winterPartialIcePersistsAndBothSlimeTypesStayImmune(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.ICE)) {
            var target=f.cow(new Vec3(3,0,0));long now=pulseTime(f);
            for(int i=0;i<5;i++)MethodDomainEffects.tick(f.aura(),now+i*20,new Rolls(false));
            float before=ColdTideFreeze.progress(target);target.setPos(f.owner.position().add(0,25,0));
            MethodDomainEffects.tick(f.aura(),now+300,new Rolls(false));ColdTideFreeze.update(target,now+300);
            float left=ColdTideFreeze.progress(target);h.assertTrue(left>0&&left<before,"Partial winter ice melts slowly outside");
            target.setPos(f.owner.position().add(3,0,0));MethodDomainEffects.tick(f.aura(),now+320,new Rolls(false));
            h.assertTrue(ColdTideFreeze.progress(target)>left&&!ColdTideFreeze.isFrozen(target),"Reentry resumes leftover partial ice without prematurely locking");
            int pulse=17;for(var type:List.of(EntityType.SLIME,EntityType.MAGMA_CUBE)) {
                var slime=type.create(h.getLevel());slime.setNoAi(true);slime.setNoGravity(true);slime.setPos(f.owner.position().add(5,0,0));
                h.getLevel().addFreshEntity(slime);f.spawned.add(slime);
                for(int i=0;i<13;i++)MethodDomainEffects.tick(f.aura(),now+(pulse++)*20,new Rolls(false));
                h.assertTrue(ColdTideFreeze.progress(slime)==0&&slime.getTicksFrozen()==0&&!slime.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Winter preserves slime-family immunity");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void boneBellSuppressesOnlyStrictlyWeakerInnateBodies(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BONE)) {
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withAddedPhysicalAttack(20));
            double strength=AdvancedMethodDomains.bodyStrength(f.owner);
            var modifier=new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","test_weapon_strength"),1000,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            f.owner.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(modifier);
            h.assertTrue(AdvancedMethodDomains.bodyStrength(f.owner)==strength,"Held/equipment-style attack modifiers do not inflate body strength");
            for(double value:new double[]{strength-1,strength,strength+20}) {
                var target=EntityType.ZOMBIE.create(h.getLevel());target.setNoAi(true);target.setNoGravity(true);
                target.setPos(f.owner.position().add(4+f.spawned.size(),0,0));target.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(value-target.getMaxHealth()/20.0);
                h.getLevel().addFreshEntity(target);f.spawned.add(target);
            }
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(f.spawned.getFirst().hasEffect(ModEffects.BONE_BELL),"Weaker body receives suppression");
            h.assertTrue(!f.spawned.get(1).hasEffect(ModEffects.BONE_BELL)&&!f.spawned.get(2).hasEffect(ModEffects.BONE_BELL),
                    "Equal and stronger bodies remain completely unaffected");
            h.assertTrue(f.spawned.stream().allMatch(e->e.getDeltaMovement().lengthSqr()==0),"Physical pressure does not apply gravity or launch targets");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void boneTemperingHealthTrainingActuallyIncreasesSuppressionStrength(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.BONE)) {
            com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(f.owner);
            double before=AdvancedMethodDomains.bodyStrength(f.owner);
            float hp=f.owner.getMaxHealth();f.owner.setHealth(hp);
            var target=EntityType.ZOMBIE.create(h.getLevel());target.setNoAi(true);target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            target.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(f.owner.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
            target.setHealth(hp);target.setPos(f.owner.position().add(4,0,0));h.getLevel().addFreshEntity(target);f.spawned.add(target);
            h.assertTrue(AdvancedMethodDomains.bodyStrength(target)==before,"Untrained equally durable bodies start equal");
            int casts=(int)Math.ceil(hp*.05/10.0);
            for(int i=0;i<casts;i++)com.example.immortal_cultivation_mod.spell.BodyCultivation.cast(f.owner);
            h.assertTrue(f.owner.getMaxHealth()>hp&&AdvancedMethodDomains.bodyStrength(f.owner)>before,"Real bone-tempering HP gain counts as body strength");
            double trained=AdvancedMethodDomains.bodyStrength(f.owner);f.owner.setHealth(1);
            h.assertTrue(AdvancedMethodDomains.bodyStrength(f.owner)==trained,"Current wounds do not erase trained body strength");
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(target.hasEffect(ModEffects.BONE_BELL),"Real body training turns equal target into a suppressible weaker body");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=70) public static void boneBellSlowsRealBeastsSurvivesSlownessCleanseAndExpires(GameTestHelper h) {
        var f=new Fixture(h,MethodDomainStyle.BONE);
        var beast=ModEntities.PHANTOM_VEIL_FOX.get().create(h.getLevel());
        try {
            beast.setNoAi(true);beast.setNoGravity(true);beast.setPos(f.owner.position().add(4,0,0));
            h.getLevel().addFreshEntity(beast);f.spawned.add(beast);
            ModAttachments.setData(f.owner,ModAttachments.getData(f.owner).withAddedPhysicalAttack(100000));
            double speed=beast.getAttributeValue(Attributes.MOVEMENT_SPEED);
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            beast.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,1,false,false));
            beast.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            h.assertTrue(beast.hasEffect(ModEffects.BONE_BELL)&&Math.abs(beast.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed*.7)<.00001,
                    "Real fox remains thirty percent slower after its ordinary Slowness cleanse");
            QiDomainSystem.stop(f.owner);
            h.runAfterDelay(42,()->{try {
                h.assertTrue(!beast.hasEffect(ModEffects.BONE_BELL)&&beast.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","bone_bell"))==null,
                        "Leaving domain removes suppression and its speed modifier after two seconds");h.succeed();
            }finally{f.close();}});
        }catch(Throwable error){f.close();throw error;}
    }
    @GameTest(template="fate_test") public static void ashesBurnAndReturnOnlyRealDamageAsVitality(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.RENEWAL_FIRE)) {
            var target=f.cow(new Vec3(8,0,0));f.owner.setHealth(f.owner.getMaxHealth()/2);float before=f.owner.getHealth();
            long now=pulseTime(f);MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            float damage=100-target.getHealth();
            h.assertTrue(damage>0&&target.isOnFire(),"Automatic flame contact burns without caster attacking");
            h.assertTrue(Math.abs(f.owner.getHealth()-before-damage*.05F)<.001,"Returning vitality equals five percent of actual HP damage");
            float hp=f.owner.getHealth();MethodDomainEffects.tick(f.aura(),now,new Rolls(false));
            h.assertTrue(f.owner.getHealth()==hp,"Cannot repeatedly extract vitality in one pulse");
        }
        try(var f=new Fixture(h,MethodDomainStyle.RENEWAL_FIRE)) {
            var target=f.cow(new Vec3(3,0,0));target.setInvulnerable(true);f.owner.setHealth(f.owner.getMaxHealth()/2);float hp=f.owner.getHealth();
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(false));
            h.assertTrue(target.getHealth()==100&&f.owner.getHealth()==hp,"Blocked damage cannot create free vitality");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void netherHasNoPassiveDamageDebuffOrHealing(GameTestHelper h) {
        try(var f=new Fixture(h,MethodDomainStyle.NETHER)) {
            var target=f.cow(QiDomainSystem.motePosition(f.owner,0,h.getLevel().getGameTime()).subtract(f.owner.position()));
            f.owner.setHealth(f.owner.getMaxHealth()/2);float hp=f.owner.getHealth();
            MethodDomainEffects.tick(f.aura(),pulseTime(f),new Rolls(true));QiDomainSystem.tickAll();
            h.assertTrue(target.getHealth()==100&&target.getActiveEffects().isEmpty()&&!target.isOnFire(),"Nether does not retain old mote wither/weakness or contact damage");
            h.assertTrue(f.owner.getHealth()==hp&&f.aura().domainExposure.isEmpty(),"Nether grants no passive healing or suppression");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void npcSolarUsesItsSavedCurrentSunPercentage(GameTestHelper h) {
        var caster=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var target=EntityType.COW.create(h.getLevel());
        try {
            caster.setNoAi(true);caster.setNoGravity(true);caster.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,64,3))));
            NpcProfile.ensure(caster,caster.npcArchetype());var profile=caster.getPersistentData().getCompound("ImmortalCultivationNpc");
            profile.putString("Method",MethodDomainStyle.SOLAR.method);profile.putInt("MethodProficiency",500);profile.putInt("Qi",100000);profile.putFloat("SolarPower",25);
            target.setNoAi(true);target.setNoGravity(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);
            target.setPos(caster.position().add(3,0,0));h.getLevel().addFreshEntity(caster);h.getLevel().addFreshEntity(target);caster.setTarget(target);caster.tickCount=20;
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(caster));
            var aura=QiDomainSystem.view(caster);long now=h.getLevel().getGameTime();now+=Math.floorMod(caster.getId()-now,20);
            float casterMaxHp=caster.getMaxHealth();caster.setHealth(25);
            MethodDomainEffects.tick(aura,now,new Rolls(false));
            h.assertTrue(Math.abs(10000-target.getHealth()-casterMaxHp*.025F)<.01&&AdvancedMethodDomains.sunEnergy(caster)==25,
                    "NPC pillar uses saved 25-percent Sun Energy and its own maximum HP, not current HP or victim HP");
        }finally{QiDomainSystem.stop(caster);caster.discard();target.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void breathingAndClearHeartHaveNoSpecialGameplayEffect(GameTestHelper h) {
        long delay=20-h.getLevel().getGameTime()%20;
        h.runAfterDelay(delay,()->{
            for(var style:List.of(MethodDomainStyle.BREATH,MethodDomainStyle.CLEAR))try(var f=new Fixture(h,style)) {
                var target=f.cow(QiDomainSystem.motePosition(f.owner,0,h.getLevel().getGameTime()).subtract(f.owner.position()));
                f.owner.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));
                f.owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0));
                target.setDeltaMovement(Vec3.ZERO);QiDomainSystem.tickAll();
                h.assertTrue(target.getHealth()==100&&!target.hasEffect(MobEffects.WEAKNESS)&&target.getDeltaMovement().lengthSqr()==0,"Visual-only domain never damages, weakens or pushes");
                h.assertTrue(f.owner.hasEffect(MobEffects.POISON)&&f.owner.hasEffect(MobEffects.WEAKNESS),"Clear Heart no longer cleanses owner");
            }h.succeed();
        });
    }
}
