package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Actual moving geometry drawn in front of the distant void panorama.  None of these
 * layers are baked into the skybox: each has its own depth, drift, visibility cycle,
 * and camera parallax.
 */
public final class CosmicVoidDynamicVfxRenderer {
    private static final float TAU = (float) (Math.PI * 2.0D);
    private static final ResourceLocation STAR_GLOW = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/void_star_glow.png");
    private static final ResourceLocation COMET_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/cosmic_void_comet.png");
    private static final ResourceLocation EARTH_SURFACE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/earth_surface.png");
    private static final ResourceLocation EARTH_SURFACE_NIGHT = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/environment/earth_surface_night.png");
    private static final ResourceLocation MERCURY = environmentTexture("mercury");
    private static final ResourceLocation VENUS = environmentTexture("venus");
    private static final ResourceLocation MOON = environmentTexture("moon");
    private static final ResourceLocation MARS = environmentTexture("mars");
    private static final ResourceLocation JUPITER = environmentTexture("jupiter");
    private static final ResourceLocation SATURN = environmentTexture("saturn");
    private static final ResourceLocation SATURN_RING = environmentTexture("saturn_ring");
    private static final ResourceLocation URANUS = environmentTexture("uranus");
    private static final ResourceLocation URANUS_RING = environmentTexture("uranus_ring");
    private static final ResourceLocation NEPTUNE = environmentTexture("neptune");
    private static final ResourceLocation SUN = environmentTexture("sun");
    // Earth sits on the same compressed ecliptic used by the other solar-system bodies.
    private static final Vector3f PLANET_DIRECTION = direction(4.722F, 0.0F);
    private static final float PLANET_DISTANCE = 220.0F;
    // The Earth shader uses a smaller internal body radius, so its quad is compensated here.
    private static final float PLANET_HALF_SIZE = 3.55F;
    private static final List<NebulaSheet> NEBULA_SHEETS = createNebulaSheets();
    private static final List<DistantPlanet> DISTANT_PLANETS = createDistantPlanets();
    private static final List<DistantRelic> DISTANT_RELICS = createDistantRelics();
    private static final List<PlanetAsteroid> PLANET_ASTEROIDS = createPlanetAsteroids();
    private static final List<StarCluster> STAR_CLUSTERS = createStarClusters();
    private static final List<DebrisShard> DEBRIS = createDebris();
    private static final List<CurrentMote> CURRENT_MOTES = createCurrentMotes();

    private CosmicVoidDynamicVfxRenderer() {
    }

