package com.example.immortal_cultivation_mod.training;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.*;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.item.*;
import com.example.immortal_cultivation_mod.network.TrainingDummyPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.DielangShield;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class TrainingDummyGameTests {
    private static final AtomicInteger LAYERS=new AtomicInteger();
    private static final class Field implements AutoCloseable {
        final TrainingDummyEntity dummy;final FakePlayer player;final List<Entity> extras=new ArrayList<>();
        Field(GameTestHelper h) {
            var point=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,8000+LAYERS.getAndIncrement()*64,2)));
            player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"dummy-owner"));player.setGameMode(GameType.SURVIVAL);player.setPos(point.add(0,0,-3));
            dummy=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());dummy.setPos(point);dummy.setNoGravity(true);dummy.controller=player.getUUID();dummy.ensureProfile();h.getLevel().addFreshEntity(dummy);
            var stick=new ItemStack(ModItems.DUMMY_CONTROL_STICK.get());DummyControlStickItem.bind(stick,dummy);player.setItemInHand(InteractionHand.MAIN_HAND,stick);
        }
        boolean edit(String action,String key,String value){return TrainingDummyControls.edit(player,dummy.getUUID(),action,key,value);}
        @Override public void close(){dummy.discard();player.discard();for(var entity:extras)entity.discard();}
    }
    @GameTest(template="fate_test") public static void exactKnowledgeDoesNotRandomizeAcrossProfileChecksAndReload(GameTestHelper h) {
        try(var f=new Field(h)) {
            h.assertTrue(TrainingDummyEntity.strings(f.dummy.profile(),"Spells").isEmpty(),"Fresh dummy knows no random spells");
            f.edit("method_known",CultivationMethods.REINCARNATION_TRUE_ART,"");f.edit("method",CultivationMethods.REINCARNATION_TRUE_ART,"");
            f.edit("mastery",CultivationMethods.REINCARNATION_TRUE_ART,"753");f.edit("spell_known",ModSpells.FIREBALL,"");
            for(int i=0;i<40;i++)NpcProfile.ensure(f.dummy,NpcProfile.JINDAN);
            h.assertTrue(NpcProfile.knowsSpell(f.dummy,NpcProfile.JINDAN,ModSpells.FIREBALL)&&TrainingDummyEntity.strings(f.dummy.profile(),"Spells").size()==1,"Only chosen spell remains learned");
            var tag=new CompoundTag();f.dummy.addAdditionalSaveData(tag);var loaded=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());loaded.readAdditionalSaveData(tag);
            try {
                h.assertTrue(loaded.controller.equals(f.player.getUUID())&&loaded.profile().getString("Method").equals(CultivationMethods.REINCARNATION_TRUE_ART),"Owner and active method survive reload");
                h.assertTrue(loaded.profile().getCompound("MethodProficiencies").getInt(CultivationMethods.REINCARNATION_TRUE_ART)==753&&TrainingDummyEntity.strings(loaded.profile(),"Spells").equals(List.of(ModSpells.FIREBALL)),"Exact mastery and spells survive reload");
            }finally{loaded.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void bindingAndOwnershipRejectForgedOrOutOfRangeEdits(GameTestHelper h) {
        try(var f=new Field(h)) {
            h.assertTrue(TrainingDummyControls.resolve(f.player,f.dummy.getUUID())==f.dummy,"Owner holding bound controller resolves loaded entity");
            var other=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"dummy-stranger"));other.setPos(f.player.position());
            try {
                other.setItemInHand(InteractionHand.MAIN_HAND,f.player.getMainHandItem().copy());
                h.assertTrue(!TrainingDummyControls.edit(other,f.dummy.getUUID(),"name","","stolen"),"Copied UUID is insufficient without ownership");
                h.assertTrue(!TrainingDummyControls.edit(other,f.dummy.getUUID(),"unlock_all_spells","","")
                        &&!TrainingDummyControls.edit(other,f.dummy.getUUID(),"unlock_all_methods","",""),"Bulk unlock requires the bound dummy's owner");
            }finally{other.discard();}
            f.player.setItemInHand(InteractionHand.OFF_HAND,f.player.getMainHandItem());f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(f.edit("name","","Practice One"),"Offhand binding controls owned dummy");
            f.player.setPos(f.dummy.position().add(97,0,0));h.assertTrue(!f.edit("toggle","Domain",""),"Remote control cannot cross 96-block range");
            f.player.setPos(f.dummy.position());f.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            h.assertTrue(!f.edit("reset","",""),"Packets without held bound stick are rejected");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void statsAreAtomicAndInactiveMethodsCannotOpenDomains(GameTestHelper h) {
        try(var f=new Field(h)) {
            String realm=CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_LATE;
            f.dummy.setHealth(123);
            h.assertTrue(f.edit("stats",realm,"999,45000,25,37,8,0.25"),"Validated stats accepted");
            h.assertTrue(f.dummy.getHealth()==f.dummy.getMaxHealth(),"Applying a lower maximum HP refills a wounded dummy");
            h.assertTrue(f.dummy.getMaxHealth()==999&&f.dummy.magicAttack==37&&f.dummy.profile().getInt("MaxQi")==45000,"Real attributes and NPC Qi changed");
            f.dummy.setHealth(321);
            h.assertTrue(!f.edit("stats",CultivationLevels.REALM_MORTAL,"10,100,NaN,0,0,0"),"Non-finite input rejected");
            h.assertTrue(f.dummy.getHealth()==321,"Rejected stats do not heal the dummy");
            h.assertTrue(f.dummy.getMaxHealth()==999&&f.dummy.profile().getString("Realm").equals(realm),"Rejected edit never partially changes realm or health");
            h.assertTrue(f.edit("stats",realm,"15000,45000,25,37,8,0.25"),"Higher maximum HP accepted");
            var snapshot=TrainingDummyControls.snapshot(f.dummy);
            h.assertTrue(f.dummy.getHealth()==f.dummy.getMaxHealth()&&snapshot.getFloat("Health")==snapshot.getFloat("MaxHealth"),
                    "Raising maximum HP restores full health and sends full HP to the editor");
            f.edit("method_known",CultivationMethods.BASIC_BREATHING,"");
            h.assertTrue(!f.edit("toggle","Domain","")&&!f.dummy.domainRequested,"Unlearning active method prevents empty-style domain");
            h.assertTrue(!f.edit("method_known","missing_method","")&&!f.edit("spell_known","missing_spell",""),"Unknown registry IDs rejected");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void domainWorksWithoutTargetAndDoesNotTrainItself(GameTestHelper h) {
        try(var f=new Field(h)) {
            h.assertTrue(f.edit("toggle","Domain",""),"Manual domain request accepted without target");
            QiDomainSystem.npcTick(new EntityTickEvent.Post(f.dummy));QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(f.dummy)!=null&&QiDomainSystem.view(f.dummy).domain(),"Real domain activates without NPC combat target");
            f.dummy.tickCount=20;QiDomainSystem.npcTick(new EntityTickEvent.Post(f.dummy));
            h.assertTrue(QiDomainSystem.mastery(f.dummy)==500,"Testing never silently increases selected proficiency");
            f.edit("toggle","Domain","");h.assertTrue(!QiDomainSystem.blasting(f.dummy),"Manual off removes real aura state");
        }h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=120) public static void twoDummiesTargetEachOtherAndLosingDomainStaysOff(GameTestHelper h) {
        var f=new Field(h);var second=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());f.extras.add(second);second.setPos(f.dummy.position().add(24,0,0));second.setNoGravity(true);second.ensureProfile();h.getLevel().addFreshEntity(second);
        second.profile().putString("Method",CultivationMethods.REINCARNATION_TRUE_ART);second.profile().getCompound("MethodProficiencies").putInt(CultivationMethods.REINCARNATION_TRUE_ART,500);
        h.assertTrue(TrainingDummyControls.target(f.dummy,second)&&NpcProfile.isCombatEnemy(f.dummy,second),"Wooden mannequins are valid practice opponents, not social allies");
        f.dummy.setDomainRequested(true);second.setDomainRequested(true);QiDomainSystem.npcTick(new EntityTickEvent.Post(f.dummy));QiDomainSystem.npcTick(new EntityTickEvent.Post(second));QiDomainSystem.tickAll();
        h.runAfterDelay(DomainClashes.CONTEST_TICKS+10,()->{try {
            h.assertTrue(!f.dummy.domainRequested&&QiDomainSystem.view(second).domain(),"Losing dummy does not repeatedly reopen defeated domain");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test",timeoutTicks=80) public static void queuedSpellUsesRealEffectQiAndCooldown(GameTestHelper h) {
        var f=new Field(h);f.dummy.unlimitedQi=false;f.edit("spell_known",ModSpells.DIELANG_SHIELD,"");
        int qi=f.dummy.profile().getInt("Qi");h.assertTrue(f.dummy.cast(ModSpells.DIELANG_SHIELD)&&!f.dummy.cast(ModSpells.DIELANG_SHIELD),"Only one windup queued at once");
        h.runAfterDelay(NpcSpellcasting.windupTicks(ModSpells.DIELANG_SHIELD)+2,()->{try {
            h.assertTrue(f.dummy.profile().getInt("Qi")<qi&&DielangShield.remaining(f.dummy)>0,"Completed spell spends real Qi and grants actual shield");
            h.assertTrue(!f.dummy.cast(ModSpells.DIELANG_SHIELD),"Real cooldown blocks immediate repeat");
            f.dummy.resetVitals();h.assertTrue(f.dummy.profile().getInt("Qi")==qi&&f.dummy.getHealth()==f.dummy.getMaxHealth(),"Reset restores actual vitals");h.succeed();
        }finally{f.close();}});
    }
    @GameTest(template="fate_test") public static void controllerTargetsOtherDummyWithoutRebindingAndStopClearsTarget(GameTestHelper h) {
        try(var f=new Field(h)) {
            var second=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());second.setPos(f.dummy.position().add(3,0,0));second.ensureProfile();f.extras.add(second);h.getLevel().addFreshEntity(second);
            var event=new PlayerInteractEvent.EntityInteract(f.player,InteractionHand.MAIN_HAND,second);DummyControlStickItem.entity(event);
            h.assertTrue(event.isCanceled()&&f.dummy.getTarget()==second&&DummyControlStickItem.bound(f.player.getMainHandItem()).equals(f.dummy.getUUID()),"Right-click another dummy selects opponent and preserves bound owner");
            f.edit("stop","","");h.assertTrue(f.dummy.getTarget()==null&&!f.dummy.combatEnabled,"Stop cancels fighting and target");
            f.player.setGameMode(GameType.CREATIVE);h.assertTrue(!TrainingDummyControls.target(f.dummy,f.player),"Cannot force attacks on creative player");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void stickAttackCommandsNpcWithoutPlayerDamageOrRetaliation(GameTestHelper h) {
        try(var f=new Field(h)) {
            var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());f.extras.add(npc);npc.setPos(f.player.position().add(0,0,1));npc.setNoAi(true);npc.setNoGravity(true);NpcProfile.ensure(npc,npc.npcArchetype());h.getLevel().addFreshEntity(npc);
            float hp=npc.getHealth();f.player.attack(npc);
            h.assertTrue(npc.getHealth()==hp&&npc.getLastHurtByMob()==f.dummy&&npc.getTarget()==f.dummy,"Actual Player.attack is canceled and NPC revenge belongs to the dummy");
            h.assertTrue(f.dummy.getTarget()==npc&&!NpcProfile.shouldAttackPlayer(npc,f.player)&&!npc.getPersistentData().getCompound("ImmortalCultivationNpc").hasUUID("RetaliationPlayer"),"Controller owner receives no new NPC hostility");
            NpcProfile.tickCultivation(npc,npc.npcArchetype());h.assertTrue(npc.getTarget()==f.dummy,"NPC social update preserves the commanded opponent");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void stickAttackAndRightClickMakeNeutralMobFightDummy(GameTestHelper h) {
        try(var f=new Field(h)) {
            var wolf=EntityType.WOLF.create(h.getLevel());f.extras.add(wolf);wolf.setPos(f.player.position().add(0,0,1));wolf.setNoAi(true);wolf.setNoGravity(true);h.getLevel().addFreshEntity(wolf);
            float hp=wolf.getHealth();f.player.attack(wolf);
            h.assertTrue(wolf.getHealth()==hp&&wolf.getTarget()==f.dummy&&wolf.getLastHurtByMob()==f.dummy,"Stick hit is a command, never controller damage");
            h.assertTrue(f.dummy.getUUID().equals(wolf.getPersistentAngerTarget()),"Neutral persistent anger targets the dummy");
            wolf.setTarget(null);wolf.setLastHurtByMob(null);wolf.setPersistentAngerTarget(null);
            var event=new PlayerInteractEvent.EntityInteract(f.player,InteractionHand.MAIN_HAND,wolf);DummyControlStickItem.entity(event);
            h.assertTrue(event.isCanceled()&&wolf.getTarget()==f.dummy&&f.dummy.getTarget()==wolf,"Right click establishes the same reciprocal practice target");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void invalidStickBindingsNeverPunchAndOffhandDoesNotHijackRealWeapons(GameTestHelper h) {
        try(var f=new Field(h)) {
            var wolf=EntityType.WOLF.create(h.getLevel());f.extras.add(wolf);wolf.setPos(f.player.position().add(0,0,1));wolf.setNoAi(true);wolf.setNoGravity(true);h.getLevel().addFreshEntity(wolf);float hp=wolf.getHealth();
            var stick=f.player.getMainHandItem().copy();f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DUMMY_CONTROL_STICK.get()));f.player.attack(wolf);
            h.assertTrue(wolf.getHealth()==hp&&wolf.getTarget()==null&&f.dummy.getTarget()==null,"Unbound controller cannot hurt or command anything");
            f.dummy.controller=UUID.randomUUID();f.player.setItemInHand(InteractionHand.MAIN_HAND,stick);f.player.attack(wolf);
            h.assertTrue(wolf.getHealth()==hp&&wolf.getTarget()==null&&f.dummy.getTarget()==null,"Copied binding without ownership cannot command or damage targets");
            f.dummy.controller=f.player.getUUID();f.player.setItemInHand(InteractionHand.OFF_HAND,stick);f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.player.attack(wolf);
            h.assertTrue(wolf.getHealth()<hp&&wolf.getLastHurtByMob()==f.player,"An offhand controller leaves ordinary main-hand attacks alone");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void stationaryImmortalDummyStillReceivesRealDamageAndCanBeReset(GameTestHelper h) {
        try(var f=new Field(h)) {
            float hp=f.dummy.getHealth();h.assertTrue(f.dummy.hurt(f.dummy.damageSources().generic(),hp*2),"Dummy accepts a real damage event");
            h.assertTrue(f.dummy.isAlive()&&f.dummy.getHealth()>=1&&f.dummy.getHealth()<hp,"Prevent death leaves measurable damage and one remaining HP");
            var before=f.dummy.position();f.dummy.knockback(2,1,1);f.dummy.travel(new Vec3(1,0,1));
            h.assertTrue(f.dummy.position().distanceTo(before)<.00001,"Stationary default does not walk or knock back");
            f.dummy.resetVitals();h.assertTrue(f.dummy.getHealth()==hp,"Reset restores dummy after damage");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void placementAndBindingSurviveItemSerialization(GameTestHelper h) {
        try(var f=new Field(h)) {
            var point=h.absolutePos(new BlockPos(2,2,2));f.player.setPos(Vec3.atCenterOf(point).add(0,0,-2));
            f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TRAINING_DUMMY.get()));
            var context=new UseOnContext(f.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(point),Direction.UP,point,false));
            h.assertTrue(ModItems.TRAINING_DUMMY.get().useOn(context).consumesAction()&&f.player.getMainHandItem().isEmpty(),"Placement consumes one survival item");
            var placed=h.getLevel().getEntitiesOfClass(TrainingDummyEntity.class,new AABB(point).inflate(2));
            h.assertTrue(placed.stream().anyMatch(d->f.player.getUUID().equals(d.controller)),"Placed mannequin has actual placer ownership");f.extras.addAll(placed);
            var stack=new ItemStack(ModItems.DUMMY_CONTROL_STICK.get());DummyControlStickItem.bind(stack,f.dummy);
            var restored=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)stack.save(h.getLevel().registryAccess()));
            h.assertTrue(f.dummy.getUUID().equals(DummyControlStickItem.bound(restored)),"Bound UUID persists through item save/load");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void completeKnowledgeSnapshotRemainsSmallAndCodecRoundTrips(GameTestHelper h) {
        try(var f=new Field(h)) {
            h.assertTrue(f.edit("unlock_all_methods","","")&&f.edit("unlock_all_spells","",""),"Bulk unlock actions populate complete knowledge snapshot");
            var packet=new TrainingDummyPayloads.State(f.dummy.getUUID(),TrainingDummyControls.snapshot(f.dummy),true);
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {TrainingDummyPayloads.State.CODEC.encode(buffer,packet);h.assertTrue(buffer.readableBytes()<16384,"Full supported catalog snapshot stays below 16 KiB without NPC inventories");
                h.assertTrue(packet.equals(TrainingDummyPayloads.State.CODEC.decode(buffer)),"Small registry-backed state codec round-trips");
            }finally{buffer.release();}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void fullFreezeBlocksControlledSpellsAndDomainsUntilReset(GameTestHelper h) {
        try(var f=new Field(h)) {
            f.edit("spell_known",ModSpells.DIELANG_SHIELD,"");long now=h.getLevel().getGameTime();
            for(int i=0;i<10000&&!ColdTideFreeze.isFrozen(f.dummy);i++)ColdTideFreeze.expose(f.player,f.dummy,now+i*20);
            h.assertTrue(ColdTideFreeze.isFrozen(f.dummy)&&!f.dummy.cast(ModSpells.DIELANG_SHIELD)&&!f.edit("toggle","Domain",""),"Controller cannot bypass real full-freeze locks");
            f.dummy.resetVitals();h.assertTrue(!ColdTideFreeze.isFrozen(f.dummy)&&f.dummy.cast(ModSpells.DIELANG_SHIELD),"Explicit dummy reset clears freeze and permits real casting");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void destroyedDummyDropsNoCorpseSoulOrBlood(GameTestHelper h) {
        try(var f=new Field(h)) {
            var area=f.dummy.getBoundingBox().inflate(4);
            int corpses=h.getLevel().getEntitiesOfClass(CultivatorCorpseEntity.class,area).size();
            int souls=h.getLevel().getEntitiesOfClass(SoulEntity.class,area).size();
            int blood=h.getLevel().getEntitiesOfClass(BloodDropEntity.class,area).size();
            h.assertTrue(SoulEntity.valueFor(f.dummy)==0,"A wooden training dummy contains no harvestable soul");
            f.dummy.immortal=false;
            h.assertTrue(f.dummy.hurt(f.player.damageSources().playerAttack(f.player),f.dummy.getHealth()*2)
                    && !f.dummy.isAlive(),"Mortal dummy can be destroyed by a real player damage event");
            h.assertTrue(h.getLevel().getEntitiesOfClass(CultivatorCorpseEntity.class,area).size()==corpses,
                    "Destroying a dummy does not create a cultivator corpse");
            h.assertTrue(h.getLevel().getEntitiesOfClass(SoulEntity.class,area).size()==souls,
                    "Destroying a dummy does not create a soul");
            h.assertTrue(h.getLevel().getEntitiesOfClass(BloodDropEntity.class,area).size()==blood,
                    "A wooden dummy cannot yield blood");
        }h.succeed();
    }

    @GameTest(template="fate_test") public static void bulkUnlockPreservesMasteryAndPersistsExactSupportedCatalogs(GameTestHelper h) {
        try(var f=new Field(h)) {
            f.edit("mastery",CultivationMethods.BASIC_BREATHING,"753");
            String active=f.dummy.profile().getString("Method");
            var methods=new HashSet<>(CultivationMethods.allMethods().stream().map(CultivationMethods.MethodDef::id).toList());
            var spells=new HashSet<>(ModSpells.all().stream().map(ModSpells.SpellDef::id).filter(NpcSpellcasting::isNpcSupportedSpell).toList());
            for(int i=0;i<2;i++) {
                h.assertTrue(f.edit("unlock_all_methods","","")&&f.edit("unlock_all_spells","",""),"Both unlock actions can be repeated");
                h.assertTrue(new HashSet<>(TrainingDummyEntity.strings(f.dummy.profile(),"Methods")).equals(methods)
                        &&TrainingDummyEntity.strings(f.dummy.profile(),"Methods").size()==methods.size(),"All registered methods are learned without duplicates");
                h.assertTrue(new HashSet<>(TrainingDummyEntity.strings(f.dummy.profile(),"Spells")).equals(spells)
                        &&TrainingDummyEntity.strings(f.dummy.profile(),"Spells").size()==spells.size(),"All NPC-supported spells are learned without duplicates");
            }
            h.assertTrue(f.dummy.profile().getString("Method").equals(active)
                    &&f.dummy.profile().getCompound("MethodProficiencies").getInt(active)==753,"Bulk learning preserves active method and existing mastery");
            h.assertTrue(!TrainingDummyEntity.strings(f.dummy.profile(),"Spells").contains(ModSpells.IGNITE_FLARE),"NPC Ignite Flare ban survives bulk unlock");
            var tag=new CompoundTag();f.dummy.addAdditionalSaveData(tag);var loaded=ModEntities.TRAINING_DUMMY.get().create(h.getLevel());
            try {
                loaded.readAdditionalSaveData(tag);
                h.assertTrue(new HashSet<>(TrainingDummyEntity.strings(loaded.profile(),"Methods")).equals(methods)
                        &&new HashSet<>(TrainingDummyEntity.strings(loaded.profile(),"Spells")).equals(spells),"Unlocked knowledge persists across reload");
            }finally{loaded.discard();}
        }h.succeed();
    }

}
