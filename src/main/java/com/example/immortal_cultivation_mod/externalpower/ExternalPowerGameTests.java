package com.example.immortal_cultivation_mod.externalpower;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.externalpower.ExternalAttackProfile.*;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Lifecycle;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class ExternalPowerGameTests {
    private static ResourceLocation id(String id){return ResourceLocation.parse(id);}
    private static final Traits PLAIN=new Traits(false,false,false,false,false,false,true,false,false,false,false,false);
    private static final Traits BEAM=new Traits(false,true,false,true,false,false,false,false,false,false,false,false);
    private static final Traits BLAST=new Traits(true,false,false,false,false,false,true,false,false,false,false,false);
    private static Observation observation(String type,float raw,Traits traits,double radius,int hits,double total) {
        return new Observation(new Key("compat_test",id(type),null,null,null,null),traits,raw,raw,0,0,radius,0,-1,hits,total,hits*20);
    }
    @SafeVarargs private static DamageSource source(String name,TagKey<DamageType>... tags) {
        // Isolated registry: test real namespaced holders without adding fake damage types to production datapacks.
        var registry=new MappedRegistry<DamageType>(Registries.DAMAGE_TYPE,Lifecycle.stable());
        var holder=registry.register(ResourceKey.create(Registries.DAMAGE_TYPE,id(name)),new DamageType("compat_test",0),RegistrationInfo.BUILT_IN);
        var bindings=new java.util.HashMap<TagKey<DamageType>,List<net.minecraft.core.Holder<DamageType>>>();
        for(var tag:tags)bindings.put(tag,List.of(holder));
        registry.bindTags(bindings);registry.freeze();return new DamageSource(holder);
    }
    private static FakePlayer player(GameTestHelper h,String realm) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"external-power-test")) {
            // FakePlayer is normally immune to every hit; this fixture exercises actual health loss.
            @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        };
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(realm));
        return p;
    }
    private static LivingDamageEvent.Pre event(net.minecraft.world.entity.LivingEntity target,DamageSource source,float raw,float reduced) {
        var container=new DamageContainer(source,raw);container.setNewDamage(reduced);
        return new LivingDamageEvent.Pre(target,container);
    }

    @GameTest(template="fate_test")
    public static void ownModAndVanillaGuardsTakePriority(GameTestHelper h) {
        var vanilla=id("minecraft:mob_attack");var own=id(ImmortalCultivationMod.MODID+":spell");var external=id("compat_test:beam");
        h.assertTrue(ExternalPowerSystem.resolveMod(vanilla,null,null,null,false)==null,"Vanilla defaults to unchanged");
        h.assertTrue("minecraft".equals(ExternalPowerSystem.resolveMod(vanilla,null,null,null,true)),"Vanilla requires explicit opt-in");
        for(int i=0;i<4;i++)h.assertTrue(ExternalPowerSystem.resolveMod(i==0?own:external,i==1?own:null,i==2?own:null,i==3?own:null,true)==null,
                "Own damage, mobs, projectiles, and used weapons are always excluded");
        h.assertTrue("compat_test".equals(ExternalPowerSystem.resolveMod(vanilla,id("compat_test:beast"),null,null,false)),"External mobs using vanilla mobAttack are attributed to the external mod");
        h.assertTrue("compat_test".equals(ExternalPowerSystem.resolveMod(vanilla,null,null,id("compat_test:sword"),false)),"External melee weapons using vanilla playerAttack are recognized");
        var target=player(h,CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY);
        var ownEvent=event(target,source(own.toString()),100,80);ExternalPowerSystem.damage(ownEvent);
        h.assertTrue(ownEvent.getNewDamage()==80,"Own namespace remains unchanged by the real handler");
        var vanillaEvent=event(target,target.damageSources().generic(),100,80);ExternalPowerSystem.damage(vanillaEvent);
        h.assertTrue(vanillaEvent.getNewDamage()==80,"Default vanilla damage remains unchanged by the real handler");
        var cow=EntityType.COW.create(h.getLevel());var animalEvent=event(cow,source("compat_test:beam"),100,80);ExternalPowerSystem.damage(animalEvent);
        h.assertTrue(animalEvent.getNewDamage()==80,"Ordinary animals are not cultivation targets");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void classifierUsesIndependentEvidenceAndSeparatesPenetration(GameTestHelper h) {
        var settings=ExternalPowerConfig.defaults();
        var raw=ExternalAttackAnalyzer.estimate(observation("compat_test:opaque",100_000_000,PLAIN,-1,1,100_000_000),settings,8);
        h.assertTrue(raw.powerTier()<=2&&raw.confidence()<.6,"An opaque huge number alone is low-confidence, not Tier 9");
        var base=ExternalAttackAnalyzer.estimate(observation("compat_test:beam",1000,BEAM,-1,1,1000),settings,4);
        var piercing=new Traits(false,true,false,true,false,false,false,true,true,true,true,true);
        var penetrated=ExternalAttackAnalyzer.estimate(observation("compat_test:beam",1000,piercing,-1,1,1000),settings,4);
        h.assertTrue(penetrated.powerTier()==base.powerTier()&&penetrated.penetrationTier()>base.penetrationTier(),"Defense bypass increases penetration separately from power");
        var small=ExternalAttackAnalyzer.estimate(observation("compat_test:nuclear_blast",300,BLAST,8,1,300),settings,4);
        var huge=ExternalAttackAnalyzer.estimate(observation("compat_test:nuclear_blast",300,BLAST,128,1,300),settings,4);
        var extreme=ExternalAttackAnalyzer.estimate(observation("compat_test:blast",300,BLAST,8192,1,300),settings,4);
        h.assertTrue(huge.powerTier()>small.powerTier()&&extreme.powerTier()>=10,"Different nuclear sizes remain distinct and strong evidence can reach Tier 10+");
        h.assertTrue(ExternalAttackAnalyzer.damageScore(100_000,settings.thresholds())==6,"Damage thresholds use the documented logarithmic baseline");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void burstClassificationDoesNotCapDamageOrMergeAttackers(GameTestHelper h) {
        ExternalAttackBurst burst=null;
        for(int i=0;i<4;i++)burst=ExternalAttackBurst.observe(burst,100+i,500,4);
        h.assertTrue(burst.hits()==4&&burst.totalDamage()==2000,"Four repeated 500-damage hits are observed as one 2000-strength burst");
        h.assertTrue(ExternalAttackBurst.observe(burst,110,500,4).hits()==1,"Quiet gaps expire old bursts");
        var first=observation("compat_test:ray",500,BEAM,-1,1,500);
        var repeated=observation("compat_test:ray",500,BEAM,-1,4,2000);
        h.assertTrue(ExternalAttackAnalyzer.estimate(repeated,ExternalPowerConfig.defaults(),4).powerTier()
                >ExternalAttackAnalyzer.estimate(first,ExternalPowerConfig.defaults(),1).powerTier(),"Burst strength can raise classification");
        var a=new ExternalAttackBurst.Key(first.key(),UUID.randomUUID(),null);
        var b=new ExternalAttackBurst.Key(first.key(),UUID.randomUUID(),null);
        h.assertTrue(!a.equals(b),"Separate attackers cannot share a burst");
        double sum=0;for(int i=0;i<4;i++)sum+=ExternalPowerSystem.adjustDamage(500,1.25);
        h.assertTrue(sum==2500,"Every hit keeps its separately calculated damage; no DPS cap");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void stableProfilesReuseCacheAndLargeChangesReclassify(GameTestHelper h) {
        var analyzer=new ExternalAttackAnalyzer();var settings=ExternalPowerConfig.defaults();
        var normal=observation("compat_test:blast",100,BLAST,4,1,100);
        Estimate cached=null;for(int i=0;i<5;i++)cached=analyzer.analyze(normal,settings);
        h.assertTrue(cached==analyzer.analyze(normal,settings),"Stable attacks reuse cached analysis after initial observations");
        var stronger=analyzer.analyze(observation("compat_test:blast",100_000,BLAST,128,1,100_000),settings);
        h.assertTrue(stronger.powerTier()>cached.powerTier(),"Dramatic damage and blast-radius changes invalidate cached estimates");
        for(int i=0;i<600;i++)analyzer.analyze(observation("compat_test:source_"+i,10,PLAIN,-1,1,10),settings);
        h.assertTrue(analyzer.size()==512,"Classification cache stays bounded");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void overridesBeatAutomaticLimitsAndValidateAtomically(GameTestHelper h) {
        var overrides=ExternalPowerOverrides.parse(new StringReader("{\"compat_test:laser\":{\"powerTier\":20,\"penetrationTier\":3},\"compat_test:bomb\":{\"powerTier\":6}}"));
        var estimate=ExternalAttackAnalyzer.estimate(observation("compat_test:laser",1,PLAIN,-1,1,1),ExternalPowerConfig.defaults(),1);
        var manual=ExternalPowerSystem.applyOverride(estimate,overrides.get(id("compat_test:laser")));
        h.assertTrue(manual.powerTier()==20&&manual.penetrationTier()==3&&manual.confidence()==1,"Manual values outrank automatic tier limits and preserve separate penetration");
        boolean rejected=false;try{ExternalPowerOverrides.parse(new StringReader("{\"compat_test:bad\":{\"powerTier\":-1}}"));}catch(RuntimeException expected){rejected=true;}
        h.assertTrue(rejected,"Malformed tiers fail validation rather than corrupting live overrides");
        var settings=ExternalPowerConfig.defaults();
        h.assertTrue(1000*settings.multiplier(5-5)>50000*settings.multiplier(2-5),"Tier 5 power can outperform much larger Tier 2 raw damage against a Tier 5 target");
        h.assertTrue(ExternalPowerSystem.adjustDamage(1_000_000,1.5)==1_500_000,"Strong attacks have no absolute damage cap");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void liveHandlerScalesReducedDamageAndCanBeDisabled(GameTestHelper h) {
        var target=player(h,CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY);
        var hit=event(target,source("compat_test:impact"),100,80);ExternalPowerSystem.damage(hit);
        h.assertTrue(Math.abs(hit.getNewDamage()-.08F)<.00001,"Resistance multiplies the post-armor value, not the original raw amount");
        boolean enabled=ExternalPowerConfig.ENABLED.get();ExternalPowerConfig.ENABLED.set(false);
        try {var disabled=event(target,source("compat_test:impact"),100,80);ExternalPowerSystem.damage(disabled);
            h.assertTrue(disabled.getNewDamage()==80,"Master switch completely disables scaling");
        } finally {ExternalPowerConfig.ENABLED.set(enabled);}
        boolean vanilla=ExternalPowerConfig.VANILLA.get();ExternalPowerConfig.VANILLA.set(true);
        try {var opted=event(target,target.damageSources().generic(),100,80);ExternalPowerSystem.damage(opted);
            h.assertTrue(opted.getNewDamage()<80,"Opted-in vanilla damage uses the same classifier");
            var own=event(target,source(ImmortalCultivationMod.MODID+":test"),100,80);ExternalPowerSystem.damage(own);
            h.assertTrue(own.getNewDamage()==80,"Vanilla opt-in never enables own-mod scaling");
        } finally {ExternalPowerConfig.VANILLA.set(vanilla);}
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void realExplosionEventSuppliesSizeToDamageClassification(GameTestHelper h) {
        var target=player(h,CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY);
        target.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3,3,3)));
        DamageSource damage=source("compat_test:explosion",DamageTypeTags.IS_EXPLOSION);
        var explosion=new Explosion(h.getLevel(),null,damage,null,target.getX(),target.getY(),target.getZ(),128,false,
                Explosion.BlockInteraction.KEEP,net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);
        ExternalPowerSystem.explosion(new ExplosionEvent.Detonate(h.getLevel(),explosion,List.of(target)));
        var hit=event(target,damage,100,80);ExternalPowerSystem.damage(hit);
        h.assertTrue(hit.getNewDamage()>=80,"Actual explosion metadata identifies a huge blast despite a small raw sample");h.succeed();
    }

    @GameTest(template="fate_test")
    public static void actualHealthLossRespectsScalingAndAllowsLethalDamage(GameTestHelper h) {
        var baseline=player(h,CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY);
        baseline.setHealth(20);baseline.invulnerableTime=0;
        boolean enabled=ExternalPowerConfig.ENABLED.get();
        ExternalPowerConfig.ENABLED.set(false);
        try {
            baseline.hurt(source("compat_test:physical",DamageTypeTags.BYPASSES_INVULNERABILITY),20);
        } finally {ExternalPowerConfig.ENABLED.set(enabled);}
        float baselineLoss=20-baseline.getHealth();
        h.assertTrue(baselineLoss>0,"Baseline damage reaches health through existing cultivation defenses");
        var target=player(h,CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY);
        target.setHealth(20);target.invulnerableTime=0;
        // Bypass ServerPlayer's login grace period; penetration must not raise the power tier.
        h.assertTrue(target.hurt(source("compat_test:physical",DamageTypeTags.BYPASSES_INVULNERABILITY),20),"External hit traverses the real Minecraft damage pipeline");
        float expected=20-baselineLoss*.05F;
        h.assertTrue(Math.abs(target.getHealth()-expected)<.01,"Real health loss matches Tier 0 versus Foundation resistance: expected "+expected+", got "+target.getHealth());
        h.assertTrue(target.invulnerableTime<=20,"Scaling never extends vanilla invulnerability frames");
        var lethal=player(h,CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY);
        lethal.setHealth(20);lethal.invulnerableTime=0;
        lethal.hurt(source("compat_test:lethal",DamageTypeTags.BYPASSES_ARMOR,DamageTypeTags.BYPASSES_INVULNERABILITY),1_000_000);
        h.assertTrue(lethal.isDeadOrDying(),"A sufficiently damaging external attack can still kill a high-realm target");h.succeed();
    }
}
