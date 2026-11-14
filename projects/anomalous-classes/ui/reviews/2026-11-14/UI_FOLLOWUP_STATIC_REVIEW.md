# UI Follow-up Static Review — 2026-11-14

Baseline: fetched `origin/main` at `408359f28750f7c0df85b2b7481ff9761a88d119`.

This review covers the invisible focus-slot report, skill-tree return navigation, focus item tier artwork, and the Mental Dominator management summary. It does not claim Minecraft client or gameplay testing.

## Inventory focus slot diagnosis

- The latest available `run/client/logs/latest.log` identifies the launch target as `forgeclientdev`, started at 13:36 on 2026-11-14. It is a development launch, not evidence that a packaged JAR was loaded.
- The previously built local JAR was timestamped 14:46, after that runtime had started. Building a JAR later does not update code/resources already loaded by the running development client. No Minecraft/runClient process was present during this follow-up, and the exact JAR used for the user's screenshot cannot be identified from the available log.
- The log initializes Mixin 0.8.7, but contains no class-specific application or failure lines for either focus-slot mixin. Mixin initialization alone does not establish that these two mixins applied.
- The prior packaged JAR and the new JAR both contain `InventoryMenuFocusMixin.class`, `InventoryScreenFocusMixin.class`, `anomalousclasses.mixins.json`, and the `[[mixins]]` declaration in `META-INF/neoforge.mods.toml`. The JSON lists the menu mixin and the client screen mixin.
- Slot 46 uses a 16×16 item/click area at `(77,35)` and an 18×18 vanilla frame at `(76,34)`. The existing `verifyFocusInventoryUi` check reports clearance from the vanilla player preview across nine supported viewport/GUI-scale cases. The position is adjacent to the preview, but static geometry does not prove that the live renderer or Mixin applied correctly. No coordinate change was justified by the available evidence.
- **Diagnosis:** the screenshot cannot be tied to a known packaged JAR. A stale development-client process is the strongest supported explanation for the version mismatch; a Mixin application failure remains unproven. The user must fully restart the client with the new JAR and check the slot before deciding whether a render/coordinate change is needed.
- The old development log also reports focus recipe parse errors (`Pattern references symbol '.' but it's not defined in the key`). This is a separate recipe/data issue and was not changed in this UI-only task.

## Skill-tree return navigation

- Direct `J` requests now record a one-shot `WORLD` return target. Requests from the profile button record `PROFILE`.
- The target is consumed only when a response arrives while the originating screen is still current. An unrelated/stale response is ignored; pending state clears on disconnect or after 200 client ticks.
- `Esc` and the back button share the same route: a direct `J` entry closes to the world; a profile entry requests the profile state. No server payload, skill purchase, or growth behavior changed.
- `M`, `J`, and `U` mappings were not changed.

## Focus tier artwork

- Reused all six existing class glyphs as the central, class-identifying marks; no external art was introduced.
- Regenerated the existing 30 item PNGs at 16×16. Grade silhouettes now progress through a socket, braced alloy, faceted casing, sealed rune ring, and fractured orbit/shards. Tier differences are shape/material details as well as color; the generator checks dimensions, non-empty alpha, and per-class tier uniqueness.
- Item IDs, item models, recipes, focus effects, and save data were not changed.
- The review image is a static asset comparison, not an in-game screenshot: `class_focus_tiers_comparison.png`.

## Mental Dominator management layout

- Replaced the unframed three-part resource text row with three aligned inset tiles for controlled count, psyche, and upkeep; the psyche tile keeps its live server-value meter.
- Kept the existing two-column list/detail structure, selection, scrolling, command controls, codex, focus entry, conditional return path, and class-change action. No server behavior or outer frame texture changed.
- The screen has not been opened in Minecraft for visual review. Existing layout verifier results are static geometry checks only.

## Verification

- Icon generator: Java 21 headless execution; generated 30 textures and the comparison sheet; dimensions and per-class tier differences passed.
- `clean build`: PASS on Java 21.0.10 / Gradle 8.8; all 16 tasks, including the existing test and verification tasks, completed.
- Existing checks passed for focus registrations/recipes, 12 focus-layout viewports, 18 class-management contexts/viewports, focus slot geometry across 9 viewports, Mental Dominator layout across 24 viewports, and profile/skill-tree KO/EN and 18 viewport cases.
- JAR verifier: PASS; 31,814,581 bytes, 110 classes, no duplicate entries. The JAR contains 30 focus textures, the mixin classes/config/metadata, project assets/data, and the new client navigation class.
- `git diff --check`: PASS; Git emitted only its existing LF/CRLF normalization notices.
- Existing compiler warnings remain for deprecated `EventBusSubscriber.bus()` / `Bus` use and Gradle 9 compatibility.
- `runClient`, in-game UI inspection, CLIENT TESTED, PLAYTESTED, and MULTIPLAYER TESTED: **NOT RUN**.
