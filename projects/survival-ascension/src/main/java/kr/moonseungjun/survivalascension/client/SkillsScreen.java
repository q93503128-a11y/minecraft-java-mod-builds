package kr.moonseungjun.survivalascension.client;

/* Multi-skill screen information architecture adapted from Skill Proficiencies, Copyright (c) 2026 balovich-matje, MIT. */

import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SkillsScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int ROW_HEIGHT = 31;
    private static final int PANEL_HEIGHT = 222;

    public SkillsScreen() {
        super(Component.literal("Survival Ascension · 숙련"));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = Math.max(10, (this.height - PANEL_HEIGHT) / 2);
        graphics.fill(left - 8, top - 8, left + PANEL_WIDTH + 8, top + PANEL_HEIGHT, 0xE0181818);
        graphics.text(this.font, this.title, left, top, 0xFFFFFFFF, true);
        graphics.text(this.font, "K 숙련 화면 · 웅크리기 = 작업 정밀 모드", left, top + 13, 0xFFAAAAAA, false);
        int y = top + 29;
        for (SkillType skill : SkillType.values()) {
            renderSkillRow(graphics, skill, left, y);
            y += ROW_HEIGHT;
        }
    }

    private void renderSkillRow(GuiGraphicsExtractor graphics, SkillType skill, int left, int top) {
        long totalXp = ClientSkillState.xp(skill);
        int level = ClientSkillState.level(skill);
        long needed = SkillTuning.xpForNextLevel(level);
        long into = SkillTuning.xpIntoLevel(totalXp);
        float progress = level >= SkillTuning.MAX_LEVEL || needed <= 0L ? 1.0F : Math.min(1.0F, into / (float) needed);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 28, 0xC0282828);
        graphics.fill(left, top, left + 4, top + 28, 0xFF000000 | skill.color());
        String heading = skill.koreanName() + "  Lv." + level + "  ·  숙련 등급 " + roman(SkillTuning.masteryTier(level));
        graphics.text(this.font, heading, left + 10, top + 4, 0xFFFFFFFF, false);
        graphics.text(this.font, detail(skill, level), left + 10, top + 14, isActive(skill) ? 0xFFD8D8D8 : 0xFF8A8A8A, false);
        int barLeft = left + 10, barTop = top + 24, barWidth = PANEL_WIDTH - 20;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, 0xFF151515);
        int fill = Math.round(barWidth * progress);
        if (fill > 0) graphics.fill(barLeft, barTop, barLeft + fill, barTop + 3, 0xFF000000 | skill.color());
    }

    private static boolean isActive(SkillType skill) {
        return skill == SkillType.MINING || skill == SkillType.WOODCUTTING || skill == SkillType.HARVESTING || skill == SkillType.COMBAT;
    }

    private static String detail(SkillType skill, int level) {
        return switch (skill) {
            case MINING -> "굴착 " + SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level)
                    + " · 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : "최대 " + SkillTuning.miningVeinLimit(level))
                    + " · 속도 " + format(SkillTuning.miningSpeedMultiplier(level));
            case WOODCUTTING -> "연쇄 로그 " + SkillTuning.woodcuttingLogLimit(level) + " · 속도 " + format(SkillTuning.woodcuttingSpeedMultiplier(level));
            case HARVESTING -> "성숙 작물 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level)
                    + " · 속도 " + format(SkillTuning.harvestingSpeedMultiplier(level));
            case COMBAT -> "피해 " + format(SkillTuning.combatDamageMultiplier(level))
                    + " · 파급 " + cleaveText(level);
            case CONSTRUCTION -> "준비 중 · 건축 체급 성장 시스템 예약";
            case MOBILITY -> "준비 중 · 이동 체급 성장 시스템 예약";
        };
    }

    private static String cleaveText(int level) {
        int targets = SkillTuning.combatCleaveTargetLimit(level);
        if (targets <= 0) return "잠김";
        return targets + "체 / 반경 " + String.format(Locale.ROOT, "%.1f", SkillTuning.combatCleaveRadius(level));
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.2f×", value); }
    private static String roman(int tier) {
        return switch (tier) { case 5 -> "V"; case 4 -> "IV"; case 3 -> "III"; case 2 -> "II"; default -> "I"; };
    }
}
