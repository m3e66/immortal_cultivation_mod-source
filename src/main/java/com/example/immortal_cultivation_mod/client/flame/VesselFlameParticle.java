package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.flame.FlameKind;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** One shared group bounds capture bursts, projectile trails, and ambient essence motes together. */
public final class VesselFlameParticle extends TextureSheetParticle {
    private VesselFlameParticle(ClientLevel level, double x, double y, double z, double dx, double dy,
            double dz, SpriteSet sprites, FlameKind kind) {
        super(level, x, y, z);
        xd = dx;
        yd = dy + (kind == FlameKind.NETHER ? -0.008 : kind == FlameKind.EARTH ? 0.006 : 0.012);
        zd = dz;
        hasPhysics = false;
        friction = 0.94F;
        lifetime = (kind == FlameKind.NETHER || kind == FlameKind.WOOD ? 22 : 16) + random.nextInt(9);
        quadSize = 0.07F + random.nextFloat() * 0.035F;
        // Each provider has its own precolored sprite; a second tint would alter the artwork.
        setColor(1.0F, 1.0F, 1.0F);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 0.85F * (1 - (float) age / lifetime);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0, 1);
        return quadSize * (1 - progress * 0.45F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return FlameRenderTypes.PARTICLE;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.of(FlameClientEffects.PARTICLE_GROUP);
    }

    // Vanilla SingleQuadParticle adds a light attribute; our fullbright format has no lightmap.
    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf rotation, float x, float y,
            float z, float partialTick) {
        float size = getQuadSize(partialTick);
        vertex(buffer, rotation, x, y, z, 1, -1, size, getU1(), getV1());
        vertex(buffer, rotation, x, y, z, 1, 1, size, getU1(), getV0());
        vertex(buffer, rotation, x, y, z, -1, 1, size, getU0(), getV0());
        vertex(buffer, rotation, x, y, z, -1, -1, size, getU0(), getV1());
    }

    private void vertex(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z,
            float offsetX, float offsetY, float size, float u, float v) {
        Vector3f point = new Vector3f(offsetX, offsetY, 0).rotate(rotation).mul(size).add(x, y, z);
        buffer.addVertex(point.x(), point.y(), point.z()).setUv(u, v).setColor(rCol, gCol, bCol, alpha);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final FlameKind kind;

        public Provider(SpriteSet sprites, FlameKind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                double z, double dx, double dy, double dz) {
            if (!FlameClientEffects.admitParticle(x, y, z)) return null;
            return new VesselFlameParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
