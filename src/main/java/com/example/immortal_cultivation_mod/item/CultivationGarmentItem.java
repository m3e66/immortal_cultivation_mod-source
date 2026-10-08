package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Repairable clothing worn in the existing custom accessory slots. */
public final class CultivationGarmentItem extends Item implements GeoItem, ICurioItem {
    private final String model;
    private final String slot;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CultivationGarmentItem(String model, String slot) {
        super(new Item.Properties().durability(switch (slot) {
            case "head_accessory" -> 600;
            case "boots" -> 900;
            default -> 1800;
        }));
        this.model = model;
        this.slot = slot;
    }

    public String model() { return model; }
    public String slot() { return slot; }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(net.minecraft.tags.ItemTags.WOOL)
                || ingredient.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "wool_strips")))
                || (slot.equals("boots") && ingredient.is(net.minecraft.world.item.Items.LEATHER))
                || super.isValidRepairItem(stack, ingredient);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return slot.equals(context.identifier());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
