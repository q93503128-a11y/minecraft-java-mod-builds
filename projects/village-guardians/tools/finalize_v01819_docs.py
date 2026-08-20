#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label}: {old!r}")
    return text.replace(old, new, 1)


readme = ROOT / "README.md"
text = read(readme)
text = replace_once(text, "- 현재 소스 버전 `0.18.18-alpha.1`", "- 현재 소스 버전 `0.18.19-alpha.1`", "README version")
text = replace_once(text, "- 목표 JAR `villageguardians-0.18.18-alpha.1.jar`", "- 목표 JAR `villageguardians-0.18.19-alpha.1.jar`", "README jar")
anchor = "낮 정비 → 장비·성장·시설·포탑·용병 준비 → 다음 밤 정찰 → 공성전 → 피해 복구·강화 → 다음 날짜로 진행한다.\n"
section = r'''

## 0.18.19 후반 연구·용병 장기 성장

- 방어 연구는 기존 세이브 ID를 유지한 채 분야별 최대 Lv.10으로 확장된다. Lv.1~5의 기존 핵심 위력은 유지하고 Lv.6~10은 성장률이 완만한 숙련 구간으로 이어진다.
- 용병 교리는 피해뿐 아니라 치유, 처치 훈련 진척, 정원까지 장기 성장한다. 기존 Lv.1~5 정원 보너스는 감소하지 않으며 최대 연구에서는 추가 정원이 더 열린다.
- 포탑 공학은 피해 외에 실제 사거리와 최대 내구도도 올린다. 0.18.18 이전 풀피 포탑 세이브는 1회 이행 시 새 최대 내구도 기준으로 풀피를 유지하며, 이후 연구 강화도 현재 HP 비율을 보존한다.
- 전리품 군수학은 장비 드랍·판매 가치 외에 전투 소모품 가격과 현장 수리 효율에도 연결된다.
- 용병은 기존 Iron Golem 기반 판정·저장 구조를 유지하면서 병과별 지속형 절차 메시 실루엣을 가진다. 방벽 수호병/돌격 집행관/성루 명사수/전장 치유사가 Lv.20·40·60 구간에서 시각적으로 단계 강화된다.
- 0.18.18에서 추가된 장비별 장기 강화 상한, 전투 소모품, 성벽 사격구·전투 보행로, Lv.60 용병 성장 구조를 그대로 유지한다.
'''
if "## 0.18.19 후반 연구·용병 장기 성장" not in text:
    text = replace_once(text, anchor, anchor + section, "README core loop anchor")
write(readme, text)

report = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
text = read(report)
text = replace_once(text, "- Current source version: `0.18.16-alpha.1`", "- Current source version: `0.18.19-alpha.1`", "report version")
text = replace_once(text, "- Target JAR: `villageguardians-0.18.16-alpha.1.jar`", "- Target JAR: `villageguardians-0.18.19-alpha.1.jar`", "report jar")
text = replace_once(text, "- Final acceptance Actions run: `32094533863`", "- Final acceptance Actions run: `32328485511`", "report run")
text = replace_once(text, "- Final acceptance head: `c41c8cde89bc1dea524156e61480e1f302a406a6`", "- Final acceptance head: `e5d5d0da931f791d48cd5d2fd5e92935bf3c5ca2`", "report head")
text = replace_once(text, "- Final JAR SHA-256: `62c1e69fba813828a7b59761bb3320daaca8097e7ce5bcff47b092cc8c918b2b`", "- Final JAR SHA-256: `092566a1f57194df43a556b709af4d97615d596565f56035679f3c29f01b37e2`", "report sha")
text = replace_once(text, "- Final JAR size: `955045` bytes", "- Final JAR size: `974113` bytes", "report size")
anchor = "## 0.18.16 Defense HUD·Presentation 감사\n"
sections = r'''## 0.18.19 후반 연구·용병 정체성·세이브 연속성 감사

- `VillageDefenseResearchSystem`의 기존 `mercenary/tower/logistics` 저장 ID를 유지한 채 각 연구 상한을 Lv.5 → Lv.10으로 확장했다. Lv.1~5의 기존 핵심 배율은 보존하고 Lv.6~10은 완만한 mastery curve를 사용한다.
- 용병 교리는 최대 연구에서 피해 x1.85, 치유 보정, 처치당 훈련 진척 최대 x3, 정원 보너스 최대 +5를 제공한다. 수동 감사에서 초기 수식이 기존 연구 Lv.2~4 정원을 줄이는 회귀를 발견해 기존 0/1/2/3/3/3 진행을 정확히 보존하도록 수정했다.
- `VillageMercenaryPresentationSystem`을 추가해 Iron Golem 기반 실제 판정/저장은 유지하면서 4개 병과에 owner-follow 절차 메시 실루엣을 부여했다. Lv.20/40/60에서 별도 시각 tier가 열린다.
- transient mercenary reset이 presentation map만 지워 중복 actor를 만들 수 있는 수명주기 문제를 수동 감사에서 발견했다. 서버 초기화에서만 map을 초기화하고 일반 transient reset에서는 유지하도록 교정했다.
- 포탑 연구가 실전 피해뿐 아니라 사거리와 최대 내구도에도 연결된다. 기존 0.18.18 풀피 포탑은 `v01819_turret_durability_migrated` 1회 마커를 사용해 새 최대 내구도로 이행하며, 이후 포탑 공학 강화는 각 포탑의 현재 HP 비율을 보존한다.
- 전리품 군수학은 기존 장비 드랍/판매 보너스 외에 전투 소모품 최대 25% 할인과 현장 포탑 수리 효율을 제공한다.
- 두 개 오래된 회귀검사가 포탑 연구의 옛 `+10%/level` 소스 문자열 자체를 고정하고 있어 새 mastery curve를 잘못 실패시켰다. 중복 연구 발사 제거/실제 배치 포탑 소유권이라는 행동 계약은 유지하고 수식 구현은 연구 시스템이 소유하도록 이관했다.
- 통합 게이트에서 canonical deterministic regressions, Java 25 clean NeoForge build, runtime JAR verifier를 통과한 뒤 source commit `78624b2d...`가 반영되었고, post-audit 게이트도 동일 검증을 통과한 뒤 `1bf96aa6...`가 반영됐다.
- 최종 acceptance Actions run `32328485511`: deterministic contracts PASS, Java 25 setup PASS, NeoForge clean build PASS, runtime JAR verifier PASS, artifact upload PASS, acceptance metadata 기록 PASS.
- 최종 artifact를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageDefenseResearchSystem`, `VillageMercenaryPresentationSystem`, `VillageMercenarySystem`, `VillagePlacedTurretSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary` 포함을 재검증했다.
- 최종 JAR SHA-256 `092566a1f57194df43a556b709af4d97615d596565f56035679f3c29f01b37e2`, 크기 `974113` bytes.

## 0.18.18 장기 강화·전투 소모품·성벽 전투 감사

- 유료 일반 식량을 일일 배급 식량으로 통합해 중복 역할을 제거하고 붕대/정화제/자극제/수호 비약/비전 촉진제/응급 포탑 수리 키트의 전투 소모품 축을 추가했다. 소모품은 서버가 기록한 custom data identity만 인정해 모루 이름 위조가 작동하지 않는다.
- 장비 강화 상한을 최대 +30까지 확장하고 무기 계열별 개별 상한 및 +10 이후 완만한 diminishing-return 구간을 적용했다.
- 기술·마법 연구소의 위력/지속 성장과 과도한 병영 쿨다운 중첩을 정리했다.
- 성벽에는 몬스터 통로가 되지 않는 높이의 사격구와 바깥으로 돌출된 수비 보행로/투하 구간을 추가해 닫힌 성벽 양면 전투를 개선했다.
- 용병 최대 레벨을 60으로 확장하고 원거리 사거리·치유 범위·직업 패시브를 장기 성장 곡선에 연결했다. 수동 감사에서 reload sanitize가 구식 Lv.5 상한으로 잘라버리는 문제를 발견해 `MAX_LEVEL` 기준으로 수정했다.
- Actions run `32224131813`: canonical regressions, Java 25 clean build, JAR verifier, artifact upload 모두 PASS. 최종 0.18.18 JAR SHA-256은 `42475cd9155e7c335a5f1f259740933e8ddc7f09bee97a7736a74fc6d8f8a9d6`, 크기 `967335` bytes.

'''
if "## 0.18.19 후반 연구·용병 정체성·세이브 연속성 감사" not in text:
    text = replace_once(text, anchor, sections + anchor, "report section anchor")
write(report, text)

print("[PASS] v0.18.19 README/report finalized")
