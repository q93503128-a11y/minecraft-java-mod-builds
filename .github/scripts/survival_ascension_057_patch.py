from __future__ import annotations

from pathlib import Path
import json

REPO = Path(__file__).resolve().parents[2]
ROOT = REPO / "projects" / "survival-ascension"


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_n(rel: str, old: str, new: str, expected: int = 1) -> None:
    text = read(rel)
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"expected {expected} anchor(s) in {rel}: {old[:160]!r}; got {count}")
    write(rel, text.replace(old, new))


# Version and one-import content lock. The six external mod versions remain unchanged.
replace_n("gradle.properties", "mod_version=0.56.0-alpha.1", "mod_version=0.57.0-alpha.1")
lock_path = ROOT / "modpack/content-lock.json"
lock = json.loads(lock_path.read_text(encoding="utf-8"))
if lock.get("version") != "0.56.0-alpha.1-content-preview.1":
    raise SystemExit(f"unexpected content-lock version: {lock.get('version')!r}")
lock["version"] = "0.57.0-alpha.1-content-preview.1"
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Mining: large follow-up work never reads a newly discovered/re-read target from an unloaded chunk.
mining = "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java"
replace_n(mining,
'''                if (Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getY() - origin.getY()) > 24 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                BlockState state = level.getBlockState(next);''',
'''                if (Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getY() - origin.getY()) > 24 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                if (!level.hasChunkAt(next)) continue;
                BlockState state = level.getBlockState(next);''')
replace_n(mining,
'''        for (BlockPos target : candidates) {
            if (broken >= limit || !player.getMainHandItem().is(ItemTags.PICKAXES)) break;
            BlockState state = level.getBlockState(target);''',
'''        for (BlockPos target : candidates) {
            if (broken >= limit || !player.getMainHandItem().is(ItemTags.PICKAXES)) break;
            if (!level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);''')
replace_n(mining,
'''            if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
            BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b) : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
            BlockState targetState = level.getBlockState(target);''',
'''            if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
            BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b) : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
            if (!level.hasChunkAt(target)) continue;
            BlockState targetState = level.getBlockState(target);''')

# Woodcutting: the real origin break counts once; queued follow-ups count via their normal BreakBlockEvent.
# Discovery and queued draining both remain strictly loaded-chunk-only.
wood = "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java"
replace_n(wood,
'''        ItemStack tool = player.getMainHandItem();
        if (!isValidLogBreak(player, level, center, centerState, tool)) return;
        if (!player.isCreative() && !player.isSpectator()) {''',
'''        ItemStack tool = player.getMainHandItem();
        if (!isValidLogBreak(player, level, center, centerState, tool)) return;
        ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);
        if (!player.isCreative() && !player.isSpectator()) {''')
replace_n(wood,
'''                    BlockPos target = job.targets.removeFirst();
                    BlockState state = level.getBlockState(target);''',
'''                    BlockPos target = job.targets.removeFirst();
                    if (!level.hasChunkAt(target)) continue;
                    BlockState state = level.getBlockState(target);''')
replace_n(wood,
'''                    if (player.gameMode.destroyBlock(target)) {
                        ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);
                    }''',
'''                    player.gameMode.destroyBlock(target);''')
replace_n(wood,
'''                        if (rx > 12 || ry > 32 || rz > 12) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;''',
'''                        if (rx > 12 || ry > 32 || rz > 12) continue;
                        if (!level.hasChunkAt(next)) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;''')
replace_n(wood,
'''    private static boolean hasAdjacentLeaf(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }''',
'''    private static boolean hasAdjacentLeaf(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos leafPos = pos.relative(direction);
            if (!level.hasChunkAt(leafPos)) continue;
            if (level.getBlockState(leafPos).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }''')

