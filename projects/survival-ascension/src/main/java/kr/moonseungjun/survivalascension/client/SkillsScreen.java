package kr.moonseungjun.survivalascension.client;

/* Skill list presentation and native-screen navigation are adapted from Skill Proficiencies MIT. */

import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class SkillsScreen extends Screen {
    private static final int ROW_WIDTH = 336;
    private static final int ROW_HEIGHT = 27;
    private static final int LIST_TOP = 44;
    private final Screen parent;

    public SkillsScreen() { this(null); }
    public SkillsScreen(Screen parent) { super(Component.literal("Survival Ascension · 숙련")); this.parent = parent; }

    @Override protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }
    @Override public void onClose() { this.minecraft.gui.setScreen(this.parent); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);
        String subtitle = "M 메뉴 · Shift 정밀 작업 · R 기동 액션";
        graphics.text(this.font, subtitle, (this.width - this.font.width(subtitle)) / 2, 27, 0xFFAAAAAA, false);
        int left = (this.width - ROW_WIDTH) / 2;
        int y = LIST_TOP;
        for (SkillType skill : SkillType.values()) { renderSkillRow(graphics, skill, left, y, mouseX, mouseY); y += ROW_HEIGHT; }
    }

    private void renderSkillRow(GuiGraphicsExtractor graphics, SkillType skill, int left, int top, int mouseX, int mouseY) {
        long totalXp = ClientSkillState.xp(skill);
        int level = ClientSkillState.level(skill);
        long needed = SkillTuning.xpForNextLevel(level);
        long into = SkillTuning.xpIntoLevel(totalXp);
        float progress = level >= SkillTuning.MAX_LEVEL || needed <= 0L ? 1.0F : Math.min(1.0F, into / (float) needed);
        boolean hovered = mouseX >= left && mouseX < left + ROW_WIDTH && mouseY >= top && mouseY < top + ROW_HEIGHT - 2;
        graphics.fill(left, top, left + ROW_WIDTH, top + ROW_HEIGHT - 2, hovered ? 0xC0484848 : 0xA8303030);
        graphics.fill(left, top, left + 3, top + ROW_HEIGHT - 2, 0xFF000000 | skill.color());
        String heading = skill.koreanName() + "  Lv." + level + "  ·  등급 " + roman(SkillTuning.masteryTier(level));
        graphics.text(this.font, heading, left + 9, top + 4, 0xFFFFFFFF, false);
        graphics.text(this.font, detail(skill, level), left + 9, top + 14, 0xFFCECECE, false);
        int barLeft = left + 9, barRight = left + ROW_WIDTH - 9, barTop = top + 23;
        graphics.fill(barLeft, barTop, barRight, barTop + 2, 0xFF151515);
        int fill = Math.round((barRight - barLeft) * progress);
        if (fill > 0) graphics.fill(barLeft, barTop, barLeft + fill, barTop + 2, 0xFF000000 | skill.color());
    }

    private static String detail(SkillType skill, int level) {
        return switch (skill) {
            case MINING -> "굴착 " + SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level)
                    + " · 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : "최대 " + SkillTuning.miningVeinLimit(level))
                    + " · 속도 " + format(SkillTuning.miningSpeedMultiplier(level));
            case WOODCUTTING -> "연쇄 " + SkillTuning.woodcuttingLogLimit(level) + "로그 · 속도 " + format(SkillTuning.woodcuttingSpeedMultiplier(level));
            case HARVESTING -> "수확 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " · 속도 " + format(SkillTuning.harvestingSpeedMultiplier(level));
            case COMBAT -> "피해 " + format(SkillTuning.combatDamageMultiplier(level)) + " · 파급 " + cleaveText(level);
            case CONSTRUCTION -> "선 " + SkillTuning.constructionLineLength(level) + "블록 · 면 " + SkillTuning.constructionPlaneSize(level) + "×" + SkillTuning.constructionPlaneSize(level);
            case MOBILITY -> "이속 " + format(SkillTuning.mobilitySpeedMultiplier(level)) + " · 단차 "
                    + String.format(Locale.ROOT, "%.2f", SkillTuning.mobilityStepHeight(level)) + " · R " + (level < 30 ? "잠김" : "돌진");
        };
    }

    private static String cleaveText(int level) {
        int targets = SkillTuning.combatCleaveTargetLimit(level);
        return targets <= 0 ? "잠김" : targets + "체 / " + String.format(Locale.ROOT, "%.1f블록", SkillTuning.combatCleaveRadius(level));
    }
    private static String format(double value) { return String.format(Locale.ROOT, "%.2f×", value); }
    private static String roman(int tier) { return switch (tier) { case 6 -> "VI"; case 5 -> "V"; case 4 -> "IV"; case 3 -> "III"; case 2 -> "II"; default -> "I"; }; }
}
