#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# Version
props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.10-alpha.1", "mod_version=0.17.11-alpha.1", "version")
write(props, text)


# Cooldown inspection APIs and compact HUD slot text.
path = JAVA / "VillageRoleSkillSystem.java"
text = read(path)
anchor = '''    public static String loadoutSummary(ServerPlayer player) {
        String first = equippedSkill(player, 0).map(ActiveSkill::displayName).orElse("비어 있음");
        String second = equippedSkill(player, 1).map(ActiveSkill::displayName).orElse("비어 있음");
        return "Z: " + first + " | X: " + second;
    }

'''
insert = '''    public static String loadoutSummary(ServerPlayer player) {
        String first = equippedSkill(player, 0).map(ActiveSkill::displayName).orElse("비어 있음");
        String second = equippedSkill(player, 1).map(ActiveSkill::displayName).orElse("비어 있음");
        return "Z: " + first + " | X: " + second;
    }

    public static synchronized int cooldownRemainingSeconds(ServerPlayer player, int slot) {
        if (player == null || VillageSkillTestSystem.isEnabled(player)) return 0;
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) return 0;
        long remaining = READY_AT.getOrDefault(player.getUUID() + "|" + skill.id(), 0L)
                - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) Math.max(1L, (remaining + 999L) / 1000L);
    }

    public static synchronized float cooldownProgress(ServerPlayer player, int slot) {
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null || VillageSkillTestSystem.isEnabled(player)) return 0.0f;
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return 0.0f;
        int total = effectiveCooldownSeconds(player, role, skill);
        int remaining = cooldownRemainingSeconds(player, slot);
        return total <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, remaining / (float) total));
    }

    public static String hudSlotText(ServerPlayer player, int slot) {
        String key = slot == 0 ? "§bZ" : "§dX";
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) return key + " §8비어 있음";
        int remaining = cooldownRemainingSeconds(player, slot);
        String state = remaining > 0 ? "§c" + remaining + "초" : "§a준비";
        return key + " §f" + skill.displayName() + " " + state;
    }

'''
text = replace_once(text, anchor, insert, "skill cooldown HUD API")
old_cooldown = '''        int cooldown = Math.max(7,
                skill.baseCooldownSeconds()
                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role));
'''
new_cooldown = '''        int cooldown = effectiveCooldownSeconds(player, role, skill);
'''
text = replace_once(text, old_cooldown, new_cooldown, "shared effective cooldown")
helper_anchor = '''    public static String useTestSkill(ServerPlayer player, String skillId) {
'''
helper = '''    private static int effectiveCooldownSeconds(
            ServerPlayer player, VillageRole role, ActiveSkill skill) {
        return Math.max(7,
                skill.baseCooldownSeconds()
                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role));
    }

    public static String useTestSkill(ServerPlayer player, String skillId) {
'''
text = replace_once(text, helper_anchor, helper, "effective cooldown helper")
write(path, text)


# Put equipped skills and live cooldowns in the existing action-bar HUD.
path = JAVA / "VillageHudSystem.java"
text = read(path)
old = '''        return "§6" + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " §8│ §bLv." + progress.level() + " §7" + xp + " XP"
                + " §8│ §f" + role
                + " §8│ §e주화 " + VillageProgressionSystem.coins(player)
                + " §8│ §6보급품 " + VillageProgressionSystem.supplies();
'''
new = '''        String skillHud = VillageRoleSkillSystem.hudSlotText(player, 0)
                + " §8· " + VillageRoleSkillSystem.hudSlotText(player, 1);
        return "§6" + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " §8│ §bLv." + progress.level() + " §7" + xp
                + " §8│ §f" + role
                + " §8│ " + skillHud
                + " §8│ §e" + VillageProgressionSystem.coins(player) + "주화"
                + " §8· §6" + VillageProgressionSystem.supplies() + "보급";
'''
text = replace_once(text, old, new, "skill cooldown actionbar HUD")
write(path, text)


# Route both test management payloads to a dedicated dark compact screen.
path = JAVA / "VillageClientUi.java"
text = read(path)
old = '''                            case "wave_intel", "skill_test", "game_over" -> new VillageFacilityScreen(payload);
'''
new = '''                            case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload);
                            case "wave_intel", "skill_test", "game_over" -> new VillageFacilityScreen(payload);
'''
text = replace_once(text, old, new, "dedicated skill test screen route")
write(path, text)


