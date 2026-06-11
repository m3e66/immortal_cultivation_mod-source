package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivationMod.MODID);

    public static final Supplier<AttachmentType<CultivationData>> CULTIVATION_DATA = ATTACHMENTS.register(
            "cultivation_data",
            () -> AttachmentType.builder(CultivationData::createDefault)
                    .serialize(CultivationData.CODEC)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    public static CultivationData getData(net.minecraft.world.entity.player.Player player) {
        return player.getData(CULTIVATION_DATA);
    }

    public static void setData(net.minecraft.world.entity.player.Player player, CultivationData data) {
        player.setData(CULTIVATION_DATA, data);
    }

    public record CultivationData(
            int qi,
            String cultivationLevel,
            int luck,
            int moral,
            String bodyType,
            int soul,
            int thoughts,
            List<String> spiritRoots,
            String spiritRootGrade,
            int agePenalty,
            long cultivationProgress,
            String activeCultivationMethod,
            int blood,
            List<String> knownSpells,
            boolean isMeditating,
            int skillPoints,
            int maxHpBonus,
            int maxQiBonus,
            int maxEnergyBonus,
            int physicalAttack,
            int magicAttack,
            int mentalAttack
    ) {
        public static final Codec<CultivationData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("qi").forGetter(CultivationData::qi),
                        Codec.STRING.optionalFieldOf("cultivationLevel", CultivationLevels.REALM_MORTAL).forGetter(CultivationData::cultivationLevel),
                        Codec.INT.optionalFieldOf("luck", 50).forGetter(CultivationData::luck),
                        Codec.INT.optionalFieldOf("moral", 50).forGetter(CultivationData::moral),
                        Codec.STRING.optionalFieldOf("bodyType", CultivationLevels.REALM_MORTAL).forGetter(CultivationData::bodyType),
                        Codec.INT.optionalFieldOf("soul", 100).forGetter(CultivationData::soul),
                        Codec.INT.optionalFieldOf("thoughts", 100).forGetter(CultivationData::thoughts),
                        Codec.STRING.listOf().optionalFieldOf("spiritRoots", List.of()).forGetter(CultivationData::spiritRoots),
                        Codec.STRING.optionalFieldOf("spiritRootGrade", "").forGetter(CultivationData::spiritRootGrade),
                        Codec.INT.optionalFieldOf("agePenalty", 0).forGetter(CultivationData::agePenalty),
                        Codec.LONG.optionalFieldOf("cultivationProgress", 0L).forGetter(CultivationData::cultivationProgress),
                        Codec.STRING.optionalFieldOf("activeCultivationMethod", CultivationMethods.NONE).forGetter(CultivationData::activeCultivationMethod),
                        Codec.INT.optionalFieldOf("blood", 0).forGetter(CultivationData::blood),
                        Codec.STRING.listOf().optionalFieldOf("knownSpells", List.of()).forGetter(CultivationData::knownSpells),
                        Codec.BOOL.optionalFieldOf("isMeditating", false).forGetter(CultivationData::isMeditating),
                        SkillStats.CODEC.optionalFieldOf("skillStats", SkillStats.EMPTY).forGetter(CultivationData::skillStats)
                ).apply(instance, (qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating, stats) ->
                        new CultivationData(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                                stats.skillPoints(), stats.maxHpBonus(), stats.maxQiBonus(), stats.maxEnergyBonus(), stats.physicalAttack(), stats.magicAttack(), stats.mentalAttack())));

        private SkillStats skillStats() {
            return new SkillStats(skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withQi(int qi) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withCultivationLevel(String level) {
            return copy(qi, level, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withKnownSpells(List<String> spells) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, spells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withBodyType(String bodyType) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withMoral(int moral) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withLuck(int luck) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withSoul(int soul) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withThoughts(int thoughts) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withSpiritRoots(List<String> roots, String grade) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, roots, grade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withAgePenalty(int penalty) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, penalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withCultivationProgress(long progress) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, progress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withActiveCultivationMethod(String methodId) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, methodId, blood, knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withBlood(int blood) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, Math.max(0, blood), knownSpells, isMeditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withMeditating(boolean meditating) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, meditating,
                    skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData withAddedSkillPoints(int points) {
            return copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                    skillPoints + points, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        public CultivationData spendSkillPoint(String stat) {
            if (skillPoints <= 0) {
                return this;
            }
            return switch (stat) {
                case "hp" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus + 4, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
                case "qi" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus, maxQiBonus + 40, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
                case "energy" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus, maxQiBonus, maxEnergyBonus + 40, physicalAttack, magicAttack, mentalAttack);
                case "physical" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack + 1, magicAttack, mentalAttack);
                case "magic" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack + 1, mentalAttack);
                case "mental" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints - 1, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack + 1);
                default -> this;
            };
        }

        public CultivationData debugAdjustStat(String stat, int delta) {
            return switch (stat) {
                case "skill_points" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        Math.max(0, skillPoints + delta), maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
                case "max_hp" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, Math.max(0, maxHpBonus + delta), maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
                case "max_qi" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, maxHpBonus, Math.max(0, maxQiBonus + delta), maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
                case "max_energy" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, maxHpBonus, maxQiBonus, Math.max(0, maxEnergyBonus + delta), physicalAttack, magicAttack, mentalAttack);
                case "physical" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, Math.max(0, physicalAttack + delta), magicAttack, mentalAttack);
                case "magic" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, Math.max(0, magicAttack + delta), mentalAttack);
                case "mental" -> copy(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress, activeCultivationMethod, blood, knownSpells, isMeditating,
                        skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, Math.max(0, mentalAttack + delta));
                default -> this;
            };
        }

        public static CultivationData createDefault() {
            return new CultivationData(0, CultivationLevels.REALM_MORTAL, 50, 50, CultivationLevels.REALM_MORTAL, 100, 100,
                    new ArrayList<>(), "", 0, 0L, CultivationMethods.NONE, 0, new ArrayList<>(), false, 0, 0, 0, 0, 0, 0, 0);
        }

        private static CultivationData copy(
                int qi,
                String cultivationLevel,
                int luck,
                int moral,
                String bodyType,
                int soul,
                int thoughts,
                List<String> spiritRoots,
                String spiritRootGrade,
                int agePenalty,
                long cultivationProgress,
                String activeCultivationMethod,
                int blood,
                List<String> knownSpells,
                boolean isMeditating,
                int skillPoints,
                int maxHpBonus,
                int maxQiBonus,
                int maxEnergyBonus,
                int physicalAttack,
                int magicAttack,
                int mentalAttack
        ) {
            return new CultivationData(qi, cultivationLevel, luck, moral, bodyType, soul, thoughts, spiritRoots, spiritRootGrade, agePenalty, cultivationProgress,
                    activeCultivationMethod, blood, knownSpells, isMeditating, skillPoints, maxHpBonus, maxQiBonus, maxEnergyBonus, physicalAttack, magicAttack, mentalAttack);
        }

        private record SkillStats(
                int skillPoints,
                int maxHpBonus,
                int maxQiBonus,
                int maxEnergyBonus,
                int physicalAttack,
                int magicAttack,
                int mentalAttack
        ) {
            private static final SkillStats EMPTY = new SkillStats(0, 0, 0, 0, 0, 0, 0);

            private static final Codec<SkillStats> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.INT.optionalFieldOf("skillPoints", 0).forGetter(SkillStats::skillPoints),
                            Codec.INT.optionalFieldOf("maxHpBonus", 0).forGetter(SkillStats::maxHpBonus),
                            Codec.INT.optionalFieldOf("maxQiBonus", 0).forGetter(SkillStats::maxQiBonus),
                            Codec.INT.optionalFieldOf("maxEnergyBonus", 0).forGetter(SkillStats::maxEnergyBonus),
                            Codec.INT.optionalFieldOf("physicalAttack", 0).forGetter(SkillStats::physicalAttack),
                            Codec.INT.optionalFieldOf("magicAttack", 0).forGetter(SkillStats::magicAttack),
                            Codec.INT.optionalFieldOf("mentalAttack", 0).forGetter(SkillStats::mentalAttack)
                    ).apply(instance, SkillStats::new));
        }
    }
}
