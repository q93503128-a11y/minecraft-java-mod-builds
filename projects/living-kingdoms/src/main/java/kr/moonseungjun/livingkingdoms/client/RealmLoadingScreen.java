package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class RealmLoadingScreen extends Screen {
    private String homelandId = "unknown";
    private String phase = "preparing";
    private int percent;
    private String message = "왕국 기록부를 확인하고 있습니다.";
    private boolean complete;
    private boolean failed;
    private int completeTicks;

    public RealmLoadingScreen(String initialMessage) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 준비"));
        if (initialMessage != null && !initialMessage.isBlank()) message = initialMessage;
    }

    public void update(RealmBuildProgressPayload payload) {
        homelandId = payload.homelandId();
        phase = payload.phase();
        percent = Math.max(percent, payload.percent());
        message = payload.message();
        complete = payload.complete();
        failed = payload.failed();
        if (failed) complete = false;
    }

    @Override
    public void tick() {
        if (complete && ++completeTicks >= 12) Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return failed;
    }

    @Override
    public void onClose() {
        if (failed) Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ExternalRpgUi.dimWorld(graphics, width, height);
        int panelWidth = Math.min(520, Math.max(290, width - 32));
        int panelHeight = Math.min(230, Math.max(178, height - 28));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        ExternalRpgUi.window(graphics, left, top, panelWidth, panelHeight);

        Item emblem = switch (homelandId) {
            case "silvana_forest" -> Items.OAK_SAPLING;
            case "kardum_league" -> Items.IRON_PICKAXE;
            default -> Items.GOLDEN_HELMET;
        };
        int iconSize = panelHeight < 200 ? 42 : 52;
        ExternalRpgUi.iconFrame(graphics, emblem, left + 27, top + 30, iconSize);
        ExternalRpgUi.title(graphics, font,
                failed ? "왕국 준비 실패" : complete ? "입국 준비 완료" : "왕국을 준비하고 있습니다",
                homelandName(), left + 92, top + 35);
        ExternalRpgUi.divider(graphics, left + 26, top + 91, panelWidth - 52);

        graphics.text(font, Component.literal(phaseLabel()), left + 30, top + 108, 0xFF5D4632, false);
        ExternalRpgUi.progress(graphics, font, left + 30, top + 126, panelWidth - 60,
                "진행률", failed ? "중단" : percent + "%", percent / 100.0F,
                failed ? 0xFF8E3E38 : complete ? 0xFF4F8259 : 0xFF4B6F8C);

        int messageY = top + 165;
        for (var line : font.split(Component.literal(message), panelWidth - 70)) {
            graphics.centeredText(font, line, width / 2, messageY, failed ? 0xFF8E3E38 : 0xFF3F342A);
            messageY += 11;
            if (messageY > top + panelHeight - 24) break;
        }
        if (failed) {
            graphics.centeredText(font, Component.literal("ESC를 눌러 닫고 서버 로그를 확인하십시오."),
                    width / 2, top + panelHeight - 19, 0xFF785248);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private String phaseLabel() {
        return switch (phase) {
            case "survey" -> "입지 조사 · 물과 절벽을 피해 수도 후보를 검토 중";
            case "chunks" -> "지역 준비 · 선택한 부지의 청크를 생성 중";
            case "planning" -> "건축 배치 · 도로와 시설 배치를 조립 중";
            case "building" -> "왕국 건설 · 구역별 작업을 적용 중";
            case "complete" -> "완료 · 선택한 거주지로 이동 중";
            case "failed" -> "실패 · 왕국 생성 작업을 중단함";
            default -> "시작 준비 · 출신과 소속을 확인 중";
        };
    }

    private String homelandName() {
        return switch (homelandId) {
            case "silvana_forest" -> "실바나 수림 의회";
            case "kardum_league" -> "카르둠 산악 연맹";
            case "erden_kingdom" -> "에르덴 왕국 · 로엔 변경백령";
            default -> "살아있는 왕국";
        };
    }
}
