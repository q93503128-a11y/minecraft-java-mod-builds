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

props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.8-alpha.1", "mod_version=0.17.9-alpha.1", "version")
write(props, text)

path = JAVA / "VillageSkillTestSystem.java"
text = read(path)
text = replace_once(
    text,
    "import net.minecraft.network.chat.Component;\n",
    "import net.minecraft.network.chat.Component;\n"
    "import net.minecraft.world.InteractionHand;\n"
    "import net.minecraft.world.InteractionResult;\n",
    "skill-test interaction imports",
)
text = replace_once(
    text,
    "import net.minecraft.world.phys.Vec3;\n",
    "import net.minecraft.world.phys.Vec3;\n"
    "import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;\n",
    "skill-test event import",
)
text = replace_once(
    text,
    "    private static final Map<String, String> TEST_LOADOUTS = new HashMap<>();\n",
    "    private static final Map<String, String> TEST_LOADOUTS = new HashMap<>();\n"
    "    private static final Map<UUID, VillageRole> TEST_ROLES = new HashMap<>();\n",
    "test role field",
)
text = replace_once(
    text,
    "        TEST_LOADOUTS.clear();\n",
    "        TEST_LOADOUTS.clear();\n"
    "        TEST_ROLES.clear();\n",
    "clear test roles on init",
)
text = replace_once(
    text,
    "        ENABLED.add(player.getUUID());\n"
    "        ensureDefaultLoadout(player);\n\n"
    "        BlockPos start = arena.offset(0, 0, 12);\n",
    "        ENABLED.add(player.getUUID());\n"
    "        selectedRole(player);\n"
    "        ensureDefaultLoadout(player);\n\n"
    "        BlockPos start = arena.offset(0, 0, 11);\n",
    "enable default role and start",
)
text = replace_once(
    text,
    "        return \"외부 기술 시험장으로 이동했습니다.\"\n"
    "                + \"\\nZ/X에 임시 장착한 기술을 실제 입력으로 사용해 모션과 판정을 확인하세요.\"\n"
    "                + \"\\nK를 누르면 시험 장착 메뉴를 다시 엽니다. \" + targets;\n",
    "        return \"외부 기술 시험장으로 이동했습니다.\"\n"
    "                + \"\\n뒤쪽의 기술 시험 관리함을 열어 직업과 Z/X 기술을 변경할 수 있습니다.\"\n"
    "                + \"\\nK를 눌러도 같은 관리 화면을 엽니다. \" + targets;\n",
    "enable message",
)
text = replace_once(
    text,
    "        clearLoadout(player.getUUID());\n"
    "        ReturnPoint point = RETURN_POINTS.remove(player.getUUID());\n",
    "        clearLoadout(player.getUUID());\n"
    "        TEST_ROLES.remove(player.getUUID());\n"
    "        ReturnPoint point = RETURN_POINTS.remove(player.getUUID());\n",
    "disable test role",
)
marker = """    public static String equip(ServerPlayer player, String skillId, int slot) {
"""
insert = """    public static VillageRole selectedRole(ServerPlayer player) {
        if (player == null) return VillageRole.VANGUARD;
        VillageRole selected = TEST_ROLES.get(player.getUUID());
        if (selected != null) return selected;
        selected = VillageCouncilState.roleOf(player.getUUID()).orElse(VillageRole.VANGUARD);
        TEST_ROLES.put(player.getUUID(), selected);
        return selected;
    }

    public static String selectRole(ServerPlayer player, String roleId) {
        if (!isEnabled(player)) return "먼저 외부 기술 시험장을 활성화해야 합니다.";
        VillageRole role = VillageRole.parse(roleId).orElse(null);
        if (role == null) return "알 수 없는 시험 직업입니다.";
        TEST_ROLES.put(player.getUUID(), role);
        clearLoadout(player.getUUID());
        ensureDefaultLoadout(player);
        return role.displayName() + "을(를) 시험 직업으로 선택했습니다. 실제 직업과 성장 데이터는 바뀌지 않습니다.";
    }

    public static String equip(ServerPlayer player, String skillId, int slot) {
"""
text = replace_once(text, marker, insert, "insert test role methods")
text = replace_once(
    text,
    "        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);\n"
    "        if (skill == null || role == null || skill.role() != role) {\n",
    "        VillageRole role = selectedRole(player);\n"
    "        if (skill == null || skill.role() != role) {\n",
    "equip selected test role",
)
text = replace_once(
    text,
    "        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);\n"
    "        return VillageRoleSkillSystem.ActiveSkill.parse(\n"
    "                        TEST_LOADOUTS.get(loadoutKey(player.getUUID(), slot == 1 ? 1 : 0)))\n"
    "                .filter(skill -> role != null && skill.role() == role);\n",
    "        VillageRole role = selectedRole(player);\n"
    "        return VillageRoleSkillSystem.ActiveSkill.parse(\n"
    "                        TEST_LOADOUTS.get(loadoutKey(player.getUUID(), slot == 1 ? 1 : 0)))\n"
    "                .filter(skill -> skill.role() == role);\n",
    "equipped selected test role",
)
text = replace_once(
    text,
    "        TEST_LOADOUTS.clear();\n"
    "    }\n",
    "        TEST_LOADOUTS.clear();\n"
    "        TEST_ROLES.clear();\n"
    "    }\n",
    "clear all test roles",
)
text = replace_once(
    text,
    "        List<VillageRoleSkillSystem.ActiveSkill> skills = VillageCouncilState.roleOf(player.getUUID())\n"
    "                .map(VillageRoleSkillSystem::skillsFor).orElse(List.of());\n",
    "        List<VillageRoleSkillSystem.ActiveSkill> skills =\n"
    "                VillageRoleSkillSystem.skillsFor(selectedRole(player));\n",
    "default loadout selected role",
)
arena_marker = """    private static BlockPos arenaCenter() {
"""
arena_insert = """    public static boolean handleManagementBox(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel)
                || !isEnabled(player)) return false;
        BlockPos box = managementBoxPosition();
        if (box == null || !box.equals(event.getPos())) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiController.openSkillTest(player);
        return true;
    }

    public static BlockPos managementBoxPosition() {
        BlockPos arena = arenaCenter();
        return arena == null ? null : arena.offset(0, 0, 14);
    }

    private static BlockPos arenaCenter() {
"""
text = replace_once(text, arena_marker, arena_insert, "management box methods")
text = replace_once(
    text,
    "        for (int x = -2; x <= 2; x++) {\n"
    "            VillageFortressTerrain.set(level, center.offset(x, -1, 10), Blocks.GOLD_BLOCK);\n"
    "        }\n",
    "        for (int x = -2; x <= 2; x++) {\n"
    "            VillageFortressTerrain.set(level, center.offset(x, -1, 10), Blocks.GOLD_BLOCK);\n"
    "        }\n"
    "        BlockPos box = managementBoxPosition();\n"
    "        if (box != null) {\n"
    "            VillageFortressTerrain.set(level, box.below(), Blocks.GOLD_BLOCK);\n"
    "            VillageFortressTerrain.set(level, box, Blocks.BARREL);\n"
    "            VillageFortressTerrain.set(level, box.west(), Blocks.SEA_LANTERN);\n"
    "            VillageFortressTerrain.set(level, box.east(), Blocks.SEA_LANTERN);\n"
    "        }\n",
    "build management box",
)
write(path, text)

