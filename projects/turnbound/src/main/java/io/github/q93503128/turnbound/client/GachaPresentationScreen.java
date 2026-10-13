package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.world.GachaPresentationFlow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen 2D summon presentation for already committed server results.
 * The character image is a build-time bake from the production GeckoLib model/texture; no live entity is rendered.
 */
public final class GachaPresentationScreen extends Screen {
    static final int TEXT = 0xFFF4F0E6;
    static final int SECONDARY = 0xFFAEB7C6;
    static final int MUTED = 0xFF707987;
    static final int GREEN = 0xFF62D39A;
    static final int GOLD = 0xFFFFC857;
    static final int PURPLE = 0xFFC794FF;
    private static final int BACKDROP = 0xFF050815;

    public record Pull(
            String characterId, int stars, boolean newlyOwned, int essence, int pityAfter,
            int bonusLevelGranted, int bonusLevelAfter, boolean spotlight) {
        public Pull(String characterId, int stars, boolean newlyOwned, int essence, int pityAfter) {
            this(characterId, stars, newlyOwned, essence, pityAfter, 0, 0, false);
        }
    }

    /** Legacy stage fields decode safely but are intentionally unused by the 2D presentation. */
    public record Batch(
            String action, int crystalSpent, List<Pull> pulls,
            double stageX, double stageY, double stageZ, float cameraYaw
    ) {
        public Batch {
            pulls = List.copyOf(pulls == null ? List.of() : pulls);
        }
        public Batch(String action, int crystalSpent, List<Pull> pulls) {
            this(action, crystalSpent, pulls, Double.NaN, Double.NaN, Double.NaN, Float.NaN);
        }
    }

    private final Batch batch;
    private final GachaPresentationFlow flow;
    private boolean finishing;
    private boolean presentationDone;
    private boolean audioStarted;
    private boolean fiveStarUpgradeCuePlayed;
    private GachaPresentationFlow.Stage lastAudioStage;
    private int lastAudioIndex = -1;
    private BattleHudButton skipButton;
    private BattleHudButton primaryButton;

    public GachaPresentationScreen(Batch batch) {
        super(Component.literal("정령의 기록"));
        this.batch = batch == null ? new Batch("", 0, List.of()) : batch;
        this.flow = new GachaPresentationFlow(this.batch.pulls().stream()
                .map(pull -> new GachaPresentationFlow.Pull(pull.stars(), pull.newlyOwned()))
                .toList());
    }

