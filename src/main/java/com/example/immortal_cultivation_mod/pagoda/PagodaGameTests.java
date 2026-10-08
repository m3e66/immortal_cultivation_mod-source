package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.RealmKillProtection;
import com.example.immortal_cultivation_mod.item.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class PagodaGameTests {
    private static PagodaData.Tower state(GameTestHelper h,double power){
        var server=h.getLevel().getServer();
        var tower=new PagodaData.Tower(UUID.randomUUID(),UUID.randomUUID(),h.getLevel().dimension().location(),h.absolutePos(new BlockPos(4,3,4)),1200);
        tower.ownerPower=power;PagodaData.get(server).towers.put(tower.id,tower);PagodaData.get(server).setDirty();return tower;
    }
    @GameTest(template="fate_test")
    public static void formulasAndBoundaryBands(GameTestHelper h){
        h.assertTrue(Math.abs(PagodaPower.breakPower(200,100,10,20,.5,7)-352)<.001,"Break Power follows every specified term");
        h.assertTrue(Math.abs(PagodaPower.containment(200,5,500,.5)-415)<.001,"Containment follows every specified term");
        h.assertTrue(PagodaPower.band(59.999,100)==PagodaPower.Band.TRAPPED,"Below 60% fully trapped");
        h.assertTrue(PagodaPower.band(60,100)==PagodaPower.Band.SLOW,"60% begins slow breakout");
        h.assertTrue(PagodaPower.band(100,100)==PagodaPower.Band.SERIOUS,"100% begins serious breakout");
        h.assertTrue(PagodaPower.band(150,100)==PagodaPower.Band.FORCED,"150% forced escape");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void savedStatePreservesIndependentPrisoners(GameTestHelper h){
        var data=new PagodaData();var first=new PagodaData.Tower(UUID.randomUUID(),UUID.randomUUID(),h.getLevel().dimension().location(),new BlockPos(5,64,8),1200);
        var second=new PagodaData.Tower(UUID.randomUUID(),first.owner,first.origin,new BlockPos(20,70,30),1200);
        var prisoner=new PagodaData.Prisoner(UUID.randomUUID(),first.origin,new BlockPos(2,65,9),true);
        prisoner.progress=75;prisoner.lastAttack=90;prisoner.forcedSince=85;first.prisoners.put(prisoner.id,prisoner);
        first.age=99;first.health=230;first.injectedDamage=15;first.ownerDead=true;first.generated=true;
        data.towers.put(first.id,first);data.towers.put(second.id,second);
        var loaded=PagodaData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var restored=loaded.towers.get(first.id);
        h.assertTrue(restored.age==99&&restored.health==230&&restored.injectedDamage==15&&restored.generated&&restored.ownerDead,"Lifetime, damage, Qi and world state survive serialization");
        h.assertTrue(restored.prisoners.get(prisoner.id).forcedSince==85&&restored.prisoners.get(prisoner.id).progress==75,"Each prisoner's progress and forced escape timer survive saves");
        h.assertTrue(loaded.towers.get(second.id).prisoners.isEmpty(),"UUID worlds never share prisoners");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void defaultsAndForgeIntegration(GameTestHelper h){
        h.assertTrue(PagodaConfig.DURATION.getDefault()==60&&PagodaConfig.COOLDOWN.getDefault()==90&&PagodaConfig.MAX_PRISONERS.getDefault()==16,"Requested duration, cooldown and capacity defaults");
        h.assertTrue(PagodaConfig.QI_COST.getDefault()==300&&PagodaConfig.TARGET_COST.getDefault()==25&&!PagodaConfig.CAPTURE_PLAYERS.getDefault(),"Requested Qi costs, players protected by default");
        var recipe=HigherWeaponRecipes.recipe("baota");
        h.assertTrue(recipe!=null&&HigherWeaponRecipes.item("baota")==ModItems.BAOTA.get(),"Forge recipe resolves registered pagoda weapon");
        var stack=new ItemStack(ModItems.BAOTA.get());var qiTag=new CompoundTag();qiTag.putDouble("immortal_cultivation_mod.weapon_qi",100);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(qiTag));
        h.assertTrue(QiInfusedWeapon.injectedQiDamage(stack)==5,"Pagoda uses existing 100 Qi = 5 damage logic");h.succeed();
    }
    @GameTest(template="fate_test")
    public static void captureEligibilityProtectsOwnedAndCreativeEntities(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-test"));player.setGameMode(GameType.SURVIVAL);
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(3,3,3));zombie.setNoAi(true);
        var wolf=h.spawn(EntityType.WOLF,new BlockPos(4,3,3));wolf.setTame(true,true);wolf.setOwnerUUID(UUID.randomUUID());wolf.setTarget(player);
        var horse=h.spawn(EntityType.HORSE,new BlockPos(5,3,3));horse.setTamed(true);
        var creative=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-creative"));creative.setGameMode(GameType.CREATIVE);
        h.assertTrue(PagodaSystem.eligible(player,zombie),"Hostile zombie eligible");
        h.assertTrue(!PagodaSystem.eligible(player,player)&&!PagodaSystem.eligible(player,wolf)&&!PagodaSystem.eligible(player,horse)&&!PagodaSystem.eligible(player,creative),"Owner, all owned/tamed mobs and creative players protected");
        zombie.discard();wolf.discard();horse.discard();player.discard();creative.discard();h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=160)
    public static void isolatedWorldTransferPreservesEntityAndReturnsOnce(GameTestHelper h){
        var server=h.getLevel().getServer();var first=state(h,10000);var second=state(h,10000);
        var a=PagodaWorlds.getOrCreate(server,first);var b=PagodaWorlds.getOrCreate(server,second);
        h.assertTrue(a!=b&&!a.dimension().equals(b.dimension()),"Separate real dimensions for each persistent tower UUID");
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPersistenceRequired();zombie.setCustomName(Component.literal("Saved prisoner"));
        zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));zombie.getPersistentData().putString("ExternalModTest","preserved");
        UUID id=zombie.getUUID();
        h.startSequence().thenWaitUntil(()->h.assertTrue(a.isPositionEntityTicking(new BlockPos(3,65,3))&&a.areEntitiesLoaded(0L),"Interior entity chunk ready"))
                .thenExecute(()->h.assertTrue(PagodaSystem.capture(server,first,zombie),"Capture transfers original entity through vanilla dimension transition"))
                .thenIdle(10).thenExecute(()->{
            var moved=(LivingEntity)a.getEntity(id);
            h.assertTrue(moved!=null&&h.getLevel().getEntity(id)==null&&b.getEntity(id)==null,"Only original tower owns UUID; no external copy");
            h.assertTrue(moved.getMainHandItem().is(Items.DIAMOND_SWORD)&&moved.getCustomName().getString().equals("Saved prisoner")&&moved.getPersistentData().getString("ExternalModTest").equals("preserved"),"Equipment, name and third-party persistent data preserved");
            var prisoner=first.prisoners.get(id);h.assertTrue(PagodaSystem.release(server,first,prisoner),"Survivor returns to safe original dimension");
            h.assertTrue(first.prisoners.isEmpty()&&h.getLevel().getEntity(id)!=null&&a.getEntity(id)==null,"Release removes record and does not duplicate prisoner");
            first.closing=true;second.closing=true;
        }).thenSucceed();
    }
    @GameTest(template="fate_test",timeoutTicks=200)
    public static void forcedEscapeTimerIgnoresAttackResets(GameTestHelper h){
        var server=h.getLevel().getServer();var tower=state(h,0);tower.health=tower.maxHealth=1;tower.age=100;
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPersistenceRequired();UUID id=zombie.getUUID();
        var inside=PagodaWorlds.getOrCreate(server,tower);
        h.startSequence().thenWaitUntil(()->h.assertTrue(inside.isPositionEntityTicking(new BlockPos(3,65,3))&&inside.areEntitiesLoaded(0L),"Interior ready"))
                .thenExecute(()->h.assertTrue(PagodaSystem.capture(server,tower,zombie),"Capture test prisoner"))
                .thenIdle(4).thenExecute(()->h.assertTrue(tower.prisoners.containsKey(id),"Strong prisoner receives short struggle, not instant escape"))
                .thenIdle(24).thenExecute(()->{
                    h.assertTrue(!tower.prisoners.containsKey(id)&&h.getLevel().getEntity(id)!=null,"Forced escape completes after 20 ticks independent of last attack");tower.closing=true;
                }).thenSucceed();
    }
    @GameTest(template="fate_test")
    public static void missingPrisonerKeepsWorldPendingRelease(GameTestHelper h){
        var server=h.getLevel().getServer();var tower=state(h,1000);PagodaWorlds.getOrCreate(server,tower);
        var p=new PagodaData.Prisoner(UUID.randomUUID(),tower.origin,tower.position,true);tower.prisoners.put(p.id,p);tower.closing=true;
        h.assertTrue(!PagodaSystem.release(server,tower,p)&&tower.prisoners.containsKey(p.id),"Offline UUID is retained, never considered dead or recreated");
        tower.prisoners.clear();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void ownerDeathWeakensWithoutDeletingPrisoners(GameTestHelper h){
        var tower=state(h,200);double before=PagodaSystem.containment(h.getLevel().getServer(),tower);tower.ownerDead=true;
        h.assertTrue(Math.abs(PagodaSystem.containment(h.getLevel().getServer(),tower)-before*.2)<.001&&!tower.closing,"Death reduces containment by 80%, retaining prison lifetime");tower.closing=true;h.succeed();
    }
    @GameTest(template="fate_test")
    public static void interiorAllowsAdminKillEvenAtHuashen(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();var tower=state(h,1000);var inside=PagodaWorlds.getOrCreate(server,tower);
        var player=new FakePlayer(inside,new GameProfile(UUID.randomUUID(),"pagoda-kill")){@Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}};
        ModAttachments.setData(player,ModAttachments.CultivationData.createDefault().withCultivationLevel(CultivationLevels.REALM_HUASHEN+CultivationLevels.STAGE_EARLY));
        var doll=new ItemStack(ModItems.TRIBULATION_DOLL.get());
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("charm").getStacks().setStackInSlot(0,doll);
        var pendant=new ItemStack(ModItems.PROTECTIVE_JADE_PENDANT.get());player.getInventory().setItem(1,pendant);
        h.assertTrue(!RealmKillProtection.protects(player),"Pagoda never overrides admin /kill");
        var source=server.createCommandSourceStack().withLevel(inside).withEntity(player).withSuppressedOutput();
        int killed=server.getCommands().getDispatcher().execute("kill",source);
        h.assertTrue(killed==1&&!player.isAlive()&&doll.getCount()==1&&pendant.getCount()==1,"Actual admin kill bypasses revival without consuming equipment");player.discard();tower.closing=true;h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=200)
    public static void destinationProtectionVetoRestoresOriginalEntity(GameTestHelper h){
        var server=h.getLevel().getServer();var tower=state(h,1000);var inside=PagodaWorlds.getOrCreate(server,tower);
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));UUID id=zombie.getUUID();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> veto=event->{if(event.getLevel()==inside&&event.getEntity().getUUID().equals(id))event.setCanceled(true);};
        h.startSequence().thenWaitUntil(()->h.assertTrue(inside.isPositionEntityTicking(new BlockPos(3,65,3))&&inside.areEntitiesLoaded(0L),"Interior ready"))
                .thenExecute(()->{
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(veto);
                    try{
                        h.assertTrue(!PagodaSystem.capture(server,tower,zombie),"Destination protection can veto capture");
                        h.assertTrue(h.getLevel().getEntity(id)==zombie&&zombie.isAlive()&&zombie.getMainHandItem().is(Items.DIAMOND_SWORD),"Original entity restored intact when destination join rejects vanilla transfer; found="+h.getLevel().getEntity(id)+", alive="+zombie.isAlive()+", added="+zombie.isAddedToLevel()+", sword="+zombie.getMainHandItem().is(Items.DIAMOND_SWORD));
                        h.assertTrue(tower.prisoners.isEmpty()&&inside.getEntity(id)==null,"No ghost record or duplicate destination entity");
                    }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(veto);zombie.discard();tower.closing=true;}
                }).thenSucceed();
    }
    @GameTest(template="fate_test")
    public static void sealDamageUsesInfusedQiAndRejectsWeakBreakouts(GameTestHelper h){
        var tower=state(h,200);tower.age=100;
        var attacker=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-seals"));
        var record=new PagodaData.Prisoner(attacker.getUUID(),tower.origin,tower.position,true);tower.prisoners.put(record.id,record);
        var weapon=new ItemStack(ModItems.BAOTA.get());var tag=new CompoundTag();tag.putDouble("immortal_cultivation_mod.weapon_qi",100);
        weapon.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));attacker.setItemSlot(EquipmentSlot.MAINHAND,weapon);
        var core=new PagodaEntity(com.example.immortal_cultivation_mod.entity.ModEntities.PAGODA.get(),h.getLevel());core.towerId=tower.id;core.setCore(true);
        h.assertTrue(!PagodaSystem.damage(core,attacker.damageSources().playerAttack(attacker),6)&&QiInfusedWeapon.getQi(weapon)==100&&record.progress==0,"Trapped prisoners cannot damage seals or spend Qi on rejected attacks");
        ModAttachments.setData(attacker,ModAttachments.CultivationData.createDefault().withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY));
        h.assertTrue(PagodaSystem.damage(core,attacker.damageSources().playerAttack(attacker),6)&&record.progress>.8&&QiInfusedWeapon.getQi(weapon)==90,"Accepted seal hit adds 5 injected damage and pays existing 10% Qi cost once");
        h.assertTrue(Math.abs(tower.health-498.35)<.01,"Core damage includes exact injected bonus");
        tower.closing=true;tower.prisoners.clear();attacker.discard();core.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void ownerControlsRemainAvailableDuringCastCooldown(GameTestHelper h){
        var tower=state(h,200);var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-owner"));
        tower.owner=owner.getUUID();owner.setPos(net.minecraft.world.phys.Vec3.atCenterOf(tower.position));tower.health=400;
        ModAttachments.setData(owner,ModAttachments.CultivationData.createDefault().withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY).withQi(1000));
        var stack=new ItemStack(ModItems.BAOTA.get());owner.setItemSlot(EquipmentSlot.MAINHAND,stack);PagodaItem.startCooldown(owner);
        h.assertTrue(PagodaItem.cooldownRemaining(owner)>0&&!owner.getCooldowns().isOnCooldown(stack.getItem()),"Persistent cast timer does not block item-use owner controls");
        owner.setShiftKeyDown(true);stack.getItem().use(h.getLevel(),owner,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!tower.closing&&ModAttachments.getData(owner).qi()==1000,"Shift use requests entry without reinforcement or recall while loading");
        owner.setShiftKeyDown(false);stack.getItem().use(h.getLevel(),owner,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(tower.closing&&ModAttachments.getData(owner).qi()==1000&&PagodaItem.cooldownRemaining(owner)>0,"Normal right-click recalls during summon cooldown without spending Qi");owner.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void suctionPermissionDoesNotExecuteWeaponAttacks(GameTestHelper h){
        var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-no-attack"));
        owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.SEVEN_STAR_SWORD.get()));
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);float health=zombie.getHealth();
        int[] attacks={0};
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> listener=e->{if(e.getEntity()==owner)attacks[0]++;};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{for(int i=0;i<40;i++)h.assertTrue(PagodaSystem.captureAllowed(owner,zombie),"Normal capture probe permitted");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(attacks[0]==0&&zombie.getHealth()==health,"Suction never posts attacks or damages target with held weapon");owner.discard();zombie.discard();h.succeed();
    }
    @GameTest(template="fate_test")
    public static void itemIdentityAndNineFloorCapacityPersist(GameTestHelper h){
        var first=new ItemStack(ModItems.BAOTA.get());var second=new ItemStack(ModItems.BAOTA.get());
        UUID id=PagodaItem.identity(first);
        h.assertTrue(id.equals(PagodaItem.identity(first))&&!id.equals(PagodaItem.identity(second)),"Each actual item receives stable independent identity");
        h.assertTrue(PagodaFloors.unlocked(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY)==2&&PagodaFloors.unlocked(CultivationLevels.REALM_DUJIE+CultivationLevels.STAGE_EARLY)==9,"Nine cultivation realms unlock one floor each");
        var tower=state(h,200);tower.container=id;tower.unlockedFloors=2;tower.layoutVersion=2;
        for(int i=0;i<20;i++){var p=new PagodaData.Prisoner(UUID.randomUUID(),tower.origin,tower.position,false);p.floor=i/10;tower.prisoners.put(p.id,p);}
        var visitor=new PagodaData.Prisoner(tower.owner,tower.origin,tower.position,true);visitor.visitor=true;tower.prisoners.put(visitor.id,visitor);
        h.assertTrue(!PagodaFloors.hasRoom(tower)&&PagodaFloors.occupancy(tower,0)==10&&PagodaFloors.occupancy(tower,1)==10,"Ten prisoners per floor; owner does not consume capacity");
        var data=PagodaData.get(h.getLevel().getServer());var restored=PagodaData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).towers.get(tower.id);
        h.assertTrue(restored.container.equals(id)&&restored.unlockedFloors==2&&restored.prisoners.get(visitor.id).visitor&&restored.prisoners.values().stream().filter(p->p.floor==1).count()==10,"Item binding, floor assignment and visiting owner survive reload");
        tower.prisoners.clear();tower.closing=true;h.succeed();
    }
    @GameTest(template="fate_test")
    public static void deployedTowerPaysQiWithoutSpendingHealth(GameTestHelper h){
        var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-upkeep"));
        int cost=PagodaConfig.UPKEEP.get();float hp=owner.getHealth();
        ModAttachments.setData(owner,ModAttachments.CultivationData.createDefault().withQi(cost+1));
        h.assertTrue(PagodaSystem.payUpkeep(owner)&&ModAttachments.getData(owner).qi()==1,"Exactly configured Qi drain paid");
        if(cost>1)h.assertTrue(!PagodaSystem.payUpkeep(owner)&&ModAttachments.getData(owner).qi()==1,"Insufficient Qi declines upkeep without debt");
        h.assertTrue(owner.getHealth()==hp,"Upkeep cannot substitute blood or health");owner.discard();h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=400)
    public static void actualInteriorHasNineFloorsAndTenCaptivesPerFloor(GameTestHelper h){
        var tower=state(h,1000);tower.unlockedFloors=2;var server=h.getLevel().getServer();var inside=PagodaWorlds.getOrCreate(server,tower);
        h.startSequence().thenWaitUntil(()->h.assertTrue(inside.isPositionEntityTicking(new BlockPos(3,65,3))&&inside.areEntitiesLoaded(0L),"Interior ready"))
            .thenExecute(()->{
                for(int floor=0;floor<9;floor++)h.assertTrue(inside.getBlockState(new BlockPos(0,64+floor*9,0)).is(net.minecraft.world.level.block.Blocks.GLOWSTONE),"All nine floors generated");
                PagodaWorlds.updateFloorLocks(inside,tower);
                h.assertTrue(inside.getBlockState(new BlockPos(2,82,4)).is(net.minecraft.world.level.block.Blocks.BARRIER),"Third floor locked for Zhuji");
                for(int i=0;i<20;i++){
                    var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setPersistenceRequired();
                    h.assertTrue(PagodaSystem.capture(server,tower,zombie),"Unlocked floors accept prisoner "+i);
                }
                var extra=h.spawn(EntityType.ZOMBIE,new BlockPos(4,3,4));extra.setNoAi(true);
                h.assertTrue(!PagodaSystem.capture(server,tower,extra)&&PagodaFloors.occupancy(tower,0)==10&&PagodaFloors.occupancy(tower,1)==10,"Twenty-first target rejected; each room max ten");extra.discard();tower.closing=true;
            }).thenSucceed();
    }

    @GameTest(template="fate_test",timeoutTicks=200)
    public static void ownerCanVisitAndExitWithoutRecallingTower(GameTestHelper h){
        var tower=state(h,200);tower.unlockedFloors=2;
        var owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pagoda-visitor"));
        var channel=new io.netty.channel.embedded.EmbeddedChannel(owner.connection.getConnection());
        tower.owner=owner.getUUID();owner.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(tower.position));
        h.getLevel().addNewPlayer(owner);
        ModAttachments.setData(owner,ModAttachments.CultivationData.createDefault().withCultivationLevel(CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY).withQi(1000));
        var inside=PagodaWorlds.getOrCreate(h.getLevel().getServer(),tower);
        h.startSequence().thenWaitUntil(()->h.assertTrue(inside.isPositionEntityTicking(new BlockPos(3,65,3))&&inside.areEntitiesLoaded(0L),"Interior ready"))
            .thenExecute(()->{
                h.assertTrue(PagodaSystem.enterOwner(owner,tower)&&owner.level()==inside&&PagodaSystem.ownerInside(tower),"Owner enters actual UUID dimension as visitor");
                h.assertTrue(!owner.hasEffect(com.example.immortal_cultivation_mod.effect.ModEffects.WEIYA_SUPPRESSED)&&PagodaFloors.occupancy(tower,0)==0,"Owner exempt from own pressure and captive capacity");
                h.assertTrue(PagodaSystem.release(h.getLevel().getServer(),tower,tower.prisoners.get(owner.getUUID()))&&owner.level()==h.getLevel()&&!tower.closing,"Owner exits safely while tower remains deployed");
                owner.discard();channel.close();tower.closing=true;
            }).thenSucceed();
    }

    @GameTest(template="fate_test",timeoutTicks=100)
    public static void deployedPagodaDoesNotSpin(GameTestHelper h) {
        var tower=state(h,200);
        tower.age=120;
        var entity=h.spawn(com.example.immortal_cultivation_mod.entity.ModEntities.PAGODA.get(),new BlockPos(4,3,4));
        entity.towerId=tower.id;
        tower.entity=entity.getUUID();
        entity.setYRot(87);entity.yBodyRot=-55;entity.yHeadRot=77;
        h.startSequence().thenExecute(()->PagodaSystem.tick(h.getLevel().getServer()))
                .thenIdle(5).thenExecute(()->{
                    h.assertTrue(entity.isAlive()&&entity.getYRot()==0&&entity.yBodyRot==0&&entity.yHeadRot==0,
                            "Deployed pagoda stays anchored through server updates instead of rotating with age");
                    tower.closing=true;
                    PagodaSystem.tick(h.getLevel().getServer());
                }).thenSucceed();
    }

}
