package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Focused readable detail surface for discovered codex entries. */
public final class CodexDetailScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int GOLD = 0xFFFFC857;
    private static final int BLUE = 0xFF6DC6FF;
    private final Screen previous;
    private final ClientMetaState.CodexRow row;
    private BattleHudButton backButton;

    public CodexDetailScreen(Screen previous, ClientMetaState.CodexRow row) {
        super(Component.literal(row == null ? "도감" : row.name()));
        this.previous = previous;
        this.row = row;
    }

    @Override
    protected void init() {
        super.init();
        backButton = addRenderableWidget(new BattleHudButton(width / 2 - 54, height - 36, 108, 22,
                Component.literal("돌아가기"), MUTED, ignored -> closeToPrevious()));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD80A0D12);
        if (row == null) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int panelW = Math.min(570, width - 42);
        int panelH = Math.min(330, height - 74);
        int x = (width - panelW) / 2;
        int y = Math.max(24, (height - panelH) / 2 - 6);
        int accent = categoryColor(row.category());
        TurnboundFrameStyle.frame(graphics, x, y, panelW, panelH, accent);

        graphics.text(font, Component.literal(categoryLabel(row.category())), x + 18, y + 16, SECONDARY, false);
        graphics.text(font, Component.literal(row.name()), x + 18, y + 36, TEXT, true);
        String status = row.discovered() ? (row.detailUnlocked() ? "발견 완료" : "발견됨 · 상세 정보 잠김") : "미발견";
        graphics.text(font, Component.literal(status), x + panelW - 18 - font.width(status), y + 18,
                row.detailUnlocked() ? GOLD : MUTED, false);

        int bodyX = x + 18;
        int bodyY = y + 66;
        int bodyW = panelW - 36;
        TurnboundUiSkin.inset(graphics, bodyX, bodyY, bodyW, panelH - 86);
        int ty = bodyY + 13;
        if (!row.discovered()) {
            graphics.text(font, Component.literal("이 항목은 아직 발견되지 않았습니다."), bodyX + 12, ty, MUTED, false);
        } else if (!row.detailUnlocked()) {
            graphics.text(font, Component.literal("기록은 확보했지만 자세한 분석은 아직 해금되지 않았습니다."), bodyX + 12, ty, SECONDARY, false);
            ty += 20;
            drawWrapped(graphics, row.summary(), bodyX + 12, ty, bodyW - 24, MUTED, 4);
        } else {
            ty = drawDetail(graphics, bodyX + 12, ty, bodyW - 24);
        }

        graphics.text(font, Component.literal("ESC / E · 돌아가기"), x + 18, y + panelH - 18, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int drawDetail(GuiGraphicsExtractor graphics, int x, int y, int maxWidth) {
        if ("ENEMIES".equals(row.category()) || "BOSSES".equals(row.category())) {
            try {
                var definition = CanonicalData.definition(row.id());
                var stats = definition.stats();
                graphics.text(font, Component.literal("HP " + stats.maxHp() + "   ATK " + stats.attack()
                        + "   DEF " + stats.defense() + "   SPD " + stats.speed()), x, y, GOLD, true);
                y += 18;
                if (!row.summary().isBlank()) {
                    graphics.text(font, Component.literal("특징"), x, y, SECONDARY, false);
                    y += 14;
                    y = drawWrapped(graphics, row.summary(), x, y, maxWidth, TEXT, 3);
                    y += 6;
                }
                List<String> skills = definition.skills().stream().map(skill -> skill.name()).distinct().limit(4).toList();
                if (!skills.isEmpty()) {
                    graphics.text(font, Component.literal("기술"), x, y, SECONDARY, false);
                    y += 14;
                    y = drawWrapped(graphics, String.join(" · ", skills), x, y, maxWidth, TEXT, 4);
                }
                return y;
            } catch (RuntimeException ignored) { }
        }

        if ("EQUIPMENT".equals(row.category())) {
            try {
                var spec = V04Catalogs.equipment(row.id());
                graphics.text(font, Component.literal(tierLabel(spec.tier()) + " · " + slotLabel(spec.slot())), x, y, GOLD, true);
                y += 18;
                graphics.text(font, Component.literal("주 능력치  " + statLabel(spec.main())), x, y, TEXT, false);
                y += 15;
                graphics.text(font, Component.literal("보조 능력치  " + statLabel(spec.sub())), x, y, TEXT, false);
                y += 18;
                String effect = equipmentRuleLabel(spec.fixedEffect());
                graphics.text(font, Component.literal("고유 효과"), x, y, SECONDARY, false);
                y += 14;
                return drawWrapped(graphics, effect.isBlank() ? "고유 효과 없음" : effect, x, y, maxWidth, TEXT, 3);
            } catch (RuntimeException ignored) {
                try {
                    var spec = V04Catalogs.signature(row.id());
                    graphics.text(font, Component.literal("전용 장비 · " + ownerName(spec.owner())), x, y, GOLD, true);
                    y += 18;
                    graphics.text(font, Component.literal("주 능력치  " + statLabel(spec.main())), x, y, TEXT, false);
                    y += 15;
                    graphics.text(font, Component.literal("보조 능력치  " + statLabel(spec.sub())), x, y, TEXT, false);
                    y += 18;
                    graphics.text(font, Component.literal("강화 효과"), x, y, SECONDARY, false);
                    y += 14;
                    List<String> effects = List.of(
                            signatureRuleLabel(spec.baseRule()),
                            "+5 · " + signatureRuleLabel(spec.milestone5()),
                            "+10 · " + signatureRuleLabel(spec.milestone10()));
                    return drawWrapped(graphics, String.join("   ", effects), x, y, maxWidth, TEXT, 5);
                } catch (RuntimeException ignoredAgain) { }
            }
        }

        return drawWrapped(graphics, row.summary().isBlank() ? "추가 기록이 없습니다." : row.summary(),
                x, y, maxWidth, TEXT, 6);
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color, int maxLines) {
        for (String line : wrap(text, maxWidth, maxLines)) {
            graphics.text(font, Component.literal(line), x, y, color, false);
            y += 12;
        }
        return y;
    }

    private List<String> wrap(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.length();
            while (cut > 1 && font.width(remaining.substring(0, cut)) > maxWidth) cut--;
            int space = remaining.lastIndexOf(' ', cut);
            if (space > Math.max(0, cut - 14)) cut = space;
            String line = remaining.substring(0, Math.max(1, cut)).stripTrailing();
            remaining = remaining.substring(Math.max(1, cut)).stripLeading();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) line = UiTextLayout.fit(line + " " + remaining, maxWidth);
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private static String statLabel(V04Catalogs.Stat stat) {
        return statTypeLabel(stat.type()) + " " + stat(stat.value());
    }

    private static String stat(double value) {
        return Math.abs(value) <= 1.0
                ? String.format(Locale.ROOT, "%.1f%%", value * 100.0)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String statTypeLabel(String type) {
        return switch (type) {
            case "HP_FLAT" -> "HP";
            case "HP_PERCENT", "HP_PCT" -> "HP%";
            case "ATK_FLAT" -> "ATK";
            case "ATK_PERCENT", "ATK_PCT" -> "ATK%";
            case "DEF_FLAT" -> "DEF";
            case "DEF_PERCENT", "DEF_PCT" -> "DEF%";
            case "SPD_FLAT" -> "SPD";
            case "SPD_PERCENT", "SPD_PCT" -> "SPD%";
            default -> "능력치";
        };
    }

    private static String equipmentRuleLabel(String rule) {
        if (rule == null || rule.isBlank()) return "";
        if (rule.startsWith("START_GAUGE_")) return "전투 시작 시 행동 게이지 +" + suffix(rule, "START_GAUGE_");
        if (rule.startsWith("DIRECT_HIT_GAUGE_")) return "직접 피격 시 행동 게이지 +" + suffix(rule, "DIRECT_HIT_GAUGE_");
        if (rule.startsWith("ALLY_GAUGE_GRANT_PLUS_")) return "아군 행동 게이지 부여량 +" + suffix(rule, "ALLY_GAUGE_GRANT_PLUS_");
        if (rule.startsWith("SINGLE_DIRECT_DAMAGE_")) return "단일 직접 피해 +" + suffix(rule, "SINGLE_DIRECT_DAMAGE_") + "%";
        if (rule.startsWith("EXECUTE_DIRECT_DAMAGE_")) return "처형 조건 직접 피해 +" + suffix(rule, "EXECUTE_DIRECT_DAMAGE_") + "%";
        if (rule.startsWith("HEAL_DONE_")) return "주는 회복량 +" + suffix(rule, "HEAL_DONE_") + "%";
        if (rule.startsWith("HEAL_RECEIVED_")) return "받는 회복량 +" + suffix(rule, "HEAL_RECEIVED_") + "%";
        if (rule.startsWith("BARRIER_RECEIVED_")) return "받는 장벽량 +" + suffix(rule, "BARRIER_RECEIVED_") + "%";
        if (rule.startsWith("REVIVE_HP_PLUS_")) return "부활 시 추가 HP +" + suffix(rule, "REVIVE_HP_PLUS_") + "%p";
        if (rule.startsWith("REACTION_DAMAGE_")) return "반응 공격 피해 +" + suffix(rule, "REACTION_DAMAGE_") + "%";
        if (rule.startsWith("HIGH_HP_DR_")) return "HP 80% 이상일 때 받는 피해 감소 " + suffix(rule, "HIGH_HP_DR_") + "%";
        return "고유 전투 효과";
    }

    private static String signatureRuleLabel(String rule) {
        if (rule == null || rule.isBlank()) return "효과 없음";
        return switch (rule) {
            case "SIG_P01_FOCUS3_ACTIVE_GAUGE_100" -> "집중 3 액티브 사용 시 행동 게이지 +100";
            case "SIG_P01_FOCUS_KILL_CARRY_2" -> "집중 3 대상 처치 후 다음 대상에 집중 2 계승";
            case "SIG_P01_BREAKER_FOLLOWUP_PLUS_25" -> "집중 3 파쇄 추가타 위력 +25%p";
            case "SIG_P02_BASIC_SELF_GAUGE_60" -> "가속 사용 시 자신 행동 게이지 +60";
            case "SIG_P02_TIME_LEAP_SPEED_20" -> "시간 도약 대상 SPD +20%";
            case "SIG_P02_TIME_LEAP_ECHO_120" -> "시간 도약 시 다른 아군 행동 게이지 +120";
            case "SIG_P03_REDIRECT_DR_20" -> "피해 대신 받기 시 받는 피해 20% 감소";
            case "SIG_P03_COUNTER_PLUS_20" -> "반격 피해 +20%";
            case "SIG_P03_REDIRECT_TARGET_GAUGE_100" -> "보호 대상 행동 게이지 +100";
            case "SIG_P04_OVERHEAL_BARRIER_60" -> "초과 회복량의 60%를 장벽으로 전환";
            case "SIG_P04_REVIVE_AEGIS" -> "부활 대상에게 보호 효과 부여";
            case "SIG_P04_SANCTUARY_GAUGE_120" -> "성역 효과 발동 시 행동 게이지 +120";
            case "SIG_P05_FIRST_FOLLOWUP_PLUS_25" -> "첫 추가타 피해 +25%";
            case "SIG_P05_FOLLOWUP_LIMIT_2" -> "추가타 발동 한도 증가";
            case "SIG_P05_SECOND_FOLLOWUP_GAUGE_160" -> "두 번째 추가타 후 행동 게이지 +160";
            case "SIG_P06_RECORD_HEAL_4" -> "기록 누적 시 회복 강화";
            case "SIG_P06_RECORD5_DAMAGE_20" -> "기록 5중첩에서 피해 +20%";
            case "SIG_P06_RETURN_CONDOLENCE_RESET" -> "귀환 효과로 핵심 기술 회전 보조";
            case "SIG_P07_PARTNER_VITALITY" -> "소환 동료의 생존력 강화";
            case "SIG_P07_COMMAND_EMPOWER" -> "지휘 기술 강화";
            case "SIG_P07_SUMMON_DEATH_WARD" -> "소환 동료에게 치명상 방지 효과";
            case "SIG_P08_LOW_HP_SPEED_15" -> "저체력에서 SPD +15%";
            case "SIG_P08_LOW_HP_BASIC_PLUS_30" -> "저체력에서 기본 공격 피해 +30%";
            case "SIG_P08_BLOOD_RUSH" -> "저체력 돌입 시 공격 템포 강화";
            default -> "전용 장비 고유 효과";
        };
    }

    private static String suffix(String rule, String prefix) { return rule.substring(prefix.length()); }

    private static String ownerName(String id) {
        try { return CanonicalData.definition(id).name(); }
        catch (RuntimeException ignored) { return "전용 캐릭터"; }
    }

    private static String categoryLabel(String category) {
        return switch (category) {
            case "ENEMIES" -> "적 도감";
            case "BOSSES" -> "보스 도감";
            case "EQUIPMENT" -> "장비 도감";
            default -> "도감";
        };
    }

    private static int categoryColor(String category) {
        return switch (category) {
            case "BOSSES" -> GOLD;
            case "EQUIPMENT" -> BLUE;
            default -> 0xFFE46D6D;
        };
    }

    private static String tierLabel(String tier) {
        return switch (tier) {
            case "T4" -> "최상급";
            case "T3" -> "고급";
            case "T2" -> "중급";
            default -> "기본";
        };
    }

    private static String slotLabel(String slot) {
        return switch (slot) {
            case "WEAPON" -> "무기";
            case "ARMOR" -> "방어구";
            case "ACCESSORY" -> "장신구";
            case "SIGNATURE" -> "전용 장비";
            default -> "장비";
        };
    }

    private void closeToPrevious() {
        if (minecraft != null) minecraft.gui.setScreen(previous);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_E) {
            closeToPrevious();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { closeToPrevious(); }

    @Override public boolean isPauseScreen() { return false; }
}
