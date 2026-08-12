#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"

VERSION = "0.18.10-alpha.1"
RUN = "31564184543"
HEAD = "067b910f149f52f4425725bd0a0fd70177daff37"
SHA256 = "6e24aa279b5f2fb29c91224ed404a8b084acfa843796a68c5099fd46f0e45209"
SIZE = "902784"


def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, got {count}")
    return text.replace(old, new, 1)


def patch_readme() -> None:
    text = README.read_text(encoding="utf-8")
    text = once(text, "- 현재 소스 버전 `0.18.9-alpha.1`", f"- 현재 소스 버전 `{VERSION}`", "README version")
    text = once(text, "- 목표 JAR `villageguardians-0.18.9-alpha.1.jar`", f"- 목표 JAR `villageguardians-{VERSION}.jar`", "README jar")
    text = once(
        text,
        "CI는 Java 25 + Gradle 9.2.1에서 기존 회귀 계약과 0.18.9 공성 계약을 먼저 실행하고, 이후 NeoForge `clean build`, `tools/verify_jar.py`, SHA-256 산출, JAR artifact 업로드를 수행한다.",
        "CI는 Java 25 + Gradle 9.2.1에서 기존 회귀 계약, 0.18.9 공성 계약, 0.18.10 추적 도탄 계약을 먼저 실행하고, 이후 NeoForge `clean build`, `tools/verify_jar.py`, SHA-256 산출, JAR artifact 업로드를 수행한다.",
        "README CI")
    marker = "## 주요 명령어\n"
    section = f'''## 0.18.10 추적 도탄 개편\n\n궁수의 `추적 도탄`은 더 이상 첫 적 적중 뒤 주변 대상에게 한 프레임에 피해를 뿌리는 방식이 아니다.\n\n- 최초 표적은 시야 중앙 전방 원뿔 안에서 선정한다.\n- 발사 순간에도 실제 화살 진행 벡터에서 목표 방향으로 부드럽게 보간해 급격한 방향 전환을 줄였다.\n- 비행 중 표적이 죽거나 사라지면 화살 진행 방향 전방에서 새 적을 재포착한다.\n- 재포착은 화살과 대상 사이 블록 충돌 검사를 통과해야 하므로 벽 너머 적을 억지로 쫓지 않는다.\n- 첫 적 적중 이후에는 이전 대상에서 가장 가까운 LOS 대상 순으로 실제 시간차 도탄한다.\n- 한 도탄 연쇄에서 같은 적은 다시 선택하지 않는다.\n- 기본 4회에서 특수 숙련에 따라 최대 8회까지 도탄하며, 연쇄 피해는 단계별로 완만하게 감소한다.\n- 2차 도탄 피해도 플레이어 처치로 귀속되어 정상 XP·주화 보상을 받을 수 있다.\n- 이미 계산된 1차 화살의 장비·직업·유물 배율은 2차 도탄에서 다시 곱하지 않으며 일반 궁수 패시브 도탄도 재귀 발동하지 않는다.\n\n최종 Java 25 acceptance run은 `{RUN}`, 검증 JAR SHA-256은 `{SHA256}`이다.\n\n'''
    text = once(text, marker, section + marker, "README ricochet section")
    README.write_text(text, encoding="utf-8")


