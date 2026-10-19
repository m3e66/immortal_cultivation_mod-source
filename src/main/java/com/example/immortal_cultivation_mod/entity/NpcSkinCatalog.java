package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Shared catalog of humanoid NPC skins that can also be worn as disguises. */
public final class NpcSkinCatalog {
    public static final String MALE = "male";
    public static final String FEMALE = "female";

    public static final List<Skin> MORTAL_SKINS = List.of(
            new Skin("fanren_npc", MALE),
            new Skin("fanren2", MALE));

    public static final List<Skin> CULTIVATOR_SKINS = List.of(
            new Skin("zhuji1", FEMALE),
            new Skin("zhuji2", FEMALE),
            new Skin("zhuji3", MALE),
            new Skin("zhuji4", FEMALE),
            new Skin("jindan1", MALE),
            new Skin("jindan2", MALE),
            new Skin("jindan3", FEMALE),
            new Skin("jindan4", FEMALE),
            new Skin("ink_robed_cultivator", MALE),
            new Skin("rose_robed_cultivator", FEMALE),
            new Skin("scarlet_robed_cultivator", FEMALE),
            new Skin("wine_robed_cultivator", FEMALE),
            new Skin("violet_robed_cultivator", MALE),
            new Skin("frost_robed_cultivator", MALE));

    public static final List<Skin> NAMED_SKINS = List.of(
            new Skin("hotelkeeper", MALE),
            new Skin("recipe_merchant", MALE));

    public static final List<Skin> ALL_SKINS;
    private static final Map<String, Skin> BY_ID;

    static {
        List<Skin> skins = new ArrayList<>();
        skins.addAll(MORTAL_SKINS);
        skins.addAll(CULTIVATOR_SKINS);
        skins.addAll(NAMED_SKINS);
        ALL_SKINS = List.copyOf(skins);

        Map<String, Skin> byId = new LinkedHashMap<>();
        for (Skin skin : ALL_SKINS) {
            byId.put(skin.texture(), skin);
        }
        BY_ID = Map.copyOf(byId);
    }

    private NpcSkinCatalog() {
    }

    public static boolean contains(String skinId) {
        return skinId != null && BY_ID.containsKey(skinId);
    }

    public static String randomSkinId(RandomSource random) {
        return ALL_SKINS.get(random.nextInt(ALL_SKINS.size())).texture();
    }

    public static ResourceLocation texture(String skinId) {
        if (!contains(skinId)) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/" + skinId + ".png");
    }

    public record Skin(String texture, String gender) {
    }
}
