package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ManifestedWeaponItem;
import com.example.immortal_cultivation_mod.item.QiPouchStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** World-owned custody for deployed physical swords, including unloaded entities. */
@EventBusSubscriber(modid="immortal_cultivation_mod")
public final class FlyingSwordRecovery extends SavedData {
    private static final String SETTING="ImmortalCultivationFlyingSwordAutoRecall";
    private static final Factory<FlyingSwordRecovery> FACTORY=new Factory<>(FlyingSwordRecovery::new,FlyingSwordRecovery::load);
    private final Map<UUID,CompoundTag> swords=new HashMap<>();
    // ServerLevel's public lookup excludes entities in hidden chunk sections.
    // Absence there does not mean the entity has unloaded or may be replaced.
    private final Map<UUID,FlyingSwordEntity> liveSwords=new HashMap<>();

    public static FlyingSwordRecovery get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"immortal_cultivation_flying_swords");}
    public static boolean autoRecallEnabled(ServerPlayer owner){return !owner.getPersistentData().contains(SETTING)||owner.getPersistentData().getBoolean(SETTING);}
    public static void setAutoRecall(ServerPlayer owner,boolean enabled){owner.getPersistentData().putBoolean(SETTING,enabled);}
    private static FlyingSwordRecovery load(CompoundTag root,HolderLookup.Provider registries){
        var data=new FlyingSwordRecovery();
        for(var tag:root.getList("Swords",Tag.TAG_COMPOUND)){var entry=(CompoundTag)tag;if(entry.hasUUID("Entity")&&entry.hasUUID("Owner"))data.swords.put(entry.getUUID("Entity"),entry.copy());}
        return data;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider registries){var entries=new ListTag();swords.values().forEach(e->entries.add(e.copy()));root.put("Swords",entries);return root;}

    public static void track(FlyingSwordEntity sword){
        if(!(sword.level() instanceof ServerLevel level)||sword.getOwnerUuid()==null||ManifestedWeaponItem.isManifested(sword.pouchRecallStack()))return;
        if(sword.isRecoveryManaged()&&!valid(sword))return;
        var data=get(level.getServer());var entry=new CompoundTag();
        entry.putUUID("Entity",sword.getUUID());entry.putUUID("Owner",sword.getOwnerUuid());
        entry.putString("Dimension",level.dimension().location().toString());entry.put("Stack",sword.pouchRecallStack().saveOptional(level.registryAccess()));
        entry.putInt("Health",sword.getSwordHealth());entry.putBoolean("Orbit",sword.isControlled());entry.putBoolean("Flight",sword.isSwordFlight());
        data.swords.put(sword.getUUID(),entry);data.liveSwords.put(sword.getUUID(),sword);sword.setRecoveryManaged();data.setDirty();
    }
    public static boolean valid(FlyingSwordEntity sword){
        if(!sword.isRecoveryManaged()||!(sword.level() instanceof ServerLevel level))return true;
        var data=get(level.getServer());var e=data.swords.get(sword.getUUID());var live=data.liveSwords.get(sword.getUUID());
        return e!=null&&!e.getBoolean("Pending")&&e.getUUID("Owner").equals(sword.getOwnerUuid())
                &&(live==null||live.isRemoved()||live==sword);
    }
    public static void complete(FlyingSwordEntity sword){
        if(sword.level() instanceof ServerLevel level){var data=get(level.getServer());data.liveSwords.remove(sword.getUUID(),sword);if(data.swords.remove(sword.getUUID())!=null)data.setDirty();}
    }
    /** No item is dropped when an owner is absent or inventory is full. */
    public static void park(FlyingSwordEntity sword){
        if(!sword.isRecoveryManaged())track(sword);
        if(sword.level() instanceof ServerLevel level){
            var data=get(level.getServer());var entry=data.swords.get(sword.getUUID());
            if(entry!=null){entry.putBoolean("Pending",true);entry.putInt("Health",sword.getSwordHealth());data.setDirty();}
        }
        sword.discard();
    }
    public static boolean returnToInventory(FlyingSwordEntity sword,ServerPlayer owner){
        if(!valid(sword))return false;
        if(sword.getOwnerUuid()==null){sword.setOwnerUuid(owner.getUUID());sword.setOwner(owner);}
        if(!owner.getUUID().equals(sword.getOwnerUuid()))return false;
        ItemStack stack=sword.pouchRecallStack();var link=QiPouchStorage.linkedPouch(stack);
        boolean delivered=link!=null&&link.ownerId().equals(owner.getUUID())&&QiPouchStorage.insertRecalled(owner,link.pouchId(),stack);
        if(!delivered)delivered=owner.getInventory().add(stack);
        if(delivered){complete(sword);sword.discard();owner.containerMenu.broadcastChanges();return true;}
        // Inventory.add may mutate a stack. Swords have count one; keep the original
        // component-bearing record until an insertion has actually succeeded.
        if(ManifestedWeaponItem.isManifested(sword.pouchRecallStack()))return false;
        park(sword);return true;
    }
    private FlyingSwordEntity loaded(MinecraftServer server,UUID id){
        var live=liveSwords.get(id);if(live!=null){if(!live.isRemoved())return live;liveSwords.remove(id,live);}
        for(var level:server.getAllLevels())if(level.getEntity(id) instanceof FlyingSwordEntity sword&&!sword.isRemoved()){liveSwords.put(id,sword);return sword;}
        return null;
    }
    public static FlyingSwordEntity flightSword(ServerPlayer owner){
        return get(owner.server).liveSwords.values().stream().filter(sword->!sword.isRemoved()
                &&sword.level()==owner.level()&&sword.isSwordFlight()&&sword.isControlled()&&!sword.isDisabled()
                &&owner.getUUID().equals(sword.getOwnerUuid())&&valid(sword)).findFirst().orElse(null);
    }
    public static void tickOwner(ServerPlayer owner){
        if(!owner.isAlive())return;
        var data=get(owner.server);
        for(var id:java.util.List.copyOf(data.swords.keySet())){
            var entry=data.swords.get(id);if(entry==null||!entry.getUUID("Owner").equals(owner.getUUID()))continue;
            var live=data.loaded(owner.server,id);
            // Portal transfers return custody to the player. They do not depend
            // on either dimension's entity chunks becoming visible first.
            if(!entry.getString("Dimension").equals(owner.level().dimension().location().toString())){
                entry.putBoolean("Pending",true);data.setDirty();if(live!=null)live.discard();
            }
            if(!entry.getBoolean("Pending")&&live!=null){
                if(live.isControlled()&&live.level()!=owner.level())park(live);
                else if(live.isControlled()&&live.distanceToSqr(owner)>64)live.catchUpToOwner(owner);
                continue;
            }
            if(!entry.getBoolean("Pending")&&!entry.getBoolean("Orbit")&&!autoRecallEnabled(owner))continue;
            if(entry.getBoolean("Orbit")&&!entry.getBoolean("Pending")){recoverOrbit(data,id,entry,live,owner);continue;}
            ItemStack stack=ItemStack.parseOptional(owner.registryAccess(),entry.getCompound("Stack"));
            if(stack.isEmpty()){data.swords.remove(id);data.setDirty();continue;}
            var link=QiPouchStorage.linkedPouch(stack);
            boolean inserted=link!=null&&link.ownerId().equals(owner.getUUID())&&QiPouchStorage.insertRecalled(owner,link.pouchId(),stack);
            if(!inserted)inserted=owner.getInventory().add(stack);
            if(inserted){data.swords.remove(id);data.setDirty();owner.containerMenu.broadcastChanges();}
            else if(!entry.getBoolean("Notified")){
                entry.putBoolean("Notified",true);data.setDirty();
                owner.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.sword_recall_inventory_full"),true);
            }
        }
    }
    private static void recoverOrbit(FlyingSwordRecovery data,UUID id,CompoundTag entry,FlyingSwordEntity old,ServerPlayer owner){
        var stack=ItemStack.parseOptional(owner.registryAccess(),entry.getCompound("Stack"));
        var replacement=FlyingSwordEntity.restoreControlled(owner,stack,entry.getInt("Health"),entry.getBoolean("Flight"));
        if(replacement==null){entry.putBoolean("Pending",true);data.setDirty();if(old!=null)old.discard();return;}
        // Removing the old claim invalidates any late chunk copy. The replacement
        // has a fresh UUID; stale entities cannot return a second stack.
        data.swords.remove(id);data.liveSwords.remove(id);data.setDirty();if(old!=null)old.discard();
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof FlyingSwordEntity sword)||!sword.isRecoveryManaged())return;
        // Reject retired chunk copies before clients can receive their spawn packet.
        if(!valid(sword)){event.setCanceled(true);sword.discard();return;}
        get(level.getServer()).liveSwords.put(sword.getUUID(),sword);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){
        // Tracking also ends on entry into a hidden chunk. Keep that live entity;
        // only actual removal/unload relinquishes its in-memory identity.
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof FlyingSwordEntity sword&&sword.isRemoved())
            get(level.getServer()).liveSwords.remove(sword.getUUID(),sword);
    }
    public static int pendingCount(ServerPlayer owner){return (int)get(owner.server).swords.values().stream().filter(e->e.getUUID("Owner").equals(owner.getUUID())&&e.getBoolean("Pending")).count();}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p&&p.tickCount%10==0)tickOwner(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p){
            var data=get(p.server);for(var e:java.util.List.copyOf(data.swords.values()))if(e.getUUID("Owner").equals(p.getUUID())&&e.getBoolean("Orbit")){
                var live=data.loaded(p.server,e.getUUID("Entity"));if(live!=null)park(live);else{e.putBoolean("Pending",true);data.setDirty();}
            }
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)tickOwner(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(event.getOriginal().getPersistentData().contains(SETTING))event.getEntity().getPersistentData().putBoolean(SETTING,event.getOriginal().getPersistentData().getBoolean(SETTING));
    }
}