# Construction: the real player placement is part of the regional objective too.
construction = "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java"
replace_n(construction,
'''        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock().asItem() instanceof BlockItem) || state.hasBlockEntity()) return;

        announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, 2L));''',
'''        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock().asItem() instanceof BlockItem) || state.hasBlockEntity()) return;

        ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION, 1);
        announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, 2L));''')

# Runtime identity/banner.
main = "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
replace_n(main, 'public static final String VERSION = "0.56.0-alpha.1";', 'public static final String VERSION = "0.57.0-alpha.1";')
replace_n(main,
    "// 0.56: Survival-snapshotted ranged projectiles retain online shooter attribution across reward/behavior layers.",
    "// 0.57: pre-test stabilization keeps enlarged mining/tree work loaded-only and closes expedition origin-action gaps.")
replace_n(main,
    "loaded: scaled mastery + ranged shooter attribution + spear momentum drive lines",
    "loaded: pre-test chunk/accounting hardening + scaled mastery + ranged shooter attribution + spear momentum drive lines")

# Release docs.
replace_n("README.md", "## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화", '''## 0.57.0-alpha.1 — Pre-Test Stabilization / 실플레이 직전 안정화
This release deliberately adds no new progression layer. It closes deterministic issues before the first broad gameplay pass.

Large Mining plane/vein/extract follow-up work now checks `hasChunkAt` before every newly discovered or re-read target. Woodcutting applies the same loaded-only boundary during connected-log/leaf discovery and when a queued fell job is drained after world state changes.

Expedition accounting is now symmetric with Mining/Harvesting: the player's first valid log break and first valid construction placement count once. Queued logs continue through the normal BreakBlock event and no longer receive a second manual increment, while successful bulk construction follow-ups retain their explicit one-per-placement credit. Shift precision therefore counts the real single action without opening bulk follow-up work.

A committed `TESTING.md` defines the first manual smoke order and pass/failure signals. No new SavedData, packet/protocol, item/entity, force-load, background simulation or external-mod version is introduced; network protocol remains `8`.

## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화''')
replace_n("PROJECT.md", "- Mod version: `0.56.0-alpha.1`", "- Mod version: `0.57.0-alpha.1`")
replace_n("PROJECT.md", "## 0.56 Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화", '''## 0.57 Pre-Test Stabilization / 실플레이 직전 안정화
- No new feature layer: this release removes deterministic noise before gameplay feedback begins.
- Mining connected-vein, planar area and extract re-read paths require `ServerLevel.hasChunkAt` before target block-state access.
- Woodcutting connected-log/leaf discovery and queued fell-job execution use the same loaded-only boundary.
- The first valid Woodcutting break contributes one `LOGS_FELLED`; queued logs rely on their normal BreakBlockEvent and have no extra duplicate increment.
- The first valid Construction placement contributes one `BLOCKS_BUILT`; successful bulk follow-ups keep their existing explicit accounting.
- Shift precision remains a single physical action and still progresses the matching regional directive.
- `TESTING.md` is the committed first-pass manual smoke matrix.
- No SavedData migration, packet/protocol change, custom item/entity, force-load/background simulation or external-content version change.

## 0.56 Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화''')
replace_n("CHANGELOG.md", "## 0.56.0-alpha.1", '''## 0.57.0-alpha.1
- Added loaded-chunk admission checks to Mining connected-vein discovery, planar area breaking and extract target re-read.
- Added loaded-chunk admission checks to Woodcutting connected-log/leaf discovery and queued fell-job execution.
- Fixed Woodland expedition accounting so the player's first valid log break counts once; removed the old secondary manual increment so queued logs are not double-counted.
- Fixed Arid construction expedition accounting so the player's first valid placement counts once while successful bulk follow-ups keep one credit each.
- Shift precision actions now progress the matching regional directive without starting bulk follow-up work.
- Added `TESTING.md` for the first manual gameplay smoke pass.
- Bumped content-preview lock to `0.57.0-alpha.1-content-preview.1`; all six external mod versions and network protocol8 remain unchanged.
- Added no SavedData, packet, custom item/entity, force-load or background simulation.

## 0.56.0-alpha.1''')

