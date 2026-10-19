package com.example.immortal_cultivation_mod.alchemy;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server policy. Each stage defines TOTAL multipliers, never incremental modifiers. */
public final class PillToxicityConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BASE, ABOVE, BELOW, REPEAT, RECOVERY, MEDITATION, DAMAGE, DETOX;
    public static final ModConfigSpec.IntValue WINDOW, REPEAT_CAP, DAMAGE_SECONDS, DETOX_SECONDS, DETOX_REALM, DAMAGE_PAUSE;
    public static final ModConfigSpec.IntValue[] THRESHOLDS = new ModConfigSpec.IntValue[4];
    public static final ModConfigSpec.DoubleValue[] RESTORE = new ModConfigSpec.DoubleValue[5], BENEFIT = new ModConfigSpec.DoubleValue[5], NATURAL = new ModConfigSpec.DoubleValue[5], CULTIVATION = new ModConfigSpec.DoubleValue[5], COST = new ModConfigSpec.DoubleValue[5];
    public static final Map<String, ModConfigSpec.DoubleValue> POTENCY = new LinkedHashMap<>(), QUALITY = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.IntValue> REALM = new LinkedHashMap<>();
    static {
        var b = new ModConfigSpec.Builder();
        BASE = b.defineInRange("baseGain", 4., 0., 100.);
        ABOVE = b.comment("Multiplier per major realm ABOVE the player.").defineInRange("aboveRealmFactor", 1.75, 1., 10.);
        BELOW = b.comment("Multiplier per major realm BELOW the player.").defineInRange("belowRealmFactor", .85, .01, 1.);
        WINDOW = b.defineInRange("repeatWindowSeconds", 120, 1, 86400);
        REPEAT = b.defineInRange("repeatExtraPerUse", .35, 0., 10.);
        REPEAT_CAP = b.defineInRange("repeatCountCap", 5, 1, 100);
        RECOVERY = b.comment("Points removed per online second, never offline.").defineInRange("recoveryPerSecond", .01, 0., 100.);
        MEDITATION = b.comment("Additional points removed per uninterrupted meditation second.").defineInRange("meditationBonusPerSecond", .04, 0., 100.);
        DAMAGE_PAUSE = b.defineInRange("damageMeditationPauseSeconds", 5, 0, 3600);
        DAMAGE = b.defineInRange("severeDamageMaxHpPercent", 1., 0., 25.);
        DAMAGE_SECONDS = b.defineInRange("severeDamageIntervalSeconds", 10, 1, 3600);
        DETOX = b.defineInRange("detoxPillRemoval", 25., 0., 100.);
        DETOX_SECONDS = b.defineInRange("detoxCooldownSeconds", 300, 1, 86400);
        DETOX_REALM = b.comment("0=mortal, 1=Qi Refining, 2=Foundation, ... 9=Tribulation.").defineInRange("detoxMinimumRealm", 1, 0, 9);
        b.push("quality");
        QUALITY.put("common", b.defineInRange("common", 1., 0., 10.));
        QUALITY.put("fine", b.defineInRange("fine", .75, 0., 10.));
        QUALITY.put("superior", b.defineInRange("superior", .5, 0., 10.));
        b.pop();
        double[] restores={1,.9,.75,.6,.4}, benefits={1,1,.9,.8,.65}, natural={1,1,.85,.7,.5}, cultivation={1,1,1,.85,.65}, cost={1,1,1,1.15,1.3};
        b.push("stages");
        for(int i=0;i<5;i++) {
            b.push("stage"+i);
            if(i>0) THRESHOLDS[i-1]=b.comment("Effective thresholds are sorted and strictly increasing.").defineInRange("threshold", i*20, 1, 100);
            RESTORE[i]=b.defineInRange("pillHealingAndQiMultiplier", restores[i], i==0?1.:0., 1.);
            BENEFIT[i]=b.comment("Other numeric alchemy benefits; activation chances, permanent legacy unlocks and fixed vanilla effects are preserved.").defineInRange("otherPillBenefitMultiplier", benefits[i], i==0?1.:0., 1.);
            NATURAL[i]=b.defineInRange("naturalQiMultiplier", natural[i], i==0?1.:0., 1.);
            CULTIVATION[i]=b.defineInRange("cultivationMultiplier", cultivation[i], i==0?1.:0., 1.);
            COST[i]=b.defineInRange("spellCostMultiplier", cost[i], 1., i==0?1.:10.);
            b.pop();
        }
        b.pop(); b.push("pills");
        for(var f:AlchemyChemistry.FORMULAS) profile(b, f.id(), switch(f.id()){case "juling","xumai","tiegu"->1.5;case "pozhang","bixie","yangshen"->1.25;default->1.;}, 1);
        profile(b,"enlightenment_pill", .5, 0); profile(b,"breakthrough_pill", 2., 2);
        profile(b,"body_tempering_pill", 1.25, 1); profile(b,"blood_qi_pill", 1.25, 1); profile(b,"bigu_pill", .5, 0);
        b.pop(); SPEC=b.build();
    }
    private static void profile(ModConfigSpec.Builder b,String id,double potency,int realm) {
        b.push(id); POTENCY.put(id,b.defineInRange("potency",potency,0.,25.)); REALM.put(id,b.defineInRange("realm",realm,0,9)); b.pop();
    }
    public static CompoundTag snapshot() {
        var t=new CompoundTag();
        t.putDouble("Base",read(BASE));t.putDouble("Above",read(ABOVE));t.putDouble("Below",read(BELOW));t.putDouble("Repeat",read(REPEAT));
        t.putInt("Window",read(WINDOW));t.putInt("Cap",read(REPEAT_CAP));t.putDouble("Recovery",read(RECOVERY));t.putDouble("Meditation",read(MEDITATION));
        t.putDouble("Damage",read(DAMAGE));t.putInt("DamageSeconds",read(DAMAGE_SECONDS));t.putDouble("Detox",read(DETOX));t.putInt("DetoxSeconds",read(DETOX_SECONDS));t.putInt("DetoxRealm",read(DETOX_REALM));
        int previous=0;
        for(int i=0;i<5;i++) {
            var s=new CompoundTag();
            int threshold=i==0?0:Math.min(100-(4-i), Math.max(previous+1,read(THRESHOLDS[i-1]))); previous=threshold;
            s.putInt("Threshold",threshold);s.putDouble("Restore",read(RESTORE[i]));s.putDouble("Benefit",read(BENEFIT[i]));s.putDouble("Natural",read(NATURAL[i]));s.putDouble("Cultivation",read(CULTIVATION[i]));s.putDouble("Cost",read(COST[i]));t.put("Stage"+i,s);
        }
        var q=new CompoundTag();QUALITY.forEach((id,v)->q.putDouble(id,read(v)));t.put("Quality",q);
        q.putDouble("fine",Math.min(q.getDouble("common"),q.getDouble("fine")));
        q.putDouble("superior",Math.min(q.getDouble("fine"),q.getDouble("superior")));
        var profiles=new CompoundTag();POTENCY.forEach((id,v)->{var p=new CompoundTag();p.putDouble("Potency",read(v));p.putInt("Realm",read(REALM.get(id)));profiles.put(id,p);});t.put("Pills",profiles);
        return t;
    }
    private static <T> T read(ModConfigSpec.ConfigValue<T> value) {return SPEC.isLoaded()?value.get():value.getDefault();}
    private PillToxicityConfig() {}
}