path = JAVA / "VillageRoleSkillSystem.java"
text = read(path)
old = """    public static synchronized Optional<ActiveSkill> equippedSkill(ServerPlayer player, int slot) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return Optional.empty();
        if (VillageSkillTestSystem.isEnabled(player)) {
            return VillageSkillTestSystem.equippedSkill(player, slot)
                    .filter(skill -> skill.role() == role);
        }
        return ActiveSkill.parse(EQUIPPED_SKILLS.get(loadoutKey(player.getUUID(), role, slot == 1 ? 1 : 0)))
                .filter(skill -> skill.role() == role && hasSkill(player, skill));
    }
"""
new = """    public static synchronized Optional<ActiveSkill> equippedSkill(ServerPlayer player, int slot) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            return VillageSkillTestSystem.equippedSkill(player, slot);
        }
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return Optional.empty();
        return ActiveSkill.parse(EQUIPPED_SKILLS.get(loadoutKey(player.getUUID(), role, slot == 1 ? 1 : 0)))
                .filter(skill -> skill.role() == role && hasSkill(player, skill));
    }
"""
text = replace_once(text, old, new, "role equipped skill test override")
old = """    public static String useEquippedSkill(ServerPlayer player, int slot) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "마을 회관에서 직업을 먼저 배치해야 합니다.";
        }
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) {
            return "기술 슬롯 " + (slot + 1) + "이 비어 있습니다. 직업 성장 화면에서 기술을 장착하세요.";
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return "현재 월드에서는 기술을 사용할 수 없습니다.";
        }
        boolean testing = VillageSkillTestSystem.isEnabled(player);
"""
new = """    public static String useEquippedSkill(ServerPlayer player, int slot) {
        boolean testing = VillageSkillTestSystem.isEnabled(player);
        VillageRole role = testing
                ? VillageSkillTestSystem.selectedRole(player)
                : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "마을 회관에서 직업을 먼저 배치해야 합니다.";
        }
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) {
            return testing
                    ? "시험 슬롯 " + (slot == 0 ? "Z" : "X") + "이 비어 있습니다. 시험 관리함에서 기술을 장착하세요."
                    : "기술 슬롯 " + (slot + 1) + "이 비어 있습니다. 직업 성장 화면에서 기술을 장착하세요.";
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return "현재 월드에서는 기술을 사용할 수 없습니다.";
        }
"""
text = replace_once(text, old, new, "role use selected test role")
text = replace_once(
    text,
    "        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);\n"
    "        if (skill == null || role == null || skill.role() != role) return \"현재 직업의 기술만 시험할 수 있습니다.\";\n",
    "        VillageRole role = VillageSkillTestSystem.selectedRole(player);\n"
    "        if (skill == null || skill.role() != role) return \"현재 시험 직업의 기술만 시험할 수 있습니다.\";\n",
    "legacy test cast selected role",
)
write(path, text)

