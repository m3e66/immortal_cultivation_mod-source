package com.example.immortal_cultivation_mod.item;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class HigherWeaponRecipes {
    private HigherWeaponRecipes() {}
    public static Item item(String name) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",name));
    }
    public static ForgingSystem.ForgingRecipe recipe(String id) {
        return switch(id) {
            case "jin_hulu" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("liujin_sand"),5,item("jingtie_motherstone"),3,item("qiannian_lingmu_heart"),2,item("spirit_storage_crystal"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "baota" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("jingtie_motherstone"),4,item("liujin_sand"),3,item("yunwen_silver_crystal"),3,item("qiannian_lingmu_heart"),2,item("zhuji_yaodan"),2),false,64,"zhen_fire");
            case "black_iron_jian" -> new ForgingSystem.ForgingRecipe(id,item("sword_mold"),4500,Map.of(item("jingtie_motherstone"),3,item("moxuan_iron"),4,item("yunwen_silver_crystal"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "black_iron_greatsword" -> new ForgingSystem.ForgingRecipe(id,item("sword_mold"),4500,Map.of(item("jingtie_motherstone"),5,item("moxuan_iron"),5,item("liujin_sand"),2,item("zhuji_yaodan"),2),false,64,"zhen_fire");
            case "moon_crescent_halberd" -> new ForgingSystem.ForgingRecipe(id,item("spear_mold"),4500,Map.of(item("jingtie_motherstone"),4,item("moxuan_iron"),4,item("yunwen_silver_crystal"),2,item("qiannian_lingmu_heart"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "spirit_silk_whip" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("spirit_silkworm_silk"),5,item("spirit_guiding_thread"),3,item("jingtie_motherstone"),2,item("ziyun_vine"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "soul_hook" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("moxuan_iron"),4,item("jingtie_motherstone"),3,item("shihun_grass"),2,item("hanpo_jade"),1,item("blood_crystal"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "wind_feather_fan" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("wind_feather"),4,item("qiannian_lingmu_heart"),2,item("yunwen_silver_crystal"),2,item("spirit_guiding_thread"),2,item("zhuji_yaodan"),1),false,64,"zhen_fire");
            case "thunderwood_sword" -> new ForgingSystem.ForgingRecipe(id,item("sword_mold"),4500,Map.of(item("qiannian_lingmu_heart"),4,item("thunder_horn"),2,item("thunder_patterned_rock_plate"),3,item("yunwen_silver_crystal"),2,item("zhuji_yaodan"),2),false,64,"zhen_fire");
            case "flame_pattern_dao" -> new ForgingSystem.ForgingRecipe(id,item("saber_mold"),4500,Map.of(item("moxuan_iron"),4,item("chitong_sui"),4,item("ink_armor_plate"),2,item("jingtie_motherstone"),2,item("zhuji_yaodan"),2),false,64,"zhen_fire");
            case "earthbreaker_hammer" -> new ForgingSystem.ForgingRecipe(id,item("weapon_mold"),4500,Map.of(item("jingtie_motherstone"),5,item("moxuan_iron"),3,item("mineral_marrow"),3,item("profound_iron_armor_core"),1,item("zhuji_yaodan"),2),false,64,"zhen_fire");
            case "seven_star_sword" -> new ForgingSystem.ForgingRecipe(id,item("sword_mold"),6500,Map.of(item("star_dust_stone"),4,item("yunwen_silver_crystal"),3,item("moxuan_iron"),2,item("liujin_sand"),2,item("jindan_yaodan"),2,item("spirit_storage_crystal"),1),false,64,"zhen_fire");
            default -> null;
        };
    }
}
