#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

repo = Path(__file__).resolve().parents[2]
project = repo / "projects/arcane-circle"
client = project / "src/main/java/kr/moonseungjun/arcanecircle/client"
tools = project / "tools"
workflows = repo / ".github/workflows"
scripts = repo / ".github/scripts"
target_version = "0.12.1-alpha.27"

retired_classes = [
    "CodexVisualLanguage", "ArcaneSigilDetailGrammar", "LowCircleVisualIdentity",
    "MidCircleVisualIdentity", "FifthCircleVisualIdentity", "SixthCircleVisualIdentity",
    "ArchmageVisualIdentity", "RangeReactivePresentation", "SpellVisualSignature",
    "CastingSilhouetteRenderer", "RobeRegaliaRenderer",
]

# Capture the only useful current validators/workflow before deleting historical tooling.
audit_text = (tools / "test_v0121_alpha26_visual_rewrite.py").read_text(encoding="utf-8")
verify_text = (tools / "verify_jar.py").read_text(encoding="utf-8")
workflow_text = (workflows / "build-arcane-circle-v0121.yml").read_text(encoding="utf-8")

# 1. Retired presentation source must be physically absent.
for name in retired_classes:
    (client / f"{name}.java").unlink(missing_ok=True)

# 2. Replace the accumulated project tools directory with two timeless validators.
shutil.rmtree(tools)
tools.mkdir(parents=True)
audit_text = audit_text.replace("0.12.1-alpha.26", target_version)
audit_text = audit_text.replace("Arcane Circle alpha.26 ground-up visual rewrite audit", "Arcane Circle current-source audit")
audit_text += r'''

# Active-tree hygiene. Git history is the archive; current source contains no version-migration machinery.
repo=root.parents[1]
retired_tokens=[n.removesuffix('.java') for n in retired]
for path in (root/'src').rglob('*.java'):
    body=text(path)
    for token in retired_tokens:
        assert token not in body, f'retired design reference remains: {token} in {path.relative_to(root)}'

tools_dir=root/'tools'
assert {p.name for p in tools_dir.iterdir() if p.is_file()} == {'test_current_source.py','verify_jar.py'}
assert not [p for p in tools_dir.iterdir() if p.is_dir()], 'legacy tool directories remain'

scripts_dir=repo/'.github/scripts'
if scripts_dir.exists():
    assert not (scripts_dir/'arcane-circle').exists(), 'legacy Arcane migration directory remains'
    assert not list(scripts_dir.glob('*arcane*')), 'legacy Arcane patch/migration script remains'

workflow_dir=repo/'.github/workflows'
arcane_workflows=sorted(p.name for p in workflow_dir.glob('*arcane*.yml'))
assert arcane_workflows == ['build-arcane-circle.yml'], f'legacy Arcane workflows remain: {arcane_workflows}'

for obsolete in ['AUDIT_REPORT_V0.5.md','BUILD_AND_RUNTIME_REPORT.md','MAGIC_WORLD_PATCH.md',
                 'docs/ALPHA10_WORLD_COMBAT.md','docs/PRESENTATION_OVERHAUL_PHASES.md']:
    assert not (root/obsolete).exists(), f'obsolete project document remains: {obsolete}'

print('legacy_arcane_tooling=absent')
print('arcane_workflows=1')
'''
(tools / "test_current_source.py").write_text(audit_text, encoding="utf-8")

# Strengthen JAR validation so old presentation bytecode cannot return.
required_anchor = '    "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class",\n'
required_extra = ''.join(f'    "kr/moonseungjun/arcanecircle/client/{name}.class",\n' for name in [
    "SpellCinematicDirector", "GrimoireScreen", "ArcaneHud", "ArcaneRegaliaRenderer",
    "ArcaneCastingPerformance", "ArcaneGearRenderer", "ArcaneWorldMesh",
])
if "SpellCinematicDirector.class" not in verify_text:
    verify_text = verify_text.replace(required_anchor, required_anchor + required_extra)
index_anchor = '    index = json.loads(archive.read("data/arcanecircle/spell_catalog/index.json"))\n'
retired_literal = repr(retired_classes)
jar_guard = f'''    retired = {retired_literal}\n    leaked = [n for n in names if any(n.endswith('/'+c+'.class') or ('/'+c+'$') in n for c in retired)]\n    if leaked:\n        raise SystemExit(f"retired presentation bytecode leaked: {{sorted(leaked)}}")\n'''
if "retired presentation bytecode leaked" not in verify_text:
    verify_text = verify_text.replace(index_anchor, jar_guard + index_anchor)
version_guard = '''    version = index.get("version")\n    if not isinstance(version, str) or not version:\n        raise SystemExit("catalog version missing")\n    if jar.name != f"arcanecircle-{version}.jar":\n        raise SystemExit(f"JAR/version mismatch: {jar.name} vs {version}")\n'''
if "JAR/version mismatch" not in verify_text:
    verify_text = verify_text.replace(index_anchor, index_anchor + version_guard)
