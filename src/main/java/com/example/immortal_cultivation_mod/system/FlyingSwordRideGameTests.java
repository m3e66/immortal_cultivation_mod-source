package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_sword_flight")
@PrefixGameTestTemplate(false)
public final class FlyingSwordRideGameTests {
    private static final String SPEED_TAG = "ImmortalCultivationJindanCreativeFlightSpeed";

    private static List<FlyingSwordEntity> release(GameTestHelper h, LifeSimulationGameTests.Player fixture, String realm, int count) {
        var p = fixture.player;
        var pos = h.absolutePos(new BlockPos(4, 15, 4));
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        p.setYRot(0); p.setXRot(0); p.setOnGround(false);
        ModAttachments.setData(p, ModAttachments.getData(p)
                .withCultivationLevel(realm + CultivationLevels.STAGE_EARLY).withQi(1_000_000));
        var stacks = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new ItemStack(ModItems.FLYING_SWORD.get())).toList();
        h.assertTrue(FlyingSwordEntity.releaseControlledFromCase(p, stacks) == count, "Real owned blades released");
        return h.getLevel().getEntitiesOfClass(FlyingSwordEntity.class, p.getBoundingBox().inflate(8),
                sword -> p.getUUID().equals(sword.getOwnerUuid()));
    }

    private static void near(GameTestHelper h, float actual, float expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < .00001F, message + ": " + actual + " expected " + expected);
    }

    @GameTest(template="fate_test")
    public static void goldenCoreSwordUsesPlayerFlightAndRestoresUnboostedSpeed(GameTestHelper h) {
        try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p = fixture.player;
            var swords = release(h, fixture, CultivationLevels.REALM_JINDAN, 2);
            h.assertTrue(FlyingSwordEntity.toggleRiding(p), "Dedicated key enables sword flight");
            var blade = FlyingSwordEntity.flightSword(p);
            h.assertTrue(blade != null && !p.isPassenger() && !blade.isVehicle(), "Flight uses player abilities, never a mount");
            h.assertTrue(p.getAbilities().mayfly && !p.getAbilities().flying, "Double-tap Jump controls takeoff");
            near(h, p.getAbilities().getFlyingSpeed(), .15F, "Default x2 selected speed receives x1.5 bonus");
            p.getAbilities().flying = true;
            p.setPos(p.position().add(2, 3, 1)); p.setYRot(45); p.setXRot(70);
            var before = p.position();
            for (int i=0; i<5; i++) { blade.tick(); JindanCreativeFlight.tick(p); }
            h.assertTrue(p.position().equals(before) && Math.abs(blade.getY() - (p.getY() - FlyingSwordEntity.FLIGHT_BLADE_OFFSET)) < 1E-6,
                    "Blade follows feet and cannot move player");
            near(h, blade.getXRot(), 0, "Blade remains level despite looking down");
            near(h, p.getAbilities().getFlyingSpeed(), .15F, "Ticks never compound the bonus");
            p.setShiftKeyDown(true); blade.tick();
            h.assertTrue(blade.isSwordFlight() && p.getAbilities().flying && !p.isPassenger(), "Sneak descends without leaving sword");
            p.setShiftKeyDown(false);
            h.assertTrue(FlyingSwordEntity.toggleRiding(p) && FlyingSwordEntity.flightSword(p) == null, "Same key leaves sword mode");
            near(h, p.getAbilities().getFlyingSpeed(), .1F, "Ordinary selected speed restored");
            h.assertTrue(p.getAbilities().flying && p.getAbilities().mayfly, "Ordinary Golden Core flight continues");
            JindanCreativeFlight.setEnabled(p, false);
            h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying, "Disabling flight revokes owned permissions");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void realmCapsAndSwordBonusApplyInCorrectOrder(GameTestHelper h) {
        String[] realms = {CultivationLevels.REALM_JINDAN, CultivationLevels.REALM_YUANYING, CultivationLevels.REALM_HUASHEN};
        float[] caps = {.25F, .5F, 1F};
        for (int i=0; i<realms.length; i++) try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p = fixture.player; var swords = release(h, fixture, realms[i], 1);
            JindanCreativeFlight.setEnabled(p, true);
            p.getPersistentData().putFloat(SPEED_TAG, 1F);
            JindanCreativeFlight.tick(p);
            near(h, p.getAbilities().getFlyingSpeed(), caps[i], "Saved high speed obeys realm cap");
            h.assertTrue(FlyingSwordEntity.toggleRiding(p), "Sword mode available at each flight realm");
            near(h, p.getAbilities().getFlyingSpeed(), caps[i] * 1.5F, "Sword exceeds cap by exactly 50 percent");
            p.getAbilities().flying = true;
            JindanCreativeFlight.adjustSpeed(p, -1);
            near(h, p.getAbilities().getFlyingSpeed(), (caps[i]-.025F)*1.5F, "Scroll changes selected base, then multiplies");
            JindanCreativeFlight.adjustSpeed(p, 8);
            near(h, p.getAbilities().getFlyingSpeed(), caps[i]*1.5F, "Increasing speed cannot stack bonus or raise selected cap");
            near(h, p.getPersistentData().getFloat(SPEED_TAG), caps[i], "Saved selection contains base speed, not boosted speed");
            FlyingSwordEntity.toggleRiding(p);
            near(h, p.getAbilities().getFlyingSpeed(), caps[i], "Removing sword restores capped base");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void foundationAndHalfCoreCannotUseSwordFlight(GameTestHelper h) {
        try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p = fixture.player; var swords = release(h, fixture, CultivationLevels.REALM_ZHUJI, 1);
            h.assertTrue(!FlyingSwordEntity.toggleRiding(p) && !p.getAbilities().mayfly, "Foundation controls swords but cannot fly");
            ModAttachments.setData(p, ModAttachments.getData(p).withCultivationLevel(CultivationLevels.REALM_HALF_JINDAN));
            h.assertTrue(!FlyingSwordEntity.toggleRiding(p) && !p.getAbilities().mayfly, "Half-step Core is below flight requirement");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void missingOrForeignSwordCannotGrantBonus(GameTestHelper h) {
        try (var owner = new LifeSimulationGameTests.Player(h); var stranger = new LifeSimulationGameTests.Player(h)) {
            var swords = release(h, owner, CultivationLevels.REALM_JINDAN, 1);
            stranger.player.setPos(owner.player.position());
            ModAttachments.setData(stranger.player, ModAttachments.getData(owner.player));
            h.assertTrue(!FlyingSwordEntity.toggleRiding(stranger.player) && !stranger.player.getAbilities().mayfly,
                    "Nearby foreign sword cannot grant flight");
            swords.forEach(FlyingSwordEntity::discard);
            h.assertTrue(!FlyingSwordEntity.toggleRiding(owner.player) && !owner.player.getAbilities().mayfly,
                    "No real sword means no sword flight");
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void terrainCollisionBelongsToPlayerAtAnySwordSpeed(GameTestHelper h) {
        try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p = fixture.player; var swords = release(h, fixture, CultivationLevels.REALM_HUASHEN, 1);
            var base = p.blockPosition();
            for (int x=-3;x<=3;x++) for(int z=-3;z<=5;z++) {
                h.getLevel().setBlockAndUpdate(base.offset(x,-1,z), Blocks.STONE.defaultBlockState());
                for(int y=0;y<=4;y++) h.getLevel().setBlockAndUpdate(base.offset(x,y,z),
                        z==4 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            h.assertTrue(FlyingSwordEntity.toggleRiding(p), "Sword selected without moving player");
            var blade = FlyingSwordEntity.flightSword(p); p.getAbilities().flying = true;
            p.move(MoverType.SELF, new Vec3(0, -8, 0)); blade.tick();
            h.assertTrue(p.getY() >= base.getY() && h.getLevel().noCollision(p), "Vanilla player movement stops on floor");
            p.move(MoverType.SELF, new Vec3(0,0,12)); blade.tick();
            h.assertTrue(p.getBoundingBox().maxZ <= base.getZ()+4+1E-6 && h.getLevel().noCollision(p), "Player stops at wall at high speed");
            h.assertTrue(!p.isPassenger(), "Collision does not require a vehicle or custom movement");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void unavailableFlightOrDestroyedBladeCannotLeaveBonus(GameTestHelper h) {
        try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player; var swords=release(h,fixture,CultivationLevels.REALM_JINDAN,1);
            h.assertTrue(FlyingSwordEntity.toggleRiding(p), "Flight enabled");
            var blade=FlyingSwordEntity.flightSword(p);
            blade.hurt(h.getLevel().damageSources().generic(), 1_000_000);
            JindanCreativeFlight.tick(p);
            h.assertTrue(FlyingSwordEntity.flightSword(p)==null, "Destroyed blade stops sword mode");
            near(h,p.getAbilities().getFlyingSpeed(),.1F,"Destroyed blade removes speed bonus");
            swords.forEach(FlyingSwordEntity::discard);
            swords=release(h,fixture,CultivationLevels.REALM_JINDAN,1);
            h.assertTrue(FlyingSwordEntity.toggleRiding(p),"Replacement blade selected");
            ModAttachments.setData(p,ModAttachments.getData(p).withQi(0));
            JindanCreativeFlight.tick(p);
            h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying && FlyingSwordEntity.flightSword(p)==null,
                    "Qi depletion clears sword mode and flight grant");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }
    @GameTest(template="fate_test")
    public static void legacyPassengerSaveCannotRetainVehicleFlight(GameTestHelper h) {
        try (var fixture = new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player; var swords=release(h,fixture,CultivationLevels.REALM_ZHUJI,1);
            var blade=swords.getFirst();
            // Force reproduces a passenger restored from an old save, bypassing normal boarding rules.
            h.assertTrue(p.startRiding(blade,true),"Legacy save fixture is mounted");
            p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.getAbilities().setFlyingSpeed(.22F);
            blade.tick();
            h.assertTrue(!p.isPassenger()&&!p.getAbilities().mayfly&&!p.getAbilities().flying,
                    "Legacy vehicle is detached and cannot leak Foundation flight permission");
            swords.forEach(FlyingSwordEntity::discard);
        } h.succeed();
    }

    @GameTest(template="fate_test")
    public static void swordFlightWakeFollowsPlayerAbilities(GameTestHelper h) {
        com.example.immortal_cultivation_mod.physics.CultivationFlightTrailGameTests
                .swordFlightUsesPlayerGroundStateAndRetainsOrdinaryFlight(h);
    }

    @GameTest(template="fate_test")
    public static void allModAndVanillaSwordsReleaseControlFlyAndRecallExactStack(GameTestHelper h) {
        var items=java.util.List.of(ModItems.TAOMU_SWORD.get(),ModItems.COPPER_COIN_SWORD.get(),ModItems.FLYING_SWORD.get(),
                ModItems.BLACK_IRON_JIAN.get(),ModItems.BLACK_IRON_GREATSWORD.get(),ModItems.THUNDERWOOD_SWORD.get(),
                ModItems.SEVEN_STAR_SWORD.get(),ModItems.MANIFESTED_TALISMAN_SWORD.get(),
                net.minecraft.world.item.Items.WOODEN_SWORD,net.minecraft.world.item.Items.STONE_SWORD,
                net.minecraft.world.item.Items.IRON_SWORD,net.minecraft.world.item.Items.GOLDEN_SWORD,
                net.minecraft.world.item.Items.DIAMOND_SWORD,net.minecraft.world.item.Items.NETHERITE_SWORD);
        try(var fixture=new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player;release(h,fixture,CultivationLevels.REALM_JINDAN,0);
            for(var item:items) {
                var stack=new ItemStack(item);stack.setDamageValue(2);
                if(com.example.immortal_cultivation_mod.item.ManifestedWeaponItem.isManifested(stack))
                    com.example.immortal_cultivation_mod.item.ManifestedWeaponItem.bindToOwner(stack,p);
                var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                tag.putString("SwordFlightTestMarker","retain-forged-components");
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
                var expected=stack.copy();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                h.assertTrue(FlyingSwordEntity.releaseFromHand(p)&&p.getMainHandItem().isEmpty(),"Hand releases sword: "+item);
                h.assertTrue(FlyingSwordEntity.controlNearby(p)==1,"Yuqi Shu controls sword: "+item);
                h.assertTrue(FlyingSwordEntity.toggleRiding(p)&&!p.isPassenger(),"Creative-style sword flight available: "+item);
                var blade=FlyingSwordEntity.flightSword(p);blade.tick();
                h.assertTrue(ItemStack.matches(expected,blade.pouchRecallStack()),"Exact sword data retained in flight: "+item);
                FlyingSwordEntity.toggleRiding(p);
                var recalled=FlyingSwordEntity.collectControlledForCase(p);
                h.assertTrue(recalled.size()==1&&ItemStack.matches(expected,recalled.getFirst()),"Recall returns exact sword without duplication: "+item);
            }
        }h.succeed();
    }

    @GameTest(template="fate_test")
    public static void nonSwordWeaponsCannotReleaseControlOrEnableFlight(GameTestHelper h) {
        var rejected=java.util.List.of(ModItems.DAO.get(),ModItems.FLAME_PATTERN_DAO.get(),ModItems.JINGTIE_SPEAR.get(),
                ModItems.FUCHEN.get(),ModItems.HEAVY_IRON_BRACER.get(),ModItems.TIEGU_SHAN.get(),ModItems.WIND_FEATHER_FAN.get(),
                ModItems.MOON_CRESCENT_HALBERD.get(),ModItems.SPIRIT_SILK_WHIP.get(),ModItems.SOUL_HOOK.get(),
                ModItems.EARTHBREAKER_HAMMER.get(),ModItems.THROWING_KNIFE.get(),net.minecraft.world.item.Items.IRON_AXE,
                net.minecraft.world.item.Items.TRIDENT,net.minecraft.world.item.Items.STICK);
        try(var fixture=new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player;release(h,fixture,CultivationLevels.REALM_JINDAN,0);
            for(var item:rejected) {
                var stack=new ItemStack(item);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                h.assertTrue(!FlyingSwordEntity.isFlyingWeapon(stack),"Non-sword rejected despite melee superclass: "+item);
                h.assertTrue(!FlyingSwordEntity.releaseFromHand(p)&&p.getMainHandItem().is(item),"Release cannot consume non-sword: "+item);
                h.assertTrue(FlyingSwordEntity.releaseControlledFromCase(p,List.of(stack))==0,"Case cannot release non-sword: "+item);
                var legacy=new FlyingSwordEntity(h.getLevel(),p,p.position());legacy.setStoredSwordStack(stack);
                h.assertTrue(!legacy.canBeControlledBy(p),"Old non-sword entity cannot be controlled: "+item);
                h.assertTrue(!FlyingSwordEntity.toggleRiding(p),"Non-sword cannot enable flight: "+item);
                legacy.discard();
            }
        }h.succeed();
    }

    @GameTest(template="fate_test")
    public static void oldFlyingDaoReturnsExactItemInsteadOfFlying(GameTestHelper h) {
        try(var fixture=new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player;release(h,fixture,CultivationLevels.REALM_ZHUJI,0);
            var dao=new ItemStack(ModItems.DAO.get());dao.setDamageValue(9);
            var tag=new net.minecraft.nbt.CompoundTag();tag.putString("LegacyMarker","preserve-original");
            dao.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            var legacy=new FlyingSwordEntity(h.getLevel(),p,p.position());legacy.setStoredSwordStack(dao);
            h.getLevel().addFreshEntity(legacy);p.startRiding(legacy,true);
            p.getAbilities().mayfly=true;p.getAbilities().flying=true;legacy.tick();
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(4),
                    item->ItemStack.matches(item.getItem(),dao));
            h.assertTrue(legacy.isRemoved()&&drops.size()==1,"Old Dao entity becomes exactly one original item");
            h.assertTrue(!p.isPassenger()&&!p.getAbilities().mayfly,"Legacy non-sword cannot retain vehicle flight");
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        }h.succeed();
    }

    @GameTest(template="fate_test")
    public static void mixedLegacySwordCaseNeverDeletesOrReleasesDao(GameTestHelper h) {
        try(var fixture=new LifeSimulationGameTests.Player(h)) {
            var p=fixture.player;release(h,fixture,CultivationLevels.REALM_JINDAN,0);
            var swordCase=new ItemStack(ModItems.SWORD_CASE.get());
            var dao=new ItemStack(ModItems.DAO.get());dao.setDamageValue(7);
            var sword=new ItemStack(ModItems.BLACK_IRON_JIAN.get());sword.setDamageValue(4);
            var root=new net.minecraft.nbt.CompoundTag();var contents=new net.minecraft.nbt.ListTag();
            contents.add(dao.saveOptional(p.registryAccess()));contents.add(sword.saveOptional(p.registryAccess()));
            root.put("immortal_cultivation_mod.sword_case_weapons",contents);
            swordCase.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
            var release=com.example.immortal_cultivation_mod.item.SwordCaseItem.class.getDeclaredMethod(
                    "releaseOrRecallAll",net.minecraft.server.level.ServerPlayer.class,ItemStack.class);
            release.setAccessible(true);
            h.assertTrue((boolean)release.invoke(null,p,swordCase),"Mixed legacy case releases eligible sword");
            var after=swordCase.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag()
                    .getList("immortal_cultivation_mod.sword_case_weapons",net.minecraft.nbt.Tag.TAG_COMPOUND);
            h.assertTrue(after.size()==1&&ItemStack.matches(dao,ItemStack.parseOptional(p.registryAccess(),after.getCompound(0))),
                    "Skipped legacy Dao remains intact, not replaced or removed by released count");
            var cycle=com.example.immortal_cultivation_mod.item.SwordCaseItem.class.getDeclaredMethod(
                    "cycleWeapon",net.minecraft.server.level.ServerPlayer.class,ItemStack.class);
            cycle.setAccessible(true);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue((boolean)cycle.invoke(null,p,swordCase)&&ItemStack.matches(dao,p.getMainHandItem()),
                    "Owner can retrieve exact legacy Dao from case with empty hand");
            var recalled=FlyingSwordEntity.collectControlledForCase(p);
            h.assertTrue(recalled.size()==1&&ItemStack.matches(sword,recalled.getFirst()),"Only released sword returns on recall");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        h.succeed();
    }

}
