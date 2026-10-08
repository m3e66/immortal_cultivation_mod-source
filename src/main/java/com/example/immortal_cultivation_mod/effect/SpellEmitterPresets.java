package com.example.immortal_cultivation_mod.effect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** The mod's original authored emitter values, exported without an external FX runtime. */
public final class SpellEmitterPresets {
    private SpellEmitterPresets() {}

    public static Map<String, List<Definition>> read(Reader reader) {
        var result = new LinkedHashMap<String, List<Definition>>();
        var gson = new Gson();
        JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                result.put(entry.getKey(), entry.getValue().getAsJsonArray().asList().stream()
                        .map(value -> gson.fromJson(value, Definition.class)).toList()));
        return result;
    }

    public static final class Definition {
        public String kind, shape;
        public float[] position, rotation, scale, shapeScale, size, speed, lifetime, end;
        public int[] color;
        public boolean local, looping, builtinCircle, exactEmission;
        public float rate, distanceInterval, radius, radiusThickness, arc, angle, width;
        public int duration, maxParticles;
        public Physics physics;
        public Trail trail;

        public boolean beam() { return "beam_emitter".equals(kind); }
        public Quaternionf rotation() { return new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]); }
        public Vec3 offset() { return new Vec3(position[0], position[1], position[2]); }

        public int emissionCount(int age, RandomSource random) {
            int whole = (int) rate;
            float fraction = rate - whole;
            if (fraction > 0 && (exactEmission ? age % Math.max(1, (int) (1 / fraction)) == 0
                    : random.nextFloat() < fraction)) whole++;
            return whole;
        }

        /** Sample the authored volume, not its enclosing box or a ring substitute. */
        public Spawn spawn(RandomSource random) {
            double inner = radius * (1.0D - radiusThickness);
            double r = "sphere".equals(shape)
                    ? Math.cbrt(inner * inner * inner + random.nextDouble() * (radius * radius * radius - inner * inner * inner))
                    : Math.sqrt(inner * inner + random.nextDouble() * (radius * radius - inner * inner));
            double polarY = "sphere".equals(shape) ? random.nextDouble() * 2.0D - 1.0D : 0.0D;
            double azimuth = random.nextDouble() * Math.toRadians(arc);
            double x = Math.cos(azimuth), z = Math.sin(azimuth);
            Vec3 point;
            Vec3 direction;
            switch (shape) {
                case "sphere" -> {
                    double planar = Math.sqrt(Math.max(0, 1 - polarY * polarY));
                    point = multiply(new Vec3(r * planar * x, r * polarY, r * planar * z), shapeScale);
                    direction = point.normalize();
                }
                case "cone" -> {
                    point = multiply(new Vec3(r * x, 0, r * z), shapeScale);
                    double tilt = radius == 0 ? 0 : r / radius * Math.toRadians(angle);
                    direction = new Vec3(Math.sin(tilt) * x, Math.cos(tilt), Math.sin(tilt) * z);
                }
                case "circle" -> {
                    point = multiply(new Vec3(r * x, 0, r * z), shapeScale);
                    direction = point.normalize();
                }
                case "cylinder" -> {
                    point = multiply(new Vec3(r * x, random.nextFloat() - 0.5D, r * z), shapeScale);
                    direction = new Vec3(x, 0, z);
                }
                default -> throw new IllegalArgumentException("Unsupported emitter shape: " + shape);
            }
            return new Spawn(point, direction.scale(range(speed, random.nextFloat()) * 0.05D));
        }
    }

    public static final class Physics {
        public float gravity, friction, bounce, chance, spread, collidedFriction;
        public boolean collision, remove;
    }

    public static final class Trail {
        public float lifetime, width, minDistance;
        public boolean sizeAffectsWidth, inheritColor, dieWithParticle;
        public JsonObject color, lifetimeColor;
    }

    public record Spawn(Vec3 position, Vec3 velocity) {}
    public static float range(float[] values, float t) { return values[0] + (values[1] - values[0]) * t; }
    public static Vec3 multiply(Vec3 value, float[] scale) {
        return value.multiply(scale[0], scale[1], scale[2]);
    }
    public static Vec3 rotate(Vec3 value, Quaternionf rotation) {
        Vector3f result = rotation.transform(value.toVector3f());
        return new Vec3(result.x, result.y, result.z);
    }
    public static Vector4f color(int argb) {
        return new Vector4f((argb >>> 16 & 255) / 255.0F, (argb >>> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F, (argb >>> 24 & 255) / 255.0F);
    }
    public static Vector4f color(int[] colors, float random) { return color(colors[0]).lerp(color(colors[1]), random); }

    public static Vector4f gradient(JsonObject function, float age, float random) {
        JsonObject data = function.getAsJsonObject("data");
        return switch (function.get("type").getAsString()) {
            case "color" -> color(data.get("number").getAsInt());
            case "random_color" -> color(new int[]{data.get("a").getAsInt(), data.get("b").getAsInt()}, random);
            case "gradient" -> {
                JsonObject gradient = data.getAsJsonObject("gradientColor");
                var rgb = gradient.getAsJsonArray("rgb");
                var alpha = gradient.getAsJsonArray("a");
                float[] channels = new float[4];
                for (int channel = 0; channel < 4; channel++) {
                    var stops = channel == 3 ? alpha : rgb;
                    int stride = channel == 3 ? 2 : 4, component = channel == 3 ? 1 : channel + 1;
                    float value = stops.get(component).getAsFloat();
                    for (int i = 0; i < stops.size(); i += stride) {
                        value = stops.get(i + component).getAsFloat();
                        if (i + stride == stops.size() || age <= stops.get(i).getAsFloat()) break;
                        float nextTime = stops.get(i + stride).getAsFloat();
                        if (age < nextTime) {
                            float t = (age - stops.get(i).getAsFloat()) / (nextTime - stops.get(i).getAsFloat());
                            value += (stops.get(i + stride + component).getAsFloat() - value) * t;
                            break;
                        }
                    }
                    channels[channel] = value;
                }
                yield new Vector4f(channels);
            }
            default -> throw new IllegalArgumentException("Unsupported emitter color function");
        };
    }
}
