package com.example.immortal_cultivation_mod.curse;

import com.example.immortal_cultivation_mod.shentong.ShentongRegistry;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.server.level.ServerPlayer;

/** Explicit debug edits for special supernatural abilities, separate from ordinary spells. */
public final class DebugShentongEditor {
    private DebugShentongEditor() {}

    public static boolean authorized(ServerPlayer editor) {
        return editor.getServer() != null && (editor.getMainHandItem().is(ModItems.DEBUG_STAT_EDITOR.get())
                || editor.getOffhandItem().is(ModItems.DEBUG_STAT_EDITOR.get()));
    }

    public static boolean edit(ServerPlayer editor, ServerPlayer target, String ability, String field, int value) {
        if (!authorized(editor) || target.getServer() != editor.getServer()) return false;
        var definition = ShentongRegistry.get(ability);
        if (definition == null) return false;
        if ("add".equals(field)) definition.grant().accept(target);
        else if ("remove".equals(field)) definition.remove().accept(target);
        else {
            var control = definition.field(field);
            if (control == null || !definition.owned().test(target)) return false;
            control.write().accept(target, Math.clamp(value, control.min(), control.max()));
        }
        definition.sync().accept(target);
        ServerEvents.syncPlayerData(target);
        if (target != editor) ServerEvents.syncPlayerDataTo(target, editor);
        return true;
    }
}
