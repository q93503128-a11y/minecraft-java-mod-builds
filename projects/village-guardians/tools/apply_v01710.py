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


def replace_between(text: str, start: str, end: str, new: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise SystemExit(f"{label}: start marker not found")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise SystemExit(f"{label}: end marker not found")
    return text[:start_index] + new + text[end_index:]


# Version
props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.9-alpha.1", "mod_version=0.17.10-alpha.1", "version")
write(props, text)


# Register key mappings on the MOD bus. The previous default GAME bus never received
# RegisterKeyMappingsEvent, so Z/X and the other key mappings were never registered.
path = JAVA / "VillageClientKeys.java"
text = read(path)
text = replace_once(
    text,
    "@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)",
    "@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID, "
    "bus = EventBusSubscriber.Bus.MOD)",
    "client key mod bus",
)
write(path, text)


# Split the single management barrel into a role barrel and a skill barrel.
path = JAVA / "VillageSkillTestSystem.java"
text = read(path)
text = replace_once(
    text,
    "        BlockPos start = arena.offset(0, 0, 11);\n",
    "        BlockPos start = arena.offset(0, 0, 10);\n",
    "test arena start position",
)
text = replace_once(
    text,
    "        return \"외부 기술 시험장으로 이동했습니다.\"\n"
    "                + \"\\n뒤쪽의 기술 시험 관리함을 열어 직업과 Z/X 기술을 변경할 수 있습니다.\"\n"
    "                + \"\\nK를 눌러도 같은 관리 화면을 엽니다. \" + targets;\n",
    "        return \"외부 기술 시험장으로 이동했습니다.\"\n"
    "                + \"\\n금색 바닥 관리함은 직업, 청금석 바닥 관리함은 Z/X 기술을 담당합니다.\"\n"
    "                + \"\\n기술을 장착하면 창이 닫히며, 그 뒤 Z/X로 실제 시전합니다. K는 기술 관리함을 엽니다. \"\n"
    "                + targets;\n",
    "test arena instructions",
)
text = replace_between(
    text,
    "    public static boolean handleManagementBox(PlayerInteractEvent.RightClickBlock event) {\n",
    "    private static BlockPos arenaCenter() {\n",
    """    public static boolean handleManagementBox(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel)
                || !isEnabled(player)) return false;
        BlockPos clicked = event.getPos();
        BlockPos roleBox = roleManagementBoxPosition();
        BlockPos skillBox = skillManagementBoxPosition();
        if (roleBox == null || skillBox == null) return false;
        if (!clicked.equals(roleBox) && !clicked.equals(skillBox)) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (clicked.equals(roleBox)) VillageUiController.openSkillTestRoleManager(player);
        else VillageUiController.openSkillTestSkillManager(player);
        return true;
    }

    public static BlockPos roleManagementBoxPosition() {
        BlockPos arena = arenaCenter();
        return arena == null ? null : arena.offset(-3, 0, 14);
    }

    public static BlockPos skillManagementBoxPosition() {
        BlockPos arena = arenaCenter();
        return arena == null ? null : arena.offset(3, 0, 14);
    }

""",
    "split management boxes",
)
text = replace_once(
    text,
    "        BlockPos box = managementBoxPosition();\n"
    "        if (box != null) {\n"
    "            VillageFortressTerrain.set(level, box.below(), Blocks.GOLD_BLOCK);\n"
    "            VillageFortressTerrain.set(level, box, Blocks.BARREL);\n"
    "            VillageFortressTerrain.set(level, box.west(), Blocks.SEA_LANTERN);\n"
    "            VillageFortressTerrain.set(level, box.east(), Blocks.SEA_LANTERN);\n"
    "        }\n",
    "        BlockPos roleBox = roleManagementBoxPosition();\n"
    "        BlockPos skillBox = skillManagementBoxPosition();\n"
    "        if (roleBox != null) {\n"
    "            VillageFortressTerrain.set(level, roleBox.below(), Blocks.GOLD_BLOCK);\n"
    "            VillageFortressTerrain.set(level, roleBox, Blocks.BARREL);\n"
    "            VillageFortressTerrain.set(level, roleBox.west(), Blocks.SEA_LANTERN);\n"
    "        }\n"
    "        if (skillBox != null) {\n"
    "            VillageFortressTerrain.set(level, skillBox.below(), Blocks.LAPIS_BLOCK);\n"
    "            VillageFortressTerrain.set(level, skillBox, Blocks.BARREL);\n"
    "            VillageFortressTerrain.set(level, skillBox.east(), Blocks.SEA_LANTERN);\n"
    "        }\n",
    "build two management boxes",
)
write(path, text)


# Separate role and skill menus, close the menu after equipping, and handle Z/X directly
# in the primary server action router instead of relying on the legacy fallback router.
path = JAVA / "VillageUiController.java"
text = read(path)
text = replace_between(
    text,
    "    public static void openSkillTest(ServerPlayer player) {\n",
    "    public static void openResult(ServerPlayer player, String title, String result, String returnAction) {\n",
    """    public static void openSkillTest(ServerPlayer player) {
        boolean alreadyEnabled = VillageSkillTestSystem.isEnabled(player);
        String mode = prepareSkillTest(player);
        if (mode == null) return;
        if (alreadyEnabled) sendSkillTestSkillManager(player, mode);
        else sendSkillTestRoleManager(player, mode);
    }

    public static void openSkillTestRoleManager(ServerPlayer player) {
        String mode = prepareSkillTest(player);
        if (mode != null) sendSkillTestRoleManager(player, mode);
    }

    public static void openSkillTestSkillManager(ServerPlayer player) {
        String mode = prepareSkillTest(player);
        if (mode != null) sendSkillTestSkillManager(player, mode);
    }

    private static String prepareSkillTest(ServerPlayer player) {
        boolean alreadyEnabled = VillageSkillTestSystem.isEnabled(player);
        if (!alreadyEnabled && !VillageLocationRules.isNearSkillHall(player)) {
            openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                    "open_role_skill_research");
            return null;
        }
        String mode = alreadyEnabled
                ? "외부 시험장 활성화"
                : VillageSkillTestSystem.enable(player);
        if (!VillageSkillTestSystem.isEnabled(player)) {
            openResult(player, "기술 시험", mode, "open_role_skill_research");
            return null;
        }
        return mode;
    }

    private static void sendSkillTestRoleManager(ServerPlayer player, String mode) {
        VillageRole selected = VillageSkillTestSystem.selectedRole(player);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRole candidate : VillageRole.values()) {
            actions.add("test_role:" + candidate.id());
            labels.add((candidate == selected ? "선택됨 · " : "") + candidate.displayName()
                    + "|" + candidate.overview()
                    + "\\n시험 전용 직업만 변경하며 실제 직업·성장·저장값은 바뀌지 않습니다.");
        }
        add(actions, labels,
                "open_skill_test_skills", "스킬 관리함 열기|현재 시험 직업의 Z/X 기술 장착 화면",
                "test_exit", "시험 종료·복귀|시험 데이터를 정리하고 원래 위치로 복귀");
        String body = mode
                + "\\n현재 시험 직업: " + selected.displayName()
                + "\\n금색 바닥 직업 관리함입니다. 청금석 바닥 관리함에서는 스킬을 장착합니다.";
        send(player, "skill_test_role", "시험 직업 관리함", body, actions, labels);
    }

    private static void sendSkillTestSkillManager(ServerPlayer player, String mode) {
        VillageRole role = VillageSkillTestSystem.selectedRole(player);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            actions.add("test_equip:" + skill.id() + ":0");
            labels.add("Z · " + skill.displayName() + "|" + skill.description()
                    + "\\n선택하면 Z 슬롯에 장착되고 창이 자동으로 닫힙니다.");
            actions.add("test_equip:" + skill.id() + ":1");
            labels.add("X · " + skill.displayName() + "|" + skill.description()
                    + "\\n선택하면 X 슬롯에 장착되고 창이 자동으로 닫힙니다.");
        }
        add(actions, labels,
                "open_skill_test_roles", "직업 관리함 열기|시험할 직업을 변경",
                "test_spawn", "시험 표적 재배치|체력·밀림 저항이 다른 표적 6개 생성",
                "test_clear", "시험 표적 정리|현재 내가 만든 시험 표적 제거",
                "test_exit", "시험 종료·복귀|표적과 임시 장착을 정리하고 원래 위치로 복귀");
        String body = mode
                + "\\n현재 시험 직업: " + role.displayName()
                + "\\n현재 임시 장착: " + VillageSkillTestSystem.loadoutSummary(player)
                + "\\n기술 선택 후 창이 닫히면 Z/X를 눌러 실제 시전합니다. K로 다시 엽니다.";
        send(player, "skill_test_skill", "시험 스킬 관리함", body, actions, labels);
    }

""",
    "separate test menus",
)
text = replace_once(
    text,
    "    public static boolean handleAction(ServerPlayer player, String action) {\n"
    "        if (action == null || action.isBlank() || action.equals(\"facility_info\")) return true;\n"
    "        MinecraftServer server = player.level().getServer();\n"
    "        if (server == null) return true;\n\n",
    "    public static boolean handleAction(ServerPlayer player, String action) {\n"
    "        if (action == null || action.isBlank() || action.equals(\"facility_info\")) return true;\n"
    "        MinecraftServer server = player.level().getServer();\n"
    "        if (server == null) return true;\n\n"
    "        if (action.startsWith(\"use_skill:\")) {\n"
    "            int slot;\n"
    "            try { slot = Integer.parseInt(action.substring(10)); }\n"
    "            catch (NumberFormatException ignored) { slot = 0; }\n"
    "            player.sendSystemMessage(Component.literal(\"§b\" + VillageRpgSystem.useRoleSkill(player, slot)));\n"
    "            return true;\n"
    "        }\n\n",
    "direct keyed skill route",
)
text = replace_once(
    text,
    "        if (action.startsWith(\"test_role:\")) {\n"
    "            player.sendSystemMessage(Component.literal(\"§b\"\n"
    "                    + VillageSkillTestSystem.selectRole(player, action.substring(10))));\n"
    "            openSkillTest(player);\n"
    "            return true;\n"
    "        }\n",
    "        if (action.startsWith(\"test_role:\")) {\n"
    "            player.sendSystemMessage(Component.literal(\"§b\"\n"
    "                    + VillageSkillTestSystem.selectRole(player, action.substring(10))));\n"
    "            openSkillTestRoleManager(player);\n"
    "            return true;\n"
    "        }\n",
    "role manager stays separate",
)
text = replace_once(
    text,
    "        if (action.startsWith(\"test_equip:\")) {\n"
    "            String[] parts = action.split(\":\", 3);\n"
    "            int slot = 0;\n"
    "            if (parts.length == 3) {\n"
    "                try { slot = Integer.parseInt(parts[2]); }\n"
    "                catch (NumberFormatException ignored) { slot = 0; }\n"
    "                player.sendSystemMessage(Component.literal(\"§b\"\n"
    "                        + VillageSkillTestSystem.equip(player, parts[1], slot)));\n"
    "            }\n"
    "            openSkillTest(player);\n"
    "            return true;\n"
    "        }\n",
    "        if (action.startsWith(\"test_equip:\")) {\n"
    "            String[] parts = action.split(\":\", 3);\n"
    "            int slot = 0;\n"
    "            if (parts.length == 3) {\n"
    "                try { slot = Integer.parseInt(parts[2]); }\n"
    "                catch (NumberFormatException ignored) { slot = 0; }\n"
    "                player.sendSystemMessage(Component.literal(\"§b\"\n"
    "                        + VillageSkillTestSystem.equip(player, parts[1], slot)));\n"
    "            }\n"
    "            return true;\n"
    "        }\n",
    "do not reopen after equip",
)
text = replace_once(
    text,
    "            case \"open_role_progress_current\" -> {\n"
    "                if (VillageSkillTestSystem.isEnabled(player)) openSkillTest(player);\n"
    "                else openRoleProgress(player);\n"
    "            }\n",
    "            case \"open_role_progress_current\" -> {\n"
    "                if (VillageSkillTestSystem.isEnabled(player)) openSkillTestSkillManager(player);\n"
    "                else openRoleProgress(player);\n"
    "            }\n",
    "K opens test skill manager",
)
text = replace_once(
    text,
    "            case \"open_skill_test\" -> openSkillTest(player);\n"
    "            case \"test_spawn\" -> {\n"
    "                player.sendSystemMessage(Component.literal(\"§b\" + VillageSkillTestSystem.spawnTargets(player)));\n"
    "                openSkillTest(player);\n"
    "            }\n"
    "            case \"test_clear\" -> {\n"
    "                player.sendSystemMessage(Component.literal(\"§b\" + VillageSkillTestSystem.clearTargets(player)));\n"
    "                openSkillTest(player);\n"
    "            }\n",
    "            case \"open_skill_test\" -> openSkillTest(player);\n"
    "            case \"open_skill_test_roles\" -> openSkillTestRoleManager(player);\n"
    "            case \"open_skill_test_skills\" -> openSkillTestSkillManager(player);\n"
    "            case \"test_spawn\" -> {\n"
    "                player.sendSystemMessage(Component.literal(\"§b\" + VillageSkillTestSystem.spawnTargets(player)));\n"
    "                openSkillTestSkillManager(player);\n"
    "            }\n"
    "            case \"test_clear\" -> {\n"
    "                player.sendSystemMessage(Component.literal(\"§b\" + VillageSkillTestSystem.clearTargets(player)));\n"
    "                openSkillTestSkillManager(player);\n"
    "            }\n",
    "test manager switch routes",
)
write(path, text)


# Role selections stay open; skill selections close immediately so the player can press Z/X
# without the screen swallowing and draining the key press.
path = JAVA / "VillageFacilityScreen.java"
text = read(path)
text = replace_once(
    text,
    "            case \"management\" -> \"회관 전용 시설 관리\";\n"
    "            case \"skill_test\" -> \"시험 관리함 · 직업과 Z/X 기술 선택\";\n"
    "            default -> \"시설 고유 기능\";\n",
    "            case \"management\" -> \"회관 전용 시설 관리\";\n"
    "            case \"skill_test_role\" -> \"금색 관리함 · 시험 직업 선택\";\n"
    "            case \"skill_test_skill\" -> \"청금석 관리함 · Z/X 기술 장착\";\n"
    "            default -> \"시설 고유 기능\";\n",
    "separate test subtitles",
)
text = replace_once(
    text,
    "                String selectedAction = actions[index];\n"
    "                if (\"skill_test\".equals(payload.screenId())\n"
    "                        && (selectedAction.startsWith(\"test_role:\")\n"
    "                        || selectedAction.startsWith(\"test_equip:\"))) {\n"
    "                    ClientPacketDistributor.sendToServer(\n"
    "                            new VillageNetwork.VillageUiActionPayload(selectedAction));\n"
    "                }\n"
    "                return true;\n",
    "                String selectedAction = actions[index];\n"
    "                if (payload.screenId().startsWith(\"skill_test_\")) {\n"
    "                    if (selectedAction.startsWith(\"test_equip:\")) {\n"
    "                        ClientPacketDistributor.sendToServer(\n"
    "                                new VillageNetwork.VillageUiActionPayload(selectedAction));\n"
    "                        onClose();\n"
    "                        return true;\n"
    "                    }\n"
    "                    if (selectedAction.startsWith(\"test_role:\")) {\n"
    "                        ClientPacketDistributor.sendToServer(\n"
    "                                new VillageNetwork.VillageUiActionPayload(selectedAction));\n"
    "                    }\n"
    "                }\n"
    "                return true;\n",
    "close test skill menu after equip",
)
write(path, text)


# Action text for the split boxes and auto-close behaviour.
path = JAVA / "VillageActionDescriptions.java"
text = read(path)
text = replace_once(
    text,
    "        if (action.startsWith(\"test_equip:\")) {\n"
    "            return label + \"\\n목록을 한 번 클릭하면 선택한 Z/X 시험 슬롯에 즉시 임시 장착합니다.\";\n"
    "        }\n",
    "        if (action.startsWith(\"test_equip:\")) {\n"
    "            return label + \"\\n선택한 Z/X 시험 슬롯에 장착한 뒤 화면을 닫습니다. 화면이 닫힌 상태에서 Z/X를 눌러 시전합니다.\";\n"
    "        }\n",
    "test equip instructions",
)
text = replace_once(
    text,
    "            case \"open_skill_test\" -> \"외부 시험장으로 이동해 전용 관리함에서 직업과 Z/X 기술을 고르고 실제 입력으로 시험합니다.\";\n"
    "            case \"test_spawn\" -> \"외부 시험장의 고정 표적 여섯 개를 다시 배치합니다.\";\n",
    "            case \"open_skill_test\" -> \"외부 시험장으로 이동해 분리된 직업·스킬 관리함에서 시험 설정을 관리합니다.\";\n"
    "            case \"open_skill_test_roles\" -> \"금색 바닥 직업 관리함을 엽니다.\";\n"
    "            case \"open_skill_test_skills\" -> \"청금석 바닥 스킬 관리함을 엽니다.\";\n"
    "            case \"test_spawn\" -> \"외부 시험장의 고정 표적 여섯 개를 다시 배치합니다.\";\n",
    "split box descriptions",
)
write(path, text)


# Keep old regression contracts aligned with the new patch and add a focused runtime contract.
for test_path in TOOLS.glob("test_*.py"):
    test_text = read(test_path)
    test_text = test_text.replace("mod_version=0.17.9-alpha.1", "mod_version=0.17.10-alpha.1")
    if test_path.name == "test_v0179_skill_test_box.py":
        test_text = test_text.replace(
            'assert "managementBoxPosition" in test and "Blocks.BARREL" in test',
            'assert "roleManagementBoxPosition" in test and "skillManagementBoxPosition" in test\n'
            '    assert test.count("Blocks.BARREL") >= 2 and "Blocks.LAPIS_BLOCK" in test',
        )
        test_text = test_text.replace(
            'assert \'"기술 시험 관리함"\' in controller',
            'assert \'"시험 직업 관리함"\' in controller and \'"시험 스킬 관리함"\' in controller',
        )
        test_text = test_text.replace(
            'assert \'"skill_test".equals(payload.screenId())\' in screen',
            'assert \'payload.screenId().startsWith("skill_test_")\' in screen',
        )
        test_text = test_text.replace(
            'print("[PASS] Test arena owns a physical management barrel")',
            'print("[PASS] Test arena owns separate role and skill management barrels")',
        )
    write(test_path, test_text)

runtime_test = """#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    test = read("VillageSkillTestSystem.java")
    controller = read("VillageUiController.java")
    screen = read("VillageFacilityScreen.java")

    assert "mod_version=0.17.10-alpha.1" in props
    assert "bus = EventBusSubscriber.Bus.MOD" in keys
    assert 'consume(ROLE_SKILL_ONE, "use_skill:0")' in keys
    assert 'consume(ROLE_SKILL_TWO, "use_skill:1")' in keys

    assert "roleManagementBoxPosition" in test
    assert "skillManagementBoxPosition" in test
    assert test.count("Blocks.BARREL") >= 2
    assert "Blocks.GOLD_BLOCK" in test and "Blocks.LAPIS_BLOCK" in test
    assert "openSkillTestRoleManager" in test and "openSkillTestSkillManager" in test

    assert 'if (action.startsWith("use_skill:"))' in controller
    assert "VillageRpgSystem.useRoleSkill(player, slot)" in controller
    assert 'send(player, "skill_test_role", "시험 직업 관리함"' in controller
    assert 'send(player, "skill_test_skill", "시험 스킬 관리함"' in controller
    assert 'case "open_skill_test_roles"' in controller
    assert 'case "open_skill_test_skills"' in controller

    equip_start = controller.index('if (action.startsWith("test_equip:"))')
    equip_end = controller.index('if (action.startsWith("relic_select:"))', equip_start)
    equip_block = controller[equip_start:equip_end]
    assert "openSkillTest(" not in equip_block
    assert "openSkillTestSkillManager(" not in equip_block

    assert 'payload.screenId().startsWith("skill_test_")' in screen
    assert 'selectedAction.startsWith("test_equip:")' in screen
    assert "onClose();" in screen

    print("[PASS] Client Z/X mappings register on the NeoForge mod event bus")
    print("[PASS] Z/X packets route directly to the real role-skill cast system")
    print("[PASS] Role and skill management use separate physical barrels and screens")
    print("[PASS] Equipping closes the skill screen so in-game Z/X input is no longer drained")


if __name__ == "__main__":
    main()
"""
write(TOOLS / "test_v01710_skill_input.py", runtime_test)

print("Applied Village Guardians v0.17.10 split test boxes and Z/X runtime fix")
