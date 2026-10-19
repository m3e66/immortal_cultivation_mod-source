package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.item.WeaponShardSystem;
import com.example.immortal_cultivation_mod.item.WeaponTribulation;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class FlameForgingGameTests {
    private static final BlockPos FORGE = new BlockPos(3,2,3);

    @GameTest(template = "fate_test")
    public static void everyAnimatedFlameBlockSavesKindAndKeepsLegacyHeatSeparate(GameTestHelper h) {
        var ding = forge(h);
        BlockPos firePos = h.absolutePos(FORGE.below());
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            h.setBlock(FORGE.below(), kind.block());
            var state = h.getLevel().getBlockState(firePos);
            var blockEntity = h.getLevel().getBlockEntity(firePos);
            h.assertTrue(state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED
                            && blockEntity instanceof SpiritFlameBlockEntity flame && flame.kind() == kind,
                    "Each placed flame uses its real animated block entity: " + kind);
            var savedState = NbtUtils.writeBlockState(state);
            var loadedState = NbtUtils.readBlockState(
                    h.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK), savedState);
            var savedEntity = blockEntity.saveWithFullMetadata(h.getLevel().registryAccess());
            var loadedEntity = BlockEntity.loadStatic(firePos, loadedState, savedEntity, h.getLevel().registryAccess());
            h.assertTrue(loadedState.is(kind.block()) && loadedEntity instanceof SpiritFlameBlockEntity flame
                            && flame.kind() == kind && !loadedState.isRandomlyTicking(),
                    "Saved block state and entity metadata restore the exact persistent flame kind: " + kind);
        }
        h.setBlock(FORGE.below(), ModBlocks.LING_FIRE.get());
        h.assertTrue(ding.lowestFireTemp() == 0 && ding.highestFireTemp() == 4000,
                "Legacy Spirit Fire keeps its original 4000-degree ceiling");
        h.setBlock(FORGE.below(), ModBlocks.ZHEN_FIRE.get());
        h.assertTrue(ding.lowestFireTemp() == 0 && ding.highestFireTemp() == 10000
                        && !(h.getLevel().getBlockEntity(firePos) instanceof SpiritFlameBlockEntity),
                "Legacy True Fire retains its separate behavior and ceiling with a zero-degree floor");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void catalogBlocksSupplyTheirActualHeatAndRequireSupport(GameTestHelper h) {
        var ding = forge(h);
        h.assertTrue(FlameKind.CRIMSON.power()==320 && FlameKind.CRIMSON.stability()==78,
                "Crimson flame preserves the specified forge power and stability");
        for (FlameKind kind : FlameKind.values()) {
            h.assertTrue(FlameKind.fromId(kind.id())==kind && kind.minTemperature()<kind.maxTemperature(),
                    "Saved flame IDs resolve to a usable temperature range: " + kind);
            if (!kind.special()) continue;
            h.setBlock(FORGE.below(), kind.block());
            h.assertTrue(ding.hasFireBelow() && ding.lowestFireTemp()==kind.minTemperature()
                            && ding.highestFireTemp()==kind.maxTemperature() && ding.fireCost()==kind.qiCost(),
                    "A real flame block drives the ding's temperature and Qi cost: " + kind);
            var state = h.getLevel().getBlockState(h.absolutePos(FORGE.below()));
            h.assertTrue(state.getBlock() instanceof SpiritFlameBlock flame && flame.kind()==kind
                            && state.canSurvive(h.getLevel(), h.absolutePos(FORGE.below())),
                    "Placed flames retain their kind and survive on a solid support");
            h.setBlock(FORGE.below().below(), Blocks.AIR);
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(FORGE.below())).isAir(),
                    "Removing the support removes the permanent flame without spreading it");
            h.setBlock(FORGE.below().below(), Blocks.STONE);
        }
        for (var source : List.of(Blocks.LAVA.defaultBlockState(),
                Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true),
                Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true),
                Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.LIT,true))) {
            h.setBlock(FORGE.below(), source);
            h.assertTrue(ding.hasFireBelow() && ding.lowestFireTemp()==0
                            && ding.highestFireTemp()==4000 && ding.fireCost()==10,
                    "Lava and burning furnaces reach Qi Refining forge heat without reaching Foundation heat");
        }
        h.setBlock(FORGE.below(), Blocks.FURNACE);
        h.assertTrue(!ding.hasFireBelow(), "An unlit furnace cannot supply forge heat");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void specialFlameCeilingGuardsNativeWeaponsAndOriginalShardRank(GameTestHelper h) {
        var foundation = ForgingSystem.recipeById("black_iron_jian");
        var goldenCore = ForgingSystem.recipeById("seven_star_sword");
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            h.assertTrue(ForgingSystem.canUseFire(foundation,kind.id()),
                    "Every catalog spirit flame reaches Foundation forging heat");
            boolean goldenCoreEligible = switch (kind) {
                case WOOD, SOLAR, NETHER, THUNDER, DRAGON, HEAVENLY -> true;
                default -> false;
            };
            h.assertTrue(ForgingSystem.canUseFire(goldenCore,kind.id())==goldenCoreEligible,
                    "Only flames reaching Golden Core heat qualify for its weapon recipe");
        }
        var original = new ItemStack(ModItems.SEVEN_STAR_SWORD.get());
        WeaponRestrictionSystem.debugSetTier(original,7);
        var shards = new ArrayList<>(WeaponShardSystem.split(original,h.getLevel().registryAccess()));
        shards.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),64));
        shards.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),64));
        var reforge = ForgingSystem.recipeById(WeaponShardSystem.REFORGE);
        h.assertTrue(!ForgingSystem.canUseFire(reforge,FlameKind.EARTH.id(),shards)
                        && !ForgingSystem.canUseFire(reforge,FlameKind.CRIMSON.id(),shards)
                        && !ForgingSystem.canUseFire(reforge,"normal_fire",shards)
                        && !ForgingSystem.canUseFire(reforge,"ling_fire",shards)
                        && ForgingSystem.canUseFire(reforge,"zhen_fire",shards)
                        && ForgingSystem.canUseFire(reforge,FlameKind.WOOD.id(),shards),
                "Shard reforging cannot bypass its native Golden Core heat with ordinary or weaker flames");
        try (var p = new Forger(h)) {
            var ding = forge(h);
            fill(ding,goldenCore,materials(goldenCore));
            h.setBlock(FORGE.below(),FlameKind.EARTH.block());
            int qi = ModAttachments.getData(p.player).qi();
            ding.startForging(p.player);
            h.assertTrue(ding.data(4)==0 && ModAttachments.getData(p.player).qi()==qi,
                    "An insufficient flame cannot start the forge or spend player Qi");
            h.setBlock(FORGE.below(),FlameKind.WOOD.block());
            ding.startForging(p.player);
            for (int i=0;i<26;i++) ding.heatPulse(p.player);
            h.assertTrue(ding.data(4)==1 && ding.data(0)==6500 && ding.data(1)==6500
                            && ModAttachments.getData(p.player).qi()==qi-100-26*FlameKind.WOOD.qiCost(),
                    "Wood flame really heats a Golden Core batch and charges its catalog pulse cost");
            var saved = ding.saveWithFullMetadata(h.getLevel().registryAccess());
            var loaded = new DingBlockEntity(ding.getBlockPos(),ding.getBlockState());
            loaded.loadWithComponents(saved,h.getLevel().registryAccess()); loaded.setLevel(h.getLevel());
            var menu = new DingMenu(1,p.player.getInventory(),loaded);
            h.assertTrue(saved.getString("activeFireType").equals(FlameKind.WOOD.id())
                            && menu.temperature()==6500 && menu.targetTemperature()==6500
                            && menu.lowTemp()==FlameKind.WOOD.minTemperature()
                            && menu.highTemp()==FlameKind.WOOD.maxTemperature()
                            && menu.fireCost()==FlameKind.WOOD.qiCost(),
                    "A saved and reopened batch retains its actual flame identity, target and heat");

            var reforging = forge(h);
            h.setBlock(FORGE.below(),FlameKind.WOOD.block());
            fill(reforging,reforge,shards);
            reforging.startForging(p.player);
            h.assertTrue(reforging.data(4)==1,"Start a real Golden Core shard reforge with sufficient heat");
            var oldReforge = reforging.saveWithFullMetadata(h.getLevel().registryAccess());
            oldReforge.putString("activeFireType","normal_fire");
            h.setBlock(FORGE.below(),Blocks.FIRE);
            var weak = new DingBlockEntity(reforging.getBlockPos(),reforging.getBlockState());
            weak.loadWithComponents(oldReforge,h.getLevel().registryAccess()); weak.setLevel(h.getLevel());
            DingBlockEntity.serverTick(h.getLevel(),weak.getBlockPos(),weak.getBlockState(),weak);
            h.assertTrue(weak.data(4)==0 && weak.getItem(DingBlockEntity.RESULT_SLOT).isEmpty(),
                    "An old ordinary-fire reforge cannot bypass the serialized Golden Core heat requirement");
            for (int i=0;i<shards.size();i++) {
                h.assertTrue(ItemStack.matches(weak.getItem(DingBlockEntity.MATERIAL_START+i),
                                reforging.getItem(DingBlockEntity.MATERIAL_START+i)),
                        "Rejected saved reforge preserves every unmelted shard and stone stack");
            }
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void savedFlameBatchesRejectOldTargetsAndClampHeatBeforeSampling(GameTestHelper h) {
        try (var p = new Forger(h)) {
            h.getLevel().addNewPlayer(p.player);
            var recipe = ForgingSystem.recipeById("seven_star_sword");
            var ding = forge(h);
            fill(ding,recipe,materials(recipe));
            h.setBlock(FORGE.below(),FlameKind.WOOD.block());
            ding.startForging(p.player);
            h.assertTrue(ding.data(4)==1,"Create a valid saved Golden Core batch");
            var saved = ding.saveWithFullMetadata(h.getLevel().registryAccess());
            saved.putInt("progressTicks",20);
            saved.putInt("maxProgressTicks",Math.max(200,saved.getInt("maxProgressTicks")));
            saved.putInt("temperature",FlameKind.WOOD.maxTemperature()+2000);
            var loaded = new DingBlockEntity(ding.getBlockPos(),ding.getBlockState());
            loaded.loadWithComponents(saved,h.getLevel().registryAccess());
            loaded.setLevel(h.getLevel());
            DingBlockEntity.serverTick(h.getLevel(),loaded.getBlockPos(),loaded.getBlockState(),loaded);
            h.assertTrue(loaded.data(4)==1 && loaded.data(0)==FlameKind.WOOD.maxTemperature()-35,
                    "Reloaded heat is clamped to the live flame ceiling before ordinary cooling");
            var ticked = loaded.saveWithFullMetadata(h.getLevel().registryAccess());
            if (h.getLevel().getGameTime()%20L==0L) {
                h.assertTrue(ticked.getLong("temperatureSamples")==1
                                && ticked.getLong("temperatureTotal")==FlameKind.WOOD.maxTemperature(),
                        "A quality sample receives the clamped heat, never the stale saved temperature");
            }
            loaded.heatPulse(p.player);
            var menu = new DingMenu(1,p.player.getInventory(),loaded);
            h.assertTrue(menu.temperature()==FlameKind.WOOD.maxTemperature()
                            && menu.lowTemp()==FlameKind.WOOD.minTemperature()
                            && menu.highTemp()==FlameKind.WOOD.maxTemperature()
                            && menu.fireCost()==FlameKind.WOOD.qiCost(),
                    "Heat pulses and reopened menu data obey the live flame range and Qi cost");

            var oldCrimson = saved.copy();
            oldCrimson.putString("activeFireType",FlameKind.CRIMSON.id());
            oldCrimson.putInt("targetTemperature",6500);
            oldCrimson.putInt("temperature",6500);
            h.setBlock(FORGE.below(),FlameKind.CRIMSON.block());
            var rejected = new DingBlockEntity(ding.getBlockPos(),ding.getBlockState());
            rejected.loadWithComponents(oldCrimson,h.getLevel().registryAccess());
            rejected.setLevel(h.getLevel());
            DingBlockEntity.serverTick(h.getLevel(),rejected.getBlockPos(),rejected.getBlockState(),rejected);
            h.assertTrue(rejected.data(4)==0,
                    "An old Crimson Golden Core target cannot continue after its flame ceiling is reduced");
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void crimsonAndEarthBonusesChangeFinishedWeaponsAndQuality(GameTestHelper h) {
        var recipe = ForgingSystem.recipeById("dao");
        var inputs = materials(recipe);
        var plain = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,"ling_fire"));
        var crimson = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,FlameKind.CRIMSON.id()));
        int max = crimson.getMaxDamage();
        h.assertTrue(max>plain.getMaxDamage() && ForgingSystem.forgedStat(crimson,"flame_durability_percent")==5,
                "Crimson forging adds real maximum durability to the tempered weapon");
        for (int i=0;i<3;i++) ForgingSystem.applyForgedDurability(crimson);
        var loaded = ItemStack.parse(h.getLevel().registryAccess(),crimson.save(h.getLevel().registryAccess())).orElseThrow();
        ForgingSystem.applyForgedDurability(loaded);
        h.assertTrue(loaded.getMaxDamage()==max && ForgingSystem.adjustInjectedQiDamage(loaded,100)
                        > ForgingSystem.adjustInjectedQiDamage(plain,100),
                "Saved crimson bonuses persist and never repeatedly multiply durability");
        var target = EntityType.ZOMBIE.create(h.getLevel());
        h.assertTrue(target!=null,"Create a target for the actual on-hit fire bonus");
        ForgingSystem.applyWeaponHitEffects(loaded,target);
        h.assertTrue(target.isOnFire(),"Crimson's existing fire-storage stat actually ignites its target");
        var earth = ForgingSystem.createMold(recipe,inputs,3200,FlameKind.EARTH.id());
        var control = ForgingSystem.createMold(recipe,inputs,3200,"ling_fire");
        h.assertTrue(data(earth).getInt("quality")==data(control).getInt("quality")+5,
                "Earth flame improves the actual mold quality rather than only its tooltip");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void woodFlameReturnsConsumedIngredientsOnceOnFailure(GameTestHelper h) {
        try (var p = new Forger(h)) {
            var recipe = ForgingSystem.recipeById("fuchen");
            var ding = forge(h);
            h.setBlock(FORGE.below(),FlameKind.WOOD.block());
            var inputs = materials(recipe);
            fill(ding,recipe,inputs);
            ding.startForging(p.player);
            h.assertTrue(ding.data(4)==1,"Start a real wood-flame staff batch");
            h.setBlock(FORGE.below(),Blocks.AIR);
            DingBlockEntity.serverTick(h.getLevel(),ding.getBlockPos(),ding.getBlockState(),ding);
            h.assertTrue(ding.getItem(DingBlockEntity.RESULT_SLOT).is(ModItems.ASHES.get()) && ding.data(4)==0,
                    "An interrupted forge still fails normally");
            int returned = 0;
            for (int i=0;i<inputs.size();i++) {
                var ingredient = inputs.get(i);
                var remaining = ding.getItem(DingBlockEntity.MATERIAL_START+i);
                int expected = ForgingSystem.isRecipeSpiritStone(ingredient) ? 0 : (ingredient.getCount()+3)/4;
                h.assertTrue(remaining.getCount()==expected && (expected==0 || ItemStack.isSameItemSameComponents(ingredient,remaining)),
                        "Wood flame returns only consumed original ingredients, preserving purity and components");
                returned += remaining.getCount();
            }
            h.assertTrue(returned>0,"Material-loss reduction returns real ingredient units");
            DingBlockEntity.serverTick(h.getLevel(),ding.getBlockPos(),ding.getBlockState(),ding);
            int after = 0;
            for (int i=0;i<inputs.size();i++) after += ding.getItem(DingBlockEntity.MATERIAL_START+i).getCount();
            h.assertTrue(after==returned,"Repeated stopped forge ticks cannot duplicate salvaged materials");
            var metal = ForgingSystem.recipeById("heavy_iron_bracer");
            h.assertTrue(!ForgingSystem.woodFlameCompatible(metal)
                            && ForgingSystem.woodFlameSalvageCount(metal,FlameKind.WOOD.id(),new ItemStack(Items.IRON_INGOT,8))==0,
                    "Unrelated metal forging receives no wood-flame material discount");
        }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void solarNetherAndThunderBonusesEnterRealCombatStats(GameTestHelper h) {
        var recipe = ForgingSystem.recipeById("dao");
        var inputs = materials(recipe);
        var plain = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,"ling_fire"));
        var solar = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,FlameKind.SOLAR.id()));
        var nether = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,FlameKind.NETHER.id()));
        var target = EntityType.ZOMBIE.create(h.getLevel());
        h.assertTrue(target!=null,"Create an armored combat target");
        target.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
        h.assertTrue(ForgingSystem.adjustWeaponDamage(solar,target,20)>ForgingSystem.adjustWeaponDamage(plain,target,20)
                        && ForgingSystem.forgedStat(solar,"armor_pen_percent")>ForgingSystem.forgedStat(plain,"armor_pen_percent"),
                "Solar sharpness and armor penetration affect the existing weapon damage pipeline");
        h.assertTrue(ForgingSystem.adjustWeaponDamage(nether,target,20)>ForgingSystem.adjustWeaponDamage(plain,target,20),
                "Nether spirit damage increases actual weapon damage");
        try (var p = new Forger(h)) {
            ModAttachments.setData(p.player,ModAttachments.getData(p.player).withThoughts(20));
            int before = ModAttachments.getData(p.player).thoughts();
            ForgingSystem.applyWeaponHitEffects(nether,p.player);
            h.assertTrue(ModAttachments.getData(p.player).thoughts()==before-2,
                    "Nether soul damage uses the real player thought damage path");
        }
        boolean affinity = false, ordinary = false;
        var random = RandomSource.create(894321L);
        for (int i=0;i<64;i++) {
            var thunder = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,FlameKind.THUNDER.id(),3500,random));
            int lightning = ForgingSystem.forgedStat(thunder,"flame_lightning_damage_percent");
            h.assertTrue(ForgingSystem.forgedStat(thunder,"tribulation_resistance_percent")==15
                            && (lightning==0 || lightning==5),
                    "Every thunder forge gains resistance, with a bounded optional lightning affinity");
            if (lightning>0) {
                affinity = true;
                h.assertTrue(ForgingSystem.adjustWeaponDamage(thunder,target,20)>ForgingSystem.adjustWeaponDamage(plain,target,20),
                        "Granted lightning affinity affects combat damage");
            } else ordinary = true;
        }
        h.assertTrue(affinity && ordinary,"Seeded forging exercises both lightning-affinity outcomes");
        var thunder = ForgingSystem.temper(ForgingSystem.createMold(recipe,inputs,3500,FlameKind.THUNDER.id()));
        h.assertTrue(WeaponTribulation.damageWeapon(thunder) && WeaponTribulation.damageWeapon(plain)
                        && thunder.getDamageValue()<plain.getDamageValue(),
                "Thunder forging really reduces tribulation durability loss while plain weapons retain their old wear");
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void crimsonSeedRecipeUsesBeastMaterialsAndOrdinaryHeat(GameTestHelper h) {
        var recipe = ForgingSystem.recipe(new ItemStack(ModItems.CRIMSON_FLAME_SEED_YUJIAN.get()));
        h.assertTrue(recipe!=null && recipe.id().equals("crimson_flame_seed") && recipe.directOutput()
                        && recipe.required().get(ModItems.CHI_LIN.get())==1
                        && recipe.required().get(ModItems.HUO_XI_BLOOD.get())==1
                        && recipe.spiritStoneCount()==8 && recipe.averageTemp()==2000
                        && ForgingSystem.canUseFire(recipe,FlameKind.MORTAL.id()),
                "The acquisition jade slip uses both fire-beast drops, eight stones and accessible mortal heat");
        var ingredients = materials(recipe);
        h.assertTrue(ForgingSystem.hasRequiredMaterials(recipe,ingredients),"The real seed recipe accepts its full finite ingredient batch");
        ingredients.removeIf(ForgingSystem::isRecipeSpiritStone);
        h.assertTrue(!ForgingSystem.hasRequiredMaterials(recipe,ingredients),"The seed cannot bypass its stone cost");
        var output = ForgingSystem.createMold(recipe,materials(recipe),2000,FlameKind.MORTAL.id());
        h.assertTrue(output.is(ModItems.CRIMSON_FLAME_SEED.get()),"Successful forging creates the actual summonable crimson seed");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void dragonSeedRecipeAndWeaponBonusUseRealBeastMaterialsAndQiBurn(GameTestHelper h) {
        var seedRecipe = ForgingSystem.recipe(new ItemStack(ModItems.DRAGON_FLAME_SEED_YUJIAN.get()));
        h.assertTrue(seedRecipe != null && seedRecipe.id().equals("dragon_flame_seed") && seedRecipe.directOutput()
                        && seedRecipe.required().get(ModItems.YUANYING_YAODAN.get()) == 1
                        && seedRecipe.required().get(ModItems.YANJIAO_HORN.get()) == 1
                        && seedRecipe.averageTemp() == 8500 && seedRecipe.spiritStoneCount() == 64,
                "Dragon seed refinement needs a Nascent Soul beast core, the flame dragon's horn and 64 stones");
        var ingredients = materials(seedRecipe);
        h.assertTrue(ForgingSystem.hasRequiredMaterials(seedRecipe, ingredients)
                        && ForgingSystem.createMold(seedRecipe, ingredients, 8500, FlameKind.THUNDER.id())
                        .is(ModItems.DRAGON_FLAME_SEED.get()),
                "The actual Dragon recipe produces its summonable seed at accessible Nascent Soul flame heat");
        ingredients.removeIf(stack -> stack.is(ModItems.YANJIAO_HORN.get()));
        h.assertTrue(!ForgingSystem.hasRequiredMaterials(seedRecipe, ingredients),
                "An unrelated Nascent Soul core cannot replace the source dragon's horn");

        var recipe = ForgingSystem.recipeById("dao");
        var inputs = materials(recipe);
        var plain = ForgingSystem.temper(ForgingSystem.createMold(recipe, inputs, 3500, "ling_fire"));
        var dragon = ForgingSystem.temper(ForgingSystem.createMold(recipe, inputs, 3500, FlameKind.DRAGON.id()));
        h.assertTrue(ForgingSystem.forgedStat(dragon, "qi_damage_percent") == 20
                        && ForgingSystem.forgedStat(dragon, "fire_storage") == 3
                        && ForgingSystem.forgedStat(dragon, "flame_fire_affinity_percent") == 35
                        && ForgingSystem.forgedStat(dragon, "flame_qi_burn_chance_percent") == 25
                        && ForgingSystem.adjustInjectedQiDamage(dragon, 100)
                        > ForgingSystem.adjustInjectedQiDamage(plain, 100),
                "Dragon's actual forged weapon carries stronger Fire storage, injected Qi and Qi-burning chance");
        var fireTarget = EntityType.ZOMBIE.create(h.getLevel());
        var immuneTarget = EntityType.BLAZE.create(h.getLevel());
        try {
            float unlit = ForgingSystem.adjustWeaponDamage(dragon, fireTarget, 100);
            fireTarget.setRemainingFireTicks(200);
            float burning = ForgingSystem.adjustWeaponDamage(dragon, fireTarget, 100);
            h.assertTrue(fireTarget.isOnFire() && Math.abs(burning - unlit * 1.35F) < .001F,
                    "Dragon's Fire affinity adds exactly 35 percent damage against an already-burning nonimmune target");
            fireTarget.clearFire();
            h.assertTrue(ForgingSystem.adjustWeaponDamage(dragon, fireTarget, 100) == unlit,
                    "An unlit target receives no conditional Dragon Fire-affinity damage");
            float immune = ForgingSystem.adjustWeaponDamage(dragon, immuneTarget, 100);
            immuneTarget.setRemainingFireTicks(200);
            h.assertTrue(immuneTarget.fireImmune()
                            && ForgingSystem.adjustWeaponDamage(dragon, immuneTarget, 100) == immune,
                    "Fire-immune targets receive no Dragon Fire-affinity damage even with fire ticks");
            var restored = ItemStack.parseOptional(h.getLevel().registryAccess(),
                    (CompoundTag) dragon.save(h.getLevel().registryAccess()));
            WeaponRestrictionSystem.debugSetTier(restored, 3);
            var parts = new ArrayList<>(WeaponShardSystem.split(restored, h.getLevel().registryAccess()));
            parts.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
            parts.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
            var reforged = WeaponShardSystem.reforge(parts, h.getLevel().registryAccess());
            h.assertTrue(ForgingSystem.forgedStat(reforged, "flame_fire_affinity_percent") == 35,
                    "Tempering, item serialization and a complete paid shard reforge retain Dragon Fire affinity");
            var control = reforged.copy();
            var controlTag = control.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            controlTag.getCompound(ForgingSystem.TAG_FORGING).remove("flame_fire_affinity_percent");
            control.set(DataComponents.CUSTOM_DATA, CustomData.of(controlTag));
            h.assertTrue(WeaponTribulation.power(reforged) > WeaponTribulation.power(control),
                    "Weapon tribulation evaluates Dragon's real conditional attack potential");
        } finally { fireTarget.discard(); immuneTarget.discard(); }
        var tag = dragon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.getCompound(ForgingSystem.TAG_FORGING).putInt("flame_qi_burn_chance_percent", 100);
        dragon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        try (var target = new Forger(h)) {
            int before = ModAttachments.getData(target.player).qi();
            ForgingSystem.applyAcceptedWeaponHitEffects(dragon, target.player);
            h.assertTrue(target.player.hasEffect(ModEffects.QI_FIRE_BURN)
                            && target.player.getEffect(ModEffects.QI_FIRE_BURN).getAmplifier() == 1,
                    "A deterministic accepted Dragon weapon hit applies the real stronger Qi-burn effect");
            var data = ModAttachments.getData(target.player);
            long maximum = Math.max(1L, (long) data.maxQi());
            int drain = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (maximum + 49) / 50));
            ModEffects.QI_FIRE_BURN.get().applyEffectTick(target.player, 1);
            h.assertTrue(ModAttachments.getData(target.player).qi() == Math.max(0, before - drain),
                    "The real Dragon burn drains two percent of maximum Qi");
            ModEffects.QI_FIRE_BURN.get().applyEffectTick(target.player, 1);
            ModEffects.QI_FIRE_BURN.get().applyEffectTick(target.player, 0);
            h.assertTrue(ModAttachments.getData(target.player).qi() == Math.max(0, before - drain),
                    "Repeated same-tick burn and ordinary amplifier-zero contact cannot duplicate the Qi drain");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void heavenlyQualityExceedsOldCapAndGuardsNativeHighGradeHeat(GameTestHelper h) {
        var recipe = ForgingSystem.recipeById("dao");
        var inputs = materials(recipe);
        var plain = ForgingSystem.temper(ForgingSystem.createMold(recipe, inputs, 3500, "zhen_fire"));
        var heavenly = ForgingSystem.temper(ForgingSystem.createMold(recipe, inputs, 3500, FlameKind.HEAVENLY.id()));
        h.assertTrue(ForgingSystem.qualityCeiling("zhen_fire") == 100
                        && ForgingSystem.qualityCeiling(FlameKind.DRAGON.id()) == 100
                        && ForgingSystem.qualityCeiling(FlameKind.HEAVENLY.id()) == 125
                        && data(plain).getInt("quality") == 100 && data(heavenly).getInt("quality") == 125
                        && ForgingSystem.forgedStat(heavenly, "flame_heavenly_quality_percent") == 25,
                "Heavenly fire raises actual weapon quality to 125 while legacy and Dragon caps stay at 100");
        var target = EntityType.ZOMBIE.create(h.getLevel());
        try {
            h.assertTrue(ForgingSystem.adjustWeaponDamage(heavenly, target, 100)
                            > ForgingSystem.adjustWeaponDamage(plain, target, 100)
                            && ForgingSystem.adjustInjectedQiDamage(heavenly, 100)
                            > ForgingSystem.adjustInjectedQiDamage(plain, 100)
                            && heavenly.getMaxDamage() > plain.getMaxDamage(),
                    "Quality beyond 100 improves real weapon damage, injected Qi and maximum durability");
            var saved = ItemStack.parseOptional(h.getLevel().registryAccess(),
                    (CompoundTag) heavenly.save(h.getLevel().registryAccess()));
            ForgingSystem.applyForgedDurability(saved);
            h.assertTrue(data(saved).getInt("quality") == 125
                            && saved.getMaxDamage() == heavenly.getMaxDamage(),
                    "Saved Heavenly quality persists without multiplying durability again");
            WeaponRestrictionSystem.debugSetTier(saved, 3);
            var shards = new ArrayList<>(WeaponShardSystem.split(saved, h.getLevel().registryAccess()));
            shards.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
            shards.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(), 64));
            var restored = WeaponShardSystem.reforge(shards, h.getLevel().registryAccess());
            ForgingSystem.applyForgedDurability(restored);
            h.assertTrue(!restored.isEmpty() && data(restored).getInt("quality") == 125
                            && ForgingSystem.forgedStat(restored, "flame_heavenly_quality_percent") == 25
                            && restored.getMaxDamage() == saved.getMaxDamage()
                            && ForgingSystem.adjustInjectedQiDamage(restored, 100)
                            == ForgingSystem.adjustInjectedQiDamage(saved, 100),
                    "A complete paid shard reforge preserves Heavenly quality, bonus damage and exact maximum durability");
        } finally { target.discard(); }
        var highGrade = new ForgingSystem.ForgingRecipe("test_native_huashen", ModItems.WEAPON_MOLD.get(), 10500,
                java.util.Map.of(Items.IRON_INGOT, 1), false, 64, "zhen_fire");
        h.assertTrue(ForgingSystem.requiresHeavenlyFire(highGrade, materials(highGrade))
                        && !ForgingSystem.canUseFire(highGrade, "zhen_fire")
                        && !ForgingSystem.canUseFire(highGrade, FlameKind.DRAGON.id())
                        && ForgingSystem.canUseFire(highGrade, FlameKind.HEAVENLY.id()),
                "Native Hua Shen heat requires the new Heavenly flame rather than bypassing the old ceiling");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void dragonQiBurnRequiresDamageAcceptedByTheRealProtectionPipeline(GameTestHelper h) {
        var recipe = ForgingSystem.recipeById("dao");
        var weapon = ForgingSystem.temper(ForgingSystem.createMold(recipe, materials(recipe), 3500, FlameKind.DRAGON.id()));
        var tag = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.getCompound(ForgingSystem.TAG_FORGING).putInt("flame_qi_burn_chance_percent", 100);
        weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var target = EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true);
        target.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 2, 5))));
        h.getLevel().addFreshEntity(target);
        try (var attacker = new Forger(h)) {
            attacker.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
            float before = target.getHealth();
            java.util.function.Consumer<LivingIncomingDamageEvent> protection = event -> {
                if (event.getEntity() == target) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(protection);
            try {
                h.assertTrue(!FlameWeaponEffects.hurtWithWeapon(weapon, target,
                                h.getLevel().damageSources().playerAttack(attacker.player), 2),
                        "The actual protection event cancels the Dragon weapon's damage");
            } finally { NeoForge.EVENT_BUS.unregister(protection); }
            h.assertTrue(target.getHealth() == before && !target.hasEffect(ModEffects.QI_FIRE_BURN),
                    "Canceled damage preserves health and cannot apply a Dragon Qi-burn proc");
            h.assertTrue(FlameWeaponEffects.hurtWithWeapon(weapon, target,
                            h.getLevel().damageSources().playerAttack(attacker.player), 2)
                            && target.getHealth() < before && target.hasEffect(ModEffects.QI_FIRE_BURN)
                            && target.getEffect(ModEffects.QI_FIRE_BURN).getAmplifier() == 1,
                    "A subsequent accepted real damage event applies the deterministic Dragon Qi-burn proc");
        } finally { target.discard(); }
        h.succeed();
    }

    @GameTest(template="fate_test")
    public static void solarAffinityFollowsMetalWeaponBodyRatherThanPreciousFittings(GameTestHelper h) {
        for (String id : List.of("dao","jingtie_spear","tiegu_shan","black_iron_jian",
                "moon_crescent_halberd","soul_hook","seven_star_sword")) {
            var recipe = ForgingSystem.recipeById(id);
            var mold = ForgingSystem.createMold(recipe,materials(recipe),recipe.averageTemp(),FlameKind.SOLAR.id());
            h.assertTrue(ForgingSystem.solarFlameCompatible(recipe)
                            && ForgingSystem.forgedStat(mold,"flame_sharpness_percent")==5,
                    "Solar forging benefits the real metal weapon body: " + id);
        }
        for (String id : List.of("thunderwood_sword","wind_feather_fan","spirit_silk_whip","fuchen","whip","shihunfan","fortune_mirror")) {
            var recipe = ForgingSystem.recipeById(id);
            var solar = ForgingSystem.createMold(recipe,materials(recipe),recipe.averageTemp(),FlameKind.SOLAR.id());
            var control = ForgingSystem.createMold(recipe,materials(recipe),recipe.averageTemp(),"zhen_fire");
            h.assertTrue(!ForgingSystem.solarFlameCompatible(recipe)
                            && ForgingSystem.forgedStat(solar,"flame_sharpness_percent")==0
                            && ForgingSystem.forgedStat(solar,"armor_pen_percent")==ForgingSystem.forgedStat(control,"armor_pen_percent"),
                    "Wood, feathers, silk and non-weapon artifacts gain no incidental Solar damage bonus: " + id);
        }
        var decorative = new ForgingSystem.ForgingRecipe("fuchen",ModItems.WEAPON_MOLD.get(),2200,
                java.util.Map.of(ModBlocks.QIANNIAN_LINGMU_HEART.get().asItem(),4,Items.GOLD_INGOT,1),false,64,"");
        h.assertTrue(!ForgingSystem.solarFlameCompatible(decorative),
                "A gold fitting cannot change the primary material of a wooden staff");
        var nascent = new ForgingSystem.ForgingRecipe("test_nascent_metal",ModItems.WEAPON_MOLD.get(),8500,
                java.util.Map.of(Items.IRON_INGOT,1),false,64,"zhen_fire");
        var spirit = new ForgingSystem.ForgingRecipe("test_huashen_metal",ModItems.WEAPON_MOLD.get(),10000,
                java.util.Map.of(Items.IRON_INGOT,1),false,64,"zhen_fire");
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            h.assertTrue(ForgingSystem.canUseFire(nascent,kind.id())
                            == (kind == FlameKind.THUNDER || kind == FlameKind.DRAGON || kind == FlameKind.HEAVENLY)
                            && ForgingSystem.canUseFire(spirit,kind.id())
                            == (kind == FlameKind.DRAGON || kind == FlameKind.HEAVENLY),
                    "Thunder, Dragon and Heavenly meet Nascent Soul heat; only the new flames reach 10000: " + kind);
        }
        h.succeed();
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(ForgingSystem.TAG_FORGING);
    }

    private static List<ItemStack> materials(ForgingSystem.ForgingRecipe recipe) {
        var result = new ArrayList<ItemStack>();
        recipe.required().forEach((item,count) -> {
            var sample = new ItemStack(item);
            int capacity = ForgingSystem.canStackInForgingRecipe(sample) ? sample.getMaxStackSize() : 1;
            for (int remaining=count;remaining>0;remaining-=capacity) {
                var stack = new ItemStack(item,Math.min(remaining,capacity));
                var tag = new CompoundTag(); tag.putInt(ForgingSystem.TAG_PURITY,100);
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); result.add(stack);
            }
        });
        result.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),recipe.spiritStoneCount()));
        return result;
    }

    private static DingBlockEntity forge(GameTestHelper h) {
        h.setBlock(FORGE,Blocks.AIR); h.setBlock(FORGE.below().below(),Blocks.STONE);
        h.setBlock(FORGE.below(),Blocks.FIRE); h.setBlock(FORGE,ModBlocks.QINGTONG_YINHUO_DING.get());
        return (DingBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(FORGE));
    }

    private static void fill(DingBlockEntity ding,ForgingSystem.ForgingRecipe recipe,List<ItemStack> inputs) {
        var slip = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",recipe.id()+"_yujian"));
        ding.setItem(DingBlockEntity.YUJIAN_SLOT,new ItemStack(slip));
        for (int i=0;i<inputs.size();i++) ding.setItem(DingBlockEntity.MATERIAL_START+i,inputs.get(i).copy());
    }

    private static final class Forger implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        Forger(GameTestHelper h) {
            player = new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"flame-forger"),ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND); channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server,connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
                @Override public void send(Packet<?> packet) { }
            };
            var pos = h.absolutePos(FORGE); player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            ModAttachments.setData(player,ModAttachments.getData(player).withQi(20000));
        }
        @Override public void close() { player.discard(); channel.close(); }
    }
}
