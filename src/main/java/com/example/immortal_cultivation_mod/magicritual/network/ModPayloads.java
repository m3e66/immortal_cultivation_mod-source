package com.example.immortal_cultivation_mod.magicritual.network;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlock;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCircleBlock;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ClientboundCenterAccessPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundCenterSettingsPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCenterPayload;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCirclePayload;
import com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToClient(ClientboundCenterAccessPayload.TYPE,
            ClientboundCenterAccessPayload.STREAM_CODEC, ModPayloads::handleCenterAccessSync);
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
         if (level.getBlockEntity(placePos) instanceof MagicCenterBlockEntity center) {
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
      if (level.getBlockEntity(payload.pos()) instanceof MagicCenterBlockEntity mcbe) {
         if (!(player.containerMenu instanceof CenterFuelMenu menu)
               || !menu.centerPos.equals(payload.pos())
               || !menu.stillValid(player)) {
            return;
         }
         String action = payload.action();
         boolean managingControllers = "add_controller".equals(action)
               || "remove_controller".equals(action)
               || "cycle_teleport_mode".equals(action);
         boolean allowed = managingControllers
               ? CenterTargetPolicy.canManageControllers(mcbe, player)
               : CenterTargetPolicy.canEdit(mcbe, player);
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
               } catch (NumberFormatException ignored) {
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
               } catch (NumberFormatException ignored) {
               }
               break;
            case "cycle_teleport_mode":
               if (com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry.TELEPORT
                     .equals(mcbe.getCenterTypeId()) && mcbe.hasTeleportLink()) {
                  mcbe.cycleTeleportLinkMode();
               }
         }
         if ("add_blacklist".equals(action) || "remove_blacklist".equals(action)
               || "add_controller".equals(action) || "remove_controller".equals(action)) {
            PacketDistributor.sendToPlayer(player, new ClientboundCenterAccessPayload(
                  payload.pos(), List.copyOf(mcbe.getBlacklistedPlayers()),
                  List.copyOf(mcbe.getControllerWhitelist())));
         }
      }
   }

   private static void handleCenterAccessSync(ClientboundCenterAccessPayload payload,
                                               IPayloadContext context) {
      if (context.player().containerMenu instanceof CenterFuelMenu menu
            && menu.centerPos.equals(payload.pos())) {
         menu.replaceAccessLists(payload.blacklistedPlayers(), payload.controllerPlayers());
      }
   }
}
