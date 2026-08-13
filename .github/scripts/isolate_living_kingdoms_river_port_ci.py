from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
PORT_MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
FIRE_MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenFireResponseManager.java"
JUSTICE_MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/ErdenJusticeManager.java"
CRIME_MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/CrimeManager.java"
MOD_MAIN = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java"
STATUS = ROOT / "projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


port = PORT_MANAGER.read_text(encoding="utf-8")
require("private static boolean isPortCi()" in port,
        "ErdenRiverPortManager dedicated CI flag method not found")
require("LIVING_KINGDOMS_CI_REALM_TEST" not in port,
        "generic realm CI flag still referenced by river-port manager")
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in port,
        "dedicated river-port CI flag missing")
require("if (!isPortCi()) releaseCi(level, activeChunk.packed());" in port,
        "completed port chunks would release their CI corridor ticket early")
require("if (!isPortCi()) releaseCi(level, packed);" in port,
        "already-built port chunks would release their CI corridor ticket early")
require("ci_corridor_retained_until_pass=true" in port,
        "river-port PASS evidence does not record CI corridor lifecycle")

fire = FIRE_MANAGER.read_text(encoding="utf-8")
if "private static boolean isFireCi()" not in fire:
    fire = fire.replace("isCi()", "isFireCi()")
fire = fire.replace(
    'return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));',
    'return "1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"));'
)
require("LIVING_KINGDOMS_CI_REALM_TEST" not in fire,
        "generic realm CI flag still referenced by fire-response fixture")
require("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST" in fire,
        "dedicated fire-response CI flag missing")
require("if (!isFireCi() || ciPassed || ciPrepared) return;" in fire,
        "fire fixture preparation is not isolated")
require("if (!isFireCi() || ciPassed || !ciPrepared || ciFirePos == null) return;" in fire,
        "fire fixture verification is not isolated")
FIRE_MANAGER.write_text(fire, encoding="utf-8")

justice = JUSTICE_MANAGER.read_text(encoding="utf-8")
# This helper depended on a non-existent StarterRealmManager.server() accessor and is not needed;
# CrimeManager can distinguish the Erden warrant directly from CrimeSavedData.
justice = re.sub(
    r'\n    public static boolean hasActiveCase\(UUID suspect\) \{.*?\n    \}\n\n    public static void onServerTick',
    '\n    public static void onServerTick',
    justice,
    count=1,
    flags=re.S,
)
require("StarterRealmManager.server()" not in justice,
        "justice manager still calls non-existent global server accessor")
require("public static void onServerTick(ServerTickEvent.Post event)" in justice,
        "justice server-tick entry point missing")
require("actual_resident=true" in justice and "synthetic_guard=false" in justice,
        "justice evidence markers missing")
JUSTICE_MANAGER.write_text(justice, encoding="utf-8")

crime = CRIME_MANAGER.read_text(encoding="utf-8")
if "ErdenJusticeManager.JURISDICTION.equals(record.jurisdiction())" not in crime:
    crime = crime.replace(
        "        if (record.wanted() <= 0) return;\n\n        String local = RealmJurisdiction.at(level, player.blockPosition());",
        "        if (record.wanted() <= 0) return;\n"
        "        // Erden warrants are enforced by population-backed resident guards, never the\n"
        "        // generic synthetic pursuit wave used by the other starter realms.\n"
        "        if (ErdenJusticeManager.JURISDICTION.equals(record.jurisdiction())) return;\n\n"
        "        String local = RealmJurisdiction.at(level, player.blockPosition());"
    )
if "ErdenJusticeManager.observeCrime(level, player, severity, description" not in crime:
    crime = crime.replace(
        "    private static void reportCrime(ServerLevel level, ServerPlayer player, String jurisdiction,\n"
        "                                    int severity, String description) {\n"
        "        CrimeSavedData.CrimeRecord record = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE)",
        "    private static void reportCrime(ServerLevel level, ServerPlayer player, String jurisdiction,\n"
        "                                    int severity, String description) {\n"
        "        if (ErdenJusticeManager.JURISDICTION.equals(jurisdiction)) {\n"
        "            ErdenJusticeManager.observeCrime(\n"
        "                    level, player, severity, description, player.blockPosition());\n"
        "            return;\n"
        "        }\n"
        "        CrimeSavedData.CrimeRecord record = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE)"
    )
require("ErdenJusticeManager.JURISDICTION.equals(record.jurisdiction())" in crime,
        "generic Erden synthetic pursuit was not disabled")
require("ErdenJusticeManager.observeCrime(level, player, severity, description" in crime,
        "Erden crime reporting was not routed through civic justice")
CRIME_MANAGER.write_text(crime, encoding="utf-8")

main = MOD_MAIN.read_text(encoding="utf-8")
if "import kr.moonseungjun.livingkingdoms.crime.ErdenJusticeManager;" not in main:
    main = main.replace(
        "import kr.moonseungjun.livingkingdoms.crime.CrimeManager;\n",
        "import kr.moonseungjun.livingkingdoms.crime.CrimeManager;\n"
        "import kr.moonseungjun.livingkingdoms.crime.ErdenJusticeManager;\n"
    )
if "ErdenJusticeManager.onServerTick(event);" not in main:
    main = main.replace(
        "        ErdenPopulationManager.onServerTick(event);\n"
        "        ErdenFireResponseManager.onServerTick(event);",
        "        ErdenPopulationManager.onServerTick(event);\n"
        "        ErdenJusticeManager.onServerTick(event);\n"
        "        ErdenFireResponseManager.onServerTick(event);"
    )
require("ErdenJusticeManager.onServerTick(event);" in main,
        "Erden justice manager was not wired into the authoritative server tick")
MOD_MAIN.write_text(main, encoding="utf-8")

status = STATUS.read_text(encoding="utf-8")
implemented_anchor = "## 왕국 완성 전 남은 핵심"
justice_line = (
    "- 에르덴 범죄를 즉시 수배로 바꾸지 않고 실제 로드된 주민 목격자가 실제 경비초소 근무자에게 걸어가 신고한 뒤 수배장을 발부하며, 주민 경비의 근접 체포·구금·실제 시민법정 심리·판결·형기 집행으로 이어지는 시민 사법 절차\n"
)
if justice_line.strip() not in status:
    require(implemented_anchor in status, "implementation-status remaining-core heading not found")
    status = status.replace(implemented_anchor, justice_line + "\n" + implemented_anchor, 1)
status = status.replace("- 경비대 목격과 신고, 체포, 구금, 재판, 판결, 형 집행\n", "")
STATUS.write_text(status, encoding="utf-8")

print("Isolated CI fixtures and wired population-backed Erden civic justice.")
