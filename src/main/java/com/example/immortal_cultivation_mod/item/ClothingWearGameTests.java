package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class ClothingWearGameTests {
    @GameTest(template = "fate_test")
    public static void clothingUpdatesAtWearBoundariesAndAfterRepair(GameTestHelper helper) {
        for (var item : new Item[]{ModItems.NEIJIA_CHESTPLATE.get(), ModItems.ZHONGYI.get(), ModItems.DAOFU_CHESTPLATE.get(),
                ModItems.DOUPENG.get(), ModItems.SHENYI.get(), ModItems.HECHANG.get(), ModItems.YUNJIAN.get(),
                ModItems.CULTIVATION_FAPAO.get(), ModItems.YUANLING_PAO.get(), ModItems.HUFU.get(), ModItems.MIANSHA.get(),
                ModItems.JIANGPA.get(), ModItems.FUTOU.get(), ModItems.WUSHA_HAT.get(), ModItems.SONG_WUSHA_HAT.get(), ModItems.CHANFU_JIN.get(), ModItems.EMBROIDERED_FOREHEAD_BAND.get(),
                ModItems.VEILED_DOULI.get(), ModItems.FANGKOU_QIAOJIAN_LV.get()}) {
            ItemStack worn = new ItemStack(item);
            ItemStack pristine = new ItemStack(item);
            int maximum = worn.getMaxDamage();
            helper.assertTrue(maximum > 0, "All garments must remain damageable");
            for (int stage = 1; stage <= 3; stage++) {
                int boundary = (maximum * stage + 3) / 4;
                worn.setDamageValue(boundary - 1);
                helper.assertTrue(ClothingWear.stage(worn) == stage - 1,
                        "Wear must not change before a durability threshold");
                worn.setDamageValue(boundary);
                helper.assertTrue(ClothingWear.stage(worn) == stage,
                        "Wear changes exactly at a durability threshold");
                helper.assertTrue(ClothingWear.stage(pristine) == 0,
                        "Two stacks of the same item must not share visual wear");
            }
            worn.setDamageValue(maximum - 1);
            helper.assertTrue(ClothingWear.stage(worn) == 3, "Almost-broken clothing uses the final texture");
            worn.setDamageValue(0);
            helper.assertTrue(ClothingWear.stage(worn) == 0, "Repair immediately restores the new texture");
        }
        helper.assertTrue(ClothingWear.stage(ItemStack.EMPTY) == 0, "Empty render contexts are safe");
        helper.assertTrue(ClothingWear.stage(new ItemStack(Items.STONE)) == 0,
                "Non-damageable stacks cannot divide by zero");
        helper.succeed();
    }
}
