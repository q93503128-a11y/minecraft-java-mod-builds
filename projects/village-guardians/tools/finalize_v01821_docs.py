#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:160]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


readme = ROOT / "README.md"
replace_once(readme, "- 현재 소스 버전 `0.18.20-alpha.1`\n- 목표 JAR `villageguardians-0.18.20-alpha.1.jar`",
             "- 현재 소스 버전 `0.18.21-alpha.1`\n- 목표 JAR `villageguardians-0.18.21-alpha.1.jar`")
replace_once(readme,
'''낮 정비 → 장비·성장·시설·포탑·용병 준비 → 다음 밤 정찰 → 공성전 → 피해 복구·강화 → 다음 날짜로 진행한다.\n\n\n## 0.18.20 용병 명부·병영 경제 정합\n''',
'''낮 정비 → 장비·성장·시설·포탑·용병 준비 → 다음 밤 정찰 → 공성전 → 피해 복구·강화 → 다음 날짜로 진행한다.\n\n\n## 0.18.21 습격 수명주기·다전선 신호 정합\n\n- 습격 적 판정은 더 이상 커스텀 이름의 `웨이브` 문자열이나 보스 표시명을 신뢰하지 않는다. 서버의 활성 UUID·아키타입·웨이브 메타데이터와 내부 raid tag만 정본으로 사용한다.\n- 적이 죽거나 사라질 때 공격 전선, 정예 교리의 이동/투척/역병 시전, 보스 2페이즈와 지연 시전 상태를 해당 UUID 기준으로 즉시 해제한다. 승리·게임오버에서는 관련 transient map을 한 번에 비운다.\n- 게임오버 직후에도 현재 NIGHT를 새 밤 진입으로 오인해 전선 경고를 다시 만드는 현상을 차단했다.\n- 야간 직전 위험 방향은 연기·불 파티클만 쓰지 않고 동기화 절차 메시 `raid_front_warning`으로 표시한다. 실제 웨이브가 도착할 때는 사용되는 모든 전선에 `raid_front_arrival` 신호가 추가되며 주공은 더 강한 적색, 별동대는 황색 계열로 구분한다.\n- Phase 2 이후 호출되지 않던 구식 고정 성루 발사 루프를 `VillageDefenseSystem`에서 물리적으로 제거했다. 실전 자동 방어 화력 소유권은 `VillagePlacedTurretSystem` 하나로 고정된다.\n- 과거 0.17~0.18 회귀 테스트가 최신 릴리스 문자열 자체를 고정하던 부분을 제거해, 이전 버전 테스트는 기능 계약만 검사하고 현재 릴리스의 정확한 버전 검증은 최신 테스트가 담당한다.\n\n## 0.18.20 용병 명부·병영 경제 정합\n''')