path = JAVA / "VillageUiController.java"
text = read(path)
start = text.index("    public static void openSkillTest(ServerPlayer player) {")
end = text.index("    public static void openResult(ServerPlayer player, String title, String result, String returnAction) {")
replacement = """    public static void openSkillTest(ServerPlayer player) {
        boolean alreadyEnabled = VillageSkillTestSystem.isEnabled(player);
        if (!alreadyEnabled && !VillageLocationRules.isNearSkillHall(player)) {
            openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                    "open_role_skill_research");
            return;
        }
        String mode = alreadyEnabled
                ? "외부 시험장 활성화 · 뒤쪽 관리함 또는 K로 이 화면을 열 수 있습니다."
                : VillageSkillTestSystem.enable(player);
        if (!VillageSkillTestSystem.isEnabled(player)) {
            openResult(player, "기술 시험", mode, "open_role_skill_research");
            return;
        }

        VillageRole role = VillageSkillTestSystem.selectedRole(player);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRole candidate : VillageRole.values()) {
            actions.add("test_role:" + candidate.id());
            labels.add((candidate == role ? "선택됨 · " : "") + candidate.displayName()
                    + "|" + candidate.overview()
                    + "\\n시험 전용 직업만 변경하며 실제 직업·성장·저장값은 바뀌지 않습니다.");
        }
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            actions.add("test_equip:" + skill.id() + ":0");
            labels.add("Z · " + skill.displayName() + "|" + skill.description()
                    + "\\n한 번 클릭하면 Z 시험 슬롯에 즉시 장착합니다.");
            actions.add("test_equip:" + skill.id() + ":1");
            labels.add("X · " + skill.displayName() + "|" + skill.description()
                    + "\\n한 번 클릭하면 X 시험 슬롯에 즉시 장착합니다.");
        }
        add(actions, labels,
                "test_spawn", "시험 표적 재배치|외부 시험장 중앙에 체력·밀림 저항이 다른 표적 6개 생성",
                "test_clear", "시험 표적 정리|현재 내가 만든 시험 표적 제거",
                "test_exit", "시험 종료·복귀|표적과 임시 장착을 정리하고 원래 위치로 복귀");
        String body = mode
                + "\\n현재 시험 직업: " + role.displayName()
                + "\\n현재 임시 장착: " + VillageSkillTestSystem.loadoutSummary(player)
                + "\\n직업·Z/X 항목은 목록에서 한 번 클릭하면 즉시 적용됩니다.";
        send(player, "skill_test", "기술 시험 관리함", body, actions, labels);
    }

"""
text = text[:start] + replacement + text[end:]
old = """        if (action.startsWith("test_choose:")) {
            openSkillTestSlot(player, action.substring(12));
            return true;
        }
        if (action.startsWith("test_equip:")) {
            String[] parts = action.split(":", 3);
            int slot = 0;
            if (parts.length == 3) {
                try { slot = Integer.parseInt(parts[2]); }
                catch (NumberFormatException ignored) { slot = 0; }
                player.sendSystemMessage(Component.literal("§b"
                        + VillageSkillTestSystem.equip(player, parts[1], slot)));
            }
            openSkillTest(player);
            return true;
        }
"""
new = """        if (action.startsWith("test_role:")) {
            player.sendSystemMessage(Component.literal("§b"
                    + VillageSkillTestSystem.selectRole(player, action.substring(10))));
            openSkillTest(player);
            return true;
        }
        if (action.startsWith("test_choose:")) {
            player.sendSystemMessage(Component.literal("§b"
                    + VillageSkillTestSystem.equip(player, action.substring(12), 0)));
            openSkillTest(player);
            return true;
        }
        if (action.startsWith("test_equip:")) {
            String[] parts = action.split(":", 3);
            int slot = 0;
            if (parts.length == 3) {
                try { slot = Integer.parseInt(parts[2]); }
                catch (NumberFormatException ignored) { slot = 0; }
                player.sendSystemMessage(Component.literal("§b"
                        + VillageSkillTestSystem.equip(player, parts[1], slot)));
            }
            openSkillTest(player);
            return true;
        }
"""
text = replace_once(text, old, new, "controller test actions")
text = replace_once(
    text,
    '            case "test_spawn" -> openResult(player, "시험 표적", VillageSkillTestSystem.spawnTargets(player), "open_skill_test");\n'
    '            case "test_clear" -> openResult(player, "시험 표적", VillageSkillTestSystem.clearTargets(player), "open_skill_test");\n'
    '            case "test_exit" -> openResult(player, "기술 시험", VillageSkillTestSystem.disable(player), "open_role_skill_research");\n',
    '            case "test_spawn" -> {\n'
    '                player.sendSystemMessage(Component.literal("§b" + VillageSkillTestSystem.spawnTargets(player)));\n'
    '                openSkillTest(player);\n'
    '            }\n'
    '            case "test_clear" -> {\n'
    '                player.sendSystemMessage(Component.literal("§b" + VillageSkillTestSystem.clearTargets(player)));\n'
    '                openSkillTest(player);\n'
    '            }\n'
    '            case "test_exit" -> openResult(player, "기술 시험", VillageSkillTestSystem.disable(player),\n'
    '                    VillageCouncilState.roleOf(player.getUUID()).isPresent()\n'
    '                            ? "open_role_skill_research" : "open_dashboard");\n',
    "controller test utility actions",
)
write(path, text)

