package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.UndeadAbilitySystem;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Body passives share the same resource and accepted-hit rules as normal combat. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class BodyTypeEffects {
    public static final String PROJECTILE_ELEMENT = "icmBodySpellElement";
    private static final String FROST = "icmBodyFrost";
    private static final String POISON_OWNER = "icmBodyPoisonOwner";
    public static final String BURN_OWNER = "icmBodyBurnOwner";
    private static final ResourceLocation HEALTH = id("body_health"), ARMOR = id("body_armor"),
            SPEED = id("body_speed"), ATTACK_SPEED = id("body_attack_speed");
    private static final Map<Player, State> STATES = new WeakHashMap<>();
    private static final Map<DamageSource, String> ELEMENTS = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> THUNDER_PROC = ThreadLocal.withInitial(() -> false);

    private static final class State {
        String body = "";
        Vec3 lastPosition;
        ResourceLocation dimension;
        int stillTicks, thunderAttacks;
        long dodgeUntil, defenseUntil, defenseReadyAt, lastThunderTime = Long.MIN_VALUE;
        java.util.UUID lastThunderSource;
    }

    private BodyTypeEffects() {}
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, name); }
    public static String body(Entity entity) { return entity instanceof Player p ? ModAttachments.getData(p).bodyType() : ""; }
    private static boolean has(Entity entity, String body) { return body.equals(body(entity)); }

    private static State state(Player player) {
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        String body = body(player);
        var dimension = player.level().dimension().location();
        if (!body.equals(state.body) || !dimension.equals(state.dimension)) {
            state = new State(); state.body = body; state.dimension = dimension;
            STATES.put(player, state);
        }
        return state;
    }

    public static int maxQi(ModAttachments.CultivationData data) {
        long base = Math.max(0L, (long) CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        int percent = qiPercent(data.bodyType());
        return (int) Math.min(Integer.MAX_VALUE, base * percent / 100);
    }

    private static int qiPercent(String body) {
        return BodyTypes.VAST_SEA.equals(body) ? 120 : BodyTypes.NINE_ORIFICE.equals(body) ? 110 : 100;
    }
    /** Debug input is the visible maximum, including the current body's multiplier. */
    public static int baseQiForMaximum(String body, int maximum) {
        int percent = qiPercent(body);
        return (int)((Math.max(0L, maximum) * 100 + percent - 1) / percent);
    }

    public static double healthMultiplier(String body) {
        return BodyTypes.EVERGREEN.equals(body) || BodyTypes.GOLDEN_BODY.equals(body) ? 1.15
                : BodyTypes.ANCIENT_SACRED.equals(body) ? 1.25 : 1;
    }
    public static double armorMultiplier(Entity entity) {
        return has(entity, BodyTypes.PROFOUND_EARTH) ? 1.2 : has(entity, BodyTypes.ANCIENT_SACRED) ? 1.15 : 1;
    }
    private static boolean sword(ItemStack item) { return item != null && (item.is(ItemTags.SWORDS) || item.getItem() instanceof SwordItem); }

    private static void modifier(Player player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 ResourceLocation id, double bonus) {
        var value = player.getAttribute(attribute);
        if (value == null) return;
        var previous = value.getModifier(id);
        if (previous == null && bonus == 0) return;
        if (previous != null && previous.amount() == bonus) return;
        value.removeModifier(id);
        if (bonus != 0) value.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void refresh(Player player) {
        String body = body(player);
        modifier(player, Attributes.MAX_HEALTH, HEALTH, healthMultiplier(body) - 1);
        modifier(player, Attributes.ARMOR, ARMOR, armorMultiplier(player) - 1);
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED, BodyTypes.WINDCHASER.equals(body) ? .2 : 0);
        modifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED, BodyTypes.WINDCHASER.equals(body) ? .15
                : BodyTypes.GENG_METAL.equals(body) && sword(player.getMainHandItem()) ? .1 : 0);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static void bodyChanged(Player player) {
        STATES.remove(player);
        refresh(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive()) { STATES.remove(player); return; }
        refresh(player);
        if (has(player, BodyTypes.PROFOUND_EARTH)) {
            State state = state(player);
            Vec3 position = player.position();
            state.stillTicks = player.onGround() && state.lastPosition != null && state.lastPosition.distanceToSqr(position) < .000001
                    && player.getDeltaMovement().lengthSqr() < .0001 ? Math.min(60, state.stillTicks + 1) : 0;
            state.lastPosition = position;
        }
        if (has(player, BodyTypes.SOLAR_DIVINE)) {
            player.clearFire();
            for (var effect : java.util.List.of(ModEffects.SPIRIT_FLAME_BURN, ModEffects.QI_FIRE_BURN,
                    ModEffects.EMBER_BURN, ModEffects.CORPSE_FIRE, ModEffects.BA_FIRE)) player.removeEffect(effect);
        }
        if (has(player, BodyTypes.MYRIAD_POISON)) {
            player.removeEffect(MobEffects.POISON); player.removeEffect(ModEffects.SHIDU);
        }
        int maximum = maxQi(ModAttachments.getData(player));
        if (ModAttachments.getData(player).qi() > maximum)
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(maximum));
    }

    public static boolean sunlight(LivingEntity entity) {
        return entity.level().isDay() && entity.level().dimensionType().hasSkyLight()
                && entity.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        entity.blockPosition().getX(), entity.blockPosition().getZ()) <= entity.blockPosition().getY()
                && entity.level().canSeeSky(entity.blockPosition()) && !entity.level().isRainingAt(entity.blockPosition());
    }
    public static int qiRegeneration(Player player, int base) {
        double multiplier = has(player, BodyTypes.NINE_ORIFICE) ? 1.25 : has(player, BodyTypes.ANCIENT_SACRED) ? .85 : 1;
        if (has(player, BodyTypes.SUPREME_YIN)) {
            if (player.level().isNight()) multiplier = 1.2;
            else if (sunlight(player)) multiplier = .85;
        }
        return (int)Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(base * multiplier)));
    }
    public static double spellCostMultiplier(Player player, String element) {
        return spellCostMultiplier(ModAttachments.getData(player), element) * com.example.immortal_cultivation_mod.alchemy.PillToxicity.multiplier(player,"Cost");
    }
    public static double spellCostMultiplier(ModAttachments.CultivationData data, String element) {
        return BodyTypes.INNATE_DAO.equals(data.bodyType()) ? .9
                : BodyTypes.VAST_SEA.equals(data.bodyType()) && SpiritRoots.elementMatches(element, SpiritRoots.WATER) ? .85 : 1;
    }
    public static int cooldown(Player player, int ticks) {
        return has(player, BodyTypes.NINE_ORIFICE) && ticks > 0 ? Math.max(1, (int) Math.ceil(ticks * .9)) : ticks;
    }
    public static double cultivationMultiplier(Player player) { return (has(player, BodyTypes.INNATE_DAO) ? 1.15 : 1) * com.example.immortal_cultivation_mod.alchemy.PillToxicity.multiplier(player,"Cultivation"); }
    public static long cultivationGain(Player player, long gain) {
        return Math.max(0L, Math.round(Math.max(0L, gain) * cultivationMultiplier(player)));
    }
    public static float healingMultiplier(LivingEntity entity) {
        return has(entity, BodyTypes.EVERGREEN) ? 1.1F : has(entity, BodyTypes.IMMACULATE) ? 1.15F : 1;
    }
    public static float naturalHealing(Player player, float amount) {
        return amount * (has(player, BodyTypes.EVERGREEN) ? 1.25F
                : has(player, BodyTypes.SOLAR_DIVINE) && sunlight(player) ? 1.2F : 1);
    }
    public static float pillHealing(LivingEntity entity, float amount) { return has(entity, BodyTypes.BLOOD_FIEND) ? amount * .8F : amount; }
    public static void dodged(LivingEntity entity) {
        if (entity instanceof ServerPlayer player && has(player, BodyTypes.WINDCHASER)) state(player).dodgeUntil = player.level().getGameTime() + 60;
    }
    public static float armorBypass(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && has(attacker, BodyTypes.GENG_METAL)
                && swordStrike(source, attacker) ? .1F : 0;
    }
    private static boolean swordStrike(DamageSource source, LivingEntity attacker) {
        return UndeadAbilitySystem.isDirectMelee(source, attacker) && sword(sourceWeapon(source, attacker))
                || source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.SwordTechniqueProjectileEntity
                || source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.SwordTechniqueMarkEntity
                || source.getDirectEntity() instanceof com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
    }
    private static ItemStack sourceWeapon(DamageSource source, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) return com.example.immortal_cultivation_mod.item.WeaponSkillAttack.sourceWeapon(player, source);
        ItemStack weapon = source.getWeaponItem();
        return weapon == null || weapon.isEmpty() ? attacker.getMainHandItem() : weapon;
    }
    public static float shieldMultiplier(DamageSource source) { return has(source.getEntity(), BodyTypes.SPIRIT_DEVOURING) ? 1.2F : 1; }

    /** Explicit metadata for delayed area pulses; never guesses from a player's previous cast. */
    public static DamageSource spellSource(DamageSource source, String spellId) {
        var spell = ModSpells.get(spellId);
        if (spell != null) ELEMENTS.put(source, spell.element());
        return source;
    }
    public static String element(DamageSource source) {
        String explicit = ELEMENTS.get(source);
        if (explicit != null) return explicit;
        Entity direct = source.getDirectEntity();
        String spellId = direct instanceof ElementalTechniqueProjectileEntity p ? p.spellId()
                : direct instanceof SpellTriggerProjectileEntity p ? p.spellId() : "";
        if (!spellId.isEmpty() && ModSpells.get(spellId) != null) return ModSpells.get(spellId).element();
        if (direct != null && direct.getPersistentData().contains(PROJECTILE_ELEMENT)) return direct.getPersistentData().getString(PROJECTILE_ELEMENT);
        if (source.is(DamageTypeTags.IS_FIRE)) return SpiritRoots.FIRE;
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return SpiritRoots.THUNDER;
        if (source.is(DamageTypes.FREEZE)) return SpiritRoots.ICE;
        return CenterEffectSupport.currentSpellElement(source.getEntity());
    }
    public static double elementalMultiplier(String body, String element) {
        if (element == null || element.isBlank()) return 1;
        return switch (body) {
            case BodyTypes.CRIMSON_FLAME -> SpiritRoots.elementMatches(element, SpiritRoots.FIRE) ? 1.25 : 1;
            case BodyTypes.PROFOUND_ICE -> SpiritRoots.elementMatches(element, SpiritRoots.ICE) ? 1.25 : 1;
            case BodyTypes.VIOLET_THUNDER -> SpiritRoots.elementMatches(element, SpiritRoots.THUNDER) ? 1.25 : 1;
            case BodyTypes.VAST_SEA -> SpiritRoots.elementMatches(element, SpiritRoots.WATER) ? 1.2 : 1;
            case BodyTypes.SUPREME_YIN -> SpiritRoots.elementMatches(element, SpiritRoots.YIN) || SpiritRoots.elementMatches(element, SpiritRoots.ICE) ? 1.3 : 1;
            case BodyTypes.SOLAR_DIVINE -> SpiritRoots.elementMatches(element, SpiritRoots.YANG) || SpiritRoots.elementMatches(element, SpiritRoots.FIRE) ? 1.3 : 1;
            case BodyTypes.INNATE_DAO -> elemental(element) ? 1.05 : 1;
            case BodyTypes.MYRIAD_POISON -> element.equals("poison") ? 1.25 : 1;
            default -> 1;
        };
    }

    private static boolean elemental(String elements) {
        for (String element : SpiritRoots.ALL_ELEMENTS) if (SpiritRoots.elementMatches(elements, element)) return true;
        return false;
    }

    public static void outgoing(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || THUNDER_PROC.get()) return;
        DamageSource source = event.getSource();
        boolean melee = UndeadAbilitySystem.isDirectMelee(source, player);
        String body = body(player), element = element(source);
        double multiplier = elementalMultiplier(body, element);
        if (BodyTypes.CRIMSON_FLAME.equals(body) && event.getEntity().isOnFire()) multiplier *= 1.1;
        if (BodyTypes.BLOOD_FIEND.equals(body) && melee) multiplier *= 1.2;
        if (BodyTypes.GENG_METAL.equals(body) && swordStrike(source, player)) multiplier *= 1.2;
        if (BodyTypes.ANCIENT_SACRED.equals(body) && element.isBlank()
                && (melee || source.is(DamageTypeTags.IS_PROJECTILE))) multiplier *= 1.2;
        if (BodyTypes.WINDCHASER.equals(body) && state(player).dodgeUntil > player.level().getGameTime()) multiplier *= 1.1;
        if (BodyTypes.SPIRIT_DEVOURING.equals(body) && event.getEntity().isBlocking()) multiplier *= 1.2;
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void burningImmunity(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && has(player, BodyTypes.SOLAR_DIVINE)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true); player.clearFire();
        }
        if (has(event.getEntity(), BodyTypes.MYRIAD_POISON)
                && (event.getSource().is(net.neoforged.neoforge.common.NeoForgeMod.POISON_DAMAGE)
                || "poison".equals(ELEMENTS.get(event.getSource())))) event.setCanceled(true);
    }

    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (has(player, BodyTypes.SOLAR_DIVINE) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true); player.clearFire(); return;
        }
        if (has(player, BodyTypes.PROFOUND_EARTH)) {
            State state = state(player);
            if (state.stillTicks >= 60 && player.onGround() && state.lastPosition != null
                    && state.lastPosition.distanceToSqr(player.position()) < .000001
                    && player.getDeltaMovement().lengthSqr() < .0001) event.setAmount(event.getAmount() * .9F);
        }
        if (has(player, BodyTypes.GOLDEN_BODY)) {
            event.setAmount(event.getAmount() * .85F);
            if (state(player).defenseUntil > player.level().getGameTime()) event.setAmount(event.getAmount() * .7F);
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer defender && has(defender, BodyTypes.GOLDEN_BODY)
                && defender.isAlive() && defender.getHealth() < defender.getMaxHealth() * .2F) {
            State state = state(defender); long now = defender.level().getGameTime();
            if (event.getNewDamage() > 0 && now >= state.defenseReadyAt) {
                state.defenseUntil = now + 100; state.defenseReadyAt = now + 1800;
            }
        }
        if (event.getNewDamage() <= 0 || THUNDER_PROC.get() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity target = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FIRE) || SpiritRoots.elementMatches(element(event.getSource()), SpiritRoots.FIRE))
            target.getPersistentData().putUUID(BURN_OWNER, player.getUUID());
        if (has(player, BodyTypes.BLOOD_FIEND) && UndeadAbilitySystem.isDirectMelee(event.getSource(), player)) player.heal(event.getNewDamage() * .05F);
        if (has(player, BodyTypes.PROFOUND_ICE) && target.isAlive()
                && !event.getSource().is(DamageTypes.ON_FIRE) && !event.getSource().is(DamageTypes.IN_FIRE)
                && !"poison".equals(element(event.getSource()))) {
            CompoundTag frost = target.getPersistentData().getCompound(FROST);
            long now = target.level().getGameTime();
            int stacks = frost.getLong("Until") > now ? frost.getInt("Stacks") + 1 : 1;
            frost.putInt("Stacks", stacks); frost.putLong("Until", now + 400);
            target.getPersistentData().put(FROST, frost);
            if (stacks >= 5) {
                target.getPersistentData().remove(FROST);
                target.addEffect(new MobEffectInstance(ModEffects.FROZEN, 40), player);
                player.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + .7, target.getZ(), 20, .4, .5, .4, .02);
            }
        }
        if (has(player, BodyTypes.VIOLET_THUNDER) && SpiritRoots.elementMatches(element(event.getSource()), SpiritRoots.THUNDER)) {
            State state = state(player); long now = player.level().getGameTime();
            Entity direct = event.getSource().getDirectEntity();
            java.util.UUID attack = direct == null ? null : direct.getUUID();
            // An area attack hitting several creatures still counts as one attack.
            if (state.lastThunderTime == now && java.util.Objects.equals(state.lastThunderSource, attack)) return;
            state.lastThunderTime = now; state.lastThunderSource = attack;
            if (++state.thunderAttacks < 5) return;
            state.thunderAttacks = 0;
            if (!target.isAlive()) return;
            // The passive's bolt is visual only, including copper and lightning-rod interactions.
            var bolt = new net.minecraft.world.entity.LightningBolt(EntityType.LIGHTNING_BOLT, player.serverLevel()) {
                @Override public void tick() { baseTick(); if (tickCount >= 6) discard(); }
            };
            bolt.moveTo(target.position()); bolt.setVisualOnly(true); player.serverLevel().addFreshEntity(bolt);
            THUNDER_PROC.set(true);
            try {
                target.invulnerableTime = 0;
                var type = player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
                target.hurt(new DamageSource(type, player, player), event.getNewDamage() * .5F);
            } finally { THUNDER_PROC.remove(); }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof Player player) STATES.remove(player);
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (player.isAlliedTo(event.getEntity())
                || com.example.immortal_cultivation_mod.entity.NpcProfile.areCombatAllies(player, event.getEntity())
                || event.getEntity() instanceof net.minecraft.world.entity.OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) return;
        double restored = has(player, BodyTypes.SPIRIT_DEVOURING) ? .05
                : has(player, BodyTypes.MYRIAD_POISON) && (event.getEntity().hasEffect(MobEffects.POISON)
                    || event.getEntity().hasEffect(ModEffects.SHIDU)) ? .01 : 0;
        if (restored <= 0) return;
        var data = ModAttachments.getData(player);
        int maximum = maxQi(data);
        ModAttachments.setData(player, data.withQi((int) Math.min(maximum, data.qi() + Math.round(maximum * restored))));
    }

    public static boolean poison(Holder<MobEffect> effect) { return effect.equals(MobEffects.POISON) || effect.equals(ModEffects.SHIDU); }
    public static boolean rejectEffect(LivingEntity entity, MobEffectInstance effect) {
        if (entity.level().isClientSide) return false;
        if (has(entity, BodyTypes.MYRIAD_POISON) && poison(effect.getEffect())) return true;
        if (has(entity, BodyTypes.SOLAR_DIVINE) && (effect.is(ModEffects.SPIRIT_FLAME_BURN)
                || effect.is(ModEffects.QI_FIRE_BURN) || effect.is(ModEffects.EMBER_BURN)
                || effect.is(ModEffects.CORPSE_FIRE) || effect.is(ModEffects.BA_FIRE))) return true;
        return has(entity, BodyTypes.IMMACULATE) && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && entity.getRandom().nextFloat() < .3F;
    }
    public static boolean shortenEffect(LivingEntity entity, MobEffectInstance effect) {
        return !entity.level().isClientSide && has(entity, BodyTypes.IMMACULATE)
                && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }
    public static void effectAdded(LivingEntity entity, MobEffectInstance effect, Entity source) {
        if (!entity.level().isClientSide && poison(effect.getEffect())) {
            String key = POISON_OWNER + (effect.is(ModEffects.SHIDU) ? ".corpse" : ".vanilla");
            if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile) source = projectile.getOwner();
            if (source instanceof LivingEntity) entity.getPersistentData().putUUID(key, source.getUUID());
            else entity.getPersistentData().remove(key);
        }
    }
    public static DamageSource poisonSource(LivingEntity target, DamageSource original) {
        if (target.level().isClientSide) return original;
        String key = POISON_OWNER + (original.is(net.neoforged.neoforge.common.NeoForgeMod.POISON_DAMAGE) ? ".vanilla" : ".corpse");
        if (target.level() instanceof ServerLevel level && target.getPersistentData().hasUUID(key)
                && level.getEntity(target.getPersistentData().getUUID(key)) instanceof LivingEntity owner) {
            DamageSource source = new DamageSource(original.typeHolder(), target, owner);
            ELEMENTS.put(source, "poison"); return source;
        }
        DamageSource source = new DamageSource(original.typeHolder(), original.getDirectEntity(), original.getEntity());
        ELEMENTS.put(source, "poison");
        return source;
    }

    public static DamageSource burnSource(LivingEntity target, DamageSource original) {
        if (original.getEntity() != null || !target.isOnFire()
                || !(original.is(DamageTypes.ON_FIRE) || original.is(DamageTypes.IN_FIRE))) return original;
        if (target.level() instanceof ServerLevel level && target.getPersistentData().hasUUID(BURN_OWNER)
                && level.getEntity(target.getPersistentData().getUUID(BURN_OWNER)) instanceof ServerPlayer owner)
            return new DamageSource(original.typeHolder(), original.getDirectEntity(), owner);
        return original;
    }
}
