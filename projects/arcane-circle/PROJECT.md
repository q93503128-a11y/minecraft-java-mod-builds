# Arcane Circle: Ninefold Arcana — Project Contract

- Mod ID / namespace: `arcanecircle`
- Version source: `gradle.properties -> mod_version`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / Gradle 9.2.1
- Direct spells 90 / Fusion spells 19 / Circles 1~9
- Canonical CI: `.github/workflows/build-arcane-circle.yml`
- Source audit: `tools/test_current_source.py`
- JAR audit: `tools/verify_jar.py`

게임 데이터·마력·숙련·시전·네트워크·판정은 서버 권위다. 0초 시전도 ready-hold 후 release 발동을 유지한다. 현재 presentation 정본은 `ArcaneSigilDirector`, `SpellCinematicDirector`, `ArcaneSpellVisualOverhaul`, `WorldMagicTracker`, `ArcaneRegaliaRenderer`다. `ArcaneCastingPerformance`는 호환 훅만 남기고 별도 플레이어 신체 지오메트리를 그리지 않는다. 구형 자동 presentation director와 버전별 migration/apply/fix 도구는 active tree에 두지 않는다.

alpha.42는 alpha.40~41의 manual-only replacement를 품질 회귀로 폐기한다. alpha.39의 검증된 layered sigil + cinematic + spell-overhaul presentation을 기준선으로 복구하고, 이후 수동 개선은 이 기준선을 제거하지 않은 채 주문별로 진행한다. 지팡이 시전시간 배율은 직접 주문과 융합 주문의 시전 계산 및 하한에 실제로 참여한다.

## Alpha.34 runtime contracts

- `CastTargetSnapshot`은 release 순간 절대 목표점·발사 원점/방향·대상 UUID·impact surface·dimension·barrage seed를 한 번만 고정한다. 비추적 주문은 이후 시선을 다시 읽지 않는다.
- Meteor Swarm은 플레이어/NPC/client가 동일 seed와 `MeteorBarragePattern`을 사용하며 16발 staggered barrage를 유지한다.
- 3인칭 시전 연출은 더 이상 `RenderPlayerEvent.Post`에서 가짜 팔/끈/칼날 filled-box geometry를 덧그리지 않는다. 주문 마법진과 실제 spell presentation만 시전 presentation을 담당한다.
- Prismatic Wall은 7색 panel만 렌더한다. 기존 흰색 structural base frame은 제출하지 않는다.
- 주문도감 상세 설명은 `SpellEffectSummary`의 짧은 실제 기계 효과 문구를 사용한다. 범위 피해, 상태 효과, 지형 파괴, 지속시간과 같은 실제 플레이 결과를 우선 표시한다.
- `Wish`는 체력·마력과 주문 회로를 복구하되 기존 이로운 버프를 지우지 않는다. 독/위더/둔화/약화/실명/혼란/채굴 피로/부양/암흑/허기와 화상·동결 같은 해로운 상태만 정리하고 재생·흡수를 추가한다. `MagicPlayerData.beginCast`의 mana/full-cooldown reset 계약을 그대로 사용한다.
- `Antimagic Field`는 12초 동안 시전자를 따라가는 서버 권위 필드다. 범위 안의 spell-like 상태효과를 지속 억제하고 플레이어/NPC의 Arcane charge, fusion, pending impact를 차단한다. 시전자 자신도 필드 안에서는 새 주문을 전개할 수 없다.
- `Time Stop`은 6초 동안 release 지점에 고정되는 서버 권위 시간장이다. 시전자를 제외한 비아군 mob은 기존 `noAI` 상태를 보존한 채 AI와 이동이 실제 정지하며, 비아군 플레이어는 이동·Arcane 시전이 봉쇄된다. 필드가 끝나거나 시전자가 로그아웃/사망/차원 이동하면 원래 mob AI 상태를 복원한다.
- `NpcSpellResolver`와 `NpcMeteorBarrageService`도 `ArcaneFieldService.blocksCasting`을 통과해야 하므로 vanilla no-AI를 우회하는 커스텀 마도사 주문 경로가 시간정지/반마법장을 뚫지 못한다.
- Alpha.34의 초기 World Sunder 7-point fissure와 420 changes / 24,000 scans 예산은 역사적 계약이다. 현재 파괴 형상과 shared budget의 정본은 아래 Alpha.38 계약(13-point fissure, 720 changes / 48,000 scans / 96 drop breaks)이다.
- 대량 지형 주문은 no-drop을 유지하며 `shatter`만 bounded drop identity를 유지한다.
- Terrain class는 **MAJOR** `disintegrate`, `delayed_blast_fireball`, `fire_storm`, `earthquake`, `meteor_swarm`, `world_sunder`, `arcane_annihilation`; **CONDITIONAL** `fireball`, `shatter`, `flame_strike`, `meteor_shard`, `move_earth`, `lightning_bolt`, `thunderwave`, `gust_of_wind`; 나머지는 기본적으로 **NONE**이다.
- Grimoire header/content/footer ownership, fine-line sigil grammar, prismatic primitive budget, temporary Light cleanup, robe atomic equipment, ready-hold release 계약은 alpha.33과 동일하게 유지한다.
- Canonical Java 25 clean build + source audit + JAR verify가 source commit 이후 반드시 성공해야 하며 source-only PASS로 배포하지 않는다.

