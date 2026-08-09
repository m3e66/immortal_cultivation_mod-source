package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
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
    public static final int TEMPORARY_DURATION_BONUS_TICKS = 20 * 3;

    private final String textureName;
    private final String tooltipName;
    private final ChatFormatting tooltipColor;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DurationFlagItem(Properties properties) {
        this(properties, "duration_flag", "duration_flag", ChatFormatting.GOLD);
    }

    public DurationFlagItem(Properties properties, String textureName,
                            String tooltipName, ChatFormatting tooltipColor) {
        super(properties);
        this.textureName = textureName;
        this.tooltipName = tooltipName;
        this.tooltipColor = tooltipColor;
    }

    public String textureName() {
        return textureName;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip." + ImmortalCultivationMod.MODID + "." + tooltipName)
                .withStyle(tooltipColor));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DurationFlagItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new DurationFlagItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
