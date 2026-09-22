# Promotion Skill Visual Redesign

Status: implementation contract for Village Guardians promotion skills.

## Goals

- Every promotion skill must be recognizable by silhouette before color.
- First-promotion skills establish the role fantasy; second-promotion skills escalate into large signature forms.
- Cast, projectile/field, and impact should not all reuse the same shape.
- Visible radius should track gameplay radius closely for fields and impacts.
- Avoid particle-only presentation and vanilla item/block display stand-ins.
- Repeated primitives such as rings, runes, shields, arrows, and pillars are support geometry only. They may not be the only readable shape.
- Stronger second-promotion skills receive larger spatial composition and stronger impact staging without hiding enemy telegraphs.

## Role visual grammar

| Role | Core grammar | Avoid |
| --- | --- | --- |
| Vanguard | blades, wedges, standards, rupture lines, forward momentum | every skill becoming a red ring/slash |
| Ranger | bow limbs, arrow formations, reticles, constellations, sky trajectories | generic magic circles with arrows pasted on |
| Arcanist | elemental volumes, cages, chain paths, vortexes, celestial cores | same rune disc recolored |
| Luminar | halos, wings, crosses, sanctuary geometry, ascension pillars | ring + pillar + wing repeated unchanged |
| Warden | gates, walls, bastions, shield formations, fortress mass | curvedShield + ring on every skill |

## 40-skill identity contract

| Skill | Primary silhouette | Timing / impact read |
| --- | --- | --- |
| 전선 절개 | three diverging sword lanes | short cast -> three forward cuts -> terminal cross-slice |
| 피의 회오리 | rotating hooked blade spiral | outward cut -> inward siphon tendrils |
| 전투 깃발 | actual pole + cloth standard | standard plants first; aura expands afterward |
| 파성 일격 | battering wedge + ground split | narrow forward tell -> heavy wedge impact |
| 검성 연환 | five-blade fan lattice | sequential five blades, not one circular slash |
| 생명 절취 | crossed crimson scythes + return strands | hit burst -> strands pull back to caster |
| 절대 돌파 | armored piercing wedge | compact charge nose -> long corridor trail -> break burst |
| 천단참 | suspended giant vertical blade | delayed sky blade -> ground fracture on strike |
| 매의 징표 | hawk crest enclosing reticle | crest closes around selected target |
| 분열 사격 | visible bow arc + five-arrow fan | bow flex -> five diverging projectiles |
| 대공 요격 | upward arrow crown | sky-pointing launch crown -> aerial intercept flashes |
| 폭우 사격 | overhead arrow canopy | ceiling canopy -> repeated vertical rain lanes |
| 별추적 화살 | orbiting star-arrow nodes | stars acquire targets -> arrows peel away |
| 관통 성단 | long stellar lance with constellation spine | charge line -> long piercing beam/arrow |
| 천공 봉쇄 | vertical sky cage | pillars form airspace cage instead of flat ring |
| 유성 대궁 | oversized bow + meteor spear | bow draw -> meteor head flight -> crater burst |
| 용암핵 | cracked molten core | rotating crust -> hot core projectile -> expanding fracture |
| 빙결 감옥 | vertical ice cage | bars rise around real radius and lock inward |
| 낙뢰 사슬 | linked lightning nodes | visible chain path between distinct nodes |
| 중력 폭풍 | tilted vortex funnel | forward-moving spiral with inward debris lanes |
| 태양핵 폭발 | sun disc + corona | expanding corona -> core detonation |
| 절대영도 | snowflake sigil + ice bloom | six-axis freeze mark -> outward crystal bloom |
| 천뢰 연쇄 | thunder lattice | multiple vertical branches connected by a sky lattice |
| 붕괴 특이점 | dark core + accretion disc | stable center; inward spiral; compressed pulse |
| 수호의 빛 | guardian halo + paired wings | halo descends onto protected ally |
| 성역 정화 | expanding cross-wave | cross sigil flashes -> clean wave expands |
| 회생 파동 | layered life wave | staggered concentric rises, not a static circle |
| 심판광 | descending judgement blade/cross | narrow pillar -> hard radial verdict burst |
| 천상의 방벽 | dome-like sanctuary ribs | barrier ribs close overhead around allies |
| 회귀의 빛 | beacon spear + return halo | light locks onto ally then collapses inward |
| 부활 성가 | choir halo crown + ascension pillars | five voices/pillars rise in sequence |
| 최후의 기적 | large mandala + cross + wing crown | multi-stage sanctuary -> verdict burst |
| 성문 충격 | gate frame + shield ram | gate silhouette slams forward then fractures ground |
| 강제 도전 | taunt crown + inward chevrons | arrows/chevrons point toward Warden |
| 수호 결계 | four wall plates | wall panels rise around party space |
| 철벽 파동 | expanding shield plates | plates kick outward as the shockwave expands |
| 불락 방벽 | three-section fortress wall | center keep plus side walls, not a curved bubble |
| 성채 돌진 | fortress ram prow | large wedge prow with long wall-like wake |
| 절대 방진 | radial phalanx | individual shields face outward in formation |
| 성채 강림 | descending keep/tower | fortress mass descends -> ground shock + wall ribs |

## Tier escalation

Second-promotion skills must visibly exceed first-promotion skills through composition, not just scale:

- larger but bounded gameplay-aligned footprint;
- at least one signature silhouette unique within the role;
- clear anticipation and impact states where gameplay timing allows;
- heavier impact geometry and audio pairing;
- no dependency on particles for shape.

## Acceptance

Static tests can prove identity branches exist and reject primitive-only regressions, but they cannot prove visual quality. Final acceptance requires in-client screenshots or playtest footage for all forty promotion skills, with special attention to 20 second-promotion skills.