## Alpha.37 presentation + buff identity contracts

- Prismatic Wall gameplay lifetime is 14 seconds (280 ticks). The alpha.36 30-second increase was reverted: the original problem was full-life visual fading, not server duration. Seven panels remain fully readable for the first 90% of life and only dissolve in the final 10%.
- 6C+ presentation complexity is structural, not radius-only. 7C adds a second ritual plane, 8C adds a three-dimensional gyroscope, and 9C adds nine satellite formulae plus displaced authority bands. Compact spells such as Power Word Kill may stay physically small while still reading as 9C.
- Portal/prison/high-circle release geometry may occupy multiple planes and anchors, but Gate's caster-side aperture still rises from the floor in front of the caster and must never center a giant frame on the caster body.
- Self buffs use `ArcaneBuffRuntime` for their defining mechanics. Vanilla effects are supplemental feedback only. Haste changes Arcane cast/cooldown timing; Shield/Mage Armor/Energy Protection/Solar Guard own charge mechanics; Invisibility/Greater Invisibility/Foresight own miss windows; Freedom and True Seeing are continuously enforced; Stoneskin/Shapechange own incoming-damage shaping.
- Persistent buff visuals are spell-authored silhouettes rather than one shared body halo: armor plates, mirror satellites, phase arcs, haste clock/helix, energy diamonds, stone facets, freedom spiral, true-sight eye, fire/solar crowns, shapechange morph rings and foresight eye-clock.
- Maintained visual lifetime follows the authoritative mechanic for Feather Fall, Mirror Image, Blur, Fly, Resilient Sphere, Globe of Invulnerability, Fire Shield, Simulacrum and Clone. Long-lived buffs keep compact state-signatures instead of holding their full cast circle on screen for the whole duration.
- Alpha.37 source audit orders runtime inputs before parity assertions and explicitly guards maintained-buff lifetime plus recharge timing, so CI fails before compilation if those contracts drift.

## Alpha.38 catastrophic-impact + cinematic-detail contracts

