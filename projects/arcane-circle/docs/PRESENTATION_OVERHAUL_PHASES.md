# Arcane Circle Presentation Overhaul

## Goal

Arcane Circle must not communicate spell rank by simply scaling one shared magic-circle template. A spell's fiction, delivery method, target relationship, and combat role determine its silhouette, placement, motion, impact, and aftermath. Circle rank primarily controls how sophisticated a formula may become.

The presentation pipeline is treated as one sequence:

`input -> caster preparation -> mana assembly -> sigil construction -> ready hold -> release -> travel/propagation -> impact -> aftermath/residue`

The visual event and server hit timing must continue to agree.

## Reference principles

The overhaul is informed by publicly viewable Minecraft magic work such as Mahou Tsukai, Iron's Spells 'n Spellbooks, Ars Nouveau, Spell Engine/Wizards, Mana and Artifice, Hex Casting and other spell showcases. External assets are not copied into this project. The useful principles are:

- the magic circle should read as an actual spell device rather than decoration;
- spells in the same school still require different silhouettes and delivery language;
- projectile launch position must visibly connect to its casting device;
- projectile speed, acceleration, trajectory and impact language should follow spell mass and intent;
- rituals, portals, prisons, weather and world-scale magic should use the world itself as staging space;
- high rank does not automatically mean physically large;
- caster pose/motion and robes should eventually reinforce the weight and school of the spell.

## Phase 1 — 1C-3C quality baseline (alpha.21)

Status: implemented and protected as the regression baseline.

Every normal spell and fusion result in circles 1-3 has an authored `SpellPresentationProfile` and is routed through `LowCircleVisualIdentity` instead of the generic school/fingerprint fallback. The baseline covers 37 formulae.

Representative presentation identities include:

- Magic Missile: three-node missile rack and staggered guided bolts.
- Fire Bolt: triangular ignition aperture and fast compact flame dart.
- Ray of Frost: crystal aperture and narrow frost ray.
- Shield / Mage Armor: body-centred defensive lattices instead of attack circles.
- Feather Fall / Fly / Levitate: feet/body mobility runes with vertical or wing language.
- Sleep: dream spiral field.
- Thunderwave: square pressure gate and expanding shock rings.
- Web: radial web lattice.
- Mirror Image: three separated mirror plates.
- Misty Step / Blink: paired depth gates rather than generic circles.
- Fireball: compressed reactor core, heavy orb travel and expanding impact body.
- Lightning Bolt: aligned rail gates and jagged full-path discharge.
- Haste / Slow: deliberately different clock languages.
- Chromatic Orb: seven-node elemental crown.
- Wind Wall: spatial wall matrix rather than a projectile.
- Counterspell / Dispel Magic: cancellation/breaking geometry rather than damage motifs.

Alpha.20 held-cast behaviour, atomic robe behaviour and responsive Grimoire UI are regression requirements for every later phase.

## Phase 2 — 4C-6C battlefield manipulation

Phase 2 is deliberately split by circle so each formula receives an authored sequence rather than a scaled copy of Phase 1.

### Phase 2A — 4C authored battlefield presentation (alpha.22)

Status: implemented.

All ten normal 4C spells and all three 4C fusion results are routed through `MidCircleVisualIdentity` before the generic school/fingerprint renderer. The director currently owns circle 4 only; 5C and 6C remain outside it until their dedicated passes.

Authored 4C identities:

- Wall of Fire: target-space foundation anchors assemble left-to-right, pylons rise, then segmented flame panels ignite into a persistent wall silhouette.
- Wall of Ice: target-space crystalline buttresses rise first, followed by alternating jagged ice plates and fractured residue. It does not reuse the fire-wall body.
- Ice Storm: a ground footprint and a separate high canopy lock together; independent hail cells descend through the full vertical volume and leave a spreading frost fracture.
- Greater Invisibility: incomplete contour shutters and body slices erase themselves progressively instead of using an attack sigil.
- Resilient Sphere: orthogonal and tilted ribs close around the caster into a defensive shell, followed by a sealed pulse.
- Dimension Door: a near aperture and target aperture are built separately and connected by visible depth rails and corridor slices. Release sends a spatial pulse through the passage.
- Stoneskin: body-height polygon plates assemble from the feet upward and settle as a layered stone shell.
- Confusion: mismatched compass/star frames occupy different heights and tilted planes around the target, deliberately refusing one stable axis.
- Blight: root/vein branches grow inward around the target and contract into a withering heart before leaving a drained residue ring.
- Freedom of Movement: shackle-like rings open into widening gaps while vertical escape rails climb through the body space.
- Phantasmal Killer: an asymmetric target-space mask with mismatched eyes, temples and jaw closes on the victim and fractures after release.
- Fire Shield: a body-centred defensive bastion closes from segmented plates and ember crests rather than reusing an offensive fire circle.
- Thunder Cage: four target-space pylons install first, then horizontal restraint rails and diagonal lightning arcs complete the prison.

Phase 2A intentionally does not claim that every 4C effect is physically larger than 3C. Scale follows the spell: battlefield walls and storms occupy large world space, while body wards and target curses remain compact but structurally more sophisticated.

