#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all(path: Path, old: str, new: str, minimum: int, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count < minimum:
        raise RuntimeError(f"{label}: expected at least {minimum} matches in {path}, found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    props = ROOT / "gradle.properties"
    replace_once(props, "mod_version=0.18.33-alpha.1", "mod_version=0.18.34-alpha.1", "version")

    role = JAVA / "VillageRoleSkillSystem.java"
    replace_once(role,
        'return "마을 회관에서 직업을 먼저 배치해야 합니다.";',
        'return "기술 연구소에서 직업을 먼저 배치해야 합니다.";',
        "role assignment stale message")

    deploy = JAVA / "VillageMercenaryDeploymentSystem.java"
    replace_once(deploy,
'''    public static boolean canOpenAt(ServerPlayer player) {
        return player != null
                && (VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)
                || VillageLocationRules.isNearTownHall(player));
    }
''',
'''    public static boolean canOpenAt(ServerPlayer player) {
        return player != null
                && VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS);
    }
''', "mercenary command ownership")
    replace_once(deploy,
        'player.sendSystemMessage(Component.literal("§c용병 지휘는 병영 또는 마을 회관에서만 가능합니다."));',
        'player.sendSystemMessage(Component.literal("§c용병 지휘는 병영 단말기 근처에서만 가능합니다."));',
        "mercenary command message")
    replace_once(deploy,
'''    public static void openClass(ServerPlayer player, VillageMercenarySystem.MercenaryClass kind) {
        if (kind == null) { openCommand(player); return; }
''',
'''    public static void openClass(ServerPlayer player, VillageMercenarySystem.MercenaryClass kind) {
        if (!canOpenAt(player)) {
            player.sendSystemMessage(Component.literal("§c용병 병과 관리는 병영 단말기 근처에서만 가능합니다."));
            return;
        }
        if (kind == null) { openCommand(player); return; }
''', "mercenary class direct-open guard")
    replace_once(deploy,
        'if (!canOpenAt(player)) return "용병 배치는 병영 또는 마을 회관 근처에서만 변경할 수 있습니다.";',
        'if (!canOpenAt(player)) return "용병 배치는 병영 단말기 근처에서만 변경할 수 있습니다.";',
        "mercenary deployment message")

    merc = JAVA / "VillageMercenarySystem.java"
    replace_once(merc,
'''        if (!VillageLocationRules.isNearTownHall(player)
                && !VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
            return "용병 퇴역은 병영 또는 마을 회관 근처에서만 가능합니다.";
        }
''',
'''        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
            return "용병 퇴역은 병영 단말기 근처에서만 가능합니다.";
        }
''', "mercenary retirement ownership")

    local = JAVA / "VillageLocalActionSystem.java"
    replace_once(local,
'''        if (requiresSiegeCommandAccess(action) && !VillageLocationRules.isNearTownHall(player)) {
            player.sendSystemMessage(Component.literal("§c성벽·포탑 관리 동작은 마을 회관 지휘대 근처에서만 실행할 수 있습니다."));
            return true;
        }

        if (action.startsWith("facility:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring("facility:".length()));
            if (building == null) {
                player.sendSystemMessage(Component.literal("§c알 수 없는 시설입니다."));
            } else if (building == VillageProgressionSystem.Building.WALLS) {
                VillageSiegeCommandUi.open(player);
            } else {
                VillageUiController.openBuilding(player, building);
            }
            return true;
        }
''',
'''        if (isSiegeCommandAction(action)
                && !VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) {
            player.sendSystemMessage(Component.literal("§c성벽·포탑 지휘는 북문 성벽 지휘 레버 근처에서만 실행할 수 있습니다."));
            return true;
        }

        if (action.startsWith("facility:") || action.startsWith("manage:")) {
            player.sendSystemMessage(Component.literal(
                    "§c구식 시설 바로가기는 폐기되었습니다. 회관은 수리·강화만, 고유 기능은 각 시설 단말기에서 사용하세요."));
            return true;
        }
''', "local facility and siege ownership")
    replace_once(local,
        'player.sendSystemMessage(Component.literal("§c용병 배치는 병영 또는 마을 회관 근처에서만 변경할 수 있습니다."));',
        'player.sendSystemMessage(Component.literal("§c용병 배치는 병영 단말기 근처에서만 변경할 수 있습니다."));',
        "local merc deployment message")
    replace_once(local,
        'player.sendSystemMessage(Component.literal("§c용병 퇴역은 병영 또는 마을 회관 근처에서만 가능합니다."));',
        'player.sendSystemMessage(Component.literal("§c용병 퇴역은 병영 단말기 근처에서만 가능합니다."));',
        "local merc retire message")
    replace_once(local,
'''    private static boolean requiresSiegeCommandAccess(String action) {
        return action.equals("siege_turret_repair_all")
                || action.startsWith("siege_segment_repair:")
                || action.startsWith("siege_segment_upgrade:")
                || action.startsWith("siege_turret_select:")
                || action.startsWith("siege_turret_repair:")
                || action.startsWith("siege_turret_upgrade:")
                || action.startsWith("siege_turret_dismantle:");
    }
''',
'''    private static boolean isSiegeCommandAction(String action) {
        return action.equals("siege_command")
                || action.equals("siege_turret_catalog")
                || action.equals("siege_turret_list")
                || action.equals("siege_turret_repair_all")
                || action.equals("siege_turret_cancel")
                || action.equals("open_tower_control")
                || action.equals("tower_status")
                || action.startsWith("tower_open:")
                || action.startsWith("tower_branch:")
                || action.startsWith("tower_upgrade:")
                || action.startsWith("siege_segment_open:")
                || action.startsWith("siege_segment_repair:")
                || action.startsWith("siege_segment_upgrade:")
                || action.startsWith("siege_turret_select:")
                || action.startsWith("siege_turret_open:")
                || action.startsWith("siege_turret_repair:")
                || action.startsWith("siege_turret_upgrade:")
                || action.startsWith("siege_turret_dismantle:");
    }
''', "complete siege action gate")

    enhance = JAVA / "VillageBuildingEnhancements.java"
    replace_once(enhance,
'''            if (building == VillageProgressionSystem.Building.TOWN_HALL
                    || building == VillageProgressionSystem.Building.WALLS) {
                continue;
            }
''',
'''            if (building == VillageProgressionSystem.Building.TOWN_HALL) {
                continue;
            }
''', "wall terminal routing")

    controller = JAVA / "VillageUiController.java"
    replace_once(controller,
        'actions.add("facility:" + building.id());',
        'actions.add("facility_card:" + building.id());',
        "town hall non-executable facility card")
    replace_once(controller,
'''        if (action.startsWith("manage:") || action.startsWith("facility:")) {
            openDashboard(player);
            return true;
        }
''',
'''        if (action.startsWith("manage:") || action.startsWith("facility:")) {
            player.sendSystemMessage(Component.literal(
                    "§c구식 시설 바로가기는 폐기되었습니다. 각 시설의 고유 기능은 해당 시설 단말기에서 사용하세요."));
            return true;
        }
''', "controller legacy facility rejection")
    replace_once(controller,
'''    public static void openMercenaryCommand(ServerPlayer player) {
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
''',
'''    public static void openMercenaryCommand(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
            player.sendSystemMessage(Component.literal("§c용병 지휘는 병영 단말기 근처에서만 가능합니다."));
            return;
        }
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
''', "legacy mercenary UI direct-open guard")
    replace_once(controller,
'''            case WALLS -> add(actions, labels,
                    "open_wave_intel", "다음 웨이브 정찰|예상 병과·특성·보스 확인");
''',
'''            case WALLS -> add(actions, labels,
                    "siege_command", "성벽·포탑 지휘|성벽 구역 상태와 10계열 포탑 배치·수리·강화·철거",
                    "open_wave_intel", "다음 웨이브 정찰|예상 병과·특성·보스 확인");
''', "wall local siege entry")
    replace_once(controller,
        'case WALLS -> "현장에서는 정찰만 확인합니다. 수리·강화·포탑 건설은 회관에서 진행합니다.";',
        'case WALLS -> "북문 성벽 지휘 레버에서 정찰과 포탑 배치·수리·강화·철거를 관리합니다. 시설 내구도 수리·강화는 회관에서도 관리할 수 있습니다.";',
        "wall local description")

    siege_ui = JAVA / "VillageSiegeCommandUi.java"
    replace_once(siege_ui,
        '/** Town-hall command surface for wall segments and player-placed turrets. */',
        '/** Wall-command surface for local segment and player-placed turret management. */',
        "siege UI ownership comment")
    replace_all(siege_ui, "nearTownHall(player)", "nearWallCommand(player)", 5, "siege UI guards")
    replace_once(siege_ui,
'''        actions.add("open_dashboard");
        labels.add("회관 전체 지휘|건물·직업·다른 시설 관리로 돌아가기");
''', "", "remove town hall return from wall command")
    replace_once(siege_ui,
'''    private static boolean nearTownHall(ServerPlayer player) {
        if (VillageLocationRules.isNearTownHall(player)) return true;
        player.sendSystemMessage(Component.literal("§c성벽·포탑 지휘는 마을 회관 지휘대 근처에서만 가능합니다."));
        return false;
    }
''',
'''    private static boolean nearWallCommand(ServerPlayer player) {
        if (VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) return true;
        player.sendSystemMessage(Component.literal("§c성벽·포탑 지휘는 북문 성벽 지휘 레버 근처에서만 가능합니다."));
        return false;
    }
''', "siege UI wall guard")

    segments = JAVA / "VillageSiegeSegmentSystem.java"
    replace_once(segments,
'''    public static String repair(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        String blocked = VillageMaintenanceRules.blockReason("성벽 수리");
''',
'''    public static String repair(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) {
            return "성벽 구역 수리는 북문 성벽 지휘 레버 근처에서만 가능합니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("성벽 수리");
''', "segment repair leaf location")
    replace_once(segments,
'''    public static String upgrade(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        String blocked = VillageMaintenanceRules.blockReason("성벽 강화");
''',
'''    public static String upgrade(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) {
            return "성벽 구역 강화는 북문 성벽 지휘 레버 근처에서만 가능합니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("성벽 강화");
''', "segment upgrade leaf location")

    turrets = JAVA / "VillagePlacedTurretSystem.java"
    replace_once(turrets,
'''    public static String selectPlacement(ServerPlayer player, TurretType type) {
        if (type == null) return "알 수 없는 포탑 계열입니다.";
        String blocked = VillageMaintenanceRules.blockReason("포탑 배치");
''',
'''    public static String selectPlacement(ServerPlayer player, TurretType type) {
        if (type == null) return "알 수 없는 포탑 계열입니다.";
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) {
            return "포탑 배치 지휘는 북문 성벽 지휘 레버 근처에서만 시작할 수 있습니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("포탑 배치");
''', "turret select leaf location")
    replace_once(turrets,
'''        PendingPlacement pending = PENDING.get(player.getUUID());
        if (pending == null) return false;
        event.setCanceled(true);
        BlockPos candidate = event.getPos().above();
''',
'''        PendingPlacement pending = PENDING.get(player.getUUID());
        if (pending == null) return false;
        event.setCanceled(true);
        String blocked = VillageMaintenanceRules.blockReason("포탑 배치");
        if (blocked != null) {
            PENDING.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§c" + blocked + " 배치 모드를 취소했습니다."));
            return true;
        }
        BlockPos candidate = event.getPos().above();
''', "turret placement confirm phase recheck")
    for signature, action_name in (
        ("public static synchronized String repair(ServerPlayer player, int id) {", "포탑 수리"),
        ("public static synchronized String upgrade(ServerPlayer player, int id) {", "포탑 강화"),
        ("public static synchronized String dismantle(ServerPlayer player, int id) {", "포탑 철거"),
        ("public static synchronized String repairAll(ServerPlayer player) {", "포탑 일괄 수리"),
    ):
        replace_once(turrets,
            f'''    {signature}\n        String blocked = VillageMaintenanceRules.blockReason("{action_name}");\n''',
            f'''    {signature}\n        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.WALLS)) {{\n            return "{action_name}는 북문 성벽 지휘 레버 근처에서만 가능합니다.";\n        }}\n        String blocked = VillageMaintenanceRules.blockReason("{action_name}");\n''',
            f"{action_name} leaf location")

    old_test = ROOT / "tools/test_v01833_playtest_ui_wall.py"
    replace_once(old_test,
'''    assert "mod_version=0.18.33-alpha.1" in props
    assert "0.18.33-alpha.1" in readme and "villageguardians-0.18.33-alpha.1.jar" in readme
''',
'''    assert "mod_version=" in props
    assert "현재 소스 버전" in readme and "목표 JAR" in readme
''', "historical v01833 version ownership")
    replace_once(old_test,
        'assert \'actions.add("facility:" + building.id())\' in dashboard',
        'assert \'actions.add("facility_card:" + building.id())\' in dashboard',
        "historical town hall card contract")

    readme = ROOT / "README.md"
    replace_once(readme, "- 현재 소스 버전 `0.18.33-alpha.1`", "- 현재 소스 버전 `0.18.34-alpha.1`", "readme version")
    replace_once(readme, "- 목표 JAR `villageguardians-0.18.33-alpha.1.jar`", "- 목표 JAR `villageguardians-0.18.34-alpha.1.jar`", "readme jar")
    marker = "## 핵심 루프\n\n낮 정비 → 장비·성장·시설·포탑·용병 준비 → 다음 밤 정찰 → 공성전 → 피해 복구·강화 → 다음 날짜로 진행한다.\n"
    section = marker + "\n## 0.18.34 Codex 후처리 · 시설 소유권/서버 leaf 정합\n\n- 회관은 시설 수리·강화 전용으로 유지하고, 용병 지휘는 병영, 성벽·포탑 지휘는 북문 성벽 지휘 레버가 단독 소유한다.\n- 회관 시설 목록은 실행 가능한 `facility:*` 대신 표시 전용 `facility_card:*`를 사용하며 구식 `facility:`/`manage:` 직접 액션은 서버에서 거부한다.\n- 성벽 지휘 레버를 실제 WALLS 현장 단말기로 라우팅하고 성벽 현장 UI에서 공성/포탑 지휘 화면에 정상 진입할 수 있게 복구한다.\n- 용병 병과 화면·배치·퇴역과 포탑/성벽 변경 작업은 각 실제 시설 위치를 leaf 메서드에서 다시 검증한다.\n- 낮에 포탑 배치 모드를 켜고 밤에 설치를 확정하던 시간차 우회를 막기 위해 두 번째 우클릭 확정 시점에도 정비 단계를 재검증한다.\n- 직업 미배치 기술 안내는 옛 회관 안내를 제거하고 실제 직업 배치 위치인 기술 연구소를 가리킨다.\n"
    replace_once(readme, marker, section, "readme v01834 section")

    report = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
    replace_once(report, "- Current source version: `0.18.33-alpha.1`", "- Current source version: `0.18.34-alpha.1`", "report version")
    replace_once(report, "- Target JAR: `villageguardians-0.18.33-alpha.1.jar`", "- Target JAR: `villageguardians-0.18.34-alpha.1.jar`", "report jar")

    test = ROOT / "tools/test_v01834_post_codex_followup.py"
    test.write_text('''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    controller = read("VillageUiController.java")
    town = read("VillageTownHallGridScreen.java")
    local = read("VillageLocalActionSystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    merc = read("VillageMercenarySystem.java")
    role = read("VillageRoleSkillSystem.java")
    enhance = read("VillageBuildingEnhancements.java")
    router = read("VillageBuildingInteractionRouter.java")
    siege = read("VillageSiegeCommandUi.java")
    segments = read("VillageSiegeSegmentSystem.java")
    turrets = read("VillagePlacedTurretSystem.java")

    assert "mod_version=0.18.34-alpha.1" in props

    dashboard = section(controller, "public static void openDashboard", "public static void openRoleAssignment")
    assert 'actions.add("facility_card:" + building.id())' in dashboard
    assert 'actions.add("facility:" + building.id())' not in dashboard
    buttons = section(town, "private List<ButtonSpec> facilityButtons", "private void drawButton")
    assert '"repair:" + f.id()' in buttons and '"upgrade:" + f.id()' in buttons
    assert "siege_command" not in buttons and "open_mercenary_command" not in buttons

    assert "VillageLocationRules.isNearTownHall(player)" not in deploy
    assert "Building.BARRACKS" in section(deploy, "public static boolean canOpenAt", "public static void openCommand")
    open_class = section(deploy, "public static void openClass", "public static String setDeployment")
    assert "if (!canOpenAt(player))" in open_class
    assert "병영 또는 마을 회관" not in deploy
    retire = section(merc, "public static synchronized String retire", "private static void bastionControl")
    assert "isNearTownHall" not in retire and "Building.BARRACKS" in retire
    assert "병영 또는 마을 회관" not in retire

    assert "마을 회관에서 직업을 먼저 배치" not in role
    assert "기술 연구소에서 직업을 먼저 배치" in role

    legacy = section(local, 'if (action.startsWith("facility:")', "// Compatibility guard")
    assert "구식 시설 바로가기" in legacy and "openBuilding" not in legacy and "VillageSiegeCommandUi.open" not in legacy
    gate = section(local, "private static boolean isSiegeCommandAction", "private static int parseInt")
    for token in ("siege_command", "siege_turret_catalog", "siege_segment_open:", "siege_turret_open:", "tower_open:"):
        assert token in gate
    assert "Building.WALLS" in local
    assert "requiresSiegeCommandAccess" not in local

    terminal = section(enhance, "static VillageProgressionSystem.Building buildingAtTerminal", "static void reinforceWallRailings")
    assert "Building.TOWN_HALL" in terminal
    assert "Building.WALLS" not in terminal
    assert "buildingAtTerminal" in router and "openBuilding(player, building)" in router
    wall_actions = section(controller, "private static void fillLocalActions", "private static String localDescription")
    assert 'case WALLS -> add(actions, labels,' in wall_actions and '"siege_command"' in wall_actions
    wall_desc = section(controller, "private static String localDescription", "private static String managementEffect")
    assert "포탑 건설은 회관" not in wall_desc and "성벽 지휘 레버" in wall_desc

    assert "Town-hall command surface" not in siege
    assert "nearTownHall" not in siege
    assert "nearWallCommand" in siege and "Building.WALLS" in siege
    assert 'actions.add("open_dashboard")' not in siege

    segment_repair = section(segments, "public static String repair", "public static String upgrade")
    segment_upgrade = section(segments, "public static String upgrade", "public static BlockPos attackPoint")
    assert "Building.WALLS" in segment_repair and "Building.WALLS" in segment_upgrade

    for start, end in (
        ("public static String selectPlacement", "public static boolean handlePlacementClick"),
        ("public static synchronized String repair(ServerPlayer player, int id)", "public static synchronized String upgrade"),
        ("public static synchronized String upgrade(ServerPlayer player, int id)", "public static synchronized String dismantle"),
        ("public static synchronized String dismantle(ServerPlayer player, int id)", "public static synchronized String repairAll"),
        ("public static synchronized String repairAll(ServerPlayer player)", "public static void tick"),
    ):
        assert "Building.WALLS" in section(turrets, start, end)
    placement = section(turrets, "public static boolean handlePlacementClick", "public static String cancelPlacement")
    assert 'VillageMaintenanceRules.blockReason("포탑 배치")' in placement
    assert "PENDING.remove(player.getUUID())" in placement

    print("[PASS] town hall emits display-only facility cards and exposes only repair/upgrade actions")
    print("[PASS] mercenary ownership is barracks-only from UI entry through retirement/deployment leaves")
    print("[PASS] wall command lever is the sole normal siege/turret command entry and legacy facility actions are rejected")
    print("[PASS] segment/turret mutation leaves revalidate wall-command location")
    print("[PASS] turret placement confirmation revalidates daytime maintenance phase")
    print("[PASS] stale role guidance now points to the skill hall")

if __name__ == "__main__":
    main()
''', encoding="utf-8")

    print("[PATCH] v0.18.34 post-Codex follow-up applied")


if __name__ == "__main__":
    main()
