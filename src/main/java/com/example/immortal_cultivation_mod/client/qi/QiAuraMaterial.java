package com.example.immortal_cultivation_mod.client.qi;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;

/** Color-only, depth-tested Qi. Separate fixed buffers preserve the held model's vertex stream. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class QiAuraMaterial {
    private static ShaderInstance energy,refraction;
    private static int previousTarget;
    private static final RenderStateShard.OutputStateShard TARGET=new RenderStateShard.OutputStateShard("battle_qi_target",()->{
        previousTarget=GL11.glGetInteger(36006);
        if(Minecraft.useShaderTransparency()&&!ShaderPackCompatibility.isShaderPackActive())Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    },()->GlStateManager._glBindFramebuffer(36009,previousTarget));
    public static final RenderType ENERGY=make("battle_qi",DefaultVertexFormat.NEW_ENTITY,()->energy,true,false);
    public static final RenderType HALO=make("battle_qi_halo",DefaultVertexFormat.NEW_ENTITY,()->energy,true,true);
    public static final RenderType GLOW=make("battle_qi_pressure",DefaultVertexFormat.POSITION_COLOR,GameRenderer::getPositionColorShader,false,false);
    private static RenderType make(String name,VertexFormat format,java.util.function.Supplier<ShaderInstance> shader,boolean cull,boolean additive){
        return RenderType.create(name,format,VertexFormat.Mode.QUADS,262144,false,true,RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,false,false))
                .setShaderState(new RenderStateShard.ShaderStateShard(shader)).setTransparencyState(additive?RenderStateShard.LIGHTNING_TRANSPARENCY:RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setCullState(cull?RenderStateShard.CULL:RenderStateShard.NO_CULL).setOutputState(TARGET).createCompositeState(false));
    }
    public static boolean ready(){return energy!=null;}
    public static ShaderInstance refraction(){return refraction;}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException {
        QiWeaponSilhouette.clearCache();
        IceCoatingClient.registerShader(event);
        ReincarnationEchoClient.registerShader(event);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("battle_qi"),DefaultVertexFormat.NEW_ENTITY),s->energy=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("qi_domain_refraction"),DefaultVertexFormat.POSITION_TEX_COLOR),s->refraction=s);
    }
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,name);}
    private QiAuraMaterial(){}
}
