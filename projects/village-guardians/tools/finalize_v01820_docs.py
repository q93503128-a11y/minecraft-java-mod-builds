#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:160]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


readme = ROOT / "README.md"
replace_once(readme, "- 현재 소스 버전 `0.18.19-alpha.1`", "- 현재 소스 버전 `0.18.20-alpha.1`")
replace_once(readme, "- 목표 JAR `villageguardians-0.18.19-alpha.1.jar`", "- 목표 JAR `villageguardians-0.18.20-alpha.1.jar`")
readme_section = '''## 0.18.20 용병 명부·병영 경제 정합\n\n- 병영의 기존 즉시 `마을 용병` 생성 경로를 폐기하고, 방벽 수호병·돌격 집행관·성루 명사수·전장 치유사 4병과를 한 명부에서 고용·확인·퇴역하는 구조로 통합했다.\n- 용병 정원은 주변 AABB에 현재 보이는 철골렘 수가 아니라 SavedData의 병과 명부가 정본이다. 용병이 먼 곳이나 언로드 구역에 있어도 정원을 우회해 추가 고용할 수 없다.\n- 병영 강화가 고용비를 올리던 역성장 수식을 제거했다. 병영 Lv.1 비용을 기준으로 이후 단계는 병과별 하한까지 완만하게 할인된다.\n- 기존 월드의 이름 `마을 용병` 철골렘은 청크가 로드될 때 방벽 수호병 Lv.1로 1회 이관되어 새 레벨·연구·Presentation·세이브 구조에 편입된다.\n- 낮 정비 시간에는 현재 로드된 용병을 개별 퇴역시킬 수 있다. 언로드된 엔티티를 UUID만 지워 고아 엔티티를 만드는 일을 막기 위해 퇴역은 실제 엔티티가 로드된 경우에만 허용하며 고용비는 환불하지 않는다.\n- 병영 시설 관리 화면과 용병 명부 화면은 동일한 연구 포함 정원을 표시한다. `용병 교리` 정원 보너스가 시설 설명에서 빠지던 UI 오표기를 최종 수동 감사에서 수정했다.\n\n'''
replace_once(readme, "## 0.18.19 후반 연구·용병 장기 성장\n", readme_section + "## 0.18.19 후반 연구·용병 장기 성장\n")

report = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
for old, new in (
    ("- Current source version: `0.18.19-alpha.1`", "- Current source version: `0.18.20-alpha.1`"),
    ("- Target JAR: `villageguardians-0.18.19-alpha.1.jar`", "- Target JAR: `villageguardians-0.18.20-alpha.1.jar`"),
    ("- Final acceptance Actions run: `32328485511`", "- Final acceptance Actions run: `32330475890`"),
    ("- Final acceptance head: `e5d5d0da931f791d48cd5d2fd5e92935bf3c5ca2`", "- Final acceptance head: `00a9ca1f4350d6dd431a4bd7ed2d6be817c50d7d`"),
    ("- Final JAR SHA-256: `092566a1f57194df43a556b709af4d97615d596565f56035679f3c29f01b37e2`", "- Final JAR SHA-256: `8349e946556c23d45694274397989ff1212fb2998c204eab0967a0a6ff04d55c`"),
    ("- Final JAR size: `974113` bytes", "- Final JAR size: `976814` bytes"),
):
    replace_once(report, old, new)

report_section = '''## 0.18.20 용병 명부·병영 경제·레거시 이관 감사\n\n- `VillageMercenarySystem`의 SavedData `CLASSES` 명부를 용병 정원의 단일 정본으로 승격했다. 고용 시 현재 전장 AABB의 Iron Golem 수를 세던 경로를 제거해 언로드/원거리 이탈로 정원을 우회할 수 없게 했다.\n- 병영 강화가 병과 고용비에 `+25/level`을 더하던 역성장 수식을 제거하고, 병과별 기본 비용을 유지한 채 병영 단계가 오를수록 완만한 할인만 적용되게 했다.\n- `VillageDefenseSystem`의 별도 generic `마을 용병` 생성 소유권을 폐기하고 생산 고용을 `VillageMercenarySystem`으로 통합했다. 기존 호환 facade도 방벽 수호병 고용으로 위임한다.\n- 기존 세이브에 남은 이름 `마을 용병` Iron Golem은 entity join 시 `adoptLegacy`를 통해 방벽 수호병 Lv.1로 1회 이관되며 병과/레벨/훈련 SavedData, 허용 엔티티 마킹, 패시브와 이름을 즉시 동기화한다.\n- 병영 UI는 전용 `mercenary_roster` 화면으로 전환했다. 네 병과별 실제 현재 고용비와 설명을 보여주고, 로드된 개별 용병은 UUID 기반 `retire_mercenary` 작업으로 낮 정비 시간에만 퇴역시킨다. 퇴역은 Presentation actor와 허용 엔티티 마킹·SavedData를 함께 정리하며 환불은 없다.\n- 수동 후속 감사에서 병영 관리 화면의 정원 표시가 연구 보너스를 빠뜨리는 불일치를 발견해 `VillageDefenseResearchSystem.mercenaryCapacityBonus()`를 포함하도록 수정했고 별도 `test_v01820_roster_ui_consistency.py` 계약을 추가했다.\n- 통합 과정에서 역사 버전 문자열을 의도적으로 고정한 0.17.x 테스트까지 전부 실행하는 게이트 오류와 `Mob`→`IronGolem` 정적 타입 오류를 실제 CI가 잡았다. 역사 테스트는 변조하지 않고 현행 canonical 회귀 세트로 복구했으며 legacy migration은 pattern binding으로 타입 정합을 맞췄다.\n- 검증된 런타임 소스는 `b2304912534dc70afae6a67f96874e212c36b2d3`, 후속 UI 정합 수정은 `4466b1d46146175a2df5e06eb34f68e645037f95`에 반영됐다.\n- 최종 acceptance Actions run `32330475890`은 canonical deterministic contracts, Java 25 setup, NeoForge clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `00a9ca1f4350d6dd431a4bd7ed2d6be817c50d7d`다.\n- Actions artifact `9392861932`를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageMercenarySystem`, `VillageMercenarySystem$RosterEntry`, `VillageMercenaryPresentationSystem`, `VillageDefenseSystem`, `VillageUiService`, `VillageActionDescriptions`, `VillageActionDetailScreen` 포함을 독립 재검증했다. Artifact digest는 `sha256:de51d284c73a9c82a99704e0678b29f8b0fb372dbc9417efc8e56ef65ca49067`이다.\n- 최종 JAR SHA-256 `8349e946556c23d45694274397989ff1212fb2998c204eab0967a0a6ff04d55c`, 크기 `976814` bytes.\n\n'''
replace_once(report, "## 0.18.19 후반 연구·용병 정체성·세이브 연속성 감사\n", report_section + "## 0.18.19 후반 연구·용병 정체성·세이브 연속성 감사\n")

print("[PASS] v0.18.20 README/report finalized")
