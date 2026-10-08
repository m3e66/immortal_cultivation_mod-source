package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.UnaryOperator;

/**
 * Supplies the state transforms missing from Ultramarine's carved railing.
 * Without these, structure positions rotate while the railing panels retain
 * their original north/east/south/west directions.
 */
@Mixin(BlockBehaviour.class)
public abstract class UltramarineRailingTransformMixin {
    private static final ResourceLocation CARVED_WOODEN_RAILING =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "carved_wooden_railing");
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    @Inject(method = "rotate", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$rotateCarvedRailing(
            BlockState state,
            Rotation rotation,
            CallbackInfoReturnable<BlockState> callback) {
        if (isCarvedWoodenRailing(state)) {
            callback.setReturnValue(transformConnections(state, rotation::rotate));
        }
    }

    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$mirrorCarvedRailing(
            BlockState state,
            Mirror mirror,
            CallbackInfoReturnable<BlockState> callback) {
        if (isCarvedWoodenRailing(state)) {
            callback.setReturnValue(transformConnections(state, mirror::mirror));
        }
    }

    private static boolean isCarvedWoodenRailing(BlockState state) {
        return CARVED_WOODEN_RAILING.equals(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static BlockState transformConnections(
            BlockState source,
            UnaryOperator<Direction> directionTransform) {
        BlockState transformed = source;
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BooleanProperty property = directionProperty(transformed, direction);
            if (property != null) {
                transformed = transformed.setValue(property, false);
            }
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BooleanProperty sourceProperty = directionProperty(source, direction);
            if (sourceProperty == null || !source.getValue(sourceProperty)) {
                continue;
            }
            BooleanProperty targetProperty = directionProperty(
                    transformed, directionTransform.apply(direction));
            if (targetProperty != null) {
                transformed = transformed.setValue(targetProperty, true);
            }
        }
        return transformed;
    }

    private static BooleanProperty directionProperty(
            BlockState state, Direction direction) {
        String name = switch (direction) {
            case NORTH -> "north";
            case EAST -> "east";
            case SOUTH -> "south";
            case WEST -> "west";
            default -> null;
        };
        if (name == null) {
            return null;
        }
        Property<?> property = state.getBlock()
                .getStateDefinition().getProperty(name);
        return property instanceof BooleanProperty booleanProperty
                ? booleanProperty : null;
    }
}
