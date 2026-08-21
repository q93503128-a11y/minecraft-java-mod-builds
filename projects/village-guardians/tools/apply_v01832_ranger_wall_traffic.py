#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise SystemExit(f"expected source fragment missing: {path}: {old[:80]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    props = ROOT / "gradle.properties"
    readme = ROOT / "README.md"
    deployment = JAVA / "VillageMercenaryDeploymentSystem.java"
    old_test = ROOT / "tools/test_v01831_pretest_hardening.py"

    replace_once(
        props,
        "mod_version=0.18.31-alpha.1",
        "mod_version=0.18.32-alpha.1",
    )
    replace_once(
        readme,
        "- 현재 소스 버전 `0.18.31-alpha.1`\n- 목표 JAR `villageguardians-0.18.31-alpha.1.jar`",
        "- 현재 소스 버전 `0.18.32-alpha.1`\n- 목표 JAR `villageguardians-0.18.32-alpha.1.jar`",
    )

    section = """## 0.18.32 성벽 계단 교통·성루 명사수 전투선 분리 안정화

- 0.18.31의 UUID 10슬롯 자체는 여러 명사수의 완전 중첩을 줄였지만, 최종 WALL 슬롯 5개씩이 북측 두 계단의 상부 착지 통로와 그대로 겹쳐 여러 명사수를 배치할수록 계단 출구를 물리적으로 막을 수 있었다.
- 계단 아래 집결점은 기존 `±25 / z=-62`를 유지해 실제 북측 계단으로 접근하게 하고, 계단을 오른 뒤의 최종 WALL 거점만 북벽 좌우 전투선으로 분리했다.
- 최종 거점은 북벽 `x=-56~-40` 및 `x=40~56`, `z=-74`에 4블록 간격으로 총 10개 배치된다. 두 북측 계단 착지부와 성벽 포좌 연결부를 모두 비워 플레이어·용병·포탑 정비 동선이 서로 막히지 않는다.
- 성루 명사수의 실제 사격은 기존처럼 물리 LOS와 공중 위협 우선순위를 사용하며, 사격 코드가 navigation을 소유하지 않는 기존 구조를 보존한다. 배치 시스템만 복귀/거점 이동을 소유한다.
- 새 회귀검사는 단순히 "10슬롯이 존재하는가"가 아니라 계산된 10개 좌표가 계단 착지 폭과 겹치지 않는지, 서로 충분히 분산되는지, 포좌 연결부와 안전거리를 갖는지까지 검증한다.

"""
    readme_text = readme.read_text(encoding="utf-8")
    marker = "## 0.18.31 실플레이 직전 성벽 동선·성루 명사수 배치 안정화"
    if section.strip() not in readme_text:
        if marker not in readme_text:
            raise SystemExit("README v0.18.31 marker missing")
        readme.write_text(readme_text.replace(marker, section + marker, 1), encoding="utf-8")

    old_posts = """        int lane = slot < 5 ? -25 : 25;
        int spread = slot % 5 - 2;
        return center.offset(lane + spread, 9, -74);"""
    new_posts = """        // Keep the stair landing itself open. Rangers approach through the ±25 stairs,
        // then spread along the north-wall firing line, clear of stair and emplacement traffic.
        int lane = slot < 5 ? -48 : 48;
        int spread = (slot % 5 - 2) * 4;
        return center.offset(lane + spread, 9, -74);"""
    replace_once(deployment, old_posts, new_posts)

    replace_once(
        old_test,
        '    assert "mod_version=0.18.31-alpha.1" in props\n    assert "0.18.31-alpha.1" in readme and "villageguardians-0.18.31-alpha.1.jar" in readme',
        '    assert "mod_version=" in props\n    assert "현재 소스 버전" in readme and "목표 JAR" in readme',
    )
    replace_once(
        old_test,
        '    assert "center.offset(lane + spread, 9, -74)" in deployment',
        '    assert "return center.offset(lane + spread, 9, -74)" in deployment',
    )

    print("[PATCH] Village Guardians 0.18.32 ranger wall traffic separation applied")


if __name__ == "__main__":
    main()
