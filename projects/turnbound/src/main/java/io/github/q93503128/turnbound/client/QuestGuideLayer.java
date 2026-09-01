package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Persistent, non-modal current-objective guide. It intentionally shows only the next useful action. */
public final class QuestGuideLayer implements GuiLayer {
    private static final int TEXT = 0xFFF6F0E4;
    private static final int MUTED = 0xFFC9BDAA;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF80D49A;

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;
        var snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() == io.github.q93503128.turnbound.world.FieldUiSnapshot.Mode.LOADING) return;
        if (snapshot.objective().isBlank()) return;

        int width = Math.min(310, Math.max(228, graphics.guiWidth() / 4));
        int x = graphics.guiWidth() - width - 12;
        int y = 12;
        String objective = playerFacingObjective(snapshot.objective());
        String hint = playerFacingHint(snapshot.dialogue());
        List<String> objectiveLines = wrap(minecraft, objective, width - 32, 2);
        List<String> hintLines = hint.isBlank() ? List.of() : wrap(minecraft, hint, width - 32, 1);
        int height = 47 + objectiveLines.size() * 12 + (hintLines.isEmpty() ? 0 : 18);

        TurnboundUiSkin.panel(graphics, x, y, width, height);
        graphics.text(minecraft.font, Component.literal("현재 목표"), x + 16, y + 13, GOLD, true);
        int ty = y + 31;
        for (String line : objectiveLines) {
            graphics.text(minecraft.font, Component.literal(line), x + 16, ty, TEXT, true);
            ty += 12;
        }
        if (!hintLines.isEmpty()) {
            graphics.text(minecraft.font, Component.literal(hintLines.getFirst()), x + 16, ty + 3, MUTED, false);
        }
        if (snapshot.patrolGoal() > 0 && snapshot.patrolsCleared() < snapshot.patrolGoal()) {
            String progress = snapshot.patrolsCleared() + " / " + snapshot.patrolGoal();
            int tw = minecraft.font.width(progress);
            graphics.text(minecraft.font, Component.literal(progress), x + width - tw - 16, y + 13, GREEN, true);
        }
    }

    /** Internal quest/encounter IDs stay in server state and saves; players see actual actions and names. */
    static String playerFacingObjective(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = stripLeadingInternalQuestId(raw.trim());
        text = text.replace("P01/P03/P04/F03", "카이렌 · 브람 · 엘리시아 · 변경 사냥꾼")
                .replace("ENC_M01/M02 승리", "초입 순찰 2개 격파")
                .replace("ENC_M04의 E003 전투에서 승리", "심부의 불안정 폭발체 격파")
                .replace("B01을 격파", "그라울 격파")
                .replace("B01 그라울 격파", "들이받는 왕 그라울 격파")
                .replace("B02 베르나 격파", "가시어미 베르나 격파")
                .replace("B03 ORO-7 정지", "수문관리기 ORO-7 정지")
                .replace("B04 콜바크 격파", "재의 거상 콜바크 격파")
                .replace("B05 세라크", "균열감시자 세라크")
                .replace("Relay fragment", "Relay 조각");
        return text;
    }

    static String playerFacingHint(String raw) {
        if (raw == null) return "";
        return raw.replace("Relay fragment", "Relay 조각")
                .replace("B01", "그라울")
                .replace("B02", "베르나")
                .replace("B03", "ORO-7")
                .replace("B04", "콜바크")
                .replace("B05", "세라크");
    }

    private static String stripLeadingInternalQuestId(String text) {
        int space = text.indexOf(' ');
        if (space > 0) {
            String first = text.substring(0, space);
            if (first.startsWith("MQ_") || first.startsWith("CQ_") || first.startsWith("RQ_")) {
                return text.substring(space + 1).stripLeading();
            }
        }
        return text;
    }

    private static List<String> wrap(Minecraft minecraft, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (minecraft.font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.length();
            while (cut > 1 && minecraft.font.width(remaining.substring(0, cut)) > maxWidth) cut--;
            int preferred = remaining.lastIndexOf(' ', cut);
            if (preferred > Math.max(0, cut - 14)) cut = preferred;
            String line = remaining.substring(0, Math.max(1, cut)).stripTrailing();
            remaining = remaining.substring(Math.max(1, cut)).stripLeading();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) line = fit(minecraft, line + " " + remaining, maxWidth);
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private static String fit(Minecraft minecraft, String text, int maxWidth) {
        if (minecraft.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 1 && minecraft.font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return text.substring(0, Math.max(1, end)) + suffix;
    }
}
