package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Past positions replay the actual animated model as faint opposing monochrome silhouettes. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class ReincarnationEchoClient {
    private static final class Echo {
        final int id;final UUID uuid;final Deque<Vec3> positions=new ArrayDeque<>();long until;
        Echo(LivingEntity entity,long now){id=entity.getId();uuid=entity.getUUID();until=now+200;positions.add(entity.position());}
    }
    private static final Map<UUID,Echo> ECHOES=new HashMap<>();
    private static final Map<ResourceLocation,RenderType> MATERIALS=new HashMap<>();
    private static ClientLevel world;
    private static ShaderInstance shader;
    private static int draws;
    public static long vanillaFrames,geoFrames,collapseEvents;
    private ReincarnationEchoClient() { }
    public static void clear(){ECHOES.clear();world=null;}
    public static void beginFrame(){draws=0;}
    public static int active(){return ECHOES.size();}
    public static void registerShader(RegisterShadersEvent event)throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"reincarnation_echo"),
                DefaultVertexFormat.NEW_ENTITY),value->shader=value);
    }
    public static void accept(LivingEntity victim) {
        var level=Minecraft.getInstance().level;if(world!=level){clear();world=level;}
        if(world==null)return;var echo=ECHOES.get(victim.getUUID());
        if(echo==null){if(ECHOES.size()>=64)return;echo=new Echo(victim,world.getGameTime());ECHOES.put(victim.getUUID(),echo);}
        echo.until=world.getGameTime()+200;
    }
    public static void tick() {
        var level=Minecraft.getInstance().level;if(world!=level){clear();world=level;}
        if(world==null)return;long now=world.getGameTime();var iterator=ECHOES.values().iterator();
        while(iterator.hasNext()) {
            var echo=iterator.next();var entity=world.getEntity(echo.id);
            if(!(entity instanceof LivingEntity living)||!entity.getUUID().equals(echo.uuid)||!entity.isAlive()){iterator.remove();continue;}
            if(now%3==0){echo.positions.addFirst(living.position());while(echo.positions.size()>5)echo.positions.removeLast();}
            if(now>=echo.until) {
                collapseEvents++;
                for(int j=0;j<10;j++) {
                    double angle=j*Math.PI/5;Vec3 point=living.getBoundingBox().getCenter().add(Math.cos(angle)*.45,Math.sin(j)*.25,Math.sin(angle)*.45);
                    QiEnergyParticles.energy(point,living.getBoundingBox().getCenter().subtract(point).scale(.12),
                            new QiElementLook(j%2==0?0xF4F4EE:0x111119,0xE5E5E0,QiElementLook.WIND,.5F),8);
                }
                iterator.remove();
            }
        }
    }
    private static Echo echo(LivingEntity entity) {
        if(shader==null||world!=entity.level()||ShaderPackCompatibility.isRenderingShadowPass()||entity.isInvisible()
                ||draws>=QiVfxQuality.actors()*3||entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>64*64)return null;
        return ECHOES.get(entity.getUUID());
    }
    private static RenderType material(ResourceLocation texture) {
        if(MATERIALS.size()>128)MATERIALS.clear();
        return MATERIALS.computeIfAbsent(texture,key->RenderType.create("reincarnation_model_echo",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,131072,false,true,
                RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader))
                        .setTextureState(new RenderStateShard.TextureStateShard(key,false,false)).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false)));
    }
    private static void lag(PoseStack pose,LivingEntity entity,Echo echo,int image,float partial) {
        var positions=new ArrayList<>(echo.positions);Vec3 old=positions.get(Math.min(positions.size()-1,image*2+1));
        Vec3 shift=old.subtract(entity.getPosition(partial));
        if(shift.lengthSqr()<.002) {
            Vec3 forward=entity.getLookAngle(),side=new Vec3(-forward.z,0,forward.x).normalize();
            double phase=(world.getGameTime()+partial)%240*Math.PI/120;
            shift=forward.scale(-.16*(image+1)).add(side.scale((image==0?1:-1)*(.12+.025*Math.sin(phase))));
        }
        if(shift.lengthSqr()>16)shift=Vec3.ZERO;
        Vector3f local=new Matrix4f(pose.last().pose()).invert().transformDirection(new Vector3f((float)shift.x,(float)shift.y,(float)shift.z));
        pose.translate(local.x,local.y,local.z);
    }
    private static int color(Echo echo,int image,float partial) {
        float fade=(float)Math.clamp((echo.until-world.getGameTime()-partial)/12,0,1);
        int alpha=(int)((image==0?52:34)*fade*QiVfxQuality.opacity());return (alpha<<24)|(image==0?0x191323:0xF0F4E9);
    }
    public static final class Layer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
        public Layer(RenderLayerParent<T,M> parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limb,float amount,float partial,float age,float yaw,float pitch) {
            var echo=echo(entity);if(echo==null||entity instanceof GeoAnimatable)return;draws++;
            var out=buffers.getBuffer(material(getTextureLocation(entity)));
            for(int i=0;i<2;i++){pose.pushPose();lag(pose,entity,echo,i,partial);getParentModel().renderToBuffer(pose,out,15728880,0,color(echo,i,partial));pose.popPose();}
            vanillaFrames++;
        }
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"})
    public static void geo(GeoRenderEvent.Entity.Post event) {
        if(!(event.getEntity() instanceof LivingEntity entity)||!(entity instanceof GeoAnimatable animatable))return;
        var echo=echo(entity);if(echo==null)return;draws++;GeoEntityRenderer renderer=event.getRenderer();
        RenderType type=material(renderer.getTextureLocation(entity));
        for(int i=0;i<2;i++){var pose=event.getPoseStack();pose.pushPose();lag(pose,entity,echo,i,event.getPartialTick());
            renderer.reRender(event.getModel(),pose,event.getBufferSource(),animatable,type,event.getBufferSource().getBuffer(type),event.getPartialTick(),15728880,0,color(echo,i,event.getPartialTick()));pose.popPose();}
        geoFrames++;
    }
}
