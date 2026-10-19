package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.shentong.ShentongRegistry;
import java.util.List;
import java.util.ArrayList;
import com.example.immortal_cultivation_mod.curse.DebugShentongEditor;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid=ImmortalCultivationMod.MODID, bus=EventBusSubscriber.Bus.MOD)
public final class DebugShentongPayloads {
    private DebugShentongPayloads() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path); }

    public record Request(UUID targetId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("debug_shentong_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p)->b.writeUUID(p.targetId), b->new Request(b.readUUID()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Edit(UUID targetId, String ability, String field, int value) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(id("debug_shentong_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = StreamCodec.of((b,p)->{
            b.writeUUID(p.targetId); b.writeUtf(p.ability,96); b.writeUtf(p.field,32); b.writeVarInt(p.value);
        },b->new Edit(b.readUUID(),b.readUtf(96),b.readUtf(32),b.readVarInt()));
        @Override public Type<Edit> type() { return TYPE; }
    }
    public record FieldState(String id, String nameKey, int min, int max, boolean toggle, int value) {}
    public record AbilityState(String id, String nameKey, boolean owned, List<FieldState> fields) {
        public AbilityState { fields = List.copyOf(fields); }
    }
    public record State(UUID targetId, List<AbilityState> abilities) implements CustomPacketPayload {
        public State { abilities = List.copyOf(abilities); }
        public static final Type<State> TYPE = new Type<>(id("debug_shentong_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((b,p)->{
            b.writeUUID(p.targetId); b.writeVarInt(p.abilities.size());
            for (var ability : p.abilities) {
                b.writeUtf(ability.id,96); b.writeUtf(ability.nameKey,256); b.writeBoolean(ability.owned);
                b.writeVarInt(ability.fields.size());
                for (var field : ability.fields) {
                    b.writeUtf(field.id,32); b.writeUtf(field.nameKey,256);
                    b.writeVarInt(field.min); b.writeVarInt(field.max); b.writeBoolean(field.toggle); b.writeVarInt(field.value);
                }
            }
        },b->{
            UUID target = b.readUUID();
            int count = boundedCount(b, ShentongRegistry.MAX_ABILITIES);
            var abilities = new ArrayList<AbilityState>(count);
            for (int i=0; i<count; i++) {
                String id = b.readUtf(96), name = b.readUtf(256); boolean owned = b.readBoolean();
                int fieldCount = boundedCount(b, ShentongRegistry.MAX_FIELDS);
                var fields = new ArrayList<FieldState>(fieldCount);
                for (int f=0; f<fieldCount; f++) {
                    String fieldId = b.readUtf(32), fieldName = b.readUtf(256);
                    int min = b.readVarInt(), max = b.readVarInt(); boolean toggle = b.readBoolean();
                    if (min > max || (toggle && (min != 0 || max != 1)))
                        throw new IllegalArgumentException("Invalid shentong field bounds");
                    fields.add(new FieldState(fieldId,fieldName,min,max,toggle,Math.clamp(b.readVarInt(),min,max)));
                }
                abilities.add(new AbilityState(id,name,owned,fields));
            }
            return new State(target,abilities);
        });
        @Override public Type<State> type() { return TYPE; }
    }
    private static int boundedCount(RegistryFriendlyByteBuf buffer, int max) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) throw new IllegalArgumentException("Invalid shentong list size");
        return count;
    }
    public static State snapshot(ServerPlayer target) {
        return new State(target.getUUID(), ShentongRegistry.all().stream().map(definition ->
                new AbilityState(definition.id(),definition.nameKey(),definition.owned().test(target),
                        definition.fields().stream().map(field -> new FieldState(field.id(),field.nameKey(),
                                field.min(),field.max(),field.toggle(),Math.clamp(field.read().applyAsInt(target),
                                field.min(),field.max()))).toList())).toList());
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (p,c)->c.enqueueWork(()->{
            if (c.player() instanceof ServerPlayer editor) {
                var target = ModPayloads.resolveDebugTarget(editor,p.targetId);
                if (target != null) PacketDistributor.sendToPlayer(editor,snapshot(target));
            }
        }));
        registrar.playToServer(Edit.TYPE, Edit.CODEC, (p,c)->c.enqueueWork(()->{
            if (c.player() instanceof ServerPlayer editor) {
                var target = ModPayloads.resolveDebugTarget(editor,p.targetId);
                if (target != null && DebugShentongEditor.edit(editor,target,p.ability,p.field,p.value))
                    PacketDistributor.sendToPlayer(editor,snapshot(target));
            }
        }));
        registrar.playToClient(State.TYPE, State.CODEC, (p,c)->{
            if (FMLEnvironment.dist.isClient()) c.enqueueWork(()->{
                if (net.minecraft.client.Minecraft.getInstance().screen instanceof
                        com.example.immortal_cultivation_mod.screen.DebugStatEditorScreen screen)
                    screen.acceptShentongState(p);
            });
        });
    }
}
