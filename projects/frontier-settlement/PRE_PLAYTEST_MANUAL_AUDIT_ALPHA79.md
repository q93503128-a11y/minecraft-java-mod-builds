# Frontier Settlement Alpha.79 — Pre-Playtest Manual Source Audit

This is a human/manual source review performed before the next real Minecraft client test. It does not claim that CI or source inspection proves real-world multiplayer behavior.

Canonical validation trigger: the Alpha.79 source/docs audit set is ready for the repository Java 25 CI pipeline.

## Scope manually reread

- shared project authority and server MAIN-thread request handling;
- civilian population reconciliation, replacement evidence and worker assignment multiplicity;
- outpost production and long-distance transporter authority;
- MAINHAND cargo death recovery and military/external-weapon recovery;
- settlement storage scanning, extraction, insertion and compound costs;
- ordinary buildings, roads, outposts and civil cut/fill/retaining rollback paths;
- player container/fluid/non-natural block protection;
- client snapshot reset on disconnect and shared B/R/Enter/Backspace controls;
- old construction/road/outpost/civil save codec defaults and phase compatibility;
- absence of force-load/logistics teleport/virtual cargo in the inspected authorities.

## Findings fixed

1. **New physical outpost pre-placement material bypass** — a desired block already present at the current blueprint position advanced the step before the per-step material charge. Alpha.79 keeps the existing block, but the builder must fetch and consume that exact wood/stone delta before advancing. Historical prepaid save phases are unchanged.
2. **Ambiguous companion resource tags** — independent wood/stone/metal/food scans could let one badly cross-tagged physical stack appear sufficient for more than one compound cost, producing a partial-removal/success edge. Alpha.79 centralizes an exclusive resource classifier. Any stack matching more than one category fails closed; expedition relics and recognized external weapons are always reserved from ordinary resource use.

## Manual review outcome

No additional source-level critical blocker was found in the inspected paths. Loaded-evidence replacement gates, deterministic first-UUID activity, physical MAINHAND cargo, server-side confirm revalidation, project serialization, rollback ordering, client cache reset and optional/default save codecs remain intact.

## Still not proven until real play

- long two-player session;
- simultaneous building/road/outpost/civil requests from two clients;
- reconnect and repeated save/reload;
- resident death/replacement loops;
- transporter death and cargo pickup/recovery;
- route chunk unload/reload and outpost production unload/reload;
- military reverse supply and external-weapon equip/death/recovery;
- civil cut/fill/retaining save/reload;
- bridge/tunnel NPC navigation;
- full companion candidate stack with graphical client;
- actual spawn/combat density and HUD/UI feel.
