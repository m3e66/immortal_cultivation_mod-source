package com.example.immortal_cultivation_mod.magicritual.network;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.TeleportCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ClientboundCenterAccessPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ClientboundTeleportDestinationsPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundCenterSettingsPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCenterPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCirclePayload;
import com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToClient(ClientboundCenterAccessPayload.TYPE, ClientboundCenterAccessPayload.STREAM_CODEC, ModPayloads::handleCenterAccessSync);
      registrar.playToClient(ClientboundTeleportDestinationsPayload.TYPE, ClientboundTeleportDestinationsPayload.STREAM_CODEC, ModPayloads::handleTeleportDestinationsSync);
      registrar.playToServer(ServerboundPlaceCirclePayload.TYPE, ServerboundPlaceCirclePayload.STREAM_CODEC, ModPayloads::handlePlaceCircle);
      registrar.playToServer(ServerboundPlaceCenterPayload.TYPE, ServerboundPlaceCenterPayload.STREAM_CODEC, ModPayloads::handlePlaceCenter);
      registrar.playToServer(ServerboundCenterSettingsPayload.TYPE, ServerboundCenterSettingsPayload.STREAM_CODEC, ModPayloads::handleCenterSettings);
   }

   private static void handlePlaceCircle(ServerboundPlaceCirclePayload payload, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer)context.player();
      CircleType type = CircleType.valueOf(payload.circleType().toUpperCase());
      ServerLevel level = player.serverLevel();
      BlockPos placePos = player.blockPosition();
      if (level.getBlockState(placePos).isAir()) {
         level.setBlock(placePos, (BlockState)((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()).defaultBlockState().setValue(MagicCircleBlock.CIRCLE_TYPE, type), 3);
      }

      player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0, false, true, true));
   }

   private static void handlePlaceCenter(ServerboundPlaceCenterPayload payload, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer)context.player();
      int radius = MagicCenterBlockEntity.normalizeRadius(payload.radius());
      ServerLevel level = player.serverLevel();
      BlockPos placePos = player.blockPosition();
      if (level.getBlockState(placePos).isAir()) {
         level.setBlock(placePos, ((Block)ModBlocks.MAGIC_CENTER_BLOCK.get()).defaultBlockState(), 3);
         BlockEntity var7 = level.getBlockEntity(placePos);
         if (var7 instanceof MagicCenterBlockEntity) {
            MagicCenterBlockEntity center = (MagicCenterBlockEntity)var7;
            center.setRadius(radius);
            center.setOwnerUuid(player.getUUID());
         }
      }

      int bleedingTicks = radius * 20;
      player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, bleedingTicks, 0, false, true, true));
   }

   private static void handleCenterSettings(ServerboundCenterSettingsPayload payload, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer)context.player();
      Level level = player.serverLevel();
      BlockEntity var5 = level.getBlockEntity(payload.pos());
      if (var5 instanceof MagicCenterBlockEntity mcbe) {
         AbstractContainerMenu var6 = player.containerMenu;
         if (!(var6 instanceof CenterFuelMenu)) {
            return;
         }

         CenterFuelMenu menu = (CenterFuelMenu)var6;
         if (!menu.centerPos.equals(payload.pos()) || !menu.stillValid(player)) {
            return;
         }

         String action = payload.action();
         boolean managingControllers = "add_controller".equals(action) || "remove_controller".equals(action) || "cycle_teleport_mode".equals(action) || "add_teleport_destination".equals(action) || "remove_teleport_destination".equals(action);
         boolean allowed = managingControllers ? CenterTargetPolicy.canManageControllers(mcbe, player) : CenterTargetPolicy.canEdit(mcbe, player);
         if (!allowed) {
            return;
         }

         switch (action) {
            case "toggle_enabled":
               mcbe.setEnabled(!mcbe.isEnabled());
               break;
            case "radius_down":
               mcbe.setRadius(mcbe.getRadius() - 2);
               break;
            case "radius_up":
               mcbe.setRadius(mcbe.getRadius() + 2);
               break;
            case "set_radius":
               try {
                  mcbe.setRadius(Integer.parseInt(payload.value()));
               } catch (NumberFormatException var13) {
               }
               break;
            case "toggle_hostile":
               mcbe.setAllowHostileMobs(!mcbe.isAllowHostileMobs());
               break;
            case "toggle_passive":
               mcbe.setAllowPassiveMobs(!mcbe.isAllowPassiveMobs());
               break;
            case "add_blacklist":
               mcbe.addBlacklistedPlayer(payload.value());
               break;
            case "remove_blacklist":
               mcbe.removeBlacklistedPlayer(payload.value());
               break;
            case "add_controller":
               mcbe.addController(payload.value());
               break;
            case "remove_controller":
               mcbe.removeController(payload.value());
               break;
            case "toggle_particles":
               mcbe.setParticlesEnabled(!mcbe.isParticlesEnabled());
               mcbe.setChanged();
               level.sendBlockUpdated(mcbe.getBlockPos(), mcbe.getBlockState(), mcbe.getBlockState(), 3);
               break;
            case "set_glyph_color":
               try {
                  mcbe.setGlyphColor(Integer.parseInt(payload.value(), 16));
                  level.sendBlockUpdated(mcbe.getBlockPos(), mcbe.getBlockState(), mcbe.getBlockState(), 3);
               } catch (NumberFormatException var12) {
               }
               break;
            case "cycle_teleport_mode":
               if (MagicCenterTypeRegistry.TELEPORT.equals(mcbe.getCenterTypeId()) && mcbe.hasTeleportLink()) {
                  mcbe.cycleTeleportLinkMode();
               }
               break;
            case "add_teleport_destination":
               if (MagicCenterTypeRegistry.TELEPORT.equals(mcbe.getCenterTypeId())) {
                  MagicCenterBlockEntity.TeleportLinkResult result = mcbe.addTeleportDestination(payload.value());
                  String var10001 = result.name();
                  player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.teleport.code." + var10001.toLowerCase(Locale.ROOT)), true);
               }
               break;
            case "select_teleport_destination":
               if (MagicCenterTypeRegistry.TELEPORT.equals(mcbe.getCenterTypeId())) {
                  mcbe.selectTeleportDestination(payload.value());
               }
               break;
            case "remove_teleport_destination":
               if (MagicCenterTypeRegistry.TELEPORT.equals(mcbe.getCenterTypeId())) {
                  mcbe.removeTeleportDestination(payload.value());
               }
               break;
            case "begin_teleport":
               if (MagicCenterTypeRegistry.TELEPORT.equals(mcbe.getCenterTypeId()) && TeleportCenterBehavior.beginChannel(mcbe, player)) {
                  player.closeContainer();
               }
         }

         if ("add_blacklist".equals(action) || "remove_blacklist".equals(action) || "add_controller".equals(action) || "remove_controller".equals(action)) {
            PacketDistributor.sendToPlayer(player, new ClientboundCenterAccessPayload(payload.pos(), List.copyOf(mcbe.getBlacklistedPlayers()), List.copyOf(mcbe.getControllerWhitelist())), new CustomPacketPayload[0]);
         }

         if ("add_teleport_destination".equals(action) || "select_teleport_destination".equals(action) || "remove_teleport_destination".equals(action) || "refresh_teleport_destinations".equals(action)) {
            PacketDistributor.sendToPlayer(player, new ClientboundTeleportDestinationsPayload(payload.pos(), mcbe.getTeleportCode(), mcbe.getSelectedTeleportDestinationCode(), mcbe.getTeleportDestinationInfo()), new CustomPacketPayload[0]);
         }
      }

   }

   private static void handleCenterAccessSync(ClientboundCenterAccessPayload payload, IPayloadContext context) {
      AbstractContainerMenu var3 = context.player().containerMenu;
      if (var3 instanceof CenterFuelMenu menu) {
         if (menu.centerPos.equals(payload.pos())) {
            menu.replaceAccessLists(payload.blacklistedPlayers(), payload.controllerPlayers());
         }
      }

   }

   private static void handleTeleportDestinationsSync(ClientboundTeleportDestinationsPayload payload, IPayloadContext context) {
      AbstractContainerMenu var3 = context.player().containerMenu;
      if (var3 instanceof CenterFuelMenu menu) {
         if (menu.centerPos.equals(payload.pos())) {
            menu.replaceTeleportDestinations(payload.localCode(), payload.selectedCode(), payload.destinations());
         }
      }

   }
}
