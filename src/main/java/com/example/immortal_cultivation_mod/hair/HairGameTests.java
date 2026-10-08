package com.example.immortal_cultivation_mod.hair;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import java.util.UUID;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class HairGameTests {
    @GameTest(template = "fate_test")
    public static void tablePlacementFacesPlayerAndRotatesMirrorHitbox(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "table-facing-test"));
        var support = h.absolutePos(new BlockPos(2, 1, 2));
        var target = support.above();
        h.getLevel().setBlockAndUpdate(support, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for (var looking : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            p.setYRot(looking.toYRot());
            p.setPos(target.getX() + .5 - 2 * looking.getStepX(), target.getY(),
                    target.getZ() + .5 - 2 * looking.getStepZ());
            h.getLevel().setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            var stack = new ItemStack(HairContent.TABLE_ITEM.get());
            var context = new net.minecraft.world.item.context.BlockPlaceContext(p,
                    net.minecraft.world.InteractionHand.MAIN_HAND, stack,
                    new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(support).add(0, .5, 0),
                            net.minecraft.core.Direction.UP, support, false));
            h.assertTrue(HairContent.TABLE_ITEM.get().place(context).consumesAction(), "Table item places successfully");
            var state = h.getLevel().getBlockState(target);
            var facing = state.getValue(HairStylingTableBlock.FACING);
            h.assertTrue(facing == looking.getOpposite(),
                    "Mirror and drawers face the player standing on the " + looking.getOpposite() + " side");
            var shape = state.getShape(h.getLevel(), target);
            h.assertTrue(shape.clip(new net.minecraft.world.phys.Vec3(target.getX() + .5, target.getY() + .25, target.getZ() - 1),
                            new net.minecraft.world.phys.Vec3(target.getX() + .5, target.getY() + .25, target.getZ() + 2), target) == null,
                    "The center below the tabletop stays open");
            var mirror = shape.toAabbs().stream()
                    .filter(box -> box.maxY > 1.4).findFirst().orElseThrow();
            var center = mirror.getCenter().subtract(.5, 0, .5);
            double behind = center.x * looking.getStepX() + center.z * looking.getStepZ();
            h.assertTrue(behind > .3,
                    "Mirror sits on the far edge, leaving the tabletop between player and mirror: " + looking);
        }
        p.discard();
        h.succeed();
    }
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"hair-test"));
        BlockPos pos=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(pos,HairContent.TABLE.get().defaultBlockState());
        p.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);p.containerMenu=new HairMenu(7,p.getInventory(),pos);return p;
    }
    @GameTest(template="fate_test") public static void emptySlotAndRepeatedStylingNeverCreateSpareItems(GameTestHelper h) {
        var p=player(h);var m=(HairMenu)p.containerMenu;
        for(int i=0;i<80;i++)h.assertTrue(m.apply(p,i%20,0x3C2417,1,false),"Styling succeeds from an empty or occupied hair slot");
        h.assertTrue(HairData.style(HairData.equipped(p,"hair"))==19,"Latest selected style is equipped");
        h.assertTrue(p.getInventory().items.stream().allMatch(ItemStack::isEmpty),"No discarded styles appear in inventory");
        h.assertTrue(HairData.color(HairData.equipped(p,"hair"))==0x3C2417,"Exact RGB is preserved");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void accessoriesRemainIndependentAndAreNotGeneratedByTheTable(GameTestHelper h) {
        var p=player(h);var m=(HairMenu)p.containerMenu;
        p.getInventory().setItem(0,new ItemStack(HairContent.ACCESSORIES.get(0).get()));
        h.assertTrue(m.apply(p,2,0x123456,1,false),"Hair can be styled independently");
        h.assertTrue(p.getInventory().getItem(0).getCount()==1&&HairData.equipped(p,"hair_accessory").isEmpty(),"The table never consumes or equips inventory accessories");
        HairData.slot(p,"hair_accessory").setStackInSlot(0,p.getInventory().removeItem(0,1));
        h.assertTrue(m.apply(p,-1,0x123456,1,false),"Hair removal succeeds");
        h.assertTrue(HairData.equipped(p,"hair").isEmpty() && HairData.accessory(HairData.equipped(p,"hair_accessory"))==0,"Removing hair preserves the ornament");
        h.assertTrue(m.apply(p,19,0x123456,1,false),"Styling preserves the manually equipped ornament");
        h.assertTrue(HairData.accessory(HairData.equipped(p,"hair_accessory"))==0,"Table cannot change the accessory slot");
        p.getInventory().setItem(0,HairData.slot(p,"hair_accessory").extractItem(0,1,false));
        h.assertTrue(HairData.style(HairData.equipped(p,"hair"))==19 && HairData.equipped(p,"hair_accessory").isEmpty(),"Hair remains when ornament is removed");
        h.assertTrue(p.getInventory().items.stream().filter(s->HairData.accessory(s)==0).mapToInt(ItemStack::getCount).sum()==1,"Ornament is returned exactly once");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void invalidDraftsAndDistantTablesCannotChangeEquipment(GameTestHelper h) {
        var p=player(h);var m=(HairMenu)p.containerMenu;
        h.assertTrue(!m.apply(p,20,0,0,false)&&!m.apply(p,-2,0,0,false)&&!m.apply(p,0,0x1000000,0,false),"Invalid styles and colors are rejected");
        p.setPos(p.getX()+20,p.getY(),p.getZ());
        h.assertTrue(!m.apply(p,0,0xABCDEF,0,false) && HairData.equipped(p,"hair").isEmpty(),"Out-of-range actions do not create equipment");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void itemAndCuriosPersistenceKeepIndependentColors(GameTestHelper h) {
        var p=player(h);var q=player(h);
        ((HairMenu)p.containerMenu).apply(p,10,0x1C1210,0,true);
        ((HairMenu)q.containerMenu).apply(q,10,0xEFE5C3,2,true);
        var registry=h.getLevel().registryAccess();
        ItemStack restored=ItemStack.parseOptional(registry,(net.minecraft.nbt.CompoundTag)HairData.equipped(p,"hair").save(registry));
        h.assertTrue(HairData.color(restored)==0x1C1210 && HairData.helmet(restored)==0,"Item components survive serialization");
        var inventory=CuriosApi.getCuriosInventory(p).orElseThrow();var saved=inventory.writeTag();
        HairData.slot(p,"hair").setStackInSlot(0,ItemStack.EMPTY);inventory.readTag(saved);
        h.assertTrue(HairData.color(HairData.equipped(p,"hair"))==0x1C1210 && HairData.color(HairData.equipped(q,"hair"))==0xEFE5C3,"Same style keeps per-player color through Curios save/load");
        h.assertTrue(HairData.colors(p,true)[0]==0x1C1210,"Saved palette remains server-owned");
        p.discard();q.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void itemsOnlyFitTheirDedicatedCuriosSlots(GameTestHelper h) {
        var p=player(h);var hair=HairContent.HAIR.get(0).get();var ornament=HairContent.ACCESSORIES.get(0).get();
        var head=new SlotContext("head_accessory",p,0,false,true);var hs=new SlotContext("hair",p,0,false,true);var as=new SlotContext("hair_accessory",p,0,false,true);
        h.assertTrue(hair.canEquip(hs,new ItemStack(hair))&&!hair.canEquip(head,new ItemStack(hair))&&!hair.canEquip(as,new ItemStack(hair)),"Hair only fits the hair slot");
        h.assertTrue(ornament.canEquip(as,new ItemStack(ornament))&&!ornament.canEquip(hs,new ItemStack(ornament)),"Ornaments only fit the accessory slot");
        p.discard();h.succeed();
    }
    @GameTest(template="fate_test") public static void pixelPickerReadsExactFaceCoordinatesAndColorChannels(GameTestHelper h) {
        int[] data=new int[4096];java.util.Arrays.fill(data,0xFF010203);
        data[8*64+8]=0xFF3C2417;data[8*64+24]=0xFFABCDEF;data[8*64+48]=0xFF123456;data[40]=0xFF112233;
        var pixels=new SkinPixels(64,64,data);
        h.assertTrue(pixels.head(0,false,0,0)==0xFF3C2417 && pixels.head(1,false,0,0)==0xFFABCDEF,"Front and back use exact source pixels");
        h.assertTrue(pixels.head(2,true,0,0)==0xFF123456 && pixels.head(4,true,0,0)==0xFF112233,"Outer left and top use the correct atlas regions");
        h.assertTrue(SkinPixels.fromAbgr(0xFF17243C)==0xFF3C2417,"Native-image ABGR is converted without swapping red and blue");
        h.succeed();
    }
}
