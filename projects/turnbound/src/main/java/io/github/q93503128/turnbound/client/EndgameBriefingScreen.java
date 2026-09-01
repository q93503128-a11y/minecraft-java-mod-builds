package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** World-backed encounter confirmation surface for Radia's post-story atrium. */
public final class EndgameBriefingScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int DANGER = 0xFFFF6B6B;
    private static final int PANEL = 0xE20A0D13;

    public record Briefing(
            String encounterId, String title, String kind, int level, int partyCp, int recommendedCp,
            String recommendedLevel, boolean firstClear, boolean hardPattern, String composition,
            int gold, int crystal, int essence, String firstExtra, String repeatExtra
    ) {}

    private final Briefing briefing;
    private int left, top, panelWidth, panelHeight;

    public EndgameBriefingScreen(Briefing briefing) {
        super(Component.literal("TURNBOUND Endgame Briefing"));
        this.briefing = briefing;
    }

    public static Briefing decode(String raw) {
        String encounterId = "", title = "", kind = "", recommendedLevel = "", composition = "", firstExtra = "", repeatExtra = "";
        int level = 0, partyCp = 0, recommendedCp = -1, gold = 0, crystal = 0, essence = 0;
        boolean firstClear = false, hardPattern = false;
        if (raw == null) raw = "";
        for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            String[] p = line.split("\\|", -1);
            try {
                switch (p[0]) {
                    case "H" -> {
                        encounterId = p[1]; title = p[2]; kind = p[3]; level = Integer.parseInt(p[4]);
                        partyCp = Integer.parseInt(p[5]); recommendedCp = Integer.parseInt(p[6]); recommendedLevel = p[7];
                        firstClear = "1".equals(p[8]); hardPattern = "1".equals(p[9]);
                    }
                    case "E" -> composition = p.length > 1 ? p[1] : "";
                    case "R" -> {
                        gold = Integer.parseInt(p[1]); crystal = Integer.parseInt(p[2]); essence = Integer.parseInt(p[3]);
                        firstExtra = p.length > 4 ? p[4] : ""; repeatExtra = p.length > 5 ? p[5] : "";
                    }
                    default -> { }
                }
            } catch (RuntimeException ignored) { }
        }
        return new Briefing(encounterId, title, kind, level, partyCp, recommendedCp, recommendedLevel,
                firstClear, hardPattern, composition, gold, crystal, essence, firstExtra, repeatExtra);
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(570, Math.max(330, width - 36));
        panelHeight = Math.min(360, Math.max(260, height - 40));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int y = top + panelHeight - 39;
        addRenderableWidget(new BattleHudButton(left + panelWidth - 202, y, 88, 24,
                Component.literal("취소"), MUTED, ignored -> closeBriefing()));
        addRenderableWidget(new BattleHudButton(left + panelWidth - 106, y, 90, 24,
                Component.literal("출전"), accent(), ignored -> deploy()));
    }

    private void deploy() {
        if (briefing.encounterId().isBlank()) return;
        ClientPacketDistributor.sendToServer(new MetaCommandPayload("START|" + briefing.encounterId()));
    }

    private void closeBriefing() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, accent());
        graphics.fill(left, top, left + 4, top + panelHeight, accent());

        int x = left + 20;
        int y = top + 18;
        graphics.text(font, Component.literal(kindLabel()), x, y, accent(), true);
        graphics.text(font, Component.literal(briefing.title()), x, y + 18, TEXT, true);
        graphics.text(font, Component.literal("Lv." + briefing.level() + "   ·   " + briefing.composition()), x, y + 37, SECONDARY, false);

        int guideY = y + 65;
        graphics.text(font, Component.literal("PARTY READINESS"), x, guideY, SECONDARY, true);
        boolean cpKnown = briefing.recommendedCp() > 0;
        boolean under = cpKnown && briefing.partyCp() < briefing.recommendedCp();
        String guide = cpKnown
                ? "Party CP  " + briefing.partyCp() + "    /    권장 CP  " + briefing.recommendedCp()
                : "Party CP  " + briefing.partyCp() + "    /    권장  " + briefing.recommendedLevel();
        graphics.text(font, Component.literal(guide), x, guideY + 16, under ? GOLD : GREEN, true);
        if (cpKnown) {
            int barW = panelWidth - 40;
            int fill = (int)Math.round(barW * Math.min(1.0, briefing.partyCp() / (double)Math.max(1, briefing.recommendedCp())));
            graphics.fill(x, guideY + 32, x + barW, guideY + 37, 0xCC161B24);
            if (fill > 0) graphics.fill(x, guideY + 32, x + fill, guideY + 37, under ? GOLD : GREEN);
            graphics.text(font, Component.literal(under ? "도전 난이도 높음 · 입장 제한 없음" : "권장 전투력 충족"),
                    x, guideY + 44, under ? GOLD : SECONDARY, false);
        }

        int rewardY = guideY + (cpKnown ? 69 : 51);
        graphics.fill(x, rewardY, left + panelWidth - 20, rewardY + 1, 0x55707987);
        graphics.text(font, Component.literal(briefing.firstClear() ? "FIRST CLEAR REWARD" : "REPEAT REWARD"),
                x, rewardY + 10, briefing.firstClear() ? GOLD : SECONDARY, true);
        int ry = rewardY + 29;
        graphics.text(font, Component.literal("Gold  +" + briefing.gold()), x, ry, GOLD, true);
        if (briefing.firstClear() && briefing.crystal() > 0) {
            graphics.text(font, Component.literal("Crystal  +" + briefing.crystal()), x + 118, ry, BLUE, true);
        }
        if (briefing.firstClear() && briefing.essence() > 0) {
            graphics.text(font, Component.literal("Star Essence  +" + briefing.essence()), x + 250, ry, GOLD, true);
        }
        String extra = briefing.firstClear() ? briefing.firstExtra() : briefing.repeatExtra();
        if (!extra.isBlank()) graphics.text(font, Component.literal(extra), x, ry + 18, GREEN, true);

        int ruleY = Math.min(top + panelHeight - 83, ry + (extra.isBlank() ? 35 : 50));
        graphics.text(font, Component.literal(ruleTitle()), x, ruleY, SECONDARY, true);
        graphics.text(font, Component.literal(ruleLine()), x, ruleY + 16, ruleColor(), false);
        if ("RIFT".equals(briefing.kind())) {
            graphics.text(font, Component.literal("전투 사이 전회복 · 층 입장 전 파티 변경 · Auto / ×2 허용"),
                    x, ruleY + 31, BLUE, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private String kindLabel() {
        return switch (briefing.kind()) {
            case "HARD" -> "BOSS REMATCH · HARD";
            case "RIFT" -> briefing.hardPattern() ? "RIFT GATE · BOSS FLOOR" : "RIFT GATE";
            default -> "BOSS REMATCH · NORMAL";
        };
    }

    private String ruleTitle() { return "BATTLE RULE"; }
    private String ruleLine() {
        if ("HARD".equals(briefing.kind())) return "HP ×1.65 · ATK ×1.25 · DEF ×1.15 · SPD +8 · 소환 적 Lv +5";
        if ("RIFT".equals(briefing.kind()) && briefing.hardPattern()) return "보스 강화 패턴 · Floor 표기 Lv 기준";
        if ("RIFT".equals(briefing.kind())) return "Rift 층별 편성 · 도주 불가";
        return "Normal 보스 재도전 · 도주 불가";
    }
    private int ruleColor() { return "HARD".equals(briefing.kind()) ? DANGER : briefing.hardPattern() ? GOLD : SECONDARY; }
    private int accent() { return "HARD".equals(briefing.kind()) ? DANGER : "RIFT".equals(briefing.kind()) ? BLUE : GOLD; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { deploy(); return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { closeBriefing(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { closeBriefing(); }
}
