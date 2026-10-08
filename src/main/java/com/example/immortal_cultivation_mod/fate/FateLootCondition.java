package com.example.immortal_cultivation_mod.fate;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Replaces only opted-in rare material/treasure chance conditions, not ordinary random mechanics. */
public record FateLootCondition(float chance) implements LootItemCondition {
    public static final MapCodec<FateLootCondition> CODEC = Codec.floatRange(0, 1).fieldOf("chance")
            .xmap(FateLootCondition::new, FateLootCondition::chance);
    public static final DeferredRegister<LootItemConditionType> TYPES = DeferredRegister.create(
            Registries.LOOT_CONDITION_TYPE, ImmortalCultivationMod.MODID);
    public static final java.util.function.Supplier<LootItemConditionType> TYPE = TYPES.register("fortune_chance",
            () -> new LootItemConditionType(CODEC));
    @Override public LootItemConditionType getType() { return TYPE.get(); }
    @Override public boolean test(LootContext context) {
        var owner = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        if (!(owner instanceof LivingEntity)) owner = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        if (!(owner instanceof LivingEntity)) {
            var opener = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            owner = opener instanceof net.minecraft.world.entity.player.Player ? opener : null;
        }
        int fortune = owner instanceof LivingEntity living ? LuckSystem.luck(living) : 50;
        return DebugEyeDiagnostics.roll(owner instanceof LivingEntity living ? living : null, "loot",
                chance, LuckRules.rareChance(chance, fortune), context.getRandom().nextFloat());
    }
}
