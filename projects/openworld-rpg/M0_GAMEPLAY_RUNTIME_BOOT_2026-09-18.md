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

BETTER COMBAT PROJECT DAMAGE ADAPTER: NOT IMPLEMENTED
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
