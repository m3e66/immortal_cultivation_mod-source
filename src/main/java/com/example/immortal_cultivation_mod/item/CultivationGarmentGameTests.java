package com.example.immortal_cultivation_mod.item;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class CultivationGarmentGameTests {
    private static void armorHit(FakePlayer player) {
        // FakePlayer deliberately rejects all damage; deliver the armor event produced by a hit.
        var entries = new java.util.EnumMap<net.minecraft.world.entity.EquipmentSlot,
                net.neoforged.neoforge.event.entity.living.ArmorHurtEvent.ArmorEntry>(net.minecraft.world.entity.EquipmentSlot.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.entity.living.ArmorHurtEvent(entries, player, player.damageSources().playerAttack(player)));
    }

    @GameTest(template = "fate_test")
    public static void garmentsWearInLayerOrderAndBreak(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "garment-wear"));
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getAbilities().instabuild = false;
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            inventory.setEquippedCurio("cloak", 0, new ItemStack(ModItems.DOUPENG.get()));
            inventory.setEquippedCurio("outer_clothes", 0, new ItemStack(ModItems.CULTIVATION_FAPAO.get()));
            inventory.setEquippedCurio("inner_clothes", 0, new ItemStack(ModItems.HUFU.get()));
            inventory.setEquippedCurio("head_accessory", 0, new ItemStack(ModItems.JIANGPA.get()));
            inventory.setEquippedCurio("boots", 0, new ItemStack(ModItems.FANGKOU_QIAOJIAN_LV.get()));
            helper.assertTrue(inventory.getStacksHandler("cloak").orElseThrow().getStacks()
                    .getStackInSlot(0).is(ModItems.DOUPENG.get()), "Test cloak is equipped");
            armorHit(player);
            for (String slot : new String[]{"cloak", "head_accessory", "boots"}) {
                helper.assertTrue(inventory.getStacksHandler(slot).orElseThrow().getStacks()
                        .getStackInSlot(0).getDamageValue() > 0, "Exposed garment takes hit wear: " + slot);
            }
            helper.assertTrue(inventory.getStacksHandler("outer_clothes").orElseThrow().getStacks()
                    .getStackInSlot(0).getDamageValue() == 0, "Cloak protects outer clothing from wear");
            var cloak = inventory.getStacksHandler("cloak").orElseThrow().getStacks().getStackInSlot(0).copy();
            cloak.setDamageValue(cloak.getMaxDamage() - 1);
            inventory.setEquippedCurio("cloak", 0, cloak);
            armorHit(player);
            helper.assertTrue(inventory.getStacksHandler("cloak").orElseThrow().getStacks()
                    .getStackInSlot(0).isEmpty(), "Exhausted cloak breaks and leaves slot");
            armorHit(player);
            helper.assertTrue(inventory.getStacksHandler("outer_clothes").orElseThrow().getStacks()
                    .getStackInSlot(0).getDamageValue() > 0, "Outer clothing wears after cloak breaks");
            helper.assertTrue(inventory.getStacksHandler("inner_clothes").orElseThrow().getStacks()
                    .getStackInSlot(0).getDamageValue() == 0, "Outer clothing protects inner layer");
            int wear = inventory.getStacksHandler("outer_clothes").orElseThrow().getStacks().getStackInSlot(0).getDamageValue();
            player.getAbilities().instabuild = true;
            armorHit(player);
            helper.assertTrue(inventory.getStacksHandler("outer_clothes").orElseThrow().getStacks()
                    .getStackInSlot(0).getDamageValue() == wear, "Creative mode does not wear clothing");
        } finally { player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void garmentsFitCustomSlotsAndSurviveSave(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "garment-test"));
        try {
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            for (Item item : new Item[]{ModItems.DOUPENG.get(), ModItems.SHENYI.get(),
                    ModItems.HECHANG.get(), ModItems.YUNJIAN.get(), ModItems.CULTIVATION_FAPAO.get(), ModItems.YUANLING_PAO.get(),
                    ModItems.HUFU.get(), ModItems.MIANSHA.get(), ModItems.JIANGPA.get(), ModItems.FUTOU.get(), ModItems.WUSHA_HAT.get(), ModItems.SONG_WUSHA_HAT.get(), ModItems.CHANFU_JIN.get(), ModItems.EMBROIDERED_FOREHEAD_BAND.get(), ModItems.VEILED_DOULI.get(), ModItems.FANGKOU_QIAOJIAN_LV.get()}) {
                var garment = (CultivationGarmentItem)item;
                var stack = new ItemStack(item);
                helper.assertTrue(stack.isDamageableItem(), "Every garment has durability");
                stack.setDamageValue(23);
                helper.assertTrue(garment.isValidRepairItem(stack, new ItemStack(net.minecraft.world.item.Items.WHITE_WOOL)),
                        "Wool repairs worn clothing");
                var slot = inventory.getStacksHandler(garment.slot()).orElseThrow();
                helper.assertTrue(slot.getSlots() > 0, "Garment slot exists on players");
                helper.assertTrue(stack.is(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("curios", garment.slot()))),
                        "Garment belongs to its Curios validator tag");
                for (String name : new String[]{"cloak", "inner_clothes", "outer_clothes", "head_accessory", "hair", "ring", "boots"}) {
                    helper.assertTrue(garment.canEquip(new SlotContext(name, player, 0, false, true), stack)
                                    == garment.slot().equals(name), "Only the intended slot accepts the garment");
                }
                slot.getStacks().setStackInSlot(0, stack);
                var saved = inventory.writeTag();
                slot.getStacks().setStackInSlot(0, ItemStack.EMPTY);
                inventory.readTag(saved);
                // Curios applies pending deserialization when its capability is fetched again.
                inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
                helper.assertTrue(inventory.getStacksHandler(garment.slot()).orElseThrow()
                        .getStacks().getStackInSlot(0).is(item), "Equipped garment survives Curios serialization");
                helper.assertTrue(inventory.getStacksHandler(garment.slot()).orElseThrow()
                        .getStacks().getStackInSlot(0).getDamageValue() == 23,
                        "Garment wear survives Curios serialization");
            }
        } finally { player.discard(); }
        helper.succeed();
    }
}
