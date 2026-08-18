#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
META = ROOT / ".ci/v01813-final.txt"


def metadata() -> dict[str, str]:
    result: dict[str, str] = {}
    for line in META.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    required = {"version", "run_id", "head_sha", "jar_name", "jar_sha256", "jar_size"}
    missing = required - result.keys()
    if missing:
        raise SystemExit(f"missing acceptance metadata: {sorted(missing)}")
    return result


def insert_once(text: str, marker: str, section: str) -> str:
    if section.splitlines()[0] in text:
        return text
    if marker not in text:
        raise SystemExit(f"marker not found: {marker}")
    return text.replace(marker, section.rstrip() + "\n\n" + marker, 1)


def main() -> None:
    meta = metadata()
    version = meta["version"]
    run_id = meta["run_id"]
    head = meta["head_sha"]
    jar = meta["jar_name"]
    sha = meta["jar_sha256"]
    size = meta["jar_size"]

    readme = README.read_text(encoding="utf-8")
    readme = readme.replace("현재 소스 버전 `0.18.12-alpha.1`", f"현재 소스 버전 `{version}`", 1)
    readme = readme.replace("목표 JAR `villageguardians-0.18.12-alpha.1.jar`", f"목표 JAR `{jar}`", 1)
    readme_section = f'''## 0.18.13 공성 방어 통합·연출 고도화

0.18.12 이후 실제 플레이 테스트 없이도 확인 가능한 전투 호출 흐름, 저장 경계, AI 소유권, 방어 자산 연출을 다시 수동 감사했다.

- 적의 실제 웨이브를 커스텀 이름에서 다시 파싱하던 구조를 제거했다. `VillageRaidSystem`이 UUID별 실제 웨이브를 authoritative transient state로 보유하며, 보스도 실제 출현 웨이브의 전선·전장 상황을 사용한다.
- 일반 침공 AI와 적 병과 능력 모두 부활 대기 중인 플레이어를 목표에서 제외한다.
- 성벽·시설 공격을 모든 적이 같은 30틱에 몰아서 때리던 구조를 UUID 위상으로 분산해 평균 공성 압박은 유지하면서 한 틱 급락과 연출 겹침을 줄였다.
- 0.18.9 이후 폐기된 고정탑 추가 사격 `VillageTowerResearchBonusSystem`을 제거했다. 탑 연구 화력은 실제 배치 포탑의 `towerDamageMultiplier()` 한 경로만 사용하므로 중복 보정과 벽 관통 우회가 없다.
- 탑 사냥꾼은 이제 구형 성루 상태가 아니라 가장 가까운 실제 배치 포탑을 매 틱 일관된 전술 목표로 추적한다. 48블록 이내 포탑을 찾아 접근하고 근처 포탑의 사격 회로를 7초 교란하며, 포탑 HP 피해는 기존 근접 거리 계약을 유지한다.
- 포탑 10종과 용병 4병과의 전투 피드백을 플레이어 스킬과 같은 동기화 procedural-mesh actor 파이프라인으로 연결했다. 단순 직선 파티클 대신 병과·포탑 역할별 발사체, 빔, 펄스, 충격 형상을 사용한다.
- 광역 투석포는 즉시 방사형 피해에서 실제 곡사 포격으로 변경했다. 직접 LOS 없이 사거리 내 목표를 포착할 수 있지만 발사 지점을 고정한 뒤 12틱 동안 포탄 궤적을 보여주고 그 위치에 실제 착탄한다. 나머지 직접 사격 포탑은 기존 블록 LOS를 유지한다.
- 연쇄 전격은 mesh 경로와 실제 피해 hop이 같은 `arcStart/arcEnd` 좌표를 공유하고, 지원 봉화·수호병·공격병·궁수·의무병도 실제 행동 시점에 대응하는 동기화 연출을 사용한다.
- 신규 mesh 색상 페이드에서 ARGB의 하위 8비트를 알파로 잘못 취급하던 수동 감사 버그를 수정했다. `withAlpha`가 RGB를 보존하고 상위 8비트만 변경하도록 고정 계약을 추가했다.
- 오래된 core 테스트가 폐기된 고정탑/중복 사격 구조를 정답으로 강제하던 부분도 현행 배치 포탑 아키텍처로 이관했다. 최종 pre-build 16개 계약이 모두 PASS했다.
- 최종 Java 25 / Gradle 9.2.1 / NeoForge 26.2 clean build acceptance는 Actions run `{run_id}`, built head `{head}`이며 JAR SHA-256은 `{sha}`, 크기는 `{size}` bytes다.
'''
    readme = insert_once(readme, "## 0.18.12 수동 품질 감사·전투 연출 정합화", readme_section)
    README.write_text(readme, encoding="utf-8")

    report = REPORT.read_text(encoding="utf-8")
    replacements = {
        "- Current source version: `0.18.12-alpha.1`": f"- Current source version: `{version}`",
        "- Target JAR: `villageguardians-0.18.12-alpha.1.jar`": f"- Target JAR: `{jar}`",
        "- Final acceptance Actions run: `32083991529`": f"- Final acceptance Actions run: `{run_id}`",
        "- Final acceptance head: `dba4f47c4a4f79f9a36d9ee492762d6a49e76cfc`": f"- Final acceptance head: `{head}`",
        "- Final JAR SHA-256: `2597579386b3a77c9a2423d70f560c03b710ee3ce91d6aeecf7662c7bd50cacd`": f"- Final JAR SHA-256: `{sha}`",
        "- Final JAR size: `908845` bytes": f"- Final JAR size: `{size}` bytes",
    }
    for old, new in replacements.items():
        if old not in report:
            raise SystemExit(f"report header token not found: {old}")
        report = report.replace(old, new, 1)

    report_section = f'''## 0.18.13 공성 통합·수동 감사

- `VillageRaidSystem`에 UUID별 `ACTIVE_WAVES`를 추가하고 entity join 전에 archetype/wave 메타데이터를 등록한다. 보스·정예 전선은 더 이상 커스텀 표시 이름에 의존하지 않는다.
- 일반 플레이어 우선 추격과 적 병과 범위 능력에서 downed 플레이어를 제외했다.
- 시설 및 측·후방 Segment 공격 주기를 공격자 UUID로 stagger하여 동일 30틱 순간에 모든 적의 구조물 피해가 몰리지 않게 했다.
- `VillageTowerResearchBonusSystem`을 제거하고 실제 배치 포탑 본체의 연구 배율만 유지했다. 구형 고정 성루 전문화는 실전 포탑 화력/교란의 소유자가 아니다.
- 탑 사냥꾼은 48블록 내 가장 가까운 실제 배치 포탑을 전용 목표로 추적하고, 같은 근접 전선의 포탑을 7초 교란한다. 포탑 피해 해결 레이어는 경로 AI를 덮어쓰지 않는다.
- 10종 포탑 및 4종 용병 연출을 `VillageDefenseEffectSystem` → `VillageSkillEffectEntity` → `VillageSkillMeshLibrary` 동기화 procedural mesh 경로로 통합했다.
- 광역 투석포는 12틱 snapshot-position 곡사 포격이며 직접 사격용 LOS 필터의 유일한 의도적 예외다. 착탄 전 궤적과 착탄 지점이 보이고, 실제 피해는 도착 시점 주변 적에게 적용된다.
- mesh ARGB 페이드가 색상 채널을 훼손하던 문제를 `withAlpha`로 교정했다.
- `tools/test_enemy_content.py`, `tools/test_runtime_safety.py` 등 오래된 core 계약을 현행 배치 포탑 소유권으로 이관했다. 완전한 pre-build 16개 테스트 세트와 0.18.8~0.18.13 회귀가 PASS했다.
- Actions run `{run_id}`에서 Java 25 설정 PASS, deterministic contracts PASS, NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS, acceptance metadata 기록 PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageDefenseEffectSystem`, `VillagePlacedTurretSystem`, `VillageRaidSystem`, `VillageEnemyArchetypeSystem`, `VillageMercenarySystem`, `VillageSkillMeshLibrary` 클래스 포함을 다시 검증했다.
- 최종 JAR SHA-256 `{sha}`, 크기 `{size}` bytes.
'''
    report = insert_once(report, "## 0.18.12 수동 품질 감사", report_section)
    REPORT.write_text(report, encoding="utf-8")

    print(f"[PASS] canonical docs updated to {version} / run {run_id}")


if __name__ == "__main__":
    main()
