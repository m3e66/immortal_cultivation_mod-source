package com.example.immortal_cultivation_mod.client.particle;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only ambient accents. Original MIT particle textures by shuimo0413. */
public final class EnvironmentParticle extends TextureSheetParticle {
    public enum Kind { ASH, LEAF, FROST, EMBER, SPIRIT }
    private final Kind kind;
    private final float size;
    private final float phase;

    private EnvironmentParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, 0, 0, 0);
        this.kind = kind;
        pickSprite(sprites);
        lifetime = 35 + random.nextInt(30);
        size = switch (kind) {
            case FROST -> .3F + random.nextFloat() * .2F;
            case LEAF -> .09F + random.nextFloat() * .05F;
            // Mote art occupies only 3-5 pixels of its untouched 32px canvas.
            default -> .22F + random.nextFloat() * .12F;
        };
        quadSize = size;
        alpha = 0;
        phase = random.nextFloat() * 6.283185F;
        xd = vx; yd = vy; zd = vz;
        hasPhysics = true;
        friction = .985F;
        gravity = kind == Kind.LEAF ? .012F : 0;
        setSize(.03F, .03F);
    }

    @Override public void tick() {
        super.tick();
        float progress = age / (float) lifetime;
        alpha = Math.min(1, age / 7F) * (1 - progress) * (kind == Kind.FROST ? .32F : .7F);
        quadSize = size * (kind == Kind.FROST ? 1 + progress * .6F : 1);
        if (kind == Kind.LEAF) {
            oRoll = roll;
            roll += .025F;
            xd += Math.sin(age * .15 + phase) * .0008;
        }
        if (kind == Kind.EMBER || kind == Kind.SPIRIT || kind == Kind.ASH) yd += .00025;
    }

    @Override protected int getLightColor(float partialTick) {
        return kind == Kind.EMBER || kind == Kind.SPIRIT ? 15728880 : super.getLightColor(partialTick);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPELL_SPARK.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.SPIRIT));
        event.registerSpriteSet(ModParticles.SPELL_VOID.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.SPIRIT));
        event.registerSpriteSet(ModParticles.AMBIENT_ASH.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.ASH));
        event.registerSpriteSet(ModParticles.AMBIENT_LEAF.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.LEAF));
        event.registerSpriteSet(ModParticles.AMBIENT_FROST.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.FROST));
        event.registerSpriteSet(ModParticles.AMBIENT_EMBER.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.EMBER));
        event.registerSpriteSet(ModParticles.AMBIENT_SPIRIT.get(), s -> (t,l,x,y,z,vx,vy,vz) -> new EnvironmentParticle(l,x,y,z,vx,vy,vz,s,Kind.SPIRIT));
    }
}
