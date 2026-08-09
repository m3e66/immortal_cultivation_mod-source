package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final Holder<ArmorMaterial> DAOFU = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "daofu"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 0);
                        defense.put(ArmorItem.Type.LEGGINGS, 0);
                        defense.put(ArmorItem.Type.CHESTPLATE, 3);
                        defense.put(ArmorItem.Type.HELMET, 0);
                        defense.put(ArmorItem.Type.BODY, 3);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "daofu"))),
                    0.0F,
                    0.0F
            )
    );

    public static final Holder<ArmorMaterial> NEIJIA = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "neijia"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 0);
                        defense.put(ArmorItem.Type.LEGGINGS, 0);
                        defense.put(ArmorItem.Type.CHESTPLATE, 5);
                        defense.put(ArmorItem.Type.HELMET, 0);
                        defense.put(ArmorItem.Type.BODY, 5);
                    }),
                    18,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, "neijia"))),
                    1.0F,
                    0.0F
            )
    );

    public static final Holder<ArmorMaterial> ZHONGYI = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "zhongyi"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 0);
                        defense.put(ArmorItem.Type.LEGGINGS, 0);
                        defense.put(ArmorItem.Type.CHESTPLATE, 2);
                        defense.put(ArmorItem.Type.HELMET, 0);
                        defense.put(ArmorItem.Type.BODY, 2);
                    }),
                    12,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, "zhongyi"))),
                    0.0F,
                    0.0F
            )
    );

    public static final Holder<ArmorMaterial> HEAVY_IRON_BRACER_DEBUG = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "heavy_iron_bracer_debug"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 0);
                        defense.put(ArmorItem.Type.LEGGINGS, 0);
                        defense.put(ArmorItem.Type.CHESTPLATE, 0);
                        defense.put(ArmorItem.Type.HELMET, 0);
                        defense.put(ArmorItem.Type.BODY, 0);
                    }),
                    0,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID, "heavy_iron_bracer_debug"))),
                    0.0F,
                    0.0F
            )
    );

    private ModArmorMaterials() {
    }
}
