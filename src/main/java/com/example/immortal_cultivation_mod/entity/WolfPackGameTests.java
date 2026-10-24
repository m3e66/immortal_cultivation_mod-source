package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.externalpower.ExternalPowerTargets;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_wolf_packs")
@PrefixGameTestTemplate(false)
public final class WolfPackGameTests {
    @GameTest(template="fate_test") public static void attackersRotateWhileWoundedWolvesYield(GameTestHelper h) {
        List<Wolf> pack=new ArrayList<>();
        for(int i=0;i<5;i++)pack.add(EntityType.WOLF.create(h.getLevel()));
        h.assertTrue(pack.stream().filter(w->WolfPackBehavior.attackTurn(pack,w,0)).count()==2,
                "Five wolves use two attackers, leaving three to hold escape lanes");
        h.assertTrue(WolfPackBehavior.attackTurn(pack,pack.get(0),59)
                && !WolfPackBehavior.attackTurn(pack,pack.get(0),60),"Attack turn stays stable then rotates");
        pack.get(1).setHealth(1);
        h.assertTrue(!WolfPackBehavior.attackTurn(pack,pack.get(1),60),"Wounded packmate yields its attack turn");
        h.assertTrue(pack.stream().filter(w->WolfPackBehavior.attackTurn(pack,w,60)).count()==1,
                "Smaller healthy group concentrates on one attacker");
        h.assertTrue(WolfPackBehavior.attackTurn(List.of(pack.get(1)),pack.get(1),60),"Lone wounded wolf can still defend itself");
        var lead=WolfPackBehavior.escapeLead(new Vec3(.2,0,0));
        h.assertTrue(lead.x>1 && lead.z==0,"Interception leads real horizontal motion");
        h.assertTrue(WolfPackBehavior.escapeLead(new Vec3(100,30,0)).length()<=2.5001,"Prediction stays bounded after abrupt movement");
        h.succeed();
    }

    @GameTest(template="fate_test") public static void packPouncesAreStaggered(GameTestHelper h) {
        var f=new Field(h,70);
        try {
            var a=f.wolf(0,1);var b=f.wolf(0,-1);var target=f.target(3,0);
            a.setTarget(target);b.setTarget(target);a.setOnGround(true);b.setOnGround(true);
            var first=new WolfPackGoal(a);var second=new WolfPackGoal(b);first.start();second.start();
            long now=h.getLevel().getGameTime()+100;
            h.assertTrue(first.tryPounce(now),"First clear pounce launches");
            h.assertTrue(!second.tryPounce(now),"Packmate cannot pile into the same pounce window");
            h.assertTrue(second.tryPounce(now+16),"Next packmate can pounce after separation window");
        } finally {f.close();}
        h.succeed();
    }

    @GameTest(template="fate_test") public static void woundedWolfPathsAwayInsteadOfJoiningThePile(GameTestHelper h) {
        var f=new Field(h,90);
        try {
            var wounded=f.wolf(0,0);var healthy=f.wolf(0,3);var target=f.target(3,0);
            wounded.setOnGround(true);healthy.setOnGround(true);
            wounded.setHealth(1);wounded.setTarget(target);healthy.setTarget(target);
            var goal=new WolfPackGoal(wounded);goal.start();goal.tick();
            var destination=wounded.getNavigation().getTargetPos();
            h.assertTrue(destination!=null && Vec3.atBottomCenterOf(destination).distanceTo(target.position())>4,
                    "Wounded wolf chooses a supported retreat lane");
        } finally {f.close();}
        h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=120) public static void wolvesStopTrackingUnseenPrey(GameTestHelper h) {
        var f=new Field(h,170);
        var wolf=EntityType.WOLF.create(h.getLevel());f.entities.add(wolf);
        wolf.setPos(Vec3.atBottomCenterOf(f.origin));var target=f.target(3,0);wolf.setTarget(target);
        var goal=new WolfPackGoal(wolf);goal.start();
        for(int z=-3;z<=3;z++)for(int y=0;y<4;y++)h.getLevel().setBlockAndUpdate(f.origin.offset(1,y,z),Blocks.STONE.defaultBlockState());
        goal.tick();
        h.runAfterDelay(81,()->{try {
            goal.tick();h.assertTrue(wolf.getTarget()==null,"Four seconds without sight ends omniscient pursuit");h.succeed();
        }finally{f.close();}});
    }
    private static class Field {
        final GameTestHelper h;
        final BlockPos origin;
        final List<net.minecraft.world.entity.Entity> entities = new ArrayList<>();
        Field(GameTestHelper h, int height) {
            this.h = h; origin = h.absolutePos(new BlockPos(2, height, 2));
            for (int x=-12;x<=12;x++) for(int z=-12;z<=12;z++) {
                h.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<4;y++) h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
        }
        Wolf wolf(double x, double z) {
            Wolf wolf = EntityType.WOLF.create(h.getLevel());
            wolf.setPos(origin.getX()+x+.5,origin.getY(),origin.getZ()+z+.5);
            h.getLevel().addFreshEntity(wolf); entities.add(wolf); return wolf;
        }
        Villager target(double x, double z) {
            var target = EntityType.VILLAGER.create(h.getLevel()); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
            target.setPos(origin.getX()+x+.5,origin.getY(),origin.getZ()+z+.5);
            h.getLevel().addFreshEntity(target);entities.add(target);return target;
        }
        void close(){entities.forEach(net.minecraft.world.entity.Entity::discard);}
    }

