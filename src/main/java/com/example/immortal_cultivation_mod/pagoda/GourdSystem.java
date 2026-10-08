package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Water is authoritative saved-world state, never trusted from an item's display tag. */
public final class GourdSystem {
    public static final int MAX_WATER=100, DRINK_COST=10, DEATH_GAIN=10;
    private static final String ID="immortal_cultivation_mod.gourd_id", WATER="immortal_cultivation_mod.gourd_water";
    private static final String STACKS="immortal_cultivation_mod.gourd_stacks", UNTIL="immortal_cultivation_mod.gourd_buff_until";
    private static final ResourceLocation BUFF=PagodaWorlds.id("gourd_drink");
    private static final List<net.minecraft.core.Holder<Attribute>> STATS=List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,
            Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.MOVEMENT_SPEED,Attributes.ATTACK_SPEED,Attributes.LUCK);
    public static UUID id(ItemStack stack){var tag=tag(stack);return tag.hasUUID(ID)?tag.getUUID(ID):null;}
    public static UUID ensure(ServerPlayer player,ItemStack stack){
        UUID id=id(stack);if(id==null){id=UUID.randomUUID();var tag=tag(stack);tag.putUUID(ID,id);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
        var data=PagodaData.get(player.server);if(!data.gourdWater.containsKey(id)){data.gourdWater.put(id,0);data.setDirty();}
        sync(stack,water(data,id));return id;
    }
    private static CompoundTag tag(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return data==null?new CompoundTag():data.copyTag();}
    public static int shownWater(ItemStack stack){return Math.clamp(tag(stack).getInt(WATER),0,MAX_WATER);}
    public static int water(PagodaData data,UUID id){return Math.clamp(data.gourdWater.getOrDefault(id,0),0,MAX_WATER);}
    public static void changeWater(PagodaData data,UUID id,int delta){if(id==null)return;data.gourdWater.put(id,Math.clamp(water(data,id)+delta,0,MAX_WATER));data.setDirty();}
    public static void sync(ItemStack stack,int water){var tag=tag(stack);if(tag.getInt(WATER)==water)return;tag.putInt(WATER,water);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static boolean drink(ServerPlayer player,ItemStack stack){
        UUID id=ensure(player,stack);var data=PagodaData.get(player.server);
        if(water(data,id)<DRINK_COST)return false;
        changeWater(data,id,-DRINK_COST);sync(stack,water(data,id));
        addBuff(player);return true;
    }
    public static void addBuff(ServerPlayer player){
        var tag=player.getPersistentData();int stacks=Math.min(3,stacks(player)+1);
        tag.putInt(STACKS,stacks);tag.putLong(UNTIL,player.server.overworld().getGameTime()+600);updateBuff(player);
    }
    public static int stacks(ServerPlayer player){return player.getPersistentData().getLong(UNTIL)>player.server.overworld().getGameTime()?Math.clamp(player.getPersistentData().getInt(STACKS),0,3):0;}
    public static double multiplier(int stacks){return Math.pow(1.05,Math.clamp(stacks,0,3));}
    public static double multiplier(net.minecraft.world.entity.Entity entity){return entity instanceof ServerPlayer player?multiplier(stacks(player)):1;}
    public static void updateBuff(ServerPlayer player){
        int stacks=player.isAlive()?stacks(player):0;double amount=multiplier(stacks)-1;
        for(var stat:STATS){var attribute=player.getAttribute(stat);if(attribute==null)continue;
            var existing=attribute.getModifier(BUFF);
            if(existing!=null&&Math.abs(existing.amount()-amount)<1E-9)continue;
            attribute.removeModifier(BUFF);
            if(stacks>0)attribute.addTransientModifier(new AttributeModifier(BUFF,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if(stacks==0){player.getPersistentData().remove(STACKS);player.getPersistentData().remove(UNTIL);}
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
    }
    public static float damagePerSecond(LivingEntity prisoner){
        double base=prisoner.getMaxHealth();
        if(prisoner instanceof ServerPlayer player){var data=ModAttachments.getData(player);base=CultivationLevels.getLevelDef(data.cultivationLevel()).maxHp()+data.maxHpBonus();}
        return (float)Math.max(.05,base*.05);
    }
    public static void death(net.minecraft.server.MinecraftServer server,PagodaData.Tower tower,UUID prisoner){
        if(tower.gourd&&tower.prisoners.containsKey(prisoner))changeWater(PagodaData.get(server),tower.container,DEATH_GAIN);
    }
    private GourdSystem(){}
}
