package com.example.immortal_cultivation_mod.hair.client;

import com.example.immortal_cultivation_mod.hair.SkinPixels;
import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Skin lookup is client-only, asynchronous, and never changes the player's profile. */
public final class SkinReference {
    private static final class LookupError extends RuntimeException {
        final net.minecraft.network.chat.Component message;
        LookupError(String key,Object... args){super(key);message=net.minecraft.network.chat.Component.translatable("hair.ui.error."+key,args);}
    }
    private static LookupError error(String key,Object... args){return new LookupError(key,args);}
    public static net.minecraft.network.chat.Component errorMessage(Throwable cause){
        while((cause instanceof CompletionException||cause instanceof ExecutionException)&&cause.getCause()!=null)cause=cause.getCause();
        if(cause instanceof LookupError known)return known.message;
        return net.minecraft.network.chat.Component.translatable("hair.ui.error."+
                (cause instanceof TimeoutException||cause instanceof java.net.http.HttpTimeoutException?"timeout":"failed"));
    }
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static byte[] get(String url,int maximum) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("User-Agent","ImmortalCultivation/1.3 MinecraftSkinPicker").GET().build();
        var response=HTTP.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(var stream=response.body()){
            if(response.statusCode()!=200)throw response.statusCode()==404?error("not_found"):response.statusCode()==429?error("busy"):error("unavailable",response.statusCode());
            byte[] data=stream.readNBytes(maximum+1);if(data.length>maximum)throw error("too_large");return data;
        }
    }
    private static JsonObject json(String url) throws Exception {
        return JsonParser.parseString(new String(get(url,65536),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    public static CompletableFuture<SkinPixels> search(String name) {
        if(!name.matches("[A-Za-z0-9_]{1,16}"))return CompletableFuture.failedFuture(error("username"));
        return CompletableFuture.supplyAsync(()->{
            try{
                JsonObject account;
                try {account=json("https://api.minecraftservices.com/minecraft/profile/lookup/name/"+name);}
                catch(Exception unavailable){account=json("https://api.mojang.com/users/profiles/minecraft/"+name);}
                String uuid=account.get("id").getAsString();
                if(!uuid.matches("[a-fA-F0-9]{32}"))throw error("profile");
                var profile=json("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid);
                for(var p:profile.getAsJsonArray("properties")){
                    var property=p.getAsJsonObject();if(!property.get("name").getAsString().equals("textures"))continue;
                    var textures=JsonParser.parseString(new String(Base64.getDecoder().decode(property.get("value").getAsString()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    String address=textures.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                    URI uri=URI.create(address);
                    if(!"textures.minecraft.net".equalsIgnoreCase(uri.getHost()) || !uri.getPath().matches("/texture/[a-fA-F0-9]+"))throw error("address");
                    try(var image=NativeImage.read(new java.io.ByteArrayInputStream(get("https://textures.minecraft.net"+uri.getPath(),1048576)))){return pixels(image);}
                }
                throw error("no_skin");
            }catch(Exception ex){throw new CompletionException(ex);}
        },net.minecraft.Util.backgroundExecutor()).orTimeout(15,TimeUnit.SECONDS);
    }
    private static SkinPixels pixels(NativeImage image) {
        if(image.getWidth()!=64 || image.getHeight()!=32 && image.getHeight()!=64)throw error("dimensions");
        int[] pixels=new int[image.getWidth()*image.getHeight()];
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)pixels[y*image.getWidth()+x]=SkinPixels.fromAbgr(image.getPixelRGBA(x,y));
        return new SkinPixels(image.getWidth(),image.getHeight(),pixels);
    }
    public static net.minecraft.resources.ResourceLocation ownTexture(){
        var player=Minecraft.getInstance().player;
        var mask=com.example.immortal_cultivation_mod.item.DisguiseMaskItem.getEquippedMask(player);
        if(!mask.isEmpty()){
            var disguise=com.example.immortal_cultivation_mod.item.DisguiseMaskItem.disguiseTexture(mask);
            if(disguise!=null)return disguise;
        }
        return player.getSkin().texture();
    }
    public static SkinPixels own() {
        var mc=Minecraft.getInstance();var location=ownTexture();
        // Disguise textures are bundled assets and need not have been uploaded to GL yet.
        var resource=mc.getResourceManager().getResource(location);
        if(resource.isPresent())try(var stream=resource.get().open();var image=NativeImage.read(stream)){return pixels(image);}
        catch(java.io.IOException error){throw error("read");}
        var texture=mc.getTextureManager().getTexture(location);
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try{
            RenderSystem.bindTexture(texture.getId());
            int w=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH);
            int h=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            if(w!=64 || h!=32 && h!=64)throw error("loading");
            try(var image=new NativeImage(w,h,false)){image.downloadTexture(0,false);return pixels(image);}
        }finally{RenderSystem.bindTexture(previous);}
    }
}
