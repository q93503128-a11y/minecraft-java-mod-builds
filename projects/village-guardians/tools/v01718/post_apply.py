#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 marker, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Keep source comments free of the retired event class name so source contracts
# distinguish implementation symbols from explanatory prose.
replace_once(
    JAVA / "VillageRoleAbilitySystem.java",
    "        // unlike only changing LivingEntityUseItemEvent.Tick duration.\n",
    "        // unlike only changing a duration value inside an item-use tick event.\n",
    "rapid draw comment",
)

replace_once(
    TOOLS / "test_runtime_safety.py",
    '    assert "B 통신 · Z/X 기술" in inventory\n'
    '    for key in ("GLFW_KEY_Z", "GLFW_KEY_X", "GLFW_KEY_B", "GLFW_KEY_H", "GLFW_KEY_J", "GLFW_KEY_K", "GLFW_KEY_U"):\n'
    '        assert key in keys\n',
    '    assert "VillageClientKeys.compactSummary()" in inventory\n'
    '    for key in ("GLFW_KEY_Z", "GLFW_KEY_X", "GLFW_KEY_B", "GLFW_KEY_H", "GLFW_KEY_J", "GLFW_KEY_K"):\n'
    '        assert key in keys\n'
    '    assert "GLFW_KEY_U" not in keys and "CALLER" not in keys\n',
    "runtime shortcut contract",
)

replace_once(
    TOOLS / "test_v01717_range_homing.py",
    '    assert "LivingEntityUseItemEvent.Tick" in guardians\n'
    '    assert "handleUseItemTick" in ability\n'
    '    assert "event.setDuration" in ability\n'
    '    assert "instanceof BowItem" in ability and "instanceof CrossbowItem" in ability\n'
    '    assert "event.setCharge(20)" in ability\n'
    '    print("[PASS] 신속 삼연사가 실제 활·석궁 사용시간과 발사 충전량을 가속합니다")\n',
    '    assert "LivingEntityUseItemEvent" not in guardians\n'
    '    assert "handleUseItemTick" not in ability\n'
    '    assert "tickRapidBow" in ability\n'
    '    assert "player.releaseUsingItem()" in ability\n'
    '    assert "event.setCharge(20)" in ability\n'
    '    print("[PASS] 신속 삼연사가 실제 활 사용을 조기에 종료해 완충 발사를 실행합니다")\n',
    "v01717 bow contract",
)

replace_once(
    TOOLS / "test_v0175_gameplay_ui.py",
    '    assert \'consume(CALLER, "open_quick_chat")\' in read("VillageClientKeys.java")\n',
    '    key_source = read("VillageClientKeys.java")\n'
    '    assert \'consume(QUICK_COMMUNICATION, "open_quick_chat")\' in key_source\n'
    '    assert "CALLER" not in key_source and "GLFW.GLFW_KEY_U" not in key_source\n',
    "v0175 caller contract",
)

role_test = TOOLS / "test_v01712_role_abilities.py"
text = role_test.read_text(encoding="utf-8")
text = text.replace(
    "[PASS] Default shortcut help matches Z/X/B/H/J/K/U registrations",
    "[PASS] Default shortcuts match Z/X/B/H/J/K with no obsolete U duplicate",
)
role_test.write_text(text, encoding="utf-8")

# apply.py intentionally migrates old B/U display phrases, but the same broad
# replacement also changes the new regression assertion from "B/U not in" to
# "B not in". Restore the assertion so the valid B shortcut is not rejected.
v01718_test = TOOLS / "test_v01718_bow_shortcuts.py"
v01718_source = v01718_test.read_text(encoding="utf-8")
v01718_source = v01718_source.replace(
    '        and "B" not in starter\n',
    '        and "B/U" not in starter\n',
)
v01718_test.write_text(v01718_source, encoding="utf-8")

# Whole-file cleanup for every obsolete caller-era shortcut phrase.  This is
# deliberately broader than the source patch above so future nearby wording
# changes cannot leave B/U or U-only help behind.
starter_path = JAVA / "VillageStarterKit.java"
starter = starter_path.read_text(encoding="utf-8")
starter = starter.replace("B/U 키", "B 키")
starter = starter.replace("B/U", "B")
starter = starter.replace(" · U 빠른 통신", "")
starter = starter.replace(
    "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장",
    "현재 단축키는 설정 > 조작 > 마을 지키기에서 확인하거나 변경하세요.",
)
starter_path.write_text(starter, encoding="utf-8")

controller_path = JAVA / "VillageUiController.java"
controller = controller_path.read_text(encoding="utf-8")
controller = controller.replace("B/U", "B")
controller = controller.replace(" · U 빠른 통신", "")
controller = controller.replace(
    "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장",
    "단축키는 설정 > 조작 > 마을 지키기에서 현재 지정 키를 확인·변경",
)
controller_path.write_text(controller, encoding="utf-8")

for path, forbidden in [
    (starter_path, ("B/U", "기본키 Z", "U 빠른 통신")),
    (controller_path, ("B/U", "기본키 Z", "U 빠른 통신")),
]:
    source = path.read_text(encoding="utf-8")
    leftovers = [token for token in forbidden if token in source]
    if leftovers:
        raise SystemExit(f"obsolete shortcut text remains in {path.name}: {leftovers}")

if "설정 > 조작 > 마을 지키기" not in starter_path.read_text(encoding="utf-8"):
    raise SystemExit("starter controls-settings guidance missing")
if "설정 > 조작 > 마을 지키기" not in controller_path.read_text(encoding="utf-8"):
    raise SystemExit("status controls-settings guidance missing")

print("Migrated Village Guardians v0.17.18 shortcut and bow contracts")
