package com.example.immortal_cultivation_mod.client.curse;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Rune alpha is masked by the actual skin, including transparent outer-skin pixels. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class RuneInkMaterial {
    private static ShaderInstance shader;
    private static final Map<ResourceLocation,RenderType> TYPES=new HashMap<>();
    private RuneInkMaterial() { }
    public static boolean ready(){return shader!=null;}
    public static RenderType of(ResourceLocation skin) {
        if(TYPES.size()>256)TYPES.clear();
        return TYPES.computeIfAbsent(skin,key->RenderType.create("moving_curse_ink",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,131072,false,true,RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(()->shader))
                .setTextureState(RenderStateShard.MultiTextureStateShard.builder().add(CurseClient.RUNE_ATLAS,false,false).add(key,false,false).build())
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false)));
    }
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException {
        TYPES.clear();event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,"moving_curse_ink"),DefaultVertexFormat.NEW_ENTITY),s->shader=s);
    }
}
