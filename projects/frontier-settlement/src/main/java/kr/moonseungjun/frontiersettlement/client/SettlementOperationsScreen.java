package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Compact RTS-style operations view built entirely from the already-synchronized presentation snapshot. */
public final class SettlementOperationsScreen extends Screen {
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int innerX;
    private int innerWidth;

    public SettlementOperationsScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("마을 운영"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(680, Math.max(320, this.width - FrontierUiTheme.L));
        panelHeight = Math.min(360, Math.max(240, this.height - FrontierUiTheme.L));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(FrontierUiTheme.S, (this.height - panelHeight) / 2);
        innerX = panelX + FrontierUiTheme.M;
        innerWidth = panelWidth - FrontierUiTheme.M * 2;

        int closeWidth = 48;
        int locationWidth = 58;
        int backWidth = 58;
        int y = panelY + FrontierUiTheme.S;
        int closeX = panelX + panelWidth - FrontierUiTheme.S - closeWidth;
        int locationX = closeX - FrontierUiTheme.XS - locationWidth;
        int backX = locationX - FrontierUiTheme.XS - backWidth;

        addRenderableWidget(Button.builder(Component.literal("건설"), b -> this.minecraft.gui.setScreen(parent))
                .bounds(backX, y, backWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal("거점"),
                        b -> this.minecraft.gui.setScreen(new SettlementLocationScreen(this)))
                .bounds(locationX, y, locationWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose())
                .bounds(closeX, y, closeWidth, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SettlementSnapshotPayload snapshot = ClientSettlementState.snapshot();
        SettlementOperationsSummary summary = SettlementOperationsSummary.from(snapshot);

        FrontierUiTheme.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        drawHeader(graphics, snapshot);
        drawPriority(graphics, summary);
        drawCards(graphics, snapshot, summary);
        drawFooter(graphics, summary);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, SettlementSnapshotPayload snapshot) {
        int titleY = panelY + FrontierUiTheme.S + 4;
        graphics.text(this.font, Component.literal("마을 운영"), innerX, titleY,
                FrontierUiTheme.TEXT_PRIMARY, true);
        String tier = snapshot.tier() == null || snapshot.tier().isBlank() ? "동기화 대기" : snapshot.tier();
        String line = "등급 " + tier + "   ·   인구 " + snapshot.population()
                + "   ·   건물 " + snapshot.context().buildingCount() + "   ·   전초 " + snapshot.context().outpostCount();
        graphics.text(this.font, Component.literal(trim(line, Math.max(40, innerWidth - 180))),
                innerX, panelY + 31, FrontierUiTheme.TEXT_SECONDARY, false);
        FrontierUiTheme.divider(graphics, innerX, panelY + 45, innerWidth);
    }

    private void drawPriority(GuiGraphicsExtractor graphics, SettlementOperationsSummary summary) {
        int y = panelY + 53;
        int height = 40;
        FrontierUiTheme.surface(graphics, innerX, y, innerWidth, height);
        graphics.text(this.font, Component.literal("운영 우선순위"), innerX + FrontierUiTheme.M, y + 7,
                FrontierUiTheme.ACCENT, true);
        graphics.text(this.font, Component.literal(trim(summary.priority(), innerWidth - FrontierUiTheme.M * 2)),
                innerX + FrontierUiTheme.M, y + 22, FrontierUiTheme.TEXT_PRIMARY, false);
        if (!summary.alerts().isEmpty()) {
            String count = "경고 " + summary.alerts().size();
            int width = this.font.width(count);
            graphics.text(this.font, Component.literal(count), innerX + innerWidth - FrontierUiTheme.M - width,
                    y + 7, FrontierUiTheme.WARNING, true);
        }
    }

    private void drawCards(GuiGraphicsExtractor graphics, SettlementSnapshotPayload snapshot,
                           SettlementOperationsSummary summary) {
        int top = panelY + 101;
        int bottom = panelY + panelHeight - 34;
        int gap = FrontierUiTheme.S;
        int cardWidth = Math.max(1, (innerWidth - gap) / 2);
        int cardHeight = Math.max(44, (bottom - top - gap) / 2);

        drawCard(graphics, innerX, top, cardWidth, cardHeight, "성장 · 인구", List.of(
                "인구 " + snapshot.population() + " / 주거 " + summary.housingCapacity(),
                "건물 " + snapshot.context().buildingCount() + " · 전초 " + summary.outposts(),
                projectLine(snapshot)));

        drawCard(graphics, innerX + cardWidth + gap, top, cardWidth, cardHeight, "생산", List.of(
                "가동 " + summary.productionWorking() + " / " + summary.productionSites()
                        + " · 주민 없음 " + summary.productionMissingWorker(),
                "병목 " + summary.productionBlocked() + " · 확인 대기 " + summary.productionUnknown(),
                "개량 가능 " + summary.productionUpgradeBacklog() + "곳"));

        int secondY = top + cardHeight + gap;
        drawCard(graphics, innerX, secondY, cardWidth, cardHeight, "물류 · 자원", List.of(
                "창고 " + summary.warehouses() + " · 수레 정거장 " + summary.cartStations()
                        + " · 포화 " + summary.logisticsSaturated(),
                "목 " + snapshot.wood() + " · 돌 " + snapshot.stone()
                        + " · 금속 " + snapshot.metal() + " · 식량 " + snapshot.food(),
                "물류 확장 가능 " + summary.logisticsUpgradeBacklog() + "곳"));

        drawCard(graphics, innerX + cardWidth + gap, secondY, cardWidth, cardHeight, "방어 · 영토", List.of(
                "초소 " + summary.guardPosts() + " · 감시탑 " + summary.watchtowers()
                        + " · 병영 " + summary.barracks(),
                "성채 " + summary.citadels() + " · 전초 " + summary.outposts(),
                "군사 개량 가능 " + summary.militaryUpgradeBacklog() + "곳"));
    }

    private void drawCard(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                          String title, List<String> lines) {
        FrontierUiTheme.surface(graphics, x, y, width, height);
        int textX = x + FrontierUiTheme.M;
        graphics.text(this.font, Component.literal(title), textX, y + 7,
                FrontierUiTheme.TEXT_PRIMARY, true);
        int lineY = y + 22;
        int maxWidth = Math.max(20, width - FrontierUiTheme.M * 2);
        int maxLines = Math.max(1, Math.min(lines.size(), (height - 25) / 11));
        for (int i = 0; i < maxLines; i++) {
            graphics.text(this.font, Component.literal(trim(lines.get(i), maxWidth)), textX, lineY,
                    i == 0 ? FrontierUiTheme.TEXT_SECONDARY : FrontierUiTheme.TEXT_MUTED, false);
            lineY += 11;
        }
    }

    private String projectLine(SettlementSnapshotPayload snapshot) {
        if (snapshot.context().projectLabel() == null || snapshot.context().projectLabel().isBlank()) {
            return "공사 없음 · 다음 목표 " + shortGoal(snapshot.nextGoal());
        }
        int progress = snapshot.context().projectProgress();
        return snapshot.context().projectLabel() + (progress >= 0 ? " · " + progress + "%" : "");
    }

    private String shortGoal(String goal) {
        if (goal == null || goal.isBlank()) return "자유 확장";
        return goal;
    }

    private void drawFooter(GuiGraphicsExtractor graphics, SettlementOperationsSummary summary) {
        int y = panelY + panelHeight - 19;
        String text;
        if (summary.alerts().isEmpty()) {
            text = "치명적 병목 없음 · 시설 개량은 월드의 해당 작업 지점에서 직접 투자";
        } else {
            text = "우선 확인 · " + summary.alerts().getFirst();
        }
        graphics.text(this.font, Component.literal(trim(text, innerWidth)), innerX, y,
                summary.alerts().isEmpty() ? FrontierUiTheme.TEXT_MUTED : FrontierUiTheme.WARNING, false);
    }

    private String trim(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String out = text;
        while (!out.isEmpty() && this.font.width(out + "…") > maxWidth) out = out.substring(0, out.length() - 1);
        return out + "…";
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean isInGameUi() { return true; }
}
