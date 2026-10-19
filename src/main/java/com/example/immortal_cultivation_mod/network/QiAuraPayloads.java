package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.QiDomainSystem;
import com.example.immortal_cultivation_mod.physics.DomainVolume;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class QiAuraPayloads {
    private QiAuraPayloads() {}
    /** Compact target coverage snapshot; client never decides growth or resource transfers. */
    public record BloodLilyStatus(ResourceLocation dimension,int entityId,UUID uuid,int progress) implements CustomPacketPayload {
        public BloodLilyStatus {if(progress<0||progress>255)throw new IllegalArgumentException("Invalid Blood Lily state");}
        public static final Type<BloodLilyStatus> TYPE=new Type<>(id("blood_lily_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,BloodLilyStatus> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);b.writeByte(v.progress);
        },b->new BloodLilyStatus(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readUnsignedByte()));
        @Override public Type<BloodLilyStatus> type(){return TYPE;}
    }
    /** One fixed-size target snapshot per second, independent of whether its original caster still exists. */
    public record ColdTideStatus(ResourceLocation dimension,int entityId,UUID uuid,int progress,int frozenTicks) implements CustomPacketPayload {
        public ColdTideStatus {
            if(progress<0||progress>255||frozenTicks<0||frozenTicks>6000)throw new IllegalArgumentException("Invalid Cold-Tide state");
        }
        public static final Type<ColdTideStatus> TYPE=new Type<>(id("cold_tide_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ColdTideStatus> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);b.writeByte(v.progress);b.writeVarInt(v.frozenTicks);
        },b->new ColdTideStatus(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readUnsignedByte(),b.readVarInt()));
        @Override public Type<ColdTideStatus> type(){return TYPE;}
    }
    public record Action(int steps) implements CustomPacketPayload {
        // Zero toggles G; scroll steps are validated independently on the server.
        public static final Type<Action> TYPE=new Type<>(id("qi_release_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,v)->b.writeVarInt(v.steps),b->new Action(b.readVarInt()));
        @Override public Type<Action> type(){return TYPE;}
    }
    public record State(ResourceLocation dimension,int entityId,UUID uuid,String method,int percent,int mode,
                        int attackMode,float power,int opponent,boolean mastered,int ttl,float sunEnergy) implements CustomPacketPayload {
        public State(ResourceLocation dimension,int entityId,UUID uuid,String method,int percent,int mode,
                     int attackMode,float power,int opponent,boolean mastered,int ttl) {
            this(dimension,entityId,uuid,method,percent,mode,attackMode,power,opponent,mastered,ttl,0);
        }
        public static final Type<State> TYPE=new Type<>(id("flowing_qi_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);b.writeUtf(v.method,64);
            b.writeVarInt(v.percent);b.writeByte(v.mode);b.writeByte(v.attackMode);b.writeFloat(v.power);
            b.writeVarInt(v.opponent);b.writeBoolean(v.mastered);b.writeVarInt(v.ttl);b.writeFloat(v.sunEnergy);
        },b->new State(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readUtf(64),b.readVarInt(),b.readUnsignedByte(),
                b.readUnsignedByte(),b.readFloat(),b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readFloat()));
        @Override public Type<State> type(){return TYPE;}
    }
    public record Burst(ResourceLocation dimension,double x,double y,double z,int style) implements CustomPacketPayload {
        public static final Type<Burst> TYPE=new Type<>(id("method_domain_burst"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Burst> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeVarInt(v.style);
        },b->new Burst(b.readResourceLocation(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt()));
        @Override public Type<Burst> type(){return TYPE;}
    }
    /** Fixed-size server contact; no entity NBT or client-selected winners. */
    public record DomainClash(ResourceLocation dimension,int firstId,UUID firstUuid,int secondId,UUID secondUuid,
                              int firstStyle,int secondStyle,int winner,int age,int kind) implements CustomPacketPayload {
        public static final Type<DomainClash> TYPE=new Type<>(id("domain_clash"));
        public DomainClash {
            if(firstStyle<0||firstStyle>=17||secondStyle<0||secondStyle>=17||winner<0||winner>2||age<0
                    ||age>=DomainVolume.CLASH_DURATION_TICKS+DomainVolume.CLASH_RECOIL_PERIOD_TICKS||kind<0||kind>2)
                throw new IllegalArgumentException("Invalid domain contact");
        }
        public static final StreamCodec<RegistryFriendlyByteBuf,DomainClash> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.firstId);b.writeUUID(v.firstUuid);b.writeVarInt(v.secondId);b.writeUUID(v.secondUuid);
            b.writeByte(v.firstStyle);b.writeByte(v.secondStyle);b.writeByte(v.winner);b.writeByte(v.age);b.writeByte(v.kind);
        },b->new DomainClash(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readVarInt(),b.readUUID(),
                b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte()));
        @Override public Type<DomainClash> type(){return TYPE;}
    }
    /** Confirmed transfers, discharges and bounded status snapshots; no client-side gameplay. */
    public record DomainEffect(ResourceLocation dimension,int ownerId,UUID ownerUuid,int victimId,UUID victimUuid,
                               int style,int kind,int strength) implements CustomPacketPayload {
        public static final int DRAIN=1,WARP=2,ARC=3,MOUNTAIN=4,FROST=5,HEAL=6,VITALITY=7,WINTER=8,BONE=9,SOLAR=10,FIRE_PLUME=11,
                REBIRTH=12,BLOOD_MARK=13,WIND_LIFT=14,TIDE_WATER=15,TIDE_RIPPLE=16;
        public DomainEffect(ResourceLocation dimension,int ownerId,UUID ownerUuid,int victimId,UUID victimUuid,int style,int kind) {
            this(dimension,ownerId,ownerUuid,victimId,victimUuid,style,kind,1);
        }
        public static final Type<DomainEffect> TYPE=new Type<>(id("method_domain_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DomainEffect> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.ownerId);b.writeUUID(v.ownerUuid);
            b.writeVarInt(v.victimId);b.writeUUID(v.victimUuid);b.writeVarInt(v.style);b.writeByte(v.kind);b.writeByte(v.strength);
        },b->new DomainEffect(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readVarInt(),b.readUUID(),b.readVarInt(),b.readUnsignedByte(),b.readUnsignedByte()));
        @Override public Type<DomainEffect> type(){return TYPE;}
    }
    /** One-shot activation event, separate from the regularly refreshed aura state. */
    public record Activation(ResourceLocation dimension,double x,double y,double z,int style,int percent,int entityId,UUID uuid) implements CustomPacketPayload {
        public Activation(ResourceLocation dimension,double x,double y,double z,int style,int percent){this(dimension,x,y,z,style,percent,-1,new UUID(0,0));}
        public static final Type<Activation> TYPE=new Type<>(id("qi_release_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Activation> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);
            b.writeVarInt(v.style);b.writeVarInt(v.percent);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);
        },b->new Activation(b.readResourceLocation(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUUID()));
        @Override public Type<Activation> type(){return TYPE;}
    }
    /** A confirmed hit leaves a short flame on the victim as it is launched. */
    public record Hit(ResourceLocation dimension,int entityId,UUID uuid,double x,double y,double z,int style,float power) implements CustomPacketPayload {
        public static final Type<Hit> TYPE=new Type<>(id("qi_release_hit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Hit> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);
            b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeVarInt(v.style);b.writeFloat(v.power);
        },b->new Hit(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readFloat()));
        @Override public Type<Hit> type(){return TYPE;}
    }
    /** Emitted only after a real launch or successful, permitted block destruction. */
    public record Motion(ResourceLocation dimension,int entityId,UUID uuid,double x,double y,double z,
                         float dx,float dy,float dz,int style,int blockState,boolean collision) implements CustomPacketPayload {
        public static final Type<Motion> TYPE=new Type<>(id("qi_impact_motion"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Motion> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeVarInt(v.entityId);b.writeUUID(v.uuid);
            b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeFloat(v.dx);b.writeFloat(v.dy);b.writeFloat(v.dz);
            b.writeVarInt(v.style);b.writeVarInt(v.blockState);b.writeBoolean(v.collision);
        },b->new Motion(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),
                b.readFloat(),b.readFloat(),b.readFloat(),b.readVarInt(),b.readVarInt(),b.readBoolean()));
        @Override public Type<Motion> type(){return TYPE;}
    }
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,s);}
    /** Only real accepted wood cells; hard cap prevents unbounded payloads. */
    public record WoodBloom(ResourceLocation dimension,net.minecraft.core.BlockPos origin,java.util.List<net.minecraft.core.BlockPos> blocks) implements CustomPacketPayload {
        public WoodBloom {
            if(blocks.size()>256)throw new IllegalArgumentException("Too many wood cells");
            blocks=java.util.List.copyOf(blocks);
        }
        public static final Type<WoodBloom> TYPE=new Type<>(id("qi_wood_bloom"));
        public static final StreamCodec<RegistryFriendlyByteBuf,WoodBloom> CODEC=StreamCodec.of((b,v)->{
            b.writeResourceLocation(v.dimension);b.writeBlockPos(v.origin);b.writeVarInt(v.blocks.size());for(var p:v.blocks)b.writeBlockPos(p);
        },b->{
            var dimension=b.readResourceLocation();var origin=b.readBlockPos();int count=b.readVarInt();
            if(count<0||count>256)throw new IllegalArgumentException("Invalid wood cell count");
            var cells=new java.util.ArrayList<net.minecraft.core.BlockPos>(count);for(int i=0;i<count;i++)cells.add(b.readBlockPos());
            return new WoodBloom(dimension,origin,cells);
        });
        @Override public Type<WoodBloom> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("12");
        r.playToClient(DomainClash.TYPE,DomainClash.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.qi.DomainClashVisuals.accept(p));
        });
        r.playToClient(BloodLilyStatus.TYPE,BloodLilyStatus.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.qi.BloodLilyClient.accept(p));
        });
        r.playToClient(ColdTideStatus.TYPE,ColdTideStatus.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.qi.IceCoatingClient.accept(p));
        });
        r.playToClient(WoodBloom.TYPE,WoodBloom.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.woodBloom(p));
        });
        r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player() instanceof ServerPlayer player) {
                if(p.steps==0)QiDomainSystem.toggle(player);else QiDomainSystem.adjust(player,p.steps);
            }
        }));
        r.playToClient(Burst.TYPE,Burst.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.burst(p));
        });
        r.playToClient(DomainEffect.TYPE,DomainEffect.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.domainEffect(p));
        });
        r.playToClient(Activation.TYPE,Activation.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.activate(p));
        });
        r.playToClient(Hit.TYPE,Hit.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.hit(p));
        });
        r.playToClient(Motion.TYPE,Motion.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.motion(p));
        });
        r.playToClient(State.TYPE,State.CODEC,(p,c)->{
            if(FMLEnvironment.dist.isClient())c.enqueueWork(()->com.example.immortal_cultivation_mod.client.QiAuraRenderer.accept(p));
        });
    }
}