path = JAVA / "VillageFacilityScreen.java"
text = read(path)
text = replace_once(
    text,
    '            case "management" -> "회관 전용 시설 관리";\n'
    '            default -> "시설 고유 기능";\n',
    '            case "management" -> "회관 전용 시설 관리";\n'
    '            case "skill_test" -> "시험 관리함 · 직업과 Z/X 기술 선택";\n'
    '            default -> "시설 고유 기능";\n',
    "skill test subtitle",
)
old = """            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                detailScroll = 0;
                return true;
            }
"""
new = """            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                selectedIndex = index;
                detailScroll = 0;
                String selectedAction = actions[index];
                if ("skill_test".equals(payload.screenId())
                        && (selectedAction.startsWith("test_role:")
                        || selectedAction.startsWith("test_equip:"))) {
                    ClientPacketDistributor.sendToServer(
                            new VillageNetwork.VillageUiActionPayload(selectedAction));
                }
                return true;
            }
"""
text = replace_once(text, old, new, "instant skill test selection")
write(path, text)

path = JAVA / "VillageGuardians.java"
text = read(path)
text = replace_once(
    text,
    "    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {\n"
    "        if (VillageWorldSystem.handleCentralBellInteraction(event)) return;\n",
    "    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {\n"
    "        if (VillageSkillTestSystem.handleManagementBox(event)) return;\n"
    "        if (VillageWorldSystem.handleCentralBellInteraction(event)) return;\n",
    "management box interaction hook",
)
write(path, text)

