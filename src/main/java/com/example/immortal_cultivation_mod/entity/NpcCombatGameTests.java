package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class NpcCombatGameTests {
    @GameTest(template="npc_combat_arena")
    public static void realNpcSlashUsesWeaponLevelCollisionRadius(GameTestHelper h) {
        var npc=npc(h);npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setPos(npc.position().add(5,0,0));npc.setTarget(enemy);
        for(float damage:new float[]{10,100,1000}) {
            h.assertTrue(WeaponQiSlashEntity.releaseNpc(npc,enemy,7,damage,.95,"any",2,false),"NPC releases a real slash");
            var slashes=h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class,npc.getBoundingBox().inflate(3));
            h.assertTrue(slashes.size()==1,"One slash exists for this release");
            var slash=slashes.getFirst();var save=new CompoundTag();slash.saveWithoutId(save);
            double radius=save.getDouble("Radius");
            h.assertTrue(Math.abs(radius-com.example.immortal_cultivation_mod.combat.ExternalQiSize.radius(npc.getMainHandItem(),1,false,false,false))<.00001,
                    "Actual entity stores weapon-level radius independently of released damage");
            h.assertTrue(save.getFloat("Damage")==damage&&save.getDouble("MaxDistance")==7,
                    "Changing width does not increase attack damage or travel range");
            h.assertTrue(save.getInt("RemainingBlocks") == WeaponQiSlashEntity.damageScaledBlockBudget(damage),
                    "Actual NPC slash uses damage-based terrain capacity");
            h.assertTrue(save.getBoolean("BlockBreaking") == h.getLevel().getGameRules().getBoolean(
                    net.minecraft.world.level.GameRules.RULE_MOBGRIEFING), "NPC destruction respects mobGriefing");
            h.assertTrue(Math.abs(slash.forcefieldSupport(new net.minecraft.world.phys.Vec3(0,0,1))
                    - com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.bladeReach(radius))<.00001,
                    "Forcefield collision includes the extended pointed tips");
            var direction = new net.minecraft.world.phys.Vec3(save.getDouble("DirectionX"), save.getDouble("DirectionY"), save.getDouble("DirectionZ"));
            var perpendicular = direction.cross(com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.axis(direction, 2)).normalize();
            h.assertTrue(Math.abs(slash.forcefieldSupport(perpendicular)
                    - com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.halfThickness(radius)) < .00001,
                    "Actual slash collision includes the thicker blade body");
            var loaded=ModEntities.WEAPON_QI_SLASH.get().create(h.getLevel());loaded.load(save);
            h.assertTrue(Math.abs(loaded.forcefieldSupport(new net.minecraft.world.phys.Vec3(0,0,1))
                    - com.example.immortal_cultivation_mod.effect.ExternalQiVfxShape.bladeReach(radius))<.00001,
                    "Reload preserves width without applying the damage scale twice");
            slash.discard();
        }
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void externalQiHasPriorityAtMeleeAndPursuitDistances(GameTestHelper h) {
        for(double distance:new double[]{2.5,5}) {
            var npc=npc(h);var profile=NpcProfile.cultivationProfile(npc);
            profile.putString("Realm",CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY);
            npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(12.5,2,12.5)));
            npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setNoAi(true);
            enemy.setPos(npc.position().add(distance,0,0));h.getLevel().addFreshEntity(enemy);npc.setTarget(enemy);
            int before=NpcProfile.qi(npc,NpcProfile.LIANQI);
            var brain=new NpcCombatBrain(npc);brain.tick();
            var state=npc.getPersistentData().getCompound("ImmortalCultivationNpcCombatState");
            h.assertTrue("external_qi".equals(state.getString("Intent")),"Affordable external Qi outranks ordinary melee or chasing at distance "+distance);
            h.assertTrue(NpcProfile.qi(npc,NpcProfile.LIANQI)==before-5,"Foundation external Qi still costs five percent of maximum Qi");
            long cooldown=state.getLong("ExternalQi")-h.getLevel().getGameTime();
            h.assertTrue(cooldown>=16&&cooldown<=24,"External Qi cooldown is 16–24 ticks");
            var slashes=h.getLevel().getEntitiesOfClass(WeaponQiSlashEntity.class,npc.getBoundingBox().inflate(3));
            h.assertTrue(!slashes.isEmpty(),"Decision releases a real Qi slash");
            brain.tick();h.assertTrue(NpcProfile.qi(npc,NpcProfile.LIANQI)==before-5,"Action lock prevents duplicate spending in the same tick");
            for(var slash:slashes)slash.discard();enemy.discard();
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void shortCombosRequireHitsAndEndWithRecovery(GameTestHelper h) {
        var rhythm=new NpcCombatRhythm();var target=UUID.randomUUID();
        rhythm.strike(target,0,true);
        h.assertTrue(rhythm.follows(target,10)&&!rhythm.finisher(target,10),"First hit opens a short follow-up window");
        rhythm.strike(target,10,true);
        h.assertTrue(rhythm.finisher(target,20),"Two connected hits prepare the heavy finisher");
        rhythm.strike(target,20,true);
        h.assertTrue(rhythm.recovering(30)&&!rhythm.follows(target,30)&&!rhythm.recovering(42),"Third hit has finite recovery, not an endless combo");
        rhythm.strike(target,50,true);rhythm.strike(target,60,false);
        h.assertTrue(!rhythm.follows(target,65),"A missed attack breaks the chain");
        rhythm.strike(target,80,true);
        h.assertTrue(!rhythm.follows(UUID.randomUUID(),90)&&!rhythm.follows(target,112),"Chains cannot transfer targets or survive a long pause");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void projectileReactionUsesTrajectoryRatherThanProximity(GameTestHelper h) {
        var body=new net.minecraft.world.phys.AABB(-.3,0,-.3,.3,1.8,.3);
        var near=new net.minecraft.world.phys.Vec3(4,1,0);
        h.assertTrue(NpcCombatReactions.incoming(body,near,new net.minecraft.world.phys.Vec3(-1,0,0)),"Incoming projectile intersects predicted body space");
        h.assertTrue(!NpcCombatReactions.incoming(body,near,new net.minecraft.world.phys.Vec3(1,0,0)),"Departing projectile is not a threat");
        h.assertTrue(!NpcCombatReactions.incoming(body,near,new net.minecraft.world.phys.Vec3(0,0,1)),"Passing projectile does not trigger omnidirectional dodging");
        h.assertTrue(!NpcCombatReactions.incoming(body,near,net.minecraft.world.phys.Vec3.ZERO),"Stationary arrows do not trigger reactions");
        h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void incomingArrowTriggersRealStepWithReactionCooldown(GameTestHelper h) {
        var defender=npc(h);defender.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(12.5,2,12.5)));
        defender.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setNoAi(true);enemy.setPos(defender.position().add(6,0,0));
        h.getLevel().addFreshEntity(enemy);defender.setTarget(enemy);
        var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(enemy);arrow.setNoGravity(true);
        arrow.setPos(defender.position().add(4,1,0));arrow.setDeltaMovement(-1,0,0);h.getLevel().addFreshEntity(arrow);
        new NpcCombatBrain(defender).tick();
        var state=defender.getPersistentData().getCompound("ImmortalCultivationNpcCombatState");
        h.assertTrue("react_to_threat".equals(state.getString("Intent")),"Brain prioritizes a visible incoming projectile");
        h.assertTrue(defender.getNavigation().isInProgress()&&Math.abs(defender.getDeltaMovement().z)>.2,
                "Actual evasive action steps across the incoming shot instead of standing still");
        h.assertTrue(state.getLong("Reaction")>=h.getLevel().getGameTime()+24,"Reactions have a cooldown so every shot cannot be dodged");
        h.assertTrue(defender.invulnerableTime==0,"A dodge adds no invulnerability frames");
        arrow.setDeltaMovement(1,0,0);
        h.assertTrue(NpcCombatReactions.projectile(defender)==null,"Live threat scan ignores a shot moving away");
        arrow.discard();
        var crossfire=npc(h);crossfire.setPos(defender.position());crossfire.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        crossfire.setTarget(enemy);
        var sideArrow=EntityType.ARROW.create(h.getLevel());sideArrow.setOwner(enemy);sideArrow.setNoGravity(true);
        sideArrow.setPos(crossfire.position().add(0,1,4));sideArrow.setDeltaMovement(0,0,-1);h.getLevel().addFreshEntity(sideArrow);
        new NpcCombatBrain(crossfire).tick();
        h.assertTrue(Math.abs(crossfire.getDeltaMovement().x)>.2&&Math.abs(crossfire.getDeltaMovement().z)<.01,
                "Crossfire changes dodge direction to cross the incoming trajectory, independent of the current target");
        sideArrow.discard();enemy.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void weaponGuardIsDirectionalCostsQiAndFailsWhenStunned(GameTestHelper h) {
        var defender=npc(h);defender.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));defender.setYRot(-90);
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setPos(defender.position().add(2,0,0));
        int before=NpcProfile.qi(defender,NpcProfile.LIANQI);
        h.assertTrue(NpcCombatReactions.startGuard(defender),"Armed NPC raises a short guard");
        var early=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(defender,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(defender.damageSources().mobAttack(enemy),10));
        NpcCombatReactions.guard(early);
        h.assertTrue(early.getAmount()==10,"Guard startup remains vulnerable");
        h.assertTrue(!NpcCombatReactions.startGuard(defender),"Guard cannot restart while cooling down");
        h.runAfterDelay(2,()-> {
        var front=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(defender,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(defender.damageSources().mobAttack(enemy),10));
        NpcCombatReactions.guard(front);
        h.assertTrue(Math.abs(front.getAmount()-4.5)<.001&&NpcProfile.qi(defender,NpcProfile.LIANQI)==before-4,
                "Frontal guard reduces damage and pays startup plus impact Qi");
        h.assertTrue(defender.getMainHandItem().getDamageValue()==1,"Guard impact wears the equipped weapon");
        enemy.setPos(defender.position().add(-2,0,0));
        var back=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(defender,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(defender.damageSources().mobAttack(enemy),10));
        NpcCombatReactions.guard(back);h.assertTrue(back.getAmount()==10,"Rear attacks bypass the guard");
        defender.addEffect(new MobEffectInstance(ModEffects.DAZE,20));
        h.assertTrue(!NpcCombatReactions.startGuard(defender),"Stunned NPC cannot raise a new guard");
        enemy.setPos(defender.position().add(2,0,0));
        var stunned=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(defender,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(defender.damageSources().mobAttack(enemy),10));
        NpcCombatReactions.guard(stunned);h.assertTrue(stunned.getAmount()==10,"Stun disables an existing guard too");
        defender.removeEffect(ModEffects.DAZE);NpcProfile.cultivationProfile(defender).putInt("Qi",0);
        NpcCombatReactions.guard(stunned);h.assertTrue(stunned.getAmount()==10&&!defender.hasEffect(ModEffects.NPC_WEAPON_GUARD),"An unaffordable impact breaks guard without deleting damage");
        h.succeed();
        });
    }

    @GameTest(template="npc_combat_arena")
    public static void hostileAcquisitionUsesTenBlocksAndStartsPursuit(GameTestHelper h) {
        var npc=npc(h);
        npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(6.5,2,6.5)));
        h.getLevel().addFreshEntity(npc);
        npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        var enemy=EntityType.ZOMBIE.create(h.getLevel());
        enemy.setNoAi(true);enemy.setPos(npc.position().add(10.1,0,0));h.getLevel().addFreshEntity(enemy);
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget()==null,"An unprovoked hostile outside ten blocks is not acquired");
        enemy.setPos(npc.position().add(10,0,0));
        new NpcCombatBrain(npc).tick();
        h.assertTrue(npc.getTarget()==enemy,"Hostile mobs at ten blocks are acquired without waiting for a hit");
        h.assertTrue(npc.getNavigation().isInProgress(),"Healthy ground fighters run toward distant enemies");
        h.assertTrue("close_gap".equals(npc.getPersistentData().getCompound("ImmortalCultivationNpcCombatState").getString("Intent")),
                "Pursuit outranks passive spacing and probing at long distance");
        enemy.discard();npc.discard();h.succeed();
    }

    @GameTest(template="npc_combat_arena")
    public static void goldenCorePursuesAirborneEnemiesWithoutWindSpellOrGroundSupport(GameTestHelper h) {
        var npc=npc(h);
        NpcProfile.cultivationProfile(npc).putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY);
        npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(6.5,40,6.5)));
        npc.setOnGround(false);
        npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        var enemy=EntityType.ZOMBIE.create(h.getLevel());
        enemy.setNoAi(true);enemy.setNoGravity(true);enemy.setPos(npc.position().add(13,7,0));h.getLevel().addFreshEntity(enemy);
        npc.setTarget(enemy);
        var brain=new NpcCombatBrain(npc);brain.tick();brain.tickFlightMotion(.48);
        h.assertTrue(!npc.hasEffect(ModEffects.YUFENG_JUE)&&npc.isNoGravity(),"Golden Core flies innately above the spell's support limit");
        h.assertTrue(npc.getDeltaMovement().x>0&&npc.getDeltaMovement().y>0,"Actual pursuit steers toward an elevated enemy in three dimensions");
        enemy.discard();h.succeed();
    }

    @GameTest(template="fate_test")
    public static void realmFlightHasThreeTimesPlayerSpeedAndBrakes(GameTestHelper h) {
        var npc=npc(h);var profile=NpcProfile.cultivationProfile(npc);
        h.assertTrue(!NpcSpellcasting.hasCreativeFlight(npc),"Qi Refining cannot use innate flight");
        profile.putString("Realm",CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_LATE);
        h.assertTrue(!NpcSpellcasting.hasCreativeFlight(npc),"Foundation still needs a flight spell");
        for(String realm:new String[]{CultivationLevels.REALM_JINDAN,CultivationLevels.REALM_YUANYING,CultivationLevels.REALM_HUASHEN}) {
            profile.putString("Realm",realm+CultivationLevels.STAGE_EARLY);
            h.assertTrue(NpcSpellcasting.hasCreativeFlight(npc),"Golden Core and higher all receive innate flight");
        }
        var destination=npc.position().add(0,50,0);
        for(int i=0;i<24;i++)NpcSpellcasting.tickCreativeFlight(npc,destination);
        double speed=com.example.immortal_cultivation_mod.spell.JindanCreativeFlight.BASE_TRAVEL_SPEED*3;
        h.assertTrue(Math.abs(npc.getDeltaMovement().length()-speed)<.0001,"Vertical flight reaches exactly three times player base travel speed");
        for(int i=0;i<24;i++)NpcSpellcasting.tickCreativeFlight(npc,null);
        h.assertTrue(npc.getDeltaMovement().length()<.001&&npc.isNoGravity(),"Idle airborne NPC brakes to a hover without falling");
        NpcSpellcasting.tickCreativeFlight(npc,npc.position().add(0,-50,0));
        h.assertTrue(npc.getDeltaMovement().y<0,"Free flight can descend as well as climb");
        h.succeed();
    }

    static LianqiCultivatorEntity npc(GameTestHelper h) {
        var npc=ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        NpcProfile.ensure(npc,NpcProfile.LIANQI);var p=NpcProfile.cultivationProfile(npc);
        p.putString("Realm",CultivationLevels.REALM_LIANQI+CultivationLevels.STAGE_EARLY);
        p.putString("Personality","gentle");p.putString("Identity","wandering_cultivator");p.putString("Name",UUID.randomUUID().toString());
        p.put("Inventory",new ListTag());p.put("Spells",new ListTag());p.put("Methods",new ListTag());p.putString("Method","");
        p.putInt("Qi",100);p.putInt("MaxQi",100);p.putInt("Age",100);
        npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        var point=h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5,2,3.5));npc.setPos(point);npc.setOnGround(true);
        NpcProfile.ensure(npc,NpcProfile.LIANQI);npc.setHealth(npc.getMaxHealth());
        com.example.immortal_cultivation_mod.fate.LuckSystem.data(npc).putInt("Luck",100);
        return npc;
    }
    private static NpcBattlePlan plan() {
        var plan=new NpcBattlePlan();plan.begin(UUID.randomUUID(),0,8,"cautious");return plan;
    }
    private static NpcBattlePlan.Situation danger(float health,boolean route,boolean teleport,boolean healing,boolean sparring) {
        return new NpcBattlePlan.Situation(health,.08F,2,0,3,5,1,.7F,false,route,teleport,healing,false,true,.4F,sparring);
    }
    @GameTest(template="fate_test")
    public static void eightPercentWithEscapeOrHealingNeverEntersFrenzy(GameTestHelper h) {
        for(int option=0;option<3;option++) {
            var plan=plan();
            for(long now=0;now<=240;now+=12) {
                plan.failedEscape(now);plan.update(danger(.08F,option==0,option==1,option==2,false),now,8);
                h.assertTrue(plan.mode()==NpcBattlePlan.Mode.SURVIVAL,"A real remaining survival option prevents frenzy");
            }
        }
        h.succeed();
    }
    @GameTest(template="fate_test")
    public static void trappedFortyPercentRequiresRepeatedFailedExits(GameTestHelper h) {
        var plan=plan();plan.update(danger(.4F,false,false,false,false),0,8);
        plan.update(danger(.4F,false,false,false,false),100,8);
        h.assertTrue(!plan.frenzy(),"Time and damage alone cannot declare escape impossible");
        for(long now=112;now<=148;now+=12){plan.failedEscape(now);plan.update(danger(.4F,false,false,false,false),now,8);}
        h.assertTrue(plan.frenzy(),"Trapped and overwhelmed at 40% HP enters a last stand after failed exits");
        plan.update(danger(.08F,false,true,false,false),160,8);
        h.assertTrue(plan.mode()==NpcBattlePlan.Mode.SURVIVAL,"A newly available teleport ends the last stand");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void sparringNeverPermitsSuicidalMode(GameTestHelper h) {
        var plan=plan();for(long now=0;now<300;now+=12){plan.failedEscape(now);plan.update(danger(.03F,false,false,false,true),now,8);}
        h.assertTrue(!plan.frenzy(),"Training battles never enter death frenzy");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void normalModePreservesQiAndHiddenTechniques(GameTestHelper h) {
        var plan=plan();
        h.assertTrue(!plan.canSpend(40,50,100,false),"A normal attack cannot spend 80% of remaining Qi");
        h.assertTrue(plan.canSpend(40,50,100,true),"A real emergency can spend the reserve");
        h.assertTrue(plan.revealPenalty(.25,false,false)>plan.revealPenalty(.25,true,false),"Unrevealed trump cards have a concealment cost");
        h.assertTrue(!plan.canSpend(51,50,100,true),"Emergency never invents Qi");
        plan.update(new NpcBattlePlan.Situation(1,1,0,0,1,0,.4F,.2F,true,true,false,false,false,false,0,false),200,8);
        h.assertTrue(plan.pressure(),"A witnessed opening replaces probing with pressure");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void hiddenRealmQiAndSpellsDoNotLeakIntoKnowledge(GameTestHelper h) {
        var observer=npc(h);var enemy=npc(h);enemy.setPos(observer.getX()+2,observer.getY(),observer.getZ());
        var before=NpcCombatKnowledge.observe(observer,enemy,false,false,false);
        var secret=NpcProfile.cultivationProfile(enemy);secret.putInt("Qi",0);
        secret.putString("Realm",CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE);
        ListTag spells=new ListTag();spells.add(StringTag.valueOf(ModSpells.ZHENSHAN_PALM));secret.put("Spells",spells);
        var after=NpcCombatKnowledge.view(observer,enemy);
        h.assertTrue(before.realmDelta()==0&&after.realmDelta()==0&&after.qi()==-1&&!after.ranged(),"Unknown enemy resources and learned spells remain unknown");
        h.assertTrue(!NpcCombatKnowledge.wasRevealed(observer,ModSpells.FIREBALL),"Unused spells start concealed even near world tick zero");
        NpcCombatKnowledge.reveal(observer,ModSpells.FIREBALL);
        h.assertTrue(NpcCombatKnowledge.wasRevealed(observer,ModSpells.FIREBALL),"Only an actual release reveals an art");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void sightMemoryExpiresAndIsBounded(GameTestHelper h) {
        var observer=npc(h);var hidden=npc(h);hidden.setPos(observer.getX()+3,observer.getY(),observer.getZ());
        for(int z=1;z<=6;z++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(5,y,z),Blocks.STONE);
        h.assertTrue(!observer.hasLineOfSight(hidden),"Fixture blocks visual observation");
        var unseen=NpcCombatKnowledge.observe(observer,hidden,true,true,true);
        h.assertTrue(unseen.lastPosition()==null&&unseen.observations()==0,"A wall blocks cast, defense and position learning");
        for(int i=0;i<16;i++) {
            var enemy=npc(h);enemy.setPos(observer.getX(),observer.getY(),observer.getZ()+.2);
            NpcCombatKnowledge.observe(observer,enemy,false,false,false);
        }
        var memory=NpcProfile.cultivationProfile(observer).getCompound("CombatKnowledge");
        h.assertTrue(memory.size()<=12,"Enemy memory remains bounded");
        for(String key:memory.getAllKeys())memory.getCompound(key).putLong("LastSeen",h.getLevel().getGameTime()-6001);
        NpcCombatKnowledge.view(observer,hidden);h.assertTrue(memory.isEmpty(),"Expired encounters are discarded");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void shieldConsumesSavedItemAndAbsorbsActualDamage(GameTestHelper h) {
        var npc=npc(h);var stored=NpcProfile.inventory(npc);stored.set(0,new ItemStack(ModItems.BOJIN_HUFU.get(),2));
        NpcProfile.writeInventory(npc,NpcProfile.cultivationProfile(npc),stored);
        h.assertTrue(NpcCombatResources.use(npc,NpcCombatResources.Purpose.SHIELD,null),"Owned shield talisman activates");
        h.assertTrue(NpcProfile.inventory(npc).get(0).getCount()==1,"Exactly one talisman is consumed");
        h.assertTrue(!NpcCombatResources.use(npc,NpcCombatResources.Purpose.SHIELD,null),"An active shield cannot consume the next talisman");
        float shield=NpcCombatResources.shield(npc);float hp=npc.getHealth();
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setPos(npc.position().add(2,0,0));npc.setTarget(enemy);
        npc.hurt(npc.damageSources().mobAttack(enemy),10);
        h.assertTrue(npc.getHealth()==hp&&NpcCombatResources.shield(npc)<shield,"The shield absorbs real incoming damage: HP "+hp+" -> "+npc.getHealth()+", shield "+shield+" -> "+NpcCombatResources.shield(npc));
        var save=new CompoundTag();NpcProfile.save(npc,save);var loaded=npc(h);NpcProfile.load(loaded,save);
        h.assertTrue(NpcProfile.inventory(loaded).get(0).getCount()==1,"Consumed inventory persists across a save/load");h.succeed();
    }
    @GameTest(template="npc_combat_arena")
    public static void lowHealthBrainUsesRealTeleportAndSealsBlockIt(GameTestHelper h) {
        var npc=npc(h);npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16.5,1,16.5)));
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setPos(npc.position().add(2,0,0));npc.setTarget(enemy);
        var stored=NpcProfile.inventory(npc);stored.set(0,new ItemStack(ModItems.DUNKONG_TALISMAN.get()));
        NpcProfile.writeInventory(npc,NpcProfile.cultivationProfile(npc),stored);
        npc.setHealth(npc.getMaxHealth()*.08F);var before=npc.position();
        npc.addEffect(new MobEffectInstance(ModEffects.SPIRIT_SEAL,100));
        h.assertTrue(!NpcCombatResources.available(npc,NpcCombatResources.Purpose.ESCAPE,null),"Seal disables the actual teleport option");
        h.assertTrue(NpcProfile.inventory(npc).get(0).getCount()==1,"A blocked escape does not consume its item");npc.removeEffect(ModEffects.SPIRIT_SEAL);
        h.assertTrue(NpcCombatResources.available(npc,NpcCombatResources.Purpose.ESCAPE,null),"Loaded safe landing makes teleport available");
        new NpcCombatBrain(npc).tick();
        var state=npc.getPersistentData().getCompound("ImmortalCultivationNpcCombatState");
        h.assertTrue(state.getString("Mode").equals("SURVIVAL")&&npc.position().distanceTo(before)>=12,"Real brain chooses survival and teleports at 8% HP");
        h.assertTrue(NpcProfile.inventory(npc).get(0).isEmpty(),"Successful escape consumes the owned talisman");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void bloodCombustionAndArtifactSacrificeHaveFiniteCosts(GameTestHelper h) {
        var npc=npc(h);var p=NpcProfile.cultivationProfile(npc);p.putInt("Qi",0);
        p.putString("Method",com.example.immortal_cultivation_mod.attachment.CultivationMethods.BLOOD_DEMON_HUASHEN);
        float before=npc.getHealth();int age=p.getInt("Age");
        h.assertTrue(NpcCombatResources.burnEssence(npc),"Trained cultivator can burn essence");
        h.assertTrue(npc.getHealth()<before&&p.getInt("Age")==age-5&&NpcProfile.qi(npc,NpcProfile.LIANQI)>0,"Combustion spends health and lifespan for Qi");
        h.assertTrue(!NpcCombatResources.burnEssence(npc),"Combustion cannot repeat every decision");
        var stored=NpcProfile.inventory(npc);var sword=new ItemStack(Items.IRON_SWORD);stored.set(0,sword);
        NpcProfile.writeInventory(npc,p,stored);NpcProfile.equipCombatWeapon(npc);
        h.assertTrue(NpcProfile.consumeEquippedArtifact(npc),"A tracked weapon can be sacrificed once");
        NpcProfile.equipCombatWeapon(npc);
        h.assertTrue(npc.getMainHandItem().isEmpty()&&NpcProfile.inventory(npc).get(0).isEmpty(),"Sacrificed equipment cannot respawn from its inventory mirror");h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=100)
    public static void canceledWindupDoesNotSpendQiOrRevealSpell(GameTestHelper h) {
        var npc=npc(h);var p=NpcProfile.cultivationProfile(npc);var spells=new ListTag();spells.add(StringTag.valueOf(ModSpells.FIREBALL));p.put("Spells",spells);
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setNoAi(true);enemy.setPos(npc.position().add(5,0,0));h.getLevel().addFreshEntity(enemy);npc.setTarget(enemy);
        var brain=new NpcCombatBrain(npc);int qi=NpcProfile.qi(npc,NpcProfile.LIANQI);brain.tick();
        h.assertTrue(npc.npcCastingTicks()>0,"A cheap opening starts an actual spell windup");
        h.assertTrue(NpcProfile.qi(npc,NpcProfile.LIANQI)==qi&&!NpcCombatKnowledge.wasRevealed(npc,ModSpells.FIREBALL),"Preparation neither spends nor reveals a spell");
        npc.addEffect(new MobEffectInstance(ModEffects.DINGSHEN,80));brain.tick();
        h.assertTrue(npc.npcCastingTicks()==0&&NpcProfile.qi(npc,NpcProfile.LIANQI)==qi,"Real control cancels preparation without spending Qi");
        enemy.discard();h.succeed();
    }

    // The combat brain searches 40 blocks, beyond this structure and into concurrent tests.
    @GameTest(template="fate_test",timeoutTicks=100,batch="npc_cast_release")
    public static void successfulReleaseSpendsOnceAndTagsActualProjectile(GameTestHelper h) {
        var npc=npc(h);var p=NpcProfile.cultivationProfile(npc);var spells=new ListTag();spells.add(StringTag.valueOf(ModSpells.FIREBALL));p.put("Spells",spells);
        var enemy=EntityType.ZOMBIE.create(h.getLevel());enemy.setNoAi(true);enemy.setPos(npc.position().add(5,0,0));h.getLevel().addFreshEntity(enemy);npc.setTarget(enemy);
        int before=NpcProfile.qi(npc,NpcProfile.LIANQI);int cost=NpcSpellcasting.qiCost(npc,ModSpells.FIREBALL);
        var brain=new NpcCombatBrain(npc);brain.tick();
        h.assertTrue(npc.getTarget()==enemy,"The isolated casting fixture keeps its intended target");
        h.assertTrue(npc.npcCastingTicks()>0,"Actual brain begins a spell");
        h.runAfterDelay(15,()-> {
            brain.tick();
            h.assertTrue(NpcProfile.qi(npc,NpcProfile.LIANQI)==before-cost,"Release spends exactly the real spell cost: before="+before
                    +", cost="+cost+", after="+NpcProfile.qi(npc,NpcProfile.LIANQI)+", enemyAlive="+enemy.isAlive()
                    +", sight="+npc.hasLineOfSight(enemy)+", castingTicks="+npc.npcCastingTicks());
            h.assertTrue(NpcCombatKnowledge.wasRevealed(npc,ModSpells.FIREBALL),"Successful release reveals its art");
            var projectiles=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,
                    npc.getBoundingBox().inflate(12),e->e.getOwner()==npc);
            h.assertTrue(!projectiles.isEmpty()&&projectiles.stream().anyMatch(e->ModSpells.FIREBALL.equals(e.getPersistentData().getString("NpcAttemptSpell"))),
                    "Real spawned projectile carries its spell identity for hit feedback");
            brain.tick();
            h.assertTrue(NpcProfile.qi(npc,NpcProfile.LIANQI)==before-cost&&npc.npcCastingTicks()==0,
                    "Repeated ticks during recovery cannot spend Qi or start a second action");
            for(var projectile:projectiles)projectile.discard();enemy.discard();h.succeed();
        });
    }

    @GameTest(template="npc_combat_arena")
    public static void killConfirmationTracksOnlyWitnessedDragonsSoul(GameTestHelper h) {
        var npc=npc(h);npc.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(12.5,1,12.5)));npc.setNoAi(true);h.getLevel().addFreshEntity(npc);
        var dragon=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());dragon.setPos(npc.position().add(7,0,0));npc.setTarget(dragon);
        NpcCombatKnowledge.witnessedDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(dragon,npc.damageSources().genericKill()));
        var core=new EscapedYuanyingCoreEntity(h.getLevel(),dragon);
        h.assertTrue(NpcCombatKnowledge.witnessedCore(npc,core),"Witnessed body death permits pursuing its visible escaped soul");
        var unrelated=ModEntities.CRIMSON_SKY_FLAME_FLOOD_DRAGON.get().create(h.getLevel());unrelated.setPos(dragon.position());
        var stranger=new EscapedYuanyingCoreEntity(h.getLevel(),unrelated);
        h.assertTrue(!NpcCombatKnowledge.witnessedCore(npc,stranger),"An unrelated soul is not identified using proximity alone");
        var save=new CompoundTag();core.addAdditionalSaveData(save);
        var loaded=ModEntities.ESCAPED_YUANYING_CORE.get().create(h.getLevel());loaded.readAdditionalSaveData(save);
        h.assertTrue(dragon.getUUID().equals(loaded.originalDragon()),"Soul origin survives save/load");npc.discard();h.succeed();
    }
}
