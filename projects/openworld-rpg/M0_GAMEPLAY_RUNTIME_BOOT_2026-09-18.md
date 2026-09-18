# Openworld RPG — M0 Gameplay Dependency Runtime Boot Evidence

> Date: **2026-09-18**  
> Scope: **pinned dependency co-load / gameplay-profile dedicated-server boot**  
> Successful code state: `d35658c21bde609001e6d440ef0089be058b74b7`  
> Workflow: **Build Openworld RPG**  
> Successful run: **35313724183**  
> Artifact: `openworld-rpg-m0-d35658c21bde609001e6d440ef0089be058b74b7` / ID `10533643636`

## 1. What this gate proves

This gate proves that the current pinned Openworld RPG Fabric 26.2 dependency stack can be loaded together on a dedicated server while the project runs with:

```text
openworld_rpg.profile=gameplay
```

It proves:

- the project dependency manifest resolves all 13 declared contracts;
- the 10 foundation/safety JARs are reproducibly resolvable;
- the three curated creature dependency JARs have exact verified runtime IDs;
- the pinned top-level dependency set can coexist in one Fabric 26.2 server runtime;
- Openworld RPG's gameplay-profile dependency validator accepts the loaded set;
- the dedicated server reaches Minecraft's ready state.

It does **not** prove:

- Better Combat attacks are routed through project damage authority;
- Spell Engine casts are routed through project Mana/cooldown/damage authority;
- donor mob spawn/loot/stat suppression is implemented;
- R01 creature presentation or hitboxes are accepted;
- client rendering/UI works;
- multiplayer gameplay works;
- R01 is playtested.

A successful co-load is an integration foundation, not finished adapter behavior.

---

## 2. Correct Loom runtime configuration

The first gameplay-runtime attempts used `modLocalRuntime`.

That failed under this project's Minecraft 26.2 no-obfuscation Loom setup because the configuration does not exist there.

Fabric Loom source confirms that when obfuscation is disabled, runtime libraries are added to:

```text
localRuntime
```

rather than `modLocalRuntime`.

Final project rule for this 26.2 dev runtime:

```text
-PopenworldGameplayRuntime=true
→ add pinned external JARs to localRuntime
→ runGameplayServer
→ system property openworld_rpg.profile=gameplay
```

This is a toolchain correction, not a gameplay-design change.

---

## 3. Foundation/safety artifacts resolved by successful run

Run `35313724183` resolved and hashed ten artifacts:

| Role | Resolved file | Bytes | SHA-256 |
|---|---|---:|---|
| Better Combat 3.2.2 Fabric | `5sy6g3kz-enlZuzkJ.jar` | 930628 | `cce827992f517b181bc25c4f6c35e2c75818d29fcdbea90c3838704364b04639` |
| Player Animation Library 1.2.6 | `PlayerAnimationLibFabric-1.2.6+mc.26.2.jar` | 228273 | `d7531e9ce2bdcf394f99a66cc79114f58c8c82ae7e723527ee27545c40619ff1` |
| Trinkets Updated 4.1.0-rc.1+26.2 | `XaT8sLP6-r453A8nE.jar` | 645999 | `bc0c41455373df0475173250912ad45f1592b55c1398913ad06941de6571058c` |
| Armor Model API 1.1.0+26.2 | `armor-model-api-1.1.0+26.2-fabric.jar` | 95250 | `406226390526a774db9bf83584b8e68a96068998541b3c044cda019c8d93c397` |
| Cloth Config 26.2.155 | `cloth-config-fabric-26.2.155.jar` | 1135186 | `def4be7639cd66704f7e304d658ea0f6bf490fb4a6eaa2dbf18ec2c3999d6349` |
| MobFilter 0.28.0+26.2 | `gRn1FzwR-NYpKsGy3.jar` | 79115 | `d0424ebf88e50793d5c17037b391373ea67480e0b4ceabeddd2747b15ec481d2` |
| GeckoLib 5.5.5 | `geckolib-388172-8819017.jar` | 1185836 | `1cade267232b852fa3ea8f309cd88fc09a8f77b41915519e4d1873cbb39fdc59` |
| Ranged Weapon API 4.0.0+26.2 | `ranged-weapon-api-4.0.0+26.2-fabric.jar` | 157327 | `87b84bea0ab193257a7666d40d098bb8de392735b736e3440f9553d52ffb27e1` |
| Spell Engine 1.10.5+26.2 | `spell-engine-1.10.5+26.2-fabric.jar` | 4722174 | `247dc6b63c9f1df62862b39ec6345666a1cade22a1cda560e6ebde27059bf832` |
| Spell Power Attributes 1.6.2+26.2 | `spell-power-1.6.2+26.2-fabric.jar` | 226491 | `5619b9f65134cce55284af0cb1e879a7bebdcab5d6099936ee1231cedbb89f90` |

