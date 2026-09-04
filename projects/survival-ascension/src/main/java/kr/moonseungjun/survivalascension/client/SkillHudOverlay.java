package kr.moonseungjun.survivalascension.client;

/* Recent-skill XP HUD structure adapted from Skill Proficiencies, Copyright (c) 2026 balovich-matje, MIT. */

import kr.moonseungjun.survivalascension.network.SkillUpdatePayload;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.List;

public final class SkillHudOverlay {
    private static final int WIDTH = 188;
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_GAP = 3;
    private static final int PROGRESS_HEIGHT = 2;
    private static final int MAX_VISIBLE = 3;
    private static final int BACKGROUND = 0xC414171B;
    private static final int BORDER = 0xA050555B;
    private static final int LEVEL_UP_BORDER = 0xFFE5B85C;

    private SkillHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        renderMythicTracker(graphics, minecraft);
        List<ClientSkillState.RecentSkillUpdate> updates = ClientSkillState.recentUpdates();
        if (updates.isEmpty()) return;

        updates.sort((a, b) -> {
            int priority = Integer.compare(priority(b), priority(a));
            if (priority != 0) return priority;
            return Long.compare(b.changedAtMillis(), a.changedAtMillis());
        });

        int visible = Math.min(MAX_VISIBLE, updates.size());
        int left = (graphics.guiWidth() - WIDTH) / 2;
        int bottomTop = graphics.guiHeight() - 58 - ROW_HEIGHT;

        for (int i = 0; i < visible; i++) {
            ClientSkillState.RecentSkillUpdate recent = updates.get(i);
            int top = bottomTop - i * (ROW_HEIGHT + ROW_GAP);
            drawRow(graphics, minecraft, recent, left, top);
        }
    }

    private static void renderMythicTracker(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        ClientMythicState.Target target = ClientMythicState.current();
        if (target == null || minecraft.player == null) return;
        double dx = target.x() - minecraft.player.getX();
        double dz = target.z() - minecraft.player.getZ();
        int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = Mth.wrapDegrees(targetYaw - minecraft.player.getYRot());
        String arrow = relativeArrow(relative);
        String label = "신화 III   " + arrow + "   " + distance + "m";
        int width = Math.max(118, minecraft.font.width(label) + 16);
        int left = (graphics.guiWidth() - width) / 2;
        int top = 28;
        int border = distance <= 48 ? 0xFFE6A23C : 0xC0906A28;
        graphics.fill(left - 1, top - 1, left + width + 1, top + 14, border);
        graphics.fill(left, top, left + width, top + 13, 0xD4141110);
        int textX = left + (width - minecraft.font.width(label)) / 2;
        graphics.text(minecraft.font, label, textX, top + 2, 0xFFFFD166, true);
    }

    private static String relativeArrow(double degrees) {
        if (degrees >= -22.5D && degrees < 22.5D) return "↑";
        if (degrees >= 22.5D && degrees < 67.5D) return "↗";
        if (degrees >= 67.5D && degrees < 112.5D) return "→";
        if (degrees >= 112.5D && degrees < 157.5D) return "↘";
        if (degrees >= 157.5D || degrees < -157.5D) return "↓";
        if (degrees >= -157.5D && degrees < -112.5D) return "↙";
        if (degrees >= -112.5D && degrees < -67.5D) return "←";
        return "↖";
    }

    private static int priority(ClientSkillState.RecentSkillUpdate recent) {
        if (recent.levelUp()) return 3;
        SkillType skill = SkillType.fromId(recent.payload().skillId());
        if (skill == SkillType.MOBILITY) return 1;
        return 2;
    }

    private static void drawRow(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            ClientSkillState.RecentSkillUpdate recent,
            int left,
            int top
    ) {
        SkillUpdatePayload update = recent.payload();
        SkillType skill = SkillType.fromId(update.skillId());
        if (skill == null) return;

        long totalXp = ClientSkillState.xp(skill);
        int level = SkillTuning.levelFromXp(totalXp);
        long needed = SkillTuning.xpForNextLevel(level);
        long into = SkillTuning.xpIntoLevel(totalXp);
        float progress = level >= SkillTuning.MAX_LEVEL || needed <= 0L
                ? 1.0F
                : Math.min(1.0F, into / (float) needed);

        int border = recent.levelUp() ? LEVEL_UP_BORDER : BORDER;
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + ROW_HEIGHT + 1, border);
        graphics.fill(left, top, left + WIDTH, top + ROW_HEIGHT, BACKGROUND);
        graphics.fill(left, top, left + 3, top + ROW_HEIGHT, 0xFF000000 | skill.color());

        int progressWidth = Math.round((WIDTH - 3) * progress);
        if (progressWidth > 0) {
            graphics.fill(
                    left + 3,
                    top + ROW_HEIGHT - PROGRESS_HEIGHT,
                    left + 3 + progressWidth,
                    top + ROW_HEIGHT,
                    0xFF000000 | skill.color()
            );
        }

        String leftLabel = skill.koreanName() + "  Lv." + level;
        String rightLabel;
        int rightColor;
        if (recent.levelUp()) {
            rightLabel = "Lv." + recent.fromLevel() + " → " + update.level();
            rightColor = 0xFFFFE082;
        } else {
            rightLabel = "+" + recent.gainedXp() + " XP";
            rightColor = skill == SkillType.MOBILITY ? 0xFFB9B4C7 : 0xFFE7E3DC;
        }

        graphics.text(minecraft.font, leftLabel, left + 8, top + 2, 0xFFFFFFFF, true);
        int rightX = left + WIDTH - 6 - minecraft.font.width(rightLabel);
        graphics.text(minecraft.font, rightLabel, rightX, top + 2, rightColor, false);
    }
}
