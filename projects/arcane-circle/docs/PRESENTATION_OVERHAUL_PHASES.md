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

Status: implemented.

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

## Phase 2 — 4C-6C

Next target.

Do not merely port the low-circle director to more spell IDs. Establish distinct mid-tier battlefield languages for:

- persistent walls and zones;
- large elemental storms;
- strong single-target control;
- advanced mobility/space folding;
- sustained beams and heavy projectiles;
- defensive domes and layered wards;
- transformation/debuff magic.

4C-6C should feel like the point where a mage starts manipulating the surrounding space, not only emitting an attack from the hands.

## Phase 3 — 7C-9C

After Phase 2 is visually stable.

Each major late spell should be reviewed as a bespoke sequence. Priority examples:

- Meteor Swarm: command sigil at caster, enormous sky ritual over target, visible meteor emergence, acceleration, ground warning, impact and sky-circle collapse.
- Power Word Kill: compact high-authority execution seal; scale should come from finality, not radius.
- Time Stop: temporal lock/clock language and environment-scale suspension.
- Gate / Demiplane: real spatial depth and destination framing.
- Control Weather / Fire Storm: sky and region become part of the effect.
- Wish: reality-rewrite ritual with restrained but unmistakable authority.
- Prismatic Wall/Spray: layered colour must be structural, not only a tint.
- World Sunder / Earthquake: ground fracture should visibly propagate through terrain space.

## Phase 4 — caster motion and clothing presentation

Once spell visuals are stable, add an animation layer without compromising input timing or server authority.

Desired casting families include compact snap casts, aimed lance/beam stance, heavy two-hand release, ground invocation, ward brace, portal split, ritual channel and recoil/recovery. Do not make every spell use one arm swing.

Robe sets should also be separated by silhouette, not only colour: cinder combat coat, crystalline long robe, lightweight tempest cloth, asymmetric rift vestments, and a distinct archmage ceremonial silhouette. Equipment remains one logical robe item even if renderer pieces are split internally.

## Validation rules

- No build-time Python source rewriting in the final canonical CI.
- Java 25 clean build is required.
- JAR structure and version are audited.
- Visible projectile/meteor impact and server damage timing must remain synchronized.
- Zero cast time still means completed sigil held until key release; reaching 100% never auto-fires.
- UI content must remain reachable at small logical resolutions / large GUI scale.
- Save compatibility must be preserved unless explicitly documented otherwise.