MobFilter exact Modrinth binding:

```text
project ID: gRn1FzwR
version ID: NYpKsGy3
maven: maven.modrinth:gRn1FzwR:NYpKsGy3
version: 0.28.0+26.2
mod id: mobfilter
```

---

## 4. Curated creature JAR metadata

The exact three creature artifacts were materialized and their real `fabric.mod.json` files were inspected by CI.

### Threateningly Mobs Continued

```text
file: JXjyo7k6-Bdd8lkUM.jar
bytes: 11159622
sha256: 95109d37c4ed674661e06ebe3724a5a1ec55a7d56171800d3da5251cd29a80be
mod id: threateningly_mobs
version: 1.1.1+fabric.26.2
environment: *
hard deps:
  fabric-api = *
  fabricloader >= 0.18.0
  java >= 25
  minecraft ~26.2
nested jars: none
```

### Alex's Mobs Continued

```text
file: alexs-mobs-continued-1635121-8856579.jar
bytes: 27803866
sha256: 00513b921071d033edc1a7fc0268e1f381da5e884dfcaad52f4ee44104593dd5
mod id: alexsmobs
version: 2.1.13
environment: *
hard deps:
  codxlib >= 1.6.0
  fabric-api >= 0.155.2+26.2
  fabricloader >= 0.18.4
  java >= 25
  minecraft = 26.2
nested jars: none
```

### CodxLib

```text
file: codxlib-1633207-8828221.jar
bytes: 196849
sha256: d6887454c1b4cab58b66bd4c0433ea0a76452084535686e7b91c7865ae721282
mod id: codxlib
version: 1.6.0
environment: *
hard deps:
  fabric-api >= 0.155.2+26.2
  fabricloader >= 0.18.4
  java >= 25
  minecraft = 26.2
nested jars: none
```

These IDs are now recorded as `RESOLVED` in the project dependency manifest.

---

## 5. Actual gameplay-profile loaded versions

The successful dedicated-server run reported:

```text
alexsmobs 2.1.13
armor_model_api 1.1.0+26.2
bettercombat 3.2.2
codxlib 1.6.0
geckolib 5.5.5
mobfilter 0.28.0+26.2
player_animation_library 1.2.6+mc.26.2
ranged_weapon_api 4.0.0+26.2
spell_engine 1.10.5+26.2
spell_power 1.6.2+26.2
threateningly_mobs 1.1.1+fabric.26.2
trinkets_updated 4.1.0-rc.1+26.2
```

Cloth Config is also present through the pinned runtime setup; Fabric API/Loader and nested runtime libraries load as expected.

The project validator then logged:

```text
Openworld RPG integration manifest schema 1 accepted (13 dependency contracts, profile gameplay).
Openworld RPG M0 core bootstrap loaded with profile gameplay.
```

Minecraft reached:

```text
Done (6.892s)! For help, type "help"
```

---

## 6. Known dependency warnings

The server boot is successful, but the following third-party warnings remain visible and must not be hidden:

### Threateningly Mobs Continued

On dedicated server:

```text
@Mixin target net.minecraft.client.animation.KeyframeAnimation was not found
threateningly_mobs.mixins.json:LenientAnimationBakeMixin
```

The target is client-only. The warning did not prevent dedicated-server boot.

Status:

```text
KNOWN_DEPENDENCY_WARNING
DEDICATED SERVER BLOCKER: NO in this gate
CLIENT RUNTIME IMPACT: NOT TESTED
```

### Trinkets Updated

On dedicated server:

```text
@Mixin target net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen was not found
trinkets.mixins.json:client.accessor.CreativeModeInventoryScreenAccessor
```

The target is client-only. The warning did not prevent dedicated-server boot.

Status:

```text
KNOWN_DEPENDENCY_WARNING
DEDICATED SERVER BLOCKER: NO in this gate
CLIENT RUNTIME IMPACT: NOT TESTED
```

### MobFilter

The dev environment prints a missing refmap warning and later:

```text
[MobFilter] No rules configured
```

That is expected at this stage because project ecology/spawn suppression rules have not yet been authored.

### Test harness

The first dedicated-server start reads `server.properties` before Minecraft creates defaults, producing a non-fatal file-missing log entry. Both core and gameplay servers still reach ready state. This is test-harness noise, not a mod startup failure.

---

## 7. Failure history and corrective changes

### Attempt A — stale manifest test

After creature runtime IDs were resolved, the existing unit test still expected at least one unresolved dependency.

