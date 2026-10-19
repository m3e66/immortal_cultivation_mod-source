package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.PhantomVeilFurItem;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class BeastRealmEvents {
    private BeastRealmEvents() { }
    // Foxes and guardians scale their attributes and technique payloads at the source.
    // Other beasts retain reference damage values, scaled here once for every attack path.
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof Mob mob
                && mob instanceof RealmBeast && !(mob instanceof PhantomVeilFoxEntity)
                && !(mob instanceof VaultGuardianEntity)) {
            event.setAmount((float)(event.getAmount() * BeastRealms.powerScale(mob)));
        }
    }
    /** Realms choose the existing ingredient tier, preserving recipes and forging compatibility. */
    public static ItemStack realmDrop(ItemStack stack, int realm) {
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(ImmortalCultivationMod.MODID)) return stack;
        realm = Math.clamp(realm, 0, 9);
        if (isCore(stack) || isMeat(stack)) {
            if (realm == 0 && isCore(stack)) return ItemStack.EMPTY;
            Item item = isCore(stack) ? cores().get(realm - 1) : meat().get(realm);
            var result = stack.transmuteCopy(item, stack.getCount());
            result.remove(DataComponents.CUSTOM_NAME);
            return result;
        }
        if (stack.is(ModItems.PHANTOM_VEIL_FUR.get())) {
            var result = stack.copy();
            PhantomVeilFurItem.setRealm(result, BeastRealms.level(realm));
            return result;
        }
        if (stack.getItem() instanceof com.example.immortal_cultivation_mod.item.BeastMaterialItem
                || stack.is(ModItems.YAOYUAN_FIST_BONE.get())) {
            var result = stack.copy();
            var tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(BeastRealms.NBT_KEY, realm);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return result;
        }
        return stack;
    }
    public static boolean isCore(ItemStack stack) { return cores().contains(stack.getItem()); }
    public static boolean isMeat(ItemStack stack) { return meat().contains(stack.getItem()); }

    private static List<Item> cores() {
        return List.of(ModItems.LIANQI_YAODAN.get(), ModItems.ZHUJI_YAODAN.get(), ModItems.JINDAN_YAODAN.get(),
                ModItems.YUANYING_YAODAN.get(), ModItems.HUASHEN_YAODAN.get(), ModItems.LIANXU_YAODAN.get(),
                ModItems.HETI_YAODAN.get(), ModItems.DACHENG_YAODAN.get(), ModItems.DUJIE_YAODAN.get());
    }
    private static List<Item> meat() {
        return List.of(ModItems.MORTAL_BEAST_MEAT.get(), ModItems.LIANQI_BEAST_MEAT.get(), ModItems.ZHUJI_BEAST_MEAT.get(), ModItems.JINDAN_BEAST_MEAT.get(),
                ModItems.YUANYING_BEAST_MEAT.get(), ModItems.HUASHEN_BEAST_MEAT.get(), ModItems.LIANXU_BEAST_MEAT.get(),
                ModItems.HETI_BEAST_MEAT.get(), ModItems.DACHENG_BEAST_MEAT.get(), ModItems.DUJIE_BEAST_MEAT.get());
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        int realm;
        if (event.getEntity() instanceof RealmBeast beast) realm = beast.beastRealm();
        else if (event.getEntity() instanceof EscapedYuanyingCoreEntity core) realm = core.coreRealm();
        else return;
        var iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            var entity = iterator.next();
            ItemStack stack = realmDrop(entity.getItem(), realm);
            if (stack.isEmpty()) iterator.remove();
            else entity.setItem(stack);
        }
    }
}
