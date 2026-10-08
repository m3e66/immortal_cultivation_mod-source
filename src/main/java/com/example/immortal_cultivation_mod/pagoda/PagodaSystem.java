package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.*;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;

import net.minecraft.world.damagesource.DamageSource;

public final class PagodaSystem {
    private static final ThreadLocal<Boolean> TRANSFERRING=ThreadLocal.withInitial(()->false);
    private static final Map<UUID,Map<UUID,Integer>> STRUGGLE=new HashMap<>();
    public static boolean transferring(){return TRANSFERRING.get();}
    public static boolean commandAffected(Entity entity){
        if(!(entity.level() instanceof ServerLevel level))return false;
        return PagodaWorlds.isInterior(level.dimension())||PagodaData.get(level.getServer()).towers.values().stream()
                .anyMatch(tower->!tower.closing&&tower.owner.equals(entity.getUUID()));
    }
    public static boolean adminKill(Entity entity,DamageSource source){return source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)&&commandAffected(entity);}
    public static PagodaData.Tower towerFor(MinecraftServer server,UUID id){return PagodaData.get(server).towers.get(id);}
    public static PagodaData.Tower interior(MinecraftServer server,ResourceKey<Level> level){
        if(!PagodaWorlds.isInterior(level))return null;
        try{return towerFor(server,UUID.fromString(level.location().getPath().substring("pagoda_instances/".length())));}catch(IllegalArgumentException ex){return null;}
    }
    public static boolean prisoner(Entity e){return !e.level().isClientSide&&e.level() instanceof ServerLevel l&&interior(l.getServer(),l.dimension())!=null;}
    public static double containment(MinecraftServer server,PagodaData.Tower tower){
        var owner=server.getPlayerList().getPlayer(tower.owner);
        double alive=tower.ownerDead?.2:1;
        return PagodaPower.containment(tower.ownerPower,tower.injectedDamage,tower.health,owner==null?0:PagodaPower.qiFraction(owner))*alive;
    }
    public static boolean eligible(ServerPlayer owner,LivingEntity target){
        if(target==owner||!target.isAlive()||target.isInvulnerable()||target.isSpectator()||target instanceof PagodaEntity
                ||target instanceof TamableAnimal tame&&tame.isTame()||target instanceof net.minecraft.world.entity.OwnableEntity owned&&owned.getOwnerUUID()!=null||target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse&&horse.isTamed()||target.isPassenger()||target.isVehicle()
                ||NpcProfile.areCombatAllies(owner,target)||owner.isAlliedTo(target)||!owner.canAttack(target))return false;
        if(target instanceof Player p && (!PagodaConfig.CAPTURE_PLAYERS.get()||p.isCreative()||p.isSpectator()
                ||!owner.server.isPvpAllowed()||!owner.canHarmPlayer(p)))return false;
        if(PagodaPower.boss(target)&&!PagodaConfig.CAPTURE_BOSSES.get())return false;
        return target instanceof Monster||target instanceof Player
                ||target instanceof CultivationNpc&&NpcProfile.isCombatEnemy(owner,target)
                ||target instanceof Mob mob&&mob.getTarget()==owner;
    }
    public static PagodaData.Tower summon(ServerPlayer player,ItemStack stack,BlockPos position){
        return summon(player,stack,position,false,PagodaItem.identity(stack));
    }
    public static PagodaData.Tower summon(ServerPlayer player,ItemStack stack,BlockPos position,boolean gourd,UUID container){
        var server=player.server;var data=PagodaData.get(server);
        // A gourd's UUID owns its temporary world. Do not overwrite a world still releasing prisoners.
        UUID id=gourd&&container!=null?container:UUID.randomUUID();
        if(data.towers.containsKey(id)||gourd&&data.towers.values().stream().anyMatch(t->t.gourd&&Objects.equals(container,t.container)))return null;
        var tower=new PagodaData.Tower(id,player.getUUID(),player.level().dimension().location(),position,PagodaConfig.DURATION.get()*20);
        tower.gourd=gourd;tower.container=container;
        tower.ownerRealm=ModAttachments.getData(player).cultivationLevel();tower.unlockedFloors=PagodaFloors.unlocked(tower.ownerRealm);
        tower.ownerPower=PagodaPower.realm(player);tower.injectedDamage=QiInfusedWeapon.injectedQiDamage(stack);
        tower.maxHealth=tower.health=500+Math.min(100000,tower.ownerPower*.4+tower.injectedDamage*4);
        data.towers.put(id,tower);data.setDirty();
        var entity=new PagodaEntity(ModEntities.PAGODA.get(),player.serverLevel());entity.towerId=id;entity.setGourd(gourd);
        entity.setPos(Vec3.atBottomCenterOf(position).add(0,gourd?2:10,0));tower.entity=entity.getUUID();
        try { PagodaWorlds.getOrCreate(server,tower); }
        catch(RuntimeException ex){data.towers.remove(id);data.setDirty();org.slf4j.LoggerFactory.getLogger(PagodaSystem.class).error("Cannot create pagoda interior",ex);return null;}
        if(!player.serverLevel().addFreshEntity(entity)){tower.closing=true;data.setDirty();return null;}
        return tower;
    }
    public static boolean reinforce(ServerPlayer owner,PagodaData.Tower tower,int qi){
        if(!tower.owner.equals(owner.getUUID())||tower.closing||!tower.origin.equals(owner.level().dimension().location())
                ||owner.distanceToSqr(Vec3.atCenterOf(tower.position))>4096)return false;
        if(!ServerEvents.spendQiOrBlood(owner,ModAttachments.getData(owner),qi))return false;
        tower.injectedDamage+=qi/20.;tower.health=Math.min(tower.maxHealth,tower.health+qi*.15);
        ServerEvents.syncPlayerData(owner);PagodaData.get(owner.server).setDirty();return true;
    }
    public static boolean damage(PagodaEntity entity,DamageSource source,float amount){
        if(!(entity.level() instanceof ServerLevel level)||entity.towerId==null||!Float.isFinite(amount)||amount<=0)return false;
        var tower=towerFor(level.getServer(),entity.towerId);if(tower==null||tower.closing)return false;
        if(entity.core()){
            if(!(source.getEntity() instanceof LivingEntity attacker)||!tower.prisoners.containsKey(attacker.getUUID())||tower.prisoners.get(attacker.getUUID()).visitor)return false;
            var prisoner=tower.prisoners.get(attacker.getUUID());double c=containment(level.getServer(),tower);
            double power=combinedPower(level,tower,attacker,prisoner);
            if(PagodaPower.band(power,c)==PagodaPower.Band.TRAPPED){prisoner.progress=Math.max(0,prisoner.progress-1);return false;}
            if(source.getDirectEntity()==attacker&&!WeaponSkillAttack.isCurrentSource(source)
                    &&(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))){
                var weapon=attacker.getMainHandItem();float bonus=QiInfusedWeapon.injectedQiDamage(weapon);
                amount+=bonus;QiInfusedWeapon.consumeInjectedQiDamage(weapon,bonus);
            }
            double gain=Math.min(12,Math.max(.1,amount/Math.max(1,c)*30))*PagodaConfig.BREAKOUT_SPEED.get();
            prisoner.progress=Math.min(100,prisoner.progress+gain);prisoner.lastAttack=tower.age;
            tower.health=Math.max(0,tower.health-amount*.15);
            if(prisoner.progress>=100)tower.closing=true;
        }else{
            if(source.getEntity() instanceof ServerPlayer owner&&tower.owner.equals(owner.getUUID())&&owner.getMainHandItem().getItem() instanceof PagodaItem)
                return reinforce(owner,tower,100);
            tower.health=Math.max(0,tower.health-amount);
        }
        if(tower.health<=0)tower.closing=true;
        PagodaData.get(level.getServer()).setDirty();return true;
    }
    private static double combinedPower(ServerLevel level,PagodaData.Tower tower,LivingEntity attacker,PagodaData.Prisoner prisoner){
        double cooperation=tower.prisoners.values().stream().filter(p->!p.id.equals(prisoner.id)&&p.lastAttack>0&&tower.age-p.lastAttack<=40)
                .map(p->level.getEntity(p.id)).filter(LivingEntity.class::isInstance).filter(e->((LivingEntity)e).isAlive())
                .mapToDouble(e->PagodaPower.power((LivingEntity)e,0)*.15).sum();
        return PagodaPower.power(attacker,prisoner.progress)+Math.min(cooperation,PagodaPower.power(attacker,0)*.5);
    }
    public static void tick(MinecraftServer server){
        var data=PagodaData.get(server);
        for(var tower:new ArrayList<>(data.towers.values())){
            var currentOwner=server.getPlayerList().getPlayer(tower.owner);
            if(currentOwner!=null&&currentOwner.isAlive()){tower.ownerPower=PagodaPower.realm(currentOwner);tower.ownerRealm=ModAttachments.getData(currentOwner).cultivationLevel();tower.unlockedFloors=PagodaFloors.unlocked(tower.ownerRealm);}
            if(!tower.gourd&&tower.container!=null&&!tower.closing){
                boolean present=currentOwner!=null&&currentOwner.isAlive();
                tower.inactiveTicks=present?0:tower.inactiveTicks+1;
                if(tower.inactiveTicks>=1200||present&&tower.age%20==0&&!payUpkeep(currentOwner))tower.closing=true;
            }
            tower.age++;if(tower.gourd&&tower.age>=tower.lifetime)tower.closing=true;
            var outside=server.getLevel(ResourceKey.create(Registries.DIMENSION,tower.origin));
            if(outside!=null){
                if(!tower.closing)PagodaWorlds.holdOutside(outside,tower);else PagodaWorlds.releaseOutside(outside,tower);
                Entity physical=tower.entity==null?null:outside.getEntity(tower.entity);
                if(physical instanceof PagodaEntity e){
                    if(!e.isAlive())tower.closing=true;
                    else {
                        double size=Math.clamp(1+Math.log1p(tower.injectedDamage)/12+Math.log1p(tower.ownerPower)/30,1,2.25);
                        e.updateVisual(tower.progress(),size);
                        e.setPos(Vec3.atBottomCenterOf(tower.position).add(0,tower.gourd?2:Math.max(0,40-tower.age)*.25+(ownerInside(tower)?2+Math.sin(tower.age*.04)*.15:0),0));
                        // A deployed tower stays anchored; only the gourd keeps its hover rotation.
                        e.setYRot(tower.gourd?(float)(tower.age*.25):0F);
                        e.yBodyRot=e.getYRot();e.yHeadRot=e.getYRot();
                        if(tower.age%20==0)PagodaVfx.field(outside,e.position(),tower.pressure*(1+Math.log1p(tower.ownerPower)/6));
                        if(!tower.gourd&&tower.age==40){PagodaVfx.ripple(outside,e.position());outside.playSound(null,e.blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1,.5F);}
                    }
                }
                if(!tower.closing&&(tower.gourd||tower.age>40)&&tower.progress()<75&&PagodaFloors.hasRoom(tower))pull(server,outside,tower);
            }
            if(!tower.prisoners.isEmpty()||!tower.closing){
                var inside=PagodaWorlds.getOrCreate(server,tower);
                ensureCores(inside,tower);
                if(tower.enterRequested&&currentOwner!=null&&enterOwner(currentOwner,tower))tower.enterRequested=false;
                if(!tower.gourd&&tower.age%20==0){
                    for(var living:inside.getEntitiesOfClass(LivingEntity.class,new AABB(-10,64,-10,10,145,10),e->e.isAlive()&&!e.getUUID().equals(tower.owner)&&!(e instanceof PagodaEntity)))
                        com.example.immortal_cultivation_mod.spell.Weiya.applyPressureEffects(tower.ownerRealm,living);
                }
                if(!tower.gourd)PagodaWorlds.updateFloorLocks(inside,tower);
                if(tower.gourd)PagodaWorlds.updateWater(inside,tower,GourdSystem.water(data,tower.container));
                if(tower.age%20==0)for(int floor=0;floor<(tower.gourd?3:tower.unlockedFloors);floor++)PagodaVfx.field(inside,new Vec3(.5,65+floor*9,.5),tower.pressure);
                for(var prisoner:new ArrayList<>(tower.prisoners.values()))tickPrisoner(server,inside,tower,prisoner);
            }
            tower.pressure=Math.max(0,tower.health/tower.maxHealth)*(1-tower.progress()*.005);
            if(tower.closing){
                if(outside!=null&&tower.entity!=null&&outside.getEntity(tower.entity) instanceof PagodaEntity e){
                    if(!tower.gourd)PagodaVfx.burst(outside,e.position(),tower.progress()>=100||tower.health<=0);
                    e.discard();
                }
                for(var prisoner:new ArrayList<>(tower.prisoners.values()))release(server,tower,prisoner);
                var interior=server.getLevel(PagodaWorlds.key(tower.id));
                if(interior!=null&&tower.prisoners.isEmpty()){
                    for(var e:new ArrayList<Entity>(){{interior.getAllEntities().forEach(this::add);}}){
                        if(e instanceof PagodaEntity){e.discard();continue;}
                        if(outside!=null){Vec3 safe=safePosition(outside,tower.position,e);if(safe!=null)transfer(e,outside,safe);}
                    }
                }
                if(tower.prisoners.isEmpty()&&PagodaWorlds.retire(server,tower)){data.towers.remove(tower.id);STRUGGLE.remove(tower.id);data.setDirty();}
            }
        }
        if(!data.towers.isEmpty())data.setDirty();
    }
    private static void pull(MinecraftServer server,ServerLevel level,PagodaData.Tower tower){
        var owner=server.getPlayerList().getPlayer(tower.owner);if(owner==null||!owner.isAlive())return;
        double radius=PagodaConfig.RADIUS.get(),contain=containment(server,tower);Vec3 center=Vec3.atBottomCenterOf(tower.position);
        var attempts=STRUGGLE.computeIfAbsent(tower.id,ignored->new HashMap<>());
        Set<UUID> touched=new HashSet<>();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->eligible(owner,e)&&e.distanceToSqr(center)<=radius*radius)){
            if(!PagodaFloors.hasRoom(tower))break;
            if(ForcefieldCenterBehavior.interceptAttackPath(owner,center.add(0,1,0),target.position(),1))continue;
            if(!captureAllowed(owner,target))continue;
            double distance=target.position().distanceTo(center),power=PagodaPower.power(target,0);
            boolean resist=power>=contain*1.5;
            Vec3 force=center.add(0,.5,0).subtract(target.position());
            if(force.lengthSqr()>1E-6)target.setDeltaMovement(target.getDeltaMovement().scale(.65).add(force.normalize().scale(resist?.025:.10+.28*(1-distance/radius))));
            target.hurtMarked=true;PagodaVfx.chain(level,target.position(),center,resist);
            touched.add(target.getUUID());
            if(resist){attempts.remove(target.getUUID());continue;}
            int struggle=attempts.merge(target.getUUID(),1,Integer::sum);
            if(struggle>=40&&distance<=3.5){
                int extra=PagodaConfig.TARGET_COST.get()+(int)Math.ceil(PagodaConfig.TARGET_COST.get()*Math.max(0,power/contain-.6));
                var before=ModAttachments.getData(owner);float beforeHealth=owner.getHealth();
                if(!ServerEvents.spendQiOrBlood(owner,before,extra))continue;
                ServerEvents.syncPlayerData(owner);
                Vec3 capturePosition=target.position();
                if(capture(server,tower,target)){attempts.remove(target.getUUID());PagodaVfx.ripple(level,capturePosition);}
                else {ModAttachments.setData(owner,before);owner.setHealth(beforeHealth);ServerEvents.syncPlayerData(owner);}
            }
        }
        attempts.keySet().retainAll(touched);
    }
    public static boolean captureAllowed(ServerPlayer owner,LivingEntity target){
        // Capture permission is not an attack. Posting AttackEntityEvent here
        // runs real weapon damage and combo handlers on every suction tick.
        return !NeoForge.EVENT_BUS.post(new PagodaCaptureEvent(owner,target)).isCanceled();
    }
    public static boolean capture(MinecraftServer server,PagodaData.Tower tower,LivingEntity entity){
        if(tower.closing||!PagodaFloors.hasRoom(tower)||prisoner(entity))return false;
        var inside=PagodaWorlds.getOrCreate(server,tower);
        if(!inside.isPositionEntityTicking(new BlockPos(3,65,3))||!inside.areEntitiesLoaded(0L))return false;
        var p=new PagodaData.Prisoner(entity.getUUID(),entity.level().dimension().location(),entity.blockPosition(),entity instanceof Player);
        int floor=tower.gourd?0:PagodaFloors.available(tower,entity);if(floor<0)return false;
        p.floor=floor;tower.prisoners.put(p.id,p);PagodaData.get(server).setDirty();
        // Vanilla/NeoForge transition transfers serialized inventories, attachments and capabilities.
        
        Entity moved=transfer(entity,inside,new Vec3(3.5,65+floor*9,3.5));
        if(moved==null){tower.prisoners.remove(p.id);PagodaData.get(server).setDirty();return false;}
        if(moved instanceof Mob mob)mob.setTarget(null);
        return true;
    }
    private static void ensureCores(ServerLevel level,PagodaData.Tower tower){
        if(!level.areEntitiesLoaded(0L)||!level.isPositionEntityTicking(new BlockPos(0,65,0)))return;
        for(int floor=0;floor<(tower.gourd?3:9);floor++){
            int y=66+floor*9;
            if(!level.getEntitiesOfClass(PagodaEntity.class,new AABB(-2,y-1,-2,2,y+3,2),PagodaEntity::core).isEmpty())continue;
            var core=new PagodaEntity(ModEntities.PAGODA.get(),level);core.towerId=tower.id;core.setGourd(tower.gourd);core.setCore(true);core.setPos(.5,y,.5);level.addFreshEntity(core);
        }
    }
    private static void tickPrisoner(MinecraftServer server,ServerLevel level,PagodaData.Tower tower,PagodaData.Prisoner p){
        Entity found=level.getEntity(p.id);
        if(found==null){
            var elsewhere=find(server,p.id);
            if(elsewhere!=null&&elsewhere.level()!=level){tower.prisoners.remove(p.id);return;}
            return; // Offline/unloaded never means deleted. Keep world until release succeeds.
        }
        if(!(found instanceof LivingEntity entity)||!entity.isAlive()){tower.prisoners.remove(p.id);return;}
        if(tower.closing)return;
        if(p.visitor){
            int floor=PagodaFloors.floor(entity.getY());
            if(entity.getY()<65||entity.getY()>=64+tower.unlockedFloors*9||Math.abs(entity.getX())>8||Math.abs(entity.getZ())>8)entity.teleportTo(3.5,65,3.5);
            return;
        }
        if(!tower.gourd&&PagodaFloors.floor(entity.getY())!=p.floor)entity.teleportTo(3.5,65+p.floor*9,3.5);
        if(Math.abs(entity.getX())>8||Math.abs(entity.getZ())>8||entity.getY()<64||entity.getY()>(tower.gourd?93:145)||tower.gourd&&entity.getX()*entity.getX()+entity.getZ()*entity.getZ()>Math.pow(PagodaWorlds.gourdRadius((int)entity.getY())-1,2))entity.teleportTo(3.5,65+p.floor*9,3.5);
        double contain=containment(server,tower),power=combinedPower(level,tower,entity,p);
        var band=PagodaPower.band(power,contain);
        if(band==PagodaPower.Band.TRAPPED)p.progress=Math.max(0,p.progress-.2);
        else if(tower.age-p.lastAttack>40)p.progress=Math.max(0,p.progress-.035);
        if(band==PagodaPower.Band.FORCED){
            if(p.forcedSince<0)p.forcedSince=tower.age;
            PagodaVfx.chain(level,entity.position(),new Vec3(.5,entity.getY(),.5),true);
            if(tower.age-p.forcedSince>=20){release(server,tower,p);return;}
        }else p.forcedSince=-1;
        if(tower.age%20==0){
            int floor=tower.gourd?Math.clamp((int)((entity.getY()-64)/9),0,2):p.floor;
            if(!tower.gourd)com.example.immortal_cultivation_mod.spell.Weiya.applyPressureEffects(tower.ownerRealm,entity);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,band==PagodaPower.Band.TRAPPED?3:1));
            float amount=tower.gourd?GourdSystem.damagePerSecond(entity):(float)(PagodaConfig.PRESSURE_DAMAGE.get()*tower.pressure*(1+floor*.25));
            if(amount>0)entity.hurt(entity.damageSources().magic(),amount);
            PagodaVfx.pressure(level,entity.position(),tower.pressure);
        }
        if(entity instanceof Mob mob && band!=PagodaPower.Band.TRAPPED){
            var cores=level.getEntitiesOfClass(PagodaEntity.class,mob.getBoundingBox().inflate(10),PagodaEntity::core);
            var core=cores.stream().min(Comparator.comparingDouble(c->Math.abs(c.getY()-mob.getY()))).orElse(null);
            if(core!=null){
                mob.setTarget(core);mob.getNavigation().moveTo(core,1.1);
                if(mob.distanceToSqr(core)<9&&tower.age%20==0){mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);core.hurt(mob.damageSources().mobAttack(mob),(float)Math.max(1,attack(mob)));}
            }
        }
        for(var core:level.getEntitiesOfClass(PagodaEntity.class,new AABB(-2,64,-2,2,tower.gourd?94:145,2),PagodaEntity::core))core.updateVisual(tower.progress(),1);
    }
    public static double attack(LivingEntity e){return e.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:e.getAttributeValue(Attributes.ATTACK_DAMAGE);}
    public static Entity find(MinecraftServer s,UUID id){var player=s.getPlayerList().getPlayer(id);if(player!=null)return player;for(var l:s.getAllLevels()){var e=l.getEntity(id);if(e!=null)return e;}return null;}
    public static boolean release(MinecraftServer server,PagodaData.Tower tower,PagodaData.Prisoner p){
        var e=find(server,p.id);if(e==null)return false;
        var destination=server.getLevel(ResourceKey.create(Registries.DIMENSION,p.origin));if(destination==null)return false;
        if(e.level()==destination){tower.prisoners.remove(p.id);return true;}
        Vec3 safe=safePosition(destination,p.position,e);
        if(safe==null||transfer(e,destination,safe)==null)return false;
        tower.prisoners.remove(p.id);PagodaData.get(server).setDirty();return true;
    }
    public static Vec3 safePosition(ServerLevel level,BlockPos origin,Entity entity){
        for(int r=0;r<=12;r++)for(int dy=0;dy<=8;dy++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.abs(x)!=r&&Math.abs(z)!=r)continue;
            BlockPos pos=origin.offset(x,dy,z);level.getChunk(pos);
            Vec3 point=Vec3.atBottomCenterOf(pos);AABB box=entity.getBoundingBox().move(point.subtract(entity.position()));
            if(level.getWorldBorder().isWithinBounds(pos)&&level.getBlockState(pos.below()).isSolid()
                    &&level.noCollision(entity,box)&&!level.containsAnyLiquid(box))return point;
        }
        BlockPos spawn=level.getSharedSpawnPos();
        for(int r=0;r<=16;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.abs(x)!=r&&Math.abs(z)!=r)continue;
            BlockPos surface=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.offset(x,0,z));
            Vec3 point=Vec3.atBottomCenterOf(surface);AABB box=entity.getBoundingBox().move(point.subtract(entity.position()));
            if(level.getWorldBorder().isWithinBounds(surface)&&level.getBlockState(surface.below()).isSolid()&&level.noCollision(entity,box)&&!level.containsAnyLiquid(box))return point;
        }
        return null; // Keep the UUID world and retry; never release into solid blocks or void.
    }
    private static Entity transfer(Entity entity,ServerLevel destination,Vec3 position){
        var originalLevel=(ServerLevel)entity.level();Vec3 originalPosition=entity.position();
        float yaw=entity.getYRot(),pitch=entity.getXRot();
        var originalState=entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        boolean previous=TRANSFERRING.get();TRANSFERRING.set(true);
        try{
            Entity moved=entity.changeDimension(new DimensionTransition(destination,position,Vec3.ZERO,yaw,pitch,
                    value->value.placePortalTicket(BlockPos.containing(position))));
            if(moved!=null&&!moved.isAddedToLevel()||moved==null&&entity.isRemoved()){
                // Vanilla removes the source before EntityJoinLevelEvent can reject the
                // destination. Reattach the original instance, never an NBT clone.
                entity.revive();
                if(entity instanceof ServerPlayer player){
                    player.changeDimension(new DimensionTransition(originalLevel,originalPosition,Vec3.ZERO,yaw,pitch,DimensionTransition.DO_NOTHING));
                    player.setServerLevel(originalLevel);
                }
                if(moved!=null&&moved!=entity)moved.discard();
                // Mob.removeAfterChangingDimensions clears original equipment counts.
                // Restore state onto that same instance only when transfer failed.
                entity.load(originalState);
                entity.moveTo(originalPosition.x,originalPosition.y,originalPosition.z,yaw,pitch);
                {
                    var manager=((com.example.immortal_cultivation_mod.mixin.PagodaEntityManagerAccessor)originalLevel).pagoda$entities();
                    if(!manager.isLoaded(entity.getUUID())&&manager.addNewEntityWithoutEvent(entity))entity.onAddedToLevel();
                }
                return null;
            }
            return moved;
        }
        finally{TRANSFERRING.set(previous);}
    }
    public static void returnInside(ServerPlayer player,PagodaData.Tower tower){transfer(player,PagodaWorlds.getOrCreate(player.server,tower),new Vec3(3.5,65,3.5));}
    public static boolean ownerInside(PagodaData.Tower tower){var p=tower.prisoners.get(tower.owner);return p!=null&&p.visitor;}
    public static boolean payUpkeep(ServerPlayer owner){
        if(owner.isCreative())return true;
        var data=ModAttachments.getData(owner);int cost=PagodaConfig.UPKEEP.get();
        if(data.qi()<cost)return false;
        ModAttachments.setData(owner,data.withQi(data.qi()-cost));ServerEvents.syncPlayerData(owner);return true;
    }
    public static boolean enterOwner(ServerPlayer owner,PagodaData.Tower tower){
        if(tower.gourd||tower.closing||!tower.owner.equals(owner.getUUID())||PagodaWorlds.isInterior(owner.level().dimension()))return false;
        var inside=PagodaWorlds.getOrCreate(owner.server,tower);
        if(!inside.isPositionEntityTicking(new BlockPos(3,65,3))||!inside.areEntitiesLoaded(0L))return false;
        var record=new PagodaData.Prisoner(owner.getUUID(),owner.level().dimension().location(),owner.blockPosition(),true);record.visitor=true;
        tower.prisoners.put(record.id,record);
        if(transfer(owner,inside,new Vec3(3.5,65,3.5))==null){tower.prisoners.remove(record.id);return false;}
        PagodaData.get(owner.server).setDirty();return true;
    }
    public static void clear(){STRUGGLE.clear();TRANSFERRING.remove();}
    private PagodaSystem(){}
}
