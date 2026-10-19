package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.item.*;
import java.util.*;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Alchemy outputs adapt to the existing ding recipe, heat, purity, and snapshot pipeline. */
public final class AlchemyForgeRecipes {
    public static final Map<String,Map<String,Integer>> LEGACY_REQUIRED = new LinkedHashMap<>();
    static {
        LEGACY_REQUIRED.put("bigu",Map.of("minecraft:wheat",3,"lingquan_leaf",1,"qingling_grass",1));
        LEGACY_REQUIRED.put("body_tempering",Map.of("ziyun_vine",3,"harmonizing_herb",2,"catalyst_blossom",1));
        LEGACY_REQUIRED.put("blood_qi",Map.of("chiyan_flower",2,"ziyun_vine",2,"ninglu_zhi",2,"blood",1));
        LEGACY_REQUIRED.put("enlightenment",Map.of("enlightenment_leaf",1,"jiuye_xianlan",2,"ningling_flower",2,"harmonizing_herb",1));
        LEGACY_REQUIRED.put("breakthrough",Map.of("jiuye_xianlan",3,"xuanbing_lotus",2,"ningling_flower",2,"catalyst_blossom",1));
    }
    private AlchemyForgeRecipes() {}
    public static boolean isPill(String id){return id.startsWith("alchemy/");}
    /** Legacy and current pills share alchemy skill; the bottle remains a forged vessel. */
    public static boolean isAlchemyRecipe(String id) {
        return id != null && (id.startsWith("alchemy/") || id.startsWith("legacy_alchemy/"));
    }
    public static String proficiencyKey(String recipeId) {
        return isAlchemyRecipe(recipeId) ? FateAlchemyFormulaItem.PROFICIENCY : IdentityItems.FORGING_PROFICIENCY;
    }
    public static void recordSuccessfulBatch(ServerPlayer player, String recipeId) {
        if (isAlchemyRecipe(recipeId)) {
            ModAttachments.setData(player, ModAttachments.getData(player)
                    .withAddedMethodProficiency(FateAlchemyFormulaItem.PROFICIENCY, 1));
            ServerEvents.syncPlayerData(player);
        } else {
            IdentityItems.recordSuccessfulForging(player);
        }
    }
    public static ForgingSystem.ForgingRecipe recipe(String id){
        if(id.startsWith("legacy_alchemy/")) {
            String name=id.substring("legacy_alchemy/".length());
            var ingredients=LEGACY_REQUIRED.get(name);
            if(ingredients==null)return null;
            Map<Item,Integer> required=new LinkedHashMap<>();
            ingredients.forEach((key,count)->required.put(BuiltInRegistries.ITEM.get(
                    key.contains(":")?ResourceLocation.parse(key):ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",key)),count));
            Item output=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",name+"_pill"));
            return new ForgingSystem.ForgingRecipe(id,output,1200,required,true,64,"normal_fire");
        }
        if(id.equals("jade_pill_bottle"))return new ForgingSystem.ForgingRecipe(id,AlchemyContent.BOTTLE.get(),1200,
                Map.of(Items.GLASS_BOTTLE,1,Items.EMERALD,1),true,64,"normal_fire");
        if(!isPill(id))return null;
        String key=id.substring(8);
        if(AlchemyChemistry.FORMULAS.stream().noneMatch(f->f.id().equals(key)))return null;
        var formula=AlchemyChemistry.formula(key);Map<Item,Integer> required=new LinkedHashMap<>();
        formula.required().forEach((name,count)->required.put(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",name)),count));
        return new ForgingSystem.ForgingRecipe(id,AlchemyContent.pill(key),1200,required,true,64,"normal_fire");
    }
    public static Map<String,Integer> herbs(List<ItemStack> materials){
        Map<String,Integer> counts=new LinkedHashMap<>();
        for(var stack:materials){String id=AlchemyMenu.plantId(stack);if(id!=null)counts.merge(id,ForgingSystem.canStackInForgingRecipe(stack)?stack.getCount():1,Integer::sum);}
        return counts;
    }
    public static ItemStack output(String id,List<ItemStack> materials){
        return PillData.create(AlchemyChemistry.brew(AlchemyChemistry.formula(id.substring(8)),herbs(materials)));
    }
}
