package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Card definitions keep world-spell baselines and costs; unsupported utility spells are not attacks. */
public final class LifeCardCatalog {
    public record Card(String id,String name,String effect,int energy,double qi,int copies,String target,
                       double[] hits,double amount,int duration,String status,boolean consumable,String item,boolean area) {
        public Card(String id,String name,String effect,int energy,double qi,int copies,String target,double[] hits,double amount,int duration,String status,boolean consumable,String item){this(id,name,effect,energy,qi,copies,target,hits,amount,duration,status,consumable,item,false);}
        public double baseTotal(){return Arrays.stream(hits).sum();}
    }
    public static final List<String> BASICS=List.of("strike","block","dodge","run","recover","inspect","objective");
    private LifeCardCatalog() {}
    private static Card basic(String id,String effect,String target,double[] hits,double amount) {
        return new Card(id,"life.cards.card."+id,effect,1,0,5,target,hits,amount,0,"",false,"");
    }
    public static Card get(CompoundTag life,CompoundTag actor,String id) {
        switch(id) {
            case "strike":return basic(id,"attack","enemy",new double[]{actor.getDouble("Power")},0);
            case "block":return basic(id,"block","self",new double[0],.30);
            case "dodge":return basic(id,"dodge","self",new double[0],.60);
            case "run":return basic(id,"run","self",new double[0],.60);
            case "recover":return basic(id,"qi","self",new double[0],actor.getDouble("MaxQi")*.10);
            case "inspect":return basic(id,"inspect","any",new double[0],0);
            case "objective":return basic(id,"objective","anchor",new double[0],0);
        }
        if(id.startsWith("item_"))return item(actor,id.substring(5).replace('~',':'));
        if(id.startsWith("spell_"))return spell(life,actor,id.substring(6));
        if(List.of("heavy","quick","pierce").contains(id)&&life.getInt("Combat")>=30) {
            var move=LifeSimulationBattle.BASE_MOVES.stream().filter(m->m.id().equals(id)).findFirst().orElse(null);
            if(move!=null)return new Card(id,move.name(),"attack",id.equals("heavy")?2:1,
                LifeSimulationBattle.cost(actor,move),2,"enemy",new double[]{actor.getDouble("Power")*move.potency()},0,0,move.effect(),false,"");
        }
        return null;
    }
    public static List<Card> available(CompoundTag life,CompoundTag actor) {
        List<Card> cards=new ArrayList<>();for(String id:BASICS)cards.add(get(life,actor,id));
        for(String id:List.of("heavy","quick","pierce")){Card card=get(life,actor,id);if(card!=null)cards.add(card);}
        for(var tag:life.getList("DiscoveredSpells",Tag.TAG_STRING)){Card card=get(life,actor,"spell_"+tag.getAsString());if(card!=null)cards.add(card);}
        return List.copyOf(cards);
    }
    private static Card spell(CompoundTag life,CompoundTag actor,String id) {
        if(life.getList("DiscoveredSpells",Tag.TAG_STRING).stream().noneMatch(t->t.getAsString().equals(id)))return null;
        var spell=ModSpells.get(id);if(spell==null)return null;
        String effect="attack",target="enemy",status="";double[] hits;double amount=0;int duration=0;
        switch(id) {
            case "fireball":hits=new double[]{10};status="burn";duration=3;break;
            case "wind_blade":hits=new double[]{8};break;
            case "zhenshan_palm":hits=new double[]{225};break;
            case "jinfeng_zhi":hits=projectile(ElementalTechniqueProjectileEntity.Kind.JINFENG_ZHI,1);break;
            case "wanren_chuanxin":hits=projectile(ElementalTechniqueProjectileEntity.Kind.WANREN_CHUANXIN,6);break;
            case "yanbao_dan":hits=projectile(ElementalTechniqueProjectileEntity.Kind.YANBAO_DAN,1);status="burn";duration=3;break;
            case "shuiren_lianzhan":hits=projectile(ElementalTechniqueProjectileEntity.Kind.SHUIREN_LIANZHAN,3);break;
            case "bingzhui_yu":hits=projectile(ElementalTechniqueProjectileEntity.Kind.BINGZHUI_YU,8);status="slow";duration=2;break;
            case "lingzhi_bullet":hits=new double[]{5};break;
            case "leishan":hits=new double[]{55};break;
            case "wulei_hongding":hits=new double[]{240,264,288,312,336};break;
            case "tianguang_guansha":hits=new double[]{950};break;
            case "canglang_chong":hits=new double[]{180};break;
            case "regeneration":hits=new double[0];effect="heal";target="friend";amount=4;break;
            case "cleanse":hits=new double[0];effect="cleanse";target="friend";break;
            case "ignite_flare":hits=new double[0];effect="burn";amount=1;duration=5;break;
            case "qi_gathering":hits=new double[0];effect="gather";target="self";amount=actor.getDouble("MaxQi")*.05;break;
            case "spirit_sight":hits=new double[0];effect="inspect";target="any";break;
            default:return null;
        }
        int copies=1;
        boolean advanced=List.of("earth","heaven").contains(spell.tier());
        int energy=advanced?(spell.qiCost()>=2000?3:2):1;
        if(effect.equals("attack"))for(int i=0;i<hits.length;i++)hits[i]+=Math.max(0,life.getDouble("MagicAttack"));
        return new Card("spell_"+id,"spell.immortal_cultivation_mod."+spell.translationKey(),effect,energy,
            spell.qiCost()*(1-LifeCardBattle.buff(actor,"NEXT_QI_SPELL")/100.),copies,target,hits,amount,duration,status,false,"");
    }
    private static double[] projectile(ElementalTechniqueProjectileEntity.Kind kind,int count) {
        double[] hits=new double[count];Arrays.fill(hits,ElementalTechniqueProjectileEntity.baseDamage(kind));return hits;
    }
    public static Card item(CompoundTag actor,String id) {
        var location=ResourceLocation.tryParse(itemTextureId(id));if(location==null||!BuiltInRegistries.ITEM.containsKey(location))return null;
        var item=BuiltInRegistries.ITEM.get(location);String effect="",target="self",status="";double amount=0;int duration=0;double[] hits={};
        if(item instanceof AlchemyPillItem pill) {
            var doses=AlchemyChemistry.formula(pill.formula).effects();
            if(doses.containsKey(AlchemyChemistry.Effect.HEALTH)){var dose=doses.get(AlchemyChemistry.Effect.HEALTH);duration=(int)Math.ceil(dose.ticks()/20.);effect=duration>0?"heal_over_time":"heal";amount=actor.getDouble("MaxHp")*dose.amount()/100.;}
            else if(doses.containsKey(AlchemyChemistry.Effect.QI)){var dose=doses.get(AlchemyChemistry.Effect.QI);duration=(int)Math.ceil(dose.ticks()/20.);effect=duration>0?"qi_over_time":"qi";amount=actor.getDouble("MaxQi")*dose.amount()/100.;}
            else if(pill.formula.equals("qingdu"))effect="cleanse";
            else for(var key:List.of(AlchemyChemistry.Effect.DEFENSE,AlchemyChemistry.Effect.FIRE_DAMAGE,AlchemyChemistry.Effect.FIRE_RESIST,AlchemyChemistry.Effect.NEXT_MELEE,AlchemyChemistry.Effect.NEXT_QI_SPELL,AlchemyChemistry.Effect.IRON_BONES,AlchemyChemistry.Effect.EMERGENCY_REGEN,AlchemyChemistry.Effect.WITHER_SHIELD)){
                if(doses.containsKey(key)){effect="buff";status=key.name();amount=key==AlchemyChemistry.Effect.IRON_BONES?60:doses.get(key).amount();duration=(int)Math.ceil(doses.get(key).ticks()/20.);break;}
            }
            if(pill.formula.equals("bingxin")){effect="buff";status="FIRE_RESIST";amount=doses.get(AlchemyChemistry.Effect.FIRE_RESIST).amount();duration=60;}
        }
        else if(id.endsWith("fireball_talisman")){effect="attack";target="enemy";hits=new double[]{10};status="burn";duration=3;}
        else if(id.endsWith("fengren_talisman")){effect="attack";target="enemy";hits=new double[]{8};}
        else if(id.endsWith("dunkong_talisman")||id.endsWith("escape_talisman")){effect="run";amount=.90;}
        else if(id.equals("minecraft:honey_bottle"))effect="cleanse";
        if(effect.isBlank())return null;
        return new Card("item_"+id.replace(':','~'),item.getDescriptionId(),effect,1,0,1,target,hits,amount,duration,status,true,id);
    }
    /** Legacy simulated IDs refer to the unified pill, but consumption keeps their saved inventory key. */
    public static String itemTextureId(String id){
        return List.of("immortal_cultivation_mod:low_qi_recovery_pill","immortal_cultivation_mod:middle_qi_recovery_pill","immortal_cultivation_mod:high_qi_recovery_pill").contains(id)?"immortal_cultivation_mod:qi_recovery_pill":id;
    }
}