(tools / "verify_jar.py").write_text(verify_text, encoding="utf-8")

# 3. Remove every old Arcane patch/migration artifact, including this one-shot driver.
for path in list(scripts.glob("*arcane*")):
    if path.is_dir():
        shutil.rmtree(path)
    else:
        path.unlink(missing_ok=True)

# 4. Delete stale reports and phase-specific documentation. Git history remains the archive.
for rel in ["AUDIT_REPORT_V0.5.md", "BUILD_AND_RUNTIME_REPORT.md", "MAGIC_WORLD_PATCH.md"]:
    (project / rel).unlink(missing_ok=True)
for rel in ["ALPHA10_WORLD_COMBAT.md", "PRESENTATION_OVERHAUL_PHASES.md"]:
    (project / "docs" / rel).unlink(missing_ok=True)

(project / "README.md").write_text(r'''# Arcane Circle: Ninefold Arcana

Minecraft Java 26.2 + NeoForge 26.2.0.38-beta + Java 25 기반 1~9써클 마법 RPG 모드다. 현재 버전은 `gradle.properties`의 `mod_version`을 단일 기준으로 사용한다.

## 현재 콘텐츠
- 직접 주문 90개, 융합 주문 19개, 1~9써클 전체 구현
- 주문서 학습, 숙련, 마력·쿨타임·시전시간 성장
- 소속·마법사 NPC·마법 세계·아카데미·경제·의뢰
- 지팡이/로브 장비와 테스트 키트
- 서버 권위 시전·판정과 클라이언트 월드 연출 동기화

## 조작
- `C`: 구중 마도서
- `1`~`5` 누르기: 장착 주문 전개/유지
- `1`~`5` 놓기: 준비된 주문 발동
- `X` + `1`~`5`: 최대 3개 주문을 융합 대기열에 추가
- `X` 놓기: 완성된 융합 주문 발동

시전시간이 0초인 주문도 누르는 순간 자동 발사하지 않고 ready-hold 후 release에서 발동한다.

## 현재 presentation
- `GrimoireScreen`: 기능 인덱스 + 1~9써클 인장 + 주문 브라우저 + 선택 주문 상세 + 장착 스트립
- `ArcaneHud`: 주문 인장형 전투 HUD
- `SpellCinematicDirector`: 주문의 실제 공간 사건을 기준으로 한 월드 연출
- `ArcaneRegaliaRenderer`: 복장별 독립 실루엣
- `ArcaneCastingPerformance`: snap/aim/heavy/ground/ward/portal/ritual 시전 자세

모든 주문에 같은 범용 마법진을 덧씌우지 않는다. 높은 써클이라는 이유만으로 항상 더 큰 원을 그리지 않으며, 주문의 역할·위치·고도·평면·체적·이동·충돌·잔류가 디자인의 시작점이다.

## 빌드와 검사
```bash
chmod +x gradlew
python3 tools/test_current_source.py
./gradlew --no-daemon --no-configuration-cache clean build
python3 tools/verify_jar.py "build/libs/arcanecircle-$(sed -n 's/^mod_version=//p' gradle.properties).jar"
```

Arcane 전용 정식 CI는 `.github/workflows/build-arcane-circle.yml` 하나만 유지한다. 과거 버전용 apply/fix/migration 스크립트와 구형 presentation 구현은 active tree에 보존하지 않는다.
''', encoding="utf-8")

(project / "PROJECT.md").write_text(r'''# Arcane Circle: Ninefold Arcana — Project Contract

- Mod ID / namespace: `arcanecircle`
- Version source: `gradle.properties -> mod_version`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / Gradle 9.2.1
- Direct spells 90 / Fusion spells 19 / Circles 1~9
- Canonical CI: `.github/workflows/build-arcane-circle.yml`
- Source audit: `tools/test_current_source.py`
- JAR audit: `tools/verify_jar.py`

게임 데이터·마력·숙련·시전·네트워크·판정은 서버 권위다. 0초 시전도 ready-hold 후 release 발동을 유지한다. 현재 presentation 정본은 `GrimoireScreen`, `ArcaneHud`, `SpellCinematicDirector`, `ArcaneRegaliaRenderer`, `ArcaneCastingPerformance`이며 구형 presentation 클래스와 버전별 migration/apply/fix 도구는 active tree에 두지 않는다.
''', encoding="utf-8")

(project / "CHANGELOG.md").write_text(r'''# Changelog

## Current 0.12.1 alpha line
- 과거 카드형 마도서와 네모 주문 핫바 presentation을 폐기했다.
- 공통 마법진 장식층과 써클별 범용 VisualIdentity 체계를 제거했다.
- `SpellCinematicDirector`가 주문의 실제 공간 사건을 기준으로 연출한다.
- 복장과 시전 동작을 `ArcaneRegaliaRenderer` / `ArcaneCastingPerformance`로 분리했다.
- 구형 presentation 클래스, 과거 전용 CI, 버전별 apply/fix/migration 도구를 active tree에서 제거했다.
- 1~9써클, 직접 주문 90개, 융합 주문 19개, ready-hold/release, 서버 권위 판정, `presentationImpactDelay`, `syncAtomicRobe` 계약은 유지한다.

과거 세부 변경 이력은 Git history가 보존한다.
''', encoding="utf-8")

