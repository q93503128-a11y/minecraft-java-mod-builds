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
new_loop = '''            // Ground work retains the historical wide local envelope as the final compatibility fallback.\n            if (work.getY() <= construction.originY() && workDistance <= WORK_POSITION_REACHED_SQR) return true;\n            // High work only becomes authoritative after the builder has physically reached the\n            // elevated scaffold work cell. Scaffold coverage may be wide across a large roof, but\n            // standing on the ground inside that same coverage radius is no longer sufficient.\n            if (relativeY > 3 && work.getY() > construction.originY()\n                    && workDistance <= SCAFFOLD_POSITION_REACHED_SQR) return true;\n            // A partial path is not authority: try the next scaffold if this exact work point cannot be reached.\n            if (moveToReachable(builder, work, 1.05D)) return false;\n'''
if new_loop not in text:
    if text.count(old_loop) != 1:
        raise SystemExit(f"work loop anchor count={text.count(old_loop)}")
    text = text.replace(old_loop, new_loop, 1)

PATH.write_text(text, encoding="utf-8")
current = PATH.read_text(encoding="utf-8")
required = [
    "DIRECT_HIGH_WORK_RANGE_SQR = 25.0D",
    "SCAFFOLD_POSITION_REACHED_SQR = 2.25D",
    "<= DIRECT_HIGH_WORK_RANGE_SQR",
    "workDistance <= SCAFFOLD_POSITION_REACHED_SQR",
    "targetDistanceSqr(candidate, target) <= HIGH_WORK_RANGE_SQR",
]
for token in required:
    if token not in current:
        raise SystemExit(f"builder scaffold invariant missing: {token}")

# The old authority leak must be gone from the direct-target check.
old_leak = "target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) <= HIGH_WORK_RANGE_SQR"
if old_leak in current:
    raise SystemExit("ground-to-roof high-range authority leak remains")

print("BUILDER SCAFFOLD AUTHORITY PATCH PASS")
