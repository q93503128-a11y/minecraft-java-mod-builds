# Openworld RPG — M0 Gameplay Foundation Resolution Evidence

> Date: **2026-09-18**  
> Scope: **primary JAR availability only — not full runtime integration**  
> Historical gate: later `M0_GAMEPLAY_RUNTIME_BOOT_2026-09-18.md` supersedes the runtime-status/next-step lines here; MobFilter was later added as the tenth foundation/safety JAR and the full pinned dependency set now co-loads in the `gameplay` server profile.  
> Successful commit: `067e47500d1f71e77f4c97907f0257a5214a21dc`  
> Workflow: **Build Openworld RPG**  
> Successful run: **35310816215**

## 1. Purpose

This gate proves that the nine non-creature foundation JARs pinned by the M0 stack can be fetched reproducibly by CI before they are admitted into the full gameplay runtime.

The resolution configuration is intentionally separate from `implementation` / `runtimeClasspath`.

Therefore:

- resolving a JAR does not give that dependency gameplay authority;
- the existing `core` server smoke remains independent;
- transitive gameplay dependencies are not silently imported;
- exact adapters are still required before project systems use dependency APIs.

## 2. First attempt and corrective change

Workflow run `35310725627` failed in the new resolution gate.

The first real failure was:

```text
com.geckolib:geckolib-fabric-26.2:5.5.5
→ creator Cloudsmith Maven
→ HTTP 401 Unauthorized
```

This was a repository-access failure, not a code regression and not evidence that GeckoLib 5.5.5 was incompatible.

The smallest corrective change was to retain the exact pinned Fabric 5.5.5 file while changing only its retrieval coordinate to:

```text
curse.maven:geckolib-388172:8819017
```

No gameplay version or behavior was changed.

## 3. Resolved artifacts

| Role | Resolved file | Bytes | SHA-256 |
|---|---|---:|---|
| Better Combat 3.2.2+26.2 Fabric | `5sy6g3kz-enlZuzkJ.jar` | 930628 | `cce827992f517b181bc25c4f6c35e2c75818d29fcdbea90c3838704364b04639` |
| Player Animation Library 1.2.6 | `PlayerAnimationLibFabric-1.2.6+mc.26.2.jar` | 228273 | `d7531e9ce2bdcf394f99a66cc79114f58c8c82ae7e723527ee27545c40619ff1` |
| Trinkets Updated 4.1.0-rc.1+26.2 | `XaT8sLP6-r453A8nE.jar` | 645999 | `bc0c41455373df0475173250912ad45f1592b55c1398913ad06941de6571058c` |
| Armor Model API 1.1.0+26.2 Fabric | `armor-model-api-1.1.0+26.2-fabric.jar` | 95250 | `406226390526a774db9bf83584b8e68a96068998541b3c044cda019c8d93c397` |
| Cloth Config 26.2.155 Fabric | `cloth-config-fabric-26.2.155.jar` | 1135186 | `def4be7639cd66704f7e304d658ea0f6bf490fb4a6eaa2dbf18ec2c3999d6349` |
| GeckoLib 5.5.5 Fabric 26.2 | `geckolib-388172-8819017.jar` | 1185836 | `1cade267232b852fa3ea8f309cd88fc09a8f77b41915519e4d1873cbb39fdc59` |
| Ranged Weapon API 4.0.0+26.2 Fabric | `ranged-weapon-api-4.0.0+26.2-fabric.jar` | 157327 | `87b84bea0ab193257a7666d40d098bb8de392735b736e3440f9553d52ffb27e1` |
| Spell Engine 1.10.5+26.2 Fabric | `spell-engine-1.10.5+26.2-fabric.jar` | 4722174 | `247dc6b63c9f1df62862b39ec6345666a1cade22a1cda560e6ebde27059bf832` |
| Spell Power Attributes 1.6.2+26.2 Fabric | `spell-power-1.6.2+26.2-fabric.jar` | 226491 | `5619b9f65134cce55284af0cb1e879a7bebdcab5d6099936ee1231cedbb89f90` |

The build task writes the same evidence to:

`build/reports/m0/gameplay-foundation-artifacts.txt`

## 4. Canon correction discovered during binding

The previous live M0 wording called Trinkets Updated `4.1.0+26.2` a stable release.

The maintained Spell Engine 26.2 source and the exact distributable accepted for this gate use:

`4.1.0-rc.1+26.2`

The project manifest and M0 dependency canon are corrected to that exact verified baseline.

This is a factual source-binding correction, not a design change.

## 5. Successful verification state

Run `35310816215`:

```text
GAMEPLAY FOUNDATION PRIMARY ARTIFACT RESOLUTION: PASS — 9/9
FOUNDATION FILE SHA-256 CAPTURE: PASS
UNIT TESTS: PASS
CLEAN BUILD: PASS
BOOTSTRAP JAR VERIFY: PASS
DEDICATED SERVER CORE-PROFILE BOOT: PASS
SERVER READY STATE: PASS — Done (4.605s)

FULL TRANSITIVE GAMEPLAY RUNTIME: NOT TESTED
GAMEPLAY PROFILE BOOT: NOT TESTED
BETTER COMBAT ADAPTER: NOT IMPLEMENTED
SPELL ENGINE ADAPTER: NOT IMPLEMENTED
TRINKETS/ARMOR/RANGED ADAPTERS: NOT IMPLEMENTED
EXTERNAL CREATURE RUNTIME BINDING: NOT TESTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

## 6. Next gate

Do not convert this resolution result into a blanket `implementation(...)` dependency dump.

Next:

1. acquire/inspect the exact pinned Alex's Mobs Continued, CodxLib and Threateningly Mobs Continued Fabric JARs;
2. extract their actual `fabric.mod.json` IDs, hard dependencies and nested JARs instead of guessing;
3. close any remaining license/provenance ambiguity without copying restricted bytes into the public repository;
4. define the explicit full gameplay runtime graph;
5. boot the `gameplay` profile with that graph;
6. then implement one bounded real adapter at a time, beginning with damage/cast authority proof rather than broad R01 content.

R01 player-facing implementation remains gated by exact asset binding and real Azari spatial closure.
