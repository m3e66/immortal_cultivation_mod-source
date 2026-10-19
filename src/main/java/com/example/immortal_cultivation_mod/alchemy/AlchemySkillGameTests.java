package com.example.immortal_cultivation_mod.alchemy;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.FateAlchemyFormulaItem;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.IdentityItems;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_alchemy_skill")
@PrefixGameTestTemplate(false)
public final class AlchemySkillGameTests {
    private static final BlockPos FORGE = new BlockPos(3, 2, 3);
    private static final String ALCHEMY = FateAlchemyFormulaItem.PROFICIENCY;
    private static final String FORGING = IdentityItems.FORGING_PROFICIENCY;

    @GameTest(template = "fate_test")
    public static void menuSelectsAlchemyGradeAndCountForModernAndLegacyFormulas(GameTestHelper h) {
        try (var user = new User(h)) {
            var stats = ModAttachments.getData(user.player)
                    .withAddedMethodProficiency(FORGING, 1000)
                    .withAddedMethodProficiency(ALCHEMY, 37);
            var ding = forge(h, new ItemStack(AlchemyContent.FORMULAS.get("qingling").get()));
            var menu = new DingMenu(1, user.player.getInventory(), ding);
            for (var slip : List.of(new ItemStack(AlchemyContent.FORMULAS.get("qingling").get()),
                    new ItemStack(AlchemyContent.LEGACY_FORMULAS.get("bigu").get()))) {
                ding.setItem(DingBlockEntity.YUJIAN_SLOT, slip);
                h.assertTrue(menu.craftProficiencyKey().equals(ALCHEMY), "Both pill families use alchemy skill");
                var text = (TranslatableContents) menu.craftProficiencyLabel(stats).getContents();
                h.assertTrue(text.getKey().endsWith(".alchemy_proficiency")
                                && text.getArgs()[1].equals(37),
                        "Alchemy label displays alchemy count, independent of forging's 1000 points");
                var grade = (net.minecraft.network.chat.Component) text.getArgs()[0];
                h.assertTrue(((TranslatableContents) grade.getContents()).getKey().endsWith(".spell_proficiency_beginner"),
                        "Alchemy label selects beginner grade instead of forging's perfected grade");
            }
            ding.setItem(DingBlockEntity.YUJIAN_SLOT, new ItemStack(AlchemyContent.BOTTLE_SLIP.get()));
            var bottleLabel = (TranslatableContents) menu.craftProficiencyLabel(stats).getContents();
            h.assertTrue(menu.craftProficiencyKey().equals(FORGING)
                            && bottleLabel.getKey().endsWith(".forging_proficiency")
                            && bottleLabel.getArgs()[1].equals(1000),
                    "Crafting the empty bottle remains vessel forging rather than pill alchemy");
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void realPillBatchesIncreaseOnlyAlchemyAndInterruptedBatchesAwardNothing(GameTestHelper h) {
        try (var user = new User(h)) {
            ModAttachments.setData(user.player, ModAttachments.getData(user.player)
                    .withAddedMethodProficiency(FORGING, 250).withAddedMethodProficiency(ALCHEMY, 5));
            var modern = complete(h, user, new ItemStack(AlchemyContent.FORMULAS.get("qingling").get()));
            h.assertTrue(modern.is(AlchemyContent.pill("qingling")), "Modern formula produces its genuine pill batch");
            assertSkills(h, user.player, 6, 250, "Modern batch awards only alchemy");
            var legacy = complete(h, user, new ItemStack(AlchemyContent.LEGACY_FORMULAS.get("bigu").get()));
            h.assertTrue(legacy.getItem() instanceof LegacyPillItem, "Legacy formula produces genuine legacy pills");
            assertSkills(h, user.player, 7, 250, "Legacy batch awards only alchemy");
            var bottle = complete(h, user, new ItemStack(AlchemyContent.BOTTLE_SLIP.get()));
            h.assertTrue(bottle.is(AlchemyContent.BOTTLE.get()), "Bottle recipe still forges its vessel");
            assertSkills(h, user.player, 7, 251, "Bottle batch awards only forging");

            var interrupted = forge(h, new ItemStack(AlchemyContent.FORMULAS.get("qingling").get()));
            interrupted.startForging(user.player);
            h.assertTrue(interrupted.isForging(), "A real pill batch starts before interruption");
            h.setBlock(FORGE.below(), Blocks.AIR);
            tick(h, interrupted, 1);
            h.assertTrue(!interrupted.isForging(), "Loss of fire interrupts the batch");
            assertSkills(h, user.player, 7, 251, "Interrupted batch grants neither craft skill");
        }
        h.succeed();
    }

    private static ItemStack complete(GameTestHelper h, User user, ItemStack slip) {
        var ding = forge(h, slip);
        ding.startForging(user.player);
        h.assertTrue(ding.isForging(), "Valid paid batch starts through real DingBlockEntity");
        tick(h, ding, ding.data(3));
        h.assertTrue(!ding.isForging(), "Batch finishes through real forge ticks");
        return ding.getItem(DingBlockEntity.RESULT_SLOT).copy();
    }

    private static void assertSkills(GameTestHelper h, ServerPlayer player, int alchemy, int forging, String message) {
        var stats = ModAttachments.getData(player);
        h.assertTrue(stats.methodProficiency(ALCHEMY) == alchemy && stats.methodProficiency(FORGING) == forging,
                message + ": alchemy=" + stats.methodProficiency(ALCHEMY) + ", forging=" + stats.methodProficiency(FORGING));
    }

    private static void tick(GameTestHelper h, DingBlockEntity ding, int count) {
        for (int i = 0; i < count; i++) {
            DingBlockEntity.serverTick(h.getLevel(), ding.getBlockPos(), ding.getBlockState(), ding);
        }
    }

    private static DingBlockEntity forge(GameTestHelper h, ItemStack slip) {
        h.setBlock(FORGE.below().below(), Blocks.STONE);
        h.setBlock(FORGE.below(), Blocks.CAMPFIRE);
        // Replace the previous fixture rather than carrying inventory from its completed batch.
        h.setBlock(FORGE, Blocks.AIR);
        h.setBlock(FORGE, ModBlocks.QINGTONG_YINHUO_DING.get());
        var ding = (DingBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(FORGE));
        ding.setItem(DingBlockEntity.YUJIAN_SLOT, slip);
        var recipe = ForgingSystem.recipe(slip);
        var inputs = new ArrayList<ItemStack>();
        recipe.required().forEach((item, count) -> {
            if (ForgingSystem.canStackInForgingRecipe(new ItemStack(item))) inputs.add(new ItemStack(item, count));
            else for (int i = 0; i < count; i++) inputs.add(new ItemStack(item));
        });
        inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),
                recipe.spiritStoneCount()));
        h.assertTrue(inputs.size() <= ding.materialSlotCount(), "Fixture ingredients fit genuine ding capacity");
        for (int i = 0; i < inputs.size(); i++) {
            var tags = new CompoundTag();
            tags.putInt(ForgingSystem.TAG_PURITY, 100);
            inputs.get(i).set(DataComponents.CUSTOM_DATA, CustomData.of(tags));
            ding.setItem(DingBlockEntity.MATERIAL_START + i, inputs.get(i));
        }
        return ding;
    }

    private static final class User implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final List<ServerPlayer> onlinePlayers;
        final Map<UUID, ServerPlayer> playersByUuid;

        User(GameTestHelper h) {
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "alchemy-skill"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { }
            };
            var position = h.absolutePos(FORGE);
            player.setPos(position.getX() + .5, position.getY() + .5, position.getZ() + .5);
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(20000));
            onlinePlayers = field(player.server.getPlayerList(), "players");
            playersByUuid = field(player.server.getPlayerList(), "playersByUUID");
            onlinePlayers.add(player);
            playersByUuid.put(player.getUUID(), player);
            h.getLevel().addNewPlayer(player);
        }

        @SuppressWarnings("unchecked")
        private static <T> T field(PlayerList list, String name) {
            try {
                var field = PlayerList.class.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(list);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Cannot register the alchemy GameTest player", exception);
            }
        }

        @Override public void close() {
            if (player.containerMenu != player.inventoryMenu) player.closeContainer();
            onlinePlayers.remove(player);
            playersByUuid.remove(player.getUUID(), player);
            player.discard();
            channel.close();
        }
    }
}