    public static void render(Matrix4f modelViewMatrix, float time, Vec3 cameraPos,
                              float turnLagYaw, float turnLagPitch, boolean earthNight,
                              float dayOrbitAngle) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }
        Matrix4f nebulaMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 0.12F, 0.035F, turnLagYaw, turnLagPitch);
        renderNebulaSheets(nebulaMatrix, time);

        Matrix4f distantPlanetMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 0.055F, 0.018F, turnLagYaw, turnLagPitch);
        renderDistantPlanets(distantPlanetMatrix, time, dayOrbitAngle);

        Matrix4f celestialMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 0.28F, 0.08F, turnLagYaw, turnLagPitch);
        renderPlanetSystem(celestialMatrix, time, earthNight, dayOrbitAngle);

        Matrix4f clusterMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 0.82F, 0.30F, turnLagYaw, turnLagPitch);
        renderStarClusters(clusterMatrix, time, dayOrbitAngle);

        Matrix4f debrisMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 2.10F, 0.68F, turnLagYaw, turnLagPitch);
        renderDebris(debrisMatrix, time, dayOrbitAngle);

        Matrix4f currentMatrix = layerMatrix(
                modelViewMatrix, cameraPos, 2.65F, 0.86F, turnLagYaw, turnLagPitch);
        renderCurrentMotes(currentMatrix, time, dayOrbitAngle);
        renderComet(currentMatrix, time);
        renderRareSilhouette(celestialMatrix, time);
    }

    private static Matrix4f layerMatrix(Matrix4f modelViewMatrix, Vec3 cameraPos,
                                        float translation, float turnFactor,
                                        float turnLagYaw, float turnLagPitch) {
        return new Matrix4f(modelViewMatrix)
                .translate(
                        (float) -Math.sin(cameraPos.x * 0.0047D) * translation,
                        (float) -Math.sin(cameraPos.y * 0.0059D) * translation * 0.46F,
                        (float) -Math.sin(cameraPos.z * 0.0047D) * translation)
                .rotateY((float) Math.toRadians(turnLagYaw * turnFactor))
                .rotateX((float) Math.toRadians(turnLagPitch * turnFactor));
    }

    /** Huge translucent volumes behind the local objects.  Their shader generates the cloud detail. */
    private static void renderNebulaSheets(Matrix4f matrix, float time) {
        if (ShaderPackCompatibility.useCustomCoreShaders()
                && CosmicVoidSkyShaders.nebulaShader() != null) {
            RenderSystem.setShader(CosmicVoidSkyShaders::nebulaShader);
        } else {
            // Shader-pack path: retain the drifting colored volumes as soft textured
            // sheets instead of submitting the procedural core shader directly.
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        }
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (NebulaSheet sheet : NEBULA_SHEETS) {
            Vector3f direction = new Vector3f(sheet.x, sheet.y, sheet.z).normalize();
            Basis base = basis(direction);
            float rotation = sheet.phase + time * sheet.rotationSpeed;
            Vector3f right = new Vector3f(base.right).mul((float) Math.cos(rotation))
                    .fma((float) Math.sin(rotation), base.up);
            Vector3f up = new Vector3f(base.up).mul((float) Math.cos(rotation))
                    .fma(-(float) Math.sin(rotation), base.right);
            Vector3f center = new Vector3f(direction).mul(sheet.distance)
                    .fma((float) Math.sin(time * sheet.driftSpeed + sheet.phase)
                            * sheet.driftRadius, right)
                    .fma((float) Math.cos(time * sheet.driftSpeed * 0.63F + sheet.phase)
                            * sheet.driftRadius * 0.31F, up);
            addBillboardRect(buffer, matrix, center, right, up,
                    sheet.halfWidth, sheet.halfHeight,
                    sheet.red, sheet.green, sheet.blue, sheet.alpha);
        }
        drawIfNotEmpty(buffer);
    }

    /** Renders Earth as one surface layer; the supplied day/night maps already contain all detail. */
    private static void renderPlanetSystem(Matrix4f matrix, float time, boolean earthNight,
                                           float dayOrbitAngle) {
        Vector3f direction = new Vector3f(PLANET_DIRECTION)
                .normalize()
                .rotateY(dayOrbitAngle);
        Basis skyBasis = basis(direction);
        Vector3f center = earthCenter(time, direction, skyBasis);

        renderEarthTrail(matrix, time, dayOrbitAngle);
        renderPlanetBloom(matrix, center, skyBasis, PLANET_HALF_SIZE,
                0.18F, 0.48F, 1.0F, 0.42F, 1.72F);
        renderPlanetSurface(matrix, center, skyBasis, PLANET_HALF_SIZE, earthNight,
                dayOrbitAngle,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderEarthTrail(Matrix4f matrix, float time,
                                         float dayOrbitAngle) {
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        if (trailLength <= 0.001F) {
            return;
        }
        float trailSpan = 0.18F * trailLength;
        Vector3f currentDirection = new Vector3f(PLANET_DIRECTION)
                .normalize()
                .rotateY(dayOrbitAngle);
        Vector3f previousDirection = new Vector3f(PLANET_DIRECTION)
                .normalize()
                .rotateY(dayOrbitAngle - trailSpan);
        Vector3f current = earthCenter(
                time, currentDirection, basis(currentDirection));
        Vector3f previous = earthCenter(
                time, previousDirection, basis(previousDirection));

        selectBloomShader();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addTrailRibbon(buffer, matrix, previous, current,
                PLANET_HALF_SIZE * 0.44F,
                0.18F, 0.48F, 1.0F,
                0.42F * Math.min(1.0F, trailLength));
        drawIfNotEmpty(buffer);
        RenderSystem.defaultBlendFunc();
    }

    private static Vector3f earthCenter(
            float time, Vector3f direction, Basis skyBasis) {
        return new Vector3f(direction).mul(PLANET_DISTANCE)
                .fma((float) Math.sin(time * 0.000021F) * 0.84F, skyBasis.right)
                .fma((float) Math.cos(time * 0.000017F) * 0.36F, skyBasis.up);
    }

    /**
     * Small, nearly fixed worlds several times farther away than the dominant planet.
     * Their tiny angular size and much weaker parallax make them read as deep-space
     * objects rather than additional foreground landmarks.
     */
    private static void renderDistantPlanets(Matrix4f matrix, float time,
                                             float dayOrbitAngle) {
        renderDistantPlanetTrails(matrix, time, dayOrbitAngle);
        renderDistantPlanetBlooms(matrix, time, dayOrbitAngle);
        for (DistantPlanet planet : DISTANT_PLANETS) {
            Vector3f direction = planetDirection(planet, dayOrbitAngle);
            Basis base = basis(direction);
            Basis rotated = rotateBasis(base, planet.rotation);
            Vector3f center = new Vector3f(direction).mul(planet.distance)
                    .fma((float) Math.sin(time * planet.driftSpeed + planet.phase)
                            * planet.driftRadius, rotated.right)
                    .fma((float) Math.cos(time * planet.driftSpeed * 0.71F + planet.phase)
                            * planet.driftRadius * 0.38F, rotated.up);
            Basis ringBasis = rotateBasis(rotated, planet.ringRotation);
            if (planet.ringTexture != null) {
                renderPlanetRing(matrix, center, ringBasis, planet, true);
            }
            renderCelestialSurface(matrix, center, rotated, planet);
            if (planet.ringTexture != null) {
                renderPlanetRing(matrix, center, ringBasis, planet, false);
            }
        }
    }

    /** Textured spherical impostors keep distant worlds round without adding heavy meshes. */
    private static void renderCelestialSurface(Matrix4f matrix, Vector3f center, Basis basis,
                                               DistantPlanet planet) {
        renderCelestialSurface(matrix, center, basis, planet.texture, planet.halfSize,
                planet.texturePhase, planet.rotationSpeed, planet.edgeGlow, planet.emissive);
    }

    private static void renderCelestialSurface(Matrix4f matrix, Vector3f center, Basis basis,
                                               ResourceLocation texture, float halfSize,
                                               float texturePhase, float rotationSpeed,
                                               float edgeGlow, float emissive) {
        boolean customShader = ShaderPackCompatibility.useCustomCoreShaders()
                && CosmicVoidSkyShaders.celestialShader() != null;
        if (customShader) {
            RenderSystem.setShader(CosmicVoidSkyShaders::celestialShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        }
        CosmicVoidTextureFiltering.bind(0, texture);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                customShader ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        if (customShader) {
            addBillboardRect(buffer, matrix, center, basis.right, basis.up,
                    halfSize, halfSize, texturePhase, rotationSpeed, edgeGlow, emissive);
        } else {
            addCompatibleSphere(buffer, matrix, center, basis, halfSize,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
        drawIfNotEmpty(buffer);
    }

    /** Replaces vanilla's flat Overworld Sun and Moon with the supplied spherical textures. */
    public static void renderOverworldCelestials(Matrix4f modelViewMatrix,
                                                  float timeOfDay, float weatherAlpha) {
        if (weatherAlpha <= 0.001F
                || ShaderPackCompatibility.isRenderingShadowPass()) {
            return;
        }

        // Minecraft time starts at sunrise rather than noon. Applying the vanilla
        // celestial-angle curve keeps the Sun above the horizon only during the day;
        // using timeOfDay * TAU directly made it rise again around midnight.
        float celestialAngle = overworldCelestialAngle(timeOfDay);
        float sunVisibility = horizonVisibility((float) Math.cos(celestialAngle));
        float moonVisibility = horizonVisibility((float) -Math.cos(celestialAngle));
        float sunAlpha = weatherAlpha * sunVisibility;
        float moonAlpha = weatherAlpha * moonVisibility;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            Matrix4f celestialMatrix = new Matrix4f(modelViewMatrix)
                    .rotateY((float) Math.toRadians(-90.0F))
                    .rotateX(celestialAngle);
            Basis plane = new Basis(new Vector3f(1.0F, 0.0F, 0.0F),
                    new Vector3f(0.0F, 0.0F, 1.0F));
            Vector3f sunCenter = new Vector3f(0.0F, 100.0F, 0.0F);
            Vector3f moonCenter = new Vector3f(0.0F, -100.0F, 0.0F);

            renderOverworldCelestialTrails(
                    modelViewMatrix, celestialAngle, sunAlpha, moonAlpha,
                    sunCenter, moonCenter, plane);
            if (sunAlpha > 0.001F) {
                renderPlanetBloom(celestialMatrix, sunCenter, plane, 16.0F,
                        1.0F, 0.56F, 0.14F, sunAlpha);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, sunAlpha);
                renderCelestialSurface(celestialMatrix, sunCenter, plane, SUN, 16.0F,
                        0.91F, 0.93F, 1.0F, 1.0F);
            }
            if (moonAlpha > 0.001F) {
                renderPlanetBloom(celestialMatrix, moonCenter, plane, 20.0F,
                        0.56F, 0.70F, 1.0F, 0.68F * moonAlpha);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, moonAlpha);
                renderCelestialSurface(celestialMatrix, moonCenter, plane, MOON, 20.0F,
                        0.57F, 0.62F, 0.16F, 0.02F);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void renderOverworldCelestialTrails(
            Matrix4f modelViewMatrix, float celestialAngle,
            float sunAlpha, float moonAlpha,
            Vector3f sunCenter, Vector3f moonCenter, Basis plane) {
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        if (trailLength <= 0.001F) {
            return;
        }

        selectBloomShader();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float trailSpan = 0.18F * trailLength;
        float trailAlpha = Math.min(1.0F, trailLength);
        Vector3f currentSun = overworldOrbitPoint(sunCenter, celestialAngle);
        Vector3f previousSun = overworldOrbitPoint(
                sunCenter, celestialAngle - trailSpan);
        Vector3f currentMoon = overworldOrbitPoint(moonCenter, celestialAngle);
        Vector3f previousMoon = overworldOrbitPoint(
                moonCenter, celestialAngle - trailSpan);
        addTrailRibbon(buffer, modelViewMatrix, previousSun, currentSun,
                7.00F, 1.0F, 0.56F, 0.14F,
                sunAlpha * 0.56F * trailAlpha);
        addTrailRibbon(buffer, modelViewMatrix, previousMoon, currentMoon,
                5.50F, 0.56F, 0.70F, 1.0F,
                moonAlpha * 0.50F * trailAlpha);
        drawIfNotEmpty(buffer);
        RenderSystem.defaultBlendFunc();
    }

    private static float overworldCelestialAngle(float timeOfDay) {
        float shiftedTime = timeOfDay - 0.25F;
        shiftedTime -= (float) Math.floor(shiftedTime);
        float smoothedTime = 0.5F
                - (float) Math.cos(shiftedTime * Math.PI) * 0.5F;
        return ((shiftedTime * 2.0F + smoothedTime) / 3.0F) * TAU;
    }

    private static float horizonVisibility(float height) {
        float value = Math.max(0.0F, Math.min(1.0F, height / 0.16F));
        return value * value * (3.0F - 2.0F * value);
    }

    private static void renderDistantPlanetTrails(
            Matrix4f matrix, float time, float dayOrbitAngle) {
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        if (trailLength <= 0.001F) {
            return;
        }

        selectBloomShader();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float trailSpan = 0.18F * trailLength;
        float trailAlpha = Math.min(1.0F, trailLength);
        for (DistantPlanet planet : DISTANT_PLANETS) {
            Vector3f currentDirection = planetDirection(planet, dayOrbitAngle);
            Vector3f previousDirection = planetDirection(
                    planet, dayOrbitAngle - trailSpan);
            Basis currentBasis = rotateBasis(
                    basis(currentDirection), planet.rotation);
            Basis previousBasis = rotateBasis(
                    basis(previousDirection), planet.rotation);
            Vector3f current = distantPlanetCenter(
                    planet, time, currentDirection, currentBasis);
            Vector3f previous = distantPlanetCenter(
                    planet, time, previousDirection, previousBasis);
            Vector3f color = planetBloomColor(planet.texture);
            addTrailRibbon(buffer, matrix, previous, current,
                    Math.max(0.70F, planet.halfSize * 0.52F),
                    color.x, color.y, color.z,
                    (0.48F + planet.emissive * 0.24F) * trailAlpha);
        }
        drawIfNotEmpty(buffer);
        RenderSystem.defaultBlendFunc();
    }

    private static void renderDistantPlanetBlooms(Matrix4f matrix, float time,
                                                  float dayOrbitAngle) {
        selectBloomShader();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (DistantPlanet planet : DISTANT_PLANETS) {
            Vector3f direction = planetDirection(planet, dayOrbitAngle);
            Basis rotated = rotateBasis(basis(direction), planet.rotation);
            Vector3f center = new Vector3f(direction).mul(planet.distance)
                    .fma((float) Math.sin(time * planet.driftSpeed + planet.phase)
                            * planet.driftRadius, rotated.right)
                    .fma((float) Math.cos(time * planet.driftSpeed * 0.71F + planet.phase)
                            * planet.driftRadius * 0.38F, rotated.up);
            addPlanetBloom(buffer, matrix, center, rotated, planet.halfSize,
                    planet.texture, 0.38F + planet.edgeGlow * 0.22F
                            + planet.emissive * 0.40F);
        }
        drawIfNotEmpty(buffer);
        RenderSystem.defaultBlendFunc();
    }

    private static Vector3f planetDirection(DistantPlanet planet, float dayOrbitAngle) {
        return new Vector3f(planet.x, planet.y, planet.z)
                .normalize()
                .rotateY(dayOrbitAngle);
    }

    private static Vector3f distantPlanetCenter(
            DistantPlanet planet, float time, Vector3f direction, Basis basis) {
        return new Vector3f(direction).mul(planet.distance)
                .fma((float) Math.sin(time * planet.driftSpeed + planet.phase)
                        * planet.driftRadius, basis.right)
                .fma((float) Math.cos(time * planet.driftSpeed * 0.71F
                                + planet.phase)
                        * planet.driftRadius * 0.38F, basis.up);
    }

    private static Vector3f overworldOrbitPoint(Vector3f point, float angle) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float rotatedY = point.y * cos - point.z * sin;
        float rotatedZ = point.y * sin + point.z * cos;
        return new Vector3f(-rotatedZ, rotatedY, point.x);
    }

    private static void renderPlanetBloom(Matrix4f matrix, Vector3f center, Basis basis,
                                          float halfSize, float red, float green, float blue,
                                          float strength) {
        renderPlanetBloom(matrix, center, basis, halfSize, red, green, blue,
                strength, 2.20F);
    }

    private static void renderPlanetBloom(Matrix4f matrix, Vector3f center, Basis basis,
                                          float halfSize, float red, float green, float blue,
                                          float strength, float spread) {
        selectBloomShader();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addBillboard(buffer, matrix, center, basis.right, basis.up, halfSize * spread,
                red, green, blue, strength);
        drawIfNotEmpty(buffer);
        RenderSystem.defaultBlendFunc();
    }

    private static void addPlanetBloom(BufferBuilder buffer, Matrix4f matrix,
                                       Vector3f center, Basis basis, float halfSize,
                                       ResourceLocation texture, float strength) {
        Vector3f color = planetBloomColor(texture);
        addBillboard(buffer, matrix, center, basis.right, basis.up, halfSize * 2.20F,
                color.x, color.y, color.z, strength);
    }

    private static Vector3f planetBloomColor(ResourceLocation texture) {
        if (texture.equals(SUN)) {
            return new Vector3f(1.0F, 0.56F, 0.14F);
        }
        if (texture.equals(MERCURY)) {
            return new Vector3f(0.72F, 0.70F, 0.68F);
        }
        if (texture.equals(VENUS)) {
            return new Vector3f(1.0F, 0.72F, 0.38F);
        }
        if (texture.equals(MOON)) {
            return new Vector3f(0.62F, 0.72F, 1.0F);
        }
        if (texture.equals(MARS)) {
            return new Vector3f(1.0F, 0.30F, 0.13F);
        }
        if (texture.equals(JUPITER)) {
            return new Vector3f(0.95F, 0.73F, 0.52F);
        }
        if (texture.equals(SATURN)) {
            return new Vector3f(1.0F, 0.82F, 0.52F);
        }
        if (texture.equals(URANUS)) {
            return new Vector3f(0.34F, 0.88F, 1.0F);
        }
        if (texture.equals(NEPTUNE)) {
            return new Vector3f(0.24F, 0.43F, 1.0F);
        }
        return new Vector3f(0.64F, 0.72F, 0.86F);
    }

    /** Draws rings in rear and front halves so the spherical body occludes their center. */
    private static void renderPlanetRing(Matrix4f matrix, Vector3f center, Basis basis,
                                         DistantPlanet planet, boolean rearHalf) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, planet.ringTexture);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int segments = 96;
        float innerRadius = planet.halfSize * planet.ringInnerScale;
        float outerRadius = planet.halfSize * planet.ringOuterScale;
        for (int segment = 0; segment < segments; segment++) {
            float angle0 = segment * TAU / segments;
            float angle1 = (segment + 1) * TAU / segments;
            float middle = (angle0 + angle1) * 0.5F;
            if (((float) Math.sin(middle) > 0.0F) != rearHalf) {
                continue;
            }

            Vector3f inner0 = ringPoint(center, basis, angle0, innerRadius, planet.ringFlatten);
            Vector3f outer0 = ringPoint(center, basis, angle0, outerRadius, planet.ringFlatten);
            Vector3f outer1 = ringPoint(center, basis, angle1, outerRadius, planet.ringFlatten);
            Vector3f inner1 = ringPoint(center, basis, angle1, innerRadius, planet.ringFlatten);
            float u0 = segment / (float) segments;
            float u1 = (segment + 1) / (float) segments;
            ringVertex(buffer, matrix, inner0, u0, 0.0F, planet.ringAlpha);
            ringVertex(buffer, matrix, outer0, u0, 1.0F, planet.ringAlpha);
            ringVertex(buffer, matrix, outer1, u1, 1.0F, planet.ringAlpha);
            ringVertex(buffer, matrix, inner1, u1, 0.0F, planet.ringAlpha);
        }
        drawIfNotEmpty(buffer);
    }

    private static Vector3f ringPoint(Vector3f center, Basis basis, float angle,
                                      float radius, float flatten) {
        return new Vector3f(center)
                .fma((float) Math.cos(angle) * radius, basis.right)
                .fma((float) Math.sin(angle) * radius * flatten, basis.up);
    }

    private static void ringVertex(BufferBuilder buffer, Matrix4f matrix, Vector3f point,
                                   float u, float v, float alpha) {
        VfxVertexWriter.vertex(buffer, matrix, point.x, point.y, point.z)
                .setUv(u, v)
                .setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    private static void renderPlanetSurface(Matrix4f matrix, Vector3f center, Basis basis,
                                            float halfSize, boolean night, float surfaceRotation,
                                            float red, float green, float blue, float alpha) {
        boolean customShader = ShaderPackCompatibility.useCustomCoreShaders()
                && CosmicVoidSkyShaders.planetShader() != null;
        if (customShader) {
            RenderSystem.setShader(CosmicVoidSkyShaders::planetShader);
            CosmicVoidSkyShaders.planetShader().safeGetUniform("SurfaceRotation")
                    .set(surfaceRotation);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        }
        CosmicVoidTextureFiltering.bind(0, night ? EARTH_SURFACE_NIGHT : EARTH_SURFACE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                customShader ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        if (customShader) {
            addBillboardRect(buffer, matrix, center, basis.right, basis.up,
                    halfSize, halfSize, red, green, blue, alpha);
        } else {
            addCompatibleSphere(buffer, matrix, center, basis, halfSize,
                    red, green, blue, alpha);
        }
        drawIfNotEmpty(buffer);
    }

    private static void renderPlanetAsteroids(Matrix4f matrix, float time,
                                               Vector3f planetCenter, Vector3f direction,
                                               Basis beltBasis, boolean rearHalf) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (PlanetAsteroid asteroid : PLANET_ASTEROIDS) {
            float angle = asteroid.angle + time * asteroid.orbitSpeed;
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            float depth = sin * asteroid.radius * 0.47F + asteroid.depthOffset;
            boolean behindPlanet = depth > 0.0F;
            if (behindPlanet != rearHalf) {
                continue;
            }

            Vector3f center = new Vector3f(planetCenter)
                    .fma(cos * asteroid.radius, beltBasis.right)
                    .fma(sin * asteroid.radius * 0.19F + asteroid.height, beltBasis.up)
                    .fma(depth, direction);
            float perspective = PLANET_DISTANCE / Math.max(54.0F, PLANET_DISTANCE + depth);
            float size = asteroid.size * perspective;
            Matrix4f rockMatrix = new Matrix4f(matrix)
                    .translate(center.x, center.y, center.z)
                    .rotateY(time * asteroid.spinY + asteroid.phase)
                    .rotateX(time * asteroid.spinX + asteroid.phase * 0.47F)
                    .rotateZ(time * asteroid.spinZ + asteroid.phase * 0.79F);
            float light = 0.64F + Math.max(0.0F, cos) * 0.36F;
            addRock(buffer, rockMatrix, size,
                    asteroid.red * light, asteroid.green * light,
                    asteroid.blue * light, asteroid.alpha);
        }
        drawIfNotEmpty(buffer);
    }

    /** Five very distant landmarks, plus fragments that orbit their own parent instead of drifting randomly. */
    private static void renderDistantRelics(Matrix4f matrix, float time) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder glowBuffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (DistantRelic relic : DISTANT_RELICS) {
            Vector3f direction = new Vector3f(relic.x, relic.y, relic.z).normalize();
            Basis basis = basis(direction);
            Vector3f center = relicCenter(relic, direction, basis, time);
            float pulse = 0.92F + (float) Math.sin(time * 0.0011F + relic.phase) * 0.08F;
            addBillboard(glowBuffer, matrix, center, basis.right, basis.up,
                    relic.size * 1.72F, relic.red, relic.green, relic.blue,
                    relic.alpha * 0.16F * pulse);
            addBillboard(glowBuffer, matrix, center, basis.right, basis.up,
                    relic.size * 1.08F, relic.red, relic.green, relic.blue,
                    relic.alpha * 0.10F * pulse);
        }
        drawIfNotEmpty(glowBuffer);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder solidBuffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (DistantRelic relic : DISTANT_RELICS) {
            Vector3f direction = new Vector3f(relic.x, relic.y, relic.z).normalize();
            Basis base = basis(direction);
            float rotation = relic.phase + time * relic.rotationSpeed;
            Basis rotated = rotateBasis(base, rotation);
            Vector3f center = relicCenter(relic, direction, base, time);
            addRelicBody(solidBuffer, matrix, center, rotated, relic);

            int orbitCount = 3 + relic.kind;
            for (int shard = 0; shard < orbitCount; shard++) {
                float angle = shard * TAU / orbitCount + relic.phase
                        + time * (0.00012F + relic.kind * 0.000012F);
                float radius = relic.size * (1.18F + (shard % 3) * 0.13F);
                Vector3f shardCenter = new Vector3f(center)
                        .fma((float) Math.cos(angle) * radius, rotated.right)
                        .fma((float) Math.sin(angle) * radius * 0.43F, rotated.up)
                        .fma((float) Math.sin(angle * 1.7F) * relic.size * 0.08F, direction);
                Matrix4f shardMatrix = new Matrix4f(matrix)
                        .translate(shardCenter.x, shardCenter.y, shardCenter.z)
                        .rotateY(angle + time * 0.0007F)
                        .rotateX(angle * 0.63F - time * 0.00043F)
                        .rotateZ(angle * 0.37F);
                addShard(solidBuffer, shardMatrix,
                        relic.size * (0.035F + (shard % 4) * 0.011F),
                        relic.red * 0.72F, relic.green * 0.72F,
                        relic.blue * 0.78F, relic.alpha * 0.72F);
            }
        }
        drawIfNotEmpty(solidBuffer);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.5F);
        BufferBuilder lineBuffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (DistantRelic relic : DISTANT_RELICS) {
            Vector3f direction = new Vector3f(relic.x, relic.y, relic.z).normalize();
            Basis base = basis(direction);
            float rotation = relic.phase + time * relic.rotationSpeed;
            Basis rotated = rotateBasis(base, rotation);
            Vector3f center = relicCenter(relic, direction, base, time);
            addRelicLines(lineBuffer, matrix, center, rotated, relic);
        }
        drawIfNotEmpty(lineBuffer);
    }

    private static Vector3f relicCenter(DistantRelic relic, Vector3f direction,
                                        Basis basis, float time) {
        return new Vector3f(direction).mul(relic.distance)
                .fma((float) Math.sin(time * relic.driftSpeed + relic.phase)
                        * relic.driftRadius, basis.right)
                .fma((float) Math.cos(time * relic.driftSpeed * 0.71F + relic.phase)
                        * relic.driftRadius * 0.42F, basis.up);
    }

    private static void addRelicBody(BufferBuilder buffer, Matrix4f matrix,
                                     Vector3f center, Basis basis, DistantRelic relic) {
        float size = relic.size;
        float red = relic.red * 0.30F;
        float green = relic.green * 0.30F;
        float blue = relic.blue * 0.38F;
        float alpha = relic.alpha * 0.88F;
        if (relic.kind == 0) {
            // A broken moon: most of the disc is dark; three pieces are missing from its rim.
            for (int segment = 0; segment < 28; segment++) {
                if (segment == 3 || segment == 4 || segment == 17 || segment == 23) {
                    continue;
                }
                float a0 = segment * TAU / 28.0F;
                float a1 = (segment + 1) * TAU / 28.0F;
                triangle(buffer, matrix, center,
                        planePoint(center, basis, (float) Math.cos(a0) * size,
                                (float) Math.sin(a0) * size),
                        planePoint(center, basis, (float) Math.cos(a1) * size,
                                (float) Math.sin(a1) * size),
                        red, green, blue, alpha);
            }
        } else if (relic.kind == 2) {
            // A colossal shattered gate.
            addPlaneQuad(buffer, matrix, center, basis,
                    -size * 0.88F, -size * 0.92F, -size * 0.63F, size * 0.82F,
                    red, green, blue, alpha);
            addPlaneQuad(buffer, matrix, center, basis,
                    size * 0.63F, -size * 0.92F, size * 0.88F, size * 0.82F,
                    red, green, blue, alpha);
            addPlaneQuad(buffer, matrix, center, basis,
                    -size * 0.82F, size * 0.58F, size * 0.82F, size * 0.82F,
                    red, green, blue, alpha);
        } else if (relic.kind == 3) {
            // A cracked platform seen edge-on.
            Vector3f a = planePoint(center, basis, -size, -size * 0.12F);
            Vector3f b = planePoint(center, basis, -size * 0.16F, size * 0.52F);
            Vector3f c = planePoint(center, basis, size, size * 0.08F);
            Vector3f d = planePoint(center, basis, size * 0.28F, -size * 0.46F);
            triangle(buffer, matrix, a, b, d, red, green, blue, alpha);
            triangle(buffer, matrix, b, c, d, red, green, blue, alpha);
        } else {
            // Ring and seal landmarks keep an empty center and are defined mainly by their luminous rim.
            float inner = size * (relic.kind == 1 ? 0.72F : 0.61F);
            for (int segment = 0; segment < 32; segment++) {
                if ((segment + relic.kind) % 9 == 0) {
                    continue;
                }
                float a0 = segment * TAU / 32.0F;
                float a1 = (segment + 1) * TAU / 32.0F;
                Vector3f outer0 = planePoint(center, basis,
                        (float) Math.cos(a0) * size, (float) Math.sin(a0) * size);
                Vector3f outer1 = planePoint(center, basis,
                        (float) Math.cos(a1) * size, (float) Math.sin(a1) * size);
                Vector3f inner0 = planePoint(center, basis,
                        (float) Math.cos(a0) * inner, (float) Math.sin(a0) * inner);
                Vector3f inner1 = planePoint(center, basis,
                        (float) Math.cos(a1) * inner, (float) Math.sin(a1) * inner);
                triangle(buffer, matrix, outer0, outer1, inner1, red, green, blue, alpha);
                triangle(buffer, matrix, outer0, inner1, inner0, red, green, blue, alpha);
            }
        }
    }

    private static void addRelicLines(BufferBuilder buffer, Matrix4f matrix,
                                      Vector3f center, Basis basis, DistantRelic relic) {
        float size = relic.size;
        float red = Math.min(1.0F, relic.red * 1.16F);
        float green = Math.min(1.0F, relic.green * 1.16F);
        float blue = Math.min(1.0F, relic.blue * 1.22F);
        float alpha = relic.alpha * 0.46F;
        addBrokenEllipse(buffer, matrix, center, basis,
                size, size * (relic.kind == 3 ? 0.46F : 1.0F),
                0.0F, red, green, blue, alpha, 48, 11 - Math.min(4, relic.kind));
        if (relic.kind == 1 || relic.kind == 4) {
            addBrokenEllipse(buffer, matrix, center, basis,
                    size * 0.69F, size * (relic.kind == 1 ? 0.32F : 0.69F),
                    relic.phase, red, green, blue, alpha * 0.74F, 40, 8);
            for (int spoke = 0; spoke < 6; spoke++) {
                float angle = spoke * TAU / 6.0F;
                line(buffer, matrix,
                        planePoint(center, basis, (float) Math.cos(angle) * size * 0.22F,
                                (float) Math.sin(angle) * size * 0.22F),
                        planePoint(center, basis, (float) Math.cos(angle) * size * 0.62F,
                                (float) Math.sin(angle) * size * 0.62F),
                        red, green, blue, alpha * 0.65F);
            }
        } else if (relic.kind == 2) {
            line(buffer, matrix, planePoint(center, basis, -size * 0.74F, -size * 0.9F),
                    planePoint(center, basis, -size * 0.74F, size * 0.7F),
                    red, green, blue, alpha);
            line(buffer, matrix, planePoint(center, basis, size * 0.74F, -size * 0.9F),
                    planePoint(center, basis, size * 0.74F, size * 0.7F),
                    red, green, blue, alpha);
            line(buffer, matrix, planePoint(center, basis, -size * 0.74F, size * 0.7F),
                    planePoint(center, basis, size * 0.74F, size * 0.7F),
                    red, green, blue, alpha);
        }
    }

    private static void renderComet(Matrix4f matrix, float time) {
        // Rare deterministic sky event: every client sees the same path without networking.
        float period = 1800.0F;
        float duration = 150.0F;
        float shifted = time + 413.0F;
        float local = positiveModulo(shifted, period);
        if (local > duration) {
            return;
        }
        int eventIndex = (int) Math.floor(shifted / period);
        float seed = pseudoRandom(eventIndex * 17.31F + 4.9F);
        Vector3f direction = direction(seed * TAU,
                -0.38F + pseudoRandom(eventIndex * 5.77F + 3.2F) * 0.76F);
        Basis basis = rotateBasis(basis(direction),
                -0.28F + pseudoRandom(eventIndex * 11.43F + 1.8F) * 0.56F);
        float progress = local / duration;
        float fade = smoothStep(0.0F, 0.13F, progress)
                * (1.0F - smoothStep(0.80F, 1.0F, progress));
        float along = -39.0F + progress * 78.0F;
        float curve = (float) Math.sin(progress * Math.PI) * 3.2F;
        Vector3f center = new Vector3f(direction).mul(88.0F)
                .fma(along, basis.right).fma(curve, basis.up);

        if (ShaderPackCompatibility.useCustomCoreShaders()
                && CosmicVoidSkyShaders.cometShader() != null) {
            RenderSystem.setShader(CosmicVoidSkyShaders::cometShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        }
        CosmicVoidTextureFiltering.bind(0, COMET_TEXTURE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addBillboardRect(buffer, matrix, center, basis.right, basis.up,
                10.0F, 3.1F, 1.0F, 1.0F, 1.0F, fade);
        drawIfNotEmpty(buffer);
    }

    private static void renderRareSilhouette(Matrix4f matrix, float time) {
        float period = 12000.0F;
        float duration = 300.0F;
        float shifted = time + 4430.0F;
        float local = positiveModulo(shifted, period);
        if (local > duration) {
            return;
        }
        int eventIndex = (int) Math.floor(shifted / period);
        Vector3f direction = direction(pseudoRandom(eventIndex * 17.3F + 1.9F) * TAU,
                -0.30F + pseudoRandom(eventIndex * 9.7F + 3.4F) * 0.60F);
        Basis basis = basis(direction);
        float progress = local / duration;
        float fade = smoothStep(0.0F, 0.17F, progress)
                * (1.0F - smoothStep(0.72F, 1.0F, progress));
        Vector3f center = new Vector3f(direction).mul(205.0F)
                .fma(-26.0F + progress * 52.0F, basis.right)
                .fma((float) Math.sin(progress * Math.PI) * 2.8F, basis.up);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder glow = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addBillboardRect(glow, matrix, center, basis.right, basis.up,
                18.0F, 7.2F, 0.16F, 0.08F, 0.29F, fade * 0.12F);
        drawIfNotEmpty(glow);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder body = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Vector3f nose = planePoint(center, basis, 15.0F, 0.0F);
        Vector3f upperWing = planePoint(center, basis, -4.0F, 6.2F);
        Vector3f lowerWing = planePoint(center, basis, -4.0F, -6.2F);
        Vector3f tail = planePoint(center, basis, -17.0F, 0.0F);
        Vector3f core = planePoint(center, basis, 1.2F, 0.0F);
        triangle(body, matrix, nose, upperWing, core,
                0.003F, 0.002F, 0.009F, fade * 0.88F);
        triangle(body, matrix, nose, core, lowerWing,
                0.003F, 0.002F, 0.009F, fade * 0.88F);
        triangle(body, matrix, upperWing, tail, core,
                0.002F, 0.001F, 0.006F, fade * 0.82F);
        triangle(body, matrix, core, tail, lowerWing,
                0.002F, 0.001F, 0.006F, fade * 0.82F);
        drawIfNotEmpty(body);
    }

    private static void renderStarClusters(
            Matrix4f matrix, float time, float dayOrbitAngle) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float trailLength = TimeFastForwardClientState.trailLengthMultiplier();
        float trailSpan = trailLength > 0.0F
                ? 0.16F * trailLength
                : 0.0F;
        float trailAlpha = Math.min(1.0F, trailLength);
        for (StarCluster cluster : STAR_CLUSTERS) {
            float visibilityWave = ((float) Math.sin(time * cluster.fadeSpeed + cluster.phase) + 1.0F) * 0.5F;
            float visibility = smoothStep(0.18F, 0.72F, visibilityWave);
            if (visibility < 0.02F) {
                continue;
            }
            StarClusterFrame current = starClusterFrame(
                    cluster, time, dayOrbitAngle);
            StarClusterFrame previous = trailSpan > 0.0F
                    ? starClusterFrame(cluster, time, dayOrbitAngle - trailSpan)
                    : null;
            for (ClusterStar star : cluster.stars) {
                Vector3f starCenter = clusterStarCenter(current, star);
                float twinkle = 0.72F + 0.28F * (float) Math.sin(time * star.twinkleSpeed + star.phase);
                if (previous != null) {
                    Vector3f previousCenter = clusterStarCenter(previous, star);
                    addTrailRibbon(buffer, matrix, previousCenter, starCenter,
                            Math.max(0.09F, star.size * 1.15F),
                            0.74F, 0.84F, 1.0F,
                            cluster.alpha * visibility * twinkle * 0.88F
                                    * trailAlpha);
                }
                addGlowingStar(buffer, matrix, starCenter,
                        current.basis.right, current.basis.up,
                        star.size * (0.95F + twinkle * 0.05F),
                        0.74F, 0.84F, 1.0F, cluster.alpha * visibility * twinkle);
            }
        }
        drawIfNotEmpty(buffer);
    }

    private static StarClusterFrame starClusterFrame(
            StarCluster cluster, float time, float dayOrbitAngle) {
        Vector3f direction = new Vector3f(cluster.x, cluster.y, cluster.z)
                .normalize()
                .rotateY(dayOrbitAngle);
        Basis clusterBasis = basis(direction);
        float drift = time * cluster.driftSpeed + cluster.phase;
        Vector3f center = new Vector3f(direction).mul(cluster.distance)
                .fma((float) Math.sin(drift) * cluster.driftRadius,
                        clusterBasis.right)
                .fma((float) Math.cos(drift * 0.73F)
                                * cluster.driftRadius * 0.42F,
                        clusterBasis.up);
        return new StarClusterFrame(center, clusterBasis);
    }

    private static Vector3f clusterStarCenter(
            StarClusterFrame frame, ClusterStar star) {
        return new Vector3f(frame.center)
                .fma(star.offsetX, frame.basis.right)
                .fma(star.offsetY, frame.basis.up);
    }

    private static void renderDebris(Matrix4f matrix, float time, float dayOrbitAngle) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder glowBuffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (DebrisShard shard : DEBRIS) {
            Vector3f direction = new Vector3f(shard.x, shard.y, shard.z)
                    .normalize()
                    .rotateY(dayOrbitAngle);
            Basis basis = basis(direction);
            float drift = time * shard.driftSpeed + shard.phase;
            Vector3f center = new Vector3f(direction).mul(shard.distance)
                    .fma((float) Math.sin(drift) * shard.driftRadius, basis.right)
                    .fma((float) Math.cos(drift * 0.67F) * shard.driftRadius * 0.55F, basis.up);
            addBillboard(glowBuffer, matrix, center, basis.right, basis.up,
                    shard.size * 2.65F, shard.red, shard.green, shard.blue, shard.alpha * 0.24F);
            addBillboard(glowBuffer, matrix, center, basis.right, basis.up,
                    shard.size * 1.28F, Math.min(1.0F, shard.red * 1.45F),
                    Math.min(1.0F, shard.green * 1.45F), Math.min(1.0F, shard.blue * 1.45F),
                    shard.alpha * 0.34F);
        }
        drawIfNotEmpty(glowBuffer);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (DebrisShard shard : DEBRIS) {
            Vector3f direction = new Vector3f(shard.x, shard.y, shard.z)
                    .normalize()
                    .rotateY(dayOrbitAngle);
            Basis basis = basis(direction);
            float drift = time * shard.driftSpeed + shard.phase;
            Vector3f center = new Vector3f(direction).mul(shard.distance)
                    .fma((float) Math.sin(drift) * shard.driftRadius, basis.right)
                    .fma((float) Math.cos(drift * 0.67F) * shard.driftRadius * 0.55F, basis.up);
            Matrix4f shardMatrix = new Matrix4f(matrix)
                    .translate(center.x, center.y, center.z)
                    .rotateY(time * shard.spinY + shard.phase)
                    .rotateX(time * shard.spinX + shard.phase * 0.43F)
                    .rotateZ(time * shard.spinZ + shard.phase * 0.71F);
            addShard(buffer, shardMatrix, shard.size,
                    shard.red, shard.green, shard.blue, shard.alpha);
        }
        drawIfNotEmpty(buffer);
    }

    private static void renderCurrentMotes(Matrix4f matrix, float time,
                                           float dayOrbitAngle) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (CurrentMote mote : CURRENT_MOTES) {
            Vector3f direction = new Vector3f(mote.x, mote.y, mote.z)
                    .normalize()
                    .rotateY(dayOrbitAngle);
            Basis basis = basis(direction);
            float progress = positiveModulo(time * mote.speed + mote.phase, 1.0F);
            float along = (progress - 0.5F) * mote.length;
            float curve = (float) Math.sin(progress * Math.PI) * mote.curve;
            Vector3f base = new Vector3f(direction).mul(mote.distance)
                    .fma(along, basis.right)
                    .fma(curve, basis.up);
            float endpointFade = smoothStep(0.0F, 0.18F, progress)
                    * (1.0F - smoothStep(0.78F, 1.0F, progress));
            for (int trail = 0; trail < 3; trail++) {
                float back = trail * 0.14F;
                Vector3f point = new Vector3f(base)
                        .fma(-back, basis.right)
                        .fma(-(float) Math.sin(progress * Math.PI) * back * 0.18F, basis.up);
                float trailAlpha = mote.alpha * endpointFade * (1.0F - trail * 0.29F);
                if (trail == 0) {
                    addGlowingStar(buffer, matrix, point, basis.right, basis.up,
                            mote.size * 1.22F, 0.48F, 0.66F, 0.90F, trailAlpha * 1.45F);
                } else {
                    addBillboard(buffer, matrix, point, basis.right, basis.up,
                            mote.size * (1.12F - trail * 0.17F),
                            0.48F, 0.66F, 0.90F, trailAlpha * 1.28F);
                }
            }
        }
        drawIfNotEmpty(buffer);
    }

    private static Basis rotateBasis(Basis basis, float rotation) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        return new Basis(
                new Vector3f(basis.right).mul(cos).fma(sin, basis.up),
                new Vector3f(basis.up).mul(cos).fma(-sin, basis.right));
    }

    private static void triangle(BufferBuilder buffer, Matrix4f matrix,
                                 Vector3f first, Vector3f second, Vector3f third,
                                 float red, float green, float blue, float alpha) {
        VfxVertexWriter.vertex(buffer, matrix, first.x, first.y, first.z).setColor(red, green, blue, alpha);
        VfxVertexWriter.vertex(buffer, matrix, second.x, second.y, second.z).setColor(red, green, blue, alpha);
        VfxVertexWriter.vertex(buffer, matrix, third.x, third.y, third.z).setColor(red, green, blue, alpha);
    }

    private static void addPlaneQuad(BufferBuilder buffer, Matrix4f matrix,
                                     Vector3f center, Basis basis,
                                     float minRight, float minUp, float maxRight, float maxUp,
                                     float red, float green, float blue, float alpha) {
        Vector3f lowerLeft = planePoint(center, basis, minRight, minUp);
        Vector3f lowerRight = planePoint(center, basis, maxRight, minUp);
        Vector3f upperRight = planePoint(center, basis, maxRight, maxUp);
        Vector3f upperLeft = planePoint(center, basis, minRight, maxUp);
        triangle(buffer, matrix, lowerLeft, lowerRight, upperRight,
                red, green, blue, alpha);
        triangle(buffer, matrix, lowerLeft, upperRight, upperLeft,
                red, green, blue, alpha);
    }

    private static void addBrokenEllipse(BufferBuilder buffer, Matrix4f matrix,
                                         Vector3f center, Basis basis,
                                         float horizontalRadius, float verticalRadius,
                                         float rotation, float red, float green, float blue,
                                         float alpha, int segments, int gapEvery) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        Vector3f right = new Vector3f(basis.right).mul(cos).fma(sin, basis.up);
        Vector3f up = new Vector3f(basis.up).mul(cos).fma(-sin, basis.right);
        Basis rotated = new Basis(right, up);
        for (int segment = 0; segment < segments; segment++) {
            if ((segment + 2) % gapEvery == 0) {
                continue;
            }
            float a0 = segment * TAU / segments;
            float a1 = (segment + 1) * TAU / segments;
            line(buffer, matrix,
                    planePoint(center, rotated,
                            (float) Math.cos(a0) * horizontalRadius,
                            (float) Math.sin(a0) * verticalRadius),
                    planePoint(center, rotated,
                            (float) Math.cos(a1) * horizontalRadius,
                            (float) Math.sin(a1) * verticalRadius),
                    red, green, blue, alpha);
        }
    }

    private static void addShard(BufferBuilder buffer, Matrix4f matrix, float size,
                                 float red, float green, float blue, float alpha) {
        VfxVertexWriter.vertex(buffer, matrix, -size * 0.74F, -size * 0.22F, 0.0F)
                .setColor(red * 0.66F, green * 0.66F, blue * 0.72F, alpha);
        VfxVertexWriter.vertex(buffer, matrix, size * 0.86F, 0.0F, 0.0F)
                .setColor(red, green, blue, alpha);
        VfxVertexWriter.vertex(buffer, matrix, -size * 0.31F, size * 0.30F, size * 0.18F)
                .setColor(red * 0.82F, green * 0.82F, blue, alpha);

        VfxVertexWriter.vertex(buffer, matrix, -size * 0.54F, 0.0F, -size * 0.13F)
                .setColor(red * 0.58F, green * 0.58F, blue * 0.68F, alpha);
        VfxVertexWriter.vertex(buffer, matrix, size * 0.70F, size * 0.13F, 0.0F)
                .setColor(red, green, blue, alpha);
        VfxVertexWriter.vertex(buffer, matrix, -size * 0.17F, -size * 0.34F, size * 0.19F)
                .setColor(red * 0.76F, green * 0.76F, blue * 0.94F, alpha);
    }

    /** Low-poly irregular octahedron with face lighting, used for the physical asteroid belt. */
    private static void addRock(BufferBuilder buffer, Matrix4f matrix, float size,
                                float red, float green, float blue, float alpha) {
        Vector3f top = new Vector3f(0.08F, 0.73F, -0.05F).mul(size);
        Vector3f bottom = new Vector3f(-0.10F, -0.62F, 0.08F).mul(size);
        Vector3f left = new Vector3f(-0.91F, -0.05F, 0.02F).mul(size);
        Vector3f right = new Vector3f(0.79F, 0.09F, -0.04F).mul(size);
        Vector3f front = new Vector3f(0.02F, 0.04F, 0.68F).mul(size);
        Vector3f back = new Vector3f(-0.04F, -0.07F, -0.57F).mul(size);

        rockFace(buffer, matrix, top, front, right,
                red * 1.16F, green * 1.18F, blue * 1.24F, alpha);
        rockFace(buffer, matrix, top, left, front,
                red * 0.98F, green, blue * 1.08F, alpha);
        rockFace(buffer, matrix, top, back, left,
                red * 0.73F, green * 0.75F, blue * 0.84F, alpha);
        rockFace(buffer, matrix, top, right, back,
                red * 0.83F, green * 0.85F, blue * 0.92F, alpha);
        rockFace(buffer, matrix, bottom, right, front,
                red * 0.70F, green * 0.72F, blue * 0.79F, alpha);
        rockFace(buffer, matrix, bottom, front, left,
                red * 0.62F, green * 0.64F, blue * 0.72F, alpha);
        rockFace(buffer, matrix, bottom, left, back,
                red * 0.42F, green * 0.44F, blue * 0.52F, alpha);
        rockFace(buffer, matrix, bottom, back, right,
                red * 0.50F, green * 0.52F, blue * 0.60F, alpha);
    }

    private static void rockFace(BufferBuilder buffer, Matrix4f matrix,
                                 Vector3f first, Vector3f second, Vector3f third,
                                 float red, float green, float blue, float alpha) {
        triangle(buffer, matrix, first, second, third,
                Math.min(1.0F, red), Math.min(1.0F, green),
                Math.min(1.0F, blue), alpha);
    }

    private static void addBillboard(BufferBuilder buffer, Matrix4f matrix,
                                     Vector3f center, Vector3f right, Vector3f up,
                                     float size, float red, float green, float blue, float alpha) {
        addBillboardRect(buffer, matrix, center, right, up, size, size,
                red, green, blue, alpha);
    }

    private static void selectBloomShader() {
        if (ShaderPackCompatibility.useCustomCoreShaders()
                && CosmicVoidSkyShaders.bloomShader() != null) {
            RenderSystem.setShader(CosmicVoidSkyShaders::bloomShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            CosmicVoidTextureFiltering.bind(0, STAR_GLOW);
        }
    }

    /**
     * Builds the visible hemisphere in ordinary POSITION_TEX_COLOR vertices. Shader
     * packs can route this through their standard textured path without interpreting
     * the custom celestial shader's parameter-packed vertex colors.
     */
    private static void addCompatibleSphere(
            BufferBuilder buffer, Matrix4f matrix, Vector3f center, Basis basis,
            float halfSize, float red, float green, float blue, float alpha) {
        float bodyRadius = halfSize * 0.94F;
        float[] mesh = CosmicSkyMeshes.HEMISPHERE;
        for (int vertex = 0; vertex < mesh.length; vertex += 4) {
            float right = mesh[vertex] * bodyRadius;
            float up = mesh[vertex + 1] * bodyRadius;
            VfxVertexWriter.vertex(buffer, matrix,
                            center.x + right * basis.right.x + up * basis.up.x,
                            center.y + right * basis.right.y + up * basis.up.y,
                            center.z + right * basis.right.z + up * basis.up.z)
                    .setUv(mesh[vertex + 2], mesh[vertex + 3])
                    .setColor(red, green, blue, alpha);
        }
    }

    private static void addBillboardRect(BufferBuilder buffer, Matrix4f matrix,
                                         Vector3f center, Vector3f right, Vector3f up,
                                         float halfWidth, float halfHeight,
                                         float red, float green, float blue, float alpha) {
        for (int corner = 0; corner < 4; corner++) {
            float horizontal = corner == 0 || corner == 3 ? -halfWidth : halfWidth;
            float vertical = corner < 2 ? -halfHeight : halfHeight;
            float x = org.joml.Math.fma(vertical, up.x, org.joml.Math.fma(horizontal, right.x, center.x));
            float y = org.joml.Math.fma(vertical, up.y, org.joml.Math.fma(horizontal, right.y, center.y));
            float z = org.joml.Math.fma(vertical, up.z, org.joml.Math.fma(horizontal, right.z, center.z));
            VfxVertexWriter.vertex(buffer, matrix, x, y, z)
                    .setUv(corner == 0 || corner == 3 ? 0 : 1, corner < 2 ? 1 : 0)
                    .setColor(red, green, blue, alpha);
        }
    }

    private static void addTrailRibbon(
            BufferBuilder buffer, Matrix4f matrix,
            Vector3f tail, Vector3f head, float halfWidth,
            float red, float green, float blue, float alpha) {
        CosmicTrailGeometry.addCurvedRibbon(
                buffer, matrix, tail, head, halfWidth,
                red, green, blue, alpha);
    }

    private static void addGlowingStar(BufferBuilder buffer, Matrix4f matrix,
                                       Vector3f center, Vector3f right, Vector3f up,
                                       float size, float red, float green, float blue, float alpha) {
        addBillboard(buffer, matrix, center, right, up, size * 3.80F,
                red * 0.68F, green * 0.75F, blue, alpha * 0.43F);
        addBillboard(buffer, matrix, center, right, up, size * 1.96F,
                red, green, blue, alpha * 0.82F);
        addBillboard(buffer, matrix, center, right, up, size * 0.56F,
                1.0F, 0.99F, 1.0F, Math.min(1.0F, alpha * 2.20F));
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             Vector3f start, Vector3f end,
                             float red, float green, float blue, float alpha) {
        VfxVertexWriter.vertex(buffer, matrix, start.x, start.y, start.z).setColor(red, green, blue, alpha);
        VfxVertexWriter.vertex(buffer, matrix, end.x, end.y, end.z).setColor(red, green, blue, alpha);
    }

    private static void drawIfNotEmpty(BufferBuilder buffer) {
        var mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    private static Vector3f planePoint(Vector3f center, Basis basis, float right, float up) {
        return new Vector3f(center).fma(right, basis.right).fma(up, basis.up);
    }

    private static Basis basis(Vector3f direction) {
        Vector3f reference = Math.abs(direction.y) > 0.88F
                ? new Vector3f(1.0F, 0.0F, 0.0F)
                : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = direction.cross(reference, new Vector3f()).normalize();
        Vector3f up = right.cross(direction, new Vector3f()).normalize();
        return new Basis(right, up);
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float x = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return x * x * (3.0F - 2.0F * x);
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }

    private static float pseudoRandom(float seed) {
        double value = Math.sin(seed * 12.9898D + 78.233D) * 43758.5453D;
        return (float) (value - Math.floor(value));
    }

    private static List<NebulaSheet> createNebulaSheets() {
        Random random = new Random(0x4E4542554C41454CL);
        List<NebulaSheet> sheets = new ArrayList<>();
        float[][] colors = {
                {0.17F, 0.22F, 0.38F},
                {0.25F, 0.14F, 0.34F},
                {0.12F, 0.27F, 0.32F},
                {0.28F, 0.22F, 0.30F}
        };
        for (int index = 0; index < 4; index++) {
            Vector3f direction = direction(
                    0.42F + index * 1.47F + random.nextFloat() * 0.24F,
                    -0.52F + random.nextFloat() * 1.04F);
            sheets.add(new NebulaSheet(direction.x, direction.y, direction.z,
                    126.0F + random.nextFloat() * 54.0F,
                    42.0F + random.nextFloat() * 24.0F,
                    17.0F + random.nextFloat() * 15.0F,
                    0.018F + random.nextFloat() * 0.036F,
                    (random.nextFloat() - 0.5F) * 0.000018F,
                    0.000018F + random.nextFloat() * 0.000027F,
                    1.8F + random.nextFloat() * 2.4F,
                    colors[index][0], colors[index][1], colors[index][2],
                    random.nextFloat() * TAU));
        }
        return List.copyOf(sheets);
    }

    private static List<DistantPlanet> createDistantPlanets() {
        // Orbital distance uses ln(1 + AU) over a 4.55-radian ecliptic arc. Visual radius
        // uses 3 * pow(diameter / Earth diameter, 0.4), preserving real ordering while
        // keeping Mercury visible and the Sun on-screen. Earth itself is rendered above.
        return List.of(
                distantPlanet(MERCURY, 5.207F, 0.10F, 220.0F, 2.04F,
                        -0.12F, 0.11F, 0.72F, 0.16F, 0.02F),
                distantPlanet(VENUS, 4.920F, -0.05F, 220.0F, 2.94F,
                        0.16F, 0.34F, 0.22F, 0.34F, 0.03F),
                distantPlanet(MOON, 4.665F, 0.045F, 220.0F, 1.78F,
                        -0.24F, 0.57F, 0.62F, 0.16F, 0.01F),
                distantPlanet(MARS, 4.414F, 0.03F, 220.0F, 2.33F,
                        0.09F, 0.72F, 0.67F, 0.22F, 0.02F),
                distantPlanet(JUPITER, 3.223F, -0.02F, 220.0F, 7.82F,
                        -0.08F, 0.28F, 0.88F, 0.30F, 0.03F),
                ringedPlanet(SATURN, SATURN_RING, 2.522F, 0.04F, 220.0F, 7.27F,
                        0.13F, 0.44F, 0.75F, 0.24F, 0.03F,
                        1.13F, 2.36F, 0.30F, -0.19F, 0.92F),
                ringedPlanet(URANUS, URANUS_RING, 1.661F, -0.01F, 220.0F, 5.21F,
                        -0.31F, 0.83F, 0.40F, 0.40F, 0.03F,
                        1.24F, 2.06F, 0.22F, 0.72F, 0.82F),
                distantPlanet(NEPTUNE, 1.090F, 0.03F, 220.0F, 5.15F,
                        0.21F, 0.66F, 0.56F, 0.48F, 0.03F),
                distantPlanet(SUN, 5.920F, -0.04F, 220.0F, 19.61F,
                        -0.06F, 0.91F, 0.93F, 1.0F, 1.0F));
    }

    private static DistantPlanet distantPlanet(ResourceLocation texture,
                                                float yaw, float pitch, float distance,
                                                float halfSize, float rotation,
                                                float texturePhase, float rotationSpeed,
                                                float edgeGlow, float emissive) {
        Vector3f direction = direction(yaw, pitch);
        return new DistantPlanet(direction.x, direction.y, direction.z,
                distance, halfSize, rotation,
                0.000004F + texturePhase * 0.000002F,
                0.28F + texturePhase * 0.25F, texturePhase * TAU,
                texture, null, texturePhase, rotationSpeed, edgeGlow, emissive,
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static DistantPlanet ringedPlanet(ResourceLocation texture,
                                               ResourceLocation ringTexture,
                                               float yaw, float pitch, float distance,
                                               float halfSize, float rotation,
                                               float texturePhase, float rotationSpeed,
                                               float edgeGlow, float emissive,
                                               float ringInnerScale, float ringOuterScale,
                                               float ringFlatten, float ringRotation,
                                               float ringAlpha) {
        Vector3f direction = direction(yaw, pitch);
        return new DistantPlanet(direction.x, direction.y, direction.z,
                distance, halfSize, rotation,
                0.000004F + texturePhase * 0.000002F,
                0.28F + texturePhase * 0.25F, texturePhase * TAU,
                texture, ringTexture, texturePhase, rotationSpeed, edgeGlow, emissive,
                ringInnerScale, ringOuterScale, ringFlatten, ringRotation, ringAlpha);
    }

    private static List<DistantRelic> createDistantRelics() {
        Random random = new Random(0x52454C4943564F49L);
        List<DistantRelic> relics = new ArrayList<>();
        float[][] colors = {
                {0.56F, 0.61F, 0.78F},
                {0.43F, 0.25F, 0.70F},
                {0.54F, 0.45F, 0.30F},
                {0.30F, 0.25F, 0.45F},
                {0.69F, 0.52F, 0.25F}
        };
        for (int kind = 0; kind < 5; kind++) {
            Vector3f direction = direction(
                    0.78F + kind * 1.13F + random.nextFloat() * 0.30F,
                    -0.58F + random.nextFloat() * 1.16F);
            relics.add(new DistantRelic(direction.x, direction.y, direction.z,
                    148.0F + random.nextFloat() * 96.0F,
                    6.2F + random.nextFloat() * 7.4F,
                    0.46F + random.nextFloat() * 0.22F,
                    (random.nextFloat() - 0.5F) * 0.000034F,
                    0.000018F + random.nextFloat() * 0.000032F,
                    0.55F + random.nextFloat() * 1.25F,
                    colors[kind][0], colors[kind][1], colors[kind][2],
                    random.nextFloat() * TAU, kind));
        }
        return List.copyOf(relics);
    }

    private static List<PlanetAsteroid> createPlanetAsteroids() {
        Random random = new Random(0x41535445524F4944L);
        List<PlanetAsteroid> asteroids = new ArrayList<>();
        for (int index = 0; index < 104; index++) {
            float radius = 36.0F + random.nextFloat() * 25.0F;
            float size = 0.22F + (float) Math.pow(random.nextFloat(), 2.15D) * 2.55F;
            // A few large foreground rocks establish scale without filling the whole belt.
            if (index % 31 == 0) {
                size += 2.7F + random.nextFloat() * 2.2F;
            }
            float warmth = random.nextFloat();
            asteroids.add(new PlanetAsteroid(
                    random.nextFloat() * TAU,
                    radius,
                    (random.nextFloat() - 0.5F) * (2.2F + radius * 0.035F),
                    (random.nextFloat() - 0.5F) * 2.6F,
                    size,
                    0.000040F + random.nextFloat() * 0.000050F,
                    (random.nextFloat() - 0.5F) * 0.0017F,
                    (random.nextFloat() - 0.5F) * 0.0019F,
                    (random.nextFloat() - 0.5F) * 0.0015F,
                    0.18F + warmth * 0.10F,
                    0.19F + warmth * 0.08F,
                    0.23F + warmth * 0.07F,
                    0.72F + random.nextFloat() * 0.25F,
                    random.nextFloat() * TAU));
        }
        return List.copyOf(asteroids);
    }

    private static List<StarCluster> createStarClusters() {
        Random random = new Random(0x53544152434C5553L);
        List<StarCluster> clusters = new ArrayList<>();
        for (int clusterIndex = 0; clusterIndex < 9; clusterIndex++) {
            float yaw = random.nextFloat() * TAU;
            float pitch = -0.70F + random.nextFloat() * 1.40F;
            Vector3f direction = direction(yaw, pitch);
            List<ClusterStar> stars = new ArrayList<>();
            int count = 10 + random.nextInt(9);
            for (int starIndex = 0; starIndex < count; starIndex++) {
                float angle = random.nextFloat() * TAU;
                float radius = (float) Math.sqrt(random.nextFloat()) * (1.3F + random.nextFloat() * 1.3F);
                stars.add(new ClusterStar(
                        (float) Math.cos(angle) * radius,
                        (float) Math.sin(angle) * radius * (0.38F + random.nextFloat() * 0.42F),
                        0.040F + random.nextFloat() * 0.16F,
                        0.009F + random.nextFloat() * 0.018F,
                        random.nextFloat() * TAU));
            }
            clusters.add(new StarCluster(direction.x, direction.y, direction.z,
                    55.0F + random.nextFloat() * 14.0F,
                    0.26F + random.nextFloat() * 0.20F,
                    0.0012F + random.nextFloat() * 0.0012F,
                    0.0010F + random.nextFloat() * 0.0008F,
                    0.42F + random.nextFloat() * 0.62F,
                    random.nextFloat() * TAU,
                    List.copyOf(stars)));
        }
        return List.copyOf(clusters);
    }

    private static List<DebrisShard> createDebris() {
        Random random = new Random(0x564F494444454252L);
        List<DebrisShard> shards = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            float yaw = random.nextFloat() * TAU;
            float pitch = -0.82F + random.nextFloat() * 1.52F;
            Vector3f direction = direction(yaw, pitch);
            boolean pale = index % 6 == 0;
            shards.add(new DebrisShard(direction.x, direction.y, direction.z,
                    22.0F + random.nextFloat() * 32.0F,
                    0.22F + random.nextFloat() * (pale ? 0.88F : 0.60F),
                    0.21F + random.nextFloat() * 0.43F,
                    0.00035F + random.nextFloat() * 0.00075F,
                    0.0008F + random.nextFloat() * 0.0018F,
                    (random.nextFloat() - 0.5F) * 0.0020F,
                    (random.nextFloat() - 0.5F) * 0.0017F,
                    pale ? 0.48F : 0.16F,
                    pale ? 0.47F : 0.10F,
                    pale ? 0.58F : 0.25F,
                    pale ? 0.40F : 0.34F,
                    random.nextFloat() * TAU));
        }
        return List.copyOf(shards);
    }

    private static List<CurrentMote> createCurrentMotes() {
        Random random = new Random(0x43555252454E5453L);
        List<CurrentMote> motes = new ArrayList<>();
        // Twelve loose currents with ten motes each: readable flow patches, not uniform screen noise.
        for (int patch = 0; patch < 12; patch++) {
            float patchYaw = random.nextFloat() * TAU;
            float patchPitch = -0.78F + random.nextFloat() * 1.46F;
            for (int index = 0; index < 10; index++) {
                Vector3f direction = direction(
                        patchYaw + (random.nextFloat() - 0.5F) * 0.19F,
                        patchPitch + (random.nextFloat() - 0.5F) * 0.14F);
                float length = 25.0F + random.nextFloat() * 80.0F;
                float worldSpeed = 0.005F + random.nextFloat() * 0.025F;
                motes.add(new CurrentMote(direction.x, direction.y, direction.z,
                        40.0F + random.nextFloat() * 210.0F,
                        length,
                        (random.nextFloat() - 0.5F) * 24.0F,
                        worldSpeed / length,
                        0.05F + random.nextFloat() * 0.25F,
                        0.15F + random.nextFloat() * 0.50F,
                        random.nextFloat()));
            }
        }
        return List.copyOf(motes);
    }

    private static Vector3f direction(float yaw, float pitch) {
        float cosPitch = (float) Math.cos(pitch);
        return new Vector3f(
                cosPitch * (float) Math.cos(yaw),
                (float) Math.sin(pitch),
                cosPitch * (float) Math.sin(yaw));
    }

    private static ResourceLocation environmentTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/environment/" + name + ".png");
    }

    private record Basis(Vector3f right, Vector3f up) {
    }

    private record NebulaSheet(float x, float y, float z, float distance,
                               float halfWidth, float halfHeight, float alpha,
                               float rotationSpeed, float driftSpeed, float driftRadius,
                               float red, float green, float blue, float phase) {
    }

    private record DistantPlanet(float x, float y, float z, float distance,
                                 float halfSize, float rotation, float driftSpeed,
                                 float driftRadius, float phase,
                                 ResourceLocation texture, ResourceLocation ringTexture,
                                 float texturePhase, float rotationSpeed,
                                 float edgeGlow, float emissive,
                                 float ringInnerScale, float ringOuterScale,
                                 float ringFlatten, float ringRotation, float ringAlpha) {
    }

    private record DistantRelic(float x, float y, float z, float distance, float size,
                                float alpha, float rotationSpeed, float driftSpeed,
                                float driftRadius, float red, float green, float blue,
                                float phase, int kind) {
    }

    private record PlanetAsteroid(float angle, float radius, float height,
                                  float depthOffset, float size, float orbitSpeed,
                                  float spinX, float spinY, float spinZ,
                                  float red, float green, float blue, float alpha,
                                  float phase) {
    }

    private record ClusterStar(float offsetX, float offsetY, float size,
                               float twinkleSpeed, float phase) {
    }

    private record StarCluster(float x, float y, float z, float distance, float alpha,
                               float fadeSpeed, float driftSpeed, float driftRadius,
                               float phase, List<ClusterStar> stars) {
    }

    private record StarClusterFrame(Vector3f center, Basis basis) {
    }

    private record DebrisShard(float x, float y, float z, float distance, float size,
                               float driftRadius, float driftSpeed,
                               float spinX, float spinY, float spinZ,
                               float red, float green, float blue, float alpha,
                               float phase) {
    }

    private record CurrentMote(float x, float y, float z, float distance, float length,
                               float curve, float speed, float size, float alpha,
                               float phase) {
    }

}
