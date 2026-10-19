package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.network.PillToxicityPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Online-time clock prevents offline detox and repeat/cooldown bypass by relogging. */
@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class PillToxicity {
    private static volatile CompoundTag serverPolicy;
    public static volatile CompoundTag clientPolicy = new CompoundTag();
    public static CompoundTag state(Player p) { return p.getData(ModAttachments.PILL_TOXICITY); }
    public static CompoundTag policy(Player p) {
        if(p.level().isClientSide) return clientPolicy;
        if(serverPolicy==null) serverPolicy=PillToxicityConfig.snapshot();
        return serverPolicy;
    }
    public static void invalidatePolicy() { serverPolicy=null; }
    public static double clamp(double n) { return Double.isFinite(n)?Math.max(0,Math.min(100,n)):0; }
    public static double value(Player p) { return clamp(state(p).getDouble("Value")); }
    public static int stage(double value,CompoundTag rules) {
        int stage=0;for(int i=1;i<5;i++) if(value>=rules.getCompound("Stage"+i).getInt("Threshold"))stage=i;
        return rules.isEmpty()?0:stage;
    }
    public static int stage(Player p) { return stage(value(p),policy(p)); }
    public static Component stageName(int stage) { return Component.translatable("toxicity.stage."+stage); }
    public static double multiplier(LivingEntity entity,String key) {
        if(!(entity instanceof Player p)) return 1;
        var rules=policy(p);return rules.isEmpty()?1:rules.getCompound("Stage"+stage(p)).getDouble(key);
    }
    public static String pillId(ItemStack stack) {
        return stack.getItem() instanceof AlchemyPillItem p?p.formula:BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }
    public static double expected(Player p,ItemStack stack) { return expected(state(p),policy(p),stack,CultivationLevels.getRealmIndex(ModAttachments.getData(p).cultivationLevel())); }
    public static double expected(CompoundTag state,CompoundTag rules,ItemStack stack,int playerRealm) {
        String id=pillId(stack);if(id.equals("qingdu")||rules.isEmpty())return 0;
        var profile=rules.getCompound("Pills").getCompound(id);if(profile.isEmpty())return 0;
        var raw=PillData.raw(stack);String quality=raw.isEmpty()?"common":raw.getString("Quality");
        var qualities=rules.getCompound("Quality");double qualityFactor=qualities.contains(quality)?qualities.getDouble(quality):qualities.getDouble("common");
        // Fortune quality is an independent saved pill property, read under current quality rules.
        int fortune=Math.max(0,Math.min(3,stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("icmPillQuality")));
        double common=qualities.getDouble("common");
        qualityFactor*=Math.pow(common>0?Math.min(1,qualities.getDouble("fine")/common):0,fortune);
        int realm=raw.contains("Realm")?Math.max(0,Math.min(9,raw.getInt("Realm"))):profile.getInt("Realm");
        int gap=realm-playerRealm;
        double realmFactor=gap>0?Math.pow(rules.getDouble("Above"),gap):Math.pow(rules.getDouble("Below"),-gap);
        var repeat=state.getCompound("Repeats").getCompound(id);
        int count=state.getLong("Seconds")-repeat.getLong("Last")<rules.getInt("Window")?Math.max(0,repeat.getInt("Count")):0;
        double batchPotency=1;
        if(stack.getItem() instanceof AlchemyPillItem pill) {
            var baseline=AlchemyChemistry.formula(pill.formula).effects();var doses=PillData.effects(stack);
            double ratios=0;int measured=0;
            for(var entry:baseline.entrySet()) if(!AlchemyChemistry.activation(entry.getKey())&&entry.getValue().amount()>0&&doses.containsKey(entry.getKey())) {
                ratios+=doses.get(entry.getKey()).amount()/entry.getValue().amount();measured++;
            }
            if(measured>0)batchPotency=Math.max(.1,Math.min(25,ratios/measured));
        }
        double gain=clamp(rules.getDouble("Base")*profile.getDouble("Potency")*batchPotency*qualityFactor*realmFactor*(1+Math.min(count,rules.getInt("Cap"))*rules.getDouble("Repeat")));
        return Math.min(100-clamp(state.getDouble("Value")),gain);
    }
    /** Called at the committed consumption point only. Does not shrink stacks or apply pill effects. */
    public static void consumed(ServerPlayer p,ItemStack stack) {
        if(stack.isEmpty()||!p.isAlive()||pillId(stack).equals("qingdu"))return;
        var s=state(p);var rules=policy(p);String id=pillId(stack);
        double gain=expected(p,stack);if(!rules.getCompound("Pills").contains(id))return;
        setValue(p,value(p)+gain);
        var repeats=s.getCompound("Repeats");var record=repeats.getCompound(id);long now=s.getLong("Seconds");
        int count=now-record.getLong("Last")<rules.getInt("Window")?record.getInt("Count"):0;
        record.putInt("Count",Math.min(rules.getInt("Cap"),count+1));record.putLong("Last",now);repeats.put(id,record);s.put("Repeats",repeats);
        com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.discover(p,"pill_toxicity");
        sync(p,true);
    }
    public static int naturalQi(Player p,int amount) {
        double scaled=amount*multiplier(p,"Natural")+state(p).getDouble("NaturalFraction");
        double whole=Math.floor(scaled+1e-9);
        state(p).putDouble("NaturalFraction",Math.max(0,scaled-whole));return (int)whole;
    }
    public static void setValue(Player p,double value) { state(p).putDouble("Value",clamp(value)); }
    public static long cooldown(Player p) { return Math.max(0,state(p).getLong("DetoxUntil")-state(p).getLong("Seconds")); }
    public static boolean canDetox(ServerPlayer p,boolean explain) {
        var rules=policy(p);String reason=detoxReason(state(p),rules,CultivationLevels.getRealmIndex(ModAttachments.getData(p).cultivationLevel()));
        Object argument=reason==null?"":switch(reason){case "realm"->realmName(rules.getInt("DetoxRealm"));case "cooldown"->cooldown(p);default->"";};
        if(reason!=null&&explain)p.displayClientMessage(Component.translatable("toxicity.detox."+reason,argument),true);
        return reason==null&&p.isAlive();
    }
    public static String detoxReason(CompoundTag state,CompoundTag rules,int playerRealm) {
        if(playerRealm<rules.getInt("DetoxRealm"))return "realm";
        if(state.getLong("DetoxUntil")>state.getLong("Seconds"))return "cooldown";
        if(clamp(state.getDouble("Value"))<=0)return "clean";
        return null;
    }
    public static Component realmName(int realm) {
        String[] names={CultivationLevels.REALM_MORTAL,CultivationLevels.REALM_LIANQI,CultivationLevels.REALM_ZHUJI,CultivationLevels.REALM_JINDAN,CultivationLevels.REALM_YUANYING,CultivationLevels.REALM_HUASHEN,CultivationLevels.REALM_LIANXU,CultivationLevels.REALM_HETI,CultivationLevels.REALM_DACHENG,CultivationLevels.REALM_DUJIE};
        return CultivationLevels.realmDisplayName(names[Math.max(0,Math.min(9,realm))]);
    }
    public static boolean detox(ServerPlayer p) {
        if(!canDetox(p,true))return false;
        setValue(p,value(p)-policy(p).getDouble("Detox"));state(p).putLong("DetoxUntil",state(p).getLong("Seconds")+policy(p).getInt("DetoxSeconds"));sync(p,true);return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if(e.getEntity() instanceof ServerPlayer p && p.isAlive() && p.tickCount%20==0) onlineSecond(p);
    }
    public static void onlineSecond(ServerPlayer p) {
        var s=state(p);
        boolean active=value(p)>0 || !s.getCompound("Repeats").isEmpty() || cooldown(p)>0;
        if(!active && s.getLong("Seconds")>=s.getLong("MeditationAfter"))return;
        long previousCooldown=cooldown(p);
        long seconds=s.getLong("Seconds")+1;s.putLong("Seconds",seconds);
        if(!active)return; // Let a damage pause expire even when clean, without unnecessary packets.
        var repeats=s.getCompound("Repeats");int window=policy(p).getInt("Window");
        int previousRecords=repeats.size();
        for(String key:java.util.List.copyOf(repeats.getAllKeys()))if(seconds-repeats.getCompound(key).getLong("Last")>=window)repeats.remove(key);
        s.put("Repeats",repeats);
        int before=stage(p);
        boolean meditation=ModAttachments.getData(p).isMeditating() && seconds>=s.getLong("MeditationAfter");
        double recovery=policy(p).getDouble("Recovery")+(meditation?policy(p).getDouble("Meditation"):0);
        setValue(p,value(p)-recovery);
        if(stage(p)==4) {
            long due=s.getLong("DamageAt");if(due==0)s.putLong("DamageAt",seconds+policy(p).getInt("DamageSeconds"));
            else if(seconds>=due) {damageNonlethal(p,policy(p).getDouble("Damage"));s.putLong("DamageAt",seconds+policy(p).getInt("DamageSeconds"));}
        } else s.remove("DamageAt");
        sync(p,before!=stage(p) || previousRecords!=repeats.size() || previousCooldown>0 && cooldown(p)==0
                || value(p)==0 && repeats.isEmpty() && cooldown(p)==0);
    }
    public static float nonlethalHealth(float health,float max,double percent) { return health<=1?health:Math.max(1,(float)(health-max*Math.max(0,percent)/100.)); }
    public static void damageNonlethal(ServerPlayer p,double percent) {
        // Direct, capped health loss: armor cannot defeat toxicity and no hurt hook can turn it lethal.
        float next=nonlethalHealth(p.getHealth(),p.getMaxHealth(),percent);
        if(next<p.getHealth()){p.setHealth(next);interruptMeditation(p);}
    }
    public static void interruptMeditation(ServerPlayer p) {
        state(p).putLong("MeditationAfter",state(p).getLong("Seconds")+PillToxicityConfig.DAMAGE_PAUSE.get());
        if(ModAttachments.getData(p).isMeditating()) com.example.immortal_cultivation_mod.event.ServerEvents.stopMeditation(p);
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post e) {
        if(e.getNewDamage()>0 && e.getEntity() instanceof ServerPlayer p)interruptMeditation(p);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent e) {if(e.getEntity() instanceof ServerPlayer p){p.setData(ModAttachments.PILL_TOXICITY,new CompoundTag());sync(p,true);}}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        e.getEntity().setData(ModAttachments.PILL_TOXICITY,e.isWasDeath()?new CompoundTag():state(e.getOriginal()).copy());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {if(e.getEntity() instanceof ServerPlayer p){state(p).remove("PolicyHash");sync(p,true);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {if(e.getEntity() instanceof ServerPlayer p)sync(p,true);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {if(e.getEntity() instanceof ServerPlayer p)sync(p,true);}
    public static void sync(ServerPlayer p,boolean immediate) {
        var s=state(p);long now=p.getServer().overworld().getGameTime();
        if(!immediate && now-s.getLong("LastSync")<40)return;
        var outgoing=s.copy();outgoing.remove("LastSync");
        outgoing.putInt("PlayerRealm",CultivationLevels.getRealmIndex(ModAttachments.getData(p).cultivationLevel()));
        // No packet when nothing visible changed; policy travels once per connection/reload.
        s.putLong("LastSync",now);
        var rules=policy(p);boolean changed=!s.contains("PolicyHash")||s.getInt("PolicyHash")!=rules.hashCode();
        outgoing.putDouble("RecoveryRate",rules.getDouble("Recovery")+(ModAttachments.getData(p).isMeditating()
                && s.getLong("Seconds")>=s.getLong("MeditationAfter")?rules.getDouble("Meditation"):0));
        s.putInt("PolicyHash",rules.hashCode());outgoing.remove("PolicyHash");
        PacketDistributor.sendToPlayer(p,new PillToxicityPayload(outgoing,changed?rules:new CompoundTag()));
    }
    @EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class ConfigEvents {
        @SubscribeEvent public static void changed(net.neoforged.fml.event.config.ModConfigEvent e) {
            if(e.getConfig().getSpec()!=PillToxicityConfig.SPEC)return;
            invalidatePolicy();
            var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if(server!=null)server.execute(()->server.getPlayerList().getPlayers().forEach(p->sync(p,true)));
        }
    }
    private PillToxicity() {}
}
