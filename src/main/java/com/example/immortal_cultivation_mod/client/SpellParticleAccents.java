package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/** Bounded secondary spell accents using credited, unchanged Elden Ring library art. */
final class SpellParticleAccents {
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static long tick = Long.MIN_VALUE;
    private static int remaining;
    private SpellParticleAccents() {}

    static void trail(String spell, Vec3 position, Vec3 velocity, long salt, double radius) {
        var level = Minecraft.getInstance().level;
        if (level != null && Math.floorMod(level.getGameTime() + salt, 3) == 0)
            emit(spell, position, velocity.scale(-.07), 2, Math.min(1.8, Math.max(.1, radius)));
    }
    static void burst(String spell, Vec3 position, Vec3 direction, double radius) {
        emit(spell, position, direction.scale(.035), 10, Math.min(2.5, Math.max(.2, radius)));
    }
    private static void emit(String id, Vec3 origin, Vec3 drift, int count, double radius) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !Double.isFinite(radius)
                || origin.distanceToSqr(mc.player.position()) > 48*48
                || mc.options.particles().get() == ParticleStatus.MINIMAL) return;
        if (world.get() != level || tick != level.getGameTime()) {
            world = new WeakReference<>(level); tick = level.getGameTime();
            remaining = mc.options.particles().get() == ParticleStatus.DECREASED ? 10 : 32;
        }
        String spell = switch (id) {
            case "ball" -> "light_beam_attack";
            case "flying_sword" -> "liuguang_jianying";
            case "water1", "water2", "water3", "puddle" -> "sliding_water";
            case "ice1", "ice2" -> "bingzhui_yu";
            case "beam" -> "beam";
            default -> id;
        };
        var definition = ModSpells.get(spell);
        String element = definition == null ? "" : definition.element();
        SimpleParticleType type;
        boolean blood = element.contains("血") || spell.startsWith("xue");
        boolean thunder = element.contains("雷") || spell.contains("lei");
        if (spell.contains("kong") || element.contains("暗")) type = ModParticles.SPELL_VOID.get();
        else if (element.contains("火") || spell.equals("fireball")) type = ModParticles.AMBIENT_EMBER.get();
        else if (element.contains("冰") || element.contains("水") || id.startsWith("ice") || id.startsWith("water") || id.equals("puddle")) type = ModParticles.AMBIENT_FROST.get();
        else if (element.contains("木")) type = ModParticles.AMBIENT_LEAF.get();
        else if (element.contains("土") || spell.equals("zhenshan_palm")) type = ModParticles.AMBIENT_ASH.get();
        else if (thunder || spell.contains("jian") || spell.equals("lingbeng")) type = ModParticles.SPELL_SPARK.get();
        else type = ModParticles.AMBIENT_SPIRIT.get();
        for (int i=0; i<count && remaining>0; i++,remaining--) {
            var random = level.random;
            Vec3 offset = new Vec3(random.nextDouble()-.5, random.nextDouble()-.5, random.nextDouble()-.5).scale(radius);
            Vec3 p = origin.add(offset);
            Vec3 motion = drift.add(offset.scale(.04)).add(0,.008,0);
            var particle = mc.particleEngine.createParticle(type, p.x,p.y,p.z,motion.x,motion.y,motion.z);
            if (particle != null) {
                particle.setLifetime(18+random.nextInt(15));
                if (blood) particle.setColor(1F,.1F,.18F);
                else if (thunder) particle.setColor(.8F,.5F,1F);
            }
        }
    }
}
