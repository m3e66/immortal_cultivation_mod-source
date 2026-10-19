package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Confirmed impacts and armor reactions, independent of client removal/despawn guesses. */
public final class BeastVfxPayloads {
    public enum Style { SILK, RAT, SCORPION, WIND, EARTH, OWL, LIZARD, SPACE, THUNDER, FROST, BLOOD, DRAGON, FOX, GUARDIAN }
    public enum Phase { HIT, BLOCK, DEFLECT, ARMOR, LANDING, DISSOLVE }
    private static final Map<Entity,long[]> LIMITS = new WeakHashMap<>();
    private BeastVfxPayloads() {}
    public static Style style(Entity entity) {
        if(entity instanceof CultivationBestiaryBeastEntity beast) return switch(beast.kind()) {
            case SPIRIT_DEVOURING_RAT -> Style.RAT;
            case IRON_SCALE_SCORPION -> Style.SCORPION;
            case WIND_FEATHER_CLIFF_DEER -> Style.WIND;
            case STONE_DEVOURING_TORTOISE -> Style.EARTH;
            case EMBER_FEATHER_FIRE_OWL -> Style.OWL;
        };
        if(entity instanceof AzureSpiritSilkwormEntity) return Style.SILK;
        if(entity instanceof CrimsonScaleFlameLizardEntity) return Style.LIZARD;
        if(entity instanceof KongmingBeetleEntity) return Style.SPACE;
        if(entity instanceof ThunderhornRockBadgerEntity) return Style.THUNDER;
        if(entity instanceof FrostspineCaveBearEntity) return Style.FROST;
        if(entity instanceof BloodApeDemonGeneralEntity) return Style.BLOOD;
        if(entity instanceof CrimsonSkyFlameFloodDragonEntity) return Style.DRAGON;
        if(entity instanceof PhantomVeilFoxEntity) return Style.FOX;
        if(entity instanceof VaultGuardianEntity) return Style.GUARDIAN;
        return null;
    }
    public static int move(Entity entity) {
        if(entity instanceof CultivationBestiaryBeastEntity b) return b.action();
        if(entity instanceof CrimsonScaleFlameLizardEntity b) return b.getActionState();
        if(entity instanceof KongmingBeetleEntity b) return b.getActionState();
        if(entity instanceof ThunderhornRockBadgerEntity b) return b.getActionState();
        if(entity instanceof FrostspineCaveBearEntity b) return b.getActionState();
        if(entity instanceof BloodApeDemonGeneralEntity b) return b.getActionState();
        if(entity instanceof CrimsonSkyFlameFloodDragonEntity b) return b.getActionState();
        if(entity instanceof PhantomVeilFoxEntity b) return b.visualMove();
        if(entity instanceof VaultGuardianEntity b) return b.getActionState();
        return 0;
    }
    public record Impact(ResourceLocation dimension,Style style,Phase phase,int sourceId,int move,
                         Vec3 point,Vec3 direction,boolean projectile,int seed) implements CustomPacketPayload {
        public static final Type<Impact> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"beast_impact"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Impact> STREAM_CODEC=StreamCodec.of((b,p)->{
            b.writeResourceLocation(p.dimension); b.writeEnum(p.style); b.writeEnum(p.phase);
            b.writeVarInt(p.sourceId); b.writeVarInt(p.move);
            b.writeDouble(p.point.x);b.writeDouble(p.point.y);b.writeDouble(p.point.z);
            b.writeFloat((float)p.direction.x);b.writeFloat((float)p.direction.y);b.writeFloat((float)p.direction.z);
            b.writeBoolean(p.projectile);b.writeInt(p.seed);
        },b->new Impact(b.readResourceLocation(),b.readEnum(Style.class),b.readEnum(Phase.class),b.readVarInt(),b.readVarInt(),
                new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readFloat(),b.readFloat(),b.readFloat()),b.readBoolean(),b.readInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void emit(Entity beast,Phase phase,Vec3 point,Vec3 direction,boolean projectile) {
        if(!(beast.level() instanceof ServerLevel level) || style(beast)==null) return;
        long now=level.getGameTime();
        long[] limit=LIMITS.computeIfAbsent(beast,b->new long[]{now,0,0});
        if(limit[0]!=now){limit[0]=now;limit[1]=0;}
        if(limit[1]++>=4)return;
        limit[2]++;
        PacketDistributor.sendToPlayersNear(level,null,point.x,point.y,point.z,96,
                new Impact(level.dimension().location(),style(beast),phase,beast.getId(),move(beast),point,direction,projectile,((int)now ^ beast.getId()*31 ^ net.minecraft.core.BlockPos.containing(point).hashCode())));
    }
    public static int emittedCount(Entity beast) {
        long[] value=LIMITS.get(beast);return value==null?0:(int)value[2];
    }
    @EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
    public static final class Events {
        @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
            if(event.getNewDamage()<=0 || !Float.isFinite(event.getNewDamage()))return;
            Entity beast=event.getSource().getEntity();
            Entity direct=event.getSource().getDirectEntity();
            if(style(beast)==null && direct instanceof Projectile p)beast=p.getOwner();
            if(beast!=null && style(beast)!=null)
                emit(beast,Phase.HIT,event.getEntity().getBoundingBox().getCenter(),
                        direct instanceof Projectile p?p.getDeltaMovement():event.getEntity().position().subtract(beast.position()).normalize(),direct instanceof Projectile);
            Entity defender=event.getEntity();
            if(defender instanceof AzureSpiritSilkwormEntity worm && (worm.isCocooned() || worm.getSpitTicks()>0)
                    || defender instanceof CultivationBestiaryBeastEntity b && (b.kind()==BestiaryBeastKind.STONE_DEVOURING_TORTOISE
                    || b.kind()==BestiaryBeastKind.IRON_SCALE_SCORPION))
                emit(defender,Phase.ARMOR,defender.getBoundingBox().getCenter().add(
                        direct==null?Vec3.ZERO:direct.position().subtract(defender.getBoundingBox().getCenter()).normalize().scale(defender.getBbWidth()*.4)),
                        defender.getLookAngle(),false);
        }
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void block(ProjectileImpactEvent event) {
            if(event.isCanceled() || !(event.getRayTraceResult() instanceof BlockHitResult hit))return;
            Entity owner=event.getProjectile().getOwner();
            if(owner!=null && style(owner)!=null)
                emit(owner,Phase.BLOCK,hit.getLocation(),event.getProjectile().getDeltaMovement(),true);
        }
    }
    @EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Impact.TYPE,Impact.STREAM_CODEC,(p,c)->{
                if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.BeastAttackVfx.accept(p));
            });
        }
    }
}
