package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Current-objective guide with a full-detail J view and a screen-space navigation cue. */
public final class QuestGuideLayer implements GuiLayer {
    private static final int TEXT = 0xFFF6F0E4;
    private static final int MUTED = 0xFFC9BDAA;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF80D49A;
    // Default stays compact; J is the explicit opt-in for the full quest explanation.
    private static boolean expanded = false;

    public static void toggle() { expanded = !expanded; }
    public static boolean expanded() { return expanded; }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() == FieldUiSnapshot.Mode.LOADING || snapshot.objective().isBlank()) return;

        Target target = target(snapshot);
        if (target != null) drawDirectionCue(graphics, minecraft, target);

        int width = expanded
                ? Math.min(238, Math.max(202, graphics.guiWidth() / 4))
                : Math.min(220, Math.max(184, graphics.guiWidth() / 5));
        int x = graphics.guiWidth() - width - 7;
        int y = 7;
        String objective = playerFacingObjective(snapshot.objective());

        if (!expanded) {
            int h = 22;
            TurnboundUiSkin.panel(graphics, x, y, width, h);
            String compact = UiTextLayout.fit("목표 · " + objective, width - 64);
            graphics.text(minecraft.font, Component.literal(compact), x + 8, y + 6, TEXT, true);
            graphics.text(minecraft.font, Component.literal("J 상세"), x + width - 8 - minecraft.font.width("J 상세"), y + 6, GOLD, false);
            return;
        }

        String hint = playerFacingHint(snapshot.dialogue());
        if (isPartyObjective(snapshot.objective())) hint = "E 메뉴 → 파티에서 편성 후 ‘편성 저장’을 누르세요.";
        if (target != null) {
            String location = targetLine(minecraft, target);
            hint = hint.isBlank() ? location : location + " · " + hint;
        }
        List<String> objectiveLines = wrap(minecraft, objective, width - 20, 4);
        List<String> hintLines = hint.isBlank() ? List.of() : wrap(minecraft, hint, width - 20, 2);
        int height = 30 + objectiveLines.size() * 10 + (hintLines.isEmpty() ? 0 : 5 + hintLines.size() * 9);
        height = Math.min(108, Math.max(44, height));

        TurnboundUiSkin.panel(graphics, x, y, width, height);
        graphics.text(minecraft.font, Component.literal("목표"), x + 10, y + 8, GOLD, true);
        graphics.text(minecraft.font, Component.literal("J 접기"), x + width - 10 - minecraft.font.width("J 접기"), y + 8, MUTED, false);
        int ty = y + 22;
        for (String line : objectiveLines) {
            graphics.text(minecraft.font, Component.literal(line), x + 10, ty, TEXT, true);
            ty += 10;
        }
        if (!hintLines.isEmpty()) {
            ty += 1;
            for (String line : hintLines) {
                if (ty + 8 >= y + height) break;
                graphics.text(minecraft.font, Component.literal(line), x + 10, ty, target == null ? MUTED : GOLD, false);
                ty += 9;
            }
        }
        if (snapshot.patrolGoal() > 0 && snapshot.patrolsCleared() < snapshot.patrolGoal()) {
            String progress = snapshot.patrolsCleared() + "/" + snapshot.patrolGoal();
            graphics.text(minecraft.font, Component.literal(progress), x + width - minecraft.font.width(progress) - 10, y + 22, GREEN, true);
        }
    }

    private static void drawDirectionCue(GuiGraphicsExtractor graphics, Minecraft minecraft, Target target) {
        double dx = target.x - minecraft.player.getX(), dz = target.z - minecraft.player.getZ();
        int distance = (int)Math.round(Math.hypot(dx, dz));
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double delta = wrapDegrees(targetYaw - minecraft.player.getYRot());
        String arrow = directionArrow(delta);
        String text = arrow + "  " + target.label + " · " + distance + "m";
        int maxW = Math.min(220, graphics.guiWidth() / 2);
        text = UiTextLayout.fit(text, maxW - 16);
        int w = minecraft.font.width(text) + 16;
        int x = (graphics.guiWidth() - w) / 2, y = 7;
        graphics.fill(x, y, x + w, y + 18, 0xB516181C);
        graphics.fill(x, y + 17, x + w, y + 19, GOLD);
        graphics.text(minecraft.font, Component.literal(text), x + 8, y + 5, GOLD, true);
    }

    private static String directionArrow(double delta) {
        if (delta >= -22.5 && delta < 22.5) return "↑";
        if (delta >= 22.5 && delta < 67.5) return "↖";
        if (delta >= 67.5 && delta < 112.5) return "←";
        if (delta >= 112.5 && delta < 157.5) return "↙";
        if (delta >= 157.5 || delta < -157.5) return "↓";
        if (delta >= -157.5 && delta < -112.5) return "↘";
        if (delta >= -112.5 && delta < -67.5) return "→";
        return "↗";
    }

    private static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) value -= 360.0;
        if (value < -180.0) value += 360.0;
        return value;
    }

    private static String targetLine(Minecraft minecraft, Target target) {
        int distance = (int)Math.round(Math.hypot(target.x - minecraft.player.getX(), target.z - minecraft.player.getZ()));
        return "◆ " + target.label + " · " + distance + "m";
    }

    private static boolean isPartyObjective(String raw) {
        return raw != null && (raw.contains("첫 파티") || raw.contains("편성 확인") || raw.contains("P01/P03/P04/F03"));
    }

    private static Target target(FieldUiSnapshot snapshot) {
        String raw = snapshot.objective();
        if (raw == null) return null;
        if (raw.contains("Director Iven") || raw.contains("라디아 도착")) return new Target("Director Iven", 0.5, 6.5);
        if (isPartyObjective(raw)) return null;
        if (raw.contains("전투 훈련") || raw.contains("남문 개방")) {
            int index = Math.max(0, Math.min(3, snapshot.patrolsCleared()));
            if (index >= 3) return new Target("South Gate", 0, 104);
            double z = index == 0 ? 49 : index == 1 ? 59 : 69;
            return new Target("전투 훈련 " + (index + 1), 50, z);
        }
        if (raw.contains("Chapter 1") || raw.contains("그라울")) {
            if (Math.abs(minecraftPlayerX()) <= 128 && minecraftPlayerZ() <= 128) return new Target("South Gate", 0, 104);
            return new Target("그라울", 355, 245);
        }
        if (raw.contains("Chapter 2") || raw.contains("베르나")) return new Target("베르나", -35, -440);
        if (raw.contains("Chapter 3") || raw.contains("ORO-7")) return new Target("ORO-7", -430, 35);
        if (raw.contains("Chapter 4") || raw.contains("콜바크")) return new Target("콜바크", 65, 455);
        if (raw.contains("중계소 열쇠") || raw.contains("Relay 조각") || raw.contains("계전소")) return new Target("라디아 계전소", 0, 24);
        if (raw.contains("Chapter 5") || raw.contains("세라크")) return new Target("세라크", 430, -350);
        return null;
    }

    private static double minecraftPlayerX() { Minecraft m=Minecraft.getInstance(); return m.player==null?0:m.player.getX(); }
    private static double minecraftPlayerZ() { Minecraft m=Minecraft.getInstance(); return m.player==null?0:m.player.getZ(); }

    static String playerFacingObjective(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = stripLeadingInternalQuestId(raw.trim());
        return text.replace("카이렌/브람/엘리시아/변경 사냥꾼", "카이렌 · 변경 사냥꾼")
                .replace("P01/P03/P04/F03", "카이렌 · 변경 사냥꾼").replace("P01/F03", "카이렌 · 변경 사냥꾼")
                .replace("ENC_M01/M02 승리", "초입 순찰 2개 격파").replace("ENC_M04의 E003 전투에서 승리", "심부의 불안정 폭발체 격파")
                .replace("B01을 격파", "그라울 격파").replace("B01 그라울 격파", "들이받는 왕 그라울 격파")
                .replace("B02 베르나 격파", "가시어미 베르나 격파").replace("B03 ORO-7 정지", "수문관리기 ORO-7 정지")
                .replace("B04 콜바크 격파", "재의 거상 콜바크 격파").replace("B05 세라크", "균열감시자 세라크")
                .replace("Relay fragment", "Relay 조각");
    }

    static String playerFacingHint(String raw) {
        if (raw == null) return "";
        return raw.replace("Relay fragment", "Relay 조각").replace("B01", "그라울").replace("B02", "베르나")
                .replace("B03", "ORO-7").replace("B04", "콜바크").replace("B05", "세라크");
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
