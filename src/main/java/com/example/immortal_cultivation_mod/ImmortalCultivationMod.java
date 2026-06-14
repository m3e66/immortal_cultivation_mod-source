package com.example.immortal_cultivation_mod;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.ModTabs;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.sound.ModSounds;
import com.example.immortal_cultivation_mod.screen.ModScreens;
import java.lang.reflect.Field;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import sun.misc.Unsafe;

@Mod(ImmortalCultivationMod.MODID)
public class ImmortalCultivationMod {
    public static final String MODID = "immortal_cultivation_mod";
    public static final double MAX_CULTIVATION_HEALTH = 1_000_000_000.0D;
    private static volatile boolean maxHealthAttributeLimitRaised;

    public ImmortalCultivationMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        ModAttachments.register(modEventBus);
        ModEffects.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModScreens.MENUS.register(modEventBus);
        ModTabs.CREATIVE_TABS.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ImmortalCultivationMod::raiseMaxHealthAttributeLimit);
    }

    public static void raiseMaxHealthAttributeLimit() {
        if (!(Attributes.MAX_HEALTH.value() instanceof RangedAttribute maxHealth)) {
            return;
        }
        if (maxHealthAttributeLimitRaised && maxHealth.sanitizeValue(MAX_CULTIVATION_HEALTH) >= MAX_CULTIVATION_HEALTH) {
            return;
        }

        try {
            Field maxValue = findMaxValueField(maxHealth);
            writeFinalDouble(maxHealth, maxValue, MAX_CULTIVATION_HEALTH);
            if (maxHealth.sanitizeValue(MAX_CULTIVATION_HEALTH) < MAX_CULTIVATION_HEALTH) {
                throw new IllegalStateException("Max health attribute still capped at " + maxHealth.sanitizeValue(MAX_CULTIVATION_HEALTH));
            }
            maxHealthAttributeLimitRaised = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to raise max health attribute limit", exception);
        }
    }

    private static Field findMaxValueField(RangedAttribute attribute) throws ReflectiveOperationException {
        try {
            Field named = RangedAttribute.class.getDeclaredField("maxValue");
            named.setAccessible(true);
            return named;
        } catch (NoSuchFieldException ignored) {
            Field best = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (Field field : RangedAttribute.class.getDeclaredFields()) {
                if (field.getType() != double.class) {
                    continue;
                }
                field.setAccessible(true);
                double value = field.getDouble(attribute);
                if (value > bestValue) {
                    best = field;
                    bestValue = value;
                }
            }
            if (best == null) {
                throw new NoSuchFieldException("RangedAttribute max double field");
            }
            return best;
        }
    }

    @SuppressWarnings("removal")
    private static void writeFinalDouble(Object target, Field field, double value) throws ReflectiveOperationException {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        unsafe.putDouble(target, unsafe.objectFieldOffset(field), value);
    }
}