Correction:

- update the test to require all current manifest contracts to have resolved runtime IDs.

### Attempt B — `modLocalRuntime` dynamic method

Gradle/Loom did not expose `modLocalRuntime(...)` as a usable configuration for this Minecraft 26.2 no-obfuscation project.

### Attempt C — explicit `modLocalRuntime`

The same underlying configuration was absent.

Correction:

- inspect Fabric Loom's current source;
- use the `localRuntime` configuration that Loom itself selects when obfuscation is disabled.

### Attempt D — MobFilter omitted

The gameplay server loaded the other dependency families, then the project authority validator correctly aborted because required mod id `mobfilter` was absent.

Correction:

- pin exact Modrinth artifact `maven.modrinth:gRn1FzwR:NYpKsGy3`;
- add it to both artifact verification and the isolated gameplay runtime.

### Final run

`35313724183` passed all workflow stages.

---

## 8. Verification state

```text
M0 CORE SOURCE BOOTSTRAP: IMPLEMENTED
M0 CORE UNIT TESTS: PASS
M0 CLEAN BUILD: PASS
M0 JAR VERIFY: PASS
M0 CORE DEDICATED SERVER: PASS

FOUNDATION / SAFETY PRIMARY JARS: RESOLVED + HASHED — 10/10
CURATED CREATURE PRIMARY JARS: RESOLVED + METADATA INSPECTED — 3/3
DEPENDENCY MANIFEST RUNTIME IDS: RESOLVED — 13/13
GAMEPLAY-PROFILE DEPENDENCY SERVER BOOT: PASS
PINNED TOP-LEVEL DEPENDENCY CO-LOAD: PASS
SERVER READY STATE: PASS

BETTER COMBAT PROJECT DAMAGE ADAPTER: IMPLEMENTED — authority seam/startup verified; real-hit runtime execution NOT TESTED
SPELL ENGINE PROJECT CAST/DAMAGE ADAPTER: NOT IMPLEMENTED
TRINKETS / ARMOR / RANGED PROJECT ADAPTERS: NOT IMPLEMENTED
MOBFILTER PROJECT ECOLOGY RULES: NOT IMPLEMENTED
ALEX / THREATENINGLY SPAWN-STAT-LOOT OVERRIDES: NOT IMPLEMENTED
CLIENT RUNTIME: NOT TESTED
R01 PLAYER-FACING IMPLEMENTATION: NOT STARTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

---

## 9. Next M0 implementation gate

Do **not** add more broad runtime mods now.

The next technical gate should prove one real authority bridge at a time:

1. **Better Combat adapter**
   - player attack presentation/cadence may come from Better Combat;
   - exactly one server-authorized project damage transaction must result;
   - no donor/default second damage result;
   - stamina/poise/guard rules remain project-owned.

2. **Spell Engine adapter**
   - project skill eligibility consumes project Mana/cooldown;
   - Spell Engine supplies cast/target/delivery;
   - exactly one project damage/heal/status transaction results;
   - donor progression/loot/HUD assumptions remain suppressed.

3. **One real external creature overlay**
   - select an already accepted dependency actor whose exact runtime ID can be verified;
   - suppress donor random spawn/loot;
   - project-authored spawn/stat/loot role becomes authoritative;
   - no donor source/JAR fork.

Player-facing R01 content still waits for exact asset acceptance and real Azari spatial closure.


---

# 10. Follow-up exact metadata regression — 2026-09-18

A later narrow regression pass added direct `fabric.mod.json` inspection for all ten foundation/safety primary JARs in addition to the three creature JARs.

Verified code state:

```text
370e409bedffadac9c5d8671e27679510e6268ce
```

Workflow:

```text
Build Openworld RPG
run 35315238440
artifact openworld-rpg-m0-370e409bedffadac9c5d8671e27679510e6268ce
artifact ID 10535410094
artifact digest sha256:f2d59e3ba6be11ac27ccb1632e1108e5d3d7b932ca39a7d1e28d83d75cbc6cf2
```

Result:

```text
GAMEPLAY FOUNDATION RESOLUTION: PASS — 10/10
GAMEPLAY FOUNDATION FABRIC METADATA INSPECTION: PASS — 10/10
CREATURE FABRIC METADATA INSPECTION: PASS — 3/3
UNIT TESTS: PASS
CLEAN BUILD: PASS
BOOTSTRAP JAR VERIFY: PASS
CORE-PROFILE DEDICATED SERVER: PASS
GAMEPLAY-PROFILE DEDICATED SERVER: PASS
GAMEPLAY MANIFEST CONTRACTS ACCEPTED: 13/13
SERVER READY STATE: PASS
```

Exact foundation metadata observed from the distributed JARs:

| Project role | Actual mod id | Actual version | Required dependency highlights | Nested runtime JARs |
|---|---|---|---|---|
| Better Combat | `bettercombat` | `3.2.2` | Cloth Config, Fabric API, Player Animation Library >=1.2.5, Minecraft >=26.2 | Tiny Config 4.0.0 Fabric |
| Player Animation Library | `player_animation_library` | `1.2.6+mc.26.2` | Fabric command/resource APIs, Loader >=0.19.3, Minecraft >=26.2 | Mochafloats 5.0.0, services 1.3.3 |
| Trinkets Updated | `trinkets_updated` | `4.1.0-rc.1+26.2` | Fabric API, Loader >=0.19.0, Minecraft 26.2.x, Yumi MC Core >=1.1.0+26.2 | Yumi MC Foundation 1.1.1+26.2 |
| Armor Model API | `armor_model_api` | `1.1.0+26.2` | Fabric rendering/resource APIs, Loader >=0.19.3, Java >=25, Minecraft >=26.2 | none |
| Cloth Config | `cloth-config` | `26.2.155` | Loader >=0.14.0, Minecraft >=26.2- | basic-math 0.6.1 |
| MobFilter | `mobfilter` | `0.28.0+26.2` | Loader >=0.15.0, Java >=25, Minecraft >=26.0 | none |
| GeckoLib | `geckolib` | `5.5.5` | Fabric API >=0.152.1+26.2, Loader >=0.19, Java >=25, Minecraft >=26.2 | none |
| Ranged Weapon API | `ranged_weapon_api` | `4.0.0+26.2` | Fabric API, Loader >=0.19.3, Java >=25, Minecraft >=26.2 | none |
| Spell Engine | `spell_engine` | `1.10.5+26.2` | Cloth Config >=26.2.155, Fabric API, Player Animation Library, Spell Power >=1.6.1+26.2 | Tiny Config 4.0.1 Fabric |
| Spell Power Attributes | `spell_power` | `1.6.2+26.2` | Fabric API, Loader >=0.19.3, Java >=25, Minecraft >=26.2 | Tiny Config 4.0.1 Fabric |

Curated creature metadata remained unchanged and re-passed:

```text
alexsmobs 2.1.13
  → codxlib >=1.6.0
  → fabric-api >=0.155.2+26.2
  → fabricloader >=0.18.4
  → java >=25
  → minecraft =26.2

