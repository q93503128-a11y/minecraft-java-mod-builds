from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
text = PATH.read_text(encoding="utf-8")

old_constants = '''    private static final double WORK_POSITION_REACHED_SQR = 110.25D;\n    private static final double HIGH_WORK_RANGE_SQR = 196.0D;\n    private static final double SUPPLY_INTERACTION_RANGE_SQR = 9.0D;\n'''
new_constants = '''    private static final double WORK_POSITION_REACHED_SQR = 110.25D;\n    // Direct hand reach is deliberately much smaller than scaffold coverage. Reusing the 14-block\n    // scaffold coverage radius here let a builder stand on the ground and place an entire tower roof.\n    private static final double DIRECT_HIGH_WORK_RANGE_SQR = 25.0D;\n    private static final double HIGH_WORK_RANGE_SQR = 196.0D;\n    private static final double SCAFFOLD_POSITION_REACHED_SQR = 2.25D;\n    private static final double SUPPLY_INTERACTION_RANGE_SQR = 9.0D;\n'''
if new_constants not in text:
    if text.count(old_constants) != 1:
        raise SystemExit(f"constant anchor count={text.count(old_constants)}")
    text = text.replace(old_constants, new_constants, 1)

old_direct = '''        if (relativeY > 3 && builder.distanceToSqr(\n                target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR) {\n            return true;\n        }\n'''
new_direct = '''        if (relativeY > 3 && builder.distanceToSqr(\n                target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) <= DIRECT_HIGH_WORK_RANGE_SQR) {\n            return true;\n        }\n'''
if new_direct not in text:
    if text.count(old_direct) != 1:
        raise SystemExit(f"direct reach anchor count={text.count(old_direct)}")
    text = text.replace(old_direct, new_direct, 1)

old_loop = '''            // Ground work retains the historical wide local envelope as the final compatibility fallback.\n            if (work.getY() <= construction.originY() && workDistance <= WORK_POSITION_REACHED_SQR) return true;\n            // A partial path is not authority: try the next scaffold if this exact work point cannot be reached.\n            if (moveToReachable(builder, work, 1.05D)) return false;\n'''
new_loop = '''            // Ground authority is legal only for genuinely low work. Never let a ground fallback\n            // authorize roof/tower placement merely because the footprint is nearby.\n            if (relativeY <= 3 && work.getY() <= construction.originY()\n                    && workDistance <= WORK_POSITION_REACHED_SQR) return true;\n            // High work only becomes authoritative after the builder has physically reached the\n            // elevated scaffold work cell. Scaffold coverage may be wide across a large roof, but\n            // standing on the ground inside that same coverage radius is no longer sufficient.\n            if (relativeY > 3 && work.getY() > construction.originY()\n                    && workDistance <= SCAFFOLD_POSITION_REACHED_SQR) return true;\n            // A partial path is not authority: try the next scaffold if this exact work point cannot be reached.\n            if (moveToReachable(builder, work, 1.05D)) return false;\n'''
legacy_patched_loop = '''            // Ground work retains the historical wide local envelope as the final compatibility fallback.\n            if (work.getY() <= construction.originY() && workDistance <= WORK_POSITION_REACHED_SQR) return true;\n            // High work only becomes authoritative after the builder has physically reached the\n            // elevated scaffold work cell. Scaffold coverage may be wide across a large roof, but\n            // standing on the ground inside that same coverage radius is no longer sufficient.\n            if (relativeY > 3 && work.getY() > construction.originY()\n                    && workDistance <= SCAFFOLD_POSITION_REACHED_SQR) return true;\n            // A partial path is not authority: try the next scaffold if this exact work point cannot be reached.\n            if (moveToReachable(builder, work, 1.05D)) return false;\n'''
if new_loop not in text:
    if legacy_patched_loop in text:
        if text.count(legacy_patched_loop) != 1:
            raise SystemExit(f"legacy patched loop anchor count={text.count(legacy_patched_loop)}")
        text = text.replace(legacy_patched_loop, new_loop, 1)
    else:
        if text.count(old_loop) != 1:
            raise SystemExit(f"work loop anchor count={text.count(old_loop)}")
        text = text.replace(old_loop, new_loop, 1)

old_high_ground_fallback = '''        // If every scaffold route is temporarily unavailable, preserve the old ground fallback so an\n        // existing save is never made stricter by this hotfix.\n        result.add(ground);\n        return List.copyOf(result);\n'''
new_high_ground_fallback = '''        // High work has no ground-authority fallback. If no claimed, usable scaffold can cover this\n        // placement, construction pauses safely instead of remotely placing from the footprint.\n        return List.copyOf(result);\n'''
if new_high_ground_fallback not in text:
    if text.count(old_high_ground_fallback) != 1:
        raise SystemExit(f"high ground fallback anchor count={text.count(old_high_ground_fallback)}")
    text = text.replace(old_high_ground_fallback, new_high_ground_fallback, 1)

old_finish_repair = '''            if (!canReplaceConstructionTarget(level, placement.pos(), current)) {\n                builder.getNavigation().stop();\n                return false;\n            }\n            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;\n            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;\n            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;\n'''
new_finish_repair = '''            if (!canReplaceConstructionTarget(level, placement.pos(), current)) {\n                builder.getNavigation().stop();\n                return false;\n            }\n            if (server.getTickCount() % BUILD_INTERVAL_TICKS != 0) return false;\n            // Completion itself never depends on scaffold cleanup. But if an already-paid blueprint\n            // block has drifted/missing at height, the repair pass needs the same physical scaffold\n            // authority as ordinary construction. Rebuild only in that exceptional repair path.\n            if (placement.pos().getY() - data.construction().originY() > 3) {\n                ensureConstructionScaffolds(level, data, type, supply);\n            }\n            if (!moveBuilderToWorkPosition(level, data.construction(), type, placement, builder, supply)) return false;\n            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;\n'''
if new_finish_repair not in text:
    if text.count(old_finish_repair) != 1:
        raise SystemExit(f"finish repair anchor count={text.count(old_finish_repair)}")
    text = text.replace(old_finish_repair, new_finish_repair, 1)

PATH.write_text(text, encoding="utf-8")
current = PATH.read_text(encoding="utf-8")
required = [
    "DIRECT_HIGH_WORK_RANGE_SQR = 25.0D",
    "SCAFFOLD_POSITION_REACHED_SQR = 2.25D",
    "<= DIRECT_HIGH_WORK_RANGE_SQR",
    "relativeY <= 3 && work.getY() <= construction.originY()",
    "workDistance <= SCAFFOLD_POSITION_REACHED_SQR",
    "targetDistanceSqr(candidate, target) <= HIGH_WORK_RANGE_SQR",
    "High work has no ground-authority fallback",
    "already-paid blueprint",
    "ensureConstructionScaffolds(level, data, type, supply);",
]
for token in required:
    if token not in current:
        raise SystemExit(f"builder scaffold invariant missing: {token}")

# Neither the original wide direct-target authority nor the historical high-work ground fallback may remain.
old_leak = "target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR"
if old_leak in current:
    raise SystemExit("ground-to-roof high-range authority leak remains")
if "result.add(ground);" in current:
    raise SystemExit("high-work ground fallback remains")
if "if (work.getY() <= construction.originY() && workDistance <= WORK_POSITION_REACHED_SQR) return true;" in current:
    raise SystemExit("ungated ground authority remains")

print("BUILDER SCAFFOLD AUTHORITY PATCH PASS")