### Phase 2B — 5C (alpha.23)

Status: implemented. All ten normal 5C spells and all three 5C fusion formulae now route through `FifthCircleVisualIdentity` before the generic renderer.

Alpha.23 also fixes the range-stat presentation/gameplay split. `Steam Burst` no longer has the legacy 11-block hit cap; wave length/end radius, low-circle fields and wall width now use shared `SpellMetrics`, and authored release geometry receives the effective post-equipment range. Casting-device size itself remains spell-authored rather than scaling blindly with range.

Priorities include Wall of Force, Cloudkill, Hold Monster, Passwall, Insect Plague, Telekinesis, Cone of Cold, Flame Strike, Dominate Person, Mass Cure Wounds, Chain Lightning, Arcane Hand and Teleportation Circle. They require new wall, toxic-volume, target restraint, true passage, swarm-volume, force-manipulation, cone, sky-drop, domination, mass-life, branching-beam and spatial-circle languages rather than 4C reskins.

### Phase 2C — 6C (alpha.24)\n\nStatus: implemented. All ten normal 6C spells plus Solar Guard use `SixthCircleVisualIdentity`, with beam, transformation, ward, target and battlefield footprints authored separately.

Priorities include Disintegrate, Sunbeam, Freezing Sphere, Globe of Invulnerability, Flesh to Stone, Eyebite, Move Earth, Mass Suggestion, True Seeing, Circle of Death and Solar Guard. Beam/lance/heavy-orb kinetics and defense/transformation silhouettes must be authored separately.

4C-6C as a whole should feel like the point where a mage starts manipulating the surrounding space, not only emitting an attack from the hands.

## Phase 3 — 7C-9C (alpha.24)\n\nStatus: implemented. Every normal and fusion formula from 7C through 9C routes through `ArchmageVisualIdentity`; none of these formulas depend on the generic fallback for their primary presentation.

Each major late spell should be reviewed as a bespoke sequence. Priority examples:

- Meteor Swarm: command sigil at caster, enormous sky ritual over target, visible meteor emergence, acceleration, ground warning, impact and sky-circle collapse.
- Power Word Kill: compact high-authority execution seal; scale should come from finality, not radius.
- Time Stop: temporal lock/clock language and environment-scale suspension.
- Gate / Demiplane: real spatial depth and destination framing.
- Control Weather / Fire Storm: sky and region become part of the effect.
- Wish: reality-rewrite ritual with restrained but unmistakable authority.
- Prismatic Wall/Spray: layered colour must be structural, not only a tint.
- World Sunder / Earthquake: ground fracture should visibly propagate through terrain space.

## Phase 4 — caster motion and clothing presentation (alpha.24)\n\nStatus: implemented as a client-only custom silhouette layer without compromising input timing or server authority. `WorldMagicTracker` exposes charge/release presentation state to the gear renderer; seven casting families reposition mod-owned sleeves and casting cloth while each robe family gains a distinct overlay silhouette.

Desired casting families include compact snap casts, aimed lance/beam stance, heavy two-hand release, ground invocation, ward brace, portal split, ritual channel and recoil/recovery. Do not make every spell use one arm swing.

Robe sets should also be separated by silhouette, not only colour: cinder combat coat, crystalline long robe, lightweight tempest cloth, asymmetric rift vestments, and a distinct archmage ceremonial silhouette. Equipment remains one logical robe item even if renderer pieces are split internally.

## Validation rules

- No build-time Python source rewriting in the final canonical CI.
- Java 25 clean build is required.
- JAR structure and version are audited.
- Phase 1's 37 authored identities must continue to route through `LowCircleVisualIdentity` before all later directors.
- Phase 2A's 13 circle-4 identities must route through `MidCircleVisualIdentity` before the generic renderer.
- Visible projectile/meteor impact and server damage timing must remain synchronized.
- Zero cast time still means completed sigil held until key release; reaching 100% never auto-fires.
- UI content must remain reachable at small logical resolutions / large GUI scale.
- The robe remains one logical outfit even if rendering uses multiple pieces.
- Save compatibility must be preserved unless explicitly documented otherwise.


## Alpha.24 integrated completion audit

- 6C: 11/11 normal+fusion formulas authored.
- Remaining high-circle authored total: 46/46 formulas.
- 7C: 12/12 normal+fusion formulas authored.
- 8C: 12/12 normal+fusion formulas authored.
- 9C: 11/11 normal+fusion formulas authored.
- Meteor Swarm uses four separately staged sky bodies and four 11-block impact envelopes at the same authoritative delay.
- Power Word Kill stays deliberately compact and command-like instead of inheriting Meteor scale.
- Earthquake, reverse gravity, antimagic, weather, incendiary cloud, time stop, Weird, prismatic wall and the high fusion domains mirror their server effect footprints.
- Remaining fusion range ceilings that prevented range equipment from continuing to scale Void Lance, Winter Domain, Phoenix Requiem, World Sunder and sight-targeted fusion spells were removed.
- Casting motion is visual-only and cannot alter cooldown, mana, target selection or authoritative hit timing.
- Robe identity remains one logical equipment item; the renderer only adds style-specific panels/tails/mantles.
