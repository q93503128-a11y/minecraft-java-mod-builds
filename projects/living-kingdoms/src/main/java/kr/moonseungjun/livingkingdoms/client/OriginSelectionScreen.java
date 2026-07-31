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

        this.speciesButton = this.addRenderableWidget(Button.builder(
                Component.empty(), button -> cycleSpecies()
        ).bounds(buttonLeft, top + 72, buttonWidth, 28).build());
        this.homelandButton = this.addRenderableWidget(Button.builder(
                Component.empty(), button -> cycleHomeland()
        ).bounds(buttonLeft, top + 122, buttonWidth, 28).build());
        this.backgroundButton = this.addRenderableWidget(Button.builder(
                Component.empty(), button -> cycleBackground()
        ).bounds(buttonLeft, top + 172, buttonWidth, 28).build());
        this.residenceButton = this.addRenderableWidget(Button.builder(
                Component.empty(), button -> cycleResidence()
        ).bounds(buttonLeft, top + 222, buttonWidth, 28).build());
        this.confirmButton = this.addRenderableWidget(Button.builder(
                Component.translatable("livingkingdoms.origin.confirm"), button -> submit()
        ).bounds(buttonLeft, top + 270, buttonWidth, 32).build());
        refreshButtonLabels();
    }

    private void cycleSpecies() {
        if (submitting) return;
        speciesIndex = (speciesIndex + 1) % TEST_SPECIES.size();
        homelandIndex = 0;
        backgroundIndex = 0;
        residenceIndex = 0;
        normalizeSelection();
        refreshButtonLabels();
    }

    private void cycleHomeland() {
        if (submitting) return;
        List<String> homelands = availableHomelands();
        homelandIndex = (homelandIndex + 1) % homelands.size();
        backgroundIndex = 0;
        residenceIndex = 0;
        normalizeSelection();
        refreshButtonLabels();
    }

    private void cycleBackground() {
        if (submitting) return;
        List<String> backgrounds = availableBackgrounds();
        backgroundIndex = (backgroundIndex + 1) % backgrounds.size();
        normalizeSelection();
        refreshButtonLabels();
    }

    private void cycleResidence() {
        if (submitting) return;
        List<String> residences = availableResidences();
        residenceIndex = (residenceIndex + 1) % residences.size();
        normalizeSelection();
        refreshButtonLabels();
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

    private void refreshButtonLabels() {
        if (speciesButton == null) return;
        speciesButton.setMessage(Component.literal("종족  ·  " + displaySpecies(selectedSpeciesId()) + "  ›"));
        homelandButton.setMessage(Component.literal("출신 세력  ·  " + displayHomeland(selectedHomelandId()) + "  ›"));
        backgroundButton.setMessage(Component.literal("사회적 배경  ·  " + displayBackground(selectedBackgroundId()) + "  ›"));
        residenceButton.setMessage(Component.literal("시작 거주지  ·  " + displayResidence(selectedResidenceId()) + "  ›"));
        confirmButton.active = !submitting;
    }

    private void submit() {
        if (submitting || schemaVersion != 1) return;
        submitting = true;
        statusMessage = "왕국 기록부에 출신을 등록하고 있습니다...";
        refreshButtonLabels();
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
            refreshButtonLabels();
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

        graphics.fill(0, 0, this.width, this.height, 0xD00A0C12);
        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xFF33261E);
        graphics.fill(left, top, right, bottom, 0xFFF0DFC0);
        graphics.fill(left, top, left + 7, bottom, 0xFF8C5A2A);
        graphics.fill(right - 7, top, right, bottom, 0xFF8C5A2A);
        graphics.fill(left + 18, top + 48, right - 18, top + 50, 0xFFB48A50);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

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
