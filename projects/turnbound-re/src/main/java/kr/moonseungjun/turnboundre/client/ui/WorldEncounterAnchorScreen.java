package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.WorldEncounterAnchorClientState;
import kr.moonseungjun.turnboundre.network.WorldEncounterAnchorPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact in-world Encounter preview. All facts and launch validation remain server-owned. */
public final class WorldEncounterAnchorScreen extends Screen {
    private WorldEncounterAnchorPayloads.PreviewView view;
    private long seenGeneration = -1L;
    private boolean startPending;

    public WorldEncounterAnchorScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.anchor.title"));
        syncState();
    }

    @Override
    protected void init() {
        syncState();
    }

    @Override
    public void tick() {
        super.tick();
        if (startPending && BattleClientState.latestSnapshot().isPresent()) {
            this.minecraft.gui.setScreen(null);
            return;
        }
        if (WorldEncounterAnchorClientState.generation() != seenGeneration) {
            syncState();
            if (view == null) this.minecraft.gui.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        WorldEncounterAnchorClientState.clear();
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Preserve the authored world and marker context behind the compact preview.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiLayoutMetrics.Rect root = root();
        UiVisualLanguage.titleBand(
                graphics, this.font,
                root.x(), root.y(), root.width(), 22,
                encounterName(), UiVisualLanguage.TEXT_FOCUS, true);

        if (view == null) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.anchor.loading"),
                    root.x() + UiLayoutMetrics.SPACE_8, root.y() + 34,
                    UiVisualLanguage.TEXT_SECONDARY, true);
        } else {
            int textX = root.x() + UiLayoutMetrics.SPACE_8;
            int y = root.y() + 32;
            graphics.text(this.font,
                    Component.translatable("screen.turnbound_re.anchor.danger", view.difficulty()),
                    textX, y, UiVisualLanguage.TEXT_WARNING, true);
            y += this.font.lineHeight + UiLayoutMetrics.SPACE_4;
            graphics.text(this.font,
                    Component.translatable("screen.turnbound_re.anchor.enemies", fit(enemyLine(), root.width() - 84)),
                    textX, y, UiVisualLanguage.TEXT_PRIMARY, true);
            y += this.font.lineHeight + UiLayoutMetrics.SPACE_4;
            graphics.text(this.font,
                    Component.translatable("screen.turnbound_re.anchor.rewards", fit(rewardLine(), root.width() - 92)),
                    textX, y, UiVisualLanguage.TEXT_PRIMARY, true);
            y += this.font.lineHeight + UiLayoutMetrics.SPACE_4;
            Component repeat = Component.translatable(view.repeatable()
                    ? "screen.turnbound_re.anchor.repeatable"
                    : "screen.turnbound_re.anchor.once");
            Component party = Component.translatable("screen.turnbound_re.anchor.party", view.partySize());
            graphics.text(this.font, Component.literal(repeat.getString() + " · " + party.getString()),
                    textX, y, view.partySize() > 0 ? UiVisualLanguage.TEXT_SECONDARY : UiVisualLanguage.TEXT_WARNING, true);
        }

        UiLayoutMetrics.Rect challenge = challengeButton();
        boolean challengeEnabled = canChallenge();
        UiVisualLanguage.FrameState challengeState = !challengeEnabled
                ? UiVisualLanguage.FrameState.DISABLED
                : TurnboundMenuScreen.contains(challenge, mouseX, mouseY)
                        ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, challenge.x(), challenge.y(), challenge.width(), challenge.height(), challengeState);
        centered(graphics, challenge, Component.translatable("screen.turnbound_re.anchor.challenge"),
                UiVisualLanguage.textColor(challengeState));

        UiLayoutMetrics.Rect back = backButton();
        UiVisualLanguage.FrameState backState = startPending
                ? UiVisualLanguage.FrameState.DISABLED
                : TurnboundMenuScreen.contains(back, mouseX, mouseY)
                        ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, back.x(), back.y(), back.width(), back.height(), backState);
        centered(graphics, back, Component.translatable("gui.back"), UiVisualLanguage.textColor(backState));

        String feedback = feedback();
        if (!feedback.isBlank()) {
            graphics.text(this.font, Component.literal(fit(feedback, root.width() - UiLayoutMetrics.SPACE_16)),
                    root.x() + UiLayoutMetrics.SPACE_8,
                    Math.min(root.bottom() - this.font.lineHeight - UiLayoutMetrics.SPACE_4,
                            challenge.y() - this.font.lineHeight - UiLayoutMetrics.SPACE_4),
                    UiVisualLanguage.TEXT_WARNING, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) Math.floor(event.x());
            int mouseY = (int) Math.floor(event.y());
            if (canChallenge() && TurnboundMenuScreen.contains(challengeButton(), mouseX, mouseY)) {
                startPending = true;
                ClientPacketDistributor.sendToServer(WorldEncounterAnchorPayloads.StartAnchorEncounterC2S.of(
                        view.anchorEntityId(), view.locator(), view.encounterId()));
                return true;
            }
            if (!startPending && TurnboundMenuScreen.contains(backButton(), mouseX, mouseY)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void syncState() {
        seenGeneration = WorldEncounterAnchorClientState.generation();
        view = WorldEncounterAnchorClientState.view().orElse(null);
        if (view != null && view.resultCode() != null && !view.resultCode().isBlank()) startPending = false;
    }

    private boolean canChallenge() {
        return view != null && view.partySize() > 0 && !startPending
                && (view.resultCode() == null || view.resultCode().isBlank());
    }

    private Component encounterName() {
        if (view == null) return Component.translatable("screen.turnbound_re.anchor.title");
        int colon = view.encounterId().indexOf(':');
        String path = colon >= 0 ? view.encounterId().substring(colon + 1) : view.encounterId();
        return Component.translatable("encounter.turnbound_re." + path + ".name");
    }

    private String enemyLine() {
        if (view == null) return "";
        List<String> names = new ArrayList<>();
        for (String source : view.enemySources()) names.add(entityName(source));
        return String.join(" · ", names);
    }

    private String rewardLine() {
        if (view == null) return "";
        List<String> names = new ArrayList<>();
        for (String kind : view.rewardKinds()) {
            String key = "screen.turnbound_re.anchor.reward." + kind.toLowerCase(Locale.ROOT);
            names.add(Component.translatable(key).getString());
        }
        return names.isEmpty()
                ? Component.translatable("screen.turnbound_re.anchor.reward.unknown").getString()
                : String.join(" · ", names);
    }

    private String entityName(String source) {
        if (source == null || source.isBlank()) return "?";
        int colon = source.indexOf(':');
        if (colon > 0 && colon < source.length() - 1) {
            String namespace = source.substring(0, colon);
            String path = source.substring(colon + 1);
            return Component.translatable("entity." + namespace + "." + path).getString();
        }
        return humanize(source);
    }

    private String feedback() {
        if (view == null) return "";
        String code = view.resultCode();
        if (code == null || code.isBlank()) return "";
        return switch (code) {
            case "EMPTY_PARTY", "INVALID_PARTY" -> Component.translatable("screen.turnbound_re.expedition.empty_party").getString();
            case "ALREADY_IN_BATTLE" -> Component.translatable("screen.turnbound_re.expedition.already_in_battle").getString();
            case "ANCHOR_CLEARED" -> Component.translatable("screen.turnbound_re.anchor.cleared").getString();
            case "TOO_FAR" -> Component.translatable("screen.turnbound_re.anchor.too_far").getString();
            case "ANCHOR_MISMATCH" -> Component.translatable("screen.turnbound_re.anchor.mismatch").getString();
            case "ANCHOR_UNAVAILABLE", "INVALID_ENCOUNTER" -> Component.translatable("screen.turnbound_re.anchor.unavailable").getString();
            default -> Component.translatable("screen.turnbound_re.anchor.server_rejected").getString();
        };
    }

    private UiLayoutMetrics.Rect root() {
        int width = Math.min(380, Math.max(252, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int height = 174;
        int x = Math.max(0, (this.width - width) / 2);
        int y = Math.max(UiLayoutMetrics.SPACE_8, this.height / 2 - height / 2);
        return new UiLayoutMetrics.Rect(x, y, width, height);
    }

    private UiLayoutMetrics.Rect challengeButton() {
        UiLayoutMetrics.Rect root = root();
        int gap = UiLayoutMetrics.SPACE_4;
        int width = (root.width() - UiLayoutMetrics.SPACE_16 - gap) * 2 / 3;
        return new UiLayoutMetrics.Rect(root.x() + UiLayoutMetrics.SPACE_8, root.bottom() - 30, width, 22);
    }

    private UiLayoutMetrics.Rect backButton() {
        UiLayoutMetrics.Rect root = root();
        UiLayoutMetrics.Rect challenge = challengeButton();
        int x = challenge.right() + UiLayoutMetrics.SPACE_4;
        return new UiLayoutMetrics.Rect(x, challenge.y(), root.right() - UiLayoutMetrics.SPACE_8 - x, challenge.height());
    }

    private void centered(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect bounds, Component label, int color) {
        int x = bounds.x() + Math.max(UiLayoutMetrics.SPACE_4, (bounds.width() - this.font.width(label)) / 2);
        int y = bounds.y() + Math.max(UiLayoutMetrics.SPACE_2, (bounds.height() - this.font.lineHeight) / 2);
        graphics.text(this.font, label, x, y, color, true);
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    private static String humanize(String id) {
        String raw = id == null ? "" : id.replace('-', '_');
        StringBuilder out = new StringBuilder();
        for (String word : raw.split("_+")) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
