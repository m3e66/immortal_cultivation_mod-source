package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class TechniqueIcons {
    public static final ResourceLocation UNKNOWN_ICON =
            texture("textures/mob_effect/unknown.png");

    private static final Map<ResourceLocation, IconSize> ICON_SIZES = new HashMap<>();
    private static final Map<String, String> SPELL_EFFECT_ICONS = Map.ofEntries(
            Map.entry("jietian_duoyun_shu", "spirit_seal"),
            Map.entry("bijie_shu", "stabilized"),
            Map.entry("guanyun_shu", "spirit_sight"),
            Map.entry(ModSpells.DINGSHEN, "dishen"),
            Map.entry(ModSpells.SUISHUANG_LINGXIAO, "frost_flight"),
            Map.entry(ModSpells.HANJING_SUOZHUA, "frozen_qi"),
            Map.entry(ModSpells.SHUANGTIAN_QI, "frozen"),
            Map.entry(ModSpells.LIGHT_BEAM_ATTACK, "unknown")
    );

    private TechniqueIcons() {
    }

    public static ResourceLocation spell(ModSpells.SpellDef spell) {
        ResourceLocation dedicated = texture("textures/spell/" + spell.id() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(dedicated).isPresent()) return dedicated;
        String iconName = SPELL_EFFECT_ICONS.getOrDefault(spell.id(), spell.id());
        ResourceLocation icon = texture("textures/mob_effect/" + iconName + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(icon).isPresent()
                ? icon
                : UNKNOWN_ICON;
    }

    public static ResourceLocation method(CultivationMethods.MethodDef method) {
        String element = method.element();
        String textureName;
        if (element.contains(SpiritRoots.METAL)) {
            textureName = "scroll_metal";
        } else if (element.contains(SpiritRoots.WOOD)) {
            textureName = "scroll_wood";
        } else if (element.contains(SpiritRoots.WATER)) {
            textureName = "scroll_water";
        } else if (element.contains(SpiritRoots.FIRE)) {
            textureName = "scroll_fire";
        } else if (element.contains(SpiritRoots.EARTH)) {
            textureName = "scroll_earth";
        } else if (element.contains(SpiritRoots.THUNDER)) {
            textureName = "scroll_thunder";
        } else if (element.contains(SpiritRoots.ICE)) {
            textureName = "scroll_ice";
        } else if (element.contains(SpiritRoots.WIND)) {
            textureName = "scroll_wind";
        } else if (element.contains(SpiritRoots.LIGHT)) {
            textureName = "scroll_light";
        } else if (element.contains(SpiritRoots.DARK)) {
            textureName = "scroll_dark";
        } else {
            textureName = "cultivation_manual";
        }
        return texture("textures/item/" + textureName + ".png");
    }

    public static void blitCentered(
            GuiGraphics graphics,
            ResourceLocation icon,
            int slotX,
            int slotY,
            int slotSize,
            int maxSize) {
        IconSize source = iconSize(icon);
        float scale = Math.min(maxSize / (float) source.width(), maxSize / (float) source.height());
        int drawWidth = Math.max(1, Math.round(source.width() * scale));
        int drawHeight = Math.max(1, Math.round(source.height() * scale));
        int drawX = slotX + (slotSize - drawWidth) / 2;
        int drawY = slotY + (slotSize - drawHeight) / 2;
        graphics.blit(icon, drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F,
                source.width(), source.height(), source.width(), source.height());
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path);
    }

    private static IconSize iconSize(ResourceLocation icon) {
        return ICON_SIZES.computeIfAbsent(icon, location -> Minecraft.getInstance().getResourceManager()
                .getResource(location)
                .map(resource -> {
                    try (var input = resource.open(); NativeImage image = NativeImage.read(input)) {
                        return new IconSize(image.getWidth(), image.getHeight());
                    } catch (Exception ignored) {
                        return new IconSize(16, 16);
                    }
                })
                .orElse(new IconSize(16, 16)));
    }

    private record IconSize(int width, int height) {
    }
}
