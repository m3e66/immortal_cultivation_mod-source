package com.example.immortal_cultivation_mod.client;

import java.util.ArrayList;
import java.util.List;

/** Static positions and UVs; camera/orbit transforms remain live in the renderers. */
final class CosmicSkyMeshes {
    static final float[] PANORAMA = panorama(18, 36);
    static final float[] HEMISPHERE = hemisphere(8, 32);

    private CosmicSkyMeshes() {}

    private static float[] panorama(int latitudes, int longitudes) {
        List<Float> values = new ArrayList<>();
        for (int latitude = 0; latitude < latitudes; latitude++) {
            for (int longitude = 0; longitude < longitudes; longitude++) {
                panoramaVertex(values, latitude, longitude, latitudes, longitudes);
                panoramaVertex(values, latitude, longitude + 1, latitudes, longitudes);
                panoramaVertex(values, latitude + 1, longitude + 1, latitudes, longitudes);
                panoramaVertex(values, latitude + 1, longitude, latitudes, longitudes);
            }
        }
        return array(values);
    }

    private static void panoramaVertex(List<Float> values, int lat, int lon, int latitudes, int longitudes) {
        float pi = (float) Math.PI;
        float latitude = -pi * 0.5F + pi * lat / latitudes;
        float longitude = -pi + (pi * 2.0F) * lon / longitudes;
        float horizontal = (float) Math.cos(latitude);
        values.add(horizontal * (float) Math.cos(longitude));
        values.add((float) Math.sin(latitude));
        values.add(horizontal * (float) Math.sin(longitude));
        values.add(lon / (float) longitudes);
        values.add(1.0F - lat / (float) latitudes);
    }

    private static float[] hemisphere(int rings, int segments) {
        List<Float> values = new ArrayList<>();
        for (int ring = 1; ring <= rings; ring++) {
            float inner = (ring - 1) / (float) rings;
            float outer = ring / (float) rings;
            for (int segment = 0; segment < segments; segment++) {
                float a = segment * (float) (Math.PI * 2.0D) / segments;
                float b = (segment + 1) * (float) (Math.PI * 2.0D) / segments;
                if (ring == 1) {
                    hemisphereVertex(values, 0.0F, 0.0F);
                    hemisphereVertex(values, outer, a);
                    hemisphereVertex(values, outer, b);
                } else {
                    hemisphereVertex(values, inner, a);
                    hemisphereVertex(values, outer, a);
                    hemisphereVertex(values, outer, b);
                    hemisphereVertex(values, inner, a);
                    hemisphereVertex(values, outer, b);
                    hemisphereVertex(values, inner, b);
                }
            }
        }
        return array(values);
    }

    private static void hemisphereVertex(List<Float> values, float radius, float angle) {
        float x = radius * (float) Math.cos(angle);
        float y = radius * (float) Math.sin(angle);
        float z = (float) Math.sqrt(Math.max(0.0F, 1.0F - radius * radius));
        values.add(x);
        values.add(y);
        values.add(0.5F + (float) Math.atan2(z, x) / (float) (Math.PI * 2.0D));
        values.add(0.5F - (float) Math.asin(y) / (float) Math.PI);
    }

    private static float[] array(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }
}