def patch_report() -> None:
    text = REPORT.read_text(encoding="utf-8")
    replacements = [
        ("- Current source version: `0.18.9-alpha.1`", f"- Current source version: `{VERSION}`", "report version"),
        ("- Target JAR: `villageguardians-0.18.9-alpha.1.jar`", f"- Target JAR: `villageguardians-{VERSION}.jar`", "report jar"),
        ("- Final acceptance Actions run: `31561823343`", f"- Final acceptance Actions run: `{RUN}`", "report run"),
        ("- Final acceptance head: `d3a45e1358e181aa51656c28128148edfaf441cf`", f"- Final acceptance head: `{HEAD}`", "report head"),
        ("- Final JAR SHA-256: `46cae2f08d801bf5599052fcc5335dcaea8e31b0312c223a070efb701b6cc385`", f"- Final JAR SHA-256: `{SHA256}`", "report sha"),
        ("- Final JAR size: `898210` bytes", f"- Final JAR size: `{SIZE}` bytes", "report size"),
        ("| Java 25 NeoForge clean build | PASS | Actions run `31561823343` |", f"| Java 25 NeoForge clean build | PASS | Actions run `{RUN}` |", "report build row"),
        ("| Actions artifact upload | PASS | artifact `villageguardians-0.18.9-alpha.1` |", f"| Actions artifact upload | PASS | artifact `villageguardians-{VERSION}` |", "report artifact row"),
        ("GitHub Actions run `31561823343`은 Java 25 / NeoForge 26.2 환경에서 deterministic contract tests → clean build → JAR verifier → artifact upload까지 모두 성공했다.", f"GitHub Actions run `{RUN}`은 Java 25 / NeoForge 26.2 환경에서 deterministic contract tests → clean build → JAR verifier → artifact upload까지 모두 성공했다.", "report final run sentence"),
        ('META-INF/neoforge.mods.toml: version="0.18.9-alpha.1"', f'META-INF/neoforge.mods.toml: version="{VERSION}"', "report toml version"),
        ("META-INF/MANIFEST.MF: Specification-Version: 0.18.9-alpha.1", f"META-INF/MANIFEST.MF: Specification-Version: {VERSION}", "report spec version"),
        ("META-INF/MANIFEST.MF: Implementation-Version: 0.18.9-alpha.1", f"META-INF/MANIFEST.MF: Implementation-Version: {VERSION}", "report impl version"),
        ("size: 898210 bytes", f"size: {SIZE} bytes", "report final size"),
        ("SHA-256: 46cae2f08d801bf5599052fcc5335dcaea8e31b0312c223a070efb701b6cc385", f"SHA-256: {SHA256}", "report final sha"),
    ]
    for old, new, label in replacements:
        text = once(text, old, new, label)

    section = '''## 0.18.10 추적 도탄 수정\n\n0.18.9의 공성 확장 상태를 유지한 채 궁수 `추적 도탄`의 실제 동작과 보상 귀속을 재설계했다.\n\n- 최초 타깃 선정은 정규화된 전방 조준 원뿔을 사용한다.\n- 발사 직후 실제 화살 벡터와 목표 벡터를 보간해 순간 90도 급회전을 줄였다.\n- 이동 예측 유도는 현재 속도와 대상 속도를 사용하며 특수 숙련에 따라 회전 보간 강도가 증가한다.\n- 기존 표적 사망/소멸 시 비행 방향 전방 52블록 내 새 적을 재포착한다.\n- 재포착 후보는 화살 위치에서 대상 몸통까지 실제 블록 충돌이 없는 경우만 허용한다.\n- 첫 적중 후 주변 일괄 피해를 제거하고 이전 적 → 가장 가까운 LOS 적 순서의 nearest-neighbour 연쇄로 변경했다.\n- 도탄 대상 UUID를 방문 집합으로 관리해 같은 연쇄에서 중복 타격하지 않는다.\n- 도탄은 2틱 간격으로 순차 발생하며 기본 4회, 특수 숙련으로 최대 8회다.\n- 연쇄 피해는 이전의 선형 감소 대신 단계당 0.86배 감쇠로 변경해 음수/과도한 후반 감쇠를 방지한다.\n- 2차 도탄은 플레이어 공격 소스로 귀속해 처치 XP·주화·처치 기반 성장과 연결한다.\n- `PRE_SCALED_RICOCHET_DAMAGE` 가드로 이미 계산된 1차 화살 장비/직업/유물 배율의 이중 적용과 일반 전투기술 도탄 재귀를 차단한다.\n- 사용자 설명도 재포착·순차 도탄 동작과 일치하도록 갱신했다.\n\n전용 `test_v01810_ranger_ricochet.py`는 조준 원뿔, 부드러운 유도, 재포착, 블록 LOS, 중복 방지, 4~8회 도탄 상한, 피해 감쇠, 플레이어 귀속, 이중 배율/재귀 차단을 검증한다.\n\n'''
    text = once(text, "## 검증 결과\n", section + "## 검증 결과\n", "report section")
    text = once(
        text,
        "| 기존 RPG/영역/성장/요새/런타임 안전 계약 | PASS | 최종 run에서 재실행 |",
        "| 기존 RPG/영역/성장/요새/런타임 안전 계약 | PASS | 최종 run에서 재실행 |\n| 0.18.10 추적 도탄 계약 | PASS | 조준·유도·재포착·LOS·순차 도탄·보상 귀속·이중배율 차단 |",
        "report validation row")
    text = once(
        text,
        "6. 오래된 고정 성루 UI action이 남아 있을 수 있음 → 새 공성 지휘 UI로 production redirect 추가.",
        "6. 오래된 고정 성루 UI action이 남아 있을 수 있음 → 새 공성 지휘 UI로 production redirect 추가.\n7. 추적 도탄이 실제 순차 도탄이 아니라 첫 적중 시 주변 적에게 즉시 피해를 뿌리던 구조 확인 → nearest-neighbour 시간차 도탄으로 교체.\n8. 추적 대상 사망 시 유도가 즉시 종료되고 매 틱 방향을 강제 스냅하던 구조 확인 → 재포착 + 예측 보간 유도로 교체.\n9. 2차 도탄을 플레이어 귀속으로 단순 전환하면 RPG 공격 배율이 두 번 적용될 위험 확인 → pre-scaled damage guard 추가.\n10. 비행 중 재포착이 벽 너머 적을 선택할 가능성 확인 → 화살 기준 block LOS 검사 추가.",
        "report issues")
    text = once(
        text,
        "- 정예 5종과 보스 3구조의 체감 가독성/난이도",
        "- 정예 5종과 보스 3구조의 체감 가독성/난이도\n- 추적 도탄이 실제 전방 타깃을 자연스럽게 추적하고, 표적 사망 후 새 적을 재포착하며, 벽을 통과하지 않고 순차 도탄하는지",
        "report playtest")
    REPORT.write_text(text, encoding="utf-8")


def main() -> None:
    patch_readme()
    patch_report()
    print(f"[PASS] README and build report finalized for {VERSION}")
    print(f"[PASS] run={RUN} head={HEAD} sha256={SHA256} size={SIZE}")


if __name__ == "__main__":
    main()
