package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.*;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.*;

/** Actual damage events, queued post-attack motion, body sweeps and terrain permissions. */
@GameTestHolder("immortal_cultivation_qi_blast_attacks")
@PrefixGameTestTemplate(false)
public final class QiBlastAttackGameTests {
    private static final String CORE=CultivationLevels.REALM_JINDAN+CultivationLevels.STAGE_EARLY;
    private static final class Arena implements AutoCloseable {
        final GameTestHelper h;
        final LifeSimulationGameTests.Player owner,target;
        final Map<BlockPos,BlockState> originals=new LinkedHashMap<>();
        final List<net.minecraft.world.level.ChunkPos> forced=new ArrayList<>();
        final boolean pvp;
        Arena(GameTestHelper h,int altitude) {
            this.h=h;owner=new LifeSimulationGameTests.Player(h);target=new LifeSimulationGameTests.Player(h);
            pvp=h.getLevel().getServer().isPvpAllowed();h.getLevel().getServer().setPvpAllowed(true);
            BlockPos base=h.absolutePos(new BlockPos(3,altitude,3));
            owner.player.setPos((base.getX()>>4)*16+8.5,base.getY(),(base.getZ()>>4)*16+1.5);
            target.player.setPos(owner.player.position().add(0,0,2));
            for(ServerPlayer p:List.of(owner.player,target.player)) {
                p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(false);
                ModAttachments.setData(p,ModAttachments.getData(p).withCultivationLevel(CORE).withQi(100000)
                        .withActiveCultivationMethod(CultivationMethods.CLEAR_HEART).withMethodProficiencies(Map.of()));
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);p.setHealth(100000);
                p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
                try {var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}
                catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            }
            owner.player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
            CombatStateManager.setEnabled(owner.player,true);CombatStateManager.toggleManualCombat(owner.player);
            owner.player.getPersistentData().putInt("ImmortalCultivationQiReleasePercent",100);
            QiDomainSystem.toggle(owner.player);
            h.assertTrue(QiDomainSystem.blasting(owner.player),"Fixture uses actual server release toggle");
        }
        BlockPos put(BlockPos pos,BlockState state) {
            pos=pos.immutable();originals.putIfAbsent(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlockAndUpdate(pos,state);return pos;
        }
        BlockPos wall(int distance,BlockState state) {
            BlockPos center=owner.player.blockPosition().offset(0,0,distance);
            for(int x=-1;x<=1;x++)for(int y=0;y<6;y++)put(center.offset(x,y,0),state);
            return center;
        }
        void hit(int type,LivingEntity victim) {
            // Other parallel arenas restore this shared flag between our scheduled hit stages.
            if(victim instanceof ServerPlayer)h.getLevel().getServer().setPvpAllowed(true);
            victim.invulnerableTime=0;victim.setDeltaMovement(Vec3.ZERO);victim.setOnGround(false);
            float before=victim.getHealth();
            if(type<2) {
                owner.player.setItemSlot(EquipmentSlot.MAINHAND,type==0?ItemStack.EMPTY:new ItemStack(Items.DIAMOND_SWORD));
                owner.player.attack(victim);
            }else if(type==2)victim.hurt(victim.damageSources().indirectMagic(owner.player,owner.player),4);
            else {
                var projectile=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),owner.player);
                projectile.setDeltaMovement(0,0,1);projectile.setPos(owner.player.position());
                try{victim.hurt(victim.damageSources().thrown(projectile,owner.player),4);}finally{projectile.discard();}
            }
            h.assertTrue(victim.getHealth()<before,"Confirmed damage reaches the real post-damage handler: "+type);
        }
        void tick(LivingEntity victim){CombatKnockback.tick(new EntityTickEvent.Pre(victim));}
        void forceFlightChunks() {
            int cx=owner.player.blockPosition().getX()>>4,cz=owner.player.blockPosition().getZ()>>4;
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++) {
                var chunk=new net.minecraft.world.level.ChunkPos(x,z);
                if(!h.getLevel().getForcedChunks().contains(chunk.toLong())){forced.add(chunk);h.getLevel().setChunkForced(x,z,true);}
            }
        }
        @Override public void close() {
            QiDomainSystem.stop(owner.player);QiDomainSystem.stop(target.player);
            owner.close();target.close();originals.forEach((pos,state)->h.getLevel().setBlockAndUpdate(pos,state));
            h.getLevel().getServer().setPvpAllowed(pvp);
            forced.forEach(chunk->h.getLevel().setChunkForced(chunk.x,chunk.z,false));
        }
    }

    @GameTest(template="fate_test",timeoutTicks=80)
    public static void onlyDirectFistsLaunchPlayersThroughWalls(GameTestHelper h) {
        var a=new Arena(h,30);
        for(int i=0;i<4;i++) {
            int type=3-i;h.runAfterDelay(3+i*3,()->{try {
                BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
                a.hit(type,a.target.player);a.tick(a.target.player);
                if(type==0){
                    h.assertTrue(a.target.player.getDeltaMovement().z>1&&a.target.player.getDeltaMovement().y>.3,"Punch launches victim");
                    h.assertTrue(h.getLevel().getBlockState(wall).isAir(),"Punch breaks collision wall");
                }else h.assertTrue(a.target.player.getDeltaMovement().horizontalDistance()<1&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Non-punch cannot blast-launch or excavate: "+type);
                if(type==0){a.close();h.succeed();}
            }catch(Throwable error){a.close();throw error;}});
        }
    }
    @GameTest(template="fate_test",timeoutTicks=80)
    public static void launchedMobActuallyTravelsThroughSuccessiveWalls(GameTestHelper h) {
        var a=new Arena(h,55);var mob=EntityType.COW.create(h.getLevel());
        try {
            a.forceFlightChunks();
            a.wall(3,Blocks.STONE.defaultBlockState());BlockPos second=a.wall(9,Blocks.STONE.defaultBlockState());
            mob.setNoAi(true);mob.setNoGravity(true);mob.setPersistenceRequired();mob.setPos(a.owner.player.position().add(0,0,2));
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);mob.setHealth(100000);h.getLevel().addFreshEntity(mob);
            h.runAfterDelay(4,()->{mob.setNoGravity(false);mob.setNoAi(false);mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);a.hit(0,mob);});
            h.runAfterDelay(9,()->{try {
                h.assertTrue(mob.getZ()>a.owner.player.getZ()+7&&mob.getY()>a.owner.player.getY()+.3,"Real entity ticks carry the launched mob forward and up: relative="+mob.position().subtract(a.owner.player.position())+", velocity="+mob.getDeltaMovement()+", ticks="+mob.tickCount);
                h.assertTrue(h.getLevel().getBlockState(second.above(2)).isAir(),"Flight breaks a second wall along its later trajectory");h.succeed();
            }finally{mob.discard();a.close();}});
        }catch(Throwable error){mob.discard();a.close();throw error;}
    }
    @GameTest(template="fate_test")
    public static void normalWalkingRejectedHitsAndNonBlastMeleeCannotExcavate(GameTestHelper h) {
        try(var a=new Arena(h,80)) {
            BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
            a.target.player.setInvulnerable(true);a.target.player.hurt(a.target.player.damageSources().playerAttack(a.owner.player),4);
            a.target.player.setInvulnerable(false);a.target.player.setDeltaMovement(Vec3.ZERO);a.tick(a.target.player);
            h.assertTrue(a.target.player.getDeltaMovement().equals(Vec3.ZERO)&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Rejected damage creates no launch or terrain work");
            QiDomainSystem.stop(a.owner.player);a.hit(0,a.target.player);a.tick(a.target.player);
            h.assertTrue(a.target.player.getDeltaMovement().horizontalDistance()<1&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Ordinary melee outside release retains original limited behavior");
            for(int i=0;i<40;i++)a.tick(a.target.player);
            a.target.player.setDeltaMovement(0,0,2);a.tick(a.target.player);
            h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Walking after an expired hit cannot break terrain");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void descendingBlastCannotDigThroughSupportingFloor(GameTestHelper h) {
        try(var a=new Arena(h,45)) {
            var mob=com.example.immortal_cultivation_mod.entity.ModEntities.INK_ARMORED_STONE_DEVOURING_TORTOISE.get().create(h.getLevel());
            try {
                mob.setPos(a.target.player.position());
                mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);mob.setHealth(100000);
                a.hit(0,mob);a.tick(mob);
                BlockPos floor=mob.blockPosition().below();
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)a.put(floor.offset(x,0,z),Blocks.STONE.defaultBlockState());
                mob.setOnGround(false);mob.setDeltaMovement(0,-.7,.4);a.tick(mob);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)
                    h.assertTrue(h.getLevel().getBlockState(floor.offset(x,0,z)).is(Blocks.STONE),
                            "Gravity during a confirmed blast must not turn the victim into a downward drill");
            }finally{mob.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void landedBlastCannotKeepExcavating(GameTestHelper h) {
        try(var a=new Arena(h,70)) {
            a.hit(0,a.target.player);a.tick(a.target.player);
            BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
            a.target.player.setOnGround(true);a.target.player.setDeltaMovement(0,-.08,1);a.tick(a.target.player);
            h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),
                    "Landing ends the attack's terrain impulse instead of reusing walking or residual motion");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void impactSettingsDisableTerrainWithoutDisablingLaunch(GameTestHelper h) {
        try(var a=new Arena(h,105)) {
            BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
            BlockBreakingSettings.setDisabledMask(a.owner.player,BlockBreakingSettings.Category.COMBAT_IMPACTS.bit());
            a.hit(0,a.target.player);a.tick(a.target.player);
            h.assertTrue(a.target.player.getDeltaMovement().z>1&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Impact setting preserves launch but prevents block damage");
            BlockBreakingSettings.setDisabledMask(a.owner.player,0);a.hit(0,a.target.player);
            BlockBreakingSettings.setDisabledMask(a.owner.player,BlockBreakingSettings.Category.ALL.bit());a.tick(a.target.player);
            h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Disabling terrain after damage still cancels pending destruction");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void bedrockInventoriesAndCancelledBreaksStopDestruction(GameTestHelper h) {
        try(var a=new Arena(h,130)) {
            for(Block block:List.of(Blocks.BEDROCK,Blocks.CHEST)) {
                BlockPos wall=a.wall(3,block.defaultBlockState());BlockPos behind=a.put(wall.south(),Blocks.STONE.defaultBlockState());
                a.hit(0,a.target.player);a.tick(a.target.player);
                h.assertTrue(h.getLevel().getBlockState(wall).is(block)&&h.getLevel().getBlockState(behind).is(Blocks.STONE),"Protected first contact prevents excavation beyond it: "+block);
            }
            BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
            java.util.function.Consumer<BlockEvent.BreakEvent> deny=e->{if(a.originals.containsKey(e.getPos()))e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(deny);
            try{a.hit(0,a.target.player);a.tick(a.target.player);h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Cancelled block-break events remain authoritative");}
            finally{NeoForge.EVENT_BUS.unregister(deny);}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void realmPressureAndKnockbackImmunityLimitAttackLaunches(GameTestHelper h) {
        try(var a=new Arena(h,155)) {
            double last=Double.MAX_VALUE;
            for(String realm:List.of(CultivationLevels.REALM_ZHUJI,CultivationLevels.REALM_JINDAN,CultivationLevels.REALM_YUANYING,CultivationLevels.REALM_HUASHEN)) {
                ModAttachments.setData(a.target.player,ModAttachments.getData(a.target.player).withCultivationLevel(realm+CultivationLevels.STAGE_EARLY));
                double strength=CombatKnockback.blastStrength(a.owner.player,a.target.player,4);
                h.assertTrue(strength<last,"Higher realms progressively resist the same attack launch");last=strength;
            }
            h.assertTrue(last==0,"Two realms above cannot be blast-launched");
            ModAttachments.setData(a.target.player,ModAttachments.getData(a.target.player).withCultivationLevel(CORE));
            a.target.player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());a.hit(0,a.target.player);a.tick(a.target.player);
            h.assertTrue(a.target.player.getDeltaMovement().equals(Vec3.ZERO)&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Full resistance prevents both launch and terrain collision work");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void npcBlastAttacksRespectMobGriefing(GameTestHelper h) {
        try(var a=new Arena(h,180)) {
            var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            var rule=h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);boolean old=rule.get();
            try {
                npc.setPos(a.owner.player.position());npc.setNoAi(true);npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                com.example.immortal_cultivation_mod.entity.NpcProfile.ensure(npc,npc.npcArchetype());npc.setTarget(a.target.player);npc.tickCount=20;
                QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
                h.assertTrue(QiDomainSystem.blasting(npc),"NPC uses real release state");
                BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());rule.set(false,h.getLevel().getServer());
                a.target.player.invulnerableTime=0;a.target.player.setDeltaMovement(Vec3.ZERO);
                h.assertTrue(a.target.player.hurt(a.target.player.damageSources().mobAttack(npc),4),"NPC spell hit is accepted");a.tick(a.target.player);
                h.assertTrue(a.target.player.getDeltaMovement().z>1&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Mob griefing false preserves impulse but prevents terrain damage");
            }finally{rule.set(old,h.getLevel().getServer());QiDomainSystem.stop(npc);npc.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void releaseFistsDealMoreDamageWithMoreRemainingQi(GameTestHelper h) {
        try(var a=new Arena(h,230)) {
            var p=a.owner.player;var target=a.target.player;
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            // Measure the original incoming hit before armor without making a second damage call.
            var amounts=new ArrayList<Float>();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> capture=e->{
                if(e.getSource().getEntity()==p&&e.getEntity()==target)amounts.add(e.getAmount());
            };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,capture);
            try {
                for(int qi:new int[]{0,100,1000}) {
                    ModAttachments.setData(p,ModAttachments.getData(p).withQi(qi));
                    target.invulnerableTime=0;
                    h.assertTrue(target.hurt(target.damageSources().playerAttack(p),8),"Actual fist damage accepted");
                }
                h.assertTrue(amounts.size()==3&&amounts.get(0)==8&&amounts.get(1)==9&&amounts.get(2)==18,
                        "Current Qi adds increasing damage to original fist hit: "+amounts);
                h.assertTrue(ModAttachments.getData(p).qi()==1000,"Bonus reads remaining Qi; release upkeep remains the existing resource cost");
                float full=QiBlastCombat.fistBonus(p,target.damageSources().playerAttack(p));
                QiDomainSystem.view(p).percent=200;
                h.assertTrue(QiBlastCombat.fistBonus(p,target.damageSources().playerAttack(p))==full*2,"Release intensity scales fist bonus");
            }finally{NeoForge.EVENT_BUS.unregister(capture);}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void fistBonusDoesNotLeakIntoSwordsSpellsOrInactiveRelease(GameTestHelper h) {
        try(var a=new Arena(h,255)) {
            var p=a.owner.player;var target=a.target.player;
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(1000));
            var melee=target.damageSources().playerAttack(p);
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
            h.assertTrue(QiBlastCombat.fistBonus(p,melee)==0,"Sword receives no fist bonus");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            h.assertTrue(QiBlastCombat.fistBonus(p,target.damageSources().indirectMagic(p,p))==0,"Empty-handed spell caster receives no fist bonus");
            var snowball=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),p);
            try{h.assertTrue(QiBlastCombat.fistBonus(p,target.damageSources().thrown(snowball,p))==0,"Projectile receives no fist bonus");}
            finally{snowball.discard();}
            h.assertTrue(QiBlastCombat.fistBonus(p,melee)>0,"Actual empty-hand melee is empowered");
            QiDomainSystem.stop(p);
            h.assertTrue(QiBlastCombat.fistBonus(p,melee)==0,"Inactive release never augments damage");
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void empoweredFistsStillRespectDamageCancellationAndArmor(GameTestHelper h) {
        try(var a=new Arena(h,280)) {
            var p=a.owner.player;var target=a.target.player;
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(1000));p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            float health=target.getHealth();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> deny=e->{if(e.getEntity()==target)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,deny);
            try{target.hurt(target.damageSources().playerAttack(p),8);h.assertTrue(target.getHealth()==health,"Cancelled hit cannot apply a separate Qi damage instance");}
            finally{NeoForge.EVENT_BUS.unregister(deny);}
            // Remove the fixture's Golden Core armor modifier before comparing equipment armor.
            target.getAttribute(Attributes.ARMOR).removeModifiers();
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);target.invulnerableTime=0;
            target.hurt(target.damageSources().playerAttack(p),8);float unarmored=health-target.getHealth();
            target.setHealth(health);target.invulnerableTime=0;target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            target.hurt(target.damageSources().playerAttack(p),8);float armored=health-target.getHealth();
            h.assertTrue(unarmored>0&&armored<unarmored,"Armor reduces original hit and Qi bonus together: "+unarmored+"/"+armored);
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void physicalDomainContactsDoNotReceiveFistDamage(GameTestHelper h) {
        try(var a=new Arena(h,305)) {
            var p=a.owner.player;var target=a.target.player;
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(1000));p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var amounts=new ArrayList<Float>();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> capture=e->{
                if(e.getSource().getEntity()==p&&e.getEntity()==target)amounts.add(e.getAmount());
            };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,capture);
            try {
                target.invulnerableTime=0;
                h.assertTrue(QiDomainDamage.hurt(p,target,8,true),"Actual physical domain contact is accepted");
                BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());a.tick(target);
                h.assertTrue(target.getDeltaMovement().horizontalDistance()<1&&h.getLevel().getBlockState(wall).is(Blocks.STONE),"Physical domain contact cannot grant a launch or terrain impulse");
                h.assertTrue(amounts.equals(List.of(8F)),"Physical domain damage remains independent from direct fist strikes: "+amounts);
            }finally{NeoForge.EVENT_BUS.unregister(capture);}
        }h.succeed();
    }
    private static net.minecraft.world.entity.animal.Cow splashTarget(Arena a, double x, double z) {
        var mob=EntityType.COW.create(a.h.getLevel());
        mob.setNoAi(true);mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);
        mob.setPos(a.target.player.position().add(x,0,z));
        a.h.getLevel().addFreshEntity(mob);
        return mob;
    }
    @GameTest(template="fate_test")
    public static void blastFistsSplashNearbyTargetsOnceWithoutChaining(GameTestHelper h) {
        try(var a=new Arena(h,65)) {
            ModAttachments.setData(a.owner.player,ModAttachments.getData(a.owner.player).withQi(1000));
            var near=splashTarget(a,1.4,0);var outside=splashTarget(a,3.1,0);
            var amounts=new ArrayList<Float>();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> capture=e->{
                if(e.getEntity()==near)amounts.add(e.getAmount());
            };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,capture);
            try {
                float primaryBefore=a.target.player.getHealth(),ownerBefore=a.owner.player.getHealth();
                a.target.player.hurt(a.target.player.damageSources().playerAttack(a.owner.player),8);
                float dealt=primaryBefore-a.target.player.getHealth();
                h.assertTrue(near.getHealth()<1000&&amounts.size()==1
                                &&Math.abs(amounts.getFirst()-dealt*QiBlastCombat.FIST_SPLASH_DAMAGE)<.02,
                        "A landed blast fist splashes once for 35% without adding the fist Qi bonus again: "+amounts);
                a.tick(near);
                h.assertTrue(near.getDeltaMovement().horizontalDistance()<1,"Splash victim was not punched and cannot blast-launch");
                h.assertTrue(outside.getHealth()==1000,"Splash cannot chain from a nearby victim to a target beyond two blocks");
                h.assertTrue(a.owner.player.getHealth()==ownerBefore,"Fist splash never hurts its owner");
            }finally{NeoForge.EVENT_BUS.unregister(capture);near.discard();outside.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void fistSplashRespectsWallsAndAllies(GameTestHelper h) {
        try(var a=new Arena(h,90)) {
            var blocked=splashTarget(a,-1.5,0);var ally=splashTarget(a,1.4,0);
            var scoreboard=h.getLevel().getScoreboard();var team=scoreboard.addPlayerTeam("splash_"+UUID.randomUUID().toString().substring(0,8));
            try {
                scoreboard.addPlayerToTeam(a.owner.player.getScoreboardName(),team);
                scoreboard.addPlayerToTeam(ally.getScoreboardName(),team);
                BlockPos wall=a.target.player.blockPosition().west();
                for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)a.put(wall.offset(0,y,z),Blocks.STONE.defaultBlockState());
                a.target.player.hurt(a.target.player.damageSources().playerAttack(a.owner.player),8);
                h.assertTrue(blocked.getHealth()==1000&&ally.getHealth()==1000,
                        "Nearby targets behind solid walls and combat allies are excluded from fist splash");
            }finally{scoreboard.removePlayerTeam(team);blocked.discard();ally.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void cancelledHitsWeaponsAndDomainContactsCannotFistSplash(GameTestHelper h) {
        try(var a=new Arena(h,115)) {
            var nearby=splashTarget(a,1.4,0);var p=a.owner.player;var primary=a.target.player;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> deny=e->{if(e.getEntity()==primary)e.setCanceled(true);};
            try {
                NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,deny);
                try{primary.hurt(primary.damageSources().playerAttack(p),8);}
                finally{NeoForge.EVENT_BUS.unregister(deny);}
                h.assertTrue(nearby.getHealth()==1000,"Cancelled primary fist hit cannot splash");
                p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
                primary.invulnerableTime=0;primary.hurt(primary.damageSources().playerAttack(p),8);
                p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                primary.invulnerableTime=0;primary.hurt(primary.damageSources().indirectMagic(p,p),8);
                primary.invulnerableTime=0;QiDomainDamage.hurt(p,primary,8,true);
                QiDomainSystem.stop(p);primary.invulnerableTime=0;primary.hurt(primary.damageSources().playerAttack(p),8);
                h.assertTrue(nearby.getHealth()==1000,"Swords, spells, domain contacts and fists outside blast Qi cannot trigger fist splash");
            }finally{nearby.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void hitPayloadPreservesVictimContactAndElement(GameTestHelper h) {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var hit=new com.example.immortal_cultivation_mod.network.QiAuraPayloads.Hit(h.getLevel().dimension().location(),42,UUID.randomUUID(),1.5,2.25,-3.5,4,.8F);
            com.example.immortal_cultivation_mod.network.QiAuraPayloads.Hit.CODEC.encode(buffer,hit);
            h.assertTrue(hit.equals(com.example.immortal_cultivation_mod.network.QiAuraPayloads.Hit.CODEC.decode(buffer)),"Hit carries exact contact, target identity, method and power");
        }finally{buffer.release();}h.succeed();
    }
    @GameTest(template="fate_test")
    public static void actualNpcBrainUsesBlastQiToPunchPlayer(GameTestHelper h) {
        try(var a=new Arena(h,230)) {
            var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            try {
                npc.setPos(a.owner.player.position());
                com.example.immortal_cultivation_mod.entity.NpcProfile.ensure(npc,npc.npcArchetype());
                var profile=npc.getPersistentData().getCompound("ImmortalCultivationNpc");
                profile.putString("Realm",CORE);profile.putInt("Qi",100000);profile.putInt("MaxQi",100000);
                npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);npc.setTarget(a.target.player);npc.tickCount=20;
                com.example.immortal_cultivation_mod.entity.NpcProfile.recordPlayerAttack(npc,a.target.player);
                npc.setHealth(npc.getMaxHealth());QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
                h.assertTrue(QiDomainSystem.blasting(npc),"NPC activates actual Blast Qi");
                a.target.player.setPos(npc.position().add(0,0,2));a.target.player.setDeltaMovement(Vec3.ZERO);a.target.player.invulnerableTime=0;
                float before=a.target.player.getHealth();BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
                new com.example.immortal_cultivation_mod.entity.NpcCombatBrain(npc).tick();
                h.assertTrue(a.target.player.getHealth()<before-100,"Real AI chooses an empowered punch over ranged spells and probing: damage="+(before-a.target.player.getHealth())+", hand="+npc.getMainHandItem()+", state="+npc.getPersistentData().getCompound("ImmortalCultivationNpcCombatState"));
                h.assertTrue(npc.swinging,"NPC visibly swings fist");a.tick(a.target.player);
                h.assertTrue(a.target.player.getDeltaMovement().z>1&&a.target.player.getDeltaMovement().y>.3,"Actual NPC punch launches player backward and upward");
                h.assertTrue(h.getLevel().getBlockState(wall).isAir(),"NPC blast punch drives real terrain collision");
            }finally{QiDomainSystem.stop(npc);npc.discard();}
        }h.succeed();
    }
    @GameTest(template="fate_test")
    public static void confirmedNpcHitSurvivesTargetChangesDuringDamage(GameTestHelper h) {
        try(var a=new Arena(h,205)) {
            var npc=com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> change=e->{if(e.getSource().getEntity()==npc)npc.setTarget(null);};
            try {
                npc.setPos(a.owner.player.position());npc.setNoAi(true);npc.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                com.example.immortal_cultivation_mod.entity.NpcProfile.ensure(npc,npc.npcArchetype());npc.setTarget(a.target.player);npc.tickCount=20;
                QiDomainSystem.npcTick(new EntityTickEvent.Post(npc));
                BlockPos wall=a.wall(3,Blocks.STONE.defaultBlockState());
                NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,change);
                a.target.player.invulnerableTime=0;a.target.player.setDeltaMovement(Vec3.ZERO);
                h.assertTrue(a.target.player.hurt(a.target.player.damageSources().mobAttack(npc),4),"Damage was accepted while NPC was attacking the victim");
                h.assertTrue(npc.getTarget()==null,"Earlier post-damage handler changed AI target before launch processing");
                a.tick(a.target.player);
                h.assertTrue(a.target.player.getDeltaMovement().z>1&&h.getLevel().getBlockState(wall).isAir(),"Confirmed hostile hit still launches and breaks contact terrain after AI target changes");
            }finally{NeoForge.EVENT_BUS.unregister(change);QiDomainSystem.stop(npc);npc.discard();}
        }h.succeed();
    }
}
