#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(ROOT / "gradle.properties", "mod_version=0.18.0-alpha.1",
             "mod_version=0.18.1-alpha.1", "version")

role = JAVA / "VillageRoleSkillSystem.java"
replace_once(
    role,
    """        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentShop.roleSkillMultiplier(player);
""",
    """        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentShop.roleSkillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
""",
    "live relic skill multiplier",
)
replace_once(
    role,
    """        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player),
""",
    """        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player),
""",
    "test relic skill multiplier",
)

controller = JAVA / "VillageUiController.java"
replace_once(
    controller,
    """        send(player, "status", "수호자 상태", body, List.of(), List.of());
    }

    public static void openPersonalProgress(ServerPlayer player) {
""",
    """        String relicLabel = "획득 유물 보기|보유 " + VillageRelicSystem.ownedCount(player)
                + " / " + VillageRelicSystem.Relic.values().length + " · 누적 효과 확인";
        send(player, "status", "수호자 상태", body,
                List.of("open_relic_collection"), List.of(relicLabel));
    }

    public static void openRelicCollection(ServerPlayer player) {
        VillageRelicSystem.openCollection(player);
    }

    public static void openPersonalProgress(ServerPlayer player) {
""",
    "status relic button",
)
replace_once(
    controller,
    """        if (action.startsWith("relic_select:")) {
            player.sendSystemMessage(Component.literal("§d" + VillageRelicSystem.select(player, action.substring(13))));
            return true;
        }
""",
    """        if (action.startsWith("relic_select:")) {
            String result = VillageRelicSystem.select(player, action.substring(13));
            player.sendSystemMessage(Component.literal("§d" + result));
            VillageRelicSystem.openCollection(player);
            return true;
        }
""",
    "relic selection result",
)
replace_once(
    controller,
    """            case "open_status" -> openStatus(player);
            case "open_personal_progress" -> openPersonalProgress(player);
""",
    """            case "open_status" -> openStatus(player);
            case "open_relic_collection" -> openRelicCollection(player);
            case "open_personal_progress" -> openPersonalProgress(player);
""",
    "relic collection action",
)

verify = TOOLS / "verify_jar.py"
replace_once(
    verify,
    '    "kr/moonseungjun/villageguardians/VillageStatusScreen.class",\n',
    '    "kr/moonseungjun/villageguardians/VillageStatusScreen.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageRelicScreen.class",\n',
    "relic screen jar requirement",
)

for path in sorted(TOOLS.glob("test_*.py")):
    text = path.read_text(encoding="utf-8")
    text = text.replace("mod_version=0.18.0-alpha.1", "mod_version=0.18.1-alpha.1")
    path.write_text(text, encoding="utf-8")

print("Applied Village Guardians v0.18.1 relic integration")
