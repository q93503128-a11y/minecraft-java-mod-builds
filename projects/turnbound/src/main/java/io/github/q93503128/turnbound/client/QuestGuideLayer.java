package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Compact current-objective guide. O collapses/opens it without losing quest context. */
public final class QuestGuideLayer implements GuiLayer {
    private static final int TEXT = 0xFFF6F0E4;
    private static final int MUTED = 0xFFC9BDAA;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF80D49A;
    private static boolean expanded = true;

    public static void toggle() { expanded = !expanded; }
    public static boolean expanded() { return expanded; }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;
        var snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() == io.github.q93503128.turnbound.world.FieldUiSnapshot.Mode.LOADING) return;
        if (snapshot.objective().isBlank()) return;

        if (!expanded) {
            int w = 86, h = 24;
            int x = graphics.guiWidth() - w - 9, y = 9;
            TurnboundUiSkin.panel(graphics, x, y, w, h);
            graphics.text(minecraft.font, Component.literal("목표 · O 열기"), x + 9, y + 8, GOLD, true);
            return;
        }

        int width = Math.min(238, Math.max(188, graphics.guiWidth() / 5));
        int x = graphics.guiWidth() - width - 9;
        int y = 9;
        String objective = playerFacingObjective(snapshot.objective());
        String hint = playerFacingHint(snapshot.dialogue());
        List<String> objectiveLines = wrap(minecraft, objective, width - 24, 2);
        Target target = target(snapshot.objective());
        String targetLine = target == null ? "" : targetLine(minecraft, target);
        if (!targetLine.isBlank()) hint = targetLine;
        List<String> hintLines = hint.isBlank() ? List.of() : wrap(minecraft, hint, width - 24, 1);
        int height = 31 + objectiveLines.size() * 11 + (hintLines.isEmpty() ? 0 : 15);

        TurnboundUiSkin.panel(graphics, x, y, width, height);
        graphics.text(minecraft.font, Component.literal("목표"), x + 12, y + 10, GOLD, true);
        graphics.text(minecraft.font, Component.literal("O 접기"), x + width - 12 - minecraft.font.width("O 접기"), y + 10, MUTED, false);
        int ty = y + 25;
        for (String line : objectiveLines) {
            graphics.text(minecraft.font, Component.literal(line), x + 12, ty, TEXT, true);
            ty += 11;
        }
        if (!hintLines.isEmpty()) graphics.text(minecraft.font, Component.literal(hintLines.getFirst()), x + 12, ty + 1, target == null ? MUTED : GOLD, false);
        if (snapshot.patrolGoal() > 0 && snapshot.patrolsCleared() < snapshot.patrolGoal()) {
            String progress = snapshot.patrolsCleared() + "/" + snapshot.patrolGoal();
            int tw = minecraft.font.width(progress);
            graphics.text(minecraft.font, Component.literal(progress), x + width - tw - 12, y + 24, GREEN, true);
        }
    }

    private static String targetLine(Minecraft minecraft, Target target) {
        double dx = target.x - minecraft.player.getX();
        double dz = target.z - minecraft.player.getZ();
        int distance = (int)Math.round(Math.hypot(dx, dz));
        return "◆ " + target.label + " · " + distance + "m · 금색 표시";
    }

    private static Target target(String raw) {
        if (raw == null) return null;
        if (raw.contains("MQ_P00_01") || raw.contains("Director Iven") || raw.contains("라디아 도착")) return new Target("Director Iven", 0.5, -1.5);
        if (raw.contains("MQ_P00_02") || raw.contains("첫 파티") || raw.contains("편성 확인")) return new Target("파티 편성 콘솔", 7.5, 2.5);
        if (raw.contains("MQ_P00_03") || raw.contains("전투 훈련") || raw.contains("남문 개방")) {
            var s = ClientFieldState.snapshot();
            int index = Math.max(0, Math.min(2, s.patrolsCleared()));
            double z = index == 0 ? 49 : index == 1 ? 59 : 69;
            return new Target("전투 훈련 " + (index + 1), 50, z);
        }
        return null;
    }

    static String playerFacingObjective(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = stripLeadingInternalQuestId(raw.trim());
        return text
                .replace("카이렌/브람/엘리시아/변경 사냥꾼", "카이렌 · 변경 사냥꾼")
                .replace("P01/P03/P04/F03", "카이렌 · 변경 사냥꾼")
                .replace("P01/F03", "카이렌 · 변경 사냥꾼")
                .replace("ENC_M01/M02 승리", "초입 순찰 2개 격파")
                .replace("ENC_M04의 E003 전투에서 승리", "심부의 불안정 폭발체 격파")
                .replace("B01을 격파", "그라울 격파")
                .replace("B01 그라울 격파", "들이받는 왕 그라울 격파")
                .replace("B02 베르나 격파", "가시어미 베르나 격파")
                .replace("B03 ORO-7 정지", "수문관리기 ORO-7 정지")
                .replace("B04 콜바크 격파", "재의 거상 콜바크 격파")
                .replace("B05 세라크", "균열감시자 세라크")
                .replace("Relay fragment", "Relay 조각");
    }

    static String playerFacingHint(String raw) {
        if (raw == null) return "";
        return raw.replace("Relay fragment", "Relay 조각")
                .replace("B01", "그라울").replace("B02", "베르나").replace("B03", "ORO-7")
                .replace("B04", "콜바크").replace("B05", "세라크");
    }

    private static String stripLeadingInternalQuestId(String text) {
        int space = text.indexOf(' ');
        if (space > 0) {
            String first = text.substring(0, space);
            if (first.startsWith("MQ_") || first.startsWith("CQ_") || first.startsWith("RQ_")) return text.substring(space + 1).stripLeading();
        }
        return text;
    }

    private static List<String> wrap(Minecraft minecraft, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (minecraft.font.width(remaining) <= maxWidth) { lines.add(remaining); break; }
            int cut = remaining.length();
            while (cut > 1 && minecraft.font.width(remaining.substring(0, cut)) > maxWidth) cut--;
            int preferred = remaining.lastIndexOf(' ', cut);
            if (preferred > Math.max(0, cut - 14)) cut = preferred;
            String line = remaining.substring(0, Math.max(1, cut)).stripTrailing();
            remaining = remaining.substring(Math.max(1, cut)).stripLeading();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) line = UiTextLayout.fit(line + " " + remaining, maxWidth);
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private record Target(String label, double x, double z) {}
}
