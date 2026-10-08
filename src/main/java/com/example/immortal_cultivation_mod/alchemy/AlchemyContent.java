package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.screen.ModScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
import java.util.*;

@EventBusSubscriber(modid="immortal_cultivation_mod",bus=EventBusSubscriber.Bus.MOD)
public final class AlchemyContent {
    public static final Map<String,DeferredItem<Item>> PILLS=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<Item>> LEGACY_FORMULAS=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<Item>> FORMULAS=new LinkedHashMap<>();
    public static final Map<String,DeferredBlock<AlchemyHerbBlock>> PLANT_BLOCKS=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<Item>> EXTRA_PLANTS=new LinkedHashMap<>();
    public static final DeferredItem<Item> BOTTLE_SLIP=ModItems.ITEMS.register("jade_pill_bottle_yujian",
        ()->new com.example.immortal_cultivation_mod.item.ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOTTLE=ModItems.ITEMS.register("jade_pill_bottle",JadeBottleItem::new);
    public static final DeferredHolder<MenuType<?>,MenuType<AlchemyMenu>> MENU=ModScreens.MENUS.register("alchemy",
        ()->IMenuTypeExtension.create((id,inv,buf)->new AlchemyMenu(id,inv,buf.readBlockPos(),buf.readUtf())));
    public static final DeferredHolder<MenuType<?>,MenuType<JadeBottleMenu>> BOTTLE_MENU=ModScreens.MENUS.register("jade_pill_bottle",
        ()->IMenuTypeExtension.create((id,inv,buf)->new JadeBottleMenu(id,inv,buf.readVarInt())));
    static {
        for(String id:AlchemyForgeRecipes.LEGACY_REQUIRED.keySet())
            LEGACY_FORMULAS.put(id,ModItems.ITEMS.register(id+"_formula",
                    ()->new com.example.immortal_cultivation_mod.item.ForgingYujianItem(new Item.Properties().stacksTo(1))));
        for(var formula:AlchemyChemistry.FORMULAS) {
            String id=formula.id();
            if(!id.equals("huiling"))PILLS.put(id,ModItems.ITEMS.register(id+"_pill",()->new AlchemyPillItem(id)));
            FORMULAS.put(id,ModItems.ITEMS.register(id+"_formula",()->new AlchemyFormulaItem(id)));
        }
        for(String id:List.of("lingquan_leaf","ningling_flower","harmonizing_herb","catalyst_blossom")) {
            var block=com.example.immortal_cultivation_mod.block.ModBlocks.BLOCKS.register(id,()->new AlchemyHerbBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.PLANT).noCollission().noOcclusion()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.GRASS)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
            PLANT_BLOCKS.put(id,block);
            EXTRA_PLANTS.put(id,ModItems.ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
        }
    }
    public static void init() { }
    @SubscribeEvent public static void aliases(RegisterEvent event) {
        if (!event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) return;
        var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        registry.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","medicinal_ash"),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","ashes"));
        for(String old:List.of("low_qi_alchemy_formula","middle_qi_alchemy_formula","high_qi_alchemy_formula"))
            registry.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",old),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","huiling_formula"));
        for(String old:List.of("small_level_up","big_level_up"))
            registry.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",old),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","ashes"));
        var target=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","qi_recovery_pill");
        for(String old:List.of("low_qi_recovery_pill","middle_qi_recovery_pill","high_qi_recovery_pill"))
            registry.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",old),target);
    }
    public static Item pill(String id) {return id.equals("huiling")?ModItems.QI_RECOVERY_PILL.get():PILLS.get(id).get();}
}
