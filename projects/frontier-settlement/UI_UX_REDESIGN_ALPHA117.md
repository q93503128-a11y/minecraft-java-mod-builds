# Frontier Settlement — UI/UX Redesign Alpha.117

This file is the project-side UI/UX authority for the Alpha.117 overhaul. It applies the repository-wide `docs/QUALITY_STANDARD.md` to Frontier Settlement without expanding the management burden or changing the server-authoritative settlement model.

## 1. Product/UI intent

Frontier Settlement is a survival settlement / territory-growth mod. The UI must help the player make a small number of meaningful decisions quickly while the deeper logistics simulation stays in the world.

The UI must therefore feel like a **settlement command surface**, not an RPG inventory dashboard and not a developer/debug panel.

Hard UI rules:

- M is the current settlement/infrastructure palette key.
- Do not introduce a separate key for every subsystem.
- Do not add per-worker priority tables, tax dashboards, happiness spreadsheets or giant research screens.
- Physical storage, workers, construction and outposts remain visible in-world authorities; UI summarizes and selects intent.
- Never hide a critical locked/error state behind color alone.
- Do not treat a functional Java Screen as visually finished until an actual client screenshot has been reviewed.

## 2. Reference extraction

The overhaul uses patterns rather than copying a single game.

- **Anno 1800**: persistent resource summary, grouped build intent, blueprint/placement feedback, contextual cost/requirement presentation.
- **Frostpunk / Frostpunk 2**: clear construction categories, strong selected state, compact requirement/output information.
- **Against the Storm**: compact resource/status hierarchy and contextual construction detail.
- **Northgard**: low-friction categorized build selection suited to a game where the world remains primary.
- **Manor Lords**: construction choice + short functional description + cost before placement.
- **Cities: Skylines**: horizontal construction-family navigation rather than a deep management tree.
- **Timberborn**: persistent resources, explicit construction progress/missing-material states, reusable visual components.
- **Foundation**: contextual building detail and direct build action while keeping the settlement visible as the main play space.

Frontier-specific conclusion:

`settlement identity/status -> resources -> current growth goal -> construction family -> building choice -> contextual detail -> world placement`

This is the intended visual/interaction order.

## 3. Information hierarchy

### First information the player should see

1. Current settlement tier and tier position (1–6).
2. Current shared resources and population.
3. The next growth objective / blocker.

### Most frequent actions

1. Pick a construction family.
2. Pick an unlocked, affordable building.
3. Enter world placement mode.
4. For infrastructure: road / outpost / civil work / location.

### Supporting information

- footprint and clear height;
- wood/stone cost;
- housing gain;
- unlock requirement;
- affordable / insufficient / locked state.

### Rare actions

- guide;
- settlement/outpost location review.

### Screen behavior

- This is an in-game non-pausing screen.
- Expected dwell time is short: normally a few seconds.
- Esc/Close returns to the world.
- M is also the mental shortcut for returning to the settlement palette.
- Placement mode exits the menu and returns focus to the world immediately.

## 4. Frontier UI design system

Alpha.117 introduces shared client-side tokens instead of each screen inventing colors and spacing.

### Color roles

- Background: deep charcoal-brown, high opacity.
- Surface: warm dark neutral.
- Elevated Surface: slightly lighter neutral used only where hierarchy requires it.
- Primary/Accent: restrained brass/amber associated with settlement construction.
- Success: muted green.
- Warning: warm amber.
- Danger: muted red.
- Disabled: desaturated neutral.
- Primary Text: warm off-white.
- Secondary Text: lower-contrast warm gray.
- Border/Divider: subtle warm neutral.

No glow-heavy fantasy treatment, excessive gradients or card-per-line design.

### Spacing scale

- XS = 4
- S = 8
- M = 12
- L = 16
- XL = 24

Layout code should derive offsets from these values instead of accumulating unrelated 7/11/13px constants.

### Typography roles

- Screen title / settlement identity: primary + bold.
- Tier: accent + bold.
- Section/category title: primary/accent.
- Resource/value: primary.
- Support text: secondary.
- Disabled/locked: disabled + explicit text.
- Warning/error: warning/danger + explicit text.

