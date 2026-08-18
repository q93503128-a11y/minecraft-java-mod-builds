#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
META = ROOT / ".ci/v01814-final.txt"


def meta() -> dict[str, str]:
    result = {}
    for line in META.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            result[k.strip()] = v.strip()
    return result


def insert_before(text: str, marker: str, section: str) -> str:
    if section.splitlines()[0] in text:
        return text
    if marker not in text:
        raise SystemExit(f"marker missing: {marker}")
    return text.replace(marker, section.rstrip() + "\n\n" + marker, 1)


def main() -> None:
    m = meta()
    version = m["version"]
    run_id = m["run_id"]
    head = m["head_sha"]
    jar = m["jar_name"]
    sha = m["jar_sha256"]
    size = m["jar_size"]

    readme = README.read_text(encoding="utf-8")
    readme = readme.replace("현재 소스 버전 `0.18.13-alpha.1`", f"현재 소스 버전 `{version}`", 1)
    readme = readme.replace("목표 JAR `villageguardians-0.18.13-alpha.1.jar`", f"목표 JAR `{jar}`", 1)
    section = f'''## 0.18.14 포탑·정예 Presentation 실체화

0.18.13의 공격 연출에 이어, 이번에는 전장에 계속 남아 있는 방어 자산과 정예 자체가 임시 구현처럼 보이는 문제를 수동 감사했다.

- 포탑의 게임 상태·HP·레벨·배치 위치는 기존 SavedData `TurretState`가 계속 정본이다. 외형만 no-save `VillageSkillEffectEntity` actor로 분리해 표시층 손실이 저장 손실로 이어지지 않는다.
- 포탑 상부의 바닐라 식별 블록을 제거하고 3블록 충돌 footprint는 보이지 않는 `BARRIER` 셸로 유지한다. 실제 보이는 본체는 `VillageTurretPresentationSystem`이 10종 전용 procedural mesh로 생성한다.
- 중쇠뇌·연사포·철갑포·화염기·서리기·전격탑·투석포·억제탑·대공포·지원 봉화가 서로 다른 몸체와 포신/코어를 가지며, 목표 방향으로 실제 본체가 회전한다. 레벨이 오르면 체급·링·장식이 커지고 교란 중에는 에너지 감소와 적색 경고 링이 보인다.
- 포탑 파괴 시 상부 충돌 셸을 제거하고 실제 무너진 잔해 mesh를 표시하며, 철거·복구·재시작 시 actor가 남지 않도록 runtime actor map을 재검증한다.
- 갈고리병은 더 이상 성벽 앞에서 한 프레임 `snapTo`로 내부 순간이동하지 않는다. 18틱 Bézier 포물선 이동과 갈고리 선을 실제 월드 위치에 보여주며 성벽을 넘는다.
- 화염 투척병은 시전자 주변 즉발 범위 피해가 아니라, 18틱 전에 플레이어 위치를 스냅샷으로 고정해 실제 투척체 궤적을 보여준 뒤 그 지점 약 3.6블록에 착탄한다. 플레이어가 벗어나면 회피할 수 있다.
- 역병술사는 9블록 위험지대를 20틱 먼저 고정 표시하고 같은 지점에서 독·약화를 발동한다. 경고와 실제 판정 위치가 일치한다.
- 다섯 정예 교리는 지속 owner-follow aura를 가져 멀리서도 역할이 읽히며, 소유 몹이 죽거나 사라지면 actor도 즉시 폐기되어 유령 연출이 남지 않는다.
- 패배 후 같은 날 재도전/처음부터 재시작에서 SavedData만 되돌리고 메모리 포탑·충돌 셸·mesh actor·성벽 외형을 즉시 재투영하지 않던 복구 경계를 발견했다. `reloadAfterPersistenceChange`와 성벽 visual restore를 재시작 흐름에 연결했다.
- 첫 Java 25 컴파일에서 신규 presentation extra 파서의 `\\|` 정규식 escaping 오류를 실제 compiler로 발견해 수정했고, 동일한 잘못된 한-backslash 패턴이 Java 트리에 남지 않는 검사도 추가했다.
- 최종 acceptance run `{run_id}` / built head `{head}`에서 전체 계약, Java 25 clean build, JAR verifier, artifact upload가 모두 성공했다. 최종 JAR SHA-256 `{sha}`, 크기 `{size}` bytes다.
'''
    readme = insert_before(readme, "## 0.18.13 공성 방어 통합·연출 고도화", section)
    README.write_text(readme, encoding="utf-8")

    report = REPORT.read_text(encoding="utf-8")
    for old, new in {
        "- Current source version: `0.18.13-alpha.1`": f"- Current source version: `{version}`",
        "- Target JAR: `villageguardians-0.18.13-alpha.1.jar`": f"- Target JAR: `{jar}`",
        "- Final acceptance Actions run: `32087562708`": f"- Final acceptance Actions run: `{run_id}`",
        "- Final acceptance head: `cabf1c597cb70744f631bee44f6ed5a7561f2c06`": f"- Final acceptance head: `{head}`",
        "- Final JAR SHA-256: `c6d96eff852929f90fa11e888a6ebbc714252f0f47914f34e4e8852a539d2f2a`": f"- Final JAR SHA-256: `{sha}`",
        "- Final JAR size: `915862` bytes": f"- Final JAR size: `{size}` bytes",
    }.items():
        if old not in report:
            raise SystemExit(f"report header token missing: {old}")
        report = report.replace(old, new, 1)
    rsection = f'''## 0.18.14 지속형 Presentation·복구 감사

- `VillageTurretPresentationSystem`을 추가해 실제 포탑 SavedData와 runtime-only mesh actor를 분리했다. actor 누락은 1초 주기로 재생성되며 철거/잔해/재초기화에서는 정리된다.
- 활성 포탑 상부는 invisible `BARRIER` collision shell, 실제 보이는 기계는 10종 전용 procedural mesh가 담당한다. 포탑 목표 방향, 레벨, 교란 상태가 시각적으로 반영된다.
- `VillageEnemyEffectSystem`과 elite owner-follow actor를 추가해 다섯 정예 교리의 지속 식별 silhouette를 만들었다.
- Grappler는 18틱 Bézier traversal, Firebrand는 18틱 fixed-point projectile, Plague Weaver는 20틱 fixed danger-zone cast로 바뀌어 텔레그래프와 판정 좌표가 일치한다.
- 실패한 밤/새 게임 복구에서 persistence state를 `VillagePlacedTurretSystem.reloadAfterPersistenceChange`로 runtime state·collision shell·mesh actor에 즉시 재투영하고 `VillageSiegeSegmentSystem.restoreAllVisuals`로 성벽 projection도 즉시 동기화한다.
- 신규 presentation parser에서 Java 정규식 escape 오타를 1차 clean build가 발견했다. 수정 후 전체 17개 pre-build contract를 다시 통과하고 2차 Java 25 clean build가 성공했다.
- Actions run `{run_id}`: deterministic contracts PASS, Java 25/NeoForge clean build PASS, runtime JAR verification PASS, artifact upload PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, mod metadata/manifest, `VillageTurretPresentationSystem`, `VillageEnemyEffectSystem`, `VillageEnemyEliteSystem` 상태기계 inner class, `VillagePlacedTurretSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary$TurretPresentation`, `VillageCouncilState` 포함을 재검증했다.
- 최종 JAR SHA-256 `{sha}`, 크기 `{size}` bytes.
'''
    report = insert_before(report, "## 0.18.13 공성 통합·수동 감사", rsection)
    REPORT.write_text(report, encoding="utf-8")
    print(f"[PASS] finalized docs for {version} / run {run_id}")


if __name__ == "__main__":
    main()
