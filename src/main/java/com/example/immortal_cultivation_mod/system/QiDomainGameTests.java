package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.*;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.TemporaryTerrainEntity;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.example.immortal_cultivation_mod.physics.QiFlowPhysics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("immortal_cultivation_qi_domain")
@PrefixGameTestTemplate(false)
public final class QiDomainGameTests {
    private static void prepare(LifeSimulationGameTests.Player f,String realm,int percent,int mastery) {
        var p=f.player;
        ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(realm)
                .withActiveCultivationMethod(CultivationMethods.CHANGQING_JUE).withMethodProficiencies(Map.of(CultivationMethods.CHANGQING_JUE,mastery)).withQi(1_000_000));
        CombatStateManager.setEnabled(p,true);CombatStateManager.toggleManualCombat(p);
        p.getPersistentData().putInt("ImmortalCultivationQiReleasePercent",percent);
    }
    private static void cleanup(LifeSimulationGameTests.Player f){QiDomainSystem.stop(f.player);f.close();}
    @GameTest(template="fate_test") public static void pollenCoversFullTwentyBlockRadius(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            for(long time:new long[]{0,120,24000}) {
                int outer=0;
                for(int i=0;i<QiDomainSystem.MOTES;i++) {
                    Vec3 offset=QiDomainSystem.motePosition(f.player,i,time).subtract(f.player.position());
                    double radius=offset.horizontalDistance();
                    h.assertTrue(radius>=10-1E-6&&radius<=20+1E-6,"Pollen spreads across 10–20 blocks, never crowded around owner");
                    if(radius>19.99)outer++;
                }
                h.assertTrue(outer>=3,"Moving pollen reaches the full twenty-block boundary");
            }
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void realmAndCombatGates(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            prepare(f,CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_LATE,200,500);
            QiDomainSystem.toggle(f.player);h.assertTrue(!QiDomainSystem.blasting(f.player),"Foundation cannot release qi");
        }
        try(var f=new LifeSimulationGameTests.Player(h)) {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);
            CombatStateManager.setEnabled(f.player,false);QiDomainSystem.toggle(f.player);
            h.assertTrue(!QiDomainSystem.blasting(f.player),"Combat required");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void releaseRequiresQiAndToggleIsEdgeSafe(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);
            QiDomainSystem.toggle(f.player);h.assertTrue(QiDomainSystem.blasting(f.player),"Golden Core can release");
            QiDomainSystem.toggle(f.player);h.assertTrue(QiDomainSystem.blasting(f.player),"Duplicate same-tick packet cannot retoggle");
            QiDomainSystem.stop(f.player);ModAttachments.setData(f.player,ModAttachments.getData(f.player).withQi(0).withBlood(0));
        }finally{cleanup(f);}
        try(var empty=new LifeSimulationGameTests.Player(h)) {
            prepare(empty,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,100,0);
            ModAttachments.setData(empty.player,ModAttachments.getData(empty.player).withQi(0).withBlood(0));
            QiDomainSystem.toggle(empty.player);h.assertTrue(!QiDomainSystem.blasting(empty.player),"Empty reserve cannot start");
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void scrollValidatedAndBounded(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,180,0);QiDomainSystem.toggle(f.player);
            QiDomainSystem.adjust(f.player,99);h.assertTrue(QiDomainSystem.percent(f.player)==180,"Shift required server-side");
            f.player.setShiftKeyDown(true);QiDomainSystem.adjust(f.player,Integer.MAX_VALUE);
            h.assertTrue(QiDomainSystem.percent(f.player)==200,"Oversized input clamped without overflow");
            QiDomainSystem.adjust(f.player,-99);h.assertTrue(QiDomainSystem.percent(f.player)==200,"Only one scroll packet per tick");
            QiDomainSystem.stop(f.player);QiDomainSystem.adjust(f.player,1);h.assertTrue(!QiDomainSystem.blasting(f.player),"Scroll cannot start release");
        }finally{cleanup(f);}h.succeed();
    }
    @GameTest(template="fate_test") public static void upkeepOncePerSecondAndStopsAtEmpty(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,0);QiDomainSystem.toggle(f.player);
            int before=ModAttachments.getData(f.player).qi();QiDomainSystem.tickAll();int after=ModAttachments.getData(f.player).qi();
            h.assertTrue(after<before,"First active tick charges upkeep");
            QiDomainSystem.tickAll();h.assertTrue(ModAttachments.getData(f.player).qi()==after,"No repeated same-tick upkeep");
            h.assertTrue(QiDomainSystem.upkeep(f.player,200)>=QiDomainSystem.upkeep(f.player,100)*3,"Quadratic intensity cost");
            QiDomainSystem.view(f.player).lastUpkeep-=20;
            ModAttachments.setData(f.player,ModAttachments.getData(f.player).withQi(0).withBlood(0));QiDomainSystem.tickAll();
            h.assertTrue(!QiDomainSystem.blasting(f.player),"Resource exhaustion stops domain");
        }finally{cleanup(f);}h.succeed();
    }
    @GameTest(template="fate_test") public static void overlappingDomainsRespectMethodAndRange(GameTestHelper h) {
        var low=new LifeSimulationGameTests.Player(h);var high=new LifeSimulationGameTests.Player(h);boolean pvp=h.getLevel().getServer().isPvpAllowed();try {
            h.getLevel().getServer().setPvpAllowed(false);
            h.assertTrue(!QiDomainSystem.canAffect(low.player,high.player),"PvP-disabled players cannot suppress or harm each other");
            h.getLevel().getServer().setPvpAllowed(true);
            prepare(low,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,0);
            prepare(high,CultivationLevels.REALM_YUANYING+CultivationLevels.STAGE_EARLY,200,0);
            high.player.setPos(low.player.position().add(5,0,0));QiDomainSystem.toggle(low.player);QiDomainSystem.toggle(high.player);QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(low.player).power==1&&QiDomainSystem.view(high.player).power==1,"Equal method limits and proficiency lock regardless of caster realm");
            h.assertTrue(QiDomainSystem.view(low.player).opponent==high.player.getId(),"Clash descriptor names actual opponent");
            h.assertTrue(!QiDomainSystem.contains(low.player,low.player.position().add(4,0,0)),"Actual lower-side volume ends at shared contact plane");
            high.player.setPos(low.player.position().add(41,0,0));QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(low.player).power==1&&QiDomainSystem.view(low.player).opponent==-1,"Nonoverlapping domains no longer suppress");
            h.assertTrue(QiDomainSystem.contains(low.player,low.player.position().add(4,0,0)),"Separating restores full domain area");
        }finally{h.getLevel().getServer().setPvpAllowed(pvp);cleanup(low);cleanup(high);}h.succeed();
    }
    @GameTest(template="fate_test") public static void attacksSelectFistOrWeaponAndOrdinaryUseExpires(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_ZHUJI+CultivationLevels.STAGE_EARLY,100,0);
            f.player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);QiDomainSystem.attack(f.player);
            h.assertTrue(QiDomainSystem.view(f.player).attackMode==QiDomainSystem.FIST&&!QiDomainSystem.blasting(f.player),"Low realm fist coating works without domain");
            f.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));QiDomainSystem.attack(f.player);
            h.assertTrue(QiDomainSystem.view(f.player).attackMode==QiDomainSystem.WEAPON,"Weapon attack selects full weapon coating");
            QiDomainSystem.view(f.player).until=f.player.level().getGameTime()-1;QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(f.player)==null,"Ordinary qi visuals expire");
        }finally{cleanup(f);}h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=80) public static void evergreenMasteryContactAndOwnerSafety(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);
        prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,499);QiDomainSystem.toggle(f.player);
        var contact=QiDomainSystem.motePosition(f.player,0,h.getLevel().getGameTime()+4);
        var woodArea=new AABB(contact,contact).inflate(QiWoodSpikes.BOUNDS_RADIUS);
        long beforeWood=countOakLogs(h,woodArea);
        var cow=EntityType.COW.create(h.getLevel());cow.setNoGravity(true);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);cow.setHealth(500);
        cow.setPos(QiDomainSystem.motePosition(f.player,0,h.getLevel().getGameTime()+4));h.getLevel().addFreshEntity(cow);
        h.runAfterDelay(6,()->{
            try {
                h.assertTrue(cow.getHealth()==500,"499 proficiency cannot trigger special hazards");
                h.assertTrue(countOakLogs(h,woodArea)==beforeWood,"Below Great mastery, pollen cannot place wooden terrain");
                ModAttachments.setData(f.player,ModAttachments.getData(f.player).withMethodProficiencies(Map.of(CultivationMethods.CHANGQING_JUE,500)));
                cow.setPos(QiDomainSystem.motePosition(f.player,0,h.getLevel().getGameTime()+2));
                h.runAfterDelay(4,()->{try {
                    h.assertTrue(cow.getHealth()<500,"500 proficiency pollen contact detonates actual spike damage");
                    h.assertTrue(countOakLogs(h,woodArea)>beforeWood,"Great-mastery contact places real oak log blocks in the world");
                    h.assertTrue(!QiDomainSystem.canAffect(f.player,f.player),"Owner immune to own domain");
                    var point=QiDomainSystem.motePosition(f.player,0,100);f.player.setPos(f.player.position().add(3,0,0));
                    h.assertTrue(QiDomainSystem.motePosition(f.player,0,100).subtract(point).distanceTo(new Vec3(3,0,0))<1E-8,"Motes follow owner");
                    h.succeed();
                }finally{cow.discard();cleanup(f);}});
            }catch(Throwable error){cow.discard();cleanup(f);throw error;}
        });
    }
    @GameTest(template="fate_test") public static void everyMethodHasDistinctDomainAndPhysicsStaysFinite(GameTestHelper h) {
        h.assertTrue(MethodDomainStyle.coversAllMethods()&&MethodDomainStyle.values().length==17,"All 17 registered methods covered");
        h.assertTrue(java.util.Arrays.stream(MethodDomainStyle.values()).map(s->s.kind).distinct().count()==17,"Each method has its own gameplay behavior");
        var flow=new QiFlowPhysics(Vec3.ZERO);
        for(int t=0;t<1000;t++)flow.tick(new Vec3(t*.1,0,0),t*.12,3,.8,new Vec3(.1,0,0));
        flow.tick(new Vec3(1000,80,-500),1,3,.8,Vec3.ZERO);
        for(int i=0;i<QiFlowPhysics.NODES;i++)h.assertTrue(QiFlowPhysics.finite(flow.point(i,1))&&flow.point(i,1).distanceTo(new Vec3(1000,80,-500))<8,"Teleport resets bounded spring nodes");
        flow.tick(Vec3.ZERO,Double.NaN,3,.8,Vec3.ZERO);flow.tick(Vec3.ZERO,1,3,.8,new Vec3(Double.NaN,0,0));
        for(int i=0;i<QiFlowPhysics.NODES;i++)h.assertTrue(QiFlowPhysics.finite(flow.point(i,1)),"Malformed motion cannot poison geometry");h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=80) public static void evergreenCreatesCollidableLogsAndExpires(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);
        // Center the entire enlarged eruption in one chunk, above the other terrain fixtures.
        var ownerPos=f.player.blockPosition();
        var center=new Vec3((ownerPos.getX()>>4)*16+8.5,ownerPos.getY()+24.5,(ownerPos.getZ()>>4)*16+8.5);
        var pattern=QiWoodSpikes.pattern(center);
        Map<BlockPos,BlockState> placed;
        try {
            h.assertTrue(pattern.size()>64&&pattern.size()<=QiWoodSpikes.MAX_BLOCKS,"Thick radial eruption exceeds the old size while remaining bounded");
            placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);
            h.assertTrue(placed.equals(pattern),"Open air receives the entire enlarged pattern without clipping at the old 64-block cap");
            h.assertTrue(placed.keySet().stream().map(pos->new net.minecraft.world.level.ChunkPos(pos)).distinct().count()==1,
                    "Large-placement regression exercises more than 64 blocks in one chunk");
            boolean longReach=false;
            for(var entry:placed.entrySet()) {
                var pos=entry.getKey();var state=h.getLevel().getBlockState(pos);
                h.assertTrue(state.equals(entry.getValue())&&state.is(Blocks.OAK_LOG),"Every returned spike exists as an oak log");
                h.assertTrue(!state.getCollisionShape(h.getLevel(),pos).isEmpty()
                        &&!h.getLevel().noCollision(new AABB(pos).deflate(.05)),"Log cells physically block movement");
                h.assertTrue(QiWoodSpikes.intersects(placed,new AABB(pos).deflate(.1)),"Damage reaches every actually placed log cell");
                longReach|=Vec3.atCenterOf(pos).distanceTo(center)>5;
            }
            h.assertTrue(longReach,"Enlarged wooden spikes extend more than five blocks from their origin");
            h.assertTrue(!QiWoodSpikes.intersects(placed,new AABB(center.add(12,0,0),center.add(13,1,1))),"No damage outside actual block reach");
            var anchors=h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new AABB(center,center).inflate(8));
            h.assertTrue(!anchors.isEmpty(),"Timed terrain anchors own the eruption");
            for(var anchor:anchors)for(var pos:anchor.placedStates().keySet())
                h.assertTrue((pos.getX()>>4)==(anchor.blockPosition().getX()>>4)&&(pos.getZ()>>4)==(anchor.blockPosition().getZ()>>4),"Each anchor shares its blocks' chunk for saved expiry");
        } catch(Throwable error) {clearWood(h,center,pattern);cleanup(f);throw error;}
        h.runAfterDelay(QiWoodSpikes.LIFETIME_TICKS+3,()->{try {
            h.assertTrue(placed.keySet().stream().allMatch(pos->h.getLevel().getBlockState(pos).isAir()),"All temporary logs disappear after their forty-tick lifetime");
            h.succeed();
        }finally{clearWood(h,center,pattern);cleanup(f);}});
    }
    @GameTest(template="fate_test") public static void evergreenPreservesTerrainAndHonorsPlacementProtection(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            var center=f.player.position().add(0,12,0);var pattern=QiWoodSpikes.pattern(center);
            var positions=new java.util.ArrayList<>(pattern.keySet());
            var stone=positions.get(0);var chest=positions.get(1);var water=positions.get(2);var protectedPos=positions.get(3);
            var originals=new LinkedHashMap<BlockPos,BlockState>();
            for(var pos:java.util.List.of(stone,chest,water))originals.put(pos,h.getLevel().getBlockState(pos));
            Consumer<BlockEvent.EntityPlaceEvent> protection=event->{if(event.getEntity()==f.player&&event.getPos().equals(protectedPos))event.setCanceled(true);};
            try {
                BlockBreakingSettings.setDisabledMask(f.player,BlockBreakingSettings.Category.TECHNIQUES.bit());
                h.assertTrue(QiWoodSpikes.erupt(h.getLevel(),f.player,center).isEmpty(),"Disabled technique terrain cannot create logs");
                BlockBreakingSettings.setDisabledMask(f.player,0);f.player.getAbilities().mayBuild=false;
                h.assertTrue(QiWoodSpikes.erupt(h.getLevel(),f.player,center).isEmpty(),"Players without build permission cannot create logs");
                f.player.getAbilities().mayBuild=true;
                h.getLevel().setBlockAndUpdate(stone,Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
                var inventory=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(chest);
                inventory.setItem(0,new ItemStack(Items.DIAMOND,3));
                h.getLevel().setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
                NeoForge.EVENT_BUS.addListener(protection);
                var placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);
                h.assertTrue(!placed.isEmpty(),"Allowed empty cells still form wooden spikes");
                h.assertTrue(!placed.containsKey(stone)&&h.getLevel().getBlockState(stone).is(Blocks.STONE),"Existing stone never overwritten");
                h.assertTrue(!placed.containsKey(chest)&&h.getLevel().getBlockEntity(chest)==inventory&&inventory.getItem(0).getCount()==3,"Chest and its contents never overwritten");
                h.assertTrue(!placed.containsKey(water)&&h.getLevel().getBlockState(water).is(Blocks.WATER),"Existing water never overwritten");
                h.assertTrue(!placed.containsKey(protectedPos)&&h.getLevel().getBlockState(protectedPos).isAir(),"Cancelled place event restores protected air cell");
                h.assertTrue(!QiWoodSpikes.intersects(placed,new AABB(protectedPos).deflate(.1)),"Rejected cells do not deal phantom spike damage");
            } finally {
                NeoForge.EVENT_BUS.unregister(protection);clearWood(h,center,pattern);
                originals.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));
            }
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void evergreenKeepsOwnerAndPetCellsClear(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            var center=f.player.position().add(0,12,0);var pattern=QiWoodSpikes.pattern(center);
            var positions=new java.util.ArrayList<>(pattern.keySet());
            var pet=EntityType.WOLF.create(h.getLevel());pet.setOwnerUUID(f.player.getUUID());pet.setNoAi(true);pet.setNoGravity(true);
            f.player.setPos(Vec3.atBottomCenterOf(positions.get(0)));
            pet.setPos(Vec3.atBottomCenterOf(positions.get(positions.size()-1)));h.getLevel().addFreshEntity(pet);
            try {
                var placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);
                h.assertTrue(!placed.isEmpty(),"Safe cells around owner and pet still erupt");
                h.assertTrue(!QiWoodSpikes.intersects(placed,f.player.getBoundingBox()),"Owner is not trapped inside their own logs");
                h.assertTrue(!QiWoodSpikes.intersects(placed,pet.getBoundingBox()),"Owned pet is not trapped inside wooden logs");
            } finally {pet.discard();clearWood(h,center,pattern);}
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void temporaryWoodPersistsExactStatesAndRespectsLaterEdits(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            // Keep every directly placed cell in one chunk, including fixtures near chunk borders.
            var base=f.player.blockPosition().above(12);
            var beside=base.relative((base.getX()&15)==15?Direction.WEST:Direction.EAST);
            var across=base.relative((base.getZ()&15)==15?Direction.NORTH:Direction.SOUTH);
            var axisX=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
            var axisZ=axisX.setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
            var placements=Map.of(base,axisX,beside,axisX,across,axisX);
            TemporaryTerrainEntity anchor=null,loaded=null,restore=null,reloaded=null;
            try {
                placements.keySet().forEach(pos->h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()));
                anchor=TemporaryTerrainEntity.placeAir(h.getLevel(),f.player,placements,40);
                h.assertTrue(anchor!=null&&anchor.placedStates().equals(placements),"Temporary terrain records exact accepted log states");
                var saved=anchor.saveWithoutId(new CompoundTag());anchor.discard();saved.putLong("ExpireGameTime",h.getLevel().getGameTime()-1);
                loaded=ModEntities.TEMPORARY_TERRAIN.get().create(h.getLevel());loaded.load(saved);
                h.assertTrue(loaded.placedStates().equals(placements),"Log axis states survive save/load");
                h.getLevel().setBlockAndUpdate(beside,axisZ);
                h.getLevel().setBlockAndUpdate(across,Blocks.STONE.defaultBlockState());
                loaded.tick();
                h.assertTrue(loaded.isRemoved()&&h.getLevel().getBlockState(base).isAir(),"Expired saved terrain cleans up immediately after loading");
                h.assertTrue(h.getLevel().getBlockState(beside).equals(axisZ),"Cleanup preserves a later same-log axis edit");
                h.assertTrue(h.getLevel().getBlockState(across).is(Blocks.STONE),"Cleanup preserves a later replacement block");
                var original=Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST).setValue(StairBlock.HALF,Half.TOP);
                h.getLevel().setBlockAndUpdate(base,original);
                restore=TemporaryTerrainEntity.replaceAndRestore(h.getLevel(),Map.of(base,axisX),40);
                h.assertTrue(restore!=null,"Existing temporary-terrain restoration remains supported");
                var restoreSave=restore.saveWithoutId(new CompoundTag());restore.discard();restoreSave.putLong("ExpireGameTime",h.getLevel().getGameTime()-1);
                reloaded=ModEntities.TEMPORARY_TERRAIN.get().create(h.getLevel());reloaded.load(restoreSave);reloaded.tick();
                h.assertTrue(h.getLevel().getBlockState(base).equals(original),"Saved restoration keeps original stair facing and half");
            }finally{
                for(var entity:new TemporaryTerrainEntity[]{anchor,loaded,restore,reloaded})if(entity!=null&&!entity.isRemoved())entity.cleanupNow();
                placements.keySet().forEach(pos->h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()));
            }
        }h.succeed();
    }
    private static long countOakLogs(GameTestHelper h,AABB area) {
        long count=0;
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(area.minX,area.minY,area.minZ),BlockPos.containing(area.maxX,area.maxY,area.maxZ)))
            if(h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG))count++;
        return count;
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void contactMethodsDealDamageAndVisualOnlyArtsDoNot(GameTestHelper h) {
        h.runAfterDelay(2-h.getLevel().getGameTime()%2,()->{
            for(var style:MethodDomainStyle.values()) {
                var f=new LifeSimulationGameTests.Player(h);
                var victim=EntityType.COW.create(h.getLevel());
                Vec3 point=null;
                try {
                    prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);
                    f.player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,42,3))));
                    ModAttachments.setData(f.player,ModAttachments.getData(f.player)
                            .withActiveCultivationMethod(style.method).withMethodProficiencies(Map.of(style.method,500)));
                    QiDomainSystem.toggle(f.player);
                    long now=h.getLevel().getGameTime();
                    int seam=0;
                    for(int i=1;i<MethodDomainEffects.WIND_SEAMS;i++)
                        if(MethodDomainEffects.windVisibility(i,now)>MethodDomainEffects.windVisibility(seam,now))seam=i;
                    point=style.kind==MethodDomainStyle.Kind.WIND
                            ?MethodDomainEffects.windStart(f.player.position(),seam,now)
                                .lerp(MethodDomainEffects.windEnd(f.player.position(),seam,now),.5)
                            :QiDomainSystem.motePosition(f.player,0,now);
                    h.getLevel().getChunkAt(BlockPos.containing(point));
                    victim.setNoAi(true);victim.setNoGravity(true);
                    victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10000);
                    victim.setHealth(10000);victim.setPos(point);h.getLevel().addFreshEntity(victim);
                    QiDomainSystem.view(f.player).motePrevious[0]=point;
                    QiDomainSystem.tickAll();
                    boolean contact=style.kind!=MethodDomainStyle.Kind.AIR&&style.kind!=MethodDomainStyle.Kind.CLEAR
                            &&style.kind!=MethodDomainStyle.Kind.REINCARNATION&&style.kind!=MethodDomainStyle.Kind.DEVOUR
                            &&style.kind!=MethodDomainStyle.Kind.STORM&&style.kind!=MethodDomainStyle.Kind.MOUNTAIN&&style.kind!=MethodDomainStyle.Kind.TIDE
                            &&style.kind!=MethodDomainStyle.Kind.FIRE&&style.kind!=MethodDomainStyle.Kind.ICE&&style.kind!=MethodDomainStyle.Kind.BONE
                            &&style.kind!=MethodDomainStyle.Kind.NETHER&&style.kind!=MethodDomainStyle.Kind.SOLAR
                            &&style.kind!=MethodDomainStyle.Kind.BLOOD&&style.kind!=MethodDomainStyle.Kind.DRAIN&&style.kind!=MethodDomainStyle.Kind.CROWN;
                    if(contact)h.assertTrue(victim.getHealth()<10000,"Actual domain contact damages for "+style.method);
                    if(style.kind==MethodDomainStyle.Kind.AIR||style.kind==MethodDomainStyle.Kind.CLEAR)
                        h.assertTrue(victim.getHealth()==10000,"Visual-only art cannot deal contact damage");
                } finally {
                    victim.discard();
                    if(point!=null)clearWood(h,point,QiWoodSpikes.pattern(point));
                    cleanup(f);
                }
            }
            h.succeed();
        });
    }

    @GameTest(template="fate_test")
    public static void contactDamageUsesCasterStatsAndProtectsFriendlyTargets(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);
            float before=QiDomainDamage.amount(f.player,1);
            var data=ModAttachments.getData(f.player);
            ModAttachments.setData(f.player,data.debugSetStat("magic",data.magicAttack()+100,CultivationLevels.getLevelDef(data.cultivationLevel())));
            h.assertTrue(QiDomainDamage.amount(f.player,1)>before,"Magic attack contributes to actual domain damage");
            h.assertTrue(Math.abs(QiDomainDamage.amount(f.player,.42F)/QiDomainDamage.amount(f.player,1)-.42F)<.00001,"Suppression scales damage");
            h.assertTrue(QiDomainDamage.amount(f.player,0)==0,"Fully suppressed domain cannot deal damage");
            var pet=EntityType.WOLF.create(h.getLevel());pet.setOwnerUUID(f.player.getUUID());
            h.assertTrue(!QiDomainDamage.hurt(f.player,pet,100,false)&&!QiDomainDamage.hurt(f.player,f.player,100,false),"Actual damage path excludes caster and owned pets");
            pet.discard();
        }
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void woodenSpikesHurtLaterSurfaceContactsRepeatAndExpire(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);
        prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);
        BlockPos base=h.absolutePos(new BlockPos(3,30,3));
        // Keep the entire hazard in this test's ticking chunk rather than neighboring completed tests.
        Vec3 center=new Vec3((base.getX()>>4)*16+8.5,base.getY()+.5,(base.getZ()>>4)*16+8.5);
        var placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);
        h.assertTrue(!placed.isEmpty(),"Real wooden spikes are placed before target arrives");
        BlockPos top=placed.keySet().stream().max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);victim.setNoGravity(true);
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10000);
        victim.setHealth(10000);victim.setPos(top.getX()+.5,top.getY()+1,top.getZ()+.5);h.getLevel().addFreshEntity(victim);
        h.assertTrue(!new AABB(top).intersects(victim.getBoundingBox()),"Fixture touches the top surface without entering the solid block");
        h.runAfterDelay(4,()->{try {
            float first=victim.getHealth();h.assertTrue(first<10000,"A target arriving after eruption takes surface contact damage");
            var anchors=h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new AABB(center,center).inflate(10));
            for(var anchor:anchors)for(int i=0;i<4;i++){victim.invulnerableTime=0;anchor.tick();}
            h.assertTrue(victim.getHealth()==first,"Several anchors/ticks cannot multiply one contact hit");
            h.runAfterDelay(22,()->{try {
                float second=victim.getHealth();h.assertTrue(second<first,"Remaining against live spikes damages again after the cooldown");
                h.runAfterDelay(20,()->{try {
                    h.assertTrue(placed.keySet().stream().allMatch(pos->h.getLevel().getBlockState(pos).isAir()),"Hazard blocks expire normally");
                    h.assertTrue(victim.getHealth()==second,"Expired spikes cannot continue dealing damage");h.succeed();
                }finally{victim.discard();clearWood(h,center,placed);cleanup(f);}});
            }catch(Throwable error){victim.discard();clearWood(h,center,placed);cleanup(f);throw error;}});
        }catch(Throwable error){victim.discard();clearWood(h,center,placed);cleanup(f);throw error;}});
    }

    @GameTest(template="fate_test",timeoutTicks=60)
    public static void editedWoodCannotDealPhantomContactDamage(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);
        Vec3 center=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,30,3)));
        var placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);
        BlockPos top=placed.keySet().stream().max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
        // Replace all the original cells before introducing a target.
        placed.keySet().forEach(pos->h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()));
        h.getLevel().setBlockAndUpdate(top,Blocks.STONE.defaultBlockState());
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);victim.setNoGravity(true);victim.setPos(top.getX()+.5,top.getY()+1,top.getZ()+.5);
        h.getLevel().addFreshEntity(victim);float health=victim.getHealth();
        h.runAfterDelay(6,()->{try {
            h.assertTrue(victim.getHealth()==health,"Removed or replaced log cells do not retain invisible damage");h.succeed();
        }finally{victim.discard();clearWood(h,center,placed);h.getLevel().setBlockAndUpdate(top,Blocks.AIR.defaultBlockState());cleanup(f);}});
    }

    @GameTest(template="fate_test",timeoutTicks=60)
    public static void savedWoodRetainsOwnerAndContactDamage(GameTestHelper h) {
        var owner=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        owner.setNoAi(true);owner.setNoGravity(true);owner.setInvulnerable(true);
        owner.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,40,3))));h.getLevel().addFreshEntity(owner);
        Vec3 center=owner.position().add(6,12,0);
        var placed=QiWoodSpikes.erupt(h.getLevel(),owner,center,30);
        var loaded=new java.util.ArrayList<TemporaryTerrainEntity>();
        for(var anchor:h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new AABB(center,center).inflate(10))) {
            if(anchor.placedStates().keySet().stream().noneMatch(placed::containsKey))continue;
            var saved=anchor.saveWithoutId(new CompoundTag());anchor.discard();
            h.assertTrue(saved.hasUUID("ContactOwner")&&saved.getFloat("ContactDamage")==30,"Hazard save retains caster attribution and damage");
            var restored=ModEntities.TEMPORARY_TERRAIN.get().create(h.getLevel());restored.load(saved);loaded.add(restored);
        }
        BlockPos top=placed.keySet().stream().max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
        var victim=EntityType.COW.create(h.getLevel());victim.setNoAi(true);victim.setNoGravity(true);
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
        victim.setPos(top.getX()+.5,top.getY()+1,top.getZ()+.5);h.getLevel().addFreshEntity(victim);
        owner.setTarget(victim);
        h.runAfterDelay(2,()->{try {
            h.assertTrue(h.getLevel().getEntity(owner.getUUID())==owner&&owner.isAlive(),"Saved caster resolves from server entity registry");
            h.assertTrue(QiDomainSystem.canAffect(owner,victim),"Saved caster may harm the contact target");
            h.assertTrue(placed.entrySet().stream().anyMatch(e->h.getLevel().getBlockState(e.getKey()).equals(e.getValue())&&new AABB(e.getKey()).intersects(victim.getBoundingBox().inflate(.12))),"Reloaded fixture remains against a live log surface");
            for(var anchor:loaded){anchor.tick();anchor.tick();}
            h.assertTrue(victim.getHealth()<1000,"Reloaded hazard resolves its saved caster and damages surface contact");h.succeed();
        }finally{victim.discard();owner.discard();loaded.forEach(TemporaryTerrainEntity::cleanupNow);}});
    }
    private static void clearWood(GameTestHelper h,Vec3 center,Map<BlockPos,BlockState> pattern) {
        for(var anchor:h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new AABB(center,center).inflate(10)))
            if(anchor.placedStates().keySet().stream().anyMatch(pattern::containsKey))anchor.cleanupNow();
    }
    @GameTest(template="fate_test") public static void activationNeverPushesUnpunchedEnemies(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);
        var near=EntityType.COW.create(h.getLevel());var far=EntityType.COW.create(h.getLevel());var pet=EntityType.WOLF.create(h.getLevel());
        try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,100,0);
            f.player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,32,3))));
            near.setPos(f.player.position().add(4,0,0));far.setPos(f.player.position().add(20,0,0));pet.setPos(f.player.position().add(3,0,1));
            pet.setOwnerUUID(f.player.getUUID());
            for(var entity:java.util.List.of(near,far,pet)){entity.setNoAi(true);entity.setNoGravity(true);entity.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(entity);}
            near.setOnGround(true);float health=near.getHealth();
            QiDomainSystem.toggle(f.player);
            h.assertTrue(near.getDeltaMovement().equals(Vec3.ZERO),"Activation leaves unpunched enemies stationary");
            h.assertTrue(near.getHealth()==health,"Activation impulse does not invent explosion damage");
            h.assertTrue(far.getDeltaMovement().equals(Vec3.ZERO)&&pet.getDeltaMovement().equals(Vec3.ZERO),"Range limit and owned pets remain protected");
            h.assertTrue(f.player.getDeltaMovement().equals(Vec3.ZERO),"Owner is never pushed");
            near.setDeltaMovement(Vec3.ZERO);QiDomainSystem.tickAll();
            f.player.setShiftKeyDown(true);QiDomainSystem.adjust(f.player,1);QiDomainSystem.stop(f.player);
            h.assertTrue(near.getDeltaMovement().equals(Vec3.ZERO),"Upkeep, scroll, and stopping cannot repeat the activation impulse");
        } finally {near.discard();far.discard();pet.discard();cleanup(f);}h.succeed();
    }
    @GameTest(template="fate_test") public static void activationImpulseScalesWithRealmAndIntensity(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var target=new LifeSimulationGameTests.Player(h);
        boolean pvp=h.getLevel().getServer().isPvpAllowed();
        try {
            h.getLevel().getServer().setPvpAllowed(true);
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,0);
            f.player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,38,3))));
            target.player.setPos(f.player.position().add(4,0,0));
            var realms=java.util.List.of(CultivationLevels.REALM_ZHUJI,CultivationLevels.REALM_JINDAN,CultivationLevels.REALM_YUANYING,CultivationLevels.REALM_HUASHEN);
            double previous=Double.MAX_VALUE;
            for(String realm:realms) {
                ModAttachments.setData(target.player,ModAttachments.getData(target.player).withCultivationLevel(realm+CultivationLevels.STAGE_EARLY));
                target.player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
                target.player.setDeltaMovement(Vec3.ZERO);
                boolean pushed=QiActivationKnockback.push(f.player,target.player,200);
                double speed=target.player.getDeltaMovement().horizontalDistance();
                h.assertTrue(speed<previous,"Higher realm targets receive progressively less displacement: "+realm);previous=speed;
                h.assertTrue(pushed==!realm.equals(CultivationLevels.REALM_HUASHEN),"Two realms above are immune; lesser gaps receive an impulse");
            }
            ModAttachments.setData(target.player,ModAttachments.getData(target.player).withCultivationLevel(CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY));
            target.player.setDeltaMovement(Vec3.ZERO);QiActivationKnockback.push(f.player,target.player,100);double low=target.player.getDeltaMovement().horizontalDistance();
            target.player.setDeltaMovement(Vec3.ZERO);QiActivationKnockback.push(f.player,target.player,200);
            h.assertTrue(target.player.getDeltaMovement().horizontalDistance()>low,"200 percent release pushes harder than 100 percent");
            target.player.setDeltaMovement(Vec3.ZERO);h.getLevel().getServer().setPvpAllowed(false);
            h.assertTrue(!QiActivationKnockback.push(f.player,target.player,200)&&target.player.getDeltaMovement().equals(Vec3.ZERO),"PvP-disabled players remain protected");
        } finally {h.getLevel().getServer().setPvpAllowed(pvp);cleanup(f);cleanup(target);}h.succeed();
    }
    @GameTest(template="fate_test") public static void activationRespectsCoverResistanceAndNpcAllies(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var victim=EntityType.COW.create(h.getLevel());
        var follower=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var wall=new java.util.ArrayList<BlockPos>();
        try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,0);
            f.player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,44,3))));
            victim.setPos(f.player.position().add(4,0,0));victim.setDeltaMovement(Vec3.ZERO);
            BlockPos base=BlockPos.containing(f.player.position()).offset(2,0,0);
            for(int y=0;y<4;y++)for(int z=-1;z<=1;z++){var pos=base.offset(0,y,z);wall.add(pos);h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());}
            h.assertTrue(!QiActivationKnockback.push(f.player,victim,200),"Solid cover blocks activation pressure");
            wall.forEach(pos->h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()));
            victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            h.assertTrue(!QiActivationKnockback.push(f.player,victim,200),"Normal full knockback resistance is respected");
            victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
            h.assertTrue(QiActivationKnockback.push(f.player,victim,200),"Uncovered non-resistant enemy is pushed");
            NpcProfile.ensure(follower,follower.npcArchetype());follower.setPos(f.player.position().add(3,0,0));
            follower.getPersistentData().getCompound("ImmortalCultivationNpc").putUUID("FollowingPlayer",f.player.getUUID());
            h.assertTrue(!QiActivationKnockback.push(f.player,follower,200),"Bonded NPC followers are not pushed");
        } finally {wall.forEach(pos->h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()));victim.discard();follower.discard();cleanup(f);}h.succeed();
    }
    @GameTest(template="fate_test") public static void payloadsRoundTripAndRemainSmall(GameTestHelper h) {
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var state=new QiAuraPayloads.State(h.getLevel().dimension().location(),42,java.util.UUID.randomUUID(),"dari_bati_jue",200,4,3,.72F,43,true,12,27.5F);
            QiAuraPayloads.State.CODEC.encode(b,state);h.assertTrue(b.readableBytes()<160,"State descriptor bounded well below packet limits");
            h.assertTrue(state.equals(QiAuraPayloads.State.CODEC.decode(b)),"State codec round trip");b.clear();
            var burst=new QiAuraPayloads.Burst(h.getLevel().dimension().location(),1,2,3,8);QiAuraPayloads.Burst.CODEC.encode(b,burst);
            h.assertTrue(burst.equals(QiAuraPayloads.Burst.CODEC.decode(b)),"Burst codec round trip");
            b.clear();var activation=new QiAuraPayloads.Activation(h.getLevel().dimension().location(),1,2,3,8,200,42,state.uuid());
            QiAuraPayloads.Activation.CODEC.encode(b,activation);
            h.assertTrue(activation.equals(QiAuraPayloads.Activation.CODEC.decode(b)),"Activation codec round trip");
            b.clear();var motion=new QiAuraPayloads.Motion(h.getLevel().dimension().location(),42,state.uuid(),1,2,3,.3F,.6F,-.8F,8,net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState()),true);
            QiAuraPayloads.Motion.CODEC.encode(b,motion);h.assertTrue(b.readableBytes()<120,"Confirmed collision packet remains bounded");
            h.assertTrue(motion.equals(QiAuraPayloads.Motion.CODEC.decode(b)),"Collision codec preserves direction, target and block texture");
            for(int kind=QiAuraPayloads.DomainEffect.DRAIN;kind<=QiAuraPayloads.DomainEffect.WIND_LIFT;kind++) {
                b.clear();var effect=new QiAuraPayloads.DomainEffect(h.getLevel().dimension().location(),42,state.uuid(),43,java.util.UUID.randomUUID(),14,kind,kind==QiAuraPayloads.DomainEffect.SOLAR?100:8);
                QiAuraPayloads.DomainEffect.CODEC.encode(b,effect);
                h.assertTrue(b.readableBytes()<120&&effect.equals(QiAuraPayloads.DomainEffect.CODEC.decode(b)),"Domain effect preserves endpoints and strength within small packet bound");
            }
        }finally{b.release();}h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=90) public static void evergreenWoodWithersGraduallyAndKeepsLaterEdits(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);var center=f.player.position().add(0,100,0);
        var placed=QiWoodSpikes.erupt(h.getLevel(),f.player,center);h.assertTrue(placed.size()>20,"Enough accepted cells for a gradual decay");
        var edited=placed.keySet().iterator().next();h.getLevel().setBlockAndUpdate(edited,Blocks.STONE.defaultBlockState());
        var anchors=h.getLevel().getEntitiesOfClass(TemporaryTerrainEntity.class,new AABB(center,center).inflate(8));
        h.assertTrue(anchors.stream().allMatch(anchor->anchor.saveWithoutId(new CompoundTag()).getBoolean("WoodWithering")),"Withering mode persists with temporary terrain");
        h.runAfterDelay(34,()->{
            long remaining=placed.keySet().stream().filter(pos->h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG)).count();
            h.assertTrue(remaining>0&&remaining<placed.size()-1,"Spikes visibly wither over several ticks instead of vanishing together");
        });
        h.runAfterDelay(43,()->{try {
            h.assertTrue(placed.keySet().stream().filter(pos->!pos.equals(edited)).allMatch(pos->h.getLevel().getBlockState(pos).isAir()),"All unedited wood fully dissolves");
            h.assertTrue(h.getLevel().getBlockState(edited).is(Blocks.STONE),"Withering preserves another player's replacement block");h.succeed();
        }finally{clearWood(h,center,placed);h.getLevel().setBlockAndUpdate(edited,Blocks.AIR.defaultBlockState());cleanup(f);}});
    }
    @GameTest(template="fate_test") public static void woodBloomPacketsAreBoundedAndPreserveAcceptedCells(GameTestHelper h) {
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var cells=new java.util.ArrayList<BlockPos>();for(int i=0;i<256;i++)cells.add(new BlockPos(i%8,i/8%4,i/32));
            var packet=new QiAuraPayloads.WoodBloom(h.getLevel().dimension().location(),BlockPos.ZERO,cells);
            QiAuraPayloads.WoodBloom.CODEC.encode(b,packet);
            h.assertTrue(b.readableBytes()<2200&&packet.equals(QiAuraPayloads.WoodBloom.CODEC.decode(b)),"Largest wood descriptor is about two KiB and preserves only actual placed cells");
            cells.add(BlockPos.ZERO);boolean rejected=false;
            try{new QiAuraPayloads.WoodBloom(packet.dimension(),BlockPos.ZERO,cells);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Unbounded wood descriptors cannot be constructed");
            b.clear();b.writeResourceLocation(packet.dimension());b.writeBlockPos(BlockPos.ZERO);b.writeVarInt(257);rejected=false;
            try{QiAuraPayloads.WoodBloom.CODEC.decode(b);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Oversized received cell count rejected before allocating list");
        }finally{b.release();}h.succeed();
    }
    @GameTest(template="fate_test") public static void npcDomainUsesRealMethodMasteryAndStopsWithoutTarget(GameTestHelper h) {
        var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var victim=EntityType.COW.create(h.getLevel());
        try {
            com.example.immortal_cultivation_mod.entity.NpcProfile.ensure(npc,npc.npcArchetype());
            npc.getPersistentData().getCompound("ImmortalCultivationNpc").getCompound("MethodProficiencies").putInt(QiDomainSystem.method(npc),500);
            npc.setTarget(victim);npc.tickCount=20;
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.blasting(npc)&&QiDomainSystem.percent(npc)==200,"Golden Core NPC opens domain during real combat");
            h.assertTrue(QiDomainSystem.mastery(npc)==501,"Experienced Golden Core NPC has saved Great mastery plus one combat use");
            var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");
            var proficiencies=profile.getCompound("MethodProficiencies");proficiencies.putInt(QiDomainSystem.method(npc),499);profile.put("MethodProficiencies",proficiencies);
            npc.tickCount=40;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.mastery(npc)==500,"Combat proficiency belongs to active NPC method");
            npc.setTarget(null);npc.tickCount=60;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.view(npc)==null,"NPC stops domain when target lost");
        }finally{QiDomainSystem.stop(npc);npc.discard();victim.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void npcKeepsOpeningExchangeForSpells(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());var victim=EntityType.COW.create(h.getLevel());
        try {
            NpcProfile.ensure(npc,npc.npcArchetype());int savedMastery=QiDomainSystem.mastery(npc);npc.tickCount=21;npc.setTarget(victim);
            int qi=NpcProfile.qi(npc,npc.npcArchetype());
            QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"Acquiring a target leaves the opening exchange for spells");
            h.assertTrue(QiDomainSystem.mastery(npc)==savedMastery,"Waiting does not train method mastery");
            h.assertTrue(NpcProfile.qi(npc,npc.npcArchetype())==qi,"No opening aura Qi drain");
        }finally{QiDomainSystem.stop(npc);npc.discard();victim.discard();}h.succeed();
    }
    @GameTest(template="fate_test",timeoutTicks=440) public static void npcBlastExpiresAndWaitsBeforeReusing(GameTestHelper h) {
        var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var victim=EntityType.COW.create(h.getLevel());
        Runnable cleanup=()->{QiDomainSystem.stop(npc);npc.discard();victim.discard();};
        try {
            npc.setPos(h.absoluteVec(new Vec3(3,45,3)));victim.setPos(npc.position().add(1,0,0));
            NpcProfile.ensure(npc,npc.npcArchetype());npc.setTarget(victim);npc.tickCount=20;
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.blasting(npc),"NPC starts a real combat burst");
            long end=QiDomainSystem.view(npc).npcBlastUntil;
            h.runAfterDelay(80,()->{try {
                npc.tickCount=40;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
                h.assertTrue(QiDomainSystem.blasting(npc)&&QiDomainSystem.view(npc).npcBlastUntil==end,"Keeping combat target cannot extend burst duration");
            }catch(Throwable e){cleanup.run();throw e;}});
            h.runAfterDelay(201,()->{try {
                h.assertTrue(!QiDomainSystem.blasting(npc),"Server upkeep stops release at ten seconds even without another NPC decision");
                npc.tickCount=60;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
                h.assertTrue(!QiDomainSystem.blasting(npc),"Still having a target cannot bypass cooldown");
            }catch(Throwable e){cleanup.run();throw e;}});
            h.runAfterDelay(350,()->{try {
                npc.tickCount=80;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
                h.assertTrue(!QiDomainSystem.blasting(npc),"NPC remains off throughout the ten-second cooldown");
            }catch(Throwable e){cleanup.run();throw e;}});
            h.runAfterDelay(402,()->{try {
                npc.tickCount=100;npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
                h.assertTrue(QiDomainSystem.blasting(npc),"NPC may use a new burst after full cooldown");h.succeed();
            }finally{cleanup.run();}});
        }catch(Throwable e){cleanup.run();throw e;}
    }
    @GameTest(template="fate_test") public static void npcCannotSpamReleaseWithLowQiRetargetingOrReloads(GameTestHelper h) {
        var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var copy=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var victim=EntityType.COW.create(h.getLevel());
        try {
            npc.setPos(h.absoluteVec(new Vec3(3,70,3)));victim.setPos(npc.position().add(1,0,0));
            NpcProfile.ensure(npc,npc.npcArchetype());npc.setTarget(victim);npc.tickCount=20;
            var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");profile.putInt("Qi",1);
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"A tiny regenerating Qi reserve cannot repeatedly trigger activation");
            profile.putInt("Qi",profile.getInt("MaxQi"));
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.blasting(npc),"Sufficient reserve allows activation");
            npc.setTarget(null);npc.tickCount=21;
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"Losing combat target stops activation immediately");
            npc.setTarget(victim);npc.tickCount=40;
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc),"Target reacquisition cannot retrigger blast during cooldown");
            var saved=new net.minecraft.nbt.CompoundTag();npc.saveWithoutId(saved);copy.load(saved);copy.setTarget(victim);copy.tickCount=20;
            copy.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(copy));
            h.assertTrue(!QiDomainSystem.blasting(copy),"Serialized/reloaded NPC retains remaining cooldown");
        }finally{QiDomainSystem.stop(npc);QiDomainSystem.stop(copy);npc.discard();copy.discard();victim.discard();}h.succeed();
    }
    @GameTest(template="fate_test") public static void petsAndOwnedProjectilesProtected(GameTestHelper h) {
        try(var f=new LifeSimulationGameTests.Player(h)) {
            var pet=EntityType.WOLF.create(h.getLevel());pet.setOwnerUUID(f.player.getUUID());
            var arrow=EntityType.ARROW.create(h.getLevel());arrow.setOwner(f.player);
            h.assertTrue(!QiDomainSystem.canAffect(f.player,pet)&&!QiDomainSystem.canAffect(f.player,arrow),"Owner's pets and projectiles protected");
            var stranger=EntityType.ZOMBIE.create(h.getLevel());arrow.setOwner(stranger);
            h.assertTrue(QiDomainSystem.canAffect(f.player,arrow),"Hostile projectile can be intercepted");
            pet.discard();arrow.discard();stranger.discard();
        }h.succeed();
    }
    @GameTest(template="fate_test") public static void combatExitAndRealmRegressionClearRelease(GameTestHelper h) {
        var first=new LifeSimulationGameTests.Player(h);var second=new LifeSimulationGameTests.Player(h);try {
            prepare(first,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);QiDomainSystem.toggle(first.player);
            CombatStateManager.setEnabled(first.player,false);QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(first.player)==null,"Leaving combat clears release");
            prepare(second,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,200,500);QiDomainSystem.toggle(second.player);
            ModAttachments.setData(second.player,ModAttachments.getData(second.player).withCultivationLevel(CultivationLevels.REALM_HALF_JINDAN));QiDomainSystem.tickAll();
            h.assertTrue(QiDomainSystem.view(second.player)==null,"Losing Golden Core clears domain");
        }finally{cleanup(first);cleanup(second);}h.succeed();
    }

    @GameTest(template="fate_test") public static void blastMobilityStartsImmediatelyAndStopsWithoutParticles(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,100,0);
            double speed=f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            double jump=f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
            double fall=f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE);
            f.player.setDeltaMovement(Vec3.ZERO);f.player.jumpFromGround();double before=f.player.getDeltaMovement().y;
            QiDomainSystem.toggle(f.player);
            h.assertTrue(Math.abs(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed*1.2)<1E-6,
                    "100-percent blast at zero mastery immediately grants Speed I strength");
            f.player.setDeltaMovement(Vec3.ZERO);f.player.jumpFromGround();
            h.assertTrue(f.player.getDeltaMovement().y>before+.09,"Real jump gains Jump Boost I strength");
            h.assertTrue(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE)==fall+1,
                    "Higher jumps receive matching safe fall allowance");
            h.assertTrue(!f.player.hasEffect(MobEffects.MOVEMENT_SPEED)&&!f.player.hasEffect(MobEffects.JUMP),
                    "Mobility adds no potion instances, particles or icons");
            QiDomainSystem.tickAll();QiDomainSystem.tickAll();
            h.assertTrue(Math.abs(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed*1.2)<1E-6,
                    "Repeated aura ticks never stack bonuses");
            QiDomainSystem.stop(f.player);
            h.assertTrue(Math.abs(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed)<1E-6
                            &&Math.abs(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)-jump)<1E-6
                            &&f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE)==fall,
                    "Stopping immediately restores original movement attributes");
        }finally{cleanup(f);}h.succeed();
    }

    @GameTest(template="fate_test") public static void blastMobilityPreservesPotionEffectsWhenResourcesRunOut(GameTestHelper h) {
        var f=new LifeSimulationGameTests.Player(h);try {
            prepare(f,CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY,100,0);
            var speed=new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,1,true,false,false);
            var jump=new net.minecraft.world.effect.MobEffectInstance(MobEffects.JUMP,800,2,true,false,false);
            f.player.addEffect(speed);f.player.addEffect(jump);
            double baseline=f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            QiDomainSystem.toggle(f.player);
            ModAttachments.setData(f.player,ModAttachments.getData(f.player).withQi(0).withBlood(0));
            QiDomainSystem.tickAll();
            h.assertTrue(!QiDomainSystem.blasting(f.player),"Resource exhaustion ends release");
            h.assertTrue(f.player.getEffect(MobEffects.MOVEMENT_SPEED)==speed&&speed.getDuration()==600
                            &&f.player.getEffect(MobEffects.JUMP)==jump&&jump.getDuration()==800,
                    "Stopping does not replace, shorten or remove existing potions");
            h.assertTrue(Math.abs(f.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-baseline)<1E-6,
                    "Potion movement remains after only the blast bonus is removed");
        }finally{cleanup(f);}h.succeed();
    }

    @GameTest(template="fate_test") public static void npcBlastMobilityStopsWhenItsTargetIsLost(GameTestHelper h) {
        var npc=ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        var target=EntityType.COW.create(h.getLevel());
        try {
            npc.setPos(h.absoluteVec(new Vec3(3,70,3)));target.setPos(npc.position().add(1,0,0));
            NpcProfile.ensure(npc,npc.npcArchetype());npc.setTarget(target);npc.tickCount=20;
            double speed=npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            double jump=npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
            npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(QiDomainSystem.blasting(npc)
                            &&Math.abs(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed*1.2)<1E-6
                            &&Math.abs(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)-jump-.1)<1E-6,
                    "NPC activation grants the same particle-free mobility bonuses");
            npc.setTarget(null);npc.getPersistentData().putLong(QiDomainSystem.NPC_COMBAT_READY_AT,0);QiDomainSystem.npcTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(npc));
            h.assertTrue(!QiDomainSystem.blasting(npc)
                            &&Math.abs(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed)<1E-6,
                    "NPC target loss removes its blast movement immediately");
        }finally{QiDomainSystem.stop(npc);npc.discard();target.discard();}h.succeed();
    }
}
