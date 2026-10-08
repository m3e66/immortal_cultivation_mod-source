package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.BestiaryBeastKind;
import com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Selects the complete model set by registered beast kind. */
public final class CultivationBestiaryBeastModel extends GeoModel<CultivationBestiaryBeastEntity> {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0D);
    private static final String[] TORTOISE_SHELL_ROOTS = {
            "shell_front", "shell_right", "shell_rear", "shell_left"
    };
    private static final String[][] TORTOISE_SHELL_PLATES = {
            {
                    "shell_front_plate_keel",
                    "shell_front_plate_lip_l",
                    "shell_front_plate_lip_r",
                    "shell_front_plate_mid",
                    "shell_front_plate_inner_l",
                    "shell_front_plate_inner_r",
                    "shell_front_plate_crown",
                    "shell_front_plate_crown_lock"
            },
            {
                    "shell_right_plate_front_crown",
                    "shell_right_plate_front_skirt",
                    "shell_right_plate_mid_crown",
                    "shell_right_plate_mid_skirt",
                    "shell_right_plate_rear_crown",
                    "shell_right_plate_rear_skirt",
                    "shell_right_plate_rear_lock"
            },
            {
                    "shell_rear_plate_crown_lock",
                    "shell_rear_plate_crown",
                    "shell_rear_plate_inner_l",
                    "shell_rear_plate_inner_r",
                    "shell_rear_plate_mid",
                    "shell_rear_plate_lip_l",
                    "shell_rear_plate_lip_r",
                    "shell_rear_plate_keel"
            },
            {
                    "shell_left_plate_front_crown",
                    "shell_left_plate_front_skirt",
                    "shell_left_plate_mid_crown",
                    "shell_left_plate_mid_skirt",
                    "shell_left_plate_rear_crown",
                    "shell_left_plate_rear_skirt",
                    "shell_left_plate_rear_lock"
            }
    };

    @Override
    public ResourceLocation getModelResource(CultivationBestiaryBeastEntity animatable) {
        return id("geo/entity/" + animatable.kind().id() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CultivationBestiaryBeastEntity animatable) {
        return id("textures/entity/" + animatable.kind().id() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CultivationBestiaryBeastEntity animatable) {
        return id("animations/entity/" + animatable.kind().id() + ".animation.json");
    }

    @Override
    public void setCustomAnimations(CultivationBestiaryBeastEntity animatable, long instanceId,
            AnimationState<CultivationBestiaryBeastEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (animatable.kind() == BestiaryBeastKind.IRON_SCALE_SCORPION) {
            setVisible("plate_front", animatable.hasArmorSection(0));
            setVisible("plate_left", animatable.hasArmorSection(1));
            setVisible("plate_right", animatable.hasArmorSection(2));
        } else if (animatable.kind() == BestiaryBeastKind.STONE_DEVOURING_TORTOISE) {
            applyTortoiseShellFracture(
                    animatable, animationState.getPartialTick());
        }
    }

    private void applyTortoiseShellFracture(
            CultivationBestiaryBeastEntity animatable, float partialTick) {
        for (int section = 0; section < TORTOISE_SHELL_ROOTS.length; section++) {
            float progress = animatable.shellDamageProgress(section);
            float collapse = smoothStep(
                    Mth.clamp((progress - 0.72F) / 0.28F, 0.0F, 1.0F));
            getBone(TORTOISE_SHELL_ROOTS[section]).ifPresent(root -> {
                root.setHidden(false);
                root.setChildrenHidden(false);
                BoneSnapshot initial = root.getInitialSnapshot();
                float baseScaleX = initial == null ? 1.0F : initial.getScaleX();
                float baseScaleY = initial == null ? 1.0F : initial.getScaleY();
                float baseScaleZ = initial == null ? 1.0F : initial.getScaleZ();
                root.updateScale(
                        baseScaleX * (1.0F - collapse * 0.08F),
                        baseScaleY * (1.0F - collapse * 0.36F),
                        baseScaleZ * (1.0F - collapse * 0.08F));
            });

            float recoil = animatable.shellBreakRecoil(section, partialTick);
            String[] plates = TORTOISE_SHELL_PLATES[section];
            for (int plate = 0; plate < plates.length; plate++) {
                applyPlateFracture(
                        plates[plate], section, plate, progress, recoil);
            }
        }
    }

    private void applyPlateFracture(
            String boneName,
            int section,
            int plateIndex,
            float damageProgress,
            float recoil) {
        getBone(boneName).ifPresent(bone -> {
            BoneSnapshot initial = bone.getInitialSnapshot();
            if (initial == null) {
                return;
            }

            float onset = 0.05F
                    + (plateIndex % 4) * 0.085F
                    + (plateIndex / 4) * 0.035F;
            float fracture = smoothStep(Mth.clamp(
                    (damageProgress - onset) / (1.0F - onset),
                    0.0F, 1.0F));
            float alternating = (plateIndex + section & 1) == 0 ? 1.0F : -1.0F;
            float outwardX = section == 1 ? -1.0F : section == 3 ? 1.0F : 0.0F;
            float outwardZ = section == 0 ? -1.0F : section == 2 ? 1.0F : 0.0F;
            float tangentX = outwardZ == 0.0F ? 0.0F : alternating;
            float tangentZ = outwardX == 0.0F ? 0.0F : alternating;
            float spread = 0.28F + plateIndex % 3 * 0.11F;
            float pulse = recoil * (0.16F + plateIndex % 3 * 0.035F);

            float offsetX = outwardX * (spread * fracture + pulse)
                    + tangentX * fracture * (0.18F + plateIndex * 0.025F);
            float offsetY = -fracture * (0.28F + plateIndex % 3 * 0.16F)
                    + Math.abs(recoil) * 0.10F;
            float offsetZ = outwardZ * (spread * fracture + pulse)
                    + tangentZ * fracture * (0.18F + plateIndex * 0.025F);

            float pitchDegrees = fracture
                    * (outwardZ * (7.0F + plateIndex % 4 * 2.0F)
                    + alternating * 3.0F)
                    + recoil * alternating * 5.0F;
            float yawDegrees = fracture * alternating
                    * (2.0F + plateIndex % 3 * 1.5F);
            float rollDegrees = fracture
                    * (outwardX * -10.0F
                    + alternating * (5.0F + plateIndex % 4 * 1.8F))
                    + recoil * alternating * 7.0F;

            bone.updatePosition(
                    initial.getOffsetX() + offsetX,
                    initial.getOffsetY() + offsetY,
                    initial.getOffsetZ() + offsetZ);
            bone.updateRotation(
                    initial.getRotX() + pitchDegrees * DEGREES_TO_RADIANS,
                    initial.getRotY() + yawDegrees * DEGREES_TO_RADIANS,
                    initial.getRotZ() + rollDegrees * DEGREES_TO_RADIANS);

            float edgeChip = fracture * (0.07F + plateIndex % 3 * 0.025F);
            bone.updateScale(
                    initial.getScaleX() * (1.0F - edgeChip),
                    initial.getScaleY() * (1.0F - edgeChip * 1.8F),
                    initial.getScaleZ() * (1.0F - edgeChip));
        });
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private void setVisible(String boneName, boolean visible) {
        getBone(boneName).ifPresent(bone -> bone.setHidden(!visible));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
    }
}
