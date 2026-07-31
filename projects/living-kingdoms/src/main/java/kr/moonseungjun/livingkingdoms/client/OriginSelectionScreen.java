package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class OriginSelectionScreen extends Screen {
    private static final List<String> TEST_SPECIES = List.of("human", "elf", "dwarf");
    private static final List<String> TEST_HOMELANDS = List.of("erden_kingdom", "silvana_forest", "kardum_league");
    private static final List<String> TEST_BACKGROUNDS = List.of(
            "common_resident", "fisher_family", "wanderer", "scholar_student"
    );

    private final int schemaVersion;
    private int speciesIndex;
    private int homelandIndex;
    private int backgroundIndex;
    private int residenceIndex;
    private boolean submitting;
    private String statusMessage = "선택에 따라 시작 위치와 초기 관계가 달라집니다.";

    private Button speciesButton;
    private Button homelandButton;
    private Button backgroundButton;
    private Button residenceButton;
    private Button confirmButton;

    public OriginSelectionScreen(int schemaVersion) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("livingkingdoms.origin.title"));
        this.schemaVersion = schemaVersion;
        FoundationCatalog.bootstrap();
        PlayableOriginCatalog.residences();
        normalizeSelection();
    }

    @Override
    protected void init() {
        super.init();
        int panelWidth = Math.min(460, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int buttonLeft = left + 38;
        int buttonWidth = panelWidth - 76;
        int top = Math.max(22, (this.height - 330) / 2);

        this.speciesButton = invisibleButton(buttonLeft, top + 72, buttonWidth, 28, this::cycleSpecies);
        this.homelandButton = invisibleButton(buttonLeft, top + 122, buttonWidth, 28, this::cycleHomeland);
        this.backgroundButton = invisibleButton(buttonLeft, top + 172, buttonWidth, 28, this::cycleBackground);
        this.residenceButton = invisibleButton(buttonLeft, top + 222, buttonWidth, 28, this::cycleResidence);
        this.confirmButton = invisibleButton(buttonLeft, top + 270, buttonWidth, 32, this::submit);
        refreshButtonState();
    }

    private Button invisibleButton(int x, int y, int width, int height, Runnable action) {
        Button button = this.addRenderableWidget(Button.builder(
                Component.empty(), ignored -> action.run()
        ).bounds(x, y, width, height).build());
        button.setAlpha(0.0F);
        return button;
    }

    private void cycleSpecies() {
        if (submitting) return;
        speciesIndex = (speciesIndex + 1) % TEST_SPECIES.size();
        homelandIndex = 0;
        backgroundIndex = 0;
        residenceIndex = 0;
        normalizeSelection();
        refreshButtonState();
    }

    private void cycleHomeland() {
        if (submitting) return;
        List<String> homelands = availableHomelands();
        homelandIndex = (homelandIndex + 1) % homelands.size();
        backgroundIndex = 0;
        residenceIndex = 0;
        normalizeSelection();
        refreshButtonState();
    }

    private void cycleBackground() {
        if (submitting) return;
        List<String> backgrounds = availableBackgrounds();
        backgroundIndex = (backgroundIndex + 1) % backgrounds.size();
        normalizeSelection();
        refreshButtonState();
    }

    private void cycleResidence() {
        if (submitting) return;
        List<String> residences = availableResidences();
        residenceIndex = (residenceIndex + 1) % residences.size();
        normalizeSelection();
        refreshButtonState();
    }

    private void normalizeSelection() {
        speciesIndex = Math.floorMod(speciesIndex, TEST_SPECIES.size());
        homelandIndex = Math.floorMod(homelandIndex, availableHomelands().size());
        backgroundIndex = Math.floorMod(backgroundIndex, availableBackgrounds().size());
        residenceIndex = Math.floorMod(residenceIndex, availableResidences().size());
    }

    private List<String> availableHomelands() {
        FoundationCatalog.SpeciesDefinition species = FoundationCatalog.species().get(selectedSpeciesId());
        List<String> result = new ArrayList<>();
        for (String homelandId : TEST_HOMELANDS) {
            if (species != null && species.allowedHomelandIds().contains(homelandId)) result.add(homelandId);
        }
        if (result.isEmpty()) result.add("erden_kingdom");
        return result;
    }

    private List<String> availableBackgrounds() {
        FoundationCatalog.HomelandDefinition homeland = FoundationCatalog.homelands().get(selectedHomelandId());
        List<String> result = new ArrayList<>();
        for (String backgroundId : TEST_BACKGROUNDS) {
            FoundationCatalog.BackgroundDefinition background = FoundationCatalog.backgrounds().get(backgroundId);
            if (background == null || homeland == null) continue;
            if (background.requiredLifestyleTags().isEmpty()
                    || !java.util.Collections.disjoint(background.requiredLifestyleTags(), homeland.lifestyleTags())) {
                result.add(backgroundId);
            }
        }
        if (result.isEmpty()) result.add("common_resident");
        return result;
    }

    private List<String> availableResidences() {
        List<String> result = PlayableOriginCatalog.residencesFor(selectedHomelandId()).stream()
                .map(PlayableOriginCatalog.ResidenceOption::id)
                .toList();
        if (result.isEmpty()) return List.of("erden_city_room");
        return result;
    }

    private String selectedSpeciesId() {
        return TEST_SPECIES.get(Math.floorMod(speciesIndex, TEST_SPECIES.size()));
    }

    private String selectedHomelandId() {
        List<String> homelands = availableHomelands();
        return homelands.get(Math.floorMod(homelandIndex, homelands.size()));
    }

    private String selectedBackgroundId() {
        List<String> backgrounds = availableBackgrounds();
        return backgrounds.get(Math.floorMod(backgroundIndex, backgrounds.size()));
    }

    private String selectedResidenceId() {
        List<String> residences = availableResidences();
        return residences.get(Math.floorMod(residenceIndex, residences.size()));
    }

    private void refreshButtonState() {
        if (confirmButton != null) {
            confirmButton.active = !submitting;
        }
    }

    private void submit() {
        if (submitting || schemaVersion != 1) return;
        submitting = true;
        statusMessage = "왕국 기록부에 출신을 등록하고 있습니다...";
        refreshButtonState();
        ClientPacketDistributor.sendToServer(new SubmitOriginPayload(
                selectedSpeciesId(), selectedHomelandId(), selectedBackgroundId(), selectedResidenceId()
        ));
    }

    public void handleServerResult(boolean accepted, String message) {
        this.statusMessage = message;
        if (accepted) {
            Minecraft.getInstance().gui.setScreen(null);
        } else {
            submitting = false;
            refreshButtonState();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // Character creation cannot be bypassed by closing the screen.
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(460, this.width - 32);
        int panelHeight = 330;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(22, (this.height - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int cardLeft = left + 38;
        int cardWidth = panelWidth - 76;

        graphics.fill(0, 0, this.width, this.height, 0xD00A0C12);
        graphics.fill(left - 7, top - 7, right + 7, bottom + 7, 0xFF18100D);
        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xFF80542D);
        graphics.fill(left, top, right, bottom, 0xFFF0DFC0);
        graphics.fill(left, top, left + 7, bottom, 0xFF70431F);
        graphics.fill(right - 7, top, right, bottom, 0xFF70431F);
        graphics.fill(left + 18, top + 48, right - 18, top + 50, 0xFFB48A50);
        graphics.fill(left + 18, bottom - 15, right - 18, bottom - 13, 0xFFD1AE6B);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        drawChoiceCard(graphics, cardLeft, top + 72, cardWidth, 28,
                "종족  ·  " + displaySpecies(selectedSpeciesId()) + "  ›",
                isInside(mouseX, mouseY, cardLeft, top + 72, cardWidth, 28), false, true);
        drawChoiceCard(graphics, cardLeft, top + 122, cardWidth, 28,
                "출신 세력  ·  " + displayHomeland(selectedHomelandId()) + "  ›",
                isInside(mouseX, mouseY, cardLeft, top + 122, cardWidth, 28), false, true);
        drawChoiceCard(graphics, cardLeft, top + 172, cardWidth, 28,
                "사회적 배경  ·  " + displayBackground(selectedBackgroundId()) + "  ›",
                isInside(mouseX, mouseY, cardLeft, top + 172, cardWidth, 28), false, true);
        drawChoiceCard(graphics, cardLeft, top + 222, cardWidth, 28,
                "시작 거주지  ·  " + displayResidence(selectedResidenceId()) + "  ›",
                isInside(mouseX, mouseY, cardLeft, top + 222, cardWidth, 28), false, true);
        drawChoiceCard(graphics, cardLeft, top + 270, cardWidth, 32,
                submitting ? "왕국 기록부에 등록 중..." : "이 삶으로 시작하기",
                isInside(mouseX, mouseY, cardLeft, top + 270, cardWidth, 32), true, !submitting);

        graphics.centeredText(this.font, Component.translatable("livingkingdoms.origin.title"),
                this.width / 2, top + 18, 0xFF3A2418);
        graphics.centeredText(this.font, Component.literal("한 세계에서 어떤 삶으로 시작하시겠습니까?"),
                this.width / 2, top + 36, 0xFF6B4B35);
        graphics.centeredText(this.font, Component.literal(speciesDescription(selectedSpeciesId())),
                this.width / 2, top + 104, 0xFF5A402D);
        graphics.centeredText(this.font, Component.literal(homelandDescription(selectedHomelandId())),
                this.width / 2, top + 154, 0xFF5A402D);
        graphics.centeredText(this.font, Component.literal(backgroundDescription(selectedBackgroundId())),
                this.width / 2, top + 204, 0xFF5A402D);
        graphics.centeredText(this.font, Component.literal(statusMessage),
                this.width / 2, top + 310, submitting ? 0xFF7D5526 : 0xFF4A3528);
    }

    private void drawChoiceCard(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean hovered,
            boolean confirm,
            boolean enabled
    ) {
        int outer = enabled ? (confirm ? 0xFF2C4937 : 0xFF59361F) : 0xFF6C6358;
        int border = enabled ? (confirm ? 0xFFD5B66D : 0xFFC89B52) : 0xFF958C7E;
        int inner = enabled
                ? (confirm ? (hovered ? 0xFF476F50 : 0xFF365941) : (hovered ? 0xFF946038 : 0xFF744726))
                : 0xFF82786B;
        int text = enabled ? 0xFFFFE8B0 : 0xFFD2C9B9;

        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, border);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, inner);
        graphics.fill(x + 8, y + height - 6, x + width - 8, y + height - 4,
                confirm ? 0xFFBFA15A : 0xFFB17E43);
        graphics.centeredText(this.font, Component.literal(label), x + width / 2, y + (height - 8) / 2, text);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static String displaySpecies(String id) {
        FoundationCatalog.SpeciesDefinition value = FoundationCatalog.species().get(id);
        return value == null ? id : value.displayName();
    }

    private static String displayHomeland(String id) {
        FoundationCatalog.HomelandDefinition value = FoundationCatalog.homelands().get(id);
        return value == null ? id : value.displayName();
    }

    private static String displayBackground(String id) {
        FoundationCatalog.BackgroundDefinition value = FoundationCatalog.backgrounds().get(id);
        return value == null ? id : value.displayName();
    }

    private static String displayResidence(String id) {
        PlayableOriginCatalog.ResidenceOption value = PlayableOriginCatalog.residences().get(id);
        return value == null ? id : value.displayName();
    }

    private static String speciesDescription(String id) {
        return switch (id) {
            case "elf" -> "긴 수명과 예민한 감각, 마력 친화성을 지닌 종족";
            case "dwarf" -> "강인한 체질과 광업·제작 전통을 지닌 종족";
            default -> "어떤 사회와 삶에도 적응하기 쉬운 유연한 종족";
        };
    }

    private static String homelandDescription(String id) {
        return switch (id) {
            case "silvana_forest" -> "거대한 숲과 마력의 흐름 속에 자리한 삼림 공동체";
            case "kardum_league" -> "산맥과 지하 도시를 잇는 광업·공학 연맹";
            default -> "도시·농촌·강변 생활권이 이어진 인간 중심 왕국";
        };
    }

    private static String backgroundDescription(String id) {
        return switch (id) {
            case "fisher_family" -> "물길과 배, 낚시의 기초를 익힌 집안에서 성장했습니다.";
            case "wanderer" -> "소유는 적지만 야영과 길 찾기에 익숙합니다.";
            case "scholar_student" -> "글과 연구 기록을 읽을 수 있는 학문 수련생입니다.";
            default -> "지역 주민으로서 기본 시민권과 이웃 관계를 갖습니다.";
        };
    }
}