    @GameTest(template="fate_test") public static void allWolfHabitatsUseFiveWithoutAddingNewHabitats(GameTestHelper h) {
        int habitats=0;
        for(var biome:h.getLevel().registryAccess().registryOrThrow(Registries.BIOME))
            for(var category:net.minecraft.world.entity.MobCategory.values())
                for(var spawn:biome.getMobSettings().getMobs(category).unwrap()) if(spawn.type==EntityType.WOLF){
                    habitats++;h.assertTrue(spawn.minCount==5&&spawn.maxCount==5,"All wolf habitats have five-member groups");
                }
        h.assertTrue(habitats>0,"Natural wolf habitats remain registered");
        var wolf=EntityType.WOLF.create(h.getLevel());var cluster=new SpawnClusterSizeEvent(wolf);
        NeoForge.EVENT_BUS.post(cluster);h.assertTrue(cluster.getSize()==5,"Natural spawn cap permits the fifth wolf");
        h.succeed();
    }

    @GameTest(template="fate_test") public static void everyCoatGetsGoalsAndRemainsMortalAfterReload(GameTestHelper h) {
        var f=new Field(h,110);
        try {
            var variants=h.getLevel().registryAccess().registryOrThrow(Registries.WOLF_VARIANT);
            for(var holder:variants.holders().toList()) {
                var wolf=f.wolf(0,0);wolf.setVariant(holder);
                h.assertTrue(wolf.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof WolfPackGoal).count()==1,
                        "Join hook installs shared combat on every coat");
                NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(wolf,h.getLevel()));
                h.assertTrue(wolf.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof WolfPackGoal).count()==1,
                        "Reload/join cannot duplicate goals");
                h.assertTrue(WolfPackBehavior.realm(wolf)==0&&ExternalPowerTargets.realm(wolf)==0&&wolf.getMaxHealth()==8,
                        "Vanilla wolves retain mortal realm and ordinary attributes");
                var id=UUID.randomUUID();wolf.getPersistentData().putUUID(WolfPackBehavior.PACK_ID,id);
                var tag=new CompoundTag();wolf.saveWithoutId(tag);
                var restored=EntityType.WOLF.create(h.getLevel());restored.load(tag);
                h.assertTrue(restored.getVariant().is(holder.unwrapKey().orElseThrow())
                        &&id.equals(restored.getPersistentData().getUUID(WolfPackBehavior.PACK_ID)),"Coat and pack survive NBT save/load");
                wolf.discard();
            }
        } finally {f.close();} h.succeed();
    }

    @GameTest(template="fate_test") public static void naturalGroupsShareIdentityButEggsDoNotMultiply(GameTestHelper h) {
        net.minecraft.world.entity.SpawnGroupData group=null;UUID id=null;
        for(int i=0;i<5;i++){
            var wolf=EntityType.WOLF.create(h.getLevel());wolf.setPos(h.absoluteVec(Vec3.ZERO));
            var event=new FinalizeSpawnEvent(wolf,h.getLevel(),wolf.getX(),wolf.getY(),wolf.getZ(),
                    h.getLevel().getCurrentDifficultyAt(wolf.blockPosition()),MobSpawnType.NATURAL,group,null);
            NeoForge.EVENT_BUS.post(event);group=event.getSpawnData();
            h.assertTrue(group instanceof WolfPackBehavior.PackSpawnData,"Vanilla-compatible natural pack data");
            var current=wolf.getPersistentData().getUUID(WolfPackBehavior.PACK_ID);
            if(id==null)id=current;else h.assertTrue(id.equals(current),"Each member inherits one pack identity");
        }
        var egg=EntityType.WOLF.create(h.getLevel());
        var event=new FinalizeSpawnEvent(egg,h.getLevel(),0,100,0,h.getLevel().getCurrentDifficultyAt(BlockPos.ZERO),MobSpawnType.SPAWN_EGG,null,null);
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(!egg.getPersistentData().hasUUID(WolfPackBehavior.PACK_ID)&&event.getSpawnData()==null,
                "Eggs/commands retain single-entity semantics");h.succeed();
    }

    @GameTest(template="fate_test") public static void rallyIsBoundedAndDoesNotStealPetsOrExistingFights(GameTestHelper h) {
        var f=new Field(h,150);
        try{
            var caller=f.wolf(0,0);var target=f.target(4,0);caller.setTarget(target);
            var busy=f.wolf(1,0);var other=f.target(5,1);busy.setTarget(other);
            var pet=f.wolf(2,0);pet.setTame(true,true);pet.setOwnerUUID(UUID.randomUUID());
            var pup=f.wolf(3,1);pup.setAge(-20000);
            for(int i=0;i<16;i++)f.wolf(i%4-2,i/4+2);
            int before=f.entities.size();int recruited=WolfPackBehavior.rally(caller,target);
            h.assertTrue(recruited==11&&WolfPackBehavior.combatPack(caller,target).size()==12,"Howl recruits a capped local pack");
            h.assertTrue(busy.getTarget()==other&&pet.getTarget()==null&&pup.getTarget()==null,"Existing fights, pets and pups unchanged");
            h.assertTrue(WolfPackBehavior.rally(caller,target)==0&&f.entities.size()==before,"Repeated calls never create more wolves");
        }finally{f.close();}h.succeed();
    }

    @GameTest(template="fate_test") public static void formationsChangeWithPackSizeAndEscapeDirection(GameTestHelper h) {
        var zero=Vec3.ZERO;var east=new Vec3(1,0,0);
        h.assertTrue(WolfPackBehavior.formationSlot(zero,east,1,0).equals(zero),"Lone wolf presses target directly");
        var left=WolfPackBehavior.formationSlot(zero,east,3,1);var right=WolfPackBehavior.formationSlot(zero,east,3,2);
        h.assertTrue(left.z*right.z<0,"Three wolves split across opposite flanks");
        var cutoff=WolfPackBehavior.formationSlot(zero,east,5,3);
        var reversed=WolfPackBehavior.formationSlot(zero,east.scale(-1),5,3);
        h.assertTrue(cutoff.x>4&&reversed.x<-4,"Interceptor turns ahead of the changed escape route");
        var rear=WolfPackBehavior.formationSlot(zero,east,5,4);
        h.assertTrue(rear.x<0,"Fifth wolf closes the rear route");h.succeed();
    }

    @GameTest(template="fate_test") public static void safetyGatesExcludePetsCreativeSpectatorsAndDazedWolves(GameTestHelper h) {
        var f=new Field(h,190);
        try{
            var wolf=f.wolf(0,0);var target=f.target(3,0);wolf.setTarget(target);
            var goal=new WolfPackGoal(wolf);h.assertTrue(goal.canUse(),"Wild wolf can fight");
            wolf.setTame(true,true);h.assertTrue(!goal.canUse(),"Tamed wolf uses its owner AI");wolf.setTame(false,true);
            wolf.addEffect(new MobEffectInstance(ModEffects.DAZE,100));h.assertTrue(!goal.canUse(),"Daze blocks pack attacks");wolf.removeEffect(ModEffects.DAZE);
            var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"wolf-test"));
            player.setGameMode(GameType.CREATIVE);h.assertTrue(!WolfPackBehavior.validTarget(wolf,player),"Creative immunity");
            player.setGameMode(GameType.SPECTATOR);h.assertTrue(!WolfPackBehavior.validTarget(wolf,player),"Spectator immunity");
            target.discard();h.assertTrue(!goal.canUse(),"Dead targets end pursuit");
        }finally{f.close();}h.succeed();
    }

    @GameTest(template="fate_test",timeoutTicks=80) public static void pouncePhysicallyTravelsAndDamagesOnlyOnContact(GameTestHelper h) {
        var f=new Field(h,230);var wolf=ModEntities.SILVER_MIST_WOLF.get().create(h.getLevel());
        wolf.setPos(Vec3.atBottomCenterOf(f.origin));h.getLevel().addFreshEntity(wolf);f.entities.add(wolf);
        var target=f.target(3,0);wolf.setTarget(target);wolf.setOnGround(true);
        var goal=(WolfPackGoal)wolf.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof WolfPackGoal).findFirst().orElseThrow();
        goal.start();float hp=target.getHealth();Vec3 start=wolf.position();
        h.assertTrue(goal.tryPounce(h.getLevel().getGameTime()+100),"Clear supported route starts real pounce");
        h.assertTrue(target.getHealth()==hp&&wolf.getDeltaMovement().y>.3,"Launch gives motion, not remote damage");
        h.runAfterDelay(20,()->{try{
            h.assertTrue(wolf.distanceToSqr(start)>1&&target.getHealth()<hp,"Physical contact inflicts the wolf's actual attack");
            h.assertTrue(target.getPersistentData().getLong("CultivationWolfPinUntil")>0,"Contact applies bounded pressure cooldown");
        }finally{f.close();}h.succeed();});
    }

    @GameTest(template="fate_test") public static void silverMistWolfHasNaturalSpawnsMortalStatsAndOwnOffspring(GameTestHelper h) {
        var wolf=ModEntities.SILVER_MIST_WOLF.get().create(h.getLevel());
        h.assertTrue(wolf!=null&&wolf.getMaxHealth()==20&&wolf.getAttributeValue(Attributes.ATTACK_DAMAGE)==4
                &&ExternalPowerTargets.realm(wolf)==0,"New model mob is mortal with ordinary combat stats");
        int habitats=0;
        for(var biome:h.getLevel().registryAccess().registryOrThrow(Registries.BIOME)){
            var spawns=biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap();
            boolean vanilla=spawns.stream().anyMatch(s->s.type==EntityType.WOLF);
            var silver=spawns.stream().filter(s->s.type==ModEntities.SILVER_MIST_WOLF.get()).toList();
            h.assertTrue(vanilla==!silver.isEmpty(),"New species uses the existing wolf habitats");
            for(var spawn:silver){habitats++;h.assertTrue(spawn.minCount==5&&spawn.maxCount==5,"New species spawns in fives");}
        }
        h.assertTrue(habitats>0,"Custom wolf can spawn naturally");
        var egg=(net.minecraft.world.item.SpawnEggItem)com.example.immortal_cultivation_mod.item.ModItems.SILVER_MIST_WOLF_SPAWN_EGG.get();
        h.assertTrue(egg.getType(egg.getDefaultInstance())==wolf.getType(),"Spawn egg creates the custom wolf");
        wolf.setOwnerUUID(UUID.randomUUID());wolf.setTame(true,true);
        var pup=wolf.getBreedOffspring(h.getLevel(),wolf);
        h.assertTrue(pup!=null&&pup.getType()==wolf.getType()&&pup.isTame()&&wolf.getOwnerUUID().equals(pup.getOwnerUUID()),
                "Breeding preserves species and ownership");h.succeed();
    }

    private static final class RankedWolf extends Wolf implements RealmBeast {
        private int realm;
        RankedWolf(net.minecraft.world.level.Level level,int realm){super(EntityType.WOLF,level);this.realm=realm;}
        public int beastRealm(){return realm;}public void setBeastRealm(int realm){this.realm=realm;}
    }
    @GameTest(template="fate_test") public static void higherRealmLeadsAndFollowersUseSavedPackIdentity(GameTestHelper h) {
        var f=new Field(h,310);
        try{
            var follower=f.wolf(-5,0);var leader=new RankedWolf(h.getLevel(),1);
            leader.setPos(Vec3.atBottomCenterOf(f.origin.offset(5,0,0)));h.getLevel().addFreshEntity(leader);f.entities.add(leader);
            var id=UUID.randomUUID();follower.getPersistentData().putUUID(WolfPackBehavior.PACK_ID,id);leader.getPersistentData().putUUID(WolfPackBehavior.PACK_ID,id);
            h.assertTrue(WolfPackBehavior.leader(List.of(follower,leader))==leader,"Higher realm leads despite equal health");
            var follow=new WolfPackFollowGoal(follower);h.assertTrue(follow.canUse(),"Same-pack subordinate follows stronger leader");
            leader.discard();h.assertTrue(!follow.canContinueToUse(),"Dead leader releases followers");
        }finally{f.close();}h.succeed();
    }

    @GameTest(template="fate_test") public static void wallsAndRavinesBlockPounces(GameTestHelper h) {
        var f=new Field(h,270);
        try{
            var wolf=f.wolf(0,0);var target=f.target(3,0);wolf.setTarget(target);wolf.setOnGround(true);
            var goal=new WolfPackGoal(wolf);goal.start();
            var wall=f.origin.offset(1,0,0);h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(wall.above(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!goal.tryPounce(h.getLevel().getGameTime()+100),"Solid wall prevents pounce");
            h.getLevel().setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(wall.above(),Blocks.AIR.defaultBlockState());
            for(int y=-1;y>=-4;y--)h.getLevel().setBlockAndUpdate(wall.offset(0,y,0),Blocks.AIR.defaultBlockState());
            h.assertTrue(!goal.tryPounce(h.getLevel().getGameTime()+100),"Deep unsupported gap prevents pounce");
            h.assertTrue(h.getLevel().getBlockState(f.origin.below()).is(Blocks.STONE),"AI never removes ground blocks");
        }finally{f.close();}h.succeed();
    }
}
