package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.TrainingDummyEntity;
import com.example.immortal_cultivation_mod.training.TrainingDummyControls;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.EventPriority;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID)
public final class DummyControlStickItem extends Item {
    public DummyControlStickItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.translatable("training_dummy.immortal_cultivation_mod.bind_hint").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(bound(stack)!=null)lines.add(Component.translatable("training_dummy.immortal_cultivation_mod.bound").withStyle(net.minecraft.ChatFormatting.GOLD));
    }
    public static UUID bound(ItemStack stack) {
        if(!(stack.getItem() instanceof DummyControlStickItem))return null;
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return tag.hasUUID("BoundDummy")?tag.getUUID("BoundDummy"):null;
    }
    public static void bind(ItemStack stack,TrainingDummyEntity dummy) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putUUID("BoundDummy",dummy.getUUID());tag.putString("DummyDimension",dummy.level().dimension().location().toString());
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static UUID heldBinding(ServerPlayer player) {
        UUID main=bound(player.getMainHandItem());return main!=null?main:bound(player.getOffhandItem());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server) {
            UUID id=bound(player.getItemInHand(hand));
            if(id==null)server.displayClientMessage(Component.translatable("training_dummy.immortal_cultivation_mod.bind_hint"),true);
            else TrainingDummyControls.open(server,id);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() instanceof ServerPlayer player) {
            UUID id=bound(context.getItemInHand());
            var dummy=id==null?null:TrainingDummyControls.resolve(player,id);
            if(dummy!=null) {
                Vec3 point=Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
                var block=net.minecraft.core.BlockPos.containing(point);
                if(point.distanceToSqr(player.position())>64*64||!player.serverLevel().hasChunkAt(block)||!player.serverLevel().getWorldBorder().isWithinBounds(block))return InteractionResult.FAIL;
                if(player.isShiftKeyDown()) {
                    var box=dummy.getBoundingBox().move(point.subtract(dummy.position()));
                    if(!player.serverLevel().noCollision(dummy,box)||!player.serverLevel().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(point)))return InteractionResult.FAIL;
                    dummy.halt();dummy.teleportTo(point.x,point.y,point.z);
                }else{dummy.moveToPoint(point);}
                TrainingDummyControls.respond(player,dummy,false);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    private static boolean interact(Player player,InteractionHand hand,net.minecraft.world.entity.Entity target) {
        var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof DummyControlStickItem))return false;
        if(player instanceof ServerPlayer server) {
            if(target instanceof TrainingDummyEntity dummy) {
                UUID current=bound(stack);
                if(!player.isShiftKeyDown()&&current!=null&&!current.equals(dummy.getUUID())) {
                    var controlled=TrainingDummyControls.resolve(server,current);
                    if(controlled!=null&&TrainingDummyControls.target(controlled,dummy))TrainingDummyControls.message(server,"target_set");
                    return true;
                }
                if(!TrainingDummyControls.mayOwn(server,dummy)){TrainingDummyControls.message(server,"denied");return true;}
                if(dummy.controller==null)dummy.controller=server.getUUID();
                bind(stack,dummy);
                if(player.isShiftKeyDown())TrainingDummyControls.message(server,"bound");else TrainingDummyControls.open(server,dummy.getUUID());
            }else if(target instanceof LivingEntity living) {
                UUID id=bound(stack);var dummy=id==null?null:TrainingDummyControls.resolve(server,id);
                if(dummy!=null&&TrainingDummyControls.target(dummy,living))TrainingDummyControls.message(server,"target_set");
            }
        }
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var player=event.getEntity();var stack=player.getMainHandItem();
        if(!(stack.getItem() instanceof DummyControlStickItem))return;
        // Cancel before damage, knockback, reputation changes, or any player attack abilities.
        event.setCanceled(true);
        if(!(player instanceof ServerPlayer server)||!(event.getTarget() instanceof LivingEntity living))return;
        UUID id=bound(stack);
        if(id==null){TrainingDummyControls.message(server,"bind_hint");return;}
        var dummy=TrainingDummyControls.resolve(server,id);
        if(dummy==null){TrainingDummyControls.message(server,"unavailable");return;}
        if(TrainingDummyControls.target(dummy,living))TrainingDummyControls.message(server,"target_set");
    }
    @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract event) {
        if(interact(event.getEntity(),event.getHand(),event.getTarget())){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);}
    }
    @SubscribeEvent public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        if(interact(event.getEntity(),event.getHand(),event.getTarget())){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);}
    }
}