(ROOT / "TESTING.md").write_text('''# Survival Ascension 0.57 — First Gameplay Test Matrix

Back up any long-lived world first. Test with the 0.57 JAR and matching 0.57 content-preview pack.

## 1. Boot / save compatibility
- Start one fresh world and one existing 0.56 world.
- PASS: no load crash, registry/data error, migration prompt or repeated login exception.
- Run `/ascension stats`; all six skills plus expedition/operation/apex/logistics/outpost/recovery summaries must render.

## 2. Precision vs physical scale
- Mining Lv10/30/60/90/100: test normal interior and chunk border. Shift=one block. Non-Shift plane/vein/extract/bore must not load or generate an unloaded neighboring chunk.
- Woodcutting: normal and Shift on a natural tree. Only loaded connected logs may queue; unloading the area must not keep/force it loaded.
- Harvesting: normal vs Shift mature crops; irrigation replant must consume a real eligible seed source.
- Construction: Shift/single, line, plane, volume and causeway. Only loaded/interactable targets place and only real material is consumed.
- Mobility: ground dash, air-dash limit, fall safety and logout/login reset.

## 3. Expedition accounting
- Woodland: one valid manual/Shift log break advances `LOGS_FELLED` by exactly1. Bulk fell advances exactly once per log actually broken.
- Arid: the first valid placement advances `BLOCKS_BUILT` by exactly1. Each successful bulk follow-up adds exactly1; skipped/denied/out-of-material targets add0.
- Deep/Wetland: one mined block / one mature crop advances exactly1; Shift precision still counts.
- Mobility/Combat/Ocean: travel, dash, hostile kill and voyage counters move only from their matching real action.

## 4. Combat identities
- Spear: vanilla Jab/Charge stays authoritative; Survival drive line requires forward momentum, deals0 secondary damage/XP and Shift suppresses it.
- Mace: vanilla smash remains intact; only the outer hostile ring is added with0 outer damage/XP.
- Shield: only a successful block can emit the zero-damage guard wave; Shift keeps precision block only.
- Bow/Crossbow: launch-time affix/Shift snapshot survives weapon swapping; kill XP/rewards stay with the still-online shooter.

## 5. Physical logistics / frontline
- Nearby usable physical Barrel cluster first, then player inventory where that system allows it.
- Linked warehouses stay same-dimension, loaded, physical and interactable.
- Outpost/recovery/operation/defense exact-local-stock rules remain authoritative.
- Freight remains a real Chest Minecart flow; no remote item teleportation/generated cargo.

## 6. World / external content
- Check Biomes O' Plenty expedition biomes and Minecraft26.2 Sulfur Caves regional detection; Sulfur/Cinnabar must not become valuable-ore vein/extract targets.
- Check The Birth of Steve dungeon/major targets if encountered; no optional-class loading crash and bounded major-target credit only inside a real expedition region.
- Check Amethyst Resonance tagged tools; imprint/reforge must preserve original item behavior/components.

## 7. Death / encounter cleanup
- Normal death near an armed operational outpost: one pending recovery, one post-respawn teleport, contract consumed only after successful move.
- Death/logout/timeout during incident, defense, apex hunt and ascension trial: encounter mobs/boss bars/state clean up without duplicate rewards.

## Stop-and-report signals
Capture screenshot/log + reproduction steps for any crash, save corruption, item duplication/loss, unloaded-chunk hitch/forced generation, repeated reward, objective increase larger than real actions, bulk work crossing Shift precision, invisible/remote storage consumption, stale encounter state after failure, or external-mod classloading error.
''', encoding="utf-8")

