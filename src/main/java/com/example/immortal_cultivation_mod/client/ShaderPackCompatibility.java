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
    private static boolean initialized;
    private static Object irisApi;
    private static Method isShaderPackInUse;
    private static Method isRenderingShadowPassMethod;

    private ShaderPackCompatibility() {
    }

    /** Keep the preference available even after the player switches shader packs off. */
    public static boolean isShaderModPresent() {
        initialize();
        return irisApi != null && isShaderPackInUse != null;
    }

    /** True only while an installed Iris-compatible shader pack is actively rendering. */
    public static boolean isShaderPackActive() {
        initialize();
        return query(isShaderPackInUse);
    }

    /** Prevents custom sky overlays from being submitted to an Iris shadow-map pass. */
    public static boolean isRenderingShadowPass() {
        initialize();
        // Iris switches this flag within a single frame. Caching even one frame can
        // hide the normal sky or submit it to a shadow map, producing flashing.
        return query(isRenderingShadowPassMethod);
    }

    /** Custom core shaders are replaced with Minecraft render shaders while a pack is active. */
    public static boolean useCustomCoreShaders() {
        return !isShaderPackActive();
    }

    private static boolean query(Method method) {
        if (irisApi == null || method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(irisApi));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
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
