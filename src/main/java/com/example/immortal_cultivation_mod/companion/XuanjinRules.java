package com.example.immortal_cultivation_mod.companion;

/** Shared balance rules for both the sealed passenger and its manifested body. */
public final class XuanjinRules {
    public static final int POSSESSION_TICKS=5*60*20;
    public static final double CLAW_RANGE=12,ROAR_RANGE=32,ROAR_SPREAD=.5;
    public static final int ROAR_TRAVEL_TICKS=32,ROAR_RINGS=10,ROAR_RING_DELAY=2,ROAR_FADE_TICKS=6;
    public static final int ROAR_EFFECT_TICKS=ROAR_TRAVEL_TICKS+(ROAR_RINGS-1)*ROAR_RING_DELAY+ROAR_FADE_TICKS;
    public static double roarRadius(double distance){return .5+Math.clamp(distance,0,ROAR_RANGE)*ROAR_SPREAD;}
    public static boolean inRoarCone(net.minecraft.world.phys.Vec3 origin,net.minecraft.world.phys.Vec3 forward,
                                     net.minecraft.world.phys.Vec3 target,double targetRadius) {
        var offset=target.subtract(origin);double axial=offset.dot(forward.normalize());
        if(axial<0 || axial>ROAR_RANGE)return false;
        double radius=roarRadius(axial)+targetRadius;
        return Math.max(0,offset.lengthSqr()-axial*axial)<=radius*radius;
    }
    public record Stage(int realmStage, float health, float attack, double armor, int energy, float scale) {}
    public static final Stage[] STAGES = {
        new Stage(3, 120, 20, 8, 100, .23F),
        new Stage(4, 250, 40, 15, 250, .32F),
        new Stage(6, 700, 100, 30, 500, .42F),
        new Stage(8, 2500, 300, 60, 1000, .53F),
        new Stage(9, 4000, 500, 90, 1600, .65F),
        new Stage(12, 22000, 1800, 180, 4000, .82F),
        new Stage(13, 50000, 4500, 350, 10000, 1F)
    };
    private XuanjinRules() {}
    public static Stage stage(int seal) { return STAGES[Math.clamp(seal, 0, 6)]; }
    public static int unlockTrust(int nextSeal) {
        return switch(Math.clamp(nextSeal,1,6)) {
            case 1 -> 40; case 2 -> 55; case 3 -> 70;
            case 4 -> 80; case 5 -> 90; default -> 100;
        };
    }
    public static int unlockEnergy(int nextSeal) { return stage(Math.clamp(nextSeal,1,6)-1).energy(); }
    public static int regeneration(int seal) { return Math.max(1,stage(seal).energy()/100); }
    public static int cooldownTicks(String action) {
        return switch(action) {
            case "manifest" -> 300;
            case "possess","vortex" -> 600;
            case "eyes","claw" -> 100;
            case "reach" -> 20;
            case "voice","devour" -> 150;
            case "tails" -> 60;
            case "dash" -> 30;
            case "feed" -> 1200;
            default -> 0;
        };
    }
    public static String cooldownSeconds(String action) {
        int ticks=cooldownTicks(action);
        return ticks%20==0?Integer.toString(ticks/20):Float.toString(ticks/20F);
    }
    public static float auraDamage(int realm, boolean spirit) {
        if (realm >= 6) return 0;
        float base = switch (realm) { case 2 -> .35F; case 3 -> .25F; case 4 -> .15F; case 5 -> .05F; default -> .40F; };
        return base + (spirit ? .15F : 0);
    }
    public static float auraSpeed(int realm) {
        return switch (realm) { case 0, 1 -> .30F; case 2 -> .25F; case 3 -> .20F; case 4 -> .10F; default -> 0; };
    }
    public static float refusal(int trust, int freedom) {
        return freedom < 70 || trust >= 70 ? 0 : Math.min(.65F, (freedom - trust) / 140F);
    }
    public static boolean fullEnergy(boolean previous, int energy) {
        return previous ? energy >= 7500 : energy >= 10000;
    }
    public static float resistance(String element) {
        return switch (element) {
            case "fire" -> .70F; case "water", "ice" -> 1.10F; case "metal" -> .90F;
            case "earth", "wind" -> .85F; case "lightning" -> .30F; case "yang", "light" -> 1.20F;
            case "yin", "dark" -> .60F; case "soul" -> .50F; case "poison", "fall" -> 0; default -> 1F;
        };
    }
    public static int lightningEnergy(float prevented) { return Math.min(2000, (int)(prevented * .5F)); }
    public static float absorbFraction(String kind, int enemyRealm) {
        float fraction = switch (kind) { case "lightning" -> 1F; case "projectile" -> .80F; case "aoe" -> .60F; default -> 0F; };
        return enemyRealm >= 6 ? Math.min(.5F, fraction) : fraction;
    }
}
