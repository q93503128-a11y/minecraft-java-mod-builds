# Arcane Circle: Ninefold Arcana — Project Contract

- Mod ID / namespace: `arcanecircle`
- Version source: `gradle.properties -> mod_version`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / Gradle 9.2.1
- Direct spells 90 / Fusion spells 19 / Circles 1~9
- Canonical CI: `.github/workflows/build-arcane-circle.yml`
- Source audit: `tools/test_current_source.py`
- JAR audit: `tools/verify_jar.py`

게임 데이터·마력·숙련·시전·네트워크·판정은 서버 권위다. 0초 시전도 ready-hold 후 release 발동을 유지한다. 현재 presentation 정본은 `GrimoireScreen`, `ArcaneHud`, `ArcaneSigilDirector`, `SpellCinematicDirector`, `ArcaneRegaliaRenderer`, `ArcaneCastingPerformance`이며 구형 presentation 클래스와 버전별 migration/apply/fix 도구는 active tree에 두지 않는다.

alpha.28부터 모든 주문은 `ArcaneSigilDirector`의 주문별 술식 마법진과 `SpellCinematicDirector`의 물리 현상을 연속된 한 연출로 사용한다. 지팡이 시전시간 배율은 직접 주문과 융합 주문의 시전 계산 및 하한에 실제로 참여한다.

## Alpha.33 runtime contracts

- `CastTargetSnapshot` is captured exactly once when a player releases a cast. Absolute target position, launch origin/direction, optional target entity UUID, impact surface, dimension and barrage seed travel together through the authoritative pending-impact path.
- Non-homing spells never resample a later player look direction. Homing is explicit opt-in only; the current canonical homing set is empty until client/server moving-target presentation is authored end-to-end.
- Pending casts are invalidated on death, spectator state or dimension mismatch. Respawn and dimension-change handlers also clear kinetic queues immediately.
- `WorldMagicService` release payload and `SpellKineticsService` gameplay consume the same snapshot. Client visuals parse the same `seed` and render Meteor Swarm under the same seeded pattern context.
- Meteor Swarm keeps 16 authored anchor strikes but applies per-cast bounded rotation/jitter, timing variance, fall-height variance and scale variance. Minimum strike separation is enforced and the last four impacts gain modest rhythmic weight.
- Meteor charge and release reuse one server-generated barrage seed, so coordinate seals do not reshuffle between charge and release.
- NPC Meteor Swarm no longer collapses into one generic sky-drop hit. `NpcMeteorBarrageService` schedules the same 16 seeded strike grammar over time, and ordinary delayed NPC projectiles consume their release-time locked target instead of a moved target's later position.
- NPC barrage damage is staggered, but NPC terrain griefing remains disabled intentionally; high-circle NPCs do not silently excavate player builds until a separate Arcane destruction rule is exposed.
- `World Sunder` is a target-ground battlefield rupture. Damage, knockback and terrain destruction are centered on the release snapshot target rather than the caster's feet.
- Destructive terrain mutation remains server-authoritative and strength-aware (`getDestroySpeed` + explosion resistance). Unbreakable blocks, block entities, fluids and unloaded chunks are never removed.
- Destruction now has a shared per-level tick budget: at most 420 changed blocks and 24,000 scanned in-range block cells per server tick across simultaneous destructive casts.
- Mass terrain spells `move_earth`, `earthquake` and `world_sunder` are no-drop destruction to prevent hundreds of item entities. `shatter` retains bounded drops because its physical identity is material breakage and its per-impact cap remains small.
- Terrain classification is exhaustive by default:
  - **A / MAJOR**: `disintegrate`, `delayed_blast_fireball`, `fire_storm`, `earthquake`, `meteor_swarm`, `world_sunder`, `arcane_annihilation`.
  - **B / CONDITIONAL**: `fireball`, `shatter`, `flame_strike`, `meteor_shard`, `move_earth`, `lightning_bolt`, `thunderwave`, `gust_of_wind`.
  - **C / NONE**: every other direct or fusion spell unless explicitly promoted later. Mental, time, space, ward and utility magic do not erase terrain merely because of circle rank. Cold magic also stays out of destructive block removal; freezing/icing is a separate environmental transformation concern, not generic excavation.
- Lightning/sonic/wind B-class spells use weak no-drop physical aftermath profiles; only sufficiently fragile blocks inside their actual launch path can fail.
- Grimoire layout still assigns header/content/footer ownership; circle rail, detail reader and loadout dock may never overlap even at high GUI scale.
- Sigil radius reacts to final range by spell geometry family; it is not a raw 1:1 range circle.
- Light uses temporary vanilla Light blocks and must clean them on expiry/session/dimension/server shutdown.
- Prismatic rendering remains bounded by per-entry and per-frame primitive caps.
- High-complexity sigils prioritize fine concentric rules, inscription rings, nested geometry and balanced satellite seals; 3D depth stays secondary and may not overpower the readable planar formula.
- Canonical Java 25 verification is required after the alpha.33 source commit; source-only audit is not sufficient.
