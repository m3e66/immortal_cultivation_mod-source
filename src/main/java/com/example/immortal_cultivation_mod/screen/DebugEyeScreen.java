package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.DebugEyeClient;
import com.example.immortal_cultivation_mod.client.ModKeyMappings;
import com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics;
import com.example.immortal_cultivation_mod.network.DebugEyePayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Live read-only pages; closing the page leaves the Debug Eye HUD enabled. */
public final class DebugEyeScreen extends CultivationTextScreen {
    private static final String[] PAGES = {"luck_tab", "player_tab", "target_tab", "events_tab", "raw_tab"};
    private int page;
    private boolean frozen;
    private DebugEyePayloads.Snapshot frozenSnapshot;
    public DebugEyeScreen() { super(DebugEyeDiagnostics.text("title"), null); }
    private DebugEyePayloads.Snapshot data() { return frozen ? frozenSnapshot : DebugEyeClient.latest(); }
    @Override protected void initTextPage() {
        int tabWidth = 64;
        for (int i = 0; i < PAGES.length; i++) {
            final int index = i;
            var button = new CultivationGuiButton(panelX() + TEXT_X + i * tabWidth,
                    panelY() + 43, tabWidth - 2, 18, DebugEyeDiagnostics.text(PAGES[i]), () -> {
                page = index; resetTextScroll(); rebuildWidgets();
            });
            button.active = page != i;
            addRenderableWidget(button);
        }
        addFooterButton(48, 100, DebugEyeDiagnostics.text(frozen ? "resume" : "freeze"), () -> {
            if (!frozen) frozenSnapshot = DebugEyeClient.latest();
            frozen = !frozen; rebuildWidgets();
        });
        addFooterButton(160, 100, DebugEyeDiagnostics.text("copy"), () -> {
            if (minecraft != null) minecraft.keyboardHandler.setClipboard(String.join("\n", currentLines().stream().map(Component::getString).toList()));
        });
        addFooterButton(272, 100, Component.translatable("gui.done"), this::onClose);
    }
    private List<Component> currentLines() {
        var snapshot = data();
        if (snapshot == null) return List.of(DebugEyeDiagnostics.text("waiting"));
        return switch (page) {
            case 1 -> snapshot.player(); case 2 -> snapshot.target(); case 3 -> snapshot.events(); case 4 -> snapshot.raw();
            default -> snapshot.luck();
        };
    }
    @Override public void tick() { if (!ClientData.debugEyeEnabled()) onClose(); }
    @Override protected List<Paragraph> paragraphs() {
        var snapshot = data();
        Component status = frozen ? DebugEyeDiagnostics.text("frozen") : DebugEyeClient.stale() ? DebugEyeDiagnostics.text("stale")
                : DebugEyeDiagnostics.text("live", snapshot == null ? "—" : snapshot.tick());
        var result = new ArrayList<Paragraph>();
        result.add(paragraph(status, JADE));
        for (Component line : currentLines()) result.add(paragraph(line));
        return result;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (ModKeyMappings.DEBUG_EYE.get().matches(key, scanCode)) {
            if (!hasShiftDown()) PacketDistributor.sendToServer(new ModPayloads.ServerboundToggleDebugEyePayload());
            onClose(); return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
