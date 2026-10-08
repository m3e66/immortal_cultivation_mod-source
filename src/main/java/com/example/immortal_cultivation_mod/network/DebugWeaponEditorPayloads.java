package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.screen.DebugWeaponEditorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class DebugWeaponEditorPayloads {
    private DebugWeaponEditorPayloads() {}
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_weapon_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());
        @Override public Type<Open> type() { return TYPE; }
    }
    public record Edit(int menuId, int action, int restriction, int value) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "debug_weapon_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.menuId); b.writeVarInt(p.action); b.writeVarInt(p.restriction); b.writeVarInt(p.value);
        }, b -> new Edit(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        @Override public Type<Edit> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Open.TYPE, Open.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && DebugWeaponEditorMenu.authorized(player)) {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new DebugWeaponEditorMenu(id, inventory),
                        Component.translatable("screen.immortal_cultivation_mod.debug_tab_weapon")));
            }
        }));
        registrar.playToServer(Edit.TYPE, Edit.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof DebugWeaponEditorMenu menu
                    && menu.containerId == payload.menuId() && menu.stillValid(player)) {
                if (payload.action() == 3) {
                    player.closeContainer();
                    ModPayloads.openDebugStatEditor(player, player, Math.clamp(payload.restriction(), 0, 6));
                } else menu.edit(player, payload.action(), payload.restriction(), payload.value());
            }
        }));
    }
}
