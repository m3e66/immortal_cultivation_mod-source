package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.event.DeadRuinProtection;
import com.example.immortal_cultivation_mod.event.SmallSectProtection;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings.Category;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Bounded terrain work shared by spell impacts, beams, domains and creature techniques. */
@EventBusSubscriber(modid = "immortal_cultivation_mod")
public final class SpellDestruction {
    public static final int MAX_BREAKS = 16384;
    public static final int MAX_PROBES = 65536;
    public static final int MAX_JOBS = 128;
    private static final int MAX_POINTS = 8192;
    private static final long MAX_WORK_NANOS = 12_000_000;
    private static final Map<ServerLevel, Work> WORLDS = new LinkedHashMap<>();
    private static int tick = Integer.MIN_VALUE, changes, probes, particles, drops;
    private static long spent;
    private static int dimensionTurn;
    private static boolean completingImpact;
    private SpellDestruction() {}

    private record Point(BlockPos pos, BlockState expected, Entity owner, Vec3 origin, boolean drops,
                         boolean externalQi, BlockBreakingSettings.Source source) {}
    private record Neighbor(BlockPos pos, BlockState old) {}
    private static final class Work {
        final ArrayDeque<Job> jobs = new ArrayDeque<>();
        final LinkedHashMap<Long, Point> points = new LinkedHashMap<>();
        final LinkedHashMap<Long, Neighbor> neighbors = new LinkedHashMap<>();
    }
    private static final class Job {
        final Iterator<BlockPos> positions;
        final Predicate<BlockPos> shape;
        final Entity owner;
        final BlockBreakingSettings.Source source;
        final Vec3 origin;
        final float hardness;
        final Consumer<BlockPos> onBreak;
        final java.util.function.IntConsumer onFinish;
        final long expires;
        int remaining, broken;
        Job(Iterator<BlockPos> positions, Predicate<BlockPos> shape, Entity owner, Vec3 origin,
                float hardness, int limit, long expires, Consumer<BlockPos> onBreak, java.util.function.IntConsumer onFinish,
                BlockBreakingSettings.Source source) {
            this.positions=positions;this.shape=shape;this.owner=owner;this.origin=origin;
            this.hardness=hardness;remaining=limit;this.expires=expires;this.onBreak=onBreak;this.onFinish=onFinish;
            this.source=source;
        }
    }
    private static Work work(ServerLevel level) { return WORLDS.computeIfAbsent(level, ignored -> new Work()); }
    private static void reset(ServerLevel level) {
        int now=level.getServer().getTickCount();
        if(tick!=now){tick=now;changes=probes=particles=drops=0;spent=0;}
    }
    public static boolean hasBudget(ServerLevel level) {
        reset(level);
        return completingImpact || (changes<MAX_BREAKS && probes<MAX_PROBES && spent<MAX_WORK_NANOS);
    }
    private static Entity caster(Entity entity) {
        Entity actor = BlockBreakingSettings.source(Category.SPELLS, entity).actor();
        return actor instanceof Projectile projectile && projectile.getOwner()!=null ? projectile.getOwner() : actor;
    }
    private static boolean allowed(ServerLevel level, BlockPos pos, BlockState state, Entity owner, Vec3 origin, float hardness) {
        return allowed(level, pos, state, owner, origin, hardness, false);
    }
    private static boolean allowed(ServerLevel level, BlockPos pos, BlockState state, Entity owner,
            Vec3 origin, float hardness, boolean externalQi) {
        var source = BlockBreakingSettings.source(externalQi ? Category.EXTERNAL_QI : Category.SPELLS, owner);
        if (!BlockBreakingSettings.enabled(source.actor(), source.category())) return false;
        boolean modBlock = externalQi && net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock()).getNamespace().equals("immortal_cultivation_mod");
        boolean qiBreakable = externalQi && (modBlock || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN));
        if(state.isAir()
                || (state.is(BlockTags.DRAGON_IMMUNE) && !qiBreakable))return false;
        float strength=state.getDestroySpeed(level,pos);
        if((!state.is(Blocks.WATER) && (strength<0 || strength>hardness)) || SmallSectProtection.isLocked(level,pos) || DeadRuinProtection.isLocked(level,pos))return false;
        owner=caster(owner);
        if(owner!=null && owner.level()!=level)return false;
        if(owner instanceof Mob && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return false;
        Player player = BlockBreakingSettings.playerOwner(owner);
        if(player == null && owner instanceof Player directPlayer) player = directPlayer;
        if(player != null && (!level.mayInteract(player,pos)
                || !player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem())))return false;
        return !(owner instanceof LivingEntity living) || !CenterEffectSupport.isForcefieldRouteBlocked(living,origin,Vec3.atCenterOf(pos));
    }
    /** Immediate removal for collision/corrosion code which needs a truthful success result. */
    public static boolean breakNow(Level world, BlockPos position, boolean loot, Entity entity) {
        if(!(world instanceof ServerLevel level))return false;
        Entity owner=caster(entity);
        return remove(level,position,loot,owner,owner==null?Vec3.atCenterOf(position):owner.position(),Float.MAX_VALUE,null,false);
    }
    public static boolean eraseNow(ServerLevel level, BlockPos pos, Entity entity) {
        Entity owner=caster(entity);
        return remove(level,pos,false,owner,owner==null?Vec3.atCenterOf(pos):owner.position(),Float.MAX_VALUE,null,true);
    }
    /** Complete an instant crater without the ordinary per-tick cutoff; erase water and lava too. */
    public static boolean eraseImpactBlockNow(ServerLevel level, BlockPos pos, LivingEntity owner) {
        boolean previous = completingImpact;
        completingImpact = true;
        try {
            return remove(level, pos, false, owner, owner.position(), Float.MAX_VALUE, null, true);
        } finally { completingImpact = previous; }
    }
    /** Check protection before a projectile commits to travelling through a queued terrain cut. */
    public static boolean canBreakBlock(ServerLevel level, BlockPos position, LivingEntity owner) {
        return level.hasChunkAt(position) && level.getWorldBorder().isWithinBounds(position)
                && !level.isOutsideBuildHeight(position)
                && allowed(level, position, level.getBlockState(position), owner, owner.position(), Float.MAX_VALUE);
    }

    /** External Qi can cut obsidian, mod ores and machinery, while retaining world protections. */
    public static boolean canExternalQiBreakBlock(ServerLevel level, BlockPos position, LivingEntity owner) {
        return level.hasChunkAt(position) && level.getWorldBorder().isWithinBounds(position)
                && !level.isOutsideBuildHeight(position)
                && allowed(level, position, level.getBlockState(position), owner, owner.position(), Float.MAX_VALUE, true);
    }

    public static boolean breakExternalQiBlock(Level world, BlockPos position, boolean loot, Entity entity) {
        return breakBlock(world, position, loot, entity, true);
    }

    /** Accepted work may finish later. Each queued block keeps its original state, avoiding replacement-block damage. */
    public static boolean breakBlock(Level world, BlockPos position, boolean loot, Entity entity) {
        return breakBlock(world, position, loot, entity, false);
    }
    private static boolean breakBlock(Level world, BlockPos position, boolean loot, Entity entity, boolean externalQi) {
        if(!(world instanceof ServerLevel level) || !level.hasChunkAt(position))return false;
        Entity owner=caster(entity);
        Vec3 origin=owner==null?Vec3.atCenterOf(position):owner.position();
        if(!allowed(level,position,level.getBlockState(position),owner,origin,Float.MAX_VALUE,externalQi))return false;
        if(hasBudget(level) && (!loot || drops<24))return remove(level,position,loot,owner,origin,Float.MAX_VALUE,null,false,false,externalQi);
        Work work=work(level);long key=position.asLong();
        if(work.points.containsKey(key))return false;
        if(work.points.size()>=MAX_POINTS)return false;
        work.points.put(key,new Point(position.immutable(),level.getBlockState(position),owner,origin,loot,externalQi,
                BlockBreakingSettings.source(externalQi ? Category.EXTERNAL_QI : Category.SPELLS, entity)));
        return true;
    }
    private static boolean remove(ServerLevel level, BlockPos pos, boolean loot, Entity owner, Vec3 origin,
            float hardness, BlockState expected, boolean erase) {
        return remove(level,pos,loot,owner,origin,hardness,expected,erase,false);
    }
    private static boolean remove(ServerLevel level, BlockPos pos, boolean loot, Entity owner, Vec3 origin,
            float hardness, BlockState expected, boolean erase, boolean drainOnly) {
        return remove(level,pos,loot,owner,origin,hardness,expected,erase,drainOnly,false);
    }
    private static boolean remove(ServerLevel level, BlockPos pos, boolean loot, Entity owner, Vec3 origin,
            float hardness, BlockState expected, boolean erase, boolean drainOnly, boolean externalQi) {
        if(!hasBudget(level) || (loot && drops>=24))return false;
        long start=System.nanoTime();
        probes++;
        try {
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos))return false;
            BlockState state=level.getBlockState(pos);
            if(expected!=null && state!=expected)return false;
            boolean water=state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            if(!allowed(level,pos,state,owner,origin,hardness,externalQi)) {
                if(!water || !allowed(level,pos,state,owner,origin,Float.MAX_VALUE,externalQi))return false;
                drainOnly=true;
            }
            // Section updates replace per-block break packets; neighbor work has a separate bounded queue.
            Work work=work(level);
            if(drainOnly && !water)return false;
            BlockState replacement=(erase || water)?Blocks.AIR.defaultBlockState():state.getFluidState().createLegacyBlock();
            if(drainOnly && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
                replacement=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false);
            if(state==replacement)return false;
            var player = BlockBreakingSettings.playerOwner(owner);
            if(player != null
                    && net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                            new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,pos,state,player)).isCanceled())return false;
            var blockEntity = loot ? level.getBlockEntity(pos) : null;
            if(!level.setBlock(pos,replacement,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS))return false;
            changes++;
            if(loot && !state.is(Blocks.WATER)){drops++;Block.dropResources(state,level,pos,blockEntity,owner,net.minecraft.world.item.ItemStack.EMPTY);}
            work.neighbors.put(pos.asLong(),new Neighbor(pos.immutable(),state));
            if(!water && particles<8 && changes%32==1){particles++;level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),
                    pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,4,.3,.25,.3,.08);}
            return true;
        } finally {spent+=System.nanoTime()-start;}
    }
    /** Distributed impact columns: no block-volume lists or chunk loads at cast time. */
    public static int area(ServerLevel level, Entity owner, AABB bounds, Vec3 origin, int limit,
            float hardness, Predicate<BlockPos> shape, Consumer<BlockPos> onBreak) {
        return area(level,owner,bounds,origin,limit,hardness,shape,onBreak,null);
    }
    public static int area(ServerLevel level, Entity owner, AABB bounds, Vec3 origin, int limit,
            float hardness, Predicate<BlockPos> shape, Consumer<BlockPos> onBreak, java.util.function.IntConsumer onFinish) {
        Work work=work(level);
        if(limit<=0 || work.jobs.size()>=MAX_JOBS)return 0;
        var source = BlockBreakingSettings.source(Category.SPELLS, owner);
        if (!BlockBreakingSettings.enabled(source.actor(), source.category())) return 0;
        int accepted=Math.min(131072,limit);
        work.jobs.add(new Job(new SpellFractureTraversal(bounds,level.random.nextLong()),
                shape,caster(owner),origin,hardness,
                accepted,level.getGameTime()+400,onBreak,onFinish,source));
        return accepted;
    }
    public static int burst(ServerLevel level, Entity owner, Vec3 center, double radius, double vertical,
            int limit, float hardness) {
        double r=Math.max(.5,Math.min(48,radius)),v=Math.max(.5,Math.min(96,vertical));
        long seed=level.random.nextLong();
        return area(level,owner,new AABB(center,center).inflate(r,v,r),center,limit,hardness,
                pos->fractured(pos,center,r,v,seed),null);
    }
    /** Continuous shallow bowl with an irregular rim, without lobes or internal floating pockets. */
    public static boolean fractured(BlockPos pos, Vec3 center, double radius, double vertical, long seed) {
        double x=pos.getX()+.5-center.x,z=pos.getZ()+.5-center.z,y=pos.getY()+.5-center.y;
        double edge=.91+.085*noise(x/(radius*.45),z/(radius*.45),seed)
                +.035*noise(x/2.7,z/2.7,seed+31);
        double radial=(x*x+z*z)/(radius*radius*edge*edge);
        if(radial>=1)return false;
        double bowl=1-radial;
        double floor=-vertical*.65*Math.pow(bowl,.7)
                +Math.min(1,vertical*.08)*noise(x/3.5,z/3.5,seed+79)*bowl;
        return y>=floor && y<=vertical*Math.sqrt(bowl);
    }
    private static double noise(double x,double z,long seed) {
        int ix=(int)Math.floor(x),iz=(int)Math.floor(z);
        double tx=x-ix,tz=z-iz;
        tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);
        double a=grain(ix,iz,seed),b=grain(ix+1,iz,seed);
        double c=grain(ix,iz+1,seed),d=grain(ix+1,iz+1,seed);
        return (a+(b-a)*tx)*(1-tz)+(c+(d-c)*tx)*tz;
    }
    private static double grain(int x,int z,long seed) {
        long hash=seed^(x*73856093L)^(z*83492791L);
        hash=(hash^(hash>>>30))*0xbf58476d1ce4e5b9L;
        hash=(hash^(hash>>>27))*0x94d049bb133111ebL;
        return ((hash^(hash>>>31))&65535)/32767.5-1;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var levels=new ArrayList<>(WORLDS.keySet());
        if(levels.isEmpty())return;
        Collections.rotate(levels,dimensionTurn++%levels.size());
        for(ServerLevel level:levels) {
            reset(level);Work work=WORLDS.get(level);
            while(!work.jobs.isEmpty() && hasBudget(level)) {
                Job job=work.jobs.removeFirst();
                if(job.expires<level.getGameTime()){if(job.onFinish!=null)job.onFinish.accept(job.broken);continue;}
                if (!BlockBreakingSettings.enabled(job.source.actor(), job.source.category())) {
                    if(job.onFinish!=null)job.onFinish.accept(job.broken);
                    continue;
                }
                // Once started, publish an entire impact together; never expose partial crater growth.
                completingImpact=true;
                try {
                  BlockBreakingSettings.runWithSource(job.source.category(), job.source.actor(), () -> {
                    while(job.positions.hasNext()) {
                        BlockPos pos=job.positions.next();probes++;
                        if(!level.hasChunkAt(pos) || !job.shape.test(pos))continue;
                        BlockState state=level.getBlockState(pos);
                        boolean water=state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
                        if(job.remaining<=0 && !water)continue;
                        boolean pureWater=state.is(Blocks.WATER);
                        if(remove(level,pos,false,job.owner,job.origin,job.hardness,null,false,job.remaining<=0)) {
                            if(!pureWater && job.remaining>0 && level.getBlockState(pos).isAir()) {
                                job.remaining--;job.broken++;
                                if(job.onBreak!=null)job.onBreak.accept(pos.immutable());
                            }
                        }
                    }
                  });
                } finally {completingImpact=false;}
                if(job.onFinish!=null)job.onFinish.accept(job.broken);
            }
            var iterator=work.points.entrySet().iterator();
            while(iterator.hasNext() && hasBudget(level)) {
                Point point=iterator.next().getValue();
                if(point.drops && drops>=24)break;
                BlockBreakingSettings.runWithSource(point.source.category(), point.source.actor(), () ->
                        remove(level,point.pos,point.drops,point.owner,point.origin,Float.MAX_VALUE,point.expected,false,false,point.externalQi));
                iterator.remove();
            }
            var neighbors=work.neighbors.entrySet().iterator();int count=0;
            long neighborStart=System.nanoTime();
            while(neighbors.hasNext() && count++<256 && System.nanoTime()-neighborStart<2_000_000) {
                Neighbor neighbor=neighbors.next().getValue();neighbors.remove();
                if(level.hasChunkAt(neighbor.pos)) {
                    long start=System.nanoTime();
                    int flags=Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS;
                    BlockState current=level.getBlockState(neighbor.pos);
                    neighbor.old.updateIndirectNeighbourShapes(level,neighbor.pos,flags,16);
                    current.updateNeighbourShapes(level,neighbor.pos,flags,16);
                    current.updateIndirectNeighbourShapes(level,neighbor.pos,flags,16);
                    level.updateNeighborsAt(neighbor.pos,neighbor.old.getBlock());
                    spent+=System.nanoTime()-start;
                }
            }
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { WORLDS.remove(event.getLevel()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { WORLDS.clear();tick=Integer.MIN_VALUE;completingImpact=false; }
    public record Stats(int broken, int checked, int queuedJobs, int queuedPoints) {}
    public static Stats stats(ServerLevel level) {
        Work work=work(level);return new Stats(changes,probes,work.jobs.size(),work.points.size());
    }

    public static net.minecraft.world.level.Explosion explode(Level world, Entity owner, double x, double y, double z,
            float power, Level.ExplosionInteraction interaction) {
        return explode(world,owner,x,y,z,power,false,interaction);
    }
    public static net.minecraft.world.level.Explosion explode(Level world, Entity owner, double x, double y, double z,
            float power, boolean fire, Level.ExplosionInteraction interaction) {
        return explode(world,owner,null,null,new Vec3(x,y,z),power,fire,interaction);
    }
    /** Keep explosion damage/knockback immediate; terrain traversal uses the shared high-throughput queue. */
    public static net.minecraft.world.level.Explosion explode(Level world, Entity owner,
            net.minecraft.world.damagesource.DamageSource damage,
            net.minecraft.world.level.ExplosionDamageCalculator original, Vec3 center, float power,
            boolean fire, Level.ExplosionInteraction interaction) {
        if(!(world instanceof ServerLevel level) || interaction==Level.ExplosionInteraction.NONE)
            return world.explode(owner,damage,original,center,power,fire,interaction);
        var rules=original!=null?original:owner==null?new net.minecraft.world.level.ExplosionDamageCalculator()
                :new net.minecraft.world.level.EntityBasedExplosionDamageCalculator(owner);
        boolean[] started={false};
        var quick=new net.minecraft.world.level.ExplosionDamageCalculator() {
            @Override public Optional<Float> getBlockExplosionResistance(net.minecraft.world.level.Explosion explosion,
                    net.minecraft.world.level.BlockGetter blocks, BlockPos pos, BlockState state,
                    net.minecraft.world.level.material.FluidState fluid) { started[0]=true;return Optional.of(Float.MAX_VALUE); }
            @Override public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity target) {
                return rules.shouldDamageEntity(explosion,target);
            }
            @Override public float getEntityDamageAmount(net.minecraft.world.level.Explosion explosion,Entity target) {
                return rules.getEntityDamageAmount(explosion,target);
            }
            @Override public float getKnockbackMultiplier(Entity target) { return rules.getKnockbackMultiplier(target); }
        };
        var explosion=world.explode(owner,damage,quick,center,power,false,Level.ExplosionInteraction.NONE);
        if(!started[0] || (interaction==Level.ExplosionInteraction.MOB
                && !net.neoforged.neoforge.event.EventHooks.canEntityGrief(level,owner)))return explosion;
        int[] fires={0};
        double radius=Math.max(1,Math.min(48,power*1.35));long seed=level.random.nextLong();
        area(level,owner,new AABB(center,center).inflate(radius),center,131072,Float.MAX_VALUE,pos->{
            if(!fractured(pos,center,radius,radius,seed) || !level.hasChunkAt(pos))return false;
            BlockState state=level.getBlockState(pos);
            if(state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))return true;
            return state.getFluidState().isEmpty()
                    && rules.getBlockExplosionResistance(explosion,level,pos,state,state.getFluidState()).orElse(0F)<power*6
                    && rules.shouldBlockExplode(explosion,level,pos,state,power);
        },fire?pos->{
            if(fires[0]<24 && level.random.nextInt(12)==0 && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.below()).isSolidRender(level,pos.below())) {
                level.setBlock(pos,net.minecraft.world.level.block.BaseFireBlock.getState(level,pos),Block.UPDATE_ALL);
                fires[0]++;
            }
        }:null);
        return explosion;
    }
}
