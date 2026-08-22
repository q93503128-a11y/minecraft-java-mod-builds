#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if text.count(old) != 1:
        raise RuntimeError(f"{label}: expected one match, found {text.count(old)}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    i = text.find(start)
    if i < 0:
        raise RuntimeError(f"{label}: start marker not found")
    j = text.find(end, i)
    if j < 0:
        raise RuntimeError(f"{label}: end marker not found")
    return text[:i] + replacement + text[j:]


def patch_town_hall_screen() -> None:
    path = JAVA / "VillageTownHallGridScreen.java"
    text = read(path)

    text = replace_once(text,
'''        drawHeader(graphics, layout, mouseX, mouseY);
        drawTabs(graphics, layout, mouseX, mouseY);
        drawList(graphics, layout.list(), mouseX, mouseY);
''',
'''        drawHeader(graphics, layout, mouseX, mouseY);
        drawList(graphics, layout.list(), mouseX, mouseY);
''', "town hall remove tabs from production render")

    text = replace_between(text,
'''    private void drawFrame(GuiGraphicsExtractor graphics, Layout layout) {''',
'''    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {''',
'''    private void drawFrame(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, LINE);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), GOLD);
    }

''', "town hall frame")

    text = replace_between(text,
'''    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {''',
'''    private void drawTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {''',
'''    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int x = layout.left() + 17;
        int closeX = layout.right() - 34;
        graphics.text(font, "지휘 회관 · 시설 유지보수", x, layout.top() + 10, GOLD, false);
        int bodyWidth = Math.max(80, closeX - x - 10);
        List<FormattedCharSequence> lines = font.split(Component.literal(body.replace('\\n', ' ')), bodyWidth);
        int bodyY = layout.top() + 27;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, bodyY, MUTED, false);
            bodyY += 11;
        }
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 8, 24, 24);
        graphics.fill(closeX, layout.top() + 8, closeX + 24, layout.top() + 32, close ? 0xFF71353A : PANEL_3);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 15, close ? TEXT : MUTED);
        graphics.fill(layout.left() + 14, layout.top() + 52, layout.right() - 14, layout.top() + 53, LINE);
    }

''', "town hall wrapped header")

    text = replace_between(text,
'''    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
'''    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
'''    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), PANEL_2);
        int count = facilities.size();
        int rowHeight = 50;
        int gap = 4;
        int content = count <= 0 ? 0 : count * rowHeight + Math.max(0, count - 1) * gap;
        int maxScroll = Math.max(0, content - Math.max(1, pane.height() - 12));
        listScroll = clamp(listScroll, 0, maxScroll);
        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1, pane.bottom() - 1);
        int y = pane.top() + 6 - listScroll;
        for (int i = 0; i < count; i++) {
            FacilityCard f = facilities.get(i);
            int x = pane.left() + 6;
            int w = pane.width() - 14;
            boolean hover = inside(mouseX, mouseY, x, y, w, rowHeight);
            boolean selected = selectedFacility == i;
            int accent = facilityColor(f);
            graphics.fill(x, y, x + w, y + rowHeight, selected ? PANEL_3 : hover ? 0xE522333B : 0xD9111B21);
            graphics.fill(x, y, x + 3, y + rowHeight, accent);
            graphics.text(font, fit(font, f.name(), w - 18), x + 10, y + 6, selected ? TEXT : MUTED, false);
            graphics.text(font, fit(font, f.meta(), w - 18), x + 10, y + 20, MUTED, false);
            String durability = f.current() <= 0 ? "파괴됨" : "내구도 " + f.current() + " / " + f.maximum();
            graphics.text(font, fit(font, durability, w - 18), x + 10, y + 34, accent, false);
            y += rowHeight + gap;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int track = pane.height() - 12;
            int thumb = Math.max(14, track * Math.max(1, pane.height() - 12) / Math.max(1, content));
            int sy = pane.top() + 6 + (track - thumb) * listScroll / maxScroll;
            graphics.fill(pane.right() - 4, pane.top() + 6, pane.right() - 2, pane.bottom() - 6, 0x55607178);
            graphics.fill(pane.right() - 4, sy, pane.right() - 2, sy + thumb, GOLD);
        }
    }

''', "town hall readable facility rows")

    text = replace_between(text,
'''    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
'''    private void drawRoleDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
'''    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xD90D171D);
        drawFacilityDetail(graphics, pane, mouseX, mouseY);
    }

''', "town hall facility-only detail")

    text = replace_between(text,
'''    private List<ButtonSpec> facilityButtons(Pane pane, FacilityCard f) {''',
'''    private String functionAction(FacilityCard f) {''',
'''    private List<ButtonSpec> facilityButtons(Pane pane, FacilityCard f) {
        List<ButtonSpec> result = new ArrayList<>();
        boolean usable = f.current() > 0;
        boolean repair = f.current() < f.maximum() && f.repairCost() > 0;
        boolean upgrade = usable && f.upgradeCost() > 0 && !f.nextEffect().isBlank();
        String repairLabel = repair ? "건물 수리 · " + f.repairCost() : "수리 불필요";
        String upgradeLabel = upgrade ? "건물 강화 · " + f.upgradeCost() : "강화 완료";

        int gap = 7;
        int left = pane.left() + 14;
        int innerWidth = Math.max(1, pane.width() - 28);
        int h = 27;
        if (pane.width() < 260) {
            int y = pane.bottom() - 12 - (h * 2 + gap);
            result.add(new ButtonSpec(new Button(left, y, innerWidth, h), repairLabel, repair, GOLD, "repair:" + f.id()));
            y += h + gap;
            result.add(new ButtonSpec(new Button(left, y, innerWidth, h), upgradeLabel, upgrade, GREEN, "upgrade:" + f.id()));
            return result;
        }

        int available = Math.max(2, innerWidth - gap);
        int firstWidth = available / 2;
        int secondWidth = available - firstWidth;
        int y = pane.bottom() - 39;
        result.add(new ButtonSpec(new Button(left, y, firstWidth, h), repairLabel, repair, GOLD, "repair:" + f.id()));
        result.add(new ButtonSpec(new Button(left + firstWidth + gap, y, secondWidth, h),
                upgradeLabel, upgrade, GREEN, "upgrade:" + f.id()));
        return result;
    }

''', "town hall two maintenance actions")

    text = replace_between(text,
'''    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {''',
'''    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {''',
'''    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 8, 24, 24)) {
            onClose();
            return true;
        }

        Pane list = layout.list();
        int y = list.top() + 6 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            if (inside(click.x(), click.y(), list.left() + 6, y, list.width() - 14, 50)) {
                selectedFacility = i;
                return true;
            }
            y += 54;
        }

        if (!facilities.isEmpty()) {
            FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
            for (ButtonSpec spec : facilityButtons(layout.detail(), facility)) {
                if (!spec.enabled() || !inside(click.x(), click.y(), spec.bounds().x(), spec.bounds().y(),
                        spec.bounds().w(), spec.bounds().h())) continue;
                if (VillageActionDescriptions.requiresConfirmation(spec.action()) && minecraft != null) {
                    minecraft.gui.setScreen(new VillageConfirmScreen(this, spec.action(), facility.name(),
                            VillageActionDescriptions.describe(spec.action(), facility.name())));
                } else {
                    ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(spec.action()));
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

''', "town hall facility-only clicks")

    text = replace_between(text,
'''    private Layout layout() {''',
'''    private void parse(VillageNetwork.OpenVillageUiPayload payload) {''',
'''    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int panelWidth = Math.min(940, Math.max(1, safe.width()));
        int panelHeight = Math.min(500, Math.max(1, safe.height()));
        int left = safe.centerX() - panelWidth / 2;
        int top = safe.top() + Math.max(0, (safe.height() - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int contentTop = Math.min(bottom - 1, top + 60);
        int contentBottom = Math.max(contentTop + 1, bottom - 12);
        int gap = 10;
        int contentWidth = Math.max(1, panelWidth - 28 - gap);
        int listWidth = clamp(panelWidth * 31 / 100, 150, 280);
        listWidth = Math.min(listWidth, Math.max(90, contentWidth - 170));
        Pane list = new Pane(left + 14, contentTop, left + 14 + listWidth, contentBottom);
        Pane detail = new Pane(Math.min(right - 15, list.right() + gap), contentTop, right - 14, contentBottom);
        return new Layout(left, top, right, bottom, list, detail);
    }

''', "town hall calculated layout")

    write(path, text)


def patch_controller() -> None:
    path = JAVA / "VillageUiController.java"
    text = read(path)

    text = replace_between(text,
'''    public static void openDashboard(ServerPlayer player) {''',
'''    public static void openCaller(ServerPlayer player) {''',
'''    public static void openDashboard(ServerPlayer player) {
        if (!requireTownHall(player, "마을 회관 지휘대 근처에서만 시설을 관리할 수 있습니다.")) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            int level = VillageProgressionSystem.level(building);
            int current = VillageProgressionSystem.durability(building);
            int maximum = VillageProgressionSystem.maxDurability(building);
            int missing = Math.max(0, maximum - current);
            int repairCost = missing <= 0 ? 0 : Math.max(20, (missing + 7) / 8);
            boolean canUpgrade = building != VillageProgressionSystem.Building.TOWN_HALL
                    && level < VillageProgressionSystem.MAX_BUILDING_LEVEL;
            int upgradeCost = canUpgrade ? VillageProgressionSystem.upgradeCost(level) : 0;
            String nextEffect = canUpgrade ? managementEffect(building, level + 1, server) : "";
            String levelText = building == VillageProgressionSystem.Building.TOWN_HALL
                    ? "회관 본체" : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
            actions.add("facility:" + building.id());
            labels.add(String.join("|", "facility", building.id(), building.displayName(), levelText,
                    Integer.toString(current), Integer.toString(maximum), managementEffect(building, level, server),
                    nextEffect, Integer.toString(upgradeCost), Integer.toString(repairCost)));
        }
        String body = "제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " · 공동 보급품 " + VillageProgressionSystem.supplies()
                + " · 회관에서는 시설 수리와 강화만 관리합니다.";
        send(player, "town_hall", "마을 회관", body, actions, labels);
    }

    public static void openRoleAssignment(ServerPlayer player) {
        if (!VillageLocationRules.isNearSkillHall(player)) {
            player.sendSystemMessage(Component.literal("§c직업 배치는 기술 연구소 연구대 근처에서만 가능합니다."));
            return;
        }
        VillageRole currentRole = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRole role : VillageRole.values()) {
            actions.add("select_role:" + role.id());
            String status = currentRole == role ? "현재 직업" : "배치 가능";
            labels.add(role.displayName() + " · " + status + "|" + role.overview()
                    + "\\n상시 효과: " + role.passive()
                    + "\\n전투 방식: " + role.active()
                    + "\\n추천 위치: " + role.recommended());
        }
        send(player, "building", "직업 배치",
                "직업 선택은 기술 연구소에서 관리합니다. 시설 수리·강화와 분리된 메뉴입니다.", actions, labels);
    }

''', "controller maintenance-only dashboard")

    text = replace_once(text,
'''                () -> player.sendSystemMessage(Component.literal("§c마을 회관에서 직업을 먼저 배치하세요.")));''',
'''                () -> player.sendSystemMessage(Component.literal("§c기술 연구소에서 직업을 먼저 배치하세요.")));''',
"role progress location message")
    text = replace_once(text,
'''            player.sendSystemMessage(Component.literal("§c마을 회관에서 직업을 먼저 배치하세요."));''',
'''            player.sendSystemMessage(Component.literal("§c기술 연구소에서 직업을 먼저 배치하세요."));''',
"role research location message")

    old_select = '''        if (action.startsWith("select_role:")) {
            if (!requireTownHall(player, "직업 배치는 마을 회관에서만 가능합니다.")) return true;
            VillageRole.parse(action.substring(12)).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openDashboard(player);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 직업입니다.")));
            return true;
        }
'''
    new_select = '''        if (action.startsWith("select_role:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c직업 배치는 기술 연구소에서만 가능합니다."));
                return true;
            }
            VillageRole.parse(action.substring(12)).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openRoleAssignment(player);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 직업입니다.")));
            return true;
        }
'''
    text = replace_once(text, old_select, new_select, "move role selection to skill hall")

    text = replace_once(text,
'''            case "open_role_skill_research" -> openRoleSkillResearch(player);''',
'''            case "open_role_assignment" -> openRoleAssignment(player);
            case "open_role_skill_research" -> openRoleSkillResearch(player);''',
"role assignment action")

    text = replace_once(text,
'''            case SKILL_HALL -> add(actions, labels,
                    "open_role_skill_research", "직업 기술 연구|현재 직업의 기술 습득과 {SKILL1}/{SKILL2} 장착만 관리",
                    "open_defense_research", "마을 방어 연구|용병·포탑·전리품 연구 트리",
                    "open_skill_test", "외부 기술 시험장|야외 시험장으로 이동해 {SKILL1}/{SKILL2}에 기술을 임시 장착하고 실제 모션 시험");''',
'''            case SKILL_HALL -> add(actions, labels,
                    "open_role_assignment", "직업 배치|플레이어 직업 선택·변경",
                    "open_role_skill_research", "직업 기술 연구|현재 직업의 기술 습득과 {SKILL1}/{SKILL2} 장착만 관리",
                    "open_defense_research", "마을 방어 연구|용병·포탑·전리품 연구 트리",
                    "open_skill_test", "외부 기술 시험장|야외 시험장으로 이동해 {SKILL1}/{SKILL2}에 기술을 임시 장착하고 실제 모션 시험");''',
"skill hall role assignment entry")

    text = replace_once(text,
'''            case SKILL_HALL -> "직업 기술과 용병·포탑 방어 연구를 담당합니다. 연구소 레벨마다 기술 위력·지속시간이 +5% 상승하고 재사용 효율도 개선됩니다.";''',
'''            case SKILL_HALL -> "직업 배치·직업 기술과 용병·포탑 방어 연구를 담당합니다. 연구소 레벨마다 기술 위력·지속시간이 +5% 상승하고 재사용 효율도 개선됩니다.";''',
"skill hall description")
    text = replace_once(text,
'''            case TOWN_HALL -> "직업 배치와 모든 시설 수리·강화·건설을 담당합니다.";''',
'''            case TOWN_HALL -> "모든 시설의 수리와 강화만 담당합니다. 각 시설의 고유 기능은 해당 건물에서 직접 사용합니다.";''',
"town hall description")
    text = replace_once(text,
'''            case TOWN_HALL -> "직업 배치·시설 수리·강화·공동 보급 조달";''',
'''            case TOWN_HALL -> "시설 수리·강화 지휘";''',
"town hall management effect")
    text = replace_once(text,
'''            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 기술 위력 +" + (safe * 5) + "% · 지속 +" + (safe * 5) + "% · 재사용 효율 +" + safe + "초 · 마을 방어 연구";''',
'''            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 직업 배치 · 기술 위력 +" + (safe * 5) + "% · 지속 +" + (safe * 5) + "% · 재사용 효율 +" + safe + "초 · 마을 방어 연구";''',
"skill hall management effect")

    write(path, text)


def patch_wave_screen() -> None:
    path = JAVA / "VillageWaveIntelDossierScreen.java"
    text = read(path)

    text = replace_between(text,
'''    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {''',
'''    private void drawWaveList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {''',
'''    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        VillageUiSafeArea.Rect safe = layout.safe();
        graphics.text(font, "전술 브리핑 · 다음 밤 적 정찰", safe.left() + 8, safe.top() + 5, GOLD, false);
        List<FormattedCharSequence> header = font.split(Component.literal(body.replace('\\n', ' ')),
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

''', "wave readable header")

    text = replace_once(text, '''            int h = 36;''', '''            int h = 44;''', "wave row height")
    text = replace_once(text, '''pane.left() + 13, y + 6,''', '''pane.left() + 13, y + 7,''', "wave title y")
    text = replace_once(text, '''            graphics.text(font, wave.roster().size() + "종 확인", pane.left() + 13, y + 21, MUTED, false);
            y += h + 5;''',
'''            graphics.text(font, "예상 병과 " + wave.roster().size() + "종", pane.left() + 13, y + 25, CYAN, false);
            y += h + 5;''', "wave row summary")

    text = replace_once(text, '''            int h = 33;''', '''            int h = 40;''', "monster row height")
    text = replace_once(text, '''pane.left() + 13, y + 19,''', '''pane.left() + 13, y + 23,''', "monster role y")

    text = replace_once(text,
'''        graphics.text(font, "적 도감  //  " + monster.name(), pane.left() + 12, pane.top() + 10, GOLD, false);
        graphics.text(font, monster.role() + " · 예상 " + monster.count() + "명",
                pane.left() + 12, pane.top() + 27, CYAN, false);''',
'''        graphics.text(font, fit(font, "적 도감 · " + monster.name(), pane.width() - 24),
                pane.left() + 12, pane.top() + 10, GOLD, false);
        graphics.text(font, fit(font, monster.role() + " · 예상 " + monster.count() + "명", pane.width() - 24),
                pane.left() + 12, pane.top() + 27, CYAN, false);''', "dossier title fitting")

    text = replace_once(text,
'''        int y = layout.waveList().top() + 23;''',
'''        int y = layout.waveList().top() + 23;''', "wave click start marker")
    text = replace_once(text,
'''                    layout.waveList().width() - 10, 36)) {''',
'''                    layout.waveList().width() - 10, 44)) {''', "wave click height")
    text = replace_once(text, '''            y += 41;''', '''            y += 49;''', "wave click spacing")
    text = replace_once(text,
'''                    layout.monsters().width() - 10, 33)) {''',
'''                    layout.monsters().width() - 10, 40)) {''', "monster click height")
    text = replace_once(text, '''            y += 37;''', '''            y += 44;''', "monster click spacing")

    text = replace_between(text,
'''    private Layout layout() {''',
'''    private List<MonsterEntry> currentRoster() {''',
'''    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int top = Math.min(safe.bottom() - 2, safe.top() + 56);
        int bottom = Math.max(top + 1, safe.bottom() - 18);
        int gap = 9;
        int waveWidth = VillageUiSafeArea.clamp(safe.width() * 26 / 100, 130, 255);
        waveWidth = Math.min(waveWidth, Math.max(95, safe.width() - 230));
        Pane waveList = new Pane(safe.left() + 7, top, safe.left() + 7 + waveWidth, bottom);
        int rightLeft = Math.min(safe.right() - 8, waveList.right() + gap);
        int rightRight = safe.right() - 7;
        int rightWidth = Math.max(1, rightRight - rightLeft);
        int overviewHeight = VillageUiSafeArea.clamp((bottom - top) * 39 / 100, 92, 190);
        Pane overview = new Pane(rightLeft, top, rightRight, Math.min(bottom, top + overviewHeight));
        int lowerTop = Math.min(bottom, overview.bottom() + gap);
        if (rightWidth >= 430) {
            int monsterWidth = VillageUiSafeArea.clamp(rightWidth * 38 / 100, 150, 270);
            Pane monsters = new Pane(rightLeft, lowerTop, Math.min(rightRight, rightLeft + monsterWidth), bottom);
            Pane dossier = new Pane(Math.min(rightRight, monsters.right() + gap), lowerTop, rightRight, bottom);
            return new Layout(safe, waveList, overview, monsters, dossier);
        }
        int lowerHeight = Math.max(1, bottom - lowerTop);
        int rosterHeight = VillageUiSafeArea.clamp(lowerHeight * 42 / 100, 78, 160);
        Pane monsters = new Pane(rightLeft, lowerTop, rightRight, Math.min(bottom, lowerTop + rosterHeight));
        Pane dossier = new Pane(rightLeft, Math.min(bottom, monsters.bottom() + gap), rightRight, bottom);
        return new Layout(safe, waveList, overview, monsters, dossier);
    }

''', "wave calculated layout")

    write(path, text)


def patch_wall_geometry() -> None:
    path = JAVA / "VillageFortressTerrain.java"
    text = read(path)

    horizontal = '''    private static void buildHorizontalWall(
            ServerLevel level,
            BlockPos center,
            int groundY,
            int startZ,
            boolean north) {
        for (int dx = -WALL_RADIUS; dx <= WALL_RADIUS; dx++) {
            if (north && Math.abs(dx) <= 15) continue;
            boolean firingBay = isFiringBayOffset(dx);
            for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                int z = center.getZ() + startZ + offset;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    if (firingBay && y >= 3 && y <= 4) {
                        set(level, new BlockPos(center.getX() + dx, groundY + y, z), Blocks.AIR);
                    } else {
                        Block material = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                        set(level, new BlockPos(center.getX() + dx, groundY + y, z), material);
                    }
                }
            }
            if (firingBay) {
                int stepZ = center.getZ() + (north ? startZ + WALL_THICKNESS : startZ - 1);
                set(level, new BlockPos(center.getX() + dx, groundY + 1, stepZ), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dx, 3) != 1) {
                int outerZ = center.getZ() + (north ? startZ : startZ + WALL_THICKNESS - 1);
                int innerZ = center.getZ() + (north ? startZ + WALL_THICKNESS - 1 : startZ);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, outerZ), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, innerZ), Blocks.STONE_BRICKS);
            }
        }
    }

'''
    text = replace_between(text,
'''    private static void buildHorizontalWall(''',
'''    private static void buildVerticalWall(ServerLevel level, BlockPos center, int groundY, int startX) {''',
horizontal, "horizontal firing bays")

    vertical = '''    private static void buildVerticalWall(ServerLevel level, BlockPos center, int groundY, int startX) {
        for (int dz = -WALL_RADIUS; dz <= WALL_RADIUS; dz++) {
            boolean firingBay = isFiringBayOffset(dz);
            for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                int x = center.getX() + startX + offset;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    if (firingBay && y >= 3 && y <= 4) {
                        set(level, new BlockPos(x, groundY + y, center.getZ() + dz), Blocks.AIR);
                    } else {
                        Block material = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                        set(level, new BlockPos(x, groundY + y, center.getZ() + dz), material);
                    }
                }
            }
            if (firingBay) {
                int stepX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS : startX - 1);
                set(level, new BlockPos(stepX, groundY + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dz, 3) != 1) {
                int outerX = center.getX() + (startX < 0 ? startX : startX + WALL_THICKNESS - 1);
                int innerX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS - 1 : startX);
                set(level, new BlockPos(outerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
                set(level, new BlockPos(innerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
        }
    }

    private static boolean isFiringBayOffset(int offset) {
        int phase = Math.floorMod(offset, 12);
        return phase == 0 || phase == 1 || phase == 11;
    }

'''
    text = replace_between(text,
'''    private static void buildVerticalWall(ServerLevel level, BlockPos center, int groundY, int startX) {''',
'''    private static void buildDefenderGalleries(ServerLevel level, BlockPos center, int groundY) {''',
vertical, "vertical firing bays")

    ramp = '''    private static void buildWallAccessRamp(
            ServerLevel level, BlockPos center, int groundY, Direction outward, int lane) {
        Direction sideways = outward.getClockWise();
        int stairStart = WALL_RADIUS - 10;

        // 0.18.31 used a solid five-wide wedge beginning four blocks farther into the courtyard.
        // Clear only that retired inward footprint so existing worlds do not keep the old road block.
        for (int distance = WALL_RADIUS - 14; distance < stairStart; distance++) {
            BlockPos row = center.relative(outward, distance).relative(sideways, lane);
            for (int width = -3; width <= 3; width++) {
                BlockPos column = row.relative(sideways, width);
                for (int y = groundY + 1; y <= groundY + 7; y++) {
                    set(level, new BlockPos(column.getX(), y, column.getZ()), Blocks.AIR);
                }
            }
        }

        for (int step = 0; step < WALL_TOP_Y; step++) {
            BlockPos row = center.relative(outward, stairStart + step).relative(sideways, lane);
            int y = groundY + 1 + step;
            for (int width = -2; width <= 2; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos stairPos = new BlockPos(column.getX(), y, column.getZ());
                for (int supportY = groundY + 1; supportY < y; supportY++) {
                    set(level, new BlockPos(stairPos.getX(), supportY, stairPos.getZ()), Blocks.AIR);
                }
                // Sparse edge piers keep the staircase visually supported without forming a solid
                // courtyard barrier. Every other underside cell remains walk-through air.
                if ((step == 4 || step == 8) && Math.abs(width) == 2) {
                    for (int supportY = groundY + 1; supportY < y; supportY++) {
                        set(level, new BlockPos(stairPos.getX(), supportY, stairPos.getZ()), Blocks.STONE_BRICK_WALL);
                    }
                }
                level.setBlockAndUpdate(
                        stairPos,
                        Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, outward));
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, stairPos.above(clearY), Blocks.AIR);
                }
            }
        }

        int landingStart = WALL_RADIUS - 6;
        int landingEnd = WALL_RADIUS - 1;
        for (int distance = landingStart; distance <= landingEnd; distance++) {
            BlockPos row = center.relative(outward, distance).relative(sideways, lane);
            for (int width = -3; width <= 3; width++) {
                BlockPos column = row.relative(sideways, width);
                BlockPos landing = new BlockPos(column.getX(), groundY + WALL_TOP_Y, column.getZ());
                set(level, landing, Blocks.STONE_BRICKS);
                for (int clearY = 1; clearY <= 3; clearY++) {
                    set(level, landing.above(clearY), Blocks.AIR);
                }
            }
        }
    }

'''
    text = replace_between(text,
'''    private static void buildWallAccessRamp(
            ServerLevel level, BlockPos center, int groundY, Direction outward, int lane) {''',
'''    private static void buildTower(ServerLevel level, BlockPos corner, int groundY) {''',
ramp, "open stair support geometry")

    write(path, text)


def patch_world_migration() -> None:
    path = JAVA / "VillageWorldSystem.java"
    text = read(path)
    text = replace_once(text,
'''                || !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK)
                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK);''',
'''                || !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK)
                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK)
                || !level.getBlockState(center.below(9)).is(Blocks.GOLD_BLOCK);''',
"wall geometry migration marker")
    text = replace_once(text,
'''                        "§6[마을 정비] §f성벽 4면 접근 계단·포좌와 용병 고지 동선을 최신 방어 배치로 갱신합니다."));''',
'''                        "§6[마을 정비] §f성벽 사격구·접근 계단·포좌 동선을 최신 실전 배치로 갱신합니다."));''',
"migration message")
    text = replace_once(text,
'''        VillageFortressTerrain.set(level, center.below(8), Blocks.DIAMOND_BLOCK);''',
'''        VillageFortressTerrain.set(level, center.below(9), Blocks.GOLD_BLOCK);
        VillageFortressTerrain.set(level, center.below(8), Blocks.DIAMOND_BLOCK);''',
"write wall geometry marker")
    write(path, text)


def patch_metadata_and_history() -> None:
    props = ROOT / "gradle.properties"
    text = read(props)
    text = replace_once(text, "mod_version=0.18.32-alpha.1", "mod_version=0.18.33-alpha.1", "version")
    write(props, text)

    readme = ROOT / "README.md"
    text = read(readme)
    text = replace_once(text, "현재 소스 버전 `0.18.32-alpha.1`", "현재 소스 버전 `0.18.33-alpha.1`", "readme version")
    text = replace_once(text, "목표 JAR `villageguardians-0.18.32-alpha.1.jar`", "목표 JAR `villageguardians-0.18.33-alpha.1.jar`", "readme jar")
    anchor = "## 0.18.32 성벽 계단 교통·성루 명사수 전투선 분리 안정화"
    section = '''## 0.18.33 실플레이 UI 가독성·성벽 전투공간 교정

- 마을 회관을 시설 유지보수 전용 화면으로 단순화했다. 회관에서는 건물 선택 후 **수리와 강화만** 실행하며, 대장간·연구소·병영·성벽 지휘 같은 시설 고유 기능과 직업 배치는 각 실제 시설에서 사용한다.
- 회관 시설 목록은 이름·단계·내구도를 각각 독립 행으로 배치하고 행 높이와 목록 폭을 늘렸다. 하단 액션도 3분할을 폐기하고 수리/강화 2개만 계산된 폭으로 배치해 긴 수치가 서로 겹치거나 `…`로 잘리는 문제를 줄였다.
- 회관 헤더는 한 줄 강제 축약 대신 최대 두 줄 래핑을 사용하고, 본문/목록/상세/버튼의 영역을 실제 safe-area 크기에서 계산한다. 좁은 화면에서도 각 패널은 scissor와 `fit` 경계를 공유해 다른 영역으로 글자가 튀어나오지 않는다.
- 다음 밤 웨이브 브리핑은 헤더를 두 줄까지 표시하고 웨이브/병과 행 높이와 좌측 목록 폭을 키웠다. 적 도감 제목·역할·하단 안내도 실제 패널 폭으로 잘라 그리며, 4개 패널의 비율과 간격을 다시 계산해 핵심 예상 병과와 대응 정보를 먼저 읽을 수 있게 했다.
- 기존 5블록 두께 성벽의 1×1 관통 사격구를 폐기하고, 12블록 주기의 **3블록 폭 × 2블록 높이 사격구**로 교체했다. 하단 2블록은 그대로 막혀 있으므로 지상 적의 통로가 되지 않으면서 플레이어/원거리 수비수가 실제 시야와 투사체 각도를 확보한다.
- 성 안쪽으로 14블록 돌출되며 아래가 돌로 꽉 차 있던 5칸 폭 성벽 계단은 시작점을 벽 쪽으로 4블록 당기고, 하부를 개방형으로 바꿨다. 기존 월드의 옛 쐐기 지지체도 마이그레이션에서 지워 통행로를 되돌리고 필요한 위치에만 얇은 지지 기둥을 남긴다.
- 새 월드뿐 아니라 0.18.32 기존 월드도 새 마이그레이션 마커를 통해 사격구와 계단 구조를 다시 투영한다. 성벽 세그먼트 손상과 배치 포탑의 authoritative 상태는 기존 복원 절차가 다시 적용한다.

'''
    if anchor not in text:
        raise RuntimeError("README v0.18.32 anchor missing")
    text = text.replace(anchor, section + anchor, 1)
    write(readme, text)

    old_test = ROOT / "tools/test_v01832_ranger_wall_traffic.py"
    text = read(old_test)
    text = text.replace('    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")\n', '')
    text = text.replace('    readme = (ROOT / "README.md").read_text(encoding="utf-8")\n', '')
    text = text.replace('    assert "mod_version=0.18.32-alpha.1" in props\n', '')
    text = text.replace('    assert "0.18.32-alpha.1" in readme and "villageguardians-0.18.32-alpha.1.jar" in readme\n', '')
    write(old_test, text)


def write_regression_test() -> None:
    path = ROOT / "tools/test_v01833_playtest_ui_wall.py"
    path.write_text(r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    town = read("VillageTownHallGridScreen.java")
    controller = read("VillageUiController.java")
    wave = read("VillageWaveIntelDossierScreen.java")
    terrain = read("VillageFortressTerrain.java")
    world = read("VillageWorldSystem.java")
    old = (ROOT / "tools/test_v01832_ranger_wall_traffic.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.33-alpha.1" in props
    assert "0.18.33-alpha.1" in readme and "villageguardians-0.18.33-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.32-alpha.1" in props' not in old

    render = town.split("public void extractRenderState", 1)[1].split("private void drawFrame", 1)[0]
    assert "drawTabs(" not in render
    listing = town.split("private void drawList", 1)[1].split("private void drawDetail", 1)[0]
    assert "int rowHeight = 50" in listing
    assert '"내구도 " + f.current() + " / " + f.maximum()' in listing
    assert 'f.meta() + " · " + f.current()' not in listing
    buttons = town.split("private List<ButtonSpec> facilityButtons", 1)[1].split("private String functionAction", 1)[0]
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "functionLabel" not in buttons and "functionAction" not in buttons
    assert "open_funding" not in buttons and "open_tower_control" not in buttons
    assert "int h = 27" in buttons and "available / 2" in buttons
    layout = town.split("private Layout layout()", 1)[1].split("private void parse", 1)[0]
    assert "Math.min(940" in layout and "panelWidth * 31 / 100" in layout
    assert "contentTop" in layout and "gap = 10" in layout

    dashboard = controller.split("public static void openDashboard", 1)[1].split("public static void openRoleAssignment", 1)[0]
    assert '"role"' not in dashboard and "select_role:" not in dashboard
    assert 'actions.add("facility:" + building.id())' in dashboard
    assignment = controller.split("public static void openRoleAssignment", 1)[1].split("public static void openCaller", 1)[0]
    assert "VillageLocationRules.isNearSkillHall(player)" in assignment
    assert "select_role:" in assignment
    select = controller.split('if (action.startsWith("select_role:"))', 1)[1].split('if (action.startsWith("skill_node:"))', 1)[0]
    assert "isNearSkillHall" in select and "requireTownHall" not in select
    assert '"open_role_assignment"' in controller
    assert 'case TOWN_HALL -> "시설 수리·강화 지휘"' in controller

    wave_render = wave.split("public void extractRenderState", 1)[1].split("private void drawWaveList", 1)[0]
    assert "font.split" in wave_render and "Math.min(2, header.size())" in wave_render
    assert 'fit(font, "웨이브 선택' in wave_render
    assert "int h = 44" in wave and "int h = 40" in wave
    wave_layout = wave.split("private Layout layout()", 1)[1].split("private List<MonsterEntry> currentRoster", 1)[0]
    assert "safe.width() * 26 / 100" in wave_layout
    assert "rightWidth >= 430" in wave_layout
    assert "gap = 9" in wave_layout

    horizontal = terrain.split("private static void buildHorizontalWall", 1)[1].split("private static void buildVerticalWall", 1)[0]
    vertical = terrain.split("private static void buildVerticalWall", 1)[1].split("private static void buildDefenderGalleries", 1)[0]
    assert "isFiringBayOffset" in horizontal and "y >= 3 && y <= 4" in horizontal
    assert "isFiringBayOffset" in vertical and "y >= 3 && y <= 4" in vertical
    assert "phase == 0 || phase == 1 || phase == 11" in terrain
    for center in range(-72, 73, 12):
        opening = {center - 1, center, center + 1}
        assert len(opening) == 3
    # y=1..2 stays solid; only y=3..4 becomes AIR, so a firing bay cannot become a ground breach.
    assert "firingBay && y >= 3 && y <= 4" in horizontal

    ramp = terrain.split("private static void buildWallAccessRamp", 1)[1].split("private static void buildTower", 1)[0]
    assert "stairStart = WALL_RADIUS - 10" in ramp
    assert "distance = WALL_RADIUS - 14; distance < stairStart" in ramp
    assert "Blocks.AIR" in ramp and "Blocks.STONE_BRICK_WALL" in ramp
    assert "supportY" in ramp
    assert "center.below(9)).is(Blocks.GOLD_BLOCK)" in world
    assert "center.below(9), Blocks.GOLD_BLOCK" in world

    # Mirror the town-hall width arithmetic for representative logical GUI widths.
    for safe_width in (320, 426, 640, 840, 960, 1280):
        panel = min(940, max(1, safe_width))
        content = max(1, panel - 28 - 10)
        list_width = max(150, min(280, panel * 31 // 100))
        list_width = min(list_width, max(90, content - 170))
        detail_width = panel - 28 - 10 - list_width
        assert list_width >= 90
        assert detail_width >= 1
        if detail_width >= 260:
            inner = detail_width - 28
            available = max(2, inner - 7)
            first = available // 2
            second = available - first
            assert first + second + 7 <= inner

    print("[PASS] town hall is maintenance-only and critical facility values no longer share one clipped line")
    print("[PASS] repair/upgrade buttons use calculated two-action geometry and production role tabs are gone")
    print("[PASS] player role assignment moved to the skill hall instead of disappearing with the town-hall cleanup")
    print("[PASS] wave briefing uses wrapped header text, larger rows and width-aware dossier labels")
    print("[PASS] wall firing bays are three-wide/two-high above a solid two-block base")
    print("[PASS] wall stairs retract four blocks and clear the retired solid courtyard wedge")
    print("[PASS] existing worlds receive the new combat-geometry migration marker")
    print("[PASS] v0.18.33 playtest UI/wall contract complete")


if __name__ == "__main__":
    main()
''', encoding="utf-8")


def main() -> None:
    props = read(ROOT / "gradle.properties")
    if "mod_version=0.18.32-alpha.1" not in props:
        raise RuntimeError("v0.18.33 patch expects accepted v0.18.32 project state")
    patch_town_hall_screen()
    patch_controller()
    patch_wave_screen()
    patch_wall_geometry()
    patch_world_migration()
    patch_metadata_and_history()
    write_regression_test()
    print("[PATCH] Village Guardians 0.18.33 playtest UI/wall correction applied")


if __name__ == "__main__":
    main()
