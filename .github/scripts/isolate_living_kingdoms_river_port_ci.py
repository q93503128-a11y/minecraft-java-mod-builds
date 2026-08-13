from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
STATUS = ROOT / "projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


manager = MANAGER.read_text(encoding="utf-8")
require("private static boolean isCi()" in manager or "private static boolean isPortCi()" in manager,
        "ErdenRiverPortManager CI flag method not found")
if "private static boolean isPortCi()" not in manager:
    manager = manager.replace("isCi()", "isPortCi()")
manager = manager.replace(
    'return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));',
    'return "1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"));'
)

# A completed CI chunk must stay retained until the physical barge has traversed the whole
# corridor. Releasing each chunk as soon as its construction plan finishes makes corePortBuilt()
# require four anchors that can never stay loaded at the same time on an empty dedicated server.
manager = manager.replace(
    "        QUEUED.remove(activeChunk.packed());\n"
    "        releaseCi(level, activeChunk.packed());\n"
    "        if (isPortCi()) {",
    "        QUEUED.remove(activeChunk.packed());\n"
    "        if (!isPortCi()) releaseCi(level, activeChunk.packed());\n"
    "        if (isPortCi()) {"
)
manager = manager.replace(
    "            if (!data.needsChunk(packed, PORT_REVISION)) {\n"
    "                QUEUED.remove(packed);\n"
    "                releaseCi(level, packed);\n"
    "                continue;\n"
    "            }",
    "            if (!data.needsChunk(packed, PORT_REVISION)) {\n"
    "                QUEUED.remove(packed);\n"
    "                if (!isPortCi()) releaseCi(level, packed);\n"
    "                continue;\n"
    "            }"
)
manager = manager.replace(
    "loaded_only_runtime=true forced_citywide=false ci_corridor_only=true\",",
    "loaded_only_runtime=true forced_citywide=false ci_corridor_only=true ci_corridor_retained_until_pass=true ci_tickets_released_at_pass=true\","
)

require("LIVING_KINGDOMS_CI_REALM_TEST" not in manager,
        "generic realm CI flag still referenced by river-port manager")
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in manager,
        "dedicated river-port CI flag was not installed")
require("if (!isPortCi()) releaseCi(level, activeChunk.packed());" in manager,
        "completed port chunks would still release their CI corridor ticket early")
require("if (!isPortCi()) releaseCi(level, packed);" in manager,
        "already-built port chunks would still release their CI corridor ticket early")
require("ci_corridor_retained_until_pass=true" in manager,
        "river-port PASS evidence does not record CI corridor ticket lifecycle")
MANAGER.write_text(manager, encoding="utf-8")

status = STATUS.read_text(encoding="utf-8")
implemented_anchor = "## 왕국 완성 전 남은 핵심"
implemented_lines = (
    "- 완성된 로드 청크의 비기능 공터·후면 마당·골목 모서리를 도로·출입 통로·기능 필지를 침범하지 않는 안뜰·적재공간·세탁공간·정원·휴게공간·목재 야드로 채우는 2차 미세 필지 조립\n"
    "- 왕도 외곽 생산 거점이 실제 재고·운송 에스크로를 통해 왕도 창고 원료를 공급하고, 로드된 생산 거점은 물리 컨테이너 재고로 동기화되는 왕국 단위 공급망\n"
    "- 왕도 화재를 감지해 경비초소 근무자가 8개 화재 저수조 중 가까운 곳에서 급수한 뒤 현장까지 실제 길찾기로 이동해 근거리에서 진압하는 소방 대응\n"
    "- 강우·막힘·하천 수위에 반응해 도로 측구·빗물 유입구·지하 배수관·6개 하수 처리 거점을 연결하는 실제 배수 시뮬레이션\n"
    "- 은빛강 가항 수로, 서부 부두, 세관, 조선소·슬립웨이와 공급 장부의 바지선 화물을 실제 보트 엔티티 이동으로 연결한 수운\n"
)
if "은빛강 가항 수로, 서부 부두, 세관" not in status:
    require(implemented_anchor in status, "implementation-status remaining-core heading not found")
    status = status.replace(implemented_anchor, implemented_lines + "\n" + implemented_anchor, 1)
for obsolete in (
    "- 1차 시가지 파츠 사이의 남은 공터·후면 마당·골목 모서리를 채우는 2차 미세 필지 조립\n",
    "- 외곽 농장·목장·광산·부두가 창고 원료 입고량을 실제 생산과 운송으로 공급하는 왕국 단위 공급망\n",
    "- 화재 시 소방 인력과 저수조를 사용하는 대응 동선\n",
    "- 강우량·막힘·하천 수위에 반응하는 실제 배수·하수 시뮬레이션\n",
    "- 강변 부두, 세관, 조선소와 실제 수운\n",
):
    status = status.replace(obsolete, "")
STATUS.write_text(status, encoding="utf-8")

print("Isolated river-port CI fixture, retained its physical corridor through PASS, and refreshed Erden status.")
