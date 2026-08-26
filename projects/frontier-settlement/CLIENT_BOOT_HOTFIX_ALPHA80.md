# Frontier Settlement 0.1.0-alpha.80 — client boot hotfix

## Trigger

A real Modrinth App client launch of Alpha.79 failed during entity-renderer creation with:

- `java.lang.NullPointerException: Components not bound yet`
- `FrontierSoldierRenderer.<clinit>`
- `new ItemStack(Items.IRON_SWORD)`

This was a Frontier client bootstrap defect, not a companion-mod dependency conflict.

## Root cause

`FrontierSoldierRenderer` held the presentation-only fallback iron service sword in a `private static final ItemStack`. Minecraft 26.2 may load entity renderer classes while registry-backed item components are still being bound during resource reload. Constructing that stack in the class initializer therefore touched `ItemStack` too early and aborted client startup.

## Fix

Alpha.80 removes registry-backed ItemStack construction from renderer class initialization. The fallback service sword is now created lazily on the first real render-state extraction and cached on that renderer instance.

The fallback remains visual only:

- it is never inserted into the soldier entity;
- it is never inserted into settlement storage or any player inventory;
- the server armory remains the sole authority for real synced MAINHAND equipment;
- an actually equipped external weapon still renders from the entity's real synced ItemStack.

The Alpha.80 cumulative source audit also scans the Frontier client package and fails if a registry-backed static `ItemStack` initializer matching this bootstrap hazard is introduced again.

## Compatibility / scope

No companion version changed. Alpha.80 reuses the Alpha.79 candidate companion stack and changes no settlement save schema, resource cost, resident lifecycle, project authority, logistics, combat balance, world generation, keybind or UI flow.

This hotfix is build/source verifiable, but successful build and dedicated-server checks do **not** prove client acceptance. The crash is only considered runtime-accepted after a real Modrinth client launch reaches the title screen and a fresh-world test can begin.
