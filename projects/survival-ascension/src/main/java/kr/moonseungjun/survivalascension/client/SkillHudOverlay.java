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
        renderFractureShrineTracker(graphics, minecraft);
        renderMobilityCooldown(graphics, minecraft);
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

    private static void renderMobilityCooldown(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        int remaining = ClientMobilityState.remainingTicks();
        if (remaining <= 0) return;
        final int width = 104;
        final int height = 13;
        int preferredLeft = graphics.guiWidth() / 2 + 98;
        int left = Math.max(6, Math.min(graphics.guiWidth() - width - 6, preferredLeft));
        int top = Math.max(6, graphics.guiHeight() - 43);
        float progress = ClientMobilityState.readyProgress();
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xB05C5570);
        graphics.fill(left, top, left + width, top + height, 0xD414171B);
        int fill = Math.round(width * progress);
        if (fill > 0) graphics.fill(left, top + height - 2, left + fill, top + height, 0xFFD8B4FF);
        String label = String.format(java.util.Locale.ROOT, "V 돌진  %.1fs", remaining / 20.0F);
        int textX = left + (width - minecraft.font.width(label)) / 2;
        graphics.text(minecraft.font, label, textX, top + 2, 0xFFF0E6FF, true);
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
        String label = "신화 III  " + arrow + "  약 " + distance + "m";
        int width = Math.max(118, minecraft.font.width(label) + 16);
        int top = 8;
        int rightMargin = 8;
        int preferredLeft = graphics.guiWidth() - width - rightMargin;
        int left;
        if (graphics.guiWidth() >= 420) {
            // Vanilla/NeoForge boss bars own the top-center lane. Keep the directional tracker
            // in the upper-right on normal desktop GUI widths so multiple Mythics cannot cover it.
            left = Math.max(6, preferredLeft);
        } else {
            // On narrow GUI scales there is not enough horizontal separation from a boss bar.
            // Fall below the normal multi-boss stack instead of fighting for the center lane.
            left = Math.max(6, (graphics.guiWidth() - width) / 2);
            top = 78;
        }
        int border = distance <= 48 ? 0xFFE6A23C : 0xC0906A28;
        graphics.fill(left - 1, top - 1, left + width + 1, top + 14, border);
        graphics.fill(left, top, left + width, top + 13, 0xD4141110);
        int textX = left + (width - minecraft.font.width(label)) / 2;
        graphics.text(minecraft.font, label, textX, top + 2, 0xFFFFD166, true);
    }

    private static void renderFractureShrineTracker(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        ClientFractureShrineState.Target target = ClientFractureShrineState.current();
        if (target == null || minecraft.player == null) return;
        double dx = target.x() - minecraft.player.getX(), dz = target.z() - minecraft.player.getZ();
        int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
        double relative = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - minecraft.player.getYRot());
        String label = "균열 성소" + (target.exact() ? "" : " 예상") + "  " + relativeArrow(relative) + "  약 " + distance + "m";
        int width = Math.max(132, minecraft.font.width(label) + 16), left, top;
        boolean mythic = ClientMythicState.current() != null;
        if (graphics.guiWidth() >= 420) { left = Math.max(6, graphics.guiWidth() - width - 8); top = mythic ? 27 : 8; }
        else { left = Math.max(6, (graphics.guiWidth() - width) / 2); top = mythic ? 97 : 78; }
        graphics.fill(left - 1, top - 1, left + width + 1, top + 14, target.exact() ? 0xFF6BA8A0 : 0xC0607774);
        graphics.fill(left, top, left + width, top + 13, 0xD4101717);
        graphics.text(minecraft.font, label, left + (width - minecraft.font.width(label)) / 2, top + 2, 0xFFE2FFF8, true);
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
