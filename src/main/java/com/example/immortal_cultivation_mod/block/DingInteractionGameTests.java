package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_ding_models")
@PrefixGameTestTemplate(false)
public final class DingInteractionGameTests {
    private static final BlockPos FORGE = new BlockPos(3, 2, 3);

    @GameTest(template = "fate_test")
    public static void liddedDingBodyClicksToggleLidAtNativeAndEnlargedSizes(GameTestHelper h) {
        // Large dings can reach outside the template; keep this fixture above surrounding test terrain.
        var anchor = h.absolutePos(FORGE).atY(h.getLevel().getMaxBuildHeight() - 16);
        try (var user = new User(h)) {
            // A held item must not consume right-click before the forge interaction.
            user.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.QINGTONG_YINHUO_DING.get(),
                    ModBlocks.CHI_JIN_SANYANG_DING.get()}) {
                for (var facing : Direction.Plane.HORIZONTAL) {
                    for (int scale : new int[]{1, 2, 5}) {
                        var state = block.defaultBlockState()
                                .setValue(DingBlock.FACING, facing).setValue(DingBlock.SCALE, scale);
                        h.getLevel().setBlockAndUpdate(anchor, state);
                        var ding = (DingBlockEntity) h.getLevel().getBlockEntity(anchor);
                        var hit = bodyHit(h, user.player, anchor, state);
                        h.assertTrue(hit != null && hit.getBlockPos().equals(anchor),
                                "Client ray finds the overhanging body at " + facing + " / " + scale);
                        user.player.setShiftKeyDown(false);
                        ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        h.assertTrue(user.player.containerMenu == user.player.inventoryMenu && !ding.isLidOpen(),
                                "Ordinary use cannot open a closed lid, even with an item held");
                        user.player.setShiftKeyDown(true);
                        boolean accepted = ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        var eye = user.player.getEyePosition();
                        var end = hit.getLocation().add(hit.getLocation().subtract(eye).normalize().scale(.08));
                        var normalHit = h.getLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE, user.player));
                        h.assertTrue(accepted,
                                "Server accepts the same nearby body hit at " + facing + " / " + scale
                                        + "; distance=" + eye.distanceTo(hit.getLocation())
                                        + "; vanilla=" + normalHit.getType() + " " + normalHit.getBlockPos()
                                        + "; anchor=" + anchor + "; surface=" + hit.getLocation());
                        h.assertTrue(ding.isLidOpen() && user.player.containerMenu == user.player.inventoryMenu,
                                "Sneak right-click only opens the physical lid, without opening a menu");
                        user.player.setShiftKeyDown(false);
                        ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        h.assertTrue(user.player.containerMenu instanceof DingMenu menu && menu.usesForge(ding)
                                        && menu.materialSlotCount() == ding.materialSlotCount() && ding.isLidOpen(),
                                "Ordinary right-click opens the correct forge only after the lid is open");
                        user.player.closeContainer();
                        h.assertTrue(ding.isLidOpen(), "Exiting the menu leaves the lid open");
                        user.player.setShiftKeyDown(false);
                        ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        h.assertTrue(user.player.containerMenu instanceof DingMenu, "Ordinary use reopens an already open ding");
                        user.player.setShiftKeyDown(true);
                        ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        h.assertTrue(!ding.isLidOpen() && user.player.containerMenu == user.player.inventoryMenu,
                                "Sneak-use closes both the physical lid and its menu");
                        user.player.setShiftKeyDown(false);
                        ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation());
                        h.assertTrue(user.player.containerMenu == user.player.inventoryMenu && !ding.isLidOpen(),
                                "Ordinary right-click cannot reopen the menu after closing the lid");
                    }
                }
            }
        } finally { h.getLevel().removeBlock(anchor, false); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void nativeSanyangBodyClicksStillRespectWallsReachAndActualGeometry(GameTestHelper h) {
        h.setBlock(FORGE, ModBlocks.CHI_JIN_SANYANG_DING.get());
        var anchor = h.absolutePos(FORGE);
        var state = h.getLevel().getBlockState(anchor);
        try (var user = new User(h)) {
            user.player.setShiftKeyDown(true);
            var hit = bodyHit(h, user.player, anchor, state);
            h.assertTrue(hit != null, "Native-size overhanging body is detectable");
            var start = user.player.getEyePosition();
            var wall = anchor.offset(0, 1, -2);
            h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            h.assertTrue(!ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation())
                            && user.player.containerMenu == user.player.inventoryMenu,
                    "A wall blocks an otherwise valid body hit");
            h.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            h.assertTrue(!ScaledDingInteractionSupport.interact(user.player, anchor, start.add(1, 0, .2)),
                    "A nearby claimed point outside the actual hull cannot open the menu");
            h.assertTrue(!ScaledDingInteractionSupport.interact(user.player, anchor, new Vec3(Double.NaN, 0, 0)),
                    "Invalid coordinates cannot open the menu");
            placeEye(user.player, start.add(0, 0, -10));
            h.assertTrue(!ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation()),
                    "A valid surface outside interaction reach cannot open the menu");
            placeEye(user.player, start);
            h.assertTrue(ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation())
                            && ((DingBlockEntity) h.getLevel().getBlockEntity(anchor)).isLidOpen()
                            && user.player.containerMenu == user.player.inventoryMenu,
                    "The same sneak body click opens only the lid after the wall is removed");
            user.player.setShiftKeyDown(false);
            h.assertTrue(ScaledDingInteractionSupport.interact(user.player, anchor, hit.getLocation())
                            && user.player.containerMenu instanceof DingMenu,
                    "Ordinary right-click can now open the menu through the uncovered body");
        }
        h.succeed();
    }

    private static BlockHitResult bodyHit(GameTestHelper h, ServerPlayer player, BlockPos anchor, BlockState state) {
        int scale = state.getValue(DingBlock.SCALE);
        var bounds = state.getShape(h.getLevel(), anchor).bounds();
        var start = new Vec3(anchor.getX() + .5, anchor.getY() + 1.55 * scale,
                anchor.getZ() + bounds.minZ - 1.5);
        placeEye(player, start);
        var aim = new Vec3(anchor.getX() + .5, start.y, anchor.getZ() + .5);
        var end = start.add(aim.subtract(start).normalize().scale(player.blockInteractionRange()));
        if (scale == 1) {
            var vanilla = h.getLevel().clip(new ClipContext(start, aim, ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE, player));
            h.assertTrue(vanilla.getType() == HitResult.Type.MISS,
                    "The native body lies above the anchor cell and reproduces the original vanilla ray miss");
        }
        return ScaledDingInteractionSupport.findNearestScaledDing(h.getLevel(), start, end,
                CollisionContext.of(player), start.distanceToSqr(end));
    }

    private static void placeEye(ServerPlayer player, Vec3 eye) {
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
    }

    private static final class User implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;

        User(GameTestHelper h) {
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "sanyang-clicks"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { }
            };
        }

        @Override public void close() {
            player.closeContainer();
            player.discard();
            channel.close();
        }
    }
}
