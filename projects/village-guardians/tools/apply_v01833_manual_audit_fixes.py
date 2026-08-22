#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    a = text.find(start)
    if a < 0:
        raise RuntimeError(f"{label}: start marker missing")
    b = text.find(end, a + len(start))
    if b < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return text[:a] + replacement + text[b:]


def patch_wave_intel() -> None:
    path = JAVA / "VillageWaveIntelDossierScreen.java"
    text = read(path)

    text = replace_between(text,
        '''    private void drawWaveList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
        '''    private void drawWaveOverview(GuiGraphicsExtractor graphics, Pane pane) {''',
        '''    private void drawWaveList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
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

''', "wave list clipping")

    text = replace_between(text,
        '''    private void drawWaveOverview(GuiGraphicsExtractor graphics, Pane pane) {''',
        '''    private void drawMonsterList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
        '''    private void drawWaveOverview(GuiGraphicsExtractor graphics, Pane pane) {
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
            String compact = wave.overview().replace('\\n', ' ').replace("  ", " ");
            graphics.text(font, fit(font, compact, pane.width() - 22), pane.left() + 11, pane.top() + 19,
                    TEXT, false);
            graphics.disableScissor();
            return;
        }
        graphics.text(font, fit(font, wave.title(), pane.width() - 22), pane.left() + 11, pane.top() + 9,
                GOLD, false);
        int y = pane.top() + 27;
        for (String paragraph : wave.overview().split("\\n", -1)) {
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

''', "wave overview compact mode")

    text = replace_between(text,
        '''    private void drawMonsterList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
        '''    private void drawDossier(GuiGraphicsExtractor graphics, Pane pane) {''',
        '''    private void drawMonsterList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
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

''', "monster list clipping")

    text = replace_between(text,
        '''    private void drawDossier(GuiGraphicsExtractor graphics, Pane pane) {''',
        '''    private int section(GuiGraphicsExtractor graphics, Pane pane, int y, String title, String text, int color) {''',
        '''    private void drawDossier(GuiGraphicsExtractor graphics, Pane pane) {
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

''', "dossier compact mode")

    text = replace_once(text,
'''        int y = layout.waveList().top() + 23;
        for (int i = 0; i < waves.size(); i++) {
            if (inside(click.x(), click.y(), layout.waveList().left() + 5, y,
                    layout.waveList().width() - 10, 44)) {''',
'''        int y = layout.waveList().top() + 23;
        for (int i = 0; i < waves.size(); i++) {
            if (y + 44 > layout.waveList().bottom() - 4) break;
            if (inside(click.x(), click.y(), layout.waveList().left() + 5, y,
                    layout.waveList().width() - 10, 44)) {''', "wave click clipping")
    text = replace_once(text,
'''        y = layout.monsters().top() + 23;
        for (int i = 0; i < roster.size(); i++) {
            if (inside(click.x(), click.y(), layout.monsters().left() + 5, y,
                    layout.monsters().width() - 10, 40)) {''',
'''        y = layout.monsters().top() + 23;
        for (int i = 0; i < roster.size(); i++) {
            if (y + 40 > layout.monsters().bottom() - 4) break;
            if (inside(click.x(), click.y(), layout.monsters().left() + 5, y,
                    layout.monsters().width() - 10, 40)) {''', "monster click clipping")

    text = replace_between(text,
        '''    private Layout layout() {''',
        '''    private List<MonsterEntry> currentRoster() {''',
        '''    private Layout layout() {
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

''', "responsive wave layout")
    write(path, text)


def patch_local_action() -> None:
    path = JAVA / "VillageLocalActionSystem.java"
    text = read(path)
    text = replace_once(text,
'''        if (action.startsWith("merc_deploy:")) {
            String[] parts = action.split(":", 3);''',
'''        if (action.startsWith("merc_deploy:")) {
            if (!VillageMercenaryDeploymentSystem.canOpenAt(player)) {
                player.sendSystemMessage(Component.literal("§c용병 배치는 병영 또는 마을 회관 근처에서만 변경할 수 있습니다."));
                return true;
            }
            String[] parts = action.split(":", 3);''', "mercenary deployment authorization")
    write(path, text)


def patch_controller() -> None:
    path = JAVA / "VillageUiController.java"
    text = read(path)
    text = replace_once(text,
'''        if (action.startsWith("role_node:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c직업 성장은 기술 연구소에서만 가능합니다."));
                return true;
            }
            String[] parts = action.split(":", 3);''',
'''        if (action.startsWith("role_node:")) {
            String[] parts = action.split(":", 3);''', "role growth anywhere contract")
    text = replace_once(text,
'''        if (action.startsWith("sell_item:")) {
            try {''',
'''        if (action.startsWith("sell_item:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                player.sendSystemMessage(Component.literal("§c보유품 판매는 창고 단말기 근처에서만 가능합니다."));
                return true;
            }
            try {''', "item sell authorization")
    write(path, text)


def patch_ui_service() -> None:
    path = JAVA / "VillageUiService.java"
    text = read(path)
    text = replace_once(text,
'''        if (action.startsWith("role_node:")) {
            if (!requireSkillHall(player, "직업 성장은 기술 연구소에서만 가능합니다.")) return;
            String[] parts = action.split(":", 3);''',
'''        if (action.startsWith("role_node:")) {
            String[] parts = action.split(":", 3);''', "legacy role growth anywhere contract")
    text = replace_once(text,
'''            case "restart_previous" -> VillageProgressionSystem.resetForRestart(server, false);
            case "restart_start" -> VillageProgressionSystem.resetForRestart(server, true);''',
'''            case "restart_previous" -> {
                if (!VillageProgressionSystem.isGameOver())
                    player.sendSystemMessage(Component.literal("§c방어 실패 상태에서만 전투 전 낮으로 되돌릴 수 있습니다."));
                else VillageProgressionSystem.resetForRestart(server, false);
            }
            case "restart_start" -> {
                if (!VillageProgressionSystem.isGameOver())
                    player.sendSystemMessage(Component.literal("§c방어 실패 상태에서만 처음부터 다시 시작할 수 있습니다."));
                else VillageProgressionSystem.resetForRestart(server, true);
            }''', "restart packet authorization")
    write(path, text)


def patch_progression() -> None:
    path = JAVA / "VillageProgressionSystem.java"
    text = read(path)
    text = replace_once(text,
'''    public static synchronized void resetForRestart(MinecraftServer server, boolean fromStart) {
        gameOver = false;''',
'''    public static synchronized void resetForRestart(MinecraftServer server, boolean fromStart) {
        if (!gameOver) return;
        gameOver = false;''', "restart defense in depth")
    write(path, text)


def patch_equipment() -> None:
    path = JAVA / "VillageEquipmentRaritySystem.java"
    text = read(path)
    text = replace_once(text,
'''        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isUpgradeable(stack.getItem())) return "게임 전용 등급 장비만 강화할 수 있습니다.";
        int current = enhancementLevel(stack);''',
'''        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isUpgradeable(stack.getItem())) return "게임 전용 등급 장비만 강화할 수 있습니다.";
        if (stack.getCount() != 1) return "강화할 장비는 해당 슬롯에 1개만 두세요.";
        int current = enhancementLevel(stack);''', "single item enhancement")
    write(path, text)


def patch_role_progress_screen() -> None:
    path = JAVA / "VillageRoleProgressScreen.java"
    text = read(path)
    text = replace_once(text,
'''                    skill.slot() == 0 ? "Z 슬롯 ✓" : "Z 슬롯", true, ACCENT);''',
'''                    skill.slot() == 0 ? VillageClientKeys.skillOneKeyName() + " 슬롯 ✓"
                            : VillageClientKeys.skillOneKeyName() + " 슬롯", true, ACCENT);''', "dynamic first skill key")
    text = replace_once(text,
'''                    skill.slot() == 1 ? "X 슬롯 ✓" : "X 슬롯", true, ACCENT);''',
'''                    skill.slot() == 1 ? VillageClientKeys.skillTwoKeyName() + " 슬롯 ✓"
                            : VillageClientKeys.skillTwoKeyName() + " 슬롯", true, ACCENT);''', "dynamic second skill key")
    text = replace_once(text,
'''        if (skill.slot() == 0) return "Z 슬롯 장착";
        if (skill.slot() == 1) return "X 슬롯 장착";''',
'''        if (skill.slot() == 0) return VillageClientKeys.skillOneKeyName() + " 슬롯 장착";
        if (skill.slot() == 1) return VillageClientKeys.skillTwoKeyName() + " 슬롯 장착";''', "dynamic skill status keys")
    write(path, text)


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    if "mod_version=0.18.33-alpha.1" not in props:
        raise RuntimeError("manual audit patch expects Village Guardians 0.18.33-alpha.1")
    patch_wave_intel()
    patch_local_action()
    patch_controller()
    patch_ui_service()
    patch_progression()
    patch_equipment()
    patch_role_progress_screen()
    print("[PATCH] Village Guardians 0.18.33 manual audit corrections applied")


if __name__ == "__main__":
    main()
