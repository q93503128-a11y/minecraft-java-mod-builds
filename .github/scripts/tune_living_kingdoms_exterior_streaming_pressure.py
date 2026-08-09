from pathlib import Path

root = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world")

builder = root / "ErdenKingdomExteriorBuilder.java"
text = builder.read_text(encoding="utf-8")
for old, new in [
    ("private static final int CI_TICK_BUDGET = 16_000;", "private static final int CI_TICK_BUDGET = 4_000;"),
    ("private static final int CI_MAX_IN_FLIGHT = 3;", "private static final int CI_MAX_IN_FLIGHT = 2;"),
]:
    if old in text:
        text = text.replace(old, new, 1)
    elif new not in text:
        raise SystemExit(f"missing builder tuning anchor: {old}")
builder.write_text(text, encoding="utf-8")

edits = root / "IncrementalWorldEditPlan.java"
text = edits.read_text(encoding="utf-8")
old = "private static final long MAX_APPLY_NANOS = 40_000_000L;"
new = "private static final long MAX_APPLY_NANOS = 12_000_000L;"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("missing incremental time-slice anchor")
edits.write_text(text, encoding="utf-8")

workforce = root / "ErdenExteriorWorkforceManager.java"
text = workforce.read_text(encoding="utf-8")
old = "private static final int SPAWN_BUDGET = 3;"
new = "private static final int SPAWN_BUDGET = 2;"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("missing workforce materialisation budget anchor")
workforce.write_text(text, encoding="utf-8")
