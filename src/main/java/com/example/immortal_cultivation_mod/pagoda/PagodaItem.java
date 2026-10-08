package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.network.WeaponUseAnimations;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class PagodaItem extends SwordItem implements GeoItem {
    private static final String COOLDOWN_KEY="immortal_cultivation_mod.pagodaCooldownUntil";
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public PagodaItem(Properties properties){super(Tiers.IRON,properties.stacksTo(1).durability(3000).attributes(SwordItem.createAttributes(Tiers.IRON,8,-2.6F)));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);
        if(!(player instanceof ServerPlayer server))return InteractionResultHolder.sidedSuccess(stack,true);
        UUID itemId=identity(stack);
        var saved=PagodaData.get(server.server);
        var active=saved.towers.values().stream().filter(t->!t.gourd&&t.owner.equals(server.getUUID())&&!t.closing&&itemId.equals(t.container)).findFirst().orElse(null);
        // Bind deployed towers from older saves to the item on their first use.
        if(active==null){active=saved.towers.values().stream().filter(t->!t.gourd&&t.container==null&&t.owner.equals(server.getUUID())&&!t.closing).findFirst().orElse(null);if(active!=null){active.container=itemId;saved.setDirty();}}
        if(active!=null){
            if(!server.isShiftKeyDown()){active.closing=true;saved.setDirty();}
            else if(PagodaSystem.ownerInside(active)){
                PagodaSystem.release(server.server,active,active.prisoners.get(server.getUUID()));
            }else if(!PagodaSystem.enterOwner(server,active))server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.pagoda.entry_wait"),true);
            return InteractionResultHolder.success(stack);
        }
        if(PagodaWorlds.isInterior(level.dimension()))return InteractionResultHolder.fail(stack);
        if(cooldownRemaining(server)>0){server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.pagoda.cooldown",(cooldownRemaining(server)+19)/20),true);return InteractionResultHolder.fail(stack);}
        if(CombatStateManager.isSpellcastingBlocked(server))return InteractionResultHolder.fail(stack);
        if(!server.isCreative()&&CultivationLevels.getRealmIndex(ModAttachments.getData(server).cultivationLevel())<2){
            server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.pagoda.realm"),true);return InteractionResultHolder.fail(stack);
        }
        Vec3 eye=server.getEyePosition();var hit=level.clip(new ClipContext(eye,eye.add(server.getLookAngle().scale(24)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,server));
        if(hit.getType()==HitResult.Type.MISS)return InteractionResultHolder.fail(stack);
        var pos=hit.getBlockPos().above();
        if(!level.getWorldBorder().isWithinBounds(pos)||pos.getY()<level.getMinBuildHeight()||pos.getY()+24>=level.getMaxBuildHeight())return InteractionResultHolder.fail(stack);
        var before=ModAttachments.getData(server);float beforeHealth=server.getHealth();
        if(!ServerEvents.spendQiOrBlood(server,before,PagodaConfig.QI_COST.get())){
            server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.not_enough_qi"),true);return InteractionResultHolder.fail(stack);
        }
        var tower=PagodaSystem.summon(server,stack,pos);if(tower==null){ModAttachments.setData(server,before);server.setHealth(beforeHealth);ServerEvents.syncPlayerData(server);return InteractionResultHolder.fail(stack);}
        tower.enterRequested=server.isShiftKeyDown();
        startCooldown(server);
        stack.hurtAndBreak(1,server,EquipmentSlot.MAINHAND);ServerEvents.syncPlayerData(server);
        WeaponUseAnimations.play(server,WeaponUseAnimations.Type.PAGODA);
        return InteractionResultHolder.success(stack);
    }
    public static UUID identity(ItemStack stack){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        String key="immortal_cultivation_mod.pagoda_item_id";
        if(!tag.hasUUID(key)){tag.putUUID(key,UUID.randomUUID());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
        return tag.getUUID(key);
    }
    public static long cooldownRemaining(ServerPlayer player){return Math.max(0,player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(COOLDOWN_KEY)-player.server.overworld().getGameTime());}
    public static void startCooldown(ServerPlayer player){
        var persistent=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persistent.putLong(COOLDOWN_KEY,player.server.overworld().getGameTime()+PagodaConfig.COOLDOWN.get()*20L);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persistent);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
    }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer){consumer.accept(new GeoRenderProvider(){
        private com.example.immortal_cultivation_mod.client.renderer.PagodaItemRenderer renderer;
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer(){if(renderer==null)renderer=new com.example.immortal_cultivation_mod.client.renderer.PagodaItemRenderer();return renderer;}
    });}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this,"idle",state->state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