codxlib 1.6.0
  → fabric-api >=0.155.2+26.2
  → fabricloader >=0.18.4
  → java >=25
  → minecraft =26.2

threateningly_mobs 1.1.1+fabric.26.2
  → fabric-api
  → fabricloader >=0.18.0
  → java >=25
  → minecraft ~26.2
```

The latest regression therefore removes the remaining ambiguity around top-level runtime IDs and declared hard dependencies.

At this metadata-only checkpoint, dependency callbacks still had not been promoted to project authority. The Better Combat gate described below was subsequently implemented and verified in §11:

```text
Better Combat presentation/cadence
→ project-owned server damage decision
→ one primary damage application
→ vanilla sweep damage suppressed during the Better Combat attack
```

The current next implementation gate is the Spell Engine resource/cooldown/impact bridge.


---

# 11. Better Combat authority seam — implemented 2026-09-18

The first real M0 authority bridge is now in source.

Verified implementation commit:

```text
579354c1154c8c60ad534fe3c6558a6a57d33e66
openworld-rpg: add Better Combat authority seam
```

Verification workflow:

```text
Build Openworld RPG
run 35325649844
conclusion: SUCCESS
```

## 11.1 Exact boundary

Better Combat 3.2.2 remains responsible for:

- attack presentation and cadence;
- its target-request flow and current-attack identity;
- its existing dual-wield / weapon animation backend.

Openworld RPG now owns the final server melee-damage admission seam:

```text
Better Combat active attack
→ vanilla Player.attack damage proposal
→ Openworld RPG CombatDamageAuthority decision
→ one Entity.hurtOrSimulate primary application
```

The adapter resolves Better Combat's pinned runtime interfaces without adding a compile-time Better Combat dependency:

```text
net.bettercombat.api.EntityPlayer_BetterCombat
net.bettercombat.logic.PlayerAttackProperties
```

If Better Combat is absent, the core profile remains pass-through. If Better Combat is loaded but those pinned interfaces cannot be resolved, startup fails clearly rather than silently dropping authority.

Vanilla sweep damage is suppressed while a Better Combat attack is active. Better Combat's own selected-target loop therefore cannot be accompanied by an accidental second vanilla sweep-damage path.

The current M0 amount policy is intentionally neutral: Better Combat/vanilla's server-rebuilt melee amount is treated as a **proposal** and accepted unchanged after finite/positive validation. This is not the final R01 stat/Defense/poise/status formula. The authority seam exists so that later project combat resolution can replace the policy without moving damage ownership back into Better Combat.

## 11.2 Verification evidence

Run `35325649844` proved:

```text
UNIT TESTS: PASS
CLEAN BUILD: PASS
BOOTSTRAP JAR VERIFY: PASS
CORE-PROFILE DEDICATED SERVER: PASS
GAMEPLAY-PROFILE DEDICATED SERVER: PASS
BETTER COMBAT RUNTIME API PREFLIGHT: PASS
SERVER READY STATE: PASS
```

Gameplay-server log evidence:

```text
Openworld RPG Better Combat authority adapter armed for profile gameplay
using net.bettercombat.api.EntityPlayer_BetterCombat
and net.bettercombat.logic.PlayerAttackProperties.

