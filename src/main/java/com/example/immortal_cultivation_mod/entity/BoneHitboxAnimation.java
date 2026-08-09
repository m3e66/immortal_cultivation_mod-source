package com.example.immortal_cultivation_mod.entity;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Lightweight server-side sampler for the GeckoLib keyframes used by multipart hitboxes. */
final class BoneHitboxAnimation {
    static final Curve ZERO = curve(0.0F, 0.0F);
    static final Curve ONE = curve(0.0F, 1.0F);
    private static final Motion IDENTITY = new Motion(
            ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ONE, ONE);

    private BoneHitboxAnimation() {
    }

    static Curve curve(float... keyframes) {
        return new Curve(keyframes);
    }

    static Entry rotation(String bone, Curve x, Curve y, Curve z) {
        return motion(bone, x, y, z, ZERO, ZERO, ZERO, ONE, ONE, ONE);
    }

    static Entry positionAndRotation(String bone,
                                     Curve rotationX, Curve rotationY, Curve rotationZ,
                                     Curve positionX, Curve positionY, Curve positionZ) {
        return motion(bone, rotationX, rotationY, rotationZ,
                positionX, positionY, positionZ, ONE, ONE, ONE);
    }

    static Entry scaleAndRotation(String bone,
                                  Curve rotationX, Curve rotationY, Curve rotationZ,
                                  Curve scaleX, Curve scaleY, Curve scaleZ) {
        return motion(bone, rotationX, rotationY, rotationZ,
                ZERO, ZERO, ZERO, scaleX, scaleY, scaleZ);
    }

    static Entry motion(String bone,
                        Curve rotationX, Curve rotationY, Curve rotationZ,
                        Curve positionX, Curve positionY, Curve positionZ,
                        Curve scaleX, Curve scaleY, Curve scaleZ) {
        return new Entry(bone, new Motion(
                rotationX, rotationY, rotationZ,
                positionX, positionY, positionZ,
                scaleX, scaleY, scaleZ));
    }

    static Clip clip(float duration, boolean loop, Entry... entries) {
        Map<String, Motion> motions = new HashMap<>();
        for (Entry entry : entries) {
            motions.put(entry.bone(), entry.motion());
        }
        return new Clip(duration, loop, Map.copyOf(motions));
    }

    record Curve(float[] keyframes) {
        Curve {
            if (keyframes.length < 2 || (keyframes.length & 1) != 0) {
                throw new IllegalArgumentException("Bone curve needs time/value pairs");
            }
        }

        float sample(float time) {
            if (time <= keyframes[0]) {
                return keyframes[1];
            }
            for (int index = 2; index < keyframes.length; index += 2) {
                float endTime = keyframes[index];
                if (time <= endTime) {
                    float startTime = keyframes[index - 2];
                    float progress = endTime == startTime
                            ? 1.0F
                            : (time - startTime) / (endTime - startTime);
                    return Mth.lerp(progress, keyframes[index - 1], keyframes[index + 1]);
                }
            }
            return keyframes[keyframes.length - 1];
        }
    }

    record Motion(Curve rotationX, Curve rotationY, Curve rotationZ,
                  Curve positionX, Curve positionY, Curve positionZ,
                  Curve scaleX, Curve scaleY, Curve scaleZ) {
    }

    record Entry(String bone, Motion motion) {
    }

    static final class Clip {
        private final float duration;
        private final boolean loop;
        private final Map<String, Motion> motions;

        private Clip(float duration, boolean loop, Map<String, Motion> motions) {
            this.duration = duration;
            this.loop = loop;
            this.motions = motions;
        }

        float normalizedTime(float time) {
            if (!loop) {
                return Mth.clamp(time, 0.0F, duration);
            }
            float wrapped = time % duration;
            return wrapped < 0.0F ? wrapped + duration : wrapped;
        }

        Motion motion(String bone) {
            return motions.getOrDefault(bone, IDENTITY);
        }
    }
}