    public static Batch decode(String raw) {
        String action = "";
        int spent = 0;
        List<Pull> pulls = new ArrayList<>();
        double stageX = Double.NaN, stageY = Double.NaN, stageZ = Double.NaN;
        float cameraYaw = Float.NaN;
        if (raw == null) raw = "";
        for (String line : raw.split("\\n")) {
            if (line.isBlank()) continue;
            String[] p = line.split("\\|", -1);
            try {
                switch (p[0]) {
                    case "H" -> {
                        action = p[1];
                        spent = Integer.parseInt(p[3]);
                        if (p.length > 7) {
                            stageX = Double.parseDouble(p[4]);
                            stageY = Double.parseDouble(p[5]);
                            stageZ = Double.parseDouble(p[6]);
                            cameraYaw = Float.parseFloat(p[7]);
                        }
                    }
                    case "P" -> pulls.add(new Pull(p[1], Integer.parseInt(p[2]), "1".equals(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                            p.length > 6 ? Integer.parseInt(p[6]) : 0,
                            p.length > 7 ? Integer.parseInt(p[7]) : 0,
                            p.length > 8 && "1".equals(p[8])));
                    default -> { }
                }
            } catch (RuntimeException ignored) { }
        }
        return new Batch(action, spent, pulls, stageX, stageY, stageZ, cameraYaw);
    }

    @Override
    protected void init() {
        super.init();
        skipButton = addRenderableWidget(new BattleHudButton(width - 92, 12, 76, 18,
                Component.literal("건너뛰기"), MUTED, ignored -> skipOrFinish()));
        primaryButton = addRenderableWidget(new BattleHudButton(width - 122, height - 34, 106, 20,
                Component.literal("다음"), GOLD, ignored -> primaryAction()));
        updateControls();
    }

    @Override
    public void tick() {
        super.tick();
        if (!audioStarted) {
            audioStarted = true;
            playStageEntry(flow.stage());
        }
        GachaPresentationFlow.Stage beforeStage = flow.stage();
        int beforeIndex = flow.currentIndex();
        boolean beforeUpgrade = flow.fiveStarUpgrade();
        flow.tick();
        if (flow.stage() != beforeStage || flow.currentIndex() != beforeIndex) playStageEntry(flow.stage());
        if (!beforeUpgrade && flow.fiveStarUpgrade() && !fiveStarUpgradeCuePlayed) {
            fiveStarUpgradeCuePlayed = true;
            ClientAudioDirector.acceptBatch("spawn|SYSTEM|3||||5");
        }
        updateControls();
    }

    private void primaryAction() {
        if (flow.holding()) {
            int beforeIndex = flow.currentIndex();
            flow.advance();
            if (flow.currentIndex() != beforeIndex || flow.summary()) playStageEntry(flow.stage());
            updateControls();
        } else if (flow.summary()) {
            finish();
        }
    }

    private void skipOrFinish() {
        if (flow.summary()) finish();
        else {
            flow.skip();
            updateControls();
        }
    }

    private void updateControls() {
        if (skipButton != null) {
            skipButton.visible = true;
            skipButton.setMessage(Component.literal(flow.summary() ? "닫기" : "건너뛰기"));
        }
        if (primaryButton != null) {
            primaryButton.visible = flow.holding();
            if (flow.holding()) {
                primaryButton.setMessage(Component.literal(flow.lastPull()
                        ? (flow.pullCount() <= 1 ? "확인" : "결과 보기") : "다음"));
            }
        }
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        if (!presentationDone) {
            presentationDone = true;
            ClientPacketDistributor.sendToServer(new MetaCommandPayload("GACHA_DONE"));
        }
        if (minecraft != null) minecraft.gui.setScreen(new FacilitySummonScreen());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            skipOrFinish();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            if (flow.holding() || flow.summary()) {
                primaryAction();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, BACKDROP);
        SummonLayerRenderer.fullScreen(g, SummonAssets.BG_ARCHIVE, width, height);
        SummonPresentationRenderer.render(this, g);
        SummonLayerRenderer.fullScreen(g, SummonAssets.VEIL_SOFT, width, height);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    Batch batchView() { return batch; }
    GachaPresentationFlow flowView() { return flow; }
    int screenWidth() { return width; }
    int screenHeight() { return height; }
    void text(GuiGraphicsExtractor g, String value, int x, int y, int color, boolean shadow) {
        g.text(font, Component.literal(value), x, y, color, shadow);
    }
    int textWidth(String value) { return font.width(value); }

    Pull currentPull() {
        int index = flow.currentIndex();
        return index >= 0 && index < batch.pulls().size() ? batch.pulls().get(index) : null;
    }

    static String characterName(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return id == null ? "" : id; }
    }

    static String stars(int stars) {
        return "★".repeat(Math.max(1, Math.min(5, stars)));
    }

    static int starColor(int stars) {
        return SummonVisualSpec.forStars(stars).accent();
    }

    static String duplicateResult(Pull pull) {
        if (pull.bonusLevelGranted() > 0) return "+레벨 +" + pull.bonusLevelGranted() + " · 누적 +" + pull.bonusLevelAfter();
        if (pull.essence() > 0) return "별의 정수 +" + pull.essence();
        return "보유 중";
    }

    private void playStageEntry(GachaPresentationFlow.Stage stage) {
        if (lastAudioStage == stage && lastAudioIndex == flow.currentIndex()) return;
        lastAudioStage = stage;
        lastAudioIndex = flow.currentIndex();
        Pull pull = currentPull();
        switch (stage) {
            case RITUAL -> ClientAudioDirector.acceptBatch("spawn|SYSTEM|1||||1");
            case OMEN -> {
                int stars = flow.omenStars();
                ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + (stars >= 4 ? 2 : 1) + "||||" + stars);
            }
            case BREAK -> {
                if (flow.highestStars() < 5) {
                    int stars = flow.breakDisplayStars();
                    ClientAudioDirector.acceptBatch("spawn|SYSTEM|" + (stars >= 4 ? 2 : 1) + "||||" + stars);
                }
            }
            case REVEAL -> {
                if (pull != null) {
                    int priority = pull.stars() >= 5 ? 3 : pull.stars() >= 4 ? 2 : 1;
                    ClientAudioDirector.acceptBatch(revealCue(pull) + "|SKILL|" + priority
                            + "|" + pull.characterId() + "||summon|" + pull.stars());
                }
            }
            default -> { }
        }
    }

    private static String revealCue(Pull pull) {
        return switch (pull.characterId()) {
            case "P01" -> "hero_kyren";
            case "P02" -> "hero_lumea";
            case "P03" -> "hero_bram";
            case "P04" -> "hero_elysia";
            case "P05" -> "hero_lynette";
            case "P06" -> "hero_morwen";
            case "P07" -> "hero_marion";
            case "P08" -> "hero_raze";
            default -> "skill";
        };
    }

    @Override
    public void onClose() { skipOrFinish(); }

    @Override
    public void removed() {
        super.removed();
        // Only finish() sends GACHA_DONE. Unexpected replacement cannot silently consume a presentation.
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