- Catastrophe-class terrain magic is no longer capped by one universal 10.5-block sphere. Every destructive profile owns a maximum radius, depth ratio, energy and block budget while still sharing loaded-chunk, block-entity, fluid, hardness/resistance and per-level tick protections.
- The shared no-drop destruction ceiling is 720 changed blocks / 48,000 scanned cells per level tick; drop-producing Shatter remains separately capped at 96. Large spells gain footprint by authored multi-focus shapes and staggered impacts, not an unbounded synchronous world edit.
- Meteor Swarm uses 16 independently seeded deep crater events with fractured rims. Fire Storm's six columns can devastate six real impact zones. Earthquake uses a dense epicenter plus eight irregular secondary foci. World Sunder uses a long/deep thirteen-point meandering cut. Arcane Annihilation bores a thick deletion corridor from safely in front of the caster to the locked endpoint.
- Catastrophe visuals use anticipation -> convergence/collapse -> primary impact -> secondary shock/fracture. Meteor Swarm keeps a sky authority crown alive across the barrage; Fire Storm has a contracting sky lattice and six synchronized columns; Delayed Blast Fireball visibly compresses before detonation; Arcane Annihilation owns a multi-gate beam body; World Sunder/Earthquake draw multiple fault bands and lifted debris accents.
- Catastrophe charge formulae add a dedicated authority layer on top of the 6C+ hierarchy: broken outer seals, converging anchors, a cross-plane lock ring and final vertical authority pylons. This is restricted to the genuinely destructive spells so high-circle support/control magic keeps a different silhouette.

## Alpha.39 grand-sigil + persistent-status presentation contracts

- Compact/low-circle formulae are intentionally left readable and light. Large 6C+ formulae add nested polygon locks, tessellated sector webs, chord lattices and satellite sub-formulae. Radius growth must increase information density, not merely enlarge a circle.
- 7C, 8C, 9C must gain structural depth rather than only radius. These historical hierarchy rules are superseded by Alpha.40's stricter spell-ID authored registry for final composition selection.
- Long-lived buffs and debuffs keep a spell-authored state silhouette for the authoritative mechanic lifetime. High-circle status magic additionally carries compact persistent authority instead of holding the enormous cast circle on screen.
- High-circle hard controls/prisons keep floor/top seals, cross-plane restraints and high-circle rune satellites while active. `sleep` and `mass_suggestion` visual lifetimes follow their real 140/160 tick control windows.
- Time Stop, Wish and Power Word Kill have dedicated dense 9C release geometry instead of relying on a generic afterimage.

## Alpha.40 manual-only presentation contract

- All 109 direct/fusion spells are explicitly dispatched by spell ID through `ManualSpellVisuals` and the circle/fusion authored files.
- `ArcaneSigilDirector`, `SpellCinematicDirector`, and `ArcaneSpellVisualOverhaul` are retired from source and JAR. They must not be reintroduced as fallback presentation generators.
- `spell.school()`, presentation form/motion, or circle rank may not select a final visual composition. Shared code is limited to geometry primitives and runtime transport/budget logic.
- Adding a spell without a manual authored case is a source-audit failure rather than a generic visual fallback.

## Alpha.41 hand-authored visual audit

- High-circle presentation is reviewed spell-by-spell; no rank/school/form decorator may substitute for authored composition.
- Antimagic Field uses broken null circuits, Maze uses broken square corridors, and Time Stop freezes its own clock geometry after release.
- Plane Shift, Teleport, Demiplane and Gate deliberately use different spatial-depth grammars.
- Power Word Kill uses compact execution closure; Wish uses a three-tier nine-contract reality lattice; Foresight keeps visible branching future trajectories.
- Shapechange uses self combat-form anatomy accents while True Polymorph uses a vertically stitched target rewrite lattice, so the two transformation spells no longer share the same silhouette.
- Prismatic Wall panels own upper/lower sub-seals and crossed internal lattice while retaining the authoritative 14-second lifetime.


## Alpha.42 presentation quality rollback
- Alpha.40/41 manual-only visuals are retired because they removed the readable base sigil and degraded the full presentation into sparse wireframe geometry.
- Alpha.39 layered presentation is the visual baseline again: `ArcaneSigilDirector` supplies the readable formula frame, `SpellCinematicDirector` supplies the physical event, and `ArcaneSpellVisualOverhaul` supplies spell-specific/high-circle/status refinements.
- Circle 1 must retain a visible compact magic-circle frame; manual refinement must never delete a working lower-circle sigil merely to satisfy a hand-authored architecture rule.
- Future hand refinement is additive/replacement per spell only after visual comparison; it may not globally replace the proven presentation stack.
