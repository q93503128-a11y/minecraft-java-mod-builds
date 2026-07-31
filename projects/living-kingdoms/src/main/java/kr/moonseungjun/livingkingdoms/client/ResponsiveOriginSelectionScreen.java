package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ResponsiveOriginSelectionScreen extends Screen {
    private final int schemaVersion;
    private final OriginChoiceState choice = new OriginChoiceState();
    private boolean submitting;
    private String status = "선택에 따라 시작 위치와 초기 관계가 달라집니다.";
    private Button confirmButton;

    public ResponsiveOriginSelectionScreen(int schemaVersion) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("livingkingdoms.origin.title"));
        this.schemaVersion = schemaVersion;
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        invisible(l.cardX(), l.speciesY(), l.cardW(), l.cardH(), choice::nextSpecies);
        invisible(l.cardX(), l.homelandY(), l.cardW(), l.cardH(), choice::nextHomeland);
        invisible(l.cardX(), l.backgroundY(), l.cardW(), l.cardH(), choice::nextBackground);
        invisible(l.cardX(), l.residenceY(), l.cardW(), l.cardH(), choice::nextResidence);
        confirmButton = invisible(l.cardX(), l.confirmY(), l.cardW(), l.confirmH(), this::submit);
        confirmButton.active = !submitting;
    }

    private Button invisible(int x, int y, int w, int h, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
            if (!submitting) action.run();
        }).bounds(x, y, w, h).build());
        button.setAlpha(0.0F);
        return button;
    }

    private void submit() {
        if (submitting || schemaVersion != 1) return;
        submitting = true;
        status = "왕국 기록부에 출신을 등록하고 있습니다...";
        if (confirmButton != null) confirmButton.active = false;
        ClientPacketDistributor.sendToServer(new SubmitOriginPayload(
                choice.speciesId(), choice.homelandId(), choice.backgroundId(), choice.residenceId()
        ));
    }

    public void handleServerResult(boolean accepted, String message) {
        status = message;
        if (accepted) {
            Minecraft.getInstance().gui.setScreen(null);
        } else {
            submitting = false;
            if (confirmButton != null) confirmButton.active = true;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        int right = l.left() + l.panelW();
        int bottom = l.top() + l.panelH();

        g.fill(0, 0, width, height, 0xD00A0C12);
        g.fill(l.left() - 7, l.top() - 7, right + 7, bottom + 7, 0xFF18100D);
        g.fill(l.left() - 4, l.top() - 4, right + 4, bottom + 4, 0xFF80542D);
        g.fill(l.left(), l.top(), right, bottom, 0xFFF0DFC0);
        g.fill(l.left(), l.top(), l.left() + 7, bottom, 0xFF70431F);
        g.fill(right - 7, l.top(), right, bottom, 0xFF70431F);
        g.fill(l.left() + 18, l.separatorY(), right - 18, l.separatorY() + 2, 0xFFB48A50);
        g.fill(l.left() + 18, bottom - 8, right - 18, bottom - 6, 0xFFD1AE6B);

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        card(g, l, l.speciesY(), "종족  ·  " + choice.speciesName() + "  ›", hover(mouseX, mouseY, l, l.speciesY()), false, true);
        card(g, l, l.homelandY(), "출신 세력  ·  " + choice.homelandName() + "  ›", hover(mouseX, mouseY, l, l.homelandY()), false, true);
        card(g, l, l.backgroundY(), "사회적 배경  ·  " + choice.backgroundName() + "  ›", hover(mouseX, mouseY, l, l.backgroundY()), false, true);
        card(g, l, l.residenceY(), "시작 거주지  ·  " + choice.residenceName(l.compact()) + "  ›", hover(mouseX, mouseY, l, l.residenceY()), false, true);
        drawCard(g, l.cardX(), l.confirmY(), l.cardW(), l.confirmH(),
                submitting ? "왕국 기록부에 등록 중..." : "이 삶으로 시작하기",
                inside(mouseX, mouseY, l.cardX(), l.confirmY(), l.cardW(), l.confirmH()), true, !submitting);

        g.centeredText(font, Component.translatable("livingkingdoms.origin.title"), width / 2, l.titleY(), 0xFF3A2418);
        g.centeredText(font, Component.literal("한 세계에서 어떤 삶으로 시작하시겠습니까?"), width / 2, l.subtitleY(), 0xFF6B4B35);
        g.centeredText(font, Component.literal(hint(mouseX, mouseY, l)), width / 2, l.hintY(), 0xFF5A402D);
        g.centeredText(font, Component.literal(status), width / 2, l.statusY(), submitting ? 0xFF7D5526 : 0xFF4A3528);
    }

    private Layout layout() {
        boolean compact = height < 320 || width < 520;
        int panelW = Math.min(460, Math.max(180, width - 16));
        int panelH = Math.min(compact ? 224 : 300, Math.max(180, height - 12));
        int left = (width - panelW) / 2;
        int top = Math.max(6, (height - panelH) / 2);
        int side = compact ? Math.max(12, Math.min(24, panelW / 12)) : 38;
        int cardX = left + side;
        int cardW = panelW - side * 2;
        int cardH = compact ? 22 : 28;
        int gap = compact ? 4 : 10;
        int speciesY = top + (compact ? 44 : 62);
        int homelandY = speciesY + cardH + gap;
        int backgroundY = homelandY + cardH + gap;
        int residenceY = backgroundY + cardH + gap;
        return new Layout(compact, panelW, panelH, left, top, cardX, cardW, cardH,
                compact ? 26 : 32, speciesY, homelandY, backgroundY, residenceY,
                top + panelH - (compact ? 61 : 65), top + (compact ? 11 : 16),
                top + (compact ? 25 : 33), top + (compact ? 36 : 46),
                residenceY + cardH + (compact ? 5 : 9), top + panelH - (compact ? 17 : 20));
    }

    private void card(GuiGraphicsExtractor g, Layout l, int y, String text, boolean hovered, boolean confirm, boolean enabled) {
        drawCard(g, l.cardX(), y, l.cardW(), l.cardH(), text, hovered, confirm, enabled);
    }

    private void drawCard(GuiGraphicsExtractor g, int x, int y, int w, int h, String text,
                          boolean hovered, boolean confirm, boolean enabled) {
        int outer = enabled ? (confirm ? 0xFF2C4937 : 0xFF59361F) : 0xFF6C6358;
        int border = enabled ? (confirm ? 0xFFD5B66D : 0xFFC89B52) : 0xFF958C7E;
        int inner = enabled ? (confirm ? (hovered ? 0xFF476F50 : 0xFF365941)
                : (hovered ? 0xFF946038 : 0xFF744726)) : 0xFF82786B;
        g.fill(x, y, x + w, y + h, outer);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, border);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, inner);
        g.fill(x + 8, y + h - 6, x + w - 8, y + h - 4, confirm ? 0xFFBFA15A : 0xFFB17E43);
        g.centeredText(font, Component.literal(text), x + w / 2, y + (h - 8) / 2, enabled ? 0xFFFFE8B0 : 0xFFD2C9B9);
    }

    private String hint(int mx, int my, Layout l) {
        if (hover(mx, my, l, l.speciesY())) return switch (choice.speciesId()) {
            case "elf" -> "마력과 감각에 뛰어난 장수 종족";
            case "dwarf" -> "제작과 광업에 강한 산악 종족";
            default -> "어떤 사회와 삶에도 적응하기 쉬운 종족";
        };
        if (hover(mx, my, l, l.homelandY())) return switch (choice.homelandId()) {
            case "silvana_forest" -> "거대 숲과 수관 마을의 삼림 공동체";
            case "kardum_league" -> "산맥과 지하도시의 광업·공학 연맹";
            default -> "도시·농촌·강변이 이어진 인간 왕국";
        };
        if (hover(mx, my, l, l.backgroundY())) return switch (choice.backgroundId()) {
            case "fisher_family" -> "낚시와 물길에 익숙한 집안 출신";
            case "wanderer" -> "야영과 길 찾기에 익숙한 방랑자";
            case "scholar_student" -> "글과 연구 기록을 익힌 수련생";
            default -> "시민권과 이웃 관계를 가진 주민";
        };
        if (hover(mx, my, l, l.residenceY())) return "선택한 거주지에서 안전하게 시작합니다.";
        return "항목을 눌러 선택을 바꾸고 아래에서 시작하세요.";
    }

    private boolean hover(int x, int y, Layout l, int cardY) {
        return inside(x, y, l.cardX(), cardY, l.cardW(), l.cardH());
    }

    private static boolean inside(int x, int y, int bx, int by, int bw, int bh) {
        return x >= bx && y >= by && x < bx + bw && y < by + bh;
    }

    private record Layout(boolean compact, int panelW, int panelH, int left, int top,
                          int cardX, int cardW, int cardH, int confirmH,
                          int speciesY, int homelandY, int backgroundY, int residenceY,
                          int confirmY, int titleY, int subtitleY, int separatorY,
                          int hintY, int statusY) {
    }
}
