# M5 Visual Reference + Mockup Contract

Status: ACTIVE WORKING CONTRACT  
Date: 2026-09-08

This document is the required reference-analysis and mockup step before further M5 visual implementation. It does not mark any screen production-ready. Real Minecraft screenshots are still required.

## 1. Reference set

### Persona 5 — battle command readability
Reference observed: Persona 5 battle command UI / menu screenshots.  
Useful property: the active decision is visually unmistakable because selection changes shape, contrast, and local composition instead of relying on a tiny cursor or color alone.

Use in TURNBOUND: RE:
- current actor and the immediate decision receive the strongest semantic emphasis;
- inactive information remains readable but visually subordinate;
- combat UI must stay around the world view rather than becoming a center-screen spreadsheet.

Do not copy:
- Persona-specific typography, red/black identity, iconography, layout silhouettes, or branded motion language.

Source references:
- https://virpivainola.com/wp-content/uploads/2020/01/virpi-vainola-how-user-interface-design-affects-the-success-of-contemporary-video-games.pdf

### Honkai: Star Rail — party presentation
Reference observed: party setup screenshots.  
Useful property: the characters are the visual subject; UI chrome supports comparison instead of covering the cast with dense cards.

Use in TURNBOUND: RE:
- selected character detail remains persistent;
- active party slots must be scannable as a group;
- model/entity preview gets protected visual space;
- ownership, level/star, role and squad cost should be layered rather than written as one long label.

Do not copy:
- character art, starfield treatment, icons, fonts, or exact screen structure.

Reference page:
- https://news.codashop.com/ph/honkai-star-rail-team-guide/

### Granblue Fantasy: Relink — battle result hierarchy
Reference observed: official system pages, result screenshots, and 2026 UI update notes.  
Useful property: result screens establish outcome first, then performance/reward information, while party information stays grouped and readable.

Use in TURNBOUND: RE:
- outcome appears first;
- rewards reveal as a short sequence instead of all text appearing at once;
- Continue cannot erase the result before the reveal is readable;
- no full-screen opaque result menu; the resolved battle world remains context.

Do not copy:
- medals, rank graphics, proprietary art, textures, or exact reward layout.

Official references:
- https://asia.sega.com/relink.granbluefantasy/kr/system/
- https://relink-ragnarok.granbluefantasy.com/ko/updates/381/

## 2. M5 shared visual semantics

The current implementation uses Minecraft built-in GUI sprites as a legal/native bridge until a final authored or clearly licensed atlas is selected. This is not permission to leave the UI as generic vanilla chrome forever.

Semantic priorities:
1. FOCUS — current turn, selected character, selected party slot, current decision.
2. WARNING — enemy intent, invalid capacity/cost, rejected server response.
3. SUCCESS — accepted change, victory, completed growth/reward operation.
4. PRIMARY — names and immediately actionable values.
5. SECONDARY — supporting resource values and descriptions.

A state must not be communicated by color alone. Frame state, position, label, or reveal/motion must reinforce it.

## 3. Mockups

### Battle HUD

```text
[TURN ORDER]                                         [FOCUS ENEMY]
>[CURRENT] ally                                         Name / HP
  next enemy                                            HP meter
  next ally                                             Poise meter
                                                        INTENT

                   [PROTECTED WORLD VIEW]

[ALLY 1] [ALLY 2] [ALLY 3] [ALLY 4]       [CURRENT ACTOR / COMMAND]
 HP/E    HP/E     HP/E     HP/E              action action action
```

Acceptance:
- world center remains visually dominant;
- CURRENT and enemy INTENT can be found in under one glance;
- four allies remain readable at supported minimum layout;
- command state cannot be confused with passive HUD information.

### Party / Character / Growth

```text
[PARTY FORMATION]                         [SQUAD COST]
[Overview] [Skills] [Growth]

[ROSTER]          [ACTIVE 4]             [SELECTED CHARACTER]
 selected row  ->  selected slot           entity/model preview
 active row        member                   name / star / level
 owned row         member                   role + core stats
 locked row        member                   contextual tab content
                  empty                     growth action only on Growth tab

[Remove] [Reset] [Apply]                                      [Done]
```

Acceptance:
- selected character is more obvious than merely prefixing text;
- active-party membership and roster selection remain distinct states;
- preview is not squeezed out by descriptive text;
- Growth actions are visually tied to growth costs and preview values.

### Battle result

```text
                 [ VICTORY / DEFEAT ]

                 Rewards
            [coin]   amount -> total
            [essence] amount -> total
            [shard]  amount -> total

                    [Continue]
```

Reveal contract:
- T+0: outcome;
- T+6 ticks: reward section begins;
- then one reward row every 3 ticks;
- Continue unlocks after 12 ticks;
- ESC follows the same lock so the result cannot be accidentally skipped immediately.

The timing is deliberately short. It exists to create hierarchy, not to slow farming.

## 4. Asset policy for the next visual pass

No third-party binary art is copied in this document or the first code pass. Before importing any external atlas/icon/font/model:
- verify license and redistribution terms;
- record exact source and intended file usage in `THIRD_PARTY_ASSETS.md`;
- prefer assets that can be recolored/rescaled consistently across all M5 screens;
- reject an asset if it forces the game to inherit another title's identity.

The next authored-asset candidate must cover at minimum:
- idle / focus / disabled frame states;
- section/title treatment;
- party slot treatment;
- HP / Energy / Poise meters;
- reward row treatment;
- compact input-glyph treatment.

## 5. Screenshot gate

Still NOT TESTED in a real Minecraft client.

Required comparison set before M5 visual completion:
- 1920x1080 default GUI scale;
- 1280x720;
- one minimum-supported logical canvas case;
- battle HUD normal turn;
- target selection / command open;
- party Overview, Skills, Growth;
- result with 0 rewards and multiple rewards.

For each screenshot compare against this document's information hierarchy, not pixel similarity to the referenced games.