# Dedicated test UI: no oversized facility detail panel, no duplicated Z/X cards.
screen = r'''package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compact, test-arena-specific role and loadout manager. */
public final class VillageSkillTestScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int BG = 0xB8000000;
    private static final int PANEL = 0xF20B1218;
    private static final int HEADER = 0xFF101B23;
    private static final int SURFACE = 0xFF17242D;
    private static final int SURFACE_HOVER = 0xFF20323D;
    private static final int BORDER = 0xFF354A57;
    private static final int TEXT = 0xFFE5EDF1;
    private static final int MUTED = 0xFF91A0A9;
    private static final int TEAL = 0xFF46B7A7;
    private static final int GOLD = 0xFFD3AE58;
    private static final int BLUE = 0xFF5C91C7;
    private static final int RED = 0xFFC0646A;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final boolean roleMode;
    private final List<Integer> roleActions = new ArrayList<>();
    private final List<Integer> utilityActions = new ArrayList<>();
    private final List<SkillCard> skills = new ArrayList<>();
    private int scroll;

    public VillageSkillTestScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        this.labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        this.roleMode = "skill_test_role".equals(payload.screenId());
        parseEntries();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(0, 0, width, height, BG);
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.top() + 48, HEADER);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), roleMode ? GOLD : BLUE);

        graphics.text(font, roleMode ? "시험 직업 관리" : "시험 스킬 관리",
                layout.left() + 14, layout.top() + 8, TEXT, false);
        graphics.text(font, fit(summaryLine(), layout.width() - 76),
                layout.left() + 14, layout.top() + 25, MUTED, false);
        drawButton(graphics, mouseX, mouseY, layout.right() - 29, layout.top() + 8,
                20, 20, "×", RED, false);

        if (roleMode) renderRoles(graphics, mouseX, mouseY, layout);
        else renderSkills(graphics, mouseX, mouseY, layout);
        renderUtilities(graphics, mouseX, mouseY, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRoles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Content content = content(layout);
        int columns = content.width() >= 520 ? 2 : 1;
        int gap = 7;
        int cardWidth = Math.max(130, (content.width() - gap * (columns - 1)) / columns);
        int cardHeight = 54;
        int rows = (roleActions.size() + columns - 1) / columns;
        int contentHeight = rows * cardHeight + Math.max(0, rows - 1) * gap;
        scroll = clamp(scroll, 0, Math.max(0, contentHeight - content.height()));

        graphics.enableScissor(content.left(), content.top(), content.right(), content.bottom());
        for (int i = 0; i < roleActions.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = content.left() + column * (cardWidth + gap);
            int y = content.top() + row * (cardHeight + gap) - scroll;
            if (y + cardHeight < content.top() || y > content.bottom()) continue;
            int index = roleActions.get(i);
            String[] parts = labelParts(labels[index]);
            boolean selected = parts[0].startsWith("선택됨 · ");
            String title = parts[0].replace("선택됨 · ", "");
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
            int accent = selected ? GOLD : TEAL;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + cardHeight + 1,
                    selected ? GOLD : hovered ? TEAL : BORDER);
            graphics.fill(x, y, x + cardWidth, y + cardHeight,
                    hovered || selected ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 4, y + cardHeight, accent);
            graphics.text(font, fit(title, cardWidth - 24), x + 10, y + 8,
                    selected ? GOLD : TEXT, false);
            List<FormattedCharSequence> lines = font.split(Component.literal(firstParagraph(parts[1])),
                    Math.max(70, cardWidth - 20));
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                graphics.text(font, lines.get(line), x + 10, y + 24 + line * 11, MUTED, false);
            }
            if (selected) graphics.text(font, "현재", x + cardWidth - 29, y + 8, GOLD, false);
        }
        graphics.disableScissor();
    }

    private void renderSkills(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Content content = content(layout);
        int columns = content.width() >= 570 ? 2 : 1;
        int gap = 8;
        int cardWidth = Math.max(150, (content.width() - gap * (columns - 1)) / columns);
        int cardHeight = 76;
        int rows = (skills.size() + columns - 1) / columns;
        int contentHeight = rows * cardHeight + Math.max(0, rows - 1) * gap;
        scroll = clamp(scroll, 0, Math.max(0, contentHeight - content.height()));

        graphics.enableScissor(content.left(), content.top(), content.right(), content.bottom());
        for (int i = 0; i < skills.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = content.left() + column * (cardWidth + gap);
            int y = content.top() + row * (cardHeight + gap) - scroll;
            if (y + cardHeight < content.top() || y > content.bottom()) continue;
            SkillCard skill = skills.get(i);
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + cardHeight + 1,
                    hovered ? TEAL : BORDER);
            graphics.fill(x, y, x + cardWidth, y + cardHeight,
                    hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(x, y, x + 4, y + cardHeight, TEAL);
            graphics.text(font, fit(skill.name(), cardWidth - 22), x + 10, y + 8, TEXT, false);
            List<FormattedCharSequence> lines = font.split(Component.literal(firstParagraph(skill.description())),
                    Math.max(80, cardWidth - 20));
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                graphics.text(font, lines.get(line), x + 10, y + 24 + line * 11, MUTED, false);
            }
            int buttonWidth = Math.max(42, (cardWidth - 27) / 2);
            int buttonY = y + cardHeight - 22;
            drawButton(graphics, mouseX, mouseY, x + 9, buttonY,
                    buttonWidth, 16, "Z 장착", BLUE, skill.zEquipped());
            drawButton(graphics, mouseX, mouseY, x + 14 + buttonWidth, buttonY,
                    buttonWidth, 16, "X 장착", 0xFF8D6DB4, skill.xEquipped());
        }
        graphics.disableScissor();
    }

    private void renderUtilities(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        if (utilityActions.isEmpty()) return;
        int y = layout.bottom() - 31;
        int left = layout.left() + 12;
        int right = layout.right() - 12;
        int gap = 5;
        int count = utilityActions.size();
        int buttonWidth = Math.max(52, (right - left - gap * (count - 1)) / count);
        for (int i = 0; i < count; i++) {
            int index = utilityActions.get(i);
            String title = labelParts(labels[index])[0];
            int color = actions[index].equals("test_exit") ? RED
                    : actions[index].contains("roles") ? GOLD : TEAL;
            drawButton(graphics, mouseX, mouseY,
                    left + i * (buttonWidth + gap), y, buttonWidth, 19,
                    fit(title, buttonWidth - 8), color, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 29, layout.top() + 8, 20, 20)) {
            onClose();
            return true;
        }
        if (roleMode && clickRole(click, layout)) return true;
        if (!roleMode && clickSkill(click, layout)) return true;
        if (clickUtility(click, layout)) return true;
        return super.mouseClicked(click, doubled);
    }

    private boolean clickRole(MouseButtonEvent click, Layout layout) {
        Content content = content(layout);
        int columns = content.width() >= 520 ? 2 : 1;
        int gap = 7;
        int cardWidth = Math.max(130, (content.width() - gap * (columns - 1)) / columns);
        int cardHeight = 54;
        for (int i = 0; i < roleActions.size(); i++) {
            int x = content.left() + (i % columns) * (cardWidth + gap);
            int y = content.top() + (i / columns) * (cardHeight + gap) - scroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, cardHeight)) {
                send(actions[roleActions.get(i)]);
                return true;
            }
        }
        return false;
    }

    private boolean clickSkill(MouseButtonEvent click, Layout layout) {
        Content content = content(layout);
        int columns = content.width() >= 570 ? 2 : 1;
        int gap = 8;
        int cardWidth = Math.max(150, (content.width() - gap * (columns - 1)) / columns);
        int cardHeight = 76;
        for (int i = 0; i < skills.size(); i++) {
            int x = content.left() + (i % columns) * (cardWidth + gap);
            int y = content.top() + (i / columns) * (cardHeight + gap) - scroll;
            int buttonWidth = Math.max(42, (cardWidth - 27) / 2);
            int buttonY = y + cardHeight - 22;
            SkillCard skill = skills.get(i);
            if (inside(click.x(), click.y(), x + 9, buttonY, buttonWidth, 16)) {
                send(skill.zAction());
                onClose();
                return true;
            }
            if (inside(click.x(), click.y(), x + 14 + buttonWidth, buttonY, buttonWidth, 16)) {
                send(skill.xAction());
                onClose();
                return true;
            }
        }
        return false;
    }

    private boolean clickUtility(MouseButtonEvent click, Layout layout) {
        if (utilityActions.isEmpty()) return false;
        int y = layout.bottom() - 31;
        int left = layout.left() + 12;
        int right = layout.right() - 12;
        int gap = 5;
        int count = utilityActions.size();
        int buttonWidth = Math.max(52, (right - left - gap * (count - 1)) / count);
        for (int i = 0; i < count; i++) {
            if (inside(click.x(), click.y(), left + i * (buttonWidth + gap), y, buttonWidth, 19)) {
                send(actions[utilityActions.get(i)]);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 30));
        return true;
    }

    private void parseEntries() {
        Map<String, MutableSkill> grouped = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(actions.length, labels.length); i++) {
            String action = actions[i];
            if (action.startsWith("test_role:")) {
                roleActions.add(i);
            } else if (action.startsWith("test_equip:")) {
                String[] parts = action.split(":", 3);
                if (parts.length != 3) continue;
                MutableSkill entry = grouped.computeIfAbsent(parts[1], ignored -> new MutableSkill());
                String[] label = labelParts(labels[i]);
                entry.name = label[0].replaceFirst("^[ZX] · ", "");
                entry.description = label[1];
                if ("0".equals(parts[2])) entry.zAction = action;
                else entry.xAction = action;
            } else {
                utilityActions.add(i);
            }
        }
        String zName = equippedName("Z:");
        String xName = equippedName("X:");
        for (MutableSkill entry : grouped.values()) {
            skills.add(new SkillCard(entry.name, entry.description,
                    entry.zAction, entry.xAction,
                    entry.name.equals(zName), entry.name.equals(xName)));
        }
    }

    private String equippedName(String prefix) {
        String body = plain(payload.body());
        int index = body.indexOf(prefix);
        if (index < 0) return "";
        int start = index + prefix.length();
        int end = body.indexOf('|', start);
        if (end < 0) end = body.indexOf('\n', start);
        if (end < 0) end = body.length();
        return body.substring(start, end).trim();
    }

    private String summaryLine() {
        String[] lines = plain(payload.body()).split("\n", -1);
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) return lines[i];
        }
        return roleMode ? "시험 전용 직업 선택" : "Z/X 실제 시전용 기술 장착";
    }

    private Content content(Layout layout) {
        return new Content(layout.left() + 12, layout.top() + 57,
                layout.right() - 12, layout.bottom() - 39);
    }

    private Layout layout() {
        int panelWidth = Math.min(720, Math.max(340, width - 28));
        int panelHeight = Math.min(460, Math.max(245, height - 28));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                            int x, int y, int w, int h, String text, int accent, boolean active) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1,
                active ? GOLD : hovered ? accent : BORDER);
        graphics.fill(x, y, x + w, y + h,
                active ? 0xFF3C3422 : hovered ? SURFACE_HOVER : SURFACE);
        graphics.centeredText(font, text, x + w / 2, y + Math.max(3, (h - 8) / 2),
                active ? GOLD : TEXT);
    }

    private void send(String action) {
        if (action == null || action.isBlank()) return;
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private String[] labelParts(String label) {
        String[] raw = label.split("\\|", 2);
        return new String[]{plain(raw.length > 0 ? raw[0] : label),
                plain(raw.length > 1 ? raw[1] : "")};
    }

    private String firstParagraph(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    private String fit(String value, int maxWidth) {
        String text = plain(value);
        if (font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int allowed = Math.max(0, maxWidth - font.width(suffix));
        while (!text.isEmpty() && font.width(text) > allowed) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private String plain(String value) {
        return value == null ? "" : value.replaceAll("§.", "");
    }

    private boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }

    private record Content(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private static final class MutableSkill {
        String name = "기술";
        String description = "";
        String zAction = "";
        String xAction = "";
    }

    private record SkillCard(String name, String description, String zAction, String xAction,
                             boolean zEquipped, boolean xEquipped) {}
}
'''
write(JAVA / "VillageSkillTestScreen.java", screen)


