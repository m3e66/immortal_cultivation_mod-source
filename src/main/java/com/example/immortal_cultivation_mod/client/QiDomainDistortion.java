package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.client.qi.QiAuraMaterial;
import com.example.immortal_cultivation_mod.client.qi.QiVfxQuality;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/** Sustained aura and domain-clash refraction, sampled from the actual scene. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class QiDomainDistortion {
    private record Source(Vec3 center, float strength, float size, Vec3 axis,float aspect,int flow) { }
    private static final Map<BlockPos, Source> SOURCES = new LinkedHashMap<>();
    private static ClientLevel world;
    private static TextureTarget snapshot;
    private static long lastActive;
    public static long renderedFrames;
    public static float peakWarp;
    public static float frameWarp;
    public static long windRefractionFrames,violentCoreFrames;

    private QiDomainDistortion() { }

    static void setSeams(java.util.List<Vec3> seams) {
        for(int i=0;i<Math.min(4,seams.size());i++)observe(seams.get(i),1F,10);
    }
    static void close(){SOURCES.clear();}
    static void observe(Vec3 center, float strength, float size) {
        observe(center,strength,size,new Vec3(0,1,0));
    }
    public static void observe(Vec3 center, float strength, float size,Vec3 axis) {
        observe(center,strength,size,axis,1.3F);
    }
    public static void observeClash(Vec3 center,float strength,float size,Vec3 axis) {
        observe(center,strength,size,axis,2F,1,3);
    }
    static void observeViolent(Vec3 center,float strength,float size) {
        observe(center,strength,size,new Vec3(0,1,0),4F,1,1);
    }
    public static void windSeam(Vec3 start,Vec3 end,float strength) {
        Vec3 axis=end.subtract(start);if(axis.lengthSqr()<.01)return;
        observe(start.lerp(end,.5),strength,(float)axis.length()+.3F,axis.normalize(),1.3F,.055F,2);
    }
    private static void observe(Vec3 center,float strength,float size,Vec3 axis,float maximum) {
        observe(center,strength,size,axis,maximum,1,0);
    }
    private static void observe(Vec3 center,float strength,float size,Vec3 axis,float maximum,float aspect,int flow) {
        strength*=QiVfxQuality.opacity();
        if (!Float.isFinite(strength)||strength<=0||!Float.isFinite(size)||size<=0
                ||ShaderPackCompatibility.isShaderPackActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) { SOURCES.clear(); world = mc.level; }
        if (world == null || mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(center) > (flow==3?96*96:40*40))
            return;
        BlockPos key=BlockPos.containing(center);
        if (SOURCES.size()<48||SOURCES.containsKey(key)) {
            Source old=SOURCES.get(key);
            if(old==null||strength>=old.strength())SOURCES.put(key,new Source(center,Math.min(strength,maximum),size,axis,aspect,flow));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SOURCES.clear(); world = null; release();
    }

    private static void release() {
        if (snapshot == null) return;
        int read = GL11.glGetInteger(36010), draw = GL11.glGetInteger(36006);
        snapshot.destroyBuffers(); snapshot = null;
        GlStateManager._glBindFramebuffer(36008, read);
        GlStateManager._glBindFramebuffer(36009, draw);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        frameWarp=0;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) { SOURCES.clear(); world = mc.level; release(); }
        if (world == null || ShaderPackCompatibility.isShaderPackActive()) {
            SOURCES.clear(); release(); return;
        }
        if (SOURCES.isEmpty()) {
            if (world.getGameTime() - lastActive > 60) release();
            return;
        }
        var hazeShader = QiAuraMaterial.refraction();
        if (hazeShader == null) { SOURCES.clear(); return; }
        lastActive = world.getGameTime();
        var main = mc.getMainRenderTarget();
        int width = Math.min(QiVfxQuality.level()==0?640:960, main.viewWidth);
        int height = Math.max(1, main.viewHeight * width / Math.max(1, main.viewWidth));
        if (width <= 0) { SOURCES.clear(); return; }
        if (snapshot == null) snapshot = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        else if (snapshot.width != width || snapshot.height != height)
            snapshot.resize(width, height, Minecraft.ON_OSX);
        snapshot.setFilterMode(9729);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        var previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        // AFTER_LEVEL fires after vanilla restores its model-view stack. Match the view used by
        // screen-space UV projection so the card and sampled scene align for every camera angle.
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.mul(event.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();
            GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
            GlStateManager._glBindFramebuffer(36009, snapshot.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.viewWidth, main.viewHeight,
                    0, 0, width, height, 16384, 9729);
            main.bindWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.setShader(() -> hazeShader);
            RenderSystem.setShaderTexture(0, snapshot.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            int patches = 0;
            float time = world.getGameTime() % 24000 + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 camera = event.getCamera().getPosition();
            Vec3 cameraRight=new Vec3(new Vector3f(1,0,0).rotate(event.getCamera().rotation()));
            Vec3 cameraUp=new Vec3(new Vector3f(0,1,0).rotate(event.getCamera().rotation()));
            for (Source source : SOURCES.values()) {
                if (patches >= 12 || !event.getFrustum().isVisible(
                        new net.minecraft.world.phys.AABB(source.center(), source.center()).inflate(source.size()))) continue;
                Vec3 relative = source.center().subtract(camera);
                Vector4f clip = projection.transform(new Vector4f((float) relative.x, (float) relative.y,
                        (float) relative.z, 1));
                if (clip.w <= .05F) continue;
                patches++;
                if(source.flow()==1)violentCoreFrames++;
                if(source.flow()==2)windRefractionFrames++;
                double projectedX=source.axis().dot(cameraRight),projectedY=source.axis().dot(cameraUp);
                double projectedLength=Math.hypot(projectedX,projectedY);
                float axisX=projectedLength>1E-5?(float)(projectedX/projectedLength):0;
                float axisY=projectedLength>1E-5?(float)(projectedY/projectedLength):1;
                // Resolve the stronger pressure waves smoothly instead of stretching a coarse card.
                int rows=QiVfxQuality.level()==0?14:28,columns=QiVfxQuality.level()==0?12:24;
                for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
                    for (int corner = 0; corner < 4; corner++) {
                        float u = (column + (corner == 1 || corner == 2 ? 1 : 0)) / (float)columns;
                        float v = (row + (corner >= 2 ? 1 : 0)) / (float)rows;
                        float across=(u-.5F)*.95F*source.size()*source.aspect();
                        float along=(v-.5F)*source.size()*(source.flow()==2?(float)Math.max(.1,projectedLength):1);
                        Vector3f offset = new Vector3f(across*axisY+along*axisX,-across*axisX+along*axisY,0)
                                .rotate(event.getCamera().rotation());
                        Vector3f position = new Vector3f((float) relative.x, (float) relative.y,
                                (float) relative.z).add(offset);
                        Vector4f screen = projection.transform(new Vector4f(position, 1));
                        // Fade smoothly to zero at all four boundaries; no rectangular haze card.
                        float fade = Mth.sin(u * Mth.PI) * Mth.sin(v * Mth.PI);
                        // Keep the flame's core intact; space bends most in the air around its silhouette.
                        float coreX=(u-.5F)/.22F,coreY=(v-.5F)/.38F;
                        float rim=Mth.clamp((Mth.sqrt(coreX*coreX+coreY*coreY)-.8F)/.3F,0,1);
                        rim=rim*rim*(3-2*rim);
                        if(source.flow()!=0)rim=1;
                        float distanceFade = Mth.clamp(((source.flow()==3?96F:40F) - (float) relative.length()) / (source.flow()==3?72:26), 0, 1);
                        float warp = .032F * source.strength() * fade * rim * distanceFade;
                        peakWarp=Math.max(peakWarp,warp);
                        frameWarp=Math.max(frameWarp,warp);
                        float screenU = screen.w <= 0 ? .5F : screen.x / screen.w * .5F + .5F;
                        float screenV = screen.w <= 0 ? .5F : screen.y / screen.w * .5F + .5F;
                        float sampleU = Mth.clamp(screenU + Mth.sin(Mth.sqrt((u-.5F)*(u-.5F)+(v-.5F)*(v-.5F))*42-time*.27F) * warp, 0, 1);
                        float sampleV = Mth.clamp(screenV + Mth.cos(u*27-v*18+time*.19F) * warp * .65F, 0, 1);
                        if(source.flow()==1) {
                            float centerU=clip.x/clip.w*.5F+.5F,centerV=clip.y/clip.w*.5F+.5F;
                            sampleU=Mth.clamp(sampleU+(centerU-screenU)*warp*2.5F,0,1);
                            sampleV=Mth.clamp(sampleV+(centerV-screenV)*warp*2.5F,0,1);
                        }else if(source.flow()==2) {
                            sampleU=Mth.clamp(screenU-axisY*warp*.25F,0,1);
                            sampleV=Mth.clamp(screenV+axisX*warp*.25F,0,1);
                        }
                        buffer.addVertex(position.x, position.y, position.z).setUv(sampleU, sampleV)
                                .setColor(1F, 1F, 1F, Math.min(1,source.strength()) * fade * rim * distanceFade);
                    }
                }
            }
            if (patches > 0) {BufferUploader.drawWithShader(buffer.buildOrThrow());renderedFrames++;}
            else buffer.build();
        } finally {
            SOURCES.clear();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.depthMask(true);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.defaultBlendFunc();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            main.bindWrite(true);
        }
    }
}
