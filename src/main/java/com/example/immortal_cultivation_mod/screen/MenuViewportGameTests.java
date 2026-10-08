package com.example.immortal_cultivation_mod.screen;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class MenuViewportGameTests {
    @GameTest(template = "fate_test")
    public static void cultivationControlsFitAndHitAtEveryGuiScale(GameTestHelper helper) {
        for (int[] resolution : new int[][]{{854, 480}, {1280, 720}, {1920, 1080}, {3440, 1440}}) {
            for (int guiScale = 1; guiScale <= 6; guiScale++) {
                int width = (int) Math.ceil(resolution[0] / (double) guiScale);
                int height = (int) Math.ceil(resolution[1] / (double) guiScale);
                for (int[] design : new int[][]{{488, 360}, {506, 360}, {450, 405}}) {
                    var view = MenuViewport.fit(width, height, design[0], design[1], 1);
                    helper.assertTrue(view.logicalWidth() >= design[0] && view.logicalHeight() >= design[1],
                            "Full menu, including tabs and bottom controls, has layout space");
                    helper.assertTrue(view.toScreen(view.logicalWidth()) <= width + .001
                            && view.toScreen(view.logicalHeight()) <= height + .001, "Menu fits viewport");
                    helper.assertTrue(view.scale() <= 1 && view.scale() > 0, "Never enlarge past chosen GUI scale");
                    if (width >= design[0] && height >= design[1]) {
                        helper.assertTrue(view.scale() == 1, "Large viewports retain original size");
                    }
                    double left = (view.logicalWidth() - 430) / 2.0;
                    double top = (view.logicalHeight() - 344) / 2.0;
                    // Real stats-menu targets: protruding proficiency tab, stat plus, breakthrough.
                    for (double[] button : new double[][]{{left - 21, top + 81, 25, 52},
                            {left + 171, top + 72, 18, 18}, {left + 309, top + 310, 89, 20}}) {
                        if (design[0] != 488) continue;
                        helper.assertTrue(view.toScreen(button[0]) >= 0 && view.toScreen(button[1]) >= 0
                                && view.toScreen(button[0] + button[2]) <= width
                                && view.toScreen(button[1] + button[3]) <= height, "Visible controls stay on screen");
                        double x = view.toMenu(view.toScreen(button[0] + button[2] / 2));
                        double y = view.toMenu(view.toScreen(button[1] + button[3] / 2));
                        helper.assertTrue(x >= button[0] && x < button[0] + button[2]
                                && y >= button[1] && y < button[1] + button[3], "Rendered button center hits same control");
                    }
                    helper.assertTrue(Math.abs(view.toMenu(view.toScreen(17.25)) - 17.25) < .0001,
                            "Drag distances use same inverse transform as clicks");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void systemWindowFitsAfterGuiScaleChanges(GameTestHelper helper) {
        for (int[] design : new int[][]{{480, 302}, {500, 326}, {620, 390}}) {
            var window = new SystemWindowState(design[0], design[1]);
            for (int[] viewport : new int[][]{{960, 540}, {320, 240}, {480, 270}, {1920, 1080}, {320, 240}}) {
                double scale = MenuViewport.fit(viewport[0] - 12, viewport[1] - 12,
                        design[0], design[1], .82).scale();
                int desktopWidth = (int) Math.floor(viewport[0] / scale);
                int desktopHeight = (int) Math.floor(viewport[1] / scale);
                window.initializeCentered(desktopWidth, desktopHeight);
                for (int mode = 0; mode < 2; mode++) {
                    helper.assertTrue(window.x() >= 0 && window.y() >= 0
                            && (window.x() + window.width()) * scale <= viewport[0] + .001
                            && (window.y() + window.renderedHeight()) * scale <= viewport[1] + .001,
                            "Normal and maximized windows fit after resize");
                    var content = window.contentLayout(design[0], design[1]);
                    double x = window.x() + design[0] - 63;
                    double y = window.y() + design[1] - 27;
                    double screenX = (content.translationX() + x * content.scale()) * scale;
                    double screenY = (content.translationY() + y * content.scale()) * scale;
                    helper.assertTrue(Math.abs(content.virtualX(screenX / scale) - x) < .001
                            && Math.abs(content.virtualY(screenY / scale) - y) < .001,
                            "System footer control hit testing matches both nested transforms");
                    window.toggleMaximize(desktopWidth, desktopHeight);
                }
            }
        }
        helper.succeed();
    }
}