# Regression/feature contract.
test = r'''#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    role = read("VillageRoleSkillSystem.java")
    hud = read("VillageHudSystem.java")
    client = read("VillageClientUi.java")
    screen = read("VillageSkillTestScreen.java")
    visuals = read("VillageSkillVisualSystem.java")

    assert "mod_version=0.17.11-alpha.1" in props
    assert "cooldownRemainingSeconds" in role
    assert "cooldownProgress" in role
    assert "hudSlotText" in role
    assert "effectiveCooldownSeconds" in role
    assert "VillageRoleSkillSystem.hudSlotText(player, 0)" in hud
    assert "VillageRoleSkillSystem.hudSlotText(player, 1)" in hud

    enum_block = role.split("public enum ActiveSkill", 1)[1]
    enum_block = enum_block.split("private final String id", 1)[0]
    skill_names = re.findall(r'^\s{8}([A-Z][A-Z0-9_]+)\("', enum_block, re.MULTILINE)
    assert len(skill_names) == 20, skill_names
    for prefix in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert sum(name.startswith(prefix + "_") for name in skill_names) == 4
    cast_block = role.split("switch (skill)", 1)[1].split("private static List<Mob> damageArea", 1)[0]
    for name in skill_names:
        assert f"case {name}" in cast_block, name
    for role_name in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert f"case {role_name}" in visuals

    assert 'case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload)' in client
    assert "final class VillageSkillTestScreen" in screen
    assert "renderRoles" in screen and "renderSkills" in screen
    assert "Z 장착" in screen and "X 장착" in screen
    assert "onClose();" in screen
    assert "panelWidth = Math.min(720" in screen
    assert "content.width() >= 520 ? 2 : 1" in screen
    assert "content.width() >= 570 ? 2 : 1" in screen

    print("[PASS] All 20 active skills have concrete cast logic and role feedback")
    print("[PASS] Action-bar HUD shows equipped Z/X skills and live cooldown seconds")
    print("[PASS] Test role and skill managers use a dedicated compact responsive UI")


if __name__ == "__main__":
    main()
'''
write(TOOLS / "test_v01711_skill_hud_ui.py", test)

print("Applied Village Guardians v0.17.11 skill HUD and test UI patch")
