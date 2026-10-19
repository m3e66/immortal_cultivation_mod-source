package com.example.immortal_cultivation_mod.alchemy;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.*;

public final class PillData {
    public static final String KEY = "CultivationPill";
    public static ItemStack create(Batch batch) {
        if(batch.failed()) return new ItemStack(com.example.immortal_cultivation_mod.item.ModItems.ASHES.get(),batch.count());
        ItemStack stack = new ItemStack(AlchemyContent.pill(batch.formula()),batch.count());
        CompoundTag data = new CompoundTag();
        data.putInt("DoseVersion",3);
        data.putString("Formula",batch.formula()); data.putString("Quality",batch.quality());
        data.putDouble("Stability",batch.stability()); data.putDouble("Conflict",batch.conflict());
        CompoundTag elements=new CompoundTag(), effects=new CompoundTag();
        batch.elements().forEach((k,v)->{if(v>0)elements.putDouble(k.name(),v);});
        batch.effects().forEach((k,v)->{
            if(v.amount()<=0)return;
            CompoundTag dose=new CompoundTag();dose.putDouble("Amount",v.amount());dose.putInt("Ticks",v.ticks());
            effects.put(k.name(),dose);
        });
        data.put("Elements",elements);data.put("Effects",effects);
        CompoundTag root=new CompoundTag();root.put(KEY,data);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
        return stack;
    }
    public static CompoundTag raw(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY);
        // Read old crafted pills as Yin/Yang, preserving strength and all unrelated data.
        CompoundTag elements = data.getCompound("Elements");
        migrateElement(elements, "LIGHT", "YANG");
        migrateElement(elements, "DARK", "YIN");
        return data;
    }

    private static void migrateElement(CompoundTag elements, String oldName, String newName) {
        if (!elements.contains(oldName)) return;
        elements.putDouble(newName, elements.getDouble(newName) + elements.getDouble(oldName));
        elements.remove(oldName);
    }
    public static Map<Effect,Dose> effects(ItemStack stack) {
        if(!(stack.getItem() instanceof AlchemyPillItem pill))return Map.of();
        CompoundTag data=raw(stack);
        if(data.isEmpty())return formula(pill.formula).effects();
        var result=new EnumMap<Effect,Dose>(Effect.class);
        CompoundTag values=data.getCompound("Effects");
        for(Effect effect:Effect.values()) {
            CompoundTag entry=values.getCompound(effect.name());
            double amount=entry.getDouble("Amount");int ticks=entry.getInt("Ticks");
            if(data.getInt("DoseVersion")<2)amount=percentAmount(effect,amount);
            if(data.getInt("DoseVersion")<3)amount=rebalanceOldDose(pill.formula,effect,amount);
            if(Double.isFinite(amount)&&amount>0&&amount<=100000&&ticks>=0&&ticks<=72000)
                result.put(effect,new Dose(amount,ticks));
        }
        return result;
    }
    /** Read-time migration keeps saved crafting multipliers, without repeatedly changing stack data. */
    private static double rebalanceOldDose(String formulaId, Effect effect, double amount) {
        double oldBase = switch (formulaId) {
            case "qingling" -> effect == Effect.QI ? 10 : effect == Effect.CULTIVATION ? 1 : 0;
            case "huichun" -> effect == Effect.HEALTH ? 60 : 0;
            case "chiyan" -> effect == Effect.FIRE_DAMAGE ? 15 : effect == Effect.COLD_RESIST ? 20 : 0;
            case "bingxin" -> effect == Effect.FIRE_RESIST ? 25 : 0;
            case "guyuan" -> effect == Effect.DEFENSE ? 10 : 0;
            case "yangshen" -> effect == Effect.SENSE ? 30 : 0;
            case "juling" -> effect == Effect.CULTIVATION_GAIN || effect == Effect.QI ? 20 : 0;
            case "huiling" -> effect == Effect.QI ? 50 : 0;
            case "xumai" -> effect == Effect.EMERGENCY_REGEN ? 90 : 0;
            case "ningqi" -> effect == Effect.NEXT_QI_SPELL ? 20 : 0;
            default -> 0;
        };
        return oldBase > 0 ? amount * (formula(formulaId).effects().get(effect).amount() / oldBase) : amount;
    }
    public static String number(double n) {return n==(long)n?Long.toString((long)n):String.format(Locale.ROOT,"%.2f",n);}
    public static Component effectLine(Effect e,Dose d) {
        if(activation(e))
            return Component.translatable("alchemy.effect."+e.name())
                    .append(": ").append(Component.translatable("alchemy.trigger_chance",number(d.amount())))
                    .append(d.ticks()>0?" / "+number(d.ticks()/20.)+"s":"");
        return Component.translatable("alchemy.effect."+e.name()).append(": "+number(d.amount()))
                .append(percent(e)?"%":"")
                .append(d.ticks()>0?" / "+number(d.ticks()/20.)+"s":"");
    }
    public static boolean percent(Effect e) {
        return true;
    }
    public static void tooltip(ItemStack stack,List<Component> lines) {
        if (stack.getItem() instanceof LegacyPillItem) {
            legacyTooltip(stack, lines);
            qualityLine("common", lines);
            return;
        }
        CompoundTag data=raw(stack);
        effects(stack).forEach((effect,dose)->lines.add(effectLine(effect,dose).copy().withStyle(effectColor(effect))));
        if(effects(stack).keySet().stream().anyMatch(AlchemyChemistry::recovery))
            lines.add(Component.translatable("alchemy.percent_max_hint").withStyle(ChatFormatting.GRAY));
        CompoundTag elements=data.getCompound("Elements");
        for(Element e:Element.values()) if(elements.getDouble(e.name())>0)
            lines.add(Component.translatable("alchemy.element."+e.name()).append(": "+number(elements.getDouble(e.name())))
                    .withStyle(elementColor(e)));
        qualityLine(data.isEmpty()?"common":data.getString("Quality"), lines);
        if(!data.isEmpty())lines.add(Component.translatable("alchemy.stability").append(": "+number(data.getDouble("Stability")))
                .withStyle(ChatFormatting.AQUA));
    }

    private static void qualityLine(String quality, List<Component> lines) {
        lines.add(Component.translatable("alchemy.quality").append(": ")
                .append(Component.translatable("alchemy.quality." + quality)).withStyle(ChatFormatting.GOLD));
    }

    private static void legacyTooltip(ItemStack stack, List<Component> lines) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        switch (id) {
            case "enlightenment_pill" -> {
                legacyLine(lines, "enlightenment", ChatFormatting.AQUA);
                legacyLine(lines, "enlightenment_hint", ChatFormatting.GRAY);
            }
            case "breakthrough_pill" -> {
                legacyLine(lines, "breakthrough", ChatFormatting.GOLD);
                legacyLine(lines, "breakthrough_hint", ChatFormatting.GRAY);
            }
            case "body_tempering_pill" -> {
                legacyLine(lines, "body_tempering", ChatFormatting.GREEN, 1);
                legacyLine(lines, "body_tempering_limit", ChatFormatting.GRAY, 10);
            }
            case "blood_qi_pill" -> {
                legacyLine(lines, "blood_qi", ChatFormatting.GREEN, 5);
            }
            case "bigu_pill" -> {
                legacyLine(lines, "hunger", ChatFormatting.GREEN, 100);
                legacyLine(lines, "saturation", ChatFormatting.GREEN, 100);
            }
            default -> { }
        }
    }

    private static void legacyLine(List<Component> lines, String key, ChatFormatting color, Object... values) {
        lines.add(Component.translatable("alchemy.legacy." + key, values).withStyle(color));
    }

    private static ChatFormatting effectColor(Effect effect) {
        return switch(effect) {
            case TOXICITY -> ChatFormatting.RED;
            case QI, QI_MULTIPLIER, NEXT_QI_SPELL, SENSE, CLEAR_MIND, MIND_SHIELD -> ChatFormatting.AQUA;
            case CULTIVATION, CULTIVATION_GAIN, BREAKTHROUGH -> ChatFormatting.GOLD;
            default -> ChatFormatting.GREEN;
        };
    }

    private static ChatFormatting elementColor(Element element) {
        return switch(element) {
            case METAL, EARTH -> ChatFormatting.GOLD;
            case WOOD, WIND -> ChatFormatting.GREEN;
            case WATER, ICE -> ChatFormatting.AQUA;
            case FIRE -> ChatFormatting.RED;
            case THUNDER, YIN -> ChatFormatting.LIGHT_PURPLE;
            case YANG -> ChatFormatting.YELLOW;
        };
    }
}
