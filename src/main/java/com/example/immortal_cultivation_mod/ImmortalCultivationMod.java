package com.example.immortal_cultivation_mod;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.block.ModBlockEntities;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import com.example.immortal_cultivation_mod.config.ModServerConfig;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.item.FateSpellScrollItems;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ModTabs;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.screen.ModScreens;
import com.example.immortal_cultivation_mod.sound.ModSounds;
import com.example.immortal_cultivation_mod.worldgen.ModFeatures;

import java.lang.reflect.Field;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

import sun.misc.Unsafe;


@Mod(ImmortalCultivationMod.MODID)
public class ImmortalCultivationMod {

    public static final String MODID =
            "immortal_cultivation_mod";


    public static final double MAX_CULTIVATION_HEALTH =
            1_000_000_000.0D;


    public static final double MAX_CULTIVATION_ARMOR =
            1_000_000_000.0D;


    private static volatile boolean
            maxHealthAttributeLimitRaised;


    private static volatile boolean
            armorAttributeLimitRaised;


    public ImmortalCultivationMod(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {


        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                ModClientConfig.SPEC
        );


        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                ModServerConfig.SPEC
        );
        modContainer.registerConfig(ModConfig.Type.SERVER,
                com.example.immortal_cultivation_mod.externalpower.ExternalPowerConfig.SPEC,
                "immortal_cultivation_mod-external-power.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER,
                com.example.immortal_cultivation_mod.alchemy.PillToxicityConfig.SPEC,
                "immortal_cultivation_mod-pill-toxicity.toml");

        modEventBus.addListener(
                this::commonSetup
        );

        ModAttachments.register(
                modEventBus
        );

        ModAttributes.ATTRIBUTES.register(
                modEventBus
        );

        ModEffects.register(
                modEventBus
        );

        com.example.immortal_cultivation_mod.fate
                .FateLootCondition
                .TYPES
                .register(
                        modEventBus
                );

        com.example.immortal_cultivation_mod.hair.HairContent.init();
        com.example.immortal_cultivation_mod.alchemy.AlchemyContent.init();

        ModBlocks.BLOCKS.register(
                modEventBus
        );


        ModBlockEntities.BLOCK_ENTITIES.register(
                modEventBus
        );

        ModItems.ITEMS.register(
                modEventBus
        );

        FateSpellScrollItems.register(
                modEventBus
        );

        com.example.immortal_cultivation_mod.item
                .ModRecipeSerializers
                .RECIPE_SERIALIZERS
                .register(
                        modEventBus
                );

        ModEntities.ENTITY_TYPES.register(
                modEventBus
        );

        ModParticles.PARTICLE_TYPES.register(
                modEventBus
        );

        ModSounds.SOUND_EVENTS.register(
                modEventBus
        );

        ModScreens.MENUS.register(
                modEventBus
        );

        ModTabs.CREATIVE_TABS.register(
                modEventBus
        );

        ModFeatures.register(
                modEventBus
        );

        com.example.immortal_cultivation_mod.magicritual
                .MagicRitualMod
                .register(
                        modEventBus
                );

        if (FMLEnvironment.dist
                == Dist.CLIENT) {

            com.example.immortal_cultivation_mod.client
                    .ModClientModBusEvents
                    .register(
                            modEventBus
                    );




            com.example.immortal_cultivation_mod.client
                    .ModClientEvents
                    .register();
        }
    }

    private void commonSetup(
            FMLCommonSetupEvent event
    ) {

        event.enqueueWork(
                () -> {

                    ModBlocks.registerVanillaInteractions();

                    raiseMaxHealthAttributeLimit();

                    raiseArmorAttributeLimit();
                }
        );
    }

    public static void raiseMaxHealthAttributeLimit() {

        if (!(Attributes.MAX_HEALTH.value()
                instanceof RangedAttribute maxHealth)) {

            return;
        }


        if (maxHealthAttributeLimitRaised
                && maxHealth.sanitizeValue(
                MAX_CULTIVATION_HEALTH
        ) >= MAX_CULTIVATION_HEALTH) {

            return;
        }


        raiseAttributeLimit(
                maxHealth,
                MAX_CULTIVATION_HEALTH,
                "Max health"
        );


        maxHealthAttributeLimitRaised =
                true;
    }

    public static void raiseArmorAttributeLimit() {

        if (!(Attributes.ARMOR.value()
                instanceof RangedAttribute armor)) {

            return;
        }


        if (armorAttributeLimitRaised
                && armor.sanitizeValue(
                MAX_CULTIVATION_ARMOR
        ) >= MAX_CULTIVATION_ARMOR) {

            return;
        }


        raiseAttributeLimit(
                armor,
                MAX_CULTIVATION_ARMOR,
                "Armor"
        );


        armorAttributeLimitRaised =
                true;
    }

    private static void raiseAttributeLimit(
            RangedAttribute attribute,
            double limit,
            String name
    ) {

        try {

            Field maxValue =
                    findMaxValueField(
                            attribute
                    );


            writeFinalDouble(
                    attribute,
                    maxValue,
                    limit
            );


            if (attribute.sanitizeValue(
                    limit
            ) < limit) {

                throw new IllegalStateException(
                        name
                                + " attribute still capped at "
                                + attribute.sanitizeValue(
                                limit
                        )
                );
            }

        } catch (
                ReflectiveOperationException exception
        ) {

            throw new IllegalStateException(
                    "Unable to raise "
                            + name.toLowerCase()
                            + " attribute limit",
                    exception
            );
        }
    }

    private static Field findMaxValueField(
            RangedAttribute attribute
    ) throws ReflectiveOperationException {

        try {

            Field named =
                    RangedAttribute.class
                            .getDeclaredField(
                                    "maxValue"
                            );


            named.setAccessible(
                    true
            );


            return named;

        } catch (
                NoSuchFieldException ignored
        ) {

            Field best =
                    null;


            double bestValue =
                    Double.NEGATIVE_INFINITY;


            for (Field field
                    : RangedAttribute.class
                    .getDeclaredFields()) {

                if (field.getType()
                        != double.class) {

                    continue;
                }


                field.setAccessible(
                        true
                );


                double value =
                        field.getDouble(
                                attribute
                        );


                if (value
                        > bestValue) {

                    best =
                            field;

                    bestValue =
                            value;
                }
            }


            if (best == null) {

                throw new NoSuchFieldException(
                        "RangedAttribute max double field"
                );
            }


            return best;
        }
    }

    @SuppressWarnings("removal")
    private static void writeFinalDouble(
            Object target,
            Field field,
            double value
    ) throws ReflectiveOperationException {

        Field unsafeField =
                Unsafe.class
                        .getDeclaredField(
                                "theUnsafe"
                        );


        unsafeField.setAccessible(
                true
        );


        Unsafe unsafe =
                (Unsafe)
                        unsafeField.get(
                                null
                        );


        unsafe.putDouble(
                target,
                unsafe.objectFieldOffset(
                        field
                ),
                value
        );
    }
}
