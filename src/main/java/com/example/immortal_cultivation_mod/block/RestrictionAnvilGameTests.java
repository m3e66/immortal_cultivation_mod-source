package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.screen.RestrictionTableMenu;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_restriction_anvil")
@PrefixGameTestTemplate(false)
public final class RestrictionAnvilGameTests {
    @GameTest(template = "fate_test")
    public static void orbitFacesCenterAndStaysSmoothWithoutChangingArt(GameTestHelper h) {
        var artwork = new HashSet<Integer>();
        for (int table = 0; table < 16; table++) {
            long seed = new BlockPos(table, 32, table * 3).asLong();
            for (int slot = 0; slot < RestrictionTableSigils.COUNT; slot++) {
                var start = RestrictionTableSigils.sample(seed, slot, 0);
                artwork.add(start.textureIndex());
                for (int tick = 0; tick <= 400; tick += 7) {
                    var pose = RestrictionTableSigils.sample(seed, slot, tick);
                    var next = RestrictionTableSigils.sample(seed, slot, tick + .125);
                    Vec3 towardCenter = new Vec3(.5, .86, .5).subtract(pose.center()).normalize();
                    h.assertTrue(pose.inwardNormal().dot(towardCenter) > .99999
                                    && pose.tangent().cross(pose.up()).dot(pose.inwardNormal()) > .99999,
                            "Sigil plane faces anvil center, not the camera");
                    h.assertTrue(pose.center().distanceTo(next.center()) < .003
                                    && pose.textureIndex() == start.textureIndex(),
                            "Sub-tick orbit interpolation stays smooth and artwork never flickers");
                    for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) {
                        Vec3 corner = pose.center().add(pose.tangent().scale(x * RestrictionTableSigils.HALF_SIZE))
                                .add(pose.up().scale(y * RestrictionTableSigils.HALF_SIZE));
                        h.assertTrue(corner.x >= -.6 && corner.x <= 1.6 && corner.y >= -.6 && corner.y <= 1.6
                                        && corner.z >= -.6 && corner.z <= 1.6,
                                "Render bounds include every orbiting sigil corner");
                    }
                }
                h.assertTrue(start.center().distanceTo(RestrictionTableSigils.sample(seed, slot, 24000).center()) < 1e-9,
                        "World-time wrap joins seamlessly");
            }
        }
        h.assertTrue(artwork.size() == RestrictionTableSigils.ARTWORK.size(), "Every restriction pattern can appear");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rebuiltAnvilStillOpensRestrictionMenu(GameTestHelper h) {
        var local = new BlockPos(3, 2, 3);
        var absolute = h.absolutePos(local);
        h.setBlock(local, ModBlocks.RESTRICTION_TABLE.get());
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new GameProfile(UUID.randomUUID(), "restriction-anvil"), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(player.server, connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) { }
        };
        player.setPos(Vec3.atCenterOf(absolute));
        try {
            var block = (RestrictionTableBlock)ModBlocks.RESTRICTION_TABLE.get();
            var state = h.getLevel().getBlockState(absolute);
            block.useWithoutItem(state, h.getLevel(), absolute, player,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
            h.assertTrue(player.containerMenu instanceof RestrictionTableMenu, "Right-click opens the existing restriction menu");
            var shape = state.getShape(h.getLevel(), absolute);
            h.assertTrue(Math.abs(shape.bounds().maxY - 13.65 / 16) < 1e-6,
                    "Hitbox follows the new anvil striking face without the old raised ornaments");
            h.assertTrue(Math.abs(shape.bounds().getXsize() - 10.35 / 16) < 1e-6
                            && Math.abs(shape.bounds().minZ + 2.67679 / 16) < 1e-6,
                    "Hitbox follows the user's slimmer structure and shortened horn");
        } finally {
            player.closeContainer();
            player.discard();
            channel.close();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void packagedModelHasTextureOnlyPatternsAndAllSigilImages(GameTestHelper h) throws Exception {
        try (var input = RestrictionAnvilGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/geo/block/restriction_table.geo.json");
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var model = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry")
                    .get(0).getAsJsonObject();
            var bones = model.getAsJsonArray("bones");
            h.assertTrue(bones.size() == 4, "Only structural anvil bones remain");
            int count = 0;
            for (var element : bones) {
                var bone = element.getAsJsonObject();
                h.assertTrue(!bone.get("name").getAsString().equals("seal_spinner"), "Raised 3D patterns are removed");
                if (bone.has("cubes")) count += bone.getAsJsonArray("cubes").size();
            }
            h.assertTrue(count == 22, "Packaged model uses the user's latest 22 structural cubes");
            var horn = java.util.stream.StreamSupport.stream(bones.spliterator(), false)
                    .map(element -> element.getAsJsonObject())
                    .filter(bone -> bone.get("name").getAsString().equals("anvil_horn"))
                    .findFirst().orElseThrow().getAsJsonArray("cubes");
            h.assertTrue(horn.size() == 7, "User's simplified horn and connecting cubes are packaged");
            for (int i = 0; i < 4; i++) {
                h.assertTrue(cuboidsConnect(horn.get(i).getAsJsonObject(), horn.get(i + 1).getAsJsonObject()),
                        "No floating or separated horn sections");
            }
            var bridge = horn.get(6).getAsJsonObject();
            var body = java.util.stream.StreamSupport.stream(bones.spliterator(), false)
                    .map(element -> element.getAsJsonObject())
                    .filter(bone -> bone.get("name").getAsString().equals("anvil_body"))
                    .findFirst().orElseThrow().getAsJsonArray("cubes");
            h.assertTrue(cuboidsConnect(bridge, horn.get(0).getAsJsonObject())
                            && cuboidsConnect(bridge, horn.get(5).getAsJsonObject())
                            && cuboidsConnect(horn.get(5).getAsJsonObject(), body.get(1).getAsJsonObject()),
                    "The new horn joins the anvil body without gaps");
        }
        var expectedTypes = java.util.Arrays.stream(
                com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.RestrictionType.values())
                .map(com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem.RestrictionType::id)
                .collect(java.util.stream.Collectors.toSet());
        h.assertTrue(new HashSet<>(RestrictionTableSigils.ARTWORK).equals(expectedTypes),
                "Each restriction type has its own artwork entry");
        for (String name : RestrictionTableSigils.ARTWORK) {
            try (var input = RestrictionAnvilGameTests.class.getResourceAsStream(
                    "/assets/immortal_cultivation_mod/textures/gui/restriction/" + name + ".png")) {
                h.assertTrue(input != null && input.read() == 137, "Unique restriction image is packaged: " + name);
            }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void outlineAndCollisionExactlyMatchCurrentModel(GameTestHelper h) throws Exception {
        VoxelShape expected = Shapes.empty();
        try (var input = RestrictionAnvilGameTests.class.getResourceAsStream(
                "/assets/immortal_cultivation_mod/geo/block/restriction_table.geo.json");
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var bones = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry")
                    .get(0).getAsJsonObject().getAsJsonArray("bones");
            for (var element : bones) {
                var bone = element.getAsJsonObject();
                h.assertTrue(!bone.has("rotation"), "Structural bones have no unhandled rotation");
                if (!bone.has("cubes")) continue;
                for (var cubeElement : bone.getAsJsonArray("cubes")) {
                    var cube = cubeElement.getAsJsonObject();
                    h.assertTrue(!cube.has("rotation") && !cube.has("inflate"),
                            "Structural cube coordinates can be used directly for collision");
                    var origin = cube.getAsJsonArray("origin");
                    var size = cube.getAsJsonArray("size");
                    double x = origin.get(0).getAsDouble(), y = origin.get(1).getAsDouble(),
                            z = origin.get(2).getAsDouble();
                    // GeckoLib negates X, then this renderer translates by one block on X.
                    expected = Shapes.or(expected, Block.box(16 - x - size.get(0).getAsDouble(), y, z,
                            16 - x, y + size.get(1).getAsDouble(), z + size.get(2).getAsDouble()));
                }
            }
        }
        BlockPos pos = h.absolutePos(new BlockPos(3, 2, 3));
        var state = ModBlocks.RESTRICTION_TABLE.get().defaultBlockState();
        h.assertTrue(!Shapes.joinIsNotEmpty(expected, state.getShape(h.getLevel(), pos), BooleanOp.NOT_SAME),
                "Selection hitbox follows every current model cube with no obsolete extensions");
        h.assertTrue(!Shapes.joinIsNotEmpty(expected, state.getCollisionShape(h.getLevel(), pos), BooleanOp.NOT_SAME),
                "Physical collision matches the selection hitbox and all new model cubes");
        Vec3 notch = new Vec3(6.725 / 16, 12.5 / 16, 14.2 / 16);
        h.assertTrue(state.getShape(h.getLevel(), pos).toAabbs().stream().noneMatch(box -> box.contains(notch)),
                "The visible off-center notch remains empty on its rendered side");
        h.succeed();
    }

    private static boolean cuboidsConnect(com.google.gson.JsonObject first, com.google.gson.JsonObject second) {
        var a = first.getAsJsonArray("origin");
        var b = second.getAsJsonArray("origin");
        var as = first.getAsJsonArray("size");
        var bs = second.getAsJsonArray("size");
        for (int axis = 0; axis < 3; axis++) {
            double minimum = Math.max(a.get(axis).getAsDouble(), b.get(axis).getAsDouble());
            double maximum = Math.min(a.get(axis).getAsDouble() + as.get(axis).getAsDouble(),
                    b.get(axis).getAsDouble() + bs.get(axis).getAsDouble());
            if (maximum < minimum - 1e-6) return false;
        }
        return true;
    }

    private RestrictionAnvilGameTests() {}
}
