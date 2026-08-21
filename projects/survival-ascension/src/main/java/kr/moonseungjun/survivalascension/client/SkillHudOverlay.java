package kr.moonseungjun.survivalascension.client;

/* Recent-skill XP HUD structure adapted from Skill Proficiencies, Copyright (c) 2026 balovich-matje, MIT. */

import kr.moonseungjun.survivalascension.network.SkillUpdatePayload;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SkillHudOverlay {
    private static final int WIDTH = 164;
    private static final int HEIGHT = 7;
    private static final long LINGER_MS = 4000L;
    private SkillHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SkillUpdatePayload update = ClientSkillState.lastUpdate();
        if (update == null || ClientSkillState.updateAgeMillis() > LINGER_MS) return;
        SkillType skill = SkillType.fromId(update.skillId());
        if (skill == null) return;
        long totalXp = ClientSkillState.xp(skill);
        int level = SkillTuning.levelFromXp(totalXp);
        long needed = SkillTuning.xpForNextLevel(level);
        long into = SkillTuning.xpIntoLevel(totalXp);
        float progress = level >= SkillTuning.MAX_LEVEL || needed <= 0L ? 1.0F : Math.min(1.0F, into / (float) needed);
        int left = (graphics.guiWidth() - WIDTH) / 2;
        int top = graphics.guiHeight() - 52;
        graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, 0xB0000000);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFF202020);
        int fill = Math.round(WIDTH * progress);
        if (fill > 0) graphics.fill(left, top, left + fill, top + HEIGHT, 0xFF000000 | skill.color());
        String label = skill.koreanName() + "  Lv." + level;
        int textX = left + (WIDTH - minecraft.font.width(label)) / 2;
        graphics.text(minecraft.font, label, textX, top - 10, 0xFFFFFFFF, true);
        if (update.levelUp() && ClientSkillState.updateAgeMillis() < 2200L) {
            String levelUp = "레벨 상승  " + update.fromLevel() + " → " + update.level();
            int lx = left + (WIDTH - minecraft.font.width(levelUp)) / 2;
            graphics.text(minecraft.font, levelUp, lx, top + HEIGHT + 4, 0xFFFFE082, true);
        }
    }
}
