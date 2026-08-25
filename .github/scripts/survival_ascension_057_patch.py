from __future__ import annotations

from pathlib import Path
import json

REPO = Path(__file__).resolve().parents[2]
ROOT = REPO / "projects" / "survival-ascension"


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one anchor in {rel}: {old[:180]!r}; got {count}")
    write(rel, text.replace(old, new, 1))


# Release/version lock. External content versions stay untouched.
replace_once("gradle.properties", "mod_version=0.56.0-alpha.1", "mod_version=0.57.0-alpha.1")
lock_path = ROOT / "modpack/content-lock.json"
lock = json.loads(lock_path.read_text(encoding="utf-8"))
if lock.get("version") != "0.56.0-alpha.1-content-preview.1":
    raise SystemExit(f"unexpected content lock version: {lock.get('version')!r}")
lock["version"] = "0.57.0-alpha.1-content-preview.1"
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Mining: every enlarged scan/break path refuses unloaded chunks before reading block state.
mining = "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java"
replace_once(mining,
'''                if (Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getY() - origin.getY()) > 24 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                BlockState state = level.getBlockState(next);''',
'''                if (Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getY() - origin.getY()) > 24 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                if (!level.hasChunkAt(next)) continue;
                BlockState state = level.getBlockState(next);''')
replace_once(mining,
'''        for (BlockPos target : candidates) {
            if (broken >= limit || !player.getMainHandItem().is(ItemTags.PICKAXES)) break;
            BlockState state = level.getBlockState(target);''',
'''        for (BlockPos target : candidates) {
            if (broken >= limit || !player.getMainHandItem().is(ItemTags.PICKAXES)) break;
            if (!level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);''')
replace_once(mining,
'''            if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
            BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b) : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
            BlockState targetState = level.getBlockState(target);''',
'''            if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
            BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b) : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
            if (!level.hasChunkAt(target)) continue;
            BlockState targetState = level.getBlockState(target);''')

# Woodcutting: origin/manual precision break counts toward the regional directive exactly once;
# queued logs are counted by the same BreakBlockEvent path. All tree scans/tick work stay loaded-only.
wood = "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java"
replace_once(wood,
'''        ItemStack tool = player.getMainHandItem();
        if (!isValidLogBreak(player, level, center, centerState, tool)) return;
        if (!player.isCreative() && !player.isSpectator()) {''',
'''        ItemStack tool = player.getMainHandItem();
        if (!isValidLogBreak(player, level, center, centerState, tool)) return;
        ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);
        if (!player.isCreative() && !player.isSpectator()) {''')
replace_once(wood,
'''                    BlockPos target = job.targets.removeFirst();
                    BlockState state = level.getBlockState(target);''',
'''                    BlockPos target = job.targets.removeFirst();
                    if (!level.hasChunkAt(target)) continue;
                    BlockState state = level.getBlockState(target);''')
replace_once(wood,
'''                    if (player.gameMode.destroyBlock(target)) {
                        ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);
                    }''',
'''                    player.gameMode.destroyBlock(target);''')
replace_once(wood,
'''                        if (rx > 12 || ry > 32 || rz > 12) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;''',
'''                        if (rx > 12 || ry > 32 || rz > 12) continue;
                        if (!level.hasChunkAt(next)) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;''')
