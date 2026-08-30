package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** World-backed RPG management menu. Server state remains authoritative. */
public final class MetaMenuScreen extends Screen {
    public enum Tab { PARTY, ENDGAME, CHALLENGE, REGION }

    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int DANGER = 0xFFFF6B6B;
    private static final int PANEL = 0xCC10141D;

    private Tab tab;
    private final List<String> draftParty = new ArrayList<>();
    private int left, top, panelWidth, panelHeight;

    public MetaMenuScreen(Tab tab) {
        super(Component.literal("TURNBOUND"));
        this.tab = tab == null ? Tab.PARTY : tab;
        draftParty.addAll(ClientMetaState.snapshot().activeParty());
    }

    public Tab tab() { return tab; }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(920, Math.max(360, width - 48));
        panelHeight = Math.min(570, Math.max(260, height - 48));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        buildTabs();
        switch (tab) {
            case PARTY -> buildParty();
            case ENDGAME -> buildEndgame();
            case CHALLENGE, REGION -> { }
        }
    }

    private void buildTabs() {
        int x = left + 18, y = top + 58, w = Math.max(72, Math.min(118, (panelWidth - 48) / 4));
        for (Tab value : Tab.values()) {
            int accent = value == tab ? BLUE : MUTED;
            addRenderableWidget(new BattleHudButton(x, y, w, 22, Component.literal(label(value)), accent, ignored -> switchTab(value)));
            x += w + 6;
        }
    }

    private void buildParty() {
        var snapshot = ClientMetaState.snapshot();
        int contentTop = top + 96;
        int gap = 8;
        int cardW = (panelWidth - 54 - gap) / 2;
        int cardH = 30;
        for (int i = 0; i < snapshot.characters().size(); i++) {
            var row = snapshot.characters().get(i);
            int column = i % 2, line = i / 2;
            int x = left + 18 + column * (cardW + gap), y = contentTop + line * (cardH + 6);
            boolean selected = draftParty.contains(row.id());
            String star = row.awakened() ? "◆6" : "★" + row.star();
            String text = (selected ? "● " : "○ ") + star + "  " + row.name() + "  Lv." + row.level() + "  CP " + row.cp();
            addRenderableWidget(new BattleHudButton(x, y, cardW, cardH, Component.literal(text), selected ? GREEN : MUTED,
                    ignored -> toggleParty(row.id())));
        }
        int y = top + panelHeight - 42;
        addRenderableWidget(new BattleHudButton(left + panelWidth - 140, y, 122, 24,
                Component.literal("편성 저장  " + draftParty.size() + "/4"), GREEN, ignored -> saveParty()));
    }

    private void buildEndgame() {
        var rows = ClientMetaState.snapshot().endgame();
        int hardX = left + 18, y = top + 106;
        for (var row : rows.stream().filter(r -> "HARD".equals(r.kind())).toList()) {
            String status = row.cleared() ? "✓" : row.unlocked() ? "" : "🔒";
            var button = new BattleHudButton(hardX, y, Math.min(250, panelWidth / 3), 25,
                    Component.literal(status + " " + row.label()), row.cleared() ? GREEN : row.unlocked() ? DANGER : MUTED,
                    ignored -> start(row));
            button.active = row.unlocked(); addRenderableWidget(button); y += 30;
        }

        int gridLeft = left + Math.min(290, panelWidth / 3 + 38);
        int gridTop = top + 106;
        int available = left + panelWidth - 18 - gridLeft;
        int cols = available >= 500 ? 6 : available >= 360 ? 5 : 4;
        int cellW = Math.max(54, (available - (cols - 1) * 5) / cols);
        int index = 0;
        for (var row : rows.stream().filter(r -> "RIFT".equals(r.kind())).toList()) {
            int x = gridLeft + (index % cols) * (cellW + 5);
            int yy = gridTop + (index / cols) * 31;
            String text = (row.cleared() ? "✓ " : "") + "F" + row.id().substring(row.id().length() - 2) + "  Lv" + row.level();
            var button = new BattleHudButton(x, yy, cellW, 25, Component.literal(text),
                    row.cleared() ? GREEN : row.hardPattern() ? GOLD : row.unlocked() ? BLUE : MUTED,
                    ignored -> start(row));
            button.active = row.unlocked(); addRenderableWidget(button); index++;
        }
    }

    private void switchTab(Tab value) {
        if (value == tab) return;
        tab = value;
        clearWidgets();
        init();
    }

    private void toggleParty(String id) {
        if (draftParty.contains(id)) {
            if (draftParty.size() > 1) draftParty.remove(id);
        } else if (draftParty.size() < 4) draftParty.add(id);
        clearWidgets(); init();
    }

    private void saveParty() { send("PARTY|" + String.join(",", draftParty)); }
    private void start(ClientMetaState.EndgameRow row) { if (row.unlocked()) send("START|" + row.id()); }
    private static void send(String command) { ClientPacketDistributor.sendToServer(new MetaCommandPayload(command)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_P || event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 4, top + panelHeight, BLUE);
        graphics.text(font, Component.literal("TURNBOUND  /  MANAGEMENT"), left + 18, top + 15, TEXT, true);
        var snapshot = ClientMetaState.snapshot();
        String resources = "Gold " + snapshot.gold() + "    Crystal " + snapshot.crystal() + "    Essence " + snapshot.essence()
                + "    Core " + snapshot.core() + "    Party CP " + snapshot.partyCp();
        graphics.text(font, Component.literal(resources), left + 18, top + 36, SECONDARY, false);
        graphics.text(font, Component.literal(title(tab)), left + 18, top + 86, TEXT, true);
        switch (tab) {
            case PARTY -> drawPartyInfo(graphics, snapshot);
            case ENDGAME -> drawEndgameInfo(graphics, snapshot);
            case CHALLENGE -> drawChallenges(graphics, snapshot);
            case REGION -> drawRegions(graphics, snapshot);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPartyInfo(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font, Component.literal("전투 참가 100% XP · 미편성 보유 캐릭터 20% XP · 중복 편성 불가"),
                left + 220, top + 87, SECONDARY, false);
    }

    private void drawEndgameInfo(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font, Component.literal(snapshot.riftUnlocked() ? "Rift Gate 개방" : "B05 클리어 후 Rift Gate 개방"),
                left + 220, top + 87, snapshot.riftUnlocked() ? GREEN : MUTED, false);
        graphics.text(font, Component.literal("Hard/Rift는 라디아에서 입장 · CP는 입장 제한이 아니라 경고 기준"),
                left + 18, top + panelHeight - 24, SECONDARY, false);
    }

    private void drawChallenges(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int x1 = left + 18, x2 = left + panelWidth / 2 + 4, y0 = top + 112;
        for (int i = 0; i < snapshot.challenges().size(); i++) {
            var c = snapshot.challenges().get(i); int x = i < 10 ? x1 : x2; int y = y0 + (i % 10) * 34;
            String state = c.completed() ? "✓" : c.autoEvaluable() ? "○" : "◇";
            graphics.text(font, Component.literal(state + "  " + c.ordinal() + ". " + c.label()), x, y,
                    c.completed() ? GREEN : c.autoEvaluable() ? TEXT : GOLD, false);
            if (!c.autoEvaluable() && !c.unresolvedReason().isBlank()) {
                graphics.text(font, Component.literal(shorten(c.unresolvedReason(), 46)), x + 14, y + 12, MUTED, false);
            }
        }
        graphics.text(font, Component.literal("Challenge 보상: Crystal 150 + Gold 1,500 · 칭호/도감 배지는 정본 배정 미정"),
                left + 18, top + panelHeight - 24, SECONDARY, false);
    }

    private void drawRegions(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int x1 = left + 18, x2 = left + panelWidth / 2 + 4, y0 = top + 112;
        for (int i = 0; i < snapshot.regionQuests().size(); i++) {
            var q = snapshot.regionQuests().get(i); int x = i < 6 ? x1 : x2; int y = y0 + (i % 6) * 50;
            graphics.text(font, Component.literal((q.completed() ? "✓ " : "○ ") + q.id()), x, y, q.completed() ? GREEN : TEXT, false);
            graphics.text(font, Component.literal(q.region() + " · Crystal 200 · Gold 2,000"), x + 14, y + 13, SECONDARY, false);
            if (!q.objectiveSpecified()) graphics.text(font, Component.literal("개별 목표/상자 티어: v0.4 정본 미정"), x + 14, y + 26, GOLD, false);
        }
    }

    private static String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private static String label(Tab tab) { return switch (tab) { case PARTY -> "PARTY"; case ENDGAME -> "HARD / RIFT"; case CHALLENGE -> "CHALLENGE"; case REGION -> "REGION"; }; }
    private static String title(Tab tab) { return switch (tab) { case PARTY -> "파티 편성"; case ENDGAME -> "Hard Boss / Rift Gate"; case CHALLENGE -> "Challenge 20"; case REGION -> "Region Quest 12"; }; }
}