Done (...)! For help, type "help"
```

The core-profile server also loaded with Better Combat absent and explicitly left the adapter inactive, proving that this seam does not turn Better Combat into a hard dependency of the core bootstrap.

## 11.3 What this still does not prove

```text
REAL PLAYER → TARGET BETTER COMBAT HIT EXECUTED: NO
EXACTLY-ONE DAMAGE OBSERVED IN RUNTIME: NO
FINAL R01 DAMAGE / DEFENSE / POISE / STATUS FORMULA: NOT IMPLEMENTED
CLIENT RUNTIME: NOT TESTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Therefore the accurate status is:

```text
BETTER COMBAT AUTHORITY ADAPTER: IMPLEMENTED
BETTER COMBAT API BINDING SERVER TESTED: YES
BETTER COMBAT REAL-HIT TRANSACTION TESTED: NO
```

The next implementation gate is the Spell Engine resource/cooldown/impact authority bridge, followed by one real external-creature spawn/stat/loot overlay.


---

# 12. Spell Engine cast-admission seam + exact dependency version enforcement — 2026-09-18

Verified implementation commits:

```text
267e69f1a967da5620240733aa64a98d99eca7a3
openworld-rpg: gate project spells through authority

2566170d51b00557dbf000fdd6c34607707902b9
openworld-rpg: enforce pinned integration contracts
```

Verification workflow:

```text
Build Openworld RPG
run 35329917184
conclusion: SUCCESS
```

What changed:

- `ActorIntegrationOverlayValidator` now requires every canonical external-actor ownership dimension, including presentation, animation, movement/combat AI, recipes, worldgen and capture/duplication policy instead of validating only the combat/economy subset;
- all 13 runtime dependency contracts enforce the exact versions read from the pinned distributed JAR metadata;
- Better Combat `3.2.2` and Player Animation Library `1.2.6+mc.26.2` use their actual `fabric.mod.json` version strings rather than artifact/display shorthand;
- Spell Engine is bound through a reflection-isolated event adapter using `CASTING_ATTEMPT.PRE`, `COST_CONSUME` and `SPELL_CAST`;
- server-side `openworld_rpg:*` spells are fail-closed until a real `SpellCastAuthority.Policy` exists;
- non-project Spell Engine spells are not silently claimed as Openworld RPG progression content;
- the CI gameplay smoke now requires explicit Better Combat and Spell Engine adapter activation logs.

The exact verification state is:

```text
ACTOR OVERLAY FULL OWNERSHIP VALIDATION: IMPLEMENTED + UNIT TESTED
DEPENDENCY EXACT VERSION VALIDATION: IMPLEMENTED + GAMEPLAY SERVER TESTED
SPELL ENGINE EVENT API BINDING: IMPLEMENTED + GAMEPLAY SERVER STARTUP TESTED
PROJECT SPELL WITHOUT AUTHORITY POLICY: FAIL-CLOSED BY CODE + UNIT TESTED

PROJECT MANA TRANSACTION THROUGH SPELL ENGINE: NOT IMPLEMENTED
PROJECT COOLDOWN TRANSACTION THROUGH SPELL ENGINE: NOT IMPLEMENTED
PROJECT SPELL IMPACT/DAMAGE TRANSACTION: NOT IMPLEMENTED
REAL PLAYER SPELL CAST EXECUTED: NO
CLIENT RUNTIME: NOT TESTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Therefore this pass closes the **cast-admission/event boundary**, not the full Spell Engine authority milestone. The next Spell Engine gate must use the real project resource/cooldown/combat state rather than a temporary Mana ledger or donor cost system.
