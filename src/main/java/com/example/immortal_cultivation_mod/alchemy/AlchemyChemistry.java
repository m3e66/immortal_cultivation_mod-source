package com.example.immortal_cultivation_mod.alchemy;

import java.util.*;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.Effect.*;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.Element.*;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.Trait.*;

public final class AlchemyChemistry {
    public enum Element { METAL, WOOD, WATER, FIRE, EARTH, THUNDER, ICE, WIND, YANG, YIN }
    public enum Trait { HARMONIZE, PURIFY, LOCK_QI, CATALYZE, KINDLE, CONDENSE_WATER }
    public enum Effect {
        HEALTH, QI, CULTIVATION, SENSE, ATTACK, DEFENSE, SPEED, DURATION,
        FIRE_DAMAGE, COLD_RESIST, FIRE_RESIST, KNOCKBACK_RESIST, CULTIVATION_GAIN,
        BREAKTHROUGH, DETOX, STABILITY, TOXICITY, CLEAR_MIND, CLEAR_FIRE, QI_MULTIPLIER,
        NIGHT_VISION, MIND_SHIELD, WATER_BREATHING, NEXT_MELEE, EMERGENCY_REGEN,
        IRON_BONES, WITHER_SHIELD, NEXT_QI_SPELL
    }
    public record Dose(double amount, int ticks) {
        public Dose {
            if (!Double.isFinite(amount) || amount < 0 || ticks < 0 || ticks > 72000)
                throw new IllegalArgumentException("Invalid medicinal dose");
        }
    }
    public record Plant(Map<Element, Double> elements, Map<Effect, Double> effects,
                        double strength, double stability, Set<Trait> traits) {
        public Plant { elements = Map.copyOf(elements); effects = Map.copyOf(effects); traits = Set.copyOf(traits); }
    }
    public record Formula(String id, Map<String, Integer> required, Map<Effect, Dose> effects,
                          int yield, double conflictReduction, double toxicityReduction) {
        public Formula { required = Map.copyOf(required); effects = Map.copyOf(effects); }
    }
    public record Batch(String formula, int count, Map<Element, Double> elements,
                        Map<Effect, Dose> effects, double stability, double conflict,
                        String quality, boolean failed) {
        public Batch { elements = Map.copyOf(elements); effects = Map.copyOf(effects); }
    }
    public static final Map<String, Plant> PLANTS = new LinkedHashMap<>();
    public static final List<Formula> FORMULAS = new ArrayList<>();
    private static final double PLANT_MEDICINAL_EFFECT_SCALE = 0.20;
    static {
        plant("qingling_grass", Map.of(WOOD,70., WATER,20.), Map.of(QI,35., CULTIVATION,8.),100,82);
        plant("chiyan_flower", Map.of(FIRE,80., WOOD,15.), Map.of(FIRE_DAMAGE,12., COLD_RESIST,25.),105,70,KINDLE);
        plant("ninglu_zhi", Map.of(WATER,75., WOOD,25.), Map.of(HEALTH,20., QI,15.),100,85,CONDENSE_WATER);
        plant("xuanbing_lotus", Map.of(WATER,90., METAL,20.), Map.of(FIRE_RESIST,35., CLEAR_MIND,20.),100,86);
        plant("ziyun_vine", Map.of(WOOD,60., EARTH,35.), Map.of(DEFENSE,15., HEALTH,20.),100,88);
        plant("shihun_grass", Map.of(METAL,50., WATER,40.), Map.of(SENSE,30., TOXICITY,20.),110,45);
        plant("jiuye_xianlan", Map.of(WOOD,90., WATER,50., EARTH,20.), Map.of(CULTIVATION,40., QI,100., STABILITY,25.),120,95,HARMONIZE,LOCK_QI);
        plant("jinwen_fruit", Map.of(METAL,60., WOOD,40.), Map.of(QI,25., CULTIVATION,10.),105,85,LOCK_QI);
        plant("fugu_tai", Map.of(WOOD,45., EARTH,25., YIN,20.), Map.of(ATTACK,12., TOXICITY,25.),110,45,CATALYZE);
        plant("huanxia_si", Map.of(WIND,45., WOOD,35., WATER,20.), Map.of(SENSE,20., DURATION,15., CLEAR_MIND,15.),100,80,HARMONIZE);
        plant("lingquan_leaf", Map.of(WATER,65., WOOD,30.), Map.of(QI,20.),100,90,PURIFY);
        plant("ningling_flower", Map.of(WOOD,55., WATER,35.), Map.of(QI_MULTIPLIER,25.),100,90);
        plant("harmonizing_herb", Map.of(EARTH,40., WOOD,20.), Map.of(STABILITY,30., DETOX,30.),100,95,HARMONIZE);
        plant("catalyst_blossom", Map.of(WIND,35., YANG,25.), Map.of(DURATION,10.),100,75,CATALYZE);
        formula("qingling", Map.of("qingling_grass",2,"ninglu_zhi",1),Map.of(QI,new Dose(3,200),CULTIVATION,new Dose(.1,0)),5,0);
        formula("huichun", Map.of("ninglu_zhi",2,"ziyun_vine",1),Map.of(HEALTH,new Dose(6,120)),5,0);
        formula("chiyan", Map.of("chiyan_flower",2,"qingling_grass",1),Map.of(FIRE_DAMAGE,new Dose(5,1200),COLD_RESIST,new Dose(2,1200)),5,5);
        formula("bingxin", Map.of("xuanbing_lotus",2,"ninglu_zhi",1),Map.of(CLEAR_FIRE,new Dose(100,0),FIRE_RESIST,new Dose(5,1200)),5,10);
        formula("guyuan", Map.of("ziyun_vine",2,"ninglu_zhi",1),Map.of(DEFENSE,new Dose(5,1800),KNOCKBACK_RESIST,new Dose(20,1800)),5,0);
        formula("yangshen", Map.of("shihun_grass",1,"jiuye_xianlan",1,"xuanbing_lotus",1),Map.of(SENSE,new Dose(5,200)),5,15);
        formula("juling", Map.of("jiuye_xianlan",1,"qingling_grass",3,"ninglu_zhi",2),Map.of(CULTIVATION_GAIN,new Dose(10,2400),QI,new Dose(2,400)),6,0);
        formula("huiling", Map.of("qingling_grass",2,"lingquan_leaf",1),Map.of(QI,new Dose(5,200)),5,0);
        formula("mingmu", Map.of("qingling_grass",2,"jiuye_xianlan",1),Map.of(NIGHT_VISION,new Dose(100,3600)),5,0);
        formula("dingshen", Map.of("ninglu_zhi",2,"jiuye_xianlan",1),Map.of(MIND_SHIELD,new Dose(100,900)),5,0);
        formula("bishui", Map.of("xuanbing_lotus",1,"ninglu_zhi",2),Map.of(WATER_BREATHING,new Dose(100,3600)),5,0);
        formula("pozhang", Map.of("chiyan_flower",2,"shihun_grass",1),Map.of(NEXT_MELEE,new Dose(25,600)),5,25,30);
        formula("xumai", Map.of("jiuye_xianlan",1,"ziyun_vine",2),Map.of(EMERGENCY_REGEN,new Dose(20,900)),5,0);
        formula("tiegu", Map.of("ziyun_vine",2,"xuanbing_lotus",1),Map.of(IRON_BONES,new Dose(100,900)),5,0);
        formula("bixie", Map.of("shihun_grass",2,"qingling_grass",1),Map.of(WITHER_SHIELD,new Dose(100,1200)),5,15,50);
        formula("ningqi", Map.of("qingling_grass",2,"xuanbing_lotus",1,"ninglu_zhi",1),Map.of(NEXT_QI_SPELL,new Dose(10,1200)),5,0);
        formula("qingdu", Map.of("harmonizing_herb",3,"lingquan_leaf",2,"xuanbing_lotus",1),Map.of(DETOX,new Dose(100,0)),5,50,100);
    }
    private static void plant(String id, Map<Element,Double> elements, Map<Effect,Double> effects,
                              double strength, double stability, Trait... traits) {
        var reducedEffects = new EnumMap<Effect, Double>(Effect.class);
        // Plant effects use percentage points directly, capped per ingredient, including chemistry effects.
        effects.forEach((effect, amount) -> reducedEffects.put(effect, Math.min(1.0, percentAmount(effect,
                switch (effect) {
                    case STABILITY, DETOX, TOXICITY -> amount;
                    default -> amount * PLANT_MEDICINAL_EFFECT_SCALE;
                }))));
        PLANTS.put(id,new Plant(elements,reducedEffects,strength,stability,Set.of(traits)));
    }
    private static void formula(String id, Map<String,Integer> required, Map<Effect,Dose> effects, int yield, double reduction) {
        formula(id,required,effects,yield,reduction,0);
    }
    private static void formula(String id, Map<String,Integer> required, Map<Effect,Dose> effects, int yield,
                                double reduction, double detox) {
        // Formula amounts are percentages directly; legacy conversion is only for herbs and old saves.
        FORMULAS.add(new Formula(id,required,effects,yield,reduction,detox));
    }
    public static Formula formula(String id) {
        return FORMULAS.stream().filter(f->f.id.equals(id)).findFirst().orElseThrow();
    }
    public static boolean matches(Formula formula, Map<String,Integer> ingredients) {
        if (ingredients.isEmpty() || ingredients.values().stream().anyMatch(n->n<=0)
                || ingredients.values().stream().mapToLong(Integer::longValue).sum()>64) return false;
        return ingredients.keySet().stream().allMatch(PLANTS::containsKey)
                && formula.required.entrySet().stream().allMatch(e->ingredients.getOrDefault(e.getKey(),0)>=e.getValue());
    }
    public static Batch brew(Formula formula, Map<String,Integer> ingredients) {
        if (!matches(formula,ingredients)) throw new IllegalArgumentException("Missing recipe ingredients or invalid additives");
        var elements = new EnumMap<Element,Double>(Element.class);
        var additions = new EnumMap<Effect,Double>(Effect.class);
        var additionLimits = new EnumMap<Effect,Double>(Effect.class);
        var traits = new EnumMap<Trait,Integer>(Trait.class);
        int count = ingredients.values().stream().mapToInt(Integer::intValue).sum();
        double stability = 0, toxicity = 0, stabilization = 0;
        boolean incompatibleAdditive = false;
        for (var entry : new TreeMap<>(ingredients).entrySet()) {
            Plant plant = PLANTS.get(entry.getKey()); int n = entry.getValue();
            int extra = n - formula.required.getOrDefault(entry.getKey(),0);
            plant.elements.forEach((e,v)->elements.merge(e,v*n,Double::sum));
            plant.traits.forEach(t->traits.merge(t,n,Integer::sum));
            stability += plant.stability*n;
            stabilization += plant.effects.getOrDefault(STABILITY,0.)*n;
            toxicity += plant.effects.getOrDefault(TOXICITY,0.)*n;
            if (extra>0) {
                if (!compatibleAdditive(formula,plant))
                    incompatibleAdditive = true;
                plant.effects.forEach((e,v)->{
                    if(allowedAdditive(formula,e)) {
                        additions.merge(e,Math.min(1.0,v*plant.strength/100.)*extra,Double::sum);
                        additionLimits.merge(e,(double)extra,Double::sum);
                    }
                });
            }
        }
        elements.replaceAll((e,v)->v/count);
        if (traits.containsKey(KINDLE)) {
            double transfer=elements.getOrDefault(WOOD,0.)*.10;
            elements.merge(WOOD,-transfer,Double::sum);elements.merge(FIRE,transfer,Double::sum);
        }
        if(traits.containsKey(CONDENSE_WATER)) elements.computeIfPresent(WATER,(e,v)->v*1.08);
        double rawConflict = conflict(elements);
        double reduction = formula.conflictReduction + traits.getOrDefault(HARMONIZE,0)*18 + stabilization/count*.6;
        double conflict = Math.max(0,rawConflict-reduction);
        toxicity = Math.max(0,toxicity-formula.toxicityReduction-stabilization-traits.getOrDefault(HARMONIZE,0)*15-additions.getOrDefault(DETOX,0.));
        stability = clamp(stability/count + stabilization/count + traits.getOrDefault(PURIFY,0)*5 - conflict*1.5 - toxicity*.6,0,100);
        boolean failed = incompatibleAdditive || conflict>=35 || stability<20 || toxicity>=60;
        var effects = new EnumMap<Effect,Dose>(Effect.class);
        effects.putAll(formula.effects);
        double catalyst = 1+Math.min(.5,traits.getOrDefault(CATALYZE,0)*.10);
        double compatibility = Math.min(.15, compatible(elements)/1000);
        for(var addition:additions.entrySet()) {
            Effect e=addition.getKey();
            if(e==STABILITY||e==TOXICITY||e==DETOX||e==QI_MULTIPLIER||e==DURATION || !formula.effects.containsKey(e)) continue;
            Dose base=effects.get(e);
            double gain=Math.min(additionLimits.get(e),addition.getValue()*(1+compatibility));
            effects.put(e,new Dose(base.amount+gain,base.ticks));
        }
        double qiMultiplier=1+Math.min(3,additions.getOrDefault(QI_MULTIPLIER,0.)/100);
        double duration=1+Math.min(2,additions.getOrDefault(DURATION,0.)/100);
        double retention=traits.containsKey(LOCK_QI)||ingredients.equals(formula.required)?1:clamp(stability/65.,.5,1);
        effects.replaceAll((e,d)->new Dose(round(activation(e)?Math.min(100,d.amount):d.amount*catalyst*(e==QI?qiMultiplier:1)*retention),
                Math.min(72000,(int)Math.round(d.ticks*duration))));
        if(toxicity>0) effects.put(TOXICITY,new Dose(round(toxicity),200));
        elements.replaceAll((e,v)->round(v));
        String quality=stability>=90?"superior":stability>=70?"fine":"common";
        return new Batch(formula.id,formula.yield,elements,effects,round(stability),round(conflict),quality,failed);
    }
    public static double conflict(Map<Element,Double> e) {
        return opposing(e,WATER,FIRE)+opposing(e,ICE,FIRE)+opposing(e,YANG,YIN)
                +.45*opposing(e,METAL,WOOD)+.45*opposing(e,EARTH,WATER)+.4*opposing(e,THUNDER,EARTH);
    }
    public static double compatible(Map<Element,Double> e) {
        return opposing(e,METAL,WATER)+opposing(e,WATER,WOOD)+opposing(e,WOOD,FIRE)
                +opposing(e,FIRE,EARTH)+opposing(e,EARTH,METAL)+opposing(e,WIND,THUNDER);
    }
    private static double opposing(Map<Element,Double> e,Element a,Element b) {
        return Math.min(e.getOrDefault(a,0.),e.getOrDefault(b,0.));
    }
    public static boolean allowedAdditive(Formula formula, Effect effect) {
        return switch(effect) {
            // These affect brewing chemistry, never add a medicinal ability to the finished pill.
            case STABILITY, DETOX, TOXICITY -> true;
            case QI_MULTIPLIER -> formula.effects.containsKey(QI);
            case DURATION -> formula.effects.values().stream().anyMatch(d->d.ticks()>0) || formula.effects.containsKey(EMERGENCY_REGEN);
            default -> formula.effects.containsKey(effect);
        };
    }
    public static boolean compatibleAdditive(Formula formula, Plant plant) {
        boolean onlyChemistry = true;
        for(var entry:plant.effects.entrySet()) {
            if(entry.getValue()<=0 || entry.getKey()==STABILITY || entry.getKey()==DETOX || entry.getKey()==TOXICITY) continue;
            onlyChemistry = false;
            if(allowedAdditive(formula,entry.getKey())) return true;
        }
        return onlyChemistry;
    }
    public static boolean recovery(Effect effect) {
        return effect==HEALTH || effect==QI || effect==SENSE || effect==CULTIVATION || effect==EMERGENCY_REGEN;
    }
    public static boolean activation(Effect effect) {
        return switch(effect) {
            case CLEAR_FIRE, DETOX, CLEAR_MIND, NIGHT_VISION, MIND_SHIELD, WATER_BREATHING, IRON_BONES, WITHER_SHIELD, TOXICITY -> true;
            default -> false;
        };
    }
    /** Convert old fixed doses using documented reference capacities; runtime uses the actual consumer's maximum. */
    public static double percentAmount(Effect effect, double legacy) {
        return switch(effect) {
            case HEALTH -> legacy * 5; // 20 reference HP
            case QI -> legacy / 10; // 1,000 reference Qi
            case CULTIVATION -> legacy / 20; // 2,000 reference cultivation progress
            case EMERGENCY_REGEN -> legacy * 90;
            case CLEAR_FIRE, NIGHT_VISION, MIND_SHIELD, WATER_BREATHING, IRON_BONES, WITHER_SHIELD -> Math.min(100,legacy*100);
            default -> legacy; // Thoughts use a reference maximum of 100; other effects already use percentages.
        };
    }
    public static double round(double value) {return Math.round(value*100.)/100.;}
    private static double clamp(double value,double min,double max) {return Math.max(min,Math.min(max,value));}
}