canon = project / "docs/MAGIC_WORLD_CANON.md"
canon_text = canon.read_text(encoding="utf-8")
new_visual = '''## 3. Presentation 절대 규칙\n\n높은 써클 = 무조건 더 큰 원이라는 규칙은 없다. 설계는 주문이 세계에서 실제로 어떤 사건인지 정의하는 것부터 시작한다. 위치·방향·고도·평면·체적을 정하고 준비 과정의 조립, release 이후 이동·전파·충돌·잔류를 설계한 다음 마지막에 써클에 맞는 복잡도와 체급을 조정한다.\n\n현재 사건 형태는 `NEEDLE / ORB / VOLLEY / RAY / CONE / FIELD / WALL / GATE / PRISON / SKY / WEATHER / AURA / MARK / SHIFT / TRANSFORM / CLOCK / TERRAIN / DOMAIN`이다. 모든 주문 위에 동일한 범용 마법진을 덧씌우지 않는다. Power Word Kill은 9써클이어도 작고 정밀할 수 있고 Meteor Swarm은 하늘 전체를 사용하는 대규모 사건이어야 한다. 보이는 범위와 게임플레이 판정 범위는 가능한 한 공유하며 고위 마법을 단순 파티클 덩어리로 대체하지 않는다.\n\n'''
canon_text = re.sub(r"## 3\. 마법진 시각 규칙.*?(?=## 4\. 소속 체계)", new_visual, canon_text, flags=re.S)
canon_text = re.sub(r"## 8\. 구현 우선순위.*$", "## 8. 구현 원칙\n\n게임플레이 결과는 서버 권위다. 클라이언트 presentation은 판정의 원인이 아니라 결과를 정확히 보여주는 계층이다. UI·연출·복장은 교체 가능해야 하며 게임 데이터와 네트워크 계약에 불필요하게 결합하지 않는다.\n", canon_text, flags=re.S)
canon.write_text(canon_text, encoding="utf-8")

# 5. Collapse all historical Arcane workflows into one canonical filename.
workflow_text = workflow_text.replace("name: Build Arcane Circle v0.12.1 Alpha.26", "name: Build Arcane Circle")
workflow_text = workflow_text.replace(".github/workflows/build-arcane-circle-v0121.yml", ".github/workflows/build-arcane-circle.yml")
workflow_text = workflow_text.replace("arcane-circle-v0121-main", "arcane-circle-main")
workflow_text = workflow_text.replace("0.12.1-alpha.26", target_version)
workflow_text = workflow_text.replace("test_v0121_alpha26_visual_rewrite.py", "test_current_source.py")
workflow_text = workflow_text.replace("Java 25 - alpha.26 ground-up UI VFX regalia", "Java 25 - current Arcane Circle")
workflow_text = workflow_text.replace("Audit committed alpha.26 source without mutation", "Audit current source without mutation")
workflow_text = workflow_text.replace("Verify alpha.26 JAR and retired stack absence", "Verify current JAR and retired stack absence")
workflow_text = workflow_text.replace("Arcane Circle alpha.26 committed-source JAR audit", "Arcane Circle current committed-source JAR audit")
workflow_text = workflow_text.replace("presentation_phase=alpha26-ground-up-ui-vfx-regalia", "presentation_phase=current-cinematic-ui-vfx-regalia\n          legacy_arcane_tooling=absent\n          repository_cleanup=complete")
for path in list(workflows.glob("*arcane*.yml")):
    path.unlink(missing_ok=True)
(workflows / "build-arcane-circle.yml").write_text(workflow_text, encoding="utf-8")

# 6. Bump current build line; gameplay data is otherwise untouched.
gradle = project / "gradle.properties"
gradle_text = re.sub(r"(?m)^mod_version=.*$", f"mod_version={target_version}", gradle.read_text(encoding="utf-8"))
gradle.write_text(gradle_text, encoding="utf-8")
main = project / "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
main_text = re.sub(r'public static final String VERSION = "[^"]*";', f'public static final String VERSION = "{target_version}";', main.read_text(encoding="utf-8"))
main.write_text(main_text, encoding="utf-8")
index = project / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
data = json.loads(index.read_text(encoding="utf-8"))
data["version"] = target_version
index.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("Arcane Circle purge materialized")
print("tools:", sorted(p.name for p in tools.iterdir()))
print("workflows:", sorted(p.name for p in workflows.glob("*arcane*.yml")))
print("scripts:", sorted(p.name for p in scripts.glob("*arcane*")))