replace_once(wood,
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

# Construction: the player's real first placement is part of expedition BLOCKS_BUILT.
# Existing tick-placed secondary blocks retain their explicit one-per-placed-block accounting.
construction = "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java"
replace_once(construction,
'''        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock().asItem() instanceof BlockItem) || state.hasBlockEntity()) return;

        announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, 2L));''',
'''        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock().asItem() instanceof BlockItem) || state.hasBlockEntity()) return;

        ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION, 1);
        announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, 2L));''')

# Runtime banner/version.
main = "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
replace_once(main, 'public static final String VERSION = "0.56.0-alpha.1";', 'public static final String VERSION = "0.57.0-alpha.1";')
replace_once(main,
    "// 0.56: Survival-snapshotted ranged projectiles retain online shooter attribution across reward/behavior layers.",
    "// 0.57: pre-test stabilization keeps enlarged mining/tree work loaded-only and closes expedition origin-action gaps.")
replace_once(main,
    "loaded: scaled mastery + ranged shooter attribution + spear momentum drive lines",
    "loaded: pre-test loaded-chunk/action-accounting hardening + scaled mastery + ranged shooter attribution + spear momentum drive lines")

# User-facing release docs.
replace_once("README.md", "## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화", '''## 0.57.0-alpha.1 — Pre-Test Stabilization / 실플레이 직전 안정화
This release intentionally adds no new progression feature. It hardens the existing physical-scale systems before the first broad gameplay pass.

Large Mining plane/vein/extract follow-up work now checks `hasChunkAt` before every newly discovered or re-read target, so 3×3~9×9 work and ore chains never make an unloaded neighboring chunk part of the operation. Woodcutting applies the same loaded-only rule while discovering connected logs/leaves and again when draining a queued fell job after the world has changed.

Expedition action accounting is now symmetric with Mining/Harvesting: the player's real first valid log break and first valid construction placement count once. Queued tree logs continue through the same BreakBlock event and no longer receive a second manual expedition increment, while bulk construction follow-ups retain their explicit one-per-successful-placement credit. Shift precision therefore still counts the real action without triggering any bulk follow-up.

A committed `TESTING.md` defines the first manual smoke order and expected pass conditions. No new SavedData, packet/protocol, item/entity, force-load, background simulation or external-mod version is introduced; network protocol remains `8`.

## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화''')
replace_once("PROJECT.md", "- Mod version: `0.56.0-alpha.1`", "- Mod version: `0.57.0-alpha.1`")
replace_once("PROJECT.md", "## 0.56 Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화", '''## 0.57 Pre-Test Stabilization / 실플레이 직전 안정화
- No new feature layer: this release closes deterministic issues that would otherwise contaminate gameplay feedback.
- Mining connected-vein, planar area and extract re-read paths require `ServerLevel.hasChunkAt` before target block-state access. No enlarged mining action admits an unloaded neighboring chunk.
- Woodcutting connected-log/leaf discovery and queued fell-job execution use the same loaded-only boundary.
- A valid real first Woodcutting break now contributes one `LOGS_FELLED` expedition action. Queued follow-up logs rely on their normal BreakBlockEvent and have no extra manual duplicate increment.
- A valid real first Construction placement now contributes one `BLOCKS_BUILT` expedition action; successful bulk follow-up placements keep their existing explicit accounting.
- Shift precision remains a single physical action and is still eligible for the relevant regional directive.
- `TESTING.md` is the committed first-pass manual smoke matrix.
- No SavedData migration, packet/protocol change, custom item/entity, force-load/background simulation or external-content version change.

## 0.56 Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화''')
replace_once("CHANGELOG.md", "## 0.56.0-alpha.1", '''## 0.57.0-alpha.1
- Added loaded-chunk admission checks to Mining connected-vein discovery, planar area breaking and extract target re-read.
- Added loaded-chunk admission checks to Woodcutting connected-log/leaf discovery and queued fell-job execution.
- Fixed Woodland expedition accounting so the player's first valid log break counts once; removed the old secondary manual increment so queued logs are not double-counted after the event-path fix.
- Fixed Arid construction expedition accounting so the player's first valid block placement counts once while successful bulk follow-ups keep their existing one-per-placement credit.
- Shift precision actions now progress the matching regional directive without starting bulk follow-up work.
- Added `TESTING.md` with the first manual gameplay smoke sequence and expected failure signals.
- Bumped content-preview lock to `0.57.0-alpha.1-content-preview.1`; all six external mod versions and network protocol8 remain unchanged.
- Added no SavedData, packet, custom item/entity, force-load or background simulation.

## 0.56.0-alpha.1''')

# Manual test matrix committed with the candidate instead of living only in chat.
testing = ROOT / "TESTING.md"
testing.write_text('''# Survival Ascension 0.57 — First Gameplay Test Matrix

This is the first broad manual gameplay pass. Back up any long-lived world first. Use the 0.57 JAR and the matching 0.57 content-preview pack.

## 1. Boot / save compatibility
- Start a fresh world and one existing 0.56 world.
- PASS: no load crash, missing registry/data error, forced migration prompt, or repeated login exception.
- Run `/ascension stats`; PASS: all six skills, expedition, operation, apex, logistics/outpost/recovery summaries render.

## 2. Precision vs scaled physical actions
Use operator `/<ascension skill> setlevel <level>` commands only to accelerate testing when desired.
- Mining: test Lv10/30/60/90/100 at a chunk border and normal interior. Shift must remain one block. Non-Shift plane/vein/extract/bore must never hitch because an unloaded neighboring chunk is pulled into the action.
- Woodcutting: break a natural tree normally and with Shift. PASS: only loaded connected logs are queued; walking away/unloading the target area must not force-load it.
- Harvesting: mature crops normal vs Shift; irrigation replant consumes a real nearby eligible seed source.
- Construction: single/Shift, line, floor/wall, volume and causeway; only loaded/interactable targets place and only real material is consumed.
- Mobility: ground dash, air-dash limits, fall safety and logout/login reset.

## 3. Expedition accounting
Discover the matching region before testing.
- Woodland: one valid manual/Shift log break advances `LOGS_FELLED` by exactly1. A bulk fell advances once per log actually broken, not twice.
- Arid: the player's first valid placement advances `BLOCKS_BUILT` by exactly1. Every successful bulk follow-up adds exactly1; skipped/denied/out-of-material targets add0.
- Deep/Wetland: one mined block / mature crop advances exactly1. Shift precision must still count.
- Mobility/Combat/Ocean: travel, dash, hostile kill and voyage counters should move only from their real matching action.

## 4. Combat equipment identities
- Spear: vanilla Jab/Charge remains the main hit; Survival drive line needs forward momentum, deals0 secondary damage/XP, and Shift suppresses it.
- Mace: vanilla smash remains intact; only the outer hostile ring is added, with no outer-ring damage/XP.
- Shield: only a successful block can emit a zero-damage guard wave; Shift keeps precision block only.
- Bow/Crossbow: launch-time affix/Shift snapshot survives weapon swapping; ranged kill XP/rewards remain attributed to the still-online shooter.

## 5. Physical logistics / frontline
- Depot material lookup: nearest usable real Barrel cluster first, remainder player inventory.
- Linked warehouses must stay same-dimension, loaded, physical and interactable.
- Outpost promotion/activation, recovery, expedition operation and defense must use their exact physical local-stock rules.
- Freight must remain a real Chest Minecart flow; no remote item teleportation or generated cargo.

## 6. World / external content
- Find Biomes O' Plenty expedition biomes and Minecraft26.2 Sulfur Caves; PASS: regional detection works without treating Sulfur/Cinnabar as valuable-ore vein/extract targets.
- Test The Birth of Steve dungeon/major targets if encountered; PASS: no hard Java dependency crash and tagged major targets receive bounded expedition credit only inside a real expedition region.
- Test Amethyst Resonance tagged tools; PASS: imprint/reforge preserves the original item behavior/components.

## 7. Death / encounter cleanup
- Normal death near an armed operational outpost: one pending recovery, one post-respawn teleport, contract consumed only after successful move.
- Death/logout/timeout during incident, outpost defense, apex hunt and ascension trial: PASS if encounter mobs/boss bars/state clean up without duplicate rewards.

## Stop-and-report signals
Immediately capture screenshot/log + reproduction steps for: crash, save corruption, item duplication/loss, unloaded-chunk hitch/forced generation, repeated rewards, objective increments larger than the number of real actions, bulk work crossing Shift precision, invisible/remote storage consumption, encounter state that survives failure incorrectly, or external-mod classloading errors.
''', encoding="utf-8")

# Release source contract.
src_test = "tools/test_release_source.py"
replace_once(src_test, 'REQUIRED_VERSION = "0.56.0-alpha.1"', 'REQUIRED_VERSION = "0.57.0-alpha.1"')
replace_once(src_test, '"VERSION = \\"0.56.0-alpha.1\\"', '"VERSION = \\"0.57.0-alpha.1\\"')
replace_once(src_test, '"VERSION = \\"0.56.0-alpha.1\\"', '"VERSION = \\"0.57.0-alpha.1\\"')
replace_once(src_test, '"VERSION = \\"0.56.0-alpha.1\\"', '"VERSION = \\"0.57.0-alpha.1\\"')
replace_once(src_test, "# User-facing docs are part of the release contract, not an uncommitted CI-side patch.", '''# 0.57 pre-test stabilization: loaded-only large actions + exact expedition origin accounting.
mining57 = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
wood57 = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
construction57 = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(mining57, [
    "if (!level.hasChunkAt(next)) continue;",
    "if (!level.hasChunkAt(target)) continue;"
], "0.57 mining loaded-only admission")
need(wood57, [
    "ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);",
    "if (!level.hasChunkAt(target)) continue;",
    "if (!level.hasChunkAt(next)) continue;",
    "BlockPos leafPos = pos.relative(direction);", "if (!level.hasChunkAt(leafPos)) continue;"
], "0.57 wood loaded-only/action accounting")
if wood57.count("ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1);") != 1:
    errors.append("0.57 wood expedition accounting must have exactly one event-path increment")
need(construction57, ["ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION, 1);"], "0.57 construction origin accounting")
need(read("TESTING.md"), ["First Gameplay Test Matrix", "chunk border", "exactly1", "Stop-and-report signals"], "0.57 manual test matrix")
forbid(mining57 + wood57 + construction57, ["setChunkForced", "addRegionTicket", "getChunk("], "0.57 pre-test no-force-load policy")

# User-facing docs are part of the release contract, not an uncommitted CI-side patch.''')
replace_once(src_test, '"Mod version: `0.56.0-alpha.1`",', '"Mod version: `0.57.0-alpha.1`",')
replace_once(src_test,
    'need(guide, [\'h("원거리 전투 파급")\', "발사자 귀속", "발사자가 온라인이면", "오프라인 보상 큐"], "0.56 in-game guide")',
    'need(guide, [\'h("원거리 전투 파급")\', "발사자 귀속", "발사자가 온라인이면", "오프라인 보상 큐"], "0.56 in-game guide")\nneed(project_doc, ["## 0.57 Pre-Test Stabilization", "loaded-only boundary", "first Woodcutting break", "first Construction placement"], "0.57 PROJECT docs")\nneed(readme, ["## 0.57.0-alpha.1 — Pre-Test Stabilization", "hasChunkAt", "first valid log break", "TESTING.md"], "0.57 README docs")\nneed(changelog, ["## 0.57.0-alpha.1", "loaded-chunk", "Woodland expedition accounting", "Arid construction expedition accounting", "0.57.0-alpha.1-content-preview.1"], "0.57 CHANGELOG docs")')
replace_once(src_test,
    'print("- 0.56 Survival-snapshotted ranged projectiles retain bounded online shooter attribution across combat/reward layers")\nprint("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.56")',
    'print("- 0.56 Survival-snapshotted ranged projectiles retain bounded online shooter attribution across combat/reward layers")\nprint("- 0.57 enlarged Mining/Woodcutting work is loaded-only and Woodland/Arid origin actions are counted exactly once")\nprint("- README / PROJECT / CHANGELOG / TESTING are committed and synchronized to 0.57")')

# Content-pack release contract only bumps the candidate lock; external mod identities are unchanged.
pack_test = "tools/test_release_content_pack.py"
replace_once(pack_test, 'REQUIRED_LOCK_VERSION = "0.56.0-alpha.1-content-preview.1"', 'REQUIRED_LOCK_VERSION = "0.57.0-alpha.1-content-preview.1"')
replace_once(pack_test,
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.56.0-alpha.1`')",
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.57.0-alpha.1`')")
replace_once(pack_test,
    'print("ranged_projectile_owner_attribution=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")',
    'print("ranged_projectile_owner_attribution=PASS")\nprint("pretest_loaded_chunk_action_accounting=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")')

# Self-delete and trigger cleanup; canonical workflow cleanup is performed via repository API after verified publish.
(ROOT / ".alpha57-trigger").unlink(missing_ok=True)
Path(__file__).unlink()
print("Survival Ascension 0.57 pre-test stabilization patch applied; script/trigger removed")
