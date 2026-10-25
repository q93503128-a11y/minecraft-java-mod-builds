# Solo Leveling-style Minecraft RPG

Status: Planning

This project adapts the core progression fantasy, System UI, gate/dungeon loop, skill growth, and shadow-summoning structure of Solo Leveling into Minecraft Java while preserving Minecraft survival play where it does not conflict with the intended RPG experience.

## Current direction

- Minecraft Java mod project.
- Visual language should stay as close as practical to Solo Leveling's webtoon System presentation and overall atmosphere.
- Primary design reference: Solo Leveling webtoon.
- Animation/motion references may use the anime when static webtoon panels are insufficient.
- Game adaptations should preserve the original experience rather than copy mechanics that do not fit Minecraft.
- Social/hunter-association simulation is intentionally omitted; rank is shown directly by the System.
- Hunter/player rank uses E, D, C, B, A, S.
- Gates naturally occur in the Overworld.
- Special gates occur probabilistically.
- A craftable gate detector is planned.
- Nether and End are not required for core progression.
- Vanilla survival remains as the ordinary-world layer; the System and gates provide the RPG layer.
- Vanilla inventory remains the real player inventory; its UI may be redesigned in the System visual language, and capacity expansion may be added if needed.
- Vanilla heart HUD is planned to be replaced by a System-style HP bar. MP and fatigue are separate System resources.
- Minecraft death is not permadeath; content-specific failure rules will be used for gates, System quests, and the Penalty Zone.
- The Penalty Zone must never soft-lock a player. Multiplayer Penalty Zones are intended to use separate player instances of the same rules/environment.
- Daily quests should use a separate System-time cadence rather than Minecraft's 20-minute day so they do not become constant chores.
- Enemy name color follows the source's relative-danger presentation: white / orange / red depending on the enemy's strength relative to the player.
- Skills can level and, where appropriate, evolve into stronger forms.
- Shadow Extraction, Shadow Storage, Shadow Exchange, and Monarch's Domain are treated as growing job skills, not static menu features.
- Shadow army limits should stay close to the source's progression fantasy rather than small party limits from mobile adaptations. Technical entity limits must be solved without turning the fantasy into a 3-5 summon party system.
- Named shadows should not become permanently unobtainable because of bad extraction RNG. Repeatable named encounters are allowed. Igris is planned to reproduce the three-attempt scene and guarantee extraction on the third attempt in the job-change sequence.
- Mana crystals exist as dungeon resources and are candidates for crafting, upgrades, potions, and gate-detector progression. Essence stones are candidates for rarer/higher-value recipes.
- Multi-weapon combat and late-game telekinetic weapon control via the Ruler's Hand / Ruler's Authority progression are intended if implementation quality is sufficient.
- Named dungeons/bosses may recur because this is a replayable game rather than a one-time narrative world.
- Original Solo Leveling monsters are the first source for dungeon rosters, followed by compatible Ragnarok additions, Minecraft dungeon variants, then mythic/classic fantasy creatures where the roster needs expansion.

## Prologue

If feasible at sufficient quality, the first Double Dungeon should be implemented as a short playable/cinematic prologue that leads to System acquisition. It should not rely on normal Minecraft death for its story outcome. If a full prologue cannot meet the quality bar, a shorter directed opening is preferable to a low-quality recreation.

The later return to the Double Dungeon remains important because the contrast between the helpless first visit and the powerful return is part of the intended progression fantasy.

## Gate / Dungeon direction

- Natural gates exist in the Overworld.
- Gate rank is E-S.
- Special gates can replace/transform normal expectations with a low probability.
- Dungeon Break is retained, but undiscovered distant gates must not create unavoidable world-wide maintenance. Activation/proximity/loading rules should prevent every natural gate from eventually breaking while nobody could reasonably respond.
- Gate and System-dungeon implementations should share the same underlying dungeon infrastructure where practical; the important distinction is access rules and narrative/system context, not separate engines.

## Multiplayer principles

- Player progression state is per-player and server authoritative.
- World gates and world-side dungeon-break state are shared.
- Ordinary gates can be entered cooperatively.
- Personal System tests such as the Penalty Zone and job-change trials should remain individually instanced unless a later design explicitly requires otherwise.
- Loot and named-shadow acquisition must avoid multiplayer griefing. Per-player reward/extraction state is preferred to winner-takes-all ownership.

## Open design questions

Work through these slowly; do not lock them all at once.

1. HP / MP / fatigue interaction with vanilla hunger and regeneration.
2. Exact inventory expansion rules and whether System-only storage is needed at all.
3. System store vs Minecraft crafting and trading.
4. Vanilla gear vs System gear and equipment slots.
5. Player EXP vs vanilla experience.
6. Exact E-S rank thresholds and whether rank is level-derived or a broader combat rating.
7. Gate spawn frequency, spacing, persistence, and activation.
8. Daily quest cadence and compressed exercise/run implementation.
9. Skill acquisition, proficiency, level-up, and evolution rules.
10. Shadow extraction probability, pity/retry rules, storage, resurrection/recovery, rank growth, and command behavior.
11. Dungeon-break timing and world safety constraints.
12. Multiplayer ownership, rewards, party EXP, and duplicate named shadows.
13. Mana-crystal / essence-stone economy and crafting sinks.
14. Dungeon roster coverage across E-S ranks.

## Design rule

Do not add systems merely because another Solo Leveling game has them. ARISE and similar adaptations may be useful references, but the webtoon is the primary design source. When Minecraft cannot support a source mechanic literally, preserve the source mechanic's role and player experience instead of forcing a direct copy.
