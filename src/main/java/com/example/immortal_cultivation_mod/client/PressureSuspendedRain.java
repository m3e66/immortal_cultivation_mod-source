package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.PressureRainMotion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Visible stationary rain replaces the moving weather curtain inside a pressure field. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT)
public final class PressureSuspendedRain {
    private static final List<Drop> DROPS = new ArrayList<>();
    private static ClientLevel world;
    private static final class Drop {
        Vec3 position, previous;
        double velocity;
        int age, released;
        boolean held;
        Drop(Vec3 p) { position=previous=p; held=true; }
    }
    private PressureSuspendedRain() {}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        DROPS.clear(); world=null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(world!=mc.level){DROPS.clear();world=mc.level;}
        if(world==null||mc.player==null||mc.isPaused())return;
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        boolean raining=world.getRainLevel(1)>.05F;
        for(Drop drop:DROPS){
            drop.previous=drop.position;
            drop.held=raining&&RealmPressureClient.strengthAt(drop.position)>.001F;
            var step=PressureRainMotion.advance(drop.position,drop.velocity,drop.held);
            drop.position=step.position();drop.velocity=step.velocity();drop.age++;
            if(!drop.held)drop.released++;else drop.released=0;
        }
        DROPS.removeIf(d -> d.age>220 || d.released>24 || d.position.distanceToSqr(camera)>24*24
                || !world.hasChunkAt(BlockPos.containing(d.position))
                || !world.getBlockState(BlockPos.containing(d.position)).getCollisionShape(world,BlockPos.containing(d.position)).isEmpty());
        if(!raining)return;
        int limit=switch(mc.options.particles().get()){case MINIMAL->64;case DECREASED->144;default->320;};
        for(int i=0;i<20&&DROPS.size()<limit;i++){
            Vec3 p=camera.add((world.random.nextDouble()-.5)*26,world.random.nextDouble()*12-4,(world.random.nextDouble()-.5)*26);
            BlockPos pos=BlockPos.containing(p);
            if(!world.hasChunkAt(pos)||RealmPressureClient.strengthAt(p)<=.001F||!world.canSeeSky(pos)
                    || !world.isEmptyBlock(pos)||world.getBiome(pos).value().getPrecipitationAt(pos)!=Biome.Precipitation.RAIN)continue;
            DROPS.add(new Drop(p));
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||world!=Minecraft.getInstance().level
                ||DROPS.isEmpty()||ShaderPackCompatibility.isRenderingShadowPass())return;
        var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();
        var out=buffers.getBuffer(FilamentVfx.TYPE);var matrix=event.getPoseStack().last().pose();
        Vec3 camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(Drop d:DROPS){
            Vec3 p=d.previous.lerp(d.position,partial);
            float alpha=(float)(Math.min(1,d.age/8.0)*Math.min(1,(24-d.released)/8.0)*.38);
            double length=d.held?.045:.13+Math.abs(d.velocity)*.18;
            FilamentVfx.ribbon(out,matrix,p,p.add(0,-length,0),camera,d.held?.022:.011,.5F,.6F,.68F,alpha,alpha*.7F);
        }
        buffers.endBatch(FilamentVfx.TYPE);
    }
}
