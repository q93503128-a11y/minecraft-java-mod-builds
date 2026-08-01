package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
        Layout layout = layout();
        invisible(layout.cardX(), layout.speciesY(), layout.cardW(), layout.cardH(), choice::nextSpecies);
        invisible(layout.cardX(), layout.homelandY(), layout.cardW(), layout.cardH(), choice::nextHomeland);
        invisible(layout.cardX(), layout.backgroundY(), layout.cardW(), layout.cardH(), choice::nextBackground);
        invisible(layout.cardX(), layout.residenceY(), layout.cardW(), layout.cardH(), choice::nextResidence);
        confirmButton = invisible(layout.cardX(), layout.confirmY(), layout.cardW(), layout.confirmH(), this::submit);
        confirmButton.active = !submitting;
    }

    private Button invisible(int x, int y, int width, int height, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
            if (!submitting) action.run();
        }).bounds(x, y, width, height).build());
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
        if (!accepted) {
            submitting = false;
            if (confirmButton != null) confirmButton.active = true;
        }
    }

    boolean allRequiredControlsFit() {
        Layout layout = layout();
        return layout.top() >= 0
                && layout.cardX() >= 0
                && layout.cardX() + layout.cardW() <= width
                && layout.residenceY() + layout.cardH() < layout.confirmY()
                && layout.confirmY() + layout.confirmH() <= height
                && layout.statusY() < height;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ExternalRpgUi.dimWorld(graphics, width, height);
        ExternalRpgUi.window(graphics, layout.left(), layout.top(), layout.panelW(), layout.panelH());
        ExternalRpgUi.title(graphics, font, "LIVING KINGDOMS", "삶의 시작을 선택하십시오",
                layout.left() + 28, layout.top() + 20);
        ExternalRpgUi.divider(graphics, layout.left() + 24, layout.top() + 53, layout.panelW() - 48);

        choiceButton(graphics, layout, layout.speciesY(), speciesIcon(),
                "종족", choice.speciesName(), mouseX, mouseY);
        choiceButton(graphics, layout, layout.homelandY(), homelandIcon(),
                "출신 세력", choice.homelandName(), mouseX, mouseY);
        choiceButton(graphics, layout, layout.backgroundY(), backgroundIcon(),
                "사회적 배경", choice.backgroundName(), mouseX, mouseY);
        choiceButton(graphics, layout, layout.residenceY(), Items.RED_BED,
                "시작 거주지", choice.residenceName(layout.compact()), mouseX, mouseY);

        ExternalRpgUi.button(graphics, font, layout.cardX(), layout.confirmY(),
                layout.cardW(), layout.confirmH(),
                submitting ? "왕국 기록부 등록 중" : "이 삶으로 시작하기",
                true, inside(mouseX, mouseY, layout.cardX(), layout.confirmY(), layout.cardW(), layout.confirmH()),
                !submitting);

        String hint = hint(mouseX, mouseY, layout);
        graphics.centeredText(font, Component.literal(hint), width / 2, layout.hintY(), 0xFF5A4636);
        graphics.centeredText(font, Component.literal(status), width / 2, layout.statusY(),
                submitting ? 0xFF775339 : 0xFF3F342A);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void choiceButton(GuiGraphicsExtractor graphics, Layout layout, int y, Item icon,
                              String category, String value, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, layout.cardX(), y, layout.cardW(), layout.cardH());
        ExternalRpgUi.button(graphics, font, layout.cardX(), y, layout.cardW(), layout.cardH(),
                "", false, hovered, !submitting);
        graphics.fakeItem(new net.minecraft.world.item.ItemStack(icon), layout.cardX() + 12,
                y + Math.max(5, (layout.cardH() - 16) / 2));
        graphics.text(font, Component.literal(category), layout.cardX() + 39, y + 8,
                0xFF806143, false);
        graphics.text(font, Component.literal(value), layout.cardX() + 126, y + 8,
                0xFF352A21, false);
        graphics.text(font, Component.literal("›"), layout.cardX() + layout.cardW() - 22,
                y + 8, 0xFF6B5038, false);
    }

    private Layout layout() {
        boolean compact = height < 325 || width < 560;
        int panelWidth = Math.min(520, Math.max(300, width - 24));
        int panelHeight = Math.min(compact ? 258 : 322, Math.max(218, height - 16));
        int left = (width - panelWidth) / 2;
        int top = Math.max(5, (height - panelHeight) / 2);
        int side = compact ? 24 : 38;
        int cardX = left + side;
        int cardWidth = panelWidth - side * 2;
        int cardHeight = compact ? 27 : 34;
        int gap = compact ? 5 : 8;
        int speciesY = top + (compact ? 65 : 76);
        int homelandY = speciesY + cardHeight + gap;
        int backgroundY = homelandY + cardHeight + gap;
        int residenceY = backgroundY + cardHeight + gap;
        int confirmHeight = compact ? 30 : 36;
        int confirmY = top + panelHeight - (compact ? 67 : 76);
        return new Layout(compact, panelWidth, panelHeight, left, top, cardX, cardWidth,
                cardHeight, confirmHeight, speciesY, homelandY, backgroundY, residenceY,
                confirmY, residenceY + cardHeight + 7, top + panelHeight - 22);
    }

    private Item speciesIcon() {
        return switch (choice.speciesId()) {
            case "elf" -> Items.AMETHYST_SHARD;
            case "dwarf" -> Items.IRON_PICKAXE;
            default -> Items.PLAYER_HEAD;
        };
    }

    private Item homelandIcon() {
        return switch (choice.homelandId()) {
            case "silvana_forest" -> Items.OAK_SAPLING;
            case "kardum_league" -> Items.ANVIL;
            default -> Items.GOLDEN_HELMET;
        };
    }

    private Item backgroundIcon() {
        return switch (choice.backgroundId()) {
            case "fisher_family" -> Items.FISHING_ROD;
            case "wanderer" -> Items.COMPASS;
            case "scholar_student" -> Items.WRITABLE_BOOK;
            default -> Items.EMERALD;
        };
    }

    private String hint(int mouseX, int mouseY, Layout layout) {
        if (hover(mouseX, mouseY, layout, layout.speciesY())) return switch (choice.speciesId()) {
            case "elf" -> "마력과 감각에 뛰어난 장수 종족";
            case "dwarf" -> "제작과 광업에 강한 산악 종족";
            default -> "어떤 사회와 삶에도 적응하기 쉬운 종족";
        };
        if (hover(mouseX, mouseY, layout, layout.homelandY())) return switch (choice.homelandId()) {
            case "silvana_forest" -> "거대 숲과 수관 마을의 삼림 공동체";
            case "kardum_league" -> "산맥과 지하도시의 광업·공학 연맹";
            default -> "도시·농촌·강변이 이어진 인간 왕국";
        };
        if (hover(mouseX, mouseY, layout, layout.backgroundY())) return switch (choice.backgroundId()) {
            case "fisher_family" -> "낚시와 물길에 익숙한 집안 출신";
            case "wanderer" -> "야영과 길 찾기에 익숙한 방랑자";
            case "scholar_student" -> "글과 연구 기록을 익힌 수련생";
            default -> "시민권과 이웃 관계를 가진 주민";
        };
        if (hover(mouseX, mouseY, layout, layout.residenceY())) return "왕국 건설이 끝난 뒤 선택한 거주지에서 시작합니다.";
        return "항목을 눌러 선택을 바꾸십시오.";
    }

    private boolean hover(int x, int y, Layout layout, int cardY) {
        return inside(x, y, layout.cardX(), cardY, layout.cardW(), layout.cardH());
    }

    private static boolean inside(int x, int y, int boxX, int boxY, int boxWidth, int boxHeight) {
        return x >= boxX && y >= boxY && x < boxX + boxWidth && y < boxY + boxHeight;
    }

    private record Layout(boolean compact, int panelW, int panelH, int left, int top,
                          int cardX, int cardW, int cardH, int confirmH,
                          int speciesY, int homelandY, int backgroundY, int residenceY,
                          int confirmY, int hintY, int statusY) {
    }
}
