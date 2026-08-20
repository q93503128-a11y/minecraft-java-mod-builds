#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
REPORT = ROOT / "BUILD_AND_RUNTIME_REPORT.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label} anchor: {old!r}")
    return text.replace(old, new, 1)


readme = README.read_text(encoding="utf-8")
readme = replace_once(readme, "현재 소스 버전 `0.18.21-alpha.1`", "현재 소스 버전 `0.18.22-alpha.1`", "README version")
readme = replace_once(readme, "목표 JAR `villageguardians-0.18.21-alpha.1.jar`", "목표 JAR `villageguardians-0.18.22-alpha.1.jar`", "README jar")
section = '''## 0.18.22 승인된 UI 복원 · 후속 기능 보존\n\n- 0.18.16에서 도입됐지만 실제 화면 평가에서 기각된 중앙 집중형 Defense HUD frame/theme을 제거하고, 승인됐던 0.18.15 계열의 작고 낮은 전투 HUD 언어로 복원했다.\n- 메인 HUD는 좌상단 2줄 컴팩트 상태 카드로, 스킬 HUD는 바닐라 핫바 위의 낮은 2슬롯 표시로 돌아갔다. 시설 내구도 BossBar도 긴급 경보 전용이 아니라 습격 중 손상 시설 우선/순환 표시 방식으로 복구했다.\n- `VillageDefenseHudFrame`과 `VillageDefenseUiTheme`은 런타임 소스와 최종 JAR에서 물리적으로 제거했다. 회관/시설 화면은 0.18.18~0.18.21에 추가된 기능을 유지하면서 자체 로컬 팔레트를 사용한다.\n- UI만 되돌렸으며 포탑 설치/배치/수리/강화 피드백, 성벽 돌파 경보, 용병/포탑 절차 메시, `raid_front_warning`/`raid_front_arrival` 등 월드 Presentation은 그대로 유지한다.\n- 장비 +30 장기 강화/무기별 상한, 전투 소모품, 연구 Lv.10, 용병 Lv.60·4병과 명부, 성벽 사격구·보행로, 습격 UUID 수명주기 등 0.18.18~0.18.21 시스템의 회귀를 모두 다시 검사했다.\n- 기존 `test_runtime_safety.py`에 남아 있던 기각 UI의 `-112 + abilityCard` 강제 조건을 승인 UI의 `-98 + 저프로필 diamond slot` 안전 계약으로 수정했고, 기각된 `test_v01816_mobile_defense_ui.py` 자체는 제거했다.\n\n'''
anchor = "## 0.18.21 습격 수명주기·다전선 신호 정합\n"
if section.strip() not in readme:
    readme = replace_once(readme, anchor, section + anchor, "README 0.18.21 section")
README.write_text(readme, encoding="utf-8")

report = REPORT.read_text(encoding="utf-8")
for old, new, label in (
    ("Current source version: `0.18.21-alpha.1`", "Current source version: `0.18.22-alpha.1`", "report version"),
    ("Target JAR: `villageguardians-0.18.21-alpha.1.jar`", "Target JAR: `villageguardians-0.18.22-alpha.1.jar`", "report jar"),
    ("Final acceptance Actions run: `32332392760`", "Final acceptance Actions run: `32338345420`", "report run"),
    ("Final acceptance head: `cb5268807406a982bc39e038cdbf5b59526e0f8c`", "Final acceptance head: `f27a50b1c7fd1632fb4443b908c8e83d73846885`", "report head"),
    ("Final JAR SHA-256: `f39d02ef247387a669991cc6cc4dfb2c67a6f6949134fd4c1db0f720cfa5ab7e`", "Final JAR SHA-256: `cd7712bacac503748ff0a03e3f6a76f5ae90df6d9322fea674e156225e67f990`", "report sha"),
    ("Final JAR size: `971022` bytes", "Final JAR size: `958353` bytes", "report size"),
):
    report = replace_once(report, old, new, label)

report_section = '''## 0.18.22 UI 롤백 완결 · 후속 시스템 보존 감사\n\n- 사용자 화면 평가에서 기각된 0.18.16 중앙 집중형 Defense HUD를 실제 정본에서 제거했다. `VillageMainHudOverlay`, `VillageHudSystem`, `VillageSkillHudOverlay`, `VillageStructureHud`는 승인된 pre-defense-pass 동작을 복원했고, Command Center/Town Hall은 최신 기능을 유지한 채 기존 로컬 팔레트로 복귀했다.\n- `VillageDefenseHudFrame.java`, `VillageDefenseUiTheme.java`, HUD 전용 `VillageRaidSystem.RaidHudSnapshot`, 기각 UI를 다시 요구하던 `test_v01816_mobile_defense_ui.py`를 제거했다.\n- 반대로 `VillageDefenseEffectSystem`의 turret placement/deploy/repair/upgrade, breach alarm, 0.18.21 front warning/arrival과 포탑·용병·보스 절차 메시 Presentation은 그대로 유지했다. 즉 UI 외형 롤백이 월드 전투 Presentation 롤백으로 번지지 않았다.\n- 0.18.18 장기 강화·전투 소모품·성벽 전투 갤러리·Lv.60 용병, 0.18.19 Lv.10 연구·포탑 내구/사거리·용병 Presentation, 0.18.20 4병과 명부/레거시 이관, 0.18.21 authoritative raid lifecycle/directional signal 회귀를 전부 재검사했다.\n- 통합 중 `test_runtime_safety.py`가 기각 UI의 `guiHeight()-112`와 `abilityCard`를 영구 요구하던 오류를 발견해 승인 UI의 `guiHeight()-98` 저프로필 안전 계약으로 수정했다. 신규 0.18.22 테스트의 잘못된 소모품 클래스명도 실제 소유자 `VillageConsumableSystem`으로 교정했다.\n- 검증된 런타임 롤백 source commit은 `2c4c97ff7335e16d063bc7e2f1202f5322c5bafc`다. integration run `32338131966`에서 canonical regressions, Java 25 clean NeoForge build, runtime JAR verifier가 모두 PASS했다.\n- 최종 acceptance Actions run `32338345420`은 deterministic contracts, Java 25 setup, clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `f27a50b1c7fd1632fb4443b908c8e83d73846885`다.\n- Actions artifact `9395435283`을 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`의 0.18.22/NeoForge/Minecraft 범위, manifest, `VillageMainHudOverlay`, `VillageSkillHudOverlay`, `VillageStructureHud`, `VillageConsumableSystem`, `VillageDefenseResearchSystem`, `VillageMercenarySystem`, `VillageDefenseEffectSystem` 포함과 rejected frame/theme class 부재를 독립 재검증했다. Artifact digest는 `sha256:a0044850a650cf315ff96b0bd5c298d32fb2267c9a243af3933dabebbc299c2d`다.\n- 최종 JAR SHA-256 `cd7712bacac503748ff0a03e3f6a76f5ae90df6d9322fea674e156225e67f990`, 크기 `958353` bytes.\n\n'''
report_anchor = "## 0.18.21 습격 정본 판정·수명주기·다전선 Presentation 감사\n"
if report_section.strip() not in report:
    report = replace_once(report, report_anchor, report_section + report_anchor, "report 0.18.21 section")
REPORT.write_text(report, encoding="utf-8")

print("[PASS] v0.18.22 canonical docs finalized")
