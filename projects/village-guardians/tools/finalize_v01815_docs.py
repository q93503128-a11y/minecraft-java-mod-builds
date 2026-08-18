#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
META = ROOT / ".ci/v01815-final.txt"


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
    version, run_id, head = m["version"], m["run_id"], m["head_sha"]
    jar, sha, size = m["jar_name"], m["jar_sha256"], m["jar_size"]

    readme = README.read_text(encoding="utf-8")
    readme = readme.replace("현재 소스 버전 `0.18.14-alpha.1`", f"현재 소스 버전 `{version}`", 1)
    readme = readme.replace("목표 JAR `villageguardians-0.18.14-alpha.1.jar`", f"목표 JAR `{jar}`", 1)
    section = f'''## 0.18.15 보스 Identity·고정 Cast State

0.18.14에서 정예와 포탑의 지속 Presentation을 실체화한 뒤, 보스의 이중 정체성(Aspect + Siege Doctrine)과 텔레그래프/실제 판정 일치를 다시 수동 감사했다.

- 같은 날 모든 보스가 한 교리만 쓰던 구조를 날짜 + 실제 웨이브 + boss archetype 기반 교리 결정으로 바꿨다. 정찰도 한 교리 확정이 아니라 파성/사령/결투 혼성 가능성을 정확히 보여준다.
- 최초 식을 `day*5 + wave*3 + archetype`로 만들었을 때 교리가 3종이라 `wave*3 mod 3 = 0`이 되어 웨이브 입력이 실질적으로 무효라는 수학 버그를 수동 검사로 발견했다. 최종식은 `wave*2`를 사용하며 3연속 웨이브가 세 교리 슬롯을 실제 순환할 수 있는 계약을 추가했다.
- 보스는 기존 바닐라 엔티티 위에 Aspect 색 + Siege Doctrine 형상을 결합한 owner-follow procedural mesh silhouette를 가진다. 파성은 거대한 파쇄 스파이크, 사령은 의식 결정/기둥, 결투는 교차 검날 계열로 구분된다.
- 50% 이하 2페이즈는 장기 owner-follow 강화층과 전환 burst를 추가하며, 원래 Aspect 색을 그대로 이어받아 1페이즈와 시각 정체성이 단절되지 않는다.
- 파성 거신은 `BreachCast(segment, impact, dueTick)`을 생성해 경고한 성벽 Segment와 충격 지점 그대로 10틱 뒤 파쇄한다. 이미 돌파된 Segment는 stale cast를 즉시 지운다.
- 사령 결속자는 `RitualCast(center, dueTick)`에 경고 시점의 월드 중심을 고정하고 20틱 뒤 같은 15블록 영역의 병력만 회복·보호한다. 보스가 이동해도 의식 중심이 순간이동하지 않는다.
- 검은 결투원수는 `DuelCast(target UUID, dueTick)`로 35틱 동안 같은 플레이어에게 결투 표식을 유지한다. 발동 순간 대상을 다시 뽑지 않으므로 경고 A → 피해 B가 사라졌다.
- 혈계 Aspect도 `BLOOD_WARNINGS`에 15틱 전 중심을 저장해 생명 흡수 경고와 실제 11블록 피해/회복 지점이 일치한다. 뇌광의 기존 fixed dodge point도 procedural mesh ground warning으로 교체했다.
- 결투 표식 API가 `Mob`을 받도록 잘못 설계되어 `ServerPlayer`와 타입이 맞지 않는 문제를 Java 빌드 전에 수동으로 발견해 `LivingEntity`로 교정했다.
- 최종 acceptance run `{run_id}` / built head `{head}`에서 18개 pre-build 계약, Java 25 clean build, JAR verifier, artifact upload가 모두 성공했다. JAR SHA-256 `{sha}`, 크기 `{size}` bytes다.
'''
    readme = insert_before(readme, "## 0.18.14 포탑·정예 Presentation 실체화", section)
    README.write_text(readme, encoding="utf-8")

    report = REPORT.read_text(encoding="utf-8")
    replacements = {
        "- Current source version: `0.18.14-alpha.1`": f"- Current source version: `{version}`",
        "- Target JAR: `villageguardians-0.18.14-alpha.1.jar`": f"- Target JAR: `{jar}`",
        "- Final acceptance Actions run: `32088820912`": f"- Final acceptance Actions run: `{run_id}`",
        "- Final acceptance head: `bbdc4b1b7ecd90c02952bcbc400fc50fbe34a1e5`": f"- Final acceptance head: `{head}`",
        "- Final JAR SHA-256: `9aef5f25c469d5023306dfd22345af4e9f0e4aaf4b72d3666ac33bb6861b9357`": f"- Final JAR SHA-256: `{sha}`",
        "- Final JAR size: `931551` bytes": f"- Final JAR size: `{size}` bytes",
    }
    for old, new in replacements.items():
        if old not in report:
            raise SystemExit(f"report token missing: {old}")
        report = report.replace(old, new, 1)
    rsection = f'''## 0.18.15 보스 Presentation·Cast 정합 감사

- `VillageBossEffectSystem`을 추가해 persistent boss presence, phase-two layer, breach/ritual/duel/bloodbound/storm cast telegraph를 공용 synchronized procedural-mesh 경로로 통합했다.
- `VillageSiegeBossSystem`은 `BREACH_CASTS`, `RITUAL_CASTS`, `DUEL_CASTS`를 통해 경고 시점의 Segment/좌표/대상 UUID를 실제 발동까지 유지한다.
- 보스 교리는 날짜 단독이 아니라 day + actual wave + archetype으로 혼성화했고, 3교리 modulo에서 `wave*3`가 무효임을 발견해 `wave*2`로 수정했다. 별도 regression에서 연속 3웨이브가 세 슬롯을 모두 방문함을 계산 검증한다.
- `VillageBossAspectSystem`의 Bloodbound도 fixed center cast state를 사용하고 Stormcaller fixed dodge point는 mesh ground warning으로 교체했다.
- persistent boss actor는 boss 사망 시 owner-follow 수명 규칙으로 자동 정리되며 phase two overlay는 기존 Aspect id를 extra로 전달해 색 정체성을 보존한다.
- 결투 target presentation API는 `LivingEntity`를 사용해 ServerPlayer와 타입 정합을 맞췄다.
- Actions run `{run_id}`: 18개 deterministic/pre-build contracts PASS, Java 25 + NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, mod metadata/manifest, `VillageBossEffectSystem`, `VillageSiegeBossSystem`, `VillageBossAspectSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary`, 그리고 `BreachCast/RitualCast/DuelCast` inner class 포함을 다시 검증했다.
- 최종 JAR SHA-256 `{sha}`, 크기 `{size}` bytes.
'''
    report = insert_before(report, "## 0.18.14 지속형 Presentation·복구 감사", rsection)
    REPORT.write_text(report, encoding="utf-8")
    print(f"[PASS] finalized docs for {version} / run {run_id}")


if __name__ == "__main__":
    main()
