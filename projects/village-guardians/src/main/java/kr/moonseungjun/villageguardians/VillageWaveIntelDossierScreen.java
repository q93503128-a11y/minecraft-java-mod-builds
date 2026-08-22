package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Next-night tactical briefing with a selectable bestiary dossier for every predicted archetype. */
public final class VillageWaveIntelDossierScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x70070A0D;
    private static final int TEXT = 0xFFF1F4F5;
    private static final int MUTED = 0xFFAAB5BA;
    private static final int CYAN = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int RED = 0xFFE06E64;
    private static final int SURFACE = 0xD1131B1F;
    private static final int SURFACE_2 = 0xE51B282E;
    private static final int LINE = 0xA34B6873;

    private final String body;
    private final List<WaveEntry> waves = new ArrayList<>();
    private int selectedWave;
    private int selectedMonster;

    public VillageWaveIntelDossierScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        parse(payload);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        VillageUiSafeArea.Rect safe = layout.safe();
        graphics.text(font, "전술 브리핑 · 다음 밤 적 정찰", safe.left() + 8, safe.top() + 5, GOLD, false);
        List<FormattedCharSequence> header = font.split(Component.literal(body.replace('\n', ' ')),
                Math.max(90, safe.width() - 20));
        int headerY = safe.top() + 21;
        for (int i = 0; i < Math.min(2, header.size()); i++) {
            graphics.text(font, header.get(i), safe.left() + 8, headerY, TEXT, false);
            headerY += 11;
        }
        graphics.fill(safe.left() + 7, safe.top() + 47, safe.right() - 7, safe.top() + 49, LINE);

        drawWaveList(graphics, layout.waveList(), mouseX, mouseY);
        drawWaveOverview(graphics, layout.overview());
        drawMonsterList(graphics, layout.monsters(), mouseX, mouseY);
        drawDossier(graphics, layout.dossier());
        graphics.text(font, fit(font, "웨이브 선택 → 예상 병과 선택 → 상세 대응 확인 · ESC 닫기", safe.width() - 12),
                safe.left() + 6, safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWaveList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        if (pane.width() <= 2 || pane.height() <= 2) return;
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xB90D1519);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        graphics.text(font, "웨이브", pane.left() + 8, pane.top() + 7, CYAN, false);
        int y = pane.top() + 23;
        for (int i = 0; i < waves.size(); i++) {
            WaveEntry wave = waves.get(i);
            int h = 44;
            if (y + h > pane.bottom() - 4) break;
            boolean hover = inside(mouseX, mouseY, pane.left() + 5, y, pane.width() - 10, h);
            boolean active = selectedWave == i;
            graphics.fill(pane.left() + 5, y, pane.right() - 5, y + h,
                    active || hover ? SURFACE_2 : SURFACE);
            graphics.fill(pane.left() + 5, y, pane.left() + 8, y + h, active ? GOLD : CYAN);
            graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 13, y + 7,
                    active ? GOLD : TEXT, false);
            graphics.text(font, fit(font, "예상 병과 " + wave.roster().size() + "종", pane.width() - 22),
                    pane.left() + 13, y + 25, CYAN, false);
            y += h + 5;
        }
        if (waves.isEmpty()) {
            graphics.centeredText(font, fit(font, "현재 정찰 가능한 웨이브 없음", pane.width() - 12),
                    pane.left() + pane.width() / 2, pane.top() + pane.height() / 2, RED);
        }
        graphics.disableScissor();
    }

    private void drawWaveOverview(GuiGraphicsExtractor graphics, Pane pane) {
        if (pane.width() <= 2 || pane.height() <= 2) return;
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xC5121B20);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        if (waves.isEmpty()) {
            graphics.disableScissor();
            return;
        }
        WaveEntry wave = waves.get(clamp(selectedWave, 0, waves.size() - 1));
        if (pane.height() < 58) {
            graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 11, pane.top() + 5,
                    GOLD, false);
            String compact = wave.overview().replace('\n', ' ').replace("  ", " ");
            graphics.text(font, fit(font, compact, pane.width() - 22), pane.left() + 11, pane.top() + 19,
                    TEXT, false);
            graphics.disableScissor();
            return;
        }
        graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 11, pane.top() + 9,
                GOLD, false);
        int y = pane.top() + 27;
        for (String paragraph : wave.overview().split("\n", -1)) {
            for (FormattedCharSequence line : font.split(Component.literal(paragraph), Math.max(40, pane.width() - 22))) {
                if (y > pane.bottom() - 11) {
                    graphics.disableScissor();
                    return;
                }
                graphics.text(font, line, pane.left() + 11, y, paragraph.startsWith("대응:") ? CYAN : TEXT, false);
                y += 11;
            }
        }
        graphics.disableScissor();
    }

    private void drawMonsterList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        if (pane.width() <= 2 || pane.height() <= 2) return;
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xB90D1519);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        graphics.text(font, fit(font, "예상 몬스터", pane.width() - 16), pane.left() + 8, pane.top() + 7, CYAN, false);
        if (waves.isEmpty()) {
            graphics.disableScissor();
            return;
        }
        List<MonsterEntry> roster = currentRoster();
        int y = pane.top() + 23;
        for (int i = 0; i < roster.size(); i++) {
            MonsterEntry monster = roster.get(i);
            int h = 40;
            if (y + h > pane.bottom() - 4) break;
            boolean hover = inside(mouseX, mouseY, pane.left() + 5, y, pane.width() - 10, h);
            boolean active = selectedMonster == i;
            graphics.fill(pane.left() + 5, y, pane.right() - 5, y + h,
                    active || hover ? SURFACE_2 : SURFACE);
            graphics.fill(pane.left() + 5, y, pane.left() + 8, y + h, active ? GOLD : CYAN);
            graphics.text(font, fit(font, monster.name() + " ×" + monster.count(), pane.width() - 22),
                    pane.left() + 13, y + 6, active ? GOLD : TEXT, false);
            graphics.text(font, fit(font, monster.role(), pane.width() - 22), pane.left() + 13, y + 23,
                    MUTED, false);
            y += h + 4;
        }
        graphics.disableScissor();
    }

    private void drawDossier(GuiGraphicsExtractor graphics, Pane pane) {
        if (pane.width() <= 2 || pane.height() <= 2) return;
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xC5121B20);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        List<MonsterEntry> roster = currentRoster();
        if (roster.isEmpty()) {
            graphics.text(font, fit(font, "이 웨이브에서 식별된 병과가 없습니다.", pane.width() - 24),
                    pane.left() + 12, pane.top() + 12, MUTED, false);
            graphics.disableScissor();
            return;
        }
        MonsterEntry monster = roster.get(clamp(selectedMonster, 0, roster.size() - 1));
        VillageEnemyBestiary.Dossier dossier = monster.archetype() == null
                ? new VillageEnemyBestiary.Dossier("정찰 데이터가 부족합니다.", "특수 능력 미확인",
                "위협도 미상", "실전에서 행동을 확인하세요.")
                : VillageEnemyBestiary.dossier(monster.archetype());
        if (pane.height() < 100) {
            graphics.text(font, fit(font, "적 도감 · " + monster.name(), pane.width() - 24),
                    pane.left() + 12, pane.top() + 6, GOLD, false);
            graphics.text(font, fit(font, monster.role() + " · 예상 " + monster.count() + "명", pane.width() - 24),
                    pane.left() + 12, pane.top() + 20, CYAN, false);
            graphics.text(font, fit(font, dossier.overview(), pane.width() - 24),
                    pane.left() + 12, pane.top() + 36, MUTED, false);
            graphics.text(font, fit(font, "대응: " + dossier.counter(), pane.width() - 24),
                    pane.left() + 12, pane.top() + 51, GOLD, false);
            graphics.disableScissor();
            return;
        }
        graphics.text(font, fit(font, "적 도감 · " + monster.name(), pane.width() - 24),
                pane.left() + 12, pane.top() + 10, GOLD, false);
        graphics.text(font, fit(font, monster.role() + " · 예상 " + monster.count() + "명", pane.width() - 24),
                pane.left() + 12, pane.top() + 27, CYAN, false);
        int y = pane.top() + 47;
        y = section(graphics, pane, y, "개요", dossier.overview(), TEXT);
        y = section(graphics, pane, y, "능력", dossier.ability(), CYAN);
        y = section(graphics, pane, y, "위협", dossier.threat(), RED);
        section(graphics, pane, y, "대응", dossier.counter(), GOLD);
        graphics.disableScissor();
    }

    private int section(GuiGraphicsExtractor graphics, Pane pane, int y, String title, String text, int color) {
        if (y > pane.bottom() - 18) return y;
        graphics.text(font, title, pane.left() + 12, y, color, false);
        y += 13;
        for (FormattedCharSequence line : font.split(Component.literal(text), Math.max(70, pane.width() - 24))) {
            if (y > pane.bottom() - 12) return y;
            graphics.text(font, line, pane.left() + 12, y, MUTED, false);
            y += 11;
        }
        return y + 7;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        int y = layout.waveList().top() + 23;
        for (int i = 0; i < waves.size(); i++) {
            if (y + 44 > layout.waveList().bottom() - 4) break;
            if (inside(click.x(), click.y(), layout.waveList().left() + 5, y,
                    layout.waveList().width() - 10, 44)) {
                selectedWave = i;
                selectedMonster = 0;
                return true;
            }
            y += 49;
        }
        List<MonsterEntry> roster = currentRoster();
        y = layout.monsters().top() + 23;
        for (int i = 0; i < roster.size(); i++) {
            if (y + 40 > layout.monsters().bottom() - 4) break;
            if (inside(click.x(), click.y(), layout.monsters().left() + 5, y,
                    layout.monsters().width() - 10, 40)) {
                selectedMonster = i;
                return true;
            }
            y += 44;
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int top = Math.min(safe.bottom() - 2, safe.top() + 56);
        int bottom = Math.max(top + 1, safe.bottom() - 18);
        int contentHeight = Math.max(1, bottom - top);
        boolean compactHeight = contentHeight < 190;
        int gap = compactHeight ? 7 : 9;
        int waveWidth = VillageUiSafeArea.clamp(safe.width() * 26 / 100, 130, 255);
        waveWidth = Math.min(waveWidth, Math.max(95, safe.width() - 230));
        Pane waveList = new Pane(safe.left() + 7, top, safe.left() + 7 + waveWidth, bottom);
        int rightLeft = Math.min(safe.right() - 8, waveList.right() + gap);
        int rightRight = Math.max(rightLeft + 1, safe.right() - 7);
        int rightWidth = Math.max(1, rightRight - rightLeft);
        int overviewHeight = compactHeight
                ? VillageUiSafeArea.clamp(contentHeight * 30 / 100, 32, 46)
                : VillageUiSafeArea.clamp(contentHeight * 39 / 100, 92, 190);
        Pane overview = new Pane(rightLeft, top, rightRight, Math.min(bottom, top + overviewHeight));
        int lowerTop = Math.min(bottom, overview.bottom() + gap);
        if (compactHeight || rightWidth >= 430) {
            int monsterMinimum = compactHeight ? Math.min(72, rightWidth) : Math.min(150, rightWidth);
            int dossierMinimum = compactHeight ? Math.min(92, Math.max(1, rightWidth - monsterMinimum - gap))
                    : Math.min(150, Math.max(1, rightWidth - monsterMinimum - gap));
            int preferred = compactHeight ? rightWidth * 38 / 100 : rightWidth * 38 / 100;
            int maximumMonster = Math.max(monsterMinimum, rightWidth - gap - dossierMinimum);
            int monsterWidth = VillageUiSafeArea.clamp(preferred, monsterMinimum, maximumMonster);
            monsterWidth = Math.min(monsterWidth, Math.max(1, rightWidth - gap - 1));
            Pane monsters = new Pane(rightLeft, lowerTop, Math.min(rightRight, rightLeft + monsterWidth), bottom);
            int dossierLeft = Math.min(rightRight - 1, monsters.right() + gap);
            Pane dossier = new Pane(dossierLeft, lowerTop, rightRight, bottom);
            return new Layout(safe, waveList, overview, monsters, dossier);
        }
        int lowerHeight = Math.max(1, bottom - lowerTop);
        int minimumDossierHeight = Math.min(66, Math.max(1, lowerHeight / 2));
        int maximumRosterHeight = Math.max(1, lowerHeight - gap - minimumDossierHeight);
        int rosterHeight = VillageUiSafeArea.clamp(lowerHeight * 42 / 100,
                Math.min(58, maximumRosterHeight), maximumRosterHeight);
        Pane monsters = new Pane(rightLeft, lowerTop, rightRight, Math.min(bottom, lowerTop + rosterHeight));
        int dossierTop = Math.min(bottom - 1, monsters.bottom() + gap);
        Pane dossier = new Pane(rightLeft, dossierTop, rightRight, bottom);
        return new Layout(safe, waveList, overview, monsters, dossier);
    }

    private List<MonsterEntry> currentRoster() {
        if (waves.isEmpty()) return List.of();
        return waves.get(clamp(selectedWave, 0, waves.size() - 1)).roster();
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            if ("facility_info".equals(actions[i])) continue;
            String[] p = labels[i].split("\\|", 2);
            String title = plain(p.length > 0 ? p[0] : "웨이브");
            String detail = plain(p.length > 1 ? p[1] : "");
            waves.add(new WaveEntry(title, overview(detail), roster(detail)));
        }
    }

    private List<MonsterEntry> roster(String detail) {
        List<MonsterEntry> result = new ArrayList<>();
        boolean roster = false;
        for (String raw : detail.split("\n", -1)) {
            String line = raw.trim();
            if (line.equals("병력:")) {
                roster = true;
                continue;
            }
            if (!roster || !line.startsWith("- ")) continue;
            String value = line.substring(2).trim();
            int times = value.indexOf('×');
            if (times <= 0) continue;
            String name = value.substring(0, times).trim();
            int roleSplit = value.indexOf(" · ", times);
            String amountRaw = roleSplit > times ? value.substring(times + 1, roleSplit).trim()
                    : value.substring(times + 1).trim();
            int amount = parseInt(amountRaw, 1);
            String role = roleSplit > times ? value.substring(roleSplit + 3).trim() : "전투 역할 미상";
            VillageEnemyArchetypeSystem.Archetype archetype = VillageEnemyBestiary.find(name).orElse(null);
            result.add(new MonsterEntry(name, amount, role, archetype));
        }
        return List.copyOf(result);
    }

    private String overview(String detail) {
        StringBuilder result = new StringBuilder();
        for (String raw : detail.split("\n", -1)) {
            if (raw.trim().equals("병력:")) break;
            if (!result.isEmpty()) result.append('\n');
            result.append(raw);
        }
        return result.toString().trim();
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record MonsterEntry(String name, int count, String role,
                                VillageEnemyArchetypeSystem.Archetype archetype) {}
    private record WaveEntry(String title, String overview, List<MonsterEntry> roster) {}
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record Layout(VillageUiSafeArea.Rect safe, Pane waveList, Pane overview, Pane monsters, Pane dossier) {}
}
