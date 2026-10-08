package com.example.immortal_cultivation_mod.companion;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.network.XuanjinPayloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Short-lived screen sessions reject stale GUI clicks and never accept client state. */
@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class XuanjinInterface {
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> LAST_CAST=new WeakHashMap<>();
    public static final Set<String> TECHNIQUES=Set.of("eyes","claw","reach","voice","devour","tails","vortex","swallow","return","refine","dash");
    public static int requiredSeal(String id) {
        return switch(id){case "eyes"->1;case "claw","reach"->2;case "voice"->4;case "devour"->5;default->6;};
    }
    public static boolean castSelected(ServerPlayer p) {
        if(!allowed(p) || !XuanjinCompanion.isToken(p.getMainHandItem(),p) || SESSIONS.containsKey(p))return false;
        long now=p.level().getGameTime();
        if(now-LAST_CAST.getOrDefault(p,Long.MIN_VALUE/2)<4)return false;
        LAST_CAST.put(p,now);
        String selected=XuanjinCompanion.state(p).getString("SelectedTechnique");
        if(!TECHNIQUES.contains(selected)){XuanjinCompanion.tell(p,"gui.select_first");return false;}
        return XuanjinCompanion.execute(p,selected)==1;
    }
    private static final Set<String> CASTS=Set.of("manifest","possess","eyes","claw","reach","voice","devour","tails","vortex","swallow","return","refine","dash");
    private static final class Session {
        final UUID id=UUID.randomUUID();
        long expires,lastAction=Long.MIN_VALUE/2,lastRefresh=Long.MIN_VALUE/2;
        Component feedback=Component.empty();int messages;boolean executing;
    }
    private XuanjinInterface() {}
    public static boolean allowed(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && XuanjinCompanion.carrying(p) && !XuanjinCompanion.state(p).getBoolean("Freed");
    }
    public static UUID open(ServerPlayer p) {
        if(!allowed(p))return null;
        Session previous=SESSIONS.get(p);
        if(previous!=null) {previous.expires=p.level().getGameTime()+80;send(p,previous,!previous.executing,false);return previous.id;}
        var s=new Session();s.expires=p.level().getGameTime()+80;SESSIONS.put(p,s);send(p,s,true,false);return s.id;
    }
    public static boolean feedback(ServerPlayer p,Component message) {
        var s=SESSIONS.get(p);if(s==null)return false;
        if(s.messages++>=3){s.feedback=message;s.messages=1;}
        else s.feedback=s.feedback.getString().isEmpty()?message:Component.empty().append(s.feedback).append("\n").append(message);
        return true;
    }
    public static CompoundTag snapshot(ServerPlayer p) {
        var source=XuanjinCompanion.state(p);var out=new CompoundTag();long now=p.level().getGameTime();
        for(String key:new String[]{"Seal","Trust","Freedom","Energy"})out.putInt(key,source.getInt(key));
        out.putInt("GuiTab",source.getInt("GuiTab"));out.putInt("GuiPage",source.getInt("GuiPage"));
        out.putString("SelectedTechnique",source.getString("SelectedTechnique"));
        out.putFloat("Health",source.getFloat("Health"));
        for(String key:new String[]{"ManifestUntil","PossessUntil","RecoverUntil","FreeConfirmUntil","VortexUntil"})out.putLong(key,source.getLong(key));
        for(String id:XuanjinCompanion.COMMANDS)out.putLong("Cooldown."+id,source.getLong("Cooldown."+id));
        out.putBoolean("VortexReady",source.getBoolean("VortexReady"));out.putFloat("Absorbed",source.getFloat("Absorbed"));
        out.putBoolean("Active",XuanjinCompanion.actor(p)!=null);out.putLong("Now",now);
        out.putInt("Realm",CultivationLevels.getStageIndex(ModAttachments.getData(p).cultivationLevel()));
        out.putInt("Qi",ModAttachments.getData(p).qi());return out;
    }
    private static void send(ServerPlayer p,Session s,boolean open,boolean close) {
        if(!(p instanceof net.neoforged.neoforge.common.util.FakePlayer))
            PacketDistributor.sendToPlayer(p,new XuanjinPayloads.View(s.id,snapshot(p),s.feedback,open,close));
    }
    public static boolean action(ServerPlayer p,UUID id,String action) {
        Session s=SESSIONS.get(p);long now=p.level().getGameTime();
        if(s==null || !s.id.equals(id))return false;
        if(action.equals("close")){XuanjinCompanion.state(p).remove("FreeConfirmUntil");SESSIONS.remove(p);return true;}
        if(s.expires<now || !allowed(p)){XuanjinCompanion.state(p).remove("FreeConfirmUntil");send(p,s,false,true);SESSIONS.remove(p);return false;}
        if(action.matches("view_[012]_[01]")) {
            int tab=action.charAt(5)-'0',page=action.charAt(7)-'0';
            var state=XuanjinCompanion.state(p);state.putInt("GuiTab",tab);state.putInt("GuiPage",tab==1?page:0);
            s.expires=now+80;return true;
        }
        if(action.startsWith("select_")) {
            String technique=action.substring(7);var state=XuanjinCompanion.state(p);
            if(!TECHNIQUES.contains(technique) || state.getInt("Seal")<requiredSeal(technique))return false;
            state.putString("SelectedTechnique",technique);s.expires=now+80;
            s.feedback=Component.translatable("xuanjin.gui.selected_hint",Component.translatable("xuanjin.action."+technique));
            send(p,s,false,false);return true;
        }
        if(action.equals("refresh")) {
            if(now-s.lastRefresh<10)return false;
            s.lastRefresh=now;s.expires=now+80;send(p,s,false,false);return true;
        }
        if(!XuanjinCompanion.COMMANDS.contains(action) && !action.equals("cancel_free"))return false;
        if(now-s.lastAction<4)return false;
        s.lastAction=now;s.expires=now+80;s.feedback=Component.empty();s.messages=0;
        if(action.equals("cancel_free")){XuanjinCompanion.state(p).remove("FreeConfirmUntil");send(p,s,false,false);return true;}
        int result;
        s.executing=true;
        try {result=XuanjinCompanion.execute(p,action);} finally {s.executing=false;}
        if(result==1 && s.feedback.getString().isEmpty())s.feedback=Component.translatable("xuanjin.gui.done",Component.translatable("xuanjin.action."+action));
        boolean close=!allowed(p) || result==1 && CASTS.contains(action);
        send(p,s,false,close);if(close)SESSIONS.remove(p);return result==1;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        Session s=SESSIONS.get(p);if(s==null)return;
        if(!allowed(p) || s.expires<p.level().getGameTime()) {
            send(p,s,false,true);SESSIONS.remove(p);XuanjinCompanion.state(p).remove("FreeConfirmUntil");
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){SESSIONS.remove(p);LAST_CAST.remove(p);XuanjinCompanion.state(p).remove("FreeConfirmUntil");}
    }
}
