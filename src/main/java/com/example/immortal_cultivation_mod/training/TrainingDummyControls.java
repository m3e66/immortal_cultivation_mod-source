package com.example.immortal_cultivation_mod.training;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.QiDomainSystem;
import com.example.immortal_cultivation_mod.entity.*;
import com.example.immortal_cultivation_mod.item.DummyControlStickItem;
import com.example.immortal_cultivation_mod.network.TrainingDummyPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

/** Small validated edits; never sends inventories or arbitrary NPC NBT to a client. */
public final class TrainingDummyControls {
    private TrainingDummyControls() { }
    public static boolean mayOwn(ServerPlayer player,TrainingDummyEntity dummy) {
        return dummy.isAlive()&&dummy.level()==player.level()&&player.distanceToSqr(dummy)<=96*96
                &&(dummy.controller==null||dummy.controller.equals(player.getUUID())||player.hasPermissions(2));
    }
    public static TrainingDummyEntity resolve(ServerPlayer player,UUID id) {
        if(!id.equals(DummyControlStickItem.bound(player.getMainHandItem()))&&!id.equals(DummyControlStickItem.bound(player.getOffhandItem())))return null;
        var entity=player.serverLevel().getEntity(id);
        return entity instanceof TrainingDummyEntity dummy&&mayOwn(player,dummy)?dummy:null;
    }
    public static void message(ServerPlayer player,String key){player.displayClientMessage(Component.translatable("training_dummy.immortal_cultivation_mod."+key),true);}
    public static void open(ServerPlayer player,UUID id) {
        var dummy=resolve(player,id);if(dummy==null){message(player,"unavailable");return;}respond(player,dummy,true);
    }
    public static void respond(ServerPlayer player,TrainingDummyEntity dummy,boolean open) {
        PacketDistributor.sendToPlayer(player,new TrainingDummyPayloads.State(dummy.getUUID(),snapshot(dummy),open));
    }
    public static CompoundTag snapshot(TrainingDummyEntity dummy) {
        var result=new CompoundTag();var p=dummy.profile();
        result.putInt("EntityId",dummy.getId());result.putBoolean("Named",dummy.hasCustomName());result.putString("Name",dummy.hasCustomName()?dummy.getName().getString():"Training Dummy");
        result.putString("Realm",p.getString("Realm"));result.putString("Method",p.getString("Method"));
        result.put("Methods",p.getList("Methods",Tag.TAG_STRING).copy());result.put("Spells",p.getList("Spells",Tag.TAG_STRING).copy());
        result.put("MethodProficiencies",p.getCompound("MethodProficiencies").copy());
        result.putFloat("Health",dummy.getHealth());result.putFloat("MaxHealth",dummy.getMaxHealth());
        result.putInt("Qi",p.getInt("Qi"));result.putInt("MaxQi",p.getInt("MaxQi"));
        result.putDouble("Attack",dummy.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));result.putFloat("Magic",dummy.magicAttack);
        result.putDouble("Armor",dummy.getAttributeBaseValue(Attributes.ARMOR));result.putDouble("Speed",dummy.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        result.putBoolean("Domain",dummy.domainRequested);result.putBoolean("Combat",dummy.combatEnabled);result.putBoolean("Mobile",dummy.mobile);
        result.putBoolean("Immortal",dummy.immortal);result.putBoolean("UnlimitedQi",dummy.unlimitedQi);
        result.putString("Target",dummy.getTarget()==null?"":dummy.getTarget().getName().getString());
        return result;
    }
    public static boolean target(TrainingDummyEntity dummy,LivingEntity target) {
        if(target==dummy||!target.isAlive()||target.level()!=dummy.level()||target.distanceToSqr(dummy)>64*64||!QiDomainSystem.canAffect(dummy,target))return false;
        dummy.setTarget(target);
        // Controller selection provokes the mannequin, never an imaginary player hit.
        if(target instanceof net.minecraft.world.entity.Mob mob) {
            mob.setLastHurtByMob(dummy);mob.setTarget(dummy);
            if(mob instanceof net.minecraft.world.entity.NeutralMob neutral) {
                neutral.setPersistentAngerTarget(dummy.getUUID());neutral.startPersistentAngerTimer();
            }
        }
        return true;
    }
    public static boolean edit(ServerPlayer player,UUID id,String action,String key,String value) {
        var dummy=resolve(player,id);if(dummy==null)return false;
        var p=dummy.profile();
        switch(action) {
            case "toggle" -> {switch(key) {
                case "Domain" -> {if(!dummy.domainRequested&&(!QiDomainSystem.eligible(dummy)||CultivationMethods.get(p.getString("Method"))==null)){message(player,"realm_required");return false;}dummy.setDomainRequested(!dummy.domainRequested);}
                case "Combat" -> {dummy.combatEnabled=!dummy.combatEnabled;if(!dummy.combatEnabled)dummy.halt();}
                case "Mobile" -> {dummy.mobile=!dummy.mobile;if(!dummy.mobile)dummy.getNavigation().stop();}
                case "Immortal" -> dummy.immortal=!dummy.immortal;
                case "UnlimitedQi" -> dummy.unlimitedQi=!dummy.unlimitedQi;
                default -> {return false;}
            }}
            case "unlock_all_methods" -> p.put("Methods",TrainingDummyEntity.strings(
                    CultivationMethods.allMethods().stream().map(CultivationMethods.MethodDef::id).toList()));
            case "unlock_all_spells" -> p.put("Spells",TrainingDummyEntity.strings(
                    ModSpells.all().stream().map(ModSpells.SpellDef::id).filter(NpcSpellcasting::isNpcSupportedSpell).toList()));
            case "method_known" -> {
                if(CultivationMethods.get(key)==null)return false;
                var methods=new ArrayList<>(TrainingDummyEntity.strings(p,"Methods"));
                if(methods.remove(key)){if(p.getString("Method").equals(key)){p.putString("Method","");dummy.setDomainRequested(false);}}
                else if(methods.size()<17)methods.add(key);
                p.put("Methods",TrainingDummyEntity.strings(methods));
            }
            case "method" -> {if(!TrainingDummyEntity.strings(p,"Methods").contains(key))return false;p.putString("Method",key);}
            case "mastery" -> {
                if(CultivationMethods.get(key)==null||!TrainingDummyEntity.strings(p,"Methods").contains(key))return false;
                int proficiency;try{proficiency=Integer.parseInt(value);}catch(NumberFormatException e){return false;}
                if(proficiency<0||proficiency>10000)return false;
                var known=p.getCompound("MethodProficiencies");known.putInt(key,proficiency);p.put("MethodProficiencies",known);
            }
            case "spell_known" -> {
                if(ModSpells.get(key)==null||!NpcSpellcasting.isNpcSupportedSpell(key))return false;
                var spells=new ArrayList<>(TrainingDummyEntity.strings(p,"Spells"));
                if(!spells.remove(key)&&spells.size()<256)spells.add(key);p.put("Spells",TrainingDummyEntity.strings(spells));
            }
            case "cast" -> {if(!dummy.cast(key)){message(player,"cast_blocked");return false;}}
            case "stats" -> {
                if(!CultivationLevels.allLevels().contains(key))return false;
                String[] parts=value.split(",",-1);if(parts.length!=6)return false;
                double[] n=new double[6];try{for(int i=0;i<6;i++)n[i]=Double.parseDouble(parts[i]);}catch(NumberFormatException e){return false;}
                double[] min={1,1,0,0,0,0},max={10000000,1000000000,1000000,1000000,2048,1};
                for(int i=0;i<6;i++)if(!Double.isFinite(n[i])||n[i]<min[i]||n[i]>max[i])return false;
                p.putString("Realm",key);p.putInt("MaxQi",(int)n[1]);p.putInt("Qi",Math.min(p.getInt("Qi"),(int)n[1]));
                dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(n[0]);dummy.setHealth(dummy.getMaxHealth());
                dummy.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(n[2]);dummy.magicAttack=(float)n[3];
                dummy.getAttribute(Attributes.ARMOR).setBaseValue(n[4]);dummy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(n[5]);
                if(!QiDomainSystem.eligible(dummy))dummy.setDomainRequested(false);
            }
            case "name" -> {if(value.isBlank()||value.length()>48)return false;dummy.setCustomName(Component.literal(value));p.putString("Name",value);}
            case "reset" -> dummy.resetVitals();
            case "clear_debuffs" -> dummy.clearDebuffs();
            case "stop" -> dummy.halt();
            case "target_self" -> {if(!target(dummy,player)){message(player,"protected_target");return false;}}
            case "copy_weapon" -> dummy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,player.getOffhandItem().copyWithCount(1));
            default -> {return false;}
        }
        respond(player,dummy,false);return true;
    }
}