# Release source audit: bump the existing contract and add the deterministic 0.57 invariants.
src_test = "tools/test_release_source.py"
replace_n(src_test, 'REQUIRED_VERSION = "0.56.0-alpha.1"', 'REQUIRED_VERSION = "0.57.0-alpha.1"')
replace_n(src_test, '"VERSION = \\"0.56.0-alpha.1\\"', '"VERSION = \\"0.57.0-alpha.1\\"', expected=3)
replace_n(src_test, '"Mod version: `0.56.0-alpha.1`",', '"Mod version: `0.57.0-alpha.1`",')
marker = "# User-facing docs are part of the release contract, not an uncommitted CI-side patch."
checks = '''# 0.57 pre-test stabilization: loaded-only large actions + exact origin accounting.
mining57 = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
wood57 = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
construction57 = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(mining57, ["if (!level.hasChunkAt(next)) continue;", "if (!level.hasChunkAt(target)) continue;"], "0.57 mining loaded-only")
need(wood57, ["if (!level.hasChunkAt(target)) continue;", "if (!level.hasChunkAt(next)) continue;", "BlockPos leafPos = pos.relative(direction);", "if (!level.hasChunkAt(leafPos)) continue;"], "0.57 wood loaded-only")
if wood57.count("ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);") != 1:
    errors.append("0.57 wood expedition increment must exist exactly once")
if construction57.count("ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION, 1);") < 2:
    errors.append("0.57 construction must account origin plus bulk placements")
need(read("TESTING.md"), ["First Gameplay Test Matrix", "chunk border", "exactly1", "Stop-and-report signals"], "0.57 testing matrix")
forbid(mining57 + wood57 + construction57, ["setChunkForced", "addRegionTicket", "getChunk("], "0.57 no-force-load")

'''
replace_n(src_test, marker, checks + marker)
replace_n(src_test,
    'need(guide, [\'h("원거리 전투 파급")\', "발사자 귀속", "발사자가 온라인이면", "오프라인 보상 큐"], "0.56 in-game guide")',
    'need(guide, [\'h("원거리 전투 파급")\', "발사자 귀속", "발사자가 온라인이면", "오프라인 보상 큐"], "0.56 in-game guide")\nneed(project_doc, ["## 0.57 Pre-Test Stabilization", "loaded-only boundary", "first valid Woodcutting break", "first valid Construction placement"], "0.57 PROJECT docs")\nneed(readme, ["## 0.57.0-alpha.1 — Pre-Test Stabilization", "hasChunkAt", "first valid log break", "TESTING.md"], "0.57 README docs")\nneed(changelog, ["## 0.57.0-alpha.1", "loaded-chunk", "Woodland expedition accounting", "Arid construction expedition accounting", "0.57.0-alpha.1-content-preview.1"], "0.57 CHANGELOG docs")')
replace_n(src_test,
    'print("- 0.56 Survival-snapshotted ranged projectiles retain bounded online shooter attribution across combat/reward layers")\nprint("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.56")',
    'print("- 0.56 Survival-snapshotted ranged projectiles retain bounded online shooter attribution across combat/reward layers")\nprint("- 0.57 Mining/Woodcutting enlarged work is loaded-only and Woodland/Arid origin actions are counted")\nprint("- README / PROJECT / CHANGELOG / TESTING are committed and synchronized to 0.57")')

pack_test = "tools/test_release_content_pack.py"
replace_n(pack_test, 'REQUIRED_LOCK_VERSION = "0.56.0-alpha.1-content-preview.1"', 'REQUIRED_LOCK_VERSION = "0.57.0-alpha.1-content-preview.1"')
replace_n(pack_test,
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.56.0-alpha.1`')",
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.57.0-alpha.1`')")
replace_n(pack_test,
    'print("ranged_projectile_owner_attribution=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")',
    'print("ranged_projectile_owner_attribution=PASS")\nprint("pretest_loaded_chunk_action_accounting=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")')

(ROOT / ".alpha57-trigger").unlink(missing_ok=True)
Path(__file__).unlink()
print("Survival Ascension 0.57 pre-test stabilization applied; staging script/trigger removed")