report = ROOT / "BUILD_AND_RUNTIME_REPORT.md"
replace_once(report,
'''- Current source version: `0.18.20-alpha.1`\n- Minecraft: `26.2`\n- NeoForge build dependency: `26.2.0.37-beta`\n- Java target: `25`\n- Gradle: `9.2.1`\n- ModDevGradle: `2.0.143`\n- Target JAR: `villageguardians-0.18.20-alpha.1.jar`\n- Final acceptance Actions run: `32330475890`\n- Final acceptance head: `00a9ca1f4350d6dd431a4bd7ed2d6be817c50d7d`\n- Final JAR SHA-256: `8349e946556c23d45694274397989ff1212fb2998c204eab0967a0a6ff04d55c`\n- Final JAR size: `976814` bytes\n\n## 0.18.20 용병 명부·병영 경제·레거시 이관 감사\n''',
'''- Current source version: `0.18.21-alpha.1`\n- Minecraft: `26.2`\n- NeoForge build dependency: `26.2.0.37-beta`\n- Java target: `25`\n- Gradle: `9.2.1`\n- ModDevGradle: `2.0.143`\n- Target JAR: `villageguardians-0.18.21-alpha.1.jar`\n- Final acceptance Actions run: `32332392760`\n- Final acceptance head: `cb5268807406a982bc39e038cdbf5b59526e0f8c`\n- Final JAR SHA-256: `f39d02ef247387a669991cc6cc4dfb2c67a6f6949134fd4c1db0f720cfa5ab7e`\n- Final JAR size: `971022` bytes\n\n## 0.18.21 습격 정본 판정·수명주기·다전선 Presentation 감사\n\n- `VillageRaidSystem.isRaidEnemy`에서 커스텀 이름의 `웨이브`/보스 표시명 fallback을 제거했다. 실제 습격 적은 `ACTIVE_ENEMIES`, `ACTIVE_ARCHETYPES`, `ACTIVE_WAVES`, 내부 `villageguardians_raid_enemy` tag 중 하나로만 판정된다.\n- `releaseEnemy`가 `VillageAttackPlanSystem`, `VillageEnemyEliteSystem`, `VillageSiegeBossSystem`, `VillageBossAspectSystem`의 해당 UUID 상태를 즉시 제거한다. 사망/누락된 적의 전선·정예 지연 시전·보스 시전이 다음 웨이브까지 남지 않는다.\n- `clearState`는 승리와 게임오버에서 전선, 정예 교리, grapple/firebrand/plague cast, 보스 doctrine/phase-two/breach/ritual/duel cast를 즉시 비운다. AttackPlan cleanup은 현재 phase를 `lastPhase`에 저장해 게임오버 NIGHT를 새 밤 진입으로 잘못 감지하지 않는다.\n- `VillageDefenseEffectSystem`과 `VillageSkillMeshLibrary`에 `raid_front_warning` / `raid_front_arrival`을 추가했다. 야간 전 위험 전선과 실제 웨이브 사용 전선을 회전 링·chevron·수직 신호로 표시하며 주공/별동대 강도를 구분한다. 기존 smoke/flame은 보조 분위기 피드백만 담당한다.\n- 더 이상 서버 tick에서 호출되지 않던 `VillageDefenseSystem`의 `TOWER_TICKS`와 `fireBallista/fireFlame/fireFrost/fireArcane` 고정 성루 전투 구현을 완전히 제거했다. 컴파일된 최종 class에서도 해당 심볼이 없음을 독립 확인했다.\n- 통합 게이트 과정에서 `test_v0189_siege_phase2.py`, `test_v01818_growth_consumables.py` 등 과거 회귀가 최신 버전 문자열을 고정해 새 릴리스를 거짓 실패시키는 문제를 확인했다. 구 버전 테스트들의 `mod_version=0.18.x-alpha.1` 리터럴 고정을 기능 계약 검사로 이관했고, 최신 `test_v01821_raid_lifecycle_presentation.py`만 현재 정확한 버전을 확인한다.\n- 검증된 런타임 소스 commit은 `04a9c9c2d9b0996f781f334475399291de49ac65`다. 통합 게이트에서 canonical regressions, Java 25 clean NeoForge build, runtime JAR verifier를 모두 통과했다.\n- 최종 acceptance Actions run `32332392760`은 deterministic contracts, Java 25 setup, NeoForge clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `cb5268807406a982bc39e038cdbf5b59526e0f8c`다.\n- Actions artifact `9393481896`를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`의 mod version/NeoForge/Minecraft 범위, manifest, `VillageRaidSystem`, `VillageAttackPlanSystem`, `VillageEnemyEliteSystem`, `VillageSiegeBossSystem`, `VillageDefenseSystem`, `VillageDefenseEffectSystem`, `VillageSkillMeshLibrary` 포함을 독립 재검증했다. Artifact digest는 `sha256:4fb0f10652991ddccb846609c8e842e9025eba9560b17a5b3a2c4fd544ee219a`다.\n- 최종 JAR SHA-256 `f39d02ef247387a669991cc6cc4dfb2c67a6f6949134fd4c1db0f720cfa5ab7e`, 크기 `971022` bytes.\n\n## 0.18.20 용병 명부·병영 경제·레거시 이관 감사\n''')

print("[PASS] v0.18.21 canonical docs finalized")
