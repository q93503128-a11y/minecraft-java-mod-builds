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

/** Main objective guide with up to two auxiliary goals and a full-detail K view. */
public final class QuestGuideLayer implements GuiLayer {
    private static final int TEXT = 0xFFF6F0E4;
    private static final int MUTED = 0xFFC9BDAA;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF80D49A;
    private static boolean expanded = false;
    private static String selectedObjectiveId = "MAIN";

    public static void toggle() { expanded = !expanded; }
    public static boolean expanded() { return expanded; }

    public static void cycleObjective() {
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        if (snapshot == null || !snapshot.active()) return;
        List<String> ids = new ArrayList<>();
        ids.add("MAIN");
        for (FieldUiSnapshot.QuestTracker quest : snapshot.questTrackers()) ids.add(quest.id());
        if (ids.size() <= 1) {
            selectedObjectiveId = "MAIN";
            ClientUiFeedbackLayer.show("표시할 다른 목표가 없습니다.");
            return;
        }
        int index = ids.indexOf(selectedObjectiveId);
        selectedObjectiveId = ids.get((index < 0 ? 0 : index + 1) % ids.size());
        DisplayObjective selected = displayObjective(snapshot);
        ClientUiFeedbackLayer.show("목표 표시 · " + selected.heading());
    }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientPresentationTransition.fieldPresentationSuppressed()) return;
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() == FieldUiSnapshot.Mode.LOADING || snapshot.objective().isBlank()) return;

        DisplayObjective selected = displayObjective(snapshot);
        Target target = selected.main() ? target(snapshot) : null;
        if (target != null) drawDirectionCue(graphics, minecraft, target);

        int width = expanded
                ? Math.min(286, Math.max(210, graphics.guiWidth() / 4))
                : Math.min(194, Math.max(158, graphics.guiWidth() / 6));
        int x = 7;
        int y = 7;
        int choiceCount = 1 + snapshot.questTrackers().size();
        int choiceIndex = objectiveIndex(snapshot, selected.id());

        if (!expanded) {
            int h = 22;
            TurnboundUiSkin.panel(graphics, x, y, width, h);
            String compact = UiTextLayout.fit(selected.heading() + " · " + selected.objective(), width - 66);
            graphics.text(minecraft.font, Component.literal(compact), x + 8, y + 6, TEXT, true);
            String right = choiceCount > 1 ? (choiceIndex + 1) + "/" + choiceCount + " · K" : "K 상세";
            graphics.text(minecraft.font, Component.literal(right), x + width - 8 - minecraft.font.width(right), y + 6, GOLD, false);
            return;
        }

        String hint = selected.main() ? playerFacingHint(snapshot.dialogue()) : "Shift+K로 표시할 목표를 전환할 수 있습니다.";
        if (selected.main() && isPartyObjective(snapshot.objective())) {
            hint = "E 메뉴 → 파티에서 편성 후 ‘편성 적용’을 누르세요.";
        }

        int maxPanelHeight = Math.max(54, graphics.guiHeight() - 14);
        int maxTextLines = Math.max(4, (maxPanelHeight - 38) / 9);
        List<String> objectiveLines = wrap(minecraft, selected.objective(), width - 20, Math.min(5, maxTextLines));
        int remainingLines = Math.max(1, maxTextLines - objectiveLines.size());
        List<String> hintLines = hint.isBlank() ? List.of() : wrap(minecraft, hint, width - 20, Math.min(4, remainingLines));
        int height = 32 + objectiveLines.size() * 10 + (hintLines.isEmpty() ? 0 : 7 + hintLines.size() * 9);
        height = Math.min(maxPanelHeight, Math.max(48, height));

        TurnboundUiSkin.panel(graphics, x, y, width, height);
        String heading = selected.heading() + (choiceCount > 1 ? "  " + (choiceIndex + 1) + "/" + choiceCount : "");
        graphics.text(minecraft.font, Component.literal(UiTextLayout.fit(heading, width - 92)), x + 10, y + 8, GOLD, true);
        String action = choiceCount > 1 ? "⇧K 전환" : "K 접기";
        graphics.text(minecraft.font, Component.literal(action), x + width - 10 - minecraft.font.width(action), y + 8, MUTED, false);

        int ty = y + 22;
        for (String line : objectiveLines) {
            graphics.text(minecraft.font, Component.literal(line), x + 10, ty, TEXT, true);
            ty += 10;
        }
        if (!hintLines.isEmpty()) {
            ty += 2;
            for (String line : hintLines) {
                if (ty + 8 >= y + height) break;
                graphics.text(minecraft.font, Component.literal(line), x + 10, ty, MUTED, false);
                ty += 9;
            }
        }
        if (selected.main() && snapshot.patrolGoal() > 0 && snapshot.patrolsCleared() < snapshot.patrolGoal()) {
            String progress = snapshot.patrolsCleared() + "/" + snapshot.patrolGoal();
            graphics.text(minecraft.font, Component.literal(progress), x + width - minecraft.font.width(progress) - 10, y + 22, GREEN, true);
        }
    }

    private static DisplayObjective displayObjective(FieldUiSnapshot snapshot) {
        if (!"MAIN".equals(selectedObjectiveId)) {
            for (FieldUiSnapshot.QuestTracker quest : snapshot.questTrackers()) {
                if (!quest.id().equals(selectedObjectiveId)) continue;
                String category = quest.repeatable() ? "지역 의뢰" : quest.category();
                return new DisplayObjective(quest.id(), category + " · " + quest.title(),
                        playerFacingObjective(quest.objective()), false);
            }
            selectedObjectiveId = "MAIN";
        }
        return new DisplayObjective("MAIN", "메인 목표", playerFacingObjective(snapshot.objective()), true);
    }

    private static int objectiveIndex(FieldUiSnapshot snapshot, String id) {
        if ("MAIN".equals(id)) return 0;
        for (int i = 0; i < snapshot.questTrackers().size(); i++) {
            if (snapshot.questTrackers().get(i).id().equals(id)) return i + 1;
        }
        return 0;
    }

    private static void drawDirectionCue(GuiGraphicsExtractor graphics, Minecraft minecraft, Target target) {
        double dx = target.x - minecraft.player.getX(), dz = target.z - minecraft.player.getZ();
        int distance = (int) Math.round(Math.hypot(dx, dz));
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double delta = wrapDegrees(targetYaw - minecraft.player.getYRot());
        String arrow = directionArrow(delta);
        String text = arrow + "  " + target.label + " · " + distance + "m";
        int maxW = Math.min(220, graphics.guiWidth() / 2);
        text = UiTextLayout.fit(text, maxW - 16);
        int w = minecraft.font.width(text) + 18;
        int x = (graphics.guiWidth() - w) / 2, y = 7;
        TurnboundUiSkin.inset(graphics, x, y, w, 20);
        graphics.text(minecraft.font, Component.literal(text), x + 9, y + 6, GOLD, true);
    }

    private static String directionArrow(double delta) {
        if (delta >= -22.5 && delta < 22.5) return "↑";
        if (delta >= 22.5 && delta < 67.5) return "↗";
        if (delta >= 67.5 && delta < 112.5) return "→";
        if (delta >= 112.5 && delta < 157.5) return "↘";
        if (delta >= 157.5 || delta < -157.5) return "↓";
        if (delta >= -157.5 && delta < -112.5) return "↙";
        if (delta >= -112.5 && delta < -67.5) return "←";
        return "↖";
    }

    private static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) value -= 360.0;
        if (value < -180.0) value += 360.0;
        return value;
    }

    private static String targetLine(Minecraft minecraft, Target target) {
        int distance = (int) Math.round(Math.hypot(target.x - minecraft.player.getX(), target.z - minecraft.player.getZ()));
        return "◆ " + target.label + " · " + distance + "m";
    }

    private static boolean isPartyObjective(String raw) {
        return raw != null && (raw.contains("첫 파티") || raw.contains("편성 확인") || raw.contains("P01/P03/P04/F03"));
    }

    private static Target target(FieldUiSnapshot snapshot) {
        FieldUiSnapshot.Navigation navigation = snapshot.navigation();
        if (navigation == null || !navigation.active()) return null;
        return new Target(navigation.label(), navigation.x(), navigation.z());
    }

    static String playerFacingObjective(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = stripLeadingInternalQuestId(raw.trim());
        return text.replace("카이렌/브람/엘리시아/변경 사냥꾼", "카이렌 · 변경 사냥꾼")
                .replace("P01/P03/P04/F03", "카이렌 · 변경 사냥꾼")
                .replace("P01/F03", "카이렌 · 변경 사냥꾼")
                .replace("ENC_M01/M02 승리", "초입 순찰 2개 격파")
                .replace("ENC_M04의 E003 전투에서 승리", "심부의 불안정 폭발체 격파")
                .replace("B01 그라울", "들이받는 왕 그라울")
                .replace("B01을 격파", "그라울 격파")
                .replace("B02 베르나", "가시어미 베르나")
                .replace("B03 ORO-7", "수문관리기 ORO-7")
                .replace("B04 콜바크", "재의 거상 콜바크")
                .replace("B05 세라크", "균열감시자 세라크")
                .replace("Relay fragment", "Relay 조각")
                .replace("Relay console", "Relay 제어 콘솔");
    }

    static String playerFacingHint(String raw) {
        if (raw == null) return "";
        return raw.replace("Relay fragment", "Relay 조각")
                .replace("Relay console", "Relay 제어 콘솔")
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
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) line = UiTextLayout.fit(line + " " + remaining, maxWidth);
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private record Target(String label, double x, double z) {}
    private record DisplayObjective(String id, String heading, String objective, boolean main) {}
}
