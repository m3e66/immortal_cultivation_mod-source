package com.example.immortal_cultivation_mod.magicritual.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class DurationFlagItem extends Item implements GeoItem {
   public static final int TEMPORARY_DURATION_BONUS_TICKS = 60;
   private final String textureName;
   private final String tooltipName;
   private final ChatFormatting tooltipColor;
   private final AnimatableInstanceCache cache;

   public DurationFlagItem(Item.Properties properties) {
      this(properties, "duration_flag", "duration_flag", ChatFormatting.GOLD);
   }

   public DurationFlagItem(Item.Properties properties, String textureName, String tooltipName, ChatFormatting tooltipColor) {
      super(properties);
      this.cache = GeckoLibUtil.createInstanceCache(this);
      this.textureName = textureName;
      this.tooltipName = tooltipName;
      this.tooltipColor = tooltipColor;
   }

   public String textureName() {
      return this.textureName;
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod." + this.tooltipName).withStyle(this.tooltipColor));
      super.appendHoverText(stack, context, tooltip, flag);
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private DurationFlagItemRenderer renderer;

         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new DurationFlagItemRenderer();
            }

            return this.renderer;
         }
      });
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
