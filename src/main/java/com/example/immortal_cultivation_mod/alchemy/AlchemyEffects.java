package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.*;

@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class AlchemyEffects {
    public static final String ACTIVE="CultivationPillDoses";
    private static final String REGEN_ARMED="CultivationPillEmergencyRegen";
    private static final String REGEN_DOSE="CultivationPillEmergencyDose";
    private static final ResourceLocation SPEED_ID=ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","pill_speed");
    public static boolean consume(LivingEntity player,ItemStack pill) {
        if(player.level().isClientSide || !(pill.getItem() instanceof AlchemyPillItem)||pill.isEmpty()||!player.isAlive())return false;
        long now=player.level().getGameTime();
        if(player instanceof ServerPlayer server && PillToxicity.pillId(pill).equals("qingdu") && !PillToxicity.canDetox(server,true))return false;
        if(player.getPersistentData().getLong("NextCultivationPill")>now)return false;
        var effects=PillData.effects(pill);
        if(effects.isEmpty())return false;
        if(player instanceof ServerPlayer server) {
            if(PillToxicity.pillId(pill).equals("qingdu")) {
                if(!PillToxicity.detox(server))return false;
            } else PillToxicity.consumed(server,pill);
        }
        ListTag active=player.getPersistentData().getList(ACTIVE,Tag.TAG_COMPOUND);
        effects.forEach((effect,dose)->{
            if(activation(effect) && player.getRandom().nextDouble()*100>=Math.min(100,dose.amount()))return;
            if(effect==Effect.CLEAR_FIRE){player.clearFire();return;}
            if(effect==Effect.DETOX){player.removeEffect(MobEffects.POISON);player.removeEffect(MobEffects.WITHER);return;}
            if(effect==Effect.CLEAR_MIND){player.removeEffect(MobEffects.CONFUSION);return;}
            if(effect==Effect.TOXICITY){player.addEffect(new MobEffectInstance(MobEffects.POISON,Math.max(20,dose.ticks()),0));return;}
            if(effect==Effect.NIGHT_VISION){player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,dose.ticks(),0));return;}
            if(effect==Effect.WATER_BREATHING){player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,dose.ticks(),0));return;}
            if(effect==Effect.IRON_BONES){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,dose.ticks(),2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,dose.ticks(),1));return;
            }
            if(effect==Effect.MIND_SHIELD){player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);player.removeEffect(MobEffects.WEAKNESS);}
            if(effect==Effect.WITHER_SHIELD)player.removeEffect(MobEffects.WITHER);
            if(effect==Effect.EMERGENCY_REGEN){
                player.getPersistentData().putBoolean(REGEN_ARMED,true);
                CompoundTag emergency=new CompoundTag(); emergency.putDouble("RecoveryTotal",recoveryAmount(player,Effect.HEALTH,dose.amount()));
                emergency.putInt("Ticks",dose.ticks()>0?dose.ticks():900); player.getPersistentData().put(REGEN_DOSE,emergency);return;
            }
            if(dose.ticks()==0){restore(player,effect,recoveryAmount(player,effect,dose.amount()));return;}
            for(int i=active.size()-1;i>=0;i--)if(active.getCompound(i).getString("Effect").equals(effect.name()))active.remove(i);
            CompoundTag tag=new CompoundTag();tag.putString("Effect",effect.name());tag.putDouble("Amount",dose.amount());
            tag.putInt("Ticks",dose.ticks());tag.putInt("Elapsed",0);active.add(tag);
            if(recovery(effect))tag.putDouble("RecoveryTotal",recoveryAmount(player,effect,dose.amount()));
        });
        player.getPersistentData().put(ACTIVE,active);
        if (player instanceof ServerPlayer server) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "pill_used"); }
        player.getPersistentData().putLong("NextCultivationPill",now+20);
        syncSpeed(player);
        if(player instanceof ServerPlayer server)ServerEvents.syncPlayerData(server);
        return true;
    }
    public static double bonus(LivingEntity player,Effect effect) {
        if(!player.isAlive())return 0;
        double value=0;
        ListTag active=player.getPersistentData().getList(ACTIVE,Tag.TAG_COMPOUND);
        for(int i=0;i<active.size();i++){
            CompoundTag tag=active.getCompound(i);
            if(tag.getString("Effect").equals(effect.name())&&tag.getInt("Elapsed")<tag.getInt("Ticks"))
                value=Math.max(value,tag.getDouble("Amount")/100.);
        }
        if(!activation(effect))value*=PillToxicity.multiplier(player,"Benefit");
        return Double.isFinite(value)?Math.max(0,Math.min(effect==Effect.DEFENSE||effect==Effect.FIRE_RESIST||effect==Effect.COLD_RESIST?.8:3,value)):0;
    }
    public static int cultivationGain(Player player,int base) {
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.round(base*(1+bonus(player,Effect.CULTIVATION_GAIN))*com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationMultiplier(player))));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player)tickPlayer(player);
    }
    public static void tickPlayer(ServerPlayer player) {
        tickLiving(player);
    }
    /** NPCs tick the same doses, durations and random batch values as players. */
    public static void tickLiving(LivingEntity player) {
        if(player.level().isClientSide)return;
        ListTag active=player.getPersistentData().getList(ACTIVE,Tag.TAG_COMPOUND);
        if(!player.isAlive()){
            player.getPersistentData().remove(ACTIVE);player.getPersistentData().remove(REGEN_ARMED);player.getPersistentData().remove(REGEN_DOSE);
            syncSpeed(player);return;
        }
        if(player.getPersistentData().getBoolean(REGEN_ARMED)&&player.getHealth()<player.getMaxHealth()*.2F){
            player.getPersistentData().remove(REGEN_ARMED);
            CompoundTag emergency=player.getPersistentData().getCompound(REGEN_DOSE);
            CompoundTag healing=new CompoundTag();healing.putString("Effect",Effect.HEALTH.name());healing.putDouble("Amount",20);
            healing.putDouble("RecoveryTotal",emergency.contains("RecoveryTotal")?emergency.getDouble("RecoveryTotal"):player.getMaxHealth()*.2);
            healing.putInt("Ticks",emergency.contains("Ticks")?emergency.getInt("Ticks"):900);healing.putInt("Elapsed",0);
            active.add(healing);player.getPersistentData().remove(REGEN_DOSE);
        }
        boolean changed=false;
        for(int i=active.size()-1;i>=0;i--){
            CompoundTag tag=active.getCompound(i);
            int ticks=tag.getInt("Ticks"),elapsed=tag.getInt("Elapsed");
            double total=tag.contains("RecoveryTotal")?tag.getDouble("RecoveryTotal"):tag.getDouble("Amount");
            Effect effect;
            try{effect=Effect.valueOf(tag.getString("Effect"));}
            catch(IllegalArgumentException ex){active.remove(i);continue;}
            if(ticks<=0||ticks>72000||elapsed<0||elapsed>=ticks||!Double.isFinite(total)||total<0||total>1E22){active.remove(i);continue;}
            int next=elapsed+1;
            if(effect==Effect.HEALTH) {
                restore(player,effect,total/ticks);changed=true;
            } else if(effect==Effect.QI||effect==Effect.SENSE||effect==Effect.CULTIVATION) {
                double delta=Math.floor(total*next/ticks)-Math.floor(total*elapsed/ticks);
                if(delta>0){restore(player,effect,delta);changed=true;}
            }
            if (player instanceof ServerPlayer server && effect != Effect.NEXT_QI_SPELL && effect != Effect.NEXT_MELEE) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "pill_effect"); }
            tag.putInt("Elapsed",next);
            if(next>=ticks)active.remove(i);
        }
        player.getPersistentData().put(ACTIVE,active);
        syncSpeed(player);
        if(changed&&player.tickCount%20==0&&player instanceof ServerPlayer server)ServerEvents.syncPlayerData(server);
    }
    private static void restore(LivingEntity entity,Effect effect,double amount) {
        amount*=PillToxicity.multiplier(entity,effect==Effect.HEALTH||effect==Effect.QI?"Restore":"Benefit");
        if(entity instanceof ServerPlayer && effect!=Effect.HEALTH) {
            String key="PillRestoreFraction_"+effect.name();
            amount+=entity.getPersistentData().getDouble(key);
            double whole=Math.floor(amount+1e-9);
            entity.getPersistentData().putDouble(key,Math.max(0,amount-whole));
            amount=whole;
        }
        if(effect==Effect.HEALTH){entity.heal(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.pillHealing(entity, (float)amount));return;}
        if(entity instanceof PathfinderMob npc && entity instanceof CultivationNpc cultivation) {
            String archetype=cultivation.npcArchetype();
            switch(effect) {
                case QI->NpcProfile.restoreQi(npc,archetype,(int)Math.min(amount,Math.max(0,NpcProfile.maxQi(npc,archetype)-NpcProfile.qi(npc,archetype))));
                case SENSE->NpcProfile.adjustThoughts(npc,archetype,(int)Math.min(amount,Math.max(0,NpcProfile.maxThoughts(npc,archetype)-NpcProfile.thoughts(npc,archetype))));
                case CULTIVATION->NpcProfile.restoreCultivationProgress(npc,archetype,(long)Math.min(amount,Math.max(0,CultivationLevels.getTotalQiNeeded(NpcProfile.realm(npc,archetype))-NpcProfile.cultivationProgress(npc,archetype))));
                default->{}
            }
            return;
        }
        if(!(entity instanceof ServerPlayer player))return;
        var data=ModAttachments.getData(player);
        switch(effect){
            case HEALTH->player.heal(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.pillHealing(player, (float)amount));
            case QI->ModAttachments.setData(player,data.withQi(data.qi()+(int)Math.min(amount,Math.max(0,
                data.maxQi()-data.qi()))));
            case SENSE->ModAttachments.setData(player,data.withThoughts(data.thoughts()+(int)Math.min(amount,Math.max(0,data.maxThoughts()-data.thoughts()))));
            case CULTIVATION->ModAttachments.setData(player,data.withCultivationProgress(data.cultivationProgress()+(long)Math.min(amount,Math.max(0,
                CultivationLevels.getTotalQiNeeded(data.cultivationLevel())-data.cultivationProgress()))));
            default->{}
        }
    }
    public static double recoveryAmount(LivingEntity entity,Effect effect,double percentage) {
        if(!recovery(effect))return percentage;
        double maximum=0;
        if(effect==Effect.HEALTH||effect==Effect.EMERGENCY_REGEN)maximum=entity.getMaxHealth();
        else if(entity instanceof PathfinderMob npc && entity instanceof CultivationNpc cultivation){
            String archetype=cultivation.npcArchetype();
            maximum=switch(effect){
                case QI->NpcProfile.maxQi(npc,archetype);
                case SENSE->NpcProfile.maxThoughts(npc,archetype);
                case CULTIVATION->CultivationLevels.getTotalQiNeeded(NpcProfile.realm(npc,archetype));
                default->0;
            };
        } else if(entity instanceof ServerPlayer player){
            var data=ModAttachments.getData(player);
            maximum=switch(effect){
                case QI->(double)data.maxQi();
                case SENSE->data.maxThoughts();
                case CULTIVATION->CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
                default->0;
            };
        }
        double recovery = Math.max(0,maximum)*percentage/100.;
        if(effect==Effect.CULTIVATION && entity instanceof Player player) recovery *= com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationMultiplier(player);
        return recovery;
    }
    private static void syncSpeed(LivingEntity player){
        var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attribute==null)return;
        double value=bonus(player,Effect.SPEED);
        var current=attribute.getModifier(SPEED_ID);
        if(current!=null&&current.amount()==value)return;
        attribute.removeModifier(SPEED_ID);
        if(value>0)attribute.addTransientModifier(new AttributeModifier(SPEED_ID,value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event){
        if(event.getEntity().level().isClientSide)return;
        double multiplier=1;
        if(event.getSource().getEntity() instanceof LivingEntity attacker){
            multiplier+=bonus(attacker,Effect.ATTACK);
            if(event.getSource().is(DamageTypeTags.IS_FIRE))multiplier+=bonus(attacker,Effect.FIRE_DAMAGE);
        }
        {
            LivingEntity player=event.getEntity();
            multiplier*=1-bonus(player,Effect.DEFENSE);
            if(event.getSource().is(DamageTypeTags.IS_FIRE))multiplier*=1-bonus(player,Effect.FIRE_RESIST);
            if(event.getSource().is(DamageTypeTags.IS_FREEZING))multiplier*=1-bonus(player,Effect.COLD_RESIST);
        }
        event.setAmount((float)(event.getAmount()*multiplier));
    }
    public static boolean hasActive(LivingEntity player,Effect effect){
        ListTag active=player.getPersistentData().getList(ACTIVE,Tag.TAG_COMPOUND);
        for(int i=0;i<active.size();i++){
            CompoundTag tag=active.getCompound(i);
            if(tag.getString("Effect").equals(effect.name())&&tag.getInt("Ticks")>tag.getInt("Elapsed"))return true;
        }
        return false;
    }
    private static void removeActive(Player player,Effect effect){
        ListTag active=player.getPersistentData().getList(ACTIVE,Tag.TAG_COMPOUND);
        for(int i=active.size()-1;i>=0;i--)
            if(active.getCompound(i).getString("Effect").equals(effect.name()))active.remove(i);
        player.getPersistentData().put(ACTIVE,active);
    }
    public static double spellDiscount(Player player){
        return hasActive(player,Effect.NEXT_QI_SPELL)?Math.min(.95,bonus(player,Effect.NEXT_QI_SPELL)):0;
    }
    public static void spendSpellDiscount(Player player){
        if (hasActive(player,Effect.NEXT_QI_SPELL) && player instanceof ServerPlayer server) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(server, "pill_effect"); }
        removeActive(player,Effect.NEXT_QI_SPELL);
    }
    @SubscribeEvent public static void meleeDamage(LivingDamageEvent.Pre event){
        if(event.getNewDamage()>0&&event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof ServerPlayer player
                && hasActive(player,Effect.NEXT_MELEE)){
            event.setNewDamage((float)(event.getNewDamage()*(1+bonus(player,Effect.NEXT_MELEE))));
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "pill_effect");
            removeActive(player,Effect.NEXT_MELEE);
        }
    }
    @SubscribeEvent public static void effectApplicable(MobEffectEvent.Applicable event){
        LivingEntity player=event.getEntity();
        var effect=event.getEffectInstance().getEffect();
        if(hasActive(player,Effect.MIND_SHIELD)
                &&(effect.equals(MobEffects.MOVEMENT_SLOWDOWN)||effect.equals(MobEffects.WEAKNESS))
                ||hasActive(player,Effect.WITHER_SHIELD)&&effect.equals(MobEffects.WITHER))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent event){
        LivingEntity p=event.getEntity();
        if(!p.level().isClientSide)
            event.setStrength((float)(event.getStrength()*(1-Math.min(1,bonus(p,Effect.KNOCKBACK_RESIST)))));
    }
    @SubscribeEvent public static void plantTooltip(ItemTooltipEvent event){
        var key=BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if(!key.getNamespace().equals("immortal_cultivation_mod"))return;
        Plant plant=PLANTS.get(key.getPath());if(plant==null)return;
        var lines=event.getToolTip();
        boolean tinted=true;
        for(Element element:Element.values()){
            double value=plant.elements().getOrDefault(element,0.);
            if(value!=0)lines.add(plantLine(Component.translatable("alchemy.element."+element.name()).append(": "+PillData.number(value)),tinted,
                    switch(element){
                        case WATER,ICE -> 0x91BBC9;
                        case WOOD -> 0x9DBE93;
                        case EARTH,METAL -> 0xC3B58C;
                        case FIRE -> 0xCC9C88;
                        case THUNDER,YIN -> 0xB2A0C7;
                        case WIND -> 0xA3C6BC;
                        case YANG -> 0xD0C69E;
                    }));
        }
        for(Effect effect:Effect.values()){
            double value=plant.effects().getOrDefault(effect,0.);
            if(value!=0)lines.add(plantLine(Component.translatable("alchemy.effect."+effect.name()).append(": +"+PillData.number(percentAmount(effect,value))+"%"),tinted,effect==Effect.TOXICITY?0xCC9C88:0xA7BFA0));
        }
        lines.add(plantLine(Component.translatable("alchemy.strength").append(": "+PillData.number(plant.strength())),tinted,0xADB4BC));
        lines.add(plantLine(Component.translatable("alchemy.stability").append(": "+PillData.number(plant.stability())),tinted,0xADB4BC));
        for(Trait trait:Trait.values())if(plant.traits().contains(trait))
            lines.add(plantLine(Component.translatable("alchemy.trait."+trait.name()),tinted,0xC9B78D));
    }
    private static net.minecraft.network.chat.MutableComponent plantLine(
            net.minecraft.network.chat.MutableComponent line,boolean tinted,int color){
        return tinted?line.withStyle(style->style.withColor(color)):line;
    }
}
