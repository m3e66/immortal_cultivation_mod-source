package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.flame.FlameKind;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Burn icons reuse the hand-drawn flame artwork and the effect's synced source kind. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FlameBurnIcons implements IClientMobEffectExtensions {
    private static final ResourceLocation[] FLAMES = new ResourceLocation[9];
    private static final ResourceLocation QI_FIRE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/mob_effect/qi_fire_burn.png");

    static {
        for (FlameKind kind : FlameKind.values()) {
            if (kind.special()) FLAMES[kind.modelIndex()] = ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/item/flame_seed_" + kind.name().toLowerCase(Locale.ROOT) + ".png");
        }
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new FlameBurnIcons(), ModEffects.SPIRIT_FLAME_BURN, ModEffects.QI_FIRE_BURN);
    }

    @Override public boolean isVisibleInInventory(MobEffectInstance instance) { return visible(instance); }
    @Override public boolean isVisibleInGui(MobEffectInstance instance) { return visible(instance); }

    private static boolean visible(MobEffectInstance instance) {
        // Special fire also carries an ordinary Qi-fire marker for combat feedback.
        // Its burn already has an icon; a real Qi-draining burn still displays separately.
        if (instance.is(ModEffects.QI_FIRE_BURN) && !instance.showIcon()) return false;
        var player = Minecraft.getInstance().player;
        return !instance.is(ModEffects.QI_FIRE_BURN) || instance.getAmplifier() > 0
                || player == null || !player.hasEffect(ModEffects.SPIRIT_FLAME_BURN);
    }

    @Override public boolean renderInventoryIcon(MobEffectInstance instance,
            EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y, int blitOffset) {
        draw(instance, graphics, x, y + 7, 1);
        return true;
    }

    @Override public boolean renderGuiIcon(MobEffectInstance instance, Gui gui,
            GuiGraphics graphics, int x, int y, float z, float alpha) {
        draw(instance, graphics, x + 3, y + 3, alpha);
        return true;
    }

    private static void draw(MobEffectInstance instance, GuiGraphics graphics, int x, int y, float alpha) {
        ResourceLocation texture = QI_FIRE;
        if (instance.is(ModEffects.SPIRIT_FLAME_BURN)) {
            int index = instance.getAmplifier();
            texture = index > 0 && index < FLAMES.length ? FLAMES[index] : FLAMES[1];
        } else if (instance.getAmplifier() > 0) {
            texture = FLAMES[FlameKind.DRAGON.modelIndex()];
        }
        graphics.setColor(1, 1, 1, alpha);
        graphics.blit(texture, x, y, 18, 18, 0, 0, 128, 128, 128, 128);
        graphics.setColor(1, 1, 1, 1);
    }
}