## 5. M palette wireframe

Wide layout:

```text
┌──────────────────────────────────────────────────────────────┐
│ FRONTIER SETTLEMENT        마을 등급  개척 도시   4 / 6     │
│ [■][■][■][■][□][□]         다음 성장  ...                   │
│ 목재 000  석재 000  금속 000  식량 000  인구 00             │
├──────────────────────────────────────────────────────────────┤
│ 기반 | 생산 | 제작·서비스 | 방어 | 랜드마크 | 인프라         │
├──────────────────────────┬───────────────────────────────────┤
│ 건물 목록                │ 선택/hover 건물 상세             │
│ 이름   상태              │ 기능 / 부지 / 비용 / 해금        │
│ 이름   상태              │ 건설 가능 / 자원 부족 / 잠김     │
│ ...                      │                                   │
├──────────────────────────┴───────────────────────────────────┤
│ M 메뉴 · R 회전 · Enter 확정             가이드      닫기   │
└──────────────────────────────────────────────────────────────┘
```

Narrow layout:

- keep the same information order;
- compact category labels if necessary;
- collapse detail below the list instead of allowing overlap;
- never clip the tier, current goal, building state or close action.

## 6. Interaction states

### Default

- first available building is the contextual detail fallback;
- category selection is visually explicit.

### Hover

- hovered building becomes the contextual detail target;
- row gains a structural marker/border change, not color alone.

### Selected/category

- selected category uses an accent marker + label state.

### Disabled / locked

- explicit `잠김` text;
- unlock requirement visible in detail;
- no ambiguous click affordance.

### Resource shortage

- explicit `자원 부족` state;
- cost and missing resource remain readable;
- do not rely only on orange/red.

### Error / server rejection

- world placement HUD must show the server-provided reason, not only a red ghost.

### Empty / sync wait

- show `동기화 대기 중` or an equivalent player-facing state instead of blank space.

## 7. HUD redesign rules

Idle HUD must be quieter than the M screen.

- First line: tier + population.
- Second line: compact shared resources.
- Current active project may add one progress strip.
- The full next-growth explanation belongs primarily in M; do not permanently expand the HUD into a quest panel unless it is currently actionable/critical.
- Placement mode gets its own stronger status surface with:
  - selected building/infrastructure;
  - valid / invalid / checking state text;
  - cost when relevant;
  - controls.

## 8. Performance/UI coupling

The UI must not justify expensive server polling.

- snapshot data remains server-authoritative;
- avoid adding a new packet stream merely for animation;
- shared-storage aggregation should use one combined container pass where possible;
- repeated environment/worker scans should be event-driven, cached or cadence-limited according to `docs/QUALITY_STANDARD.md`;
- no client UI action force-loads chunks.

## 9. Acceptance checklist

Implementation is not complete until all applicable items pass.

- [ ] M screen makes settlement tier immediately obvious.
- [ ] 6-stage tier progress is visible without reading a sentence.
- [ ] next growth goal is visible near tier, not buried in a footer.
- [ ] resources/population have a stable visual location.
- [ ] construction categories have an unmistakable selected state.
- [ ] building rows communicate available / resource-short / locked using text + structure/color.
- [ ] contextual detail shows cost, footprint, housing, unlock requirement.
- [ ] infrastructure uses the same visual language as buildings.
- [ ] Esc/Close/M behavior remains predictable.
- [ ] no new management key proliferation.
- [ ] HUD is less obstructive while idle.
- [ ] placement mode reason text remains server-derived.
- [ ] 16:9, 16:10, small window and multiple GUI scales are checked.
- [ ] long Korean labels and large resource values do not overlap critical controls.
- [ ] locked / insufficient / empty / hover states are actually checked.
- [ ] Java 25 clean build passes.
- [ ] runtime JAR verification passes.
- [ ] an actual Minecraft client screenshot is reviewed against this document and the reference patterns.

## 10. Non-goals for Alpha.117

- no UI framework dependency merely for this screen;
- no LDLib2 for a construction palette;
- no new economy/stat layer;
- no per-NPC management dashboard;
- no copied commercial-game art;
- no claim of visual completion before graphical client review.
