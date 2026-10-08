package com.example.immortal_cultivation_mod.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional Iris/Oculus integration without making either renderer a required dependency.
 * The API is queried at runtime because shader packs can be enabled or disabled while the
 * game is running.
 */
public final class ShaderPackCompatibility {
    private static final String[] IRIS_API_CLASS_NAMES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };
    private static final long PROBE_INTERVAL_NANOS = 250_000_000L;

    private static boolean initialized;
    private static Object irisApi;
    private static Method isShaderPackInUse;
    private static Method isRenderingShadowPassMethod;
    private static long lastProbeNanos = Long.MIN_VALUE;
    private static boolean shaderPackActive;
    private static boolean shadowPass;

    private ShaderPackCompatibility() {
    }

    /** True only while an installed Iris-compatible shader pack is actively rendering. */
    public static boolean isShaderPackActive() {
        refreshState();
        return shaderPackActive;
    }

    /** Prevents custom sky overlays from being submitted to an Iris shadow-map pass. */
    public static boolean isRenderingShadowPass() {
        refreshState();
        return shadowPass;
    }

    /** Custom core shaders are replaced with Minecraft render shaders while a pack is active. */
    public static boolean useCustomCoreShaders() {
        return !isShaderPackActive();
    }

    private static void refreshState() {
        initialize();
        if (irisApi == null || isShaderPackInUse == null) {
            shaderPackActive = false;
            shadowPass = false;
            return;
        }

        long now = System.nanoTime();
        if (lastProbeNanos != Long.MIN_VALUE
                && now - lastProbeNanos < PROBE_INTERVAL_NANOS) {
            return;
        }
        lastProbeNanos = now;
        try {
            shaderPackActive = Boolean.TRUE.equals(isShaderPackInUse.invoke(irisApi));
            shadowPass = shaderPackActive && isRenderingShadowPassMethod != null
                    && Boolean.TRUE.equals(
                            isRenderingShadowPassMethod.invoke(irisApi));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            shaderPackActive = false;
            shadowPass = false;
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClassLoader classLoader = ShaderPackCompatibility.class.getClassLoader();
        for (String className : IRIS_API_CLASS_NAMES) {
            try {
                Class<?> apiClass = Class.forName(className, false, classLoader);
                Object candidate = apiClass.getMethod("getInstance").invoke(null);
                Method packMethod = apiClass.getMethod("isShaderPackInUse");
                Method shadowMethod;
                try {
                    shadowMethod = apiClass.getMethod("isRenderingShadowPass");
                } catch (NoSuchMethodException ignored) {
                    shadowMethod = null;
                }
                irisApi = candidate;
                isShaderPackInUse = packMethod;
                isRenderingShadowPassMethod = shadowMethod;
                return;
            } catch (ClassNotFoundException | NoSuchMethodException
                     | IllegalAccessException | InvocationTargetException
                     | LinkageError ignored) {
                // Optional integration: continue with the next known Iris API package.
            }
        }
    }
}