path = JAVA / "VillageActionDescriptions.java"
text = read(path)
text = replace_once(
    text,
    '            return label + "\\n레벨당 1개씩 얻는 전술 포인트를 사용합니다. 비용은 단계에 따라 1~3P입니다.";\n',
    '            return label + "\\n레벨당 1개씩 얻는 전술 포인트를 사용합니다. 비용은 단계에 따라 1~4P입니다.";\n',
    "point cost description",
)
text = replace_once(
    text,
    '        if (action.startsWith("test_choose:")) {\n'
    '            return label + "\\n외부 시험장에서 사용할 임시 Z/X 슬롯을 선택합니다.";\n'
    '        }\n'
    '        if (action.startsWith("test_equip:")) {\n'
    '            return label + "\\n습득 여부와 비용을 무시하고 선택한 시험 슬롯에만 임시 장착합니다.";\n'
    '        }\n',
    '        if (action.startsWith("test_role:")) {\n'
    '            return label + "\\n시험 전용 직업만 바꾸며 실제 직업과 저장된 성장 상태는 유지합니다.";\n'
    '        }\n'
    '        if (action.startsWith("test_choose:")) {\n'
    '            return label + "\\n이전 시험 UI 호환 경로로 Z 시험 슬롯에 장착합니다.";\n'
    '        }\n'
    '        if (action.startsWith("test_equip:")) {\n'
    '            return label + "\\n목록을 한 번 클릭하면 선택한 Z/X 시험 슬롯에 즉시 임시 장착합니다.";\n'
    '        }\n',
    "test action descriptions",
)
text = replace_once(
    text,
    '            case "open_skill_test" -> "외부 시험장으로 이동해 기술을 Z/X에 임시 장착하고 실제 입력으로 시험합니다.";\n',
    '            case "open_skill_test" -> "외부 시험장으로 이동해 전용 관리함에서 직업과 Z/X 기술을 고르고 실제 입력으로 시험합니다.";\n',
    "open skill test description",
)
write(path, text)

path = TOOLS / "test_v0178_gameplay.py"
text = read(path)
text = replace_once(text, '    assert "test_choose:" in controller\n', '    assert "test_role:" in controller\n', "v0178 role action")
text = replace_once(text, '    assert "openSkillTestSlot" in controller\n', '    assert "test_equip:" in controller\n', "v0178 slot flow")
write(path, text)

test = """#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    test = read("VillageSkillTestSystem.java")
    role = read("VillageRoleSkillSystem.java")
    controller = read("VillageUiController.java")
    screen = read("VillageFacilityScreen.java")
    guard = read("VillageGuardians.java")

    assert "mod_version=0.17.9-alpha.1" in props
    assert "TEST_ROLES" in test and "selectedRole" in test and "selectRole" in test
    assert "실제 직업과 성장 데이터는 바뀌지 않습니다" in test
    assert "managementBoxPosition" in test and "Blocks.BARREL" in test
    assert "handleManagementBox" in test and "InteractionResult.SUCCESS" in test
    assert "VillageSkillTestSystem.handleManagementBox(event)" in guard

    assert "? VillageSkillTestSystem.selectedRole(player)" in role
    assert "VillageSkillTestSystem.equippedSkill(player, slot)" in role
    assert "시험 관리함에서 기술을 장착하세요" in role

    assert '\"test_role:\" + candidate.id()' in controller
    assert '\"test_equip:\" + skill.id() + \":0\"' in controller
    assert '\"test_equip:\" + skill.id() + \":1\"' in controller
    assert '\"기술 시험 관리함\"' in controller
    assert "openSkillTestSlot" not in controller

    assert '\"skill_test\".equals(payload.screenId())' in screen
    assert 'selectedAction.startsWith(\"test_role:\")' in screen
    assert 'selectedAction.startsWith(\"test_equip:\")' in screen

    print("[PASS] Test arena owns a physical management barrel")
    print("[PASS] Test-only job selection leaves permanent role and progression untouched")
    print("[PASS] Skill cards directly assign Z/X and the real keyed cast path reads the test loadout")

if __name__ == "__main__":
    main()
"""
write(TOOLS / "test_v0179_skill_test_box.py", test)

print("Applied Village Guardians v0.17.9 skill-test management box patch")
