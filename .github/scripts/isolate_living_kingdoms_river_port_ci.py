from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANAGER = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
WORKFLOW = ROOT / ".github/workflows/audit-living-kingdoms-river-port.yml"
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
require("LIVING_KINGDOMS_CI_REALM_TEST" not in manager,
        "generic realm CI flag still referenced by river-port manager")
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in manager,
        "dedicated river-port CI flag was not installed")
MANAGER.write_text(manager, encoding="utf-8")

workflow = WORKFLOW.read_text(encoding="utf-8")
if "LIVING_KINGDOMS_CI_RIVER_PORT_TEST" not in workflow:
    workflow = workflow.replace(
        "  LIVING_KINGDOMS_CI_REALM_TEST: '1'\n",
        "  LIVING_KINGDOMS_CI_REALM_TEST: '1'\n  LIVING_KINGDOMS_CI_RIVER_PORT_TEST: '1'\n"
    )
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST: '1'" in workflow,
        "river-port audit does not enable its dedicated CI fixture")
WORKFLOW.write_text(workflow, encoding="utf-8")

status = STATUS.read_text(encoding="utf-8")
implemented_anchor = "## 왕국 완성 전 남은 핵심"
implemented_lines = (
    "- 왕도 외곽 생산 거점이 실제 재고·운송 에스크로를 통해 왕도 창고 원료를 공급하고, 로드된 생산 거점은 물리 컨테이너 재고로 동기화되는 왕국 단위 공급망\n"
    "- 왕도 화재를 감지해 경비초소 근무자가 8개 화재 저수조 중 가까운 곳에서 급수한 뒤 현장까지 실제 길찾기로 이동해 근거리에서 진압하는 소방 대응\n"
    "- 강우·막힘·하천 수위에 반응해 도로 측구·빗물 유입구·지하 배수관·6개 하수 처리 거점을 연결하는 실제 배수 시뮬레이션\n"
    "- 은빛강 가항 수로, 서부 부두, 세관, 조선소·슬립웨이와 공급 장부의 바지선 화물을 실제 보트 엔티티 이동으로 연결한 수운\n"
)
if "은빛강 가항 수로, 서부 부두, 세관" not in status:
    require(implemented_anchor in status, "implementation-status remaining-core heading not found")
    status = status.replace(implemented_anchor, implemented_lines + "\n" + implemented_anchor, 1)
for obsolete in (
    "- 외곽 농장·목장·광산·부두가 창고 원료 입고량을 실제 생산과 운송으로 공급하는 왕국 단위 공급망\n",
    "- 화재 시 소방 인력과 저수조를 사용하는 대응 동선\n",
    "- 강우량·막힘·하천 수위에 반응하는 실제 배수·하수 시뮬레이션\n",
    "- 강변 부두, 세관, 조선소와 실제 수운\n",
):
    status = status.replace(obsolete, "")
STATUS.write_text(status, encoding="utf-8")

# These were temporary watchdog instrumentation helpers. The blocking-chunk regression is now
# covered by the permanent fresh-world exterior audits, so keeping self-mutating diagnostics would
# only add CI noise and accidental write surfaces.
for relative in (
    ".github/workflows/diagnose-living-kingdoms-exterior-watchdog-stack.yml",
    ".github/workflows/add-living-kingdoms-exterior-ci-chunk-trace.yml",
    ".github/scripts/add_living_kingdoms_exterior_ci_chunk_trace.py",
    ".github/workflows/patch-living-kingdoms-river-port-ci-isolation.yml",
    ".github/scripts/isolate_living_kingdoms_river_port_ci.py",
):
    path = ROOT / relative
    if path.exists():
        path.unlink()

print("Isolated river-port CI fixture, refreshed Erden status, and removed temporary watchdog patchers.")
