package com.example.immortal_cultivation_mod.magicritual.item;

import com.example.immortal_cultivation_mod.magicritual.item.custom.AmplifierFlagItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.DurationFlagItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.RitualDaggerItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.SpiritMapItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.SpiritSilkwormSilkItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.ZhenpanItem;
import com.example.immortal_cultivation_mod.magicritual.item.custom.ZhenwenJadeItem;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("immortal_cultivation_mod");
   public static final DeferredItem<Item> RITUAL_DAGGER = registerItem("ritual_dagger", () -> new RitualDaggerItem(Tiers.IRON, (new Item.Properties()).durability(250).stacksTo(1)));
   public static final DeferredItem<Item> SPIRIT_STONE = registerItem("spirit_stone", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> MEDIUM_SPIRIT_STONE = registerItem("medium_spirit_stone", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> HIGH_SPIRIT_STONE = registerItem("high_spirit_stone", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.EPIC)));
   public static final DeferredItem<Item> INFINITE_SPIRIT_STONE = registerItem("infinite_spirit_stone", () -> new Item((new Item.Properties()).stacksTo(1).rarity(Rarity.EPIC)));
   public static final DeferredItem<Item> SPIRIT_MAP = registerItem("spirit_map", () -> new SpiritMapItem((new Item.Properties()).stacksTo(1)));
   public static final DeferredItem<Item> ZHENPAN = registerItem("zhenpan", () -> new ZhenpanItem((new Item.Properties()).stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> ZHENWEN_JADE = registerItem("zhenwen_jade", () -> new ZhenwenJadeItem((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> DURATION_FLAG = registerItem("duration_flag", () -> new DurationFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> AMPLIFIER_FLAG = registerItem("amplifier_flag", () -> new AmplifierFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> BLANK_FORMATION_FLAG = registerItem("blank_formation_flag", () -> new DurationFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.UNCOMMON), "blank_formation_flag", "blank_formation_flag", ChatFormatting.GRAY));
   public static final DeferredItem<Item> SPIRIT_GATHERING_FLAG = registerItem("spirit_gathering_flag", () -> new DurationFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.RARE), "spirit_gathering_flag", "spirit_gathering_flag", ChatFormatting.AQUA));
   public static final DeferredItem<Item> HASTE_FLAG = registerItem("haste_flag", () -> new DurationFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.RARE), "haste_flag", "haste_flag", ChatFormatting.YELLOW));
   public static final DeferredItem<Item> QI_RETURN_FLAG = registerItem("qi_return_flag", () -> new DurationFlagItem((new Item.Properties()).stacksTo(1).rarity(Rarity.RARE), "qi_return_flag", "qi_return_flag", ChatFormatting.GREEN));
   public static final DeferredItem<Item> PATTERNED_CLOTH = registerItem("patterned_cloth", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> SPIRIT_SILKWORM_SILK = registerItem("spirit_silkworm_silk", () -> new SpiritSilkwormSilkItem((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> SPIRIT_GUIDING_THREAD = registerItem("spirit_guiding_thread", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> SPIRIT_STORAGE_CRYSTAL = registerItem("spirit_storage_crystal", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> FLOWING_SPIRIT_CRYSTAL = registerItem("flowing_spirit_crystal", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> LINGMU_FLAGPOLE = registerItem("lingmu_flagpole", () -> new Item((new Item.Properties()).stacksTo(64).rarity(Rarity.UNCOMMON)));

   public static boolean isFormationFlag(Item item) {
      return item == DURATION_FLAG.get() || item == AMPLIFIER_FLAG.get() || item == SPIRIT_GATHERING_FLAG.get() || item == HASTE_FLAG.get() || item == QI_RETURN_FLAG.get();
   }

   public static boolean isSpiritStone(Item item) {
      return item == SPIRIT_STONE.get() || item == MEDIUM_SPIRIT_STONE.get() || item == HIGH_SPIRIT_STONE.get() || item == INFINITE_SPIRIT_STONE.get();
   }

   public static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
      return ITEMS.register(name, itemSupplier);
   }
}
