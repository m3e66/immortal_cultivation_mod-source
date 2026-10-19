package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.config.HudElement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Compact player-health fallback; AsteorBar keeps complete ownership when installed. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class HealthBarOverlay {
    public static final int WIDTH = 81, HEIGHT = 18;
    private static final boolean ENABLED = !ModList.get().isLoaded("asteorbar");
    private static float lastHealth = Float.NaN, lastMaximum = Float.NaN, lastAbsorption = Float.NaN;
    private static Component cachedLabel = Component.empty();

    private HealthBarOverlay() {}

    public static boolean enabled() { return ENABLED; }

    /** Match vanilla's damageable-player HUD, including F1 and spectator visibility. */
    public static boolean visible() {
        var mc = Minecraft.getInstance();
        return ENABLED && mc.player != null && mc.gameMode != null && !mc.options.hideGui
                && !mc.player.isSpectator() && mc.gameMode.canHurtPlayer()
                && mc.getCameraEntity() == mc.player;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void healthLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || !visible()) return;
        var mc = Minecraft.getInstance();
        event.setCanceled(true);
        render(event.getGuiGraphics(), mc);
        // Replacing stacked hearts must still reserve a bounded row for armor.
        mc.gui.leftHeight += 20;
    }

    private static float safe(float value) {
        return Float.isFinite(value) ? Math.max(0, value) : 0;
    }

    static String number(float value) {
        // Round tiny positive HP upward instead of showing a living player as 0.
        return BigDecimal.valueOf(safe(value)).setScale(1, RoundingMode.CEILING).stripTrailingZeros().toPlainString();
    }

    private static Component label(float health, float maximum, float absorption) {
        if (health != lastHealth || maximum != lastMaximum || absorption != lastAbsorption) {
            var value = Component.translatable("hud.immortal_cultivation_mod.health", number(health), number(maximum));
            if (absorption > 0) value.append(Component.literal(" +" + number(absorption)).withStyle(ChatFormatting.GOLD));
            cachedLabel = value;
            lastHealth = health; lastMaximum = maximum; lastAbsorption = absorption;
        }
        return cachedLabel;
    }

    private static void render(GuiGraphics graphics, Minecraft mc) {
        float health = safe(mc.player.getHealth()), maximum = safe(mc.player.getMaxHealth());
        float absorption = safe(mc.player.getAbsorptionAmount());
        float ratio = maximum > 0 ? Mth.clamp(health / maximum, 0, 1) : 0;
        var position = HudLayout.place(graphics, HudElement.HEALTH, graphics.guiWidth() / 2 - 91,
                graphics.guiHeight() - mc.gui.leftHeight - 11, WIDTH, HEIGHT);
        int x = position.x(), y = position.y();
        graphics.fill(x, y + 10, x + WIDTH, y + 17, 0xCC17191D);
        graphics.fill(x + 1, y + 11, x + WIDTH - 1, y + 16, 0xFF492028);
        int fill = Math.round((WIDTH - 2) * ratio);
        if (fill > 0) graphics.fill(x + 1, y + 11, x + 1 + fill, y + 16,
                ratio <= .25F ? 0xFFED4B42 : 0xFFC83C4A);
        float cursed=com.example.immortal_cultivation_mod.curse.AnnihilationCurse.blackHp(mc.player);
        if(cursed>0&&maximum>0) {
            int normal=Math.round((WIDTH-2)*Math.max(0,health-cursed)/maximum);
            if(fill>normal){graphics.fill(x+1+normal,y+11,x+1+fill,y+16,0xFF0B080E);
                graphics.fill(x+1+normal,y+11,x+1+fill,y+12,0xFF51404E);}
        }
        if (absorption > 0) {
            int gold = Math.max(1, Math.round((WIDTH - 2) * absorption / Math.max(1, maximum + absorption)));
            graphics.fill(x + 1, y + 14, x + 1 + gold, y + 16, 0xFFFFCE55);
        }
        Component text = label(health, maximum, absorption);
        float scale = Math.min(1, WIDTH / (float) Math.max(1, mc.font.width(text)));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + WIDTH / 2F, y, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawCenteredString(mc.font, text, 0, 0, 0xFFFFE5E5);
        } finally {
            graphics.pose().popPose();
        }
    }
}
