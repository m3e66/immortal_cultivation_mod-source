package com.example.immortal_cultivation_mod.pagoda;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.WeaponUseAnimations;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class GoldenGourdItem extends Item implements GeoItem,ICurioItem {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private static final String COOLDOWN="immortal_cultivation_mod.gourd_cooldown";
    public GoldenGourdItem(Properties properties){super(properties.stacksTo(1).durability(3000)
            .attributes(SwordItem.createAttributes(Tiers.IRON,5,-1.8F)));software.bernie.geckolib.animatable.SingletonGeoAnimatable.registerSyncedAnimatable(this);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown()){
            if(player instanceof ServerPlayer server){GourdSystem.ensure(server,stack);triggerAnim(server,GeoItem.getOrAssignId(stack,server.serverLevel()),"gourd","drink");}
            player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
        }
        if(!(player instanceof ServerPlayer server))return InteractionResultHolder.sidedSuccess(stack,true);
        if(PagodaWorlds.isInterior(level.dimension())||CombatStateManager.isSpellcastingBlocked(server))return InteractionResultHolder.fail(stack);
        if(!server.isCreative()&&CultivationLevels.getRealmIndex(ModAttachments.getData(server).cultivationLevel())<2){
            server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.pagoda.realm"),true);return InteractionResultHolder.fail(stack);}
        var id=GourdSystem.ensure(server,stack);
        var active=PagodaData.get(server.server).towers.values().stream().filter(t->t.gourd&&!t.closing&&id.equals(t.container)).findFirst().orElse(null);
        if(active!=null){
            if(!active.owner.equals(server.getUUID()))return InteractionResultHolder.fail(stack);
            if(!PagodaSystem.reinforce(server,active,100))server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.not_enough_qi"),true);
            return InteractionResultHolder.success(stack);
        }
        long time=server.server.overworld().getGameTime();
        if(server.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(COOLDOWN)>time){server.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.pagoda.cooldown",(server.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(COOLDOWN)-time+19)/20),true);return InteractionResultHolder.fail(stack);}
        Vec3 eye=server.getEyePosition();var hit=level.clip(new ClipContext(eye,eye.add(server.getLookAngle().scale(24)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,server));
        var pos=hit.getType()==HitResult.Type.MISS?server.blockPosition():hit.getBlockPos().above();
        if(!level.getWorldBorder().isWithinBounds(pos)||pos.getY()<level.getMinBuildHeight()||pos.getY()+24>=level.getMaxBuildHeight())return InteractionResultHolder.fail(stack);
        var before=ModAttachments.getData(server);float health=server.getHealth();
        if(!ServerEvents.spendQiOrBlood(server,before,PagodaConfig.QI_COST.get()))return InteractionResultHolder.fail(stack);
        var tower=PagodaSystem.summon(server,stack,pos,true,id);
        if(tower==null){ModAttachments.setData(server,before);server.setHealth(health);ServerEvents.syncPlayerData(server);return InteractionResultHolder.fail(stack);}
        var persistent=server.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);persistent.putLong(COOLDOWN,time+PagodaConfig.COOLDOWN.get()*20L);server.getPersistentData().put(Player.PERSISTED_NBT_TAG,persistent);
        triggerAnim(server,GeoItem.getOrAssignId(stack,server.serverLevel()),"gourd","suck");
        WeaponUseAnimations.play(server,WeaponUseAnimations.Type.PAGODA);ServerEvents.syncPlayerData(server);
        return InteractionResultHolder.success(stack);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(entity instanceof ServerPlayer player){if(GourdSystem.drink(player,stack)){
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_DRINK,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }else player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.gourd.empty"),true);}
        return stack;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(entity instanceof ServerPlayer player&&!level.isClientSide&&player.tickCount%10==0)GourdSystem.ensure(player,stack);}
    @Override public void curioTick(SlotContext context,ItemStack stack){if(context.entity() instanceof ServerPlayer player&&player.tickCount%10==0)GourdSystem.ensure(player,stack);}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.identifier().equals("waist_accessory");}
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13*GourdSystem.shownWater(stack)/100F);}
    @Override public int getBarColor(ItemStack stack){return 0x4BCBCD;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.gourd.water",GourdSystem.shownWater(stack)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.immortal_cultivation_mod.gourd.drink").withStyle(ChatFormatting.GREEN));
    }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer){consumer.accept(new GeoRenderProvider(){
        private com.example.immortal_cultivation_mod.client.renderer.GoldenGourdItemRenderer renderer;
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer(){if(renderer==null)renderer=new com.example.immortal_cultivation_mod.client.renderer.GoldenGourdItemRenderer();return renderer;}
    });}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"gourd",state->state.setAndContinue(RawAnimation.begin().thenLoop("idle"))).triggerableAnim("drink",RawAnimation.begin().thenPlay("drink")).triggerableAnim("suck",RawAnimation.begin().thenPlay("suck")));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
