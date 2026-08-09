package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeaponRestrictionSystem {
    public static final String TAG_RESTRICTIONS = ImmortalCultivationMod.MODID + ".weapon_restrictions";
    public static final int MAX_LAYERS_PER_TIER = 72;
    public static final int MAX_TIER = 7;
    private static final String TAG_MERGED = "merged";
    private static final ResourceLocation MERGED_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "merged_restriction_damage");

    public enum RestrictionType {
        SHARPNESS("sharpness"),
        SPIRIT_GATHERING("spirit_gathering"),
        SPIRIT_DEVOURING("spirit_devouring"),
        SWIFT_FLIGHT("swift_flight"),
        STURDY("sturdy"),
        QI_STORAGE("qi_storage"),
        ARMOR_BREAKING("armor_breaking");

        private final String id;

        RestrictionType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private WeaponRestrictionSystem() {
    }

    public static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.SHIHUNFAN.get()) || QiInfusedWeapon.isWeapon(stack));
    }

    public static boolean hasRestrictions(ItemStack stack) {
        return !restrictionData(stack).isEmpty();
    }

    public static int tier(ItemStack stack) {
        return Math.max(0, Math.min(MAX_TIER, restrictionData(stack).getInt("tier")));
    }

    public static void setTier(ItemStack stack, int tier) {
        if (!isWeapon(stack)) {
            return;
        }
        CompoundTag root = rootTag(stack);
        CompoundTag data = root.getCompound(TAG_RESTRICTIONS);
        data.putInt("tier", Math.max(0, Math.min(MAX_TIER, tier)));
        data.putInt("tier_progress", 0);
        root.put(TAG_RESTRICTIONS, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static int tierProgress(ItemStack stack) {
        return Math.max(0, Math.min(MAX_LAYERS_PER_TIER, restrictionData(stack).getInt("tier_progress")));
    }

    public static int remainingCapacity(ItemStack stack) {
        return MAX_LAYERS_PER_TIER - tierProgress(stack);
    }

    public static int totalLayers(ItemStack stack) {
        return tier(stack) * MAX_LAYERS_PER_TIER + tierProgress(stack);
    }

    public static int count(ItemStack stack, RestrictionType type) {
        return Math.max(0, restrictionData(stack).getInt(type.id()));
    }

    public static int mergedCount(ItemStack stack, RestrictionType type) {
        return Math.max(0, restrictionData(stack).getCompound(TAG_MERGED).getInt(type.id()));
    }

    public static int effectiveCount(ItemStack stack, RestrictionType type) {
        return count(stack, type) + mergedCount(stack, type);
    }

    public static boolean addRestrictions(ItemStack stack, int[] additions) {
        if (!isWeapon(stack) || additions == null || additions.length != RestrictionType.values().length) {
            return false;
        }
        int total = 0;
        for (int addition : additions) {
            if (addition < 0) {
                return false;
            }
            total += addition;
        }
        if (total <= 0 || total > remainingCapacity(stack)) {
            return false;
        }

        CompoundTag root = rootTag(stack);
        CompoundTag data = root.getCompound(TAG_RESTRICTIONS);
        if (data.getInt("base_max_damage") <= 0
                && additions[RestrictionType.STURDY.ordinal()] > 0
                && stack.isDamageableItem()) {
            data.putInt("base_max_damage", stack.getMaxDamage());
        }
        for (RestrictionType type : RestrictionType.values()) {
            int addition = additions[type.ordinal()];
            if (addition > 0) {
                data.putInt(type.id(), data.getInt(type.id()) + addition);
            }
        }
        data.putInt("tier_progress", tierProgress(stack) + total);
        root.put(TAG_RESTRICTIONS, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        applyDurability(stack);
        return true;
    }

    public static boolean evolve(ItemStack stack) {
        if (!isWeapon(stack) || tierProgress(stack) < MAX_LAYERS_PER_TIER || tier(stack) >= MAX_TIER) {
            return false;
        }
        CompoundTag root = rootTag(stack);
        CompoundTag data = root.getCompound(TAG_RESTRICTIONS);
        int sharpness = Math.max(0, data.getInt(RestrictionType.SHARPNESS.id()));
        if (sharpness > 0) {
            bakeSharpnessIntoBaseDamage(stack, sharpness);
        }

        CompoundTag merged = data.getCompound(TAG_MERGED);
        for (RestrictionType type : RestrictionType.values()) {
            int current = Math.max(0, data.getInt(type.id()));
            if (current > 0) {
                merged.putInt(type.id(), merged.getInt(type.id()) + current);
            }
            data.remove(type.id());
        }
        data.put(TAG_MERGED, merged);
        data.putInt("tier", tier(stack) + 1);
        data.putInt("tier_progress", 0);
        root.put(TAG_RESTRICTIONS, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        applyDurability(stack);
        return true;
    }

    public static boolean removeOneRestrictionLayer(ItemStack stack, RandomSource random) {
        if (!isWeapon(stack) || totalLayers(stack) <= 0) {
            return false;
        }
        CompoundTag root = rootTag(stack);
        CompoundTag data = root.getCompound(TAG_RESTRICTIONS);
        List<RestrictionType> present = new ArrayList<>();
        for (RestrictionType type : RestrictionType.values()) {
            if (data.getInt(type.id()) > 0) {
                present.add(type);
            }
        }
        if (present.isEmpty()) {
            return false;
        }
        RestrictionType removed = present.get(random.nextInt(present.size()));
        data.putInt(removed.id(), Math.max(0, data.getInt(removed.id()) - 1));
        int progress = tierProgress(stack);
        int currentTier = tier(stack);
        if (progress > 0) {
            data.putInt("tier_progress", progress - 1);
        } else if (currentTier > 0) {
            data.putInt("tier", currentTier - 1);
            data.putInt("tier_progress", MAX_LAYERS_PER_TIER - 1);
        }
        root.put(TAG_RESTRICTIONS, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        applyDurability(stack);
        return true;
    }

    public static float adjustWeaponDamage(ItemStack stack, LivingEntity target, float damage) {
        // Merged sharpness is already part of the item's actual attack-damage component.
        int sharpness = count(stack, RestrictionType.SHARPNESS);
        if (sharpness > 0) {
            damage *= 1.0F + sharpness / 100.0F;
        }
        int armorBreaking = effectiveCount(stack, RestrictionType.ARMOR_BREAKING);
        if (armorBreaking > 0 && hasArmor(target)) {
            damage *= 1.0F + armorBreaking / 100.0F;
        }
        return damage;
    }

    public static int heldQiRegenBonusPercent(Player player) {
        return effectiveCount(player.getMainHandItem(), RestrictionType.SPIRIT_GATHERING)
                + effectiveCount(player.getOffhandItem(), RestrictionType.SPIRIT_GATHERING);
    }

    public static double qiStorageMultiplier(ItemStack stack) {
        return 1.0D + effectiveCount(stack, RestrictionType.QI_STORAGE) / 100.0D;
    }

    public static double flyingSpeedMultiplier(ItemStack stack) {
        return 1.0D + effectiveCount(stack, RestrictionType.SWIFT_FLIGHT) * 0.001D;
    }

    public static void applyWeaponHitEffects(ItemStack stack, LivingEntity target) {
        int layers = effectiveCount(stack, RestrictionType.SPIRIT_DEVOURING);
        if (layers <= 0) {
            return;
        }
        if (target instanceof ServerPlayer player) {
            var data = ModAttachments.getData(player);
            int maxQi = Math.max(0, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
            int drain = drainAmount(maxQi, layers);
            if (drain > 0) {
                ModAttachments.setData(player, data.withQi(Math.max(0, data.qi() - drain)));
                ServerEvents.syncPlayerData(player);
            }
        } else if (target instanceof PathfinderMob mob && target instanceof CultivationNpc npc) {
            int drain = drainAmount(npc.npcMaxQi(), layers);
            if (drain > 0) {
                NpcProfile.drainQi(mob, npc.npcArchetype(), drain);
            }
        }
    }

    public static void applyDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }
        CompoundTag data = restrictionData(stack);
        int sturdy = effectiveCount(stack, RestrictionType.STURDY);
        int baseMax = data.getInt("base_max_damage");
        if (sturdy <= 0 && baseMax <= 0) {
            return;
        }
        if (baseMax <= 0) {
            baseMax = stack.getMaxDamage();
            CompoundTag root = rootTag(stack);
            data = root.getCompound(TAG_RESTRICTIONS);
            data.putInt("base_max_damage", baseMax);
            root.put(TAG_RESTRICTIONS, data);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        int boosted = Math.max(baseMax, (int) Math.ceil(baseMax * (1.0D + sturdy / 100.0D)));
        if (stack.getMaxDamage() != boosted) {
            stack.set(DataComponents.MAX_DAMAGE, boosted);
            stack.setDamageValue(Math.min(stack.getDamageValue(), Math.max(0, boosted - 1)));
        }
    }

    private static void bakeSharpnessIntoBaseDamage(ItemStack stack, int sharpness) {
        double currentDamage = QiInfusedWeapon.baseAttackDamage(stack);
        if (currentDamage <= 0.0D || sharpness <= 0) {
            return;
        }

        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        double previousMergedBonus = 0.0D;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.modifier().id().equals(MERGED_DAMAGE_ID)) {
                previousMergedBonus += entry.modifier().amount();
                continue;
            }
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        double addedDamage = currentDamage * sharpness / 100.0D
                / QiInfusedWeapon.attackDamageAddCoefficient(stack);
        builder.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(MERGED_DAMAGE_ID, previousMergedBonus + addedDamage,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    public static List<Component> tooltips(ItemStack stack) {
        if (!hasRestrictions(stack)) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".restriction.tier",
                Component.translatable("weapon_tier." + ImmortalCultivationMod.MODID + "." + tier(stack)))
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".restriction.progress",
                tierProgress(stack), MAX_LAYERS_PER_TIER).withStyle(ChatFormatting.AQUA));
        for (RestrictionType type : RestrictionType.values()) {
            int count = count(stack, type);
            if (count <= 0) {
                continue;
            }
            String amount = switch (type) {
                case SPIRIT_DEVOURING -> format(count * 0.01D) + "%";
                case SWIFT_FLIGHT -> format(count * 0.1D) + "%";
                default -> count + "%";
            };
            lines.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".restriction.entry." + type.id(),
                    Component.translatable("restriction." + ImmortalCultivationMod.MODID + "." + type.id()),
                    count,
                    amount).withStyle(ChatFormatting.DARK_GREEN));
        }
        return lines;
    }

    private static int drainAmount(int maxQi, int layers) {
        if (maxQi <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(maxQi * layers * 0.0001D));
    }

    private static boolean hasArmor(LivingEntity target) {
        for (ItemStack armor : target.getArmorSlots()) {
            if (!armor.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag restrictionData(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return rootTag(stack).getCompound(TAG_RESTRICTIONS);
    }

    private static CompoundTag rootTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
