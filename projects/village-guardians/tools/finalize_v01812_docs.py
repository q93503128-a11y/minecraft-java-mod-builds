#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}: {count} for {old!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    replace_once(README, "- 현재 소스 버전 `0.18.11-alpha.1`", "- 현재 소스 버전 `0.18.12-alpha.1`")
    replace_once(README, "- 목표 JAR `villageguardians-0.18.11-alpha.1.jar`", "- 목표 JAR `villageguardians-0.18.12-alpha.1.jar`")
    marker = "## 장비·무기·세트\n"
    section = '''## 0.18.12 수동 품질 감사·전투 연출 정합화\n\n실제 플레이 테스트가 어려운 기간을 이용해 자동 검사만으로는 놓치기 쉬운 런타임 경계와 전투 읽기 문제를 수동으로 재검사했다.\n\n- 포탑 LOS 시작점이 포탑 자신의 실체 블록 내부에 있던 위험을 제거했다. 각 목표 방향으로 상단 캡 밖에 실제 muzzle을 만들고 그 위치에서 블록 충돌 LOS와 사격선을 계산한다.\n- 대공 포탑은 후보 순서가 아니라 실제 가장 가까운 고고도 적을 우선하며, 연쇄 전격은 모든 빔이 포탑에서 뻗는 별모양이 아니라 첫 목표에서 다음 목표로 이어지는 실제 연쇄 경로를 그린다.\n- 용병 지휘가 커스텀 이름 문자열을 다시 파싱하던 구조를 없애고 SavedData에서 복원된 병과 맵을 단일 정본으로 사용한다. 궁수·치유사는 랠리 복귀 중이 아닐 때 바닐라 철골렘의 근접 추격을 빠르게 억제한다.\n- 파성 거신이 가장 흔한 정면 북문 공성에서 고유 행동을 포기하던 조기 return을 제거했다. 2페이즈는 설명대로 파쇄 주기가 45틱에서 30틱으로 빨라지고, 파쇄 직전 경고와 충격 피드백이 추가됐다.\n- 사령 결속자의 의식, 검은 결투원수의 약화 베기, 갈고리병·화염 투척병·역병술사의 특수 행동에 발동 전 텔레그래프를 추가해 갑작스러운 판정보다 읽고 대응할 수 있는 전투로 바꿨다.\n- 암살자·돌파 기병은 서버 플레이어 목록의 우연한 순서가 아니라 실제 가장 가까운 생존 수호자를 고른다.\n- 보스 변이는 다운된 플레이어를 공격 후보에서 제외한다. 뇌광은 경고 시점의 고정된 지점을 저장하고 15틱 뒤 같은 지점에 떨어지므로 표시 밖으로 움직여 실제로 회피할 수 있다.\n- 신규 `tools/test_v01812_quality_audit.py`가 위 계약을 고정하고, 0.18.8~0.18.11 회귀 계약과 함께 Java 25 clean build에서 검증된다.\n- 최종 acceptance: Actions run `32083991529`, built head `dba4f47c4a4f79f9a36d9ee492762d6a49e76cfc`, JAR SHA-256 `2597579386b3a77c9a2423d70f560c03b710ee3ce91d6aeecf7662c7bd50cacd`.\n\n'''
    text = README.read_text(encoding="utf-8")
    if "## 0.18.12 수동 품질 감사·전투 연출 정합화" not in text:
        if marker not in text:
            raise SystemExit("README insertion marker missing")
        README.write_text(text.replace(marker, section + marker, 1), encoding="utf-8")

    replacements = {
        "- Current source version: `0.18.11-alpha.1`": "- Current source version: `0.18.12-alpha.1`",
        "- Target JAR: `villageguardians-0.18.11-alpha.1.jar`": "- Target JAR: `villageguardians-0.18.12-alpha.1.jar`",
        "- Final acceptance Actions run: `31583212244`": "- Final acceptance Actions run: `32083991529`",
        "- Final acceptance head: `c9971ef5acd00f30d84d683b12de0914d869c567`": "- Final acceptance head: `dba4f47c4a4f79f9a36d9ee492762d6a49e76cfc`",
        "- Final JAR SHA-256: `fc0b411c5c4400a44434f7d7205f1de94ceacce484c8917f0f64425a42def4b7`": "- Final JAR SHA-256: `2597579386b3a77c9a2423d70f560c03b710ee3ce91d6aeecf7662c7bd50cacd`",
        "- Final JAR size: `906456` bytes": "- Final JAR size: `908845` bytes",
    }
    for old, new in replacements.items():
        replace_once(REPORT, old, new)

    report_section = '''## 0.18.12 수동 품질 감사\n\n- 자동 테스트에 의존하지 않고 포탑, 용병, 정예, 공성 보스, 보스 변이의 실제 호출 흐름과 상태 경계를 수동으로 추적했다.\n- 포탑 LOS가 자기 3블록 실루엣에 막힐 수 있는 시작점 문제를 수정하고 목표 방향으로 캡 바깥 muzzle을 계산한다. 대공 우선순위와 연쇄 전격의 경로 연출도 실제 판정 순서와 맞췄다.\n- 용병 배치는 표시 이름 파싱을 제거하고 `VillageMercenarySystem.classOf`의 SavedData 기반 병과 상태를 정본으로 사용한다. 원거리/치유 병과의 바닐라 근접 경로도 랠리 복귀와 충돌하지 않도록 억제한다.\n- 파성 거신의 북문 no-op을 제거하고 2페이즈 파쇄 주기를 45→30틱으로 실제 단축했다. 사령 결속자와 검은 결투원수에도 발동 전 시각 경고를 추가했다.\n- 정예 화염/역병/갈고리 행동은 즉발 판정 전에 텔레그래프가 나오며, 암살자와 돌파 기병은 가장 가까운 유효 플레이어를 선택한다.\n- 보스 변이는 다운된 플레이어를 제외한다. 뇌광은 경고한 월드 좌표를 `STORM_WARNINGS`에 저장하고 같은 지점에서만 피해를 판정하여 실제 회피 가능한 공격이 됐다.\n- `tools/test_v01812_quality_audit.py`와 기존 0.18.8~0.18.11 계약을 모두 실행했다.\n- 최종 Actions run `32083991529`: Java 25 설정 PASS, deterministic contracts PASS, NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS.\n- 실제 Actions artifact를 다시 내려받아 JAR CRC/압축 무결성, `neoforge.mods.toml`, manifest, 수정 핵심 클래스 포함을 재검증했다.\n- 최종 JAR SHA-256 `2597579386b3a77c9a2423d70f560c03b710ee3ce91d6aeecf7662c7bd50cacd`, 크기 `908845` bytes.\n\n'''
    report = REPORT.read_text(encoding="utf-8")
    report_marker = "## 0.18.11 용병·포탑 안정화\n"
    if "## 0.18.12 수동 품질 감사" not in report:
        if report_marker not in report:
            raise SystemExit("report insertion marker missing")
        REPORT.write_text(report.replace(report_marker, report_section + report_marker, 1), encoding="utf-8")
    print("[OK] v0.18.12 canonical docs finalized")


if __name__ == "__main__":
    main()
