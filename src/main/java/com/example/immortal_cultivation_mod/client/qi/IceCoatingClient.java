package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.network.QiAuraPayloads;
import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Re-renders the actual posed vanilla or GeckoLib mesh. Never substitutes a bounding-box shell. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class IceCoatingClient {
    private static final UUID COLD_TIDE_SOURCE=new UUID(0x434F4C4454494445L,0);
    private static final ResourceLocation VANILLA_ICE=ResourceLocation.withDefaultNamespace("textures/block/ice.png");
    private static final Map<UUID,Long> LOCKED=new HashMap<>();
    private static final Map<UUID,Integer> LOCKED_POSES=new HashMap<>();
    private record Source(float progress,long until) { }
    private record Surface(Vec3 offset,Vec3 normal) { }
    public record Appearance(float progress,float cracking) { }
    private static final class Frozen {
        final int id;final UUID uuid;
        final Map<UUID,Source> sources=new HashMap<>();
        final List<Surface> surfaces=new ArrayList<>();
        float previous,target;long changed,crackAt=-1;
        int sampled;
        Frozen(LivingEntity entity,long now){id=entity.getId();uuid=entity.getUUID();changed=now;}
        float progress(double time){return previous+(target-previous)*(float)Math.clamp((time-changed)/20,0,1);}
        void moveTo(float value,long now){previous=progress(now);target=value;changed=now;}
    }
    private static final Map<UUID,Frozen> FROZEN=new HashMap<>();
    private static final Map<ResourceLocation,RenderType> MATERIALS=new HashMap<>();
    private static ClientLevel world;
    private static ShaderInstance shader;
    private static int drawn;
    public static long vanillaDraws,geoDraws,vertices,shatterEvents,surfaceFragments,crackFrames;
    private IceCoatingClient() { }
    public static void clear(){FROZEN.clear();LOCKED.clear();LOCKED_POSES.clear();world=null;}
    public static void beginFrame(){drawn=0;}
    public static boolean ready(){return shader!=null;}
    public static int active(){return FROZEN.size();}
    public static boolean isFrozen(LivingEntity entity){return world==entity.level()&&LOCKED.getOrDefault(entity.getUUID(),0L)>world.getGameTime();}
    public static void registerShader(RegisterShadersEvent event)throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"winter_coating"),DefaultVertexFormat.NEW_ENTITY),value->shader=value);
    }
    public static void accept(QiAuraPayloads.ColdTideStatus packet) {
        var level=Minecraft.getInstance().level;
        if(world!=level){clear();world=level;}
        if(world==null||!world.dimension().location().equals(packet.dimension()))return;
        var entity=world.getEntity(packet.entityId());
        if(!(entity instanceof LivingEntity victim)||!entity.getUUID().equals(packet.uuid())
                ||victim instanceof net.minecraft.world.entity.monster.Slime)return;
        var frozen=FROZEN.get(packet.uuid());long now=world.getGameTime();
        if(packet.frozenTicks()>0){LOCKED.put(packet.uuid(),now+packet.frozenTicks());LOCKED_POSES.putIfAbsent(packet.uuid(),victim.tickCount);}
        else {LOCKED.remove(packet.uuid());LOCKED_POSES.remove(packet.uuid());}
        if(packet.progress()==0){if(frozen!=null)frozen.sources.remove(COLD_TIDE_SOURCE);return;}
        if(frozen==null){if(FROZEN.size()>=96)return;frozen=new Frozen(victim,now);FROZEN.put(packet.uuid(),frozen);}
        frozen.sources.put(COLD_TIDE_SOURCE,new Source(packet.progress()/255F,now+Math.max(45,packet.frozenTicks())));
        float progress=0;for(var source:frozen.sources.values())progress=Math.max(progress,source.progress);
        if(frozen.target!=progress)frozen.moveTo(progress,now);frozen.crackAt=-1;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void frozenActor(EntityTickEvent.Pre event) {
        var entity=event.getEntity();if(!entity.level().isClientSide||world!=entity.level()||!entity.isAlive())return;
        if(LOCKED.getOrDefault(entity.getUUID(),0L)>world.getGameTime()) {
            entity.tickCount=LOCKED_POSES.getOrDefault(entity.getUUID(),entity.tickCount);
            if(entity instanceof LivingEntity living&&living.hurtTime>0)living.hurtTime--;
            entity.setDeltaMovement(Vec3.ZERO);event.setCanceled(true);
        }
    }
    public static void tick() {
        var level=Minecraft.getInstance().level;
        if(world!=level){clear();world=level;}
        if(world==null)return;
        LOCKED.entrySet().removeIf(entry->entry.getValue()<=world.getGameTime());
        LOCKED_POSES.keySet().retainAll(LOCKED.keySet());
        long now=world.getGameTime();var iterator=FROZEN.values().iterator();
        while(iterator.hasNext()) {
            var frozen=iterator.next();var entity=world.getEntity(frozen.id);
            if(!(entity instanceof LivingEntity living)||!entity.getUUID().equals(frozen.uuid)){iterator.remove();continue;}
            frozen.sources.entrySet().removeIf(entry->entry.getValue().until<now);
            if(frozen.sources.isEmpty()||!living.isAlive()) {
                if(frozen.crackAt<0)frozen.crackAt=now;
                if(now-frozen.crackAt>=10||!living.isAlive()){shatter(frozen,living);iterator.remove();}
            }else {
                float progress=0;for(var source:frozen.sources.values())progress=Math.max(progress,source.progress);
                if(progress!=frozen.target)frozen.moveTo(progress,now);
                if(living.position().subtract(new Vec3(living.xo,living.yo,living.zo)).lengthSqr()>.002&&now%6==0)
                    fragments(frozen,living,2);
            }
        }
    }
    private static void shatter(Frozen frozen,LivingEntity entity){shatterEvents++;fragments(frozen,entity,32);}
    private static void fragments(Frozen frozen,LivingEntity entity,int count) {
        if(frozen.surfaces.isEmpty())return;
        var ice=new QiElementLook(0x9BCDE6,0xEDFFFF,QiElementLook.ICE,.5F);
        for(int i=0;i<count;i++) {
            var surface=frozen.surfaces.get(world.random.nextInt(frozen.surfaces.size()));
            Vec3 velocity=surface.normal.scale(.04+world.random.nextDouble()*.10).add(0,.025,0);
            QiEnergyParticles.energy(entity.position().add(surface.offset),velocity,ice,16);surfaceFragments++;
        }
    }
    public static Appearance appearance(LivingEntity entity,float partial) {
        var frozen=FROZEN.get(entity.getUUID());
        if(frozen==null||world!=entity.level()||frozen.id!=entity.getId())return null;
        double now=world.getGameTime()+partial;
        float cracking=frozen.crackAt<0?0:(float)Math.clamp((now-frozen.crackAt)/10,0,1);
        return new Appearance(frozen.progress(now),cracking);
    }
    private static boolean visible(LivingEntity entity) {
        return ready()&&!entity.isInvisible()&&!ShaderPackCompatibility.isRenderingShadowPass()
                &&drawn<QiVfxQuality.actors()*8&&entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())<64*64;
    }
    private static RenderType material(ResourceLocation texture) {
        if(MATERIALS.size()>256)MATERIALS.clear();
        return MATERIALS.computeIfAbsent(texture,key->RenderType.create("winter_model_coating",DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,131072,false,true,RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(()->shader))
                        .setTextureState(RenderStateShard.MultiTextureStateShard.builder().add(key,false,false).add(VANILLA_ICE,false,false).build())
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        // Bias depth without moving vertices: face-normal expansion tears cuboid seams apart.
                        .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .setCullState(RenderStateShard.NO_CULL).createCompositeState(false)));
    }
    private static int light(LivingEntity entity,float partial) {
        double feet=entity.getPosition(partial).y-Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
        int lower=(int)Math.clamp(Math.round(feet*128),-32768,32767);
        int height=(int)Math.clamp(Math.round(entity.getBbHeight()*256),16,32767);
        return (lower&65535)|(height<<16);
    }
    private static int overlay(Appearance look) {
        return Math.round(look.progress*1000)|(Math.round(look.cracking*1000)<<16);
    }
    private static VertexConsumer consumer(VertexConsumer delegate,LivingEntity entity,Appearance look) {
        var frozen=FROZEN.get(entity.getUUID());frozen.sampled=0;frozen.surfaces.clear();
        Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return new VertexConsumer() {
            Vec3 pending;
            @Override public VertexConsumer addVertex(float x,float y,float z) {
                delegate.addVertex(x,y,z);vertices++;
                pending=frozen.sampled%4==0&&frozen.surfaces.size()<64?new Vec3(x,y,z).add(camera):null;return this;
            }
            @Override public VertexConsumer setColor(int r,int g,int b,int a){delegate.setColor(r,g,b,(int)(a*QiVfxQuality.opacity()));return this;}
            @Override public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
            @Override public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
            @Override public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
            @Override public VertexConsumer setNormal(float x,float y,float z) {
                delegate.setNormal(x,y,z);
                frozen.sampled++;
                if(pending!=null&&frozen.surfaces.size()<64
                        &&(look.progress>=.95F||pending.y-entity.getY()<entity.getBbHeight()*look.progress))
                    frozen.surfaces.add(new Surface(pending.subtract(entity.position()),new Vec3(x,y,z).normalize()));
                return this;
            }
        };
    }
    public static final class Layer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
        public Layer(RenderLayerParent<T,M> parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int packed,T entity,float limb,float amount,float partial,float age,float yaw,float pitch) {
            var look=appearance(entity,partial);
            if(look==null||entity instanceof GeoAnimatable||!visible(entity))return;
            drawn++;if(look.cracking>0)crackFrames++;
            var vertices=consumer(buffers.getBuffer(material(getTextureLocation(entity))),entity,look);
            getParentModel().renderToBuffer(pose,vertices,light(entity,partial),overlay(look),0xFFFFFFFF);vanillaDraws++;
        }
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"})
    public static void geo(GeoRenderEvent.Entity.Post event) {
        if(!(event.getEntity() instanceof LivingEntity entity)||!(entity instanceof GeoAnimatable animatable))return;
        var look=appearance(entity,event.getPartialTick());if(look==null||!visible(entity))return;
        drawn++;if(look.cracking>0)crackFrames++;
        GeoEntityRenderer renderer=event.getRenderer();RenderType type=material(renderer.getTextureLocation(entity));
        renderer.reRender(event.getModel(),event.getPoseStack(),event.getBufferSource(),animatable,type,
                consumer(event.getBufferSource().getBuffer(type),entity,look),event.getPartialTick(),light(entity,event.getPartialTick()),overlay(look),0xFFFFFFFF);
        geoDraws++;
    }
}
