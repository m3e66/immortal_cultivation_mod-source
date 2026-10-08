package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.item.GeoRockBlockItem;
import com.example.immortal_cultivation_mod.item.QiPouchStorage;
import com.example.immortal_cultivation_mod.item.QiPouchWorld;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.QiPouchPayloads;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;

/** Full-volume view of the pouch's real, ticking pocket plot. */
public class QiPouchScreen extends AbstractContainerScreen<QiPouchMenu> {
    private static final long DOUBLE_CLICK_WINDOW_MS = 250L;
    private static final int SCENE_LEFT = 14;
    private static final int SCENE_TOP = 42;
    private static final int SCENE_RIGHT = 382;
    private static final int SCENE_BOTTOM = 304;
    private static final float SCENE_CENTER_X = 198.0F;
    private static final float SCENE_CENTER_Y = 173.0F;
    private static final int INFO_LEFT = 390;
    private static final int INFO_RIGHT = 558;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "textures/misc/white.png");

    private int pendingCell = -1;
    private long pendingClickTime;
    private int dragButton = -1;
    private double pressX;
    private double pressY;
    private double lastDragX;
    private double lastDragY;
    private boolean dragged;
    private float cameraYaw = 45.0F;
    private float cameraPitch = 58.0F;
    private float cameraZoom = 1.0F;
    private float cameraPanX;
    private float cameraPanY;
    private ModelPart doubleLeftLid;
    private ModelPart doubleLeftLock;
    private ModelPart doubleLeftBottom;
    private ModelPart doubleRightLid;
    private ModelPart doubleRightLock;
    private ModelPart doubleRightBottom;
    private CultivationGuiButton buildTabButton;
    private CultivationGuiButton chestTabButton;
    private CultivationGuiButton resetCameraButton;
    private CultivationGuiButton previousPageButton;
    private CultivationGuiButton nextPageButton;
    private EditBox chestSearchBox;
    private String chestSearch = "";

    public QiPouchScreen(
            QiPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 568;
        imageHeight = 330;
        inventoryLabelX = QiPouchMenu.PLAYER_X;
        inventoryLabelY = 98;
    }

    @Override
    protected void init() {
        super.init();
        buildTabButton = addRenderableWidget(new CultivationGuiButton(
                leftPos + 10,
                topPos + 10,
                66,
                20,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.build_tab"),
                () -> selectTab(QiPouchMenu.BUILD_TAB_BUTTON)));
        chestTabButton = addRenderableWidget(new CultivationGuiButton(
                leftPos + 80,
                topPos + 10,
                86,
                20,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.chest_tab"),
                () -> selectTab(QiPouchMenu.CHEST_TAB_BUTTON)));
        resetCameraButton = addRenderableWidget(new CultivationGuiButton(
                leftPos + 500,
                topPos + 10,
                56,
                20,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.reset_camera"),
                this::resetCamera));
        previousPageButton = addRenderableWidget(new CultivationGuiButton(
                leftPos + 80,
                topPos + 274,
                52,
                20,
                Component.literal("<"),
                () -> sendCellButton(
                        QiPouchMenu.CHEST_PREVIOUS_BUTTON)));
        nextPageButton = addRenderableWidget(new CultivationGuiButton(
                leftPos + 264,
                topPos + 274,
                52,
                20,
                Component.literal(">"),
                () -> sendCellButton(QiPouchMenu.CHEST_NEXT_BUTTON)));
        chestSearchBox = new EditBox(
                font,
                leftPos + 31,
                topPos + 47,
                324,
                18,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.search"));
        chestSearchBox.setMaxLength(64);
        chestSearchBox.setHint(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".qi_pouch.search"));
        chestSearchBox.setValue(chestSearch);
        chestSearchBox.setResponder(this::updateChestSearch);
        addRenderableWidget(chestSearchBox);
        updateControls();
    }

    private void selectTab(int button) {
        pendingCell = -1;
        dragButton = -1;
        dragged = false;
        sendCellButton(button);
    }

    private void updateControls() {
        if (buildTabButton == null) return;
        boolean chestTab = menu.isChestTab();
        buildTabButton.active = chestTab;
        chestTabButton.active = !chestTab;
        resetCameraButton.visible = !chestTab;
        chestSearchBox.visible = chestTab;
        chestSearchBox.setEditable(chestTab);
        if (!chestTab && chestSearchBox.isFocused()) {
            chestSearchBox.setFocused(false);
        }
        boolean paged = chestTab && menu.chestPages() > 1;
        previousPageButton.visible = paged;
        nextPageButton.visible = paged;
        previousPageButton.active = menu.chestPage() > 0;
        nextPageButton.active = menu.chestPage() + 1
                < menu.chestPages();
    }

    private void resetCamera() {
        cameraYaw = 45.0F;
        cameraPitch = 58.0F;
        cameraZoom = 1.0F;
        cameraPanX = 0.0F;
        cameraPanY = 0.0F;
    }

    private void updateChestSearch(String value) {
        chestSearch = value;
        PacketDistributor.sendToServer(
                new QiPouchPayloads.ServerboundChestSearch(
                        menu.containerId, value));
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics, float partialTick,
            int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        CultivationGuiTextures.drawLandscape(
                graphics, left, top, imageWidth, imageHeight);
        graphics.fill(left + 6, top + 34,
                left + imageWidth - 6, top + imageHeight - 8,
                0x55F5E8C5);

        CultivationGuiTextures.drawInnerBox(
                graphics, left + INFO_LEFT, top + 42,
                INFO_RIGHT - INFO_LEFT, 262);
        graphics.fill(left + INFO_LEFT + 7, top + 49,
                left + INFO_RIGHT - 7, top + 297,
                0xC7EFE5C8);

        if (menu.isChestTab()) {
            renderChestStorage(graphics);
        } else {
            renderBuildTab(graphics, partialTick, mouseX, mouseY);
        }

        CultivationGuiTextures.drawInnerBox(
                graphics, left + QiPouchMenu.PLAYER_X - 6,
                top + QiPouchMenu.PLAYER_Y - 7,
                174, 92);
        for (int slotIndex = QiPouchMenu.PLAYER_SLOT_START;
             slotIndex < menu.slots.size(); slotIndex++) {
            renderSlotBackground(graphics, menu.getSlot(slotIndex));
        }
    }

    private void renderBuildTab(
            GuiGraphics graphics, float partialTick,
            int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        CultivationGuiTextures.drawInnerBox(
                graphics,
                left + SCENE_LEFT - 5,
                top + SCENE_TOP - 5,
                SCENE_RIGHT - SCENE_LEFT + 10,
                SCENE_BOTTOM - SCENE_TOP + 10);
        graphics.fillGradient(
                left + SCENE_LEFT, top + SCENE_TOP,
                left + SCENE_RIGHT, top + SCENE_BOTTOM,
                0xF0183537, 0xF0061110);

        renderPocketWorld(graphics, partialTick);
        renderVolumeGuide(graphics);
        HoverTarget hovered = hoverTarget(mouseX, mouseY);
        if (hovered != null) renderCellHighlight(graphics, hovered);

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.build_volume",
                        menu.side(), menu.side(), QiPouchWorld.HEIGHT),
                left + (INFO_LEFT + INFO_RIGHT) / 2,
                top + 55, 0xFF4B3925);
        drawWrapped(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.place_help"),
                left + INFO_LEFT + 12, top + 211,
                INFO_RIGHT - INFO_LEFT - 24, 0xFF3B5B48);
        drawWrapped(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.interact_help"),
                left + INFO_LEFT + 12, top + 251,
                INFO_RIGHT - INFO_LEFT - 24, 0xFF5A4430);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.camera_help"),
                left + 18, top + 312, 0xFF4D4638, false);
    }

    private void renderChestStorage(GuiGraphics graphics) {
        int left = leftPos;
        int top = topPos;
        CultivationGuiTextures.drawInnerBox(
                graphics,
                left + SCENE_LEFT - 5,
                top + SCENE_TOP - 5,
                SCENE_RIGHT - SCENE_LEFT + 10,
                SCENE_BOTTOM - SCENE_TOP + 10);
        graphics.fill(left + SCENE_LEFT, top + SCENE_TOP,
                left + SCENE_RIGHT, top + SCENE_BOTTOM,
                0xDCEFE5C8);

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.chest_summary",
                        menu.chestCount(), menu.chestCount() * 27),
                (int) (left + SCENE_CENTER_X), top + 69, 0xFF4B3925);

        if (menu.chestCount() == 0) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.no_chests"),
                    (int) (left + SCENE_CENTER_X), top + 172, 0xFF67523A);
        } else if (!chestSearch.isBlank()
                && menu.chestVisibleSlotCount() == 0) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".qi_pouch.no_search_results"),
                    (int) (left + SCENE_CENTER_X), top + 172, 0xFF67523A);
        }

        for (int slotIndex = QiPouchMenu.CHEST_SLOT_START;
             slotIndex < QiPouchMenu.PLAYER_SLOT_START; slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            if (slot.isActive()) renderSlotBackground(graphics, slot);
        }
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.page",
                        menu.chestPage() + 1, menu.chestPages()),
                (int) (left + SCENE_CENTER_X), top + 280, 0xFF4B3925);

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.chest_access"),
                left + (INFO_LEFT + INFO_RIGHT) / 2,
                top + 58, 0xFF4B3925);
        drawWrapped(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".qi_pouch.chest_help"),
                left + INFO_LEFT + 12, top + 211,
                INFO_RIGHT - INFO_LEFT - 24, 0xFF3B5B48);
    }

    private void renderSlotBackground(
            GuiGraphics graphics, Slot slot) {
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        graphics.fill(x - 1, y - 1, x + 17, y + 17,
                0xFF6D5B42);
        graphics.fill(x, y, x + 16, y + 16,
                0xCC1A2722);
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(
                font, title, imageWidth / 2, 15, 0xFF392C20);
        graphics.drawString(
                font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY,
                0xFF493827, false);
    }

    private void drawWrapped(
            GuiGraphics graphics, Component text,
            int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(
                    font, lines.get(index), x, y + index * 10,
                    color, false);
        }
    }

    private void renderPocketWorld(
            GuiGraphics graphics, float partialTick) {
        graphics.enableScissor(
                leftPos + SCENE_LEFT + 1,
                topPos + SCENE_TOP + 1,
                leftPos + SCENE_RIGHT - 1,
                topPos + SCENE_BOTTOM - 1);
        graphics.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(sceneOriginX(), sceneOriginY(), 180.0F);
        float scale = worldScale();
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(cameraPitch));
        pose.mulPose(Axis.YP.rotationDegrees(cameraYaw));
        int side = menu.side();
        pose.translate(-side * 0.5F, 0.0F, -side * 0.5F);

        // Every grass tile is a normal 1 x 1 x 1 block. Nothing is stretched.
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                renderBlock(
                        graphics, pose,
                        Blocks.GRASS_BLOCK.defaultBlockState(),
                        column, -1.0F, row);
            }
        }

        for (int layer = 0; layer < QiPouchWorld.HEIGHT; layer++) {
            for (int row = 0; row < side; row++) {
                for (int column = 0; column < side; column++) {
                    int cell = QiPouchMenu.volumeCell(
                            layer, row, column);
                    ItemStack placed = menu.getSlot(cell).getItem();
                    if (placed.isEmpty()) continue;
                    if (placed.getItem() instanceof BlockItem blockItem) {
                        BlockState state = QiPouchWorld.displayState(placed);
                        if (blockItem instanceof GeoRockBlockItem) {
                            renderGeoBlockItem(
                                    graphics, pose, placed,
                                    column, layer, row);
                        } else if (state.getBlock() instanceof ChestBlock
                                && state.hasProperty(ChestBlock.TYPE)
                                && state.getValue(ChestBlock.TYPE)
                                != ChestType.SINGLE) {
                            renderDoubleChestHalf(
                                    graphics, pose, state,
                                    column, layer, row);
                        } else {
                            renderBlock(
                                    graphics, pose, state,
                                    column, layer, row);
                        }
                    } else if (placed.getItem() instanceof GeoItem) {
                        renderGeoArtifact(
                                graphics, pose, placed,
                                column, layer, row,
                                partialTick, cell);
                    } else if (FlyingSwordEntity.isFlyingWeapon(placed)) {
                        renderFlyingWeapon(
                                graphics, pose, placed,
                                column, layer, row,
                                partialTick, cell);
                    }
                }
            }
        }

        pose.popPose();
        graphics.flush();
        RenderSystem.disableDepthTest();
        Lighting.setupForFlatItems();
        graphics.disableScissor();
    }

    private void renderBlock(
            GuiGraphics graphics, PoseStack pose, BlockState state,
            float x, float y, float z) {
        pose.pushPose();
        pose.translate(x, y, z);
        minecraft.getBlockRenderer().renderSingleBlock(
                state, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void renderGeoBlockItem(
            GuiGraphics graphics, PoseStack pose, ItemStack stack,
            float x, float y, float z) {
        pose.pushPose();
        // GeoItemRenderer normally raises an item by 0.51 block. Cancel that
        // item-only offset so its model occupies the same coordinates as the
        // real GeoBlockRenderer used in the world.
        pose.translate(x, y - 0.51F, z);
        minecraft.getItemRenderer().renderStatic(
                stack, ItemDisplayContext.NONE,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose, graphics.bufferSource(),
                minecraft.level, 0);
        pose.popPose();
    }

    private void renderDoubleChestHalf(
            GuiGraphics graphics, PoseStack pose, BlockState state,
            float x, float y, float z) {
        ensureDoubleChestModels();
        ChestType type = state.getValue(ChestBlock.TYPE);
        pose.pushPose();
        pose.translate(x, y, z);
        float facing = state.getValue(ChestBlock.FACING).toYRot();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(-0.5F, -0.5F, -0.5F);

        Material material;
        if (state.is(Blocks.TRAPPED_CHEST)) {
            material = type == ChestType.LEFT
                    ? Sheets.CHEST_TRAP_LOCATION_LEFT
                    : Sheets.CHEST_TRAP_LOCATION_RIGHT;
        } else {
            material = type == ChestType.LEFT
                    ? Sheets.CHEST_LOCATION_LEFT
                    : Sheets.CHEST_LOCATION_RIGHT;
        }
        VertexConsumer vertices = material.buffer(
                graphics.bufferSource(), RenderType::entityCutout);
        ModelPart lid = type == ChestType.LEFT
                ? doubleLeftLid : doubleRightLid;
        ModelPart lock = type == ChestType.LEFT
                ? doubleLeftLock : doubleRightLock;
        ModelPart bottom = type == ChestType.LEFT
                ? doubleLeftBottom : doubleRightBottom;
        lid.xRot = 0.0F;
        lock.xRot = 0.0F;
        lid.render(pose, vertices,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        lock.render(pose, vertices,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        bottom.render(pose, vertices,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void ensureDoubleChestModels() {
        if (doubleLeftLid != null && doubleRightLid != null) return;
        ModelPart left = minecraft.getEntityModels()
                .bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        doubleLeftLid = left.getChild("lid");
        doubleLeftLock = left.getChild("lock");
        doubleLeftBottom = left.getChild("bottom");
        ModelPart right = minecraft.getEntityModels()
                .bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        doubleRightLid = right.getChild("lid");
        doubleRightLock = right.getChild("lock");
        doubleRightBottom = right.getChild("bottom");
    }

    private void renderFlyingWeapon(
            GuiGraphics graphics, PoseStack pose, ItemStack weapon,
            int column, int layer, int row,
            float partialTick, int seed) {
        float age = minecraft.level == null
                ? (Util.getMillis() % 20_000L) / 50.0F
                : minecraft.level.getGameTime() + partialTick;
        float bob = 0.10F
                + Mth.sin(age * 0.18F + seed) * 0.08F;
        pose.pushPose();
        pose.translate(
                column + 0.5F,
                layer + 0.72F + bob,
                row + 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(
                age * 2.4F + seed * 31.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-18.0F));
        pose.scale(1.18F, 1.18F, 1.18F);
        minecraft.getItemRenderer().renderStatic(
                weapon, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose, graphics.bufferSource(),
                minecraft.level, seed);
        pose.popPose();
    }

    /**
     * Renders every GeckoLib item through its registered GeoRenderProvider.
     * FIXED lets each model apply its own display transform instead of the
     * pouch having to know every concrete renderer class.
     */
    private void renderGeoArtifact(
            GuiGraphics graphics, PoseStack pose, ItemStack artifact,
            int column, int layer, int row,
            float partialTick, int seed) {
        float age = minecraft.level == null
                ? (Util.getMillis() % 20_000L) / 50.0F
                : minecraft.level.getGameTime() + partialTick;
        float bob = Mth.sin(age * 0.12F + seed) * 0.035F;
        pose.pushPose();
        pose.translate(
                column + 0.5F,
                layer + 0.55F + bob,
                row + 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(
                age * 0.8F + seed * 31.0F));
        minecraft.getItemRenderer().renderStatic(
                artifact, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                pose, graphics.bufferSource(),
                minecraft.level, seed);
        pose.popPose();
    }

    private void renderCellHighlight(
            GuiGraphics graphics, HoverTarget target) {
        float y = target.surfaceY() + 0.015F;
        float[] northWest = projectedPoint(
                target.column(), y, target.row());
        float[] northEast = projectedPoint(
                target.column() + 1.0F, y, target.row());
        float[] southEast = projectedPoint(
                target.column() + 1.0F, y,
                target.row() + 1.0F);
        float[] southWest = projectedPoint(
                target.column(), y, target.row() + 1.0F);
        ItemStack carried = menu.getCarried();
        int color;
        if (!carried.isEmpty()) {
            color = target.placementCell() >= 0
                    && QiPouchStorage.isFloorItem(carried)
                    ? 0x8849E789 : 0x88E05252;
        } else if (target.occupiedCell() >= 0) {
            color = 0x88F0CE67;
        } else {
            color = 0x5549A970;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(
                RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
        highlightVertex(
                vertices, matrix, northWest, color, 0.0F, 0.0F);
        highlightVertex(
                vertices, matrix, northEast, color, 1.0F, 0.0F);
        highlightVertex(
                vertices, matrix, southEast, color, 1.0F, 1.0F);
        highlightVertex(
                vertices, matrix, southWest, color, 0.0F, 1.0F);
        if (target.occupiedCell() >= 0 && carried.isEmpty()) {
            renderOccupiedSelectionBox(
                    vertices, matrix, target, 0xDDF6D77A);
        }
        graphics.flush();
    }

    private void renderOccupiedSelectionBox(
            VertexConsumer vertices, Matrix4f matrix,
            HoverTarget target, int color) {
        float bottom = QiPouchMenu.layerOf(target.occupiedCell()) + 0.01F;
        float top = bottom + 0.98F;
        float left = target.column() + 0.01F;
        float right = target.column() + 0.99F;
        float near = target.row() + 0.01F;
        float far = target.row() + 0.99F;
        float[][] corners = {
                projectedPoint(left, bottom, near),
                projectedPoint(right, bottom, near),
                projectedPoint(right, bottom, far),
                projectedPoint(left, bottom, far),
                projectedPoint(left, top, near),
                projectedPoint(right, top, near),
                projectedPoint(right, top, far),
                projectedPoint(left, top, far)
        };
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            gridLine(vertices, matrix,
                    corners[edge], corners[next], color);
            gridLine(vertices, matrix,
                    corners[edge + 4], corners[next + 4], color);
            gridLine(vertices, matrix,
                    corners[edge], corners[edge + 4], color);
        }
    }

    private void renderVolumeGuide(GuiGraphics graphics) {
        graphics.enableScissor(
                leftPos + SCENE_LEFT + 1,
                topPos + SCENE_TOP + 1,
                leftPos + SCENE_RIGHT - 1,
                topPos + SCENE_BOTTOM - 1);
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(
                RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
        int side = menu.side();
        for (int line = 0; line <= side; line++) {
            gridLine(
                    vertices, matrix,
                    projectedPoint(line, 0.005F, 0.0F),
                    projectedPoint(line, 0.005F, side),
                    0x3859B77C);
            gridLine(
                    vertices, matrix,
                    projectedPoint(0.0F, 0.005F, line),
                    projectedPoint(side, 0.005F, line),
                    0x3859B77C);
        }

        float top = QiPouchWorld.HEIGHT;
        float[][] bottomCorners = {
                projectedPoint(0.0F, 0.0F, 0.0F),
                projectedPoint(side, 0.0F, 0.0F),
                projectedPoint(side, 0.0F, side),
                projectedPoint(0.0F, 0.0F, side)
        };
        float[][] topCorners = {
                projectedPoint(0.0F, top, 0.0F),
                projectedPoint(side, top, 0.0F),
                projectedPoint(side, top, side),
                projectedPoint(0.0F, top, side)
        };
        for (int corner = 0; corner < 4; corner++) {
            gridLine(
                    vertices, matrix,
                    bottomCorners[corner], topCorners[corner],
                    0x4E77D9A2);
            gridLine(
                    vertices, matrix,
                    topCorners[corner], topCorners[(corner + 1) % 4],
                    0x4E77D9A2);
        }
        graphics.flush();
        graphics.disableScissor();
    }

    private static void gridLine(
            VertexConsumer vertices, Matrix4f matrix,
            float[] from, float[] to, int color) {
        float dx = to[0] - from[0];
        float dy = to[1] - from[1];
        float length = Math.max(
                0.001F, Mth.sqrt(dx * dx + dy * dy));
        float nx = -dy / length * 0.34F;
        float ny = dx / length * 0.34F;
        vertices.addVertex(
                        matrix, from[0] + nx, from[1] + ny, 405.0F)
                .setColor(color).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(
                        matrix, to[0] + nx, to[1] + ny, 405.0F)
                .setColor(color).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(
                        matrix, to[0] - nx, to[1] - ny, 405.0F)
                .setColor(color).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        vertices.addVertex(
                        matrix, from[0] - nx, from[1] - ny, 405.0F)
                .setColor(color).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void highlightVertex(
            VertexConsumer vertices, Matrix4f matrix, float[] point,
            int color, float u, float v) {
        vertices.addVertex(
                        matrix, point[0], point[1], 410.0F)
                .setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    @Override
    protected void renderSlotContents(
            GuiGraphics graphics, ItemStack stack, Slot slot,
            @Nullable String countString) {
        if (menu.isFloorSlot(slot)) return;
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
        if (menu.isChestTab()) {
            pendingCell = -1;
        } else if (pendingCell >= 0
                && Util.getMillis() - pendingClickTime
                >= DOUBLE_CLICK_WINDOW_MS) {
            sendCellButton(
                    QiPouchMenu.INTERACT_BUTTON_BASE + pendingCell);
            pendingCell = -1;
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX, double mouseY, int button) {
        if (!menu.isChestTab()
                && (button == 0 || button == 1)
                && insideScene(mouseX, mouseY)) {
            dragButton = button;
            pressX = lastDragX = mouseX;
            pressY = lastDragY = mouseY;
            dragged = false;
            return true;
        }
        pendingCell = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (!menu.isChestTab()
                && button == dragButton && dragButton >= 0) {
            double deltaX = mouseX - lastDragX;
            double deltaY = mouseY - lastDragY;
            if (Math.hypot(
                    mouseX - pressX, mouseY - pressY) > 2.0D) {
                dragged = true;
            }
            if (dragged && button == 0) {
                cameraYaw = Mth.wrapDegrees(
                        cameraYaw + (float)deltaX * 0.65F);
                cameraPitch = Mth.clamp(
                        cameraPitch + (float)deltaY * 0.45F,
                        28.0F, 82.0F);
                pendingCell = -1;
            } else if (dragged && button == 1) {
                cameraPanX += (float)deltaX;
                cameraPanY += (float)deltaY;
                pendingCell = -1;
            }
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(
                mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(
            double mouseX, double mouseY, int button) {
        if (!menu.isChestTab()
                && button == dragButton && dragButton >= 0) {
            boolean wasDragged = dragged;
            dragButton = -1;
            dragged = false;
            if (!wasDragged) {
                handleSceneClick(mouseX, mouseY, button);
            }
            return true;
        }
        // Picking an item up from the inventory and releasing it over the
        // miniature world is a real drag-to-place gesture. AbstractContainerScreen
        // otherwise treats the release over this non-slot scene as an outside click.
        if (!menu.isChestTab()
                && (button == 0 || button == 1)
                && insideScene(mouseX, mouseY)
                && !menu.getCarried().isEmpty()) {
            handleSceneClick(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleSceneClick(
            double mouseX, double mouseY, int button) {
        HoverTarget target = hoverTarget(mouseX, mouseY);
        if (target == null) return;
        if (!menu.getCarried().isEmpty()) {
            if (target.placementCell() >= 0) {
                placeCarriedInCell(target.placementCell());
            }
            pendingCell = -1;
            return;
        }
        int cell = target.occupiedCell();
        if (cell < 0 || !menu.getSlot(cell).hasItem()) {
            pendingCell = -1;
            return;
        }
        if (button == 1) {
            sendCellButton(
                    QiPouchMenu.CYCLE_DIRECTION_BUTTON_BASE + cell);
            pendingCell = -1;
            return;
        }
        if (button != 0) {
            pendingCell = -1;
            return;
        }
        long now = Util.getMillis();
        if (pendingCell == cell
                && now - pendingClickTime
                < DOUBLE_CLICK_WINDOW_MS) {
            sendCellButton(
                    QiPouchMenu.REMOVE_BUTTON_BASE + cell);
            pendingCell = -1;
        } else {
            pendingCell = cell;
            pendingClickTime = now;
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY,
            double scrollX, double scrollY) {
        if (menu.isChestTab()
                && insideScene(mouseX, mouseY)
                && scrollY != 0.0D) {
            sendCellButton(scrollY > 0.0D
                    ? QiPouchMenu.CHEST_PREVIOUS_BUTTON
                    : QiPouchMenu.CHEST_NEXT_BUTTON);
            return true;
        }
        if (insideScene(mouseX, mouseY) && scrollY != 0.0D) {
            cameraZoom = Mth.clamp(
                    cameraZoom * (scrollY > 0.0D
                            ? 1.12F : 0.89F),
                    0.45F, 3.2F);
            return true;
        }
        return super.mouseScrolled(
                mouseX, mouseY, scrollX, scrollY);
    }

    private void placeCarriedInCell(int cell) {
        if (minecraft.gameMode != null
                && minecraft.player != null
                && menu.isActiveCell(cell)
                && menu.getSlot(cell).getItem().isEmpty()) {
            int directionIndex = Math.floorMod(
                    // The camera sits on the positive rotated-Z side and
                    // looks inward. Add 180 degrees to encode that virtual
                    // player's look direction; placement then faces the
                    // block back toward the camera, like vanilla placement.
                    Mth.floor((cameraYaw + 180.0F) / 90.0F + 0.5F),
                    QiPouchMenu.PLACE_DIRECTION_COUNT);
            sendCellButton(QiPouchMenu.PLACE_BUTTON_BASE
                    + directionIndex * QiPouchMenu.SLOT_COUNT + cell);
        }
    }

    @Override
    public void onClose() {
        pendingCell = -1;
        dragButton = -1;
        super.onClose();
    }

    @Nullable
    private HoverTarget hoverTarget(
            double mouseX, double mouseY) {
        if (menu.isChestTab() || !insideScene(mouseX, mouseY)) return null;
        SurfaceHit best = null;

        GridPoint floor = pointOnPlane(mouseX, mouseY, 0.0F);
        if (floor != null) {
            int floorCell = QiPouchMenu.volumeCell(
                    0, floor.row(), floor.column());
            if (!menu.getSlot(floorCell).hasItem()) {
                best = new SurfaceHit(
                        -1, floorCell, floor.column(), floor.row(),
                        0.0F, floor.depth());
            }
        }

        // Empty-hand interaction must pick the first real block face under the
        // cursor. Testing complete cubes (rather than top planes only) allows an
        // exposed side of a lower block in a vertical stack to be selected.
        if (menu.getCarried().isEmpty()) {
            SurfaceHit occupied = occupiedSurfaceHit(mouseX, mouseY);
            if (occupied != null
                    && (best == null || occupied.depth() > best.depth())) {
                best = occupied;
            }
            return best == null ? null : new HoverTarget(
                    best.occupiedCell(), best.placementCell(),
                    best.column(), best.row(), best.surfaceY());
        }

        for (int layer = 0;
             layer < QiPouchWorld.HEIGHT; layer++) {
            float surfaceY = layer + 1.0F;
            GridPoint point = pointOnPlane(
                    mouseX, mouseY, surfaceY);
            if (point == null) continue;
            int cell = QiPouchMenu.volumeCell(
                    layer, point.row(), point.column());
            if (!menu.getSlot(cell).hasItem()) continue;

            int placement = -1;
            if (layer + 1 < QiPouchWorld.HEIGHT) {
                int above = QiPouchMenu.volumeCell(
                        layer + 1, point.row(), point.column());
                if (menu.getSlot(above).getItem().isEmpty()) {
                    placement = above;
                } else {
                    continue;
                }
            }
            SurfaceHit candidate = new SurfaceHit(
                    cell, placement,
                    point.column(), point.row(),
                    surfaceY, point.depth());
            if (best == null
                    || candidate.depth() > best.depth()) {
                best = candidate;
            }
        }
        return best == null ? null : new HoverTarget(
                best.occupiedCell(), best.placementCell(),
                best.column(), best.row(), best.surfaceY());
    }

    @Nullable
    private SurfaceHit occupiedSurfaceHit(
            double mouseX, double mouseY) {
        float scale = worldScale();
        float yaw = cameraYaw * Mth.DEG_TO_RAD;
        float pitch = cameraPitch * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        float screenX = ((float) mouseX - sceneOriginX()) / scale;
        float screenY = ((float) mouseY - sceneOriginY()) / scale;

        // This is the world-space point on the orthographic cursor ray where
        // camera depth is zero, followed by the direction of increasing depth.
        float rotatedZ = sinPitch * screenY;
        float originX = cosYaw * screenX - sinYaw * rotatedZ
                + menu.side() * 0.5F;
        float originY = -cosPitch * screenY;
        float originZ = sinYaw * screenX + cosYaw * rotatedZ
                + menu.side() * 0.5F;
        float directionX = -sinYaw * cosPitch;
        float directionY = sinPitch;
        float directionZ = cosYaw * cosPitch;

        SurfaceHit best = null;
        int side = menu.side();
        for (int layer = 0; layer < QiPouchWorld.HEIGHT; layer++) {
            for (int row = 0; row < side; row++) {
                for (int column = 0; column < side; column++) {
                    int cell = QiPouchMenu.volumeCell(
                            layer, row, column);
                    if (!menu.getSlot(cell).hasItem()) continue;
                    float depth = rayBoxFrontDepth(
                            originX, originY, originZ,
                            directionX, directionY, directionZ,
                            column, layer, row,
                            column + 1.0F, layer + 1.0F, row + 1.0F);
                    if (!Float.isFinite(depth)) continue;
                    SurfaceHit candidate = new SurfaceHit(
                            cell, -1, column, row,
                            layer + 1.0F, depth);
                    if (best == null || depth > best.depth()) {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private static float rayBoxFrontDepth(
            float originX, float originY, float originZ,
            float directionX, float directionY, float directionZ,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        double intervalMin = Double.NEGATIVE_INFINITY;
        double intervalMax = Double.POSITIVE_INFINITY;

        if (Math.abs(directionX) < 1.0E-6F) {
            if (originX < minX || originX > maxX) return Float.NaN;
        } else {
            double first = (minX - originX) / directionX;
            double second = (maxX - originX) / directionX;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            intervalMin = Math.max(intervalMin, first);
            intervalMax = Math.min(intervalMax, second);
            if (intervalMin > intervalMax) return Float.NaN;
        }

        if (Math.abs(directionY) < 1.0E-6F) {
            if (originY < minY || originY > maxY) return Float.NaN;
        } else {
            double first = (minY - originY) / directionY;
            double second = (maxY - originY) / directionY;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            intervalMin = Math.max(intervalMin, first);
            intervalMax = Math.min(intervalMax, second);
            if (intervalMin > intervalMax) return Float.NaN;
        }

        if (Math.abs(directionZ) < 1.0E-6F) {
            if (originZ < minZ || originZ > maxZ) return Float.NaN;
        } else {
            double first = (minZ - originZ) / directionZ;
            double second = (maxZ - originZ) / directionZ;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            intervalMin = Math.max(intervalMin, first);
            intervalMax = Math.min(intervalMax, second);
            if (intervalMin > intervalMax) return Float.NaN;
        }
        return (float) intervalMax;
    }

    @Nullable
    private GridPoint pointOnPlane(
            double mouseX, double mouseY, float y) {
        float scale = worldScale();
        float yaw = cameraYaw * Mth.DEG_TO_RAD;
        float pitch = cameraPitch * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Math.max(0.01F, Mth.sin(pitch));
        float screenX = ((float)mouseX - sceneOriginX()) / scale;
        float screenY = ((float)mouseY - sceneOriginY()) / scale;
        float rotatedZ = (screenY + cosPitch * y) / sinPitch;
        float localX = cosYaw * screenX - sinYaw * rotatedZ;
        float localZ = sinYaw * screenX + cosYaw * rotatedZ;
        int column = Mth.floor(localX + menu.side() * 0.5F);
        int row = Mth.floor(localZ + menu.side() * 0.5F);
        if (column < 0 || column >= menu.side()
                || row < 0 || row >= menu.side()) {
            return null;
        }
        float depth = sinPitch * y + cosPitch * rotatedZ;
        return new GridPoint(column, row, depth);
    }

    private float[] projectedPoint(float x, float y, float z) {
        float localX = x - menu.side() * 0.5F;
        float localZ = z - menu.side() * 0.5F;
        float yaw = cameraYaw * Mth.DEG_TO_RAD;
        float pitch = cameraPitch * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float sinPitch = Mth.sin(pitch);
        float cosPitch = Mth.cos(pitch);
        float rotatedX = cosYaw * localX + sinYaw * localZ;
        float rotatedZ = -sinYaw * localX + cosYaw * localZ;
        float scale = worldScale();
        return new float[]{
                sceneOriginX() + scale * rotatedX,
                sceneOriginY() + scale
                        * (sinPitch * rotatedZ - cosPitch * y)
        };
    }

    private float worldScale() {
        float yaw = cameraYaw * Mth.DEG_TO_RAD;
        float pitch = cameraPitch * Mth.DEG_TO_RAD;
        float footprintSpan = Math.abs(Mth.cos(yaw))
                + Math.abs(Mth.sin(yaw));
        float widthScale = (SCENE_RIGHT - SCENE_LEFT - 42.0F)
                / Math.max(1.0F, menu.side() * footprintSpan);
        float heightUnits = menu.side() * footprintSpan
                * Math.abs(Mth.sin(pitch))
                + QiPouchWorld.HEIGHT * Math.abs(Mth.cos(pitch));
        float heightScale = (SCENE_BOTTOM - SCENE_TOP - 38.0F)
                / Math.max(1.0F, heightUnits);
        return Mth.clamp(
                Math.min(widthScale, heightScale),
                3.2F, 42.0F) * cameraZoom;
    }

    private float sceneOriginX() {
        return leftPos + SCENE_CENTER_X + cameraPanX;
    }

    private float sceneOriginY() {
        float pitch = cameraPitch * Mth.DEG_TO_RAD;
        return topPos + SCENE_CENTER_Y + cameraPanY
                + worldScale() * Mth.cos(pitch)
                * QiPouchWorld.HEIGHT * 0.5F;
    }

    private boolean insideScene(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCENE_LEFT
                && mouseX < leftPos + SCENE_RIGHT
                && mouseY >= topPos + SCENE_TOP
                && mouseY < topPos + SCENE_BOTTOM;
    }

    private void sendCellButton(int id) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId, id);
        }
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!menu.isChestTab()
                && (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.POUCH_PLACE)
                || DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.POUCH_REMOVE))) {
            int pulse = 145 + (int) (Math.sin(System.currentTimeMillis() / 180.0D) * 65.0D);
            int color = (Math.max(70, Math.min(230, pulse)) << 24) | 0xF0C66B;
            graphics.renderOutline(leftPos + SCENE_LEFT - 3,
                    topPos + SCENE_TOP - 3,
                    SCENE_RIGHT - SCENE_LEFT + 6,
                    SCENE_BOTTOM - SCENE_TOP + 6, color);
        }
        HoverTarget target = hoverTarget(mouseX, mouseY);
        if (target != null && target.occupiedCell() >= 0
                && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(
                    font,
                    menu.getSlot(target.occupiedCell()).getItem(),
                    mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private record GridPoint(int column, int row, float depth) {
    }

    private record SurfaceHit(
            int occupiedCell, int placementCell,
            int column, int row, float surfaceY, float depth) {
    }

    private record HoverTarget(
            int occupiedCell, int placementCell,
            int column, int row, float surfaceY) {
    }
}
